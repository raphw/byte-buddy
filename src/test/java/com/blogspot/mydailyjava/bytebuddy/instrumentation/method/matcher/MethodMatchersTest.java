package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MethodMatchersTest {

    private static final String FOO_METHOD_NAME = "foo";
    private static final String BAR_METHOD_NAME = "bar";
    private static final String BAZ_METHOD_NAME = "baz";
    private static final String QUX_METHOD_NAME = "qux";

    private static final String FIN_METHOD_NAME = "fin";
    private static final String STAT_METHOD_NAME = "stat";
    private static final String VARARGS_METHOD_NAME = "varargs";
    private static final String SYNC_METHOD_NAME = "sync";
    private static final String STRICT_METHOD_NAME = "strict";

    private static final String FOOBAR_METHOD_NAME = "foobar";

    private static final String SET_PROPERTY_METHOD_NAME = "setProperty";
    private static final String GET_PROPERTY_METHOD_NAME = "getProperty";

    private static final String GENERIC_INTERFACE_METHOD_NAME = "gen";

    private static final String FOO_METHOD_NAME_REGEX = "fo{2}";
    private static final String BAR_METHOD_NAME_REGEX = "b[a]r";

    private static final String JAVA_LANG_PACKAGE = "java.lang";
    private static final String HASH_CODE_METHOD_NAME = "hashCode";
    private static final String FINALIZE_METHOD_NAME = "finalize";

    @SuppressWarnings("unused")
    private static interface TestInterface<T> {

        void gen(T o);
    }

    @SuppressWarnings("unused")
    private static class TestClassBase implements TestInterface<String> {

        public void foo() {
            /* empty */
        }

        private Object bar(Object o) throws Exception {
            return null;
        }

        protected void baz() {
            /* empty */
        }

        void qux() {
            /* empty */
        }

        public static void stat() {
            /* empty */
        }

        public final void fin1() {
            /* empty */
        }

        @Override
        public void gen(String o) {
            /* empty */
        }

        public Object foobar() {
            return null;
        }
    }

    @SuppressWarnings("unused")
    private static class TestClassExtension extends TestClassBase {

        @Override
        public void foo() {
            /* empty */
        }

        private Object bar(Object o) throws RuntimeException {
            return null;
        }

        @Override
        protected void baz() {
            /* empty */
        }

        @Override
        void qux() {
            /* empty */
        }

        public static void stat() {
            /* empty */
        }

        public final void fin2() {
            /* empty */
        }

        @Override
        public String foobar() {
            return null;
        }
    }

    @SuppressWarnings("unused")
    private static class TestModifier {

        public synchronized void sync() {
            /* empty */
        }

        public void varargs(Object... o) {
            /* empty */
        }

        public strictfp void strict() {
            /* empty */
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
        }
    }

    @SuppressWarnings("unused")
    private static class TestBean {

        public String getProperty() {
            return null;
        }

        public void setProperty(String property) {
            /* empty */
        }
    }

    private MethodDescription testClassBase$foo;
    private MethodDescription testClassBase$bar;
    private MethodDescription testClassBase$baz;
    private MethodDescription testClassBase$qux;
    private MethodDescription testClassBase$fin;
    private MethodDescription testClassBase$stat;

    private MethodDescription testClassBase$compareTo;
    private MethodDescription testClassBase$compareTo$synth;

    private MethodDescription testClassExtension$foo;
    private MethodDescription testClassExtension$bar;
    private MethodDescription testClassExtension$baz;
    private MethodDescription testClassExtension$qux;
    private MethodDescription testClassExtension$fin;
    private MethodDescription testClassExtension$stat;

    private MethodDescription testClassBase$foobar;
    private MethodDescription testClassExtension$fooBar;

    private MethodDescription testBean$getter;
    private MethodDescription testBean$setter;

    private MethodDescription object$hashCode;
    private MethodDescription object$finalize;
    private MethodDescription testModifier$finalize;

    private MethodDescription testModifier$sync;
    private MethodDescription testModifier$varargs;
    private MethodDescription testModifier$strict;

    private MethodDescription testModifier$constructor;
    private MethodDescription testClassBase$constructor;

    @Before
    public void setUp() throws Exception {
        testClassBase$foo = new MethodDescription.ForMethod(TestClassBase.class.getDeclaredMethod(FOO_METHOD_NAME));
        testClassBase$bar = new MethodDescription.ForMethod(TestClassBase.class.getDeclaredMethod(BAR_METHOD_NAME, Object.class));
        testClassBase$baz = new MethodDescription.ForMethod(TestClassBase.class.getDeclaredMethod(BAZ_METHOD_NAME));
        testClassBase$qux = new MethodDescription.ForMethod(TestClassBase.class.getDeclaredMethod(QUX_METHOD_NAME));
        testClassBase$fin = new MethodDescription.ForMethod(TestClassBase.class.getDeclaredMethod(FIN_METHOD_NAME + "1"));
        testClassBase$stat = new MethodDescription.ForMethod(TestClassBase.class.getDeclaredMethod(STAT_METHOD_NAME));

        testClassBase$compareTo$synth = new MethodDescription.ForMethod(TestClassBase.class.getDeclaredMethod(GENERIC_INTERFACE_METHOD_NAME, Object.class));
        testClassBase$compareTo = new MethodDescription.ForMethod(TestClassBase.class.getDeclaredMethod(GENERIC_INTERFACE_METHOD_NAME, String.class));

        testClassExtension$foo = new MethodDescription.ForMethod(TestClassExtension.class.getDeclaredMethod(FOO_METHOD_NAME));
        testClassExtension$bar = new MethodDescription.ForMethod(TestClassExtension.class.getDeclaredMethod(BAR_METHOD_NAME, Object.class));
        testClassExtension$baz = new MethodDescription.ForMethod(TestClassExtension.class.getDeclaredMethod(BAZ_METHOD_NAME));
        testClassExtension$qux = new MethodDescription.ForMethod(TestClassExtension.class.getDeclaredMethod(QUX_METHOD_NAME));
        testClassExtension$fin = new MethodDescription.ForMethod(TestClassExtension.class.getDeclaredMethod(FIN_METHOD_NAME + "2"));
        testClassExtension$stat = new MethodDescription.ForMethod(TestClassExtension.class.getDeclaredMethod(STAT_METHOD_NAME));

        object$hashCode = new MethodDescription.ForMethod(Object.class.getDeclaredMethod(HASH_CODE_METHOD_NAME));
        object$finalize = new MethodDescription.ForMethod(Object.class.getDeclaredMethod(FINALIZE_METHOD_NAME));
        testModifier$finalize = new MethodDescription.ForMethod(TestModifier.class.getDeclaredMethod(FINALIZE_METHOD_NAME));

        testClassBase$foobar = new MethodDescription.ForMethod(TestClassBase.class.getDeclaredMethod(FOOBAR_METHOD_NAME));
        testClassExtension$fooBar = new MethodDescription.ForMethod(TestClassExtension.class.getDeclaredMethod(FOOBAR_METHOD_NAME));

        testBean$getter = new MethodDescription.ForMethod(TestBean.class.getDeclaredMethod(GET_PROPERTY_METHOD_NAME));
        testBean$setter = new MethodDescription.ForMethod(TestBean.class.getDeclaredMethod(SET_PROPERTY_METHOD_NAME, String.class));

        testModifier$sync = new MethodDescription.ForMethod(TestModifier.class.getDeclaredMethod(SYNC_METHOD_NAME));
        testModifier$varargs = new MethodDescription.ForMethod(TestModifier.class.getDeclaredMethod(VARARGS_METHOD_NAME, Object[].class));
        testModifier$strict = new MethodDescription.ForMethod(TestModifier.class.getDeclaredMethod(STRICT_METHOD_NAME));

        testModifier$constructor = new MethodDescription.ForConstructor(TestModifier.class.getDeclaredConstructor());
        testClassBase$constructor = new MethodDescription.ForConstructor(TestClassBase.class.getDeclaredConstructor());
    }

    @Test
    public void testNamed() throws Exception {
        assertThat(MethodMatchers.named(FOO_METHOD_NAME).matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.named(FOO_METHOD_NAME).matches(testClassExtension$foo), is(true));
        assertThat(MethodMatchers.named(BAR_METHOD_NAME).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.named(BAR_METHOD_NAME).matches(testClassExtension$foo), is(false));
    }

    @Test
    public void testNamedIgnoreCase() throws Exception {
        assertThat(MethodMatchers.namedIgnoreCase(FOO_METHOD_NAME.toUpperCase()).matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.namedIgnoreCase(FOO_METHOD_NAME.toUpperCase()).matches(testClassExtension$foo), is(true));
        assertThat(MethodMatchers.namedIgnoreCase(BAR_METHOD_NAME.toUpperCase()).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.namedIgnoreCase(BAR_METHOD_NAME.toUpperCase()).matches(testClassExtension$foo), is(false));
    }

    @Test
    public void testNameStartsWith() throws Exception {
        assertThat(MethodMatchers.nameStartsWith(FOO_METHOD_NAME.substring(0, 1)).matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.nameStartsWith(FOO_METHOD_NAME.substring(0, 1)).matches(testClassExtension$foo), is(true));
        assertThat(MethodMatchers.nameStartsWith(BAR_METHOD_NAME.substring(0, 1)).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.nameStartsWith(BAR_METHOD_NAME.substring(0, 1)).matches(testClassExtension$foo), is(false));
    }

    @Test
    public void testNameStartsWithIgnoreCase() throws Exception {
        assertThat(MethodMatchers.nameStartsWithIgnoreCase(FOO_METHOD_NAME.toUpperCase().substring(0, 1)).matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.nameStartsWithIgnoreCase(FOO_METHOD_NAME.toUpperCase().substring(0, 1)).matches(testClassExtension$foo), is(true));
        assertThat(MethodMatchers.nameStartsWithIgnoreCase(BAR_METHOD_NAME.toUpperCase().substring(0, 1)).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.nameStartsWithIgnoreCase(BAR_METHOD_NAME.toUpperCase().substring(0, 1)).matches(testClassExtension$foo), is(false));
    }

    @Test
    public void testNameEndsWith() throws Exception {
        assertThat(MethodMatchers.nameEndsWith(FOO_METHOD_NAME.substring(2)).matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.nameEndsWith(FOO_METHOD_NAME.substring(2)).matches(testClassExtension$foo), is(true));
        assertThat(MethodMatchers.nameEndsWith(BAR_METHOD_NAME.substring(2)).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.nameEndsWith(BAR_METHOD_NAME.substring(2)).matches(testClassExtension$foo), is(false));
    }

    @Test
    public void testNameEndsWithIgnoreCase() throws Exception {
        assertThat(MethodMatchers.nameEndsWithIgnoreCase(FOO_METHOD_NAME.toUpperCase().substring(2)).matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.nameEndsWithIgnoreCase(FOO_METHOD_NAME.toUpperCase().substring(2)).matches(testClassExtension$foo), is(true));
        assertThat(MethodMatchers.nameEndsWithIgnoreCase(BAR_METHOD_NAME.toUpperCase().substring(2)).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.nameEndsWithIgnoreCase(BAR_METHOD_NAME.toUpperCase().substring(2)).matches(testClassExtension$foo), is(false));
    }

    @Test
    public void testNameContains() throws Exception {
        assertThat(MethodMatchers.nameContains(FOO_METHOD_NAME.substring(1, 2)).matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.nameContains(FOO_METHOD_NAME.substring(1, 2)).matches(testClassExtension$foo), is(true));
        assertThat(MethodMatchers.nameContains(BAR_METHOD_NAME.substring(1, 2)).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.nameContains(BAR_METHOD_NAME.substring(1, 2)).matches(testClassExtension$foo), is(false));
    }

    @Test
    public void testNameContainsIgnoreCase() throws Exception {
        assertThat(MethodMatchers.nameContainsIgnoreCase(FOO_METHOD_NAME.toUpperCase().substring(1, 2)).matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.nameContainsIgnoreCase(FOO_METHOD_NAME.toUpperCase().substring(1, 2)).matches(testClassExtension$foo), is(true));
        assertThat(MethodMatchers.nameContainsIgnoreCase(BAR_METHOD_NAME.toUpperCase().substring(1, 2)).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.nameContainsIgnoreCase(BAR_METHOD_NAME.toUpperCase().substring(1, 2)).matches(testClassExtension$foo), is(false));
    }

    @Test
    public void testMatches() throws Exception {
        assertThat(MethodMatchers.nameMatches(FOO_METHOD_NAME_REGEX).matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.nameMatches(FOO_METHOD_NAME_REGEX).matches(testClassExtension$foo), is(true));
        assertThat(MethodMatchers.nameMatches(BAR_METHOD_NAME_REGEX).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.nameMatches(BAR_METHOD_NAME_REGEX).matches(testClassExtension$foo), is(false));
    }

    @Test
    public void testIsPublic() throws Exception {
        assertThat(MethodMatchers.isPublic().matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.isPublic().matches(testClassBase$bar), is(false));
        assertThat(MethodMatchers.isPublic().matches(testClassBase$baz), is(false));
        assertThat(MethodMatchers.isPublic().matches(testClassBase$qux), is(false));
        assertThat(MethodMatchers.isPublic().matches(testClassExtension$foo), is(true));
        assertThat(MethodMatchers.isPublic().matches(testClassExtension$bar), is(false));
        assertThat(MethodMatchers.isPublic().matches(testClassExtension$baz), is(false));
        assertThat(MethodMatchers.isPublic().matches(testClassExtension$qux), is(false));
    }

    @Test
    public void testIsProtected() throws Exception {
        assertThat(MethodMatchers.isProtected().matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.isProtected().matches(testClassBase$bar), is(false));
        assertThat(MethodMatchers.isProtected().matches(testClassBase$baz), is(true));
        assertThat(MethodMatchers.isProtected().matches(testClassBase$qux), is(false));
        assertThat(MethodMatchers.isProtected().matches(testClassExtension$foo), is(false));
        assertThat(MethodMatchers.isProtected().matches(testClassExtension$bar), is(false));
        assertThat(MethodMatchers.isProtected().matches(testClassExtension$baz), is(true));
        assertThat(MethodMatchers.isProtected().matches(testClassExtension$qux), is(false));
    }

    @Test
    public void testIsPackagePrivate() throws Exception {
        assertThat(MethodMatchers.isPackagePrivate().matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.isPackagePrivate().matches(testClassBase$bar), is(false));
        assertThat(MethodMatchers.isPackagePrivate().matches(testClassBase$baz), is(false));
        assertThat(MethodMatchers.isPackagePrivate().matches(testClassBase$qux), is(true));
        assertThat(MethodMatchers.isPackagePrivate().matches(testClassExtension$foo), is(false));
        assertThat(MethodMatchers.isPackagePrivate().matches(testClassExtension$bar), is(false));
        assertThat(MethodMatchers.isPackagePrivate().matches(testClassExtension$baz), is(false));
        assertThat(MethodMatchers.isPackagePrivate().matches(testClassExtension$qux), is(true));
    }

    @Test
    public void testIsPrivate() throws Exception {
        assertThat(MethodMatchers.isPrivate().matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.isPrivate().matches(testClassBase$bar), is(true));
        assertThat(MethodMatchers.isPrivate().matches(testClassBase$baz), is(false));
        assertThat(MethodMatchers.isPrivate().matches(testClassBase$qux), is(false));
        assertThat(MethodMatchers.isPrivate().matches(testClassExtension$foo), is(false));
        assertThat(MethodMatchers.isPrivate().matches(testClassExtension$bar), is(true));
        assertThat(MethodMatchers.isPrivate().matches(testClassExtension$baz), is(false));
        assertThat(MethodMatchers.isPrivate().matches(testClassExtension$qux), is(false));
    }

    @Test
    public void testIsFinal() throws Exception {
        assertThat(MethodMatchers.isFinal().matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.isFinal().matches(testClassBase$bar), is(false));
        assertThat(MethodMatchers.isFinal().matches(testClassBase$fin), is(true));
        assertThat(MethodMatchers.isFinal().matches(testClassExtension$foo), is(false));
        assertThat(MethodMatchers.isFinal().matches(testClassExtension$bar), is(false));
        assertThat(MethodMatchers.isFinal().matches(testClassExtension$fin), is(true));
    }

    @Test
    public void testIsStatic() throws Exception {
        assertThat(MethodMatchers.isStatic().matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.isStatic().matches(testClassBase$bar), is(false));
        assertThat(MethodMatchers.isStatic().matches(testClassBase$stat), is(true));
        assertThat(MethodMatchers.isStatic().matches(testClassExtension$foo), is(false));
        assertThat(MethodMatchers.isStatic().matches(testClassExtension$bar), is(false));
        assertThat(MethodMatchers.isStatic().matches(testClassExtension$stat), is(true));
    }

    @Test
    public void testIsSynchronized() throws Exception {
        assertThat(MethodMatchers.isSynchronized().matches(testModifier$strict), is(false));
        assertThat(MethodMatchers.isSynchronized().matches(testModifier$sync), is(true));
        assertThat(MethodMatchers.isSynchronized().matches(testModifier$varargs), is(false));
    }

    @Test
    public void testIsNative() throws Exception {
        assertThat(MethodMatchers.isNative().matches(testClassExtension$stat), is(false));
        assertThat(MethodMatchers.isNative().matches(object$hashCode), is(true));
    }

    @Test
    public void testIsStrict() throws Exception {
        assertThat(MethodMatchers.isStrict().matches(testModifier$strict), is(true));
        assertThat(MethodMatchers.isStrict().matches(testModifier$sync), is(false));
        assertThat(MethodMatchers.isStrict().matches(testModifier$varargs), is(false));

    }

    @Test
    public void testIsVarArgs() throws Exception {
        assertThat(MethodMatchers.isVarArgs().matches(testModifier$strict), is(false));
        assertThat(MethodMatchers.isVarArgs().matches(testModifier$sync), is(false));
        assertThat(MethodMatchers.isVarArgs().matches(testModifier$varargs), is(true));
    }

    @Test
    public void testIsSynthetic() throws Exception {
        assertThat(MethodMatchers.isSynthetic().matches(testClassBase$compareTo$synth), is(true));
        assertThat(MethodMatchers.isSynthetic().matches(testClassBase$compareTo), is(false));
    }

    @Test
    public void testIsBridge() throws Exception {
        assertThat(MethodMatchers.isBridge().matches(testClassBase$compareTo$synth), is(true));
        assertThat(MethodMatchers.isBridge().matches(testClassBase$compareTo), is(false));
    }

    @Test
    public void testReturns() throws Exception {
        assertThat(MethodMatchers.returns(Object.class).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.returns(Object.class).matches(testClassBase$bar), is(true));
        assertThat(MethodMatchers.returns(String.class).matches(testClassBase$bar), is(false));
        assertThat(MethodMatchers.takesArguments(Object.class).matches(testClassExtension$foo), is(false));
        assertThat(MethodMatchers.takesArguments(Object.class).matches(testClassExtension$bar), is(true));
        assertThat(MethodMatchers.takesArguments(String.class).matches(testClassExtension$bar), is(false));
    }

    @Test
    public void testTakesArgumentsTypes() throws Exception {
        assertThat(MethodMatchers.takesArguments(Object.class).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.takesArguments(Object.class).matches(testClassBase$bar), is(true));
        assertThat(MethodMatchers.takesArguments(String.class).matches(testClassBase$bar), is(false));
        assertThat(MethodMatchers.takesArguments(Object.class).matches(testClassExtension$foo), is(false));
        assertThat(MethodMatchers.takesArguments(Object.class).matches(testClassExtension$bar), is(true));
        assertThat(MethodMatchers.takesArguments(String.class).matches(testClassExtension$bar), is(false));
    }

    @Test
    public void testTakesArgumentsNumeric() throws Exception {
        assertThat(MethodMatchers.takesArguments(0).matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.takesArguments(1).matches(testClassBase$bar), is(true));
        assertThat(MethodMatchers.takesArguments(1).matches(testClassBase$bar), is(true));
        assertThat(MethodMatchers.takesArguments(1).matches(testClassExtension$foo), is(false));
        assertThat(MethodMatchers.takesArguments(2).matches(testClassExtension$bar), is(false));
        assertThat(MethodMatchers.takesArguments(0).matches(testClassExtension$bar), is(false));
    }

    @Test
    public void testCanThrow() throws Exception {
        assertThat(MethodMatchers.canThrow(RuntimeException.class).matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.canThrow(RuntimeException.class).matches(testClassBase$bar), is(true));
        assertThat(MethodMatchers.canThrow(Exception.class).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.canThrow(Exception.class).matches(testClassBase$bar), is(true));
        assertThat(MethodMatchers.canThrow(RuntimeException.class).matches(testClassExtension$foo), is(true));
        assertThat(MethodMatchers.canThrow(RuntimeException.class).matches(testClassExtension$bar), is(true));
        assertThat(MethodMatchers.canThrow(Exception.class).matches(testClassExtension$bar), is(false));
    }

    @Test
    public void testIsGivenMethod() throws Exception {
        assertThat(MethodMatchers.is(TestClassBase.class.getDeclaredMethod(FOO_METHOD_NAME)).matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.is(TestClassExtension.class.getDeclaredMethod(FOO_METHOD_NAME)).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.is(TestClassBase.class.getDeclaredMethod(FOO_METHOD_NAME)).matches(testClassExtension$foo), is(false));
        assertThat(MethodMatchers.is(TestClassExtension.class.getDeclaredMethod(FOO_METHOD_NAME)).matches(testClassExtension$foo), is(true));
    }

    @Test
    public void testIsGivenConstructor() throws Exception {
        assertThat(MethodMatchers.is(TestModifier.class.getDeclaredConstructor()).matches(testModifier$constructor), is(true));
        assertThat(MethodMatchers.is(TestModifier.class.getDeclaredConstructor()).matches(testClassBase$constructor), is(false));
        assertThat(MethodMatchers.is(TestClassBase.class.getDeclaredConstructor()).matches(testClassBase$foo), is(false));
    }

    @Test
    public void testIsGivenMethodDescription() throws Exception {
        assertThat(MethodMatchers.is(testClassBase$foo).matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.is(testClassExtension$foo).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.is(testClassBase$foo).matches(testClassExtension$foo), is(false));
        assertThat(MethodMatchers.is(testClassExtension$foo).matches(testClassExtension$foo), is(true));
        assertThat(MethodMatchers.is(testModifier$constructor).matches(testModifier$constructor), is(true));
        assertThat(MethodMatchers.is(testClassBase$constructor).matches(testClassBase$foo), is(false));
    }

    @Test
    public void testIsMethod() throws Exception {
        assertThat(MethodMatchers.isMethod().matches(testModifier$constructor), is(false));
        assertThat(MethodMatchers.isMethod().matches(testClassBase$constructor), is(false));
        assertThat(MethodMatchers.isMethod().matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.isMethod().matches(testClassBase$bar), is(true));
    }

    @Test
    public void testIsConstructor() throws Exception {
        assertThat(MethodMatchers.isConstructor().matches(testModifier$constructor), is(true));
        assertThat(MethodMatchers.isConstructor().matches(testClassBase$constructor), is(true));
        assertThat(MethodMatchers.isConstructor().matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.isConstructor().matches(testClassBase$bar), is(false));
    }

    @Test
    public void testIsVisibleFromPackage() throws Exception {
        assertThat(MethodMatchers.isVisibleFromPackage(MethodMatchersTest.class.getPackage().getName()).matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.isVisibleFromPackage(MethodMatchersTest.class.getPackage().getName()).matches(testClassExtension$foo), is(true));
        assertThat(MethodMatchers.isVisibleFromPackage(JAVA_LANG_PACKAGE).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.isVisibleFromPackage(JAVA_LANG_PACKAGE).matches(testClassExtension$foo), is(false));
    }

    @Test
    public void testIsDeclaredBy() throws Exception {
        assertThat(MethodMatchers.isDeclaredBy(TestModifier.class).matches(testModifier$finalize), is(true));
        assertThat(MethodMatchers.isDeclaredBy(TestModifier.class).matches(testClassExtension$fooBar), is(false));
        assertThat(MethodMatchers.isDeclaredBy(TestModifier.class).matches(testClassBase$foo), is(false));
    }

    @Test
    public void testSetter() throws Exception {
        assertThat(MethodMatchers.isSetter().matches(testBean$setter), is(true));
        assertThat(MethodMatchers.isSetter().matches(testBean$getter), is(false));
        assertThat(MethodMatchers.isSetter(String.class).matches(testBean$setter), is(true));
        assertThat(MethodMatchers.isSetter(Object.class).matches(testBean$setter), is(false));
    }

    @Test
    public void testGetter() throws Exception {
        assertThat(MethodMatchers.isGetter().matches(testBean$getter), is(true));
        assertThat(MethodMatchers.isGetter().matches(testBean$setter), is(false));
        assertThat(MethodMatchers.isGetter(String.class).matches(testBean$getter), is(true));
        assertThat(MethodMatchers.isGetter(Object.class).matches(testBean$getter), is(false));
    }

    @Test
    public void testHasSameByteCodeSignatureAs() throws Exception {
        assertThat(MethodMatchers.hasSameByteCodeSignatureAs(object$finalize).matches(testModifier$finalize), is(true));
        assertThat(MethodMatchers.hasSameByteCodeSignatureAs(testClassBase$foobar).matches(testClassExtension$fooBar), is(false));
        assertThat(MethodMatchers.hasSameByteCodeSignatureAs(object$finalize).matches(testClassBase$foo), is(false));
    }

    @Test
    public void testHasSameJavaCompilerSignatureAs() throws Exception {
        assertThat(MethodMatchers.hasSameJavaCompilerSignatureAs(object$finalize).matches(testModifier$finalize), is(true));
        assertThat(MethodMatchers.hasSameJavaCompilerSignatureAs(testClassBase$foobar).matches(testClassExtension$fooBar), is(true));
        assertThat(MethodMatchers.hasSameJavaCompilerSignatureAs(object$finalize).matches(testClassBase$foo), is(false));
    }

    @Test
    public void testIsOverridable() throws Exception {
        assertThat(MethodMatchers.isOverridable().matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.isOverridable().matches(testClassBase$bar), is(false));
        assertThat(MethodMatchers.isOverridable().matches(testClassBase$stat), is(false));
        assertThat(MethodMatchers.isOverridable().matches(testModifier$constructor), is(false));
    }

    @Test
    public void testIsDefaultFinalizer() throws Exception {
        assertThat(MethodMatchers.isDefaultFinalizer().matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.isDefaultFinalizer().matches(object$finalize), is(true));
        assertThat(MethodMatchers.isDefaultFinalizer().matches(testModifier$finalize), is(false));
    }

    @Test
    public void testIsFinalizer() throws Exception {
        assertThat(MethodMatchers.isFinalizer().matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.isFinalizer().matches(object$finalize), is(true));
        assertThat(MethodMatchers.isFinalizer().matches(testModifier$finalize), is(true));
    }

    @Test
    public void testNot() throws Exception {
        assertThat(MethodMatchers.not(MethodMatchers.any()).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.not(MethodMatchers.any()).matches(testClassExtension$foo), is(false));
    }

    @Test
    public void testAny() throws Exception {
        assertThat(MethodMatchers.any().matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.any().matches(testClassExtension$foo), is(true));
    }

    @Test
    public void testNone() throws Exception {
        assertThat(MethodMatchers.none().matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.none().matches(testClassExtension$foo), is(false));
    }
}
