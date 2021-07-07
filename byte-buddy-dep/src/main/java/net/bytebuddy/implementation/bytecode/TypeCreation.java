package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A stack manipulation for creating an <i>undefined</i> type on which a constructor is to be called. 一种堆栈操作，用于创建一个未定义的类型，在该类型上调用构造函数
 */
@HashCodeAndEqualsPlugin.Enhance
public class TypeCreation implements StackManipulation {

    /**
     * The type that is being created. 正在创建的类型
     */
    private final TypeDescription typeDescription;

    /**
     * Constructs a new type creation. 构造一个新的类型创建
     *
     * @param typeDescription The type to be create.
     */
    protected TypeCreation(TypeDescription typeDescription) {
        this.typeDescription = typeDescription;
    }

    /**
     * Creates a type creation for the given type. 为给定类型创建类型创建
     *
     * @param typeDescription The type to be create.
     * @return A stack manipulation that represents the creation of the given type. 表示给定类型的创建的堆栈操作
     */
    public static StackManipulation of(TypeDescription typeDescription) {
        if (typeDescription.isArray() || typeDescription.isPrimitive() || typeDescription.isAbstract()) {
            throw new IllegalArgumentException(typeDescription + " is not instantiable");
        }
        return new TypeCreation(typeDescription);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitTypeInsn(Opcodes.NEW, typeDescription.getInternalName());
        return new Size(1, 1);
    }
}
