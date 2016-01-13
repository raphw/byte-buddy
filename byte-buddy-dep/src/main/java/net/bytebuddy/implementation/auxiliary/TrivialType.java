package net.bytebuddy.implementation.auxiliary;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;

import java.util.Collections;

/**
 * A trivial type that extends {@link java.lang.Object} without defining any fields, methods or constructors.
 * This type is meant to be used as a marker type only.
 */
public enum TrivialType implements AuxiliaryType {

    /**
     * A trivial type that defines the {@link SignatureRelevant} annotation.
     */
    SIGNATURE_RELEVANT(true),

    /**
     * A non-annotated trivial type.
     */
    PLAIN(false);

    /**
     * Determines if this type determines the {@link SignatureRelevant} annotation.
     */
    private final boolean eager;

    /**
     * Creates a new trivial type.
     *
     * @param eager Determines if this type determines the {@link SignatureRelevant} annotation.
     */
    TrivialType(boolean eager) {
        this.eager = eager;
    }

    @Override
    public DynamicType make(String auxiliaryTypeName,
                            ClassFileVersion classFileVersion,
                            MethodAccessorFactory methodAccessorFactory) {
        return new ByteBuddy(classFileVersion)
                .with(MethodGraph.Empty.INSTANCE) // avoid parsing the graph
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .annotateType(eager
                        ? Collections.singletonList(AnnotationDescription.Builder.ofType(SignatureRelevant.class).build())
                        : Collections.<AnnotationDescription>emptyList())
                .name(auxiliaryTypeName)
                .modifiers(DEFAULT_TYPE_MODIFIER)
                .make();
    }

    @Override
    public String toString() {
        return "TrivialType." + name();
    }
}
