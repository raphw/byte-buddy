package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass;

import com.blogspot.mydailyjava.bytebuddy.ClassFormatVersion;
import com.blogspot.mydailyjava.bytebuddy.NamingStrategy;
import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapper;
import com.blogspot.mydailyjava.bytebuddy.dynamic.ClassLoadingStrategy;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.FieldRegistry;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.MethodRegistry;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.util.Arrays;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.none;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class LoadedSuperclassDynamicTypeBuilderTest {

    private static final String FOO = "foo";

    @Test
    public void testPlainSubclass() throws Exception {
        Class<?> loaded = new LoadedSuperclassDynamicTypeBuilder<Object>(ClassFormatVersion.forCurrentJavaVersion(),
                new NamingStrategy.Fixed(FOO),
                Object.class,
                Arrays.<Class<?>>asList(Serializable.class),
                Opcodes.ACC_PUBLIC,
                TypeAttributeAppender.NoOp.INSTANCE,
                none(),
                new ClassVisitorWrapper.Chain(),
                new FieldRegistry.Default(),
                new MethodRegistry.Default(),
                FieldAttributeAppender.NoOp.INSTANCE,
                MethodAttributeAppender.NoOp.INSTANCE,
                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(loaded.getName(), is(FOO));
        assertEquals(Object.class, loaded.getSuperclass());
        assertThat(loaded.getInterfaces().length, is(1));
        assertEquals(Serializable.class, loaded.getInterfaces()[0]);
        assertThat(loaded.getDeclaredMethods().length, is(0));
        assertThat(loaded.getDeclaredFields().length, is(0));
        assertThat(loaded.getDeclaredConstructors().length, is(1));
    }
}
