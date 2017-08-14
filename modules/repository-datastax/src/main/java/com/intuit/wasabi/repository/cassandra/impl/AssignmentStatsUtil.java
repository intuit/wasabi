package com.intuit.wasabi.repository.cassandra.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

class AssignmentStatsUtil {

    private static DateFormat dayFormatter = new SimpleDateFormat("yyyy-MM-dd");
    private static DateFormat hourFormatter = new SimpleDateFormat("HH");
    private static final int UUID_LENGTH = 36;

    /**
     * Helper method takes a time and returns a Date object which is an hour before the input time
     *
     * @param time number of milliseconds between Jan 1, 1970 and a desired time
     * @return Date object
     */
    static Date getLastCompletedHour(long time) {
        return new Date(time - 3600 * 1000);
    }

    /**
     * Helper method takes a Date object, extracts the last hour that has been completed,
     * and returns the hour as an int. (e.g.: returns 23 for date: 2017-08-10 0:05:06)

     * @param date the date containing the last completed hour
     * @return int representing an hour of the day
     */
    static int getHour(Date date) {
        return Integer.parseInt(hourFormatter.format(date));
    }

    /**
     * Helper method takes a Date object and converts it to a String in yyyy-MM-dd form
     *
     * @param date the date object for which the date string is returned
     * @return String representing a day
     */
    static String getDayString(Date date) {
        return dayFormatter.format(date);
    }

    /**
     * Helper method extracts the experiment UUID from an experiment, assignment pair
     *
     * @param pair contains a String consisting of concatenated experiment id and bucket label
     * @return UUID of an experiment
     */
    static UUID getExpUUID(Map.Entry pair){
        if (pair == null) return null;
        String expIDString = pair.getKey().toString().substring(0, UUID_LENGTH);
        return UUID.fromString(expIDString);
    }

    /**
     * Helper method returns the bucket label from an experiment
     *
     * @param pair contains a String consisting of concatenated experiment id and bucket label
     * @return bucket label for this pair
     */
    static String getBucketLabel(Map.Entry pair){
        if (pair == null) return null;
        return pair.getKey().toString().substring(UUID_LENGTH);
    }
}
