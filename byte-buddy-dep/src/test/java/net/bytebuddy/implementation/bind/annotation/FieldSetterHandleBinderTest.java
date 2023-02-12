package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.utility.JavaType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class FieldSetterHandleBinderTest extends AbstractAnnotationBinderTest<FieldSetterHandle> {

    private static final String FOO = "foo", BAR = "bar";

    @Mock
    private FieldDescription.InDefinedShape fieldDescription;

    @Mock
    private TypeDescription.Generic fieldType, targetType;

    @Mock
    private TypeDescription rawFieldType, rawTargetType, rawDeclaringType;

    public FieldSetterHandleBinderTest() {
        super(FieldSetterHandle.class);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(fieldDescription.asDefined()).thenReturn(fieldDescription);
        when(fieldDescription.getType()).thenReturn(fieldType);
        when(fieldDescription.getDeclaringType()).thenReturn(rawDeclaringType);
        when(target.getType()).thenReturn(targetType);
        when(fieldType.asErasure()).thenReturn(rawFieldType);
        when(targetType.asErasure()).thenReturn(rawTargetType);
    }

    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldSetterHandle> getSimpleBinder() {
        return FieldSetterHandle.Binder.INSTANCE;
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalParameter() throws Exception {
        doReturn(void.class).when(annotation).declaringType();
        when(annotation.value()).thenReturn(FOO);
        when(instrumentedType.getDeclaredFields()).thenReturn(new FieldList.Explicit<FieldDescription.InDefinedShape>(fieldDescription));
        when(fieldDescription.getActualName()).thenReturn(FOO);
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(stackManipulation.isValid()).thenReturn(true);
        FieldSetterHandle.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
    }

    @Test
    public void testLegalAssignment() throws Exception {
        when(rawTargetType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        doReturn(void.class).when(annotation).declaringType();
        when(annotation.value()).thenReturn(FOO);
        when(instrumentedType.getDeclaredFields()).thenReturn(new FieldList.Explicit<FieldDescription.InDefinedShape>(fieldDescription));
        when(fieldDescription.getActualName()).thenReturn(FOO);
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(stackManipulation.isValid()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = FieldSetterHandle.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testLegalStaticAssignment() throws Exception {
        when(rawTargetType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        doReturn(void.class).when(annotation).declaringType();
        when(annotation.value()).thenReturn(FOO);
        when(instrumentedType.getDeclaredFields()).thenReturn(new FieldList.Explicit<FieldDescription.InDefinedShape>(fieldDescription));
        when(fieldDescription.isStatic()).thenReturn(true);
        when(fieldDescription.getActualName()).thenReturn(FOO);
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(stackManipulation.isValid()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = FieldSetterHandle.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }

    public static class Foo {

        public String foo;
    }
}
