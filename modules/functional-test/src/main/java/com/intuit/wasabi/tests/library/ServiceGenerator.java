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
package com.intuit.wasabi.tests.library;

import com.jakewharton.retrofit.Ok3Client;
import okhttp3.OkHttpClient;
import org.apache.commons.codec.binary.Base64;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;


/**
 * Created on 1/29/16.
 */
public class ServiceGenerator {

    public static final String API_BASE_URL = "http://localhost:8080";
    public static final String BASIC_REALM = "Basic";

    private static RestAdapter.Builder builder = new RestAdapter.Builder()
            .setEndpoint(API_BASE_URL)
            .setClient(new Ok3Client(new OkHttpClient()));

    public static <S> S createService(Class<S> serviceClass, RequestInterceptor interceptor) {
        return createService(serviceClass, null, null);
    }

    public static <S> S createService(Class<S> serviceClass, String username, String password) {
        if (username != null && password != null) {
            // concatenate username and password with colon for authentication
            final String basic = createBasicAuthorization(BASIC_REALM, username, password);

            builder.setRequestInterceptor(LoginReuqestInterceptor(basic));
        }

        RestAdapter adapter = builder.build();
        return adapter.create(serviceClass);
    }

    public static String createBasicAuthorization(String Realm, String username, String password) {
        String credentials = username + ":" + password;
        // create Base64 encoded string
        final String basic = Realm + " " + Base64.encodeBase64(credentials.getBytes());
        return basic;
    }

    private static RequestInterceptor LoginReuqestInterceptor(final String basic) {
        return new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("Authorization", basic);
                request.addHeader("Accept", "application/json");
                request.addHeader("User-Agent", "TestAgent");
            }
        };
    }

}
