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
package com.intuit.wasabi.analyticsobjects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.intuit.wasabi.exceptions.AnalyticsException;
import com.intuit.wasabi.experimentobjects.Context;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

/**
 * An event represents an users interaction with the experiment. When they are presented
 * with the A/B Test on a page for example this triggers the recording of an Event with
 * the name IMPRESSION.
 */
public class Event implements Cloneable {

    private static final String IMPRESSION = "IMPRESSION";
    @ApiModelProperty(value = "time at which the event occurred; defaults to the current time", hidden = true)
    private Date timestamp = new Date();
    @ApiModelProperty(value = "DO NOT USE", hidden = true)
    private Type type = Type.IMPRESSION;
    @ApiModelProperty(example = "IMPRESSION OR myEventName", value = "Event ID; Use \"IMPRESSION\" for impressions", dataType = "String", required = true)
    private Event.Name name = Event.Name.valueOf(IMPRESSION);
    @ApiModelProperty(value = "context for the event, e.g. \"PROD\", \"QA\"; Defaults to \"PROD\".", dataType = "String", required = false, hidden = true)
    private Context context = Context.valueOf("PROD");
    @ApiModelProperty(value = "payload for the event; defaults to an empty String.", dataType = "String")
    private Payload payload = Payload.valueOf("");

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date value) {
        if (Objects.isNull(value)) {
            throw new IllegalArgumentException("Event timestamp must not be null.");
        }
        timestamp = new Date(value.getTime());
    }

    public Type getType() {
        return type;
    }

    public Event.Name getName() {
        return name;
    }

    public void setName(Event.Name value) {
        if (Objects.isNull(value)) {
            throw new IllegalArgumentException("Event.Name must not be null.");
        }
        name = value;
        switch (name.toString()) {
            case IMPRESSION:
                type = Type.IMPRESSION;
                break;
            default:
                type = Type.BINARY_ACTION;
        }
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context value) {
        context = value;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload value) {
        if (Objects.isNull(value)) {
            throw new IllegalArgumentException("Invalid value for payload. Payload must not be null.");
        }
        payload = value;
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
    public Event clone() {
        try {
            return (Event) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AnalyticsException("Event clone not supported: " + e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public enum Type {
        IMPRESSION,
        BINARY_ACTION
    }

    /**
     * Encapsulates the name for the event
     */
    @JsonSerialize(using = Event.Name.Serializer.class)
    @JsonDeserialize(using = Event.Name.Deserializer.class)
    public static class Name {

        private String name;

        private Name(String name) {
            this.name = name;
        }

        public static Name valueOf(String value) {
            if (StringUtils.isBlank(value)) {
                throw new IllegalArgumentException("Event name must not be null or empty.");
            }
            return new Name(value);
        }

        @Override
        public String toString() {
            return name;
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
            public void serialize(Name name, JsonGenerator generator,
                                  SerializerProvider provider)
                    throws IOException {
                generator.writeString(name.toString());
            }
        }

        public static class Deserializer
                extends JsonDeserializer<Name> {
            @Override
            public Name deserialize(JsonParser parser,
                                    DeserializationContext context)
                    throws IOException {
                return Name.valueOf(parser.getText());
            }
        }
    }


    @JsonSerialize(using = Event.Payload.Serializer.class)
    @JsonDeserialize(using = Event.Payload.Deserializer.class)
    public static class Payload {

        private String payload;

        private Payload(String payload) {
            if (Objects.isNull(payload)) {
                throw new IllegalArgumentException("Payload must not be null.");
            }
            if (payload.length() > 4096) {
                throw new IllegalArgumentException(
                        String.format("Invalid value for payload \"%s\". Payload should be 4096 characters or less",
                                this.payload));
            }
            this.payload = payload;
        }

        public static Payload valueOf(String value) {
            return new Payload(value);
        }

        @Override
        public String toString() {
            return payload;
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

        public static class Serializer extends JsonSerializer<Payload> {
            @Override
            public void serialize(Payload payload, JsonGenerator generator,
                                  SerializerProvider provider)
                    throws IOException {
                generator.writeString(payload.toString());
            }
        }

        public static class Deserializer
                extends JsonDeserializer<Payload> {
            @Override
            public Payload deserialize(JsonParser parser,
                                       DeserializationContext context)
                    throws IOException {
                return Payload.valueOf(parser.getText());
            }
        }
    }
}
