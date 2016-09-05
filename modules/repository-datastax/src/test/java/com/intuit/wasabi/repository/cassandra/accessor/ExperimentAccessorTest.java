package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import com.intuit.wasabi.repository.cassandra.IntegrationTestBase;
import com.intuit.wasabi.repository.cassandra.pojo.Experiment;

import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExperimentAccessorTest extends IntegrationTestBase{
    static ExperimentAccessor accessor;
    static Mapper<Experiment> mapper;
    private final Logger logger = LoggerFactory.getLogger(ExperimentAccessorTest.class);
    private static final String TEST_UUID = "29f9db95-9a58-44f4-9e0f-2f1218e15e3c";
    @BeforeClass
    public static void setup(){
    	IntegrationTestBase.setup();
    	if (accessor != null) return;
        mapper = manager.mapper(Experiment.class);
        accessor = manager.createAccessor(ExperimentAccessor.class);
    }

//    @Test
//    public void insertData(){
//          Experiment.ExperimentBuilder builder = Experiment.builder()
//                  .id(UUID.fromString(TEST_UUID))
//                  .appName("TestApp")
//                  .created(new Date())
//                  .creatorId("me :)")
//                  .description("Test description")
//                  .startTime(new Date())
//                  .endTime(new Date())
//                  .modelName("")
//                  .modelVersion("")
//                  .personalized(false)
//                  .rapidExperiment(false)
//                  .samplePercent(1.0)
//                  .userCap(1_000_000)
//                  .state("DRAFT")
//                  .modified(new Date());
//
//        logger.info(mapper.saveQuery(builder.build()).toString());
//        logger.info(builder.build().toString());
////        Experiment experiment = new Experiment(UUID.fromString(TEST_UUID),
////                "Test description",
////                1.0,
////                new Date(),
////                new Date(),
////                "DRAFT",
////                "TestApp",
////                new Date(),
////                new Date(),
////                "",
////                "",
////                "",
////                false,
////                false,
////                1_000_000,
////                "me :)");
////
////        mapper.save(experiment);
//    }
//
//    @Test
//    public void insertPartialData(){
//        /*
//         INSERT INTO wassabi_experiment_local.experiment
//         ("modified",
//         "creatorid",
//         "appName",
//         "start_time",
//         "model_name",
//         "id",
//         "description",
//         "rule",
//         "is_personalized",
//         "user_cap",
//         "end_time",
//         "is_rapid_experiment",
//         "created",
//         "sample_percent",
//         "model_version",
//         "state")
//         VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);
//         */
//        /*
//        Experiment experiment = new Experiment(UUID.randomUUID(),
//                "Test description",
//                1.0,
//                null,
//                null,
//                "DRAFT",
//                "TestApp",
//                null,
//                null,
//                null,
//                null,
//                null,
//                false,
//                false,
//                1_000_000,
//                "me :)");
//
//        logger.info(mapper.saveQuery(experiment).toString());
//        */
//    }
//
//    @Test
//    public void getExperimentByAppName(){
//        Result<Experiment> experimentResult = accessor.getExperimentBy("TestApp");
//        logger.info(experimentResult.one().toString());
//    }
//
//    @Test
//    public void getExperimentUsingDefaultMapper(){
//        Experiment experiment = mapper.get(UUID.fromString(TEST_UUID));
//        logger.info(experiment.toString());
//    }
//
//    @Test
//    public void preparedStatementTest(){
//        PreparedStatement preparedStatement1 = session.prepare("UPDATE experiment SET STATE=?, MODIFIED=? WHERE id = ?");
//        session.execute(preparedStatement1.bind(
//                "RUNNING",
//                new Date(),
//                UUID.fromString(TEST_UUID)
//        ));
//    }
//
//    @Test
//    public void preparedStatementTest2(){
//
//        PreparedStatement preparedStatement2 = session.prepare("UPDATE experiment SET STATE=:state, MODIFIED=:modified WHERE id = :id");
//        //Using positional binding
//        session.execute(preparedStatement2.bind(
//                "RUNNING2",
//                new Date(),
//                UUID.fromString(TEST_UUID)
//        ));
//        //Using name binding
//        session.execute(preparedStatement2.bind()
//                .setString("state", "RUNNING")
//                .setTimestamp("modified", new Date())
//                .setUUID("id", UUID.fromString(TEST_UUID))
//        );
//    }


}