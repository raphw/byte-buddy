package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.type.TypeDescription;

import java.util.Collections;
import java.util.Set;

public enum TrivialType implements AuxiliaryType, MethodLookupEngine.Factory, MethodLookupEngine {

    INSTANCE;

    @Override
    public DynamicType make(String auxiliaryTypeName,
                            ClassFileVersion classFileVersion,
                            MethodAccessorFactory methodAccessorFactory) {
        return new ByteBuddy(classFileVersion)
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(auxiliaryTypeName)
                .modifiers(DEFAULT_TYPE_MODIFIER)
                .methodLookupEngine(this)
                .make();
    }

    @Override
    public MethodLookupEngine make(ClassFileVersion classFileVersion) {
        return this;
    }

    @Override
    public Finding process(TypeDescription typeDescription) {
        return new Finding.Default(typeDescription,
                new MethodList.Empty(),
                Collections.<TypeDescription, Set<MethodDescription>>emptyMap());
    }
}
