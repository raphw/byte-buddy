package net.bytebuddy.description.type;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.TypeVariableSource;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.packaging.SimpleType;
import net.bytebuddy.test.scope.EnclosingType;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.visibility.Sample;
import net.bytebuddy.utility.AsmClassReader;
import net.bytebuddy.utility.AsmClassWriter;
import net.bytebuddy.utility.OpenedClassReader;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractTypeDescriptionTest extends AbstractTypeDescriptionGenericVariableDefiningTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private final List<Class<?>> standardTypes;

    protected AbstractTypeDescriptionTest() {
        standardTypes = Arrays.<Class<?>>asList(
//                Object.class,
//                Object[].class,
//                SampleClass.class,
//                SampleClass[].class,
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
                Array.newInstance(EnclosingType.LOCAL_INITIALIZER, 1).getClass(),
                EnclosingType.ANONYMOUS_INITIALIZER,
                Array.newInstance(EnclosingType.ANONYMOUS_INITIALIZER, 1).getClass(),
                EnclosingType.LOCAL_METHOD,
                Array.newInstance(EnclosingType.LOCAL_METHOD, 1).getClass(),
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
                Array.newInstance(EnclosingType.DEPRECATED, 1).getClass(),
                Type$With$Dollar.class,
                new ByteBuddy()
                        .subclass(Object.class)
                        .name("sample.WithoutDefinedPackage")
                        .make()
                        .load(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                                ClassLoadingStrategy.Default.WRAPPER_PERSISTENT.with(PackageDefinitionStrategy.NoOp.INSTANCE))
                        .getLoaded(),
                new ByteBuddy()
                        .subclass(Object.class)
                        .name("WithoutPackage")
                        .make()
                        .load(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                                ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                        .getLoaded());
    }

    @Test
    @SuppressWarnings("cast")
    public void testPrecondition() throws Exception {
        assertThat(describe(SampleClass.class), not(describe(SampleInterface.class)));
        assertThat(describe(SampleClass.class), not(describe(SampleAnnotation.class)));
        assertThat(describe(SampleClass.class), is(describe(SampleClass.class)));
        assertThat(describe(SampleInterface.class), is(describe(SampleInterface.class)));
        assertThat(describe(SampleAnnotation.class), is(describe(SampleAnnotation.class)));
        assertThat(describe(SampleClass.class), is((TypeDescription) TypeDescription.ForLoadedType.of(SampleClass.class)));
        assertThat(describe(SampleInterface.class), is((TypeDescription) TypeDescription.ForLoadedType.of(SampleInterface.class)));
        assertThat(describe(SampleAnnotation.class), is((TypeDescription) TypeDescription.ForLoadedType.of(SampleAnnotation.class)));
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
    public void getLongSimpleName() throws Exception {
        for (Class<?> type : standardTypes) {
            if (type.getDeclaringClass() == null) {
                assertThat(describe(type).getLongSimpleName(), is(type.getSimpleName()));
            } else {
                assertThat(describe(type).getLongSimpleName(), is(type.getDeclaringClass().getSimpleName()
                        + "."
                        + type.getSimpleName()));
            }
        }
    }

    @Test
    public void testIsMemberType() throws Exception {
        for (Class<?> type : standardTypes) {
            assertThat(describe(type).isMemberType(), is(type.isMemberClass()));
        }
    }

    @Test
    public void testIsAnonymousType() throws Exception {
        for (Class<?> type : standardTypes) {
            assertThat(describe(type).isAnonymousType(), is(type.isAnonymousClass()));
        }
    }

    @Test
    public void testIsLocalType() throws Exception {
        for (Class<?> type : standardTypes) {
            assertThat(describe(type).isLocalType(), is(type.isLocalClass()));
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
    @SuppressWarnings("cast")
    public void testDeclaringType() throws Exception {
        for (Class<?> type : standardTypes) {
            assertThat(describe(type).getDeclaringType(), type.getDeclaringClass() == null
                    ? nullValue(TypeDescription.class)
                    : is((TypeDescription) TypeDescription.ForLoadedType.of(type.getDeclaringClass())));
        }
    }

    @Test
    public void testDeclaredTypes() throws Exception {
        for (Class<?> type : standardTypes) {
            assertThat(describe(type).getDeclaredTypes(), is((TypeList) new TypeList.ForLoadedTypes(type.getDeclaredClasses())));
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
    @SuppressWarnings("cast")
    public void testEnclosingType() throws Exception {
        for (Class<?> type : standardTypes) {
            assertThat(describe(type).getEnclosingType(), type.getEnclosingClass() == null
                    ? nullValue(TypeDescription.class)
                    : is((TypeDescription) TypeDescription.ForLoadedType.of(type.getEnclosingClass())));
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
    @SuppressWarnings("cast")
    public void testEquals() throws Exception {
        TypeDescription identical = describe(SampleClass.class);
        assertThat(identical, is(identical));
        TypeDescription equalFirst = mock(TypeDescription.class);
        when(equalFirst.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(equalFirst.asErasure()).thenReturn(equalFirst);
        when(equalFirst.getName()).thenReturn(SampleClass.class.getName());
        assertThat(describe(SampleClass.class), is(equalFirst));
        assertThat(describe(SampleClass.class), not(describe(SampleInterface.class)));
        assertThat(describe(SampleClass.class), not((TypeDescription) TypeDescription.ForLoadedType.of(SampleInterface.class)));
        TypeDefinition nonRawType = mock(TypeDescription.Generic.class);
        when(nonRawType.getSort()).thenReturn(TypeDefinition.Sort.VARIABLE);
        assertThat(describe(SampleClass.class), not(nonRawType));
        assertThat(describe(SampleClass.class), not(new Object()));
        assertThat(describe(SampleClass.class), not(equalTo(null)));
        assertThat(describe(Object[].class), is((TypeDescription) TypeDescription.ForLoadedType.of(Object[].class)));
        assertThat(describe(Object[].class), not(TypeDescription.ForLoadedType.of(Object.class)));
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
        for (Class<?> type : standardTypes) {
            if (type.isArray() || type.isPrimitive()) {
                assertThat(describe(type).getPackage(), nullValue(PackageDescription.class));
            } else {
                String packageName = type.getName();
                int packageIndex = packageName.lastIndexOf('.');
                assertThat(describe(type).getPackage().getName(), is(packageIndex == -1
                        ? ""
                        : packageName.substring(0, packageIndex)));
            }
        }
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
        assertThat(describe(SampleClass.class).getSuperClass(), is(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)));
        assertThat(describe(SampleIndirectInterfaceImplementation.class).getSuperClass(),
                is((TypeDefinition) TypeDescription.ForLoadedType.of(SampleInterfaceImplementation.class)));
        assertThat(describe(Object[].class).getSuperClass(), is(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)));
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
                ClassFileLocator.ForClassLoader.readToNames(SimpleType.class),
                ByteArrayClassLoader.PersistenceHandler.MANIFEST);
        Class<?> otherSimpleType = classLoader.loadClass(SimpleType.class.getName());
        assertThat(describe(SimpleType.class).isAssignableFrom(describe(otherSimpleType)), is(true));
        assertThat(describe(SimpleType.class).isAssignableTo(describe(otherSimpleType)), is(true));
        assertThat(describe(Object.class).isAssignableFrom(describe(otherSimpleType)), is(true));
        assertThat(describe(otherSimpleType).isAssignableTo(describe(Object.class)), is(true));
    }

    @Test
    public void testIsInHierarchyWith() throws Exception {
        assertThat(describe(Object.class).isInHierarchyWith(Object.class), is(true));
        assertThat(describe(Object.class).isInHierarchyWith(String.class), is(true));
        assertThat(describe(String.class).isInHierarchyWith(Object.class), is(true));
        assertThat(describe(Integer.class).isInHierarchyWith(Long.class), is(false));
        assertThat(describe(Integer.class).isInHierarchyWith(int.class), is(false));
        assertThat(describe(Object.class).isInHierarchyWith(int.class), is(false));
    }

    @Test
    public void testIsVisible() throws Exception {
        assertThat(describe(SampleClass.class).isVisibleTo(TypeDescription.ForLoadedType.of(SampleInterface.class)), is(true));
        assertThat(describe(SamplePackagePrivate.class).isVisibleTo(TypeDescription.ForLoadedType.of(SampleClass.class)), is(true));
        assertThat(describe(SampleInterface.class).isVisibleTo(TypeDescription.ForLoadedType.of(SampleClass.class)), is(true));
        assertThat(describe(OtherAnnotation.class).isVisibleTo(TypeDescription.ForLoadedType.of(SampleClass.class)), is(true));
        assertThat(describe(SamplePackagePrivate.class).isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(SampleInterface.class).isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(true));
        assertThat(describe(OtherAnnotation.class).isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(int.class).isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(true));
        assertThat(describe(SampleInterface[].class).isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(true));
        assertThat(describe(SamplePackagePrivate[].class).isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
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
                        .toArray(new AnnotationDescription[0])));
        assertThat(describe(type).getDeclaredAnnotations().size(), is(type.getDeclaredAnnotations().length));
        assertThat(describe(type).getInheritedAnnotations(),
                hasItems(new AnnotationList.ForLoadedAnnotations(type.getAnnotations())
                        .toArray(new AnnotationDescription[0])));
        assertThat(describe(type).getInheritedAnnotations().size(), is(type.getAnnotations().length));
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
    public void testGenericType() throws Exception {
        assertThat(describe(SampleGenericType.class).getTypeVariables(), is(TypeDescription.ForLoadedType.of(SampleGenericType.class).getTypeVariables()));
        assertThat(describe(SampleGenericType.class).getSuperClass(), is(TypeDescription.ForLoadedType.of(SampleGenericType.class).getSuperClass()));
        assertThat(describe(SampleGenericType.class).getInterfaces(), is(TypeDescription.ForLoadedType.of(SampleGenericType.class).getInterfaces()));
    }

    @Test
    public void testHierarchyIteration() throws Exception {
        Iterator<TypeDefinition> iterator = describe(Traversal.class).iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is((TypeDefinition) TypeDescription.ForLoadedType.of(Traversal.class)));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is((TypeDefinition) TypeDescription.ForLoadedType.of(Object.class)));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test(expected = NoSuchElementException.class)
    public void testHierarchyEnds() throws Exception {
        Iterator<TypeDefinition> iterator = describe(Object.class).iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is((TypeDefinition) TypeDescription.ForLoadedType.of(Object.class)));
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

    @Test(expected = TypeNotPresentException.class)
    public void testMalformedTypeVariableDefinition() throws Exception {
        TypeDescription typeDescription = describe(TypeVariableMalformer.malform(MalformedTypeVariable.class));
        assertThat(typeDescription.getDeclaredFields().size(), is(1));
        typeDescription.getDeclaredFields().getOnly().getType().getUpperBounds();
    }

    @Test(expected = TypeNotPresentException.class)
    public void testMalformedParameterizedTypeVariableDefinition() throws Exception {
        TypeDescription typeDescription = describe(TypeVariableMalformer.malform(MalformedParameterizedTypeVariable.class));
        assertThat(typeDescription.getDeclaredFields().getOnly().getType().getTypeArguments().size(), is(1));
        typeDescription.getDeclaredFields().getOnly().getType().getTypeArguments().getOnly().getUpperBounds();
    }

    @Test(expected = MalformedParameterizedTypeException.class)
    public void testMalformedParameterizedLengthDefinitionArguments() throws Exception {
        TypeDescription typeDescription = describe(ParameterizedTypeLengthMalformer.malform(MalformedParameterizedLength.class));
        typeDescription.getDeclaredFields().getOnly().getType().getTypeArguments();
    }

    @Test(expected = MalformedParameterizedTypeException.class)
    public void testMalformedParameterizedLengthDefinitionOwner() throws Exception {
        TypeDescription typeDescription = describe(ParameterizedTypeLengthMalformer.malform(MalformedParameterizedLength.class));
        typeDescription.getDeclaredFields().getOnly().getType().getOwnerType();
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
                ClassFileLocator.ForClassLoader.readToNames(MissingAnnotations.class),
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
        assertThat(describe(anonymousGenericSample().getClass()).isGenerified(), is(true));
        assertThat(describe(innerGenericSample().getClass()).isGenerified(), is(true));
    }

    @Test
    public void testInnerClass() throws Exception {
        assertThat(describe(Object.class).isInnerClass(), is(false));
        assertThat(describe(GenericSample.class).isInnerClass(), is(false));
        assertThat(describe(GenericSample.Inner.class).isInnerClass(), is(true));
    }

    @Test
    public void testNestedClass() throws Exception {
        assertThat(describe(Object.class).isNestedClass(), is(false));
        assertThat(describe(GenericSample.class).isNestedClass(), is(true));
        assertThat(describe(GenericSample.Inner.class).isNestedClass(), is(true));
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

    @Test
    public void testNestMatesTrivial() throws Exception {
        assertThat(describe(Object.class).getNestHost(), is(TypeDescription.ForLoadedType.of(Object.class)));
        assertThat(describe(Object.class).getNestMembers().size(), is(1));
        assertThat(describe(Object.class).getNestMembers(), hasItem(TypeDescription.ForLoadedType.of(Object.class)));
        assertThat(describe(Object.class).isNestMateOf(Object.class), is(true));
        assertThat(describe(Object.class).isNestMateOf(TypeDescription.ForLoadedType.of(String.class)), is(false));
        assertThat(describe(Object.class).isNestHost(), is(true));
    }

    @Test
    @JavaVersionRule.Enforce(value = 11, target = SampleClass.class)
    public void testNestMatesSupported() throws Exception {
        assertThat(describe(SampleClass.class).getNestHost(), is(describe(AbstractTypeDescriptionTest.class)));
        assertThat(describe(SampleClass.class).getNestMembers(), hasItems(describe(SampleClass.class), describe(AbstractTypeDescriptionTest.class)));
        assertThat(describe(SampleClass.class).isNestHost(), is(false));
        assertThat(describe(AbstractTypeDescriptionTest.class).getNestHost(), is(describe(AbstractTypeDescriptionTest.class)));
        assertThat(describe(AbstractTypeDescriptionTest.class).getNestMembers(), hasItems(describe(SampleClass.class), describe(AbstractTypeDescriptionTest.class)));
        assertThat(describe(AbstractTypeDescriptionTest.class).isNestHost(), is(true));
        assertThat(describe(SampleClass.class).isNestMateOf(SampleClass.class), is(true));
        assertThat(describe(SampleClass.class).isNestMateOf(AbstractTypeDescriptionTest.class), is(true));
        assertThat(describe(AbstractTypeDescriptionTest.class).isNestMateOf(SampleClass.class), is(true));
        assertThat(describe(AbstractTypeDescriptionTest.class).isNestMateOf(Object.class), is(false));
        assertThat(describe(Object.class).isNestMateOf(AbstractTypeDescriptionTest.class), is(false));
    }

    @Test
    public void testNonEnclosedAnonymousType() throws Exception {
        AsmClassWriter classWriter = AsmClassWriter.Factory.Default.IMPLICIT.make(AsmVisitorWrapper.NO_FLAGS);
        classWriter.getVisitor().visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, "foo/Bar", null, "java/lang/Object", null);
        classWriter.getVisitor().visitInnerClass("foo/Bar", null, null, Opcodes.ACC_PUBLIC);
        classWriter.getVisitor().visitEnd();

        ClassLoader classLoader = new ByteArrayClassLoader(null,
                Collections.singletonMap("foo.Bar", classWriter.getBinaryRepresentation()),
                ByteArrayClassLoader.PersistenceHandler.MANIFEST);
        Class<?> type = classLoader.loadClass("foo.Bar");

        assertThat(describe(type).isAnonymousType(), is(type.isAnonymousClass()));
        assertThat(describe(type).isLocalType(), is(type.isLocalClass()));
        assertThat(describe(type).isMemberType(), is(type.isMemberClass()));
    }

    @Test
    public void testNotSealed() throws Exception {
        assertThat(describe(SampleClass.class).isSealed(), is(false));
        assertThat(describe(SampleClass.class).getPermittedSubtypes().isEmpty(), is(true));
    }

    @Test
    @JavaVersionRule.Enforce(17)
    public void testSealed() throws Exception {
        Class<?> sealed = Class.forName("net.bytebuddy.test.precompiled.v17.Sealed");
        assertThat(describe(sealed).isSealed(), is(true));
        assertThat(describe(sealed).getPermittedSubtypes().size(), is(3));
        assertThat(describe(sealed).getPermittedSubtypes().get(0), is(describe(Class.forName(sealed.getName() + "$SubNonSealed"))));
        assertThat(describe(sealed).getPermittedSubtypes().get(1), is(describe(Class.forName(sealed.getName() + "$SubSealed"))));
        assertThat(describe(sealed).getPermittedSubtypes().get(2), is(describe(Class.forName(sealed.getName() + "$SubFinal"))));
    }

    @Test
    public void testNonRecordComponents() throws Exception {
        assertThat(describe(String.class).isRecord(), is(false));
        assertThat(describe(String.class).getRecordComponents().size(), is(0));
    }

    @Test
    @JavaVersionRule.Enforce(16)
    public void testRecordComponents() throws Exception {
        Class<?> sampleRecord = Class.forName("net.bytebuddy.test.precompiled.v16.RecordSample");
        assertThat(describe(sampleRecord).isRecord(), is(true));
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName("net.bytebuddy.test.precompiled.v8.TypeAnnotation");
        MethodDescription.InDefinedShape value = new MethodDescription.ForLoadedMethod(typeAnnotation.getMethod("value"));
        RecordComponentList<RecordComponentDescription.InDefinedShape> recordComponents = describe(sampleRecord).getRecordComponents();
        assertThat(recordComponents.size(), is(1));
        assertThat(recordComponents.getOnly().getActualName(), is(FOO));
        assertThat(recordComponents.getOnly().getAccessor(), is((MethodDescription) new MethodDescription.ForLoadedMethod(sampleRecord.getMethod(FOO))));
        assertThat(recordComponents.getOnly().getDeclaringType(), is((TypeDefinition) TypeDescription.ForLoadedType.of(sampleRecord)));
        assertThat(recordComponents.getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(recordComponents.getOnly().getDeclaredAnnotations().getOnly().getAnnotationType().represents(SampleAnnotation.class), is(true));
        assertThat(recordComponents.getOnly().getType().asErasure().represents(List.class), is(true));
        assertThat(recordComponents.getOnly().getType().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(recordComponents.getOnly().getType().getDeclaredAnnotations().size(), is(1));
        assertThat(recordComponents.getOnly().getType().getDeclaredAnnotations().getOnly().getAnnotationType().represents(typeAnnotation), is(true));
        assertThat(recordComponents.getOnly().getType().getDeclaredAnnotations().getOnly().prepare(typeAnnotation).getValue(value).resolve(), is((Object) 42));
        assertThat(recordComponents.getOnly().getType().getTypeArguments().size(), is(1));
        assertThat(recordComponents.getOnly().getType().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(recordComponents.getOnly().getType().getTypeArguments().getOnly().asErasure().represents(String.class), is(true));
        assertThat(recordComponents.getOnly().getType().getTypeArguments().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(recordComponents.getOnly().getType().getTypeArguments().getOnly().getDeclaredAnnotations().getOnly().getAnnotationType().represents(typeAnnotation), is(true));
        assertThat(recordComponents.getOnly().getType().getTypeArguments().getOnly().getDeclaredAnnotations().getOnly().prepare(typeAnnotation).getValue(value).resolve(), is((Object) 84));
    }

    @Test
    @JavaVersionRule.Enforce(16)
    public void testRecordComponentsField() throws Exception {
        Class<?> sampleRecord = Class.forName("net.bytebuddy.test.precompiled.v16.RecordSample");
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName("net.bytebuddy.test.precompiled.v8.TypeAnnotation");
        MethodDescription.InDefinedShape value = new MethodDescription.ForLoadedMethod(typeAnnotation.getMethod("value"));
        FieldDescription fieldDescription = describe(sampleRecord).getDeclaredFields().filter(named(FOO)).getOnly();
        assertThat(fieldDescription.getDeclaredAnnotations().size(), is(1));
        assertThat(fieldDescription.getDeclaredAnnotations().getOnly().getAnnotationType().represents(SampleAnnotation.class), is(true));
        assertThat(fieldDescription.getType().asErasure().represents(List.class), is(true));
        assertThat(fieldDescription.getType().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(fieldDescription.getType().getDeclaredAnnotations().size(), is(1));
        assertThat(fieldDescription.getType().getDeclaredAnnotations().getOnly().getAnnotationType().represents(typeAnnotation), is(true));
        assertThat(fieldDescription.getType().getDeclaredAnnotations().getOnly().prepare(typeAnnotation).getValue(value).resolve(), is((Object) 42));
        assertThat(fieldDescription.getType().getTypeArguments().size(), is(1));
        assertThat(fieldDescription.getType().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(fieldDescription.getType().getTypeArguments().getOnly().asErasure().represents(String.class), is(true));
        assertThat(fieldDescription.getType().getTypeArguments().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(fieldDescription.getType().getTypeArguments().getOnly().getDeclaredAnnotations().getOnly().getAnnotationType().represents(typeAnnotation), is(true));
        assertThat(fieldDescription.getType().getTypeArguments().getOnly().getDeclaredAnnotations().getOnly().prepare(typeAnnotation).getValue(value).resolve(), is((Object) 84));
    }

    @Test
    @JavaVersionRule.Enforce(16)
    public void testRecordComponentsAccessor() throws Exception {
        Class<?> sampleRecord = Class.forName("net.bytebuddy.test.precompiled.v16.RecordSample");
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName("net.bytebuddy.test.precompiled.v8.TypeAnnotation");
        MethodDescription.InDefinedShape value = new MethodDescription.ForLoadedMethod(typeAnnotation.getMethod("value"));
        MethodDescription methodDescription = describe(sampleRecord).getDeclaredMethods().filter(named(FOO)).getOnly();
        assertThat(methodDescription.getDeclaredAnnotations().size(), is(1));
        assertThat(methodDescription.getDeclaredAnnotations().getOnly().getAnnotationType().represents(SampleAnnotation.class), is(true));
        assertThat(methodDescription.getReturnType().asErasure().represents(List.class), is(true));
        assertThat(methodDescription.getReturnType().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(methodDescription.getReturnType().getDeclaredAnnotations().size(), is(1));
        assertThat(methodDescription.getReturnType().getDeclaredAnnotations().getOnly().getAnnotationType().represents(typeAnnotation), is(true));
        assertThat(methodDescription.getReturnType().getDeclaredAnnotations().getOnly().prepare(typeAnnotation).getValue(value).resolve(), is((Object) 42));
        assertThat(methodDescription.getReturnType().getTypeArguments().size(), is(1));
        assertThat(methodDescription.getReturnType().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(methodDescription.getReturnType().getTypeArguments().getOnly().asErasure().represents(String.class), is(true));
        assertThat(methodDescription.getReturnType().getTypeArguments().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(methodDescription.getReturnType().getTypeArguments().getOnly().getDeclaredAnnotations().getOnly().getAnnotationType().represents(typeAnnotation), is(true));
        assertThat(methodDescription.getReturnType().getTypeArguments().getOnly().getDeclaredAnnotations().getOnly().prepare(typeAnnotation).getValue(value).resolve(), is((Object) 84));
    }

    @Test
    @JavaVersionRule.Enforce(16)
    public void testRecordComponentsConstructorParameter() throws Exception {
        Class<?> sampleRecord = Class.forName("net.bytebuddy.test.precompiled.v16.RecordSample");
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName("net.bytebuddy.test.precompiled.v8.TypeAnnotation");
        MethodDescription.InDefinedShape value = new MethodDescription.ForLoadedMethod(typeAnnotation.getMethod("value"));
        ParameterDescription parameterDescription = describe(sampleRecord).getDeclaredMethods().filter(isConstructor()).getOnly().getParameters().getOnly();
        assertThat(parameterDescription.getDeclaredAnnotations().size(), is(1));
        assertThat(parameterDescription.getDeclaredAnnotations().getOnly().getAnnotationType().represents(SampleAnnotation.class), is(true));
        assertThat(parameterDescription.getType().asErasure().represents(List.class), is(true));
        assertThat(parameterDescription.getType().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(parameterDescription.getType().getDeclaredAnnotations().size(), is(1));
        assertThat(parameterDescription.getType().getDeclaredAnnotations().getOnly().getAnnotationType().represents(typeAnnotation), is(true));
        assertThat(parameterDescription.getType().getDeclaredAnnotations().getOnly().prepare(typeAnnotation).getValue(value).resolve(), is((Object) 42));
        assertThat(parameterDescription.getType().getTypeArguments().size(), is(1));
        assertThat(parameterDescription.getType().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(parameterDescription.getType().getTypeArguments().getOnly().asErasure().represents(String.class), is(true));
        assertThat(parameterDescription.getType().getTypeArguments().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(parameterDescription.getType().getTypeArguments().getOnly().getDeclaredAnnotations().getOnly().getAnnotationType().represents(typeAnnotation), is(true));
        assertThat(parameterDescription.getType().getTypeArguments().getOnly().getDeclaredAnnotations().getOnly().prepare(typeAnnotation).getValue(value).resolve(), is((Object) 84));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testIllegalReferenceOnTypeVariableOnOutdatedJdks() throws Exception {
        Class<?> type = Class.forName("net.bytebuddy.test.precompiled.v11.ClassExtendsTypeReference");
        TypeDescription description = describe(type);
        assertThat(description.getDeclaredMethods().filter(isMethod()).getOnly().getTypeVariables().size(), is(0));
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
    public @interface SampleAnnotation {
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

        public SignatureMalformer(ClassVisitor classVisitor) {
            super(OpenedClassReader.ASM_API, classVisitor);
        }

        public static Class<?> malform(Class<?> type) throws Exception {
            AsmClassReader classReader = AsmClassReader.Factory.Default.IMPLICIT.make(ClassFileLocator.ForClassLoader.read(type));
            AsmClassWriter classWriter = AsmClassWriter.Factory.Default.IMPLICIT.make(AsmVisitorWrapper.NO_FLAGS, classReader);
            classReader.accept(new SignatureMalformer(classWriter.getVisitor()), AsmVisitorWrapper.NO_FLAGS);
            ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                    Collections.singletonMap(type.getName(), classWriter.getBinaryRepresentation()),
                    ByteArrayClassLoader.PersistenceHandler.MANIFEST);
            return classLoader.loadClass(type.getName());
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, FOO, superName, interfaces);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            return super.visitField(access, name, descriptor, FOO, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            return super.visitMethod(access, name, desc, FOO, exceptions);
        }
    }

    private static class TypeVariableMalformer extends ClassVisitor {

        public TypeVariableMalformer(ClassVisitor classVisitor) {
            super(OpenedClassReader.ASM_API, classVisitor);
        }

        public static Class<?> malform(Class<?> type) throws Exception {
            AsmClassReader classReader = AsmClassReader.Factory.Default.IMPLICIT.make(ClassFileLocator.ForClassLoader.read(type));
            AsmClassWriter classWriter = AsmClassWriter.Factory.Default.IMPLICIT.make(AsmVisitorWrapper.NO_FLAGS, classReader);
            classReader.accept(new TypeVariableMalformer(classWriter.getVisitor()), AsmVisitorWrapper.NO_FLAGS);
            ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                    Collections.singletonMap(type.getName(), classWriter.getBinaryRepresentation()),
                    ByteArrayClassLoader.PersistenceHandler.MANIFEST);
            return classLoader.loadClass(type.getName());
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (descriptor.equals(Type.getDescriptor(Object.class))) {
                signature = "TA;";
            } else if (descriptor.equals(Type.getDescriptor(Set.class))) {
                signature = "L" + Type.getInternalName(Set.class) + "<TA;>;";
            }
            return super.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            /* do nothing */
        }

        @Override
        public void visitOuterClass(String owner, String name, String descriptor) {
            /* do nothing */
        }
    }

    private static class ParameterizedTypeLengthMalformer extends ClassVisitor {

        public ParameterizedTypeLengthMalformer(ClassVisitor classVisitor) {
            super(OpenedClassReader.ASM_API, classVisitor);
        }

        public static Class<?> malform(Class<?> type) throws Exception {
            AsmClassReader classReader = AsmClassReader.Factory.Default.IMPLICIT.make(ClassFileLocator.ForClassLoader.read(type));
            AsmClassWriter classWriter = AsmClassWriter.Factory.Default.IMPLICIT.make(AsmVisitorWrapper.NO_FLAGS, classReader);
            classReader.accept(new ParameterizedTypeLengthMalformer(classWriter.getVisitor()), AsmVisitorWrapper.NO_FLAGS);
            ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                    Collections.singletonMap(type.getName(), classWriter.getBinaryRepresentation()),
                    ByteArrayClassLoader.PersistenceHandler.MANIFEST);
            return classLoader.loadClass(type.getName());
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (descriptor.equals(Type.getDescriptor(Map.class))) {
                signature = "L" + Type.getInternalName(Map.class) + "<" + Type.getDescriptor(Object.class) + ">;";
            }
            return super.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            /* do nothing */
        }

        @Override
        public void visitOuterClass(String owner, String name, String descriptor) {
            /* do nothing */
        }
    }

    @SuppressWarnings("unused")
    public abstract static class MalformedBase<T> implements Callable<T> {

        Callable<T> foo;

        abstract Callable<T> foo();
    }

    @SuppressWarnings("unused")
    public abstract static class MalformedTypeVariable {

        Object foo;
    }

    @SuppressWarnings("unused")
    public abstract static class MalformedParameterizedTypeVariable {

        Set<Object> foo;
    }

    @SuppressWarnings("unused")
    public abstract static class MalformedParameterizedLength {

        Map<Object, Object> foo;
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

        private static final long serialVersionUID = 1L;

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

    private static  <T> GenericSample<T> anonymousGenericSample() {
        return new GenericSample<T>() {
            /* empty */
        };
    }

    private static  <T> GenericSample<T> innerGenericSample() {
        class ExtendedGenericSample<T> extends GenericSample<T> {
            /* empty */
        };
        return new ExtendedGenericSample<T>();
    }

    private static class Type$With$Dollar {
        /* */
    }
}
