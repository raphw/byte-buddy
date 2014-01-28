package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.collection;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.IntegerConstant;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

public class ArrayFactory {

    private class ArrayAssignment implements Assignment {

        private final List<Assignment> assignments;

        public ArrayAssignment(List<Assignment> assignments) {
            this.assignments = assignments;
        }

        @Override
        public boolean isValid() {
            for(Assignment assignment : assignments) {
                if(!assignment.isValid()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            Size size = IntegerConstant.forValue(assignments.size()).apply(methodVisitor);
            methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, arrayTypeInternalName);
            size = size.aggregate(TypeSize.SINGLE.toIncreasingSize());
            int index = 0;
            for(Assignment assignment : assignments) {
                methodVisitor.visitInsn(Opcodes.DUP);
                size = size.aggregate(TypeSize.SINGLE.toIncreasingSize());
                size = size.aggregate(IntegerConstant.forValue(index++).apply(methodVisitor));
                size = size.aggregate(assignment.apply(methodVisitor));
                methodVisitor.visitInsn(Opcodes.AASTORE);
                size = size.aggregate(TypeSize.SINGLE.toDecreasingSize());
            }
            return size;
        }
    }

    private final String arrayTypeInternalName;

    public ArrayFactory(Class<?> type) {
        this.arrayTypeInternalName = Type.getInternalName(type);
    }

    public Assignment withValues(List<Assignment> assignments) {
        return new ArrayAssignment(assignments);
    }
}

