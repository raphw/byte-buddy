package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.ByteCodeElement;
import net.bytebuddy.instrumentation.ModifierReviewable;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.RuntimeType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;
import static net.bytebuddy.utility.ByteBuddyCommons.nonVoid;

public class ElementMatchers {

    public static <T> ElementMatcher.Junction<T> not(ElementMatcher<T> elementMatcher) {
        return new NegatingMatcher<T>(nonNull(elementMatcher));
    }

    public static ElementMatcher.Junction<Object> any() {
        return new BooleanMatcher<Object>(true);
    }

    public static ElementMatcher.Junction<Object> none() {
        return new BooleanMatcher<Object>(false);
    }

    public static ElementMatcher.Junction<TypeDescription> is(TypeDescription typeDescription) {
        return new EqualityMatcher<TypeDescription>(nonNull(typeDescription));
    }

    public static ElementMatcher.Junction<TypeDescription> is(Class<?> type) {
        return new EqualityMatcher<TypeDescription>(new TypeDescription.ForLoadedType(nonNull(type)));
    }

    public static ElementMatcher.Junction<MethodDescription> is(MethodDescription methodDescription) {
        return new EqualityMatcher<MethodDescription>(nonNull(methodDescription));
    }

    public static ElementMatcher.Junction<MethodDescription> is(Method method) {
        return new EqualityMatcher<MethodDescription>((new MethodDescription.ForLoadedMethod(nonNull(method))));
    }

    public static ElementMatcher.Junction<MethodDescription> is(Constructor<?> constructor) {
        return new EqualityMatcher<MethodDescription>((new MethodDescription.ForLoadedConstructor(nonNull(constructor))));
    }

    public static ElementMatcher.Junction<ByteCodeElement> named(String name) {
        return new NameMatcher<ByteCodeElement>(new StringMatcher(nonNull(name), StringMatcher.Mode.EQUALS_FULLY));
    }

    public static ElementMatcher.Junction<ByteCodeElement> namedIgnoreCase(String name) {
        return new NameMatcher<ByteCodeElement>(new StringMatcher(nonNull(name), StringMatcher.Mode.EQUALS_FULLY_IGNORE_CASE));
    }

    public static ElementMatcher.Junction<ByteCodeElement> nameStartsWith(String prefix) {
        return new NameMatcher<ByteCodeElement>(new StringMatcher(nonNull(prefix), StringMatcher.Mode.STARTS_WITH));
    }

    public static ElementMatcher.Junction<ByteCodeElement> nameStartsWithIgnoreCase(String prefix) {
        return new NameMatcher<ByteCodeElement>(new StringMatcher(nonNull(prefix), StringMatcher.Mode.STARTS_WITH_IGNORE_CASE));
    }

    public static ElementMatcher.Junction<ByteCodeElement> nameEndsWith(String suffix) {
        return new NameMatcher<ByteCodeElement>(new StringMatcher(nonNull(suffix), StringMatcher.Mode.ENDS_WITH));
    }

    public static ElementMatcher.Junction<ByteCodeElement> nameEndsWithIgnoreCase(String suffix) {
        return new NameMatcher<ByteCodeElement>(new StringMatcher(nonNull(suffix), StringMatcher.Mode.ENDS_WITH_IGNORE_CASE));
    }

    public static ElementMatcher.Junction<ByteCodeElement> nameContains(String infix) {
        return new NameMatcher<ByteCodeElement>(new StringMatcher(nonNull(infix), StringMatcher.Mode.CONTAINS));
    }

    public static ElementMatcher.Junction<ByteCodeElement> nameContainsIgnoreCase(String infix) {
        return new NameMatcher<ByteCodeElement>(new StringMatcher(nonNull(infix), StringMatcher.Mode.CONTAINS_IGNORE_CASE));
    }

    public static ElementMatcher.Junction<ByteCodeElement> nameMatches(String regex) {
        return new NameMatcher<ByteCodeElement>(new StringMatcher(nonNull(regex), StringMatcher.Mode.MATCHES));
    }

    public static ElementMatcher.Junction<ModifierReviewable> isPublic() {
        return new ModifierMatcher<ModifierReviewable>(ModifierMatcher.MatchMode.PUBLIC);
    }

    public static ElementMatcher.Junction<ModifierReviewable> isProtected() {
        return new ModifierMatcher<ModifierReviewable>(ModifierMatcher.MatchMode.PROTECTED);
    }

    public static ElementMatcher.Junction<ModifierReviewable> isPackagePrivate() {
        return not(isPublic().or(isProtected()).or(isPrivate()));
    }

    public static ElementMatcher.Junction<ModifierReviewable> isPrivate() {
        return new ModifierMatcher<ModifierReviewable>(ModifierMatcher.MatchMode.PRIVATE);
    }

    public static ElementMatcher.Junction<ModifierReviewable> isFinal() {
        return new ModifierMatcher<ModifierReviewable>(ModifierMatcher.MatchMode.FINAL);
    }

    public static ElementMatcher.Junction<ModifierReviewable> isStatic() {
        return new ModifierMatcher<ModifierReviewable>(ModifierMatcher.MatchMode.STATIC);
    }

    public static ElementMatcher.Junction<ModifierReviewable> isSynchronized() {
        return new ModifierMatcher<ModifierReviewable>(ModifierMatcher.MatchMode.SYNCHRONIZED);
    }

    public static ElementMatcher.Junction<ModifierReviewable> isNative() {
        return new ModifierMatcher<ModifierReviewable>(ModifierMatcher.MatchMode.NATIVE);
    }

    public static ElementMatcher.Junction<ModifierReviewable> isStrict() {
        return new ModifierMatcher<ModifierReviewable>(ModifierMatcher.MatchMode.STRICT);
    }

    public static ElementMatcher.Junction<ModifierReviewable> isVarArgs() {
        return new ModifierMatcher<ModifierReviewable>(ModifierMatcher.MatchMode.VAR_ARGS);
    }

    public static ElementMatcher.Junction<ModifierReviewable> isSynthetic() {
        return new ModifierMatcher<ModifierReviewable>(ModifierMatcher.MatchMode.SYNTHETIC);
    }

    public static ElementMatcher.Junction<ModifierReviewable> isBridge() {
        return new ModifierMatcher<ModifierReviewable>(ModifierMatcher.MatchMode.BRIDGE);
    }

    public static ElementMatcher.Junction<ByteCodeElement> isDeclaredBy(TypeDescription type) {
        return isDeclaredBy(is(nonNull(type)));
    }

    public static ElementMatcher.Junction<ByteCodeElement> isDeclaredBy(Class<?> type) {
        return isDeclaredBy(new TypeDescription.ForLoadedType(nonNull(type)));
    }

    public static ElementMatcher.Junction<ByteCodeElement> isDeclaredBy(ElementMatcher<? super TypeDescription> typeMatcher) {
        return new DeclaringTypeMatcher<ByteCodeElement>(nonNull(typeMatcher));
    }

    public static ElementMatcher.Junction<ByteCodeElement> isVisibleTo(Class<?> type) {
        return isVisibleTo(new TypeDescription.ForLoadedType(nonNull(type)));
    }

    public static ElementMatcher.Junction<ByteCodeElement> isVisibleTo(TypeDescription typeDescription) {
        return new VisibilityMatcher<ByteCodeElement>(typeDescription);
    }

    public static ElementMatcher.Junction<MethodDescription> returns(Class<?> type) {
        return returns(is(nonNull(type)));
    }

    public static ElementMatcher.Junction<MethodDescription> returns(TypeDescription typeDescription) {
        return returns(is(nonNull(typeDescription)));
    }

    public static ElementMatcher.Junction<MethodDescription> returns(ElementMatcher<? super TypeDescription> typeMatcher) {
        return new MethodReturnTypeMatcher(nonNull(typeMatcher));
    }

    public static ElementMatcher.Junction<MethodDescription> takesArguments(Class<?>... types) {
        TypeDescription[] typeDescription = new TypeDescription[types.length];
        int index = 0;
        for (Class<?> type : types) {
            typeDescription[index++] = new TypeDescription.ForLoadedType(type);
        }
        return takesArguments(typeDescription);
    }

    public static ElementMatcher.Junction<MethodDescription> takesArguments(TypeDescription... typeDescriptions) {
        List<ElementMatcher<? super TypeDescription>> typeMatchers = new ArrayList<ElementMatcher<? super TypeDescription>>(typeDescriptions.length);
        for (TypeDescription typeDescription : typeDescriptions) {
            typeMatchers.add(is(nonVoid(typeDescription)));
        }
        return takesArguments(new ListOneToOneMatcher<TypeDescription>(typeMatchers));
    }

    public static ElementMatcher.Junction<MethodDescription> takesArguments(ElementMatcher<? super List<? extends TypeDescription>> parameterMatcher) {
        return new MethodParameterTypeMatcher<MethodDescription>(nonNull(parameterMatcher));
    }

    public static ElementMatcher.Junction<MethodDescription> takesArguments(int length) {
        return new MethodParameterLengthMatcher<MethodDescription>(length);
    }

    public static ElementMatcher.Junction<MethodDescription> canThrow(Class<? extends Throwable> exceptionType) {
        return canThrow(new TypeDescription.ForLoadedType(nonNull(exceptionType)));
    }

    public static ElementMatcher.Junction<MethodDescription> canThrow(TypeDescription exceptionType) {
        if (exceptionType.isAssignableTo(Throwable.class)) {
            return exceptionType.isAssignableTo(RuntimeType.class) || exceptionType.isAssignableTo(Error.class)
                    ? new BooleanMatcher<MethodDescription>(true)
                    : canThrow(new ListItemMatcher<TypeDescription>(new SubTypeMatcher<TypeDescription>(exceptionType), ListItemMatcher.Mode.MATCH_ANY));
        } else {
            throw new IllegalArgumentException(exceptionType + " is not an exception type");
        }
    }

    public static ElementMatcher.Junction<MethodDescription> canThrow(ElementMatcher<? super List<? extends TypeDescription>> exceptionMatcher) {
        return new MethodExceptionTypeMatcher<MethodDescription>(exceptionMatcher);
    }

    public static ElementMatcher.Junction<ByteCodeElement> hasDescriptor(String methodDescriptor) {
        return new DescriptorMatcher<ByteCodeElement>(new StringMatcher(methodDescriptor, StringMatcher.Mode.EQUALS_FULLY));
    }

    public static ElementMatcher.Junction<MethodDescription> isMethod() {
        return new MethodSortMatcher<MethodDescription>(MethodSortMatcher.Sort.METHOD);
    }

    public static ElementMatcher.Junction<MethodDescription> isConstructor() {
        return new MethodSortMatcher<MethodDescription>(MethodSortMatcher.Sort.CONSTRUCTOR);
    }

    public static ElementMatcher.Junction<MethodDescription> isTypeInitializer() {
        return new MethodSortMatcher<MethodDescription>(MethodSortMatcher.Sort.TYPE_INITIALIZER);
    }

    public static ElementMatcher.Junction<MethodDescription> isVisibilityBridge() {
        return new MethodSortMatcher<MethodDescription>(MethodSortMatcher.Sort.VISIBILITY_BRIDGE);
    }

    public static ElementMatcher.Junction<MethodDescription> isOverridable() {
        return new MethodSortMatcher<MethodDescription>(MethodSortMatcher.Sort.OVERRIDABLE);
    }

    public static ElementMatcher.Junction<MethodDescription> isDefaultFinalizer() {
        return isFinalizer().and(isDeclaredBy(Object.class));
    }

    public static ElementMatcher.Junction<MethodDescription> isFinalizer() {
        return named("finalize").and(takesArguments(0)).and(returns(void.class));
    }

    public static ElementMatcher.Junction<MethodDescription> isHashCode() {
        return named("hashCode").and(takesArguments(0)).and(returns(int.class));
    }

    public static ElementMatcher.Junction<MethodDescription> isEquals() {
        return named("equals").and(takesArguments(Object.class)).and(returns(boolean.class));
    }

    public static ElementMatcher.Junction<MethodDescription> isToString() {
        return named("toString").and(takesArguments(0)).and(returns(String.class));
    }

    public static ElementMatcher.Junction<MethodDescription> isSetter() {
        return nameStartsWith("set").and(takesArguments(1)).and(returns(void.class));
    }

    public static ElementMatcher.Junction<MethodDescription> isSetter(Class<?> type) {
        return isSetter(new TypeDescription.ForLoadedType(nonNull(type)));
    }

    public static ElementMatcher.Junction<MethodDescription> isSetter(TypeDescription type) {
        return isSetter().and(takesArguments(type));
    }

    public static ElementMatcher.Junction<MethodDescription> isGetter() {
        return takesArguments(0).and(not(returns(void.class))).and(nameStartsWith("get")
                .or(nameStartsWith("is").and(returns(boolean.class).or(returns(Boolean.class)))));
    }

    public static ElementMatcher.Junction<MethodDescription> isGetter(Class<?> type) {
        return isGetter(new TypeDescription.ForLoadedType(nonNull(type)));
    }

    public static ElementMatcher.Junction<MethodDescription> isGetter(TypeDescription type) {
        return isGetter().and(returns(type));
    }

    public static ElementMatcher.Junction<MethodDescription> isCompatibleTo(MethodDescription methodDescription) {
        TypeList parameterTypes = methodDescription.getParameterTypes();
        List<ElementMatcher<TypeDescription>> matchers = new ArrayList<ElementMatcher<TypeDescription>>(parameterTypes.size());
        for (TypeDescription typeDescription : parameterTypes) {
            matchers.add(isSuperTypeOf(typeDescription));
        }
        return (methodDescription.isStatic() ? isStatic() : not(isStatic()))
                .and(returns(isSubTypeOf(methodDescription.getReturnType())))
                .and(takesArguments(new ListOneToOneMatcher<TypeDescription>(matchers)))
                .and(named(methodDescription.getName()));
    }

    public static ElementMatcher.Junction<TypeDescription> isSubTypeOf(Class<?> type) {
        return isSubTypeOf(new TypeDescription.ForLoadedType(nonNull(type)));
    }

    public static ElementMatcher.Junction<TypeDescription> isSubTypeOf(TypeDescription typeDescription) {
        return new SubTypeMatcher<TypeDescription>(nonNull(typeDescription));
    }

    public static ElementMatcher.Junction<TypeDescription> isSuperTypeOf(Class<?> type) {
        return isSuperTypeOf(new TypeDescription.ForLoadedType(nonNull(type)));
    }

    public static ElementMatcher.Junction<TypeDescription> isSuperTypeOf(TypeDescription typeDescription) {
        return new SuperTypeMatcher<TypeDescription>(nonNull(typeDescription));
    }

    private ElementMatchers() {
        throw new UnsupportedOperationException();
    }
}
