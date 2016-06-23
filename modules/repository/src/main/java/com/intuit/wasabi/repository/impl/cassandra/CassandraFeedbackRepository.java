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
package com.intuit.wasabi.repository.impl.cassandra;


import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.cassandra.CassandraDriver;
import com.intuit.wasabi.cassandra.ExperimentDriver;
import com.intuit.wasabi.feedbackobjects.UserFeedback;
import com.intuit.wasabi.repository.FeedbackRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.impl.cassandra.serializer.UsernameSerializer;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.CqlResult;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.serializers.DateSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Cassandra implementation of
 * @see FeedbackRepository
 *
 */
public class CassandraFeedbackRepository implements FeedbackRepository {

    private final CassandraDriver driver;
    private final ExperimentsKeyspace keyspace;

    /**
     * Constructor
     * @param driver Cassandra driver
     * @param keyspace cassandra keyspace
     * @throws IOException   io exception
     * @throws ConnectionException connection exception
     */
    @Inject
    public CassandraFeedbackRepository(@ExperimentDriver CassandraDriver driver, ExperimentsKeyspace keyspace)
            throws IOException, ConnectionException {

        super();
        this.driver = driver;
        this.keyspace = keyspace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createUserFeedback(UserFeedback userFeedback) {

        String CQL = "INSERT INTO user_feedback " +
                "(user_id, submitted, score, comments, contact_okay, user_email) " +
                "VALUES (?, ?, ?, ?, ?, ?);";

        try {

            driver.getKeyspace().prepareQuery(keyspace.feedbackCF())
                    .withCql(CQL)
                    .asPreparedStatement()
                    .withByteBufferValue(userFeedback.getUsername(), UsernameSerializer.get())
                    .withByteBufferValue(userFeedback.getSubmitted(), DateSerializer.get())
                    .withIntegerValue(userFeedback.getScore())
                    .withStringValue(userFeedback.getComments())
                    .withBooleanValue(userFeedback.isContactOkay())
                    .withStringValue(userFeedback.getEmail())
                    .execute()
                    .getResult();

        } catch (ConnectionException e) {
            throw new RepositoryException("Could not insert feedback from user " + userFeedback.getUsername(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserFeedback> getUserFeedback(UserInfo.Username username) {

        Preconditions.checkNotNull(username, "Parameter \"username\" cannot be null");

        final String CQL = "select * from user_feedback " +
                "where user_id = ?";

        try {
            OperationResult<CqlResult<UserInfo.Username,String>> opResult=
                    driver.getKeyspace()
                            .prepareQuery(keyspace.feedbackCF())
                            .withCql(CQL)
                            .asPreparedStatement()
                            .withByteBufferValue(username, UsernameSerializer.get())
                            .execute();

            Rows<UserInfo.Username,String> rows=opResult.getResult().getRows();

            List<UserFeedback> result = new ArrayList<>();
            for (Row<UserInfo.Username, String> row : rows) {
                result.add(UserFeedback.newInstance(username)
                        .withScore(row.getColumns().getColumnByName("score").getIntegerValue())
                        .withSubmitted(row.getColumns().getColumnByName("submitted").getDateValue())
                        .withComments(row.getColumns().getColumnByName("comments").getStringValue())
                        .withContactOkay(row.getColumns().getColumnByName("contact_okay").getBooleanValue())
                        .withEmail(row.getColumns().getColumnByName("user_email").getStringValue())
                        .build());
            }
            return result;
        }
        catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve feedback from user " + username.toString(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserFeedback> getAllUserFeedback() {
        final String CQL = "select * from user_feedback";

        try {
            OperationResult<CqlResult<UserInfo.Username,String>> opResult=
                    driver.getKeyspace()
                            .prepareQuery(keyspace.feedbackCF())
                            .withCql(CQL)
                            .asPreparedStatement()
                            .execute();

            Rows<UserInfo.Username,String> rows=opResult.getResult().getRows();

            List<UserFeedback> result = new ArrayList<>();
            for (Row<UserInfo.Username, String> row : rows) {
                result.add(UserFeedback.newInstance(
                        UserInfo.Username.valueOf(row.getColumns().getColumnByName("user_id").getStringValue()))
                        .withScore(row.getColumns().getColumnByName("score").getIntegerValue())
                        .withSubmitted(row.getColumns().getColumnByName("submitted").getDateValue())
                        .withComments(row.getColumns().getColumnByName("comments").getStringValue())
                        .withContactOkay(row.getColumns().getColumnByName("contact_okay").getBooleanValue())
                        .withEmail(row.getColumns().getColumnByName("user_email").getStringValue())
                        .build());
            }
            return result;
        }
        catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve feedback from all users",e);
        }
    }
}
