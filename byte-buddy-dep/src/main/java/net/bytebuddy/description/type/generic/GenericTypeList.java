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

public interface GenericTypeList extends FilterableList<GenericTypeDescription, GenericTypeList> {

    TypeList asRawTypes();

    GenericTypeList accept(GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor);

    /**
     * Returns the sum of the size of all types contained in this list.
     *
     * @return The sum of the size of all types contained in this list.
     */
    int getStackSize();

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
        public TypeList asRawTypes() {
            List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(size());
            for (GenericTypeDescription genericTypeDescription : this) {
                typeDescriptions.add(genericTypeDescription.asRawType());
            }
            return new TypeList.Explicit(typeDescriptions);
        }
    }

    class Explicit extends AbstractBase {

        private final List<? extends GenericTypeDescription> genericTypes;

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

    class ForLoadedType extends AbstractBase {

        private final List<? extends Type> types;

        public ForLoadedType(Type... type) {
            this(Arrays.asList(type));
        }

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

        @Override
        public TypeList asRawTypes() {
            List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(types.size());
            for (GenericTypeDescription genericTypeDescription : this) {
                typeDescriptions.add(genericTypeDescription.asRawType());
            }
            return new TypeList.Explicit(typeDescriptions);
        }
    }

    class ForDetachedTypes extends AbstractBase {

        public static GenericTypeList attach(TypeDescription typeDescription, List<? extends GenericTypeDescription> detachedTypes) {
            return new ForDetachedTypes(GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(typeDescription), detachedTypes);
        }

        public static GenericTypeList attach(FieldDescription fieldDescription, List<? extends GenericTypeDescription> detachedTypes) {
            return new ForDetachedTypes(GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(fieldDescription), detachedTypes);
        }

        public static GenericTypeList attach(MethodDescription methodDescription, List<? extends GenericTypeDescription> detachedTypes) {
            return new ForDetachedTypes(GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(methodDescription), detachedTypes);
        }

        public static GenericTypeList attach(ParameterDescription parameterDescription, List<? extends GenericTypeDescription> detachedTypes) {
            return new ForDetachedTypes(GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(parameterDescription), detachedTypes);
        }

        private final GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor;

        private final List<? extends GenericTypeDescription> detachedTypes;

        protected ForDetachedTypes(GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor,
                                   List<? extends GenericTypeDescription> detachedTypes) {
            this.visitor = visitor;
            this.detachedTypes = detachedTypes;
        }

        @Override
        public GenericTypeDescription get(int index) {
            return detachedTypes.get(index).accept(visitor);
        }

        @Override
        public int size() {
            return detachedTypes.size();
        }

        public static class OfTypeVariable extends GenericTypeList.AbstractBase {

            public static GenericTypeList attach(TypeDescription typeDescription, List<? extends GenericTypeDescription> detachedTypes) {
                return new OfTypeVariable(typeDescription, GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(typeDescription), detachedTypes);
            }

            public static GenericTypeList attach(MethodDescription methodDescription, List<? extends GenericTypeDescription> detachedTypes) {
                return new OfTypeVariable(methodDescription, GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(methodDescription), detachedTypes);
            }

            private final TypeVariableSource typeVariableSource;

            private final GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor;

            private final List<? extends GenericTypeDescription> detachedTypes;

            protected OfTypeVariable(TypeVariableSource typeVariableSource,
                                     GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor,
                                     List<? extends GenericTypeDescription> detachedTypes) {
                this.typeVariableSource = typeVariableSource;
                this.visitor = visitor;
                this.detachedTypes = detachedTypes;
            }

            @Override
            public GenericTypeDescription get(int index) {
                return LazyTypeVariable.of(detachedTypes.get(index), typeVariableSource, visitor);
            }

            @Override
            public int size() {
                return detachedTypes.size();
            }

            protected static class LazyTypeVariable extends GenericTypeDescription.ForTypeVariable {

                public static GenericTypeDescription of(GenericTypeDescription detachedVariable,
                                                        TypeVariableSource typeVariableSource,
                                                        GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor) {
                    return new LazyTypeVariable(detachedVariable.getSymbol(), typeVariableSource, visitor, detachedVariable.getUpperBounds());
                }

                private final String symbol;

                private final TypeVariableSource typeVariableSource;

                private final GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor;

                private final List<? extends GenericTypeDescription> detachedBounds;

                protected LazyTypeVariable(String symbol,
                                           TypeVariableSource typeVariableSource,
                                           GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor,
                                           List<? extends GenericTypeDescription> detachedBounds) {
                    this.symbol = symbol;
                    this.typeVariableSource = typeVariableSource;
                    this.visitor = visitor;
                    this.detachedBounds = detachedBounds;
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

    class Empty extends FilterableList.Empty<GenericTypeDescription, GenericTypeList> implements GenericTypeList {

        @Override
        public TypeList asRawTypes() {
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

    class OfPotentiallyRawType extends AbstractBase {

        private final List<? extends GenericTypeDescription> typeDescriptions;

        private final GenericTypeDescription.Visitor<? extends GenericTypeDescription> transformer;

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

    abstract class LazyProjection extends AbstractBase {

        public static class OfInterfaces extends LazyProjection {

            private final Class<?> type;

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
            public TypeList asRawTypes() {
                return new TypeList.ForLoadedType(type.getInterfaces());
            }

            private static class TypeProjection extends GenericTypeDescription.LazyProjection {

                private final Class<?> type;

                private final int index;

                private final Class<?> rawType;

                public TypeProjection(Class<?> type, int index, Class<?> rawType) {
                    this.type = type;
                    this.index = index;
                    this.rawType = rawType;
                }

                @Override
                protected GenericTypeDescription resolve() {
                    return GenericTypeDescription.Sort.describe(type.getGenericInterfaces()[index]);
                }

                @Override
                public TypeDescription asRawType() {
                    return new TypeDescription.ForLoadedType(rawType);
                }
            }
        }

        public static class OfConstructorExceptionTypes extends LazyProjection {

            private final Constructor<?> constructor;

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
            public TypeList asRawTypes() {
                return new TypeList.ForLoadedType(constructor.getExceptionTypes());
            }

            private static class TypeProjection extends GenericTypeDescription.LazyProjection {

                private final Constructor<?> constructor;

                private final int index;

                private final Class<?> rawType;

                public TypeProjection(Constructor<?> constructor, int index, Class<?> rawType) {
                    this.constructor = constructor;
                    this.index = index;
                    this.rawType = rawType;
                }

                @Override
                protected GenericTypeDescription resolve() {
                    return GenericTypeDescription.Sort.describe(constructor.getGenericExceptionTypes()[index]);
                }

                @Override
                public TypeDescription asRawType() {
                    return new TypeDescription.ForLoadedType(rawType);
                }
            }
        }

        public static class OfMethodExceptionTypes extends LazyProjection {

            private final Method method;

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
            public TypeList asRawTypes() {
                return new TypeList.ForLoadedType(method.getExceptionTypes());
            }

            private static class TypeProjection extends GenericTypeDescription.LazyProjection {

                private final Method method;

                private final int index;

                private final Class<?> rawType;

                public TypeProjection(Method method, int index, Class<?> rawType) {
                    this.method = method;
                    this.index = index;
                    this.rawType = rawType;
                }

                @Override
                protected GenericTypeDescription resolve() {
                    return GenericTypeDescription.Sort.describe(method.getGenericExceptionTypes()[index]);
                }

                @Override
                public TypeDescription asRawType() {
                    return new TypeDescription.ForLoadedType(rawType);
                }
            }
        }
    }
}
