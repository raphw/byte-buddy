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
package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.utility.ConstantValue;
import net.bytebuddy.utility.JavaConstant;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * A constant representing a {@link JavaConstant}. By using this stack manipulation, a value is always
 * represented as a constant pool value and no attempt is made to load the value via a specialized byte
 * code instruction, in contrast to using {@link ConstantValue#toStackManipulation()}.
 */
@HashCodeAndEqualsPlugin.Enhance
public class JavaConstantValue extends StackManipulation.AbstractBase {

    /**
     * The instance to load onto the operand stack.
     */
    private final JavaConstant constant;

    /**
     * Creates a constant pool value representing a {@link JavaConstant}.
     *
     * @param constant The instance to load onto the operand stack.
     */
    public JavaConstantValue(JavaConstant constant) {
        this.constant = constant;
    }

    /**
     * {@inheritDoc}
     */
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitLdcInsn(constant.accept(Visitor.INSTANCE));
        return constant.getTypeDescription().getStackSize().toIncreasingSize();
    }

    /**
     * A visitor to resolve a {@link JavaConstant} to a ASM constant pool representation.
     */
    public enum Visitor implements JavaConstant.Visitor<Object> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public Object onValue(JavaConstant.Simple<?> constant) {
            return constant.getValue();
        }

        /**
         * {@inheritDoc}
         */
        public Type onType(JavaConstant.Simple<TypeDescription> constant) {
            return Type.getType(constant.getValue().getDescriptor());
        }

        /**
         * {@inheritDoc}
         */
        public Type onMethodType(JavaConstant.MethodType constant) {
            StringBuilder stringBuilder = new StringBuilder().append('(');
            for (TypeDescription parameterType : constant.getParameterTypes()) {
                stringBuilder.append(parameterType.getDescriptor());
            }
            return Type.getMethodType(stringBuilder.append(')').append(constant.getReturnType().getDescriptor()).toString());
        }

        /**
         * {@inheritDoc}
         */
        public Handle onMethodHandle(JavaConstant.MethodHandle constant) {
            return new Handle(constant.getHandleType().getIdentifier(),
                    constant.getOwnerType().getInternalName(),
                    constant.getName(),
                    constant.getDescriptor(),
                    constant.getOwnerType().isInterface());
        }

        /**
         * {@inheritDoc}
         */
        public ConstantDynamic onDynamic(JavaConstant.Dynamic constant) {
            Object[] argument = new Object[constant.getArguments().size()];
            for (int index = 0; index < argument.length; index++) {
                argument[index] = constant.getArguments().get(index).accept(this);
            }
            return new ConstantDynamic(constant.getName(),
                    constant.getTypeDescription().getDescriptor(),
                    onMethodHandle(constant.getBootstrap()),
                    argument);
        }
    }
}
