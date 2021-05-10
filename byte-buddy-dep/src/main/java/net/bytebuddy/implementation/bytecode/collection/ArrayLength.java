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
package net.bytebuddy.implementation.bytecode.collection;

import net.bytebuddy.implementation.Implementation.Context;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Allows accessing the length of the array.
 */
public enum ArrayLength implements StackManipulation {
	
	INSTANCE;
	
	@Override
	public boolean isValid() {
		return true;
	}
	
	@Override
	public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
		methodVisitor.visitInsn(Opcodes.ARRAYLENGTH);
		return new Size(0, 0);
	}
}
