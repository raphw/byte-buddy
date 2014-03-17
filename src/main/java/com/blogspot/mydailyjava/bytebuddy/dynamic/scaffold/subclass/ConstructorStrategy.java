package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass;

import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.MethodRegistry;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.SuperMethodCall;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodList;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;

/**
 * A constructor strategy is responsible for creating bootstrap constructors for a
 * {@link SubclassDynamicTypeBuilder}.
 *
 * @see com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default
 */
public interface ConstructorStrategy {

    /**
     * Default implementations of constructor strategies.
     * <ol>
     * <li>The {@link com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default#NO_CONSTRUCTORS}
     * strategy is adding no constructors such that the instrumented type will by default not have any. This is legal by
     * Java byte code requirements. However, if no constructor is added manually if this strategy is applied, the type
     * is not constructable without using JVM non-public functionality.</li>
     * <li>The {@link com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default#DEFAULT_CONSTRUCTOR}
     * strategy is adding a default constructor that calls it's super types default constructor. If no such constructor is defined,
     * an {@link IllegalArgumentException} is thrown. Only {@code public} or {@code protected} constructors are considered
     * by this strategy.</li>
     * <li>The {@link com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default#IMITATE_SUPER_TYPE}
     * strategy is adding all constructors of the super type which are making direct calls to their super constructor of
     * same signature. Only {@code public} or {@code protected} constructors are considered by this strategy.</li>
     * </ol>
     */
    static enum Default implements ConstructorStrategy {

        NO_CONSTRUCTORS,
        DEFAULT_CONSTRUCTOR,
        IMITATE_SUPER_TYPE;

        @Override
        public MethodList extractConstructors(TypeDescription superType) {
            switch (this) {
                case NO_CONSTRUCTORS:
                    return new MethodList.Empty();
                case DEFAULT_CONSTRUCTOR:
                    MethodList methodList = superType.getDeclaredMethods()
                            .filter(isConstructor().and(takesArguments(0)).and(isPublic().or(isProtected())));
                    if (methodList.size() == 1) {
                        return methodList;
                    } else {
                        throw new IllegalArgumentException(superType + " does not declare a default constructor");
                    }
                case IMITATE_SUPER_TYPE:
                    return superType.getDeclaredMethods().filter(isConstructor().and(isPublic().or(isProtected())));
                default:
                    throw new AssertionError();
            }
        }

        @Override
        public MethodRegistry inject(MethodRegistry methodRegistry,
                                     MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory) {
            switch (this) {
                case NO_CONSTRUCTORS:
                    return methodRegistry;
                case DEFAULT_CONSTRUCTOR:
                case IMITATE_SUPER_TYPE:
                    return methodRegistry.append(new MethodRegistry.LatentMethodMatcher.Simple(isConstructor()),
                            SuperMethodCall.INSTANCE,
                            defaultMethodAttributeAppenderFactory);
                default:
                    throw new AssertionError();
            }
        }
    }

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
     * {@link com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy#extractConstructors(com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription)}
     * method of this instance.
     *
     * @param methodRegistry                        The original method registry.
     * @param defaultMethodAttributeAppenderFactory The default method attribute appender factory.
     * @return A method registry that is capable of providing byte code for the constructors that were added by
     * this strategy.
     */
    MethodRegistry inject(MethodRegistry methodRegistry,
                          MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory);
}
