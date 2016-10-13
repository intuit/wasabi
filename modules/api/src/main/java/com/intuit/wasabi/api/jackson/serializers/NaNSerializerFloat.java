/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.api.jackson.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Custom serializer that parses Float.NaN and Float.Infinity to null in the Json
 */
public class NaNSerializerFloat extends JsonSerializer<Float> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(Float t, JsonGenerator jg, SerializerProvider sp) throws IOException {
        if (t.isInfinite() || t.isNaN()) {
            jg.writeNumber("null");
        } else {
            jg.writeNumber(t);
        }
    }
}
