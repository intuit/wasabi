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
package com.intuit.wasabi.api.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.intuit.wasabi.experimentobjects.Experiment.State;
import org.junit.Test;

import java.io.IOException;

import static java.nio.charset.Charset.forName;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ExperimentStateDeserializerTest {

    ExperimentStateDeserializer experimentStateDeserializer = new ExperimentStateDeserializer();

    @Test
    public void canDeserializeUpperCase() throws Exception {
        assertThat(parsedValueFor("RUNNING"), is(equalTo(State.RUNNING)));
    }

    @Test
    public void canDeserializeLowerCase() throws Exception {
        assertThat(parsedValueFor("running"), is(equalTo(State.RUNNING)));
    }

    @Test
    public void canDeserialzeMixedCase() throws Exception {
        assertThat(parsedValueFor("rUnNiNg"), is(equalTo(State.RUNNING)));
    }

    private State parsedValueFor(String value) throws IOException {
        JsonParser jsonParser = new JsonFactory().createParser(String.format("{\"state\": \"%s\"}", value).getBytes(forName("UTF-8")));
        jsonParser.nextToken();
        jsonParser.nextToken();
        jsonParser.nextToken();
        return experimentStateDeserializer.deserialize(jsonParser, null);
    }
}
