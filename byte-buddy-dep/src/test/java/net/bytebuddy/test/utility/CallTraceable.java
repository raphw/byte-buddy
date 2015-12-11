package net.bytebuddy.test.utility;


import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CallTraceable {

    protected final List<MethodCall> methodCalls;

    public CallTraceable() {
        methodCalls = new ArrayList<MethodCall>();
    }

    public void register(String name, Object... arguments) {
        methodCalls.add(new MethodCall(name, arguments));
    }

    public void assertOnlyCall(String name, Object... arguments) {
        assertThat(methodCalls.size(), is(1));
        assertThat(methodCalls.get(0).name, is(name));
        assertThat(methodCalls.get(0).arguments, equalTo(arguments));
    }

    public void assertZeroCalls() {
        assertThat(methodCalls.size(), is(0));
    }

    public void reset() {
        methodCalls.clear();
    }

    protected static class MethodCall {

        public final String name;

        public final Object arguments[];

        public MethodCall(String name, Object... arguments) {
            this.name = name;
            this.arguments = arguments;
        }
    }
}
