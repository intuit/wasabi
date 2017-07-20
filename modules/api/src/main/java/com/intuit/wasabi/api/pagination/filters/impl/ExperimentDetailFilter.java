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

import com.intuit.wasabi.analyticsobjects.wrapper.ExperimentDetail;
import com.intuit.wasabi.api.pagination.filters.FilterUtil;
import com.intuit.wasabi.api.pagination.filters.PaginationFilter;
import com.intuit.wasabi.api.pagination.filters.PaginationFilterProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Implements the {@link PaginationFilter} for {@link com.intuit.wasabi.analyticsobjects.wrapper.ExperimentDetail}s.
 */
public class ExperimentDetailFilter extends PaginationFilter<ExperimentDetail> {

    /**
     * Initializes the ExperimentDetailFilter.
     * <p>
     * Registers modifiers to handle timezones correctly and excludes duplicate and specialized search query fields
     * from fulltext search to avoid difficulties in overly specific queries and to avoid duplications.
     */
    public ExperimentDetailFilter() {
        super.registerFilterModifierForProperties(FilterUtil.FilterModifier.APPEND_TIMEZONEOFFSET, Property.mod_time,
                Property.start_time, Property.end_time, ExperimentFilter.Property.date_constraint_start,
                ExperimentFilter.Property.date_constraint_end);
        super.excludeFromFulltext(Property.application_name_exact, Property.mod_time,
                Property.end_time, Property.start_time, Property.favorite, Property.isControl,
                Property.date_constraint_start, Property.date_constraint_end);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean test(ExperimentDetail experimentFilter) {
        return super.test(experimentFilter, Property.class);
    }

    public enum Property implements PaginationFilterProperty<ExperimentDetail> {
        state(ExperimentDetail::getState, (state, filter) -> StringUtils.containsIgnoreCase(state.toString(), filter)),
        state_exact(ExperimentDetail::getState, ExperimentFilter::stateTest),
        experiment_label(experimentDetail -> experimentDetail.getLabel().toString(), StringUtils::containsIgnoreCase),
        application_name(experimentDetail -> experimentDetail.getApplicationName().toString(), StringUtils::containsIgnoreCase),
        application_name_exact(experimentDetail -> experimentDetail.getApplicationName().toString(), StringUtils::equalsIgnoreCase),
        favorite(ExperimentDetail::isFavorite, (isFavorite, filter) -> Boolean.parseBoolean(filter) == isFavorite),
        bucket_label(ExperimentDetail::getBuckets, (bucketDetails, filter) ->
                bucketDetails.stream().anyMatch(bucketDetail ->
                        StringUtils.containsIgnoreCase(bucketDetail.getLabel().toString(), filter))),
        isControl(ExperimentDetail::getBuckets, ((bucketDetails, filter) ->
                bucketDetails.stream().anyMatch(bucketDetail -> bucketDetail.isControl() == Boolean.valueOf(filter)))),
        mod_time(ExperimentDetail::getModificationTime, FilterUtil::extractTimeZoneAndTestDate),
        start_time(ExperimentDetail::getStartTime, FilterUtil::extractTimeZoneAndTestDate),
        end_time(ExperimentDetail::getEndTime, FilterUtil::extractTimeZoneAndTestDate),
        date_constraint_start(ExperimentDetail::getStartTime, ExperimentFilter::constraintTest),
        date_constraint_end(ExperimentDetail::getEndTime, ExperimentFilter::constraintTest),
        tags(ExperimentDetail::getTags, (tagsSet, filter) -> tagsSet.stream().anyMatch(tag
                -> Arrays.asList(filter.split(";")).contains(tag))),
        tags_and(ExperimentDetail::getTags, (tagsSet, filter) -> tagsSet.containsAll(Arrays.asList(filter.split(";"))));

        private final Function<ExperimentDetail, ?> propertyExtractor;
        private final BiPredicate<?, String> filterPredicate;

        /**
         * Creates a Property.
         *
         * @param propertyExtractor the property extractor
         * @param filterPredicate   the filter predicate
         * @param <T>               the property type
         */
        <T> Property(Function<ExperimentDetail, T> propertyExtractor, BiPredicate<T, String> filterPredicate) {
            this.propertyExtractor = propertyExtractor;
            this.filterPredicate = filterPredicate;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Function<ExperimentDetail, ?> getPropertyExtractor() {
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
