package net.bytebuddy.implementation.auxiliary;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodAccessorFactory;
import net.bytebuddy.utility.RandomString;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 辅助类型，什么辅助类型：修改原始类的字节码，织入一些方法为其他的织入方法提供帮助，注意一定要实现有意义的equals()和hashCode()，避免相同的目标类重复放入
 * An auxiliary type that provides services to the instrumentation of another type. Implementations should provide
 * meaningful {@code equals(Object)} and {@code hashCode()} implementations in order to avoid multiple creations
 * of this type. 一种辅助类型，为另一种类型的插装提供服务。实现应该提供有意义的{@code equals(Object)}和{@code hashCode()}实现，以避免这种类型的多次创建
 */
public interface AuxiliaryType {

    /**
     * The default type access of an auxiliary type. <b>This array must not be mutated</b>.
     */
    @SuppressFBWarnings(value = {"MS_MUTABLE_ARRAY", "MS_OOI_PKGPROTECT"}, justification = "The array is not to be modified by contract")
    ModifierContributor.ForType[] DEFAULT_TYPE_MODIFIER = {SyntheticState.SYNTHETIC};

    /**
     * Creates a new auxiliary type.
     *
     * @param auxiliaryTypeName     The fully qualified binary name for this auxiliary type. The type should be in
     *                              the same package than the instrumented type this auxiliary type is providing services
     *                              to in order to allow package-private access. 此辅助类型的完全限定二进制名称。该类型应与此辅助类型提供服务的检测类型位于同一个包中，以便允许包专用访问
     * @param classFileVersion      The class file version the auxiliary class should be written in.
     * @param methodAccessorFactory A factory for accessor methods.
     * @return A dynamically created type representing this auxiliary type.
     */
    DynamicType make(String auxiliaryTypeName, ClassFileVersion classFileVersion, MethodAccessorFactory methodAccessorFactory);

    /**
     * Representation of a naming strategy for an auxiliary type. 表示辅助类型的命名策略
     */
    interface NamingStrategy {

        /**
         * Names an auxiliary type.
         *
         * @param instrumentedType The instrumented type for which an auxiliary type is registered.
         * @return The fully qualified name for the given auxiliary type.
         */
        String name(TypeDescription instrumentedType);

        /**
         * A naming strategy for an auxiliary type which returns the instrumented type's name with a fixed extension
         * and a random number as a suffix. All generated names will be in the same package as the instrumented type. 一种辅助类型的命名策略，它返回插入指令的类型的名称，并带有固定的扩展名和一个随机数作为后缀。所有生成的名称都将与插入指令的类型位于同一个包中
         */
        @HashCodeAndEqualsPlugin.Enhance
        class SuffixingRandom implements NamingStrategy {

            /**
             * The suffix to append to the instrumented type for creating names for the auxiliary types. 附加到插入指令的类型的后缀，用于为辅助类型创建名称
             */
            private final String suffix;

            /**
             * An instance for creating random values.
             */
            @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.IGNORE)
            private final RandomString randomString;

            /**
             * Creates a new suffixing random naming strategy.
             *
             * @param suffix The suffix to extend to the instrumented type.
             */
            public SuffixingRandom(String suffix) {
                this.suffix = suffix;
                randomString = new RandomString();
            }

            @Override
            public String name(TypeDescription instrumentedType) {
                return instrumentedType.getName() + "$" + suffix + "$" + randomString.nextString();
            }
        }
    }

    /**
     * A marker to indicate that an auxiliary type is part of the instrumented types signature. This information can be used to load a type before
     * the instrumented type such that reflection on the instrumented type does not cause a {@link NoClassDefFoundError}.  标注辅助类型是否参与类型签名的生成，避免生成前面时报NoClassDefFoundError错误
     */
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE)
    @interface SignatureRelevant {
        /* empty */
    }
}

// 举个例
// class A 的方法 String hello()，要替换成 class B 的方法 String hello()。
//
// 简单的实现是: 修改A的实现字节码，实现中调用B的hello方法。
// A.hello() –> B.hello()
//
// 复杂的实现：定义一个 helloProxy() 方法，修改A的实现字节码，实现中调的helloProxy()方法。然后helloProxy()中调用B的实现。
// A.hello –> Proxy.helloProxy() –> B.helloProxy()。
//
// 复杂实现的好处是，如果要换成class C的方法String hello()，那么无需再特定的实现，和Proxy.helloProxy()交互就可以。
//
// 所以看出织入A的代码并不是目的代码，而是一个辅助的工具类Proxy。这就是辅助类的作用