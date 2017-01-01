package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isToString;

public class MethodDelegationOtherTest {

    @Test(expected = IllegalStateException.class)
    public void testDelegationToInvisibleInstanceThrowsException() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .method(isToString())
                .intercept(MethodDelegation.to(new Foo()))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testDelegationToInvisibleFieldTypeThrowsException() throws Exception {
        new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .subclass(Object.class)
                .defineField("foo", Foo.class)
                .method(isToString())
                .intercept(MethodDelegation.toField("foo"))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDelegationWithIllegalType() throws Exception {
        MethodDelegation.to(new Object(), String.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldDoesNotExist() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .method(any())
                .intercept(MethodDelegation.toField("foo"))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotDelegateToInstanceFieldFromStaticMethod() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineField("foo", Object.class)
                .defineMethod("bar", void.class, Ownership.STATIC)
                .intercept(MethodDelegation.toField("foo"))
                .make();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodDelegation.class).apply();
        ObjectPropertyAssertion.of(MethodDelegation.WithCustomProperties.class).apply();
        ObjectPropertyAssertion.of(MethodDelegation.Appender.class).apply();
        ObjectPropertyAssertion.of(MethodDelegation.ImplementationDelegate.ForField.WithInstance.class).apply();
        ObjectPropertyAssertion.of(MethodDelegation.ImplementationDelegate.ForField.WithLookup.class).apply();
        ObjectPropertyAssertion.of(MethodDelegation.ImplementationDelegate.ForConstruction.class).apply();
        ObjectPropertyAssertion.of(MethodDelegation.ImplementationDelegate.ForStaticMethod.class).apply();
    }

    static class Foo {
        /* empty */
    }
}
