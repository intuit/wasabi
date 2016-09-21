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

import com.intuit.wasabi.analyticsobjects.wrapper.ExperimentDetail;
import com.intuit.wasabi.api.pagination.filters.FilterUtil;
import com.intuit.wasabi.api.pagination.filters.PaginationFilter;
import com.intuit.wasabi.api.pagination.filters.PaginationFilterProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.function.BiPredicate;
import java.util.function.Function;

import static cern.clhep.Units.s;

/**
 * Implements the {@link PaginationFilter} for {@link com.intuit.wasabi.analyticsobjects.wrapper.ExperimentDetail}s.
 */
public class ExperimentDetailFilter extends PaginationFilter<ExperimentDetail> {

    public enum Property implements PaginationFilterProperty<ExperimentDetail>{
        state(ExperimentDetail::getState, (state, filter) -> StringUtils.containsIgnoreCase(state.toString(), filter)),
        state_exact(ExperimentDetail::getState, ExperimentFilter::stateTest),
        experiment_label(experimentDetail -> experimentDetail.getLabel().toString(), StringUtils::containsIgnoreCase),
        application_name(experimentDetail -> experimentDetail.getAppName().toString(), StringUtils::containsIgnoreCase),
        application_name_exact(experimentDetail -> experimentDetail.getAppName().toString(), StringUtils::equalsIgnoreCase),
        favorite(ExperimentDetail::isFavorite, (isFavorite, filter) -> Boolean.parseBoolean(filter) == isFavorite),
        bucket_label(ExperimentDetail::getBuckets, (bucketDetails, filter) ->
                bucketDetails.stream().anyMatch(bucketDetail -> StringUtils.containsIgnoreCase(bucketDetail.getLabel().toString(), filter))),
        mod_time(experimentDetail -> experimentDetail.getModificationTime(), FilterUtil::extractTimeZoneAndTestDate);

        private final Function<ExperimentDetail, ?> propertyExtractor;
        private final BiPredicate<?, String> filterPredicate;

        /**
         * Creates a Property.
         *
         * @param propertyExtractor the property extractor
         * @param filterPredicate the filter predicate
         * @param <T> the property type
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

    /**
     * Initializes the ExperimentDetailFilter.
     *
     * Registers modifiers to handle timezones correctly and excludes duplicate and specialized search query fields
     * from fulltext search to avoid difficulties in overly specific queries and to avoid duplications.
     *
     */
    public ExperimentDetailFilter() {
        super.registerFilterModifierForProperties(FilterUtil.FilterModifier.APPEND_TIMEZONEOFFSET, Property.mod_time);
        super.excludeFromFulltext(Property.application_name_exact, Property.mod_time);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean test(ExperimentDetail experimentFilter) {
        return super.test(experimentFilter, Property.class);
    }


}
