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
package com.intuit.wasabi.database;

import com.intuit.wasabi.experimentobjects.Bucket;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BucketLabelArgumentFactoryTest {

    BucketLabelArgumentFactory factory = null;

    @Before
    public void setUp() {
        factory = new BucketLabelArgumentFactory();
    }

    @Test
    public void testAcceptsBucketLabelTrue() {
        assertEquals(true, factory.accepts(null, Bucket.Label.valueOf("b1"), null));
    }

    @Test
    public void testAcceptsStringFalse() {
        assertEquals(false, factory.accepts(null, "b1", null));
    }

    @Test
    public void testBuildBucketLabel() {
        assertEquals(StringArgument.class,
                factory.build(null, Bucket.Label.valueOf("b1"), null).getClass());
    }
}
