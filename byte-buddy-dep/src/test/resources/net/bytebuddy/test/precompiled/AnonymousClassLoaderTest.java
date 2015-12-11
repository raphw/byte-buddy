package net.bytebuddy.test.precompiled;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.FixedValue;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AnonymousClassLoaderTest implements Runnable {

    private static final String FOO = "foo", BAR = "bar";

    @Override
    public void run() {
        Instrumentation instrumentation = ByteBuddyAgent.install();
        Callable<String> lambda = () -> FOO;
        ClassReloadingStrategy classReloadingStrategy = ClassReloadingStrategy.of(instrumentation).preregistered(lambda.getClass());
        ClassFileLocator classFileLocator = ClassFileLocator.AgentBased.of(instrumentation, lambda.getClass());
        try {

            try {
                assertThat(lambda.call(), is(FOO));
                new ByteBuddy()
                        .redefine(lambda.getClass(), classFileLocator)
                        .method(named("call"))
                        .intercept(FixedValue.value(BAR))
                        .make()
                        .load(lambda.getClass().getClassLoader(), classReloadingStrategy);
                assertThat(lambda.call(), is(BAR));
            } finally {
                classReloadingStrategy.reset(classFileLocator, lambda.getClass());
            }
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
