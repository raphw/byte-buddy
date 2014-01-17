package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.ByteCodeAppender;

import java.util.Iterator;

public interface MethodDelegationBinder {

    static interface BoundMethodDelegation extends ByteCodeAppender {

        boolean isBound();

        Integer getBindingIndex(Object identificationToken);

        MethodDescription getBindingTarget();
    }

    static interface AmbiguityResolver {

        static enum Resolution {

            NONE(true),
            LEFT(false),
            RIGHT(false),
            AMBIGUOUS(true);

            private final boolean unresolved;

            private Resolution(boolean unresolved) {
                this.unresolved = unresolved;
            }

            public boolean isUnresolved() {
                return unresolved;
            }

            public Resolution merge(Resolution other) {
                switch (this) {
                    case NONE:
                        return other;
                    case AMBIGUOUS:
                        return AMBIGUOUS;
                    case LEFT:
                    case RIGHT:
                        return other == this ? other : AMBIGUOUS;
                    default:
                        throw new AssertionError();
                }
            }
        }

        static class Chain implements AmbiguityResolver {

            private final Iterable<AmbiguityResolver> ambiguityResolvers;

            public Chain(Iterable<AmbiguityResolver> ambiguityResolvers) {
                this.ambiguityResolvers = ambiguityResolvers;
            }

            @Override
            public Resolution resolve(MethodDescription source,
                                      BoundMethodDelegation left,
                                      BoundMethodDelegation right) {
                Resolution resolution = Resolution.NONE;
                Iterator<AmbiguityResolver> iterator = ambiguityResolvers.iterator();
                while (resolution.isUnresolved() && iterator.hasNext()) {
                    resolution = iterator.next().resolve(source, left, right);
                }
                return resolution;
            }
        }

        Resolution resolve(MethodDescription source, BoundMethodDelegation left, BoundMethodDelegation right);
    }

    BoundMethodDelegation bind(MethodDescription source, MethodDescription target);
}
