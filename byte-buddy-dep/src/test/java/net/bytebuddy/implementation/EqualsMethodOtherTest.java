package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class EqualsMethodOtherTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test(expected = NullPointerException.class)
    public void testNullableField() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.isolated().withNonNullableFields(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        type.getDeclaredConstructor().newInstance().equals(type.getDeclaredConstructor().newInstance());
    }

    @Test
    public void testEqualToSelf() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.isolated().withNonNullableFields(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance, is(instance));
    }

    @Test
    public void testEqualToSelfIdentity() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.isolated())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        loaded.getLoaded().getDeclaredField(FOO).set(instance, new NonEqualsBase());
        assertThat(instance, is(instance));
    }

    @Test
    public void testIgnoredField() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.isolated().withIgnoredFields(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object left = loaded.getLoaded().getDeclaredConstructor().newInstance(), right = loaded.getLoaded().getDeclaredConstructor().newInstance();
        left.getClass().getDeclaredField(FOO).set(left, FOO);
        left.getClass().getDeclaredField(FOO).set(left, BAR);
        assertThat(left, is(right));
    }

    @Test
    public void testSuperMethod() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(EqualsBase.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.requiringSuperClassEquality())
                .make()
                .load(EqualsBase.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object left = loaded.getLoaded().getDeclaredConstructor().newInstance(), right = loaded.getLoaded().getDeclaredConstructor().newInstance();
        left.getClass().getDeclaredField(FOO).set(left, FOO);
        right.getClass().getDeclaredField(FOO).set(right, FOO);
        assertThat(left, is(right));
    }

    @Test
    public void testSuperClass() throws Exception {
        DynamicType.Loaded<?> superClass = new ByteBuddy()
                .subclass(EqualsBase.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.isolated())
                .make()
                .load(EqualsBase.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        DynamicType.Loaded<?> subClass = new ByteBuddy()
                .subclass(superClass.getLoaded())
                .make()
                .load(superClass.getLoaded().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Object left = subClass.getLoaded().getDeclaredConstructor().newInstance(), right = subClass.getLoaded().getDeclaredConstructor().newInstance();
        superClass.getLoaded().getDeclaredField(FOO).set(left, FOO);
        superClass.getLoaded().getDeclaredField(FOO).set(right, FOO);
        assertThat(left, is(right));
    }

    @Test
    public void testSuperMethodNoMatch() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(NonEqualsBase.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.requiringSuperClassEquality())
                .make()
                .load(NonEqualsBase.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object left = loaded.getLoaded().getDeclaredConstructor().newInstance(), right = loaded.getLoaded().getDeclaredConstructor().newInstance();
        left.getClass().getDeclaredField(FOO).set(left, FOO);
        right.getClass().getDeclaredField(FOO).set(right, FOO);
        assertThat(left, not(right));
    }

    @Test
    public void testInstanceOf() throws Exception {
        DynamicType.Loaded<?> superClass = new ByteBuddy()
                .subclass(Object.class)
                .method(isEquals())
                .intercept(EqualsMethod.isolated().withSubclassEquality())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        DynamicType.Loaded<?> subClass = new ByteBuddy()
                .subclass(superClass.getLoaded())
                .make()
                .load(superClass.getLoaded().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(superClass.getLoaded().getDeclaredConstructor().newInstance(), is(subClass.getLoaded().getDeclaredConstructor().newInstance()));
        assertThat(subClass.getLoaded().getDeclaredConstructor().newInstance(), is(superClass.getLoaded().getDeclaredConstructor().newInstance()));
    }

    @Test
    public void testTypeOrderForPrimitiveTypedFields() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .defineField(BAR, int.class, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.isolated().withNonNullableFields(any()).withPrimitiveTypedFieldsFirst())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(2));
        Object left = loaded.getLoaded().getDeclaredConstructor().newInstance(), right = loaded.getLoaded().getDeclaredConstructor().newInstance();
        left.getClass().getDeclaredField(BAR).setInt(left, 42);
        right.getClass().getDeclaredField(BAR).setInt(right, 84);
        assertThat(left, not(right));
    }

    @Test
    public void testTypeOrderForEnumerationTypedFields() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .defineField(BAR, RetentionPolicy.class, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.isolated().withNonNullableFields(any()).withEnumerationTypedFieldsFirst())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(2));
        Object left = loaded.getLoaded().getDeclaredConstructor().newInstance(), right = loaded.getLoaded().getDeclaredConstructor().newInstance();
        left.getClass().getDeclaredField(BAR).set(left, RetentionPolicy.RUNTIME);
        right.getClass().getDeclaredField(BAR).set(right, RetentionPolicy.CLASS);
        assertThat(left, not(right));
    }

    @Test
    public void testTypeOrderForStringTypedFields() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .defineField(BAR, String.class, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.isolated().withNonNullableFields(any()).withStringTypedFieldsFirst())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(2));
        Object left = loaded.getLoaded().getDeclaredConstructor().newInstance(), right = loaded.getLoaded().getDeclaredConstructor().newInstance();
        left.getClass().getDeclaredField(BAR).set(left, FOO);
        right.getClass().getDeclaredField(BAR).set(right, BAR);
        assertThat(left, not(right));
    }

    @Test
    public void testTypeOrderForPrimitiveWrapperTypes() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .defineField(BAR, Integer.class, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.isolated().withNonNullableFields(any()).withPrimitiveWrapperTypedFieldsFirst())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(2));
        Object left = loaded.getLoaded().getDeclaredConstructor().newInstance(), right = loaded.getLoaded().getDeclaredConstructor().newInstance();
        left.getClass().getDeclaredField(BAR).set(left, 42);
        right.getClass().getDeclaredField(BAR).set(right, 84);
        assertThat(left, not(right));
    }

    @Test
    public void testNaturalOrderComparator() {
        Comparator<FieldDescription.InDefinedShape> comparator = EqualsMethod.NaturalOrderComparator.INSTANCE;
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        assertThat(comparator.compare(left, right), is(0));
    }

    @Test
    public void testPrimitiveTypeComparatorLeftPrimitive() {
        Comparator<FieldDescription.InDefinedShape> comparator = EqualsMethod.TypePropertyComparator.FOR_PRIMITIVE_TYPES;
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        TypeDescription.Generic leftType = mock(TypeDescription.Generic.class), rightType = mock(TypeDescription.Generic.class);
        when(left.getType()).thenReturn(leftType);
        when(right.getType()).thenReturn(rightType);
        when(leftType.isPrimitive()).thenReturn(true);
        assertThat(comparator.compare(left, right), is(-1));
    }

    @Test
    public void testPrimitiveTypeComparatorRightPrimitive() {
        Comparator<FieldDescription.InDefinedShape> comparator = EqualsMethod.TypePropertyComparator.FOR_PRIMITIVE_TYPES;
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        TypeDescription.Generic leftType = mock(TypeDescription.Generic.class), rightType = mock(TypeDescription.Generic.class);
        when(left.getType()).thenReturn(leftType);
        when(right.getType()).thenReturn(rightType);
        when(rightType.isPrimitive()).thenReturn(true);
        assertThat(comparator.compare(left, right), is(1));
    }

    @Test
    public void testPrimitiveTypeComparatorBothPrimitive() {
        Comparator<FieldDescription.InDefinedShape> comparator = EqualsMethod.TypePropertyComparator.FOR_PRIMITIVE_TYPES;
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        TypeDescription.Generic leftType = mock(TypeDescription.Generic.class), rightType = mock(TypeDescription.Generic.class);
        when(left.getType()).thenReturn(leftType);
        when(right.getType()).thenReturn(rightType);
        when(leftType.isPrimitive()).thenReturn(true);
        when(rightType.isPrimitive()).thenReturn(true);
        assertThat(comparator.compare(left, right), is(0));
    }

    @Test
    public void testEnumerationTypeComparatorLeftEnumeration() {
        Comparator<FieldDescription.InDefinedShape> comparator = EqualsMethod.TypePropertyComparator.FOR_ENUMERATION_TYPES;
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        TypeDescription.Generic leftType = mock(TypeDescription.Generic.class), rightType = mock(TypeDescription.Generic.class);
        when(left.getType()).thenReturn(leftType);
        when(right.getType()).thenReturn(rightType);
        when(leftType.isEnum()).thenReturn(true);
        assertThat(comparator.compare(left, right), is(-1));
    }

    @Test
    public void testEnumerationTypeComparatorRightEnumeration() {
        Comparator<FieldDescription.InDefinedShape> comparator = EqualsMethod.TypePropertyComparator.FOR_ENUMERATION_TYPES;
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        TypeDescription.Generic leftType = mock(TypeDescription.Generic.class), rightType = mock(TypeDescription.Generic.class);
        when(left.getType()).thenReturn(leftType);
        when(right.getType()).thenReturn(rightType);
        when(rightType.isEnum()).thenReturn(true);
        assertThat(comparator.compare(left, right), is(1));
    }

    @Test
    public void testStringTypeComparatorBothEnumeration() {
        Comparator<FieldDescription.InDefinedShape> comparator = EqualsMethod.TypePropertyComparator.FOR_ENUMERATION_TYPES;
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        TypeDescription.Generic leftType = mock(TypeDescription.Generic.class), rightType = mock(TypeDescription.Generic.class);
        when(left.getType()).thenReturn(leftType);
        when(right.getType()).thenReturn(rightType);
        when(leftType.isEnum()).thenReturn(true);
        when(rightType.isEnum()).thenReturn(true);
        assertThat(comparator.compare(left, right), is(0));
    }

    @Test
    public void testStringTypeComparatorLeftString() {
        Comparator<FieldDescription.InDefinedShape> comparator = EqualsMethod.TypePropertyComparator.FOR_STRING_TYPES;
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        TypeDescription.Generic leftType = mock(TypeDescription.Generic.class), rightType = mock(TypeDescription.Generic.class);
        when(left.getType()).thenReturn(leftType);
        when(right.getType()).thenReturn(rightType);
        when(leftType.represents(String.class)).thenReturn(true);
        assertThat(comparator.compare(left, right), is(-1));
    }

    @Test
    public void testStringTypeComparatorRightString() {
        Comparator<FieldDescription.InDefinedShape> comparator = EqualsMethod.TypePropertyComparator.FOR_STRING_TYPES;
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        TypeDescription.Generic leftType = mock(TypeDescription.Generic.class), rightType = mock(TypeDescription.Generic.class);
        when(left.getType()).thenReturn(leftType);
        when(right.getType()).thenReturn(rightType);
        when(rightType.represents(String.class)).thenReturn(true);
        assertThat(comparator.compare(left, right), is(1));
    }

    @Test
    public void testStringTypeComparatorBothString() {
        Comparator<FieldDescription.InDefinedShape> comparator = EqualsMethod.TypePropertyComparator.FOR_STRING_TYPES;
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        TypeDescription.Generic leftType = mock(TypeDescription.Generic.class), rightType = mock(TypeDescription.Generic.class);
        when(left.getType()).thenReturn(leftType);
        when(right.getType()).thenReturn(rightType);
        when(leftType.represents(String.class)).thenReturn(true);
        when(rightType.represents(String.class)).thenReturn(true);
        assertThat(comparator.compare(left, right), is(0));
    }

    @Test
    public void testPrimitiveWrapperTypeComparatorLeftPrimitiveWrapper() {
        Comparator<FieldDescription.InDefinedShape> comparator = EqualsMethod.TypePropertyComparator.FOR_PRIMITIVE_WRAPPER_TYPES;
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        TypeDescription.Generic leftType = mock(TypeDescription.Generic.class), rightType = mock(TypeDescription.Generic.class);
        when(left.getType()).thenReturn(leftType);
        when(right.getType()).thenReturn(rightType);
        TypeDescription leftErasure = mock(TypeDescription.class), rightErasure = mock(TypeDescription.class);
        when(leftType.asErasure()).thenReturn(leftErasure);
        when(rightType.asErasure()).thenReturn(rightErasure);
        when(leftErasure.isPrimitiveWrapper()).thenReturn(true);
        assertThat(comparator.compare(left, right), is(-1));
    }

    @Test
    public void testPrimitiveWrapperTypeComparatorRightPrimitiveWrapper() {
        Comparator<FieldDescription.InDefinedShape> comparator = EqualsMethod.TypePropertyComparator.FOR_PRIMITIVE_WRAPPER_TYPES;
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        TypeDescription.Generic leftType = mock(TypeDescription.Generic.class), rightType = mock(TypeDescription.Generic.class);
        when(left.getType()).thenReturn(leftType);
        when(right.getType()).thenReturn(rightType);
        TypeDescription leftErasure = mock(TypeDescription.class), rightErasure = mock(TypeDescription.class);
        when(leftType.asErasure()).thenReturn(leftErasure);
        when(rightType.asErasure()).thenReturn(rightErasure);
        when(rightErasure.isPrimitiveWrapper()).thenReturn(true);
        assertThat(comparator.compare(left, right), is(1));
    }

    @Test
    public void testPrimitiveWrapperTypeComparatorBothPrimitiveWrapper() {
        Comparator<FieldDescription.InDefinedShape> comparator = EqualsMethod.TypePropertyComparator.FOR_PRIMITIVE_WRAPPER_TYPES;
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        TypeDescription.Generic leftType = mock(TypeDescription.Generic.class), rightType = mock(TypeDescription.Generic.class);
        when(left.getType()).thenReturn(leftType);
        when(right.getType()).thenReturn(rightType);
        TypeDescription leftErasure = mock(TypeDescription.class), rightErasure = mock(TypeDescription.class);
        when(leftType.asErasure()).thenReturn(leftErasure);
        when(rightType.asErasure()).thenReturn(rightErasure);
        when(leftErasure.isPrimitiveWrapper()).thenReturn(true);
        when(rightErasure.isPrimitiveWrapper()).thenReturn(true);
        assertThat(comparator.compare(left, right), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCompoundComparatorNoComparator() {
        Comparator<FieldDescription.InDefinedShape> comparator = new EqualsMethod.CompoundComparator();
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        assertThat(comparator.compare(left, right), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCompoundComparatorSingleComparator() {
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        Comparator<FieldDescription.InDefinedShape> delegate = mock(Comparator.class);
        when(delegate.compare(left, right)).thenReturn(42);
        Comparator<FieldDescription.InDefinedShape> comparator = new EqualsMethod.CompoundComparator(delegate);
        assertThat(comparator.compare(left, right), is(42));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCompoundComparatorDoubleComparator() {
        FieldDescription.InDefinedShape left = mock(FieldDescription.InDefinedShape.class), right = mock(FieldDescription.InDefinedShape.class);
        Comparator<FieldDescription.InDefinedShape> delegate = mock(Comparator.class), first = mock(Comparator.class);
        when(delegate.compare(left, right)).thenReturn(42);
        when(first.compare(left, right)).thenReturn(0);
        Comparator<FieldDescription.InDefinedShape> comparator = new EqualsMethod.CompoundComparator(first, delegate);
        assertThat(comparator.compare(left, right), is(42));
        verify(first).compare(left, right);
    }

    @Test(expected = IllegalStateException.class)
    public void testInterface() throws Exception {
        new ByteBuddy()
                .makeInterface()
                .method(isEquals())
                .intercept(EqualsMethod.isolated())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testIncompatibleReturn() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, Object.class)
                .withParameters(Object.class)
                .intercept(EqualsMethod.isolated())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testIncompatibleArgumentLength() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, boolean.class)
                .withParameters(Object.class, Object.class)
                .intercept(EqualsMethod.isolated())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testIncompatibleArgumentType() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, boolean.class)
                .withParameters(int.class)
                .intercept(EqualsMethod.isolated())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testStaticMethod() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, boolean.class, Ownership.STATIC)
                .withParameters(Object.class)
                .intercept(EqualsMethod.isolated())
                .make();
    }

    public static class EqualsBase {

        @Override
        public boolean equals(Object other) {
            return true;
        }
    }

    public static class NonEqualsBase {

        @Override
        public boolean equals(Object other) {
            return false;
        }
    }
}
