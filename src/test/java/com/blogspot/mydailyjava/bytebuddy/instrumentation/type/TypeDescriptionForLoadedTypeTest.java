package com.blogspot.mydailyjava.bytebuddy.instrumentation.type;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.isConstructor;
import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.isMethod;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class TypeDescriptionForLoadedTypeTest {

    private TypeDescription objectType;
    private TypeDescription intType;
    private TypeDescription longType;
    private TypeDescription integerType;
    private TypeDescription runnableType;
    private TypeDescription objectArrayType;

    @Before
    public void setUp() throws Exception {
        objectType = new TypeDescription.ForLoadedType(Object.class);
        intType = new TypeDescription.ForLoadedType(int.class);
        longType = new TypeDescription.ForLoadedType(long.class);
        integerType = new TypeDescription.ForLoadedType(Integer.class);
        runnableType = new TypeDescription.ForLoadedType(Runnable.class);
        objectArrayType = new TypeDescription.ForLoadedType(Object[].class);
    }

    @Test
    public void testIsInstance() throws Exception {
        assertThat(objectType.isInstance(new Object()), is(true));
        assertThat(objectType.isInstance(0), is(true));
        assertThat(objectType.isInstance(0L), is(true));
        assertThat(objectType.isInstance(new Object[0]), is(true));

        assertThat(intType.isInstance(new Object()), is(false));
        assertThat(intType.isInstance(0), is(false));
        assertThat(intType.isInstance(0L), is(false));
        assertThat(intType.isInstance(new Object[0]), is(false));

        assertThat(longType.isInstance(new Object()), is(false));
        assertThat(longType.isInstance(0), is(false));
        assertThat(longType.isInstance(0L), is(false));
        assertThat(longType.isInstance(new Object[0]), is(false));

        assertThat(integerType.isInstance(new Object()), is(false));
        assertThat(integerType.isInstance(0), is(true));
        assertThat(integerType.isInstance(0L), is(false));
        assertThat(integerType.isInstance(new Object[0]), is(false));

        assertThat(runnableType.isInstance(new Object()), is(false));
        assertThat(runnableType.isInstance(0), is(false));
        assertThat(runnableType.isInstance(0L), is(false));
        assertThat(runnableType.isInstance(new Object[0]), is(false));

        assertThat(objectArrayType.isInstance(new Object()), is(false));
        assertThat(objectArrayType.isInstance(0), is(false));
        assertThat(objectArrayType.isInstance(0L), is(false));
        assertThat(objectArrayType.isInstance(new Object[0]), is(true));
    }

    @Test
    public void testIsAssignableFromTypeDescription() throws Exception {
        assertThat(objectType.isAssignableFrom(objectType), is(true));
        assertThat(objectType.isAssignableFrom(intType), is(false));
        assertThat(objectType.isAssignableFrom(longType), is(false));
        assertThat(objectType.isAssignableFrom(integerType), is(true));
        assertThat(objectType.isAssignableFrom(runnableType), is(true));
        assertThat(objectType.isAssignableFrom(objectArrayType), is(true));

        assertThat(intType.isAssignableFrom(objectType), is(false));
        assertThat(intType.isAssignableFrom(intType), is(true));
        assertThat(intType.isAssignableFrom(longType), is(false));
        assertThat(intType.isAssignableFrom(integerType), is(false));
        assertThat(intType.isAssignableFrom(runnableType), is(false));
        assertThat(intType.isAssignableFrom(objectArrayType), is(false));

        assertThat(longType.isAssignableFrom(objectType), is(false));
        assertThat(longType.isAssignableFrom(intType), is(false));
        assertThat(longType.isAssignableFrom(longType), is(true));
        assertThat(longType.isAssignableFrom(integerType), is(false));
        assertThat(longType.isAssignableFrom(runnableType), is(false));
        assertThat(longType.isAssignableFrom(objectArrayType), is(false));

        assertThat(integerType.isAssignableFrom(objectType), is(false));
        assertThat(integerType.isAssignableFrom(intType), is(false));
        assertThat(integerType.isAssignableFrom(longType), is(false));
        assertThat(integerType.isAssignableFrom(integerType), is(true));
        assertThat(integerType.isAssignableFrom(runnableType), is(false));
        assertThat(integerType.isAssignableFrom(objectArrayType), is(false));

        assertThat(runnableType.isAssignableFrom(objectType), is(false));
        assertThat(runnableType.isAssignableFrom(intType), is(false));
        assertThat(runnableType.isAssignableFrom(longType), is(false));
        assertThat(runnableType.isAssignableFrom(integerType), is(false));
        assertThat(runnableType.isAssignableFrom(runnableType), is(true));
        assertThat(runnableType.isAssignableFrom(objectArrayType), is(false));
    }

    @Test
    public void testIsAssignableFromType() throws Exception {
        assertThat(objectType.isAssignableFrom(Object.class), is(true));
        assertThat(objectType.isAssignableFrom(int.class), is(false));
        assertThat(objectType.isAssignableFrom(long.class), is(false));
        assertThat(objectType.isAssignableFrom(Integer.class), is(true));
        assertThat(objectType.isAssignableFrom(Runnable.class), is(true));
        assertThat(objectType.isAssignableFrom(Object[].class), is(true));

        assertThat(intType.isAssignableFrom(Object.class), is(false));
        assertThat(intType.isAssignableFrom(int.class), is(true));
        assertThat(intType.isAssignableFrom(long.class), is(false));
        assertThat(intType.isAssignableFrom(Integer.class), is(false));
        assertThat(intType.isAssignableFrom(Runnable.class), is(false));
        assertThat(intType.isAssignableFrom(Object[].class), is(false));

        assertThat(longType.isAssignableFrom(Object.class), is(false));
        assertThat(longType.isAssignableFrom(int.class), is(false));
        assertThat(longType.isAssignableFrom(long.class), is(true));
        assertThat(longType.isAssignableFrom(Integer.class), is(false));
        assertThat(longType.isAssignableFrom(Runnable.class), is(false));
        assertThat(longType.isAssignableFrom(Object[].class), is(false));

        assertThat(integerType.isAssignableFrom(Object.class), is(false));
        assertThat(integerType.isAssignableFrom(int.class), is(false));
        assertThat(integerType.isAssignableFrom(long.class), is(false));
        assertThat(integerType.isAssignableFrom(Integer.class), is(true));
        assertThat(integerType.isAssignableFrom(Runnable.class), is(false));
        assertThat(integerType.isAssignableFrom(Object[].class), is(false));

        assertThat(runnableType.isAssignableFrom(Object.class), is(false));
        assertThat(runnableType.isAssignableFrom(int.class), is(false));
        assertThat(runnableType.isAssignableFrom(long.class), is(false));
        assertThat(runnableType.isAssignableFrom(Integer.class), is(false));
        assertThat(runnableType.isAssignableFrom(Runnable.class), is(true));
        assertThat(runnableType.isAssignableFrom(Object[].class), is(false));

        assertThat(objectArrayType.isAssignableFrom(Object.class), is(false));
        assertThat(objectArrayType.isAssignableFrom(int.class), is(false));
        assertThat(objectArrayType.isAssignableFrom(long.class), is(false));
        assertThat(objectArrayType.isAssignableFrom(Integer.class), is(false));
        assertThat(objectArrayType.isAssignableFrom(Runnable.class), is(false));
        assertThat(objectArrayType.isAssignableFrom(Object[].class), is(true));
    }

    @Test
    public void testIsAssignableTo() throws Exception {
        assertThat(objectType.isAssignableTo(objectType), is(true));
        assertThat(objectType.isAssignableTo(intType), is(false));
        assertThat(objectType.isAssignableTo(longType), is(false));
        assertThat(objectType.isAssignableTo(integerType), is(false));
        assertThat(objectType.isAssignableTo(runnableType), is(false));
        assertThat(objectType.isAssignableTo(objectArrayType), is(false));

        assertThat(intType.isAssignableTo(objectType), is(false));
        assertThat(intType.isAssignableTo(intType), is(true));
        assertThat(intType.isAssignableTo(longType), is(false));
        assertThat(intType.isAssignableTo(integerType), is(false));
        assertThat(intType.isAssignableTo(runnableType), is(false));
        assertThat(intType.isAssignableTo(objectArrayType), is(false));

        assertThat(longType.isAssignableTo(objectType), is(false));
        assertThat(longType.isAssignableTo(intType), is(false));
        assertThat(longType.isAssignableTo(longType), is(true));
        assertThat(longType.isAssignableTo(integerType), is(false));
        assertThat(longType.isAssignableTo(runnableType), is(false));
        assertThat(longType.isAssignableTo(objectArrayType), is(false));

        assertThat(integerType.isAssignableTo(objectType), is(true));
        assertThat(integerType.isAssignableTo(intType), is(false));
        assertThat(integerType.isAssignableTo(longType), is(false));
        assertThat(integerType.isAssignableTo(integerType), is(true));
        assertThat(integerType.isAssignableTo(runnableType), is(false));
        assertThat(integerType.isAssignableTo(objectArrayType), is(false));

        assertThat(runnableType.isAssignableTo(objectType), is(true));
        assertThat(runnableType.isAssignableTo(intType), is(false));
        assertThat(runnableType.isAssignableTo(longType), is(false));
        assertThat(runnableType.isAssignableTo(integerType), is(false));
        assertThat(runnableType.isAssignableTo(runnableType), is(true));
        assertThat(runnableType.isAssignableTo(objectArrayType), is(false));

        assertThat(objectArrayType.isAssignableTo(objectType), is(true));
        assertThat(objectArrayType.isAssignableTo(intType), is(false));
        assertThat(objectArrayType.isAssignableTo(longType), is(false));
        assertThat(objectArrayType.isAssignableTo(integerType), is(false));
        assertThat(objectArrayType.isAssignableTo(runnableType), is(false));
        assertThat(objectArrayType.isAssignableTo(objectArrayType), is(true));
    }

    @Test
    public void testIsAssignableToType() throws Exception {
        assertThat(objectType.isAssignableTo(Object.class), is(true));
        assertThat(objectType.isAssignableTo(int.class), is(false));
        assertThat(objectType.isAssignableTo(long.class), is(false));
        assertThat(objectType.isAssignableTo(Integer.class), is(false));
        assertThat(objectType.isAssignableTo(Runnable.class), is(false));
        assertThat(objectType.isAssignableTo(Object[].class), is(false));

        assertThat(intType.isAssignableTo(Object.class), is(false));
        assertThat(intType.isAssignableTo(int.class), is(true));
        assertThat(intType.isAssignableTo(long.class), is(false));
        assertThat(intType.isAssignableTo(Integer.class), is(false));
        assertThat(intType.isAssignableTo(Runnable.class), is(false));
        assertThat(intType.isAssignableTo(Object[].class), is(false));

        assertThat(longType.isAssignableTo(Object.class), is(false));
        assertThat(longType.isAssignableTo(int.class), is(false));
        assertThat(longType.isAssignableTo(long.class), is(true));
        assertThat(longType.isAssignableTo(Integer.class), is(false));
        assertThat(longType.isAssignableTo(Runnable.class), is(false));
        assertThat(longType.isAssignableTo(Object[].class), is(false));

        assertThat(integerType.isAssignableTo(Object.class), is(true));
        assertThat(integerType.isAssignableTo(int.class), is(false));
        assertThat(integerType.isAssignableTo(long.class), is(false));
        assertThat(integerType.isAssignableTo(Integer.class), is(true));
        assertThat(integerType.isAssignableTo(Runnable.class), is(false));
        assertThat(integerType.isAssignableTo(Object[].class), is(false));

        assertThat(runnableType.isAssignableTo(Object.class), is(true));
        assertThat(runnableType.isAssignableTo(int.class), is(false));
        assertThat(runnableType.isAssignableTo(long.class), is(false));
        assertThat(runnableType.isAssignableTo(Integer.class), is(false));
        assertThat(runnableType.isAssignableTo(Runnable.class), is(true));
        assertThat(runnableType.isAssignableTo(Object[].class), is(false));

        assertThat(objectArrayType.isAssignableTo(Object.class), is(true));
        assertThat(objectArrayType.isAssignableTo(int.class), is(false));
        assertThat(objectArrayType.isAssignableTo(long.class), is(false));
        assertThat(objectArrayType.isAssignableTo(Integer.class), is(false));
        assertThat(objectArrayType.isAssignableTo(Runnable.class), is(false));
        assertThat(objectArrayType.isAssignableTo(Object[].class), is(true));
    }

    @Test
    public void testRepresents() throws Exception {
        assertThat(objectType.represents(Object.class), is(true));
        assertThat(objectType.represents(int.class), is(false));
        assertThat(objectType.represents(long.class), is(false));
        assertThat(objectType.represents(Integer.class), is(false));
        assertThat(objectType.represents(Runnable.class), is(false));
        assertThat(objectType.represents(Object[].class), is(false));

        assertThat(intType.represents(Object.class), is(false));
        assertThat(intType.represents(int.class), is(true));
        assertThat(intType.represents(long.class), is(false));
        assertThat(intType.represents(Integer.class), is(false));
        assertThat(intType.represents(Runnable.class), is(false));
        assertThat(intType.represents(Object[].class), is(false));

        assertThat(longType.represents(Object.class), is(false));
        assertThat(longType.represents(int.class), is(false));
        assertThat(longType.represents(long.class), is(true));
        assertThat(longType.represents(Integer.class), is(false));
        assertThat(longType.represents(Runnable.class), is(false));
        assertThat(longType.represents(Object[].class), is(false));

        assertThat(integerType.represents(Object.class), is(false));
        assertThat(integerType.represents(int.class), is(false));
        assertThat(integerType.represents(long.class), is(false));
        assertThat(integerType.represents(Integer.class), is(true));
        assertThat(integerType.represents(Runnable.class), is(false));
        assertThat(integerType.represents(Object[].class), is(false));

        assertThat(runnableType.represents(Object.class), is(false));
        assertThat(runnableType.represents(int.class), is(false));
        assertThat(runnableType.represents(long.class), is(false));
        assertThat(runnableType.represents(Integer.class), is(false));
        assertThat(runnableType.represents(Runnable.class), is(true));
        assertThat(runnableType.represents(Object[].class), is(false));
    }

    @Test
    public void testIsInterface() throws Exception {
        assertThat(objectType.isInterface(), is(false));
        assertThat(intType.isInterface(), is(false));
        assertThat(longType.isInterface(), is(false));
        assertThat(integerType.isInterface(), is(false));
        assertThat(runnableType.isInterface(), is(true));
        assertThat(objectArrayType.isInterface(), is(false));
    }

    @Test
    public void testIsArray() throws Exception {
        assertThat(objectType.isArray(), is(false));
        assertThat(intType.isArray(), is(false));
        assertThat(longType.isArray(), is(false));
        assertThat(integerType.isArray(), is(false));
        assertThat(runnableType.isArray(), is(false));
        assertThat(objectArrayType.isArray(), is(true));
    }

    @Test
    public void testGetComponentType() throws Exception {
        assertThat(objectType.getComponentType(), nullValue(TypeDescription.class));
        assertThat(intType.getComponentType(), nullValue(TypeDescription.class));
        assertThat(longType.getComponentType(), nullValue(TypeDescription.class));
        assertThat(integerType.getComponentType(), nullValue(TypeDescription.class));
        assertThat(runnableType.getComponentType(), nullValue(TypeDescription.class));
        assertThat(objectArrayType.getComponentType(), equalTo(objectType));
    }

    @Test
    public void testIsPrimitive() throws Exception {
        assertThat(objectType.isPrimitive(), is(false));
        assertThat(intType.isPrimitive(), is(true));
        assertThat(longType.isPrimitive(), is(true));
        assertThat(integerType.isPrimitive(), is(false));
        assertThat(runnableType.isPrimitive(), is(false));
        assertThat(objectArrayType.isPrimitive(), is(false));
    }

    @Test
    public void testIsAnnotation() throws Exception {
        assertThat(objectType.isAnnotation(), is(false));
        assertThat(intType.isAnnotation(), is(false));
        assertThat(longType.isAnnotation(), is(false));
        assertThat(integerType.isAnnotation(), is(false));
        assertThat(runnableType.isAnnotation(), is(false));
        assertThat(objectArrayType.isAnnotation(), is(false));
        assertThat(new TypeDescription.ForLoadedType(SuppressWarnings.class).isAnnotation(), is(true));
    }

    @Test
    public void testGetSupertype() throws Exception {
        assertThat(objectType.getSupertype(), nullValue(TypeDescription.class));
        assertThat(intType.getSupertype(), nullValue(TypeDescription.class));
        assertThat(longType.getSupertype(), nullValue(TypeDescription.class));
        assertThat(integerType.getSupertype(), equalTo((TypeDescription) new TypeDescription.ForLoadedType(Number.class)));
        assertThat(runnableType.getSupertype(), nullValue(TypeDescription.class));
        assertThat(objectArrayType.getSupertype(), equalTo(objectType));
    }

    @SuppressWarnings("unused")
    public static interface Foo {
        /* empty */
    }

    @SuppressWarnings("unused")
    public static class Bar implements Foo {
        /* empty */
    }

    @Test
    public void testGetInterfaces() throws Exception {
        assertThat(objectType.getInterfaces().size(), is(0));
        assertThat(new TypeDescription.ForLoadedType(Bar.class).getInterfaces().size(), is(1));
        assertThat(new TypeDescription.ForLoadedType(Bar.class).getInterfaces().get(0),
                equalTo((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
    }

    @Test
    public void testGetDeclaredMethods() throws Exception {
        assertMethodEquality(objectType, Object.class);
        assertMethodEquality(integerType, Integer.class);
        assertMethodEquality(runnableType, Runnable.class);
        assertMethodEquality(objectArrayType, Object[].class);
    }

    private static void assertMethodEquality(TypeDescription typeDescription, Class<?> type) {
        assertThat(typeDescription.getDeclaredMethods().filter(isMethod()).size(), is(type.getDeclaredMethods().length));
        for (Method method : type.getDeclaredMethods()) {
            assertThat(typeDescription.getDeclaredMethods().filter(isMethod()), hasItems((MethodDescription) new MethodDescription.ForMethod(method)));
        }
        assertThat(typeDescription.getDeclaredMethods().filter(isConstructor()).size(), is(type.getDeclaredConstructors().length));
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            assertThat(typeDescription.getDeclaredMethods().filter(isConstructor()), hasItems((MethodDescription) new MethodDescription.ForConstructor(constructor)));
        }
    }

    @Test
    public void testGetStackSize() throws Exception {
        assertThat(objectType.getStackSize(), is(TypeSize.SINGLE));
        assertThat(intType.getStackSize(), is(TypeSize.SINGLE));
        assertThat(longType.getStackSize(), is(TypeSize.DOUBLE));
        assertThat(integerType.getStackSize(), is(TypeSize.SINGLE));
        assertThat(runnableType.getStackSize(), is(TypeSize.SINGLE));
        assertThat(objectArrayType.getStackSize(), is(TypeSize.SINGLE));
    }

    @Test
    public void testGetName() throws Exception {
        assertThat(objectType.getName(), is(Object.class.getName()));
        assertThat(intType.getName(), is(int.class.getName()));
        assertThat(longType.getName(), is(long.class.getName()));
        assertThat(integerType.getName(), is(Integer.class.getName()));
        assertThat(runnableType.getName(), is(Runnable.class.getName()));
        assertThat(objectArrayType.getName(), is(Object[].class.getName()));
    }

    @Test
    public void testGetDescriptor() throws Exception {
        assertThat(objectType.getDescriptor(), is(Type.getDescriptor(Object.class)));
        assertThat(intType.getDescriptor(), is(Type.getDescriptor(int.class)));
        assertThat(longType.getDescriptor(), is(Type.getDescriptor(long.class)));
        assertThat(integerType.getDescriptor(), is(Type.getDescriptor(Integer.class)));
        assertThat(runnableType.getDescriptor(), is(Type.getDescriptor(Runnable.class)));
        assertThat(objectArrayType.getDescriptor(), is(Type.getDescriptor(Object[].class)));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(objectType.hashCode(), is(Object.class.getName().hashCode()));
        assertThat(intType.hashCode(), is(int.class.getName().hashCode()));
        assertThat(longType.hashCode(), is(long.class.getName().hashCode()));
        assertThat(integerType.hashCode(), is(Integer.class.getName().hashCode()));
        assertThat(runnableType.hashCode(), is(Runnable.class.getName().hashCode()));
        assertThat(objectArrayType.hashCode(), is(Object[].class.getName().hashCode()));
    }

    @Test
    public void testEquals() throws Exception {
        assertTypeEquality(objectType, Object.class);
        assertTypeEquality(intType, int.class);
        assertTypeEquality(longType, long.class);
        assertTypeEquality(integerType, Integer.class);
        assertTypeEquality(runnableType, Runnable.class);
        assertTypeEquality(objectArrayType, Object[].class);
    }

    private static void assertTypeEquality(TypeDescription typeDescription, Class<?> type) {
        TypeDescription otherType = mock(TypeDescription.class);
        when(otherType.getName()).thenReturn(type.getName());
        assertThat(typeDescription.equals(otherType), is(true));
        verify(otherType).getName();
        verifyNoMoreInteractions(otherType);
    }
}
