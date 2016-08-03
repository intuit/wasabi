/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.api;

import com.intuit.wasabi.api.pagination.comparators.impl.AuditLogEntryComparator;
import com.intuit.wasabi.api.pagination.filters.impl.AuditLogEntryFilter;
import com.intuit.wasabi.auditlog.AuditLog;
import com.intuit.wasabi.auditlog.impl.AuditLogImpl;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.auditlogobjects.AuditLogEntryFactory;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.eventlog.events.SimpleEvent;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.api.pagination.PaginationHelper;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
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
        AuditLogResource lr = new AuditLogResource(al, auth, new HttpHeader("MyApp-???"), new PaginationHelper<>(new AuditLogEntryFilter(), new AuditLogEntryComparator()));

        Mockito.when(al.getAuditLogs()).thenReturn(list);
        Response r = lr.getCompleteLogs("", 1, 10, "", "", null);
        assertEquals("{logEntries=[], totalEntries=0}", r.getEntity().toString());

        for (int i = 0; i < 5; i++) {
            list.add(AuditLogEntryFactory.createFromEvent(new SimpleEvent("Event")));
        }
        Mockito.when(al.getAuditLogs()).thenReturn(list);
        r = lr.getCompleteLogs("", 1, 10, "", "", null);
        Assert.assertTrue(r.getEntity().toString().contains("totalEntries=5"));

        for (int i = 0; i < 6; i++) {
            list.add(AuditLogEntryFactory.createFromEvent(new SimpleEvent("Event")));
        }
        Mockito.when(al.getAuditLogs()).thenReturn(list);
        r = lr.getCompleteLogs("", 1, 10, "", "", null);
        Assert.assertTrue(r.getEntity().toString().contains("totalEntries=11"));
    }


    @Test
    @SuppressWarnings("unchecked")
    public void getApplicationLogs() throws Exception {
        AuditLog al = mock(AuditLogImpl.class);
        Application.Name appName = Application.Name.valueOf("TestApp");
        Authorization auth = mock(Authorization.class);
        List<AuditLogEntry> list = new ArrayList<>();
        AuditLogResource lr = new AuditLogResource(al, auth, new HttpHeader("MyApp-???"), new PaginationHelper<>(new AuditLogEntryFilter(), new AuditLogEntryComparator()));

        Mockito.when(al.getAuditLogs(appName)).thenReturn(list);
        Response r = lr.getLogs("", appName, 1, 10, "", "", null);
        assertEquals(0, ((List) ((Map<String, Object>) r.getEntity()).get("logEntries")).size());
        assertEquals(0, ((Map<String, Object>) r.getEntity()).get("totalEntries"));

        for (int i = 0; i < 5; i++) {
            list.add(AuditLogEntryFactory.createFromEvent(new SimpleEvent("Event")));
        }
        Mockito.when(al.getAuditLogs(appName)).thenReturn(list);
        r = lr.getLogs("", appName, 1, 10, "", "", null);
        assertEquals(5, ((List) ((Map<String, Object>) r.getEntity()).get("logEntries")).size());
        assertEquals(5, ((Map<String, Object>) r.getEntity()).get("totalEntries"));

        for (int i = 0; i < 6; i++) {
            list.add(AuditLogEntryFactory.createFromEvent(new SimpleEvent("Event")));
        }
        Mockito.when(al.getAuditLogs(appName)).thenReturn(list);
        r = lr.getLogs("", appName, 1, 10, "", "", null);
        assertEquals(10, ((List) ((Map<String, Object>) r.getEntity()).get("logEntries")).size());
        assertEquals(11, ((Map<String, Object>) r.getEntity()).get("totalEntries"));
    }

}
