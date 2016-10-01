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

import com.intuit.wasabi.api.pagination.filters.FilterUtil;
import com.intuit.wasabi.api.pagination.filters.PaginationFilter;
import com.intuit.wasabi.api.pagination.filters.PaginationFilterProperty;
import com.intuit.wasabi.exceptions.PaginationException;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.exceptions.ErrorCode;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Implements the {@link PaginationFilter} for {@link Experiment}s.
 */
public class ExperimentFilter extends PaginationFilter<Experiment> {

    /**
     * Holds a map to allow filtering by state with complex combinations.
     * Each key is a possible filter value, and maps to allowed states for that filter value.
     */
    private static final Map<String, EnumSet<Experiment.State>> stateMap = new HashMap<>(6);

    static {
        stateMap.put("notterminated", EnumSet.of(Experiment.State.DRAFT, Experiment.State.RUNNING, Experiment.State.PAUSED));
        stateMap.put("terminated", EnumSet.of(Experiment.State.TERMINATED));
        stateMap.put("running", EnumSet.of(Experiment.State.RUNNING));
        stateMap.put("draft", EnumSet.of(Experiment.State.DRAFT));
        stateMap.put("paused", EnumSet.of(Experiment.State.PAUSED));
        stateMap.put("any", EnumSet.allOf(Experiment.State.class));
    }

    /**
     * Initializes the ExperimentFilter.
     * <p>
     * Registers modifiers to handle timezones correctly and excludes duplicate and specialized search query fields
     * from fulltext search to avoid difficulties in overly specific queries and to avoid duplications.
     * <p>
     * The fields modified are:<br />
     * {@link Property#CREATION_TIME}, {@link Property#START_TIME}, {@link Property#END_TIME},
     * {@link Property#MODIFICATION_TIME}, {@link Property#DATE_CONSTRAINT_START}, {@link Property#DATE_CONSTRAINT_END}
     * <p>
     * The fields excluded from fulltext search are:<br />
     * {@link Property#APPLICATION_NAME_EXACT}, {@link Property#STATE_EXACT}, {@link Property#DATE_CONSTRAINT_START},
     * {@link Property#DATE_CONSTRAINT_END}
     */
    public ExperimentFilter() {
        super.registerFilterModifierForProperties(FilterUtil.FilterModifier.APPEND_TIMEZONEOFFSET,
                Property.CREATION_TIME, Property.START_TIME, Property.END_TIME, Property.MODIFICATION_TIME,
                Property.DATE_CONSTRAINT_START, Property.DATE_CONSTRAINT_END);
        super.excludeFromFulltext(Property.APPLICATION_NAME_EXACT, Property.STATE_EXACT, Property.DATE_CONSTRAINT_START,
                Property.DATE_CONSTRAINT_END);
    }

    /**
     * A filter function used to test experiments for specific states with the filter key {@code state_exact}.
     * Allowed filter values (case insensitive) and their results are:
     * <p>
     * <ul>
     * <li><b>{@code notTerminated}</b>: {@code true} if the experiment state is either {@code DRAFT},
     * {@code RUNNING}, or {@code PAUSED}.</li>
     * <li><b>{@code terminated}</b>: {@code true} if the experiment state is {@code TERMINATED}.</li>
     * <li><b>{@code running}</b>: {@code true} if the experiment state is {@code RUNNING}.</li>
     * <li><b>{@code draft}</b>: {@code true} if the experiment state is {@code DRAFT}.</li>
     * <li><b>{@code paused}</b>: {@code true} if the experiment state is {@code PAUSED}.</li>
     * <li><b>{@code any}</b>: {@code true}.</li>
     * <li>default/invalid filter values: {@code false}.</li>
     * </ul>
     *
     * @param state  the experiment's state.
     * @param filter the filter value to check, see above for details.
     * @return true/false depending on the state check, see above for details.
     */
    static boolean stateTest(Experiment.State state, String filter) {
        return state != Experiment.State.DELETED &&
                stateMap.getOrDefault(filter, EnumSet.noneOf(Experiment.State.class)).contains(state);
    }

    /**
     * Tests dates for more sophisticated constraints than just partial string matches.
     * <p>
     * <p>
     * The a filter to check these constraints has to be of the following form:<br />
     * {@code is[any|on|before|after|between]:MM/dd/yyyy[:MM/dd/yyyy]}
     * <p>
     * For example to check whether the start date was before March 15, 2014 you would use a filter like:<br />
     * {@code date_constraint_start=isbefore:03/15/2014}
     * <p>
     * To check if an end date lies between (inclusive!) May 4, 2013 and July 4, 2014 you would use a
     * filter like:<br />
     * {@code date_constraint_end=isbetween:05/04/2013:07/04/2014}
     * <p>
     * Note that {@code isbetween} is the only value taking two dates and {@code isany} as well as empty strings
     * and {@code null} always return true.
     *
     * @param experimentDate the experiment date value to test
     * @param filter         the filter
     * @return true if the constraint is fulfilled
     * @throws PaginationException on mal-formatted filters
     */
    static boolean constraintDateTest(Date experimentDate, String filter) {
        String[] extracted = FilterUtil.extractTimeZone(filter);
        String originalFilter = extracted[0];
        String timeZoneOffset = extracted.length > 1 ? extracted[1] : "+0000";

        String[] structuredFilter = originalFilter.split(":");

        if (structuredFilter.length < 2) {
            return constraintDateTestShortFilter(structuredFilter[0]);
        }

        if (StringUtils.isBlank(structuredFilter[0])) {
            throw new PaginationException(ErrorCode.FILTER_KEY_UNPROCESSABLE,
                    "Wrong format for constraint date ( " + filter + " ), " +
                            "needs one of is[any|on|before|after|between] before the colon.");
        }

        LocalDate experimentLocalDate = FilterUtil.convertDateToOffsetDateTime(experimentDate).toLocalDate();
        if ("isBetween".equalsIgnoreCase(structuredFilter[0])) {
            return constraintDateTestInBetween(structuredFilter, experimentLocalDate, timeZoneOffset, filter);
        }
        return constraintDateTestRelation(experimentLocalDate, structuredFilter[0], structuredFilter[1], timeZoneOffset, filter);
    }

    /**
     * Tests date constrains for short filters, so when the length of the filter mask was only 1.
     * Returns true if the filter is either blank or "isAny". Otherwise it throws an exception as the filter
     * is not well formatted.
     *
     * @param filter the filter to check
     * @return true if the filter is empty or "isAny", else throws
     * @throws PaginationException on mal-formatted filters
     */
    private static boolean constraintDateTestShortFilter(String filter) {
        if (StringUtils.isBlank(filter) || "isAny".equalsIgnoreCase(filter)) {
            return true;
        }
        throw new PaginationException(ErrorCode.FILTER_KEY_UNPROCESSABLE,
                "Wrong format for constraint date ( " + filter + " ), " +
                        "use: is[any|on|before|after|between]:MM/dd/yyyy[:MM/dd/yyyy]");
    }

    /**
     * Parses before and after and checks if experimentLocalDate lies in between (inclusive).
     *
     * @param filterArray         first element should be isBetween, followed left boundary and right boundary.
     * @param experimentLocalDate the date to check against
     * @param timeZoneOffset      the timezone offset to handle dates properly
     * @param originalFilter      the original filter string to provide meaningful error messages
     * @return true if the experiment date is in between before and after.
     * @throws PaginationException on mal-formatted filters
     */
    private static boolean constraintDateTestInBetween(String[] filterArray, LocalDate experimentLocalDate, String timeZoneOffset, String originalFilter) {
        try {
            LocalDate beforeExperimentDate = FilterUtil.parseUIDate(filterArray[1], timeZoneOffset)
                    .minusDays(1).toLocalDate();
            LocalDate afterExperimentDate = FilterUtil.parseUIDate(filterArray[2], timeZoneOffset)
                    .plusDays(1).toLocalDate();
            return experimentLocalDate.isAfter(beforeExperimentDate)
                    && experimentLocalDate.isBefore(afterExperimentDate);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            throw new PaginationException(ErrorCode.FILTER_KEY_UNPROCESSABLE,
                    "Wrong format for isBetween ( " + originalFilter + " ), " +
                            "use: isbetween:MM/dd/yyyy:MM/dd/yyyy .", aioobe);
        }
    }

    /**
     * Tests if a date is before, after or on a specific date.
     * If key is neither of "isbefore", "isafter", or "ison", an exception will be thrown.
     *
     * @param experimentLocalDate the date to check against
     * @param key                 "isbefore", "isafter", or "ison"
     * @param timeZoneOfffset     the timezone offset to handle dates properly
     * @param originalFilter      the original filter string to provide meaningful error messages
     * @return the result of the test
     * @throws PaginationException on mal-formatted filters
     */
    private static boolean constraintDateTestRelation(LocalDate experimentLocalDate, String key, String date, String timeZoneOfffset, String originalFilter) {
        LocalDate filterDate = FilterUtil.parseUIDate(date, timeZoneOfffset).toLocalDate();
        switch (key.toLowerCase()) {
            case "isbefore":
                return experimentLocalDate.isBefore(filterDate);
            case "isafter":
                return experimentLocalDate.isAfter(filterDate);
            case "ison":
                return experimentLocalDate.isEqual(filterDate);
            default:
                throw new PaginationException(ErrorCode.FILTER_KEY_UNPROCESSABLE,
                        "Wrong format: not processable filter format ( " + originalFilter + " ).");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean test(Experiment experiment) {
        return super.test(experiment, Property.class);
    }

    /**
     * Implementation of {@link PaginationFilterProperty} for {@link Experiment}s.
     *
     * @see PaginationFilterProperty
     */
    public enum Property implements PaginationFilterProperty<Experiment> {
        APPLICATION_NAME(experiment -> experiment.getApplicationName().toString(), StringUtils::containsIgnoreCase),
        APPLICATION_NAME_EXACT(experiment -> experiment.getApplicationName().toString(), StringUtils::equalsIgnoreCase),
        EXPERIMENT_NAME(experiment -> experiment.getLabel().toString(), StringUtils::containsIgnoreCase),
        CREATED_BY(Experiment::getCreatorID, StringUtils::containsIgnoreCase),
        CREATION_TIME(Experiment::getCreationTime, FilterUtil::extractTimeZoneAndTestDate),
        START_TIME(Experiment::getStartTime, FilterUtil::extractTimeZoneAndTestDate),
        END_TIME(Experiment::getEndTime, FilterUtil::extractTimeZoneAndTestDate),
        MODIFICATION_TIME(Experiment::getModificationTime, FilterUtil::extractTimeZoneAndTestDate),
        STATE(Experiment::getState, (state, filter) -> StringUtils.containsIgnoreCase(state.toString(), filter)),
        STATE_EXACT(Experiment::getState, ExperimentFilter::stateTest),
        DATE_CONSTRAINT_START(Experiment::getStartTime, ExperimentFilter::constraintDateTest),
        DATE_CONSTRAINT_END(Experiment::getEndTime, ExperimentFilter::constraintDateTest),
        FAVORITE(Experiment::isFavorite, (isFavorite, filter) -> Boolean.parseBoolean(filter) == isFavorite);

        private final Function<Experiment, ?> propertyExtractor;
        private final BiPredicate<?, String> filterPredicate;

        /**
         * Creates a Property.
         *
         * @param propertyExtractor the property extractor
         * @param filterPredicate   the filter predicate
         * @param <T>               the property type
         */
        <T> Property(Function<Experiment, T> propertyExtractor, BiPredicate<T, String> filterPredicate) {
            this.propertyExtractor = propertyExtractor;
            this.filterPredicate = filterPredicate;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Function<Experiment, ?> getPropertyExtractor() {
            return propertyExtractor;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public BiPredicate<?, String> getFilterPredicate() {
            return filterPredicate;
        }
    }
}
