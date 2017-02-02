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

import com.fasterxml.jackson.core.JsonGenerator;
import com.intuit.wasabi.api.jackson.NaNSerializerFloat;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

//import org.codehaus.jackson.JsonGenerator;

/**
 * Unit test for the {@link NaNSerializerFloat}
 * <p>
 * Created by asuckro on 8/20/15.
 */
public class NaNSerializerFloatTest {

    @Test
    public void testFloatValues() {
        JsonGenerator jsonGen = mock(JsonGenerator.class);

        NaNSerializerFloat ser = new NaNSerializerFloat();
        try {
            ser.serialize(Float.NEGATIVE_INFINITY, jsonGen, null);
            verify(jsonGen).writeNumber("null");
            reset(jsonGen);

            ser.serialize(42f / 0.0f, jsonGen, null);
            verify(jsonGen).writeNumber("null");
            reset(jsonGen);

            ser.serialize(42f, jsonGen, null);
            verify(jsonGen).writeNumber(42f);
        } catch (IOException e) {
            fail();
        }


    }

}
