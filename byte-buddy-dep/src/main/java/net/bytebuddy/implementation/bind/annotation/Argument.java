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
import net.bytebuddy.implementation.bind.ArgumentTypeResolver;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * <p>
 * Parameters that are annotated with this annotation will be assigned the value of the parameter of the source method
 * with the given parameter. For example, if source method {@code foo(String, Integer)} is bound to target method
 * {@code bar(@Argument(1) Integer)}, the second parameter of {@code foo} will be bound to the first argument of
 * {@code bar}.
 * </p>
 * <p>
 * If a source method has less parameters than specified by {@link Argument#value()}, the method carrying this parameter
 * annotation is excluded from the list of possible binding candidates to this particular source method. The same happens,
 * if the source method parameter at the specified index is not assignable to the annotated parameter.
 * </p>
 * <p>
 * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.asm.Advice.Argument} or
 * {@link net.bytebuddy.asm.MemberSubstitution.Argument}. This annotation should be used with
 * {@link net.bytebuddy.implementation.MethodDelegation} only.
 * </p>
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 * @see net.bytebuddy.implementation.bind.annotation.RuntimeType
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Argument {

    /**
     * The index of the parameter of the source method that should be bound to this parameter.
     *
     * @return The required parameter index.
     */
    int value();

    /**
     * Determines if the argument binding is to be considered by a
     * {@link net.bytebuddy.implementation.bind.ArgumentTypeResolver}
     * for resolving ambiguous bindings of two methods. If
     * {@link net.bytebuddy.implementation.bind.annotation.Argument.BindingMechanic#UNIQUE},
     * of two bindable target methods such as for example {@code foo(String)} and {@code bar(Object)}, the {@code foo}
     * method would be considered as dominant over the {@code bar} method because of its more specific argument type. As
     * a side effect, only one parameter of any target method can be bound to a source method parameter with a given
     * index unless the {@link net.bytebuddy.implementation.bind.annotation.Argument.BindingMechanic#ANONYMOUS}
     * option is used for any other binding.
     *
     * @return The binding type that should be applied to this parameter binding.
     * @see net.bytebuddy.implementation.bind.ArgumentTypeResolver
     */
    BindingMechanic bindingMechanic() default BindingMechanic.UNIQUE;

    /**
     * Determines if a parameter binding should be considered for resolving ambiguous method bindings.
     *
     * @see Argument#bindingMechanic()
     * @see net.bytebuddy.implementation.bind.ArgumentTypeResolver
     */
    enum BindingMechanic {

        /**
         * The binding is unique, i.e. only one such binding must be present among all parameters of a method. As a
         * consequence, the binding can be latter identified by an
         * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver}.
         */
        UNIQUE {
            @Override
            protected MethodDelegationBinder.ParameterBinding<?> makeBinding(TypeDescription.Generic source,
                                                                             TypeDescription.Generic target,
                                                                             int sourceParameterIndex,
                                                                             Assigner assigner,
                                                                             Assigner.Typing typing,
                                                                             int parameterOffset) {
                return MethodDelegationBinder.ParameterBinding.Unique.of(
                        new StackManipulation.Compound(
                                MethodVariableAccess.of(source).loadFrom(parameterOffset),
                                assigner.assign(source, target, typing)),
                        new ArgumentTypeResolver.ParameterIndexToken(sourceParameterIndex)
                );
            }
        },

        /**
         * The binding is anonymous, i.e. it can be present on several parameters of the same method.
         */
        ANONYMOUS {
            @Override
            protected MethodDelegationBinder.ParameterBinding<?> makeBinding(TypeDescription.Generic source,
                                                                             TypeDescription.Generic target,
                                                                             int sourceParameterIndex,
                                                                             Assigner assigner,
                                                                             Assigner.Typing typing,
                                                                             int parameterOffset) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(
                        new StackManipulation.Compound(MethodVariableAccess.of(source).loadFrom(parameterOffset), assigner.assign(source, target, typing))
                );
            }
        };

        /**
         * Creates a binding that corresponds to this binding mechanic.
         *
         * @param source               The source type to be bound.
         * @param target               The target type the {@code sourceType} is to be bound to.
         * @param sourceParameterIndex The index of the source parameter.
         * @param assigner             The assigner that is used to perform the assignment.
         * @param typing               Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @param parameterOffset      The offset of the source method's parameter.
         * @return A binding considering the chosen binding mechanic.
         */
        protected abstract MethodDelegationBinder.ParameterBinding<?> makeBinding(TypeDescription.Generic source,
                                                                                  TypeDescription.Generic target,
                                                                                  int sourceParameterIndex,
                                                                                  Assigner assigner,
                                                                                  Assigner.Typing typing,
                                                                                  int parameterOffset);
    }

    /**
     * A binder for handling the
     * {@link net.bytebuddy.implementation.bind.annotation.Argument}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Argument> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * A description of the {@link Argument#value()} method.
         */
        private static final MethodDescription.InDefinedShape VALUE;

        /**
         * A description of the {@link Argument#bindingMechanic()} method.
         */
        private static final MethodDescription.InDefinedShape BINDING_MECHANIC;

        /*
         * Resolves annotation properties.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(Argument.class).getDeclaredMethods();
            VALUE = methods.filter(named("value")).getOnly();
            BINDING_MECHANIC = methods.filter(named("bindingMechanic")).getOnly();
        }

        /**
         * {@inheritDoc}
         */
        public Class<Argument> getHandledType() {
            return Argument.class;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<Argument> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            if (annotation.getValue(VALUE).resolve(Integer.class) < 0) {
                throw new IllegalArgumentException("@Argument annotation on " + target + " specifies negative index");
            } else if (source.getParameters().size() <= annotation.getValue(VALUE).resolve(Integer.class)) {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
            return annotation.getValue(BINDING_MECHANIC)
                    .load(Argument.class.getClassLoader())
                    .resolve(BindingMechanic.class)
                    .makeBinding(source.getParameters().get(annotation.getValue(VALUE).resolve(Integer.class)).getType(),
                            target.getType(),
                            annotation.getValue(VALUE).resolve(Integer.class),
                            assigner,
                            typing,
                            source.getParameters().get(annotation.getValue(VALUE).resolve(Integer.class)).getOffset());
        }
    }
}
