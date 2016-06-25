/*******************************************************************************
 * Copyright 2016 Intuit
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.database.impl;

import com.intuit.wasabi.database.HandleBlock;
import com.intuit.wasabi.database.Transaction;
import com.intuit.wasabi.exceptions.ConstraintViolationException;
import com.intuit.wasabi.exceptions.DatabaseException;
import com.intuit.wasabi.exceptions.WasabiServerException;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiException;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Update;
import org.skife.jdbi.v2.exceptions.DBIException;
import org.skife.jdbi.v2.util.IntegerMapper;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;
import static org.slf4j.LoggerFactory.getLogger;

public class DBITransaction implements Transaction {

    private static final Logger LOGGER = getLogger(DBITransaction.class);
    private final Pattern notNullPattern = compile("^.*Column \'(\\S+)\' cannot be null");
    private final Pattern duplicateEntryPattern = compile("^.*Duplicate entry \'(\\S+)\' for key \'(\\S+)\'");
    private Handle handle;
    private DBI dbi;
    private boolean inTransaction = false;

    public DBITransaction(DBI dbi) {
        this.dbi = dbi;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object transaction(Block block) {
        try {
            begin();
            Object value = block.value(this);
            commit();
            return value;
        } catch (WasabiException | IllegalArgumentException e) {
            LOGGER.warn("Problem executing block. Trying to rollback...", e);
            tryRollback();
            throw e;
        } catch (Exception e) {
            LOGGER.warn("Unexpected exception executing block. Trying to rollback...", e);

            tryRollback();

            throw new DatabaseException("Unexpected exception when executing block \"" + block + "\"", e);
        } finally {
            close();
        }
    }

    private void tryRollback() {
        try {
            rollback();
        } catch (Exception e) {
            LOGGER.error("Failed rolling back transaction", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <S> S attach(Class<S> transactional) {
        return getHandle().attach(transactional);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List select(final String query, final Object... params) {
        return (List) perform(new HandleBlock() {
            /**
             * {@inheritDoc}
             */
            @Override
            public Object value(Handle arg) {
                return handle.select(query, params);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert(final String query, final Object... params) throws Exception {
        insert(false, query, params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer insertAndReturnKey(final String query, final Object... params) {
        return insert(true, query, params);
    }

    private Integer insert(final boolean shouldReturnId, final String query, final Object... params) {
        try {
            return (Integer) perform(new HandleBlock() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public Object value(Handle handle) {
                    Update statement = handle.createStatement(query);

                    for (int i = 0; i < params.length; i++) {
                        statement.bind(i, params[i]);
                    }

                    return shouldReturnId ?
                            statement.executeAndReturnGeneratedKeys(IntegerMapper.FIRST).first() : statement.execute();
                }
            });
        } catch (ConstraintViolationException ex) {
            LOGGER.error("Failed to execute query: " + query, ex);

            throw ex;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer update(final String query, final Object... params) {
        return (Integer) perform(new HandleBlock() {
            /**
             * {@inheritDoc}
             */
            @Override
            public Object value(Handle handle) {
                return handle.update(query, params);
            }
        });
    }

    private Object perform(HandleBlock block) {
        handle = getHandle();
        try {
            return block.value(handle);
        } catch (DBIException ex) {
            Exception inner = (Exception) ex.getCause();

            if (inner != null) {
                throw remapMySQLException((SQLException) inner);
            }

            throw ex;
        } finally {
            if (!inTransaction) {
                close();
            }
        }
    }

    protected Handle getHandle() {
        if (null == handle) {
            handle = dbi.open();
            // Force MySQL timezone to UTC
            handle.execute("set time_zone = \"+0:00\"");
        }

        return handle;
    }

    protected void begin() throws WasabiServerException {
        Handle h = getHandle();

        try {
            Connection connection = h.getConnection();

            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new DatabaseException("Error obtaining database connection", e);
        }

        h.begin();

        inTransaction = true;
    }

    protected void commit() {
        if (handleIsMissing()) {
            return;
        }

        handle.commit();

        inTransaction = false;
    }

    protected void rollback() {
        if (handleIsMissing()) {
            return;
        }

        handle.rollback();

        inTransaction = false;
    }

    public void close() {
        if (handleIsMissing()) {
            return;
        }

        handle.close();

        handle = null;
    }

    private boolean handleIsMissing() {
        if (handle == null) {
            LOGGER.warn("no handle present - this should not happen");

            return true;
        }

        return false;
    }

    private WasabiException remapMySQLException(SQLException ex) {
        String msg = ex.getMessage();
        final Matcher notNull = notNullPattern.matcher(msg);

        if (notNull.matches()) {
            return new ConstraintViolationException(
                    ConstraintViolationException.Reason.NULL_CONSTRAINT_VIOLATION,
                    null,
                    new HashMap<String, Object>() {{
                        put("columns", notNull.group(1));
                    }});
        }

        final Matcher duplicateEntry = duplicateEntryPattern.matcher(msg);

        if (duplicateEntry.matches()) {
            return new ConstraintViolationException(
                    ConstraintViolationException.Reason.UNIQUE_CONSTRAINT_VIOLATION,
                    null,
                    new HashMap<String, Object>() {{
                        put("columns", duplicateEntry.group(2));
                        put("values", duplicateEntry.group(1));
                    }});
        }

        return new DatabaseException(ex.getMessage(), ex);
    }
}
