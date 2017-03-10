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

import java.io.IOException;
import java.sql.Timestamp;

import static java.lang.String.format;

/**
 * This class deserializes string to {@link Timestamp} if possible.
 */
public class SQLTimestampDeserializer extends JsonDeserializer<Timestamp> {

    /*
     * (non-Javadoc)
     * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
     *
     * @throws IllegalArgumentException if argument is not legal
     */
    @Override
    public Timestamp deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String token = parser.getText();

        try {
            return Timestamp.valueOf(token);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(format("Invalid timestamp \"%s\"", token), e);
        }
    }
}
