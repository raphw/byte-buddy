package com.blogspot.mydailyjava.bytebuddy.type;

import com.blogspot.mydailyjava.bytebuddy.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Collection;

public class TypeDescription implements NamingStrategy.UnnamedType {

    private final ClassVersion classVersion;

    private final Class<?> superClass;
    private final Collection<Class<?>> interfaces;

    private final Visibility visibility;
    private final TypeManifestation typeManifestation;
    private final SyntheticState syntheticState;

    private final String name;

    public TypeDescription(ClassVersion classVersion,
                           Class<?> superClass,
                           Collection<Class<?>> interfaces,
                           Visibility visibility,
                           TypeManifestation typeManifestation,
                           SyntheticState syntheticState,
                           NamingStrategy namingStrategy) {
        this.classVersion = classVersion;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.visibility = visibility;
        this.typeManifestation = typeManifestation;
        this.syntheticState = syntheticState;
        this.name = namingStrategy.getName(this);
    }

    @Override
    public int getClassVersion() {
        return classVersion.getVersionNumber();
    }

    @Override
    public Class<?> getSuperClass() {
        return superClass;
    }

    @Override
    public Collection<Class<?>> getInterfaces() {
        return interfaces;
    }

    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public TypeManifestation getTypeManifestation() {
        return typeManifestation;
    }

    @Override
    public SyntheticState getSyntheticState() {
        return syntheticState;
    }

    public String getName() {
        return name;
    }

    public String getInternalName() {
        return name.replace('.', '/');
    }

    public String getSuperClassInternalName() {
        return Type.getInternalName(superClass);
    }

    public String[] getInterfacesInternalNames() {
        if (interfaces.size() == 0) {
            return null;
        }
        String[] internalName = new String[interfaces.size()];
        int i = 0;
        for (Class<?> type : interfaces) {
            internalName[i] = Type.getInternalName(type);
        }
        return internalName;
    }

    public int getTypeModifier() {
        return visibility.getMask() + typeManifestation.getMask() + syntheticState.getMask() + Opcodes.ACC_SUPER;
    }
}
