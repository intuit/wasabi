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
package com.intuit.wasabi.cassandra.datastax.health;

import com.codahale.metrics.health.HealthCheck;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Cassandra healthcheck class
 */
public class DefaultCassandraHealthCheck extends HealthCheck {

    public static final String SELECT_NOW_FROM_SYSTEM_LOCAL = "SELECT now() FROM system.local";
    private static final Logger LOGGER = getLogger(DefaultCassandraHealthCheck.class);
    /**
     * {@inheritDoc}
     */
    private Session session;
//    private PreparedStatement preparedStatement;

    public DefaultCassandraHealthCheck(Session session) {
        super();
        this.session = session;
//        this.preparedStatement = this.session.prepare(SELECT_NOW_FROM_SYSTEM_LOCAL)
//                .setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
//                .disableTracing();
    }
//
//    BoundStatement getBoundedStatemen(){
//        return new BoundStatement(preparedStatement);
//    }

    /**
     * @return Result of healthy or unhealthy based on the cql statement
     */
    @Override
    public Result check() {
        boolean res = false;
        String msg = "";
        try {
//            this.session.execute( getBoundedStatemen() );
            this.session.execute(SELECT_NOW_FROM_SYSTEM_LOCAL);
            res = true;
        } catch (NoHostAvailableException ex) {
            LOGGER.error("No hosts available", ex);
            msg = ex.getMessage();
        }

        return res ? Result.healthy() : Result.unhealthy(msg);
    }
}
