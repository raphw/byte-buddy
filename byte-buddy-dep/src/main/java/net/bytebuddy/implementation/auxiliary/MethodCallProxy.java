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
package net.bytebuddy.implementation.auxiliary;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeVariableToken;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodAccessorFactory;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.TypeCreation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;

/**
 * A method call proxy represents a class that is compiled against a particular method which can then be called whenever
 * either its {@link java.util.concurrent.Callable#call()} or {@link Runnable#run()} method is called where the method
 * call proxy implements both interfaces.
 * <p>&nbsp;</p>
 * In order to do so, the method call proxy instances are constructed by providing all the necessary information for
 * calling a particular method:
 * <ol>
 * <li>If the target method is not {@code static}, the first argument should be an instance on which the method is called.</li>
 * <li>All arguments for the called method in the order in which they are required.</li>
 * </ol>
 */
@HashCodeAndEqualsPlugin.Enhance
public class MethodCallProxy implements AuxiliaryType {

    /**
     * The prefix of the fields holding the original method invocation's arguments.
     */
    private static final String FIELD_NAME_PREFIX = "argument";

    /**
     * The special method invocation to invoke from the auxiliary type.
     */
    private final Implementation.SpecialMethodInvocation specialMethodInvocation;

    /**
     * Determines if the generated proxy should be serializableProxy.
     */
    private final boolean serializableProxy;

    /**
     * The assigner to use for invoking a bridge method target where the parameter and return types need to be
     * assigned.
     */
    private final Assigner assigner;

    /**
     * Creates a new method call proxy for a given method and uses a default assigner for assigning the method's return
     * value to either the {@link java.util.concurrent.Callable#call()} or {@link Runnable#run()} method returns.
     *
     * @param specialMethodInvocation The special method invocation which should be invoked by this method call proxy.
     * @param serializableProxy       Determines if the generated proxy should be serializableProxy.
     */
    public MethodCallProxy(Implementation.SpecialMethodInvocation specialMethodInvocation, boolean serializableProxy) {
        this(specialMethodInvocation, serializableProxy, Assigner.DEFAULT);
    }

    /**
     * Creates a new method call proxy for a given method.
     *
     * @param specialMethodInvocation The special method invocation which should be invoked by this method call proxy.
     * @param serializableProxy       Determines if the generated proxy should be serializableProxy.
     * @param assigner                An assigner for assigning the target method's return value to either the
     *                                {@link java.util.concurrent.Callable#call()} or {@link Runnable#run()}} methods'
     *                                return values.
     */
    public MethodCallProxy(Implementation.SpecialMethodInvocation specialMethodInvocation, boolean serializableProxy, Assigner assigner) {
        this.specialMethodInvocation = specialMethodInvocation;
        this.serializableProxy = serializableProxy;
        this.assigner = assigner;
    }

    /**
     * Creates a linked hash map of field names to their types where each field represents a parameter of the method.
     *
     * @param methodDescription The method to extract into fields.
     * @return A map of fields in the order they need to be loaded onto the operand stack for invoking the original
     * method, including a reference to the instance of the instrumented type that is invoked if applicable.
     */
    private static LinkedHashMap<String, TypeDescription> extractFields(MethodDescription methodDescription) {
        LinkedHashMap<String, TypeDescription> typeDescriptions = new LinkedHashMap<String, TypeDescription>();
        int currentIndex = 0;
        if (!methodDescription.isStatic()) {
            typeDescriptions.put(fieldName(currentIndex++), methodDescription.getDeclaringType().asErasure());
        }
        for (ParameterDescription parameterDescription : methodDescription.getParameters()) {
            typeDescriptions.put(fieldName(currentIndex++), parameterDescription.getType().asErasure());
        }
        return typeDescriptions;
    }

    /**
     * Creates a field name for a method parameter of a given index.
     *
     * @param index The index for which the field name is to be created.
     * @return The name for the given parameter.
     */
    private static String fieldName(int index) {
        return FIELD_NAME_PREFIX + index;
    }

    /**
     * {@inheritDoc}
     */
    public String getSuffix() {
        return RandomString.hashOf(specialMethodInvocation.getMethodDescription().hashCode()) + (serializableProxy ? "S" : "0");
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType make(String auxiliaryTypeName,
                            ClassFileVersion classFileVersion,
                            MethodAccessorFactory methodAccessorFactory) {
        MethodDescription accessorMethod = methodAccessorFactory.registerAccessorFor(specialMethodInvocation, MethodAccessorFactory.AccessType.DEFAULT);
        LinkedHashMap<String, TypeDescription> parameterFields = extractFields(accessorMethod);
        DynamicType.Builder<?> builder = new ByteBuddy(classFileVersion)
                .with(TypeValidation.DISABLED)
                .with(PrecomputedMethodGraph.INSTANCE)
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(auxiliaryTypeName)
                .modifiers(DEFAULT_TYPE_MODIFIER)
                .implement(Runnable.class, Callable.class).intercept(new MethodCall(accessorMethod, assigner))
                .implement(serializableProxy ? new Class<?>[]{Serializable.class} : new Class<?>[0])
                .defineConstructor().withParameters(parameterFields.values())
                .intercept(ConstructorCall.INSTANCE);
        for (Map.Entry<String, TypeDescription> field : parameterFields.entrySet()) {
            builder = builder.defineField(field.getKey(), field.getValue(), Visibility.PRIVATE);
        }
        return builder.make();
    }

    /**
     * A precomputed method graph that only displays the methods that are relevant for creating a method call proxy.
     */
    protected enum PrecomputedMethodGraph implements MethodGraph.Compiler {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * The precomputed method graph.
         */
        private final transient MethodGraph.Linked methodGraph;

        /**
         * Creates the precomputed method graph.
         */
        PrecomputedMethodGraph() {
            LinkedHashMap<MethodDescription.SignatureToken, MethodGraph.Node> nodes = new LinkedHashMap<MethodDescription.SignatureToken, MethodGraph.Node>();
            MethodDescription callMethod = new MethodDescription.Latent(TypeDescription.ForLoadedType.of(Callable.class),
                    "call",
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                    Collections.<TypeVariableToken>emptyList(),
                    TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class),
                    Collections.<ParameterDescription.Token>emptyList(),
                    Collections.singletonList(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Exception.class)),
                    Collections.<AnnotationDescription>emptyList(),
                    AnnotationValue.UNDEFINED,
                    TypeDescription.Generic.UNDEFINED);
            nodes.put(callMethod.asSignatureToken(), new MethodGraph.Node.Simple(callMethod));
            MethodDescription runMethod = new MethodDescription.Latent(TypeDescription.ForLoadedType.of(Runnable.class),
                    "run",
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                    Collections.<TypeVariableToken>emptyList(),
                    TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(void.class),
                    Collections.<ParameterDescription.Token>emptyList(),
                    Collections.<TypeDescription.Generic>emptyList(),
                    Collections.<AnnotationDescription>emptyList(),
                    AnnotationValue.UNDEFINED,
                    TypeDescription.Generic.UNDEFINED);
            nodes.put(runMethod.asSignatureToken(), new MethodGraph.Node.Simple(runMethod));
            MethodGraph methodGraph = new MethodGraph.Simple(nodes);
            this.methodGraph = new MethodGraph.Linked.Delegation(methodGraph, methodGraph, Collections.<TypeDescription, MethodGraph>emptyMap());
        }

        /**
         * {@inheritDoc}
         */
        public MethodGraph.Linked compile(TypeDefinition typeDefinition) {
            return methodGraph;
        }

        /**
         * {@inheritDoc}
         */
        @Deprecated
        public MethodGraph.Linked compile(TypeDescription typeDescription) {
            return methodGraph;
        }

        /**
         * {@inheritDoc}
         */
        public MethodGraph.Linked compile(TypeDefinition typeDefinition, TypeDescription viewPoint) {
            return methodGraph;
        }

        /**
         * {@inheritDoc}
         */
        @Deprecated
        public MethodGraph.Linked compile(TypeDescription typeDefinition, TypeDescription viewPoint) {
            return methodGraph;
        }
    }

    /**
     * An implementation for a constructor of a {@link net.bytebuddy.implementation.auxiliary.MethodCallProxy}.
     */
    protected enum ConstructorCall implements Implementation {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * A reference of the {@link Object} type default constructor.
         */
        private final MethodDescription objectTypeDefaultConstructor;

        /**
         * Creates the constructor call singleton.
         */
        ConstructorCall() {
            objectTypeDefaultConstructor = TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isConstructor()).getOnly();
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
            return new Appender(implementationTarget.getInstrumentedType());
        }

        /**
         * The appender for implementing the {@link net.bytebuddy.implementation.auxiliary.MethodCallProxy.ConstructorCall}.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class Appender implements ByteCodeAppender {

            /**
             * The instrumented type being created.
             */
            private final TypeDescription instrumentedType;

            /**
             * Creates a new appender.
             *
             * @param instrumentedType The instrumented type that is being created.
             */
            private Appender(TypeDescription instrumentedType) {
                this.instrumentedType = instrumentedType;
            }

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                FieldList<?> fieldList = instrumentedType.getDeclaredFields();
                StackManipulation[] fieldLoading = new StackManipulation[fieldList.size()];
                int index = 0;
                for (FieldDescription fieldDescription : fieldList) {
                    fieldLoading[index] = new StackManipulation.Compound(
                            MethodVariableAccess.loadThis(),
                            MethodVariableAccess.load(instrumentedMethod.getParameters().get(index)),
                            FieldAccess.forField(fieldDescription).write()
                    );
                    index++;
                }
                StackManipulation.Size stackSize = new StackManipulation.Compound(
                        MethodVariableAccess.loadThis(),
                        MethodInvocation.invoke(ConstructorCall.INSTANCE.objectTypeDefaultConstructor),
                        new StackManipulation.Compound(fieldLoading),
                        MethodReturn.VOID
                ).apply(methodVisitor, implementationContext);
                return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
            }
        }
    }

    /**
     * An implementation for a method of a {@link net.bytebuddy.implementation.auxiliary.MethodCallProxy}.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class MethodCall implements Implementation {

        /**
         * The method that is accessed by the implemented method.
         */
        private final MethodDescription accessorMethod;

        /**
         * The assigner to be used for invoking the accessor method.
         */
        private final Assigner assigner;

        /**
         * Creates a new method call implementation.
         *
         * @param accessorMethod The method that is accessed by the implemented method.
         * @param assigner       The assigner to be used for invoking the accessor method.
         */
        protected MethodCall(MethodDescription accessorMethod, Assigner assigner) {
            this.accessorMethod = accessorMethod;
            this.assigner = assigner;
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
            return new Appender(implementationTarget.getInstrumentedType());
        }

        /**
         * The appender for implementing the {@link net.bytebuddy.implementation.auxiliary.MethodCallProxy.MethodCall}.
         */
        @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
        protected class Appender implements ByteCodeAppender {

            /**
             * The instrumented type that is implemented.
             */
            private final TypeDescription instrumentedType;

            /**
             * Creates a new appender.
             *
             * @param instrumentedType The instrumented type to be implemented.
             */
            private Appender(TypeDescription instrumentedType) {
                this.instrumentedType = instrumentedType;
            }

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor,
                              Context implementationContext,
                              MethodDescription instrumentedMethod) {
                FieldList<?> fieldList = instrumentedType.getDeclaredFields();
                List<StackManipulation> fieldLoadings = new ArrayList<StackManipulation>(fieldList.size());
                for (FieldDescription fieldDescription : fieldList) {
                    fieldLoadings.add(new StackManipulation.Compound(MethodVariableAccess.loadThis(), FieldAccess.forField(fieldDescription).read()));
                }
                StackManipulation.Size stackSize = new StackManipulation.Compound(
                        new StackManipulation.Compound(fieldLoadings),
                        MethodInvocation.invoke(accessorMethod),
                        assigner.assign(accessorMethod.getReturnType(), instrumentedMethod.getReturnType(), Assigner.Typing.DYNAMIC),
                        MethodReturn.of(instrumentedMethod.getReturnType())
                ).apply(methodVisitor, implementationContext);
                return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
            }
        }
    }

    /**
     * A stack manipulation that creates a {@link net.bytebuddy.implementation.auxiliary.MethodCallProxy}
     * for a given method an pushes such an object onto the call stack. For this purpose, all arguments of the proxied method
     * are loaded onto the stack what is only possible if this instance is used from a method with an identical signature such
     * as the target method itself.
     */
    @HashCodeAndEqualsPlugin.Enhance
    public static class AssignableSignatureCall extends StackManipulation.AbstractBase {

        /**
         * The special method invocation to be proxied by this stack manipulation.
         */
        private final Implementation.SpecialMethodInvocation specialMethodInvocation;

        /**
         * Determines if the generated proxy should be serializableProxy.
         */
        private final boolean serializable;

        /**
         * Creates an operand stack assignment that creates a
         * {@link net.bytebuddy.implementation.auxiliary.MethodCallProxy} for the
         * {@code targetMethod} and pushes this proxy object onto the stack.
         *
         * @param specialMethodInvocation The special method invocation which should be invoked by the created method
         *                                call proxy.
         * @param serializable            Determines if the generated proxy should be serializableProxy.
         */
        public AssignableSignatureCall(Implementation.SpecialMethodInvocation specialMethodInvocation,
                                       boolean serializable) {
            this.specialMethodInvocation = specialMethodInvocation;
            this.serializable = serializable;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            TypeDescription auxiliaryType = implementationContext
                    .register(new MethodCallProxy(specialMethodInvocation, serializable));
            return new Compound(
                    TypeCreation.of(auxiliaryType),
                    Duplication.SINGLE,
                    MethodVariableAccess.allArgumentsOf(specialMethodInvocation.getMethodDescription()).prependThisReference(),
                    MethodInvocation.invoke(auxiliaryType.getDeclaredMethods().filter(isConstructor()).getOnly())
            ).apply(methodVisitor, implementationContext);
        }
    }
}
