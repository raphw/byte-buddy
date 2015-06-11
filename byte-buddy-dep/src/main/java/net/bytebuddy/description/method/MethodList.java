package net.bytebuddy.description.method;

import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.matcher.FilterableList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Implementations represent a list of method descriptions.
 */
public interface MethodList extends FilterableList<MethodDescription, MethodList> {

    List<MethodDescription.Token> asTokenList();

    List<MethodDescription.Token> accept(GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor);

    abstract class AbstractBase extends FilterableList.AbstractBase<MethodDescription, MethodList> implements MethodList {

        @Override
        protected MethodList wrap(List<MethodDescription> values) {
            return new Explicit(values);
        }

        @Override
        public List<MethodDescription.Token> asTokenList() {
            return accept(GenericTypeDescription.Visitor.NoOp.INSTANCE);
        }

        @Override
        public List<MethodDescription.Token> accept(GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor) {
            List<MethodDescription.Token> tokens = new ArrayList<MethodDescription.Token>(size());
            for (MethodDescription methodDescription : this) {
                tokens.add(methodDescription.accept(visitor));
            }
            return tokens;
        }
    }

    /**
     * A method list implementation that returns all loaded byte code methods (methods and constructors) that
     * are declared for a given type.
     */
    class ForLoadedType extends AbstractBase {

        /**
         * The loaded methods that are represented by this method list.
         */
        private final List<? extends Method> methods;

        /**
         * The loaded constructors that are represented by this method list.
         */
        private final List<? extends Constructor<?>> constructors;

        /**
         * Creates a new list for a loaded type. Method descriptions are created on demand.
         *
         * @param type The type to be represented by this method list.
         */
        public ForLoadedType(Class<?> type) {
            this(type.getDeclaredConstructors(), type.getDeclaredMethods());
        }

        /**
         * Creates a method list that represents the given constructors and methods in their given order. The
         * constructors are assigned the indices before the methods.
         *
         * @param constructor The constructors to be represented by the method list.
         * @param method      The methods to be represented by the method list.
         */
        public ForLoadedType(Constructor<?>[] constructor, Method[] method) {
            this(Arrays.asList(constructor), Arrays.asList(method));
        }

        /**
         * Creates a method list that represents the given constructors and methods in their given order. The
         * constructors are assigned the indices before the methods.
         *
         * @param constructors The constructors to be represented by the method list.
         * @param methods      The methods to be represented by the method list.
         */
        public ForLoadedType(List<? extends Constructor<?>> constructors, List<? extends Method> methods) {
            this.constructors = constructors;
            this.methods = methods;
        }

        @Override
        public MethodDescription get(int index) {
            return index < constructors.size()
                    ? new MethodDescription.ForLoadedConstructor(constructors.get(index))
                    : new MethodDescription.ForLoadedMethod(methods.get(index - constructors.size()));

        }

        @Override
        public int size() {
            return constructors.size() + methods.size();
        }
    }

    /**
     * A method list that is a wrapper for a given list of method descriptions.
     */
    class Explicit extends AbstractBase {

        /**
         * The list of methods that is represented by this method list.
         */
        private final List<? extends MethodDescription> methodDescriptions;

        /**
         * Creates a new wrapper for a given list of methods.
         *
         * @param methodDescriptions The underlying list of methods used for this method list.
         */
        public Explicit(List<? extends MethodDescription> methodDescriptions) {
            this.methodDescriptions = Collections.unmodifiableList(methodDescriptions);
        }

        @Override
        public MethodDescription get(int index) {
            return methodDescriptions.get(index);
        }

        @Override
        public int size() {
            return methodDescriptions.size();
        }
    }

    /**
     * An implementation of an empty method list.
     */
    class Empty extends FilterableList.Empty<MethodDescription, MethodList> implements MethodList {

        @Override
        public List<MethodDescription.Token> asTokenList() {
            return Collections.emptyList();
        }

        @Override
        public List<MethodDescription.Token> accept(GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor) {
            return Collections.emptyList();
        }
    }
}
