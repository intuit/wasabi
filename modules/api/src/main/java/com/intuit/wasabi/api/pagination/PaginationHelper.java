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

import java.util.List;
import java.util.stream.Collectors;

public class PaginationHelper<T> {

    private final PaginationFilter<T> paginationFilter;
    private final PaginationComparator<T> paginationComparator;

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
     * @param page the page, should be greater than 0
     * @param perPage the number of entries per page, should be greater than 0
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
     * @param page the page, should be greater than 0. Will be ignored if {@code perPage} is -1.
     * @param perPage the number of entries per page, should be greater than 0 or -1 to return all items
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

    public List<T> paginate(List<T> list, String filter, String timezoneOffset, String sort, int page, int perPage) {
        list = list.parallelStream()
                   .filter(paginationFilter.setFilter(filter, timezoneOffset))
                   .sorted(paginationComparator.setSortorder(sort))
                   .collect(Collectors.toList());

        return perPage == -1 ? list : list.subList(fromIndex(page, perPage, list.size()),
                                                   toIndex(page, perPage, list.size()));
    }

}
