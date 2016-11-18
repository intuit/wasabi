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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests the {@link EventList}
 */
public class EventListTest {


    @Test
    public void testBasicOperations() {
        EventList eventList = new EventList();

        assertThat(eventList.getEvents(), is(not(nullValue())));
        assertThat(eventList.getEvents().size(), is(0));

        List<Event> list = new ArrayList<Event>();
        list.add(new Event());
        eventList.setEvents(list);

        assertThat(eventList.getEvents().size(), is(1));

    }

}
