package com.intuit.wasabi.export.rest.impl;

import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.intuit.wasabi.export.rest.Driver;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created on 4/13/16.
 */
public class ExportModuleTest {
    @Before
    @After
    public void setSysProps() {
        System.getProperties().setProperty("export.rest.client.connectionTimeout", "5000");
        System.getProperties().setProperty("export.rest.client.socketTimeout", "8000");
        System.getProperties().setProperty("http.proxy.host", "192.168.1.1");
        System.getProperties().setProperty("http.proxy.port", "8080");
    }

    @Test
    public void testGetDriverConfigurationImplOrFail() throws Exception {
        try {
            Injector injector = Guice.createInjector(new ExportModule());
            injector.getInstance(DefaultDriverConfiguration.class);
        } catch (CreationException ignored) {
            Assert.fail();
        }

    }

    @Test
    public void testGetDefaultRestDriveOrFail() throws Exception {
        try {
            Injector injector = Guice.createInjector(new ExportModule());
            injector.getInstance(Driver.class);
        } catch (CreationException ignored) {
            Assert.fail();
        }
    }

    @Test
    public void testGetNamedInstances() {
        Injector injector = Guice.createInjector(new ExportModule());
        assertThat(injector.getInstance(Key.get(Integer.class, Names.named("export.rest.client.connectionTimeout"))),
                is(5000));
        assertThat(injector.getInstance(Key.get(Integer.class, Names.named("export.rest.client.socketTimeout"))),
                is(8000));
        assertThat(injector.getInstance(Key.get(Integer.class, Names.named("export.http.proxy.port"))),
                is(8080));
        assertThat(injector.getInstance(Key.get(String.class, Names.named("export.http.proxy.host"))),
                is("192.168.1.1"));
    }

}
