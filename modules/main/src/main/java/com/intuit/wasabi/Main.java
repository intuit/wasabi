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
package com.intuit.wasabi;

import com.intuit.autumn.metrics.MetricsModule;
import com.intuit.autumn.service.ServiceManager;
import com.intuit.wasabi.api.ApiModule;
import com.intuit.wasabi.eventlog.EventLogService;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;

import java.util.TimeZone;

import static com.intuit.autumn.metrics.MetricsServices.getEnabledMetricsServices;
import static com.intuit.autumn.web.WebServices.getEnabledWebServices;
import static java.util.TimeZone.getTimeZone;
import static org.joda.time.DateTimeZone.UTC;
import static org.slf4j.LoggerFactory.getLogger;

public class Main {

    private static final Logger LOGGER = getLogger(MetricsModule.class);

    // Stop people from accidentally create a new Main Object
    private Main() {
    }

    /**
     * Application entry point.
     *
     * @param args application arguments
     * @throws Exception unintended exception
     */

    public static void main(String[] args) throws Exception {
        LOGGER.info("starting {}", Main.class.getSimpleName());

        TimeZone.setDefault(getTimeZone("UTC"));
        DateTimeZone.setDefault(UTC);

        ServiceManager serviceManager = new ServiceManager()
                .addModules(ApiModule.class, MetricsModule.class)
                .addServices(getEnabledWebServices())
                .addServices(getEnabledMetricsServices())
                .addServices(EventLogService.class);

        serviceManager.start();

        LOGGER.info("started {}", Main.class.getSimpleName());
    }
}
