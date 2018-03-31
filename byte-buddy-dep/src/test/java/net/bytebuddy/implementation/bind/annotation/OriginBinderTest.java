package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class OriginBinderTest extends AbstractAnnotationBinderTest<Origin> {

    private static final String FOO = "foo";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Mock
    private TypeDescription targetType;

    @Mock
    private TypeDescription.Generic genericTargetType;

    @Mock
    private MethodDescription.InDefinedShape methodDescription;

    public OriginBinderTest() {
        super(Origin.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(target.getType()).thenReturn(genericTargetType);
        when(targetType.asErasure()).thenReturn(targetType);
        when(genericTargetType.asErasure()).thenReturn(targetType);
        when(source.asDefined()).thenReturn(methodDescription);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<Origin> getSimpleBinder() {
        return Origin.Binder.INSTANCE;
    }

    @Test
    public void testClassBinding() throws Exception {
        when(targetType.getInternalName()).thenReturn(FOO);
        when(targetType.represents(Class.class)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(true));
        verify(implementationTarget).getOriginType();
    }

    @Test
    public void testMethodBinding() throws Exception {
        when(targetType.getInternalName()).thenReturn(FOO);
        when(targetType.represents(Method.class)).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testMethodBindingForNonMethod() throws Exception {
        when(targetType.getInternalName()).thenReturn(FOO);
        when(targetType.represents(Method.class)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test
    public void testConstructorBinding() throws Exception {
        when(targetType.getInternalName()).thenReturn(FOO);
        when(targetType.represents(Constructor.class)).thenReturn(true);
        when(source.isConstructor()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testConstructorBindingForNonConstructor() throws Exception {
        when(targetType.getInternalName()).thenReturn(FOO);
        when(targetType.represents(Constructor.class)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test
    public void testStringBinding() throws Exception {
        when(targetType.getInternalName()).thenReturn(FOO);
        when(targetType.represents(String.class)).thenReturn(true);
        when(targetType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testModifierBinding() throws Exception {
        when(targetType.getInternalName()).thenReturn(FOO);
        when(targetType.represents(int.class)).thenReturn(true);
        when(targetType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testMethodHandleBinding() throws Exception {
        when(genericTargetType.asErasure()).thenReturn(new TypeDescription.ForLoadedType(JavaType.METHOD_HANDLE.load()));
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InDefinedShape>());
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(typeDescription.asErasure()).thenReturn(typeDescription);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testMethodTypeBinding() throws Exception {
        when(genericTargetType.asErasure()).thenReturn(new TypeDescription.ForLoadedType(JavaType.METHOD_TYPE.load()));
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InDefinedShape>());
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalBinding() throws Exception {
        when(targetType.getInternalName()).thenReturn(FOO);
        when(targetType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        Origin.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
    }
}
