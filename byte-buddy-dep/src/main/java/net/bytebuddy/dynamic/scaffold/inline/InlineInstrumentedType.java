package net.bytebuddy.dynamic.scaffold.inline;

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.utility.ByteBuddyCommons.isValidTypeName;
import static net.bytebuddy.utility.ByteBuddyCommons.join;

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
    private final List<GenericTypeDescription> interfaces;

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
                                  List<? extends GenericTypeDescription> interfaces,
                                  int modifiers,
                                  NamingStrategy namingStrategy) {
        super(LoadedTypeInitializer.NoOp.INSTANCE,
                TypeInitializer.None.INSTANCE,
                named(levelType.getSourceCodeName()),
                levelType.getTypeVariables(),
                levelType.getDeclaredFields(),
                levelType.getDeclaredMethods());
        this.levelType = levelType;
        this.modifiers = modifiers;
        Set<GenericTypeDescription> interfaceTypes = new HashSet<GenericTypeDescription>(levelType.getInterfacesGen());
        interfaceTypes.addAll(interfaces);
        this.interfaces = new ArrayList<GenericTypeDescription>(interfaceTypes);
        this.name = isValidTypeName(namingStrategy.name(new NamingStrategy.UnnamedType.Default(levelType.getSuperTypeGen(),
                interfaces,
                modifiers,
                classFileVersion)));
    }

    protected InlineInstrumentedType(TypeDescription levelType,
                                     String name,
                                     List<GenericTypeDescription> interfaces,
                                     int modifiers,
                                     List<? extends GenericTypeDescription> typeVariables,
                                     List<? extends FieldDescription> fieldDescriptions,
                                     List<? extends MethodDescription> methodDescriptions,
                                     LoadedTypeInitializer loadedTypeInitializer,
                                     TypeInitializer typeInitializer) {
        super(loadedTypeInitializer,
                typeInitializer,
                named(name),
                typeVariables,
                fieldDescriptions,
                methodDescriptions);
        this.levelType = levelType;
        this.name = name;
        this.modifiers = modifiers;
        this.interfaces = interfaces;
    }

    @Override
    public InstrumentedType withField(String internalName,
                                      GenericTypeDescription fieldType,
                                      int modifiers) {
        FieldDescription additionalField = new FieldDescription.Latent(internalName, this, fieldType, modifiers);
        if (fieldDescriptions.contains(additionalField)) {
            throw new IllegalArgumentException("Field " + additionalField + " is already defined on " + this);
        }
        return new InlineInstrumentedType(levelType,
                name,
                interfaces,
                this.modifiers,
                typeVariables,
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
        return new InlineInstrumentedType(levelType,
                name,
                interfaces,
                this.modifiers,
                typeVariables,
                fieldDescriptions,
                join(methodDescriptions, additionalMethod),
                loadedTypeInitializer,
                typeInitializer);
    }

    @Override
    public InstrumentedType withInitializer(LoadedTypeInitializer loadedTypeInitializer) {
        return new InlineInstrumentedType(levelType,
                name,
                interfaces,
                modifiers,
                typeVariables,
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
                typeVariables,
                fieldDescriptions,
                methodDescriptions,
                loadedTypeInitializer,
                typeInitializer.expandWith(byteCodeAppender));
    }

    @Override
    public GenericTypeDescription getSuperTypeGen() {
        return levelType.getSuperTypeGen();
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
    public AnnotationList getDeclaredAnnotations() {
        return levelType.getDeclaredAnnotations();
    }

    @Override
    public AnnotationList getInheritedAnnotations() {
        return levelType.getInheritedAnnotations();
    }
}
