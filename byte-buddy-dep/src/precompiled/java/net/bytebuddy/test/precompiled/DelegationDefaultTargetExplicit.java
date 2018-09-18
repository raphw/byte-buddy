package net.bytebuddy.test.precompiled;

import net.bytebuddy.implementation.bind.annotation.Default;

import java.io.Serializable;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class DelegationDefaultTargetExplicit {

    private static final String FOO = "foo", BAR = "bar";

    public static String intercept(@Default(proxyType = DelegationDefaultInterface.class) Object proxy) throws Exception {
        assertThat(proxy, not(instanceOf(Serializable.class)));
        return DelegationDefaultInterface.class.getDeclaredMethod(FOO).invoke(proxy) + BAR;
    }
}
