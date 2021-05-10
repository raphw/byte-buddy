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
package net.bytebuddy.implementation.bytecode.assign.reference;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;

import java.util.*;

/**
 * <p>
 * An assigner implementation that considers generic type assignability.
 * </p>
 * <p>
 * <b>Important</b>: This implementation does not currently support method variables and their type inference.
 * </p>
 */
public enum GenericTypeAwareAssigner implements Assigner {

    /**
     * The singleton instance.
     */
    INSTANCE;

    @Override
    public StackManipulation assign(TypeDescription.Generic source, TypeDescription.Generic target, Typing typing) {
        if (source.isPrimitive() || target.isPrimitive()) {
            return source.equals(target)
                    ? StackManipulation.Trivial.INSTANCE
                    : StackManipulation.Illegal.INSTANCE;
        } else if (source.accept(new IsAssignableToVisitor(target))) {
            return StackManipulation.Trivial.INSTANCE;
        } else if (typing.isDynamic()) {
            return source.asErasure().isAssignableTo(target.asErasure())
                    ? StackManipulation.Trivial.INSTANCE
                    : TypeCasting.to(target);
        } else {
            return StackManipulation.Illegal.INSTANCE;
        }
    }

    /**
     * A visitor for generic types that determines assignability of such types.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class IsAssignableToVisitor implements TypeDescription.Generic.Visitor<Boolean> {

        /**
         * The type to which another type is being assigned.
         */
        private final TypeDescription.Generic typeDescription;

        /**
         * {@code true} if the assignment is polymorphic.
         */
        private final boolean polymorphic;

        /**
         * Creates a new visitor to determine assignability of the supplied type.
         *
         * @param typeDescription The type to which another type is being assigned.
         */
        public IsAssignableToVisitor(TypeDescription.Generic typeDescription) {
            this(typeDescription, true);
        }

        /**
         * Creates a new visitor to determine assignability of the supplied type.
         *
         * @param typeDescription The type to which another type is being assigned.
         * @param polymorphic     {@code true} if the assignment is polymorphic.
         */
        protected IsAssignableToVisitor(TypeDescription.Generic typeDescription, boolean polymorphic) {
            this.typeDescription = typeDescription;
            this.polymorphic = polymorphic;
        }

        /**
         * {@inheritDoc}
         */
        public Boolean onGenericArray(TypeDescription.Generic genericArray) {
            return typeDescription.accept(new OfGenericArray(genericArray, polymorphic));
        }

        /**
         * {@inheritDoc}
         */
        public Boolean onWildcard(TypeDescription.Generic wildcard) {
            return typeDescription.accept(new OfWildcard(wildcard));
        }

        /**
         * {@inheritDoc}
         */
        public Boolean onParameterizedType(TypeDescription.Generic parameterizedType) {
            return typeDescription.accept(new OfParameterizedType(parameterizedType, polymorphic));
        }

        /**
         * {@inheritDoc}
         */
        public Boolean onTypeVariable(TypeDescription.Generic typeVariable) {
            if (typeVariable.getTypeVariableSource().isInferrable()) {
                throw new UnsupportedOperationException("Assignability checks for type variables declared by methods are not currently supported");
            } else if (typeVariable.equals(typeDescription)) {
                return true;
            } else if (polymorphic) {
                Queue<TypeDescription.Generic> candidates = new LinkedList<TypeDescription.Generic>(typeVariable.getUpperBounds());
                while (!candidates.isEmpty()) {
                    TypeDescription.Generic candidate = candidates.remove();
                    if (candidate.accept(new IsAssignableToVisitor(typeDescription))) {
                        return true;
                    } else if (candidate.getSort().isTypeVariable()) {
                        candidates.addAll(candidate.getUpperBounds());
                    }
                }
                return false;
            } else {
                return false;
            }
        }

        /**
         * {@inheritDoc}
         */
        public Boolean onNonGenericType(TypeDescription.Generic typeDescription) {
            return this.typeDescription.accept(new OfNonGenericType(typeDescription, polymorphic));
        }

        /**
         * An implementation of a assignability visitor that is applicable for any non-wildcard type.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected abstract static class OfManifestType implements TypeDescription.Generic.Visitor<Boolean> {

            /**
             * The type being assigned to another type.
             */
            protected final TypeDescription.Generic typeDescription;

            /**
             * {@code true} if the assignment is polymorphic.
             */
            protected final boolean polymorphic;

            /**
             * Creates a new visitor for a manifest type.
             *
             * @param typeDescription The type being assigned to another type.
             * @param polymorphic     {@code true} if the assignment is polymorphic.
             */
            protected OfManifestType(TypeDescription.Generic typeDescription, boolean polymorphic) {
                this.typeDescription = typeDescription;
                this.polymorphic = polymorphic;
            }

            /**
             * {@inheritDoc}
             */
            public Boolean onWildcard(TypeDescription.Generic wildcard) {
                for (TypeDescription.Generic upperBound : wildcard.getUpperBounds()) {
                    if (!typeDescription.accept(new IsAssignableToVisitor(upperBound))) {
                        return false;
                    }
                }
                for (TypeDescription.Generic lowerBound : wildcard.getLowerBounds()) {
                    if (!lowerBound.accept(new IsAssignableToVisitor(typeDescription))) {
                        return false;
                    }
                }
                return true;
            }

            /**
             * {@inheritDoc}
             */
            public Boolean onTypeVariable(TypeDescription.Generic typeVariable) {
                if (typeVariable.getTypeVariableSource().isInferrable()) {
                    throw new UnsupportedOperationException("Assignability checks for type variables declared by methods arel not currently supported");
                } else {
                    return false;
                }
            }
        }

        /**
         * A visitor for determining assignability of a type in a type hierarchy, i.e. a non-generic or parameterized type.
         */
        protected abstract static class OfSimpleType extends OfManifestType {

            /**
             * Creates a new visitor.
             *
             * @param typeDescription The type being assigned to another type.
             * @param polymorphic     {@code true} if the assignment is polymorphic.
             */
            protected OfSimpleType(TypeDescription.Generic typeDescription, boolean polymorphic) {
                super(typeDescription, polymorphic);
            }

            /**
             * {@inheritDoc}
             */
            public Boolean onParameterizedType(TypeDescription.Generic parameterizedType) {
                Queue<TypeDescription.Generic> candidates = new LinkedList<TypeDescription.Generic>(Collections.singleton(typeDescription));
                Set<TypeDescription> previous = new HashSet<TypeDescription>(Collections.singleton(typeDescription.asErasure()));
                do {
                    TypeDescription.Generic candidate = candidates.remove();
                    if (candidate.asErasure().equals(parameterizedType.asErasure())) {
                        if (candidate.getSort().isNonGeneric()) {
                            return true;
                        } else /* if (candidate.getSort().isParameterized() */ {
                            TypeList.Generic source = candidate.getTypeArguments(), target = parameterizedType.getTypeArguments();
                            int size = target.size();
                            if (source.size() != size) {
                                return false;
                            }
                            for (int index = 0; index < size; index++) {
                                if (!source.get(index).accept(new IsAssignableToVisitor(target.get(index), false))) {
                                    return false;
                                }
                            }
                            TypeDescription.Generic ownerType = parameterizedType.getOwnerType();
                            return ownerType == null || ownerType.accept(new IsAssignableToVisitor(parameterizedType.getOwnerType()));
                        }
                    } else if (polymorphic) {
                        TypeDescription.Generic superClass = candidate.getSuperClass();
                        if (superClass != null && previous.add(superClass.asErasure())) {
                            candidates.add(superClass);
                        }
                        for (TypeDescription.Generic anInterface : candidate.getInterfaces()) {
                            if (previous.add(anInterface.asErasure())) {
                                candidates.add(anInterface);
                            }
                        }
                    }
                } while (!candidates.isEmpty());
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public Boolean onNonGenericType(TypeDescription.Generic typeDescription) {
                return polymorphic
                        ? this.typeDescription.asErasure().isAssignableTo(typeDescription.asErasure())
                        : this.typeDescription.asErasure().equals(typeDescription.asErasure());
            }
        }

        /**
         * A visitor for determining assignability of a generic array type.
         */
        protected static class OfGenericArray extends OfManifestType {

            /**
             * Creates a new visitor.
             *
             * @param typeDescription The type being assigned to another type.
             * @param polymorphic     {@code true} if the assignment is polymorphic.
             */
            protected OfGenericArray(TypeDescription.Generic typeDescription, boolean polymorphic) {
                super(typeDescription, polymorphic);
            }

            /**
             * {@inheritDoc}
             */
            public Boolean onGenericArray(TypeDescription.Generic genericArray) {
                TypeDescription.Generic source = typeDescription.getComponentType(), target = genericArray.getComponentType();
                while (source.getSort().isGenericArray() && target.getSort().isGenericArray()) {
                    source = source.getComponentType();
                    target = target.getComponentType();
                }
                return !source.getSort().isGenericArray() && !target.getSort().isGenericArray() && source.accept(new IsAssignableToVisitor(target));
            }

            /**
             * {@inheritDoc}
             */
            public Boolean onParameterizedType(TypeDescription.Generic parameterizedType) {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public Boolean onNonGenericType(TypeDescription.Generic typeDescription) {
                return polymorphic
                        ? this.typeDescription.asErasure().isAssignableTo(typeDescription.asErasure())
                        : this.typeDescription.asErasure().equals(typeDescription.asErasure());
            }
        }

        /**
         * A visitor to determine the assignability of a wildcard type.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class OfWildcard implements TypeDescription.Generic.Visitor<Boolean> {

            /**
             * The wildcard type being assigned to another type.
             */
            private final TypeDescription.Generic wildcard;

            /**
             * Creates a visitor for a wildcard type assignment.
             *
             * @param wildcard The wildcard type being assigned to another type.
             */
            protected OfWildcard(TypeDescription.Generic wildcard) {
                this.wildcard = wildcard;
            }

            /**
             * {@inheritDoc}
             */
            public Boolean onGenericArray(TypeDescription.Generic genericArray) {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public Boolean onWildcard(TypeDescription.Generic wildcard) {
                boolean hasUpperBounds = false, hasLowerBounds = false;
                for (TypeDescription.Generic target : wildcard.getUpperBounds()) {
                    for (TypeDescription.Generic source : this.wildcard.getUpperBounds()) {
                        if (!source.accept(new IsAssignableToVisitor(target))) {
                            return false;
                        }
                    }
                    hasUpperBounds = hasUpperBounds || !target.represents(Object.class);
                }
                for (TypeDescription.Generic target : wildcard.getLowerBounds()) {
                    for (TypeDescription.Generic source : this.wildcard.getLowerBounds()) {
                        if (!target.accept(new IsAssignableToVisitor(source))) {
                            return false;
                        }
                    }
                    hasLowerBounds = true;
                }
                if (hasUpperBounds) {
                    return this.wildcard.getLowerBounds().isEmpty();
                } else if (hasLowerBounds) {
                    TypeList.Generic upperBounds = this.wildcard.getUpperBounds();
                    return upperBounds.size() == 0 || upperBounds.size() == 1 && upperBounds.getOnly().represents(Object.class);
                } else {
                    return true;
                }
            }

            /**
             * {@inheritDoc}
             */
            public Boolean onParameterizedType(TypeDescription.Generic parameterizedType) {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public Boolean onTypeVariable(TypeDescription.Generic typeVariable) {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public Boolean onNonGenericType(TypeDescription.Generic typeDescription) {
                return false;
            }
        }

        /**
         * A visitor for determining the assignability of a parameterized type.
         */
        protected static class OfParameterizedType extends OfSimpleType {

            /**
             * Creates a new visitor.
             *
             * @param typeDescription The type being assigned to another type.
             * @param polymorphic     {@code true} if the assignment is polymorphic.
             */
            protected OfParameterizedType(TypeDescription.Generic typeDescription, boolean polymorphic) {
                super(typeDescription, polymorphic);
            }

            /**
             * {@inheritDoc}
             */
            public Boolean onGenericArray(TypeDescription.Generic genericArray) {
                return false;
            }
        }

        /**
         * A visitor for determining assignability of a non-generic type.
         */
        protected static class OfNonGenericType extends OfSimpleType {

            /**
             * Creates a new visitor.
             *
             * @param typeDescription The type being assigned to another type.
             * @param polymorphic     {@code true} if the assignment is polymorphic.
             */
            protected OfNonGenericType(TypeDescription.Generic typeDescription, boolean polymorphic) {
                super(typeDescription, polymorphic);
            }

            /**
             * {@inheritDoc}
             */
            public Boolean onGenericArray(TypeDescription.Generic genericArray) {
                return polymorphic
                        ? typeDescription.asErasure().isAssignableTo(genericArray.asErasure())
                        : typeDescription.asErasure().equals(genericArray.asErasure());
            }
        }
    }
}
