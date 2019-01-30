package com.privacy.wasabi.database;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class MongoService {
    private static MongoService ourInstance = new MongoService();
    private static final Logger LOGGER = getLogger(MongoService.class);

    private MongoClient mongoClient = null;

    private String mongoURI = System.getenv("MONGO_URI");
    private String mongoDB = System.getenv("MONGO_DB");

    public static MongoService getInstance() {
        return ourInstance;
    }

    public MongoClient getMongoClient(){
        if (mongoURI != null && mongoDB != null) {
            synchronized (mongoClient) {
                if (mongoClient == null || mongoClient.getDatabaseNames().isEmpty()) {
                    LOGGER.debug("Instantiating MongoClient");
                    mongoClient = new MongoClient(new MongoClientURI(mongoURI));
                }

                return mongoClient;
            }
        }
    }

    private MongoService() {

    }
}
