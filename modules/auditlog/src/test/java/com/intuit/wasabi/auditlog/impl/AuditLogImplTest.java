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
package com.intuit.wasabi.auditlog.impl;

import com.intuit.wasabi.auditlog.AuditLog;
import com.intuit.wasabi.auditlogobjects.AuditLogAction;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.AuditLogRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tests for {@link AuditLogImpl}.
 */
public class AuditLogImplTest {

    private AuditLog auditLog;

    @Mock
    private AuditLogRepository repository;
    private int limit = 100;

    private List<AuditLogEntry> appList;
    private List<AuditLogEntry> completeList;

    @Before
    public void init() throws Exception {
        MockitoAnnotations.initMocks(this);

        auditLog = new AuditLogImpl(repository, limit);

        appList = new ArrayList<>();
        completeList = new ArrayList<>();
        fillLists();

        Mockito.when(repository.getAuditLogEntryList(Mockito.<Application.Name>any())).thenReturn(appList);
        Mockito.when(repository.getAuditLogEntryList(Mockito.<Application.Name>any(), Mockito.eq(limit))).thenReturn(appList);
        Mockito.when(repository.getCompleteAuditLogEntryList()).thenReturn(completeList);
        Mockito.when(repository.getCompleteAuditLogEntryList(Mockito.eq(limit))).thenReturn(completeList);
    }

    /**
     * Fills the member array lists like this:
     *
     * The complete list is filled:
     * <ul>
     *     <li>10 Entries</li>
     *     <li>Time: Minute-steps from {@code 2001-03-15T02:00:10Z} to {@code 2001-03-15T02:09:10Z}.</li>
     *     <li>Odd entries are by the the {@code System Users}, even entries by admin users.</li>
     *     <li>All actions are {@code UNSPECIFIED_ACTIONS}.</li>
     *     <li>Every third entry has an experiment, starting with the first (thus indices {@code 0, 3, 6, 9}).</li>
     *     <li>Those experiments are labeled {@code exp4} through {@code exp1}.</li>
     *     <li>All experiments belong to the Application {@code App}.</li>
     *     <li>Their UUIDs are created with most = least sig bits, both initializing to {@code 1 << i}</li>
     *     <li>Each experiment has one bucket with the label {@code label}.</li>
     *     <li>Even-number-indexed experiments (thus index 0 and 6) have a changed property {@code prop} which changed for the first from {@code 0} to {@code 6} and vice-verse for the second.</li>
     *     <li>The users at index 1 and 3 (admin) are the only ones with userIDs.</li>
     * </ul>
     *
     * The appList contains all of those events which have their app != null.
     * The globalList contains all events which are not in the appList.
     */
    private void fillLists() {
        appList.clear();
        completeList.clear();
        // note: this string/label is critical to be ordered before string "system" in order for the tests to pass ... at this time
        UserInfo userInfo = UserInfo.from(UserInfo.Username.valueOf("aUser"))
                .withFirstName("firstname")
                .withLastName("lastname")
                .withEmail("user@example.org")
                .build();

        for (int i = 0; i < 10; ++i) {
            // count date up
            Calendar time = Calendar.getInstance();
            time.setTimeZone(TimeZone.getTimeZone("UTC"));
            time.set(2001, Calendar.MARCH, 15, 2, i, 10);

            // odd users system, even users (admin)
            UserInfo user = userInfo;
            if ((i & 1) == 0) {
                user = EventLog.SYSTEM_USER;
            }
            if (i == 1 || i == 3) {
                user = UserInfo.from(userInfo.getUsername())
                        .withEmail(userInfo.getEmail())
                        .withFirstName(userInfo.getFirstName())
                        .withLastName(userInfo.getLastName())
                        .withUserId("userid")
                        .build();
            }

            // 0, 3, 6, 9 experiments: exp4, exp3, exp2, exp1
            // Application: App
            // 0, 6 changed properties from 0 -> 6 and from 6 -> 0
            // IDs: 2^0 = 0x1, 2^3 = 0x8, 2^6 = 0x40, 2^9 = 0x200 (for each, MSB and LSB)
            Experiment exp = null;
            String property = null;
            if ((i % 3) == 0) {
                exp = Mockito.mock(Experiment.class);
                Mockito.doReturn(Application.Name.valueOf("App")).when(exp).getApplicationName();
                Mockito.doReturn(Experiment.ID.valueOf(new UUID(1 << i, 1 << i))).when(exp).getID();
                Mockito.doReturn(Experiment.Label.valueOf("exp" + ((9 - i) / 3 + 1))).when(exp).getLabel();
                if ((i & 1) == 0) {
                    property = "prop";
                }
            }
            AuditLogEntry ale = new AuditLogEntry(time, user, AuditLogAction.UNSPECIFIED_ACTION,
                    exp, exp == null ? null : Bucket.Label.valueOf("label"),
                    property, property == null ? null : String.valueOf(i), property == null ? null : String.valueOf(Math.abs(6 - i)));
            completeList.add(ale);
        }

        appList.addAll(completeList.stream().filter(auditLogEntry -> auditLogEntry.getApplicationName() != null).collect(Collectors.toList()));
    }

    @Test
    public void testGetCompleteAuditLogs1() throws Exception {
        Assert.assertArrayEquals(completeList.toArray(), auditLog.getAuditLogs().toArray());
    }
}
