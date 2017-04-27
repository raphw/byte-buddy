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
import net.bytebuddy.implementation.bytecode.Removal;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.constant.DefaultValue;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.CompoundList;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

@EqualsAndHashCode(callSuper = false)
public class MemberSubstitution extends AsmVisitorWrapper.AbstractBase {

    private final MethodGraph.Compiler methodGraphCompiler;

    private final boolean allowUnresolved;

    private final Substitution substitution;

    protected MemberSubstitution(MethodGraph.Compiler methodGraphCompiler, boolean allowUnresolved, Substitution substitution) {
        this.methodGraphCompiler = methodGraphCompiler;
        this.allowUnresolved = allowUnresolved;
        this.substitution = substitution;
    }

    public MemberSubstitution stub(ElementMatcher<? super ByteCodeElement> matcher) {
        return new MemberSubstitution(methodGraphCompiler, allowUnresolved, new Substitution.Compound(substitution, new Substitution.ForStub(matcher, matcher)));
    }

    public MemberSubstitution stubField(ElementMatcher<? super FieldDescription.InDefinedShape> matcher) {
        return new MemberSubstitution(methodGraphCompiler, allowUnresolved, new Substitution.Compound(substitution, new Substitution.ForStub(matcher, none())));
    }

    public MemberSubstitution stubMethod(ElementMatcher<? super MethodDescription> matcher) {
        return new MemberSubstitution(methodGraphCompiler, allowUnresolved, new Substitution.Compound(substitution, new Substitution.ForStub(none(), matcher)));
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
        return new SubstitutingClassVisitor(classVisitor, methodGraphCompiler, allowUnresolved, substitution, implementationContext, typePool);
    }

    protected static class SubstitutingClassVisitor extends ClassVisitor {

        private final MethodGraph.Compiler methodGraphCompiler;

        private final boolean allowUnresolved;

        private final Substitution substitution;

        private final Implementation.Context implementationContext;

        private final TypePool typePool;

        protected SubstitutingClassVisitor(ClassVisitor classVisitor,
                                           MethodGraph.Compiler methodGraphCompiler,
                                           boolean allowUnresolved,
                                           Substitution substitution,
                                           Implementation.Context implementationContext,
                                           TypePool typePool) {
            super(Opcodes.ASM5, classVisitor);
            this.methodGraphCompiler = methodGraphCompiler;
            this.allowUnresolved = allowUnresolved;
            this.substitution = substitution;
            this.implementationContext = implementationContext;
            this.typePool = typePool;
        }

        @Override
        public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exception) {
            return new SubstitutingMethodVisitor(super.visitMethod(modifiers, internalName, descriptor, signature, exception),
                    methodGraphCompiler,
                    allowUnresolved,
                    substitution,
                    implementationContext,
                    typePool);
        }
    }

    protected static class SubstitutingMethodVisitor extends MethodVisitor {

        private final MethodGraph.Compiler methodGraphCompiler;

        private final boolean allowUnresolved;

        private final Substitution substitution;

        private final Implementation.Context implementationContext;

        private final TypePool typePool;

        private int freeIndex;

        private int additionalStack, additionalVariables;

        protected SubstitutingMethodVisitor(MethodVisitor methodVisitor,
                                            MethodGraph.Compiler methodGraphCompiler,
                                            boolean allowUnresolved,
                                            Substitution substitution,
                                            Implementation.Context implementationContext,
                                            TypePool typePool) {
            super(Opcodes.ASM5, methodVisitor);
            this.methodGraphCompiler = methodGraphCompiler;
            this.allowUnresolved = allowUnresolved;
            this.substitution = substitution;
            this.implementationContext = implementationContext;
            this.typePool = typePool;
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
                        StackSize baseSize;
                        switch (opcode) {
                            case Opcodes.PUTFIELD:
                                arguments = new TypeList.Generic.Explicit(candidates.getOnly().getDeclaringType(), candidates.getOnly().getType());
                                result = TypeDescription.Generic.VOID;
                                baseSize = StackSize.SINGLE;
                                break;
                            case Opcodes.PUTSTATIC:
                                arguments = new TypeList.Generic.Explicit(candidates.getOnly().getType());
                                result = TypeDescription.Generic.VOID;
                                baseSize = StackSize.ZERO;
                                break;
                            case Opcodes.GETFIELD:
                                arguments = new TypeList.Generic.Explicit(candidates.getOnly().getDeclaringType());
                                result = candidates.getOnly().getType();
                                baseSize = StackSize.SINGLE;
                                break;
                            case Opcodes.GETSTATIC:
                                arguments = new TypeList.Generic.Empty();
                                result = candidates.getOnly().getType();
                                baseSize = StackSize.ZERO;
                                break;
                            default:
                                throw new AssertionError();
                        }
                        StackManipulation.Size size = resolver.apply(freeIndex, arguments, result).apply(mv, implementationContext);
                        // TODO: Record size requirements!
                        return;
                    }
                } else if (!allowUnresolved) {
                    throw new IllegalStateException("Could not resolve " + owner.replace('/', '.')
                            + "." + internalName + descriptor + " using " + typePool);
                }
            } else if (!allowUnresolved) {
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
                        StackManipulation.Size size = resolver.apply(freeIndex,
                                (opcode & Opcodes.ACC_STATIC) != 0 || internalName.equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME)
                                        ? candidates.getOnly().getParameters().asTypeList()
                                        : new TypeList.Generic.Explicit(CompoundList.of(candidates.getOnly().getDeclaringType(), candidates.getOnly().getParameters().asTypeList())),
                                candidates.getOnly().getReturnType()).apply(mv, implementationContext);
                        // TODO: Record size requirements!
                        return;
                    }
                } else if (!allowUnresolved) {
                    throw new IllegalStateException("Could not resolve " + owner.replace('/', '.')
                            + "." + internalName + descriptor + " using " + typePool);
                }
            } else if (!allowUnresolved) {
                throw new IllegalStateException("Could not resolve " + owner.replace('/', '.') + " using " + typePool);
            }
            super.visitMethodInsn(opcode, owner, internalName, descriptor, isInterface);
        }
    }

    protected interface Substitution {

        Resolver resolve(FieldDescription.InDefinedShape fieldDescription);

        Resolver resolve(MethodDescription methodDescription);

        interface Resolver {

            boolean isResolved();

            StackManipulation apply(int freeIndex, TypeList.Generic arguments, TypeDescription.Generic result);

            enum Unresolved implements Resolver {

                INSTANCE;

                @Override
                public boolean isResolved() {
                    return false;
                }

                @Override
                public StackManipulation apply(int freeIndex, TypeList.Generic arguments, TypeDescription.Generic result) {
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
                public StackManipulation apply(int freeIndex, TypeList.Generic arguments, TypeDescription.Generic result) {
                    List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(arguments.size());
                    for (TypeDescription argument : arguments.asErasures()) {
                        stackManipulations.add(Removal.of(argument));
                    }
                    return new StackManipulation.Compound(CompoundList.of(stackManipulations, DefaultValue.of(result.asErasure())));
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

        class ForStub implements Substitution {

            private final ElementMatcher<? super FieldDescription.InDefinedShape> fieldMatcher;

            private final ElementMatcher<? super MethodDescription> methodMatcher;

            protected ForStub(ElementMatcher<? super FieldDescription.InDefinedShape> fieldMatcher, ElementMatcher<? super MethodDescription> methodMatcher) {
                this.fieldMatcher = fieldMatcher;
                this.methodMatcher = methodMatcher;
            }

            @Override
            public Resolver resolve(FieldDescription.InDefinedShape fieldDescription) {
                return fieldMatcher.matches(fieldDescription)
                        ? Resolver.Stubbing.INSTANCE
                        : Resolver.Unresolved.INSTANCE;
            }

            @Override
            public Resolver resolve(MethodDescription methodDescription) {
                return methodMatcher.matches(methodDescription)
                        ? Resolver.Stubbing.INSTANCE
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
}
