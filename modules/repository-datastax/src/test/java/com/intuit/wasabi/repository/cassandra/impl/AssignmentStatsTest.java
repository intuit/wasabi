package com.intuit.wasabi.repository.cassandra.impl;

import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class AssignmentStatsTest {
    private AssignmentStats assignmentStats = new AssignmentStats();

    @Test
    public void incrementCount() throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
        String dateInString = "22-01-2015 10:20:56";
        Date created = dateFormat.parse(dateInString);
        Bucket.Label testBucket = Bucket.Label.valueOf("Maxwell");

        Experiment experiment = mock(Experiment.class);
        Assignment assignment = mock(Assignment.class);
        Mockito.when(assignment.getCreated()).thenReturn(created);
        Mockito.when(assignment.getBucketLabel()).thenReturn(testBucket);
        Mockito.when(experiment.getID()).thenReturn(Experiment.ID.newInstance());

        assignmentStats.incrementCount(experiment, assignment);
        int count = assignmentStats.getCount(experiment, assignment.getBucketLabel(), 10);
        Assert.assertEquals(1, count);
        assignmentStats.incrementCount(experiment, assignment);
        count = assignmentStats.getCount(experiment, assignment.getBucketLabel(), 10);
        Assert.assertEquals(2, count);
        assignmentStats.incrementCount(experiment, assignment);
        count = assignmentStats.getCount(experiment, assignment.getBucketLabel(), 10);
        Assert.assertEquals(3, count);

        dateInString = "22-01-2015 21:20:56";
        created = dateFormat.parse(dateInString);
        testBucket = Bucket.Label.valueOf("Bruckhaus");
        Mockito.when(assignment.getCreated()).thenReturn(created);
        Mockito.when(assignment.getBucketLabel()).thenReturn(testBucket);
        assignmentStats.incrementCount(experiment, assignment);
        count = assignmentStats.getCount(experiment, assignment.getBucketLabel(), 21);
        Assert.assertEquals(1, count);
    }

    @Test
    public void writeCounts() throws Exception {

    }

    @Test
    public void getLastCompletedHour() throws Exception {
        DateFormat format = new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss");
        String timeNowString = "May 24, 2014 23:39:05";
        String oneHourAgoString = "May 24, 2014 22:39:05";
        Date timeNowDate = format.parse(timeNowString);
        Date oneHourAgoDate = format.parse(oneHourAgoString);
        long timeNowMillis = 1400999945000L;
        Date timeNowMillisDate = new Date(timeNowMillis);

        assertEquals(timeNowMillisDate, timeNowDate);
        Assert.assertEquals(oneHourAgoDate, assignmentStats.getLastCompletedHour(timeNowMillis));
    }

    @Test
    public void getHour() throws Exception {
        DateFormat format = new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss");

        String timeNowString = "May 24, 2014 23:39:05";
        Date timeNowDate = format.parse(timeNowString);
        assertEquals(23, assignmentStats.getHour(timeNowDate));

        String timeNowString2 = "June 17, 2017 05:10:26";
        Date timeNowDate2 = format.parse(timeNowString2);
        assertEquals(05, assignmentStats.getHour(timeNowDate2));
    }

    @Test
    public void getDayString() throws Exception {
        DateFormat format = new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss");
        String testDateString = "May 24, 2014 23:39:05";
        Date testDate = format.parse(testDateString);
        assertEquals("2014-05-24", assignmentStats.getDayString(testDate));
    }
}
