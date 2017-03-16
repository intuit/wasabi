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
package com.intuit.wasabi.api.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.AbstractModule;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.slf4j.Logger;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;
import static org.slf4j.LoggerFactory.getLogger;

public class JacksonModule extends AbstractModule {

    private static final String ISO8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXX";
    private static final Logger LOGGER = getLogger(JacksonModule.class);

    @Override
    protected void configure() {
        LOGGER.debug("installing module: {}", JacksonModule.class.getSimpleName());

        bind(JacksonJsonProvider.class).toInstance(createJacksonJsonProvider());

        LOGGER.debug("installed module: {}", JacksonModule.class.getSimpleName());
    }

    private JacksonJsonProvider createJacksonJsonProvider() {
        ObjectMapper mapper = new ObjectMapper();

        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setSerializationInclusion(ALWAYS);
        mapper.registerModule(new SimpleModule() {
            {
                addSerializer(Double.class, new NaNSerializerDouble());
                addSerializer(Float.class, new NaNSerializerFloat());
                addDeserializer(Timestamp.class, new SQLTimestampDeserializer());
                addDeserializer(Experiment.State.class, new ExperimentStateDeserializer());
                addSerializer(new UpperCaseToStringSerializer<>(Experiment.State.class));
            }
        });

        SimpleDateFormat iso8601Formatter = new SimpleDateFormat(ISO8601_DATE_FORMAT);
        iso8601Formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        mapper.setDateFormat(iso8601Formatter);

        JacksonJsonProvider provider = new WasabiJacksonJsonProvider();

        provider.setMapper(mapper);

        return provider;
    }
}
