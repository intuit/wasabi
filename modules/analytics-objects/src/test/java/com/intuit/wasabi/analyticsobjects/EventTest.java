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

import com.intuit.wasabi.experimentobjects.Context;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Created on 2/25/16.
 */
public class EventTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testSetPayLoadNotNull() {
        Event event = new Event();
        event.setPayload(Event.Payload.valueOf("one"));
        assertEquals("one", event.getPayload().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetPayLoadNull() {
        Event event = new Event();
        event.setPayload(null);
    }

    @Test
    public void enumTest() {
        Set<String> expected = new HashSet<>(Arrays.asList("IMPRESSION", "BINARY_ACTION"));
        Set<String> actual = new HashSet<>();
        for (Event.Type t : Event.Type.values())
            actual.add(t.name());
        assertEquals(expected, actual);
    }

    @Test
    public void getterAndSetterTest() {
        Context c = Context.newInstance("Test").build();
        Event event = new Event();
        event.setContext(c);
        assertThat(event.getContext(), is(c));

        Event.Name name = Event.Name.valueOf("test name");
        event.setName(name);
        assertThat(event.getName(), is(name));
        assertThat(event.getType(), is(Event.Type.BINARY_ACTION));

        Event.Payload payload = Event.Payload.valueOf("test payload");
        event.setPayload(payload);
        assertThat(event.getPayload(), is(payload));

        Date date = new Date();
        event.setTimestamp(date);
        assertThat(event.getTimestamp(), is(date));

        String value = "test value";
        event.setValue(value);
        assertThat(event.getValue(), is(value));

        name = Event.Name.valueOf("IMPRESSION");
        event.setName(name);
        assertThat(event.getName(), is(name));
        assertThat(event.getType(), is(Event.Type.IMPRESSION));
    }

    @Test
    public void ensureCorrectExceptionThrownOnEmptyPayload() {
        Event event = new Event();
        exception.expect(IllegalArgumentException.class);
        event.setPayload(Event.Payload.valueOf(null));
    }

    @Test
    public void testEqual() {
        Event e1 = new Event();
        Event e2 = e1.clone();
        assertThat(e1, is(e2));
    }

    @Test
    public void testHashCode() {
        Event event = new Event();
        Event event2 = new Event();
        assertThat(event.hashCode(), is(event2.hashCode()));
        assertThat(event.toString(), containsString("Event"));
    }
}
