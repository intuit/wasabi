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
package com.intuit.wasabi.api.util;

import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class holds all the methods that might be otherwise duplicated by
 * different paginable resources.
 */
public class PaginationHelper {

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
     * @param page the page, must be positive
     * @param perPage the number of entries per page, must be positive
     * @param totalEntries the total number of entries available
     * @return an index to be used as a fromIndex for {@link List#subList(int, int)}
     */
    public static int toIndex(int page, int perPage, int totalEntries) {
        if (page <= 0 || perPage <= 0 || totalEntries < 0) {
            return 0;
        }
        if ((page - 1) * perPage >= totalEntries) {
            return 0;
        }
        return perPage * page < totalEntries ? perPage * page : totalEntries;
    }

    /**
     * Calculates the total number of pages needed to present the number of entries.
     * <br />
     * The number of entries must be 0 or positive, the perPage value must be strictly positive.
     * If any of these conditions does not hold, 1 is returned, as you will always have one page - it might then be
     * empty.
     *
     *
     * @param totalEntries the number of entries
     * @param perPage the number of entries per page
     * @return the number of pages, 1 on invalid values.
     */
    public static int numberOfPages(int totalEntries, int perPage) {
        if (totalEntries < 0 || perPage <= 0) {
            return 1;
        }
        return totalEntries / perPage + (totalEntries % perPage > 0 ? 1 : 0);
    }

    /**
     * Calculates a fromIndex for use with {@link List#subList(int, int)} to paginate a list correctly.
     * Returns 0 for all invalid values (alongside with {@link #toIndex(int, int, int)}), so that
     * for invalid values the call to {@link List#subList(int, int)} will result in an empty list.
     * <br />
     * Invalid values are negative values for the parameters (or 0 for page or per page) as well as parameter
     * combinations which define "inaccessible pages". For example if you just have 2 entries and show up to
     * 4 per page, you will not have a second page.
     *
     * @param page the page, must be positive
     * @param perPage the number of entries per page, must be positive
     * @param totalEntries the total number of entries available
     * @return an index to be used as a fromIndex for {@link List#subList(int, int)}
     */
    public static int fromIndex(int page, int perPage, int totalEntries) {
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
     * Makes sure that the timezone offset is present in the filter mask.<br />
     * If the timezoneOffset is blank, the initial filter is returned.<br />
     * If the timezoneOffset is already set in the filter mask (no matter which one), the filter mask is
     * returned.<br />
     * If the filter does not contain the "time" property, it is added, unless the cases before are effective.
     *
     * @param filter the filter to modify
     * @param timezoneOffset the timezoneOffset to be placed in the mask
     * @return
     */
    public static String prepareDateFilter(final String filter, final String timezoneOffset) {
        if (StringUtils.isBlank(timezoneOffset) || StringUtils.isBlank(filter)) {
            return filter;
        }
        if (!filter.contains(",time={")) {
            if (filter.contains(",time=")) {
                int ind = filter.indexOf(",time=");
                return filter.substring(0, ind + 6) + "{" + timezoneOffset + "}" + filter.substring(ind + 6);
            }
            if (filter.contains(",time")) {
                int ind = filter.indexOf(",time");

                return filter.substring(0, ind + 5) + "={" + timezoneOffset + "}" + filter.substring(ind + 5);
            }
            return filter + ",time={" + timezoneOffset + "}";
        }
        return filter;
    }

    /**
     * Creates navigation links for a {@code Link} header field.<br />
     * This will contain a comma separated list of entries with (relative) links in {@code < >} and a {@code rel}
     * attribute which describes the link.<br />
     *
     * Examples:
     * <ul>
     *     <li>{@code &lt;/api/v1/logs/applications/MyApplication?per_page=20&page=2&gt;; rel="Page 2"}</li>
     *     <li>{@code &lt;/api/v1/logs/applications/MyApplication?per_page=20&page=2&gt;; rel="Page 2",&lt;/api/v1/logs/applications/MyApplication?per_page=20&page=1&gt;; rel="Previous"}</li>
     * </ul>
     *
     * The method automatically generates links to the "First" and "Last" pages and, if applicable, for the "Next" and
     * "Previous" pages. Additionally it will generate links from pages {@code page-boundary} to {@code page+boundary},
     * excluding the current page and unreachable pages. Those pages will have their rel-attributes set to "Page X",
     * where X is their page number.
     *
     * @param path the path for this endpoint, for example {@code logs}
     * @param entries the total number of entries to calculate the number of pages
     * @param page the current page
     * @param perPage the number of entries per page
     * @param boundary the boundary for the links to be calculated
     * @param parameters a map with other parameters to append to the link, for example {@code &filter=-time} would
     *                   be contained with the key {@code filter} and the value {@code -time}.
     * @return a string suitable for a {@code Link} header field describing navigational links
     */
    public static String linkHeaderValue(String path, int entries, int page, int perPage,
                                         int boundary, Map<String, String> parameters) {
        String base = "/api/v1" + path + "?per_page=" + perPage;
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            base += "&" + parameter.getKey() + "=" + parameter.getValue();
        }
        int pages = numberOfPages(entries, perPage);

        List<String> links = new ArrayList<>();
        links.add("<" + base + "&page=1>; rel=\"First\"");
        links.add("<" + base + "&page=" + pages + ">; rel=\"Last\"");

        for (int i = page - boundary; i <= page + boundary; ++i) {
            if (i <= 0 || i == page || i > pages) {
                continue;
            }
            if (i == page - 1 || i == page + 1) {
                links.add("<" + base + "&page=" + i + ">; rel=\"" + (i == page - 1 ? "Previous" : "Next") + "\"");
            }
            links.add("<" + base + "&page=" + i + ">; rel=\"Page " + i + "\"");
        }

        return StringUtils.join(links, ", ");
    }


    /**
     * Prepares the response for a list of entries - like {@link AuditLogEntry}.
     *
     * @param name the name that should be used in the respons
     * @param path the path for this endpoint, for example {@code logs}
     * @param entries the list of Entries
     * @param page the current page
     * @param perPage the number of entries per page
     * @param filter the applied filter
     * @param sort the applied sort order
     * @return the response
     */
    public static Response preparePageFilterResponse(String name, String path, final List<?> entries,
                                                     int page, int perPage, String filter, String sort) {
        int total = entries.size();

        Map<String, Object> response = new HashMap<>();
        response.put(name, entries.subList(fromIndex(page, perPage, total), toIndex(page, perPage, total)));
        response.put("totalEntries", total);

        Map<String, String> params = new HashMap<>();
        params.put("filter", filter);
        params.put("sort", sort);
        return Response.ok(response).header("Link", linkHeaderValue(path, total, page, perPage, 3, params)).build();
    }

}