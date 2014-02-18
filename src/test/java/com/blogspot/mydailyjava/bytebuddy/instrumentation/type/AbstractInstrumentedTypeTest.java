package com.blogspot.mydailyjava.bytebuddy.instrumentation.type;

import com.blogspot.mydailyjava.bytebuddy.SyntheticState;
import com.blogspot.mydailyjava.bytebuddy.TypeManifestation;
import com.blogspot.mydailyjava.bytebuddy.Visibility;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.StackSize;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.Mockito.*;

public abstract class AbstractInstrumentedTypeTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    protected abstract InstrumentedType makeInstrumentedType(String name,
                                                             Class<?> superType,
                                                             Class<?>[] interfaces,
                                                             Visibility visibility,
                                                             TypeManifestation typeManifestation,
                                                             SyntheticState syntheticState);

    private InstrumentedType instrumentedType;

    @Before
    public void setUp() throws Exception {
        instrumentedType = makeInstrumentedType(FOO,
                Object.class,
                new Class<?>[]{Serializable.class},
                Visibility.PUBLIC,
                TypeManifestation.CONCRETE,
                SyntheticState.NON_SYNTHETIC);
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
        assertThat(instrumentedType.getStackSize(), is(StackSize.SINGLE));
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

    @Test
    public void testWithField() throws Exception {
        TypeDescription fieldType = mock(TypeDescription.class);
        when(fieldType.getInternalName()).thenReturn(BAR);
        instrumentedType = instrumentedType.withField(QUX, fieldType, Modifier.PUBLIC, false);
        assertThat(instrumentedType.getDeclaredFields().size(), is(1));
        FieldDescription fieldDescription = instrumentedType.getDeclaredFields().get(0);
        assertThat(fieldDescription.getFieldType(), is(fieldType));
        assertThat(fieldDescription.getModifiers(), is(Modifier.PUBLIC));
        assertThat(fieldDescription.isSynthetic(), is(false));
        assertThat(fieldDescription.getName(), is(QUX));
    }

    @Test
    public void testWithFieldOfInstrumentedType() throws Exception {
        instrumentedType = instrumentedType.withField(QUX, instrumentedType, Modifier.PUBLIC, false);
        assertThat(instrumentedType.getDeclaredFields().size(), is(1));
        FieldDescription fieldDescription = instrumentedType.getDeclaredFields().get(0);
        assertThat(fieldDescription.getFieldType(), sameInstance((TypeDescription) instrumentedType));
        assertThat(fieldDescription.getModifiers(), is(Modifier.PUBLIC));
        assertThat(fieldDescription.isSynthetic(), is(false));
        assertThat(fieldDescription.getName(), is(QUX));
    }

    @Test
    public void testWithMethod() throws Exception {
        TypeDescription parameterType = mock(TypeDescription.class);
        when(parameterType.getInternalName()).thenReturn(BAR);
        TypeDescription returnType = mock(TypeDescription.class);
        when(returnType.getInternalName()).thenReturn(BAR);
        instrumentedType = instrumentedType.withMethod(QUX, returnType, Arrays.asList(parameterType), Modifier.PUBLIC, false);
        assertThat(instrumentedType.getDeclaredMethods().size(), is(1));
        MethodDescription methodDescription = instrumentedType.getDeclaredMethods().get(0);
        assertThat(methodDescription.getParameterTypes().size(), is(1));
        assertThat(methodDescription.getParameterTypes().get(0), is(parameterType));
        assertThat(methodDescription.getReturnType(), is(returnType));
        assertThat(methodDescription.getModifiers(), is(Modifier.PUBLIC));
        assertThat(methodDescription.isSynthetic(), is(false));
        assertThat(methodDescription.getName(), is(QUX));
    }

    @Test
    public void testWithMethodOfInstrumentedType() throws Exception {
        instrumentedType = instrumentedType.withMethod(QUX, instrumentedType, Arrays.asList(instrumentedType), Modifier.PUBLIC, false);
        assertThat(instrumentedType.getDeclaredMethods().size(), is(1));
        MethodDescription methodDescription = instrumentedType.getDeclaredMethods().get(0);
        assertThat(methodDescription.getParameterTypes().size(), is(1));
        assertThat(methodDescription.getParameterTypes().get(0), sameInstance((TypeDescription) instrumentedType));
        assertThat(methodDescription.getReturnType(), sameInstance((TypeDescription) instrumentedType));
        assertThat(methodDescription.getModifiers(), is(Modifier.PUBLIC));
        assertThat(methodDescription.isSynthetic(), is(false));
        assertThat(methodDescription.getName(), is(QUX));
    }
}
