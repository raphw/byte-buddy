package net.bytebuddy.description.type;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.generic.AbstractGenericTypeDescriptionTest;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.asm.Type;
import org.objectweb.asm.*;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.util.*;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractTypeDescriptionTest extends AbstractGenericTypeDescriptionTest {

    private static final String FOO = "foo", BAR = "bar";

    @SuppressWarnings("unchecked")
    private static final List<Class<?>> TYPES = Arrays.asList(Object.class,
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

    private final Class<?> constructorType;

    protected AbstractTypeDescriptionTest() {
        class ConstructorType {

        }
        constructorType = ConstructorType.class;
    }

    protected abstract TypeDescription describe(Class<?> type);

    @Test
    public void testGenericTypeProperties() throws Exception {
        assertThat(describe(Object.class).getOwnerType(), nullValue(GenericTypeDescription.class));
        assertThat(describe(Object.class).getParameters().size(), is(0));
    }

    @Test(expected = IllegalStateException.class)
    public void testNoUpperBounds() throws Exception {
        describe(Object.class).getUpperBounds();
    }

    @Test(expected = IllegalStateException.class)
    public void testNoLowerBounds() throws Exception {
        describe(Object.class).getLowerBounds();
    }

    @Test(expected = IllegalStateException.class)
    public void testNoSymbol() throws Exception {
        describe(Object.class).getSymbol();
    }

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
    public void testSourceName() throws Exception {
        for (Class<?> type : TYPES) {
            if (type.isArray()) {
                assertThat(describe(type).getSourceCodeName(), is(type.getComponentType().getName() + "[]"));
            } else {
                assertThat(describe(type).getSourceCodeName(), is(type.getName()));
            }
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
        assertThat(describe(Object.class).getSourceCodeName(), is(Object.class.getName()));
        assertThat(describe(SampleClass.class).getSourceCodeName(), is(SampleClass.class.getName()));
        assertThat(describe(void.class).getSourceCodeName(), is(void.class.getName()));
        assertThat(describe(boolean.class).getSourceCodeName(), is(boolean.class.getName()));
        assertThat(describe(byte.class).getSourceCodeName(), is(byte.class.getName()));
        assertThat(describe(short.class).getSourceCodeName(), is(short.class.getName()));
        assertThat(describe(char.class).getSourceCodeName(), is(char.class.getName()));
        assertThat(describe(int.class).getSourceCodeName(), is(int.class.getName()));
        assertThat(describe(long.class).getSourceCodeName(), is(long.class.getName()));
        assertThat(describe(float.class).getSourceCodeName(), is(float.class.getName()));
        assertThat(describe(double.class).getSourceCodeName(), is(double.class.getName()));
        assertThat(describe(Object[].class).getSourceCodeName(), is(Object.class.getName() + "[]"));
        assertThat(describe(SampleClass[].class).getSourceCodeName(), is(SampleClass.class.getName() + "[]"));
        assertThat(describe(Object[][].class).getSourceCodeName(), is(Object.class.getName() + "[][]"));
        assertThat(describe(boolean[].class).getSourceCodeName(), is(boolean.class.getName() + "[]"));
        assertThat(describe(byte[].class).getSourceCodeName(), is(byte.class.getName() + "[]"));
        assertThat(describe(short[].class).getSourceCodeName(), is(short.class.getName() + "[]"));
        assertThat(describe(char[].class).getSourceCodeName(), is(char.class.getName() + "[]"));
        assertThat(describe(int[].class).getSourceCodeName(), is(int.class.getName() + "[]"));
        assertThat(describe(long[].class).getSourceCodeName(), is(long.class.getName() + "[]"));
        assertThat(describe(float[].class).getSourceCodeName(), is(float.class.getName() + "[]"));
        assertThat(describe(double[].class).getSourceCodeName(), is(double.class.getName() + "[]"));
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
        assertThat(describe(SampleClass.class).hashCode(), is(Type.getInternalName(SampleClass.class).hashCode()));
        assertThat(describe(SampleInterface.class).hashCode(), is(Type.getInternalName(SampleInterface.class).hashCode()));
        assertThat(describe(SampleAnnotation.class).hashCode(), is(Type.getInternalName(SampleAnnotation.class).hashCode()));
        assertThat(describe(SampleClass.class).hashCode(), is(describe(SampleClass.class).hashCode()));
        assertThat(describe(SampleClass.class).hashCode(), not(is(describe(SampleInterface.class).hashCode())));
        assertThat(describe(SampleClass.class).hashCode(), not(is(describe(SampleAnnotation.class).hashCode())));
        assertThat(describe(Object[].class).hashCode(), is(describe(Object[].class).hashCode()));
        assertThat(describe(Object[].class).hashCode(), not(is(describe(Object.class).hashCode())));
        assertThat(describe(void.class).hashCode(), is(Type.getInternalName(void.class).hashCode()));
    }

    @Test
    public void testEquals() throws Exception {
        TypeDescription identical = describe(SampleClass.class);
        assertThat(identical, equalTo(identical));
        TypeDescription equalFirst = mock(TypeDescription.class);
        when(equalFirst.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        when(equalFirst.asErasure()).thenReturn(equalFirst);
        when(equalFirst.getInternalName()).thenReturn(Type.getInternalName(SampleClass.class));
        assertThat(describe(SampleClass.class), equalTo(equalFirst));
        assertThat(describe(SampleClass.class), not(equalTo(describe(SampleInterface.class))));
        assertThat(describe(SampleClass.class), not(equalTo((TypeDescription) new TypeDescription.ForLoadedType(SampleInterface.class))));
        GenericTypeDescription nonRawType = mock(GenericTypeDescription.class);
        when(nonRawType.getSort()).thenReturn(GenericTypeDescription.Sort.VARIABLE);
        assertThat(describe(SampleClass.class), not(equalTo(nonRawType)));
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
    public void testPackage() throws Exception {
        assertThat(describe(SampleClass.class).getPackage(),
                is((PackageDescription) new PackageDescription.ForLoadedPackage(SampleClass.class.getPackage())));
        assertThat(describe(Object.class).getPackage(),
                is((PackageDescription) new PackageDescription.ForLoadedPackage(Object.class.getPackage())));
        assertThat(describe(Object[].class).getPackage(), nullValue(PackageDescription.class));
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
        assertThat(describe(Object.class).getSuperType(), nullValue(GenericTypeDescription.class));
        assertThat(describe(SampleInterface.class).getSuperType(), nullValue(GenericTypeDescription.class));
        assertThat(describe(SampleAnnotation.class).getSuperType(), nullValue(GenericTypeDescription.class));
        assertThat(describe(void.class).getSuperType(), nullValue(GenericTypeDescription.class));
        assertThat(describe(SampleClass.class).getSuperType(), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Object.class)));
        assertThat(describe(SampleIndirectInterfaceImplementation.class).getSuperType(),
                is((GenericTypeDescription) new TypeDescription.ForLoadedType(SampleInterfaceImplementation.class)));
        assertThat(describe(Object[].class).getSuperType(), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Object.class)));
    }

    @Test
    public void testInterfaces() throws Exception {
        assertThat(describe(Object.class).getInterfaces(), is((GenericTypeList) new GenericTypeList.Empty()));
        assertThat(describe(SampleInterface.class).getInterfaces(), is((GenericTypeList) new GenericTypeList.Empty()));
        assertThat(describe(SampleAnnotation.class).getInterfaces(), is((GenericTypeList) new GenericTypeList.ForLoadedType(Annotation.class)));
        assertThat(describe(SampleInterfaceImplementation.class).getInterfaces(),
                is((GenericTypeList) new GenericTypeList.ForLoadedType(SampleInterface.class)));
        assertThat(describe(SampleIndirectInterfaceImplementation.class).getInterfaces(), is((GenericTypeList) new GenericTypeList.Empty()));
        assertThat(describe(SampleTransitiveInterfaceImplementation.class).getInterfaces(),
                is((GenericTypeList) new GenericTypeList.ForLoadedType(SampleTransitiveInterface.class)));
        assertThat(describe(Object[].class).getInterfaces(), is((GenericTypeList) new GenericTypeList.ForLoadedType(Cloneable.class, Serializable.class)));
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
    public void testIsAssignableClassLoader() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(null,
                ClassFileExtraction.of(SampleClass.class),
                null,
                AccessController.getContext(),
                ByteArrayClassLoader.PersistenceHandler.MANIFEST,
                PackageDefinitionStrategy.NoOp.INSTANCE);
        Class<?> otherSampleClass = classLoader.loadClass(SampleClass.class.getName());
        assertThat(describe(SampleClass.class).isAssignableFrom(describe(otherSampleClass)), is(true));
        assertThat(describe(SampleClass.class).isAssignableTo(describe(otherSampleClass)), is(true));
        assertThat(describe(Object.class).isAssignableFrom(describe(otherSampleClass)), is(true));
        assertThat(describe(otherSampleClass).isAssignableTo(describe(Object.class)), is(true));
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
        assertThat(describe(int.class).isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(true));
        assertThat(describe(SampleInterface[].class).isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(true));
        assertThat(describe(SamplePackagePrivate[].class).isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(false));
    }

    @Test
    public void testAnnotations() throws Exception {
        assertAnnotations(SampleClass.class);
        assertAnnotations(SampleInterface.class);
        assertAnnotations(SampleClassInherited.class);
        assertAnnotations(SampleClassInheritedOverride.class);
    }

    private void assertAnnotations(Class<?> type) {
        assertThat(describe(type).getDeclaredAnnotations(),
                hasItems(new AnnotationList.ForLoadedAnnotation(type.getDeclaredAnnotations())
                        .toArray(new AnnotationDescription[type.getDeclaredAnnotations().length])));
        assertThat(describe(type).getDeclaredAnnotations().size(), is(type.getDeclaredAnnotations().length));
        assertThat(describe(type).getInheritedAnnotations(),
                hasItems(new AnnotationList.ForLoadedAnnotation(type.getAnnotations())
                        .toArray(new AnnotationDescription[type.getAnnotations().length])));
        assertThat(describe(type).getInheritedAnnotations().size(), is(type.getAnnotations().length));
    }

    @Test
    public void testDeclaredInMethod() throws Exception {
        Method method = AbstractTypeDescriptionTest.class.getDeclaredMethod("testDeclaredInMethod");
        Constructor<?> constructor = AbstractTypeDescriptionTest.class.getDeclaredConstructor();
        class MethodSample {

        }
        assertThat(describe(MethodSample.class).getEnclosingMethod().represents(method), is(true));
        assertThat(describe(constructorType).getEnclosingMethod().represents(constructor), is(true));
        assertThat(describe(SampleClass.class).getEnclosingMethod(), nullValue(MethodDescription.class));
        assertThat(describe(Object[].class).getEnclosingMethod(), nullValue(MethodDescription.class));
    }

    @Test
    public void testDeclaredInType() throws Exception {
        assertThat(describe(SampleClass.class).getEnclosingType().represents(AbstractTypeDescriptionTest.class), is(true));
        assertThat(describe(Object.class).getEnclosingType(), nullValue(TypeDescription.class));
        assertThat(describe(Object[].class).getEnclosingType(), nullValue(TypeDescription.class));
    }

    @Test
    public void testComponentType() throws Exception {
        assertThat(describe(Object.class).getComponentType(), nullValue(Object.class));
        assertThat(describe(Object[].class).getComponentType(), is(describe(Object.class)));
    }

    @Test
    public void testWrapperType() throws Exception {
        assertThat(describe(Object.class).isPrimitiveWrapper(), is(false));
        assertThat(describe(Boolean.class).isPrimitiveWrapper(), is(true));
        assertThat(describe(Byte.class).isPrimitiveWrapper(), is(true));
        assertThat(describe(Short.class).isPrimitiveWrapper(), is(true));
        assertThat(describe(Character.class).isPrimitiveWrapper(), is(true));
        assertThat(describe(Integer.class).isPrimitiveWrapper(), is(true));
        assertThat(describe(Long.class).isPrimitiveWrapper(), is(true));
        assertThat(describe(Float.class).isPrimitiveWrapper(), is(true));
        assertThat(describe(Double.class).isPrimitiveWrapper(), is(true));
        assertThat(describe(Void.class).isPrimitiveWrapper(), is(false));
    }

    @Test
    public void testConstantPool() throws Exception {
        assertThat(describe(Object.class).isConstantPool(), is(false));
        assertThat(describe(boolean.class).isConstantPool(), is(false));
        assertThat(describe(int.class).isConstantPool(), is(true));
        assertThat(describe(Integer.class).isConstantPool(), is(false));
        assertThat(describe(String.class).isConstantPool(), is(true));
        assertThat(describe(Class.class).isConstantPool(), is(true));
    }

    @Test
    public void testGenericType() throws Exception {
        assertThat(describe(SampleGenericType.class).getTypeVariables(), is(new TypeDescription.ForLoadedType(SampleGenericType.class).getTypeVariables()));
        assertThat(describe(SampleGenericType.class).getSuperType(), is(new TypeDescription.ForLoadedType(SampleGenericType.class).getSuperType()));
        assertThat(describe(SampleGenericType.class).getInterfaces(), is(new TypeDescription.ForLoadedType(SampleGenericType.class).getInterfaces()));
    }

    @Test
    public void testHierarchyIteration() throws Exception {
        Iterator<GenericTypeDescription> iterator = describe(Traversal.class).iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Traversal.class)));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is((GenericTypeDescription) TypeDescription.OBJECT));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test(expected = NoSuchElementException.class)
    public void testHierarchyEnds() throws Exception {
        Iterator<GenericTypeDescription> iterator = describe(Object.class).iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is((GenericTypeDescription) TypeDescription.OBJECT));
        assertThat(iterator.hasNext(), is(false));
        iterator.next();
    }

    @Test
    public void testMalformedTypeHasLegalErasure() throws Exception {
        TypeDescription malformed = new TypeDescription.ForLoadedType(Callable.class);
        TypeDescription typeDescription = describe(SignatureMalformer.malform(MalformedBase.class));
        assertThat(typeDescription.getSuperType().asErasure(), is(TypeDescription.OBJECT));
        assertThat(typeDescription.getInterfaces().asErasures().size(), is(1));
        assertThat(typeDescription.getInterfaces().asErasures().getOnly(), is(malformed));
        assertThat(typeDescription.getDeclaredFields().getOnly().getType().asErasure(), is(malformed));
        assertThat(typeDescription.getDeclaredMethods().filter(isMethod()).getOnly().getReturnType().asErasure(), is(malformed));
        assertThat(typeDescription.getGenericSignature(), nullValue(String.class));
        assertThat(typeDescription.getGenericSignature(), nullValue(String.class));
        assertThat(typeDescription.getDeclaredFields().getOnly().getGenericSignature(), nullValue(String.class));
        assertThat(typeDescription.getDeclaredMethods().filter(isMethod()).getOnly().getGenericSignature(), nullValue(String.class));
    }

    @Test(expected = GenericSignatureFormatError.class)
    public void testMalformedTypeSignature() throws Exception {
        TypeDescription typeDescription = describe(SignatureMalformer.malform(MalformedBase.class));
        assertThat(typeDescription.getInterfaces().size(), is(1));
        typeDescription.getInterfaces().getOnly().getSort();
    }

    @Test(expected = GenericSignatureFormatError.class)
    public void testMalformedFieldSignature() throws Exception {
        TypeDescription typeDescription = describe(SignatureMalformer.malform(MalformedBase.class));
        assertThat(typeDescription.getDeclaredFields().size(), is(1));
        typeDescription.getDeclaredFields().getOnly().getType().getSort();
    }

    @Test(expected = GenericSignatureFormatError.class)
    public void testMalformedMethodSignature() throws Exception {
        TypeDescription typeDescription = describe(SignatureMalformer.malform(MalformedBase.class));
        assertThat(typeDescription.getDeclaredMethods().filter(isMethod()).size(), is(1));
        typeDescription.getDeclaredMethods().filter(isMethod()).getOnly().getReturnType().getSort();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.AbstractBase.SuperTypeIterator.class).applyBasic();
    }

    protected interface SampleInterface {
        /* empty */
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface SampleAnnotation {
        /* empty */
    }

    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    private @interface OtherAnnotation {

        String value();
    }

    public interface SampleTransitiveInterface extends SampleInterface {
        /* empty */
    }

    private static class SignatureMalformer extends ClassVisitor {

        private static final String FOO = "foo";

        public SignatureMalformer(ClassVisitor classVisitor) {
            super(Opcodes.ASM5, classVisitor);
        }

        public static Class<?> malform(Class<?> type) throws Exception {
            ClassReader classReader = new ClassReader(type.getName());
            ClassWriter classWriter = new ClassWriter(classReader, 0);
            classReader.accept(new SignatureMalformer(classWriter), 0);
            ClassLoader classLoader = new ByteArrayClassLoader(null,
                    Collections.singletonMap(type.getName(), classWriter.toByteArray()),
                    null,
                    AccessController.getContext(),
                    ByteArrayClassLoader.PersistenceHandler.MANIFEST,
                    PackageDefinitionStrategy.NoOp.INSTANCE);
            return classLoader.loadClass(type.getName());
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, FOO, superName, interfaces);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            return super.visitField(access, name, desc, FOO, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            return super.visitMethod(access, name, desc, FOO, exceptions);
        }
    }

    @SuppressWarnings("unused")
    public abstract static class MalformedBase<T> implements Callable<T> {

        Callable<T> foo;

        abstract Callable<T> foo();
    }

    static class SamplePackagePrivate {
        /* empty */
    }

    public static class SampleInterfaceImplementation implements SampleInterface {
        /* empty */
    }

    public static class SampleIndirectInterfaceImplementation extends SampleInterfaceImplementation {
        /* empty */
    }

    public static class SampleTransitiveInterfaceImplementation implements SampleTransitiveInterface {
        /* empty */
    }

    public static class SampleGenericType<T extends ArrayList<T> & Callable<T>,
            S extends Callable<?>,
            U extends Callable<? extends Callable<U>>,
            V extends ArrayList<? super ArrayList<V>>,
            W extends Callable<W[]>> extends ArrayList<T> implements Callable<T> {

        @Override
        public T call() throws Exception {
            return null;
        }
    }

    public static class Traversal {
        /* empty */
    }

    @SampleAnnotation
    @OtherAnnotation(FOO)
    public class SampleClass {
        /* empty */
    }

    public class SampleClassInherited extends SampleClass {
        /* empty */
    }

    @OtherAnnotation(BAR)
    public class SampleClassInheritedOverride extends SampleClass {
        /* empty */
    }
}
