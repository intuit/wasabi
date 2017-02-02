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
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.ExperimentPageList;
import com.intuit.wasabi.experimentobjects.Page;
import com.intuit.wasabi.experimentobjects.PageExperiment;

import java.util.List;
import java.util.Map;

/**
 * Interface to perform CRUD operations for pages of experiments.
 */
public interface Pages {

    /**
     * Generic Search api: Currently for just pages - Get the experiment information for the chosen pages for an experiment
     *
     * @param applicationName Name of application
     * @param pageName        Name of page
     * @return The experiment information for the chosen pages for an experiment
     */
    ExperimentList getPageExperiments(Application.Name applicationName, Page.Name pageName);

    /**
     * Add a list of pages to an experiment
     *
     * @param experimentID       ID of an experiment
     * @param experimentPageList List of experiment pages
     * @param user               the user who added the page(s)
     */
    void postPages(Experiment.ID experimentID, ExperimentPageList experimentPageList, UserInfo user);

    /**
     * Delete the page from an experiment
     *
     * @param experimentID ID of an experiment
     * @param pageName     Name of a page
     * @param user         the user who triggered the page deletion
     */
    void deletePage(Experiment.ID experimentID, Page.Name pageName, UserInfo user);

    /**
     * Get the page information(name and allowNewAssignment) for the associated pages for an experiment
     *
     * @param experimentID ID of an experiment
     * @return The page information(name and allowNewAssignment) for the associated pages for an experiment
     */
    ExperimentPageList getExperimentPages(Experiment.ID experimentID);

    /**
     * Erase the page related data associated to an experiment
     *
     * @param applicationName Name of application
     * @param experimentID    ID of an experiment
     * @param user            the user who triggered
     */
    void erasePageData(Application.Name applicationName, Experiment.ID experimentID, UserInfo user);

    /**
     * @param applicationName Name of application
     * @return The set of pages associated with the requested application.
     */
    List<Page> getPageList(Application.Name applicationName);

    /**
     * @param applicationName Name of application
     * @param pageName        Name of page
     * @return A list of experiment information(id and allowNewAssignment) associated with the requested application and page name.
     */
    List<PageExperiment> getExperiments(Application.Name applicationName, Page.Name pageName);

    /**
     * @param applicationName Name of application
     * @return The set of pages and its associated experiments for the requested application.
     */
    Map<Page.Name, List<PageExperiment>> getPageAndExperimentList(Application.Name applicationName);

    /**
     * @param applicationName
     * @param pageName
     * @return PageExperiment list having only ExperimentIDs and isAssign flag (No labels)
     */
    List<PageExperiment> getExperimentsWithoutLabels(Application.Name applicationName, Page.Name pageName);


}
