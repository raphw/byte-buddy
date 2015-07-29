package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;

import java.util.*;

public interface MethodGraph {

    MethodDescription locate(MethodDescription.Token methodToken);

    interface Factory {

        MethodGraph make(TypeDescription typeDescription);

        class Default implements Factory {

            @Override
            public MethodGraph make(TypeDescription typeDescription) {
                return null;
            }

            protected static class KeyCollection {

                private final String internalName;

                private final Set<MethodDescription.Token> keys;

                protected KeyCollection(String internalName, Set<MethodDescription.Token> keys) {
                    this.internalName = internalName;
                    this.keys = keys;
                }

                @Override
                public boolean equals(Object other) {
                    return other == this || other instanceof KeyCollection
                            && !Collections.disjoint(keys, ((KeyCollection) other).keys);
                }

                @Override
                public int hashCode() {
                    return internalName.hashCode();
                }
            }
        }
    }
}
