package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.TypeManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.JavaConstant;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypeWriterDefaultTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final String LEGACY_INTERFACE = "net.bytebuddy.test.precompiled.LegacyInterface";

    private static final String JAVA_8_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodInterface";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test(expected = IllegalStateException.class)
    public void testConstructorOnInterfaceAssertion() throws Exception {
        new ByteBuddy()
                .makeInterface()
                .defineConstructor(Visibility.PUBLIC)
                .intercept(SuperMethodCall.INSTANCE)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testConstructorOnAnnotationAssertion() throws Exception {
        new ByteBuddy()
                .makeAnnotation()
                .defineConstructor(Visibility.PUBLIC)
                .intercept(SuperMethodCall.INSTANCE)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAbstractConstructorAssertion() throws Exception {
        new ByteBuddy()
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .defineConstructor(Visibility.PUBLIC)
                .withoutCode()
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testStaticAbstractMethodAssertion() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, void.class, Ownership.STATIC)
                .withoutCode()
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testPrivateAbstractMethodAssertion() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, void.class, Visibility.PRIVATE)
                .withoutCode()
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAbstractMethodOnNonAbstractClassAssertion() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class)
                .withoutCode()
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonPublicFieldOnInterfaceAssertion() throws Exception {
        new ByteBuddy()
                .makeInterface()
                .defineField(FOO, String.class, Ownership.STATIC)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonPublicFieldOnAnnotationAssertion() throws Exception {
        new ByteBuddy()
                .makeAnnotation()
                .defineField(FOO, String.class, Ownership.STATIC)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonStaticFieldOnInterfaceAssertion() throws Exception {
        new ByteBuddy()
                .makeInterface()
                .defineField(FOO, String.class, Visibility.PUBLIC)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonStaticFieldOnAnnotationAssertion() throws Exception {
        new ByteBuddy()
                .makeAnnotation()
                .defineField(FOO, String.class, Visibility.PUBLIC)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testStaticFieldWithIncompatibleConstantValue() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, String.class, Ownership.STATIC)
                .value(0)
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStaticFieldWithNullConstantValue() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, String.class, Ownership.STATIC)
                .value(null);
    }

    @Test(expected = IllegalStateException.class)
    public void testStaticNumericFieldWithIncompatibleConstantValue() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, boolean.class, Ownership.STATIC)
                .value(Integer.MAX_VALUE)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testStaticFieldWithNonDefinableConstantValue() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Ownership.STATIC)
                .value(FOO)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonPublicMethodOnInterfaceAssertion() throws Exception {
        new ByteBuddy()
                .makeInterface()
                .defineMethod(FOO, void.class)
                .withoutCode()
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonPublicMethodOnAnnotationAssertion() throws Exception {
        new ByteBuddy()
                .makeAnnotation()
                .defineMethod(FOO, void.class)
                .withoutCode()
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testStaticMethodOnInterfaceAssertion() throws Exception {
        new ByteBuddy(ClassFileVersion.JAVA_V6)
                .makeInterface()
                .defineMethod(FOO, String.class, Visibility.PUBLIC, Ownership.STATIC)
                .withoutCode()
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testStaticMethodOnAnnotationAssertionJava8() throws Exception {
        new ByteBuddy()
                .makeInterface()
                .defineMethod(FOO, String.class, Visibility.PUBLIC, Ownership.STATIC)
                .intercept(StubMethod.INSTANCE)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testStaticMethodOnAnnotationAssertion() throws Exception {
        new ByteBuddy(ClassFileVersion.JAVA_V6)
                .makeAnnotation()
                .defineMethod(FOO, String.class, Visibility.PUBLIC, Ownership.STATIC)
                .intercept(StubMethod.INSTANCE)
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testStaticMethodOnInterfaceAssertionJava8() throws Exception {
        new ByteBuddy()
                .makeAnnotation()
                .defineMethod(FOO, String.class, Visibility.PUBLIC, Ownership.STATIC)
                .intercept(StubMethod.INSTANCE)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotationDefaultValueOnClassAssertion() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class)
                .defaultValue(BAR)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotationDefaultValueOnInterfaceClassAssertion() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .modifiers(TypeManifestation.INTERFACE)
                .defineMethod(FOO, String.class)
                .defaultValue(BAR)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotationPropertyWithVoidReturnAssertion() throws Exception {
        new ByteBuddy()
                .makeAnnotation()
                .defineMethod(FOO, void.class, Visibility.PUBLIC)
                .withoutCode()
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotationPropertyWithParametersAssertion() throws Exception {
        new ByteBuddy()
                .makeAnnotation()
                .defineMethod(FOO, String.class, Visibility.PUBLIC).withParameters(Void.class)
                .withoutCode()
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testPackageDescriptionWithModifiers() throws Exception {
        new ByteBuddy()
                .makePackage(FOO)
                .modifiers(Visibility.PRIVATE)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testPackageDescriptionWithInterfaces() throws Exception {
        new ByteBuddy()
                .makePackage(FOO)
                .implement(Serializable.class)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testPackageDescriptionWithField() throws Exception {
        new ByteBuddy()
                .makePackage(FOO)
                .defineField(FOO, Void.class)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testPackageDescriptionWithMethod() throws Exception {
        new ByteBuddy()
                .makePackage(FOO)
                .defineMethod(FOO, void.class)
                .withoutCode()
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotationPreJava5TypeAssertion() throws Exception {
        new ByteBuddy(ClassFileVersion.JAVA_V4)
                .makeAnnotation()
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotationOnTypePreJava5TypeAssertion() throws Exception {
        new ByteBuddy(ClassFileVersion.JAVA_V4)
                .subclass(Object.class)
                .annotateType(AnnotationDescription.Builder.ofType(Foo.class).build())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotationOnFieldPreJava5TypeAssertion() throws Exception {
        new ByteBuddy(ClassFileVersion.JAVA_V4)
                .subclass(Object.class)
                .defineField(FOO, Void.class)
                .annotateField(AnnotationDescription.Builder.ofType(Foo.class).build())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotationOnMethodPreJava5TypeAssertion() throws Exception {
        new ByteBuddy(ClassFileVersion.JAVA_V4)
                .subclass(Object.class)
                .defineMethod(FOO, void.class)
                .intercept(StubMethod.INSTANCE)
                .annotateMethod(AnnotationDescription.Builder.ofType(Foo.class).build())
                .make();
    }

    @Test
    public void testTypeInitializerOnInterface() throws Exception {
        assertThat(new ByteBuddy()
                .makeInterface()
                .invokable(isTypeInitializer())
                .intercept(StubMethod.INSTANCE)
                .make(), notNullValue(DynamicType.class));
    }

    @Test
    public void testTypeInitializerOnAnnotation() throws Exception {
        assertThat(new ByteBuddy()
                .makeAnnotation()
                .invokable(isTypeInitializer())
                .intercept(StubMethod.INSTANCE)
                .make(), notNullValue(DynamicType.class));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testTypeInitializerOnRebasedModernInterface() throws Exception {
        assertThat(new ByteBuddy()
                .rebase(Class.forName(JAVA_8_INTERFACE))
                .invokable(isTypeInitializer())
                .intercept(StubMethod.INSTANCE)
                .make(), notNullValue(DynamicType.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeInitializerOnRebasedLegacyInterface() throws Exception {
        new ByteBuddy()
                .rebase(Class.forName(LEGACY_INTERFACE))
                .invokable(isTypeInitializer())
                .intercept(StubMethod.INSTANCE)
                .make();
    }

    @Test
    public void testTypeInLegacyConstantPoolRemapped() throws Exception {
        Class<?> dynamicType = new ByteBuddy(ClassFileVersion.JAVA_V4)
                .with(TypeValidation.DISABLED)
                .subclass(Object.class)
                .defineMethod(FOO, Object.class, Visibility.PUBLIC)
                .intercept(FixedValue.value(Object.class))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethod(FOO).invoke(dynamicType.newInstance()), is((Object) Object.class));
    }

    @Test
    public void testArrayTypeInLegacyConstantPoolRemapped() throws Exception {
        Class<?> dynamicType = new ByteBuddy(ClassFileVersion.JAVA_V4)
                .with(TypeValidation.DISABLED)
                .subclass(Object.class)
                .defineMethod(FOO, Object.class, Visibility.PUBLIC)
                .intercept(FixedValue.value(Object[].class))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethod(FOO).invoke(dynamicType.newInstance()), is((Object) Object[].class));
    }

    @Test
    public void testPrimitiveTypeInLegacyConstantPoolRemapped() throws Exception {
        Class<?> dynamicType = new ByteBuddy(ClassFileVersion.JAVA_V4)
                .with(TypeValidation.DISABLED)
                .subclass(Object.class)
                .defineMethod(FOO, Object.class, Visibility.PUBLIC)
                .intercept(FixedValue.value(int.class))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethod(FOO).invoke(dynamicType.newInstance()), is((Object) int.class));
    }

    @Test
    public void testLegacyTypeRedefinitionIsDiscovered() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .redefine(Class.forName("net.bytebuddy.test.precompiled.TypeConstantSample"))
                .method(named(BAR))
                .intercept(FixedValue.value(int.class))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethod(BAR).invoke(null), is((Object) int.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodTypeInLegacyConstantPool() throws Exception {
        new ByteBuddy(ClassFileVersion.JAVA_V4)
                .subclass(Object.class)
                .defineMethod(FOO, Object.class)
                .intercept(FixedValue.value(JavaConstant.MethodType.of(Object.class, Object.class)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodHandleInLegacyConstantPool() throws Exception {
        new ByteBuddy(ClassFileVersion.JAVA_V4)
                .subclass(Object.class)
                .defineMethod(FOO, Object.class)
                .intercept(FixedValue.value(JavaConstant.MethodHandle.of(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString")))))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(8)
    public void testDefaultMethodCallFromLegacyType() throws Exception {
        new ByteBuddy(ClassFileVersion.JAVA_V7)
                .subclass(Class.forName("net.bytebuddy.test.precompiled.SingleDefaultMethodInterface"))
                .method(isDefaultMethod())
                .intercept(SuperMethodCall.INSTANCE)
                .make();
    }

    @Test
    public void testBridgeNonLegacyType() throws Exception {
        Class<?> base = new ByteBuddy(ClassFileVersion.JAVA_V5)
                .subclass(Object.class)
                .modifiers(Visibility.PACKAGE_PRIVATE)
                .defineMethod("foo", void.class, Visibility.PUBLIC).intercept(StubMethod.INSTANCE)
                .defineMethod("bar", Object.class).intercept(StubMethod.INSTANCE)
                .defineMethod("bar", String.class).intercept(StubMethod.INSTANCE)
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Class<?> subclass = new ByteBuddy(ClassFileVersion.JAVA_V5)
                .subclass(base)
                .modifiers(Visibility.PUBLIC)
                .method(named("bar")).intercept(StubMethod.INSTANCE)
                .make()
                .load(base.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(subclass.getDeclaredMethods().length, is(3));
        assertThat(subclass.getDeclaredMethod("foo").isBridge(), is(true));
        assertThat(subclass.getDeclaredMethod("bar").isBridge(), is(false));
        assertThat(subclass.getDeclaredMethod("bar").getReturnType(), is((Object) String.class));
    }

    @Test
    public void testNoBridgeLegacyType() throws Exception {
        Class<?> base = new ByteBuddy(ClassFileVersion.JAVA_V4)
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .modifiers(Visibility.PACKAGE_PRIVATE)
                .defineConstructor(Visibility.PUBLIC).intercept(SuperMethodCall.INSTANCE)
                .defineMethod("foo", void.class, Visibility.PUBLIC).intercept(StubMethod.INSTANCE)
                .defineMethod("bar", Object.class).intercept(StubMethod.INSTANCE)
                .defineMethod("bar", String.class).intercept(StubMethod.INSTANCE)
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Class<?> subclass = new ByteBuddy(ClassFileVersion.JAVA_V4)
                .subclass(base)
                .modifiers(Visibility.PUBLIC)
                .method(named("bar")).intercept(StubMethod.INSTANCE)
                .make()
                .load(base.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(subclass.getDeclaredMethods().length, is(1));
        assertThat(subclass.getDeclaredMethod("bar").isBridge(), is(false));
        assertThat(subclass.getDeclaredMethod("bar").getReturnType(), is((Object) String.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeWriter.Default.UnresolvedType.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.Default.ForCreation.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.Default.ForInlining.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.Default.ForInlining.ContextRegistry.class).applyBasic();
        ObjectPropertyAssertion.of(TypeWriter.Default.ForInlining.RedefinitionClassVisitor.class).create(new ObjectPropertyAssertion.Creator<TypeWriter.Default.ForInlining<?>>() {
            @Override
            @SuppressWarnings("unchecked")
            public TypeWriter.Default.ForInlining<?> create() {
                TypeWriter.Default.ForInlining<?> inlining = mock(TypeWriter.Default.ForInlining.class);
                TypeDescription typeDescription = mock(TypeDescription.class);
                when(typeDescription.getDeclaredFields()).thenReturn(new FieldList.Empty<FieldDescription.InDefinedShape>());
                try {
                    Field instrumentedMethods = TypeWriter.Default.class.getDeclaredField("instrumentedMethods");
                    Field instrumentedType = TypeWriter.Default.class.getDeclaredField("instrumentedType");
                    instrumentedMethods.setAccessible(true);
                    instrumentedType.setAccessible(true);
                    instrumentedMethods.set(inlining, new MethodList.Empty<MethodDescription>());
                    instrumentedType.set(inlining, typeDescription);
                    return inlining;
                } catch (Exception ignored) {
                    throw new AssertionError();
                }
            }
        }).applyBasic();
        ObjectPropertyAssertion.of(TypeWriter.Default.ForInlining.RedefinitionClassVisitor.AttributeObtainingFieldVisitor.class).applyBasic();
        ObjectPropertyAssertion.of(TypeWriter.Default.ForInlining.RedefinitionClassVisitor.AttributeObtainingMethodVisitor.class).applyBasic();
        ObjectPropertyAssertion.of(TypeWriter.Default.ForInlining.RedefinitionClassVisitor.CodePreservingMethodVisitor.class).applyBasic();
        ObjectPropertyAssertion.of(TypeWriter.Default.ForInlining.RedefinitionClassVisitor.TypeInitializerInjection.class).applyBasic();
        ObjectPropertyAssertion.of(TypeWriter.Default.ValidatingClassVisitor.class).applyBasic();
        ObjectPropertyAssertion.of(TypeWriter.Default.ValidatingClassVisitor.ValidatingFieldVisitor.class).applyBasic();
        ObjectPropertyAssertion.of(TypeWriter.Default.ValidatingClassVisitor.ValidatingMethodVisitor.class).applyBasic();
        ObjectPropertyAssertion.of(TypeWriter.Default.ValidatingClassVisitor.Constraint.ForAnnotation.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.Default.ValidatingClassVisitor.Constraint.ForInterface.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.Default.ValidatingClassVisitor.Constraint.ForClass.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.Default.ValidatingClassVisitor.Constraint.ForPackageType.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.Default.ValidatingClassVisitor.Constraint.ForClassFileVersion.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.Default.ValidatingClassVisitor.Constraint.Compound.class).apply();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Foo {
        /* empty */
    }
}
