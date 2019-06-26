/*******************************************************************************

 *******************************************************************************/
package com.intuit.wasabi.assignmentlogger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.inject.name.Names.named;
import static java.util.concurrent.TimeUnit.MILLISECONDS;


import com.intuit.wasabi.assignment.AssignmentsModule;
import com.intuit.wasabi.assignmentlogger.impl.AssignmentFileLogger;
import com.intuit.wasabi.assignmentlogger.impl.LoggerIngestionExecutor;
import static com.intuit.wasabi.assignmentlogger.AssignmentLoggerAnnotations.LOGGER_EXECUTOR;
import static com.intuit.wasabi.assignmentlogger.AssignmentLoggerAnnotations.LOGGER_ASSIGNMENT_FILE;

import static com.intuit.wasabi.assignmentlogger.AssignmentLoggerAnnotations.LOGGER_FILE_PATH;
import static com.intuit.wasabi.assignmentlogger.AssignmentLoggerAnnotations.LOGGER_FILE_NAME;

import static com.intuit.wasabi.assignmentlogger.AssignmentConstants.W_A_FILE_NAME;
import static com.intuit.wasabi.assignmentlogger.AssignmentConstants.W_A_FILE_PATH;

import java.util.Properties;
import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.autumn.utils.PropertyFactory.getProperty;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by tislam1 on 6/21/16.
 */
public class AssignmentLoggerModule extends AssignmentsModule {

  public static final String PROPERTY_NAME = "/logger.properties";
  private static final Logger LOGGER = getLogger(LoggerIngestionExecutor.class);

    @Override
    protected void configure() {
        super.configure();


        Properties properties = create(PROPERTY_NAME, AssignmentLoggerModule.class);

        // binding the threadpool executor to logger ingestion executer threadpool
        bindMyIngestionThreadPool();

        //bind the assignent logger interface to the assignment file logger
        bind(AssignmentLogger.class).annotatedWith(named(LOGGER_ASSIGNMENT_FILE)).to(AssignmentFileLogger.class);


        LOGGER.debug("Evn path  => {}", System.getenv(W_A_FILE_PATH));
        LOGGER.debug("Evn name  => {}", System.getenv(W_A_FILE_NAME));

        String fileName = System.getenv(W_A_FILE_NAME) != null ? System.getenv(W_A_FILE_NAME) :  getProperty(LOGGER_FILE_NAME, properties, "");
        String filePath = System.getenv(W_A_FILE_PATH) != null ? System.getenv(W_A_FILE_PATH) :  getProperty(LOGGER_FILE_PATH, properties, "");

        bind(String.class).annotatedWith(named(LOGGER_FILE_PATH))
                .toInstance(filePath);


        LOGGER.debug("File Path  => {}", filePath);


        bind(String.class).annotatedWith(named(LOGGER_FILE_NAME))
                .toInstance(fileName);


        LOGGER.debug("File Name  => {}", fileName);


        // adding the logger ingestion executer class to the mapBinder
        mapBinder.addBinding(LoggerIngestionExecutor.NAME).to(LoggerIngestionExecutor.class);
    }

    private void bindMyIngestionThreadPool() {
        // creating an in-memory queue
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

        // creating a thread pool of size 5
        int loggerThreadPoolSize = 5;
        ThreadPoolExecutor loggerThreadPoolExecutor = new ThreadPoolExecutor(loggerThreadPoolSize,
                loggerThreadPoolSize, 0L, MILLISECONDS, queue, new ThreadFactoryBuilder()
                .setNameFormat("LoggerIngestion-%d")
                .setDaemon(true)
                .build());
        bind(ThreadPoolExecutor.class).annotatedWith(named(LOGGER_EXECUTOR)).toInstance(loggerThreadPoolExecutor);
    }
}
