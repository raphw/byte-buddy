package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.JavaType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.stubbing.Answer;
import org.objectweb.asm.Type;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResettableClassFileTransformerWithDelegationWithCallbackTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Mock
    private ResettableClassFileTransformer classFileTransformer;

    @Mock
    private ResettableClassFileTransformer.WithDelegation.Callback<Object> callback;

    @Mock
    private Object value;

    @Test
    public void testCallbackNoModules() throws Exception {
        when(classFileTransformer.transform(
                Foo.class.getClassLoader(),
                Type.getInternalName(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain(),
                new byte[]{1, 2, 3})).thenAnswer(new Answer<byte[]>() { // Avoids Java 6 bug.
            @Override
            public byte[] answer(InvocationOnMock invocationOnMock) {
                return new byte[]{4, 5, 6};
            }
        });
        when(callback.onBeforeTransform(null,
                Foo.class.getClassLoader(),
                Type.getInternalName(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain(),
                new byte[]{1, 2, 3})).thenReturn(value);
        assertThat(ResettableClassFileTransformer.WithDelegation.of(classFileTransformer, callback).transform(
                Foo.class.getClassLoader(),
                Type.getInternalName(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain(),
                new byte[]{1, 2, 3}), is(new byte[]{4, 5, 6}));
        verify(callback).onBeforeTransform(null,
                Foo.class.getClassLoader(),
                Type.getInternalName(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain(),
                new byte[]{1, 2, 3});
        verify(callback).onAfterTransform(value,
                null,
                Foo.class.getClassLoader(),
                Type.getInternalName(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain(),
                new byte[]{1, 2, 3});
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testCallbackWithModules() throws Exception {
        Method transform = ClassFileTransformer.class.getMethod("transform",
                JavaType.MODULE.load(),
                ClassLoader.class,
                String.class,
                Class.class,
                ProtectionDomain.class,
                byte[].class);
        when(transform.invoke(classFileTransformer,
                JavaModule.ofType(Foo.class).unwrap(),
                Foo.class.getClassLoader(),
                Type.getInternalName(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain(),
                new byte[]{1, 2, 3})).thenReturn(new byte[]{4, 5, 6});
        when(callback.onBeforeTransform(JavaModule.ofType(Foo.class),
                Foo.class.getClassLoader(),
                Type.getInternalName(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain(),
                new byte[]{1, 2, 3})).thenReturn(value);
        assertThat(transform.invoke(ResettableClassFileTransformer.WithDelegation.of(classFileTransformer, callback),
                JavaModule.ofType(Foo.class).unwrap(),
                Foo.class.getClassLoader(),
                Type.getInternalName(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain(),
                new byte[]{1, 2, 3}), is((Object) new byte[]{4, 5, 6}));
        verify(callback).onBeforeTransform(JavaModule.ofType(Foo.class),
                Foo.class.getClassLoader(),
                Type.getInternalName(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain(),
                new byte[]{1, 2, 3});
        verify(callback).onAfterTransform(value,
                JavaModule.ofType(Foo.class),
                Foo.class.getClassLoader(),
                Type.getInternalName(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain(),
                new byte[]{1, 2, 3});
    }

    private static class Foo {
        /* empty */
    }
}
