/*
 * ******************************************************************************
 *  * Copyright 2016 Intuit
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  ******************************************************************************
 */

package com.intuit.wasabi.tests.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CombinableDataProvider {
    private final Logger logger = LoggerFactory.getLogger(CombinableDataProvider.class);

    /**
     * Returns the list of combination of color and shape codes.
     *
     * @return the collection of combined color and shape codes.
     */
    public static Object[][] combine(Object[][] a1, Object[][] a2) {
        List<Object[]> combined = new LinkedList<>();
        for (Object[] o : a1) {
            for (Object[] o2 : a2) {
                combined.add(concatAll(o, o2));
            }
        }
        return combined.toArray(new Object[0][0]);
    }


    @SafeVarargs
    public static <T> T[] concatAll(T[] first, T[]... rest) {
        //calculate the total length of the final object array after the concat
        int totalLength = first.length;
        for (T[] array : rest) {
            totalLength += array.length;
        }
        //copy the first array to result array and then copy each array completely to result
        T[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (T[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }

        return result;
    }
}
