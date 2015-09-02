package net.bytebuddy.utility;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.asm.Opcodes;

import java.lang.annotation.Retention;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

import static net.bytebuddy.utility.ByteBuddyCommons.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ByteBuddyCommonsTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz", FOOBAR = "foo.bar", PUBLIC = "public";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription first, second;

    @Before
    public void setUp() throws Exception {
        when(first.getInternalName()).thenReturn(FOO);
        when(first.isAssignableTo(Throwable.class)).thenReturn(true);
        when(second.getInternalName()).thenReturn(BAR);
        when(second.isAssignableTo(Throwable.class)).thenReturn(false);
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
    public void testNonNullArray() throws Exception {
        Object[] object = new Object[]{new Object()};
        assertThat(nonNull(object), sameInstance(object));
    }

    @Test(expected = NullPointerException.class)
    public void testNonNullArrayThrowsException() throws Exception {
        nonNull(new Object[1]);
    }

    @Test
    public void testIsAnnotation() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(Retention.class);
        assertThat(isAnnotation(typeDescription), sameInstance(typeDescription));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsAnnotationThrowsException() throws Exception {
        isAnnotation(new TypeDescription.ForLoadedType(Object.class));
    }

    @Test
    public void testIsThrowable() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(Throwable.class);
        assertThat(isThrowable(typeDescription), sameInstance(typeDescription));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsThrowableThrowsExceptionForWildcard() throws Exception {
        GenericTypeDescription genericTypeDescription = mock(GenericTypeDescription.class);
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.WILDCARD);
        isThrowable(genericTypeDescription);
    }

    @Test
    public void testIsThrowableForExceptionVariable() throws Exception {
        GenericTypeDescription genericTypeDescription = mock(GenericTypeDescription.class);
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.VARIABLE);
        when(genericTypeDescription.asErasure()).thenReturn(new TypeDescription.ForLoadedType(Throwable.class));
        assertThat(isThrowable(genericTypeDescription), sameInstance(genericTypeDescription));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsThrowableThrowsExceptionForExceptionVariableOfNonThrowableType() throws Exception {
        GenericTypeDescription genericTypeDescription = mock(GenericTypeDescription.class);
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.VARIABLE);
        when(genericTypeDescription.asErasure()).thenReturn(new TypeDescription.ForLoadedType(Object.class));
        isThrowable(genericTypeDescription);
    }

    @Test
    public void testIsThrowableCollection() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(Throwable.class);
        List<TypeDescription> typeDescriptions = Collections.singletonList(typeDescription);
        assertThat(isThrowable(typeDescriptions), sameInstance(typeDescriptions));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsThrowableCollectionThrowsException() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(Object.class);
        List<TypeDescription> typeDescriptions = Collections.singletonList(typeDescription);
        isThrowable(typeDescriptions);
    }

    @Test
    public void testIsDefineable() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(Object.class);
        assertThat(isDefineable(typeDescription), sameInstance(typeDescription));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrimitiveTypeIsDefineableThrowsException() throws Exception {
        isDefineable(new TypeDescription.ForLoadedType(int.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testArrayTypeIsDefineableThrowsException() throws Exception {
        isDefineable(new TypeDescription.ForLoadedType(Object[].class));
    }

    @Test
    public void testIsExtendable() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(Object.class);
        assertThat(isExtendable(typeDescription), sameInstance(typeDescription));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrimitiveTypeIsExtendableThrowsException() throws Exception {
        isExtendable(new TypeDescription.ForLoadedType(int.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testArrayTypeIsExtendableThrowsException() throws Exception {
        isExtendable(new TypeDescription.ForLoadedType(Object[].class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFinalTypeIsExtendableThrowsException() throws Exception {
        isExtendable(new TypeDescription.ForLoadedType(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTypeVaribaleTypeIsExtendableThrowsException() throws Exception {
        GenericTypeDescription genericTypeDescription = mock(GenericTypeDescription.class);
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.VARIABLE);
        isExtendable(genericTypeDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWildcardTypeIsExtendableThrowsException() throws Exception {
        GenericTypeDescription genericTypeDescription = mock(GenericTypeDescription.class);
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.WILDCARD);
        isExtendable(genericTypeDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenericArrayTypeIsExtendableThrowsException() throws Exception {
        GenericTypeDescription genericTypeDescription = mock(GenericTypeDescription.class);
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.GENERIC_ARRAY);
        isExtendable(genericTypeDescription);
    }

    @Test
    public void testParameterizedTypeIsExtendable() throws Exception {
        GenericTypeDescription genericTypeDescription = mock(GenericTypeDescription.class);
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.PARAMETERIZED);
        when(genericTypeDescription.asErasure()).thenReturn(new TypeDescription.ForLoadedType(Object.class));
        assertThat(isExtendable(genericTypeDescription), sameInstance(genericTypeDescription));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParameterizedTypeWithIllegalErasureIsExtendableThrowsException() throws Exception {
        GenericTypeDescription genericTypeDescription = mock(GenericTypeDescription.class);
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.PARAMETERIZED);
        when(genericTypeDescription.asErasure()).thenReturn(new TypeDescription.ForLoadedType(String.class));
        isExtendable(genericTypeDescription);
    }

    @Test
    public void testIsImplementable() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(Runnable.class);
        assertThat(isImplementable(typeDescription), sameInstance(typeDescription));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testClassIsImplementableThrowsException() throws Exception {
        isImplementable(new TypeDescription.ForLoadedType(Object.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrimitiveTypeIsImplementableThrowsException() throws Exception {
        isImplementable(new TypeDescription.ForLoadedType(int.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testArrayTypeIsImplementableThrowsException() throws Exception {
        isImplementable(new TypeDescription.ForLoadedType(Object[].class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTypeVaribaleTypeIsImplementableThrowsException() throws Exception {
        GenericTypeDescription genericTypeDescription = mock(GenericTypeDescription.class);
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.VARIABLE);
        isImplementable(genericTypeDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWildcardTypeIsImplementableThrowsException() throws Exception {
        GenericTypeDescription genericTypeDescription = mock(GenericTypeDescription.class);
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.WILDCARD);
        isImplementable(genericTypeDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenericArrayTypeIsImplementableThrowsException() throws Exception {
        GenericTypeDescription genericTypeDescription = mock(GenericTypeDescription.class);
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.GENERIC_ARRAY);
        isImplementable(genericTypeDescription);
    }

    @Test
    public void testParameterizedTypeIsImplementable() throws Exception {
        GenericTypeDescription genericTypeDescription = mock(GenericTypeDescription.class);
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.PARAMETERIZED);
        when(genericTypeDescription.asErasure()).thenReturn(new TypeDescription.ForLoadedType(Runnable.class));
        assertThat(isImplementable(genericTypeDescription), sameInstance(genericTypeDescription));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParameterizedTypeWithIllegalErasureIsImplementableThrowsException() throws Exception {
        GenericTypeDescription genericTypeDescription = mock(GenericTypeDescription.class);
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.PARAMETERIZED);
        when(genericTypeDescription.asErasure()).thenReturn(new TypeDescription.ForLoadedType(Object.class));
        isImplementable(genericTypeDescription);
    }

    @Test
    public void testCollectionIsImplementable() throws Exception {
        Collection<TypeDescription> typeDescriptions = Collections.<TypeDescription>singleton(new TypeDescription.ForLoadedType(Runnable.class));
        assertThat(isImplementable(typeDescriptions), sameInstance(typeDescriptions));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCollectionIsImplementableThrowsException() throws Exception {
        isImplementable(Collections.<TypeDescription>singleton(new TypeDescription.ForLoadedType(Object.class)));
    }

    @Test
    public void testIsActualTypeOrVoidForRawType() throws Exception {
        GenericTypeDescription typeDescription = mock(GenericTypeDescription.class);
        when(typeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        assertThat(isActualTypeOrVoid(typeDescription), sameInstance(typeDescription));
    }

    @Test
    public void testIsActualTypeOrVoidForGenericArray() throws Exception {
        GenericTypeDescription typeDescription = mock(GenericTypeDescription.class);
        when(typeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.GENERIC_ARRAY);
        assertThat(isActualTypeOrVoid(typeDescription), sameInstance(typeDescription));
    }

    @Test
    public void testIsActualTypeOrVoidForTypeVariable() throws Exception {
        GenericTypeDescription typeDescription = mock(GenericTypeDescription.class);
        when(typeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.VARIABLE);
        assertThat(isActualTypeOrVoid(typeDescription), sameInstance(typeDescription));
    }

    @Test
    public void testIsActualTypeOrVoidForParameterizedType() throws Exception {
        GenericTypeDescription typeDescription = mock(GenericTypeDescription.class);
        when(typeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.PARAMETERIZED);
        assertThat(isActualTypeOrVoid(typeDescription), sameInstance(typeDescription));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsActualTypeOrVoidForWildcardThrowsException() throws Exception {
        GenericTypeDescription typeDescription = mock(GenericTypeDescription.class);
        when(typeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.WILDCARD);
        isActualTypeOrVoid(typeDescription);
    }

    @Test
    public void testCollectionIsActualTypeOrVoid() throws Exception {
        GenericTypeDescription typeDescription = mock(GenericTypeDescription.class);
        when(typeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        when(typeDescription.asErasure()).thenReturn(new TypeDescription.ForLoadedType(void.class));
        Collection<GenericTypeDescription> typeDescriptions = Collections.singleton(typeDescription);
        assertThat(isActualTypeOrVoid(typeDescriptions), sameInstance(typeDescriptions));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCollectionIsActualTypeOrVoidThrowsException() throws Exception {
        GenericTypeDescription typeDescription = mock(GenericTypeDescription.class);
        when(typeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.WILDCARD);
        isActualTypeOrVoid(Collections.singleton(typeDescription));
    }

    @Test
    public void testIsActualTypeForRawType() throws Exception {
        GenericTypeDescription typeDescription = mock(GenericTypeDescription.class);
        when(typeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        when(typeDescription.asErasure()).thenReturn(new TypeDescription.ForLoadedType(Object.class));
        assertThat(isActualType(typeDescription), sameInstance(typeDescription));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsActualTypeForRawVoidThrowsException() throws Exception {
        isActualType(new TypeDescription.ForLoadedType(void.class));
    }

    @Test
    public void testIsActualTypeForGenericArray() throws Exception {
        GenericTypeDescription typeDescription = mock(GenericTypeDescription.class);
        when(typeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.GENERIC_ARRAY);
        when(typeDescription.asErasure()).thenReturn(new TypeDescription.ForLoadedType(Object.class));
        assertThat(isActualType(typeDescription), sameInstance(typeDescription));
    }

    @Test
    public void testIsActualTypeForTypeVariable() throws Exception {
        GenericTypeDescription typeDescription = mock(GenericTypeDescription.class);
        when(typeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.VARIABLE);
        when(typeDescription.asErasure()).thenReturn(new TypeDescription.ForLoadedType(Object.class));
        assertThat(isActualType(typeDescription), sameInstance(typeDescription));
    }

    @Test
    public void testIsActualTypeForParameterizedType() throws Exception {
        GenericTypeDescription typeDescription = mock(GenericTypeDescription.class);
        when(typeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.PARAMETERIZED);
        when(typeDescription.asErasure()).thenReturn(new TypeDescription.ForLoadedType(Object.class));
        assertThat(isActualType(typeDescription), sameInstance(typeDescription));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsActualTypeForWildcardThrowsException() throws Exception {
        GenericTypeDescription typeDescription = mock(GenericTypeDescription.class);
        when(typeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.WILDCARD);
        when(typeDescription.asErasure()).thenReturn(new TypeDescription.ForLoadedType(Object.class));
        isActualType(typeDescription);
    }

    @Test
    public void testCollectionIsActualType() throws Exception {
        GenericTypeDescription typeDescription = mock(GenericTypeDescription.class);
        when(typeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        when(typeDescription.asErasure()).thenReturn(new TypeDescription.ForLoadedType(Object.class));
        Collection<GenericTypeDescription> typeDescriptions = Collections.singleton(typeDescription);
        assertThat(isActualType(typeDescriptions), sameInstance(typeDescriptions));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCollectionIsActualTypeThrowsException() throws Exception {
        isActualType(Collections.singleton(new TypeDescription.ForLoadedType(void.class)));
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
    public void testJoinListAndList() throws Exception {
        assertThat(join(Arrays.asList(FOO, BAR), Arrays.asList(QUX, BAZ)), is(Arrays.asList(FOO, BAR, QUX, BAZ)));
    }

    @Test
    public void testFilterUniqueNoDuplicates() throws Exception {
        assertThat(filterUnique(Arrays.asList(FOO, BAR), Arrays.asList(QUX, BAZ)), is(Arrays.asList(FOO, BAR, QUX, BAZ)));
    }

    @Test
    public void testFilterUniqueDuplicates() throws Exception {
        assertThat(filterUnique(Arrays.asList(FOO, BAR), Arrays.asList(FOO, BAZ)), is(Arrays.asList(FOO, BAR, BAZ)));
    }

    @Test
    public void testJoinUnique() throws Exception {
        assertThat(joinUnique(Arrays.asList(FOO, BAR), QUX), is(Arrays.asList(FOO, BAR, QUX)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testJoinUniqueDuplicate() throws Exception {
        joinUnique(Arrays.asList(FOO, BAR), FOO);
    }

    @Test
    public void testUniqueRaw() throws Exception {
        TypeDescription first = mock(TypeDescription.class), second = mock(TypeDescription.class);
        when(first.asErasure()).thenReturn(first);
        when(second.asErasure()).thenReturn(second);
        Collection<TypeDescription> typeDescriptions = Arrays.asList(first, second);
        assertThat(uniqueRaw(typeDescriptions), sameInstance(typeDescriptions));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUniqueRawThrowsException() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(typeDescription.asErasure()).thenReturn(typeDescription);
        Collection<TypeDescription> typeDescriptions = Arrays.asList(typeDescription, typeDescription);
        uniqueRaw(typeDescriptions);
    }

    @Test
    public void testJoinUniqueRaw() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(typeDescription.asErasure()).thenReturn(typeDescription);
        assertThat(joinUniqueRaw(Collections.singleton(typeDescription), Collections.singleton(typeDescription)),
                is(Collections.singletonList(typeDescription)));
    }

    @Test
    public void testJoinUniqueRawWithDuplicate() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(typeDescription.asErasure()).thenReturn(typeDescription);
        assertThat(joinUniqueRaw(Collections.singleton(typeDescription), Collections.singleton(typeDescription)),
                is(Collections.singletonList(typeDescription)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testJoinUniqueRawWithConflictingDuplicate() throws Exception {
        GenericTypeDescription first = mock(GenericTypeDescription.class), second = mock(GenericTypeDescription.class);
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(first.asErasure()).thenReturn(typeDescription);
        when(second.asErasure()).thenReturn(typeDescription);
        joinUniqueRaw(Collections.singletonList(first), Collections.singleton(second));
    }

    @Test
    public void testIsValidIdentifier() throws Exception {
        assertThat(isValidIdentifier(FOO), is(FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsValidIdentifierInvalidTokenStartThrowsException() throws Exception {
        isValidIdentifier(MethodDescription.CONSTRUCTOR_INTERNAL_NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsValidIdentifierInvalidTokenMiddleThrowsException() throws Exception {
        isValidIdentifier(FOO + ">");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsValidIdentifierAsKeywordThrowsException() throws Exception {
        isValidIdentifier(PUBLIC);
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
        List<String> list = Collections.singletonList(FOO);
        assertThat(isNotEmpty(list, FOO), sameInstance(list));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsNotEmptyThrowsException() throws Exception {
        isNotEmpty(Collections.emptyList(), FOO);
    }

    @Test
    public void testIsEmpty() throws Exception {
        List<String> list = Collections.emptyList();
        assertThat(isEmpty(list, FOO), sameInstance(list));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsEmptyThrowsException() throws Exception {
        isEmpty(Collections.singletonList(BAR), FOO);
    }

    @Test
    public void testResolveModifierContributors() throws Exception {
        assertThat(resolveModifierContributors(ByteBuddyCommons.FIELD_MODIFIER_MASK,
                FieldManifestation.FINAL,
                Ownership.STATIC,
                Visibility.PRIVATE), is(Opcodes.ACC_FINAL | Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE));
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
        assertThat(unique(Arrays.asList(first, second)), is(Arrays.asList(first, second)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUniqueForNonUniqueTypes() throws Exception {
        unique(Arrays.asList(first, second, first));
    }

    @Test
    public void testToListNonList() throws Exception {
        List<String> list = toList(new HashSet<String>(Arrays.asList(FOO, BAR)));
        assertThat(list.size(), is(2));
        assertThat(list.contains(FOO), is(true));
        assertThat(list.contains(BAR), is(true));
    }

    @Test
    public void testToListList() throws Exception {
        List<String> original = Arrays.asList(FOO, BAR);
        List<String> list = toList(original);
        assertThat(list, sameInstance(original));
    }

    @Test
    public void testToListIterable() throws Exception {
        List<String> list = toList(new ArrayIterable(FOO, BAR));
        assertThat(list.size(), is(2));
        assertThat(list.contains(FOO), is(true));
        assertThat(list.contains(BAR), is(true));
    }

    @Test
    public void testToListIterableCollection() throws Exception {
        List<String> original = Arrays.asList(FOO, BAR);
        List<String> list = toList((Iterable<String>) original);
        assertThat(list, sameInstance(original));
    }

    @Test
    public void testConstructorIsHidden() throws Exception {
        assertThat(ByteBuddyCommons.class.getDeclaredConstructors().length, is(1));
        Constructor<?> constructor = ByteBuddyCommons.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers()), is(true));
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            assertEquals(UnsupportedOperationException.class, exception.getCause().getClass());
        }
    }

    @Test
    public void testTypeIsFinal() throws Exception {
        assertThat(Modifier.isFinal(ByteBuddyCommons.class.getModifiers()), is(true));
    }

    private static class ArrayIterable implements Iterable<String> {

        private final String[] values;

        public ArrayIterable(String... values) {
            this.values = values;
        }

        @Override
        public Iterator<String> iterator() {
            return Arrays.asList(values).iterator();
        }
    }
}
