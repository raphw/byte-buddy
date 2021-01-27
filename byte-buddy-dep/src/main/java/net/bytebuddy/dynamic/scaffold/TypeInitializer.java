package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * A type initializer is responsible for defining a type's static initialization block. 类初始化器负责定义类的静态初始化块 <clinit>方法
 */
public interface TypeInitializer extends ByteCodeAppender {

    /**
     * Indicates if this type initializer is defined. 指示是否定义了此类型初始值设定项
     *
     * @return {@code true} if this type initializer is defined. {@code true}如果定义了此类型初始值设定项
     */
    boolean isDefined();

    /**
     * Expands this type initializer with another byte code appender. For this to be possible, this type initializer must
     * be defined. 用另一个字节码追加器展开此类型初始值设定项。为此，必须定义此类型初始值设定项
     *
     * @param byteCodeAppender The byte code appender to apply as the type initializer.
     * @return A defined type initializer.
     */
    TypeInitializer expandWith(ByteCodeAppender byteCodeAppender);

    /**
     * Creates a method pool record that applies this type initializer while preserving the record that was supplied. 创建应用此 类初始化器 的方法池记录，同时保留提供的记录
     *
     * @param record The record to wrap.
     * @return A new record that represents the supplied record while also executing this type initializer.
     */
    TypeWriter.MethodPool.Record wrap(TypeWriter.MethodPool.Record record);

    /**
     * A drain for writing a type initializer. 用于编写类型初始值设定项的 drain
     */
    interface Drain {

        /**
         * Applies the drain.
         *
         * @param classVisitor          The class visitor to apply the initializer to. 要应用初始值设定项的类访问者
         * @param typeInitializer       The type initializer to write. 要写入的类型初始值设定项
         * @param implementationContext The corresponding implementation context. 相应的实现上下文
         */
        void apply(ClassVisitor classVisitor, TypeInitializer typeInitializer, Implementation.Context implementationContext);

        /**
         * A default implementation of a type initializer drain that creates a initializer method. 创建初始值设定项方法 的 类型初始值设定项 drain 的默认实现
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Default implements Drain {

            /**
             * The instrumented type.
             */
            protected final TypeDescription instrumentedType;

            /**
             * The method pool to use. 要使用的方法池
             */
            protected final TypeWriter.MethodPool methodPool;

            /**
             * The annotation value filter factory to use.
             */
            protected final AnnotationValueFilter.Factory annotationValueFilterFactory;

            /**
             * Creates a new default type initializer drain. 创建新的 默认类初始器 drain
             *
             * @param instrumentedType             The instrumented type.
             * @param methodPool                   The method pool to use.
             * @param annotationValueFilterFactory The annotation value filter factory to use.
             */
            public Default(TypeDescription instrumentedType,
                           TypeWriter.MethodPool methodPool,
                           AnnotationValueFilter.Factory annotationValueFilterFactory) {
                this.instrumentedType = instrumentedType;
                this.methodPool = methodPool;
                this.annotationValueFilterFactory = annotationValueFilterFactory;
            }

            @Override
            public void apply(ClassVisitor classVisitor, TypeInitializer typeInitializer, Implementation.Context implementationContext) {
                typeInitializer.wrap(methodPool.target(new MethodDescription.Latent.TypeInitializer(instrumentedType))).apply(classVisitor,
                        implementationContext,
                        annotationValueFilterFactory);
            }
        }
    }

    /**
     * Canonical implementation of a non-defined type initializer. 未定义类初始器的规范实现
     */
    enum None implements TypeInitializer {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public boolean isDefined() {
            return false;
        }

        @Override
        public TypeInitializer expandWith(ByteCodeAppender byteCodeAppenderFactory) {
            return new TypeInitializer.Simple(byteCodeAppenderFactory);
        }

        @Override
        public TypeWriter.MethodPool.Record wrap(TypeWriter.MethodPool.Record record) {
            return record;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
            return new Size(0, 0);
        }
    }

    /**
     * A simple, defined type initializer that executes a given {@link ByteCodeAppender}. 一个简单的、已定义的类型初始值设定项，它执行给定的{@link ByteCodeAppender}
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Simple implements TypeInitializer {

        /**
         * The byte code appender to apply as the type initializer. 字节码追加器 用作 类初始值器
         */
        private final ByteCodeAppender byteCodeAppender;

        /**
         * Creates a new simple type initializer. 创建一个新的简单 类初始化器
         *
         * @param byteCodeAppender The byte code appender to apply as the type initializer.
         */
        public Simple(ByteCodeAppender byteCodeAppender) {
            this.byteCodeAppender = byteCodeAppender;
        }

        @Override
        public boolean isDefined() {
            return true;
        }

        @Override
        public TypeInitializer expandWith(ByteCodeAppender byteCodeAppender) {
            return new TypeInitializer.Simple(new Compound(this.byteCodeAppender, byteCodeAppender));
        }

        @Override
        public TypeWriter.MethodPool.Record wrap(TypeWriter.MethodPool.Record record) {
            return record.prepend(byteCodeAppender);
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
            return byteCodeAppender.apply(methodVisitor, implementationContext, instrumentedMethod);
        }
    }
}
/**
    总所周知，jvm 中一个对象的周期主要分为 加载 链接 初始化 使用 卸载几部分，  其中链接部分又分为 校验(Verification) 准备(preparation) 解析(resolution)

    加载过程 依赖双亲加载机制完成的，依赖的主体方法是 {@link java.lang.ClassLoader#loadClass(java.lang.String)}，首先查询缓存是否已经有过加载，然后 递归往上解析类，也就是在各自加载的范围去找对应的类。当父类都没有能够
 正常加载类的时候。这个时候会调用 {@link java.lang.ClassLoader#findClass(java.lang.String)} 完成类的加载工作

    类加载的二进制字节码文件可以来自 jar包、网络、数据库以及各种语言的编译器而来的.class 文件等各种来源

    加载的过程主要的工作项
    （1）通过类的全限定名（包名 + 类名） 来获取定义此类的二进制字节码流
    （2）将字节码流所代表的静态存储结构转化为运行时数据结构存储在方法区（java8 之后，迁移到 metaSpace），在 jvm 中的表示为 xxxKlass，其中有个字段 java_mirror 指向了 java/lang/Class的实例，当然该实例也存在相应的字段指向 xxxKlass
    （3）使用java/lang/Class对象，作为该类在上层的唯一入口

    验证阶段
        验证过程相对来说就有复杂一点了，不过验证过程对JVM的安全还是至关重要的，毕竟你不知道比人的代码究竟能干出些什么
    （1）文件格式验证
    （2）元数据验证
    （3）字节码验证
    （4）符号引用验证

    准备阶段
        准备阶段瞄准了类变量，将其放入 java/lang/Class这个对应的实例末尾，需要注意的是 这个阶段的初始化并不是<init> 或者 <clinit>操作，将这些类型按照既定格式的类进行 "置零" 操作，当然，遇到一些比如 final static 的字段，则会直接进行初始化操作的


    初始化阶段
        初始化阶段，这个时候执行<clinit>方法，而<clinit>方法是由编译器按照源码 <br>顺序<br/> 依次扫描 <br>类变量<br/> 的赋值动作 和 <br>static代码块<br/> 得到的

    （1）在类没有进行过初始化的前提下，当执行new、getStatic、setStatic、invokeStatic字节码指令时，类会立即初始化。对应的java操作就是new一个对象、读取/写入一个类变量（非final类型）或者执行静态方法
    （2）在类没有进行过初始化的前提下，当一个类的子类被初始化之前，该父类会立即初始化
    （3）在类没有进行过初始化的前提下，当包含main方法时，该类会第一个初始化
    （4）在类没有进行过初始化的前提下，当使用java.lang.reflect包的方法对类进行反射调用时，该类会立即初始化
    （5）在类没有进行过初始化的前提下，当使用JDK1.5支持时，如果一个java.langl.incoke.MethodHandle实例最后的解析结果REF_getStatic、REF_putStatic、REF_invokeStatic的方法句柄，并且这个方法句柄所对应的类没有进行过初始化，则需要先触发其初始化

        JVM虚拟机规定了几条标准：
         （1）先父类后子类，（源码中）先出现先执行
         （2）向前引用：一个类变量在定义前可以赋值，但是不能访问。
         （3）非必须：如果一个类或接口没有类变量的赋值动作和static代码块，那就不生成<clinit>方法.
         （4）执行接口的<clinit>方法不需要先执行父接口的<clinit>方法。只有当父接口中定义的变量被使用时，父接口才会被初始化。另外，接口的实现类在初始化时也一样不会执行接口的<clinit>方法。
         （5）同步性：<clinit>方法的执行具有同步性，并且只执行一次。但当一个线程执行该类的<clinit>方法时，其他的初始化线程需阻塞等待
 */