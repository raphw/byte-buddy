package net.bytebuddy.matcher;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.ModifierReviewable;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class ElementMatchersTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    private static final String SINGLE_DEFAULT_METHOD = "net.bytebuddy.test.precompiled.SingleDefaultMethodInterface";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @SuppressWarnings({"unchecked", "row"})
    public void testFailSafe() throws Exception {
        ElementMatcher<Object> exceptional = mock(ElementMatcher.class), nonExceptional = mock(ElementMatcher.class);
        when(exceptional.matches(any())).thenThrow(RuntimeException.class);
        when(nonExceptional.matches(any())).thenReturn(true);
        assertThat(ElementMatchers.failSafe(exceptional).matches(new Object()), is(false));
        assertThat(ElementMatchers.failSafe(nonExceptional).matches(new Object()), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCachedNegativeSize() throws Exception {
        ElementMatchers.cached(new BooleanMatcher<Object>(true), -1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCachingMatcherEvictionSize() throws Exception {
        ElementMatcher<Object> delegate = mock(ElementMatcher.class);
        ElementMatcher<Object> matcher = ElementMatchers.cached(delegate, 1);
        Object target = new Object();
        when(delegate.matches(target)).thenReturn(true);
        assertThat(matcher.matches(target), is(true));
        assertThat(matcher.matches(target), is(true));
        verify(delegate).matches(target);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCachingMatcherMap() throws Exception {
        ElementMatcher<Object> delegate = mock(ElementMatcher.class);
        ConcurrentMap<Object, Boolean> map = new ConcurrentHashMap<Object, Boolean>();
        ElementMatcher<Object> matcher = ElementMatchers.cached(delegate, map);
        Object target = new Object();
        when(delegate.matches(target)).thenReturn(true);
        assertThat(matcher.matches(target), is(true));
        assertThat(matcher.matches(target), is(true));
        verify(delegate).matches(target);
        assertThat(map.get(target), is(true));
    }

    @Test
    public void testIs() throws Exception {
        Object value = new Object();
        assertThat(ElementMatchers.is(value).matches(value), is(true));
        assertThat(ElementMatchers.is(value).matches(new Object()), is(false));
        assertThat(ElementMatchers.is((Object) null).matches(null), is(true));
        assertThat(ElementMatchers.is((Object) null).matches(new Object()), is(false));
    }

    @Test
    public void testIsInterface() throws Exception {
        assertThat(ElementMatchers.isInterface().matches(new TypeDescription.ForLoadedType(Collection.class)), is(true));
        assertThat(ElementMatchers.isInterface().matches(new TypeDescription.ForLoadedType(ArrayList.class)), is(false));
    }

    @Test
    public void testIsType() throws Exception {
        assertThat(ElementMatchers.is(Object.class).matches(TypeDescription.OBJECT), is(true));
        assertThat(ElementMatchers.is(String.class).matches(TypeDescription.OBJECT), is(false));
    }

    @Test
    public void testIsField() throws Exception {
        assertThat(ElementMatchers.is(FieldSample.class.getDeclaredField("foo"))
                .matches(new FieldDescription.ForLoadedField(FieldSample.class.getDeclaredField("foo"))), is(true));
        assertThat(ElementMatchers.is(FieldSample.class.getDeclaredField("bar"))
                .matches(new FieldDescription.ForLoadedField(FieldSample.class.getDeclaredField("foo"))), is(false));
    }

    @Test
    public void testIsVolatile() throws Exception {
        assertThat(ElementMatchers.isVolatile().matches(new FieldDescription.ForLoadedField(FieldSample.class.getDeclaredField("foo"))), is(false));
        assertThat(ElementMatchers.isVolatile().matches(new FieldDescription.ForLoadedField(FieldSample.class.getDeclaredField("qux"))), is(true));
        assertThat(ElementMatchers.isVolatile().matches(new FieldDescription.ForLoadedField(FieldSample.class.getDeclaredField("baz"))), is(false));
    }

    @Test
    public void testIsTransient() throws Exception {
        assertThat(ElementMatchers.isTransient().matches(new FieldDescription.ForLoadedField(FieldSample.class.getDeclaredField("foo"))), is(false));
        assertThat(ElementMatchers.isTransient().matches(new FieldDescription.ForLoadedField(FieldSample.class.getDeclaredField("qux"))), is(false));
        assertThat(ElementMatchers.isTransient().matches(new FieldDescription.ForLoadedField(FieldSample.class.getDeclaredField("baz"))), is(true));
    }

    @Test
    public void testIsFieldDefinedShape() throws Exception {
        Field field = GenericFieldType.class.getDeclaredField(FOO);
        FieldDescription fieldDescription = new TypeDescription.ForLoadedType(GenericFieldType.Inner.class).getSuperClass()
                .getDeclaredFields().filter(named(FOO)).getOnly();
        assertThat(ElementMatchers.is(field).matches(fieldDescription), is(true));
        assertThat(ElementMatchers.definedField(ElementMatchers.is(fieldDescription.asDefined())).matches(fieldDescription), is(true));
        assertThat(ElementMatchers.is(fieldDescription.asDefined()).matches(fieldDescription.asDefined()), is(true));
        assertThat(ElementMatchers.is(fieldDescription.asDefined()).matches(fieldDescription), is(true));
        assertThat(ElementMatchers.is(fieldDescription).matches(fieldDescription.asDefined()), is(false));
    }

    @Test
    public void testIsMethodOrConstructor() throws Exception {
        assertThat(ElementMatchers.is(Object.class.getDeclaredMethod("toString"))
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(true));
        assertThat(ElementMatchers.is(Object.class.getDeclaredMethod("toString"))
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("hashCode"))), is(false));
        assertThat(ElementMatchers.is(Object.class.getDeclaredConstructor())
                .matches(new MethodDescription.ForLoadedConstructor(Object.class.getDeclaredConstructor())), is(true));
        assertThat(ElementMatchers.is(Object.class.getDeclaredConstructor())
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("hashCode"))), is(false));
    }

    @Test
    public void testIsMethodDefinedShape() throws Exception {
        Method method = GenericMethodType.class.getDeclaredMethod("foo", Exception.class);
        MethodDescription methodDescription = new TypeDescription.ForLoadedType(GenericMethodType.Inner.class).getInterfaces().getOnly()
                .getDeclaredMethods().filter(named(FOO)).getOnly();
        assertThat(ElementMatchers.is(method).matches(methodDescription), is(true));
        assertThat(ElementMatchers.definedMethod(ElementMatchers.is(methodDescription.asDefined())).matches(methodDescription), is(true));
        assertThat(ElementMatchers.is(methodDescription.asDefined()).matches(methodDescription.asDefined()), is(true));
        assertThat(ElementMatchers.is(methodDescription.asDefined()).matches(methodDescription), is(true));
        assertThat(ElementMatchers.is(methodDescription).matches(methodDescription.asDefined()), is(false));
    }

    @Test
    public void testIsConstructorDefinedShape() throws Exception {
        Constructor<?> constructor = GenericConstructorType.class.getDeclaredConstructor(Exception.class);
        MethodDescription methodDescription = new TypeDescription.ForLoadedType(GenericConstructorType.Inner.class).getSuperClass()
                .getDeclaredMethods().filter(isConstructor()).getOnly();
        assertThat(ElementMatchers.is(constructor).matches(methodDescription), is(true));
        assertThat(ElementMatchers.definedMethod(ElementMatchers.is(methodDescription.asDefined())).matches(methodDescription), is(true));
        assertThat(ElementMatchers.is(methodDescription.asDefined()).matches(methodDescription.asDefined()), is(true));
        assertThat(ElementMatchers.is(methodDescription.asDefined()).matches(methodDescription), is(true));
        assertThat(ElementMatchers.is(methodDescription).matches(methodDescription.asDefined()), is(false));
    }

    @Test
    public void testIsParameterDefinedShape() throws Exception {
        ParameterDescription parameterDescription = new TypeDescription.ForLoadedType(GenericMethodType.Inner.class).getInterfaces().getOnly()
                .getDeclaredMethods().filter(named(FOO)).getOnly().getParameters().getOnly();
        assertThat(ElementMatchers.definedParameter(ElementMatchers.is(parameterDescription.asDefined())).matches(parameterDescription), is(true));
        assertThat(ElementMatchers.is(parameterDescription.asDefined()).matches(parameterDescription.asDefined()), is(true));
        assertThat(ElementMatchers.is(parameterDescription.asDefined()).matches(parameterDescription), is(true));
        assertThat(ElementMatchers.is(parameterDescription).matches(parameterDescription.asDefined()), is(false));
    }

    @Test
    public void testIsAnnotation() throws Exception {
        AnnotationDescription annotationDescription = new TypeDescription.ForLoadedType(IsAnnotatedWith.class)
                .getDeclaredAnnotations().ofType(IsAnnotatedWithAnnotation.class);
        assertThat(ElementMatchers.is(IsAnnotatedWith.class.getAnnotation(IsAnnotatedWithAnnotation.class)).matches(annotationDescription), is(true));
        assertThat(ElementMatchers.is(Other.class.getAnnotation(OtherAnnotation.class)).matches(annotationDescription), is(false));
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
    public void testAnyOfType() throws Exception {
        assertThat(ElementMatchers.anyOf(Object.class).matches(TypeDescription.OBJECT), is(true));
        assertThat(ElementMatchers.anyOf(String.class, Object.class).matches(TypeDescription.OBJECT), is(true));
        assertThat(ElementMatchers.anyOf(String.class).matches(TypeDescription.OBJECT), is(false));
    }

    @Test
    public void testAnyOfMethodOrConstructor() throws Exception {
        Method toString = Object.class.getDeclaredMethod("toString"), hashCode = Object.class.getDeclaredMethod("hashCode");
        assertThat(ElementMatchers.anyOf(toString)
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(true));
        assertThat(ElementMatchers.anyOf(toString, hashCode)
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(true));
        assertThat(ElementMatchers.anyOf(toString)
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("hashCode"))), is(false));
        assertThat(ElementMatchers.anyOf(Object.class.getDeclaredConstructor())
                .matches(new MethodDescription.ForLoadedConstructor(Object.class.getDeclaredConstructor())), is(true));
        assertThat(ElementMatchers.anyOf(Object.class.getDeclaredConstructor(), String.class.getDeclaredConstructor(String.class))
                .matches(new MethodDescription.ForLoadedConstructor(Object.class.getDeclaredConstructor())), is(true));
        assertThat(ElementMatchers.anyOf(Object.class.getDeclaredConstructor())
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("hashCode"))), is(false));
    }

    @Test
    public void testAnyMethodDefinedShape() throws Exception {
        Method method = GenericMethodType.class.getDeclaredMethod("foo", Exception.class);
        MethodDescription methodDescription = new TypeDescription.ForLoadedType(GenericMethodType.Inner.class).getInterfaces().getOnly()
                .getDeclaredMethods().filter(named(FOO)).getOnly();
        assertThat(ElementMatchers.anyOf(method).matches(methodDescription), is(true));
        assertThat(ElementMatchers.definedMethod(ElementMatchers.anyOf(methodDescription.asDefined())).matches(methodDescription), is(true));
        assertThat(ElementMatchers.anyOf(methodDescription.asDefined()).matches(methodDescription.asDefined()), is(true));
        assertThat(ElementMatchers.anyOf(methodDescription.asDefined()).matches(methodDescription), is(false));
        assertThat(ElementMatchers.anyOf(methodDescription).matches(methodDescription.asDefined()), is(false));
    }

    @Test
    public void testAnyOfConstructorDefinedShape() throws Exception {
        Constructor<?> constructor = GenericConstructorType.class.getDeclaredConstructor(Exception.class);
        MethodDescription methodDescription = new TypeDescription.ForLoadedType(GenericConstructorType.Inner.class).getSuperClass()
                .getDeclaredMethods().filter(isConstructor()).getOnly();
        assertThat(ElementMatchers.anyOf(constructor).matches(methodDescription), is(true));
        assertThat(ElementMatchers.definedMethod(ElementMatchers.anyOf(methodDescription.asDefined())).matches(methodDescription), is(true));
        assertThat(ElementMatchers.anyOf(methodDescription.asDefined()).matches(methodDescription.asDefined()), is(true));
        assertThat(ElementMatchers.anyOf(methodDescription.asDefined()).matches(methodDescription), is(false));
        assertThat(ElementMatchers.anyOf(methodDescription).matches(methodDescription.asDefined()), is(false));
    }

    @Test
    public void testAnyOfField() throws Exception {
        assertThat(ElementMatchers.anyOf(Integer.class.getDeclaredField("MAX_VALUE"))
                .matches(new FieldDescription.ForLoadedField(Integer.class.getDeclaredField("MAX_VALUE"))), is(true));
        assertThat(ElementMatchers.anyOf(Integer.class.getDeclaredField("MAX_VALUE"), Integer.class.getDeclaredField("MIN_VALUE"))
                .matches(new FieldDescription.ForLoadedField(Integer.class.getDeclaredField("MAX_VALUE"))), is(true));
        assertThat(ElementMatchers.anyOf(Integer.class.getDeclaredField("MAX_VALUE"), Integer.class.getDeclaredField("MIN_VALUE"))
                .matches(new FieldDescription.ForLoadedField(Integer.class.getDeclaredField("SIZE"))), is(false));
    }

    @Test
    public void testAnyOfFieldDefinedShape() throws Exception {
        Field field = GenericFieldType.class.getDeclaredField(FOO);
        FieldDescription fieldDescription = new TypeDescription.ForLoadedType(GenericFieldType.Inner.class).getSuperClass()
                .getDeclaredFields().filter(named(FOO)).getOnly();
        assertThat(ElementMatchers.anyOf(field).matches(fieldDescription), is(true));
        assertThat(ElementMatchers.definedField(ElementMatchers.anyOf(fieldDescription.asDefined())).matches(fieldDescription), is(true));
        assertThat(ElementMatchers.anyOf(fieldDescription.asDefined()).matches(fieldDescription.asDefined()), is(true));
        assertThat(ElementMatchers.anyOf(fieldDescription.asDefined()).matches(fieldDescription), is(false));
        assertThat(ElementMatchers.anyOf(fieldDescription).matches(fieldDescription.asDefined()), is(false));
    }

    @Test
    public void testAnyOfAnnotation() throws Exception {
        AnnotationDescription annotationDescription = new TypeDescription.ForLoadedType(IsAnnotatedWith.class)
                .getDeclaredAnnotations().ofType(IsAnnotatedWithAnnotation.class);
        assertThat(ElementMatchers.anyOf(IsAnnotatedWith.class.getAnnotation(IsAnnotatedWithAnnotation.class))
                .matches(annotationDescription), is(true));
        assertThat(ElementMatchers.anyOf(IsAnnotatedWith.class.getAnnotation(IsAnnotatedWithAnnotation.class),
                Other.class.getAnnotation(OtherAnnotation.class)).matches(annotationDescription), is(true));
        assertThat(ElementMatchers.anyOf(Other.class.getAnnotation(OtherAnnotation.class)).matches(annotationDescription), is(false));
    }

    @Test
    public void testAnnotationType() throws Exception {
        AnnotationDescription annotationDescription = new TypeDescription.ForLoadedType(IsAnnotatedWith.class)
                .getDeclaredAnnotations().ofType(IsAnnotatedWithAnnotation.class);
        assertThat(ElementMatchers.annotationType(IsAnnotatedWithAnnotation.class).matches(annotationDescription), is(true));
        assertThat(ElementMatchers.annotationType(OtherAnnotation.class).matches(annotationDescription), is(false));
        assertThat(ElementMatchers.annotationType(IsAnnotatedWithAnnotation.class)
                .matches(AnnotationDescription.ForLoadedAnnotation.of(Other.class.getAnnotation(OtherAnnotation.class))), is(false));
    }

    @Test
    public void testNone() throws Exception {
        assertThat(ElementMatchers.none().matches(new Object()), is(false));
    }

    @Test
    public void testNoneOfType() throws Exception {
        assertThat(ElementMatchers.noneOf(Object.class).matches(TypeDescription.OBJECT), is(false));
        assertThat(ElementMatchers.noneOf(String.class, Object.class).matches(TypeDescription.OBJECT), is(false));
        assertThat(ElementMatchers.noneOf(String.class).matches(TypeDescription.OBJECT), is(true));
    }

    @Test
    public void testNoneOfConstructorDefinedShape() throws Exception {
        Constructor<?> constructor = GenericConstructorType.class.getDeclaredConstructor(Exception.class);
        MethodDescription methodDescription = new TypeDescription.ForLoadedType(GenericConstructorType.Inner.class).getSuperClass()
                .getDeclaredMethods().filter(isConstructor()).getOnly();
        assertThat(ElementMatchers.noneOf(constructor).matches(methodDescription), is(false));
        assertThat(ElementMatchers.definedMethod(ElementMatchers.noneOf(methodDescription.asDefined())).matches(methodDescription), is(false));
        assertThat(ElementMatchers.noneOf(methodDescription.asDefined()).matches(methodDescription.asDefined()), is(false));
        assertThat(ElementMatchers.noneOf(methodDescription.asDefined()).matches(methodDescription), is(true));
        assertThat(ElementMatchers.noneOf(methodDescription).matches(methodDescription.asDefined()), is(true));
    }

    @Test
    public void testNoneOfMethodDefinedShape() throws Exception {
        Method method = GenericMethodType.class.getDeclaredMethod("foo", Exception.class);
        MethodDescription methodDescription = new TypeDescription.ForLoadedType(GenericMethodType.Inner.class).getInterfaces().getOnly()
                .getDeclaredMethods().filter(named(FOO)).getOnly();
        assertThat(ElementMatchers.noneOf(method).matches(methodDescription), is(false));
        assertThat(ElementMatchers.definedMethod(ElementMatchers.noneOf(methodDescription.asDefined())).matches(methodDescription), is(false));
        assertThat(ElementMatchers.noneOf(methodDescription.asDefined()).matches(methodDescription.asDefined()), is(false));
        assertThat(ElementMatchers.noneOf(methodDescription.asDefined()).matches(methodDescription), is(true));
        assertThat(ElementMatchers.noneOf(methodDescription).matches(methodDescription.asDefined()), is(true));
    }

    @Test
    public void testNoneOfField() throws Exception {
        assertThat(ElementMatchers.noneOf(FieldSample.class.getDeclaredField("foo"))
                .matches(new FieldDescription.ForLoadedField(FieldSample.class.getDeclaredField("foo"))), is(false));
        assertThat(ElementMatchers.noneOf(FieldSample.class.getDeclaredField("bar"))
                .matches(new FieldDescription.ForLoadedField(FieldSample.class.getDeclaredField("foo"))), is(true));
        assertThat(ElementMatchers.noneOf(FieldSample.class.getDeclaredField("foo"), FieldSample.class.getDeclaredField("bar"))
                .matches(new FieldDescription.ForLoadedField(FieldSample.class.getDeclaredField("foo"))), is(false));
    }

    @Test
    public void testNoneOfFieldDefinedShape() throws Exception {
        Field field = GenericFieldType.class.getDeclaredField(FOO);
        FieldDescription fieldDescription = new TypeDescription.ForLoadedType(GenericFieldType.Inner.class).getSuperClass()
                .getDeclaredFields().filter(named(FOO)).getOnly();
        assertThat(ElementMatchers.noneOf(field).matches(fieldDescription), is(false));
        assertThat(ElementMatchers.definedField(ElementMatchers.noneOf(fieldDescription.asDefined())).matches(fieldDescription), is(false));
        assertThat(ElementMatchers.noneOf(fieldDescription.asDefined()).matches(fieldDescription.asDefined()), is(false));
        assertThat(ElementMatchers.noneOf(fieldDescription.asDefined()).matches(fieldDescription), is(true));
        assertThat(ElementMatchers.noneOf(fieldDescription).matches(fieldDescription.asDefined()), is(true));
    }

    @Test
    public void testNoneAnnotation() throws Exception {
        AnnotationDescription annotationDescription = new TypeDescription.ForLoadedType(IsAnnotatedWith.class)
                .getDeclaredAnnotations().ofType(IsAnnotatedWithAnnotation.class);
        assertThat(ElementMatchers.noneOf(IsAnnotatedWith.class.getAnnotation(IsAnnotatedWithAnnotation.class))
                .matches(annotationDescription), is(false));
        assertThat(ElementMatchers.noneOf(IsAnnotatedWith.class.getAnnotation(IsAnnotatedWithAnnotation.class),
                Other.class.getAnnotation(OtherAnnotation.class)).matches(annotationDescription), is(false));
        assertThat(ElementMatchers.noneOf(Other.class.getAnnotation(OtherAnnotation.class)).matches(annotationDescription), is(true));
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
    public void testWhereAny() throws Exception {
        assertThat(ElementMatchers.whereAny(ElementMatchers.is(FOO)).matches(Arrays.asList(FOO, BAR)), is(true));
        assertThat(ElementMatchers.whereAny(ElementMatchers.is(FOO)).matches(Arrays.asList(BAR, QUX)), is(false));
    }

    @Test
    public void testWhereNone() throws Exception {
        assertThat(ElementMatchers.whereNone(ElementMatchers.is(FOO)).matches(Arrays.asList(FOO, BAR)), is(false));
        assertThat(ElementMatchers.whereNone(ElementMatchers.is(FOO)).matches(Arrays.asList(BAR, QUX)), is(true));
    }

    @Test
    public void testRawType() throws Exception {
        assertThat(ElementMatchers.erasure(Exception.class).matches(TypeDefinition.Sort.describe(GenericMethodType.class.getTypeParameters()[0])), is(true));
        assertThat(ElementMatchers.erasure(Object.class).matches(TypeDefinition.Sort.describe(GenericMethodType.class.getTypeParameters()[0])), is(false));
    }

    @Test
    public void testRawTypes() throws Exception {
        assertThat(ElementMatchers.erasures(Exception.class)
                .matches(Collections.singletonList(TypeDefinition.Sort.describe(GenericMethodType.class.getTypeParameters()[0]))), is(true));
        assertThat(ElementMatchers.erasures(Object.class)
                .matches(Collections.singletonList(TypeDefinition.Sort.describe(GenericMethodType.class.getTypeParameters()[0]))), is(false));
    }

    @Test
    public void testIsTypeVariable() throws Exception {
        assertThat(ElementMatchers.isVariable("T").matches(new TypeDescription.ForLoadedType(GenericDeclaredBy.class).getTypeVariables().getOnly()), is(true));
        assertThat(ElementMatchers.isVariable(FOO).matches(new TypeDescription.ForLoadedType(GenericDeclaredBy.class).getTypeVariables().getOnly()), is(false));
        assertThat(ElementMatchers.isVariable(FOO).matches(TypeDescription.OBJECT), is(false));
    }

    @Test
    public void testMethodName() throws Exception {
        assertThat(ElementMatchers.hasMethodName(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME), is(ElementMatchers.isTypeInitializer()));
        assertThat(ElementMatchers.hasMethodName(MethodDescription.CONSTRUCTOR_INTERNAL_NAME), is(ElementMatchers.isConstructor()));
        ElementMatcher<MethodDescription> nameMatcher = named(FOO);
        assertThat(ElementMatchers.hasMethodName(FOO), is(nameMatcher));
    }

    @Test
    public void testNamed() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getActualName()).thenReturn(FOO);
        assertThat(named(FOO).matches(byteCodeElement), is(true));
        assertThat(named(FOO.toUpperCase()).matches(byteCodeElement), is(false));
        assertThat(named(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNamedIgnoreCase() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getActualName()).thenReturn(FOO);
        assertThat(ElementMatchers.namedIgnoreCase(FOO).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.namedIgnoreCase(FOO.toUpperCase()).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.namedIgnoreCase(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameStartsWith() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getActualName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameStartsWith(FOO.substring(0, 2)).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameStartsWith(FOO.substring(0, 2).toUpperCase()).matches(byteCodeElement), is(false));
        assertThat(ElementMatchers.nameStartsWith(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameStartsWithIgnoreCase() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getActualName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameStartsWithIgnoreCase(FOO.substring(0, 2)).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameStartsWithIgnoreCase(FOO.substring(0, 2).toUpperCase()).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameStartsWithIgnoreCase(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameEndsWith() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getActualName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameEndsWith(FOO.substring(1)).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameEndsWith(FOO.substring(1).toUpperCase()).matches(byteCodeElement), is(false));
        assertThat(ElementMatchers.nameEndsWith(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameEndsWithIgnoreCase() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getActualName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameEndsWithIgnoreCase(FOO.substring(1)).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameEndsWithIgnoreCase(FOO.substring(1).toUpperCase()).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameEndsWithIgnoreCase(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameContains() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getActualName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameContains(FOO.substring(1, 2)).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameContains(FOO.substring(1, 2).toUpperCase()).matches(byteCodeElement), is(false));
        assertThat(ElementMatchers.nameContains(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameContainsIgnoreCase() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getActualName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameContainsIgnoreCase(FOO.substring(1, 2)).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameContainsIgnoreCase(FOO.substring(1, 2).toUpperCase()).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameContainsIgnoreCase(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameMatches() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getActualName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameMatches("^" + FOO + "$").matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameMatches(FOO.toUpperCase()).matches(byteCodeElement), is(false));
        assertThat(ElementMatchers.nameMatches(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testIsNamed() throws Exception {
        NamedElement.WithOptionalName namedElement = mock(NamedElement.WithOptionalName.class);
        assertThat(ElementMatchers.isNamed().matches(namedElement), is(false));
        when(namedElement.isNamed()).thenReturn(true);
        assertThat(ElementMatchers.isNamed().matches(namedElement), is(true));
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
    public void testIsDeclaredByGeneric() throws Exception {
        assertThat(ElementMatchers.isDeclaredByGeneric(GenericDeclaredBy.Inner.class.getGenericInterfaces()[0])
                .matches(new TypeDescription.ForLoadedType(GenericDeclaredBy.Inner.class)
                        .getInterfaces().getOnly().getDeclaredMethods().filter(ElementMatchers.isMethod()).getOnly()), is(true));
        assertThat(ElementMatchers.isDeclaredByGeneric(GenericDeclaredBy.Inner.class.getGenericInterfaces()[0])
                .matches(new TypeDescription.ForLoadedType(GenericDeclaredBy.class)
                        .getDeclaredMethods().filter(ElementMatchers.isMethod()).getOnly()), is(false));
        assertThat(ElementMatchers.isDeclaredByGeneric(GenericDeclaredBy.class)
                .matches(new TypeDescription.ForLoadedType(GenericDeclaredBy.Inner.class)
                        .getInterfaces().getOnly().getDeclaredMethods().filter(ElementMatchers.isMethod()).getOnly()), is(false));
    }

    @Test
    public void testIsOverriddenFrom() throws Exception {
        assertThat(ElementMatchers.isOverriddenFrom(Object.class).matches(new MethodDescription.ForLoadedMethod(String.class.getDeclaredMethod("toString"))), is(true));
        assertThat(ElementMatchers.isOverriddenFrom(Object.class).matches(new MethodDescription.ForLoadedMethod(String.class.getDeclaredMethod("substring", int.class))), is(false));
        assertThat(ElementMatchers.isOverriddenFrom(Comparable.class).matches(new MethodDescription.ForLoadedMethod(String.class.getDeclaredMethod("compareTo", String.class))), is(true));
        assertThat(ElementMatchers.isOverriddenFromGeneric(Object.class).matches(new MethodDescription.ForLoadedMethod(String.class.getDeclaredMethod("toString"))), is(true));
        assertThat(ElementMatchers.isOverriddenFromGeneric(Object.class).matches(new MethodDescription.ForLoadedMethod(String.class.getDeclaredMethod("substring", int.class))), is(false));
        assertThat(ElementMatchers.isOverriddenFromGeneric(Comparable.class).matches(new MethodDescription.ForLoadedMethod(String.class.getDeclaredMethod("compareTo", String.class))), is(false));
        assertThat(ElementMatchers.isOverriddenFromGeneric(String.class.getGenericInterfaces()[1])
                .matches(new MethodDescription.ForLoadedMethod(String.class.getDeclaredMethod("compareTo", String.class))), is(true));
    }

    @Test
    public void testIsVisibleTo() throws Exception {
        assertThat(ElementMatchers.isVisibleTo(Object.class).matches(new TypeDescription.ForLoadedType(IsVisibleTo.class)), is(true));
        assertThat(ElementMatchers.isVisibleTo(Object.class).matches(new TypeDescription.ForLoadedType(IsNotVisibleTo.class)), is(false));
    }

    @Test
    public void testIsAccessibleTo() throws Exception {
        assertThat(ElementMatchers.isAccessibleTo(Object.class).matches(new TypeDescription.ForLoadedType(IsVisibleTo.class)), is(true));
        assertThat(ElementMatchers.isAccessibleTo(Object.class).matches(new TypeDescription.ForLoadedType(IsNotVisibleTo.class)), is(false));
    }

    @Test
    public void testIsAnnotatedWith() throws Exception {
        assertThat(ElementMatchers.isAnnotatedWith(IsAnnotatedWithAnnotation.class)
                .matches(new TypeDescription.ForLoadedType(IsAnnotatedWith.class)), is(true));
        assertThat(ElementMatchers.isAnnotatedWith(IsAnnotatedWithAnnotation.class)
                .matches(TypeDescription.OBJECT), is(false));
    }

    @Test
    public void testIsPublic() throws Exception {
        ModifierReviewable.OfByteCodeElement modifierReviewable = mock(ModifierReviewable.OfByteCodeElement.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_PUBLIC);
        assertThat(ElementMatchers.isPublic().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isPublic().matches(mock(ModifierReviewable.OfByteCodeElement.class)), is(false));
    }

    @Test
    public void testIsProtected() throws Exception {
        ModifierReviewable.OfByteCodeElement modifierReviewable = mock(ModifierReviewable.OfByteCodeElement.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_PROTECTED);
        assertThat(ElementMatchers.isProtected().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isProtected().matches(mock(ModifierReviewable.OfByteCodeElement.class)), is(false));
    }

    @Test
    public void testIsPackagePrivate() throws Exception {
        ModifierReviewable.OfByteCodeElement modifierReviewable = mock(ModifierReviewable.OfByteCodeElement.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED);
        assertThat(ElementMatchers.isPackagePrivate().matches(mock(ModifierReviewable.OfByteCodeElement.class)), is(true));
        assertThat(ElementMatchers.isPackagePrivate().matches(modifierReviewable), is(false));
    }

    @Test
    public void testIsPrivate() throws Exception {
        ModifierReviewable.OfByteCodeElement modifierReviewable = mock(ModifierReviewable.OfByteCodeElement.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_PRIVATE);
        assertThat(ElementMatchers.isPrivate().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isPrivate().matches(mock(ModifierReviewable.OfByteCodeElement.class)), is(false));
    }

    @Test
    public void testIsAbstract() throws Exception {
        ModifierReviewable.OfAbstraction modifierReviewable = mock(ModifierReviewable.OfAbstraction.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_ABSTRACT);
        assertThat(ElementMatchers.isAbstract().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isAbstract().matches(mock(ModifierReviewable.OfAbstraction.class)), is(false));
    }

    @Test
    public void testIsEnum() throws Exception {
        ModifierReviewable.OfEnumeration modifierReviewable = mock(ModifierReviewable.OfEnumeration.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_ENUM);
        assertThat(ElementMatchers.isEnum().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isEnum().matches(mock(ModifierReviewable.OfEnumeration.class)), is(false));
    }

    @Test
    public void testIsMandated() throws Exception {
        ParameterDescription parameterDescription = mock(ParameterDescription.class);
        when(parameterDescription.getModifiers()).thenReturn(Opcodes.ACC_MANDATED);
        assertThat(ElementMatchers.isMandated().matches(parameterDescription), is(true));
        assertThat(ElementMatchers.isMandated().matches(mock(ParameterDescription.class)), is(false));
    }

    @Test
    public void testIsFinal() throws Exception {
        ModifierReviewable.OfByteCodeElement modifierReviewable = mock(ModifierReviewable.OfByteCodeElement.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_FINAL);
        assertThat(ElementMatchers.isFinal().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isFinal().matches(mock(ModifierReviewable.OfByteCodeElement.class)), is(false));
    }

    @Test
    public void testIsStatic() throws Exception {
        ModifierReviewable.OfByteCodeElement modifierReviewable = mock(ModifierReviewable.OfByteCodeElement.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_STATIC);
        assertThat(ElementMatchers.isStatic().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isStatic().matches(mock(ModifierReviewable.OfByteCodeElement.class)), is(false));
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

    @Test
    public void testIsMethod() throws Exception {
        assertThat(ElementMatchers.is(IsEqual.class.getDeclaredMethod(FOO))
                .matches(new MethodDescription.ForLoadedMethod(IsEqual.class.getDeclaredMethod(FOO))), is(true));
        assertThat(ElementMatchers.is(IsEqual.class.getDeclaredMethod(FOO))
                .matches(mock(MethodDescription.class)), is(false));
        assertThat(ElementMatchers.is(IsEqual.class.getDeclaredConstructor())
                .matches(new MethodDescription.ForLoadedConstructor(IsEqual.class.getDeclaredConstructor())), is(true));
        assertThat(ElementMatchers.is(IsEqual.class.getDeclaredConstructor())
                .matches(mock(MethodDescription.class)), is(false));
    }

    @Test
    public void testReturnsGeneric() throws Exception {
        assertThat(ElementMatchers.returnsGeneric(GenericMethodType.class.getTypeParameters()[0])
                .matches(new MethodDescription.ForLoadedMethod(GenericMethodType.class.getDeclaredMethod(FOO, Exception.class))), is(true));
        assertThat(ElementMatchers.returnsGeneric(Exception.class)
                .matches(new MethodDescription.ForLoadedMethod(GenericMethodType.class.getDeclaredMethod(FOO, Exception.class))), is(false));
        assertThat(ElementMatchers.returns(Exception.class)
                .matches(new MethodDescription.ForLoadedMethod(GenericMethodType.class.getDeclaredMethod(FOO, Exception.class))), is(true));
    }

    @Test
    public void testReturns() throws Exception {
        assertThat(ElementMatchers.returns(void.class)
                .matches(new MethodDescription.ForLoadedMethod(Returns.class.getDeclaredMethod(FOO))), is(true));
        assertThat(ElementMatchers.returns(void.class)
                .matches(new MethodDescription.ForLoadedMethod(Returns.class.getDeclaredMethod(BAR))), is(false));
        assertThat(ElementMatchers.returns(String.class)
                .matches(new MethodDescription.ForLoadedMethod(Returns.class.getDeclaredMethod(BAR))), is(true));
        assertThat(ElementMatchers.returns(String.class)
                .matches(new MethodDescription.ForLoadedMethod(Returns.class.getDeclaredMethod(FOO))), is(false));
    }

    @Test
    public void testTakesArgumentsGeneric() throws Exception {
        assertThat(ElementMatchers.takesGenericArguments(GenericMethodType.class.getTypeParameters()[0])
                .matches(new MethodDescription.ForLoadedMethod(GenericMethodType.class.getDeclaredMethod(FOO, Exception.class))), is(true));
        assertThat(ElementMatchers.takesGenericArguments(TypeDefinition.Sort.describe(GenericMethodType.class.getTypeParameters()[0]))
                .matches(new MethodDescription.ForLoadedMethod(GenericMethodType.class.getDeclaredMethod(FOO, Exception.class))), is(true));
        assertThat(ElementMatchers.takesGenericArguments(Exception.class)
                .matches(new MethodDescription.ForLoadedMethod(GenericMethodType.class.getDeclaredMethod(FOO, Exception.class))), is(false));
        assertThat(ElementMatchers.takesGenericArguments(Collections.singletonList(new TypeDescription.ForLoadedType(Exception.class)))
                .matches(new MethodDescription.ForLoadedMethod(GenericMethodType.class.getDeclaredMethod(FOO, Exception.class))), is(false));
        assertThat(ElementMatchers.takesArguments(Exception.class)
                .matches(new MethodDescription.ForLoadedMethod(GenericMethodType.class.getDeclaredMethod(FOO, Exception.class))), is(true));
        assertThat(ElementMatchers.takesArguments(Collections.singletonList(new TypeDescription.ForLoadedType(Exception.class)))
                .matches(new MethodDescription.ForLoadedMethod(GenericMethodType.class.getDeclaredMethod(FOO, Exception.class))), is(true));
    }

    @Test
    public void testTakesArguments() throws Exception {
        assertThat(ElementMatchers.takesArguments(Void.class)
                .matches(new MethodDescription.ForLoadedMethod(TakesArguments.class.getDeclaredMethod(FOO, Void.class))), is(true));
        assertThat(ElementMatchers.takesArguments(Void.class, Object.class)
                .matches(new MethodDescription.ForLoadedMethod(TakesArguments.class.getDeclaredMethod(FOO, Void.class))), is(false));
        assertThat(ElementMatchers.takesArguments(String.class, int.class)
                .matches(new MethodDescription.ForLoadedMethod(TakesArguments.class.getDeclaredMethod(BAR, String.class, int.class))), is(true));
        assertThat(ElementMatchers.takesArguments(String.class, Integer.class)
                .matches(new MethodDescription.ForLoadedMethod(TakesArguments.class.getDeclaredMethod(BAR, String.class, int.class))), is(false));
    }

    @Test
    public void testTakesArgumentGeneric() throws Exception {
        assertThat(ElementMatchers.takesGenericArgument(0, GenericMethodType.class.getTypeParameters()[0])
                .matches(new MethodDescription.ForLoadedMethod(GenericMethodType.class.getDeclaredMethod(FOO, Exception.class))), is(true));
        assertThat(ElementMatchers.takesGenericArgument(0, Exception.class)
                .matches(new MethodDescription.ForLoadedMethod(GenericMethodType.class.getDeclaredMethod(FOO, Exception.class))), is(false));
        assertThat(ElementMatchers.takesGenericArgument(1, GenericMethodType.class.getTypeParameters()[0])
                .matches(new MethodDescription.ForLoadedMethod(GenericMethodType.class.getDeclaredMethod(FOO, Exception.class))), is(false));
    }

    @Test
    public void testTakesArgument() throws Exception {
        assertThat(ElementMatchers.takesArgument(0, Void.class)
                .matches(new MethodDescription.ForLoadedMethod(TakesArguments.class.getDeclaredMethod(FOO, Void.class))), is(true));
        assertThat(ElementMatchers.takesArgument(0, Object.class)
                .matches(new MethodDescription.ForLoadedMethod(TakesArguments.class.getDeclaredMethod(FOO, Void.class))), is(false));
        assertThat(ElementMatchers.takesArgument(1, int.class)
                .matches(new MethodDescription.ForLoadedMethod(TakesArguments.class.getDeclaredMethod(BAR, String.class, int.class))), is(true));
        assertThat(ElementMatchers.takesArgument(1, Integer.class)
                .matches(new MethodDescription.ForLoadedMethod(TakesArguments.class.getDeclaredMethod(BAR, String.class, int.class))), is(false));
        assertThat(ElementMatchers.takesArgument(2, int.class)
                .matches(new MethodDescription.ForLoadedMethod(TakesArguments.class.getDeclaredMethod(BAR, String.class, int.class))), is(false));
    }

    @Test
    public void testTakesArgumentsLength() throws Exception {
        assertThat(ElementMatchers.takesArguments(1)
                .matches(new MethodDescription.ForLoadedMethod(TakesArguments.class.getDeclaredMethod(FOO, Void.class))), is(true));
        assertThat(ElementMatchers.takesArguments(2)
                .matches(new MethodDescription.ForLoadedMethod(TakesArguments.class.getDeclaredMethod(FOO, Void.class))), is(false));
        assertThat(ElementMatchers.takesArguments(2)
                .matches(new MethodDescription.ForLoadedMethod(TakesArguments.class.getDeclaredMethod(BAR, String.class, int.class))), is(true));
        assertThat(ElementMatchers.takesArguments(3)
                .matches(new MethodDescription.ForLoadedMethod(TakesArguments.class.getDeclaredMethod(BAR, String.class, int.class))), is(false));
    }

    @Test
    public void testDeclaresException() throws Exception {
        assertThat(ElementMatchers.declaresException(IOException.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(FOO))), is(true));
        assertThat(ElementMatchers.declaresException(SQLException.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(FOO))), is(false));
        assertThat(ElementMatchers.declaresException(Error.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(FOO))), is(false));
        assertThat(ElementMatchers.declaresException(RuntimeException.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(FOO))), is(false));
        assertThat(ElementMatchers.declaresException(IOException.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(BAR))), is(false));
        assertThat(ElementMatchers.declaresException(SQLException.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(BAR))), is(false));
        assertThat(ElementMatchers.declaresException(Error.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(BAR))), is(false));
        assertThat(ElementMatchers.declaresException(RuntimeException.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(BAR))), is(false));
    }

    @Test
    public void testDeclaresGenericException() throws Exception {
        assertThat(ElementMatchers.declaresGenericException(GenericMethodType.class.getTypeParameters()[0])
                .matches(new MethodDescription.ForLoadedMethod(GenericMethodType.class.getDeclaredMethod(FOO, Exception.class))), is(true));
        assertThat(ElementMatchers.declaresGenericException(Exception.class)
                .matches(new MethodDescription.ForLoadedMethod(GenericMethodType.class.getDeclaredMethod(FOO, Exception.class))), is(false));
        assertThat(ElementMatchers.declaresException(Exception.class)
                .matches(new MethodDescription.ForLoadedMethod(GenericMethodType.class.getDeclaredMethod(FOO, Exception.class))), is(true));
    }

    @Test
    public void testCanThrow() throws Exception {
        assertThat(ElementMatchers.canThrow(IOException.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(FOO))), is(true));
        assertThat(ElementMatchers.canThrow(SQLException.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(FOO))), is(false));
        assertThat(ElementMatchers.canThrow(Error.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(FOO))), is(true));
        assertThat(ElementMatchers.canThrow(RuntimeException.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(FOO))), is(true));
        assertThat(ElementMatchers.canThrow(IOException.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(BAR))), is(false));
        assertThat(ElementMatchers.canThrow(SQLException.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(BAR))), is(false));
        assertThat(ElementMatchers.canThrow(Error.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(BAR))), is(true));
        assertThat(ElementMatchers.canThrow(RuntimeException.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(BAR))), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeclaresExceptionForNonThrowableType() throws Exception {
        ElementMatcher<Object> elementMatcher = (ElementMatcher) ElementMatchers.declaresException((Class) Object.class);
        assertThat(elementMatcher.matches(new Object()), is(false));
    }

    @Test
    public void testSortIsMethod() throws Exception {
        assertThat(ElementMatchers.isMethod().matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(true));
        assertThat(ElementMatchers.isMethod().matches(new MethodDescription.ForLoadedConstructor(Object.class.getDeclaredConstructor())), is(false));
        assertThat(ElementMatchers.isMethod().matches(new MethodDescription.Latent.TypeInitializer(mock(TypeDescription.class))), is(false));
    }

    @Test
    public void testSortIsConstructor() throws Exception {
        assertThat(ElementMatchers.isConstructor()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(false));
        assertThat(ElementMatchers.isConstructor()
                .matches(new MethodDescription.ForLoadedConstructor(Object.class.getDeclaredConstructor())), is(true));
        assertThat(ElementMatchers.isConstructor()
                .matches(new MethodDescription.Latent.TypeInitializer(mock(TypeDescription.class))), is(false));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testIsDefaultMethod() throws Exception {
        assertThat(ElementMatchers.isDefaultMethod().matches(new MethodDescription.ForLoadedMethod(Class.forName(SINGLE_DEFAULT_METHOD)
                .getDeclaredMethod(FOO))), is(true));
        assertThat(ElementMatchers.isDefaultMethod()
                .matches(new MethodDescription.ForLoadedMethod(Runnable.class.getDeclaredMethod("run"))), is(false));
    }

    @Test
    public void testSortIsTypeInitializer() throws Exception {
        assertThat(ElementMatchers.isTypeInitializer()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(false));
        assertThat(ElementMatchers.isTypeInitializer()
                .matches(new MethodDescription.ForLoadedConstructor(Object.class.getDeclaredConstructor())), is(false));
        assertThat(ElementMatchers.isTypeInitializer()
                .matches(new MethodDescription.Latent.TypeInitializer(mock(TypeDescription.class))), is(true));
    }

    @Test
    public void testSortIsBridge() throws Exception {
        assertThat(ElementMatchers.isBridge()
                .matches(new MethodDescription.ForLoadedMethod(GenericType.Extension.class.getDeclaredMethod("foo", Object.class))), is(true));
        assertThat(ElementMatchers.isBridge()
                .matches(new MethodDescription.ForLoadedMethod(GenericType.Extension.class.getDeclaredMethod("foo", Void.class))), is(false));
        assertThat(ElementMatchers.isBridge()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(false));
    }

    @Test
    public void testIsVirtual() throws Exception {
        assertThat(ElementMatchers.isVirtual().matches(new MethodDescription.ForLoadedMethod(IsVirtual.class.getDeclaredMethod("baz"))), is(true));
        assertThat(ElementMatchers.isVirtual().matches(new MethodDescription.ForLoadedMethod(IsVirtual.class.getDeclaredMethod("foo"))), is(false));
        assertThat(ElementMatchers.isVirtual().matches(new MethodDescription.ForLoadedMethod(IsVirtual.class.getDeclaredMethod("bar"))), is(false));
        assertThat(ElementMatchers.isVirtual().matches(new MethodDescription.ForLoadedMethod(IsVirtual.class.getDeclaredMethod("qux"))), is(true));
        assertThat(ElementMatchers.isVirtual().matches(new MethodDescription.ForLoadedConstructor(IsVirtual.class.getDeclaredConstructor())), is(false));
        assertThat(ElementMatchers.isVirtual().matches(new MethodDescription.Latent.TypeInitializer(TypeDescription.OBJECT)), is(false));
    }

    @Test
    public void testIsDefaultFinalizer() throws Exception {
        assertThat(ElementMatchers.isDefaultFinalizer()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("finalize"))), is(true));
        assertThat(ElementMatchers.isDefaultFinalizer()
                .matches(new MethodDescription.ForLoadedMethod(ObjectMethods.class.getDeclaredMethod("finalize"))), is(false));
        assertThat(ElementMatchers.isDefaultFinalizer()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(false));
    }

    @Test
    public void testIsFinalizer() throws Exception {
        assertThat(ElementMatchers.isFinalizer()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("finalize"))), is(true));
        assertThat(ElementMatchers.isFinalizer()
                .matches(new MethodDescription.ForLoadedMethod(ObjectMethods.class.getDeclaredMethod("finalize"))), is(true));
        assertThat(ElementMatchers.isFinalizer()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(false));
    }

    @Test
    public void testIsHashCode() throws Exception {
        assertThat(ElementMatchers.isHashCode()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("hashCode"))), is(true));
        assertThat(ElementMatchers.isHashCode()
                .matches(new MethodDescription.ForLoadedMethod(ObjectMethods.class.getDeclaredMethod("hashCode"))), is(true));
        assertThat(ElementMatchers.isHashCode()
                .matches(new MethodDescription.ForLoadedMethod(Runnable.class.getDeclaredMethod("run"))), is(false));
    }

    @Test
    public void testIsEquals() throws Exception {
        assertThat(ElementMatchers.isEquals()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("equals", Object.class))), is(true));
        assertThat(ElementMatchers.isEquals()
                .matches(new MethodDescription.ForLoadedMethod(ObjectMethods.class.getDeclaredMethod("equals", Object.class))), is(true));
        assertThat(ElementMatchers.isEquals()
                .matches(new MethodDescription.ForLoadedMethod(Runnable.class.getDeclaredMethod("run"))), is(false));
    }

    @Test
    public void testIsClone() throws Exception {
        assertThat(ElementMatchers.isClone()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("clone"))), is(true));
        assertThat(ElementMatchers.isClone()
                .matches(new MethodDescription.ForLoadedMethod(ObjectMethods.class.getDeclaredMethod("clone"))), is(true));
        assertThat(ElementMatchers.isClone()
                .matches(new MethodDescription.ForLoadedMethod(Runnable.class.getDeclaredMethod("run"))), is(false));
    }

    @Test
    public void testIsToString() throws Exception {
        assertThat(ElementMatchers.isToString()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(true));
        assertThat(ElementMatchers.isToString()
                .matches(new MethodDescription.ForLoadedMethod(ObjectMethods.class.getDeclaredMethod("toString"))), is(true));
        assertThat(ElementMatchers.isToString()
                .matches(new MethodDescription.ForLoadedMethod(Runnable.class.getDeclaredMethod("run"))), is(false));
    }

    @Test
    public void testIsDefaultConstructor() throws Exception {
        assertThat(ElementMatchers.isDefaultConstructor()
                .matches(new MethodDescription.ForLoadedConstructor(Object.class.getDeclaredConstructor())), is(true));
        assertThat(ElementMatchers.isDefaultConstructor()
                .matches(new MethodDescription.ForLoadedConstructor(String.class.getDeclaredConstructor(String.class))), is(false));
        assertThat(ElementMatchers.isDefaultConstructor()
                .matches(new MethodDescription.ForLoadedMethod(Runnable.class.getDeclaredMethod("run"))), is(false));
    }

    @Test
    public void testIsGetter() throws Exception {
        assertThat(ElementMatchers.isGetter()
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getFoo"))), is(false));
        assertThat(ElementMatchers.isGetter()
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("isQux"))), is(true));
        assertThat(ElementMatchers.isGetter()
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getQux"))), is(true));
        assertThat(ElementMatchers.isGetter()
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("isBar"))), is(true));
        assertThat(ElementMatchers.isGetter()
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getBar"))), is(true));
        assertThat(ElementMatchers.isGetter()
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("isBaz"))), is(false));
        assertThat(ElementMatchers.isGetter()
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getBaz"))), is(true));
        assertThat(ElementMatchers.isGetter()
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getBaz", Void.class))), is(false));
        assertThat(ElementMatchers.isGetter()
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("get"))), is(true));
        assertThat(ElementMatchers.isGetter()
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("is"))), is(true));
    }

    @Test
    public void testPropertyGetter() throws Exception {
        assertThat(ElementMatchers.isGetter("qux")
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getQux"))), is(true));
        assertThat(ElementMatchers.isGetter("bar")
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getQux"))), is(false));
        assertThat(ElementMatchers.isGetter("foo")
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getFoo"))), is(false));
        assertThat(ElementMatchers.isGetter("")
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("get"))), is(true));
        assertThat(ElementMatchers.isGetter("")
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("is"))), is(true));
    }

    @Test
    public void testIsSetter() throws Exception {
        assertThat(ElementMatchers.isSetter()
                .matches(new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setFoo"))), is(false));
        assertThat(ElementMatchers.isSetter()
                .matches(new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setBar", boolean.class))), is(true));
        assertThat(ElementMatchers.isSetter()
                .matches(new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setQux", Boolean.class))), is(true));
        assertThat(ElementMatchers.isSetter()
                .matches(new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setBaz", String.class))), is(true));
        assertThat(ElementMatchers.isSetter()
                .matches(new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setBaz", String.class, Void.class))), is(false));
        assertThat(ElementMatchers.isSetter()
                .matches(new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("set", Object.class))), is(true));
    }

    @Test
    public void testPropertySetter() throws Exception {
        assertThat(ElementMatchers.isSetter("foo")
                .matches(new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setFoo"))), is(false));
        assertThat(ElementMatchers.isSetter("qux")
                .matches(new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setQux", Boolean.class))), is(true));
        assertThat(ElementMatchers.isSetter("bar")
                .matches(new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setQux", Boolean.class))), is(false));
        assertThat(ElementMatchers.isSetter("")
                .matches(new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("set", Object.class))), is(true));
    }

    @Test
    public void testIsNonGenericGetter() throws Exception {
        assertThat(ElementMatchers.isGetter(String.class)
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getBaz"))), is(true));
        assertThat(ElementMatchers.isGetter(Void.class)
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getBaz"))), is(false));
        assertThat(ElementMatchers.isGetter(Object.class)
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getQuxbaz"))), is(true));
    }

    @Test
    public void testIsNonGenericSetter() throws Exception {
        assertThat(ElementMatchers.isSetter(String.class)
                .matches(new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setBaz", String.class))), is(true));
        assertThat(ElementMatchers.isSetter(Void.class)
                .matches(new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setBaz", String.class))), is(false));
        assertThat(ElementMatchers.isSetter(Object.class)
                .matches(new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setQuxbaz", Object.class))), is(true));
    }

    @Test
    public void testIsGenericGetter() throws Exception {
        assertThat(ElementMatchers.isGenericGetter(String.class)
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getBaz"))), is(true));
        assertThat(ElementMatchers.isGenericGetter(Void.class)
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getBaz"))), is(false));
        assertThat(ElementMatchers.isGenericGetter(Getters.class.getTypeParameters()[0])
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getQuxbaz"))), is(true));
        assertThat(ElementMatchers.isGenericGetter(Object.class)
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getQuxbaz"))), is(false));
    }

    @Test
    public void testIsGenericSetter() throws Exception {
        assertThat(ElementMatchers.isGenericSetter(String.class)
                .matches(new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setBaz", String.class))), is(true));
        assertThat(ElementMatchers.isGenericSetter(Void.class)
                .matches(new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setBaz", String.class))), is(false));
        assertThat(ElementMatchers.isGenericSetter(Setters.class.getTypeParameters()[0])
                .matches(new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setQuxbaz", Object.class))), is(true));
        assertThat(ElementMatchers.isGenericSetter(Object.class)
                .matches(new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setQuxbaz", Object.class))), is(false));
    }

    @Test
    public void testHasSignature() throws Exception {
        MethodDescription.SignatureToken signatureToken = new MethodDescription.SignatureToken("toString", TypeDescription.STRING, Collections.<TypeDescription>emptyList());
        assertThat(ElementMatchers.hasSignature(signatureToken)
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(true));
        assertThat(ElementMatchers.hasSignature(signatureToken)
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("hashCode"))), is(false));
    }

    @Test
    public void testIsSubOrSuperType() throws Exception {
        assertThat(ElementMatchers.isSubTypeOf(String.class).matches(TypeDescription.OBJECT), is(false));
        assertThat(ElementMatchers.isSubTypeOf(Object.class).matches(TypeDescription.STRING), is(true));
        assertThat(ElementMatchers.isSubTypeOf(Serializable.class).matches(TypeDescription.STRING), is(true));
        assertThat(ElementMatchers.isSuperTypeOf(Object.class).matches(TypeDescription.STRING), is(false));
        assertThat(ElementMatchers.isSuperTypeOf(String.class).matches(TypeDescription.OBJECT), is(true));
        assertThat(ElementMatchers.isSuperTypeOf(String.class).matches(new TypeDescription.ForLoadedType(Serializable.class)), is(true));
    }

    @Test
    public void testHasSuperType() throws Exception {
        assertThat(ElementMatchers.hasSuperType(ElementMatchers.is(Object.class)).matches(TypeDescription.STRING), is(true));
        assertThat(ElementMatchers.hasSuperType(ElementMatchers.is(String.class)).matches(TypeDescription.OBJECT), is(false));
        assertThat(ElementMatchers.hasSuperType(ElementMatchers.is(Serializable.class)).matches(TypeDescription.STRING), is(true));
        assertThat(ElementMatchers.hasSuperType(ElementMatchers.is(Serializable.class)).matches(TypeDescription.OBJECT), is(false));
    }

    @Test
    public void testIsAnnotatedInheritedWith() throws Exception {
        assertThat(ElementMatchers.inheritsAnnotation(OtherAnnotation.class)
                .matches(new TypeDescription.ForLoadedType(OtherInherited.class)), is(true));
        assertThat(ElementMatchers.isAnnotatedWith(OtherAnnotation.class)
                .matches(new TypeDescription.ForLoadedType(OtherInherited.class)), is(false));
    }

    @Test
    public void testTypeSort() throws Exception {
        assertThat(ElementMatchers.ofSort(TypeDefinition.Sort.NON_GENERIC).matches(TypeDescription.OBJECT), is(true));
        assertThat(ElementMatchers.ofSort(TypeDefinition.Sort.VARIABLE).matches(TypeDescription.OBJECT), is(false));
    }

    @Test
    public void testDeclaresField() throws Exception {
        assertThat(ElementMatchers.declaresField(ElementMatchers.isAnnotatedWith(OtherAnnotation.class))
                .matches(new TypeDescription.ForLoadedType(DeclaresFieldOrMethod.class)), is(true));
        assertThat(ElementMatchers.declaresField(ElementMatchers.isAnnotatedWith(OtherAnnotation.class))
                .matches(TypeDescription.OBJECT), is(false));
        assertThat(ElementMatchers.declaresMethod(ElementMatchers.isAnnotatedWith(OtherAnnotation.class))
                .matches(new TypeDescription.ForLoadedType(DeclaresFieldOrMethod.class)), is(true));
        assertThat(ElementMatchers.declaresMethod(ElementMatchers.isAnnotatedWith(OtherAnnotation.class))
                .matches(TypeDescription.OBJECT), is(false));
    }

    @Test
    public void testFieldType() throws Exception {
        assertThat(ElementMatchers.fieldType(GenericFieldType.class).matches(new FieldDescription.ForLoadedField(GenericFieldType.class.getDeclaredField(FOO))), is(true));
        assertThat(ElementMatchers.fieldType(Object.class).matches(new FieldDescription.ForLoadedField(GenericFieldType.class.getDeclaredField(FOO))), is(false));
    }

    @Test
    public void testGenericFieldType() throws Exception {
        assertThat(ElementMatchers.genericFieldType(GenericFieldType.class.getTypeParameters()[0])
                .matches(new FieldDescription.ForLoadedField(GenericFieldType.class.getDeclaredField(BAR))), is(true));
        assertThat(ElementMatchers.genericFieldType(Object.class)
                .matches(new FieldDescription.ForLoadedField(GenericFieldType.class.getDeclaredField(BAR))), is(false));
        assertThat(ElementMatchers.fieldType(Object.class)
                .matches(new FieldDescription.ForLoadedField(GenericFieldType.class.getDeclaredField(BAR))), is(true));
    }

    @Test
    public void testIsBootstrapClassLoader() throws Exception {
        assertThat(ElementMatchers.isBootstrapClassLoader().matches(null), is(true));
        assertThat(ElementMatchers.isBootstrapClassLoader().matches(mock(ClassLoader.class)), is(false));
    }

    @Test
    public void testIsSystemClassLoader() throws Exception {
        assertThat(ElementMatchers.isSystemClassLoader().matches(ClassLoader.getSystemClassLoader()), is(true));
        assertThat(ElementMatchers.isSystemClassLoader().matches(null), is(false));
        assertThat(ElementMatchers.isSystemClassLoader().matches(ClassLoader.getSystemClassLoader().getParent()), is(false));
        assertThat(ElementMatchers.isSystemClassLoader().matches(mock(ClassLoader.class)), is(false));
    }

    @Test
    public void testIsExtensionClassLoader() throws Exception {
        assertThat(ElementMatchers.isExtensionClassLoader().matches(ClassLoader.getSystemClassLoader().getParent()), is(true));
        assertThat(ElementMatchers.isExtensionClassLoader().matches(ClassLoader.getSystemClassLoader()), is(false));
        assertThat(ElementMatchers.isExtensionClassLoader().matches(null), is(false));
        assertThat(ElementMatchers.isExtensionClassLoader().matches(mock(ClassLoader.class)), is(false));
    }

    @Test
    public void testIsChildOf() throws Exception {
        ClassLoader parent = new URLClassLoader(new URL[0], null);
        assertThat(ElementMatchers.isChildOf(parent).matches(new URLClassLoader(new URL[0], parent)), is(true));
        assertThat(ElementMatchers.isChildOf(parent).matches(new URLClassLoader(new URL[0], null)), is(false));
        assertThat(ElementMatchers.isChildOf(parent).matches(null), is(false));
        assertThat(ElementMatchers.isChildOf(null).matches(mock(ClassLoader.class)), is(true));
    }

    @Test
    public void testIsParentOf() throws Exception {
        ClassLoader parent = new URLClassLoader(new URL[0], null);
        assertThat(ElementMatchers.isParentOf(new URLClassLoader(new URL[0], parent)).matches(parent), is(true));
        assertThat(ElementMatchers.isParentOf(new URLClassLoader(new URL[0], null)).matches(parent), is(false));
        assertThat(ElementMatchers.isParentOf(null).matches(new URLClassLoader(new URL[0], null)), is(false));
        assertThat(ElementMatchers.isParentOf(null).matches(null), is(true));
        assertThat(ElementMatchers.isParentOf(mock(ClassLoader.class)).matches(null), is(true));
    }

    @Test
    public void testOfType() throws Exception {
        ClassLoader classLoader = new URLClassLoader(new URL[0], null);
        assertThat(ElementMatchers.ofType(ElementMatchers.is(URLClassLoader.class)).matches(classLoader), is(true));
        assertThat(ElementMatchers.ofType(ElementMatchers.is(ClassLoader.class)).matches(classLoader), is(false));
        assertThat(ElementMatchers.ofType(ElementMatchers.is(URLClassLoader.class)).matches(null), is(false));
    }

    @Test
    public void testSupportsModules() throws Exception {
        assertThat(ElementMatchers.supportsModules().matches(mock(JavaModule.class)), is(true));
        assertThat(ElementMatchers.supportsModules().matches(null), is(false));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConstructorIsHidden() throws Exception {
        assertThat(Modifier.isPrivate(ElementMatchers.class.getDeclaredConstructor().getModifiers()), is(true));
        Constructor<?> constructor = ElementMatchers.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            throw (UnsupportedOperationException) exception.getCause();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface IsAnnotatedWithAnnotation {
        /* empty */
    }

    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    public @interface OtherAnnotation {
        /* empty */
    }

    public interface GenericMethodType<T extends Exception> {

        T foo(T t) throws T;

        interface Inner extends GenericMethodType<RuntimeException> {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public interface GenericDeclaredBy<T> {

        void foo();

        interface Inner extends GenericDeclaredBy<String> {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class FieldSample {

        String foo;

        Object bar;

        volatile Object qux;

        transient Object baz;
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

    @SuppressWarnings("unused")
    private abstract static class IsEqual {

        abstract void foo();
    }

    @SuppressWarnings("unused")
    private abstract static class Returns {

        abstract void foo();

        abstract String bar();
    }

    @SuppressWarnings("unused")
    private abstract static class TakesArguments {

        abstract void foo(Void a);

        abstract void bar(String a, int b);
    }

    private abstract static class CanThrow {

        protected abstract void foo() throws IOException;

        protected abstract void bar();
    }

    public static class GenericType<T> {

        public void foo(T t) {
            /* empty */
        }

        public static class Extension extends GenericType<Void> {

            @Override
            public void foo(Void t) {
            /* empty */
            }
        }
    }

    private static class ObjectMethods {

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return super.equals(other);
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public String toString() {
            return super.toString();
        }

        @Override
        @SuppressWarnings("deprecation")
        protected void finalize() throws Throwable {
            super.finalize();
        }
    }

    @SuppressWarnings("unused")
    public static class IsVirtual {

        public static void bar() {
            /* empty */
        }

        private void foo() {
            /* empty */
        }

        public final void qux() {
            /* empty */
        }

        public void baz() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class Getters<T> {

        public void getFoo() {
            /* empty */
        }

        public Boolean isBar() {
            return null;
        }

        public boolean isQux() {
            return false;
        }

        public Boolean getBar() {
            return null;
        }

        public boolean getQux() {
            return false;
        }

        public String isBaz() {
            return null;
        }

        public String getBaz() {
            return null;
        }

        public String getBaz(Void argument) {
            return null;
        }

        public T getQuxbaz() {
            return null;
        }

        public Object get() {
            return null;
        }

        public boolean is() {
            return false;
        }
    }

    @SuppressWarnings("unused")
    public static class Setters<T> {

        public void setFoo() {
            /* empty */
        }

        public void setBar(boolean argument) {
            /* empty */
        }

        public void setQux(Boolean argument) {
            /* empty */
        }

        public void setBaz(String argument) {
            /* empty */
        }

        public void setBaz(String argument, Void argument2) {
            /* empty */
        }

        public void setQuxbaz(T argument) {
            /* empty */
        }

        public void set(Object argument) {
            /* empty */
        }
    }

    @OtherAnnotation
    public static class Other {
        /* empty */
    }

    public static class OtherInherited extends Other {
        /* empty */
    }

    @SuppressWarnings("unused")
    public static class DeclaresFieldOrMethod {

        @OtherAnnotation
        Void field;

        @OtherAnnotation
        void method() {

        }
    }

    @SuppressWarnings("unused")
    public static class GenericFieldType<T> {

        GenericFieldType<?> foo;

        T bar;

        public static class Inner extends GenericFieldType<Void> {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class GenericConstructorType<T extends Exception> {

        GenericConstructorType(T t) throws T {
            /* empty */
        }

        public static class Inner extends GenericConstructorType<RuntimeException> {

            public Inner(RuntimeException exception) throws RuntimeException {
                super(exception);
            }
        }
    }
}
