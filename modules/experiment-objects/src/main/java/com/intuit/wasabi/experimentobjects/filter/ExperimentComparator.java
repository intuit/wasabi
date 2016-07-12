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
package com.intuit.wasabi.experimentobjects.filter;


import com.intuit.wasabi.experimentobjects.Experiment;
import java.util.Comparator;

/**
 * A comparator for {@link Experiment}s.
 */
public class ExperimentComparator implements Comparator<Experiment>{

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
    public ExperimentComparator(String sortOrder){
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


    @Override
    public int compare(Experiment exp1, Experiment exp2) {
        int result = 0;
        for (String sort : sortOrder.toLowerCase().split(",")) {
            boolean descending = sort.contains("-");

            ExperimentProperty property = ExperimentProperty.forKey(descending ? sort.substring(1) : sort);
            if (property == null) {
                continue;
            }


            switch (property){
                case APP_NAME:
                    result = exp1.getApplicationName().toString().compareToIgnoreCase(exp2.getApplicationName().toString());
                    break;
                case EXP_NAME:
                    result = exp1.getLabel().toString().compareToIgnoreCase(exp2.getLabel().toString());
                    break;
                case CREATE_BY:
                    result = exp1.getCreatorID().compareToIgnoreCase(exp2.getCreatorID());
                    break;
                case SAMPLING_PERC:
                    if ((result = compareNull(exp1.getSamplingPercent(), exp2.getSamplingPercent(), descending)) == NOT_NULL)
                        result = exp1.getSamplingPercent().compareTo(exp2.getSamplingPercent());
                    break;
                case START_DATE:
                    if ((result = compareNull(exp1.getStartTime(), exp2.getStartTime(), descending)) == NOT_NULL)
                        result = exp1.getStartTime().compareTo(exp2.getStartTime());
                    break;
                case END_DATE:
                    if ((result = compareNull(exp1.getEndTime(), exp2.getEndTime(), descending)) == NOT_NULL)
                        result = exp1.getEndTime().compareTo(exp2.getEndTime());
                    break;
                case MOD_DATE:
                    if ((result = compareNull(exp1.getModificationTime(), exp2.getModificationTime(), descending)) == NOT_NULL)
                        result = exp1.getModificationTime().compareTo(exp2.getModificationTime());
                    break;
                case STATUS:
                    result = exp1.getState().compareTo(exp2.getState());
                    break;
            }

            // reverse order in the case of descending
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

