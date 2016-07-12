package com.intuit.wasabi.experimentobjects.filter;

import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static jdk.nashorn.internal.objects.Global.print;
import static org.junit.Assert.*;

/**
 * This is the test class for {@link ExperimentFilter}
 */
public class ExperimentFilterTest {

    private List<Experiment> experiments = new ArrayList<>();
    private Experiment exp1;
    private Experiment exp2;
    private Experiment exp3;

    @Before
    public void setUpExperimentList(){

        Calendar start = Calendar.getInstance();
        start.set(2016,8,28);
        start.set(Calendar.HOUR_OF_DAY,17);
        start.set(Calendar.MINUTE,30);
        start.set(Calendar.SECOND,0);
        start.set(Calendar.MILLISECOND,0);


        Calendar end = Calendar.getInstance(start.getTimeZone());
        end.setTime(start.getTime());
        end.add(Calendar.MONTH,1);

        exp1 = Experiment.withID(Experiment.ID.newInstance())
                                    .withApplicationName(Application.Name.valueOf("wasabi"))
                                    .withLabel(Experiment.Label.valueOf("wasabi-experiment"))
                                    .withSamplingPercent(.20)
                                    .withCreationTime(start.getTime())
                                    .withEndTime(end.getTime())
                                    .withState(Experiment.State.DRAFT)
                                    .build();

        exp2 = Experiment.withID(Experiment.ID.newInstance())
                                    .withApplicationName(Application.Name.valueOf("mint"))
                                    .withLabel(Experiment.Label.valueOf("new-payment-option"))
                                    .withSamplingPercent(.03)
                                    .withCreationTime(start.getTime())
                                    .withEndTime(end.getTime())
                                    .withState(Experiment.State.RUNNING)
                                    .build();

        exp3 = Experiment.withID(Experiment.ID.newInstance())
                                    .withApplicationName(Application.Name.valueOf("app"))
                                    .withLabel(Experiment.Label.valueOf("label"))
                                    .withSamplingPercent(.31)
                                    .withCreationTime(start.getTime())
                                    .withEndTime(end.getTime())
                                    .withState(Experiment.State.TERMINATED)
                                    .build();

        experiments.add(exp1);
        experiments.add(exp2);
        experiments.add(exp3);

    }

    @Test
    public void testFilteringAll() {
        assertEquals("expected no matches", 0, ExperimentFilter.filter(experiments, "atermthatfitstonothing").size());
        assertEquals("expected to match all", experiments.size(), ExperimentFilter.filter(experiments, "a").size());
    }

    @Test
    public void testFilteringSingle() {
        assertEquals("expected one experiment", 1, ExperimentFilter.filter(experiments, "wasabi").size());
        Experiment exp = experiments.get(0);
        assertEquals(exp, exp1);
    }

    @Test
    public void testFilteringMoreExperimentsByName(){
        // 'ab' is part of wasabi and label
        ExperimentFilter.filter(experiments, "experiment_name=ab");
        assertEquals("expected one experiment", 2, experiments.size());
        assertTrue(experiments.contains(exp1));
        assertTrue(experiments.contains(exp3));
    }


}
