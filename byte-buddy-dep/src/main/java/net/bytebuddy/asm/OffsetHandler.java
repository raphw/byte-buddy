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

        int self();

        int argument(int index);

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
            public int self() {
                return 0;
            }

            @Override
            public int argument(int index) {
                return instrumentedMethod.getParameters().get(index).getOffset();
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
        class ForMethodExit implements Resolved {

            private final MethodDescription instrumentedMethod;

            private final int base;

            private final TypeDefinition enterType;

            protected ForMethodExit(MethodDescription instrumentedMethod, int base, TypeDefinition enterType) {
                this.instrumentedMethod = instrumentedMethod;
                this.base = base;
                this.enterType = enterType;
            }

            @Override
            public boolean isNonInitialized() {
                return false;
            }

            @Override
            public int self() {
                return base;
            }

            @Override
            public int argument(int index) {
                return base + instrumentedMethod.getParameters().get(index).getOffset();
            }

            @Override
            public int enter() {
                return base + instrumentedMethod.getStackSize();
            }

            @Override
            public int returned() {
                return base + instrumentedMethod.getStackSize() + enterType.getStackSize().getSize();
            }

            @Override
            public int thrown() {
                return base + instrumentedMethod.getStackSize() + enterType.getStackSize().getSize() + instrumentedMethod.getReturnType().getStackSize().getSize();
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
            return new Resolved.ForMethodExit(instrumentedMethod, 0, enterType);
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
                methodVisitor.visitVarInsn(type.getOpcode(Opcodes.ALOAD), 0);
                methodVisitor.visitVarInsn(type.getOpcode(Opcodes.ASTORE), instrumentedMethod.getStackSize());
                stackSize = StackSize.SINGLE;
            } else {
                stackSize = StackSize.ZERO;
            }
            for (ParameterDescription parameterDescription : instrumentedMethod.getParameters()) {
                Type type = Type.getType(parameterDescription.getType().asErasure().getDescriptor());
                methodVisitor.visitVarInsn(type.getOpcode(Opcodes.ALOAD), parameterDescription.getOffset());
                methodVisitor.visitVarInsn(type.getOpcode(Opcodes.ASTORE), instrumentedMethod.getStackSize() + parameterDescription.getOffset());
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
            return new Resolved.ForMethodExit(instrumentedMethod, instrumentedMethod.getStackSize(), enterType);
        }
    }
}
