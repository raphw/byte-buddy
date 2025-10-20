package net.bytebuddy.description.method;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.TypeVariableSource;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.test.packaging.MethodDescriptionTestHelper;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractMethodDescriptionTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    protected Method firstMethod, secondMethod, thirdMethod, genericMethod, genericMethodWithRawException, genericMethodWithTypeVariable, nativeMethod;

    protected Constructor<?> firstConstructor, secondConstructor;

    private static int hashCode(Method method) {
        int hashCode = 17 + TypeDescription.ForLoadedType.of(method.getDeclaringClass()).hashCode();
        hashCode = 31 * hashCode + method.getName().hashCode();
        hashCode = 31 * hashCode + TypeDescription.ForLoadedType.of(method.getReturnType()).hashCode();
        return 31 * hashCode + new TypeList.ForLoadedTypes(method.getParameterTypes()).hashCode();
    }

    private static int hashCode(Constructor<?> constructor) {
        int hashCode = 17 + TypeDescription.ForLoadedType.of(constructor.getDeclaringClass()).hashCode();
        hashCode = 31 * hashCode + MethodDescription.CONSTRUCTOR_INTERNAL_NAME.hashCode();
        hashCode = 31 * hashCode + TypeDescription.ForLoadedType.of(void.class).hashCode();
        return 31 * hashCode + new TypeList.ForLoadedTypes(constructor.getParameterTypes()).hashCode();
    }

    protected abstract MethodDescription.InDefinedShape describe(Method method);

    protected abstract MethodDescription.InDefinedShape describe(Constructor<?> constructor);

    @Before
    public void setUp() throws Exception {
        firstMethod = Sample.class.getDeclaredMethod("first");
        secondMethod = Sample.class.getDeclaredMethod("second", String.class, long.class);
        thirdMethod = Sample.class.getDeclaredMethod("third", Object[].class, int[].class);
        firstConstructor = Sample.class.getDeclaredConstructor(Void.class);
        secondConstructor = Sample.class.getDeclaredConstructor(int[].class, long.class);
        genericMethod = GenericMethod.class.getDeclaredMethod("foo", Exception.class);
        genericMethodWithRawException = GenericMethod.class.getDeclaredMethod("bar", Exception.class);
        genericMethodWithTypeVariable = GenericMethod.class.getDeclaredMethod("qux");
        nativeMethod = NativeMethod.class.getDeclaredMethod("foo");
    }

    @Test
    public void testPrecondition() throws Exception {
        assertThat(describe(firstMethod), not(describe(secondMethod)));
        assertThat(describe(firstMethod), not(describe(thirdMethod)));
        assertThat(describe(firstMethod), is(describe(firstMethod)));
        assertThat(describe(secondMethod), is(describe(secondMethod)));
        assertThat(describe(thirdMethod), is(describe(thirdMethod)));
        assertThat(describe(firstMethod), is((MethodDescription) new MethodDescription.ForLoadedMethod(firstMethod)));
        assertThat(describe(secondMethod), is((MethodDescription) new MethodDescription.ForLoadedMethod(secondMethod)));
        assertThat(describe(thirdMethod), is((MethodDescription) new MethodDescription.ForLoadedMethod(thirdMethod)));
        assertThat(describe(firstConstructor), not(describe(secondConstructor)));
        assertThat(describe(firstConstructor), is(describe(firstConstructor)));
        assertThat(describe(secondConstructor), is(describe(secondConstructor)));
        assertThat(describe(firstConstructor), is((MethodDescription) new MethodDescription.ForLoadedConstructor(firstConstructor)));
        assertThat(describe(secondConstructor), is((MethodDescription) new MethodDescription.ForLoadedConstructor(secondConstructor)));
    }

    @Test
    public void testReturnType() throws Exception {
        assertThat(describe(firstMethod).getReturnType(), is((TypeDefinition) TypeDescription.ForLoadedType.of(firstMethod.getReturnType())));
        assertThat(describe(secondMethod).getReturnType(), is((TypeDefinition) TypeDescription.ForLoadedType.of(secondMethod.getReturnType())));
        assertThat(describe(thirdMethod).getReturnType(), is((TypeDefinition) TypeDescription.ForLoadedType.of(thirdMethod.getReturnType())));
        assertThat(describe(firstConstructor).getReturnType(), is(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(void.class)));
        assertThat(describe(secondConstructor).getReturnType(), is(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(void.class)));
    }

    @Test
    public void testParameterTypes() throws Exception {
        assertThat(describe(firstMethod).getParameters().asTypeList(), is((TypeList.Generic) new TypeList.Generic.ForLoadedTypes(firstMethod.getParameterTypes())));
        assertThat(describe(secondMethod).getParameters().asTypeList(), is((TypeList.Generic) new TypeList.Generic.ForLoadedTypes(secondMethod.getParameterTypes())));
        assertThat(describe(thirdMethod).getParameters().asTypeList(), is((TypeList.Generic) new TypeList.Generic.ForLoadedTypes(thirdMethod.getParameterTypes())));
        assertThat(describe(firstConstructor).getParameters().asTypeList(), is((TypeList.Generic) new TypeList.Generic.ForLoadedTypes(firstConstructor.getParameterTypes())));
        assertThat(describe(secondConstructor).getParameters().asTypeList(), is((TypeList.Generic) new TypeList.Generic.ForLoadedTypes(secondConstructor.getParameterTypes())));
    }

    @Test
    public void testMethodName() throws Exception {
        assertThat(describe(firstMethod).getName(), is(firstMethod.getName()));
        assertThat(describe(secondMethod).getName(), is(secondMethod.getName()));
        assertThat(describe(thirdMethod).getName(), is(thirdMethod.getName()));
        assertThat(describe(firstConstructor).getName(), is(firstConstructor.getDeclaringClass().getName()));
        assertThat(describe(secondConstructor).getName(), is(secondConstructor.getDeclaringClass().getName()));
        assertThat(describe(firstMethod).getInternalName(), is(firstMethod.getName()));
        assertThat(describe(secondMethod).getInternalName(), is(secondMethod.getName()));
        assertThat(describe(thirdMethod).getInternalName(), is(thirdMethod.getName()));
        assertThat(describe(firstConstructor).getInternalName(), is(MethodDescription.CONSTRUCTOR_INTERNAL_NAME));
        assertThat(describe(secondConstructor).getInternalName(), is(MethodDescription.CONSTRUCTOR_INTERNAL_NAME));
    }

    @Test
    public void testDescriptor() throws Exception {
        assertThat(describe(firstMethod).getDescriptor(), is(Type.getMethodDescriptor(firstMethod)));
        assertThat(describe(secondMethod).getDescriptor(), is(Type.getMethodDescriptor(secondMethod)));
        assertThat(describe(thirdMethod).getDescriptor(), is(Type.getMethodDescriptor(thirdMethod)));
        assertThat(describe(firstConstructor).getDescriptor(), is(Type.getConstructorDescriptor(firstConstructor)));
        assertThat(describe(secondConstructor).getDescriptor(), is(Type.getConstructorDescriptor(secondConstructor)));
    }

    @Test
    public void testMethodModifiers() throws Exception {
        assertThat(describe(firstMethod).getModifiers(), is(firstMethod.getModifiers()));
        assertThat(describe(secondMethod).getModifiers(), is(secondMethod.getModifiers()));
        assertThat(describe(thirdMethod).getModifiers(), is(thirdMethod.getModifiers()));
        assertThat(describe(firstConstructor).getModifiers(), is(firstConstructor.getModifiers()));
        assertThat(describe(secondConstructor).getModifiers(), is(secondConstructor.getModifiers()));
    }

    @Test
    @SuppressWarnings("cast")
    public void testMethodDeclaringType() throws Exception {
        assertThat(describe(firstMethod).getDeclaringType(), is((TypeDescription) TypeDescription.ForLoadedType.of(firstMethod.getDeclaringClass())));
        assertThat(describe(secondMethod).getDeclaringType(), is((TypeDescription) TypeDescription.ForLoadedType.of(secondMethod.getDeclaringClass())));
        assertThat(describe(thirdMethod).getDeclaringType(), is((TypeDescription) TypeDescription.ForLoadedType.of(thirdMethod.getDeclaringClass())));
        assertThat(describe(firstConstructor).getDeclaringType(), is((TypeDescription) TypeDescription.ForLoadedType.of(firstConstructor.getDeclaringClass())));
        assertThat(describe(secondConstructor).getDeclaringType(), is((TypeDescription) TypeDescription.ForLoadedType.of(secondConstructor.getDeclaringClass())));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(describe(firstMethod).hashCode(), is(hashCode(firstMethod)));
        assertThat(describe(secondMethod).hashCode(), is(hashCode(secondMethod)));
        assertThat(describe(thirdMethod).hashCode(), is(hashCode(thirdMethod)));
        assertThat(describe(firstMethod).hashCode(), not(hashCode(secondMethod)));
        assertThat(describe(firstMethod).hashCode(), not(hashCode(thirdMethod)));
        assertThat(describe(firstMethod).hashCode(), not(hashCode(firstConstructor)));
        assertThat(describe(firstMethod).hashCode(), not(hashCode(secondConstructor)));
        assertThat(describe(firstConstructor).hashCode(), is(hashCode(firstConstructor)));
        assertThat(describe(secondConstructor).hashCode(), is(hashCode(secondConstructor)));
        assertThat(describe(firstConstructor).hashCode(), not(hashCode(firstMethod)));
        assertThat(describe(firstConstructor).hashCode(), not(hashCode(secondMethod)));
        assertThat(describe(firstConstructor).hashCode(), not(hashCode(thirdMethod)));
        assertThat(describe(firstConstructor).hashCode(), not(hashCode(secondConstructor)));
    }

    @Test
    public void testEqualsMethod() throws Exception {
        MethodDescription identical = describe(firstMethod);
        assertThat(identical, is(identical));
        assertThat(describe(firstMethod), is(describe(firstMethod)));
        assertThat(describe(firstMethod), not(describe(secondMethod)));
        assertThat(describe(firstMethod), not(describe(thirdMethod)));
        assertThat(describe(firstMethod), not(describe(firstConstructor)));
        assertThat(describe(firstMethod), not(describe(secondConstructor)));
        assertThat(describe(firstMethod), is((MethodDescription) new MethodDescription.ForLoadedMethod(firstMethod)));
        assertThat(describe(firstMethod), not((MethodDescription) new MethodDescription.ForLoadedMethod(secondMethod)));
        assertThat(describe(firstMethod), not((MethodDescription) new MethodDescription.ForLoadedMethod(thirdMethod)));
        assertThat(describe(firstMethod), not((MethodDescription) new MethodDescription.ForLoadedConstructor(firstConstructor)));
        assertThat(describe(firstMethod), not((MethodDescription) new MethodDescription.ForLoadedConstructor(secondConstructor)));
        MethodDescription.InDefinedShape equalMethod = mock(MethodDescription.InDefinedShape.class);
        when(equalMethod.getInternalName()).thenReturn(firstMethod.getName());
        when(equalMethod.getDeclaringType()).thenReturn(TypeDescription.ForLoadedType.of(firstMethod.getDeclaringClass()));
        when(equalMethod.getReturnType()).thenReturn(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(firstMethod.getReturnType()));
        when(equalMethod.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(equalMethod,
                new TypeList.Generic.ForLoadedTypes(firstMethod.getParameterTypes())));
        assertThat(describe(firstMethod), is(equalMethod));
        MethodDescription.InDefinedShape equalMethodButName = mock(MethodDescription.InDefinedShape.class);
        when(equalMethodButName.getInternalName()).thenReturn(secondMethod.getName());
        when(equalMethodButName.getDeclaringType()).thenReturn(TypeDescription.ForLoadedType.of(firstMethod.getDeclaringClass()));
        when(equalMethodButName.getReturnType()).thenReturn(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(firstMethod.getReturnType()));
        when(equalMethodButName.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(equalMethodButName,
                new TypeList.Generic.ForLoadedTypes(firstMethod.getParameterTypes())));
        assertThat(describe(firstMethod), not(equalMethodButName));
        MethodDescription.InDefinedShape equalMethodButReturnType = mock(MethodDescription.InDefinedShape.class);
        when(equalMethodButReturnType.getInternalName()).thenReturn(firstMethod.getName());
        when(equalMethodButReturnType.getDeclaringType()).thenReturn(TypeDescription.ForLoadedType.of(Object.class));
        when(equalMethodButReturnType.getReturnType()).thenReturn(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(firstMethod.getReturnType()));
        when(equalMethodButReturnType.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(equalMethodButReturnType,
                new TypeList.Generic.ForLoadedTypes(firstMethod.getParameterTypes())));
        assertThat(describe(firstMethod), not(equalMethodButReturnType));
        MethodDescription.InDefinedShape equalMethodButDeclaringType = mock(MethodDescription.InDefinedShape.class);
        when(equalMethodButDeclaringType.getInternalName()).thenReturn(firstMethod.getName());
        when(equalMethodButDeclaringType.getDeclaringType()).thenReturn(TypeDescription.ForLoadedType.of(firstMethod.getDeclaringClass()));
        when(equalMethodButDeclaringType.getReturnType()).thenReturn(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(secondMethod.getReturnType()));
        when(equalMethodButDeclaringType.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(equalMethodButDeclaringType,
                new TypeList.Generic.ForLoadedTypes(firstMethod.getParameterTypes())));
        assertThat(describe(firstMethod), not(equalMethodButDeclaringType));
        MethodDescription.InDefinedShape equalMethodButParameterTypes = mock(MethodDescription.InDefinedShape.class);
        when(equalMethodButParameterTypes.getInternalName()).thenReturn(firstMethod.getName());
        when(equalMethodButParameterTypes.getDeclaringType()).thenReturn(TypeDescription.ForLoadedType.of(firstMethod.getDeclaringClass()));
        when(equalMethodButParameterTypes.getReturnType()).thenReturn(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(firstMethod.getReturnType()));
        when(equalMethodButParameterTypes.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(equalMethodButParameterTypes,
                new TypeList.Generic.ForLoadedTypes(secondMethod.getParameterTypes())));
        assertThat(describe(firstMethod), not(equalMethodButParameterTypes));
        assertThat(describe(firstMethod), not(new Object()));
        assertThat(describe(firstMethod), not(equalTo(null)));
    }

    @Test
    public void testEqualsConstructor() throws Exception {
        MethodDescription identical = describe(firstConstructor);
        assertThat(identical, is(identical));
        assertThat(describe(firstConstructor), is(describe(firstConstructor)));
        assertThat(describe(firstConstructor), not(describe(secondConstructor)));
        assertThat(describe(firstConstructor), not(describe(firstMethod)));
        assertThat(describe(firstConstructor), not(describe(secondMethod)));
        assertThat(describe(firstConstructor), not(describe(thirdMethod)));
        assertThat(describe(firstConstructor), is((MethodDescription) new MethodDescription.ForLoadedConstructor(firstConstructor)));
        assertThat(describe(firstConstructor), not((MethodDescription) new MethodDescription.ForLoadedConstructor(secondConstructor)));
        assertThat(describe(firstConstructor), not((MethodDescription) new MethodDescription.ForLoadedMethod(firstMethod)));
        assertThat(describe(firstConstructor), not((MethodDescription) new MethodDescription.ForLoadedMethod(secondMethod)));
        assertThat(describe(firstConstructor), not((MethodDescription) new MethodDescription.ForLoadedMethod(thirdMethod)));
        MethodDescription.InDefinedShape equalMethod = mock(MethodDescription.InDefinedShape.class);
        when(equalMethod.getInternalName()).thenReturn(MethodDescription.CONSTRUCTOR_INTERNAL_NAME);
        when(equalMethod.getDeclaringType()).thenReturn(TypeDescription.ForLoadedType.of(firstConstructor.getDeclaringClass()));
        when(equalMethod.getReturnType()).thenReturn(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(void.class));
        when(equalMethod.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(equalMethod,
                new TypeList.ForLoadedTypes(firstConstructor.getParameterTypes())));
        assertThat(describe(firstConstructor), is(equalMethod));
        MethodDescription.InDefinedShape equalMethodButName = mock(MethodDescription.InDefinedShape.class);
        when(equalMethodButName.getInternalName()).thenReturn(firstMethod.getName());
        when(equalMethodButName.getDeclaringType()).thenReturn(TypeDescription.ForLoadedType.of(firstConstructor.getDeclaringClass()));
        when(equalMethodButName.getReturnType()).thenReturn(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(void.class));
        when(equalMethodButName.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(equalMethodButName,
                new TypeList.ForLoadedTypes(firstConstructor.getParameterTypes())));
        assertThat(describe(firstConstructor), not(equalMethodButName));
        MethodDescription.InDefinedShape equalMethodButReturnType = mock(MethodDescription.InDefinedShape.class);
        when(equalMethodButReturnType.getInternalName()).thenReturn(MethodDescription.CONSTRUCTOR_INTERNAL_NAME);
        when(equalMethodButReturnType.getDeclaringType()).thenReturn(TypeDescription.ForLoadedType.of(Object.class));
        when(equalMethodButReturnType.getReturnType()).thenReturn(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class));
        when(equalMethodButReturnType.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(equalMethodButReturnType,
                new TypeList.ForLoadedTypes(firstConstructor.getParameterTypes())));
        assertThat(describe(firstConstructor), not(equalMethodButReturnType));
        MethodDescription.InDefinedShape equalMethodButDeclaringType = mock(MethodDescription.InDefinedShape.class);
        when(equalMethodButDeclaringType.getInternalName()).thenReturn(MethodDescription.CONSTRUCTOR_INTERNAL_NAME);
        when(equalMethodButDeclaringType.getDeclaringType()).thenReturn(TypeDescription.ForLoadedType.of(Object.class));
        when(equalMethodButDeclaringType.getReturnType()).thenReturn(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(void.class));
        when(equalMethodButDeclaringType.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(equalMethodButDeclaringType,
                new TypeList.ForLoadedTypes(firstConstructor.getParameterTypes())));
        assertThat(describe(firstConstructor), not(equalMethodButDeclaringType));
        MethodDescription.InDefinedShape equalMethodButParameterTypes = mock(MethodDescription.InDefinedShape.class);
        when(equalMethodButParameterTypes.getInternalName()).thenReturn(MethodDescription.CONSTRUCTOR_INTERNAL_NAME);
        when(equalMethodButParameterTypes.getDeclaringType()).thenReturn(TypeDescription.ForLoadedType.of(firstConstructor.getDeclaringClass()));
        when(equalMethodButParameterTypes.getReturnType()).thenReturn(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(void.class));
        when(equalMethodButParameterTypes.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(equalMethodButParameterTypes,
                new TypeList.ForLoadedTypes(secondConstructor.getParameterTypes())));
        assertThat(describe(firstConstructor), not(equalMethodButParameterTypes));
        assertThat(describe(firstConstructor), not(new Object()));
        assertThat(describe(firstConstructor), not(equalTo(null)));
    }

    @Test
    public void testToString() throws Exception {
        assertThat(describe(firstMethod).toString(), is(firstMethod.toString()));
        assertThat(describe(secondMethod).toString(), is(secondMethod.toString()));
        assertThat(describe(thirdMethod).toString(), is(thirdMethod.toString()));
        assertThat(describe(firstConstructor).toString(), is(firstConstructor.toString()));
        assertThat(describe(secondConstructor).toString(), is(secondConstructor.toString()));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testEqualsParameter() throws Exception {
        ParameterDescription identical = describe(secondMethod).getParameters().get(0);
        assertThat(identical, is(identical));
        assertThat(identical, not(new Object()));
        assertThat(identical, not(equalTo(null)));
        assertThat(describe(secondMethod).getParameters().get(0), is(describe(secondMethod).getParameters().get(0)));
        ParameterDescription equal = mock(ParameterDescription.class);
        when(equal.getDeclaringMethod()).thenReturn(describe(secondMethod));
        when(equal.getIndex()).thenReturn(0);
        assertThat(describe(secondMethod).getParameters().get(0), is(equal));
        ParameterDescription notEqualMethod = mock(ParameterDescription.class);
        when(equal.getDeclaringMethod()).thenReturn(mock(MethodDescription.class));
        when(equal.getIndex()).thenReturn(0);
        assertThat(describe(secondMethod).getParameters().get(0), not(notEqualMethod));
        ParameterDescription notEqualMethodIndex = mock(ParameterDescription.class);
        when(equal.getDeclaringMethod()).thenReturn(describe(secondMethod));
        when(equal.getIndex()).thenReturn(1);
        assertThat(describe(secondMethod).getParameters().get(0), not(notEqualMethodIndex));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testHashCodeParameter() throws Exception {
        assertThat(describe(secondMethod).getParameters().get(0).hashCode(), is(hashCode(secondMethod, 0)));
        assertThat(describe(secondMethod).getParameters().get(1).hashCode(), is(hashCode(secondMethod, 1)));
        assertThat(describe(firstConstructor).getParameters().get(0).hashCode(), is(hashCode(firstConstructor, 0)));
    }

    private int hashCode(Method method, int index) {
        return hashCode(method) ^ index;
    }

    private int hashCode(Constructor<?> constructor, int index) {
        return hashCode(constructor) ^ index;
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testToStringParameter() throws Exception {
        Class<?> executable = Class.forName("java.lang.reflect.Executable");
        Method method = executable.getDeclaredMethod("getParameters");
        assertThat(describe(secondMethod).getParameters().get(0).toString(), is(((Object[]) method.invoke(secondMethod))[0].toString()));
        assertThat(describe(secondMethod).getParameters().get(1).toString(), is(((Object[]) method.invoke(secondMethod))[1].toString()));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testParameterNameAndModifiers() throws Exception {
        Class<?> type = Class.forName("net.bytebuddy.test.precompiled.v8parameters.ParameterNames");
        assertThat(describe(type.getDeclaredMethod("foo", String.class, long.class, int.class)).getParameters().get(0).isNamed(), is(true));
        assertThat(describe(type.getDeclaredMethod("foo", String.class, long.class, int.class)).getParameters().get(1).isNamed(), is(true));
        assertThat(describe(type.getDeclaredMethod("foo", String.class, long.class, int.class)).getParameters().get(2).isNamed(), is(true));
        assertThat(describe(type.getDeclaredMethod("foo", String.class, long.class, int.class)).getParameters().get(0).getName(), is("first"));
        assertThat(describe(type.getDeclaredMethod("foo", String.class, long.class, int.class)).getParameters().get(1).getName(), is("second"));
        assertThat(describe(type.getDeclaredMethod("foo", String.class, long.class, int.class)).getParameters().get(2).getName(), is("third"));
        assertThat(describe(type.getDeclaredMethod("foo", String.class, long.class, int.class)).getParameters().get(0).getModifiers(), is(Opcodes.ACC_FINAL));
        assertThat(describe(type.getDeclaredMethod("foo", String.class, long.class, int.class)).getParameters().get(1).getModifiers(), is(0));
        assertThat(describe(type.getDeclaredMethod("foo", String.class, long.class, int.class)).getParameters().get(2).getModifiers(), is(0));
        assertThat(describe(type.getDeclaredMethod("bar", String.class, long.class, int.class)).getParameters().get(0).isNamed(), is(true));
        assertThat(describe(type.getDeclaredMethod("bar", String.class, long.class, int.class)).getParameters().get(1).isNamed(), is(true));
        assertThat(describe(type.getDeclaredMethod("bar", String.class, long.class, int.class)).getParameters().get(2).isNamed(), is(true));
        assertThat(describe(type.getDeclaredMethod("bar", String.class, long.class, int.class)).getParameters().get(0).getName(), is("first"));
        assertThat(describe(type.getDeclaredMethod("bar", String.class, long.class, int.class)).getParameters().get(1).getName(), is("second"));
        assertThat(describe(type.getDeclaredMethod("bar", String.class, long.class, int.class)).getParameters().get(2).getName(), is("third"));
        assertThat(describe(type.getDeclaredMethod("bar", String.class, long.class, int.class)).getParameters().get(0).getModifiers(), is(0));
        assertThat(describe(type.getDeclaredMethod("bar", String.class, long.class, int.class)).getParameters().get(1).getModifiers(), is(Opcodes.ACC_FINAL));
        assertThat(describe(type.getDeclaredMethod("bar", String.class, long.class, int.class)).getParameters().get(2).getModifiers(), is(0));
        assertThat(describe(type.getDeclaredConstructor(String.class, int.class)).getParameters().get(0).isNamed(), is(true));
        assertThat(describe(type.getDeclaredConstructor(String.class, int.class)).getParameters().get(1).isNamed(), is(true));
        assertThat(describe(type.getDeclaredConstructor(String.class, int.class)).getParameters().get(0).getName(), is("first"));
        assertThat(describe(type.getDeclaredConstructor(String.class, int.class)).getParameters().get(1).getName(), is("second"));
        assertThat(describe(type.getDeclaredConstructor(String.class, int.class)).getParameters().get(0).getModifiers(), is(0));
        assertThat(describe(type.getDeclaredConstructor(String.class, int.class)).getParameters().get(1).getModifiers(), is(Opcodes.ACC_FINAL));
    }

    @Test
    public void testNoParameterNameAndModifiers() throws Exception {
        assertThat(describe(secondMethod).getParameters().get(0).isNamed(), is(false));
        assertThat(describe(secondMethod).getParameters().get(1).isNamed(), is(false));
        assertThat(describe(secondMethod).getParameters().get(0).getName(), is("arg0"));
        assertThat(describe(secondMethod).getParameters().get(1).getName(), is("arg1"));
        assertThat(describe(secondMethod).getParameters().get(0).getModifiers(), is(0));
        assertThat(describe(secondMethod).getParameters().get(1).getModifiers(), is(0));
        assertThat(describe(firstConstructor).getParameters().get(0).isNamed(), is(canReadDebugInformation()));
        assertThat(describe(firstConstructor).getParameters().get(0).getName(), is(canReadDebugInformation() ? "argument" : "arg0"));
        assertThat(describe(firstConstructor).getParameters().get(0).getModifiers(), is(0));
    }

    protected abstract boolean canReadDebugInformation();

    @Test
    public void testSynthetic() throws Exception {
        assertThat(describe(firstMethod).isSynthetic(), is(firstMethod.isSynthetic()));
        assertThat(describe(secondMethod).isSynthetic(), is(secondMethod.isSynthetic()));
        assertThat(describe(thirdMethod).isSynthetic(), is(thirdMethod.isSynthetic()));
        assertThat(describe(firstConstructor).isSynthetic(), is(firstConstructor.isSynthetic()));
        assertThat(describe(secondConstructor).isSynthetic(), is(secondConstructor.isSynthetic()));
    }

    @Test
    public void testType() throws Exception {
        assertThat(describe(firstMethod).isMethod(), is(true));
        assertThat(describe(firstMethod).isConstructor(), is(false));
        assertThat(describe(firstMethod).isTypeInitializer(), is(false));
        assertThat(describe(firstConstructor).isMethod(), is(false));
        assertThat(describe(firstConstructor).isConstructor(), is(true));
        assertThat(describe(firstConstructor).isTypeInitializer(), is(false));
    }

    @Test
    public void testMethodIsVisibleTo() throws Exception {
        assertThat(describe(PublicType.class.getDeclaredMethod("publicMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("protectedMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("packagePrivateMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("privateMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("publicMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Sample.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("protectedMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Sample.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("packagePrivateMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Sample.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("privateMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Sample.class)), is(ClassFileVersion.of(Sample.class).isAtLeast(ClassFileVersion.JAVA_V11)));
        assertThat(describe(PublicType.class.getDeclaredMethod("publicMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("protectedMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredMethod("packagePrivateMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredMethod("privateMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredMethod("publicMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(MethodDescriptionTestHelper.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("protectedMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(MethodDescriptionTestHelper.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("packagePrivateMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(MethodDescriptionTestHelper.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredMethod("privateMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(MethodDescriptionTestHelper.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredMethod("publicMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredMethod("protectedMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredMethod("packagePrivateMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredMethod("privateMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredMethod("staticPublicMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredMethod("staticProtectedMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredMethod("staticPackagePrivateMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredMethod("staticPrivateMethod"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
    }

    @Test
    public void testConstructorIsVisibleTo() throws Exception {
        assertThat(describe(PublicType.class.getDeclaredConstructor())
                .isVisibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor(Void.class))
                .isVisibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor(Object.class))
                .isVisibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor(String.class))
                .isVisibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor())
                .isVisibleTo(TypeDescription.ForLoadedType.of(Sample.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor(Void.class))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Sample.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor(Object.class))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Sample.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor(String.class))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Sample.class)), is(ClassFileVersion.of(Sample.class).isAtLeast(ClassFileVersion.JAVA_V11)));
        assertThat(describe(PublicType.class.getDeclaredConstructor())
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor(Void.class))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredConstructor(Object.class))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredConstructor(String.class))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredConstructor())
                .isVisibleTo(TypeDescription.ForLoadedType.of(MethodDescriptionTestHelper.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor(Void.class))
                .isVisibleTo(TypeDescription.ForLoadedType.of(MethodDescriptionTestHelper.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor(Object.class))
                .isVisibleTo(TypeDescription.ForLoadedType.of(MethodDescriptionTestHelper.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredConstructor(String.class))
                .isVisibleTo(TypeDescription.ForLoadedType.of(MethodDescriptionTestHelper.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredConstructor())
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredConstructor(Void.class))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredConstructor(Object.class))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredConstructor(String.class))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(MethodVisibilityType.class.getDeclaredMethod("packagePrivateArgument", PackagePrivateType.class))
                .isVisibleTo(TypeDescription.ForLoadedType.of(MethodVisibilityType.class)), is(true));
        assertThat(describe(MethodVisibilityType.class.getDeclaredMethod("packagePrivateReturnType"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(MethodVisibilityType.class)), is(true));
        assertThat(describe(MethodVisibilityType.class.getDeclaredMethod("packagePrivateArgument", PackagePrivateType.class))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(true));
        assertThat(describe(MethodVisibilityType.class.getDeclaredMethod("packagePrivateReturnType"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(true));
    }

    @Test
    public void testMethodIsAccessibleTo() throws Exception {
        assertThat(describe(PublicType.class.getDeclaredMethod("publicMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("protectedMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("packagePrivateMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("privateMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("publicMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Sample.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("protectedMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Sample.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("packagePrivateMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Sample.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("privateMethod"))
                        .isAccessibleTo(TypeDescription.ForLoadedType.of(Sample.class)),
                is(ClassFileVersion.of(PublicType.class).isAtLeast(ClassFileVersion.JAVA_V11))); // introduction of nest mates
        assertThat(describe(PublicType.class.getDeclaredMethod("publicMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("protectedMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredMethod("packagePrivateMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredMethod("privateMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredMethod("publicMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(MethodDescriptionTestHelper.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredMethod("protectedMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(MethodDescriptionTestHelper.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredMethod("packagePrivateMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(MethodDescriptionTestHelper.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredMethod("privateMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(MethodDescriptionTestHelper.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredMethod("publicMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredMethod("protectedMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredMethod("packagePrivateMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredMethod("privateMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredMethod("staticPublicMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredMethod("staticProtectedMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredMethod("staticPackagePrivateMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredMethod("staticPrivateMethod"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
    }

    @Test
    public void testConstructorIsAccessibleTo() throws Exception {
        assertThat(describe(PublicType.class.getDeclaredConstructor())
                .isAccessibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor(Void.class))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor(Object.class))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor(String.class))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor())
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Sample.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor(Void.class))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Sample.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor(Object.class))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Sample.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor(String.class))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(AbstractMethodDescriptionTestNoNestMate.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredConstructor())
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor(Void.class))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredConstructor(Object.class))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredConstructor(String.class))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredConstructor())
                .isAccessibleTo(TypeDescription.ForLoadedType.of(MethodDescriptionTestHelper.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredConstructor(Void.class))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(MethodDescriptionTestHelper.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredConstructor(Object.class))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(MethodDescriptionTestHelper.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredConstructor(String.class))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(MethodDescriptionTestHelper.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredConstructor())
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredConstructor(Void.class))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredConstructor(Object.class))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredConstructor(String.class))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(MethodVisibilityType.class.getDeclaredMethod("packagePrivateArgument", PackagePrivateType.class))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(MethodVisibilityType.class)), is(true));
        assertThat(describe(MethodVisibilityType.class.getDeclaredMethod("packagePrivateReturnType"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(MethodVisibilityType.class)), is(true));
        assertThat(describe(MethodVisibilityType.class.getDeclaredMethod("packagePrivateArgument", PackagePrivateType.class))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(true));
        assertThat(describe(MethodVisibilityType.class.getDeclaredMethod("packagePrivateReturnType"))
                .isAccessibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(true));
    }

    @Test
    public void testExceptions() throws Exception {
        TypeList.Generic firstMethodExecptions = describe(firstMethod).getExceptionTypes();
        TypeList.Generic firstMethodLoaded = (TypeList.Generic) new TypeList.Generic.ForLoadedTypes(firstMethod.getExceptionTypes());
        assertTrue(firstMethodExecptions.size() == firstMethodLoaded.size()
                && firstMethodExecptions.containsAll(firstMethodLoaded)
                && firstMethodLoaded.containsAll(firstMethodExecptions)
        );
        TypeList.Generic secondMethodExecptions = describe(secondMethod).getExceptionTypes();
        TypeList.Generic secondMethodLoaded = (TypeList.Generic) new TypeList.Generic.ForLoadedTypes(secondMethod.getExceptionTypes());
        assertTrue(secondMethodExecptions.size() == secondMethodLoaded.size()
                && secondMethodExecptions.containsAll(secondMethodLoaded)
                && secondMethodLoaded.containsAll(secondMethodExecptions)
        );
        TypeList.Generic thirdMethodExecptions = describe(thirdMethod).getExceptionTypes();
        TypeList.Generic thirdMethodLoaded = (TypeList.Generic) new TypeList.Generic.ForLoadedTypes(thirdMethod.getExceptionTypes());
        assertTrue(thirdMethodExecptions.size() == thirdMethodLoaded.size()
                && thirdMethodExecptions.containsAll(thirdMethodLoaded)
                && thirdMethodLoaded.containsAll(thirdMethodExecptions)
        );
        TypeList.Generic firstConstructorExecptions = describe(firstConstructor).getExceptionTypes();
        TypeList.Generic firstConstructorLoaded = (TypeList.Generic) new TypeList.Generic.ForLoadedTypes(firstConstructor.getExceptionTypes());
        assertTrue(firstConstructorExecptions.size() == firstConstructorLoaded.size()
                && firstConstructorExecptions.containsAll(firstConstructorLoaded)
                && firstConstructorLoaded.containsAll(firstConstructorExecptions)
        );
        TypeList.Generic secondConstructorExecptions = describe(secondConstructor).getExceptionTypes();
        TypeList.Generic secondConstructorLoaded = (TypeList.Generic) new TypeList.Generic.ForLoadedTypes(secondConstructor.getExceptionTypes());
        assertTrue(secondConstructorExecptions.size() == secondConstructorLoaded.size()
                && secondConstructorExecptions.containsAll(secondConstructorLoaded)
                && secondConstructorLoaded.containsAll(secondConstructorExecptions)
        );
    }

    @Test
    public void testAnnotations() throws Exception {
        assertThat(describe(firstMethod).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.Empty()));
        assertThat(describe(secondMethod).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.Empty()));
        assertThat(describe(thirdMethod).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.ForLoadedAnnotations(thirdMethod.getDeclaredAnnotations())));
        assertThat(describe(firstConstructor).getDeclaredAnnotations(), is((AnnotationList) new AnnotationList.Empty()));
        assertThat(describe(secondConstructor).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.ForLoadedAnnotations(secondConstructor.getDeclaredAnnotations())));
    }

    @Test
    public void testParameterAnnotations() throws Exception {
        assertThat(describe(secondMethod).getParameters().get(0).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.Empty()));
        assertThat(describe(secondMethod).getParameters().get(1).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.Empty()));
        assertThat(describe(thirdMethod).getParameters().get(0).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.ForLoadedAnnotations(thirdMethod.getParameterAnnotations()[0])));
        assertThat(describe(thirdMethod).getParameters().get(1).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.ForLoadedAnnotations(thirdMethod.getParameterAnnotations()[1])));
        assertThat(describe(firstConstructor).getParameters().get(0).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.Empty()));
        assertThat(describe(secondConstructor).getParameters().get(0).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.ForLoadedAnnotations(secondConstructor.getParameterAnnotations()[0])));
        assertThat(describe(secondConstructor).getParameters().get(1).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.ForLoadedAnnotations(secondConstructor.getParameterAnnotations()[1])));
    }

    @Test
    public void testRepresents() throws Exception {
        assertThat(describe(firstMethod).represents(firstMethod), is(true));
        assertThat(describe(firstMethod).represents(secondMethod), is(false));
        assertThat(describe(firstMethod).represents(thirdMethod), is(false));
        assertThat(describe(firstMethod).represents(firstConstructor), is(false));
        assertThat(describe(firstMethod).represents(secondConstructor), is(false));
        assertThat(describe(firstConstructor).represents(firstConstructor), is(true));
        assertThat(describe(firstConstructor).represents(secondConstructor), is(false));
        assertThat(describe(firstConstructor).represents(firstMethod), is(false));
        assertThat(describe(firstConstructor).represents(secondMethod), is(false));
        assertThat(describe(firstConstructor).represents(thirdMethod), is(false));
    }

    @Test
    public void testSpecializable() throws Exception {
        assertThat(describe(firstMethod).isSpecializableFor(TypeDescription.ForLoadedType.of(Sample.class)), is(false));
        assertThat(describe(secondMethod).isSpecializableFor(TypeDescription.ForLoadedType.of(Sample.class)), is(false));
        assertThat(describe(thirdMethod).isSpecializableFor(TypeDescription.ForLoadedType.of(Sample.class)), is(true));
        assertThat(describe(thirdMethod).isSpecializableFor(TypeDescription.ForLoadedType.of(SampleSub.class)), is(true));
        assertThat(describe(thirdMethod).isSpecializableFor(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(firstConstructor).isSpecializableFor(TypeDescription.ForLoadedType.of(Sample.class)), is(true));
        assertThat(describe(firstConstructor).isSpecializableFor(TypeDescription.ForLoadedType.of(SampleSub.class)), is(false));
    }

    @Test
    public void testInvokable() throws Exception {
        assertThat(describe(firstMethod).isInvokableOn(TypeDescription.ForLoadedType.of(Sample.class)), is(false));
        assertThat(describe(secondMethod).isInvokableOn(TypeDescription.ForLoadedType.of(Sample.class)), is(true));
        assertThat(describe(secondMethod).isInvokableOn(TypeDescription.ForLoadedType.of(SampleSub.class)), is(true));
        assertThat(describe(secondMethod).isInvokableOn(TypeDescription.ForLoadedType.of(Object.class)), is(false));
    }

    @Test
    public void testGenericTypes() throws Exception {
        assertThat(describe(genericMethod).getReturnType(), is(TypeDefinition.Sort.describe(genericMethod.getGenericReturnType())));
        assertThat(describe(genericMethod).getParameters().asTypeList(),
                is((TypeList.Generic) new TypeList.Generic.ForLoadedTypes(genericMethod.getGenericParameterTypes())));
        assertThat(describe(genericMethod).getExceptionTypes(),
                is((TypeList.Generic) new TypeList.Generic.ForLoadedTypes(genericMethod.getGenericExceptionTypes())));
    }

    @Test
    public void testGenericTypesOfMethodWithoutException() throws Exception {
        assertThat(describe(genericMethodWithRawException).getReturnType(),
                is(TypeDefinition.Sort.describe(genericMethodWithRawException.getGenericReturnType())));
        assertThat(describe(genericMethodWithRawException).getParameters().asTypeList(),
                is((TypeList.Generic) new TypeList.Generic.ForLoadedTypes(genericMethodWithRawException.getGenericParameterTypes())));
        assertThat(describe(genericMethodWithRawException).getExceptionTypes(),
                is((TypeList.Generic) new TypeList.Generic.ForLoadedTypes(genericMethodWithRawException.getGenericExceptionTypes())));
    }

    @Test
    public void testToGenericString() throws Exception {
        assertThat(describe(genericMethod).toGenericString(), is(genericMethod.toGenericString()));
    }

    @Test
    public void testEnclosingSource() throws Exception {
        assertThat(describe(firstMethod).getEnclosingSource(), nullValue(TypeVariableSource.class));
        assertThat(describe(secondMethod).getEnclosingSource(), is((TypeVariableSource) TypeDescription.ForLoadedType.of(Sample.class)));
    }

    @Test
    public void testIsGenerified() throws Exception {
        assertThat(describe(genericMethodWithTypeVariable).isGenerified(), is(true));
        assertThat(describe(firstMethod).isGenerified(), is(false));
    }

    @Test
    public void testImplicitReceiverTypes() throws Exception {
        assertThat(describe(firstMethod).getReceiverType(), nullValue(TypeDescription.Generic.class));
        assertThat(describe(secondMethod).getReceiverType(), is(TypeDefinition.Sort.describe(Sample.class)));
        assertThat(describe(firstConstructor).getReceiverType(), is(TypeDefinition.Sort.describe(AbstractMethodDescriptionTest.class)));
        assertThat(describe(AbstractMethodDescriptionTest.class.getDeclaredConstructor()).getReceiverType(),
                is(TypeDefinition.Sort.describe(AbstractMethodDescriptionTest.class)));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGetActualModifiers() throws Exception {
        assertThat(describe(firstMethod).getActualModifiers(), is(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC));
        assertThat(describe(firstMethod).getActualModifiers(true), is(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC));
        assertThat(describe(firstMethod).getActualModifiers(false), is(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT));
        assertThat(describe(DeprecationSample.class.getDeclaredMethod("foo")).getActualModifiers(), is(Opcodes.ACC_PRIVATE | Opcodes.ACC_DEPRECATED));
        assertThat(describe(firstMethod).getActualModifiers(true, Visibility.PUBLIC), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC));
        assertThat(describe(secondMethod).getActualModifiers(false, Visibility.PRIVATE), is(Opcodes.ACC_PROTECTED | Opcodes.ACC_ABSTRACT));
        assertThat(describe(nativeMethod).getActualModifiers(false, Visibility.PRIVATE), is(Opcodes.ACC_NATIVE));
        assertThat(describe(nativeMethod).getActualModifiers(true, Visibility.PRIVATE), is(0));
    }

    @Test
    public void testBridgeCompatible() throws Exception {
        assertThat(describe(firstMethod).isBridgeCompatible(new MethodDescription.TypeToken(TypeDescription.ForLoadedType.of(void.class), Collections.<TypeDescription>emptyList())), is(true));
        assertThat(describe(firstMethod).isBridgeCompatible(new MethodDescription.TypeToken(TypeDescription.ForLoadedType.of(void.class), Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)))), is(false));
        assertThat(describe(firstMethod).isBridgeCompatible(new MethodDescription.TypeToken(TypeDescription.ForLoadedType.of(Object.class), Collections.<TypeDescription>emptyList())), is(false));
        assertThat(describe(firstMethod).isBridgeCompatible(new MethodDescription.TypeToken(TypeDescription.ForLoadedType.of(int.class), Collections.<TypeDescription>emptyList())), is(false));
        assertThat(describe(secondMethod).isBridgeCompatible(new MethodDescription.TypeToken(TypeDescription.ForLoadedType.of(Object.class),
                Arrays.asList(TypeDescription.ForLoadedType.of(String.class), TypeDescription.ForLoadedType.of(long.class)))), is(true));
        assertThat(describe(secondMethod).isBridgeCompatible(new MethodDescription.TypeToken(TypeDescription.ForLoadedType.of(Object.class),
                Arrays.asList(TypeDescription.ForLoadedType.of(Object.class), TypeDescription.ForLoadedType.of(long.class)))), is(true));
        assertThat(describe(secondMethod).isBridgeCompatible(new MethodDescription.TypeToken(TypeDescription.ForLoadedType.of(String.class),
                Arrays.asList(TypeDescription.ForLoadedType.of(Object.class), TypeDescription.ForLoadedType.of(long.class)))), is(true));
        assertThat(describe(secondMethod).isBridgeCompatible(new MethodDescription.TypeToken(TypeDescription.ForLoadedType.of(void.class),
                Arrays.asList(TypeDescription.ForLoadedType.of(String.class), TypeDescription.ForLoadedType.of(long.class)))), is(false));
        assertThat(describe(secondMethod).isBridgeCompatible(new MethodDescription.TypeToken(TypeDescription.ForLoadedType.of(Object.class),
                Arrays.asList(TypeDescription.ForLoadedType.of(int.class), TypeDescription.ForLoadedType.of(long.class)))), is(false));
        assertThat(describe(secondMethod).isBridgeCompatible(new MethodDescription.TypeToken(TypeDescription.ForLoadedType.of(Object.class),
                Arrays.asList(TypeDescription.ForLoadedType.of(String.class), TypeDescription.ForLoadedType.of(Object.class)))), is(false));
        assertThat(describe(secondMethod).isBridgeCompatible(new MethodDescription.TypeToken(TypeDescription.ForLoadedType.of(Object.class),
                Arrays.asList(TypeDescription.ForLoadedType.of(String.class), TypeDescription.ForLoadedType.of(int.class)))), is(false));
    }

    @Test
    public void testSyntethicParameter() throws Exception {
        assertThat(describe(SyntheticParameter.class.getDeclaredConstructor(AbstractMethodDescriptionTest.class, Void.class))
                .getParameters()
                .get(1)
                .getDeclaredAnnotations()
                .isAnnotationPresent(SyntheticMarker.class), is(true));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes", "cast"})
    public void testIsDefault() throws Exception {
        Map<String, AnnotationValue<?, ?>> properties = new LinkedHashMap<String, AnnotationValue<?, ?>>();
        properties.put("boolean_property", AnnotationValue.ForConstant.of(true));
        properties.put("boolean_property_array", AnnotationValue.ForConstant.of(new boolean[]{true}));
        properties.put("byte_property", AnnotationValue.ForConstant.of((byte) 0));
        properties.put("byte_property_array", AnnotationValue.ForConstant.of(new byte[]{0}));
        properties.put("short_property", AnnotationValue.ForConstant.of((short) 0));
        properties.put("short_property_array", AnnotationValue.ForConstant.of(new short[]{0}));
        properties.put("int_property", AnnotationValue.ForConstant.of(0));
        properties.put("int_property_array", AnnotationValue.ForConstant.of(new int[]{0}));
        properties.put("long_property", AnnotationValue.ForConstant.of(0L));
        properties.put("long_property_array", AnnotationValue.ForConstant.of(new long[]{0}));
        properties.put("float_property", AnnotationValue.ForConstant.of(0f));
        properties.put("float_property_array", AnnotationValue.ForConstant.of(new float[]{0}));
        properties.put("double_property", AnnotationValue.ForConstant.of(0d));
        properties.put("double_property_array", AnnotationValue.ForConstant.of(new double[]{0d}));
        properties.put("string_property", AnnotationValue.ForConstant.of("foo"));
        properties.put("string_property_array", AnnotationValue.ForConstant.of(new String[]{"foo"}));
        AnnotationDescription annotationDescription = mock(AnnotationDescription.class);
        when(annotationDescription.getAnnotationType()).thenReturn(TypeDescription.ForLoadedType.of(SampleAnnotation.class));
        properties.put("annotation_property", new AnnotationValue.ForAnnotationDescription(annotationDescription));
        properties.put("annotation_property_array", AnnotationValue.ForDescriptionArray.of(TypeDescription.ForLoadedType.of(SampleAnnotation.class), new AnnotationDescription[]{annotationDescription}));
        EnumerationDescription enumerationDescription = mock(EnumerationDescription.class);
        when(enumerationDescription.getEnumerationType()).thenReturn(TypeDescription.ForLoadedType.of(SampleEnumeration.class));
        properties.put("enum_property", AnnotationValue.ForEnumerationDescription.<Enum>of(enumerationDescription));
        properties.put("enum_property_array", AnnotationValue.ForDescriptionArray.<Enum>of(TypeDescription.ForLoadedType.of(SampleEnumeration.class), new EnumerationDescription[]{enumerationDescription}));
        MethodList<?> methods = TypeDescription.ForLoadedType.of(AnnotationValues.class).getDeclaredMethods();
        for (Map.Entry<String, AnnotationValue<?, ?>> entry : properties.entrySet()) {
            assertThat(methods.filter(named(entry.getKey())).getOnly().isDefaultValue(entry.getValue()), is(true));
            assertThat(methods.filter(named(entry.getKey())).getOnly().isDefaultValue(mock(AnnotationValue.class)), is(false));
        }
        when(annotationDescription.getAnnotationType()).thenReturn(TypeDescription.ForLoadedType.of(Object.class));
        assertThat(methods.filter(named("annotation_property")).getOnly().isDefaultValue(new AnnotationValue.ForAnnotationDescription(annotationDescription)), is(false));
        assertThat(methods.filter(named("annotation_property_array")).getOnly().isDefaultValue(AnnotationValue.ForDescriptionArray.of(TypeDescription.ForLoadedType.of(Object.class), new AnnotationDescription[]{annotationDescription})), is(false));
        when(enumerationDescription.getEnumerationType()).thenReturn(TypeDescription.ForLoadedType.of(Object.class));
        assertThat(methods.filter(named("enum_property")).getOnly().isDefaultValue(AnnotationValue.ForEnumerationDescription.<Enum>of(enumerationDescription)), is(false));
        assertThat(methods.filter(named("enum_property_array")).getOnly().isDefaultValue(AnnotationValue.ForDescriptionArray.<Enum>of(TypeDescription.ForLoadedType.of(Object.class), new EnumerationDescription[]{enumerationDescription})), is(false));
    }

    @Test
    public void testEnumConstructorAnnotation() throws Exception {
        MethodDescription.InDefinedShape constructor = describe(EnumConstructorAnnotationSample.class.getDeclaredConstructor(String.class, int.class, Void.class));
        assertThat(constructor.getParameters().get(2).getDeclaredAnnotations().size(), is(1));
        assertThat(constructor.getParameters().get(2).getDeclaredAnnotations().get(0).getAnnotationType().represents(SampleAnnotation.class), is(true));
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface SampleAnnotation {
        /* empty */
    }

    private enum SampleEnumeration {
        INSTANCE
    }

    @SuppressWarnings("unused")
    private abstract static class Sample {

        Sample(final Void argument) {
            /* do nothing */
        }

        @SampleAnnotation
        private Sample(int[] first, @SampleAnnotation long second) throws IOException {

        }

        private static void first() {
            /* do nothing */
        }

        protected abstract Object second(String first, long second) throws RuntimeException, IOException;

        @SampleAnnotation
        public boolean[] third(@SampleAnnotation final Object[] first, int[] second) throws Throwable {
            return null;
        }
    }

    private abstract static class SampleSub extends Sample {

        protected SampleSub(Void argument) {
            super(argument);
        }
    }

    @SuppressWarnings("unused")
    public abstract static class PublicType {

        public PublicType() {
            /* do nothing*/
        }

        protected PublicType(Void protectedConstructor) {
            /* do nothing*/
        }

        PublicType(Object packagePrivateConstructor) {
            /* do nothing*/
        }

        private PublicType(String privateConstructor) {
            /* do nothing*/
        }

        public abstract void publicMethod();

        protected abstract void protectedMethod();

        abstract void packagePrivateMethod();

        private void privateMethod() {
            /* do nothing*/
        }
    }

    @SuppressWarnings("unused")
    abstract static class PackagePrivateType {

        public PackagePrivateType() {
            /* do nothing*/
        }

        protected PackagePrivateType(Void protectedConstructor) {
            /* do nothing*/
        }

        PackagePrivateType(Object packagePrivateConstructor) {
            /* do nothing*/
        }

        private PackagePrivateType(String privateConstructor) {
            /* do nothing*/
        }

        public abstract void publicMethod();

        protected abstract void protectedMethod();

        abstract void packagePrivateMethod();

        public static void staticPublicMethod() {
            /* empty */
        }

        protected static void staticProtectedMethod() {
            /* empty */
        }

        static void staticPackagePrivateMethod() {
            /* empty */
        }

        private static void staticPrivateMethod() {
            /* empty */
        }

        private void privateMethod() {
            /* do nothing*/
        }
    }

    @SuppressWarnings("unused")
    public static class MethodVisibilityType {

        public void packagePrivateArgument(PackagePrivateType arg) {
            /* do nothing */
        }

        public PackagePrivateType packagePrivateReturnType() {
            return null;
        }
    }

    @SuppressWarnings("unused")
    static class GenericMethod<T extends Exception> {

        T foo(T t) throws T {
            return null;
        }

        T bar(T t) throws Exception {
            return null;
        }

        <Q> void qux() {
            /* empty */
        }
    }

    static class NativeMethod {

        native void foo();
    }

    private static class DeprecationSample {

        @Deprecated
        private void foo() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    private @interface AnnotationValues {

        boolean boolean_property();

        boolean[] boolean_property_array();

        byte byte_property();

        byte[] byte_property_array();

        short short_property();

        short[] short_property_array();

        int int_property();

        int[] int_property_array();

        long long_property();

        long[] long_property_array();

        float float_property();

        float[] float_property_array();

        double double_property();

        double[] double_property_array();

        String string_property();

        String[] string_property_array();

        SampleAnnotation annotation_property();

        SampleAnnotation[] annotation_property_array();

        SampleEnumeration enum_property();

        SampleEnumeration[] enum_property_array();
    }

    public class SyntheticParameter {

        public SyntheticParameter(@SyntheticMarker Void unused) {
            /* empty */
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface SyntheticMarker {
        /* empty */
    }

    private enum EnumConstructorAnnotationSample {

        INSTANCE(null);

        @SuppressWarnings("unused")
        EnumConstructorAnnotationSample(@SampleAnnotation Void ignored) {
            /* empty */
        }
    }
}
