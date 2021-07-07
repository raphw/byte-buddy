package net.bytebuddy.implementation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.utility.privilege.SetAccessibleAction;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementations of this interface explicitly initialize a loaded type. Usually, such implementations inject runtime
 * context into an instrumented type which cannot be defined by the means of the Java class file format. 此接口的实现显式初始化加载的类型。通常，这样的实现将运行时上下文注入到一个不能通过Java类文件格式定义的插装类中
 */
public interface LoadedTypeInitializer {

    /**
     * Callback that is invoked on the creation of an instrumented type. If the loaded type initializer is alive, this
     * method should be implemented empty instead of throwing an exception. 在创建插桩类型时调用的回调。如果加载的类型初始值设定项是活动的，则此方法应实现为空，而不是引发异常
     *
     * @param type The manifestation of the instrumented type.
     */
    void onLoad(Class<?> type);

    /**
     * Indicates if this initializer is alive and needs to be invoked. This is only meant as a mark. A loaded type
     * initializer that is not alive might still be called and must therefore not throw an exception but rather
     * provide an empty implementation. 指示此初始化器是否处于活动状态并需要调用。这只是一个标记。可能仍会调用未激活的已加载类型初始化器，因此不能引发异常，而是提供空实现
     *
     * @return {@code true} if this initializer is alive.
     */
    boolean isAlive();

    /**
     * A loaded type initializer that does not do anything. 不执行任何操作的已加载类型初始值设定项
     */
    enum NoOp implements LoadedTypeInitializer {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public void onLoad(Class<?> type) {
            /* do nothing */
        }

        @Override
        public boolean isAlive() {
            return false;
        }
    }

    /**
     * A type initializer for setting a value for a static field. 用于设置静态字段值的类型初始值设定项
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForStaticField implements LoadedTypeInitializer, Serializable {

        /**
         * This class's serial version UID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * A value for accessing a static field.
         */
        private static final Object STATIC_FIELD = null;

        /**
         * The name of the field.
         */
        private final String fieldName;

        /**
         * The value of the field.
         */
        private final Object value;

        /**
         * Creates a new {@link LoadedTypeInitializer} for setting a static field.
         *
         * @param fieldName the name of the field.
         * @param value     The value to be set.
         */
        protected ForStaticField(String fieldName, Object value) {
            this.fieldName = fieldName;
            this.value = value;
        }

        @Override
        public void onLoad(Class<?> type) {
            try {
                Field field = type.getDeclaredField(fieldName);
                if (!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(field.getDeclaringClass().getModifiers())) {
                    AccessController.doPrivileged(new SetAccessibleAction<Field>(field));
                }
                field.set(STATIC_FIELD, value);
            } catch (IllegalAccessException exception) {
                throw new IllegalArgumentException("Cannot access " + fieldName + " from " + type, exception);
            } catch (NoSuchFieldException exception) {
                throw new IllegalStateException("There is no field " + fieldName + " defined on " + type, exception);
            }
        }

        @Override
        public boolean isAlive() {
            return true;
        }
    }

    /**
     * A compound loaded type initializer that combines several type initializers. 组合了几个类型初始值设定项的复合加载类型初始值设定项
     */
    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "Serialization is considered opt-in for a rare use case")
    @HashCodeAndEqualsPlugin.Enhance
    class Compound implements LoadedTypeInitializer, Serializable {

        /**
         * This class's serial version UID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * The loaded type initializers that are represented by this compound type initializer. 由该复合类型初始值设定项表示的已加载类型初始值设定项
         */
        private final List<LoadedTypeInitializer> loadedTypeInitializers;

        /**
         * Creates a new compound loaded type initializer.
         *
         * @param loadedTypeInitializer A number of loaded type initializers in their invocation order.
         */
        public Compound(LoadedTypeInitializer... loadedTypeInitializer) {
            this(Arrays.asList(loadedTypeInitializer));
        }

        /**
         * Creates a new compound loaded type initializer.
         *
         * @param loadedTypeInitializers A number of loaded type initializers in their invocation order.
         */
        public Compound(List<? extends LoadedTypeInitializer> loadedTypeInitializers) {
            this.loadedTypeInitializers = new ArrayList<LoadedTypeInitializer>();
            for (LoadedTypeInitializer loadedTypeInitializer : loadedTypeInitializers) {
                if (loadedTypeInitializer instanceof Compound) {
                    this.loadedTypeInitializers.addAll(((Compound) loadedTypeInitializer).loadedTypeInitializers);
                } else if (!(loadedTypeInitializer instanceof NoOp)) {
                    this.loadedTypeInitializers.add(loadedTypeInitializer);
                }
            }
        }

        @Override
        public void onLoad(Class<?> type) {
            for (LoadedTypeInitializer loadedTypeInitializer : loadedTypeInitializers) {
                loadedTypeInitializer.onLoad(type);
            }
        }

        @Override
        public boolean isAlive() {
            for (LoadedTypeInitializer loadedTypeInitializer : loadedTypeInitializers) {
                if (loadedTypeInitializer.isAlive()) {
                    return true;
                }
            }
            return false;
        }
    }
}
