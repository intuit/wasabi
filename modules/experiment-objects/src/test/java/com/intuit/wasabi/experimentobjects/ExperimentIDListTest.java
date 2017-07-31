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

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * This class tests the functionality of the {@link ExperimentIDList}
 */
public class ExperimentIDListTest {

    private List<Experiment.ID> ids = new ArrayList<>();

    @Before
    public void initList() {
        for (int i = 0; i < 21; i++) {
            ids.add(Experiment.ID.newInstance());
        }
    }

    @Test
    public void testBuilder() {
        ExperimentIDList expList = ExperimentIDList.newInstance().withExperimentIDs(ids).build();
        //list are equal iff their elements are equal!
        assertEquals(expList.getExperimentIDs(), ids);

        ExperimentIDList expListClone = ExperimentIDList.from(expList).build();
        assertEquals(expList.getExperimentIDs(), expListClone.getExperimentIDs());

    }

    @Test
    public void testEquals() {
        ExperimentIDList oneList = ExperimentIDList.withExperimentIDList(ids).build();
        ExperimentIDList anotherList = ExperimentIDList.withExperimentIDList(ids).build();
        // the same Id's should lead to equal lists
        assertEquals(oneList, anotherList);
        //remove an element -> not equal anymore
        anotherList.getExperimentIDs().remove(0);
        assertNotEquals(oneList, anotherList);

        assertNotEquals(oneList, null);
        assertNotEquals(oneList, "Apple");
        assertEquals(oneList, oneList);
    }

    @Test
    public void testHashCode() {
        ExperimentIDList oneList = ExperimentIDList.withExperimentIDList(ids).build();
        ExperimentIDList anotherList = ExperimentIDList.withExperimentIDList(ids).build();

        assertEquals(oneList.hashCode(), anotherList.hashCode());
        assertEquals(oneList.hashCode(), oneList.hashCode());
    }

    @Test
    public void testToString() {

        ExperimentIDList list = ExperimentIDList.withExperimentIDList(ids).build();
        assertEquals(list.toString(),
                "ExperimentIDList[experimentIDs=" + ids + "]");

    }
}
