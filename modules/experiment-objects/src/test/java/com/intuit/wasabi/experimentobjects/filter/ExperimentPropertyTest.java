package com.intuit.wasabi.experimentobjects.filter;

import org.junit.Assert;
import org.junit.Test;

/**
 * This is the test class for {@link ExperimentProperty}
 */
public class ExperimentPropertyTest {

    @Test
    public void testProperties(){
        ExperimentProperty prop = ExperimentProperty.forKey("end_date");
        Assert.assertEquals(prop, ExperimentProperty.END_DATE);

        prop = ExperimentProperty.forKey("unknown");
        Assert.assertNull(prop);
    }
}
