package com.intuit.wasabi.repository.cassandra.data;

import com.intuit.wasabi.experimentobjects.ExperimentPage;
import com.intuit.wasabi.experimentobjects.ExperimentPageList;
import com.intuit.wasabi.experimentobjects.Page;
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
        experimentPageList1.addPage(ExperimentPage.withAttributes(Page.Name.valueOf("page2"), true).build());

        ExperimentPageList experimentPageList2 = new ExperimentPageList();
        experimentPageList2.addPage(ExperimentPage.withAttributes(Page.Name.valueOf("page1"), true).build());

        ExperimentPageList experimentPageList3 = new ExperimentPageList();
        experimentPageList3.addPage(ExperimentPage.withAttributes(Page.Name.valueOf("page2"), true).build());
        experimentPageList3.addPage(ExperimentPage.withAttributes(Page.Name.valueOf("page3"), true).build());

        ExperimentPageList experimentPageList4 = new ExperimentPageList();
        experimentPageList4.addPage(ExperimentPage.withAttributes(Page.Name.valueOf("page4"), true).build());

        return new Object[][]{
                new Object[]{
                        "App1",
                        "046b6c7f-0b8a-43b9-b35d-6489e6daee91",
                        experimentPageList1
                },
                new Object[]{
                        "App2",
                        "000b6c7f-0b8a-43b9-b35d-6489e6daee91",
                        experimentPageList2
                },
                new Object[]{
                        "App1",
                        "222b6c7f-0b8a-43b9-b35d-6489e6daee91",
                        experimentPageList3
                },
                new Object[]{
                        "App3",
                        "111b6c7f-0b8a-43b9-b35d-6489e6daee91",
                        experimentPageList4
                }
        };
    }
}