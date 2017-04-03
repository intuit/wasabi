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

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.email.EmailLinksList;
import com.intuit.wasabi.email.EmailService;
import com.intuit.wasabi.experimentobjects.Application;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * The API endpoint /email can be used to send emails to application admins and ask for access.
 */
@Path("/v1/email")
@Produces(APPLICATION_JSON)
@Singleton
@Api(value = "Email (Send Emails)", produces = "application/json", protocols = "https")
public class EmailResource {

    private static final Logger LOGGER = getLogger(EmailResource.class);
    private final HttpHeader httpHeader;
    private EmailService emailService;

    @Inject
    EmailResource(final EmailService emailService, final HttpHeader httpHeader) {
        this.emailService = emailService;
        this.httpHeader = httpHeader;
    }

    /**
     * Sends an email to all the administrators of the specified application to ask for permission for the
     * specified user.
     *
     * @param applicationName the name of the application for which the log should be fetched
     * @param user            the {@link com.intuit.wasabi.authenticationobjects.UserInfo.Username}
     *                        of the user access is requested for
     * @param emails          the {@link com.intuit.wasabi.email.EmailLinksList} of links list
     * @return a {@link Response} that is ok when the call succeeded.
     */
    @POST
    @Path("/applications/{applicationName}/users/{user}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Sends email to administrators of the specified application requesting access for a user",
            httpMethod = "POST",
            produces = "application/json",
            protocols = "https")
    @Timed
    public Response postEmail(
            @PathParam("applicationName")
            @ApiParam(value = "Application Name")
            final Application.Name applicationName,

            @PathParam("user")
            @ApiParam(value = "Requesting user ID")
            final UserInfo.Username user,

            @ApiParam(name = "clickableAccessEmailLinks", value = "Clickable Access Email Links")
            final EmailLinksList emails) {
        try {
            String message;

            if (emailService.isActive()) {
                emailService.sendEmailForUserPermission(applicationName, user, emails);

                // todo: string.format
                message = "An email has been sent to the administrators of "
                        + applicationName + " to ask for access for user "
                        + user + " with links " + emails.toString();
                return httpHeader.headers().entity(message).build();
            } else {
                LOGGER.warn("User tried to send an email via API-call, but the service is not active.");

                message = "The email service is not activated at the moment.";
            }

            return httpHeader.headers(SERVICE_UNAVAILABLE).entity(message).build();
        } catch (Exception exception) {
            LOGGER.error("postEmail failed for applicationName={}, user={}, emails={} with error:",
                    applicationName, user, emails, exception);
            throw exception;
        }
    }
}
