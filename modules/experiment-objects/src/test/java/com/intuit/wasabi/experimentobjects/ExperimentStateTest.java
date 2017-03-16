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

import static com.intuit.wasabi.experimentobjects.Experiment.State.DELETED;
import static com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT;
import static com.intuit.wasabi.experimentobjects.Experiment.State.PAUSED;
import static com.intuit.wasabi.experimentobjects.Experiment.State.RUNNING;
import static com.intuit.wasabi.experimentobjects.Experiment.State.TERMINATED;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ExperimentStateTest {

    @Test
    public void draft() throws Exception {
        assertThat(DRAFT.isStateTransitionAllowed(DRAFT), is(TRUE));
        assertThat(DRAFT.isStateTransitionAllowed(DELETED), is(TRUE));
        assertThat(DRAFT.isStateTransitionAllowed(RUNNING), is(TRUE));
        assertThat(DRAFT.isStateTransitionAllowed(PAUSED), is(TRUE));
        assertThat(DRAFT.isStateTransitionAllowed(TERMINATED), is(FALSE));
    }

    @Test
    public void running() throws Exception {
        assertThat(RUNNING.isStateTransitionAllowed(RUNNING), is(TRUE));
        assertThat(RUNNING.isStateTransitionAllowed(PAUSED), is(TRUE));
        assertThat(RUNNING.isStateTransitionAllowed(TERMINATED), is(TRUE));
        assertThat(RUNNING.isStateTransitionAllowed(DRAFT), is(FALSE));
        assertThat(RUNNING.isStateTransitionAllowed(DELETED), is(FALSE));
    }

    @Test
    public void testPaused() throws Exception {
        assertThat(PAUSED.isStateTransitionAllowed(PAUSED), is(TRUE));
        assertThat(PAUSED.isStateTransitionAllowed(RUNNING), is(TRUE));
        assertThat(PAUSED.isStateTransitionAllowed(TERMINATED), is(TRUE));
        assertThat(PAUSED.isStateTransitionAllowed(DELETED), is(FALSE));
        assertThat(PAUSED.isStateTransitionAllowed(DRAFT), is(FALSE));
    }

    @Test
    public void terminated() throws Exception {
        assertThat(TERMINATED.isStateTransitionAllowed(TERMINATED), is(TRUE));
        assertThat(TERMINATED.isStateTransitionAllowed(DELETED), is(TRUE));
        assertThat(TERMINATED.isStateTransitionAllowed(DRAFT), is(FALSE));
        assertThat(TERMINATED.isStateTransitionAllowed(RUNNING), is(FALSE));
        assertThat(TERMINATED.isStateTransitionAllowed(PAUSED), is(FALSE));
    }

    @Test
    public void deleted() throws Exception {
        assertThat(DELETED.isStateTransitionAllowed(DRAFT), is(FALSE));
        assertThat(DELETED.isStateTransitionAllowed(RUNNING), is(FALSE));
        assertThat(DELETED.isStateTransitionAllowed(PAUSED), is(FALSE));
        assertThat(DELETED.isStateTransitionAllowed(TERMINATED), is(FALSE));
    }
}
