/*******************************************************************************

 *******************************************************************************/
package com.intuit.wasabi.assignmentlogger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.inject.name.Names.named;
import static java.util.concurrent.TimeUnit.MILLISECONDS;


import com.intuit.wasabi.assignment.AssignmentsModule;
import static com.intuit.wasabi.assignmentlogger.AssignmentLoggerAnnotations.LOGGER_EXECUTOR;

/**
 * Created by tislam1 on 6/21/16.
 */
public class AssignmentLoggerModule extends AssignmentsModule {

    @Override
    protected void configure() {
        super.configure();

        // binding the threadpool executor to logger ingestion executer threadpool
        bindMyIngestionThreadPool();

        // adding the logger ingestion executer class to the mapBinder
        mapBinder.addBinding(LoggerIngestionExecutor.NAME).to(LoggerIngestionExecutor.class);
    }

    private void bindMyIngestionThreadPool() {
        // creating an in-memory queue
        LinkedBlockingQueue<Runnable> myQueue = new LinkedBlockingQueue<>();

        // creating a thread pool of size 5
        int loggerThreadPoolSize = 5;
        ThreadPoolExecutor loggerThreadPoolExecutor = new ThreadPoolExecutor(loggerThreadPoolSize,
                loggerThreadPoolSize, 0L, MILLISECONDS, myQueue, new ThreadFactoryBuilder()
                .setNameFormat("LoggerIngestion-%d")
                .setDaemon(true)
                .build());
        bind(ThreadPoolExecutor.class).annotatedWith(named(LOGGER_EXECUTOR)).toInstance(loggerThreadPoolExecutor);
    }
}
