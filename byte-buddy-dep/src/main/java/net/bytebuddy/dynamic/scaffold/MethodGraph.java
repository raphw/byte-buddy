package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;

import java.util.*;

public interface MethodGraph {

    Resolution resolve(MethodDescription.Token methodToken);

    Resolution resolveDefault(TypeDescription typeDescription, MethodDescription.Token methodToken);

    List<MethodDescription> getInvokableMethods();

    TypeDescription getTypeDescription();

    interface Resolution {

        MethodDescription getResolvedMethod();

        boolean isResolved();

        enum Illegal implements Resolution {

            INSTANCE;

            @Override
            public MethodDescription getResolvedMethod() {
                throw new IllegalStateException();
            }

            @Override
            public boolean isResolved() {
                return false;
            }
        }

        class ForMethod implements Resolution {

            private final MethodDescription methodDescription;

            public ForMethod(MethodDescription methodDescription) {
                this.methodDescription = methodDescription;
            }

            @Override
            public MethodDescription getResolvedMethod() {
                return methodDescription;
            }

            @Override
            public boolean isResolved() {
                return true;
            }
        }
    }

    interface Fabricator {

        MethodGraph process(TypeDescription typeDescription);

        class Default implements Fabricator {

            private final Key.Factory keyFactory;

            public Default(Key.Factory keyFactory) {
                this.keyFactory = keyFactory;
            }

            @Override
            public MethodGraph process(TypeDescription typeDescription) {
//                Registry registry = new Registry();
//                for (GenericTypeDescription currentType : typeDescription) {
//                    registry.append(currentType.getDeclaredMethods());
//                }
//                return registry.toGraph();
                return null;
            }

            protected static class Registry {

                private final Key.Factory keyFactory;

                private final Map<Key, Key> primaryKeys;

                private final Map<Key, Entry> entries;

                protected Registry(Key.Factory keyFactory) {
                    this.keyFactory = keyFactory;
                    primaryKeys = new HashMap<Key, Key>();
                    entries = new LinkedHashMap<Key, Entry>();
                }

                protected void append(List<? extends MethodDescription> methodDescriptions) {
                    for (MethodDescription methodDescription : methodDescriptions) {
                        Key key = primaryKeys.get(keyFactory.make(methodDescription.asToken())); // TODO: Smarter structure, get or insert
                        Entry entry = entries.get(key); // TODO: Smarter structure, get or create
                        entry.append(methodDescription); // TODO: Add declared method's key.
                    }
                }

                protected MethodGraph toGraph() {
                    return null;
                }

                protected static class Entry {

                    List<MethodDescription> representedMethods;

                    Entry append(MethodDescription methodDescription) {
                        return null;
                    }
                }
            }

            public interface Key {

                MethodDescription.Token getToken();

                interface Factory {

                    Key make(MethodDescription.Token methodToken);

                    enum ForJavaMethod implements Factory {

                        INSTANCE;

                        @Override
                        public Key make(MethodDescription.Token methodToken) {
                            return new Key.ForJavaMethod(methodToken);
                        }
                    }
                }

                class ForJavaMethod implements Key {

                    private final MethodDescription.Token methodToken;

                    public ForJavaMethod(MethodDescription.Token methodToken) {
                        this.methodToken = methodToken;
                    }

                    @Override
                    public MethodDescription.Token getToken() {
                        return methodToken;
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (other == this) return true;
                        if (!(other instanceof Key)) return false;
                        Key key = (Key) other;
                        if (!methodToken.getInternalName().equals(key.getToken().getInternalName())) {
                            return false;
                        }
                        int index = 0;
                        for (ParameterDescription.Token parameterToken : methodToken.getParameterTokens()) {
                            if (!parameterToken.getType().asRawType().equals(key.getToken().getParameterTokens().get(index++).getType().asRawType())) {
                                return false;
                            }
                        }
                        return true;
                    }

                    @Override
                    public int hashCode() {
                        int hashCode = methodToken.getInternalName().hashCode();
                        for (ParameterDescription.Token parameterToken : methodToken.getParameterTokens()) {
                            hashCode = hashCode * 31 + parameterToken.getType().asRawType().hashCode();
                        }
                        return hashCode;
                    }
                }
            }
        }
    }

    class Simple implements MethodGraph {

        private final TypeDescription typeDescription;

        private final List<MethodDescription> invokeableMethods;

        private final Map<MethodDescription.Token, MethodDescription> superMethods;

        private final Map<TypeDescription, Map<MethodDescription.Token, MethodDescription>> defaultMethods;

        public Simple(TypeDescription typeDescription,
                      List<MethodDescription> invokeableMethods,
                      Map<MethodDescription.Token, MethodDescription> superMethods,
                      Map<TypeDescription, Map<MethodDescription.Token, MethodDescription>> defaultMethods) {
            this.typeDescription = typeDescription;
            this.invokeableMethods = invokeableMethods;
            this.superMethods = superMethods;
            this.defaultMethods = defaultMethods;
        }

        @Override
        public Resolution resolve(MethodDescription.Token methodToken) {
            MethodDescription methodDescription = superMethods.get(methodToken);
            return methodDescription == null
                    ? Resolution.Illegal.INSTANCE
                    : new Resolution.ForMethod(methodDescription);
        }

        @Override
        public Resolution resolveDefault(TypeDescription typeDescription, MethodDescription.Token methodToken) {
            Map<MethodDescription.Token, MethodDescription> methodDescriptions = defaultMethods.get(typeDescription);
            if (methodDescriptions == null) {
                throw new IllegalArgumentException("Not a default method interface: " + typeDescription);
            }
            MethodDescription methodDescription = methodDescriptions.get(methodToken);
            return methodDescription == null
                    ? Resolution.Illegal.INSTANCE
                    : new Resolution.ForMethod(methodDescription);
        }

        @Override
        public List<MethodDescription> getInvokableMethods() {
            return invokeableMethods;
        }

        @Override
        public TypeDescription getTypeDescription() {
            return typeDescription;
        }
    }
}
