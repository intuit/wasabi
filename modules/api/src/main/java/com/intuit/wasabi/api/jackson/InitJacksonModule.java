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

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.intuit.wasabi.experimentobjects.Experiment.State;

import java.sql.Timestamp;

/**
 * Jackson Initializer for double, float, timestamp and State
 *
 * @see State
 * @see NaNSerializerFloat
 * @see NaNSerializerDouble
 * @see ExperimentStateDeserializer
 * @see SQLTimestampDeserializer
 * @see UpperCaseToStringSerializer
 */
public class InitJacksonModule extends SimpleModule {

    public InitJacksonModule() {
        addSerializer(Double.class, new NaNSerializerDouble());
        addSerializer(Float.class, new NaNSerializerFloat());
        addDeserializer(Timestamp.class, new SQLTimestampDeserializer());
        addDeserializer(State.class, new ExperimentStateDeserializer());
        addSerializer(new UpperCaseToStringSerializer<>(State.class));
    }
}
