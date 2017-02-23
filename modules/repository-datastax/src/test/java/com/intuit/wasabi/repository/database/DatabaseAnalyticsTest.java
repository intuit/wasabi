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
package com.intuit.wasabi.repository.database;

import com.googlecode.flyway.core.Flyway;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.BucketCounts;
import com.intuit.wasabi.database.Transaction;
import com.intuit.wasabi.database.TransactionFactory;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.RepositoryException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatabaseAnalyticsTest {
    TransactionFactory transactionFactory = Mockito.mock(TransactionFactory.class);
    Transaction transaction = Mockito.mock(Transaction.class);
    DataSource dataSource = Mockito.mock(DataSource.class);
    Flyway flyway = Mockito.mock(Flyway.class);
    DatabaseAnalytics databaseAnalytics;

    @Before
    public void setup() throws SQLException {
        when(transactionFactory.newTransaction()).thenReturn(transaction);
        when(transactionFactory.getDataSource()).thenReturn(dataSource);
        databaseAnalytics = spy(new DatabaseAnalytics(transactionFactory, flyway));
    }

    @Test
    public void initilizeTest() {
        Flyway mockedFlyway = Mockito.mock(Flyway.class);
        databaseAnalytics.initialize(mockedFlyway);
        verify(transactionFactory, atLeastOnce()).getDataSource();
        verify(flyway, atLeastOnce()).setLocations("com/intuit/wasabi/repository/impl/mysql/migration");
        verify(flyway, atLeastOnce()).setDataSource(dataSource);
        verify(flyway, atLeastOnce()).migrate();
    }

    @Test(expected = RepositoryException.class)
    public void getRollupRowsTest() {
        Experiment.ID experimentId = Experiment.ID.newInstance();
        String rollupDate = "TEST";
        Parameters parameters = mock(Parameters.class, RETURNS_DEEP_STUBS);
        List<Map> expected = mock(List.class);
        when(parameters.getContext().getContext()).thenReturn("TEST");
        when(transaction.select(anyString(), eq(experimentId), eq(true), eq(rollupDate), anyString()))
                .thenReturn(expected);
        List<Map> result = databaseAnalytics.getRollupRows(experimentId, rollupDate, parameters);
        assertThat(result, is(expected));
        //Exception is thrown in this case
        doThrow(new RuntimeException())
                .when(transaction)
                .select(anyString(), eq(experimentId), eq(true), eq(rollupDate), anyString());
        databaseAnalytics.getRollupRows(experimentId, rollupDate, parameters);
        fail();
    }

    @Test(expected = RepositoryException.class)
    public void getActionsRowsTest() {
        Experiment.ID experimentId = Experiment.ID.newInstance();
        Parameters parameters = mock(Parameters.class, RETURNS_DEEP_STUBS);
        when(parameters.getContext().getContext()).thenReturn("TEST");
        Date from = mock(Date.class), to = mock(Date.class);
        when(parameters.getFromTime()).thenReturn(from);
        when(parameters.getToTime()).thenReturn(to);
        List<String> actions = new ArrayList<String>();
        actions.add("TEST_ACTION");
        when(parameters.getActions()).thenReturn(actions);
        List<Map> expected = mock(List.class);
        when(transaction.select(anyString(), Matchers.anyVararg())).thenReturn(expected);
        List<Map> result = databaseAnalytics.getActionsRows(experimentId, parameters);
        assertThat(result, is(expected));
        //exception while select
        doThrow(new RuntimeException()).when(transaction)
                .select(anyString(), Matchers.anyVararg());
        databaseAnalytics.getActionsRows(experimentId, parameters);
        fail();
    }

    @Test(expected = RepositoryException.class)
    public void getJointActionsTest() {
        Experiment.ID experimentId = Experiment.ID.newInstance();
        Parameters parameters = mock(Parameters.class, RETURNS_DEEP_STUBS);
        when(parameters.getContext().getContext()).thenReturn("TEST");
        Date from = mock(Date.class), to = mock(Date.class);
        when(parameters.getFromTime()).thenReturn(from);
        when(parameters.getToTime()).thenReturn(to);
        List<String> actions = new ArrayList<String>();
        actions.add("TEST_ACTION");
        when(parameters.getActions()).thenReturn(actions);
        List<Map> expected = mock(List.class);
        when(transaction.select(anyString(), Matchers.anyVararg())).thenReturn(expected);
        List<Map> result = databaseAnalytics.getJointActions(experimentId, parameters);
        assertThat(result, is(expected));
        //exception while select
        doThrow(new RuntimeException()).when(transaction)
                .select(anyString(), Matchers.anyVararg());
        databaseAnalytics.getJointActions(experimentId, parameters);
        fail();
    }


    @Test(expected = RepositoryException.class)
    public void getImpressionRowsTest() {
        Experiment.ID experimentId = Experiment.ID.newInstance();
        Parameters parameters = mock(Parameters.class, RETURNS_DEEP_STUBS);
        when(parameters.getContext().getContext()).thenReturn("TEST");
        Date from = mock(Date.class), to = mock(Date.class);
        when(parameters.getFromTime()).thenReturn(from);
        when(parameters.getToTime()).thenReturn(to);
        List<Map> expected = mock(List.class);
        when(transaction.select(anyString(), Matchers.anyVararg())).thenReturn(expected);
        List<Map> result = databaseAnalytics.getImpressionRows(experimentId, parameters);
        assertThat(result, is(expected));
        //exception while select
        doThrow(new RuntimeException()).when(transaction)
                .select(anyString(), Matchers.anyVararg());
        databaseAnalytics.getImpressionRows(experimentId, parameters);
        fail();
    }

    @Test(expected = RepositoryException.class)
    public void getEmptyBucketsTest() {
        Experiment.ID experimentId = Experiment.ID.newInstance();
        List<Map> input = new ArrayList<Map>();
        Map<String, String> map = new HashMap<String, String>();
        map.put("label", "TEST_LABEL");
        input.add(map);
        when(transaction.select(anyString(), Matchers.anyVararg())).thenReturn(input);
        Map<Bucket.Label, BucketCounts> result = databaseAnalytics.getEmptyBuckets(experimentId);
        assertThat(result.size(), is(1));
        assertThat(result.get(Bucket.Label.valueOf("TEST_LABEL")).getLabel().toString(), is("TEST_LABEL"));
        assertThat(result.get(Bucket.Label.valueOf("TEST_LABEL")).getActionCounts().size(), is(0));
        //exception while select
        doThrow(new RuntimeException()).when(transaction)
                .select(anyString(), Matchers.anyVararg());
        databaseAnalytics.getEmptyBuckets(experimentId);
        fail();
    }

    @Test(expected = RepositoryException.class)
    public void getCountsFromRollupsTest() {
        List<Map> expected = mock(List.class);
        Experiment.ID experimentId = Experiment.ID.newInstance();
        Parameters parameters = mock(Parameters.class, RETURNS_DEEP_STUBS);
        when(parameters.getContext().getContext()).thenReturn("TEST");
        when(transaction.select(anyString(), Matchers.anyVararg())).thenReturn(expected);
        List<Map> result = databaseAnalytics.getCountsFromRollups(experimentId, parameters);
        assertThat(result, is(expected));
        //exception while select
        doThrow(new RuntimeException()).when(transaction)
                .select(anyString(), Matchers.anyVararg());
        databaseAnalytics.getCountsFromRollups(experimentId, parameters);
        fail();
    }

    @Test(expected = RepositoryException.class)
    public void checkMostRecentRollupTest() {
        Experiment.ID experimentId = Experiment.ID.newInstance();
        Experiment experiment = mock(Experiment.class);
        when(experiment.getID()).thenReturn(experimentId);
        List queryResult = mock(List.class);
        Parameters parameters = mock(Parameters.class, RETURNS_DEEP_STUBS);
        when(parameters.getContext().getContext()).thenReturn("TEST");
        Date to = mock(Date.class);
        when(to.getTime()).thenReturn(9999L);
        when(transaction.select(anyString(), eq(experimentId), eq("TEST"))).thenReturn(queryResult);
        when(queryResult.isEmpty()).thenReturn(true);
        boolean result = databaseAnalytics.checkMostRecentRollup(experiment, parameters, to);
        assertThat(result, is(true));

        when(queryResult.isEmpty()).thenReturn(false);
        Map map = mock(Map.class);
        Date maxDay = mock(Date.class);
        when(queryResult.get(0)).thenReturn(map);
        when(map.get("day")).thenReturn(maxDay);
        when(maxDay.getTime()).thenReturn(100000L);
        result = databaseAnalytics.checkMostRecentRollup(experiment, parameters, to);
        assertThat(result, is(true));

        when(maxDay.getTime()).thenReturn(1000L);
        result = databaseAnalytics.checkMostRecentRollup(experiment, parameters, to);
        assertThat(result, is(false));

        doThrow(new RuntimeException()).when(transaction)
                .select(anyString(), eq(experimentId), eq("TEST"));
        databaseAnalytics.checkMostRecentRollup(experiment, parameters, to);
        fail();

    }

    @Test
    public void testAddActionsToSql() {
        Parameters parameters = mock(Parameters.class);
        when(parameters.getActions()).thenReturn(null);
        StringBuilder stringBuilder = new StringBuilder();
        List param = new ArrayList<>();
        databaseAnalytics.addActionsToSql(parameters, stringBuilder, param);
        assertThat(stringBuilder.toString(), is(""));
        assertThat(param.size(), is(0));
        List<String> actions = new ArrayList<>();
        actions.add("a1");
        when(parameters.getActions()).thenReturn(actions);
        databaseAnalytics.addActionsToSql(parameters, stringBuilder, param);
        assertThat(actions.size(), is(1));
        assertThat(param.size(), is(1));
        assertThat(stringBuilder.toString(), is(" and action in (?) "));
        stringBuilder.setLength(0); //reset the builder
        param.clear();
        actions.add("a2");
        databaseAnalytics.addActionsToSql(parameters, stringBuilder, param);
        assertThat(actions.size(), is(2));
        assertThat(param.size(), is(2));
        assertThat(stringBuilder.toString(), is(" and action in (?,?) "));
    }


}
