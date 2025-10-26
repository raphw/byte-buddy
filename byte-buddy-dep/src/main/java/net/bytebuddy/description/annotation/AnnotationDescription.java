/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.description.annotation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.build.CachedReturnPlugin;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.utility.nullability.AlwaysNull;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.privilege.SetAccessibleAction;

import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationTypeMismatchException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * An annotation description describes {@link java.lang.annotation.Annotation} meta data of a class without this class
 * being required to be loaded. All values of an annotation are therefore represented in unloaded state:
 * <ul>
 * <li>{@link java.lang.Class} instances are represented as {@link TypeDescription}s.</li>
 * <li>{@link java.lang.Enum} instances are represented as
 * {@link net.bytebuddy.description.enumeration.EnumerationDescription}s.</li>
 * <li>{@link java.lang.annotation.Annotation}s are described as
 * {@link AnnotationDescription}s.</li>
 * <li>All primitive types are represented as their wrapper types.</li>
 * </ul>
 * An annotation can however be loaded in order to access unwrapped values. This will cause a loading of the classes
 * of these values.
 */
public interface AnnotationDescription {

    /**
     * Indicates a nonexistent annotation in a type-safe manner.
     */
    @AlwaysNull
    AnnotationDescription.Loadable<?> UNDEFINED = null;

    /**
     * Returns a value of this annotation.
     *
     * @param property The name of the property being accessed.
     * @return The value for the supplied property.
     */
    AnnotationValue<?, ?> getValue(String property);

    /**
     * Returns a value of this annotation.
     *
     * @param property The property being accessed.
     * @return The value for the supplied property.
     */
    AnnotationValue<?, ?> getValue(MethodDescription.InDefinedShape property);

    /**
     * Returns a description of the annotation type of this annotation.
     *
     * @return A description of the annotation type of this annotation.
     */
    TypeDescription getAnnotationType();

    /**
     * Links this annotation description to a given annotation type such that it can be loaded. This does not cause
     * the values of this annotation to be loaded.
     *
     * @param annotationType The loaded annotation type of this annotation description.
     * @param <T>            The type of the annotation.
     * @return A loadable version of this annotation description.
     */
    <T extends Annotation> Loadable<T> prepare(Class<T> annotationType);

    /**
     * Returns this annotation's retention policy.
     *
     * @return This annotation's retention policy.
     */
    RetentionPolicy getRetention();

    /**
     * Returns a set of all {@link ElementType}s that can declare this annotation.
     *
     * @return A set of all element types that can declare this annotation.
     */
    Set<ElementType> getElementTypes();

    /**
     * Checks if this annotation is supported on the supplied element type.
     *
     * @param elementType The element type to check.
     * @return {@code true} if the supplied element type is supported by this annotation.
     */
    boolean isSupportedOn(ElementType elementType);

    /**
     * Checks if this annotation is supported on the supplied element type.
     *
     * @param elementType The element type to check.
     * @return {@code true} if the supplied element type is supported by this annotation.
     */
    boolean isSupportedOn(String elementType);

    /**
     * Checks if this annotation is inherited.
     *
     * @return {@code true} if this annotation is inherited.
     * @see Inherited
     */
    boolean isInherited();

    /**
     * Checks if this annotation is documented.
     *
     * @return {@code true} if this annotation is documented.
     * @see Documented
     */
    boolean isDocumented();

    /**
     * An annotation description that is linked to a given loaded annotation type which allows its representation
     * as a fully loaded instance.
     *
     * @param <S> The annotation type.
     */
    interface Loadable<S extends Annotation> extends AnnotationDescription {

        /**
         * Loads this annotation description. This causes all classes referenced by the annotation value to be loaded.
         * Without specifying a class loader, the annotation's class loader which was used to prepare this instance
         * is used.
         *
         * @return A loaded version of this annotation description.
         */
        S load();
    }

    /**
     * A rendering dispatcher is responsible for resolving annotation descriptions to {@link String} representations.
     */
    enum RenderingDispatcher {

        /**
         * A rendering dispatcher for any VM previous to Java 14.
         */
        LEGACY_VM,

        /**
         * A rendering dispatcher for Java 14 until Java 18.
         */
        JAVA_14_CAPABLE_VM {
            @Override
            public void appendPrefix(StringBuilder toString, String key, int count) {
                if (count > 1 || !key.equals("value")) {
                    super.appendPrefix(toString, key, count);
                }
            }
        },

        /**
         * A rendering dispatcher for Java 19 onward.
         */
        JAVA_19_CAPABLE_VM {
            @Override
            public void appendPrefix(StringBuilder toString, String key, int count) {
                if (count > 1 || !key.equals("value")) {
                    super.appendPrefix(toString, key, count);
                }
            }

            @Override
            public void appendType(StringBuilder toString, TypeDescription typeDescription) {
                toString.append(typeDescription.getCanonicalName());
            }
        };

        /**
         * The rendering dispatcher for the current VM.
         */
        public static final RenderingDispatcher CURRENT;

        /*
         * Initializes the rendering dispatcher.
         */
        static {
            ClassFileVersion classFileVersion = ClassFileVersion.ofThisVm(ClassFileVersion.JAVA_V5);
            if (classFileVersion.isAtLeast(ClassFileVersion.JAVA_V19)) {
                CURRENT = RenderingDispatcher.JAVA_19_CAPABLE_VM;
            } else if (classFileVersion.isAtLeast(ClassFileVersion.JAVA_V14)) {
                CURRENT = RenderingDispatcher.JAVA_14_CAPABLE_VM;
            } else {
                CURRENT = RenderingDispatcher.LEGACY_VM;
            }
        }

        /**
         * Appends the key property prefix to a string builder representing an annotation's string representation.
         *
         * @param toString The string builder that creates the string representation.
         * @param key      The key's name.
         * @param count    The property count.
         */
        public void appendPrefix(StringBuilder toString, String key, int count) {
            toString.append(key).append('=');
        }

        /**
         * Appends the type name of the annotation being rendered.
         *
         * @param toString        The string builder that creates the string representation.
         * @param typeDescription The annotation type being rendered.
         */
        public void appendType(StringBuilder toString, TypeDescription typeDescription) {
            toString.append(typeDescription.getName());
        }
    }

    /**
     * An {@link java.lang.reflect.InvocationHandler} for implementing annotations.
     *
     * @param <T> The type of the handled annotation.
     */
    class AnnotationInvocationHandler<T extends Annotation> implements InvocationHandler {

        /**
         * The name of the {@link Object#hashCode()} method.
         */
        private static final String HASH_CODE = "hashCode";

        /**
         * The name of the {@link Object#equals(Object)} method.
         */
        private static final String EQUALS = "equals";

        /**
         * The name of the {@link Object#toString()} method.
         */
        private static final String TO_STRING = "toString";

        /**
         * The name of the {@link Annotation#annotationType()} method.
         */
        private static final String ANNOTATION_TYPE = "annotationType";

        /**
         * An empty array that can be used to indicate no arguments to avoid an allocation on a reflective call.
         */
        private static final Object[] NO_ARGUMENT = new Object[0];

        /**
         * The loaded annotation type.
         */
        private final Class<? extends Annotation> annotationType;

        /**
         * A sorted list of values of this annotation.
         */
        private final LinkedHashMap<Method, AnnotationValue.Loaded<?>> values;

        /**
         * Creates a new invocation handler.
         *
         * @param annotationType The loaded annotation type.
         * @param values         A sorted list of values of this annotation.
         */
        protected AnnotationInvocationHandler(Class<T> annotationType, LinkedHashMap<Method, AnnotationValue.Loaded<?>> values) {
            this.annotationType = annotationType;
            this.values = values;
        }

        /**
         * Creates a proxy instance for the supplied annotation type and values.
         *
         * @param classLoader    The class loader that should be used for loading the annotation's values.
         * @param annotationType The annotation's type.
         * @param values         The values that the annotation contains.
         * @param <S>            The type of the handled annotation.
         * @return A proxy for the annotation type and values.
         */
        @SuppressWarnings("unchecked")
        public static <S extends Annotation> S of(@MaybeNull ClassLoader classLoader,
                                                  Class<S> annotationType,
                                                  Map<String, ? extends AnnotationValue<?, ?>> values) {
            LinkedHashMap<Method, AnnotationValue.Loaded<?>> loadedValues = new LinkedHashMap<Method, AnnotationValue.Loaded<?>>();
            for (Method method : annotationType.getDeclaredMethods()) {
                AnnotationValue<?, ?> annotationValue = values.get(method.getName());
                if (annotationValue == null) {
                    Object defaultValue = method.getDefaultValue();
                    loadedValues.put(method, (defaultValue == null
                            ? new AnnotationValue.ForMissingValue<Void, Void>(new TypeDescription.ForLoadedType(method.getDeclaringClass()), method.getName())
                            : AnnotationDescription.ForLoadedAnnotation.asValue(defaultValue, method.getReturnType())).load(classLoader));
                } else {
                    loadedValues.put(method, annotationValue.filter(new MethodDescription.ForLoadedMethod(method)).load(classLoader));
                }
            }
            return (S) Proxy.newProxyInstance(classLoader, new Class<?>[]{annotationType}, new AnnotationInvocationHandler<S>(annotationType, loadedValues));
        }

        /**
         * {@inheritDoc}
         */
        public Object invoke(Object proxy, Method method, @MaybeNull Object[] argument) {
            if (method.getDeclaringClass() != annotationType) {
                if (method.getName().equals(HASH_CODE)) {
                    return hashCodeRepresentation();
                } else if (method.getName().equals(EQUALS) && method.getParameterTypes().length == 1) {
                    return equalsRepresentation(proxy, argument[0]);
                } else if (method.getName().equals(TO_STRING)) {
                    return toStringRepresentation();
                } else if (method.getName().equals(ANNOTATION_TYPE)) {
                    return annotationType;
                } else {
                    throw new IllegalStateException("Unexpected method: " + method);
                }
            }
            return values.get(method).resolve();
        }

        /**
         * Returns the string representation of the represented annotation.
         *
         * @return The string representation of the represented annotation.
         */
        protected String toStringRepresentation() {
            StringBuilder toString = new StringBuilder();
            toString.append('@');
            RenderingDispatcher.CURRENT.appendType(toString, TypeDescription.ForLoadedType.of(annotationType));
            toString.append('(');
            boolean firstMember = true;
            for (Map.Entry<Method, AnnotationValue.Loaded<?>> entry : values.entrySet()) {
                if (!entry.getValue().getState().isDefined()) {
                    continue;
                }
                if (firstMember) {
                    firstMember = false;
                } else {
                    toString.append(", ");
                }
                RenderingDispatcher.CURRENT.appendPrefix(toString, entry.getKey().getName(), values.entrySet().size());
                toString.append(entry.getValue().toString());
            }
            toString.append(')');
            return toString.toString();
        }

        /**
         * Returns the hash code of the represented annotation.
         *
         * @return The hash code of the represented annotation.
         */
        private int hashCodeRepresentation() {
            int hashCode = 0;
            for (Map.Entry<Method, AnnotationValue.Loaded<?>> entry : values.entrySet()) {
                if (!entry.getValue().getState().isDefined()) {
                    continue;
                }
                hashCode += (127 * entry.getKey().getName().hashCode()) ^ entry.getValue().hashCode();
            }
            return hashCode;
        }

        /**
         * Checks if another instance is equal to this instance.
         *
         * @param self  The annotation proxy instance.
         * @param other The instance to be examined for equality to the represented instance.
         * @return {@code true} if the given instance is equal to the represented instance.
         */
        private boolean equalsRepresentation(Object self, Object other) {
            if (self == other) {
                return true;
            } else if (!annotationType.isInstance(other)) {
                return false;
            } else if (Proxy.isProxyClass(other.getClass())) {
                InvocationHandler invocationHandler = Proxy.getInvocationHandler(other);
                if (invocationHandler instanceof AnnotationInvocationHandler) {
                    return invocationHandler.equals(this);
                }
            }
            try {
                for (Map.Entry<Method, AnnotationValue.Loaded<?>> entry : values.entrySet()) {
                    try {
                        if (!entry.getValue().represents(entry.getKey().invoke(other, NO_ARGUMENT))) {
                            return false;
                        }
                    } catch (RuntimeException exception) {
                        return false; // Incomplete annotations are not equal to one another.
                    }
                }
                return true;
            } catch (InvocationTargetException ignored) {
                return false;
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Could not access annotation property", exception);
            }
        }

        @Override
        @CachedReturnPlugin.Enhance("hashCode")
        public int hashCode() {
            int result = annotationType.hashCode();
            result = 31 * result + values.hashCode();
            for (Map.Entry<Method, ?> entry : values.entrySet()) {
                result = 31 * result + entry.getValue().hashCode();
            }
            return result;
        }

        @Override
        public boolean equals(@MaybeNull Object other) {
            if (this == other) {
                return true;
            } else if (!(other instanceof AnnotationInvocationHandler)) {
                return false;
            }
            AnnotationInvocationHandler<?> that = (AnnotationInvocationHandler<?>) other;
            if (!annotationType.equals(that.annotationType)) {
                return false;
            }
            for (Map.Entry<Method, AnnotationValue.Loaded<?>> entry : values.entrySet()) {
                if (!entry.getValue().equals(that.values.get(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * An adapter implementation of an annotation.
     */
    abstract class AbstractBase implements AnnotationDescription {

        /**
         * An array containing all element types that are a legal annotation target when such a target
         * is not specified explicitly.
         */
        private static final Set<ElementType> DEFAULT_TARGET;

        /**
         * A description of the {@link Retention#value()} method.
         */
        private static final MethodDescription.InDefinedShape RETENTION_VALUE;

        /**
         * A description of the {@link Target#value()} method.
         */
        private static final MethodDescription.InDefinedShape TARGET_VALUE;

        /*
         * Resolves common annotation properties.
         */
        static {
            DEFAULT_TARGET = new HashSet<ElementType>();
            for (ElementType elementType : ElementType.values()) {
                if (!elementType.name().equals("TYPE_PARAMETER")) {
                    DEFAULT_TARGET.add(elementType);
                }
            }
            RETENTION_VALUE = TypeDescription.ForLoadedType.of(Retention.class)
                    .getDeclaredMethods()
                    .filter(named("value"))
                    .getOnly();
            TARGET_VALUE = TypeDescription.ForLoadedType.of(Target.class)
                    .getDeclaredMethods()
                    .filter(named("value"))
                    .getOnly();
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationValue<?, ?> getValue(String property) {
            MethodList<MethodDescription.InDefinedShape> candidates = getAnnotationType().getDeclaredMethods().filter(named(property)
                    .and(takesArguments(0))
                    .and(isPublic())
                    .and(not(isStatic())));
            if (candidates.size() == 1) {
                return getValue(candidates.getOnly());
            } else {
                throw new IllegalArgumentException("Unknown property of " + getAnnotationType() + ": " + property);
            }
        }

        /**
         * {@inheritDoc}
         */
        public RetentionPolicy getRetention() {
            AnnotationDescription.Loadable<Retention> retention = getAnnotationType().getDeclaredAnnotations().ofType(Retention.class);
            return retention == null
                    ? RetentionPolicy.CLASS
                    : retention.getValue(RETENTION_VALUE).load(ClassLoadingStrategy.BOOTSTRAP_LOADER).resolve(RetentionPolicy.class);
        }

        /**
         * {@inheritDoc}
         */
        public Set<ElementType> getElementTypes() {
            AnnotationDescription.Loadable<Target> target = getAnnotationType().getDeclaredAnnotations().ofType(Target.class);
            return target == null
                    ? Collections.unmodifiableSet(DEFAULT_TARGET)
                    : new HashSet<ElementType>(Arrays.asList(target.getValue(TARGET_VALUE).load(ClassLoadingStrategy.BOOTSTRAP_LOADER).resolve(ElementType[].class)));
        }

        /**
         * {@inheritDoc}
         */
        public boolean isSupportedOn(ElementType elementType) {
            return isSupportedOn(elementType.name());
        }

        /**
         * {@inheritDoc}
         */
        public boolean isSupportedOn(String elementType) {
            AnnotationDescription.Loadable<Target> target = getAnnotationType().getDeclaredAnnotations().ofType(Target.class);
            if (target == null) {
                if (elementType.equals("TYPE_USE")) {
                    return true;
                }
                for (ElementType candidate : DEFAULT_TARGET) {
                    if (candidate.name().equals(elementType)) {
                        return true;
                    }
                }
            } else {
                for (EnumerationDescription enumerationDescription : target.getValue(TARGET_VALUE).resolve(EnumerationDescription[].class)) {
                    if (enumerationDescription.getValue().equals(elementType)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isInherited() {
            return getAnnotationType().getDeclaredAnnotations().isAnnotationPresent(Inherited.class);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isDocumented() {
            return getAnnotationType().getDeclaredAnnotations().isAnnotationPresent(Documented.class);
        }

        @Override
        @CachedReturnPlugin.Enhance("hashCode")
        public int hashCode() {
            int hashCode = 0;
            for (MethodDescription.InDefinedShape methodDescription : getAnnotationType().getDeclaredMethods()) {
                hashCode += 31 * getValue(methodDescription).hashCode();
            }
            return hashCode;
        }

        @Override
        public boolean equals(@MaybeNull Object other) {
            if (this == other) {
                return true;
            } else if (!(other instanceof AnnotationDescription)) {
                return false;
            }
            AnnotationDescription annotationDescription = ((AnnotationDescription) other);
            TypeDescription annotationType = getAnnotationType();
            if (!annotationDescription.getAnnotationType().equals(annotationType)) {
                return false;
            }
            for (MethodDescription.InDefinedShape methodDescription : annotationType.getDeclaredMethods()) {
                if (!getValue(methodDescription).equals(annotationDescription.getValue(methodDescription))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            TypeDescription annotationType = getAnnotationType();
            StringBuilder toString = new StringBuilder().append('@');
            RenderingDispatcher.CURRENT.appendType(toString, annotationType);
            toString.append('(');
            boolean firstMember = true;
            for (MethodDescription.InDefinedShape methodDescription : annotationType.getDeclaredMethods()) {
                AnnotationValue<?, ?> value = getValue(methodDescription);
                if (value.getState() == AnnotationValue.State.UNDEFINED) {
                    continue;
                } else if (firstMember) {
                    firstMember = false;
                } else {
                    toString.append(", ");
                }
                RenderingDispatcher.CURRENT.appendPrefix(toString, methodDescription.getName(), annotationType.getDeclaredMethods().size());
                toString.append(value);
            }
            return toString.append(')').toString();
        }
    }

    /**
     * A description of an already loaded annotation.
     *
     * @param <S> The type of the annotation.
     */
    class ForLoadedAnnotation<S extends Annotation> extends AbstractBase implements Loadable<S> {

        /**
         * An empty array that can be used to indicate no arguments to avoid an allocation on a reflective call.
         */
        private static final Object[] NO_ARGUMENT = new Object[0];

        /**
         * The represented annotation value.
         */
        private final S annotation;

        /**
         * The annotation's loaded type which might be loaded by a different class loader than the value's
         * annotation type but must be structurally equal to it.
         */
        private final Class<S> annotationType;

        /**
         * Creates a new annotation description for a loaded annotation.
         *
         * @param annotation The annotation to represent.
         */
        @SuppressWarnings("unchecked")
        protected ForLoadedAnnotation(S annotation) {
            this(annotation, (Class<S>) annotation.annotationType());
        }

        /**
         * Creates a new annotation description for a loaded annotation.
         *
         * @param annotation     The annotation to represent.
         * @param annotationType The annotation's loaded type which might be loaded by a different class loader than the value's
         *                       annotation type but must be structurally equal to it.
         */
        private ForLoadedAnnotation(S annotation, Class<S> annotationType) {
            this.annotation = annotation;
            this.annotationType = annotationType;
        }

        /**
         * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
         *
         * @param action The action to execute from a privileged context.
         * @param <T>    The type of the action's resolved value.
         * @return The action's resolved value.
         */
        @AccessControllerPlugin.Enhance
        private static <T> T doPrivileged(PrivilegedAction<T> action) {
            return action.run();
        }

        /**
         * Creates a description of the given annotation.
         *
         * @param annotation The annotation to be described.
         * @param <U>        The type of the annotation.
         * @return A description of the given annotation.
         */
        public static <U extends Annotation> Loadable<U> of(U annotation) {
            return new ForLoadedAnnotation<U>(annotation);
        }

        /**
         * {@inheritDoc}
         */
        public S load() {
            return annotationType == annotation.annotationType()
                    ? annotation
                    : AnnotationInvocationHandler.of(annotationType.getClassLoader(), annotationType, asValue(annotation));
        }

        /**
         * Extracts the annotation values of an annotation into a property map.
         *
         * @param annotation The annotation to convert.
         * @return A mapping of property names to their annotation value.
         */
        @SuppressWarnings("rawtypes")
        private static Map<String, AnnotationValue<?, ?>> asValue(Annotation annotation) {
            Map<String, AnnotationValue<?, ?>> annotationValues = new HashMap<String, AnnotationValue<?, ?>>();
            for (Method property : annotation.annotationType().getDeclaredMethods()) {
                try {
                    annotationValues.put(property.getName(), asValue(property.invoke(annotation, NO_ARGUMENT), property.getReturnType()));
                } catch (InvocationTargetException exception) {
                    Throwable cause = exception.getTargetException();
                    if (cause instanceof TypeNotPresentException) {
                        annotationValues.put(property.getName(), new AnnotationValue.ForMissingType<Void, Void>(((TypeNotPresentException) cause).typeName()));
                    } else if (cause instanceof EnumConstantNotPresentException) {
                        annotationValues.put(property.getName(), new AnnotationValue.ForEnumerationDescription.WithUnknownConstant(
                                new TypeDescription.ForLoadedType(((EnumConstantNotPresentException) cause).enumType()),
                                ((EnumConstantNotPresentException) cause).constantName()));
                    } else if (cause instanceof AnnotationTypeMismatchException) {
                        annotationValues.put(property.getName(), new AnnotationValue.ForMismatchedType<Void, Void>(
                                new MethodDescription.ForLoadedMethod(((AnnotationTypeMismatchException) cause).element()),
                                ((AnnotationTypeMismatchException) cause).foundType()));
                    } else if (!(cause instanceof IncompleteAnnotationException)) {
                        throw new IllegalStateException("Cannot read " + property, cause);
                    }
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + property, exception);
                }
            }
            return annotationValues;
        }

        /**
         * Transforms an annotation property to an annotation value.
         *
         * @param type  The annotation's type.
         * @param value The annotations value.
         * @return An annotation value representation.
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static AnnotationValue<?, ?> asValue(Object value, Class<?> type) {
            // Because enums can implement annotation interfaces, the enum property needs to be checked first.
            if (Enum.class.isAssignableFrom(type)) {
                return AnnotationValue.ForEnumerationDescription.<Enum>of(new EnumerationDescription.ForLoadedEnumeration((Enum) value));
            } else if (Enum[].class.isAssignableFrom(type)) {
                Enum<?>[] element = (Enum<?>[]) value;
                EnumerationDescription[] enumerationDescription = new EnumerationDescription[element.length];
                int index = 0;
                for (Enum<?> anElement : element) {
                    enumerationDescription[index++] = new EnumerationDescription.ForLoadedEnumeration(anElement);
                }
                return AnnotationValue.ForDescriptionArray.<Enum>of(TypeDescription.ForLoadedType.of(type.getComponentType()), enumerationDescription);
            } else if (Annotation.class.isAssignableFrom(type)) {
                return AnnotationValue.ForAnnotationDescription.<Annotation>of(TypeDescription.ForLoadedType.of(type), asValue((Annotation) value));
            } else if (Annotation[].class.isAssignableFrom(type)) {
                Annotation[] element = (Annotation[]) value;
                AnnotationDescription[] annotationDescription = new AnnotationDescription[element.length];
                int index = 0;
                for (Annotation anElement : element) {
                    annotationDescription[index++] = new AnnotationDescription.Latent(TypeDescription.ForLoadedType.of(type.getComponentType()), asValue(anElement));
                }
                return AnnotationValue.ForDescriptionArray.of(TypeDescription.ForLoadedType.of(type.getComponentType()), annotationDescription);
            } else if (Class.class.isAssignableFrom(type)) {
                return AnnotationValue.ForTypeDescription.<Class>of(TypeDescription.ForLoadedType.of((Class<?>) value));
            } else if (Class[].class.isAssignableFrom(type)) {
                Class<?>[] element = (Class<?>[]) value;
                TypeDescription[] typeDescription = new TypeDescription[element.length];
                int index = 0;
                for (Class<?> anElement : element) {
                    typeDescription[index++] = TypeDescription.ForLoadedType.of(anElement);
                }
                return AnnotationValue.ForDescriptionArray.of(typeDescription);
            } else {
                return AnnotationValue.ForConstant.of(value);
            }
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings({"deprecation", "rawtypes"})
        @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should always be wrapped for clarity.")
        public AnnotationValue<?, ?> getValue(MethodDescription.InDefinedShape property) {
            if (!property.getDeclaringType().represents(annotation.annotationType())) {
                throw new IllegalArgumentException(property + " does not represent " + annotation.annotationType());
            }
            try {
                boolean accessible = property.getDeclaringType().isPublic(); // method is required to be public
                Method method = property instanceof MethodDescription.ForLoadedMethod
                        ? ((MethodDescription.ForLoadedMethod) property).getLoadedMethod()
                        : null;
                if (method == null || method.getDeclaringClass() != annotation.annotationType() || (!accessible && !method.isAccessible())) {
                    method = annotation.annotationType().getMethod(property.getName());
                    if (!accessible) {
                        doPrivileged(new SetAccessibleAction<Method>(method));
                    }
                }
                return asValue(method.invoke(annotation, NO_ARGUMENT), method.getReturnType()).filter(property);
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getTargetException();
                if (cause instanceof TypeNotPresentException) {
                    return new AnnotationValue.ForMissingType<Void, Void>(((TypeNotPresentException) cause).typeName());
                } else if (cause instanceof EnumConstantNotPresentException) {
                    return new AnnotationValue.ForEnumerationDescription.WithUnknownConstant(
                            new TypeDescription.ForLoadedType(((EnumConstantNotPresentException) cause).enumType()),
                            ((EnumConstantNotPresentException) cause).constantName());
                } else if (cause instanceof AnnotationTypeMismatchException) {
                    return new AnnotationValue.ForMismatchedType<Void, Void>(
                            new MethodDescription.ForLoadedMethod(((AnnotationTypeMismatchException) cause).element()),
                            ((AnnotationTypeMismatchException) cause).foundType());
                } else if (cause instanceof IncompleteAnnotationException) {
                    return new AnnotationValue.ForMissingValue<Void, Void>(
                            new TypeDescription.ForLoadedType(((IncompleteAnnotationException) cause).annotationType()),
                            ((IncompleteAnnotationException) cause).elementName());
                } else {
                    throw new IllegalStateException("Error reading annotation property " + property, cause);
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Cannot access annotation property " + property, exception);
            }
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public <T extends Annotation> Loadable<T> prepare(Class<T> annotationType) {
            if (!annotation.annotationType().getName().equals(annotationType.getName())) {
                throw new IllegalArgumentException(annotationType + " does not represent " + annotation.annotationType());
            }
            return annotationType == annotation.annotationType()
                    ? (Loadable<T>) this
                    : new ForLoadedAnnotation<T>((T) annotation, annotationType);
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getAnnotationType() {
            return TypeDescription.ForLoadedType.of(annotation.annotationType());
        }
    }

    /**
     * A latent description of an annotation value that is defined explicitly.
     */
    class Latent extends AbstractBase {

        /**
         * The type of the annotation.
         */
        private final TypeDescription annotationType;

        /**
         * The values of the annotation mapped by their property name.
         */
        private final Map<String, ? extends AnnotationValue<?, ?>> annotationValues;

        /**
         * Creates a new latent annotation description.
         *
         * @param annotationType   The type of the annotation.
         * @param annotationValues The values of the annotation mapped by their property name.
         */
        protected Latent(TypeDescription annotationType, Map<String, ? extends AnnotationValue<?, ?>> annotationValues) {
            this.annotationType = annotationType;
            this.annotationValues = annotationValues;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationValue<?, ?> getValue(MethodDescription.InDefinedShape property) {
            if (!property.getDeclaringType().equals(annotationType)) {
                throw new IllegalArgumentException("Not a property of " + annotationType + ": " + property);
            }
            AnnotationValue<?, ?> value = annotationValues.get(property.getName());
            if (value != null) {
                return value.filter(property);
            }
            AnnotationValue<?, ?> defaultValue = property.getDefaultValue();
            return defaultValue == null
                    ? new AnnotationValue.ForMissingValue<Void, Void>(annotationType, property.getName())
                    : defaultValue;
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getAnnotationType() {
            return annotationType;
        }

        /**
         * {@inheritDoc}
         */
        public <T extends Annotation> Loadable<T> prepare(Class<T> annotationType) {
            if (!this.annotationType.represents(annotationType)) {
                throw new IllegalArgumentException(annotationType + " does not represent " + this.annotationType);
            }
            return new Loadable<T>(annotationType);
        }

        /**
         * A loadable annotation description of a latent annotation description.
         *
         * @param <S> The annotation type.
         */
        protected class Loadable<S extends Annotation> extends AbstractBase implements AnnotationDescription.Loadable<S> {

            /**
             * The annotation type.
             */
            private final Class<S> annotationType;

            /**
             * Creates a loadable version of a latent annotation description.
             *
             * @param annotationType The annotation type.
             */
            protected Loadable(Class<S> annotationType) {
                this.annotationType = annotationType;
            }

            /**
             * {@inheritDoc}
             */
            public S load() {
                return AnnotationDescription.AnnotationInvocationHandler.of(annotationType.getClassLoader(), annotationType, annotationValues);
            }

            /**
             * {@inheritDoc}
             */
            public AnnotationValue<?, ?> getValue(MethodDescription.InDefinedShape property) {
                return Latent.this.getValue(property);
            }

            /**
             * {@inheritDoc}
             */
            public TypeDescription getAnnotationType() {
                return TypeDescription.ForLoadedType.of(annotationType);
            }

            /**
             * {@inheritDoc}
             */
            public <T extends Annotation> Loadable<T> prepare(Class<T> annotationType) {
                return Latent.this.prepare(annotationType);
            }
        }
    }

    /**
     * A builder for pragmatically creating {@link net.bytebuddy.description.annotation.AnnotationDescription}.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Builder {

        /**
         * The annotation type.
         */
        private final TypeDescription annotationType;

        /**
         * A mapping of annotation properties to their annotation values.
         */
        private final Map<String, AnnotationValue<?, ?>> annotationValues;

        /**
         * Creates a builder for an annotation description.
         *
         * @param annotationType   The annotation type.
         * @param annotationValues A mapping of annotation properties to their annotation values.
         */
        protected Builder(TypeDescription annotationType, Map<String, AnnotationValue<?, ?>> annotationValues) {
            this.annotationType = annotationType;
            this.annotationValues = annotationValues;
        }

        /**
         * Creates a builder for creating an annotation of the given type.
         *
         * @param annotationType The annotation type.
         * @return A builder for creating an annotation of the given type.
         */
        public static Builder ofType(Class<? extends Annotation> annotationType) {
            return ofType(TypeDescription.ForLoadedType.of(annotationType));
        }

        /**
         * Creates a builder for creating an annotation of the given type.
         *
         * @param annotationType A description of the annotation type.
         * @return A builder for creating an annotation of the given type.
         */
        public static Builder ofType(TypeDescription annotationType) {
            if (!annotationType.isAnnotation()) {
                throw new IllegalArgumentException("Not an annotation type: " + annotationType);
            }
            return new Builder(annotationType, Collections.<String, AnnotationValue<?, ?>>emptyMap());
        }

        /**
         * Returns a builder with the additional, given property.
         *
         * @param property The name of the property to define.
         * @param value    An explicit description of the annotation value.
         * @return A builder with the additional, given property.
         */
        public Builder define(String property, AnnotationValue<?, ?> value) {
            MethodList<MethodDescription.InDefinedShape> methodDescriptions = annotationType.getDeclaredMethods().filter(named(property));
            if (methodDescriptions.isEmpty()) {
                throw new IllegalArgumentException(annotationType + " does not define a property named " + property);
            }
            Map<String, AnnotationValue<?, ?>> annotationValues = new HashMap<String, AnnotationValue<?, ?>>(this.annotationValues);
            if (annotationValues.put(methodDescriptions.getOnly().getName(), value) != null) {
                throw new IllegalArgumentException("Property already defined: " + property);
            }
            return new Builder(annotationType, annotationValues);
        }

        /**
         * Returns a builder with the additional enumeration property.
         *
         * @param property The name of the property to define.
         * @param value    The enumeration value to define.
         * @return A builder with the additional enumeration property.
         */
        public Builder define(String property, Enum<?> value) {
            return define(property, new EnumerationDescription.ForLoadedEnumeration(value));
        }

        /**
         * Returns a builder with the additional enumeration property.
         *
         * @param property        The name of the property to define.
         * @param enumerationType The type of the enumeration.
         * @param value           The enumeration value to define.
         * @return A builder with the additional enumeration property.
         */
        public Builder define(String property, TypeDescription enumerationType, String value) {
            return define(property, new EnumerationDescription.Latent(enumerationType, value));
        }

        /**
         * Returns a builder with the additional enumeration property.
         *
         * @param property The name of the property to define.
         * @param value    A description of the enumeration value to define.
         * @return A builder with the additional enumeration property.
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Builder define(String property, EnumerationDescription value) {
            return define(property, AnnotationValue.ForEnumerationDescription.<Enum>of(value));
        }

        /**
         * Returns a builder with the additional annotation property.
         *
         * @param property   The name of the property to define.
         * @param annotation The annotation value to define.
         * @return A builder with the additional annotation property.
         */
        public Builder define(String property, Annotation annotation) {
            return define(property, new ForLoadedAnnotation<Annotation>(annotation));
        }

        /**
         * Returns a builder with the additional annotation property.
         *
         * @param property              The name of the property to define.
         * @param annotationDescription A description of the annotation value to define.
         * @return A builder with the additional annotation property.
         */
        public Builder define(String property, AnnotationDescription annotationDescription) {
            return define(property, new AnnotationValue.ForAnnotationDescription<Annotation>(annotationDescription));
        }

        /**
         * Returns a builder with the additional class property.
         *
         * @param property The name of the property to define.
         * @param type     The class value to define.
         * @return A builder with the additional class property.
         */
        public Builder define(String property, Class<?> type) {
            return define(property, TypeDescription.ForLoadedType.of(type));
        }

        /**
         * Returns a builder with the additional class property.
         *
         * @param property        The name of the property to define.
         * @param typeDescription A description of the type to define as a property value.
         * @return A builder with the additional class property.
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Builder define(String property, TypeDescription typeDescription) {
            return define(property, AnnotationValue.ForTypeDescription.<Class>of(typeDescription));
        }

        /**
         * Returns a builder with the additional enumeration array property.
         *
         * @param property        The name of the property to define.
         * @param enumerationType The type of the enumeration, i.e. the component type of the enumeration array.
         * @param value           The enumeration values to be contained by the array.
         * @param <T>             The enumeration type.
         * @return A builder with the additional class property.
         */
        @SuppressWarnings("unchecked")
        public <T extends Enum<?>> Builder defineEnumerationArray(String property, Class<T> enumerationType, T... value) {
            EnumerationDescription[] enumerationDescription = new EnumerationDescription[value.length];
            int index = 0;
            for (T aValue : value) {
                enumerationDescription[index++] = new EnumerationDescription.ForLoadedEnumeration(aValue);
            }
            return defineEnumerationArray(property, TypeDescription.ForLoadedType.of(enumerationType), enumerationDescription);
        }

        /**
         * Returns a builder with the additional enumeration array property.
         *
         * @param property        The name of the property to define.
         * @param enumerationType The type of the enumerations, i.e. is the component type of the enumeration array.
         * @param value           The enumeration values to be contained by the array.
         * @return A builder with the additional enumeration property.
         */
        public Builder defineEnumerationArray(String property, TypeDescription enumerationType, String... value) {
            if (!enumerationType.isEnum()) {
                throw new IllegalArgumentException("Not an enumeration type: " + enumerationType);
            }
            EnumerationDescription[] enumerationDescription = new EnumerationDescription[value.length];
            for (int i = 0; i < value.length; i++) {
                enumerationDescription[i] = new EnumerationDescription.Latent(enumerationType, value[i]);
            }
            return defineEnumerationArray(property, enumerationType, enumerationDescription);
        }

        /**
         * Returns a builder with the additional enumeration array property.
         *
         * @param property        The name of the property to define.
         * @param enumerationType The type of the enumerations, i.e. the component type of the enumeration array.
         * @param value           Descriptions of the enumerations to be contained by the array.
         * @return A builder with the additional enumeration property.
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Builder defineEnumerationArray(String property, TypeDescription enumerationType, EnumerationDescription... value) {
            return define(property, AnnotationValue.ForDescriptionArray.<Enum>of(enumerationType, value));
        }

        /**
         * Returns a builder with the additional annotation array property.
         *
         * @param property       The name of the property to define.
         * @param annotationType The type of the annotations, i.e. the component type of the enumeration array.
         * @param annotation     The annotation values to be contained by the array.
         * @param <T>            The annotation type.
         * @return A builder with the additional annotation property.
         */
        @SuppressWarnings("unchecked")
        public <T extends Annotation> Builder defineAnnotationArray(String property, Class<T> annotationType, T... annotation) {
            return defineAnnotationArray(property,
                    TypeDescription.ForLoadedType.of(annotationType),
                    new AnnotationList.ForLoadedAnnotations(annotation).toArray(new AnnotationDescription[0]));
        }

        /**
         * Returns a builder with the additional annotation array property.
         *
         * @param property              The name of the property to define.
         * @param annotationType        The type of the annotations, i.e. the component type of the enumeration array.
         * @param annotationDescription Descriptions of the annotation values to be contained by the array.
         * @return A builder with the additional annotation property.
         */
        public Builder defineAnnotationArray(String property, TypeDescription annotationType, AnnotationDescription... annotationDescription) {
            return define(property, AnnotationValue.ForDescriptionArray.of(annotationType, annotationDescription));
        }

        /**
         * Returns a builder with the additional type array property.
         *
         * @param property The name of the property to define.
         * @param type     The types that should be contained by the array.
         * @return A builder with the additional type array property.
         */
        public Builder defineTypeArray(String property, Class<?>... type) {
            return defineTypeArray(property, new TypeList.ForLoadedTypes(type).toArray(new TypeDescription[0]));
        }

        /**
         * Returns a builder with the additional type array property.
         *
         * @param property        The name of the property to define.
         * @param typeDescription Descriptions of the types that should be contained by the array.
         * @return A builder with the additional type array property.
         */
        public Builder defineTypeArray(String property, TypeDescription... typeDescription) {
            return define(property, AnnotationValue.ForDescriptionArray.of(typeDescription));
        }

        /**
         * Returns a builder with the additional {@code boolean} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code boolean} value to define for the property.
         * @return A builder with the additional {@code boolean} property.
         */
        public Builder define(String property, boolean value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code byte} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code byte} value to define for the property.
         * @return A builder with the additional {@code byte} property.
         */
        public Builder define(String property, byte value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code char} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code char} value to define for the property.
         * @return A builder with the additional {@code char} property.
         */
        public Builder define(String property, char value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code short} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code short} value to define for the property.
         * @return A builder with the additional {@code short} property.
         */
        public Builder define(String property, short value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code int} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code int} value to define for the property.
         * @return A builder with the additional {@code int} property.
         */
        public Builder define(String property, int value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code long} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code long} value to define for the property.
         * @return A builder with the additional {@code long} property.
         */
        public Builder define(String property, long value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code float} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code float} value to define for the property.
         * @return A builder with the additional {@code float} property.
         */
        public Builder define(String property, float value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code double} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code double} value to define for the property.
         * @return A builder with the additional {@code double} property.
         */
        public Builder define(String property, double value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@link java.lang.String} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@link java.lang.String} value to define for the property.
         * @return A builder with the additional {@link java.lang.String} property.
         */
        public Builder define(String property, String value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code boolean} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code boolean} values to define for the property.
         * @return A builder with the additional {@code boolean} array property.
         */
        public Builder defineArray(String property, boolean... value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code byte} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code byte} values to define for the property.
         * @return A builder with the additional {@code byte} array property.
         */
        public Builder defineArray(String property, byte... value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code char} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code char} values to define for the property.
         * @return A builder with the additional {@code char} array property.
         */
        public Builder defineArray(String property, char... value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code short} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code short} values to define for the property.
         * @return A builder with the additional {@code short} array property.
         */
        public Builder defineArray(String property, short... value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code int} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code int} values to define for the property.
         * @return A builder with the additional {@code int} array property.
         */
        public Builder defineArray(String property, int... value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code long} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code long} values to define for the property.
         * @return A builder with the additional {@code long} array property.
         */
        public Builder defineArray(String property, long... value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code float} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code float} values to define for the property.
         * @return A builder with the additional {@code float} array property.
         */
        public Builder defineArray(String property, float... value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code double} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code double} values to define for the property.
         * @return A builder with the additional {@code double} array property.
         */
        public Builder defineArray(String property, double... value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@link java.lang.String} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@link java.lang.String} array value to define for the property.
         * @return A builder with the additional {@link java.lang.String} array property.
         */
        public Builder defineArray(String property, String... value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Creates an annotation description for the values that were defined for this builder. It is validated that all
         * properties are defined if no default value is set for an annotation property.
         *
         * @return An appropriate annotation description.
         */
        public AnnotationDescription build() {
            for (MethodDescription.InDefinedShape methodDescription : annotationType.getDeclaredMethods()) {
                AnnotationValue<?, ?> annotationValue = annotationValues.get(methodDescription.getName());
                if (annotationValue == null && methodDescription.getDefaultValue() == null) {
                    throw new IllegalStateException("No value or default value defined for " + methodDescription.getName());
                } else if (annotationValue != null && annotationValue.filter(methodDescription).getState() != AnnotationValue.State.RESOLVED) {
                    throw new IllegalStateException("Illegal annotation value for " + methodDescription + ": " + annotationValue);
                }
            }
            return new Latent(annotationType, annotationValues);
        }

        /**
         * Creates an annotation description for the values that were defined for this builder.
         *
         * @param validated {@code true} if the annotation description should be validated for having included all values.
         * @return An appropriate annotation description.
         */
        public AnnotationDescription build(boolean validated) {
            return validated
                    ? build()
                    : new Latent(annotationType, annotationValues);
        }
    }
}
