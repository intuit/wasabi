/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.intuit.wasabi.experiment.impl;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.eventlog.events.ExperimentChangeEvent;
import com.intuit.wasabi.experiment.Experiments;
import com.intuit.wasabi.experiment.Pages;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.ExperimentPage;
import com.intuit.wasabi.experimentobjects.ExperimentPageList;
import com.intuit.wasabi.experimentobjects.Page;
import com.intuit.wasabi.experimentobjects.PageExperiment;
import com.intuit.wasabi.experimentobjects.exception.ApplicationNotFoundException;
import com.intuit.wasabi.experimentobjects.exception.ExperimentNotFoundException;
import com.intuit.wasabi.experimentobjects.exception.InvalidExperimentStateException;
import com.intuit.wasabi.repository.CassandraRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.PagesRepository;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.intuit.wasabi.experimentobjects.Experiment.State.TERMINATED;

public class PagesImpl implements Pages {

    final Date NOW = new Date();
    private final ExperimentRepository cassandraRepository;
    private final Experiments experiments;
    private final PagesRepository pagesRepository;
    private final EventLog eventLog;

    @Inject
    public PagesImpl(@CassandraRepository ExperimentRepository cassandraRepository, PagesRepository pagesRepository,
                     Experiments experiments, EventLog eventLog) {
        super();
        this.cassandraRepository = cassandraRepository;
        this.experiments = experiments;
        this.pagesRepository = pagesRepository;
        this.eventLog = eventLog;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postPages(Experiment.ID experimentID, ExperimentPageList experimentPageList, UserInfo user) {
        Application.Name applicationName = getApplicationNameForModifyingPages(experimentID);
        pagesRepository.postPages(applicationName, experimentID, experimentPageList);

        Experiment experiment = experiments.getExperiment(experimentID);
        if (Objects.nonNull(experiment)) {
            List<String> pageNames = new ArrayList<>();
            for (ExperimentPage experimentPage : experimentPageList.getPages()) {
                pageNames.add(experimentPage.getName().toString());
            }
            String pageString = StringUtils.join(pageNames, ", ");
            eventLog.postEvent(new ExperimentChangeEvent(user, experiment, "pages", null, pageString));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deletePage(Experiment.ID experimentID, Page.Name pageName, UserInfo user) {
        Application.Name applicationName = getApplicationNameForModifyingPages(experimentID);
        pagesRepository.deletePage(applicationName, experimentID, pageName);

        Experiment experiment = experiments.getExperiment(experimentID);
        if (Objects.nonNull(experiment)) {
            eventLog.postEvent(new ExperimentChangeEvent(user, experiment, "pages", pageName.toString(), null));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExperimentPageList getExperimentPages(Experiment.ID experimentID) {
        Experiment experiment = experiments.getExperiment(experimentID);

        // Throw an exception if the experiment is not found
        if (Objects.isNull(experiment)) {
            throw new ExperimentNotFoundException(experimentID);
        }
        return pagesRepository.getExperimentPages(experimentID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExperimentList getPageExperiments(Application.Name applicationName, Page.Name pageName) {

        ExperimentList result = new ExperimentList();

        if (Objects.isNull(applicationName) || applicationName.toString().isEmpty() || Objects.isNull(pageName) || pageName.toString().isEmpty()) {
            return result;
        } else {

            List<PageExperiment> pageExperiments = pagesRepository.getExperiments(applicationName, pageName);
            List<Experiment.ID> expIDList = new ArrayList<>();
            if (!pageExperiments.isEmpty()) {
                for (PageExperiment pageExperiment : pageExperiments) {
                    Experiment.ID experimentID = pageExperiment.getId();
                    expIDList.add(experimentID);
                }
            }
            result = cassandraRepository.getExperiments(expIDList);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void erasePageData(Application.Name applicationName, Experiment.ID experimentID, UserInfo user) {
        pagesRepository.erasePageData(applicationName, experimentID);

        Experiment experiment = experiments.getExperiment(experimentID);
        if (Objects.nonNull(experiment)) {
            eventLog.postEvent(new ExperimentChangeEvent(user, experiment, "pages", "all pages", null));
        }
    }

    private Application.Name getApplicationNameForModifyingPages(Experiment.ID experimentID) {

        Experiment experiment = experiments.getExperiment(experimentID);

        // Throw an exception if the experiment is not found
        if (Objects.isNull(experiment)) {
            throw new ExperimentNotFoundException(experimentID);
        }
        Application.Name applicationName = experiment.getApplicationName();

        // Throw an exception if the experiment is in a TERMINATED state
        if (experiment.getState() == TERMINATED) {
            throw new InvalidExperimentStateException("Experiment must be in DRAFT, RUNNING or PAUSED states\"" +
                    experimentID);
        }
        // Throw an exception if the experiment's end time has passed
        if (experiment.getEndTime().before(NOW)) {
            throw new IllegalArgumentException("Cannot modify pages of the experiment \"" + experimentID +
                    "\" that has passed its end time");
        }

        return applicationName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Page> getPageList(Application.Name applicationName) {
        // Throw an exception if application name is invalid
        if (Objects.isNull(applicationName) || StringUtils.isBlank(applicationName.toString())) {
            throw new ApplicationNotFoundException("The Application name can not be null or empty");
        }
        return pagesRepository.getPageList(applicationName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PageExperiment> getExperiments(Application.Name applicationName, Page.Name pageName) {
        // Throw an exception if application name is invalid
        if (Objects.isNull(applicationName) || StringUtils.isBlank(applicationName.toString())) {
            throw new ApplicationNotFoundException("The Application name can not be null or empty");
        }

        return pagesRepository.getExperiments(applicationName, pageName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Page.Name, List<PageExperiment>> getPageAndExperimentList(Application.Name applicationName) {
        // Throw an exception if application name is invalid
        if (Objects.isNull(applicationName) || StringUtils.isBlank(applicationName.toString())) {
            throw new ApplicationNotFoundException("The Application name can not be null or empty");
        }
        return pagesRepository.getPageExperimentList(applicationName);
    }
}
