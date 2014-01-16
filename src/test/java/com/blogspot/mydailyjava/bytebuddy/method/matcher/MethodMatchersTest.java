package com.blogspot.mydailyjava.bytebuddy.method.matcher;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
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

    private static final String GENERIC_INTERFACE_METHOD_NAME = "gen";

    private static final String FOO_METHOD_NAME_REGEX = "fo{2}";
    private static final String BAR_METHOD_NAME_REGEX = "b[a]r";

    private static final String JAVA_LANG_PACKAGE = "java.lang";

    @SuppressWarnings("unused")
    private static interface TestInterface<T> {

        void gen(T o);
    }

    @SuppressWarnings("unused")
    private static class TestClassBase implements TestInterface<String> {

        public void foo() {
            /* empty */
        }

        private Object bar(Object o) throws RuntimeException {
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
    }

    @Test
    public void testDeclaredIn() throws Exception {
        assertThat(MethodMatchers.declaredIn(Object.class).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.declaredIn(TestClassBase.class).matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.declaredIn(TestClassExtension.class).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.declaredIn(Object.class).matches(testClassExtension$foo), is(false));
        assertThat(MethodMatchers.declaredIn(TestClassBase.class).matches(testClassExtension$foo), is(false));
        assertThat(MethodMatchers.declaredIn(TestClassExtension.class).matches(testClassExtension$foo), is(true));
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
        assertThat(MethodMatchers.matches(FOO_METHOD_NAME_REGEX).matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.matches(FOO_METHOD_NAME_REGEX).matches(testClassExtension$foo), is(true));
        assertThat(MethodMatchers.matches(BAR_METHOD_NAME_REGEX).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.matches(BAR_METHOD_NAME_REGEX).matches(testClassExtension$foo), is(false));
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
    public void testTakesArguments() throws Exception {
        assertThat(MethodMatchers.takesArguments(Object.class).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.takesArguments(Object.class).matches(testClassBase$bar), is(true));
        assertThat(MethodMatchers.takesArguments(String.class).matches(testClassBase$bar), is(false));
        assertThat(MethodMatchers.takesArguments(Object.class).matches(testClassExtension$foo), is(false));
        assertThat(MethodMatchers.takesArguments(Object.class).matches(testClassExtension$bar), is(true));
        assertThat(MethodMatchers.takesArguments(String.class).matches(testClassExtension$bar), is(false));
    }

    @Test
    public void testCanThrow() throws Exception {
        assertThat(MethodMatchers.canThrow(RuntimeException.class).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.canThrow(RuntimeException.class).matches(testClassBase$bar), is(true));
        assertThat(MethodMatchers.canThrow(Exception.class).matches(testClassBase$bar), is(false));
        assertThat(MethodMatchers.canThrow(RuntimeException.class).matches(testClassExtension$foo), is(false));
        assertThat(MethodMatchers.canThrow(RuntimeException.class).matches(testClassExtension$bar), is(true));
        assertThat(MethodMatchers.canThrow(Exception.class).matches(testClassExtension$bar), is(false));
    }

    @Test
    public void testIs() throws Exception {
        assertThat(MethodMatchers.is(TestClassBase.class.getDeclaredMethod(FOO_METHOD_NAME)).matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.is(TestClassExtension.class.getDeclaredMethod(FOO_METHOD_NAME)).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.is(TestClassBase.class.getDeclaredMethod(FOO_METHOD_NAME)).matches(testClassExtension$foo), is(false));
        assertThat(MethodMatchers.is(TestClassExtension.class.getDeclaredMethod(FOO_METHOD_NAME)).matches(testClassExtension$foo), is(true));
    }

    @Test
    public void testIsDefinedInPackage() throws Exception{
        assertThat(MethodMatchers.isDefinedInPackage(MethodMatchersTest.class.getPackage().getName()).matches(testClassBase$foo), is(true));
        assertThat(MethodMatchers.isDefinedInPackage(MethodMatchersTest.class.getPackage().getName()).matches(testClassExtension$foo), is(true));
        assertThat(MethodMatchers.isDefinedInPackage(JAVA_LANG_PACKAGE).matches(testClassBase$foo), is(false));
        assertThat(MethodMatchers.isDefinedInPackage(JAVA_LANG_PACKAGE).matches(testClassExtension$foo), is(false));
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
