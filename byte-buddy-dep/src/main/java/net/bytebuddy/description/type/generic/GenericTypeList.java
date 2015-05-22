package net.bytebuddy.description.type.generic;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.matcher.FilterableList;
import org.objectweb.asm.signature.SignatureWriter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface GenericTypeList extends FilterableList<GenericType, GenericTypeList> {

    TypeList asRawTypes();

    String toSignature();

    abstract class AbstractBase extends FilterableList.AbstractBase<GenericType, GenericTypeList> implements GenericTypeList {

        @Override
        public String toSignature() {
            return null; // TODO: implement, remove?
        }

        @Override
        protected GenericTypeList wrap(List<GenericType> values) {
            return new Explicit(values);
        }
    }

    class Explicit extends AbstractBase {

        private final List<? extends GenericType> genericTypes;

        public Explicit(List<? extends GenericType> genericTypes) {
            this.genericTypes = genericTypes;
        }

        @Override
        public GenericType get(int index) {
            return genericTypes.get(index);
        }

        @Override
        public int size() {
            return genericTypes.size();
        }

        @Override
        public TypeList asRawTypes() {
            List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(genericTypes.size());
            for (GenericType genericType : genericTypes) {
                typeDescriptions.add(genericType.asRawType());
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
        public GenericType get(int index) {
            return GenericType.Sort.describe(types.get(index));
        }

        @Override
        public int size() {
            return types.size();
        }

        @Override
        public TypeList asRawTypes() {
            List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(types.size());
            for (GenericType genericType : this) {
                typeDescriptions.add(genericType.asRawType());
            }
            return new TypeList.Explicit(typeDescriptions);
        }
    }

    class Empty extends FilterableList.Empty<GenericType, GenericTypeList> implements GenericTypeList {

        @Override
        public TypeList asRawTypes() {
            return new TypeList.Empty();
        }

        @Override
        public String toSignature() {
            return null;
        }
    }

    abstract class LazyProjection extends AbstractBase {

        public static class OfInterfaces extends LazyProjection {

            private final Class<?> type;

            public OfInterfaces(Class<?> type) {
                this.type = type;
            }

            @Override
            public GenericType get(int index) {
                return GenericType.Sort.describe(type.getGenericInterfaces()[index]);
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
            public GenericType get(int index) {
                return GenericType.Sort.describe(constructor.getGenericExceptionTypes()[index]);
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
            public GenericType get(int index) {
                return GenericType.Sort.describe(method.getGenericExceptionTypes()[index]);
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
