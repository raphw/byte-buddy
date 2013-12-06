package com.blogspot.mydailyjava.bytebuddy.extraction.method.stack;

import com.blogspot.mydailyjava.bytebuddy.extraction.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.extraction.context.MethodContext;
import org.objectweb.asm.MethodVisitor;

public interface CallStackArgument {

    static final class Size {

        private final int finalSize;
        private final int maximalSize;

        public Size(int finalSize, int maximalSize) {
            this.finalSize = finalSize;
            this.maximalSize = maximalSize;
        }

        public int getFinalSize() {
            return finalSize;
        }

        public int getMaximalSize() {
            return maximalSize;
        }

        public Size merge(Size other) {
            return new Size(finalSize + other.finalSize, Math.max(maximalSize + other.finalSize, other.maximalSize));
        }
    }

    Size load(MethodVisitor methodVisitor, ClassContext classContext, MethodContext methodContext);
}
