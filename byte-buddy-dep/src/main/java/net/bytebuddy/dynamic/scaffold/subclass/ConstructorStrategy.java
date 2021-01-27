package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.Transformer;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMatcher;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A constructor strategy is responsible for creating bootstrap constructors for a
 * {@link SubclassDynamicTypeBuilder}. 负责为任何给定的类创建一组预定义的构造函数
 *
 * @see net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default
 */
public interface ConstructorStrategy {

    /**
     * Extracts constructors for a given super type. The extracted constructor signatures will then be imitated by the
     * created dynamic type.
     *
     * @param instrumentedType The type for which the constructors should be created.
     * @return A list of tokens that describe the constructors that are to be implemented.
     */
    List<MethodDescription.Token> extractConstructors(TypeDescription instrumentedType);

    /**
     * Returns a method registry that is capable of creating byte code for the constructors that were
     * provided by the
     * {@link net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy#extractConstructors(TypeDescription)}
     * method of this instance.
     *
     * @param instrumentedType The instrumented type.
     * @param methodRegistry   The original method registry.
     * @return A method registry that is capable of providing byte code for the constructors that were added by this strategy.
     */
    MethodRegistry inject(TypeDescription instrumentedType, MethodRegistry methodRegistry);

    /**
     * Default implementations of constructor strategies. Any such strategy offers to additionally apply an {@link MethodAttributeAppender.Factory}. 构造函数策略的默认实现。任何这样的策略都提供了一个 {@link MethodAttributeAppender.Factory}
     */
    enum Default implements ConstructorStrategy {

        /**
         * This strategy is adding no constructors such that the instrumented type will by default not have any. This
         * is legal by Java byte code requirements. However, if no constructor is added manually if this strategy is
         * applied, the type is not constructable without using JVM non-public functionality. 此策略不添加构造函数，这样 插桩类型 在默认情况下将没有任何构造函数。对于Java字节码而言是合理的。但是，如果应用此策略时没有手动添加构造函数，则在不使用JVM非公共功能的情况下无法构造类型
         */
        NO_CONSTRUCTORS { // 也就意味着必须手动添加构造函数
            @Override
            protected List<MethodDescription.Token> doExtractConstructors(TypeDescription superClass) {
                return Collections.emptyList();
            }

            @Override
            protected MethodRegistry doInject(MethodRegistry methodRegistry, MethodAttributeAppender.Factory methodAttributeAppenderFactory) {
                return methodRegistry;
            }
        },

        /**
         * This strategy is adding a default constructor that calls it's super types default constructor. If no such
         * constructor is defined by the super class, an {@link IllegalArgumentException} is thrown. Note that the default
         * constructor needs to be visible to its sub type for this strategy to work. The declared default constructor of
         * the created class is declared public and without annotations. 这个策略是添加一个默认构造函数来调用它的超类型默认构造函数。如果超类没有定义这样的构造函数，则抛出{@link IllegalArgumentException}。请注意，默认构造函数需要对其子类型可见，才能使此策略正常工作。所创建类的声明的默认构造函数被声明为public，并且没有注释
         */
        DEFAULT_CONSTRUCTOR {
            @Override
            protected List<MethodDescription.Token> doExtractConstructors(TypeDescription instrumentedType) {
                TypeDescription.Generic superClass = instrumentedType.getSuperClass();
                MethodList<?> defaultConstructors = superClass == null
                        ? new MethodList.Empty<MethodDescription.InGenericShape>()
                        : superClass.getDeclaredMethods().filter(isConstructor().and(takesArguments(0)).<MethodDescription>and(isVisibleTo(instrumentedType)));
                if (defaultConstructors.size() == 1) {
                    return Collections.singletonList(new MethodDescription.Token(Opcodes.ACC_PUBLIC));
                } else {
                    throw new IllegalArgumentException(instrumentedType.getSuperClass() + " declares no constructor that is visible to " + instrumentedType);
                }
            }

            @Override
            protected MethodRegistry doInject(MethodRegistry methodRegistry, MethodAttributeAppender.Factory methodAttributeAppenderFactory) {
                return methodRegistry.append(new LatentMatcher.Resolved<MethodDescription>(isConstructor()),
                        new MethodRegistry.Handler.ForImplementation(SuperMethodCall.INSTANCE),
                        methodAttributeAppenderFactory,
                        Transformer.NoOp.<MethodDescription>make());
            }
        },

        /**
         * This strategy is adding all constructors of the instrumented type's super class where each constructor is
         * directly invoking its signature-equivalent super class constructor. Only constructors that are visible to the
         * instrumented type are added, i.e. package-private constructors are only added if the super type is defined
         * in the same package as the instrumented type and private constructors are always skipped. 此策略是添加插桩类型的超类的所有构造函数，其中每个构造函数直接调用其签名等价的超类构造函数。只添加对插桩类型可见的构造函数，即仅当超级类型与插桩类型在同一个包中定义并且始终跳过私有构造函数时，才添加包私有构造函数
         */
        IMITATE_SUPER_CLASS {
            @Override
            protected List<MethodDescription.Token> doExtractConstructors(TypeDescription instrumentedType) {
                TypeDescription.Generic superClass = instrumentedType.getSuperClass();
                return (superClass == null
                        ? new MethodList.Empty<MethodDescription.InGenericShape>()
                        : superClass.getDeclaredMethods().filter(isConstructor().and(isVisibleTo(instrumentedType)))).asTokenList(is(instrumentedType));
            }

            @Override
            public MethodRegistry doInject(MethodRegistry methodRegistry, MethodAttributeAppender.Factory methodAttributeAppenderFactory) {
                return methodRegistry.append(new LatentMatcher.Resolved<MethodDescription>(isConstructor()),
                        new MethodRegistry.Handler.ForImplementation(SuperMethodCall.INSTANCE),
                        methodAttributeAppenderFactory,
                        Transformer.NoOp.<MethodDescription>make());
            }
        },

        /**
         * This strategy is adding all constructors of the instrumented type's super class where each constructor is
         * directly invoking its signature-equivalent super class constructor. Only {@code public} constructors are
         * added. 此策略是添加插桩类型的超类的所有构造函数，其中每个构造函数直接调用其签名等价的超类构造函数。只添加{@code public}构造函数
         */
        IMITATE_SUPER_CLASS_PUBLIC {
            @Override
            protected List<MethodDescription.Token> doExtractConstructors(TypeDescription instrumentedType) {
                TypeDescription.Generic superClass = instrumentedType.getSuperClass();
                return (superClass == null
                        ? new MethodList.Empty<MethodDescription.InGenericShape>()
                        : superClass.getDeclaredMethods().filter(isPublic().and(isConstructor()))).asTokenList(is(instrumentedType));
            }

            @Override
            public MethodRegistry doInject(MethodRegistry methodRegistry, MethodAttributeAppender.Factory methodAttributeAppenderFactory) {
                return methodRegistry.append(new LatentMatcher.Resolved<MethodDescription>(isConstructor()),
                        new MethodRegistry.Handler.ForImplementation(SuperMethodCall.INSTANCE),
                        methodAttributeAppenderFactory,
                        Transformer.NoOp.<MethodDescription>make());
            }
        },

        /**
         * This strategy is adding all constructors of the instrumented type's super class where each constructor is
         * directly invoking its signature-equivalent super class constructor. A constructor is added for any constructor
         * of the super class that is invokable and is declared as {@code public}. 此策略是添加插桩类型超类的所有构造函数，其中每个构造函数都直接调用其签名等效的超类构造函数。为超类的任何构造函数添加一个构造函数，该类可调用并声明为{@code public}
         */
        IMITATE_SUPER_CLASS_OPENING {
            @Override
            protected List<MethodDescription.Token> doExtractConstructors(TypeDescription instrumentedType) {
                TypeDescription.Generic superClass = instrumentedType.getSuperClass();
                return (superClass == null
                        ? new MethodList.Empty<MethodDescription.InGenericShape>()
                        : superClass.getDeclaredMethods().filter(isConstructor().and(isVisibleTo(instrumentedType)))).asTokenList(is(instrumentedType));
            }

            @Override
            public MethodRegistry doInject(MethodRegistry methodRegistry, MethodAttributeAppender.Factory methodAttributeAppenderFactory) {
                return methodRegistry.append(new LatentMatcher.Resolved<MethodDescription>(isConstructor()),
                        new MethodRegistry.Handler.ForImplementation(SuperMethodCall.INSTANCE),
                        methodAttributeAppenderFactory,
                        Transformer.NoOp.<MethodDescription>make());
            }

            @Override
            protected int resolveModifier(int modifiers) {
                return Opcodes.ACC_PUBLIC;
            }
        };

        @Override
        public List<MethodDescription.Token> extractConstructors(TypeDescription instrumentedType) {
            List<MethodDescription.Token> tokens = doExtractConstructors(instrumentedType), stripped = new ArrayList<MethodDescription.Token>(tokens.size());
            for (MethodDescription.Token token : tokens) {
                stripped.add(new MethodDescription.Token(token.getName(),
                        resolveModifier(token.getModifiers()),
                        token.getTypeVariableTokens(),
                        token.getReturnType(),
                        token.getParameterTokens(),
                        token.getExceptionTypes(),
                        token.getAnnotations(),
                        token.getDefaultValue(),
                        TypeDescription.Generic.UNDEFINED));
            }
            return stripped;
        }

        /**
         * Resolves a constructor's modifiers.
         *
         * @param modifiers The actual constructor's modifiers.
         * @return The resolved modifiers.
         */
        protected int resolveModifier(int modifiers) {
            return modifiers;
        }

        /**
         * Extracts the relevant method tokens of the instrumented type's constructors. 提取插桩类型的构造函数的相关方法标记
         *
         * @param instrumentedType The type for which to extract the constructors.
         * @return A list of relevant method tokens.
         */
        protected abstract List<MethodDescription.Token> doExtractConstructors(TypeDescription instrumentedType);

        @Override
        public MethodRegistry inject(TypeDescription instrumentedType, MethodRegistry methodRegistry) {
            return doInject(methodRegistry, MethodAttributeAppender.NoOp.INSTANCE);
        }

        /**
         * Applies the actual injection with a method attribute appender factory supplied.
         *
         * @param methodRegistry                 The method registry into which to inject the constructors.
         * @param methodAttributeAppenderFactory The method attribute appender to use.
         * @return The resulting method registry.
         */
        protected abstract MethodRegistry doInject(MethodRegistry methodRegistry, MethodAttributeAppender.Factory methodAttributeAppenderFactory);

        /**
         * Returns a constructor strategy that supplies the supplied method attribute appender factory.
         *
         * @param methodAttributeAppenderFactory The method attribute appender factory to use.
         * @return A copy of this constructor strategy with the method attribute appender factory applied.
         */
        public ConstructorStrategy with(MethodAttributeAppender.Factory methodAttributeAppenderFactory) {
            return new WithMethodAttributeAppenderFactory(this, methodAttributeAppenderFactory);
        }

        /**
         * Applies this constructor strategy while retaining any of the base constructor's annotations.
         *
         * @return A copy of this constructor strategy which retains any of the base constructor's annotations.
         */
        public ConstructorStrategy withInheritedAnnotations() {
            return new WithMethodAttributeAppenderFactory(this, MethodAttributeAppender.ForInstrumentedMethod.EXCLUDING_RECEIVER);
        }

        /**
         * A wrapper for a default constructor strategy which additionally applies a method attribute appender factory.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class WithMethodAttributeAppenderFactory implements ConstructorStrategy {

            /**
             * The delegate default constructor strategy.
             */
            private final Default delegate;

            /**
             * The method attribute appender factory to apply.
             */
            private final MethodAttributeAppender.Factory methodAttributeAppenderFactory;

            /**
             * Creates a new wrapper for a default constructor strategy.
             *
             * @param delegate                       The delegate default constructor strategy.
             * @param methodAttributeAppenderFactory The method attribute appender factory to apply.
             */
            protected WithMethodAttributeAppenderFactory(Default delegate, MethodAttributeAppender.Factory methodAttributeAppenderFactory) {
                this.delegate = delegate;
                this.methodAttributeAppenderFactory = methodAttributeAppenderFactory;
            }

            @Override
            public List<MethodDescription.Token> extractConstructors(TypeDescription instrumentedType) {
                return delegate.extractConstructors(instrumentedType);
            }

            @Override
            public MethodRegistry inject(TypeDescription instrumentedType, MethodRegistry methodRegistry) {
                return delegate.doInject(methodRegistry, methodAttributeAppenderFactory);
            }
        }
    }

    /**
     * A constructor strategy that creates a default constructor that invokes a super constructor with default arguments.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForDefaultConstructor implements ConstructorStrategy {

        /**
         * A matcher to select a super constructor among possible candidates.
         */
        private final ElementMatcher<? super MethodDescription> elementMatcher;

        /**
         * The method attribute appender factory to apply.
         */
        private final MethodAttributeAppender.Factory methodAttributeAppenderFactory;

        /**
         * Creates a constructor strategy for invoking a super constructor with default arguments.
         */
        public ForDefaultConstructor() {
            this(any());
        }

        /**
         * Creates a constructor strategy for invoking a super constructor with default arguments.
         *
         * @param elementMatcher A matcher to select a super constructor among possible candidates.
         */
        public ForDefaultConstructor(ElementMatcher<? super MethodDescription> elementMatcher) {
            this(elementMatcher, MethodAttributeAppender.NoOp.INSTANCE);
        }

        /**
         * Creates a constructor strategy for invoking a super constructor with default arguments.
         *
         * @param methodAttributeAppenderFactory The method attribute appender factory to apply.
         */
        public ForDefaultConstructor(MethodAttributeAppender.Factory methodAttributeAppenderFactory) {
            this(any(), methodAttributeAppenderFactory);
        }

        /**
         * Creates a constructor strategy for invoking a super constructor with default arguments.
         *
         * @param elementMatcher                 A matcher to select a super constructor among possible candidates.
         * @param methodAttributeAppenderFactory The method attribute appender factory to apply.
         */
        public ForDefaultConstructor(ElementMatcher<? super MethodDescription> elementMatcher,
                                     MethodAttributeAppender.Factory methodAttributeAppenderFactory) {
            this.elementMatcher = elementMatcher;
            this.methodAttributeAppenderFactory = methodAttributeAppenderFactory;
        }

        @Override
        public List<MethodDescription.Token> extractConstructors(TypeDescription instrumentedType) {
            if (instrumentedType.getSuperClass().getDeclaredMethods().filter(isConstructor()).isEmpty()) {
                throw new IllegalStateException("Cannot define default constructor for class without super class constructor");
            }
            return Collections.singletonList(new MethodDescription.Token(Opcodes.ACC_PUBLIC));
        }

        @Override
        public MethodRegistry inject(TypeDescription instrumentedType, MethodRegistry methodRegistry) {
            MethodList<?> candidates = instrumentedType.getSuperClass().getDeclaredMethods().filter(isConstructor().and(elementMatcher));
            if (candidates.isEmpty()) {
                throw new IllegalStateException("No possible candidate for super constructor invocation in " + instrumentedType.getSuperClass());
            } else if (!candidates.filter(takesArguments(0)).isEmpty()) {
                candidates = candidates.filter(takesArguments(0));
            } else if (candidates.size() > 1) {
                throw new IllegalStateException("More than one possible super constructor for constructor delegation: " + candidates);
            }
            MethodCall methodCall = MethodCall.invoke(candidates.getOnly());
            for (TypeDescription typeDescription : candidates.getOnly().getParameters().asTypeList().asErasures()) {
                methodCall = methodCall.with(typeDescription.getDefaultValue());
            }
            return methodRegistry.append(new LatentMatcher.Resolved<MethodDescription>(isConstructor().and(takesArguments(0))),
                    new MethodRegistry.Handler.ForImplementation(methodCall),
                    methodAttributeAppenderFactory,
                    Transformer.NoOp.<MethodDescription>make());
        }
    }
}
