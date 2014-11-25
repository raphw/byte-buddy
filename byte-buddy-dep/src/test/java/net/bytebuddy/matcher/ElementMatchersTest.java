package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.ByteCodeElement;
import net.bytebuddy.instrumentation.ModifierReviewable;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class ElementMatchersTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testIsValue() throws Exception {
        Object value = new Object();
        assertThat(ElementMatchers.is(value).matches(value), is(true));
        assertThat(ElementMatchers.is(value).matches(new Object()), is(false));
        assertThat(ElementMatchers.is((Object) null).matches(null), is(true));
        assertThat(ElementMatchers.is((Object) null).matches(new Object()), is(false));
    }

    @Test
    public void testNot() throws Exception {
        Object value = new Object();
        @SuppressWarnings("unchecked")
        ElementMatcher<Object> elementMatcher = mock(ElementMatcher.class);
        when(elementMatcher.matches(value)).thenReturn(true);
        assertThat(ElementMatchers.not(elementMatcher).matches(value), is(false));
        verify(elementMatcher).matches(value);
        Object otherValue = new Object();
        assertThat(ElementMatchers.not(elementMatcher).matches(otherValue), is(true));
        verify(elementMatcher).matches(otherValue);
        verifyNoMoreInteractions(elementMatcher);
    }

    @Test
    public void testAny() throws Exception {
        assertThat(ElementMatchers.any().matches(new Object()), is(true));
    }

    @Test
    public void testNone() throws Exception {
        assertThat(ElementMatchers.none().matches(new Object()), is(false));
    }

    @Test
    public void testAnyOf() throws Exception {
        Object value = new Object(), otherValue = new Object();
        assertThat(ElementMatchers.anyOf(value, otherValue).matches(value), is(true));
        assertThat(ElementMatchers.anyOf(value, otherValue).matches(otherValue), is(true));
        assertThat(ElementMatchers.anyOf(value, otherValue).matches(new Object()), is(false));
    }

    @Test
    public void testNoneOf() throws Exception {
        Object value = new Object(), otherValue = new Object();
        assertThat(ElementMatchers.noneOf(value, otherValue).matches(value), is(false));
        assertThat(ElementMatchers.noneOf(value, otherValue).matches(otherValue), is(false));
        assertThat(ElementMatchers.noneOf(value, otherValue).matches(new Object()), is(true));
    }

    @Test
    public void testNamed() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getSourceCodeName()).thenReturn(FOO);
        assertThat(ElementMatchers.named(FOO).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.named(FOO.toUpperCase()).matches(byteCodeElement), is(false));
        assertThat(ElementMatchers.named(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNamedIgnoreCase() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getSourceCodeName()).thenReturn(FOO);
        assertThat(ElementMatchers.namedIgnoreCase(FOO).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.namedIgnoreCase(FOO.toUpperCase()).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.namedIgnoreCase(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameStartsWith() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getSourceCodeName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameStartsWith(FOO.substring(0, 2)).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameStartsWith(FOO.substring(0, 2).toUpperCase()).matches(byteCodeElement), is(false));
        assertThat(ElementMatchers.nameStartsWith(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameStartsWithIgnoreCase() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getSourceCodeName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameStartsWithIgnoreCase(FOO.substring(0, 2)).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameStartsWithIgnoreCase(FOO.substring(0, 2).toUpperCase()).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameStartsWithIgnoreCase(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameEndsWith() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getSourceCodeName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameEndsWith(FOO.substring(1)).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameEndsWith(FOO.substring(1).toUpperCase()).matches(byteCodeElement), is(false));
        assertThat(ElementMatchers.nameEndsWith(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameEndsWithIgnoreCase() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getSourceCodeName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameEndsWithIgnoreCase(FOO.substring(1)).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameEndsWithIgnoreCase(FOO.substring(1).toUpperCase()).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameEndsWithIgnoreCase(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameContains() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getSourceCodeName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameContains(FOO.substring(1, 2)).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameContains(FOO.substring(1, 2).toUpperCase()).matches(byteCodeElement), is(false));
        assertThat(ElementMatchers.nameContains(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameContainsIgnoreCase() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getSourceCodeName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameContainsIgnoreCase(FOO.substring(1, 2)).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameContainsIgnoreCase(FOO.substring(1, 2).toUpperCase()).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameContainsIgnoreCase(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testHasDescriptor() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getDescriptor()).thenReturn(FOO);
        assertThat(ElementMatchers.hasDescriptor(FOO).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.hasDescriptor(FOO.toUpperCase()).matches(byteCodeElement), is(false));
        assertThat(ElementMatchers.hasDescriptor(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testIsDeclaredBy() throws Exception {
        assertThat(ElementMatchers.isDeclaredBy(IsDeclaredBy.class).matches(new TypeDescription.ForLoadedType(IsDeclaredBy.Inner.class)), is(true));
        assertThat(ElementMatchers.isDeclaredBy(IsDeclaredBy.class).matches(mock(ByteCodeElement.class)), is(false));
        assertThat(ElementMatchers.isDeclaredBy(Object.class).matches(mock(ByteCodeElement.class)), is(false));
    }

    @Test
    public void testIsVisibleTo() throws Exception {
        assertThat(ElementMatchers.isVisibleTo(Object.class).matches(new TypeDescription.ForLoadedType(IsVisibleTo.class)), is(true));
        assertThat(ElementMatchers.isVisibleTo(Object.class).matches(new TypeDescription.ForLoadedType(IsNotVisibleTo.class)), is(false));
    }

    @Test
    public void testIsAnnotatedWith() throws Exception {
        assertThat(ElementMatchers.isAnnotatedWith(IsAnnotatedWithAnnotation.class).matches(new TypeDescription.ForLoadedType(IsAnnotatedWith.class)), is(true));
        assertThat(ElementMatchers.isAnnotatedWith(IsAnnotatedWithAnnotation.class).matches(new TypeDescription.ForLoadedType(Object.class)), is(false));
    }

    @Test
    public void testIsPublic() throws Exception {
        ModifierReviewable modifierReviewable = mock(ModifierReviewable.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_PUBLIC);
        assertThat(ElementMatchers.isPublic().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isPublic().matches(mock(ModifierReviewable.class)), is(false));
    }

    @Test
    public void testIsProtected() throws Exception {
        ModifierReviewable modifierReviewable = mock(ModifierReviewable.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_PROTECTED);
        assertThat(ElementMatchers.isProtected().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isProtected().matches(mock(ModifierReviewable.class)), is(false));
    }

    @Test
    public void testIsPackagePrivate() throws Exception {
        ModifierReviewable modifierReviewable = mock(ModifierReviewable.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED);
        assertThat(ElementMatchers.isPackagePrivate().matches(mock(ModifierReviewable.class)), is(true));
        assertThat(ElementMatchers.isPackagePrivate().matches(modifierReviewable), is(false));
    }

    @Test
    public void testIsPrivate() throws Exception {
        ModifierReviewable modifierReviewable = mock(ModifierReviewable.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_PRIVATE);
        assertThat(ElementMatchers.isPrivate().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isPrivate().matches(mock(ModifierReviewable.class)), is(false));
    }

    @Test
    public void testIsFinal() throws Exception {
        ModifierReviewable modifierReviewable = mock(ModifierReviewable.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_FINAL);
        assertThat(ElementMatchers.isFinal().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isFinal().matches(mock(ModifierReviewable.class)), is(false));
    }

    @Test
    public void testIsStatic() throws Exception {
        ModifierReviewable modifierReviewable = mock(ModifierReviewable.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_STATIC);
        assertThat(ElementMatchers.isStatic().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isStatic().matches(mock(ModifierReviewable.class)), is(false));
    }

    @Test
    public void testIsSynthetic() throws Exception {
        ModifierReviewable modifierReviewable = mock(ModifierReviewable.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_SYNTHETIC);
        assertThat(ElementMatchers.isSynthetic().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isSynthetic().matches(mock(ModifierReviewable.class)), is(false));
    }

    @Test
    public void testIsSynchronized() throws Exception {
        MethodDescription methodDescription = mock(MethodDescription.class);
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_SYNCHRONIZED);
        assertThat(ElementMatchers.isSynchronized().matches(methodDescription), is(true));
        assertThat(ElementMatchers.isSynchronized().matches(mock(MethodDescription.class)), is(false));
    }


    @Test
    public void testIsNative() throws Exception {
        MethodDescription methodDescription = mock(MethodDescription.class);
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_NATIVE);
        assertThat(ElementMatchers.isNative().matches(methodDescription), is(true));
        assertThat(ElementMatchers.isNative().matches(mock(MethodDescription.class)), is(false));
    }

    @Test
    public void testIsStrict() throws Exception {
        MethodDescription methodDescription = mock(MethodDescription.class);
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_STRICT);
        assertThat(ElementMatchers.isStrict().matches(methodDescription), is(true));
        assertThat(ElementMatchers.isStrict().matches(mock(MethodDescription.class)), is(false));
    }

    @Test
    public void testIsVarArgs() throws Exception {
        MethodDescription modifierReviewable = mock(MethodDescription.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_VARARGS);
        assertThat(ElementMatchers.isVarArgs().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isVarArgs().matches(mock(MethodDescription.class)), is(false));
    }

    @Test
    public void testIsBridge() throws Exception {
        MethodDescription modifierReviewable = mock(MethodDescription.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_BRIDGE);
        assertThat(ElementMatchers.isBridge().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isBridge().matches(mock(MethodDescription.class)), is(false));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConstructorIsHidden() throws Exception {
        assertThat(Modifier.isPrivate(ElementMatchers.class.getDeclaredConstructor().getModifiers()), is(true));
        Constructor<?> constructor = ElementMatchers.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException e) {
            throw (UnsupportedOperationException) e.getCause();
        }
    }

    private static class IsDeclaredBy {

        static class Inner {
            /* empty */
        }
    }

    public static class IsVisibleTo {
        /* empty */
    }

    private static class IsNotVisibleTo {
        /* empty */
    }

    @IsAnnotatedWithAnnotation
    private static class IsAnnotatedWith {

    }

    @Retention(RetentionPolicy.RUNTIME)
    private static @interface IsAnnotatedWithAnnotation {

    }
}
