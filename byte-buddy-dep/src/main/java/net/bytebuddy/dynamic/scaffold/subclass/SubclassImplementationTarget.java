/*
 * Copyright 2014 - 2018 Rafael Winterhalter
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
package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.Implementation;

import static net.bytebuddy.matcher.ElementMatchers.hasSignature;
import static net.bytebuddy.matcher.ElementMatchers.isVisibleTo;

/**
 * An implementation target for creating a subclass of a given type.
 */
@HashCodeAndEqualsPlugin.Enhance
public class SubclassImplementationTarget extends Implementation.Target.AbstractBase {

    /**
     * The origin type identifier to use.
     */
    protected final OriginTypeResolver originTypeResolver;

    /**
     * Creates a new subclass implementation target.
     *
     * @param instrumentedType        The instrumented type.
     * @param methodGraph             A method graph of the instrumented type.
     * @param defaultMethodInvocation The default method invocation mode to apply.
     * @param originTypeResolver      A resolver for the origin type.
     */
    protected SubclassImplementationTarget(TypeDescription instrumentedType,
                                           MethodGraph.Linked methodGraph,
                                           DefaultMethodInvocation defaultMethodInvocation,
                                           OriginTypeResolver originTypeResolver) {
        super(instrumentedType, methodGraph, defaultMethodInvocation);
        this.originTypeResolver = originTypeResolver;
    }

    /**
     * {@inheritDoc}
     */
    public Implementation.SpecialMethodInvocation invokeSuper(MethodDescription.SignatureToken token) {
        return token.getName().equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME)
                ? invokeConstructor(token)
                : invokeMethod(token);
    }

    /**
     * Resolves a special method invocation for a constructor invocation.
     *
     * @param token A token describing the constructor to be invoked.
     * @return A special method invocation for a constructor representing the given method token, if available.
     */
    private Implementation.SpecialMethodInvocation invokeConstructor(MethodDescription.SignatureToken token) {
        TypeDescription.Generic superClass = instrumentedType.getSuperClass();
        MethodList<?> candidates = superClass == null
                ? new MethodList.Empty<MethodDescription.InGenericShape>()
                : superClass.getDeclaredMethods().filter(hasSignature(token).and(isVisibleTo(instrumentedType)));
        return candidates.size() == 1
                ? Implementation.SpecialMethodInvocation.Simple.of(candidates.getOnly(), instrumentedType.getSuperClass().asErasure())
                : Implementation.SpecialMethodInvocation.Illegal.INSTANCE;
    }

    /**
     * Resolves a special method invocation for a non-constructor invocation.
     *
     * @param token A token describing the method to be invoked.
     * @return A special method invocation for a method representing the given method token, if available.
     */
    private Implementation.SpecialMethodInvocation invokeMethod(MethodDescription.SignatureToken token) {
        MethodGraph.Node methodNode = methodGraph.getSuperClassGraph().locate(token);
        return methodNode.getSort().isUnique()
                ? Implementation.SpecialMethodInvocation.Simple.of(methodNode.getRepresentative(), instrumentedType.getSuperClass().asErasure())
                : Implementation.SpecialMethodInvocation.Illegal.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    public TypeDefinition getOriginType() {
        return originTypeResolver.identify(instrumentedType);
    }

    /**
     * Responsible for identifying the origin type that an implementation target represents when
     * {@link Implementation.Target#getOriginType()} is invoked.
     */
    public enum OriginTypeResolver {

        /**
         * Identifies the super type of an instrumented type as the origin class.
         */
        SUPER_CLASS {
            @Override
            protected TypeDefinition identify(TypeDescription typeDescription) {
                return typeDescription.getSuperClass();
            }
        },

        /**
         * Identifies the instrumented type as its own origin type.
         */
        LEVEL_TYPE {
            @Override
            protected TypeDefinition identify(TypeDescription typeDescription) {
                return typeDescription;
            }
        };

        /**
         * Identifies the origin type to a given type description.
         *
         * @param typeDescription The type description for which an origin type should be identified.
         * @return The origin type to the given type description.
         */
        protected abstract TypeDefinition identify(TypeDescription typeDescription);
    }

    /**
     * A factory for creating a {@link net.bytebuddy.dynamic.scaffold.subclass.SubclassImplementationTarget}.
     */
    public enum Factory implements Implementation.Target.Factory {

        /**
         * A factory creating a subclass implementation target with a {@link OriginTypeResolver#SUPER_CLASS}.
         */
        SUPER_CLASS(OriginTypeResolver.SUPER_CLASS),

        /**
         * A factory creating a subclass implementation target with a {@link OriginTypeResolver#LEVEL_TYPE}.
         */
        LEVEL_TYPE(OriginTypeResolver.LEVEL_TYPE);

        /**
         * The origin type resolver that this factory hands to the created {@link SubclassImplementationTarget}.
         */
        private final OriginTypeResolver originTypeResolver;

        /**
         * Creates a new factory.
         *
         * @param originTypeResolver The origin type resolver that this factory hands to the created {@link SubclassImplementationTarget}.
         */
        Factory(OriginTypeResolver originTypeResolver) {
            this.originTypeResolver = originTypeResolver;
        }

        /**
         * {@inheritDoc}
         */
        public Implementation.Target make(TypeDescription instrumentedType, MethodGraph.Linked methodGraph, ClassFileVersion classFileVersion) {
            return new SubclassImplementationTarget(instrumentedType, methodGraph, DefaultMethodInvocation.of(classFileVersion), originTypeResolver);
        }
    }
}
