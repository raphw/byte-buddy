package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

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
    private TypeDescription.Generic genericSetterType, genericGetterType, genericFieldType;

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
        when(instrumentedType.getDeclaredFields()).thenReturn(new FieldList.Explicit<FieldDescription.InDefinedShape>(fieldDescription));
        when(fieldDescription.getType()).thenReturn(genericFieldType);
        when(genericFieldType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(genericFieldType.getStackSize()).thenReturn(StackSize.ZERO);
        when(genericFieldType.asErasure()).thenReturn(fieldType);
        when(fieldType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(fieldType.asErasure()).thenReturn(fieldType);
        when(fieldType.getInternalName()).thenReturn(FOO);
        when(genericSetterType.asErasure()).thenReturn(setterType);
        when(genericGetterType.asErasure()).thenReturn(getterType);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldProxy> getSimpleBinder() {
        return new FieldProxy.Binder(getterMethod, setterMethod);
    }


    @Test(expected = IllegalStateException.class)
    public void testFieldOfArrayThrowsException() throws Exception {
        doReturn(Object[].class).when(annotation).declaringType();
        new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription, source, target, implementationTarget, assigner);
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldOfPrimitiveThrowsException() throws Exception {
        doReturn(int.class).when(annotation).declaringType();
        new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription, source, target, implementationTarget, assigner);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalType() throws Exception {
        doReturn(Foo.class).when(annotation).declaringType();
        when(annotation.value()).thenReturn(FOO);
        TypeDescription targetType = mock(TypeDescription.class);
        TypeDescription.Generic genericTargetType = mock(TypeDescription.Generic.class);
        when(genericTargetType.asErasure()).thenReturn(targetType);
        when(target.getType()).thenReturn(genericTargetType);
        when(instrumentedType.isAssignableTo(new TypeDescription.ForLoadedType(Foo.class))).thenReturn(true);
        new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner);
    }

    @Test
    public void testGetterForImplicitNamedFieldInHierarchy() throws Exception {
        when(target.getType()).thenReturn(genericGetterType);
        doReturn(void.class).when(annotation).declaringType();
        when(annotation.value()).thenReturn(FieldProxy.Binder.BEAN_PROPERTY);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(genericFieldType);
        when(source.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InDefinedShape>());
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
        when(target.getType()).thenReturn(genericGetterType);
        doReturn(void.class).when(annotation).declaringType();
        when(annotation.value()).thenReturn(FOO);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(genericFieldType);
        when(source.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InDefinedShape>());
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
        when(target.getType()).thenReturn(genericGetterType);
        doReturn(Foo.class).when(annotation).declaringType();
        when(instrumentedType.isAssignableTo(new TypeDescription.ForLoadedType(Foo.class))).thenReturn(true);
        when(annotation.value()).thenReturn(FieldProxy.Binder.BEAN_PROPERTY);
        when(fieldDescription.getInternalName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(genericFieldType);
        when(source.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InDefinedShape>());
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
        when(target.getType()).thenReturn(genericGetterType);
        doReturn(Foo.class).when(annotation).declaringType();
        when(instrumentedType.isAssignableTo(new TypeDescription.ForLoadedType(Foo.class))).thenReturn(true);
        when(annotation.value()).thenReturn(FOO);
        when(fieldDescription.getInternalName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(genericFieldType);
        when(source.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InDefinedShape>());
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
        when(target.getType()).thenReturn(genericSetterType);
        doReturn(void.class).when(annotation).declaringType();
        when(annotation.value()).thenReturn(FieldProxy.Binder.BEAN_PROPERTY);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(source.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(source, fieldType));
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
        when(target.getType()).thenReturn(genericSetterType);
        doReturn(void.class).when(annotation).declaringType();
        when(annotation.value()).thenReturn(FOO);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(source.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(source, fieldType));
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
        when(target.getType()).thenReturn(genericSetterType);
        doReturn(Foo.class).when(annotation).declaringType();
        when(instrumentedType.isAssignableTo(new TypeDescription.ForLoadedType(Foo.class))).thenReturn(true);
        when(annotation.value()).thenReturn(FieldProxy.Binder.BEAN_PROPERTY);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(source.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(source, fieldType));
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
        when(target.getType()).thenReturn(genericSetterType);
        doReturn(Foo.class).when(annotation).declaringType();
        when(instrumentedType.isAssignableTo(new TypeDescription.ForLoadedType(Foo.class))).thenReturn(true);
        when(annotation.value()).thenReturn(FOO);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(source.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(source, fieldType));
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
    public void testDefiningTypeNotAssignable() throws Exception {
        when(target.getType()).thenReturn(genericSetterType);
        doReturn(Foo.class).when(annotation).declaringType();
        when(instrumentedType.isAssignableTo(new TypeDescription.ForLoadedType(Foo.class))).thenReturn(false);
        MethodDelegationBinder.ParameterBinding<?> binding = new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner);
        assertThat(binding.isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testDefiningTypePrimitive() throws Exception {
        when(target.getType()).thenReturn(genericSetterType);
        doReturn(int.class).when(annotation).declaringType();
        new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldProxy.Binder.class).apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.StaticFieldConstructor.class).apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.InstanceFieldConstructor.class).apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.InstanceFieldConstructor.Appender.class).refine(new ObjectPropertyAssertion.Refinement<Implementation.Target>() {
            @Override
            @SuppressWarnings("unchecked")
            public void apply(Implementation.Target mock) {
                TypeDescription typeDescription = mock(TypeDescription.class);
                when(mock.getInstrumentedType()).thenReturn(typeDescription);
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
                when(mock.getInstrumentedType()).thenReturn(mock(TypeDescription.class));
            }
        }).skipSynthetic().apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.AccessType.Setter.class).apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.AccessType.Setter.Appender.class).refine(new ObjectPropertyAssertion.Refinement<Implementation.Target>() {
            @Override
            public void apply(Implementation.Target mock) {
                when(mock.getInstrumentedType()).thenReturn(mock(TypeDescription.class));
            }
        }).skipSynthetic().apply();
        ObjectPropertyAssertion.of(FieldProxy.Binder.AccessorProxy.class).apply();
    }

    public static class Foo {

        public Foo foo;
    }
}
