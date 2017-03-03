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

import com.google.common.net.HttpHeaders;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_MAX_AGE;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static java.lang.Boolean.TRUE;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class HttpHeaderTest {

    private static CacheControl CACHE_CONTROL = new CacheControl();
    private HttpHeader httpHeader;

    @BeforeClass
    public static void beforeClass() {
        CACHE_CONTROL.setPrivate(TRUE);
        CACHE_CONTROL.setNoCache(TRUE);
    }

    @Before
    public void before() {
        httpHeader = new HttpHeader("name", "600");
    }

    @Test
    public void getApplicationName() throws Exception {
        assertThat(httpHeader.getApplicationName(), is("name"));
    }

    @Test
    public void headers() throws Exception {
        Response response = httpHeader.headers().build();

        assertThat(response.getStatus(), is(OK.getStatusCode()));

        MultivaluedMap<String, Object> metaData = response.getMetadata();

        assertThat(metaData.size(), is(7));
        assertThat(metaData.get(HttpHeaders.CACHE_CONTROL), hasItem(CACHE_CONTROL));
        assertThat(metaData.get(ACCESS_CONTROL_ALLOW_ORIGIN), hasItem("*"));
        assertThat(metaData.get(ACCESS_CONTROL_ALLOW_HEADERS),
                hasItem("Authorization,X-Forwarded-For,Accept-Language,Content-Type"));
        assertThat(metaData.get(ACCESS_CONTROL_ALLOW_METHODS), hasItem("GET,POST,PUT,DELETE,OPTIONS"));
        assertThat(metaData.get(ACCESS_CONTROL_REQUEST_METHOD), hasItem("GET,POST,PUT,DELETE,OPTIONS"));
        assertThat(metaData.get(ACCESS_CONTROL_MAX_AGE), hasItem("600"));
        assertThat(metaData.get("X-Application-Id"), hasItem("name"));
        assertThat(response.getEntity(), is(nullValue()));
    }

    @Test
    public void headersNoContent() throws Exception {
        Response response = httpHeader.headers(NO_CONTENT).build();

        assertThat(response.getStatus(), is(NO_CONTENT.getStatusCode()));

        MultivaluedMap<String, Object> metaData = response.getMetadata();

        assertThat(metaData.size(), is(7));
        assertThat(metaData.get(HttpHeaders.CACHE_CONTROL), hasItem(CACHE_CONTROL));
        assertThat(metaData.get(ACCESS_CONTROL_ALLOW_ORIGIN), hasItem("*"));
        assertThat(metaData.get(ACCESS_CONTROL_ALLOW_HEADERS),
                hasItem("Authorization,X-Forwarded-For,Accept-Language,Content-Type"));
        assertThat(metaData.get(ACCESS_CONTROL_ALLOW_METHODS), hasItem("GET,POST,PUT,DELETE,OPTIONS"));
        assertThat(metaData.get(ACCESS_CONTROL_REQUEST_METHOD), hasItem("GET,POST,PUT,DELETE,OPTIONS"));
        assertThat(metaData.get(ACCESS_CONTROL_MAX_AGE), hasItem("600"));
        assertThat(metaData.get("X-Application-Id"), hasItem("name"));
        assertThat(response.getEntity(), is(nullValue()));
    }
}
