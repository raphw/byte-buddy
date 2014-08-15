package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.instrumentation.LoadedTypeInitializer;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.bytebuddy.utility.ByteBuddyCommons.isValidTypeName;

/**
 * An instrumented type which enhances a given type description by an extending redefinition.
 */
public class FlatInstrumentedType extends InstrumentedType.AbstractBase {

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
     * Creates a new flat instrumented type.
     *
     * @param classFileVersion The class file version for the given type.
     * @param levelType        The name of the instrumented type.
     * @param interfaces       The additional interfaces that this type should implement.
     * @param modifiers        The name of the instrumented type.
     * @param namingStrategy   The naming strategy to apply for the given type.
     */
    public FlatInstrumentedType(ClassFileVersion classFileVersion,
                                TypeDescription levelType,
                                List<TypeDescription> interfaces,
                                int modifiers,
                                NamingStrategy namingStrategy) {
        super(LoadedTypeInitializer.NoOp.INSTANCE,
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
     * Creates a new flat instrumented type.
     *
     * @param levelType         The name of the instrumented type.
     * @param name              The name of the instrumented type.
     * @param interfaces        The additional interfaces that this type should implement.
     * @param modifiers         The name of the instrumented type.
     * @param fieldDescriptions     A list of field descriptions for this instrumented type.
     * @param methodDescriptions    A list of method descriptions for this instrumented type.
     * @param loadedTypeInitializer A loaded type initializer for this instrumented type.
     */
    protected FlatInstrumentedType(TypeDescription levelType,
                                   String name,
                                   List<TypeDescription> interfaces,
                                   int modifiers,
                                   List<? extends FieldDescription> fieldDescriptions,
                                   List<? extends MethodDescription> methodDescriptions,
                                   LoadedTypeInitializer loadedTypeInitializer) {
        super(loadedTypeInitializer, name, fieldDescriptions, methodDescriptions);
        this.levelType = levelType;
        this.name = name;
        this.modifiers = modifiers;
        this.interfaces = interfaces;
    }

    @Override
    public InstrumentedType withField(String internalName,
                                      TypeDescription fieldType,
                                      int modifiers) {
        FieldDescription additionalField = new FieldToken(internalName, fieldType, modifiers);
        if (fieldDescriptions.contains(additionalField)) {
            throw new IllegalArgumentException("Field " + additionalField + " is already defined on " + this);
        }
        List<FieldDescription> fieldDescriptions = new ArrayList<FieldDescription>(this.fieldDescriptions);
        fieldDescriptions.add(additionalField);
        return new FlatInstrumentedType(levelType,
                name,
                interfaces,
                this.modifiers,
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
        return new FlatInstrumentedType(levelType,
                name,
                interfaces,
                this.modifiers,
                fieldDescriptions,
                methodDescriptions,
                loadedTypeInitializer);
    }

    @Override
    public InstrumentedType withInitializer(LoadedTypeInitializer loadedTypeInitializer) {
        return new FlatInstrumentedType(levelType,
                name,
                interfaces,
                modifiers,
                fieldDescriptions,
                methodDescriptions,
                new LoadedTypeInitializer.Compound(this.loadedTypeInitializer, loadedTypeInitializer));
    }

    @Override
    public TypeDescription detach() {
        return new FlatInstrumentedType(levelType,
                name,
                interfaces,
                modifiers,
                fieldDescriptions,
                methodDescriptions,
                LoadedTypeInitializer.NoOp.INSTANCE);
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
    public boolean isSealed() {
        return levelType.isSealed();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getModifiers() {
        return modifiers;
    }
}
