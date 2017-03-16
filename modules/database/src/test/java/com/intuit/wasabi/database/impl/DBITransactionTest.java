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
package com.intuit.wasabi.database.impl;

import com.googlecode.catchexception.CatchException;
import com.intuit.wasabi.exceptions.DatabaseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.GeneratedKeys;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Update;

import java.sql.Connection;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class DBITransactionTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Handle handle;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DBI dbi;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    com.intuit.wasabi.database.Transaction.Block block;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Connection connection;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Update update;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    GeneratedKeys<Integer> gks;

    DBITransaction transaction;

    @Before
    public void setUp() {
        transaction = new DBITransaction(dbi);
    }

    @Test
    public void testTransaction() {
        given(block.value(transaction)).willReturn("abc");
        Object result = transaction.transaction(block);
        then((String) result).isEqualTo("abc");
    }

    @Test
    public void testInsert() throws Exception {
        given(handle.createStatement("q1")).willReturn(update);
        given(update.execute()).willReturn(1);
        transaction.insert("q1", new Object[]{});
    }


    @Test
    public void testTransactionThrowsIllegalArgumentException() {
        given(block.value(transaction)).willThrow(new IllegalArgumentException("test", null));
        CatchException.verifyException(transaction,
                IllegalArgumentException.class).transaction(block);
    }

    @Test
    public void testTransactionThrowsRuntimeException() {
        given(block.value(transaction)).willThrow(new RuntimeException("test", null));
        CatchException.verifyException(transaction,
                DatabaseException.class).transaction(block);
    }
}
