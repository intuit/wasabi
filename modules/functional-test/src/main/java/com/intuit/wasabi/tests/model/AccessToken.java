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
package com.intuit.wasabi.tests.model;

import com.google.gson.annotations.SerializedName;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Objects;

/**
 * A very simple AccessToken wrapper.
 */
public class AccessToken extends ModelItem {

    /**
     * The serialization strategy for comparisons and JSON serialization.
     */
    private static SerializationStrategy serializationStrategy = new DefaultNameExclusionStrategy();
    /**
     * the token
     */
    @SerializedName("access_token")
    public String accessToken;
    /**
     * token type
     */
    @SerializedName("token_type")
    public String tokenType;

    /**
     * Creates an access token.
     */
    public AccessToken() {
    }

    /**
     * Copies an access token.
     *
     * @param other the token to copy.
     */
    public AccessToken(AccessToken other) {
        update(other);
    }

    /**
     * Sets the token and returns this instance.
     *
     * @param accessToken the token
     * @return this
     */
    public AccessToken setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    /**
     * Sets the token type and returns this instance.
     *
     * @param tokenType the token type
     * @return this
     */
    public AccessToken setTokenType(String tokenType) {
        this.tokenType = tokenType;
        return this;
    }

    @Override
    public SerializationStrategy getSerializationStrategy() {
        return AccessToken.serializationStrategy;
    }

    @Override
    public void setSerializationStrategy(SerializationStrategy serializationStrategy) {
        AccessToken.serializationStrategy = serializationStrategy;
    }

    /**
     * If two access tokens are not equal, this will try to at least match the independent parts.
     * A token is considered equal if the version, the token and the user ID.
     *
     * @param other another object
     * @return "fuzzy" equality.
     */
    @Override
    public boolean equals(Object other) {
        boolean equal = super.equals(other);
        if (!equal && other instanceof AccessToken) {
            equal = Objects.equals(this.tokenType, ((AccessToken) other).tokenType);
            equal &= Objects.equals(this.getTokenVersion(), ((AccessToken) other).getTokenVersion());
            equal &= Objects.equals(this.getIUSToken(), ((AccessToken) other).getIUSToken());
            equal &= Objects.equals(this.getTokenUserId(), ((AccessToken) other).getTokenUserId());
        }
        return equal;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(tokenType)
                .append(getTokenVersion())
                .append(getIUSToken())
                .append(getTokenUserId())
                .build();
    }

    /**
     * Returns the raw token (base64 decoded).
     *
     * @return the raw token
     */
    public String getRawToken() {
        if (Objects.isNull(accessToken)) {
            return "";
        }
        return new String(Base64.decodeBase64(accessToken.getBytes()));
    }

    /**
     * Splits the raw token at the slashes to retrieve an array of token contents.
     *
     * @return the token parts
     */
    public String[] getTokenParts() {
        String[] parts = getRawToken().split("/");
        if (parts.length == 6) {
            return parts;
        } else {
            return new String[]{"", "", "", "", "", ""};
        }
    }

    /**
     * Returns the version information of the token.
     *
     * @return the version
     */
    public String getTokenVersion() {
        return getTokenParts()[0];
    }

    /**
     * Returns the IUS token.
     *
     * @return the IUS token
     */
    public String getIUSToken() {
        return getTokenParts()[1];
    }

    /**
     * Returns the timestamp.
     *
     * @return the timestamp
     */
    public String getTokenTimestamp() {
        return getTokenParts()[2];
    }

    /**
     * Returns the token date as a string.
     *
     * @return the date
     */
    public String getTokenDate() {
        return getTokenParts()[3];
    }

    /**
     * Returns the token user id.
     *
     * @return user id
     */
    public String getTokenUserId() {
        return getTokenParts()[4];
    }

    /**
     * Returns the salted token.
     *
     * @return salted token
     */
    public String getSaltoken() {
        return getTokenParts()[5];
    }
}
