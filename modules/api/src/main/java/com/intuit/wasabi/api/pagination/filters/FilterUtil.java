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
import org.apache.commons.lang3.StringUtils;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.function.BiFunction;

/**
 * Bundles methods useful for dealing with different filter challenges.
 */
public class FilterUtil {

    /**
     * Used to append (and split) the timezone to (and from) the filter.
     */
    /*test*/ static final String TIMEZONE_SEPARATOR = "\t";

    /**
     * Modifiers to register with
     * {@link PaginationFilter#registerFilterModifierForProperties(FilterModifier, PaginationFilterProperty[])}.
     */
    public enum FilterModifier {
        APPEND_TIMEZONEOFFSET((fs, fi) -> fs + TIMEZONE_SEPARATOR + fi.getTimeZoneOffset()),;

        private BiFunction<String, PaginationFilter, String> modifier;

        FilterModifier(BiFunction<String, PaginationFilter, String> modifier) {
            this.modifier = modifier;
        }

        public String apply(String filter, PaginationFilter instance) {
            return modifier.apply(filter, instance);
        }
    }

    /**
     * Extracts the timezone and original filter string and performs a partial match checking on the date.
     *
     * @param date   the date to check
     * @param filter the filter string
     * @return the result of a partial match
     */
    public static boolean extractTimeZoneAndTestDate(Date date, String filter) {
        String[] timezoneAndFilter = extractTimeZone(filter);
        return StringUtils.containsIgnoreCase(formatDateTimeAsUI(convertDateToOffsetDateTime(date),
                timezoneAndFilter[1]), timezoneAndFilter[0]);
    }

    /**
     * Converts the old {@link Date} to a new {@link OffsetDateTime}, taking the UTC offset into account.
     *
     * @param date the date to convert
     * @return the converted date
     */
    public static OffsetDateTime convertDateToOffsetDateTime(Date date) {
        return OffsetDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
    }

    /**
     * Complements {@link FilterModifier#APPEND_TIMEZONEOFFSET}: Splits the filter at {@link #TIMEZONE_SEPARATOR} and returns the resulting array.
     *
     * @param filter the filter
     * @return both components, the filter and the timezone, in an array (in that order)
     */
    public static String[] extractTimeZone(String filter) {
        return filter.split(TIMEZONE_SEPARATOR);
    }

    /**
     * Formats a date as it is shown in the UI to allow for matching searches on date fields.
     * Needs the requesting user's timezone offset to UTC for correct matches.
     *
     * @param date           the date
     * @param timeZoneOffset the timezone offset to UTC
     * @return a timezone offset adjusted string of the UI pattern {@code MMM d, YYYY HH:mm:ss a}.
     */
    /*test*/
    static String formatDateTimeAsUI(OffsetDateTime date, String timeZoneOffset) {
        try {
            return date.format(DateTimeFormatter.ofPattern("MMM d, YYYY HH:mm:ss a")
                    .withZone(ZoneId.ofOffset("UTC", ZoneOffset.of(timeZoneOffset))));
        } catch (DateTimeException dateTimeException) {
            throw new PaginationException(ErrorCode.FILTER_KEY_UNPROCESSABLE,
                    "Wrong format: Can not parse timezone '" + timeZoneOffset + "' or date " + date.toString()
                            + " properly.", dateTimeException);
        }
    }

    /**
     * Parses a UI date of the format {@code M/d/yZ} (See {@link DateTimeFormatter}) as it is allowed to be
     * entered in advanced search fields in the UI. Throws a {@link PaginationException} on failure, notifying the user.
     *
     * @param dateString     the string as received from the UI
     * @param timeZoneOffset the user's timezone offset
     * @return a parsed date
     */
    public static OffsetDateTime parseUIDate(String dateString, String timeZoneOffset) {
        try {
            TemporalAccessor tempAccessor = DateTimeFormatter.ofPattern("M/d/yyyyZ").parse(dateString + timeZoneOffset);
            return OffsetDateTime.of(java.time.LocalDate.from(tempAccessor), LocalTime.MIDNIGHT, ZoneOffset.UTC);
        } catch (DateTimeParseException parseException) {
            throw new PaginationException(ErrorCode.FILTER_KEY_UNPROCESSABLE,
                    "Wrong format: Can not parse date (" + dateString + ") , must be of " +
                            "format MM/dd/yyyy , e.g. 05/23/2014 or 4/7/2013",
                    parseException);
        }
    }


}
