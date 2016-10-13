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
package com.intuit.wasabi.assignmentobjects;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Test for the {@link PersonalizationEngineResponse}
 */
public class PersonalizationEngineResponseTest {

    private String tid = "testTid";
    private Map<String, Double> data = new HashMap<>();
    private String model = "testModel";

    private PersonalizationEngineResponse response;

    @Before
    public void setUp() throws Exception {
        response = getPersonalizationEngineResponse();
    }

    private PersonalizationEngineResponse getPersonalizationEngineResponse() {
        return PersonalizationEngineResponse.withTid(tid)
                .withData(data)
                .withModel(model)
                .build();
    }

    @Test
    public void testPersonalizationEngineResponseSet() {
        response.setTid(tid);
        response.setData(data);
        response.setModel(model);

        assertEquals(tid, response.getTid());
        assertEquals(data, response.getData());
        assertEquals(model, response.getModel());
    }

    @Test
    public void testPersonalizationEngineResponseFromOther() {
        PersonalizationEngineResponse other = PersonalizationEngineResponse.from(response).build();

        assertEquals(response, other);
        assertEquals(response.toString(), other.toString());
        assertEquals(response.hashCode(), other.hashCode());
    }

}
