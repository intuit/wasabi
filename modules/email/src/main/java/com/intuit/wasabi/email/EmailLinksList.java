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
package com.intuit.wasabi.email;

import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * A Class to hold a list of email links for email.
 */
public class EmailLinksList {


    public static final String LINE_SEPARATOR = " \n";

    @ApiModelProperty(value = "List of clickable email links for application Admins")
    private List<String> emailLinks = new ArrayList<>();

    protected EmailLinksList() {
        super();
    }

    public static Builder newInstance() {
        return new Builder();
    }

    public static Builder from(EmailLinksList value) {
        return new Builder(value);
    }

    public static Builder withEmailLinksList(List<String> emailLinks) {
        return new Builder(emailLinks);
    }

    public List<String> getEmailLinks() {
        return emailLinks;
    }

    @Override
    public String toString() {
        StringBuilder allLinks = new StringBuilder();
        for (String link : getEmailLinks()) {
            allLinks.append(link);
            allLinks.append(LINE_SEPARATOR);
        }
        return allLinks.toString();
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    public static class Builder {

        private EmailLinksList instance;

        private Builder() {
            instance = new EmailLinksList();
        }

        private Builder(EmailLinksList other) {
            this();
            instance.emailLinks = new ArrayList<>(other.emailLinks);
        }

        public Builder(List<String> emailLinks) {
            this();
            instance.emailLinks = new ArrayList<>(emailLinks);
        }

        public EmailLinksList build() {
            EmailLinksList result = instance;
            instance = null;
            return result;
        }

    }
}
