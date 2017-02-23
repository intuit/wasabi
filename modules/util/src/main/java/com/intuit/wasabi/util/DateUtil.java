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
package com.intuit.wasabi.util;

import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;
import static java.util.TimeZone.getTimeZone;
import static org.slf4j.LoggerFactory.getLogger;

public class DateUtil {

    private static final String[] ISO_8601_FORMATS = {
            "yyyy-MM-dd'T'HH:mm:ssXX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
    };
    private static final Logger LOGGER = getLogger(DateUtil.class);

    //Add private constructor to not allowing people to create new instance of this class
    //since everything is static method here
    private DateUtil() {
    }

    /**
     * Parses an ISOTimestamp String to a Timestamp object
     * returns null, if the format is not recognized
     *
     * Acceptable formats are:
     * <ul>
     * <li>"yyyy-MM-dd'T'HH:mm:ssXX"</li>
     * <li>"yyyy-MM-dd'T'HH:mm:ss"</li>
     * <li>"yyyy-MM-dd"</li>
     * </ul>
     *
     * @param timestampStr The input string in one of the formats given above
     * @return Timestamp object, if successful; null if unsuccessful
     */
    public static Date parseISOTimestamp(String timestampStr) {
        for (String iso8601Format : ISO_8601_FORMATS) {
            try {
                SimpleDateFormat dateFmt = new SimpleDateFormat(iso8601Format);
                /* The default in java.text.DateFormat.parse() is to treat the condensed ("yyyy-MM-DDThh:mm:ss")
                 * timestamp string as localtime. By explicitly setting the timezone to "UTC" we are forcing
                 * interpretation of all input times as UTC.
                 */

                dateFmt.setTimeZone(getTimeZone("UTC"));
                dateFmt.setLenient(false);

                return dateFmt.parse(timestampStr);
            } catch (Exception ex) {
                LOGGER.error("unable to parse date", ex);
            }
        }

        return null;
    }

    /**
     * Creates a calendar with hour, minute, second, millisecond set to zero for given Date.
     *
     * @param date the date to use
     * @return calendar object create from date and set to midnight
     */
    public static Calendar createCalendarMidnight(final Date date) {
        Calendar cal = new GregorianCalendar(getTimeZone("GMT+0"));

        cal.setTime(date);
        cal.set(HOUR_OF_DAY, 0);
        cal.set(MINUTE, 0);
        cal.set(SECOND, 0);
        cal.set(MILLISECOND, 0);

        return cal;
    }

    /**
     * Returns current time in UTC timezone as string
     * @return string
     */
    public static String getUTCTime() {
        Date currentTime = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, yyyy hh:mm:ss a z");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(currentTime);
    }
}
