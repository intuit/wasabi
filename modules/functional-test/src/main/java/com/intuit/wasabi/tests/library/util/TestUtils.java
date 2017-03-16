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
package com.intuit.wasabi.tests.library.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Provides several static methods useful for the test classes.
 */
public class TestUtils {

    /**
     * Returns the current time as a String formatted as needed.
     *
     * @return a correctly formatted time String
     */
    public static String currentTimeString() {
        return TestUtils.relativeTimeString(0);
    }

    /**
     * Returns the current time + days as a String formatted as needed.
     *
     * @param days the number of days relative to now
     * @return a correctly formatted time String
     */
    public static String relativeTimeString(int days) {
        GregorianCalendar gregCal = new GregorianCalendar();
        gregCal.add(Calendar.DAY_OF_YEAR, days);
        return TestUtils.getTimeString(gregCal);
    }

    /**
     * Returns the time given by {@code cal} as a String formatted as needed.
     *
     * @param cal the time to format
     * @return a correctly formatted time String
     */
    public static String getTimeString(Calendar cal) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(cal.getTime()).replace("+0000", "Z");
    }

    /**
     * Wraps a JSON String into a JSON object.
     * <p>
     * If the JSON String where {@code str} and the JSON object {@code obj} the resulting
     * JSON String would be: {@code {"obj":str}}.
     *
     * @param jsonString the JSON String to wrap
     * @param object     the object to wrap into
     * @return the new JSON string
     */
    public static String wrapJsonIntoObject(String jsonString, String object) {
        return "{\"" + object + "\":" + jsonString + "}";
    }

    /**
     * Turns a csv with a header row into a JSON array.
     *
     * @param csv            the csv string
     * @param fieldSeparator the column separators
     * @return a String representing a JSON array
     */
    public static String csvToJsonArray(String csv, String fieldSeparator) {
        String[] lines = csv.split(System.getProperty("line.separator"));
        if (lines.length == 0)
            return "[]";
        String[] header = lines[0].split(fieldSeparator);

        List<JsonElement> jsonObjects = new ArrayList<>(lines.length - 1);

        Gson gson = new GsonBuilder().create();
        Map<String, String> jsonMap = new HashMap<>();
        for (int i = 1; i < lines.length; ++i) {
            String[] fields = lines[i].split(fieldSeparator);
            if (fields.length != header.length) {
                continue;
            }

            for (int j = 0; j < header.length; j++) {
                jsonMap.put(header[j], fields[j]);
            }
            jsonObjects.add(gson.toJsonTree(jsonMap));
        }
        return gson.toJson(jsonObjects);
    }

    /**
     * Returns a calendar for the given time string. The time string should be formatted
     * {@code yyyy-MM-dd'T'hh:mm:ssZ}, where {@code Z} is a literal (and stands for +0000).
     * It is assumed to be in UTC.
     *
     * @param timestring the time string
     * @return a calendar representing the date assumed to be UTC
     * @throws ParseException if the time string is not correctly formatted.
     */
    public static Calendar parseTime(String timestring) throws java.text.ParseException {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(timestring)).toEpochMilli());
        return cal;
    }

    /**
     * Returns a UI formatted date string (M/d/y).
     *
     * @param datetime datetime to format
     */
    public static String formatDateForUI(LocalDateTime datetime) {
        return DateTimeFormatter.ofPattern("M/d/y").format(datetime);
    }

    /**
     * Parse UI formatted date string (m/d/Y).
     */
    public static LocalDateTime parseDateFromUI(String datetime) {
        return LocalDateTime.from(DateTimeFormatter.ofPattern("M/d/y").parse(datetime));
    }

}
