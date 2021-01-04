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
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.utility.JavaConstant;
import org.objectweb.asm.MethodVisitor;

/**
 * A constant representing a {@link JavaConstant}.
 */
@HashCodeAndEqualsPlugin.Enhance
public class JavaConstantValue implements StackManipulation {

    /**
     * The instance to load onto the operand stack.
     */
    private final JavaConstant javaConstant;

    /**
     * Creates a constant pool value representing a {@link JavaConstant}.
     *
     * @param javaConstant The instance to load onto the operand stack.
     */
    public JavaConstantValue(JavaConstant javaConstant) {
        this.javaConstant = javaConstant;
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
        methodVisitor.visitLdcInsn(javaConstant.asConstantPoolValue());
        return StackSize.SINGLE.toIncreasingSize();
    }
}
