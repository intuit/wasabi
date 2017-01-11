package com.intuit.wasabi.repository.cassandra;

import com.datastax.driver.core.exceptions.DriverException;
import com.datastax.driver.core.exceptions.DriverInternalError;
import com.datastax.driver.mapping.Result;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by nbarge on 1/4/17.
 */
public class UninterruptibleUtil {

    public static <T> Result<T> getUninterruptibly(ListenableFuture<Result<T>> aFuture, long timeout, TimeUnit unit) throws TimeoutException {
        try {
            return Uninterruptibles.getUninterruptibly(aFuture, timeout, unit);
        } catch (ExecutionException e) {
            throw propagateCause(e);
        }
    }

    public static <T> Result<T> getUninterruptibly(ListenableFuture<Result<T>> aFuture)  {
        try {
            return Uninterruptibles.getUninterruptibly(aFuture);
        } catch (ExecutionException e) {
            throw propagateCause(e);
        }
    }

    static RuntimeException propagateCause(ExecutionException e) {
        Throwable cause = e.getCause();

        if (cause instanceof Error)
            throw ((Error) cause);

        // We could just rethrow e.getCause(). However, the cause of the ExecutionException has likely been
        // created on the I/O thread receiving the response. Which means that the stacktrace associated
        // with said cause will make no mention of the current thread. This is painful for say, finding
        // out which execute() statement actually raised the exception. So instead, we re-create the
        // exception.
        if (cause instanceof DriverException)
            throw ((DriverException) cause).copy();
        else
            throw new DriverInternalError("Unexpected exception thrown", cause);
    }

}
