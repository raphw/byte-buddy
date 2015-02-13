package net.bytebuddy.utility;

import net.bytebuddy.instrumentation.type.TypeDescription;

import java.io.Serializable;

public enum JavaType {

    METHOD_HANDLE("java.lang.invoke.MethodHandle") {
        @Override
        protected boolean isSubTypeOf(TypeDescription typeDescription) {
            return typeDescription.represents(Object.class);
        }
    },

    METHOD_TYPE("java.lang.invoke.MethodType") {
        @Override
        protected boolean isSubTypeOf(TypeDescription typeDescription) {
            return typeDescription.represents(Object.class) || typeDescription.represents(Serializable.class);
        }
    },

    METHOD_TYPES_LOOKUP("java.lang.invoke.MethodHandles$Lookup") {
        @Override
        protected boolean isSubTypeOf(TypeDescription typeDescription) {
            return typeDescription.represents(Object.class);
        }
    },

    CALL_SITE("java.lang.invoke.CallSite") {
        @Override
        protected boolean isSubTypeOf(TypeDescription typeDescription) {
            return typeDescription.represents(Object.class);
        }
    };

    protected static interface TypeLookup {

        static class ForNamedType implements TypeLookup {

            private final String typeName;

            public ForNamedType(String typeName) {
                this.typeName = typeName;
            }

            @Override
            public boolean isAssignableFrom(TypeDescription typeDescription) {
                do {
                    if (typeDescription.getName().equals(typeName)) {
                        return true;
                    }
                    typeDescription = typeDescription.getSupertype();
                } while (typeDescription != null);
                return false;
            }

            @Override
            public boolean represents(TypeDescription typeDescription) {
                return typeDescription.getName().equals(typeName);
            }

            @Override
            public Class<?> load() {
                throw new IllegalStateException("Could not load " + typeName);
            }
        }

        static class ForLoadedType implements TypeLookup {

            private final Class<?> type;

            public ForLoadedType(Class<?> type) {
                this.type = type;
            }

            @Override
            public boolean isAssignableFrom(TypeDescription typeDescription) {
                return typeDescription.isAssignableTo(type);
            }

            @Override
            public boolean represents(TypeDescription typeDescription) {
                return typeDescription.represents(type);
            }

            @Override
            public Class<?> load() {
                return type;
            }
        }

        boolean isAssignableFrom(TypeDescription typeDescription);

        boolean represents(TypeDescription typeDescription);

        Class<?> load();
    }

    private final TypeLookup typeLookup;

    private JavaType(String typeName) {
        TypeLookup typeLookup;
        try {
            typeLookup = new TypeLookup.ForLoadedType(Class.forName(typeName));
        } catch (Exception ignored) {
            typeLookup = new TypeLookup.ForNamedType(typeName);
        }
        this.typeLookup = typeLookup;
    }

    public boolean isAssignableFrom(TypeDescription typeDescription) {
        return typeLookup.isAssignableFrom(typeDescription);
    }

    public boolean isAssignableTo(TypeDescription typeDescription) {
        return representedBy(typeDescription) || isSubTypeOf(typeDescription);
    }

    protected abstract boolean isSubTypeOf(TypeDescription typeDescription);

    public boolean isAssignableFromOrTo(TypeDescription typeDescription) {
        return isSubTypeOf(typeDescription) || isAssignableFrom(typeDescription);
    }

    public boolean representedBy(TypeDescription typeDescription) {
        return typeLookup.represents(typeDescription);
    }

    public Class<?> load() {
        return typeLookup.load();
    }
}
