package net.bytebuddy.implementation;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.FieldLocator;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Field;

/**
 * <p>
 * Defines a method to access a given field by following the Java bean conventions for getters and setters: 通过遵循getter和setter的Java bean约定，定义访问给定字段的方法
 * </p>
 * <ul>
 * <li>Getter: A method named {@code getFoo()} will be instrumented to read and return the value of a field {@code foo}
 * or another field if one was specified explicitly. If a property is of type {@link java.lang.Boolean} or
 * {@code boolean}, the name {@code isFoo()} is also permitted.</li>
 * <li>Setter: A method named {@code setFoo(value)} will be instrumented to write the given argument {@code value}
 * to a field {@code foo} or to another field if one was specified explicitly.</li>
 * </ul>
 * <p>
 * Field accessors always implement a getter if a non-{@code void} value is returned from a method and attempt to define a setter
 * otherwise. If a field accessor is not explicitly defined as a setter via {@link PropertyConfigurable}, an instrumented
 * method must define exactly one parameter. Using the latter API, an explicit parameter index can be defined and a return
 * value can be specified explicitly when {@code void} is not returned.
 * </p>
 */
@HashCodeAndEqualsPlugin.Enhance
public abstract class FieldAccessor implements Implementation {

    /**
     * The field's location. 字段的位置
     */
    protected final FieldLocation fieldLocation;

    /**
     * The assigner to use.
     */
    protected final Assigner assigner;

    /**
     * Indicates if dynamic type castings should be attempted for incompatible assignments. 指示是否应尝试对不兼容的分配进行动态类型转换
     */
    protected final Assigner.Typing typing;

    /**
     * Creates a new field accessor.
     *
     * @param fieldLocation The field's location.
     * @param assigner      The assigner to use.
     * @param typing        Indicates if dynamic type castings should be attempted for incompatible assignments.
     */
    protected FieldAccessor(FieldLocation fieldLocation, Assigner assigner, Assigner.Typing typing) {
        this.fieldLocation = fieldLocation;
        this.assigner = assigner;
        this.typing = typing;
    }

    /**
     * Defines a field accessor where any access is targeted to a field named {@code name}. 定义一个字段访问器，其中任何访问都指向名为{@code name}的字段
     *
     * @param name The name of the field to be accessed. 要访问的字段的名称
     * @return A field accessor for a field of a given name. 给定名称字段的字段访问器
     */
    public static OwnerTypeLocatable ofField(String name) {
        return of(new FieldNameExtractor.ForFixedValue(name));
    }

    /**
     * Defines a field accessor where any access is targeted to a field that matches the methods
     * name with the Java specification for bean properties, i.e. a method {@code getFoo} or {@code setFoo(value)}
     * will either read or write a field named {@code foo}. 定义一个字段访问器，其中任何访问都指向与bean属性的Java规范匹配的方法名的字段，即方法{@code getFoo}或{@code setFoo(value)}将读取或写入名为{@code foo}的字段
     *
     * @return A field accessor that follows the Java naming conventions for bean properties.
     */
    public static OwnerTypeLocatable ofBeanProperty() {
        return of(FieldNameExtractor.ForBeanProperty.INSTANCE);
    }

    /**
     * Defines a custom strategy for determining the field that is accessed by this field accessor. 定义用于确定此字段访问器访问的字段的自定义策略
     *
     * @param fieldNameExtractor The field name extractor to use.
     * @return A field accessor using the given field name extractor.
     */
    public static OwnerTypeLocatable of(FieldNameExtractor fieldNameExtractor) {
        return new ForImplicitProperty(new FieldLocation.Relative(fieldNameExtractor));
    }

    /**
     * Defines a field accessor where the specified field is accessed. The field must be within the hierarchy of the instrumented type. 定义访问指定字段的字段访问器。该字段必须位于检测类型的层次结构中
     *
     * @param field The field being accessed.
     * @return A field accessor for the given field.
     */
    public static AssignerConfigurable of(Field field) {
        return of(new FieldDescription.ForLoadedField(field));
    }

    /**
     * Defines a field accessor where the specified field is accessed. The field must be within the hierarchy of the instrumented type.
     *
     * @param fieldDescription The field being accessed.
     * @return A field accessor for the given field.
     */
    public static AssignerConfigurable of(FieldDescription fieldDescription) {
        return new ForImplicitProperty(new FieldLocation.Absolute(fieldDescription));
    }

    /**
     * Creates a getter getter.
     *
     * @param fieldDescription   The field to read the value from.
     * @param instrumentedMethod The getter method.
     * @return A stack manipulation that gets the field's value.
     */
    protected StackManipulation getter(FieldDescription fieldDescription, MethodDescription instrumentedMethod) {
        return access(fieldDescription, instrumentedMethod, new StackManipulation.Compound(FieldAccess.forField(fieldDescription).read(),
                assigner.assign(fieldDescription.getType(), instrumentedMethod.getReturnType(), typing)));
    }

    /**
     * Creates a setter instruction. 创建setter指令
     *
     * @param fieldDescription     The field to set a value for.
     * @param parameterDescription The parameter for what value is to be set.
     * @return A stack manipulation that sets the field's value.
     */
    protected StackManipulation setter(FieldDescription fieldDescription, ParameterDescription parameterDescription) {
        if (fieldDescription.isFinal() && parameterDescription.getDeclaringMethod().isMethod()) {
            throw new IllegalArgumentException("Cannot set final field " + fieldDescription + " from " + parameterDescription.getDeclaringMethod());
        }
        return access(fieldDescription,
                parameterDescription.getDeclaringMethod(),
                new StackManipulation.Compound(MethodVariableAccess.load(parameterDescription),
                        assigner.assign(parameterDescription.getType(), fieldDescription.getType(), typing),
                        FieldAccess.forField(fieldDescription).write()));
    }

    /**
     * Checks a field access and loads the {@code this} instance if necessary.
     *
     * @param fieldDescription   The field to get a value
     * @param instrumentedMethod The instrumented method.
     * @param fieldAccess        A stack manipulation describing the field access.
     * @return An appropriate stack manipulation.
     */
    private StackManipulation access(FieldDescription fieldDescription, MethodDescription instrumentedMethod, StackManipulation fieldAccess) {
        if (!fieldAccess.isValid()) {
            throw new IllegalStateException("Incompatible type of " + fieldDescription + " and " + instrumentedMethod);
        } else if (instrumentedMethod.isStatic() && !fieldDescription.isStatic()) {
            throw new IllegalArgumentException("Cannot call instance field " + fieldDescription + " from static method " + instrumentedMethod);
        }
        return new StackManipulation.Compound(fieldDescription.isStatic()
                ? StackManipulation.Trivial.INSTANCE
                : MethodVariableAccess.loadThis(), fieldAccess);
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
    }

    /**
     * A field location represents an identified field description which depends on the instrumented type and method. 字段位置表示标识的字段描述，该描述取决于插入指令的类型和方法
     */
    protected interface FieldLocation {

        /**
         * Specifies a field locator factory to use. 指定要使用的字段定位器工厂
         *
         * @param fieldLocatorFactory The field locator factory to use.
         * @return An appropriate field location.
         */
        FieldLocation with(FieldLocator.Factory fieldLocatorFactory);

        /**
         * A prepared field location. 准备好的现场位置
         *
         * @param instrumentedType The instrumented type.
         * @return A prepared field location.
         */
        Prepared prepare(TypeDescription instrumentedType);

        /**
         * A prepared field location.
         */
        interface Prepared {

            /**
             * Resolves the field description to use.
             *
             * @param instrumentedMethod The instrumented method.
             * @return The resolved field description.
             */
            FieldDescription resolve(MethodDescription instrumentedMethod);
        }

        /**
         * An absolute field description representing a previously resolved field. 表示先前解析字段的绝对字段描述s
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Absolute implements FieldLocation, Prepared {

            /**
             * The field description.
             */
            private final FieldDescription fieldDescription;

            /**
             * Creates an absolute field location.
             *
             * @param fieldDescription The field description.
             */
            protected Absolute(FieldDescription fieldDescription) {
                this.fieldDescription = fieldDescription;
            }

            @Override
            public FieldLocation with(FieldLocator.Factory fieldLocatorFactory) {
                throw new IllegalStateException("Cannot specify a field locator factory for an absolute field location");
            }

            @Override
            public Prepared prepare(TypeDescription instrumentedType) {
                if (!instrumentedType.isAssignableTo(fieldDescription.getDeclaringType().asErasure())) {
                    throw new IllegalStateException(fieldDescription + " is not declared by " + instrumentedType);
                } else if (!fieldDescription.isVisibleTo(instrumentedType)) {
                    throw new IllegalStateException("Cannot access " + fieldDescription + " from " + instrumentedType);
                }
                return this;
            }

            @Override
            public FieldDescription resolve(MethodDescription instrumentedMethod) {
                return fieldDescription;
            }
        }

        /**
         * A relative field location where a field is located dynamically. 字段动态定位的相对字段位置
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Relative implements FieldLocation {

            /**
             * The field name extractor to use.
             */
            private final FieldNameExtractor fieldNameExtractor;

            /**
             * The field locator factory to use.
             */
            private final FieldLocator.Factory fieldLocatorFactory;

            /**
             * Creates a new relative field location.
             *
             * @param fieldNameExtractor The field name extractor to use.
             */
            protected Relative(FieldNameExtractor fieldNameExtractor) {
                this(fieldNameExtractor, FieldLocator.ForClassHierarchy.Factory.INSTANCE);
            }

            /**
             * Creates a new relative field location.
             *
             * @param fieldNameExtractor  The field name extractor to use.
             * @param fieldLocatorFactory The field locator factory to use.
             */
            private Relative(FieldNameExtractor fieldNameExtractor, FieldLocator.Factory fieldLocatorFactory) {
                this.fieldNameExtractor = fieldNameExtractor;
                this.fieldLocatorFactory = fieldLocatorFactory;
            }

            @Override
            public FieldLocation with(FieldLocator.Factory fieldLocatorFactory) {
                return new Relative(fieldNameExtractor, fieldLocatorFactory);
            }

            @Override
            public FieldLocation.Prepared prepare(TypeDescription instrumentedType) {
                return new Prepared(fieldNameExtractor, fieldLocatorFactory.make(instrumentedType));
            }

            /**
             * A prepared version of a field location.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class Prepared implements FieldLocation.Prepared {

                /**
                 * The field name extractor to use.
                 */
                private final FieldNameExtractor fieldNameExtractor;

                /**
                 * The field locator factory to use.
                 */
                private final FieldLocator fieldLocator;

                /**
                 * Creates a new relative field location.
                 *
                 * @param fieldNameExtractor The field name extractor to use.
                 * @param fieldLocator       The field locator to use.
                 */
                protected Prepared(FieldNameExtractor fieldNameExtractor, FieldLocator fieldLocator) {
                    this.fieldNameExtractor = fieldNameExtractor;
                    this.fieldLocator = fieldLocator;
                }

                @Override
                public FieldDescription resolve(MethodDescription instrumentedMethod) {
                    FieldLocator.Resolution resolution = fieldLocator.locate(fieldNameExtractor.resolve(instrumentedMethod));
                    if (!resolution.isResolved()) {
                        throw new IllegalStateException("Cannot resolve field for " + instrumentedMethod + " using " + fieldLocator);
                    }
                    return resolution.getField();
                }
            }
        }
    }

    /**
     * A field name extractor is responsible for determining a field name to a method that is implemented
     * to access this method. 字段名提取器负责确定实现为访问此方法的方法的字段名
     */
    public interface FieldNameExtractor {

        /**
         * Extracts a field name to be accessed by a getter or setter method. 提取要由getter或setter方法访问的字段名
         *
         * @param methodDescription The method for which a field name is to be determined.
         * @return The name of the field to be accessed by this method.
         */
        String resolve(MethodDescription methodDescription);

        /**
         * A {@link net.bytebuddy.implementation.FieldAccessor.FieldNameExtractor} that determines a field name
         * according to the rules of Java bean naming conventions.
         */
        enum ForBeanProperty implements FieldNameExtractor {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public String resolve(MethodDescription methodDescription) {
                String name = methodDescription.getInternalName();
                int crop;
                if (name.startsWith("get") || name.startsWith("set")) {
                    crop = 3;
                } else if (name.startsWith("is")) {
                    crop = 2;
                } else {
                    throw new IllegalArgumentException(methodDescription + " does not follow Java bean naming conventions");
                }
                name = name.substring(crop);
                if (name.length() == 0) {
                    throw new IllegalArgumentException(methodDescription + " does not specify a bean name");
                }
                return Character.toLowerCase(name.charAt(0)) + name.substring(1);
            }
        }

        /**
         * A field name extractor that returns a fixed value. 返回固定值的字段名提取器
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForFixedValue implements FieldNameExtractor {

            /**
             * The name to return.
             */
            private final String name;

            /**
             * Creates a new field name extractor for a fixed value.
             *
             * @param name The name to return.
             */
            protected ForFixedValue(String name) {
                this.name = name;
            }

            @Override
            public String resolve(MethodDescription methodDescription) {
                return name;
            }
        }
    }

    /**
     * A field accessor that allows to define the access to be a field write of a given argument. 允许将访问定义为给定参数的字段写入的字段访问器
     */
    public interface PropertyConfigurable extends Implementation {

        /**
         * Creates a field accessor for the described field that serves as a setter for the supplied parameter index. The instrumented
         * method must return {@code void} or a chained instrumentation must be supplied. 为所描述的字段创建字段访问器，该字段用作所提供参数索引的设置器。插入指令的方法必须返回{@code void}，或者必须提供链式插入指令
         *
         * @param index The index of the parameter for which to set the field's value. 要为其设置字段值的参数的索引
         * @return An instrumentation that sets the parameter's value to the described field. 将参数值设置为所描述字段的指令插入
         */
        Implementation.Composable setsArgumentAt(int index);
    }

    /**
     * A field accessor that can be configured to use a given assigner and runtime type use configuration.
     */
    public interface AssignerConfigurable extends PropertyConfigurable {

        /**
         * Returns a field accessor that is identical to this field accessor but uses the given assigner
         * and runtime type use configuration.
         *
         * @param assigner The assigner to use.
         * @param typing   Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @return This field accessor with the given assigner and runtime type use configuration.
         */
        PropertyConfigurable withAssigner(Assigner assigner, Assigner.Typing typing);
    }

    /**
     * A field accessor that can be configured to locate a field in a specific manner. 一种字段存取器，可配置为以特定方式定位字段
     */
    public interface OwnerTypeLocatable extends AssignerConfigurable {

        /**
         * Determines that a field should only be considered when it was defined in a given type. 确定只有在给定类型中定义字段时才应考虑该字段
         *
         * @param type The type to be considered.
         * @return This field accessor which will only considered fields that are defined in the given type.
         */
        AssignerConfigurable in(Class<?> type);

        /**
         * Determines that a field should only be considered when it was defined in a given type.
         *
         * @param typeDescription A description of the type to be considered.
         * @return This field accessor which will only considered fields that are defined in the given type.
         */
        AssignerConfigurable in(TypeDescription typeDescription);

        /**
         * Determines that a field should only be considered when it was identified by a field locator that is
         * produced by the given factory.
         *
         * @param fieldLocatorFactory A factory that will produce a field locator that will be used to find locate
         *                            a field to be accessed.
         * @return This field accessor which will only considered fields that are defined in the given type.
         */
        AssignerConfigurable in(FieldLocator.Factory fieldLocatorFactory);
    }

    /**
     * A field accessor for an implicit property where a getter or setter property is inferred from the signature. 隐式属性的字段访问器，其中从签名推断getter或setter属性
     */
    protected static class ForImplicitProperty extends FieldAccessor implements OwnerTypeLocatable {

        /**
         * Creates a field accessor for an implicit property.
         *
         * @param fieldLocation The field's location.
         */
        protected ForImplicitProperty(FieldLocation fieldLocation) {
            this(fieldLocation, Assigner.DEFAULT, Assigner.Typing.STATIC);
        }

        /**
         * Creates a field accessor for an implicit property.
         *
         * @param fieldLocation The field's location.
         * @param assigner      The assigner to use.
         * @param typing        The typing to use.
         */
        private ForImplicitProperty(FieldLocation fieldLocation, Assigner assigner, Assigner.Typing typing) {
            super(fieldLocation, assigner, typing);
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(fieldLocation.prepare(implementationTarget.getInstrumentedType()));
        }

        @Override
        public Composable setsArgumentAt(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("A parameter index cannot be negative: " + index);
            }
            return new ForParameterSetter(fieldLocation, assigner, typing, index);
        }

        @Override
        public PropertyConfigurable withAssigner(Assigner assigner, Assigner.Typing typing) {
            return new ForImplicitProperty(fieldLocation, assigner, typing);
        }

        @Override
        public AssignerConfigurable in(Class<?> type) {
            return in(TypeDescription.ForLoadedType.of(type));
        }

        @Override
        public AssignerConfigurable in(TypeDescription typeDescription) {
            return in(new FieldLocator.ForExactType.Factory(typeDescription));
        }

        @Override
        public AssignerConfigurable in(FieldLocator.Factory fieldLocatorFactory) {
            return new ForImplicitProperty(fieldLocation.with(fieldLocatorFactory), assigner, typing);
        }

        /**
         * An byte code appender for an field accessor implementation. 字段存取器实现的字节码追加器
         */
        @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
        protected class Appender implements ByteCodeAppender {

            /**
             * The field's location.
             */
            private final FieldLocation.Prepared fieldLocation;

            /**
             * Creates a new byte code appender for a field accessor implementation. 为字段访问器实现创建新的字节码追加器
             *
             * @param fieldLocation The field's location.
             */
            protected Appender(FieldLocation.Prepared fieldLocation) {
                this.fieldLocation = fieldLocation;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                if (!instrumentedMethod.isMethod()) {
                    throw new IllegalArgumentException(instrumentedMethod + " does not describe a field getter or setter");
                }
                FieldDescription fieldDescription = fieldLocation.resolve(instrumentedMethod);
                StackManipulation implementation;
                if (!instrumentedMethod.getReturnType().represents(void.class)) {
                    implementation = new StackManipulation.Compound(getter(fieldDescription, instrumentedMethod), MethodReturn.of(instrumentedMethod.getReturnType()));
                } else if (instrumentedMethod.getReturnType().represents(void.class) && instrumentedMethod.getParameters().size() == 1) {
                    implementation = new StackManipulation.Compound(setter(fieldDescription, instrumentedMethod.getParameters().get(0)), MethodReturn.VOID);
                } else {
                    throw new IllegalArgumentException("Method " + implementationContext + " is no bean property");
                }
                return new Size(implementation.apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
            }
        }
    }

    /**
     * A field accessor that sets a parameters value of a given index.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class ForParameterSetter extends FieldAccessor implements Implementation.Composable {

        /**
         * The targeted parameter index.
         */
        private final int index;

        /**
         * The termination handler to apply.
         */
        private final TerminationHandler terminationHandler;

        /**
         * Creates a new field accessor.
         *
         * @param fieldLocation The field's location.
         * @param assigner      The assigner to use.
         * @param typing        Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @param index         The targeted parameter index.
         */
        protected ForParameterSetter(FieldLocation fieldLocation, Assigner assigner, Assigner.Typing typing, int index) {
            this(fieldLocation, assigner, typing, index, TerminationHandler.RETURNING);
        }

        /**
         * Creates a new field accessor.
         *
         * @param fieldLocation      The field's location.
         * @param assigner           The assigner to use.
         * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @param index              The targeted parameter index.
         * @param terminationHandler The termination handler to apply.
         */
        private ForParameterSetter(FieldLocation fieldLocation, Assigner assigner, Assigner.Typing typing, int index, TerminationHandler terminationHandler) {
            super(fieldLocation, assigner, typing);
            this.index = index;
            this.terminationHandler = terminationHandler;
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(fieldLocation.prepare(implementationTarget.getInstrumentedType()));
        }

        @Override
        public Implementation andThen(Implementation implementation) {
            return new Compound(new ForParameterSetter(fieldLocation,
                    assigner,
                    typing,
                    index, TerminationHandler.NON_OPERATIONAL), implementation);
        }

        @Override
        public Composable andThen(Composable implementation) {
            return new Compound.Composable(new ForParameterSetter(fieldLocation,
                    assigner,
                    typing,
                    index, TerminationHandler.NON_OPERATIONAL), implementation);
        }

        /**
         * A termination handler is responsible for handling a field accessor's return. 终止处理程序负责处理字段访问器的返回
         */
        protected enum TerminationHandler {

            /**
             * Returns {@code void} or throws an exception if this is not the return type of the instrumented method. 返回{@code void}或抛出异常（如果这不是插入指令的方法的返回类型）
             */
            RETURNING {
                @Override
                protected StackManipulation resolve(MethodDescription instrumentedMethod) {
                    if (!instrumentedMethod.getReturnType().represents(void.class)) {
                        throw new IllegalStateException("Cannot implement setter with return value for " + instrumentedMethod);
                    }
                    return MethodReturn.VOID;
                }
            },

            /**
             * Does not return from the method at all. 完全不从方法返回
             */
            NON_OPERATIONAL {
                @Override
                protected StackManipulation resolve(MethodDescription instrumentedMethod) {
                    return StackManipulation.Trivial.INSTANCE;
                }
            };

            /**
             * Resolves the return instruction. 解析返回指令
             *
             * @param instrumentedMethod The instrumented method.
             * @return An appropriate stack manipulation. 适当的堆栈操作
             */
            protected abstract StackManipulation resolve(MethodDescription instrumentedMethod);
        }

        /**
         * An appender for a field accessor that sets a parameter of a given index. 字段存取器的追加器，用于设置给定索引的参数
         */
        @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
        protected class Appender implements ByteCodeAppender {

            /**
             * The field's location.
             */
            private final FieldLocation.Prepared fieldLocation;

            /**
             * Creates a new byte code appender for a field accessor implementation.
             *
             * @param fieldLocation The field's location.
             */
            protected Appender(FieldLocation.Prepared fieldLocation) {
                this.fieldLocation = fieldLocation;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                if (instrumentedMethod.getParameters().size() <= index) {
                    throw new IllegalStateException(instrumentedMethod + " does not define a parameter with index " + index);
                } else {
                    return new Size(new StackManipulation.Compound(
                            setter(fieldLocation.resolve(instrumentedMethod), instrumentedMethod.getParameters().get(index)),
                            terminationHandler.resolve(instrumentedMethod)
                    ).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
                }
            }
        }
    }
}
