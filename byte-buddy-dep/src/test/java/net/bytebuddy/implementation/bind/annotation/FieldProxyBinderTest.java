package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class FieldProxyBinderTest extends AbstractAnnotationBinderTest<FieldProxy> {

    private static final String FOO = "foo";

    @Mock
    private MethodDescription getterMethod, setterMethod;

    @Mock
    private TypeDescription setterType, getterType, fieldType;

    @Mock
    private FieldDescription.InDefinedShape fieldDescription;

    public FieldProxyBinderTest() {
        super(FieldProxy.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(getterMethod.getDeclaringType()).thenReturn(getterType);
        when(setterMethod.getDeclaringType()).thenReturn(setterType);
        when(instrumentedType.getDeclaredFields())
                .thenReturn(new FieldList.Explicit<FieldDescription.InDefinedShape>(Collections.singletonList(fieldDescription)));
        when(fieldDescription.getType()).thenReturn(fieldType);
        when(fieldType.getStackSize()).thenReturn(StackSize.ZERO);
        when(fieldType.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        when(fieldType.asErasure()).thenReturn(fieldType);
        when(setterType.asErasure()).thenReturn(setterType);
        when(getterType.asErasure()).thenReturn(getterType);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldProxy> getSimpleBinder() {
        return new FieldProxy.Binder(getterMethod, setterMethod);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalType() throws Exception {
        TypeDescription targetType = mock(TypeDescription.class);
        when(targetType.asErasure()).thenReturn(targetType);
        when(target.getType()).thenReturn(targetType);
        new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner);
    }

    @Test
    public void testGetterForImplicitNamedFieldInHierarchy() throws Exception {
        when(target.getType()).thenReturn(getterType);
        doReturn(void.class).when(annotation).definingType();
        when(annotation.value()).thenReturn(FieldProxy.BEAN_PROPERTY);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(fieldType);
        when(source.getParameters()).thenReturn(new ParameterList.Empty());
        when(source.getName()).thenReturn("getFoo");
        when(source.getSourceCodeName()).thenReturn("getFoo");
        when(source.getInternalName()).thenReturn("getFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testGetterForExplicitNamedFieldInHierarchy() throws Exception {
        when(target.getType()).thenReturn(getterType);
        doReturn(void.class).when(annotation).definingType();
        when(annotation.value()).thenReturn(FOO);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(fieldType);
        when(source.getParameters()).thenReturn(new ParameterList.Empty());
        when(source.getName()).thenReturn("getFoo");
        when(source.getInternalName()).thenReturn("getFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testGetterForImplicitNamedFieldInNamedType() throws Exception {
        when(target.getType()).thenReturn(getterType);
        doReturn(Foo.class).when(annotation).definingType();
        when(annotation.value()).thenReturn(FieldProxy.BEAN_PROPERTY);
        when(fieldDescription.getInternalName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(fieldType);
        when(source.getParameters()).thenReturn(new ParameterList.Empty());
        when(source.getName()).thenReturn("getFoo");
        when(source.getSourceCodeName()).thenReturn("getFoo");
        when(source.getInternalName()).thenReturn("getFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testGetterForExplicitNamedFieldInNamedType() throws Exception {
        when(target.getType()).thenReturn(getterType);
        doReturn(Foo.class).when(annotation).definingType();
        when(annotation.value()).thenReturn(FOO);
        when(fieldDescription.getInternalName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(fieldType);
        when(source.getParameters()).thenReturn(new ParameterList.Empty());
        when(source.getName()).thenReturn("getFoo");
        when(source.getInternalName()).thenReturn("getFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testSetterForImplicitNamedFieldInHierarchy() throws Exception {
        when(target.getType()).thenReturn(setterType);
        doReturn(void.class).when(annotation).definingType();
        when(annotation.value()).thenReturn(FieldProxy.BEAN_PROPERTY);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(new TypeDescription.ForLoadedType(void.class));
        when(source.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(source, Collections.singletonList(fieldType)));
        when(source.getSourceCodeName()).thenReturn("setFoo");
        when(source.getInternalName()).thenReturn("setFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testSetterForExplicitNamedFieldInHierarchy() throws Exception {
        when(target.getType()).thenReturn(setterType);
        doReturn(void.class).when(annotation).definingType();
        when(annotation.value()).thenReturn(FOO);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(new TypeDescription.ForLoadedType(void.class));
        when(source.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(source, Collections.singletonList(fieldType)));
        when(source.getName()).thenReturn("setFoo");
        when(source.getInternalName()).thenReturn("setFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testSetterForImplicitNamedFieldInNamedType() throws Exception {
        when(target.getType()).thenReturn(setterType);
        doReturn(Foo.class).when(annotation).definingType();
        when(annotation.value()).thenReturn(FieldProxy.BEAN_PROPERTY);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(new TypeDescription.ForLoadedType(void.class));
        when(source.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(source, Collections.singletonList(fieldType)));
        when(source.getName()).thenReturn("setFoo");
        when(source.getSourceCodeName()).thenReturn("setFoo");
        when(source.getInternalName()).thenReturn("setFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testSetterForExplicitNamedFieldInNamedType() throws Exception {
        when(target.getType()).thenReturn(setterType);
        doReturn(Foo.class).when(annotation).definingType();
        when(annotation.value()).thenReturn(FOO);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(new TypeDescription.ForLoadedType(void.class));
        when(source.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(source, Collections.singletonList(fieldType)));
        when(source.getName()).thenReturn("setFoo");
        when(source.getInternalName()).thenReturn("setFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldProxy.Binder.class).apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.StaticFieldConstructor.class).apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.FieldLocator.Legal.class).apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.FieldLocator.Illegal.class).apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.FieldLocator.LookupEngine.ForHierarchy.class).apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.FieldLocator.LookupEngine.ForExplicitType.class).apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.FieldLocator.LookupEngine.Illegal.class).apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.FieldLocator.Resolution.Resolved.class).apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.FieldLocator.Resolution.Unresolved.class).apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.InstanceFieldConstructor.class).apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.InstanceFieldConstructor.Appender.class).refine(new ObjectPropertyAssertion.Refinement<Implementation.Target>() {
            @Override
            @SuppressWarnings("unchecked")
            public void apply(Implementation.Target mock) {
                TypeDescription typeDescription = mock(TypeDescription.class);
                when(mock.getTypeDescription()).thenReturn(typeDescription);
                FieldList fieldList = mock(FieldList.class);
                FieldList filteredFieldList = mock(FieldList.class);
                when(typeDescription.getDeclaredFields()).thenReturn(fieldList);
                when(fieldList.filter(any(ElementMatcher.class))).thenReturn(filteredFieldList);
                when(filteredFieldList.getOnly()).thenReturn(mock(FieldDescription.class));
            }
        }).skipSynthetic().apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.AccessType.class).apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.AccessType.Getter.class).apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.AccessType.Getter.Appender.class).refine(new ObjectPropertyAssertion.Refinement<Implementation.Target>() {
            @Override
            public void apply(Implementation.Target mock) {
                when(mock.getTypeDescription()).thenReturn(mock(TypeDescription.class));
            }
        }).skipSynthetic().apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.AccessType.Setter.class).apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.AccessType.Setter.Appender.class).refine(new ObjectPropertyAssertion.Refinement<Implementation.Target>() {
            @Override
            public void apply(Implementation.Target mock) {
                when(mock.getTypeDescription()).thenReturn(mock(TypeDescription.class));
            }
        }).skipSynthetic().apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.AccessorProxy.class).apply();
    }

    public static class Foo {

        public Foo foo;
    }
}
