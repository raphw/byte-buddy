package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodAccessorFactory;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class FieldProxyBinderTest extends AbstractAnnotationBinderTest<FieldProxy> {

    private static final String FOO = "foo";

    @Mock
    private MethodDescription.InDefinedShape getterMethod, setterMethod;

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
        new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldOfPrimitiveThrowsException() throws Exception {
        doReturn(int.class).when(annotation).declaringType();
        new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
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
        new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
    }

    @Test
    public void testGetterForImplicitNamedFieldInHierarchy() throws Exception {
        when(target.getType()).thenReturn(genericGetterType);
        doReturn(void.class).when(annotation).declaringType();
        when(annotation.value()).thenReturn(FieldProxy.Binder.BEAN_PROPERTY);
        when(fieldDescription.getActualName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(genericFieldType);
        when(source.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InDefinedShape>());
        when(source.getName()).thenReturn("getFoo");
        when(source.getActualName()).thenReturn("getFoo");
        when(source.getInternalName()).thenReturn("getFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testGetterForExplicitNamedFieldInHierarchy() throws Exception {
        when(target.getType()).thenReturn(genericGetterType);
        doReturn(void.class).when(annotation).declaringType();
        when(annotation.value()).thenReturn(FOO);
        when(fieldDescription.getActualName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(genericFieldType);
        when(source.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InDefinedShape>());
        when(source.getName()).thenReturn("getFoo");
        when(source.getInternalName()).thenReturn("getFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
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
        when(source.getActualName()).thenReturn("getFoo");
        when(source.getInternalName()).thenReturn("getFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
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
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testSetterForImplicitNamedFieldInHierarchy() throws Exception {
        when(target.getType()).thenReturn(genericSetterType);
        doReturn(void.class).when(annotation).declaringType();
        when(annotation.value()).thenReturn(FieldProxy.Binder.BEAN_PROPERTY);
        when(fieldDescription.getActualName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(source.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(source, fieldType));
        when(source.getActualName()).thenReturn("setFoo");
        when(source.getInternalName()).thenReturn("setFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testSetterForExplicitNamedFieldInHierarchy() throws Exception {
        when(target.getType()).thenReturn(genericSetterType);
        doReturn(void.class).when(annotation).declaringType();
        when(annotation.value()).thenReturn(FOO);
        when(fieldDescription.getActualName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(source.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(source, fieldType));
        when(source.getName()).thenReturn("setFoo");
        when(source.getInternalName()).thenReturn("setFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testSetterForImplicitNamedFieldInNamedType() throws Exception {
        when(target.getType()).thenReturn(genericSetterType);
        doReturn(Foo.class).when(annotation).declaringType();
        when(instrumentedType.isAssignableTo(new TypeDescription.ForLoadedType(Foo.class))).thenReturn(true);
        when(annotation.value()).thenReturn(FieldProxy.Binder.BEAN_PROPERTY);
        when(fieldDescription.getActualName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(source.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(source, fieldType));
        when(source.getName()).thenReturn("setFoo");
        when(source.getActualName()).thenReturn("setFoo");
        when(source.getInternalName()).thenReturn("setFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testSetterForExplicitNamedFieldInNamedType() throws Exception {
        when(target.getType()).thenReturn(genericSetterType);
        doReturn(Foo.class).when(annotation).declaringType();
        when(instrumentedType.isAssignableTo(new TypeDescription.ForLoadedType(Foo.class))).thenReturn(true);
        when(annotation.value()).thenReturn(FOO);
        when(fieldDescription.getActualName()).thenReturn(FOO);
        when(source.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(source.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(source, fieldType));
        when(source.getName()).thenReturn("setFoo");
        when(source.getInternalName()).thenReturn("setFoo");
        when(fieldDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = new FieldProxy.Binder(getterMethod, setterMethod).bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
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
                assigner,
                Assigner.Typing.STATIC);
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
                assigner,
                Assigner.Typing.STATIC);
    }

    @Test(expected = IllegalStateException.class)
    public void testUnresolvedResolverNoProxyType() throws Exception {
        FieldProxy.Binder.FieldResolver.Unresolved.INSTANCE.getProxyType();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnresolvedResolverNoApplication() throws Exception {
        FieldProxy.Binder.FieldResolver.Unresolved.INSTANCE.apply(mock(DynamicType.Builder.class),
                mock(FieldDescription.class),
                mock(Assigner.class),
                mock(MethodAccessorFactory.class));
    }

    public static class Foo {

        public Foo foo;
    }
}
