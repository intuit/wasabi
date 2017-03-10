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
package com.intuit.wasabi.api;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.experiment.Favorites;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.intuit.wasabi.api.APISwaggerResource.EXAMPLE_AUTHORIZATION_HEADER;

/**
 * Tests for {@link FavoritesResource}.
 */
@RunWith(MockitoJUnitRunner.class)
public class FavoritesResourceTest {

    @Mock
    private Favorites favorites;

    @Mock
    private Authorization authorization;

    private HttpHeader httpHeader = new HttpHeader("TestApplication", "600");
    private FavoritesResource favoritesResource;
    private Experiment testExperiment;
    private List<Experiment.ID> expectedFavoriteList;

    @Before
    public void setup() throws Exception {
        favoritesResource = new FavoritesResource(httpHeader, favorites, authorization);
        testExperiment = Experiment.withID(Experiment.ID.newInstance()).build();

        expectedFavoriteList = new ArrayList<>();
        expectedFavoriteList.add(Experiment.ID.newInstance());
        expectedFavoriteList.add(Experiment.ID.newInstance());
        expectedFavoriteList.add(Experiment.ID.newInstance());

        UserInfo.Username user = UserInfo.Username.valueOf("TestUser");
        Mockito.doReturn(user).when(authorization).getUser(EXAMPLE_AUTHORIZATION_HEADER);
        Mockito.doReturn(expectedFavoriteList).when(favorites).addFavorite(user, testExperiment.getID());
        Mockito.doReturn(expectedFavoriteList).when(favorites).getFavorites(user);
        Mockito.doReturn(expectedFavoriteList).when(favorites).deleteFavorite(user, testExperiment.getID());
    }

    @After
    public void verify() throws Exception {
        Mockito.verify(favorites, Mockito.atMost(1)).addFavorite(Mockito.any(), Mockito.any());
        Mockito.verify(favorites, Mockito.atMost(1)).getFavorites(Mockito.any());
        Mockito.verify(favorites, Mockito.atMost(1)).deleteFavorite(Mockito.any(), Mockito.any());
    }

    @Test
    public void testPostFavorite() throws Exception {
        Response response = favoritesResource.postFavorite(EXAMPLE_AUTHORIZATION_HEADER, testExperiment);
        Assert.assertEquals("List was not correctly returned on POST.", expectedFavoriteList, ((Map) response.getEntity()).get("experimentIDs"));
    }

    @Test
    public void testGetFavorites() throws Exception {
        Response response = favoritesResource.getFavorites(EXAMPLE_AUTHORIZATION_HEADER);
        Assert.assertEquals("List was not correctly returned on GET.", expectedFavoriteList, ((Map) response.getEntity()).get("experimentIDs"));
    }

    @Test
    public void testDeleteFavorite() throws Exception {
        Response response = favoritesResource.deleteFavorite(EXAMPLE_AUTHORIZATION_HEADER, testExperiment.getID());
        Assert.assertEquals("List was not correctly returned on GET.", expectedFavoriteList, ((Map) response.getEntity()).get("experimentIDs"));
    }

}
