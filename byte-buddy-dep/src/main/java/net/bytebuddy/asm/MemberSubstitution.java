package net.bytebuddy.asm;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.Removal;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.constant.DefaultValue;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.CompoundList;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

@EqualsAndHashCode(callSuper = false)
public class MemberSubstitution extends AsmVisitorWrapper.AbstractBase {

    private final MethodGraph.Compiler methodGraphCompiler;

    private final boolean strict;

    private final Substitution substitution;

    protected MemberSubstitution(MethodGraph.Compiler methodGraphCompiler, boolean strict, Substitution substitution) {
        this.methodGraphCompiler = methodGraphCompiler;
        this.strict = strict;
        this.substitution = substitution;
    }

    public static MemberSubstitution strict() {
        return new MemberSubstitution(MethodGraph.Compiler.DEFAULT, true, Substitution.NoOp.INSTANCE);
    }

    public static MemberSubstitution relaxed() {
        return new MemberSubstitution(MethodGraph.Compiler.DEFAULT, false, Substitution.NoOp.INSTANCE);
    }

    public WithoutSpecification element(ElementMatcher<? super ByteCodeElement> matcher) {
        return new WithoutSpecification.ForMatchedElement(methodGraphCompiler, strict, substitution, matcher);
    }

    public WithoutSpecification field(ElementMatcher<? super FieldDescription.InDefinedShape> matcher) {
        return new WithoutSpecification.ForMatchedField(methodGraphCompiler, strict, substitution, matcher);
    }

    public WithoutSpecification method(ElementMatcher<? super MethodDescription> matcher) {
        return new WithoutSpecification.ForMatchedMethod(methodGraphCompiler, strict, substitution, matcher);
    }

    public AsmVisitorWrapper with(MethodGraph.Compiler methodGraphCompiler) {
        return new MemberSubstitution(methodGraphCompiler, strict, substitution);
    }

    @Override
    public ClassVisitor wrap(TypeDescription instrumentedType,
                             ClassVisitor classVisitor,
                             Implementation.Context implementationContext,
                             TypePool typePool,
                             FieldList<FieldDescription.InDefinedShape> fields,
                             MethodList<?> methods,
                             int writerFlags,
                             int readerFlags) {
        return new SubstitutingClassVisitor(classVisitor,
                methodGraphCompiler,
                strict,
                substitution,
                instrumentedType,
                implementationContext,
                typePool);
    }

    public abstract static class WithoutSpecification {

        protected final MethodGraph.Compiler methodGraphCompiler;

        protected final boolean strict;

        protected final Substitution substitution;

        protected WithoutSpecification(MethodGraph.Compiler methodGraphCompiler,
                                       boolean strict,
                                       Substitution substitution) {
            this.methodGraphCompiler = methodGraphCompiler;
            this.strict = strict;
            this.substitution = substitution;
        }

        public abstract MemberSubstitution stub();

        public MemberSubstitution replaceWith(Field field) {
            return replaceWith(new FieldDescription.ForLoadedField(field));
        }

        public abstract MemberSubstitution replaceWith(FieldDescription fieldDescription);

        public MemberSubstitution replaceWith(Method method) {
            return replaceWith(new MethodDescription.ForLoadedMethod(method));
        }

        public abstract MemberSubstitution replaceWith(MethodDescription methodDescription);

        protected static class ForMatchedElement extends WithoutSpecification {

            private final ElementMatcher<? super ByteCodeElement> matcher;

            protected ForMatchedElement(MethodGraph.Compiler methodGraphCompiler,
                                        boolean strict,
                                        Substitution substitution,
                                        ElementMatcher<? super ByteCodeElement> matcher) {
                super(methodGraphCompiler, strict, substitution);
                this.matcher = matcher;
            }

            @Override
            public MemberSubstitution stub() {
                return new MemberSubstitution(methodGraphCompiler,
                        strict,
                        new Substitution.Compound(new Substitution.Matching(matcher, matcher, Substitution.Resolver.Stubbing.INSTANCE), substitution));
            }

            @Override
            public MemberSubstitution replaceWith(FieldDescription fieldDescription) {
                return new MemberSubstitution(methodGraphCompiler,
                        strict,
                        new Substitution.Compound(new Substitution.Matching(matcher, matcher, new Substitution.Resolver.FieldAccessing(fieldDescription)), substitution));
            }

            @Override
            public MemberSubstitution replaceWith(MethodDescription methodDescription) {
                return new MemberSubstitution(methodGraphCompiler,
                        strict,
                        new Substitution.Compound(new Substitution.Matching(matcher, matcher, new Substitution.Resolver.MethodInvoking(methodDescription)), substitution));
            }
        }

        protected static class ForMatchedField extends WithoutSpecification {

            private final ElementMatcher<? super FieldDescription.InDefinedShape> matcher;

            protected ForMatchedField(MethodGraph.Compiler methodGraphCompiler,
                                      boolean strict,
                                      Substitution substitution,
                                      ElementMatcher<? super FieldDescription.InDefinedShape> matcher) {
                super(methodGraphCompiler, strict, substitution);
                this.matcher = matcher;
            }

            @Override
            public MemberSubstitution stub() {
                return new MemberSubstitution(methodGraphCompiler,
                        strict,
                        new Substitution.Compound(new Substitution.Matching(matcher, none(), Substitution.Resolver.Stubbing.INSTANCE), substitution));
            }

            @Override
            public MemberSubstitution replaceWith(FieldDescription fieldDescription) {
                return new MemberSubstitution(methodGraphCompiler,
                        strict,
                        new Substitution.Compound(new Substitution.Matching(matcher, none(), new Substitution.Resolver.FieldAccessing(fieldDescription)), substitution));
            }

            @Override
            public MemberSubstitution replaceWith(MethodDescription methodDescription) {
                return new MemberSubstitution(methodGraphCompiler,
                        strict,
                        new Substitution.Compound(new Substitution.Matching(matcher, none(), new Substitution.Resolver.MethodInvoking(methodDescription)), substitution));
            }
        }

        protected static class ForMatchedMethod extends WithoutSpecification {

            private final ElementMatcher<? super MethodDescription> matcher;

            protected ForMatchedMethod(MethodGraph.Compiler methodGraphCompiler,
                                       boolean strict,
                                       Substitution substitution,
                                       ElementMatcher<? super MethodDescription> matcher) {
                super(methodGraphCompiler, strict, substitution);
                this.matcher = matcher;
            }

            @Override
            public MemberSubstitution stub() {
                return new MemberSubstitution(methodGraphCompiler,
                        strict,
                        new Substitution.Compound(new Substitution.Matching(none(), matcher, Substitution.Resolver.Stubbing.INSTANCE), substitution));
            }

            @Override
            public MemberSubstitution replaceWith(FieldDescription fieldDescription) {
                return new MemberSubstitution(methodGraphCompiler,
                        strict,
                        new Substitution.Compound(new Substitution.Matching(none(), matcher, new Substitution.Resolver.FieldAccessing(fieldDescription)), substitution));
            }

            @Override
            public MemberSubstitution replaceWith(MethodDescription methodDescription) {
                return new MemberSubstitution(methodGraphCompiler,
                        strict,
                        new Substitution.Compound(new Substitution.Matching(none(), matcher, new Substitution.Resolver.MethodInvoking(methodDescription)), substitution));
            }
        }
    }

    protected interface Substitution {

        Resolver resolve(FieldDescription.InDefinedShape fieldDescription);

        Resolver resolve(MethodDescription methodDescription);

        interface Resolver {

            boolean isResolved();

            StackManipulation apply(TypeDescription instrumentedType, TypeList.Generic arguments, TypeDescription.Generic result);

            enum Unresolved implements Resolver {

                INSTANCE;

                @Override
                public boolean isResolved() {
                    return false;
                }

                @Override
                public StackManipulation apply(TypeDescription instrumentedType, TypeList.Generic arguments, TypeDescription.Generic result) {
                    throw new IllegalStateException();
                }
            }

            enum Stubbing implements Resolver {

                INSTANCE;

                @Override
                public boolean isResolved() {
                    return true;
                }

                @Override
                public StackManipulation apply(TypeDescription instrumentedType, TypeList.Generic arguments, TypeDescription.Generic result) {
                    List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(arguments.size());
                    for (TypeDescription argument : arguments.asErasures()) {
                        stackManipulations.add(Removal.of(argument));
                    }
                    return new StackManipulation.Compound(CompoundList.of(stackManipulations, DefaultValue.of(result.asErasure())));
                }
            }

            class FieldAccessing implements Resolver {

                private final FieldDescription fieldDescription;

                protected FieldAccessing(FieldDescription fieldDescription) {
                    this.fieldDescription = fieldDescription;
                }

                @Override
                public boolean isResolved() {
                    return true;
                }

                @Override
                public StackManipulation apply(TypeDescription instrumentedType, TypeList.Generic arguments, TypeDescription.Generic result) {
                    if (!fieldDescription.isAccessibleTo(instrumentedType)) {
                        throw new IllegalStateException();
                    } else if (result.represents(void.class)) {
                        if (arguments.size() != (fieldDescription.isStatic() ? 1 : 2)) {
                            throw new IllegalStateException();
                        } else if (!fieldDescription.isStatic() && arguments.get(0).asErasure().isAssignableTo(fieldDescription.getDeclaringType().asErasure())) {
                            throw new IllegalStateException();
                        } else if (arguments.get(fieldDescription.isStatic() ? 0 : 1).asErasure().isAssignableTo(fieldDescription.getType().asErasure())) {
                            throw new IllegalStateException();
                        }
                        return FieldAccess.forField(fieldDescription).write();
                    } else {
                        if (arguments.size() != (fieldDescription.isStatic() ? 0 : 1)) {
                            throw new IllegalStateException();
                        } else if (!fieldDescription.isStatic() && arguments.get(0).asErasure().isAssignableTo(fieldDescription.getDeclaringType().asErasure())) {
                            throw new IllegalStateException();
                        } else if (!fieldDescription.getType().asErasure().isAssignableTo(result.asErasure())) {
                            throw new IllegalStateException();
                        }
                        return FieldAccess.forField(fieldDescription).read();
                    }
                }
            }

            class MethodInvoking implements Resolver {

                private final MethodDescription methodDescription;

                protected MethodInvoking(MethodDescription methodDescription) {
                    this.methodDescription = methodDescription;
                }

                @Override
                public boolean isResolved() {
                    return true;
                }

                @Override
                public StackManipulation apply(TypeDescription instrumentedType, TypeList.Generic arguments, TypeDescription.Generic result) {
                    if (!methodDescription.isAccessibleTo(instrumentedType)) {
                        throw new IllegalStateException();
                    }
                    TypeList.Generic mapped = methodDescription.isStatic()
                            ? methodDescription.getParameters().asTypeList()
                            : new TypeList.Generic.Explicit(CompoundList.of(methodDescription.getDeclaringType(), methodDescription.getParameters().asTypeList()));
                    if (methodDescription.getReturnType().asErasure().isAssignableTo(result.asErasure())) {
                        throw new IllegalStateException();
                    } else if (mapped.size() != arguments.size()) {
                        throw new IllegalStateException();
                    }
                    for (int index = 0; index < mapped.size(); index++) {
                        if (!mapped.get(index).asErasure().isAssignableTo(arguments.get(index).asErasure())) {
                            throw new IllegalStateException();
                        }
                    }
                    return MethodInvocation.invoke(methodDescription);
                }
            }
        }

        enum NoOp implements Substitution {

            INSTANCE;

            @Override
            public Resolver resolve(FieldDescription.InDefinedShape fieldDescription) {
                return Resolver.Unresolved.INSTANCE;
            }

            @Override
            public Resolver resolve(MethodDescription methodDescription) {
                return Resolver.Unresolved.INSTANCE;
            }
        }

        class Matching implements Substitution {

            private final ElementMatcher<? super FieldDescription.InDefinedShape> fieldMatcher;

            private final ElementMatcher<? super MethodDescription> methodMatcher;

            private final Resolver resolver;

            protected Matching(ElementMatcher<? super FieldDescription.InDefinedShape> fieldMatcher,
                               ElementMatcher<? super MethodDescription> methodMatcher,
                               Resolver resolver) {
                this.fieldMatcher = fieldMatcher;
                this.methodMatcher = methodMatcher;
                this.resolver = resolver;
            }

            @Override
            public Resolver resolve(FieldDescription.InDefinedShape fieldDescription) {
                return fieldMatcher.matches(fieldDescription)
                        ? resolver
                        : Resolver.Unresolved.INSTANCE;
            }

            @Override
            public Resolver resolve(MethodDescription methodDescription) {
                return methodMatcher.matches(methodDescription)
                        ? resolver
                        : Resolver.Unresolved.INSTANCE;
            }
        }

        class Compound implements Substitution {

            private final List<Substitution> substitutions;

            protected Compound(Substitution... substitution) {
                this(Arrays.asList(substitution));
            }

            protected Compound(List<? extends Substitution> substitutions) {
                this.substitutions = new ArrayList<Substitution>(substitutions.size());
                for (Substitution substitution : substitutions) {
                    if (substitution instanceof Compound) {
                        this.substitutions.addAll(((Compound) substitution).substitutions);
                    } else if (!(substitution instanceof NoOp)) {
                        this.substitutions.add(substitution);
                    }
                }
            }

            @Override
            public Resolver resolve(FieldDescription.InDefinedShape fieldDescription) {
                for (Substitution substitution : substitutions) {
                    Resolver resolver = substitution.resolve(fieldDescription);
                    if (resolver.isResolved()) {
                        return resolver;
                    }
                }
                return Resolver.Unresolved.INSTANCE;
            }

            @Override
            public Resolver resolve(MethodDescription methodDescription) {
                for (Substitution substitution : substitutions) {
                    Resolver resolver = substitution.resolve(methodDescription);
                    if (resolver.isResolved()) {
                        return resolver;
                    }
                }
                return Resolver.Unresolved.INSTANCE;
            }
        }
    }

    protected static class SubstitutingClassVisitor extends ClassVisitor {

        private final MethodGraph.Compiler methodGraphCompiler;

        private final boolean strict;

        private final Substitution substitution;

        private final TypeDescription instrumentedType;

        private final Implementation.Context implementationContext;

        private final TypePool typePool;

        protected SubstitutingClassVisitor(ClassVisitor classVisitor,
                                           MethodGraph.Compiler methodGraphCompiler,
                                           boolean strict,
                                           Substitution substitution,
                                           TypeDescription instrumentedType,
                                           Implementation.Context implementationContext,
                                           TypePool typePool) {
            super(Opcodes.ASM5, classVisitor);
            this.methodGraphCompiler = methodGraphCompiler;
            this.strict = strict;
            this.instrumentedType = instrumentedType;
            this.substitution = substitution;
            this.implementationContext = implementationContext;
            this.typePool = typePool;
        }

        @Override
        public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exception) {
            return new SubstitutingMethodVisitor(super.visitMethod(modifiers, internalName, descriptor, signature, exception),
                    methodGraphCompiler,
                    strict,
                    substitution,
                    instrumentedType, implementationContext,
                    typePool);
        }
    }

    protected static class SubstitutingMethodVisitor extends MethodVisitor {

        private final MethodGraph.Compiler methodGraphCompiler;

        private final boolean strict;

        private final Substitution substitution;

        private final TypeDescription instrumentedType;

        private final Implementation.Context implementationContext;

        private final TypePool typePool;

        private int stackSizeBuffer;

        protected SubstitutingMethodVisitor(MethodVisitor methodVisitor,
                                            MethodGraph.Compiler methodGraphCompiler,
                                            boolean strict,
                                            Substitution substitution,
                                            TypeDescription instrumentedType,
                                            Implementation.Context implementationContext,
                                            TypePool typePool) {
            super(Opcodes.ASM5, methodVisitor);
            this.methodGraphCompiler = methodGraphCompiler;
            this.strict = strict;
            this.substitution = substitution;
            this.instrumentedType = instrumentedType;
            this.implementationContext = implementationContext;
            this.typePool = typePool;
            stackSizeBuffer = 0;
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String internalName, String descriptor) {
            TypePool.Resolution resolution = typePool.describe(owner.replace('/', '.'));
            if (resolution.isResolved()) {
                FieldList<FieldDescription.InDefinedShape> candidates = resolution.resolve()
                        .getDeclaredFields()
                        .filter(named(internalName).and(hasDescriptor(descriptor)));
                if (!candidates.isEmpty()) {
                    Substitution.Resolver resolver = substitution.resolve(candidates.getOnly());
                    if (resolver.isResolved()) {
                        TypeList.Generic arguments;
                        TypeDescription.Generic result;
                        switch (opcode) {
                            case Opcodes.PUTFIELD:
                                arguments = new TypeList.Generic.Explicit(candidates.getOnly().getDeclaringType(), candidates.getOnly().getType());
                                result = TypeDescription.Generic.VOID;
                                break;
                            case Opcodes.PUTSTATIC:
                                arguments = new TypeList.Generic.Explicit(candidates.getOnly().getType());
                                result = TypeDescription.Generic.VOID;
                                break;
                            case Opcodes.GETFIELD:
                                arguments = new TypeList.Generic.Explicit(candidates.getOnly().getDeclaringType());
                                result = candidates.getOnly().getType();
                                break;
                            case Opcodes.GETSTATIC:
                                arguments = new TypeList.Generic.Empty();
                                result = candidates.getOnly().getType();
                                break;
                            default:
                                throw new AssertionError();
                        }
                        resolver.apply(instrumentedType, arguments, result).apply(mv, implementationContext);
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
                if ((opcode & Opcodes.INVOKESTATIC) != 0 || internalName.equals(resolution.resolve().getInternalName())) {
                    candidates = resolution.resolve().getDeclaredMethods();
                } else if ((opcode & Opcodes.INVOKESPECIAL) != 0 && internalName.equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME)) {
                    candidates = resolution.resolve().getSuperClass().getDeclaredMethods();
                } else {
                    candidates = methodGraphCompiler.compile(resolution.resolve()).listNodes().asMethodList();
                }
                candidates = candidates.filter(named(internalName).and(hasDescriptor(descriptor)));
                if (!candidates.isEmpty()) {
                    Substitution.Resolver resolver = substitution.resolve(candidates.getOnly());
                    if (resolver.isResolved()) {
                        resolver.apply(instrumentedType,
                                (opcode & Opcodes.ACC_STATIC) != 0 || internalName.equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME)
                                        ? candidates.getOnly().getParameters().asTypeList()
                                        : new TypeList.Generic.Explicit(CompoundList.of(candidates.getOnly().getDeclaringType(), candidates.getOnly().getParameters().asTypeList())),
                                candidates.getOnly().isConstructor()
                                        ? candidates.getOnly().getDeclaringType().asGenericType()
                                        : candidates.getOnly().getReturnType()).apply(mv, implementationContext);
                        if (candidates.getOnly().isConstructor()) {
                            stackSizeBuffer = new StackManipulation.Compound(
                                    Duplication.SINGLE.flipOver(TypeDescription.OBJECT),
                                    Removal.SINGLE,
                                    Removal.SINGLE,
                                    Duplication.SINGLE.flipOver(TypeDescription.OBJECT),
                                    Removal.SINGLE,
                                    Removal.SINGLE
                            ).apply(mv, implementationContext).getMaximalSize();
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
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(maxStack + stackSizeBuffer, maxLocals);
        }
    }
}
