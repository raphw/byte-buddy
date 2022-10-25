package net.bytebuddy;

import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.Closeable;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ByteBuddySubclassDeadlockTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @JavaVersionRule.Enforce(value = 8, target = Tester.class)
    public void testDeadlock() throws Exception {
        List<URL> urls = new ArrayList<URL>();
        for (String path : System.getProperty("java.class.path").split(File.pathSeparator, -1)) {
            urls.add(new File(path).toURI().toURL());
        }
        ClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]));
        try {
            ((Runnable) classLoader.loadClass(Tester.class.getName()).getConstructor().newInstance()).run();
        } finally {
            if (classLoader instanceof Closeable) {
                ((Closeable) classLoader).close();
            }
        }
    }

    public static class Foo {
        /* empty */
    }

    public interface Bar {
        /* empty */
    }

    public static class Tester implements Runnable {

        public void run() {
            Semaphore semaphore = new Semaphore(0);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<?> foo = executor.submit(new Subclassing(semaphore, Foo.class));
                Future<?> bar = executor.submit(new Subclassing(semaphore, Bar.class));
                semaphore.release(2);
                foo.get();
                bar.get();
            } catch (Exception exception) {
                throw new AssertionError(exception);
            } finally {
                executor.shutdown();
            }
        }
    }

    private static class Subclassing implements Runnable {

        private final Semaphore semaphore;

        private final Class<?> type;

        private Subclassing(Semaphore semaphore, Class<?> type) {
            this.semaphore = semaphore;
            this.type = type;
        }

        public void run() {
            try {
                semaphore.acquire();
            } catch (InterruptedException exception) {
                throw new AssertionError(exception);
            }
            new ByteBuddy().subclass(type);
        }
    }
}
