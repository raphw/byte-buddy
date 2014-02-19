package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapper;
import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers;
import org.objectweb.asm.Opcodes;

public class ByteBuddy {

    public static final int DEFAULT_CLASS_VERSION = Opcodes.V1_5;
    public static final Visibility DEFAULT_VISIBILITY = Visibility.PUBLIC;
    public static final TypeManifestation DEFAULT_TYPE_MANIFESTATION = TypeManifestation.PLAIN;
    public static final String DEFAULT_NAME_PREFIX = "ByteBuddy";
    public static final SyntheticState DEFAULT_SYNTHETIC_STATE = SyntheticState.NON_SYNTHETIC;
    public static final MethodMatcher DEFAULT_IGNORED_METHODS = MethodMatchers.isDefaultFinalize();

    public static ByteBuddy make() {
        return new ByteBuddy(new ClassVersion(DEFAULT_CLASS_VERSION),
                DEFAULT_VISIBILITY,
                DEFAULT_TYPE_MANIFESTATION,
                DEFAULT_SYNTHETIC_STATE,
                new NamingStrategy.PrefixingRandom(DEFAULT_NAME_PREFIX),
                DEFAULT_IGNORED_METHODS,
                new ClassVisitorWrapper.Chain());
    }

    private final ClassVersion classVersion;
    private final Visibility visibility;
    private final TypeManifestation typeManifestation;
    private final SyntheticState syntheticState;
    private final NamingStrategy namingStrategy;
    private final MethodMatcher ignoredMethods;
    private final ClassVisitorWrapper.Chain classVisitorWrapperChain;

    protected ByteBuddy(ClassVersion classVersion,
                        Visibility visibility,
                        TypeManifestation typeManifestation,
                        SyntheticState syntheticState,
                        NamingStrategy namingStrategy,
                        MethodMatcher ignoredMethods,
                        ClassVisitorWrapper.Chain classVisitorWrapperChain) {
        this.classVersion = classVersion;
        this.visibility = visibility;
        this.typeManifestation = typeManifestation;
        this.syntheticState = syntheticState;
        this.namingStrategy = namingStrategy;
        this.ignoredMethods = ignoredMethods;
        this.classVisitorWrapperChain = classVisitorWrapperChain;
    }

    public ByteBuddy withDefaultClassVersion(int versionNumber) {
        return new ByteBuddy(new ClassVersion(versionNumber),
                visibility,
                typeManifestation,
                syntheticState,
                namingStrategy,
                ignoredMethods,
                classVisitorWrapperChain);
    }

    public ByteBuddy withDefaultVisibility(Visibility visibility) {
        return new ByteBuddy(classVersion,
                checkNotNull(visibility),
                typeManifestation,
                syntheticState,
                namingStrategy,
                ignoredMethods,
                classVisitorWrapperChain);
    }

    public ByteBuddy withDefaultTypeManifestation(TypeManifestation typeManifestation) {
        return new ByteBuddy(classVersion,
                visibility,
                checkNotNull(typeManifestation),
                syntheticState,
                namingStrategy,
                ignoredMethods,
                classVisitorWrapperChain);
    }

    public ByteBuddy withDefaultSyntheticState(SyntheticState syntheticState) {
        return new ByteBuddy(classVersion,
                visibility,
                typeManifestation,
                checkNotNull(syntheticState),
                namingStrategy,
                ignoredMethods,
                classVisitorWrapperChain);
    }

    public ByteBuddy withNameMaker(NamingStrategy namingStrategy) {
        return new ByteBuddy(classVersion,
                visibility,
                typeManifestation,
                syntheticState,
                checkNotNull(namingStrategy),
                ignoredMethods,
                classVisitorWrapperChain);
    }

    public ByteBuddy withDefaultIgnoredMethods(MethodMatcher ignoredMethods) {
        return new ByteBuddy(classVersion,
                visibility,
                typeManifestation,
                syntheticState,
                namingStrategy,
                checkNotNull(ignoredMethods),
                classVisitorWrapperChain);
    }

    public ByteBuddy withPrependedClassVisitorWrapper(ClassVisitorWrapper classVisitorWrapper) {
        return new ByteBuddy(
                classVersion,
                visibility,
                typeManifestation,
                syntheticState,
                checkNotNull(namingStrategy),
                ignoredMethods,
                classVisitorWrapperChain.prepend(checkNotNull(classVisitorWrapper)));
    }

    public ByteBuddy withAppendedClassVisitorWrapper(ClassVisitorWrapper classVisitorWrapper) {
        return new ByteBuddy(classVersion,
                visibility,
                typeManifestation,
                syntheticState,
                checkNotNull(namingStrategy),
                ignoredMethods,
                classVisitorWrapperChain.append(checkNotNull(classVisitorWrapper)));
    }

    public ClassVersion getClassVersion() {
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

    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public MethodMatcher getIgnoredMethods() {
        return ignoredMethods;
    }

    public ClassVisitorWrapper.Chain getClassVisitorWrapperChain() {
        return classVisitorWrapperChain;
    }

    public <T> DynamicType.Builder<T> subclass(Class<? extends T> type) {
//        return SubclassDynamicTypeBuilder.of(type, this);
        throw new RuntimeException();
    }

    private static <T> T checkNotNull(T type) {
        if (type == null) {
            throw new NullPointerException();
        }
        return type;
    }
}
