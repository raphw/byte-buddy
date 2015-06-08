package net.bytebuddy.dynamic;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DynamicTypeBuilderTokenTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final int QUX = 42, BAZ = QUX * 2;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription singleType, parameterType, exceptionType;

    private GenericTypeList parameterTypes, exceptionTypes;

    @Before
    public void setUp() throws Exception {
        parameterTypes = new GenericTypeList.Explicit(Collections.singletonList(parameterType));
        exceptionTypes = new GenericTypeList.Explicit(Collections.singletonList(exceptionType));
        when(singleType.accept(any(GenericTypeDescription.Visitor.class))).thenReturn(singleType);
        when(parameterType.accept(any(GenericTypeDescription.Visitor.class))).thenReturn(parameterType);
        when(exceptionType.accept(any(GenericTypeDescription.Visitor.class))).thenReturn(exceptionType);
    }

    @Test
    public void testMethodTokenHashCode() throws Exception {
        assertThat(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, parameterTypes, exceptionTypes, QUX).hashCode(),
                is(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, parameterTypes, exceptionTypes, QUX).hashCode()));
        assertThat(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, parameterTypes, exceptionTypes, QUX).hashCode(),
                not(is(new DynamicType.Builder.AbstractBase.MethodToken(BAR, singleType, parameterTypes, exceptionTypes, QUX).hashCode())));
        assertThat(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, parameterTypes, exceptionTypes, QUX).hashCode(),
                not(is(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, new TypeList.Empty(), exceptionTypes, QUX).hashCode())));
        assertThat(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, parameterTypes, exceptionTypes, QUX).hashCode(),
                not(is(new DynamicType.Builder.AbstractBase.MethodToken(FOO, mock(TypeDescription.class), parameterTypes, exceptionTypes, QUX).hashCode())));
        assertThat(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, parameterTypes, exceptionTypes, QUX).hashCode(),
                is(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, parameterTypes, exceptionTypes, BAZ).hashCode()));
        assertThat(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, parameterTypes, exceptionTypes, QUX).hashCode(),
                is(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, parameterTypes, new TypeList.Empty(), QUX).hashCode()));
    }

    @Test
    public void testMethodTokenEquals() throws Exception {
        DynamicType.Builder.AbstractBase.MethodToken equal = mock(DynamicType.Builder.AbstractBase.MethodToken.class);
        when(equal.getInternalName()).thenReturn(FOO);
        when(equal.getReturnType()).thenReturn(singleType);
        when(equal.getParameterTypes()).thenReturn(parameterTypes);
        assertThat(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, parameterTypes, exceptionTypes, QUX),
                is(equal));
        DynamicType.Builder.AbstractBase.MethodToken equalButName = mock(DynamicType.Builder.AbstractBase.MethodToken.class);
        when(equalButName.getInternalName()).thenReturn(BAR);
        when(equalButName.getReturnType()).thenReturn(singleType);
        when(equalButName.getParameterTypes()).thenReturn(parameterTypes);
        assertThat(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, parameterTypes, exceptionTypes, QUX),
                not(is(equalButName)));
        DynamicType.Builder.AbstractBase.MethodToken equalButReturnType = mock(DynamicType.Builder.AbstractBase.MethodToken.class);
        when(equalButReturnType.getInternalName()).thenReturn(BAR);
        when(equalButReturnType.getReturnType()).thenReturn(mock(TypeDescription.class));
        when(equalButReturnType.getParameterTypes()).thenReturn(parameterTypes);
        assertThat(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, parameterTypes, exceptionTypes, QUX),
                not(is(equalButReturnType)));
        DynamicType.Builder.AbstractBase.MethodToken equalButParameterType = mock(DynamicType.Builder.AbstractBase.MethodToken.class);
        when(equalButParameterType.getInternalName()).thenReturn(BAR);
        when(equalButParameterType.getReturnType()).thenReturn(singleType);
        when(equalButParameterType.getParameterTypes()).thenReturn(new GenericTypeList.Empty());
        assertThat(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, parameterTypes, exceptionTypes, QUX),
                not(is(equalButParameterType)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodTokenSubstitute() throws Exception {
        assertThat(new DynamicType.Builder.AbstractBase.MethodToken(FOO, TargetType.DESCRIPTION, parameterTypes, exceptionTypes, QUX)
                .resolveReturnType(singleType), is((GenericTypeDescription) singleType));
        assertThat(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, parameterTypes, new TypeList.ForLoadedType(TargetType.class), QUX)
                .resolveExceptionTypes(singleType), is((List<GenericTypeDescription>) new GenericTypeList.Explicit(Collections.singletonList(singleType))));
        assertThat(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, new TypeList.ForLoadedType(TargetType.class), exceptionTypes, QUX)
                .resolveParameterTypes(singleType), is((List<GenericTypeDescription>) new GenericTypeList.Explicit(Collections.singletonList(singleType))));
        assertThat(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, parameterTypes, exceptionTypes, QUX)
                .resolveReturnType(mock(TypeDescription.class)), is((GenericTypeDescription) singleType));
        assertThat(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, parameterTypes, exceptionTypes, QUX)
                .resolveExceptionTypes(mock(TypeDescription.class)), is((List<GenericTypeDescription>) exceptionTypes));
        assertThat(new DynamicType.Builder.AbstractBase.MethodToken(FOO, singleType, parameterTypes, exceptionTypes, QUX)
                .resolveParameterTypes(mock(TypeDescription.class)), is((List<GenericTypeDescription>) parameterTypes));
    }

    @Test
    public void testFieldTokenHashCode() throws Exception {
        assertThat(new DynamicType.Builder.AbstractBase.FieldToken(FOO, singleType, QUX).hashCode(),
                is(new DynamicType.Builder.AbstractBase.FieldToken(FOO, singleType, QUX).hashCode()));
        assertThat(new DynamicType.Builder.AbstractBase.FieldToken(FOO, singleType, QUX).hashCode(),
                not(is(new DynamicType.Builder.AbstractBase.FieldToken(BAR, singleType, QUX).hashCode())));
        assertThat(new DynamicType.Builder.AbstractBase.FieldToken(FOO, singleType, QUX).hashCode(),
                is(new DynamicType.Builder.AbstractBase.FieldToken(FOO, mock(TypeDescription.class), QUX).hashCode()));
        assertThat(new DynamicType.Builder.AbstractBase.FieldToken(FOO, singleType, QUX).hashCode(),
                is(new DynamicType.Builder.AbstractBase.FieldToken(FOO, singleType, BAZ).hashCode()));
    }

    @Test
    public void testFieldTokenEquals() throws Exception {
        DynamicType.Builder.AbstractBase.FieldToken equal = mock(DynamicType.Builder.AbstractBase.FieldToken.class);
        when(equal.getFieldName()).thenReturn(FOO);
        assertThat(new DynamicType.Builder.AbstractBase.FieldToken(FOO, singleType, QUX), is(equal));
        DynamicType.Builder.AbstractBase.FieldToken equalButName = mock(DynamicType.Builder.AbstractBase.FieldToken.class);
        when(equalButName.getFieldName()).thenReturn(BAR);
    }

    @Test
    public void testFieldTokenSubstitute() throws Exception {
        assertThat(new DynamicType.Builder.AbstractBase.FieldToken(FOO, TargetType.DESCRIPTION, QUX)
                .resolveFieldType(singleType), is((GenericTypeDescription) singleType));
        assertThat(new DynamicType.Builder.AbstractBase.FieldToken(FOO, singleType, QUX)
                .resolveFieldType(mock(TypeDescription.class)), is((GenericTypeDescription) singleType));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(DynamicType.Builder.AbstractBase.FieldToken.class).applyMutable();
        ObjectPropertyAssertion.of(DynamicType.Builder.AbstractBase.MethodToken.class).create(new ObjectPropertyAssertion.Creator<TypeList>() {
            @Override
            public TypeList create() {
                return new TypeList.Explicit(Collections.singletonList(mock(TypeDescription.class)));
            }
        }).applyMutable();
    }
}
