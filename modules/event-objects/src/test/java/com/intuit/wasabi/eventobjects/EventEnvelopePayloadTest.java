package com.intuit.wasabi.eventobjects;

import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;

public class EventEnvelopePayloadTest {

    Assignment assignment = Assignment.newInstance(Experiment.ID.newInstance())
            .withUserID(User.ID.valueOf("TestUser"))
            .withContext(Context.valueOf("PROD"))
            .withBucketLabel(Bucket.Label.valueOf("blue"))
            .build();

    Event event = getEvent();
    Application.Name name = Application.Name.valueOf("a1");
    Experiment.Label label = Experiment.Label.valueOf("l1");

    private Event getEvent() {
        Event evt = new Event();
        evt.setPayload(Event.Payload.valueOf("SomePayload{}\\\\!@#\"\"withEscaping"));
        evt.setTimestamp(new Date());
        evt.setName(Event.Name.valueOf("CLICKED_BUTTON"));
        return evt;
    }

    @Test
    @Ignore("Just creates debug output.")
    public void output() {
        System.out.println(new EventEnvelopePayload(name, label, assignment, event).toJson());
    }


}
