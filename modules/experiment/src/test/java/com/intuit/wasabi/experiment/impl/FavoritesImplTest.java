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

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.experiment.Favorites;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.FavoritesRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

/**
 * Tests for {@link FavoritesImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class FavoritesImplTest {

    @Mock
    private FavoritesRepository favoritesRepository;

    @Test
    public void testGetFavorites() throws Exception {
        Mockito.doReturn(Collections.emptyList()).when(favoritesRepository).getFavorites(Mockito.any());

        Favorites favorites = new FavoritesImpl(favoritesRepository);
        UserInfo.Username user = UserInfo.Username.valueOf("TestUser");
        favorites.getFavorites(user);

        Mockito.verify(favoritesRepository).getFavorites(user);
    }

    @Test
    public void testAddFavorites() throws Exception {
        Mockito.doReturn(Collections.emptyList()).when(favoritesRepository).addFavorite(Mockito.any(), Mockito.any());

        Favorites favorites = new FavoritesImpl(favoritesRepository);
        UserInfo.Username user = UserInfo.Username.valueOf("TestUser");
        Experiment.ID id = Experiment.ID.newInstance();
        favorites.addFavorite(user, id);

        Mockito.verify(favoritesRepository).addFavorite(user, id);
    }

    @Test
    public void testDeleteFavorites() throws Exception {
        Mockito.doReturn(Collections.emptyList()).when(favoritesRepository).deleteFavorite(Mockito.any(), Mockito.any());

        Favorites favorites = new FavoritesImpl(favoritesRepository);
        UserInfo.Username user = UserInfo.Username.valueOf("TestUser");
        Experiment.ID id = Experiment.ID.newInstance();
        favorites.deleteFavorite(user, id);

        Mockito.verify(favoritesRepository).deleteFavorite(user, id);
    }
}
