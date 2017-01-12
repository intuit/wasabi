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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test for the {@link com.intuit.wasabi.analyticsobjects.Event.Name}
 */
public class EventNameTest {

    protected ObjectMapper mapper;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Test
    public void payloadSerialization() throws IOException {
        Event.Name name = Event.Name.valueOf("some name");
        StringWriter stringJson = new StringWriter();
        JsonGenerator generator = new JsonFactory().createGenerator(stringJson);
        mapper.writeValue(generator, name);
        assertThat(stringJson.toString(), is("\"" + name.toString() + "\""));
    }

    @Test
    public void payloadDeSerialization() throws IOException {
        Event.Name expected = Event.Name.valueOf("some name");
        Event.Name name = mapper.readValue("\"some name\"", Event.Name.class);
        assertThat(name, is(expected));
    }

    @Test
    public void equalsTest() {
        Event.Name expected = Event.Name.valueOf("some name");
        boolean result = expected.equals("some name");
        assertThat(result, is(false));
    }

    @Test
    public void hashCodeTest() {
        Event.Payload expected = Event.Payload.valueOf("some name");
        assertThat(expected.hashCode(), is(-642289492));
    }

}
