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
package com.intuit.wasabi.api.pagination;

import com.google.inject.Inject;
import com.intuit.wasabi.api.pagination.comparators.PaginationComparator;
import com.intuit.wasabi.api.pagination.filters.PaginationFilter;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.ExperimentList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A pagination helper allows for simple pagination of resources.
 * <p>
 * A common usage pattern is to build up the list of response elements and then call the pagination helper.
 * Note that due to the type-nature of the pagination helper, a separate pagination helper is required for each type
 * of objects one wants to return, e.g. you will need a {@code PaginationHelper<Experiment>} as well as a
 * {@code PaginationHelper<Application>} for the same API resource if you have endpoints for both types.
 * <p>
 * Usually one will use a pagination helper like this:
 * <pre>{@code
 * Map<String, Object> response = paginationHelper.paginate("identifier", responseList, filter,
 *                                                          timezoneOffset, sort, page, perPage);
 * return httpHeader.headers().entity(response).build();
 * }</pre>
 * This results in a JSON response similar to:
 * <pre>{@code
 * {"identifier": [...list of elements...], "totalEntries": 231}
 * }</pre>
 * <p>
 * Note that you will need to modify the endpoint to take the parameters for {@code filter}, {@code timezoneOffset},
 * {@code sort}, {@code page}, and {@code perPage}. See
 * {@link com.intuit.wasabi.api.AuditLogResource#getLogsForApplication(String, Application.Name, int, int,
 * String, String, String)}
 * for an example.
 * <p>
 * You can not use the list wrappers directly (e.g. {@link ExperimentList}), instead you have to use the raw lists.
 *
 * @param <T> the type of elements to be paginated
 */
public class PaginationHelper<T> {

    private final PaginationFilter<T> paginationFilter;
    private final PaginationComparator<T> paginationComparator;

    /**
     * Initializes the pagination helper with implementations of filter and comparator via Guice injection.
     *
     * @param paginationFilter     the pagination filter
     * @param paginationComparator the pagination comparator
     */
    @Inject
    public PaginationHelper(PaginationFilter<T> paginationFilter, PaginationComparator<T> paginationComparator) {
        this.paginationFilter = paginationFilter;
        this.paginationComparator = paginationComparator;
    }

    /**
     * Calculates a toIndex for use with {@link List#subList(int, int)} to paginate a list correctly.
     * Returns 0 for all invalid values (alongside with {@link #fromIndex(int, int, int)}), so that
     * for invalid values the call to {@link List#subList(int, int)} will result in an empty list.
     * <br />
     * Invalid values are negative values for the parameters (or 0 for page or per page) as well as parameter
     * combinations which define "inaccessible pages". For example if you just have 2 entries and show up to
     * 4 per page, you will not have a second page.
     * <br />
     * If the resulting index is bigger than the number of total entries, it is clamped to that value. This is
     * needed when pages are incomplete. Visiting the example above again but accessing page 1 with 4 entries
     * per page and just 2 entries in total, this method will yield 2 rather than 4.
     *
     * @param page         the page, should be greater than 0
     * @param perPage      the number of entries per page, should be greater than 0
     * @param totalEntries the total number of entries available
     * @return an index to be used as a toIndex for {@link List#subList(int, int)}. 0 for invalid parameters.
     */
    /*test*/ int toIndex(int page, int perPage, int totalEntries) {
        // invalid parameters
        if (page <= 0 || perPage <= 0 || totalEntries < 0) {
            return 0;
        }
        // behind the limited range
        if ((page - 1) * perPage >= totalEntries) {
            return 0;
        }
        // valid and possible pages
        return perPage * page < totalEntries ? perPage * page : totalEntries;
    }

    /**
     * Calculates a fromIndex for use with {@link List#subList(int, int)} to paginate a list correctly.
     * Returns 0 for all invalid values (alongside with {@link #toIndex(int, int, int)}), so that
     * for invalid values the call to {@link List#subList(int, int)} will result in an empty list.
     * <br />
     * Invalid values are negative values for the parameters (or 0 for page or per page) as well as parameter
     * combinations which define "inaccessible pages". For example if you just have 2 entries and show up to
     * 4 per page, you will not have a second page.
     * <br />
     * If {@code perPage} is -1, all items are returned. This ignores the {@code page} parameter.
     *
     * @param page         the page, should be greater than 0. Will be ignored if {@code perPage} is -1.
     * @param perPage      the number of entries per page, should be greater than 0 or -1 to return all items
     * @param totalEntries the total number of entries available
     * @return an index to be used as a fromIndex for {@link List#subList(int, int)}
     */
    /*test*/ int fromIndex(int page, int perPage, int totalEntries) {
        // invalid parameters
        if (page <= 0 || perPage <= 0 || totalEntries < 0) {
            return 0;
        }
        // impossible pages
        if ((page - 1) * perPage >= totalEntries) {
            return 0;
        }
        // valid and possible pages
        return (page - 1) * perPage;
    }

    /**
     * Paginates a resource.
     * <p>
     * The {@code jsonKey} will be the identifier in the JSON response:
     * <pre>{@code {"jsonKey": [...], "totalEntries": ...}}</pre>
     *
     * The number in {@code "totalEntries"} is taken after filtering but before sorting and slicing the list.
     *
     * @param jsonKey        the identifier of the response object array
     * @param list           the list to paginate
     * @param filter         the filter to apply before pagination (see {@link PaginationFilter}
     * @param timezoneOffset the timezone offset of the user (see {@link PaginationFilter}
     * @param sort           the sort order (see {@link PaginationComparator})
     * @param page           the page (starting at 1)
     * @param perPage        the number of elements per page (positive or -1 for all items)
     * @return a response map to be passed to {@link javax.ws.rs.core.Response.ResponseBuilder#entity(Object)}.
     */
    public Map<String, Object> paginate(String jsonKey, List<T> list, String filter,
                                        String timezoneOffset, String sort, int page, int perPage) {
        Map<String, Object> response = new HashMap<>();

        paginationFilter.replaceFilter(filter, timezoneOffset);
        paginationComparator.replaceSortorder(sort);
        list = list.size() == 0 ? list :
                list.parallelStream()
                        .filter(paginationFilter)
                        .sorted(paginationComparator)
                        .collect(Collectors.toList());
        response.put("totalEntries", list.size());

        response.put(jsonKey, (perPage == -1 || list.size() == 0) ? list :
                list.subList(fromIndex(page, perPage, list.size()),
                        toIndex(page, perPage, list.size())));

        return response;
    }

}
