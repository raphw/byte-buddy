package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * A type initializer is responsible for defining a type's static initialization block.
 */
public interface TypeInitializer extends ByteCodeAppender {

    /**
     * Indicates if this type initializer is defined.
     *
     * @return {@code true} if this type initializer is defined.
     */
    boolean isDefined();

    /**
     * Expands this type initializer with another byte code appender. For this to be possible, this type initializer must
     * be defined.
     *
     * @param byteCodeAppender The byte code appender to apply as the type initializer.
     * @return A defined type initializer.
     */
    TypeInitializer expandWith(ByteCodeAppender byteCodeAppender);

    /**
     * Returns this type initializer with an ending return statement. For this to be possible, this type initializer must
     * be defined.
     *
     * @return This type initializer with an ending return statement.
     */
    ByteCodeAppender withReturn();

    interface Drain {

        void apply(ClassVisitor classVisitor, TypeInitializer typeInitializer, Implementation.Context implementationContext);

        class Default implements Drain {

            protected final TypeDescription instrumentedType;

            protected final TypeWriter.MethodPool methodPool;

            protected final AnnotationValueFilter.Factory annotationValueFilterFactory;

            public Default(TypeDescription instrumentedType,
                           TypeWriter.MethodPool methodPool,
                           AnnotationValueFilter.Factory annotationValueFilterFactory) {
                this.instrumentedType = instrumentedType;
                this.methodPool = methodPool;
                this.annotationValueFilterFactory = annotationValueFilterFactory;
            }

            @Override
            public void apply(ClassVisitor classVisitor, TypeInitializer typeInitializer, Implementation.Context implementationContext) {
                MethodDescription typeInitializerMethod = new MethodDescription.Latent.TypeInitializer(instrumentedType);
                TypeWriter.MethodPool.Record initializerRecord = methodPool.target(typeInitializerMethod);
                if (initializerRecord.getSort().isImplemented() && typeInitializer.isDefined()) {
                    initializerRecord = initializerRecord.prepend(typeInitializer); // TODO: Automate?
                } else if (typeInitializer.isDefined()) {
                    initializerRecord = new TypeWriter.MethodPool.Record.ForDefinedMethod.WithBody(typeInitializerMethod, typeInitializer.withReturn());
                }
                initializerRecord.apply(classVisitor, implementationContext, annotationValueFilterFactory);
            }
        }
    }

    /**
     * Canonical implementation of a non-defined type initializer.
     */
    enum None implements TypeInitializer {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public boolean isDefined() {
            return false;
        }

        @Override
        public TypeInitializer expandWith(ByteCodeAppender byteCodeAppenderFactory) {
            return new TypeInitializer.Simple(byteCodeAppenderFactory);
        }

        @Override
        public ByteCodeAppender withReturn() {
            throw new IllegalStateException("Cannot append return to non-defined type initializer");
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
            throw new IllegalStateException("Cannot apply a non-defined type initializer");
        }

        @Override
        public String toString() {
            return "TypeInitializer.None." + name();
        }
    }

    /**
     * A simple, defined type initializer that executes a given {@link ByteCodeAppender}.
     */
    class Simple implements TypeInitializer {

        /**
         * The byte code appender to apply as the type initializer.
         */
        private final ByteCodeAppender byteCodeAppender;

        /**
         * Creates a new simple type initializer.
         *
         * @param byteCodeAppender The byte code appender to apply as the type initializer.
         */
        public Simple(ByteCodeAppender byteCodeAppender) {
            this.byteCodeAppender = byteCodeAppender;
        }

        @Override
        public boolean isDefined() {
            return true;
        }

        @Override
        public TypeInitializer expandWith(ByteCodeAppender byteCodeAppender) {
            return new TypeInitializer.Simple(new Compound(this.byteCodeAppender, byteCodeAppender));
        }

        @Override
        public ByteCodeAppender withReturn() {
            return new ByteCodeAppender.Compound(byteCodeAppender, new ByteCodeAppender.Simple(MethodReturn.VOID));
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
            return byteCodeAppender.apply(methodVisitor, implementationContext, instrumentedMethod);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && byteCodeAppender.equals(((TypeInitializer.Simple) other).byteCodeAppender);
        }

        @Override
        public int hashCode() {
            return byteCodeAppender.hashCode();
        }

        @Override
        public String toString() {
            return "TypeInitializer.Simple{" +
                    "byteCodeAppender=" + byteCodeAppender +
                    '}';
        }
    }
}
