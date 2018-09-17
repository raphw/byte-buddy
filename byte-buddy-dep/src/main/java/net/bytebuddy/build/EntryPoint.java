package net.bytebuddy.build;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.implementation.Implementation;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * An entry point for a build tool which is responsible for the transformation's configuration.
 */
public interface EntryPoint {

    /**
     * Returns the Byte Buddy instance to use.
     *
     * @param classFileVersion The class file version in which to represent class files.
     * @return The Byte Buddy instance to use.
     */
    ByteBuddy byteBuddy(ClassFileVersion classFileVersion);

    /**
     * Applies a transformation.
     *
     * @param typeDescription       The type to transform.
     * @param byteBuddy             The Byte Buddy instance to use.
     * @param classFileLocator      The class file locator to use.
     * @param methodNameTransformer The Method name transformer to use.
     * @return A builder for the dynamic type to create.
     */
    DynamicType.Builder<?> transform(TypeDescription typeDescription,
                                     ByteBuddy byteBuddy,
                                     ClassFileLocator classFileLocator,
                                     MethodNameTransformer methodNameTransformer);

    /**
     * Default implementations for an entry point.
     */
    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "An enumeration does not serialize fields")
    enum Default implements EntryPoint {

        /**
         * An entry point that rebases a type.
         */
        REBASE {
            /** {@inheritDoc} */
            public ByteBuddy byteBuddy(ClassFileVersion classFileVersion) {
                return new ByteBuddy(classFileVersion);
            }

            /** {@inheritDoc} */
            public DynamicType.Builder<?> transform(TypeDescription typeDescription,
                                                    ByteBuddy byteBuddy,
                                                    ClassFileLocator classFileLocator,
                                                    MethodNameTransformer methodNameTransformer) {
                return byteBuddy.rebase(typeDescription, classFileLocator, methodNameTransformer);
            }
        },

        /**
         * An entry point that redefines a type.
         */
        REDEFINE {
            /** {@inheritDoc} */
            public ByteBuddy byteBuddy(ClassFileVersion classFileVersion) {
                return new ByteBuddy(classFileVersion);
            }

            /** {@inheritDoc} */
            public DynamicType.Builder<?> transform(TypeDescription typeDescription,
                                                    ByteBuddy byteBuddy,
                                                    ClassFileLocator classFileLocator,
                                                    MethodNameTransformer methodNameTransformer) {
                return byteBuddy.redefine(typeDescription, classFileLocator);
            }
        },

        /**
         * An entry point that redefines a type and which does not change the dynamic type's shape, i.e. does
         * not add any methods or considers intercepting inherited methods.
         */
        REDEFINE_LOCAL {
            /** {@inheritDoc} */
            public ByteBuddy byteBuddy(ClassFileVersion classFileVersion) {
                return new ByteBuddy(classFileVersion).with(Implementation.Context.Disabled.Factory.INSTANCE);
            }

            /** {@inheritDoc} */
            public DynamicType.Builder<?> transform(TypeDescription typeDescription,
                                                    ByteBuddy byteBuddy,
                                                    ClassFileLocator classFileLocator,
                                                    MethodNameTransformer methodNameTransformer) {
                return byteBuddy.redefine(typeDescription, classFileLocator).ignoreAlso(not(isDeclaredBy(typeDescription)));
            }
        },

        /**
         * An entry point that decorates a type and which only offers limited support for transformation by only allowing
         * for the application of {@link net.bytebuddy.asm.AsmVisitorWrapper}s while improving performance.
         */
        DECORATE {
            /** {@inheritDoc} */
            public ByteBuddy byteBuddy(ClassFileVersion classFileVersion) {
                return new ByteBuddy(classFileVersion)
                        .with(MethodGraph.Compiler.ForDeclaredMethods.INSTANCE)
                        .with(Implementation.Context.Disabled.Factory.INSTANCE);
            }

            /** {@inheritDoc} */
            public DynamicType.Builder<?> transform(TypeDescription typeDescription,
                                                    ByteBuddy byteBuddy,
                                                    ClassFileLocator classFileLocator,
                                                    MethodNameTransformer methodNameTransformer) {
                return byteBuddy.decorate(typeDescription, classFileLocator);
            }
        }
    }
}
