package net.bytebuddy.implementation.auxiliary;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;

/**
 * A trivial type that extends {@link java.lang.Object} without defining any fields, methods or constructors.
 * This type is meant to be used as a marker type only.
 */
public enum TrivialType implements AuxiliaryType {

    /**
     * The singleton instance.
     */
    INSTANCE;

    @Override
    public DynamicType make(String auxiliaryTypeName,
                            ClassFileVersion classFileVersion,
                            MethodAccessorFactory methodAccessorFactory) {
        return new ByteBuddy(classFileVersion)
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(auxiliaryTypeName)
                .modifiers(DEFAULT_TYPE_MODIFIER)
                .methodGraphCompiler(MethodGraph.Empty.INSTANCE)
                .make();
    }

    @Override
    public String toString() {
        return "TrivialType." + name();
    }
}
