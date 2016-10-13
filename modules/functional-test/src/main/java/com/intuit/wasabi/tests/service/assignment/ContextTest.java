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
package com.intuit.wasabi.tests.service.assignment;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.model.Assignment;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Event;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.User;
import com.intuit.wasabi.tests.model.factory.BucketFactory;
import com.intuit.wasabi.tests.model.factory.EventFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import com.intuit.wasabi.tests.model.factory.UserFactory;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intuit.wasabi.tests.library.util.Constants.ASSIGNMENT_EXISTING_ASSIGNMENT;
import static com.intuit.wasabi.tests.library.util.Constants.ASSIGNMENT_EXPERIMENT_IN_DRAFT_STATE;
import static com.intuit.wasabi.tests.library.util.Constants.EXPERIMENT_STATE_RUNNING;
import static com.intuit.wasabi.tests.library.util.Constants.NEW_ASSIGNMENT;
import static org.testng.Assert.assertEquals;

/**
 * Context integration test
 */
public class ContextTest extends TestBase {

    private Experiment experiment;
    private List<Bucket> buckets = new ArrayList<>();
    private String[] labels = {"blue", "white"};
    private double[] allocations = {.5, .5};
    private boolean[] control = {false, true};

    private String contextFoo = "FOO";
    private String contextBar = "BAR";
    private String contextProd = "PROD";

    private String[] contexts = new String[]{contextFoo, contextBar, contextProd};

    private User userBill = UserFactory.createUser("Bill");
    private User userJane = UserFactory.createUser("Jane");
    private User userTom = UserFactory.createUser("Tom");

    private User[] users = {userBill, userJane, userTom};

    private String actionImpression = "IMPRESSION";
    private String actionClick = "click";
    private String actionLike = "like";

    @BeforeClass
    public void setup() {
        setResponseLogLengthLimit(1000);

        experiment = ExperimentFactory.createExperiment();
        experiment.samplingPercent = 1;
        DefaultNameExclusionStrategy experimentComparisonStrategy = new DefaultNameExclusionStrategy("creationTime", "modificationTime", "ruleJson");
        experiment.setSerializationStrategy(experimentComparisonStrategy);
    }

    @Test(dependsOnGroups = {"ping"})
    public void assignContextWithExperimentInDraftState() {
        Experiment exp = postExperiment(experiment);
        experiment.update(exp);
        buckets = BucketFactory.createCompleteBuckets(experiment, allocations, labels, control);
        List<Bucket> resultBuckets = postBuckets(buckets);

        Assert.assertEquals(buckets, resultBuckets);

        for (String context : contexts) {
            Assignment result = postAssignment(experiment, userBill, context);
            assertEquals(result.status, ASSIGNMENT_EXPERIMENT_IN_DRAFT_STATE);
        }

    }

    @Test(dependsOnMethods = {"assignContextWithExperimentInDraftState"})
    public void assignWithoutContextExperimentInDraftState() {
        for (User user : users) {
            Assignment result = postAssignment(experiment, user);
            assertEquals(result.status, ASSIGNMENT_EXPERIMENT_IN_DRAFT_STATE);
        }
    }

    @Test(dependsOnMethods = {"assignWithoutContextExperimentInDraftState"})
    public void experimentInRunningState() {
        experiment.state = EXPERIMENT_STATE_RUNNING;
        experiment = putExperiment(experiment);
    }

    @Test(dependsOnMethods = {"experimentInRunningState"})
    public void assignWithoutContextExperimentInRunningState() {
        for (User user : users) {
            Assignment result = postAssignment(experiment, user);
            assertEquals(result.status, NEW_ASSIGNMENT);
            assertEquals(result.context, contexts[2]);
        }
    }

    @Test(dependsOnMethods = {"assignWithoutContextExperimentInRunningState"})
    public void assignAgainWithoutContextExperimentInRunningState() {
        for (User user : users) {
            Assignment result = postAssignment(experiment, user);
            assertEquals(result.status, ASSIGNMENT_EXISTING_ASSIGNMENT);
        }
    }

    @Test(dependsOnMethods = {"experimentInRunningState"})
    public void assignWithContextFooExperimentInRunningState() {

        for (User user : users) {
            Assignment result = postAssignment(experiment, user, contexts[0]);
            assertEquals(result.status, NEW_ASSIGNMENT);
        }
    }

    @Test(dependsOnMethods = {"experimentInRunningState"})
    public void assignWithContextBarExperimentInRunningState() {

        for (User user : users) {
            Assignment result = postAssignment(experiment, user, contexts[1]);
            assertEquals(result.status, NEW_ASSIGNMENT);
        }
    }

    @Test(dependsOnMethods = {"assignWithContextFooExperimentInRunningState"})
    public void assignAgainWithContextFooExperimentInRunningState() {

        for (User user : users) {
            Assignment result = postAssignment(experiment, user, contexts[0]);
            assertEquals(result.status, ASSIGNMENT_EXISTING_ASSIGNMENT);
        }
    }

    @Test(dependsOnMethods = {"assignWithContextBarExperimentInRunningState"})
    public void assignAgainWithContextBarExperimentInRunningState() {

        for (User user : users) {
            Assignment result = postAssignment(experiment, user, contexts[1]);
            assertEquals(result.status, ASSIGNMENT_EXISTING_ASSIGNMENT);
        }
    }

    @Test(dependsOnMethods = {"assignWithContextBarExperimentInRunningState"})
    public void postImpressionEventInBarExperimentInRunningState() {
        for (User user : users) {
            Event event = EventFactory.createEvent();
            event.context = contextBar;
            event.name = actionImpression;
            Response result = postEvent(event, experiment, user, HttpStatus.SC_CREATED);
            assertEquals(result.getStatusCode(), HttpStatus.SC_CREATED);
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("fromTime", "");
        List<Event> events = postEvents(experiment,
                parameters, true,
                HttpStatus.SC_OK, apiServerConnector);

        assertEquals(events.size(), 3);
        for (Event event : events) {
            assertEquals(event.name, actionImpression);
        }
    }

    @Test(dependsOnMethods = {"postImpressionEventInBarExperimentInRunningState"})
    public void post2ClickEventInFooExperimentInRunningState() {
        for (int i = 0; i < 2; i++) {
            User user = users[i];
            Event event = EventFactory.createEvent();
            event.context = contextBar;
            event.name = actionClick;
            Response result = postEvent(event, experiment, user, HttpStatus.SC_CREATED);
            assertEquals(result.getStatusCode(), HttpStatus.SC_CREATED);
        }
        List<Event> events = postEvents(experiment);

        assertEquals(events.size(), 5);
        int clickCount = 0;
        int impressionCount = 0;
        for (Event event : events) {
            if (event.name.equals(actionImpression))
                impressionCount++;
            else if (event.name.equals(actionClick))
                clickCount++;
        }
        assertEquals(impressionCount, 3);
        assertEquals(clickCount, 2);
    }

    @Test(dependsOnMethods = {"postImpressionEventInBarExperimentInRunningState"})
    public void post2LikeEventWithoutContextExperimentInRunningState() {
        for (int i = 0; i < 2; i++) {
            User user = users[i];
            Event event = EventFactory.createEvent();
            event.name = actionLike;
            Response result = postEvent(event, experiment, user, HttpStatus.SC_CREATED);
            assertEquals(result.getStatusCode(), HttpStatus.SC_CREATED);
        }
        List<Event> events = postEvents(experiment);

        assertEquals(events.size(), 7);
        int clickCount = 0;
        int likeCount = 0;
        int impressionCount = 0;
        for (Event event : events) {
            if (event.name.equals(actionImpression))
                impressionCount++;
            else if (event.name.equals(actionClick))
                clickCount++;
            else if (event.name.equals(actionLike))
                likeCount++;
        }
        assertEquals(impressionCount, 3);
        assertEquals(clickCount, 2);
        assertEquals(likeCount, 2);
    }

    @Test(dependsOnMethods = {"postImpressionEventInBarExperimentInRunningState"})
    public void postLikeAndClickEventsWithoutContextExperimentInRunningState() {
        for (int i = 0; i < 2; i++) {
            User user = users[i];
            Event eventLike = EventFactory.createEvent();
            eventLike.name = actionLike;
            Event eventClick = EventFactory.createEvent();
            eventClick.name = actionClick;
            List<Event> events = new ArrayList<>();
            events.add(eventLike);
            events.add(eventClick);
            Response result = postEvents(events, experiment, user, HttpStatus.SC_CREATED);
            assertEquals(result.getStatusCode(), HttpStatus.SC_CREATED);
        }
        List<Event> events = postEvents(experiment);

        assertEquals(events.size(), 15);
        int clickCount = 0;
        int likeCount = 0;
        int impressionCount = 0;
        for (Event event : events) {
            if (event.name.equals(actionImpression))
                impressionCount++;
            else if (event.name.equals(actionClick))
                clickCount++;
            else if (event.name.equals(actionLike))
                likeCount++;
        }
        assertEquals(impressionCount, 3);
        assertEquals(clickCount, 6);
        assertEquals(likeCount, 6);
    }

    @Test(dependsOnMethods = {"postImpressionEventInBarExperimentInRunningState"})
    public void postLikeAndClickEventsWithBarContextExperimentInRunningState() {
        for (int i = 0; i < 2; i++) {
            User user = users[i];
            Event eventLike = EventFactory.createEvent();
            eventLike.name = actionLike;
            eventLike.context = contextBar;
            Event eventClick = EventFactory.createEvent();
            eventClick.name = actionClick;
            eventClick.context = contextBar;
            List<Event> events = new ArrayList<>();
            events.add(eventLike);
            events.add(eventClick);
            Response result = postEvents(events, experiment, user, HttpStatus.SC_CREATED);
            assertEquals(result.getStatusCode(), HttpStatus.SC_CREATED);
        }
        List<Event> events = postEvents(experiment);

        assertEquals(events.size(), 11);
        int clickCount = 0;
        int likeCount = 0;
        int impressionCount = 0;
        for (Event event : events) {
            if (event.name.equals(actionImpression))
                impressionCount++;
            else if (event.name.equals(actionClick))
                clickCount++;
            else if (event.name.equals(actionLike))
                likeCount++;
        }
        assertEquals(impressionCount, 3);
        assertEquals(clickCount, 4);
        assertEquals(likeCount, 4);
    }

    @AfterClass
    public void terminateAndDeleteExperiment() {
        toCleanUp.add(experiment);
        cleanUpExperiments();
    }

}
