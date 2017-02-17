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
package com.intuit.wasabi.repository;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.experimentobjects.Experiment;

import java.util.List;

/**
 * Handles favorites inside the database.
 */
public interface FavoritesRepository {

    /**
     * Retrieves the list of favorites.
     *
     * @param username the requesting user
     * @return the list of favorites
     */
    List<Experiment.ID> getFavorites(UserInfo.Username username);

    /**
     * Adds an experiment ID to the favorites.
     *
     * @param username     the requesting user
     * @param experimentID the experiment to favorite
     * @return the updated list of favorites
     * @throws RepositoryException if the favorites can not be updated
     */
    List<Experiment.ID> addFavorite(UserInfo.Username username, Experiment.ID experimentID) throws RepositoryException;

    /**
     * Removes an experiment from the favorites.
     *
     * @param username     the requesting user
     * @param experimentID the experiment to unfavorite
     * @return the updated list of favorites
     * @throws RepositoryException if the favorites can not be updated
     */
    List<Experiment.ID> deleteFavorite(UserInfo.Username username, Experiment.ID experimentID) throws RepositoryException;
}
