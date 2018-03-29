package net.bytebuddy.matcher;

import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractElementMatcherTest<T extends ElementMatcher<?>> {

    private final Class<? extends T> type;

    protected final String startsWith;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    protected AbstractElementMatcherTest(Class<? extends T> type, String startsWith) {
        this.type = type;
        this.startsWith = startsWith;
    }

    @Test
    public void testStringRepresentation() throws Exception {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            List<Object> arguments = new ArrayList<Object>();
            for (Class<?> type : constructor.getParameterTypes()) {
                if (type == boolean.class) {
                    arguments.add(false);
                } else if (type == byte.class) {
                    arguments.add((byte) 0);
                } else if (type == short.class) {
                    arguments.add((short) 0);
                } else if (type == char.class) {
                    arguments.add((char) 0);
                } else if (type == int.class) {
                    arguments.add(0);
                } else if (type == long.class) {
                    arguments.add(0L);
                } else if (type == float.class) {
                    arguments.add(0f);
                } else if (type == double.class) {
                    arguments.add(0d);
                } else {
                    arguments.add(null);
                }
            }
            assertThat(constructor.newInstance(arguments.toArray(new Object[arguments.size()])).toString(), startsWith(startsWith));
        }
    }
}
