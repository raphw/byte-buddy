/*
 * Copyright 2014 - 2020 Rafael Winterhalter
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
package net.bytebuddy.dynamic;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.modifier.*;
import net.bytebuddy.description.type.*;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.InjectionClassLoader;
import net.bytebuddy.dynamic.scaffold.*;
import net.bytebuddy.implementation.*;
import net.bytebuddy.implementation.attribute.*;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.CompoundList;
import org.objectweb.asm.Opcodes;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.jar.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

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
    Map<TypeDescription, byte[]> getAuxiliaryTypes();

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
     * is overridden during injection. The resulting jar is going to be a recreation of the original jar and not a
     * patched version with a new central directory. No directory entries are added to the generated jar.
     *
     * @param sourceJar The original jar file.
     * @param targetJar The {@code source} jar file with the injected contents.
     * @return The {@code target} jar file.
     * @throws IOException If an I/O exception occurs while injecting from the source into the target.
     */
    File inject(File sourceJar, File targetJar) throws IOException;

    /**
     * Injects the types of this dynamic type into a given <i>jar</i> file. Any pre-existent type with the same name
     * is overridden during injection. The resulting jar is going to be a recreation of the original jar and not a
     * patched version with a new central directory. No directory entries are added to the generated jar.
     *
     * @param jar The jar file to replace with an injected version.
     * @return The {@code jar} file.
     * @throws IOException If an I/O exception occurs while injecting into the jar.
     */
    File inject(File jar) throws IOException;

    /**
     * Saves the contents of this dynamic type inside a <i>jar</i> file. The folder of the given {@code file} must
     * exist prior to calling this method. The jar file is created with a simple manifest that only contains a version
     * number. No directory entries are added to the generated jar.
     *
     * @param file The target file to which the <i>jar</i> is written to.
     * @return The given {@code file}.
     * @throws IOException If an I/O exception occurs while writing the file.
     */
    File toJar(File file) throws IOException;

    /**
     * Saves the contents of this dynamic type inside a <i>jar</i> file. The folder of the given {@code file} must
     * exist prior to calling this method. No directory entries are added to the generated jar.
     *
     * @param file     The target file to which the <i>jar</i> is written to.
     * @param manifest The manifest of the created <i>jar</i>.
     * @return The given {@code file}.
     * @throws IOException If an I/O exception occurs while writing the file.
     */
    File toJar(File file, Manifest manifest) throws IOException;

    /**
     * A builder for creating a dynamic type.
     *
     * @param <T> A loaded type that the built type is guaranteed to be a subclass of.
     */
    interface Builder<T> {

        /**
         * Applies the supplied {@link AsmVisitorWrapper} onto the {@link org.objectweb.asm.ClassVisitor} during building a dynamic type.
         * Using an ASM visitor, it is possible to manipulate byte code directly. Byte Buddy does not validate directly created byte code
         * and it remains the responsibility of the visitor's implementor to generate legal byte code. If several ASM visitor wrappers
         * are registered, they are applied on top of another in their registration order.
         *
         * @param asmVisitorWrapper The ASM visitor wrapper to apply during
         * @return A new builder that is equal to this builder and applies the ASM visitor wrapper.
         */
        Builder<T> visit(AsmVisitorWrapper asmVisitorWrapper);

        /**
         * Names the dynamic type by the supplied name. The name needs to be fully qualified and in the binary format (packages separated
         * by dots: {@code foo.Bar}). A type's package determines what other types are visible to the instrumented type and what methods
         * can be overridden or be represented in method signatures or as field types.
         *
         * @param name The fully qualified name of the generated class in a binary format.
         * @return A new builder that is equal to this builder but with the instrumented type named by the supplied name.
         */
        Builder<T> name(String name);

        /**
         * Adds a suffix to the current type name without changing the type's package.
         *
         * @param suffix The suffix to append to the current type name.
         * @return A new builder that is equal to this builder but with the instrumented type named suffixed by the supplied suffix.
         */
        Builder<T> suffix(String suffix);

        /**
         * Defines the supplied modifiers as the modifiers of the instrumented type.
         *
         * @param modifierContributor The modifiers of the instrumented type.
         * @return A new builder that is equal to this builder but with the supplied modifiers applied onto the instrumented type.
         */
        Builder<T> modifiers(ModifierContributor.ForType... modifierContributor);

        /**
         * Defines the supplied modifiers as the modifiers of the instrumented type.
         *
         * @param modifierContributors The modifiers of the instrumented type.
         * @return A new builder that is equal to this builder but with the supplied modifiers applied onto the instrumented type.
         */
        Builder<T> modifiers(Collection<? extends ModifierContributor.ForType> modifierContributors);

        /**
         * Defines the supplied modifiers as the modifiers of the instrumented type.
         *
         * @param modifiers The modifiers of the instrumented type.
         * @return A new builder that is equal to this builder but with the supplied modifiers applied onto the instrumented type.
         */
        Builder<T> modifiers(int modifiers);

        /**
         * Merges the supplied modifier contributors with the modifiers of the instrumented type and defines them as the instrumented
         * type's new modifiers.
         *
         * @param modifierContributor The modifiers of the instrumented type.
         * @return A new builder that is equal to this builder but with the supplied modifiers merged into the instrumented type's modifiers.
         */
        Builder<T> merge(ModifierContributor.ForType... modifierContributor);

        /**
         * Merges the supplied modifier contributors with the modifiers of the instrumented type and defines them as the instrumented
         * type's new modifiers.
         *
         * @param modifierContributors The modifiers of the instrumented type.
         * @return A new builder that is equal to this builder but with the supplied modifiers merged into the instrumented type's modifiers.
         */
        Builder<T> merge(Collection<? extends ModifierContributor.ForType> modifierContributors);

        /**
         * <p>
         * Defines this type as a top-level type that is not declared by another type or enclosed by another member.
         * </p>
         * <p>
         * <b>Important</b>: Changing the declaration hierarchy of a type has no influence on the nest mate hierarchy.
         * </p>
         * <p>
         * <b>Warning</b>: By changing this type's declaration, any other type will not change its declaration of enclosing members or
         * declared types about any nesting of a declaration. It is the responsibility of the user of this API to keep such declarations
         * consistent among the definitions of connected types.
         * </p>
         *
         * @return A new builder that is equal to this builder but without any declaration of a a declared or enclosed type.
         */
        Builder<T> topLevelType();

        /**
         * <p>
         * Defines this type as an inner type of the supplied type. Without any additional configuration, the type declaration is defined
         * as a local type.
         * </p>
         * <p>
         * <b>Important</b>: Changing the declaration hierarchy of a type has no influence on the nest mate hierarchy.
         * </p>
         * <p>
         * <b>Warning</b>: By changing this type's declaration, any other type will not change its declaration of enclosing members or
         * declared types about any nesting of a declaration. It is the responsibility of the user of this API to keep such declarations
         * consistent among the definitions of connected types.
         * </p>
         *
         * @param type The type to declare as the built type's outer type.
         * @return A new builder that is equal to this builder with the supplied type as the built type's outer type.
         */
        InnerTypeDefinition.ForType<T> innerTypeOf(Class<?> type);

        /**
         * <p>
         * Defines this type as an inner type of the supplied type. Without any additional configuration, the type declaration is
         * defined as a local type.
         * </p>
         * <p>
         * <b>Important</b>: Changing the declaration hierarchy of a type has no influence on the nest mate hierarchy.
         * </p>
         * <p>
         * <b>Warning</b>: By changing this type's declaration, any other type will not change its declaration of enclosing members or
         * declared types about any nesting of a declaration. It is the responsibility of the user of this API to keep such declarations
         * consistent among the definitions of connected types.
         * </p>
         *
         * @param type The type to declare as the built type's outer type.
         * @return A new builder that is equal to this builder with the supplied type as the built type's outer type.
         */
        InnerTypeDefinition.ForType<T> innerTypeOf(TypeDescription type);

        /**
         * <p>
         * Defines this type as an inner type that was declared within the supplied method.  Without any additional configuration, the type
         * declaration is defined as a local type.
         * </p>
         * <p>
         * <b>Important</b>: Changing the declaration hierarchy of a type has no influence on the nest mate hierarchy.
         * </p>
         * <p>
         * <b>Warning</b>: By changing this type's declaration, any other type will not change its declaration of enclosing members or
         * declared types about any nesting of a declaration. It is the responsibility of the user of this API to keep such declarations
         * consistent among the definitions of connected types.
         * </p>
         *
         * @param method The method to declare as the built type's declaring method.
         * @return A new builder that is equal to this builder with the supplied method as the built type's declaring method.
         */
        InnerTypeDefinition<T> innerTypeOf(Method method);

        /**
         * <p>
         * Defines this type as an inner type that was declared within the supplied constructor. Without any additional configuration, the type
         * declaration is defined as a local type.
         * </p>
         * <p>
         * <b>Important</b>: Changing the declaration hierarchy of a type has no influence on the nest mate hierarchy.
         * </p>
         * <p>
         * <b>Warning</b>: By changing this type's declaration, any other type will not change its declaration of enclosing members or
         * declared types about any nesting of a declaration. It is the responsibility of the user of this API to keep such declarations
         * consistent among the definitions of connected types.
         * </p>
         *
         * @param constructor The constructor to declare as the built type's declaring method.
         * @return A new builder that is equal to this builder with the supplied method as the built type's declaring constructor.
         */
        InnerTypeDefinition<T> innerTypeOf(Constructor<?> constructor);

        /**
         * <p>
         * Defines this type as an inner type that was declared within the supplied method or constructor. Without any additional configuration,
         * the type declaration is defined as a local type.
         * </p>
         * <p>
         * <b>Important</b>: Changing the declaration hierarchy of a type has no influence on the nest mate hierarchy.
         * </p>
         * <p>
         * <b>Warning</b>: By changing this type's declaration, any other type will not change its declaration of enclosing members or
         * declared types about any nesting of a declaration. It is the responsibility of the user of this API to keep such declarations
         * consistent among the definitions of connected types.
         * </p>
         *
         * @param methodDescription The method or constructor to declare as the built type's declaring method.
         * @return A new builder that is equal to this builder with the supplied method as the built type's declaring method or constructor.
         */
        InnerTypeDefinition<T> innerTypeOf(MethodDescription.InDefinedShape methodDescription);

        /**
         * <p>
         * Defines this type as an the outer type of the supplied types. Using this method, it is possible to add inner type declarations
         * for anonymous or local types which are not normally exposed by type descriptions. Doing so, it is however possible to indicate to
         * Byte Buddy that the required attributes for such an inner type declaration should be added to a class file.
         * </p>
         * <p>
         * <b>Important</b>: Changing the declaration hierarchy of a type has no influence on the nest mate hierarchy.
         * </p>
         * <p>
         * <b>Warning</b>: By changing this type's declaration, any other type will not change its declaration of enclosing members or
         * declared types about any nesting of a declaration. It is the responsibility of the user of this API to keep such declarations
         * consistent among the definitions of connected types.
         * </p>
         *
         * @param type The types being declared.
         * @return A new builder that is equal to this builder with the supplied types being declared by the built type.
         */
        Builder<T> declaredTypes(Class<?>... type);

        /**
         * <p>
         * Defines this type as an the outer type of the supplied types. Using this method, it is possible to add inner type declarations
         * for anonymous or local types which are not normally exposed by type descriptions. Doing so, it is however possible to indicate to
         * Byte Buddy that the required attributes for such an inner type declaration should be added to a class file.
         * </p>
         * <p>
         * <b>Important</b>: Changing the declaration hierarchy of a type has no influence on the nest mate hierarchy.
         * </p>
         * <p>
         * <b>Warning</b>: By changing this type's declaration, any other type will not change its declaration of enclosing members or
         * declared types about any nesting of a declaration. It is the responsibility of the user of this API to keep such declarations
         * consistent among the definitions of connected types.
         * </p>
         *
         * @param type The types being declared.
         * @return A new builder that is equal to this builder with the supplied types being declared by the built type.
         */
        Builder<T> declaredTypes(TypeDescription... type);

        /**
         * <p>
         * Defines this type as an the outer type of the supplied types. Using this method, it is possible to add inner type declarations
         * for anonymous or local types which are not normally exposed by type descriptions. Doing so, it is however possible to indicate to
         * Byte Buddy that the required attributes for such an inner type declaration should be added to a class file.
         * </p>
         * <p>
         * <b>Important</b>: Changing the declaration hierarchy of a type has no influence on the nest mate hierarchy.
         * </p>
         * <p>
         * <b>Warning</b>: By changing this type's declaration, any other type will not change its declaration of enclosing members or
         * declared types about any nesting of a declaration. It is the responsibility of the user of this API to keep such declarations
         * consistent among the definitions of connected types.
         * </p>
         *
         * @param types The types being declared.
         * @return A new builder that is equal to this builder with the supplied types being declared by the built type.
         */
        Builder<T> declaredTypes(List<? extends Class<?>> types);

        /**
         * <p>
         * Defines this type as an the outer type of the supplied types. Using this method, it is possible to add inner type declarations
         * for anonymous or local types which are not normally exposed by type descriptions. Doing so, it is however possible to indicate to
         * Byte Buddy that the required attributes for such an inner type declaration should be added to a class file.
         * </p>
         * <p>
         * <b>Important</b>: Changing the declaration hierarchy of a type has no influence on the nest mate hierarchy.
         * </p>
         * <p>
         * <b>Warning</b>: By changing this type's declaration, any other type will not change its declaration of enclosing members or
         * declared types about any nesting of a declaration. It is the responsibility of the user of this API to keep such declarations
         * consistent among the definitions of connected types.
         * </p>
         *
         * @param types The types being declared.
         * @return A new builder that is equal to this builder with the supplied types being declared by the built type.
         */
        Builder<T> declaredTypes(Collection<? extends TypeDescription> types);

        /**
         * <p>
         * Defines this type as self-hosted, i.e. as only being a nest mate of itself.
         * </p>
         * <p>
         * <b>Important</b>: Changing the nest mate hierarchy of a type has no influence on the declaration hierarchy.
         * </p>
         * <p>
         * <b>Warning</b>: Changing nest mate hierarchies always requires changing a member and its host or a host and all its members.
         * Otherwise, the runtime will not accept further nest mates. It is the responsibility of the user of this API to keep such declarations
         * consistent among the definitions of connected types.
         * </p>
         *
         * @return A new builder that is equal to this builder but where the built type is a self-hosted nest mate.
         */
        Builder<T> noNestMate();

        /**
         * <p>
         * Defines this type as a nest member of the supplied type as a nest host.
         * </p>
         * <p>
         * <b>Important</b>: Changing the nest mate hierarchy of a type has no influence on the declaration hierarchy.
         * </p>
         * <p>
         * <b>Warning</b>: Changing nest mate hierarchies always requires changing a member and its host or a host and all its members.
         * Otherwise, the runtime will not accept further nest mates. It is the responsibility of the user of this API to keep such declarations
         * consistent among the definitions of connected types.
         * </p>
         *
         * @param type The nest host.
         * @return A new builder that is equal to this builder but where the built type is a nest member of the supplied host.
         */
        Builder<T> nestHost(Class<?> type);

        /**
         * <p>
         * Defines this type as a nest member of the supplied type as a nest host.
         * </p>
         * <p>
         * <b>Important</b>: Changing the nest mate hierarchy of a type has no influence on the declaration hierarchy.
         * </p>
         * <p>
         * <b>Warning</b>: Changing nest mate hierarchies always requires changing a member and its host or a host and all its members.
         * Otherwise, the runtime will not accept further nest mates. It is the responsibility of the user of this API to keep such declarations
         * consistent among the definitions of connected types.
         * </p>
         *
         * @param type The nest host.
         * @return A new builder that is equal to this builder but where the built type is a nest member of the supplied host.
         */
        Builder<T> nestHost(TypeDescription type);

        /**
         * <p>
         * Defines this type as a nest host for the supplied types.
         * </p>
         * <p>
         * <b>Important</b>: Changing the nest mate hierarchy of a type has no influence on the declaration hierarchy.
         * </p>
         * <p>
         * <b>Warning</b>: Changing nest mate hierarchies always requires changing a member and its host or a host and all its members.
         * Otherwise, the runtime will not accept further nest mates. It is the responsibility of the user of this API to keep such declarations
         * consistent among the definitions of connected types.
         * </p>
         *
         * @param type The nest members.
         * @return A new builder that is equal to this builder but where the built type is a nest host of the supplied types.
         */
        Builder<T> nestMembers(Class<?>... type);

        /**
         * <p>
         * Defines this type as a nest host for the supplied types.
         * </p>
         * <p>
         * <b>Important</b>: Changing the nest mate hierarchy of a type has no influence on the declaration hierarchy.
         * </p>
         * <p>
         * <b>Warning</b>: Changing nest mate hierarchies always requires changing a member and its host or a host and all its members.
         * Otherwise, the runtime will not accept further nest mates. It is the responsibility of the user of this API to keep such declarations
         * consistent among the definitions of connected types.
         * </p>
         *
         * @param type The nest members.
         * @return A new builder that is equal to this builder but where the built type is a nest host of the supplied types.
         */
        Builder<T> nestMembers(TypeDescription... type);

        /**
         * <p>
         * Defines this type as a nest host for the supplied types.
         * </p>
         * <p>
         * <b>Important</b>: Changing the nest mate hierarchy of a type has no influence on the declaration hierarchy.
         * </p>
         * <p>
         * <b>Warning</b>: Changing nest mate hierarchies always requires changing a member and its host or a host and all its members.
         * Otherwise, the runtime will not accept further nest mates. It is the responsibility of the user of this API to keep such declarations
         * consistent among the definitions of connected types.
         * </p>
         *
         * @param types The nest members.
         * @return A new builder that is equal to this builder but where the built type is a nest host of the supplied types.
         */
        Builder<T> nestMembers(List<? extends Class<?>> types);

        /**
         * <p>
         * Defines this type as a nest host for the supplied types.
         * </p>
         * <p>
         * <b>Important</b>: Changing the nest mate hierarchy of a type has no influence on the declaration hierarchy.
         * </p>
         * <p>
         * <b>Warning</b>: Changing nest mate hierarchies always requires changing a member and its host or a host and all its members.
         * Otherwise, the runtime will not accept further nest mates. It is the responsibility of the user of this API to keep such declarations
         * consistent among the definitions of connected types.
         * </p>
         *
         * @param types The nest members.
         * @return A new builder that is equal to this builder but where the built type is a nest host of the supplied types.
         */
        Builder<T> nestMembers(Collection<? extends TypeDescription> types);

        /**
         * Defines this type to allow the supplied permitted subclasses additionally to any prior permitted subclasses. If
         * this type was not previously sealed, only the supplied subclasses are permitted.
         *
         * @param type The permitted subclasses.
         * @return A new builder that is equal to this builder but where the built type permits the supplied subclasses.
         */
        Builder<T> permittedSubclass(Class<?>... type);

        /**
         * Defines this type to allow the supplied permitted subclasses additionally to any prior permitted subclasses. If
         * this type was not previously sealed, only the supplied subclasses are permitted.
         *
         * @param type The permitted subclasses.
         * @return A new builder that is equal to this builder but where the built type permits the supplied subclasses.
         */
        Builder<T> permittedSubclass(TypeDescription... type);

        /**
         * Defines this type to allow the supplied permitted subclasses additionally to any prior permitted subclasses. If
         * this type was not previously sealed, only the supplied subclasses are permitted.
         *
         * @param types The permitted subclasses.
         * @return A new builder that is equal to this builder but where the built type permits the supplied subclasses.
         */
        Builder<T> permittedSubclass(List<? extends Class<?>> types);

        /**
         * Defines this type to allow the supplied permitted subclasses additionally to any prior permitted subclasses. If
         * this type was not previously sealed, only the supplied subclasses are permitted.
         *
         * @param types The permitted subclasses.
         * @return A new builder that is equal to this builder but where the built type permits the supplied subclasses.
         */
        Builder<T> permittedSubclass(Collection<? extends TypeDescription> types);

        /**
         * Unseales this type.
         *
         * @return A new builder that is equal to this builder but where the built type does not restrain its permitted subclasses.
         */
        Builder<T> unsealed();

        /**
         * Applies the given type attribute appender onto the instrumented type. Using a type attribute appender, it is possible to append
         * any type of meta data to a type, not only Java {@link Annotation}s.
         *
         * @param typeAttributeAppender The type attribute appender to apply.
         * @return A new builder that is equal to this builder but with the supplied type attribute appender applied to the instrumented type.
         */
        Builder<T> attribute(TypeAttributeAppender typeAttributeAppender);

        /**
         * Annotates the instrumented type with the supplied annotations.
         *
         * @param annotation The annotations to add to the instrumented type.
         * @return A new builder that is equal to this builder but with the annotations added to the instrumented type.
         */
        Builder<T> annotateType(Annotation... annotation);

        /**
         * Annotates the instrumented type with the supplied annotations.
         *
         * @param annotations The annotations to add to the instrumented type.
         * @return A new builder that is equal to this builder but with the annotations added to the instrumented type.
         */
        Builder<T> annotateType(List<? extends Annotation> annotations);

        /**
         * Annotates the instrumented type with the supplied annotations.
         *
         * @param annotation The annotations to add to the instrumented type.
         * @return A new builder that is equal to this builder but with the annotations added to the instrumented type.
         */
        Builder<T> annotateType(AnnotationDescription... annotation);

        /**
         * Annotates the instrumented type with the supplied annotations.
         *
         * @param annotations The annotations to add to the instrumented type.
         * @return A new builder that is equal to this builder but with the annotations added to the instrumented type.
         */
        Builder<T> annotateType(Collection<? extends AnnotationDescription> annotations);

        /**
         * <p>
         * Implements the supplied interfaces for the instrumented type. Optionally, it is possible to define the
         * methods that are defined by the interfaces or the interfaces' super interfaces. This excludes methods that
         * are explicitly ignored.
         * </p>
         * <p>
         * <b>Note</b>: This methods implements the supplied types <i>as is</i>, i.e. any {@link Class} values are implemented
         * as raw types if they declare type variables or an owner type.
         * </p>
         *
         * @param interfaceType The interface types to implement.
         * @return A new builder that is equal to this builder but with the interfaces implemented by the instrumented type.
         */
        MethodDefinition.ImplementationDefinition.Optional<T> implement(Type... interfaceType);

        /**
         * <p>
         * Implements the supplied interfaces for the instrumented type. Optionally, it is possible to define the
         * methods that are defined by the interfaces or the interfaces' super interfaces. This excludes methods that
         * are explicitly ignored.
         * </p>
         * <p>
         * <b>Note</b>: This methods implements the supplied types <i>as is</i>, i.e. any {@link Class} values are implemented
         * as raw types if they declare type variables or an owner type.
         * </p>
         *
         * @param interfaceTypes The interface types to implement.
         * @return A new builder that is equal to this builder but with the interfaces implemented by the instrumented type.
         */
        MethodDefinition.ImplementationDefinition.Optional<T> implement(List<? extends Type> interfaceTypes);

        /**
         * <p>
         * Implements the supplied interfaces for the instrumented type. Optionally, it is possible to define the
         * methods that are defined by the interfaces or the interfaces' super interfaces. This excludes methods that
         * are explicitly ignored.
         * </p>
         * <p>
         * <b>Note</b>: This methods implements the supplied types <i>as is</i>, i.e. any {@link TypeDescription} values are
         * implemented as raw types if they declare type variables or an owner type.
         * </p>
         *
         * @param interfaceType The interface types to implement.
         * @return A new builder that is equal to this builder but with the interfaces implemented by the instrumented type.
         */
        MethodDefinition.ImplementationDefinition.Optional<T> implement(TypeDefinition... interfaceType);

        /**
         * <p>
         * Implements the supplied interfaces for the instrumented type. Optionally, it is possible to define the
         * methods that are defined by the interfaces or the interfaces' super interfaces. This excludes methods that
         * are explicitly ignored.
         * </p>
         * <p>
         * <b>Note</b>: This methods implements the supplied types <i>as is</i>, i.e. any {@link TypeDescription} values are
         * implemented as raw types if they declare type variables or an owner type.
         * </p>
         *
         * @param interfaceTypes The interface types to implement.
         * @return A new builder that is equal to this builder but with the interfaces implemented by the instrumented type.
         */
        MethodDefinition.ImplementationDefinition.Optional<T> implement(Collection<? extends TypeDefinition> interfaceTypes);

        /**
         * <p>
         * Executes the supplied byte code appender within the beginning of the instrumented type's type initializer. The
         * supplied byte code appender <b>must not return</b> from the method. If several byte code appenders are supplied,
         * they are executed within their application order.
         * </p>
         * <p>
         * This method should only be used for preparing an instrumented type with a specific configuration. Normally,
         * a byte code appender is applied via Byte Buddy's standard API by invoking {@link Builder#invokable(ElementMatcher)}
         * using the {@link net.bytebuddy.matcher.ElementMatchers#isTypeInitializer()} matcher.
         * </p>
         *
         * @param byteCodeAppender The byte code appender to execute within the instrumented type's type initializer.
         * @return A new builder that is equal to this builder but with the supplied byte code appender being executed within
         * the instrumented type's type initializer.
         */
        Builder<T> initializer(ByteCodeAppender byteCodeAppender);

        /**
         * Executes the supplied loaded type initializer when loading the created instrumented type. If several loaded
         * type initializers are supplied, each loaded type initializer is executed in its registration order.
         *
         * @param loadedTypeInitializer The loaded type initializer to execute upon loading the instrumented type.
         * @return A new builder that is equal to this builder but with the supplied loaded type initializer executed upon
         * loading the instrumented type.
         */
        Builder<T> initializer(LoadedTypeInitializer loadedTypeInitializer);

        /**
         * Explicitly requires another dynamic type for the creation of this type.
         *
         * @param type                 The type to require.
         * @param binaryRepresentation The type's binary representation.
         * @return A new builder that is equal to this builder but which explicitly requires the supplied type.
         */
        Builder<T> require(TypeDescription type, byte[] binaryRepresentation);

        /**
         * Explicitly requires another dynamic type for the creation of this type.
         *
         * @param type                 The type to require.
         * @param binaryRepresentation The type's binary representation.
         * @param typeInitializer      The type's loaded type initializer.
         * @return A new builder that is equal to this builder but which explicitly requires the supplied type.
         */
        Builder<T> require(TypeDescription type, byte[] binaryRepresentation, LoadedTypeInitializer typeInitializer);

        /**
         * Explicitly requires other dynamic types for the creation of this type.
         *
         * @param auxiliaryType The required dynamic types.
         * @return A new builder that is equal to this builder but which explicitly requires the supplied types.
         */
        Builder<T> require(DynamicType... auxiliaryType);

        /**
         * Explicitly requires other dynamic types for the creation of this type.
         *
         * @param auxiliaryTypes The required dynamic types.
         * @return A new builder that is equal to this builder but which explicitly requires the supplied types.
         */
        Builder<T> require(Collection<DynamicType> auxiliaryTypes);

        /**
         * Defines the supplied type variable without any bounds as a type variable of the instrumented type.
         *
         * @param symbol The type variable's symbol.
         * @return A new builder that is equal to this builder but with the given type variable defined for the instrumented type.
         */
        TypeVariableDefinition<T> typeVariable(String symbol);

        /**
         * Defines the supplied type variable with the given bound as a type variable of the instrumented type.
         *
         * @param symbol The type variable's symbol.
         * @param bound  The type variable's upper bounds. Can also be {@link net.bytebuddy.dynamic.TargetType} if the bound type
         *               should be equal to the currently instrumented type.
         * @return A new builder that is equal to this builder but with the given type variable defined for the instrumented type.
         */
        TypeVariableDefinition<T> typeVariable(String symbol, Type... bound);

        /**
         * Defines the supplied type variable with the given bound as a type variable of the instrumented type.
         *
         * @param symbol The type variable's symbol.
         * @param bounds The type variable's upper bounds. Can also be {@link net.bytebuddy.dynamic.TargetType} if the bound type
         *               should be equal to the currently instrumented type.
         * @return A new builder that is equal to this builder but with the given type variable defined for the instrumented type.
         */
        TypeVariableDefinition<T> typeVariable(String symbol, List<? extends Type> bounds);

        /**
         * Defines the supplied type variable with the given bound as a type variable of the instrumented type.
         *
         * @param symbol The type variable's symbol.
         * @param bound  The type variable's upper bounds. Can also be {@link net.bytebuddy.dynamic.TargetType} if the bound type
         *               should be equal to the currently instrumented type.
         * @return A new builder that is equal to this builder but with the given type variable defined for the instrumented type.
         */
        TypeVariableDefinition<T> typeVariable(String symbol, TypeDefinition... bound);

        /**
         * Defines the supplied type variable with the given bound as a type variable of the instrumented type.
         *
         * @param symbol The type variable's symbol.
         * @param bounds The type variable's upper bounds. Can also be {@link net.bytebuddy.dynamic.TargetType} if the bound type
         *               should be equal to the currently instrumented type.
         * @return A new builder that is equal to this builder but with the given type variable defined for the instrumented type.
         */
        TypeVariableDefinition<T> typeVariable(String symbol, Collection<? extends TypeDefinition> bounds);

        /**
         * Transforms any type variable that is defined by this type if it is matched by the supplied matcher.
         *
         * @param matcher     The matcher to decide what type variables to transform.
         * @param transformer The transformer to apply to the matched type variables.
         * @return A new builder that is equal to this builder but with the supplied transformer applied to all type variables.
         */
        Builder<T> transform(ElementMatcher<? super TypeDescription.Generic> matcher, Transformer<TypeVariableToken> transformer);

        /**
         * Defines the specified field as a field of the built dynamic type.
         *
         * @param name                The name of the field.
         * @param type                The type of the field. Can also be {@link net.bytebuddy.dynamic.TargetType} if the field type
         *                            should be equal to the currently instrumented type.
         * @param modifierContributor The modifiers of the field.
         * @return A new builder that is equal to this builder but with the given field defined for the instrumented type.
         * Furthermore, it is possible to optionally define a value, annotations or custom attributes for the field.
         */
        FieldDefinition.Optional.Valuable<T> defineField(String name, Type type, ModifierContributor.ForField... modifierContributor);

        /**
         * Defines the specified field as a field of the built dynamic type.
         *
         * @param name                 The name of the field.
         * @param type                 The type of the field. Can also be {@link net.bytebuddy.dynamic.TargetType} if the field type
         *                             should be equal to the currently instrumented type.
         * @param modifierContributors The modifiers of the field.
         * @return A new builder that is equal to this builder but with the given field defined for the instrumented type.
         * Furthermore, it is possible to optionally define a value, annotations or custom attributes for the field.
         */
        FieldDefinition.Optional.Valuable<T> defineField(String name, Type type, Collection<? extends ModifierContributor.ForField> modifierContributors);

        /**
         * Defines the specified field as a field of the built dynamic type.
         *
         * @param name      The name of the field.
         * @param type      The type of the field. Can also be {@link net.bytebuddy.dynamic.TargetType} if the field type
         *                  should be equal to the currently instrumented type.
         * @param modifiers The modifiers of the field.
         * @return A new builder that is equal to this builder but with the given field defined for the instrumented type.
         * Furthermore, it is possible to optionally define a value, annotations or custom attributes for the field.
         */
        FieldDefinition.Optional.Valuable<T> defineField(String name, Type type, int modifiers);

        /**
         * Defines the specified field as a field of the built dynamic type.
         *
         * @param name                The name of the field.
         * @param type                The type of the field. Can also be {@link net.bytebuddy.dynamic.TargetType} if the field type
         *                            should be equal to the currently instrumented type.
         * @param modifierContributor The modifiers of the field.
         * @return A new builder that is equal to this builder but with the given field defined for the instrumented type.
         * Furthermore, it is possible to optionally define a value, annotations or custom attributes for the field.
         */
        FieldDefinition.Optional.Valuable<T> defineField(String name, TypeDefinition type, ModifierContributor.ForField... modifierContributor);

        /**
         * Defines the specified field as a field of the built dynamic type.
         *
         * @param name                 The name of the field.
         * @param type                 The type of the field. Can also be {@link net.bytebuddy.dynamic.TargetType} if the field type
         *                             should be equal to the currently instrumented type.
         * @param modifierContributors The modifiers of the field.
         * @return A new builder that is equal to this builder but with the given field defined for the instrumented type.
         * Furthermore, it is possible to optionally define a value, annotations or custom attributes for the field.
         */
        FieldDefinition.Optional.Valuable<T> defineField(String name, TypeDefinition type, Collection<? extends ModifierContributor.ForField> modifierContributors);

        /**
         * Defines the specified field as a field of the built dynamic type.
         *
         * @param name      The name of the field.
         * @param type      The type of the field. Can also be {@link net.bytebuddy.dynamic.TargetType} if the field type
         *                  should be equal to the currently instrumented type.
         * @param modifiers The modifiers of the field.
         * @return A new builder that is equal to this builder but with the given field defined for the instrumented type.
         * Furthermore, it is possible to optionally define a value, annotations or custom attributes for the field.
         */
        FieldDefinition.Optional.Valuable<T> defineField(String name, TypeDefinition type, int modifiers);

        /**
         * Defines a field that is similar to the supplied field but without copying any annotations on the field.
         *
         * @param field The field to imitate as a field of the instrumented type.
         * @return A new builder that is equal to this builder but with the given field defined for the instrumented type.
         * Furthermore, it is possible to optionally define a value, annotations or custom attributes for the field.
         */
        FieldDefinition.Optional.Valuable<T> define(Field field);

        /**
         * Defines a field that is similar to the supplied field but without copying any annotations on the field.
         *
         * @param field The field to imitate as a field of the instrumented type.
         * @return A new builder that is equal to this builder but with the given field defined for the instrumented type.
         * Furthermore, it is possible to optionally define a value, annotations or custom attributes for the field.
         */
        FieldDefinition.Optional.Valuable<T> define(FieldDescription field);

        /**
         * Defines a private, static, final field for a serial version UID of the given value.
         *
         * @param serialVersionUid The serial version UID to define as a value.
         * @return A new builder that is equal to this builder but with the given type variable defined for the instrumented type.
         * Furthermore, it is possible to optionally define a value, annotations or custom attributes for the field.
         */
        FieldDefinition.Optional<T> serialVersionUid(long serialVersionUid);

        /**
         * <p>
         * Matches a field that is already declared by the instrumented type. This gives opportunity to change that field's
         * default value, annotations or custom attributes.
         * </p>
         * <p>
         * When a type is redefined or rebased, any annotations that the field declared previously is preserved
         * <i>as it is</i> if Byte Buddy is configured to retain such annotations by
         * {@link net.bytebuddy.implementation.attribute.AnnotationRetention#ENABLED}. If any existing annotations should be
         * altered, annotation retention must be disabled.
         * </p>
         * <p>
         * If a field is already matched by a previously specified field matcher, the new field definition gets precedence
         * over the previous definition, i.e. the previous field definition is no longer applied.
         * </p>
         *
         * @param matcher The matcher that determines what declared fields are affected by the subsequent specification.
         * @return A builder that allows for changing a field's definition.
         */
        FieldDefinition.Valuable<T> field(ElementMatcher<? super FieldDescription> matcher);

        /**
         * <p>
         * Matches a field that is already declared by the instrumented type. This gives opportunity to change that field's
         * default value, annotations or custom attributes. Using a latent matcher gives opportunity to resolve an
         * {@link ElementMatcher} based on the instrumented type before applying the matcher.
         * </p>
         * <p>
         * When a type is redefined or rebased, any annotations that the field declared previously is preserved
         * <i>as it is</i> if Byte Buddy is configured to retain such annotations by
         * {@link net.bytebuddy.implementation.attribute.AnnotationRetention#ENABLED}. If any existing annotations should be
         * altered, annotation retention must be disabled.
         * </p>
         * <p>
         * If a field is already matched by a previously specified field matcher, the new field definition gets precedence
         * over the previous definition, i.e. the previous field definition is no longer applied.
         * </p>
         *
         * @param matcher The matcher that determines what declared fields are affected by the subsequent specification.
         * @return A builder that allows for changing a field's definition.
         */
        FieldDefinition.Valuable<T> field(LatentMatcher<? super FieldDescription> matcher);

        /**
         * <p>
         * Specifies to exclude any method that is matched by the supplied matcher from instrumentation. Previously supplied matchers
         * remain valid after supplying a new matcher, i.e. any method that is matched by a previously supplied matcher is always ignored.
         * </p>
         * <p>
         * When ignoring a type, previously registered matchers are applied before this matcher. If a previous matcher indicates that a type
         * is to be ignored, this matcher is no longer executed.
         * </p>
         *
         * @param ignoredMethods The matcher for determining what methods to exclude from instrumentation.
         * @return A new builder that is equal to this builder but that is excluding any method that is matched by the supplied matcher from
         * instrumentation.
         */
        Builder<T> ignoreAlso(ElementMatcher<? super MethodDescription> ignoredMethods);

        /**
         * <p>
         * Specifies to exclude any method that is matched by the supplied matcher from instrumentation. Previously supplied matchers
         * remain valid after supplying a new matcher, i.e. any method that is matched by a previously supplied matcher is always ignored.
         * Using a latent matcher gives opportunity to resolve an {@link ElementMatcher} based on the instrumented type before applying the
         * matcher.
         * </p>
         * <p>
         * When ignoring a type, previously registered matchers are applied before this matcher. If a previous matcher indicates that a type
         * is to be ignored, this matcher is no longer executed.
         * </p>
         *
         * @param ignoredMethods The matcher for determining what methods to exclude from instrumentation.
         * @return A new builder that is equal to this builder but that is excluding any method that is matched by the supplied matcher from
         * instrumentation.
         */
        Builder<T> ignoreAlso(LatentMatcher<? super MethodDescription> ignoredMethods);

        /**
         * Defines the specified method to be declared by the instrumented type. Method parameters or parameter types, declared exceptions and
         * type variables can be defined in subsequent steps.
         *
         * @param name                The name of the method.
         * @param returnType          The method's return type. Can also be {@link net.bytebuddy.dynamic.TargetType} if the return type
         *                            should be equal to the currently instrumented type.
         * @param modifierContributor The method's modifiers.
         * @return A builder that allows for further defining the method, either by adding more properties or by defining an implementation.
         */
        MethodDefinition.ParameterDefinition.Initial<T> defineMethod(String name, Type returnType, ModifierContributor.ForMethod... modifierContributor);

        /**
         * Defines the specified method to be declared by the instrumented type. Method parameters or parameter types, declared exceptions and
         * type variables can be defined in subsequent steps.
         *
         * @param name                 The name of the method.
         * @param returnType           The method's return type. Can also be {@link net.bytebuddy.dynamic.TargetType} if the return type
         *                             should be equal to the currently instrumented type.
         * @param modifierContributors The method's modifiers.
         * @return A builder that allows for further defining the method, either by adding more properties or by defining an implementation.
         */
        MethodDefinition.ParameterDefinition.Initial<T> defineMethod(String name, Type returnType, Collection<? extends ModifierContributor.ForMethod> modifierContributors);

        /**
         * Defines the specified method to be declared by the instrumented type. Method parameters or parameter types, declared exceptions and
         * type variables can be defined in subsequent steps.
         *
         * @param name       The name of the method.
         * @param returnType The method's return type. Can also be {@link net.bytebuddy.dynamic.TargetType} if the return type
         *                   should be equal to the currently instrumented type.
         * @param modifiers  The method's modifiers.
         * @return A builder that allows for further defining the method, either by adding more properties or by defining an implementation.
         */
        MethodDefinition.ParameterDefinition.Initial<T> defineMethod(String name, Type returnType, int modifiers);

        /**
         * Defines the specified method to be declared by the instrumented type. Method parameters or parameter types, declared exceptions and
         * type variables can be defined in subsequent steps.
         *
         * @param name                The name of the method.
         * @param returnType          The method's return type. Can also be {@link net.bytebuddy.dynamic.TargetType} if the return type
         *                            should be equal to the currently instrumented type.
         * @param modifierContributor The method's modifiers.
         * @return A builder that allows for further defining the method, either by adding more properties or by defining an implementation.
         */
        MethodDefinition.ParameterDefinition.Initial<T> defineMethod(String name, TypeDefinition returnType, ModifierContributor.ForMethod... modifierContributor);

        /**
         * Defines the specified method to be declared by the instrumented type. Method parameters or parameter types, declared exceptions and
         * type variables can be defined in subsequent steps.
         *
         * @param name                 The name of the method.
         * @param returnType           The method's return type. Can also be {@link net.bytebuddy.dynamic.TargetType} if the return type
         *                             should be equal to the currently instrumented type.
         * @param modifierContributors The method's modifiers.
         * @return A builder that allows for further defining the method, either by adding more properties or by defining an implementation.
         */
        MethodDefinition.ParameterDefinition.Initial<T> defineMethod(String name, TypeDefinition returnType, Collection<? extends ModifierContributor.ForMethod> modifierContributors);

        /**
         * Defines the specified method to be declared by the instrumented type. Method parameters or parameter types, declared exceptions and
         * type variables can be defined in subsequent steps.
         *
         * @param name       The name of the method.
         * @param returnType The method's return type. Can also be {@link net.bytebuddy.dynamic.TargetType} if the return type
         *                   should be equal to the currently instrumented type.
         * @param modifiers  The method's modifiers.
         * @return A builder that allows for further defining the method, either by adding more properties or by defining an implementation.
         */
        MethodDefinition.ParameterDefinition.Initial<T> defineMethod(String name, TypeDefinition returnType, int modifiers);

        /**
         * Defines the specified constructor to be declared by the instrumented type. Method parameters or parameter types, declared exceptions and
         * type variables can be defined in subsequent steps.
         *
         * @param modifierContributor The constructor's modifiers.
         * @return A builder that allows for further defining the constructor, either by adding more properties or by defining an implementation.
         */
        MethodDefinition.ParameterDefinition.Initial<T> defineConstructor(ModifierContributor.ForMethod... modifierContributor);

        /**
         * Defines the specified constructor to be declared by the instrumented type. Method parameters or parameter types, declared exceptions and
         * type variables can be defined in subsequent steps.
         *
         * @param modifierContributors The constructor's modifiers.
         * @return A builder that allows for further defining the constructor, either by adding more properties or by defining an implementation.
         */
        MethodDefinition.ParameterDefinition.Initial<T> defineConstructor(Collection<? extends ModifierContributor.ForMethod> modifierContributors);

        /**
         * Defines the specified constructor to be declared by the instrumented type. Method parameters or parameter types, declared exceptions and
         * type variables can be defined in subsequent steps.
         *
         * @param modifiers The constructor's modifiers.
         * @return A builder that allows for further defining the constructor, either by adding more properties or by defining an implementation.
         */
        MethodDefinition.ParameterDefinition.Initial<T> defineConstructor(int modifiers);

        /**
         * Defines a method that is similar to the supplied method but without copying any annotations of the method or method parameters.
         *
         * @param method The method to imitate as a method of the instrumented type.
         * @return A builder that allows for defining an implementation for the method.
         */
        MethodDefinition.ImplementationDefinition<T> define(Method method);

        /**
         * Defines a constructor that is similar to the supplied constructor but without copying any annotations of the constructor or
         * constructor parameters.
         *
         * @param constructor The constructor to imitate as a method of the instrumented type.
         * @return A builder that allows for defining an implementation for the constructor.
         */
        MethodDefinition.ImplementationDefinition<T> define(Constructor<?> constructor);

        /**
         * Defines a method or constructor that is similar to the supplied method description but without copying any annotations of
         * the method/constructor or method/constructor parameters.
         *
         * @param methodDescription The method description to imitate as a method or constructor of the instrumented type.
         * @return A builder that allows for defining an implementation for the method or constructor.
         */
        MethodDefinition.ImplementationDefinition<T> define(MethodDescription methodDescription);

        /**
         * Defines a Java bean property with the specified name.
         *
         * @param name The name of the property.
         * @param type The property type.
         * @return A builder that defines the specified property where the field holding the property can be refined by subsequent steps.
         */
        FieldDefinition.Optional<T> defineProperty(String name, Type type);

        /**
         * Defines a Java bean property with the specified name.
         *
         * @param name     The name of the property.
         * @param type     The property type.
         * @param readOnly {@code true} if the property is read only, i.e. no setter should be defined and the field should be {@code final}.
         * @return A builder that defines the specified property where the field holding the property can be refined by subsequent steps.
         */
        FieldDefinition.Optional<T> defineProperty(String name, Type type, boolean readOnly);

        /**
         * Defines a Java bean property with the specified name.
         *
         * @param name The name of the property.
         * @param type The property type.
         * @return A builder that defines the specified property where the field holding the property can be refined by subsequent steps.
         */
        FieldDefinition.Optional<T> defineProperty(String name, TypeDefinition type);

        /**
         * Defines a Java bean property with the specified name.
         *
         * @param name     The name of the property.
         * @param type     The property type.
         * @param readOnly {@code true} if the property is read only, i.e. no setter should be defined and the field should be {@code final}.
         * @return A builder that defines the specified property where the field holding the property can be refined by subsequent steps.
         */
        FieldDefinition.Optional<T> defineProperty(String name, TypeDefinition type, boolean readOnly);

        /**
         * <p>
         * Matches a method that is already declared or inherited by the instrumented type. This gives opportunity to change or to
         * override that method's implementation, default value, annotations or custom attributes. It is also possible to make
         * a method abstract.
         * </p>
         * <p>
         * When a type is redefined or rebased, any annotations that the method declared previously is preserved
         * <i>as it is</i> if Byte Buddy is configured to retain such annotations by
         * {@link net.bytebuddy.implementation.attribute.AnnotationRetention#ENABLED}. If any existing annotations should be
         * altered, annotation retention must be disabled.
         * </p>
         * <p>
         * If a method is already matched by a previously specified matcher, the new method definition gets precedence
         * over the previous definition, i.e. the previous method definition is no longer applied.
         * </p>
         * <p>
         * Note that the specified definition does never apply for methods that are explicitly ignored.
         * </p>
         *
         * @param matcher The matcher that determines what methods are affected by the subsequent specification.
         * @return A builder that allows for changing a method's or constructor's definition.
         */
        MethodDefinition.ImplementationDefinition<T> method(ElementMatcher<? super MethodDescription> matcher);

        /**
         * <p>
         * Matches a constructor that is already declared by the instrumented type. This gives opportunity to change that constructor's
         * implementation, default value, annotations or custom attributes.
         * </p>
         * <p>
         * When a type is redefined or rebased, any annotations that the constructor declared previously is preserved
         * <i>as it is</i> if Byte Buddy is configured to retain such annotations by
         * {@link net.bytebuddy.implementation.attribute.AnnotationRetention#ENABLED}. If any existing annotations should be
         * altered, annotation retention must be disabled.
         * </p>
         * <p>
         * If a constructor is already matched by a previously specified matcher, the new constructor definition gets precedence
         * over the previous definition, i.e. the previous constructor definition is no longer applied.
         * </p>
         * <p>
         * Note that the specified definition does never apply for methods that are explicitly ignored.
         * </p>
         *
         * @param matcher The matcher that determines what constructors are affected by the subsequent specification.
         * @return A builder that allows for changing a method's or constructor's definition.
         */
        MethodDefinition.ImplementationDefinition<T> constructor(ElementMatcher<? super MethodDescription> matcher);

        /**
         * <p>
         * Matches a method or constructor that is already declared or inherited by the instrumented type. This gives
         * opportunity to change or to override that method's or constructor's implementation, default value, annotations
         * or custom attributes. It is also possible to make a method abstract.
         * </p>
         * <p>
         * When a type is redefined or rebased, any annotations that the method or constructor declared previously is preserved
         * <i>as it is</i> if Byte Buddy is configured to retain such annotations by
         * {@link net.bytebuddy.implementation.attribute.AnnotationRetention#ENABLED}. If any existing annotations should be
         * altered, annotation retention must be disabled.
         * </p>
         * <p>
         * If a method or constructor is already matched by a previously specified matcher, the new definition gets precedence
         * over the previous definition, i.e. the previous definition is no longer applied.
         * </p>
         * <p>
         * Note that the specified definition does never apply for methods that are explicitly ignored.
         * </p>
         * <p>
         * <b>Important</b>: It is possible to instrument the dynamic type's initializer. Depending on the used {@link TypeResolutionStrategy},
         * the type initializer might be run <b>before</b> Byte Buddy could apply any {@link LoadedTypeInitializer}s which are
         * responsible for preparing the instrumented type prior to the initializer's execution. For preparing the type prior to
         * executing the initializer, an {@link TypeResolutionStrategy.Active} resolver must be chosen.
         * </p>
         *
         * @param matcher The matcher that determines what methods or constructors are affected by the subsequent specification.
         * @return A builder that allows for changing a method's or constructor's definition.
         */
        MethodDefinition.ImplementationDefinition<T> invokable(ElementMatcher<? super MethodDescription> matcher);

        /**
         * <p>
         * Matches a method or constructor that is already declared or inherited by the instrumented type. This gives
         * opportunity to change or to override that method's or constructor's implementation, default value, annotations
         * or custom attributes. It is also possible to make a method abstract. Using a latent matcher gives opportunity
         * to resolve an {@link ElementMatcher} based on the instrumented type before applying the matcher.
         * </p>
         * <p>
         * When a type is redefined or rebased, any annotations that the method or constructor declared previously is preserved
         * <i>as it is</i> if Byte Buddy is configured to retain such annotations by
         * {@link net.bytebuddy.implementation.attribute.AnnotationRetention#ENABLED}. If any existing annotations should be
         * altered, annotation retention must be disabled.
         * </p>
         * <p>
         * If a method or constructor is already matched by a previously specified matcher, the new definition gets precedence
         * over the previous definition, i.e. the previous definition is no longer applied.
         * </p>
         * <p>
         * Note that the specified definition does never apply for methods that are explicitly ignored.
         * </p>
         * <p>
         * <b>Important</b>: It is possible to instrument the dynamic type's initializer. Depending on the used {@link TypeResolutionStrategy},
         * the type initializer might be run <b>before</b> Byte Buddy could apply any {@link LoadedTypeInitializer}s which are
         * responsible for preparing the instrumented type prior to the initializer's execution. For preparing the type prior to
         * executing the initializer, an {@link TypeResolutionStrategy.Active} resolver must be chosen.
         * </p>
         *
         * @param matcher The matcher that determines what declared methods or constructors are affected by the subsequent specification.
         * @return A builder that allows for changing a method's or constructor's definition.
         */
        MethodDefinition.ImplementationDefinition<T> invokable(LatentMatcher<? super MethodDescription> matcher);

        /**
         * Implements {@link Object#hashCode()} and {@link Object#equals(Object)} methods for the instrumented type if those
         * methods are not declared as {@code final} by a super class. The implementations do not consider any implementations
         * of a super class and compare a class field by field without considering synthetic fields.
         *
         * @return A new type builder that defines {@link Object#hashCode()} and {@link Object#equals(Object)} methods accordingly.
         */
        Builder<T> withHashCodeEquals();

        /**
         * Implements a {@link Object#toString()} method for the instrumented type if such a method is not declared as {@code final}
         * by a super class. The implementation prefixes the string with the simple class name and prints each non-synthetic field's
         * value after the field's name.
         *
         * @return A new type builder that defines {@link Object#toString()} method accordingly.
         */
        Builder<T> withToString();

        /**
         * Defines a new record component. Note that this does not add or change implementations for a field, an accessor
         * to this field or a constructor unless {@link net.bytebuddy.ByteBuddy#makeRecord()} is used.
         *
         * @param name The record component's name.
         * @param type The record component's type.
         * @return A new builder that is equal to this builder but also defines the supplied record component.
         */
        RecordComponentDefinition.Optional<T> defineRecordComponent(String name, Type type);

        /**
         * Defines a new record component. Note that this does not add or change implementations for a field, an accessor
         * to this field or a constructor unless {@link net.bytebuddy.ByteBuddy#makeRecord()} is used.
         *
         * @param name The record component's name.
         * @param type The record component's type.
         * @return A new builder that is equal to this builder but also defines the supplied record component.
         */
        RecordComponentDefinition.Optional<T> defineRecordComponent(String name, TypeDefinition type);

        /**
         * Defines a new record component. Note that this does not add or change implementations for a field, an accessor
         * to this field or a constructor unless {@link net.bytebuddy.ByteBuddy#makeRecord()} is used.
         *
         * @param recordComponentDescription A description of the record component to immitate.
         * @return A new builder that is equal to this builder but also defines the supplied record component.
         */
        RecordComponentDefinition.Optional<T> define(RecordComponentDescription recordComponentDescription);

        /**
         * <p>
         * Matches a record component that is already declared by the instrumented type. This gives opportunity to change that
         * record component's annotations or custom attributes.
         * </p>
         * <p>
         * When a type is redefined or rebased, any annotations that the field declared previously is preserved
         * <i>as it is</i> if Byte Buddy is configured to retain such annotations by
         * {@link net.bytebuddy.implementation.attribute.AnnotationRetention#ENABLED}. If any existing annotations should be
         * altered, annotation retention must be disabled.
         * </p>
         * <p>
         * If a record component is already matched by a previously specified record component matcher, the new record component
         * definition gets precedence over the previous definition, i.e. the previous record component definition is no longer applied.
         * </p>
         *
         * @param matcher The matcher that determines what declared record components are affected by the subsequent specification.
         * @return A builder that allows for changing a record component's definition.
         */
        RecordComponentDefinition<T> recordComponent(ElementMatcher<? super RecordComponentDescription> matcher);

        /**
         * <p>
         * Matches a record component that is already declared by the instrumented type. This gives opportunity to change that
         * record component's annotations or custom attributes.
         * </p>
         * <p>
         * When a type is redefined or rebased, any annotations that the field declared previously is preserved
         * <i>as it is</i> if Byte Buddy is configured to retain such annotations by
         * {@link net.bytebuddy.implementation.attribute.AnnotationRetention#ENABLED}. If any existing annotations should be
         * altered, annotation retention must be disabled.
         * </p>
         * <p>
         * If a record component is already matched by a previously specified record component matcher, the new record component
         * definition gets precedence over the previous definition, i.e. the previous record component definition is no longer applied.
         * </p>
         *
         * @param matcher The matcher that determines what declared record components are affected by the subsequent specification.
         * @return A builder that allows for changing a record component's definition.
         */
        RecordComponentDefinition<T> recordComponent(LatentMatcher<? super RecordComponentDescription> matcher);

        /**
         * <p>
         * Creates the dynamic type this builder represents. If the specified dynamic type is not legal, an {@link IllegalStateException} is thrown.
         * </p>
         * <p>
         * Other than {@link DynamicType.Builder#make(TypePool)}, this method supplies a context-dependant type pool to the underlying class writer.
         * Supplying a type pool only makes sense if custom byte code is created by adding a custom {@link AsmVisitorWrapper} where ASM might be
         * required to compute stack map frames by processing information over any mentioned type's class hierarchy.
         * </p>
         * <p>
         * The dynamic type is initialized using a {@link TypeResolutionStrategy.Passive} strategy. Using this strategy, no
         * {@link LoadedTypeInitializer} is run during the execution of the type's initializer such that no {@link Implementation} used for
         * executing the initializer must rely on such an initializer.
         * </p>
         *
         * @return An unloaded dynamic type representing the type specified by this builder.
         */
        DynamicType.Unloaded<T> make();

        /**
         * <p>
         * Creates the dynamic type this builder represents. If the specified dynamic type is not legal, an {@link IllegalStateException} is thrown.
         * </p>
         * <p>
         * The dynamic type is initialized using a {@link TypeResolutionStrategy.Passive} strategy. Using this strategy, no
         * {@link LoadedTypeInitializer} is run during the execution of the type's initializer such that no {@link Implementation} used for
         * executing the initializer must rely on such an initializer.
         * </p>
         *
         * @param typeResolutionStrategy The type resolution strategy to use for the created type's initialization.
         * @return An unloaded dynamic type representing the type specified by this builder.
         */
        DynamicType.Unloaded<T> make(TypeResolutionStrategy typeResolutionStrategy);

        /**
         * <p>
         * Creates the dynamic type this builder represents. If the specified dynamic type is not legal, an {@link IllegalStateException} is thrown.
         * </p>
         * <p>
         * The dynamic type is initialized using a {@link TypeResolutionStrategy.Passive} strategy. Using this strategy, no
         * {@link LoadedTypeInitializer} is run during the execution of the type's initializer such that no {@link Implementation} used for
         * executing the initializer must rely on such an initializer.
         * </p>
         *
         * @param typePool A type pool that is used for computing stack map frames by the underlying class writer, if required.
         * @return An unloaded dynamic type representing the type specified by this builder.
         */
        DynamicType.Unloaded<T> make(TypePool typePool);

        /**
         * Creates the dynamic type this builder represents. If the specified dynamic type is not legal, an {@link IllegalStateException} is thrown.
         *
         * @param typeResolutionStrategy The type resolution strategy to use for the created type's initialization.
         * @param typePool               A type pool that is used for computing stack map frames by the underlying class writer, if required.
         * @return An unloaded dynamic type representing the type specified by this builder.
         */
        DynamicType.Unloaded<T> make(TypeResolutionStrategy typeResolutionStrategy, TypePool typePool);

        /**
         * Returns a {@link TypeDescription} for the currently built type.
         *
         * @return A {@link TypeDescription} for the currently built type.
         */
        TypeDescription toTypeDescription();

        /**
         * An inner type definition for defining a type that is contained within another type, method or constructor.
         *
         * @param <S> A loaded type that the built type is guaranteed to be a subclass of.
         */
        interface InnerTypeDefinition<S> extends Builder<S> {

            /**
             * Defines this inner type declaration as an anonymous type.
             *
             * @return A new builder that is equal to this type builder but that defines the previous inner type definition as a anonymous type.
             */
            Builder<S> asAnonymousType();

            /**
             * An inner type definition for defining a type that is contained within another type.
             *
             * @param <U> A loaded type that the built type is guaranteed to be a subclass of.
             */
            interface ForType<U> extends InnerTypeDefinition<U> {

                /**
                 * Defines this inner type declaration as a member type.
                 *
                 * @return A new builder that is equal to this type builder but that defines the previous inner type definition as a member type.
                 */
                Builder<U> asMemberType();
            }
        }

        /**
         * A builder for a type variable definition.
         *
         * @param <S> A loaded type that the built type is guaranteed to be a subclass of.
         */
        interface TypeVariableDefinition<S> extends Builder<S> {

            /**
             * Annotates the previously defined type variable with the supplied annotations.
             *
             * @param annotation The annotations to declare on the previously defined type variable.
             * @return A new builder that is equal to this builder but with the given annotations declared
             * on the previously defined type variable.
             */
            TypeVariableDefinition<S> annotateTypeVariable(Annotation... annotation);

            /**
             * Annotates the previously defined type variable with the supplied annotations.
             *
             * @param annotations The annotations to declare on the previously defined type variable.
             * @return A new builder that is equal to this builder but with the given annotations declared
             * on the previously defined type variable.
             */
            TypeVariableDefinition<S> annotateTypeVariable(List<? extends Annotation> annotations);

            /**
             * Annotates the previously defined type variable with the supplied annotations.
             *
             * @param annotation The annotations to declare on the previously defined type variable.
             * @return A new builder that is equal to this builder but with the given annotations declared
             * on the previously defined type variable.
             */
            TypeVariableDefinition<S> annotateTypeVariable(AnnotationDescription... annotation);

            /**
             * Annotates the previously defined type variable with the supplied annotations.
             *
             * @param annotations The annotations to declare on the previously defined type variable.
             * @return A new builder that is equal to this builder but with the given annotations declared
             * on the previously defined type variable.
             */
            TypeVariableDefinition<S> annotateTypeVariable(Collection<? extends AnnotationDescription> annotations);

            /**
             * An abstract base implementation of a type variable definition.
             *
             * @param <U> A loaded type that the built type is guaranteed to be a subclass of.
             */
            abstract class AbstractBase<U> extends Builder.AbstractBase.Delegator<U> implements TypeVariableDefinition<U> {

                /**
                 * {@inheritDoc}
                 */
                public TypeVariableDefinition<U> annotateTypeVariable(Annotation... annotation) {
                    return annotateTypeVariable(Arrays.asList(annotation));
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeVariableDefinition<U> annotateTypeVariable(List<? extends Annotation> annotations) {
                    return annotateTypeVariable(new AnnotationList.ForLoadedAnnotations(annotations));
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeVariableDefinition<U> annotateTypeVariable(AnnotationDescription... annotation) {
                    return annotateTypeVariable(Arrays.asList(annotation));
                }
            }
        }

        /**
         * A builder for a field definition.
         *
         * @param <S> A loaded type that the built type is guaranteed to be a subclass of.
         */
        interface FieldDefinition<S> {

            /**
             * Annotates the previously defined or matched field with the supplied annotations.
             *
             * @param annotation The annotations to declare on the previously defined or matched field.
             * @return A new builder that is equal to this builder but with the given annotations declared
             * on the previously defined or matched field.
             */
            FieldDefinition.Optional<S> annotateField(Annotation... annotation);

            /**
             * Annotates the previously defined or matched field with the supplied annotations.
             *
             * @param annotations The annotations to declare on the previously defined or matched field.
             * @return A new builder that is equal to this builder but with the given annotations declared
             * on the previously defined or matched field.
             */
            FieldDefinition.Optional<S> annotateField(List<? extends Annotation> annotations);

            /**
             * Annotates the previously defined or matched field with the supplied annotations.
             *
             * @param annotation The annotations to declare on the previously defined or matched field.
             * @return A new builder that is equal to this builder but with the given annotations declared
             * on the previously defined or matched field.
             */
            FieldDefinition.Optional<S> annotateField(AnnotationDescription... annotation);

            /**
             * Annotates the previously defined or matched field with the supplied annotations.
             *
             * @param annotations The annotations to declare on the previously defined or matched field.
             * @return A new builder that is equal to this builder but with the given annotations declared
             * on the previously defined or matched field.
             */
            FieldDefinition.Optional<S> annotateField(Collection<? extends AnnotationDescription> annotations);

            /**
             * Applies the supplied attribute appender factory onto the previously defined or matched field.
             *
             * @param fieldAttributeAppenderFactory The field attribute appender factory that should be applied on the
             *                                      previously defined or matched field.
             * @return A new builder that is equal to this builder but with the supplied field attribute appender factory
             * applied to the previously defined or matched field.
             */
            FieldDefinition.Optional<S> attribute(FieldAttributeAppender.Factory fieldAttributeAppenderFactory);

            /**
             * Applies the supplied transformer onto the previously defined or matched field. The transformed
             * field is written <i>as it is</i> and it not subject to any validations.
             *
             * @param transformer The transformer to apply to the previously defined or matched field.
             * @return A new builder that is equal to this builder but with the supplied field transformer
             * applied to the previously defined or matched field.
             */
            FieldDefinition.Optional<S> transform(Transformer<FieldDescription> transformer);

            /**
             * A builder for a field definition that allows for defining a value.
             *
             * @param <U> A loaded type that the built type is guaranteed to be a subclass of.
             */
            interface Valuable<U> extends FieldDefinition<U> {

                /**
                 * <p>
                 * Defines the supplied {@code boolean} value as a default value of the previously defined or matched field. The value can only
                 * be set for numeric fields of type {@code boolean}, {@code byte}, {@code short}, {@code char} or {@code int}. For non-boolean
                 * fields, the field's value is set to {@code 0} for {@code false} or {@code 1} for {@code true}.
                 * </p>
                 * <p>
                 * <b>Important</b>: A default value in a Java class file defines a field's value prior to the class's initialization. This value
                 * is only visible to code if the field is declared {@code static}. A default value can also be set for non-static fields where
                 * the value is not visible to code. The Java compiler only defines such values for {@code final} fields.
                 * </p>
                 *
                 * @param value The value to define as a default value of the defined field.
                 * @return A new builder that is equal to this builder but with the given default value declared for the
                 * previously defined or matched field.
                 */
                FieldDefinition.Optional<U> value(boolean value);

                /**
                 * <p>
                 * Defines the supplied {@code int} value as a default value of the previously defined or matched field. The value can only
                 * be set for numeric fields of type {@code boolean}, {@code byte}, {@code short}, {@code char} or {@code int} where the
                 * value must be within the numeric type's range. The {@code boolean} type is regarded as a numeric type with the possible
                 * values of {@code 0} and {@code 1} representing {@code false} and {@code true}.
                 * </p>
                 * <p>
                 * <b>Important</b>: A default value in a Java class file defines a field's value prior to the class's initialization. This value
                 * is only visible to code if the field is declared {@code static}. A default value can also be set for non-static fields where
                 * the value is not visible to code. The Java compiler only defines such values for {@code final} fields.
                 * </p>
                 *
                 * @param value The value to define as a default value of the defined field.
                 * @return A new builder that is equal to this builder but with the given default value declared for the
                 * previously defined or matched field.
                 */
                FieldDefinition.Optional<U> value(int value);

                /**
                 * <p>
                 * Defines the supplied {@code long} value as a default value of the previously defined or matched field.
                 * </p>
                 * <p>
                 * <b>Important</b>: A default value in a Java class file defines a field's value prior to the class's initialization. This value
                 * is only visible to code if the field is declared {@code static}. A default value can also be set for non-static fields where
                 * the value is not visible to code. The Java compiler only defines such values for {@code final} fields.
                 * </p>
                 *
                 * @param value The value to define as a default value of the defined field.
                 * @return A new builder that is equal to this builder but with the given default value declared for the
                 * previously defined or matched field.
                 */
                FieldDefinition.Optional<U> value(long value);

                /**
                 * <p>
                 * Defines the supplied {@code float} value as a default value of the previously defined or matched field.
                 * </p>
                 * <p>
                 * <b>Important</b>: A default value in a Java class file defines a field's value prior to the class's initialization. This value
                 * is only visible to code if the field is declared {@code static}. A default value can also be set for non-static fields where
                 * the value is not visible to code. The Java compiler only defines such values for {@code final} fields.
                 * </p>
                 *
                 * @param value The value to define as a default value of the defined field.
                 * @return A new builder that is equal to this builder but with the given default value declared for the
                 * previously defined or matched field.
                 */
                FieldDefinition.Optional<U> value(float value);

                /**
                 * <p>
                 * Defines the supplied {@code double} value as a default value of the previously defined or matched field.
                 * </p>
                 * <p>
                 * <b>Important</b>: A default value in a Java class file defines a field's value prior to the class's initialization. This value
                 * is only visible to code if the field is declared {@code static}. A default value can also be set for non-static fields where
                 * the value is not visible to code. The Java compiler only defines such values for {@code final} fields.
                 * </p>
                 *
                 * @param value The value to define as a default value of the defined field.
                 * @return A new builder that is equal to this builder but with the given default value declared for the
                 * previously defined or matched field.
                 */
                FieldDefinition.Optional<U> value(double value);

                /**
                 * <p>
                 * Defines the supplied {@link String} value as a default value of the previously defined or matched field.
                 * </p>
                 * <p>
                 * <b>Important</b>: A default value in a Java class file defines a field's value prior to the class's initialization. This value
                 * is only visible to code if the field is declared {@code static}. A default value can also be set for non-static fields where
                 * the value is not visible to code. The Java compiler only defines such values for {@code final} fields.
                 * </p>
                 *
                 * @param value The value to define as a default value of the defined field.
                 * @return A new builder that is equal to this builder but with the given default value declared for the
                 * previously defined or matched field.
                 */
                FieldDefinition.Optional<U> value(String value);
            }

            /**
             * A builder for an optional field definition.
             *
             * @param <U> A loaded type that the built type is guaranteed to be a subclass of.
             */
            interface Optional<U> extends FieldDefinition<U>, Builder<U> {

                /**
                 * A builder for an optional field definition that allows for defining a value.
                 *
                 * @param <V> A loaded type that the built type is guaranteed to be a subclass of.
                 */
                interface Valuable<V> extends FieldDefinition.Valuable<V>, Optional<V> {

                    /**
                     * An abstract base implementation of an optional field definition that allows for defining a value.
                     *
                     * @param <U> A loaded type that the built type is guaranteed to be a subclass of.
                     */
                    abstract class AbstractBase<U> extends Optional.AbstractBase<U> implements Optional.Valuable<U> {

                        /**
                         * {@inheritDoc}
                         */
                        public FieldDefinition.Optional<U> value(boolean value) {
                            return defaultValue(value ? 1 : 0);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public FieldDefinition.Optional<U> value(int value) {
                            return defaultValue(value);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public FieldDefinition.Optional<U> value(long value) {
                            return defaultValue(value);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public FieldDefinition.Optional<U> value(float value) {
                            return defaultValue(value);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public FieldDefinition.Optional<U> value(double value) {
                            return defaultValue(value);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public FieldDefinition.Optional<U> value(String value) {
                            if (value == null) {
                                throw new IllegalArgumentException("Cannot set null as a default value");
                            }
                            return defaultValue(value);
                        }

                        /**
                         * Defines the supplied value as a default value of the previously defined or matched field.
                         *
                         * @param defaultValue The value to define as a default value of the defined field.
                         * @return A new builder that is equal to this builder but with the given default value declared for the
                         * previously defined or matched field.
                         */
                        protected abstract FieldDefinition.Optional<U> defaultValue(Object defaultValue);

                        /**
                         * An adapter for an optional field definition that allows for defining a value.
                         *
                         * @param <V> A loaded type that the built type is guaranteed to be a subclass of.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        private abstract static class Adapter<V> extends Optional.Valuable.AbstractBase<V> {

                            /**
                             * The field attribute appender factory to apply.
                             */
                            protected final FieldAttributeAppender.Factory fieldAttributeAppenderFactory;

                            /**
                             * The field transformer to apply.
                             */
                            protected final Transformer<FieldDescription> transformer;

                            /**
                             * The field's default value or {@code null} if no value is to be defined.
                             */
                            @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
                            protected final Object defaultValue;

                            /**
                             * Creates a new field adapter.
                             *
                             * @param fieldAttributeAppenderFactory The field attribute appender factory to apply.
                             * @param transformer                   The field transformer to apply.
                             * @param defaultValue                  The field's default value or {@code null} if no value is to be defined.
                             */
                            protected Adapter(FieldAttributeAppender.Factory fieldAttributeAppenderFactory,
                                              Transformer<FieldDescription> transformer,
                                              Object defaultValue) {
                                this.fieldAttributeAppenderFactory = fieldAttributeAppenderFactory;
                                this.transformer = transformer;
                                this.defaultValue = defaultValue;
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public FieldDefinition.Optional<V> attribute(FieldAttributeAppender.Factory fieldAttributeAppenderFactory) {
                                return materialize(new FieldAttributeAppender.Factory.Compound(this.fieldAttributeAppenderFactory, fieldAttributeAppenderFactory), transformer, defaultValue);
                            }

                            /**
                             * {@inheritDoc}
                             */
                            @SuppressWarnings("unchecked") // In absence of @SafeVarargs
                            public FieldDefinition.Optional<V> transform(Transformer<FieldDescription> transformer) {
                                return materialize(fieldAttributeAppenderFactory, new Transformer.Compound<FieldDescription>(this.transformer, transformer), defaultValue);
                            }

                            @Override
                            protected FieldDefinition.Optional<V> defaultValue(Object defaultValue) {
                                return materialize(fieldAttributeAppenderFactory, transformer, defaultValue);
                            }

                            /**
                             * Creates a new optional field definition for which all of the supplied values are represented.
                             *
                             * @param fieldAttributeAppenderFactory The field attribute appender factory to apply.
                             * @param transformer                   The field transformer to apply.
                             * @param defaultValue                  The field's default value or {@code null} if no value is to be defined.
                             * @return A new field definition that represents the supplied values.
                             */
                            protected abstract FieldDefinition.Optional<V> materialize(FieldAttributeAppender.Factory fieldAttributeAppenderFactory,
                                                                                       Transformer<FieldDescription> transformer,
                                                                                       Object defaultValue);
                        }
                    }
                }

                /**
                 * An abstract base implementation for an optional field definition.
                 *
                 * @param <U> A loaded type that the built type is guaranteed to be a subclass of.
                 */
                abstract class AbstractBase<U> extends Builder.AbstractBase.Delegator<U> implements FieldDefinition.Optional<U> {

                    /**
                     * {@inheritDoc}
                     */
                    public FieldDefinition.Optional<U> annotateField(Annotation... annotation) {
                        return annotateField(Arrays.asList(annotation));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public FieldDefinition.Optional<U> annotateField(List<? extends Annotation> annotations) {
                        return annotateField(new AnnotationList.ForLoadedAnnotations(annotations));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public FieldDefinition.Optional<U> annotateField(AnnotationDescription... annotation) {
                        return annotateField(Arrays.asList(annotation));
                    }
                }
            }
        }

        /**
         * A builder for a method definition.
         *
         * @param <S> A loaded type that the built type is guaranteed to be a subclass of.
         */
        interface MethodDefinition<S> extends Builder<S> {

            /**
             * Annotates the previously defined or matched method with the supplied annotations.
             *
             * @param annotation The annotations to declare on the previously defined or matched method.
             * @return A new builder that is equal to this builder but with the given annotations declared
             * on the previously defined or matched method.
             */
            MethodDefinition<S> annotateMethod(Annotation... annotation);

            /**
             * Annotates the previously defined or matched method with the supplied annotations.
             *
             * @param annotations The annotations to declare on the previously defined or matched method.
             * @return A new builder that is equal to this builder but with the given annotations declared
             * on the previously defined or matched method.
             */
            MethodDefinition<S> annotateMethod(List<? extends Annotation> annotations);

            /**
             * Annotates the previously defined or matched method with the supplied annotations.
             *
             * @param annotation The annotations to declare on the previously defined or matched method.
             * @return A new builder that is equal to this builder but with the given annotations declared
             * on the previously defined or matched method.
             */
            MethodDefinition<S> annotateMethod(AnnotationDescription... annotation);

            /**
             * Annotates the previously defined or matched method with the supplied annotations.
             *
             * @param annotations The annotations to declare on the previously defined or matched method.
             * @return A new builder that is equal to this builder but with the given annotations declared
             * on the previously defined or matched method.
             */
            MethodDefinition<S> annotateMethod(Collection<? extends AnnotationDescription> annotations);

            /**
             * Annotates the parameter of the given index of the previously defined or matched method with the supplied annotations.
             *
             * @param index      The parameter's index.
             * @param annotation The annotations to declare on the previously defined or matched method.
             * @return A new builder that is equal to this builder but with the given annotations declared
             * on the previously defined or matched method's parameter of the given index.
             */
            MethodDefinition<S> annotateParameter(int index, Annotation... annotation);

            /**
             * Annotates the parameter of the given index of the previously defined or matched method with the supplied annotations.
             *
             * @param index       The parameter's index.
             * @param annotations The annotations to declare on the previously defined or matched method.
             * @return A new builder that is equal to this builder but with the given annotations declared
             * on the previously defined or matched method's parameter of the given index.
             */
            MethodDefinition<S> annotateParameter(int index, List<? extends Annotation> annotations);

            /**
             * Annotates the parameter of the given index of the previously defined or matched method with the supplied annotations.
             *
             * @param index      The parameter's index.
             * @param annotation The annotations to declare on the previously defined or matched method.
             * @return A new builder that is equal to this builder but with the given annotations declared
             * on the previously defined or matched method's parameter of the given index.
             */
            MethodDefinition<S> annotateParameter(int index, AnnotationDescription... annotation);

            /**
             * Annotates the parameter of the given index of the previously defined or matched method with the supplied annotations.
             *
             * @param index       The parameter's index.
             * @param annotations The annotations to declare on the previously defined or matched method.
             * @return A new builder that is equal to this builder but with the given annotations declared
             * on the previously defined or matched method's parameter of the given index.
             */
            MethodDefinition<S> annotateParameter(int index, Collection<? extends AnnotationDescription> annotations);

            /**
             * Applies the supplied method attribute appender factory onto the previously defined or matched method.
             *
             * @param methodAttributeAppenderFactory The method attribute appender factory that should be applied on the
             *                                       previously defined or matched method.
             * @return A new builder that is equal to this builder but with the supplied method attribute appender factory
             * applied to the previously defined or matched method.
             */
            MethodDefinition<S> attribute(MethodAttributeAppender.Factory methodAttributeAppenderFactory);

            /**
             * Applies the supplied transformer onto the previously defined or matched method. The transformed
             * method is written <i>as it is</i> and it not subject to any validations.
             *
             * @param transformer The transformer to apply to the previously defined or matched method.
             * @return A new builder that is equal to this builder but with the supplied transformer
             * applied to the previously defined or matched method.
             */
            MethodDefinition<S> transform(Transformer<MethodDescription> transformer);

            /**
             * A builder for a method definition with a receiver type.
             *
             * @param <U> A loaded type that the built type is guaranteed to be a subclass of.
             */
            interface ReceiverTypeDefinition<U> extends MethodDefinition<U> {

                /**
                 * Defines the supplied (annotated) receiver type for the previously defined or matched method.
                 *
                 * @param receiverType The receiver type to define on the previously defined or matched method.
                 * @return A new builder that is equal to this builder but with the given type defined as the
                 * receiver on the previously defined or matched method.
                 */
                MethodDefinition<U> receiverType(AnnotatedElement receiverType);

                /**
                 * Defines the supplied (annotated) receiver type for the previously defined or matched method.
                 *
                 * @param receiverType The receiver type to define on the previously defined or matched method.
                 * @return A new builder that is equal to this builder but with the given type defined as the
                 * receiver on the previously defined or matched method.
                 */
                MethodDefinition<U> receiverType(TypeDescription.Generic receiverType);

                /**
                 * An abstract base implementation of a method definition that can accept a receiver type.
                 *
                 * @param <V> A loaded type that the built type is guaranteed to be a subclass of.
                 */
                abstract class AbstractBase<V> extends MethodDefinition.AbstractBase<V> implements ReceiverTypeDefinition<V> {

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDefinition<V> receiverType(AnnotatedElement receiverType) {
                        return receiverType(TypeDescription.Generic.AnnotationReader.DISPATCHER.resolve(receiverType));
                    }
                }
            }

            /**
             * A builder for defining an implementation of a method.
             *
             * @param <U> A loaded type that the built type is guaranteed to be a subclass of.
             */
            interface ImplementationDefinition<U> {

                /**
                 * Implements the previously defined or matched method by the supplied implementation. A method interception
                 * is typically implemented in one of the following ways:
                 * <ol>
                 * <li>If a method is declared by the instrumented type and the type builder creates a subclass or redefinition,
                 * any preexisting method is replaced by the given implementation. Any previously defined implementation is lost.</li>
                 * <li>If a method is declared by the instrumented type and the type builder creates a rebased version of the
                 * instrumented type, the original method is preserved within a private, synthetic method within the instrumented
                 * type. The original method therefore remains invokeable and is treated as the direct super method of the new
                 * method. When rebasing a type, it therefore becomes possible to invoke a non-virtual method's super method
                 * when a preexisting method body is replaced.</li>
                 * <li>If a virtual method is inherited from a super type, it is overridden. The overridden method is available
                 * for super method invocation.</li>
                 * </ol>
                 *
                 * @param implementation The implementation for implementing the previously defined or matched method.
                 * @return A new builder where the previously defined or matched method is implemented by the
                 * supplied implementation.
                 */
                MethodDefinition.ReceiverTypeDefinition<U> intercept(Implementation implementation);

                /**
                 * Defines the previously defined or matched method not to declare a method body. This implies the
                 * method to be {@code abstract} unless it was already declared to be {@code native}.
                 *
                 * @return A new builder where the previously defined or matched method is implemented to be abstract.
                 */
                MethodDefinition.ReceiverTypeDefinition<U> withoutCode();

                /**
                 * Defines the previously defined or matched method to return the supplied value as an annotation default value. The
                 * value must be supplied in its unloaded state, i.e. enumerations as {@link net.bytebuddy.description.enumeration.EnumerationDescription},
                 * types as {@link TypeDescription} and annotations as {@link AnnotationDescription}. For supplying loaded types, use
                 * {@link ImplementationDefinition#defaultValue(Object, Class)} must be used.
                 *
                 * @param annotationValue The value to be defined as a default value.
                 * @return A builder where the previously defined or matched method is implemented to return an annotation default value.
                 */
                MethodDefinition.ReceiverTypeDefinition<U> defaultValue(AnnotationValue<?, ?> annotationValue);

                /**
                 * Defines the previously defined or matched method to return the supplied value as an annotation default value. The
                 * value must be supplied in its loaded state paired with the property type of the value.
                 *
                 * @param value The value to be defined as a default value.
                 * @param type  The type of the annotation property.
                 * @param <W>   The type of the annotation property.
                 * @return A builder where the previously defined or matched method is implemented to return an annotation default value.
                 */
                <W> MethodDefinition.ReceiverTypeDefinition<U> defaultValue(W value, Class<? extends W> type);

                /**
                 * A builder for optionally defining an implementation of a method.
                 *
                 * @param <V> A loaded type that the built type is guaranteed to be a subclass of.
                 */
                interface Optional<V> extends ImplementationDefinition<V>, Builder<V> {
                    /* union type */
                }

                /**
                 * An abstract base implementation for a builder optionally defining an implementation of a method.
                 *
                 * @param <V> A loaded type that the built type is guaranteed to be a subclass of.
                 */
                abstract class AbstractBase<V> implements ImplementationDefinition<V> {

                    /**
                     * {@inheritDoc}
                     */
                    public <W> MethodDefinition.ReceiverTypeDefinition<V> defaultValue(W value, Class<? extends W> type) {
                        return defaultValue(AnnotationDescription.ForLoadedAnnotation.asValue(value, type));
                    }
                }
            }

            /**
             * A builder for defining an implementation of a method and optionally defining a type variable.
             *
             * @param <U> A loaded type that the built type is guaranteed to be a subclass of.
             */
            interface TypeVariableDefinition<U> extends ImplementationDefinition<U> {

                /**
                 * Defines a method variable to be declared by the currently defined method. The defined method variable does not define any bounds.
                 *
                 * @param symbol The symbol of the type variable.
                 * @return A new builder that is equal to the current builder but where the currently defined method declares the specified type variable.
                 */
                Annotatable<U> typeVariable(String symbol);

                /**
                 * Defines a method variable to be declared by the currently defined method.
                 *
                 * @param symbol The symbol of the type variable.
                 * @param bound  The bounds of the type variables. Can also be {@link net.bytebuddy.dynamic.TargetType} for any type
                 *               if a bound type should be equal to the currently instrumented type.
                 * @return A new builder that is equal to the current builder but where the currently defined method declares the specified type variable.
                 */
                Annotatable<U> typeVariable(String symbol, Type... bound);

                /**
                 * Defines a method variable to be declared by the currently defined method.
                 *
                 * @param symbol The symbol of the type variable.
                 * @param bounds The bounds of the type variables. Can also be {@link net.bytebuddy.dynamic.TargetType} for any type
                 *               if a bound type should be equal to the currently instrumented type.
                 * @return A new builder that is equal to the current builder but where the currently defined method declares the specified type variable.
                 */
                Annotatable<U> typeVariable(String symbol, List<? extends Type> bounds);

                /**
                 * Defines a method variable to be declared by the currently defined method.
                 *
                 * @param symbol The symbol of the type variable.
                 * @param bound  The bounds of the type variables. Can also be {@link net.bytebuddy.dynamic.TargetType} for any type
                 *               if a bound type should be equal to the currently instrumented type.
                 * @return A new builder that is equal to the current builder but where the currently defined method declares the specified type variable.
                 */
                Annotatable<U> typeVariable(String symbol, TypeDefinition... bound);

                /**
                 * Defines a method variable to be declared by the currently defined method.
                 *
                 * @param symbol The symbol of the type variable.
                 * @param bounds The bounds of the type variables. Can also be {@link net.bytebuddy.dynamic.TargetType} for any type
                 *               if a bound type should be equal to the currently instrumented type.
                 * @return A new builder that is equal to the current builder but where the currently defined method declares the specified type variable.
                 */
                Annotatable<U> typeVariable(String symbol, Collection<? extends TypeDefinition> bounds);

                /**
                 * A builder for optionally defining an annotation for a type variable.
                 *
                 * @param <V> A loaded type that the built type is guaranteed to be a subclass of.
                 */
                interface Annotatable<V> extends TypeVariableDefinition<V> {

                    /**
                     * Annotates the previously defined type variable with the supplied annotations.
                     *
                     * @param annotation The annotations to declare on the previously defined type variable.
                     * @return A new builder that is equal to this builder but with the given annotations declared
                     * on the previously defined type variable.
                     */
                    Annotatable<V> annotateTypeVariable(Annotation... annotation);

                    /**
                     * Annotates the previously defined type variable with the supplied annotations.
                     *
                     * @param annotations The annotations to declare on the previously defined type variable.
                     * @return A new builder that is equal to this builder but with the given annotations declared
                     * on the previously defined type variable.
                     */
                    Annotatable<V> annotateTypeVariable(List<? extends Annotation> annotations);

                    /**
                     * Annotates the previously defined type variable with the supplied annotations.
                     *
                     * @param annotation The annotations to declare on the previously defined type variable.
                     * @return A new builder that is equal to this builder but with the given annotations declared
                     * on the previously defined type variable.
                     */
                    Annotatable<V> annotateTypeVariable(AnnotationDescription... annotation);

                    /**
                     * Annotates the previously defined type variable with the supplied annotations.
                     *
                     * @param annotations The annotations to declare on the previously defined type variable.
                     * @return A new builder that is equal to this builder but with the given annotations declared
                     * on the previously defined type variable.
                     */
                    Annotatable<V> annotateTypeVariable(Collection<? extends AnnotationDescription> annotations);

                    /**
                     * An abstract base implementation for defining an annotation on a parameter.
                     *
                     * @param <W> A loaded type that the built type is guaranteed to be a subclass of.
                     */
                    abstract class AbstractBase<W> extends TypeVariableDefinition.AbstractBase<W> implements Annotatable<W> {

                        /**
                         * {@inheritDoc}
                         */
                        public TypeVariableDefinition.Annotatable<W> annotateTypeVariable(Annotation... annotation) {
                            return annotateTypeVariable(Arrays.asList(annotation));
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public TypeVariableDefinition.Annotatable<W> annotateTypeVariable(List<? extends Annotation> annotations) {
                            return annotateTypeVariable(new AnnotationList.ForLoadedAnnotations(annotations));
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public TypeVariableDefinition.Annotatable<W> annotateTypeVariable(AnnotationDescription... annotation) {
                            return annotateTypeVariable(Arrays.asList(annotation));
                        }

                        /**
                         * An adapter implementation for an annotatable type variable definition.
                         *
                         * @param <X> A loaded type that the built type is guaranteed to be a subclass of.
                         */
                        protected abstract static class Adapter<X> extends TypeVariableDefinition.Annotatable.AbstractBase<X> {

                            /**
                             * {@inheritDoc}
                             */
                            public TypeVariableDefinition.Annotatable<X> typeVariable(String symbol, Collection<? extends TypeDefinition> bounds) {
                                return materialize().typeVariable(symbol, bounds);
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public MethodDefinition.ReceiverTypeDefinition<X> intercept(Implementation implementation) {
                                return materialize().intercept(implementation);
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public MethodDefinition.ReceiverTypeDefinition<X> withoutCode() {
                                return materialize().withoutCode();
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public MethodDefinition.ReceiverTypeDefinition<X> defaultValue(AnnotationValue<?, ?> annotationValue) {
                                return materialize().defaultValue(annotationValue);
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public <V> MethodDefinition.ReceiverTypeDefinition<X> defaultValue(V value, Class<? extends V> type) {
                                return materialize().defaultValue(value, type);
                            }

                            /**
                             * Materializes this instance as a parameter definition with the currently defined properties.
                             *
                             * @return A parameter definition with the currently defined properties.
                             */
                            protected abstract MethodDefinition.ParameterDefinition<X> materialize();
                        }
                    }
                }

                /**
                 * An abstract base implementation for defining an implementation of a method and optionally defining a type variable.
                 *
                 * @param <V> A loaded type that the built type is guaranteed to be a subclass of.
                 */
                abstract class AbstractBase<V> extends ImplementationDefinition.AbstractBase<V> implements TypeVariableDefinition<V> {

                    /**
                     * {@inheritDoc}
                     */
                    public Annotatable<V> typeVariable(String symbol) {
                        return typeVariable(symbol, Collections.singletonList(Object.class));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Annotatable<V> typeVariable(String symbol, Type... bound) {
                        return typeVariable(symbol, Arrays.asList(bound));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Annotatable<V> typeVariable(String symbol, List<? extends Type> bounds) {
                        return typeVariable(symbol, new TypeList.Generic.ForLoadedTypes(bounds));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Annotatable<V> typeVariable(String symbol, TypeDefinition... bound) {
                        return typeVariable(symbol, Arrays.asList(bound));
                    }
                }
            }

            /**
             * A builder for defining an implementation of a method and optionally defining a type variable or thrown exception.
             *
             * @param <U> A loaded type that the built type is guaranteed to be a subclass of.
             */
            interface ExceptionDefinition<U> extends TypeVariableDefinition<U> {

                /**
                 * Defines a method variable to be declared by the currently defined method.
                 *
                 * @param type The type of the exception being declared by the currently defined method.
                 * @return A new builder that is equal to the current builder but where the currently defined method declares the specified exception type.
                 */
                ExceptionDefinition<U> throwing(Type... type);

                /**
                 * Defines a method variable to be declared by the currently defined method.
                 *
                 * @param types The type of the exception being declared by the currently defined method.
                 * @return A new builder that is equal to the current builder but where the currently defined method declares the specified exception type.
                 */
                ExceptionDefinition<U> throwing(List<? extends Type> types);

                /**
                 * Defines a method variable to be declared by the currently defined method.
                 *
                 * @param type The type of the exception being declared by the currently defined method.
                 * @return A new builder that is equal to the current builder but where the currently defined method declares the specified exception type.
                 */
                ExceptionDefinition<U> throwing(TypeDefinition... type);

                /**
                 * Defines a method variable to be declared by the currently defined method.
                 *
                 * @param types The type of the exception being declared by the currently defined method.
                 * @return A new builder that is equal to the current builder but where the currently defined method declares the specified exception type.
                 */
                ExceptionDefinition<U> throwing(Collection<? extends TypeDefinition> types);

                /**
                 * An abstract base implementation for defining an implementation of a method and optionally defining a type variable or thrown exception.
                 *
                 * @param <V> A loaded type that the built type is guaranteed to be a subclass of.
                 */
                abstract class AbstractBase<V> extends TypeVariableDefinition.AbstractBase<V> implements ExceptionDefinition<V> {

                    /**
                     * {@inheritDoc}
                     */
                    public ExceptionDefinition<V> throwing(Type... type) {
                        return throwing(Arrays.asList(type));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public ExceptionDefinition<V> throwing(List<? extends Type> types) {
                        return throwing(new TypeList.Generic.ForLoadedTypes(types));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public ExceptionDefinition<V> throwing(TypeDefinition... type) {
                        return throwing(Arrays.asList(type));
                    }
                }
            }

            /**
             * A builder for defining an implementation of a method and optionally defining a type variable, thrown exception or method parameter.
             *
             * @param <U> A loaded type that the built type is guaranteed to be a subclass of.
             */
            interface ParameterDefinition<U> extends ExceptionDefinition<U> {

                /**
                 * Defines the specified parameter for the currently defined method as the last parameter of the currently defined method.
                 *
                 * @param type                The parameter's type. Can also be {@link net.bytebuddy.dynamic.TargetType} if the parameter type
                 *                            should be equal to the currently instrumented type.
                 * @param name                The parameter's name.
                 * @param modifierContributor The parameter's modifiers.
                 * @return A new builder that is equal to the current builder but where the currently defined method appends the specified parameter.
                 */
                Annotatable<U> withParameter(Type type, String name, ModifierContributor.ForParameter... modifierContributor);

                /**
                 * Defines the specified parameter for the currently defined method as the last parameter of the currently defined method.
                 *
                 * @param type                 The parameter's type. Can also be {@link net.bytebuddy.dynamic.TargetType} if the parameter type
                 *                             should be equal to the currently instrumented type.
                 * @param name                 The parameter's name.
                 * @param modifierContributors The parameter's modifiers.
                 * @return A new builder that is equal to the current builder but where the currently defined method appends the specified parameter.
                 */
                Annotatable<U> withParameter(Type type, String name, Collection<? extends ModifierContributor.ForParameter> modifierContributors);

                /**
                 * Defines the specified parameter for the currently defined method as the last parameter of the currently defined method.
                 *
                 * @param type      The parameter's type. Can also be {@link net.bytebuddy.dynamic.TargetType} if the parameter type
                 *                  should be equal to the currently instrumented type.
                 * @param name      The parameter's name.
                 * @param modifiers The parameter's modifiers.
                 * @return A new builder that is equal to the current builder but where the currently defined method appends the specified parameter.
                 */
                Annotatable<U> withParameter(Type type, String name, int modifiers);

                /**
                 * Defines the specified parameter for the currently defined method as the last parameter of the currently defined method.
                 *
                 * @param type                The parameter's type. Can also be {@link net.bytebuddy.dynamic.TargetType} if the parameter type
                 *                            should be equal to the currently instrumented type.
                 * @param name                The parameter's name.
                 * @param modifierContributor The parameter's modifiers.
                 * @return A new builder that is equal to the current builder but where the currently defined method appends the specified parameter.
                 */
                Annotatable<U> withParameter(TypeDefinition type, String name, ModifierContributor.ForParameter... modifierContributor);

                /**
                 * Defines the specified parameter for the currently defined method as the last parameter of the currently defined method.
                 *
                 * @param type                 The parameter's type. Can also be {@link net.bytebuddy.dynamic.TargetType} if the parameter type
                 *                             should be equal to the currently instrumented type.
                 * @param name                 The parameter's name.
                 * @param modifierContributors The parameter's modifiers.
                 * @return A new builder that is equal to the current builder but where the currently defined method appends the specified parameter.
                 */
                Annotatable<U> withParameter(TypeDefinition type, String name, Collection<? extends ModifierContributor.ForParameter> modifierContributors);

                /**
                 * Defines the specified parameter for the currently defined method as the last parameter of the currently defined method.
                 *
                 * @param type      The parameter's type. Can also be {@link net.bytebuddy.dynamic.TargetType} if the parameter type
                 *                  should be equal to the currently instrumented type.
                 * @param name      The parameter's name.
                 * @param modifiers The parameter's modifiers.
                 * @return A new builder that is equal to the current builder but where the currently defined method appends the specified parameter.
                 */
                Annotatable<U> withParameter(TypeDefinition type, String name, int modifiers);

                /**
                 * A builder for optionally defining an annotation on a parameter.
                 *
                 * @param <V> A loaded type that the built type is guaranteed to be a subclass of.
                 */
                interface Annotatable<V> extends ParameterDefinition<V> {

                    /**
                     * Annotates the previously defined parameter with the specified annotations.
                     *
                     * @param annotation The annotations to declare on the previously defined parameter.
                     * @return A new builder that is equal to this builder but with the previously defined parameter annotated with
                     * the specified annotations.
                     */
                    Annotatable<V> annotateParameter(Annotation... annotation);

                    /**
                     * Annotates the previously defined parameter with the specified annotations.
                     *
                     * @param annotations The annotations to declare on the previously defined parameter.
                     * @return A new builder that is equal to this builder but with the previously defined parameter annotated with
                     * the specified annotations.
                     */
                    Annotatable<V> annotateParameter(List<? extends Annotation> annotations);

                    /**
                     * Annotates the previously defined parameter with the specified annotations.
                     *
                     * @param annotation The annotations to declare on the previously defined parameter.
                     * @return A new builder that is equal to this builder but with the previously defined parameter annotated with
                     * the specified annotations.
                     */
                    Annotatable<V> annotateParameter(AnnotationDescription... annotation);

                    /**
                     * Annotates the previously defined parameter with the specified annotations.
                     *
                     * @param annotations The annotations to declare on the previously defined parameter.
                     * @return A new builder that is equal to this builder but with the previously defined parameter annotated with
                     * the specified annotations.
                     */
                    Annotatable<V> annotateParameter(Collection<? extends AnnotationDescription> annotations);

                    /**
                     * An abstract base implementation for defining an annotation on a parameter.
                     *
                     * @param <W> A loaded type that the built type is guaranteed to be a subclass of.
                     */
                    abstract class AbstractBase<W> extends ParameterDefinition.AbstractBase<W> implements Annotatable<W> {

                        /**
                         * {@inheritDoc}
                         */
                        public ParameterDefinition.Annotatable<W> annotateParameter(Annotation... annotation) {
                            return annotateParameter(Arrays.asList(annotation));
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public ParameterDefinition.Annotatable<W> annotateParameter(List<? extends Annotation> annotations) {
                            return annotateParameter(new AnnotationList.ForLoadedAnnotations(annotations));
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public ParameterDefinition.Annotatable<W> annotateParameter(AnnotationDescription... annotation) {
                            return annotateParameter(Arrays.asList(annotation));
                        }

                        /**
                         * An adapter implementation for defining an annotation on a parameter.
                         *
                         * @param <X> A loaded type that the built type is guaranteed to be a subclass of.
                         */
                        protected abstract static class Adapter<X> extends ParameterDefinition.Annotatable.AbstractBase<X> {

                            /**
                             * {@inheritDoc}
                             */
                            public ParameterDefinition.Annotatable<X> withParameter(TypeDefinition type, String name, int modifiers) {
                                return materialize().withParameter(type, name, modifiers);
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public ExceptionDefinition<X> throwing(Collection<? extends TypeDefinition> types) {
                                return materialize().throwing(types);
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public TypeVariableDefinition.Annotatable<X> typeVariable(String symbol, Collection<? extends TypeDefinition> bounds) {
                                return materialize().typeVariable(symbol, bounds);
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public MethodDefinition.ReceiverTypeDefinition<X> intercept(Implementation implementation) {
                                return materialize().intercept(implementation);
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public MethodDefinition.ReceiverTypeDefinition<X> withoutCode() {
                                return materialize().withoutCode();
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public MethodDefinition.ReceiverTypeDefinition<X> defaultValue(AnnotationValue<?, ?> annotationValue) {
                                return materialize().defaultValue(annotationValue);
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public <V> MethodDefinition.ReceiverTypeDefinition<X> defaultValue(V value, Class<? extends V> type) {
                                return materialize().defaultValue(value, type);
                            }

                            /**
                             * Materializes this instance as a parameter definition with the currently defined properties.
                             *
                             * @return A parameter definition with the currently defined properties.
                             */
                            protected abstract MethodDefinition.ParameterDefinition<X> materialize();
                        }
                    }
                }

                /**
                 * A builder for defining an implementation of a method and optionally defining a type variable, thrown exception or a parameter type.
                 *
                 * @param <V> A loaded type that the built type is guaranteed to be a subclass of.
                 */
                interface Simple<V> extends ExceptionDefinition<V> {

                    /**
                     * Defines the specified parameter for the currently defined method as the last parameter of the currently defined method.
                     *
                     * @param type The parameter's type. Can also be {@link net.bytebuddy.dynamic.TargetType} if the parameter type
                     *             should be equal to the currently instrumented type.
                     * @return A new builder that is equal to the current builder but where the currently defined method appends the specified parameter.
                     */
                    Annotatable<V> withParameter(Type type);

                    /**
                     * Defines the specified parameter for the currently defined method as the last parameter of the currently defined method.
                     *
                     * @param type The parameter's type. Can also be {@link net.bytebuddy.dynamic.TargetType} if the parameter type
                     *             should be equal to the currently instrumented type.
                     * @return A new builder that is equal to the current builder but where the currently defined method appends the specified parameter.
                     */
                    Annotatable<V> withParameter(TypeDefinition type);

                    /**
                     * A builder for optionally defining an annotation on a parameter.
                     *
                     * @param <V> A loaded type that the built type is guaranteed to be a subclass of.
                     */
                    interface Annotatable<V> extends Simple<V> {

                        /**
                         * Annotates the previously defined parameter with the specified annotations.
                         *
                         * @param annotation The annotations to declare on the previously defined parameter.
                         * @return A new builder that is equal to this builder but with the previously defined parameter annotated with
                         * the specified annotations.
                         */
                        Annotatable<V> annotateParameter(Annotation... annotation);

                        /**
                         * Annotates the previously defined parameter with the specified annotations.
                         *
                         * @param annotations The annotations to declare on the previously defined parameter.
                         * @return A new builder that is equal to this builder but with the previously defined parameter annotated with
                         * the specified annotations.
                         */
                        Annotatable<V> annotateParameter(List<? extends Annotation> annotations);

                        /**
                         * Annotates the previously defined parameter with the specified annotations.
                         *
                         * @param annotation The annotations to declare on the previously defined parameter.
                         * @return A new builder that is equal to this builder but with the previously defined parameter annotated with
                         * the specified annotations.
                         */
                        Annotatable<V> annotateParameter(AnnotationDescription... annotation);

                        /**
                         * Annotates the previously defined parameter with the specified annotations.
                         *
                         * @param annotations The annotations to declare on the previously defined parameter.
                         * @return A new builder that is equal to this builder but with the previously defined parameter annotated with
                         * the specified annotations.
                         */
                        Annotatable<V> annotateParameter(Collection<? extends AnnotationDescription> annotations);

                        /**
                         * An abstract base implementation of a simple parameter definition.
                         *
                         * @param <W> A loaded type that the built type is guaranteed to be a subclass of.
                         */
                        abstract class AbstractBase<W> extends Simple.AbstractBase<W> implements Annotatable<W> {

                            /**
                             * {@inheritDoc}
                             */
                            public Simple.Annotatable<W> annotateParameter(Annotation... annotation) {
                                return annotateParameter(Arrays.asList(annotation));
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public Simple.Annotatable<W> annotateParameter(List<? extends Annotation> annotations) {
                                return annotateParameter(new AnnotationList.ForLoadedAnnotations(annotations));
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public Simple.Annotatable<W> annotateParameter(AnnotationDescription... annotation) {
                                return annotateParameter(Arrays.asList(annotation));
                            }

                            /**
                             * An adapter implementation of a simple parameter definition.
                             *
                             * @param <X> A loaded type that the built type is guaranteed to be a subclass of.
                             */
                            protected abstract static class Adapter<X> extends Simple.Annotatable.AbstractBase<X> {

                                /**
                                 * {@inheritDoc}
                                 */
                                public Simple.Annotatable<X> withParameter(TypeDefinition type) {
                                    return materialize().withParameter(type);
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public ExceptionDefinition<X> throwing(Collection<? extends TypeDefinition> types) {
                                    return materialize().throwing(types);
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public TypeVariableDefinition.Annotatable<X> typeVariable(String symbol, Collection<? extends TypeDefinition> bounds) {
                                    return materialize().typeVariable(symbol, bounds);
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public MethodDefinition.ReceiverTypeDefinition<X> intercept(Implementation implementation) {
                                    return materialize().intercept(implementation);
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public MethodDefinition.ReceiverTypeDefinition<X> withoutCode() {
                                    return materialize().withoutCode();
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public MethodDefinition.ReceiverTypeDefinition<X> defaultValue(AnnotationValue<?, ?> annotationValue) {
                                    return materialize().defaultValue(annotationValue);
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public <V> MethodDefinition.ReceiverTypeDefinition<X> defaultValue(V value, Class<? extends V> type) {
                                    return materialize().defaultValue(value, type);
                                }

                                /**
                                 * Materializes this instance as a simple parameter definition with the currently defined properties.
                                 *
                                 * @return A simple parameter definition with the currently defined properties.
                                 */
                                protected abstract MethodDefinition.ParameterDefinition.Simple<X> materialize();
                            }
                        }
                    }

                    /**
                     * An abstract base implementation of an exception definition.
                     *
                     * @param <W> A loaded type that the built type is guaranteed to be a subclass of.
                     */
                    abstract class AbstractBase<W> extends ExceptionDefinition.AbstractBase<W> implements Simple<W> {

                        /**
                         * {@inheritDoc}
                         */
                        public Simple.Annotatable<W> withParameter(Type type) {
                            return withParameter(TypeDefinition.Sort.describe(type));
                        }
                    }
                }

                /**
                 * A builder for defining an implementation of a method and optionally defining a type variable, thrown exception or method parameter.
                 * Implementations allow for the <i>one-by-one</i> definition of parameters what gives opportunity to annotate parameters in a fluent
                 * style. Doing so, it is optionally possible to define parameter names and modifiers. This can be done for either all or no parameters.
                 * Alternatively, parameters without annotations, names or modifiers can be defined by a single step.
                 *
                 * @param <V> A loaded type that the built type is guaranteed to be a subclass of.
                 */
                interface Initial<V> extends ParameterDefinition<V>, Simple<V> {

                    /**
                     * Defines the specified parameters for the currently defined method.
                     *
                     * @param type The parameter types. Any type can also be {@link net.bytebuddy.dynamic.TargetType} if the parameter type
                     *             should be equal to the currently instrumented type.
                     * @return A new builder that is equal to the current builder but where the currently defined method appends the specified parameters.
                     */
                    ExceptionDefinition<V> withParameters(Type... type);

                    /**
                     * Defines the specified parameters for the currently defined method.
                     *
                     * @param types The parameter types. Any type can also be {@link net.bytebuddy.dynamic.TargetType} if the parameter type
                     *              should be equal to the currently instrumented type.
                     * @return A new builder that is equal to the current builder but where the currently defined method appends the specified parameters.
                     */
                    ExceptionDefinition<V> withParameters(List<? extends Type> types);

                    /**
                     * Defines the specified parameters for the currently defined method.
                     *
                     * @param type The parameter types. Any type can also be {@link net.bytebuddy.dynamic.TargetType} if the parameter type
                     *             should be equal to the currently instrumented type.
                     * @return A new builder that is equal to the current builder but where the currently defined method appends the specified parameters.
                     */
                    ExceptionDefinition<V> withParameters(TypeDefinition... type);

                    /**
                     * Defines the specified parameters for the currently defined method.
                     *
                     * @param types The parameter types. Any type can also be {@link net.bytebuddy.dynamic.TargetType} if the parameter type
                     *              should be equal to the currently instrumented type.
                     * @return A new builder that is equal to the current builder but where the currently defined method appends the specified parameters.
                     */
                    ExceptionDefinition<V> withParameters(Collection<? extends TypeDefinition> types);

                    /**
                     * An abstract base implementation for an initial parameter definition.
                     *
                     * @param <W> A loaded type that the built type is guaranteed to be a subclass of.
                     */
                    abstract class AbstractBase<W> extends ParameterDefinition.AbstractBase<W> implements Initial<W> {

                        /**
                         * {@inheritDoc}
                         */
                        public Simple.Annotatable<W> withParameter(Type type) {
                            return withParameter(TypeDefinition.Sort.describe(type));
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public ExceptionDefinition<W> withParameters(Type... type) {
                            return withParameters(Arrays.asList(type));
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public ExceptionDefinition<W> withParameters(List<? extends Type> types) {
                            return withParameters(new TypeList.Generic.ForLoadedTypes(types));
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public ExceptionDefinition<W> withParameters(TypeDefinition... type) {
                            return withParameters(Arrays.asList(type));
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public ExceptionDefinition<W> withParameters(Collection<? extends TypeDefinition> types) {
                            ParameterDefinition.Simple<W> parameterDefinition = this;
                            for (TypeDefinition type : types) {
                                parameterDefinition = parameterDefinition.withParameter(type);
                            }
                            return parameterDefinition;
                        }
                    }
                }

                /**
                 * An abstract base implementation for defining an implementation of a method and optionally defining a type variable, thrown exception or parameter type.
                 *
                 * @param <V> A loaded type that the built type is guaranteed to be a subclass of.
                 */
                abstract class AbstractBase<V> extends ExceptionDefinition.AbstractBase<V> implements ParameterDefinition<V> {

                    /**
                     * {@inheritDoc}
                     */
                    public ParameterDefinition.Annotatable<V> withParameter(Type type, String name, ModifierContributor.ForParameter... modifierContributor) {
                        return withParameter(type, name, Arrays.asList(modifierContributor));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public ParameterDefinition.Annotatable<V> withParameter(Type type, String name, Collection<? extends ModifierContributor.ForParameter> modifierContributors) {
                        return withParameter(type, name, ModifierContributor.Resolver.of(modifierContributors).resolve());
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public ParameterDefinition.Annotatable<V> withParameter(Type type, String name, int modifiers) {
                        return withParameter(TypeDefinition.Sort.describe(type), name, modifiers);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public ParameterDefinition.Annotatable<V> withParameter(TypeDefinition type, String name, ModifierContributor.ForParameter... modifierContributor) {
                        return withParameter(type, name, Arrays.asList(modifierContributor));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public ParameterDefinition.Annotatable<V> withParameter(TypeDefinition type, String name, Collection<? extends ModifierContributor.ForParameter> modifierContributors) {
                        return withParameter(type, name, ModifierContributor.Resolver.of(modifierContributors).resolve());
                    }
                }
            }

            /**
             * An abstract base implementation of a method definition.
             *
             * @param <U> A loaded type that the built type is guaranteed to be a subclass of.
             */
            abstract class AbstractBase<U> extends Builder.AbstractBase.Delegator<U> implements MethodDefinition<U> {

                /**
                 * {@inheritDoc}
                 */
                public MethodDefinition<U> annotateMethod(Annotation... annotation) {
                    return annotateMethod(Arrays.asList(annotation));
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodDefinition<U> annotateMethod(List<? extends Annotation> annotations) {
                    return annotateMethod(new AnnotationList.ForLoadedAnnotations(annotations));
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodDefinition<U> annotateMethod(AnnotationDescription... annotation) {
                    return annotateMethod(Arrays.asList(annotation));
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodDefinition<U> annotateParameter(int index, Annotation... annotation) {
                    return annotateParameter(index, Arrays.asList(annotation));
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodDefinition<U> annotateParameter(int index, List<? extends Annotation> annotations) {
                    return annotateParameter(index, new AnnotationList.ForLoadedAnnotations(annotations));
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodDefinition<U> annotateParameter(int index, AnnotationDescription... annotation) {
                    return annotateParameter(index, Arrays.asList(annotation));
                }

                /**
                 * An adapter implementation of a method definition.
                 *
                 * @param <V> A loaded type that the built type is guaranteed to be a subclass of.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                protected abstract static class Adapter<V> extends MethodDefinition.ReceiverTypeDefinition.AbstractBase<V> {

                    /**
                     * The handler that determines how a method is implemented.
                     */
                    protected final MethodRegistry.Handler handler;

                    /**
                     * The method attribute appender factory to apply onto the method that is currently being implemented.
                     */
                    protected final MethodAttributeAppender.Factory methodAttributeAppenderFactory;

                    /**
                     * The transformer to apply onto the method that is currently being implemented.
                     */
                    protected final Transformer<MethodDescription> transformer;

                    /**
                     * Creates a new adapter for a method definition.
                     *
                     * @param handler                        The handler that determines how a method is implemented.
                     * @param methodAttributeAppenderFactory The method attribute appender factory to apply onto the method that is currently being implemented.
                     * @param transformer                    The transformer to apply onto the method that is currently being implemented.
                     */
                    protected Adapter(MethodRegistry.Handler handler,
                                      MethodAttributeAppender.Factory methodAttributeAppenderFactory,
                                      Transformer<MethodDescription> transformer) {
                        this.handler = handler;
                        this.methodAttributeAppenderFactory = methodAttributeAppenderFactory;
                        this.transformer = transformer;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDefinition<V> attribute(MethodAttributeAppender.Factory methodAttributeAppenderFactory) {
                        return materialize(handler, new MethodAttributeAppender.Factory.Compound(this.methodAttributeAppenderFactory, methodAttributeAppenderFactory), transformer);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @SuppressWarnings("unchecked") // In absence of @SafeVarargs
                    public MethodDefinition<V> transform(Transformer<MethodDescription> transformer) {
                        return materialize(handler, methodAttributeAppenderFactory, new Transformer.Compound<MethodDescription>(this.transformer, transformer));
                    }

                    /**
                     * Materializes the current builder as a method definition.
                     *
                     * @param handler                        The handler that determines how a method is implemented.
                     * @param methodAttributeAppenderFactory The method attribute appender factory to apply onto the method that is currently being implemented.
                     * @param transformer                    The method transformer to apply onto the method that is currently being implemented.
                     * @return Returns a method definition for the supplied properties.
                     */
                    protected abstract MethodDefinition<V> materialize(MethodRegistry.Handler handler,
                                                                       MethodAttributeAppender.Factory methodAttributeAppenderFactory,
                                                                       Transformer<MethodDescription> transformer);
                }
            }
        }

        /**
         * A builder for a record component definition.
         *
         * @param <S> A loaded type that the built type is guaranteed to be a subclass of.
         */
        interface RecordComponentDefinition<S> {

            /**
             * Annotates the record component with the supplied annotations.
             *
             * @param annotation The annotations to declare.
             * @return A new builder that is equal to this builder but where the defined component declares the supplied annotations.
             */
            Optional<S> annotateRecordComponent(Annotation... annotation);

            /**
             * Annotates the record component with the supplied annotations.
             *
             * @param annotations The annotations to declare.
             * @return A new builder that is equal to this builder but where the defined component declares the supplied annotations.
             */
            Optional<S> annotateRecordComponent(List<? extends Annotation> annotations);

            /**
             * Annotates the record component with the supplied annotations.
             *
             * @param annotation The annotations to declare.
             * @return A new builder that is equal to this builder but where the defined component declares the supplied annotations.
             */
            Optional<S> annotateRecordComponent(AnnotationDescription... annotation);

            /**
             * Annotates the record component with the supplied annotations.
             *
             * @param annotations The annotations to declare.
             * @return A new builder that is equal to this builder but where the defined component declares the supplied annotations.
             */
            Optional<S> annotateRecordComponent(Collection<? extends AnnotationDescription> annotations);

            /**
             * Applies the supplied record component attribute appender factory onto the previously defined record component.
             *
             * @param recordComponentAttributeAppenderFactory The record component attribute appender factory that should be applied on the
             *                                                previously defined or matched method.
             * @return A new builder that is equal to this builder but with the supplied record component attribute appender factory
             * applied to the previously defined record component.
             */
            Optional<S> attribute(RecordComponentAttributeAppender.Factory recordComponentAttributeAppenderFactory);

            /**
             * Transforms a record component description before writing.
             *
             * @param transformer The transformer to apply.
             * @return new builder that is equal to this builder but with the supplied transformer being applied.
             */
            Optional<S> transform(Transformer<RecordComponentDescription> transformer);

            /**
             * A {@link RecordComponentDefinition} as an optional build step.
             *
             * @param <U> A loaded type that the built type is guaranteed to be a subclass of.
             */
            interface Optional<U> extends RecordComponentDefinition<U>, Builder<U> {

                /**
                 * An abstract base implementation of a record definition.
                 *
                 * @param <U> A loaded type that the built type is guaranteed to be a subclass of.
                 */
                abstract class AbstractBase<U> extends Builder.AbstractBase.Delegator<U> implements RecordComponentDefinition.Optional<U> {

                    /**
                     * {@inheritDoc}
                     */
                    public Optional<U> annotateRecordComponent(Annotation... annotation) {
                        return annotateRecordComponent(Arrays.asList(annotation));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Optional<U> annotateRecordComponent(List<? extends Annotation> annotations) {
                        return annotateRecordComponent(new AnnotationList.ForLoadedAnnotations(annotations));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Optional<U> annotateRecordComponent(AnnotationDescription... annotation) {
                        return annotateRecordComponent(Arrays.asList(annotation));
                    }
                }
            }
        }

        /**
         * An abstract base implementation of a dynamic type builder.
         *
         * @param <S> A loaded type that the built type is guaranteed to be a subclass of.
         */
        abstract class AbstractBase<S> implements Builder<S> {

            /**
             * {@inheritDoc}
             */
            public InnerTypeDefinition.ForType<S> innerTypeOf(Class<?> type) {
                return innerTypeOf(TypeDescription.ForLoadedType.of(type));
            }

            /**
             * {@inheritDoc}
             */
            public InnerTypeDefinition<S> innerTypeOf(Method method) {
                return innerTypeOf(new MethodDescription.ForLoadedMethod(method));
            }

            /**
             * {@inheritDoc}
             */
            public InnerTypeDefinition<S> innerTypeOf(Constructor<?> constructor) {
                return innerTypeOf(new MethodDescription.ForLoadedConstructor(constructor));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> declaredTypes(Class<?>... type) {
                return declaredTypes(Arrays.asList(type));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> declaredTypes(TypeDescription... type) {
                return declaredTypes(Arrays.asList(type));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> declaredTypes(List<? extends Class<?>> type) {
                return declaredTypes(new TypeList.ForLoadedTypes(type));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> noNestMate() {
                return nestHost(TargetType.DESCRIPTION);
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> nestHost(Class<?> type) {
                return nestHost(TypeDescription.ForLoadedType.of(type));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> nestMembers(Class<?>... type) {
                return nestMembers(Arrays.asList(type));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> nestMembers(TypeDescription... type) {
                return nestMembers(Arrays.asList(type));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> nestMembers(List<? extends Class<?>> types) {
                return nestMembers(new TypeList.ForLoadedTypes(types));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> permittedSubclass(Class<?>... type) {
                return permittedSubclass(Arrays.asList(type));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> permittedSubclass(TypeDescription... type) {
                return permittedSubclass(Arrays.asList(type));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> permittedSubclass(List<? extends Class<?>> types) {
                return permittedSubclass(new TypeList.ForLoadedTypes(types));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> annotateType(Annotation... annotation) {
                return annotateType(Arrays.asList(annotation));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> annotateType(List<? extends Annotation> annotations) {
                return annotateType(new AnnotationList.ForLoadedAnnotations(annotations));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> annotateType(AnnotationDescription... annotation) {
                return annotateType(Arrays.asList(annotation));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> modifiers(ModifierContributor.ForType... modifierContributor) {
                return modifiers(Arrays.asList(modifierContributor));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> modifiers(Collection<? extends ModifierContributor.ForType> modifierContributors) {
                return modifiers(ModifierContributor.Resolver.of(modifierContributors).resolve());
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> merge(ModifierContributor.ForType... modifierContributor) {
                return merge(Arrays.asList(modifierContributor));
            }

            /**
             * {@inheritDoc}
             */
            public MethodDefinition.ImplementationDefinition.Optional<S> implement(Type... interfaceType) {
                return implement(Arrays.asList(interfaceType));
            }

            /**
             * {@inheritDoc}
             */
            public MethodDefinition.ImplementationDefinition.Optional<S> implement(List<? extends Type> interfaceTypes) {
                return implement(new TypeList.Generic.ForLoadedTypes(interfaceTypes));
            }

            /**
             * {@inheritDoc}
             */
            public MethodDefinition.ImplementationDefinition.Optional<S> implement(TypeDefinition... interfaceType) {
                return implement(Arrays.asList(interfaceType));
            }

            /**
             * {@inheritDoc}
             */
            public TypeVariableDefinition<S> typeVariable(String symbol) {
                return typeVariable(symbol, TypeDescription.Generic.OBJECT);
            }

            /**
             * {@inheritDoc}
             */
            public TypeVariableDefinition<S> typeVariable(String symbol, Type... bound) {
                return typeVariable(symbol, Arrays.asList(bound));
            }

            /**
             * {@inheritDoc}
             */
            public TypeVariableDefinition<S> typeVariable(String symbol, List<? extends Type> bounds) {
                return typeVariable(symbol, new TypeList.Generic.ForLoadedTypes(bounds));
            }

            /**
             * {@inheritDoc}
             */
            public TypeVariableDefinition<S> typeVariable(String symbol, TypeDefinition... bound) {
                return typeVariable(symbol, Arrays.asList(bound));
            }

            /**
             * {@inheritDoc}
             */
            public RecordComponentDefinition.Optional<S> defineRecordComponent(String name, Type type) {
                return defineRecordComponent(name, TypeDefinition.Sort.describe(type));
            }

            /**
             * {@inheritDoc}
             */
            public RecordComponentDefinition.Optional<S> define(RecordComponentDescription recordComponentDescription) {
                return defineRecordComponent(recordComponentDescription.getActualName(), recordComponentDescription.getType());
            }

            /**
             * {@inheritDoc}
             */
            public RecordComponentDefinition<S> recordComponent(ElementMatcher<? super RecordComponentDescription> matcher) {
                return recordComponent(new LatentMatcher.Resolved<RecordComponentDescription>(matcher));
            }

            /**
             * {@inheritDoc}
             */
            public FieldDefinition.Optional.Valuable<S> defineField(String name, Type type, ModifierContributor.ForField... modifierContributor) {
                return defineField(name, type, Arrays.asList(modifierContributor));
            }

            /**
             * {@inheritDoc}
             */
            public FieldDefinition.Optional.Valuable<S> defineField(String name, Type type, Collection<? extends ModifierContributor.ForField> modifierContributors) {
                return defineField(name, type, ModifierContributor.Resolver.of(modifierContributors).resolve());
            }

            /**
             * {@inheritDoc}
             */
            public FieldDefinition.Optional.Valuable<S> defineField(String name, Type type, int modifiers) {
                return defineField(name, TypeDefinition.Sort.describe(type), modifiers);
            }

            /**
             * {@inheritDoc}
             */
            public FieldDefinition.Optional.Valuable<S> defineField(String name, TypeDefinition type, ModifierContributor.ForField... modifierContributor) {
                return defineField(name, type, Arrays.asList(modifierContributor));
            }

            /**
             * {@inheritDoc}
             */
            public FieldDefinition.Optional.Valuable<S> defineField(String name, TypeDefinition type, Collection<? extends ModifierContributor.ForField> modifierContributors) {
                return defineField(name, type, ModifierContributor.Resolver.of(modifierContributors).resolve());
            }

            /**
             * {@inheritDoc}
             */
            public FieldDefinition.Optional.Valuable<S> define(Field field) {
                return define(new FieldDescription.ForLoadedField(field));
            }

            /**
             * {@inheritDoc}
             */
            public FieldDefinition.Optional.Valuable<S> define(FieldDescription field) {
                return defineField(field.getName(), field.getType(), field.getModifiers());
            }

            /**
             * {@inheritDoc}
             */
            public FieldDefinition.Optional<S> serialVersionUid(long serialVersionUid) {
                return defineField("serialVersionUID", long.class, Visibility.PRIVATE, FieldManifestation.FINAL, Ownership.STATIC).value(serialVersionUid);
            }

            /**
             * {@inheritDoc}
             */
            public FieldDefinition.Valuable<S> field(ElementMatcher<? super FieldDescription> matcher) {
                return field(new LatentMatcher.Resolved<FieldDescription>(matcher));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> ignoreAlso(ElementMatcher<? super MethodDescription> ignoredMethods) {
                return ignoreAlso(new LatentMatcher.Resolved<MethodDescription>(ignoredMethods));
            }

            /**
             * {@inheritDoc}
             */
            public MethodDefinition.ParameterDefinition.Initial<S> defineMethod(String name, Type returnType, ModifierContributor.ForMethod... modifierContributor) {
                return defineMethod(name, returnType, Arrays.asList(modifierContributor));
            }

            /**
             * {@inheritDoc}
             */
            public MethodDefinition.ParameterDefinition.Initial<S> defineMethod(String name, Type returnType, Collection<? extends ModifierContributor.ForMethod> modifierContributors) {
                return defineMethod(name, returnType, ModifierContributor.Resolver.of(modifierContributors).resolve());
            }

            /**
             * {@inheritDoc}
             */
            public MethodDefinition.ParameterDefinition.Initial<S> defineMethod(String name, Type returnType, int modifiers) {
                return defineMethod(name, TypeDefinition.Sort.describe(returnType), modifiers);
            }

            /**
             * {@inheritDoc}
             */
            public MethodDefinition.ParameterDefinition.Initial<S> defineMethod(String name, TypeDefinition returnType, ModifierContributor.ForMethod... modifierContributor) {
                return defineMethod(name, returnType, Arrays.asList(modifierContributor));
            }

            /**
             * {@inheritDoc}
             */
            public MethodDefinition.ParameterDefinition.Initial<S> defineMethod(String name, TypeDefinition returnType, Collection<? extends ModifierContributor.ForMethod> modifierContributors) {
                return defineMethod(name, returnType, ModifierContributor.Resolver.of(modifierContributors).resolve());
            }

            /**
             * {@inheritDoc}
             */
            public MethodDefinition.ParameterDefinition.Initial<S> defineConstructor(ModifierContributor.ForMethod... modifierContributor) {
                return defineConstructor(Arrays.asList(modifierContributor));
            }

            /**
             * {@inheritDoc}
             */
            public MethodDefinition.ParameterDefinition.Initial<S> defineConstructor(Collection<? extends ModifierContributor.ForMethod> modifierContributors) {
                return defineConstructor(ModifierContributor.Resolver.of(modifierContributors).resolve());
            }

            /**
             * {@inheritDoc}
             */
            public MethodDefinition.ImplementationDefinition<S> define(Method method) {
                return define(new MethodDescription.ForLoadedMethod(method));
            }

            /**
             * {@inheritDoc}
             */
            public MethodDefinition.ImplementationDefinition<S> define(Constructor<?> constructor) {
                return define(new MethodDescription.ForLoadedConstructor(constructor));
            }

            /**
             * {@inheritDoc}
             */
            public MethodDefinition.ImplementationDefinition<S> define(MethodDescription methodDescription) {
                MethodDefinition.ParameterDefinition.Initial<S> initialParameterDefinition = methodDescription.isConstructor()
                        ? defineConstructor(methodDescription.getModifiers())
                        : defineMethod(methodDescription.getInternalName(), methodDescription.getReturnType(), methodDescription.getModifiers());
                ParameterList<?> parameterList = methodDescription.getParameters();
                MethodDefinition.ExceptionDefinition<S> exceptionDefinition;
                if (parameterList.hasExplicitMetaData()) {
                    MethodDefinition.ParameterDefinition<S> parameterDefinition = initialParameterDefinition;
                    for (ParameterDescription parameter : parameterList) {
                        parameterDefinition = parameterDefinition.withParameter(parameter.getType(), parameter.getName(), parameter.getModifiers());
                    }
                    exceptionDefinition = parameterDefinition;
                } else {
                    exceptionDefinition = initialParameterDefinition.withParameters(parameterList.asTypeList());
                }
                MethodDefinition.TypeVariableDefinition<S> typeVariableDefinition = exceptionDefinition.throwing(methodDescription.getExceptionTypes());
                for (TypeDescription.Generic typeVariable : methodDescription.getTypeVariables()) {
                    typeVariableDefinition = typeVariableDefinition.typeVariable(typeVariable.getSymbol(), typeVariable.getUpperBounds());
                }
                return typeVariableDefinition;
            }

            /**
             * {@inheritDoc}
             */
            public FieldDefinition.Optional<S> defineProperty(String name, Type type) {
                return defineProperty(name, TypeDefinition.Sort.describe(type));
            }

            /**
             * {@inheritDoc}
             */
            public FieldDefinition.Optional<S> defineProperty(String name, Type type, boolean readOnly) {
                return defineProperty(name, TypeDefinition.Sort.describe(type), readOnly);
            }

            /**
             * {@inheritDoc}
             */
            public FieldDefinition.Optional<S> defineProperty(String name, TypeDefinition type) {
                return defineProperty(name, type, false);
            }

            /**
             * {@inheritDoc}
             */
            public FieldDefinition.Optional<S> defineProperty(String name, TypeDefinition type, boolean readOnly) {
                if (name.length() == 0) {
                    throw new IllegalArgumentException("A bean property cannot have an empty name");
                } else if (type.represents(void.class)) {
                    throw new IllegalArgumentException("A bean property cannot have a void type");
                }
                DynamicType.Builder<S> builder = this;
                FieldManifestation fieldManifestation;
                if (!readOnly) {
                    builder = builder
                            .defineMethod("set" + Character.toUpperCase(name.charAt(0)) + name.substring(1), void.class, Visibility.PUBLIC)
                            .withParameters(type)
                            .intercept(FieldAccessor.ofField(name));
                    fieldManifestation = FieldManifestation.PLAIN;
                } else {
                    fieldManifestation = FieldManifestation.FINAL;
                }
                return builder
                        .defineMethod((type.represents(boolean.class)
                                ? "is"
                                : "get") + Character.toUpperCase(name.charAt(0)) + name.substring(1), type, Visibility.PUBLIC)
                        .intercept(FieldAccessor.ofField(name))
                        .defineField(name, type, Visibility.PRIVATE, fieldManifestation);
            }

            /**
             * {@inheritDoc}
             */
            public MethodDefinition.ImplementationDefinition<S> method(ElementMatcher<? super MethodDescription> matcher) {
                return invokable(isMethod().and(matcher));
            }

            /**
             * {@inheritDoc}
             */
            public MethodDefinition.ImplementationDefinition<S> constructor(ElementMatcher<? super MethodDescription> matcher) {
                return invokable(isConstructor().and(matcher));
            }

            /**
             * {@inheritDoc}
             */
            public MethodDefinition.ImplementationDefinition<S> invokable(ElementMatcher<? super MethodDescription> matcher) {
                return invokable(new LatentMatcher.Resolved<MethodDescription>(matcher));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> withHashCodeEquals() {
                return method(isHashCode())
                        .intercept(HashCodeMethod.usingDefaultOffset().withIgnoredFields(isSynthetic()))
                        .method(isEquals())
                        .intercept(EqualsMethod.isolated().withIgnoredFields(isSynthetic()));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> withToString() {
                return method(isToString()).intercept(ToStringMethod.prefixedBySimpleClassName());
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> require(TypeDescription type, byte[] binaryRepresentation) {
                return require(type, binaryRepresentation, LoadedTypeInitializer.NoOp.INSTANCE);
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> require(TypeDescription type, byte[] binaryRepresentation, LoadedTypeInitializer typeInitializer) {
                return require(new Default(type, binaryRepresentation, typeInitializer, Collections.<DynamicType>emptyList()));
            }

            /**
             * {@inheritDoc}
             */
            public Builder<S> require(DynamicType... auxiliaryType) {
                return require(Arrays.asList(auxiliaryType));
            }

            /**
             * {@inheritDoc}
             */
            public Unloaded<S> make(TypePool typePool) {
                return make(TypeResolutionStrategy.Passive.INSTANCE, typePool);
            }

            /**
             * {@inheritDoc}
             */
            public Unloaded<S> make() {
                return make(TypeResolutionStrategy.Passive.INSTANCE);
            }

            /**
             * A delegator for a dynamic type builder delegating all invocations to another dynamic type builder.
             *
             * @param <U> A loaded type that the built type is guaranteed to be a subclass of.
             */
            public abstract static class Delegator<U> extends AbstractBase<U> {

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> visit(AsmVisitorWrapper asmVisitorWrapper) {
                    return materialize().visit(asmVisitorWrapper);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> initializer(LoadedTypeInitializer loadedTypeInitializer) {
                    return materialize().initializer(loadedTypeInitializer);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> annotateType(Collection<? extends AnnotationDescription> annotations) {
                    return materialize().annotateType(annotations);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> attribute(TypeAttributeAppender typeAttributeAppender) {
                    return materialize().attribute(typeAttributeAppender);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> modifiers(int modifiers) {
                    return materialize().modifiers(modifiers);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> merge(Collection<? extends ModifierContributor.ForType> modifierContributors) {
                    return materialize().merge(modifierContributors);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> suffix(String suffix) {
                    return materialize().suffix(suffix);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> name(String name) {
                    return materialize().name(name);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> topLevelType() {
                    return materialize().topLevelType();
                }

                /**
                 * {@inheritDoc}
                 */
                public InnerTypeDefinition.ForType<U> innerTypeOf(TypeDescription type) {
                    return materialize().innerTypeOf(type);
                }

                /**
                 * {@inheritDoc}
                 */
                public InnerTypeDefinition<U> innerTypeOf(MethodDescription.InDefinedShape methodDescription) {
                    return materialize().innerTypeOf(methodDescription);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> declaredTypes(Collection<? extends TypeDescription> types) {
                    return materialize().declaredTypes(types);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> nestHost(TypeDescription type) {
                    return materialize().nestHost(type);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> nestMembers(Collection<? extends TypeDescription> types) {
                    return materialize().nestMembers(types);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> permittedSubclass(Collection<? extends TypeDescription> types) {
                    return materialize().permittedSubclass(types);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> unsealed() {
                    return materialize().unsealed();
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodDefinition.ImplementationDefinition.Optional<U> implement(Collection<? extends TypeDefinition> interfaceTypes) {
                    return materialize().implement(interfaceTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> initializer(ByteCodeAppender byteCodeAppender) {
                    return materialize().initializer(byteCodeAppender);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> ignoreAlso(ElementMatcher<? super MethodDescription> ignoredMethods) {
                    return materialize().ignoreAlso(ignoredMethods);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> ignoreAlso(LatentMatcher<? super MethodDescription> ignoredMethods) {
                    return materialize().ignoreAlso(ignoredMethods);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeVariableDefinition<U> typeVariable(String symbol, Collection<? extends TypeDefinition> bounds) {
                    return materialize().typeVariable(symbol, bounds);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> transform(ElementMatcher<? super TypeDescription.Generic> matcher, Transformer<TypeVariableToken> transformer) {
                    return materialize().transform(matcher, transformer);
                }

                /**
                 * {@inheritDoc}
                 */
                public FieldDefinition.Optional.Valuable<U> defineField(String name, TypeDefinition type, int modifiers) {
                    return materialize().defineField(name, type, modifiers);
                }

                /**
                 * {@inheritDoc}
                 */
                public FieldDefinition.Valuable<U> field(LatentMatcher<? super FieldDescription> matcher) {
                    return materialize().field(matcher);
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodDefinition.ParameterDefinition.Initial<U> defineMethod(String name, TypeDefinition returnType, int modifiers) {
                    return materialize().defineMethod(name, returnType, modifiers);
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodDefinition.ParameterDefinition.Initial<U> defineConstructor(int modifiers) {
                    return materialize().defineConstructor(modifiers);
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodDefinition.ImplementationDefinition<U> invokable(LatentMatcher<? super MethodDescription> matcher) {
                    return materialize().invokable(matcher);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> require(Collection<DynamicType> auxiliaryTypes) {
                    return materialize().require(auxiliaryTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public RecordComponentDefinition.Optional<U> defineRecordComponent(String name, TypeDefinition type) {
                    return materialize().defineRecordComponent(name, type);
                }

                /**
                 * {@inheritDoc}
                 */
                public RecordComponentDefinition.Optional<U> define(RecordComponentDescription recordComponentDescription) {
                    return materialize().define(recordComponentDescription);
                }

                /**
                 * {@inheritDoc}
                 */
                public RecordComponentDefinition<U> recordComponent(ElementMatcher<? super RecordComponentDescription> matcher) {
                    return materialize().recordComponent(matcher);
                }

                /**
                 * {@inheritDoc}
                 */
                public RecordComponentDefinition<U> recordComponent(LatentMatcher<? super RecordComponentDescription> matcher) {
                    return materialize().recordComponent(matcher);
                }

                /**
                 * {@inheritDoc}
                 */
                public DynamicType.Unloaded<U> make() {
                    return materialize().make();
                }

                /**
                 * {@inheritDoc}
                 */
                public Unloaded<U> make(TypeResolutionStrategy typeResolutionStrategy) {
                    return materialize().make(typeResolutionStrategy);
                }

                /**
                 * {@inheritDoc}
                 */
                public Unloaded<U> make(TypePool typePool) {
                    return materialize().make(typePool);
                }

                /**
                 * {@inheritDoc}
                 */
                public Unloaded<U> make(TypeResolutionStrategy typeResolutionStrategy, TypePool typePool) {
                    return materialize().make(typeResolutionStrategy, typePool);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription toTypeDescription() {
                    return materialize().toTypeDescription();
                }

                /**
                 * Creates a new builder that realizes the current state of the builder.
                 *
                 * @return A new builder that realizes the current state of the builder.
                 */
                protected abstract Builder<U> materialize();
            }

            /**
             * An adapter implementation of a dynamic type builder.
             *
             * @param <U> A loaded type that the built type is guaranteed to be a subclass of.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public abstract static class Adapter<U> extends AbstractBase<U> {

                /**
                 * The instrumented type to be created.
                 */
                protected final InstrumentedType.WithFlexibleName instrumentedType;

                /**
                 * The current field registry.
                 */
                protected final FieldRegistry fieldRegistry;

                /**
                 * The current method registry.
                 */
                protected final MethodRegistry methodRegistry;

                /**
                 * The current record component registry.
                 */
                protected final RecordComponentRegistry recordComponentRegistry;

                /**
                 * The type attribute appender to apply onto the instrumented type.
                 */
                protected final TypeAttributeAppender typeAttributeAppender;

                /**
                 * The ASM visitor wrapper to apply onto the class writer.
                 */
                protected final AsmVisitorWrapper asmVisitorWrapper;

                /**
                 * The class file version to define auxiliary types in.
                 */
                protected final ClassFileVersion classFileVersion;

                /**
                 * The naming strategy for auxiliary types to apply.
                 */
                protected final AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

                /**
                 * The annotation value filter factory to apply.
                 */
                protected final AnnotationValueFilter.Factory annotationValueFilterFactory;

                /**
                 * The annotation retention to apply.
                 */
                protected final AnnotationRetention annotationRetention;

                /**
                 * The implementation context factory to apply.
                 */
                protected final Implementation.Context.Factory implementationContextFactory;

                /**
                 * The method graph compiler to use.
                 */
                protected final MethodGraph.Compiler methodGraphCompiler;

                /**
                 * Determines if a type should be explicitly validated.
                 */
                protected final TypeValidation typeValidation;

                /**
                 * The visibility bridge strategy to apply.
                 */
                protected final VisibilityBridgeStrategy visibilityBridgeStrategy;

                /**
                 * The class writer strategy to use.
                 */
                protected final ClassWriterStrategy classWriterStrategy;

                /**
                 * A matcher for identifying methods that should be excluded from instrumentation.
                 */
                protected final LatentMatcher<? super MethodDescription> ignoredMethods;

                /**
                 * A list of explicitly defined auxiliary types.
                 */
                protected final List<? extends DynamicType> auxiliaryTypes;

                /**
                 * Creates a new default type writer for creating a new type that is not based on an existing class file.
                 *
                 * @param instrumentedType             The instrumented type to be created.
                 * @param fieldRegistry                The current field registry.
                 * @param methodRegistry               The current method registry.
                 * @param recordComponentRegistry      The record component pool to use.
                 * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
                 * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
                 * @param classFileVersion             The class file version to define auxiliary types in.
                 * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.
                 * @param annotationValueFilterFactory The annotation value filter factory to apply.
                 * @param annotationRetention          The annotation retention to apply.
                 * @param implementationContextFactory The implementation context factory to apply.
                 * @param methodGraphCompiler          The method graph compiler to use.
                 * @param typeValidation               Determines if a type should be explicitly validated.
                 * @param visibilityBridgeStrategy     The visibility bridge strategy to apply.
                 * @param classWriterStrategy          The class writer strategy to use.
                 * @param ignoredMethods               A matcher for identifying methods that should be excluded from instrumentation.
                 * @param auxiliaryTypes               A list of explicitly defined auxiliary types.
                 */
                protected Adapter(InstrumentedType.WithFlexibleName instrumentedType,
                                  FieldRegistry fieldRegistry,
                                  MethodRegistry methodRegistry,
                                  RecordComponentRegistry recordComponentRegistry,
                                  TypeAttributeAppender typeAttributeAppender,
                                  AsmVisitorWrapper asmVisitorWrapper,
                                  ClassFileVersion classFileVersion,
                                  AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                  AnnotationValueFilter.Factory annotationValueFilterFactory,
                                  AnnotationRetention annotationRetention,
                                  Implementation.Context.Factory implementationContextFactory,
                                  MethodGraph.Compiler methodGraphCompiler,
                                  TypeValidation typeValidation,
                                  VisibilityBridgeStrategy visibilityBridgeStrategy,
                                  ClassWriterStrategy classWriterStrategy,
                                  LatentMatcher<? super MethodDescription> ignoredMethods,
                                  List<? extends DynamicType> auxiliaryTypes) {
                    this.instrumentedType = instrumentedType;
                    this.fieldRegistry = fieldRegistry;
                    this.methodRegistry = methodRegistry;
                    this.recordComponentRegistry = recordComponentRegistry;
                    this.typeAttributeAppender = typeAttributeAppender;
                    this.asmVisitorWrapper = asmVisitorWrapper;
                    this.classFileVersion = classFileVersion;
                    this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
                    this.annotationValueFilterFactory = annotationValueFilterFactory;
                    this.annotationRetention = annotationRetention;
                    this.implementationContextFactory = implementationContextFactory;
                    this.methodGraphCompiler = methodGraphCompiler;
                    this.typeValidation = typeValidation;
                    this.visibilityBridgeStrategy = visibilityBridgeStrategy;
                    this.classWriterStrategy = classWriterStrategy;
                    this.ignoredMethods = ignoredMethods;
                    this.auxiliaryTypes = auxiliaryTypes;
                }

                /**
                 * {@inheritDoc}
                 */
                public FieldDefinition.Optional.Valuable<U> defineField(String name, TypeDefinition type, int modifiers) {
                    return new FieldDefinitionAdapter(new FieldDescription.Token(name, modifiers, type.asGenericType()));
                }

                /**
                 * {@inheritDoc}
                 */
                public FieldDefinition.Valuable<U> field(LatentMatcher<? super FieldDescription> matcher) {
                    return new FieldMatchAdapter(matcher);
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodDefinition.ParameterDefinition.Initial<U> defineMethod(String name, TypeDefinition returnType, int modifiers) {
                    return new MethodDefinitionAdapter(new MethodDescription.Token(name, modifiers, returnType.asGenericType()));
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodDefinition.ParameterDefinition.Initial<U> defineConstructor(int modifiers) {
                    return new MethodDefinitionAdapter(new MethodDescription.Token(modifiers));
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodDefinition.ImplementationDefinition<U> invokable(LatentMatcher<? super MethodDescription> matcher) {
                    return new MethodMatchAdapter(matcher);
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodDefinition.ImplementationDefinition.Optional<U> implement(Collection<? extends TypeDefinition> interfaceTypes) {
                    return new OptionalMethodMatchAdapter(new TypeList.Generic.Explicit(new ArrayList<TypeDefinition>(interfaceTypes)));
                }

                /**
                 * {@inheritDoc}
                 */
                @SuppressWarnings("unchecked") // In absence of @SafeVarargs
                public Builder<U> ignoreAlso(LatentMatcher<? super MethodDescription> ignoredMethods) {
                    return materialize(instrumentedType,
                            fieldRegistry,
                            methodRegistry,
                            recordComponentRegistry,
                            typeAttributeAppender,
                            asmVisitorWrapper,
                            classFileVersion,
                            auxiliaryTypeNamingStrategy,
                            annotationValueFilterFactory,
                            annotationRetention,
                            implementationContextFactory,
                            methodGraphCompiler,
                            typeValidation,
                            visibilityBridgeStrategy,
                            classWriterStrategy,
                            new LatentMatcher.Disjunction<MethodDescription>(this.ignoredMethods, ignoredMethods),
                            auxiliaryTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public RecordComponentDefinition.Optional<U> defineRecordComponent(String name, TypeDefinition type) {
                    return new RecordComponentDefinitionAdapter(new RecordComponentDescription.Token(name, type.asGenericType()));
                }

                /**
                 * {@inheritDoc}
                 */
                public RecordComponentDefinition<U> recordComponent(LatentMatcher<? super RecordComponentDescription> matcher) {
                    return new RecordComponentMatchAdapter(matcher);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> initializer(ByteCodeAppender byteCodeAppender) {
                    return materialize(instrumentedType.withInitializer(byteCodeAppender),
                            fieldRegistry,
                            methodRegistry,
                            recordComponentRegistry,
                            typeAttributeAppender,
                            asmVisitorWrapper,
                            classFileVersion,
                            auxiliaryTypeNamingStrategy,
                            annotationValueFilterFactory,
                            annotationRetention,
                            implementationContextFactory,
                            methodGraphCompiler,
                            typeValidation,
                            visibilityBridgeStrategy,
                            classWriterStrategy,
                            ignoredMethods,
                            auxiliaryTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> initializer(LoadedTypeInitializer loadedTypeInitializer) {
                    return materialize(instrumentedType.withInitializer(loadedTypeInitializer),
                            fieldRegistry,
                            methodRegistry,
                            recordComponentRegistry,
                            typeAttributeAppender,
                            asmVisitorWrapper,
                            classFileVersion,
                            auxiliaryTypeNamingStrategy,
                            annotationValueFilterFactory,
                            annotationRetention,
                            implementationContextFactory,
                            methodGraphCompiler,
                            typeValidation,
                            visibilityBridgeStrategy,
                            classWriterStrategy,
                            ignoredMethods,
                            auxiliaryTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> name(String name) {
                    return materialize(instrumentedType.withName(name),
                            fieldRegistry,
                            methodRegistry,
                            recordComponentRegistry,
                            typeAttributeAppender,
                            asmVisitorWrapper,
                            classFileVersion,
                            auxiliaryTypeNamingStrategy,
                            annotationValueFilterFactory,
                            annotationRetention,
                            implementationContextFactory,
                            methodGraphCompiler,
                            typeValidation,
                            visibilityBridgeStrategy,
                            classWriterStrategy,
                            ignoredMethods,
                            auxiliaryTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> suffix(String suffix) {
                    return name(instrumentedType.getName() + "$" + suffix);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> modifiers(int modifiers) {
                    return materialize(instrumentedType.withModifiers(modifiers),
                            fieldRegistry,
                            methodRegistry,
                            recordComponentRegistry,
                            typeAttributeAppender,
                            asmVisitorWrapper,
                            classFileVersion,
                            auxiliaryTypeNamingStrategy,
                            annotationValueFilterFactory,
                            annotationRetention,
                            implementationContextFactory,
                            methodGraphCompiler,
                            typeValidation,
                            visibilityBridgeStrategy,
                            classWriterStrategy,
                            ignoredMethods,
                            auxiliaryTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> merge(Collection<? extends ModifierContributor.ForType> modifierContributors) {
                    return materialize(instrumentedType.withModifiers(ModifierContributor.Resolver.of(modifierContributors).resolve(instrumentedType.getModifiers())),
                            fieldRegistry,
                            methodRegistry,
                            recordComponentRegistry,
                            typeAttributeAppender,
                            asmVisitorWrapper,
                            classFileVersion,
                            auxiliaryTypeNamingStrategy,
                            annotationValueFilterFactory,
                            annotationRetention,
                            implementationContextFactory,
                            methodGraphCompiler,
                            typeValidation,
                            visibilityBridgeStrategy,
                            classWriterStrategy,
                            ignoredMethods,
                            auxiliaryTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> topLevelType() {
                    return Adapter.this.materialize(instrumentedType
                                    .withDeclaringType(TypeDescription.UNDEFINED)
                                    .withEnclosingType(TypeDescription.UNDEFINED)
                                    .withLocalClass(false),
                            fieldRegistry,
                            methodRegistry,
                            recordComponentRegistry,
                            typeAttributeAppender,
                            asmVisitorWrapper,
                            classFileVersion,
                            auxiliaryTypeNamingStrategy,
                            annotationValueFilterFactory,
                            annotationRetention,
                            implementationContextFactory,
                            methodGraphCompiler,
                            typeValidation,
                            visibilityBridgeStrategy,
                            classWriterStrategy,
                            ignoredMethods,
                            auxiliaryTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public InnerTypeDefinition.ForType<U> innerTypeOf(TypeDescription type) {
                    return new InnerTypeDefinitionForTypeAdapter(type);
                }

                /**
                 * {@inheritDoc}
                 */
                public InnerTypeDefinition<U> innerTypeOf(MethodDescription.InDefinedShape methodDescription) {
                    return methodDescription.isTypeInitializer()
                            ? new InnerTypeDefinitionForTypeAdapter(methodDescription.getDeclaringType())
                            : new InnerTypeDefinitionForMethodAdapter(methodDescription);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> declaredTypes(Collection<? extends TypeDescription> types) {
                    return materialize(instrumentedType.withDeclaredTypes(new TypeList.Explicit(new ArrayList<TypeDescription>(types))),
                            fieldRegistry,
                            methodRegistry,
                            recordComponentRegistry,
                            typeAttributeAppender,
                            asmVisitorWrapper,
                            classFileVersion,
                            auxiliaryTypeNamingStrategy,
                            annotationValueFilterFactory,
                            annotationRetention,
                            implementationContextFactory,
                            methodGraphCompiler,
                            typeValidation,
                            visibilityBridgeStrategy,
                            classWriterStrategy,
                            ignoredMethods,
                            auxiliaryTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> nestHost(TypeDescription type) {
                    return materialize(instrumentedType.withNestHost(type),
                            fieldRegistry,
                            methodRegistry,
                            recordComponentRegistry,
                            typeAttributeAppender,
                            asmVisitorWrapper,
                            classFileVersion,
                            auxiliaryTypeNamingStrategy,
                            annotationValueFilterFactory,
                            annotationRetention,
                            implementationContextFactory,
                            methodGraphCompiler,
                            typeValidation,
                            visibilityBridgeStrategy,
                            classWriterStrategy,
                            ignoredMethods,
                            auxiliaryTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> nestMembers(Collection<? extends TypeDescription> types) {
                    return materialize(instrumentedType.withNestMembers(new TypeList.Explicit(new ArrayList<TypeDescription>(types))),
                            fieldRegistry,
                            methodRegistry,
                            recordComponentRegistry,
                            typeAttributeAppender,
                            asmVisitorWrapper,
                            classFileVersion,
                            auxiliaryTypeNamingStrategy,
                            annotationValueFilterFactory,
                            annotationRetention,
                            implementationContextFactory,
                            methodGraphCompiler,
                            typeValidation,
                            visibilityBridgeStrategy,
                            classWriterStrategy,
                            ignoredMethods,
                            auxiliaryTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> permittedSubclass(Collection<? extends TypeDescription> types) {
                    return materialize(instrumentedType.withPermittedSubclasses(new TypeList.Explicit(new ArrayList<TypeDescription>(types))),
                            fieldRegistry,
                            methodRegistry,
                            recordComponentRegistry,
                            typeAttributeAppender,
                            asmVisitorWrapper,
                            classFileVersion,
                            auxiliaryTypeNamingStrategy,
                            annotationValueFilterFactory,
                            annotationRetention,
                            implementationContextFactory,
                            methodGraphCompiler,
                            typeValidation,
                            visibilityBridgeStrategy,
                            classWriterStrategy,
                            ignoredMethods,
                            auxiliaryTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> unsealed() {
                    return materialize(instrumentedType.withSealed(false),
                            fieldRegistry,
                            methodRegistry,
                            recordComponentRegistry,
                            typeAttributeAppender,
                            asmVisitorWrapper,
                            classFileVersion,
                            auxiliaryTypeNamingStrategy,
                            annotationValueFilterFactory,
                            annotationRetention,
                            implementationContextFactory,
                            methodGraphCompiler,
                            typeValidation,
                            visibilityBridgeStrategy,
                            classWriterStrategy,
                            ignoredMethods,
                            auxiliaryTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeVariableDefinition<U> typeVariable(String symbol, Collection<? extends TypeDefinition> bounds) {
                    return new TypeVariableDefinitionAdapter(new TypeVariableToken(symbol, new TypeList.Generic.Explicit(new ArrayList<TypeDefinition>(bounds))));
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> transform(ElementMatcher<? super TypeDescription.Generic> matcher, Transformer<TypeVariableToken> transformer) {
                    return materialize(instrumentedType.withTypeVariables(matcher, transformer),
                            fieldRegistry,
                            methodRegistry,
                            recordComponentRegistry,
                            typeAttributeAppender,
                            asmVisitorWrapper,
                            classFileVersion,
                            auxiliaryTypeNamingStrategy,
                            annotationValueFilterFactory,
                            annotationRetention,
                            implementationContextFactory,
                            methodGraphCompiler,
                            typeValidation,
                            visibilityBridgeStrategy,
                            classWriterStrategy,
                            ignoredMethods,
                            auxiliaryTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> attribute(TypeAttributeAppender typeAttributeAppender) {
                    return materialize(instrumentedType,
                            fieldRegistry,
                            methodRegistry,
                            recordComponentRegistry,
                            new TypeAttributeAppender.Compound(this.typeAttributeAppender, typeAttributeAppender),
                            asmVisitorWrapper,
                            classFileVersion,
                            auxiliaryTypeNamingStrategy,
                            annotationValueFilterFactory,
                            annotationRetention,
                            implementationContextFactory,
                            methodGraphCompiler,
                            typeValidation,
                            visibilityBridgeStrategy,
                            classWriterStrategy,
                            ignoredMethods,
                            auxiliaryTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> annotateType(Collection<? extends AnnotationDescription> annotations) {
                    return materialize(instrumentedType.withAnnotations(new ArrayList<AnnotationDescription>(annotations)),
                            fieldRegistry,
                            methodRegistry,
                            recordComponentRegistry,
                            typeAttributeAppender,
                            asmVisitorWrapper,
                            classFileVersion,
                            auxiliaryTypeNamingStrategy,
                            annotationValueFilterFactory,
                            annotationRetention,
                            implementationContextFactory,
                            methodGraphCompiler,
                            typeValidation,
                            visibilityBridgeStrategy,
                            classWriterStrategy,
                            ignoredMethods,
                            auxiliaryTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> visit(AsmVisitorWrapper asmVisitorWrapper) {
                    return materialize(instrumentedType,
                            fieldRegistry,
                            methodRegistry,
                            recordComponentRegistry,
                            typeAttributeAppender,
                            new AsmVisitorWrapper.Compound(this.asmVisitorWrapper, asmVisitorWrapper),
                            classFileVersion,
                            auxiliaryTypeNamingStrategy,
                            annotationValueFilterFactory,
                            annotationRetention,
                            implementationContextFactory,
                            methodGraphCompiler,
                            typeValidation,
                            visibilityBridgeStrategy,
                            classWriterStrategy,
                            ignoredMethods,
                            auxiliaryTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public Builder<U> require(Collection<DynamicType> auxiliaryTypes) {
                    return materialize(instrumentedType,
                            fieldRegistry,
                            methodRegistry,
                            recordComponentRegistry,
                            typeAttributeAppender,
                            asmVisitorWrapper,
                            classFileVersion,
                            auxiliaryTypeNamingStrategy,
                            annotationValueFilterFactory,
                            annotationRetention,
                            implementationContextFactory,
                            methodGraphCompiler,
                            typeValidation,
                            visibilityBridgeStrategy,
                            classWriterStrategy,
                            ignoredMethods,
                            CompoundList.of(this.auxiliaryTypes, new ArrayList<DynamicType>(auxiliaryTypes)));
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription toTypeDescription() {
                    return instrumentedType;
                }

                /**
                 * Materializes the supplied state of a dynamic type builder.
                 *
                 * @param instrumentedType             The instrumented type.
                 * @param fieldRegistry                The current field registry.
                 * @param methodRegistry               The current method registry.
                 * @param recordComponentRegistry      The record component pool to use.
                 * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
                 * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
                 * @param classFileVersion             The class file version to define auxiliary types in.
                 * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.
                 * @param annotationValueFilterFactory The annotation value filter factory to apply.
                 * @param annotationRetention          The annotation retention to apply.
                 * @param implementationContextFactory The implementation context factory to apply.
                 * @param methodGraphCompiler          The method graph compiler to use.
                 * @param typeValidation               The type validation state.
                 * @param visibilityBridgeStrategy     The visibility bridge strategy to apply.
                 * @param classWriterStrategy          The class writer strategy to use.
                 * @param ignoredMethods               A matcher for identifying methods that should be excluded from instrumentation.
                 * @param auxiliaryTypes               A list of explicitly required auxiliary types.
                 * @return A type builder that represents the supplied arguments.
                 */
                protected abstract Builder<U> materialize(InstrumentedType.WithFlexibleName instrumentedType,
                                                          FieldRegistry fieldRegistry,
                                                          MethodRegistry methodRegistry,
                                                          RecordComponentRegistry recordComponentRegistry,
                                                          TypeAttributeAppender typeAttributeAppender,
                                                          AsmVisitorWrapper asmVisitorWrapper,
                                                          ClassFileVersion classFileVersion,
                                                          AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                          AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                          AnnotationRetention annotationRetention,
                                                          Implementation.Context.Factory implementationContextFactory,
                                                          MethodGraph.Compiler methodGraphCompiler,
                                                          TypeValidation typeValidation,
                                                          VisibilityBridgeStrategy visibilityBridgeStrategy,
                                                          ClassWriterStrategy classWriterStrategy,
                                                          LatentMatcher<? super MethodDescription> ignoredMethods,
                                                          List<? extends DynamicType> auxiliaryTypes);

                /**
                 * An adapter for applying an inner type definition for an outer type.
                 */
                @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                protected class InnerTypeDefinitionForTypeAdapter extends Builder.AbstractBase.Delegator<U> implements InnerTypeDefinition.ForType<U> {

                    /**
                     * A description of the type that is the defined outer type.
                     */
                    private final TypeDescription typeDescription;

                    /**
                     * Creates a new adapter for an inner type definition for an outer type.
                     *
                     * @param typeDescription A description of the type that is the defined outer type.
                     */
                    protected InnerTypeDefinitionForTypeAdapter(TypeDescription typeDescription) {
                        this.typeDescription = typeDescription;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Builder<U> asAnonymousType() {
                        return Adapter.this.materialize(instrumentedType
                                        .withDeclaringType(TypeDescription.UNDEFINED)
                                        .withEnclosingType(typeDescription)
                                        .withAnonymousClass(true),
                                fieldRegistry,
                                methodRegistry,
                                recordComponentRegistry,
                                typeAttributeAppender,
                                asmVisitorWrapper,
                                classFileVersion,
                                auxiliaryTypeNamingStrategy,
                                annotationValueFilterFactory,
                                annotationRetention,
                                implementationContextFactory,
                                methodGraphCompiler,
                                typeValidation,
                                visibilityBridgeStrategy,
                                classWriterStrategy,
                                ignoredMethods,
                                auxiliaryTypes);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Builder<U> asMemberType() {
                        return Adapter.this.materialize(instrumentedType
                                        .withDeclaringType(typeDescription)
                                        .withEnclosingType(typeDescription)
                                        .withAnonymousClass(false)
                                        .withLocalClass(false),
                                fieldRegistry,
                                methodRegistry,
                                recordComponentRegistry,
                                typeAttributeAppender,
                                asmVisitorWrapper,
                                classFileVersion,
                                auxiliaryTypeNamingStrategy,
                                annotationValueFilterFactory,
                                annotationRetention,
                                implementationContextFactory,
                                methodGraphCompiler,
                                typeValidation,
                                visibilityBridgeStrategy,
                                classWriterStrategy,
                                ignoredMethods,
                                auxiliaryTypes);
                    }

                    @Override
                    protected Builder<U> materialize() {
                        return Adapter.this.materialize(instrumentedType
                                        .withDeclaringType(TypeDescription.UNDEFINED)
                                        .withEnclosingType(typeDescription)
                                        .withLocalClass(true),
                                fieldRegistry,
                                methodRegistry,
                                recordComponentRegistry,
                                typeAttributeAppender,
                                asmVisitorWrapper,
                                classFileVersion,
                                auxiliaryTypeNamingStrategy,
                                annotationValueFilterFactory,
                                annotationRetention,
                                implementationContextFactory,
                                methodGraphCompiler,
                                typeValidation,
                                visibilityBridgeStrategy,
                                classWriterStrategy,
                                ignoredMethods,
                                auxiliaryTypes);
                    }
                }

                /**
                 * An adapter for applying an inner type definition for an outer method or constructor.
                 */
                @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                protected class InnerTypeDefinitionForMethodAdapter extends Builder.AbstractBase.Delegator<U> implements InnerTypeDefinition<U> {

                    /**
                     * A description of the declaring method or constructor.
                     */
                    private final MethodDescription.InDefinedShape methodDescription;

                    /**
                     * Creates a new adapter for defining a type that is declared within a method or constructor.
                     *
                     * @param methodDescription A description of the declaring method or constructor.
                     */
                    protected InnerTypeDefinitionForMethodAdapter(MethodDescription.InDefinedShape methodDescription) {
                        this.methodDescription = methodDescription;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Builder<U> asAnonymousType() {
                        return Adapter.this.materialize(instrumentedType
                                        .withDeclaringType(TypeDescription.UNDEFINED)
                                        .withEnclosingMethod(methodDescription)
                                        .withAnonymousClass(true),
                                fieldRegistry,
                                methodRegistry,
                                recordComponentRegistry,
                                typeAttributeAppender,
                                asmVisitorWrapper,
                                classFileVersion,
                                auxiliaryTypeNamingStrategy,
                                annotationValueFilterFactory,
                                annotationRetention,
                                implementationContextFactory,
                                methodGraphCompiler,
                                typeValidation,
                                visibilityBridgeStrategy,
                                classWriterStrategy,
                                ignoredMethods,
                                auxiliaryTypes);
                    }

                    @Override
                    protected Builder<U> materialize() {
                        return Adapter.this.materialize(instrumentedType
                                        .withDeclaringType(TypeDescription.UNDEFINED)
                                        .withEnclosingMethod(methodDescription)
                                        .withLocalClass(true),
                                fieldRegistry,
                                methodRegistry,
                                recordComponentRegistry,
                                typeAttributeAppender,
                                asmVisitorWrapper,
                                classFileVersion,
                                auxiliaryTypeNamingStrategy,
                                annotationValueFilterFactory,
                                annotationRetention,
                                implementationContextFactory,
                                methodGraphCompiler,
                                typeValidation,
                                visibilityBridgeStrategy,
                                classWriterStrategy,
                                ignoredMethods,
                                auxiliaryTypes);
                    }
                }

                /**
                 * An adapter for defining a new type variable for the instrumented type.
                 */
                @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                protected class TypeVariableDefinitionAdapter extends TypeVariableDefinition.AbstractBase<U> {

                    /**
                     * The current definition of the type variable.
                     */
                    private final TypeVariableToken token;

                    /**
                     * Creates a new type variable definition adapter.
                     *
                     * @param token The current definition of the type variable.
                     */
                    protected TypeVariableDefinitionAdapter(TypeVariableToken token) {
                        this.token = token;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public TypeVariableDefinition<U> annotateTypeVariable(Collection<? extends AnnotationDescription> annotations) {
                        return new TypeVariableDefinitionAdapter(new TypeVariableToken(token.getSymbol(),
                                token.getBounds(),
                                CompoundList.of(token.getAnnotations(), new ArrayList<AnnotationDescription>(annotations))));
                    }

                    @Override
                    protected Builder<U> materialize() {
                        return Adapter.this.materialize(instrumentedType.withTypeVariable(token),
                                fieldRegistry,
                                methodRegistry,
                                recordComponentRegistry,
                                typeAttributeAppender,
                                asmVisitorWrapper,
                                classFileVersion,
                                auxiliaryTypeNamingStrategy,
                                annotationValueFilterFactory,
                                annotationRetention,
                                implementationContextFactory,
                                methodGraphCompiler,
                                typeValidation,
                                visibilityBridgeStrategy,
                                classWriterStrategy,
                                ignoredMethods,
                                auxiliaryTypes);
                    }
                }

                /**
                 * An adapter for defining a new field.
                 */
                @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                protected class FieldDefinitionAdapter extends FieldDefinition.Optional.Valuable.AbstractBase.Adapter<U> {

                    /**
                     * The token representing the current field definition.
                     */
                    private final FieldDescription.Token token;

                    /**
                     * Creates a new field definition adapter.
                     *
                     * @param token The token representing the current field definition.
                     */
                    protected FieldDefinitionAdapter(FieldDescription.Token token) {
                        this(FieldAttributeAppender.ForInstrumentedField.INSTANCE,
                                Transformer.NoOp.<FieldDescription>make(),
                                FieldDescription.NO_DEFAULT_VALUE,
                                token);
                    }

                    /**
                     * Creates a new field definition adapter.
                     *
                     * @param fieldAttributeAppenderFactory The field attribute appender factory to apply.
                     * @param transformer                   The field transformer to apply.
                     * @param defaultValue                  The field's default value or {@code null} if no value is to be defined.
                     * @param token                         The token representing the current field definition.
                     */
                    protected FieldDefinitionAdapter(FieldAttributeAppender.Factory fieldAttributeAppenderFactory,
                                                     Transformer<FieldDescription> transformer,
                                                     Object defaultValue,
                                                     FieldDescription.Token token) {
                        super(fieldAttributeAppenderFactory, transformer, defaultValue);
                        this.token = token;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Optional<U> annotateField(Collection<? extends AnnotationDescription> annotations) {
                        return new FieldDefinitionAdapter(fieldAttributeAppenderFactory, transformer, defaultValue, new FieldDescription.Token(token.getName(),
                                token.getModifiers(),
                                token.getType(),
                                CompoundList.of(token.getAnnotations(), new ArrayList<AnnotationDescription>(annotations))));
                    }

                    @Override
                    protected Builder<U> materialize() {
                        return Builder.AbstractBase.Adapter.this.materialize(instrumentedType.withField(token),
                                fieldRegistry.prepend(new LatentMatcher.ForFieldToken(token), fieldAttributeAppenderFactory, defaultValue, transformer),
                                methodRegistry,
                                recordComponentRegistry,
                                typeAttributeAppender,
                                asmVisitorWrapper,
                                classFileVersion,
                                auxiliaryTypeNamingStrategy,
                                annotationValueFilterFactory,
                                annotationRetention,
                                implementationContextFactory,
                                methodGraphCompiler,
                                typeValidation,
                                visibilityBridgeStrategy,
                                classWriterStrategy,
                                ignoredMethods,
                                auxiliaryTypes);
                    }

                    @Override
                    protected Optional<U> materialize(FieldAttributeAppender.Factory fieldAttributeAppenderFactory,
                                                      Transformer<FieldDescription> transformer,
                                                      Object defaultValue) {
                        return new FieldDefinitionAdapter(fieldAttributeAppenderFactory, transformer, defaultValue, token);
                    }
                }

                /**
                 * An adapter for matching an existing field.
                 */
                @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                protected class FieldMatchAdapter extends FieldDefinition.Optional.Valuable.AbstractBase.Adapter<U> {

                    /**
                     * The matcher for any fields to apply this matcher to.
                     */
                    private final LatentMatcher<? super FieldDescription> matcher;

                    /**
                     * Creates a new field match adapter.
                     *
                     * @param matcher The matcher for any fields to apply this matcher to.
                     */
                    protected FieldMatchAdapter(LatentMatcher<? super FieldDescription> matcher) {
                        this(FieldAttributeAppender.NoOp.INSTANCE,
                                Transformer.NoOp.<FieldDescription>make(),
                                FieldDescription.NO_DEFAULT_VALUE,
                                matcher);
                    }

                    /**
                     * Creates a new field match adapter.
                     *
                     * @param fieldAttributeAppenderFactory The field attribute appender factory to apply.
                     * @param transformer                   The field transformer to apply.
                     * @param defaultValue                  The field's default value or {@code null} if no value is to be defined.
                     * @param matcher                       The matcher for any fields to apply this matcher to.
                     */
                    protected FieldMatchAdapter(FieldAttributeAppender.Factory fieldAttributeAppenderFactory,
                                                Transformer<FieldDescription> transformer,
                                                Object defaultValue,
                                                LatentMatcher<? super FieldDescription> matcher) {
                        super(fieldAttributeAppenderFactory, transformer, defaultValue);
                        this.matcher = matcher;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Optional<U> annotateField(Collection<? extends AnnotationDescription> annotations) {
                        return attribute(new FieldAttributeAppender.Explicit(new ArrayList<AnnotationDescription>(annotations)));
                    }

                    @Override
                    protected Builder<U> materialize() {
                        return Builder.AbstractBase.Adapter.this.materialize(instrumentedType,
                                fieldRegistry.prepend(matcher, fieldAttributeAppenderFactory, defaultValue, transformer),
                                methodRegistry,
                                recordComponentRegistry,
                                typeAttributeAppender,
                                asmVisitorWrapper,
                                classFileVersion,
                                auxiliaryTypeNamingStrategy,
                                annotationValueFilterFactory,
                                annotationRetention,
                                implementationContextFactory,
                                methodGraphCompiler,
                                typeValidation,
                                visibilityBridgeStrategy,
                                classWriterStrategy,
                                ignoredMethods,
                                auxiliaryTypes);
                    }

                    @Override
                    protected Optional<U> materialize(FieldAttributeAppender.Factory fieldAttributeAppenderFactory,
                                                      Transformer<FieldDescription> transformer,
                                                      Object defaultValue) {
                        return new FieldMatchAdapter(fieldAttributeAppenderFactory, transformer, defaultValue, matcher);
                    }
                }

                /**
                 * An adapter for defining a new method.
                 */
                @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                protected class MethodDefinitionAdapter extends MethodDefinition.ParameterDefinition.Initial.AbstractBase<U> {

                    /**
                     * A token representing the currently defined method.
                     */
                    private final MethodDescription.Token token;

                    /**
                     * Creates a new method definition adapter.
                     *
                     * @param token A token representing the currently defined method.
                     */
                    protected MethodDefinitionAdapter(MethodDescription.Token token) {
                        this.token = token;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDefinition.ParameterDefinition.Annotatable<U> withParameter(TypeDefinition type, String name, int modifiers) {
                        return new ParameterAnnotationAdapter(new ParameterDescription.Token(type.asGenericType(), name, modifiers));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Simple.Annotatable<U> withParameter(TypeDefinition type) {
                        return new SimpleParameterAnnotationAdapter(new ParameterDescription.Token(type.asGenericType()));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDefinition.ExceptionDefinition<U> throwing(Collection<? extends TypeDefinition> types) {
                        return new MethodDefinitionAdapter(new MethodDescription.Token(token.getName(),
                                token.getModifiers(),
                                token.getTypeVariableTokens(),
                                token.getReturnType(),
                                token.getParameterTokens(),
                                CompoundList.of(token.getExceptionTypes(), new TypeList.Generic.Explicit(new ArrayList<TypeDefinition>(types))),
                                token.getAnnotations(),
                                token.getDefaultValue(),
                                token.getReceiverType()));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDefinition.TypeVariableDefinition.Annotatable<U> typeVariable(String symbol, Collection<? extends TypeDefinition> bounds) {
                        return new TypeVariableAnnotationAdapter(new TypeVariableToken(symbol, new TypeList.Generic.Explicit(new ArrayList<TypeDefinition>(bounds))));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDefinition.ReceiverTypeDefinition<U> intercept(Implementation implementation) {
                        return materialize(new MethodRegistry.Handler.ForImplementation(implementation));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDefinition.ReceiverTypeDefinition<U> withoutCode() {
                        return new MethodDefinitionAdapter(new MethodDescription.Token(token.getName(),
                                (token.getModifiers() & Opcodes.ACC_NATIVE) == 0
                                        ? ModifierContributor.Resolver.of(MethodManifestation.ABSTRACT).resolve(token.getModifiers())
                                        : token.getModifiers(),
                                token.getTypeVariableTokens(),
                                token.getReturnType(),
                                token.getParameterTokens(),
                                token.getExceptionTypes(),
                                token.getAnnotations(),
                                token.getDefaultValue(),
                                token.getReceiverType())).materialize(MethodRegistry.Handler.ForAbstractMethod.INSTANCE);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDefinition.ReceiverTypeDefinition<U> defaultValue(AnnotationValue<?, ?> annotationValue) {
                        return new MethodDefinitionAdapter(new MethodDescription.Token(token.getName(),
                                ModifierContributor.Resolver.of(MethodManifestation.ABSTRACT).resolve(token.getModifiers()),
                                token.getTypeVariableTokens(),
                                token.getReturnType(),
                                token.getParameterTokens(),
                                token.getExceptionTypes(),
                                token.getAnnotations(),
                                annotationValue,
                                token.getReceiverType())).materialize(new MethodRegistry.Handler.ForAnnotationValue(annotationValue));
                    }

                    /**
                     * Materializes the given handler as the implementation.
                     *
                     * @param handler The handler for implementing the method.
                     * @return A method definition for the given handler.
                     */
                    private MethodDefinition.ReceiverTypeDefinition<U> materialize(MethodRegistry.Handler handler) {
                        return new AnnotationAdapter(handler);
                    }

                    /**
                     * An adapter for defining a new type variable for the currently defined method.
                     */
                    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                    protected class TypeVariableAnnotationAdapter extends MethodDefinition.TypeVariableDefinition.Annotatable.AbstractBase.Adapter<U> {

                        /**
                         * The currently defined type variable.
                         */
                        private final TypeVariableToken token;

                        /**
                         * Creates a new type variable annotation adapter.
                         *
                         * @param token The currently defined type variable.
                         */
                        protected TypeVariableAnnotationAdapter(TypeVariableToken token) {
                            this.token = token;
                        }

                        @Override
                        protected MethodDefinition.ParameterDefinition<U> materialize() {
                            return new MethodDefinitionAdapter(new MethodDescription.Token(MethodDefinitionAdapter.this.token.getName(),
                                    MethodDefinitionAdapter.this.token.getModifiers(),
                                    CompoundList.of(MethodDefinitionAdapter.this.token.getTypeVariableTokens(), token),
                                    MethodDefinitionAdapter.this.token.getReturnType(),
                                    MethodDefinitionAdapter.this.token.getParameterTokens(),
                                    MethodDefinitionAdapter.this.token.getExceptionTypes(),
                                    MethodDefinitionAdapter.this.token.getAnnotations(),
                                    MethodDefinitionAdapter.this.token.getDefaultValue(),
                                    MethodDefinitionAdapter.this.token.getReceiverType()));
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Annotatable<U> annotateTypeVariable(Collection<? extends AnnotationDescription> annotations) {
                            return new TypeVariableAnnotationAdapter(new TypeVariableToken(token.getSymbol(),
                                    token.getBounds(),
                                    CompoundList.of(token.getAnnotations(), new ArrayList<AnnotationDescription>(annotations))));
                        }
                    }

                    /**
                     * An annotation adapter for a parameter definition.
                     */
                    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                    protected class ParameterAnnotationAdapter extends MethodDefinition.ParameterDefinition.Annotatable.AbstractBase.Adapter<U> {

                        /**
                         * The token of the currently defined parameter.
                         */
                        private final ParameterDescription.Token token;

                        /**
                         * Creates a new parameter annotation adapter.
                         *
                         * @param token The token of the currently defined parameter.
                         */
                        protected ParameterAnnotationAdapter(ParameterDescription.Token token) {
                            this.token = token;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public MethodDefinition.ParameterDefinition.Annotatable<U> annotateParameter(Collection<? extends AnnotationDescription> annotations) {
                            return new ParameterAnnotationAdapter(new ParameterDescription.Token(token.getType(),
                                    CompoundList.of(token.getAnnotations(), new ArrayList<AnnotationDescription>(annotations)),
                                    token.getName(),
                                    token.getModifiers()));
                        }

                        @Override
                        protected MethodDefinition.ParameterDefinition<U> materialize() {
                            return new MethodDefinitionAdapter(new MethodDescription.Token(MethodDefinitionAdapter.this.token.getName(),
                                    MethodDefinitionAdapter.this.token.getModifiers(),
                                    MethodDefinitionAdapter.this.token.getTypeVariableTokens(),
                                    MethodDefinitionAdapter.this.token.getReturnType(),
                                    CompoundList.of(MethodDefinitionAdapter.this.token.getParameterTokens(), token),
                                    MethodDefinitionAdapter.this.token.getExceptionTypes(),
                                    MethodDefinitionAdapter.this.token.getAnnotations(),
                                    MethodDefinitionAdapter.this.token.getDefaultValue(),
                                    MethodDefinitionAdapter.this.token.getReceiverType()));
                        }
                    }

                    /**
                     * An annotation adapter for a simple parameter definition.
                     */
                    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                    protected class SimpleParameterAnnotationAdapter extends MethodDefinition.ParameterDefinition.Simple.Annotatable.AbstractBase.Adapter<U> {

                        /**
                         * The token of the currently defined parameter.
                         */
                        private final ParameterDescription.Token token;

                        /**
                         * Creates a new simple parameter annotation adapter.
                         *
                         * @param token The token of the currently defined parameter.
                         */
                        protected SimpleParameterAnnotationAdapter(ParameterDescription.Token token) {
                            this.token = token;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public MethodDefinition.ParameterDefinition.Simple.Annotatable<U> annotateParameter(Collection<? extends AnnotationDescription> annotations) {
                            return new SimpleParameterAnnotationAdapter(new ParameterDescription.Token(token.getType(),
                                    CompoundList.of(token.getAnnotations(), new ArrayList<AnnotationDescription>(annotations)),
                                    token.getName(),
                                    token.getModifiers()));
                        }

                        @Override
                        protected MethodDefinition.ParameterDefinition.Simple<U> materialize() {
                            return new MethodDefinitionAdapter(new MethodDescription.Token(MethodDefinitionAdapter.this.token.getName(),
                                    MethodDefinitionAdapter.this.token.getModifiers(),
                                    MethodDefinitionAdapter.this.token.getTypeVariableTokens(),
                                    MethodDefinitionAdapter.this.token.getReturnType(),
                                    CompoundList.of(MethodDefinitionAdapter.this.token.getParameterTokens(), token),
                                    MethodDefinitionAdapter.this.token.getExceptionTypes(),
                                    MethodDefinitionAdapter.this.token.getAnnotations(),
                                    MethodDefinitionAdapter.this.token.getDefaultValue(),
                                    MethodDefinitionAdapter.this.token.getReceiverType()));
                        }
                    }

                    /**
                     * An annotation adapter for a method definition.
                     */
                    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                    protected class AnnotationAdapter extends MethodDefinition.AbstractBase.Adapter<U> {

                        /**
                         * Creates a new annotation adapter.
                         *
                         * @param handler The handler that determines how a method is implemented.
                         */
                        protected AnnotationAdapter(MethodRegistry.Handler handler) {
                            this(handler,
                                    MethodAttributeAppender.ForInstrumentedMethod.INCLUDING_RECEIVER,
                                    Transformer.NoOp.<MethodDescription>make());
                        }

                        /**
                         * Creates a new annotation adapter.
                         *
                         * @param handler                        The handler that determines how a method is implemented.
                         * @param methodAttributeAppenderFactory The method attribute appender factory to apply onto the method that is currently being implemented.
                         * @param transformer                    The method transformer to apply onto the method that is currently being implemented.
                         */
                        protected AnnotationAdapter(MethodRegistry.Handler handler,
                                                    MethodAttributeAppender.Factory methodAttributeAppenderFactory,
                                                    Transformer<MethodDescription> transformer) {
                            super(handler, methodAttributeAppenderFactory, transformer);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public MethodDefinition<U> receiverType(TypeDescription.Generic receiverType) {
                            return new MethodDefinitionAdapter(new MethodDescription.Token(token.getName(),
                                    token.getModifiers(),
                                    token.getTypeVariableTokens(),
                                    token.getReturnType(),
                                    token.getParameterTokens(),
                                    token.getExceptionTypes(),
                                    token.getAnnotations(),
                                    token.getDefaultValue(),
                                    receiverType)).new AnnotationAdapter(handler, methodAttributeAppenderFactory, transformer);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public MethodDefinition<U> annotateMethod(Collection<? extends AnnotationDescription> annotations) {
                            return new MethodDefinitionAdapter(new MethodDescription.Token(token.getName(),
                                    token.getModifiers(),
                                    token.getTypeVariableTokens(),
                                    token.getReturnType(),
                                    token.getParameterTokens(),
                                    token.getExceptionTypes(),
                                    CompoundList.of(token.getAnnotations(), new ArrayList<AnnotationDescription>(annotations)),
                                    token.getDefaultValue(),
                                    token.getReceiverType())).new AnnotationAdapter(handler, methodAttributeAppenderFactory, transformer);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public MethodDefinition<U> annotateParameter(int index, Collection<? extends AnnotationDescription> annotations) {
                            List<ParameterDescription.Token> parameterTokens = new ArrayList<ParameterDescription.Token>(token.getParameterTokens());
                            parameterTokens.set(index, new ParameterDescription.Token(token.getParameterTokens().get(index).getType(),
                                    CompoundList.of(token.getParameterTokens().get(index).getAnnotations(), new ArrayList<AnnotationDescription>(annotations)),
                                    token.getParameterTokens().get(index).getName(),
                                    token.getParameterTokens().get(index).getModifiers()));
                            return new MethodDefinitionAdapter(new MethodDescription.Token(token.getName(),
                                    token.getModifiers(),
                                    token.getTypeVariableTokens(),
                                    token.getReturnType(),
                                    parameterTokens,
                                    token.getExceptionTypes(),
                                    token.getAnnotations(),
                                    token.getDefaultValue(),
                                    token.getReceiverType())).new AnnotationAdapter(handler, methodAttributeAppenderFactory, transformer);
                        }

                        @Override
                        protected MethodDefinition<U> materialize(MethodRegistry.Handler handler,
                                                                  MethodAttributeAppender.Factory methodAttributeAppenderFactory,
                                                                  Transformer<MethodDescription> transformer) {
                            return new AnnotationAdapter(handler, methodAttributeAppenderFactory, transformer);
                        }

                        @Override
                        protected Builder<U> materialize() {
                            return Builder.AbstractBase.Adapter.this.materialize(instrumentedType.withMethod(token),
                                    fieldRegistry,
                                    methodRegistry.prepend(new LatentMatcher.ForMethodToken(token),
                                            handler,
                                            methodAttributeAppenderFactory,
                                            transformer),
                                    recordComponentRegistry,
                                    typeAttributeAppender,
                                    asmVisitorWrapper,
                                    classFileVersion,
                                    auxiliaryTypeNamingStrategy,
                                    annotationValueFilterFactory,
                                    annotationRetention,
                                    implementationContextFactory,
                                    methodGraphCompiler,
                                    typeValidation,
                                    visibilityBridgeStrategy,
                                    classWriterStrategy,
                                    ignoredMethods,
                                    auxiliaryTypes);
                        }
                    }
                }

                /**
                 * An adapter for matching an existing method.
                 */
                @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                protected class MethodMatchAdapter extends MethodDefinition.ImplementationDefinition.AbstractBase<U> {

                    /**
                     * The method matcher of this adapter.
                     */
                    private final LatentMatcher<? super MethodDescription> matcher;

                    /**
                     * Creates a new method match adapter.
                     *
                     * @param matcher The method matcher of this adapter.
                     */
                    protected MethodMatchAdapter(LatentMatcher<? super MethodDescription> matcher) {
                        this.matcher = matcher;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDefinition.ReceiverTypeDefinition<U> intercept(Implementation implementation) {
                        return materialize(new MethodRegistry.Handler.ForImplementation(implementation));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDefinition.ReceiverTypeDefinition<U> withoutCode() {
                        return materialize(MethodRegistry.Handler.ForAbstractMethod.INSTANCE);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDefinition.ReceiverTypeDefinition<U> defaultValue(AnnotationValue<?, ?> annotationValue) {
                        return materialize(new MethodRegistry.Handler.ForAnnotationValue(annotationValue));
                    }

                    /**
                     * Materializes the method definition with the supplied handler.
                     *
                     * @param handler The handler that implements any method matched by this instances matcher.
                     * @return A method definition where any matched method is implemented by the supplied handler.
                     */
                    private MethodDefinition.ReceiverTypeDefinition<U> materialize(MethodRegistry.Handler handler) {
                        return new AnnotationAdapter(handler);
                    }

                    /**
                     * An annotation adapter for implementing annotations during a method definition.
                     */
                    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                    protected class AnnotationAdapter extends MethodDefinition.AbstractBase.Adapter<U> {

                        /**
                         * Creates a new annotation adapter.
                         *
                         * @param handler The handler that determines how a method is implemented.
                         */
                        protected AnnotationAdapter(MethodRegistry.Handler handler) {
                            this(handler, MethodAttributeAppender.NoOp.INSTANCE, Transformer.NoOp.<MethodDescription>make());
                        }

                        /**
                         * Creates a new annotation adapter.
                         *
                         * @param handler                        The handler that determines how a method is implemented.
                         * @param methodAttributeAppenderFactory The method attribute appender factory to apply onto the method that is currently being implemented.
                         * @param transformer                    The method transformer to apply onto the method that is currently being implemented.
                         */
                        protected AnnotationAdapter(MethodRegistry.Handler handler,
                                                    MethodAttributeAppender.Factory methodAttributeAppenderFactory,
                                                    Transformer<MethodDescription> transformer) {
                            super(handler, methodAttributeAppenderFactory, transformer);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public MethodDefinition<U> receiverType(TypeDescription.Generic receiverType) {
                            return new AnnotationAdapter(handler,
                                    new MethodAttributeAppender.Factory.Compound(methodAttributeAppenderFactory, new MethodAttributeAppender.ForReceiverType(receiverType)),
                                    transformer);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public MethodDefinition<U> annotateMethod(Collection<? extends AnnotationDescription> annotations) {
                            return new AnnotationAdapter(handler,
                                    new MethodAttributeAppender.Factory.Compound(methodAttributeAppenderFactory, new MethodAttributeAppender.Explicit(new ArrayList<AnnotationDescription>(annotations))),
                                    transformer);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public MethodDefinition<U> annotateParameter(int index, Collection<? extends AnnotationDescription> annotations) {
                            return new AnnotationAdapter(handler,
                                    new MethodAttributeAppender.Factory.Compound(methodAttributeAppenderFactory, new MethodAttributeAppender.Explicit(index, new ArrayList<AnnotationDescription>(annotations))),
                                    transformer);
                        }

                        @Override
                        protected MethodDefinition<U> materialize(MethodRegistry.Handler handler,
                                                                  MethodAttributeAppender.Factory methodAttributeAppenderFactory,
                                                                  Transformer<MethodDescription> transformer) {
                            return new AnnotationAdapter(handler, methodAttributeAppenderFactory, transformer);
                        }

                        @Override
                        protected Builder<U> materialize() {
                            return Builder.AbstractBase.Adapter.this.materialize(instrumentedType,
                                    fieldRegistry,
                                    methodRegistry.prepend(matcher, handler, methodAttributeAppenderFactory, transformer),
                                    recordComponentRegistry,
                                    typeAttributeAppender,
                                    asmVisitorWrapper,
                                    classFileVersion,
                                    auxiliaryTypeNamingStrategy,
                                    annotationValueFilterFactory,
                                    annotationRetention,
                                    implementationContextFactory,
                                    methodGraphCompiler,
                                    typeValidation,
                                    visibilityBridgeStrategy,
                                    classWriterStrategy,
                                    ignoredMethods,
                                    auxiliaryTypes);
                        }
                    }
                }

                /**
                 * An adapter for optionally matching methods defined by declared interfaces.
                 */
                @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                protected class OptionalMethodMatchAdapter extends Builder.AbstractBase.Delegator<U> implements MethodDefinition.ImplementationDefinition.Optional<U> {

                    /**
                     * The interfaces whose methods are optionally matched.
                     */
                    private final TypeList.Generic interfaces;

                    /**
                     * Creates a new optional method match adapter.
                     *
                     * @param interfaces The interfaces whose methods are optionally matched.
                     */
                    protected OptionalMethodMatchAdapter(TypeList.Generic interfaces) {
                        this.interfaces = interfaces;
                    }

                    @Override
                    protected Builder<U> materialize() {
                        return Adapter.this.materialize(instrumentedType.withInterfaces(interfaces),
                                fieldRegistry,
                                methodRegistry,
                                recordComponentRegistry,
                                typeAttributeAppender,
                                asmVisitorWrapper,
                                classFileVersion,
                                auxiliaryTypeNamingStrategy,
                                annotationValueFilterFactory,
                                annotationRetention,
                                implementationContextFactory,
                                methodGraphCompiler,
                                typeValidation,
                                visibilityBridgeStrategy,
                                classWriterStrategy,
                                ignoredMethods,
                                auxiliaryTypes);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDefinition.ReceiverTypeDefinition<U> intercept(Implementation implementation) {
                        return interfaceType().intercept(implementation);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDefinition.ReceiverTypeDefinition<U> withoutCode() {
                        return interfaceType().withoutCode();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDefinition.ReceiverTypeDefinition<U> defaultValue(AnnotationValue<?, ?> annotationValue) {
                        return interfaceType().defaultValue(annotationValue);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public <V> MethodDefinition.ReceiverTypeDefinition<U> defaultValue(V value, Class<? extends V> type) {
                        return interfaceType().defaultValue(value, type);
                    }

                    /**
                     * Returns a matcher for the interfaces' methods.
                     *
                     * @return A matcher for the interfaces' methods.
                     */
                    private MethodDefinition.ImplementationDefinition<U> interfaceType() {
                        ElementMatcher.Junction<TypeDescription> elementMatcher = none();
                        for (TypeDescription typeDescription : interfaces.asErasures()) {
                            elementMatcher = elementMatcher.or(isSuperTypeOf(typeDescription));
                        }
                        return materialize().invokable(isDeclaredBy(isInterface().and(elementMatcher)));
                    }
                }

                /**
                 * An adapter for defining a record component.
                 */
                @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                protected class RecordComponentDefinitionAdapter extends RecordComponentDefinition.Optional.AbstractBase<U> {

                    /**
                     * The record component attribute appender factory to apply.
                     */
                    private final RecordComponentAttributeAppender.Factory recordComponentAttributeAppenderFactory;

                    /**
                     * A token representing the defined record component.
                     */
                    private final RecordComponentDescription.Token token;

                    /**
                     * A transformer to apply on matched record component descriptions.
                     */
                    private final Transformer<RecordComponentDescription> transformer;

                    /**
                     * Creates a new record component definition adapter.
                     *
                     * @param token A token representing the defined record component.
                     */
                    protected RecordComponentDefinitionAdapter(RecordComponentDescription.Token token) {
                        this(RecordComponentAttributeAppender.ForInstrumentedRecordComponent.INSTANCE,
                                Transformer.NoOp.<RecordComponentDescription>make(),
                                token);
                    }

                    /**
                     * Creates a new record component definition adapter.
                     *
                     * @param recordComponentAttributeAppenderFactory The record component attribute appender factory to apply.
                     * @param transformer                             A transformer to apply on matched record component descriptions.
                     * @param token                                   A token representing the defined record component.
                     */
                    protected RecordComponentDefinitionAdapter(RecordComponentAttributeAppender.Factory recordComponentAttributeAppenderFactory,
                                                               Transformer<RecordComponentDescription> transformer,
                                                               RecordComponentDescription.Token token) {
                        this.recordComponentAttributeAppenderFactory = recordComponentAttributeAppenderFactory;
                        this.transformer = transformer;
                        this.token = token;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Optional<U> annotateRecordComponent(Collection<? extends AnnotationDescription> annotations) {
                        return new RecordComponentDefinitionAdapter(recordComponentAttributeAppenderFactory, transformer, new RecordComponentDescription.Token(token.getName(),
                                token.getType(),
                                CompoundList.of(token.getAnnotations(), new ArrayList<AnnotationDescription>(annotations))));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Optional<U> attribute(RecordComponentAttributeAppender.Factory recordComponentAttributeAppenderFactory) {
                        return new RecordComponentDefinitionAdapter(new RecordComponentAttributeAppender.Factory.Compound(this.recordComponentAttributeAppenderFactory,
                                recordComponentAttributeAppenderFactory), transformer, token);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @SuppressWarnings("unchecked") // In absence of @SafeVarargs
                    public Optional<U> transform(Transformer<RecordComponentDescription> transformer) {
                        return new RecordComponentDefinitionAdapter(recordComponentAttributeAppenderFactory,
                                new Transformer.Compound<RecordComponentDescription>(this.transformer, transformer),
                                token);
                    }

                    @Override
                    protected Builder<U> materialize() {
                        return Builder.AbstractBase.Adapter.this.materialize(instrumentedType.withRecordComponent(token),
                                fieldRegistry,
                                methodRegistry,
                                recordComponentRegistry.prepend(new LatentMatcher.ForRecordComponentToken(token),
                                        recordComponentAttributeAppenderFactory,
                                        transformer),
                                typeAttributeAppender,
                                asmVisitorWrapper,
                                classFileVersion,
                                auxiliaryTypeNamingStrategy,
                                annotationValueFilterFactory,
                                annotationRetention,
                                implementationContextFactory,
                                methodGraphCompiler,
                                typeValidation,
                                visibilityBridgeStrategy,
                                classWriterStrategy,
                                ignoredMethods,
                                auxiliaryTypes);
                    }
                }

                /**
                 * An adapter for matching record components.
                 */
                protected class RecordComponentMatchAdapter extends RecordComponentDefinition.Optional.AbstractBase<U> {

                    /**
                     * The matcher for identifying record components to match.
                     */
                    private final LatentMatcher<? super RecordComponentDescription> matcher;

                    /**
                     * The record component attribute appender factory to apply.
                     */
                    private final RecordComponentAttributeAppender.Factory recordComponentAttributeAppenderFactory;

                    /**
                     * A transformer to apply on matched record component descriptions.
                     */
                    private final Transformer<RecordComponentDescription> transformer;

                    /**
                     * Creates a new record component match adapter.
                     *
                     * @param matcher The matcher for identifying record components to match.
                     */
                    protected RecordComponentMatchAdapter(LatentMatcher<? super RecordComponentDescription> matcher) {
                        this(matcher, RecordComponentAttributeAppender.NoOp.INSTANCE, Transformer.NoOp.<RecordComponentDescription>make());
                    }

                    /**
                     * Creates a new record component match adapter.
                     *
                     * @param matcher                                 The matcher for identifying record components to match.
                     * @param recordComponentAttributeAppenderFactory The record component attribute appender factory to apply.
                     * @param transformer                             A transformer to apply on matched record component descriptions.
                     */
                    protected RecordComponentMatchAdapter(LatentMatcher<? super RecordComponentDescription> matcher,
                                                          RecordComponentAttributeAppender.Factory recordComponentAttributeAppenderFactory,
                                                          Transformer<RecordComponentDescription> transformer) {
                        this.matcher = matcher;
                        this.recordComponentAttributeAppenderFactory = recordComponentAttributeAppenderFactory;
                        this.transformer = transformer;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Optional<U> annotateRecordComponent(Collection<? extends AnnotationDescription> annotations) {
                        return attribute(new RecordComponentAttributeAppender.Explicit(new ArrayList<AnnotationDescription>(annotations)));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Optional<U> attribute(RecordComponentAttributeAppender.Factory recordComponentAttributeAppenderFactory) {
                        return new RecordComponentMatchAdapter(matcher, new RecordComponentAttributeAppender.Factory.Compound(this.recordComponentAttributeAppenderFactory,
                                recordComponentAttributeAppenderFactory), transformer);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @SuppressWarnings("unchecked")  // In absence of @SafeVarargs
                    public Optional<U> transform(Transformer<RecordComponentDescription> transformer) {
                        return new RecordComponentMatchAdapter(matcher,
                                recordComponentAttributeAppenderFactory,
                                new Transformer.Compound<RecordComponentDescription>(this.transformer, transformer));
                    }

                    @Override
                    protected Builder<U> materialize() {
                        return Builder.AbstractBase.Adapter.this.materialize(instrumentedType,
                                fieldRegistry,
                                methodRegistry,
                                recordComponentRegistry.prepend(matcher, recordComponentAttributeAppenderFactory, transformer),
                                typeAttributeAppender,
                                asmVisitorWrapper,
                                classFileVersion,
                                auxiliaryTypeNamingStrategy,
                                annotationValueFilterFactory,
                                annotationRetention,
                                implementationContextFactory,
                                methodGraphCompiler,
                                typeValidation,
                                visibilityBridgeStrategy,
                                classWriterStrategy,
                                ignoredMethods,
                                auxiliaryTypes);
                    }
                }
            }
        }
    }

    /**
     * A dynamic type that has not yet been loaded by a given {@link java.lang.ClassLoader}.
     *
     * @param <T> The most specific known loaded type that is implemented by this dynamic type, usually the
     *            type itself, an interface or the direct super class.
     */
    interface Unloaded<T> extends DynamicType {

        /**
         * <p>
         * Attempts to load this dynamic type including all of its auxiliary types, if any. If the class loader is an
         * unsealed instance of {@link InjectionClassLoader}, the classes are injected directy into the class loader, otherwise,
         * a new class loader is created where the supplied class loader is set as parent.
         * </p>
         * <p>
         * <b>Note</b>: A new class is attempted to be loaded each time this method is invoked, even if a compatible class was
         * created previously. Consider using a {@link net.bytebuddy.TypeCache}.
         * </p>
         *
         * @param classLoader The class loader to use for this class loading.
         * @return This dynamic type in its loaded state.
         */
        Loaded<T> load(ClassLoader classLoader);

        /**
         * <p>
         * Attempts to load this dynamic type including all of its auxiliary types, if any.
         * </p>
         * <p>
         * <b>Note</b>: A new class is attempted to be loaded each time this method is invoked, even if a compatible class was
         * created previously. Consider using a {@link net.bytebuddy.TypeCache}.
         * </p>
         *
         * @param classLoader          The class loader to use for this class loading.
         * @param classLoadingStrategy The class loader strategy which should be used for this class loading.
         * @param <S>                  The least specific type of class loader this strategy can apply to.
         * @return This dynamic type in its loaded state.
         * @see net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default
         */
        <S extends ClassLoader> Loaded<T> load(S classLoader, ClassLoadingStrategy<? super S> classLoadingStrategy);

        /**
         * Includes the provided dynamic types as auxiliary types of this instance.
         *
         * @param dynamicType The dynamic types to include.
         * @return A copy of this unloaded dynamic type which includes the provided dynamic types.
         */
        Unloaded<T> include(DynamicType... dynamicType);

        /**
         * Includes the provided dynamic types as auxiliary types of this instance.
         *
         * @param dynamicTypes The dynamic types to include.
         * @return A copy of this unloaded dynamic type which includes the provided dynamic types.
         */
        Unloaded<T> include(List<? extends DynamicType> dynamicTypes);
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
     * A default implementation of a dynamic type.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Default implements DynamicType {

        /**
         * The file name extension for Java class files.
         */
        private static final String CLASS_FILE_EXTENSION = ".class";

        /**
         * The default version of a jar file manifest.
         */
        private static final String MANIFEST_VERSION = "1.0";

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
         * A dispacher for applying a file copy.
         */
        protected static final Dispatcher DISPATCHER = AccessController.doPrivileged(Dispatcher.CreationAction.INSTANCE);

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
         * @param binaryRepresentation  A byte array containing the binary representation of this dynamic type. The array must not be modified.
         * @param loadedTypeInitializer The loaded type initializer of this dynamic type.
         * @param auxiliaryTypes        The auxiliary type required for this dynamic type.
         */
        @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The array is not to be modified by contract")
        public Default(TypeDescription typeDescription,
                       byte[] binaryRepresentation,
                       LoadedTypeInitializer loadedTypeInitializer,
                       List<? extends DynamicType> auxiliaryTypes) {
            this.typeDescription = typeDescription;
            this.binaryRepresentation = binaryRepresentation;
            this.loadedTypeInitializer = loadedTypeInitializer;
            this.auxiliaryTypes = auxiliaryTypes;
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getTypeDescription() {
            return typeDescription;
        }

        /**
         * {@inheritDoc}
         */
        public Map<TypeDescription, byte[]> getAllTypes() {
            Map<TypeDescription, byte[]> allTypes = new LinkedHashMap<TypeDescription, byte[]>();
            allTypes.put(typeDescription, binaryRepresentation);
            for (DynamicType auxiliaryType : auxiliaryTypes) {
                allTypes.putAll(auxiliaryType.getAllTypes());
            }
            return allTypes;
        }

        /**
         * {@inheritDoc}
         */
        public Map<TypeDescription, LoadedTypeInitializer> getLoadedTypeInitializers() {
            Map<TypeDescription, LoadedTypeInitializer> classLoadingCallbacks = new HashMap<TypeDescription, LoadedTypeInitializer>();
            for (DynamicType auxiliaryType : auxiliaryTypes) {
                classLoadingCallbacks.putAll(auxiliaryType.getLoadedTypeInitializers());
            }
            classLoadingCallbacks.put(typeDescription, loadedTypeInitializer);
            return classLoadingCallbacks;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasAliveLoadedTypeInitializers() {
            for (LoadedTypeInitializer loadedTypeInitializer : getLoadedTypeInitializers().values()) {
                if (loadedTypeInitializer.isAlive()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The array is not to be modified by contract")
        public byte[] getBytes() {
            return binaryRepresentation;
        }

        /**
         * {@inheritDoc}
         */
        public Map<TypeDescription, byte[]> getAuxiliaryTypes() {
            Map<TypeDescription, byte[]> auxiliaryTypes = new HashMap<TypeDescription, byte[]>();
            for (DynamicType auxiliaryType : this.auxiliaryTypes) {
                auxiliaryTypes.put(auxiliaryType.getTypeDescription(), auxiliaryType.getBytes());
                auxiliaryTypes.putAll(auxiliaryType.getAuxiliaryTypes());
            }
            return auxiliaryTypes;
        }

        /**
         * {@inheritDoc}
         */
        public Map<TypeDescription, File> saveIn(File folder) throws IOException {
            Map<TypeDescription, File> files = new HashMap<TypeDescription, File>();
            File target = new File(folder, typeDescription.getName().replace('.', File.separatorChar) + CLASS_FILE_EXTENSION);
            if (target.getParentFile() != null && !target.getParentFile().isDirectory() && !target.getParentFile().mkdirs()) {
                throw new IllegalArgumentException("Could not create directory: " + target.getParentFile());
            }
            OutputStream outputStream = new FileOutputStream(target);
            try {
                outputStream.write(binaryRepresentation);
            } finally {
                outputStream.close();
            }
            files.put(typeDescription, target);
            for (DynamicType auxiliaryType : auxiliaryTypes) {
                files.putAll(auxiliaryType.saveIn(folder));
            }
            return files;
        }

        /**
         * {@inheritDoc}
         */
        public File inject(File sourceJar, File targetJar) throws IOException {
            return sourceJar.equals(targetJar)
                    ? inject(sourceJar)
                    : doInject(sourceJar, targetJar);
        }

        /**
         * {@inheritDoc}
         */
        public File inject(File jar) throws IOException {
            File temporary = doInject(jar, File.createTempFile(jar.getName(), TEMP_SUFFIX));
            boolean delete = true;
            try {
                delete = DISPATCHER.copy(temporary, jar);
            } finally {
                if (delete && !temporary.delete()) {
                    temporary.deleteOnExit();
                }
            }
            return jar;
        }

        /**
         * Injects this dynamic type into a source jar and writes the result to the target jar.
         *
         * @param sourceJar The source jar.
         * @param targetJar The target jar.
         * @return The jar file that was written to.
         * @throws IOException If an I/O error occurs.
         */
        private File doInject(File sourceJar, File targetJar) throws IOException {
            JarInputStream inputStream = new JarInputStream(new FileInputStream(sourceJar));
            try {
                if (!targetJar.isFile() && !targetJar.createNewFile()) {
                    throw new IllegalArgumentException("Could not create file: " + targetJar);
                }
                Manifest manifest = inputStream.getManifest();
                JarOutputStream outputStream = manifest == null
                        ? new JarOutputStream(new FileOutputStream(targetJar))
                        : new JarOutputStream(new FileOutputStream(targetJar), manifest);
                try {
                    Map<TypeDescription, byte[]> rawAuxiliaryTypes = getAuxiliaryTypes();
                    Map<String, byte[]> files = new HashMap<String, byte[]>();
                    for (Map.Entry<TypeDescription, byte[]> entry : rawAuxiliaryTypes.entrySet()) {
                        files.put(entry.getKey().getInternalName() + CLASS_FILE_EXTENSION, entry.getValue());
                    }
                    files.put(typeDescription.getInternalName() + CLASS_FILE_EXTENSION, binaryRepresentation);
                    JarEntry jarEntry;
                    while ((jarEntry = inputStream.getNextJarEntry()) != null) {
                        byte[] replacement = files.remove(jarEntry.getName());
                        if (replacement == null) {
                            outputStream.putNextEntry(jarEntry);
                            byte[] buffer = new byte[BUFFER_SIZE];
                            int index;
                            while ((index = inputStream.read(buffer)) != END_OF_FILE) {
                                outputStream.write(buffer, FROM_BEGINNING, index);
                            }
                        } else {
                            outputStream.putNextEntry(new JarEntry(jarEntry.getName()));
                            outputStream.write(replacement);
                        }
                        inputStream.closeEntry();
                        outputStream.closeEntry();
                    }
                    for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                        outputStream.putNextEntry(new JarEntry(entry.getKey()));
                        outputStream.write(entry.getValue());
                        outputStream.closeEntry();
                    }
                } finally {
                    outputStream.close();
                }
            } finally {
                inputStream.close();
            }
            return targetJar;
        }

        /**
         * {@inheritDoc}
         */
        public File toJar(File file) throws IOException {
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, MANIFEST_VERSION);
            return toJar(file, manifest);
        }

        /**
         * {@inheritDoc}
         */
        public File toJar(File file, Manifest manifest) throws IOException {
            if (!file.isFile() && !file.createNewFile()) {
                throw new IllegalArgumentException("Could not create file: " + file);
            }
            JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(file), manifest);
            try {
                for (Map.Entry<TypeDescription, byte[]> entry : getAuxiliaryTypes().entrySet()) {
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

        /**
         * A dispatcher that allows for file copy operations based on NIO2 if available.
         */
        protected interface Dispatcher {

            /**
             * Copies the source file to the target location.
             *
             * @param source The source file.
             * @param target The target file.
             * @return {@code true} if the source file needs to be deleted.
             * @throws IOException If an I/O error occurs.
             */
            boolean copy(File source, File target) throws IOException;

            /**
             * An action for creating a dispatcher.
             */
            enum CreationAction implements PrivilegedAction<Dispatcher> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                @SuppressWarnings("unchecked")
                public Dispatcher run() {
                    try {
                        Class<?> path = Class.forName("java.nio.file.Path");
                        Object[] arguments = (Object[]) Array.newInstance(Class.forName("java.nio.file.CopyOption"), 1);
                        arguments[0] = Enum.valueOf((Class) Class.forName("java.nio.file.StandardCopyOption"), "REPLACE_EXISTING");
                        return new ForJava7CapableVm(File.class.getMethod("toPath"),
                                Class.forName("java.nio.file.Files").getMethod("move", path, path, arguments.getClass()),
                                arguments);
                    } catch (Throwable ignored) {
                        return ForLegacyVm.INSTANCE;
                    }
                }
            }

            /**
             * A legacy dispatcher that is not capable of NIO.
             */
            enum ForLegacyVm implements Dispatcher {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public boolean copy(File source, File target) throws IOException {
                    InputStream inputStream = new FileInputStream(source);
                    try {
                        OutputStream outputStream = new FileOutputStream(target);
                        try {
                            byte[] buffer = new byte[BUFFER_SIZE];
                            int index;
                            while ((index = inputStream.read(buffer)) != END_OF_FILE) {
                                outputStream.write(buffer, FROM_BEGINNING, index);
                            }
                        } finally {
                            outputStream.close();
                        }
                    } finally {
                        inputStream.close();
                    }
                    return true;
                }
            }

            /**
             * A dispatcher for VMs that are capable of NIO2.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForJava7CapableVm implements Dispatcher {

                /**
                 * The {@code java.io.File#toPath()} method.
                 */
                private final Method toPath;

                /**
                 * The {@code java.nio.Files#copy(Path,Path,CopyOption[])} method.
                 */
                private final Method move;

                /**
                 * The copy options to apply.
                 */
                private final Object[] copyOptions;

                /**
                 * Creates a new NIO2 capable dispatcher.
                 *
                 * @param toPath      The {@code java.io.File#toPath()} method.
                 * @param move        The {@code java.nio.Files#move(Path,Path,CopyOption[])} method.
                 * @param copyOptions The copy options to apply.
                 */
                protected ForJava7CapableVm(Method toPath, Method move, Object[] copyOptions) {
                    this.toPath = toPath;
                    this.move = move;
                    this.copyOptions = copyOptions;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean copy(File source, File target) throws IOException {
                    try {
                        move.invoke(null, toPath.invoke(source), toPath.invoke(target), copyOptions);
                        return false;
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access NIO file copy", exception);
                    } catch (InvocationTargetException exception) {
                        Throwable cause = exception.getCause();
                        if (cause instanceof IOException) {
                            throw (IOException) cause;
                        } else {
                            throw new IllegalStateException("Cannot execute NIO file copy", cause);
                        }
                    }
                }
            }
        }

        /**
         * A default implementation of an unloaded dynamic type.
         *
         * @param <T> The most specific known loaded type that is implemented by this dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        @HashCodeAndEqualsPlugin.Enhance
        public static class Unloaded<T> extends Default implements DynamicType.Unloaded<T> {

            /**
             * The type resolution strategy to use for initializing the dynamic type.
             */
            private final TypeResolutionStrategy.Resolved typeResolutionStrategy;

            /**
             * Creates a new unloaded representation of a dynamic type.
             *
             * @param typeDescription        A description of this dynamic type.
             * @param binaryRepresentation   An array of byte of the binary representation of this dynamic type.
             * @param loadedTypeInitializer  The type initializer of this dynamic type.
             * @param auxiliaryTypes         The auxiliary types that are required for this dynamic type.
             * @param typeResolutionStrategy The type resolution strategy to use for initializing the dynamic type.
             */
            public Unloaded(TypeDescription typeDescription,
                            byte[] binaryRepresentation,
                            LoadedTypeInitializer loadedTypeInitializer,
                            List<? extends DynamicType> auxiliaryTypes,
                            TypeResolutionStrategy.Resolved typeResolutionStrategy) {
                super(typeDescription, binaryRepresentation, loadedTypeInitializer, auxiliaryTypes);
                this.typeResolutionStrategy = typeResolutionStrategy;
            }

            /**
             * {@inheritDoc}
             */
            public DynamicType.Loaded<T> load(ClassLoader classLoader) {
                return classLoader instanceof InjectionClassLoader && !((InjectionClassLoader) classLoader).isSealed()
                        ? load((InjectionClassLoader) classLoader, InjectionClassLoader.Strategy.INSTANCE)
                        : load(classLoader, ClassLoadingStrategy.Default.WRAPPER);
            }

            /**
             * {@inheritDoc}
             */
            public <S extends ClassLoader> DynamicType.Loaded<T> load(S classLoader, ClassLoadingStrategy<? super S> classLoadingStrategy) {
                return new Default.Loaded<T>(typeDescription,
                        binaryRepresentation,
                        loadedTypeInitializer,
                        auxiliaryTypes,
                        typeResolutionStrategy.initialize(this, classLoader, classLoadingStrategy));
            }

            /**
             * {@inheritDoc}
             */
            public DynamicType.Unloaded<T> include(DynamicType... dynamicType) {
                return include(Arrays.asList(dynamicType));
            }

            /**
             * {@inheritDoc}
             */
            public DynamicType.Unloaded<T> include(List<? extends DynamicType> dynamicType) {
                return new Default.Unloaded<T>(typeDescription,
                        binaryRepresentation,
                        loadedTypeInitializer,
                        CompoundList.of(auxiliaryTypes, dynamicType),
                        typeResolutionStrategy);
            }
        }

        /**
         * A default implementation of a loaded dynamic type.
         *
         * @param <T> The most specific known loaded type that is implemented by this dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        @HashCodeAndEqualsPlugin.Enhance
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

            /**
             * {@inheritDoc}
             */
            @SuppressWarnings("unchecked")
            public Class<? extends T> getLoaded() {
                return (Class<? extends T>) loadedTypes.get(typeDescription);
            }

            /**
             * {@inheritDoc}
             */
            public Map<TypeDescription, Class<?>> getLoadedAuxiliaryTypes() {
                Map<TypeDescription, Class<?>> loadedAuxiliaryTypes = new HashMap<TypeDescription, Class<?>>(
                        loadedTypes);
                loadedAuxiliaryTypes.remove(typeDescription);
                return loadedAuxiliaryTypes;
            }
        }
    }
}
