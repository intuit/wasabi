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

import com.intuit.wasabi.analytics.Analytics;
import com.intuit.wasabi.analytics.ExperimentDetails;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.analyticsobjects.counts.ExperimentCounts;
import com.intuit.wasabi.analyticsobjects.counts.ExperimentCumulativeCounts;
import com.intuit.wasabi.analyticsobjects.statistics.ExperimentCumulativeStatistics;
import com.intuit.wasabi.analyticsobjects.statistics.ExperimentStatistics;
import com.intuit.wasabi.analyticsobjects.wrapper.ExperimentDetail;
import com.intuit.wasabi.api.pagination.PaginationHelper;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.experiment.Favorites;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnalyticsResourceTest {

    @Mock
    private Analytics analytics;
    @Mock
    private AuthorizedExperimentGetter authorizedExperimentGetter;
    @Mock
    private HttpHeader httpHeader;
    @Mock
    private ExperimentCounts experimentCounts;
    @Mock
    private Experiment experiment;
    @Mock
    private Response.ResponseBuilder responseBuilder;
    @Mock
    private ExperimentCumulativeCounts experimentCumulativeCounts;
    @Mock
    private ExperimentStatistics experimentStatistics;
    @Mock
    private ExperimentCumulativeStatistics experimentCumulativeStatistics;
    @Mock
    private AssignmentCounts assignmentCounts;
    @Mock
    private Response response;
    @Mock
    private Experiment.ID experimentID;
    @Mock
    private Context context;
    @Mock
    private Parameters parameters;
    @Mock
    private Experiment.Label experimentLabel;
    @Mock
    private Application.Name applicationName;
    private AnalyticsResource analyticsResource;

    @Mock
    private PaginationHelper<ExperimentDetail> experimentDetailPaginationHelper;
    @Mock
    private ExperimentDetails experimentDetails;
    @Mock
    private Favorites favorites;
    @Mock
    private Authorization authorization;

    @Before
    public void setup() {
        analyticsResource = new AnalyticsResource(analytics, authorizedExperimentGetter, httpHeader,
                experimentDetailPaginationHelper, experimentDetails, favorites, authorization);
    }

    @Test
    public void getExperimentCountsParameters() throws Exception {
        when(analytics.getExperimentRollup(experimentID, parameters)).thenReturn(experimentCounts);
        when(httpHeader.headers()).thenReturn(responseBuilder);
        whenHttpHeader(experimentCounts);

        analyticsResource.getExperimentCountsParameters(experimentID, parameters, "foo");

        verifyAuthorizedExperimentGetter();
        verify(parameters).parse();
        verify(analytics).getExperimentRollup(experimentID, parameters);
        verifyHttpHeader(experimentCounts);
    }

    private void whenHttpHeader(final Object entity) {
        when(httpHeader.headers()).thenReturn(responseBuilder);
        when(responseBuilder.entity(entity)).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);
    }

    private void verifyAuthorizedExperimentGetter() {
        verify(authorizedExperimentGetter).getAuthorizedExperimentById("foo", experimentID);
    }

    private void verifyHttpHeader(final Object entity) {
        verify(httpHeader).headers();
        verify(responseBuilder).entity(entity);
        verify(responseBuilder).build();
    }

    @Test
    public void getExperimentCounts() throws Exception {
        when(analytics.getExperimentRollup(eq(experimentID), any(Parameters.class)))
                .thenReturn(experimentCounts);
        whenHttpHeader(experimentCounts);

        analyticsResource.getExperimentCounts(experimentID, context, "foo");

        verifyAuthorizedExperimentGetter();
        verify(analytics).getExperimentRollup(eq(experimentID), any(Parameters.class));
        verifyHttpHeader(experimentCounts);
    }

    @Test
    public void getExperimentCountsDailiesParameters() throws Exception {
        when(analytics.getExperimentRollupDailies(experimentID, parameters))
                .thenReturn(experimentCumulativeCounts);
        whenHttpHeader(experimentCumulativeCounts);

        analyticsResource.getExperimentCountsDailiesParameters(experimentID, parameters, "foo");

        verifyAuthorizedExperimentGetter();
        verify(parameters).parse();
        verifyHttpHeader(experimentCumulativeCounts);
    }

    @Test
    public void getExperimentCountsDailies() throws Exception {
        when(analytics.getExperimentRollupDailies(eq(experimentID), any(Parameters.class)))
                .thenReturn(experimentCumulativeCounts);
        whenHttpHeader(experimentCumulativeCounts);

        analyticsResource.getExperimentCountsDailies(experimentID, context, "foo");

        verifyAuthorizedExperimentGetterWithExperimentID();
        verify(analytics).getExperimentRollupDailies(eq(experimentID), any(Parameters.class));
        verifyHttpHeader(experimentCumulativeCounts);
    }

    private void verifyAuthorizedExperimentGetterWithExperimentID() {
        verify(authorizedExperimentGetter).getAuthorizedExperimentById("foo", experimentID);
    }

    @Test
    public void getExperimentStatisticsParameters() throws Exception {
        when(analytics.getExperimentStatistics(experimentID, parameters)).thenReturn(experimentStatistics);
        whenHttpHeader(experimentStatistics);

        analyticsResource.getExperimentStatisticsParameters(experimentID, parameters, "foo");

        verifyAuthorizedExperimentGetterWithExperimentID();
        verify(parameters).parse();
        verifyHttpHeader(experimentStatistics);
    }

    @Test
    public void getExperimentStatistics() throws Exception {
        when(analytics.getExperimentStatistics(eq(experimentID), any(Parameters.class)))
                .thenReturn(experimentStatistics);
        whenHttpHeader(experimentStatistics);

        analyticsResource.getExperimentStatistics(experimentID, context, "foo");

        verifyAuthorizedExperimentGetterWithExperimentID();
        verifyHttpHeader(experimentStatistics);
    }

    @Test
    public void getExperimentStatisticsDailiesParameters() throws Exception {
        when(analytics.getExperimentStatisticsDailies(experimentID, parameters))
                .thenReturn(experimentCumulativeStatistics);
        whenHttpHeader(experimentCumulativeStatistics);

        analyticsResource.getExperimentStatisticsDailiesParameters(experimentID, parameters, "foo");

        verifyAuthorizedExperimentGetterWithExperimentID();
        verify(parameters).parse();
        verifyHttpHeader(experimentCumulativeStatistics);
    }

    @Test
    public void getExperimentStatisticsDailies() throws Exception {
        when(analytics.getExperimentStatisticsDailies(eq(experimentID), any(Parameters.class)))
                .thenReturn(experimentCumulativeStatistics);
        whenHttpHeader(experimentCumulativeStatistics);

        analyticsResource.getExperimentStatisticsDailies(experimentID, context, "foo");

        verifyAuthorizedExperimentGetterWithExperimentID();
        verifyHttpHeader(experimentCumulativeStatistics);
    }

    @Test
    public void getAssignmentCounts() throws Exception {
        when(analytics.getAssignmentCounts(experimentID, context)).thenReturn(assignmentCounts);
        whenHttpHeader(assignmentCounts);

        analyticsResource.getAssignmentCounts(experimentID, context, "foo");

        verifyAuthorizedExperimentGetterWithExperimentID();
        verify(analytics).getAssignmentCounts(experimentID, context);
        verifyHttpHeader(assignmentCounts);
    }

    @Test
    public void getAssignmentCountsByApp() throws Exception {
        when(authorizedExperimentGetter.getAuthorizedExperimentByName("foo", applicationName,
                experimentLabel))
                .thenReturn(experiment);
        when(experiment.getID()).thenReturn(experimentID);
        when(analytics.getAssignmentCounts(experimentID, context)).thenReturn(assignmentCounts);
        whenHttpHeader(assignmentCounts);

        analyticsResource.getAssignmentCountsByApp(applicationName, experimentLabel, context, "foo");

        verify(authorizedExperimentGetter).getAuthorizedExperimentByName("foo", applicationName,
                experimentLabel);
        verifyHttpHeader(assignmentCounts);
    }
}
