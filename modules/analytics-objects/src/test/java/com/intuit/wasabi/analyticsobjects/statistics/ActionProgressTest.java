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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.*;

public class ActionProgressTest {
    Event.Name actionName;
    List<Bucket.Label> winnersSoFar;
    List<Bucket.Label> losersSoFar;
    boolean hasSufficientData;
    Double fractionDataCollected;
    Progress progress;
    ActionProgress actionProgress;

    @Before
    public void setup() {
        actionName = Event.Name.valueOf("TestAction");
        winnersSoFar = new ArrayList<>();
        losersSoFar = new ArrayList<>();
        Bucket.Label winner = Bucket.Label.valueOf("TestWinner");
        Bucket.Label loser = Bucket.Label.valueOf("TestLoser");
        winnersSoFar.add(winner);
        losersSoFar.add(loser);
        hasSufficientData = true;
        fractionDataCollected = 0.5;
        progress = new Progress();
        actionProgress = new ActionProgress.Builder().withActionName(actionName).withFractionDataCollected(fractionDataCollected)
                .withSufficientData(hasSufficientData).withWinnersSoFarList(winnersSoFar).withLosersSoFarList(losersSoFar)
                .withProgress(progress).build();

    }

    @Test
    public void testBuilder() {
        assertEquals(actionProgress.getActionName(), actionName);

        assertNotNull(actionProgress.hashCode());
        assertNotNull(actionProgress.toString());
        assertNotNull(actionProgress.clone());
        assertTrue(actionProgress.equals(actionProgress.clone()));
        assertTrue(actionProgress.equals(actionProgress));
        assertFalse(actionProgress.equals(null));
        Assert.assertFalse(actionProgress.equals(progress));
    }

    @Test
    public void testSettersAndGetters() {
        actionProgress.setActionName(null);
        assertEquals(actionProgress.getActionName(), null);
    }

}
