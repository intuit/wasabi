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
package com.intuit.wasabi.test.util;

import com.intuit.wasabi.analyticsobjects.counts.BucketCounts;
import com.intuit.wasabi.experimentobjects.Bucket.Label;

import java.util.Map;
import java.util.Map.Entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestUtils {

    public static void assertMapsEqual(Map<Label, BucketCounts> map1, Map<Label, BucketCounts> map2) {
        assertEquals(map1.keySet(), map2.keySet());
        assertTrue(map1.toString().equals(map2.toString()));
        assertTrue(map2.toString().equals(map1.toString()));
        for (Entry<Label, BucketCounts> entry : map1.entrySet()) {
            assertEquals(entry.getValue(), map2.get(entry.getKey()));
        }
        for (Entry<Label, BucketCounts> entry : map2.entrySet()) {
            assertEquals(entry.getValue(), map1.get(entry.getKey()));
        }
    }

}
