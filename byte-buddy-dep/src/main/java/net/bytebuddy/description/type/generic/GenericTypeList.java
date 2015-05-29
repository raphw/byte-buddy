package net.bytebuddy.description.type.generic;

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
        public int getStackSize() {
            int stackSize = 0;
            for (GenericTypeDescription genericTypeDescription : this) {
                stackSize += genericTypeDescription.getStackSize().getSize();
            }
            return stackSize;
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

        @Override
        public TypeList asRawTypes() {
            List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(genericTypes.size());
            for (GenericTypeDescription genericTypeDescription : genericTypes) {
                typeDescriptions.add(genericTypeDescription.asRawType());
            }
            return new TypeList.Explicit(typeDescriptions);
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

    class Empty extends FilterableList.Empty<GenericTypeDescription, GenericTypeList> implements GenericTypeList {

        @Override
        public TypeList asRawTypes() {
            return new TypeList.Empty();
        }

        @Override
        public int getStackSize() {
            return 0;
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
                return GenericTypeDescription.Sort.describe(type.getGenericInterfaces()[index]);
            }

            @Override
            public int size() {
                return type.getInterfaces().length;
            }

            @Override
            public TypeList asRawTypes() {
                return new TypeList.ForLoadedType(type.getInterfaces());
            }
        }

        public static class OfConstructorExceptionTypes extends LazyProjection {

            private final Constructor<?> constructor;

            public OfConstructorExceptionTypes(Constructor<?> constructor) {
                this.constructor = constructor;
            }

            @Override
            public GenericTypeDescription get(int index) {
                return GenericTypeDescription.Sort.describe(constructor.getGenericExceptionTypes()[index]);
            }

            @Override
            public int size() {
                return constructor.getExceptionTypes().length;
            }

            @Override
            public TypeList asRawTypes() {
                return new TypeList.ForLoadedType(constructor.getExceptionTypes());
            }
        }

        public static class OfMethodExceptionTypes extends LazyProjection {

            private final Method method;

            public OfMethodExceptionTypes(Method method) {
                this.method = method;
            }

            @Override
            public GenericTypeDescription get(int index) {
                return GenericTypeDescription.Sort.describe(method.getGenericExceptionTypes()[index]);
            }

            @Override
            public int size() {
                return method.getExceptionTypes().length;
            }

            @Override
            public TypeList asRawTypes() {
                return new TypeList.ForLoadedType(method.getExceptionTypes());
            }
        }
    }
}
