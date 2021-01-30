package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * An access representation to a given field. 对给定字段的访问表示 类似，但是更复杂一点。代表了 field 和 method
 */
public enum FieldAccess {

    /**
     * The representation of field access to a static field.
     */
    STATIC(Opcodes.PUTSTATIC, Opcodes.GETSTATIC, StackSize.ZERO),

    /**
     * The representation of field access to an instance field.
     */
    INSTANCE(Opcodes.PUTFIELD, Opcodes.GETFIELD, StackSize.SINGLE);

    /**
     * The opcode for setting a field value.
     */
    private final int putterOpcode;

    /**
     * The opcode for getting a field value.
     */
    private final int getterOpcode;

    /**
     * The amount of operand slots this field access operation consumes when it is applied before eventually
     * adding new values onto the operand stack. 在将新值最终添加到操作数堆栈之前应用此字段访问操作时所消耗的操作数插槽数
     */
    private final int targetSizeChange;

    /**
     * Creates a new field access.
     *
     * @param putterOpcode     The opcode for setting a field value.
     * @param getterOpcode     The opcode for getting a field value.
     * @param targetSizeChange The amount of operand slots this field access operation consumes when it is applied
     *                         before eventually adding new values onto the operand stack.
     */
    FieldAccess(int putterOpcode, int getterOpcode, StackSize targetSizeChange) {
        this.putterOpcode = putterOpcode;
        this.getterOpcode = getterOpcode;
        this.targetSizeChange = targetSizeChange.getSize();
    }

    /**
     * Creates an accessor to read an enumeration value. 创建用于读取枚举值的访问器
     *
     * @param enumerationDescription The description of the enumeration.
     * @return A stack manipulation for reading the enumeration.
     */
    public static StackManipulation forEnumeration(EnumerationDescription enumerationDescription) {
        FieldList<FieldDescription.InDefinedShape> fieldList = enumerationDescription.getEnumerationType()
                .getDeclaredFields()
                .filter(named(enumerationDescription.getValue()));
        return fieldList.size() != 1 || !fieldList.getOnly().isStatic() || !fieldList.getOnly().isPublic() || !fieldList.getOnly().isEnum()
                ? StackManipulation.Illegal.INSTANCE
                : STATIC.new AccessDispatcher(fieldList.getOnly()).read();
    }

    /**
     * Creates a field access representation for a given field. 为给定字段创建字段访问表示
     *
     * @param fieldDescription The field to be accessed.
     * @return A field access definition for the given field.
     */
    public static Defined forField(FieldDescription.InDefinedShape fieldDescription) {
        return fieldDescription.isStatic()
                ? STATIC.new AccessDispatcher(fieldDescription)
                : INSTANCE.new AccessDispatcher(fieldDescription);
    }

    /**
     * Creates a field access representation for a given field. If the field's return type derives from its declared shape, the value
     * is additionally casted to the generically resolved field. 为给定字段创建字段访问表示。如果字段的返回类型派生自其声明的形状，则该值将额外强制转换为一般解析的字段
     *
     * @param fieldDescription The field to be accessed. 要访问的字段
     * @return A field access definition for the given field. 给定字段的字段访问定义
     */
    public static Defined forField(FieldDescription fieldDescription) {
        FieldDescription.InDefinedShape declaredField = fieldDescription.asDefined();
        return fieldDescription.getType().asErasure().equals(declaredField.getType().asErasure())
                ? forField(declaredField)
                : OfGenericField.of(fieldDescription, forField(declaredField));
    }

    /**
     * Representation of a field access for which a getter and a setter can be created. 可以为其创建getter和setter的字段访问的表示
     */
    public interface Defined {

        /**
         * Creates a getter representation for a given field.
         *
         * @return A stack manipulation representing the retrieval of a field value.
         */
        StackManipulation read();

        /**
         * Creates a setter representation for a given field. 为给定字段创建setter表示
         *
         * @return A stack manipulation representing the setting of a field value.
         */
        StackManipulation write();
    }

    /**
     * A dispatcher for implementing a generic read or write access on a field. 一种调度器，用于实现对字段的通用读写访问
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class OfGenericField implements Defined {

        /**
         * The resolved generic field type.
         */
        private final TypeDefinition targetType;

        /**
         * An accessor for the field in its defined shape.
         */
        private final Defined defined;

        /**
         * Creates a new dispatcher for a generic field.
         *
         * @param targetType The resolved generic field type.
         * @param defined    An accessor for the field in its defined shape.
         */
        protected OfGenericField(TypeDefinition targetType, Defined defined) {
            this.targetType = targetType;
            this.defined = defined;
        }

        /**
         * Creates a generic access dispatcher for a given field.
         *
         * @param fieldDescription The field that is being accessed.
         * @param fieldAccess      A field accessor for the field in its defined shape.
         * @return A field access dispatcher for the given field.
         */
        protected static Defined of(FieldDescription fieldDescription, Defined fieldAccess) {
            return new OfGenericField(fieldDescription.getType(), fieldAccess);
        }

        @Override
        public StackManipulation read() {
            return new StackManipulation.Compound(defined.read(), TypeCasting.to(targetType));
        }

        @Override
        public StackManipulation write() {
            return defined.write();
        }
    }

    /**
     * A dispatcher for implementing a non-generic read or write access on a field. 一种调度器，用于实现对字段的非通用读或写访问
     */
    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
    protected class AccessDispatcher implements Defined {

        /**
         * A description of the accessed field. 已访问字段的说明
         */
        private final FieldDescription.InDefinedShape fieldDescription;

        /**
         * Creates a new access dispatcher. 创建新的访问调度程序
         *
         * @param fieldDescription A description of the accessed field.
         */
        protected AccessDispatcher(FieldDescription.InDefinedShape fieldDescription) {
            this.fieldDescription = fieldDescription;
        }

        @Override
        public StackManipulation read() {
            return new FieldGetInstruction();
        }

        @Override
        public StackManipulation write() {
            return new FieldPutInstruction();
        }

        /**
         * An abstract base implementation for accessing a field value. 用于访问字段值的抽象基本实现
         */
        private abstract class AbstractFieldInstruction implements StackManipulation {

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                methodVisitor.visitFieldInsn(getOpcode(),
                        fieldDescription.getDeclaringType().getInternalName(),
                        fieldDescription.getInternalName(),
                        fieldDescription.getDescriptor());
                return resolveSize(fieldDescription.getType().getStackSize());
            }

            /**
             * Returns the opcode for implementing the field access. 返回用于实现字段访问的操作码
             *
             * @return The opcode for implementing the field access.
             */
            protected abstract int getOpcode();

            /**
             * Resolves the actual size of this field access operation. 解析此字段访问操作的实际大小
             *
             * @param fieldSize The size of the accessed field.
             * @return The size of the field access operation based on the field's size.
             */
            protected abstract Size resolveSize(StackSize fieldSize);
        }

        /**
         * A reading field access operation. 读取字段访问操作
         */
        @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
        protected class FieldGetInstruction extends AbstractFieldInstruction {

            @Override
            protected int getOpcode() {
                return getterOpcode;
            }

            @Override
            protected Size resolveSize(StackSize fieldSize) {
                int sizeChange = fieldSize.getSize() - targetSizeChange;
                return new Size(sizeChange, sizeChange);
            }
        }

        /**
         * A writing field access operation. 写入字段访问操作
         */
        @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
        protected class FieldPutInstruction extends AbstractFieldInstruction {

            @Override
            protected int getOpcode() {
                return putterOpcode;
            }

            @Override
            protected Size resolveSize(StackSize fieldSize) {
                return new Size(-1 * (fieldSize.getSize() + targetSizeChange), 0);
            }
        }
    }
}
