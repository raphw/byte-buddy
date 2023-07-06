package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.JavaType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class ResettableClassFileTransformerSubstitutableTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Mock
    private ResettableClassFileTransformer classFileTransformer, other;

    private byte[] first = new byte[]{0}, second = new byte[]{1};

    @Test
    public void testTransformLegacy() throws Exception {
        when(classFileTransformer.transform(Foo.class.getClassLoader(),
            FOO,
            Foo.class,
            Foo.class.getProtectionDomain(),
            first)).thenReturn(second);
        assertThat(ResettableClassFileTransformer.WithDelegation.Substitutable.of(classFileTransformer).transform(Foo.class.getClassLoader(),
            FOO,
            Foo.class,
            Foo.class.getProtectionDomain(),
            first), is(second));
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testTransformModern() throws Exception {
        Method transform = ClassFileTransformer.class.getMethod("transform",
                JavaType.MODULE.load(),
                ClassLoader.class,
                String.class,
                Class.class,
                ProtectionDomain.class,
                byte[].class);
        Object module = JavaModule.ofType(Foo.class).unwrap();
        when(transform.invoke(classFileTransformer,
                module,
                Foo.class.getClassLoader(),
                FOO,
                Foo.class,
                Foo.class.getProtectionDomain(),
                first)).thenReturn(second);
        assertThat(transform.invoke(ResettableClassFileTransformer.WithDelegation.Substitutable.of(classFileTransformer),
                module,
                Foo.class.getClassLoader(),
                FOO,
                Foo.class,
                Foo.class.getProtectionDomain(),
                first), is((Object) second));
    }

    @Test
    public void testReplace() throws Exception {
        when(classFileTransformer.transform(Foo.class.getClassLoader(),
                FOO,
                Foo.class,
                Foo.class.getProtectionDomain(),
                first)).thenReturn(second);
        ResettableClassFileTransformer.Substitutable substitutable = ResettableClassFileTransformer.WithDelegation.Substitutable.of(other);
        substitutable.substitute(classFileTransformer);
        assertThat(substitutable.transform(Foo.class.getClassLoader(),
                FOO,
                Foo.class,
                Foo.class.getProtectionDomain(),
                first), is(second));

    }

    private static class Foo {
        /* empty */
    }
}