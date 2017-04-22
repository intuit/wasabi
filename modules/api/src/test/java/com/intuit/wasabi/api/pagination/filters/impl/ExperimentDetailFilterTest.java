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
package com.intuit.wasabi.api.pagination.filters.impl;

import com.intuit.wasabi.analyticsobjects.wrapper.ExperimentDetail;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;


/**
 * Test class for {@link ExperimentDetailFilter}
 */
@RunWith(Parameterized.class)
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
        cal.add(Calendar.DATE, 14);
        Date endTime = cal.getTime();
        Set<String> tags = new TreeSet<>(java.util.Arrays.asList("tag1", "tag2", "tag3"));

        experimentDetail = new ExperimentDetail(expId, Experiment.State.RUNNING, expLabel, appName,
                modTime, startTime, endTime, "testDescription", tags);

        Bucket b1 = Bucket.newInstance(expId, Bucket.Label.valueOf("Bucket1")).withAllocationPercent(0.6).build();
        Bucket b2 = Bucket.newInstance(expId, Bucket.Label.valueOf("Bucket2")).withAllocationPercent(0.4).build();

        List<Bucket> buckets = new ArrayList<>();
        buckets.add(b1);
        buckets.add(b2);

        experimentDetail.addBuckets(buckets);
    }

    @Parameterized.Parameters(name = "expDetailFilter({index})")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"experiment_label=ExperimentLabel", true},
                {"bucket_label=Bucket2", true},
                {"application_name=testApp", false},
                {"mod_time=Summer", false},
                {"Experiment", true},
                {"tags=tag1", true},
        });
    }

    private String filter;
    private boolean allowed;

    public ExperimentDetailFilterTest(String filter, boolean allowed) {
        this.filter = filter;
        this.allowed = allowed;
    }


    @Test
    public void testTest() throws Exception {
        ExperimentDetailFilter experimentDetailFilter = new ExperimentDetailFilter();
        experimentDetailFilter.replaceFilter(filter, null);
        Assert.assertEquals("test case " + filter + " failed.", allowed,
                experimentDetailFilter.test(experimentDetail));
    }
}
