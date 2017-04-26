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

import com.intuit.wasabi.api.pagination.filters.FilterUtil;
import com.intuit.wasabi.api.pagination.filters.PaginationFilter;
import com.intuit.wasabi.api.pagination.filters.PaginationFilterProperty;
import com.intuit.wasabi.exceptions.PaginationException;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.exceptions.ErrorCode;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Implements the {@link PaginationFilter} for {@link Experiment}s.
 */
public class ExperimentFilter extends PaginationFilter<Experiment> {

    /**
     * Initializes the ExperimentFilter.
     * <p>
     * Registers modifiers to handle timezones correctly and excludes duplicate and specialized search query fields
     * from fulltext search to avoid difficulties in overly specific queries and to avoid duplications.
     * <p>
     * The fields modified are:<br />
     * {@link Property#creation_time}, {@link Property#start_time}, {@link Property#end_time},
     * {@link Property#modification_time}, {@link Property#date_constraint_start}, {@link Property#date_constraint_end}
     * <p>
     * The fields excluded from fulltext search are:<br />
     * {@link Property#application_name_exact}, {@link Property#state_exact}, {@link Property#date_constraint_start},
     * {@link Property#date_constraint_end}
     */
    public ExperimentFilter() {
        super.registerFilterModifierForProperties(FilterUtil.FilterModifier.APPEND_TIMEZONEOFFSET,
                Property.creation_time, Property.start_time, Property.end_time, Property.modification_time,
                Property.date_constraint_start, Property.date_constraint_end);
        super.excludeFromFulltext(Property.application_name_exact, Property.state_exact, Property.date_constraint_start,
                Property.date_constraint_end, Property.favorite, Property.modification_time, Property.creation_time);
    }

    /**
     * Implementation of {@link PaginationFilterProperty} for {@link Experiment}s.
     *
     * @see PaginationFilterProperty
     */
    public enum Property implements PaginationFilterProperty<Experiment> {
        application_name(experiment -> experiment.getApplicationName().toString(), StringUtils::containsIgnoreCase),
        application_name_exact(experiment -> experiment.getApplicationName().toString(), StringUtils::equalsIgnoreCase),
        experiment_label(experiment -> experiment.getLabel().toString(), StringUtils::containsIgnoreCase),
        created_by(Experiment::getCreatorID, StringUtils::containsIgnoreCase),
        creation_time(Experiment::getCreationTime, FilterUtil::extractTimeZoneAndTestDate),
        start_time(Experiment::getStartTime, FilterUtil::extractTimeZoneAndTestDate),
        end_time(Experiment::getEndTime, FilterUtil::extractTimeZoneAndTestDate),
        modification_time(Experiment::getModificationTime, FilterUtil::extractTimeZoneAndTestDate),
        state(Experiment::getState, (state, filter) -> StringUtils.containsIgnoreCase(state.toString(), filter)),
        state_exact(Experiment::getState, ExperimentFilter::stateTest),
        date_constraint_start(Experiment::getStartTime, ExperimentFilter::constraintTest),
        date_constraint_end(Experiment::getEndTime, ExperimentFilter::constraintTest),
        favorite(Experiment::isFavorite, (isFavorite, filter) -> Boolean.parseBoolean(filter) == isFavorite),
        tags(Experiment::getTags, (tagsSet, filter) -> tagsSet.stream().anyMatch(tag
                -> Arrays.asList(filter.split(";")).contains(tag))),
        tags_and(Experiment::getTags, (tagsSet, filter) -> tagsSet.containsAll(Arrays.asList(filter.split(";"))));

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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean test(Experiment experiment) {
        return super.test(experiment, Property.class);
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
    /*test*/
    static boolean stateTest(Experiment.State state, String filter) {
        if (state == Experiment.State.DELETED) {
            return false;
        }
        switch (filter.toLowerCase()) {
            case "notterminated":
                return state == Experiment.State.DRAFT || state == Experiment.State.RUNNING || state == Experiment.State.PAUSED;
            case "terminated":
                return state == Experiment.State.TERMINATED;
            case "running":
                return state == Experiment.State.RUNNING;
            case "draft":
                return state == Experiment.State.DRAFT;
            case "paused":
                return state == Experiment.State.PAUSED;
            case "any":
                return true;
            default:
                return false;
        }
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
     */
    /*test*/
    static boolean constraintTest(Date experimentDate, String filter) {
        String[] extracted = FilterUtil.extractTimeZone(filter);
        String originalFilter = extracted[0];
        String timeZoneOffset = "+0000";
        try {
            timeZoneOffset = extracted[1];
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }

        String[] structuredFilter = originalFilter.split(":");

        if (structuredFilter.length < 2) {
            if (StringUtils.isBlank(structuredFilter[0]) || structuredFilter[0].equalsIgnoreCase("isAny")) {
                return true;
            }
            throw new PaginationException(ErrorCode.FILTER_KEY_UNPROCESSABLE,
                    "Wrong format for constraint date ( " + filter + " ), " +
                            "use: is[any|on|before|after|between]:MM/dd/yyyy[:MM/dd/yyyy]");
        }

        if (StringUtils.isBlank(structuredFilter[0])) {
            throw new PaginationException(ErrorCode.FILTER_KEY_UNPROCESSABLE,
                    "Wrong format for constraint date ( " + filter + " ), " +
                            "needs one of is[any|on|before|after|between] before the colon.");
        }

        LocalDate experimentLocalDate = FilterUtil.convertDateToOffsetDateTime(experimentDate).toLocalDate();

        if (structuredFilter[0].equalsIgnoreCase("isBetween")) {
            try {
                LocalDate beforeExperimentDate = FilterUtil.parseUIDate(structuredFilter[1], timeZoneOffset)
                        .minusDays(1).toLocalDate();
                LocalDate afterExperimentDate = FilterUtil.parseUIDate(structuredFilter[2], timeZoneOffset)
                        .plusDays(1).toLocalDate();
                return experimentLocalDate.isAfter(beforeExperimentDate)
                        && experimentLocalDate.isBefore(afterExperimentDate);
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                throw new PaginationException(ErrorCode.FILTER_KEY_UNPROCESSABLE,
                        "Wrong format for isBetween ( " + filter + " ), " +
                                "use: isbetween:MM/dd/yyyy:MM/dd/yyyy .", aioobe);
            }
        }

        LocalDate filterDate = FilterUtil.parseUIDate(structuredFilter[1], timeZoneOffset).toLocalDate();
        switch (structuredFilter[0].toLowerCase()) {
            case "isbefore":
                return experimentLocalDate.isBefore(filterDate);
            case "isafter":
                return experimentLocalDate.isAfter(filterDate);
            case "ison":
                return experimentLocalDate.isEqual(filterDate);
        }
        throw new PaginationException(ErrorCode.FILTER_KEY_UNPROCESSABLE,
                "Wrong format: not processable filter format ( " + filter + " ).");
    }
}
