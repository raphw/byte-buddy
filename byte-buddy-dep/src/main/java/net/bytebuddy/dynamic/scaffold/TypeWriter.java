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
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationAppender;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.ParameterDescription;
import net.bytebuddy.instrumentation.method.ParameterList;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
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

            Sort getSort();

            Entry mergeWith(ByteCodeAppender byteCodeAppender)

            void apply(ClassVisitor classVisitor, Instrumentation.Context instrumentationContext, MethodDescription methodDescription);

            void applyHead(MethodVisitor methodVisitor, MethodDescription methodDescription);

            void applyBody(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext, MethodDescription methodDescription);

            enum Sort {

                SKIP(false, false),

                DEFINE(true, false),

                IMPLEMENT(true, true);

                private final boolean define;

                private final boolean implement;

                Sort(boolean define, boolean implement) {
                    this.define = define;
                    this.implement = implement;
                }

                public boolean isDefined() {
                    return define;
                }

                public boolean isImplemented() {
                    return implement;
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

            abstract class AbstractDefiningEntry implements Entry {

                @Override
                public void apply(ClassVisitor classVisitor, Instrumentation.Context instrumentationContext, MethodDescription methodDescription) {
                    MethodVisitor methodVisitor = classVisitor
                            .visitMethod(methodDescription.getAdjustedModifiers(getSort().isImplemented()),
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
                    applyHead(methodVisitor, methodDescription);
                    applyBody(methodVisitor, instrumentationContext, methodDescription);
                    methodVisitor.visitEnd();
                }
            }

            enum ForSkippedMethod implements Entry, Factory {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public void apply(ClassVisitor classVisitor,
                                  Instrumentation.Context instrumentationContext,
                                  MethodDescription methodDescription) {
                    /* do nothing */
                }

                @Override
                public void applyBody(MethodVisitor methodVisitor,
                                      Instrumentation.Context instrumentationContext,
                                      MethodDescription methodDescription) {
                    throw new IllegalStateException("Cannot apply headless implementation for method that should be skipped");
                }

                @Override
                public void applyHead(MethodVisitor methodVisitor, MethodDescription methodDescription) {
                    throw new IllegalStateException("Cannot apply headless implementation for method that should be skipped");
                }

                @Override
                public Sort getSort() {
                    return Sort.SKIP;
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

            class ForImplementation extends AbstractDefiningEntry {

                private final ByteCodeAppender byteCodeAppender;

                private final MethodAttributeAppender methodAttributeAppender;

                public ForImplementation(ByteCodeAppender byteCodeAppender, MethodAttributeAppender methodAttributeAppender) {
                    this.byteCodeAppender = byteCodeAppender;
                    this.methodAttributeAppender = methodAttributeAppender;
                }

                @Override
                public void applyHead(MethodVisitor methodVisitor, MethodDescription methodDescription) {
                    /* do nothing */
                }

                @Override
                public void applyBody(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext, MethodDescription methodDescription) {
                    methodAttributeAppender.apply(methodVisitor, methodDescription);
                    methodVisitor.visitCode();
                    ByteCodeAppender.Size size = byteCodeAppender.apply(methodVisitor, instrumentationContext, methodDescription);
                    methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                }

                @Override
                public Sort getSort() {
                    return Sort.IMPLEMENT;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && byteCodeAppender.equals(((ForImplementation) other).byteCodeAppender)
                            && methodAttributeAppender.equals(((ForImplementation) other).methodAttributeAppender);
                }

                @Override
                public int hashCode() {
                    return 31 * byteCodeAppender.hashCode() + methodAttributeAppender.hashCode();
                }

                @Override
                public String toString() {
                    return "TypeWriter.MethodPool.Entry.ForImplementation{" +
                            "byteCodeAppender=" + byteCodeAppender +
                            ", methodAttributeAppender=" + methodAttributeAppender +
                            '}';
                }
            }

            class ForAbstractMethod extends AbstractDefiningEntry {

                private final MethodAttributeAppender methodAttributeAppender;

                public ForAbstractMethod(MethodAttributeAppender methodAttributeAppender) {
                    this.methodAttributeAppender = methodAttributeAppender;
                }

                @Override
                public Sort getSort() {
                    return Sort.DEFINE;
                }

                @Override
                public void applyHead(MethodVisitor methodVisitor, MethodDescription methodDescription) {
                    /* do nothing */
                }

                @Override
                public void applyBody(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext, MethodDescription methodDescription) {
                    methodAttributeAppender.apply(methodVisitor, methodDescription);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && methodAttributeAppender.equals(((ForAbstractMethod) other).methodAttributeAppender);
                }

                @Override
                public int hashCode() {
                    return methodAttributeAppender.hashCode();
                }

                @Override
                public String toString() {
                    return "TypeWriter.MethodPool.Entry.ForAbstractMethod{" +
                            "methodAttributeAppender=" + methodAttributeAppender +
                            '}';
                }
            }

            class ForAnnotationDefaultValue extends AbstractDefiningEntry {

                private final Object annotationValue;

                private final MethodAttributeAppender methodAttributeAppender;

                public ForAnnotationDefaultValue(Object annotationValue, MethodAttributeAppender methodAttributeAppender) {
                    this.annotationValue = annotationValue;
                    this.methodAttributeAppender = methodAttributeAppender;
                }

                @Override
                public Sort getSort() {
                    return Sort.DEFINE;
                }

                @Override
                public void applyHead(MethodVisitor methodVisitor, MethodDescription methodDescription) {
                    AnnotationAppender.Default.apply(methodVisitor.visitAnnotationDefault(),
                            methodDescription.getReturnType(),
                            AnnotationAppender.NO_NAME,
                            annotationValue);
                }

                @Override
                public void applyBody(MethodVisitor methodVisitor,
                                      Instrumentation.Context instrumentationContext,
                                      MethodDescription methodDescription) {
                    methodAttributeAppender.apply(methodVisitor, methodDescription);
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    ForAnnotationDefaultValue that = (ForAnnotationDefaultValue) other;
                    return annotationValue.equals(that.annotationValue) && methodAttributeAppender.equals(that.methodAttributeAppender);

                }

                @Override
                public int hashCode() {
                    int result = annotationValue.hashCode();
                    result = 31 * result + methodAttributeAppender.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "TypeWriter.MethodPool.Entry.ForAnnotationDefaultValue{" +
                            "annotationValue=" + annotationValue +
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
    abstract class Default<S> implements TypeWriter<S> {

        /**
         * A flag for ASM not to automatically compute any information such as operand stack sizes and stack map frames.
         */
        protected int ASM_MANUAL_FLAG = 0;

        /**
         * The ASM API version to use.
         */
        protected int ASM_API_VERSION = Opcodes.ASM5;

        /**
         * The instrumented type that is to be written.
         */
        protected final TypeDescription instrumentedType;

        /**
         * The loaded type initializer of the instrumented type.
         */
        protected final LoadedTypeInitializer loadedTypeInitializer;

        /**
         * The type initializer of the instrumented type.
         */
        protected final InstrumentedType.TypeInitializer typeInitializer;

        /**
         * A list of explicit auxiliary types that are to be added to the created dynamic type.
         */
        protected final List<DynamicType> explicitAuxiliaryTypes;

        /**
         * The class file version of the written type.
         */
        protected final ClassFileVersion classFileVersion;

        protected final AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

        protected final ClassVisitorWrapper classVisitorWrapper;

        protected final TypeAttributeAppender attributeAppender;

        protected final FieldPool fieldPool;

        protected final MethodPool methodPool;

        protected final MethodList invokeableMethods;

        public static <U> TypeWriter<U> forCreation(MethodRegistry.Compiled methodRegistry,
                                                    FieldPool fieldPool,
                                                    AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                    ClassVisitorWrapper classVisitorWrapper,
                                                    TypeAttributeAppender attributeAppender,
                                                    ClassFileVersion classFileVersion) {
            return new ForCreation<U>(methodRegistry.getInstrumentedType(),
                    methodRegistry.getLoadedTypeInitializer(),
                    methodRegistry.getTypeInitializer(),
                    Collections.<DynamicType>emptyList(),
                    classFileVersion,
                    auxiliaryTypeNamingStrategy,
                    classVisitorWrapper,
                    attributeAppender,
                    fieldPool,
                    methodRegistry,
                    methodRegistry.getInstrumentedMethods());
        }

        public static <U> TypeWriter<U> forRebasing(MethodRegistry.Compiled methodRegistry,
                                                    FieldPool fieldPool,
                                                    AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                    ClassVisitorWrapper classVisitorWrapper,
                                                    TypeAttributeAppender attributeAppender,
                                                    ClassFileVersion classFileVersion,
                                                    ClassFileLocator classFileLocator,
                                                    TypeDescription targetType,
                                                    MethodRebaseResolver methodRebaseResolver) {
            return new ForInlining<U>(methodRegistry.getInstrumentedType(),
                    methodRegistry.getLoadedTypeInitializer(),
                    methodRegistry.getTypeInitializer(),
                    methodRebaseResolver.getAuxiliaryTypes(),
                    classFileVersion,
                    auxiliaryTypeNamingStrategy,
                    classVisitorWrapper,
                    attributeAppender,
                    fieldPool,
                    methodRegistry,
                    methodRegistry.getInstrumentedMethods(),
                    classFileLocator,
                    targetType,
                    methodRebaseResolver);
        }

        public static <U> TypeWriter<U> forRedefinition(MethodRegistry.Compiled methodRegistry,
                                                        FieldPool fieldPool,
                                                        AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                        ClassVisitorWrapper classVisitorWrapper,
                                                        TypeAttributeAppender attributeAppender,
                                                        ClassFileVersion classFileVersion,
                                                        ClassFileLocator classFileLocator,
                                                        TypeDescription targetType) {
            return new ForInlining<U>(methodRegistry.getInstrumentedType(),
                    methodRegistry.getLoadedTypeInitializer(),
                    methodRegistry.getTypeInitializer(),
                    Collections.<DynamicType>emptyList(),
                    classFileVersion,
                    auxiliaryTypeNamingStrategy,
                    classVisitorWrapper,
                    attributeAppender,
                    fieldPool,
                    methodRegistry,
                    methodRegistry.getInstrumentedMethods(),
                    classFileLocator,
                    targetType,
                    MethodRebaseResolver.Forbidden.INSTANCE);
        }

        protected Default(TypeDescription instrumentedType,
                          LoadedTypeInitializer loadedTypeInitializer,
                          InstrumentedType.TypeInitializer typeInitializer,
                          List<DynamicType> explicitAuxiliaryTypes,
                          ClassFileVersion classFileVersion,
                          AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                          ClassVisitorWrapper classVisitorWrapper,
                          TypeAttributeAppender attributeAppender,
                          FieldPool fieldPool,
                          MethodPool methodPool,
                          MethodList instrumentedMethods) {
            this.instrumentedType = instrumentedType;
            this.loadedTypeInitializer = loadedTypeInitializer;
            this.typeInitializer = typeInitializer;
            this.explicitAuxiliaryTypes = explicitAuxiliaryTypes;
            this.classFileVersion = classFileVersion;
            this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
            this.classVisitorWrapper = classVisitorWrapper;
            this.attributeAppender = attributeAppender;
            this.fieldPool = fieldPool;
            this.methodPool = methodPool;
            this.invokeableMethods = instrumentedMethods;
        }

        @Override
        public DynamicType.Unloaded<S> make() {
            Instrumentation.Context.ExtractableView instrumentationContext = new Instrumentation.Context.Default(instrumentedType,
                    auxiliaryTypeNamingStrategy,
                    typeInitializer,
                    classFileVersion);
            return new DynamicType.Default.Unloaded<S>(instrumentedType,
                    create(instrumentationContext),
                    loadedTypeInitializer,
                    join(explicitAuxiliaryTypes, instrumentationContext.getRegisteredAuxiliaryTypes()));
        }

        protected abstract byte[] create(Instrumentation.Context.ExtractableView instrumentationContext);

        public static class ForInlining<U> extends Default<U> {

            private static final TypeDescription NO_SUPER_TYPE = null;

            private static final MethodDescription RETAIN_METHOD = null;

            private static final MethodVisitor IGNORE_METHOD = null;

            private static final AnnotationVisitor IGNORE_ANNOTATION = null;

            private final ClassFileLocator classFileLocator;

            private final TypeDescription targetType;

            private final MethodRebaseResolver methodRebaseResolver;

            public ForInlining(TypeDescription instrumentedType,
                               LoadedTypeInitializer loadedTypeInitializer,
                               InstrumentedType.TypeInitializer typeInitializer,
                               List<DynamicType> explicitAuxiliaryTypes,
                               ClassFileVersion classFileVersion,
                               AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                               ClassVisitorWrapper classVisitorWrapper,
                               TypeAttributeAppender attributeAppender,
                               FieldPool fieldPool,
                               MethodPool methodPool,
                               MethodList invokeableMethods,
                               ClassFileLocator classFileLocator,
                               TypeDescription targetType,
                               MethodRebaseResolver methodRebaseResolver) {
                super(instrumentedType,
                        loadedTypeInitializer,
                        typeInitializer,
                        explicitAuxiliaryTypes,
                        classFileVersion,
                        auxiliaryTypeNamingStrategy,
                        classVisitorWrapper,
                        attributeAppender,
                        fieldPool,
                        methodPool,
                        invokeableMethods);
                this.classFileLocator = classFileLocator;
                this.targetType = targetType;
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
            private byte[] doCreate(Instrumentation.Context.ExtractableView instrumentationContext, byte[] binaryRepresentation) {
                ClassReader classReader = new ClassReader(binaryRepresentation);
                ClassWriter classWriter = new ClassWriter(classReader, ASM_MANUAL_FLAG);
                classReader.accept(writeTo(classVisitorWrapper.wrap(classWriter), instrumentationContext), ASM_MANUAL_FLAG);
                return classWriter.toByteArray();
            }

            /**
             * Creates a class visitor which weaves all changes and additions on the fly.
             *
             * @param classVisitor           The class visitor to which this entry is to be written to.
             * @param instrumentationContext The instrumentation context to use for implementing the class file.
             * @return A class visitor which is capable of applying the changes.
             */
            private ClassVisitor writeTo(ClassVisitor classVisitor, Instrumentation.Context.ExtractableView instrumentationContext) {
                String originalName = targetType.getInternalName();
                String targetName = instrumentedType.getInternalName();
                ClassVisitor targetClassVisitor = new RedefinitionClassVisitor(classVisitor, instrumentationContext);
                return originalName.equals(targetName)
                        ? targetClassVisitor
                        : new RemappingClassAdapter(targetClassVisitor, new SimpleRemapper(originalName, targetName));
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
                protected RedefinitionClassVisitor(ClassVisitor classVisitor, Instrumentation.Context.ExtractableView instrumentationContext) {
                    super(ASM_API_VERSION, classVisitor);
                    this.instrumentationContext = instrumentationContext;
                    List<? extends FieldDescription> fieldDescriptions = instrumentedType.getDeclaredFields();
                    declaredFields = new HashMap<String, FieldDescription>(fieldDescriptions.size());
                    for (FieldDescription fieldDescription : fieldDescriptions) {
                        declaredFields.put(fieldDescription.getInternalName(), fieldDescription);
                    }
                    declarableMethods = new HashMap<String, MethodDescription>(invokeableMethods.size());
                    for (MethodDescription methodDescription : invokeableMethods) {
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
                protected MethodVisitor redefine(MethodDescription methodDescription, boolean abstractOrigin) {
                    TypeWriter.MethodPool.Entry entry = methodPool.target(methodDescription);
                    if (!entry.getSort().isDefined()) {
                        return super.visitMethod(methodDescription.getModifiers(),
                                methodDescription.getInternalName(),
                                methodDescription.getDescriptor(),
                                methodDescription.getGenericSignature(),
                                methodDescription.getExceptionTypes().toInternalNames());
                    }
                    MethodVisitor methodVisitor = super.visitMethod(
                            methodDescription.getAdjustedModifiers(entry.getSort().isImplemented()),
                            methodDescription.getInternalName(),
                            methodDescription.getDescriptor(),
                            methodDescription.getGenericSignature(),
                            methodDescription.getExceptionTypes().toInternalNames());
                    return abstractOrigin
                            ? new AttributeObtainingMethodVisitor(methodVisitor, entry, methodDescription)
                            : new CodePreservingMethodVisitor(methodVisitor, entry, methodDescription);
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
                protected class CodePreservingMethodVisitor extends MethodVisitor {

                    /**
                     * The method visitor of the actual method.
                     */
                    private final MethodVisitor actualMethodVisitor;

                    private final MethodPool.Entry entry;

                    /**
                     * A description of the actual method.
                     */
                    private final MethodDescription methodDescription;

                    /**
                     * The resolution of a potential rebased method.
                     */
                    private final MethodRebaseResolver.Resolution resolution;

                    protected CodePreservingMethodVisitor(MethodVisitor actualMethodVisitor,
                                                          MethodPool.Entry entry,
                                                          MethodDescription methodDescription) {
                        super(ASM_API_VERSION, actualMethodVisitor);
                        this.actualMethodVisitor = actualMethodVisitor;
                        this.entry = entry;
                        this.methodDescription = methodDescription;
                        this.resolution = methodRebaseResolver.resolve(methodDescription);
                        entry.applyHead(actualMethodVisitor, methodDescription);
                    }

                    @Override
                    public AnnotationVisitor visitAnnotationDefault() {
                        return IGNORE_ANNOTATION; // Annotation types can never be rebased.
                    }

                    @Override
                    public void visitCode() {
                        entry.applyBody(actualMethodVisitor, instrumentationContext, methodDescription);
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
                }

                /**
                 * A method visitor that obtains all attributes and annotations of a method that is found in the
                 * class file but discards all code.
                 */
                protected class AttributeObtainingMethodVisitor extends MethodVisitor {

                    /**
                     * The method visitor to which the actual method is to be written to.
                     */
                    private final MethodVisitor actualMethodVisitor;

                    private final MethodPool.Entry entry;

                    /**
                     * A description of the method that is currently written.
                     */
                    private final MethodDescription methodDescription;

                    protected AttributeObtainingMethodVisitor(MethodVisitor actualMethodVisitor,
                                                              MethodPool.Entry entry,
                                                              MethodDescription methodDescription) {
                        super(ASM_API_VERSION, actualMethodVisitor);
                        this.actualMethodVisitor = actualMethodVisitor;
                        this.entry = entry;
                        this.methodDescription = methodDescription;
                        entry.applyHead(actualMethodVisitor, methodDescription);
                    }

                    @Override
                    public AnnotationVisitor visitAnnotationDefault() {
                        return IGNORE_ANNOTATION;
                    }

                    @Override
                    public void visitCode() {
                        mv = IGNORE_METHOD;
                    }

                    @Override
                    public void visitEnd() {
                        entry.applyBody(actualMethodVisitor, instrumentationContext, methodDescription);
                        actualMethodVisitor.visitEnd();
                    }

                    @Override
                    public String toString() {
                        return "TypeWriter.Engine.ForRedefinition.RedefinitionClassVisitor.AttributeObtainingMethodVisitor{" +
                                "actualMethodVisitor=" + actualMethodVisitor +
                                ", entry=" + entry +
                                ", methodDescription=" + methodDescription +
                                '}';
                    }
                }

                /**
                 * A code injection for the type initializer that invokes a method representing the original type initializer
                 * which is copied to a static method.
                 */
                protected class TypeInitializerInjection implements Instrumentation.Context.ExtractableView.InjectedCode {

                    /**
                     * The modifiers for the method that consumes the original type initializer.
                     */
                    private static final int TYPE_INITIALIZER_PROXY_MODIFIERS = Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;

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

        public static class ForCreation<U> extends Default<U> {

            public ForCreation(TypeDescription instrumentedType,
                               LoadedTypeInitializer loadedTypeInitializer,
                               InstrumentedType.TypeInitializer typeInitializer,
                               List<DynamicType> explicitAuxiliaryTypes,
                               ClassFileVersion classFileVersion,
                               AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                               ClassVisitorWrapper classVisitorWrapper,
                               TypeAttributeAppender attributeAppender,
                               FieldPool fieldPool,
                               MethodPool methodPool,
                               MethodList instrumentedMethods) {
                super(instrumentedType,
                        loadedTypeInitializer,
                        typeInitializer,
                        explicitAuxiliaryTypes,
                        classFileVersion,
                        auxiliaryTypeNamingStrategy,
                        classVisitorWrapper,
                        attributeAppender,
                        fieldPool,
                        methodPool,
                        instrumentedMethods);
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
                for (MethodDescription methodDescription : invokeableMethods) {
                    methodPool.target(methodDescription).apply(classVisitor, instrumentationContext, methodDescription);
                }
                instrumentationContext.drain(classVisitor, methodPool, Instrumentation.Context.ExtractableView.InjectedCode.None.INSTANCE);
                classVisitor.visitEnd();
                return classWriter.toByteArray();
            }
        }
    }
}
