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
package com.intuit.wasabi.api.pagination.comparators.impl;

import com.intuit.wasabi.api.pagination.comparators.PaginationComparator;
import com.intuit.wasabi.api.pagination.comparators.PaginationComparatorProperty;
import com.intuit.wasabi.experimentobjects.Experiment;

import java.util.Date;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ExperimentComparator extends PaginationComparator<Experiment> {

    public ExperimentComparator() {
        super("-modified");
    }

    private enum Property implements PaginationComparatorProperty<Experiment> {
        app(experiment -> experiment.getApplicationName().toString(), String::compareToIgnoreCase),
        name(experiment -> experiment.getLabel().toString(), String::compareToIgnoreCase),
        creator(Experiment::getCreatorID, String::compareToIgnoreCase),
        created(Experiment::getCreationTime, Date::compareTo),
        started(Experiment::getStartTime, Date::compareTo),
        ended(Experiment::getEndTime, Date::compareTo),
        modified(Experiment::getModificationTime, Date::compareTo),
        state(Experiment::getState, Experiment.State::compareTo)
        ;

        private final Function<Experiment, ?> propertyExtractor;
        private final BiFunction<?, ?, Integer> comparisonFunction;

        <T> Property(Function<Experiment, T> propertyExtractor, BiFunction<T, T, Integer> comparisonFunction) {

            this.propertyExtractor = propertyExtractor;
            this.comparisonFunction = comparisonFunction;
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
        public BiFunction<?, ?, Integer> getComparisonFunction() {
            return comparisonFunction;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(Experiment o1, Experiment o2) {
        return super.compare(o1, o2, Property.class);
    }
}
