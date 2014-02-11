package com.blogspot.mydailyjava.bytebuddy.instrumentation.type;

import com.blogspot.mydailyjava.bytebuddy.*;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SubclassLoadedTypeInstrumentation
        extends InstrumentedType.AbstractInstrumentedType
        implements NamingStrategy.UnnamedType {

    private final ClassVersion classVersion;

    private final Class<?> superClass;
    private final Collection<Class<?>> interfaces;

    private final Visibility visibility;
    private final TypeManifestation typeManifestation;
    private final SyntheticState syntheticState;

    private final String name;

    public SubclassLoadedTypeInstrumentation(ClassVersion classVersion,
                                             Class<?> superClass,
                                             Collection<Class<?>> interfaces,
                                             Visibility visibility,
                                             TypeManifestation typeManifestation,
                                             SyntheticState syntheticState,
                                             NamingStrategy namingStrategy) {
        super(Collections.<FieldDescription>emptyList(), Collections.<MethodDescription>emptyList());
        this.classVersion = classVersion;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.visibility = visibility;
        this.typeManifestation = typeManifestation;
        this.syntheticState = syntheticState;
        this.name = namingStrategy.getName(this);
    }

    private SubclassLoadedTypeInstrumentation(ClassVersion classVersion,
                                              Class<?> superClass,
                                              Collection<Class<?>> interfaces,
                                              Visibility visibility,
                                              TypeManifestation typeManifestation,
                                              SyntheticState syntheticState,
                                              String name,
                                              List<? extends FieldDescription> fieldDescriptions,
                                              List<? extends MethodDescription> methodDescriptions) {
        super(fieldDescriptions, methodDescriptions);
        this.classVersion = classVersion;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.visibility = visibility;
        this.typeManifestation = typeManifestation;
        this.syntheticState = syntheticState;
        this.name = name;
    }

    @Override
    public InstrumentedType withField(String name,
                                      TypeDescription fieldType,
                                      int modifiers,
                                      boolean synthetic) {
        FieldDescription additionalField = new FieldToken(name, fieldType, modifiers, synthetic);
        if (fieldDescriptions.contains(additionalField)) {
            throw new IllegalArgumentException("Field " + additionalField + " is already defined on " + this);
        }
        List<FieldDescription> fieldDescriptions = new ArrayList<FieldDescription>(this.fieldDescriptions);
        fieldDescriptions.add(additionalField);
        return new SubclassLoadedTypeInstrumentation(classVersion,
                superClass,
                interfaces,
                visibility,
                typeManifestation,
                syntheticState,
                this.name,
                fieldDescriptions,
                methodDescriptions);
    }

    @Override
    public InstrumentedType withMethod(String internalName,
                                       TypeDescription returnType,
                                       List<? extends TypeDescription> parameterTypes,
                                       int modifiers,
                                       boolean synthetic) {
        MethodDescription additionalMethod = new MethodToken(internalName, returnType, parameterTypes, modifiers, synthetic);
        if (methodDescriptions.contains(additionalMethod)) {
            throw new IllegalArgumentException("Method " + additionalMethod + " is alread defined on " + this);
        }
        List<MethodDescription> methodDescriptions = new ArrayList<MethodDescription>(this.methodDescriptions);
        methodDescriptions.add(additionalMethod);
        return new SubclassLoadedTypeInstrumentation(classVersion,
                superClass,
                interfaces,
                visibility,
                typeManifestation,
                syntheticState,
                name,
                fieldDescriptions,
                methodDescriptions);
    }

    @Override
    public boolean isAssignableFrom(TypeDescription typeDescription) {
        if (typeDescription.getName().equals(getName())) {
            return true;
        }
        for (Class<?> anInterface : interfaces) {
            if (typeDescription.isAssignableTo(anInterface)) {
                return true;
            }
        }
        return typeDescription.isAssignableTo(superClass);
    }

    @Override
    public boolean isAssignableTo(TypeDescription typeDescription) {
        if (typeDescription.getName().equals(getName())) {
            return true;
        }
        for (Class<?> anInterface : interfaces) {
            if (typeDescription.isAssignableFrom(anInterface)) {
                return true;
            }
        }
        return typeDescription.isAssignableFrom(superClass);
    }

    @Override
    public TypeDescription getSupertype() {
        return new ForLoadedType(superClass);
    }

    @Override
    public TypeList getInterfaces() {
        return new TypeList.ForLoadedType(interfaces.toArray(new Class<?>[interfaces.size()]));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getModifiers() {
        return typeManifestation.getMask() | syntheticState.getMask() | visibility.getMask();
    }

    @Override
    public boolean isSynthetic() {
        return syntheticState.isSynthetic();
    }

    @Override
    public Class<?> getSuperClass() {
        return superClass;
    }

    @Override
    public Collection<Class<?>> getDeclaredInterfaces() {
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

    @Override
    public int getClassVersion() {
        return classVersion.getVersionNumber();
    }
}
