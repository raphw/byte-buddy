package net.bytebuddy.test.precompiled;

import net.bytebuddy.implementation.bind.annotation.Default;

import java.io.Serializable;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

public class DelegationDefaultTargetSerializable {

    private static final String BAR = "bar";

    public static String intercept(@Default(serializableProxy = true) DelegationDefaultInterface proxy) {
        assertThat(proxy, instanceOf(Serializable.class));
        return proxy.foo() + BAR;
    }
}
