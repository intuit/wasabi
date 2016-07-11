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
            return 2;
        }
        if (o1 != null) {
            return descending ? 1 : -1;
        }
        if (o2 != null) {
            return descending ? -1 : 1;
        }
        return 0;
    }


    @Override
    public int compare(Experiment exp1, Experiment exp2) {
        return 0;
    }
}

