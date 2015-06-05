package net.bytebuddy.dynamic;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.*;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.matcher.LatentMethodMatcher;
import org.objectweb.asm.Opcodes;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    interface Builder<T> {

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
        OptionalMatchedMethodInterception<T> implement(Class<?>... interfaceType);

        /**
         * Adds the given interfaces to be implemented by the created type.
         *
         * @param interfaceTypes The interfaces to implement.
         * @return A builder which will create a dynamic type that implements the given interfaces.
         */
        OptionalMatchedMethodInterception<T> implement(Iterable<? extends Class<?>> interfaceTypes);

        /**
         * Adds the given interfaces to be implemented by the created type.
         *
         * @param interfaceType A description of the interfaces to implement.
         * @return A builder which will create a dynamic type that implements the given interfaces.
         */
        OptionalMatchedMethodInterception<T> implement(TypeDescription... interfaceType);

        /**
         * Adds the given interfaces to be implemented by the created type.
         *
         * @param interfaceTypes A description of the interfaces to implement.
         * @return A builder which will create a dynamic type that implements the given interfaces.
         */
        OptionalMatchedMethodInterception<T> implement(Collection<? extends TypeDescription> interfaceTypes);

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
        FieldValueTarget<T> defineField(String name, Class<?> fieldType, ModifierContributor.ForField... modifier);

        /**
         * Defines a new field for this type.
         *
         * @param name                 The name of the method.
         * @param fieldTypeDescription The type of this field where the current type can be represented by
         *                             {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifier             The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        FieldValueTarget<T> defineField(String name, TypeDescription fieldTypeDescription, ModifierContributor.ForField... modifier);

        /**
         * Defines a new field for this type.
         *
         * @param name      The name of the method.
         * @param fieldType The type of this field where the current type can be represented by
         *                  {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifiers The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        FieldValueTarget<T> defineField(String name, Class<?> fieldType, int modifiers);

        /**
         * Defines a new field for this type.
         *
         * @param name                 The name of the method.
         * @param fieldTypeDescription The type of this field where the current type can be represented by
         *                             {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifiers            The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        FieldValueTarget<T> defineField(String name, TypeDescription fieldTypeDescription, int modifiers);

        /**
         * Defines a new field for this type. The annotations of the given field are not copied and
         * must be added manually if they are required.
         *
         * @param field The field that the generated type should imitate.
         * @return An interception delegate that exclusively matches the new method.
         */
        FieldValueTarget<T> defineField(Field field);

        /**
         * Defines a new field for this type. The annotations of the given field are not copied and
         * must be added manually if they are required.
         *
         * @param fieldDescription The field that the generated type should imitate.
         * @return An interception delegate that exclusively matches the new method.
         */
        FieldValueTarget<T> defineField(FieldDescription fieldDescription);

        /**
         * Defines a new method for this type.
         * <p>&nbsp;</p>
         * Note that a method definition overrides any method of identical signature that was defined in a super
         * type what is only valid if the method is of at least broader visibility and if the overridden method
         * is not {@code final}.
         *
         * @param name           The name of the method.
         * @param returnType     The return type of the method  where the current type can be represented by
         *                       {@link net.bytebuddy.dynamic.TargetType}.
         * @param parameterTypes The parameter types of this method  where the current type can be represented by
         *                       {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifier       The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        ExceptionDeclarableMethodInterception<T> defineMethod(String name,
                                                              Class<?> returnType,
                                                              List<? extends Class<?>> parameterTypes,
                                                              ModifierContributor.ForMethod... modifier);

        /**
         * Defines a new method for this type.
         * <p>&nbsp;</p>
         * Note that a method definition overrides any method of identical signature that was defined in a super
         * type what is only valid if the method is of at least broader visibility and if the overridden method
         * is not {@code final}.
         *
         * @param name           The name of the method.
         * @param returnType     A description of the return type of the method  where the current type can be
         *                       represented by {@link net.bytebuddy.dynamic.TargetType}.
         * @param parameterTypes Descriptions of the parameter types of this method  where the current type can be
         *                       represented by {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifier       The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        ExceptionDeclarableMethodInterception<T> defineMethod(String name,
                                                              TypeDescription returnType,
                                                              List<? extends TypeDescription> parameterTypes,
                                                              ModifierContributor.ForMethod... modifier);

        /**
         * Defines a new method for this type.
         * <p>&nbsp;</p>
         * Note that a method definition overrides any method of identical signature that was defined in a super
         * type what is only valid if the method is of at least broader visibility and if the overridden method
         * is not {@code final}.
         *
         * @param name           The name of the method.
         * @param returnType     The return type of the method  where the current type can be represented by
         *                       {@link net.bytebuddy.dynamic.TargetType}.
         * @param parameterTypes The parameter types of this method  where the current type can be represented by
         *                       {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifiers      The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        ExceptionDeclarableMethodInterception<T> defineMethod(String name,
                                                              Class<?> returnType,
                                                              List<? extends Class<?>> parameterTypes,
                                                              int modifiers);

        /**
         * Defines a new method for this type.
         * <p>&nbsp;</p>
         * Note that a method definition overrides any method of identical signature that was defined in a super
         * type what is only valid if the method is of at least broader visibility and if the overridden method
         * is not {@code final}.
         *
         * @param name           The name of the method.
         * @param returnType     A description of the return type of the method  where the current type can be
         *                       represented by {@link net.bytebuddy.dynamic.TargetType}.
         * @param parameterTypes Descriptions of the parameter types of this method  where the current type can be
         *                       represented by {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifiers      The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        ExceptionDeclarableMethodInterception<T> defineMethod(String name,
                                                              TypeDescription returnType,
                                                              List<? extends TypeDescription> parameterTypes,
                                                              int modifiers);

        /**
         * Defines a new method for this type. Declared exceptions or annotations of the method are not copied and must
         * be added manually.
         * <p>&nbsp;</p>
         * Note that a method definition overrides any method of identical signature that was defined in a super
         * type what is only valid if the method is of at least broader visibility and if the overridden method
         * is not {@code final}.
         *
         * @param method The method that the generated type should imitate.
         * @return An interception delegate that exclusively matches the new method.
         */
        ExceptionDeclarableMethodInterception<T> defineMethod(Method method);

        /**
         * Defines a new method for this type. Declared exceptions or annotations of the method are not copied and must
         * be added manually.
         * <p>&nbsp;</p>
         * Note that a method definition overrides any method of identical signature that was defined in a super
         * type what is only valid if the method is of at least broader visibility and if the overridden method
         * is not {@code final}.
         *
         * @param methodDescription The method that the generated type should imitate.
         * @return An interception delegate that exclusively matches the new method.
         */
        ExceptionDeclarableMethodInterception<T> defineMethod(MethodDescription methodDescription);

        /**
         * Defines a new constructor for this type. A constructor must not be {@code static}. Instead, a static type
         * initializer is added automatically if such an initializer is required. See
         * {@link net.bytebuddy.matcher.ElementMatchers#isTypeInitializer()} for a matcher for this initializer
         * which can be intercepted using
         * {@link net.bytebuddy.dynamic.DynamicType.Builder#invokable(net.bytebuddy.matcher.ElementMatcher)}.
         * <p>&nbsp;</p>
         * Note that a constructor's implementation must call another constructor of the same class or a constructor of
         * its super class. This constructor call must be hardcoded inside of the constructor's method body. Before
         * this constructor call is made, it is not legal to call any methods or to read any fields of the instance
         * under construction.
         *
         * @param parameterTypes The parameter types of this constructor where the current type can be represented by
         *                       {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifier       The modifiers for this constructor.
         * @return An interception delegate that exclusively matches the new constructor.
         */
        ExceptionDeclarableMethodInterception<T> defineConstructor(Iterable<? extends Class<?>> parameterTypes, ModifierContributor.ForMethod... modifier);

        /**
         * Defines a new constructor for this type. A constructor must not be {@code static}. Instead, a static type
         * initializer is added automatically if such an initializer is required. See
         * {@link net.bytebuddy.matcher.ElementMatchers#isTypeInitializer()} for a matcher for this initializer
         * which can be intercepted using
         * {@link net.bytebuddy.dynamic.DynamicType.Builder#invokable(net.bytebuddy.matcher.ElementMatcher)}.
         * <p>&nbsp;</p>
         * Note that a constructor's implementation must call another constructor of the same class or a constructor of
         * its super class. This constructor call must be hardcoded inside of the constructor's method body. Before
         * this constructor call is made, it is not legal to call any methods or to read any fields of the instance
         * under construction.
         *
         * @param parameterTypes The parameter types of this constructor where the current type can be represented by
         *                       {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifier       The modifiers for this constructor.
         * @return An interception delegate that exclusively matches the new constructor.
         */
        ExceptionDeclarableMethodInterception<T> defineConstructor(List<? extends TypeDescription> parameterTypes, ModifierContributor.ForMethod... modifier);

        /**
         * Defines a new constructor for this type. A constructor must not be {@code static}. Instead, a static type
         * initializer is added automatically if such an initializer is required. See
         * {@link net.bytebuddy.matcher.ElementMatchers#isTypeInitializer()} for a matcher for this initializer
         * which can be intercepted using
         * {@link net.bytebuddy.dynamic.DynamicType.Builder#invokable(net.bytebuddy.matcher.ElementMatcher)}.
         * <p>&nbsp;</p>
         * Note that a constructor's implementation must call another constructor of the same class or a constructor of
         * its super class. This constructor call must be hardcoded inside of the constructor's method body. Before
         * this constructor call is made, it is not legal to call any methods or to read any fields of the instance
         * under construction.
         *
         * @param parameterTypes The parameter types of this constructor where the current type can be represented by
         *                       {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifiers      The modifiers for this constructor.
         * @return An interception delegate that exclusively matches the new constructor.
         */
        ExceptionDeclarableMethodInterception<T> defineConstructor(Iterable<? extends Class<?>> parameterTypes, int modifiers);

        /**
         * Defines a new constructor for this type. A constructor must not be {@code static}. Instead, a static type
         * initializer is added automatically if such an initializer is required. See
         * {@link net.bytebuddy.matcher.ElementMatchers#isTypeInitializer()} for a matcher for this initializer
         * which can be intercepted using
         * {@link net.bytebuddy.dynamic.DynamicType.Builder#invokable(net.bytebuddy.matcher.ElementMatcher)}.
         * <p>&nbsp;</p>
         * Note that a constructor's implementation must call another constructor of the same class or a constructor of
         * its super class. This constructor call must be hardcoded inside of the constructor's method body. Before
         * this constructor call is made, it is not legal to call any methods or to read any fields of the instance
         * under construction.
         *
         * @param parameterTypes The parameter types of this constructor where the current type can be represented by
         *                       {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifiers      The modifiers for this constructor.
         * @return An interception delegate that exclusively matches the new constructor.
         */
        ExceptionDeclarableMethodInterception<T> defineConstructor(List<? extends TypeDescription> parameterTypes, int modifiers);

        /**
         * Defines a new constructor for this type. A constructor must not be {@code static}. Instead, a static type
         * initializer is added automatically if such an initializer is required. See
         * {@link net.bytebuddy.matcher.ElementMatchers#isTypeInitializer()} for a matcher for this initializer
         * which can be intercepted using
         * {@link net.bytebuddy.dynamic.DynamicType.Builder#invokable(net.bytebuddy.matcher.ElementMatcher)}.
         * Declared exceptions or annotations of the method are not copied and must be added manually.
         * <p>&nbsp;</p>
         * Note that a constructor's implementation must call another constructor of the same class or a constructor of
         * its super class. This constructor call must be hardcoded inside of the constructor's method body. Before
         * this constructor call is made, it is not legal to call any methods or to read any fields of the instance
         * under construction.
         *
         * @param constructor The constructor for the generated type to imitate.
         * @return An interception delegate that exclusively matches the new constructor.
         */
        ExceptionDeclarableMethodInterception<T> defineConstructor(Constructor<?> constructor);

        /**
         * Defines a new constructor for this type. A constructor must not be {@code static}. Instead, a static type
         * initializer is added automatically if such an initializer is required. See
         * {@link net.bytebuddy.matcher.ElementMatchers#isTypeInitializer()} for a matcher for this initializer
         * which can be intercepted using
         * {@link net.bytebuddy.dynamic.DynamicType.Builder#invokable(net.bytebuddy.matcher.ElementMatcher)}.
         * Declared exceptions or annotations of the method are not copied and must be added manually.
         * <p>&nbsp;</p>
         * Note that a constructor's implementation must call another constructor of the same class or a constructor of
         * its super class. This constructor call must be hardcoded inside of the constructor's method body. Before
         * this constructor call is made, it is not legal to call any methods or to read any fields of the instance
         * under construction.
         *
         * @param methodDescription The constructor for the generated type to imitate.
         * @return An interception delegate that exclusively matches the new constructor.
         */
        ExceptionDeclarableMethodInterception<T> defineConstructor(MethodDescription methodDescription);

        /**
         * Defines a new method or constructor for this type. Declared exceptions or annotations of the method
         * are not copied and must be added manually.
         * <p>&nbsp;</p>
         * Note that a method definition overrides any method of identical signature that was defined in a super
         * type what is only valid if the method is of at least broader visibility and if the overridden method
         * is not {@code final}.
         *
         * @param methodDescription The method tor constructor hat the generated type should imitate.
         * @return An interception delegate that exclusively matches the new method or constructor.
         */
        ExceptionDeclarableMethodInterception<T> define(MethodDescription methodDescription);

        /**
         * Selects a set of methods of this type for instrumentation.
         *
         * @param methodMatcher A matcher describing the methods to be intercepted by this instrumentation.
         * @return An interception delegate for methods matching the given method matcher.
         */
        MatchedMethodInterception<T> method(ElementMatcher<? super MethodDescription> methodMatcher);

        /**
         * Selects a set of constructors of this type for implementation.
         *
         * @param methodMatcher A matcher describing the constructors to be intercepted by this implementation.
         * @return An interception delegate for constructors matching the given method matcher.
         */
        MatchedMethodInterception<T> constructor(ElementMatcher<? super MethodDescription> methodMatcher);

        /**
         * Selects a set of byte code level methods, i.e. methods, constructors and the type initializer of
         * this type for implementation.
         *
         * @param methodMatcher A matcher describing the byte code methods to be intercepted by this implementation.
         * @return An interception delegate for byte code methods matching the given method matcher.
         */
        MatchedMethodInterception<T> invokable(ElementMatcher<? super MethodDescription> methodMatcher);

        /**
         * Selects a set of byte code level methods, i.e. methods, construcors and the type initializer of
         * this type for implementation.
         *
         * @param methodMatcher A latent matcher describing the byte code methods to be intercepted by this implementation.
         * @return An interception delegate for byte code methods matching the given method matcher.
         */
        MatchedMethodInterception<T> invokable(LatentMethodMatcher methodMatcher);

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
        interface MatchedMethodInterception<S> {

            /**
             * Intercepts the currently selected methods with the provided implementation. If this intercepted method is
             * not yet declared by the current type, it might be added to the currently built type as a result of this
             * interception. If the method is already declared by the current type, its byte code code might be copied
             * into the body of a synthetic method in order to preserve the original code's invokeability.
             *
             * @param implementation The implementation to apply to the currently selected method.
             * @return A builder which will intercept the currently selected methods by the given implementation.
             */
            MethodAnnotationTarget<S> intercept(Implementation implementation);

            /**
             * Implements the currently selected methods as {@code abstract} methods.
             *
             * @return A builder which will implement the currently selected methods as {@code abstract} methods.
             */
            MethodAnnotationTarget<S> withoutCode();

            /**
             * Defines a default annotation value to set for any matched method.
             *
             * @param value The value that the annotation property should set as a default.
             * @param type  The type of the annotation property.
             * @return A builder which defines the given default value for all matched methods.
             */
            MethodAnnotationTarget<S> withDefaultValue(Object value, Class<?> type);

            /**
             * Defines a default annotation value to set for any matched method. The value is to be represented in a wrapper format,
             * {@code enum} values should be handed as {@link net.bytebuddy.description.enumeration.EnumerationDescription}
             * instances, annotations as {@link AnnotationDescription} instances and
             * {@link Class} values as {@link TypeDescription} instances. Other values are handed in their raw format or as their wrapper types.
             *
             * @param value A non-loaded value that the annotation property should set as a default.
             * @return A builder which defines the given default value for all matched methods.
             */
            MethodAnnotationTarget<S> withDefaultValue(Object value);
        }

        /**
         * Defines an implementation for a method that was added to this instrumentation and allows to include
         * exception declarations for the newly defined method.
         *
         * @param <S> The most specific known loaded type that is implemented by the created dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        interface ExceptionDeclarableMethodInterception<S> extends MatchedMethodInterception<S> {

            /**
             * Defines a number of {@link java.lang.Throwable} types to be include in the exception declaration.
             *
             * @param exceptionType The types that should be declared to be thrown by the selected method.
             * @return A target for instrumenting the defined method where the method will declare the given exception
             * types.
             */
            MatchedMethodInterception<S> throwing(Class<?>... exceptionType);

            /**
             * Defines a number of {@link java.lang.Throwable} types to be include in the exception declaration.
             *
             * @param exceptionTypes The types that should be declared to be thrown by the selected method.
             * @return A target for instrumenting the defined method where the method will declare the given exception
             * types.
             */
            MatchedMethodInterception<S> throwing(Iterable<? extends Class<?>> exceptionTypes);

            /**
             * Defines a number of {@link java.lang.Throwable} types to be include in the exception declaration.
             *
             * @param exceptionType Descriptions of the types that should be declared to be thrown by the selected method.
             * @return A target for instrumenting the defined method where the method will declare the given exception
             * types.
             */
            MatchedMethodInterception<S> throwing(TypeDescription... exceptionType);

            /**
             * Defines a number of {@link java.lang.Throwable} types to be include in the exception declaration.
             *
             * @param exceptionTypes Descriptions of the types that should be declared to be thrown by the selected method.
             * @return A target for instrumenting the defined method where the method will declare the given exception
             * types.
             */
            MatchedMethodInterception<S> throwing(Collection<? extends TypeDescription> exceptionTypes);
        }

        /**
         * An optional matched method interception allows to define an interception without requiring the definition
         * of an implementation.
         *
         * @param <S> The most specific known loaded type that is implemented by the created dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        interface OptionalMatchedMethodInterception<S> extends MatchedMethodInterception<S>, Builder<S> {
            /* This interface is merely a combinator of the matched method interception and the builder interfaces. */
        }

        /**
         * A builder to which a method was just added or an interception for existing methods was specified such that
         * attribute changes can be applied to these methods.
         *
         * @param <S> The most specific known loaded type that is implemented by the created dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        interface MethodAnnotationTarget<S> extends Builder<S> {

            /**
             * Defines an attribute appender factory to be applied onto the currently selected methods.
             *
             * @param attributeAppenderFactory The attribute appender factory to apply onto the currently selected
             *                                 methods.
             * @return A builder where the given attribute appender factory will be applied to the currently selected methods.
             */
            MethodAnnotationTarget<S> attribute(MethodAttributeAppender.Factory attributeAppenderFactory);

            /**
             * Defines annotations to be added to the currently selected method.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to
             * {@link Implementation}s.
             *
             * @param annotation The annotations to add to the currently selected methods.
             * @return A builder where the given annotation will be added to the currently selected methods.
             */
            MethodAnnotationTarget<S> annotateMethod(Annotation... annotation);

            /**
             * Defines annotations to be added to the currently selected method.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to
             * {@link Implementation}s.
             *
             * @param annotations The annotations to add to the currently selected methods.
             * @return A builder where the given annotation will be added to the currently selected methods.
             */
            MethodAnnotationTarget<S> annotateMethod(Iterable<? extends Annotation> annotations);

            /**
             * Defines annotations to be added to the currently selected method.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to
             * {@link Implementation}s.
             *
             * @param annotation The annotations to add to the currently selected methods.
             * @return A builder where the given annotation will be added to the currently selected methods.
             */
            MethodAnnotationTarget<S> annotateMethod(AnnotationDescription... annotation);

            /**
             * Defines annotations to be added to the currently selected method.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to
             * {@link Implementation}s.
             *
             * @param annotations The annotations to add to the currently selected methods.
             * @return A builder where the given annotation will be added to the currently selected methods.
             */
            MethodAnnotationTarget<S> annotateMethod(Collection<? extends AnnotationDescription> annotations);

            /**
             * Defines annotations to be added to a parameter of the currently selected methods.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to
             * {@link Implementation}s.
             *
             * @param parameterIndex The index of the parameter to annotate.
             * @param annotation     The annotations to add to a parameter of the currently selected methods.
             * @return A builder where the given annotation will be added to a parameter of the currently selected
             * methods.
             */
            MethodAnnotationTarget<S> annotateParameter(int parameterIndex, Annotation... annotation);

            /**
             * Defines annotations to be added to a parameter of the currently selected methods.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to
             * {@link Implementation}s.
             *
             * @param parameterIndex The index of the parameter to annotate.
             * @param annotations    The annotations to add to a parameter of the currently selected methods.
             * @return A builder where the given annotation will be added to a parameter of the currently selected
             * methods.
             */
            MethodAnnotationTarget<S> annotateParameter(int parameterIndex, Iterable<? extends Annotation> annotations);

            /**
             * Defines annotations to be added to a parameter of the currently selected methods.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to
             * {@link Implementation}s.
             *
             * @param parameterIndex The index of the parameter to annotate.
             * @param annotation     The annotations to add to a parameter of the currently selected methods.
             * @return A builder where the given annotation will be added to a parameter of the currently selected
             * methods.
             */
            MethodAnnotationTarget<S> annotateParameter(int parameterIndex, AnnotationDescription... annotation);

            /**
             * Defines annotations to be added to a parameter of the currently selected methods.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to
             * {@link Implementation}s.
             *
             * @param parameterIndex The index of the parameter to annotate.
             * @param annotations    The annotations to add to a parameter of the currently selected methods.
             * @return A builder where the given annotation will be added to a parameter of the currently selected
             * methods.
             */
            MethodAnnotationTarget<S> annotateParameter(int parameterIndex, Collection<? extends AnnotationDescription> annotations);
        }

        /**
         * A builder to which a field was just added such that default values can be defined for the field. Default
         * values must only be defined for {@code static} fields of a primitive type or of the {@link java.lang.String}
         * type.
         *
         * @param <S> The most specific known type of the dynamic type, usually the type itself, an interface or the
         *            direct super class.
         */
        interface FieldValueTarget<S> extends FieldAnnotationTarget<S> {

            /**
             * Defines a {@code boolean} value to become the optional default value for the recently defined
             * {@code static} field. Defining such a boolean default value is only legal for fields that are
             * represented as an integer within the Java virtual machine. These types are the {@code boolean} type,
             * the {@code byte} type, the {@code short} type, the {@code char} type and the {@code int} type.
             *
             * @param value The value to be defined as a default value for the recently defined field.
             * @return A field annotation target for the currently defined field.
             */
            FieldAnnotationTarget<S> value(boolean value);

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
            FieldAnnotationTarget<S> value(int value);

            /**
             * Defined a default value for a {@code long}-typed {@code static} field. This is only legal if the
             * defined field is also of type {@code long}.
             *
             * @param value The value to be defined as a default value for the recently defined field.
             * @return A field annotation target for the currently defined field.
             */
            FieldAnnotationTarget<S> value(long value);

            /**
             * Defined a default value for a {@code float}-typed {@code static} field. This is only legal if the
             * defined field is also of type {@code float}.
             *
             * @param value The value to be defined as a default value for the recently defined field.
             * @return A field annotation target for the currently defined field.
             */
            FieldAnnotationTarget<S> value(float value);

            /**
             * Defined a default value for a {@code double}-typed {@code static} field. This is only legal if the
             * defined field is also of type {@code double}.
             *
             * @param value The value to be defined as a default value for the recently defined field.
             * @return A field annotation target for the currently defined field.
             */
            FieldAnnotationTarget<S> value(double value);

            /**
             * Defined a default value for a {@link java.lang.String}-typed {@code static} field. This is only legal if
             * the defined field is also of type {@link java.lang.String}. The string must not be {@code null}.
             *
             * @param value The value to be defined as a default value for the recently defined field.
             * @return A field annotation target for the currently defined field.
             */
            FieldAnnotationTarget<S> value(String value);

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
                        throw new IllegalStateException(String.format("A field of type %s does not permit an " +
                                "integer-typed default value", typeDescription));
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
                        throw new IllegalArgumentException(String.format("The value %d overflows for %s", value, this));
                    }
                    return value;
                }

                @Override
                public String toString() {
                    return "DynamicType.Builder.FieldValueTarget.NumericRangeValidator." + name();
                }
            }
        }

        /**
         * A builder to which a field was just added such that attribute changes can be applied to this field.
         *
         * @param <S> The most specific known type of the dynamic type, usually the type itself, an interface or the
         *            direct super class.
         */
        interface FieldAnnotationTarget<S> extends Builder<S> {

            /**
             * Defines an attribute appender factory to be applied onto the currently selected field.
             *
             * @param attributeAppenderFactory The attribute appender factory to apply onto the currently selected
             *                                 field.
             * @return A builder where the given attribute appender factory will be applied to the currently selected field.
             */
            FieldAnnotationTarget<S> attribute(FieldAttributeAppender.Factory attributeAppenderFactory);

            /**
             * Defines annotations to be added to the currently selected field.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to {@link Implementation}s.
             *
             * @param annotation The annotations to add to the currently selected field.
             * @return A builder where the given annotation will be added to the currently selected field.
             */
            FieldAnnotationTarget<S> annotateField(Annotation... annotation);

            /**
             * Defines annotations to be added to the currently selected field.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to {@link Implementation}s.
             *
             * @param annotations The annotations to add to the currently selected field.
             * @return A builder where the given annotation will be added to the currently selected field.
             */
            FieldAnnotationTarget<S> annotateField(Iterable<? extends Annotation> annotations);

            /**
             * Defines annotations to be added to the currently selected field.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to {@link Implementation}s.
             *
             * @param annotation The annotations to add to the currently selected field.
             * @return A builder where the given annotation will be added to the currently selected field.
             */
            FieldAnnotationTarget<S> annotateField(AnnotationDescription... annotation);

            /**
             * Defines annotations to be added to the currently selected field.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to {@link Implementation}s.
             *
             * @param annotations The annotations to add to the currently selected field.
             * @return A builder where the given annotation will be added to the currently selected field.
             */
            FieldAnnotationTarget<S> annotateField(Collection<? extends AnnotationDescription> annotations);
        }

        /**
         * An abstract base implementation for a dynamic type builder. For representing the built type, the
         * {@link net.bytebuddy.dynamic.TargetType} class can be used as a placeholder.
         *
         * @param <S> The most specific known loaded type that is implemented by the created dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        abstract class AbstractBase<S> implements Builder<S> {

            /**
             * The class file version specified for this builder.
             */
            protected final ClassFileVersion classFileVersion;

            /**
             * The naming strategy specified for this builder.
             */
            protected final NamingStrategy namingStrategy;

            /**
             * The naming strategy for auxiliary types specified for this builder.
             */
            protected final AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

            /**
             * The target type description that is specified for this builder.
             */
            protected final TypeDescription targetType;

            /**
             * The interface types to implement as specified for this builder.
             */
            protected final List<TypeDescription> interfaceTypes;

            /**
             * The modifiers specified for this builder.
             */
            protected final int modifiers;

            /**
             * The type attribute appender specified for this builder.
             */
            protected final TypeAttributeAppender attributeAppender;

            /**
             * The method matcher for ignored method specified for this builder.
             */
            protected final ElementMatcher<? super MethodDescription> ignoredMethods;

            /**
             * The bridge method resolver factory specified for this builder.
             */
            protected final BridgeMethodResolver.Factory bridgeMethodResolverFactory;

            /**
             * The class visitor wrapper chain that is applied on created types by this builder.
             */
            protected final ClassVisitorWrapper.Chain classVisitorWrapperChain;

            /**
             * The field registry of this builder.
             */
            protected final FieldRegistry fieldRegistry;

            /**
             * The method registry of this builder.
             */
            protected final MethodRegistry methodRegistry;

            /**
             * The method lookup engine factory to be used by this builder.
             */
            protected final MethodLookupEngine.Factory methodLookupEngineFactory;

            /**
             * The default field attribute appender factory that is automatically added to any field that is
             * registered on this builder.
             */
            protected final FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory;

            /**
             * The default method attribute appender factory that is automatically added to any field method is
             * registered on this builder.
             */
            protected final MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory;

            /**
             * This builder's currently registered field tokens.
             */
            protected final List<FieldToken> fieldTokens;

            /**
             * This builder's currently registered method tokens.
             */
            protected final List<MethodToken> methodTokens;

            /**
             * Creates a new immutable type builder base implementation.
             *
             * @param classFileVersion                      The class file version for the created dynamic type.
             * @param namingStrategy                        The naming strategy for naming the dynamic type.
             * @param auxiliaryTypeNamingStrategy           The naming strategy for naming auxiliary types of the dynamic type.
             * @param targetType                            A description of the type that the dynamic type should represent.
             * @param interfaceTypes                        A list of interfaces that should be implemented by the created dynamic type.
             * @param modifiers                             The modifiers to be represented by the dynamic type.
             * @param attributeAppender                     The attribute appender to apply onto the dynamic type that is created.
             * @param ignoredMethods                        A matcher for determining methods that are to be ignored for instrumentation.
             * @param bridgeMethodResolverFactory           A factory for creating a bridge method resolver.
             * @param classVisitorWrapperChain              A chain of ASM class visitors to apply to the writing process.
             * @param fieldRegistry                         The field registry to apply to the dynamic type creation.
             * @param methodRegistry                        The method registry to apply to the dynamic type creation.
             * @param methodLookupEngineFactory             The method lookup engine factory to apply to the dynamic type creation.
             * @param defaultFieldAttributeAppenderFactory  The field attribute appender factory that should be applied by default if
             *                                              no specific appender was specified for a given field.
             * @param defaultMethodAttributeAppenderFactory The method attribute appender factory that should be applied by default
             *                                              if no specific appender was specified for a given method.
             * @param fieldTokens                           A list of field representations that were added explicitly to this
             *                                              dynamic type.
             * @param methodTokens                          A list of method representations that were added explicitly to this
             *                                              dynamic type.
             */
            protected AbstractBase(ClassFileVersion classFileVersion,
                                   NamingStrategy namingStrategy,
                                   AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                   TypeDescription targetType,
                                   List<TypeDescription> interfaceTypes,
                                   int modifiers,
                                   TypeAttributeAppender attributeAppender,
                                   ElementMatcher<? super MethodDescription> ignoredMethods,
                                   BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                                   ClassVisitorWrapper.Chain classVisitorWrapperChain,
                                   FieldRegistry fieldRegistry,
                                   MethodRegistry methodRegistry,
                                   MethodLookupEngine.Factory methodLookupEngineFactory,
                                   FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                   MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                   List<FieldToken> fieldTokens,
                                   List<MethodToken> methodTokens) {
                this.classFileVersion = classFileVersion;
                this.namingStrategy = namingStrategy;
                this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
                this.targetType = targetType;
                this.interfaceTypes = interfaceTypes;
                this.modifiers = modifiers;
                this.attributeAppender = attributeAppender;
                this.ignoredMethods = ignoredMethods;
                this.bridgeMethodResolverFactory = bridgeMethodResolverFactory;
                this.classVisitorWrapperChain = classVisitorWrapperChain;
                this.fieldRegistry = fieldRegistry;
                this.methodRegistry = methodRegistry;
                this.methodLookupEngineFactory = methodLookupEngineFactory;
                this.defaultFieldAttributeAppenderFactory = defaultFieldAttributeAppenderFactory;
                this.defaultMethodAttributeAppenderFactory = defaultMethodAttributeAppenderFactory;
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
                            methodToken.resolveExceptionTypes(instrumentedType),
                            methodToken.modifiers);
                }
                return instrumentedType;
            }

            @Override
            public OptionalMatchedMethodInterception<S> implement(Class<?>... interfaceType) {
                return implement(new TypeList.ForLoadedType(nonNull(interfaceType)));
            }

            @Override
            public OptionalMatchedMethodInterception<S> implement(Iterable<? extends Class<?>> interfaceTypes) {
                return implement(new TypeList.ForLoadedType(toList(interfaceTypes)));
            }

            @Override
            public OptionalMatchedMethodInterception<S> implement(TypeDescription... interfaceType) {
                return implement(Arrays.asList(interfaceType));
            }

            @Override
            public OptionalMatchedMethodInterception<S> implement(Collection<? extends TypeDescription> interfaceTypes) {
                return new DefaultOptionalMatchedMethodInterception(new ArrayList<TypeDescription>(isImplementable(interfaceTypes)));
            }

            @Override
            public FieldValueTarget<S> defineField(String name,
                                                   Class<?> fieldType,
                                                   ModifierContributor.ForField... modifier) {
                return defineField(name, new TypeDescription.ForLoadedType(fieldType), modifier);
            }

            @Override
            public ExceptionDeclarableMethodInterception<S> defineMethod(String name,
                                                                         Class<?> returnType,
                                                                         List<? extends Class<?>> parameterTypes,
                                                                         ModifierContributor.ForMethod... modifier) {
                return defineMethod(name,
                        new TypeDescription.ForLoadedType(returnType),
                        new TypeList.ForLoadedType(new ArrayList<Class<?>>(nonNull(parameterTypes))),
                        modifier);
            }

            @Override
            public ExceptionDeclarableMethodInterception<S> defineConstructor(Iterable<? extends Class<?>> parameterTypes,
                                                                              ModifierContributor.ForMethod... modifier) {
                return defineConstructor(new TypeList.ForLoadedType(toList(parameterTypes)), modifier);
            }

            @Override
            public Builder<S> classFileVersion(ClassFileVersion classFileVersion) {
                return materialize(nonNull(classFileVersion),
                        namingStrategy,
                        auxiliaryTypeNamingStrategy,
                        targetType,
                        interfaceTypes,
                        modifiers,
                        attributeAppender,
                        ignoredMethods,
                        bridgeMethodResolverFactory,
                        classVisitorWrapperChain,
                        fieldRegistry,
                        methodRegistry,
                        methodLookupEngineFactory,
                        defaultFieldAttributeAppenderFactory,
                        defaultMethodAttributeAppenderFactory,
                        fieldTokens,
                        methodTokens);
            }

            @Override
            public Builder<S> name(String name) {
                return materialize(classFileVersion,
                        new NamingStrategy.Fixed(isValidTypeName(name)),
                        auxiliaryTypeNamingStrategy,
                        targetType,
                        interfaceTypes,
                        modifiers,
                        attributeAppender,
                        ignoredMethods,
                        bridgeMethodResolverFactory,
                        classVisitorWrapperChain,
                        fieldRegistry,
                        methodRegistry,
                        methodLookupEngineFactory,
                        defaultFieldAttributeAppenderFactory,
                        defaultMethodAttributeAppenderFactory,
                        fieldTokens,
                        methodTokens);
            }

            @Override
            public Builder<S> name(NamingStrategy namingStrategy) {
                return materialize(classFileVersion,
                        nonNull(namingStrategy),
                        auxiliaryTypeNamingStrategy,
                        targetType,
                        interfaceTypes,
                        modifiers,
                        attributeAppender,
                        ignoredMethods,
                        bridgeMethodResolverFactory,
                        classVisitorWrapperChain,
                        fieldRegistry,
                        methodRegistry,
                        methodLookupEngineFactory,
                        defaultFieldAttributeAppenderFactory,
                        defaultMethodAttributeAppenderFactory,
                        fieldTokens,
                        methodTokens);
            }

            @Override
            public Builder<S> name(AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy) {
                return materialize(classFileVersion,
                        namingStrategy,
                        nonNull(auxiliaryTypeNamingStrategy),
                        targetType,
                        interfaceTypes,
                        modifiers,
                        attributeAppender,
                        ignoredMethods,
                        bridgeMethodResolverFactory,
                        classVisitorWrapperChain,
                        fieldRegistry,
                        methodRegistry,
                        methodLookupEngineFactory,
                        defaultFieldAttributeAppenderFactory,
                        defaultMethodAttributeAppenderFactory,
                        fieldTokens,
                        methodTokens);
            }

            @Override
            public Builder<S> modifiers(ModifierContributor.ForType... modifier) {
                return materialize(classFileVersion,
                        namingStrategy,
                        auxiliaryTypeNamingStrategy,
                        targetType,
                        interfaceTypes,
                        resolveModifierContributors(TYPE_MODIFIER_MASK, nonNull(modifier)),
                        attributeAppender,
                        ignoredMethods,
                        bridgeMethodResolverFactory,
                        classVisitorWrapperChain,
                        fieldRegistry,
                        methodRegistry,
                        methodLookupEngineFactory,
                        defaultFieldAttributeAppenderFactory,
                        defaultMethodAttributeAppenderFactory,
                        fieldTokens,
                        methodTokens);
            }

            @Override
            public Builder<S> modifiers(int modifiers) {
                return materialize(classFileVersion,
                        namingStrategy,
                        auxiliaryTypeNamingStrategy,
                        targetType,
                        interfaceTypes,
                        modifiers,
                        attributeAppender,
                        ignoredMethods,
                        bridgeMethodResolverFactory,
                        classVisitorWrapperChain,
                        fieldRegistry,
                        methodRegistry,
                        methodLookupEngineFactory,
                        defaultFieldAttributeAppenderFactory,
                        defaultMethodAttributeAppenderFactory,
                        fieldTokens,
                        methodTokens);
            }

            @Override
            public Builder<S> ignoreMethods(ElementMatcher<? super MethodDescription> ignoredMethods) {
                return materialize(classFileVersion,
                        namingStrategy,
                        auxiliaryTypeNamingStrategy,
                        targetType,
                        interfaceTypes,
                        modifiers,
                        attributeAppender,
                        new ElementMatcher.Junction.Conjunction<MethodDescription>(this.ignoredMethods,
                                nonNull(ignoredMethods)),
                        bridgeMethodResolverFactory,
                        classVisitorWrapperChain,
                        fieldRegistry,
                        methodRegistry,
                        methodLookupEngineFactory,
                        defaultFieldAttributeAppenderFactory,
                        defaultMethodAttributeAppenderFactory,
                        fieldTokens,
                        methodTokens);
            }

            @Override
            public Builder<S> attribute(TypeAttributeAppender attributeAppender) {
                return materialize(classFileVersion,
                        namingStrategy,
                        auxiliaryTypeNamingStrategy,
                        targetType,
                        interfaceTypes,
                        modifiers,
                        new TypeAttributeAppender.Compound(this.attributeAppender, nonNull(attributeAppender)),
                        ignoredMethods,
                        bridgeMethodResolverFactory,
                        classVisitorWrapperChain,
                        fieldRegistry,
                        methodRegistry,
                        methodLookupEngineFactory,
                        defaultFieldAttributeAppenderFactory,
                        defaultMethodAttributeAppenderFactory,
                        fieldTokens,
                        methodTokens);
            }

            @Override
            public Builder<S> annotateType(Annotation... annotation) {
                return annotateType((new AnnotationList.ForLoadedAnnotation(nonNull(annotation))));
            }

            @Override
            public Builder<S> annotateType(Iterable<? extends Annotation> annotations) {
                return annotateType(new AnnotationList.ForLoadedAnnotation(toList(annotations)));
            }

            @Override
            public Builder<S> annotateType(AnnotationDescription... annotation) {
                return annotateType(new AnnotationList.Explicit(Arrays.asList(nonNull(annotation))));
            }

            @Override
            public Builder<S> annotateType(Collection<? extends AnnotationDescription> annotations) {
                return attribute(new TypeAttributeAppender.ForAnnotation(new ArrayList<AnnotationDescription>(nonNull(annotations))));
            }

            @Override
            public Builder<S> classVisitor(ClassVisitorWrapper classVisitorWrapper) {
                return materialize(classFileVersion,
                        namingStrategy,
                        auxiliaryTypeNamingStrategy,
                        targetType,
                        interfaceTypes,
                        modifiers,
                        attributeAppender,
                        ignoredMethods,
                        bridgeMethodResolverFactory,
                        classVisitorWrapperChain.append(nonNull(classVisitorWrapper)),
                        fieldRegistry,
                        methodRegistry,
                        methodLookupEngineFactory,
                        defaultFieldAttributeAppenderFactory,
                        defaultMethodAttributeAppenderFactory,
                        fieldTokens,
                        methodTokens);
            }

            @Override
            public Builder<S> methodLookupEngine(MethodLookupEngine.Factory methodLookupEngineFactory) {
                return materialize(classFileVersion,
                        namingStrategy,
                        auxiliaryTypeNamingStrategy,
                        targetType,
                        interfaceTypes,
                        modifiers,
                        attributeAppender,
                        ignoredMethods,
                        bridgeMethodResolverFactory,
                        classVisitorWrapperChain,
                        fieldRegistry,
                        methodRegistry,
                        nonNull(methodLookupEngineFactory),
                        defaultFieldAttributeAppenderFactory,
                        defaultMethodAttributeAppenderFactory,
                        fieldTokens,
                        methodTokens);
            }

            @Override
            public Builder<S> bridgeMethodResolverFactory(BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
                return materialize(classFileVersion,
                        namingStrategy,
                        auxiliaryTypeNamingStrategy,
                        targetType,
                        interfaceTypes,
                        modifiers,
                        attributeAppender,
                        ignoredMethods,
                        nonNull(bridgeMethodResolverFactory),
                        classVisitorWrapperChain,
                        fieldRegistry,
                        methodRegistry,
                        methodLookupEngineFactory,
                        defaultFieldAttributeAppenderFactory,
                        defaultMethodAttributeAppenderFactory,
                        fieldTokens,
                        methodTokens);
            }

            @Override
            public FieldValueTarget<S> defineField(String name,
                                                   TypeDescription fieldType,
                                                   ModifierContributor.ForField... modifier) {
                return defineField(name,
                        fieldType,
                        resolveModifierContributors(FIELD_MODIFIER_MASK, nonNull(modifier)));
            }

            @Override
            public FieldValueTarget<S> defineField(String name,
                                                   Class<?> fieldType,
                                                   int modifiers) {
                return defineField(name,
                        new TypeDescription.ForLoadedType(nonNull(fieldType)),
                        modifiers);
            }

            @Override
            public FieldValueTarget<S> defineField(String name,
                                                   TypeDescription fieldTypeDescription,
                                                   int modifiers) {
                return new DefaultFieldValueTarget(new FieldToken(isValidIdentifier(name), isActualType(fieldTypeDescription), modifiers),
                        defaultFieldAttributeAppenderFactory);
            }

            @Override
            public FieldValueTarget<S> defineField(Field field) {
                return defineField(field.getName(), field.getType(), field.getModifiers());
            }

            @Override
            public FieldValueTarget<S> defineField(FieldDescription fieldDescription) {
                return defineField(fieldDescription.getName(), fieldDescription.getFieldType(), fieldDescription.getModifiers());
            }

            @Override
            public ExceptionDeclarableMethodInterception<S> defineMethod(String name,
                                                                         TypeDescription returnType,
                                                                         List<? extends TypeDescription> parameterTypes,
                                                                         ModifierContributor.ForMethod... modifier) {
                return defineMethod(name, returnType, parameterTypes, resolveModifierContributors(METHOD_MODIFIER_MASK, modifier));
            }

            @Override
            public ExceptionDeclarableMethodInterception<S> defineMethod(Method method) {
                return defineMethod(method.getName(), method.getReturnType(), Arrays.asList(method.getParameterTypes()), method.getModifiers());
            }

            @Override
            public ExceptionDeclarableMethodInterception<S> defineMethod(MethodDescription methodDescription) {
                if (!methodDescription.isMethod()) {
                    throw new IllegalArgumentException("Not a method: " + methodDescription);
                }
                return defineMethod(methodDescription.getName(),
                        methodDescription.getReturnType(),
                        methodDescription.getParameters().asTypeList(),
                        methodDescription.getModifiers());
            }

            @Override
            public ExceptionDeclarableMethodInterception<S> defineMethod(String name,
                                                                         Class<?> returnType,
                                                                         List<? extends Class<?>> parameterTypes,
                                                                         int modifiers) {
                return defineMethod(name,
                        new TypeDescription.ForLoadedType(nonNull(returnType)),
                        new TypeList.ForLoadedType(nonNull(parameterTypes)),
                        modifiers);
            }

            @Override
            public ExceptionDeclarableMethodInterception<S> defineMethod(String name,
                                                                         TypeDescription returnType,
                                                                         List<? extends TypeDescription> parameterTypes,
                                                                         int modifiers) {
                return new DefaultExceptionDeclarableMethodInterception(new MethodToken(isValidIdentifier(name),
                        isActualTypeOrVoid(returnType),
                        isActualType(parameterTypes),
                        Collections.<TypeDescription>emptyList(),
                        modifiers));
            }

            @Override
            public ExceptionDeclarableMethodInterception<S> defineConstructor(
                    List<? extends TypeDescription> parameterTypes,
                    ModifierContributor.ForMethod... modifier) {
                return defineConstructor(parameterTypes, resolveModifierContributors(METHOD_MODIFIER_MASK & ~Opcodes.ACC_STATIC, nonNull(modifier)));
            }

            @Override
            public ExceptionDeclarableMethodInterception<S> defineConstructor(Constructor<?> constructor) {
                return defineConstructor(Arrays.asList(constructor.getParameterTypes()),
                        constructor.getModifiers());
            }

            @Override
            public ExceptionDeclarableMethodInterception<S> defineConstructor(MethodDescription methodDescription) {
                if (!methodDescription.isConstructor()) {
                    throw new IllegalArgumentException("Not a constructor: " + methodDescription);
                }
                return defineConstructor(methodDescription.getParameters().asTypeList(), methodDescription.getModifiers());
            }

            @Override
            public ExceptionDeclarableMethodInterception<S> defineConstructor(Iterable<? extends Class<?>> parameterTypes, int modifiers) {
                return defineConstructor(new TypeList.ForLoadedType(toList(parameterTypes)), modifiers);
            }

            @Override
            public ExceptionDeclarableMethodInterception<S> defineConstructor(List<? extends TypeDescription> parameterTypes, int modifiers) {
                return new DefaultExceptionDeclarableMethodInterception(new MethodToken(isActualType(parameterTypes),
                        Collections.<TypeDescription>emptyList(),
                        modifiers));
            }

            @Override
            public ExceptionDeclarableMethodInterception<S> define(MethodDescription methodDescription) {
                return methodDescription.isMethod()
                        ? defineMethod(methodDescription)
                        : defineConstructor(methodDescription);
            }

            @Override
            public MatchedMethodInterception<S> method(ElementMatcher<? super MethodDescription> methodMatcher) {
                return invokable(isMethod().and(nonNull(methodMatcher)));
            }

            @Override
            public MatchedMethodInterception<S> constructor(ElementMatcher<? super MethodDescription> methodMatcher) {
                return invokable(isConstructor().and(nonNull(methodMatcher)));
            }

            @Override
            public MatchedMethodInterception<S> invokable(ElementMatcher<? super MethodDescription> methodMatcher) {
                return invokable(new LatentMethodMatcher.Resolved(nonNull(methodMatcher)));
            }

            @Override
            public MatchedMethodInterception<S> invokable(LatentMethodMatcher methodMatcher) {
                return new DefaultMatchedMethodInterception(nonNull(methodMatcher), methodTokens);
            }

            /**
             * Creates a new immutable type builder which represents the given arguments.
             *
             * @param classFileVersion                      The class file version for the created dynamic type.
             * @param namingStrategy                        The naming strategy for naming the dynamic type.
             * @param auxiliaryTypeNamingStrategy           The naming strategy for naming the auxiliary type of the dynamic type.
             * @param targetType                            A description of the type that the dynamic type should represent.
             * @param interfaceTypes                        A list of interfaces that should be implemented by the created dynamic type.
             * @param modifiers                             The modifiers to be represented by the dynamic type.
             * @param attributeAppender                     The attribute appender to apply onto the dynamic type that is created.
             * @param ignoredMethods                        A matcher for determining methods that are to be ignored for implementation.
             * @param bridgeMethodResolverFactory           A factory for creating a bridge method resolver.
             * @param classVisitorWrapperChain              A chain of ASM class visitors to apply to the writing process.
             * @param fieldRegistry                         The field registry to apply to the dynamic type creation.
             * @param methodRegistry                        The method registry to apply to the dynamic type creation.
             * @param methodLookupEngineFactory             The method lookup engine factory to apply to the dynamic type creation.
             * @param defaultFieldAttributeAppenderFactory  The field attribute appender factory that should be applied by default if
             *                                              no specific appender was specified for a given field.
             * @param defaultMethodAttributeAppenderFactory The method attribute appender factory that should be applied by default
             *                                              if no specific appender was specified for a given method.
             * @param fieldTokens                           A list of field representations that were added explicitly to this
             *                                              dynamic type.
             * @param methodTokens                          A list of method representations that were added explicitly to this
             *                                              dynamic type.
             * @return A dynamic type builder that represents the given arguments.
             */
            protected abstract Builder<S> materialize(ClassFileVersion classFileVersion,
                                                      NamingStrategy namingStrategy,
                                                      AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                      TypeDescription targetType,
                                                      List<TypeDescription> interfaceTypes,
                                                      int modifiers,
                                                      TypeAttributeAppender attributeAppender,
                                                      ElementMatcher<? super MethodDescription> ignoredMethods,
                                                      BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                                                      ClassVisitorWrapper.Chain classVisitorWrapperChain,
                                                      FieldRegistry fieldRegistry,
                                                      MethodRegistry methodRegistry,
                                                      MethodLookupEngine.Factory methodLookupEngineFactory,
                                                      FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                                      MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                                      List<FieldToken> fieldTokens,
                                                      List<MethodToken> methodTokens);

            @Override
            public boolean equals(Object other) {
                if (this == other)
                    return true;
                if (other == null || getClass() != other.getClass())
                    return false;
                AbstractBase that = (AbstractBase) other;
                return modifiers == that.modifiers
                        && attributeAppender.equals(that.attributeAppender)
                        && bridgeMethodResolverFactory.equals(that.bridgeMethodResolverFactory)
                        && classFileVersion.equals(that.classFileVersion)
                        && classVisitorWrapperChain.equals(that.classVisitorWrapperChain)
                        && defaultFieldAttributeAppenderFactory.equals(that.defaultFieldAttributeAppenderFactory)
                        && defaultMethodAttributeAppenderFactory.equals(that.defaultMethodAttributeAppenderFactory)
                        && fieldRegistry.equals(that.fieldRegistry)
                        && fieldTokens.equals(that.fieldTokens)
                        && ignoredMethods.equals(that.ignoredMethods)
                        && interfaceTypes.equals(that.interfaceTypes)
                        && targetType.equals(that.targetType)
                        && methodLookupEngineFactory.equals(that.methodLookupEngineFactory)
                        && methodRegistry.equals(that.methodRegistry)
                        && methodTokens.equals(that.methodTokens)
                        && namingStrategy.equals(that.namingStrategy)
                        && auxiliaryTypeNamingStrategy.equals(that.auxiliaryTypeNamingStrategy);
            }

            @Override
            public int hashCode() {
                int result = classFileVersion.hashCode();
                result = 31 * result + namingStrategy.hashCode();
                result = 31 * result + auxiliaryTypeNamingStrategy.hashCode();
                result = 31 * result + targetType.hashCode();
                result = 31 * result + interfaceTypes.hashCode();
                result = 31 * result + modifiers;
                result = 31 * result + attributeAppender.hashCode();
                result = 31 * result + ignoredMethods.hashCode();
                result = 31 * result + bridgeMethodResolverFactory.hashCode();
                result = 31 * result + classVisitorWrapperChain.hashCode();
                result = 31 * result + fieldRegistry.hashCode();
                result = 31 * result + methodRegistry.hashCode();
                result = 31 * result + methodLookupEngineFactory.hashCode();
                result = 31 * result + defaultFieldAttributeAppenderFactory.hashCode();
                result = 31 * result + defaultMethodAttributeAppenderFactory.hashCode();
                result = 31 * result + fieldTokens.hashCode();
                result = 31 * result + methodTokens.hashCode();
                return result;
            }

            /**
             * A method token representing a latent method that is defined for the built dynamic type.
             */
            protected static class MethodToken implements LatentMethodMatcher {

                /**
                 * The internal name of the method.
                 */
                protected final String internalName;

                /**
                 * A description of the return type of the method or a type describing the
                 * {@link net.bytebuddy.dynamic.TargetType} placeholder.
                 */
                protected final TypeDescription returnType;

                /**
                 * A list of parameter type descriptions for the method which might be represented by the
                 * {@link net.bytebuddy.dynamic.TargetType} placeholder.
                 */
                protected final List<TypeDescription> parameterTypes;

                /**
                 * A list of exception type descriptions for the method.
                 */
                protected final List<TypeDescription> exceptionTypes;

                /**
                 * The modifiers of the method.
                 */
                protected final int modifiers;

                /**
                 * Creates a new method token representing a constructor to implement for the built dynamic type.
                 *
                 * @param parameterTypes A list of parameters for the constructor.
                 * @param exceptionTypes A list of exception types that are declared for the constructor.
                 * @param modifiers      The modifiers of the constructor.
                 */
                public MethodToken(List<? extends TypeDescription> parameterTypes,
                                   List<? extends TypeDescription> exceptionTypes,
                                   int modifiers) {
                    this(MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                            TypeDescription.VOID,
                            parameterTypes,
                            exceptionTypes,
                            modifiers);
                }

                /**
                 * Creates a new method token representing a method to implement for the built dynamic type.
                 *
                 * @param internalName   The internal name of the method.
                 * @param returnType     The return type of the method.
                 * @param parameterTypes A list of parameters for the method.
                 * @param exceptionTypes A list of exception types that are declared for the method.
                 * @param modifiers      The modifiers of the method.
                 */
                public MethodToken(String internalName,
                                   TypeDescription returnType,
                                   List<? extends TypeDescription> parameterTypes,
                                   List<? extends TypeDescription> exceptionTypes,
                                   int modifiers) {
                    this.internalName = internalName;
                    this.returnType = returnType;
                    this.parameterTypes = Collections.unmodifiableList(new ArrayList<TypeDescription>(parameterTypes));
                    this.exceptionTypes = Collections.unmodifiableList(new ArrayList<TypeDescription>(exceptionTypes));
                    this.modifiers = modifiers;
                }

                @Override
                public ElementMatcher<? super MethodDescription> resolve(TypeDescription instrumentedType) {
                    return (MethodDescription.CONSTRUCTOR_INTERNAL_NAME.equals(internalName)
                            ? isConstructor()
                            : ElementMatchers.<MethodDescription>named(internalName))
                            .and(returns(resolveReturnType(instrumentedType)))
                            .<MethodDescription>and(takesArguments(resolveParameterTypes(instrumentedType)));
                }

                /**
                 * Resolves the return type for the method which could be represented by the
                 * {@link net.bytebuddy.dynamic.TargetType} placeholder type.
                 *
                 * @param instrumentedType The instrumented place which is used for replacement.
                 * @return A type description for the actual return type.
                 */
                protected TypeDescription resolveReturnType(TypeDescription instrumentedType) {
                    return TargetType.resolve(returnType, instrumentedType, TargetType.MATCHER);
                }

                /**
                 * Resolves the parameter types for the method which could be represented by the
                 * {@link net.bytebuddy.dynamic.TargetType} placeholder type.
                 *
                 * @param instrumentedType The instrumented place which is used for replacement.
                 * @return A list of type descriptions for the actual parameter types.
                 */
                protected List<TypeDescription> resolveParameterTypes(TypeDescription instrumentedType) {
                    return TargetType.resolve(parameterTypes, instrumentedType, TargetType.MATCHER).asRawTypes();
                }

                /**
                 * Resolves the declared exception types for the method.
                 *
                 * @param instrumentedType The instrumented place which is used for replacement.
                 * @return A list of type descriptions for the actual exception types.
                 */
                protected List<TypeDescription> resolveExceptionTypes(TypeDescription instrumentedType) {
                    return TargetType.resolve(exceptionTypes, instrumentedType, TargetType.MATCHER).asRawTypes();
                }

                /**
                 * Returns the internal name of this method token.
                 *
                 * @return The internal name of this method token.
                 */
                public String getInternalName() {
                    return internalName;
                }

                /**
                 * Returns a description of the return type of this method token.
                 *
                 * @return A description of the return type of this method token.
                 */
                public TypeDescription getReturnType() {
                    return returnType;
                }

                /**
                 * Returns a list of descriptions of the parameter types of this method token.
                 *
                 * @return A list of descriptions of the parameter types of this method token.
                 */
                public List<TypeDescription> getParameterTypes() {
                    return parameterTypes;
                }

                /**
                 * Returns a list of exception types of this method token.
                 *
                 * @return A list of exception types of this method token.
                 */
                public List<TypeDescription> getExceptionTypes() {
                    return exceptionTypes;
                }

                /**
                 * Returns the modifiers for this method token.
                 *
                 * @return The modifiers for this method token.
                 */
                public int getModifiers() {
                    return modifiers;
                }

                @Override
                public boolean equals(Object other) {
                    return (this == other || other instanceof MethodToken)
                            && internalName.equals(((MethodToken) other).getInternalName())
                            && parameterTypes.equals(((MethodToken) other).getParameterTypes())
                            && returnType.equals(((MethodToken) other).getReturnType());
                }

                @Override
                public int hashCode() {
                    int result = internalName.hashCode();
                    result = 31 * result + returnType.hashCode();
                    result = 31 * result + parameterTypes.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "DynamicType.Builder.AbstractBase.MethodToken{" +
                            "internalName='" + internalName + '\'' +
                            ", returnType=" + returnType +
                            ", parameterTypes=" + parameterTypes +
                            ", exceptionTypes=" + exceptionTypes +
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
                 * {@link net.bytebuddy.dynamic.TargetType} placeholder.
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
                 * {@link net.bytebuddy.dynamic.TargetType} placeholder type.
                 *
                 * @param instrumentedType The instrumented place which is used for replacement.
                 * @return A type description for the actual field type.
                 */
                protected TypeDescription resolveFieldType(TypeDescription instrumentedType) {
                    return TargetType.resolve(fieldType, instrumentedType, TargetType.MATCHER);
                }

                /**
                 * Returns the name of this field token.
                 *
                 * @return The name of this field token.
                 */
                public String getName() {
                    return name;
                }

                /**
                 * Returns the type of this field token.
                 *
                 * @return The type of this field token.
                 */
                public TypeDescription getFieldType() {
                    return fieldType;
                }

                /**
                 * Returns the modifiers of this field token.
                 *
                 * @return The modifiers of this field token.
                 */
                public int getModifiers() {
                    return modifiers;
                }

                @Override
                public String getFieldName() {
                    return name;
                }

                @Override
                public boolean equals(Object other) {
                    return (this == other || other instanceof FieldToken)
                            && name.equals(((FieldToken) other).getFieldName());
                }

                @Override
                public int hashCode() {
                    return name.hashCode();
                }

                @Override
                public String toString() {
                    return "DynamicType.Builder.AbstractBase.FieldToken{" +
                            "name='" + name + '\'' +
                            ", fieldType=" + fieldType +
                            ", modifiers=" + modifiers + '}';
                }
            }

            /**
             * A base implementation of a builder that is capable of manifesting a change that was not yet applied to
             * the builder.
             *
             * @param <U> The most specific known loaded type that is implemented by the created dynamic type, usually the
             *            type itself, an interface or the direct super class.
             */
            protected abstract class AbstractDelegatingBuilder<U> implements Builder<U> {

                @Override
                public Builder<U> classFileVersion(ClassFileVersion classFileVersion) {
                    return materialize().classFileVersion(classFileVersion);
                }

                @Override
                public OptionalMatchedMethodInterception<U> implement(Class<?>... interfaceType) {
                    return materialize().implement(interfaceType);
                }

                @Override
                public OptionalMatchedMethodInterception<U> implement(Iterable<? extends Class<?>> interfaceTypes) {
                    return materialize().implement(interfaceTypes);
                }

                @Override
                public OptionalMatchedMethodInterception<U> implement(TypeDescription... interfaceType) {
                    return materialize().implement(interfaceType);
                }

                @Override
                public OptionalMatchedMethodInterception<U> implement(Collection<? extends TypeDescription> typeDescriptions) {
                    return materialize().implement(typeDescriptions);
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
                public Builder<U> methodLookupEngine(MethodLookupEngine.Factory methodLookupEngineFactory) {
                    return materialize().methodLookupEngine(methodLookupEngineFactory);
                }

                @Override
                public Builder<U> bridgeMethodResolverFactory(BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
                    return materialize().bridgeMethodResolverFactory(bridgeMethodResolverFactory);
                }

                @Override
                public FieldValueTarget<U> defineField(String name,
                                                       Class<?> fieldType,
                                                       ModifierContributor.ForField... modifier) {
                    return materialize().defineField(name, fieldType, modifier);
                }

                @Override
                public FieldValueTarget<U> defineField(String name,
                                                       TypeDescription fieldTypeDescription,
                                                       ModifierContributor.ForField... modifier) {
                    return materialize().defineField(name, fieldTypeDescription, modifier);
                }

                @Override
                public FieldValueTarget<U> defineField(String name, Class<?> fieldType, int modifiers) {
                    return materialize().defineField(name, fieldType, modifiers);
                }

                @Override
                public FieldValueTarget<U> defineField(String name,
                                                       TypeDescription fieldTypeDescription,
                                                       int modifiers) {
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
                public ExceptionDeclarableMethodInterception<U> defineMethod(String name,
                                                                             Class<?> returnType,
                                                                             List<? extends Class<?>> parameterTypes,
                                                                             ModifierContributor.ForMethod... modifier) {
                    return materialize().defineMethod(name, returnType, parameterTypes, modifier);
                }

                @Override
                public ExceptionDeclarableMethodInterception<U> defineMethod(String name,
                                                                             TypeDescription returnType,
                                                                             List<? extends TypeDescription> parameterTypes,
                                                                             ModifierContributor.ForMethod... modifier) {
                    return materialize().defineMethod(name, returnType, parameterTypes, modifier);
                }

                @Override
                public ExceptionDeclarableMethodInterception<U> defineMethod(String name,
                                                                             Class<?> returnType,
                                                                             List<? extends Class<?>> parameterTypes,
                                                                             int modifiers) {
                    return materialize().defineMethod(name, returnType, parameterTypes, modifiers);
                }

                @Override
                public ExceptionDeclarableMethodInterception<U> defineMethod(String name,
                                                                             TypeDescription returnType,
                                                                             List<? extends TypeDescription> parameterTypes,
                                                                             int modifiers) {
                    return materialize().defineMethod(name, returnType, parameterTypes, modifiers);
                }

                @Override
                public ExceptionDeclarableMethodInterception<U> defineMethod(Method method) {
                    return materialize().defineMethod(method);
                }

                @Override
                public ExceptionDeclarableMethodInterception<U> defineMethod(MethodDescription methodDescription) {
                    return materialize().defineMethod(methodDescription);
                }

                @Override
                public ExceptionDeclarableMethodInterception<U> defineConstructor(Iterable<? extends Class<?>> parameterTypes,
                                                                                  ModifierContributor.ForMethod... modifier) {
                    return materialize().defineConstructor(parameterTypes, modifier);
                }

                @Override
                public ExceptionDeclarableMethodInterception<U> defineConstructor(List<? extends TypeDescription> parameterTypes,
                                                                                  ModifierContributor.ForMethod... modifier) {
                    return materialize().defineConstructor(parameterTypes, modifier);
                }

                @Override
                public ExceptionDeclarableMethodInterception<U> defineConstructor(Iterable<? extends Class<?>> parameterTypes, int modifiers) {
                    return materialize().defineConstructor(parameterTypes, modifiers);
                }

                @Override
                public ExceptionDeclarableMethodInterception<U> defineConstructor(List<? extends TypeDescription> parameterTypes, int modifiers) {
                    return materialize().defineConstructor(parameterTypes, modifiers);
                }

                @Override
                public ExceptionDeclarableMethodInterception<U> defineConstructor(Constructor<?> constructor) {
                    return materialize().defineConstructor(constructor);
                }

                @Override
                public ExceptionDeclarableMethodInterception<U> defineConstructor(MethodDescription methodDescription) {
                    return materialize().defineConstructor(methodDescription);
                }

                @Override
                public ExceptionDeclarableMethodInterception<U> define(MethodDescription methodDescription) {
                    return materialize().define(methodDescription);
                }

                @Override
                public MatchedMethodInterception<U> method(ElementMatcher<? super MethodDescription> methodMatcher) {
                    return materialize().method(methodMatcher);
                }

                @Override
                public MatchedMethodInterception<U> constructor(ElementMatcher<? super MethodDescription> methodMatcher) {
                    return materialize().constructor(methodMatcher);
                }

                @Override
                public MatchedMethodInterception<U> invokable(ElementMatcher<? super MethodDescription> methodMatcher) {
                    return materialize().invokable(methodMatcher);
                }

                @Override
                public MatchedMethodInterception<U> invokable(LatentMethodMatcher methodMatcher) {
                    return materialize().invokable(methodMatcher);
                }

                @Override
                public Unloaded<U> make() {
                    return materialize().make();
                }

                /**
                 * Materializes the current state of the build before applying another modification.
                 *
                 * @return A builder with all pending changes materialized.
                 */
                protected abstract Builder<U> materialize();
            }

            /**
             * A {@link net.bytebuddy.dynamic.DynamicType.Builder} for which a field was recently defined such that attributes
             * can be added to this recently defined field.
             */
            protected class DefaultFieldValueTarget extends AbstractDelegatingBuilder<S> implements FieldValueTarget<S> {

                /**
                 * Representations of {@code boolean} values as JVM integers.
                 */
                private static final int NUMERIC_BOOLEAN_TRUE = 1, NUMERIC_BOOLEAN_FALSE = 0;

                /**
                 * A token representing the field that was recently defined.
                 */
                private final FieldToken fieldToken;

                /**
                 * The attribute appender factory that was defined for this field token.
                 */
                private final FieldAttributeAppender.Factory attributeAppenderFactory;

                /**
                 * The default value that is to be defined for the recently defined field or {@code null} if no such
                 * value is to be defined. Default values must only be defined for {@code static} fields of primitive types
                 * or of the {@link java.lang.String} type.
                 */
                private final Object defaultValue;

                /**
                 * Creates a new subclass field annotation target for a field without a default value.
                 *
                 * @param fieldToken               A token representing the field that was recently defined.
                 * @param attributeAppenderFactory The attribute appender factory that was defined for this field token.
                 */
                private DefaultFieldValueTarget(FieldToken fieldToken,
                                                FieldAttributeAppender.Factory attributeAppenderFactory) {
                    this(fieldToken, attributeAppenderFactory, null);
                }

                /**
                 * Creates a new subclass field annotation target.
                 *
                 * @param fieldToken               A token representing the field that was recently defined.
                 * @param attributeAppenderFactory The attribute appender factory that was defined for this field token.
                 * @param defaultValue             The default value to define for the recently defined field.
                 */
                private DefaultFieldValueTarget(FieldToken fieldToken,
                                                FieldAttributeAppender.Factory attributeAppenderFactory,
                                                Object defaultValue) {
                    this.fieldToken = fieldToken;
                    this.attributeAppenderFactory = attributeAppenderFactory;
                    this.defaultValue = defaultValue;
                }

                @Override
                protected DynamicType.Builder<S> materialize() {
                    return AbstractBase.this.materialize(classFileVersion,
                            namingStrategy,
                            auxiliaryTypeNamingStrategy,
                            targetType,
                            interfaceTypes,
                            modifiers,
                            attributeAppender,
                            ignoredMethods,
                            bridgeMethodResolverFactory,
                            classVisitorWrapperChain,
                            fieldRegistry.include(fieldToken, attributeAppenderFactory, defaultValue),
                            methodRegistry,
                            methodLookupEngineFactory,
                            defaultFieldAttributeAppenderFactory,
                            defaultMethodAttributeAppenderFactory,
                            join(fieldTokens, fieldToken),
                            methodTokens);
                }

                @Override
                public FieldAnnotationTarget<S> value(boolean value) {
                    return value(value ? NUMERIC_BOOLEAN_TRUE : NUMERIC_BOOLEAN_FALSE);
                }

                @Override
                public FieldAnnotationTarget<S> value(int value) {
                    return makeFieldAnnotationTarget(
                            NumericRangeValidator.of(fieldToken.getFieldType()).validate(value));
                }

                @Override
                public FieldAnnotationTarget<S> value(long value) {
                    return makeFieldAnnotationTarget(isValid(value, long.class));
                }

                @Override
                public FieldAnnotationTarget<S> value(float value) {
                    return makeFieldAnnotationTarget(isValid(value, float.class));
                }

                @Override
                public FieldAnnotationTarget<S> value(double value) {
                    return makeFieldAnnotationTarget(isValid(value, double.class));
                }

                @Override
                public FieldAnnotationTarget<S> value(String value) {
                    return makeFieldAnnotationTarget(isValid(value, String.class));
                }

                /**
                 * Asserts the field's type to be of a given legal types.
                 *
                 * @param defaultValue The default value to define for the recently defined field.
                 * @param legalType    The type of which this default value needs to be in order to be legal.
                 * @return The given default value.
                 */
                private Object isValid(Object defaultValue, Class<?> legalType) {
                    if (fieldToken.getFieldType().represents(legalType)) {
                        return defaultValue;
                    } else {
                        throw new IllegalStateException(
                                String.format("The given value %s was not of the required type %s",
                                        defaultValue, legalType));
                    }
                }

                /**
                 * Creates a field annotation target for the given default value.
                 *
                 * @param defaultValue The default value to define for the recently defined field.
                 * @return The resulting field annotation target.
                 */
                private FieldAnnotationTarget<S> makeFieldAnnotationTarget(Object defaultValue) {
                    if ((fieldToken.getModifiers() & Opcodes.ACC_STATIC) == 0) {
                        throw new IllegalStateException("Default field values can only be set for static fields");
                    }
                    return new DefaultFieldValueTarget(fieldToken, attributeAppenderFactory, defaultValue);
                }

                @Override
                public FieldAnnotationTarget<S> attribute(FieldAttributeAppender.Factory attributeAppenderFactory) {
                    return new DefaultFieldValueTarget(fieldToken,
                            new FieldAttributeAppender.Factory.Compound(this.attributeAppenderFactory,
                                    nonNull(attributeAppenderFactory)));
                }

                @Override
                public FieldAnnotationTarget<S> annotateField(Annotation... annotation) {
                    return annotateField((new AnnotationList.ForLoadedAnnotation(nonNull(annotation))));
                }

                @Override
                public FieldAnnotationTarget<S> annotateField(Iterable<? extends Annotation> annotations) {
                    return annotateField(new AnnotationList.ForLoadedAnnotation(toList(annotations)));
                }

                @Override
                public FieldAnnotationTarget<S> annotateField(AnnotationDescription... annotation) {
                    return annotateField(Arrays.asList(nonNull(annotation)));
                }

                @Override
                public FieldAnnotationTarget<S> annotateField(Collection<? extends AnnotationDescription> annotations) {
                    return attribute(new FieldAttributeAppender.ForAnnotation(new ArrayList<AnnotationDescription>(nonNull(annotations))));
                }

                @Override
                @SuppressWarnings("unchecked")
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    DefaultFieldValueTarget that = (DefaultFieldValueTarget) other;
                    return attributeAppenderFactory.equals(that.attributeAppenderFactory)
                            && !(defaultValue != null ?
                            !defaultValue.equals(that.defaultValue) :
                            that.defaultValue != null)
                            && fieldToken.equals(that.fieldToken)
                            && AbstractBase.this.equals(that.getDynamicTypeBuilder());
                }

                @Override
                public int hashCode() {
                    int result = fieldToken.hashCode();
                    result = 31 * result + attributeAppenderFactory.hashCode();
                    result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
                    result = 31 * result + AbstractBase.this.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "DynamicType.Builder.AbstractBase.DefaultFieldValueTarget{" +
                            "base=" + AbstractBase.this +
                            ", fieldToken=" + fieldToken +
                            ", attributeAppenderFactory=" + attributeAppenderFactory +
                            ", defaultValue=" + defaultValue +
                            '}';
                }

                /**
                 * Returns the outer instance.
                 *
                 * @return The outer instance.
                 */
                private Builder<?> getDynamicTypeBuilder() {
                    return AbstractBase.this;
                }
            }

            /**
             * A {@link net.bytebuddy.dynamic.DynamicType.Builder.MatchedMethodInterception} for which a method was recently
             * identified or defined such that an {@link Implementation} for these methods can
             * now be defined.
             */
            protected class DefaultMatchedMethodInterception implements MatchedMethodInterception<S> {

                /**
                 * The latent method matcher that identifies this interception.
                 */
                private final LatentMethodMatcher methodMatcher;

                /**
                 * A list of all method tokens that were previously defined.
                 */
                private final List<MethodToken> methodTokens;

                /**
                 * Creates a new instance of a default matched method interception.
                 *
                 * @param methodMatcher The latent method matcher that identifies this interception.
                 * @param methodTokens  A list of all method tokens that were previously defined.
                 */
                protected DefaultMatchedMethodInterception(LatentMethodMatcher methodMatcher,
                                                           List<MethodToken> methodTokens) {
                    this.methodMatcher = methodMatcher;
                    this.methodTokens = methodTokens;
                }

                @Override
                public MethodAnnotationTarget<S> intercept(Implementation implementation) {
                    return new DefaultMethodAnnotationTarget(methodTokens,
                            methodMatcher,
                            new MethodRegistry.Handler.ForImplementation(nonNull(implementation)),
                            defaultMethodAttributeAppenderFactory);
                }

                @Override
                public MethodAnnotationTarget<S> withoutCode() {
                    return new DefaultMethodAnnotationTarget(methodTokens,
                            methodMatcher,
                            MethodRegistry.Handler.ForAbstractMethod.INSTANCE,
                            defaultMethodAttributeAppenderFactory);
                }

                @Override
                public MethodAnnotationTarget<S> withDefaultValue(Object value, Class<?> type) {
                    return withDefaultValue(AnnotationDescription.ForLoadedAnnotation.describe(nonNull(value), new TypeDescription.ForLoadedType(nonNull(type))));
                }

                @Override
                public MethodAnnotationTarget<S> withDefaultValue(Object value) {
                    return new DefaultMethodAnnotationTarget(methodTokens,
                            methodMatcher,
                            MethodRegistry.Handler.ForAnnotationValue.of(value),
                            MethodAttributeAppender.NoOp.INSTANCE);
                }

                @Override
                @SuppressWarnings("unchecked")
                public boolean equals(Object other) {
                    if (this == other)
                        return true;
                    if (other == null || getClass() != other.getClass())
                        return false;
                    DefaultMatchedMethodInterception that = (DefaultMatchedMethodInterception) other;
                    return methodMatcher.equals(that.methodMatcher)
                            && methodTokens.equals(that.methodTokens)
                            && AbstractBase.this.equals(that.getDynamicTypeBuilder());
                }

                @Override
                public int hashCode() {
                    int result = methodMatcher.hashCode();
                    result = 31 * result + methodTokens.hashCode();
                    result = 31 * result + AbstractBase.this.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "DynamicType.Builder.AbstractBase.DefaultMatchedMethodInterception{" +
                            "base=" + AbstractBase.this +
                            ", methodMatcher=" + methodMatcher +
                            ", methodTokens=" + methodTokens +
                            '}';
                }

                /**
                 * Returns the outer instance.
                 *
                 * @return The outer instance.
                 */
                private Builder<?> getDynamicTypeBuilder() {
                    return AbstractBase.this;
                }
            }

            /**
             * A {@link net.bytebuddy.dynamic.DynamicType.Builder.ExceptionDeclarableMethodInterception} which allows the
             * definition of exceptions for a recently defined method.
             */
            protected class DefaultExceptionDeclarableMethodInterception implements ExceptionDeclarableMethodInterception<S> {

                /**
                 * The method token for which exceptions can be defined additionally.
                 */
                private final MethodToken methodToken;

                /**
                 * Creates a new subclass exception declarable method interception.
                 *
                 * @param methodToken The method token to define on the currently constructed method.
                 */
                private DefaultExceptionDeclarableMethodInterception(MethodToken methodToken) {
                    this.methodToken = methodToken;
                }

                @Override
                public MatchedMethodInterception<S> throwing(Class<?>... exceptionType) {
                    return throwing(new TypeList.ForLoadedType(nonNull(exceptionType)));
                }

                @Override
                public MatchedMethodInterception<S> throwing(Iterable<? extends Class<?>> exceptionTypes) {
                    return throwing(new TypeList.ForLoadedType(toList(exceptionTypes)));
                }

                @Override
                public MatchedMethodInterception<S> throwing(TypeDescription... exceptionType) {
                    return throwing(Arrays.asList(nonNull(exceptionType)));
                }

                @Override
                public MatchedMethodInterception<S> throwing(Collection<? extends TypeDescription> exceptionTypes) {
                    return materialize(new MethodToken(methodToken.getInternalName(),
                            methodToken.getReturnType(),
                            methodToken.getParameterTypes(),
                            uniqueRaw(isThrowable(new ArrayList<TypeDescription>(exceptionTypes))),
                            methodToken.getModifiers()));
                }

                @Override
                public MethodAnnotationTarget<S> intercept(Implementation implementation) {
                    return materialize(methodToken).intercept(implementation);
                }

                @Override
                public MethodAnnotationTarget<S> withoutCode() {
                    return materialize(methodToken).withoutCode();
                }

                @Override
                public MethodAnnotationTarget<S> withDefaultValue(Object value, Class<?> type) {
                    return materialize(methodToken).withDefaultValue(value, type);
                }

                @Override
                public MethodAnnotationTarget<S> withDefaultValue(Object value) {
                    return materialize(methodToken).withDefaultValue(value);
                }

                /**
                 * Materializes the given method definition and returns an instance for defining an implementation.
                 *
                 * @param methodToken The method token to define on the currently constructed type.
                 * @return A subclass matched method interception that represents the materialized method.
                 */
                private DefaultMatchedMethodInterception materialize(MethodToken methodToken) {
                    return new DefaultMatchedMethodInterception(methodToken, join(methodTokens, methodToken));
                }

                @Override
                @SuppressWarnings("unchecked")
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && methodToken.equals(((DefaultExceptionDeclarableMethodInterception) other).methodToken)
                            && AbstractBase.this
                            .equals(((DefaultExceptionDeclarableMethodInterception) other).getDynamicTypeBuilder());
                }

                @Override
                public int hashCode() {
                    return 31 * AbstractBase.this.hashCode() + methodToken.hashCode();
                }

                @Override
                public String toString() {
                    return "DynamicType.Builder.AbstractBase.DefaultExceptionDeclarableMethodInterception{" +
                            "base=" + AbstractBase.this +
                            ", methodToken=" + methodToken +
                            '}';
                }

                /**
                 * Returns the outer instance.
                 *
                 * @return The outer instance.
                 */
                private Builder<?> getDynamicTypeBuilder() {
                    return AbstractBase.this;
                }
            }

            /**
             * A {@link net.bytebuddy.dynamic.DynamicType.Builder.MethodAnnotationTarget} which allows the definition of
             * annotations for a recently identified method.
             */
            protected class DefaultMethodAnnotationTarget extends AbstractDelegatingBuilder<S> implements MethodAnnotationTarget<S> {

                /**
                 * A list of all method tokens that were previously defined.
                 */
                private final List<MethodToken> methodTokens;

                /**
                 * A matcher that allows to identify the methods to be intercepted.
                 */
                private final LatentMethodMatcher methodMatcher;

                /**
                 * The handler to apply to any matched method.
                 */
                private final MethodRegistry.Handler handler;

                /**
                 * The method attribute appender factory to be applied to the matched methods.
                 */
                private final MethodAttributeAppender.Factory attributeAppenderFactory;

                /**
                 * Creates a new default method annotation target.
                 *
                 * @param methodTokens             A list of all method tokens that were previously defined.
                 * @param methodMatcher            A matcher that allows to identify the methods to be intercepted.
                 * @param handler                  The handler to apply to any matched method.
                 * @param attributeAppenderFactory The method attribute appender factory to be applied to the matched methods.
                 */
                protected DefaultMethodAnnotationTarget(List<MethodToken> methodTokens,
                                                        LatentMethodMatcher methodMatcher,
                                                        MethodRegistry.Handler handler,
                                                        MethodAttributeAppender.Factory attributeAppenderFactory) {
                    this.methodMatcher = methodMatcher;
                    this.methodTokens = methodTokens;
                    this.handler = handler;
                    this.attributeAppenderFactory = attributeAppenderFactory;
                }

                @Override
                protected DynamicType.Builder<S> materialize() {
                    return AbstractBase.this.materialize(classFileVersion,
                            namingStrategy,
                            auxiliaryTypeNamingStrategy,
                            targetType,
                            interfaceTypes,
                            modifiers,
                            attributeAppender,
                            ignoredMethods,
                            bridgeMethodResolverFactory,
                            classVisitorWrapperChain,
                            fieldRegistry,
                            methodRegistry.prepend(methodMatcher, handler, attributeAppenderFactory),
                            methodLookupEngineFactory,
                            defaultFieldAttributeAppenderFactory,
                            defaultMethodAttributeAppenderFactory,
                            fieldTokens,
                            methodTokens);
                }

                @Override
                public MethodAnnotationTarget<S> attribute(MethodAttributeAppender.Factory attributeAppenderFactory) {
                    return new DefaultMethodAnnotationTarget(methodTokens,
                            methodMatcher,
                            handler,
                            new MethodAttributeAppender.Factory.Compound(this.attributeAppenderFactory,
                                    nonNull(attributeAppenderFactory)));
                }

                @Override
                public MethodAnnotationTarget<S> annotateMethod(Annotation... annotation) {
                    return annotateMethod((new AnnotationList.ForLoadedAnnotation(nonNull(annotation))));
                }

                @Override
                public MethodAnnotationTarget<S> annotateMethod(Iterable<? extends Annotation> annotations) {
                    return annotateMethod(new AnnotationList.ForLoadedAnnotation(toList(annotations)));
                }

                @Override
                public MethodAnnotationTarget<S> annotateMethod(AnnotationDescription... annotation) {
                    return annotateMethod(Arrays.asList(nonNull(annotation)));
                }

                @Override
                public MethodAnnotationTarget<S> annotateMethod(Collection<? extends AnnotationDescription> annotations) {
                    return attribute(new MethodAttributeAppender.ForAnnotation((nonNull(new ArrayList<AnnotationDescription>(annotations)))));
                }

                @Override
                public MethodAnnotationTarget<S> annotateParameter(int parameterIndex, Annotation... annotation) {
                    return annotateParameter(parameterIndex, new AnnotationList.ForLoadedAnnotation(nonNull(annotation)));
                }

                @Override
                public MethodAnnotationTarget<S> annotateParameter(int parameterIndex, Iterable<? extends Annotation> annotations) {
                    return annotateParameter(parameterIndex, new AnnotationList.ForLoadedAnnotation(toList(annotations)));
                }

                @Override
                public MethodAnnotationTarget<S> annotateParameter(int parameterIndex, AnnotationDescription... annotation) {
                    return annotateParameter(parameterIndex, Arrays.asList(nonNull(annotation)));
                }

                @Override
                public MethodAnnotationTarget<S> annotateParameter(int parameterIndex, Collection<? extends AnnotationDescription> annotations) {
                    return attribute(new MethodAttributeAppender.ForAnnotation(parameterIndex, nonNull(new ArrayList<AnnotationDescription>(annotations))));
                }

                @Override
                @SuppressWarnings("unchecked")
                public boolean equals(Object other) {
                    if (this == other)
                        return true;
                    if (other == null || getClass() != other.getClass())
                        return false;
                    DefaultMethodAnnotationTarget that = (DefaultMethodAnnotationTarget) other;
                    return attributeAppenderFactory.equals(that.attributeAppenderFactory)
                            && handler.equals(that.handler)
                            && methodMatcher.equals(that.methodMatcher)
                            && methodTokens.equals(that.methodTokens)
                            && AbstractBase.this.equals(that.getDynamicTypeBuilder());
                }

                @Override
                public int hashCode() {
                    int result = methodTokens.hashCode();
                    result = 31 * result + methodMatcher.hashCode();
                    result = 31 * result + handler.hashCode();
                    result = 31 * result + attributeAppenderFactory.hashCode();
                    result = 31 * result + AbstractBase.this.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "DynamicType.Builder.AbstractBase.DefaultMethodAnnotationTarget{" +
                            "base=" + AbstractBase.this +
                            ", methodTokens=" + methodTokens +
                            ", methodMatcher=" + methodMatcher +
                            ", handler=" + handler +
                            ", attributeAppenderFactory=" + attributeAppenderFactory +
                            '}';
                }

                /**
                 * Returns the outer instance.
                 *
                 * @return The outer instance.
                 */
                private Builder<?> getDynamicTypeBuilder() {
                    return AbstractBase.this;
                }
            }

            /**
             * Allows for the direct implementation of an interface after its implementation was specified.
             */
            protected class DefaultOptionalMatchedMethodInterception extends AbstractDelegatingBuilder<S> implements OptionalMatchedMethodInterception<S> {

                /**
                 * A list of all interfaces to implement.
                 */
                private List<TypeDescription> additionalInterfaceTypes;

                /**
                 * Creates a new subclass optional matched method interception.
                 *
                 * @param interfaceTypes An array of all interfaces to implement.
                 */
                protected DefaultOptionalMatchedMethodInterception(List<TypeDescription> interfaceTypes) {
                    additionalInterfaceTypes = interfaceTypes;
                }

                @Override
                public MethodAnnotationTarget<S> intercept(Implementation implementation) {
                    return materialize().method(isDeclaredBy(anyOf(additionalInterfaceTypes))).intercept(nonNull(implementation));
                }

                @Override
                public MethodAnnotationTarget<S> withoutCode() {
                    return materialize().method(isDeclaredBy(anyOf(additionalInterfaceTypes))).withoutCode();
                }

                @Override
                public MethodAnnotationTarget<S> withDefaultValue(Object value, Class<?> type) {
                    return materialize().method(isDeclaredBy(anyOf(additionalInterfaceTypes))).withDefaultValue(value, type);
                }

                @Override
                public MethodAnnotationTarget<S> withDefaultValue(Object value) {
                    return materialize().method(isDeclaredBy(anyOf(additionalInterfaceTypes))).withDefaultValue(value);
                }

                @Override
                protected DynamicType.Builder<S> materialize() {
                    return AbstractBase.this.materialize(classFileVersion,
                            namingStrategy,
                            auxiliaryTypeNamingStrategy,
                            targetType,
                            joinUniqueRaw(interfaceTypes, additionalInterfaceTypes),
                            modifiers,
                            attributeAppender,
                            ignoredMethods,
                            bridgeMethodResolverFactory,
                            classVisitorWrapperChain,
                            fieldRegistry,
                            methodRegistry,
                            methodLookupEngineFactory,
                            defaultFieldAttributeAppenderFactory,
                            defaultMethodAttributeAppenderFactory,
                            fieldTokens,
                            methodTokens);
                }

                /**
                 * Returns the outer instance.
                 *
                 * @return The outer instance.
                 */
                private DynamicType.Builder<?> getDynamicTypeBuilder() {
                    return AbstractBase.this;
                }

                @Override
                @SuppressWarnings("unchecked")
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    DefaultOptionalMatchedMethodInterception that = (DefaultOptionalMatchedMethodInterception) other;
                    return additionalInterfaceTypes.equals(that.additionalInterfaceTypes)
                            && AbstractBase.this.equals(that.getDynamicTypeBuilder());
                }

                @Override
                public int hashCode() {
                    return 31 * AbstractBase.this.hashCode() + additionalInterfaceTypes.hashCode();
                }

                @Override
                public String toString() {
                    return "DynamicType.Builder.AbstractBase.DefaultOptionalMatchedMethodInterception{" +
                            "base=" + AbstractBase.this +
                            "additionalInterfaceTypes=" + additionalInterfaceTypes +
                            '}';
                }
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
