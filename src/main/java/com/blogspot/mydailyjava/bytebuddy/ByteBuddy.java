package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapper;
import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapperChain;
import com.blogspot.mydailyjava.bytebuddy.type.instrumentation.DynamicProxy;
import com.blogspot.mydailyjava.bytebuddy.type.scaffold.SubclassDynamicProxyBuilder;
import org.objectweb.asm.Opcodes;

public class ByteBuddy {

    public static final String DEFAULT_NAME_PREFIX = "ByteBuddy";
    public static final int DEFAULT_CLASS_VERSION = Opcodes.V1_5;
    public static final Visibility DEFAULT_VISIBILITY = Visibility.PUBLIC;
    public static final TypeManifestation DEFAULT_TYPE_MANIFESTATION = TypeManifestation.CONCRETE;
    public static final SyntheticState DEFAULT_SYNTHETIC_STATE = SyntheticState.NON_SYNTHETIC;

    public static ByteBuddy make() {
        return new ByteBuddy(DEFAULT_CLASS_VERSION,
                DEFAULT_VISIBILITY,
                DEFAULT_TYPE_MANIFESTATION,
                DEFAULT_SYNTHETIC_STATE,
                new NameMaker.PrefixingRandom(DEFAULT_NAME_PREFIX),
                new ClassVisitorWrapperChain());
    }

    private final int classVersion;
    private final Visibility visibility;
    private final TypeManifestation typeManifestation;
    private final SyntheticState syntheticState;
    private final NameMaker nameMaker;
    private final ClassVisitorWrapperChain classVisitorWrapperChain;

    protected ByteBuddy(int classVersion,
                        Visibility visibility,
                        TypeManifestation typeManifestation,
                        SyntheticState syntheticState,
                        NameMaker nameMaker,
                        ClassVisitorWrapperChain classVisitorWrapperChain) {
        this.classVersion = classVersion;
        this.visibility = visibility;
        this.typeManifestation = typeManifestation;
        this.syntheticState = syntheticState;
        this.nameMaker = nameMaker;
        this.classVisitorWrapperChain = classVisitorWrapperChain;
    }

    public ByteBuddy withDefaultClassVersion(int classVersion) {
        return new ByteBuddy(checkClassVersion(classVersion),
                visibility,
                typeManifestation,
                syntheticState,
                nameMaker,
                classVisitorWrapperChain);
    }

    public ByteBuddy withDefaultVisibility(Visibility visibility) {
        return new ByteBuddy(classVersion,
                checkNotNull(visibility),
                typeManifestation,
                syntheticState,
                nameMaker,
                classVisitorWrapperChain);
    }

    public ByteBuddy withDefaultTypeManifestation(TypeManifestation typeManifestation) {
        return new ByteBuddy(classVersion,
                visibility,
                checkNotNull(typeManifestation),
                syntheticState,
                nameMaker,
                classVisitorWrapperChain);
    }

    public ByteBuddy withDefaultSyntheticState(SyntheticState syntheticState) {
        return new ByteBuddy(classVersion,
                visibility,
                typeManifestation,
                checkNotNull(syntheticState),
                nameMaker,
                classVisitorWrapperChain);
    }

    public ByteBuddy withNameMaker(NameMaker nameMaker) {
        return new ByteBuddy(classVersion,
                visibility,
                typeManifestation,
                syntheticState,
                checkNotNull(nameMaker),
                classVisitorWrapperChain);
    }

    public ByteBuddy prependClassVisitorWrapper(ClassVisitorWrapper classVisitorWrapper) {
        return new ByteBuddy(classVersion,
                visibility,
                typeManifestation,
                syntheticState,
                checkNotNull(nameMaker),
                classVisitorWrapperChain.prepend(checkNotNull(classVisitorWrapper)));
    }

    public ByteBuddy appendClassVisitorWrapper(ClassVisitorWrapper classVisitorWrapper) {
        return new ByteBuddy(classVersion,
                visibility,
                typeManifestation,
                syntheticState,
                checkNotNull(nameMaker),
                classVisitorWrapperChain.append(checkNotNull(classVisitorWrapper)));
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

    public ClassVisitorWrapperChain getClassVisitorWrapperChain() {
        return classVisitorWrapperChain;
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
