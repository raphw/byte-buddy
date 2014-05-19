package net.bytebuddy.instrumentation.method;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.packaging.PackagePrivateConstructor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.reflect.Constructor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class MethodDescriptionForLoadedConstructorTest {

    private static final String TO_STRING = "toString";

    private MethodDescription objectDefaultConstructor;
    private MethodDescription stringDefaultConstructor;
    private MethodDescription stringSingleArgConstructor;
    private MethodDescription packagePrivateConstructor;

    private static int hashCode(Constructor<?> constructor) {
        return (Type.getInternalName(constructor.getDeclaringClass()) + "."
                + MethodDescription.CONSTRUCTOR_INTERNAL_NAME + Type.getConstructorDescriptor(constructor)).hashCode();
    }

    private static void assertConstructorEquality(MethodDescription methodDescription, Constructor<?> constructor) {
        MethodDescription otherMethod = mock(MethodDescription.class);
        TypeDescription otherType = mock(TypeDescription.class);
        when(otherMethod.getUniqueSignature()).thenReturn(MethodDescription.CONSTRUCTOR_INTERNAL_NAME + Type.getConstructorDescriptor(constructor));
        when(otherMethod.getDeclaringType()).thenReturn(otherType);
        when(otherType.getName()).thenReturn(constructor.getDeclaringClass().getName());
        assertThat(methodDescription.equals(otherMethod), is(true));
        verify(otherType).getName();
        verifyNoMoreInteractions(otherType);
    }

    @Before
    public void setUp() throws Exception {
        objectDefaultConstructor = new MethodDescription.ForLoadedConstructor(Object.class.getDeclaredConstructor());
        stringDefaultConstructor = new MethodDescription.ForLoadedConstructor(String.class.getDeclaredConstructor());
        stringSingleArgConstructor = new MethodDescription.ForLoadedConstructor(String.class.getDeclaredConstructor(String.class));
        packagePrivateConstructor = new MethodDescription.ForLoadedConstructor(PackagePrivateConstructor.class.getDeclaredConstructor());
    }

    @Test
    public void testRepresents() throws Exception {
        assertThat(objectDefaultConstructor.represents(Object.class.getDeclaredConstructor()), is(true));
        assertThat(objectDefaultConstructor.represents(String.class.getDeclaredConstructor()), is(false));
        assertThat(objectDefaultConstructor.represents(String.class.getDeclaredConstructor(String.class)), is(false));
        assertThat(objectDefaultConstructor.represents(Object.class.getMethod(TO_STRING)), is(false));
        assertThat(stringDefaultConstructor.represents(Object.class.getDeclaredConstructor()), is(false));
        assertThat(stringDefaultConstructor.represents(String.class.getDeclaredConstructor()), is(true));
        assertThat(stringDefaultConstructor.represents(String.class.getDeclaredConstructor(String.class)), is(false));
        assertThat(stringDefaultConstructor.represents(Object.class.getMethod(TO_STRING)), is(false));
        assertThat(stringSingleArgConstructor.represents(Object.class.getDeclaredConstructor()), is(false));
        assertThat(stringSingleArgConstructor.represents(String.class.getDeclaredConstructor()), is(false));
        assertThat(stringSingleArgConstructor.represents(String.class.getDeclaredConstructor(String.class)), is(true));
        assertThat(stringSingleArgConstructor.represents(Object.class.getMethod(TO_STRING)), is(false));
    }

    @Test
    public void testGetInternalName() throws Exception {
        assertThat(objectDefaultConstructor.getInternalName(), is(MethodDescription.CONSTRUCTOR_INTERNAL_NAME));
        assertThat(stringDefaultConstructor.getInternalName(), is(MethodDescription.CONSTRUCTOR_INTERNAL_NAME));
        assertThat(stringSingleArgConstructor.getInternalName(), is(MethodDescription.CONSTRUCTOR_INTERNAL_NAME));
    }

    @Test
    public void testGetDescriptor() throws Exception {
        assertThat(objectDefaultConstructor.getDescriptor(), is(Type.getConstructorDescriptor(Object.class.getDeclaredConstructor())));
        assertThat(stringDefaultConstructor.getDescriptor(), is(Type.getConstructorDescriptor(String.class.getDeclaredConstructor())));
        assertThat(stringSingleArgConstructor.getDescriptor(), is(Type.getConstructorDescriptor(String.class.getDeclaredConstructor(String.class))));
    }

    @Test
    public void testIsOverridable() throws Exception {
        assertThat(objectDefaultConstructor.isOverridable(), is(false));
        assertThat(stringDefaultConstructor.isOverridable(), is(false));
        assertThat(stringSingleArgConstructor.isOverridable(), is(false));
    }

    @Test
    public void testStackSize() throws Exception {
        assertThat(objectDefaultConstructor.getStackSize(), is(1));
        assertThat(stringDefaultConstructor.getStackSize(), is(1));
        assertThat(stringSingleArgConstructor.getStackSize(), is(2));
    }

    @Test
    public void testGetParameterOffset() throws Exception {
        assertThat(stringSingleArgConstructor.getParameterOffset(0), is(1));
    }

    @Test
    public void testIsVisibleTo() throws Exception {
        assertThat(objectDefaultConstructor.isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(true));
        assertThat(packagePrivateConstructor.isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(false));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(objectDefaultConstructor.hashCode(), is(hashCode(Object.class.getDeclaredConstructor())));
        assertThat(stringDefaultConstructor.hashCode(), is(hashCode(String.class.getDeclaredConstructor())));
        assertThat(stringSingleArgConstructor.hashCode(), is(hashCode(String.class.getDeclaredConstructor(String.class))));
    }

    @Test
    public void testEquals() throws Exception {
        assertConstructorEquality(objectDefaultConstructor, Object.class.getDeclaredConstructor());
        assertConstructorEquality(stringDefaultConstructor, String.class.getDeclaredConstructor());
        assertConstructorEquality(stringSingleArgConstructor, String.class.getDeclaredConstructor(String.class));
    }
}
