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
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.IOException;
import java.io.Serializable;

public class Page {

    @ApiModelProperty(value = "The name of the page", required = true)
    private Page.Name name;

    private Page() {
        super();
    }

    public static Builder withName(Name name) {
        return new Builder().withName(name);
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    public static class Builder {

        private Page instance;

        public Builder withName(Name name) {
            instance = new Page();
            instance.name = name;
            return this;
        }

        public Page build() {
            Page result = instance;
            instance = null;
            return result;
        }

    }


    /**
     * Encapsulates the Name of the Page.
     */
    @JsonSerialize(using = Page.Name.Serializer.class)
    @JsonDeserialize(using = Page.Name.Deserializer.class)
    public static class Name implements Serializable {

        private final String inner_name;

        private Name(String name) {
            this.inner_name = Preconditions.checkNotNull(name);

            if (name.trim().isEmpty()) {
                throw new IllegalArgumentException("Page name cannot be " +
                        "an empty string");
            }

            if (name.length() > 256) {
                throw new InvalidIdentifierException("Page name \"" + name +
                        "\" must be 256 characters or less");
            }
        }

        public static Name valueOf(String value) {
            return new Name(value);
        }

        @Override
        public String toString() {
            return inner_name;
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

        public static class Serializer extends JsonSerializer<Name> {
            @Override
            public void serialize(Name name, JsonGenerator generator, SerializerProvider provider)
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
    }
}
