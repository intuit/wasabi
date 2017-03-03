package com.intuit.wasabi.export.rest.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Properties;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created on 4/13/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultRestEndPointConfigurationTest {
    Properties properties;
    DefaultRestEndPointConfiguration defaultRestEndPointConfiguration;

    @Before
    public void setup() {
        properties = new Properties();
        defaultRestEndPointConfiguration = new DefaultRestEndPointConfiguration(properties);
        properties.put("export.rest.scheme", "scheme1");
        properties.put("export.rest.host", "host");
        properties.put("export.rest.path", "path");
        properties.put("export.rest.useProxy", "false");
        properties.put("export.rest.retries", "5");
        properties.put("export.rest.port", "80");
    }

    @Test
    public void testPropertiesGetter() {
        assertThat(defaultRestEndPointConfiguration.getScheme(), is("scheme1"));
        assertThat(defaultRestEndPointConfiguration.getHost(), is("host"));
        assertThat(defaultRestEndPointConfiguration.getPath(), is("path"));
        assertThat(defaultRestEndPointConfiguration.useProxy(), is(false));
        assertThat(defaultRestEndPointConfiguration.getRetries(), is(5));
        assertThat(defaultRestEndPointConfiguration.getPort(), is(80));
        properties.remove("export.rest.port");
        assertThat(defaultRestEndPointConfiguration.getPort(), is(0));

    }
}
