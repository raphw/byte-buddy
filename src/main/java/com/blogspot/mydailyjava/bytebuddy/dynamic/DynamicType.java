package com.blogspot.mydailyjava.bytebuddy.dynamic;

import com.blogspot.mydailyjava.bytebuddy.ClassFormatVersion;
import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapper;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.FieldRegistry;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.MethodRegistry;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.TypeInitializer;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.isConstructor;
import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.named;

/**
 * A dynamic type that is created at runtime, usually as the result of applying a
 * {@link com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType.Builder} or as the result of an
 * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType}.
 * <p/>
 * Note that the {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription}s will represent their
 * unloaded forms and therefore differ from the loaded types, especially with regards to annotations.
 */
public interface DynamicType {

    /**
     * A builder for defining a dynamic type. Implementations of such builders are usually immutable.
     *
     * @param <T> The most specific known loaded type that is implemented by the created dynamic type, usually the
     *            type itself, an interface or the direct super class.
     */
    static interface Builder<T> {

        /**
         * An abstract base implementation for a dynamic type builder. For representing the built type, the
         * {@link com.blogspot.mydailyjava.bytebuddy.dynamic.TargetType} class can be used as a placeholder.
         *
         * @param <T> The most specific known loaded type that is implemented by the created dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        static abstract class AbstractBase<T> implements Builder<T> {

            /**
             * A method token representing a latent method that is defined for the built dynamic type.
             */
            protected static class MethodToken implements MethodRegistry.LatentMethodMatcher {

                private static class SignatureMatcher implements MethodMatcher {

                    private final TypeDescription returnType;
                    private final List<TypeDescription> parameterTypes;

                    private SignatureMatcher(TypeDescription returnType, List<TypeDescription> parameterTypes) {
                        this.returnType = returnType;
                        this.parameterTypes = parameterTypes;
                    }

                    @Override
                    public boolean matches(MethodDescription methodDescription) {
                        return methodDescription.getReturnType().equals(returnType)
                                && methodDescription.getParameterTypes().equals(parameterTypes);
                    }

                    @Override
                    public String toString() {
                        return "hasSignature(returnType=" + returnType + ", parameterTypes=" + parameterTypes + ')';
                    }
                }

                /**
                 * The internal name of the method.
                 */
                protected final String internalName;

                /**
                 * A description of the return type of the method or a type describing the
                 * {@link com.blogspot.mydailyjava.bytebuddy.dynamic.TargetType} placeholder.
                 */
                protected final TypeDescription returnType;

                /**
                 * A list of parameter type descriptions for the method which might be represented by the
                 * {@link com.blogspot.mydailyjava.bytebuddy.dynamic.TargetType} placeholder.
                 */
                protected final List<TypeDescription> parameterTypes;

                /**
                 * A list of modifiers of the method.
                 */
                protected final int modifiers;

                /**
                 * Creates a new method token representing a method to implement for the built dynamic type.
                 *
                 * @param internalName   The internal internalName of the method.
                 * @param returnType     The return type of the method.
                 * @param parameterTypes A list of parameters for the method.
                 * @param modifiers      The modifers of the method.
                 */
                public MethodToken(String internalName,
                                   TypeDescription returnType,
                                   List<? extends TypeDescription> parameterTypes,
                                   int modifiers) {
                    this.internalName = internalName;
                    this.returnType = returnType;
                    this.parameterTypes = Collections.unmodifiableList(new ArrayList<TypeDescription>(parameterTypes));
                    this.modifiers = modifiers;
                }

                @Override
                public MethodMatcher manifest(TypeDescription instrumentedType) {
                    return (MethodDescription.CONSTRUCTOR_INTERNAL_NAME.equals(internalName) ? isConstructor() : named(internalName))
                            .and(new SignatureMatcher(resolveReturnType(instrumentedType), resolveParameterTypes(instrumentedType)));
                }

                /**
                 * Resolves the return type for the method which could be represented by the
                 * {@link com.blogspot.mydailyjava.bytebuddy.dynamic.TargetType} placeholder type.
                 *
                 * @param instrumentedType The instrumented place which is used for replacement.
                 * @return A type description for the actual return type.
                 */
                protected TypeDescription resolveReturnType(TypeDescription instrumentedType) {
                    return considerSubstitution(returnType, instrumentedType);
                }

                /**
                 * Resolves the parameter types for the method which could be represented by the
                 * {@link com.blogspot.mydailyjava.bytebuddy.dynamic.TargetType} placeholder type.
                 *
                 * @param instrumentedType The instrumented place which is used for replacement.
                 * @return A list of type descriptions for the actual parameter types.
                 */
                protected List<TypeDescription> resolveParameterTypes(TypeDescription instrumentedType) {
                    List<TypeDescription> parameterTypes = new ArrayList<TypeDescription>(this.parameterTypes.size());
                    for (TypeDescription parameterType : this.parameterTypes) {
                        parameterTypes.add(considerSubstitution(parameterType, instrumentedType));
                    }
                    return parameterTypes;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && modifiers == ((MethodToken) other).modifiers
                            && internalName.equals(((MethodToken) other).internalName)
                            && parameterTypes.equals(((MethodToken) other).parameterTypes)
                            && returnType.equals(((MethodToken) other).returnType);
                }

                @Override
                public int hashCode() {
                    int result = internalName.hashCode();
                    result = 31 * result + returnType.hashCode();
                    result = 31 * result + parameterTypes.hashCode();
                    result = 31 * result + modifiers;
                    return result;
                }

                @Override
                public String toString() {
                    return "MethodToken{" +
                            "internalName='" + internalName + '\'' +
                            ", returnType=" + returnType +
                            ", parameterTypes=" + parameterTypes +
                            ", modifiers=" + modifiers + '}';
                }
            }

            /**
             * A field token representing a latent field that is defined for the built dynamic type.
             */
            protected static class FieldToken implements FieldRegistry.LatentFieldMatcher {

                /**
                 * The name of the field.
                 */
                protected final String name;

                /**
                 * A description of the field type or a description of the
                 * {@link com.blogspot.mydailyjava.bytebuddy.dynamic.TargetType} placeholder.
                 */
                protected final TypeDescription fieldType;

                /**
                 * The field modifiers.
                 */
                protected final int modifiers;

                /**
                 * Creates a new field token.
                 *
                 * @param name      The name of the field.
                 * @param fieldType A description of the field type.
                 * @param modifiers The modifers of the field.
                 */
                public FieldToken(String name, TypeDescription fieldType, int modifiers) {
                    this.name = name;
                    this.fieldType = fieldType;
                    this.modifiers = modifiers;
                }

                /**
                 * Resolves the field type which could be represented by the
                 * {@link com.blogspot.mydailyjava.bytebuddy.dynamic.TargetType} placeholder type.
                 *
                 * @param instrumentedType The instrumented place which is used for replacement.
                 * @return A type description for the actual field type.
                 */
                protected TypeDescription resolveFieldType(TypeDescription instrumentedType) {
                    return considerSubstitution(fieldType, instrumentedType);
                }

                @Override
                public String getFieldName() {
                    return name;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && modifiers == ((FieldToken) other).modifiers
                            && fieldType.equals(((FieldToken) other).fieldType)
                            && name.equals(((FieldToken) other).name);
                }

                @Override
                public int hashCode() {
                    int result = name.hashCode();
                    result = 31 * result + fieldType.hashCode();
                    result = 31 * result + modifiers;
                    return result;
                }

                @Override
                public String toString() {
                    return "FieldToken{" +
                            "internalName='" + name + '\'' +
                            ", fieldType=" + fieldType +
                            ", modifiers=" + modifiers + '}';
                }
            }

            private static TypeDescription considerSubstitution(TypeDescription type, TypeDescription instrumentedType) {
                return type.represents(TargetType.class) ? instrumentedType : type;
            }

            /**
             * A base implementation of a builder that is capable of manifesting a change that was not yet applied to
             * the builder.
             *
             * @param <T> The most specific known loaded type that is implemented by the created dynamic type, usually the
             *            type itself, an interface or the direct super class.
             */
            protected abstract class AbstractDelegatingBuilder<T> implements Builder<T> {

                @Override
                public Builder<T> classFormatVersion(ClassFormatVersion classFormatVersion) {
                    return materialize().classFormatVersion(classFormatVersion);
                }

                @Override
                public OptionalMatchedMethodInterception<T> implement(Class<?> interfaceType) {
                    return materialize().implement(interfaceType);
                }

                @Override
                public OptionalMatchedMethodInterception<T> implement(TypeDescription interfaceType) {
                    return materialize().implement(interfaceType);
                }

                @Override
                public Builder<T> name(String name) {
                    return materialize().name(name);
                }

                @Override
                public Builder<T> modifiers(ModifierContributor.ForType... modifier) {
                    return materialize().modifiers(modifier);
                }

                @Override
                public Builder<T> ignoreMethods(MethodMatcher ignoredMethods) {
                    return materialize().ignoreMethods(ignoredMethods);
                }

                @Override
                public Builder<T> attribute(TypeAttributeAppender attributeAppender) {
                    return materialize().attribute(attributeAppender);
                }

                @Override
                public Builder<T> annotateType(Annotation... annotation) {
                    return materialize().annotateType(annotation);
                }

                @Override
                public Builder<T> classVisitor(ClassVisitorWrapper classVisitorWrapper) {
                    return materialize().classVisitor(classVisitorWrapper);
                }

                @Override
                public FieldAnnotationTarget<T> defineField(String name,
                                                            Class<?> fieldType,
                                                            ModifierContributor.ForField... modifier) {
                    return materialize().defineField(name, fieldType, modifier);
                }

                @Override
                public FieldAnnotationTarget<T> defineField(String name, TypeDescription fieldTypeDescription, ModifierContributor.ForField... modifier) {
                    return materialize().defineField(name, fieldTypeDescription, modifier);
                }

                @Override
                public MatchedMethodInterception<T> defineMethod(String name,
                                                                 Class<?> returnType,
                                                                 List<Class<?>> parameterTypes,
                                                                 ModifierContributor.ForMethod... modifier) {
                    return materialize().defineMethod(name, returnType, parameterTypes, modifier);
                }

                @Override
                public MatchedMethodInterception<T> defineMethod(String name,
                                                                 TypeDescription returnType,
                                                                 List<? extends TypeDescription> parameterTypes,
                                                                 ModifierContributor.ForMethod... modifier) {
                    return materialize().defineMethod(name, returnType, parameterTypes, modifier);
                }

                @Override
                public MatchedMethodInterception<T> defineConstructor(List<Class<?>> parameterTypes,
                                                                      ModifierContributor.ForMethod... modifier) {
                    return materialize().defineConstructor(parameterTypes, modifier);
                }

                @Override
                public MatchedMethodInterception<T> defineConstructorDescriptive(List<? extends TypeDescription> parameterTypes,
                                                                                 ModifierContributor.ForMethod... modifier) {
                    return materialize().defineConstructorDescriptive(parameterTypes, modifier);
                }

                @Override
                public MatchedMethodInterception<T> method(MethodMatcher methodMatcher) {
                    return materialize().method(methodMatcher);
                }

                @Override
                public MatchedMethodInterception<T> constructor(MethodMatcher methodMatcher) {
                    return materialize().constructor(methodMatcher);
                }

                @Override
                public MatchedMethodInterception<T> invokable(MethodMatcher methodMatcher) {
                    return materialize().invokable(methodMatcher);
                }

                @Override
                public Unloaded<T> make() {
                    return materialize().make();
                }

                /**
                 * Materializes the current state of the build before applying another modification.
                 *
                 * @return A builder with all pending changes materialized.
                 */
                protected abstract Builder<T> materialize();
            }

            /**
             * This builder's currently registered field tokens.
             */
            protected final List<FieldToken> fieldTokens;

            /**
             * This builder's currently registered method tokens.
             */
            protected final List<MethodToken> methodTokens;

            /**
             * Creates a new builder for a dynamic type.
             *
             * @param fieldTokens  A list of fields registered for this builder.
             * @param methodTokens A list of methods registered for this builder.
             */
            protected AbstractBase(List<FieldToken> fieldTokens, List<MethodToken> methodTokens) {
                this.fieldTokens = fieldTokens;
                this.methodTokens = methodTokens;
            }

            /**
             * Adds all fields and methods to an instrumented type.
             *
             * @param instrumentedType The instrumented type that is basis of the alteration.
             * @return The created instrumented type with all fields and methods applied.
             */
            protected InstrumentedType applyRecordedMembersTo(InstrumentedType instrumentedType) {
                for (FieldToken fieldToken : fieldTokens) {
                    instrumentedType = instrumentedType.withField(fieldToken.name,
                            fieldToken.resolveFieldType(instrumentedType),
                            fieldToken.modifiers);
                }
                for (MethodToken methodToken : methodTokens) {
                    instrumentedType = instrumentedType.withMethod(methodToken.internalName,
                            methodToken.resolveReturnType(instrumentedType),
                            methodToken.resolveParameterTypes(instrumentedType),
                            methodToken.modifiers);
                }
                return instrumentedType;
            }

            @Override
            public OptionalMatchedMethodInterception<T> implement(Class<?> interfaceType) {
                return implement(new TypeDescription.ForLoadedType(interfaceType));
            }

            @Override
            public FieldAnnotationTarget<T> defineField(String name,
                                                        Class<?> fieldType,
                                                        ModifierContributor.ForField... modifier) {
                return defineField(name, new TypeDescription.ForLoadedType(fieldType), modifier);
            }

            @Override
            public MatchedMethodInterception<T> defineMethod(String name,
                                                             Class<?> returnType,
                                                             List<Class<?>> parameterTypes,
                                                             ModifierContributor.ForMethod... modifier) {
                return defineMethod(name,
                        new TypeDescription.ForLoadedType(returnType),
                        new TypeList.ForLoadedType(parameterTypes),
                        modifier);
            }

            @Override
            public MatchedMethodInterception<T> defineConstructor(List<Class<?>> parameterTypes,
                                                                  ModifierContributor.ForMethod... modifier) {
                return null;
            }
        }

        /**
         * Defines an instrumentation for a method that was added to this instrumentation or a method selection.
         *
         * @param <T> The most specific known loaded type that is implemented by the created dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        static interface MatchedMethodInterception<T> {

            /**
             * Intercepts the currently selected method by a given instrumentation.
             *
             * @param instrumentation An instrumentation to apply to the currently selected method.
             * @return A builder which will intercept the currently selected methods by the given instrumentation.
             */
            MethodAnnotationTarget<T> intercept(Instrumentation instrumentation);

            /**
             * Implements the currently selected methods as {@code abstract} methods.
             *
             * @return A builder which will implement the currently selected methods as {@code abstract} methods.
             */
            MethodAnnotationTarget<T> withoutCode();
        }

        /**
         * An optional matched method interception allows to define an interception without requiring the definition
         * of an implementation.
         *
         * @param <T> The most specific known loaded type that is implemented by the created dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        static interface OptionalMatchedMethodInterception<T> extends MatchedMethodInterception<T>, Builder<T> {
            /* This interface is merely a combinator of the matched method interception and the builder interfaces. */
        }

        /**
         * A builder to which a method was just added or an interception for existing methods was specified such that
         * attribute changes can be applied to these methods.
         *
         * @param <T> The most specific known loaded type that is implemented by the created dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        static interface MethodAnnotationTarget<T> extends Builder<T> {

            /**
             * Defines an attribute appender factory to be applied onto the currently selected methods.
             *
             * @param attributeAppenderFactory The attribute appender factory to apply onto the currently selected
             *                                 methods.
             * @return A builder where the given attribute appender factory will be applied to the currently selected methods.
             */
            MethodAnnotationTarget<T> attribute(MethodAttributeAppender.Factory attributeAppenderFactory);

            /**
             * Defines annotations to be added to the currently selected method.
             * <p/>
             * Note: The annotations will not be visible to
             * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation}s.
             *
             * @param annotation The annotations to add to the currently selected methods.
             * @return A builder where the given annotation will be added to the currently selected methods.
             */
            MethodAnnotationTarget<T> annotateMethod(Annotation... annotation);

            /**
             * Defines annotations to be added to a parameter of the currently selected methods.
             * <p/>
             * Note: The annotations will not be visible to
             * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation}s.
             *
             * @param annotation The annotations to add to a parameter of the currently selected methods.
             * @return A builder where the given annotation will be added to a parameter of the currently selected
             * methods.
             */
            MethodAnnotationTarget<T> annotateParameter(int parameterIndex, Annotation... annotation);
        }

        /**
         * A builder to which a field was just added such that attribute changes can be applied to this field.
         *
         * @param <T> The most specific known type of the dynamic type, usually the type itself, an interface or the direct super class.
         */
        static interface FieldAnnotationTarget<T> extends Builder<T> {

            /**
             * Defines an attribute appender factory to be applied onto the currently selected field.
             *
             * @param attributeAppenderFactory The attribute appender factory to apply onto the currently selected
             *                                 field.
             * @return A builder where the given attribute appender factory will be applied to the currently selected field.
             */
            FieldAnnotationTarget<T> attribute(FieldAttributeAppender.Factory attributeAppenderFactory);

            /**
             * Defines annotations to be added to the currently selected field.
             * <p/>
             * Note: The annotations will not be visible to
             * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation}s.
             *
             * @param annotation The annotations to add to the currently selected field.
             * @return A builder where the given annotation will be added to the currently selected field.
             */
            FieldAnnotationTarget<T> annotateField(Annotation... annotation);
        }

        /**
         * Defines a class file format version for this builder for which the dynamic types should be created.
         *
         * @param classFormatVersion The class format version for the dynamic type to implement.
         * @return A builder that writes its classes in a given class format version.
         */
        Builder<T> classFormatVersion(ClassFormatVersion classFormatVersion);

        /**
         * Adds an interface to be implemented the created type.
         *
         * @param interfaceType The interface to implement.
         * @return A builder which will create a dynamic type that implements the given interface.
         */
        OptionalMatchedMethodInterception<T> implement(Class<?> interfaceType);

        /**
         * Adds an interface to be implemented the created type.
         *
         * @param interfaceType A description of the interface to implement.
         * @return A builder which will create a dynamic type that implements the given interface.
         */
        OptionalMatchedMethodInterception<T> implement(TypeDescription interfaceType);

        /**
         * Names the currently created dynamic type by a fixed name.
         *
         * @param name A fully qualified name to give to the created dynamic type.
         * @return A builder that will name its dynamic type by the given name.
         */
        Builder<T> name(String name);

        /**
         * Defines modifiers for the created dynamic type.
         *
         * @param modifier A collection of modifiers to be reflected by the created dynamic type.
         * @return A builder that will create a dynamic type that reflects the given modifiers.
         */
        Builder<T> modifiers(ModifierContributor.ForType... modifier);

        /**
         * Defines a matcher for methods that will be ignored for any interception attempt. Any methods
         * that were directly declared on an instrumented type will never be ignored, i.e. ignored methods
         * only represent a filter for methods that are declared in super types that should never be overriden.
         *
         * @param ignoredMethods A method matcher characterizing the methods to be ignored.
         * @return A builder that will always ignore the methods matched by the given method matcher.
         */
        Builder<T> ignoreMethods(MethodMatcher ignoredMethods);

        /**
         * Adds an attribute appender to the currently constructed type which will be applied on the creation of
         * the type.
         *
         * @param attributeAppender An attribute appender to be applied onto the currently created type.
         * @return A builder that will apply the given attribute appender onto the currently created type.
         */
        Builder<T> attribute(TypeAttributeAppender attributeAppender);

        /**
         * Adds annotations to the currently constructed type.
         * <p/>
         * Note: The annotations will not be visible to
         * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation}s.
         *
         * @param annotation The annotations to be added to the currently constructed type.
         * @return A builder that will add the given annotation to the created type.
         */
        Builder<T> annotateType(Annotation... annotation);

        /**
         * Adds an additional ASM {@link org.objectweb.asm.ClassVisitor} to this builder which will be applied in
         * the construction process of this dynamic type.
         *
         * @param classVisitorWrapper The wrapper delegate for the ASM class visitor.
         * @return A builder that will apply the given ASM class visitor.
         */
        Builder<T> classVisitor(ClassVisitorWrapper classVisitorWrapper);

        /**
         * Defines a new field for this type.
         *
         * @param name      The name of the method.
         * @param fieldType The type of this field where the current type can be represented by
         *                  {@link com.blogspot.mydailyjava.bytebuddy.dynamic.TargetType}.
         * @param modifier  The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        FieldAnnotationTarget<T> defineField(String name,
                                             Class<?> fieldType,
                                             ModifierContributor.ForField... modifier);

        /**
         * Defines a new field for this type.
         *
         * @param name                 The name of the method.
         * @param fieldTypeDescription The type of this field where the current type can be represented by
         *                             {@link com.blogspot.mydailyjava.bytebuddy.dynamic.TargetType}.
         * @param modifier             The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        FieldAnnotationTarget<T> defineField(String name,
                                             TypeDescription fieldTypeDescription,
                                             ModifierContributor.ForField... modifier);

        /**
         * Defines a new method for this type.
         *
         * @param name           The name of the method.
         * @param returnType     The return type of the method  where the current type can be represented by
         *                       {@link com.blogspot.mydailyjava.bytebuddy.dynamic.TargetType}.
         * @param parameterTypes The parameter types of this method  where the current type can be represented by
         *                       {@link com.blogspot.mydailyjava.bytebuddy.dynamic.TargetType}.
         * @param modifier       The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        MatchedMethodInterception<T> defineMethod(String name,
                                                  Class<?> returnType,
                                                  List<Class<?>> parameterTypes,
                                                  ModifierContributor.ForMethod... modifier);

        /**
         * Defines a new method for this type.
         *
         * @param name           The name of the method.
         * @param returnType     A description of the return type of the method  where the current type can be
         *                       represented by {@link com.blogspot.mydailyjava.bytebuddy.dynamic.TargetType}.
         * @param parameterTypes Descriptions of the parameter types of this method  where the current type can be
         *                       represented by {@link com.blogspot.mydailyjava.bytebuddy.dynamic.TargetType}.
         * @param modifier       The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        MatchedMethodInterception<T> defineMethod(String name,
                                                  TypeDescription returnType,
                                                  List<? extends TypeDescription> parameterTypes,
                                                  ModifierContributor.ForMethod... modifier);

        /**
         * Defines a new constructor for this type.
         *
         * @param parameterTypes The parameter types of this constructor  where the current type can be represented by
         *                       {@link com.blogspot.mydailyjava.bytebuddy.dynamic.TargetType}.
         * @param modifier       The modifiers for this constructor.
         * @return An interception delegate that exclusively matches the new constructor.
         */
        MatchedMethodInterception<T> defineConstructor(List<Class<?>> parameterTypes,
                                                       ModifierContributor.ForMethod... modifier);

        /**
         * Defines a new constructor for this type.
         *
         * @param parameterTypes The descriptions of the parameter types of this constructor where the current type can be
         *                       represented by a description of {@link com.blogspot.mydailyjava.bytebuddy.dynamic.TargetType}.
         * @param modifier       The modifiers for this constructor.
         * @return An interception delegate that exclusively matches the new constructor.
         */
        MatchedMethodInterception<T> defineConstructorDescriptive(List<? extends TypeDescription> parameterTypes,
                                                                  ModifierContributor.ForMethod... modifier);

        /**
         * Selects a set of methods of this type for instrumentation.
         *
         * @param methodMatcher A matcher describing the methods to be intercepted by this instrumentation.
         * @return An interception delegate for methods matching the given method matcher.
         */
        MatchedMethodInterception<T> method(MethodMatcher methodMatcher);

        /**
         * Selects a set of constructors of this type for instrumentation.
         *
         * @param methodMatcher A matcher describing the constructors to be intercepted by this instrumentation.
         * @return An interception delegate for constructors matching the given method matcher.
         */
        MatchedMethodInterception<T> constructor(MethodMatcher methodMatcher);

        /**
         * Selects a set of byte code methods of this type for instrumentation.
         *
         * @param methodMatcher A matcher describing the byte code methods to be intercepted by this instrumentation.
         * @return An interception delegate for byte code methods matching the given method matcher.
         */
        MatchedMethodInterception<T> invokable(MethodMatcher methodMatcher);

        /**
         * Creates the dynamic type without loading it.
         *
         * @return An unloaded representation of the dynamic type.
         */
        Unloaded<T> make();
    }

    /**
     * A dynamic type that has been loaded into the running instance of the Java virtual machine.
     *
     * @param <T> The most specific known loaded type that is implemented by this dynamic type, usually the
     *            type itself, an interface or the direct super class.
     */
    static interface Loaded<T> extends DynamicType {

        /**
         * Returns the loaded main class.
         *
         * @return A loaded class representation of this dynamic type.
         */
        Class<? extends T> getLoaded();

        /**
         * Returns a map of all loaded auxiliary types to this dynamic type.
         *
         * @return A mapping from the fully qualified names of all auxiliary types to their loaded class representations.
         */
        Map<TypeDescription, Class<?>> getLoadedAuxiliaryTypes();
    }

    /**
     * A dynamic type that has not yet been loaded by a given {@link java.lang.ClassLoader}.
     *
     * @param <T> The most specific known loaded type that is implemented by this dynamic type, usually the
     *            type itself, an interface or the direct super class.
     */
    static interface Unloaded<T> extends DynamicType {

        /**
         * Attempts to load this dynamic type including all of its auxiliary types, if any.
         *
         * @param classLoader          The class loader to use for this class loading.
         * @param classLoadingStrategy The class loader strategy which should be used for this class loading.
         * @return This dynamic type in its loaded state.
         * @see com.blogspot.mydailyjava.bytebuddy.dynamic.ClassLoadingStrategy.Default
         */
        Loaded<T> load(ClassLoader classLoader, ClassLoadingStrategy classLoadingStrategy);
    }

    /**
     * A default implementation of a dynamic type.
     */
    static class Default implements DynamicType {

        /**
         * A default implementation of an unloaded dynamic type.
         *
         * @param <T> The most specific known loaded type that is implemented by this dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        public static class Unloaded<T> extends Default implements DynamicType.Unloaded<T> {

            /**
             * Creates a new unloaded representation of a dynamic type.
             *
             * @param typeDescription A description of this dynamic type.
             * @param typeByte        An array of byte of the binary representation of this dynamic type.
             * @param typeInitializer The type initializer of this dynamic type.
             * @param auxiliaryTypes  The auxiliary types that are required for this dynamic type.
             */
            public Unloaded(TypeDescription typeDescription,
                            byte[] typeByte,
                            TypeInitializer typeInitializer,
                            List<? extends DynamicType> auxiliaryTypes) {
                super(typeDescription, typeByte, typeInitializer, auxiliaryTypes);
            }

            @Override
            public DynamicType.Loaded<T> load(ClassLoader classLoader, ClassLoadingStrategy classLoadingStrategy) {
                LinkedHashMap<TypeDescription, byte[]> types = new LinkedHashMap<TypeDescription, byte[]>(getRawAuxiliaryTypes());
                types.put(typeDescription, binaryRepresentation);
                return new Default.Loaded<T>(typeDescription,
                        binaryRepresentation,
                        typeInitializer,
                        auxiliaryTypes,
                        initialize(classLoadingStrategy.load(classLoader, types)));
            }

            private Map<TypeDescription, Class<?>> initialize(Map<TypeDescription, Class<?>> uninitialized) {
                Map<TypeDescription, TypeInitializer> typeInitializers = getTypeInitializers();
                for (Map.Entry<TypeDescription, Class<?>> entry : uninitialized.entrySet()) {
                    typeInitializers.get(entry.getKey()).onLoad(entry.getValue());
                }
                return new HashMap<TypeDescription, Class<?>>(uninitialized);
            }

            @Override
            public String toString() {
                return "DynamicType.Default.Unloaded{" +
                        "typeDescription='" + typeDescription + '\'' +
                        ", binaryRepresentation=" + Arrays.toString(binaryRepresentation) +
                        ", typeInitializer=" + typeInitializer +
                        ", auxiliaryTypes=" + auxiliaryTypes +
                        '}';
            }
        }

        /**
         * A default implementation of a loaded dynamic type.
         *
         * @param <T> The most specific known loaded type that is implemented by this dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        protected static class Loaded<T> extends Default implements DynamicType.Loaded<T> {

            private final Map<TypeDescription, Class<?>> loadedTypes;

            /**
             * Creates a new representation of a loaded dynamic type.
             *
             * @param typeDescription A description of this dynamic type.
             * @param typeByte        An array of byte of the binary representation of this dynamic type.
             * @param typeInitializer The type initializer of this dynamic type.
             * @param auxiliaryTypes  The auxiliary types that are required for this dynamic type.
             * @param loadedTypes     A map of loaded types for this dynamic type and all its auxiliary types.
             */
            protected Loaded(TypeDescription typeDescription,
                             byte[] typeByte,
                             TypeInitializer typeInitializer,
                             List<? extends DynamicType> auxiliaryTypes,
                             Map<TypeDescription, Class<?>> loadedTypes) {
                super(typeDescription, typeByte, typeInitializer, auxiliaryTypes);
                this.loadedTypes = loadedTypes;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends T> getLoaded() {
                return (Class<? extends T>) loadedTypes.get(typeDescription);
            }

            @Override
            public Map<TypeDescription, Class<?>> getLoadedAuxiliaryTypes() {
                Map<TypeDescription, Class<?>> loadedAuxiliaryTypes = new HashMap<TypeDescription, Class<?>>(loadedTypes);
                loadedAuxiliaryTypes.remove(typeDescription);
                return loadedAuxiliaryTypes;
            }

            @Override
            public String toString() {
                return "DynamicType.Default.Loaded{" +
                        "typeDescription='" + typeDescription + '\'' +
                        ", binaryRepresentation=" + Arrays.toString(binaryRepresentation) +
                        ", typeInitializer=" + typeInitializer +
                        ", auxiliaryTypes=" + auxiliaryTypes +
                        '}';
            }
        }

        private static final String CLASS_FILE_EXTENSION = ".class";

        /**
         * A type description of this dynamic type.
         */
        protected final TypeDescription typeDescription;

        /**
         * The byte array representing this dynamic type.
         */
        protected final byte[] binaryRepresentation;

        /*
         * The type initializer for this dynamic type.
         */
        protected final TypeInitializer typeInitializer;

        /**
         * A list of auxiliary types for this dynamic type.
         */
        protected final List<? extends DynamicType> auxiliaryTypes;

        /**
         * Creates a new dynamic type.
         *
         * @param typeDescription      A description of this dynamic type.
         * @param binaryRepresentation A byte array containing the binary representation of this dynamic type.
         * @param typeInitializer      The type initializer of this dynamic type.
         * @param auxiliaryTypes       The auxiliary type required for this dynamic type.
         */
        public Default(TypeDescription typeDescription,
                       byte[] binaryRepresentation,
                       TypeInitializer typeInitializer,
                       List<? extends DynamicType> auxiliaryTypes) {
            this.typeDescription = typeDescription;
            this.binaryRepresentation = binaryRepresentation;
            this.typeInitializer = typeInitializer;
            this.auxiliaryTypes = auxiliaryTypes;
        }

        @Override
        public TypeDescription getDescription() {
            return typeDescription;
        }

        @Override
        public Map<TypeDescription, TypeInitializer> getTypeInitializers() {
            Map<TypeDescription, TypeInitializer> classLoadingCallbacks = new HashMap<TypeDescription, TypeInitializer>();
            for (DynamicType auxiliaryType : auxiliaryTypes) {
                classLoadingCallbacks.putAll(auxiliaryType.getTypeInitializers());
            }
            classLoadingCallbacks.put(typeDescription, typeInitializer);
            return classLoadingCallbacks;
        }

        @Override
        public boolean hasAliveTypeInitializers() {
            for (TypeInitializer typeInitializer : getTypeInitializers().values()) {
                if (typeInitializer.isAlive()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public byte[] getByte() {
            return binaryRepresentation;
        }

        @Override
        public Map<TypeDescription, byte[]> getRawAuxiliaryTypes() {
            Map<TypeDescription, byte[]> auxiliaryTypes = new HashMap<TypeDescription, byte[]>();
            for (DynamicType auxiliaryType : this.auxiliaryTypes) {
                auxiliaryTypes.put(auxiliaryType.getDescription(), auxiliaryType.getByte());
                auxiliaryTypes.putAll(auxiliaryType.getRawAuxiliaryTypes());
            }
            return auxiliaryTypes;
        }

        @Override
        public Map<TypeDescription, File> saveIn(File folder) throws IOException {
            Map<TypeDescription, File> savedFiles = new HashMap<TypeDescription, File>();
            File target = new File(folder, typeDescription.getName().replace('.', File.separatorChar) + CLASS_FILE_EXTENSION);
            FileOutputStream fileOutputStream = new FileOutputStream(target);
            try {
                fileOutputStream.write(binaryRepresentation);
            } finally {
                fileOutputStream.close();
            }
            savedFiles.put(typeDescription, target);
            for (DynamicType auxiliaryType : auxiliaryTypes) {
                savedFiles.putAll(auxiliaryType.saveIn(folder));
            }
            return savedFiles;
        }

        @Override
        public String toString() {
            return "DynamicType.Default{" +
                    "typeDescription='" + typeDescription + '\'' +
                    ", binaryRepresentation=" + Arrays.toString(binaryRepresentation) +
                    ", typeInitializer=" + typeInitializer +
                    ", auxiliaryTypes=" + auxiliaryTypes +
                    '}';
        }
    }

    /**
     * Returns a description of this dynamic type.
     *
     * @return A description of this dynamic type.
     */
    TypeDescription getDescription();

    /**
     * Returns a byte array representing this dynamic type. This byte array might be reused by this dynamic type and
     * should therefore not be altered.
     *
     * @return A byte array of the type's binary representation.
     */
    byte[] getByte();

    /**
     * Returns a map of all auxiliary types that are required for making use of the main type.
     *
     * @return A map of all auxiliary types by their descriptions to their binary representation.
     */
    Map<TypeDescription, byte[]> getRawAuxiliaryTypes();

    /**
     * Returns a map of all type initializers for the main type and all auxiliary types, if any.
     *
     * @return A mapping of all types' descriptions to their type initializers.
     */
    Map<TypeDescription, TypeInitializer> getTypeInitializers();

    /**
     * Checks if a dynamic type requires some form of explicit type initialization, either for itself or for one
     * of its auxiliary types, if any. This is the case when this dynamic type was defined to delegate method calls
     * to a specific instance which is stored in a field of the created type. If this class serialized, it could not
     * be used without its type initializers since the field value represents a specific runtime context.
     *
     * @return {@code true} if this type requires explicit type initialization.
     */
    boolean hasAliveTypeInitializers();

    /**
     * Saves a dynamic type in a given folder using the Java class file format while respecting the naming conventions
     * for saving compiled Java classes. All auxiliary types, if any, are saved in the same directory. The resulting
     * folder structure will resemble the structure that is required for Java runtimes, i.e. each folder representing
     * a segment of the package name.
     *
     * @param folder The base target folder for storing this dynamic type and its auxiliary types, if any.
     * @return A map of type descriptions pointing to files with their stored binary representations within {@code folder}.
     * @throws IOException Thrown if the underlying file operations cause an {@code IOException}.
     */
    Map<TypeDescription, File> saveIn(File folder) throws IOException;
}
