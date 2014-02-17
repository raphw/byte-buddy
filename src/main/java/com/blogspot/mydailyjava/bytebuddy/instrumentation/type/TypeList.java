package com.blogspot.mydailyjava.bytebuddy.instrumentation.type;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.StackSize;
import org.objectweb.asm.Type;

import java.util.*;

public interface TypeList extends List<TypeDescription> {

    static class ForLoadedType extends AbstractList<TypeDescription> implements TypeList {

        private final Class<?>[] type;

        public ForLoadedType(Class<?>[] type) {
            this.type = type;
        }

        @Override
        public TypeDescription get(int index) {
            return new TypeDescription.ForLoadedType(type[index]);
        }

        @Override
        public int size() {
            return type.length;
        }

        @Override
        public String[] toInternalNames() {
            String[] internalNames = new String[type.length];
            int i = 0;
            for(Class<?> aType : type) {
                internalNames[i++] = Type.getInternalName(aType);
            }
            return internalNames.length == 0 ? null : internalNames;
        }

        @Override
        public int getStackSize() {
            return StackSize.sizeOf(Arrays.asList(type));
        }
    }

    static class Explicit extends AbstractList<TypeDescription> implements TypeList {

        private final List<? extends TypeDescription> typeDescriptions;

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
            for(TypeDescription typeDescription : typeDescriptions) {
                internalNames[i++] = typeDescription.getInternalName();
            }
            return internalNames.length == 0 ? null : internalNames;
        }

        @Override
        public int getStackSize() {
            int stackSize = 0;
            for(TypeDescription typeDescription : typeDescriptions) {
                stackSize += typeDescription.getStackSize().getSize();
            }
            return stackSize;
        }
    }

    static class Empty extends AbstractList<TypeDescription> implements TypeList {

        @Override
        public TypeDescription get(int index) {
            throw new NoSuchElementException();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public String[] toInternalNames() {
            return null;
        }

        @Override
        public int getStackSize() {
            return 0;
        }
    }

    String[] toInternalNames();

    int getStackSize();
}
