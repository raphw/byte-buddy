package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;

/**
 * A type initializer is responsible for defining a type's static initialization block.
 */
public interface TypeInitializer extends ByteCodeAppender.Factory {

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
     * @param byteCodeAppenderFactory The byte code appender factory to apply as the type initializer.
     * @return A defined type initializer.
     */
    TypeInitializer expandWith(ByteCodeAppender.Factory byteCodeAppenderFactory);

    /**
     * Returns this type initializer with an ending return statement. For this to be possible, this type initializer must
     * be defined.
     *
     * @return This type initializer with an ending return statement.
     */
    ByteCodeAppender withReturn(Implementation.Target implementationTarget);

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
        public TypeInitializer expandWith(ByteCodeAppender.Factory byteCodeAppenderFactory) {
            return new Simple(byteCodeAppenderFactory);
        }

        @Override
        public ByteCodeAppender withReturn(Implementation.Target implementationTarget) {
            throw new IllegalStateException("Cannot append return to non-defined type initializer");
        }

        @Override
        public ByteCodeAppender appender(Implementation.Target implementationTarget) {
            throw new IllegalStateException("Cannot apply a non-defined type initializer");
        }

        @Override
        public String toString() {
            return "InstrumentedType.TypeInitializer.None." + name();
        }
    }

    /**
     * A simple, defined type initializer that executes a given {@link ByteCodeAppender}.
     */
    class Simple implements TypeInitializer {

        /**
         * The byte code appender factory to apply as the type initializer.
         */
        private final ByteCodeAppender.Factory byteCodeAppenderFactory;

        /**
         * Creates a new simple type initializer.
         *
         * @param byteCodeAppenderFactory The byte code appender facotry to apply as the type initializer.
         */
        public Simple(ByteCodeAppender.Factory byteCodeAppenderFactory) {
            this.byteCodeAppenderFactory = byteCodeAppenderFactory;
        }

        @Override
        public boolean isDefined() {
            return true;
        }

        @Override
        public TypeInitializer expandWith(ByteCodeAppender.Factory byteCodeAppenderFactory) {
            return new Simple(new Compound(this.byteCodeAppenderFactory, byteCodeAppenderFactory));
        }

        @Override
        public ByteCodeAppender withReturn(Implementation.Target implementationTarget) {
            return new ByteCodeAppender.Compound(byteCodeAppenderFactory.appender(implementationTarget), new ByteCodeAppender.Simple(MethodReturn.VOID));
        }

        @Override
        public ByteCodeAppender appender(Implementation.Target implementationTarget) {
            return byteCodeAppenderFactory.appender(implementationTarget);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && byteCodeAppenderFactory.equals(((Simple) other).byteCodeAppenderFactory);
        }

        @Override
        public int hashCode() {
            return byteCodeAppenderFactory.hashCode();
        }

        @Override
        public String toString() {
            return "InstrumentedType.TypeInitializer.Simple{" +
                    "byteCodeAppenderFactory=" + byteCodeAppenderFactory +
                    '}';
        }
    }
}
