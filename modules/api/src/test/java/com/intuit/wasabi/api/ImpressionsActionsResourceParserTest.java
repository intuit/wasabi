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
package com.intuit.wasabi.api;

import com.intuit.wasabi.experimentobjects.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class ImpressionsActionsResourceParserTest {

    private static final Application.Name TESTAPP = Application.Name.valueOf("test_app");

    @Test
    public void parse() {
        Map<Application.Name, List<List<String>>> body = new HashMap<Application.Name, List<List<String>>>();
        List<List<String>> sub1 = new ArrayList<List<String>>();
        List<String> subSub1 = new ArrayList<String>();

        subSub1.add(null);
        sub1.add(subSub1);
        body.put(TESTAPP, sub1);

        ImpressionsActionsResourceParser.parse(body);

        assertNotNull(subSub1.get(0));
    }
}
