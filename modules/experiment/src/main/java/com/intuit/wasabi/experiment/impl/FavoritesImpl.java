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
package com.intuit.wasabi.experiment.impl;

import com.google.inject.Inject;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.experiment.Favorites;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.FavoritesRepository;

import java.util.List;

/**
 * An implementation for Favorites.
 */
public class FavoritesImpl implements Favorites {

    private final FavoritesRepository favoritesRepository;

    /**
     * Initializes the default FavoritesImpl.
     *
     * @param favoritesRepository the favorites repository
     */
    @Inject
    public FavoritesImpl(final FavoritesRepository favoritesRepository) {
        this.favoritesRepository = favoritesRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Experiment.ID> getFavorites(UserInfo.Username username) {
        return favoritesRepository.getFavorites(username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Experiment.ID> addFavorite(UserInfo.Username username, Experiment.ID experimentID) {
        return favoritesRepository.addFavorite(username, experimentID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Experiment.ID> deleteFavorite(UserInfo.Username username, Experiment.ID experimentID) {
        return favoritesRepository.deleteFavorite(username, experimentID);
    }
}
