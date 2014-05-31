package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.instrumentation.SuperMethodCall;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.type.TypeDescription;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;

/**
 * A constructor strategy is responsible for creating bootstrap constructors for a
 * {@link SubclassDynamicTypeBuilder}.
 *
 * @see net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default
 */
public interface ConstructorStrategy {

    /**
     * Extracts constructors for a given super type. The extracted constructor signatures will then be imitated by the
     * created dynamic type.
     *
     * @param superType The super type from which constructors are to be extracted.
     * @return A list of constructor descriptions which will be mimicked by the instrumented type of which
     * the {@code superType} is the direct super type of the instrumented type.
     */
    MethodList extractConstructors(TypeDescription superType);

    /**
     * Returns a method registry that is capable of creating byte code for the constructors that were
     * provided by the
     * {@link net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy#extractConstructors(net.bytebuddy.instrumentation.type.TypeDescription)}
     * method of this instance.
     *
     * @param methodRegistry                        The original method registry.
     * @param defaultMethodAttributeAppenderFactory The default method attribute appender factory.
     * @return A method registry that is capable of providing byte code for the constructors that were added by
     * this strategy.
     */
    MethodRegistry inject(MethodRegistry methodRegistry,
                          MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory);

    /**
     * Default implementations of constructor strategies.
     */
    static enum Default implements ConstructorStrategy {

        /**
         * This strategy is adding no constructors such that the instrumented type will by default not have any. This
         * is legal by Java byte code requirements. However, if no constructor is added manually if this strategy is
         * applied, the type is not constructable without using JVM non-public functionality.
         */
        NO_CONSTRUCTORS {
            @Override
            public MethodList extractConstructors(TypeDescription superType) {
                return new MethodList.Empty();
            }
        },

        /**
         * This strategy is adding a default constructor that calls it's super types default constructor. If no such
         * constructor is defined, an {@link IllegalArgumentException} is thrown. Note that the default constructor
         * needs to be visible to its sub type for this strategy to work.
         */
        DEFAULT_CONSTRUCTOR {
            @Override
            public MethodList extractConstructors(TypeDescription superType) {
                MethodList methodList = superType.getDeclaredMethods().filter(isConstructor().and(takesArguments(0)));
                if (methodList.size() == 1) {
                    return methodList;
                } else {
                    throw new IllegalArgumentException(superType + " does not declare a visible default constructor");
                }
            }
        },

        /**
         * This strategy is adding all constructors of the super type which are making direct calls to their super
         * constructor of same signature. Only {@code public} or {@code protected} constructors are considered by this
         * strategy.
         */
        IMITATE_SUPER_TYPE {
            @Override
            public MethodList extractConstructors(TypeDescription superType) {
                return superType.getDeclaredMethods().filter(isConstructor().and(isPublic().or(isProtected())));
            }
        },

        /**
         * This strategy is similar to the {@link net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default#IMITATE_SUPER_TYPE}
         * strategy but defines any constructor of the super type. This includes package-private and {@code private}
         * constructors which might not be visible to its sub type. If such constructors were added, it is a user's
         * responsibility to define a sub type to be in the correct package and to change the implementation of
         * private constructors to call another visible constructor.
         */
        IMITATE_SUPER_TYPE_FULLY {
            @Override
            public MethodList extractConstructors(TypeDescription superType) {
                return superType.getDeclaredMethods().filter(isConstructor());
            }
        };

        @Override
        public MethodRegistry inject(MethodRegistry methodRegistry,
                                     MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory) {
            switch (this) {
                case NO_CONSTRUCTORS:
                    return methodRegistry;
                case DEFAULT_CONSTRUCTOR:
                case IMITATE_SUPER_TYPE:
                case IMITATE_SUPER_TYPE_FULLY:
                    return methodRegistry.prepend(new MethodRegistry.LatentMethodMatcher.Simple(isConstructor()),
                            SuperMethodCall.INSTANCE,
                            defaultMethodAttributeAppenderFactory);
                default:
                    throw new AssertionError();
            }
        }
    }
}
