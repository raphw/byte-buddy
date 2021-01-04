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
package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.utility.JavaConstant;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * An exact invocation of a method handle with a polymorphic signature.
 */
@HashCodeAndEqualsPlugin.Enhance
public class HandleInvocation implements StackManipulation {

    /**
     * The name of the {@code java.lang.invoke.MethodHandle} type.
     */
    private static final String METHOD_HANDLE_NAME = "java/lang/invoke/MethodHandle";

    /**
     * The name of the {@code invokeExact} method.
     */
    private static final String INVOKE_EXACT = "invokeExact";

    /**
     * The method type of the invoked handle.
     */
    private final JavaConstant.MethodType methodType;

    /**
     * Creates a public invocation of a method handle.
     *
     * @param methodType The method type of the invoked handle.
     */
    public HandleInvocation(JavaConstant.MethodType methodType) {
        this.methodType = methodType;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValid() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, METHOD_HANDLE_NAME, INVOKE_EXACT, methodType.getDescriptor(), false);
        int size = methodType.getReturnType().getStackSize().getSize() - methodType.getParameterTypes().getStackSize();
        return new Size(size, Math.max(size, 0));
    }
}
