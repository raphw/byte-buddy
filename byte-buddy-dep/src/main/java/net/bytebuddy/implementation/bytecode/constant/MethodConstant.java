/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.auxiliary.PrivilegedMemberLookupAction;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.TypeCreation;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import net.bytebuddy.implementation.bytecode.collection.ArrayFactory;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;

/**
 * Represents the creation of a {@link java.lang.reflect.Method} value which can be created from a given
 * set of constant pool values and can therefore be considered a constant in the broader meaning.
 */
public abstract class MethodConstant implements StackManipulation {

    /**
     * A description of the method to be loaded onto the stack.
     */
    protected final MethodDescription.InDefinedShape methodDescription;

    /**
     * Creates a new method constant.
     *
     * @param methodDescription The method description for which the {@link java.lang.reflect.Method} representation
     *                          should be created.
     */
    protected MethodConstant(MethodDescription.InDefinedShape methodDescription) {
        this.methodDescription = methodDescription;
    }

    /**
     * Creates a stack manipulation that loads a method constant onto the operand stack.
     *
     * @param methodDescription The method to be loaded onto the stack.
     * @return A stack manipulation that assigns a method constant for the given method description.
     */
    public static CanCache of(MethodDescription.InDefinedShape methodDescription) {
        if (methodDescription.isTypeInitializer()) {
            return CanCacheIllegal.INSTANCE;
        } else if (methodDescription.isConstructor()) {
            return new ForConstructor(methodDescription);
        } else {
            return new ForMethod(methodDescription);
        }
    }

    /**
     * Creates a stack manipulation that loads a method constant onto the operand stack using an {@link AccessController}.
     *
     * @param methodDescription The method to be loaded onto the stack.
     * @return A stack manipulation that assigns a method constant for the given method description.
     */
    public static CanCache ofPrivileged(MethodDescription.InDefinedShape methodDescription) {
        if (methodDescription.isTypeInitializer()) {
            return CanCacheIllegal.INSTANCE;
        } else if (methodDescription.isConstructor()) {
            return new ForConstructor(methodDescription).privileged();
        } else {
            return new ForMethod(methodDescription).privileged();
        }
    }

    /**
     * Returns a list of type constant load operations for the given list of parameters.
     *
     * @param parameterTypes A list of all type descriptions that should be represented as type constant
     *                       load operations.
     * @return A corresponding list of type constant load operations.
     */
    protected static List<StackManipulation> typeConstantsFor(List<TypeDescription> parameterTypes) {
        List<StackManipulation> typeConstants = new ArrayList<StackManipulation>(parameterTypes.size());
        for (TypeDescription parameterType : parameterTypes) {
            typeConstants.add(ClassConstant.of(parameterType));
        }
        return typeConstants;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValid() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        return new Compound(
                ClassConstant.of(methodDescription.getDeclaringType()),
                methodName(),
                ArrayFactory.forType(TypeDescription.Generic.OfNonGenericType.CLASS)
                        .withValues(typeConstantsFor(methodDescription.getParameters().asTypeList().asErasures())),
                MethodInvocation.invoke(accessorMethod())
        ).apply(methodVisitor, implementationContext);
    }

    /**
     * Returns a method constant that uses an {@link AccessController} to look up this constant.
     *
     * @return A method constant that uses an {@link AccessController} to look up this constant.
     */
    protected CanCache privileged() {
        return new PrivilegedLookup(methodDescription, methodName());
    }

    /**
     * Returns a stack manipulation that loads the method name onto the operand stack if this is required.
     *
     * @return A stack manipulation that loads the method name onto the operand stack if this is required.
     */
    protected abstract StackManipulation methodName();

    /**
     * Returns the method for loading a declared method or constructor onto the operand stack.
     *
     * @return The method for loading a declared method or constructor onto the operand stack.
     */
    protected abstract MethodDescription.InDefinedShape accessorMethod();

    @Override
    public int hashCode() {
        return methodDescription.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null || getClass() != other.getClass()) {
            return false;
        }
        MethodConstant methodConstant = (MethodConstant) other;
        return methodDescription.equals(methodConstant.methodDescription);
    }

    /**
     * Represents a method constant that cannot be represented by Java's reflection API.
     */
    protected enum CanCacheIllegal implements CanCache {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public StackManipulation cached() {
            return Illegal.INSTANCE;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isValid() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            return Illegal.INSTANCE.apply(methodVisitor, implementationContext);
        }
    }

    /**
     * Represents a {@link net.bytebuddy.implementation.bytecode.constant.MethodConstant} that is
     * directly loaded onto the operand stack without caching the value. Since the look-up of a Java method bares
     * some costs that sometimes need to be avoided, such a stack manipulation offers a convenience method for
     * defining this loading instruction as the retrieval of a field value that is initialized in the instrumented
     * type's type initializer.
     */
    public interface CanCache extends StackManipulation {

        /**
         * Returns this method constant as a cached version.
         *
         * @return A cached version of the method constant that is represented by this instance.
         */
        StackManipulation cached();
    }

    /**
     * Creates a {@link net.bytebuddy.implementation.bytecode.constant.MethodConstant} for loading
     * a {@link java.lang.reflect.Method} instance onto the operand stack.
     */
    protected static class ForMethod extends MethodConstant implements CanCache {

        /**
         * The {@link Class#getMethod(String, Class[])} method.
         */
        private static final MethodDescription.InDefinedShape GET_METHOD;

        /**
         * The {@link Class#getDeclaredMethod(String, Class[])} method.
         */
        private static final MethodDescription.InDefinedShape GET_DECLARED_METHOD;

        /*
         * Looks up methods used for creating the manipulation.
         */
        static {
            try {
                GET_METHOD = new MethodDescription.ForLoadedMethod(Class.class.getMethod("getMethod", String.class, Class[].class));
                GET_DECLARED_METHOD = new MethodDescription.ForLoadedMethod(Class.class.getMethod("getDeclaredMethod", String.class, Class[].class));
            } catch (NoSuchMethodException exception) {
                throw new IllegalStateException("Could not locate method lookup", exception);
            }
        }

        /**
         * Creates a new {@link net.bytebuddy.implementation.bytecode.constant.MethodConstant} for
         * creating a {@link java.lang.reflect.Method} instance.
         *
         * @param methodDescription The method to be loaded onto the stack.
         */
        protected ForMethod(MethodDescription.InDefinedShape methodDescription) {
            super(methodDescription);
        }

        @Override
        protected StackManipulation methodName() {
            return new TextConstant(methodDescription.getInternalName());
        }

        @Override
        protected MethodDescription.InDefinedShape accessorMethod() {
            return methodDescription.isPublic()
                    ? GET_METHOD
                    : GET_DECLARED_METHOD;
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation cached() {
            return new CachedMethod(this);
        }
    }

    /**
     * Creates a {@link net.bytebuddy.implementation.bytecode.constant.MethodConstant} for loading
     * a {@link java.lang.reflect.Constructor} instance onto the operand stack.
     */
    protected static class ForConstructor extends MethodConstant implements CanCache {

        /**
         * The {@link Class#getConstructor(Class[])} method.
         */
        private static final MethodDescription.InDefinedShape GET_CONSTRUCTOR;

        /**
         * The {@link Class#getDeclaredConstructor(Class[])} method.
         */
        private static final MethodDescription.InDefinedShape GET_DECLARED_CONSTRUCTOR;

        /*
         * Looks up the method used for creating the manipulation.
         */
        static {
            try {
                GET_CONSTRUCTOR = new MethodDescription.ForLoadedMethod(Class.class.getMethod("getConstructor", Class[].class));
                GET_DECLARED_CONSTRUCTOR = new MethodDescription.ForLoadedMethod(Class.class.getMethod("getDeclaredConstructor", Class[].class));
            } catch (NoSuchMethodException exception) {
                throw new IllegalStateException("Could not locate Class::getDeclaredConstructor", exception);
            }
        }

        /**
         * Creates a new {@link net.bytebuddy.implementation.bytecode.constant.MethodConstant} for
         * creating a {@link java.lang.reflect.Constructor} instance.
         *
         * @param methodDescription The constructor to be loaded onto the stack.
         */
        protected ForConstructor(MethodDescription.InDefinedShape methodDescription) {
            super(methodDescription);
        }

        @Override
        protected StackManipulation methodName() {
            return Trivial.INSTANCE;
        }

        @Override
        protected MethodDescription.InDefinedShape accessorMethod() {
            return methodDescription.isPublic()
                    ? GET_CONSTRUCTOR
                    : GET_DECLARED_CONSTRUCTOR;
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation cached() {
            return new CachedConstructor(this);
        }
    }

    /**
     * Performs a privileged lookup of a method constant by using an {@link AccessController}.
     */
    protected static class PrivilegedLookup implements StackManipulation, CanCache {

        /**
         * The {@link AccessController#doPrivileged(PrivilegedExceptionAction)} method.
         */
        private static final MethodDescription.InDefinedShape DO_PRIVILEGED;

        /*
         * Locates the access controller's do privileged method.
         */
        static {
            try {
                DO_PRIVILEGED = new MethodDescription.ForLoadedMethod(AccessController.class.getMethod("doPrivileged", PrivilegedExceptionAction.class));
            } catch (NoSuchMethodException exception) {
                throw new IllegalStateException("Cannot locate AccessController::doPrivileged", exception);
            }
        }

        /**
         * The method constant to load.
         */
        private final MethodDescription.InDefinedShape methodDescription;

        /**
         * The stack manipulation for locating the method name.
         */
        private final StackManipulation methodName;

        /**
         * Creates a new privileged lookup.
         *
         * @param methodDescription The method constant to load.
         * @param methodName        The stack manipulation for locating the method name.
         */
        protected PrivilegedLookup(MethodDescription.InDefinedShape methodDescription, StackManipulation methodName) {
            this.methodDescription = methodDescription;
            this.methodName = methodName;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isValid() {
            return methodName.isValid();
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            TypeDescription auxiliaryType = implementationContext.register(PrivilegedMemberLookupAction.of(methodDescription));
            return new Compound(
                    TypeCreation.of(auxiliaryType),
                    Duplication.SINGLE,
                    ClassConstant.of(methodDescription.getDeclaringType()),
                    methodName,
                    ArrayFactory.forType(TypeDescription.Generic.OfNonGenericType.CLASS)
                            .withValues(typeConstantsFor(methodDescription.getParameters().asTypeList().asErasures())),
                    MethodInvocation.invoke(auxiliaryType.getDeclaredMethods().filter(isConstructor()).getOnly()),
                    MethodInvocation.invoke(DO_PRIVILEGED),
                    TypeCasting.to(TypeDescription.ForLoadedType.of(methodDescription.isConstructor()
                            ? Constructor.class
                            : Method.class))
            ).apply(methodVisitor, implementationContext);
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation cached() {
            return methodDescription.isConstructor()
                    ? new CachedConstructor(this)
                    : new CachedMethod(this);
        }

        @Override
        public int hashCode() {
            return methodDescription.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (other == null || getClass() != other.getClass()) {
                return false;
            }
            PrivilegedLookup privilegedLookup = (PrivilegedLookup) other;
            return methodDescription.equals(privilegedLookup.methodDescription);
        }
    }

    /**
     * Represents a cached method for a {@link net.bytebuddy.implementation.bytecode.constant.MethodConstant}.
     */
    protected static class CachedMethod implements StackManipulation {

        /**
         * A description of the {@link java.lang.reflect.Method} type.
         */
        private static final TypeDescription METHOD_TYPE = TypeDescription.ForLoadedType.of(Method.class);

        /**
         * The stack manipulation that is represented by this caching wrapper.
         */
        private final StackManipulation methodConstant;

        /**
         * Creates a new cached {@link net.bytebuddy.implementation.bytecode.constant.MethodConstant}.
         *
         * @param methodConstant The method constant to store in the field cache.
         */
        protected CachedMethod(StackManipulation methodConstant) {
            this.methodConstant = methodConstant;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isValid() {
            return methodConstant.isValid();
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            return FieldAccess.forField(implementationContext.cache(methodConstant, METHOD_TYPE))
                    .read()
                    .apply(methodVisitor, implementationContext);
        }

        @Override
        public int hashCode() {
            return methodConstant.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (other == null || getClass() != other.getClass()) {
                return false;
            }
            CachedMethod cachedMethod = (CachedMethod) other;
            return methodConstant.equals(cachedMethod.methodConstant);
        }
    }

    /**
     * Represents a cached constructor for a {@link net.bytebuddy.implementation.bytecode.constant.MethodConstant}.
     */
    protected static class CachedConstructor implements StackManipulation {

        /**
         * A description of the {@link java.lang.reflect.Constructor} type.
         */
        private static final TypeDescription CONSTRUCTOR_TYPE = TypeDescription.ForLoadedType.of(Constructor.class);

        /**
         * The stack manipulation that is represented by this caching wrapper.
         */
        private final StackManipulation constructorConstant;

        /**
         * Creates a new cached {@link net.bytebuddy.implementation.bytecode.constant.MethodConstant}.
         *
         * @param constructorConstant The method constant to store in the field cache.
         */
        protected CachedConstructor(StackManipulation constructorConstant) {
            this.constructorConstant = constructorConstant;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isValid() {
            return constructorConstant.isValid();
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            return FieldAccess.forField(implementationContext.cache(constructorConstant, CONSTRUCTOR_TYPE))
                    .read()
                    .apply(methodVisitor, implementationContext);
        }

        @Override
        public int hashCode() {
            return constructorConstant.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (other == null || getClass() != other.getClass()) {
                return false;
            }
            CachedConstructor cachedConstructor = (CachedConstructor) other;
            return constructorConstant.equals(cachedConstructor.constructorConstant);
        }
    }
}
