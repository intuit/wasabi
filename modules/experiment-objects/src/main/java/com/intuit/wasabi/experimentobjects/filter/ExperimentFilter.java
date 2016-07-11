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
import java.util.List;

/**
 * This class contains the logic to sort and filter experiments.
 */
public class ExperimentFilter {

    /**
     * Filters a list of Experiments according to the given filterMask.
     * If the filterMask is null, empty or ill-formatted, the original list will be returned.
     * <p>
     * A filterMask must be of the following format:
     * <pre>{@code
     * FilterMask   := Value | Value,KeyValueList | KeyValueList
     * KeyValueList := Property=Value | Property=Value,KeyValueList | Property={Options}Value | Property={Options}Value,KeyValueList
     * Property     := app_name | experiment_name | created_by | sampling_perc | start_date | end_date | mod_date | status | attr | before | after
     * Value        := any value, may not contain commas (,) followed by a Property. If it starts with an escaped
     *                 dash (\-), the value is negated, thus shall not match.
     * Options      := property specific options, see below.
     *
     * }</pre>
     * If the filter mask just contains a value, all fields are filtered by this value: If the value is prefixed by a
     * dash, all matches are removed from the list, if it is not prefixed all entries which have no match are removed.
     * The KeyValueList will be split by commas. The matching is then done as the global matching but on a per field
     * basis. All rules have to be fulfilled to retain an entry in the returned list.
     * <p>
     * Available special {@code Options} by {@code Property}
     *
     * <ul>
     *     <li>{@code time}: time can take a time zone offset of the format {@code sign digit digit digit digit}.<p>Example: {@code time={-0700}mar 14}</li>
     * </ul>
     * <p>
     * Examples:
     * <ul>
     *     <li>{@code searchValue} would yield all entries which somewhere contain 'searchValue'.</li>
     *     <li>{@code username=jdoe,experiment=testexp,bucket=-b} would yield all entries for users with names containing 'jdoe', experiments containing 'testexp' und buckets not containing 'b'.</li>
     *     <li>{@code \-3} would yield all entries for users with names containing '-3'.</li>
     *     <li>{@code -3} would yield all entries for users with names not containing '3'.</li>
     *     <li>{@code rapid,bucket=abc} would yield all entries which somewhere contain 'rapid' and have buckets' labels containing 'abc'.</li>
     *     <li>{@code rapid,abc} would yield all entries which somewhere contain 'rapid,abc'.</li>
     *     <li>{@code null} searches for 'null'</li>
     * </ul>
     *
     * @param experiments the list to be filtered
     * @param filterMasks     the filter mask
     * @return a filtered list
     */
    public static List<Experiment> filter(List<Experiment> experiments, String filterMasks){
        return null;
    }

    /**
     * Sorts a list of AuditLogEntries according to the given sortOrder.
     * If the sortOrder is null, empty or ill-formatted, the original list will be returned.
     * <p>
     * A sortOrder must be of the following format:
     * <pre>{@code
     * SortOrder    := Property | PropertyList
     * PropertyList := Property,SortOrder
     * Property     := any ExperimentProperty, may be prefixed with -
     * }</pre>
     * Sorting is done from left to right, sorting by the first supplied property and if there are ties the next
     * property decides how to break them etc. If a field is prefixed by a dash this means descending order, otherwise
     * ascending order is used.
     * <p>
     * Examples:
     * <ul>
     *     <li>{@code -username,experiment} will sort by users (username and userid) (descending) and for each user by experiment labels (ascending) to break ties</li>
     *     <li>{@code experiment,bucket} will sort by experiments and then for each experiment by bucket</li>
     * </ul>
     * Note that both, {@code username} and {@code user} will try to sort by username first, and if that doesn't work will
     * resort to userIDs.
     *
     * @param experiments the list to be sorted
     * @param sortOrder       the sort order
     * @return a sorted list
     * @see ExperimentProperty#keys()
     */
    public static List<Experiment> sort(List<Experiment> experiments, String sortOrder){
        return null;
    }
}