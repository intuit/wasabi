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

package com.intuit.wasabi.database;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.Inject;
import com.intuit.wasabi.database.impl.DBITransaction;
import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;

import javax.sql.DataSource;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * INFO: includes registering a MySql connection health check (metrics-healthchecks)
 */
public class DBITransactionFactory extends TransactionFactory {

    private static final Logger LOGGER = getLogger(DBITransactionFactory.class);
    private DBI dbi;
    private DataSource dataSource;

    @Inject
    public DBITransactionFactory(BoneCPConfig config, HealthCheckRegistry healthChecks) {
        super();

        // Register for health check
        healthChecks.register("MySql", new PrivateMySqlHealthCheck(this));

        LOGGER.debug("Creating BoneCPDataSource");
        this.dataSource = new BoneCPDataSource(config);
        this.dbi = new DBI(dataSource);

        // Register JDBI argument factories
        dbi.registerArgumentFactory(new UUIDArgumentFactory());
        dbi.registerArgumentFactory(new ExperimentIDArgumentFactory());
        dbi.registerArgumentFactory(new BucketLabelArgumentFactory());
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Transaction newTransaction() {
        return new DBITransaction(dbi);
    }

    private static class PrivateMySqlHealthCheck extends HealthCheck {
        private TransactionFactory transactionFactory;

        public PrivateMySqlHealthCheck(TransactionFactory transactionFactory) {
            this.transactionFactory = transactionFactory;
        }

        @Override
        public Result check() {
            Transaction trans = transactionFactory.newTransaction();
            boolean res = false;
            String msg = "";

            try {
                // "SELECT 1" means SQL connection validation of db
                trans.select("SELECT 1");
                res = true;
            } catch (Exception ex) {
                LOGGER.error("Unable to do check", ex);
                msg = ex.getMessage();
            } finally {
                //No need to close here as it (handle) gets closed as part of select() operation.
                //((DBITransaction) trans).close();
            }

            return res ? Result.healthy() : Result.unhealthy(msg);
        }

    }
}
