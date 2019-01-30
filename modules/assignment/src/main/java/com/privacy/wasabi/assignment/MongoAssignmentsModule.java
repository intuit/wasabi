package com.privacy.wasabi.assignment;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.intuit.wasabi.assignment.AssignmentsModule;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.inject.name.Names.named;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Created by tislam1 on 6/21/16.
 */
public class MongoAssignmentsModule extends AssignmentsModule {

    @Override
    protected void configure() {
        super.configure();

        // bind the threadpool executor to your ingestor's threadpool
        bindMyIngestionThreadPool();

        // add your IngestionExecutor's class to the mapBinder
        mapBinder.addBinding(MongoAssignmentIngestionExecutor.NAME).to(MongoAssignmentIngestionExecutor.class);
    }

    private void bindMyIngestionThreadPool() {
        // create an in-memory queue
        LinkedBlockingQueue<Runnable> myQueue = new LinkedBlockingQueue<>();
        // set your threadpool size
        int myThreadPoolSize = 5;
        ThreadPoolExecutor myThreadPoolExecutor = new ThreadPoolExecutor(myThreadPoolSize,
                myThreadPoolSize, 0L, MILLISECONDS, myQueue, new ThreadFactoryBuilder()
                .setNameFormat("MongoAssignmentIngestion-%d")
                .setDaemon(true)
                .build());
        bind(ThreadPoolExecutor.class).annotatedWith(named("my.assignmentThreadPoolExecutor")).toInstance(myThreadPoolExecutor);
    }
}
