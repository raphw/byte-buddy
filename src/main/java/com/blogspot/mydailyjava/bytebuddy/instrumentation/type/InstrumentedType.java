package com.blogspot.mydailyjava.bytebuddy.instrumentation.type;

import com.blogspot.mydailyjava.bytebuddy.*;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodList;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;

import java.lang.annotation.Annotation;
import java.util.Collection;

public class InstrumentedType extends TypeDescription.AbstractTypeDescription implements TypeDescription, NamingStrategy.UnnamedType {

    private final ClassVersion classVersion;

    private final Class<?> superClass;
    private final Collection<Class<?>> interfaces;

    private final Visibility visibility;
    private final TypeManifestation typeManifestation;
    private final SyntheticState syntheticState;

    private final String name;

    public InstrumentedType(ClassVersion classVersion,
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
    public boolean isInstance(Object object) {
        return isAssignableFrom(object.getClass());
    }

    @Override
    public boolean isAssignableFrom(Class<?> type) {
        return isAssignableFrom(new ForLoadedType(type));
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
    public boolean isAssignableTo(Class<?> type) {
        return isAssignableTo(new ForLoadedType(type));
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
    public boolean represents(Class<?> type) {
        return type.getName().equals(getName());
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public TypeDescription getComponentType() {
        return null;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isAnnotation() {
        return false;
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
    public TypeDescription getDeclaringClass() {
        return null;
    }

    @Override
    public MethodDescription getEnclosingMethod() {
        return null;
    }

    @Override
    public TypeDescription getEnclosingClass() {
        return null;
    }

    @Override
    public String getSimpleName() {
        return getName().substring(getPackageName().length(), getName().length());
    }

    @Override
    public String getCanonicalName() {
        return getName();
    }

    @Override
    public boolean isAnonymousClass() {
        return false;
    }

    @Override
    public boolean isLocalClass() {
        return false;
    }

    @Override
    public boolean isMemberClass() {
        return false;
    }

    @Override
    public MethodList getDeclaredMethods() {
        return new MethodList.Empty();
    }

    @Override
    public String getPackageName() {
        int packageIndex = getName().lastIndexOf('.');
        if (packageIndex == -1) {
            return "";
        } else {
            return getName().substring(0, packageIndex);
        }
    }

    @Override
    public TypeSize getStackSize() {
        return TypeSize.SINGLE;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return false;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return null;
    }

    @Override
    public Annotation[] getAnnotations() {
        return new Annotation[0];
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return new Annotation[0];
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescriptor() {
        return "L" + getName() + ";";
    }

    @Override
    public int getModifiers() {
        return typeManifestation.getMask() | syntheticState.getMask() | visibility.getMask();
    }

    @Override
    public boolean isSynthetic() {
        return true;
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

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && name.equals(((InstrumentedType) other).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "InstrumentedType{" + name + "}";
    }
}
