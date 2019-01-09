package com.privacy.wasabi.assignment;

import com.intuit.wasabi.assignment.AssignmentIngestionExecutor;
import com.intuit.wasabi.assignmentobjects.AssignmentEnvelopePayload;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.assignmentobjects.User.ID;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.Label;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Date;

import java.net.UnknownHostException;

public class MongoAssignmentIngestionExecutor implements AssignmentIngestionExecutor {

    public static final String NAME = "MONGOINGESTOR";
    private static final Logger LOGGER = getLogger(MongoAssignmentIngestionExecutor.class);

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public Future<?> execute(AssignmentEnvelopePayload assignmentEnvelopePayload) {
        return executor.submit(() -> {
            String mongoURI = System.getenv("MONGO_URI");
            String mongoDB = System.getenv("MONGO_DB");
            if (mongoURI != null && mongoDB != null) {
                try {
                    MongoClient mongoClient = new MongoClient(new MongoClientURI(mongoURI));
                    DB database = mongoClient.getDB(mongoDB);
                    DBCollection assignmentCollection = database.getCollection("experimentassignments");

                    User.ID userID = assignmentEnvelopePayload.getUserID();
                    Bucket.Label bucketLabel = assignmentEnvelopePayload.getBucketLabel();
                    Experiment.Label experimentLabel = assignmentEnvelopePayload.getExperimentLabel();

                    BasicDBObject doc = new BasicDBObject("userTrackingID", userID.toString())
                        .append("experiment", experimentLabel.toString())
                        .append("bucket", bucketLabel.toString())
                        .append("createTime", new Date());
                    assignmentCollection.insert(doc);
                    mongoClient.close();
                } catch (UnknownHostException e) {
                    LOGGER.debug("An error occurred in writing to MongoDB");
                }
            }
            return 0;
        });
    }

    @Override
    public int queueLength() {
        return 0;
    }

    @Override
    public Map<String, Object> queueDetails() {
        Map<String, Object> map = new HashMap<>();
        return map;
    }

    @Override
    public void flushMessages() {
    }

    @Override
    public String name() {
        return NAME;
    }
}
