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

import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.JavaConstantValue;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;

import java.lang.annotation.*;
import java.util.Arrays;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * <p>
 * Binds a method handle in the context of the instrumented method.
 * </p>
 * <p>
 * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.asm.Advice.Handle} or
 * {@link net.bytebuddy.asm.MemberSubstitution.Handle}. This annotation should be used with
 * {@link net.bytebuddy.implementation.MethodDelegation} only.
 * </p>
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Handle {

    /**
     * Returns the type of the method handle to resolve.
     *
     * @return The type of the method handle to resolve.
     */
    JavaConstant.MethodHandle.HandleType type();

    /**
     * Returns the owner type of the method handle, or {@code void}, to represent the instrumented type.
     *
     * @return The owner type of the method handle, or {@code void}, to represent the instrumented type.
     */
    Class<?> owner() default void.class;

    /**
     * Returns the name of the method handle.
     *
     * @return The name of the method handle.
     */
    String name();

    /**
     * Returns the return type of the method handle.
     *
     * @return The return type of the method handle.
     */
    Class<?> returnType();

    /**
     * Returns the parameter types of the method handle.
     *
     * @return The parameter types of the method handle.
     */
    Class<?>[] parameterTypes();

    /**
     * Binds a {@link Handle} annotation.
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Handle> {

        /**
         * The singleton instance.
         */
        INSTANCE;
        /**
         * The {@link MemberSubstitution.Handle#type()} method.
         */
        private static final MethodDescription.InDefinedShape HANDLE_TYPE;

        /**
         * The {@link MemberSubstitution.Handle#owner()} method.
         */
        private static final MethodDescription.InDefinedShape HANDLE_OWNER;

        /**
         * The {@link MemberSubstitution.Handle#name()} method.
         */
        private static final MethodDescription.InDefinedShape HANDLE_NAME;

        /**
         * The {@link MemberSubstitution.Handle#returnType()} method.
         */
        private static final MethodDescription.InDefinedShape HANDLE_RETURN_TYPE;

        /**
         * The {@link MemberSubstitution.Handle#parameterTypes()} method.
         */
        private static final MethodDescription.InDefinedShape HANDLE_PARAMETER_TYPES;

        /*
         * Initializes the methods of the annotation that is read by this binder.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(MemberSubstitution.Handle.class).getDeclaredMethods();
            HANDLE_TYPE = methods.filter(named("type")).getOnly();
            HANDLE_OWNER = methods.filter(named("owner")).getOnly();
            HANDLE_NAME = methods.filter(named("name")).getOnly();
            HANDLE_RETURN_TYPE = methods.filter(named("returnType")).getOnly();
            HANDLE_PARAMETER_TYPES = methods.filter(named("parameterTypes")).getOnly();
        }

        /**
         * {@inheritDoc}
         */
        public Class<Handle> getHandledType() {
            return Handle.class;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<Handle> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            if (!target.getType().asErasure().isAssignableTo(JavaType.METHOD_HANDLE.getTypeStub())) {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
            return new MethodDelegationBinder.ParameterBinding.Anonymous(new JavaConstantValue(new JavaConstant.MethodHandle(
                    annotation.getValue(HANDLE_TYPE).resolve(EnumerationDescription.class).load(JavaConstant.MethodHandle.HandleType.class),
                    annotation.getValue(HANDLE_OWNER).resolve(TypeDescription.class),
                    annotation.getValue(HANDLE_NAME).resolve(String.class),
                    annotation.getValue(HANDLE_RETURN_TYPE).resolve(TypeDescription.class),
                    Arrays.asList(annotation.getValue(HANDLE_PARAMETER_TYPES).resolve(TypeDescription[].class)))));
        }
    }
}
