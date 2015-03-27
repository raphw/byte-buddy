package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.ParameterList;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class FieldBinderTest extends AbstractAnnotationBinderTest<Field> {

    private static final String FOO = "foo";

    @Mock
    private MethodDescription getterMethod, setterMethod;

    @Mock
    private TypeDescription setterType, getterType, fieldType;

    @Mock
    private FieldDescription fieldDescription;

    public FieldBinderTest() {
        super(Field.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(getterMethod.getDeclaringType()).thenReturn(getterType);
        when(setterMethod.getDeclaringType()).thenReturn(setterType);
        when(instrumentedType.getDeclaredFields()).thenReturn(new FieldList.Explicit(Collections.singletonList(fieldDescription)));
        when(fieldDescription.getFieldType()).thenReturn(fieldType);
        when(fieldType.getStackSize()).thenReturn(StackSize.ZERO);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<Field> getSimpleBinder() {
        return new Field.Binder(getterMethod, setterMethod);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalType() throws Exception {
        when(target.getTypeDescription()).thenReturn(mock(TypeDescription.class));
        new Field.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                instrumentationTarget,
                assigner);
    }

    @Test
    public void testGetterForImplicitNamedFieldInHierarchy() throws Exception {
        when(target.getTypeDescription()).thenReturn(getterType);
        doReturn(void.class).when(annotation).definingType();
        when(annotation.value()).thenReturn(Field.BEAN_PROPERTY);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(fieldType);
        when(source.getParameters()).thenReturn(new ParameterList.Empty());
        when(source.getName()).thenReturn("getFoo");
        when(source.getSourceCodeName()).thenReturn("getFoo");
        when(source.getInternalName()).thenReturn("getFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new Field.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                instrumentationTarget,
                assigner);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testGetterForExplicitNamedFieldInHierarchy() throws Exception {
        when(target.getTypeDescription()).thenReturn(getterType);
        doReturn(void.class).when(annotation).definingType();
        when(annotation.value()).thenReturn(FOO);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(fieldType);
        when(source.getParameters()).thenReturn(new ParameterList.Empty());
        when(source.getName()).thenReturn("getFoo");
        when(source.getInternalName()).thenReturn("getFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new Field.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                instrumentationTarget,
                assigner);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testGetterForImplicitNamedFieldInNamedType() throws Exception {
        when(target.getTypeDescription()).thenReturn(getterType);
        doReturn(Foo.class).when(annotation).definingType();
        when(annotation.value()).thenReturn(Field.BEAN_PROPERTY);
        when(fieldDescription.getInternalName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(fieldType);
        when(source.getParameters()).thenReturn(new ParameterList.Empty());
        when(source.getName()).thenReturn("getFoo");
        when(source.getSourceCodeName()).thenReturn("getFoo");
        when(source.getInternalName()).thenReturn("getFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new Field.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                instrumentationTarget,
                assigner);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testGetterForExplicitNamedFieldInNamedType() throws Exception {
        when(target.getTypeDescription()).thenReturn(getterType);
        doReturn(Foo.class).when(annotation).definingType();
        when(annotation.value()).thenReturn(FOO);
        when(fieldDescription.getInternalName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(fieldType);
        when(source.getParameters()).thenReturn(new ParameterList.Empty());
        when(source.getName()).thenReturn("getFoo");
        when(source.getInternalName()).thenReturn("getFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new Field.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                instrumentationTarget,
                assigner);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testSetterForImplicitNamedFieldInHierarchy() throws Exception {
        when(target.getTypeDescription()).thenReturn(setterType);
        doReturn(void.class).when(annotation).definingType();
        when(annotation.value()).thenReturn(Field.BEAN_PROPERTY);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(new TypeDescription.ForLoadedType(void.class));
        ParameterList parameterList = ParameterList.Explicit.latent(source, Collections.singletonList(fieldType));
        when(source.getParameters()).thenReturn(parameterList);
        when(source.getSourceCodeName()).thenReturn("setFoo");
        when(source.getInternalName()).thenReturn("setFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new Field.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                instrumentationTarget,
                assigner);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testSetterForExplicitNamedFieldInHierarchy() throws Exception {
        when(target.getTypeDescription()).thenReturn(setterType);
        doReturn(void.class).when(annotation).definingType();
        when(annotation.value()).thenReturn(FOO);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(new TypeDescription.ForLoadedType(void.class));
        ParameterList parameterList = ParameterList.Explicit.latent(source, Collections.singletonList(fieldType));
        when(source.getParameters()).thenReturn(parameterList);
        when(source.getName()).thenReturn("setFoo");
        when(source.getInternalName()).thenReturn("setFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new Field.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                instrumentationTarget,
                assigner);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testSetterForImplicitNamedFieldInNamedType() throws Exception {
        when(target.getTypeDescription()).thenReturn(setterType);
        doReturn(Foo.class).when(annotation).definingType();
        when(annotation.value()).thenReturn(Field.BEAN_PROPERTY);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(new TypeDescription.ForLoadedType(void.class));
        ParameterList parameterList = ParameterList.Explicit.latent(source, Collections.singletonList(fieldType));
        when(source.getParameters()).thenReturn(parameterList);
        when(source.getName()).thenReturn("setFoo");
        when(source.getSourceCodeName()).thenReturn("setFoo");
        when(source.getInternalName()).thenReturn("setFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new Field.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                instrumentationTarget,
                assigner);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testSetterForExplicitNamedFieldInNamedType() throws Exception {
        when(target.getTypeDescription()).thenReturn(setterType);
        doReturn(Foo.class).when(annotation).definingType();
        when(annotation.value()).thenReturn(FOO);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(new TypeDescription.ForLoadedType(void.class));
        ParameterList parameterList = ParameterList.Explicit.latent(source, Collections.singletonList(fieldType));
        when(source.getParameters()).thenReturn(parameterList);
        when(source.getName()).thenReturn("setFoo");
        when(source.getInternalName()).thenReturn("setFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new Field.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                instrumentationTarget,
                assigner);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Field.Binder.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.StaticFieldConstructor.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.FieldLocator.Legal.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.FieldLocator.Illegal.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.FieldLocator.LookupEngine.ForHierarchy.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.FieldLocator.LookupEngine.ForExplicitType.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.FieldLocator.LookupEngine.Illegal.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.FieldLocator.Resolution.Resolved.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.FieldLocator.Resolution.Unresolved.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.InstanceFieldConstructor.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.InstanceFieldConstructor.Appender.class).refine(new ObjectPropertyAssertion.Refinement<Instrumentation.Target>() {
            @Override
            @SuppressWarnings("unchecked")
            public void apply(Instrumentation.Target mock) {
                TypeDescription typeDescription = mock(TypeDescription.class);
                when(mock.getTypeDescription()).thenReturn(typeDescription);
                FieldList fieldList = mock(FieldList.class);
                FieldList filteredFieldList = mock(FieldList.class);
                when(typeDescription.getDeclaredFields()).thenReturn(fieldList);
                when(fieldList.filter(any(ElementMatcher.class))).thenReturn(filteredFieldList);
                when(filteredFieldList.getOnly()).thenReturn(mock(FieldDescription.class));
            }
        }).skipSynthetic().apply();
        ObjectPropertyAssertion.of(Field.Binder.AccessType.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.AccessType.Getter.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.AccessType.Getter.Appender.class).refine(new ObjectPropertyAssertion.Refinement<Instrumentation.Target>() {
            @Override
            public void apply(Instrumentation.Target mock) {
                when(mock.getTypeDescription()).thenReturn(mock(TypeDescription.class));
            }
        }).skipSynthetic().apply();
        ObjectPropertyAssertion.of(Field.Binder.AccessType.Setter.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.AccessType.Setter.Appender.class).refine(new ObjectPropertyAssertion.Refinement<Instrumentation.Target>() {
            @Override
            public void apply(Instrumentation.Target mock) {
                when(mock.getTypeDescription()).thenReturn(mock(TypeDescription.class));
            }
        }).skipSynthetic().apply();
        ObjectPropertyAssertion.of(Field.Binder.AccessorProxy.class).apply();
    }

    public static class Foo {

        public Foo foo;
    }
}
