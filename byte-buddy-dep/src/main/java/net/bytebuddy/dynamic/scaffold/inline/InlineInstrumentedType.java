package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.bytebuddy.utility.ByteBuddyCommons.isValidTypeName;

/**
 * An instrumented type which enhances a given type description by an extending redefinition.
 */
public class InlineInstrumentedType extends InstrumentedType.AbstractBase {

    /**
     * The type which is the base for this instrumented type.
     */
    private final TypeDescription levelType;

    /**
     * The name of the instrumented type.
     */
    private final String name;

    /**
     * The modifiers of the instrumented type.
     */
    private final int modifiers;

    /**
     * The additional interfaces that this type should implement.
     */
    private final List<TypeDescription> interfaces;

    /**
     * Creates a new inlined instrumented type.
     *
     * @param classFileVersion The class file version for the given type.
     * @param levelType        The name of the instrumented type.
     * @param interfaces       The additional interfaces that this type should implement.
     * @param modifiers        The name of the instrumented type.
     * @param namingStrategy   The naming strategy to apply for the given type.
     */
    public InlineInstrumentedType(ClassFileVersion classFileVersion,
                                  TypeDescription levelType,
                                  List<TypeDescription> interfaces,
                                  int modifiers,
                                  NamingStrategy namingStrategy) {
        super(LoadedTypeInitializer.NoOp.INSTANCE,
                TypeInitializer.None.INSTANCE,
                levelType.getName(),
                levelType.getDeclaredFields(),
                levelType.getDeclaredMethods());
        this.levelType = levelType;
        this.modifiers = modifiers;
        Set<TypeDescription> interfaceTypes = new HashSet<TypeDescription>(levelType.getInterfaces());
        interfaceTypes.addAll(interfaces);
        this.interfaces = new ArrayList<TypeDescription>(interfaceTypes);
        this.name = isValidTypeName(namingStrategy.name(new NamingStrategy.UnnamedType.Default(levelType.getSupertype(),
                interfaces,
                modifiers,
                classFileVersion)));
    }

    /**
     * Creates a new inlined instrumented type.
     *
     * @param levelType             The name of the instrumented type.
     * @param name                  The name of the instrumented type.
     * @param interfaces            The additional interfaces that this type should implement.
     * @param modifiers             The name of the instrumented type.
     * @param fieldDescriptions     A list of field descriptions for this instrumented type.
     * @param methodDescriptions    A list of method descriptions for this instrumented type.
     * @param loadedTypeInitializer A loaded type initializer for this instrumented type.
     * @param typeInitializer       A type initializer for this instrumented type.
     */
    protected InlineInstrumentedType(TypeDescription levelType,
                                     String name,
                                     List<TypeDescription> interfaces,
                                     int modifiers,
                                     List<? extends FieldDescription> fieldDescriptions,
                                     List<? extends MethodDescription> methodDescriptions,
                                     LoadedTypeInitializer loadedTypeInitializer,
                                     TypeInitializer typeInitializer) {
        super(loadedTypeInitializer,
                typeInitializer,
                name,
                fieldDescriptions,
                methodDescriptions);
        this.levelType = levelType;
        this.name = name;
        this.modifiers = modifiers;
        this.interfaces = interfaces;
    }

    @Override
    public InstrumentedType withField(String internalName,
                                      TypeDescription fieldType,
                                      int modifiers) {
        FieldDescription additionalField = new FieldDescription.Latent(internalName, this, fieldType, modifiers);
        if (fieldDescriptions.contains(additionalField)) {
            throw new IllegalArgumentException("Field " + additionalField + " is already defined on " + this);
        }
        List<FieldDescription> fieldDescriptions = new ArrayList<FieldDescription>(this.fieldDescriptions);
        fieldDescriptions.add(additionalField);
        return new InlineInstrumentedType(levelType,
                name,
                interfaces,
                this.modifiers,
                fieldDescriptions,
                methodDescriptions,
                loadedTypeInitializer,
                typeInitializer);
    }

    @Override
    public InstrumentedType withMethod(String internalName,
                                       TypeDescription returnType,
                                       List<? extends TypeDescription> parameterTypes,
                                       List<? extends TypeDescription> exceptionTypes,
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
        List<MethodDescription> methodDescriptions = new ArrayList<MethodDescription>(this.methodDescriptions);
        methodDescriptions.add(additionalMethod);
        return new InlineInstrumentedType(levelType,
                name,
                interfaces,
                this.modifiers,
                fieldDescriptions,
                methodDescriptions,
                loadedTypeInitializer,
                typeInitializer);
    }

    @Override
    public InstrumentedType withInitializer(LoadedTypeInitializer loadedTypeInitializer) {
        return new InlineInstrumentedType(levelType,
                name,
                interfaces,
                modifiers,
                fieldDescriptions,
                methodDescriptions,
                new LoadedTypeInitializer.Compound(this.loadedTypeInitializer, loadedTypeInitializer),
                typeInitializer);
    }

    @Override
    public InstrumentedType withInitializer(ByteCodeAppender byteCodeAppender) {
        return new InlineInstrumentedType(levelType,
                name,
                interfaces,
                modifiers,
                fieldDescriptions,
                methodDescriptions,
                loadedTypeInitializer,
                typeInitializer.expandWith(byteCodeAppender));
    }

    @Override
    public TypeDescription detach() {
        return new InlineInstrumentedType(levelType,
                name,
                interfaces,
                modifiers,
                fieldDescriptions,
                methodDescriptions,
                LoadedTypeInitializer.NoOp.INSTANCE,
                TypeInitializer.None.INSTANCE);
    }

    @Override
    public TypeDescription getSupertype() {
        return levelType.getSupertype();
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
    public AnnotationList getDeclaredAnnotations() {
        return levelType.getDeclaredAnnotations();
    }

    @Override
    public AnnotationList getInheritedAnnotations() {
        return levelType.getInheritedAnnotations();
    }
}
