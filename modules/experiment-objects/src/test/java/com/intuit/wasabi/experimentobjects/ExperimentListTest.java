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
package com.intuit.wasabi.experimentobjects;

import org.junit.Test;

import static org.junit.Assert.*;


/**
 * This tests the functionality of the {@link ExperimentList}
 *
 * Created by asuckro on 8/20/15.
 */
public class ExperimentListTest {

    private ExperimentList expList = new ExperimentList(42);
    private ExperimentList expList2 = new ExperimentList(7);

    @Test
    public void testBuilder() {
        //equals of the lists is dependend on the elements inside the list
        assertEquals(expList.hashCode(), expList2.hashCode());

        expList2.addExperiment(new Experiment());
        assertNotEquals(expList.hashCode(), expList2.hashCode());
    }

    @Test
    public void testEquals() {

        assertTrue(expList.equals(expList2));
        assertFalse(expList.equals(null));
        assertFalse(expList.equals(42));

    }

}
