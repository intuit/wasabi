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
package com.intuit.wasabi.api.pagination.filters;

import com.intuit.wasabi.exceptions.PaginationException;
import com.intuit.wasabi.experimentobjects.exceptions.ErrorCode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.BiFunction;

public class FilterUtil {

    private static final String TIMEZONE_SEPARATOR = "\t";

    public enum FilterModifier {
        APPEND_TIMEZONEOFFSET((fs, fi) -> fs + TIMEZONE_SEPARATOR + fi.getTimeZoneOffset()),
        ;

        private BiFunction<String, PaginationFilter, String> modifier;

        FilterModifier(BiFunction<String, PaginationFilter, String> modifier) {
            this.modifier = modifier;
        }

        public String apply(String filter, PaginationFilter instance) {
            return modifier.apply(filter, instance);
        }
    }

    public static boolean extractTimeZoneAndTestDate(Date date, String filter) {
        String[] timezoneFilter = extractTimeZone(filter);
        return formatDateTimeLikeUI(date, timezoneFilter[1]).contains(timezoneFilter[0]);
    }

    public static String[] extractTimeZone(String filter) {
        return filter.split(TIMEZONE_SEPARATOR);
    }

    private static String formatDateTimeLikeUI(Date date, String timeZoneOffset) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, YYYY HH:mm:ss a");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT" + timeZoneOffset));
        return sdf.format(date);
    }

    public static Date parseUIDate(String dateString, String timeZoneOffset) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT" + timeZoneOffset));
        try {
            return sdf.parse(dateString);
        } catch (ParseException parseException) {
          throw new PaginationException(ErrorCode.FILTER_KEY_UNPROCESSABLE,
                  "Can not parse date (" + dateString + ") , must be of format MM/dd/yyyy , e.g. 05/23/2014.",
                  parseException);
        }
    }


}
