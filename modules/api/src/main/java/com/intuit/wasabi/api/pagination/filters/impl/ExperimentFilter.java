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
import com.intuit.wasabi.util.DateUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class ExperimentFilter extends PaginationFilter<Experiment> {

    public ExperimentFilter() {
        super.registerFilterModifierForProperties(FilterUtil.FilterModifier.APPEND_TIMEZONEOFFSET,
                Property.creation_time, Property.start_time, Property.end_time, Property.modification_time,
                Property.date_constraint_start, Property.date_constraint_end);
        super.excludeFromFulltext(Property.application_name_exact, Property.state_exact, Property.date_constraint_start,
                Property.date_constraint_end);
    }

    private enum Property implements PaginationFilterProperty<Experiment> {
        application_name(experiment -> experiment.getApplicationName().toString(), StringUtils::containsIgnoreCase),
        application_name_exact(experiment -> experiment.getApplicationName().toString(), StringUtils::equalsIgnoreCase),
        experiment_name(experiment -> experiment.getLabel().toString(), StringUtils::containsIgnoreCase),
        created_by(Experiment::getCreatorID, StringUtils::containsIgnoreCase),
        creation_time(Experiment::getCreationTime, FilterUtil::extractTimeZoneAndTestDate),
        start_time(Experiment::getStartTime, FilterUtil::extractTimeZoneAndTestDate),
//        sampling_percent(Experiment::getSamplingPercent, ),
        end_time(Experiment::getEndTime, FilterUtil::extractTimeZoneAndTestDate),
        modification_time(Experiment::getModificationTime, FilterUtil::extractTimeZoneAndTestDate),
        state(Experiment::getState, (state, filter) -> StringUtils.containsIgnoreCase(state.toString(), filter)),
        state_exact(Experiment::getState, ExperimentFilter::statusTest),
        date_constraint_start(Experiment::getStartTime, ExperimentFilter::constraintTest),
        date_constraint_end(Experiment::getEndTime, ExperimentFilter::constraintTest)
        ;

        private final Function<Experiment, ?> propertyExtractor;
        private final BiPredicate<?, String> filterPredicate;

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

    private static boolean statusTest(Experiment.State state, String filter) {
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
            default:
                return true;
        }
    }

    private static boolean constraintTest(Date experimentDate, String filter) {
        String[] extracted = FilterUtil.extractTimeZone(filter);
        String originalFilter = extracted[0];
        String timeZoneOffset = extracted[1];

        String[] structuredFilter = originalFilter.split(":");

        if (structuredFilter[0].equalsIgnoreCase("isAny")) {
            return true;
        }

        if (structuredFilter.length < 2) {
            throw new PaginationException(ErrorCode.FILTER_KEY_UNPROCESSABLE,
                    "Wrong format for constraint date (" + filter + "), " +
                    "use: is[any|on|before|after|between]:MM/dd/yyyy[:MM/dd/yyyy]");
        }

        experimentDate = DateUtil.createCalendarMidnight(experimentDate).getTime();

        if (structuredFilter[0].equalsIgnoreCase("isBetween")) {
            try {
                Date beforeEperimentDate = FilterUtil.parseUIDate(structuredFilter[1], timeZoneOffset);
                Date afterEperimentDate = FilterUtil.parseUIDate(structuredFilter[2], timeZoneOffset);
                return experimentDate.after(beforeEperimentDate) && experimentDate.before(afterEperimentDate);
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                throw new PaginationException(ErrorCode.FILTER_KEY_UNPROCESSABLE,
                        "Wrong format for inBetween (" + filter + "), use: isbetween:MM/dd/yyyy:MM/dd/yyyy .",
                        aioobe);
            }
        }

        Date filterDate = FilterUtil.parseUIDate(structuredFilter[1], timeZoneOffset);
        switch (structuredFilter[0].toLowerCase()) {
            case "isbefore":
                return experimentDate.before(filterDate);
            case "isafter":
                return experimentDate.after(filterDate);
            case "ison":
                return experimentDate.compareTo(filterDate) == 0;
        }

        return false;
    }
}
