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

import com.intuit.wasabi.experimentobjects.Bucket;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests the {@link Progress}.
 */
public class ProgressTest {

    private Set<Bucket.Label> winnersSoFar;
    private Set<Bucket.Label> losersSoFar;
    private boolean hasSufficientData;
    private Double fractionDataCollected;
    private Progress progress;

    @Before
    public void setup() {
        winnersSoFar = new HashSet<>();
        losersSoFar = new HashSet<>();
        Bucket.Label winner = Bucket.Label.valueOf("TestWinner");
        Bucket.Label loser = Bucket.Label.valueOf("TestLoser");
        winnersSoFar.add(winner);
        losersSoFar.add(loser);
        hasSufficientData = true;
        fractionDataCollected = 0.5;
        progress = new Progress.Builder().withFractionDataCollected(fractionDataCollected)
                .withSufficientData(hasSufficientData)
                .withWinnersSoFar(winnersSoFar).withLosersSoFar(losersSoFar).build();

    }

    @Test
    public void testBuilder() {
        assertThat(progress.getFractionDataCollected(), is(fractionDataCollected));
        assertThat(progress.getLosersSoFar(), is(losersSoFar));
        assertThat(progress.getWinnersSoFar(), is(winnersSoFar));
        assertThat(progress.isHasSufficientData(), is(hasSufficientData));

        assertThat(progress.hashCode(), is(progress.clone().hashCode()));

        String prog = progress.toString();
        assertThat(prog, containsString(String.valueOf(fractionDataCollected)));
        assertThat(prog, containsString(losersSoFar.toString()));
        assertThat(prog, containsString(winnersSoFar.toString()));
        assertThat(prog, containsString(String.valueOf(hasSufficientData)));

        assertThat(progress, equalTo(progress.clone()));
        assertThat(progress, equalTo(progress));
        assertThat(progress, not(equalTo(null)));
        assertThat(progress, not(equalTo(fractionDataCollected)));
    }

    @Test
    public void testAddWinners() {
        progress.setWinnersSoFar(null);
        Bucket.Label bucketLabel = Bucket.Label.valueOf("TestWinner");
        progress.addToWinnersSoFarList(bucketLabel);
        assertThat(progress.getWinnersSoFar(), contains(bucketLabel));
        try {
            progress.addToWinnersSoFarList(null);
            fail();
        } catch (IllegalArgumentException e) {
            //expected this exception
        }
        progress.addToWinnersSoFarList(Bucket.Label.valueOf("TestWinner"));
        assertThat(progress.getWinnersSoFar(), is(winnersSoFar));
    }

    @Test
    public void testAddLosers() {
        progress.setLosersSoFar(null);
        Bucket.Label bucketLabel = Bucket.Label.valueOf("TestLoser");
        progress.addToLosersSoFarList(bucketLabel);
        assertThat(progress.getLosersSoFar(), contains(bucketLabel));
        try {
            progress.addToLosersSoFarList(null);
            fail();
        } catch (IllegalArgumentException e) {
            //expected this exception
        }
        progress.addToLosersSoFarList(Bucket.Label.valueOf("TestLoser"));
        assertThat(progress.getLosersSoFar(), is(losersSoFar));
    }

    @Test
    public void testSettersAndGetters() {
        fractionDataCollected = 0.0;
        progress.setFractionDataCollected(fractionDataCollected);
        progress.setHasSufficientData(!hasSufficientData);
        assertThat(progress.isHasSufficientData(), not(hasSufficientData));
        assertThat(progress.getFractionDataCollected(), is(fractionDataCollected));
    }
}
