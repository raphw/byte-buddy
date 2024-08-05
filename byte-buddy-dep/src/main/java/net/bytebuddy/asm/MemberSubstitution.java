/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.asm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.TypeVariableToken;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.scaffold.FieldLocator;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.*;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.collection.ArrayFactory;
import net.bytebuddy.implementation.bytecode.constant.*;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.*;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.visitor.LocalVariableAwareMethodVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * <p>
 * Substitutes field access, method invocations or constructor calls within a method's body.
 * </p>
 * <p>
 * <b>Note</b>: This substitution must not be used to match constructor calls to an instrumented class's super constructor invocation from
 * within a constructor. Matching such constructors will result in an invalid stack and a verification error.
 * </p>
 * <p>
 * <b>Note</b>: This visitor will compute the required stack size on a best effort basis. For allocating an optimal stack size, ASM needs
 * to be configured to compute the stack size.
 * </p>
 * <p>
 * <b>Important</b>: This component relies on using a {@link TypePool} for locating types within method bodies. Within a redefinition
 * or a rebasement, this type pool normally resolved correctly by Byte Buddy. When subclassing a type, the type pool must be set
 * explicitly, using {@link net.bytebuddy.dynamic.DynamicType.Builder#make(TypePool)} or any similar method. It is however not normally
 * necessary to use this component when subclassing a type where methods are only defined explicitly.
 * </p>
 */
@HashCodeAndEqualsPlugin.Enhance
public class MemberSubstitution implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper {

    /**
     * The index of the this reference within a non-static method.
     */
    protected static final int THIS_REFERENCE = 0;

    /**
     * The method graph compiler to use.
     */
    private final MethodGraph.Compiler methodGraphCompiler;

    /**
     * The type pool resolver to use.
     */
    private final TypePoolResolver typePoolResolver;

    /**
     * {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
     */
    private final boolean strict;

    /**
     * {@code true} if the instrumentation should fail if applied to a method without match.
     */
    private final boolean failIfNoMatch;

    /**
     * The replacement factory to use.
     */
    private final Replacement.Factory replacementFactory;

    /**
     * Creates a default member substitution.
     *
     * @param strict {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
     */
    protected MemberSubstitution(boolean strict) {
        this(MethodGraph.Compiler.DEFAULT, TypePoolResolver.OfImplicitPool.INSTANCE, strict, false, Replacement.NoOp.INSTANCE);
    }

    /**
     * Creates a new member substitution.
     *
     * @param methodGraphCompiler The method graph compiler to use.
     * @param typePoolResolver    The type pool resolver to use.
     * @param strict              {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
     * @param failIfNoMatch       {@code true} if the instrumentation should fail if applied to a method without match.
     * @param replacementFactory  The replacement factory to use.
     */
    protected MemberSubstitution(MethodGraph.Compiler methodGraphCompiler,
                                 TypePoolResolver typePoolResolver,
                                 boolean strict,
                                 boolean failIfNoMatch,
                                 Replacement.Factory replacementFactory) {
        this.methodGraphCompiler = methodGraphCompiler;
        this.typePoolResolver = typePoolResolver;
        this.failIfNoMatch = failIfNoMatch;
        this.strict = strict;
        this.replacementFactory = replacementFactory;
    }

    /**
     * Creates a member substitution that requires the resolution of all fields and methods that are referenced within a method body. Doing so,
     * this component raises an exception if any member cannot be resolved what makes this component unusable when facing optional types.
     *
     * @return A strict member substitution.
     */
    public static MemberSubstitution strict() {
        return new MemberSubstitution(true);
    }

    /**
     * Creates a member substitution that skips any unresolvable fields or methods that are referenced within a method body. Using a relaxed
     * member substitution, methods containing optional types are supported. In the process, it is however possible that misconfigurations
     * of this component remain undiscovered.
     *
     * @return A relaxed member substitution.
     */
    public static MemberSubstitution relaxed() {
        return new MemberSubstitution(false);
    }

    /**
     * Substitutes any interaction with a field or method that matches the given matcher.
     *
     * @param matcher The matcher to determine what access to byte code elements to substitute.
     * @return A specification that allows to determine how to substitute any interaction with byte code elements that match the supplied matcher.
     */
    public WithoutSpecification element(ElementMatcher<? super ByteCodeElement.Member> matcher) {
        return new WithoutSpecification.ForMatchedByteCodeElement(methodGraphCompiler, typePoolResolver, strict, failIfNoMatch, replacementFactory, matcher);
    }

    /**
     * Substitutes any field access that matches the given matcher.
     *
     * @param matcher The matcher to determine what fields to substitute.
     * @return A specification that allows to determine how to substitute any field access that match the supplied matcher.
     */
    public WithoutSpecification.ForMatchedField field(ElementMatcher<? super FieldDescription> matcher) {
        return new WithoutSpecification.ForMatchedField(methodGraphCompiler, typePoolResolver, strict, failIfNoMatch, replacementFactory, matcher);
    }

    /**
     * Substitutes any method invocation that matches the given matcher.
     *
     * @param matcher The matcher to determine what methods to substitute.
     * @return A specification that allows to determine how to substitute any method invocations that match the supplied matcher.
     */
    public WithoutSpecification.ForMatchedMethod method(ElementMatcher<? super MethodDescription> matcher) {
        return new WithoutSpecification.ForMatchedMethod(methodGraphCompiler, typePoolResolver, strict, failIfNoMatch, replacementFactory, matcher);
    }

    /**
     * Substitutes any constructor invocation that matches the given matcher.
     *
     * @param matcher The matcher to determine what constructors to substitute.
     * @return A specification that allows to determine how to substitute any constructor invocations that match the supplied matcher.
     */
    public WithoutSpecification constructor(ElementMatcher<? super MethodDescription> matcher) {
        return invokable(isConstructor().and(matcher));
    }

    /**
     * Substitutes any method or constructor invocation that matches the given matcher.
     *
     * @param matcher The matcher to determine what method or constructors to substitute.
     * @return A specification that allows to determine how to substitute any constructor invocations that match the supplied matcher.
     */
    public WithoutSpecification invokable(ElementMatcher<? super MethodDescription> matcher) {
        return new WithoutSpecification.ForMatchedMethod(methodGraphCompiler, typePoolResolver, strict, failIfNoMatch, replacementFactory, matcher);
    }

    /**
     * Specifies the use of a specific method graph compiler for the resolution of virtual methods.
     *
     * @param methodGraphCompiler The method graph compiler to use.
     * @return A new member substitution that is equal to this but uses the specified method graph compiler.
     */
    public MemberSubstitution with(MethodGraph.Compiler methodGraphCompiler) {
        return new MemberSubstitution(methodGraphCompiler, typePoolResolver, strict, failIfNoMatch, replacementFactory);
    }

    /**
     * Specifies a type pool resolver to be used for locating members.
     *
     * @param typePoolResolver The type pool resolver to use.
     * @return A new instance of this member substitution that uses the supplied type pool resolver.
     */
    public MemberSubstitution with(TypePoolResolver typePoolResolver) {
        return new MemberSubstitution(methodGraphCompiler, typePoolResolver, strict, failIfNoMatch, replacementFactory);
    }

    /**
     * Specifies if this substitution should fail if applied on a method without a match.
     *
     * @param failIfNoMatch {@code true} if the instrumentation should fail if applied to a method without match.
     * @return A new instance of this member substitution that fails if applied on a method without a match.
     */
    public MemberSubstitution failIfNoMatch(boolean failIfNoMatch) {
        return new MemberSubstitution(methodGraphCompiler, typePoolResolver, strict, failIfNoMatch, replacementFactory);
    }

    /**
     * Applies this member substitution to any method that matches the supplied matcher.
     *
     * @param matcher The matcher to determine this substitutions application.
     * @return An ASM visitor wrapper that applies all specified substitutions for any matched method.
     */
    public AsmVisitorWrapper.ForDeclaredMethods on(ElementMatcher<? super MethodDescription> matcher) {
        return new AsmVisitorWrapper.ForDeclaredMethods().invokable(matcher, this);
    }

    /**
     * {@inheritDoc}
     */
    public MethodVisitor wrap(TypeDescription instrumentedType,
                              MethodDescription instrumentedMethod,
                              MethodVisitor methodVisitor,
                              Implementation.Context implementationContext,
                              TypePool typePool,
                              int writerFlags,
                              int readerFlags) {
        typePool = typePoolResolver.resolve(instrumentedType, instrumentedMethod, typePool);
        return new SubstitutingMethodVisitor(methodVisitor,
                instrumentedType,
                instrumentedMethod,
                methodGraphCompiler,
                strict,
                failIfNoMatch,
                replacementFactory.make(instrumentedType, instrumentedMethod, typePool),
                implementationContext,
                typePool,
                implementationContext.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V11));
    }

    /**
     * A member substitution that lacks a specification for how to substitute the matched members references within a method body.
     */
    @HashCodeAndEqualsPlugin.Enhance
    public abstract static class WithoutSpecification {

        /**
         * The method graph compiler to use.
         */
        protected final MethodGraph.Compiler methodGraphCompiler;

        /**
         * The type pool resolver to use.
         */
        protected final TypePoolResolver typePoolResolver;

        /**
         * {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
         */
        protected final boolean strict;

        /**
         * {@code true} if the instrumentation should fail if applied to a method without match.
         */
        protected final boolean failIfNoMatch;

        /**
         * The replacement factory to use for creating substitutions.
         */
        protected final Replacement.Factory replacementFactory;

        /**
         * Creates a new member substitution that requires a specification for how to perform a substitution.
         *
         * @param methodGraphCompiler The method graph compiler to use.
         * @param typePoolResolver    The type pool resolver to use.
         * @param strict              {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
         * @param failIfNoMatch       {@code true} if the instrumentation should fail if applied to a method without match.
         * @param replacementFactory  The replacement factory to use for creating substitutions.
         */
        protected WithoutSpecification(MethodGraph.Compiler methodGraphCompiler,
                                       TypePoolResolver typePoolResolver,
                                       boolean strict,
                                       boolean failIfNoMatch,
                                       Replacement.Factory replacementFactory) {
            this.methodGraphCompiler = methodGraphCompiler;
            this.typePoolResolver = typePoolResolver;
            this.strict = strict;
            this.failIfNoMatch = failIfNoMatch;
            this.replacementFactory = replacementFactory;
        }

        /**
         * Subs any interaction with a matched byte code element. Any value read from the element will be replaced with the stubbed
         * value's default, i.e. {@code null} for reference types and the specific {@code 0} value for primitive types. Any written
         * value will simply be discarded.
         *
         * @return A member substitution that stubs any interaction with a matched byte code element.
         */
        public MemberSubstitution stub() {
            return replaceWith(Substitution.Stubbing.INSTANCE);
        }

        /**
         * Replaces any interaction with a matched byte code element with the provided compile-time constant.
         *
         * @param value The compile-time constant to set.
         * @return A member substitution that replaces any interaction with the supplied compile-time constant.
         */
        public MemberSubstitution replaceWithConstant(Object value) {
            ConstantValue constant = ConstantValue.Simple.wrap(value);
            return replaceWith(new Substitution.ForValue(constant.toStackManipulation(), constant.getTypeDescription().asGenericType()));
        }

        /**
         * <p>
         * Replaces any interaction with a matched byte code element by an interaction with the specified field. If a field
         * is replacing a method or constructor invocation, it is treated as if it was a field getter or setter respectively.
         * </p>
         * <p>
         * A replacement can only be applied if the field is compatible to the original byte code element, i.e. consumes an
         * instance of the declaring type if it is not {@code static} as an argument and consumes or produces an instance of
         * the field's type.
         * </p>
         *
         * @param field The field to access instead of interacting with any of the matched byte code elements.
         * @return A member substitution that replaces any matched byte code element with an access of the specified field.
         */
        public MemberSubstitution replaceWith(Field field) {
            return replaceWith(new FieldDescription.ForLoadedField(field));
        }

        /**
         * <p>
         * Replaces any interaction with a matched byte code element by an interaction with the specified field. If a field
         * is replacing a method or constructor invocation, it is treated as if it was a field getter or setter respectively.
         * </p>
         * <p>
         * A replacement can only be applied if the field is compatible to the original byte code element, i.e. consumes an
         * instance of the declaring type if it is not {@code static} as an argument and consumes or produces an instance of
         * the field's type.
         * </p>
         *
         * @param fieldDescription The field to access instead of interacting with any of the matched byte code elements.
         * @return A member substitution that replaces any matched byte code element with an access of the specified field.
         */
        public MemberSubstitution replaceWith(FieldDescription fieldDescription) {
            return replaceWith(new Substitution.ForFieldAccess.OfGivenField(fieldDescription));
        }

        /**
         * Replaces any interaction with a matched byte code element with a non-static field access on the first
         * parameter of the matched element. When matching a non-static field access or method invocation, the
         * substituted field is located on the same receiver type as the original access. For static access, the
         * first argument is used as a receiver.
         *
         * @param matcher A matcher for locating a field on the original interaction's receiver type.
         * @return A member substitution that replaces any matched byte code element with an access of the matched field.
         */
        public MemberSubstitution replaceWithField(ElementMatcher<? super FieldDescription> matcher) {
            return replaceWith(new Substitution.ForFieldAccess.OfMatchedField(matcher));
        }

        /**
         * <p>
         * Replaces any interaction with a matched byte code element by an invocation of the specified method. If a method
         * is replacing a field access, it is treated as if it was replacing an invocation of the field's getter or setter respectively.
         * </p>
         * <p>
         * A replacement can only be applied if the method is compatible to the original byte code element, i.e. consumes compatible
         * arguments and returns a compatible value. If the method is not {@code static}, it is treated as if {@code this} was an implicit
         * first argument.
         * </p>
         *
         * @param method The method to invoke instead of interacting with any of the matched byte code elements.
         * @return A member substitution that replaces any matched byte code element with an invocation of the specified method.
         */
        public MemberSubstitution replaceWith(Method method) {
            return replaceWith(new MethodDescription.ForLoadedMethod(method));
        }

        /**
         * <p>
         * Replaces any interaction with a matched byte code element by an invocation of the specified method. If a method
         * is replacing a field access, it is treated as if it was replacing an invocation of the field's getter or setter respectively.
         * </p>
         * <p>
         * A replacement can only be applied if the method is compatible to the original byte code element, i.e. consumes compatible
         * arguments and returns a compatible value. If the method is not {@code static}, it is treated as if {@code this} was an implicit
         * first argument.
         * </p>
         * <p>
         * <b>Important</b>: It is not allowed to specify a constructor or the static type initializer as a replacement.
         * </p>
         *
         * @param methodDescription The method to invoke instead of interacting with any of the matched byte code elements.
         * @return A member substitution that replaces any matched byte code element with an invocation of the specified method.
         */
        public MemberSubstitution replaceWith(MethodDescription methodDescription) {
            if (!methodDescription.isMethod()) {
                throw new IllegalArgumentException("Cannot use " + methodDescription + " as a replacement");
            }
            return replaceWith(new Substitution.ForMethodInvocation.OfGivenMethod(methodDescription));
        }

        /**
         * Replaces any interaction with a matched byte code element with a non-static method access on the first
         * parameter of the matched element. When matching a non-static field access or method invocation, the
         * substituted method is located on the same receiver type as the original access. For static access, the
         * first argument is used as a receiver.
         *
         * @param matcher A matcher for locating a method on the original interaction's receiver type.
         * @return A member substitution that replaces any matched byte code element with an access of the matched method.
         */
        public MemberSubstitution replaceWithMethod(ElementMatcher<? super MethodDescription> matcher) {
            return replaceWithMethod(matcher, methodGraphCompiler);
        }

        /**
         * Replaces any interaction with a matched byte code element with a non-static method access on the first
         * parameter of the matched element. When matching a non-static field access or method invocation, the
         * substituted method is located on the same receiver type as the original access. For static access, the
         * first argument is used as a receiver.
         *
         * @param matcher             A matcher for locating a method on the original interaction's receiver type.
         * @param methodGraphCompiler The method graph compiler to use for locating a method.
         * @return A member substitution that replaces any matched byte code element with an access of the matched method.
         */
        public MemberSubstitution replaceWithMethod(ElementMatcher<? super MethodDescription> matcher, MethodGraph.Compiler methodGraphCompiler) {
            return replaceWith(new Substitution.ForMethodInvocation.OfMatchedMethod(matcher, methodGraphCompiler));
        }

        /**
         * Replaces any interaction with a matched byte code element with an invocation of the instrumented
         * method. This can cause an infinite recursive call if the arguments to the method are not altered.
         *
         * @return A member substitution that replaces any matched byte code element with an invocation of the
         * instrumented method.
         */
        public MemberSubstitution replaceWithInstrumentedMethod() {
            return replaceWith(Substitution.ForMethodInvocation.OfInstrumentedMethod.INSTANCE);
        }

        /**
         * Replaces the matched byte code elements with a chain of substitutions that can operate on the same values as the substituted element. This is a
         * shortcut for creating a substitution chain with a default assigner.
         *
         * @param step The steps to apply for a substitution.
         * @return A member substitution that replaces any matched byte code element with the provided substitution chain.
         */
        public MemberSubstitution replaceWithChain(Substitution.Chain.Step.Factory... step) {
            return replaceWithChain(Arrays.asList(step));
        }

        /**
         * Replaces the matched byte code elements with a chain of substitutions that can operate on the same values as the substituted element. This is a
         * shortcut for creating a substitution chain with a default assigner.
         *
         * @param steps The steps to apply for a substitution.
         * @return A member substitution that replaces any matched byte code element with the provided substitution chain.
         */
        public MemberSubstitution replaceWithChain(List<? extends Substitution.Chain.Step.Factory> steps) {
            return replaceWith(Substitution.Chain.withDefaultAssigner().executing(steps));
        }

        /**
         * Replaces any interaction with the supplied substitution.
         *
         * @param factory The substitution factory to use for creating the applied substitution.
         * @return A member substitution that replaces any matched byte code element with the supplied substitution.
         */
        public abstract MemberSubstitution replaceWith(Substitution.Factory factory);

        /**
         * Describes a member substitution that requires a specification for how to replace a byte code element.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class ForMatchedByteCodeElement extends WithoutSpecification {

            /**
             * A matcher for any byte code elements that should be substituted.
             */
            private final ElementMatcher<? super ByteCodeElement.Member> matcher;

            /**
             * Creates a new member substitution for a matched byte code element that requires a specification for how to perform a substitution.
             *
             * @param methodGraphCompiler The method graph compiler to use.
             * @param typePoolResolver    The type pool resolver to use.
             * @param strict              {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
             * @param failIfNoMatch       {@code true} if the instrumentation should fail if applied to a method without match.
             * @param replacementFactory  The replacement factory to use.
             * @param matcher             A matcher for any byte code elements that should be substituted.
             */
            protected ForMatchedByteCodeElement(MethodGraph.Compiler methodGraphCompiler,
                                                TypePoolResolver typePoolResolver,
                                                boolean strict,
                                                boolean failIfNoMatch,
                                                Replacement.Factory replacementFactory,
                                                ElementMatcher<? super ByteCodeElement.Member> matcher) {
                super(methodGraphCompiler, typePoolResolver, strict, failIfNoMatch, replacementFactory);
                this.matcher = matcher;
            }

            /**
             * {@inheritDoc}
             */
            public MemberSubstitution replaceWith(Substitution.Factory substitutionFactory) {
                return new MemberSubstitution(methodGraphCompiler,
                        typePoolResolver,
                        strict,
                        failIfNoMatch,
                        new Replacement.Factory.Compound(this.replacementFactory, Replacement.ForElementMatchers.Factory.of(matcher, substitutionFactory)));
            }
        }

        /**
         * Describes a member substitution that requires a specification for how to replace a field.
         */
        @HashCodeAndEqualsPlugin.Enhance
        public static class ForMatchedField extends WithoutSpecification {

            /**
             * A matcher for any field that should be substituted.
             */
            private final ElementMatcher<? super FieldDescription> matcher;

            /**
             * {@code true} if read access to a field should be substituted.
             */
            private final boolean matchRead;

            /**
             * {@code true} if write access to a field should be substituted.
             */
            private final boolean matchWrite;

            /**
             * Creates a new member substitution for a matched field that requires a specification for how to perform a substitution.
             *
             * @param methodGraphCompiler The method graph compiler to use.
             * @param typePoolResolver    The type pool resolver to use.
             * @param strict              {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
             * @param failIfNoMatch       {@code true} if the instrumentation should fail if applied to a method without match.
             * @param replacementFactory  The replacement factory to use.
             * @param matcher             A matcher for any field that should be substituted.
             */
            protected ForMatchedField(MethodGraph.Compiler methodGraphCompiler,
                                      TypePoolResolver typePoolResolver,
                                      boolean strict,
                                      boolean failIfNoMatch,
                                      Replacement.Factory replacementFactory,
                                      ElementMatcher<? super FieldDescription> matcher) {
                this(methodGraphCompiler, typePoolResolver, strict, failIfNoMatch, replacementFactory, matcher, true, true);
            }

            /**
             * Creates a new member substitution for a matched field that requires a specification for how to perform a substitution.
             *
             * @param methodGraphCompiler The method graph compiler to use.
             * @param typePoolResolver    The type pool resolver to use.
             * @param strict              {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
             * @param failIfNoMatch       {@code true} if the instrumentation should fail if applied to a method without match.
             * @param replacementFactory  The replacement factory to use.
             * @param matcher             A matcher for any field that should be substituted.
             * @param matchRead           {@code true} if read access to a field should be substituted.
             * @param matchWrite          {@code true} if write access to a field should be substituted.
             */
            protected ForMatchedField(MethodGraph.Compiler methodGraphCompiler,
                                      TypePoolResolver typePoolResolver,
                                      boolean strict,
                                      boolean failIfNoMatch,
                                      Replacement.Factory replacementFactory,
                                      ElementMatcher<? super FieldDescription> matcher,
                                      boolean matchRead,
                                      boolean matchWrite) {
                super(methodGraphCompiler, typePoolResolver, strict, failIfNoMatch, replacementFactory);
                this.matcher = matcher;
                this.matchRead = matchRead;
                this.matchWrite = matchWrite;
            }

            /**
             * When invoked, only read access of the previously matched field is substituted.
             *
             * @return This instance with the limitation that only read access to the matched field is substituted.
             */
            public WithoutSpecification onRead() {
                return new ForMatchedField(methodGraphCompiler, typePoolResolver, strict, failIfNoMatch, replacementFactory, matcher, true, false);
            }

            /**
             * When invoked, only write access of the previously matched field is substituted.
             *
             * @return This instance with the limitation that only write access to the matched field is substituted.
             */
            public WithoutSpecification onWrite() {
                return new ForMatchedField(methodGraphCompiler, typePoolResolver, strict, failIfNoMatch, replacementFactory, matcher, false, true);
            }

            /**
             * {@inheritDoc}
             */
            public MemberSubstitution replaceWith(Substitution.Factory substitutionFactory) {
                return new MemberSubstitution(methodGraphCompiler,
                        typePoolResolver,
                        strict,
                        failIfNoMatch,
                        new Replacement.Factory.Compound(this.replacementFactory, Replacement.ForElementMatchers.Factory.ofField(matcher, matchRead, matchWrite, substitutionFactory)));
            }
        }

        /**
         * Describes a member substitution that requires a specification for how to replace a method or constructor.
         */
        @HashCodeAndEqualsPlugin.Enhance
        public static class ForMatchedMethod extends WithoutSpecification {

            /**
             * A matcher for any method or constructor that should be substituted.
             */
            private final ElementMatcher<? super MethodDescription> matcher;

            /**
             * {@code true} if this specification includes virtual invocations.
             */
            private final boolean includeVirtualCalls;

            /**
             * {@code true} if this specification includes {@code super} invocations.
             */
            private final boolean includeSuperCalls;

            /**
             * Creates a new member substitution for a matched method that requires a specification for how to perform a substitution.
             *
             * @param methodGraphCompiler The method graph compiler to use.
             * @param typePoolResolver    The type pool resolver to use.
             * @param strict              {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
             * @param failIfNoMatch       {@code true} if the instrumentation should fail if applied to a method without match.
             * @param replacementFactory  The replacement factory to use.
             * @param matcher             A matcher for any method or constructor that should be substituted.
             */
            protected ForMatchedMethod(MethodGraph.Compiler methodGraphCompiler,
                                       TypePoolResolver typePoolResolver,
                                       boolean strict,
                                       boolean failIfNoMatch,
                                       Replacement.Factory replacementFactory,
                                       ElementMatcher<? super MethodDescription> matcher) {
                this(methodGraphCompiler, typePoolResolver, strict, failIfNoMatch, replacementFactory, matcher, true, true);
            }

            /**
             * Creates a new member substitution for a matched method that requires a specification for how to perform a substitution.
             *
             * @param methodGraphCompiler The method graph compiler to use.
             * @param typePoolResolver    The type pool resolver to use.
             * @param strict              {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
             * @param failIfNoMatch       {@code true} if the instrumentation should fail if applied to a method without match.
             * @param replacementFactory  The replacement factory to use.
             * @param matcher             A matcher for any method or constructor that should be substituted.
             * @param includeVirtualCalls {@code true} if this specification includes virtual invocations.
             * @param includeSuperCalls   {@code true} if this specification includes {@code super} invocations.
             */
            protected ForMatchedMethod(MethodGraph.Compiler methodGraphCompiler,
                                       TypePoolResolver typePoolResolver,
                                       boolean strict,
                                       boolean failIfNoMatch,
                                       Replacement.Factory replacementFactory,
                                       ElementMatcher<? super MethodDescription> matcher,
                                       boolean includeVirtualCalls,
                                       boolean includeSuperCalls) {
                super(methodGraphCompiler, typePoolResolver, strict, failIfNoMatch, replacementFactory);
                this.matcher = matcher;
                this.includeVirtualCalls = includeVirtualCalls;
                this.includeSuperCalls = includeSuperCalls;
            }

            /**
             * Limits the substituted method calls to method calls that invoke a method virtually (as opposed to a {@code super} invocation).
             *
             * @return This specification where only virtual methods are matched if they are not invoked as a virtual call.
             */
            public WithoutSpecification onVirtualCall() {
                return new ForMatchedMethod(methodGraphCompiler,
                        typePoolResolver,
                        strict,
                        failIfNoMatch,
                        replacementFactory,
                        isVirtual().and(matcher),
                        true,
                        false);
            }

            /**
             * Limits the substituted method calls to method calls that invoke a method as a {@code super} call.
             *
             * @return This specification where only virtual methods are matched if they are not invoked as a super call.
             */
            public WithoutSpecification onSuperCall() {
                return new ForMatchedMethod(methodGraphCompiler,
                        typePoolResolver,
                        strict,
                        failIfNoMatch,
                        replacementFactory,
                        isVirtual().and(matcher),
                        false,
                        true);
            }

            /**
             * {@inheritDoc}
             */
            public MemberSubstitution replaceWith(Substitution.Factory substitutionFactory) {
                return new MemberSubstitution(methodGraphCompiler,
                        typePoolResolver,
                        strict,
                        failIfNoMatch,
                        new Replacement.Factory.Compound(this.replacementFactory, Replacement.ForElementMatchers.Factory.ofMethod(matcher, includeVirtualCalls, includeSuperCalls, substitutionFactory)));
            }
        }
    }

    /**
     * A type pool resolver is responsible for resolving a {@link TypePool} for locating substituted members.
     */
    public interface TypePoolResolver {

        /**
         * Resolves a type pool to use for locating substituted members.
         *
         * @param instrumentedType   The instrumented type.
         * @param instrumentedMethod The instrumented method.
         * @param typePool           The type pool implicit to the instrumentation.
         * @return The type pool to use.
         */
        TypePool resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool);

        /**
         * Returns the implicit type pool.
         */
        enum OfImplicitPool implements TypePoolResolver {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public TypePool resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
                return typePool;
            }
        }

        /**
         * A type pool resolver that returns a specific type pool.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForExplicitPool implements TypePoolResolver {

            /**
             * The type pool to return.
             */
            private final TypePool typePool;

            /**
             * Creates a resolver for an explicit type pool.
             *
             * @param typePool The type pool to return.
             */
            public ForExplicitPool(TypePool typePool) {
                this.typePool = typePool;
            }

            /**
             * {@inheritDoc}
             */
            public TypePool resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
                return this.typePool;
            }
        }

        /**
         * A type pool resolver that resolves the implicit pool but additionally checks another class file locator.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForClassFileLocator implements TypePoolResolver {

            /**
             * The class file locator to use.
             */
            private final ClassFileLocator classFileLocator;

            /**
             * The reader mode to apply.
             */
            private final TypePool.Default.ReaderMode readerMode;

            /**
             * Creates a new type pool resolver for a class file locator as a supplement of the implicit type pool.
             *
             * @param classFileLocator The class file locator to use.
             */
            public ForClassFileLocator(ClassFileLocator classFileLocator) {
                this(classFileLocator, TypePool.Default.ReaderMode.FAST);
            }

            /**
             * Creates a new type pool resolver for a class file locator as a supplement of the implicit type pool.
             *
             * @param classFileLocator The class file locator to use.
             * @param readerMode       The reader mode to apply.
             */
            public ForClassFileLocator(ClassFileLocator classFileLocator, TypePool.Default.ReaderMode readerMode) {
                this.classFileLocator = classFileLocator;
                this.readerMode = readerMode;
            }

            /**
             * Creates a new type pool resolver that supplements the supplied class loader to the implicit type pool.
             *
             * @param classLoader The class loader to use as a supplement which can be {@code null} to represent the bootstrap loader.
             * @return An appropriate type pool resolver.
             */
            public static TypePoolResolver of(@MaybeNull ClassLoader classLoader) {
                return new ForClassFileLocator(ClassFileLocator.ForClassLoader.of(classLoader));
            }

            /**
             * {@inheritDoc}
             */
            public TypePool resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
                return new TypePool.Default(new TypePool.CacheProvider.Simple(), classFileLocator, readerMode, typePool);
            }
        }
    }

    /**
     * A substitution replaces or enhances an interaction with a field or method within an instrumented method.
     */
    public interface Substitution {

        /**
         * Resolves this substitution into a stack manipulation.
         *
         * @param receiver          The target type on which a member is accessed.
         * @param original          The field, method or constructor that is substituted.
         * @param parameters        All parameters that serve as input to this access.
         * @param result            The result that is expected from the interaction or {@code void} if no result is expected.
         * @param methodHandle      A method handle describing the substituted expression.
         * @param stackManipulation The original byte code expression that is being executed.
         * @param freeOffset        The first free offset of the local variable array that can be used for storing values.
         * @return A stack manipulation that represents the access.
         */
        StackManipulation resolve(TypeDescription receiver,
                                  ByteCodeElement.Member original,
                                  TypeList.Generic parameters,
                                  TypeDescription.Generic result,
                                  JavaConstant.MethodHandle methodHandle,
                                  StackManipulation stackManipulation,
                                  int freeOffset);

        /**
         * A factory for creating a substitution for an instrumented method.
         */
        interface Factory {

            /**
             * Creates a substitution for an instrumented method.
             *
             * @param instrumentedType   The instrumented type.
             * @param instrumentedMethod The instrumented method.
             * @param typePool           The type pool being used.
             * @return The substitution to apply within the instrumented method.
             */
            Substitution make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool);
        }

        /**
         * A substitution that drops any field or method access and returns the expected return
         * type's default value, i.e {@code null} or zero for primitive types.
         */
        enum Stubbing implements Substitution, Factory {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public Substitution make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
                return this;
            }

            /**
             * {@inheritDoc}
             */
            public StackManipulation resolve(TypeDescription receiver, ByteCodeElement.Member original, TypeList.Generic parameters, TypeDescription.Generic result, JavaConstant.MethodHandle methodHandle, StackManipulation stackManipulation, int freeOffset) {
                List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(parameters.size());
                for (int index = parameters.size() - 1; index >= 0; index--) {
                    stackManipulations.add(Removal.of(parameters.get(index)));
                }
                return new StackManipulation.Compound(CompoundList.of(stackManipulations, DefaultValue.of(result.asErasure())));
            }
        }

        /**
         * A substitution that loads a fixed value.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForValue implements Substitution, Factory {

            /**
             * The stack manipulation to load the value that represents the substitution.
             */
            private final StackManipulation stackManipulation;

            /**
             * The type of the represented stack manipulation.
             */
            private final TypeDescription.Generic typeDescription;

            /**
             * Creates a new substitution for loading a constant value.
             *
             * @param stackManipulation The stack manipulation to load the value that represents the substitution.
             * @param typeDescription   The type of the represented stack manipulation.
             */
            public ForValue(StackManipulation stackManipulation, TypeDescription.Generic typeDescription) {
                this.stackManipulation = stackManipulation;
                this.typeDescription = typeDescription;
            }

            /**
             * {@inheritDoc}
             */
            public Substitution make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
                return this;
            }

            /**
             * {@inheritDoc}
             */
            public StackManipulation resolve(TypeDescription receiver,
                                             ByteCodeElement.Member original,
                                             TypeList.Generic parameters,
                                             TypeDescription.Generic result,
                                             JavaConstant.MethodHandle methodHandle,
                                             StackManipulation stackManipulation,
                                             int freeOffset) {
                List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(parameters.size());
                for (int index = parameters.size() - 1; index >= 0; index--) {
                    stackManipulations.add(Removal.of(parameters.get(index)));
                }
                if (!typeDescription.asErasure().isAssignableTo(result.asErasure())) {
                    throw new IllegalStateException("Cannot assign " + typeDescription + " to " + result);
                }
                return new StackManipulation.Compound(CompoundList.of(stackManipulations, this.stackManipulation));
            }
        }

        /**
         * A substitution with a field access.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForFieldAccess implements Substitution {

            /**
             * The instrumented type.
             */
            private final TypeDescription instrumentedType;

            /**
             * A resolver to locate the field to access.
             */
            private final FieldResolver fieldResolver;

            /**
             * Creates a new substitution with a field access.
             *
             * @param instrumentedType The instrumented type.
             * @param fieldResolver    A resolver to locate the field to access.
             */
            public ForFieldAccess(TypeDescription instrumentedType, FieldResolver fieldResolver) {
                this.instrumentedType = instrumentedType;
                this.fieldResolver = fieldResolver;
            }

            /**
             * {@inheritDoc}
             */
            @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming declaring type for type member.")
            public StackManipulation resolve(TypeDescription receiver,
                                             ByteCodeElement.Member original,
                                             TypeList.Generic parameters,
                                             TypeDescription.Generic result,
                                             JavaConstant.MethodHandle methodHandle,
                                             StackManipulation stackManipulation,
                                             int freeOffset) {
                FieldDescription fieldDescription = fieldResolver.resolve(receiver, original, parameters, result);
                if (!fieldDescription.isAccessibleTo(instrumentedType)) {
                    throw new IllegalStateException(instrumentedType + " cannot access " + fieldDescription);
                } else if (result.represents(void.class)) {
                    if (parameters.size() != (fieldDescription.isStatic() ? 1 : 2)) {
                        throw new IllegalStateException("Cannot set " + fieldDescription + " with " + parameters);
                    } else if (!fieldDescription.isStatic() && !parameters.get(0).asErasure().isAssignableTo(fieldDescription.getDeclaringType().asErasure())) {
                        throw new IllegalStateException("Cannot set " + fieldDescription + " on " + parameters.get(0));
                    } else if (!parameters.get(fieldDescription.isStatic() ? 0 : 1).asErasure().isAssignableTo(fieldDescription.getType().asErasure())) {
                        throw new IllegalStateException("Cannot set " + fieldDescription + " to " + parameters.get(fieldDescription.isStatic() ? 0 : 1));
                    }
                    return FieldAccess.forField(fieldDescription).write();
                } else {
                    if (parameters.size() != (fieldDescription.isStatic() ? 0 : 1)) {
                        throw new IllegalStateException("Cannot set " + fieldDescription + " with " + parameters);
                    } else if (!fieldDescription.isStatic() && !parameters.get(0).asErasure().isAssignableTo(fieldDescription.getDeclaringType().asErasure())) {
                        throw new IllegalStateException("Cannot get " + fieldDescription + " on " + parameters.get(0));
                    } else if (!fieldDescription.getType().asErasure().isAssignableTo(result.asErasure())) {
                        throw new IllegalStateException("Cannot get " + fieldDescription + " as " + result);
                    }
                    return FieldAccess.forField(fieldDescription).read();
                }
            }

            /**
             * A method resolver for locating a field for a substitute.
             */
            public interface FieldResolver {

                /**
                 * Resolves the field to substitute with.
                 *
                 * @param receiver   The target type on which a member is accessed.
                 * @param original   The target field, method or constructor that is substituted,
                 * @param parameters All parameters that serve as input to this access.
                 * @param result     The result that is expected from the interaction or {@code void} if no result is expected.
                 * @return The field to substitute with.
                 */
                FieldDescription resolve(TypeDescription receiver, ByteCodeElement.Member original, TypeList.Generic parameters, TypeDescription.Generic result);

                /**
                 * A simple field resolver that returns a specific field.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class Simple implements FieldResolver {

                    /**
                     * The field to access.
                     */
                    private final FieldDescription fieldDescription;

                    /**
                     * Creates a simple field resolver.
                     *
                     * @param fieldDescription The field to access.
                     */
                    public Simple(FieldDescription fieldDescription) {
                        this.fieldDescription = fieldDescription;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public FieldDescription resolve(TypeDescription receiver, ByteCodeElement.Member original, TypeList.Generic parameters, TypeDescription.Generic result) {
                        return fieldDescription;
                    }
                }

                /**
                 * A field matcher that resolves a non-static field on the first parameter type of the substituted member usage.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForElementMatcher implements FieldResolver {

                    /**
                     * The instrumented type.
                     */
                    private final TypeDescription instrumentedType;

                    /**
                     * The matcher to use for locating the field to substitute with.
                     */
                    private final ElementMatcher<? super FieldDescription> matcher;

                    /**
                     * Creates a new field resolver that locates a field on the receiver type using a matcher.
                     *
                     * @param instrumentedType The instrumented type.
                     * @param matcher          The matcher to use for locating the field to substitute with.
                     */
                    protected ForElementMatcher(TypeDescription instrumentedType, ElementMatcher<? super FieldDescription> matcher) {
                        this.instrumentedType = instrumentedType;
                        this.matcher = matcher;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public FieldDescription resolve(TypeDescription receiver, ByteCodeElement.Member original, TypeList.Generic parameters, TypeDescription.Generic result) {
                        if (parameters.isEmpty()) {
                            throw new IllegalStateException("Cannot substitute parameterless instruction with " + original);
                        } else if (parameters.get(0).isPrimitive() || parameters.get(0).isArray()) {
                            throw new IllegalStateException("Cannot access field on primitive or array type for " + original);
                        }
                        TypeDefinition current = parameters.get(0).accept(new TypeDescription.Generic.Visitor.Substitutor.ForReplacement(instrumentedType));
                        do {
                            FieldList<?> fields = current.getDeclaredFields().filter(not(isStatic()).<FieldDescription>and(isVisibleTo(instrumentedType)).and(matcher));
                            if (fields.size() == 1) {
                                return fields.getOnly();
                            } else if (fields.size() > 1) {
                                throw new IllegalStateException("Ambiguous field location of " + fields);
                            }
                            current = current.getSuperClass();
                        } while (current != null);
                        throw new IllegalStateException("Cannot locate field matching " + matcher + " on " + receiver);
                    }
                }
            }

            /**
             * A factory for a substitution that substitutes with a given field.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public static class OfGivenField implements Factory {

                /**
                 * The field to substitute with.
                 */
                private final FieldDescription fieldDescription;

                /**
                 * Creates a new factory that substitues with a given field.
                 *
                 * @param fieldDescription The field to substitute with.
                 */
                public OfGivenField(FieldDescription fieldDescription) {
                    this.fieldDescription = fieldDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public Substitution make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
                    return new ForFieldAccess(instrumentedType, new FieldResolver.Simple(fieldDescription));
                }
            }

            /**
             * A factory for a substitution that locates a field on the receiver type using a matcher.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public static class OfMatchedField implements Factory {

                /**
                 * The matcher to apply.
                 */
                private final ElementMatcher<? super FieldDescription> matcher;

                /**
                 * Creates a new substitution factory that locates a field by applying a matcher on the receiver type.
                 *
                 * @param matcher The matcher to apply.
                 */
                public OfMatchedField(ElementMatcher<? super FieldDescription> matcher) {
                    this.matcher = matcher;
                }

                /**
                 * {@inheritDoc}
                 */
                public Substitution make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
                    return new ForFieldAccess(instrumentedType, new FieldResolver.ForElementMatcher(instrumentedType, matcher));
                }
            }
        }

        /**
         * A substitution with a method invocation.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForMethodInvocation implements Substitution {

            /**
             * The instrumented type.
             */
            private final TypeDescription instrumentedType;

            /**
             * The method resolver to use.
             */
            private final MethodResolver methodResolver;

            /**
             * Creates a new method-resolving substitution.
             *
             * @param instrumentedType The instrumented type.
             * @param methodResolver   The method resolver to use.
             */
            public ForMethodInvocation(TypeDescription instrumentedType, MethodResolver methodResolver) {
                this.instrumentedType = instrumentedType;
                this.methodResolver = methodResolver;
            }

            /**
             * {@inheritDoc}
             */
            public StackManipulation resolve(TypeDescription receiver,
                                             ByteCodeElement.Member original,
                                             TypeList.Generic parameters,
                                             TypeDescription.Generic result,
                                             JavaConstant.MethodHandle methodHandle,
                                             StackManipulation stackManipulation,
                                             int freeOffset) {
                MethodDescription methodDescription = methodResolver.resolve(receiver, original, parameters, result);
                if (!methodDescription.isAccessibleTo(instrumentedType)) {
                    throw new IllegalStateException(instrumentedType + " cannot access " + methodDescription);
                }
                TypeList.Generic mapped = methodDescription.isStatic()
                        ? methodDescription.getParameters().asTypeList()
                        : new TypeList.Generic.Explicit(CompoundList.of(methodDescription.getDeclaringType(), methodDescription.getParameters().asTypeList()));
                if (!methodDescription.getReturnType().asErasure().isAssignableTo(result.asErasure())) {
                    throw new IllegalStateException("Cannot assign return value of " + methodDescription + " to " + result);
                } else if (mapped.size() != parameters.size()) {
                    throw new IllegalStateException("Cannot invoke " + methodDescription + " on " + parameters.size() + " parameters");
                }
                for (int index = 0; index < mapped.size(); index++) {
                    if (!parameters.get(index).asErasure().isAssignableTo(mapped.get(index).asErasure())) {
                        throw new IllegalStateException("Cannot invoke " + methodDescription + " on parameter " + index + " of type " + parameters.get(index));
                    }
                }
                return methodDescription.isVirtual() ? MethodInvocation.invoke(methodDescription).virtual(mapped.get(THIS_REFERENCE).asErasure()) : MethodInvocation.invoke(methodDescription);
            }

            /**
             * A method resolver for locating a method for a substitute.
             */
            public interface MethodResolver {

                /**
                 * Resolves the method to substitute with.
                 *
                 * @param receiver   The target type on which a member is accessed.
                 * @param original   The target field, method or constructor that is substituted,
                 * @param parameters All parameters that serve as input to this access.
                 * @param result     The result that is expected from the interaction or {@code void} if no result is expected.
                 * @return The field to substitute with.
                 */
                MethodDescription resolve(TypeDescription receiver, ByteCodeElement.Member original, TypeList.Generic parameters, TypeDescription.Generic result);

                /**
                 * A simple method resolver that returns a given method.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class Simple implements MethodResolver {

                    /**
                     * The method to substitute with.
                     */
                    private final MethodDescription methodDescription;

                    /**
                     * Creates a new simple method resolver.
                     *
                     * @param methodDescription The method to substitute with.
                     */
                    public Simple(MethodDescription methodDescription) {
                        this.methodDescription = methodDescription;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDescription resolve(TypeDescription receiver, ByteCodeElement.Member original, TypeList.Generic parameters, TypeDescription.Generic result) {
                        return methodDescription;
                    }
                }

                /**
                 * A method resolver that locates a non-static method by locating it from the receiver type.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class Matching implements MethodResolver {

                    /**
                     * The instrumented type.
                     */
                    private final TypeDescription instrumentedType;

                    /**
                     * The method graph compiler to use.
                     */
                    private final MethodGraph.Compiler methodGraphCompiler;

                    /**
                     * The matcher to use for locating the method to substitute with.
                     */
                    private final ElementMatcher<? super MethodDescription> matcher;

                    /**
                     * Creates a new matching method resolver.
                     *
                     * @param instrumentedType    The instrumented type.
                     * @param methodGraphCompiler The method graph compiler to use.
                     * @param matcher             The matcher to use for locating the method to substitute with.
                     */
                    public Matching(TypeDescription instrumentedType, MethodGraph.Compiler methodGraphCompiler, ElementMatcher<? super MethodDescription> matcher) {
                        this.instrumentedType = instrumentedType;
                        this.methodGraphCompiler = methodGraphCompiler;
                        this.matcher = matcher;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDescription resolve(TypeDescription receiver, ByteCodeElement.Member original, TypeList.Generic parameters, TypeDescription.Generic result) {
                        if (parameters.isEmpty()) {
                            throw new IllegalStateException("Cannot substitute parameterless instruction with " + original);
                        } else if (parameters.get(0).isPrimitive() || parameters.get(0).isArray()) {
                            throw new IllegalStateException("Cannot invoke method on primitive or array type for " + original);
                        }
                        TypeDefinition typeDefinition = parameters.get(0).accept(new TypeDescription.Generic.Visitor.Substitutor.ForReplacement(instrumentedType));
                        List<MethodDescription> candidates = CompoundList.<MethodDescription>of(methodGraphCompiler.compile(typeDefinition, instrumentedType).listNodes()
                                .asMethodList()
                                .filter(matcher), typeDefinition.getDeclaredMethods().filter(isPrivate().<MethodDescription>and(isVisibleTo(instrumentedType)).and(matcher)));
                        if (candidates.size() == 1) {
                            return candidates.get(0);
                        } else {
                            throw new IllegalStateException("Not exactly one method that matches " + matcher + ": " + candidates);
                        }
                    }
                }
            }

            /**
             * A factory for a substitution that invokes the instrumented method.
             */
            enum OfInstrumentedMethod implements Factory {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Substitution make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
                    return new ForMethodInvocation(instrumentedType, new MethodResolver.Simple(instrumentedMethod));
                }
            }

            /**
             * A factory for a substitution that invokes a given method.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public static class OfGivenMethod implements Factory {

                /**
                 * The method to invoke.
                 */
                private final MethodDescription methodDescription;

                /**
                 * Creates a new factory for a substitution that invokes a given method.
                 *
                 * @param methodDescription The method to invoke.
                 */
                public OfGivenMethod(MethodDescription methodDescription) {
                    this.methodDescription = methodDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public Substitution make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
                    return new ForMethodInvocation(instrumentedType, new MethodResolver.Simple(methodDescription));
                }
            }

            /**
             * A factory for a substitution that locates a method on the receiver type using a matcher.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public static class OfMatchedMethod implements Factory {

                /**
                 * The matcher for locating the method to substitute with.
                 */
                private final ElementMatcher<? super MethodDescription> matcher;

                /**
                 * The method graph compiler to use.
                 */
                private final MethodGraph.Compiler methodGraphCompiler;

                /**
                 * Creates a factory for a substitution that locates a method on the receiver type.
                 *
                 * @param matcher             The matcher for locating the method to substitute with.
                 * @param methodGraphCompiler The method graph compiler to use.
                 */
                public OfMatchedMethod(ElementMatcher<? super MethodDescription> matcher, MethodGraph.Compiler methodGraphCompiler) {
                    this.matcher = matcher;
                    this.methodGraphCompiler = methodGraphCompiler;
                }

                /**
                 * {@inheritDoc}
                 */
                public Substitution make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
                    return new ForMethodInvocation(instrumentedType, new MethodResolver.Matching(instrumentedType, methodGraphCompiler, matcher));
                }
            }
        }

        /**
         * A substitution chain allows for chaining multiple substitution steps for a byte code element being replaced.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Chain implements Substitution {

            /**
             * The assigner to use.
             */
            private final Assigner assigner;

            /**
             * The typing of the assignment to use.
             */
            private final Assigner.Typing typing;

            /**
             * The substitution steps to apply.
             */
            private final List<Step> steps;

            /**
             * Creates a new substitution chain.
             *
             * @param assigner The assigner to use.
             * @param typing   The typing of the assignment to use.
             * @param steps    The substitution steps to apply.
             */
            protected Chain(Assigner assigner, Assigner.Typing typing, List<Step> steps) {
                this.assigner = assigner;
                this.typing = typing;
                this.steps = steps;
            }

            /**
             * Creates a new substitution chain that uses a default assigner and static typing.
             *
             * @return A new substitution chain.
             */
            public static Chain.Factory withDefaultAssigner() {
                return with(Assigner.DEFAULT, Assigner.Typing.STATIC);
            }

            /**
             * Creates a new substitution chain.
             *
             * @param assigner The assigner to use.
             * @param typing   The typing of the assignment to use.
             * @return A new substitution chain.
             */
            public static Chain.Factory with(Assigner assigner, Assigner.Typing typing) {
                return new Chain.Factory(assigner, typing, Collections.<Step.Factory>emptyList());
            }

            /**
             * {@inheritDoc}
             */
            public StackManipulation resolve(TypeDescription receiver,
                                             ByteCodeElement.Member original,
                                             TypeList.Generic parameters,
                                             TypeDescription.Generic result,
                                             JavaConstant.MethodHandle methodHandle,
                                             StackManipulation stackManipulation,
                                             int freeOffset) {
                List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(1
                        + parameters.size() + steps.size() * 2
                        + (result.represents(void.class) ? 0 : 2));
                Map<Integer, Integer> offsets = new HashMap<Integer, Integer>();
                for (int index = parameters.size() - 1; index >= 0; index--) {
                    stackManipulations.add(MethodVariableAccess.of(parameters.get(index)).storeAt(freeOffset));
                    offsets.put(index, freeOffset);
                    freeOffset += parameters.get(index).getStackSize().getSize();
                }
                stackManipulations.add(DefaultValue.of(result));
                TypeDescription.Generic current = result;
                for (Step step : steps) {
                    Step.Resolution resolution = step.resolve(receiver, original, parameters, result, methodHandle, stackManipulation, current, offsets, freeOffset);
                    stackManipulations.add(resolution.getStackManipulation());
                    current = resolution.getResultType();
                }
                StackManipulation assignment = assigner.assign(current, result, typing);
                if (!assignment.isValid()) {
                    throw new IllegalStateException("Failed to assign " + current + " to " + result);
                }
                stackManipulations.add(assignment);
                return new StackManipulation.Compound(stackManipulations);
            }

            /**
             * Represents a step of a substitution chain.
             */
            public interface Step {

                /**
                 * Resolves this step of a substitution chain.
                 *
                 * @param receiver          The target result type of the substitution.
                 * @param original          The byte code element that is currently substituted.
                 * @param parameters        The parameters of the substituted element.
                 * @param result            The resulting type of the substituted element.
                 * @param methodHandle      A method handle of the stackManipulation invocation that is being substituted.
                 * @param stackManipulation The byte code instruction that is being substituted.
                 * @param current           The current type of the applied substitution that is the top element on the operand stack.
                 * @param offsets           The arguments of the substituted byte code element mapped to their local variable offsets.
                 * @param freeOffset        The first free offset in the local variable array.
                 * @return A resolved substitution step for the supplied inputs.
                 */
                Resolution resolve(TypeDescription receiver,
                                   ByteCodeElement.Member original,
                                   TypeList.Generic parameters,
                                   TypeDescription.Generic result,
                                   JavaConstant.MethodHandle methodHandle,
                                   StackManipulation stackManipulation,
                                   TypeDescription.Generic current,
                                   Map<Integer, Integer> offsets,
                                   int freeOffset);

                /**
                 * A resolved substitution step.
                 */
                interface Resolution {

                    /**
                     * Returns the stack manipulation to apply the substitution.
                     *
                     * @return The stack manipulation to apply the substitution.
                     */
                    StackManipulation getStackManipulation();

                    /**
                     * Returns the resulting type of the substitution or {@code void} if no resulting value is applied.
                     *
                     * @return The resulting type of the substitution or {@code void} if no resulting value is applied.
                     */
                    TypeDescription.Generic getResultType();
                }

                /**
                 * Resolves a substitution for an instrumented method.
                 */
                interface Factory {

                    /**
                     * Creates a substitution step for an instrumented method.
                     *
                     * @param assigner           The assigner to use.
                     * @param typing             The typing to use.
                     * @param instrumentedType   The instrumented type.
                     * @param instrumentedMethod The instrumented method.
                     * @return The substitution step to apply.
                     */
                    Step make(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod);
                }

                /**
                 * A step that executes the original method invocation or field access.
                 */
                enum OfOriginalExpression implements Step, Factory {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * {@inheritDoc}
                     */
                    public Resolution resolve(TypeDescription receiver,
                                              ByteCodeElement.Member original,
                                              TypeList.Generic parameters,
                                              TypeDescription.Generic result,
                                              JavaConstant.MethodHandle methodHandle,
                                              StackManipulation stackManipulation,
                                              TypeDescription.Generic current,
                                              Map<Integer, Integer> offsets,
                                              int freeOffset) {
                        List<StackManipulation> stackManipulations;
                        if (original instanceof MethodDescription && ((MethodDescription) original).isConstructor()) {
                            stackManipulations = new ArrayList<StackManipulation>(parameters.size() + 4);
                            stackManipulations.add(Removal.of(current));
                            stackManipulations.add(TypeCreation.of(original.getDeclaringType().asErasure()));
                            stackManipulations.add(Duplication.SINGLE);
                        } else {
                            stackManipulations = new ArrayList<StackManipulation>(parameters.size() + 4);
                            stackManipulations.add(Removal.of(current));
                        }
                        for (int index = 0; index < parameters.size(); index++) {
                            stackManipulations.add(MethodVariableAccess.of(parameters.get(index)).loadFrom(offsets.get(index)));
                        }
                        if (original instanceof MethodDescription) {
                            stackManipulations.add(stackManipulation);
                            return new Simple(new StackManipulation.Compound(stackManipulations), ((MethodDescription) original).isConstructor()
                                    ? original.getDeclaringType().asGenericType()
                                    : ((MethodDescription) original).getReturnType());
                        } else if (original instanceof FieldDescription) {
                            if (original.isStatic()) {
                                if (parameters.isEmpty()) {
                                    stackManipulations.add(stackManipulation);
                                    return new Simple(new StackManipulation.Compound(stackManipulations), ((FieldDescription) original).getType());
                                } else /* if (parameters.size() == 1) */ {
                                    stackManipulations.add(stackManipulation);
                                    return new Simple(new StackManipulation.Compound(stackManipulations), TypeDefinition.Sort.describe(void.class));
                                }
                            } else {
                                if (parameters.size() == 1) {
                                    stackManipulations.add(FieldAccess.forField((FieldDescription) original).read());
                                    return new Simple(new StackManipulation.Compound(stackManipulations), ((FieldDescription) original).getType());
                                } else /* if (parameters.size() == 2) */ {
                                    stackManipulations.add(FieldAccess.forField((FieldDescription) original).write());
                                    return new Simple(new StackManipulation.Compound(stackManipulations), TypeDefinition.Sort.describe(void.class));
                                }
                            }
                        } else {
                            throw new IllegalArgumentException("Unexpected target type: " + original);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Step make(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                        return this;
                    }
                }

                /**
                 * A simple substitution step within a substitution chain.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class Simple implements Step, Resolution, Factory {

                    /**
                     * The stack manipulation to apply.
                     */
                    private final StackManipulation stackManipulation;

                    /**
                     * The resulting type of applying the stack manipulation.
                     */
                    private final TypeDescription.Generic resultType;

                    /**
                     * Creates a new simple substitution step.
                     *
                     * @param stackManipulation The stack manipulation to apply.
                     * @param resultType        The resulting type of applying the stack manipulation.
                     */
                    public Simple(StackManipulation stackManipulation, Type resultType) {
                        this(stackManipulation, TypeDefinition.Sort.describe(resultType));
                    }

                    /**
                     * Creates a new simple substitution step.
                     *
                     * @param stackManipulation The stack manipulation to apply.
                     * @param resultType        The resulting type of applying the stack manipulation.
                     */
                    public Simple(StackManipulation stackManipulation, TypeDescription.Generic resultType) {
                        this.stackManipulation = stackManipulation;
                        this.resultType = resultType;
                    }

                    /**
                     * Resolves a compile-time constant as the next step value.
                     *
                     * @param value The compile-time constant to resolve.
                     * @return An appropriate step factory.
                     */
                    public static Step.Factory of(Object value) {
                        ConstantValue constant = ConstantValue.Simple.wrap(value);
                        return new Simple(constant.toStackManipulation(), constant.getTypeDescription().asGenericType());
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Step make(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                        return this;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Resolution resolve(TypeDescription receiver,
                                              ByteCodeElement.Member original,
                                              TypeList.Generic parameters,
                                              TypeDescription.Generic result,
                                              JavaConstant.MethodHandle methodHandle,
                                              StackManipulation stackManipulation,
                                              TypeDescription.Generic current,
                                              Map<Integer, Integer> offsets,
                                              int freeOffset) {
                        return receiver.represents(void.class)
                                ? this
                                : new Simple(new StackManipulation.Compound(Removal.of(current), this.stackManipulation), resultType);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation getStackManipulation() {
                        return stackManipulation;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public TypeDescription.Generic getResultType() {
                        return resultType;
                    }
                }

                /**
                 * A step within a substitution chain that converts the current type to the expected return type.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForAssignment implements Step {

                    /**
                     * The result type or {@code null} if the type of the substitution result should be targeted.
                     */
                    @MaybeNull
                    @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
                    private final TypeDescription.Generic result;

                    /**
                     * The assigner to use.
                     */
                    private final Assigner assigner;

                    /**
                     * Creates a step for a type assignment.
                     *
                     * @param result   The result type or {@code null} if the type of the substitution result should be targeted.
                     * @param assigner The assigner to use.
                     */
                    protected ForAssignment(@MaybeNull TypeDescription.Generic result, Assigner assigner) {
                        this.result = result;
                        this.assigner = assigner;
                    }

                    /**
                     * Creates a step factory that casts the current stack top value to the specified type.
                     *
                     * @param type The type that should be cast to.
                     * @return An appropriate step factory.
                     */
                    public static Step.Factory castTo(Type type) {
                        return new Factory(TypeDefinition.Sort.describe(type));
                    }

                    /**
                     * Creates a step factory that casts the current stack top value to the specified type.
                     *
                     * @param typeDescription The description of the type that should be cast to.
                     * @return An appropriate step factory.
                     */
                    public static Step.Factory castTo(TypeDescription.Generic typeDescription) {
                        return new Factory(typeDescription);
                    }

                    /**
                     * Creates a step factory that casts the current stack top value to the expected return value.
                     *
                     * @return An appropriate step factory.
                     */
                    public static Step.Factory castToSubstitutionResult() {
                        return new Factory(null);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Resolution resolve(TypeDescription receiver,
                                              ByteCodeElement.Member original,
                                              TypeList.Generic parameters,
                                              TypeDescription.Generic result,
                                              JavaConstant.MethodHandle methodHandle,
                                              StackManipulation stackManipulation,
                                              TypeDescription.Generic current,
                                              Map<Integer, Integer> offsets,
                                              int freeOffset) {
                        StackManipulation assignment = assigner.assign(current, this.result == null ? result : this.result, Assigner.Typing.DYNAMIC);
                        if (!assignment.isValid()) {
                            throw new IllegalStateException("Failed to assign " + current + " to " + (this.result == null ? result : this.result));
                        }
                        return new Simple(assignment, this.result == null ? result : this.result);
                    }

                    /**
                     * A factory for creating a step for a dynamic type assignment.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    protected static class Factory implements Step.Factory {

                        /**
                         * The result type or {@code null} if the type of the substitution result should be targeted.
                         */
                        @MaybeNull
                        @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
                        private final TypeDescription.Generic result;

                        /**
                         * Creates a new factory for a step that applies a type assignment.
                         *
                         * @param result The result type or {@code null} if the type of the substitution result should be targeted.
                         */
                        protected Factory(@MaybeNull TypeDescription.Generic result) {
                            this.result = result;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Step make(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                            return new ForAssignment(result, assigner);
                        }
                    }
                }

                /**
                 * A step that substitutes an argument of a given index with a compatible type.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForArgumentSubstitution implements Step {

                    /**
                     * The stack manipulation that loads the substituted argument.
                     */
                    private final StackManipulation substitution;

                    /**
                     * The type of the substituted argument.
                     */
                    private final TypeDescription.Generic typeDescription;

                    /**
                     * The index of the argument to substitute.
                     */
                    private final int index;

                    /**
                     * The assigner to use for assigning the argument.
                     */
                    private final Assigner assigner;

                    /**
                     * The typing to use for the argument assignment.
                     */
                    private final Assigner.Typing typing;

                    /**
                     * Creates an argument substitution step.
                     *
                     * @param substitution    The stack manipulation that loads the substituted argument.
                     * @param typeDescription The type of the substituted argument.
                     * @param index           The index of the argument to substitute.
                     * @param assigner        The assigner to use for assigning the argument.
                     * @param typing          The typing to use for the argument assignment.
                     */
                    protected ForArgumentSubstitution(StackManipulation substitution, TypeDescription.Generic typeDescription, int index, Assigner assigner, Assigner.Typing typing) {
                        this.substitution = substitution;
                        this.typeDescription = typeDescription;
                        this.index = index;
                        this.assigner = assigner;
                        this.typing = typing;
                    }

                    /**
                     * Resolves a step substitution factory for a compile-time constant to replace an argument value at a given index.
                     *
                     * @param value The compile-time constant to replace.
                     * @param index The index of the substituted argument.
                     * @return An appropriate step factory.
                     */
                    public static Step.Factory of(Object value, int index) {
                        if (index < 0) {
                            throw new IllegalArgumentException("Index cannot be negative: " + index);
                        }
                        ConstantValue constant = ConstantValue.Simple.wrap(value);
                        return new Factory(constant.toStackManipulation(), constant.getTypeDescription().asGenericType(), index);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Resolution resolve(TypeDescription receiver,
                                              ByteCodeElement.Member original,
                                              TypeList.Generic parameters,
                                              TypeDescription.Generic result,
                                              JavaConstant.MethodHandle methodHandle,
                                              StackManipulation stackManipulation,
                                              TypeDescription.Generic current,
                                              Map<Integer, Integer> offsets,
                                              int freeOffset) {
                        if (index >= parameters.size()) {
                            throw new IllegalStateException(original + " has not " + index + " arguments");
                        }
                        StackManipulation assignment = assigner.assign(typeDescription, parameters.get(index), typing);
                        if (!assignment.isValid()) {
                            throw new IllegalStateException("Cannot assign " + typeDescription + " to " + parameters.get(index));
                        }
                        return new Simple(new StackManipulation.Compound(substitution, assignment, MethodVariableAccess.of(parameters.get(index)).storeAt(offsets.get(index))), current);
                    }

                    /**
                     * A factory to create an argument substitution step.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    public static class Factory implements Step.Factory {

                        /**
                         * The stack manipulation that loads the substituted argument.
                         */
                        private final StackManipulation stackManipulation;

                        /**
                         * The type of the substituted argument.
                         */
                        private final TypeDescription.Generic typeDescription;

                        /**
                         * The index of the argument to substitute.
                         */
                        private final int index;

                        /**
                         * Creates a factory for an argument substitution step.
                         *
                         * @param stackManipulation The stack manipulation that loads the substituted argument.
                         * @param type              The type of the substituted argument.
                         * @param index             The index of the argument to substitute.
                         */
                        public Factory(StackManipulation stackManipulation, Type type, int index) {
                            this(stackManipulation, TypeDefinition.Sort.describe(type), index);
                        }

                        /**
                         * Creates a factory for an argument substitution step.
                         *
                         * @param stackManipulation The stack manipulation that loads the substituted argument.
                         * @param typeDescription   The type of the substituted argument.
                         * @param index             The index of the argument to substitute.
                         */
                        public Factory(StackManipulation stackManipulation, TypeDescription.Generic typeDescription, int index) {
                            this.stackManipulation = stackManipulation;
                            this.typeDescription = typeDescription;
                            this.index = index;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Step make(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                            return new ForArgumentSubstitution(stackManipulation, typeDescription, index, assigner, typing);
                        }
                    }
                }

                /**
                 * A step that loads an argument to a method as the current chain value.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForArgumentLoading implements Step, Factory {

                    /**
                     * The index of the argument to substitute.
                     */
                    private final int index;

                    /**
                     * Creates an argument loading step.
                     *
                     * @param index The index of the argument to load.
                     */
                    protected ForArgumentLoading(int index) {
                        this.index = index;
                    }

                    /**
                     * Creates a factory that loads the argument for the targeted value's parameter of the specified index.
                     *
                     * @param index The index to load.
                     * @return An appropriate factory.
                     */
                    public static Factory ofTarget(int index) {
                        if (index < 0) {
                            throw new IllegalArgumentException("Argument index cannot be negative: " + index);
                        }
                        return new ForArgumentLoading(index);
                    }

                    /**
                     * Creates a factory that loads the argument for the instrumented method's parameter of the specified index.
                     *
                     * @param index The index to load.
                     * @return An appropriate factory.
                     */
                    public static Factory ofInstrumentedMethod(int index) {
                        if (index < 0) {
                            throw new IllegalArgumentException("Argument index cannot be negative: " + index);
                        }
                        return new OfInstrumentedMethod(index);
                    }

                    /**
                     * Creates a factory that loads the {@code this} reference of the instrumented method.
                     *
                     * @return An appropriate factory.
                     */
                    public static Factory ofThis() {
                        return OfInstrumentedMethodThis.INSTANCE;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Step make(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                        return this;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Resolution resolve(TypeDescription receiver,
                                              ByteCodeElement.Member original,
                                              TypeList.Generic parameters,
                                              TypeDescription.Generic result,
                                              JavaConstant.MethodHandle methodHandle,
                                              StackManipulation stackManipulation,
                                              TypeDescription.Generic current,
                                              Map<Integer, Integer> offsets,
                                              int freeOffset) {
                        if (index >= parameters.size()) {
                            throw new IllegalStateException(original + " has not " + index + " arguments");
                        }
                        return new Simple(new StackManipulation.Compound(Removal.of(current), MethodVariableAccess.of(parameters.get(index)).loadFrom(offsets.get(index))), parameters.get(index));
                    }

                    /**
                     * A factory that resolves the {@code this} reference of the instrumented method.
                     */
                    protected enum OfInstrumentedMethodThis implements Factory {

                        /**
                         * The singleton instance.
                         */
                        INSTANCE;

                        /**
                         * {@inheritDoc}
                         */
                        public Step make(Assigner assigner,
                                         Assigner.Typing typing,
                                         TypeDescription instrumentedType,
                                         MethodDescription instrumentedMethod) {
                            if (instrumentedMethod.isStatic()) {
                                throw new IllegalStateException(instrumentedMethod + " is static and does not define a this reference");
                            }
                            return new Simple(MethodVariableAccess.loadThis(), instrumentedType.asGenericType());
                        }
                    }

                    /**
                     * A factory that resolves a given argument of the instrumented method.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    protected static class OfInstrumentedMethod implements Factory {

                        /**
                         * The index of the argument to load.
                         */
                        private final int index;

                        /**
                         * Creates a new factory for resolving an argument of the instrumented method.
                         *
                         * @param index The index of the argument to load.
                         */
                        protected OfInstrumentedMethod(int index) {
                            this.index = index;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Step make(Assigner assigner,
                                         Assigner.Typing typing,
                                         TypeDescription instrumentedType,
                                         MethodDescription instrumentedMethod) {
                            if (instrumentedMethod.getParameters().size() < index) {
                                throw new IllegalStateException(instrumentedMethod + " does not declare " + index + " parameters");
                            }
                            ParameterDescription parameterDescription = instrumentedMethod.getParameters().get(index);
                            return new Simple(MethodVariableAccess.load(parameterDescription), parameterDescription.getType());
                        }
                    }
                }

                /**
                 * Creates a step for a field access.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                abstract class ForField implements Step {

                    /**
                     * The field description accessed in this step.
                     */
                    protected final FieldDescription fieldDescription;

                    /**
                     * The assigner to use.
                     */
                    protected final Assigner assigner;

                    /**
                     * The typing to use when assigning.
                     */
                    protected final Assigner.Typing typing;

                    /**
                     * Creates a new step for a field access.
                     *
                     * @param fieldDescription The field description accessed in this step.
                     * @param assigner         The assigner to use.
                     * @param typing           The typing to use when assigning.
                     */
                    protected ForField(FieldDescription fieldDescription, Assigner assigner, Assigner.Typing typing) {
                        this.fieldDescription = fieldDescription;
                        this.assigner = assigner;
                        this.typing = typing;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Field description always has declaring type.")
                    public Resolution resolve(TypeDescription receiver,
                                              ByteCodeElement.Member original,
                                              TypeList.Generic parameters,
                                              TypeDescription.Generic result,
                                              JavaConstant.MethodHandle methodHandle,
                                              StackManipulation stackManipulation,
                                              TypeDescription.Generic current,
                                              Map<Integer, Integer> offsets,
                                              int freeOffset) {
                        List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(2);
                        if (fieldDescription.isStatic()) {
                            stackManipulations.add(Removal.of(current));
                        } else {
                            StackManipulation assignment = assigner.assign(current, fieldDescription.getDeclaringType().asGenericType(), typing);
                            if (!assignment.isValid()) {
                                throw new IllegalStateException("Cannot assign " + current + " to " + fieldDescription.getDeclaringType());
                            }
                            stackManipulations.add(assignment);
                        }
                        return doResolve(original, parameters, offsets, new StackManipulation.Compound(stackManipulations));
                    }

                    /**
                     * Completes the resolution.
                     *
                     * @param original          The byte code element that is currently substituted.
                     * @param parameters        The parameters of the substituted element.
                     * @param offsets           The arguments of the substituted byte code element mapped to their local variable offsets.
                     * @param stackManipulation A stack manipulation to prepare the field access.
                     * @return A resolved substitution step for the supplied inputs.
                     */
                    protected abstract Resolution doResolve(ByteCodeElement.Member original, TypeList.Generic parameters, Map<Integer, Integer> offsets, StackManipulation stackManipulation);

                    /**
                     * A step for reading a field.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    public static class Read extends ForField {

                        /**
                         * Creates a step for reading a field.
                         *
                         * @param fieldDescription A description of the field being read.
                         * @param assigner         The assigner to use.
                         * @param typing           The typing to use when assigning.
                         */
                        protected Read(FieldDescription fieldDescription, Assigner assigner, Assigner.Typing typing) {
                            super(fieldDescription, assigner, typing);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        protected Resolution doResolve(ByteCodeElement.Member original, TypeList.Generic parameters, Map<Integer, Integer> offsets, StackManipulation stackManipulation) {
                            return new Simple(new StackManipulation.Compound(stackManipulation, FieldAccess.forField(fieldDescription).read()), fieldDescription.getType());
                        }

                        /**
                         * A factory for creating a field read step in a chain.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        public static class Factory implements Step.Factory {

                            /**
                             * A description of the field being read.
                             */
                            private final FieldDescription fieldDescription;

                            /**
                             * Creates a factory for a step reading a field.
                             *
                             * @param field The field being read.
                             */
                            public Factory(Field field) {
                                this(new FieldDescription.ForLoadedField(field));
                            }

                            /**
                             * Creates a factory for a step reading a field.
                             *
                             * @param fieldDescription A description of the field being read.
                             */
                            public Factory(FieldDescription fieldDescription) {
                                this.fieldDescription = fieldDescription;
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public Step make(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                return new Read(fieldDescription, assigner, typing);
                            }
                        }
                    }

                    /**
                     * A step for writing to a field.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    public static class Write extends ForField {

                        /**
                         * The index of the parameter being accessed. If the targeted element is a non-static method, is increased by one.
                         */
                        private final int index;

                        /**
                         * Creates a step for writing to a field.
                         *
                         * @param fieldDescription A description of the field to write to.
                         * @param assigner         The assigner to use.
                         * @param typing           The typing to use when assigning.
                         * @param index            The index of the parameter being accessed. If the targeted element is a non-static method, is increased by one.
                         */
                        protected Write(FieldDescription fieldDescription, Assigner assigner, Assigner.Typing typing, int index) {
                            super(fieldDescription, assigner, typing);
                            this.index = index;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        protected Resolution doResolve(ByteCodeElement.Member original, TypeList.Generic parameters, Map<Integer, Integer> offsets, StackManipulation stackManipulation) {
                            int index = ((original.getModifiers() & Opcodes.ACC_STATIC) == 0)
                                    && !(original instanceof MethodDescription
                                    && ((MethodDescription) original).isConstructor()) ? this.index + 1 : this.index;
                            if (index >= parameters.size()) {
                                throw new IllegalStateException(original + " does not define an argument with index " + index);
                            }
                            StackManipulation assignment = assigner.assign(parameters.get(index), fieldDescription.getType(), typing);
                            if (!assignment.isValid()) {
                                throw new IllegalStateException("Cannot write " + parameters.get(index) + " to " + fieldDescription);
                            }
                            return new Simple(new StackManipulation.Compound(stackManipulation,
                                    MethodVariableAccess.of(parameters.get(index)).loadFrom(offsets.get(index)),
                                    assignment,
                                    FieldAccess.forField(fieldDescription).write()), TypeDefinition.Sort.describe(void.class));
                        }

                        /**
                         * A factory for creating a step to write to a field.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        public static class Factory implements Step.Factory {

                            /**
                             * A description of the field to write to.
                             */
                            private final FieldDescription fieldDescription;

                            /**
                             * The index of the parameter being accessed. If the targeted element is a non-static method, is increased by one.
                             */
                            private final int index;

                            /**
                             * Creates a factory for writing to a field.
                             *
                             * @param field The field to write to.
                             * @param index The index of the parameter being accessed. If the targeted element is a non-static method, is increased by one.
                             */
                            public Factory(Field field, int index) {
                                this(new FieldDescription.ForLoadedField(field), index);
                            }

                            /**
                             * Creates a factory for writing to a field.
                             *
                             * @param fieldDescription A description of the field to write to.
                             * @param index            The index of the parameter being accessed. If the targeted element is a non-static method, is increased by one.
                             */
                            public Factory(FieldDescription fieldDescription, int index) {
                                this.fieldDescription = fieldDescription;
                                this.index = index;
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public Step make(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                return new Write(fieldDescription, assigner, typing, index);
                            }
                        }
                    }
                }

                /**
                 * A step for invoking a method or constructor. If non-static, a method is invoked upon a the current stack argument of the chain.
                 * Arguments are loaded from the intercepted byte code element with a possibility of substitution.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForInvocation implements Step {

                    /**
                     * The invoked method or constructor.
                     */
                    private final MethodDescription methodDescription;

                    /**
                     * A mapping of substituted parameter indices. For targets that are non-static methods, the targeted index is increased by one.
                     */
                    private final Map<Integer, Integer> substitutions;

                    /**
                     * The assigner to use.
                     */
                    private final Assigner assigner;

                    /**
                     * The typing to use when assigning.
                     */
                    private final Assigner.Typing typing;

                    /**
                     * Creates a new step of an invocation.
                     *
                     * @param methodDescription The invoked method or constructor.
                     * @param substitutions     A mapping of substituted parameter indices. For targets that are non-static methods, the targeted index is increased by one.
                     * @param assigner          The assigner to use.
                     * @param typing            The typing to use when assigning.
                     */
                    protected ForInvocation(MethodDescription methodDescription, Map<Integer, Integer> substitutions, Assigner assigner, Assigner.Typing typing) {
                        this.methodDescription = methodDescription;
                        this.substitutions = substitutions;
                        this.assigner = assigner;
                        this.typing = typing;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Resolution resolve(TypeDescription receiver,
                                              ByteCodeElement.Member original,
                                              TypeList.Generic parameters,
                                              TypeDescription.Generic result,
                                              JavaConstant.MethodHandle methodHandle,
                                              StackManipulation stackManipulation,
                                              TypeDescription.Generic current,
                                              Map<Integer, Integer> offsets,
                                              int freeOffset) {
                        List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(3 + parameters.size() * 2);
                        if (methodDescription.isStatic()) {
                            stackManipulations.add(Removal.of(current));
                        } else if (methodDescription.isConstructor()) {
                            stackManipulations.add(Removal.of(current));
                            stackManipulations.add(TypeCreation.of(methodDescription.getDeclaringType().asErasure()));
                        } else {
                            StackManipulation assignment = assigner.assign(current, methodDescription.getDeclaringType().asGenericType(), typing);
                            if (!assignment.isValid()) {
                                throw new IllegalStateException("Cannot assign " + current + " to " + methodDescription.getDeclaringType());
                            }
                            stackManipulations.add(assignment);
                        }
                        boolean shift = ((original.getModifiers() & Opcodes.ACC_STATIC) == 0) && !(original instanceof MethodDescription && ((MethodDescription) original).isConstructor());
                        for (int index = 0; index < methodDescription.getParameters().size(); index++) {
                            int substitution = substitutions.containsKey(index + (shift ? 1 : 0)) ? substitutions.get(index + (shift ? 1 : 0)) : index + (shift ? 1 : 0);
                            if (substitution >= parameters.size()) {
                                throw new IllegalStateException(original + " does not support an index " + substitution);
                            }
                            stackManipulations.add(MethodVariableAccess.of(parameters.get(substitution)).loadFrom(offsets.get(substitution)));
                            StackManipulation assignment = assigner.assign(parameters.get(substitution), methodDescription.getParameters().get(index).getType(), typing);
                            if (!assignment.isValid()) {
                                throw new IllegalStateException("Cannot assign parameter with " + index + " of type " + parameters.get(substitution) + " to " + methodDescription);
                            }
                            stackManipulations.add(assignment);
                        }
                        stackManipulations.add(MethodInvocation.invoke(methodDescription));
                        return new Simple(new StackManipulation.Compound(stackManipulations), methodDescription.getReturnType());
                    }

                    /**
                     * A factory to create a step for a method invocation.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    public static class Factory implements Step.Factory {

                        /**
                         * The invoked method or constructor.
                         */
                        private final MethodDescription methodDescription;

                        /**
                         * A mapping of substituted parameter indices. For targets that are non-static methods, the targeted index is increased by one.
                         */
                        private final Map<Integer, Integer> substitutions;

                        /**
                         * Creates a factory for a method invocation without parameter substitutions.
                         *
                         * @param method The invoked method.
                         */
                        public Factory(Method method) {
                            this(new MethodDescription.ForLoadedMethod(method));
                        }

                        /**
                         * Creates a factory for a method invocation without parameter substitutions.
                         *
                         * @param constructor The constructor.
                         */
                        public Factory(Constructor<?> constructor) {
                            this(new MethodDescription.ForLoadedConstructor(constructor));
                        }

                        /**
                         * Creates a factory for a method invocation without parameter substitutions.
                         *
                         * @param methodDescription The invoked method or constructor.
                         */
                        public Factory(MethodDescription methodDescription) {
                            this(methodDescription, Collections.<Integer, Integer>emptyMap());
                        }

                        /**
                         * Creates a factory for a method invocation.
                         *
                         * @param methodDescription The invoked method or constructor.
                         * @param substitutions     A mapping of substituted parameter indices. For targets that are non-static methods,
                         *                          the targeted index is increased by one.
                         */
                        public Factory(MethodDescription methodDescription, Map<Integer, Integer> substitutions) {
                            this.methodDescription = methodDescription;
                            this.substitutions = substitutions;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Step make(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                            return new ForInvocation(methodDescription, substitutions, assigner, typing);
                        }
                    }
                }

                /**
                 * A step that invokes a delegation method based on annotations on the parameters of the targeted method.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForDelegation implements Step {

                    /**
                     * The type on top of the stack after the delegation is complete.
                     */
                    private final TypeDescription.Generic returned;

                    /**
                     * The dispatcher to use.
                     */
                    private final Dispatcher.Resolved dispatcher;

                    /**
                     * A list of offset mappings to execute prior to delegation.
                     */
                    private final List<OffsetMapping.Resolved> offsetMappings;

                    /**
                     * @param returned       The type on top of the stack after the delegation is complete.
                     * @param dispatcher     The dispatcher to use.
                     * @param offsetMappings A list of offset mappings to execute prior to delegation.
                     */
                    protected ForDelegation(TypeDescription.Generic returned, Dispatcher.Resolved dispatcher, List<OffsetMapping.Resolved> offsetMappings) {
                        this.returned = returned;
                        this.dispatcher = dispatcher;
                        this.offsetMappings = offsetMappings;
                    }

                    /**
                     * Returns a delegating step factory for the supplied method.
                     *
                     * @param method The method to delegate to.
                     * @return An appropriate step factory.
                     */
                    public static Step.Factory to(Method method) {
                        return to(new MethodDescription.ForLoadedMethod(method));
                    }

                    /**
                     * Returns a delegating step factory for the supplied constructor.
                     *
                     * @param constructor The constructor to delegate to.
                     * @return An appropriate step factory.
                     */
                    public static Step.Factory to(Constructor<?> constructor) {
                        return to(new MethodDescription.ForLoadedConstructor(constructor));
                    }

                    /**
                     * Returns a delegating step factory for the supplied method description..
                     *
                     * @param methodDescription A description of the method or constructor to delegate to.
                     * @return An appropriate step factory.
                     */
                    public static Step.Factory to(MethodDescription.InDefinedShape methodDescription) {
                        if (methodDescription.isTypeInitializer()) {
                            throw new IllegalArgumentException("Cannot delegate to a type initializer: " + methodDescription);
                        }
                        return to(methodDescription, Dispatcher.ForRegularInvocation.Factory.INSTANCE, Collections.<OffsetMapping.Factory<?>>emptyList());
                    }

                    /**
                     * Creates an appropriate step factory for the given delegate method, dispatcher factory and user factories.
                     *
                     * @param delegate          A description of the method or constructor to delegate to.
                     * @param dispatcherFactory The dispatcher factory to use.
                     * @param userFactories     Factories for custom annotation bindings.
                     * @return An appropriate step factory.
                     */
                    @SuppressWarnings("unchecked")
                    private static Step.Factory to(MethodDescription.InDefinedShape delegate, Dispatcher.Factory dispatcherFactory, List<? extends OffsetMapping.Factory<?>> userFactories) {
                        if (delegate.isTypeInitializer()) {
                            throw new IllegalArgumentException("Cannot delegate to type initializer: " + delegate);
                        }
                        return new Factory(delegate, dispatcherFactory.make(delegate), CompoundList.of(Arrays.asList(
                                OffsetMapping.ForArgument.Factory.INSTANCE,
                                OffsetMapping.ForThisReference.Factory.INSTANCE,
                                OffsetMapping.ForAllArguments.Factory.INSTANCE,
                                OffsetMapping.ForSelfCallHandle.Factory.INSTANCE,
                                OffsetMapping.ForField.Unresolved.Factory.INSTANCE,
                                OffsetMapping.ForFieldHandle.Unresolved.GetterFactory.INSTANCE,
                                OffsetMapping.ForFieldHandle.Unresolved.SetterFactory.INSTANCE,
                                OffsetMapping.ForOrigin.Factory.INSTANCE,
                                OffsetMapping.ForStubValue.Factory.INSTANCE,
                                new OffsetMapping.ForStackManipulation.OfDefaultValue<Unused>(Unused.class),
                                OffsetMapping.ForCurrent.Factory.INSTANCE), userFactories));
                    }

                    /**
                     * Returns a builder for creating a {@link ForDelegation} with custom configuration.
                     *
                     * @return A bulder for creating a custom delegator.
                     */
                    public static WithCustomMapping withCustomMapping() {
                        return new WithCustomMapping(Dispatcher.ForRegularInvocation.Factory.INSTANCE, Collections.<Class<? extends Annotation>, OffsetMapping.Factory<?>>emptyMap());
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Resolution resolve(TypeDescription receiver,
                                              ByteCodeElement.Member original,
                                              TypeList.Generic parameters,
                                              TypeDescription.Generic result,
                                              JavaConstant.MethodHandle methodHandle,
                                              StackManipulation stackManipulation,
                                              TypeDescription.Generic current,
                                              Map<Integer, Integer> offsets,
                                              int freeOffset) {
                        List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(offsetMappings.size() + 3);
                        stackManipulations.add(current.represents(void.class)
                                ? StackManipulation.Trivial.INSTANCE
                                : MethodVariableAccess.of(current).storeAt(freeOffset));
                        stackManipulations.add(dispatcher.initialize());
                        for (OffsetMapping.Resolved offsetMapping : offsetMappings) {
                            stackManipulations.add(offsetMapping.apply(receiver, original, parameters, result, current, methodHandle, offsets, freeOffset));
                        }
                        stackManipulations.add(dispatcher.apply(receiver, original, methodHandle));
                        return new Simple(new StackManipulation.Compound(stackManipulations), returned);
                    }

                    /**
                     * A factory for creating a delegating step during a member substitution.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    protected static class Factory implements Step.Factory {

                        /**
                         * A description of the method or constructor to delegate to.
                         */
                        private final MethodDescription.InDefinedShape delegate;

                        /**
                         * The dispatcher to use for invoking the delegate.
                         */
                        private final Dispatcher dispatcher;

                        /**
                         * The offset mappings to use.
                         */
                        private final List<OffsetMapping> offsetMappings;

                        /**
                         * Creates a new factory for a delegating step.
                         *
                         * @param delegate   A description of the method or constructor to delegate to.
                         * @param dispatcher The dispatcher to use for invoking the delegate.
                         * @param factories  The dispatcher to use for invoking the delegate.
                         */
                        protected Factory(MethodDescription.InDefinedShape delegate, Dispatcher dispatcher, List<? extends OffsetMapping.Factory<?>> factories) {
                            Map<TypeDescription, OffsetMapping.Factory<?>> offsetMappings = new HashMap<TypeDescription, OffsetMapping.Factory<?>>();
                            for (OffsetMapping.Factory<?> factory : factories) {
                                offsetMappings.put(net.bytebuddy.description.type.TypeDescription.ForLoadedType.of(factory.getAnnotationType()), factory);
                            }
                            this.offsetMappings = new ArrayList<OffsetMapping>(factories.size());
                            if (delegate.isMethod() && !delegate.isStatic()) {
                                OffsetMapping offsetMapping = null;
                                for (AnnotationDescription annotationDescription : delegate.getDeclaredAnnotations()) {
                                    OffsetMapping.Factory<?> factory = offsetMappings.get(annotationDescription.getAnnotationType());
                                    if (factory != null) {
                                        @SuppressWarnings("unchecked") OffsetMapping current = factory.make(delegate, (AnnotationDescription.Loadable) annotationDescription.prepare(factory.getAnnotationType()));
                                        if (offsetMapping == null) {
                                            offsetMapping = current;
                                        } else {
                                            throw new IllegalStateException(delegate + " is bound to both " + current + " and " + offsetMapping);
                                        }
                                    }
                                }
                                this.offsetMappings.add(offsetMapping == null
                                        ? new OffsetMapping.ForThisReference(delegate.getDeclaringType().asGenericType(), null, Source.SUBSTITUTED_ELEMENT, false)
                                        : offsetMapping);
                            }
                            for (int index = 0; index < delegate.getParameters().size(); index++) {
                                ParameterDescription.InDefinedShape parameterDescription = delegate.getParameters().get(index);
                                OffsetMapping offsetMapping = null;
                                for (AnnotationDescription annotationDescription : parameterDescription.getDeclaredAnnotations()) {
                                    OffsetMapping.Factory<?> factory = offsetMappings.get(annotationDescription.getAnnotationType());
                                    if (factory != null) {
                                        @SuppressWarnings("unchecked") OffsetMapping current = factory.make(parameterDescription, (AnnotationDescription.Loadable) annotationDescription.prepare(factory.getAnnotationType()));
                                        if (offsetMapping == null) {
                                            offsetMapping = current;
                                        } else {
                                            throw new IllegalStateException(parameterDescription + " is bound to both " + current + " and " + offsetMapping);
                                        }
                                    }
                                }
                                this.offsetMappings.add(offsetMapping == null
                                        ? new OffsetMapping.ForArgument(parameterDescription.getType(), index, null, Source.SUBSTITUTED_ELEMENT, false)
                                        : offsetMapping);
                            }
                            this.delegate = delegate;
                            this.dispatcher = dispatcher;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Step make(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                            List<OffsetMapping.Resolved> targets = new ArrayList<OffsetMapping.Resolved>(offsetMappings.size());
                            for (OffsetMapping offsetMapping : offsetMappings) {
                                targets.add(offsetMapping.resolve(assigner, typing, instrumentedType, instrumentedMethod));
                            }
                            return new ForDelegation(delegate.getReturnType(), dispatcher.resolve(instrumentedType, instrumentedMethod), targets);
                        }
                    }

                    /**
                     * An offset mapping for binding a parameter or dispatch target for the method or constructor that is delegated to.
                     */
                    public interface OffsetMapping {

                        /**
                         * Resolves an offset mapping for a given instrumented method.
                         *
                         * @param assigner           The assigner to use.
                         * @param typing             The typing to use if no explicit typing is specified.
                         * @param instrumentedType   The instrumented type.
                         * @param instrumentedMethod The instrumented method.
                         * @return A resolved version of this offset mapping.
                         */
                        OffsetMapping.Resolved resolve(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod);

                        /**
                         * An offset mapping that was resolved for a given instrumented type and method.
                         */
                        interface Resolved {

                            /**
                             * Applies this offset mapping.
                             *
                             * @param receiver     The target type of the invoked delegate.
                             * @param original     The substituted element.
                             * @param parameters   The parameters that are supplied to the substituted expression.
                             * @param result       The resulting type of the substituted expression.
                             * @param current      The type of the value that was produced by the previous step in the substitution chain.
                             * @param methodHandle A method handle that represents the substituted element.
                             * @param offsets      The offsets of the supplied parameters.
                             * @param offset       The offset of the value that was produced by the previous step.
                             * @return An appropriate stack manipulation.
                             */
                            StackManipulation apply(TypeDescription receiver,
                                                    ByteCodeElement.Member original,
                                                    TypeList.Generic parameters,
                                                    TypeDescription.Generic result,
                                                    TypeDescription.Generic current,
                                                    JavaConstant.MethodHandle methodHandle,
                                                    Map<Integer, Integer> offsets,
                                                    int offset);

                            /**
                             * An offset mapping that loads a stack manipulation.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            class ForStackManipulation implements OffsetMapping.Resolved {

                                /**
                                 * The stack manipulation to load.
                                 */
                                private final StackManipulation stackManipulation;

                                /**
                                 * Creates a resolved offset mapping for a stack manipulation.
                                 *
                                 * @param stackManipulation The stack manipulation to load.
                                 */
                                public ForStackManipulation(StackManipulation stackManipulation) {
                                    this.stackManipulation = stackManipulation;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public StackManipulation apply(TypeDescription receiver,
                                                               ByteCodeElement.Member original,
                                                               TypeList.Generic parameters,
                                                               TypeDescription.Generic result,
                                                               TypeDescription.Generic current,
                                                               JavaConstant.MethodHandle methodHandle,
                                                               Map<Integer, Integer> offsets,
                                                               int offset) {
                                    return stackManipulation;
                                }
                            }
                        }

                        /**
                         * A factory for creating an offset mapping based on an annotation on a parameter, method or constructor.
                         *
                         * @param <T> The type of the annotation.
                         */
                        interface Factory<T extends Annotation> {

                            /**
                             * Returns the type of the annotation for this factory.
                             *
                             * @return The type of the annotation for this factory.
                             */
                            Class<T> getAnnotationType();

                            /**
                             * Creates an offset mapping for an annotation that was found on a non-static method.
                             *
                             * @param target     The method that is the delegated to.
                             * @param annotation The annotation that was found on the method.
                             * @return An appropriate offset mapping.
                             */
                            OffsetMapping make(MethodDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation);

                            /**
                             * Creates an offset mapping for a parameter of the method or constructor that is the delegation target.
                             *
                             * @param target     The parameter that is bound to an expression.
                             * @param annotation The annotation that was found on the parameter.
                             * @return An appropriate offset mapping.
                             */
                            OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation);

                            /**
                             * An abstract base implementation of a factory for an offset mapping.
                             *
                             * @param <S> The type of the represented annotation.
                             */
                            abstract class AbstractBase<S extends Annotation> implements OffsetMapping.Factory<S> {

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(MethodDescription.InDefinedShape target, AnnotationDescription.Loadable<S> annotation) {
                                    return make(target.getDeclaringType().asGenericType(), annotation);
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<S> annotation) {
                                    return make(target.getType(), annotation);
                                }

                                /**
                                 * Returns an offset mapping for the bound method target or parameter.
                                 *
                                 * @param target     The declaring type of a non-static method or a parameter type.
                                 * @param annotation The annotation that was found on the method or parameter.
                                 * @return An appropriate offset mapping.
                                 */
                                protected abstract OffsetMapping make(TypeDescription.Generic target, AnnotationDescription.Loadable<S> annotation);
                            }

                            /**
                             * A factory for an offset mapping that does not support binding a method target.
                             *
                             * @param <S> The type of the represented annotation.
                             */
                            abstract class WithParameterSupportOnly<S extends Annotation> implements OffsetMapping.Factory<S> {

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(MethodDescription.InDefinedShape target, AnnotationDescription.Loadable<S> annotation) {
                                    throw new UnsupportedOperationException("This factory does not support binding a method receiver");
                                }
                            }

                            /**
                             * A simple factory for an offset mapping.
                             *
                             * @param <S> The type of the represented annotation.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            class Simple<S extends Annotation> extends OffsetMapping.Factory.AbstractBase<S> {

                                /**
                                 * The type of the bound annotation.
                                 */
                                private final Class<S> annotationType;

                                /**
                                 * The offset mapping to return.
                                 */
                                private final OffsetMapping offsetMapping;

                                /**
                                 * Creates a simple factory for an offset mapping.
                                 *
                                 * @param annotationType The type of the bound annotation.
                                 * @param offsetMapping  The offset mapping to return.
                                 */
                                public Simple(Class<S> annotationType, OffsetMapping offsetMapping) {
                                    this.annotationType = annotationType;
                                    this.offsetMapping = offsetMapping;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public Class<S> getAnnotationType() {
                                    return annotationType;
                                }

                                @Override
                                protected OffsetMapping make(TypeDescription.Generic target, AnnotationDescription.Loadable<S> annotation) {
                                    return offsetMapping;
                                }
                            }
                        }

                        /**
                         * An offset mapping that resolves a given stack manipulation.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        class ForStackManipulation implements OffsetMapping {

                            /**
                             * The stack manipulation to apply.
                             */
                            private final StackManipulation stackManipulation;

                            /**
                             * The type of the value that is produced by the stack manipulation.
                             */
                            private final TypeDescription.Generic typeDescription;

                            /**
                             * The type of the parameter or method target that is bound by this mapping.
                             */
                            private final TypeDescription.Generic targetType;

                            /**
                             * Creates a new offset mapping for a stack manipulation.
                             *
                             * @param stackManipulation The stack manipulation to apply.
                             * @param typeDescription   The type of the value that is produced by the stack manipulation.
                             * @param targetType        The type of the parameter or method target that is bound by this mapping.
                             */
                            public ForStackManipulation(StackManipulation stackManipulation, TypeDescription.Generic typeDescription, TypeDescription.Generic targetType) {
                                this.targetType = targetType;
                                this.stackManipulation = stackManipulation;
                                this.typeDescription = typeDescription;
                            }

                            /**
                             * Resolves an offset mapping that binds the provided annotation type to a given constant value.
                             *
                             * @param annotationType The annotation type to bind.
                             * @param value          The constant value being bound or {@code null}.
                             * @param <S>            The type of the annotation.
                             * @return An appropriate factory for an offset mapping.
                             */
                            public static <S extends Annotation> OffsetMapping.Factory<S> of(Class<S> annotationType, @MaybeNull Object value) {
                                return value == null
                                        ? new OffsetMapping.ForStackManipulation.OfDefaultValue<S>(annotationType)
                                        : new OffsetMapping.ForStackManipulation.Factory<S>(annotationType, ConstantValue.Simple.wrap(value));
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public OffsetMapping.Resolved resolve(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                return new ForStackManipulation.Resolved(assigner, typing, stackManipulation, typeDescription, targetType);
                            }

                            /**
                             * A resolved offset mapping for a stack manipulation.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            protected static class Resolved implements OffsetMapping.Resolved {

                                /**
                                 * The assigner to use.
                                 */
                                private final Assigner assigner;

                                /**
                                 * The typing to apply.
                                 */
                                private final Assigner.Typing typing;

                                /**
                                 * The stack manipulation to apply.
                                 */
                                private final StackManipulation stackManipulation;

                                /**
                                 * The type of the value that is produced by the stack manipulation.
                                 */
                                private final TypeDescription.Generic typeDescription;

                                /**
                                 * The type of the parameter or method target that is bound by this mapping.
                                 */
                                private final TypeDescription.Generic targetType;

                                /**
                                 * Creates a resolved offset mapping for a given stack manipulation.
                                 *
                                 * @param assigner          The assigner to use.
                                 * @param typing            The typing to apply.
                                 * @param stackManipulation The stack manipulation to apply.
                                 * @param typeDescription   The type of the value that is produced by the stack manipulation.
                                 * @param targetType        The type of the parameter or method target that is bound by this mapping.
                                 */
                                protected Resolved(Assigner assigner,
                                                   Assigner.Typing typing,
                                                   StackManipulation stackManipulation,
                                                   TypeDescription.Generic typeDescription,
                                                   TypeDescription.Generic targetType) {
                                    this.assigner = assigner;
                                    this.typing = typing;
                                    this.stackManipulation = stackManipulation;
                                    this.typeDescription = typeDescription;
                                    this.targetType = targetType;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public StackManipulation apply(TypeDescription receiver,
                                                               ByteCodeElement.Member original,
                                                               TypeList.Generic parameters,
                                                               TypeDescription.Generic result,
                                                               TypeDescription.Generic current,
                                                               JavaConstant.MethodHandle methodHandle,
                                                               Map<Integer, Integer> offsets,
                                                               int offset) {
                                    StackManipulation assignment = assigner.assign(typeDescription, targetType, typing);
                                    if (!assignment.isValid()) {
                                        throw new IllegalStateException("Cannot assign " + typeDescription + " to " + targetType);
                                    }
                                    return new StackManipulation.Compound(stackManipulation, assignment);
                                }
                            }

                            /**
                             * A factory that binds the default value of the annotated parameter, i.e. {@code null} for reference types
                             * or the specific version of {@code 0} for primitive types.
                             *
                             * @param <T> The type of the annotation.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            public static class OfDefaultValue<T extends Annotation> implements OffsetMapping.Factory<T> {

                                /**
                                 * The annotation type.
                                 */
                                private final Class<T> annotationType;

                                /**
                                 * Creates a new factory for binding a default value.
                                 *
                                 * @param annotationType The annotation type.
                                 */
                                public OfDefaultValue(Class<T> annotationType) {
                                    this.annotationType = annotationType;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public Class<T> getAnnotationType() {
                                    return annotationType;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(MethodDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation) {
                                    throw new UnsupportedOperationException("This factory does not support binding a method receiver");
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation) {
                                    return new ForStackManipulation(DefaultValue.of(target.getType()), target.getType(), target.getType());
                                }
                            }

                            /**
                             * A factory that binds a given annotation property to the parameter.
                             *
                             * @param <T> The type of the annotation.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            public static class OfAnnotationProperty<T extends Annotation> extends OffsetMapping.Factory.WithParameterSupportOnly<T> {

                                /**
                                 * The annotation type.
                                 */
                                private final Class<T> annotationType;

                                /**
                                 * The annotation property to resolve.
                                 */
                                private final MethodDescription.InDefinedShape property;

                                /**
                                 * Creates a factory for assigning an annotation property to the annotated parameter.
                                 *
                                 * @param annotationType The annotation type.
                                 * @param property       The annotation property to resolve.
                                 */
                                protected OfAnnotationProperty(Class<T> annotationType, MethodDescription.InDefinedShape property) {
                                    this.annotationType = annotationType;
                                    this.property = property;
                                }

                                /**
                                 * Resolves an offset mapping factory where the provided property is assigned to any parameter that
                                 * is annotated with the given annotation.
                                 *
                                 * @param annotationType The annotation type.
                                 * @param property       The name of the property on the
                                 * @param <S>            The type of the annotation from which the property is read.
                                 * @return An appropriate factory for an offset mapping.
                                 */
                                public static <S extends Annotation> OffsetMapping.Factory<S> of(Class<S> annotationType, String property) {
                                    if (!annotationType.isAnnotation()) {
                                        throw new IllegalArgumentException("Not an annotation type: " + annotationType);
                                    }
                                    try {
                                        return new ForStackManipulation.OfAnnotationProperty<S>(annotationType, new MethodDescription.ForLoadedMethod(annotationType.getMethod(property)));
                                    } catch (NoSuchMethodException exception) {
                                        throw new IllegalArgumentException("Cannot find a property " + property + " on " + annotationType, exception);
                                    }
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public Class<T> getAnnotationType() {
                                    return annotationType;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation) {
                                    ConstantValue value = ConstantValue.Simple.wrapOrNull(annotation.getValue(property).resolve());
                                    if (value == null) {
                                        throw new IllegalStateException("Not a constant value property: " + property);
                                    }
                                    return new ForStackManipulation(value.toStackManipulation(), value.getTypeDescription().asGenericType(), target.getType());
                                }
                            }

                            /**
                             * Assigns a value to the annotated parameter that is deserialized from a given input.
                             *
                             * @param <T> The type of the annotation.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            public static class OfSerializedConstant<T extends Annotation> extends OffsetMapping.Factory.AbstractBase<T> {

                                /**
                                 * The annotation type.
                                 */
                                private final Class<T> annotationType;

                                /**
                                 * A stack manipulation that represents the deserialization.
                                 */
                                private final StackManipulation deserialization;

                                /**
                                 * A description of the type that is returned as a result of the deserialization.
                                 */
                                private final TypeDescription.Generic typeDescription;

                                /**
                                 * Creates a factory that creates an offset mapping for a value that is deserialized.
                                 *
                                 * @param annotationType  The annotation type.
                                 * @param deserialization A stack manipulation that represents the deserialization.
                                 * @param typeDescription A description of the type that is returned as a result of the deserialization.
                                 */
                                protected OfSerializedConstant(Class<T> annotationType, StackManipulation deserialization, TypeDescription.Generic typeDescription) {
                                    this.annotationType = annotationType;
                                    this.deserialization = deserialization;
                                    this.typeDescription = typeDescription;
                                }

                                /**
                                 * Creates a factory for an offset mapping that deserializes a given value that is then assigned to the annotated parameter or used as a method target.
                                 *
                                 * @param type       The annotation type.
                                 * @param value      The serialized value.
                                 * @param targetType The type of the value that is deserialized.
                                 * @param <S>        The type of the annotation.
                                 * @param <U>        The type of the serialized value.
                                 * @return An appropriate factory for an offset mapping.
                                 */
                                public static <S extends Annotation, U extends Serializable> OffsetMapping.Factory<S> of(Class<S> type, U value, Class<? super U> targetType) {
                                    if (!targetType.isInstance(value)) {
                                        throw new IllegalArgumentException(value + " is no instance of " + targetType);
                                    }
                                    return new ForStackManipulation.OfSerializedConstant<S>(type, SerializedConstant.of(value), net.bytebuddy.description.type.TypeDescription.ForLoadedType.of(targetType).asGenericType());
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public Class<T> getAnnotationType() {
                                    return annotationType;
                                }

                                @Override
                                protected OffsetMapping make(TypeDescription.Generic target, AnnotationDescription.Loadable<T> annotation) {
                                    return new ForStackManipulation(deserialization, typeDescription, target);
                                }
                            }

                            /**
                             * A factory that invokes a method dynamically and assignes the result to the annotated parameter.
                             *
                             * @param <T> The type of the annotation.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            public static class OfDynamicInvocation<T extends Annotation> extends OffsetMapping.Factory.AbstractBase<T> {

                                /**
                                 * The annotation type.
                                 */
                                private final Class<T> annotationType;

                                /**
                                 * The bootstrap method to use.
                                 */
                                private final MethodDescription.InDefinedShape bootstrapMethod;

                                /**
                                 * The constants to provide to the bootstrap method.
                                 */
                                private final List<? extends JavaConstant> arguments;

                                /**
                                 * Creates a factory for an offset mapping that assigns the result of a dynamic method invocation.
                                 *
                                 * @param annotationType  The annotation type.
                                 * @param bootstrapMethod The bootstrap method to use.
                                 * @param arguments       The constants to provide to the bootstrap method.
                                 */
                                public OfDynamicInvocation(Class<T> annotationType, MethodDescription.InDefinedShape bootstrapMethod, List<? extends JavaConstant> arguments) {
                                    this.annotationType = annotationType;
                                    this.bootstrapMethod = bootstrapMethod;
                                    this.arguments = arguments;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public Class<T> getAnnotationType() {
                                    return annotationType;
                                }

                                @Override
                                protected OffsetMapping make(TypeDescription.Generic target, AnnotationDescription.Loadable<T> annotation) {
                                    if (!target.isInterface()) {
                                        throw new IllegalArgumentException(target + " is not an interface");
                                    } else if (!target.getInterfaces().isEmpty()) {
                                        throw new IllegalArgumentException(target + " must not extend other interfaces");
                                    } else if (!target.isPublic()) {
                                        throw new IllegalArgumentException(target + " is mot public");
                                    }
                                    MethodList<?> methodCandidates = target.getDeclaredMethods().filter(isAbstract());
                                    if (methodCandidates.size() != 1) {
                                        throw new IllegalArgumentException(target + " must declare exactly one abstract method");
                                    }
                                    return new OffsetMapping.ForStackManipulation(MethodInvocation.invoke(bootstrapMethod).dynamic(methodCandidates.getOnly().getInternalName(),
                                            target.asErasure(),
                                            Collections.<TypeDescription>emptyList(),
                                            arguments), target, target);
                                }
                            }

                            /**
                             * A factory to produce an offset mapping based upon a stack manipulation..
                             *
                             * @param <T> The type of the annotation.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            public static class Factory<T extends Annotation> extends OffsetMapping.Factory.AbstractBase<T> {

                                /**
                                 * The annotation type.
                                 */
                                private final Class<T> annotationType;

                                /**
                                 * The stack manipulation that produces the assigned value.
                                 */
                                private final StackManipulation stackManipulation;

                                /**
                                 * The type of the value that is produced by the stack manipulation.
                                 */
                                private final TypeDescription.Generic typeDescription;

                                /**
                                 * Creates a factory for a given constant value.
                                 *
                                 * @param annotationType The value to assign to the parameter.
                                 * @param value          The value that is bound.
                                 */
                                public Factory(Class<T> annotationType, ConstantValue value) {
                                    this(annotationType, value.toStackManipulation(), value.getTypeDescription().asGenericType());
                                }

                                /**
                                 * Creates a factory for a given stack manipulation.
                                 *
                                 * @param annotationType    The value to assign to the parameter.
                                 * @param stackManipulation The stack manipulation that produces the assigned value.
                                 * @param typeDescription   The type of the value that is produced by the stack manipulation.
                                 */
                                public Factory(Class<T> annotationType, StackManipulation stackManipulation, TypeDescription.Generic typeDescription) {
                                    this.annotationType = annotationType;
                                    this.stackManipulation = stackManipulation;
                                    this.typeDescription = typeDescription;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public Class<T> getAnnotationType() {
                                    return annotationType;
                                }

                                @Override
                                protected OffsetMapping make(TypeDescription.Generic target, AnnotationDescription.Loadable<T> annotation) {
                                    return new ForStackManipulation(stackManipulation, typeDescription, target);
                                }
                            }
                        }

                        /**
                         * An offset mapping that assigns an argument of either the instrumented
                         * method or the substituted expression.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        class ForArgument implements OffsetMapping {

                            /**
                             * A description of the targeted type.
                             */
                            private final TypeDescription.Generic targetType;

                            /**
                             * The index of the parameter.
                             */
                            private final int index;

                            /**
                             * The typing to use or {@code null} if the global typing setting should be applied.
                             */
                            @MaybeNull
                            @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
                            private final Assigner.Typing typing;

                            /**
                             * The source providing the argument.
                             */
                            private final Source source;

                            /**
                             * {@code true} if {@code null} or a primitive {@code 0} should be assigned to the parameter
                             * if the provided index is not available.
                             */
                            private final boolean optional;

                            /**
                             * Creates a new offset mapping for an argument to either the substituted expression or the instrumented method.
                             *
                             * @param targetType A description of the targeted type.
                             * @param index      The index of the parameter.
                             * @param typing     The typing to use or {@code null} if the global typing setting should be applied.
                             * @param source     The source providing the argument.
                             * @param optional   {@code true} if {@code null} or a primitive {@code 0} should be assigned to the parameter
                             *                   if the provided index is not available.
                             */
                            public ForArgument(TypeDescription.Generic targetType, int index, @MaybeNull Assigner.Typing typing, Source source, boolean optional) {
                                this.targetType = targetType;
                                this.index = index;
                                this.typing = typing;
                                this.source = source;
                                this.optional = optional;
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public OffsetMapping.Resolved resolve(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                return new ForArgument.Resolved(targetType, index, this.typing == null ? typing : this.typing, source, optional, assigner, instrumentedMethod);
                            }

                            /**
                             * A factory for creating an offset mapping for a parameter value of either the instrumented
                             * method or the substituted element.
                             */
                            protected enum Factory implements OffsetMapping.Factory<Argument> {

                                /**
                                 * The singleton instance.
                                 */
                                INSTANCE;

                                /**
                                 * The {@link Argument#value()} property.
                                 */
                                private static final MethodDescription.InDefinedShape ARGUMENT_VALUE;

                                /**
                                 * The {@link Argument#typing()} property.
                                 */
                                private static final MethodDescription.InDefinedShape ARGUMENT_TYPING;

                                /**
                                 * The {@link Argument#source()} property.
                                 */
                                private static final MethodDescription.InDefinedShape ARGUMENT_SOURCE;

                                /**
                                 * The {@link Argument#optional()} property.
                                 */
                                private static final MethodDescription.InDefinedShape ARGUMENT_OPTIONAL;

                                /*
                                 * Resolves all annotation properties.
                                 */
                                static {
                                    MethodList<MethodDescription.InDefinedShape> methods = net.bytebuddy.description.type.TypeDescription.ForLoadedType.of(Argument.class).getDeclaredMethods();
                                    ARGUMENT_VALUE = methods.filter(named("value")).getOnly();
                                    ARGUMENT_TYPING = methods.filter(named("typing")).getOnly();
                                    ARGUMENT_SOURCE = methods.filter(named("source")).getOnly();
                                    ARGUMENT_OPTIONAL = methods.filter(named("optional")).getOnly();
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public Class<Argument> getAnnotationType() {
                                    return Argument.class;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(MethodDescription.InDefinedShape target, AnnotationDescription.Loadable<Argument> annotation) {
                                    return new ForArgument(target.getDeclaringType().asGenericType(),
                                            annotation.getValue(ARGUMENT_VALUE).resolve(Integer.class),
                                            annotation.getValue(ARGUMENT_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class),
                                            annotation.getValue(ARGUMENT_SOURCE).resolve(EnumerationDescription.class).load(Source.class),
                                            annotation.getValue(ARGUMENT_OPTIONAL).resolve(Boolean.class));
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<Argument> annotation) {
                                    int index = annotation.getValue(ARGUMENT_VALUE).resolve(Integer.class);
                                    if (index < 0) {
                                        throw new IllegalStateException("Cannot assign negative parameter index " + index + " for " + target);
                                    }
                                    return new ForArgument(target.getType(),
                                            index,
                                            annotation.getValue(ARGUMENT_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class),
                                            annotation.getValue(ARGUMENT_SOURCE).resolve(EnumerationDescription.class).load(Source.class),
                                            annotation.getValue(ARGUMENT_OPTIONAL).resolve(Boolean.class));
                                }
                            }

                            /**
                             * A resolved offset mapping to the parameter of either the instrumented method or
                             * the substituted element.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            protected static class Resolved implements OffsetMapping.Resolved {

                                /**
                                 * The targeted type.
                                 */
                                private final TypeDescription.Generic targetType;

                                /**
                                 * The index of the parameter.
                                 */
                                private final int index;

                                /**
                                 * The typing to use when assigning.
                                 */
                                private final Assigner.Typing typing;

                                /**
                                 * The source providing the argument.
                                 */
                                private final Source source;

                                /**
                                 * {@code true} if {@code null} or a primitive {@code 0} should be assigned to the parameter
                                 * if the provided index is not available.
                                 */
                                private final boolean optional;

                                /**
                                 * The assigner to use.
                                 */
                                private final Assigner assigner;

                                /**
                                 * The instrumented method.
                                 */
                                private final MethodDescription instrumentedMethod;

                                /**
                                 * Creates a resolved offset mapping for assigning a parameter.
                                 *
                                 * @param targetType         The targeted type.
                                 * @param index              The index of the parameter.
                                 * @param typing             The typing to use when assigning.
                                 * @param source             The source providing the argument.
                                 * @param optional           {@code true} if {@code null} or a primitive {@code 0} should be assigned
                                 *                           to the parameter if the provided index is not available.
                                 * @param assigner           The assigner to use.
                                 * @param instrumentedMethod The instrumented method.
                                 */
                                protected Resolved(TypeDescription.Generic targetType,
                                                   int index,
                                                   Assigner.Typing typing,
                                                   Source source,
                                                   boolean optional,
                                                   Assigner assigner,
                                                   MethodDescription instrumentedMethod) {
                                    this.targetType = targetType;
                                    this.index = index;
                                    this.typing = typing;
                                    this.source = source;
                                    this.optional = optional;
                                    this.assigner = assigner;
                                    this.instrumentedMethod = instrumentedMethod;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public StackManipulation apply(TypeDescription receiver,
                                                               ByteCodeElement.Member original,
                                                               TypeList.Generic parameters,
                                                               TypeDescription.Generic result,
                                                               TypeDescription.Generic current,
                                                               JavaConstant.MethodHandle methodHandle,
                                                               Map<Integer, Integer> offsets,
                                                               int offset) {
                                    Source.Value value = source.argument(index, parameters, offsets, original, instrumentedMethod);
                                    if (value != null) {
                                        StackManipulation assignment = assigner.assign(value.getTypeDescription(), targetType, typing);
                                        if (!assignment.isValid()) {
                                            throw new IllegalStateException("Cannot assign " + value.getTypeDescription() + " to " + targetType);
                                        }
                                        return new StackManipulation.Compound(MethodVariableAccess.of(value.getTypeDescription()).loadFrom(value.getOffset()), assignment);
                                    } else if (optional) {
                                        return DefaultValue.of(targetType);
                                    } else {
                                        throw new IllegalStateException("No argument with index " + index + " available for " + original);
                                    }
                                }
                            }
                        }

                        /**
                         * An offset mapping that assigns the {@code this} reference.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        class ForThisReference implements OffsetMapping {

                            /**
                             * The targeted type.
                             */
                            private final TypeDescription.Generic targetType;

                            /**
                             * The typing to use or {@code null} if implicit typing.
                             */
                            @MaybeNull
                            @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
                            private final Assigner.Typing typing;

                            /**
                             * The source providing the reference.
                             */
                            private final Source source;

                            /**
                             * {@code true} if {@code null} or a primitive {@code 0} should be assigned to the parameter
                             * if no {@code this} reference is available.
                             */
                            private final boolean optional;

                            /**
                             * Creates an offset mapping that resolves the {@code this} reference.
                             *
                             * @param targetType The targeted type.
                             * @param typing     The typing to use or {@code null} if implicit typing.
                             * @param source     The source providing the reference.
                             * @param optional   {@code true} if {@code null} or a primitive {@code 0} should be assigned
                             *                   to the parameter if no {@code this} reference is available.
                             */
                            public ForThisReference(TypeDescription.Generic targetType, @MaybeNull Assigner.Typing typing, Source source, boolean optional) {
                                this.targetType = targetType;
                                this.typing = typing;
                                this.source = source;
                                this.optional = optional;
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public ForThisReference.Resolved resolve(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                return new ForThisReference.Resolved(targetType, this.typing == null ? typing : this.typing, source, optional, assigner, instrumentedMethod);
                            }

                            /**
                             * A resolved offset mapping for resolving the {@code this} reference.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            protected static class Resolved implements OffsetMapping.Resolved {

                                /**
                                 * The targeted type.
                                 */
                                private final TypeDescription.Generic targetType;

                                /**
                                 * The typing to use..
                                 */
                                private final Assigner.Typing typing;

                                /**
                                 * The source providing the reference.
                                 */
                                private final Source source;

                                /**
                                 * {@code true} if {@code null} or a primitive {@code 0} should be assigned to the parameter
                                 * if no {@code this} reference is available.
                                 */
                                private final boolean optional;

                                /**
                                 * The assigner to use.
                                 */
                                private final Assigner assigner;

                                /**
                                 * The instrumented method.
                                 */
                                private final MethodDescription instrumentedMethod;

                                /**
                                 * Creates a resolved offset mapping for assigning the {@code this} reference.
                                 *
                                 * @param targetType         The targeted type.
                                 * @param typing             The typing to use.
                                 * @param source             The source providing the reference.
                                 * @param optional           {@code true} if {@code null} or a primitive {@code 0} should be assigned
                                 *                           to the parameter if no {@code this} reference is available.
                                 * @param assigner           The assigner to use.
                                 * @param instrumentedMethod The instrumented method.
                                 */
                                protected Resolved(TypeDescription.Generic targetType,
                                                   Assigner.Typing typing,
                                                   Source source,
                                                   boolean optional,
                                                   Assigner assigner,
                                                   MethodDescription instrumentedMethod) {
                                    this.targetType = targetType;
                                    this.typing = typing;
                                    this.source = source;
                                    this.optional = optional;
                                    this.assigner = assigner;
                                    this.instrumentedMethod = instrumentedMethod;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public StackManipulation apply(TypeDescription receiver,
                                                               ByteCodeElement.Member original,
                                                               TypeList.Generic parameters,
                                                               TypeDescription.Generic result,
                                                               TypeDescription.Generic current,
                                                               JavaConstant.MethodHandle methodHandle,
                                                               Map<Integer, Integer> offsets,
                                                               int offset) {
                                    Source.Value value = source.self(parameters, offsets, original, instrumentedMethod);
                                    if (value != null) {
                                        StackManipulation assignment = assigner.assign(value.getTypeDescription(), targetType, typing);
                                        if (!assignment.isValid()) {
                                            throw new IllegalStateException("Cannot assign " + value.getTypeDescription() + " to " + targetType);
                                        }
                                        return new StackManipulation.Compound(MethodVariableAccess.of(value.getTypeDescription()).loadFrom(value.getOffset()), assignment);
                                    } else if (optional) {
                                        return DefaultValue.of(targetType);
                                    } else {
                                        throw new IllegalStateException("No this reference available for " + original);
                                    }
                                }
                            }

                            /**
                             * A factory for creating an offset mapping for binding a {@link This} reference.
                             */
                            protected enum Factory implements OffsetMapping.Factory<This> {

                                /**
                                 * The singleton instance.
                                 */
                                INSTANCE;

                                /**
                                 * The {@link This#typing()} property.
                                 */
                                private static final MethodDescription.InDefinedShape THIS_TYPING;

                                /**
                                 * The {@link This#source()} reference.
                                 */
                                private static final MethodDescription.InDefinedShape THIS_SOURCE;

                                /**
                                 * The {@link This#optional()} property.
                                 */
                                private static final MethodDescription.InDefinedShape THIS_OPTIONAL;

                                /*
                                 * Resolves the annotation properties.
                                 */
                                static {
                                    MethodList<MethodDescription.InDefinedShape> methods = net.bytebuddy.description.type.TypeDescription.ForLoadedType.of(This.class).getDeclaredMethods();
                                    THIS_TYPING = methods.filter(named("typing")).getOnly();
                                    THIS_SOURCE = methods.filter(named("source")).getOnly();
                                    THIS_OPTIONAL = methods.filter(named("optional")).getOnly();
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public Class<This> getAnnotationType() {
                                    return This.class;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(MethodDescription.InDefinedShape target, AnnotationDescription.Loadable<This> annotation) {
                                    return new ForThisReference(target.getDeclaringType().asGenericType(),
                                            annotation.getValue(THIS_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class),
                                            annotation.getValue(THIS_SOURCE).resolve(EnumerationDescription.class).load(Source.class),
                                            annotation.getValue(THIS_OPTIONAL).resolve(Boolean.class));
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<This> annotation) {
                                    return new ForThisReference(target.getType(),
                                            annotation.getValue(THIS_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class),
                                            annotation.getValue(THIS_SOURCE).resolve(EnumerationDescription.class).load(Source.class),
                                            annotation.getValue(THIS_OPTIONAL).resolve(Boolean.class));
                                }
                            }
                        }

                        /**
                         * An offset mapping that assigns an array containing all arguments to the annotated parameter.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        class ForAllArguments implements OffsetMapping {

                            /**
                             * The component type of the annotated parameter.
                             */
                            private final TypeDescription.Generic targetComponentType;

                            /**
                             * The typing to use or {@code null} if implicit typing.
                             */
                            @MaybeNull
                            @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
                            private final Assigner.Typing typing;

                            /**
                             * The source providing the reference.
                             */
                            private final Source source;

                            /**
                             * {@code true} if the {@code this} reference should be included in the created array, if available.
                             */
                            private final boolean includeSelf;

                            /**
                             * {@code true} if {@code null} should be assigned to the parameter if no arguments are available.
                             */
                            private final boolean nullIfEmpty;

                            /**
                             * Creates a new offset mapping for an array containing all supplied arguments.
                             *
                             * @param targetComponentType The component type of the annotated parameter.
                             * @param typing              The typing to use or {@code null} if implicit typing.
                             * @param source              The source providing the reference.
                             * @param includeSelf         {@code true} if the {@code this} reference should be included in the created array, if available.
                             * @param nullIfEmpty         {@code true} if {@code null} should be assigned to the parameter if no arguments are available.
                             */
                            public ForAllArguments(TypeDescription.Generic targetComponentType, @MaybeNull Assigner.Typing typing, Source source, boolean includeSelf, boolean nullIfEmpty) {
                                this.targetComponentType = targetComponentType;
                                this.typing = typing;
                                this.source = source;
                                this.includeSelf = includeSelf;
                                this.nullIfEmpty = nullIfEmpty;
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public OffsetMapping.Resolved resolve(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                return new ForAllArguments.Resolved(targetComponentType, this.typing == null ? typing : this.typing, source, includeSelf, nullIfEmpty, assigner, instrumentedMethod);
                            }

                            /**
                             * A factory for creating an offset mapping containing all supplies arguments.
                             */
                            protected enum Factory implements OffsetMapping.Factory<AllArguments> {

                                /**
                                 * The singleton instance.
                                 */
                                INSTANCE;

                                /**
                                 * The {@link AllArguments#typing()} property.
                                 */
                                private static final MethodDescription.InDefinedShape ALL_ARGUMENTS_TYPING;

                                /**
                                 * The {@link AllArguments#source()} property.
                                 */
                                private static final MethodDescription.InDefinedShape ALL_ARGUMENTS_SOURCE;

                                /**
                                 * The {@link AllArguments#includeSelf()} property.
                                 */
                                private static final MethodDescription.InDefinedShape ALL_ARGUMENTS_INCLUDE_SELF;

                                /**
                                 * The {@link AllArguments#nullIfEmpty()} property.
                                 */
                                private static final MethodDescription.InDefinedShape ALL_ARGUMENTS_NULL_IF_EMPTY;

                                /*
                                 * Resolves all annotation properties.
                                 */
                                static {
                                    MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(AllArguments.class).getDeclaredMethods();
                                    ALL_ARGUMENTS_TYPING = methods.filter(named("typing")).getOnly();
                                    ALL_ARGUMENTS_SOURCE = methods.filter(named("source")).getOnly();
                                    ALL_ARGUMENTS_INCLUDE_SELF = methods.filter(named("includeSelf")).getOnly();
                                    ALL_ARGUMENTS_NULL_IF_EMPTY = methods.filter(named("nullIfEmpty")).getOnly();
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public Class<AllArguments> getAnnotationType() {
                                    return AllArguments.class;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(MethodDescription.InDefinedShape target, AnnotationDescription.Loadable<AllArguments> annotation) {
                                    throw new UnsupportedOperationException("This factory does not support binding a method receiver");
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming component type for array type.")
                                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<AllArguments> annotation) {
                                    if (!target.getType().isArray()) {
                                        throw new IllegalStateException("Expected array as parameter type for " + target);
                                    }
                                    return new ForAllArguments(target.getType().getComponentType(),
                                            annotation.getValue(ALL_ARGUMENTS_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class),
                                            annotation.getValue(ALL_ARGUMENTS_SOURCE).resolve(EnumerationDescription.class).load(Source.class),
                                            annotation.getValue(ALL_ARGUMENTS_INCLUDE_SELF).resolve(Boolean.class),
                                            annotation.getValue(ALL_ARGUMENTS_NULL_IF_EMPTY).resolve(Boolean.class));
                                }
                            }

                            /**
                             * A resolves offset mapping for an array containing all arguments.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            protected static class Resolved implements OffsetMapping.Resolved {

                                /**
                                 * The component type of the annotated parameter.
                                 */
                                private final TypeDescription.Generic targetComponentType;

                                /**
                                 * The typing to use.
                                 */
                                private final Assigner.Typing typing;

                                /**
                                 * The source providing the reference.
                                 */
                                private final Source source;

                                /**
                                 * {@code true} if the {@code this} reference should be included in the created array, if available.
                                 */
                                private final boolean includeSelf;

                                /**
                                 * {@code true} if {@code null} should be assigned to the parameter if no arguments are available.
                                 */
                                private final boolean nullIfEmpty;

                                /**
                                 * The assigner to use.
                                 */
                                private final Assigner assigner;

                                /**
                                 * The instrumented method.
                                 */
                                private final MethodDescription instrumentedMethod;

                                /**
                                 * Creates a resolved version for an offset mapping of all arguments.
                                 *
                                 * @param targetComponentType The component type of the annotated parameter.
                                 * @param typing              The typing to use.
                                 * @param source              The source providing the reference.
                                 * @param includeSelf         {@code true} if the {@code this} reference should be included in the created array, if available.
                                 * @param nullIfEmpty         {@code true} if {@code null} should be assigned to the parameter if no arguments are available.
                                 * @param assigner            The assigner to use.
                                 * @param instrumentedMethod  The instrumented method.
                                 */
                                protected Resolved(TypeDescription.Generic targetComponentType,
                                                   Assigner.Typing typing,
                                                   Source source,
                                                   boolean includeSelf,
                                                   boolean nullIfEmpty,
                                                   Assigner assigner,
                                                   MethodDescription instrumentedMethod) {
                                    this.targetComponentType = targetComponentType;
                                    this.typing = typing;
                                    this.source = source;
                                    this.includeSelf = includeSelf;
                                    this.nullIfEmpty = nullIfEmpty;
                                    this.assigner = assigner;
                                    this.instrumentedMethod = instrumentedMethod;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public StackManipulation apply(TypeDescription receiver,
                                                               ByteCodeElement.Member original,
                                                               TypeList.Generic parameters,
                                                               TypeDescription.Generic result,
                                                               TypeDescription.Generic current,
                                                               JavaConstant.MethodHandle methodHandle,
                                                               Map<Integer, Integer> offsets,
                                                               int offset) {
                                    List<Source.Value> values = source.arguments(includeSelf, parameters, offsets, original, instrumentedMethod);
                                    if (nullIfEmpty && values.isEmpty()) {
                                        return NullConstant.INSTANCE;
                                    } else {
                                        List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>();
                                        for (Source.Value value : values) {
                                            StackManipulation assignment = assigner.assign(value.getTypeDescription(), targetComponentType, typing);
                                            if (!assignment.isValid()) {
                                                throw new IllegalStateException("Cannot assign " + value.getTypeDescription() + " to " + targetComponentType);
                                            }
                                            stackManipulations.add(new StackManipulation.Compound(MethodVariableAccess.of(value.getTypeDescription()).loadFrom(value.getOffset()), assignment));
                                        }
                                        return ArrayFactory.forType(targetComponentType).withValues(stackManipulations);
                                    }
                                }
                            }
                        }

                        /**
                         * An offset mapping resolving a method handle to invoke the original expression or the instrumented method.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        class ForSelfCallHandle implements OffsetMapping {

                            /**
                             * The source providing the reference.
                             */
                            private final Source source;

                            /**
                             * {@code true} if the handle should be bound to the original arguments.
                             */
                            private final boolean bound;

                            /**
                             * Creates a new offset mapping for a self call handle.
                             *
                             * @param source The source providing the reference.
                             * @param bound  {@code true} if the handle should be bound to the original arguments.
                             */
                            public ForSelfCallHandle(Source source, boolean bound) {
                                this.source = source;
                                this.bound = bound;
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public OffsetMapping.Resolved resolve(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                return bound ? new ForSelfCallHandle.Bound(source, instrumentedMethod) : new ForSelfCallHandle.Unbound(source, instrumentedMethod);
                            }

                            /**
                             * A factory for creating an offset mapping for binding a self call handle.
                             */
                            protected enum Factory implements OffsetMapping.Factory<SelfCallHandle> {

                                /**
                                 * The singleton instance.
                                 */
                                INSTANCE;

                                /**
                                 * The {@link SelfCallHandle#source()} property.
                                 */
                                private static final MethodDescription.InDefinedShape ALL_ARGUMENTS_SOURCE;

                                /**
                                 * The {@link SelfCallHandle#bound()} property.
                                 */
                                private static final MethodDescription.InDefinedShape ALL_ARGUMENTS_BOUND;

                                /*
                                 * Resolves all annotation properties.
                                 */
                                static {
                                    MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(SelfCallHandle.class).getDeclaredMethods();
                                    ALL_ARGUMENTS_SOURCE = methods.filter(named("source")).getOnly();
                                    ALL_ARGUMENTS_BOUND = methods.filter(named("bound")).getOnly();
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public Class<SelfCallHandle> getAnnotationType() {
                                    return SelfCallHandle.class;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(MethodDescription.InDefinedShape target, AnnotationDescription.Loadable<SelfCallHandle> annotation) {
                                    throw new UnsupportedOperationException("This factory does not support binding a method receiver");
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<SelfCallHandle> annotation) {
                                    if (!target.getType().asErasure().isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())) {
                                        throw new IllegalStateException("Cannot assign method handle to " + target);
                                    }
                                    return new ForSelfCallHandle(
                                            annotation.getValue(ALL_ARGUMENTS_SOURCE).resolve(EnumerationDescription.class).load(Source.class),
                                            annotation.getValue(ALL_ARGUMENTS_BOUND).resolve(Boolean.class));
                                }
                            }

                            /**
                             * Resolves a bound self call handle for an offset mapping.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            protected static class Bound implements OffsetMapping.Resolved {

                                /**
                                 * The source providing the reference.
                                 */
                                private final Source source;

                                /**
                                 * The instrumented method.
                                 */
                                private final MethodDescription instrumentedMethod;

                                /**
                                 * Creates an offset mapping for a bound version of a self call handle.
                                 *
                                 * @param source             The source providing the reference.
                                 * @param instrumentedMethod The instrumented method.
                                 */
                                protected Bound(Source source, MethodDescription instrumentedMethod) {
                                    this.source = source;
                                    this.instrumentedMethod = instrumentedMethod;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public StackManipulation apply(TypeDescription receiver,
                                                               ByteCodeElement.Member original,
                                                               TypeList.Generic parameters,
                                                               TypeDescription.Generic result,
                                                               TypeDescription.Generic current,
                                                               JavaConstant.MethodHandle methodHandle,
                                                               Map<Integer, Integer> offsets,
                                                               int offset) {
                                    Source.Value dispatched = source.self(parameters, offsets, original, instrumentedMethod);
                                    List<Source.Value> values = source.arguments(false, parameters, offsets, original, instrumentedMethod);
                                    List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(1 + (values.size()
                                            + (dispatched == null ? 0 : 2))
                                            + (values.isEmpty() ? 0 : 1));
                                    stackManipulations.add(source.handle(methodHandle, instrumentedMethod).toStackManipulation());
                                    if (dispatched != null) {
                                        stackManipulations.add(MethodVariableAccess.of(dispatched.getTypeDescription()).loadFrom(dispatched.getOffset()));
                                        stackManipulations.add(MethodInvocation.invoke(new MethodDescription.Latent(JavaType.METHOD_HANDLE.getTypeStub(), new MethodDescription.Token("bindTo",
                                                Opcodes.ACC_PUBLIC,
                                                JavaType.METHOD_HANDLE.getTypeStub().asGenericType(),
                                                new TypeList.Generic.Explicit(TypeDefinition.Sort.describe(Object.class))))));
                                    }
                                    if (!values.isEmpty()) {
                                        for (Source.Value value : values) {
                                            stackManipulations.add(MethodVariableAccess.of(value.getTypeDescription()).loadFrom(value.getOffset()));
                                        }
                                        stackManipulations.add(MethodInvocation.invoke(new MethodDescription.Latent(JavaType.METHOD_HANDLES.getTypeStub(), new MethodDescription.Token("insertArguments",
                                                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                                                JavaType.METHOD_HANDLE.getTypeStub().asGenericType(),
                                                new TypeList.Generic.Explicit(JavaType.METHOD_HANDLE.getTypeStub(), TypeDefinition.Sort.describe(int.class), TypeDefinition.Sort.describe(Object[].class))))));
                                    }
                                    return new StackManipulation.Compound(stackManipulations);
                                }
                            }

                            /**
                             * Resolves an unbound self call handle for an offset mapping.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            protected static class Unbound implements OffsetMapping.Resolved {

                                /**
                                 * The source providing the reference.
                                 */
                                private final Source source;

                                /**
                                 * The instrumented method.
                                 */
                                private final MethodDescription instrumentedMethod;

                                /**
                                 * Creates an offset mapping for an unbound version of a self call handle.
                                 *
                                 * @param source             The source providing the reference.
                                 * @param instrumentedMethod The instrumented method.
                                 */
                                protected Unbound(Source source, MethodDescription instrumentedMethod) {
                                    this.source = source;
                                    this.instrumentedMethod = instrumentedMethod;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public StackManipulation apply(TypeDescription receiver,
                                                               ByteCodeElement.Member original,
                                                               TypeList.Generic parameters,
                                                               TypeDescription.Generic result,
                                                               TypeDescription.Generic current,
                                                               JavaConstant.MethodHandle methodHandle,
                                                               Map<Integer, Integer> offsets,
                                                               int offset) {
                                    return source.handle(methodHandle, instrumentedMethod).toStackManipulation();
                                }
                            }
                        }

                        /**
                         * An offset mapping for a field value.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        abstract class ForField implements OffsetMapping {

                            /**
                             * The {@link FieldValue#value()} property.
                             */
                            private static final MethodDescription.InDefinedShape FIELD_VALUE_VALUE;

                            /**
                             * The {@link FieldValue#declaringType()} property.
                             */
                            private static final MethodDescription.InDefinedShape FIELD_VALUE_DECLARING_TYPE;

                            /**
                             * The {@link FieldValue#typing()} property.
                             */
                            private static final MethodDescription.InDefinedShape FIELD_VALUE_TYPING;

                            /*
                             * Resolves all annotation properties.
                             */
                            static {
                                MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(FieldValue.class).getDeclaredMethods();
                                FIELD_VALUE_VALUE = methods.filter(named("value")).getOnly();
                                FIELD_VALUE_DECLARING_TYPE = methods.filter(named("declaringType")).getOnly();
                                FIELD_VALUE_TYPING = methods.filter(named("typing")).getOnly();
                            }

                            /**
                             * A description of the targeted type.
                             */
                            private final TypeDescription.Generic target;

                            /**
                             * The typing to use or {@code null} if implicit typing.
                             */
                            @MaybeNull
                            @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
                            private final Assigner.Typing typing;

                            /**
                             * Creates an offset mapping for a field value.
                             *
                             * @param target A description of the targeted type.
                             * @param typing The typing to use or {@code null} if implicit typing.
                             */
                            protected ForField(TypeDescription.Generic target, @MaybeNull Assigner.Typing typing) {
                                this.target = target;
                                this.typing = typing;
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public OffsetMapping.Resolved resolve(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                FieldDescription fieldDescription = resolve(instrumentedType, instrumentedMethod);
                                if (!fieldDescription.isStatic() && instrumentedMethod.isStatic()) {
                                    throw new IllegalStateException("Cannot access non-static field " + fieldDescription + " from static method " + instrumentedMethod);
                                }
                                StackManipulation assignment = assigner.assign(fieldDescription.getType(), target, this.typing == null ? typing : this.typing);
                                if (!assignment.isValid()) {
                                    throw new IllegalStateException("Cannot assign " + fieldDescription + " to " + target);
                                }
                                return new OffsetMapping.Resolved.ForStackManipulation(new StackManipulation.Compound(fieldDescription.isStatic()
                                        ? StackManipulation.Trivial.INSTANCE
                                        : MethodVariableAccess.loadThis(), FieldAccess.forField(fieldDescription).read(), assignment));
                            }

                            /**
                             * Resolves a description of the field being accessed.
                             *
                             * @param instrumentedType   The instrumented type.
                             * @param instrumentedMethod The instrumented method.
                             * @return A description of the field being accessed.
                             */
                            protected abstract FieldDescription resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod);

                            /**
                             * An offset mapping for an unresolved field value.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            public abstract static class Unresolved extends ForField {

                                /**
                                 * Indicates that the name of the field should be inferred from the instrumented method's name as a bean property.
                                 */
                                protected static final String BEAN_PROPERTY = "";

                                /**
                                 * The name of the field being accessed or an empty string if the name of the field should be inferred.
                                 */
                                private final String name;

                                /**
                                 * Creates an offset mapping for the value of an unresolved field.
                                 *
                                 * @param target A description of the targeted type.
                                 * @param typing The typing to use.
                                 * @param name   The name of the field being accessed or an empty string if the name of the field should be inferred.
                                 */
                                protected Unresolved(TypeDescription.Generic target, Assigner.Typing typing, String name) {
                                    super(target, typing);
                                    this.name = name;
                                }

                                @Override
                                protected FieldDescription resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                    FieldLocator locator = fieldLocator(instrumentedType);
                                    FieldLocator.Resolution resolution = name.equals(BEAN_PROPERTY)
                                            ? FieldLocator.Resolution.Simple.ofBeanAccessor(locator, instrumentedMethod)
                                            : locator.locate(name);
                                    if (!resolution.isResolved()) {
                                        throw new IllegalStateException("Cannot locate field named " + name + " for " + instrumentedType);
                                    } else {
                                        return resolution.getField();
                                    }
                                }

                                /**
                                 * Creates a field locator for the instrumented type.
                                 *
                                 * @param instrumentedType The instrumented type.
                                 * @return An appropriate field locator.
                                 */
                                protected abstract FieldLocator fieldLocator(TypeDescription instrumentedType);

                                /**
                                 * An offset mapping for an unresolved field with an implicit declaring type.
                                 */
                                public static class WithImplicitType extends Unresolved {

                                    /**
                                     * Creates an offset mapping for an unresolved field value with an implicit declaring type.
                                     *
                                     * @param target     A description of the targeted type.
                                     * @param annotation The annotation describing the access.
                                     */
                                    protected WithImplicitType(TypeDescription.Generic target, AnnotationDescription.Loadable<FieldValue> annotation) {
                                        this(target,
                                                annotation.getValue(FIELD_VALUE_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class),
                                                annotation.getValue(FIELD_VALUE_VALUE).resolve(String.class));
                                    }

                                    /**
                                     * Creates an offset mapping for the value of an unresolved field with an implicit declaring type.
                                     *
                                     * @param target A description of the targeted type.
                                     * @param typing The typing to use.
                                     * @param name   The name of the field being accessed or an empty string if the name of the field should be inferred.
                                     */
                                    public WithImplicitType(TypeDescription.Generic target, Assigner.Typing typing, String name) {
                                        super(target, typing, name);
                                    }

                                    @Override
                                    protected FieldLocator fieldLocator(TypeDescription instrumentedType) {
                                        return new FieldLocator.ForClassHierarchy(instrumentedType);
                                    }
                                }

                                /**
                                 * An offset mapping for an unresolved field value with an explicit declaring type.
                                 */
                                @HashCodeAndEqualsPlugin.Enhance
                                public static class WithExplicitType extends Unresolved {

                                    /**
                                     * The field's declaring type.
                                     */
                                    private final TypeDescription declaringType;

                                    /**
                                     * Creates an offset mapping for the value of an unresolved field with an explicit declaring type.
                                     *
                                     * @param target        A description of the targeted type.
                                     * @param annotation    The annotation describing the field access.
                                     * @param declaringType The field's declaring type.
                                     */
                                    protected WithExplicitType(TypeDescription.Generic target, AnnotationDescription.Loadable<FieldValue> annotation, TypeDescription declaringType) {
                                        this(target,
                                                annotation.getValue(FIELD_VALUE_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class),
                                                annotation.getValue(FIELD_VALUE_VALUE).resolve(String.class),
                                                declaringType);
                                    }

                                    /**
                                     * Creates an offset mapping for the value of an unresolved field with an explicit declaring type.
                                     *
                                     * @param target        A description of the targeted type.
                                     * @param typing        The typing to use.
                                     * @param name          The name of the field being accessed or an empty string if the name of the field should be inferred.
                                     * @param declaringType The field's declaring type.
                                     */
                                    public WithExplicitType(TypeDescription.Generic target, Assigner.Typing typing, String name, TypeDescription declaringType) {
                                        super(target, typing, name);
                                        this.declaringType = declaringType;
                                    }

                                    @Override
                                    protected FieldLocator fieldLocator(TypeDescription instrumentedType) {
                                        if (!declaringType.represents(TargetType.class) && !instrumentedType.isAssignableTo(declaringType)) {
                                            throw new IllegalStateException(declaringType + " is no super type of " + instrumentedType);
                                        }
                                        return new FieldLocator.ForExactType(TargetType.resolve(declaringType, instrumentedType));
                                    }
                                }

                                /**
                                 * A factory for creating an offset mapping for a field value.
                                 */
                                protected enum Factory implements OffsetMapping.Factory<FieldValue> {

                                    /**
                                     * The singleton instance.
                                     */
                                    INSTANCE;

                                    /**
                                     * {@inheritDoc}
                                     */
                                    public Class<FieldValue> getAnnotationType() {
                                        return FieldValue.class;
                                    }

                                    /**
                                     * {@inheritDoc}
                                     */
                                    public OffsetMapping make(MethodDescription.InDefinedShape target, AnnotationDescription.Loadable<FieldValue> annotation) {
                                        TypeDescription declaringType = annotation.getValue(FIELD_VALUE_DECLARING_TYPE).resolve(TypeDescription.class);
                                        return declaringType.represents(void.class)
                                                ? new Unresolved.WithImplicitType(target.getDeclaringType().asGenericType(), annotation)
                                                : new Unresolved.WithExplicitType(target.getDeclaringType().asGenericType(), annotation, declaringType);
                                    }

                                    /**
                                     * {@inheritDoc}
                                     */
                                    public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<FieldValue> annotation) {
                                        TypeDescription declaringType = annotation.getValue(FIELD_VALUE_DECLARING_TYPE).resolve(TypeDescription.class);
                                        return declaringType.represents(void.class)
                                                ? new Unresolved.WithImplicitType(target.getType(), annotation)
                                                : new Unresolved.WithExplicitType(target.getType(), annotation, declaringType);
                                    }
                                }
                            }

                            /**
                             * An offset mapping for a resolved field access.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            public static class Resolved extends ForField {

                                /**
                                 * A description of the field being accessed.
                                 */
                                private final FieldDescription fieldDescription;

                                /**
                                 * Creates a resolved offset mapping for a field access.
                                 *
                                 * @param target           A description of the targeted type.
                                 * @param typing           The typing to use or {@code null} if implicit typing.
                                 * @param fieldDescription A description of the field accessed.
                                 */
                                public Resolved(TypeDescription.Generic target, Assigner.Typing typing, FieldDescription fieldDescription) {
                                    super(target, typing);
                                    this.fieldDescription = fieldDescription;
                                }

                                @Override
                                @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming declaring type for type member.")
                                protected FieldDescription resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                    if (!fieldDescription.isStatic() && !fieldDescription.getDeclaringType().asErasure().isAssignableFrom(instrumentedType)) {
                                        throw new IllegalStateException(fieldDescription + " is no member of " + instrumentedType);
                                    } else if (!fieldDescription.isVisibleTo(instrumentedType)) {
                                        throw new IllegalStateException("Cannot access " + fieldDescription + " from " + instrumentedType);
                                    }
                                    return fieldDescription;
                                }

                                /**
                                 * A factory for creating a resolved offset mapping of a field value.
                                 *
                                 * @param <T> The type of the annotation.
                                 */
                                @HashCodeAndEqualsPlugin.Enhance
                                public static class Factory<T extends Annotation> extends OffsetMapping.Factory.AbstractBase<T> {

                                    /**
                                     * The annotation type.
                                     */
                                    private final Class<T> annotationType;

                                    /**
                                     * The field being accessed.
                                     */
                                    private final FieldDescription fieldDescription;

                                    /**
                                     * The typing to use.
                                     */
                                    private final Assigner.Typing typing;

                                    /**
                                     * Creates a factory for reading a given field.
                                     *
                                     * @param annotationType   The annotation type.
                                     * @param fieldDescription The field being accessed.
                                     */
                                    public Factory(Class<T> annotationType, FieldDescription fieldDescription) {
                                        this(annotationType, fieldDescription, Assigner.Typing.STATIC);
                                    }

                                    /**
                                     * Creates a factory for reading a given field.
                                     *
                                     * @param annotationType   The annotation type.
                                     * @param fieldDescription The field being accessed.
                                     * @param typing           The typing to use.
                                     */
                                    public Factory(Class<T> annotationType, FieldDescription fieldDescription, Assigner.Typing typing) {
                                        this.annotationType = annotationType;
                                        this.fieldDescription = fieldDescription;
                                        this.typing = typing;
                                    }

                                    /**
                                     * {@inheritDoc}
                                     */
                                    public Class<T> getAnnotationType() {
                                        return annotationType;
                                    }

                                    @Override
                                    protected OffsetMapping make(TypeDescription.Generic target, AnnotationDescription.Loadable<T> annotation) {
                                        return new ForField.Resolved(target, typing, fieldDescription);
                                    }
                                }
                            }
                        }

                        /**
                         * An offset mapping for a method handle representing a field getter or setter.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        abstract class ForFieldHandle implements OffsetMapping {

                            /**
                             * The type of access to the field.
                             */
                            private final Access access;

                            /**
                             * Creates an offset mapping for a field getter or setter.
                             *
                             * @param access The type of access to the field.
                             */
                            protected ForFieldHandle(Access access) {
                                this.access = access;
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public OffsetMapping.Resolved resolve(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                FieldDescription fieldDescription = resolve(instrumentedType, instrumentedMethod);
                                if (!fieldDescription.isStatic() && instrumentedMethod.isStatic()) {
                                    throw new IllegalStateException("Cannot access non-static field " + fieldDescription + " from static method " + instrumentedMethod);
                                }
                                if (fieldDescription.isStatic()) {
                                    return new OffsetMapping.Resolved.ForStackManipulation(access.resolve(fieldDescription.asDefined()).toStackManipulation());
                                } else {
                                    return new OffsetMapping.Resolved.ForStackManipulation(new StackManipulation.Compound(
                                            access.resolve(fieldDescription.asDefined()).toStackManipulation(), MethodVariableAccess.REFERENCE.loadFrom(THIS_REFERENCE),
                                            MethodInvocation.invoke(new MethodDescription.Latent(JavaType.METHOD_HANDLE.getTypeStub(), new MethodDescription.Token("bindTo",
                                                    Opcodes.ACC_PUBLIC,
                                                    JavaType.METHOD_HANDLE.getTypeStub().asGenericType(),
                                                    new TypeList.Generic.Explicit(TypeDefinition.Sort.describe(Object.class)))))));
                                }
                            }

                            /**
                             * Resolves a description of the field being accessed.
                             *
                             * @param instrumentedType   The instrumented type.
                             * @param instrumentedMethod The instrumented method.
                             * @return A description of the field being accessed.
                             */
                            protected abstract FieldDescription resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod);

                            /**
                             * The type of access to the field.
                             */
                            public enum Access {

                                /**
                                 * Describes a field getter.
                                 */
                                GETTER {
                                    @Override
                                    protected JavaConstant.MethodHandle resolve(FieldDescription.InDefinedShape fieldDescription) {
                                        return JavaConstant.MethodHandle.ofGetter(fieldDescription);
                                    }
                                },

                                /**
                                 * Describes a field setter.
                                 */
                                SETTER {
                                    @Override
                                    protected JavaConstant.MethodHandle resolve(FieldDescription.InDefinedShape fieldDescription) {
                                        return JavaConstant.MethodHandle.ofSetter(fieldDescription);
                                    }
                                };

                                /**
                                 * Resolves a handle for the represented field access.
                                 *
                                 * @param fieldDescription The field that is being accessed.
                                 * @return An appropriate method handle.
                                 */
                                protected abstract JavaConstant.MethodHandle resolve(FieldDescription.InDefinedShape fieldDescription);
                            }

                            /**
                             * An offset mapping for an unresolved field handle.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            public abstract static class Unresolved extends ForFieldHandle {

                                /**
                                 * Indicates that the field's name should be resolved as a bean property.
                                 */
                                protected static final String BEAN_PROPERTY = "";

                                /**
                                 * The name of the field or an empty string if the name should be resolved from the instrumented method.
                                 */
                                private final String name;

                                /**
                                 * Creates an offset mapping for an unresolved field handle.
                                 *
                                 * @param access The type of access to the field.
                                 * @param name   The name of the field or an empty string if the name should be resolved from the instrumented method.
                                 */
                                public Unresolved(Access access, String name) {
                                    super(access);
                                    this.name = name;
                                }

                                @Override
                                protected FieldDescription resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                    FieldLocator locator = fieldLocator(instrumentedType);
                                    FieldLocator.Resolution resolution = name.equals(BEAN_PROPERTY)
                                            ? FieldLocator.Resolution.Simple.ofBeanAccessor(locator, instrumentedMethod)
                                            : locator.locate(name);
                                    if (!resolution.isResolved()) {
                                        throw new IllegalStateException("Cannot locate field named " + name + " for " + instrumentedType);
                                    } else {
                                        return resolution.getField();
                                    }
                                }

                                /**
                                 * Resolves a field locator for the instrumented type.
                                 *
                                 * @param instrumentedType The instrumented type.
                                 * @return Returns an appropriate field locator.
                                 */
                                protected abstract FieldLocator fieldLocator(TypeDescription instrumentedType);

                                /**
                                 * An offset mapping for an unresolved field handle with an implicit declaring type.
                                 */
                                public static class WithImplicitType extends Unresolved {

                                    /**
                                     * Creates an offset mapping for an unresolved field handle with an implicit declaring type.
                                     *
                                     * @param access The type of access to the field.
                                     * @param name   The name of the field or an empty string if the name should be resolved from the instrumented method.
                                     */
                                    public WithImplicitType(Access access, String name) {
                                        super(access, name);
                                    }

                                    @Override
                                    protected FieldLocator fieldLocator(TypeDescription instrumentedType) {
                                        return new FieldLocator.ForClassHierarchy(instrumentedType);
                                    }
                                }

                                /**
                                 * An offset mapping for an unresolved field handle with an explicit declaring type.
                                 */
                                @HashCodeAndEqualsPlugin.Enhance
                                public static class WithExplicitType extends Unresolved {

                                    /**
                                     * The field's declaring type.
                                     */
                                    private final TypeDescription declaringType;

                                    /**
                                     * Creates an offset mapping for an unresolved field handle with an explicit declaring type.
                                     *
                                     * @param access        The type of access to the field.
                                     * @param name          The name of the field or an empty string if the name should be resolved from the instrumented method.
                                     * @param declaringType The field's declaring type.
                                     */
                                    public WithExplicitType(Access access, String name, TypeDescription declaringType) {
                                        super(access, name);
                                        this.declaringType = declaringType;
                                    }

                                    @Override
                                    protected FieldLocator fieldLocator(TypeDescription instrumentedType) {
                                        if (!declaringType.represents(TargetType.class) && !instrumentedType.isAssignableTo(declaringType)) {
                                            throw new IllegalStateException(declaringType + " is no super type of " + instrumentedType);
                                        }
                                        return new FieldLocator.ForExactType(TargetType.resolve(declaringType, instrumentedType));
                                    }
                                }

                                /**
                                 * A factory for creating a method handle representing a getter for the targeted field.
                                 */
                                protected enum GetterFactory implements OffsetMapping.Factory<FieldGetterHandle> {

                                    /**
                                     * The singleton instance.
                                     */
                                    INSTANCE;

                                    /**
                                     * The {@link FieldGetterHandle#value()} method.
                                     */
                                    private static final MethodDescription.InDefinedShape FIELD_GETTER_HANDLE_VALUE;

                                    /**
                                     * The {@link FieldGetterHandle#declaringType()} method.
                                     */
                                    private static final MethodDescription.InDefinedShape FIELD_GETTER_HANDLE_DECLARING_TYPE;

                                    /*
                                     * Resolves all annotation properties.
                                     */
                                    static {
                                        MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(FieldGetterHandle.class).getDeclaredMethods();
                                        FIELD_GETTER_HANDLE_VALUE = methods.filter(named("value")).getOnly();
                                        FIELD_GETTER_HANDLE_DECLARING_TYPE = methods.filter(named("declaringType")).getOnly();
                                    }

                                    /**
                                     * {@inheritDoc}
                                     */
                                    public Class<FieldGetterHandle> getAnnotationType() {
                                        return FieldGetterHandle.class;
                                    }

                                    /**
                                     * {@inheritDoc}
                                     */
                                    public OffsetMapping make(MethodDescription.InDefinedShape target, AnnotationDescription.Loadable<FieldGetterHandle> annotation) {
                                        throw new UnsupportedOperationException("This factory does not support binding a method receiver");
                                    }

                                    /**
                                     * {@inheritDoc}
                                     */
                                    public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<FieldGetterHandle> annotation) {
                                        if (!target.getType().asErasure().isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())) {
                                            throw new IllegalStateException("Cannot assign method handle to " + target);
                                        }
                                        TypeDescription declaringType = annotation.getValue(FIELD_GETTER_HANDLE_DECLARING_TYPE).resolve(TypeDescription.class);
                                        return declaringType.represents(void.class)
                                                ? new ForFieldHandle.Unresolved.WithImplicitType(Access.GETTER, annotation.getValue(FIELD_GETTER_HANDLE_VALUE).resolve(String.class))
                                                : new ForFieldHandle.Unresolved.WithExplicitType(Access.GETTER, annotation.getValue(FIELD_GETTER_HANDLE_VALUE).resolve(String.class), declaringType);
                                    }
                                }

                                /**
                                 * A factory for creating a method handle representing a setter for the targeted field.
                                 */
                                protected enum SetterFactory implements OffsetMapping.Factory<FieldSetterHandle> {

                                    /**
                                     * The singleton instance.
                                     */
                                    INSTANCE;

                                    /**
                                     * The {@link FieldGetterHandle#value()} method.
                                     */
                                    private static final MethodDescription.InDefinedShape FIELD_SETTER_HANDLE_VALUE;

                                    /**
                                     * The {@link FieldGetterHandle#declaringType()} method.
                                     */
                                    private static final MethodDescription.InDefinedShape FIELD_SETTER_HANDLE_DECLARING_TYPE;

                                    /*
                                     * Resolves the annotation properties.
                                     */
                                    static {
                                        MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(FieldSetterHandle.class).getDeclaredMethods();
                                        FIELD_SETTER_HANDLE_VALUE = methods.filter(named("value")).getOnly();
                                        FIELD_SETTER_HANDLE_DECLARING_TYPE = methods.filter(named("declaringType")).getOnly();
                                    }

                                    /**
                                     * {@inheritDoc}
                                     */
                                    public Class<FieldSetterHandle> getAnnotationType() {
                                        return FieldSetterHandle.class;
                                    }

                                    /**
                                     * {@inheritDoc}
                                     */
                                    public OffsetMapping make(MethodDescription.InDefinedShape target, AnnotationDescription.Loadable<FieldSetterHandle> annotation) {
                                        throw new UnsupportedOperationException("This factory does not support binding a method receiver");
                                    }

                                    /**
                                     * {@inheritDoc}
                                     */
                                    public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<FieldSetterHandle> annotation) {
                                        if (!target.getType().asErasure().isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())) {
                                            throw new IllegalStateException("Cannot assign method handle to " + target);
                                        }
                                        TypeDescription declaringType = annotation.getValue(FIELD_SETTER_HANDLE_DECLARING_TYPE).resolve(TypeDescription.class);
                                        return declaringType.represents(void.class)
                                                ? new ForFieldHandle.Unresolved.WithImplicitType(Access.SETTER, annotation.getValue(FIELD_SETTER_HANDLE_VALUE).resolve(String.class))
                                                : new ForFieldHandle.Unresolved.WithExplicitType(Access.SETTER, annotation.getValue(FIELD_SETTER_HANDLE_VALUE).resolve(String.class), declaringType);
                                    }
                                }
                            }

                            /**
                             * An offset mapping for a resolved field handle.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            public static class Resolved extends OffsetMapping.ForFieldHandle {

                                /**
                                 * The field that is being accessed.
                                 */
                                private final FieldDescription fieldDescription;

                                /**
                                 * Creates a resolved mapping for a field access handle.
                                 *
                                 * @param access           The type of access.
                                 * @param fieldDescription The field that is being accessed.
                                 */
                                public Resolved(Access access, FieldDescription fieldDescription) {
                                    super(access);
                                    this.fieldDescription = fieldDescription;
                                }

                                @Override
                                @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming declaring type for type member.")
                                protected FieldDescription resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                    if (!fieldDescription.isStatic() && !fieldDescription.getDeclaringType().asErasure().isAssignableFrom(instrumentedType)) {
                                        throw new IllegalStateException(fieldDescription + " is no member of " + instrumentedType);
                                    } else if (!fieldDescription.isVisibleTo(instrumentedType)) {
                                        throw new IllegalStateException("Cannot access " + fieldDescription + " from " + instrumentedType);
                                    }
                                    return fieldDescription;
                                }

                                /**
                                 * A factory to create an offset mapping for a resolved field handle.
                                 *
                                 * @param <T> The type of the annotation.
                                 */
                                @HashCodeAndEqualsPlugin.Enhance
                                public static class Factory<T extends Annotation> implements OffsetMapping.Factory<T> {

                                    /**
                                     * The annotation type.
                                     */
                                    private final Class<T> annotationType;

                                    /**
                                     * The field being accessed.
                                     */
                                    private final FieldDescription fieldDescription;

                                    /**
                                     * The type of access.
                                     */
                                    private final Access access;

                                    /**
                                     * Creates a new factory for a field access handle.
                                     *
                                     * @param annotationType   The annotation type.
                                     * @param fieldDescription The field being accessed.
                                     * @param access           The type of access.
                                     */
                                    public Factory(Class<T> annotationType, FieldDescription fieldDescription, Access access) {
                                        this.annotationType = annotationType;
                                        this.fieldDescription = fieldDescription;
                                        this.access = access;
                                    }

                                    /**
                                     * {@inheritDoc}
                                     */
                                    public Class<T> getAnnotationType() {
                                        return annotationType;
                                    }

                                    /**
                                     * {@inheritDoc}
                                     */
                                    public OffsetMapping make(MethodDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation) {
                                        throw new UnsupportedOperationException("This factory does not support binding a method receiver");
                                    }

                                    /**
                                     * {@inheritDoc}
                                     */
                                    public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation) {
                                        if (!target.getType().asErasure().isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())) {
                                            throw new IllegalStateException("Cannot assign method handle to " + target);
                                        }
                                        return new ForFieldHandle.Resolved(access, fieldDescription);
                                    }
                                }
                            }
                        }

                        /**
                         * An offset mapping for describing a representation of the substituted element or the instrumented method.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        class ForOrigin implements OffsetMapping {

                            /**
                             * The sort of the origin representation.
                             */
                            private final Sort sort;

                            /**
                             * The source providing the reference.
                             */
                            private final Source source;

                            /**
                             * Creates an offset mapping a representation of the substituted element or instrumented method.
                             *
                             * @param sort   The sort of the origin representation.
                             * @param source The source providing the reference.
                             */
                            protected ForOrigin(Sort sort, Source source) {
                                this.sort = sort;
                                this.source = source;
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public OffsetMapping.Resolved resolve(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                return new ForOrigin.Resolved(sort, source, instrumentedMethod);
                            }

                            /**
                             * The sort of the origin expression.
                             */
                            protected enum Sort {

                                /**
                                 * Represents the supplied value as a {@link Method}.
                                 */
                                METHOD {
                                    @Override
                                    protected boolean isRepresentable(ByteCodeElement.Member original) {
                                        return original instanceof MethodDescription && ((MethodDescription) original).isMethod();
                                    }

                                    @Override
                                    protected StackManipulation resolve(ByteCodeElement.Member original, List<TypeDescription> parameterTypes, TypeDescription returnType) {
                                        return MethodConstant.of(((MethodDescription) original).asDefined());
                                    }
                                },

                                /**
                                 * Represents the supplied value as a {@link Constructor}.
                                 */
                                CONSTRUCTOR {
                                    @Override
                                    protected boolean isRepresentable(ByteCodeElement.Member original) {
                                        return original instanceof MethodDescription && ((MethodDescription) original).isConstructor();
                                    }

                                    @Override
                                    protected StackManipulation resolve(ByteCodeElement.Member original, List<TypeDescription> parameterTypes, TypeDescription returnType) {
                                        return MethodConstant.of(((MethodDescription) original).asDefined());
                                    }
                                },

                                /**
                                 * Represents the supplied value as a {@link Field}.
                                 */
                                FIELD {
                                    @Override
                                    protected boolean isRepresentable(ByteCodeElement.Member original) {
                                        return original instanceof FieldDescription;
                                    }

                                    @Override
                                    protected StackManipulation resolve(ByteCodeElement.Member original, List<TypeDescription> parameterTypes, TypeDescription returnType) {
                                        return new FieldConstant(((FieldDescription) original).asDefined());
                                    }
                                },

                                /**
                                 * Represents the supplied value as a {@code java.lang.reflect.Executable}.
                                 */
                                EXECUTABLE {
                                    @Override
                                    protected boolean isRepresentable(ByteCodeElement.Member original) {
                                        return original instanceof MethodDescription;
                                    }

                                    @Override
                                    protected StackManipulation resolve(ByteCodeElement.Member original, List<TypeDescription> parameterTypes, TypeDescription returnType) {
                                        return MethodConstant.of(((MethodDescription) original).asDefined());
                                    }
                                },

                                /**
                                 * Represents the supplied value as a {@link Class}.
                                 */
                                TYPE {
                                    @Override
                                    protected boolean isRepresentable(ByteCodeElement.Member original) {
                                        return true;
                                    }

                                    @Override
                                    protected StackManipulation resolve(ByteCodeElement.Member original, List<TypeDescription> parameterTypes, TypeDescription returnType) {
                                        return ClassConstant.of(original.getDeclaringType().asErasure());
                                    }
                                },

                                /**
                                 * Represents the supplied value as a {@code java.lang.invoke.MethodHandles.Lookup}.
                                 */
                                LOOKUP {
                                    @Override
                                    protected boolean isRepresentable(ByteCodeElement.Member original) {
                                        return true;
                                    }

                                    @Override
                                    protected StackManipulation resolve(ByteCodeElement.Member original, List<TypeDescription> parameterTypes, TypeDescription returnType) {
                                        return MethodInvocation.lookup();
                                    }
                                },

                                /**
                                 * Represents the supplied value as a {@code java.lang.invoke.MethodHandle}.
                                 */
                                METHOD_HANDLE {
                                    @Override
                                    protected boolean isRepresentable(ByteCodeElement.Member original) {
                                        return true;
                                    }

                                    @Override
                                    protected StackManipulation resolve(ByteCodeElement.Member original, List<TypeDescription> parameterTypes, TypeDescription returnType) {
                                        JavaConstant.MethodHandle handle;
                                        if (original instanceof MethodDescription) {
                                            handle = JavaConstant.MethodHandle.of(((MethodDescription) original).asDefined());
                                        } else if (original instanceof FieldDescription) {
                                            handle = returnType.represents(void.class)
                                                    ? JavaConstant.MethodHandle.ofSetter(((FieldDescription) original).asDefined())
                                                    : JavaConstant.MethodHandle.ofGetter(((FieldDescription) original).asDefined());
                                        } else {
                                            throw new IllegalStateException("Unexpected byte code element: " + original);
                                        }
                                        return handle.toStackManipulation();
                                    }
                                },

                                /**
                                 * Represents the supplied value as a {@code java.lang.invoke.MethodType}.
                                 */
                                METHOD_TYPE {
                                    @Override
                                    protected boolean isRepresentable(ByteCodeElement.Member original) {
                                        return true;
                                    }

                                    @Override
                                    protected StackManipulation resolve(ByteCodeElement.Member original, List<TypeDescription> parameterTypes, TypeDescription returnType) {
                                        return JavaConstant.MethodType.of(returnType, parameterTypes).toStackManipulation();
                                    }
                                },

                                /**
                                 * Represents the supplied value as its {@link Object#toString()} representation.
                                 */
                                STRING {
                                    @Override
                                    protected boolean isRepresentable(ByteCodeElement.Member original) {
                                        return true;
                                    }

                                    @Override
                                    protected StackManipulation resolve(ByteCodeElement.Member original, List<TypeDescription> parameterTypes, TypeDescription returnType) {
                                        return new TextConstant(original.toString());
                                    }
                                };

                                /**
                                 * Checks if the supplied member can be represented by this sort.
                                 *
                                 * @param original The byte code element to check.
                                 * @return {@code true} if the supplied element can be represented.
                                 */
                                protected abstract boolean isRepresentable(ByteCodeElement.Member original);

                                /**
                                 * Creates a stack manipulation for the supplied byte code element.
                                 *
                                 * @param original       The substituted element.
                                 * @param parameterTypes The parameter types.
                                 * @param returnType     The return type.
                                 * @return A stack manipulation loading the supplied byte code element's representation onto the stack.
                                 */
                                protected abstract StackManipulation resolve(ByteCodeElement.Member original, List<TypeDescription> parameterTypes, TypeDescription returnType);
                            }

                            /**
                             * A factory for an offset mapping that describes a representation of the substituted element or instrumented method.
                             */
                            protected enum Factory implements OffsetMapping.Factory<Origin> {

                                /**
                                 * The singleton instance.
                                 */
                                INSTANCE;

                                /**
                                 * The {@link Origin#source()} property.
                                 */
                                private static final MethodDescription.InDefinedShape ORIGIN_TYPE = TypeDescription.ForLoadedType.of(Origin.class)
                                        .getDeclaredMethods()
                                        .filter(named("source"))
                                        .getOnly();

                                /**
                                 * {@inheritDoc}
                                 */
                                public Class<Origin> getAnnotationType() {
                                    return Origin.class;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(MethodDescription.InDefinedShape target, AnnotationDescription.Loadable<Origin> annotation) {
                                    throw new UnsupportedOperationException("This factory does not support binding a method receiver");
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<Origin> annotation) {
                                    Sort sort;
                                    if (target.getType().asErasure().represents(Class.class)) {
                                        sort = Sort.TYPE;
                                    } else if (target.getType().asErasure().represents(Method.class)) {
                                        sort = Sort.METHOD;
                                    } else if (target.getType().asErasure().represents(Constructor.class)) {
                                        sort = Sort.CONSTRUCTOR;
                                    } else if (target.getType().asErasure().represents(Field.class)) {
                                        sort = Sort.FIELD;
                                    } else if (JavaType.EXECUTABLE.getTypeStub().equals(target.getType().asErasure())) {
                                        sort = Sort.EXECUTABLE;
                                    } else if (JavaType.METHOD_HANDLE.getTypeStub().equals(target.getType().asErasure())) {
                                        sort = Sort.METHOD_HANDLE;
                                    } else if (JavaType.METHOD_TYPE.getTypeStub().equals(target.getType().asErasure())) {
                                        sort = Sort.METHOD_TYPE;
                                    } else if (JavaType.METHOD_HANDLES_LOOKUP.getTypeStub().equals(target.getType().asErasure())) {
                                        sort = Sort.LOOKUP;
                                    } else if (target.getType().asErasure().isAssignableFrom(String.class)) {
                                        sort = Sort.STRING;
                                    } else {
                                        throw new IllegalStateException("Non-supported type " + target.getType() + " for @Origin annotation");
                                    }
                                    return new ForOrigin(sort, annotation.getValue(ORIGIN_TYPE).resolve(EnumerationDescription.class).load(Source.class));
                                }
                            }

                            /**
                             * A resolved offset mapping for a representation of the substituted expression or instrumented method.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            protected static class Resolved implements OffsetMapping.Resolved {

                                /**
                                 * The sort of the origin representation.
                                 */
                                private final Sort sort;

                                /**
                                 * The source providing the reference.
                                 */
                                private final Source source;

                                /**
                                 * The instrumented method.
                                 */
                                private final MethodDescription instrumentedMethod;

                                /**
                                 * Creates a resolved version of an offset mapping for describing the substituted expression or instrumented method.
                                 *
                                 * @param sort               The sort of the origin representation.
                                 * @param source             The source providing the reference.
                                 * @param instrumentedMethod The instrumented method.
                                 */
                                protected Resolved(Sort sort, Source source, MethodDescription instrumentedMethod) {
                                    this.sort = sort;
                                    this.source = source;
                                    this.instrumentedMethod = instrumentedMethod;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public StackManipulation apply(TypeDescription receiver,
                                                               ByteCodeElement.Member original,
                                                               TypeList.Generic parameters,
                                                               TypeDescription.Generic result,
                                                               TypeDescription.Generic current,
                                                               JavaConstant.MethodHandle methodHandle,
                                                               Map<Integer, Integer> offsets,
                                                               int offset) {
                                    if (!source.isRepresentable(sort, original, instrumentedMethod)) {
                                        throw new IllegalStateException("Cannot represent " + sort + " for " + source + " in " + instrumentedMethod);
                                    }
                                    return source.resolve(sort, original, parameters, result, instrumentedMethod);
                                }
                            }
                        }

                        /**
                         * An offset mapping that assigns a stub value.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        class ForStubValue implements OffsetMapping {

                            /**
                             * The source providing the reference.
                             */
                            private final Source source;

                            /**
                             * Creates an offset mapping for a stub value.
                             *
                             * @param source The source providing the reference.
                             */
                            protected ForStubValue(Source source) {
                                this.source = source;
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public OffsetMapping.Resolved resolve(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                return new Resolved(source, instrumentedMethod);
                            }

                            /**
                             * A resolved offset mapping for an offset mapping of a stub value.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            protected static class Resolved implements OffsetMapping.Resolved {

                                /**
                                 * The source providing the reference.
                                 */
                                private final Source source;

                                /**
                                 * The instrumented method.
                                 */
                                private final MethodDescription instrumentedMethod;

                                /**
                                 * Creates a resolved version of an offset mapping for a stub value.
                                 *
                                 * @param source             The source providing the reference.
                                 * @param instrumentedMethod The instrumented method.
                                 */
                                protected Resolved(Source source, MethodDescription instrumentedMethod) {
                                    this.source = source;
                                    this.instrumentedMethod = instrumentedMethod;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public StackManipulation apply(TypeDescription receiver,
                                                               ByteCodeElement.Member original,
                                                               TypeList.Generic parameters,
                                                               TypeDescription.Generic result,
                                                               TypeDescription.Generic current,
                                                               JavaConstant.MethodHandle methodHandle,
                                                               Map<Integer, Integer> offsets,
                                                               int offset) {
                                    return DefaultValue.of(source.handle(methodHandle, instrumentedMethod).getReturnType());
                                }
                            }

                            /**
                             * A factory for creating an offset mapping for a stub value.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            protected enum Factory implements OffsetMapping.Factory<StubValue> {

                                /**
                                 * The singleton instance.
                                 */
                                INSTANCE;

                                /**
                                 * The {@link StubValue#source()} property.
                                 */
                                private static final MethodDescription.InDefinedShape STUB_VALUE_SOURCE = TypeDescription.ForLoadedType.of(StubValue.class)
                                        .getDeclaredMethods()
                                        .filter(named("source"))
                                        .getOnly();

                                /**
                                 * {@inheritDoc}
                                 */
                                public Class<StubValue> getAnnotationType() {
                                    return StubValue.class;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(MethodDescription.InDefinedShape target, AnnotationDescription.Loadable<StubValue> annotation) {
                                    throw new UnsupportedOperationException("This factory does not support binding a method receiver");
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<StubValue> annotation) {
                                    if (!target.getType().represents(Object.class)) {
                                        throw new IllegalStateException("Expected " + target + " to declare an Object type");
                                    }
                                    return new ForStubValue(annotation.getValue(STUB_VALUE_SOURCE).resolve(EnumerationDescription.class).load(Source.class));
                                }
                            }
                        }

                        /**
                         * An offset mapping that assigns the value of the previous chain instruction.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        class ForCurrent implements OffsetMapping {

                            /**
                             * The type of the targeted expression.
                             */
                            private final TypeDescription.Generic targetType;

                            /**
                             * The typing to use or {@code null} if implicit typing.
                             */
                            @MaybeNull
                            @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
                            private final Assigner.Typing typing;

                            /**
                             * Creates an offset mapping for the previous chain instruction.
                             *
                             * @param targetType The type of the targeted expression.
                             * @param typing     The typing to use or {@code null} if implicit typing.
                             */
                            public ForCurrent(TypeDescription.Generic targetType, @MaybeNull Assigner.Typing typing) {
                                this.targetType = targetType;
                                this.typing = typing;
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public OffsetMapping.Resolved resolve(Assigner assigner, Assigner.Typing typing, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                return new ForCurrent.Resolved(targetType, assigner, this.typing == null ? typing : this.typing);
                            }

                            /**
                             * A factory for creating an offset mapping for assigning the result of the previous chain instruction.
                             */
                            protected enum Factory implements OffsetMapping.Factory<Current> {

                                /**
                                 * The singleton instance.
                                 */
                                INSTANCE;

                                /**
                                 * The {@link Current#typing()} property.
                                 */
                                private static final MethodDescription.InDefinedShape CURRENT_TYPING = TypeDescription.ForLoadedType.of(Current.class)
                                        .getDeclaredMethods()
                                        .filter(named("typing"))
                                        .getOnly();

                                /**
                                 * {@inheritDoc}
                                 */
                                public Class<Current> getAnnotationType() {
                                    return Current.class;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(MethodDescription.InDefinedShape target, AnnotationDescription.Loadable<Current> annotation) {
                                    return new ForCurrent(target.getDeclaringType().asGenericType(),
                                            annotation.getValue(CURRENT_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class));
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<Current> annotation) {
                                    return new ForCurrent(target.getType(), annotation.getValue(CURRENT_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class));
                                }
                            }

                            /**
                             * A resolved offset mapping for assigning the previous chain instruction.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            protected static class Resolved implements OffsetMapping.Resolved {

                                /**
                                 * The type of the targeted expression.
                                 */
                                private final TypeDescription.Generic targetType;

                                /**
                                 * The assigner to use.
                                 */
                                private final Assigner assigner;

                                /**
                                 * The typing to use.
                                 */
                                private final Assigner.Typing typing;

                                /**
                                 * Creates a resolved offset mapping for assigning the previous chain instruction.
                                 *
                                 * @param targetType The type of the targeted expression.
                                 * @param assigner   The assigner to use.
                                 * @param typing     The typing to use.
                                 */
                                public Resolved(TypeDescription.Generic targetType, Assigner assigner, Assigner.Typing typing) {
                                    this.targetType = targetType;
                                    this.assigner = assigner;
                                    this.typing = typing;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public StackManipulation apply(TypeDescription receiver,
                                                               ByteCodeElement.Member original,
                                                               TypeList.Generic parameters,
                                                               TypeDescription.Generic result,
                                                               TypeDescription.Generic current,
                                                               JavaConstant.MethodHandle methodHandle,
                                                               Map<Integer, Integer> offsets,
                                                               int offset) {
                                    StackManipulation assignment = assigner.assign(current, targetType, typing);
                                    if (!assignment.isValid()) {
                                        throw new IllegalStateException("Cannot assign " + current + " to " + targetType);
                                    }
                                    return new StackManipulation.Compound(MethodVariableAccess.of(current).loadFrom(offset), assignment);
                                }
                            }
                        }
                    }

                    /**
                     * A dispatcher for invoking a delegation method.
                     */
                    protected interface Dispatcher {

                        /**
                         * Resolves a dispatcher for a given instrumented type and method.
                         *
                         * @param instrumentedType   The instrumented type.
                         * @param instrumentedMethod The instrumented method.
                         * @return A resolved version of this dispatcher.
                         */
                        Dispatcher.Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod);

                        /**
                         * A dispatcher that has been resolved for a given instrumented type and method.
                         */
                        interface Resolved {

                            StackManipulation initialize();

                            /**
                             * Creates a stack manipulation for a given substitution target.
                             *
                             * @param receiver     The type upon which the substituted element is invoked upon.
                             * @param original     The substituted element.
                             * @param methodHandle A method handle that describes the invocation.
                             * @return A stack manipulation that executes the represented delegation.
                             */
                            StackManipulation apply(TypeDescription receiver, ByteCodeElement.Member original, JavaConstant.MethodHandle methodHandle);
                        }

                        /**
                         * A factory for creating a dispatcher.
                         */
                        interface Factory {

                            /**
                             * Creates a dispatcher for a given delegation method.
                             *
                             * @param delegate The method or constructor to delegate to.
                             * @return An appropriate dispatcher.
                             */
                            Dispatcher make(MethodDescription.InDefinedShape delegate);
                        }

                        /**
                         * A dispatcher that invokes a delegate method directly.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        class ForRegularInvocation implements Dispatcher, Dispatcher.Resolved {

                            /**
                             * The delegation method.
                             */
                            private final MethodDescription delegate;

                            /**
                             * Creates a dispatcher for a regular method invocation.
                             *
                             * @param delegate The delegation method.
                             */
                            protected ForRegularInvocation(MethodDescription delegate) {
                                this.delegate = delegate;
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                return this;
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public StackManipulation initialize() {
                                return delegate.isConstructor()
                                        ? new StackManipulation.Compound(TypeCreation.of(delegate.getDeclaringType().asErasure()), Duplication.SINGLE)
                                        : StackManipulation.Trivial.INSTANCE;
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public StackManipulation apply(TypeDescription receiver, ByteCodeElement.Member original, JavaConstant.MethodHandle methodHandle) {
                                return MethodInvocation.invoke(delegate);
                            }

                            /**
                             * A factory for creating a dispatcher for a regular method invocation.
                             */
                            protected enum Factory implements Dispatcher.Factory {

                                /**
                                 * The singleton instance.
                                 */
                                INSTANCE;

                                /**
                                 * {@inheritDoc}
                                 */
                                public Dispatcher make(MethodDescription.InDefinedShape delegate) {
                                    return new ForRegularInvocation(delegate);
                                }
                            }
                        }

                        /**
                         * A method dispatcher that is using a dynamic method invocation.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        class ForDynamicInvocation implements Dispatcher {

                            /**
                             * The bootstrap method.
                             */
                            private final MethodDescription.InDefinedShape bootstrapMethod;

                            /**
                             * The delegation method.
                             */
                            private final MethodDescription.InDefinedShape delegate;

                            /**
                             * A resolver for supplying arguments to the bootstrap method.
                             */
                            private final BootstrapArgumentResolver resolver;

                            /**
                             * Creates a dispatcher for a dynamic method invocation.
                             *
                             * @param bootstrapMethod The bootstrap method.
                             * @param delegate        The delegation method.
                             * @param resolver        A resolver for supplying arguments to the bootstrap method.
                             */
                            protected ForDynamicInvocation(MethodDescription.InDefinedShape bootstrapMethod, MethodDescription.InDefinedShape delegate, BootstrapArgumentResolver resolver) {
                                this.bootstrapMethod = bootstrapMethod;
                                this.delegate = delegate;
                                this.resolver = resolver;
                            }

                            /**
                             * Creates a dispatcher factory for a dynamic method invocation.
                             *
                             * @param bootstrapMethod The bootstrap method.
                             * @param resolverFactory A resolver for supplying arguments to the bootstrap method.
                             * @return An appropriate dispatcher factory.
                             */
                            protected static Dispatcher.Factory of(MethodDescription.InDefinedShape bootstrapMethod, BootstrapArgumentResolver.Factory resolverFactory) {
                                if (!bootstrapMethod.isInvokeBootstrap()) {
                                    throw new IllegalStateException("Not a bootstrap method: " + bootstrapMethod);
                                }
                                return new ForDynamicInvocation.Factory(bootstrapMethod, resolverFactory);
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public Dispatcher.Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                return new ForDynamicInvocation.Resolved(bootstrapMethod, delegate, resolver.resolve(instrumentedType, instrumentedMethod));
                            }

                            /**
                             * A resolved dispatcher for a dynamically bound method invocation.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            protected static class Resolved implements Dispatcher.Resolved {

                                /**
                                 * The bootstrap method.
                                 */
                                private final MethodDescription.InDefinedShape bootstrapMethod;

                                /**
                                 * The delegation target.
                                 */
                                private final MethodDescription.InDefinedShape delegate;

                                /**
                                 * The bootstrap argument resolver to use.
                                 */
                                private final BootstrapArgumentResolver.Resolved resolver;

                                /**
                                 * Creates a resolved dispatcher of a dynamic method dispatcher.
                                 *
                                 * @param bootstrapMethod The bootstrap method.
                                 * @param delegate        The delegation target.
                                 * @param resolver        The bootstrap argument resolver to use.
                                 */
                                protected Resolved(MethodDescription.InDefinedShape bootstrapMethod, MethodDescription.InDefinedShape delegate, BootstrapArgumentResolver.Resolved resolver) {
                                    this.bootstrapMethod = bootstrapMethod;
                                    this.delegate = delegate;
                                    this.resolver = resolver;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public StackManipulation initialize() {
                                    return StackManipulation.Trivial.INSTANCE;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public StackManipulation apply(TypeDescription receiver, ByteCodeElement.Member original, JavaConstant.MethodHandle methodHandle) {
                                    List<JavaConstant> constants = resolver.make(receiver, original, methodHandle);
                                    if (!bootstrapMethod.isInvokeBootstrap(TypeList.Explicit.of(constants))) {
                                        throw new IllegalArgumentException(bootstrapMethod + " is not accepting advice bootstrap arguments: " + constants);
                                    }
                                    return MethodInvocation.invoke(bootstrapMethod).dynamic(delegate.getInternalName(),
                                            delegate.getReturnType().asErasure(),
                                            delegate.getParameters().asTypeList().asErasures(),
                                            constants);
                                }
                            }

                            /**
                             * A factory for a dynamic method invocation of the dispatcher method or constructor.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            protected static class Factory implements Dispatcher.Factory {

                                /**
                                 * The bootstrap method.
                                 */
                                private final MethodDescription.InDefinedShape bootstrapMethod;

                                /**
                                 * A factory for a bootstrap argument resolver.
                                 */
                                private final BootstrapArgumentResolver.Factory resolverFactory;

                                /**
                                 * Creates a new factory for a dispatcher using a dynamic method invocation.
                                 *
                                 * @param bootstrapMethod The bootstrap method.
                                 * @param resolverFactory A factory for a bootstrap argument resolver.
                                 */
                                protected Factory(MethodDescription.InDefinedShape bootstrapMethod, BootstrapArgumentResolver.Factory resolverFactory) {
                                    this.bootstrapMethod = bootstrapMethod;
                                    this.resolverFactory = resolverFactory;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public Dispatcher make(MethodDescription.InDefinedShape delegate) {
                                    return new ForDynamicInvocation(bootstrapMethod, delegate, resolverFactory.make(delegate));
                                }
                            }
                        }
                    }

                    /**
                     * A resolver for supplying arguments to a bootstrap method which is binding the delegation method's invocation.
                     */
                    public interface BootstrapArgumentResolver {

                        /**
                         * Resolves this resolver for a given instrumented type and method.
                         *
                         * @param instrumentedType   The instrumented type.
                         * @param instrumentedMethod The instrumented method.
                         * @return A resolved version of this argument resolver.
                         */
                        BootstrapArgumentResolver.Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod);

                        /**
                         * A resolved version of a bootstrap argument handler.
                         */
                        interface Resolved {

                            /**
                             * Returns the constant values to supply to the bootstrap method.
                             *
                             * @param receiver     The type upon which the substituted element is applied.
                             * @param original     The substituted element.
                             * @param methodHandle A method handle that represents the substituted element.
                             * @return A list of constant values to supply to the bootstrap method.
                             */
                            List<JavaConstant> make(TypeDescription receiver, ByteCodeElement.Member original, JavaConstant.MethodHandle methodHandle);
                        }

                        /**
                         * A factory for a bootstrap argument resolver.
                         */
                        interface Factory {

                            /**
                             * Creates a bootstrap argument resolver for a given delegation method.
                             *
                             * @param delegate The method or constructor to which to delegate.
                             * @return An appropriate bootstrap argument resolver.
                             */
                            BootstrapArgumentResolver make(MethodDescription.InDefinedShape delegate);
                        }

                        /**
                         * An implementation that supplies a default set of arguments to a bootstrap method. The arguments are:
                         * <ul>
                         * <li>A {@code java.lang.invoke.MethodHandles.Lookup} representing the source method.</li>
                         * <li>A {@link String} representing the target's internal name.</li>
                         * <li>A {@code java.lang.invoke.MethodType} representing the type that is requested for binding.</li>
                         * <li>A {@link String} representation of the delegate's binary class name.</li>
                         * <li>A {@link Class} representing the receiver type of the substituted element.</li>
                         * <li>A {@link String} representing the internal name of the substituted element.</li>
                         * <li>A {@code java.lang.invoke.MethodHandle} to the substituted element.</li>
                         * <li>A {@link Class} describing the instrumented type.</li>
                         * <li>A {@link String} representing the instrumented method or constructor.</li>
                         * </ul>
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        class ForDefaultValues implements BootstrapArgumentResolver {

                            /**
                             * The delegation target.
                             */
                            private final MethodDescription.InDefinedShape delegate;

                            /**
                             * Creates a default bootstrap argument resolver.
                             *
                             * @param delegate The delegation target.
                             */
                            protected ForDefaultValues(MethodDescription.InDefinedShape delegate) {
                                this.delegate = delegate;
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public BootstrapArgumentResolver.Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                return new Resolved(delegate, instrumentedType, instrumentedMethod);
                            }

                            /**
                             * A resolved default bootstrap argument resolver.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            protected static class Resolved implements BootstrapArgumentResolver.Resolved {

                                /**
                                 * The delegation target.
                                 */
                                private final MethodDescription.InDefinedShape delegate;

                                /**
                                 * The instrumented type.
                                 */
                                private final TypeDescription instrumentedType;

                                /**
                                 * The instrumented method.
                                 */
                                private final MethodDescription instrumentedMethod;

                                /**
                                 * Creates a resolved version of a bootstrap argument resolver.
                                 *
                                 * @param delegate           The delegation target.
                                 * @param instrumentedType   The instrumented type.
                                 * @param instrumentedMethod The instrumented method.
                                 */
                                protected Resolved(MethodDescription.InDefinedShape delegate, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                    this.delegate = delegate;
                                    this.instrumentedType = instrumentedType;
                                    this.instrumentedMethod = instrumentedMethod;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public List<JavaConstant> make(TypeDescription receiver, ByteCodeElement.Member original, JavaConstant.MethodHandle methodHandle) {
                                    if (instrumentedMethod.isTypeInitializer()) {
                                        return Arrays.asList(JavaConstant.Simple.ofLoaded(delegate.getDeclaringType().getName()),
                                                JavaConstant.Simple.of(receiver),
                                                JavaConstant.Simple.ofLoaded(original.getInternalName()),
                                                methodHandle,
                                                JavaConstant.Simple.of(instrumentedType),
                                                JavaConstant.Simple.ofLoaded(instrumentedMethod.getInternalName()));
                                    } else {
                                        return Arrays.asList(JavaConstant.Simple.ofLoaded(delegate.getDeclaringType().getName()),
                                                JavaConstant.Simple.of(receiver),
                                                JavaConstant.Simple.ofLoaded(original.getInternalName()),
                                                methodHandle,
                                                JavaConstant.Simple.of(instrumentedType),
                                                JavaConstant.Simple.ofLoaded(instrumentedMethod.getInternalName()),
                                                JavaConstant.MethodHandle.of(instrumentedMethod.asDefined()));
                                    }
                                }
                            }

                            /**
                             * A factory for creating a default bootstrap argument resolver.
                             */
                            public enum Factory implements BootstrapArgumentResolver.Factory {

                                /**
                                 * The singleton instance.
                                 */
                                INSTANCE;

                                /**
                                 * {@inheritDoc}
                                 */
                                public BootstrapArgumentResolver make(MethodDescription.InDefinedShape delegate) {
                                    return new ForDefaultValues(delegate);
                                }
                            }
                        }
                    }

                    /**
                     * A factory for a {@link ForDelegation} which allows for a custom configuration.
                     */
                    public static class WithCustomMapping {

                        /**
                         * The dispatcher factory to use.
                         */
                        private final Dispatcher.Factory dispatcherFactory;

                        /**
                         * A mapping of offset mapping factories by their respective annotation type.
                         */
                        private final Map<Class<? extends Annotation>, OffsetMapping.Factory<?>> offsetMappings;

                        /**
                         * Creates a factory for a {@link ForDelegation} with a custom value.
                         *
                         * @param dispatcherFactory The dispatcher factory to use.
                         * @param offsetMappings    A mapping of offset mapping factories by their respective annotation type.
                         */
                        protected WithCustomMapping(Dispatcher.Factory dispatcherFactory, Map<Class<? extends Annotation>, OffsetMapping.Factory<?>> offsetMappings) {
                            this.dispatcherFactory = dispatcherFactory;
                            this.offsetMappings = offsetMappings;
                        }

                        /**
                         * Binds the supplied annotation to a type constant of the supplied value. Constants can be strings, method handles, method types
                         * and any primitive or the value {@code null}.
                         *
                         * @param type  The type of the annotation being bound.
                         * @param value The value to bind to the annotation or {@code null} to bind the parameter type's default value.
                         * @param <T>   The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation type during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bind(Class<T> type, @MaybeNull Object value) {
                            return bind(OffsetMapping.ForStackManipulation.of(type, value));
                        }

                        /**
                         * Binds the supplied annotation to the value of the supplied field. The field must be visible by the
                         * instrumented type and must be declared by a super type of the instrumented field.
                         *
                         * @param type  The type of the annotation being bound.
                         * @param field The field to bind to this annotation.
                         * @param <T>   The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation type during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bind(Class<T> type, Field field) {
                            return bind(type, new FieldDescription.ForLoadedField(field));
                        }

                        /**
                         * Binds the supplied annotation to the value of the supplied field. The field must be visible by the
                         * instrumented type and must be declared by a super type of the instrumented field. The binding is defined
                         * as read-only and applied static typing.
                         *
                         * @param type             The type of the annotation being bound.
                         * @param fieldDescription The field to bind to this annotation.
                         * @param <T>              The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation type during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bind(Class<T> type, FieldDescription fieldDescription) {
                            return bind(new OffsetMapping.ForField.Resolved.Factory<T>(type, fieldDescription));
                        }

                        /**
                         * Binds the supplied annotation to the supplied type constant.
                         *
                         * @param type  The type of the annotation being bound.
                         * @param value The type constant to bind.
                         * @param <T>   The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation type during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bind(Class<T> type, Class<?> value) {
                            return bind(type, TypeDescription.ForLoadedType.of(value));
                        }

                        /**
                         * Binds the supplied annotation to the supplied type constant.
                         *
                         * @param type  The type of the annotation being bound.
                         * @param value The type constant to bind.
                         * @param <T>   The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation type during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bind(Class<T> type, TypeDescription value) {
                            return bind(new OffsetMapping.ForStackManipulation.Factory<T>(type, ConstantValue.Simple.wrap(value)));
                        }

                        /**
                         * Binds the supplied annotation to the supplied enumeration constant.
                         *
                         * @param type  The type of the annotation being bound.
                         * @param value The enumeration constant to bind.
                         * @param <T>   The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation type during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bind(Class<T> type, Enum<?> value) {
                            return bind(type, new EnumerationDescription.ForLoadedEnumeration(value));
                        }

                        /**
                         * Binds the supplied annotation to the supplied enumeration constant.
                         *
                         * @param type  The type of the annotation being bound.
                         * @param value The enumeration constant to bind.
                         * @param <T>   The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation type during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bind(Class<T> type, EnumerationDescription value) {
                            return bind(new OffsetMapping.ForStackManipulation.Factory<T>(type, ConstantValue.Simple.wrap(value)));
                        }

                        /**
                         * Binds the supplied annotation to the supplied fixed value.
                         *
                         * @param type  The type of the annotation being bound.
                         * @param value The value to bind to this annotation.
                         * @param <T>   The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation type during binding.
                         */
                        @SuppressWarnings("unchecked")
                        public <T extends Annotation> WithCustomMapping bindSerialized(Class<T> type, Serializable value) {
                            return bindSerialized(type, value, (Class<Serializable>) value.getClass());
                        }

                        /**
                         * Binds the supplied annotation to the supplied fixed value.
                         *
                         * @param type       The type of the annotation being bound.
                         * @param value      The value to bind to this annotation.
                         * @param targetType The type of {@code value} as which the instance should be treated.
                         * @param <T>        The annotation type.
                         * @param <S>        The type of the serialized instance.
                         * @return A new builder for a delegate that considers the supplied annotation type during binding.
                         */
                        public <T extends Annotation, S extends Serializable> WithCustomMapping bindSerialized(Class<T> type, S value, Class<? super S> targetType) {
                            return bind(OffsetMapping.ForStackManipulation.OfSerializedConstant.of(type, value, targetType));
                        }

                        /**
                         * Binds the supplied annotation to the annotation's property of the specified name.
                         *
                         * @param type     The type of the annotation being bound.
                         * @param property The name of the annotation property to be bound.
                         * @param <T>      The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bindProperty(Class<T> type, String property) {
                            return bind(OffsetMapping.ForStackManipulation.OfAnnotationProperty.of(type, property));
                        }

                        /**
                         * Binds the supplied annotation to the given Java constant.
                         *
                         * @param type     The type of the annotation being bound.
                         * @param constant The constant value that is bound.
                         * @param <T>      The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bind(Class<T> type, ConstantValue constant) {
                            return bind(new OffsetMapping.ForStackManipulation.Factory<T>(type, constant.toStackManipulation(), constant.getTypeDescription().asGenericType()));
                        }

                        /**
                         * Binds the supplied annotation to the annotation's property of the specified name.
                         *
                         * @param type              The type of the annotation being bound.
                         * @param stackManipulation The stack manipulation loading the bound value.
                         * @param targetType        The type of the loaded value.
                         * @param <T>               The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bind(Class<T> type, StackManipulation stackManipulation, java.lang.reflect.Type targetType) {
                            return bind(type, stackManipulation, TypeDefinition.Sort.describe(targetType));
                        }

                        /**
                         * Binds the supplied annotation to the annotation's property of the specified name.
                         *
                         * @param type              The type of the annotation being bound.
                         * @param stackManipulation The stack manipulation loading the bound value.
                         * @param targetType        The type of the loaded value.
                         * @param <T>               The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bind(Class<T> type, StackManipulation stackManipulation, TypeDescription.Generic targetType) {
                            return bind(new OffsetMapping.ForStackManipulation.Factory<T>(type, stackManipulation, targetType));
                        }

                        /**
                         * Binds the supplied annotation as a lambda expression via the JVM's lambda metafactory.
                         *
                         * @param type                The type of the annotation being bound.
                         * @param constructor         The constructor being bound as the lambda expression's implementation.
                         * @param functionalInterface The functional interface that represents the lambda expression.
                         * @param <T>                 The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bindLambda(Class<T> type, Constructor<?> constructor, Class<?> functionalInterface) {
                            return bindLambda(type, new MethodDescription.ForLoadedConstructor(constructor), TypeDescription.ForLoadedType.of(functionalInterface));
                        }

                        /**
                         * Binds the supplied annotation as a lambda expression via the JVM's lambda metafactory.
                         *
                         * @param type                The type of the annotation being bound.
                         * @param constructor         The constructor being bound as the lambda expression's implementation.
                         * @param functionalInterface The functional interface that represents the lambda expression.
                         * @param methodGraphCompiler The method graph compiler that resolves the functional method of the function interface.
                         * @param <T>                 The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bindLambda(Class<T> type, Constructor<?> constructor, Class<?> functionalInterface, MethodGraph.Compiler methodGraphCompiler) {
                            return bindLambda(type, new MethodDescription.ForLoadedConstructor(constructor), TypeDescription.ForLoadedType.of(functionalInterface), methodGraphCompiler);
                        }

                        /**
                         * Binds the supplied annotation as a lambda expression via the JVM's lambda metafactory.
                         *
                         * @param type                The type of the annotation being bound.
                         * @param method              The method being bound as the lambda expression's implementation.
                         * @param functionalInterface The functional interface that represents the lambda expression.
                         * @param <T>                 The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bindLambda(Class<T> type, Method method, Class<?> functionalInterface) {
                            return bindLambda(type, new MethodDescription.ForLoadedMethod(method), TypeDescription.ForLoadedType.of(functionalInterface));
                        }

                        /**
                         * Binds the supplied annotation as a lambda expression via the JVM's lambda metafactory.
                         *
                         * @param type                The type of the annotation being bound.
                         * @param method              The method being bound as the lambda expression's implementation.
                         * @param functionalInterface The functional interface that represents the lambda expression.
                         * @param methodGraphCompiler The method graph compiler that resolves the functional method of the function interface.
                         * @param <T>                 The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bindLambda(Class<T> type, Method method, Class<?> functionalInterface, MethodGraph.Compiler methodGraphCompiler) {
                            return bindLambda(type, new MethodDescription.ForLoadedMethod(method), TypeDescription.ForLoadedType.of(functionalInterface), methodGraphCompiler);
                        }

                        /**
                         * Binds the supplied annotation as a lambda expression via the JVM's lambda metafactory.
                         *
                         * @param type                The type of the annotation being bound.
                         * @param methodDescription   The method or constructor being bound as the lambda expression's implementation.
                         * @param functionalInterface The functional interface that represents the lambda expression.
                         * @param <T>                 The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bindLambda(Class<T> type, MethodDescription.InDefinedShape methodDescription, TypeDescription functionalInterface) {
                            return bindLambda(type, methodDescription, functionalInterface, MethodGraph.Compiler.DEFAULT);
                        }

                        /**
                         * Binds the supplied annotation as a lambda expression via the JVM's lambda metafactory.
                         *
                         * @param type                The type of the annotation being bound.
                         * @param methodDescription   The method or constuctor being bound as the lambda expression's implementation.
                         * @param functionalInterface The functional interface that represents the lambda expression.
                         * @param methodGraphCompiler The method graph compiler that resolves the functional method of the function interface.
                         * @param <T>                 The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bindLambda(Class<T> type,
                                                                                   MethodDescription.InDefinedShape methodDescription,
                                                                                   TypeDescription functionalInterface,
                                                                                   MethodGraph.Compiler methodGraphCompiler) {
                            if (!functionalInterface.isInterface()) {
                                throw new IllegalArgumentException(functionalInterface + " is not an interface type");
                            }
                            MethodList<?> methods = methodGraphCompiler.compile((TypeDefinition) functionalInterface).listNodes().asMethodList().filter(isAbstract());
                            if (methods.size() != 1) {
                                throw new IllegalArgumentException(functionalInterface + " does not define exactly one abstract method: " + methods);
                            }
                            return bindDynamic(type, new MethodDescription.Latent(new TypeDescription.Latent("java.lang.invoke.LambdaMetafactory",
                                            Opcodes.ACC_PUBLIC,
                                            TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)),
                                            "metafactory",
                                            Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                                            Collections.<TypeVariableToken>emptyList(),
                                            JavaType.CALL_SITE.getTypeStub().asGenericType(),
                                            Arrays.asList(
                                                    new ParameterDescription.Token(JavaType.METHOD_HANDLES_LOOKUP.getTypeStub().asGenericType()),
                                                    new ParameterDescription.Token(TypeDescription.ForLoadedType.of(String.class).asGenericType()),
                                                    new ParameterDescription.Token(JavaType.METHOD_TYPE.getTypeStub().asGenericType()),
                                                    new ParameterDescription.Token(JavaType.METHOD_TYPE.getTypeStub().asGenericType()),
                                                    new ParameterDescription.Token(JavaType.METHOD_HANDLE.getTypeStub().asGenericType()),
                                                    new ParameterDescription.Token(JavaType.METHOD_TYPE.getTypeStub().asGenericType())),
                                            Collections.<TypeDescription.Generic>emptyList(),
                                            Collections.<AnnotationDescription>emptyList(),
                                            AnnotationValue.UNDEFINED,
                                            TypeDescription.Generic.UNDEFINED),
                                    JavaConstant.MethodType.ofSignature(methods.asDefined().getOnly()),
                                    JavaConstant.MethodHandle.of(methodDescription),
                                    JavaConstant.MethodType.ofSignature(methods.asDefined().getOnly()));
                        }

                        /**
                         * Binds the supplied annotation to a dynamically bootstrapped value.
                         *
                         * @param type            The type of the annotation being bound.
                         * @param bootstrapMethod The bootstrap method returning the call site.
                         * @param constant        The arguments supplied to the bootstrap method.
                         * @param <T>             The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bindDynamic(Class<T> type, Method bootstrapMethod, Object... constant) {
                            return bindDynamic(type, bootstrapMethod, Arrays.asList(constant));
                        }

                        /**
                         * Binds the supplied annotation to a dynamically bootstrapped value.
                         *
                         * @param type            The type of the annotation being bound.
                         * @param bootstrapMethod The bootstrap method returning the call site.
                         * @param constants       The arguments supplied to the bootstrap method.
                         * @param <T>             The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bindDynamic(Class<T> type, Method bootstrapMethod, List<?> constants) {
                            return bindDynamic(type, new MethodDescription.ForLoadedMethod(bootstrapMethod), constants);
                        }

                        /**
                         * Binds the supplied annotation to a dynamically bootstrapped value.
                         *
                         * @param type            The type of the annotation being bound.
                         * @param bootstrapMethod The bootstrap constructor returning the call site.
                         * @param constant        The arguments supplied to the bootstrap method.
                         * @param <T>             The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bindDynamic(Class<T> type, Constructor<?> bootstrapMethod, Object... constant) {
                            return bindDynamic(type, bootstrapMethod, Arrays.asList(constant));
                        }

                        /**
                         * Binds the supplied annotation to a dynamically bootstrapped value.
                         *
                         * @param type            The type of the annotation being bound.
                         * @param bootstrapMethod The bootstrap constructor returning the call site.
                         * @param constants       The arguments supplied to the bootstrap method.
                         * @param <T>             The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bindDynamic(Class<T> type, Constructor<?> bootstrapMethod, List<?> constants) {
                            return bindDynamic(type, new MethodDescription.ForLoadedConstructor(bootstrapMethod), constants);
                        }

                        /**
                         * Binds the supplied annotation to a dynamically bootstrapped value.
                         *
                         * @param type            The type of the annotation being bound.
                         * @param bootstrapMethod The bootstrap method or constructor returning the call site.
                         * @param constant        The arguments supplied to the bootstrap method.
                         * @param <T>             The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bindDynamic(Class<T> type, MethodDescription.InDefinedShape bootstrapMethod, Object... constant) {
                            return bindDynamic(type, bootstrapMethod, Arrays.asList(constant));
                        }

                        /**
                         * Binds the supplied annotation to a dynamically bootstrapped value.
                         *
                         * @param type            The type of the annotation being bound.
                         * @param bootstrapMethod The bootstrap method or constructor returning the call site.
                         * @param constants       The arguments supplied to the bootstrap method.
                         * @param <T>             The annotation type.
                         * @return A new builder for a delegate that considers the supplied annotation during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bindDynamic(Class<T> type, MethodDescription.InDefinedShape bootstrapMethod, List<?> constants) {
                            List<JavaConstant> arguments = JavaConstant.Simple.wrap(constants);
                            if (!bootstrapMethod.isInvokeBootstrap(TypeList.Explicit.of(arguments))) {
                                throw new IllegalArgumentException("Not a valid bootstrap method " + bootstrapMethod + " for " + arguments);
                            }
                            return bind(new OffsetMapping.ForStackManipulation.OfDynamicInvocation<T>(type, bootstrapMethod, arguments));
                        }

                        /**
                         * Binds the supplied annotation to the annotation's property of the specified name.
                         *
                         * @param type          The type of the annotation being bound.
                         * @param offsetMapping The offset mapping being bound.
                         * @param <T>           The annotation type.
                         * @return A new builder for a delegation that considers the supplied annotation during binding.
                         */
                        public <T extends Annotation> WithCustomMapping bind(Class<T> type, OffsetMapping offsetMapping) {
                            return bind(new OffsetMapping.Factory.Simple<T>(type, offsetMapping));
                        }

                        /**
                         * Binds an annotation to a dynamically computed value. Whenever the {@link ForDelegation} target discovers the given annotation on
                         * a parameter of an advice method, the dynamic value is asked to provide a value that is then assigned to the parameter in question.
                         *
                         * @param offsetMapping The dynamic value that is computed for binding the parameter to a value.
                         * @return A new builder for a delegation that considers the supplied annotation type during binding.
                         */
                        public WithCustomMapping bind(OffsetMapping.Factory<?> offsetMapping) {
                            Map<Class<? extends Annotation>, OffsetMapping.Factory<?>> offsetMappings = new LinkedHashMap<Class<? extends Annotation>, OffsetMapping.Factory<?>>(this.offsetMappings);
                            if (!offsetMapping.getAnnotationType().isAnnotation()) {
                                throw new IllegalArgumentException("Not an annotation type: " + offsetMapping.getAnnotationType());
                            } else if (offsetMappings.put(offsetMapping.getAnnotationType(), offsetMapping) != null) {
                                throw new IllegalArgumentException("Annotation type already mapped: " + offsetMapping.getAnnotationType());
                            }
                            return new WithCustomMapping(dispatcherFactory, offsetMappings);
                        }

                        /**
                         * Defines the supplied constructor as a dynamic invocation bootstrap target for delegating advice methods. The bootstrap
                         * method arguments are:
                         * <ul>
                         * <li>A {@code java.lang.invoke.MethodHandles.Lookup} representing the source method.</li>
                         * <li>A {@link String} representing the constructor's internal name {@code <init>}.</li>
                         * <li>A {@code java.lang.invoke.MethodType} representing the type that is requested for binding.</li>
                         * <li>A {@link String} representation of the delegate's binary class name.</li>
                         * <li>A {@link Class} representing the receiver type of the substituted element.</li>
                         * <li>A {@link String} representing the internal name of the substituted element.</li>
                         * <li>A {@code java.lang.invoke.MethodHandle} to the substituted element.</li>
                         * <li>A {@link Class} describing the instrumented type.</li>
                         * <li>A {@link String} representing the instrumented method or constructor.</li>
                         * </ul>
                         *
                         * @param constructor The bootstrap constructor.
                         * @return A new builder for a delegation within a member substitution that uses the supplied constructor for bootstrapping.
                         */
                        public WithCustomMapping bootstrap(Constructor<?> constructor) {
                            return bootstrap(new MethodDescription.ForLoadedConstructor(constructor));
                        }

                        /**
                         * Defines the supplied constructor as a dynamic invocation bootstrap target for delegating advice methods.
                         *
                         * @param constructor     The bootstrap method or constructor.
                         * @param resolverFactory A factory for resolving the arguments to the bootstrap method.
                         * @return A new builder for a delegation within a member substitution that uses the supplied constructor for bootstrapping.
                         */
                        public WithCustomMapping bootstrap(Constructor<?> constructor, BootstrapArgumentResolver.Factory resolverFactory) {
                            return bootstrap(new MethodDescription.ForLoadedConstructor(constructor), resolverFactory);
                        }

                        /**
                         * Defines the supplied method as a dynamic invocation bootstrap target for delegating advice methods. The bootstrap
                         * method arguments are:
                         * <ul>
                         * <li>A {@code java.lang.invoke.MethodHandles.Lookup} representing the source method.</li>
                         * <li>A {@link String} representing the method's name.</li>
                         * <li>A {@code java.lang.invoke.MethodType} representing the type that is requested for binding.</li>
                         * <li>A {@link String} representation of the delegate's binary class name.</li>
                         * <li>A {@link Class} representing the receiver type of the substituted element.</li>
                         * <li>A {@link String} representing the internal name of the substituted element.</li>
                         * <li>A {@code java.lang.invoke.MethodHandle} to the substituted element.</li>
                         * <li>A {@link Class} describing the instrumented type.</li>
                         * <li>A {@link String} representing the instrumented method or constructor.</li>
                         * </ul>
                         *
                         * @param method The bootstrap method.
                         * @return A new builder for a delegation within a member substitution that uses the supplied method for bootstrapping.
                         */
                        public WithCustomMapping bootstrap(Method method) {
                            return bootstrap(new MethodDescription.ForLoadedMethod(method));
                        }

                        /**
                         * Defines the supplied method as a dynamic invocation bootstrap target for delegating advice methods.
                         *
                         * @param method          The bootstrap method or constructor.
                         * @param resolverFactory A factory for resolving the arguments to the bootstrap method.
                         * @return A new builder for a delegation within a member substitution that uses the supplied method for bootstrapping.
                         */
                        public WithCustomMapping bootstrap(Method method, BootstrapArgumentResolver.Factory resolverFactory) {
                            return bootstrap(new MethodDescription.ForLoadedMethod(method), resolverFactory);
                        }

                        /**
                         * Defines the supplied method description as a dynamic invocation bootstrap target for delegating advice methods. The bootstrap
                         * method arguments are:
                         * <ul>
                         * <li>A {@code java.lang.invoke.MethodHandles.Lookup} representing the source method.</li>
                         * <li>A {@link String} representing the target's internal name.</li>
                         * <li>A {@code java.lang.invoke.MethodType} representing the type that is requested for binding.</li>
                         * <li>A {@link String} representation of the delegate's binary class name.</li>
                         * <li>A {@link Class} representing the receiver type of the substituted element.</li>
                         * <li>A {@link String} representing the internal name of the substituted element.</li>
                         * <li>A {@code java.lang.invoke.MethodHandle} to the substituted element.</li>
                         * <li>A {@link Class} describing the instrumented type.</li>
                         * <li>A {@link String} representing the instrumented method or constructor.</li>
                         * <li>A method handle of the instrumented method or constructor, only if the instrumented method is not a type initializer.</li>
                         * </ul>
                         *
                         * @param bootstrap The bootstrap method or constructor.
                         * @return A new builder for a delegation within a member substitution that uses the supplied method or constructor for bootstrapping.
                         */
                        public WithCustomMapping bootstrap(MethodDescription.InDefinedShape bootstrap) {
                            return bootstrap(bootstrap, BootstrapArgumentResolver.ForDefaultValues.Factory.INSTANCE);
                        }

                        /**
                         * Defines the supplied method description as a dynamic invocation bootstrap target for delegating advice methods.
                         *
                         * @param bootstrap       The bootstrap method or constructor.
                         * @param resolverFactory A factory for resolving the arguments to the bootstrap method.
                         * @return A new builder for a delegation within a member substitution that uses the supplied method or constructor for bootstrapping.
                         */
                        public WithCustomMapping bootstrap(MethodDescription.InDefinedShape bootstrap, BootstrapArgumentResolver.Factory resolverFactory) {
                            return new WithCustomMapping(Dispatcher.ForDynamicInvocation.of(bootstrap, resolverFactory), offsetMappings);
                        }

                        /**
                         * Returns a delegating step factory for the supplied method.
                         *
                         * @param method The method to delegate to.
                         * @return An appropriate step factory.
                         */
                        public Step.Factory to(Method method) {
                            return to(new MethodDescription.ForLoadedMethod(method));
                        }

                        /**
                         * Returns a delegating step factory for the supplied constructor.
                         *
                         * @param constructor the constructor to delegate to.
                         * @return An appropriate step factory.
                         */
                        public Step.Factory to(Constructor<?> constructor) {
                            return to(new MethodDescription.ForLoadedConstructor(constructor));
                        }

                        /**
                         * Returns a delegating step factory for the supplied method description.
                         *
                         * @param methodDescription A description of the method or constructor to delegate to.
                         * @return An appropriate step factory.
                         */
                        public Step.Factory to(MethodDescription.InDefinedShape methodDescription) {
                            return ForDelegation.to(methodDescription, dispatcherFactory, new ArrayList<OffsetMapping.Factory<?>>(offsetMappings.values()));
                        }
                    }
                }
            }

            /**
             * A factory for creating a substitution chain.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public static class Factory implements Substitution.Factory {

                /**
                 * The assigner to use.
                 */
                private final Assigner assigner;

                /**
                 * The typing of the assignment to use.
                 */
                private final Assigner.Typing typing;

                /**
                 * The substitution steps to apply.
                 */
                private final List<Step.Factory> steps;

                /**
                 * Creates a new factory for a substitution chain.
                 *
                 * @param assigner The assigner to use.
                 * @param typing   The typing of the assignment to use.
                 * @param steps    The substitution steps to apply.
                 */
                protected Factory(Assigner assigner, Assigner.Typing typing, List<Step.Factory> steps) {
                    this.assigner = assigner;
                    this.typing = typing;
                    this.steps = steps;
                }

                /**
                 * {@inheritDoc}
                 */
                public Substitution make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
                    if (steps.isEmpty()) {
                        return Stubbing.INSTANCE;
                    }
                    List<Step> steps = new ArrayList<Step>(this.steps.size());
                    for (Step.Factory step : this.steps) {
                        steps.add(step.make(assigner, typing, instrumentedType, instrumentedMethod));
                    }
                    return new Chain(assigner, typing, steps);
                }

                /**
                 * Appends the supplied steps to the substitution chain.
                 *
                 * @param step The steps to append.
                 * @return A new substitution chain that is equal to this substitution chain but with the supplied steps appended.
                 */
                public Factory executing(Step.Factory... step) {
                    return executing(Arrays.asList(step));
                }

                /**
                 * Appends the supplied steps to the substitution chain.
                 *
                 * @param steps The steps to append.
                 * @return A new substitution chain that is equal to this substitution chain but with the supplied steps appended.
                 */
                public Factory executing(List<? extends Step.Factory> steps) {
                    return new Factory(assigner, typing, CompoundList.of(this.steps, steps));
                }
            }
        }
    }

    /**
     * A replacement combines a {@link Substitution} and a way of choosing if this substitution should be applied for a discovered member.
     */
    protected interface Replacement {

        /**
         * Binds this replacement for a field that was discovered.
         *
         * @param instrumentedType   The instrumented type.
         * @param instrumentedMethod The instrumented method.
         * @param typeDescription    The type on which the field was read.
         * @param fieldDescription   The field that was discovered.
         * @param writeAccess        {@code true} if this field was written to.
         * @return A binding for the discovered field access.
         */
        Binding bind(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypeDescription typeDescription, FieldDescription fieldDescription, boolean writeAccess);

        /**
         * Binds this replacement for a field that was discovered.
         *
         * @param instrumentedType   The instrumented type.FieldDescription
         * @param instrumentedMethod The instrumented method.
         * @param typeDescription    The type on which the method was invoked.
         * @param methodDescription  The method that was discovered.
         * @param invocationType     The invocation type for this method.
         * @return A binding for the discovered method invocation.
         */
        Binding bind(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypeDescription typeDescription, MethodDescription methodDescription, InvocationType invocationType);

        /**
         * A binding for a replacement of a field or method access within another method.
         */
        interface Binding {

            /**
             * Returns {@code true} if this binding is resolved.
             *
             * @return {@code true} if this binding is resolved.
             */
            boolean isBound();

            /**
             * Creates a stack manipulation that represents the substitution. This method can only be called for actually bound bindings.
             *
             * @param parameters        The parameters that are accessible to the substitution target.
             * @param result            The result that is expected from the substitution target or {@code void} if none is expected.
             * @param methodHandle      A method handle that represents the original expression that is being substituted.
             * @param stackManipulation The original byte code expression that is being substituted.
             * @param freeOffset        The first offset that can be used for storing local variables.
             * @return A stack manipulation that represents the replacement.
             */
            StackManipulation make(TypeList.Generic parameters,
                                   TypeDescription.Generic result,
                                   JavaConstant.MethodHandle methodHandle,
                                   StackManipulation stackManipulation,
                                   int freeOffset);

            /**
             * An unresolved binding.
             */
            enum Unresolved implements Binding {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public boolean isBound() {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation make(TypeList.Generic parameters, TypeDescription.Generic result, JavaConstant.MethodHandle methodHandle, StackManipulation stackManipulation, int freeOffset) {
                    throw new IllegalStateException("Cannot resolve unresolved binding");
                }
            }

            /**
             * A binding that was resolved for an actual substitution.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Resolved implements Binding {

                /**
                 * The type on which a field or method was accessed.
                 */
                private final TypeDescription receiver;

                /**
                 * The field or method that was accessed.
                 */
                private final ByteCodeElement.Member original;

                /**
                 * The substitution to apply.
                 */
                private final Substitution substitution;

                /**
                 * Creates a new resolved binding.
                 *
                 * @param receiver     The type on which a field or method was accessed.
                 * @param original     The field or method that was accessed.
                 * @param substitution The substitution to apply.
                 */
                protected Resolved(TypeDescription receiver, ByteCodeElement.Member original, Substitution substitution) {
                    this.receiver = receiver;
                    this.original = original;
                    this.substitution = substitution;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isBound() {
                    return true;
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation make(TypeList.Generic parameters, TypeDescription.Generic result, JavaConstant.MethodHandle methodHandle, StackManipulation stackManipulation, int freeOffset) {
                    return substitution.resolve(receiver, original, parameters, result, methodHandle, stackManipulation, freeOffset);
                }
            }
        }

        /**
         * A factory for creating a replacement for an instrumented method.
         */
        interface Factory {

            /**
             * Creates a replacement for an instrumented method.
             *
             * @param instrumentedType   The instrumented type.
             * @param instrumentedMethod The instrumented method.
             * @param typePool           The type pool being used within the member substitution being applied.
             * @return A replacement to use within the supplied instrumented method.
             */
            Replacement make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool);

            /**
             * A compound factory.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Compound implements Factory {

                /**
                 * A list of represented factories.
                 */
                private final List<Factory> factories;

                /**
                 * Creates a new compound factory.
                 *
                 * @param factory A list of represented factories.
                 */
                protected Compound(Factory... factory) {
                    this(Arrays.asList(factory));
                }

                /**
                 * Creates a new compound factory.
                 *
                 * @param factories A list of represented factories.
                 */
                protected Compound(List<? extends Factory> factories) {
                    this.factories = new ArrayList<Factory>();
                    for (Factory factory : factories) {
                        if (factory instanceof Compound) {
                            this.factories.addAll(((Compound) factory).factories);
                        } else if (!(factory instanceof NoOp)) {
                            this.factories.add(factory);
                        }
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Replacement make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
                    List<Replacement> replacements = new ArrayList<Replacement>();
                    for (Factory factory : factories) {
                        replacements.add(factory.make(instrumentedType, instrumentedMethod, typePool));
                    }
                    return new ForFirstBinding(replacements);
                }
            }
        }

        /**
         * Describes a method invocation type.
         */
        enum InvocationType {

            /**
             * Describes a virtual method invocation.
             */
            VIRTUAL,

            /**
             * Describes a super method invocation.
             */
            SUPER,

            /**
             * Describes any method invocation that is not virtual or a super method invocation.
             */
            OTHER;

            /**
             * Resolves an invocation type.
             *
             * @param opcode            The opcode that is used for invoking the method.
             * @param methodDescription The method that is being invoked.
             * @return The invocation type for the method given that opcode.
             */
            protected static InvocationType of(int opcode, MethodDescription methodDescription) {
                switch (opcode) {
                    case Opcodes.INVOKEVIRTUAL:
                    case Opcodes.INVOKEINTERFACE:
                        return InvocationType.VIRTUAL;
                    case Opcodes.INVOKESPECIAL:
                        return methodDescription.isVirtual() ? SUPER : OTHER;
                    default:
                        return OTHER;
                }
            }

            /**
             * Checks if this invocation type matches the specified inputs.
             *
             * @param includeVirtualCalls {@code true} if a virtual method should be matched.
             * @param includeSuperCalls   {@code true} if a super method call should be matched.
             * @return {@code true} if this invocation type matches the specified parameters.
             */
            protected boolean matches(boolean includeVirtualCalls, boolean includeSuperCalls) {
                switch (this) {
                    case VIRTUAL:
                        return includeVirtualCalls;
                    case SUPER:
                        return includeSuperCalls;
                    default:
                        return true;
                }
            }
        }

        /**
         * A non-operational replacement.
         */
        enum NoOp implements Replacement, Factory {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public Replacement make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
                return this;
            }

            /**
             * {@inheritDoc}
             */
            public Binding bind(TypeDescription instrumentedType,
                                MethodDescription instrumentedMethod,
                                TypeDescription typeDescription,
                                FieldDescription fieldDescription,
                                boolean writeAccess) {
                return Binding.Unresolved.INSTANCE;
            }

            /**
             * {@inheritDoc}
             */
            public Binding bind(TypeDescription instrumentedType,
                                MethodDescription instrumentedMethod,
                                TypeDescription typeDescription,
                                MethodDescription methodDescription,
                                InvocationType invocationType) {
                return Binding.Unresolved.INSTANCE;
            }
        }

        /**
         * A replacement that substitutes a member based on a row of element matchers.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForElementMatchers implements Replacement {

            /**
             * The field matcher to consider when discovering fields.
             */
            private final ElementMatcher<? super FieldDescription> fieldMatcher;

            /**
             * The method matcher to consider when discovering methods.
             */
            private final ElementMatcher<? super MethodDescription> methodMatcher;

            /**
             * {@code true} if field reading access should be matched.
             */
            private final boolean matchFieldRead;

            /**
             * {@code true} if field writing access should be matched.
             */
            private final boolean matchFieldWrite;

            /**
             * {@code true} if virtual method calls should be matched.
             */
            private final boolean includeVirtualCalls;

            /**
             * {@code true} if super method calls should be matched.
             */
            private final boolean includeSuperCalls;

            /**
             * The substitution to trigger if a member is matched.
             */
            private final Substitution substitution;

            /**
             * Creates a new replacement that triggers a substitution based on a row of matchers.
             *
             * @param fieldMatcher        The field matcher to consider when discovering fields.
             * @param methodMatcher       The method matcher to consider when discovering methods.
             * @param matchFieldRead      {@code true} if field reading access should be matched.
             * @param matchFieldWrite     {@code true} if field writing access should be matched.
             * @param includeVirtualCalls {@code true} if virtual method calls should be matched.
             * @param includeSuperCalls   {@code true} if super method calls should be matched.
             * @param substitution        The substitution to trigger if a member is matched.
             */
            protected ForElementMatchers(ElementMatcher<? super FieldDescription> fieldMatcher,
                                         ElementMatcher<? super MethodDescription> methodMatcher,
                                         boolean matchFieldRead,
                                         boolean matchFieldWrite,
                                         boolean includeVirtualCalls,
                                         boolean includeSuperCalls,
                                         Substitution substitution) {
                this.fieldMatcher = fieldMatcher;
                this.methodMatcher = methodMatcher;
                this.matchFieldRead = matchFieldRead;
                this.matchFieldWrite = matchFieldWrite;
                this.includeVirtualCalls = includeVirtualCalls;
                this.includeSuperCalls = includeSuperCalls;
                this.substitution = substitution;
            }

            /**
             * {@inheritDoc}
             */
            public Binding bind(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypeDescription typeDescription, FieldDescription fieldDescription, boolean writeAccess) {
                return (writeAccess ? matchFieldWrite : matchFieldRead) && fieldMatcher.matches(fieldDescription)
                        ? new Binding.Resolved(typeDescription, fieldDescription, substitution)
                        : Binding.Unresolved.INSTANCE;
            }

            /**
             * {@inheritDoc}
             */
            public Binding bind(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypeDescription typeDescription, MethodDescription methodDescription, InvocationType invocationType) {
                return invocationType.matches(includeVirtualCalls, includeSuperCalls) && methodMatcher.matches(methodDescription)
                        ? new Binding.Resolved(typeDescription, methodDescription, substitution)
                        : Binding.Unresolved.INSTANCE;
            }

            /**
             * A factory for creating a replacement that chooses members based on a row of element matchers.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class Factory implements Replacement.Factory {

                /**
                 * The field matcher to consider when discovering fields.
                 */
                private final ElementMatcher<? super FieldDescription> fieldMatcher;

                /**
                 * The method matcher to consider when discovering methods.
                 */
                private final ElementMatcher<? super MethodDescription> methodMatcher;

                /**
                 * {@code true} if field reading access should be matched.
                 */
                private final boolean matchFieldRead;

                /**
                 * {@code true} if field writing access should be matched.
                 */
                private final boolean matchFieldWrite;

                /**
                 * {@code true} if virtual method calls should be matched.
                 */
                private final boolean includeVirtualCalls;

                /**
                 * {@code true} if super method calls should be matched.
                 */
                private final boolean includeSuperCalls;

                /**
                 * The substitution factory to create a substitution from.
                 */
                private final Substitution.Factory substitutionFactory;

                /**
                 * Creates a new replacement that triggers a substitution based on a row of matchers.
                 *
                 * @param fieldMatcher        The field matcher to consider when discovering fields.
                 * @param methodMatcher       The method matcher to consider when discovering methods.
                 * @param matchFieldRead      {@code true} if field reading access should be matched.
                 * @param matchFieldWrite     {@code true} if field writing access should be matched.
                 * @param includeVirtualCalls {@code true} if virtual method calls should be matched.
                 * @param includeSuperCalls   {@code true} if super method calls should be matched.
                 * @param substitutionFactory The substitution factory to create a substitution from.
                 */
                protected Factory(ElementMatcher<? super FieldDescription> fieldMatcher,
                                  ElementMatcher<? super MethodDescription> methodMatcher,
                                  boolean matchFieldRead,
                                  boolean matchFieldWrite,
                                  boolean includeVirtualCalls,
                                  boolean includeSuperCalls,
                                  Substitution.Factory substitutionFactory) {
                    this.fieldMatcher = fieldMatcher;
                    this.methodMatcher = methodMatcher;
                    this.matchFieldRead = matchFieldRead;
                    this.matchFieldWrite = matchFieldWrite;
                    this.includeVirtualCalls = includeVirtualCalls;
                    this.includeSuperCalls = includeSuperCalls;
                    this.substitutionFactory = substitutionFactory;
                }

                /**
                 * Creates a factory for applying a substitution on all matched byte code elements for all access types.
                 *
                 * @param matcher The matcher to apply.
                 * @param factory The substitution factory to create a substitution from.
                 * @return An appropriate replacement factory for the supplied matcher and substitution factory.
                 */
                protected static Replacement.Factory of(ElementMatcher<? super ByteCodeElement.Member> matcher, Substitution.Factory factory) {
                    return new Factory(matcher, matcher, true, true, true, true, factory);
                }

                /**
                 * Creates a factory that only matches field access for given access types.
                 *
                 * @param matcher         The matcher to identify fields for substitution.
                 * @param matchFieldRead  {@code true} if field read access should be matched.
                 * @param matchFieldWrite {@code true} if field write access should be matched.
                 * @param factory         The substitution factory to apply for fields that match the specified criteria.
                 * @return An appropriate replacement factory.
                 */
                protected static Replacement.Factory ofField(ElementMatcher<? super FieldDescription> matcher, boolean matchFieldRead, boolean matchFieldWrite, Substitution.Factory factory) {
                    return new Factory(matcher, none(), matchFieldRead, matchFieldWrite, false, false, factory);
                }

                /**
                 * Creates a factory that only matches method and constructor invocations for given invocation types.
                 *
                 * @param matcher             The matcher to identify methods and constructors for substitution.
                 * @param includeVirtualCalls {@code true} if virtual method calls should be matched.
                 * @param includeSuperCalls   {@code true} if super method calls should be matched.
                 * @param factory             The substitution factory to apply for methods and constructors that match the specified criteria.
                 * @return An appropriate replacement factory.
                 */
                protected static Replacement.Factory ofMethod(ElementMatcher<? super MethodDescription> matcher, boolean includeVirtualCalls, boolean includeSuperCalls, Substitution.Factory factory) {
                    return new Factory(none(), matcher, false, false, includeVirtualCalls, includeSuperCalls, factory);
                }

                /**
                 * {@inheritDoc}
                 */
                public Replacement make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
                    return new ForElementMatchers(fieldMatcher,
                            methodMatcher,
                            matchFieldRead,
                            matchFieldWrite,
                            includeVirtualCalls,
                            includeSuperCalls,
                            substitutionFactory.make(instrumentedType, instrumentedMethod, typePool));
                }
            }
        }

        /**
         * A replacement that only resolves the first matching replacement of a list of replacements.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForFirstBinding implements Replacement {

            /**
             * The list of replacements to consider.
             */
            private final List<? extends Replacement> replacements;

            /**
             * Creates a new replacement that triggers the first matching replacement, if any.
             *
             * @param replacements The list of replacements to consider.
             */
            protected ForFirstBinding(List<? extends Replacement> replacements) {
                this.replacements = replacements;
            }

            /**
             * {@inheritDoc}
             */
            public Binding bind(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypeDescription typeDescription, FieldDescription fieldDescription, boolean writeAccess) {
                for (Replacement replacement : replacements) {
                    Binding binding = replacement.bind(instrumentedType, instrumentedMethod, typeDescription, fieldDescription, writeAccess);
                    if (binding.isBound()) {
                        return binding;
                    }
                }
                return Binding.Unresolved.INSTANCE;
            }

            /**
             * {@inheritDoc}
             */
            public Binding bind(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypeDescription typeDescription, MethodDescription methodDescription, InvocationType invocationType) {
                for (Replacement replacement : replacements) {
                    Binding binding = replacement.bind(instrumentedType, instrumentedMethod, typeDescription, methodDescription, invocationType);
                    if (binding.isBound()) {
                        return binding;
                    }
                }
                return Binding.Unresolved.INSTANCE;
            }
        }
    }

    /**
     * A method visitor that applies a substitution for matched methods.
     */
    protected static class SubstitutingMethodVisitor extends LocalVariableAwareMethodVisitor {

        /**
         * The instrumented type.
         */
        private final TypeDescription instrumentedType;

        /**
         * The instrumented method.
         */
        private final MethodDescription instrumentedMethod;

        /**
         * The method graph compiler to use.
         */
        private final MethodGraph.Compiler methodGraphCompiler;

        /**
         * {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
         */
        private final boolean strict;

        /**
         * {@code true} if the instrumentation should fail if applied to a method without match.
         */
        private final boolean failIfNoMatch;

        /**
         * The replacement to use for creating substitutions.
         */
        private final Replacement replacement;

        /**
         * The implementation context to use.
         */
        private final Implementation.Context implementationContext;

        /**
         * The type pool to use.
         */
        private final TypePool typePool;

        /**
         * If {@code true}, virtual method calls might target private methods in accordance to the nest mate specification.
         */
        private final boolean virtualPrivateCalls;

        /**
         * An additional buffer for the operand stack that is required.
         */
        private int stackSizeBuffer;

        /**
         * The minimum amount of local variable array slots that are required to apply substitutions.
         */
        private int localVariableExtension;

        /**
         * {@code true} if at least one member was substituted during the application of this visitor.
         */
        private boolean matched;

        /**
         * Creates a new substituting method visitor.
         *
         * @param methodVisitor         The method visitor to delegate to.
         * @param instrumentedType      The instrumented type.
         * @param instrumentedMethod    The instrumented method.
         * @param methodGraphCompiler   The method graph compiler to use.
         * @param strict                {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
         * @param failIfNoMatch         {@code true} if the instrumentation should fail if applied to a method without match.
         * @param replacement           The replacement to use for creating substitutions.
         * @param implementationContext The implementation context to use.
         * @param typePool              The type pool to use.
         * @param virtualPrivateCalls   {@code true}, virtual method calls might target private methods in accordance to the nest mate specification.
         */
        protected SubstitutingMethodVisitor(MethodVisitor methodVisitor,
                                            TypeDescription instrumentedType,
                                            MethodDescription instrumentedMethod,
                                            MethodGraph.Compiler methodGraphCompiler,
                                            boolean strict,
                                            boolean failIfNoMatch,
                                            Replacement replacement,
                                            Implementation.Context implementationContext,
                                            TypePool typePool,
                                            boolean virtualPrivateCalls) {
            super(methodVisitor, instrumentedMethod);
            this.instrumentedType = instrumentedType;
            this.instrumentedMethod = instrumentedMethod;
            this.methodGraphCompiler = methodGraphCompiler;
            this.strict = strict;
            this.failIfNoMatch = failIfNoMatch;
            this.replacement = replacement;
            this.implementationContext = implementationContext;
            this.typePool = typePool;
            this.virtualPrivateCalls = virtualPrivateCalls;
            stackSizeBuffer = 0;
            localVariableExtension = 0;
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String internalName, String descriptor) {
            TypePool.Resolution resolution = typePool.describe(owner.replace('/', '.'));
            if (resolution.isResolved()) {
                FieldList<?> candidates;
                Iterator<TypeDefinition> iterator = resolution.resolve().iterator();
                do {
                    candidates = iterator.next().getDeclaredFields().filter(strict
                            ? ElementMatchers.<FieldDescription>named(internalName).and(hasDescriptor(descriptor))
                            : ElementMatchers.<FieldDescription>failSafe(named(internalName).and(hasDescriptor(descriptor))));
                } while (iterator.hasNext() && candidates.isEmpty());
                if (!candidates.isEmpty()) {
                    Replacement.Binding binding = replacement.bind(instrumentedType,
                            instrumentedMethod,
                            resolution.resolve(),
                            candidates.getOnly(),
                            opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC);
                    if (binding.isBound()) {
                        TypeList.Generic parameters;
                        TypeDescription.Generic result;
                        boolean read;
                        switch (opcode) {
                            case Opcodes.PUTFIELD:
                                parameters = new TypeList.Generic.Explicit(candidates.getOnly().getDeclaringType(), candidates.getOnly().getType());
                                result = TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(void.class);
                                read = false;
                                break;
                            case Opcodes.PUTSTATIC:
                                parameters = new TypeList.Generic.Explicit(candidates.getOnly().getType());
                                result = TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(void.class);
                                read = false;
                                break;
                            case Opcodes.GETFIELD:
                                parameters = new TypeList.Generic.Explicit(candidates.getOnly().getDeclaringType());
                                result = candidates.getOnly().getType();
                                read = true;
                                break;
                            case Opcodes.GETSTATIC:
                                parameters = new TypeList.Generic.Empty();
                                result = candidates.getOnly().getType();
                                read = true;
                                break;
                            default:
                                throw new IllegalStateException("Unexpected opcode: " + opcode);
                        }
                        stackSizeBuffer = Math.max(stackSizeBuffer, binding.make(parameters,
                                result,
                                read
                                        ? JavaConstant.MethodHandle.ofGetter(candidates.getOnly().asDefined())
                                        : JavaConstant.MethodHandle.ofSetter(candidates.getOnly().asDefined()),
                                read
                                        ? FieldAccess.forField(candidates.getOnly()).read()
                                        : FieldAccess.forField(candidates.getOnly()).write(),
                                getFreeOffset()).apply(new LocalVariableTracingMethodVisitor(mv), implementationContext).getMaximalSize());
                        matched = true;
                        return;
                    }
                } else if (strict) {
                    throw new IllegalStateException("Could not resolve " + owner.replace('/', '.') + "." + internalName + descriptor + " using " + typePool);
                }
            } else if (strict) {
                throw new IllegalStateException("Could not resolve " + owner.replace('/', '.') + " using " + typePool);
            }
            super.visitFieldInsn(opcode, owner, internalName, descriptor);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String internalName, String descriptor, boolean isInterface) {
            TypePool.Resolution resolution = typePool.describe(owner.replace('/', '.'));
            if (resolution.isResolved()) {
                MethodList<?> candidates;
                if (opcode == Opcodes.INVOKESPECIAL && internalName.equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME)) {
                    candidates = resolution.resolve().getDeclaredMethods().filter(strict
                            ? ElementMatchers.<MethodDescription>isConstructor().and(hasDescriptor(descriptor))
                            : ElementMatchers.<MethodDescription>failSafe(isConstructor().and(hasDescriptor(descriptor))));
                } else if (opcode == Opcodes.INVOKESTATIC) {
                    Iterator<TypeDefinition> iterator = resolution.resolve().iterator();
                    do {
                        candidates = iterator.next().getDeclaredMethods().filter(strict
                                ? ElementMatchers.<MethodDescription>named(internalName).and(hasDescriptor(descriptor))
                                : ElementMatchers.<MethodDescription>failSafe(named(internalName).and(hasDescriptor(descriptor))));
                    } while (iterator.hasNext() && candidates.isEmpty());
                } else if (opcode == Opcodes.INVOKESPECIAL) {
                    candidates = resolution.resolve().getDeclaredMethods().filter(strict
                            ? ElementMatchers.<MethodDescription>named(internalName).and(hasDescriptor(descriptor))
                            : ElementMatchers.<MethodDescription>failSafe(named(internalName).and(hasDescriptor(descriptor))));
                } else if (virtualPrivateCalls) {
                    candidates = resolution.resolve().getDeclaredMethods().filter(strict
                            ? ElementMatchers.<MethodDescription>isPrivate().and(not(isStatic())).and(named(internalName).and(hasDescriptor(descriptor)))
                            : ElementMatchers.<MethodDescription>failSafe(isPrivate().<MethodDescription>and(not(isStatic())).and(named(internalName).and(hasDescriptor(descriptor)))));
                    if (candidates.isEmpty()) {
                        candidates = methodGraphCompiler.compile((TypeDefinition) resolution.resolve(), instrumentedType).listNodes().asMethodList().filter(strict
                                ? ElementMatchers.<MethodDescription>named(internalName).and(hasDescriptor(descriptor))
                                : ElementMatchers.<MethodDescription>failSafe(named(internalName).and(hasDescriptor(descriptor))));
                    }
                } else {
                    candidates = methodGraphCompiler.compile((TypeDefinition) resolution.resolve(), instrumentedType).listNodes().asMethodList().filter(strict
                            ? ElementMatchers.<MethodDescription>named(internalName).and(hasDescriptor(descriptor))
                            : ElementMatchers.<MethodDescription>failSafe(named(internalName).and(hasDescriptor(descriptor))));
                }
                if (!candidates.isEmpty()) {
                    Replacement.Binding binding = replacement.bind(instrumentedType,
                            instrumentedMethod,
                            resolution.resolve(),
                            candidates.getOnly(),
                            Replacement.InvocationType.of(opcode, candidates.getOnly()));
                    if (binding.isBound()) {
                        StackManipulation.Size size = binding.make(
                                candidates.getOnly().isStatic() || candidates.getOnly().isConstructor()
                                        ? candidates.getOnly().getParameters().asTypeList()
                                        : new TypeList.Generic.Explicit(CompoundList.of(resolution.resolve(), candidates.getOnly().getParameters().asTypeList())),
                                candidates.getOnly().isConstructor()
                                        ? candidates.getOnly().getDeclaringType().asGenericType()
                                        : candidates.getOnly().getReturnType(),
                                opcode == Opcodes.INVOKESPECIAL && candidates.getOnly().isMethod() && !candidates.getOnly().isPrivate()
                                        ? JavaConstant.MethodHandle.ofSpecial(candidates.getOnly().asDefined(), resolution.resolve())
                                        : JavaConstant.MethodHandle.of(candidates.getOnly().asDefined()),
                                opcode == Opcodes.INVOKESPECIAL && candidates.getOnly().isMethod() && !candidates.getOnly().isPrivate()
                                        ? MethodInvocation.invoke(candidates.getOnly()).special(resolution.resolve())
                                        : MethodInvocation.invoke(candidates.getOnly()), getFreeOffset()).apply(new LocalVariableTracingMethodVisitor(mv), implementationContext);
                        if (candidates.getOnly().isConstructor()) {
                            stackSizeBuffer = Math.max(stackSizeBuffer, size.getMaximalSize() + 2);
                            stackSizeBuffer = Math.max(stackSizeBuffer, new StackManipulation.Compound(Duplication.SINGLE.flipOver(TypeDescription.ForLoadedType.of(Object.class)),
                                    Removal.SINGLE,
                                    Removal.SINGLE,
                                    Duplication.SINGLE.flipOver(TypeDescription.ForLoadedType.of(Object.class)),
                                    Removal.SINGLE,
                                    Removal.SINGLE).apply(mv, implementationContext).getMaximalSize() + StackSize.SINGLE.getSize());
                        } else {
                            stackSizeBuffer = Math.max(stackSizeBuffer, size.getMaximalSize());
                        }
                        matched = true;
                        return;
                    }
                } else if (strict) {
                    throw new IllegalStateException("Could not resolve " + owner.replace('/', '.') + "." + internalName + descriptor + " using " + typePool);
                }
            } else if (strict) {
                throw new IllegalStateException("Could not resolve " + owner.replace('/', '.') + " using " + typePool);
            }
            super.visitMethodInsn(opcode, owner, internalName, descriptor, isInterface);
        }

        @Override
        public void visitMaxs(int stackSize, int localVariableLength) {
            if (failIfNoMatch && !matched) {
                throw new IllegalStateException("No substitution found within " + instrumentedMethod + " of " + instrumentedType);
            }
            super.visitMaxs(stackSize + stackSizeBuffer, Math.max(localVariableExtension, localVariableLength));
        }

        /**
         * A method visitor that traces offsets of the local variable array being used.
         */
        private class LocalVariableTracingMethodVisitor extends MethodVisitor {

            /**
             * Creates a new local variable tracing method visitor.
             *
             * @param methodVisitor The method visitor to delegate to.
             */
            private LocalVariableTracingMethodVisitor(MethodVisitor methodVisitor) {
                super(OpenedClassReader.ASM_API, methodVisitor);
            }

            @Override
            @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT", justification = "No action required on default option.")
            public void visitVarInsn(int opcode, int offset) {
                switch (opcode) {
                    case Opcodes.ISTORE:
                    case Opcodes.FSTORE:
                    case Opcodes.ASTORE:
                        localVariableExtension = Math.max(localVariableExtension, offset + 1);
                        break;
                    case Opcodes.LSTORE:
                    case Opcodes.DSTORE:
                        localVariableExtension = Math.max(localVariableExtension, offset + 2);
                        break;
                }
                super.visitVarInsn(opcode, offset);
            }
        }
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to the {@code this} reference of the substituted field,
     * method, constructor or of the instrumented method.
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.implementation.bind.annotation.This} or
     * {@link net.bytebuddy.asm.Advice.This}. This annotation should be used only in combination with {@link Substitution.Chain.Step.ForDelegation}.
     * </p>
     *
     * @see Substitution.Chain.Step.ForDelegation
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.CONSTRUCTOR})
    public @interface This {

        /**
         * The typing that should be applied when assigning the {@code this} value.
         *
         * @return The typing to apply upon assignment.
         */
        Assigner.Typing typing() default Assigner.Typing.STATIC;

        /**
         * Determines the source that is considered for this annotation which can be either the substituted method,
         * constructor or field, or the instrumented method.
         *
         * @return The source that is considered for this annotation.
         */
        Source source() default Source.SUBSTITUTED_ELEMENT;

        /**
         * Determines if the parameter should be assigned {@code null} if no {@code this} parameter is available.
         *
         * @return {@code true} if the value assignment is optional.
         */
        boolean optional() default false;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to the parameter with index {@link Argument#value()}.
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.implementation.bind.annotation.Argument} or
     * {@link net.bytebuddy.asm.Advice.Argument}. This annotation should be used only in combination with {@link Substitution.Chain.Step.ForDelegation}.
     * </p>
     *
     * @see Substitution.Chain.Step.ForDelegation
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.CONSTRUCTOR})
    public @interface Argument {

        /**
         * Determines the index of the parameter that is being assigned.
         *
         * @return The index of the parameter that is being assigned.
         */
        int value();

        /**
         * The typing that should be applied when assigning the argument.
         *
         * @return The typing to apply upon assignment.
         */
        Assigner.Typing typing() default Assigner.Typing.STATIC;

        /**
         * Determines the source that is considered for this annotation which can be either the substituted method,
         * constructor or field, or the instrumented method.
         *
         * @return The source that is considered for this annotation.
         */
        Source source() default Source.SUBSTITUTED_ELEMENT;

        /**
         * Determines if the parameter should be assigned {@code null} if no argument with the specified index is available.
         *
         * @return {@code true} if the value assignment is optional.
         */
        boolean optional() default false;
    }

    /**
     * <p>
     * Assigns an array containing all arguments of the targeted element to the annotated parameter. The annotated parameter must
     * be an array type.
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.implementation.bind.annotation.AllArguments} or
     * {@link net.bytebuddy.asm.Advice.AllArguments}. This annotation should be used only in combination with {@link Substitution.Chain.Step.ForDelegation}.
     * </p>
     *
     * @see Substitution.Chain.Step.ForDelegation
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface AllArguments {

        /**
         * The typing that should be applied when assigning the arguments to an array element.
         *
         * @return The typing to apply upon assignment.
         */
        Assigner.Typing typing() default Assigner.Typing.STATIC;

        /**
         * Determines the source that is considered for this annotation which can be either the substituted method,
         * constructor or field, or the instrumented method.
         *
         * @return The source that is considered for this annotation.
         */
        Source source() default Source.SUBSTITUTED_ELEMENT;

        /**
         * Determines if the produced array should include the instrumented method's target reference within the array, if
         * the targeted element is non-static.
         *
         * @return {@code true} if a possible {@code this} reference should be included in the assigned array.
         */
        boolean includeSelf() default false;

        /**
         * Determines if {@code null} should be assigned to the annotated parameter to the annotated parameter.
         *
         * @return {@code true} if {@code null} should be assigned to the annotated parameter to the annotated parameter.
         */
        boolean nullIfEmpty() default false;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should load a {@code java.lang.invoke.MethodHandle} that represents an invocation of
     * the substituted expression or instrumented method. If the current method is virtual, it is bound to the current instance such
     * that the virtual hierarchy is avoided. This annotation cannot be used to acquire a handle on enclosing constructors.
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.asm.Advice.SelfCallHandle}. This annotation should
     * be used only in combination with {@link Substitution.Chain.Step.ForDelegation}.
     * </p>
     *
     * @see Substitution.Chain.Step.ForDelegation
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface SelfCallHandle {

        /**
         * Determines the source that is considered for this annotation which can be either the substituted method,
         * constructor or field, or the instrumented method.
         *
         * @return The source that is considered for this annotation.
         */
        Source source() default Source.SUBSTITUTED_ELEMENT;

        /**
         * Determines if the method is bound to the arguments and instance of the represented invocation.
         *
         * @return {@code true} if the handle should be bound to the current arguments.
         */
        boolean bound() default true;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to a field in the scope of the instrumented type.
     * </p>
     * <p>
     * Setting {@link FieldValue#value()} is optional. If the value is not set, the field value attempts to bind a setter's
     * or getter's field if the intercepted method is an accessor method. Otherwise, the binding renders the target method
     * to be an illegal candidate for binding.
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.implementation.bind.annotation.FieldValue} or
     * {@link net.bytebuddy.asm.Advice.FieldValue}. This annotation should be used only in combination with {@link Substitution.Chain.Step.ForDelegation}.
     * </p>
     *
     * @see Substitution.Chain.Step.ForDelegation
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.CONSTRUCTOR})
    public @interface FieldValue {

        /**
         * Returns the name of the field.
         *
         * @return The name of the field.
         */
        String value() default Substitution.Chain.Step.ForDelegation.OffsetMapping.ForField.Unresolved.BEAN_PROPERTY;

        /**
         * Returns the type that declares the field that should be mapped to the annotated parameter. If this property
         * is set to {@code void}, the field is looked up implicitly within the instrumented class's class hierarchy.
         * The value can also be set to {@link TargetType} in order to look up the type on the instrumented type.
         *
         * @return The type that declares the field, {@code void} if this type should be determined implicitly or
         * {@link TargetType} for the instrumented type.
         */
        Class<?> declaringType() default void.class;

        /**
         * The typing that should be applied when assigning the field value.
         *
         * @return The typing to apply upon assignment.
         */
        Assigner.Typing typing() default Assigner.Typing.STATIC;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to a {@code java.lang.invoke.MethodHandle} representing a field getter.
     * </p>
     * <p>
     * Setting {@link FieldValue#value()} is optional. If the value is not set, the field value attempts to bind a setter's
     * or getter's field if the intercepted method is an accessor method. Otherwise, the binding renders the target method
     * to be an illegal candidate for binding.
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.implementation.bind.annotation.FieldGetterHandle} or
     * {@link net.bytebuddy.asm.Advice.FieldGetterHandle}. This annotation should be used only in combination with {@link Substitution.Chain.Step.ForDelegation}.
     * </p>
     *
     * @see Substitution.Chain.Step.ForDelegation
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface FieldGetterHandle {

        /**
         * Returns the name of the field.
         *
         * @return The name of the field.
         */
        String value() default Substitution.Chain.Step.ForDelegation.OffsetMapping.ForFieldHandle.Unresolved.BEAN_PROPERTY;

        /**
         * Returns the type that declares the field that should be mapped to the annotated parameter. If this property
         * is set to {@code void}, the field is looked up implicitly within the instrumented class's class hierarchy.
         * The value can also be set to {@link TargetType} in order to look up the type on the instrumented type.
         *
         * @return The type that declares the field, {@code void} if this type should be determined implicitly or
         * {@link TargetType} for the instrumented type.
         */
        Class<?> declaringType() default void.class;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to a {@code java.lang.invoke.MethodHandle} representing a field setter.
     * </p>
     * <p>
     * Setting {@link FieldValue#value()} is optional. If the value is not set, the field value attempts to bind a setter's
     * or getter's field if the intercepted method is an accessor method. Otherwise, the binding renders the target method
     * to be an illegal candidate for binding.
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.implementation.bind.annotation.FieldSetterHandle} or
     * {@link net.bytebuddy.asm.Advice.FieldSetterHandle}. This annotation should be used only in combination with {@link Substitution.Chain.Step.ForDelegation}.
     * </p>
     *
     * @see Substitution.Chain.Step.ForDelegation
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface FieldSetterHandle {

        /**
         * Returns the name of the field.
         *
         * @return The name of the field.
         */
        String value() default Substitution.Chain.Step.ForDelegation.OffsetMapping.ForFieldHandle.Unresolved.BEAN_PROPERTY;

        /**
         * Returns the type that declares the field that should be mapped to the annotated parameter. If this property
         * is set to {@code void}, the field is looked up implicitly within the instrumented class's class hierarchy.
         * The value can also be set to {@link TargetType} in order to look up the type on the instrumented type.
         *
         * @return The type that declares the field, {@code void} if this type should be determined implicitly or
         * {@link TargetType} for the instrumented type.
         */
        Class<?> declaringType() default void.class;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to a representation of the substituted element or
     * instrumented method. This representation can be a string representation, a constant representing
     * the {@link Class}, a {@link Method}, {@link Constructor} or {@code java.lang.reflect.Executable}. It can also load
     * a {@code java.lang.invoke.MethodType}, a {@code java.lang.invoke.MethodHandle} or a {@code java.lang.invoke.MethodHandles$Lookup}.
     * </p>
     * <p>
     * <b>Note</b>: A constant representing a {@link Method} or {@link Constructor} is not cached but is recreated for
     * every delegation.
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.implementation.bind.annotation.Origin} or
     * {@link Advice.Origin}. This annotation should be used only in combination with {@link Substitution.Chain.Step.ForDelegation}.
     * </p>
     *
     * @see Substitution.Chain.Step.ForDelegation
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface Origin {

        /**
         * Determines the source that is considered for this annotation which can be either the substituted method,
         * constructor or field, or the instrumented method.
         *
         * @return The source that is considered for this annotation.
         */
        Source source() default Source.SUBSTITUTED_ELEMENT;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should always return a default value (i.e. {@code 0} for numeric values, {@code false}
     * for {@code boolean} types and {@code null} for reference types).
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.implementation.bind.annotation.Empty} or
     * {@link Advice.Unused}. This annotation should be used only in combination with {@link Substitution.Chain.Step.ForDelegation}.
     * </p>
     *
     * @see Substitution.Chain.Step.ForDelegation
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface Unused {
        /* empty */
    }

    /**
     * <p>
     * Indicates that the annotated parameter should always return a boxed version of the instrumented method's return value
     * (i.e. {@code 0} for numeric values, {@code false} for {@code boolean} types and {@code null} for reference types). The annotated
     * parameter must be of type {@link Object}.
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.implementation.bind.annotation.StubValue} or
     * {@link Advice.StubValue}. This annotation should be used only in combination with {@link Substitution.Chain.Step.ForDelegation}.
     * </p>
     *
     * @see Substitution.Chain.Step.ForDelegation
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface StubValue {

        /**
         * Determines the source that is considered for this annotation which can be either the substituted method,
         * constructor or field, or the instrumented method.
         *
         * @return The source that is considered for this annotation.
         */
        Source source() default Source.SUBSTITUTED_ELEMENT;
    }

    /**
     * Indicates that the annotated parameter should be assigned the value of the result that was
     * yielded by the previous chain expression.
     *
     * @see Substitution.Chain.Step.ForDelegation
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.CONSTRUCTOR})
    public @interface Current {

        /**
         * The typing that should be applied when assigning the latest stack value.
         *
         * @return The typing to apply upon assignment.
         */
        Assigner.Typing typing() default Assigner.Typing.STATIC;
    }

    /**
     * Identifies the source of an instruction that might describe a value of the substituted element
     * or the instrumented method.
     */
    public enum Source {

        /**
         * Indicates that an element should be loaded in context of the substituted method, constructor or field.
         */
        SUBSTITUTED_ELEMENT {
            @Override
            protected ByteCodeElement.Member element(ByteCodeElement.Member original, MethodDescription instrumentedMethod) {
                return original;
            }

            @Override
            @MaybeNull
            protected Source.Value self(TypeList.Generic parameters, Map<Integer, Integer> offsets, ByteCodeElement.Member original, MethodDescription instrumentedMethod) {
                return original.isStatic()
                        ? null
                        : new Source.Value(parameters.get(THIS_REFERENCE), offsets.get(THIS_REFERENCE));
            }

            @Override
            @MaybeNull
            protected Source.Value argument(int index, TypeList.Generic parameters, Map<Integer, Integer> offsets, ByteCodeElement.Member original, MethodDescription instrumentedMethod) {
                return index < parameters.size() - (original.isStatic() ? 0 : 1)
                        ? new Source.Value(parameters.get(index + (original.isStatic() ? 0 : 1)), offsets.get(index + (original.isStatic() ? 0 : 1)))
                        : null;
            }

            @Override
            protected List<Source.Value> arguments(boolean includesSelf,
                                                   TypeList.Generic parameters,
                                                   Map<Integer, Integer> offsets,
                                                   ByteCodeElement.Member original,
                                                   MethodDescription instrumentedMethod) {
                List<Source.Value> values = new ArrayList<Source.Value>(parameters.size() - (!includesSelf && !original.isStatic() ? 1 : 0));
                for (int index = original.isStatic() || includesSelf ? 0 : 1; index < parameters.size(); index++) {
                    values.add(new Source.Value(parameters.get(index), offsets.get(index)));
                }
                return values;
            }

            @Override
            protected JavaConstant.MethodHandle handle(JavaConstant.MethodHandle methodHandle, MethodDescription instrumentedMethod) {
                return methodHandle;
            }

            @Override
            protected boolean isRepresentable(Substitution.Chain.Step.ForDelegation.OffsetMapping.ForOrigin.Sort sort, ByteCodeElement.Member original, MethodDescription instrumentedMethod) {
                return sort.isRepresentable(original);
            }

            @Override
            protected StackManipulation resolve(Substitution.Chain.Step.ForDelegation.OffsetMapping.ForOrigin.Sort sort,
                                                ByteCodeElement.Member original,
                                                TypeList.Generic parameters,
                                                TypeDescription.Generic result,
                                                MethodDescription instrumentedMethod) {
                return sort.resolve(original, parameters.asErasures(), result.asErasure());
            }
        },

        /**
         * Indicates that an element should be loaded in context of the instrumented method.
         */
        ENCLOSING_METHOD {
            @Override
            protected ByteCodeElement.Member element(ByteCodeElement.Member original, MethodDescription instrumentedMethod) {
                return instrumentedMethod;
            }

            @Override
            @MaybeNull
            protected Source.Value self(TypeList.Generic parameters, Map<Integer, Integer> offsets, ByteCodeElement.Member original, MethodDescription instrumentedMethod) {
                return instrumentedMethod.isStatic()
                        ? null
                        : new Source.Value(instrumentedMethod.getDeclaringType().asGenericType(), THIS_REFERENCE);
            }

            @Override
            @MaybeNull
            protected Source.Value argument(int index, TypeList.Generic parameters, Map<Integer, Integer> offsets, ByteCodeElement.Member original, MethodDescription instrumentedMethod) {
                if (index < instrumentedMethod.getParameters().size()) {
                    ParameterDescription parameterDescription = instrumentedMethod.getParameters().get(index);
                    return new Source.Value(parameterDescription.getType(), parameterDescription.getOffset());
                } else {
                    return null;
                }
            }

            @Override
            protected List<Source.Value> arguments(boolean includesSelf, TypeList.Generic parameters, Map<Integer, Integer> offsets, ByteCodeElement.Member original, MethodDescription instrumentedMethod) {
                List<Source.Value> values;
                if (includesSelf && !instrumentedMethod.isStatic()) {
                    values = new ArrayList<Source.Value>(instrumentedMethod.getParameters().size() + 1);
                    values.add(new Source.Value(instrumentedMethod.getDeclaringType().asGenericType(), THIS_REFERENCE));
                } else {
                    values = new ArrayList<Source.Value>(instrumentedMethod.getParameters().size());
                }
                for (ParameterDescription parameterDescription : instrumentedMethod.getParameters()) {
                    values.add(new Source.Value(parameterDescription.getType(), parameterDescription.getOffset()));
                }
                return values;
            }

            @Override
            protected JavaConstant.MethodHandle handle(JavaConstant.MethodHandle methodHandle, MethodDescription instrumentedMethod) {
                return JavaConstant.MethodHandle.of(instrumentedMethod.asDefined());
            }

            @Override
            protected boolean isRepresentable(Substitution.Chain.Step.ForDelegation.OffsetMapping.ForOrigin.Sort sort, ByteCodeElement.Member original, MethodDescription instrumentedMethod) {
                return sort.isRepresentable(instrumentedMethod);
            }

            @Override
            protected StackManipulation resolve(Substitution.Chain.Step.ForDelegation.OffsetMapping.ForOrigin.Sort sort,
                                                ByteCodeElement.Member original,
                                                TypeList.Generic parameters,
                                                TypeDescription.Generic result,
                                                MethodDescription instrumentedMethod) {
                return sort.resolve(instrumentedMethod,
                        instrumentedMethod.isStatic() || instrumentedMethod.isConstructor()
                                ? instrumentedMethod.getParameters().asTypeList().asErasures()
                                : CompoundList.of(instrumentedMethod.getDeclaringType().asErasure(), instrumentedMethod.getParameters().asTypeList().asErasures()),
                        instrumentedMethod.isConstructor()
                                ? instrumentedMethod.getDeclaringType().asErasure()
                                : instrumentedMethod.getReturnType().asErasure());
            }
        };

        /**
         * Resolves the targeted byte code element.
         *
         * @param original           The substituted element.
         * @param instrumentedMethod The instrumented element.
         * @return The byte code element that is represented by this source.
         */
        protected abstract ByteCodeElement.Member element(ByteCodeElement.Member original, MethodDescription instrumentedMethod);

        /**
         * Resolves a value representation of the {@code this} reference or {@code null} if no such reference is available.
         *
         * @param parameters         The list of parameters of the substituted element.
         * @param offsets            A mapping of offsets of parameter indices to offsets.
         * @param original           The substituted element.
         * @param instrumentedMethod The instrumented method.
         * @return A representation of the {@code this} reference or {@code null} if no such reference is available.
         */
        @MaybeNull
        protected abstract Source.Value self(TypeList.Generic parameters, Map<Integer, Integer> offsets, ByteCodeElement.Member original, MethodDescription instrumentedMethod);

        /**
         * Resolves a value representation of the parameter of the specified index or {@code null} if no such parameter is available.
         *
         * @param index              The index of the targeted parameter.
         * @param parameters         The list of parameters of the substituted element.
         * @param offsets            A mapping of offsets of parameter indices to offsets.
         * @param original           The substituted element.
         * @param instrumentedMethod The instrumented method.
         * @return A representation of the parameter of the specified index or {@code null} if no such parameter is available.
         */
        @MaybeNull
        protected abstract Source.Value argument(int index, TypeList.Generic parameters, Map<Integer, Integer> offsets, ByteCodeElement.Member original, MethodDescription instrumentedMethod);

        /**
         * Resolves a list of value representation of all parameters.
         *
         * @param includesSelf       {@code true} if the {@code this} reference should be included if available.
         * @param parameters         The list of parameters of the substituted element.
         * @param offsets            A mapping of offsets of parameter indices to offsets.
         * @param original           The substituted element.
         * @param instrumentedMethod The instrumented method.
         * @return A list of representation of all values of all parameters.
         */
        protected abstract List<Source.Value> arguments(boolean includesSelf, TypeList.Generic parameters, Map<Integer, Integer> offsets, ByteCodeElement.Member original, MethodDescription instrumentedMethod);

        /**
         * Resolves a method handle.
         *
         * @param methodHandle       A method handle of the substituted element.
         * @param instrumentedMethod The instrumented method.
         * @return An appropriate method handle.
         */
        protected abstract JavaConstant.MethodHandle handle(JavaConstant.MethodHandle methodHandle, MethodDescription instrumentedMethod);

        /**
         * Validates if the supplied origin sort is representable.
         *
         * @param sort               The sort of origin.
         * @param original           The substituted element.
         * @param instrumentedMethod The instrumented method.
         * @return {@code true} if the supplied sort of origin is representable.
         */
        protected abstract boolean isRepresentable(Substitution.Chain.Step.ForDelegation.OffsetMapping.ForOrigin.Sort sort, ByteCodeElement.Member original, MethodDescription instrumentedMethod);

        /**
         * Resolves a stack manipulation that loads the supplied sort of origin onto the operand stack.
         *
         * @param sort               The sort of origin.
         * @param original           The substituted element.
         * @param parameters         The parameters to the substituted element.
         * @param result             The type upon which the substituted element is invoked.
         * @param instrumentedMethod The instrumented method.
         * @return A stack manipulation loading the supplied sort of origin onto the operand stack.
         */
        protected abstract StackManipulation resolve(Substitution.Chain.Step.ForDelegation.OffsetMapping.ForOrigin.Sort sort,
                                                     ByteCodeElement.Member original,
                                                     TypeList.Generic parameters,
                                                     TypeDescription.Generic result,
                                                     MethodDescription instrumentedMethod);

        /**
         * Represents a value that can be loaded from a given offset.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class Value {

            /**
             * The type of the loaded value.
             */
            private final TypeDescription.Generic typeDescription;

            /**
             * The offset of the loaded value.
             */
            private final int offset;

            /**
             * Creates a value representation.
             *
             * @param typeDescription The type of the loaded value.
             * @param offset          The offset of the loaded value.
             */
            protected Value(TypeDescription.Generic typeDescription, int offset) {
                this.typeDescription = typeDescription;
                this.offset = offset;
            }

            /**
             * Returns the type of the loaded value.
             *
             * @return The type of the loaded value.
             */
            protected TypeDescription.Generic getTypeDescription() {
                return typeDescription;
            }

            /**
             * Returns the offset of the loaded value.
             *
             * @return The offset of the loaded value.
             */
            protected int getOffset() {
                return offset;
            }
        }
    }
}
