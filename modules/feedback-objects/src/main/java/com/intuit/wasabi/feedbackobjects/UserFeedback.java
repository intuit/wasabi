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
package com.intuit.wasabi.feedbackobjects;

import com.google.common.base.Preconditions;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

public class UserFeedback {

    @ApiModelProperty(value = "User Name", dataType = "String")
    private UserInfo.Username username;
    @ApiModelProperty(value = "Feedback Date", dataType = "String", hidden = true)
    private Date submitted;
    @ApiModelProperty(value = "Score in the range from 1-10", required = false)
    private int score = 0;
    @ApiModelProperty(value = "User feedback comments", required = false)
    private String comments = "";
    @ApiModelProperty(value = "boolean indicating that it's okay to contact the user", required = false)
    private boolean contactOkay = false;
    @ApiModelProperty(value = "User's email address", required = false)
    private String email = "";

    public Date getSubmitted() {
        return submitted;
    }

    public void setSubmitted(Date submitted) {
        this.submitted = submitted;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserInfo.Username getUsername() {
        return username;
    }

    public void setUsername(UserInfo.Username username) {
        this.username = username;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        Preconditions.checkArgument(1 <= score && score <= 10, "error, score was %s but " +
                "expected in the range [1,10]", score);
        this.score = score;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public boolean isContactOkay() {
        return contactOkay;
    }

    public void setContactOkay(boolean contactOkay) {
        this.contactOkay = contactOkay;
    }

    public UserFeedback() {
        super();
        submitted = new Date();
    }

    public static Builder newInstance(UserInfo.Username username) {
        return new Builder(username);
    }

    public static Builder from(UserFeedback feedback) {
        return new Builder(feedback);
    }

    public static class Builder {

        private Builder(UserInfo.Username username) {
            instance = new UserFeedback();
            instance.username = Preconditions.checkNotNull(username);
        }

        private Builder(UserFeedback other) {
            this(other.getUsername());
            instance.comments = other.getComments();
            instance.email = other.getEmail();
            instance.score = other.getScore();
            instance.contactOkay = other.isContactOkay();
            instance.submitted = other.submitted;
        }

        public Builder withComments(final String comments) {
            instance.comments = comments;
            return this;
        }

        public Builder withScore(final int score) {
            instance.score = score;
            return this;
        }

        public Builder withContactOkay(final boolean contactOkay) {
            instance.contactOkay = contactOkay;
            return this;
        }

        public Builder withSubmitted(final Date submitted) {
            instance.submitted = submitted;
            return this;
        }

        public Builder withEmail(final String email) {
            instance.email = email;
            return this;
        }

        public UserFeedback build() {
            UserFeedback result = instance;
            instance = null;
            return result;
        }

        private UserFeedback instance;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(5, 69)
                .append(submitted)
                .append(email)
                .append(username)
                .append(score)
                .append(comments)
                .append(contactOkay)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof UserFeedback))
            return false;

        UserFeedback other = (UserFeedback) obj;
        return new EqualsBuilder()
                .append(submitted, other.getSubmitted())
                .append(email, other.getEmail())
                .append(username, other.getUsername())
                .append(score, other.getScore())
                .append(comments, other.getComments())
                .append(contactOkay, other.isContactOkay())
                .isEquals();
    }
}
