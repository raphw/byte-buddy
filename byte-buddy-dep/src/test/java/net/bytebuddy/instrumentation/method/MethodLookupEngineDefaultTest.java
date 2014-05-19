package net.bytebuddy.instrumentation.method;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.precompiled.*;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.not;
import static net.bytebuddy.utility.CustomHamcrestMatchers.containsAllOf;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class MethodLookupEngineDefaultTest {

    private static final String TO_STRING = "toString", FOO = "foo";

    private MethodLookupEngine methodLookupEngine;

    private static MethodMatcher isVirtualTo(TypeDescription typeDescription) {
        return isMethod().and(not(isPrivate().or(isStatic()).or(isPackagePrivate().and(not(isVisibleTo(typeDescription))))));
    }

    @Before
    public void setUp() throws Exception {
        methodLookupEngine = MethodLookupEngine.Default.Factory.INSTANCE.make(ClassFileVersion.JAVA_V8);
    }

    @Test
    public void testTrivialLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(objectType);
        assertThat(finding.getLookedUpType(), is(objectType));
        assertThat(finding.getInvokableMethods().size(), is(objectType.getDeclaredMethods().size()));
        assertThat(finding.getInvokableMethods(), containsAllOf(objectType.getDeclaredMethods()));
        assertThat(finding.getInvokableDefaultMethods().size(), is(0));
    }

    @Test
    public void testClassOverrideLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription classOverridingToString = new TypeDescription.ForLoadedType(ClassOverridingToString.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(classOverridingToString);
        assertThat(finding.getLookedUpType(), is(classOverridingToString));
        assertThat(finding.getInvokableMethods(), containsAllOf(classOverridingToString.getDeclaredMethods()));
        assertThat(finding.getInvokableMethods(), containsAllOf(objectType.getDeclaredMethods()
                .filter(isVirtualTo(classOverridingToString)).filter(not(named(TO_STRING)))));
        assertThat(finding.getInvokableMethods().size(), is(classOverridingToString.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(classOverridingToString)).size() - 1));
        assertThat(finding.getInvokableDefaultMethods().size(), is(0));
    }

    @Test
    public void testInterfaceLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription abstractSingleInterfaceClass = new TypeDescription.ForLoadedType(AbstractSingleInterfaceClass.class);
        TypeDescription singleMethodInterface = new TypeDescription.ForLoadedType(SingleMethodInterface.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(abstractSingleInterfaceClass);
        assertThat(finding.getLookedUpType(), is(abstractSingleInterfaceClass));
        assertThat(finding.getInvokableMethods(), containsAllOf(abstractSingleInterfaceClass.getDeclaredMethods()));
        assertThat(finding.getInvokableMethods(), containsAllOf(objectType.getDeclaredMethods().filter(isVirtualTo(abstractSingleInterfaceClass))));
        assertThat(finding.getInvokableMethods(), containsAllOf(singleMethodInterface.getDeclaredMethods()));
        assertThat(finding.getInvokableMethods().size(), is(abstractSingleInterfaceClass.getDeclaredMethods().size()
                + singleMethodInterface.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(abstractSingleInterfaceClass)).size()));
        assertThat(finding.getInvokableDefaultMethods().size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(singleMethodInterface).size(), is(0));
    }

    @Test
    public void testInterfaceImplementedLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription singleInterfaceClass = new TypeDescription.ForLoadedType(SingleInterfaceClass.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleInterfaceClass);
        assertThat(finding.getLookedUpType(), is(singleInterfaceClass));
        assertThat(finding.getInvokableMethods(), containsAllOf(singleInterfaceClass.getDeclaredMethods()));
        assertThat(finding.getInvokableMethods(), containsAllOf(objectType.getDeclaredMethods().filter(isVirtualTo(singleInterfaceClass))));
        assertThat(finding.getInvokableMethods().size(), is(singleInterfaceClass.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(singleInterfaceClass)).size()));
        assertThat(finding.getInvokableDefaultMethods().size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(new TypeDescription.ForLoadedType(SingleMethodInterface.class)).size(), is(0));
    }

    @Test
    public void testInterfaceOverrideLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription singleMethodOverridingInterface = new TypeDescription.ForLoadedType(SingleMethodOverridingInterface.class);
        TypeDescription abstractSingleMethodOverridingClass = new TypeDescription.ForLoadedType(AbstractSingleMethodOverridingClass.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(abstractSingleMethodOverridingClass);
        assertThat(finding.getLookedUpType(), is(abstractSingleMethodOverridingClass));
        assertThat(finding.getInvokableMethods(), containsAllOf(abstractSingleMethodOverridingClass.getDeclaredMethods()));
        assertThat(finding.getInvokableMethods(), containsAllOf(singleMethodOverridingInterface.getDeclaredMethods()));
        assertThat(finding.getInvokableMethods(), containsAllOf(objectType.getDeclaredMethods().filter(isVirtualTo(abstractSingleMethodOverridingClass))));
        assertThat(finding.getInvokableMethods().size(), is(abstractSingleMethodOverridingClass.getDeclaredMethods().size()
                + singleMethodOverridingInterface.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(abstractSingleMethodOverridingClass)).size()));
        assertThat(finding.getInvokableDefaultMethods().size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(singleMethodOverridingInterface).size(), is(0));
    }

    @Test
    public void testInterfaceFoldedRedefinitionLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription singleMethodOverridingInterface = new TypeDescription.ForLoadedType(SingleMethodOverridingInterface.class);
        TypeDescription abstractFoldedInterfaceClass = new TypeDescription.ForLoadedType(AbstractFoldedInterfaceClass.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(abstractFoldedInterfaceClass);
        assertThat(finding.getLookedUpType(), is(abstractFoldedInterfaceClass));
        assertThat(finding.getInvokableMethods(), containsAllOf(abstractFoldedInterfaceClass.getDeclaredMethods()));
        assertThat(finding.getInvokableMethods(), containsAllOf(singleMethodOverridingInterface.getDeclaredMethods()));
        assertThat(finding.getInvokableMethods(), containsAllOf(objectType.getDeclaredMethods().filter(isVirtualTo(abstractFoldedInterfaceClass))));
        assertThat(finding.getInvokableMethods().size(), is(abstractFoldedInterfaceClass.getDeclaredMethods().size()
                + singleMethodOverridingInterface.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(abstractFoldedInterfaceClass)).size()));
        assertThat(finding.getInvokableDefaultMethods().size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(singleMethodOverridingInterface).size(), is(0));
    }

    @Test
    public void testInterfaceFoldedReversedRedefinitionLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription singleMethodOverridingInterface = new TypeDescription.ForLoadedType(SingleMethodOverridingInterface.class);
        TypeDescription abstractFoldedReverseInterfaceClass = new TypeDescription.ForLoadedType(AbstractFoldedReverseInterfaceClass.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(abstractFoldedReverseInterfaceClass);
        assertThat(finding.getLookedUpType(), is(abstractFoldedReverseInterfaceClass));
        assertThat(finding.getInvokableMethods(), containsAllOf(abstractFoldedReverseInterfaceClass.getDeclaredMethods()));
        assertThat(finding.getInvokableMethods(), containsAllOf(singleMethodOverridingInterface.getDeclaredMethods()));
        assertThat(finding.getInvokableMethods(), containsAllOf(objectType.getDeclaredMethods().filter(isVirtualTo(abstractFoldedReverseInterfaceClass))));
        assertThat(finding.getInvokableMethods().size(), is(abstractFoldedReverseInterfaceClass.getDeclaredMethods().size()
                + singleMethodOverridingInterface.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(abstractFoldedReverseInterfaceClass)).size()));
        assertThat(finding.getInvokableDefaultMethods().size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(new TypeDescription.ForLoadedType(SingleMethodInterface.class)).size(), is(0));
    }

    @Test
    public void testInterfaceInheritanceLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription singleMethodOverridingInterface = new TypeDescription.ForLoadedType(SingleMethodOverridingInterface.class);
        TypeDescription additionalMethodInterface = new TypeDescription.ForLoadedType(AdditionalMethodInterface.class);
        TypeDescription abstractAdditionalMethodInterfaceClass = new TypeDescription.ForLoadedType(AbstractAdditionalMethodInterfaceClass.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(abstractAdditionalMethodInterfaceClass);
        assertThat(finding.getLookedUpType(), is(abstractAdditionalMethodInterfaceClass));
        assertThat(finding.getInvokableMethods(), containsAllOf(abstractAdditionalMethodInterfaceClass.getDeclaredMethods()));
        assertThat(finding.getInvokableMethods(), containsAllOf(singleMethodOverridingInterface.getDeclaredMethods()));
        assertThat(finding.getInvokableMethods(), containsAllOf(additionalMethodInterface.getDeclaredMethods()));
        assertThat(finding.getInvokableMethods(), containsAllOf(objectType.getDeclaredMethods().filter(isVirtualTo(abstractAdditionalMethodInterfaceClass))));
        assertThat(finding.getInvokableMethods().size(), is(abstractAdditionalMethodInterfaceClass.getDeclaredMethods().size()
                + singleMethodOverridingInterface.getDeclaredMethods().size()
                + additionalMethodInterface.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(abstractAdditionalMethodInterfaceClass)).size()));
        assertThat(finding.getInvokableDefaultMethods().size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(additionalMethodInterface).size(), is(0));
    }

    @Test
    public void testConflictingInterfaceLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription conflictingInterfaceClass = new TypeDescription.ForLoadedType(ConflictingInterfaceClass.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(conflictingInterfaceClass);
        assertThat(finding.getLookedUpType(), is(conflictingInterfaceClass));
        assertThat(finding.getInvokableMethods(), containsAllOf(conflictingInterfaceClass.getDeclaredMethods()));
        assertThat(finding.getInvokableMethods(), containsAllOf(objectType.getDeclaredMethods().filter(isVirtualTo(conflictingInterfaceClass))));
        assertThat(finding.getInvokableMethods().size(), is(conflictingInterfaceClass.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(conflictingInterfaceClass)).size()
                + 1));
        MethodDescription conflictMethod = finding.getInvokableMethods().filter(named(FOO)).getOnly();
        assertEquals(MethodLookupEngine.ConflictingInterfaceMethod.class, conflictMethod.getClass());
        assertThat(conflictMethod.getDeclaringType(), is(conflictingInterfaceClass));
        assertThat(finding.getInvokableDefaultMethods().size(), is(2));
        assertThat(finding.getInvokableDefaultMethods().get(new TypeDescription.ForLoadedType(SingleMethodInterface.class)).size(), is(0));
        assertThat(finding.getInvokableDefaultMethods().get(new TypeDescription.ForLoadedType(ConflictingSingleMethodInterface.class)).size(), is(0));
    }

    @Test
    public void testTrivialDefaultMethodLookup() throws Exception {
        TypeDescription singleDefaultMethodClass = new TypeDescription.ForLoadedType(SingleDefaultMethodClass.class);
        TypeDescription singleDefaultMethodInterface = new TypeDescription.ForLoadedType(SingleDefaultMethodInterface.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleDefaultMethodClass);
        assertThat(finding.getLookedUpType(), is(singleDefaultMethodClass));
        assertThat(finding.getInvokableDefaultMethods().size(), is(1));
        MethodDescription interfaceMethod = singleDefaultMethodInterface.getDeclaredMethods().getOnly();
        Set<MethodDescription> discoveredDefaultMethods = finding.getInvokableDefaultMethods().get(singleDefaultMethodInterface);
        assertThat(discoveredDefaultMethods, hasItem(interfaceMethod));
        assertThat(discoveredDefaultMethods.size(), is(1));
        assertThat(finding.getInvokableMethods(), hasItem(interfaceMethod));
    }

    @Test
    public void testOverridingDefaultMethodLookup() throws Exception {
        TypeDescription singleDefaultMethodClass = new TypeDescription.ForLoadedType(SingleDefaultMethodOverridingClass.class);
        TypeDescription singleDefaultMethodInterface = new TypeDescription.ForLoadedType(SingleDefaultMethodInterface.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleDefaultMethodClass);
        assertThat(finding.getLookedUpType(), is(singleDefaultMethodClass));
        assertThat(finding.getInvokableDefaultMethods().size(), is(1));
        MethodDescription interfaceMethod = singleDefaultMethodInterface.getDeclaredMethods().getOnly();
        Set<MethodDescription> discoveredDefaultMethods = finding.getInvokableDefaultMethods().get(singleDefaultMethodInterface);
        assertThat(discoveredDefaultMethods, hasItem(interfaceMethod));
        assertThat(discoveredDefaultMethods.size(), is(1));
        assertThat(finding.getInvokableMethods(), CoreMatchers.not(hasItem(interfaceMethod)));
    }

    @Test
    public void testConflictingDefaultMethodLookup() throws Exception {
        TypeDescription singleDefaultMethodClass = new TypeDescription.ForLoadedType(SingleDefaultMethodConflictingClass.class);
        TypeDescription singleDefaultMethodInterface = new TypeDescription.ForLoadedType(SingleDefaultMethodInterface.class);
        TypeDescription singleConflictingDefaultMethodInterface = new TypeDescription.ForLoadedType(SingleDefaultMethodConflictingInterface.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleDefaultMethodClass);
        assertThat(finding.getLookedUpType(), is(singleDefaultMethodClass));
        assertThat(finding.getInvokableDefaultMethods().size(), is(2));
        MethodDescription firstInterfaceMethod = singleDefaultMethodInterface.getDeclaredMethods().getOnly();
        MethodDescription secondInterfaceMethod = singleConflictingDefaultMethodInterface.getDeclaredMethods().getOnly();
        assertThat(finding.getInvokableMethods(), CoreMatchers.not(hasItems(firstInterfaceMethod, secondInterfaceMethod)));
        Set<MethodDescription> singleDefaultMethodInterfaceMethods = finding.getInvokableDefaultMethods().get(singleDefaultMethodInterface);
        assertThat(singleDefaultMethodInterfaceMethods.size(), is(1));
        assertThat(singleDefaultMethodInterfaceMethods, hasItem(firstInterfaceMethod));
        Set<MethodDescription> singleConflictingDefaultMethodInterfaceMethods = finding.getInvokableDefaultMethods().get(singleConflictingDefaultMethodInterface);
        assertThat(singleConflictingDefaultMethodInterfaceMethods.size(), is(1));
        assertThat(singleConflictingDefaultMethodInterfaceMethods, hasItem(secondInterfaceMethod));
    }

    @Test
    public void testAbstractOverridingDefaultMethodLookup() throws Exception {
        TypeDescription singleDefaultMethodAbstractOverridingClass = new TypeDescription.ForLoadedType(SingleDefaultMethodAbstractOverridingClass.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleDefaultMethodAbstractOverridingClass);
        assertThat(finding.getLookedUpType(), is(singleDefaultMethodAbstractOverridingClass));
        assertThat(finding.getInvokableDefaultMethods().size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(new TypeDescription.ForLoadedType(SingleDefaultMethodAbstractOverridingInterface.class)).size(), is(0));
    }

    @Test
    public void testManifestOverridingDefaultMethodLookup() throws Exception {
        TypeDescription singleDefaultMethodManifestOverridingClass = new TypeDescription.ForLoadedType(SingleDefaultMethodManifestOverridingClass.class);
        TypeDescription singleDefaultMethodManifestOverridingInterface = new TypeDescription.ForLoadedType(SingleDefaultMethodManifestOverridingInterface.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleDefaultMethodManifestOverridingClass);
        assertThat(finding.getLookedUpType(), is(singleDefaultMethodManifestOverridingClass));
        assertThat(finding.getInvokableDefaultMethods().size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodManifestOverridingInterface).size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodManifestOverridingInterface),
                hasItem(singleDefaultMethodManifestOverridingInterface.getDeclaredMethods().getOnly()));
    }

    @Test
    public void testNonOverridenDefaultMethodLookup() throws Exception {
        TypeDescription singleDefaultMethodNonOverridingClass = new TypeDescription.ForLoadedType(SingleDefaultMethodNonOverridingClass.class);
        TypeDescription singleDefaultMethodNonOverridingInterface = new TypeDescription.ForLoadedType(SingleDefaultMethodNonOverridingInterface.class);
        TypeDescription singleDefaultMethodInterface = new TypeDescription.ForLoadedType(SingleDefaultMethodInterface.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleDefaultMethodNonOverridingClass);
        assertThat(finding.getLookedUpType(), is(singleDefaultMethodNonOverridingClass));
        MethodDescription interfaceMethod = singleDefaultMethodInterface.getDeclaredMethods().getOnly();
        assertThat(finding.getInvokableMethods(), hasItem(interfaceMethod));
        assertThat(finding.getInvokableDefaultMethods().size(), is(1));
        Set<MethodDescription> discoveredDefaultMethods = finding.getInvokableDefaultMethods().get(singleDefaultMethodNonOverridingInterface);
        assertThat(discoveredDefaultMethods.size(), is(1));
        assertThat(discoveredDefaultMethods, hasItem(interfaceMethod));
    }

    @Test
    public void testAmbiguousNonOverridingDefaultMethodLookup() throws Exception {
        TypeDescription singleDefaultMethodAmbiguousInheritanceClass = new TypeDescription.ForLoadedType(SingleDefaultMethodAmbiguousInheritanceClass.class);
        TypeDescription singleDefaultMethodNonOverridingInterface = new TypeDescription.ForLoadedType(SingleDefaultMethodNonOverridingInterface.class);
        TypeDescription singleDefaultMethodAmbiguousInheritanceInterface = new TypeDescription.ForLoadedType(SingleDefaultMethodAmbiguousInheritanceInterface.class);
        TypeDescription singleDefaultMethodInterface = new TypeDescription.ForLoadedType(SingleDefaultMethodInterface.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleDefaultMethodAmbiguousInheritanceClass);
        assertThat(finding.getLookedUpType(), is(singleDefaultMethodAmbiguousInheritanceClass));
        MethodDescription interfaceMethod = singleDefaultMethodInterface.getDeclaredMethods().getOnly();
        assertThat(finding.getInvokableMethods(), hasItem(interfaceMethod));
        assertThat(finding.getInvokableDefaultMethods().size(), is(2));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodNonOverridingInterface).size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodNonOverridingInterface), hasItem(interfaceMethod));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodAmbiguousInheritanceInterface).size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodAmbiguousInheritanceInterface), hasItem(interfaceMethod));
    }

    @Test
    public void testAmbiguousAbstractOverridingDefaultMethodLookup() throws Exception {
        TypeDescription singleDefaultMethodAmbiguousAbstractOverridingClass = new TypeDescription.ForLoadedType(SingleDefaultMethodAmbiguousAbstractOverridingClass.class);
        TypeDescription singleDefaultMethodNonOverridingInterface = new TypeDescription.ForLoadedType(SingleDefaultMethodNonOverridingInterface.class);
        TypeDescription singleDefaultMethodAbstractOverridingInterface = new TypeDescription.ForLoadedType(SingleDefaultMethodAbstractOverridingInterface.class);
        TypeDescription singleDefaultMethodInterface = new TypeDescription.ForLoadedType(SingleDefaultMethodInterface.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleDefaultMethodAmbiguousAbstractOverridingClass);
        assertThat(finding.getLookedUpType(), is(singleDefaultMethodAmbiguousAbstractOverridingClass));
        MethodDescription interfaceMethod = singleDefaultMethodInterface.getDeclaredMethods().getOnly();
        assertThat(finding.getInvokableDefaultMethods().size(), is(2));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodNonOverridingInterface).size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodNonOverridingInterface), hasItem(interfaceMethod));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodAbstractOverridingInterface).size(), is(0));
    }

    @Test
    public void testAmbiguousManifestOverridingDefaultMethodLookup() throws Exception {
        TypeDescription singleDefaultMethodAmbiguousManifestOverridingClass = new TypeDescription.ForLoadedType(SingleDefaultMethodAmbiguousManifestOverridingClass.class);
        TypeDescription singleDefaultMethodNonOverridingInterface = new TypeDescription.ForLoadedType(SingleDefaultMethodNonOverridingInterface.class);
        TypeDescription singleDefaultMethodManifestOverridingInterface = new TypeDescription.ForLoadedType(SingleDefaultMethodManifestOverridingInterface.class);
        TypeDescription singleDefaultMethodInterface = new TypeDescription.ForLoadedType(SingleDefaultMethodInterface.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleDefaultMethodAmbiguousManifestOverridingClass);
        assertThat(finding.getLookedUpType(), is(singleDefaultMethodAmbiguousManifestOverridingClass));
        MethodDescription interfaceMethod = singleDefaultMethodInterface.getDeclaredMethods().getOnly();
        assertThat(finding.getInvokableDefaultMethods().size(), is(2));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodNonOverridingInterface).size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodNonOverridingInterface), hasItem(interfaceMethod));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodNonOverridingInterface).size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodManifestOverridingInterface),
                hasItem(singleDefaultMethodManifestOverridingInterface.getDeclaredMethods().getOnly()));
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(methodLookupEngine.hashCode(),
                is(new MethodLookupEngine.Default(MethodLookupEngine.Default.DefaultMethodLookup.ENABLED).hashCode()));
        assertThat(methodLookupEngine,
                is((MethodLookupEngine) new MethodLookupEngine.Default(MethodLookupEngine.Default.DefaultMethodLookup.ENABLED)));
        MethodLookupEngine otherEngine = new MethodLookupEngine.Default(MethodLookupEngine.Default.DefaultMethodLookup.ENABLED);
        otherEngine.process(new TypeDescription.ForLoadedType(Object.class));
        assertThat(methodLookupEngine.hashCode(), CoreMatchers.not(is(otherEngine.hashCode())));
        assertThat(methodLookupEngine, CoreMatchers.not(is(otherEngine)));
    }

    private static interface SingleMethodInterface {

        void foo();
    }

    private static interface SingleMethodOverridingInterface extends SingleMethodInterface {

        @Override
        void foo();
    }

    private static interface AdditionalMethodInterface extends SingleMethodOverridingInterface {

        void bar();
    }

    private static interface ConflictingSingleMethodInterface {

        void foo();
    }

    private static class ClassOverridingToString {

        @Override
        public String toString() {
            return super.toString();
        }
    }

    private static abstract class AbstractSingleInterfaceClass implements SingleMethodInterface {
        /* empty */
    }

    private static class SingleInterfaceClass implements SingleMethodInterface {

        @Override
        public void foo() {
            /* empty */
        }
    }

    private static abstract class AbstractSingleMethodOverridingClass implements SingleMethodOverridingInterface {
        /* empty */
    }

    private static abstract class AbstractFoldedInterfaceClass extends AbstractSingleInterfaceClass implements SingleMethodOverridingInterface {
        /* empty */
    }

    private static abstract class AbstractFoldedReverseInterfaceBaseClass implements SingleMethodOverridingInterface {
        /* empty */
    }

    private static abstract class AbstractFoldedReverseInterfaceClass extends AbstractFoldedReverseInterfaceBaseClass implements SingleMethodInterface {
        /* empty */
    }

    private static abstract class AbstractAdditionalMethodInterfaceClass implements AdditionalMethodInterface {
        /* empty */
    }

    private static abstract class ConflictingInterfaceClass implements SingleMethodInterface, ConflictingSingleMethodInterface {
        /* empty */
    }
}
