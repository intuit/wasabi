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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.intuit.wasabi.experimentobjects.Experiment.State;

import java.io.IOException;

/**
 * Experiment state deserializer
 *
 * @see State
 */

public class ExperimentStateDeserializer extends JsonDeserializer<State> {

    /**
     * {@inheritDoc}
     */
    @Override
    public State deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        return State.valueOf(jp.getValueAsString().toUpperCase());
    }
}
