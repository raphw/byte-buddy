package net.bytebuddy.build;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.EqualsMethod;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class HashCodeAndEqualsPluginTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testPluginMatches() throws Exception {
        Plugin plugin = new HashCodeAndEqualsPlugin();
        assertThat(plugin.matches(new TypeDescription.ForLoadedType(SimpleSample.class)), is(true));
        assertThat(plugin.matches(TypeDescription.OBJECT), is(false));
    }

    @Test
    public void testPluginEnhance() throws Exception {
        Class<?> type = new HashCodeAndEqualsPlugin()
                .apply(new ByteBuddy().redefine(SimpleSample.class), new TypeDescription.ForLoadedType(SimpleSample.class))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredConstructor().newInstance().hashCode(), is(type.getDeclaredConstructor().newInstance().hashCode()));
        assertThat(type.getDeclaredConstructor().newInstance(), is(type.getDeclaredConstructor().newInstance()));
    }

    @Test
    public void testPluginEnhanceRedundant() throws Exception {
        Class<?> type = new HashCodeAndEqualsPlugin()
                .apply(new ByteBuddy().redefine(RedundantSample.class), new TypeDescription.ForLoadedType(RedundantSample.class))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredConstructor().newInstance().hashCode(), is(42));
        assertThat(type.getDeclaredConstructor().newInstance(), is(new Object()));
    }

    @Test
    public void testPluginEnhanceIgnore() throws Exception {
        Class<?> type = new HashCodeAndEqualsPlugin()
                .apply(new ByteBuddy().redefine(IgnoredFieldSample.class), new TypeDescription.ForLoadedType(IgnoredFieldSample.class))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object left = type.getDeclaredConstructor().newInstance(), right = type.getDeclaredConstructor().newInstance();
        type.getDeclaredField(FOO).set(left, FOO);
        type.getDeclaredField(FOO).set(right, BAR);
        assertThat(left.hashCode(), is(right.hashCode()));
        assertThat(left, is(right));
    }

    @Test(expected = NullPointerException.class)
    public void testPluginEnhanceNonNullableHashCode() throws Exception {
        new HashCodeAndEqualsPlugin()
                .apply(new ByteBuddy().redefine(NonNullableField.class), new TypeDescription.ForLoadedType(NonNullableField.class))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()
                .hashCode();
    }

    @Test(expected = NullPointerException.class)
    public void testPluginEnhanceNonNullableEquals() throws Exception {
        Class<?> type = new HashCodeAndEqualsPlugin()
                .apply(new ByteBuddy().redefine(NonNullableField.class), new TypeDescription.ForLoadedType(NonNullableField.class))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        type.getDeclaredConstructor().newInstance().equals(type.getDeclaredConstructor().newInstance());
    }

    @Test
    public void testPluginEnhanceNonNullableReversed() throws Exception {
        Class<?> type = new HashCodeAndEqualsPlugin.WithNonNullableFields()
                .apply(new ByteBuddy().redefine(NonNullableField.class), new TypeDescription.ForLoadedType(NonNullableField.class))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object left = type.getDeclaredConstructor().newInstance(), right = type.getDeclaredConstructor().newInstance();
        assertThat(left.hashCode(), is(right.hashCode()));
        assertThat(left, is(right));
    }

    @Test(expected = NullPointerException.class)
    public void testPluginEnhanceNonNullableReversedHashCode() throws Exception {
        new HashCodeAndEqualsPlugin.WithNonNullableFields()
                .apply(new ByteBuddy().redefine(SimpleSample.class), new TypeDescription.ForLoadedType(SimpleSample.class))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()
                .hashCode();
    }

    @Test(expected = NullPointerException.class)
    public void testPluginEnhanceNonNullableReversedEquals() throws Exception {
        Class<?> type = new HashCodeAndEqualsPlugin.WithNonNullableFields()
                .apply(new ByteBuddy().redefine(SimpleSample.class), new TypeDescription.ForLoadedType(SimpleSample.class))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        type.getDeclaredConstructor().newInstance().equals(type.getDeclaredConstructor().newInstance());
    }

    @Test
    public void testInvokeSuper() {
        assertThat(HashCodeAndEqualsPlugin.Enhance.InvokeSuper.IF_ANNOTATED.equalsMethod(new TypeDescription.ForLoadedType(SimpleSample.class)),
                is(EqualsMethod.isolated()));
        assertThat(HashCodeAndEqualsPlugin.Enhance.InvokeSuper.IF_ANNOTATED.equalsMethod(new TypeDescription.ForLoadedType(SimpleSampleSubclass.class)),
                is(EqualsMethod.requiringSuperClassEquality()));
        assertThat(HashCodeAndEqualsPlugin.Enhance.InvokeSuper.IF_DECLARED.equalsMethod(new TypeDescription.ForLoadedType(SimpleSample.class)),
                is(EqualsMethod.isolated()));
        assertThat(HashCodeAndEqualsPlugin.Enhance.InvokeSuper.IF_DECLARED.equalsMethod(new TypeDescription.ForLoadedType(SimpleSampleSubclass.class)),
                is(EqualsMethod.requiringSuperClassEquality()));
        assertThat(HashCodeAndEqualsPlugin.Enhance.InvokeSuper.IF_DECLARED.equalsMethod(new TypeDescription.ForLoadedType(DeclaredSubclass.class)),
                is(EqualsMethod.requiringSuperClassEquality()));
        assertThat(HashCodeAndEqualsPlugin.Enhance.InvokeSuper.ALWAYS.equalsMethod(new TypeDescription.ForLoadedType(SimpleSample.class)),
                is(EqualsMethod.requiringSuperClassEquality()));
        assertThat(HashCodeAndEqualsPlugin.Enhance.InvokeSuper.ALWAYS.equalsMethod(new TypeDescription.ForLoadedType(SimpleSampleSubclass.class)),
                is(EqualsMethod.requiringSuperClassEquality()));
        assertThat(HashCodeAndEqualsPlugin.Enhance.InvokeSuper.NEVER.equalsMethod(new TypeDescription.ForLoadedType(SimpleSample.class)),
                is(EqualsMethod.isolated()));
        assertThat(HashCodeAndEqualsPlugin.Enhance.InvokeSuper.NEVER.equalsMethod(new TypeDescription.ForLoadedType(SimpleSampleSubclass.class)),
                is(EqualsMethod.isolated()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(HashCodeAndEqualsPlugin.class).apply();
        ObjectPropertyAssertion.of(HashCodeAndEqualsPlugin.ValueMatcher.class).apply();
    }

    @HashCodeAndEqualsPlugin.Enhance
    public static class SimpleSample {

        private String foo;
    }

    @HashCodeAndEqualsPlugin.Enhance
    public static class SimpleSampleSubclass extends SimpleSample {
        /* empty */
    }

    public static class Declared {

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object other) {
            return false;
        }
    }

    @HashCodeAndEqualsPlugin.Enhance
    public static class DeclaredSubclass extends Declared {
        /* empty */
    }

    @HashCodeAndEqualsPlugin.Enhance
    public static class RedundantSample {

        @Override
        public int hashCode() {
            return 42;
        }

        @Override
        public boolean equals(Object other) {
            return true;
        }
    }

    @HashCodeAndEqualsPlugin.Enhance
    public static class IgnoredFieldSample {

        @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.IGNORE)
        public String foo;
    }

    @HashCodeAndEqualsPlugin.Enhance
    public static class NonNullableField {

        @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
        public String foo;
    }
}