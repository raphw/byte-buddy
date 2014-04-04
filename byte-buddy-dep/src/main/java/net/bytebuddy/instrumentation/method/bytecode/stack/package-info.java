/**
 * This package offers an abstraction of creating Java byte code by only manipulating a method's operand stack.
 * Such manipulations are represented by a {@link net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation}
 * where each stack manipulation indicates its size impact onto the operand stack. Any such manipulation indicates its
 * validity what allows for conditional stack manipulations that are only performed if such a manipulation is applicable.
 */
package net.bytebuddy.instrumentation.method.bytecode.stack;
