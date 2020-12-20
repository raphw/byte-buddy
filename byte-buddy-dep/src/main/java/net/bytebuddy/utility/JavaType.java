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
package net.bytebuddy.utility;

import net.bytebuddy.build.CachedReturnPlugin;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.lang.reflect.*;

/**
 * Representations of Java types that do not exist in Java 6 but that have a special meaning to the JVM.
 */
public enum JavaType {

    /**
     * The Java 12 {@code java.lang.constant.Constable} type.
     */
    CONSTABLE("java.lang.constant.Constable", Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE, TypeDescription.UNDEFINED),

    /**
     * The Java 12 {@code java.lang.invoke.TypeDescriptor} type.
     */
    TYPE_DESCRIPTOR("java.lang.invoke.TypeDescriptor", Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE, TypeDescription.UNDEFINED),

    /**
     * The Java 12 {@code java.lang.invoke.TypeDescriptor$OfMethod} type.
     */
    TYPE_DESCRIPTOR_OF_FIELD("java.lang.invoke.TypeDescriptor$OfField",
            Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
            TypeDescription.UNDEFINED,
            TYPE_DESCRIPTOR.getTypeStub()),

    /**
     * The Java 12 {@code java.lang.invoke.TypeDescriptor$OfMethod} type.
     */
    TYPE_DESCRIPTOR_OF_METHOD("java.lang.invoke.TypeDescriptor$OfMethod",
            Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
            TypeDescription.UNDEFINED,
            TYPE_DESCRIPTOR.getTypeStub()),

    /**
     * The Java 12 {@code java.lang.constant.ConstableDesc} type.
     */
    CONSTANT_DESCRIPTION("java.lang.constant.ConstantDesc", Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE, TypeDescription.UNDEFINED),

    /**
     * The Java 12 {@code java.lang.constant.DynamicConstantDesc} type.
     */
    DYNAMIC_CONSTANT_DESCRIPTION("java.lang.constant.DynamicConstantDesc",
            Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
            TypeDescription.OBJECT,
            CONSTANT_DESCRIPTION.getTypeStub()),

    /**
     * The Java 12 {@code java.lang.constant.ClassDesc} type.
     */
    CLASS_DESCRIPTION("java.lang.constant.ClassDesc",
            Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
            TypeDescription.UNDEFINED,
            CONSTANT_DESCRIPTION.getTypeStub(),
            TYPE_DESCRIPTOR_OF_FIELD.getTypeStub()),

    /**
     * The Java 12 {@code java.lang.constant.MethodTypeDesc} type.
     */
    METHOD_TYPE_DESCRIPTION("java.lang.constant.MethodTypeDesc",
            Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
            TypeDescription.UNDEFINED,
            CONSTANT_DESCRIPTION.getTypeStub(),
            TYPE_DESCRIPTOR_OF_METHOD.getTypeStub()),

    /**
     * The Java 12 {@code java.lang.constant.MethodHandleDesc} type.
     */
    METHOD_HANDLE_DESCRIPTION("java.lang.constant.MethodHandleDesc",
            Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
            TypeDescription.UNDEFINED,
            CONSTANT_DESCRIPTION.getTypeStub()),

    /**
     * The Java 12 {@code java.lang.constant.DirectMethodHandleDesc} type.
     */
    DIRECT_METHOD_HANDLE_DESCRIPTION("java.lang.constant.DirectMethodHandleDesc",
            Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
            TypeDescription.UNDEFINED,
            METHOD_HANDLE_DESCRIPTION.getTypeStub()),

    /**
     * The Java 7 {@code java.lang.invoke.MethodHandle} type.
     */
    METHOD_HANDLE("java.lang.invoke.MethodHandle", Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, TypeDescription.OBJECT, CONSTABLE.getTypeStub()),

    /**
     * The Java 7 {@code java.lang.invoke.MethodHandles} type.
     */
    METHOD_HANDLES("java.lang.invoke.MethodHandles", Opcodes.ACC_PUBLIC, Object.class),

    /**
     * The Java 7 {@code java.lang.invoke.MethodType} type.
     */
    METHOD_TYPE("java.lang.invoke.MethodType",
            Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
            TypeDescription.OBJECT,
            CONSTABLE.getTypeStub(),
            TYPE_DESCRIPTOR_OF_METHOD.getTypeStub(),
            TypeDescription.ForLoadedType.of(Serializable.class)),

    /**
     * The Java 7 {@code java.lang.invoke.MethodTypes.Lookup} type.
     */
    METHOD_HANDLES_LOOKUP("java.lang.invoke.MethodHandles$Lookup", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, Object.class),

    /**
     * The Java 7 {@code java.lang.invoke.CallSite} type.
     */
    CALL_SITE("java.lang.invoke.CallSite", Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, Object.class),

    /**
     * The Java 9 {@code java.lang.invoke.VarHandle} type.
     */
    VAR_HANDLE("java.lang.invoke.VarHandle", Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, TypeDescription.Generic.OBJECT, CONSTABLE.getTypeStub()),

    /**
     * The Java 8 {@code java.lang.reflect.Parameter} type.
     */
    PARAMETER("java.lang.reflect.Parameter", Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, Object.class, AnnotatedElement.class),

    /**
     * The Java 7 {@code java.lang.reflect.Executable} type.
     */
    EXECUTABLE("java.lang.reflect.Executable", Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, AccessibleObject.class, Member.class, GenericDeclaration.class),

    /**
     * The Java 9 {@code java.lang.Module} type.
     */
    MODULE("java.lang.Module", Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, Object.class, AnnotatedElement.class),

    /**
     * The Java 14 {@code java.lang.Record} type.
     */
    RECORD("java.lang.Record", Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, Object.class),

    /**
     * The Java 14 {@code java.lang.runtime.ObjectMethods} type.
     */
    OBJECT_METHODS("java.lang.runtime.ObjectMethods", Opcodes.ACC_PUBLIC, Object.class);

    /**
     * The type description to represent this type which is either a loaded type or a stub.
     */
    private final TypeDescription typeDescription;

    /**
     * Creates a new java type representation.
     *
     * @param typeName    The binary name of this type.
     * @param modifiers   The modifiers of this type when creating a stub.
     * @param superClass  The super class of this type when creating a stub.
     * @param anInterface The interfaces of this type when creating a stub.
     */
    JavaType(String typeName, int modifiers, Type superClass, Type... anInterface) {
        this(typeName, modifiers, superClass == null
                ? TypeDescription.Generic.UNDEFINED
                : TypeDescription.Generic.Sort.describe(superClass), new TypeList.Generic.ForLoadedTypes(anInterface));
    }

    /**
     * Creates a new java type representation.
     *
     * @param typeName    The binary name of this type.
     * @param modifiers   The modifiers of this type when creating a stub.
     * @param superClass  The super class of this type when creating a stub.
     * @param anInterface The interfaces of this type when creating a stub.
     */
    JavaType(String typeName, int modifiers, TypeDefinition superClass, TypeDefinition... anInterface) {
        this(typeName, modifiers, superClass == null
                ? TypeDescription.Generic.UNDEFINED
                : superClass.asGenericType(), new TypeList.Generic.Explicit(anInterface));
    }

    /**
     * Creates a new java type representation.
     *
     * @param typeName   The binary name of this type.
     * @param modifiers  The modifiers of this type when creating a stub.
     * @param superClass The super class of this type when creating a stub.
     * @param interfaces The interfaces of this type when creating a stub.
     */
    JavaType(String typeName, int modifiers, TypeDescription.Generic superClass, TypeList.Generic interfaces) {
        typeDescription = new TypeDescription.Latent(typeName, modifiers, superClass, interfaces);
    }

    /**
     * Returns at least a stub representing this type where the stub does not define any methods or fields. If a type exists for
     * the current runtime, a loaded type representation is returned.
     *
     * @return A type description for this Java type.
     */
    public TypeDescription getTypeStub() {
        return typeDescription;
    }

    /**
     * Loads the class that is represented by this Java type.
     *
     * @return A loaded type of this Java type.
     * @throws ClassNotFoundException If the represented type cannot be loaded.
     */
    @CachedReturnPlugin.Enhance("loaded")
    public Class<?> load() throws ClassNotFoundException {
        return Class.forName(typeDescription.getName(), false, ClassLoadingStrategy.BOOTSTRAP_LOADER);
    }

    /**
     * Loads the class that is represented by this Java type and represents it as a {@link TypeDescription}.
     *
     * @return A loaded type of this Java type.
     * @throws ClassNotFoundException If the represented type cannot be loaded.
     */
    public TypeDescription loadAsDescription() throws ClassNotFoundException {
        return TypeDescription.ForLoadedType.of(load());
    }

    /**
     * Returns {@code true} if this type is available on the current JVM.
     *
     * @return {@code true} if this type is available on the current JVM.
     */
    @CachedReturnPlugin.Enhance("available")
    public boolean isAvailable() {
        try {
            load();
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    /**
     * Checks if the supplied object is an instance of this type.
     *
     * @param instance The instance to check.
     * @return {@code true} if the supplied object is an instance of this type.
     */
    public boolean isInstance(Object instance) {
        if (!isAvailable()) {
            return false;
        }
        try {
            return load().isInstance(instance);
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
