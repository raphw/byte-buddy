package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.inline.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.inline.MethodFlatteningResolver;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.LoadedTypeInitializer;
import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static net.bytebuddy.utility.ByteBuddyCommons.join;

/**
 * A type writer allows an easier creation of a dynamic type by enforcing the writing order
 * (type, annotations, fields, methods) that is required by ASM in order to successfully creating a Java type.
 * <p>&nbsp;</p>
 * Note: This type represents a mutable data structure since it is a wrapper around an ASM
 * {@link org.objectweb.asm.ClassWriter}. Once a phase of this type writer is left the old instances must not longer
 * be used.
 *
 * @param <T> The best known loaded type for the dynamically created type.
 */
public interface TypeWriter<T> {

    DynamicType.Unloaded<T> make(Instrumentation.Context.ExtractableView instrumentationContext);

    static class Default<S> implements TypeWriter<S> {

        private final TypeDescription typeDescription;

        private final LoadedTypeInitializer loadedTypeInitializer;

        private final List<DynamicType> explicitAuxiliaryTypes;

        private final Engine engine;

        public Default(TypeDescription typeDescription,
                       LoadedTypeInitializer loadedTypeInitializer,
                       List<DynamicType> explicitAuxiliaryTypes,
                       Engine engine) {
            this.typeDescription = typeDescription;
            this.loadedTypeInitializer = loadedTypeInitializer;
            this.explicitAuxiliaryTypes = explicitAuxiliaryTypes;
            this.engine = engine;
        }

        @Override
        public DynamicType.Unloaded<S> make(Instrumentation.Context.ExtractableView instrumentationContext) {
            return new DynamicType.Default.Unloaded<S>(typeDescription,
                    engine.create(instrumentationContext),
                    loadedTypeInitializer,
                    join(explicitAuxiliaryTypes, instrumentationContext.getRegisteredAuxiliaryTypes()));
        }
    }

    static interface Engine {

        static final int ASM_MANUAL_FLAG = 0;

        static final int ASM_API_VERSION = Opcodes.ASM5;

        byte[] create(Instrumentation.Context.ExtractableView instrumentationContext);

        static class ForRedefinition implements Engine {

            public static interface InputStreamProvider {

                static class ForClassFileLocator implements InputStreamProvider {

                    private final TypeDescription originalType;

                    private final ClassFileLocator classFileLocator;

                    public ForClassFileLocator(TypeDescription originalType, ClassFileLocator classFileLocator) {
                        this.originalType = originalType;
                        this.classFileLocator = classFileLocator;
                    }

                    @Override
                    public InputStream create() {
                        return classFileLocator.classFileFor(originalType);
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && classFileLocator.equals(((ForClassFileLocator) other).classFileLocator)
                                && originalType.equals(((ForClassFileLocator) other).originalType);
                    }

                    @Override
                    public int hashCode() {
                        int result = originalType.hashCode();
                        result = 31 * result + classFileLocator.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "Engine.ForRedefinition.InputStreamProvider.ForClassFileLocator{" +
                                "originalType=" + originalType +
                                ", classFileLocator=" + classFileLocator +
                                '}';
                    }
                }

                InputStream create();
            }

            private final TypeDescription instrumentedType;
            private final ClassFileVersion classFileVersion;
            private final List<? extends MethodDescription> invokableMethods;
            private final ClassVisitorWrapper classVisitorWrapper;
            private final TypeAttributeAppender attributeAppender;
            private final TypeWriter.FieldPool fieldPool;
            private final TypeWriter.MethodPool methodPool;
            private final InputStreamProvider inputStreamProvider;
            private final MethodFlatteningResolver methodFlatteningResolver;

            public ForRedefinition(TypeDescription instrumentedType,
                                   ClassFileVersion classFileVersion,
                                   List<? extends MethodDescription> invokableMethods,
                                   ClassVisitorWrapper classVisitorWrapper,
                                   TypeAttributeAppender attributeAppender,
                                   TypeWriter.FieldPool fieldPool,
                                   TypeWriter.MethodPool methodPool,
                                   InputStreamProvider inputStreamProvider,
                                   MethodFlatteningResolver methodFlatteningResolver) {
                this.instrumentedType = instrumentedType;
                this.classFileVersion = classFileVersion;
                this.invokableMethods = invokableMethods;
                this.classVisitorWrapper = classVisitorWrapper;
                this.attributeAppender = attributeAppender;
                this.fieldPool = fieldPool;
                this.methodPool = methodPool;
                this.inputStreamProvider = inputStreamProvider;
                this.methodFlatteningResolver = methodFlatteningResolver;
            }

            @Override
            public byte[] create(Instrumentation.Context.ExtractableView instrumentationContext) {
                InputStream classFile = inputStreamProvider.create();
                try {
                    try {
                        return doCreate(instrumentationContext, classFile);
                    } finally {
                        classFile.close();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            private byte[] doCreate(Instrumentation.Context.ExtractableView instrumentationContext,
                                    InputStream classFile) throws IOException {
                ClassReader classReader = new ClassReader(classFile);
                ClassWriter classWriter = new ClassWriter(classReader, ASM_MANUAL_FLAG);
                classReader.accept(new RedefinitionClassVisitor(classVisitorWrapper.wrap(classWriter), instrumentationContext), ASM_MANUAL_FLAG);
                return classWriter.toByteArray();
            }

            protected class RedefinitionClassVisitor extends ClassVisitor {

                private final Instrumentation.Context.ExtractableView instrumentationContext;

                private final Map<String, FieldDescription> declaredFields;
                private final Map<String, MethodDescription> declarableMethods;

                private Instrumentation.Context.ExtractableView.InjectedCode injectedCode;

                public RedefinitionClassVisitor(ClassVisitor classVisitor,
                                                Instrumentation.Context.ExtractableView instrumentationContext) {
                    super(ASM_API_VERSION, classVisitor);
                    this.instrumentationContext = instrumentationContext;
                    List<? extends FieldDescription> fieldDescriptions = instrumentedType.getDeclaredFields();
                    declaredFields = new HashMap<String, FieldDescription>(fieldDescriptions.size());
                    for (FieldDescription fieldDescription : fieldDescriptions) {
                        declaredFields.put(fieldDescription.getInternalName(), fieldDescription);
                    }
                    declarableMethods = new HashMap<String, MethodDescription>(invokableMethods.size());
                    for (MethodDescription methodDescription : invokableMethods) {
                        declarableMethods.put(methodDescription.getUniqueSignature(), methodDescription);
                    }
                    injectedCode = Instrumentation.Context.ExtractableView.InjectedCode.None.INSTANCE;
                }

                @Override
                public void visit(int classFileVersionNumber,
                                  int modifiers,
                                  String internalName,
                                  String genericSignature,
                                  String superTypeInternalName,
                                  String[] interfaceTypeInternalName) {
                    super.visit(classFileVersion.getVersionNumber(), // TODO: Make optional override
                            instrumentedType.getActualModifiers(),
                            instrumentedType.getInternalName(),
                            instrumentedType.getGenericSignature(),
                            instrumentedType.getSupertype() == null ? null : instrumentedType.getSupertype().getInternalName(),
                            instrumentedType.getInterfaces().toInternalNames());
                    attributeAppender.apply(this, instrumentedType);
                }

                @Override
                public FieldVisitor visitField(int modifiers,
                                               String internalName,
                                               String descriptor,
                                               String genericSignature,
                                               Object defaultValue) {
                    declaredFields.remove(internalName); // Ignore in favor of the class file definition.
                    return super.visitField(modifiers, internalName, descriptor, genericSignature, defaultValue);
                }

                @Override
                public MethodVisitor visitMethod(int modifiers,
                                                 String internalName,
                                                 String descriptor,
                                                 String genericSignature,
                                                 String[] exceptionTypeInternalName) {
                    if (internalName.equals(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME)) {
                        TypeInitializerInjection injectedCode = new TypeInitializerInjection();
                        this.injectedCode = injectedCode;
                        return super.visitMethod(injectedCode.getInjectorProxyMethod().getModifiers(),
                                injectedCode.getInjectorProxyMethod().getInternalName(),
                                injectedCode.getInjectorProxyMethod().getDescriptor(),
                                injectedCode.getInjectorProxyMethod().getGenericSignature(),
                                injectedCode.getInjectorProxyMethod().getExceptionTypes().toInternalNames());
                    }
                    MethodDescription methodDescription = declarableMethods.remove(internalName + descriptor);
                    return methodDescription == null
                            ? super.visitMethod(modifiers, internalName, descriptor, genericSignature, exceptionTypeInternalName)
                            : redefine(methodDescription, (modifiers & Opcodes.ACC_ABSTRACT) != 0);
                }

                private MethodVisitor redefine(MethodDescription methodDescription, boolean nonAbstractOrigin) {
                    TypeWriter.MethodPool.Entry entry = methodPool.target(methodDescription);
                    MethodVisitor methodVisitor = super.visitMethod(methodDescription.getAdjustedModifiers(entry.isDefineMethod()
                                    && entry.getByteCodeAppender().appendsCode()),
                            methodDescription.getInternalName(),
                            methodDescription.getDescriptor(),
                            methodDescription.getGenericSignature(),
                            methodDescription.getExceptionTypes().toInternalNames());
                    entry.getAttributeAppender().apply(methodVisitor, methodDescription);
                    MethodFlatteningResolver.Resolution resolution = methodFlatteningResolver.resolve(methodDescription);
                    return nonAbstractOrigin && resolution.isRedefined()
                            ? new CodePreservingMethodVisitor(methodVisitor, entry.getByteCodeAppender(), methodDescription, this, resolution.getResolvedMethod())
                            : new AttributeObtainingMethodVisitor(methodVisitor, entry.getByteCodeAppender(), methodDescription);
                }

                @Override
                public void visitEnd() {
                    for (FieldDescription fieldDescription : declaredFields.values()) {
                        fieldPool.target(fieldDescription).apply(cv, fieldDescription);
                    }
                    for (MethodDescription methodDescription : declarableMethods.values()) {
                        methodPool.target(methodDescription).apply(cv, instrumentationContext, methodDescription);
                    }
                    instrumentationContext.drain(cv, methodPool, injectedCode);
                    super.visitEnd();
                }

                private class CodePreservingMethodVisitor extends MethodVisitor {

                    private final MethodVisitor actualMethodVisitor;

                    private final ByteCodeAppender byteCodeAppender;

                    private final MethodDescription methodDescription;

                    private final ClassVisitor classVisitor;

                    private final MethodDescription redirectionMethod;

                    private CodePreservingMethodVisitor(MethodVisitor actualMethodVisitor,
                                                        ByteCodeAppender byteCodeAppender,
                                                        MethodDescription methodDescription,
                                                        ClassVisitor classVisitor,
                                                        MethodDescription redirectionMethod) {
                        super(ASM_API_VERSION, actualMethodVisitor);
                        this.actualMethodVisitor = actualMethodVisitor;
                        this.byteCodeAppender = byteCodeAppender;
                        this.methodDescription = methodDescription;
                        this.classVisitor = classVisitor;
                        this.redirectionMethod = redirectionMethod;
                    }

                    @Override
                    public void visitCode() {
                        if (byteCodeAppender.appendsCode()) {
                            actualMethodVisitor.visitCode();
                            ByteCodeAppender.Size size = byteCodeAppender.apply(actualMethodVisitor,
                                    instrumentationContext,
                                    methodDescription);
                            actualMethodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                        }
                        actualMethodVisitor.visitEnd();
                        mv = classVisitor.visitMethod(redirectionMethod.getModifiers(),
                                redirectionMethod.getInternalName(),
                                redirectionMethod.getDescriptor(),
                                redirectionMethod.getGenericSignature(),
                                redirectionMethod.getExceptionTypes().toInternalNames());
                        super.visitCode();
                    }
                }

                private class AttributeObtainingMethodVisitor extends MethodVisitor {

                    private final MethodVisitor actualMethodVisitor;

                    private final ByteCodeAppender byteCodeAppender;

                    private final MethodDescription methodDescription;

                    public AttributeObtainingMethodVisitor(MethodVisitor actualMethodVisitor,
                                                           ByteCodeAppender byteCodeAppender,
                                                           MethodDescription methodDescription) {
                        super(ASM_API_VERSION, actualMethodVisitor);
                        this.actualMethodVisitor = actualMethodVisitor;
                        this.byteCodeAppender = byteCodeAppender;
                        this.methodDescription = methodDescription;
                    }

                    @Override
                    public void visitCode() {
                        cv = null; // Ignore byte code instructions, if existent.
                    }

                    @Override
                    public void visitEnd() {
                        if (byteCodeAppender.appendsCode()) {
                            actualMethodVisitor.visitCode();
                            ByteCodeAppender.Size size = byteCodeAppender.apply(actualMethodVisitor,
                                    instrumentationContext,
                                    methodDescription);
                            actualMethodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                        }
                        actualMethodVisitor.visitEnd();
                    }
                }

                private class TypeInitializerInjection implements Instrumentation.Context.ExtractableView.InjectedCode {

                    private static final int TYPE_INITIALIZER_PROXY_MODIFIERS = Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;

                    private static final String TYPE_INITIALIZER_PROXY_PREFIX = "originalTypeInitializer";

                    private final MethodDescription injectorProxyMethod;

                    private TypeInitializerInjection() {
                        injectorProxyMethod = new MethodDescription.Latent(
                                String.format("%s$%d", TYPE_INITIALIZER_PROXY_PREFIX, Math.abs(new Random().nextInt())),
                                instrumentedType,
                                new TypeDescription.ForLoadedType(void.class),
                                new TypeList.Empty(),
                                TYPE_INITIALIZER_PROXY_MODIFIERS);
                    }

                    @Override
                    public StackManipulation getInjectedCode() {
                        return MethodInvocation.invoke(injectorProxyMethod);
                    }

                    @Override
                    public boolean isInjected() {
                        return true;
                    }

                    public MethodDescription getInjectorProxyMethod() {
                        return injectorProxyMethod;
                    }
                }
            }
        }

        static class ForCreation implements Engine {

            private final TypeDescription instrumentedType;
            private final ClassFileVersion classFileVersion;
            private final List<? extends MethodDescription> invokableMethods;
            private final ClassVisitorWrapper classVisitorWrapper;
            private final TypeAttributeAppender attributeAppender;
            private final TypeWriter.FieldPool fieldPool;
            private final TypeWriter.MethodPool methodPool;

            public ForCreation(TypeDescription instrumentedType,
                               ClassFileVersion classFileVersion,
                               List<? extends MethodDescription> invokableMethods,
                               ClassVisitorWrapper classVisitorWrapper,
                               TypeAttributeAppender attributeAppender,
                               TypeWriter.FieldPool fieldPool,
                               TypeWriter.MethodPool methodPool) {
                this.instrumentedType = instrumentedType;
                this.classFileVersion = classFileVersion;
                this.invokableMethods = invokableMethods;
                this.classVisitorWrapper = classVisitorWrapper;
                this.attributeAppender = attributeAppender;
                this.fieldPool = fieldPool;
                this.methodPool = methodPool;
            }

            @Override
            public byte[] create(Instrumentation.Context.ExtractableView instrumentationContext) {
                ClassWriter classWriter = new ClassWriter(ASM_MANUAL_FLAG);
                classWriter.visit(classFileVersion.getVersionNumber(),
                        instrumentedType.getActualModifiers(),
                        instrumentedType.getInternalName(),
                        instrumentedType.getGenericSignature(),
                        instrumentedType.getSupertype().getInternalName(),
                        instrumentedType.getInterfaces().toInternalNames());
                ClassVisitor classVisitor = classVisitorWrapper.wrap(classWriter);
                attributeAppender.apply(classVisitor, instrumentedType);
                for (FieldDescription fieldDescription : instrumentedType.getDeclaredFields()) {
                    fieldPool.target(fieldDescription).apply(classVisitor, fieldDescription);
                }
                for (MethodDescription methodDescription : invokableMethods) {
                    methodPool.target(methodDescription).apply(classVisitor, instrumentationContext, methodDescription);
                }
                instrumentationContext.drain(classVisitor,
                        methodPool,
                        Instrumentation.Context.ExtractableView.InjectedCode.None.INSTANCE);
                classVisitor.visitEnd();
                return classWriter.toByteArray();
            }
        }
    }

    /**
     * An field pool that allows a lookup for how to implement a field.
     */
    static interface FieldPool {

        /**
         * Returns the field attribute appender that matches a given field description or a default field
         * attribute appender if no appender was registered for the given field.
         *
         * @param fieldDescription The field description of interest.
         * @return The registered field attribute appender for the given field or the default appender if no such
         * appender was found.
         */
        Entry target(FieldDescription fieldDescription);

        /**
         * An entry of a field pool that describes how a field is implemented.
         *
         * @see net.bytebuddy.dynamic.scaffold.TypeWriter.FieldPool
         */
        static interface Entry {

            /**
             * Returns the field attribute appender for a given field.
             *
             * @return The attribute appender to be applied on the given field.
             */
            FieldAttributeAppender getFieldAppender();

            /**
             * Returns the default value for the field that is represented by this entry. This value might be
             * {@code null} if no such value is set.
             *
             * @return The default value for the field that is represented by this entry.
             */
            Object getDefaultValue();

            void apply(ClassVisitor classVisitor, FieldDescription fieldDescription);

            /**
             * A default implementation of a compiled field registry that simply returns a no-op
             * {@link net.bytebuddy.instrumentation.attribute.FieldAttributeAppender.Factory}
             * for any field.
             */
            static enum NoOp implements Entry {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public FieldAttributeAppender getFieldAppender() {
                    return FieldAttributeAppender.NoOp.INSTANCE;
                }

                @Override
                public Object getDefaultValue() {
                    return null;
                }

                @Override
                public void apply(ClassVisitor classVisitor, FieldDescription fieldDescription) {
                    classVisitor.visitField(fieldDescription.getModifiers(),
                            fieldDescription.getInternalName(),
                            fieldDescription.getDescriptor(),
                            fieldDescription.getGenericSignature(),
                            null).visitEnd();
                }
            }

            /**
             * A simple entry that creates a specific
             * {@link net.bytebuddy.instrumentation.attribute.FieldAttributeAppender.Factory}
             * for any field.
             */
            static class Simple implements Entry {

                /**
                 * The field attribute appender factory that is represented by this entry.
                 */
                private final FieldAttributeAppender attributeAppender;

                /**
                 * The field's default value or {@code null} if no default value is set.
                 */
                private final Object defaultValue;

                /**
                 * Creates a new simple entry for a given attribute appender factory.
                 *
                 * @param attributeAppender The attribute appender to be returned.
                 * @param defaultValue      The field's default value or {@code null} if no default value is
                 *                          set.
                 */
                public Simple(FieldAttributeAppender attributeAppender, Object defaultValue) {
                    this.attributeAppender = attributeAppender;
                    this.defaultValue = defaultValue;
                }

                @Override
                public FieldAttributeAppender getFieldAppender() {
                    return attributeAppender;
                }

                @Override
                public Object getDefaultValue() {
                    return defaultValue;
                }

                @Override
                public void apply(ClassVisitor classVisitor, FieldDescription fieldDescription) {
                    FieldVisitor fieldVisitor = classVisitor.visitField(fieldDescription.getModifiers(),
                            fieldDescription.getInternalName(),
                            fieldDescription.getDescriptor(),
                            fieldDescription.getGenericSignature(),
                            defaultValue);
                    attributeAppender.apply(fieldVisitor, fieldDescription);
                    fieldVisitor.visitEnd();
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Simple simple = (Simple) other;
                    return attributeAppender.equals(simple.attributeAppender)
                            && !(defaultValue != null ? !defaultValue.equals(simple.defaultValue) : simple.defaultValue != null);
                }

                @Override
                public int hashCode() {
                    return 31 * attributeAppender.hashCode() + (defaultValue != null ? defaultValue.hashCode() : 0);
                }

                @Override
                public String toString() {
                    return "TypeWriter.FieldPool.Entry.Simple{" +
                            "attributeAppenderFactory=" + attributeAppender +
                            ", defaultValue=" + defaultValue +
                            '}';
                }
            }
        }
    }

    /**
     * An method pool that allows a lookup for how to implement a method.
     */
    static interface MethodPool {

        /**
         * Looks up a handler entry for a given method.
         *
         * @param methodDescription The method being processed.
         * @return A handler entry for the given method.
         */
        Entry target(MethodDescription methodDescription);

        /**
         * An entry of a method pool that describes how a method is implemented.
         *
         * @see net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool
         */
        static interface Entry {

            static interface Factory {

                Entry compile(Instrumentation.Target instrumentationTarget);
            }

            /**
             * Determines if this entry requires a method to be defined for a given instrumentation.
             *
             * @return {@code true} if a method should be defined for a given instrumentation.
             */
            boolean isDefineMethod();

            /**
             * The byte code appender to be used for the instrumentation by this entry. Must not
             * be called if {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry#isDefineMethod()}
             * returns {@code false}.
             *
             * @return The byte code appender that is responsible for the instrumentation of a method matched for
             * this entry.
             */
            ByteCodeAppender getByteCodeAppender();

            /**
             * The method attribute appender that is to be used for the instrumentation by this entry.  Must not
             * be called if {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry#isDefineMethod()}
             * returns {@code false}.
             *
             * @return The method attribute appender that is responsible for the instrumentation of a method matched for
             * this entry.
             */
            MethodAttributeAppender getAttributeAppender();

            void apply(ClassVisitor classVisitor,
                       Instrumentation.Context instrumentationContext,
                       MethodDescription methodDescription);

            /**
             * A skip entry that instructs to ignore a method.
             */
            static enum Skip implements Entry, Factory {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public boolean isDefineMethod() {
                    return false;
                }

                @Override
                public ByteCodeAppender getByteCodeAppender() {
                    throw new IllegalStateException();
                }

                @Override
                public MethodAttributeAppender getAttributeAppender() {
                    throw new IllegalStateException();
                }

                @Override
                public void apply(ClassVisitor classVisitor,
                                  Instrumentation.Context instrumentationContext,
                                  MethodDescription methodDescription) {
                    /* do nothing */
                }

                @Override
                public Entry compile(Instrumentation.Target instrumentationTarget) {
                    return this;
                }
            }

            /**
             * A default implementation of {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry}
             * that is not to be ignored but is represented by a tuple of a byte code appender and a method attribute appender.
             */
            static class Simple implements Entry {

                /**
                 * The byte code appender that is represented by this entry.
                 */
                private final ByteCodeAppender byteCodeAppender;

                /**
                 * The method attribute appender that is represented by this entry.
                 */
                private final MethodAttributeAppender methodAttributeAppender;

                /**
                 * Creates a new simple entry of a method pool.
                 *
                 * @param byteCodeAppender        The byte code appender that is represented by this entry.
                 * @param methodAttributeAppender The method attribute appender that is represented by this entry.
                 */
                public Simple(ByteCodeAppender byteCodeAppender, MethodAttributeAppender methodAttributeAppender) {
                    this.byteCodeAppender = byteCodeAppender;
                    this.methodAttributeAppender = methodAttributeAppender;
                }

                @Override
                public boolean isDefineMethod() {
                    return true;
                }

                @Override
                public ByteCodeAppender getByteCodeAppender() {
                    return byteCodeAppender;
                }

                @Override
                public MethodAttributeAppender getAttributeAppender() {
                    return methodAttributeAppender;
                }

                @Override
                public void apply(ClassVisitor classVisitor,
                                  Instrumentation.Context instrumentationContext,
                                  MethodDescription methodDescription) {
                    boolean appendsCode = byteCodeAppender.appendsCode();
                    MethodVisitor methodVisitor = classVisitor.visitMethod(methodDescription.getAdjustedModifiers(appendsCode),
                            methodDescription.getInternalName(),
                            methodDescription.getDescriptor(),
                            methodDescription.getGenericSignature(),
                            methodDescription.getExceptionTypes().toInternalNames());
                    methodAttributeAppender.apply(methodVisitor, methodDescription);
                    if (appendsCode) {
                        methodVisitor.visitCode();
                        ByteCodeAppender.Size size = byteCodeAppender.apply(methodVisitor, instrumentationContext, methodDescription);
                        methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                    }
                    methodVisitor.visitEnd();
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && byteCodeAppender.equals(((Simple) other).byteCodeAppender)
                            && methodAttributeAppender.equals(((Simple) other).methodAttributeAppender);
                }

                @Override
                public int hashCode() {
                    return 31 * byteCodeAppender.hashCode() + methodAttributeAppender.hashCode();
                }

                @Override
                public String toString() {
                    return "TypeWriter.MethodPool.Entry.Simple{" +
                            "byteCodeAppender=" + byteCodeAppender +
                            ", methodAttributeAppender=" + methodAttributeAppender +
                            '}';
                }
            }
        }
    }
}
