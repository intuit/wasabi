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
package com.intuit.wasabi.auditlogobjects;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.ObjectCodec;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

/*
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.ObjectCodec;
*/

/**
 * Tests for {@link AuditLogEntry}.
 */
public class AuditLogEntryTest {

    @Test
    public void testConstructor() throws Exception {
        try {
            new AuditLogEntry(null, null, null);
            Assert.fail("AuditLogEntry must not allow null time");
        } catch (IllegalArgumentException ignored) {
        }

        try {
            new AuditLogEntry(Calendar.getInstance(), null, null);
            Assert.fail("AuditLogEntry must not allow null user");
        } catch (IllegalArgumentException ignored) {
        }


        UserInfo user = Mockito.mock(UserInfo.class);
        try {
            new AuditLogEntry(Calendar.getInstance(), user, null);
            Assert.fail("AuditLogEntry must not allow null username");
        } catch (IllegalArgumentException ignored) {
        }

        Mockito.doReturn(Mockito.mock(UserInfo.Username.class)).when(user).getUsername();
        Mockito.doReturn("firstname").when(user).getFirstName();
        Mockito.doReturn("lastname").when(user).getLastName();
        Mockito.doReturn("admin@example.com").when(user).getEmail();
        try {
            new AuditLogEntry(Calendar.getInstance(), user, null);
            Assert.fail("AuditLogEntry must not allow null action");
        } catch (IllegalArgumentException ignored) {
        }

        // simple constructor
        new AuditLogEntry(Calendar.getInstance(), user, AuditLogAction.UNSPECIFIED_ACTION);

        // complete example
        Experiment experiment = Mockito.mock(Experiment.class);
        Mockito.doReturn(Mockito.mock(Experiment.Label.class)).when(experiment).getLabel();
        Mockito.doReturn(Mockito.mock(Experiment.ID.class)).when(experiment).getID();
        Mockito.doReturn(Mockito.mock(Application.Name.class)).when(experiment).getApplicationName();
        AuditLogEntry ale = new AuditLogEntry(Calendar.getInstance(), user, AuditLogAction.UNSPECIFIED_ACTION,
                experiment, Mockito.mock(Bucket.Label.class), "Property", "before", "after");

        // fields
        for (Field f : AuditLogEntry.class.getDeclaredFields()) {
            f.setAccessible(true);
            Assert.assertNotNull(f.getName(), f.get(ale));
        }

        // getters
        for (Method m : AuditLogEntry.class.getDeclaredMethods()) {
            if (m.getParameterTypes().length == 0 && m.getName().startsWith("get")) {
                Assert.assertNotNull(m.invoke(ale));
            }
        }
    }

    @Test
    public void testSerializer() throws Exception {
        // create an audit log entry
        Experiment exp = Mockito.mock(Experiment.class);
        Mockito.when(exp.getApplicationName()).thenReturn(Application.Name.valueOf("App"));
        Mockito.when(exp.getLabel()).thenReturn(Experiment.Label.valueOf("Exp1"));
        Mockito.when(exp.getID()).thenReturn(Experiment.ID.valueOf(new UUID(1, 1)));
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.set(2011, Calendar.JULY, 1, 15, 0, 13);
        AuditLogEntry entry = new AuditLogEntry(time, EventLog.SYSTEM_USER, AuditLogAction.BUCKET_CHANGED,
                exp, Bucket.Label.valueOf("label"),
                "allocation", String.valueOf(0.2), String.valueOf(0.5));


        // create a json generator which writes the date and user etc correctly
        abstract class MyAbstractObjectCodec extends ObjectCodec {
            @Override
            public void writeValue(JsonGenerator jgen, Object value) throws IOException {
                if (value instanceof Calendar) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    jgen.writeString(sdf.format(((Calendar) value).getTime()).replace("+0000", "Z"));
                } else if (value instanceof UserInfo) {
                    UserInfo user = (UserInfo) value;
                    jgen.writeStartObject();
                    jgen.writeStringField("firstName", user.getFirstName());
                    jgen.writeStringField("lastName", user.getLastName());
                    jgen.writeStringField("username", user.getUsername().toString());
                    jgen.writeStringField("userID", user.getUserId());
                    jgen.writeStringField("email", user.getEmail());
                    jgen.writeEndObject();
                } else {
                    jgen.writeString(value.toString());
                }
            }
        }

        File tmpFile = File.createTempFile("jacksonSerialized", "json");
        JsonGenerator jsonGenerator = new JsonFactory().createGenerator(tmpFile, JsonEncoding.UTF8);
        ObjectCodec codec = Mockito.mock(MyAbstractObjectCodec.class);
        jsonGenerator.setCodec(codec);

        Mockito.doCallRealMethod().when(codec).writeValue(Mockito.eq(jsonGenerator), Mockito.any());

        // try serialization
        AuditLogEntry.Serializer serializer = new AuditLogEntry.Serializer();
        serializer.serialize(entry, jsonGenerator, null);
        jsonGenerator.close();

        try (BufferedReader br = new BufferedReader(new FileReader(tmpFile))) {
            Assert.assertEquals(
                    "{\"time\":\"2011-07-01T15:00:13Z\","
                            + "\"user\":{\"firstName\":\"System\",\"lastName\":\"User\",\"username\":\"system_user\",\"userID\":\"SystemUser\",\"email\":\"admin@example.com\"},"
                            + "\"action\":{\"type\":\"BUCKET_CHANGED\",\"message\":\"changed allocation of bucket label to 50%\"},"
                            + "\"applicationName\":\"App\","
                            + "\"experiment\":{\"experimentLabel\":\"Exp1\",\"experimentId\":\"00000000-0000-0001-0000-000000000001\"},"
                            + "\"bucketLabel\":\"label\","
                            + "\"change\":{\"changedAttribute\":\"allocation\",\"before\":\"0.2\",\"after\":\"0.5\"}}",
                    br.readLine());
        } catch (IOException ex) {
            Assert.fail("Failed to read TempFile: " + ex.getMessage());
        }
    }

    @Test
    public void testToString() throws Exception {
        // create an audit log entry
        Experiment exp = Mockito.mock(Experiment.class);
        Mockito.when(exp.getApplicationName()).thenReturn(Application.Name.valueOf("App"));
        Mockito.when(exp.getLabel()).thenReturn(Experiment.Label.valueOf("Exp1"));
        Mockito.when(exp.getID()).thenReturn(Experiment.ID.valueOf(new UUID(1, 1)));
        Calendar time = Calendar.getInstance();
        time.set(2011, Calendar.JULY, 1, 15, 0, 13);
        AuditLogEntry entry = new AuditLogEntry(time, EventLog.SYSTEM_USER, AuditLogAction.BUCKET_CHANGED,
                exp, Bucket.Label.valueOf("label"),
                "allocationPercent", String.valueOf(0.2), String.valueOf(0.5));
        Assert.assertFalse("toString seems to be the default.", entry.toString().contains("AuditLogEntry@"));
    }
}
