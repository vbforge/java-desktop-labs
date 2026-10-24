package com.vbforge.slidecraft.engine;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Starts the JavaFX toolkit for tests that need real rendering. If no display (or Monocle, see the
 * {@code headless-fx} Maven profile) is available, {@link #available()} is false and callers skip.
 */
final class FxTestSupport {

    private static Boolean available;

    private FxTestSupport() {
    }

    static synchronized boolean available() {
        if (available == null) {
            try {
                CountDownLatch started = new CountDownLatch(1);
                Platform.startup(started::countDown);
                available = started.await(15, TimeUnit.SECONDS);
            } catch (IllegalStateException alreadyRunning) {
                available = true;
            } catch (Throwable noToolkit) {
                available = false;
            }
        }
        return available;
    }

    static <T> T onFxThread(Callable<T> task) throws Exception {
        FutureTask<T> future = new FutureTask<>(task);
        Platform.runLater(future);
        return future.get(60, TimeUnit.SECONDS);
    }
}
