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
package com.intuit.wasabi.experiment;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.experimentobjects.Experiment;

import java.util.List;

/**
 * Favorites are realized as lists of Experiment IDs.
 */
public interface Favorites {

    /**
     * Returns a list of experiment IDs which are favorites for the specified user.
     *
     * @param username the user to get the favorites for
     * @return the list offavorite IDs
     */
    List<Experiment.ID> getFavorites(UserInfo.Username username);

    /**
     * Adds a new favorite for a user.
     *
     * @param username     the user
     * @param experimentID the new favorite experiment ID
     * @return the new list of favorite IDs
     */
    List<Experiment.ID> addFavorite(UserInfo.Username username, Experiment.ID experimentID);

    /**
     * Deletes a favorite for a user.
     *
     * @param username     the user
     * @param experimentID the favorite to delete
     * @return the new list of favorite IDs
     */
    List<Experiment.ID> deleteFavorite(UserInfo.Username username, Experiment.ID experimentID);

}
