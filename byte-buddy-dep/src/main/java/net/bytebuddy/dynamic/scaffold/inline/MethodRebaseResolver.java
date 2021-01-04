/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodAccessorFactory;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.auxiliary.TrivialType;
import net.bytebuddy.utility.CompoundList;
import org.objectweb.asm.Opcodes;

import java.util.*;

/**
 * A method rebase resolver is responsible for mapping methods of an instrumented type to an alternative signature.
 * This way a method can exist in two versions within a class:
 * <ol>
 * <li>The rebased method which represents the original implementation as it is present in a class file.</li>
 * <li>An overridden method which implements user code which is still able to invoke the original, rebased method.</li>
 * </ol>
 */
public interface MethodRebaseResolver {

    /**
     * Checks if a method is eligible for rebasing and resolves this possibly rebased method.
     *
     * @param methodDescription A description of the method to resolve.
     * @return A resolution for the given method.
     */
    Resolution resolve(MethodDescription.InDefinedShape methodDescription);

    /**
     * Returns a (potentially empty) list of auxiliary types that are required by this method rebase resolver.
     *
     * @return A list of auxiliary types that are required by this method rebase resolver.
     */
    List<DynamicType> getAuxiliaryTypes();

    /**
     * Returns a map of all rebasable methods' signature tokens to their resolution.
     *
     * @return A map of all rebasable methods' signature tokens to their resolution.
     */
    Map<MethodDescription.SignatureToken, Resolution> asTokenMap();

    /**
     * A method rebase resolver that preserves any method in its original form.
     */
    enum Disabled implements MethodRebaseResolver {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public Resolution resolve(MethodDescription.InDefinedShape methodDescription) {
            return new Resolution.Preserved(methodDescription);
        }

        /**
         * {@inheritDoc}
         */
        public List<DynamicType> getAuxiliaryTypes() {
            return Collections.emptyList();
        }

        /**
         * {@inheritDoc}
         */
        public Map<MethodDescription.SignatureToken, Resolution> asTokenMap() {
            return Collections.emptyMap();
        }

    }

    /**
     * A resolution for a method that was checked by a {@link MethodRebaseResolver}.
     */
    interface Resolution {

        /**
         * Checks if this resolution represents a rebased method.
         *
         * @return {@code true} if this resolution requires to rebase a method.
         */
        boolean isRebased();

        /**
         * Returns the resolved method if this resolution represents a rebased method or the original method.
         *
         * @return The resolved method if this resolution represents a rebased method or the original method.
         */
        MethodDescription.InDefinedShape getResolvedMethod();

        /**
         * A rebased method might require additional arguments in order to create a distinct signature.
         *
         * @return A list of prepended method parameters.
         */
        TypeList getPrependedParameters();

        /**
         * A {@link MethodRebaseResolver.Resolution} of a non-rebased method.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Preserved implements Resolution {

            /**
             * The preserved method.
             */
            private final MethodDescription.InDefinedShape methodDescription;

            /**
             * Creates a new {@link MethodRebaseResolver.Resolution} for
             * a non-rebased method.
             *
             * @param methodDescription The preserved method.
             */
            public Preserved(MethodDescription.InDefinedShape methodDescription) {
                this.methodDescription = methodDescription;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isRebased() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public MethodDescription.InDefinedShape getResolvedMethod() {
                return methodDescription;
            }

            /**
             * {@inheritDoc}
             */
            public TypeList getPrependedParameters() {
                throw new IllegalStateException("Cannot process additional parameters for non-rebased method: " + methodDescription);
            }
        }

        /**
         * A {@link MethodRebaseResolver.Resolution} of a rebased method.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForRebasedMethod implements Resolution {

            /**
             * The rebased method.
             */
            private final MethodDescription.InDefinedShape methodDescription;

            /**
             * Creates a resolution for a rebased method.
             *
             * @param methodDescription The rebased method.
             */
            protected ForRebasedMethod(MethodDescription.InDefinedShape methodDescription) {
                this.methodDescription = methodDescription;
            }

            /**
             * Resolves a rebasement for the provided method.
             *
             * @param instrumentedType      The instrumented type.
             * @param methodDescription     The method to be rebased.
             * @param methodNameTransformer The transformer to use for renaming the method.
             * @return A resolution for rebasing the provided method.
             */
            public static Resolution of(TypeDescription instrumentedType,
                                        MethodDescription.InDefinedShape methodDescription,
                                        MethodNameTransformer methodNameTransformer) {
                return new ForRebasedMethod(new RebasedMethod(instrumentedType, methodDescription, methodNameTransformer));
            }

            /**
             * {@inheritDoc}
             */
            public boolean isRebased() {
                return true;
            }

            /**
             * {@inheritDoc}
             */
            public MethodDescription.InDefinedShape getResolvedMethod() {
                return methodDescription;
            }

            /**
             * {@inheritDoc}
             */
            public TypeList getPrependedParameters() {
                return new TypeList.Empty();
            }

            /**
             * A description of a rebased method.
             */
            protected static class RebasedMethod extends MethodDescription.InDefinedShape.AbstractBase {

                /**
                 * The instrumented type.
                 */
                private final TypeDescription instrumentedType;

                /**
                 * The method that is being rebased.
                 */
                private final InDefinedShape methodDescription;

                /**
                 * The transformer to use for renaming the method.
                 */
                private final MethodNameTransformer methodNameTransformer;

                /**
                 * Creates a new rebased method.
                 *
                 * @param instrumentedType      The instrumented type.
                 * @param methodDescription     The method that is being rebased.
                 * @param methodNameTransformer The transformer to use for renaming the method.
                 */
                protected RebasedMethod(TypeDescription instrumentedType, InDefinedShape methodDescription, MethodNameTransformer methodNameTransformer) {
                    this.instrumentedType = instrumentedType;
                    this.methodDescription = methodDescription;
                    this.methodNameTransformer = methodNameTransformer;
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription.Generic getReturnType() {
                    return methodDescription.getReturnType().asRawType();
                }

                /**
                 * {@inheritDoc}
                 */
                public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                    return new ParameterList.Explicit.ForTypes(this, methodDescription.getParameters().asTypeList().asRawTypes());
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getExceptionTypes() {
                    return methodDescription.getExceptionTypes().asRawTypes();
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationValue<?, ?> getDefaultValue() {
                    return AnnotationValue.UNDEFINED;
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getTypeVariables() {
                    return new TypeList.Generic.Empty();
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return new AnnotationList.Empty();
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription getDeclaringType() {
                    return methodDescription.getDeclaringType();
                }

                /**
                 * {@inheritDoc}
                 */
                public int getModifiers() {
                    return Opcodes.ACC_SYNTHETIC
                            | (methodDescription.isStatic() ? Opcodes.ACC_STATIC : EMPTY_MASK)
                            | (methodDescription.isNative() ? Opcodes.ACC_NATIVE | Opcodes.ACC_FINAL : EMPTY_MASK)
                            | (instrumentedType.isInterface() && !methodDescription.isNative() ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE);
                }

                /**
                 * {@inheritDoc}
                 */
                public String getInternalName() {
                    return methodNameTransformer.transform(methodDescription);
                }
            }
        }

        /**
         * A {@link MethodRebaseResolver.Resolution} of a rebased constructor.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForRebasedConstructor implements Resolution {

            /**
             * The rebased constructor.
             */
            private final MethodDescription.InDefinedShape methodDescription;

            /**
             * The placeholder type that is prepended to the constructor signature.
             */
            private final TypeDescription placeholderType;

            /**
             * Creates a new resolution for a rebased constructor.
             *
             * @param methodDescription The rebased constructor.
             * @param placeholderType   The placeholder type that is prepended to the constructor signature.
             */
            protected ForRebasedConstructor(MethodDescription.InDefinedShape methodDescription, TypeDescription placeholderType) {
                this.methodDescription = methodDescription;
                this.placeholderType = placeholderType;
            }

            /**
             * Resolves a constructor rebasement.
             *
             * @param methodDescription The constructor to rebase.
             * @param placeholderType   The placeholder type to use to distinguish the constructor's signature.
             * @return A resolution of the provided constructor.
             */
            public static Resolution of(MethodDescription.InDefinedShape methodDescription, TypeDescription placeholderType) {
                return new ForRebasedConstructor(new RebasedConstructor(methodDescription, placeholderType), placeholderType);
            }

            /**
             * {@inheritDoc}
             */
            public boolean isRebased() {
                return true;
            }

            /**
             * {@inheritDoc}
             */
            public MethodDescription.InDefinedShape getResolvedMethod() {
                return methodDescription;
            }

            /**
             * {@inheritDoc}
             */
            public TypeList getPrependedParameters() {
                return new TypeList.Explicit(placeholderType);
            }

            /**
             * An description of a rebased constructor.
             */
            protected static class RebasedConstructor extends MethodDescription.InDefinedShape.AbstractBase {

                /**
                 * The constructor that is rebased.
                 */
                private final InDefinedShape methodDescription;

                /**
                 * The placeholder type that is used to distinguish the constructor's signature.
                 */
                private final TypeDescription placeholderType;

                /**
                 * Creates a new rebased constructor.
                 *
                 * @param methodDescription The constructor that is rebased.
                 * @param placeholderType   The placeholder type that is used to distinguish the constructor's signature.
                 */
                protected RebasedConstructor(InDefinedShape methodDescription, TypeDescription placeholderType) {
                    this.methodDescription = methodDescription;
                    this.placeholderType = placeholderType;
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription.Generic getReturnType() {
                    return TypeDescription.Generic.VOID;
                }

                /**
                 * {@inheritDoc}
                 */
                public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                    return new ParameterList.Explicit.ForTypes(this, CompoundList.of(methodDescription.getParameters().asTypeList().asErasures(), placeholderType));
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getExceptionTypes() {
                    return methodDescription.getExceptionTypes().asRawTypes();
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationValue<?, ?> getDefaultValue() {
                    return AnnotationValue.UNDEFINED;
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getTypeVariables() {
                    return new TypeList.Generic.Empty();
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return new AnnotationList.Empty();
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription getDeclaringType() {
                    return methodDescription.getDeclaringType();
                }

                /**
                 * {@inheritDoc}
                 */
                public int getModifiers() {
                    return Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PRIVATE;
                }

                /**
                 * {@inheritDoc}
                 */
                public String getInternalName() {
                    return MethodDescription.CONSTRUCTOR_INTERNAL_NAME;
                }
            }
        }
    }

    /**
     * A default implementation of a method rebase resolver.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Default implements MethodRebaseResolver {

        /**
         * A mapping of rebased methods to their existing resolutions.
         */
        private final Map<MethodDescription.InDefinedShape, Resolution> resolutions;

        /**
         * A list of dynamic types that need to be appended to the created type in order to allow for the rebasement.
         */
        private final List<DynamicType> dynamicTypes;

        /**
         * Creates a new default method rebased resolver.
         *
         * @param resolutions  A mapping of rebased methods to their existing resolutions.
         * @param dynamicTypes A list of dynamic types that need to be appended to the created type in order to allow for the rebasement.
         */
        protected Default(Map<MethodDescription.InDefinedShape, Resolution> resolutions, List<DynamicType> dynamicTypes) {
            this.resolutions = resolutions;
            this.dynamicTypes = dynamicTypes;
        }

        /**
         * Creates a new method rebase resolver.
         *
         * @param instrumentedType            The instrumented type.
         * @param rebaseables      Tokens describing all methods that can possibly be rebased.
         * @param classFileVersion            The class file version for the instrumentation.
         * @param auxiliaryTypeNamingStrategy The naming strategy for naming a potential auxiliary type.
         * @param methodNameTransformer       A transformer for method names.
         * @return A method rebase resolver that is capable of rebasing any of the provided methods.
         */
        public static MethodRebaseResolver make(TypeDescription instrumentedType,
                                                Set<? extends MethodDescription.SignatureToken> rebaseables,
                                                ClassFileVersion classFileVersion,
                                                AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                MethodNameTransformer methodNameTransformer) {
            DynamicType placeholderType = null;
            Map<MethodDescription.InDefinedShape, Resolution> resolutions = new HashMap<MethodDescription.InDefinedShape, Resolution>();
            for (MethodDescription.InDefinedShape instrumentedMethod : instrumentedType.getDeclaredMethods()) {
                if (rebaseables.contains(instrumentedMethod.asSignatureToken())) {
                    Resolution resolution;
                    if (instrumentedMethod.isConstructor()) {
                        if (placeholderType == null) {
                            placeholderType = TrivialType.SIGNATURE_RELEVANT.make(auxiliaryTypeNamingStrategy.name(instrumentedType),
                                    classFileVersion,
                                    MethodAccessorFactory.Illegal.INSTANCE);
                        }
                        resolution = Resolution.ForRebasedConstructor.of(instrumentedMethod, placeholderType.getTypeDescription());
                    } else {
                        resolution = Resolution.ForRebasedMethod.of(instrumentedType, instrumentedMethod, methodNameTransformer);
                    }
                    resolutions.put(instrumentedMethod, resolution);
                }
            }
            return placeholderType == null
                    ? new Default(resolutions, Collections.<DynamicType>emptyList())
                    : new Default(resolutions, Collections.singletonList(placeholderType));
        }

        /**
         * {@inheritDoc}
         */
        public Resolution resolve(MethodDescription.InDefinedShape methodDescription) {
            Resolution resolution = resolutions.get(methodDescription);
            return resolution == null
                    ? new Resolution.Preserved(methodDescription)
                    : resolution;
        }

        /**
         * {@inheritDoc}
         */
        public List<DynamicType> getAuxiliaryTypes() {
            return dynamicTypes;
        }

        /**
         * {@inheritDoc}
         */
        public Map<MethodDescription.SignatureToken, Resolution> asTokenMap() {
            Map<MethodDescription.SignatureToken, Resolution> tokenMap = new HashMap<MethodDescription.SignatureToken, Resolution>();
            for (Map.Entry<MethodDescription.InDefinedShape, Resolution> entry : resolutions.entrySet()) {
                tokenMap.put(entry.getKey().asSignatureToken(), entry.getValue());
            }
            return tokenMap;
        }
    }
}
