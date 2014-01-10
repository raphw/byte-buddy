package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.type.ModifierContributor;
import com.blogspot.mydailyjava.bytebuddy.type.scaffold.SubclassDynamicProxyBuilder;
import org.objectweb.asm.Opcodes;

public class ByteBuddy {

    public static enum Visibility implements ModifierContributor {

        PUBLIC(Opcodes.ACC_PUBLIC),
        PROTECTED(Opcodes.ACC_PROTECTED),
        PACKAGE_PRIVATE(0),
        PRIVATE(Opcodes.ACC_PRIVATE);

        private final int mask;

        private Visibility(int mask) {
            this.mask = mask;
        }

        @Override
        public int getMask() {
            return mask;
        }
    }

    public static enum Manifestation implements ModifierContributor {

        CONCRETE(0),
        ABSTRACT(Opcodes.ACC_ABSTRACT),
        INTERFACE(Opcodes.ACC_INTERFACE);

        private final int mask;

        private Manifestation(int mask) {
            this.mask = mask;
        }

        public int getMask() {
            return mask;
        }
    }

    public static enum FinalState implements ModifierContributor {

        FINAL(Opcodes.ACC_FINAL),
        NON_FINAL(0);

        public static FinalState is(boolean isFinal) {
            return isFinal ? FINAL : NON_FINAL;
        }

        private final int mask;

        private FinalState(int mask) {
            this.mask = mask;
        }

        @Override
        public int getMask() {
            return mask;
        }
    }

    public static enum SyntheticState implements ModifierContributor {

        SYNTHETIC(Opcodes.ACC_SYNTHETIC),
        NON_SYNTHETIC(0);

        public static SyntheticState is(boolean synthetic) {
            return synthetic ? SYNTHETIC : NON_SYNTHETIC;
        }

        private final int mask;

        private SyntheticState(int mask) {
            this.mask = mask;
        }

        @Override
        public int getMask() {
            return mask;
        }
    }

    public static final String DEFAULT_NAME_PREFIX = "ByteBuddy";
    public static final int DEFAULT_CLASS_VERSION = Opcodes.V1_5;
    public static final Visibility DEFAULT_VISIBILITY = Visibility.PUBLIC;
    public static final Manifestation DEFAULT_MANIFESTATION = Manifestation.CONCRETE;
    public static final FinalState DEFAULT_FINAL_STATE = FinalState.NON_FINAL;
    public static final SyntheticState DEFAULT_SYNTHETIC_STATE = SyntheticState.NON_SYNTHETIC;

    public DynamicProxy.Builder subclass(Class<?> type) {
        return SubclassDynamicProxyBuilder.of(type);
    }
}
