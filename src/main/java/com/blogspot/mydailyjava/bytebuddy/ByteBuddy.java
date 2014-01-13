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

    public static enum TypeManifestation implements ModifierContributor {

        CONCRETE(0),
        FINAL(Opcodes.ACC_FINAL),
        ABSTRACT(Opcodes.ACC_ABSTRACT),
        INTERFACE(Opcodes.ACC_INTERFACE);

        private final int mask;

        private TypeManifestation(int mask) {
            this.mask = mask;
        }

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
    public static final TypeManifestation DEFAULT_TYPE_MANIFESTATION = TypeManifestation.CONCRETE;
    public static final SyntheticState DEFAULT_SYNTHETIC_STATE = SyntheticState.NON_SYNTHETIC;

    public static ByteBuddy make() {
        return new ByteBuddy(DEFAULT_CLASS_VERSION, DEFAULT_VISIBILITY, DEFAULT_TYPE_MANIFESTATION,
                DEFAULT_SYNTHETIC_STATE, new NameMaker.PrefixingRandom(DEFAULT_NAME_PREFIX));
    }

    private final int classVersion;
    private final Visibility visibility;
    private final TypeManifestation typeManifestation;
    private final SyntheticState syntheticState;
    private final NameMaker nameMaker;

    protected ByteBuddy(int classVersion, Visibility visibility, TypeManifestation typeManifestation,
                        SyntheticState syntheticState, NameMaker nameMaker) {
        this.classVersion = classVersion;
        this.visibility = visibility;
        this.typeManifestation = typeManifestation;
        this.syntheticState = syntheticState;
        this.nameMaker = nameMaker;
    }

    public ByteBuddy withDefaultClassVersion(int classVersion) {
        return new ByteBuddy(checkClassVersion(classVersion), visibility, typeManifestation, syntheticState, nameMaker);
    }

    public ByteBuddy withDefaultVisibility(Visibility visibility) {
        return new ByteBuddy(classVersion, checkNotNull(visibility), typeManifestation, syntheticState, nameMaker);
    }

    public ByteBuddy withDefaultTypeManifestation(TypeManifestation typeManifestation) {
        return new ByteBuddy(classVersion, visibility, checkNotNull(typeManifestation), syntheticState, nameMaker);
    }

    public ByteBuddy withDefaultSyntheticState(SyntheticState syntheticState) {
        return new ByteBuddy(classVersion, visibility, typeManifestation, checkNotNull(syntheticState), nameMaker);
    }

    public ByteBuddy withNameMaker(NameMaker nameMaker) {
        return new ByteBuddy(classVersion, visibility, typeManifestation, syntheticState, checkNotNull(nameMaker));
    }

    public int getClassVersion() {
        return classVersion;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public TypeManifestation getTypeManifestation() {
        return typeManifestation;
    }

    public SyntheticState getSyntheticState() {
        return syntheticState;
    }

    public NameMaker getNameMaker() {
        return nameMaker;
    }

    public DynamicProxy.Builder subclass(Class<?> type) {
        return SubclassDynamicProxyBuilder.of(type, this);
    }

    private static <T> T checkNotNull(T type) {
        if (type == null) {
            throw new NullPointerException();
        }
        return type;
    }

    private static int checkClassVersion(int classVersion) {
        if (!(classVersion > 0)) {
            throw new IllegalArgumentException("Class version " + classVersion + " is not valid");
        }
        return classVersion;
    }
}
