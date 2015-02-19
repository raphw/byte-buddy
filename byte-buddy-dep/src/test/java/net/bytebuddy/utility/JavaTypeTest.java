package net.bytebuddy.utility;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class JavaTypeTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule(7);

    @Test
    public void testIsAssignableTo() throws Exception {
        assertThat(JavaType.METHOD_HANDLE.isAssignableTo(new TypeDescription.ForLoadedType(Object.class)), is(true));
        assertThat(JavaType.METHOD_TYPE.isAssignableTo(new TypeDescription.ForLoadedType(Object.class)), is(true));
        assertThat(JavaType.METHOD_TYPES_LOOKUP.isAssignableTo(new TypeDescription.ForLoadedType(Object.class)), is(true));
        assertThat(JavaType.CALL_SITE.isAssignableTo(new TypeDescription.ForLoadedType(Object.class)), is(true));
    }

    @Test
    public void testIsAssignableToSerializable() throws Exception {
        assertThat(JavaType.METHOD_HANDLE.isAssignableTo(new TypeDescription.ForLoadedType(Serializable.class)), is(false));
        assertThat(JavaType.METHOD_TYPE.isAssignableTo(new TypeDescription.ForLoadedType(Serializable.class)), is(true));
        assertThat(JavaType.METHOD_TYPES_LOOKUP.isAssignableTo(new TypeDescription.ForLoadedType(Serializable.class)), is(false));
        assertThat(JavaType.CALL_SITE.isAssignableTo(new TypeDescription.ForLoadedType(Serializable.class)), is(false));
    }

    @Test
    public void testIsAssignableFrom() throws Exception {
        assertThat(JavaType.METHOD_HANDLE.isAssignableFrom(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(JavaType.METHOD_TYPE.isAssignableFrom(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(JavaType.METHOD_TYPES_LOOKUP.isAssignableFrom(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(JavaType.CALL_SITE.isAssignableFrom(new TypeDescription.ForLoadedType(Object.class)), is(false));
    }

    @Test
    public void testRepresents() throws Exception {
        assertThat(JavaType.METHOD_HANDLE.representedBy(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(JavaType.METHOD_TYPE.representedBy(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(JavaType.METHOD_TYPES_LOOKUP.representedBy(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(JavaType.CALL_SITE.representedBy(new TypeDescription.ForLoadedType(Object.class)), is(false));
    }

    @Test
    @JavaVersionRule.Enforce
    public void testLoading() throws Exception {
        JavaType.METHOD_HANDLE.load();
        JavaType.METHOD_TYPE.load();
        JavaType.METHOD_TYPES_LOOKUP.load();
        JavaType.CALL_SITE.load();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(JavaType.TypeLookup.ForNamedType.class).apply();
        final Iterator<Class<?>> iterator = Arrays.<Class<?>>asList(Object.class, String.class).iterator();
        ObjectPropertyAssertion.of(JavaType.TypeLookup.ForLoadedType.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return iterator.next();
            }
        }).apply();
    }
}
