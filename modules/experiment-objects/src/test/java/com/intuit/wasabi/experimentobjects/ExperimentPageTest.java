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

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Testing the functionality of {@link ExperimentPage}
 * <p>
 * Created by asuckro on 8/20/15.
 */
public class ExperimentPageTest {

    @Test
    public void testEquals() {
        ExperimentPage ep = ExperimentPage.withAttributes(Page.Name.valueOf("fluffy"), true).build();
        ExperimentPage ep2 = ExperimentPage.withAttributes(Page.Name.valueOf("fluffy"), true).build();

        assertTrue(ep.equals(ep));
        assertFalse(ep.equals(null));
        assertFalse(ep.equals(42));
        assertTrue(ep.equals(ep2));
    }

    @Test
    public void testHashCode() {
        ExperimentPage ep = ExperimentPage.withAttributes(Page.Name.valueOf("fluffy"), true).build();
        ExperimentPage ep2 = ExperimentPage.withAttributes(Page.Name.valueOf("fluffy"), true).build();

        assertEquals(ep.hashCode(), ep2.hashCode());

        ep2.setAllowNewAssignment(false);

        assertNotEquals(ep.hashCode(), ep2.hashCode());

    }

    @Test
    public void testInitialize() {
        ExperimentPageList experimentList = new ExperimentPageList(11);
        assertThat(experimentList.getPages().size(), is(0));
        List<ExperimentPage> experimentPageList = new ArrayList<ExperimentPage>();
        ExperimentPage page = ExperimentPage.withAttributes(Page.Name.valueOf("page"), true).build();
        experimentPageList.add(page);
        experimentList.setPages(experimentPageList);
        assertThat(experimentList.toString(), is("ExperimentPageList[pages=[ExperimentPage[name=page,allowNewAssignment=true]]]"));
    }

}
