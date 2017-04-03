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

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.googlecode.flyway.core.Flyway;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.database.TransactionFactory;
import com.intuit.wasabi.exceptions.DatabaseException;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.FavoritesRepository;
import com.intuit.wasabi.repository.RepositoryException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@inheritDoc}
 */
public class DatabaseFavoritesRepository implements FavoritesRepository {

    private final TransactionFactory transactionFactory;

    /**
     * Creates a DatabaseFavoritesRepository.
     *
     * @param transactionFactory the transaction factory
     * @param flyway             the flyway instance to initialize the database
     */
    @Inject
    public DatabaseFavoritesRepository(final TransactionFactory transactionFactory, final Flyway flyway,
                                       final @Named("mysql.mutagen.root.resource.path") String mutagenRootResourcePath) {
        this.transactionFactory = transactionFactory;
        initialize(flyway, mutagenRootResourcePath);
    }

    /**
     * Initializes the database.
     *
     * @param flyway flyway instance
     */
    void initialize(Flyway flyway, String mutagenRootResourcePath) {
        flyway.setLocations(mutagenRootResourcePath);
        flyway.setDataSource(transactionFactory.getDataSource());
        flyway.migrate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Experiment.ID> getFavorites(UserInfo.Username username) {
        // Remove favorites of now deleted experiments
        String updateSQL = "UPDATE user_experiment_properties INNER JOIN experiment "
                + "ON user_experiment_properties.experiment_id = experiment.id "
                + "SET user_experiment_properties.is_favorite = 0 "
                + "WHERE experiment.state = 'DELETED' AND user_experiment_properties.is_favorite = 1;";
        transactionFactory.newTransaction().update(updateSQL);

        String sql = "SELECT experiment_id FROM user_experiment_properties WHERE user_id = ? AND is_favorite = 1;";
        List list = transactionFactory.newTransaction().select(sql, username.toString());

        List<Experiment.ID> favorites = new ArrayList<>(list.size());
        for (Object row : list) {
            if (row instanceof Map) {
                Map map = (Map) row;
                if (map.get("experiment_id") instanceof byte[]) {
                    favorites.add(Experiment.ID.valueOf((byte[]) map.get("experiment_id")));
                }
            }
        }

        return favorites;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Experiment.ID> addFavorite(UserInfo.Username username, Experiment.ID experimentID)
            throws RepositoryException {
        updateFavorite(username, experimentID, true);
        return getFavorites(username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Experiment.ID> deleteFavorite(UserInfo.Username username, Experiment.ID experimentID)
            throws RepositoryException {
        updateFavorite(username, experimentID, false);
        return getFavorites(username);
    }

    /**
     * Inserts or updates the favorite value of an experiment for a user in the database.
     * Sets the value to {@code favorite}.
     *
     * @param username     the username
     * @param experimentID the experiment ID
     * @param favorite     the favorite status
     * @throws DatabaseException if the update was unsuccessful
     */
    private void updateFavorite(UserInfo.Username username, Experiment.ID experimentID, boolean favorite)
            throws DatabaseException {
        String sql = "INSERT INTO user_experiment_properties (user_id, experiment_id, is_favorite) "
                + "VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY "
                + "UPDATE is_favorite = VALUES(is_favorite);";
        try {
            transactionFactory.newTransaction().insert(sql, username.toString(), experimentID, favorite);
        } catch (Exception e) {
            throw new DatabaseException("Can not update favorites in the database.", e);
        }
    }
}
