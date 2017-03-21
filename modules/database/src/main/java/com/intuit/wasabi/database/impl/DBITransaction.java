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

import static java.util.Objects.isNull;
import static java.util.regex.Pattern.compile;
import static org.slf4j.LoggerFactory.getLogger;

public class DBITransaction implements Transaction {

    private static final Logger LOGGER = getLogger(DBITransaction.class);
    private DBI dbi;
    private final Pattern notNullPattern = compile("^.*Column \'(\\S+)\' cannot be null");
    private final Pattern duplicateEntryPattern = compile("^.*Duplicate entry \'(\\S+)\' for key \'(\\S+)\'");

    public DBITransaction(DBI dbi) {
        this.dbi = dbi;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object transaction(Block block) {
        Handle handle = null;
        try {
            handle = getHandle();
            begin(handle);
            Object value = block.value(this);
            commit(handle);
            return value;
        } catch (WasabiException | IllegalArgumentException e) {
            LOGGER.warn("Problem executing block. Trying to rollback...", e);
            rollback(handle);
            throw e;
        } catch (Exception e) {
            LOGGER.warn("Unexpected exception executing block. Trying to rollback...", e);
            rollback(handle);
            throw new DatabaseException("Unexpected exception when executing block \"" + block + "\"", e);
        } finally {
            // Close the handle (internally this call releases the JDBC connection to the pool)
            close(handle);
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
            public Object value(Handle handle) {
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

    /**
     * Make use of perform() operation to execute INSERT statement
     *
     * @param shouldReturnId
     * @param query
     * @param params
     * @return
     */
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

    /**
     * Execute a block (database operation) with a new handle
     *
     * @param block representing a database operation
     * @return The result of a given database operation
     */
    private Object perform(HandleBlock block) {
        Handle handle = getHandle();
        try {
            return block.value(handle);
        } catch (DBIException ex) {
            Exception inner = (Exception) ex.getCause();

            if (inner != null) {
                throw remapMySQLException((SQLException) inner);
            }

            throw ex;
        } finally {
            // Close the handle (internally this call releases the JDBC connection to the pool)
            close(handle);
        }
    }

    /**
     * Creates a new handle to perform database operation.
     * Handle represents a connection to the database system. It is a wrapper around a JDBC Connection object.
     *
     * @return handle to perform database operation.
     */
    protected Handle getHandle() {
        //This operation reuses JDBC Connection object from the pool and then creates a new handle object.
        Handle handle = dbi.open();

        // Force MySQL timezone to UTC
        handle.execute("set time_zone = \"+0:00\"");

        return handle;
    }

    /**
     * Begin a new transaction for this instance of DBITransaction.
     * <p>
     * Initialize the handle of this instance of DBITransaction
     *
     * @throws WasabiServerException
     */
    protected void begin(Handle handle) throws WasabiServerException {
        try {
            Connection connection = handle.getConnection();

            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new DatabaseException("Error obtaining database connection", e);
        }

        handle.begin();
    }

    /**
     * Commits an active transaction of this instance of DBITransaction
     */
    protected void commit(Handle handle) {
        try {
            handle.commit();
        } catch (Exception e) {
            LOGGER.error("Failed to commit transaction", e);
        }
    }

    /**
     * Rollback an active transaction of this instance of DBITransaction
     */
    protected void rollback(Handle handle) {
        if (isNull(handle)) return;

        try {
            handle.rollback();
        } catch (Exception e) {
            LOGGER.error("Failed rolling back transaction", e);
        }
    }

    /**
     * Close the handle (JDBC Connection) of this instance of DBITransaction
     */
    public void close(Handle handle) {
        if (isNull(handle)) return;

        try {
            handle.close();
        } catch (Exception e) {
            LOGGER.error("Failed to close handle", e);
        }
    }

    /**
     * Re-map MySQL Exception
     *
     * @param ex
     * @return
     */
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
