/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
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

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for the {@link ActionProgress}.
 */
public class ActionProgressTest {

    private Event.Name actionName;
    private List<Bucket.Label> winnersSoFar;
    private List<Bucket.Label> losersSoFar;
    private boolean hasSufficientData;
    private Double fractionDataCollected;
    private Progress progress;
    private ActionProgress actionProgress;

    @Before
    public void setup(){
        actionName = Event.Name.valueOf("TestAction");
        winnersSoFar = new ArrayList<>();
        losersSoFar = new ArrayList<>();
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
    public void testBuilder(){
        assertEquals(actionProgress.getActionName(), actionName);

        assertTrue(actionProgress.hashCode() == actionProgress.clone().hashCode());

        String actionProg = actionProgress.toString();
        assertTrue(actionProg.contains(actionName.toString()));
        assertTrue(actionProg.contains(fractionDataCollected.toString()));
        assertTrue(actionProg.contains(String.valueOf(hasSufficientData)));
        assertTrue(actionProg.contains(String.valueOf(winnersSoFar)));
        assertTrue(actionProg.contains(String.valueOf(losersSoFar)));

        assertTrue(actionProgress.equals(actionProgress.clone()));
        assertTrue(actionProgress.equals(actionProgress));
        assertFalse(actionProgress.equals(null));
        assertFalse(actionProgress.equals(progress));
    }

    @Test
    public void testBuilderWithProgress(){
        Progress prog = new Progress();
        prog.setFractionDataCollected(fractionDataCollected);
        prog.setHasSufficientData(false);
        prog.setLosersSoFar(losersSoFar);
        prog.setWinnersSoFar(winnersSoFar);

        actionProgress =  new ActionProgress.Builder().withProgress(prog).build();

        assertEquals(actionProgress.getFractionDataCollected(), fractionDataCollected);
        assertEquals(actionProgress.getLosersSoFar(), losersSoFar);
        assertEquals(actionProgress.getWinnersSoFar(), winnersSoFar);
    }

    @Test
    public void testSettersAndGetters(){
        actionProgress.setActionName(null);
        assertEquals(actionProgress.getActionName(), null);
    }

}
