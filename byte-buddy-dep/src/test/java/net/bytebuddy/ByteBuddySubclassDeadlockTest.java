package net.bytebuddy;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

public class ByteBuddySubclassDeadlockTest {

    public static class C {}

    public interface I {}

    @Test
    public void test() throws Exception {
        final Semaphore s = new Semaphore(0);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> t1 =
                executor.submit(
                        new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                s.acquire();
                                new ByteBuddy().subclass(C.class);
                                return null;
                            }
                        });
        Future<?> t2 =
                executor.submit(
                        new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                s.acquire();
                                new ByteBuddy().subclass(I.class);
                                return null;
                            }
                        });
        s.release(2);
        t1.get();
        t2.get();
        executor.shutdown();
    }
}
