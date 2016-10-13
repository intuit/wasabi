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
package com.intuit.wasabi.api;

import com.intuit.wasabi.api.pagination.PaginationHelper;
import com.intuit.wasabi.api.pagination.comparators.impl.ExperimentComparator;
import com.intuit.wasabi.api.pagination.filters.impl.ExperimentFilter;
import com.intuit.wasabi.assignment.Assignments;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authenticationobjects.exceptions.AuthenticationException;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.authorizationobjects.Permission;
import com.intuit.wasabi.events.EventsExport;
import com.intuit.wasabi.exceptions.TimeFormatException;
import com.intuit.wasabi.exceptions.TimeZoneFormatException;
import com.intuit.wasabi.experiment.Buckets;
import com.intuit.wasabi.experiment.Experiments;
import com.intuit.wasabi.experiment.Favorites;
import com.intuit.wasabi.experiment.Mutex;
import com.intuit.wasabi.experiment.Pages;
import com.intuit.wasabi.experiment.Priorities;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.experimentobjects.ExperimentIDList;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.ExperimentPageList;
import com.intuit.wasabi.experimentobjects.NewExperiment;
import com.intuit.wasabi.experimentobjects.Page;
import com.intuit.wasabi.experimentobjects.exception.BucketNotFoundException;
import com.intuit.wasabi.experimentobjects.exception.ExperimentNotFoundException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static java.nio.charset.Charset.forName;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.UriBuilder.fromPath;
import static org.apache.commons.codec.binary.Base64.encodeBase64;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExperimentsResourceTest {

    private static final String USERPASS = new String(encodeBase64("admin@example.com:admin01".getBytes(forName("UTF-8"))), forName("UTF-8"));
    private static final String AUTHHEADER = "Basic: " + USERPASS;
    private static final UserInfo.Username USER = UserInfo.Username.valueOf("admin@example.com");
    private static final UserInfo USERINFO = UserInfo.from(USER).build();
    private static final UserInfo.Username TESTUSER = UserInfo.Username.valueOf("test_user");
    private static final Application.Name TESTAPP = Application.Name.valueOf("test_app");
    private static final Application.Name TESTAPP2 = Application.Name.valueOf("test_app2");
    private static final Page.Name TESTPAGE = Page.Name.valueOf("test_page");
    private static final UUID EXPERIMENT_ID = randomUUID();
    private static final String PATH = "http://somewhere:7979/foo/experiment/" + EXPERIMENT_ID + "/bucket";
    private final Date date = new Date();
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    private UriInfo uriInfo;
    @Mock
    private Experiments experiments;
    @Mock
    private EventsExport eventsExport;
    @Mock
    private Assignments assignments;
    @Mock
    private Authorization authorization;
    @Mock
    private Mutex mutex;
    @Mock
    private Pages pages;
    @Mock
    private Priorities priorities;
    @Mock
    private Buckets buckets;
    @Mock
    private Favorites favorites;
    @Mock
    private Context context;
    private Experiment experiment;
    private Bucket bucket;
    private String ignoreStringNullBucket = "false";
    private String fromStringDate = "1970-00-00 00:00:00";
    private String toStringDate = "2040-05-10 18:03:39";
    private String timeZoneString = "UTC";
    private String description = "Example hypothesis.";

    private PaginationHelper<Experiment> paginationHelper = new PaginationHelper<>(
            new ExperimentFilter(), new ExperimentComparator());

    @Before
    public void setup() {
        experiment = Experiment.withID(Experiment.ID.newInstance())
                .withApplicationName(TESTAPP)
                .withStartTime(new Date())
                .withEndTime(new Date())
                .withState(Experiment.State.DRAFT)
                .withDescription(description)
                .build();

        bucket = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("foo"))
                .withAllocationPercent(.5)
                .withControl(false)
                .withDescription("")
                .withPayload("")
                .build();

        doReturn(Collections.<Experiment.ID>emptyList()).when(favorites).getFavorites(Mockito.any());
    }

    @Test
    public void getExperiments() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD",
                new HttpHeader("MyApp-???"), paginationHelper);

        Experiment experiment1 = Experiment.withID(Experiment.ID.newInstance())
                .withApplicationName(TESTAPP)
                .withStartTime(date)
                .withEndTime(date)
                .withState(Experiment.State.DRAFT)
                .build();

        Experiment experiment2 = Experiment.withID(Experiment.ID.newInstance())
                .withApplicationName(TESTAPP2)
                .withStartTime(date)
                .withEndTime(date)
                .withState(Experiment.State.DRAFT)
                .build();

        ExperimentList experimentList = new ExperimentList();
        experimentList.addExperiment(experiment);
        experimentList.addExperiment(experiment1);
        experimentList.addExperiment(experiment2);

        when(experiments.getExperiments()).thenReturn(experimentList);

        Response response = experimentsResource.getExperiments(AUTHHEADER, 1, 10, "", "", "");

        List responseList = Collections.EMPTY_LIST;
        if (response.getEntity() instanceof HashMap) {
            if (((HashMap) response.getEntity()).get("experiments") instanceof List) {
                responseList = (List) ((HashMap) response.getEntity()).get("experiments");
            }
        }
        assert experimentList.getExperiments().containsAll(responseList);
        assert experimentList.getExperiments().size() == responseList.size();

        // fewer allowed experiments
        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);

        //this throw is so that only the allowed (TESTAPP) experiments get returned
        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, TESTAPP2, Permission.READ);

        response = experimentsResource.getExperiments(AUTHHEADER, 1, 10, "", "", "");

        responseList = Collections.EMPTY_LIST;
        if (response.getEntity() instanceof HashMap) {
            if (((HashMap) response.getEntity()).get("experiments") instanceof List) {
                responseList = (List) ((HashMap) response.getEntity()).get("experiments");
            }
        }
        assert experimentList.getExperiments().containsAll(responseList);
        assert 2 == responseList.size();
        assert !responseList.contains(experiment2);
    }

    @Test
    public void testGetExperiments_NullAuth() throws Exception {
        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD",
                new HttpHeader("MyApp-???"), paginationHelper);

        thrown.expect(AuthenticationException.class);
        experimentsResource.getExperiments(null, 1, 10, "", "", "");
    }

    @Test
    public void testGetExperiments_NullExperiment() throws Exception {
        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD",
                new HttpHeader("MyApp-???"), paginationHelper);

        ExperimentList experimentList = new ExperimentList();
        experimentList.addExperiment(null);
        experimentList.addExperiment(experiment);
        when(experiments.getExperiments()).thenReturn(experimentList);

        Response response = experimentsResource.getExperiments(AUTHHEADER, 1, 10, "", "", "");
        if (response.getEntity() instanceof HashMap) {
            if (((HashMap) response.getEntity()).get("experiments") instanceof List) {
                assertThat("Experiment was not included in list.",
                        ((List) ((HashMap) response.getEntity()).get("experiments")).contains(experiment));
                assertEquals("Null experiment was not skipped.", 1,
                        ((List) ((HashMap) response.getEntity()).get("experiments")).size());
            }
        }

    }

    @Test
    public void postExperiment() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        NewExperiment newExperiment = NewExperiment.withID(Experiment.ID.newInstance())
                .withAppName(TESTAPP)
                .withLabel(Experiment.Label.valueOf("label"))
                .withStartTime(date)
                .withEndTime(date)
                .withSamplingPercent(.90)
                .withDescription(description)
                .build();
        try {
            newExperiment.setApplicationName(null);
            experimentsResource.postExperiment(newExperiment, false, AUTHHEADER);
            fail();
        } catch (IllegalArgumentException ignored) {
        }

        newExperiment.setApplicationName(TESTAPP);
        doThrow(AuthenticationException.class).when(authorization).getUser(null);
        try {
            experimentsResource.postExperiment(newExperiment, false, null);
            fail();
        } catch (AuthenticationException ignored) {
        }

        Experiment experiment1 = Experiment.withID(newExperiment.getID())
                .withApplicationName(newExperiment.getApplicationName())
                .withEndTime(newExperiment.getEndTime())
                .withStartTime(newExperiment.getStartTime())
                .withLabel(newExperiment.getLabel())
                .build();

        when(experiments.getExperiment(newExperiment.getID())).thenReturn(experiment1);
        Response response = experimentsResource.postExperiment(newExperiment, false, AUTHHEADER);
        assert (experiment1.equals(response.getEntity()));

        // When user(TESTUSER) doesn't have create permissions we throw an exception
        when(authorization.getUser(AUTHHEADER)).thenReturn(TESTUSER);
        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(TESTUSER, TESTAPP, Permission.CREATE);
        try {
            experimentsResource.postExperiment(newExperiment, false, null);
            fail();
        } catch (AuthenticationException ignored) {
        }

        // When user(TESTUSER) doesn't have create permissions but flags is true
        // we create a new application and add him as ADMIN_LABEL
        when(authorization.getUser(AUTHHEADER)).thenReturn(TESTUSER);
        when(experiments.getExperiment(newExperiment.getID())).thenReturn(experiment1);
        Response responseNewApp = experimentsResource.postExperiment(newExperiment, true, AUTHHEADER);
        assert (experiment1.equals(responseNewApp.getEntity()));

        // When no AUTHHEADER is present
        doThrow(AuthenticationException.class).when(authorization)
                .getUser(AUTHHEADER);
        try {
            experimentsResource.postExperiment(newExperiment, false, null);
            fail();
        } catch (AuthenticationException ignored) {
        }
    }

    @Test
    public void getExperiment() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        when(experiments.getExperiment(experiment.getID())).thenReturn(null);
        try {
            experimentsResource.getExperiment(experiment.getID(), null);
            fail();
        } catch (ExperimentNotFoundException ignored) {
        }

        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        Response response = experimentsResource.getExperiment(experiment.getID(), null);
        assert (experiment.equals(response.getEntity()));

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        response = experimentsResource.getExperiment(experiment.getID(), AUTHHEADER);
        assert (experiment.equals(response.getEntity()));

        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, TESTAPP, Permission.READ);
        try {
            experimentsResource.getExperiment(experiment.getID(), AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }

        doThrow(AuthenticationException.class).when(authorization)
                .getUser(AUTHHEADER);
        try {
            experimentsResource.getExperiment(experiment.getID(), AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }
    }

    @Test
    public void putExperiment() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(authorization.getUserInfo(USER)).thenReturn(USERINFO);
        when(experiments.getExperiment(experiment.getID())).thenReturn(null);
        try {
            experimentsResource.putExperiment(experiment.getID(), experiment, false, null);
            fail();
        } catch (ExperimentNotFoundException ignored) {
        }

        when(experiments.updateExperiment(experiment.getID(), experiment, USERINFO)).thenReturn(experiment);
        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        Response response = experimentsResource.putExperiment(experiment.getID(), experiment, false, AUTHHEADER);
        assert (experiment.equals(response.getEntity()));

        // When a user wants to create a new App and update experiment with it
        experiment.setApplicationName(TESTAPP2);
        when(experiments.updateExperiment(experiment.getID(), experiment, USERINFO)).thenReturn(experiment);
        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        Response responseNewApp = experimentsResource.putExperiment(experiment.getID(), experiment, true, AUTHHEADER);
        assert (experiment.equals(responseNewApp.getEntity()));

        // When experiment is in deleted state don't allow updates in both cases
        // Old app and new app
        experiment.setState(Experiment.State.DELETED);

        response = experimentsResource.putExperiment(experiment.getID(), experiment, false, AUTHHEADER);
        assert (Objects.isNull(response.getEntity()));

        response = experimentsResource.putExperiment(experiment.getID(), experiment, true, AUTHHEADER);
        assert (Objects.isNull(response.getEntity()));

        // Set app name back to TESTAPP
        experiment.setApplicationName(TESTAPP);
        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, TESTAPP, Permission.UPDATE);
        try {
            experimentsResource.putExperiment(experiment.getID(), experiment, false, AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }

        doThrow(AuthenticationException.class).when(authorization)
                .getUser(AUTHHEADER);
        try {
            experimentsResource.putExperiment(experiment.getID(), experiment, false, AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }
    }

    @Test
    public void deleteExperiment() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(authorization.getUserInfo(USER)).thenReturn(USERINFO);

        when(experiments.getExperiment(experiment.getID())).thenReturn(null);
        try {
            experimentsResource.deleteExperiment(experiment.getID(), AUTHHEADER);
        } catch (ExperimentNotFoundException ignored) {
        }

        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        Experiment updatedExperiment = Experiment.from(experiment)
                .withState(Experiment.State.DELETED)
                .build();

        when(experiments.updateExperiment(experiment.getID(), updatedExperiment, USERINFO)).thenReturn(null);
        try {
            experimentsResource.deleteExperiment(experiment.getID(), AUTHHEADER);
            fail();
        } catch (AssertionError ignored) {
        }

        when(experiments.updateExperiment(experiment.getID(), updatedExperiment, USERINFO)).thenReturn(updatedExperiment);
        experimentsResource.deleteExperiment(experiment.getID(), AUTHHEADER);

        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, TESTAPP, Permission.DELETE);
        try {
            experimentsResource.deleteExperiment(experiment.getID(), AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }

        doThrow(AuthenticationException.class).when(authorization).getUser(AUTHHEADER);
        try {
            experimentsResource.deleteExperiment(experiment.getID(), AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }
    }

    @Test
    public void getBuckets() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        Bucket bucket1 = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("bar"))
                .withAllocationPercent(.5)
                .withControl(false)
                .withDescription("")
                .withPayload("")
                .build();

        BucketList bucketList = new BucketList();
        bucketList.addBucket(bucket);
        bucketList.addBucket(bucket1);

        when(buckets.getBuckets(experiment.getID())).thenReturn(bucketList);
        Response response = experimentsResource.getBuckets(experiment.getID(), null);
        assert (bucketList.equals(response.getEntity()));

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        response = experimentsResource.getBuckets(experiment.getID(), AUTHHEADER);
        assert (bucketList.equals(response.getEntity()));

        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, TESTAPP, Permission.READ);
        try {
            experimentsResource.getBuckets(experiment.getID(), AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }

        doThrow(AuthenticationException.class).when(authorization).getUser(AUTHHEADER);
        try {
            experimentsResource.getBuckets(experiment.getID(), AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }
    }

    @Test
    public void postBucket() throws Exception {
        //todo: implement
    }

    @Test
    public void getBucket() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(experiments.getExperiment(experiment.getID())).thenReturn(null);
        try {
            experimentsResource.getBucket(experiment.getID(), bucket.getLabel(), AUTHHEADER);
            fail();
        } catch (ExperimentNotFoundException ignored) {
        }

        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        when(buckets.getBucket(experiment.getID(), bucket.getLabel())).thenReturn(null);
        try {
            experimentsResource.getBucket(experiment.getID(), bucket.getLabel(), AUTHHEADER);
            fail();
        } catch (BucketNotFoundException ignored) {
        }

        when(buckets.getBucket(experiment.getID(), bucket.getLabel())).thenReturn(bucket);
        Response response = experimentsResource.getBucket(experiment.getID(), bucket.getLabel(), AUTHHEADER);

        assert (bucket.equals(response.getEntity()));


        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, experiment.getApplicationName(), Permission.READ);
        try {
            experimentsResource.getBucket(experiment.getID(), bucket.getLabel(), AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }

        doThrow(AuthenticationException.class).when(authorization).getUser(AUTHHEADER);
        try {
            experimentsResource.getBucket(experiment.getID(), bucket.getLabel(), AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }
    }

    @Test
    public void putBucket() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(authorization.getUserInfo(USER)).thenReturn(USERINFO);
        when(experiments.getExperiment(experiment.getID())).thenReturn(null);

        try {
            experimentsResource.putBucket(experiment.getID(), bucket.getLabel(), bucket, AUTHHEADER);
            fail();
        } catch (ExperimentNotFoundException ignored) {
        }

        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        when(buckets.updateBucket(experiment.getID(), bucket.getLabel(), bucket, USERINFO)).thenReturn(null);

        try {
            experimentsResource.putBucket(experiment.getID(), bucket.getLabel(), bucket, AUTHHEADER);
            fail();
        } catch (AssertionError ignored) {
        }


        when(buckets.updateBucket(experiment.getID(), bucket.getLabel(), bucket, USERINFO)).thenReturn(bucket);
        Response response = experimentsResource.putBucket(experiment.getID(), bucket.getLabel(), bucket, AUTHHEADER);
        assert (bucket.equals(response.getEntity()));

        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, experiment.getApplicationName(), Permission.UPDATE);
        try {
            experimentsResource.putBucket(experiment.getID(), bucket.getLabel(), bucket, AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }

        doThrow(AuthenticationException.class).when(authorization).getUser(AUTHHEADER);
        try {
            experimentsResource.putBucket(experiment.getID(), bucket.getLabel(), bucket, AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }
    }

    @Test
    public void putBucketState() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(authorization.getUserInfo(USER)).thenReturn(USERINFO);
        when(experiments.getExperiment(experiment.getID())).thenReturn(null);
        try {
            experimentsResource.putBucketState(experiment.getID(), bucket.getLabel(), Bucket.State.valueOf("OPEN"),
                    AUTHHEADER);
        } catch (ExperimentNotFoundException ignored) {
        }

        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        when(buckets.updateBucketState(experiment.getID(), bucket.getLabel(), Bucket.State.valueOf("OPEN"),
                USERINFO)).thenReturn(null);
        try {
            experimentsResource.putBucketState(experiment.getID(), bucket.getLabel(), Bucket.State.valueOf("OPEN"),
                    AUTHHEADER);
        } catch (AssertionError ignored) {
        }

        when(buckets.updateBucketState(experiment.getID(), bucket.getLabel(), Bucket.State.valueOf("OPEN"),
                USERINFO)).thenReturn(bucket);

        Response response = experimentsResource.putBucketState(experiment.getID(), bucket.getLabel(), Bucket.State.valueOf("OPEN"),
                AUTHHEADER);
        assert (bucket.equals(response.getEntity()));

        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, experiment.getApplicationName(), Permission.UPDATE);
        try {
            experimentsResource.putBucketState(experiment.getID(), bucket.getLabel(), Bucket.State.valueOf("OPEN"),
                    AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }

        doThrow(AuthenticationException.class).when(authorization).getUser(AUTHHEADER);
        try {
            experimentsResource.putBucketState(experiment.getID(), bucket.getLabel(), Bucket.State.valueOf("OPEN"),
                    AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }
    }

    @Test
    public void deleteBucketExceptions() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(experiments.getExperiment(experiment.getID())).thenReturn(null);
        thrown.expect(ExperimentNotFoundException.class);
        experimentsResource.deleteBucket(experiment.getID(), bucket.getLabel(), AUTHHEADER);

        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, experiment.getApplicationName(), Permission.DELETE);
        thrown.expect(AuthenticationException.class);
        experimentsResource.deleteBucket(experiment.getID(), bucket.getLabel(), AUTHHEADER);

        thrown.expect(AuthenticationException.class);
        doThrow(AuthenticationException.class).when(authorization).getUser(AUTHHEADER);
        experimentsResource.deleteBucket(experiment.getID(), bucket.getLabel(), AUTHHEADER);
    }

    @Test
    public void deleteBucket() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(authorization.getUserInfo(USER)).thenReturn(USERINFO);
        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        experimentsResource.deleteBucket(experiment.getID(), bucket.getLabel(), AUTHHEADER);
        verify(buckets).deleteBucket(experiment.getID(), bucket.getLabel(), USERINFO);
    }

    @Test
    public void exportActions_getExperimentNull() throws Exception {
        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);
        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(experiments.getExperiment(experiment.getID())).thenReturn(null);
        thrown.expect(ExperimentNotFoundException.class);
        experimentsResource.exportActions_get(experiment.getID(), AUTHHEADER);
    }

    @Test
    public void exportActions_get() throws Exception {
        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);
        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        assertNotNull(experimentsResource.exportActions_get(experiment.getID(), AUTHHEADER));
    }

    @Test
    public void exportAssignmentsExperimentNull() throws Exception {
        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);
        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(experiments.getExperiment(experiment.getID())).thenReturn(null);
        thrown.expect(ExperimentNotFoundException.class);
        experimentsResource.exportAssignments(experiment.getID(), context, ignoreStringNullBucket,
                fromStringDate, toStringDate, timeZoneString, AUTHHEADER);
    }

    @Test
    public void exportAssignments() throws Exception {
        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);
        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        assertNotNull(experimentsResource.exportAssignments(experiment.getID(), context, ignoreStringNullBucket,
                fromStringDate, toStringDate, timeZoneString, AUTHHEADER));
    }

    @Test
    public void exportAssignments_InvalidTimeZone() throws Exception {
        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD",
                new HttpHeader("MyApp-???"), paginationHelper);

        thrown.expect(TimeZoneFormatException.class);
        experimentsResource.exportAssignments(experiment.getID(), null, null, null, null, "noTimezoneString", null);
    }

    @Test
    public void exportAssignment_InvalidStartDate() throws Exception {
        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD",
                new HttpHeader("MyApp-???"), paginationHelper);

        thrown.expect(TimeFormatException.class);
        experimentsResource.exportAssignments(experiment.getID(), null, null, "invalidStart", null, null, null);
    }

    @Test
    public void exportAssignment_InvalidEndDate() throws Exception {
        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD",
                new HttpHeader("MyApp-???"), paginationHelper);

        thrown.expect(TimeFormatException.class);
        experimentsResource.exportAssignments(experiment.getID(), null, null, null, "invalidEnd", null, null);
    }

    @Test
    public void getPageExperiments() throws Exception {
        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);
        assertNotNull(experimentsResource.getPageExperiments(TESTAPP, TESTPAGE));
    }

    @Test
    public void exportActions_post() throws Exception {
        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        doReturn(USER).when(authorization).getUser(AUTHHEADER);
        doReturn(null).when(experiments).getExperiment(experiment.getID());

        thrown.expect(ExperimentNotFoundException.class);
        experimentsResource.exportActions(experiment.getID(), null, AUTHHEADER);
    }

    @Test
    public void createExclusions() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(authorization.getUserInfo(USER)).thenReturn(USERINFO);
        when(experiments.getExperiment(experiment.getID())).thenReturn(null);
        Experiment experiment2 = Experiment.withID(Experiment.ID.newInstance())
                .withApplicationName(TESTAPP)
                .withStartTime(new Date())
                .withEndTime(new Date())
                .withState(Experiment.State.DRAFT)
                .build();
        List<Experiment.ID> experimentIDs = new ArrayList<>();
        experimentIDs.add(experiment2.getID());
        ExperimentIDList experimentIDList = ExperimentIDList.newInstance().withExperimentIDs(experimentIDs).build();
        try {
            experimentsResource.createExclusions(experiment.getID(), experimentIDList, AUTHHEADER);
        } catch (ExperimentNotFoundException ignored) {
        }

        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        HashMap<Experiment.ID, Experiment.ID> hashMap = new HashMap<>();
        hashMap.put(experiment.getID(), experiment2.getID());
        List<Map> exclusionsList = new ArrayList<>();
        exclusionsList.add(hashMap);
        when(mutex.createExclusions(experiment.getID(), experimentIDList, USERINFO)).thenReturn(exclusionsList);
        HashMap<String, Object> result = new HashMap<>();
        result.put("exclusions", exclusionsList);
        Response response = experimentsResource.createExclusions(experiment.getID(), experimentIDList, AUTHHEADER);
        assert (result.equals(response.getEntity()));

        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, experiment.getApplicationName(), Permission.CREATE);
        try {
            experimentsResource.createExclusions(experiment.getID(), experimentIDList, AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }
    }

    @Test
    public void removeExclusions() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        Experiment experiment2 = Experiment.withID(Experiment.ID.newInstance())
                .withApplicationName(TESTAPP)
                .withStartTime(new Date())
                .withEndTime(new Date())
                .withState(Experiment.State.DRAFT)
                .build();

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(experiments.getExperiment(experiment.getID())).thenReturn(null);
        try {
            experimentsResource.removeExclusions(experiment.getID(), experiment2.getID(), AUTHHEADER);
        } catch (ExperimentNotFoundException ignored) {
        }

        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, experiment.getApplicationName(), Permission.DELETE);
        try {
            experimentsResource.removeExclusions(experiment.getID(), experiment2.getID(), AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }

        doThrow(AuthenticationException.class).when(authorization)
                .getUser(AUTHHEADER);
        try {
            experimentsResource.removeExclusions(experiment.getID(), experiment2.getID(), AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }

        doThrow(AuthenticationException.class).when(authorization).getUser(AUTHHEADER);
        try {
            experimentsResource.removeExclusions(experiment.getID(), experiment2.getID(), AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }

        // simply pass through the method when we get valid input and check if the response is correct
        doReturn(USER).when(authorization).getUser(AUTHHEADER);
        doReturn(experiment).when(experiments).getExperiment(experiment.getID());
        doNothing().when(authorization).checkUserPermissions(USER, experiment.getApplicationName(), Permission.DELETE);
        doNothing().when(mutex).deleteExclusion(experiment.getID(), experiment2.getID(), USERINFO);

        Response response = experimentsResource.removeExclusions(experiment.getID(), experiment2.getID(), AUTHHEADER);
        assertEquals("Response code indicates no deletion occurred.", response.getStatus(), NO_CONTENT.getStatusCode());
    }

    @Test
    public void getExclusions() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        Experiment experiment2 = Experiment.withID(Experiment.ID.newInstance())
                .withApplicationName(TESTAPP)
                .withStartTime(new Date())
                .withEndTime(new Date())
                .withState(Experiment.State.DRAFT)
                .build();
        Experiment experiment3 = Experiment.withID(Experiment.ID.newInstance())
                .withApplicationName(TESTAPP)
                .withStartTime(new Date())
                .withEndTime(new Date())
                .withState(Experiment.State.RUNNING)
                .build();
        Experiment experiment4 = Experiment.withID(Experiment.ID.newInstance())
                .withApplicationName(TESTAPP)
                .withStartTime(new Date())
                .withEndTime(new Date())
                .withState(Experiment.State.TERMINATED)
                .build();
        Experiment experiment5 = Experiment.withID(Experiment.ID.newInstance())
                .withApplicationName(TESTAPP)
                .withStartTime(new Date())
                .withEndTime(new Date())
                .withState(Experiment.State.DELETED)
                .build();

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(experiments.getExperiment(experiment.getID())).thenReturn(null);
        try {
            experimentsResource.getExclusions(experiment.getID(), true, true, AUTHHEADER);
        } catch (ExperimentNotFoundException ignored) {
        }

        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);

        ExperimentList experimentList = new ExperimentList();
        experimentList.addExperiment(experiment2);
        experimentList.addExperiment(experiment3);
        experimentList.addExperiment(experiment4);
        experimentList.addExperiment(experiment5);

        ExperimentList experimentListResponse = new ExperimentList();
        experimentListResponse.addExperiment(experiment2);
        experimentListResponse.addExperiment(experiment3);

        when(mutex.getExclusions(experiment.getID())).thenReturn(experimentList);
        when(mutex.getNotExclusions(experiment.getID())).thenReturn(experimentList);

        Response response = experimentsResource.getExclusions(experiment.getID(), true, true, AUTHHEADER);
        assert (experimentList.equals(response.getEntity()));
        response = experimentsResource.getExclusions(experiment.getID(), true, false, AUTHHEADER);
        assert (experimentList.equals(response.getEntity()));
        response = experimentsResource.getExclusions(experiment.getID(), false, true, AUTHHEADER);
        assert (experimentListResponse.equals(response.getEntity()));
        response = experimentsResource.getExclusions(experiment.getID(), false, false, AUTHHEADER);
        assert (experimentListResponse.equals(response.getEntity()));

        response = experimentsResource.getExclusions(experiment.getID(), true, true, null);
        assert (experimentList.equals(response.getEntity()));
        response = experimentsResource.getExclusions(experiment.getID(), true, false, null);
        assert (experimentList.equals(response.getEntity()));
        response = experimentsResource.getExclusions(experiment.getID(), false, true, null);
        assert (experimentListResponse.equals(response.getEntity()));
        response = experimentsResource.getExclusions(experiment.getID(), false, false, null);
        assert (experimentListResponse.equals(response.getEntity()));

        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, experiment.getApplicationName(), Permission.READ);
        try {
            experimentsResource.getExclusions(experiment.getID(), true, true, AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }

        doThrow(AuthenticationException.class).when(authorization)
                .getUser(AUTHHEADER);
        try {
            experimentsResource.getExclusions(experiment.getID(), true, true, AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }
    }

    @Test
    public void setPriority() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(experiments.getExperiment(experiment.getID())).thenReturn(null);
        try {
            experimentsResource.setPriority(experiment.getID(), 1, AUTHHEADER);
        } catch (ExperimentNotFoundException ignored) {
        }

        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);

        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, experiment.getApplicationName(), Permission.CREATE);
        try {
            experimentsResource.setPriority(experiment.getID(), 1, AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }

        doThrow(AuthenticationException.class).when(authorization).getUser(AUTHHEADER);
        try {
            experimentsResource.setPriority(experiment.getID(), 1, AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }

        // simply pass through the method when we get valid input and check if the response is correct
        doReturn(USER).when(authorization).getUser(AUTHHEADER);
        doReturn(experiment).when(experiments).getExperiment(experiment.getID());
        doNothing().when(authorization).checkUserPermissions(USER, experiment.getApplicationName(), Permission.CREATE);
        doNothing().when(priorities).setPriority(experiment.getID(), 1);

        Response response = experimentsResource.setPriority(experiment.getID(), 1, AUTHHEADER);
        assertEquals("Response code indicates priority was not set.", response.getStatus(), CREATED.getStatusCode());
    }

    @Test
    public void postPages() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        ExperimentPageList experimentPageList = new ExperimentPageList();

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(experiments.getExperiment(experiment.getID())).thenReturn(null);
        try {
            experimentsResource.postPages(experiment.getID(), experimentPageList, AUTHHEADER);
        } catch (ExperimentNotFoundException ignored) {
        }

        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);

        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, experiment.getApplicationName(), Permission.CREATE);
        try {
            experimentsResource.postPages(experiment.getID(), experimentPageList, AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }

        doThrow(AuthenticationException.class).when(authorization).getUser(AUTHHEADER);
        try {
            experimentsResource.postPages(experiment.getID(), experimentPageList, AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }

        // simply pass through the method when we get valid input and check if the response is correct
        doReturn(USER).when(authorization).getUser(AUTHHEADER);
        doReturn(experiment).when(experiments).getExperiment(experiment.getID());
        doNothing().when(authorization).checkUserPermissions(USER, experiment.getApplicationName(), Permission.CREATE);
        doNothing().when(pages).postPages(experiment.getID(), experimentPageList, USERINFO);

        Response response = experimentsResource.postPages(experiment.getID(), experimentPageList, AUTHHEADER);
        assertEquals("Response code indicates pages were not set.", response.getStatus(), CREATED.getStatusCode());
    }

    @Test
    public void deletePageErrors() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(experiments.getExperiment(experiment.getID())).thenReturn(null);
        try {
            experimentsResource.deletePage(experiment.getID(), Page.Name.valueOf("pageName"), AUTHHEADER);
        } catch (ExperimentNotFoundException ignored) {
        }

        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);

        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, experiment.getApplicationName(), Permission.DELETE);
        try {
            experimentsResource.deletePage(experiment.getID(), Page.Name.valueOf("pageName"), AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }

        doThrow(AuthenticationException.class).when(authorization).getUser(AUTHHEADER);
        try {
            experimentsResource.deletePage(experiment.getID(), Page.Name.valueOf("pageName"), AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }
    }

    @Test
    public void deletePage() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        experimentsResource.deletePage(experiment.getID(), Page.Name.valueOf("pageName"), AUTHHEADER);
    }

    @Test
    public void getExperimentPages() throws Exception {

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(experiments.getExperiment(experiment.getID())).thenReturn(null);
        try {
            experimentsResource.getExperimentPages(experiment.getID(), AUTHHEADER);
        } catch (ExperimentNotFoundException ignored) {
        }

        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);

        ExperimentPageList experimentPageList = new ExperimentPageList();
        when(pages.getExperimentPages(experiment.getID())).thenReturn(experimentPageList);
        Response response = experimentsResource.getExperimentPages(experiment.getID(), AUTHHEADER);
        assert (experimentPageList.equals(response.getEntity()));
        response = experimentsResource.getExperimentPages(experiment.getID(), null);
        assert (experimentPageList.equals(response.getEntity()));

        doThrow(AuthenticationException.class).when(authorization)
                .checkUserPermissions(USER, experiment.getApplicationName(), Permission.READ);
        try {
            experimentsResource.getExperimentPages(experiment.getID(), AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }

        doThrow(AuthenticationException.class).when(authorization).getUser(AUTHHEADER);
        try {
            experimentsResource.getExperimentPages(experiment.getID(), AUTHHEADER);
            fail();
        } catch (AuthenticationException ignored) {
        }
    }

    @Test
    public void bucketReturnsLocationHeader() throws Exception {
        Experiment experiment =
                Experiment.withID(Experiment.ID.valueOf(EXPERIMENT_ID)).build();

        Bucket.Label bucketLabel = Bucket.Label.valueOf("foo");

        Bucket bucket = Bucket.newInstance(experiment.getID(), bucketLabel)
                .withAllocationPercent(0d)
                .withControl(false)
                .withDescription("")
                .withPayload("")
                .build();

        Bucket newBucket = Bucket.newInstance(experiment.getID(), bucketLabel)
                .withAllocationPercent(0d)
                .withControl(false)
                .withDescription("")
                .withPayload("")
                .build();

        UserInfo userInfo = UserInfo.from(UserInfo.Username.valueOf(USERPASS)).build();

        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        when(buckets.createBucket(argThat(equalTo(experiment.getID())),
                Mockito.any(Bucket.class), Mockito.any(UserInfo.class))).thenReturn(bucket);
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(fromPath(PATH));
        UserInfo.Username subject = UserInfo.Username.valueOf("auser");
        when(authorization.getUser(USERPASS)).thenReturn(subject);
        when(authorization.getUserInfo(subject)).thenReturn(userInfo);

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);
        Response response = experimentsResource.postBucket(experiment.getID(), newBucket, USERPASS);

        Bucket content = (Bucket) response.getEntity();

        assertThat(content, equalTo(bucket));
        assertThat(response.getStatus(), is(CREATED.getStatusCode()));
    }

    @Test
    public void postBucketExperimentNull() throws Exception {
        Experiment experiment =
                Experiment.withID(Experiment.ID.valueOf(EXPERIMENT_ID)).build();
        UserInfo.Username subject = UserInfo.Username.valueOf("auser");
        Bucket newBucket = Mockito.mock(Bucket.class);
        when(authorization.getUser(USERPASS)).thenReturn(subject);
        when(experiments.getExperiment(experiment.getID())).thenReturn(null);

        thrown.expect(ExperimentNotFoundException.class);
        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);
        experimentsResource.postBucket(experiment.getID(), newBucket, USERPASS);
    }

    @Test
    public void putBucketListExperimentNull() throws Exception {
        Experiment experiment =
                Experiment.withID(Experiment.ID.valueOf(EXPERIMENT_ID)).build();
        UserInfo.Username subject = UserInfo.Username.valueOf("auser");
        BucketList bucketList = Mockito.mock(BucketList.class);
        when(authorization.getUser(USERPASS)).thenReturn(subject);
        when(experiments.getExperiment(experiment.getID())).thenReturn(null);

        thrown.expect(ExperimentNotFoundException.class);
        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);
        experimentsResource.putBucket(experiment.getID(), bucketList, USERPASS);
    }

    @Test
    public void putBucketList() throws Exception {
        Experiment experiment =
                Experiment.withID(Experiment.ID.valueOf(EXPERIMENT_ID)).build();
        UserInfo.Username subject = UserInfo.Username.valueOf("auser");
        BucketList bucketList = Mockito.mock(BucketList.class);
        when(authorization.getUser(USERPASS)).thenReturn(subject);
        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);
        assertNotNull(experimentsResource.putBucket(experiment.getID(), bucketList, USERPASS));
    }

    @Test
    public void experimentReturnsLocationHeader() throws Exception {

        final Experiment.ID EXPERIMENT_ID = ID.newInstance();

        Experiment experiment = Experiment.withID(EXPERIMENT_ID)
                .withApplicationName(Application.Name.valueOf("foo"))
                .withDescription("")
                .withStartTime(new Date())
                .withEndTime(new Date())
                .withSamplingPercent(1d)
                .withDescription(description)
                .withLabel(Experiment.Label.valueOf("foo"))
                .build();

        NewExperiment newExperiment = NewExperiment.withID(EXPERIMENT_ID)
                .withAppName(Application.Name.valueOf("foo"))
                .withDescription("")
                .withStartTime(new Date())
                .withEndTime(new Date())
                .withSamplingPercent(.5d)
                .withDescription(description)
                .withLabel(Experiment.Label.valueOf("foo"))
                .build();

        when(experiments.getExperiment(experiment.getID())).thenReturn(experiment);
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(fromPath(PATH));

        ExperimentsResource experimentsResource = new ExperimentsResource(experiments, eventsExport, assignments,
                authorization, buckets, mutex, pages, priorities, favorites, "US/New York", "YYYY-mm-DD", new HttpHeader("MyApp-???"), paginationHelper);

        Response response = experimentsResource.postExperiment(newExperiment, false, "Basic: " + USERPASS);

        Experiment payload = (Experiment) response.getEntity();

        assertThat(payload, equalTo(experiment));
        assertThat(response.getStatus(), is(CREATED.getStatusCode()));
    }
}
