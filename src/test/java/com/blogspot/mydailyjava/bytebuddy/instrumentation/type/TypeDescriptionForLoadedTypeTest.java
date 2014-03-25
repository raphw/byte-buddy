package com.blogspot.mydailyjava.bytebuddy.instrumentation.type;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import com.blogspot.mydailyjava.bytebuddy.utility.PackagePrivateType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class TypeDescriptionForLoadedTypeTest {

    private TypeDescription objectType;
    private TypeDescription intType;
    private TypeDescription longType;
    private TypeDescription numberType;
    private TypeDescription integerType;
    private TypeDescription serializableType;
    private TypeDescription objectArrayType;

    @Before
    public void setUp() throws Exception {
        objectType = new TypeDescription.ForLoadedType(Object.class);
        intType = new TypeDescription.ForLoadedType(int.class);
        longType = new TypeDescription.ForLoadedType(long.class);
        numberType = new TypeDescription.ForLoadedType(Number.class);
        integerType = new TypeDescription.ForLoadedType(Integer.class);
        serializableType = new TypeDescription.ForLoadedType(Serializable.class);
        objectArrayType = new TypeDescription.ForLoadedType(Object[].class);
    }

    @Test
    public void testIsAssignableFromTypeDescription() throws Exception {
        assertThat(objectType.isAssignableFrom(objectType), is(true));
        assertThat(objectType.isAssignableFrom(intType), is(false));
        assertThat(objectType.isAssignableFrom(longType), is(false));
        assertThat(objectType.isAssignableFrom(numberType), is(true));
        assertThat(objectType.isAssignableFrom(integerType), is(true));
        assertThat(objectType.isAssignableFrom(serializableType), is(true));
        assertThat(objectType.isAssignableFrom(objectArrayType), is(true));

        assertThat(intType.isAssignableFrom(objectType), is(false));
        assertThat(intType.isAssignableFrom(intType), is(true));
        assertThat(intType.isAssignableFrom(longType), is(false));
        assertThat(intType.isAssignableFrom(numberType), is(false));
        assertThat(intType.isAssignableFrom(integerType), is(false));
        assertThat(intType.isAssignableFrom(serializableType), is(false));
        assertThat(intType.isAssignableFrom(objectArrayType), is(false));

        assertThat(longType.isAssignableFrom(objectType), is(false));
        assertThat(longType.isAssignableFrom(intType), is(false));
        assertThat(longType.isAssignableFrom(longType), is(true));
        assertThat(longType.isAssignableFrom(numberType), is(false));
        assertThat(longType.isAssignableFrom(integerType), is(false));
        assertThat(longType.isAssignableFrom(serializableType), is(false));
        assertThat(longType.isAssignableFrom(objectArrayType), is(false));

        assertThat(numberType.isAssignableFrom(objectType), is(false));
        assertThat(numberType.isAssignableFrom(intType), is(false));
        assertThat(numberType.isAssignableFrom(longType), is(false));
        assertThat(numberType.isAssignableFrom(numberType), is(true));
        assertThat(numberType.isAssignableFrom(integerType), is(true));
        assertThat(numberType.isAssignableFrom(serializableType), is(false));
        assertThat(numberType.isAssignableFrom(objectArrayType), is(false));

        assertThat(integerType.isAssignableFrom(objectType), is(false));
        assertThat(integerType.isAssignableFrom(intType), is(false));
        assertThat(integerType.isAssignableFrom(longType), is(false));
        assertThat(integerType.isAssignableFrom(numberType), is(false));
        assertThat(integerType.isAssignableFrom(integerType), is(true));
        assertThat(integerType.isAssignableFrom(serializableType), is(false));
        assertThat(integerType.isAssignableFrom(objectArrayType), is(false));

        assertThat(serializableType.isAssignableFrom(objectType), is(false));
        assertThat(serializableType.isAssignableFrom(intType), is(false));
        assertThat(serializableType.isAssignableFrom(longType), is(false));
        assertThat(serializableType.isAssignableFrom(numberType), is(true));
        assertThat(serializableType.isAssignableFrom(integerType), is(true));
        assertThat(serializableType.isAssignableFrom(serializableType), is(true));
        assertThat(serializableType.isAssignableFrom(objectArrayType), is(true));
    }

    @Test
    public void testIsAssignableFromType() throws Exception {
        assertThat(objectType.isAssignableFrom(Object.class), is(true));
        assertThat(objectType.isAssignableFrom(int.class), is(false));
        assertThat(objectType.isAssignableFrom(long.class), is(false));
        assertThat(objectType.isAssignableFrom(Number.class), is(true));
        assertThat(objectType.isAssignableFrom(Integer.class), is(true));
        assertThat(objectType.isAssignableFrom(Serializable.class), is(true));
        assertThat(objectType.isAssignableFrom(Object[].class), is(true));

        assertThat(intType.isAssignableFrom(Object.class), is(false));
        assertThat(intType.isAssignableFrom(int.class), is(true));
        assertThat(intType.isAssignableFrom(long.class), is(false));
        assertThat(intType.isAssignableFrom(Number.class), is(false));
        assertThat(intType.isAssignableFrom(Integer.class), is(false));
        assertThat(intType.isAssignableFrom(Serializable.class), is(false));
        assertThat(intType.isAssignableFrom(Object[].class), is(false));

        assertThat(longType.isAssignableFrom(Object.class), is(false));
        assertThat(longType.isAssignableFrom(int.class), is(false));
        assertThat(longType.isAssignableFrom(long.class), is(true));
        assertThat(longType.isAssignableFrom(Number.class), is(false));
        assertThat(longType.isAssignableFrom(Integer.class), is(false));
        assertThat(longType.isAssignableFrom(Serializable.class), is(false));
        assertThat(longType.isAssignableFrom(Object[].class), is(false));

        assertThat(numberType.isAssignableFrom(Object.class), is(false));
        assertThat(numberType.isAssignableFrom(int.class), is(false));
        assertThat(numberType.isAssignableFrom(long.class), is(false));
        assertThat(numberType.isAssignableFrom(Number.class), is(true));
        assertThat(numberType.isAssignableFrom(Integer.class), is(true));
        assertThat(numberType.isAssignableFrom(Serializable.class), is(false));
        assertThat(numberType.isAssignableFrom(Object[].class), is(false));

        assertThat(integerType.isAssignableFrom(Object.class), is(false));
        assertThat(integerType.isAssignableFrom(int.class), is(false));
        assertThat(integerType.isAssignableFrom(long.class), is(false));
        assertThat(integerType.isAssignableFrom(Number.class), is(false));
        assertThat(integerType.isAssignableFrom(Integer.class), is(true));
        assertThat(integerType.isAssignableFrom(Serializable.class), is(false));
        assertThat(integerType.isAssignableFrom(Object[].class), is(false));

        assertThat(serializableType.isAssignableFrom(Object.class), is(false));
        assertThat(serializableType.isAssignableFrom(int.class), is(false));
        assertThat(serializableType.isAssignableFrom(long.class), is(false));
        assertThat(serializableType.isAssignableFrom(Number.class), is(true));
        assertThat(serializableType.isAssignableFrom(Integer.class), is(true));
        assertThat(serializableType.isAssignableFrom(Serializable.class), is(true));
        assertThat(serializableType.isAssignableFrom(Object[].class), is(true));

        assertThat(objectArrayType.isAssignableFrom(Object.class), is(false));
        assertThat(objectArrayType.isAssignableFrom(int.class), is(false));
        assertThat(objectArrayType.isAssignableFrom(long.class), is(false));
        assertThat(objectArrayType.isAssignableFrom(Number.class), is(false));
        assertThat(objectArrayType.isAssignableFrom(Integer.class), is(false));
        assertThat(objectArrayType.isAssignableFrom(Serializable.class), is(false));
        assertThat(objectArrayType.isAssignableFrom(Object[].class), is(true));
    }

    @Test
    public void testIsAssignableTo() throws Exception {
        assertThat(objectType.isAssignableTo(objectType), is(true));
        assertThat(objectType.isAssignableTo(intType), is(false));
        assertThat(objectType.isAssignableTo(longType), is(false));
        assertThat(objectType.isAssignableTo(numberType), is(false));
        assertThat(objectType.isAssignableTo(integerType), is(false));
        assertThat(objectType.isAssignableTo(serializableType), is(false));
        assertThat(objectType.isAssignableTo(objectArrayType), is(false));

        assertThat(intType.isAssignableTo(objectType), is(false));
        assertThat(intType.isAssignableTo(intType), is(true));
        assertThat(intType.isAssignableTo(longType), is(false));
        assertThat(intType.isAssignableTo(numberType), is(false));
        assertThat(intType.isAssignableTo(integerType), is(false));
        assertThat(intType.isAssignableTo(serializableType), is(false));
        assertThat(intType.isAssignableTo(objectArrayType), is(false));

        assertThat(longType.isAssignableTo(objectType), is(false));
        assertThat(longType.isAssignableTo(intType), is(false));
        assertThat(longType.isAssignableTo(longType), is(true));
        assertThat(longType.isAssignableTo(numberType), is(false));
        assertThat(longType.isAssignableTo(integerType), is(false));
        assertThat(longType.isAssignableTo(serializableType), is(false));
        assertThat(longType.isAssignableTo(objectArrayType), is(false));

        assertThat(integerType.isAssignableTo(objectType), is(true));
        assertThat(integerType.isAssignableTo(intType), is(false));
        assertThat(integerType.isAssignableTo(longType), is(false));
        assertThat(integerType.isAssignableTo(numberType), is(true));
        assertThat(integerType.isAssignableTo(integerType), is(true));
        assertThat(integerType.isAssignableTo(serializableType), is(true));
        assertThat(integerType.isAssignableTo(objectArrayType), is(false));

        assertThat(serializableType.isAssignableTo(objectType), is(true));
        assertThat(serializableType.isAssignableTo(intType), is(false));
        assertThat(serializableType.isAssignableTo(longType), is(false));
        assertThat(serializableType.isAssignableTo(numberType), is(false));
        assertThat(serializableType.isAssignableTo(integerType), is(false));
        assertThat(serializableType.isAssignableTo(serializableType), is(true));
        assertThat(serializableType.isAssignableTo(objectArrayType), is(false));

        assertThat(objectArrayType.isAssignableTo(objectType), is(true));
        assertThat(objectArrayType.isAssignableTo(intType), is(false));
        assertThat(objectArrayType.isAssignableTo(longType), is(false));
        assertThat(objectArrayType.isAssignableTo(numberType), is(false));
        assertThat(objectArrayType.isAssignableTo(integerType), is(false));
        assertThat(objectArrayType.isAssignableTo(serializableType), is(true));
        assertThat(objectArrayType.isAssignableTo(objectArrayType), is(true));
    }

    @Test
    public void testIsAssignableToType() throws Exception {
        assertThat(objectType.isAssignableTo(Object.class), is(true));
        assertThat(objectType.isAssignableTo(int.class), is(false));
        assertThat(objectType.isAssignableTo(long.class), is(false));
        assertThat(objectType.isAssignableTo(Number.class), is(false));
        assertThat(objectType.isAssignableTo(Integer.class), is(false));
        assertThat(objectType.isAssignableTo(Serializable.class), is(false));
        assertThat(objectType.isAssignableTo(Object[].class), is(false));

        assertThat(intType.isAssignableTo(Object.class), is(false));
        assertThat(intType.isAssignableTo(int.class), is(true));
        assertThat(intType.isAssignableTo(long.class), is(false));
        assertThat(intType.isAssignableTo(Number.class), is(false));
        assertThat(intType.isAssignableTo(Integer.class), is(false));
        assertThat(intType.isAssignableTo(Serializable.class), is(false));
        assertThat(intType.isAssignableTo(Object[].class), is(false));

        assertThat(longType.isAssignableTo(Object.class), is(false));
        assertThat(longType.isAssignableTo(int.class), is(false));
        assertThat(longType.isAssignableTo(long.class), is(true));
        assertThat(longType.isAssignableTo(Number.class), is(false));
        assertThat(longType.isAssignableTo(Integer.class), is(false));
        assertThat(longType.isAssignableTo(Serializable.class), is(false));
        assertThat(longType.isAssignableTo(Object[].class), is(false));

        assertThat(numberType.isAssignableTo(Object.class), is(true));
        assertThat(numberType.isAssignableTo(int.class), is(false));
        assertThat(numberType.isAssignableTo(long.class), is(false));
        assertThat(numberType.isAssignableTo(Number.class), is(true));
        assertThat(numberType.isAssignableTo(Integer.class), is(false));
        assertThat(numberType.isAssignableTo(Serializable.class), is(true));
        assertThat(numberType.isAssignableTo(Object[].class), is(false));

        assertThat(integerType.isAssignableTo(Object.class), is(true));
        assertThat(integerType.isAssignableTo(int.class), is(false));
        assertThat(integerType.isAssignableTo(long.class), is(false));
        assertThat(integerType.isAssignableTo(Number.class), is(true));
        assertThat(integerType.isAssignableTo(Integer.class), is(true));
        assertThat(integerType.isAssignableTo(Serializable.class), is(true));
        assertThat(integerType.isAssignableTo(Object[].class), is(false));

        assertThat(serializableType.isAssignableTo(Object.class), is(true));
        assertThat(serializableType.isAssignableTo(int.class), is(false));
        assertThat(serializableType.isAssignableTo(long.class), is(false));
        assertThat(serializableType.isAssignableTo(Number.class), is(false));
        assertThat(serializableType.isAssignableTo(Integer.class), is(false));
        assertThat(serializableType.isAssignableTo(Serializable.class), is(true));
        assertThat(serializableType.isAssignableTo(Object[].class), is(false));

        assertThat(objectArrayType.isAssignableTo(Object.class), is(true));
        assertThat(objectArrayType.isAssignableTo(int.class), is(false));
        assertThat(objectArrayType.isAssignableTo(long.class), is(false));
        assertThat(objectArrayType.isAssignableTo(Number.class), is(false));
        assertThat(objectArrayType.isAssignableTo(Integer.class), is(false));
        assertThat(objectArrayType.isAssignableTo(Serializable.class), is(true));
        assertThat(objectArrayType.isAssignableTo(Object[].class), is(true));
    }

    @Test
    public void testRepresents() throws Exception {
        assertThat(objectType.represents(Object.class), is(true));
        assertThat(objectType.represents(int.class), is(false));
        assertThat(objectType.represents(long.class), is(false));
        assertThat(objectType.represents(Number.class), is(false));
        assertThat(objectType.represents(Integer.class), is(false));
        assertThat(objectType.represents(Serializable.class), is(false));
        assertThat(objectType.represents(Object[].class), is(false));

        assertThat(intType.represents(Object.class), is(false));
        assertThat(intType.represents(int.class), is(true));
        assertThat(intType.represents(long.class), is(false));
        assertThat(intType.represents(Number.class), is(false));
        assertThat(intType.represents(Integer.class), is(false));
        assertThat(intType.represents(Serializable.class), is(false));
        assertThat(intType.represents(Object[].class), is(false));

        assertThat(longType.represents(Object.class), is(false));
        assertThat(longType.represents(int.class), is(false));
        assertThat(longType.represents(long.class), is(true));
        assertThat(longType.represents(Number.class), is(false));
        assertThat(longType.represents(Integer.class), is(false));
        assertThat(longType.represents(Serializable.class), is(false));
        assertThat(longType.represents(Object[].class), is(false));

        assertThat(numberType.represents(Object.class), is(false));
        assertThat(numberType.represents(int.class), is(false));
        assertThat(numberType.represents(long.class), is(false));
        assertThat(numberType.represents(Number.class), is(true));
        assertThat(numberType.represents(Integer.class), is(false));
        assertThat(numberType.represents(Serializable.class), is(false));
        assertThat(numberType.represents(Object[].class), is(false));

        assertThat(integerType.represents(Object.class), is(false));
        assertThat(integerType.represents(int.class), is(false));
        assertThat(integerType.represents(long.class), is(false));
        assertThat(integerType.represents(Number.class), is(false));
        assertThat(integerType.represents(Integer.class), is(true));
        assertThat(integerType.represents(Serializable.class), is(false));
        assertThat(integerType.represents(Object[].class), is(false));

        assertThat(serializableType.represents(Object.class), is(false));
        assertThat(serializableType.represents(int.class), is(false));
        assertThat(serializableType.represents(long.class), is(false));
        assertThat(serializableType.represents(Number.class), is(false));
        assertThat(serializableType.represents(Integer.class), is(false));
        assertThat(serializableType.represents(Serializable.class), is(true));
        assertThat(serializableType.represents(Object[].class), is(false));
    }

    @Test
    public void testGetComponentType() throws Exception {
        assertThat(objectType.getComponentType(), nullValue(TypeDescription.class));
        assertThat(intType.getComponentType(), nullValue(TypeDescription.class));
        assertThat(longType.getComponentType(), nullValue(TypeDescription.class));
        assertThat(numberType.getComponentType(), nullValue(TypeDescription.class));
        assertThat(integerType.getComponentType(), nullValue(TypeDescription.class));
        assertThat(serializableType.getComponentType(), nullValue(TypeDescription.class));
        assertThat(objectArrayType.getComponentType(), equalTo(objectType));
    }

    @Test
    public void testGetSupertype() throws Exception {
        assertThat(objectType.getSupertype(), nullValue(TypeDescription.class));
        assertThat(intType.getSupertype(), nullValue(TypeDescription.class));
        assertThat(longType.getSupertype(), nullValue(TypeDescription.class));
        assertThat(numberType.getSupertype(), equalTo(objectType));
        assertThat(integerType.getSupertype(), equalTo(numberType));
        assertThat(serializableType.getSupertype(), nullValue(TypeDescription.class));
        assertThat(objectArrayType.getSupertype(), equalTo(objectType));
    }

    @Test
    public void testGetInterfaces() throws Exception {
        assertThat(numberType.getInterfaces().size(), is(1));
        assertThat(numberType.getInterfaces(), hasItems(serializableType));
        assertThat(objectType.getInterfaces().size(), is(0));
    }

    @Test
    public void testGetStackSize() throws Exception {
        assertThat(objectType.getStackSize(), is(StackSize.SINGLE));
        assertThat(intType.getStackSize(), is(StackSize.SINGLE));
        assertThat(longType.getStackSize(), is(StackSize.DOUBLE));
        assertThat(numberType.getStackSize(), is(StackSize.SINGLE));
        assertThat(integerType.getStackSize(), is(StackSize.SINGLE));
        assertThat(serializableType.getStackSize(), is(StackSize.SINGLE));
        assertThat(objectArrayType.getStackSize(), is(StackSize.SINGLE));
    }

    @Test
    public void testGetDescriptor() throws Exception {
        assertThat(objectType.getDescriptor(), is(Type.getDescriptor(Object.class)));
        assertThat(intType.getDescriptor(), is(Type.getDescriptor(int.class)));
        assertThat(longType.getDescriptor(), is(Type.getDescriptor(long.class)));
        assertThat(numberType.getDescriptor(), is(Type.getDescriptor(Number.class)));
        assertThat(integerType.getDescriptor(), is(Type.getDescriptor(Integer.class)));
        assertThat(serializableType.getDescriptor(), is(Type.getDescriptor(Serializable.class)));
        assertThat(objectArrayType.getDescriptor(), is(Type.getDescriptor(Object[].class)));
    }

    @Test
    public void testGetReachableMethods() throws Exception {
        assertThat(objectType.getReachableMethods().size(), is(Object.class.getDeclaredMethods().length
                + Object.class.getDeclaredConstructors().length));
        Set<String> signatures = new HashSet<String>();
        for (Method method : Object.class.getDeclaredMethods()) {
            if (!(Modifier.isAbstract(method.getModifiers()) || Modifier.isPrivate(method.getModifiers()))) {
                signatures.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        for (Method method : Number.class.getDeclaredMethods()) {
            if (!(Modifier.isAbstract(method.getModifiers()) || Modifier.isPrivate(method.getModifiers()))) {
                signatures.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        for (Method method : Comparable.class.getDeclaredMethods()) {
            signatures.add(method.getName() + Type.getMethodDescriptor(method));
        }
        for (Method method : Integer.class.getDeclaredMethods()) {
            signatures.add(method.getName() + Type.getMethodDescriptor(method));
        }
        for (Constructor<?> constructor : Integer.class.getDeclaredConstructors()) {
            signatures.add(constructor.getName() + Type.getConstructorDescriptor(constructor));
        }
        assertThat(integerType.getReachableMethods().size(), is(signatures.size()));
    }

    @Test
    public void testStackSize() throws Exception {
        assertThat(objectType.getStackSize(), is(StackSize.SINGLE));
        assertThat(intType.getStackSize(), is(StackSize.SINGLE));
        assertThat(longType.getStackSize(), is(StackSize.DOUBLE));
        assertThat(numberType.getStackSize(), is(StackSize.SINGLE));
        assertThat(integerType.getStackSize(), is(StackSize.SINGLE));
        assertThat(serializableType.getStackSize(), is(StackSize.SINGLE));
        assertThat(objectArrayType.getStackSize(), is(StackSize.SINGLE));
    }

    @Test
    public void testIsVisibleTo() throws Exception {
        assertThat(objectType.isVisibleTo(integerType), is(true));
        assertThat(objectType.isVisibleTo(new TypeDescription.ForLoadedType(PackagePrivateType.TYPE)), is(true));
        assertThat(new TypeDescription.ForLoadedType(PackagePrivateType.TYPE).isVisibleTo(objectType), is(false));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(objectType.hashCode(), is(Object.class.getName().hashCode()));
        assertThat(intType.hashCode(), is(int.class.getName().hashCode()));
        assertThat(longType.hashCode(), is(long.class.getName().hashCode()));
        assertThat(numberType.hashCode(), is(Number.class.getName().hashCode()));
        assertThat(integerType.hashCode(), is(Integer.class.getName().hashCode()));
        assertThat(serializableType.hashCode(), is(Serializable.class.getName().hashCode()));
        assertThat(objectArrayType.hashCode(), is(Object[].class.getName().hashCode()));
    }

    @Test
    public void testEquals() throws Exception {
        assertTypeEquality(objectType, Object.class);
        assertTypeEquality(intType, int.class);
        assertTypeEquality(longType, long.class);
        assertTypeEquality(numberType, Number.class);
        assertTypeEquality(integerType, Integer.class);
        assertTypeEquality(serializableType, Serializable.class);
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
