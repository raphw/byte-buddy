package com.blogspot.mydailyjava.bytebuddy.utility;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.blogspot.mydailyjava.bytebuddy.utility.ByteBuddyCommons.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;

public class ByteBuddyCommonsTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", FOOBAR = "foo.bar";

    @Test
    public void testNonNull() throws Exception {
        Object object = new Object();
        assertThat(nonNull(object), sameInstance(object));
    }

    @Test(expected = NullPointerException.class)
    public void testNonNullThrowsException() throws Exception {
        nonNull(null);
    }

    @Test
    public void testIsInterface() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(Runnable.class);
        assertThat(isInterface(typeDescription), is(typeDescription));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsInterfaceThrowsException() throws Exception {
        isInterface(new TypeDescription.ForLoadedType(Object.class));
    }

    @Test
    public void testClassIsImplementable() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(Object.class);
        assertThat(isImplementable(typeDescription), is(typeDescription));
    }

    @Test
    public void testInterfaceIsImplementable() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(Runnable.class);
        assertThat(isImplementable(typeDescription), is(typeDescription));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrimitiveIsImplementableThrowsException() throws Exception {
        isImplementable(new TypeDescription.ForLoadedType(int.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testArrayIsImplementableThrowsException() throws Exception {
        isImplementable(new TypeDescription.ForLoadedType(Object[].class));
    }

    @Test
    public void testJoin() throws Exception {
        assertThat(join(Arrays.asList(FOO, BAR), QUX), is(Arrays.asList(FOO, BAR, QUX)));
    }

    @Test
    public void testIsValidIdentifier() throws Exception {
        assertThat(isValidIdentifier(FOO), is(FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsValidIdentifierThrowsException() throws Exception {
        isValidIdentifier(MethodDescription.CONSTRUCTOR_INTERNAL_NAME);
    }

    @Test
    public void testIsValidJavaTypeName() throws Exception {
        assertThat(isValidTypeName(FOOBAR), is(FOOBAR));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsValidJavaTypeNameThrowsException() throws Exception {
        assertThat(isValidTypeName("." + FOO), is(FOOBAR));
    }

    @Test
    public void testIsNotEmpty() throws Exception {
        List<String> list = Arrays.asList(FOO);
        assertThat(isNotEmpty(list, FOO), sameInstance(list));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsNotEmptyThrowsException() throws Exception {
        isNotEmpty(Arrays.<Object>asList(), FOO);
    }
}
