package net.bytebuddy.instrumentation.method;

import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static net.bytebuddy.utility.CustomHamcrestMatchers.containsAllOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class MethodLookupEngineDefaultTest {

    private static final String TO_STRING = "toString", FOO = "foo";

    private MethodLookupEngine methodLookupEngine;

    @Before
    public void setUp() throws Exception {
        methodLookupEngine = MethodLookupEngine.Default.Factory.INSTANCE.make();
    }

    @Test
    public void testTrivialLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        MethodList reachableMethods = methodLookupEngine.getReachableMethods(objectType);
        assertThat(reachableMethods.size(), is(objectType.getDeclaredMethods().size()));
        assertThat(reachableMethods, containsAllOf(objectType.getDeclaredMethods()));
    }

    private static class ClassOverridingToString {

        @Override
        public String toString() {
            return super.toString();
        }
    }

    @Test
    public void testClassOverrideLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription classOverridingToString = new TypeDescription.ForLoadedType(ClassOverridingToString.class);
        MethodList reachableMethods = methodLookupEngine.getReachableMethods(classOverridingToString);
        assertThat(reachableMethods, containsAllOf(classOverridingToString.getDeclaredMethods()));
        assertThat(reachableMethods, containsAllOf(objectType.getDeclaredMethods()
                .filter(isVirtualTo(classOverridingToString)).filter(not(named(TO_STRING)))));
        assertThat(reachableMethods.size(), is(classOverridingToString.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(classOverridingToString)).size() - 1));
    }

    private static interface SingleMethodInterface {

        void foo();
    }

    private static abstract class AbstractSingleInterfaceClass implements SingleMethodInterface {
        /* empty */
    }

    @Test
    public void testInterfaceLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription abstractSingleInterfaceClass = new TypeDescription.ForLoadedType(AbstractSingleInterfaceClass.class);
        TypeDescription singleMethodInterface = new TypeDescription.ForLoadedType(SingleMethodInterface.class);
        MethodList reachableMethods = methodLookupEngine.getReachableMethods(abstractSingleInterfaceClass);
        assertThat(reachableMethods, containsAllOf(abstractSingleInterfaceClass.getDeclaredMethods()));
        assertThat(reachableMethods, containsAllOf(objectType.getDeclaredMethods().filter(isVirtualTo(abstractSingleInterfaceClass))));
        assertThat(reachableMethods, containsAllOf(singleMethodInterface.getDeclaredMethods()));
        assertThat(reachableMethods.size(), is(abstractSingleInterfaceClass.getDeclaredMethods().size()
                + singleMethodInterface.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(abstractSingleInterfaceClass)).size()));
    }

    private static class SingleInterfaceClass implements SingleMethodInterface {

        @Override
        public void foo() {
            /* empty */
        }
    }

    @Test
    public void testInterfaceImplementedLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription singleInterfaceClass = new TypeDescription.ForLoadedType(SingleInterfaceClass.class);
        MethodList reachableMethods = methodLookupEngine.getReachableMethods(singleInterfaceClass);
        assertThat(reachableMethods, containsAllOf(singleInterfaceClass.getDeclaredMethods()));
        assertThat(reachableMethods, containsAllOf(objectType.getDeclaredMethods().filter(isVirtualTo(singleInterfaceClass))));
        assertThat(reachableMethods.size(), is(singleInterfaceClass.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(singleInterfaceClass)).size()));
    }

    private static interface SingleMethodOverridingInterface extends SingleMethodInterface {

        @Override
        void foo();
    }

    private static abstract class AbstractSingleMethodOverridingClass implements SingleMethodOverridingInterface {
        /* empty */
    }

    @Test
    public void testInterfaceOverrideLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription singleMethodOverridingInterface = new TypeDescription.ForLoadedType(SingleMethodOverridingInterface.class);
        TypeDescription abstractSingleMethodOverridingClass = new TypeDescription.ForLoadedType(AbstractSingleMethodOverridingClass.class);
        MethodList reachableMethods = methodLookupEngine.getReachableMethods(abstractSingleMethodOverridingClass);
        assertThat(reachableMethods, containsAllOf(abstractSingleMethodOverridingClass.getDeclaredMethods()));
        assertThat(reachableMethods, containsAllOf(singleMethodOverridingInterface.getDeclaredMethods()));
        assertThat(reachableMethods, containsAllOf(objectType.getDeclaredMethods().filter(isVirtualTo(abstractSingleMethodOverridingClass))));
        assertThat(reachableMethods.size(), is(abstractSingleMethodOverridingClass.getDeclaredMethods().size()
                + singleMethodOverridingInterface.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(abstractSingleMethodOverridingClass)).size()));
    }

    private static abstract class AbstractFoldedInterfaceClass extends AbstractSingleInterfaceClass implements SingleMethodOverridingInterface {
        /* empty */
    }

    @Test
    public void testInterfaceFoldedRedefinitionLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription singleMethodOverridingInterface = new TypeDescription.ForLoadedType(SingleMethodOverridingInterface.class);
        TypeDescription abstractFoldedInterfaceClass = new TypeDescription.ForLoadedType(AbstractFoldedInterfaceClass.class);
        MethodList reachableMethods = methodLookupEngine.getReachableMethods(abstractFoldedInterfaceClass);
        assertThat(reachableMethods, containsAllOf(abstractFoldedInterfaceClass.getDeclaredMethods()));
        assertThat(reachableMethods, containsAllOf(singleMethodOverridingInterface.getDeclaredMethods()));
        assertThat(reachableMethods, containsAllOf(objectType.getDeclaredMethods().filter(isVirtualTo(abstractFoldedInterfaceClass))));
        assertThat(reachableMethods.size(), is(abstractFoldedInterfaceClass.getDeclaredMethods().size()
                + singleMethodOverridingInterface.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(abstractFoldedInterfaceClass)).size()));
    }

    private static abstract class AbstractFoldedReverseInterfaceBaseClass implements SingleMethodOverridingInterface {
        /* empty */
    }

    private static abstract class AbstractFoldedReverseInterfaceClass extends AbstractFoldedReverseInterfaceBaseClass implements SingleMethodInterface {
        /* empty */
    }

    @Test
    public void testInterfaceFoldedReversedRedefinitionLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription singleMethodOverridingInterface = new TypeDescription.ForLoadedType(SingleMethodOverridingInterface.class);
        TypeDescription abstractFoldedReverseInterfaceClass = new TypeDescription.ForLoadedType(AbstractFoldedReverseInterfaceClass.class);
        MethodList reachableMethods = methodLookupEngine.getReachableMethods(abstractFoldedReverseInterfaceClass);
        assertThat(reachableMethods, containsAllOf(abstractFoldedReverseInterfaceClass.getDeclaredMethods()));
        assertThat(reachableMethods, containsAllOf(singleMethodOverridingInterface.getDeclaredMethods()));
        assertThat(reachableMethods, containsAllOf(objectType.getDeclaredMethods().filter(isVirtualTo(abstractFoldedReverseInterfaceClass))));
        assertThat(reachableMethods.size(), is(abstractFoldedReverseInterfaceClass.getDeclaredMethods().size()
                + singleMethodOverridingInterface.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(abstractFoldedReverseInterfaceClass)).size()));
    }

    private static interface AdditionalMethodInterface extends SingleMethodOverridingInterface {

        void bar();
    }

    private static abstract class AbstractAdditionalMethodInterfaceClass implements AdditionalMethodInterface {
        /* empty */
    }

    @Test
    public void testInterfaceInheritanceLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription singleMethodOverridingInterface = new TypeDescription.ForLoadedType(SingleMethodOverridingInterface.class);
        TypeDescription additionalMethodInterface = new TypeDescription.ForLoadedType(AdditionalMethodInterface.class);
        TypeDescription abstractAdditionalMethodInterfaceClass = new TypeDescription.ForLoadedType(AbstractAdditionalMethodInterfaceClass.class);
        MethodList reachableMethods = methodLookupEngine.getReachableMethods(abstractAdditionalMethodInterfaceClass);
        assertThat(reachableMethods, containsAllOf(abstractAdditionalMethodInterfaceClass.getDeclaredMethods()));
        assertThat(reachableMethods, containsAllOf(singleMethodOverridingInterface.getDeclaredMethods()));
        assertThat(reachableMethods, containsAllOf(additionalMethodInterface.getDeclaredMethods()));
        assertThat(reachableMethods, containsAllOf(objectType.getDeclaredMethods().filter(isVirtualTo(abstractAdditionalMethodInterfaceClass))));
        assertThat(reachableMethods.size(), is(abstractAdditionalMethodInterfaceClass.getDeclaredMethods().size()
                + singleMethodOverridingInterface.getDeclaredMethods().size()
                + additionalMethodInterface.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(abstractAdditionalMethodInterfaceClass)).size()));
    }

    private static interface ConflictingSingleMethodInterface {

        void foo();
    }

    private static abstract class ConflictingInterfaceClass implements SingleMethodInterface, ConflictingSingleMethodInterface {
        /* empty */
    }

    @Test
    public void testConflictingInterfaceLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription conflictingInterfaceClass = new TypeDescription.ForLoadedType(ConflictingInterfaceClass.class);
        MethodList reachableMethods = methodLookupEngine.getReachableMethods(conflictingInterfaceClass);
        assertThat(reachableMethods, containsAllOf(conflictingInterfaceClass.getDeclaredMethods()));
        assertThat(reachableMethods, containsAllOf(objectType.getDeclaredMethods().filter(isVirtualTo(conflictingInterfaceClass))));
        assertThat(reachableMethods.size(), is(conflictingInterfaceClass.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(conflictingInterfaceClass)).size()
                + 1));
        MethodDescription conflictMethod = reachableMethods.filter(named(FOO)).getOnly();
        assertEquals(MethodLookupEngine.ConflictingInterfaceMethod.class, conflictMethod.getClass());
        assertThat(conflictMethod.getDeclaringType(), is(conflictingInterfaceClass));
    }

    private static MethodMatcher isVirtualTo(TypeDescription typeDescription) {
        return isMethod().and(not(isPrivate().or(isStatic()).or(isPackagePrivate().and(not(isVisibleTo(typeDescription))))));
    }
}
