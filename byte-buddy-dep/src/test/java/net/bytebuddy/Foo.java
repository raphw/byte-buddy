package net.bytebuddy;

import net.bytebuddy.agent.ByteBuddyAgent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Foo {

    public static void main(String[] args) {
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        ByteBuddyAgent.install().addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(final ClassLoader loader,
                                    String className,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {
                if (!className.startsWith("net/bytebuddy/")) {
                    return null;
                }
                System.out.println("Instrument start: " + className);
                if (className.equals("net/bytebuddy/Foo$SubType")) {
                    final boolean holdsLock = Thread.holdsLock(loader);
                    Future<Class<?>> future = executor.submit(new Callable<Class<?>>() {
                        @Override
                        public Class<?> call() throws Exception {
                            if (holdsLock) {
                                synchronized (loader) {
                                    try {
                                        return Class.forName("net.bytebuddy.Foo$SuperType", false, loader);
                                    } finally {
                                        loader.notifyAll();
                                    }
                                }
                            } else {
                                return Class.forName("net.bytebuddy.Foo$SuperType", false, loader);
                            }
                        }
                    });
                    try {
                        while (holdsLock && !future.isDone()) {
                            loader.wait();
                        }
                        System.out.println("Loaded: " + future.get());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("Instrument end: " + className);
                return null;
            }
        });

        new SubType();

        executor.shutdown();
    }

    static class SuperType {

    }

    static class SubType extends SuperType {

    }
}
