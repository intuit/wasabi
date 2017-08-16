/*******************************************************************************
 * Copyright 2017 Intuit
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
package com.intuit.wasabi.repository.cassandra.impl;

import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.cassandra.accessor.count.HourlyBucketCountAccessor;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class AssignmentStatsTest {
    private HourlyBucketCountAccessor hourlyBucketCountAccessor;
    private AssignmentStats assignmentStats = new AssignmentStats(hourlyBucketCountAccessor);

    @Test
    public void incrementCountTest() throws Exception {
        // Checks whether hourlyCountMap accurately increments counts each time incrementCount is called
        Experiment experiment = mock(Experiment.class);
        Assignment assignment = mock(Assignment.class);
        Bucket.Label testBucket = Bucket.Label.valueOf("Maxwell");
        Mockito.when(assignment.getBucketLabel()).thenReturn(testBucket);
        Mockito.when(experiment.getID()).thenReturn(Experiment.ID.newInstance());

        int assignmentHour = AssignmentStatsUtil.getHour(new Date(System.currentTimeMillis()));
        assignmentStats.incrementCount(experiment, assignment);
        int count = assignmentStats.getCount(experiment, assignment.getBucketLabel(), assignmentHour);
        Assert.assertEquals(1, count);
        assignmentStats.incrementCount(experiment, assignment);
        count = assignmentStats.getCount(experiment, assignment.getBucketLabel(), assignmentHour);
        Assert.assertEquals(2, count);
        assignmentStats.incrementCount(experiment, assignment);
        count = assignmentStats.getCount(experiment, assignment.getBucketLabel(), assignmentHour);
        Assert.assertEquals(3, count);

        testBucket = Bucket.Label.valueOf("Bruckhaus");
        Mockito.when(assignment.getBucketLabel()).thenReturn(testBucket);
        assignmentStats.incrementCount(experiment, assignment);
        count = assignmentStats.getCount(experiment, assignment.getBucketLabel(), assignmentHour);
        Assert.assertEquals(1, count);
    }


    @Test
    public void getLastCompletedHourTest() throws Exception {
        // Checks correct date and time logic for determining the last completed hour
        long time = 1400999945000L;
        int hourInMillis = 3600000;
        long timeOneHourAgoMillis = time - hourInMillis;
        Date timeOneHourAgoMillisDate = new Date(timeOneHourAgoMillis);
        Assert.assertEquals(timeOneHourAgoMillisDate, AssignmentStatsUtil.getLastCompletedHour(time));
    }

    @Test
    public void getHourTest() throws Exception {
        // Checks whether getHour returns only the hour from a given date correctly
        DateFormat format = new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss");

        String timeNowString = "May 24, 2014 23:39:05";
        Date timeNowDate = format.parse(timeNowString);
        assertEquals(23, AssignmentStatsUtil.getHour(timeNowDate));

        String timeNowString2 = "June 17, 2017 05:10:26";
        Date timeNowDate2 = format.parse(timeNowString2);
        assertEquals(05, AssignmentStatsUtil.getHour(timeNowDate2));
    }

    @Test
    public void getDayStringTest() throws Exception {
        // Checks if getDayString returns date in correct format: yyyy-MM-dd
        DateFormat format = new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss");
        String testDateString = "May 24, 2014 23:39:05";
        Date testDate = format.parse(testDateString);
        assertEquals("2014-05-24", AssignmentStatsUtil.getDayString(testDate));
    }
}
