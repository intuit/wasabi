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
package com.intuit.wasabi.webapi.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.intuit.wasabi.api.jackson.SQLTimestampDeserializer;
import org.junit.Test;

import java.io.IOException;
import java.sql.Timestamp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//import org.codehaus.jackson.JsonParser;

/**
 * Test for the {@link SQLTimestampDeserializerTest}
 * <p>
 * Created by asuckro on 8/20/15.
 */
public class SQLTimestampDeserializerTest {

    @Test
    public void deserializationFailureTest() throws IOException {
        JsonParser jsonParser = mock(JsonParser.class);
        when(jsonParser.getText()).thenReturn("asdasd");

        SQLTimestampDeserializer deserializer = new SQLTimestampDeserializer();
        try {
            deserializer.deserialize(jsonParser, null);
            fail();

        } catch (IllegalArgumentException e) {
            //this is what we want
        }


    }

    @Test
    public void deserializationTest() throws IOException {
        JsonParser jsonParser = mock(JsonParser.class);
        when(jsonParser.getText()).thenReturn("2014-05-12 12:32:32");

        SQLTimestampDeserializer deserializer = new SQLTimestampDeserializer();
        try {
            Timestamp ts = deserializer.deserialize(jsonParser, null);
            assertEquals(ts, Timestamp.valueOf("2014-05-12 12:32:32"));

        } catch (IllegalArgumentException e) {
            fail();
        }

    }

}
