package com.blogspot.mydailyjava.bytebuddy.instrumentation.type;

import com.blogspot.mydailyjava.bytebuddy.*;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class InstrumentedTypeTest {

    private static final String FOO = "foo", BAR = "bar";

    private InstrumentedType instrumentedType;

    @Before
    public void setUp() throws Exception {
        NamingStrategy namingStrategy = mock(NamingStrategy.class);
        when(namingStrategy.getName(any(NamingStrategy.UnnamedType.class))).thenReturn(FOO);
        instrumentedType = new InstrumentedType(new ClassVersion(Opcodes.V1_6),
                Object.class,
                Arrays.<Class<?>>asList(Serializable.class),
                Visibility.PUBLIC,
                TypeManifestation.CONCRETE,
                SyntheticState.NON_SYNTHETIC,
                namingStrategy);
        verify(namingStrategy).getName(instrumentedType);
        verifyNoMoreInteractions(namingStrategy);
    }

    @Test
    public void testIsAssignableFrom() throws Exception {
        TypeDescription nameEqualType = mock(TypeDescription.class);
        when(nameEqualType.getName()).thenReturn(FOO);
        assertThat(instrumentedType.isAssignableFrom(nameEqualType), is(true));
        verify(nameEqualType).getName();
        verifyNoMoreInteractions(nameEqualType);
        TypeDescription serializableAcceptingType = mock(TypeDescription.class);
        when(serializableAcceptingType.getName()).thenReturn(BAR);
        when(serializableAcceptingType.isAssignableTo(Serializable.class)).thenReturn(true);
        assertThat(instrumentedType.isAssignableFrom(serializableAcceptingType), is(true));
        verify(serializableAcceptingType).getName();
        verify(serializableAcceptingType).isAssignableTo(Serializable.class);
        verifyNoMoreInteractions(serializableAcceptingType);
        TypeDescription objectAcceptingType = mock(TypeDescription.class);
        when(objectAcceptingType.getName()).thenReturn(BAR);
        when(objectAcceptingType.isAssignableTo(Object.class)).thenReturn(true);
        assertThat(instrumentedType.isAssignableFrom(objectAcceptingType), is(true));
        verify(objectAcceptingType).getName();
        verify(objectAcceptingType).isAssignableTo(Serializable.class);
        verify(objectAcceptingType).isAssignableTo(Object.class);
        verifyNoMoreInteractions(objectAcceptingType);
    }

    @Test
    public void testIsAssignableTo() throws Exception {
        TypeDescription nameEqualType = mock(TypeDescription.class);
        when(nameEqualType.getName()).thenReturn(FOO);
        assertThat(instrumentedType.isAssignableTo(nameEqualType), is(true));
        verify(nameEqualType).getName();
        verifyNoMoreInteractions(nameEqualType);
        TypeDescription serializableAcceptingType = mock(TypeDescription.class);
        when(serializableAcceptingType.getName()).thenReturn(BAR);
        when(serializableAcceptingType.isAssignableFrom(Serializable.class)).thenReturn(true);
        assertThat(instrumentedType.isAssignableTo(serializableAcceptingType), is(true));
        verify(serializableAcceptingType).getName();
        verify(serializableAcceptingType).isAssignableFrom(Serializable.class);
        verifyNoMoreInteractions(serializableAcceptingType);
        TypeDescription objectAcceptingType = mock(TypeDescription.class);
        when(objectAcceptingType.getName()).thenReturn(BAR);
        when(objectAcceptingType.isAssignableFrom(Object.class)).thenReturn(true);
        assertThat(instrumentedType.isAssignableTo(objectAcceptingType), is(true));
        verify(objectAcceptingType).getName();
        verify(objectAcceptingType).isAssignableFrom(Serializable.class);
        verify(objectAcceptingType).isAssignableFrom(Object.class);
        verifyNoMoreInteractions(objectAcceptingType);
    }

    @Test
    public void testRepresents() throws Exception {
        assertThat(instrumentedType.represents(Object.class), is(false));
        assertThat(instrumentedType.represents(Serializable.class), is(false));
    }

    @Test
    public void testGetSupertype() throws Exception {
        assertThat(instrumentedType.getSupertype().getName(), is(Object.class.getName()));
    }

    @Test
    public void testGetInterfaces() throws Exception {
        assertThat(instrumentedType.getInterfaces().size(), is(1));
        assertThat(instrumentedType.getInterfaces().get(0).getName(), is(Serializable.class.getName()));
    }

    @Test
    public void testGetPackageName() throws Exception {
        assertThat(instrumentedType.getPackageName().length(), is(0));
    }

    @Test
    public void testGetStackSize() throws Exception {
        assertThat(instrumentedType.getStackSize(), is(TypeSize.SINGLE));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(instrumentedType.hashCode(), is(FOO.hashCode()));
    }

    @Test
    public void testEquals() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(typeDescription.getName()).thenReturn(FOO);
        assertThat(instrumentedType, equalTo(typeDescription));
        verify(typeDescription).getName();
        verifyNoMoreInteractions(typeDescription);
    }
}
