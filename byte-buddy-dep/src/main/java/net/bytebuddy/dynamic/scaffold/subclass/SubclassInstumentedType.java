package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ClassFormatVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.instrumentation.TypeInitializer;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.modifier.MemberVisibility;
import net.bytebuddy.modifier.SyntheticState;
import net.bytebuddy.modifier.TypeManifestation;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.utility.ByteBuddyCommons.isValidTypeName;

/**
 * Represents a type instrumentation that creates a new type based on a loaded superclass.
 */
public class SubclassInstumentedType
        extends InstrumentedType.AbstractBase
        implements NamingStrategy.UnnamedType {

    private final ClassFormatVersion classFormatVersion;
    private final TypeDescription superClass;
    private final List<TypeDescription> interfaces;
    private final int modifiers;
    private final String name;

    /**
     * Creates a new immutable type instrumentation for a loaded superclass.
     *
     * @param classFormatVersion The class format version of this instrumentation.
     * @param superClass         The superclass of this instrumentation.
     * @param interfaces         A collection of loaded interfaces that are implemented by this instrumented class.
     * @param modifiers          The modifiers for this instrumentation.
     * @param namingStrategy     The naming strategy to be applied for this instrumentation.
     */
    public SubclassInstumentedType(ClassFormatVersion classFormatVersion,
                                   TypeDescription superClass,
                                   List<TypeDescription> interfaces,
                                   int modifiers,
                                   NamingStrategy namingStrategy) {
        this.classFormatVersion = classFormatVersion;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.modifiers = modifiers;
        this.name = isValidTypeName(namingStrategy.getName(this));
    }

    /**
     * Creates a new immutable type instrumentation for a loaded superclass.
     *
     * @param classFormatVersion The class format version of this instrumentation.
     * @param superClass         The superclass of this instrumentation.
     * @param interfaces         A collection of loaded interfaces that are implemented by this instrumented class.
     * @param modifiers          The modifiers for this instrumentation.
     * @param name               The name of this instrumented type.
     * @param fieldDescriptions  A list of field descriptions to be applied for this instrumentation.
     * @param methodDescriptions A list of method descriptions to be applied for this instrumentation.
     * @param typeInitializer    A type initializer to be applied for this instrumentation.
     */
    protected SubclassInstumentedType(ClassFormatVersion classFormatVersion,
                                      TypeDescription superClass,
                                      List<TypeDescription> interfaces,
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
        return new SubclassInstumentedType(classFormatVersion,
                superClass,
                interfaces,
                this.modifiers,
                this.name,
                fieldDescriptions,
                methodDescriptions,
                typeInitializer);
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
        return new SubclassInstumentedType(classFormatVersion,
                superClass,
                interfaces,
                this.modifiers,
                name,
                fieldDescriptions,
                methodDescriptions,
                typeInitializer);
    }

    @Override
    public InstrumentedType withInitializer(TypeInitializer typeInitializer) {
        return new SubclassInstumentedType(classFormatVersion,
                superClass,
                interfaces,
                modifiers,
                name,
                fieldDescriptions,
                methodDescriptions,
                new TypeInitializer.Compound(this.typeInitializer, typeInitializer));
    }

    @Override
    public TypeDescription detach() {
        return new SubclassInstumentedType(classFormatVersion,
                superClass,
                interfaces,
                modifiers,
                name, fieldDescriptions,
                methodDescriptions,
                TypeInitializer.NoOp.INSTANCE);
    }

    @Override
    public TypeDescription getSupertype() {
        return superClass;
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
    public MemberVisibility getVisibility() {
        if ((modifiers & Modifier.PUBLIC) != 0) {
            return MemberVisibility.PUBLIC;
        } else if ((modifiers & Modifier.PROTECTED) != 0) {
            return MemberVisibility.PROTECTED;
        } else if ((modifiers & Modifier.PRIVATE) != 0) {
            return MemberVisibility.PROTECTED;
        } else {
            return MemberVisibility.PACKAGE_PRIVATE;
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
    public ClassFormatVersion getClassFormatVersion() {
        return classFormatVersion;
    }

    @Override
    public String toString() {
        return "SubclassInstumentedType{" +
                "classFormatVersion=" + classFormatVersion +
                ", superClass=" + superClass +
                ", interfaces=" + interfaces +
                ", modifiers=" + modifiers +
                ", name='" + name + '\'' +
                '}';
    }
}
