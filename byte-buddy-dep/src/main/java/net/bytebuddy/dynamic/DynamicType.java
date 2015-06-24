package net.bytebuddy.dynamic;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.FieldRegistry;
import net.bytebuddy.dynamic.scaffold.MethodLookupEngine;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMethodMatcher;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Logger;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static net.bytebuddy.utility.ByteBuddyCommons.*;

/**
 * A dynamic type that is created at runtime, usually as the result of applying a
 * {@link net.bytebuddy.dynamic.DynamicType.Builder} or as the result of an
 * {@link net.bytebuddy.implementation.auxiliary.AuxiliaryType}.
 * <p>&nbsp;</p>
 * Note that the {@link TypeDescription}s will represent their
 * unloaded forms and therefore differ from the loaded types, especially with regards to annotations.
 */
public interface DynamicType {

    /**
     * <p>
     * Returns a description of this dynamic type.
     * </p>
     * <p>
     * <b>Note</b>: This description will most likely differ from the binary representation of this type. Normally,
     * annotations and intercepted methods are not added to this type description.
     * </p>
     *
     * @return A description of this dynamic type.
     */
    TypeDescription getTypeDescription();

    /**
     * Returns a byte array representing this dynamic type. This byte array might be reused by this dynamic type and
     * must therefore not be altered.
     *
     * @return A byte array of the type's binary representation.
     */
    byte[] getBytes();

    /**
     * <p>
     * Returns a map of all auxiliary types that are required for making use of the main type.
     * </p>
     * <p>
     * <b>Note</b>: The type descriptions will most likely differ from the binary representation of this type.
     * Normally, annotations and intercepted methods are not added to the type descriptions of auxiliary types.
     * </p>
     *
     * @return A map of all auxiliary types by their descriptions to their binary representation.
     */
    Map<TypeDescription, byte[]> getRawAuxiliaryTypes();

    /**
     * Returns all types that are implied by this dynamic type.
     *
     * @return A mapping from all type descriptions, the actual type and its auxiliary types to their binary
     * representation
     */
    Map<TypeDescription, byte[]> getAllTypes();

    /**
     * <p>
     * Returns a map of all loaded type initializers for the main type and all auxiliary types, if any.
     * </p>
     * <p>
     * <b>Note</b>: The type descriptions will most likely differ from the binary representation of this type.
     * Normally, annotations and intercepted methods are not added to the type descriptions of auxiliary types.
     * </p>
     *
     * @return A mapping of all types' descriptions to their loaded type initializers.
     */
    Map<TypeDescription, LoadedTypeInitializer> getLoadedTypeInitializers();

    /**
     * Checks if a dynamic type requires some form of explicit type initialization, either for itself or for one
     * of its auxiliary types, if any. This is the case when this dynamic type was defined to delegate method calls
     * to a specific instance which is stored in a field of the created type. If this class serialized, it could not
     * be used without its loaded type initializers since the field value represents a specific runtime context.
     *
     * @return {@code true} if this type requires explicit type initialization.
     */
    boolean hasAliveLoadedTypeInitializers();

    /**
     * <p>
     * Saves a dynamic type in a given folder using the Java class file format while respecting the naming conventions
     * for saving compiled Java classes. All auxiliary types, if any, are saved in the same directory. The resulting
     * folder structure will resemble the structure that is required for Java run times, i.e. each folder representing
     * a segment of the package name. If the specified {@code folder} does not yet exist, it is created during the
     * call of this method.
     * </p>
     * <p>
     * <b>Note</b>: The type descriptions will most likely differ from the binary representation of this type.
     * Normally, annotations and intercepted methods are not added to the type descriptions of auxiliary types.
     * </p>
     *
     * @param folder The base target folder for storing this dynamic type and its auxiliary types, if any.
     * @return A map of type descriptions pointing to files with their stored binary representations within {@code folder}.
     * @throws IOException Thrown if the underlying file operations cause an {@code IOException}.
     */
    Map<TypeDescription, File> saveIn(File folder) throws IOException;

    /**
     * Injects the types of this dynamic type into a given <i>jar</i> file. Any pre-existent type with the same name
     * is overridden during injection. The {@code target} file's folder must exist prior to calling this method. The
     * file itself is overwritten or created depending on its prior existence.
     *
     * @param sourceJar The original jar file.
     * @param targetJar The {@code source} jar file with the injected contents.
     * @return The {@code target} jar file.
     * @throws IOException If an IO exception occurs while injecting from the source into the target.
     */
    File inject(File sourceJar, File targetJar) throws IOException;

    /**
     * Injects the types of this dynamic type into a given <i>jar</i> file. Any pre-existent type with the same name
     * is overridden during injection.
     *
     * @param jar The jar file to replace with an injected version.
     * @return The {@code jar} file.
     * @throws IOException If an IO exception occurs while injecting into the jar.
     */
    File inject(File jar) throws IOException;

    /**
     * Saves the contents of this dynamic type inside a <i>jar</i> file. The folder of the given {@code file} must
     * exist prior to calling this method.
     *
     * @param file     The target file to which the <i>jar</i> is written to.
     * @param manifest The manifest of the created <i>jar</i>.
     * @return The given {@code file}.
     * @throws IOException If an IO exception occurs while writing the file.
     */
    File toJar(File file, Manifest manifest) throws IOException;

    /**
     * A builder for defining a dynamic type. Implementations of such builders are fully immutable and return
     * modified instances.
     *
     * @param <T> The most specific known loaded type that is implemented by the created dynamic type, usually the
     *            type itself, an interface or the direct super class.
     */
    interface Builder<T> extends TypeVariableDefinable<T> {

        /**
         * Defines a class file format version for this builder for which the dynamic types should be created.
         *
         * @param classFileVersion The class format version for the dynamic type to implement.
         * @return A builder that writes its classes in a given class format version.
         */
        Builder<T> classFileVersion(ClassFileVersion classFileVersion);

        /**
         * Adds the given interfaces to be implemented by the created type.
         *
         * @param interfaceType The interfaces to implement.
         * @return A builder which will create a dynamic type that implements the given interfaces.
         */
        MethodInterception.Optional<T> implement(Type... interfaceType);

        /**
         * Adds the given interfaces to be implemented by the created type.
         *
         * @param interfaceTypes The interfaces to implement.
         * @return A builder which will create a dynamic type that implements the given interfaces.
         */
        MethodInterception.Optional<T> implement(Iterable<? extends Type> interfaceTypes);

        /**
         * Adds the given interfaces to be implemented by the created type.
         *
         * @param interfaceType A description of the interfaces to implement.
         * @return A builder which will create a dynamic type that implements the given interfaces.
         */
        MethodInterception.Optional<T> implement(GenericTypeDescription... interfaceType);

        /**
         * Adds the given interfaces to be implemented by the created type.
         *
         * @param interfaceTypes A description of the interfaces to implement.
         * @return A builder which will create a dynamic type that implements the given interfaces.
         */
        MethodInterception.Optional<T> implement(Collection<? extends GenericTypeDescription> interfaceTypes);

        /**
         * Names the currently created dynamic type by a fixed name.
         *
         * @param name A fully qualified name to give to the created dynamic type.
         * @return A builder that will name its dynamic type by the given name.
         */
        Builder<T> name(String name);

        /**
         * Names the currently created dynamic type by the given naming strategy.
         *
         * @param namingStrategy The naming strategy to apply.
         * @return A builder that creates a type by applying the given naming strategy.
         */
        Builder<T> name(NamingStrategy namingStrategy);

        /**
         * Defines a naming strategy for naming auxiliary types.
         *
         * @param namingStrategy The naming strategy to use.
         * @return This builder where the auxiliary naming strategy was set to be used.
         */
        Builder<T> name(AuxiliaryType.NamingStrategy namingStrategy);

        /**
         * Defines modifiers for the created dynamic type.
         *
         * @param modifier A collection of modifiers to be reflected by the created dynamic type.
         * @return A builder that will create a dynamic type that reflects the given modifiers.
         */
        Builder<T> modifiers(ModifierContributor.ForType... modifier);

        /**
         * Defines modifiers for the created dynamic type.
         *
         * @param modifiers The modifiers to be reflected by the created dynamic type.
         * @return A builder that will create a dynamic type that reflects the given modifiers.
         */
        Builder<T> modifiers(int modifiers);

        /**
         * Defines a matcher for methods that will be ignored for any interception attempt. Any methods
         * that were directly declared on an instrumented type will never be ignored, i.e. ignored methods
         * only represent a filter for methods that are declared in super types that should never be overriden.
         *
         * @param ignoredMethods A method matcher characterizing the methods to be ignored.
         * @return A builder that will always ignore the methods matched by the given method matcher.
         */
        Builder<T> ignoreMethods(ElementMatcher<? super MethodDescription> ignoredMethods);

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
         * <p>&nbsp;</p>
         * Note: The annotations will not be visible to {@link Implementation}s.
         *
         * @param annotation The annotations to be added to the currently constructed type.
         * @return A builder that will add the given annotation to the created type.
         */
        Builder<T> annotateType(Annotation... annotation);

        /**
         * Adds annotations to the currently constructed type.
         * <p>&nbsp;</p>
         * Note: The annotations will not be visible to {@link Implementation}s.
         *
         * @param annotations The annotations to be added to the currently constructed type.
         * @return A builder that will add the given annotation to the created type.
         */
        Builder<T> annotateType(Iterable<? extends Annotation> annotations);

        /**
         * Adds annotations to the currently constructed type.
         * <p>&nbsp;</p>
         * Note: The annotations will not be visible to {@link Implementation}s.
         *
         * @param annotation The annotations to be added to the currently constructed type.
         * @return A builder that will add the given annotation to the created type.
         */
        Builder<T> annotateType(AnnotationDescription... annotation);

        /**
         * Adds annotations to the currently constructed type.
         * <p>&nbsp;</p>
         * Note: The annotations will not be visible to {@link Implementation}s.
         *
         * @param annotations The annotations to be added to the currently constructed type.
         * @return A builder that will add the given annotation to the created type.
         */
        Builder<T> annotateType(Collection<? extends AnnotationDescription> annotations);

        /**
         * Adds an additional ASM {@link org.objectweb.asm.ClassVisitor} to this builder which will be applied in
         * the construction process of this dynamic type.
         *
         * @param classVisitorWrapper The wrapper delegate for the ASM class visitor.
         * @return A builder that will apply the given ASM class visitor.
         */
        Builder<T> classVisitor(ClassVisitorWrapper classVisitorWrapper);

        /**
         * Defines a bridge method resolver factory to be applied to this type creation. A bridge method resolver is
         * responsible for determining the target method that is invoked by a bridge method. This way, a super method
         * invocation is resolved by invoking the actual super method instead of the bridge method which would in turn
         * resolve the actual method virtually.
         *
         * @param bridgeMethodResolverFactory The bridge method resolver factory that is to be used.
         * @return A builder that will apply the given bridge method resolver factory.
         */
        Builder<T> bridgeMethodResolverFactory(BridgeMethodResolver.Factory bridgeMethodResolverFactory);

        /**
         * Defines the use of a specific factory for a {@link MethodLookupEngine}.
         *
         * @param methodLookupEngineFactory The factory to be used.
         * @return A builder that applies the given method lookup engine factory.
         */
        Builder<T> methodLookupEngine(MethodLookupEngine.Factory methodLookupEngineFactory);

        /**
         * Defines a new field for this type.
         *
         * @param name      The name of the method.
         * @param fieldType The type of this field where the current type can be represented by
         *                  {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifier  The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        FieldValueTarget<T> defineField(String name, Type fieldType, ModifierContributor.ForField... modifier);

        /**
         * Defines a new field for this type.
         *
         * @param name                 The name of the method.
         * @param fieldTypeDescription The type of this field where the current type can be represented by
         *                             {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifier             The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        FieldValueTarget<T> defineField(String name, GenericTypeDescription fieldTypeDescription, ModifierContributor.ForField... modifier);

        /**
         * Defines a new field for this type.
         *
         * @param name      The name of the method.
         * @param fieldType The type of this field where the current type can be represented by
         *                  {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifiers The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        FieldValueTarget<T> defineField(String name, Type fieldType, int modifiers);

        /**
         * Defines a new field for this type.
         *
         * @param name                 The name of the method.
         * @param fieldTypeDescription The type of this field where the current type can be represented by
         *                             {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifiers            The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        FieldValueTarget<T> defineField(String name, GenericTypeDescription fieldTypeDescription, int modifiers);

        /**
         * Defines a new field for this type.
         *
         * @param field The field that the generated type should imitate.
         * @return An interception delegate that exclusively matches the new method.
         */
        FieldValueTarget<T> defineField(Field field);

        /**
         * Defines a new field for this type.
         *
         * @param fieldDescription The field that the generated type should imitate.
         * @return An interception delegate that exclusively matches the new method.
         */
        FieldValueTarget<T> defineField(FieldDescription fieldDescription);

        /**
         * Selects a set of methods of this type for instrumentation.
         *
         * @param methodMatcher A matcher describing the methods to be intercepted by this instrumentation.
         * @return An interception delegate for methods matching the given method matcher.
         */
        MethodInterception.ForMatchedMethod<T> method(ElementMatcher<? super MethodDescription> methodMatcher);

        /**
         * Selects a set of constructors of this type for implementation.
         *
         * @param methodMatcher A matcher describing the constructors to be intercepted by this implementation.
         * @return An interception delegate for constructors matching the given method matcher.
         */
        MethodInterception.ForMatchedMethod<T> constructor(ElementMatcher<? super MethodDescription> methodMatcher);

        /**
         * Selects a set of byte code level methods, i.e. methods, constructors and the type initializer of
         * this type for implementation.
         *
         * @param methodMatcher A matcher describing the byte code methods to be intercepted by this implementation.
         * @return An interception delegate for byte code methods matching the given method matcher.
         */
        MethodInterception.ForMatchedMethod<T> invokable(ElementMatcher<? super MethodDescription> methodMatcher);

        /**
         * Selects a set of byte code level methods, i.e. methods, construcors and the type initializer of
         * this type for implementation.
         *
         * @param methodMatcher A latent matcher describing the byte code methods to be intercepted by this implementation.
         * @return An interception delegate for byte code methods matching the given method matcher.
         */
        MethodInterception.ForMatchedMethod<T> invokable(LatentMethodMatcher methodMatcher);

        /**
         * Creates the dynamic type without loading it.
         *
         * @return An unloaded representation of the dynamic type.
         */
        Unloaded<T> make();

        /**
         * Defines an implementation for a method that was added to this instrumentation or a to method selection
         * of existing methods.
         *
         * @param <S> The most specific known loaded type that is implemented by the created dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        interface MethodInterception<S> {

            /**
             * Intercepts the currently selected methods with the provided implementation. If this intercepted method is
             * not yet declared by the current type, it might be added to the currently built type as a result of this
             * interception. If the method is already declared by the current type, its byte code code might be copied
             * into the body of a synthetic method in order to preserve the original code's invokeability.
             *
             * @param implementation The implementation to apply to the currently selected method.
             * @return A builder which will intercept the currently selected methods by the given implementation.
             */
            Builder<S> intercept(Implementation implementation);

            /**
             * Implements the currently selected methods as {@code abstract} methods.
             *
             * @return A builder which will implement the currently selected methods as {@code abstract} methods.
             */
            Builder<S> withoutCode();

            /**
             * Defines a default annotation value to set for any matched method.
             *
             * @param value The value that the annotation property should set as a default.
             * @param type  The type of the annotation property.
             * @return A builder which defines the given default value for all matched methods.
             */
            Builder<S> withDefaultValue(Object value, Class<?> type);

            /**
             * Defines a default annotation value to set for any matched method. The value is to be represented in a wrapper format,
             * {@code enum} values should be handed as {@link net.bytebuddy.description.enumeration.EnumerationDescription}
             * instances, annotations as {@link AnnotationDescription} instances and
             * {@link Class} values as {@link TypeDescription} instances. Other values are handed in their raw format or as their wrapper types.
             *
             * @param value A non-loaded value that the annotation property should set as a default.
             * @return A builder which defines the given default value for all matched methods.
             */
            Builder<S> withDefaultValue(Object value);

            /**
             * Defines an attribute appender factory to be applied onto the currently selected methods.
             *
             * @param attributeAppenderFactory The attribute appender factory to apply onto the currently selected
             *                                 methods.
             * @return A builder where the given attribute appender factory will be applied to the currently selected methods.
             */
            MethodInterception<S> attribute(MethodAttributeAppender.Factory attributeAppenderFactory);

            /**
             * Defines annotations to be added to the currently selected method.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to
             * {@link Implementation}s.
             *
             * @param annotation The annotations to add to the currently selected methods.
             * @return A builder where the given annotation will be added to the currently selected methods.
             */
            MethodInterception<S> annotateMethod(Annotation... annotation);

            /**
             * Defines annotations to be added to the currently selected method.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to
             * {@link Implementation}s.
             *
             * @param annotations The annotations to add to the currently selected methods.
             * @return A builder where the given annotation will be added to the currently selected methods.
             */
            MethodInterception<S> annotateMethod(Iterable<? extends Annotation> annotations);

            /**
             * Defines annotations to be added to the currently selected method.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to
             * {@link Implementation}s.
             *
             * @param annotation The annotations to add to the currently selected methods.
             * @return A builder where the given annotation will be added to the currently selected methods.
             */
            MethodInterception<S> annotateMethod(AnnotationDescription... annotation);

            /**
             * Defines annotations to be added to the currently selected method.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to
             * {@link Implementation}s.
             *
             * @param annotations The annotations to add to the currently selected methods.
             * @return A builder where the given annotation will be added to the currently selected methods.
             */
            MethodInterception<S> annotateMethod(Collection<? extends AnnotationDescription> annotations);

            /**
             * Defines an implementation for a method that was added to this instrumentation and allows to include
             * exception declarations for the newly defined method.
             *
             * @param <U> The most specific known loaded type that is implemented by the created dynamic type, usually the
             *            type itself, an interface or the direct super class.
             */
            interface ExceptionDeclarable<U> extends MethodInterception<U> {

                /**
                 * Defines a number of {@link java.lang.Throwable} types to be include in the exception declaration.
                 *
                 * @param exceptionType The types that should be declared to be thrown by the selected method.
                 * @return A target for instrumenting the defined method where the method will declare the given exception
                 * types.
                 */
                ExceptionDeclarable<U> throwing(Type... exceptionType);

                /**
                 * Defines a number of {@link java.lang.Throwable} types to be include in the exception declaration.
                 *
                 * @param exceptionTypes The types that should be declared to be thrown by the selected method.
                 * @return A target for instrumenting the defined method where the method will declare the given exception
                 * types.
                 */
                ExceptionDeclarable<U> throwing(Iterable<? extends Type> exceptionTypes);

                /**
                 * Defines a number of {@link java.lang.Throwable} types to be include in the exception declaration.
                 *
                 * @param exceptionType Descriptions of the types that should be declared to be thrown by the selected method.
                 * @return A target for instrumenting the defined method where the method will declare the given exception
                 * types.
                 */
                ExceptionDeclarable<U> throwing(GenericTypeDescription... exceptionType);

                /**
                 * Defines a number of {@link java.lang.Throwable} types to be include in the exception declaration.
                 *
                 * @param exceptionTypes Descriptions of the types that should be declared to be thrown by the selected method.
                 * @return A target for instrumenting the defined method where the method will declare the given exception
                 * types.
                 */
                ExceptionDeclarable<U> throwing(Collection<? extends GenericTypeDescription> exceptionTypes);

                abstract class AbstractBase<S> extends MethodInterception.AbstractBase<S> implements ExceptionDeclarable<S> {

                    @Override
                    public ExceptionDeclarable<S> throwing(GenericTypeDescription... exceptionType) {
                        return throwing(Arrays.asList(exceptionType));
                    }

                    @Override
                    public ExceptionDeclarable<S> throwing(Type... exceptionType) {
                        return throwing(Arrays.asList(exceptionType));
                    }

                    @Override
                    public ExceptionDeclarable<S> throwing(Iterable<? extends Type> exceptionTypes) {
                        return throwing(new GenericTypeList.ForLoadedType(nonNull(toList(exceptionTypes))));
                    }
                }
            }

            interface ForMatchedMethod<S> extends MethodInterception<S> {

                @Override
                ForMatchedMethod<S> attribute(MethodAttributeAppender.Factory attributeAppenderFactory);

                @Override
                ForMatchedMethod<S> annotateMethod(Annotation... annotation);

                @Override
                ForMatchedMethod<S> annotateMethod(Iterable<? extends Annotation> annotations);

                @Override
                ForMatchedMethod<S> annotateMethod(AnnotationDescription... annotation);

                @Override
                ForMatchedMethod<S> annotateMethod(Collection<? extends AnnotationDescription> annotations);

                ForMatchedMethod<S> annotateParameter(int parameterIndex, Annotation... annotation);

                ForMatchedMethod<S> annotateParameter(int parameterIndex, Iterable<? extends Annotation> annotations);

                ForMatchedMethod<S> annotateParameter(int parameterIndex, AnnotationDescription... annotation);

                ForMatchedMethod<S> annotateParameter(int parameterIndex, Collection<? extends AnnotationDescription> annotations);

                abstract class AbstractBase<U> extends MethodInterception.AbstractBase<U> implements ForMatchedMethod<U> {

                    @Override
                    public ForMatchedMethod<U> annotateMethod(Annotation... annotation) {
                        return annotateMethod(Arrays.asList(annotation));
                    }

                    @Override
                    public ForMatchedMethod<U> annotateMethod(Iterable<? extends Annotation> annotations) {
                        return annotateMethod(new AnnotationList.ForLoadedAnnotation(nonNull(toList(annotations))));
                    }

                    @Override
                    public ForMatchedMethod<U> annotateMethod(AnnotationDescription... annotation) {
                        return annotateMethod(Arrays.asList(annotation));
                    }

                    @Override
                    public ForMatchedMethod<U> annotateParameter(int parameterIndex, AnnotationDescription... annotation) {
                        return annotateParameter(parameterIndex, Arrays.asList(annotation));
                    }

                    @Override
                    public ForMatchedMethod<U> annotateParameter(int parameterIndex, Iterable<? extends Annotation> annotations) {
                        return annotateParameter(parameterIndex, new AnnotationList.ForLoadedAnnotation(toList(nonNull(annotations))));
                    }

                    @Override
                    public ForMatchedMethod<U> annotateParameter(int parameterIndex, Annotation... annotation) {
                        return annotateParameter(parameterIndex, Arrays.asList(annotation));
                    }
                }
            }

            /**
             * An optional matched method interception allows to define an interception without requiring the definition
             * of an implementation.
             *
             * @param <S> The most specific known loaded type that is implemented by the created dynamic type, usually the
             *            type itself, an interface or the direct super class.
             */
            interface Optional<S> extends ForMatchedMethod<S>, Builder<S> {

                abstract class AbstractBase<U> extends Builder.AbstractBase.Delegator<U> implements Optional<U> {

                    protected abstract Builder<U> materialize(MethodRegistry.Handler handler, Object defaultValue);

                    @Override
                    public Builder<U> intercept(Implementation implementation) {
                        return materialize(new MethodRegistry.Handler.ForImplementation(nonNull(implementation)), MethodDescription.NO_DEFAULT_VALUE);
                    }

                    @Override
                    public Builder<U> withoutCode() {
                        return materialize(MethodRegistry.Handler.ForAbstractMethod.INSTANCE, MethodDescription.NO_DEFAULT_VALUE);
                    }

                    @Override
                    public Builder<U> withDefaultValue(Object value, Class<?> type) {
                        return withDefaultValue(AnnotationDescription.ForLoadedAnnotation.describe(value, new TypeDescription.ForLoadedType(nonNull(type))));
                    }

                    @Override
                    public Builder<U> withDefaultValue(Object value) {
                        return materialize(MethodRegistry.Handler.ForAnnotationValue.of(value), value);
                    }

                    @Override
                    public MethodInterception.ForMatchedMethod<U> annotateMethod(Annotation... annotation) {
                        return annotateMethod(Arrays.asList(annotation));
                    }

                    @Override
                    public MethodInterception.ForMatchedMethod<U> annotateMethod(Iterable<? extends Annotation> annotations) {
                        return annotateMethod(new AnnotationList.ForLoadedAnnotation(nonNull(toList(annotations))));
                    }

                    @Override
                    public MethodInterception.ForMatchedMethod<U> annotateMethod(AnnotationDescription... annotation) {
                        return annotateMethod(Arrays.asList(annotation));
                    }

                    @Override
                    public ForMatchedMethod<U> annotateParameter(int parameterIndex, AnnotationDescription... annotation) {
                        return annotateParameter(parameterIndex, Arrays.asList(annotation));
                    }

                    @Override
                    public ForMatchedMethod<U> annotateParameter(int parameterIndex, Annotation... annotation) {
                        return annotateParameter(parameterIndex, Arrays.asList(annotation));
                    }

                    @Override
                    public ForMatchedMethod<U> annotateParameter(int parameterIndex, Iterable<? extends Annotation> annotations) {
                        return annotateParameter(parameterIndex, new AnnotationList.ForLoadedAnnotation(toList(nonNull(annotations))));
                    }
                }
            }

            interface ParameterDefinable<S> extends ExceptionDeclarable<S> {

                AnnotationDeclarable<S> withParameter(Type parameterType);

                AnnotationDeclarable<S> withParameter(GenericTypeDescription parameterType);

                ParameterDefinable<S> withParameter(Type... parameterType);

                ParameterDefinable<S> withParameter(Iterable<? extends Type> parameterTypes);

                ParameterDefinable<S> withParameter(GenericTypeDescription... parameterType);

                ParameterDefinable<S> withParameter(Collection<? extends GenericTypeDescription> parameterTypes);

                interface AnnotationDeclarable<U> extends ParameterDefinable<U> {

                    AnnotationDeclarable<U> annotateParameter(Annotation... annotation);

                    AnnotationDeclarable<U> annotateParameter(Iterable<? extends Annotation> annotations);

                    AnnotationDeclarable<U> annotateParameter(AnnotationDescription... annotationDescription);

                    AnnotationDeclarable<U> annotateParameter(Collection<? extends AnnotationDescription> annotationDescriptions);

                    abstract class AbstractBase<V> extends ParameterDefinable.AbstractBase<V> implements AnnotationDeclarable<V> {

                        @Override
                        public AnnotationDeclarable<V> annotateParameter(AnnotationDescription... annotationDescription) {
                            return annotateParameter(Arrays.asList(annotationDescription));
                        }

                        @Override
                        public AnnotationDeclarable<V> annotateParameter(Iterable<? extends Annotation> annotations) {
                            return annotateParameter(new AnnotationList.ForLoadedAnnotation(toList(nonNull(annotations))));
                        }

                        @Override
                        public AnnotationDeclarable<V> annotateParameter(Annotation... annotation) {
                            return annotateParameter(Arrays.asList(annotation));
                        }
                    }
                }

                interface WithMetaData<U> extends ExceptionDeclarable<U> {

                    AnnotationDeclarable<U> withParameter(Type parameterType, String name, ModifierContributor.ForParameter... modifier);

                    AnnotationDeclarable<U> withParameter(GenericTypeDescription parameterType, String name, ModifierContributor.ForParameter... modifier);

                    AnnotationDeclarable<U> withParameter(Type parameterType, String name, int modifiers);

                    AnnotationDeclarable<U> withParameter(GenericTypeDescription parameterType, String name, int modifiers);

                    interface AnnotationDeclarable<W> extends ParameterDefinable.WithMetaData<W> {

                        AnnotationDeclarable<W> annotateParameter(Annotation... annotation);

                        AnnotationDeclarable<W> annotateParameter(Iterable<? extends Annotation> annotations);

                        AnnotationDeclarable<W> annotateParameter(AnnotationDescription... annotationDescription);

                        AnnotationDeclarable<W> annotateParameter(Collection<? extends AnnotationDescription> annotationDescriptions);

                        abstract class AbstractBase<X> extends WithMetaData.AbstractBase<X> implements AnnotationDeclarable<X> {

                            @Override
                            public AnnotationDeclarable<X> annotateParameter(Annotation... annotation) {
                                return annotateParameter(Arrays.asList(annotation));
                            }

                            @Override
                            public AnnotationDeclarable<X> annotateParameter(Iterable<? extends Annotation> annotations) {
                                return annotateParameter(new AnnotationList.ForLoadedAnnotation(toList(nonNull(annotations))));
                            }

                            @Override
                            public AnnotationDeclarable<X> annotateParameter(AnnotationDescription... annotationDescription) {
                                return annotateParameter(Arrays.asList(annotationDescription));
                            }
                        }
                    }

                    abstract class AbstractBase<V> extends ExceptionDeclarable.AbstractBase<V> implements WithMetaData<V> {

                        @Override
                        public AnnotationDeclarable<V> withParameter(Type parameterType, String name, ModifierContributor.ForParameter... modifier) {
                            return withParameter(GenericTypeDescription.Sort.describe(parameterType), name, modifier);
                        }

                        @Override
                        public AnnotationDeclarable<V> withParameter(GenericTypeDescription parameterType,
                                                                     String name,
                                                                     ModifierContributor.ForParameter... modifier) {
                            return withParameter(parameterType, name, resolveModifierContributors(PARAMETER_MODIFIER_MASK, modifier));
                        }

                        @Override
                        public AnnotationDeclarable<V> withParameter(Type parameterType, String name, int modifiers) {
                            return withParameter(GenericTypeDescription.Sort.describe(parameterType), name, modifiers);
                        }
                    }
                }

                interface Binary<U> extends ParameterDefinable<U>, WithMetaData<U> {

                    abstract class AbstractBase<V> extends ParameterDefinable.AbstractBase<V> implements Binary<V> {

                        @Override
                        public WithMetaData.AnnotationDeclarable<V> withParameter(Type parameterType, String name, ModifierContributor.ForParameter... modifier) {
                            return withParameter(GenericTypeDescription.Sort.describe(parameterType), name, modifier);
                        }

                        @Override
                        public WithMetaData.AnnotationDeclarable<V> withParameter(GenericTypeDescription parameterType,
                                                                                  String name,
                                                                                  ModifierContributor.ForParameter... modifier) {
                            return withParameter(parameterType, name, resolveModifierContributors(PARAMETER_MODIFIER_MASK, modifier));
                        }

                        @Override
                        public WithMetaData.AnnotationDeclarable<V> withParameter(Type parameterType, String name, int modifiers) {
                            return withParameter(GenericTypeDescription.Sort.describe(parameterType), name, modifiers);
                        }
                    }
                }

                abstract class AbstractBase<U> extends ExceptionDeclarable.AbstractBase<U> implements ParameterDefinable<U> {

                    @Override
                    public AnnotationDeclarable<U> withParameter(Type parameterType) {
                        return withParameter(GenericTypeDescription.Sort.describe(parameterType));
                    }

                    @Override
                    public ParameterDefinable<U> withParameter(Type... parameterType) {
                        return withParameter(Arrays.asList(parameterType));
                    }

                    @Override
                    public ParameterDefinable<U> withParameter(Iterable<? extends Type> parameterTypes) {
                        return withParameter(new GenericTypeList.ForLoadedType(nonNull(toList(parameterTypes))));
                    }

                    @Override
                    public ParameterDefinable<U> withParameter(GenericTypeDescription... parameterType) {
                        return withParameter(Arrays.asList(parameterType));
                    }
                }
            }

            abstract class AbstractBase<U> implements MethodInterception<U> {

                protected abstract Builder<U> materialize(MethodRegistry.Handler handler, Object defaultValue);

                @Override
                public Builder<U> intercept(Implementation implementation) {
                    return materialize(new MethodRegistry.Handler.ForImplementation(nonNull(implementation)), MethodDescription.NO_DEFAULT_VALUE);
                }

                @Override
                public Builder<U> withoutCode() {
                    return materialize(MethodRegistry.Handler.ForAbstractMethod.INSTANCE, MethodDescription.NO_DEFAULT_VALUE);
                }

                @Override
                public Builder<U> withDefaultValue(Object value, Class<?> type) {
                    return withDefaultValue(AnnotationDescription.ForLoadedAnnotation.describe(value, new TypeDescription.ForLoadedType(nonNull(type))));
                }

                @Override
                public Builder<U> withDefaultValue(Object value) {
                    return materialize(MethodRegistry.Handler.ForAnnotationValue.of(value), value);
                }

                @Override
                public MethodInterception<U> annotateMethod(Annotation... annotation) {
                    return annotateMethod(Arrays.asList(annotation));
                }

                @Override
                public MethodInterception<U> annotateMethod(Iterable<? extends Annotation> annotations) {
                    return annotateMethod(new AnnotationList.ForLoadedAnnotation(nonNull(toList(annotations))));
                }

                @Override
                public MethodInterception<U> annotateMethod(AnnotationDescription... annotation) {
                    return annotateMethod(Arrays.asList(annotation));
                }
            }
        }

        /**
         * A builder to which a field was just added such that default values can be defined for the field. Default
         * values must only be defined for {@code static} fields of a primitive type or of the {@link java.lang.String}
         * type.
         *
         * @param <S> The most specific known type of the dynamic type, usually the type itself, an interface or the
         *            direct super class.
         */
        interface FieldValueTarget<S> extends Builder<S> {

            /**
             * Defines a {@code boolean} value to become the optional default value for the recently defined
             * {@code static} field. Defining such a boolean default value is only legal for fields that are
             * represented as an integer within the Java virtual machine. These types are the {@code boolean} type,
             * the {@code byte} type, the {@code short} type, the {@code char} type and the {@code int} type.
             *
             * @param value The value to be defined as a default value for the recently defined field.
             * @return A field annotation target for the currently defined field.
             */
            Builder<S> value(boolean value);

            /**
             * Defines an {@code int} value to be become the optional default value for the recently defined
             * {@code static} field. Defining such an integer default value is only legal for fields that are
             * represented as an integer within the Java virtual machine. These types are the {@code boolean} type,
             * the {@code byte} type, the {@code short} type, the {@code char} type and the {@code int} type. By
             * extension, integer types can also be defined for {@code long} types and are automatically converted.
             *
             * @param value The value to be defined as a default value for the recently defined field.
             * @return A field annotation target for the currently defined field.
             */
            Builder<S> value(int value);

            /**
             * Defined a default value for a {@code long}-typed {@code static} field. This is only legal if the
             * defined field is also of type {@code long}.
             *
             * @param value The value to be defined as a default value for the recently defined field.
             * @return A field annotation target for the currently defined field.
             */
            Builder<S> value(long value);

            /**
             * Defined a default value for a {@code float}-typed {@code static} field. This is only legal if the
             * defined field is also of type {@code float}.
             *
             * @param value The value to be defined as a default value for the recently defined field.
             * @return A field annotation target for the currently defined field.
             */
            Builder<S> value(float value);

            /**
             * Defined a default value for a {@code double}-typed {@code static} field. This is only legal if the
             * defined field is also of type {@code double}.
             *
             * @param value The value to be defined as a default value for the recently defined field.
             * @return A field annotation target for the currently defined field.
             */
            Builder<S> value(double value);

            /**
             * Defined a default value for a {@link java.lang.String}-typed {@code static} field. This is only legal if
             * the defined field is also of type {@link java.lang.String}. The string must not be {@code null}.
             *
             * @param value The value to be defined as a default value for the recently defined field.
             * @return A field annotation target for the currently defined field.
             */
            Builder<S> value(String value);

            abstract class AbstractBase<S> extends Builder.AbstractBase.Delegator<S> implements FieldValueTarget<S> {

                @Override
                public Builder<S> value(boolean value) {
                    return value(value ? 1 : 0, Boolean.class);
                }

                @Override
                public Builder<S> value(long value) {
                    return value(value, Long.class);
                }

                @Override
                public Builder<S> value(float value) {
                    return value(value, Float.class);
                }

                @Override
                public Builder<S> value(double value) {
                    return value(value, Double.class);
                }

                @Override
                public Builder<S> value(String value) {
                    return value(value, String.class);
                }

                protected abstract Builder<S> value(Object value, Class<?> expectedType);
            }

            /**
             * A validator for assuring that a given value can be represented by a given primitive type.
             */
            enum NumericRangeValidator {

                /**
                 * A validator for {@code boolean} values.
                 */
                BOOLEAN(0, 1),

                /**
                 * A validator for {@code byte} values.
                 */
                BYTE(Byte.MIN_VALUE, Byte.MAX_VALUE),

                /**
                 * A validator for {@code short} values.
                 */
                SHORT(Short.MIN_VALUE, Short.MAX_VALUE),

                /**
                 * A validator for {@code char} values.
                 */
                CHARACTER(Character.MIN_VALUE, Character.MAX_VALUE),

                /**
                 * A validator for {@code int} values.
                 */
                INTEGER(Integer.MIN_VALUE, Integer.MAX_VALUE),

                /**
                 * A validator for {@code long} values.
                 */
                LONG(Integer.MIN_VALUE, Integer.MAX_VALUE) {
                    @Override
                    public Object validate(int value) {
                        return (long) value;
                    }
                };

                /**
                 * The minimum and maximum values for an {@code int} value for the represented primitive value.
                 */
                private final int minimum, maximum;

                /**
                 * Creates a new numeric range validator.
                 *
                 * @param minimum The minimum {@code int} value that can be represented by this primitive type.
                 * @param maximum The maximum {@code int} value that can be represented by this primitive type.
                 */
                NumericRangeValidator(int minimum, int maximum) {
                    this.minimum = minimum;
                    this.maximum = maximum;
                }

                /**
                 * Identifies the correct validator for a given type description.
                 *
                 * @param typeDescription The type of a field for which a default value should be validated.
                 * @return The corresponding numeric range validator.
                 */
                public static NumericRangeValidator of(TypeDescription typeDescription) {
                    if (typeDescription.represents(boolean.class)) {
                        return BOOLEAN;
                    } else if (typeDescription.represents(byte.class)) {
                        return BYTE;
                    } else if (typeDescription.represents(short.class)) {
                        return SHORT;
                    } else if (typeDescription.represents(char.class)) {
                        return CHARACTER;
                    } else if (typeDescription.represents(int.class)) {
                        return INTEGER;
                    } else if (typeDescription.represents(long.class)) {
                        return LONG;
                    } else {
                        throw new IllegalStateException("A field of type " + typeDescription + " does not permit an integer-typed default value");
                    }
                }

                /**
                 * Validates and wraps a given {@code int} value for the represented numeric range.
                 *
                 * @param value The value to be validated for a given numeric range.
                 * @return The wrapped value after validation.
                 */
                public Object validate(int value) {
                    if (value < minimum || value > maximum) {
                        throw new IllegalArgumentException(value + " overflows for " + this);
                    }
                    return value;
                }

                @Override
                public String toString() {
                    return "DynamicType.Builder.FieldValueTarget.NumericRangeValidator." + name();
                }
            }

            interface AnnotationDeclarable<S> extends FieldValueTarget<S> {

                /**
                 * Defines an attribute appender factory to be applied onto the currently selected field.
                 *
                 * @param attributeAppenderFactory The attribute appender factory to apply onto the currently selected
                 *                                 field.
                 * @return A builder where the given attribute appender factory will be applied to the currently selected field.
                 */
                AnnotationDeclarable<S> attribute(FieldAttributeAppender.Factory attributeAppenderFactory);

                /**
                 * Defines annotations to be added to the currently selected field.
                 * <p>&nbsp;</p>
                 * Note: The annotations will not be visible to {@link Implementation}s.
                 *
                 * @param annotation The annotations to add to the currently selected field.
                 * @return A builder where the given annotation will be added to the currently selected field.
                 */
                AnnotationDeclarable<S> annotateField(Annotation... annotation);

                /**
                 * Defines annotations to be added to the currently selected field.
                 * <p>&nbsp;</p>
                 * Note: The annotations will not be visible to {@link Implementation}s.
                 *
                 * @param annotations The annotations to add to the currently selected field.
                 * @return A builder where the given annotation will be added to the currently selected field.
                 */
                AnnotationDeclarable<S> annotateField(Iterable<? extends Annotation> annotations);

                /**
                 * Defines annotations to be added to the currently selected field.
                 * <p>&nbsp;</p>
                 * Note: The annotations will not be visible to {@link Implementation}s.
                 *
                 * @param annotation The annotations to add to the currently selected field.
                 * @return A builder where the given annotation will be added to the currently selected field.
                 */
                AnnotationDeclarable<S> annotateField(AnnotationDescription... annotation);

                /**
                 * Defines annotations to be added to the currently selected field.
                 * <p>&nbsp;</p>
                 * Note: The annotations will not be visible to {@link Implementation}s.
                 *
                 * @param annotations The annotations to add to the currently selected field.
                 * @return A builder where the given annotation will be added to the currently selected field.
                 */
                AnnotationDeclarable<S> annotateField(Collection<? extends AnnotationDescription> annotations);

                abstract class AbstractBase<U> extends FieldValueTarget.AbstractBase<U> implements AnnotationDeclarable<U> {

                    @Override
                    public AnnotationDeclarable<U> annotateField(Annotation... annotation) {
                        return annotateField(Arrays.asList(annotation));
                    }

                    @Override
                    public AnnotationDeclarable<U> annotateField(Iterable<? extends Annotation> annotations) {
                        return annotateField(new AnnotationList.ForLoadedAnnotation(toList(nonNull(annotations))));
                    }

                    @Override
                    public AnnotationDeclarable<U> annotateField(AnnotationDescription... annotation) {
                        return annotateField(Arrays.asList(annotation));
                    }
                }
            }
        }

        /**
         * An abstract base implementation for a dynamic type builder. For representing the built type, the
         * {@link net.bytebuddy.dynamic.TargetType} class can be used as a placeholder.
         *
         * @param <S> The most specific known loaded type that is implemented by the created dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        abstract class AbstractBase<S> extends TypeVariableDefinable.AbstractBase<S> implements Builder<S> {

            private final ClassFileVersion classFileVersion;

            private final int modifiers;

            private final List<GenericTypeDescription> typeVariables;

            private final List<GenericTypeDescription> interfaceTypes;

            private final NamingStrategy namingStrategy;

            private final AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

            private final ElementMatcher<? super MethodDescription> ignoredMethods;

            private final TypeAttributeAppender typeAttributeAppender;

            private final List<AnnotationDescription> annotationDescriptions;

            private final ClassVisitorWrapper classVisitorWrapper;

            private final BridgeMethodResolver.Factory bridgeMethodResolverFactory;

            private final MethodLookupEngine.Factory methodLookupEngineFactory;

            private final FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory;

            private final MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory;

            private final FieldRegistry fieldRegistry;

            private final MethodRegistry methodRegistry;

            private final List<FieldDescription.Token> fieldTokens;

            private final List<MethodDescription.Token> methodTokens;

            protected AbstractBase(ClassFileVersion classFileVersion,
                                   int modifiers,
                                   List<GenericTypeDescription> typeVariables,
                                   List<GenericTypeDescription> interfaceTypes,
                                   NamingStrategy namingStrategy,
                                   AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                   ElementMatcher<? super MethodDescription> ignoredMethods,
                                   TypeAttributeAppender typeAttributeAppender,
                                   List<AnnotationDescription> annotationDescriptions,
                                   ClassVisitorWrapper classVisitorWrapper,
                                   BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                                   MethodLookupEngine.Factory methodLookupEngineFactory,
                                   FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                   MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                   FieldRegistry fieldRegistry,
                                   MethodRegistry methodRegistry,
                                   List<FieldDescription.Token> fieldTokens,
                                   List<MethodDescription.Token> methodTokens) {
                this.classFileVersion = classFileVersion;
                this.modifiers = modifiers;
                this.typeVariables = typeVariables;
                this.interfaceTypes = interfaceTypes;
                this.namingStrategy = namingStrategy;
                this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
                this.ignoredMethods = ignoredMethods;
                this.typeAttributeAppender = typeAttributeAppender;
                this.annotationDescriptions = annotationDescriptions;
                this.classVisitorWrapper = classVisitorWrapper;
                this.bridgeMethodResolverFactory = bridgeMethodResolverFactory;
                this.methodLookupEngineFactory = methodLookupEngineFactory;
                this.defaultFieldAttributeAppenderFactory = defaultFieldAttributeAppenderFactory;
                this.defaultMethodAttributeAppenderFactory = defaultMethodAttributeAppenderFactory;
                this.fieldRegistry = fieldRegistry;
                this.methodRegistry = methodRegistry;
                this.fieldTokens = fieldTokens;
                this.methodTokens = methodTokens;
            }

            @Override
            public Builder<S> classFileVersion(ClassFileVersion classFileVersion) {
                return null;
            }

            @Override
            public MethodInterception.Optional<S> implement(Type... interfaceType) {
                return implement(Arrays.asList(interfaceType));
            }

            @Override
            public MethodInterception.Optional<S> implement(Iterable<? extends Type> interfaceTypes) {
                return implement(new GenericTypeList.ForLoadedType(nonNull(toList(interfaceTypes))));
            }

            @Override
            public MethodInterception.Optional<S> implement(GenericTypeDescription... interfaceType) {
                return implement(Arrays.asList(interfaceType));
            }

            @Override
            public MethodInterception.Optional<S> implement(Collection<? extends GenericTypeDescription> interfaceTypes) {
                List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(interfaceTypes.size());
                for (GenericTypeDescription interfaceType : interfaceTypes) {
                    typeDescriptions.add(interfaceType.asRawType());
                }
                return new OptionalInterception(new ArrayList<GenericTypeDescription>(interfaceTypes),
                        isDeclaredBy(anyOf(typeDescriptions)),
                        defaultMethodAttributeAppenderFactory);
            }

            @Override
            public Builder<S> name(String name) {
                return name(new NamingStrategy.Fixed(nonNull(name)));
            }

            @Override
            public Builder<S> name(NamingStrategy namingStrategy) {
                return null; // TODO
            }

            @Override
            public Builder<S> name(AuxiliaryType.NamingStrategy namingStrategy) {
                return null; // TODO
            }

            @Override
            public Builder<S> modifiers(ModifierContributor.ForType... modifier) {
                return modifiers(resolveModifierContributors(TYPE_MODIFIER_MASK, modifier));
            }

            @Override
            public Builder<S> modifiers(int modifiers) {
                return null; // TODO
            }

            @Override
            public Builder<S> ignoreMethods(ElementMatcher<? super MethodDescription> ignoredMethods) {
                return null; // TODO
            }

            @Override
            public Builder<S> attribute(TypeAttributeAppender attributeAppender) {
                return null; // TODO
            }

            @Override
            public Builder<S> annotateType(Annotation... annotation) {
                return annotateType(Arrays.asList(annotation));
            }

            @Override
            public Builder<S> annotateType(Iterable<? extends Annotation> annotations) {
                return annotateType(new AnnotationList.ForLoadedAnnotation(nonNull(toList(annotations))));
            }

            @Override
            public Builder<S> annotateType(AnnotationDescription... annotation) {
                return annotateType(Arrays.asList(annotation));
            }

            @Override
            public Builder<S> annotateType(Collection<? extends AnnotationDescription> annotations) {
                return null; // TODO
            }

            @Override
            public Builder<S> classVisitor(ClassVisitorWrapper classVisitorWrapper) {
                return null; // TODO
            }

            @Override
            public Builder<S> bridgeMethodResolverFactory(BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
                return null; // TODO
            }

            @Override
            public Builder<S> methodLookupEngine(MethodLookupEngine.Factory methodLookupEngineFactory) {
                return null; // TODO
            }

            @Override
            public FieldValueTarget.AnnotationDeclarable<S> defineField(String name, Type fieldType, ModifierContributor.ForField... modifier) {
                return defineField(name, GenericTypeDescription.Sort.describe(fieldType), modifier);
            }

            @Override
            public FieldValueTarget.AnnotationDeclarable<S> defineField(String name, GenericTypeDescription fieldTypeDescription, ModifierContributor.ForField... modifier) {
                return defineField(name, fieldTypeDescription, resolveModifierContributors(FIELD_MODIFIER_MASK, modifier));
            }

            @Override
            public FieldValueTarget.AnnotationDeclarable<S> defineField(String name, Type fieldType, int modifiers) {
                return defineField(name, GenericTypeDescription.Sort.describe(fieldType), modifiers);
            }

            @Override
            public FieldValueTarget.AnnotationDeclarable<S> defineField(String name, GenericTypeDescription fieldTypeDescription, int modifiers) {
                return new FieldDefinition(new FieldDescription.Token(isValidIdentifier(name),
                        isLegalModifiers(FIELD_MODIFIER_MASK, modifiers),
                        isActualType(fieldTypeDescription),
                        Collections.<AnnotationDescription>emptyList()), defaultFieldAttributeAppenderFactory);
            }

            @Override
            public FieldValueTarget.AnnotationDeclarable<S> defineField(Field field) {
                return defineField(new FieldDescription.ForLoadedField(nonNull(field)));
            }

            @Override
            public FieldValueTarget.AnnotationDeclarable<S> defineField(FieldDescription fieldDescription) {
                return defineField(fieldDescription.getName(),
                        fieldDescription.getType(),
                        fieldDescription.getModifiers()).annotateField(fieldDescription.getDeclaredAnnotations());
            }

            @Override
            public MethodInterception.ForMatchedMethod<S> method(ElementMatcher<? super MethodDescription> methodMatcher) {
                return invokable(isMethod().and(nonNull(methodMatcher)));
            }

            @Override
            public MethodInterception.ForMatchedMethod<S> constructor(ElementMatcher<? super MethodDescription> methodMatcher) {
                return invokable(isConstructor().and(nonNull(methodMatcher)));
            }

            @Override
            public MethodInterception.ForMatchedMethod<S> invokable(ElementMatcher<? super MethodDescription> methodMatcher) {
                return invokable(new LatentMethodMatcher.Resolved(nonNull(methodMatcher)));
            }

            @Override
            public MethodInterception.ForMatchedMethod<S> invokable(LatentMethodMatcher methodMatcher) {
                return new RequiredInterception(methodMatcher, defaultMethodAttributeAppenderFactory);
            }

            protected abstract DynamicType.Builder<S> materialize(ClassFileVersion classFileVersion,
                                                                  int modifiers,
                                                                  List<GenericTypeDescription> interfaceTypes,
                                                                  NamingStrategy namingStrategy,
                                                                  AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                                  ElementMatcher<? super MethodDescription> ignoredMethods,
                                                                  TypeAttributeAppender typeAttributeAppender,
                                                                  List<AnnotationDescription> annotationDescriptions,
                                                                  ClassVisitorWrapper classVisitorWrapper,
                                                                  BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                                                                  MethodLookupEngine.Factory methodLookupEngineFactory,
                                                                  FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                                                  MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                                                  FieldRegistry fieldRegistry,
                                                                  MethodRegistry methodRegistry,
                                                                  List<FieldDescription.Token> fieldTokens,
                                                                  List<MethodDescription.Token> methodTokens);

            protected abstract static class Delegator<U> implements DynamicType.Builder<U> {

                @Override
                public Builder<U> classFileVersion(ClassFileVersion classFileVersion) {
                    return materialize().classFileVersion(classFileVersion);
                }

                @Override
                public MethodInterception.Optional<U> implement(Type... interfaceType) {
                    return materialize().implement(interfaceType);
                }

                @Override
                public MethodInterception.Optional<U> implement(Iterable<? extends Type> interfaceTypes) {
                    return materialize().implement(interfaceTypes);
                }

                @Override
                public MethodInterception.Optional<U> implement(GenericTypeDescription... interfaceType) {
                    return materialize().implement(interfaceType);
                }

                @Override
                public MethodInterception.Optional<U> implement(Collection<? extends GenericTypeDescription> interfaceTypes) {
                    return materialize().implement(interfaceTypes);
                }

                @Override
                public Builder<U> name(String name) {
                    return materialize().name(name);
                }

                @Override
                public Builder<U> name(NamingStrategy namingStrategy) {
                    return materialize().name(namingStrategy);
                }

                @Override
                public Builder<U> name(AuxiliaryType.NamingStrategy namingStrategy) {
                    return materialize().name(namingStrategy);
                }

                @Override
                public Builder<U> modifiers(ModifierContributor.ForType... modifier) {
                    return materialize().modifiers(modifier);
                }

                @Override
                public Builder<U> modifiers(int modifiers) {
                    return materialize().modifiers(modifiers);
                }

                @Override
                public Builder<U> ignoreMethods(ElementMatcher<? super MethodDescription> ignoredMethods) {
                    return materialize().ignoreMethods(ignoredMethods);
                }

                @Override
                public Builder<U> attribute(TypeAttributeAppender attributeAppender) {
                    return materialize().attribute(attributeAppender);
                }

                @Override
                public Builder<U> annotateType(Annotation... annotation) {
                    return materialize().annotateType(annotation);
                }

                @Override
                public Builder<U> annotateType(Iterable<? extends Annotation> annotations) {
                    return materialize().annotateType(annotations);
                }

                @Override
                public Builder<U> annotateType(AnnotationDescription... annotation) {
                    return materialize().annotateType(annotation);
                }

                @Override
                public Builder<U> annotateType(Collection<? extends AnnotationDescription> annotations) {
                    return materialize().annotateType(annotations);
                }

                @Override
                public Builder<U> classVisitor(ClassVisitorWrapper classVisitorWrapper) {
                    return materialize().classVisitor(classVisitorWrapper);
                }

                @Override
                public Builder<U> bridgeMethodResolverFactory(BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
                    return materialize().bridgeMethodResolverFactory(bridgeMethodResolverFactory);
                }

                @Override
                public Builder<U> methodLookupEngine(MethodLookupEngine.Factory methodLookupEngineFactory) {
                    return materialize().methodLookupEngine(methodLookupEngineFactory);
                }

                @Override
                public FieldValueTarget<U> defineField(String name, Type fieldType, ModifierContributor.ForField... modifier) {
                    return materialize().defineField(name, fieldType, modifier);
                }

                @Override
                public FieldValueTarget<U> defineField(String name, GenericTypeDescription fieldTypeDescription, ModifierContributor.ForField... modifier) {
                    return materialize().defineField(name, fieldTypeDescription, modifier);
                }

                @Override
                public FieldValueTarget<U> defineField(String name, Type fieldType, int modifiers) {
                    return materialize().defineField(name, fieldType, modifiers);
                }

                @Override
                public FieldValueTarget<U> defineField(String name, GenericTypeDescription fieldTypeDescription, int modifiers) {
                    return materialize().defineField(name, fieldTypeDescription, modifiers);
                }

                @Override
                public FieldValueTarget<U> defineField(Field field) {
                    return materialize().defineField(field);
                }

                @Override
                public FieldValueTarget<U> defineField(FieldDescription fieldDescription) {
                    return materialize().defineField(fieldDescription);
                }

                @Override
                public TypeVariableDefinable<U> withTypeVariable(String symbol, Type... bound) {
                    return materialize().withTypeVariable(symbol, bound);
                }

                @Override
                public TypeVariableDefinable<U> withTypeVariable(String symbol, Iterable<? extends Type> bounds) {
                    return materialize().withTypeVariable(symbol, bounds);
                }

                @Override
                public TypeVariableDefinable<U> withTypeVariable(String symbol, GenericTypeDescription... bound) {
                    return materialize().withTypeVariable(symbol, bound);
                }

                @Override
                public TypeVariableDefinable<U> withTypeVariable(String symbol, Collection<? extends GenericTypeDescription> bounds) {
                    return materialize().withTypeVariable(symbol, bounds);
                }

                @Override
                public MethodInterception.ParameterDefinable.Binary<U> defineMethod(String name, Type returnType, ModifierContributor.ForMethod... modifier) {
                    return materialize().defineMethod(name, returnType, modifier);
                }

                @Override
                public MethodInterception.ParameterDefinable.Binary<U> defineMethod(String name, GenericTypeDescription returnType, ModifierContributor.ForMethod... modifier) {
                    return materialize().defineMethod(name, returnType, modifier);
                }

                @Override
                public MethodInterception.ParameterDefinable.Binary<U> defineMethod(String name, Type returnType, int modifiers) {
                    return materialize().defineMethod(name, returnType, modifiers);
                }

                @Override
                public MethodInterception.ParameterDefinable.Binary<U> defineMethod(String name, GenericTypeDescription returnType, int modifiers) {
                    return materialize().defineMethod(name, returnType, modifiers);
                }

                @Override
                public MethodInterception<U> defineMethod(Method method) {
                    return materialize().defineMethod(method);
                }

                @Override
                public MethodInterception.ParameterDefinable.Binary<U> defineConstructor(ModifierContributor.ForMethod... modifier) {
                    return materialize().defineConstructor(modifier);
                }

                @Override
                public MethodInterception.ParameterDefinable.Binary<U> defineConstructor(int modifiers) {
                    return materialize().defineConstructor(modifiers);
                }

                @Override
                public MethodInterception<U> defineConstructor(Constructor<?> constructor) {
                    return materialize().defineConstructor(constructor);
                }

                @Override
                public MethodInterception<U> define(MethodDescription methodDescription) {
                    return materialize().define(methodDescription);
                }

                @Override
                public MethodInterception.ForMatchedMethod<U> method(ElementMatcher<? super MethodDescription> methodMatcher) {
                    return materialize().method(methodMatcher);
                }

                @Override
                public MethodInterception.ForMatchedMethod<U> constructor(ElementMatcher<? super MethodDescription> methodMatcher) {
                    return materialize().constructor(methodMatcher);
                }

                @Override
                public MethodInterception.ForMatchedMethod<U> invokable(ElementMatcher<? super MethodDescription> methodMatcher) {
                    return materialize().invokable(methodMatcher);
                }

                @Override
                public MethodInterception.ForMatchedMethod<U> invokable(LatentMethodMatcher methodMatcher) {
                    return materialize().invokable(methodMatcher);
                }

                @Override
                public Unloaded<U> make() {
                    return materialize().make();
                }

                protected abstract Builder<U> materialize();

                @Override
                public int hashCode() {
                    return materialize().hashCode();
                }

                @Override
                public boolean equals(Object other) {
                    return materialize().equals(other);
                }
            }

            protected class OptionalInterception extends MethodInterception.Optional.AbstractBase<S> {

                private final List<GenericTypeDescription> interfaceTypes;

                private final ElementMatcher<? super MethodDescription> declarationMatcher;

                private final MethodAttributeAppender.Factory attributeAppenderFactory;

                public OptionalInterception(List<GenericTypeDescription> interfaceTypes,
                                            ElementMatcher<? super MethodDescription> declarationMatcher,
                                            MethodAttributeAppender.Factory attributeAppenderFactory) {
                    this.interfaceTypes = interfaceTypes;
                    this.declarationMatcher = declarationMatcher;
                    this.attributeAppenderFactory = attributeAppenderFactory;
                }

                @Override
                protected Builder<S> materialize() {
                    return Builder.AbstractBase.this.materialize(classFileVersion,
                            modifiers,
                            joinUniqueRaw(Builder.AbstractBase.this.interfaceTypes, interfaceTypes),
                            namingStrategy,
                            auxiliaryTypeNamingStrategy,
                            ignoredMethods,
                            typeAttributeAppender,
                            annotationDescriptions,
                            classVisitorWrapper,
                            bridgeMethodResolverFactory,
                            methodLookupEngineFactory, defaultFieldAttributeAppenderFactory,
                            defaultMethodAttributeAppenderFactory,
                            fieldRegistry,
                            methodRegistry,
                            fieldTokens,
                            methodTokens);
                }

                @Override
                protected Builder<S> materialize(MethodRegistry.Handler handler, Object defaultValue) {
                    return Builder.AbstractBase.this.materialize(classFileVersion,
                            modifiers,
                            joinUniqueRaw(Builder.AbstractBase.this.interfaceTypes, interfaceTypes),
                            namingStrategy,
                            auxiliaryTypeNamingStrategy,
                            ignoredMethods,
                            typeAttributeAppender,
                            annotationDescriptions,
                            classVisitorWrapper,
                            bridgeMethodResolverFactory,
                            methodLookupEngineFactory, defaultFieldAttributeAppenderFactory,
                            defaultMethodAttributeAppenderFactory,
                            fieldRegistry,
                            methodRegistry.append(new LatentMethodMatcher.Resolved(declarationMatcher), handler, attributeAppenderFactory),
                            fieldTokens,
                            methodTokens);
                }

                @Override
                public ForMatchedMethod<S> attribute(MethodAttributeAppender.Factory attributeAppenderFactory) {
                    return new OptionalInterception(interfaceTypes,
                            declarationMatcher,
                            new MethodAttributeAppender.Factory.Compound(this.attributeAppenderFactory, nonNull(attributeAppenderFactory)));
                }

                @Override
                public ForMatchedMethod<S> annotateMethod(Collection<? extends AnnotationDescription> annotations) {
                    return attribute(new MethodAttributeAppender.ForAnnotation(toList(nonNull(annotations))));
                }

                @Override
                public ForMatchedMethod<S> annotateParameter(int parameterIndex, Collection<? extends AnnotationDescription> annotations) {
                    return attribute(new MethodAttributeAppender.ForAnnotation(parameterIndex, toList(nonNull(annotations))));

                }
            }

            protected class RequiredInterception extends MethodInterception.ForMatchedMethod.AbstractBase<S> {

                private final LatentMethodMatcher methodMatcher;

                private final MethodAttributeAppender.Factory attributeAppenderFactory;

                public RequiredInterception(LatentMethodMatcher methodMatcher, MethodAttributeAppender.Factory attributeAppenderFactory) {
                    this.methodMatcher = methodMatcher;
                    this.attributeAppenderFactory = attributeAppenderFactory;
                }

                @Override
                protected Builder<S> materialize(MethodRegistry.Handler handler, Object defaultValue) {
                    return Builder.AbstractBase.this.materialize(classFileVersion,
                            modifiers,
                            interfaceTypes,
                            namingStrategy,
                            auxiliaryTypeNamingStrategy,
                            ignoredMethods,
                            typeAttributeAppender,
                            annotationDescriptions,
                            classVisitorWrapper,
                            bridgeMethodResolverFactory,
                            methodLookupEngineFactory, defaultFieldAttributeAppenderFactory,
                            defaultMethodAttributeAppenderFactory,
                            fieldRegistry,
                            methodRegistry.append(methodMatcher, handler, attributeAppenderFactory),
                            fieldTokens,
                            methodTokens);
                }

                @Override
                public ForMatchedMethod<S> attribute(MethodAttributeAppender.Factory attributeAppenderFactory) {
                    return new RequiredInterception(methodMatcher,
                            new MethodAttributeAppender.Factory.Compound(this.attributeAppenderFactory, nonNull(attributeAppenderFactory)));
                }

                @Override
                public ForMatchedMethod<S> annotateMethod(Collection<? extends AnnotationDescription> annotations) {
                    return attribute(new MethodAttributeAppender.ForAnnotation(nonNull(toList(annotations))));
                }

                @Override
                public ForMatchedMethod<S> annotateParameter(int parameterIndex, Collection<? extends AnnotationDescription> annotations) {
                    return attribute(new MethodAttributeAppender.ForAnnotation(parameterIndex, nonNull(toList(annotations))));
                }
            }

            protected class FieldDefinition extends FieldValueTarget.AnnotationDeclarable.AbstractBase<S> {

                private final FieldDescription.Token fieldToken;

                private final FieldAttributeAppender.Factory attributeAppenderFactory;

                public FieldDefinition(FieldDescription.Token fieldToken, FieldAttributeAppender.Factory attributeAppenderFactory) {
                    this.fieldToken = fieldToken;
                    this.attributeAppenderFactory = attributeAppenderFactory;
                }

                @Override
                protected Builder<S> materialize() {
                    return Builder.AbstractBase.this.materialize(classFileVersion,
                            modifiers,
                            interfaceTypes,
                            namingStrategy,
                            auxiliaryTypeNamingStrategy,
                            ignoredMethods,
                            typeAttributeAppender,
                            annotationDescriptions,
                            classVisitorWrapper,
                            bridgeMethodResolverFactory,
                            methodLookupEngineFactory,
                            defaultFieldAttributeAppenderFactory,
                            defaultMethodAttributeAppenderFactory,
                            fieldRegistry.include(new FieldRegistry.LatentFieldMatcher.Simple(fieldToken.getName()), attributeAppenderFactory, null),
                            methodRegistry,
                            unique(join(fieldTokens, fieldToken)),
                            methodTokens);
                }

                @Override
                public Builder<S> value(int value) {
                    return value(NumericRangeValidator.of(fieldToken.getType().asRawType()).validate(value), Integer.class);
                }

                @Override
                protected Builder<S> value(Object value, Class<?> expectedType) {
                    if (!fieldToken.getType().asRawType().represents(expectedType)) {
                        throw new IllegalArgumentException("Cannot assign value of type " + fieldToken.getType() + " to " + expectedType);
                    }
                    return Builder.AbstractBase.this.materialize(classFileVersion,
                            modifiers,
                            interfaceTypes,
                            namingStrategy,
                            auxiliaryTypeNamingStrategy,
                            ignoredMethods,
                            typeAttributeAppender,
                            annotationDescriptions,
                            classVisitorWrapper,
                            bridgeMethodResolverFactory,
                            methodLookupEngineFactory,
                            defaultFieldAttributeAppenderFactory,
                            defaultMethodAttributeAppenderFactory,
                            fieldRegistry.include(new FieldRegistry.LatentFieldMatcher.Simple(fieldToken.getName()), attributeAppenderFactory, value),
                            methodRegistry,
                            join(fieldTokens, fieldToken),
                            methodTokens);
                }

                @Override
                public AnnotationDeclarable<S> attribute(FieldAttributeAppender.Factory attributeAppenderFactory) {
                    return new FieldDefinition(fieldToken,
                            new FieldAttributeAppender.Factory.Compound(this.attributeAppenderFactory, nonNull(attributeAppenderFactory)));
                }

                @Override
                public AnnotationDeclarable<S> annotateField(Collection<? extends AnnotationDescription> annotations) {
                    List<AnnotationDescription> annotationList = new ArrayList<AnnotationDescription>(annotations);
                    return new FieldDefinition(new FieldDescription.Token(fieldToken.getName(),
                            fieldToken.getModifiers(),
                            fieldToken.getType(),
                            uniqueAnnotation(join(fieldToken.getAnnotations(), annotationList))),
                            new FieldAttributeAppender.Factory.Compound(attributeAppenderFactory, new FieldAttributeAppender.ForAnnotation(annotationList)));
                }
            }
        }
    }

    interface TypeVariableDefinable<T> {

        TypeVariableDefinable<T> withTypeVariable(String symbol, Type... bound);

        TypeVariableDefinable<T> withTypeVariable(String symbol, Iterable<? extends Type> bounds);

        TypeVariableDefinable<T> withTypeVariable(String symbol, GenericTypeDescription... bound);

        TypeVariableDefinable<T> withTypeVariable(String symbol, Collection<? extends GenericTypeDescription> bounds);

        Builder.MethodInterception.ParameterDefinable.Binary<T> defineMethod(String name, Type returnType, ModifierContributor.ForMethod... modifier);

        Builder.MethodInterception.ParameterDefinable.Binary<T> defineMethod(String name, GenericTypeDescription returnType, ModifierContributor.ForMethod... modifier);

        Builder.MethodInterception.ParameterDefinable.Binary<T> defineMethod(String name, Type returnType, int modifiers);

        Builder.MethodInterception.ParameterDefinable.Binary<T> defineMethod(String name, GenericTypeDescription returnType, int modifiers);

        Builder.MethodInterception<T> defineMethod(Method method);

        Builder.MethodInterception.ParameterDefinable.Binary<T> defineConstructor(ModifierContributor.ForMethod... modifier);

        Builder.MethodInterception.ParameterDefinable.Binary<T> defineConstructor(int modifiers);

        Builder.MethodInterception<T> defineConstructor(Constructor<?> constructor);

        Builder.MethodInterception<T> define(MethodDescription methodDescription);

        abstract class AbstractBase<S> implements TypeVariableDefinable<S> {

            @Override
            public TypeVariableDefinable<S> withTypeVariable(String symbol, Type... bound) {
                return withTypeVariable(symbol, Arrays.asList(bound));
            }

            @Override
            public TypeVariableDefinable<S> withTypeVariable(String symbol, Iterable<? extends Type> bounds) {
                return withTypeVariable(symbol, new GenericTypeList.ForLoadedType(toList(nonNull(bounds))));
            }

            @Override
            public TypeVariableDefinable<S> withTypeVariable(String symbol, GenericTypeDescription... bound) {
                return withTypeVariable(symbol, Arrays.asList(bound));
            }

            @Override
            public Builder.MethodInterception.ParameterDefinable.Binary<S> defineMethod(String name,
                                                                                        Type returnType,
                                                                                        ModifierContributor.ForMethod... modifier) {
                return defineMethod(name, GenericTypeDescription.Sort.describe(returnType), modifier);
            }

            @Override
            public Builder.MethodInterception.ParameterDefinable.Binary<S> defineMethod(String name,
                                                                                        GenericTypeDescription returnType,
                                                                                        ModifierContributor.ForMethod... modifier) {
                return defineMethod(name, returnType, resolveModifierContributors(METHOD_MODIFIER_MASK, modifier));
            }

            @Override
            public Builder.MethodInterception.ParameterDefinable.Binary<S> defineMethod(String name, Type returnType, int modifiers) {
                return defineMethod(name, GenericTypeDescription.Sort.describe(returnType), modifiers);
            }

            @Override
            public Builder.MethodInterception<S> defineMethod(Method method) {
                return define(new MethodDescription.ForLoadedMethod(nonNull(method)));
            }

            @Override
            public Builder.MethodInterception.ParameterDefinable.Binary<S> defineConstructor(ModifierContributor.ForMethod... modifier) {
                return defineConstructor(resolveModifierContributors(METHOD_MODIFIER_MASK, modifier));
            }

            @Override
            public Builder.MethodInterception<S> defineConstructor(Constructor<?> constructor) {
                return define(new MethodDescription.ForLoadedConstructor(nonNull(constructor)));
            }

            @Override
            public Builder.MethodInterception<S> define(MethodDescription methodDescription) {
                MethodDescription.Token token = methodDescription.asToken();
                Builder.MethodInterception.ParameterDefinable.Binary<S> builder = methodDescription.isMethod()
                        ? defineMethod(token.getInternalName(), token.getReturnType(), token.getModifiers())
                        : defineConstructor(token.getModifiers());
                Builder.MethodInterception.ExceptionDeclarable<S> exceptionDeclarable;
                if (methodDescription.getParameters().hasExplicitMetaData()) {
                    Builder.MethodInterception.ParameterDefinable.WithMetaData<S> parameterBuilder = builder;
                    for (ParameterDescription.Token parameter : token.getParameterTokens()) {
                        parameterBuilder = parameterBuilder
                                .withParameter(parameter.getType(), parameter.getName(), parameter.getModifiers())
                                .annotateParameter(parameter.getAnnotations());
                    }
                    exceptionDeclarable = parameterBuilder;
                } else {
                    Builder.MethodInterception.ParameterDefinable<S> parameterBuilder = builder;
                    for (ParameterDescription.Token parameter : token.getParameterTokens()) {
                        parameterBuilder = parameterBuilder
                                .withParameter(parameter.getType())
                                .annotateParameter(parameter.getAnnotations());
                    }
                    exceptionDeclarable = parameterBuilder;
                }
                return exceptionDeclarable.throwing(token.getExceptionTypes());
            }
        }
    }

    /**
     * A dynamic type that has been loaded into the running instance of the Java virtual machine.
     *
     * @param <T> The most specific known loaded type that is implemented by this dynamic type, usually the
     *            type itself, an interface or the direct super class.
     */
    interface Loaded<T> extends DynamicType {

        /**
         * Returns the loaded main class.
         *
         * @return A loaded class representation of this dynamic type.
         */
        Class<? extends T> getLoaded();

        /**
         * <p>
         * Returns a map of all loaded auxiliary types to this dynamic type.
         * </p>
         * <p>
         * <b>Note</b>: The type descriptions will most likely differ from the binary representation of this type.
         * Normally, annotations and intercepted methods are not added to the type descriptions of auxiliary types.
         * </p>
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
    interface Unloaded<T> extends DynamicType {

        /**
         * Attempts to load this dynamic type including all of its auxiliary types, if any.
         *
         * @param classLoader          The class loader to use for this class loading.
         * @param classLoadingStrategy The class loader strategy which should be used for this class loading.
         * @return This dynamic type in its loaded state.
         * @see net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default
         */
        Loaded<T> load(ClassLoader classLoader, ClassLoadingStrategy classLoadingStrategy);
    }

    /**
     * A default implementation of a dynamic type.
     */
    class Default implements DynamicType {

        /**
         * The file name extension for Java class files.
         */
        private static final String CLASS_FILE_EXTENSION = ".class";

        /**
         * The size of a writing buffer.
         */
        private static final int BUFFER_SIZE = 1024;

        /**
         * A convenience index for the beginning of an array to improve the readability of the code.
         */
        private static final int FROM_BEGINNING = 0;

        /**
         * A convenience representative of an {@link java.io.InputStream}'s end to improve the readability of the code.
         */
        private static final int END_OF_FILE = -1;

        /**
         * A suffix for temporary files.
         */
        private static final String TEMP_SUFFIX = "tmp";

        /**
         * A type description of this dynamic type.
         */
        protected final TypeDescription typeDescription;

        /**
         * The byte array representing this dynamic type.
         */
        protected final byte[] binaryRepresentation;

        /**
         * The loaded type initializer for this dynamic type.
         */
        protected final LoadedTypeInitializer loadedTypeInitializer;

        /**
         * A list of auxiliary types for this dynamic type.
         */
        protected final List<? extends DynamicType> auxiliaryTypes;

        /**
         * Creates a new dynamic type.
         *
         * @param typeDescription       A description of this dynamic type.
         * @param binaryRepresentation  A byte array containing the binary representation of this dynamic type.
         * @param loadedTypeInitializer The loaded type initializer of this dynamic type.
         * @param auxiliaryTypes        The auxiliary type required for this dynamic type.
         */
        public Default(TypeDescription typeDescription,
                       byte[] binaryRepresentation,
                       LoadedTypeInitializer loadedTypeInitializer,
                       List<? extends DynamicType> auxiliaryTypes) {
            this.typeDescription = typeDescription;
            this.binaryRepresentation = binaryRepresentation;
            this.loadedTypeInitializer = loadedTypeInitializer;
            this.auxiliaryTypes = auxiliaryTypes;
        }

        @Override
        public TypeDescription getTypeDescription() {
            return typeDescription;
        }

        @Override
        public Map<TypeDescription, byte[]> getAllTypes() {
            Map<TypeDescription, byte[]> allTypes = new HashMap<TypeDescription, byte[]>(auxiliaryTypes.size() + 1);
            for (DynamicType auxiliaryType : auxiliaryTypes) {
                allTypes.putAll(auxiliaryType.getAllTypes());
            }
            allTypes.put(typeDescription, binaryRepresentation);
            return allTypes;
        }

        @Override
        public Map<TypeDescription, LoadedTypeInitializer> getLoadedTypeInitializers() {
            Map<TypeDescription, LoadedTypeInitializer> classLoadingCallbacks = new HashMap<TypeDescription, LoadedTypeInitializer>(
                    auxiliaryTypes.size() + 1);
            for (DynamicType auxiliaryType : auxiliaryTypes) {
                classLoadingCallbacks.putAll(auxiliaryType.getLoadedTypeInitializers());
            }
            classLoadingCallbacks.put(typeDescription, loadedTypeInitializer);
            return classLoadingCallbacks;
        }

        @Override
        public boolean hasAliveLoadedTypeInitializers() {
            for (LoadedTypeInitializer loadedTypeInitializer : getLoadedTypeInitializers().values()) {
                if (loadedTypeInitializer.isAlive()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public byte[] getBytes() {
            return binaryRepresentation;
        }

        @Override
        public Map<TypeDescription, byte[]> getRawAuxiliaryTypes() {
            Map<TypeDescription, byte[]> auxiliaryTypes = new HashMap<TypeDescription, byte[]>();
            for (DynamicType auxiliaryType : this.auxiliaryTypes) {
                auxiliaryTypes.put(auxiliaryType.getTypeDescription(), auxiliaryType.getBytes());
                auxiliaryTypes.putAll(auxiliaryType.getRawAuxiliaryTypes());
            }
            return auxiliaryTypes;
        }

        @Override
        public Map<TypeDescription, File> saveIn(File folder) throws IOException {
            Map<TypeDescription, File> savedFiles = new HashMap<TypeDescription, File>();
            File target = new File(folder, typeDescription.getName().replace('.', File.separatorChar) + CLASS_FILE_EXTENSION);
            if (target.getParentFile() != null) {
                target.getParentFile().mkdirs();
            }
            OutputStream outputStream = new FileOutputStream(target);
            try {
                outputStream.write(binaryRepresentation);
            } finally {
                outputStream.close();
            }
            savedFiles.put(typeDescription, target);
            for (DynamicType auxiliaryType : auxiliaryTypes) {
                savedFiles.putAll(auxiliaryType.saveIn(folder));
            }
            return savedFiles;
        }

        @Override
        public File inject(File sourceJar, File targetJar) throws IOException {
            JarInputStream jarInputStream = new JarInputStream(new BufferedInputStream(new FileInputStream(sourceJar)));
            try {
                targetJar.createNewFile();
                JarOutputStream jarOutputStream = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(targetJar)), jarInputStream.getManifest());
                try {
                    Map<TypeDescription, byte[]> rawAuxiliaryTypes = getRawAuxiliaryTypes();
                    Map<String, byte[]> files = new HashMap<String, byte[]>(rawAuxiliaryTypes.size() + 1);
                    for (Map.Entry<TypeDescription, byte[]> entry : rawAuxiliaryTypes.entrySet()) {
                        files.put(entry.getKey().getInternalName() + CLASS_FILE_EXTENSION, entry.getValue());
                    }
                    files.put(typeDescription.getInternalName() + CLASS_FILE_EXTENSION, binaryRepresentation);
                    JarEntry jarEntry;
                    while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                        jarOutputStream.putNextEntry(jarEntry);
                        byte[] replacement = files.remove(jarEntry.getName());
                        if (replacement == null) {
                            byte[] buffer = new byte[BUFFER_SIZE];
                            int index;
                            while ((index = jarInputStream.read(buffer)) != END_OF_FILE) {
                                jarOutputStream.write(buffer, FROM_BEGINNING, index);
                            }
                        } else {
                            jarOutputStream.write(replacement);
                        }
                        jarInputStream.closeEntry();
                        jarOutputStream.closeEntry();
                    }
                    for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                        jarOutputStream.putNextEntry(new JarEntry(entry.getKey()));
                        jarOutputStream.write(entry.getValue());
                        jarOutputStream.closeEntry();
                    }
                } finally {
                    jarOutputStream.close();
                }
            } finally {
                jarInputStream.close();
            }
            return targetJar;
        }

        @Override
        public File inject(File jar) throws IOException {
            File temporary = inject(jar, File.createTempFile(jar.getName(), TEMP_SUFFIX));
            try {
                InputStream jarInputStream = new BufferedInputStream(new FileInputStream(temporary));
                try {
                    OutputStream jarOutputStream = new BufferedOutputStream(new FileOutputStream(jar));
                    try {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int index;
                        while ((index = jarInputStream.read(buffer)) != END_OF_FILE) {
                            jarOutputStream.write(buffer, FROM_BEGINNING, index);
                        }
                    } finally {
                        jarOutputStream.close();
                    }
                } finally {
                    jarInputStream.close();
                }
            } finally {
                if (!temporary.delete()) {
                    Logger.getAnonymousLogger().warning("Cannot delete " + temporary);
                }
            }
            return jar;
        }

        @Override
        public File toJar(File file, Manifest manifest) throws IOException {
            file.createNewFile();
            JarOutputStream outputStream = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(file)), manifest);
            try {
                for (Map.Entry<TypeDescription, byte[]> entry : getRawAuxiliaryTypes().entrySet()) {
                    outputStream.putNextEntry(new JarEntry(entry.getKey().getInternalName() + CLASS_FILE_EXTENSION));
                    outputStream.write(entry.getValue());
                    outputStream.closeEntry();
                }
                outputStream.putNextEntry(new JarEntry(typeDescription.getInternalName() + CLASS_FILE_EXTENSION));
                outputStream.write(binaryRepresentation);
                outputStream.closeEntry();
            } finally {
                outputStream.close();
            }
            return file;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (other == null || getClass() != other.getClass())
                return false;
            Default aDefault = (Default) other;
            return auxiliaryTypes.equals(aDefault.auxiliaryTypes)
                    && Arrays.equals(binaryRepresentation, aDefault.binaryRepresentation)
                    && typeDescription.equals(aDefault.typeDescription)
                    && loadedTypeInitializer.equals(aDefault.loadedTypeInitializer);

        }

        @Override
        public int hashCode() {
            int result = typeDescription.hashCode();
            result = 31 * result + Arrays.hashCode(binaryRepresentation);
            result = 31 * result + loadedTypeInitializer.hashCode();
            result = 31 * result + auxiliaryTypes.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "DynamicType.Default{" +
                    "typeDescription='" + typeDescription + '\'' +
                    ", binaryRepresentation=<" + binaryRepresentation.length + " bytes>" +
                    ", loadedTypeInitializer=" + loadedTypeInitializer +
                    ", auxiliaryTypes=" + auxiliaryTypes +
                    '}';
        }

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
             * @param typeDescription       A description of this dynamic type.
             * @param typeByte              An array of byte of the binary representation of this dynamic type.
             * @param loadedTypeInitializer The type initializer of this dynamic type.
             * @param auxiliaryTypes        The auxiliary types that are required for this dynamic type.
             */
            public Unloaded(TypeDescription typeDescription,
                            byte[] typeByte,
                            LoadedTypeInitializer loadedTypeInitializer,
                            List<? extends DynamicType> auxiliaryTypes) {
                super(typeDescription, typeByte, loadedTypeInitializer, auxiliaryTypes);
            }

            @Override
            public DynamicType.Loaded<T> load(ClassLoader classLoader, ClassLoadingStrategy classLoadingStrategy) {
                LinkedHashMap<TypeDescription, byte[]> types = new LinkedHashMap<TypeDescription, byte[]>(
                        getRawAuxiliaryTypes());
                types.put(typeDescription, binaryRepresentation);
                return new Default.Loaded<T>(typeDescription,
                        binaryRepresentation,
                        loadedTypeInitializer,
                        auxiliaryTypes,
                        initialize(classLoadingStrategy.load(classLoader, types)));
            }

            /**
             * Runs all loaded type initializers for all loaded classes.
             *
             * @param uninitialized The uninitialized loaded classes mapped by their type description.
             * @return A new hash map that contains the same classes as those given.
             */
            private Map<TypeDescription, Class<?>> initialize(Map<TypeDescription, Class<?>> uninitialized) {
                Map<TypeDescription, LoadedTypeInitializer> typeInitializers = getLoadedTypeInitializers();
                for (Map.Entry<TypeDescription, Class<?>> entry : uninitialized.entrySet()) {
                    typeInitializers.get(entry.getKey()).onLoad(entry.getValue());
                }
                return new HashMap<TypeDescription, Class<?>>(uninitialized);
            }

            @Override
            public String toString() {
                return "DynamicType.Default.Unloaded{" +
                        "typeDescription='" + typeDescription + '\'' +
                        ", binaryRepresentation=<" + binaryRepresentation.length + " bytes>" +
                        ", typeInitializer=" + loadedTypeInitializer +
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

            /**
             * The loaded types for the given loaded dynamic type.
             */
            private final Map<TypeDescription, Class<?>> loadedTypes;

            /**
             * Creates a new representation of a loaded dynamic type.
             *
             * @param typeDescription       A description of this dynamic type.
             * @param typeByte              An array of byte of the binary representation of this dynamic type.
             * @param loadedTypeInitializer The type initializer of this dynamic type.
             * @param auxiliaryTypes        The auxiliary types that are required for this dynamic type.
             * @param loadedTypes           A map of loaded types for this dynamic type and all its auxiliary types.
             */
            protected Loaded(TypeDescription typeDescription,
                             byte[] typeByte,
                             LoadedTypeInitializer loadedTypeInitializer,
                             List<? extends DynamicType> auxiliaryTypes,
                             Map<TypeDescription, Class<?>> loadedTypes) {
                super(typeDescription, typeByte, loadedTypeInitializer, auxiliaryTypes);
                this.loadedTypes = loadedTypes;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends T> getLoaded() {
                return (Class<? extends T>) loadedTypes.get(typeDescription);
            }

            @Override
            public Map<TypeDescription, Class<?>> getLoadedAuxiliaryTypes() {
                Map<TypeDescription, Class<?>> loadedAuxiliaryTypes = new HashMap<TypeDescription, Class<?>>(
                        loadedTypes);
                loadedAuxiliaryTypes.remove(typeDescription);
                return loadedAuxiliaryTypes;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && super.equals(other) && loadedTypes.equals(((Default.Loaded) other).loadedTypes);
            }

            @Override
            public int hashCode() {
                return 31 * super.hashCode() + loadedTypes.hashCode();
            }

            @Override
            public String toString() {
                return "DynamicType.Default.Loaded{" +
                        "typeDescription='" + typeDescription + '\'' +
                        ", binaryRepresentation=<" + binaryRepresentation.length + " bytes>" +
                        ", typeInitializer=" + loadedTypeInitializer +
                        ", auxiliaryTypes=" + auxiliaryTypes +
                        ", loadedTypes=" + loadedTypes +
                        '}';
            }
        }
    }
}
