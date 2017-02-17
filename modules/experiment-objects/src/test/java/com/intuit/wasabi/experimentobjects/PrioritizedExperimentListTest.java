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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test for the {@link PrioritizedExperimentList}
 * <p>
 * Created by asuckro on 8/20/15.
 */
public class PrioritizedExperimentListTest {

    static final Field field;

    static {
        try {
            field = ArrayList.class.getDeclaredField("elementData");
            field.setAccessible(true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <E> int getArrayListCapacity(List<E> arrayList) {
        try {
            final E[] elementData = (E[]) field.get(arrayList);
            return elementData.length;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void testConstructor() {
        PrioritizedExperimentList prioList = new PrioritizedExperimentList(42);
        assertEquals(42, getArrayListCapacity(prioList.getPrioritizedExperiments()));

        List<PrioritizedExperiment> emptyList = new ArrayList<>();
        prioList.setPrioritizedExperiments(emptyList);
        assertEquals(0, getArrayListCapacity(prioList.getPrioritizedExperiments()));

    }


}
