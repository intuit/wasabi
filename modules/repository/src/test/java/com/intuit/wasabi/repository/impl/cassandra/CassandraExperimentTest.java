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
package com.intuit.wasabi.repository.impl.cassandra;

import com.googlecode.catchexception.apis.CatchExceptionBdd;
import com.intuit.wasabi.experimentobjects.Application.Name;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.Label;
import com.intuit.wasabi.experimentobjects.Experiment.State;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ExperimentIDSerializer;
import com.netflix.astyanax.model.ColumnList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;

import static org.mockito.BDDMockito.given;


@RunWith(MockitoJUnitRunner.class)
public class CassandraExperimentTest {

    CassandraExperiment experiment;

    @Mock
    ColumnList<String> columns;

    @Before
    public void setUp() {
        given(columns.getValue("id",
                ExperimentIDSerializer.get(), null)).willReturn(Experiment.ID.newInstance());
        given(columns.getStringValue("description", null)).willReturn("description1");
        given(columns.getStringValue("rule", null)).willReturn("rule1");
        given(columns.getStringValue("rule", null)).willReturn(null);
        given(columns.getDoubleValue("sample_percent", null)).willReturn(0.01d);
        given(columns.getDateValue("start_time", null)).willReturn(new Date());
        given(columns.getDateValue("end_time", null)).willReturn(new Date());
        given(columns.getStringValue("state", null)).willReturn(State.RUNNING.name());
        given(columns.getStringValue("label", null)).willReturn(Experiment.Label.valueOf("l1").toString());
        given(columns.getStringValue("app_name", null)).willReturn("app1");
        given(columns.getDateValue("created", null)).willReturn(new Date());
        given(columns.getDateValue("modified", null)).willReturn(new Date());
        given(columns.getBooleanValue("is_personalized", false)).willReturn(true);
        given(columns.getStringValue("model_name", "")).willReturn("m1");
        given(columns.getStringValue("model_version", "")).willReturn("v1");
        given(columns.getBooleanValue("is_rapid_experiment", false)).willReturn(true);
        given(columns.getIntegerValue("user_cap", Integer.MAX_VALUE)).willReturn(5);
        given(columns.getStringValue("creatorid", null)).willReturn("c1");
        experiment = new CassandraExperiment(columns);
    }

//	@Test
//	public void testCassandraExperiment() {
//		fail("Not yet implemented");
//	}

    @Test
    public void testSetIDID() {
        CatchExceptionBdd.when(experiment).setID(Experiment.ID.newInstance());
        CatchExceptionBdd.thenThrown(UnsupportedOperationException.class);
    }

    @Test
    public void testSetDescriptionString() {
        CatchExceptionBdd.when(experiment).setDescription("d1");
        CatchExceptionBdd.thenThrown(UnsupportedOperationException.class);
    }

    @Test
    public void testSetRuleString() {
        CatchExceptionBdd.when(experiment).setRule("r1");
        CatchExceptionBdd.thenThrown(UnsupportedOperationException.class);
    }

    @Test
    public void testSetSamplingPercentDouble() {
        CatchExceptionBdd.when(experiment).setSamplingPercent(0.5d);
        CatchExceptionBdd.thenThrown(UnsupportedOperationException.class);
    }

    @Test
    public void testSetStartTimeDate() {
        CatchExceptionBdd.when(experiment).setStartTime(new Date());
        CatchExceptionBdd.thenThrown(UnsupportedOperationException.class);
    }

    @Test
    public void testSetEndTimeDate() {
        CatchExceptionBdd.when(experiment).setEndTime(new Date());
        CatchExceptionBdd.thenThrown(UnsupportedOperationException.class);
    }

    @Test
    public void testSetStateState() {
        CatchExceptionBdd.when(experiment).setState(State.DELETED);
        CatchExceptionBdd.thenThrown(UnsupportedOperationException.class);
    }

    @Test
    public void testSetLabelLabel() {
        CatchExceptionBdd.when(experiment).setLabel(Label.valueOf("l2"));
        CatchExceptionBdd.thenThrown(UnsupportedOperationException.class);
    }

    @Test
    public void testSetApplicationNameName() {
        CatchExceptionBdd.when(experiment).setApplicationName(Name.valueOf("n2"));
        CatchExceptionBdd.thenThrown(UnsupportedOperationException.class);
    }

    @Test
    public void testSetCreationTimeDate() {
        CatchExceptionBdd.when(experiment).setCreationTime(new Date());
        CatchExceptionBdd.thenThrown(UnsupportedOperationException.class);
    }

    @Test
    public void testSetModificationTimeDate() {
        CatchExceptionBdd.when(experiment).setModificationTime(new Date());
        CatchExceptionBdd.thenThrown(UnsupportedOperationException.class);
    }

    @Test
    public void testSetIsPersonalizationEnabledBoolean() {
        CatchExceptionBdd.when(experiment).setIsPersonalizationEnabled(true);
        CatchExceptionBdd.thenThrown(UnsupportedOperationException.class);
    }

    @Test
    public void testSetModelNameString() {
        CatchExceptionBdd.when(experiment).setModelName("m2");
        CatchExceptionBdd.thenThrown(UnsupportedOperationException.class);
    }

    @Test
    public void testSetModelVersionString() {
        CatchExceptionBdd.when(experiment).setModelVersion("v2");
        CatchExceptionBdd.thenThrown(UnsupportedOperationException.class);
    }

    @Test
    public void testSetIsRapidExperimentBoolean() {
        CatchExceptionBdd.when(experiment).setIsRapidExperiment(true);
        CatchExceptionBdd.thenThrown(UnsupportedOperationException.class);
    }

    @Test
    public void testSetUserCapInteger() {
        CatchExceptionBdd.when(experiment).setUserCap(4);
        CatchExceptionBdd.thenThrown(UnsupportedOperationException.class);
    }

    @Test
    public void testSetCreatorIDString() {
        CatchExceptionBdd.when(experiment).setCreatorID("c2");
        CatchExceptionBdd.thenThrown(UnsupportedOperationException.class);
    }


    @Test
    public void testSetCreationTime() {
        CatchExceptionBdd.when(experiment).setCreationTime(new Date());
        CatchExceptionBdd.thenThrown(UnsupportedOperationException.class);
    }
}
