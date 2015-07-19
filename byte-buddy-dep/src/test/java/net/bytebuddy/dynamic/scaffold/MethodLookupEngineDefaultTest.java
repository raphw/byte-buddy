package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.test.utility.PrecompiledTypeClassLoader;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.util.List;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.test.utility.CustomHamcrestMatchers.containsAllOf;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class MethodLookupEngineDefaultTest {

    private static final String TO_STRING = "toString", FOO = "foo", BAR = "bar", PREFIX = "net.bytebuddy.test.precompiled.";

    private static final String SINGLE_DEFAULT_METHOD_ABSTRACT_OVERRIDING_CLASS = PREFIX + "SingleDefaultMethodAbstractOverridingClass",
            SINGLE_DEFAULT_METHOD_ABSTRACT_OVERRIDING_INTERFACE = PREFIX + "SingleDefaultMethodAbstractOverridingInterface",
            SINGLE_DEFAULT_METHOD_AMBIGUOUS_ABSTRACT_OVERRIDING_CLASS = PREFIX + "SingleDefaultMethodAmbiguousAbstractOverridingClass",
            SINGLE_DEFAULT_METHOD_AMBIGUOUS_INHERITANCE_CLASS = PREFIX + "SingleDefaultMethodAmbiguousInheritanceClass",
            SINGLE_DEFAULT_METHOD_AMBIGUOUS_INHERITANCE_INTERFACE = PREFIX + "SingleDefaultMethodAmbiguousInheritanceInterface",
            SINGLE_DEFAULT_METHOD_AMBIGUOUS_MANIFEST_OVERRIDING_CLASS = PREFIX + "SingleDefaultMethodAmbiguousManifestOverridingClass",
            SINGLE_DEFAULT_METHOD_CLASS = PREFIX + "SingleDefaultMethodClass",
            SINGLE_DEFAULT_METHOD_CONFLICTING_CLASS = PREFIX + "SingleDefaultMethodConflictingClass",
            SINGLE_DEFAULT_METHOD_CONFLICTING_INTERFACE = PREFIX + "SingleDefaultMethodConflictingInterface",
            SINGLE_DEFAULT_METHOD_INTERFACE = PREFIX + "SingleDefaultMethodInterface",
            SINGLE_DEFAULT_METHOD_MANIFEST_OVERRIDING_CLASS = PREFIX + "SingleDefaultMethodManifestOverridingClass",
            SINGLE_DEFAULT_METHOD_MANIFEST_OVERRIDING_INTERFACE = PREFIX + "SingleDefaultMethodManifestOverridingInterface",
            SINGLE_DEFAULT_METHOD_NON_OVERRIDING_CLASS = PREFIX + "SingleDefaultMethodNonOverridingClass",
            SINGLE_DEFAULT_METHOD_NON_OVERRIDING_INTERFACE = PREFIX + "SingleDefaultMethodNonOverridingInterface",
            SINGLE_DEFAULT_METHOD_OVERRIDING_CLASS = PREFIX + "SingleDefaultMethodOverridingClass";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private MethodLookupEngine methodLookupEngine;

    private ClassLoader classLoader;

    private static ElementMatcher<? super MethodDescription> isVirtualTo(TypeDescription typeDescription) {
        return isMethod().<MethodDescription>and(not(isPrivate().or(isStatic())
                .or(isPackagePrivate().and(not(isVisibleTo(typeDescription))))));
    }

    private TypeDescription findType(String name) throws Exception {
        return new TypeDescription.ForLoadedType(Class.forName(name, true, classLoader));
    }

    @Before
    public void setUp() throws Exception {
        methodLookupEngine = MethodLookupEngine.Default.Factory.INSTANCE.make(true);
        classLoader = new PrecompiledTypeClassLoader(getClass().getClassLoader());
    }

    @Test
    public void testTrivialLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(objectType);
        assertThat(finding.getTypeDescription(), is(objectType));
        assertThat(finding.getInvokableMethods().size(), is(objectType.getDeclaredMethods().size()));
        assertThat(finding.getInvokableMethods(), containsAllOf(objectType.getDeclaredMethods()));
        assertThat(finding.getInvokableDefaultMethods().size(), is(0));
    }

    @Test
    public void testClassOverrideLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription classOverridingToString = new TypeDescription.ForLoadedType(ClassOverridingToString.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(classOverridingToString);
        assertThat(finding.getTypeDescription(), is(classOverridingToString));
        assertThat(finding.getInvokableMethods(), containsAllOf(classOverridingToString.getDeclaredMethods()));
        assertThat(finding.getInvokableMethods(), containsAllOf(objectType.getDeclaredMethods()
                .filter(isVirtualTo(classOverridingToString)).filter(ElementMatchers.not(named(TO_STRING)))));
        assertThat(finding.getInvokableMethods().filter(named(TO_STRING)).getOnly().isSpecializableFor(classOverridingToString), is(true));
        assertThat(finding.getInvokableMethods().filter(named(TO_STRING)).getOnly().isSpecializableFor(objectType), is(true));
        assertThat(finding.getInvokableMethods().size(), is(classOverridingToString.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(classOverridingToString)).size() - 1));
        assertThat(finding.getInvokableDefaultMethods().size(), is(0));
    }

    @Test
    public void testClassOverrideAbstractMethodLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription classOverridingToStringAbstract = new TypeDescription.ForLoadedType(ClassOverridingToStringAbstract.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(classOverridingToStringAbstract);
        assertThat(finding.getTypeDescription(), is(classOverridingToStringAbstract));
        assertThat(finding.getInvokableMethods(), containsAllOf(classOverridingToStringAbstract.getDeclaredMethods()));
        assertThat(finding.getInvokableMethods(), containsAllOf(objectType.getDeclaredMethods()
                .filter(isVirtualTo(classOverridingToStringAbstract)).filter(ElementMatchers.not(named(TO_STRING)))));
        assertThat(finding.getInvokableMethods().filter(named(TO_STRING)).getOnly().isSpecializableFor(classOverridingToStringAbstract), is(false));
        assertThat(finding.getInvokableMethods().filter(named(TO_STRING)).getOnly().isSpecializableFor(objectType), is(true));
        assertThat(finding.getInvokableMethods().size(), is(classOverridingToStringAbstract.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(classOverridingToStringAbstract)).size() - 1));
        assertThat(finding.getInvokableDefaultMethods().size(), is(0));
    }

    @Test
    public void testClassReOverrideAbstractMethodLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription classOverridingToStringAbstract = new TypeDescription.ForLoadedType(ClassOverridingToStringAbstract.class);
        TypeDescription classReOverridingToStringManifest = new TypeDescription.ForLoadedType(ClassReOverridingToStringManifest.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(classReOverridingToStringManifest);
        assertThat(finding.getTypeDescription(), is(classReOverridingToStringManifest));
        assertThat(finding.getInvokableMethods(), containsAllOf(classReOverridingToStringManifest.getDeclaredMethods()));
        assertThat(finding.getInvokableMethods(), containsAllOf(objectType.getDeclaredMethods()
                .filter(isVirtualTo(classReOverridingToStringManifest)).filter(ElementMatchers.not(named(TO_STRING)))));
        assertThat(finding.getInvokableMethods().filter(named(TO_STRING)).getOnly().isSpecializableFor(classReOverridingToStringManifest), is(true));
        assertThat(finding.getInvokableMethods().filter(named(TO_STRING)).getOnly().isSpecializableFor(classOverridingToStringAbstract), is(false));
        assertThat(finding.getInvokableMethods().filter(named(TO_STRING)).getOnly().isSpecializableFor(objectType), is(true));
        assertThat(finding.getInvokableMethods().size(), is(classReOverridingToStringManifest.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(classReOverridingToStringManifest)).size() - 1));
        assertThat(finding.getInvokableDefaultMethods().size(), is(0));
    }

    @Test
    public void testInterfaceLookup() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription abstractSingleInterfaceClass = new TypeDescription.ForLoadedType(AbstractSingleInterfaceClass.class);
        TypeDescription singleMethodInterface = new TypeDescription.ForLoadedType(SingleMethodInterface.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(abstractSingleInterfaceClass);
        assertThat(finding.getTypeDescription(), is(abstractSingleInterfaceClass));
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
        assertThat(finding.getTypeDescription(), is(singleInterfaceClass));
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
        assertThat(finding.getTypeDescription(), is(abstractSingleMethodOverridingClass));
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
        assertThat(finding.getTypeDescription(), is(abstractFoldedInterfaceClass));
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
        assertThat(finding.getTypeDescription(), is(abstractFoldedReverseInterfaceClass));
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
        assertThat(finding.getTypeDescription(), is(abstractAdditionalMethodInterfaceClass));
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
        assertThat(finding.getTypeDescription(), is(conflictingInterfaceClass));
        assertThat(finding.getInvokableMethods(), containsAllOf(conflictingInterfaceClass.getDeclaredMethods()));
        assertThat(finding.getInvokableMethods(), containsAllOf(objectType.getDeclaredMethods().filter(isVirtualTo(conflictingInterfaceClass))));
        assertThat(finding.getInvokableMethods().size(), is(conflictingInterfaceClass.getDeclaredMethods().size()
                + objectType.getDeclaredMethods().filter(isVirtualTo(conflictingInterfaceClass)).size()
                + 1));
        MethodDescription conflictMethod = finding.getInvokableMethods().filter(named(FOO)).getOnly();
        assertEquals(MethodLookupEngine.ConflictingInterfaceMethod.class, conflictMethod.getClass());
        assertThat(conflictMethod.getDeclaringType(), is((GenericTypeDescription) conflictingInterfaceClass));
        assertThat(finding.getInvokableDefaultMethods().size(), is(2));
        assertThat(finding.getInvokableDefaultMethods().get(new TypeDescription.ForLoadedType(SingleMethodInterface.class)).size(), is(0));
        assertThat(finding.getInvokableDefaultMethods().get(new TypeDescription.ForLoadedType(ConflictingSingleMethodInterface.class)).size(), is(0));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testTrivialDefaultMethodLookup() throws Exception {
        TypeDescription singleDefaultMethodClass = findType(SINGLE_DEFAULT_METHOD_CLASS);
        TypeDescription singleDefaultMethodInterface = findType(SINGLE_DEFAULT_METHOD_INTERFACE);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleDefaultMethodClass);
        assertThat(finding.getTypeDescription(), is(singleDefaultMethodClass));
        assertThat(finding.getInvokableDefaultMethods().size(), is(1));
        MethodDescription interfaceMethod = singleDefaultMethodInterface.getDeclaredMethods().getOnly();
        Set<MethodDescription> discoveredDefaultMethods = finding.getInvokableDefaultMethods().get(singleDefaultMethodInterface);
        assertThat(discoveredDefaultMethods, hasItem(interfaceMethod));
        assertThat(discoveredDefaultMethods.size(), is(1));
        assertThat(finding.getInvokableMethods(), hasItem(interfaceMethod));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testOverridingDefaultMethodLookup() throws Exception {
        TypeDescription singleDefaultMethodClass = findType(SINGLE_DEFAULT_METHOD_OVERRIDING_CLASS);
        TypeDescription singleDefaultMethodInterface = findType(SINGLE_DEFAULT_METHOD_INTERFACE);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleDefaultMethodClass);
        assertThat(finding.getTypeDescription(), is(singleDefaultMethodClass));
        assertThat(finding.getInvokableDefaultMethods().size(), is(1));
        MethodDescription interfaceMethod = singleDefaultMethodInterface.getDeclaredMethods().getOnly();
        Set<MethodDescription> discoveredDefaultMethods = finding.getInvokableDefaultMethods().get(singleDefaultMethodInterface);
        assertThat(discoveredDefaultMethods, hasItem(interfaceMethod));
        assertThat(discoveredDefaultMethods.size(), is(1));
        assertThat(finding.getInvokableMethods(), CoreMatchers.not(hasItem(interfaceMethod)));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testConflictingDefaultMethodLookup() throws Exception {
        TypeDescription singleDefaultMethodClass = findType(SINGLE_DEFAULT_METHOD_CONFLICTING_CLASS);
        TypeDescription singleDefaultMethodInterface = findType(SINGLE_DEFAULT_METHOD_INTERFACE);
        TypeDescription singleConflictingDefaultMethodInterface = findType(SINGLE_DEFAULT_METHOD_CONFLICTING_INTERFACE);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleDefaultMethodClass);
        assertThat(finding.getTypeDescription(), is(singleDefaultMethodClass));
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
    @JavaVersionRule.Enforce(8)
    public void testAbstractOverridingDefaultMethodLookup() throws Exception {
        TypeDescription singleDefaultMethodAbstractOverridingClass = findType(SINGLE_DEFAULT_METHOD_ABSTRACT_OVERRIDING_CLASS);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleDefaultMethodAbstractOverridingClass);
        assertThat(finding.getTypeDescription(), is(singleDefaultMethodAbstractOverridingClass));
        assertThat(finding.getInvokableDefaultMethods().size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(findType(SINGLE_DEFAULT_METHOD_ABSTRACT_OVERRIDING_INTERFACE)).size(), is(0));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testManifestOverridingDefaultMethodLookup() throws Exception {
        TypeDescription singleDefaultMethodManifestOverridingClass = findType(SINGLE_DEFAULT_METHOD_MANIFEST_OVERRIDING_CLASS);
        TypeDescription singleDefaultMethodManifestOverridingInterface = findType(SINGLE_DEFAULT_METHOD_MANIFEST_OVERRIDING_INTERFACE);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleDefaultMethodManifestOverridingClass);
        assertThat(finding.getTypeDescription(), is(singleDefaultMethodManifestOverridingClass));
        assertThat(finding.getInvokableDefaultMethods().size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodManifestOverridingInterface).size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodManifestOverridingInterface),
                hasItem(singleDefaultMethodManifestOverridingInterface.getDeclaredMethods().getOnly()));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testNonOverridenDefaultMethodLookup() throws Exception {
        TypeDescription singleDefaultMethodNonOverridingClass = findType(SINGLE_DEFAULT_METHOD_NON_OVERRIDING_CLASS);
        TypeDescription singleDefaultMethodNonOverridingInterface = findType(SINGLE_DEFAULT_METHOD_NON_OVERRIDING_INTERFACE);
        TypeDescription singleDefaultMethodInterface = findType(SINGLE_DEFAULT_METHOD_INTERFACE);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleDefaultMethodNonOverridingClass);
        assertThat(finding.getTypeDescription(), is(singleDefaultMethodNonOverridingClass));
        MethodDescription interfaceMethod = singleDefaultMethodInterface.getDeclaredMethods().getOnly();
        assertThat(finding.getInvokableMethods(), hasItem(interfaceMethod));
        assertThat(finding.getInvokableDefaultMethods().size(), is(1));
        Set<MethodDescription> discoveredDefaultMethods = finding.getInvokableDefaultMethods().get(singleDefaultMethodNonOverridingInterface);
        assertThat(discoveredDefaultMethods.size(), is(1));
        assertThat(discoveredDefaultMethods, hasItem(interfaceMethod));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testAmbiguousNonOverridingDefaultMethodLookup() throws Exception {
        TypeDescription singleDefaultMethodAmbiguousInheritanceClass = findType(SINGLE_DEFAULT_METHOD_AMBIGUOUS_INHERITANCE_CLASS);
        TypeDescription singleDefaultMethodNonOverridingInterface = findType(SINGLE_DEFAULT_METHOD_NON_OVERRIDING_INTERFACE);
        TypeDescription singleDefaultMethodAmbiguousInheritanceInterface = findType(SINGLE_DEFAULT_METHOD_AMBIGUOUS_INHERITANCE_INTERFACE);
        TypeDescription singleDefaultMethodInterface = findType(SINGLE_DEFAULT_METHOD_INTERFACE);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleDefaultMethodAmbiguousInheritanceClass);
        assertThat(finding.getTypeDescription(), is(singleDefaultMethodAmbiguousInheritanceClass));
        MethodDescription interfaceMethod = singleDefaultMethodInterface.getDeclaredMethods().getOnly();
        assertThat(finding.getInvokableMethods(), hasItem(interfaceMethod));
        assertThat(finding.getInvokableDefaultMethods().size(), is(2));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodNonOverridingInterface).size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodNonOverridingInterface), hasItem(interfaceMethod));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodAmbiguousInheritanceInterface).size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodAmbiguousInheritanceInterface), hasItem(interfaceMethod));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testAmbiguousAbstractOverridingDefaultMethodLookup() throws Exception {
        TypeDescription singleDefaultMethodAmbiguousAbstractOverridingClass = findType(SINGLE_DEFAULT_METHOD_AMBIGUOUS_ABSTRACT_OVERRIDING_CLASS);
        TypeDescription singleDefaultMethodNonOverridingInterface = findType(SINGLE_DEFAULT_METHOD_NON_OVERRIDING_INTERFACE);
        TypeDescription singleDefaultMethodAbstractOverridingInterface = findType(SINGLE_DEFAULT_METHOD_ABSTRACT_OVERRIDING_INTERFACE);
        TypeDescription singleDefaultMethodInterface = findType(SINGLE_DEFAULT_METHOD_INTERFACE);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleDefaultMethodAmbiguousAbstractOverridingClass);
        assertThat(finding.getTypeDescription(), is(singleDefaultMethodAmbiguousAbstractOverridingClass));
        MethodDescription interfaceMethod = singleDefaultMethodInterface.getDeclaredMethods().getOnly();
        assertThat(finding.getInvokableDefaultMethods().size(), is(2));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodNonOverridingInterface).size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodNonOverridingInterface), hasItem(interfaceMethod));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodAbstractOverridingInterface).size(), is(0));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testAmbiguousManifestOverridingDefaultMethodLookup() throws Exception {
        TypeDescription singleDefaultMethodAmbiguousManifestOverridingClass = findType(SINGLE_DEFAULT_METHOD_AMBIGUOUS_MANIFEST_OVERRIDING_CLASS);
        TypeDescription singleDefaultMethodNonOverridingInterface = findType(SINGLE_DEFAULT_METHOD_NON_OVERRIDING_INTERFACE);
        TypeDescription singleDefaultMethodManifestOverridingInterface = findType(SINGLE_DEFAULT_METHOD_MANIFEST_OVERRIDING_INTERFACE);
        TypeDescription singleDefaultMethodInterface = findType(SINGLE_DEFAULT_METHOD_INTERFACE);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(singleDefaultMethodAmbiguousManifestOverridingClass);
        assertThat(finding.getTypeDescription(), is(singleDefaultMethodAmbiguousManifestOverridingClass));
        MethodDescription interfaceMethod = singleDefaultMethodInterface.getDeclaredMethods().getOnly();
        assertThat(finding.getInvokableDefaultMethods().size(), is(2));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodNonOverridingInterface).size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodNonOverridingInterface), hasItem(interfaceMethod));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodNonOverridingInterface).size(), is(1));
        assertThat(finding.getInvokableDefaultMethods().get(singleDefaultMethodManifestOverridingInterface),
                hasItem(singleDefaultMethodManifestOverridingInterface.getDeclaredMethods().getOnly()));
    }

    @Test
    public void testResolvedGenericType() throws Exception {
        MethodLookupEngine.Finding finding = methodLookupEngine.process(new TypeDescription.ForLoadedType(GenericType.Resolved.class));
        assertThat(finding.getInvokableMethods().filter(named(FOO)).size(), is(1));
        GenericTypeDescription foo = finding.getInvokableMethods().filter(named(FOO)).getOnly().getReturnType();
        assertThat(foo.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(foo.asRawType().represents(List.class), is(true));
        assertThat(foo.getParameters().size(), is(1));
        assertThat(foo.getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(foo.getParameters().getOnly().asRawType().represents(String.class), is(true));
        assertThat(finding.getInvokableMethods().filter(named(BAR)).size(), is(1));
        GenericTypeDescription bar = finding.getInvokableMethods().filter(named(BAR)).getOnly().getReturnType();
        assertThat(bar.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(bar.asRawType().represents(List.class), is(true));
        assertThat(bar.getParameters().size(), is(1));
        assertThat(bar.getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(bar.getParameters().getOnly().asRawType().represents(String.class), is(true));
    }

    @Test
    public void testUnresolvedGenericType() throws Exception {
        TypeDescription unresolvedType = new TypeDescription.ForLoadedType(GenericType.Unresolved.class);
        MethodLookupEngine.Finding finding = methodLookupEngine.process(unresolvedType);
        assertThat(finding.getInvokableMethods().filter(named(FOO)).size(), is(1));
        GenericTypeDescription foo = finding.getInvokableMethods().filter(named(FOO)).getOnly().getReturnType();
        assertThat(foo.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(foo.asRawType().represents(List.class), is(true));
        assertThat(foo.getParameters().size(), is(1));
        assertThat(foo.getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(foo.getParameters().getOnly(), is(unresolvedType.getTypeVariables().getOnly()));
        assertThat(finding.getInvokableMethods().filter(named(BAR)).size(), is(1));
        GenericTypeDescription bar = finding.getInvokableMethods().filter(named(BAR)).getOnly().getReturnType();
        assertThat(bar.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(bar.asRawType().represents(List.class), is(true));
        assertThat(bar.getParameters().size(), is(1));
        assertThat(bar.getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(bar.getParameters().getOnly(), is(unresolvedType.getTypeVariables().getOnly()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodLookupEngine.Default.class).apply();
        ObjectPropertyAssertion.of(MethodLookupEngine.Default.Factory.class).apply();
        ObjectPropertyAssertion.of(MethodLookupEngine.Default.MethodBucket.class).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                when(mock.getDeclaredMethods()).thenReturn(new MethodList.Empty());
            }
        }).applyBasic();
        ObjectPropertyAssertion.of(MethodLookupEngine.Default.MethodBucket.DefaultMethodLookup.Enabled.class).applyBasic();
        ObjectPropertyAssertion.of(MethodLookupEngine.Default.MethodBucket.DefaultMethodLookup.Disabled.class).apply();
    }

    private interface SingleMethodInterface {

        void foo();
    }

    private interface SingleMethodOverridingInterface extends SingleMethodInterface {

        @Override
        void foo();
    }

    private interface AdditionalMethodInterface extends SingleMethodOverridingInterface {

        void bar();
    }

    private interface ConflictingSingleMethodInterface {

        void foo();
    }

    private static class ClassOverridingToString {

        @Override
        public String toString() {
            return super.toString();
        }
    }

    private abstract static class ClassOverridingToStringAbstract {

        @Override
        public abstract String toString();
    }

    private static class ClassReOverridingToStringManifest extends ClassOverridingToStringAbstract {

        @Override
        public String toString() {
            return FOO;
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

    public static abstract class GenericType<T> {

        abstract List<T> foo();

        interface GenericInterfaceType<S> {

            List<S> bar();
        }

        static abstract class Resolved extends GenericType<String> implements GenericInterfaceType<String> {
            /* empty */
        }

        static abstract class Unresolved<S> extends GenericType<S> implements GenericInterfaceType<S> {
            /* empty */
        }
    }
}
