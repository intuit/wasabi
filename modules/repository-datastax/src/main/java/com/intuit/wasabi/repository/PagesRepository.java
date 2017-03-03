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

import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentPageList;
import com.intuit.wasabi.experimentobjects.Page;
import com.intuit.wasabi.experimentobjects.PageExperiment;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Interface for supporting pages data
 *
 * @see Experiment
 * @see Application
 * @see Page
 * @see ExperimentPageList
 */
public interface PagesRepository {

    /**
     * Add a list of pages to an experiment
     *
     * @param applicationName    name of the application
     * @param experimentID       id of the experiment
     * @param experimentPageList pagelist of the experiment
     * @throws RepositoryException exception
     */
    void postPages(Application.Name applicationName, Experiment.ID experimentID, ExperimentPageList experimentPageList)
            throws RepositoryException;

    /**
     * Delete a page from an experiment
     *
     * @param applicationName name of the application
     * @param experimentID    id of the experiment
     * @param pageName        page name
     */
    void deletePage(Application.Name applicationName, Experiment.ID experimentID, Page.Name pageName);

    /**
     * Get the names of all pages associated with an application
     *
     * @param applicationName name of the application
     * @return list of pages
     */
    List<Page> getPageList(Application.Name applicationName);

    /**
     * Get the names of all pages associated with an application with their associated experiments
     */
    Map<Page.Name, List<PageExperiment>> getPageExperimentList(Application.Name applicationName);

    /**
     * Get the page information(name and allowNewAssignment) for the associated pages for an experiment
     *
     * @param experimentID id of the experiment
     * @return list of experiment pages
     * <p>
     * TODO: return list of experiment pages
     */
    ExperimentPageList getExperimentPages(Experiment.ID experimentID);

    /**
     * Get the experiment information(id and allowNewAssignment) for the associated experiments for a page
     *
     * @param applicationName name of the application
     * @param pageName        page name
     * @return list of PageExperiment
     */
    List<PageExperiment> getExperiments(Application.Name applicationName, Page.Name pageName);

    /**
     * Erase the page related data associated to an experiment
     *
     * @param applicationName name of the application
     * @param experimentID    page name
     */
    void erasePageData(Application.Name applicationName, Experiment.ID experimentID);

    /**
     * Get the experiment information (id and allowNewAssignment - No labels) for the associated experiments for a page
     *
     * @param applicationName name of the application
     * @param pageName        page name
     * @return list of PageExperiment (id and allowNewAssignment - No labels)
     */
    List<PageExperiment> getExperimentsWithoutLabels(Application.Name applicationName, Page.Name pageName);

    /**
     * In batch, get the experiments (List of PageExperiments) for given application and page pairs.
     *
     * @param appAndPagePairs name of the application
     * @return list of PageExperiment
     */
    Map<Pair<Application.Name, Page.Name>, List<PageExperiment>> getExperimentsWithoutLabels(Collection<Pair<Application.Name, Page.Name>> appAndPagePairs);

}
