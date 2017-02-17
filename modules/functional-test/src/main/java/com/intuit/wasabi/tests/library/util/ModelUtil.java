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
package com.intuit.wasabi.tests.library.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides helper methods to handle model items.
 */
public class ModelUtil<T> {

    /**
     * This interface describes a filter to filter collections.
     *
     * @param <T> the class to be filtered
     */
    public interface Filter<T> {

        /**
         * Returns true if this item shall shall be contained in the filtered collection.
         *
         * @param collectionItem the item to filter
         * @return true if it shall be contained
         */
        boolean filter(T collectionItem);
    }

    /**
     * Filteres a list according to the supplied filter strategy.
     *
     * @param dataList the list to filter
     * @param filter   the filter to use
     * @return the filtered list
     */
    public List<T> filterList(List<T> dataList, Filter<T> filter) {
        List<T> filteredList = new ArrayList<T>();
        for (T dataItem : dataList) {
            if (filter.filter(dataItem)) {
                filteredList.add(dataItem);
            }
        }
        return filteredList;
    }

}
