package net.bytebuddy.implementation;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;

/**
 * A factory for creating method proxies for an auxiliary type. Such proxies are required to allow a type to
 * call methods of a second type that are usually not accessible for the first type. This strategy is also adapted
 * by the Java compiler that creates accessor methods for example to implement inner classes. 为辅助类型创建方法代理的工厂。这样的代理需要允许一个类调用第二个类的方法，而第一个类通常无法访问这些方法。Java编译器也采用了这种策略，例如，它创建了访问器方法来实现内部类
 */
public interface MethodAccessorFactory {

    /**
     * Registers an accessor method for a
     * {@link Implementation.SpecialMethodInvocation} which cannot itself be
     * triggered invoked directly from outside a type. The method is registered on the instrumented type
     * with package-private visibility, similarly to a Java compiler's accessor methods. 为 {@link Implementation.SpecialMethodInvocation} 注册访问器方法，该方法本身不能从类型外部直接触发或调用。该方法以包私有可见性在插桩类型上注册，类似于Java编译器的访问器方法
     *
     * @param specialMethodInvocation The special method invocation.
     * @param accessType              The required access type.
     * @return The accessor method for invoking the special method invocation.
     */
    MethodDescription.InDefinedShape registerAccessorFor(Implementation.SpecialMethodInvocation specialMethodInvocation, AccessType accessType);

    /**
     * Registers a getter for the given {@link net.bytebuddy.description.field.FieldDescription} which might
     * itself not be accessible from outside the class. The returned getter method defines the field type as
     * its return type, does not take any arguments and is of package-private visibility, similarly to the Java
     * compiler's accessor methods. If the field is {@code static}, this accessor method is also {@code static}.
     *
     * @param fieldDescription The field which is to be accessed.
     * @param accessType       The required access type.
     * @return A getter method for the given field.
     */
    MethodDescription.InDefinedShape registerGetterFor(FieldDescription fieldDescription, AccessType accessType);

    /**
     * Registers a setter for the given {@link FieldDescription} which might
     * itself not be accessible from outside the class. The returned setter method defines the field type as
     * its only argument type, returns {@code void} and is of package-private visibility, similarly to the Java
     * compiler's accessor methods. If the field is {@code static}, this accessor method is also {@code static}.
     *
     * @param fieldDescription The field which is to be accessed.
     * @param accessType       The required access type.
     * @return A setter method for the given field.
     */
    MethodDescription.InDefinedShape registerSetterFor(FieldDescription fieldDescription, AccessType accessType);

    /**
     * Indicates the type of access to an accessor method. 指示访问器方法的访问类型
     */
    enum AccessType {

        /**
         * An access with {@code public visibility}. 具有 {@code public visibility} 的访问
         */
        PUBLIC(Visibility.PUBLIC),

        /**
         * An access with default visibility. 具有默认可见性的访问
         */
        DEFAULT(Visibility.PACKAGE_PRIVATE);

        /**
         * The implied visibility. 隐含的可见性
         */
        private final Visibility visibility;

        /**
         * Creates a new access type.
         *
         * @param visibility The implied visibility.
         */
        AccessType(Visibility visibility) {
            this.visibility = visibility;
        }

        /**
         * Returns the implied visibility.
         *
         * @return The implied visibility.
         */
        public Visibility getVisibility() {
            return visibility;
        }
    }

    /**
     * A method accessor factory that forbids any accessor registration. 禁止任何访问器注册的方法访问器工厂
     */
    enum Illegal implements MethodAccessorFactory {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public MethodDescription.InDefinedShape registerAccessorFor(Implementation.SpecialMethodInvocation specialMethodInvocation, AccessType accessType) {
            throw new IllegalStateException("It is illegal to register an accessor for this type");
        }

        @Override
        public MethodDescription.InDefinedShape registerGetterFor(FieldDescription fieldDescription, AccessType accessType) {
            throw new IllegalStateException("It is illegal to register a field getter for this type");
        }

        @Override
        public MethodDescription.InDefinedShape registerSetterFor(FieldDescription fieldDescription, AccessType accessType) {
            throw new IllegalStateException("It is illegal to register a field setter for this type");
        }
    }
}
