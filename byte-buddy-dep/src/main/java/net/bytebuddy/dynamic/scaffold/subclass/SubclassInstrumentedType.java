package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.instrumentation.LoadedTypeInitializer;
import net.bytebuddy.instrumentation.ModifierContributor;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.modifier.SyntheticState;
import net.bytebuddy.modifier.TypeManifestation;
import net.bytebuddy.modifier.Visibility;
import net.bytebuddy.utility.ByteBuddyCommons;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.utility.ByteBuddyCommons.isValidTypeName;

/**
 * Represents a type instrumentation that creates a new type based on a given superclass.
 */
public class SubclassInstrumentedType
        extends InstrumentedType.AbstractBase
        implements NamingStrategy.UnnamedType {

    /**
     * The class file version of this type.
     */
    private final ClassFileVersion classFileVersion;

    /**
     * The super class of this type.
     */
    private final TypeDescription superClass;

    /**
     * The interfaces that are represented by this type.
     */
    private final List<TypeDescription> interfaces;

    /**
     * The modifiers of this type.
     */
    private final int modifiers;

    /**
     * The non-internal name of this type.
     */
    private final String name;

    /**
     * Creates a new immutable type instrumentation for a loaded superclass.
     *
     * @param classFileVersion The class file version of this instrumentation.
     * @param superClass       The superclass of this instrumentation.
     * @param interfaces       A collection of loaded interfaces that are implemented by this instrumented class.
     * @param modifiers        The modifiers for this instrumentation.
     * @param namingStrategy   The naming strategy to be applied for this instrumentation.
     */
    public SubclassInstrumentedType(ClassFileVersion classFileVersion,
                                    TypeDescription superClass,
                                    List<TypeDescription> interfaces,
                                    int modifiers,
                                    NamingStrategy namingStrategy) {
        this.classFileVersion = classFileVersion;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.modifiers = modifiers;
        this.name = isValidTypeName(namingStrategy.name(this));
    }

    /**
     * Creates a new immutable type instrumentation for a loaded superclass.
     *
     * @param classFileVersion      The class file version of this instrumentation.
     * @param superClass            The superclass of this instrumentation.
     * @param interfaces            A collection of loaded interfaces that are implemented by this instrumented class.
     * @param modifiers             The modifiers for this instrumentation.
     * @param name                  The name of this instrumented type.
     * @param fieldDescriptions     A list of field descriptions to be applied for this instrumentation.
     * @param methodDescriptions    A list of method descriptions to be applied for this instrumentation.
     * @param loadedTypeInitializer A loaded type initializer to be applied for this instrumentation.
     */
    protected SubclassInstrumentedType(ClassFileVersion classFileVersion,
                                       TypeDescription superClass,
                                       List<TypeDescription> interfaces,
                                       int modifiers,
                                       String name,
                                       List<? extends FieldDescription> fieldDescriptions,
                                       List<? extends MethodDescription> methodDescriptions,
                                       LoadedTypeInitializer loadedTypeInitializer) {
        super(loadedTypeInitializer, name, fieldDescriptions, methodDescriptions);
        this.classFileVersion = classFileVersion;
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
        return new SubclassInstrumentedType(classFileVersion,
                superClass,
                interfaces,
                this.modifiers,
                this.name,
                fieldDescriptions,
                methodDescriptions,
                loadedTypeInitializer);
    }

    @Override
    public InstrumentedType withMethod(String internalName,
                                       TypeDescription returnType,
                                       List<? extends TypeDescription> parameterTypes,
                                       List<? extends TypeDescription> exceptionTypes,
                                       int modifiers) {
        MethodDescription additionalMethod = new MethodToken(internalName,
                returnType,
                parameterTypes,
                exceptionTypes,
                modifiers);
        if (methodDescriptions.contains(additionalMethod)) {
            throw new IllegalArgumentException("Method " + additionalMethod + " is already defined on " + this);
        }
        List<MethodDescription> methodDescriptions = new ArrayList<MethodDescription>(this.methodDescriptions);
        methodDescriptions.add(additionalMethod);
        return new SubclassInstrumentedType(classFileVersion,
                superClass,
                interfaces,
                this.modifiers,
                name,
                fieldDescriptions,
                methodDescriptions,
                loadedTypeInitializer);
    }

    @Override
    public InstrumentedType withInitializer(LoadedTypeInitializer loadedTypeInitializer) {
        return new SubclassInstrumentedType(classFileVersion,
                superClass,
                interfaces,
                modifiers,
                name,
                fieldDescriptions,
                methodDescriptions,
                new LoadedTypeInitializer.Compound(this.loadedTypeInitializer, loadedTypeInitializer));
    }

    @Override
    public TypeDescription detach() {
        return new SubclassInstrumentedType(classFileVersion,
                superClass,
                interfaces,
                modifiers,
                name, fieldDescriptions,
                methodDescriptions,
                LoadedTypeInitializer.NoOp.INSTANCE);
    }

    @Override
    public TypeDescription getSupertype() {
        return isInterface() ? null : superClass;
    }

    @Override
    public TypeList getInterfaces() {
        return new TypeList.Explicit(interfaces);
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
    public TypeDescription getSuperClass() {
        return superClass;
    }

    @Override
    public List<TypeDescription> getDeclaredInterfaces() {
        return interfaces;
    }

    @Override
    public Visibility getVisibility() {
        switch (modifiers & ByteBuddyCommons.VISIBILITY_MODIFIER_MASK) {
            case Opcodes.ACC_PUBLIC:
                return Visibility.PUBLIC;
            case Opcodes.ACC_PROTECTED:
                return Visibility.PROTECTED;
            case Opcodes.ACC_PRIVATE:
                return Visibility.PRIVATE;
            case ModifierContributor.EMPTY_MASK:
                return Visibility.PACKAGE_PRIVATE;
            default:
                throw new IllegalStateException("Ambiguous modifier: " + modifiers);
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
    public ClassFileVersion getClassFileVersion() {
        return classFileVersion;
    }

    @Override
    public boolean isSealed() {
        return false;
    }

    @Override
    public String toString() {
        return "SubclassInstrumentedType{" +
                "classFileVersion=" + classFileVersion +
                ", superClass=" + superClass +
                ", interfaces=" + interfaces +
                ", modifiers=" + modifiers +
                ", name='" + name + '\'' +
                '}';
    }
}
