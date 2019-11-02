package com.intuit.wasabi.assignmentwriter;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import static com.google.inject.name.Names.named;

import com.intuit.wasabi.assignment.AssignmentsModule;

public final class AssignmentWriterModule extends AssignmentsModule implements AssignmentWriterAnnotations {

    private final static String DEFAULT_ASSIGNMENTS_FILE_PATH = "./";
    private final static String DEFAULT_ASSIGNMENTS_FILE_NAME = "assignments.json";

    @Override
    protected void configure() {
        super.configure();

        // bind the threadpool executor to assignments writer executor
        bindMyIngestionThreadPool();

        // bind the assignments file writer to the JSON file writer
        bind(AssignmentsFileWriter.class)
            .annotatedWith(named(AssignmentWriterAnnotations.ASSIGNMENTS_FILE_WRITER))
            .to(AssignmentsJSONFileWriter.class);

        String filePath = (System.getenv("WASABI_ASSIGNMENT_FILE_PATH") != null)
            ? System.getenv("WASABI_ASSIGNMENT_FILE_PATH")
            : DEFAULT_ASSIGNMENTS_FILE_PATH;

        String fileName = (System.getenv("WASABI_ASSIGNMENT_FILE_NAME") != null)
            ? System.getenv("WASABI_ASSIGNMENT_FILE_NAME")
            : DEFAULT_ASSIGNMENTS_FILE_NAME;

        String fullPath = filePath + fileName;
        bind(String.class).annotatedWith(named(AssignmentWriterAnnotations.ASSIGNMENTS_FILE_PATH)).toInstance(fullPath);

        // add your IngestionExecutor's class to the mapBinder
        mapBinder.addBinding(AssignmentsWriterIngestionExecutor.NAME).to(AssignmentsWriterIngestionExecutor.class);
    }

    private void bindMyIngestionThreadPool() {
        // create an in-memory queue
        LinkedBlockingQueue<Runnable> myQueue = new LinkedBlockingQueue<>();
        // set your threadpool size
        int myThreadPoolSize = 5;
        ThreadPoolExecutor myThreadPoolExecutor = new ThreadPoolExecutor(myThreadPoolSize,
            myThreadPoolSize, 0L, TimeUnit.MILLISECONDS, myQueue, new ThreadFactoryBuilder()
            .setNameFormat("AssignmentWriter-%d")
            .setDaemon(true)
            .build());

        bind(ThreadPoolExecutor.class)
            .annotatedWith(named(AssignmentWriterAnnotations.ASSIGNMENTS_WRITER_THREAD_POOL))
            .toInstance(myThreadPoolExecutor);
    }
}
