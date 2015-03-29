package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.inline.MethodRebaseResolver;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.LoadedTypeInitializer;
import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.ParameterDescription;
import net.bytebuddy.instrumentation.method.ParameterList;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.bytebuddy.utility.ByteBuddyCommons.join;

/**
 * A type writer is a utility for writing an actual class file using the ASM library.
 *
 * @param <T> The best known loaded type for the dynamically created type.
 */
public interface TypeWriter<T> {

    /**
     * Creates the dynamic type that is described by this type writer.
     *
     * @return An unloaded dynamic type that describes the created type.
     */
    DynamicType.Unloaded<T> make();

    /**
     * An engine that is responsible for writing the actual class file.
     */
    interface Engine {

        /**
         * A flag for ASM not to automatically compute any information such as operand stack sizes and stack map frames.
         */
        int ASM_MANUAL_FLAG = 0;

        /**
         * The ASM API version to use.
         */
        int ASM_API_VERSION = Opcodes.ASM5;

        /**
         * Creates the class file.
         *
         * @param instrumentationContext The instrumentation context to use for implementing the class file.
         * @return A byte array representing the created class file.
         */
        byte[] create(Instrumentation.Context.ExtractableView instrumentationContext);

        /**
         * A type writer engine that copies the contents of a class file while allowing to override
         * method implementations.
         */
        class ForRedefinition implements Engine {

            /**
             * Instructs the retention of a method in its original form if it is not redefined.
             */
            private static final MethodDescription RETAIN_METHOD = null;

            /**
             * Representative of a {@link org.objectweb.asm.MethodVisitor} that is instructed to ignoring a result.
             */
            private static final MethodVisitor IGNORE_METHOD = null;

            /**
             * Indicates that a class has no super type, namely the {@link java.lang.Object} type.
             */
            private static final TypeDescription NO_SUPER_TYPE = null;

            /**
             * The instrumented type that is written.
             */
            private final TypeDescription instrumentedType;

            /**
             * The original type which is redefined.
             */
            private final TypeDescription targetType;

            /**
             * The specified class file version.
             */
            private final ClassFileVersion classFileVersion;

            /**
             * The methods that are to be considered for implementation.
             */
            private final List<? extends MethodDescription> invokableMethods;

            /**
             * A wrapper to apply to the actual class visitor.
             */
            private final ClassVisitorWrapper classVisitorWrapper;

            /**
             * The attribute appender to apply.
             */
            private final TypeAttributeAppender attributeAppender;

            /**
             * The field pool to use for writing fields.
             */
            private final TypeWriter.FieldPool fieldPool;

            /**
             * The method pool to use for writing methods.
             */
            private final TypeWriter.MethodPool methodPool;

            /**
             * A provider for creating an input stream.
             */
            private final ClassFileLocator classFileLocator;

            /**
             * A resolver for method rebasing.
             */
            private final MethodRebaseResolver methodRebaseResolver;

            /**
             * Creates a new type writer that reads a class file and weaves in user defined method implementations.
             *
             * @param instrumentedType     The instrumented type that is written.
             * @param targetType           The original type which is redefined.
             * @param classFileVersion     The specified class file version.
             * @param invokableMethods     The methods that are to be considered for implementation.
             * @param classVisitorWrapper  A wrapper to apply to the actual class visitor.
             * @param attributeAppender    The attribute appender to apply.
             * @param fieldPool            The field pool to use for writing fields.
             * @param methodPool           The method pool to use for writing fields.
             * @param classFileLocator     A provider for creating an input stream.
             * @param methodRebaseResolver A resolver for method rebasing.
             */
            public ForRedefinition(TypeDescription instrumentedType,
                                   TypeDescription targetType,
                                   ClassFileVersion classFileVersion,
                                   List<? extends MethodDescription> invokableMethods,
                                   ClassVisitorWrapper classVisitorWrapper,
                                   TypeAttributeAppender attributeAppender,
                                   TypeWriter.FieldPool fieldPool,
                                   TypeWriter.MethodPool methodPool,
                                   ClassFileLocator classFileLocator,
                                   MethodRebaseResolver methodRebaseResolver) {
                this.instrumentedType = instrumentedType;
                this.targetType = targetType;
                this.classFileVersion = classFileVersion;
                this.invokableMethods = invokableMethods;
                this.classVisitorWrapper = classVisitorWrapper;
                this.attributeAppender = attributeAppender;
                this.fieldPool = fieldPool;
                this.methodPool = methodPool;
                this.classFileLocator = classFileLocator;
                this.methodRebaseResolver = methodRebaseResolver;
            }

            @Override
            public byte[] create(Instrumentation.Context.ExtractableView instrumentationContext) {
                try {
                    ClassFileLocator.Resolution resolution = classFileLocator.locate(targetType.getName());
                    if (!resolution.isResolved()) {
                        throw new IllegalArgumentException("Cannot locate the class file for " + targetType + " using " + classFileLocator);
                    }
                    return doCreate(instrumentationContext, resolution.resolve());
                } catch (IOException e) {
                    throw new RuntimeException("The class file could not be written", e);
                }
            }

            /**
             * Performs the actual creation of a class file.
             *
             * @param instrumentationContext The instrumentation context to use for implementing the class file.
             * @param binaryRepresentation   The binary representation of the class file.
             * @return The byte array representing the created class.
             */
            private byte[] doCreate(Instrumentation.Context.ExtractableView instrumentationContext,
                                    byte[] binaryRepresentation) {
                ClassReader classReader = new ClassReader(binaryRepresentation);
                ClassWriter classWriter = new ClassWriter(classReader, ASM_MANUAL_FLAG);
                classReader.accept(writeTo(classVisitorWrapper.wrap(classWriter), instrumentationContext),
                        ASM_MANUAL_FLAG);
                return classWriter.toByteArray();
            }

            /**
             * Creates a class visitor which weaves all changes and additions on the fly.
             *
             * @param classVisitor           The class visitor to which this entry is to be written to.
             * @param instrumentationContext The instrumentation context to use for implementing the class file.
             * @return A class visitor which is capable of applying the changes.
             */
            private ClassVisitor writeTo(ClassVisitor classVisitor,
                                         Instrumentation.Context.ExtractableView instrumentationContext) {
                String originalName = targetType.getInternalName();
                String targetName = instrumentedType.getInternalName();
                ClassVisitor targetClassVisitor = new RedefinitionClassVisitor(classVisitor, instrumentationContext);
                return originalName.equals(targetName)
                        ? targetClassVisitor
                        : new RemappingClassAdapter(targetClassVisitor, new SimpleRemapper(originalName, targetName));
            }

            @Override
            public boolean equals(Object other) {
                if (this == other)
                    return true;
                if (other == null || getClass() != other.getClass())
                    return false;
                ForRedefinition that = (ForRedefinition) other;
                return attributeAppender.equals(that.attributeAppender)
                        && classFileLocator.equals(that.classFileLocator)
                        && classFileVersion.equals(that.classFileVersion)
                        && classVisitorWrapper.equals(that.classVisitorWrapper)
                        && fieldPool.equals(that.fieldPool)
                        && instrumentedType.equals(that.instrumentedType)
                        && invokableMethods.equals(that.invokableMethods)
                        && methodPool.equals(that.methodPool)
                        && methodRebaseResolver.equals(that.methodRebaseResolver)
                        && targetType.equals(that.targetType);
            }

            @Override
            public int hashCode() {
                int result = instrumentedType.hashCode();
                result = 31 * result + targetType.hashCode();
                result = 31 * result + classFileVersion.hashCode();
                result = 31 * result + invokableMethods.hashCode();
                result = 31 * result + classVisitorWrapper.hashCode();
                result = 31 * result + attributeAppender.hashCode();
                result = 31 * result + fieldPool.hashCode();
                result = 31 * result + methodPool.hashCode();
                result = 31 * result + classFileLocator.hashCode();
                result = 31 * result + methodRebaseResolver.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "TypeWriter.Engine.ForRedefinition{" +
                        "instrumentedType=" + instrumentedType +
                        ", targetType=" + targetType +
                        ", classFileVersion=" + classFileVersion +
                        ", invokableMethods=" + invokableMethods +
                        ", classVisitorWrapper=" + classVisitorWrapper +
                        ", attributeAppender=" + attributeAppender +
                        ", fieldPool=" + fieldPool +
                        ", methodPool=" + methodPool +
                        ", classFileLocator=" + classFileLocator +
                        ", methodRebaseResolver=" + methodRebaseResolver +
                        '}';
            }

            /**
             * A class visitor which is capable of applying a redefinition of an existing class file.
             */
            protected class RedefinitionClassVisitor extends ClassVisitor {

                /**
                 * The instrumentation context for this class creation.
                 */
                private final Instrumentation.Context.ExtractableView instrumentationContext;

                /**
                 * A mutable map of all declared fields of the instrumented type by their names.
                 */
                private final Map<String, FieldDescription> declaredFields;

                /**
                 * A mutable map of all declarable methods of the instrumented type by their unique signatures.
                 */
                private final Map<String, MethodDescription> declarableMethods;

                /**
                 * A mutable reference for code that is to be injected into the actual type initializer, if any.
                 * Usually, this represents an invocation of the actual type initializer that is found in the class
                 * file which is relocated into a static method.
                 */
                private Instrumentation.Context.ExtractableView.InjectedCode injectedCode;

                /**
                 * Creates a class visitor which is capable of redefining an existent class on the fly.
                 *
                 * @param classVisitor           The underlying class visitor to which writes are delegated.
                 * @param instrumentationContext The instrumentation context to use for implementing the class file.
                 */
                protected RedefinitionClassVisitor(ClassVisitor classVisitor,
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
                    ClassFileVersion originalClassFileVersion = new ClassFileVersion(classFileVersionNumber);
                    super.visit((classFileVersion.compareTo(originalClassFileVersion) > 0
                                    ? classFileVersion
                                    : originalClassFileVersion).getVersionNumber(),
                            instrumentedType.getActualModifiers((modifiers & Opcodes.ACC_SUPER) != 0),
                            instrumentedType.getInternalName(),
                            instrumentedType.getGenericSignature(),
                            instrumentedType.getSupertype() == NO_SUPER_TYPE ?
                                    null :
                                    instrumentedType.getSupertype().getInternalName(),
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
                    return methodDescription == RETAIN_METHOD
                            ? super.visitMethod(modifiers, internalName, descriptor, genericSignature, exceptionTypeInternalName)
                            : redefine(methodDescription, (modifiers & Opcodes.ACC_ABSTRACT) != 0);
                }

                /**
                 * Redefines a given method if this is required by looking up a potential implementation from the
                 * {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool}.
                 *
                 * @param methodDescription The method being considered for redefinition.
                 * @param abstractOrigin    {@code true} if the original method is abstract, i.e. there is no implementation
                 *                          to preserve.
                 * @return A method visitor which is capable of consuming the original method.
                 */
                private MethodVisitor redefine(MethodDescription methodDescription, boolean abstractOrigin) {
                    TypeWriter.MethodPool.Entry entry = methodPool.target(methodDescription);
                    if (!entry.isDefineMethod()) {
                        return super.visitMethod(methodDescription.getModifiers(),
                                methodDescription.getInternalName(),
                                methodDescription.getDescriptor(),
                                methodDescription.getGenericSignature(),
                                methodDescription.getExceptionTypes().toInternalNames());
                    }
                    MethodVisitor methodVisitor = super.visitMethod(
                            methodDescription.getAdjustedModifiers(entry.getByteCodeAppender().appendsCode()),
                            methodDescription.getInternalName(),
                            methodDescription.getDescriptor(),
                            methodDescription.getGenericSignature(),
                            methodDescription.getExceptionTypes().toInternalNames());
                    entry.getAttributeAppender().apply(methodVisitor, methodDescription);
                    return abstractOrigin
                            ? new AttributeObtainingMethodVisitor(methodVisitor, entry.getByteCodeAppender(), methodDescription)
                            : new CodePreservingMethodVisitor(methodVisitor, entry.getByteCodeAppender(), methodDescription);
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

                /**
                 * A method visitor that preserves the code of a method in the class file by copying it into a rebased
                 * method while copying all attributes and annotations to the actual method.
                 */
                private class CodePreservingMethodVisitor extends MethodVisitor {

                    /**
                     * The method visitor of the actual method.
                     */
                    private final MethodVisitor actualMethodVisitor;

                    /**
                     * The byte code appender to apply to the actual method.
                     */
                    private final ByteCodeAppender byteCodeAppender;

                    /**
                     * A description of the actual method.
                     */
                    private final MethodDescription methodDescription;

                    /**
                     * The resolution of a potential rebased method.
                     */
                    private final MethodRebaseResolver.Resolution resolution;

                    /**
                     * Creates a code preserving method visitor.
                     *
                     * @param actualMethodVisitor The method visitor of the actual method.
                     * @param byteCodeAppender    The byte code appender to apply to the actual method.
                     * @param methodDescription   A description of the actual method.
                     */
                    private CodePreservingMethodVisitor(MethodVisitor actualMethodVisitor,
                                                        ByteCodeAppender byteCodeAppender,
                                                        MethodDescription methodDescription) {
                        super(ASM_API_VERSION, actualMethodVisitor);
                        this.actualMethodVisitor = actualMethodVisitor;
                        this.byteCodeAppender = byteCodeAppender;
                        this.methodDescription = methodDescription;
                        this.resolution = methodRebaseResolver.resolve(methodDescription);
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
                        mv = resolution.isRebased()
                                ? cv.visitMethod(resolution.getResolvedMethod().getModifiers(),
                                resolution.getResolvedMethod().getInternalName(),
                                resolution.getResolvedMethod().getDescriptor(),
                                resolution.getResolvedMethod().getGenericSignature(),
                                resolution.getResolvedMethod().getExceptionTypes().toInternalNames())
                                : IGNORE_METHOD;
                        super.visitCode();
                    }

                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        super.visitMaxs(maxStack, Math.max(maxLocals, resolution.getResolvedMethod().getStackSize()));
                    }

                    @Override
                    public String toString() {
                        return "TypeWriter.Engine.ForRedefinition.RedefinitionClassVisitor.CodePreservingMethodVisitor{" +
                                "actualMethodVisitor=" + actualMethodVisitor +
                                ", byteCodeAppender=" + byteCodeAppender +
                                ", methodDescription=" + methodDescription +
                                ", resolution=" + resolution +
                                '}';
                    }
                }

                /**
                 * A method visitor that obtains all attributes and annotations of a method that is found in the
                 * class file but discards all code.
                 */
                private class AttributeObtainingMethodVisitor extends MethodVisitor {

                    /**
                     * The method visitor to which the actual method is to be written to.
                     */
                    private final MethodVisitor actualMethodVisitor;

                    /**
                     * The byte code appender to apply to the actual method.
                     */
                    private final ByteCodeAppender byteCodeAppender;

                    /**
                     * A description of the method that is currently written.
                     */
                    private final MethodDescription methodDescription;

                    /**
                     * Creates a new attribute obtaining method visitor.
                     *
                     * @param actualMethodVisitor The method visitor to which the actual method is to be written to.
                     * @param byteCodeAppender    The byte code appender to apply to the actual method.
                     * @param methodDescription   A description of the method that is currently written.
                     */
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
                        mv = IGNORE_METHOD;
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

                    @Override
                    public String toString() {
                        return "TypeWriter.Engine.ForRedefinition.RedefinitionClassVisitor.AttributeObtainingMethodVisitor{"
                                +
                                "actualMethodVisitor=" + actualMethodVisitor +
                                ", byteCodeAppender=" + byteCodeAppender +
                                ", methodDescription=" + methodDescription +
                                '}';
                    }
                }

                /**
                 * A code injection for the type initializer that invokes a method representing the original type initializer
                 * which is copied to a static method.
                 */
                private class TypeInitializerInjection implements Instrumentation.Context.ExtractableView.InjectedCode {

                    /**
                     * The modifiers for the method that consumes the original type initializer.
                     */
                    private static final int TYPE_INITIALIZER_PROXY_MODIFIERS =
                            Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;

                    /**
                     * A prefix for the name of the method that represents the original type initializer.
                     */
                    private static final String TYPE_INITIALIZER_PROXY_PREFIX = "originalTypeInitializer";

                    /**
                     * The method to which the original type initializer code is to be written to.
                     */
                    private final MethodDescription injectorProxyMethod;

                    /**
                     * Creates a new type initializer injection.
                     */
                    private TypeInitializerInjection() {
                        injectorProxyMethod = new MethodDescription.Latent(
                                String.format("%s$%s", TYPE_INITIALIZER_PROXY_PREFIX, RandomString.make()),
                                instrumentedType,
                                TypeDescription.VOID,
                                new TypeList.Empty(),
                                TYPE_INITIALIZER_PROXY_MODIFIERS,
                                Collections.<TypeDescription>emptyList());
                    }

                    @Override
                    public StackManipulation getStackManipulation() {
                        return MethodInvocation.invoke(injectorProxyMethod);
                    }

                    @Override
                    public boolean isDefined() {
                        return true;
                    }

                    /**
                     * Returns the proxy method to which the original type initializer code is written to.
                     *
                     * @return A method description of this proxy method.
                     */
                    public MethodDescription getInjectorProxyMethod() {
                        return injectorProxyMethod;
                    }

                    @Override
                    public String toString() {
                        return "TypeWriter.Engine.ForRedefinition.RedefinitionClassVisitor.TypeInitializerInjection{" +
                                "injectorProxyMethod=" + injectorProxyMethod +
                                '}';
                    }
                }
            }
        }

        /**
         * A type writer engine that creates a new dynamic type that is not based on an existent type.
         */
        class ForCreation implements Engine {

            /**
             * The instrumented type that is created.
             */
            private final TypeDescription instrumentedType;

            /**
             * The class file version of the type that is to be written.
             */
            private final ClassFileVersion classFileVersion;

            /**
             * The invokable methods to consider for implementation.
             */
            private final List<? extends MethodDescription> invokableMethods;

            /**
             * The class visitor wrapper to apply to the ASM class writer.
             */
            private final ClassVisitorWrapper classVisitorWrapper;

            /**
             * The attribute appender to apply.
             */
            private final TypeAttributeAppender attributeAppender;

            /**
             * The field pool to use for writing fields.
             */
            private final TypeWriter.FieldPool fieldPool;

            /**
             * The method pool to use for writing methods.
             */
            private final TypeWriter.MethodPool methodPool;

            /**
             * Creates a new type writer engine for redefining an existent class file.
             *
             * @param instrumentedType    The instrumented type that is created.
             * @param classFileVersion    The class file version of the type that is to be written.
             * @param invokableMethods    The invokable methods to consider for implementation.
             * @param classVisitorWrapper The class visitor wrapper to apply to the ASM class writer.
             * @param attributeAppender   The attribute appender to apply.
             * @param fieldPool           The field pool to use for writing fields.
             * @param methodPool          The method pool to use for writing methods.
             */
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
                ClassVisitor classVisitor = classVisitorWrapper.wrap(classWriter);
                classVisitor.visit(classFileVersion.getVersionNumber(),
                        instrumentedType.getActualModifiers(!instrumentedType.isInterface()),
                        instrumentedType.getInternalName(),
                        instrumentedType.getGenericSignature(),
                        (instrumentedType.getSupertype() == null
                                ? TypeDescription.OBJECT
                                : instrumentedType.getSupertype()).getInternalName(),
                        instrumentedType.getInterfaces().toInternalNames());
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

            @Override
            public boolean equals(Object other) {
                if (this == other)
                    return true;
                if (other == null || getClass() != other.getClass())
                    return false;
                ForCreation that = (ForCreation) other;
                return attributeAppender.equals(that.attributeAppender)
                        && classFileVersion.equals(that.classFileVersion)
                        && classVisitorWrapper.equals(that.classVisitorWrapper)
                        && fieldPool.equals(that.fieldPool)
                        && instrumentedType.equals(that.instrumentedType)
                        && invokableMethods.equals(that.invokableMethods)
                        && methodPool.equals(that.methodPool);
            }

            @Override
            public int hashCode() {
                int result = instrumentedType.hashCode();
                result = 31 * result + classFileVersion.hashCode();
                result = 31 * result + invokableMethods.hashCode();
                result = 31 * result + classVisitorWrapper.hashCode();
                result = 31 * result + attributeAppender.hashCode();
                result = 31 * result + fieldPool.hashCode();
                result = 31 * result + methodPool.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "TypeWriter.Engine.ForCreation{" +
                        "instrumentedType=" + instrumentedType +
                        ", classFileVersion=" + classFileVersion +
                        ", invokableMethods=" + invokableMethods +
                        ", classVisitorWrapper=" + classVisitorWrapper +
                        ", attributeAppender=" + attributeAppender +
                        ", fieldPool=" + fieldPool +
                        ", methodPool=" + methodPool +
                        '}';
            }
        }
    }

    /**
     * An field pool that allows a lookup for how to implement a field.
     */
    interface FieldPool {

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
        interface Entry {

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

            /**
             * Writes this entry to a given class visitor.
             *
             * @param classVisitor     The class visitor to which this entry is to be written to.
             * @param fieldDescription A description of the field that is to be written.
             */
            void apply(ClassVisitor classVisitor, FieldDescription fieldDescription);

            /**
             * A default implementation of a compiled field registry that simply returns a no-op
             * {@link net.bytebuddy.instrumentation.attribute.FieldAttributeAppender.Factory}
             * for any field.
             */
            enum NoOp implements Entry {

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

                @Override
                public String toString() {
                    return "TypeWriter.FieldPool.Entry.NoOp." + name();
                }
            }

            /**
             * A simple entry that creates a specific
             * {@link net.bytebuddy.instrumentation.attribute.FieldAttributeAppender.Factory}
             * for any field.
             */
            class Simple implements Entry {

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
                    if (this == other)
                        return true;
                    if (other == null || getClass() != other.getClass())
                        return false;
                    Simple simple = (Simple) other;
                    return attributeAppender.equals(simple.attributeAppender)
                            && !(defaultValue != null ?
                            !defaultValue.equals(simple.defaultValue) :
                            simple.defaultValue != null);
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
    interface MethodPool {

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
        interface Entry {

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

            /**
             * Writes the method that is represented by this entry to the provided class visitor.
             *
             * @param classVisitor           The class visitor to which this entry is to be written to.
             * @param instrumentationContext The instrumentation context to use for writing this method.
             * @param methodDescription      A description of the method that is to be written
             */
            void apply(ClassVisitor classVisitor,
                       Instrumentation.Context instrumentationContext,
                       MethodDescription methodDescription);

            /**
             * A skip entry that instructs to ignore a method.
             */
            enum Skip implements Entry, Factory {

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

                @Override
                public String toString() {
                    return "TypeWriter.MethodPool.Entry.Skip." + name();
                }
            }

            /**
             * A factory for creating a {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry}.
             */
            interface Factory {

                /**
                 * Compiles a {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry}.
                 *
                 * @param instrumentationTarget The instrumentation target for which this factory is to be compiled.
                 * @return A compiled entry for the given instrumentation target.
                 */
                Entry compile(Instrumentation.Target instrumentationTarget);
            }

            /**
             * A default implementation of {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry}
             * that is not to be ignored but is represented by a tuple of a byte code appender and a method attribute appender.
             */
            class Simple implements Entry {

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
                    MethodVisitor methodVisitor = classVisitor
                            .visitMethod(methodDescription.getAdjustedModifiers(appendsCode),
                                    methodDescription.getInternalName(),
                                    methodDescription.getDescriptor(),
                                    methodDescription.getGenericSignature(),
                                    methodDescription.getExceptionTypes().toInternalNames());
                    ParameterList parameterList = methodDescription.getParameters();
                    if (parameterList.hasExplicitMetaData()) {
                        for (ParameterDescription parameterDescription : parameterList) {
                            methodVisitor.visitParameter(parameterDescription.getName(), parameterDescription.getModifiers());
                        }
                    }
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

    /**
     * A default implementation of a {@link net.bytebuddy.dynamic.scaffold.TypeWriter}.
     *
     * @param <S> The best known loaded type for the dynamically created type.
     */
    class Default<S> implements TypeWriter<S> {

        /**
         * The instrumented type that is to be written.
         */
        private final TypeDescription instrumentedType;

        /**
         * The loaded type initializer of the instrumented type.
         */
        private final LoadedTypeInitializer loadedTypeInitializer;

        /**
         * The type initializer of the instrumented type.
         */
        private final InstrumentedType.TypeInitializer typeInitializer;

        /**
         * A list of explicit auxiliary types that are to be added to the created dynamic type.
         */
        private final List<DynamicType> explicitAuxiliaryTypes;

        /**
         * The class file version of the written type.
         */
        private final ClassFileVersion classFileVersion;

        /**
         * An engine for writing the actual class file for the instrumented type.
         */
        private final Engine engine;

        /**
         * Creates a new immutable type writer.
         *
         * @param instrumentedType       The instrumented type that is to be written.
         * @param loadedTypeInitializer  The loaded type initializer of the instrumented type.
         * @param typeInitializer        The type initializer of the instrumented type.
         * @param explicitAuxiliaryTypes A list of explicit auxiliary types that are to be added to the created
         *                               dynamic type.
         * @param classFileVersion       The class file version of the type that is to be written.
         * @param engine                 An engine for writing the actual class file for the instrumented type.
         */
        public Default(TypeDescription instrumentedType,
                       LoadedTypeInitializer loadedTypeInitializer,
                       InstrumentedType.TypeInitializer typeInitializer,
                       List<DynamicType> explicitAuxiliaryTypes,
                       ClassFileVersion classFileVersion,
                       Engine engine) {
            this.instrumentedType = instrumentedType;
            this.loadedTypeInitializer = loadedTypeInitializer;
            this.typeInitializer = typeInitializer;
            this.explicitAuxiliaryTypes = explicitAuxiliaryTypes;
            this.classFileVersion = classFileVersion;
            this.engine = engine;
        }

        @Override
        public DynamicType.Unloaded<S> make() {
            Instrumentation.Context.ExtractableView instrumentationContext = new Instrumentation.Context.Default(
                    instrumentedType,
                    typeInitializer,
                    classFileVersion);
            return new DynamicType.Default.Unloaded<S>(instrumentedType,
                    engine.create(instrumentationContext),
                    loadedTypeInitializer,
                    join(explicitAuxiliaryTypes, instrumentationContext.getRegisteredAuxiliaryTypes()));
        }

        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (other == null || getClass() != other.getClass())
                return false;
            Default aDefault = (Default) other;
            return engine.equals(aDefault.engine)
                    && explicitAuxiliaryTypes.equals(aDefault.explicitAuxiliaryTypes)
                    && instrumentedType.equals(aDefault.instrumentedType)
                    && classFileVersion.equals(aDefault.classFileVersion)
                    && loadedTypeInitializer.equals(aDefault.loadedTypeInitializer)
                    && typeInitializer.equals(aDefault.typeInitializer);
        }

        @Override
        public int hashCode() {
            int result = instrumentedType.hashCode();
            result = 31 * result + loadedTypeInitializer.hashCode();
            result = 31 * result + typeInitializer.hashCode();
            result = 31 * result + explicitAuxiliaryTypes.hashCode();
            result = 31 * result + engine.hashCode();
            result = 31 * result + classFileVersion.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "TypeWriter.Default{" +
                    "instrumentedType=" + instrumentedType +
                    ", loadedTypeInitializer=" + loadedTypeInitializer +
                    ", typeInitializer=" + typeInitializer +
                    ", explicitAuxiliaryTypes=" + explicitAuxiliaryTypes +
                    ", classFileVersion=" + classFileVersion +
                    ", engine=" + engine +
                    '}';
        }
    }
}
