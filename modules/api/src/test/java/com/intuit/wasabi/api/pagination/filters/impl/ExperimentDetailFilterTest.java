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
package com.intuit.wasabi.api.pagination.filters.impl;

import com.intuit.wasabi.analyticsobjects.wrapper.ExperimentDetail;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;


/**
 * Test class for {@link ExperimentDetailFilter}
 */
@RunWith(MockitoJUnitRunner.class)
public class ExperimentDetailFilterTest {

    private ExperimentDetail experimentDetail;

    @Before
    public void setup() {

        Experiment.ID expId = Experiment.ID.newInstance();
        Experiment.Label expLabel = Experiment.Label.valueOf("ExperimentLabel");
        Application.Name appName = Application.Name.valueOf("ApplicationName");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        Date modTime = cal.getTime();
        cal.add(Calendar.DATE, 1);
        Date startTime = cal.getTime();

        experimentDetail = new ExperimentDetail(expId, Experiment.State.RUNNING, expLabel, appName, modTime, startTime);

        Bucket b1 = Bucket.newInstance(expId, Bucket.Label.valueOf("Bucket1")).withAllocationPercent(0.6).build();
        Bucket b2 = Bucket.newInstance(expId, Bucket.Label.valueOf("Bucket2")).withAllocationPercent(0.4).build();

        List<Bucket> buckets = new ArrayList<>();
        buckets.add(b1);
        buckets.add(b2);

        experimentDetail.addBuckets(buckets);
    }

    @Test
    public void testTest() throws Exception{
        ExperimentDetailFilter experimentDetailFilter = new ExperimentDetailFilter();

        HashMap<String, Boolean> testCases = new HashMap<>();

        testCases.put("experiment_label=ExperimentLabel", true);
        testCases.put("bucket_label=Bucket2", true);
        testCases.put("application_name=testApp", false);
        //testCases.put("mod_time=Summer", false); //FIXME: why is this not working?
        //testCases.put("Experiment", true);

        for (Map.Entry<String, Boolean> testCase : testCases.entrySet()) {
            experimentDetailFilter.replaceFilter(testCase.getKey(), "+0000");

            Assert.assertEquals("test case " + testCase.getKey() + " failed.",
                    testCase.getValue(),
                    experimentDetailFilter.test(experimentDetail));
        }

    }
}
