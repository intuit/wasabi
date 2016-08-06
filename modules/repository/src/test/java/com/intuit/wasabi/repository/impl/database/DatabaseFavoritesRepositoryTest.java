package com.intuit.wasabi.repository.impl.database;

import com.googlecode.flyway.core.Flyway;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.database.Transaction;
import com.intuit.wasabi.database.TransactionFactory;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

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
        Mockito.doNothing().when(flyway).setDataSource(Mockito. any());
        Mockito.doNothing().when(flyway).migrate();
        Mockito.doReturn(Mockito. any()).when(transactionFactory).getDataSource();

        // newTransaction().select(sql, username);
        Mockito.doReturn(transaction).when(transactionFactory).newTransaction();

        // instantiate repository
        databaseFavoritesRepository = new DatabaseFavoritesRepository(transactionFactory, flyway);
    }

    @Test
    public void testGetFavorites() throws Exception {
        List<Map<String, byte[]>> resultList = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            Map<String, byte[]> row = new HashMap<>();
            row.put("experiment_id", new byte[] {1, (byte)i});
            resultList.add(row);
        }
        Mockito.doReturn(resultList).when(transaction).select(Mockito.anyString(), username);

        List<Experiment.ID> results = databaseFavoritesRepository.getFavorites(username);
        Assert.assertArrayEquals("Not all expected favorites are returned.", resultList.toArray(), results.toArray());
    }

    @Test
    public void testDeleteFavorite() throws Exception {
        List<Map<String, byte[]>> resultList = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            Map<String, byte[]> row = new HashMap<>();
            row.put("experiment_id", new byte[] {1, (byte)i});
            resultList.add(row);
        }
        Mockito.doReturn(resultList).when(transaction).select(Mockito.anyString(), username);
        Mockito.doNothing().when(transaction).insert(Mockito.any(), Mockito.anyCollection().toArray());

        List<Experiment.ID> results = databaseFavoritesRepository.deleteFavorite(username, Mockito.any());
        Assert.assertArrayEquals("Not all expected favorites are returned.", resultList.toArray(), results.toArray());
    }

    @Test
    public void testAddFavorite() throws Exception {
        List<Map<String, byte[]>> resultList = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            Map<String, byte[]> row = new HashMap<>();
            row.put("experiment_id", new byte[] {1, (byte)i});
            resultList.add(row);
        }
        Mockito.doReturn(resultList).when(transaction).select(Mockito.anyString(), username);
        Mockito.doNothing().when(transaction).insert(Mockito.any(), Mockito.anyCollection().toArray());

        List<Experiment.ID> results = databaseFavoritesRepository.addFavorite(username, Mockito.any());
        Assert.assertArrayEquals("Not all expected favorites are returned.", resultList.toArray(), results.toArray());
    }
}
