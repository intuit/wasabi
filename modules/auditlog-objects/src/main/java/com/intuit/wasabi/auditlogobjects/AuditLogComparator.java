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
package com.intuit.wasabi.auditlogobjects;

import java.util.Comparator;

/**
 * Comparator for {@link AuditLogEntry}s.
 */
public class AuditLogComparator implements Comparator<AuditLogEntry>{

    private final int RIGHT_NULL = -1;
    private final int LEFT_NULL = 1;
    private final int NOT_NULL = 2;
    private final int BOTH_NULL = 0;

    private final String sortOrder;

    /**
     * Constructor. Must be given a sort order as a String.
     *
     * @param sortOrder
     */
    public AuditLogComparator(String sortOrder){
        this.sortOrder = sortOrder;
    }

    /**
     * Compares to results for null values. If both are not null or both are null, this returns 0.
     * If o1 is not null but o2 is, it returns -1. If o2 is not null but o1 is, it returns 1.
     * The descending flag flips the results to make sure "null" values are always at the end.
     *
     * @param o1 an object
     * @param o2 another object
     * @return -1, 0, 1 depending on null status
     */
    private int compareNull(Object o1, Object o2, boolean descending) {
        if (o1 != null && o2 != null) {
            return NOT_NULL;
        }
        if (o1 != null) {
            return descending ? LEFT_NULL : RIGHT_NULL;
        }
        if (o2 != null) {
            return descending ? RIGHT_NULL : LEFT_NULL;
        }
        return BOTH_NULL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(AuditLogEntry o1, AuditLogEntry o2) {
        int result = 0;
        for (String sort : sortOrder.toLowerCase().split(",")) {
            boolean descending = sort.contains("-");

            AuditLogProperty property = AuditLogProperty.forKey(descending ? sort.substring(1) : sort);
            if (property == null) {
                continue;
            }
            switch (property) {
                case FIRSTNAME:
                    if ((result = compareNull(o1.getUser().getFirstName(), o2.getUser().getFirstName(), descending)) == NOT_NULL) {
                        result = o1.getUser().getFirstName().compareToIgnoreCase(o2.getUser().getFirstName());
                    }
                    break;
                case LASTNAME:
                    if ((result = compareNull(o1.getUser().getLastName(), o2.getUser().getLastName(), descending)) == NOT_NULL) {
                        result = o1.getUser().getLastName().compareToIgnoreCase(o2.getUser().getLastName());
                    }
                    break;
                case USER: case USERNAME:
                    if ((result = compareNull(o1.getUser().getUsername(), o2.getUser().getUsername(), descending)) == NOT_NULL) {
                        result = o1.getUser().getUsername().toString().compareToIgnoreCase(o2.getUser().getUsername().toString());
                    }
                    if (result == 0 && (result = compareNull(o1.getUser().getUserId(), o2.getUser().getUserId(), descending)) == NOT_NULL) {
                        result = o1.getUser().getUserId().compareToIgnoreCase(o2.getUser().getUserId());
                    }
                    break;
                case MAIL:
                    if ((result = compareNull(o1.getUser().getEmail(), o2.getUser().getEmail(), descending)) == NOT_NULL) {
                        result = o1.getUser().getEmail().compareToIgnoreCase(o2.getUser().getEmail());
                    }
                    break;
                case ACTION:
                    if ((result = compareNull(o1.getAction(), o2.getAction(), descending)) == NOT_NULL) {
                        result = o1.getAction().compareTo(o2.getAction());
                    }
                    break;
                case EXPERIMENT:
                    if ((result = compareNull(o1.getExperimentLabel(), o2.getExperimentLabel(), descending)) == NOT_NULL) {
                        result = o1.getExperimentLabel().toString().compareToIgnoreCase(o2.getExperimentLabel().toString());
                    }
                    break;
                case BUCKET:
                    if ((result = compareNull(o1.getBucketLabel(), o2.getBucketLabel(), descending)) == NOT_NULL) {
                        result = o1.getBucketLabel().toString().compareToIgnoreCase(o2.getBucketLabel().toString());
                    }
                    break;
                case APP:
                    if ((result = compareNull(o1.getApplicationName(), o2.getApplicationName(), descending)) == NOT_NULL) {
                        result = o1.getApplicationName().toString().compareToIgnoreCase(o2.getApplicationName().toString());
                    }
                    break;
                case TIME:
                    if ((result = compareNull(o1.getTime(), o2.getTime(), descending)) == NOT_NULL) {
                        result = o1.getTime().compareTo(o2.getTime());
                    }
                    break;
                case ATTR:
                    if ((result = compareNull(o1.getChangedProperty(), o2.getChangedProperty(), descending)) == NOT_NULL) {
                        result = o1.getChangedProperty().compareToIgnoreCase(o2.getChangedProperty());
                    }
                    break;
                case BEFORE:
                    if ((result = compareNull(o1.getBefore(), o2.getBefore(), descending)) == NOT_NULL) {
                        result = o1.getBefore().compareToIgnoreCase(o2.getBefore());
                    }
                    break;
                case AFTER:
                    if ((result = compareNull(o1.getAfter(), o2.getAfter(), descending)) == NOT_NULL) {
                        result = o1.getAfter().compareToIgnoreCase(o2.getAfter());
                    }
                    break;
                case DESCRIPTION:
                    String desc1 = AuditLogAction.getDescription(o1);
                    String desc2 = AuditLogAction.getDescription(o2);
                    result = desc1.compareToIgnoreCase(desc2);
                    break;
            }
            if (result != 0) {
                if (descending) {
                    result = -result;
                }
                break;
            }

        }
        return result;
    }
}