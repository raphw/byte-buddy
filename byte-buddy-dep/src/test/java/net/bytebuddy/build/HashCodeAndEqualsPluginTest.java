package net.bytebuddy.build;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.EqualsMethod;
import org.junit.Test;

import java.util.Comparator;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HashCodeAndEqualsPluginTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Test
    public void testPluginMatches() throws Exception {
        Plugin plugin = new HashCodeAndEqualsPlugin();
        assertThat(plugin.matches(TypeDescription.ForLoadedType.of(SimpleSample.class)), is(true));
        assertThat(plugin.matches(TypeDescription.OBJECT), is(false));
    }

    @Test
    public void testPluginEnhance() throws Exception {
        Class<?> type = new HashCodeAndEqualsPlugin()
                .apply(new ByteBuddy().redefine(SimpleSample.class), TypeDescription.ForLoadedType.of(SimpleSample.class), ClassFileLocator.ForClassLoader.of(SimpleSample.class.getClassLoader()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredConstructor().newInstance().hashCode(), is(type.getDeclaredConstructor().newInstance().hashCode()));
        assertThat(type.getDeclaredConstructor().newInstance(), is(type.getDeclaredConstructor().newInstance()));
    }

    @Test
    public void testPluginEnhanceRedundant() throws Exception {
        Class<?> type = new HashCodeAndEqualsPlugin()
                .apply(new ByteBuddy().redefine(RedundantSample.class), TypeDescription.ForLoadedType.of(RedundantSample.class), ClassFileLocator.ForClassLoader.of(RedundantSample.class.getClassLoader()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredConstructor().newInstance().hashCode(), is(42));
        assertThat(type.getDeclaredConstructor().newInstance(), is(new Object()));
    }

    @Test
    public void testPluginEnhanceIgnore() throws Exception {
        Class<?> type = new HashCodeAndEqualsPlugin()
                .apply(new ByteBuddy().redefine(IgnoredFieldSample.class), TypeDescription.ForLoadedType.of(IgnoredFieldSample.class), ClassFileLocator.ForClassLoader.of(IgnoredFieldSample.class.getClassLoader()))
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
                .apply(new ByteBuddy().redefine(NonNullableField.class), TypeDescription.ForLoadedType.of(NonNullableField.class), ClassFileLocator.ForClassLoader.of(NonNullableField.class.getClassLoader()))
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
                .apply(new ByteBuddy().redefine(NonNullableField.class), TypeDescription.ForLoadedType.of(NonNullableField.class), ClassFileLocator.ForClassLoader.of(NonNullableField.class.getClassLoader()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        type.getDeclaredConstructor().newInstance().equals(type.getDeclaredConstructor().newInstance());
    }

    @Test
    public void testPluginEnhanceNonNullableReversed() throws Exception {
        Class<?> type = new HashCodeAndEqualsPlugin.WithNonNullableFields()
                .apply(new ByteBuddy().redefine(NonNullableField.class), TypeDescription.ForLoadedType.of(NonNullableField.class), ClassFileLocator.ForClassLoader.of(NonNullableField.class.getClassLoader()))
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
                .apply(new ByteBuddy().redefine(SimpleSample.class), TypeDescription.ForLoadedType.of(SimpleSample.class), ClassFileLocator.ForClassLoader.of(SimpleSample.class.getClassLoader()))
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
                .apply(new ByteBuddy().redefine(SimpleSample.class), TypeDescription.ForLoadedType.of(SimpleSample.class), ClassFileLocator.ForClassLoader.of(SimpleSample.class.getClassLoader()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        type.getDeclaredConstructor().newInstance().equals(type.getDeclaredConstructor().newInstance());
    }

    @Test
    public void testPluginFieldOrder() throws Exception {
        Class<?> type = new HashCodeAndEqualsPlugin.WithNonNullableFields()
                .apply(new ByteBuddy().redefine(FieldSortOrderSample.class), TypeDescription.ForLoadedType.of(FieldSortOrderSample.class), ClassFileLocator.ForClassLoader.of(FieldSortOrderSample.class.getClassLoader()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object left = type.getDeclaredConstructor().newInstance(), right = type.getDeclaredConstructor().newInstance();
        type.getDeclaredField(QUX).set(left, FOO);
        type.getDeclaredField(QUX).set(right, BAR);
        assertThat(left.equals(right), is(false));
    }

    @Test
    public void testInvokeSuper() {
        assertThat(HashCodeAndEqualsPlugin.Enhance.InvokeSuper.IF_ANNOTATED.equalsMethod(TypeDescription.ForLoadedType.of(SimpleSample.class)),
                hasPrototype(EqualsMethod.isolated()));
        assertThat(HashCodeAndEqualsPlugin.Enhance.InvokeSuper.IF_ANNOTATED.equalsMethod(TypeDescription.ForLoadedType.of(SimpleSampleSubclass.class)),
                hasPrototype(EqualsMethod.requiringSuperClassEquality()));
        assertThat(HashCodeAndEqualsPlugin.Enhance.InvokeSuper.IF_DECLARED.equalsMethod(TypeDescription.ForLoadedType.of(SimpleSample.class)),
                hasPrototype(EqualsMethod.isolated()));
        assertThat(HashCodeAndEqualsPlugin.Enhance.InvokeSuper.IF_DECLARED.equalsMethod(TypeDescription.ForLoadedType.of(SimpleSampleSubclass.class)),
                hasPrototype(EqualsMethod.requiringSuperClassEquality()));
        assertThat(HashCodeAndEqualsPlugin.Enhance.InvokeSuper.IF_DECLARED.equalsMethod(TypeDescription.ForLoadedType.of(DeclaredSubclass.class)),
                hasPrototype(EqualsMethod.requiringSuperClassEquality()));
        assertThat(HashCodeAndEqualsPlugin.Enhance.InvokeSuper.ALWAYS.equalsMethod(TypeDescription.ForLoadedType.of(SimpleSample.class)),
                hasPrototype(EqualsMethod.requiringSuperClassEquality()));
        assertThat(HashCodeAndEqualsPlugin.Enhance.InvokeSuper.ALWAYS.equalsMethod(TypeDescription.ForLoadedType.of(SimpleSampleSubclass.class)),
                hasPrototype(EqualsMethod.requiringSuperClassEquality()));
        assertThat(HashCodeAndEqualsPlugin.Enhance.InvokeSuper.NEVER.equalsMethod(TypeDescription.ForLoadedType.of(SimpleSample.class)),
                hasPrototype(EqualsMethod.isolated()));
        assertThat(HashCodeAndEqualsPlugin.Enhance.InvokeSuper.NEVER.equalsMethod(TypeDescription.ForLoadedType.of(SimpleSampleSubclass.class)),
                hasPrototype(EqualsMethod.isolated()));
    }

    @Test
    public void testAnnotationComparatorEqualsNoAnnotations() {
        Comparator<FieldDescription.InDefinedShape> comparator = HashCodeAndEqualsPlugin.AnnotationOrderComparator.INSTANCE;
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        when(left.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(right.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        assertThat(comparator.compare(left, right), is(0));
    }

    @Test
    public void testAnnotationComparatorEqualsEqualAnnotations() {
        Comparator<FieldDescription.InDefinedShape> comparator = HashCodeAndEqualsPlugin.AnnotationOrderComparator.INSTANCE;
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        when(left.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(AnnotationDescription.Builder.ofType(HashCodeAndEqualsPlugin.Sorted.class)
                .define("value", 0)
                .build()));
        when(right.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(AnnotationDescription.Builder.ofType(HashCodeAndEqualsPlugin.Sorted.class)
                .define("value", 0)
                .build()));
        assertThat(comparator.compare(left, right), is(0));
    }

    @Test
    public void testAnnotationComparatorEqualsLeftBiggerAnnotations() {
        Comparator<FieldDescription.InDefinedShape> comparator = HashCodeAndEqualsPlugin.AnnotationOrderComparator.INSTANCE;
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        when(left.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(AnnotationDescription.Builder.ofType(HashCodeAndEqualsPlugin.Sorted.class)
                .define("value", 42)
                .build()));
        when(right.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(AnnotationDescription.Builder.ofType(HashCodeAndEqualsPlugin.Sorted.class)
                .define("value", 0)
                .build()));
        assertThat(comparator.compare(left, right), is(-1));
    }

    @Test
    public void testAnnotationComparatorEqualsRightBiggerAnnotations() {
        Comparator<FieldDescription.InDefinedShape> comparator = HashCodeAndEqualsPlugin.AnnotationOrderComparator.INSTANCE;
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        when(left.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(AnnotationDescription.Builder.ofType(HashCodeAndEqualsPlugin.Sorted.class)
                .define("value", 0)
                .build()));
        when(right.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(AnnotationDescription.Builder.ofType(HashCodeAndEqualsPlugin.Sorted.class)
                .define("value", 42)
                .build()));
        assertThat(comparator.compare(left, right), is(1));
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

    @HashCodeAndEqualsPlugin.Enhance
    public static class FieldSortOrderSample {

        public String foo;

        @HashCodeAndEqualsPlugin.Sorted(-1)
        public String bar;

        @HashCodeAndEqualsPlugin.Sorted(1)
        public String qux;
    }
}