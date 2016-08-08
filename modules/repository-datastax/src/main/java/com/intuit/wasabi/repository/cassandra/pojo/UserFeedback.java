/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.repository.cassandra.pojo;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.google.common.base.Preconditions;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

@Table(name="user_feedback",keyspace="wasabi_experiments")
public class UserFeedback {

	@PartitionKey
	@Column(name="user_id")
	private String userId;

	@ClusteringColumn(0)
    private Date submitted;
    private int score = 0;
    private String comments= "";
    
    @Column(name="contact_okay")
    private boolean contactOkay = false;
    
    @Column(name="user_email")
    private String email = "";

    public UserFeedback(String userId, Date submitted, int score,
			String comments, boolean contactOkay, String email) {
		this.userId = userId;
		this.submitted = submitted;
		this.score = score;
		this.comments = comments;
		this.contactOkay = contactOkay;
		this.email = email;
	}
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
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
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

    @Override
    public String toString() {
    	return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(5, 69)
                .append(submitted)
                .append(email)
                .append(userId)
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
                .append(email, other.getEmail())
                .append(userId, other.getUserId())
                .append(score, other.getScore())
                .append(comments, other.getComments())
                .append(contactOkay, other.isContactOkay())
                .isEquals();
    }
}
