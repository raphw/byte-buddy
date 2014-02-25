package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass;

import com.blogspot.mydailyjava.bytebuddy.ClassFormatVersion;
import com.blogspot.mydailyjava.bytebuddy.NamingStrategy;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.TypeInitializer;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import com.blogspot.mydailyjava.bytebuddy.modifier.SyntheticState;
import com.blogspot.mydailyjava.bytebuddy.modifier.TypeManifestation;
import com.blogspot.mydailyjava.bytebuddy.modifier.Visibility;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.blogspot.mydailyjava.bytebuddy.utility.UserInput.isValidIdentifier;

public class SubclassTypeInstrumentation
        extends InstrumentedType.AbstractBase
        implements NamingStrategy.UnnamedType {

    private final ClassFormatVersion classFormatVersion;
    private final Class<?> superClass;
    private final Collection<Class<?>> interfaces;
    private final int modifiers;
    private final String name;

    public SubclassTypeInstrumentation(ClassFormatVersion classFormatVersion,
                                       Class<?> superClass,
                                       Collection<Class<?>> interfaces,
                                       int modifiers,
                                       NamingStrategy namingStrategy) {

        this.classFormatVersion = classFormatVersion;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.modifiers = modifiers;
        this.name = isValidIdentifier(namingStrategy.getName(this));
    }

    protected SubclassTypeInstrumentation(ClassFormatVersion classFormatVersion,
                                          Class<?> superClass,
                                          Collection<Class<?>> interfaces,
                                          int modifiers,
                                          String name,
                                          List<? extends FieldDescription> fieldDescriptions,
                                          List<? extends MethodDescription> methodDescriptions,
                                          TypeInitializer typeInitializer) {
        super(typeInitializer, name, fieldDescriptions, methodDescriptions);
        this.classFormatVersion = classFormatVersion;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.modifiers = modifiers;
        this.name = name;
    }

    @Override
    public InstrumentedType withField(String name,
                                      TypeDescription fieldType,
                                      int modifiers) {
        FieldDescription additionalField = new FieldToken(name, fieldType, modifiers);
        if (fieldDescriptions.contains(additionalField)) {
            throw new IllegalArgumentException("Field " + additionalField + " is already defined on " + this);
        }
        List<FieldDescription> fieldDescriptions = new ArrayList<FieldDescription>(this.fieldDescriptions);
        fieldDescriptions.add(additionalField);
        return new SubclassTypeInstrumentation(classFormatVersion,
                superClass,
                interfaces,
                modifiers,
                this.name,
                fieldDescriptions,
                methodDescriptions,
                typeInitializer);
    }

    @Override
    public InstrumentedType withMethod(String internalName,
                                       TypeDescription returnType,
                                       List<? extends TypeDescription> parameterTypes,
                                       int modifiers) {
        MethodDescription additionalMethod = new MethodToken(internalName, returnType, parameterTypes, modifiers);
        if (methodDescriptions.contains(additionalMethod)) {
            throw new IllegalArgumentException("Method " + additionalMethod + " is already defined on " + this);
        }
        List<MethodDescription> methodDescriptions = new ArrayList<MethodDescription>(this.methodDescriptions);
        methodDescriptions.add(additionalMethod);
        return new SubclassTypeInstrumentation(classFormatVersion,
                superClass,
                interfaces,
                modifiers,
                name,
                fieldDescriptions,
                methodDescriptions,
                typeInitializer);
    }

    @Override
    public InstrumentedType withInitializer(TypeInitializer typeInitializer) {
        return new SubclassTypeInstrumentation(classFormatVersion,
                superClass,
                interfaces,
                modifiers,
                name,
                fieldDescriptions,
                methodDescriptions,
                new TypeInitializer.Compound(this.typeInitializer, typeInitializer));
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
        return modifiers;
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
        if ((modifiers & Modifier.PUBLIC) != 0) {
            return Visibility.PUBLIC;
        } else if ((modifiers & Modifier.PROTECTED) != 0) {
            return Visibility.PROTECTED;
        } else if ((modifiers & Modifier.PRIVATE) != 0) {
            return Visibility.PROTECTED;
        } else {
            return Visibility.PACKAGE_PRIVATE;
        }
    }

    @Override
    public TypeManifestation getTypeManifestation() {
        if ((modifiers & Modifier.FINAL) != 0) {
            return TypeManifestation.FINAL;
            /* Note: Interfaces are abstract, the interface condition needs to be checked before abstraction. */
        } else if ((modifiers & Opcodes.ACC_INTERFACE) != 0) {
            return TypeManifestation.INTERFACE;
        } else if ((modifiers & Opcodes.ACC_ABSTRACT) != 0) {
            return TypeManifestation.ABSTRACT;
        } else {
            return TypeManifestation.PLAIN;
        }
    }

    @Override
    public SyntheticState getSyntheticState() {
        return SyntheticState.is(isSynthetic());
    }

    @Override
    public int getClassFormatVersion() {
        return classFormatVersion.getVersionNumber();
    }
}
