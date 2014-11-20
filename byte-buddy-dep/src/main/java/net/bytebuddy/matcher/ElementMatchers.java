package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.ByteCodeElement;
import net.bytebuddy.instrumentation.ModifierReviewable;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotatedElement;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.RuntimeType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;
import static net.bytebuddy.utility.ByteBuddyCommons.nonVoid;

public class ElementMatchers {

    public static <T> ElementMatcher.Junction<T> not(ElementMatcher<? super T> elementMatcher) {
        return new NegatingMatcher<T>(nonNull(elementMatcher));
    }

    public static <T> ElementMatcher.Junction<T> any() {
        return new BooleanMatcher<T>(true);
    }

    public static <T> ElementMatcher.Junction<T> none() {
        return new BooleanMatcher<T>(false);
    }

    public static <T> ElementMatcher.Junction<T> anyOf(T... value) {
        return anyOf(Arrays.asList(value));
    }

    public static <T> ElementMatcher.Junction<T> anyOf(List<? extends T> values) {
        ElementMatcher.Junction<T> matcher = none();
        for (T value : values) {
            matcher = matcher.or(new EqualityMatcher<T>(value));
        }
        return matcher;
    }

    public static <T> ElementMatcher.Junction<T> noneOf(T... value) {
        return noneOf(Arrays.asList(value));
    }

    public static <T> ElementMatcher.Junction<T> noneOf(List<? extends T> values) {
        ElementMatcher.Junction<T> matcher = any();
        for (T value : values) {
            matcher = matcher.and(not(new EqualityMatcher<T>(value)));
        }
        return matcher;
    }

    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> named(String name) {
        return new NameMatcher<T>(new StringMatcher(nonNull(name), StringMatcher.Mode.EQUALS_FULLY));
    }

    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> namedIgnoreCase(String name) {
        return new NameMatcher<T>(new StringMatcher(nonNull(name), StringMatcher.Mode.EQUALS_FULLY_IGNORE_CASE));
    }

    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> nameStartsWith(String prefix) {
        return new NameMatcher<T>(new StringMatcher(nonNull(prefix), StringMatcher.Mode.STARTS_WITH));
    }

    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> nameStartsWithIgnoreCase(String prefix) {
        return new NameMatcher<T>(new StringMatcher(nonNull(prefix), StringMatcher.Mode.STARTS_WITH_IGNORE_CASE));
    }

    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> nameEndsWith(String suffix) {
        return new NameMatcher<T>(new StringMatcher(nonNull(suffix), StringMatcher.Mode.ENDS_WITH));
    }

    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> nameEndsWithIgnoreCase(String suffix) {
        return new NameMatcher<T>(new StringMatcher(nonNull(suffix), StringMatcher.Mode.ENDS_WITH_IGNORE_CASE));
    }

    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> nameContains(String infix) {
        return new NameMatcher<T>(new StringMatcher(nonNull(infix), StringMatcher.Mode.CONTAINS));
    }

    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> nameContainsIgnoreCase(String infix) {
        return new NameMatcher<T>(new StringMatcher(nonNull(infix), StringMatcher.Mode.CONTAINS_IGNORE_CASE));
    }

    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> nameMatches(String regex) {
        return new NameMatcher<T>(new StringMatcher(nonNull(regex), StringMatcher.Mode.MATCHES));
    }

    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> isDeclaredBy(TypeDescription type) {
        return isDeclaredBy(is(nonNull(type)));
    }

    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> isDeclaredBy(Class<?> type) {
        return isDeclaredBy(new TypeDescription.ForLoadedType(nonNull(type)));
    }

    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> isDeclaredBy(ElementMatcher<? super TypeDescription> typeMatcher) {
        return new DeclaringTypeMatcher<T>(nonNull(typeMatcher));
    }

    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> isVisibleTo(Class<?> type) {
        return isVisibleTo(new TypeDescription.ForLoadedType(nonNull(type)));
    }

    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> isVisibleTo(TypeDescription typeDescription) {
        return new VisibilityMatcher<T>(typeDescription);
    }

    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> hasDescriptor(String methodDescriptor) {
        return new DescriptorMatcher<T>(new StringMatcher(methodDescriptor, StringMatcher.Mode.EQUALS_FULLY));
    }

    public static <T extends AnnotatedElement> ElementMatcher.Junction<T> isAnnotatedWith(TypeDescription typeDescription) {
        return isAnnotatedWith(is(typeDescription));
    }

    public static <T extends AnnotatedElement> ElementMatcher.Junction<T> isAnnotatedWith(Class<?> type) {
        return isAnnotatedWith(is(type));
    }

    public static <T extends AnnotatedElement> ElementMatcher.Junction<T> isAnnotatedWith(ElementMatcher<? super TypeDescription> annotationTypeMatcher) {
        return hasAnnotation(new AnnotationTypeMatcher<AnnotationDescription>(annotationTypeMatcher));
    }

    public static <T extends AnnotatedElement> ElementMatcher.Junction<T> hasAnnotation(ElementMatcher<? super AnnotationDescription> annotationDescription) {
        return new AnnotationMatcher<T>(new ListItemMatcher<AnnotationDescription>(annotationDescription, ListItemMatcher.Mode.MATCH_ANY));
    }

    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isPublic() {
        return new ModifierMatcher<T>(ModifierMatcher.MatchMode.PUBLIC);
    }

    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isProtected() {
        return new ModifierMatcher<T>(ModifierMatcher.MatchMode.PROTECTED);
    }

    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isPackagePrivate() {
        return not(isPublic().or(isProtected()).or(isPrivate()));
    }

    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isPrivate() {
        return new ModifierMatcher<T>(ModifierMatcher.MatchMode.PRIVATE);
    }

    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isFinal() {
        return new ModifierMatcher<T>(ModifierMatcher.MatchMode.FINAL);
    }

    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isStatic() {
        return new ModifierMatcher<T>(ModifierMatcher.MatchMode.STATIC);
    }

    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isSynchronized() {
        return new ModifierMatcher<T>(ModifierMatcher.MatchMode.SYNCHRONIZED);
    }

    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isNative() {
        return new ModifierMatcher<T>(ModifierMatcher.MatchMode.NATIVE);
    }

    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isStrict() {
        return new ModifierMatcher<T>(ModifierMatcher.MatchMode.STRICT);
    }

    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isVarArgs() {
        return new ModifierMatcher<T>(ModifierMatcher.MatchMode.VAR_ARGS);
    }

    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isSynthetic() {
        return new ModifierMatcher<T>(ModifierMatcher.MatchMode.SYNTHETIC);
    }

    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isBridge() {
        return new ModifierMatcher<T>(ModifierMatcher.MatchMode.BRIDGE);
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> is(T methodDescription) {
        return new EqualityMatcher<T>(nonNull(methodDescription));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> is(Method method) {
        return new EqualityMatcher<T>(nonNull(new MethodDescription.ForLoadedMethod(nonNull(method))));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> is(Constructor<?> constructor) {
        return new EqualityMatcher<T>(nonNull(new MethodDescription.ForLoadedConstructor(nonNull(constructor))));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> returns(Class<?> type) {
        return returns(is(nonNull(type)));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> returns(TypeDescription typeDescription) {
        return returns(is(nonNull(typeDescription)));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> returns(ElementMatcher<? super TypeDescription> typeMatcher) {
        return new MethodReturnTypeMatcher<T>(nonNull(typeMatcher));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesArguments(Class<?>... types) {
        TypeDescription[] typeDescription = new TypeDescription[types.length];
        int index = 0;
        for (Class<?> type : types) {
            typeDescription[index++] = new TypeDescription.ForLoadedType(type);
        }
        return takesArguments(typeDescription);
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesArguments(TypeDescription... typeDescriptions) {
        return takesArguments((Arrays.asList(typeDescriptions)));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesArguments(List<? extends TypeDescription> typeDescriptions) {
        List<ElementMatcher<? super TypeDescription>> typeMatchers = new ArrayList<ElementMatcher<? super TypeDescription>>(typeDescriptions.size());
        for (TypeDescription typeDescription : typeDescriptions) {
            typeMatchers.add(is(nonVoid(typeDescription)));
        }
        return takesArguments(new ListOneToOneMatcher<TypeDescription>(typeMatchers));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesArguments(ElementMatcher<? super List<? extends TypeDescription>> parameterMatcher) {
        return new MethodParameterTypeMatcher<T>(nonNull(parameterMatcher));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesArguments(int length) {
        return new MethodParameterLengthMatcher<T>(length);
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> canThrow(Class<? extends Throwable> exceptionType) {
        return canThrow(new TypeDescription.ForLoadedType(nonNull(exceptionType)));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> canThrow(TypeDescription exceptionType) {
        if (exceptionType.isAssignableTo(Throwable.class)) {
            return exceptionType.isAssignableTo(RuntimeType.class) || exceptionType.isAssignableTo(Error.class)
                    ? new BooleanMatcher<T>(true)
                    : ElementMatchers.<T>canThrow(new ListItemMatcher<TypeDescription>(new SubTypeMatcher<TypeDescription>(exceptionType),
                    ListItemMatcher.Mode.MATCH_ANY));
        } else {
            throw new IllegalArgumentException(exceptionType + " is not an exception type");
        }
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> canThrow(ElementMatcher<? super List<? extends TypeDescription>> exceptionMatcher) {
        return new MethodExceptionTypeMatcher<T>(exceptionMatcher);
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> isMethod() {
        return new MethodSortMatcher<T>(MethodSortMatcher.Sort.METHOD);
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> isConstructor() {
        return new MethodSortMatcher<T>(MethodSortMatcher.Sort.CONSTRUCTOR);
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> isTypeInitializer() {
        return new MethodSortMatcher<T>(MethodSortMatcher.Sort.TYPE_INITIALIZER);
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> isVisibilityBridge() {
        return new MethodSortMatcher<T>(MethodSortMatcher.Sort.VISIBILITY_BRIDGE);
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> isOverridable() {
        return new MethodSortMatcher<T>(MethodSortMatcher.Sort.OVERRIDABLE);
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> isDefaultMethod() {
        return new MethodSortMatcher<T>(MethodSortMatcher.Sort.DEFAULT_METHOD);
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> isDefaultFinalizer() {
        return isFinalizer().and(isDeclaredBy(Object.class));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> isFinalizer() {
        return named("finalize").and(takesArguments(0)).and(returns(void.class));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> isHashCode() {
        return named("hashCode").and(takesArguments(0)).and(returns(int.class));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> isEquals() {
        return named("equals").and(takesArguments(Object.class)).and(returns(boolean.class));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> isToString() {
        return named("toString").and(takesArguments(0)).and(returns(String.class));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> isSetter() {
        return nameStartsWith("set").and(takesArguments(1)).and(returns(void.class));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> isSetter(Class<?> type) {
        return isSetter(new TypeDescription.ForLoadedType(nonNull(type)));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> isSetter(TypeDescription type) {
        return isSetter().and(takesArguments(type));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> isGetter() {
        return takesArguments(0).and(not(returns(void.class))).and(nameStartsWith("get")
                .or(nameStartsWith("is").and(returns(boolean.class).or(returns(Boolean.class)))));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> isGetter(Class<?> type) {
        return isGetter(new TypeDescription.ForLoadedType(nonNull(type)));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> isGetter(TypeDescription type) {
        return isGetter().and(returns(type));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> isCompatibleTo(MethodDescription methodDescription) {
        TypeList parameterTypes = methodDescription.getParameterTypes();
        List<ElementMatcher<TypeDescription>> matchers = new ArrayList<ElementMatcher<TypeDescription>>(parameterTypes.size());
        for (TypeDescription typeDescription : parameterTypes) {
            matchers.add(isSubTypeOf(typeDescription));
        }
        return (methodDescription.isStatic() ? isStatic() : not(isStatic()))
                .and(named(methodDescription.getName()))
                .and(returns(isSubTypeOf(methodDescription.getReturnType())))
                .and(takesArguments(new ListOneToOneMatcher<TypeDescription>(matchers)));
    }

    public static <T extends TypeDescription> ElementMatcher.Junction<T> is(T typeDescription) {
        return new EqualityMatcher<T>(nonNull(typeDescription));
    }

    public static <T extends TypeDescription> ElementMatcher.Junction<T> is(Class<?> type) {
        return new EqualityMatcher<T>(new TypeDescription.ForLoadedType(nonNull(type)));
    }

    public static <T extends TypeDescription> ElementMatcher.Junction<T> isSubTypeOf(Class<?> type) {
        return isSubTypeOf(new TypeDescription.ForLoadedType(nonNull(type)));
    }

    public static <T extends TypeDescription> ElementMatcher.Junction<T> isSubTypeOf(TypeDescription typeDescription) {
        return new SubTypeMatcher<T>(nonNull(typeDescription));
    }

    public static <T extends TypeDescription> ElementMatcher.Junction<T> isSuperTypeOf(Class<?> type) {
        return isSuperTypeOf(new TypeDescription.ForLoadedType(nonNull(type)));
    }

    public static <T extends TypeDescription> ElementMatcher.Junction<T> isSuperTypeOf(TypeDescription typeDescription) {
        return new SuperTypeMatcher<T>(nonNull(typeDescription));
    }

    private ElementMatchers() {
        throw new UnsupportedOperationException();
    }
}
