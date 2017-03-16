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
package com.intuit.wasabi.events.impl;

import com.google.inject.Inject;
import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.database.TransactionFactory;
import com.intuit.wasabi.events.EventsExport;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.DatabaseRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.ResultIterator;

import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

/**
 * This class exports events from a given Experiment.ID.
 */
public class EventsExportImpl implements EventsExport {
    private DBI db;
    private ExperimentRepository databaseRepository;

    @Inject
    public EventsExportImpl(final TransactionFactory transactionFactory,
                            final @DatabaseRepository ExperimentRepository databaseRepository) {
        this.databaseRepository = databaseRepository;

        db = new DBI(transactionFactory.getDataSource());
    }

    public Experiment getExperiment(Experiment.ID id) {
        return databaseRepository.getExperiment(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamingOutput getEventStream(final Experiment.ID experimentID, final Parameters parameters) {
        // Check to make sure that experiment exists
        Experiment id = getExperiment(experimentID);

        if (id == null) {
            throw new ExperimentNotFoundException(experimentID);
        }

        Date fromTs = parameters.getFromTime();
        Date toTs = parameters.getToTime();
        Timestamp fromTsNew = fromTs != null ? new Timestamp(fromTs.getTime()) : new Timestamp(id.getStartTime().getTime());
        Timestamp toTsNew = toTs != null ? new Timestamp(toTs.getTime()) : new Timestamp(id.getEndTime().getTime());

        final Date fromTsFinal = fromTsNew;
        final Date toTsFinal = toTsNew;

        return new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException {
                Writer writer = new BufferedWriter(new OutputStreamWriter(os));
                Handle h = db.open();

                ResultIterator<Map<String, Object>> rs = h.createQuery(
                        "SELECT user_id, bucket_label, timestamp, 'ACTION' AS event_type, action AS name, payload" +
                                " FROM event_action" +
                                " WHERE experiment_id = UNHEX(REPLACE('" + experimentID + "','-','')) " +
                                "and timestamp >= '" + fromTsFinal + "'" + " and timestamp <= '" + toTsFinal + "'" +
                                " UNION ALL" +
                                " SELECT user_id, bucket_label, timestamp, 'IMPRESSION' AS event_type , 'IMPRESSION' AS name, payload" +
                                " FROM event_impression" +
                                " WHERE experiment_id = UNHEX(REPLACE('" + experimentID + "','-',''))" +
                                "and timestamp >= '" + fromTsFinal + "'" + " and timestamp <= '" + toTsFinal + "'" +
                                " ORDER BY user_id, timestamp")
                        .iterator();
                String header = "userId" + "\t" +
                        "bucketLabel" + "\t" +
                        "timestamp" + "\t" +
                        "eventType" + "\t" +
                        "name" + "\t" +
                        "payload" + "\n";
                writer.write(header);
                while (rs.hasNext()) {
                    Map<String, Object> row = rs.next();
                    writer.write(row.get("user_id") + "\t" +
                            row.get("bucket_label") + "\t" +
                            row.get("timestamp") + "\t" +
                            row.get("event_type") + "\t" +
                            row.get("name") + "\t" +
                            row.get("payload") + "\n"
                    );
                }
                rs.close();
                h.close();
                writer.flush();
            }
        };
    }
}
