package net.bytebuddy.description.type.generic;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.matcher.FilterableList;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface GenericTypeList extends FilterableList<GenericType, GenericTypeList> {

    TypeList asRawTypes();

    class Explicit extends FilterableList.AbstractBase<GenericType, GenericTypeList> implements GenericTypeList {

        private final List<? extends GenericType> genericTypes;

        public Explicit(List<? extends GenericType> genericTypes) {
            this.genericTypes = genericTypes;
        }

        @Override
        protected GenericTypeList wrap(List<GenericType> values) {
            return new Explicit(values);
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

    class ForLoadedType extends FilterableList.AbstractBase<GenericType, GenericTypeList> implements GenericTypeList {

        private final List<? extends Type> types;

        public ForLoadedType(Type... type) {
            this(Arrays.asList(type));
        }

        public ForLoadedType(List<? extends Type> types) {
            this.types = types;
        }

        @Override
        protected GenericTypeList wrap(List<GenericType> values) {
            return new Explicit(values);
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
    }
}
