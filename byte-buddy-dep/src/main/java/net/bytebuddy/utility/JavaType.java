package net.bytebuddy.utility;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Member;

/**
 * Representations of Java types that do not exist in Java 6 but that have a special meaning to the JVM.
 */
public enum JavaType {

    /**
     * The Java 7 {@code java.lang.invoke.MethodHandle} type.
     */
    METHOD_HANDLE("java.lang.invoke.MethodHandle", Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, Object.class),

    /**
     * The Java 7 {@code java.lang.invoke.MethodType} type.
     */
    METHOD_TYPE("java.lang.invoke.MethodType", Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, Object.class, Serializable.class),

    /**
     * The Java 7 {@code java.lang.invoke.MethodTypes.Lookup} type.
     */
    METHOD_HANDLES_LOOKUP("java.lang.invoke.MethodHandles$Lookup", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, Object.class),

    /**
     * The Java 7 {@code java.lang.invoke.CallSite} type.
     */
    CALL_SITE("java.lang.invoke.CallSite", Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, Object.class),

    /**
     * The {@code java.lang.reflect.Executable} type.
     */
    EXECUTABLE("java.lang.reflect.Executable", Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, AccessibleObject.class, Member.class, GenericDeclaration.class),

    /**
     * The {@code java.lang.reflect.Module} type.
     */
    MODULE("java.lang.reflect.Module", Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, Object.class);

    /**
     * The type description to represent this type which is either a loaded type or a stub.
     */
    private final TypeDescription typeDescription;

    /**
     * Creates a new java type representation.
     *
     * @param typeName   The binary name of this type.
     * @param modifiers  The modifiers of this type when creating a stub.
     * @param superClass The super class of this type when creating a stub.
     * @param interfaces The interfaces of this type when creating a stub.
     */
    JavaType(String typeName, int modifiers, Class<?> superClass, Class<?>... interfaces) {
        TypeDescription typeDescription;
        try {
            typeDescription = new TypeDescription.ForLoadedType(Class.forName(typeName));
        } catch (Exception ignored) {
            typeDescription = new TypeDescription.Latent(typeName,
                    modifiers,
                    new TypeDescription.Generic.OfNonGenericType.ForLoadedType(superClass),
                    new TypeList.Generic.ForLoadedTypes(interfaces));
        }
        this.typeDescription = typeDescription;
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
    public Class<?> load() throws ClassNotFoundException {
        return Class.forName(typeDescription.getName());
    }

    @Override
    public String toString() {
        return "JavaType." + name();
    }
}
