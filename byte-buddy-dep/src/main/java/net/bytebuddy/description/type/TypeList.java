package net.bytebuddy.description.type;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.TypeVariableSource;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.matcher.FilterableList;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementations represent a list of type descriptions.
 */
public interface TypeList extends FilterableList<TypeDescription, TypeList> {

    /**
     * Represents that a type list does not contain any values for ASM interoperability which is represented by {@code null}.
     */
    @SuppressFBWarnings(value = {"MS_MUTABLE_ARRAY", "MS_OOI_PKGPROTECT"}, justification = "value is null and should be available to extensions")
    String[] NO_INTERFACES = null;

    /**
     * Returns a list of internal names of all types represented by this list.
     *
     * @return An array of all internal names or {@code null} if the list is empty.
     */
    String[] toInternalNames();

    /**
     * Returns the sum of the size of all types contained in this list.
     *
     * @return The sum of the size of all types contained in this list.
     */
    int getStackSize();

    /**
     * An abstract base implementation of a type list.
     */
    abstract class AbstractBase extends FilterableList.AbstractBase<TypeDescription, TypeList> implements TypeList {

        @Override
        protected TypeList wrap(List<TypeDescription> values) {
            return new Explicit(values);
        }
    }

    /**
     * Implementation of a type list for an array of loaded types.
     */
    class ForLoadedTypes extends AbstractBase {

        /**
         * The loaded types this type list represents.
         */
        private final List<? extends Class<?>> types;

        /**
         * Creates a new type list for an array of loaded types.
         *
         * @param type The types to be represented by this list.
         */
        public ForLoadedTypes(Class<?>... type) {
            this(Arrays.asList(type));
        }

        /**
         * Creates a new type list for an array of loaded types.
         *
         * @param types The types to be represented by this list.
         */
        public ForLoadedTypes(List<? extends Class<?>> types) {
            this.types = types;
        }

        @Override
        public TypeDescription get(int index) {
            return new TypeDescription.ForLoadedType(types.get(index));
        }

        @Override
        public int size() {
            return types.size();
        }

        @Override
        public String[] toInternalNames() {
            String[] internalNames = new String[types.size()];
            int i = 0;
            for (Class<?> type : types) {
                internalNames[i++] = Type.getInternalName(type);
            }
            return internalNames.length == 0
                    ? NO_INTERFACES
                    : internalNames;
        }

        @Override
        public int getStackSize() {
            return StackSize.sizeOf(types);
        }
    }

    /**
     * A wrapper implementation of an explicit list of types.
     */
    class Explicit extends AbstractBase {

        /**
         * The list of type descriptions this list represents.
         */
        private final List<? extends TypeDescription> typeDescriptions;

        /**
         * Creates an immutable wrapper.
         *
         * @param typeDescriptions The list of types to be represented by this wrapper.
         */
        public Explicit(List<? extends TypeDescription> typeDescriptions) {
            this.typeDescriptions = typeDescriptions;
        }

        @Override
        public TypeDescription get(int index) {
            return typeDescriptions.get(index);
        }

        @Override
        public int size() {
            return typeDescriptions.size();
        }

        @Override
        public String[] toInternalNames() {
            String[] internalNames = new String[typeDescriptions.size()];
            int i = 0;
            for (TypeDescription typeDescription : typeDescriptions) {
                internalNames[i++] = typeDescription.getInternalName();
            }
            return internalNames.length == 0
                    ? NO_INTERFACES
                    : internalNames;
        }

        @Override
        public int getStackSize() {
            int stackSize = 0;
            for (TypeDescription typeDescription : typeDescriptions) {
                stackSize += typeDescription.getStackSize().getSize();
            }
            return stackSize;
        }
    }

    /**
     * An implementation of an empty type list.
     */
    class Empty extends FilterableList.Empty<TypeDescription, TypeList> implements TypeList {

        @Override
        @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Return value is always null")
        public String[] toInternalNames() {
            return NO_INTERFACES;
        }

        @Override
        public int getStackSize() {
            return 0;
        }
    }

    /**
     * A list containing descriptions of generic types.
     */
    interface Generic extends FilterableList<TypeDescription.Generic, Generic> {

        /**
         * Returns a list of the generic types' erasures.
         *
         * @return A list of the generic types' erasures.
         */
        TypeList asErasures();

        Generic asRawTypes();

        /**
         * Transforms the generic types by applying the supplied visitor to each of them.
         *
         * @param visitor The visitor to apply to each type.
         * @return A list of the types returned by the supplied visitor.
         */
        Generic accept(TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor);

        /**
         * Returns the sum of the size of all types contained in this list.
         *
         * @return The sum of the size of all types contained in this list.
         */
        int getStackSize();

        /**
         * An abstract base implementation of a generic type list.
         */
        abstract class AbstractBase extends FilterableList.AbstractBase<TypeDescription.Generic, Generic> implements Generic {

            @Override
            protected Generic wrap(List<TypeDescription.Generic> values) {
                return new Explicit(values);
            }

            @Override
            public Generic accept(TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
                List<TypeDescription.Generic> visited = new ArrayList<TypeDescription.Generic>(size());
                for (TypeDescription.Generic typeDescription : this) {
                    visited.add(typeDescription.accept(visitor));
                }
                return new Explicit(visited);
            }

            @Override
            public int getStackSize() {
                int stackSize = 0;
                for (TypeDescription.Generic typeDescription : this) {
                    stackSize += typeDescription.getStackSize().getSize();
                }
                return stackSize;
            }

            @Override
            public TypeList asErasures() {
                List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(size());
                for (TypeDescription.Generic typeDescription : this) {
                    typeDescriptions.add(typeDescription.asErasure());
                }
                return new TypeList.Explicit(typeDescriptions);
            }

            @Override
            public Generic asRawTypes() {
                List<TypeDescription.Generic> typeDescriptions = new ArrayList<TypeDescription.Generic>(size());
                for (TypeDescription.Generic typeDescription : this) {
                    typeDescriptions.add(typeDescription.asRawType());
                }
                return new Explicit(typeDescriptions);
            }
        }

        /**
         * An explicit list of generic types.
         */
        class Explicit extends AbstractBase {

            /**
             * The generic types represented by this list.
             */
            private final List<? extends TypeDefinition> genericTypes;

            public Explicit(TypeDefinition... typeDefinition) {
                this(Arrays.asList(typeDefinition));
            }

            /**
             * Creates a new explicit list of generic types.
             *
             * @param genericTypes The generic types represented by this list.
             */
            public Explicit(List<? extends TypeDefinition> genericTypes) {
                this.genericTypes = genericTypes;
            }

            @Override
            public TypeDescription.Generic get(int index) {
                return genericTypes.get(index).asGenericType();
            }

            @Override
            public int size() {
                return genericTypes.size();
            }
        }

        /**
         * A list of loaded generic types.
         */
        class ForLoadedTypes extends AbstractBase {

            /**
             * The loaded types this list represents.
             */
            private final List<? extends java.lang.reflect.Type> types;

            /**
             * Creates a list of loaded generic types.
             *
             * @param type The loaded types this list represents.
             */
            public ForLoadedTypes(java.lang.reflect.Type... type) {
                this(Arrays.asList(type));
            }

            /**
             * Creates a list of loaded generic types.
             *
             * @param types The loaded types this list represents.
             */
            public ForLoadedTypes(List<? extends java.lang.reflect.Type> types) {
                this.types = types;
            }

            @Override
            public TypeDescription.Generic get(int index) {
                return TypeDefinition.Sort.describe(types.get(index));
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
             * The detached types this list represents.
             */
            private final List<? extends TypeDescription.Generic> detachedTypes;

            /**
             * The visitor to use for attaching the detached types.
             */
            private final TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor;

            /**
             * Creates a list of detached types that are attached on reception.
             *
             * @param detachedTypes The detached types this list represents.
             * @param visitor       The visitor to use for attaching the detached types.
             */
            public ForDetachedTypes(List<? extends TypeDescription.Generic> detachedTypes,
                                    TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
                this.detachedTypes = detachedTypes;
                this.visitor = visitor;
            }

            /**
             * Creates a list of types that are attached to the provided type.
             *
             * @param typeDescription The type to which the detached variables are attached to.
             * @param detachedTypes   The detached types.
             * @return A type list representing the detached types being attached to the provided type description.
             */
            public static Generic attach(TypeDescription typeDescription, List<? extends TypeDescription.Generic> detachedTypes) {
                return new ForDetachedTypes(detachedTypes, TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(typeDescription));
            }

            /**
             * Creates a list of types that are attached to the provided field.
             *
             * @param fieldDescription The field to which the detached variables are attached to.
             * @param detachedTypes    The detached types.
             * @return A type list representing the detached types being attached to the provided field description.
             */
            public static Generic attach(FieldDescription fieldDescription, List<? extends TypeDescription.Generic> detachedTypes) {
                return new ForDetachedTypes(detachedTypes, TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(fieldDescription));
            }

            /**
             * Creates a list of types that are attached to the provided method.
             *
             * @param methodDescription The method to which the detached variables are attached to.
             * @param detachedTypes     The detached types.
             * @return A type list representing the detached types being attached to the provided method description.
             */
            public static Generic attach(MethodDescription methodDescription, List<? extends TypeDescription.Generic> detachedTypes) {
                return new ForDetachedTypes(detachedTypes, TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(methodDescription));
            }

            /**
             * Creates a list of types that are attached to the provided parameter.
             *
             * @param parameterDescription The parameter to which the detached variables are attached to.
             * @param detachedTypes        The detached types.
             * @return A type list representing the detached types being attached to the provided parameter description.
             */
            public static Generic attach(ParameterDescription parameterDescription, List<? extends TypeDescription.Generic> detachedTypes) {
                return new ForDetachedTypes(detachedTypes, TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(parameterDescription));
            }

            @Override
            public TypeDescription.Generic get(int index) {
                return detachedTypes.get(index).accept(visitor);
            }

            @Override
            public int size() {
                return detachedTypes.size();
            }

            /**
             * A list of type variables that are attached on reception.
             */
            public static class OfTypeVariable extends Generic.AbstractBase {

                /**
                 * The type variable source of the represented type variables.
                 */
                private final TypeVariableSource typeVariableSource;

                /**
                 * The visitor to use for attaching the represented types.
                 */
                private final TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor;

                /**
                 * A list of detached types that are attached on reception.
                 */
                private final List<? extends TypeDescription.Generic> detachedTypes;

                /**
                 * Creates a new list of attached type variables.
                 *
                 * @param typeVariableSource The type variable source of the represented type variables.
                 * @param visitor            The visitor to use for attaching the represented types.
                 * @param detachedTypes      A list of detached types that are attached on reception.
                 */
                protected OfTypeVariable(TypeVariableSource typeVariableSource,
                                         TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor,
                                         List<? extends TypeDescription.Generic> detachedTypes) {
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
                public static Generic attach(TypeDescription typeDescription, List<? extends TypeDescription.Generic> detachedTypes) {
                    return new OfTypeVariable(typeDescription, TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(typeDescription), detachedTypes);
                }

                /**
                 * Creates a list of detached type variables that are attached on reception.
                 *
                 * @param methodDescription The method by which the type variables are defined.
                 * @param detachedTypes     The detached type variable bounds this list represents.
                 * @return A list of attached type variables.
                 */
                public static Generic attach(MethodDescription methodDescription, List<? extends TypeDescription.Generic> detachedTypes) {
                    return new OfTypeVariable(methodDescription, TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(methodDescription), detachedTypes);
                }

                @Override
                public TypeDescription.Generic get(int index) {
                    return ForDetachedTypes.OfTypeVariable.LazyTypeVariable.of(detachedTypes.get(index), typeVariableSource, visitor);
                }

                @Override
                public int size() {
                    return detachedTypes.size();
                }

                /**
                 * A lazy type variable.
                 */
                protected static class LazyTypeVariable extends TypeDescription.Generic.OfTypeVariable {

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
                    private final Visitor<? extends TypeDescription.Generic> visitor;

                    /**
                     * The detached bounds of this type variable.
                     */
                    private final List<? extends TypeDescription.Generic> detachedBounds;

                    /**
                     * Creates a new lazy type variable.
                     *
                     * @param symbol             The represented symbol of the represented type variable.
                     * @param typeVariableSource The type variable source of this type variable.
                     * @param detachedBounds     The detached bounds of this type variable.
                     * @param visitor            The visitor to use for attaching the represented type variable bounds.
                     */
                    protected LazyTypeVariable(String symbol,
                                               TypeVariableSource typeVariableSource,
                                               List<? extends TypeDescription.Generic> detachedBounds,
                                               Visitor<? extends TypeDescription.Generic> visitor) {
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
                    public static TypeDescription.Generic of(TypeDescription.Generic detachedVariable,
                                                             TypeVariableSource typeVariableSource,
                                                             Visitor<? extends TypeDescription.Generic> visitor) {
                        return new ForDetachedTypes.OfTypeVariable.LazyTypeVariable(detachedVariable.getSymbol(), typeVariableSource, detachedVariable.getUpperBounds(), visitor);
                    }

                    @Override
                    public Generic getUpperBounds() {
                        return new ForDetachedTypes(detachedBounds, visitor);
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
         * A lazy projection of a type's generic interface types.
         */
        class OfLoadedInterfaceTypes extends AbstractBase {

            /**
             * The type of which the interface types are represented by this list.
             */
            private final Class<?> type;

            /**
             * Creates a lazy projection of interface types.
             *
             * @param type The type of which the interface types are represented by this list.
             */
            public OfLoadedInterfaceTypes(Class<?> type) {
                this.type = type;
            }

            @Override
            public TypeDescription.Generic get(int index) {
                return new OfLoadedInterfaceTypes.TypeProjection(type, index, type.getInterfaces()[index]);
            }

            @Override
            public int size() {
                return type.getInterfaces().length;
            }

            @Override
            public TypeList asErasures() {
                return new TypeList.ForLoadedTypes(type.getInterfaces());
            }

            /**
             * A type projection of an interface type.
             */
            private static class TypeProjection extends TypeDescription.Generic.LazyProjection {

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
                protected TypeDescription.Generic resolve() {
                    return Sort.describe(type.getGenericInterfaces()[index]);
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
        class OfConstructorExceptionTypes extends AbstractBase {

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
            public TypeDescription.Generic get(int index) {
                return new OfConstructorExceptionTypes.TypeProjection(constructor, index, constructor.getExceptionTypes()[index]);
            }

            @Override
            public int size() {
                return constructor.getExceptionTypes().length;
            }

            @Override
            public TypeList asErasures() {
                return new TypeList.ForLoadedTypes(constructor.getExceptionTypes());
            }

            /**
             * A projection of a specific exception type.
             */
            private static class TypeProjection extends TypeDescription.Generic.LazyProjection {

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
                protected TypeDescription.Generic resolve() {
                    return Sort.describe(constructor.getGenericExceptionTypes()[index]);
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
        class OfMethodExceptionTypes extends AbstractBase {

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
            public TypeDescription.Generic get(int index) {
                return new OfMethodExceptionTypes.TypeProjection(method, index, method.getExceptionTypes()[index]);
            }

            @Override
            public int size() {
                return method.getExceptionTypes().length;
            }

            @Override
            public TypeList asErasures() {
                return new TypeList.ForLoadedTypes(method.getExceptionTypes());
            }

            /**
             * A projection of a specific exception type.
             */
            private static class TypeProjection extends TypeDescription.Generic.LazyProjection {

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
                protected TypeDescription.Generic resolve() {
                    return Sort.describe(method.getGenericExceptionTypes()[index]);
                }

                @Override
                public TypeDescription asErasure() {
                    return new TypeDescription.ForLoadedType(erasure);
                }
            }
        }

        /**
         * An empty list of generic types.
         */
        class Empty extends FilterableList.Empty<TypeDescription.Generic, Generic> implements Generic {

            @Override
            public TypeList asErasures() {
                return new TypeList.Empty();
            }

            @Override
            public Generic asRawTypes() {
                return this;
            }

            @Override
            public Generic accept(TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
                return new Generic.Empty();
            }

            @Override
            public int getStackSize() {
                return 0;
            }
        }
    }
}
