package com.intuit.wasabi.analyticsobjects;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created on 4/14/16.
 */
public class EventListTest {


    @Test
    public void testBasicOperations(){
        EventList eventList = new EventList();

        assertThat(eventList.getEvents(), is(not(nullValue())));
        assertThat(eventList.getEvents().size(), is(0));

        List<Event> list = new ArrayList<Event>();
        list.add(new Event());
        eventList.setEvents( list );

        assertThat(eventList.getEvents().size(), is(1));

    }

}
