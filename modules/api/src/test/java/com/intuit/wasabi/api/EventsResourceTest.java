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
package com.intuit.wasabi.api;

import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.analyticsobjects.Event.Name;
import com.intuit.wasabi.analyticsobjects.EventList;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.events.Events;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class EventsResourceTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    private Events events;
    private EventsResource resource;
    private EventList eventList = new EventList();
    private Application.Name applicationName = Application.Name.valueOf("testApp");
    private Experiment.Label experimentLabel = Experiment.Label.valueOf("testExp");
    private User.ID userID = User.ID.valueOf("12345");

    @Before
    public void setUp() throws Exception {
        resource = new EventsResource(events, new HttpHeader("App-???", "600"));
    }

    @Test
    public void recordEventsNameNull() throws Exception {
        List<Event> listOfEvents = new ArrayList<>();
        listOfEvents.add(new Event());
        eventList.setEvents(listOfEvents);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Event name cannot be null or an empty string");
        resource.recordEvents(applicationName, experimentLabel, userID, eventList);
    }

    @Test
    public void recordEventsNameEmpty() throws Exception {
        List<Event> listOfEvents = new ArrayList<>();
        Event e = new Event();
        e.setName(Name.valueOf(""));
        listOfEvents.add(e);
        eventList.setEvents(listOfEvents);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Event name cannot be null or an empty string");
        resource.recordEvents(applicationName, experimentLabel, userID, eventList);
    }

    @Test
    public void recordEvents() throws Exception {
        List<Event> listOfEvents = new ArrayList<>();
        Event e = new Event();
        e.setName(Name.valueOf("someEventName"));
        listOfEvents.add(e);
        eventList.setEvents(listOfEvents);

        resource.recordEvents(applicationName, experimentLabel, userID, eventList);
        verify(events).recordEvents(any(Application.Name.class),
                any(Experiment.Label.class), any(User.ID.class), any(EventList.class), any(Set.class));
    }

    @Test
    public void recordUsersEvents() throws UnsupportedOperationException {
        Map<User.ID, List<Event>> eventList = new HashMap<>();

        thrown.expect(UnsupportedOperationException.class);
        thrown.expectMessage("Not implemented");
        resource.recordUsersEvents(applicationName, experimentLabel, eventList);
    }

    @Test
    public void recordExperimentsEvents() throws Exception {
        Map<Experiment.Label, Map<User.ID, List<Event>>> eventList = new HashMap<>();

        thrown.expect(UnsupportedOperationException.class);
        thrown.expectMessage("Not implemented");
        resource.recordExperimentsEvents(applicationName, eventList);
    }

    @Test
    public void getEventsQueueLength() throws Exception {
        assertThat(resource.getEventsQueueLength().getStatus(), is(HttpStatus.SC_OK));
    }
}
