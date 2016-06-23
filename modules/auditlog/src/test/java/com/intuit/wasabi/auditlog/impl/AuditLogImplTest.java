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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Tests for {@link AuditLogImpl}.
 */
public class AuditLogImplTest {

    private AuditLog auditLog;

    @Mock
    private AuditLogRepository repository;
    private int limit = 100;

    private List<AuditLogEntry> appList;
    private List<AuditLogEntry> globalList;
    private List<AuditLogEntry> completeList;

    @Before
    public void init() throws Exception {
        MockitoAnnotations.initMocks(this);

        auditLog = new AuditLogImpl(repository, limit);

        appList = new ArrayList<>();
        globalList = new ArrayList<>();
        completeList = new ArrayList<>();
        fillLists();

        Mockito.when(repository.getAuditLogEntryList(Mockito.<Application.Name> any())).thenReturn(appList);
        Mockito.when(repository.getAuditLogEntryList(Mockito.<Application.Name>any(), Mockito.eq(limit))).thenReturn(appList);
        Mockito.when(repository.getCompleteAuditLogEntryList()).thenReturn(completeList);
        Mockito.when(repository.getCompleteAuditLogEntryList(Mockito.eq(limit))).thenReturn(completeList);
        Mockito.when(repository.getGlobalAuditLogEntryList()).thenReturn(globalList);
        Mockito.when(repository.getGlobalAuditLogEntryList(Mockito.eq(limit))).thenReturn(globalList);
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
        globalList.clear();
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

        for (AuditLogEntry auditLogEntry : completeList) {
            (auditLogEntry.getApplicationName() != null ? appList : globalList).add(auditLogEntry);
        }
    }

    @Test
    public void testGetAuditLogs1() throws Exception {
        Assert.assertArrayEquals(appList.toArray(), auditLog.getAuditLogs(Mockito.mock(Application.Name.class), null, null).toArray());
    }

    @Test
    public void testGetAuditLogs2() throws Exception {
        Assert.assertArrayEquals(appList.toArray(), auditLog.getAuditLogs(Mockito.mock(Application.Name.class), "", null).toArray());
    }

    @Test
    public void testGetAuditLogs3() throws Exception {
        Assert.assertArrayEquals(appList.toArray(), auditLog.getAuditLogs(Mockito.mock(Application.Name.class), null, "").toArray());
    }

    @Test
    public void testGetAuditLogs4() throws Exception {
        Assert.assertArrayEquals(appList.toArray(), auditLog.getAuditLogs(Mockito.mock(Application.Name.class), "", "").toArray());
    }

    @Test
    public void testGetCompleteAuditLogs1() throws Exception {
        Assert.assertArrayEquals(completeList.toArray(), auditLog.getAuditLogs(null, null).toArray());
    }

    @Test
    public void testGetCompleteAuditLogs2() throws Exception {
        Assert.assertArrayEquals(completeList.toArray(), auditLog.getAuditLogs("", null).toArray());
    }

    @Test
    public void testGetCompleteAuditLogs3() throws Exception {
        Assert.assertArrayEquals(completeList.toArray(), auditLog.getAuditLogs(null, "").toArray());
    }

    @Test
    public void testGetCompleteAuditLogs4() throws Exception {
        Assert.assertArrayEquals(completeList.toArray(), auditLog.getAuditLogs("", "").toArray());
    }

    @Test
    public void testGetGlobalAuditLogs1() throws Exception {
        Assert.assertArrayEquals(globalList.toArray(), auditLog.getGlobalAuditLogs(null, null).toArray());
    }

    @Test
    public void testGetGlobalAuditLogs2() throws Exception {
        Assert.assertArrayEquals(globalList.toArray(), auditLog.getGlobalAuditLogs("", null).toArray());
    }

    @Test
    public void testGetGlobalAuditLogs3() throws Exception {
        Assert.assertArrayEquals(globalList.toArray(), auditLog.getGlobalAuditLogs(null, "").toArray());
    }

    @Test
    public void testGetGlobalAuditLogs4() throws Exception {
        Assert.assertArrayEquals(globalList.toArray(), auditLog.getGlobalAuditLogs("", "").toArray());
    }

    @Test
    public void testFullTextSearch1() throws Exception {
        // a search for "em use" should return all even entries
        List<AuditLogEntry> expectedResults = new ArrayList<>();
        for (int i = 0; i < completeList.size(); i += 2) {
            expectedResults.add(completeList.get(i));
        }
        Assert.assertArrayEquals(expectedResults.toArray(), auditLog.getAuditLogs("em use", "").toArray());
    }

    @Test
    public void testFullTextSearch2() throws Exception {
        Assert.assertEquals("expected no matches", 0, auditLog.getAuditLogs("nonmatchingfulltextsearch", "").size());
    }

    @Test
    public void testFullTextSearch3() throws Exception {
        Assert.assertEquals("expected all to match", 10, auditLog.getAuditLogs("\\-nonmatchingfulltextsearch", "").size());
    }

    @Test
    public void testFilterSortFailFast1() throws Exception {
        // filter & sort blank
        Assert.assertArrayEquals(completeList.toArray(), auditLog.getAuditLogs("", "").toArray());
    }

    @Test
    public void testFilterSortFailFast2() throws Exception {
        // filter just ,
        Assert.assertArrayEquals(completeList.toArray(), auditLog.getAuditLogs(",","").toArray());
    }

    @Test
    public void testFilterSortFailFast3() throws Exception {
        // sort default
        Assert.assertArrayEquals(completeList.toArray(), auditLog.getAuditLogs("","-time").toArray());
    }

    @Test
    public void testFilterGrammar1() throws Exception {
        // filter for "ro" on field "attr", should match "prop" and return index 0 and 6.
        List<AuditLogEntry> expectedList = Arrays.asList(completeList.get(0), completeList.get(6));
        Assert.assertArrayEquals(expectedList.toArray(), auditLog.getAuditLogs("attr=ro", "").toArray());
    }

    @Test
    public void testFilterGrammar2() throws Exception {
        Assert.assertArrayEquals(completeList.toArray(), auditLog.getAuditLogs("time={-0900}Mar 14, 2001", "").toArray());
    }

    @Test
    public void testFilterGrammar3() throws Exception {
        // filter for "ro" on field "attr" and filter for "4" on experiment - only one entry should be left
        Assert.assertArrayEquals(new AuditLogEntry[] { completeList.get(0) }, auditLog.getAuditLogs("attr=ro,experiment=4", "").toArray());
    }

    @Test
    public void testFilterGrammar4() throws Exception {
        // filter for "ro" on field "attr" and filter for "4" on experiment - only one entry should be left
        Assert.assertArrayEquals(completeList.toArray(), auditLog.getAuditLogs("time=", "").toArray());
    }

    @Test
    public void testFilterGrammar5() throws Exception {
        // filter for "ro" on field "attr" and filter for "4" on experiment - only one entry should be left
        List<AuditLogEntry> expectedList = new ArrayList<>(9);
        for (AuditLogEntry auditLogEntry : completeList) {
            if (completeList.indexOf(auditLogEntry) != 3) {
                expectedList.add(auditLogEntry);
            }
        }
        Assert.assertArrayEquals(expectedList.toArray(), auditLog.getAuditLogs("time=\\-Mar 15, 2001 02:03:10", "").toArray());
    }


    @Test
    public void testFilterGrammar6() throws Exception {
        // filter for "ro" on field "attr" and filter for "4" on experiment - only one entry should be left
        Assert.assertArrayEquals(Collections.emptyList().toArray(), auditLog.getAuditLogs("time=a=b", "").toArray());
    }

    @Test
    public void testSortGrammar1() throws Exception {
        // manually sorting lists by experiment name ascending - null values should always go last
        List<AuditLogEntry> expectedSortedList = new ArrayList<>(completeList.size());

        int[] order = new int[] {9, 6, 3, 0};
        for (int i : order) {
            expectedSortedList.add(completeList.get(i));
        }
        Assert.assertArrayEquals("Sort order not correct (sort: experiment).", expectedSortedList.toArray(), auditLog.getAuditLogs("", "experiment").subList(0, 4).toArray());
    }

    @Test
    public void testSortGrammar2() throws Exception {
        // manually sorting lists by experiment name descending - null values should always go last
        List<AuditLogEntry> expectedSortedList = new ArrayList<>(completeList.size());

        int[] order = new int[] {0, 3, 6, 9};
        for (int i : order) {
            expectedSortedList.add(completeList.get(i));
        }
        Assert.assertArrayEquals("Sort order not correct (sort: -experiment).", expectedSortedList.toArray(), auditLog.getAuditLogs("", "-experiment").subList(0, 4).toArray());
    }

    @Test
    public void testSortGrammar3() throws Exception {
        List<AuditLogEntry> expectedSortedList = new ArrayList<>(completeList.size());

        // sort by bucket, action, -experiment, firstname, lastname, mail, app, attr, before, after, -time
        // where only bucket, -experiment, firstname and -time should have impacts
        int[] order = new int[] {
                // bucket determines these 4, -experiment determines the order
                0, 3, 6, 9,
                    // name breaks ties for the following six, leaving the order 1, 5, 7 (user), 2, 4, 8 (system)
                    // -time reverse the order to 7, 5, 1 / 8, 4, 2
                    7, 5, 1,
                    8, 4, 2
        };
        for (int i : order) {
            expectedSortedList.add(completeList.get(i));
        }
        List<AuditLogEntry> result = auditLog.getAuditLogs("", "bucket,-experiment,firstname,action,lastname,mail,app,attr,before,after,-time");
        Assert.assertArrayEquals("Sort order not correct (sort: bucket,-experiment,firstname,action,lastname,mail,app,attr,before,after,-time).", expectedSortedList.toArray(), result.toArray());
        // sort the result by app name (changes nothing)
        result = auditLog.sort(result, "app");
        Assert.assertArrayEquals("Sort order not correct (sort: app).", expectedSortedList.toArray(), result.toArray());

        // sort the result a bit around with properties, changes the order: brings 0,6 to the front (before 3,9 and the rest as above)
        expectedSortedList.remove(2);
        expectedSortedList.add(1, completeList.get(2));
        result = auditLog.sort(result, "attr,before");
        Assert.assertArrayEquals("Sort order not correct (sort: attr,before).", expectedSortedList.toArray(), result.toArray());

        // swaps the first two elements
        expectedSortedList.add(0, expectedSortedList.get(1));
        expectedSortedList.remove(2);
        result = auditLog.sort(result, "attr,after");
        Assert.assertArrayEquals("Sort order not correct (sort: attr,after).", expectedSortedList.toArray(), result.toArray());

        // sorts the list by the userNames and IDs
        expectedSortedList.add(7, expectedSortedList.get(1));
        expectedSortedList.add(7, expectedSortedList.get(0));
        expectedSortedList.remove(0);
        expectedSortedList.remove(0);
        expectedSortedList.add(1, expectedSortedList.get(4));
        expectedSortedList.remove(5);
        result = auditLog.sort(result, "user");
        Assert.assertArrayEquals("Sort order not correct (sort: user).", expectedSortedList.toArray(), result.toArray());
    }

    @Test
    public void testSortGrammar4() throws Exception {
        List<AuditLogEntry> result = auditLog.getAuditLogs("", "nonsortablekey");
        Assert.assertArrayEquals("Order of items changed for invalid value.", completeList.toArray(), result.toArray());
    }

    /**
     * Since the description is very volatile it is just checked if it does not fail to do something.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testSortGrammar5() throws Exception {
        List<AuditLogEntry> result = auditLog.getAuditLogs("", "desc");
        Assert.assertEquals(completeList.size(), result.size());
        for (AuditLogEntry auditLogEntry : result) {
            if (!completeList.contains(auditLogEntry)) {
                Assert.fail("AuditLogEntry " + auditLogEntry + " not in sorted list.");
            }
        }
        for (AuditLogEntry auditLogEntry : completeList) {
            if (!result.contains(auditLogEntry)) {
                Assert.fail("AuditLogEntry " + auditLogEntry + " not in expected list.");
            }
        }
    }

    @Test
    public void testFilterImmediateReturns1() throws Exception {
        Assert.assertEquals(completeList, auditLog.filter(completeList, ","));
    }

    @Test
    public void testFilterImmediateReturns2() throws Exception {
        Assert.assertEquals(Collections.emptyList(), auditLog.filter(completeList, ",johndoe"));
    }

    @Test
    public void testSingleFieldSearch() throws Exception {
        Assert.assertFalse(((AuditLogImpl) auditLog).singleFieldSearch(Mockito.mock(AuditLogEntry.class), "invalidKey", "somePattern", "", true));
        Assert.assertFalse(((AuditLogImpl) auditLog).singleFieldSearch(appList.get(0), "app", "App", "", false));
        Assert.assertFalse(((AuditLogImpl) auditLog).singleFieldSearch(appList.get(0), "-app", "App", "", true));
        Assert.assertFalse(((AuditLogImpl) auditLog).singleFieldSearch(appList.get(0), null, "App", "", true));
        Assert.assertTrue(((AuditLogImpl) auditLog).singleFieldSearch(appList.get(0), null, "App", "", false));
        Assert.assertTrue(((AuditLogImpl) auditLog).singleFieldSearch(appList.get(0), "time", "Mar 14, 2001 19:00", "-0700", true));
        Assert.assertFalse(((AuditLogImpl) auditLog).singleFieldSearch(appList.get(0), "time", "Mar 15, 2001", "", false));
        Assert.assertTrue(((AuditLogImpl) auditLog).singleFieldSearch(appList.get(0), null, "", "", false));
    }

}
