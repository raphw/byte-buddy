package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.TypeManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.security.acl.Owner;
import java.util.Collections;

public class TypeWriterDefaultTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

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
                .defineField(FOO, String.class, Visibility.PUBLIC, Ownership.STATIC)
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(value = 8)
    public void testStaticMethodOnAnnotationAssertionJava8() throws Exception {
        new ByteBuddy()
                .makeInterface()
                .defineField(FOO, String.class, Visibility.PUBLIC, Ownership.STATIC)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(value = 8, type = JavaVersionRule.Type.LESS_THEN)
    public void testStaticMethodOnAnnotationAssertion() throws Exception {
        new ByteBuddy()
                .makeAnnotation()
                .defineField(FOO, String.class, Visibility.PUBLIC, Ownership.STATIC)
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(value = 8)
    public void testStaticMethodOnInterfaceAssertionJava8() throws Exception {
        new ByteBuddy()
                .makeAnnotation()
                .defineField(FOO, String.class, Visibility.PUBLIC, Ownership.STATIC)
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

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeWriter.Default.ForCreation.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.Default.ForInlining.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.Default.ValidatingClassVisitor.class).applyMutable();
        ObjectPropertyAssertion.of(TypeWriter.Default.ValidatingClassVisitor.ValidatingMethodVisitor.class).applyMutable();
    }
}
