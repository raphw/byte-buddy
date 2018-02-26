package net.bytebuddy.asm;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.utility.CompoundList;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.List;

public interface OffsetHandler {

    int prepare(MethodVisitor methodVisitor);

    List<TypeDescription> getIntermediateTypes();

    Resolved resolveEnter();

    Resolved resolveExit(TypeDefinition enterType);

    enum Factory {

        SIMPLE {
            @Override
            protected OffsetHandler make(MethodDescription instrumentedMethod) {
                return new Simple(instrumentedMethod);
            }
        },

        COPYING {
            @Override
            protected OffsetHandler make(MethodDescription instrumentedMethod) {
                return new Copying(instrumentedMethod);
            }
        };

        protected static Factory of(boolean copyArguments) {
            return copyArguments ? COPYING : SIMPLE;
        }

        protected abstract OffsetHandler make(MethodDescription instrumentedMethod);
    }

    interface Resolved {

        boolean isNonInitialized();

        int argument(int offset);

        int enter();

        int returned();

        int thrown();

        @EqualsAndHashCode
        class ForMethodEnter implements Resolved {

            private final MethodDescription instrumentedMethod;

            protected ForMethodEnter(MethodDescription instrumentedMethod) {
                this.instrumentedMethod = instrumentedMethod;
            }

            @Override
            public boolean isNonInitialized() {
                return instrumentedMethod.isConstructor();
            }

            @Override
            public int argument(int offset) {
                return offset;
            }

            @Override
            public int enter() {
                return instrumentedMethod.getStackSize();
            }

            @Override
            public int returned() {
                throw new IllegalStateException();
            }

            @Override
            public int thrown() {
                throw new IllegalStateException();
            }
        }

        @EqualsAndHashCode
        abstract class ForMethodExit implements Resolved {

            protected final MethodDescription instrumentedMethod;

            protected final TypeDefinition enterType;

            protected ForMethodExit(MethodDescription instrumentedMethod, TypeDefinition enterType) {
                this.instrumentedMethod = instrumentedMethod;
                this.enterType = enterType;
            }

            @Override
            public boolean isNonInitialized() {
                return false;
            }

            @Override
            public int enter() {
                return instrumentedMethod.getStackSize();
            }

            protected static class Simple extends ForMethodExit {

                protected Simple(MethodDescription instrumentedMethod, TypeDefinition enterType) {
                    super(instrumentedMethod, enterType);
                }

                @Override
                public int argument(int offset) {
                    return offset;
                }

                @Override
                public int returned() {
                    return enterType.getStackSize().getSize() + instrumentedMethod.getStackSize();
                }

                @Override
                public int thrown() {
                    return instrumentedMethod.getStackSize() + enterType.getStackSize().getSize() + instrumentedMethod.getReturnType().getStackSize().getSize();
                }
            }

            protected static class WithCopiedArguments extends ForMethodExit {

                protected WithCopiedArguments(MethodDescription instrumentedMethod, TypeDefinition enterType) {
                    super(instrumentedMethod, enterType);
                }

                @Override
                public int argument(int offset) {
                    return instrumentedMethod.getStackSize() + enterType.getStackSize().getSize() + offset;
                }

                @Override
                public int returned() {
                    return instrumentedMethod.getStackSize() * 2 + enterType.getStackSize().getSize();
                }

                @Override
                public int thrown() {
                    return instrumentedMethod.getStackSize() * 2 + enterType.getStackSize().getSize() + instrumentedMethod.getReturnType().getStackSize().getSize();
                }
            }
        }
    }

    @EqualsAndHashCode
    class Simple implements OffsetHandler {

        private final MethodDescription instrumentedMethod;

        protected Simple(MethodDescription instrumentedMethod) {
            this.instrumentedMethod = instrumentedMethod;
        }

        @Override
        public int prepare(MethodVisitor methodVisitor) {
            return 0;
        }

        @Override
        public List<TypeDescription> getIntermediateTypes() {
            return Collections.emptyList();
        }

        @Override
        public Resolved resolveEnter() {
            return new Resolved.ForMethodEnter(instrumentedMethod);
        }

        @Override
        public Resolved resolveExit(TypeDefinition enterType) {
            return new Resolved.ForMethodExit.Simple(instrumentedMethod, enterType);
        }
    }

    @EqualsAndHashCode
    class Copying implements OffsetHandler {

        private final MethodDescription instrumentedMethod;

        protected Copying(MethodDescription instrumentedMethod) {
            this.instrumentedMethod = instrumentedMethod;
        }

        @Override
        public int prepare(MethodVisitor methodVisitor) {
            StackSize stackSize;
            if (!instrumentedMethod.isStatic()) {
                Type type = Type.getType(instrumentedMethod.getDeclaringType().asErasure().getDescriptor());
                methodVisitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), 0);
                methodVisitor.visitVarInsn(type.getOpcode(Opcodes.ISTORE), instrumentedMethod.getStackSize());
                stackSize = StackSize.SINGLE;
            } else {
                stackSize = StackSize.ZERO;
            }
            for (ParameterDescription parameterDescription : instrumentedMethod.getParameters()) {
                Type type = Type.getType(parameterDescription.getType().asErasure().getDescriptor());
                methodVisitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), parameterDescription.getOffset());
                methodVisitor.visitVarInsn(type.getOpcode(Opcodes.ISTORE), instrumentedMethod.getStackSize() + parameterDescription.getOffset());
                stackSize = stackSize.maximum(parameterDescription.getType().getStackSize());
            }
            return stackSize.getSize();
        }

        @Override
        public List<TypeDescription> getIntermediateTypes() {
            return instrumentedMethod.isStatic()
                    ? instrumentedMethod.getParameters().asTypeList().asErasures()
                    : CompoundList.of(instrumentedMethod.getDeclaringType().asErasure(), instrumentedMethod.getParameters().asTypeList().asErasures());
        }

        @Override
        public Resolved resolveEnter() {
            return new Resolved.ForMethodEnter(instrumentedMethod);
        }

        @Override
        public Resolved resolveExit(TypeDefinition enterType) {
            return new Resolved.ForMethodExit.WithCopiedArguments(instrumentedMethod, enterType);
        }
    }
}
