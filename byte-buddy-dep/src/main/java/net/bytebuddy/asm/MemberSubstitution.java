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
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.Removal;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.DefaultValue;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.CompoundList;
import net.bytebuddy.utility.OpenedClassReader;
import net.bytebuddy.utility.visitor.LocalVariableAwareMethodVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * <p>
 * Substitutes field access or method invocations within a method's body.
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
     * The method graph compiler to use.
     */
    private final MethodGraph.Compiler methodGraphCompiler;

    /**
     * {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
     */
    private final boolean strict;

    /**
     * The type pool resolver to use.
     */
    private final TypePoolResolver typePoolResolver;

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
        this(MethodGraph.Compiler.DEFAULT, TypePoolResolver.OfImplicitPool.INSTANCE, strict, Replacement.NoOp.INSTANCE);
    }

    /**
     * Creates a new member substitution.
     *
     * @param methodGraphCompiler The method graph compiler to use.
     * @param typePoolResolver    The type pool resolver to use.
     * @param strict              {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
     * @param replacementFactory  The replacement factory to use.
     */
    protected MemberSubstitution(MethodGraph.Compiler methodGraphCompiler,
                                 TypePoolResolver typePoolResolver,
                                 boolean strict,
                                 Replacement.Factory replacementFactory) {
        this.methodGraphCompiler = methodGraphCompiler;
        this.typePoolResolver = typePoolResolver;
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
    public WithoutSpecification element(ElementMatcher<? super ByteCodeElement> matcher) {
        return new WithoutSpecification.ForMatchedByteCodeElement(methodGraphCompiler, typePoolResolver, strict, replacementFactory, matcher);
    }

    /**
     * Substitutes any field access that matches the given matcher.
     *
     * @param matcher The matcher to determine what fields to substitute.
     * @return A specification that allows to determine how to substitute any field access that match the supplied matcher.
     */
    public WithoutSpecification.ForMatchedField field(ElementMatcher<? super FieldDescription.InDefinedShape> matcher) {
        return new WithoutSpecification.ForMatchedField(methodGraphCompiler, typePoolResolver, strict, replacementFactory, matcher);
    }

    /**
     * Substitutes any method invocation that matches the given matcher.
     *
     * @param matcher The matcher to determine what methods to substitute.
     * @return A specification that allows to determine how to substitute any method invocations that match the supplied matcher.
     */
    public WithoutSpecification.ForMatchedMethod method(ElementMatcher<? super MethodDescription> matcher) {
        return new WithoutSpecification.ForMatchedMethod(methodGraphCompiler, typePoolResolver, strict, replacementFactory, matcher);
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
        return new WithoutSpecification.ForMatchedMethod(methodGraphCompiler, typePoolResolver, strict, replacementFactory, matcher);
    }

    /**
     * Specifies the use of a specific method graph compiler for the resolution of virtual methods.
     *
     * @param methodGraphCompiler The method graph compiler to use.
     * @return A new member substitution that is equal to this but uses the specified method graph compiler.
     */
    public MemberSubstitution with(MethodGraph.Compiler methodGraphCompiler) {
        return new MemberSubstitution(methodGraphCompiler, typePoolResolver, strict, replacementFactory);
    }

    /**
     * Specifies a type pool resolver to be used for locating members.
     *
     * @param typePoolResolver The type pool resolver to use.
     * @return A new instance of this member substitution that uses the supplied type pool resolver.
     */
    public MemberSubstitution with(TypePoolResolver typePoolResolver) {
        return new MemberSubstitution(methodGraphCompiler, typePoolResolver, strict, replacementFactory);
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
         * The replacement factory to use for creating substitutions.
         */
        protected final Replacement.Factory replacementFactory;

        /**
         * Creates a new member substitution that requires a specification for how to perform a substitution.
         *
         * @param methodGraphCompiler The method graph compiler to use.
         * @param typePoolResolver    The type pool resolver to use.
         * @param strict              {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
         * @param replacementFactory  The replacement factory to use for creating substitutions.
         */
        protected WithoutSpecification(MethodGraph.Compiler methodGraphCompiler,
                                       TypePoolResolver typePoolResolver,
                                       boolean strict,
                                       Replacement.Factory replacementFactory) {
            this.methodGraphCompiler = methodGraphCompiler;
            this.typePoolResolver = typePoolResolver;
            this.strict = strict;
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
            private final ElementMatcher<? super ByteCodeElement> matcher;

            /**
             * Creates a new member substitution for a matched byte code element that requires a specification for how to perform a substitution.
             *
             * @param methodGraphCompiler The method graph compiler to use.
             * @param typePoolResolver    The type pool resolver to use.
             * @param strict              {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
             * @param replacementFactory  The replacement factory to use.
             * @param matcher             A matcher for any byte code elements that should be substituted.
             */
            protected ForMatchedByteCodeElement(MethodGraph.Compiler methodGraphCompiler,
                                                TypePoolResolver typePoolResolver,
                                                boolean strict,
                                                Replacement.Factory replacementFactory,
                                                ElementMatcher<? super ByteCodeElement> matcher) {
                super(methodGraphCompiler, typePoolResolver, strict, replacementFactory);
                this.matcher = matcher;
            }

            /**
             * {@inheritDoc}
             */
            public MemberSubstitution replaceWith(Substitution.Factory substitutionFactory) {
                return new MemberSubstitution(methodGraphCompiler,
                        typePoolResolver,
                        strict,
                        new Replacement.Factory.Compound(
                                this.replacementFactory,
                                Replacement.ForElementMatchers.Factory.of(matcher, substitutionFactory)));
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
            private final ElementMatcher<? super FieldDescription.InDefinedShape> matcher;

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
             * @param replacementFactory  The replacement factory to use.
             * @param matcher             A matcher for any field that should be substituted.
             */
            protected ForMatchedField(MethodGraph.Compiler methodGraphCompiler,
                                      TypePoolResolver typePoolResolver,
                                      boolean strict,
                                      Replacement.Factory replacementFactory,
                                      ElementMatcher<? super FieldDescription.InDefinedShape> matcher) {
                this(methodGraphCompiler, typePoolResolver, strict, replacementFactory, matcher, true, true);
            }

            /**
             * Creates a new member substitution for a matched field that requires a specification for how to perform a substitution.
             *
             * @param methodGraphCompiler The method graph compiler to use.
             * @param typePoolResolver    The type pool resolver to use.
             * @param strict              {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
             * @param replacementFactory  The replacement factory to use.
             * @param matcher             A matcher for any field that should be substituted.
             * @param matchRead           {@code true} if read access to a field should be substituted.
             * @param matchWrite          {@code true} if write access to a field should be substituted.
             */
            protected ForMatchedField(MethodGraph.Compiler methodGraphCompiler,
                                      TypePoolResolver typePoolResolver,
                                      boolean strict,
                                      Replacement.Factory replacementFactory,
                                      ElementMatcher<? super FieldDescription.InDefinedShape> matcher,
                                      boolean matchRead,
                                      boolean matchWrite) {
                super(methodGraphCompiler, typePoolResolver, strict, replacementFactory);
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
                return new ForMatchedField(methodGraphCompiler, typePoolResolver, strict, replacementFactory, matcher, true, false);
            }

            /**
             * When invoked, only write access of the previously matched field is substituted.
             *
             * @return This instance with the limitation that only write access to the matched field is substituted.
             */
            public WithoutSpecification onWrite() {
                return new ForMatchedField(methodGraphCompiler, typePoolResolver, strict, replacementFactory, matcher, false, true);
            }

            /**
             * {@inheritDoc}
             */
            public MemberSubstitution replaceWith(Substitution.Factory substitutionFactory) {
                return new MemberSubstitution(methodGraphCompiler,
                        typePoolResolver,
                        strict,
                        new Replacement.Factory.Compound(
                                this.replacementFactory,
                                Replacement.ForElementMatchers.Factory.ofField(matcher, matchRead, matchWrite, substitutionFactory)));
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
             * @param replacementFactory  The replacement factory to use.
             * @param matcher             A matcher for any method or constructor that should be substituted.
             */
            protected ForMatchedMethod(MethodGraph.Compiler methodGraphCompiler,
                                       TypePoolResolver typePoolResolver,
                                       boolean strict,
                                       Replacement.Factory replacementFactory,
                                       ElementMatcher<? super MethodDescription> matcher) {
                this(methodGraphCompiler, typePoolResolver, strict, replacementFactory, matcher, true, true);
            }

            /**
             * Creates a new member substitution for a matched method that requires a specification for how to perform a substitution.
             *
             * @param methodGraphCompiler The method graph compiler to use.
             * @param typePoolResolver    The type pool resolver to use.
             * @param strict              {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
             * @param replacementFactory  The replacement factory to use.
             * @param matcher             A matcher for any method or constructor that should be substituted.
             * @param includeVirtualCalls {@code true} if this specification includes virtual invocations.
             * @param includeSuperCalls   {@code true} if this specification includes {@code super} invocations.
             */
            protected ForMatchedMethod(MethodGraph.Compiler methodGraphCompiler,
                                       TypePoolResolver typePoolResolver,
                                       boolean strict,
                                       Replacement.Factory replacementFactory,
                                       ElementMatcher<? super MethodDescription> matcher,
                                       boolean includeVirtualCalls,
                                       boolean includeSuperCalls) {
                super(methodGraphCompiler, typePoolResolver, strict, replacementFactory);
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
                return new ForMatchedMethod(methodGraphCompiler, typePoolResolver, strict, replacementFactory, isVirtual().and(matcher), true, false);
            }

            /**
             * Limits the substituted method calls to method calls that invoke a method as a {@code super} call.
             *
             * @return This specification where only virtual methods are matched if they are not invoked as a super call.
             */
            public WithoutSpecification onSuperCall() {
                return new ForMatchedMethod(methodGraphCompiler, typePoolResolver, strict, replacementFactory, isVirtual().and(matcher), false, true);
            }

            /**
             * {@inheritDoc}
             */
            public MemberSubstitution replaceWith(Substitution.Factory substitutionFactory) {
                return new MemberSubstitution(methodGraphCompiler,
                        typePoolResolver,
                        strict,
                        new Replacement.Factory.Compound(
                                this.replacementFactory,
                                Replacement.ForElementMatchers.Factory.ofMethod(matcher, includeVirtualCalls, includeSuperCalls, substitutionFactory)));
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
            public static TypePoolResolver of(ClassLoader classLoader) {
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
         * @param targetType The target type on which a member is accessed.
         * @param target     The target field, method or constructor that is substituted,
         * @param parameters All parameters that serve as input to this access.
         * @param result     The result that is expected from the interaction or {@code void} if no result is expected.
         * @param freeOffset The first free offset of the local variable array that can be used for storing values.
         * @return A stack manipulation that represents the access.
         */
        StackManipulation resolve(TypeDescription targetType,
                                  ByteCodeElement target,
                                  TypeList.Generic parameters,
                                  TypeDescription.Generic result,
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
         * A substitution that drops any field or method access and returns the expected return type's default value, i.e {@code null} or zero for primitive types.
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
            public StackManipulation resolve(TypeDescription targetType,
                                             ByteCodeElement target,
                                             TypeList.Generic parameters,
                                             TypeDescription.Generic result,
                                             int freeOffset) {
                List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(parameters.size());
                for (int index = parameters.size() - 1; index >= 0; index--) {
                    stackManipulations.add(Removal.of(parameters.get(index)));
                }
                return new StackManipulation.Compound(CompoundList.of(stackManipulations, DefaultValue.of(result.asErasure())));
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
            public StackManipulation resolve(TypeDescription targetType,
                                             ByteCodeElement target,
                                             TypeList.Generic parameters,
                                             TypeDescription.Generic result,
                                             int freeOffset) {
                FieldDescription fieldDescription = fieldResolver.resolve(targetType, target, parameters, result);
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
                 * @param targetType The target type on which a member is accessed.
                 * @param target     The target field, method or constructor that is substituted,
                 * @param parameters All parameters that serve as input to this access.
                 * @param result     The result that is expected from the interaction or {@code void} if no result is expected.
                 * @return The field to substitute with.
                 */
                FieldDescription resolve(TypeDescription targetType, ByteCodeElement target, TypeList.Generic parameters, TypeDescription.Generic result);

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
                    public FieldDescription resolve(TypeDescription targetType, ByteCodeElement target, TypeList.Generic parameters, TypeDescription.Generic result) {
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
                    public FieldDescription resolve(TypeDescription targetType, ByteCodeElement target, TypeList.Generic parameters, TypeDescription.Generic result) {
                        if (parameters.isEmpty()) {
                            throw new IllegalStateException("Cannot substitute parameterless instruction with " + target);
                        } else if (parameters.get(0).isPrimitive() || parameters.get(0).isArray()) {
                            throw new IllegalStateException("Cannot access field on primitive or array type for " + target);
                        }
                        TypeDefinition current = parameters.get(0);
                        do {
                            FieldList<?> fields = current.getDeclaredFields().filter(not(isStatic()).<FieldDescription>and(isVisibleTo(instrumentedType)).and(matcher));
                            if (fields.size() == 1) {
                                return fields.getOnly();
                            } else if (fields.size() > 1) {
                                throw new IllegalStateException("Ambiguous field location of " + fields);
                            }
                            current = current.getSuperClass();
                        } while (current != null);
                        throw new IllegalStateException("Cannot locate field matching " + matcher + " on " + targetType);
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
             * The index of the this reference within a non-static method.
             */
            private static final int THIS_REFERENCE = 0;

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
            public StackManipulation resolve(TypeDescription targetType,
                                             ByteCodeElement target,
                                             TypeList.Generic parameters,
                                             TypeDescription.Generic result,
                                             int freeOffset) {
                MethodDescription methodDescription = methodResolver.resolve(targetType, target, parameters, result);
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
                return methodDescription.isVirtual()
                        ? MethodInvocation.invoke(methodDescription).virtual(mapped.get(THIS_REFERENCE).asErasure())
                        : MethodInvocation.invoke(methodDescription);
            }

            /**
             * A method resolver for locating a method for a substitute.
             */
            public interface MethodResolver {

                /**
                 * Resolves the method to substitute with.
                 *
                 * @param targetType The target type on which a member is accessed.
                 * @param target     The target field, method or constructor that is substituted,
                 * @param parameters All parameters that serve as input to this access.
                 * @param result     The result that is expected from the interaction or {@code void} if no result is expected.
                 * @return The field to substitute with.
                 */
                MethodDescription resolve(TypeDescription targetType, ByteCodeElement target, TypeList.Generic parameters, TypeDescription.Generic result);

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
                    public MethodDescription resolve(TypeDescription targetType, ByteCodeElement target, TypeList.Generic parameters, TypeDescription.Generic result) {
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
                    public MethodDescription resolve(TypeDescription targetType, ByteCodeElement target, TypeList.Generic parameters, TypeDescription.Generic result) {
                        if (parameters.isEmpty()) {
                            throw new IllegalStateException("Cannot substitute parameterless instruction with " + target);
                        } else if (parameters.get(0).isPrimitive() || parameters.get(0).isArray()) {
                            throw new IllegalStateException("Cannot invoke method on primitive or array type for " + target);
                        }
                        TypeDefinition typeDefinition = parameters.get(0);
                        List<MethodDescription> candidates = CompoundList.<MethodDescription>of(methodGraphCompiler.compile(typeDefinition, instrumentedType)
                                .listNodes()
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
            public StackManipulation resolve(TypeDescription targetType,
                                             ByteCodeElement target,
                                             TypeList.Generic parameters,
                                             TypeDescription.Generic result,
                                             int freeOffset) {
                List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(1
                        + parameters.size()
                        + steps.size() * 2
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
                    Step.Resolution resulution = step.resolve(targetType,
                            target,
                            parameters,
                            current,
                            offsets,
                            freeOffset);
                    stackManipulations.add(resulution.getStackManipulation());
                    current = resulution.getResultType();
                }
                stackManipulations.add(assigner.assign(current, result, typing));
                return new StackManipulation.Compound(stackManipulations);
            }

            /**
             * Represents a step of a substitution chain.
             */
            protected interface Step {

                /**
                 * Resolves this step of a substitution chain.
                 *
                 * @param targetType The target result type of the substitution.
                 * @param target     The byte code element that is currently substituted.
                 * @param parameters The parameters of the substituted element.
                 * @param current    The current type of the applied substitution that is the top element on the operand stack.
                 * @param offsets    The arguments of the substituted byte code element mapped to their local variable offsets.
                 * @param freeOffset The first free offset in the local variable array.
                 * @return A resolved substitution step for the supplied inputs.
                 */
                Resolution resolve(TypeDescription targetType,
                                   ByteCodeElement target,
                                   TypeList.Generic parameters,
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
                    Step make(Assigner assigner,
                              Assigner.Typing typing,
                              TypeDescription instrumentedType,
                              MethodDescription instrumentedMethod);
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
                    public Simple(StackManipulation stackManipulation, TypeDescription.Generic resultType) {
                        this.stackManipulation = stackManipulation;
                        this.resultType = resultType;
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
                    public Resolution resolve(TypeDescription targetType,
                                              ByteCodeElement target,
                                              TypeList.Generic parameters,
                                              TypeDescription.Generic current,
                                              Map<Integer, Integer> offsets,
                                              int freeOffset) {
                        return targetType.represents(void.class)
                                ? this
                                : new Simple(new StackManipulation.Compound(Removal.of(targetType), stackManipulation), resultType);
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
            }

            /**
             * A factory for creating a substitution chain.
             */
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
         * @param fieldDescription   The field that was discovered.
         * @param writeAccess        {@code true} if this field was written to.
         * @return A binding for the discovered field access.
         */
        Binding bind(TypeDescription instrumentedType,
                     MethodDescription instrumentedMethod,
                     FieldDescription.InDefinedShape fieldDescription,
                     boolean writeAccess);

        /**
         * Binds this replacement for a field that was discovered.
         *
         * @param instrumentedType   The instrumented type.
         * @param instrumentedMethod The instrumented method.
         * @param typeDescription    The type on which the method was invoked.
         * @param methodDescription  The method that was discovered.
         * @param invocationType     The invocation type for this method.
         * @return A binding for the discovered method invocation.
         */
        Binding bind(TypeDescription instrumentedType,
                     MethodDescription instrumentedMethod,
                     TypeDescription typeDescription,
                     MethodDescription methodDescription,
                     InvocationType invocationType);

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
             * @param parameters The parameters that are accessible to the substitution target.
             * @param result     The result that is expected from the substitution target or {@code void} if none is expected.
             * @param freeOffset The first offset that can be used for storing local variables.
             * @return A stack manipulation that represents the replacement.
             */
            StackManipulation make(TypeList.Generic parameters, TypeDescription.Generic result, int freeOffset);

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
                public StackManipulation make(TypeList.Generic parameters, TypeDescription.Generic result, int freeOffset) {
                    throw new IllegalStateException("Cannot resolve unresolved binding");
                }
            }

            /**
             * A binding that was resolved for an actual substitution.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Resolved implements Binding {

                /**
                 * The instrumented type.
                 */
                private final TypeDescription instrumentedType;

                /**
                 * The instrumented method.
                 */
                private final MethodDescription instrumentedMethod;

                /**
                 * The type on which a field or method was accessed.
                 */
                private final TypeDescription targetType;

                /**
                 * The field or method that was accessed.
                 */
                private final ByteCodeElement target;

                /**
                 * The substitution to apply.
                 */
                private final Substitution substitution;

                /**
                 * Creates a new resolved binding.
                 *
                 * @param instrumentedType   The instrumented type.
                 * @param instrumentedMethod The instrumented method.
                 * @param targetType         The type on which a field or method was accessed.
                 * @param target             The field or method that was accessed.
                 * @param substitution       The substitution to apply.
                 */
                protected Resolved(TypeDescription instrumentedType,
                                   MethodDescription instrumentedMethod,
                                   TypeDescription targetType,
                                   ByteCodeElement target,
                                   Substitution substitution) {
                    this.instrumentedType = instrumentedType;
                    this.instrumentedMethod = instrumentedMethod;
                    this.targetType = targetType;
                    this.target = target;
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
                public StackManipulation make(TypeList.Generic parameters, TypeDescription.Generic result, int freeOffset) {
                    return substitution.resolve(targetType, target, parameters, result, freeOffset);
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
             * Desribes a virtual method invocation.
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
             * @return The invokation type for the method given that opcode.
             */
            protected static InvocationType of(int opcode, MethodDescription methodDescription) {
                switch (opcode) {
                    case Opcodes.INVOKEVIRTUAL:
                    case Opcodes.INVOKEINTERFACE:
                        return InvocationType.VIRTUAL;
                    case Opcodes.INVOKESPECIAL:
                        return methodDescription.isVirtual()
                                ? SUPER
                                : OTHER;
                    default:
                        return OTHER;
                }
            }

            /**
             * Checks if this invokation type matches the specified inputs.
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
             * The singelton instance.
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
                                FieldDescription.InDefinedShape fieldDescription,
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
            private final ElementMatcher<? super FieldDescription.InDefinedShape> fieldMatcher;

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
            protected ForElementMatchers(ElementMatcher<? super FieldDescription.InDefinedShape> fieldMatcher,
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
            public Binding bind(TypeDescription instrumentedType,
                                MethodDescription instrumentedMethod,
                                FieldDescription.InDefinedShape fieldDescription,
                                boolean writeAccess) {
                return (writeAccess ? matchFieldWrite : matchFieldRead) && fieldMatcher.matches(fieldDescription)
                        ? new Binding.Resolved(instrumentedType, instrumentedMethod, fieldDescription.getDeclaringType(), fieldDescription, substitution)
                        : Binding.Unresolved.INSTANCE;
            }

            /**
             * {@inheritDoc}
             */
            public Binding bind(TypeDescription instrumentedType,
                                MethodDescription instrumentedMethod,
                                TypeDescription typeDescription,
                                MethodDescription methodDescription,
                                InvocationType invocationType) {
                return invocationType.matches(includeVirtualCalls, includeSuperCalls) && methodMatcher.matches(methodDescription)
                        ? new Binding.Resolved(instrumentedType, instrumentedMethod, typeDescription, methodDescription, substitution)
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
                private final ElementMatcher<? super FieldDescription.InDefinedShape> fieldMatcher;

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
                protected Factory(ElementMatcher<? super FieldDescription.InDefinedShape> fieldMatcher,
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
                protected static Replacement.Factory of(ElementMatcher<? super ByteCodeElement> matcher, Substitution.Factory factory) {
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
                protected static Replacement.Factory ofField(ElementMatcher<? super FieldDescription.InDefinedShape> matcher,
                                                             boolean matchFieldRead,
                                                             boolean matchFieldWrite,
                                                             Substitution.Factory factory) {
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
                protected static Replacement.Factory ofMethod(ElementMatcher<? super MethodDescription> matcher,
                                                              boolean includeVirtualCalls,
                                                              boolean includeSuperCalls,
                                                              Substitution.Factory factory) {
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
            public Binding bind(TypeDescription instrumentedType,
                                MethodDescription instrumentedMethod,
                                FieldDescription.InDefinedShape fieldDescription,
                                boolean writeAccess) {
                for (Replacement replacement : replacements) {
                    Binding binding = replacement.bind(instrumentedType, instrumentedMethod, fieldDescription, writeAccess);
                    if (binding.isBound()) {
                        return binding;
                    }
                }
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
         * Creates a new substituting method visitor.
         *
         * @param methodVisitor         The method visitor to delegate to.
         * @param instrumentedType      The instrumented type.
         * @param instrumentedMethod    The instrumented method.
         * @param methodGraphCompiler   The method graph compiler to use.
         * @param strict                {@code true} if the method processing should be strict where an exception is raised if a member cannot be found.
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
                                            Replacement replacement,
                                            Implementation.Context implementationContext,
                                            TypePool typePool,
                                            boolean virtualPrivateCalls) {
            super(methodVisitor, instrumentedMethod);
            this.instrumentedType = instrumentedType;
            this.instrumentedMethod = instrumentedMethod;
            this.methodGraphCompiler = methodGraphCompiler;
            this.strict = strict;
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
                FieldList<FieldDescription.InDefinedShape> candidates = resolution.resolve().getDeclaredFields().filter(strict
                        ? ElementMatchers.<FieldDescription>named(internalName).and(hasDescriptor(descriptor))
                        : ElementMatchers.<FieldDescription>failSafe(named(internalName).and(hasDescriptor(descriptor))));
                if (!candidates.isEmpty()) {
                    Replacement.Binding binding = replacement.bind(instrumentedType,
                            instrumentedMethod,
                            candidates.getOnly(),
                            opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC);
                    if (binding.isBound()) {
                        TypeList.Generic parameters;
                        TypeDescription.Generic result;
                        switch (opcode) {
                            case Opcodes.PUTFIELD:
                                parameters = new TypeList.Generic.Explicit(candidates.getOnly().getDeclaringType(), candidates.getOnly().getType());
                                result = TypeDescription.Generic.VOID;
                                break;
                            case Opcodes.PUTSTATIC:
                                parameters = new TypeList.Generic.Explicit(candidates.getOnly().getType());
                                result = TypeDescription.Generic.VOID;
                                break;
                            case Opcodes.GETFIELD:
                                parameters = new TypeList.Generic.Explicit(candidates.getOnly().getDeclaringType());
                                result = candidates.getOnly().getType();
                                break;
                            case Opcodes.GETSTATIC:
                                parameters = new TypeList.Generic.Empty();
                                result = candidates.getOnly().getType();
                                break;
                            default:
                                throw new IllegalStateException("Unexpected opcode: " + opcode);
                        }
                        stackSizeBuffer = Math.max(stackSizeBuffer, binding.make(parameters, result, getFreeOffset())
                                .apply(new LocalVariableTracingMethodVisitor(mv), implementationContext)
                                .getMaximalSize() - result.getStackSize().getSize());
                        return;
                    }
                } else if (strict) {
                    throw new IllegalStateException("Could not resolve " + owner.replace('/', '.')
                            + "." + internalName + descriptor + " using " + typePool);
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
                } else if (opcode == Opcodes.INVOKESTATIC || opcode == Opcodes.INVOKESPECIAL) {
                    candidates = resolution.resolve().getDeclaredMethods().filter(strict
                            ? ElementMatchers.<MethodDescription>named(internalName).and(hasDescriptor(descriptor))
                            : ElementMatchers.<MethodDescription>failSafe(named(internalName).and(hasDescriptor(descriptor))));
                } else if (virtualPrivateCalls) {
                    candidates = resolution.resolve().getDeclaredMethods().filter(strict
                            ? ElementMatchers.<MethodDescription>isPrivate().and(not(isStatic())).and(named(internalName).and(hasDescriptor(descriptor)))
                            : ElementMatchers.<MethodDescription>failSafe(isPrivate().<MethodDescription>and(not(isStatic())).and(named(internalName).and(hasDescriptor(descriptor)))));
                    if (candidates.isEmpty()) {
                        candidates = methodGraphCompiler.compile(resolution.resolve(), instrumentedType).listNodes().asMethodList().filter(strict
                                ? ElementMatchers.<MethodDescription>named(internalName).and(hasDescriptor(descriptor))
                                : ElementMatchers.<MethodDescription>failSafe(named(internalName).and(hasDescriptor(descriptor))));
                    }
                } else {
                    candidates = methodGraphCompiler.compile(resolution.resolve(), instrumentedType).listNodes().asMethodList().filter(strict
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
                        stackSizeBuffer = Math.max(stackSizeBuffer, binding.make(
                                candidates.getOnly().isStatic() || candidates.getOnly().isConstructor()
                                        ? candidates.getOnly().getParameters().asTypeList()
                                        : new TypeList.Generic.Explicit(CompoundList.of(resolution.resolve(), candidates.getOnly().getParameters().asTypeList())),
                                candidates.getOnly().isConstructor()
                                        ? candidates.getOnly().getDeclaringType().asGenericType()
                                        : candidates.getOnly().getReturnType(),
                                getFreeOffset())
                                .apply(new LocalVariableTracingMethodVisitor(mv), implementationContext).getMaximalSize() - (candidates.getOnly().isConstructor()
                                ? StackSize.SINGLE
                                : candidates.getOnly().getReturnType().getStackSize()).getSize());
                        if (candidates.getOnly().isConstructor()) {
                            stackSizeBuffer = Math.max(stackSizeBuffer, new StackManipulation.Compound(
                                    Duplication.SINGLE.flipOver(TypeDescription.OBJECT),
                                    Removal.SINGLE,
                                    Removal.SINGLE,
                                    Duplication.SINGLE.flipOver(TypeDescription.OBJECT),
                                    Removal.SINGLE,
                                    Removal.SINGLE
                            ).apply(mv, implementationContext).getMaximalSize() + StackSize.SINGLE.getSize());
                        }
                        return;
                    }
                } else if (strict) {
                    throw new IllegalStateException("Could not resolve " + owner.replace('/', '.')
                            + "." + internalName + descriptor + " using " + typePool);
                }
            } else if (strict) {
                throw new IllegalStateException("Could not resolve " + owner.replace('/', '.') + " using " + typePool);
            }
            super.visitMethodInsn(opcode, owner, internalName, descriptor, isInterface);
        }

        @Override
        public void visitMaxs(int stackSize, int localVariableLength) {
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
}
