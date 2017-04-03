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
package com.intuit.wasabi.exceptions;

import com.intuit.wasabi.experimentobjects.exceptions.ErrorCode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WasabiServerExceptionTest {

    private String detailMessage = " WasabiServerException error: ";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testServerException1() {
        thrown.expect(WasabiServerException.class);
        throw new ServerExceptionImpl(ErrorCode.APPLICATION_CONSTRAINT_VIOLATION);
    }

    @Test
    public void testServerException2() {
        thrown.expect(WasabiServerException.class);
        throw new ServerExceptionImpl(ErrorCode.ASSIGNMENT_EXISTS_VIOLATION, new Throwable());
    }

    @Test
    public void testServerException3() {
        thrown.expect(WasabiServerException.class);
        throw new ServerExceptionImpl(ErrorCode.ASSIGNMENT_NOT_FOUND, detailMessage);
    }

    @Test
    public void testServerException4() {
        thrown.expect(WasabiServerException.class);
        throw new ServerExceptionImpl(ErrorCode.BUCKET_NOT_FOUND, detailMessage, new Throwable());
    }


    /**
     * Test implementation for WasabiServerException
     *
     */
    private static class ServerExceptionImpl extends WasabiServerException {

        private static final long serialVersionUID = 4395416261119073415L;

        protected ServerExceptionImpl(ErrorCode errorCode) {
            super(errorCode);
        }

        protected ServerExceptionImpl(ErrorCode errorCode, Throwable cause) {
            super(errorCode, cause);
        }

        protected ServerExceptionImpl(ErrorCode errorCode, String detailMessage) {
            super(errorCode, detailMessage);
        }

        protected ServerExceptionImpl(ErrorCode errorCode, String detailMessage,
                                      Throwable cause) {
            super(errorCode, detailMessage, cause);
        }

    }

}
