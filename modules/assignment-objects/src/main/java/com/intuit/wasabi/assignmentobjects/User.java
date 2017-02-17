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
package com.intuit.wasabi.assignmentobjects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.IOException;

/**
 * Represents an end user who's being assigned experiences to for experiments
 */
public interface User {

    /**
     * Encapsulates the label for the user
     */
    @JsonSerialize(using = User.ID.Serializer.class)
    @JsonDeserialize(using = User.ID.Deserializer.class)
    class ID {

        private ID(String id) {
            super();
            this.id = Preconditions.checkNotNull(id);

            // TODO: Add validation
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

        @Override
        public String toString() {
            return id;
        }

        public static ID valueOf(String value) {
            return new ID(value);
        }

        public static class Serializer extends JsonSerializer<ID> {
            @Override
            public void serialize(ID label, JsonGenerator generator, SerializerProvider provider) throws IOException {
                generator.writeString(label.toString());
            }
        }

        public static class Deserializer
                extends JsonDeserializer<ID> {
            @Override
            public ID deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                return ID.valueOf(parser.getText());
            }
        }

        private String id;
    }

}
