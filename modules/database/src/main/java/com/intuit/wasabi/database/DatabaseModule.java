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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.jolbox.bonecp.BoneCPConfig;
import org.slf4j.Logger;

import java.util.Properties;

import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.autumn.utils.PropertyFactory.getProperty;
import static java.lang.Integer.parseInt;
import static org.slf4j.LoggerFactory.getLogger;

public class DatabaseModule extends AbstractModule {

    public static final String PROPERTY_NAME = "/database.properties";
    private static final Logger LOGGER = getLogger(DatabaseModule.class);

    @Override
    protected void configure() {
        LOGGER.debug("installing module: {}", DatabaseModule.class.getSimpleName());

        bind(TransactionFactory.class).to(DBITransactionFactory.class).asEagerSingleton();

        LOGGER.debug("installed module: {}", DatabaseModule.class.getSimpleName());
    }

    @Provides
    BoneCPConfig provideCPConfig() {

        BoneCPConfig config = new BoneCPConfig();

        // Presuming MySQL, can set driver properties for timestamp handling
        /*
         * From http://dev.mysql.com/doc/refman/5.5/en/connector-j-reference-configuration-properties.html:
		 *
		 * noTimezoneConversionForTimeType
		 * Don't convert TIME values using the server timezone if 'useTimezone'='true'
		 * Default: false
		 *
		 * serverTimezone
		 * Override detection/mapping of timezone. Used when timezone from 
         * server doesn't map to Java timezone
		 *
		 * useGmtMillisForDatetimes
		 * Convert between session timezone and GMT before creating Date and 
         * Timestamp instances (value of "false" is legacy behavior, "true" 
         * leads to more JDBC-compliant behavior.
		 * Default: false
		 *
		 * useJDBCCompliantTimezoneShift
		 * Should the driver use JDBC-compliant rules when converting 
         * TIME/TIMESTAMP/DATETIME values' timezone information for those JDBC 
         * arguments which take a java.util.Calendar argument? (Notice that 
         * this option is exclusive of the "useTimezone=true" configuration 
         * option.)
		 * Default: false
		 *
		 * useLegacyDatetimeCode
		 * Use code for DATE/TIME/DATETIME/TIMESTAMP handling in result sets 
         * and statements that consistently handles timezone conversions from 
         * client to server and back again, or use the legacy code for these 
         * datatypes that has been in the driver for backwards-compatibility?
		 * Default: true
		 *
		 * useSSPSCompatibleTimezoneShift
		 * If migrating from an environment that was using server-side prepared 
         * statements, and the configuration property 
         * "useJDBCCompliantTimeZoneShift" set to "true", use compatible 
         * behavior when not using server-side prepared statements when sending 
         * TIMESTAMP values to the MySQL server.
		 * Default: false
		 *
		 * useTimezone
		 * Convert time/date types between client and server timezones 
         * (true/false, defaults to 'false')?
		 * Default: false
		 */

        Properties properties = create(PROPERTY_NAME, DatabaseModule.class);

        String host = getProperty("database.url.host", properties);
        String port = getProperty("database.url.port", properties);
        String dbName = getProperty("database.url.dbname", properties);
        String dbArgs = getProperty("database.url.args", properties);

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + dbName + "?" + dbArgs);
        config.setUsername(getProperty("database.user", properties));
        config.setPassword(getProperty("database.password", properties));
        config.setPartitionCount(parseInt(getProperty("database.pool.partitions", properties)));
        config.setMinConnectionsPerPartition(parseInt(getProperty("database.pool.connections.min", properties)));
        config.setMaxConnectionsPerPartition(parseInt(getProperty("database.pool.connections.max", properties)));

        return config;
    }
}
