package net.bytebuddy.instrumentation.type;

import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import org.junit.Test;
import org.mockito.asm.Type;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractTypeDescriptionTest {

    private static final List<Class<?>> TYPES = Arrays.<Class<?>>asList(Object.class,
            SampleClass.class,
            void.class,
            byte.class,
            short.class,
            char.class,
            int.class,
            long.class,
            float.class,
            double.class,
            Object[].class);

    protected abstract TypeDescription describe(Class<?> type);

    @Test
    public void testPrecondition() throws Exception {
        assertThat(describe(SampleClass.class), not(equalTo(describe(SampleInterface.class))));
        assertThat(describe(SampleClass.class), not(equalTo(describe(SampleAnnotation.class))));
        assertThat(describe(SampleClass.class), equalTo(describe(SampleClass.class)));
        assertThat(describe(SampleInterface.class), equalTo(describe(SampleInterface.class)));
        assertThat(describe(SampleAnnotation.class), equalTo(describe(SampleAnnotation.class)));
        assertThat(describe(SampleClass.class), is((TypeDescription) new TypeDescription.ForLoadedType(SampleClass.class)));
        assertThat(describe(SampleInterface.class), is((TypeDescription) new TypeDescription.ForLoadedType(SampleInterface.class)));
        assertThat(describe(SampleAnnotation.class), is((TypeDescription) new TypeDescription.ForLoadedType(SampleAnnotation.class)));
    }

    @Test
    public void testStackSize() throws Exception {
        assertThat(describe(void.class).getStackSize(), is(StackSize.ZERO));
        assertThat(describe(boolean.class).getStackSize(), is(StackSize.SINGLE));
        assertThat(describe(byte.class).getStackSize(), is(StackSize.SINGLE));
        assertThat(describe(short.class).getStackSize(), is(StackSize.SINGLE));
        assertThat(describe(char.class).getStackSize(), is(StackSize.SINGLE));
        assertThat(describe(int.class).getStackSize(), is(StackSize.SINGLE));
        assertThat(describe(long.class).getStackSize(), is(StackSize.DOUBLE));
        assertThat(describe(float.class).getStackSize(), is(StackSize.SINGLE));
        assertThat(describe(double.class).getStackSize(), is(StackSize.DOUBLE));
        assertThat(describe(Object.class).getStackSize(), is(StackSize.SINGLE));
        assertThat(describe(SampleClass.class).getStackSize(), is(StackSize.SINGLE));
        assertThat(describe(Object[].class).getStackSize(), is(StackSize.SINGLE));
        assertThat(describe(long[].class).getStackSize(), is(StackSize.SINGLE));
    }

    @Test
    public void testName() throws Exception {
        for (Class<?> type : TYPES) {
            assertThat(describe(type).getName(), is(type.getName()));
        }
    }

    @Test
    public void testInternalName() throws Exception {
        for (Class<?> type : TYPES) {
            assertThat(describe(type).getInternalName(), is(Type.getInternalName(type)));
        }
    }

    @Test
    public void testCanonicalName() throws Exception {
        for (Class<?> type : TYPES) {
            assertThat(describe(type).getCanonicalName(), is(type.getCanonicalName()));
        }
    }

    @Test
    public void testSimpleName() throws Exception {
        for (Class<?> type : TYPES) {
            assertThat(describe(type).getSimpleName(), is(type.getSimpleName()));
        }
    }

    @Test
    public void testJavaName() throws Exception {
        assertThat(describe(Object.class).getJavaName(), is(Object.class.getName()));
        assertThat(describe(SampleClass.class).getJavaName(), is(SampleClass.class.getName()));
        assertThat(describe(void.class).getJavaName(), is(void.class.getName()));
        assertThat(describe(boolean.class).getJavaName(), is(boolean.class.getName()));
        assertThat(describe(byte.class).getJavaName(), is(byte.class.getName()));
        assertThat(describe(short.class).getJavaName(), is(short.class.getName()));
        assertThat(describe(char.class).getJavaName(), is(char.class.getName()));
        assertThat(describe(int.class).getJavaName(), is(int.class.getName()));
        assertThat(describe(long.class).getJavaName(), is(long.class.getName()));
        assertThat(describe(float.class).getJavaName(), is(float.class.getName()));
        assertThat(describe(double.class).getJavaName(), is(double.class.getName()));
        assertThat(describe(Object[].class).getJavaName(), is(Object.class.getName() + "[]"));
        assertThat(describe(SampleClass[].class).getJavaName(), is(SampleClass.class.getName() + "[]"));
        assertThat(describe(Object[][].class).getJavaName(), is(Object.class.getName() + "[][]"));
        assertThat(describe(boolean[].class).getJavaName(), is(boolean.class.getName() + "[]"));
        assertThat(describe(byte[].class).getJavaName(), is(byte.class.getName() + "[]"));
        assertThat(describe(short[].class).getJavaName(), is(short.class.getName() + "[]"));
        assertThat(describe(char[].class).getJavaName(), is(char.class.getName() + "[]"));
        assertThat(describe(int[].class).getJavaName(), is(int.class.getName() + "[]"));
        assertThat(describe(long[].class).getJavaName(), is(long.class.getName() + "[]"));
        assertThat(describe(float[].class).getJavaName(), is(float.class.getName() + "[]"));
        assertThat(describe(double[].class).getJavaName(), is(double.class.getName() + "[]"));
    }

    @Test
    public void testDescriptor() throws Exception {
        for (Class<?> type : TYPES) {
            assertThat(describe(type).getDescriptor(), is(Type.getDescriptor(type)));
        }
    }

    @Test
    public void testModifier() throws Exception {
        assertThat(describe(SampleClass.class).getModifiers(), is(SampleClass.class.getModifiers()));
        assertThat(describe(SampleInterface.class).getModifiers(), is(SampleInterface.class.getModifiers()));
        assertThat(describe(SampleAnnotation.class).getModifiers(), is(SampleAnnotation.class.getModifiers()));
        assertThat(describe(Object[].class).getModifiers(), is(Object[].class.getModifiers()));
    }

    @Test
    public void testDeclaringType() throws Exception {
        assertThat(describe(SampleClass.class).getDeclaringType(),
                is((TypeDescription) new TypeDescription.ForLoadedType(AbstractTypeDescriptionTest.class)));
        assertThat(describe(Object.class).getDeclaringType(), nullValue(TypeDescription.class));
        assertThat(describe(Object[].class).getDeclaringType(), nullValue(TypeDescription.class));
        assertThat(describe(void.class).getDeclaringType(), nullValue(TypeDescription.class));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(describe(SampleClass.class).hashCode(), is(SampleClass.class.getName().hashCode()));
        assertThat(describe(SampleInterface.class).hashCode(), is(SampleInterface.class.getName().hashCode()));
        assertThat(describe(SampleAnnotation.class).hashCode(), is(SampleAnnotation.class.getName().hashCode()));
        assertThat(describe(SampleClass.class).hashCode(), is(describe(SampleClass.class).hashCode()));
        assertThat(describe(SampleClass.class).hashCode(), not(is(describe(SampleInterface.class).hashCode())));
        assertThat(describe(SampleClass.class).hashCode(), not(is(describe(SampleAnnotation.class).hashCode())));
        assertThat(describe(Object[].class).hashCode(), is(describe(Object[].class).hashCode()));
        assertThat(describe(Object[].class).hashCode(), not(is(describe(Object.class).hashCode())));
        assertThat(describe(void.class).hashCode(), is(void.class.getName().hashCode()));
    }

    @Test
    public void testEquals() throws Exception {
        TypeDescription identical = describe(SampleClass.class);
        assertThat(identical, equalTo(identical));
        TypeDescription equalFirst = mock(TypeDescription.class);
        when(equalFirst.getName()).thenReturn(SampleClass.class.getName());
        assertThat(describe(SampleClass.class), equalTo(equalFirst));
        assertThat(describe(SampleClass.class), not(equalTo(describe(SampleInterface.class))));
        assertThat(describe(SampleClass.class), not(equalTo((TypeDescription) new TypeDescription.ForLoadedType(SampleInterface.class))));
        assertThat(describe(SampleClass.class), not(equalTo(new Object())));
        assertThat(describe(SampleClass.class), not(equalTo(null)));
        assertThat(describe(Object[].class), equalTo((TypeDescription) new TypeDescription.ForLoadedType(Object[].class)));
        assertThat(describe(Object[].class), not(equalTo((TypeDescription) new TypeDescription.ForLoadedType(Object.class))));
    }

    @Test
    public void testIsInstance() throws Exception {
        assertThat(describe(SampleClass.class).isInstance(new SampleClass()), is(true));
        assertThat(describe(SampleClass.class).isInstance(new Object()), is(false));
        assertThat(describe(SampleInterface.class).isInstance(new SampleInterfaceImplementation()), is(true));
        assertThat(describe(Object[].class).isInstance(new Object[0]), is(true));
        assertThat(describe(Object[].class).isInstance(new Object()), is(false));
    }

    @Test
    public void testPackageName() throws Exception {
        assertThat(describe(SampleClass.class).getPackageName(), is(SampleClass.class.getPackage().getName()));
        assertThat(describe(Object.class).getPackageName(), is(Object.class.getPackage().getName()));
        assertThat(describe(Object[].class).getPackageName(), nullValue(String.class));
    }

    @Test
    public void testActualModifiers() throws Exception {
        assertThat(describe(SampleClass.class).getActualModifiers(true), is(SampleClass.class.getModifiers() | Opcodes.ACC_SUPER));
        assertThat(describe(SampleClass.class).getActualModifiers(false), is(SampleClass.class.getModifiers()));
        assertThat(describe(SampleInterface.class).getActualModifiers(true),
                is((SampleInterface.class.getModifiers() & ~(Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC) | Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER)));
        assertThat(describe(SampleInterface.class).getActualModifiers(false),
                is((SampleInterface.class.getModifiers() & ~(Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC) | Opcodes.ACC_PUBLIC)));
        assertThat(describe(SampleAnnotation.class).getActualModifiers(true),
                is((SampleAnnotation.class.getModifiers() & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) | Opcodes.ACC_SUPER));
        assertThat(describe(SampleAnnotation.class).getActualModifiers(false),
                is((SampleAnnotation.class.getModifiers() & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC))));
        assertThat(describe(SamplePackagePrivate.class).getActualModifiers(true),
                is((SamplePackagePrivate.class.getModifiers() & ~(Opcodes.ACC_STATIC)) | Opcodes.ACC_SUPER));
        assertThat(describe(SamplePackagePrivate.class).getActualModifiers(false),
                is((SamplePackagePrivate.class.getModifiers() & ~(Opcodes.ACC_STATIC))));
    }

    @Test
    public void testSuperType() throws Exception {
        assertThat(describe(Object.class).getSupertype(), nullValue(TypeDescription.class));
        assertThat(describe(SampleInterface.class).getSupertype(), nullValue(TypeDescription.class));
        assertThat(describe(SampleAnnotation.class).getSupertype(), nullValue(TypeDescription.class));
        assertThat(describe(void.class).getSupertype(), nullValue(TypeDescription.class));
        assertThat(describe(SampleClass.class).getSupertype(),
                is((TypeDescription) new TypeDescription.ForLoadedType(Object.class)));
        assertThat(describe(SampleIndirectInterfaceImplementation.class).getSupertype(),
                is((TypeDescription) new TypeDescription.ForLoadedType(SampleInterfaceImplementation.class)));
        assertThat(describe(Object[].class).getSupertype(),
                is((TypeDescription) new TypeDescription.ForLoadedType(Object.class)));
    }

    @Test
    public void testInterfaces() throws Exception {
        assertThat(describe(Object.class).getInterfaces(), is((TypeList) new TypeList.Empty()));
        assertThat(describe(SampleInterface.class).getInterfaces(), is((TypeList) new TypeList.Empty()));
        assertThat(describe(SampleAnnotation.class).getInterfaces(), is((TypeList) new TypeList.ForLoadedType(Annotation.class)));
        assertThat(describe(SampleInterfaceImplementation.class).getInterfaces(), is((TypeList) new TypeList.ForLoadedType(SampleInterface.class)));
        assertThat(describe(SampleIndirectInterfaceImplementation.class).getInterfaces(), is((TypeList) new TypeList.Empty()));
        assertThat(describe(SampleTransitiveInterfaceImplementation.class).getInterfaces(), is((TypeList) new TypeList.ForLoadedType(SampleTransitiveInterface.class)));
    }

    @Test
    public void testInterfacesInherited() throws Exception {
        assertThat(describe(Object.class).getInheritedInterfaces(), is((TypeList) new TypeList.Empty()));
        assertThat(describe(SampleInterface.class).getInheritedInterfaces(), is((TypeList) new TypeList.Empty()));
        assertThat(describe(SampleAnnotation.class).getInheritedInterfaces(), is((TypeList) new TypeList.ForLoadedType(Annotation.class)));
        assertThat(describe(SampleInterfaceImplementation.class).getInheritedInterfaces(), is((TypeList) new TypeList.ForLoadedType(SampleInterface.class)));
        assertThat(describe(SampleTransitiveInterfaceImplementation.class).getInheritedInterfaces().size(), is(2));
        assertThat(describe(SampleTransitiveInterfaceImplementation.class).getInheritedInterfaces(), hasItems((TypeDescription) new TypeDescription.ForLoadedType(SampleInterface.class)));
        assertThat(describe(SampleTransitiveInterfaceImplementation.class).getInheritedInterfaces(), hasItems((TypeDescription) new TypeDescription.ForLoadedType(SampleTransitiveInterface.class)));
    }

    @Test
    public void testToString() throws Exception {
        for (Class<?> type : TYPES) {
            assertThat(describe(type).toString(), is(type.toString()));
        }
    }

    @Test
    public void testIsAssignable() throws Exception {
        assertThat(describe(Object.class).isAssignableTo(Object.class), is(true));
        assertThat(describe(Object.class).isAssignableFrom(Object.class), is(true));
        assertThat(describe(Object[].class).isAssignableTo(Object.class), is(true));
        assertThat(describe(Object.class).isAssignableFrom(Object[].class), is(true));
        assertThat(describe(String[].class).isAssignableTo(Object[].class), is(true));
        assertThat(describe(Object[].class).isAssignableFrom(String[].class), is(true));
        assertThat(describe(int.class).isAssignableTo(int.class), is(true));
        assertThat(describe(int.class).isAssignableFrom(int.class), is(true));
        assertThat(describe(void.class).isAssignableTo(void.class), is(true));
        assertThat(describe(void.class).isAssignableFrom(void.class), is(true));
        assertThat(describe(SampleInterfaceImplementation.class).isAssignableTo(SampleInterface.class), is(true));
        assertThat(describe(SampleInterface.class).isAssignableFrom(SampleInterfaceImplementation.class), is(true));
        assertThat(describe(SampleTransitiveInterfaceImplementation.class).isAssignableTo(SampleInterface.class), is(true));
        assertThat(describe(SampleInterface.class).isAssignableFrom(SampleTransitiveInterfaceImplementation.class), is(true));
        assertThat(describe(SampleInterface.class).isAssignableTo(Object.class), is(true));
        assertThat(describe(Object.class).isAssignableFrom(SampleInterface.class), is(true));
        assertThat(describe(SampleInterfaceImplementation.class).isAssignableTo(SampleClass.class), is(false));
        assertThat(describe(SampleClass.class).isAssignableFrom(SampleInterfaceImplementation.class), is(false));
        assertThat(describe(SampleInterfaceImplementation.class).isAssignableTo(boolean.class), is(false));
        assertThat(describe(boolean.class).isAssignableFrom(SampleInterfaceImplementation.class), is(false));
        assertThat(describe(boolean.class).isAssignableTo(Object.class), is(false));
        assertThat(describe(Object.class).isAssignableFrom(boolean.class), is(false));
        assertThat(describe(boolean[].class).isAssignableTo(Object.class), is(true));
        assertThat(describe(Object.class).isAssignableFrom(boolean[].class), is(true));
        assertThat(describe(boolean[].class).isAssignableTo(Object[].class), is(false));
        assertThat(describe(Object[].class).isAssignableFrom(boolean[].class), is(false));
    }

    @Test
    public void testIsVisible() throws Exception {
        assertThat(describe(SampleClass.class).isVisibleTo(new TypeDescription.ForLoadedType(SampleInterface.class)), is(true));
        assertThat(describe(SamplePackagePrivate.class).isVisibleTo(new TypeDescription.ForLoadedType(SampleClass.class)), is(true));
        assertThat(describe(SampleInterface.class).isVisibleTo(new TypeDescription.ForLoadedType(SampleClass.class)), is(true));
        assertThat(describe(SampleAnnotation.class).isVisibleTo(new TypeDescription.ForLoadedType(SampleClass.class)), is(true));
        assertThat(describe(SamplePackagePrivate.class).isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(describe(SampleInterface.class).isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(true));
        assertThat(describe(SampleAnnotation.class).isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(false));
    }

    public class SampleClass {
    }

    protected static interface SampleInterface {
    }

    private static @interface SampleAnnotation {
    }

    static class SamplePackagePrivate {
    }

    public static class SampleInterfaceImplementation implements SampleInterface {
    }

    public static class SampleIndirectInterfaceImplementation extends SampleInterfaceImplementation {
    }

    public static interface SampleTransitiveInterface extends SampleInterface {
    }

    public static class SampleTransitiveInterfaceImplementation implements SampleTransitiveInterface {
    }
}
