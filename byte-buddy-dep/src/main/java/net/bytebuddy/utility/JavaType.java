package net.bytebuddy.utility;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;

/**
 * Representations of Java types that do not exist in Java 6 but that have a special meaning to the JVM.
 */
public enum JavaType {

    /**
     * The Java 7 {@code java.lang.invoke.MethodHandle} type.
     */
    METHOD_HANDLE("java.lang.invoke.MethodHandle", Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT),

    /**
     * The Java 7 {@code java.lang.invoke.MethodType} type.
     */
    METHOD_TYPE("java.lang.invoke.MethodType", Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, Serializable.class),

    /**
     * The Java 7 {@code java.lang.invoke.MethodTypes.Lookup} type.
     */
    METHOD_HANDLES_LOOKUP("java.lang.invoke.MethodHandles$Lookup", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL),

    /**
     * The Java 7 {@code java.lang.invoke.CallSite} type.
     */
    CALL_SITE("java.lang.invoke.CallSite", Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT);

    /**
     * The type description to represent this type which is either a loaded type or a stub.
     */
    private final TypeDescription typeDescription;

    /**
     * Creates a new java type representation.
     *
     * @param typeName   The binary name of this type.
     * @param modifiers  The modifiers of this type when creating a stub.
     * @param interfaces The interfaces of this type when creating a stub.
     */
    JavaType(String typeName, int modifiers, Class<?>... interfaces) {
        TypeDescription typeDescription;
        try {
            typeDescription = new TypeDescription.ForLoadedType(Class.forName(typeName));
        } catch (Exception ignored) {
            typeDescription = new TypeDescription.Latent(typeName, modifiers, TypeDescription.OBJECT, new TypeList.ForLoadedType(interfaces));
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
