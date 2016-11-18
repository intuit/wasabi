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
package com.intuit.wasabi.analyticsobjects.statistics;

import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.experimentobjects.Bucket;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;


/**
 * Test for the {@link ActionProgress}.
 */
public class ActionProgressTest {

    private Event.Name actionName;
    private Set<Bucket.Label> winnersSoFar;
    private Set<Bucket.Label> losersSoFar;
    private boolean hasSufficientData;
    private Double fractionDataCollected;
    private ActionProgress actionProgress;

    @Before
    public void setup() {
        actionName = Event.Name.valueOf("TestAction");
        winnersSoFar = new HashSet<>();
        losersSoFar = new HashSet<>();
        Bucket.Label winner = Bucket.Label.valueOf("TestWinner");
        Bucket.Label loser = Bucket.Label.valueOf("TestLoser");
        winnersSoFar.add(winner);
        losersSoFar.add(loser);
        hasSufficientData = true;
        fractionDataCollected = 0.5;

        actionProgress = new ActionProgress.Builder().withActionName(actionName)
                .withFractionDataCollected(fractionDataCollected)
                .withSufficientData(hasSufficientData).withWinnersSoFarList(winnersSoFar)
                .withLosersSoFarList(losersSoFar).build();

    }

    @Test
    public void testBuilder() {
        assertThat(actionProgress.getActionName(), equalTo(actionName));

        assertThat(actionProgress.hashCode(), is(actionProgress.clone().hashCode()));

        String actionProg = actionProgress.toString();
        assertThat(actionProg, containsString(actionName.toString()));
        assertThat(actionProg, containsString(fractionDataCollected.toString()));
        assertThat(actionProg, containsString(String.valueOf(hasSufficientData)));
        assertThat(actionProg, containsString(String.valueOf(winnersSoFar)));
        assertThat(actionProg, containsString(String.valueOf(losersSoFar)));

        assertThat(actionProgress, equalTo(actionProgress.clone()));
        assertThat(actionProgress, equalTo(actionProgress));
        assertThat(actionProgress, notNullValue());
        assertThat(actionProgress, not(equalTo(new Progress())));
    }

    @Test
    public void testBuilderWithProgress() {
        Progress prog = new Progress();
        prog.setFractionDataCollected(fractionDataCollected);
        prog.setHasSufficientData(false);
        prog.setLosersSoFar(losersSoFar);
        prog.setWinnersSoFar(winnersSoFar);

        actionProgress = new ActionProgress.Builder().withProgress(prog).build();

        assertThat(actionProgress.getFractionDataCollected(), equalTo(fractionDataCollected));
        assertThat(actionProgress.getLosersSoFar(), equalTo(losersSoFar));
        assertThat(actionProgress.getWinnersSoFar(), equalTo(winnersSoFar));
    }

    @Test
    public void testSettersAndGetters() {
        actionProgress.setActionName(null);
        assertThat(actionProgress.getActionName(), nullValue());
    }

}
