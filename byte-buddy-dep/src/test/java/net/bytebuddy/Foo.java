package net.bytebuddy;

import net.bytebuddy.implementation.StubMethod;
import org.elasticsearch.client.Client;
import org.junit.Test;

import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.any;

public class Foo {

    @Test
    public void testFoo() throws Exception {
        new ByteBuddy().subclass(Client.class).method(any()).intercept(StubMethod.INSTANCE).make();
    }

    @Test
    public void testBar() throws Exception {
        new ByteBuddy().subclass(Qux.class).method(any()).intercept(StubMethod.INSTANCE).make();
    }

    interface Bar<T> {

        <S> void bar(Callable<S> arg);
    }

    interface Qux extends Bar<Void> {

    }
}
