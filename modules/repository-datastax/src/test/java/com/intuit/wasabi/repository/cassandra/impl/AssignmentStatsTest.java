package com.intuit.wasabi.repository.cassandra.impl;

import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Assert;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;


public class AssignmentStatsTest {


    @Test
    public void run() throws Exception {
    }

    @Test
    public void populateMaps() throws Exception {
        Map<Integer, Map<ExpBucket, AtomicInteger>> hourlyCountMap = new ConcurrentHashMap<>();
        for (int hour = 0; hour <= 23; hour++){
            hourlyCountMap.put(hour, new ConcurrentHashMap<>());
        }
        Experiment.ID expID = Experiment.ID.newInstance();
        Bucket.Label bucket = Bucket.Label.valueOf("label1");
        Date completedHour = AssignmentStats.getLastCompletedHour(System.currentTimeMillis());
        int eventTimeHour = AssignmentStats.getHour(completedHour);
        ExpBucket expBucket = new ExpBucket(expID, bucket);
        AssignmentStats.populateMaps(hourlyCountMap, expBucket, eventTimeHour);
        Assert.assertEquals(1, hourlyCountMap.get(eventTimeHour).get(expBucket).getAndIncrement());
        AssignmentStats.populateMaps(hourlyCountMap, expBucket, eventTimeHour);
        Assert.assertEquals(2, hourlyCountMap.get(eventTimeHour).get(expBucket).getAndIncrement());
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
        Assert.assertEquals(oneHourAgoDate, AssignmentStats.getLastCompletedHour(timeNowMillis));
    }

    @Test
    public void getHour() throws Exception {
        DateFormat format = new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss");

        String timeNowString = "May 24, 2014 23:39:05";
        Date timeNowDate = format.parse(timeNowString);
        assertEquals(23, AssignmentStats.getHour(timeNowDate));

        String timeNowString2 = "June 17, 2017 05:10:26";
        Date timeNowDate2 = format.parse(timeNowString2);
        assertEquals(05, AssignmentStats.getHour(timeNowDate2));
    }

}




    /*

    @Test
    public void getDayString() throws Exception {
        DateFormat format = new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss");
        String timeNowString = "May 24, 2014 23:39:05";
        Date timeNowDate = format.parse(timeNowString);
        assertEquals("2014-05-24", AssignmentHourlyAggregatorTask.getDayString(timeNowDate));
    }



    @Test
    public void getStartTime() throws Exception {
        DateFormat format = new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss");
        String timeNowString = "November 11, 2011 05:06:07";
        String expectedStartTimeString = "November 11, 2011 05:00:00";
        Date timeNowDate = format.parse(timeNowString);
        Date expectedStartTimeDate = format.parse(expectedStartTimeString);
        assertEquals(expectedStartTimeDate, AssignmentHourlyAggregatorTask.getStartTime(timeNowDate));
    }

    @Test
    public void getEndTime() throws Exception {
        DateFormat format = new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss");

        String timeNowString = "November 11, 2011 05:06:07";
        String expectedEndTimeString = "November 11, 2011 06:00:00";
        Date timeNowDate = format.parse(timeNowString);
        Date expectedEndTimeDate = format.parse(expectedEndTimeString);
        assertEquals(expectedEndTimeDate, AssignmentHourlyAggregatorTask.getEndTime(timeNowDate));

        String timeNowString2 = "November 08, 2016 11:52:12";
        String expectedEndTimeString2 = "November 08, 2016 12:00:00";
        Date timeNowDate2 = format.parse(timeNowString2);
        Date expectedEndTimeDate2 = format.parse(expectedEndTimeString2);
        assertEquals(expectedEndTimeDate2, AssignmentHourlyAggregatorTask.getEndTime(timeNowDate2));
    }
    */
