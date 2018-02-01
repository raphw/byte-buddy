package net.bytebuddy.description.type;

import net.bytebuddy.description.TypeVariableSource;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.packaging.SimpleType;
import net.bytebuddy.test.scope.EnclosingType;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.visibility.Sample;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.objectweb.asm.*;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.lang.reflect.GenericSignatureFormatError;
import java.util.*;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractTypeDescriptionTest extends AbstractTypeDescriptionGenericVariableDefiningTest {

    private static final String FOO = "foo", BAR = "bar";

    private final List<Class<?>> standardTypes;

    @SuppressWarnings({"unchecked", "deprecation"})
    protected AbstractTypeDescriptionTest() {
        standardTypes = Arrays.asList(
                Object.class,
                Object[].class,
                SampleClass.class,
                SampleClass[].class,
                SampleInterface.class,
                SampleInterface[].class,
                SampleAnnotation.class,
                SampleAnnotation[].class,
                void.class,
                byte.class,
                byte[].class,
                short.class,
                short[].class,
                char.class,
                char[].class,
                int.class,
                int[].class,
                long.class,
                long[].class,
                float.class,
                float[].class,
                double.class,
                double[].class,
                new EnclosingType().localMethod,
                Array.newInstance(new EnclosingType().localConstructor, 1).getClass(),
                new EnclosingType().anonymousMethod,
                Array.newInstance(new EnclosingType().anonymousMethod, 1).getClass(),
                new EnclosingType().localConstructor,
                Array.newInstance(new EnclosingType().localConstructor, 1).getClass(),
                new EnclosingType().anonymousConstructor,
                Array.newInstance(new EnclosingType().anonymousConstructor, 1).getClass(),
                EnclosingType.LOCAL_INITIALIZER,
                Array.newInstance(EnclosingType.LOCAL_INITIALIZER.getClass(), 1).getClass(),
                EnclosingType.ANONYMOUS_INITIALIZER,
                Array.newInstance(EnclosingType.ANONYMOUS_INITIALIZER, 1).getClass(),
                EnclosingType.LOCAL_METHOD,
                Array.newInstance(EnclosingType.LOCAL_METHOD.getClass(), 1).getClass(),
                EnclosingType.ANONYMOUS_METHOD,
                Array.newInstance(EnclosingType.ANONYMOUS_METHOD, 1).getClass(),
                EnclosingType.INNER,
                Array.newInstance(EnclosingType.INNER, 1).getClass(),
                EnclosingType.NESTED,
                Array.newInstance(EnclosingType.NESTED, 1).getClass(),
                EnclosingType.PRIVATE_INNER,
                Array.newInstance(EnclosingType.PRIVATE_INNER, 1).getClass(),
                EnclosingType.PRIVATE_NESTED,
                Array.newInstance(EnclosingType.PRIVATE_NESTED, 1).getClass(),
                EnclosingType.PROTECTED_INNER,
                Array.newInstance(EnclosingType.PROTECTED_INNER, 1).getClass(),
                EnclosingType.PROTECTED_NESTED,
                Array.newInstance(EnclosingType.PROTECTED_NESTED, 1).getClass(),
                EnclosingType.PACKAGE_INNER,
                Array.newInstance(EnclosingType.PACKAGE_INNER, 1).getClass(),
                EnclosingType.PACKAGE_NESTED,
                Array.newInstance(EnclosingType.PACKAGE_NESTED, 1).getClass(),
                EnclosingType.FINAL_NESTED,
                Array.newInstance(EnclosingType.FINAL_NESTED, 1).getClass(),
                EnclosingType.FINAL_INNER,
                Array.newInstance(EnclosingType.FINAL_INNER, 1).getClass(),
                EnclosingType.DEPRECATED,
                Array.newInstance(EnclosingType.DEPRECATED, 1).getClass());
    }

    @Test
    public void testPrecondition() throws Exception {
        assertThat(describe(SampleClass.class), not(describe(SampleInterface.class)));
        assertThat(describe(SampleClass.class), not(describe(SampleAnnotation.class)));
        assertThat(describe(SampleClass.class), is(describe(SampleClass.class)));
        assertThat(describe(SampleInterface.class), is(describe(SampleInterface.class)));
        assertThat(describe(SampleAnnotation.class), is(describe(SampleAnnotation.class)));
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
    public void testDefaultValue() throws Exception {
        assertThat(describe(void.class).getDefaultValue(), nullValue(Object.class));
        assertThat(describe(boolean.class).getDefaultValue(), is((Object) false));
        assertThat(describe(byte.class).getDefaultValue(), is((Object) (byte) 0));
        assertThat(describe(short.class).getDefaultValue(), is((Object) (short) 0));
        assertThat(describe(char.class).getDefaultValue(), is((Object) (char) 0));
        assertThat(describe(int.class).getDefaultValue(), is((Object) 0));
        assertThat(describe(long.class).getDefaultValue(), is((Object) 0L));
        assertThat(describe(float.class).getDefaultValue(), is((Object) 0f));
        assertThat(describe(double.class).getDefaultValue(), is((Object) 0d));
        assertThat(describe(Object.class).getDefaultValue(), nullValue(Object.class));
        assertThat(describe(SampleClass.class).getDefaultValue(), nullValue(Object.class));
        assertThat(describe(Object[].class).getDefaultValue(), nullValue(Object.class));
        assertThat(describe(long[].class).getDefaultValue(), nullValue(Object.class));
    }

    @Test
    public void testName() throws Exception {
        for (Class<?> type : standardTypes) {
            assertThat(describe(type).getName(), is(type.getName()));
        }
    }

    @Test
    public void testSourceName() throws Exception {
        for (Class<?> type : standardTypes) {
            if (type.isArray()) {
                assertThat(describe(type).getActualName(), is(type.getComponentType().getName() + "[]"));
            } else {
                assertThat(describe(type).getActualName(), is(type.getName()));
            }
        }
    }

    @Test
    public void testInternalName() throws Exception {
        for (Class<?> type : standardTypes) {
            assertThat(describe(type).getInternalName(), is(Type.getInternalName(type)));
        }
    }

    @Test
    public void testCanonicalName() throws Exception {
        for (Class<?> type : standardTypes) {
            assertThat(describe(type).getCanonicalName(), is(type.getCanonicalName()));
        }
    }

    @Test
    public void testSimpleName() throws Exception {
        for (Class<?> type : standardTypes) {
            assertThat(describe(type).getSimpleName(), is(type.getSimpleName()));
        }
    }

    @Test
    public void testIsMemberClass() throws Exception {
        for (Class<?> type : standardTypes) {
            assertThat(describe(type).isMemberClass(), is(type.isMemberClass()));
        }
    }

    @Test
    public void testIsAnonymousClass() throws Exception {
        for (Class<?> type : standardTypes) {
            assertThat(describe(type).isAnonymousClass(), is(type.isAnonymousClass()));
        }
    }

    @Test
    public void testIsLocalClass() throws Exception {
        for (Class<?> type : standardTypes) {
            assertThat(describe(type).isLocalClass(), is(type.isLocalClass()));
        }
    }

    @Test
    public void testJavaName() throws Exception {
        assertThat(describe(Object.class).getActualName(), is(Object.class.getName()));
        assertThat(describe(SampleClass.class).getActualName(), is(SampleClass.class.getName()));
        assertThat(describe(void.class).getActualName(), is(void.class.getName()));
        assertThat(describe(boolean.class).getActualName(), is(boolean.class.getName()));
        assertThat(describe(byte.class).getActualName(), is(byte.class.getName()));
        assertThat(describe(short.class).getActualName(), is(short.class.getName()));
        assertThat(describe(char.class).getActualName(), is(char.class.getName()));
        assertThat(describe(int.class).getActualName(), is(int.class.getName()));
        assertThat(describe(long.class).getActualName(), is(long.class.getName()));
        assertThat(describe(float.class).getActualName(), is(float.class.getName()));
        assertThat(describe(double.class).getActualName(), is(double.class.getName()));
        assertThat(describe(Object[].class).getActualName(), is(Object.class.getName() + "[]"));
        assertThat(describe(SampleClass[].class).getActualName(), is(SampleClass.class.getName() + "[]"));
        assertThat(describe(Object[][].class).getActualName(), is(Object.class.getName() + "[][]"));
        assertThat(describe(boolean[].class).getActualName(), is(boolean.class.getName() + "[]"));
        assertThat(describe(byte[].class).getActualName(), is(byte.class.getName() + "[]"));
        assertThat(describe(short[].class).getActualName(), is(short.class.getName() + "[]"));
        assertThat(describe(char[].class).getActualName(), is(char.class.getName() + "[]"));
        assertThat(describe(int[].class).getActualName(), is(int.class.getName() + "[]"));
        assertThat(describe(long[].class).getActualName(), is(long.class.getName() + "[]"));
        assertThat(describe(float[].class).getActualName(), is(float.class.getName() + "[]"));
        assertThat(describe(double[].class).getActualName(), is(double.class.getName() + "[]"));
    }

    @Test
    public void testDescriptor() throws Exception {
        for (Class<?> type : standardTypes) {
            assertThat(describe(type).getDescriptor(), is(Type.getDescriptor(type)));
        }
    }

    @Test
    public void testModifiers() throws Exception {
        for (Class<?> type : standardTypes) {
            assertThat(describe(type).getModifiers(), is(type.getModifiers()));
        }
    }

    @Test
    public void testDeclaringType() throws Exception {
        for (Class<?> type : standardTypes) {
            assertThat(describe(type).getDeclaringType(), type.getDeclaringClass() == null
                    ? nullValue(TypeDescription.class)
                    : is((TypeDescription) new TypeDescription.ForLoadedType(type.getDeclaringClass())));
        }
    }

    @Test
    public void testEnclosingMethod() throws Exception {
        for (Class<?> type : standardTypes) {
            Matcher<MethodDescription> matcher;
            if (type.getEnclosingMethod() != null) {
                matcher = CoreMatchers.<MethodDescription>is(new MethodDescription.ForLoadedMethod(type.getEnclosingMethod()));
            } else if (type.getEnclosingConstructor() != null) {
                matcher = CoreMatchers.<MethodDescription>is(new MethodDescription.ForLoadedConstructor(type.getEnclosingConstructor()));
            } else {
                matcher = nullValue(MethodDescription.class);
            }
            assertThat(describe(type).getEnclosingMethod(), matcher);
        }
    }

    @Test
    public void testEnclosingType() throws Exception {
        for (Class<?> type : standardTypes) {
            assertThat(describe(type).getEnclosingType(), type.getEnclosingClass() == null
                    ? nullValue(TypeDescription.class)
                    : is((TypeDescription) new TypeDescription.ForLoadedType(type.getEnclosingClass())));
        }
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(describe(SampleClass.class).hashCode(), is(SampleClass.class.getName().hashCode()));
        assertThat(describe(SampleInterface.class).hashCode(), is(SampleInterface.class.getName().hashCode()));
        assertThat(describe(SampleAnnotation.class).hashCode(), is(SampleAnnotation.class.getName().hashCode()));
        assertThat(describe(SampleClass.class).hashCode(), is(describe(SampleClass.class).hashCode()));
        assertThat(describe(SampleClass.class).hashCode(), not(describe(SampleInterface.class).hashCode()));
        assertThat(describe(SampleClass.class).hashCode(), not(describe(SampleAnnotation.class).hashCode()));
        assertThat(describe(Object[].class).hashCode(), is(describe(Object[].class).hashCode()));
        assertThat(describe(Object[].class).hashCode(), not(describe(Object.class).hashCode()));
        assertThat(describe(void.class).hashCode(), is(void.class.getName().hashCode()));
    }

    @Test
    public void testEquals() throws Exception {
        TypeDescription identical = describe(SampleClass.class);
        assertThat(identical, is(identical));
        TypeDescription equalFirst = mock(TypeDescription.class);
        when(equalFirst.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(equalFirst.asErasure()).thenReturn(equalFirst);
        when(equalFirst.getName()).thenReturn(SampleClass.class.getName());
        assertThat(describe(SampleClass.class), is(equalFirst));
        assertThat(describe(SampleClass.class), not(describe(SampleInterface.class)));
        assertThat(describe(SampleClass.class), not((TypeDescription) new TypeDescription.ForLoadedType(SampleInterface.class)));
        TypeDefinition nonRawType = mock(TypeDescription.Generic.class);
        when(nonRawType.getSort()).thenReturn(TypeDefinition.Sort.VARIABLE);
        assertThat(describe(SampleClass.class), not(nonRawType));
        assertThat(describe(SampleClass.class), not(new Object()));
        assertThat(describe(SampleClass.class), not(equalTo(null)));
        assertThat(describe(Object[].class), is((TypeDescription) new TypeDescription.ForLoadedType(Object[].class)));
        assertThat(describe(Object[].class), not(TypeDescription.OBJECT));
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
    public void testActualModifiersDeprecation() throws Exception {
        assertThat(describe(EnclosingType.DEPRECATED).getActualModifiers(false), is(Opcodes.ACC_DEPRECATED));
        assertThat(describe(EnclosingType.DEPRECATED).getActualModifiers(true), is(Opcodes.ACC_DEPRECATED | Opcodes.ACC_SUPER));
    }

    @Test
    public void testSuperClass() throws Exception {
        assertThat(describe(Object.class).getSuperClass(), nullValue(TypeDescription.Generic.class));
        assertThat(describe(SampleInterface.class).getSuperClass(), nullValue(TypeDescription.Generic.class));
        assertThat(describe(SampleAnnotation.class).getSuperClass(), nullValue(TypeDescription.Generic.class));
        assertThat(describe(void.class).getSuperClass(), nullValue(TypeDescription.Generic.class));
        assertThat(describe(SampleClass.class).getSuperClass(), is(TypeDescription.Generic.OBJECT));
        assertThat(describe(SampleIndirectInterfaceImplementation.class).getSuperClass(),
                is((TypeDefinition) new TypeDescription.ForLoadedType(SampleInterfaceImplementation.class)));
        assertThat(describe(Object[].class).getSuperClass(), is(TypeDescription.Generic.OBJECT));
    }

    @Test
    public void testInterfaces() throws Exception {
        assertThat(describe(Object.class).getInterfaces(), is((TypeList.Generic) new TypeList.Generic.Empty()));
        assertThat(describe(SampleInterface.class).getInterfaces(), is((TypeList.Generic) new TypeList.Generic.Empty()));
        assertThat(describe(SampleAnnotation.class).getInterfaces(), is((TypeList.Generic) new TypeList.Generic.ForLoadedTypes(Annotation.class)));
        assertThat(describe(SampleInterfaceImplementation.class).getInterfaces(),
                is((TypeList.Generic) new TypeList.Generic.ForLoadedTypes(SampleInterface.class)));
        assertThat(describe(SampleIndirectInterfaceImplementation.class).getInterfaces(), is((TypeList.Generic) new TypeList.Generic.Empty()));
        assertThat(describe(SampleTransitiveInterfaceImplementation.class).getInterfaces(),
                is((TypeList.Generic) new TypeList.Generic.ForLoadedTypes(SampleTransitiveInterface.class)));
        assertThat(describe(Object[].class).getInterfaces(), is((TypeList.Generic) new TypeList.Generic.ForLoadedTypes(Cloneable.class, Serializable.class)));
    }

    @Test
    public void testToString() throws Exception {
        for (Class<?> type : standardTypes) {
            assertThat(describe(type).toString(), is(type.toString()));
        }
    }

    @Test
    public void testIsAssignable() throws Exception {
        assertThat(describe(Object.class).isAssignableTo(Object.class), is(true));
        assertThat(describe(Object.class).isAssignableFrom(Object.class), is(true));
        assertThat(describe(Object[].class).isAssignableTo(Object.class), is(true));
        assertThat(describe(Object.class).isAssignableFrom(Object[].class), is(true));
        assertThat(describe(Object[].class).isAssignableTo(Serializable.class), is(true));
        assertThat(describe(Serializable.class).isAssignableFrom(Object[].class), is(true));
        assertThat(describe(Object[].class).isAssignableTo(Cloneable.class), is(true));
        assertThat(describe(Cloneable.class).isAssignableFrom(Object[].class), is(true));
        assertThat(describe(String[].class).isAssignableTo(Object[].class), is(true));
        assertThat(describe(Object[].class).isAssignableFrom(String[].class), is(true));
        assertThat(describe(Object[].class).isAssignableFrom(String[][].class), is(true));
        assertThat(describe(String[][].class).isAssignableTo(Object[].class), is(true));
        assertThat(describe(String[].class).isAssignableFrom(String[][].class), is(false));
        assertThat(describe(String[][].class).isAssignableTo(String[].class), is(false));
        assertThat(describe(Cloneable[].class).isAssignableFrom(String[].class), is(false));
        assertThat(describe(String[].class).isAssignableTo(Cloneable[].class), is(false));
        assertThat(describe(Foo[].class).isAssignableFrom(String[].class), is(false));
        assertThat(describe(String[].class).isAssignableTo(Foo[].class), is(false));
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
        ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                ClassFileExtraction.of(SimpleType.class),
                ByteArrayClassLoader.PersistenceHandler.MANIFEST);
        Class<?> otherSimpleType = classLoader.loadClass(SimpleType.class.getName());
        assertThat(describe(SimpleType.class).isAssignableFrom(describe(otherSimpleType)), is(true));
        assertThat(describe(SimpleType.class).isAssignableTo(describe(otherSimpleType)), is(true));
        assertThat(describe(Object.class).isAssignableFrom(describe(otherSimpleType)), is(true));
        assertThat(describe(otherSimpleType).isAssignableTo(describe(Object.class)), is(true));
    }

    @Test
    public void testIsVisible() throws Exception {
        assertThat(describe(SampleClass.class).isVisibleTo(new TypeDescription.ForLoadedType(SampleInterface.class)), is(true));
        assertThat(describe(SamplePackagePrivate.class).isVisibleTo(new TypeDescription.ForLoadedType(SampleClass.class)), is(true));
        assertThat(describe(SampleInterface.class).isVisibleTo(new TypeDescription.ForLoadedType(SampleClass.class)), is(true));
        assertThat(describe(SampleAnnotation.class).isVisibleTo(new TypeDescription.ForLoadedType(SampleClass.class)), is(true));
        assertThat(describe(SamplePackagePrivate.class).isVisibleTo(TypeDescription.OBJECT), is(false));
        assertThat(describe(SampleInterface.class).isVisibleTo(TypeDescription.OBJECT), is(true));
        assertThat(describe(SampleAnnotation.class).isVisibleTo(TypeDescription.OBJECT), is(false));
        assertThat(describe(int.class).isVisibleTo(TypeDescription.OBJECT), is(true));
        assertThat(describe(SampleInterface[].class).isVisibleTo(TypeDescription.OBJECT), is(true));
        assertThat(describe(SamplePackagePrivate[].class).isVisibleTo(TypeDescription.OBJECT), is(false));
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
                hasItems(new AnnotationList.ForLoadedAnnotations(type.getDeclaredAnnotations())
                        .toArray(new AnnotationDescription[type.getDeclaredAnnotations().length])));
        assertThat(describe(type).getDeclaredAnnotations().size(), is(type.getDeclaredAnnotations().length));
        assertThat(describe(type).getInheritedAnnotations(),
                hasItems(new AnnotationList.ForLoadedAnnotations(type.getAnnotations())
                        .toArray(new AnnotationDescription[type.getAnnotations().length])));
        assertThat(describe(type).getInheritedAnnotations().size(), is(type.getAnnotations().length));
    }

    @Test
    public void testDeclaredTypes() throws Exception {
        assertThat(describe(SampleClass.class).getDeclaredTypes().size(), is(0));
        assertThat(describe(AbstractTypeDescriptionTest.class).getDeclaredTypes(),
                is((TypeList) new TypeList.ForLoadedTypes(AbstractTypeDescriptionTest.class.getDeclaredClasses())));
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
        assertThat(describe(SampleGenericType.class).getSuperClass(), is(new TypeDescription.ForLoadedType(SampleGenericType.class).getSuperClass()));
        assertThat(describe(SampleGenericType.class).getInterfaces(), is(new TypeDescription.ForLoadedType(SampleGenericType.class).getInterfaces()));
    }

    @Test
    public void testHierarchyIteration() throws Exception {
        Iterator<TypeDefinition> iterator = describe(Traversal.class).iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is((TypeDefinition) new TypeDescription.ForLoadedType(Traversal.class)));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is((TypeDefinition) TypeDescription.OBJECT));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test(expected = NoSuchElementException.class)
    public void testHierarchyEnds() throws Exception {
        Iterator<TypeDefinition> iterator = describe(Object.class).iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is((TypeDefinition) TypeDescription.OBJECT));
        assertThat(iterator.hasNext(), is(false));
        iterator.next();
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
    public void testRepresents() throws Exception {
        assertThat(describe(Object.class).represents(Object.class), is(true));
        assertThat(describe(Object.class).represents(Serializable.class), is(false));
        assertThat(describe(List.class).represents(SimpleParameterizedType.class.getDeclaredField(FOO).getGenericType()), is(false));
    }

    @Test
    public void testNonAvailableAnnotations() throws Exception {
        TypeDescription typeDescription = describe(new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                ClassFileExtraction.of(MissingAnnotations.class),
                ByteArrayClassLoader.PersistenceHandler.MANIFEST).loadClass(MissingAnnotations.class.getName()));
        assertThat(typeDescription.getDeclaredAnnotations().isAnnotationPresent(SampleAnnotation.class), is(false));
        assertThat(typeDescription.getDeclaredFields().getOnly().getDeclaredAnnotations().isAnnotationPresent(SampleAnnotation.class), is(false));
        assertThat(typeDescription.getDeclaredMethods().filter(isMethod()).getOnly().getDeclaredAnnotations().isAnnotationPresent(SampleAnnotation.class), is(false));
    }

    @Test
    public void testIsPackageDescription() throws Exception {
        assertThat(describe(Class.forName(Sample.class.getPackage().getName() + "." + PackageDescription.PACKAGE_CLASS_NAME)).isPackageType(), is(true));
        assertThat(describe(Object.class).isPackageType(), is(false));
    }

    @Test
    public void testEnclosingSource() throws Exception {
        assertThat(describe(SampleClass.class).getEnclosingSource(), is((TypeVariableSource) describe(AbstractTypeDescriptionTest.class)));
        assertThat(describe(Traversal.class).getEnclosingSource(), nullValue(TypeVariableSource.class));
    }

    @Test
    public void testInMethodType() throws Exception {
        assertThat(describe(inMethodClass()).getEnclosingMethod(),
                is((TypeVariableSource) new MethodDescription.ForLoadedMethod(AbstractTypeDescriptionTest.class.getDeclaredMethod("inMethodClass"))));
        assertThat(describe(inMethodClass()).getEnclosingSource(),
                is((TypeVariableSource) new MethodDescription.ForLoadedMethod(AbstractTypeDescriptionTest.class.getDeclaredMethod("inMethodClass"))));
    }

    @Test
    public void testEnclosingAndDeclaringType() throws Exception {
        assertThat(describe(SampleClass.class).getEnclosingType(), is(describe(AbstractTypeDescriptionTest.class)));
        assertThat(describe(SampleClass.class).getDeclaringType(), is(describe(AbstractTypeDescriptionTest.class)));
        Class<?> anonymousType = new Object() {
            /* empty */
        }.getClass();
        assertThat(describe(anonymousType).getEnclosingType(), is(describe(AbstractTypeDescriptionTest.class)));
        assertThat(describe(anonymousType).getDeclaringType(), nullValue(TypeDescription.class));
    }

    @Test
    public void testIsGenerified() throws Exception {
        assertThat(describe(GenericSample.class).isGenerified(), is(true));
        assertThat(describe(GenericSample.Inner.class).isGenerified(), is(true));
        assertThat(describe(GenericSample.Nested.class).isGenerified(), is(false));
        assertThat(describe(GenericSample.NestedInterface.class).isGenerified(), is(false));
        assertThat(describe(Object.class).isGenerified(), is(false));
    }

    @Test
    public void testGetSegmentCount() throws Exception {
        assertThat(describe(GenericSample.class).getInnerClassCount(), is(0));
        assertThat(describe(GenericSample.Inner.class).getInnerClassCount(), is(1));
        assertThat(describe(GenericSample.Nested.class).getInnerClassCount(), is(0));
        assertThat(describe(GenericSample.NestedInterface.class).getInnerClassCount(), is(0));
        assertThat(describe(Object.class).getInnerClassCount(), is(0));
    }

    @Test
    public void testBoxed() throws Exception {
        assertThat(describe(boolean.class).asBoxed(), is(describe(Boolean.class)));
        assertThat(describe(byte.class).asBoxed(), is(describe(Byte.class)));
        assertThat(describe(short.class).asBoxed(), is(describe(Short.class)));
        assertThat(describe(char.class).asBoxed(), is(describe(Character.class)));
        assertThat(describe(int.class).asBoxed(), is(describe(Integer.class)));
        assertThat(describe(long.class).asBoxed(), is(describe(Long.class)));
        assertThat(describe(float.class).asBoxed(), is(describe(Float.class)));
        assertThat(describe(double.class).asBoxed(), is(describe(Double.class)));
        assertThat(describe(void.class).asBoxed(), is(describe(void.class)));
        assertThat(describe(Object.class).asBoxed(), is(describe(Object.class)));
    }

    @Test
    public void testUnboxed() throws Exception {
        assertThat(describe(Boolean.class).asUnboxed(), is(describe(boolean.class)));
        assertThat(describe(Byte.class).asUnboxed(), is(describe(byte.class)));
        assertThat(describe(Short.class).asUnboxed(), is(describe(short.class)));
        assertThat(describe(Character.class).asUnboxed(), is(describe(char.class)));
        assertThat(describe(Integer.class).asUnboxed(), is(describe(int.class)));
        assertThat(describe(Long.class).asUnboxed(), is(describe(long.class)));
        assertThat(describe(Float.class).asUnboxed(), is(describe(float.class)));
        assertThat(describe(Double.class).asUnboxed(), is(describe(double.class)));
        assertThat(describe(Void.class).asUnboxed(), is(describe(Void.class)));
        assertThat(describe(Object.class).asUnboxed(), is(describe(Object.class)));
    }

    private Class<?> inMethodClass() {
        class InMethod {
            /* empty */
        }
        return InMethod.class;
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
            super(Opcodes.ASM6, classVisitor);
        }

        public static Class<?> malform(Class<?> type) throws Exception {
            ClassReader classReader = new ClassReader(type.getName());
            ClassWriter classWriter = new ClassWriter(classReader, 0);
            classReader.accept(new SignatureMalformer(classWriter), 0);
            ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                    Collections.singletonMap(type.getName(), classWriter.toByteArray()),
                    ByteArrayClassLoader.PersistenceHandler.MANIFEST);
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

    @SuppressWarnings("unused")
    public static class InnerInnerClass {

        public static class Foo {
            /* empty */
        }
    }

    @SampleAnnotation
    public static class MissingAnnotations {

        @SampleAnnotation
        Void foo;

        @SampleAnnotation
        void foo(@SampleAnnotation Void foo) {
            /* empty */
        }
    }

    private static class GenericSample<T> {

        static class Nested {
            /* empty */
        }

        class Inner {
            /* empty */
        }

        interface NestedInterface {
            /* empty */
        }
    }
}
