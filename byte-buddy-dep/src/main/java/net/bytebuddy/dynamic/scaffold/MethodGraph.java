package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;

import java.util.*;

public interface MethodGraph {

    Node find(MethodDescription.Token methodToken);

    List<Node> listNodes();

    MethodGraph getSuperGraph();

    Map<GenericTypeDescription, MethodGraph> getInterfaceGraphs();

    interface Node {

        MethodDescription getRepresentative();

        Set<MethodDescription.Token> getBridges();
    }

    interface Factory {

        MethodGraph make(TypeDescription typeDescription);

        class Default implements Factory {

            public static Factory forJavaLanguage() {
                return new Default(KeyFactory.ForJavaLanguage.INSTANCE);
            }

            private final KeyFactory<?> keyFactory;

            public Default(KeyFactory<?> keyFactory) {
                this.keyFactory = keyFactory;
            }

            @Override
            public MethodGraph make(TypeDescription typeDescription) {
                return null;
            }

            public interface KeyFactory<T> {

                T make(MethodDescription.Token methodToken);

                enum ForJavaLanguage implements KeyFactory<ForJavaLanguage.Key> {

                    INSTANCE;

                    @Override
                    public Key make(MethodDescription.Token methodToken) {
                        List<ParameterDescription.Token> parameterTokens = methodToken.getParameterTokens();
                        List<TypeDescription> rawParameterTypes = new ArrayList<TypeDescription>(parameterTokens.size());
                        for (ParameterDescription.Token parameterToken : parameterTokens) {
                            rawParameterTypes.add(parameterToken.getType().asRawType());
                        }
                        return new Key(methodToken.getInternalName(), rawParameterTypes);
                    }

                    public static class Key {

                        private final String internalName;

                        private final List<? extends TypeDescription> rawParameterTypes;

                        protected Key(String internalName, List<? extends TypeDescription> rawParameterTypes) {
                            this.internalName = internalName;
                            this.rawParameterTypes = rawParameterTypes;
                        }

                        @Override
                        public boolean equals(Object other) {
                            if (this == other) return true;
                            if (other == null || getClass() != other.getClass()) return false;
                            Key key = (Key) other;
                            return internalName.equals(key.internalName)
                                    && rawParameterTypes.equals(key.rawParameterTypes);
                        }

                        @Override
                        public int hashCode() {
                            int result = internalName.hashCode();
                            result = 31 * result + rawParameterTypes.hashCode();
                            return result;
                        }
                    }
                }
            }
        }
    }

    class Simple implements MethodGraph {

        private final MethodGraph superGraph;

        private final Map<GenericTypeDescription, MethodGraph> interfaceGraphs;

        protected Simple(MethodGraph superGraph, Map<GenericTypeDescription, MethodGraph> interfaceGraphs) {
            this.superGraph = superGraph;
            this.interfaceGraphs = interfaceGraphs;
        }

        @Override
        public Node find(MethodDescription.Token methodToken) {
            return null;
        }

        @Override
        public List<Node> listNodes() {
            return null;
        }

        @Override
        public MethodGraph getSuperGraph() {
            return superGraph;
        }

        @Override
        public Map<GenericTypeDescription, MethodGraph> getInterfaceGraphs() {
            return interfaceGraphs;
        }
    }
}
