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

import com.intuit.wasabi.experimentobjects.Bucket;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProgressTest {
    List<Bucket.Label> winnersSoFar;
    List<Bucket.Label> losersSoFar;
    boolean hasSufficientData;
    Double fractionDataCollected;
    Progress progress;

    @Before
    public void setup(){
        winnersSoFar = new ArrayList<Bucket.Label>();
        losersSoFar = new ArrayList<Bucket.Label>();
        Bucket.Label winner = Bucket.Label.valueOf("TestWinner");
        Bucket.Label loser = Bucket.Label.valueOf("TestLoser");
        winnersSoFar.add(winner);
        losersSoFar.add(loser);
        hasSufficientData = true;
        fractionDataCollected = 0.5;
        progress = new Progress.Builder().withFractionDataCollected(fractionDataCollected).withSufficientData(hasSufficientData)
                    .withWinnersSoFar(winnersSoFar).withLosersSoFar(losersSoFar).build();

    }

    @Test
    public void testBuilder(){
        assertEquals(progress.getFractionDataCollected(), fractionDataCollected);
        assertEquals(progress.getLosersSoFar(), losersSoFar);
        assertEquals(progress.getWinnersSoFar(), winnersSoFar);
        assertEquals(progress.isHasSufficientData(), hasSufficientData);

        assertTrue(progress.equals(progress.clone()));
        assertTrue(progress.equals(progress));
        assertFalse(progress.equals(null));
        assertFalse(progress.equals(fractionDataCollected));
    }

    @Test
    public void testAddWinners(){
        progress.setWinnersSoFar(null);
        progress.addToWinnersSoFarList(Bucket.Label.valueOf("TestWinner"));
        try{
            progress.addToWinnersSoFarList(null);
            fail();
        }catch (IllegalArgumentException e){
            //expected this exception
        }
        progress.addToWinnersSoFarList(Bucket.Label.valueOf("TestWinner"));
        assertEquals(progress.getWinnersSoFar(), winnersSoFar);
    }

    @Test
    public void testAddLosers() {
        progress.setLosersSoFar(null);
        progress.addToLosersSoFarList(Bucket.Label.valueOf("TestLoser"));
        try {
            progress.addToLosersSoFarList(null);
            fail();
        } catch (IllegalArgumentException e) {
            //expected this exception
        }
        progress.addToLosersSoFarList(Bucket.Label.valueOf("TestLoser"));
        assertEquals(progress.getLosersSoFar(), losersSoFar);
    }

    @Test
    public void testSettersAndGetters(){
        fractionDataCollected = 0.0;
        progress.setFractionDataCollected(fractionDataCollected);
        progress.setHasSufficientData(!hasSufficientData);
        assertEquals(progress.isHasSufficientData(), !hasSufficientData);
        assertEquals(progress.getFractionDataCollected(), fractionDataCollected);

        fractionDataCollected = 0.5;
        progress.setHasSufficientData(hasSufficientData);
    }
}
