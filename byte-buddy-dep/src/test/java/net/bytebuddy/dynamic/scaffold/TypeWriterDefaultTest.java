package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.TypeManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.util.Collections;

public class TypeWriterDefaultTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test(expected = IllegalStateException.class)
    public void testConstructorOnInterfaceAssertion() throws Exception {
        new ByteBuddy()
                .makeInterface()
                .defineConstructor(Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .intercept(SuperMethodCall.INSTANCE)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testConstructorOnAnnotationAssertion() throws Exception {
        new ByteBuddy()
                .makeAnnotation()
                .defineConstructor(Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .intercept(SuperMethodCall.INSTANCE)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAbstractConstructorAssertion() throws Exception {
        new ByteBuddy()
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .defineConstructor(Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .withoutCode()
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testStaticAbstractMethodAssertion() throws Exception {
        new ByteBuddy()
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .defineMethod(FOO, void.class, Collections.<Class<?>>emptyList(), Ownership.STATIC)
                .withoutCode()
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAbstractMethodOnNonAbstractClassAssertion() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Collections.<Class<?>>emptyList())
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
    public void testNonPublicMethodOnInterfaceAssertion() throws Exception {
        new ByteBuddy()
                .makeInterface()
                .defineMethod(FOO, void.class, Collections.<Class<?>>emptyList())
                .withoutCode()
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonPublicMethodOnAnnotationAssertion() throws Exception {
        new ByteBuddy()
                .makeAnnotation()
                .defineMethod(FOO, void.class, Collections.<Class<?>>emptyList())
                .withoutCode()
                .make();
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(value = 8, type = JavaVersionRule.Type.LESS_THEN)
    public void testStaticMethodOnInterfaceAssertion() throws Exception {
        new ByteBuddy()
                .makeInterface()
                .defineMethod(FOO, String.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC, Ownership.STATIC)
                .withoutCode()
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testStaticMethodOnAnnotationAssertionJava8() throws Exception {
        new ByteBuddy()
                .makeInterface()
                .defineMethod(FOO, String.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC, Ownership.STATIC)
                .intercept(StubMethod.INSTANCE)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(value = 8, type = JavaVersionRule.Type.LESS_THEN)
    public void testStaticMethodOnAnnotationAssertion() throws Exception {
        new ByteBuddy()
                .makeAnnotation()
                .defineMethod(FOO, String.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC, Ownership.STATIC)
                .intercept(StubMethod.INSTANCE)
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testStaticMethodOnInterfaceAssertionJava8() throws Exception {
        new ByteBuddy()
                .makeAnnotation()
                .defineMethod(FOO, String.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC, Ownership.STATIC)
                .intercept(StubMethod.INSTANCE)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotationOnClassAssertion() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Collections.<Class<?>>emptyList())
                .withDefaultValue(BAR)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotationOnAbstractClassAssertion() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .modifiers(TypeManifestation.ABSTRACT)
                .defineMethod(FOO, String.class, Collections.<Class<?>>emptyList())
                .withDefaultValue(BAR)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotationOnInterfaceClassAssertion() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .modifiers(TypeManifestation.INTERFACE)
                .defineMethod(FOO, String.class, Collections.<Class<?>>emptyList())
                .withDefaultValue(BAR)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotationPropertyWithVoidReturnAssertion() throws Exception {
        new ByteBuddy()
                .makeAnnotation()
                .defineMethod(FOO, void.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .withoutCode()
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotationPropertyWithParametersAssertion() throws Exception {
        new ByteBuddy()
                .makeAnnotation()
                .defineMethod(FOO, String.class, Collections.<Class<?>>singletonList(Void.class), Visibility.PUBLIC)
                .withoutCode()
                .make();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeWriter.Default.ForCreation.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.Default.ForInlining.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.Default.ForInlining.FramePreservingRemapper.class).applyMutable();
        ObjectPropertyAssertion.of(TypeWriter.Default.ForInlining.FramePreservingRemapper.FramePreservingMethodRemapper.class)
                .create(new ObjectPropertyAssertion.Creator<String>() {
                    @Override
                    public String create() {
                        return "()V";
                    }
                }).applyMutable();
        ObjectPropertyAssertion.of(TypeWriter.Default.ValidatingClassVisitor.class).applyMutable();
        ObjectPropertyAssertion.of(TypeWriter.Default.ValidatingClassVisitor.ValidatingMethodVisitor.class).applyMutable();
        ObjectPropertyAssertion.of(TypeWriter.Default.ValidatingClassVisitor.Constraint.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.Default.ValidatingClassVisitor.Constraint.class).apply();
    }
}
