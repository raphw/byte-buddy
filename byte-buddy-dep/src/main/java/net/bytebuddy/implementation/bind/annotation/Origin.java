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
package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.ClassConstant;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.constant.MethodConstant;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * <p>
 * The origin annotation provides some meta information about the source method that is bound to this method where
 * the binding is dependant of the parameter's type:
 * </p>
 * <ol>
 * <li>If the annotated parameter is of type {@link java.lang.reflect.Method}, {@link java.lang.reflect.Constructor} or
 * {@code java.lang.reflect.Executable}, the parameter is assigned a reference to the method or constructor it
 * instruments. If the reference is not assignable to the sort of the intercepted source, the target is not considered
 * for binding.</li>
 * <li>If the annotated parameter is of type {@link java.lang.Class}, the parameter is assigned a reference of the
 * type of the instrumented type.</li>
 * <li>If the annotated parameter is of type {@link java.lang.String}, the parameter is assigned a string with
 * the value that would be returned by the {@link Method#toString()} method.
 * </li>
 * <li>If the annotated parameter is a {@code int} type, it is assigned the intercepted method's modifiers.</li>
 * <li>If the annotated type is {@code java.lang.invoke.MethodHandle}, a handle of the intercepted method is injected.
 * A {@code java.lang.invoke.MethodHandle} is stored in a class's constant pool and does therefore not face the same
 * runtime performance limitations as a (non-cached) {@link java.lang.reflect.Method} reference. Method handles are
 * only supported for byte code versions starting from Java 7.</li>
 * <li>If the annotated type is {@code java.lang.invoke.MethodType}, a description of the intercepted method's type
 * is injected. Method type descriptions are only supported for byte code versions starting from Java 7.</li>
 * <li>If the annotated type is {@code java.lang.invoke.MethodHandles$Lookup}, a method handle lookup of the instrumented
 * class is returned. Method type descriptions are only supported for byte code versions starting from Java 7.</li>
 * </ol>
 * <p>
 * Any other parameter type will cause an {@link java.lang.IllegalStateException}.
 * </p>
 * <p>
 * <b>Important:</b> A method handle or method type reference can only be used if the referenced method's types are all visible
 * to the instrumented type or an {@link IllegalAccessError} will be thrown at runtime.
 * </p>
 * <p>
 * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.asm.Advice.Origin} or
 * {@link net.bytebuddy.asm.MemberSubstitution.Origin}. This annotation should be used with
 * {@link net.bytebuddy.implementation.MethodDelegation} only.
 * </p>
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Origin {

    /**
     * Determines if the value that is assigned by this annotation is cached. For values that can be stored in the constant pool,
     * this value is ignored as such values are cached implicitly. As a result, this value currently only affects caching of
     * {@link Method} instances.
     *
     * @return {@code true} if the value for this parameter should be cached in a {@code static} field inside the instrumented class.
     */
    boolean cache() default true;

    /**
     * Determines if the method should be resolved by using an {@code java.security.AccessController} using the privileges of the generated class.
     * Doing so requires the generation of an auxiliary class that implements {@code java.security.PrivilegedExceptionAction}.
     *
     * @return {@code true} if the class should be looked up using an {@code java.security.AccessController}.
     */
    boolean privileged() default false;

    /**
     * A binder for binding parameters that are annotated with {@link net.bytebuddy.implementation.bind.annotation.Origin}.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Origin> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * A description of the {@link Origin#cache()} method.
         */
        private static final MethodDescription.InDefinedShape CACHE;

        /**
         * A description of the {@link Origin#privileged()} method.
         */
        private static final MethodDescription.InDefinedShape PRIVILEGED;

        /*
         * Resolves annotation properties.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(Origin.class).getDeclaredMethods();
            CACHE = methods.filter(named("cache")).getOnly();
            PRIVILEGED = methods.filter(named("privileged")).getOnly();
        }

        /**
         * Loads a method constant onto the operand stack.
         *
         * @param annotation        The origin annotation.
         * @param methodDescription The method description to load.
         * @return An appropriate stack manipulation.
         */
        private static StackManipulation methodConstant(AnnotationDescription.Loadable<Origin> annotation, MethodDescription.InDefinedShape methodDescription) {
            MethodConstant.CanCache methodConstant = annotation.getValue(PRIVILEGED).resolve(Boolean.class)
                    ? MethodConstant.ofPrivileged(methodDescription)
                    : MethodConstant.of(methodDescription);
            return annotation.getValue(CACHE).resolve(Boolean.class)
                    ? methodConstant.cached()
                    : methodConstant;
        }

        /**
         * {@inheritDoc}
         */
        public Class<Origin> getHandledType() {
            return Origin.class;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<Origin> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            TypeDescription parameterType = target.getType().asErasure();
            if (parameterType.represents(Class.class)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(ClassConstant.of(implementationTarget.getOriginType().asErasure()));
            } else if (parameterType.represents(Method.class)) {
                return source.isMethod()
                        ? new MethodDelegationBinder.ParameterBinding.Anonymous(methodConstant(annotation, source.asDefined()))
                        : MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            } else if (parameterType.represents(Constructor.class)) {
                return source.isConstructor()
                        ? new MethodDelegationBinder.ParameterBinding.Anonymous(methodConstant(annotation, source.asDefined()))
                        : MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            } else if (JavaType.EXECUTABLE.getTypeStub().equals(parameterType)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(methodConstant(annotation, source.asDefined()));
            } else if (parameterType.represents(String.class)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(new TextConstant(source.toString()));
            } else if (parameterType.represents(int.class)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(IntegerConstant.forValue(source.getModifiers()));
            } else if (parameterType.equals(JavaType.METHOD_HANDLE.getTypeStub())) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(JavaConstant.MethodHandle.of(source.asDefined()).toStackManipulation());
            } else if (parameterType.equals(JavaType.METHOD_TYPE.getTypeStub())) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(JavaConstant.MethodType.of(source.asDefined()).toStackManipulation());
            } else if (parameterType.equals(JavaType.METHOD_HANDLES_LOOKUP.getTypeStub())) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(MethodInvocation.lookup());
            } else {
                throw new IllegalStateException("The " + target + " method's " + target.getIndex() +
                        " parameter is annotated with a Origin annotation with an argument not representing a Class," +
                        " Method, Constructor, String, int, MethodType or MethodHandle type");
            }
        }
    }
}
