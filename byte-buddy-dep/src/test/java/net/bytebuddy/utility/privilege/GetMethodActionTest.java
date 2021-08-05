package net.bytebuddy.utility.privilege;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class GetMethodActionTest {

    private static final String FOO = "foo";

    @Test
    public void testTypeAndMethodExist() throws Exception {
        assertThat(new GetMethodAction(Object.class.getName(), "toString").run(), is(Object.class.getMethod("toString")));
    }

    @Test
    public void testTypeDoesNotExist() throws Exception {
        assertThat(new GetMethodAction("net.bytebuddy.inexistent.Type", "toString").run(), nullValue(Method.class));
    }

    @Test
    public void testMethodDoesNotExist() throws Exception {
        assertThat(new GetMethodAction(Object.class.getName(), "other").run(), nullValue(Method.class));
    }
}
