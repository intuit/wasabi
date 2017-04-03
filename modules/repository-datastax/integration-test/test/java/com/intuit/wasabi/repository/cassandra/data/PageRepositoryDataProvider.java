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
package com.intuit.wasabi.repository.cassandra.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test
public class PageRepositoryDataProvider {
    private final Logger logger = LoggerFactory.getLogger(PageRepositoryDataProvider.class);

    //void postPages(Application.Name applicationName, Experiment.ID experimentID, ExperimentPageList experimentPageList)
    @DataProvider(name = "postPagesDataProvider")
    public static Object[][] postPagesDataProvider() {
        ExperimentPageList experimentPageList1 = new ExperimentPageList();
        experimentPageList1.addPage(ExperimentPage.withAttributes(Page.Name.valueOf("page1"), true).build());
        experimentPageList1.addPage(ExperimentPage.withAttributes(Page.Name.valueOf("page2"), false).build());

        ExperimentPageList experimentPageList2 = new ExperimentPageList();
        experimentPageList2.addPage(ExperimentPage.withAttributes(Page.Name.valueOf("page1"), false).build());

        ExperimentPageList experimentPageList3 = new ExperimentPageList();
        experimentPageList3.addPage(ExperimentPage.withAttributes(Page.Name.valueOf("page2"), true).build());
        experimentPageList3.addPage(ExperimentPage.withAttributes(Page.Name.valueOf("page3"), false).build());

        ExperimentPageList experimentPageList4 = new ExperimentPageList();
        experimentPageList4.addPage(ExperimentPage.withAttributes(Page.Name.valueOf("page4"), true).build());

        return new Object[][]{
                new Object[]{
                        Application.Name.valueOf("App1"),
                        Experiment.ID.valueOf("046b6c7f-0b8a-43b9-b35d-6489e6daee91"),
                        Experiment.Label.valueOf("label1"),
                        experimentPageList1
                },
                new Object[]{
                        Application.Name.valueOf("App2"),
                        Experiment.ID.valueOf("000b6c7f-0b8a-43b9-b35d-6489e6daee91"),
                        Experiment.Label.valueOf("label2"),
                        experimentPageList2
                },
                new Object[]{
                        Application.Name.valueOf("App1"),
                        Experiment.ID.valueOf("222b6c7f-0b8a-43b9-b35d-6489e6daee91"),
                        Experiment.Label.valueOf("label3"),
                        experimentPageList3
                },
                new Object[]{
                        Application.Name.valueOf("App3"),
                        Experiment.ID.valueOf("111b6c7f-0b8a-43b9-b35d-6489e6daee91"),
                        Experiment.Label.valueOf("label4"),
                        experimentPageList4
                }
        };
    }
}