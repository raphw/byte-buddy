package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;

import java.util.Collections;
import java.util.List;

import static net.bytebuddy.utility.ByteBuddyCommons.isValidTypeName;
import static net.bytebuddy.utility.ByteBuddyCommons.join;

/**
 * Represents a type instrumentation that creates a new type based on a given superclass.
 */
public class SubclassInstrumentedType extends InstrumentedType.AbstractBase {

    /**
     * The class file version of this type.
     */
    private final ClassFileVersion classFileVersion;

    /**
     * The super class of this type.
     */
    private final GenericTypeDescription superClass;

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
                                    GenericTypeDescription superClass,
                                    List<TypeDescription> interfaces,
                                    int modifiers,
                                    NamingStrategy namingStrategy) {
        this.classFileVersion = classFileVersion;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.modifiers = modifiers;
        this.name = isValidTypeName(namingStrategy.name(new NamingStrategy.UnnamedType.Default(superClass,
                interfaces,
                modifiers,
                classFileVersion)));
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
     * @param typeInitializer       A type initializer to be applied for this instrumentation.
     */
    protected SubclassInstrumentedType(ClassFileVersion classFileVersion,
                                       GenericTypeDescription superClass,
                                       List<TypeDescription> interfaces,
                                       int modifiers,
                                       String name,
                                       List<? extends FieldDescription> fieldDescriptions,
                                       List<? extends MethodDescription> methodDescriptions,
                                       LoadedTypeInitializer loadedTypeInitializer,
                                       TypeInitializer typeInitializer) {
        super(loadedTypeInitializer,
                typeInitializer,
                name,
                fieldDescriptions,
                methodDescriptions);
        this.classFileVersion = classFileVersion;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.modifiers = modifiers;
        this.name = name;
    }

    @Override
    public InstrumentedType withField(String internalName,
                                      GenericTypeDescription fieldType,
                                      int modifiers) {
        FieldDescription additionalField = new FieldDescription.Latent(internalName, this, fieldType, modifiers);
        if (fieldDescriptions.contains(additionalField)) {
            throw new IllegalArgumentException("Field " + additionalField + " is already defined on " + this);
        }
        return new SubclassInstrumentedType(classFileVersion,
                superClass,
                interfaces,
                this.modifiers,
                name,
                join(fieldDescriptions, additionalField),
                methodDescriptions,
                loadedTypeInitializer,
                typeInitializer);
    }

    @Override
    public InstrumentedType withMethod(String internalName,
                                       GenericTypeDescription returnType,
                                       List<? extends GenericTypeDescription> parameterTypes,
                                       List<? extends GenericTypeDescription> exceptionTypes,
                                       int modifiers) {
        MethodDescription additionalMethod = new MethodDescription.Latent(internalName,
                this,
                returnType,
                parameterTypes,
                modifiers,
                exceptionTypes);
        if (methodDescriptions.contains(additionalMethod)) {
            throw new IllegalArgumentException("Method " + additionalMethod + " is already defined on " + this);
        }
        return new SubclassInstrumentedType(classFileVersion,
                superClass,
                interfaces,
                this.modifiers,
                name,
                fieldDescriptions,
                join(methodDescriptions, additionalMethod),
                loadedTypeInitializer,
                typeInitializer);
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
                new LoadedTypeInitializer.Compound(this.loadedTypeInitializer, loadedTypeInitializer),
                typeInitializer);
    }

    @Override
    public InstrumentedType withInitializer(ByteCodeAppender byteCodeAppender) {
        return new SubclassInstrumentedType(classFileVersion,
                superClass,
                interfaces,
                modifiers,
                name,
                fieldDescriptions,
                methodDescriptions,
                loadedTypeInitializer,
                typeInitializer.expandWith(byteCodeAppender));
    }

    @Override
    public TypeDescription detach() {
        return new SubclassInstrumentedType(classFileVersion,
                superClass,
                interfaces,
                modifiers,
                name,
                fieldDescriptions,
                methodDescriptions,
                LoadedTypeInitializer.NoOp.INSTANCE,
                TypeInitializer.None.INSTANCE);
    }

    @Override
    public GenericTypeDescription getSuperTypeGen() {
        return isInterface()
                ? null
                : superClass;
    }

    @Override
    public GenericTypeList getInterfacesGen() {
        return new GenericTypeList.Explicit(interfaces);
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
    public GenericTypeList getTypeVariables() {
        return new GenericTypeList.Empty();
    }

    @Override
    public AnnotationList getDeclaredAnnotations() {
        return new AnnotationList.Empty();
    }

    @Override
    public AnnotationList getInheritedAnnotations() {
        return getSuperType() == null
                ? new AnnotationList.Empty()
                : getSuperType().getInheritedAnnotations().inherited(Collections.<TypeDescription>emptySet());
    }
}
