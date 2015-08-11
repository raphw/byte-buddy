package net.bytebuddy.description.type.generic;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.matcher.FilterableList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A list containing descriptions of generic types.
 */
public interface GenericTypeList extends FilterableList<GenericTypeDescription, GenericTypeList> {

    /**
     * Returns a list of the generic types' erasures.
     *
     * @return A list of the generic types' erasures.
     */
    TypeList asErasures();

    /**
     * Transforms the generic types by applying the supplied visitor to each of them.
     *
     * @param visitor The visitor to apply to each type.
     * @return A list of the types returned by the supplied visitor.
     */
    GenericTypeList accept(GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor);

    /**
     * Returns the sum of the size of all types contained in this list.
     *
     * @return The sum of the size of all types contained in this list.
     */
    int getStackSize();

    /**
     * An abstract base implementation of a generic type list.
     */
    abstract class AbstractBase extends FilterableList.AbstractBase<GenericTypeDescription, GenericTypeList> implements GenericTypeList {

        @Override
        protected GenericTypeList wrap(List<GenericTypeDescription> values) {
            return new Explicit(values);
        }

        @Override
        public GenericTypeList accept(GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor) {
            List<GenericTypeDescription> visited = new ArrayList<GenericTypeDescription>(size());
            for (GenericTypeDescription genericTypeDescription : this) {
                visited.add(genericTypeDescription.accept(visitor));
            }
            return new Explicit(visited);
        }

        @Override
        public int getStackSize() {
            int stackSize = 0;
            for (GenericTypeDescription genericTypeDescription : this) {
                stackSize += genericTypeDescription.getStackSize().getSize();
            }
            return stackSize;
        }

        @Override
        public TypeList asErasures() {
            List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(size());
            for (GenericTypeDescription genericTypeDescription : this) {
                typeDescriptions.add(genericTypeDescription.asErasure());
            }
            return new TypeList.Explicit(typeDescriptions);
        }
    }

    /**
     * An explicit list of generic types.
     */
    class Explicit extends AbstractBase {

        /**
         * The generic types represented by this list.
         */
        private final List<? extends GenericTypeDescription> genericTypes;

        /**
         * Creates a new explicit list of generic types.
         *
         * @param genericTypes The generic types represented by this list.
         */
        public Explicit(List<? extends GenericTypeDescription> genericTypes) {
            this.genericTypes = genericTypes;
        }

        @Override
        public GenericTypeDescription get(int index) {
            return genericTypes.get(index);
        }

        @Override
        public int size() {
            return genericTypes.size();
        }
    }

    /**
     * A list of loaded generic types.
     */
    class ForLoadedType extends AbstractBase {

        /**
         * The loaded types this list represents.
         */
        private final List<? extends Type> types;

        /**
         * Creates a list of loaded generic types.
         *
         * @param type The loaded types this list represents.
         */
        public ForLoadedType(Type... type) {
            this(Arrays.asList(type));
        }

        /**
         * Creates a list of loaded generic types.
         *
         * @param types The loaded types this list represents.
         */
        public ForLoadedType(List<? extends Type> types) {
            this.types = types;
        }

        @Override
        public GenericTypeDescription get(int index) {
            return GenericTypeDescription.Sort.describe(types.get(index));
        }

        @Override
        public int size() {
            return types.size();
        }
    }

    /**
     * A list of detached types that are attached on reception.
     */
    class ForDetachedTypes extends AbstractBase {

        /**
         * The visitor to use for attaching the detached types.
         */
        private final GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor;

        /**
         * The detached types this list represents.
         */
        private final List<? extends GenericTypeDescription> detachedTypes;

        /**
         * Creates a list of detached types that are attached on reception.
         *
         * @param visitor       The visitor to use for attaching the detached types.
         * @param detachedTypes The detached types this list represents.
         */
        protected ForDetachedTypes(GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor,
                                   List<? extends GenericTypeDescription> detachedTypes) {
            this.visitor = visitor;
            this.detachedTypes = detachedTypes;
        }

        /**
         * Creates a list of types that are attached to the provided type.
         *
         * @param typeDescription The type to which the detached variables are attached to.
         * @param detachedTypes   The detached types.
         * @return A type list representing the detached types being attached to the provided type description.
         */
        public static GenericTypeList attach(TypeDescription typeDescription, List<? extends GenericTypeDescription> detachedTypes) {
            return new ForDetachedTypes(GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(typeDescription), detachedTypes);
        }

        /**
         * Creates a list of types that are attached to the provided field.
         *
         * @param fieldDescription The field to which the detached variables are attached to.
         * @param detachedTypes    The detached types.
         * @return A type list representing the detached types being attached to the provided field description.
         */
        public static GenericTypeList attach(FieldDescription fieldDescription, List<? extends GenericTypeDescription> detachedTypes) {
            return new ForDetachedTypes(GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(fieldDescription), detachedTypes);
        }

        /**
         * Creates a list of types that are attached to the provided method.
         *
         * @param methodDescription The method to which the detached variables are attached to.
         * @param detachedTypes     The detached types.
         * @return A type list representing the detached types being attached to the provided method description.
         */
        public static GenericTypeList attach(MethodDescription methodDescription, List<? extends GenericTypeDescription> detachedTypes) {
            return new ForDetachedTypes(GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(methodDescription), detachedTypes);
        }

        /**
         * Creates a list of types that are attached to the provided parameter.
         *
         * @param parameterDescription The parameter to which the detached variables are attached to.
         * @param detachedTypes        The detached types.
         * @return A type list representing the detached types being attached to the provided parameter description.
         */
        public static GenericTypeList attach(ParameterDescription parameterDescription, List<? extends GenericTypeDescription> detachedTypes) {
            return new ForDetachedTypes(GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(parameterDescription), detachedTypes);
        }

        @Override
        public GenericTypeDescription get(int index) {
            return detachedTypes.get(index).accept(visitor);
        }

        @Override
        public int size() {
            return detachedTypes.size();
        }

        /**
         * A list of type variables that are attached on reception.
         */
        public static class OfTypeVariable extends GenericTypeList.AbstractBase {

            /**
             * The type variable source of the represented type variables.
             */
            private final TypeVariableSource typeVariableSource;

            /**
             * The visitor to use for attaching the represented types.
             */
            private final GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor;

            /**
             * A list of detached types that are attached on reception.
             */
            private final List<? extends GenericTypeDescription> detachedTypes;

            /**
             * Creates a new list of attached type variables.
             *
             * @param typeVariableSource The type variable source of the represented type variables.
             * @param visitor            The visitor to use for attaching the represented types.
             * @param detachedTypes      A list of detached types that are attached on reception.
             */
            protected OfTypeVariable(TypeVariableSource typeVariableSource,
                                     GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor,
                                     List<? extends GenericTypeDescription> detachedTypes) {
                this.typeVariableSource = typeVariableSource;
                this.visitor = visitor;
                this.detachedTypes = detachedTypes;
            }

            /**
             * Creates a list of detached type variables that are attached on reception.
             *
             * @param typeDescription The type of which the type variables are defined.
             * @param detachedTypes   The detached type variable bounds this list represents.
             * @return A list of attached type variables.
             */
            public static GenericTypeList attach(TypeDescription typeDescription, List<? extends GenericTypeDescription> detachedTypes) {
                return new OfTypeVariable(typeDescription, GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(typeDescription), detachedTypes);
            }

            /**
             * Creates a list of detached type variables that are attached on reception.
             *
             * @param methodDescription The method by which the type variables are defined.
             * @param detachedTypes     The detached type variable bounds this list represents.
             * @return A list of attached type variables.
             */
            public static GenericTypeList attach(MethodDescription methodDescription, List<? extends GenericTypeDescription> detachedTypes) {
                return new OfTypeVariable(methodDescription, GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(methodDescription), detachedTypes);
            }

            @Override
            public GenericTypeDescription get(int index) {
                return LazyTypeVariable.of(detachedTypes.get(index), typeVariableSource, visitor);
            }

            @Override
            public int size() {
                return detachedTypes.size();
            }

            /**
             * A lazy type variable.
             */
            protected static class LazyTypeVariable extends GenericTypeDescription.ForTypeVariable {

                /**
                 * The represented symbol of the represented type variable.
                 */
                private final String symbol;

                /**
                 * The type variable source of this type variable.
                 */
                private final TypeVariableSource typeVariableSource;

                /**
                 * The visitor to use for attaching the represented type variable bounds.
                 */
                private final GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor;

                /**
                 * The detached bounds of this type variable.
                 */
                private final List<? extends GenericTypeDescription> detachedBounds;

                /**
                 * Creates a new lazy type variable.
                 *
                 * @param symbol             The represented symbol of the represented type variable.
                 * @param typeVariableSource The type variable source of this type variable.
                 * @param visitor            The visitor to use for attaching the represented type variable bounds.
                 * @param detachedBounds     The detached bounds of this type variable.
                 */
                protected LazyTypeVariable(String symbol,
                                           TypeVariableSource typeVariableSource,
                                           GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor,
                                           List<? extends GenericTypeDescription> detachedBounds) {
                    this.symbol = symbol;
                    this.typeVariableSource = typeVariableSource;
                    this.visitor = visitor;
                    this.detachedBounds = detachedBounds;
                }

                /**
                 * Creates a lazy type variable representation.
                 *
                 * @param detachedVariable   The detached variable to represent.
                 * @param typeVariableSource The type variable source of the type variable.
                 * @param visitor            The visitor to be used for attaching the type variable's bounds.
                 * @return A representation of the type variable with attached bounds.
                 */
                public static GenericTypeDescription of(GenericTypeDescription detachedVariable,
                                                        TypeVariableSource typeVariableSource,
                                                        GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor) {
                    return new LazyTypeVariable(detachedVariable.getSymbol(), typeVariableSource, visitor, detachedVariable.getUpperBounds());
                }

                @Override
                public GenericTypeList getUpperBounds() {
                    return new ForDetachedTypes(visitor, detachedBounds);
                }

                @Override
                public TypeVariableSource getVariableSource() {
                    return typeVariableSource;
                }

                @Override
                public String getSymbol() {
                    return symbol;
                }
            }
        }
    }

    /**
     * An empty list of generic types.
     */
    class Empty extends FilterableList.Empty<GenericTypeDescription, GenericTypeList> implements GenericTypeList {

        @Override
        public TypeList asErasures() {
            return new TypeList.Empty();
        }

        @Override
        public GenericTypeList accept(GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor) {
            return new GenericTypeList.Empty();
        }

        @Override
        public int getStackSize() {
            return 0;
        }
    }

    /**
     * A list of potentially raw generic type descriptions.
     */
    class OfPotentiallyRawType extends AbstractBase {

        /**
         * The potentially raw types that are represented by this list.
         */
        private final List<? extends GenericTypeDescription> typeDescriptions;

        /**
         * The transformer to apply to non-raw types.
         */
        private final GenericTypeDescription.Visitor<? extends GenericTypeDescription> transformer;

        /**
         * Creates a new list of potentially raw types.
         *
         * @param typeDescriptions The potentially raw types that are represented by this list.
         * @param transformer      The transformer to apply to non-raw types.
         */
        public OfPotentiallyRawType(List<? extends GenericTypeDescription> typeDescriptions,
                                    GenericTypeDescription.Visitor<? extends GenericTypeDescription> transformer) {
            this.typeDescriptions = typeDescriptions;
            this.transformer = transformer;
        }

        @Override
        public GenericTypeDescription get(int index) {
            return GenericTypeDescription.LazyProjection.OfPotentiallyRawType.of(typeDescriptions.get(index), transformer);
        }

        @Override
        public int size() {
            return typeDescriptions.size();
        }
    }

    /**
     * A lazy projection that only locates generic types on demand.
     */
    abstract class LazyProjection extends AbstractBase {

        /**
         * A lazy projection of a type's generic interface types.
         */
        public static class OfInterfaces extends LazyProjection {

            /**
             * The type of which the interface types are represented by this list.
             */
            private final Class<?> type;

            /**
             * Creates a lazy projection of interface types.
             *
             * @param type The type of which the interface types are represented by this list.
             */
            public OfInterfaces(Class<?> type) {
                this.type = type;
            }

            @Override
            public GenericTypeDescription get(int index) {
                return new TypeProjection(type, index, type.getInterfaces()[index]);
            }

            @Override
            public int size() {
                return type.getInterfaces().length;
            }

            @Override
            public TypeList asErasures() {
                return new TypeList.ForLoadedType(type.getInterfaces());
            }

            /**
             * A type projection of an interface type.
             */
            private static class TypeProjection extends GenericTypeDescription.LazyProjection {

                /**
                 * The type of which an interface type is represented.
                 */
                private final Class<?> type;

                /**
                 * The index of the generic interface type that is represented.
                 */
                private final int index;

                /**
                 * The erasure of the type of the interface this lazy projection represents.
                 */
                private final Class<?> erasure;

                /**
                 * Creates a new lazy type projection of a generic interface type.
                 *
                 * @param type    The type of which an interface type is represented.
                 * @param index   The index of the generic interface type that is represented.
                 * @param erasure The erasure of the type of the interface this lazy projection represents.
                 */
                private TypeProjection(Class<?> type, int index, Class<?> erasure) {
                    this.type = type;
                    this.index = index;
                    this.erasure = erasure;
                }

                @Override
                protected GenericTypeDescription resolve() {
                    return GenericTypeDescription.Sort.describe(type.getGenericInterfaces()[index]);
                }

                @Override
                public TypeDescription asErasure() {
                    return new TypeDescription.ForLoadedType(erasure);
                }
            }
        }

        /**
         * A lazy projection of a constructor's exception types.
         */
        public static class OfConstructorExceptionTypes extends LazyProjection {

            /**
             * The constructor of which the exception types are represented.
             */
            private final Constructor<?> constructor;

            /**
             * Creates a new lazy projection of a constructor's exception types.
             *
             * @param constructor The constructor of which the exception types are represented.
             */
            public OfConstructorExceptionTypes(Constructor<?> constructor) {
                this.constructor = constructor;
            }

            @Override
            public GenericTypeDescription get(int index) {
                return new TypeProjection(constructor, index, constructor.getExceptionTypes()[index]);
            }

            @Override
            public int size() {
                return constructor.getExceptionTypes().length;
            }

            @Override
            public TypeList asErasures() {
                return new TypeList.ForLoadedType(constructor.getExceptionTypes());
            }

            /**
             * A projection of a specific exception type.
             */
            private static class TypeProjection extends GenericTypeDescription.LazyProjection {

                /**
                 * The constructor of which the exception types are represented.
                 */
                private final Constructor<?> constructor;

                /**
                 * The index of the exception type.
                 */
                private final int index;

                /**
                 * The erasure of the type of the interface this lazy projection represents.
                 */
                private final Class<?> erasure;

                /**
                 * Creates a lazy type projection of a constructor's exception type.
                 *
                 * @param constructor The constructor of which the exception types are represented.
                 * @param index       The index of the exception type.
                 * @param erasure     The erasure of the type of the interface this lazy projection represents.
                 */
                private TypeProjection(Constructor<?> constructor, int index, Class<?> erasure) {
                    this.constructor = constructor;
                    this.index = index;
                    this.erasure = erasure;
                }

                @Override
                protected GenericTypeDescription resolve() {
                    return GenericTypeDescription.Sort.describe(constructor.getGenericExceptionTypes()[index]);
                }

                @Override
                public TypeDescription asErasure() {
                    return new TypeDescription.ForLoadedType(erasure);
                }
            }
        }

        /**
         * A lazy projection of a method's exception types.
         */
        public static class OfMethodExceptionTypes extends LazyProjection {

            /**
             * The method of which the exception types are represented.
             */
            private final Method method;

            /**
             * Creates a new lazy projection of a constructor's exception types.
             *
             * @param method The method of which the exception types are represented.
             */
            public OfMethodExceptionTypes(Method method) {
                this.method = method;
            }

            @Override
            public GenericTypeDescription get(int index) {
                return new TypeProjection(method, index, method.getExceptionTypes()[index]);
            }

            @Override
            public int size() {
                return method.getExceptionTypes().length;
            }

            @Override
            public TypeList asErasures() {
                return new TypeList.ForLoadedType(method.getExceptionTypes());
            }

            /**
             * A projection of a specific exception type.
             */
            private static class TypeProjection extends GenericTypeDescription.LazyProjection {

                /**
                 * The method of which the exception types are represented.
                 */
                private final Method method;

                /**
                 * The index of the exception type.
                 */
                private final int index;

                /**
                 * The erasure of the type of the interface this lazy projection represents.
                 */
                private final Class<?> erasure;

                /**
                 * Creates a lazy type projection of a constructor's exception type.
                 *
                 * @param method  The method of which the exception types are represented.
                 * @param index   The index of the exception type.
                 * @param erasure The erasure of the type of the interface this lazy projection represents.
                 */
                public TypeProjection(Method method, int index, Class<?> erasure) {
                    this.method = method;
                    this.index = index;
                    this.erasure = erasure;
                }

                @Override
                protected GenericTypeDescription resolve() {
                    return GenericTypeDescription.Sort.describe(method.getGenericExceptionTypes()[index]);
                }

                @Override
                public TypeDescription asErasure() {
                    return new TypeDescription.ForLoadedType(erasure);
                }
            }
        }
    }
}
