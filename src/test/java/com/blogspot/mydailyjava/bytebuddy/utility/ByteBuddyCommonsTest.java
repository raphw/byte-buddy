package com.blogspot.mydailyjava.bytebuddy.utility;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.modifier.FieldManifestation;
import com.blogspot.mydailyjava.bytebuddy.modifier.MemberVisibility;
import com.blogspot.mydailyjava.bytebuddy.modifier.Ownership;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.asm.Opcodes;

import java.util.Arrays;
import java.util.List;

import static com.blogspot.mydailyjava.bytebuddy.utility.ByteBuddyCommons.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.Mockito.when;

public class ByteBuddyCommonsTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", FOOBAR = "foo.bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription first, second;

    @Before
    public void setUp() throws Exception {
        when(first.getInternalName()).thenReturn(FOO);
        when(second.getInternalName()).thenReturn(BAR);
    }

    @Test
    public void testNonNull() throws Exception {
        Object object = new Object();
        assertThat(nonNull(object), sameInstance(object));
    }

    @Test(expected = NullPointerException.class)
    public void testNonNullThrowsException() throws Exception {
        nonNull(null);
    }

    @Test
    public void testIsInterface() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(Runnable.class);
        assertThat(isInterface(typeDescription), is(typeDescription));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsInterfaceThrowsException() throws Exception {
        isInterface(new TypeDescription.ForLoadedType(Object.class));
    }

    @Test
    public void testClassIsImplementable() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(Object.class);
        assertThat(isImplementable(typeDescription), is(typeDescription));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFinalClassIsImplementableThrowsException() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(String.class);
        isImplementable(typeDescription);
    }

    @Test
    public void testInterfaceIsImplementable() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(Runnable.class);
        assertThat(isImplementable(typeDescription), is(typeDescription));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrimitiveIsImplementableThrowsException() throws Exception {
        isImplementable(new TypeDescription.ForLoadedType(int.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testArrayIsImplementableThrowsException() throws Exception {
        isImplementable(new TypeDescription.ForLoadedType(Object[].class));
    }

    @Test
    public void testJoinListAndElement() throws Exception {
        assertThat(join(Arrays.asList(FOO, BAR), QUX), is(Arrays.asList(FOO, BAR, QUX)));
    }

    @Test
    public void testJoinElementAndList() throws Exception {
        assertThat(join(FOO, Arrays.asList(BAR, QUX)), is(Arrays.asList(FOO, BAR, QUX)));
    }

    @Test
    public void testIsValidIdentifier() throws Exception {
        assertThat(isValidIdentifier(FOO), is(FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsValidIdentifierThrowsException() throws Exception {
        isValidIdentifier(MethodDescription.CONSTRUCTOR_INTERNAL_NAME);
    }

    @Test
    public void testIsValidTypeName() throws Exception {
        assertThat(isValidTypeName(FOOBAR), is(FOOBAR));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsValidTypeNameThrowsException() throws Exception {
        assertThat(isValidTypeName("." + FOO), is(FOOBAR));
    }

    @Test
    public void testIsNotEmpty() throws Exception {
        List<String> list = Arrays.asList(FOO);
        assertThat(isNotEmpty(list, FOO), sameInstance(list));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsNotEmptyThrowsException() throws Exception {
        isNotEmpty(Arrays.<String>asList(), FOO);
    }

    @Test
    public void testResolveModifierContributors() throws Exception {
        assertThat(resolveModifierContributors(ByteBuddyCommons.FIELD_MODIFIER_MASK,
                FieldManifestation.FINAL,
                Ownership.STATIC,
                MemberVisibility.PRIVATE), is(Opcodes.ACC_FINAL | Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolveModifierContributorsDuplication() throws Exception {
        resolveModifierContributors(Integer.MAX_VALUE, Ownership.STATIC, Ownership.MEMBER);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolveModifierContributorsMask() throws Exception {
        resolveModifierContributors(ModifierContributor.EMPTY_MASK, Ownership.STATIC);
    }

    @Test
    public void testUniqueForUniqueTypes() throws Exception {
        assertThat(uniqueTypes(Arrays.asList(first, second)), is(Arrays.asList(first, second)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUniqueForNonUniqueTypes() throws Exception {
        uniqueTypes(Arrays.asList(first, second, first));
    }
}
