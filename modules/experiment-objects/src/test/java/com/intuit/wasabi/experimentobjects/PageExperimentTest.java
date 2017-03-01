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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * This class tests the functionality of the {@link PageExperiment}
 * <p>
 * Created by asuckro on 8/19/15.
 */
public class PageExperimentTest {

    private Experiment.ID id = Experiment.ID.newInstance();
    private Experiment.Label label = Experiment.Label.valueOf("expLabel");
    private PageExperiment pe = PageExperiment.withAttributes(id, label, false).build();


    @Test
    public void testBuilder() {

        assertEquals(pe.getLabel(), label);
        assertEquals(pe.getId(), id);
        assertFalse(pe.getAllowNewAssignment());

        id = Experiment.ID.newInstance();
        pe.setId(id);
        assertEquals(pe.getId(), id);

        label = Experiment.Label.valueOf("NewLabel");
        pe.setLabel(label);
        assertEquals(pe.getLabel(), label);

        pe.setAllowNewAssignment(true);
        assertTrue(pe.getAllowNewAssignment());
    }

    @Test
    public void testHashCode() {

        PageExperiment pe2 = PageExperiment.withAttributes(id, label, false).build();

        assertEquals(pe.hashCode(), pe2.hashCode());

        pe2.setId(Experiment.ID.newInstance());
        assertNotEquals(pe.hashCode(), pe2.hashCode());

    }

    @Test
    public void testEquals() {

        PageExperiment pe2 = PageExperiment.withAttributes(id, label, false).build();

        assertFalse(pe.equals(null));
        assertFalse(pe.equals("notAnPageExperiment"));
        assertTrue(pe.equals(pe));

        assertTrue(pe.equals(pe2));
        pe2.setAllowNewAssignment(true);
        assertFalse(pe.equals(pe2));
    }

    @Test
    public void testClone() throws Exception {
        PageExperiment pClone = pe.clone();
        assertEquals(pClone, pe);
    }

    @Test
    public void testToString() {
        assertEquals(pe.toString(), "PageExperiment{" +
                "id=" + id +
                ", label=" + label +
                ", allowNewAssignment=" + false + '}');
    }
}
