package com.intuit.wasabi.api;

import com.intuit.wasabi.authenticationobjects.UserInfo.Username;
import com.intuit.wasabi.email.EmailLinksList;
import com.intuit.wasabi.email.EmailService;
import com.intuit.wasabi.experimentobjects.Application;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EmailResourceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    EmailService mock;

    EmailResource resource;

    @Before
    public void setUp() {
        resource = new EmailResource(mock, new HttpHeader("apbcc"));
    }

    @Test
    public void testSendEmailServiceNotActive() {
        when(mock.isActive()).thenReturn(false);
        Response result = resource.postEmail(Application.Name.valueOf("a1"), Username.valueOf("u1"), EmailLinksList.newInstance().build());
        assertEquals("The email service is not activate at the moment.", result.getEntity());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE_503, result.getStatus());
    }

    @Test
    public void testSendEmailServiceActive() {
        when(mock.isActive()).thenReturn(true);
        Response result = resource.postEmail(Application.Name.valueOf("a1"), Username.valueOf("u1"), EmailLinksList.newInstance().build());
        assertEquals("An email has been sent to the administrators of a1 to ask for access for user u1with links", result.getEntity());
        assertEquals(HttpStatus.OK_200, result.getStatus());
    }
}
