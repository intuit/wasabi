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
package com.intuit.wasabi.database;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.PreparedStatement;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class UUIDArgumentTest {

    UUIDArgument argument;

    @Mock
    PreparedStatement preparedStatement;

    @Before
    public void setUp() {
        argument = new UUIDArgument(UUID.randomUUID());
    }

    @Test
    public void test() throws Exception {
        argument.apply(1, preparedStatement, null);
        Mockito.verify(preparedStatement).setBytes(Mockito.eq(1), Mockito.isA(byte[].class));
    }

}
