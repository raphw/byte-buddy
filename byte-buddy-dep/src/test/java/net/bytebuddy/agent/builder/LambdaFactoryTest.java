package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LambdaFactoryTest {

    private static final String FOO = "foo";

    private static final byte[] BAR = new byte[]{1, 2, 3};

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Object a1, a3, a4, a5, a6;

    @Mock
    private List<?> a8, a9;

    @Mock
    private ClassFileTransformer classFileTransformer, otherTransformer;

    @Test
    public void testValidFactory() throws Exception {
        PseudoFactory pseudoFactory = new PseudoFactory();
        assertThat(LambdaFactory.register(classFileTransformer, pseudoFactory, AgentBuilder.LambdaInstrumentationStrategy.ENABLED), is(true));
        try {
            assertThat(AgentBuilder.LambdaInstrumentationStrategy.ENABLED.call()
                    .getDeclaredMethod("make", Object.class, String.class, Object.class, Object.class, Object.class, Object.class, boolean.class, List.class, List.class)
                    .invoke(null, a1, FOO, a3, a4, a5, a6, true, a8, a9), is((Object) BAR));
            assertThat(pseudoFactory.args[0], is(a1));
            assertThat(pseudoFactory.args[1], is((Object) FOO));
            assertThat(pseudoFactory.args[2], is(a3));
            assertThat(pseudoFactory.args[3], is(a4));
            assertThat(pseudoFactory.args[4], is(a5));
            assertThat(pseudoFactory.args[5], is(a6));
            assertThat(pseudoFactory.args[6], is((Object) true));
            assertThat(pseudoFactory.args[7], is((Object) a8));
            assertThat(pseudoFactory.args[8], is((Object) a9));
            assertThat(pseudoFactory.args[9], is((Object) Collections.singleton(classFileTransformer)));
        } finally {
            assertThat(LambdaFactory.release(classFileTransformer), is(true));
        }
    }

    @Test
    public void testUnknownTransformer() throws Exception {
        assertThat(LambdaFactory.release(classFileTransformer), is(false));
    }

    @Test
    public void testPreviousTransformer() throws Exception {
        PseudoFactory pseudoFactory = new PseudoFactory();
        try {
            assertThat(LambdaFactory.register(classFileTransformer, pseudoFactory, AgentBuilder.LambdaInstrumentationStrategy.ENABLED), is(true));
            assertThat(LambdaFactory.register(otherTransformer, pseudoFactory, AgentBuilder.LambdaInstrumentationStrategy.ENABLED), is(false));
        } finally {
            assertThat(LambdaFactory.release(classFileTransformer), is(false));
            assertThat(LambdaFactory.release(otherTransformer), is(true));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalTransformer() throws Exception {
        LambdaFactory.register(classFileTransformer, new Object(), AgentBuilder.LambdaInstrumentationStrategy.ENABLED);
    }

    @Test
    public void testTypeAndMethodArePublic() throws Exception {
        assertThat(Modifier.isPublic(LambdaFactory.class.getModifiers()), is(true));
        assertThat(Modifier.isPublic(LambdaFactory.class.getDeclaredMethod("make",
                Object.class,
                String.class,
                Object.class,
                Object.class,
                Object.class,
                Object.class,
                boolean.class,
                List.class,
                List.class).getModifiers()), is(true));
        assertThat(Modifier.isStatic(LambdaFactory.class.getDeclaredMethod("make",
                Object.class,
                String.class,
                Object.class,
                Object.class,
                Object.class,
                Object.class,
                boolean.class,
                List.class,
                List.class).getModifiers()), is(true));

    }

    @Test
    public void testObjectProperties() throws Exception {
        final Iterator<Method> methods = Arrays.asList(Object.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(LambdaFactory.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return methods.next();
            }
        }).apply();
    }

    public static class PseudoFactory {

        private Object args[];

        public byte[] make(Object a1, String a2, Object a3, Object a4, Object a5, Object a6, boolean a7, List<?> a8, List<?> a9, Collection<?> a10) {
            args = new Object[]{a1, a2, a3, a4, a5, a6, a7, a8, a9, a10};
            return BAR;
        }
    }
}