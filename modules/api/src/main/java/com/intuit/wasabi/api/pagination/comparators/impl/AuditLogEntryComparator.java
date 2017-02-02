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
import com.intuit.wasabi.auditlogobjects.AuditLogAction;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;

import java.util.Calendar;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Implements the {@link PaginationComparator} for {@link AuditLogEntry} objects.
 */
public class AuditLogEntryComparator extends PaginationComparator<AuditLogEntry> {

    /**
     * Initializes an AuditLogEntryComparator.
     * <p>
     * Sets the default sort order to descending time, that means
     * the most recent events are first.
     */
    public AuditLogEntryComparator() {
        super("-time");
    }

    /**
     * Implementation of {@link PaginationComparatorProperty} for {@link AuditLogEntry}s.
     *
     * @see PaginationComparatorProperty
     */
    private enum Property implements PaginationComparatorProperty<AuditLogEntry> {
        firstname(auditLogEntry -> auditLogEntry.getUser().getFirstName(), String::compareToIgnoreCase),
        lastname(auditLogEntry -> auditLogEntry.getUser().getLastName(), String::compareToIgnoreCase),
        user(auditLogEntry -> auditLogEntry.getUser().getUsername().toString(), String::compareToIgnoreCase),
        username(auditLogEntry -> auditLogEntry.getUser().getUsername().toString(), String::compareToIgnoreCase),
        userid(auditLogEntry -> auditLogEntry.getUser().getUserId(), String::compareToIgnoreCase),
        mail(auditLogEntry -> auditLogEntry.getUser().getEmail(), String::compareToIgnoreCase),
        action(AuditLogAction::getDescription, String::compareToIgnoreCase),
        experiment(auditLogEntry -> auditLogEntry.getExperimentLabel().toString(), String::compareToIgnoreCase),
        bucket(auditLogEntry -> auditLogEntry.getBucketLabel().toString(), String::compareToIgnoreCase),
        app(auditLogEntry -> auditLogEntry.getApplicationName().toString(), String::compareToIgnoreCase),
        time(AuditLogEntry::getTime, Calendar::compareTo),
        attribute(AuditLogEntry::getChangedProperty, String::compareToIgnoreCase),
        before(AuditLogEntry::getBefore, String::compareToIgnoreCase),
        after(AuditLogEntry::getAfter, String::compareToIgnoreCase),
        description(AuditLogAction::getDescription, String::compareToIgnoreCase),;

        private final Function<AuditLogEntry, ?> propertyExtractor;
        private final BiFunction<?, ?, Integer> comparisonFunction;

        /**
         * Creates a Property.
         *
         * @param propertyExtractor  the property extractor
         * @param comparisonFunction the comparison function
         * @param <T>                the property type
         */
        <T> Property(Function<AuditLogEntry, T> propertyExtractor, BiFunction<T, T, Integer> comparisonFunction) {
            this.propertyExtractor = propertyExtractor;
            this.comparisonFunction = comparisonFunction;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Function<AuditLogEntry, ?> getPropertyExtractor() {
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
    public int compare(AuditLogEntry left, AuditLogEntry right) {
        return super.compare(left, right, Property.class);
    }
}
