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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;


/**
 * This tests the functionality of the {@link ExperimentList}
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
    }

    @Test
    public void testContents() {
        ExperimentList el = new ExperimentList(3);
        Experiment exp1 = new Experiment();
        Experiment exp2 = new Experiment();
        el.addExperiment(exp1);
        el.addExperiment(exp2);

        assertTrue("Experiments are not in the list.", el.getExperiments().contains(exp1) && el.getExperiments().contains(exp2));
    }

}
