package net.bytebuddy.test.precompiled;

import net.bytebuddy.implementation.bind.annotation.Default;

import java.io.Serializable;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class DelegationDefaultTarget {

    private static final String BAR = "bar";

    public static String intercept(@Default DelegationDefaultInterface proxy) {
        assertThat(proxy, not(instanceOf(Serializable.class)));
        return proxy.foo() + BAR;
    }
}
