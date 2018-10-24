/*
 * Copyright 2014 - 2018 Rafael Winterhalter
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
package net.bytebuddy.implementation;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import org.objectweb.asm.MethodVisitor;

import java.util.*;

/**
 * This {@link Implementation} invokes a default method for the methods it instruments.
 * A default method is potentially ambiguous if a method of identical signature is defined in several interfaces.
 * For this reason, this implementation allows for the specification of <i>prioritized interfaces</i> whose default
 * methods, if a method of equivalent signature is defined for a specific interface. All prioritized interfaces are
 * searched for default methods in the order of their specification. If no prioritized interface defines a default method
 * of equivalent signature to the given instrumented method, any non-prioritized interface is searched for a suitable
 * default method. If exactly one of those interfaces defines a suitable default method, this method is invoked.
 * If no method or more than one method is identified as a suitable default method, an exception is thrown.
 * <p>&nbsp;</p>
 * When it comes to default methods, the Java programming language specifies stronger requirements for the
 * legitimacy of invoking a default method than the Java runtime. The Java compiler requires a method to be
 * the most specific method in its defining type's type hierarchy, i.e. the method must not be overridden by another
 * interface or class type. Additionally, the method must be invokable from an interface type which is directly
 * implemented by the instrumented type. The Java runtime only requires the second condition to be fulfilled which
 * is why this implementation only checks the later condition, as well.
 */
@HashCodeAndEqualsPlugin.Enhance
public class DefaultMethodCall implements Implementation {

    /**
     * A list of prioritized interfaces in the order in which a method should be attempted to be called.
     */
    private final List<TypeDescription> prioritizedInterfaces;

    /**
     * Creates a new {@link net.bytebuddy.implementation.DefaultMethodCall} implementation for a given list of
     * prioritized interfaces.
     *
     * @param prioritizedInterfaces A list of prioritized interfaces in the order in which a method should be attempted to
     *                              be called.
     */
    protected DefaultMethodCall(List<TypeDescription> prioritizedInterfaces) {
        this.prioritizedInterfaces = prioritizedInterfaces;
    }

    /**
     * Creates a {@link net.bytebuddy.implementation.DefaultMethodCall} implementation which searches the given list
     * of interface types for a suitable default method in their order. If no such prioritized interface is suitable,
     * because it is either not defined on the instrumented type or because it does not define a suitable default method,
     * any remaining interface is searched for a suitable default method. If no or more than one method defines a
     * suitable default method, an exception is thrown.
     *
     * @param prioritizedInterface A list of prioritized default method interfaces in their prioritization order.
     * @return An implementation which calls an instrumented method's compatible default method that considers the given
     * interfaces to be prioritized in their order.
     */
    public static Implementation prioritize(Class<?>... prioritizedInterface) {
        return prioritize(new TypeList.ForLoadedTypes(prioritizedInterface));
    }

    /**
     * Creates a {@link net.bytebuddy.implementation.DefaultMethodCall} implementation which searches the given list
     * of interface types for a suitable default method in their order. If no such prioritized interface is suitable,
     * because it is either not defined on the instrumented type or because it does not define a suitable default method,
     * any remaining interface is searched for a suitable default method. If no or more than one method defines a
     * suitable default method, an exception is thrown.
     *
     * @param prioritizedInterfaces A list of prioritized default method interfaces in their prioritization order.
     * @return An implementation which calls an instrumented method's compatible default method that considers the given
     * interfaces to be prioritized in their order.
     */
    public static Implementation prioritize(Iterable<? extends Class<?>> prioritizedInterfaces) {
        List<Class<?>> list = new ArrayList<Class<?>>();
        for (Class<?> prioritizedInterface : prioritizedInterfaces) {
            list.add(prioritizedInterface);
        }
        return prioritize(new TypeList.ForLoadedTypes(list));
    }

    /**
     * Creates a {@link net.bytebuddy.implementation.DefaultMethodCall} implementation which searches the given list
     * of interface types for a suitable default method in their order. If no such prioritized interface is suitable,
     * because it is either not defined on the instrumented type or because it does not define a suitable default method,
     * any remaining interface is searched for a suitable default method. If no or more than one method defines a
     * suitable default method, an exception is thrown.
     *
     * @param prioritizedInterface A list of prioritized default method interfaces in their prioritization order.
     * @return An implementation which calls an instrumented method's compatible default method that considers the given
     * interfaces to be prioritized in their order.
     */
    public static Implementation prioritize(TypeDescription... prioritizedInterface) {
        return prioritize(Arrays.asList(prioritizedInterface));
    }

    /**
     * Creates a {@link net.bytebuddy.implementation.DefaultMethodCall} implementation which searches the given list
     * of interface types for a suitable default method in their order. If no such prioritized interface is suitable,
     * because it is either not defined on the instrumented type or because it does not define a suitable default method,
     * any remaining interface is searched for a suitable default method. If no or more than one method defines a
     * suitable default method, an exception is thrown.
     *
     * @param prioritizedInterfaces A collection of prioritized default method interfaces in their prioritization order.
     * @return An implementation which calls an instrumented method's compatible default method that considers the given
     * interfaces to be prioritized in their order.
     */
    public static Implementation prioritize(Collection<? extends TypeDescription> prioritizedInterfaces) {
        return new DefaultMethodCall(new ArrayList<TypeDescription>(prioritizedInterfaces));
    }

    /**
     * Creates a {@link net.bytebuddy.implementation.DefaultMethodCall} implementation without prioritizing any
     * interface. Instead, any interface that is defined for a given type is searched for a suitable default method. If
     * exactly one interface defines a suitable default method, this method is invoked from the instrumented method.
     * Otherwise, an exception is thrown.
     *
     * @return An implementation which calls an instrumented method's compatible default method if such a method
     * is unambiguous.
     */
    public static Implementation unambiguousOnly() {
        return new DefaultMethodCall(Collections.<TypeDescription>emptyList());
    }

    /**
     * {@inheritDoc}
     */
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
    }

    /**
     * {@inheritDoc}
     */
    public ByteCodeAppender appender(Target implementationTarget) {
        return new Appender(implementationTarget, filterRelevant(implementationTarget.getInstrumentedType()));
    }

    /**
     * Filters the relevant prioritized interfaces for a given type, i.e. finds the types that are
     * directly declared on a given instrumented type.
     *
     * @param typeDescription The instrumented type for which the prioritized interfaces are to be filtered.
     * @return A list of prioritized interfaces that are additionally implemented by the given type.
     */
    private List<TypeDescription> filterRelevant(TypeDescription typeDescription) {
        List<TypeDescription> filtered = new ArrayList<TypeDescription>(prioritizedInterfaces.size());
        Set<TypeDescription> relevant = new HashSet<TypeDescription>(typeDescription.getInterfaces().asErasures());
        for (TypeDescription prioritizedInterface : prioritizedInterfaces) {
            if (relevant.remove(prioritizedInterface)) {
                filtered.add(prioritizedInterface);
            }
        }
        return filtered;
    }

    /**
     * The appender for implementing a {@link net.bytebuddy.implementation.DefaultMethodCall}.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class Appender implements ByteCodeAppender {

        /**
         * The implementation target of this appender.
         */
        private final Target implementationTarget;

        /**
         * The relevant prioritized interfaces to be considered by this appender.
         */
        private final List<TypeDescription> prioritizedInterfaces;

        /**
         * The relevant non-prioritized interfaces to be considered by this appender.
         */
        @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.IGNORE)
        private final Set<TypeDescription> nonPrioritizedInterfaces;

        /**
         * Creates a new appender for implementing a {@link net.bytebuddy.implementation.DefaultMethodCall}.
         *
         * @param implementationTarget  The implementation target of this appender.
         * @param prioritizedInterfaces The relevant prioritized interfaces to be considered by this appender.
         */
        protected Appender(Target implementationTarget, List<TypeDescription> prioritizedInterfaces) {
            this.implementationTarget = implementationTarget;
            this.prioritizedInterfaces = prioritizedInterfaces;
            this.nonPrioritizedInterfaces = new HashSet<TypeDescription>(implementationTarget.getInstrumentedType().getInterfaces().asErasures());
            nonPrioritizedInterfaces.removeAll(prioritizedInterfaces);
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            StackManipulation defaultMethodInvocation = locateDefault(instrumentedMethod);
            if (!defaultMethodInvocation.isValid()) {
                throw new IllegalStateException("Cannot invoke default method on " + instrumentedMethod);
            }
            StackManipulation.Size stackSize = new StackManipulation.Compound(
                    MethodVariableAccess.allArgumentsOf(instrumentedMethod).prependThisReference(),
                    defaultMethodInvocation,
                    MethodReturn.of(instrumentedMethod.getReturnType())
            ).apply(methodVisitor, implementationContext);
            return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
        }

        /**
         * Locates a special method invocation to be invoked from a given method.
         *
         * @param methodDescription The method that is currently instrumented.
         * @return A potentially illegal stack manipulation representing the default method invocation for the
         * given method.
         */
        private StackManipulation locateDefault(MethodDescription methodDescription) {
            MethodDescription.SignatureToken methodToken = methodDescription.asSignatureToken();
            SpecialMethodInvocation specialMethodInvocation = SpecialMethodInvocation.Illegal.INSTANCE;
            for (TypeDescription typeDescription : prioritizedInterfaces) {
                specialMethodInvocation = implementationTarget.invokeDefault(methodToken, typeDescription);
                if (specialMethodInvocation.isValid()) {
                    return specialMethodInvocation;
                }
            }
            for (TypeDescription typeDescription : nonPrioritizedInterfaces) {
                SpecialMethodInvocation other = implementationTarget.invokeDefault(methodToken, typeDescription);
                if (specialMethodInvocation.isValid() && other.isValid()) {
                    throw new IllegalStateException(methodDescription + " has an ambiguous default method with "
                            + other.getMethodDescription() + " and " + specialMethodInvocation.getMethodDescription());
                }
                specialMethodInvocation = other;
            }
            return specialMethodInvocation;
        }
    }
}
