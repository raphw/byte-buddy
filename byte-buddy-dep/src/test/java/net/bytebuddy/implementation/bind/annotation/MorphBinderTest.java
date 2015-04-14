package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.Mock;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MorphBinderTest extends AbstractAnnotationBinderTest<Morph> {

    private static final String FOO = "foo";

    private static final int INDEX = 0;

    @Mock
    private MethodDescription morphMethod;

    @Mock
    private TypeDescription morphType, defaultType;

    @Mock
    private Implementation.SpecialMethodInvocation specialMethodInvocation;

    public MorphBinderTest() {
        super(Morph.class);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<Morph> getSimpleBinder() {
        return new Morph.Binder(morphMethod);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalType() throws Exception {
        when(target.getTypeDescription()).thenReturn(morphType);
        when(morphMethod.getDeclaringType()).thenReturn(mock(TypeDescription.class));
        new Morph.Binder(morphMethod).bind(annotationDescription, source, target, implementationTarget, assigner);
    }

    @Test
    public void testSuperMethodCallInvalid() throws Exception {
        when(target.getTypeDescription()).thenReturn(morphType);
        when(morphMethod.getDeclaringType()).thenReturn(morphType);
        doReturn(void.class).when(annotation).defaultTarget();
        when(implementationTarget.invokeSuper(source, Implementation.Target.MethodLookup.Default.EXACT))
                .thenReturn(specialMethodInvocation);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = new Morph.Binder(morphMethod)
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(false));
        verify(specialMethodInvocation).isValid();
    }

    @Test
    public void testSuperMethodCallValid() throws Exception {
        when(target.getTypeDescription()).thenReturn(morphType);
        when(morphMethod.getDeclaringType()).thenReturn(morphType);
        doReturn(void.class).when(annotation).defaultTarget();
        when(implementationTarget.invokeSuper(source, Implementation.Target.MethodLookup.Default.EXACT))
                .thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = new Morph.Binder(morphMethod)
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
        verify(specialMethodInvocation).isValid();
    }

    @Test
    public void testDefaultMethodCallImplicitInvalid() throws Exception {
        when(source.getUniqueSignature()).thenReturn(FOO);
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.ForLoadedType(Foo.class));
        when(target.getTypeDescription()).thenReturn(morphType);
        when(morphMethod.getDeclaringType()).thenReturn(morphType);
        when(annotation.defaultMethod()).thenReturn(true);
        doReturn(void.class).when(annotation).defaultTarget();
        when(source.isSpecializableFor(new TypeDescription.ForLoadedType(Foo.class))).thenReturn(true);
        when(implementationTarget.invokeDefault(new TypeDescription.ForLoadedType(Foo.class), FOO))
                .thenReturn(specialMethodInvocation);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = new Morph.Binder(morphMethod)
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(false));
        verify(specialMethodInvocation).isValid();
    }

    @Test
    public void testDefaultMethodCallImplicitValid() throws Exception {
        when(source.getUniqueSignature()).thenReturn(FOO);
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.ForLoadedType(Foo.class));
        when(target.getTypeDescription()).thenReturn(morphType);
        when(morphMethod.getDeclaringType()).thenReturn(morphType);
        when(annotation.defaultMethod()).thenReturn(true);
        doReturn(void.class).when(annotation).defaultTarget();
        when(source.isSpecializableFor(new TypeDescription.ForLoadedType(Foo.class))).thenReturn(true);
        when(implementationTarget.invokeDefault(new TypeDescription.ForLoadedType(Foo.class), FOO))
                .thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = new Morph.Binder(morphMethod)
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
        verify(specialMethodInvocation).isValid();
    }

    @Test
    public void testDefaultMethodCallExplicitInvalid() throws Exception {
        when(source.getUniqueSignature()).thenReturn(FOO);
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.ForLoadedType(Foo.class));
        when(target.getTypeDescription()).thenReturn(morphType);
        when(morphMethod.getDeclaringType()).thenReturn(morphType);
        when(annotation.defaultMethod()).thenReturn(true);
        doReturn(Foo.class).when(annotation).defaultTarget();
        when(implementationTarget.invokeDefault(new TypeDescription.ForLoadedType(Foo.class), FOO))
                .thenReturn(specialMethodInvocation);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = new Morph.Binder(morphMethod)
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(false));
        verify(specialMethodInvocation).isValid();
    }

    @Test
    public void testDefaultMethodCallExplicitValid() throws Exception {
        when(source.getUniqueSignature()).thenReturn(FOO);
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.ForLoadedType(Foo.class));
        when(target.getTypeDescription()).thenReturn(morphType);
        when(morphMethod.getDeclaringType()).thenReturn(morphType);
        when(annotation.defaultMethod()).thenReturn(true);
        doReturn(Foo.class).when(annotation).defaultTarget();
        when(implementationTarget.invokeDefault(new TypeDescription.ForLoadedType(Foo.class), FOO))
                .thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = new Morph.Binder(morphMethod)
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
        verify(specialMethodInvocation).isValid();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Morph.Binder.class).apply();
        ObjectPropertyAssertion.of(Morph.Binder.RedirectionProxy.class).apply();
        ObjectPropertyAssertion.of(Morph.Binder.RedirectionProxy.MethodCall.class).apply();
        ObjectPropertyAssertion.of(Morph.Binder.RedirectionProxy.MethodCall.Appender.class).refine(new ObjectPropertyAssertion.Refinement<Implementation.Target>() {
            @Override
            public void apply(Implementation.Target mock) {
                when(mock.getTypeDescription()).thenReturn(mock(TypeDescription.class));
            }
        }).apply();
        ObjectPropertyAssertion.of(Morph.Binder.RedirectionProxy.StaticFieldConstructor.class).apply();
        ObjectPropertyAssertion.of(Morph.Binder.RedirectionProxy.InstanceFieldConstructor.class).apply();
        ObjectPropertyAssertion.of(Morph.Binder.RedirectionProxy.InstanceFieldConstructor.Appender.class).refine(new ObjectPropertyAssertion.Refinement<Implementation.Target>() {
            @Override
            public void apply(Implementation.Target mock) {
                TypeDescription typeDescription = mock(TypeDescription.class);
                FieldList fieldList = mock(FieldList.class);
                FieldList filteredFieldList = mock(FieldList.class);
                when(fieldList.filter(named(Morph.Binder.RedirectionProxy.FIELD_NAME))).thenReturn(filteredFieldList);
                when(filteredFieldList.getOnly()).thenReturn(mock(FieldDescription.class));
                when(typeDescription.getDeclaredFields()).thenReturn(fieldList);
                when(mock.getTypeDescription()).thenReturn(typeDescription);
            }
        }).apply();
        ObjectPropertyAssertion.of(Morph.Binder.PrecomputedFinding.class).apply();
        ObjectPropertyAssertion.of(Morph.Binder.DefaultMethodLocator.Implicit.class).apply();
        ObjectPropertyAssertion.of(Morph.Binder.DefaultMethodLocator.Explicit.class).apply();
    }

    private interface Foo {

    }
}
