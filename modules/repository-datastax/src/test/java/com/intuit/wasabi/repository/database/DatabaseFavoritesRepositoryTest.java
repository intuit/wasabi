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
package com.intuit.wasabi.repository.database;

import com.googlecode.flyway.core.Flyway;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.database.Transaction;
import com.intuit.wasabi.database.TransactionFactory;
import com.intuit.wasabi.exceptions.ConstraintViolationException;
import com.intuit.wasabi.exceptions.DatabaseException;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Tests for {@link DatabaseFavoritesRepository}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DatabaseFavoritesRepositoryTest {

    private DatabaseFavoritesRepository databaseFavoritesRepository;
    private UserInfo.Username username = UserInfo.Username.valueOf("Username");

    @Mock
    private Flyway flyway;

    @Mock
    private TransactionFactory transactionFactory;

    @Mock
    private Transaction transaction;

    @Before
    public void setup() {
        // mock the flyway setup
        Mockito.doNothing().when(flyway).setLocations(Mockito.anyString());
        Mockito.doNothing().when(flyway).setDataSource(Mockito.any());
        Mockito.doReturn(0).when(flyway).migrate();
        Mockito.doReturn(null).when(transactionFactory).getDataSource();

        Mockito.doReturn(transaction).when(transactionFactory).newTransaction();

        // instantiate repository
        databaseFavoritesRepository = new DatabaseFavoritesRepository(transactionFactory, flyway, "com/intuit/wasabi/repository/impl/mysql/migration");
    }

    @Test
    public void testGetFavorites() throws Exception {
        List<Map<String, byte[]>> queryResult = prepareFavorites();
        Mockito.doReturn(queryResult).when(transaction).select(Mockito.anyString(), Mockito.eq(username.getUsername()));

        List<Experiment.ID> results = databaseFavoritesRepository.getFavorites(username);
        Assert.assertArrayEquals("Not all expected favorites are returned.",
                expectedResult(queryResult).toArray(), results.toArray());
    }

    @Test
    public void testDeleteFavorite() throws Exception {
        List<Map<String, byte[]>> queryResult = prepareFavorites();
        Mockito.doReturn(queryResult).when(transaction).select(Mockito.anyString(), Mockito.eq(username.getUsername()));
        Mockito.doNothing().when(transaction).insert(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        List<Experiment.ID> results = databaseFavoritesRepository.deleteFavorite(username, Experiment.ID.newInstance());
        Assert.assertArrayEquals("Not all expected favorites are returned.",
                expectedResult(queryResult).toArray(), results.toArray());
    }

    @Test
    public void testAddFavorite() throws Exception {
        List<Map<String, byte[]>> queryResult = prepareFavorites();
        Mockito.doReturn(queryResult).when(transaction).select(Mockito.anyString(), Mockito.eq(username.getUsername()));
        Mockito.doNothing().when(transaction).insert(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        List<Experiment.ID> results = databaseFavoritesRepository.addFavorite(username, Experiment.ID.newInstance());
        Assert.assertArrayEquals("Not all expected favorites are returned.",
                expectedResult(queryResult).toArray(), results.toArray());
    }

    @Test
    public void testUpdateFailureFavorite() throws Exception {
        List<Map<String, byte[]>> queryResult = prepareFavorites();
        Mockito.doReturn(queryResult).when(transaction).select(Mockito.anyString(), Mockito.eq(username.getUsername()));
        Mockito.doThrow(new ConstraintViolationException(
                ConstraintViolationException.Reason.UNIQUE_CONSTRAINT_VIOLATION))
                .when(transaction).insert(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        try {
            databaseFavoritesRepository.addFavorite(username, Experiment.ID.newInstance());
            Assert.fail("Expected a DatabaseException to be thrown.");
        } catch (DatabaseException expected) {
            // ignored
        } catch (Exception wrongException) {
            Assert.fail("Caught " + wrongException + " but expected DatabaseException!");
        }
    }

    /**
     * Simulates a database response with multiple rows.
     *
     * @return a simulated database response
     */
    private List<Map<String, byte[]>> prepareFavorites() {
        List<Map<String, byte[]>> queryResult = new ArrayList<>();
        for (int i = 0; i < 8; ++i) {
            Map<String, byte[]> row = new HashMap<>();
            byte[] id = new byte[16];
            new Random().nextBytes(id);
            id[0] = (byte) i;
            row.put("experiment_id", id);
            queryResult.add(row);
        }
        return queryResult;
    }

    /**
     * Extracts expected results from a simulated database list.
     *
     * @param queryResult the database list
     * @return the expected response
     */
    private List<Experiment.ID> expectedResult(List<Map<String, byte[]>> queryResult) {
        List<Experiment.ID> resultList = new ArrayList<>(queryResult.size());
        queryResult.forEach(map -> resultList.add(Experiment.ID.valueOf(map.get("experiment_id"))));
        return resultList;
    }
}
