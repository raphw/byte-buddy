package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.type.TypeDescription;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public interface MethodDelegationBinder {

    static interface BoundMethodDelegation extends ByteCodeAppender{

        boolean isBound();

        Integer getBindingIndex(Object identificationToken);

        MethodDescription getBindingTarget();
    }

    static interface AmbiguityResolver {

        static enum Resolution {

            UNKNOWN(true),
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
                    case UNKNOWN:
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
                Resolution resolution = Resolution.UNKNOWN;
                Iterator<AmbiguityResolver> iterator = ambiguityResolvers.iterator();
                while (resolution.isUnresolved() && iterator.hasNext()) {
                    resolution = iterator.next().resolve(source, left, right);
                }
                return resolution;
            }
        }

        Resolution resolve(MethodDescription source, BoundMethodDelegation left, BoundMethodDelegation right);
    }

    static class Processor {

        private static final int ONLY = 0;
        private static final int LEFT = 0;
        private static final int RIGHT = 1;

        private final MethodDelegationBinder methodDelegationBinder;
        private final AmbiguityResolver ambiguityResolver;

        public Processor(MethodDelegationBinder methodDelegationBinder,
                         AmbiguityResolver ambiguityResolver) {
            this.methodDelegationBinder = methodDelegationBinder;
            this.ambiguityResolver = ambiguityResolver;
        }

        public BoundMethodDelegation process(TypeDescription typeDescription,
                                             MethodDescription source,
                                             Iterable<? extends MethodDescription> targets) {
            List<BoundMethodDelegation> possibleDelegations = bind(typeDescription, source, targets);
            if (possibleDelegations.size() == 0) {
                throw new IllegalArgumentException("No method can be bound to " + source);
            }
            return resolve(source, possibleDelegations);
        }

        private List<BoundMethodDelegation> bind(TypeDescription typeDescription,
                                                 MethodDescription source,
                                                 Iterable<? extends MethodDescription> targets) {
            List<BoundMethodDelegation> possibleDelegations = new LinkedList<BoundMethodDelegation>();
            for (MethodDescription target : targets) {
                BoundMethodDelegation boundMethodDelegation = methodDelegationBinder.bind(typeDescription, source, target);
                if (boundMethodDelegation.isBound()) {
                    possibleDelegations.add(boundMethodDelegation);
                }
            }
            return possibleDelegations;
        }

        private BoundMethodDelegation resolve(MethodDescription source,
                                              List<BoundMethodDelegation> targets) {
            switch (targets.size()) {
                case 1:
                    return targets.get(ONLY);
                case 2: {
                    BoundMethodDelegation left = targets.get(LEFT);
                    BoundMethodDelegation right = targets.get(RIGHT);
                    switch (ambiguityResolver.resolve(source, left, right)) {
                        case LEFT:
                            return left;
                        case RIGHT:
                            return right;
                        case AMBIGUOUS:
                        case UNKNOWN:
                            throw new IllegalArgumentException("Could not resolve ambiguous delegation to " + left + " or " + right);
                        default:
                            throw new AssertionError();
                    }
                }
                default: /* case 3+: */ {
                    BoundMethodDelegation left = targets.get(LEFT);
                    BoundMethodDelegation right = targets.get(RIGHT);
                    switch (ambiguityResolver.resolve(source, left, right)) {
                        case LEFT:
                            targets.remove(RIGHT);
                            return resolve(source, targets);
                        case RIGHT:
                            targets.remove(LEFT);
                            return resolve(source, targets);
                        case AMBIGUOUS:
                        case UNKNOWN:
                            targets.remove(RIGHT); // Remove right element first due to index alteration!
                            targets.remove(LEFT);
                            BoundMethodDelegation subResult = resolve(source, targets);
                            switch (ambiguityResolver.resolve(source, left, subResult).merge(ambiguityResolver.resolve(source, right, subResult))) {
                                case RIGHT:
                                    return subResult;
                                case LEFT:
                                case AMBIGUOUS:
                                case UNKNOWN:
                                    throw new IllegalArgumentException("Could not resolve ambiguous delegation to either " + left + " or " + right);
                                default:
                                    throw new AssertionError();
                            }
                        default:
                            throw new AssertionError();
                    }
                }
            }
        }
    }

    BoundMethodDelegation bind(TypeDescription typeDescription, MethodDescription source, MethodDescription target);
}
