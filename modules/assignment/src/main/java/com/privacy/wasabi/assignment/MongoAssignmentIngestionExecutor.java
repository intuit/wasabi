package com.privacy.wasabi.assignment;

import com.intuit.wasabi.assignment.AssignmentIngestionExecutor;
import com.intuit.wasabi.assignmentobjects.AssignmentEnvelopePayload;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.ServerAddress;
import com.mongodb.MongoException;

import com.privacy.wasabi.database.MongoService;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Date;

public class MongoAssignmentIngestionExecutor implements AssignmentIngestionExecutor {

    public static final String NAME = "MONGOINGESTOR";
    private static final Logger LOGGER = getLogger(MongoAssignmentIngestionExecutor.class);

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private String mongoDB = System.getenv("MONGO_DB");

    @Override
    public Future<?> execute(AssignmentEnvelopePayload assignmentEnvelopePayload) {
        return executor.submit(() -> {
            try {

                MongoClient mongoClient = MongoService.getInstance().getMongoClient();
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
            } catch (MongoException e) {
                LOGGER.warn("Cannot write to mongoDB. Error=", e.toString());
            } catch (Exception e) {
                LOGGER.warn("An error occurred. Error=", e.toString());
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
