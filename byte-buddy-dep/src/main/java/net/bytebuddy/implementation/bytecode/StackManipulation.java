package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.implementation.Implementation;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Describes a manipulation of a method's operand stack that does not affect the frame's variable array. 描述对方法操作数堆栈的操作，该操作不影响框架的变量数组
 */
public interface StackManipulation {

    /**
     * Determines if this stack manipulation is valid. 确定此堆栈操作是否有效
     *
     * @return If {@code false}, this manipulation cannot be applied and should throw an exception.
     */
    boolean isValid();

    /**
     * Applies the stack manipulation that is described by this instance. 应用此实例描述的堆栈操作
     *
     * @param methodVisitor         The method visitor used to write the method implementation to. 用于将方法实现写入的方法访问者
     * @param implementationContext The context of the current implementation.
     * @return The changes to the size of the operand stack that are implied by this stack manipulation. 此堆栈操作所隐含的对操作数堆栈大小的更改
     */
    Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext);

    /**
     * Canonical representation of an illegal stack manipulation. 非法堆栈操作的规范表示
     */
    enum Illegal implements StackManipulation {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            throw new IllegalStateException("An illegal stack manipulation must not be applied");
        }
    }

    /**
     * Canonical representation of a legal stack manipulation which does not require any action. 不需要任何操作的合法堆栈操作的规范表示
     */
    enum Trivial implements StackManipulation {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            return StackSize.ZERO.toIncreasingSize();
        }
    }

    /**
     * A description of the size change that is imposed by some
     * {@link StackManipulation}. 对某些 {@link StackManipulation} 施加的大小更改的描述
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Size {

        /**
         * The impact of any size operation onto the operand stack. This value can be negative if more values
         * were consumed from the stack than added to it. 任何大小操作对操作数堆栈的影响。如果从堆栈中消耗的值多于添加到堆栈中的值，则此值可能为负值
         */
        private final int sizeImpact;

        /**
         * The maximal size of stack slots this stack manipulation ever requires. If an operation for example pushes
         * five values onto the stack and subsequently consumes three operations, this value should still be five
         * to express that a stack operation requires at least five slots in order to be applicable. 此堆栈操作所需的最大堆栈插槽大小。例如，如果一个操作将五个值推送到堆栈上并随后使用三个操作，则该值仍应为五，以表示堆栈操作至少需要五个插槽才能适用
         */
        private final int maximalSize;

        /**
         * Creates an immutable descriptor of the size change that is implied by some stack manipulation.
         *
         * @param sizeImpact  The change of the size of the operand stack that is implied by some stack manipulation. 某些栈操作暗含的操作数堆栈大小的更改
         * @param maximalSize The maximal stack size that is required for executing this stack manipulation. Should
         *                    never be negative number.
         */
        public Size(int sizeImpact, int maximalSize) {
            this.sizeImpact = sizeImpact;
            this.maximalSize = maximalSize;
        }

        /**
         * Returns the size change on the operand stack that is represented by this instance. 返回此实例表示的操作数堆栈的大小更改
         *
         * @return The size change on the operand stack that is represented by this instance.
         */
        public int getSizeImpact() {
            return sizeImpact;
        }

        /**
         * Returns the maximal interim size of the operand stack that is represented by this instance.
         *
         * @return The maximal interim size of the operand stack that is represented by this instance.
         */
        public int getMaximalSize() {
            return maximalSize;
        }

        /**
         * Concatenates this size representation with another size representation in order to represent the size
         * change that is represented by both alterations of the operand stack size. 将此大小表示形式与另一个大小表示形式相连接，以便表示由操作数堆栈大小的两个更改表示的大小更改
         *
         * @param other The other size representation.
         * @return A new size representation representing both stack size requirements.
         */
        public Size aggregate(Size other) {
            return aggregate(other.sizeImpact, other.maximalSize);
        }

        /**
         * Aggregates a size change with this stack manipulation size.
         *
         * @param sizeChange         The change in size the other operation implies.
         * @param interimMaximalSize The interim maximal size of the operand stack that the other operation requires
         *                           at least to function.
         * @return The aggregated size.
         */
        private Size aggregate(int sizeChange, int interimMaximalSize) {
            return new Size(sizeImpact + sizeChange, Math.max(maximalSize, sizeImpact + interimMaximalSize));
        }
    }

    /**
     * An immutable stack manipulation that aggregates a sequence of other stack manipulations. 一种不可变的堆栈操作，它聚合了一系列其他堆栈操作
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Compound implements StackManipulation {

        /**
         * The stack manipulations this compound operation represents in their application order. 此复合操作按应用程序顺序表示的堆栈操作
         */
        private final List<StackManipulation> stackManipulations;

        /**
         * Creates a new compound stack manipulation. 创建新的复合堆栈操作
         *
         * @param stackManipulation The stack manipulations to be composed in the order of their composition. 要按组合顺序组合的堆栈操作
         */
        public Compound(StackManipulation... stackManipulation) {
            this(Arrays.asList(stackManipulation));
        }

        /**
         * Creates a new compound stack manipulation. 创建一个新的复合堆栈操作
         *
         * @param stackManipulations The stack manipulations to be composed in the order of their composition. 堆栈操作按其组成顺序进行组成
         */
        public Compound(List<? extends StackManipulation> stackManipulations) {
            this.stackManipulations = new ArrayList<StackManipulation>();
            for (StackManipulation stackManipulation : stackManipulations) {
                if (stackManipulation instanceof Compound) {
                    this.stackManipulations.addAll(((Compound) stackManipulation).stackManipulations);
                } else if (!(stackManipulation instanceof Trivial)) {
                    this.stackManipulations.add(stackManipulation);
                }
            }
        }

        @Override
        public boolean isValid() {
            for (StackManipulation stackManipulation : stackManipulations) {
                if (!stackManipulation.isValid()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            Size size = new Size(0, 0);
            for (StackManipulation stackManipulation : stackManipulations) {
                size = size.aggregate(stackManipulation.apply(methodVisitor, implementationContext));
            }
            return size;
        }
    }
}
