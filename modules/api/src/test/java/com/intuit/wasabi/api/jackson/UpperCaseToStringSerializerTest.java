/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.api.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.intuit.wasabi.api.jackson.serializers.UpperCaseToStringSerializer;
import com.intuit.wasabi.experimentobjects.Experiment.State;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.assertThat;

public class UpperCaseToStringSerializerTest {

    @Test
    public void serializesToLowercase() throws Exception {
        StringWriter output = new StringWriter();
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("state");
        module.addSerializer(new UpperCaseToStringSerializer<>(State.class));
        objectMapper.registerModule(module);
        objectMapper.writeValue(output, State.RUNNING);
        assertThat(output.toString(), Matchers.is(Matchers.equalTo("\"RUNNING\"")));
    }
}
