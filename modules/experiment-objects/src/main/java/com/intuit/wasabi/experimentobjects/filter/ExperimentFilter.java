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
import org.apache.commons.lang3.StringUtils;

import java.util.*;

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
     * @param filterMask     the filter mask
     * @return a filtered list
     */
    public static List<Experiment> filter(List<Experiment> experiments, String filterMask){
        if (StringUtils.isBlank(filterMask)) {
            return experiments;
        }

        String[] mask = prepareMask(filterMask);
        boolean filterFullText = !mask[0].contains("=");
        String fullTextPattern = null;
        if (filterFullText) {
            fullTextPattern = mask[0].startsWith("\\-") ? mask[0].substring(2, mask[0].length()) : mask[0];
        }

        Map<String, String> optionsMap = new HashMap<>();

        for (Iterator<Experiment> iter = experiments.iterator(); iter.hasNext(); ) {
            Experiment exp = iter.next();
            boolean remove = false;

            // filter for each key value pair until none is left or the entry should be removed
            for (int i = filterFullText ? 1 : 0; i < mask.length && !remove; ++i) {
                // determine key and value, fail if not well formatted
                String[] keyValue = mask[i].split("=");
                String options = "";
                if (keyValue.length > 2) { // return empty list if invalid
                    return Collections.emptyList();
                } else if (keyValue.length == 1) { // skip empty filters
                    continue;
                } else {
                    // options
                    if (keyValue[1].startsWith("{") && keyValue[1].contains("}")) {
                        options = keyValue[1].substring(1, keyValue[1].indexOf("}"));
                        keyValue[1] = keyValue[1].substring(keyValue[1].indexOf("}") + 1);
                        optionsMap.put(keyValue[0], options);
                    }
                }
                String pattern = keyValue[1].startsWith("\\-") ? keyValue[1].substring(2, keyValue[1].length()) : keyValue[1];
                remove = !singleFieldSearch(exp, keyValue[0], pattern, options, !keyValue[1].startsWith("\\-"));
            }

            if (filterFullText && !remove) {
                remove = !fullTextSearch(exp, fullTextPattern, optionsMap, !mask[0].startsWith("\\-"));
            }

            if (remove) {
                iter.remove();
            }
        }

        return experiments;
    }

    /**
     * Calls all distinguishing getters of the Experiment until a match is found and returns {@code filter}.
     * If no match is found, {@code !filter} is returned.
     * Note: This method is case-insensitive.
     *
     * @param exp the experiment
     * @param pattern the pattern to search
     * @param filter the return value on success
     * @return {@code filter} on match, {@code !filter} otherwise
     */
    private static boolean fullTextSearch(Experiment exp, String pattern, Map<String, String> options, boolean filter) {
        for (String key : ExperimentProperty.keys()) {
            if (singleFieldSearch(exp, key, pattern, options.get(key), filter) == filter) {
                return filter;
            }
        }
        if (contains(exp.getID(), pattern)) return filter;
        return !filter;
    }

    /**
     * Prepares the filter mask as it is defined by the rules described in {@link #filter(List, String)}.
     *
     * @param filterMask the filter mask
     * @return the individual filters
     */
    private static String[] prepareMask(String filterMask) {
        String filterMaskEsc = filterMask.replaceAll(",(" + StringUtils.join(ExperimentProperty.keys(), "|") + ")+", "\\\\,$1");
        return filterMaskEsc.toLowerCase().split("\\\\,");
    }

    /**
     * Calls the getter identified by {@code key.toLowerCase()} of the experiment returns {@code filter} on success.
     * If no match is found, {@code !filter} is returned.
     *
     * The allowed keys and their fields are:
     * <ul>
     *     <li>app_name {@link Experiment#getApplicationName()}</li>
     *     <li>experiment_name {@link Experiment#getLabel()}</li>
     *     <li>created_by {@link Experiment#getCreatorID()}</li>
     *     <li>sampling_perc {@link Experiment#getSamplingPercent()}</li>
     *     <li>start_date {@link Experiment#getStartTime()}</li>
     *     <li>end_date {@link Experiment#getEndTime()}</li>
     *     <li>mod_date {@link Experiment#getModificationTime()}</li>
     *     <li>status {@link Experiment#getState()}</li>
     * </ul>
     * Note: This method is case-insensitive!
     *
     * @param exp the Experiment
     * @param key the key determining the checked field
     * @param pattern the pattern to search
     * @param filter the return value on success
     * @return {@code filter} on match, {@code !filter} otherwise
     */
    /*test*/ static boolean singleFieldSearch(Experiment exp, String key, String pattern, String options, boolean filter) {
        ExperimentProperty property = ExperimentProperty.forKey(key);
        boolean filtered = !filter;


        if (property == null) {
            return filtered;
        }
        switch (property) {
            case APP_NAME: filtered = contains(exp.getApplicationName().toString(), pattern) ? filter : !filter;
                break;
            case EXP_NAME: filtered = contains(exp.getLabel().toString(), pattern) ? filter : !filter;
                break;
            case CREATE_BY: filtered = contains(exp.getCreatorID(), pattern) ? filter : !filter;
                break;
            case SAMPLING_PERC: filtered = contains(exp.getSamplingPercent(), pattern) ? filter : !filter;
                break;
            case START_DATE: filtered = contains(exp.getStartTime(), pattern) ? filter : !filter;
                break;
            case END_DATE: filtered = contains(exp.getEndTime(), pattern) ? filter : !filter;
                break;
            case MOD_DATE: filtered = contains(exp.getModificationTime(), pattern) ? filter : !filter;
                break;
            case STATUS: filtered = contains(exp.getState().toString(), pattern) ? filter : !filter;
                break;
            default:
                break;
        }
        return filtered;
    }

    /**
     * Returns true iff both, container and contained, are not blank and if
     * {@code container.toString().toLowerCase().contains(contained.toString().toLowerCase())}.
     * Note: This method is case-insensitive!
     *
     * @param container the string to search in
     * @param contained the string to search for
     * @return true if container contains contained
     */
    private static boolean contains(Object container, Object contained) {
        if (container == null || contained == null) {
            return false;
        }
        String s1 = container.toString().toLowerCase();
        String s2 = contained.toString().toLowerCase();
        return s1.contains(s2);
    }

    /**
     * Sorts a list of Experiments according to the given sortOrder.
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
     *     <li>{@code -appname,expname} will sort by the application name (descending) and for each experiment label (ascending) to break ties</li>
     *     <li>{@code status, moddate} will sort by experiment status and then for each experiment by modification date</li>
     * </ul>
     *
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