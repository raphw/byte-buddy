package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.pool.TypePool;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * A class writer strategy is responsible for the creation of a {@link ClassWriter} when creating a type. 创建类型时，类编写器策略负责创建{@link ClassWriter}
 */
public interface ClassWriterStrategy {

    /**
     * Resolves a class writer. 解析类编写器
     *
     * @param flags    The flags to set.
     * @param typePool A type pool for locating types.
     * @return The class writer to use.
     */
    ClassWriter resolve(int flags, TypePool typePool);

    /**
     * Resolves a class writer.
     *
     * @param flags       The flags to set.
     * @param typePool    A type pool for locating types.
     * @param classReader The class reader from which the original class is read.
     * @return The class writer to use.
     */
    ClassWriter resolve(int flags, TypePool typePool, ClassReader classReader);

    /**
     * Default implementations of class writer strategies. 类编写器策略的默认实现
     */
    enum Default implements ClassWriterStrategy {

        /**
         * A class writer strategy that retains the original class's constant pool if applicable. 如果适用的话，将保留原始类的常量池的类编写器策略
         */
        CONSTANT_POOL_RETAINING {
            @Override
            public ClassWriter resolve(int flags, TypePool typePool, ClassReader classReader) {
                return new FrameComputingClassWriter(classReader, flags, typePool);
            }
        },

        /**
         * A class writer strategy that discards the original class's constant pool if applicable. 一个类编写器策略，如果适用，该策略将丢弃原始类的常量池
         */
        CONSTANT_POOL_DISCARDING {
            @Override
            public ClassWriter resolve(int flags, TypePool typePool, ClassReader classReader) {
                return resolve(flags, typePool);
            }
        };

        @Override
        public ClassWriter resolve(int flags, TypePool typePool) {
            return new FrameComputingClassWriter(flags, typePool);
        }
    }

    /**
     * A class writer that piggy-backs on Byte Buddy's {@link TypePool} to avoid class loading or look-up errors when redefining a class.
     * This is not available when creating a new class where automatic frame computation is however not normally a requirement. 但是，在创建通常不需要自动帧计算的新类时，此功能不可用
     */
    class FrameComputingClassWriter extends ClassWriter {

        /**
         * The type pool to use for computing stack map frames, if required. 如果需要，用于计算栈映射框架的类型池
         */
        private final TypePool typePool;

        /**
         * Creates a new frame computing class writer. 创建一个新的框架计算类编写器
         *
         * @param flags    The flags to be handed to the writer.
         * @param typePool The type pool to use for computing stack map frames, if required.
         */
        public FrameComputingClassWriter(int flags, TypePool typePool) {
            super(flags);
            this.typePool = typePool;
        }

        /**
         * Creates a new frame computing class writer. 创建一个新的框架计算类编写器
         *
         * @param classReader The class reader from which the original class is read.
         * @param flags       The flags to be handed to the writer.
         * @param typePool    The type pool to use for computing stack map frames, if required.
         */
        public FrameComputingClassWriter(ClassReader classReader, int flags, TypePool typePool) {
            super(classReader, flags);
            this.typePool = typePool;
        }

        @Override
        protected String getCommonSuperClass(String leftTypeName, String rightTypeName) {
            TypeDescription leftType = typePool.describe(leftTypeName.replace('/', '.')).resolve();
            TypeDescription rightType = typePool.describe(rightTypeName.replace('/', '.')).resolve();
            if (leftType.isAssignableFrom(rightType)) {
                return leftType.getInternalName();
            } else if (leftType.isAssignableTo(rightType)) {
                return rightType.getInternalName();
            } else if (leftType.isInterface() || rightType.isInterface()) {
                return TypeDescription.OBJECT.getInternalName();
            } else {
                do {
                    leftType = leftType.getSuperClass().asErasure();
                } while (!leftType.isAssignableFrom(rightType));
                return leftType.getInternalName();
            }
        }
    }
}
