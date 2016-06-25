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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LogsResource}
 */
public class LogsResourceTest {

    @Test
    public void fromIndex() throws Exception {
        LogsResource lr = new LogsResource(mock(AuditLog.class), mock(Authorization.class), new HttpHeader("jaba-???"));

        // invalid: just return 0
        // parameters
        assertEquals(0, lr.fromIndex(0, 1, 2));
        assertEquals(0, lr.fromIndex(1, 0, 2));
        assertEquals(0, lr.fromIndex(-1, 1, 2));
        assertEquals(0, lr.fromIndex(1, -1, 2));
        assertEquals(0, lr.fromIndex(1, 1, -1));
        // impossible pages
        assertEquals(0, lr.fromIndex(20, 10, 5));
        assertEquals(0, lr.fromIndex(2, 10, 10));
        assertEquals(0, lr.fromIndex(2, 100, 100));

        // valid
        assertEquals(0, lr.fromIndex(1, 10, 2));
        assertEquals(10, lr.fromIndex(2, 10, 11));
        assertEquals(264, lr.fromIndex(23, 12, 1230));
    }

    @Test
    public void toIndex() throws Exception {
        LogsResource lr = new LogsResource(mock(AuditLog.class), mock(Authorization.class), new HttpHeader("jaba-???"));

        // invalid: just return 0
        // parameters
        assertEquals(0, lr.toIndex(0, 1, 2));
        assertEquals(0, lr.toIndex(1, 0, 2));
        assertEquals(0, lr.toIndex(-1, 1, 2));
        assertEquals(0, lr.toIndex(1, -1, 2));
        assertEquals(0, lr.toIndex(1, 1, -1));
        // impossible pages
        assertEquals(0, lr.toIndex(20, 10, 5));
        assertEquals(0, lr.toIndex(2, 10, 10));
        assertEquals(0, lr.toIndex(2, 100, 100));

        // valid
        assertEquals(2, lr.toIndex(1, 10, 2));
        assertEquals(11, lr.toIndex(2, 10, 11));
        assertEquals(276, lr.toIndex(23, 12, 1230));
    }

    @Test
    public void numberOfPages() throws Exception {
        LogsResource lr = new LogsResource(mock(AuditLog.class), mock(Authorization.class), new HttpHeader("jaba-???"));

        // invalid: just return 1
        assertEquals(1, lr.numberOfPages(5, 10));
        assertEquals(1, lr.numberOfPages(-3, 32));
        assertEquals(1, lr.numberOfPages(0, 0));
        assertEquals(1, lr.numberOfPages(23, -1));

        // valid
        assertEquals(1, lr.numberOfPages(10, 10));
        assertEquals(12, lr.numberOfPages(119, 10));
        assertEquals(12, lr.numberOfPages(120, 10));
        assertEquals(13, lr.numberOfPages(121, 10));
        assertEquals(189, lr.numberOfPages(4325, 23));
    }

    @Test
    public void getCompleteLogs() throws Exception {
        AuditLog al = mock(AuditLogImpl.class);
        Authorization auth = mock(Authorization.class);
        List<AuditLogEntry> list = new ArrayList<>();
        LogsResource lr = new LogsResource(al, auth, new HttpHeader("jaba-???"));

        Mockito.when(al.getAuditLogs("", "")).thenReturn(list);
        Response r = lr.getCompleteLogs("", 1, 10, "", "", null);
        assertEquals("{logEntries=[], totalEntries=0}", r.getEntity().toString());

        for (int i = 0; i < 5; i++) {
            list.add(AuditLogEntryFactory.createFromEvent(new SimpleEvent("Event")));
        }
        Mockito.when(al.getAuditLogs("", "")).thenReturn(list);
        r = lr.getCompleteLogs("", 1, 10, "", "", null);
        Assert.assertTrue(r.getEntity().toString().contains("totalEntries=5"));

        for (int i = 0; i < 6; i++) {
            list.add(AuditLogEntryFactory.createFromEvent(new SimpleEvent("Event")));
        }
        Mockito.when(al.getAuditLogs("", "")).thenReturn(list);
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
        LogsResource lr = new LogsResource(al, auth, new HttpHeader("jaba-???"));

        Mockito.when(al.getAuditLogs(appName, "", "")).thenReturn(list);
        Response r = lr.getLogs("", appName, 1, 10, "", "", null);
        assertEquals(0, ((List) ((Map<String, Object>) r.getEntity()).get("logEntries")).size());
        assertEquals(0, ((Map<String, Object>) r.getEntity()).get("totalEntries"));

        for (int i = 0; i < 5; i++) {
            list.add(AuditLogEntryFactory.createFromEvent(new SimpleEvent("Event")));
        }
        Mockito.when(al.getAuditLogs(appName, "", "")).thenReturn(list);
        r = lr.getLogs("", appName, 1, 10, "", "", null);
        assertEquals(5, ((List) ((Map<String, Object>) r.getEntity()).get("logEntries")).size());
        assertEquals(5, ((Map<String, Object>) r.getEntity()).get("totalEntries"));

        for (int i = 0; i < 6; i++) {
            list.add(AuditLogEntryFactory.createFromEvent(new SimpleEvent("Event")));
        }
        Mockito.when(al.getAuditLogs(appName, "", "")).thenReturn(list);
        r = lr.getLogs("", appName, 1, 10, "", "", null);
        assertEquals(10, ((List) ((Map<String, Object>) r.getEntity()).get("logEntries")).size());
        assertEquals(11, ((Map<String, Object>) r.getEntity()).get("totalEntries"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void prepareLogListResponse() throws Exception {
        LogsResource lr = new LogsResource(mock(AuditLog.class), mock(Authorization.class), new HttpHeader("jaba-???"));
        List<AuditLogEntry> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            list.add(AuditLogEntryFactory.createFromEvent(new SimpleEvent("Event")));
        }
        Response respMap = lr.prepareLogListResponse(list, 2, 2, "", "");
        assertEquals(1, ((List) ((Map<String, Object>) respMap.getEntity()).get("logEntries")).size());
        assertEquals(3, ((Map<String, Object>) respMap.getEntity()).get("totalEntries"));
    }

    @Test
    public void linkHeaderValue() throws Exception {
        LogsResource lr = new LogsResource(mock(AuditLog.class), mock(Authorization.class), new HttpHeader("jaba-???"));
        String lh = lr.linkHeaderValue("/test", 100, 3, 5, 1, new HashMap<>());

        assertEquals("</api/v1/logs/test?per_page=5&page=1>; rel=\"First\", </api/v1/logs/test?per_page=5&page=20>; rel=\"Last\", </api/v1/logs/test?per_page=5&page=2>; rel=\"Previous\", </api/v1/logs/test?per_page=5&page=2>; rel=\"Page 2\", </api/v1/logs/test?per_page=5&page=4>; rel=\"Next\", </api/v1/logs/test?per_page=5&page=4>; rel=\"Page 4\"", lh);
    }

    @Test
    public void prepareDateFilter() throws Exception {
        LogsResource lr = new LogsResource(mock(AuditLog.class), mock(Authorization.class), new HttpHeader("jaba-???"));

        assertEquals("", lr.prepareDateFilter("", ""));
        assertEquals("filterMask", lr.prepareDateFilter("filterMask", ""));
        assertEquals("filterMask,time", lr.prepareDateFilter("filterMask,time", ""));
        assertEquals("filterMask,time={+0100}", lr.prepareDateFilter("filterMask,time", "+0100"));
        assertEquals("filterMask,time={+0100}", lr.prepareDateFilter("filterMask", "+0100"));
        assertEquals("filterMask,time={+0100}", lr.prepareDateFilter("filterMask,time=", "+0100"));
        assertEquals("filterMask,time={+0100}Sep 22", lr.prepareDateFilter("filterMask,time=Sep 22", "+0100"));
        assertEquals("filterMask,time={+0200}", lr.prepareDateFilter("filterMask,time={+0200}", "+0100"));
        assertEquals("filterMask,time={+0200}Sep 22", lr.prepareDateFilter("filterMask,time={+0200}Sep 22", "+0100"));
        assertEquals("filterMask,time,action=ed exp", lr.prepareDateFilter("filterMask,time,action=ed exp", ""));
        assertEquals("filterMask,time={+0100},action=ed exp", lr.prepareDateFilter("filterMask,time,action=ed exp", "+0100"));
        assertEquals("filterMask,action=ed exp,time={+0100}", lr.prepareDateFilter("filterMask,action=ed exp", "+0100"));
        assertEquals("filterMask,time={+0100},action=ed exp", lr.prepareDateFilter("filterMask,time=,action=ed exp", "+0100"));
        assertEquals("filterMask,time={+0100}Sep 22,action=ed exp", lr.prepareDateFilter("filterMask,time=Sep 22,action=ed exp", "+0100"));
        assertEquals("filterMask,time={+0200},action=ed exp", lr.prepareDateFilter("filterMask,time={+0200},action=ed exp", "+0100"));
        assertEquals("filterMask,time={+0200}Sep 22,action=ed exp", lr.prepareDateFilter("filterMask,time={+0200}Sep 22,action=ed exp", "+0100"));
    }
}
