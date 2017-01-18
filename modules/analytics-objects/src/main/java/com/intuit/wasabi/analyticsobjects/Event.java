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
package com.intuit.wasabi.analyticsobjects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import com.intuit.wasabi.exceptions.AnalyticsException;
import com.intuit.wasabi.experimentobjects.Context;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.IOException;
import java.util.Date;

/**
 * An event represents an users interaction with the experiment. When they are presented
 * with the A/B Test on a page for example this triggers the recording of an Event with
 * the name IMPRESSION.
 */
public class Event implements Cloneable {

    public static final String IMPRESSION = "IMPRESSION";
    String emptyString = "";
    @ApiModelProperty(value = "time at which the event occurred; defaults to the current time", hidden = true)
    private Date timestamp;
    @ApiModelProperty(value = "DO NOT USE", hidden = true)
    private Type type;
    @ApiModelProperty(example = "IMPRESSION OR myEventName", value = "Event ID; Use \"IMPRESSION\" for impressions", dataType = "String", required = true)
    private Event.Name name;
    @ApiModelProperty(value = "context for the event, eg \"PROD\", \"QA\"", dataType = "String", required = false, hidden = true)
    private Context context = Context.valueOf("PROD");
    @ApiModelProperty(value = "payload for the event; defaults to null", dataType = "String")
    private Payload payload;
    @ApiModelProperty(value = "not yet implemented", hidden = true)
    private String value;

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date value) {
        timestamp = new Date(value.getTime());
    }

    public Type getType() {
        return type;
    }

    public Event.Name getName() {
        return name;
    }

    public void setName(Event.Name value) {
        name = value;
        if (name.toString().equals(IMPRESSION)) {
            type = Type.IMPRESSION;
        } else { //todo: for other event types, will need to check this.value
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
        if (value == null || value.toString() == null) {
            throw new IllegalArgumentException("Invalid value for payload. Payload should not be null.");
        } else if (value.toString().length() > 4096) {
            throw new IllegalArgumentException(
                    String.format("Invalid value for payload \"%s\". Payload should be 4096 characters or less",
                            value));
        }
        payload = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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
        return "Event={\"timestamp\":\"" + (timestamp != null ? timestamp : emptyString) + "\"" +
                ",\"type\":\"" + (type != null ? type.toString() : emptyString) + "\"" +
                ",\"name\":\"" + (name != null ? name.toString() : emptyString) + "\"" +
                ",\"context\":\"" + (context != null ? context.toString() : emptyString) + "\"" +
                ",\"payload\":\"" + (payload != null ? payload.toString() : emptyString) + "\"" +
                ",\"value\":\"" + (value != null ? value : emptyString) + "\"}";
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
            super();
            this.name = Preconditions.checkNotNull(name);
        }

        public static Name valueOf(String value) {
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


    /**
     * Encapsulates the payload for the event
     */
    @JsonSerialize(using = Event.Payload.Serializer.class)
    @JsonDeserialize(using = Event.Payload.Deserializer.class)
    public static class Payload {

        private String payload;

        private Payload(String payload) {
            super();
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
