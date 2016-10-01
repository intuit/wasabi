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
import com.intuit.wasabi.auditlogobjects.AuditLogAction;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import org.apache.commons.lang3.StringUtils;

import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Implements the {@link PaginationFilter} for {@link AuditLogEntry}s.
 */
public class AuditLogEntryFilter extends PaginationFilter<AuditLogEntry> {

    /**
     * Initializes the AuditLogEntryFilter.
     *
     * Registers the {@link com.intuit.wasabi.api.pagination.filters.FilterUtil.FilterModifier#APPEND_TIMEZONEOFFSET}
     * for {@link Property#TIME} to handle timezones.
     */
    public AuditLogEntryFilter() {
        super.registerFilterModifierForProperties(FilterUtil.FilterModifier.APPEND_TIMEZONEOFFSET,
                Property.TIME);
    }

    /**
     * Implementation of {@link PaginationFilterProperty} for {@link AuditLogEntry}s.
     *
     * @see PaginationFilterProperty
     */
    private enum Property implements PaginationFilterProperty<AuditLogEntry> {
        FIRSTNAME(auditLogEntry -> auditLogEntry.getUser().getFirstName(), StringUtils::containsIgnoreCase),
        LASTNAME(auditLogEntry -> auditLogEntry.getUser().getLastName(), StringUtils::containsIgnoreCase),
        USERNAME(auditLogEntry -> auditLogEntry.getUser().getUsername().getUsername(), StringUtils::containsIgnoreCase),
        USERID(auditLogEntry -> auditLogEntry.getUser().getUserId(), StringUtils::containsIgnoreCase),
        MAIL(auditLogEntry -> auditLogEntry.getUser().getEmail(), StringUtils::containsIgnoreCase),
        ACTION(auditLogEntry -> auditLogEntry.getAction().toString(), StringUtils::containsIgnoreCase),
        DESCRIPTION(AuditLogAction::getDescription, StringUtils::containsIgnoreCase),
        EXPERIMENT(auditLogEntry -> auditLogEntry.getExperimentLabel().toString(), StringUtils::containsIgnoreCase),
        EXPERIMENTID(auditLogEntry -> auditLogEntry.getExperimentId().toString(), String::equals),
        BUCKET(auditLogEntry -> auditLogEntry.getBucketLabel().toString(), StringUtils::containsIgnoreCase),
        APP(auditLogEntry -> auditLogEntry.getApplicationName().toString(), StringUtils::containsIgnoreCase),
        TIME(auditLogEntry -> auditLogEntry.getTime().getTime(), FilterUtil::extractTimeZoneAndTestDate),
        ATTRIBUTE(AuditLogEntry::getChangedProperty, StringUtils::containsIgnoreCase),
        BEFORE(AuditLogEntry::getBefore, StringUtils::containsIgnoreCase),
        AFTER(AuditLogEntry::getAfter, StringUtils::containsIgnoreCase),
        USER(auditLogEntry -> auditLogEntry.getUser().getFirstName() + " " + auditLogEntry.getUser().getLastName(), StringUtils::containsIgnoreCase),
        ;

        private final Function<AuditLogEntry, ?> propertyExtractor;
        private final BiPredicate<?, String> filterPredicate;

        /**
         * Creates a Property.
         *
         * @param propertyExtractor the property extractor
         * @param filterPredicate the filter predicate
         * @param <T> the property type
         */
        <T> Property(Function<AuditLogEntry, T> propertyExtractor, BiPredicate<T, String> filterPredicate) {
            this.propertyExtractor = propertyExtractor;
            this.filterPredicate = filterPredicate;
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
        public BiPredicate<?, String> getFilterPredicate() {
            return filterPredicate;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean test(AuditLogEntry auditLogEntry) {
        return super.test(auditLogEntry, Property.class);
    }

}
