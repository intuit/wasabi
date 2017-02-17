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
package com.intuit.wasabi.experimentobjects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import com.intuit.wasabi.experimentobjects.exceptions.InvalidIdentifierException;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

/**
 * An application interface to wrap a nested Name class
 */
public interface Application {

    @JsonSerialize(using = Application.Name.Serializer.class)
    @JsonDeserialize(using = Application.Name.Deserializer.class)
    class Name implements Serializable {

        private Name(String name) {
            super();
            this.name = Preconditions.checkNotNull(name);
            if (!name.matches("^[_\\-$A-Za-z][_\\-$A-Za-z0-9]*")) {
                throw new InvalidIdentifierException("Application name \"" +
                        name + "\" must begin with a letter, dollar sign, or " +
                        "underscore and may not contain any spaces");
            }
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 11 * hash + Objects.hashCode(this.name);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

        @Override
        public String toString() {
            return name;
        }

        public static Name valueOf(String value) {
            return new Name(value);
        }

        public static class Serializer extends JsonSerializer<Name> {
            @Override
            public void serialize(Name name, JsonGenerator generator,
                                  SerializerProvider provider)
                    throws IOException {
                generator.writeString(name.toString());
            }
        }

        public static class Deserializer extends JsonDeserializer<Name> {
            @Override
            public Name deserialize(JsonParser parser, DeserializationContext context)
                    throws IOException {
                return Name.valueOf(parser.getText());
            }
        }

        private String name;
    }

}
