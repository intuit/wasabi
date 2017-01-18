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

package com.intuit.wasabi.tests.model;

import com.google.common.base.Joiner;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class StatisticsTest {
    private final Logger logger = LoggerFactory.getLogger(StatisticsTest.class);
    private OutputBucketStatistics currentStatistics;

    @Before
    public void setup() {
        currentStatistics = new OutputBucketStatistics("test");
    }


    @Test
    public void increaseActionCountTest() {
        Map<String, Integer> jsonObject1 = new HashMap<>();
        jsonObject1.put("click", 2);
        jsonObject1.put("impression", 2);
        jsonObject1.put("love it", 1);
        Map<String, Integer> jsonObject2 = new HashMap<>();
        jsonObject2.put("click", 2);
        jsonObject2.put("impression", 2);
        jsonObject2.put("love it", 1);
        currentStatistics.increaseCounts(jsonObject1, (p) -> false);
        currentStatistics.increaseCounts(jsonObject2, (p) -> false);
        logger.info(Joiner.on('&').withKeyValueSeparator("=").join(currentStatistics.getActionCounts()));
    }
}
