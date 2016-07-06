package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.Transformer;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.matcher.LatentMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

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
     * @param methodRegistry The original method registry.
     * @return A method registry that is capable of providing byte code for the constructors that were added by this strategy.
     */
    MethodRegistry inject(MethodRegistry methodRegistry);

    /**
     * Default implementations of constructor strategies.
     */
    enum Default implements ConstructorStrategy {

        /**
         * This strategy is adding no constructors such that the instrumented type will by default not have any. This
         * is legal by Java byte code requirements. However, if no constructor is added manually if this strategy is
         * applied, the type is not constructable without using JVM non-public functionality.
         */
        NO_CONSTRUCTORS {
            @Override
            protected List<MethodDescription.Token> doExtractConstructors(TypeDescription superClass) {
                return Collections.emptyList();
            }

            @Override
            public MethodRegistry inject(MethodRegistry methodRegistry) {
                return methodRegistry;
            }
        },

        /**
         * This strategy is adding a default constructor that calls it's super types default constructor. If no such
         * constructor is defined, an {@link IllegalArgumentException} is thrown. Note that the default constructor
         * needs to be visible to its sub type for this strategy to work.
         */
        DEFAULT_CONSTRUCTOR {
            @Override
            protected List<MethodDescription.Token> doExtractConstructors(TypeDescription instrumentedType) {
                TypeDescription.Generic superClass = instrumentedType.getSuperClass();
                MethodList<?> defaultConstructors = superClass == null
                        ? new MethodList.Empty<MethodDescription.InGenericShape>()
                        : superClass.getDeclaredMethods().filter(isConstructor().and(takesArguments(0)).<MethodDescription>and(isVisibleTo(instrumentedType)));
                if (defaultConstructors.size() == 1) {
                    return defaultConstructors.asTokenList(is(instrumentedType));
                } else {
                    throw new IllegalArgumentException(instrumentedType.getSuperClass() + " declares no constructor that is visible to " + instrumentedType);
                }
            }

            @Override
            public MethodRegistry inject(MethodRegistry methodRegistry) {
                return methodRegistry.append(new LatentMatcher.Resolved<MethodDescription>(isConstructor()),
                        new MethodRegistry.Handler.ForImplementation(SuperMethodCall.INSTANCE),
                        MethodAttributeAppender.NoOp.INSTANCE,
                        Transformer.NoOp.<MethodDescription>make());
            }
        },

        /**
         * This strategy is adding all constructors of the instrumented type's super class where each constructor is
         * directly invoking its signature-equivalent super class constructor. Only constructors that are visible to the
         * instrumented type are added, i.e. package-private constructors are only added if the super type is defined
         * in the same package as the instrumented type and private constructors are always skipped.
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
            public MethodRegistry inject(MethodRegistry methodRegistry) {
                return methodRegistry.append(new LatentMatcher.Resolved<MethodDescription>(isConstructor()),
                        new MethodRegistry.Handler.ForImplementation(SuperMethodCall.INSTANCE),
                        MethodAttributeAppender.ForInstrumentedMethod.EXCLUDING_RECEIVER,
                        Transformer.NoOp.<MethodDescription>make());
            }
        },

        /**
         * This strategy is adding all constructors of the instrumented type's super class where each constructor is
         * directly invoking its signature-equivalent super class constructor. Only {@code public} constructors are
         * added.
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
            public MethodRegistry inject(MethodRegistry methodRegistry) {
                return methodRegistry.append(new LatentMatcher.Resolved<MethodDescription>(isConstructor()),
                        new MethodRegistry.Handler.ForImplementation(SuperMethodCall.INSTANCE),
                        MethodAttributeAppender.ForInstrumentedMethod.EXCLUDING_RECEIVER,
                        Transformer.NoOp.<MethodDescription>make());
            }
        };

        @Override
        public List<MethodDescription.Token> extractConstructors(TypeDescription instrumentedType) {
            List<MethodDescription.Token> tokens = doExtractConstructors(instrumentedType), stripped = new ArrayList<MethodDescription.Token>(tokens.size());
            for (MethodDescription.Token token : tokens) {
                stripped.add(new MethodDescription.Token(token.getName(),
                        token.getModifiers(),
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
         * Extracts the relevant method tokens of the instrumented type's constructors.
         *
         * @param instrumentedType The type for which to extract the constructors.
         * @return A list of relevant method tokens.
         */
        protected abstract List<MethodDescription.Token> doExtractConstructors(TypeDescription instrumentedType);

        @Override
        public String toString() {
            return "ConstructorStrategy.Default." + name();
        }

    }
}
