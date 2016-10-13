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
package com.intuit.wasabi.tests.service.experiment;

import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FavoritesTest extends TestBase {
    private final List<Experiment> experimentList = new ArrayList<>(12);
    private final String experimentPrefix = "FavoriteExperimentTestExperiments";

    public FavoritesTest() {
        setResponseLogLengthLimit(500);
    }

    /**
     * Sets up 12 experiments to test favorites properly.
     */
    @BeforeClass(dependsOnGroups = {"ping", "basicExperimentTests"})
    public void setup() {
        cleanup();
        for (int i = 0; i < 3; i++) {
            experimentList.add(postExperiment(
                    ExperimentFactory.createCompleteExperiment()
                            .setLabel(String.format(experimentPrefix + "Exp%02d", i))));
        }
    }

    @Test
    public void testGetEmptyFavorites() {
        List<String> favorites = getFavorites();
        Assert.assertTrue(favorites.isEmpty(), "Favorites are not empty!");
    }

    @Test(dependsOnMethods = {"testGetEmptyFavorites"})
    public void testAddFavorites() {
        List<String> favorites = addFavorite(experimentList.get(0).id);
        Assert.assertEquals(favorites.size(), 1, "There is not exactly one favorite!");
        Assert.assertEquals(favorites.get(0), experimentList.get(0).id,
                "The element in favorites is not the correct one.");

        favorites = addFavorite(experimentList.get(1).id);
        Assert.assertEquals(favorites.size(), 2, "There are not exactly two favorites after insertion!");
        Assert.assertTrue(favorites.contains(experimentList.get(0).id),
                "The favorites don't contain the first ID (index 0) after insertion.");
        Assert.assertTrue(favorites.contains(experimentList.get(1).id),
                "The favorites don't contain the second ID (index 1) after insertion.");
    }

    @Test(dependsOnMethods = {"testAddFavorites"})
    public void testGetFavorites() {
        List<String> favorites = getFavorites();
        Assert.assertEquals(favorites.size(), 2, "There are not exactly two favorites!");
        Assert.assertTrue(favorites.contains(experimentList.get(0).id),
                "The favorites don't contain the first ID (index 0) anymore.");
        Assert.assertTrue(favorites.contains(experimentList.get(1).id),
                "The favorites don't contain the second ID (index 1) anymore.");
    }

    @Test(dependsOnMethods = {"testAddFavorites", "testGetFavorites"})
    public void testGetFavoritesDeletedExperiment() {
        addFavorite(experimentList.get(2).id);
        deleteExperiment(experimentList.get(2));

        List<String> favorites = getFavorites();

        Assert.assertEquals(favorites.size(), 2, "There should only be two favorites!");
        Assert.assertTrue(favorites.contains(experimentList.get(0).id),
                "The favorites don't contain the first ID (index 0) anymore.");
        Assert.assertTrue(favorites.contains(experimentList.get(1).id),
                "The favorites don't contain the second ID (index 1) anymore.");
        Assert.assertFalse(favorites.contains(experimentList.get(2).id),
                "The favorites do contain the third ID (index 2), which is wrong.");
    }

    @Test(dependsOnMethods = {"testGetFavorites", "testGetFavoritesDeletedExperiment"})
    public void testDeleteOneFavorite() {
        List<String> favorites = deleteFavorite(experimentList.get(0).id);
        Assert.assertEquals(favorites.size(), 1, "There is not only one remaining favorite!");
        Assert.assertFalse(favorites.contains(experimentList.get(0).id),
                "The favorites still contain the first ID (index 0).");
        Assert.assertTrue(favorites.contains(experimentList.get(1).id),
                "The favorites don't contain the second ID (index 1) anymore.");
    }

    @Test(dependsOnMethods = {"testDeleteOneFavorite"})
    public void testDeleteFavorites() {
        List<String> favorites = deleteFavorite(experimentList.get(1).id);
        Assert.assertEquals(favorites.size(), 0, "There are remaining favorites!");
    }

    @AfterClass
    public void cleanup() {
        List<Map<String, Object>> experimentMaps = apiServerConnector
                .doGet("experiments?per_page=-1&filter=" + experimentPrefix)
                .jsonPath().getList("experiments");
        List<Experiment> experiments = experimentMaps.stream()
                .map(experimentMap -> ExperimentFactory.createFromJSONString(simpleGson.toJson(experimentMap)))
                .collect(Collectors.toList());
        deleteExperiments(experiments);
    }
}
