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

import com.intuit.wasabi.api.pagination.PaginationHelper;
import com.intuit.wasabi.api.pagination.comparators.impl.AuditLogEntryComparator;
import com.intuit.wasabi.api.pagination.filters.impl.AuditLogEntryFilter;
import com.intuit.wasabi.auditlog.AuditLog;
import com.intuit.wasabi.auditlog.impl.AuditLogImpl;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.auditlogobjects.AuditLogEntryFactory;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.eventlog.events.SimpleEvent;
import com.intuit.wasabi.experimentobjects.Application;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AuditLogResource}
 */
public class AuditLogResourceTest {


    @Test
    public void getCompleteLogs() throws Exception {
        AuditLog al = mock(AuditLogImpl.class);
        Authorization auth = mock(Authorization.class);
        List<AuditLogEntry> list = new ArrayList<>();
        AuditLogResource lr = new AuditLogResource(al, auth,
                new HttpHeader("MyApp-???", "600"),
                new PaginationHelper<>(new AuditLogEntryFilter(), new AuditLogEntryComparator()));

        Mockito.when(al.getAuditLogs()).thenReturn(list);
        Response r = lr.getLogsForAllApplications(
                "", 1, 10, "", "", null);
        assertThat("{logEntries=[], totalEntries=0}", is(r.getEntity().toString()));

        for (int i = 0; i < 5; i++) {
            list.add(AuditLogEntryFactory.createFromEvent(new SimpleEvent("Event")));
        }
        Mockito.when(al.getAuditLogs()).thenReturn(list);
        r = lr.getLogsForAllApplications(
                "", 1, 10, "", "", null);
        Assert.assertTrue(r.getEntity().toString().contains("totalEntries=5"));

        for (int i = 0; i < 6; i++) {
            list.add(AuditLogEntryFactory.createFromEvent(new SimpleEvent("Event")));
        }
        Mockito.when(al.getAuditLogs()).thenReturn(list);
        r = lr.getLogsForAllApplications(
                "", 1, 10, "", "", null);
        Assert.assertTrue(r.getEntity().toString().contains("totalEntries=11"));
    }


    @Test
    @SuppressWarnings("unchecked")
    public void getApplicationLogs() throws Exception {
        AuditLog al = mock(AuditLogImpl.class);
        Application.Name appName = Application.Name.valueOf("TestApp");
        Authorization auth = mock(Authorization.class);
        List<AuditLogEntry> list = new ArrayList<>();
        AuditLogResource lr = new AuditLogResource(
                al, auth, new HttpHeader("MyApp-???", "600"),
                new PaginationHelper<>(new AuditLogEntryFilter(), new AuditLogEntryComparator()));

        Mockito.when(al.getAuditLogs(appName)).thenReturn(list);
        Response r = lr.getLogsForApplication(
                "", appName, 1, 10, "", "", null);
        assertThat(((List) ((Map<String, Object>) r.getEntity()).get("logEntries")).size(), is(0));
        assertThat(((Map<String, Object>) r.getEntity()).get("totalEntries"), is(0));

        for (int i = 0; i < 5; i++) {
            list.add(AuditLogEntryFactory.createFromEvent(new SimpleEvent("Event")));
        }
        Mockito.when(al.getAuditLogs(appName)).thenReturn(list);
        r = lr.getLogsForApplication(
                "", appName, 1, 10, "", "", null);
        assertThat(((List) ((Map<String, Object>) r.getEntity()).get("logEntries")).size(), is(5));
        assertThat(((Map<String, Object>) r.getEntity()).get("totalEntries"), is(5));

        for (int i = 0; i < 6; i++) {
            list.add(AuditLogEntryFactory.createFromEvent(new SimpleEvent("Event")));
        }
        Mockito.when(al.getAuditLogs(appName)).thenReturn(list);
        r = lr.getLogsForApplication(
                "", appName, 1, 10, "", "", null);
        assertThat(((List) ((Map<String, Object>) r.getEntity()).get("logEntries")).size(), is(10));
        assertThat(((Map<String, Object>) r.getEntity()).get("totalEntries"), is(11));
    }

}
