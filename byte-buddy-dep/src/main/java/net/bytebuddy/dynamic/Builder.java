package net.bytebuddy.dynamic;

import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.asm.FieldVisitorWrapper;
import net.bytebuddy.asm.MethodVisitorWrapper;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.scaffold.FieldRegistry;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMethodMatcher;
import net.bytebuddy.utility.CompoundList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

public interface Builder<T> {

    Builder<T> visit(ClassVisitorWrapper classVisitorWrapper);

    Builder<T> visit(FieldVisitorWrapper fieldVisitorWrapper);

    Builder<T> visit(ElementMatcher<? super FieldDescription> matcher, FieldVisitorWrapper fieldVisitorWrapper);

    Builder<T> visit(MethodVisitorWrapper methodVisitorWrapper);

    Builder<T> visit(ElementMatcher<? super MethodDescription> matcher, MethodVisitorWrapper methodVisitorWrapper);

    Builder<T> name(String name);

    Builder<T> attribute(TypeAttributeAppender typeAttributeAppender);

    Builder<T> modifiers(ModifierContributor.ForType... modifierContributor);

    Builder<T> modifiers(Collection<? extends ModifierContributor.ForType> modifierContributors);

    Builder<T> modifiers(int modifiers);

    Builder<T> annotateType(Annotation... annotation);

    Builder<T> annotateType(List<? extends Annotation> annotations);

    Builder<T> annotateType(AnnotationDescription... annotation);

    Builder<T> annotateType(Collection<? extends AnnotationDescription> annotations);

    MethodDefinition.ImplementationDefinition.Optional<T> implement(Type... type);

    MethodDefinition.ImplementationDefinition.Optional<T> implement(List<? extends Type> types);

    MethodDefinition.ImplementationDefinition.Optional<T> implement(TypeDefinition... type);

    MethodDefinition.ImplementationDefinition.Optional<T> implement(Collection<? extends TypeDefinition> types);

    Builder<T> initializer(ByteCodeAppender byteCodeAppender);

    Builder<T> initializer(LoadedTypeInitializer loadedTypeInitializer);

    Builder<T> typeVariable(String symbol);

    Builder<T> typeVariable(String symbol, Type bound);

    Builder<T> typeVariable(String symbol, TypeDefinition bound);

    FieldDefinition.Optional.Valuable<T> defineField(String name, Type type, ModifierContributor.ForField... modifierContributor);

    FieldDefinition.Optional.Valuable<T> defineField(String name, Type type, Collection<? extends ModifierContributor.ForField> modifierContributors);

    FieldDefinition.Optional.Valuable<T> defineField(String name, Type type, int modifiers);

    FieldDefinition.Optional.Valuable<T> defineField(String name, TypeDefinition type, ModifierContributor.ForField... modifierContributor);

    FieldDefinition.Optional.Valuable<T> defineField(String name, TypeDefinition type, Collection<? extends ModifierContributor.ForField> modifierContributors);

    FieldDefinition.Optional.Valuable<T> defineField(String name, TypeDefinition type, int modifiers);

    FieldDefinition.Optional.Valuable<T> define(Field field);

    FieldDefinition.Valuable<T> field(ElementMatcher<? super FieldDescription> matcher);

    Builder<T> ignore(ElementMatcher<? super MethodDescription> ignored);

    MethodDefinition.ParameterDefinition.Initial<T> defineMethod(String name, Type returnType, ModifierContributor.ForMethod... modifierContributor);

    MethodDefinition.ParameterDefinition.Initial<T> defineMethod(String name, Type returnType, Collection<? extends ModifierContributor.ForMethod> modifierContributor);

    MethodDefinition.ParameterDefinition.Initial<T> defineMethod(String name, Type returnType, int modifiers);

    MethodDefinition.ParameterDefinition.Initial<T> defineMethod(String name, TypeDefinition returnType, ModifierContributor.ForMethod... modifierContributor);

    MethodDefinition.ParameterDefinition.Initial<T> defineMethod(String name, TypeDefinition returnType, Collection<? extends ModifierContributor.ForMethod> modifierContributor);

    MethodDefinition.ParameterDefinition.Initial<T> defineMethod(String name, TypeDefinition returnType, int modifiers);

    MethodDefinition.ParameterDefinition.Initial<T> defineConstructor(ModifierContributor.ForMethod... modifierContributor);

    MethodDefinition.ParameterDefinition.Initial<T> defineConstructor(Collection<? extends ModifierContributor.ForMethod> modifierContributor);

    MethodDefinition.ParameterDefinition.Initial<T> defineConstructor(int modifiers);

    MethodDefinition.ImplementationDefinition<T> define(Method method);

    MethodDefinition.ImplementationDefinition<T> define(Constructor<?> constructor);

    MethodDefinition.ImplementationDefinition<T> define(MethodDescription methodDescription);

    MethodDefinition.ImplementationDefinition<T> method(ElementMatcher<? super MethodDescription> matcher);

    MethodDefinition.ImplementationDefinition<T> constructor(ElementMatcher<? super MethodDescription> matcher);

    MethodDefinition.ImplementationDefinition<T> invokable(ElementMatcher<? super MethodDescription> matcher);

    MethodDefinition.ImplementationDefinition<T> invokable(LatentMethodMatcher matcher);

    DynamicType.Unloaded<T> make();

    interface FieldDefinition<S> {

        FieldDefinition.Optional<S> annotateField(Annotation... annotation);

        FieldDefinition.Optional<S> annotateField(List<? extends Annotation> annotations);

        FieldDefinition.Optional<S> annotateField(AnnotationDescription... annotation);

        FieldDefinition.Optional<S> annotateField(Collection<? extends AnnotationDescription> annotations);

        FieldDefinition.Optional<S> attribute(FieldAttributeAppender.Factory fieldAttributeAppenderFactory);

        FieldDefinition.Optional<S> transform(Transformer<FieldDescription> transformer);

        interface Valuable<U> extends FieldDefinition<U> {

            FieldDefinition.Optional<U> value(boolean value);

            FieldDefinition.Optional<U> value(int value);

            FieldDefinition.Optional<U> value(long value);

            FieldDefinition.Optional<U> value(float value);

            FieldDefinition.Optional<U> value(double value);

            FieldDefinition.Optional<U> value(String value);

            FieldDefinition.Optional<U> withoutValue();
        }

        interface Optional<U> extends FieldDefinition<U>, Builder<U> {

            interface Valuable<V> extends FieldDefinition.Valuable<V>, Optional<V> {

                abstract class AbstractBase<U> extends Optional.AbstractBase<U> implements Optional.Valuable<U> {

                    @Override
                    public FieldDefinition.Optional<U> value(boolean value) {
                        return defaultValue(value ? 1 : 0);
                    }

                    @Override
                    public FieldDefinition.Optional<U> value(int value) {
                        return defaultValue(value);
                    }

                    @Override
                    public FieldDefinition.Optional<U> value(long value) {
                        return defaultValue(value);
                    }

                    @Override
                    public FieldDefinition.Optional<U> value(float value) {
                        return defaultValue(value);
                    }

                    @Override
                    public FieldDefinition.Optional<U> value(double value) {
                        return defaultValue(value);
                    }

                    @Override
                    public FieldDefinition.Optional<U> value(String value) {
                        return defaultValue(value);
                    }

                    protected abstract FieldDefinition.Optional<U> defaultValue(Object defaultValue);

                    protected abstract static class Adapter<V> extends Optional.Valuable.AbstractBase<V> {

                        protected final FieldAttributeAppender.Factory fieldAttributeAppenderFactory;

                        protected final Transformer<FieldDescription> transformer;

                        protected final Object defaultValue;

                        protected Adapter(FieldAttributeAppender.Factory fieldAttributeAppenderFactory,
                                          Transformer<FieldDescription> transformer,
                                          Object defaultValue) {
                            this.fieldAttributeAppenderFactory = fieldAttributeAppenderFactory;
                            this.transformer = transformer;
                            this.defaultValue = defaultValue;
                        }

                        @Override
                        public FieldDefinition.Optional<V> attribute(FieldAttributeAppender.Factory fieldAttributeAppenderFactory) {
                            return materialize(new FieldAttributeAppender.Factory.Compound(this.fieldAttributeAppenderFactory, fieldAttributeAppenderFactory), transformer, defaultValue);
                        }

                        @Override
                        public FieldDefinition.Optional<V> transform(Transformer<FieldDescription> transformer) {
                            return materialize(fieldAttributeAppenderFactory, new Transformer.Compound<FieldDescription>(this.transformer, transformer), defaultValue);
                        }

                        @Override
                        protected FieldDefinition.Optional<V> defaultValue(Object defaultValue) {
                            return materialize(fieldAttributeAppenderFactory, transformer, defaultValue);
                        }

                        @Override
                        public Optional<V> withoutValue() {
                            return materialize(fieldAttributeAppenderFactory, transformer, FieldDescription.NO_DEFAULT_VALUE);
                        }

                        protected abstract FieldDefinition.Optional<V> materialize(FieldAttributeAppender.Factory fieldAttributeAppenderFactory,
                                                                                   Transformer<FieldDescription> transformer,
                                                                                   Object defaultValue);
                    }
                }
            }

            abstract class AbstractBase<U> extends Builder.AbstractBase.Delegator<U> implements FieldDefinition.Optional<U> {

                @Override
                public FieldDefinition.Optional<U> annotateField(Annotation... annotation) {
                    return annotateField(Arrays.asList(annotation));
                }

                @Override
                public FieldDefinition.Optional<U> annotateField(List<? extends Annotation> annotations) {
                    return annotateField(new AnnotationList.ForLoadedAnnotation(annotations));
                }

                @Override
                public FieldDefinition.Optional<U> annotateField(AnnotationDescription... annotation) {
                    return annotateField(Arrays.asList(annotation));
                }
            }
        }
    }

    interface MethodDefinition<S> extends Builder<S> {

        MethodDefinition<S> annotateMethod(Annotation... annotation);

        MethodDefinition<S> annotateMethod(List<? extends Annotation> annotations);

        MethodDefinition<S> annotateMethod(AnnotationDescription... annotation);

        MethodDefinition<S> annotateMethod(Collection<? extends AnnotationDescription> annotations);

        MethodDefinition<S> annotateParameter(int index, Annotation... annotation);

        MethodDefinition<S> annotateParameter(int index, List<? extends Annotation> annotations);

        MethodDefinition<S> annotateParameter(int index, AnnotationDescription... annotation);

        MethodDefinition<S> annotateParameter(int index, Collection<? extends AnnotationDescription> annotations);

        MethodDefinition<S> attribute(MethodAttributeAppender.Factory methodAttributeAppenderFactory);

        MethodDefinition<S> transform(Transformer<MethodDescription> transformer);

        interface ImplementationDefinition<U> {

            MethodDefinition<U> implement(Implementation implementation);

            MethodDefinition<U> withoutCode();

            MethodDefinition<U> defaultValue(Object value);

            MethodDefinition<U> defaultValue(Object value, Class<?> type);

            interface Optional<V> extends ImplementationDefinition<V>, Builder<V> {
                /* union type */
            }

            abstract class AbstractBase<V> implements ImplementationDefinition<V> {

                @Override
                public MethodDefinition<V> defaultValue(Object value, Class<?> type) {
                    return defaultValue(AnnotationDescription.ForLoadedAnnotation.describe(value, new TypeDescription.ForLoadedType(type)));
                }
            }
        }

        interface TypeVariableDefinition<U> extends ImplementationDefinition<U> {

            TypeVariableDefinition<U> typeVariable(String symbol);

            TypeVariableDefinition<U> typeVariable(String symbol, Type... bound);

            TypeVariableDefinition<U> typeVariable(String symbol, List<? extends Type> bounds);

            TypeVariableDefinition<U> typeVariable(String symbol, TypeDefinition... bound);

            TypeVariableDefinition<U> typeVariable(String symbol, Collection<? extends TypeDefinition> bounds);

            abstract class AbstractBase<V> extends ImplementationDefinition.AbstractBase<V> implements TypeVariableDefinition<V> {

                @Override
                public TypeVariableDefinition<V> typeVariable(String symbol) {
                    return typeVariable(symbol, Collections.<TypeDefinition>emptyList());
                }

                @Override
                public TypeVariableDefinition<V> typeVariable(String symbol, Type... bound) {
                    return typeVariable(symbol, Arrays.asList(bound));
                }

                @Override
                public TypeVariableDefinition<V> typeVariable(String symbol, List<? extends Type> bounds) {
                    return typeVariable(symbol, new TypeList.Generic.ForLoadedTypes(bounds));
                }

                @Override
                public TypeVariableDefinition<V> typeVariable(String symbol, TypeDefinition... bound) {
                    return typeVariable(symbol, Arrays.asList(bound));
                }
            }
        }

        interface ExceptionDefinition<U> extends TypeVariableDefinition<U> {

            ExceptionDefinition<U> throwing(Type... type);

            ExceptionDefinition<U> throwing(List<? extends Type> types);

            ExceptionDefinition<U> throwing(TypeDefinition... type);

            ExceptionDefinition<U> throwing(Collection<? extends TypeDefinition> types);

            abstract class AbstractBase<V> extends TypeVariableDefinition.AbstractBase<V> implements ExceptionDefinition<V> {

                @Override
                public ExceptionDefinition<V> throwing(Type... type) {
                    return throwing(Arrays.asList(type));
                }

                @Override
                public ExceptionDefinition<V> throwing(List<? extends Type> types) {
                    return throwing(new TypeList.Generic.ForLoadedTypes(types));
                }

                @Override
                public ExceptionDefinition<V> throwing(TypeDefinition... type) {
                    return throwing(Arrays.asList(type));
                }
            }
        }

        interface ParameterDefinition<U> extends ExceptionDefinition<U> {

            Annotatable<U> withParameter(Type type, String name, ModifierContributor.ForParameter... modifierContributor);

            Annotatable<U> withParameter(Type type, String name, Collection<? extends ModifierContributor.ForParameter> modifierContributors);

            Annotatable<U> withParameter(Type type, String name, int modifiers);

            Annotatable<U> withParameter(TypeDefinition type, String name, ModifierContributor.ForParameter... modifierContributor);

            Annotatable<U> withParameter(TypeDefinition type, String name, Collection<? extends ModifierContributor.ForParameter> modifierContributors);

            Annotatable<U> withParameter(TypeDefinition type, String name, int modifiers);

            interface Annotatable<V> extends ParameterDefinition<V> {

                Annotatable<V> annotateParameter(Annotation... annotation);

                Annotatable<V> annotateParameter(List<? extends Annotation> annotations);

                Annotatable<V> annotateParameter(AnnotationDescription... annotation);

                Annotatable<V> annotateParameter(Collection<? extends AnnotationDescription> annotations);

                abstract class AbstractBase<W> extends ParameterDefinition.AbstractBase<W> implements Annotatable<W> {

                    @Override
                    public Annotatable<W> annotateParameter(Annotation... annotation) {
                        return annotateParameter(Arrays.asList(annotation));
                    }

                    @Override
                    public Annotatable<W> annotateParameter(List<? extends Annotation> annotations) {
                        return annotateParameter(new AnnotationList.ForLoadedAnnotation(annotations));
                    }

                    @Override
                    public Annotatable<W> annotateParameter(AnnotationDescription... annotation) {
                        return annotateParameter(Arrays.asList(annotation));
                    }

                    protected abstract static class Adapter<X> extends Annotatable.AbstractBase<X> {

                        @Override
                        public Annotatable<X> withParameter(TypeDefinition type, String name, int modifiers) {
                            return materialize().withParameter(type, name, modifiers);
                        }

                        @Override
                        public MethodDefinition.ExceptionDefinition<X> throwing(Collection<? extends TypeDefinition> types) {
                            return materialize().throwing(types);
                        }

                        @Override
                        public MethodDefinition.TypeVariableDefinition<X> typeVariable(String symbol, Collection<? extends TypeDefinition> bounds) {
                            return materialize().typeVariable(symbol, bounds);
                        }

                        @Override
                        public MethodDefinition<X> implement(Implementation implementation) {
                            return materialize().implement(implementation);
                        }

                        @Override
                        public MethodDefinition<X> withoutCode() {
                            return materialize().withoutCode();
                        }

                        @Override
                        public MethodDefinition<X> defaultValue(Object value) {
                            return materialize().defaultValue(value);
                        }

                        @Override
                        public MethodDefinition<X> defaultValue(Object value, Class<?> type) {
                            return materialize().defaultValue(value, type);
                        }

                        protected abstract MethodDefinition.ParameterDefinition<X> materialize();
                    }
                }
            }

            interface Simple<V> extends ExceptionDefinition<V> {

                Annotatable<V> withParameter(Type type);

                Annotatable<V> withParameter(TypeDefinition type);

                interface Annotatable<V> extends Simple<V> {

                    Annotatable<V> annotateParameter(Annotation... annotation);

                    Annotatable<V> annotateParameter(List<? extends Annotation> annotations);

                    Annotatable<V> annotateParameter(AnnotationDescription... annotation);

                    Annotatable<V> annotateParameter(Collection<? extends AnnotationDescription> annotations);

                    abstract class AbstractBase<W> extends ParameterDefinition.Simple.AbstractBase<W> implements Annotatable<W> {

                        @Override
                        public Annotatable<W> annotateParameter(Annotation... annotation) {
                            return annotateParameter(Arrays.asList(annotation));
                        }

                        @Override
                        public Annotatable<W> annotateParameter(List<? extends Annotation> annotations) {
                            return annotateParameter(new AnnotationList.ForLoadedAnnotation(annotations));
                        }

                        @Override
                        public Annotatable<W> annotateParameter(AnnotationDescription... annotation) {
                            return annotateParameter(Arrays.asList(annotation));
                        }

                        protected abstract static class Adapter<X> extends Annotatable.AbstractBase<X> {

                            @Override
                            public Annotatable<X> withParameter(TypeDefinition type) {
                                return materialize().withParameter(type);
                            }

                            @Override
                            public MethodDefinition.ExceptionDefinition<X> throwing(Collection<? extends TypeDefinition> types) {
                                return materialize().throwing(types);
                            }

                            @Override
                            public MethodDefinition.TypeVariableDefinition<X> typeVariable(String symbol, Collection<? extends TypeDefinition> bounds) {
                                return materialize().typeVariable(symbol, bounds);
                            }

                            @Override
                            public MethodDefinition<X> implement(Implementation implementation) {
                                return materialize().implement(implementation);
                            }

                            @Override
                            public MethodDefinition<X> withoutCode() {
                                return materialize().withoutCode();
                            }

                            @Override
                            public MethodDefinition<X> defaultValue(Object value) {
                                return materialize().defaultValue(value);
                            }

                            @Override
                            public MethodDefinition<X> defaultValue(Object value, Class<?> type) {
                                return materialize().defaultValue(value, type);
                            }

                            protected abstract MethodDefinition.ParameterDefinition.Simple<X> materialize();
                        }
                    }
                }

                abstract class AbstractBase<W> extends ExceptionDefinition.AbstractBase<W> implements Simple<W> {

                    @Override
                    public Annotatable<W> withParameter(Type type) {
                        return withParameter(TypeDefinition.Sort.describe(type));
                    }
                }
            }

            interface Initial<V> extends ParameterDefinition<V>, Simple<V> {

                ExceptionDefinition<V> withParameters(Type... type);

                ExceptionDefinition<V> withParameters(List<? extends Type> types);

                ExceptionDefinition<V> withParameters(TypeDefinition... type);

                ExceptionDefinition<V> withParameters(Collection<? extends TypeDefinition> types);

                abstract class AbstractBase<W> extends ParameterDefinition.AbstractBase<W> implements Initial<W> {

                    @Override
                    public Simple.Annotatable<W> withParameter(Type type) {
                        return withParameter(TypeDefinition.Sort.describe(type));
                    }

                    @Override
                    public ExceptionDefinition<W> withParameters(Type... type) {
                        return withParameters(Arrays.asList(type));
                    }

                    @Override
                    public ExceptionDefinition<W> withParameters(List<? extends Type> types) {
                        return withParameters(new TypeList.Generic.ForLoadedTypes(types));
                    }

                    @Override
                    public ExceptionDefinition<W> withParameters(TypeDefinition... type) {
                        return withParameters(Arrays.asList(type));
                    }

                    @Override
                    public ExceptionDefinition<W> withParameters(Collection<? extends TypeDefinition> types) {
                        ParameterDefinition.Simple<W> parameterDefinition = this;
                        for (TypeDefinition type : types) {
                            parameterDefinition = parameterDefinition.withParameter(type);
                        }
                        return parameterDefinition;
                    }
                }
            }

            abstract class AbstractBase<V> extends ExceptionDefinition.AbstractBase<V> implements ParameterDefinition<V> {

                @Override
                public Annotatable<V> withParameter(Type type, String name, ModifierContributor.ForParameter... modifierContributor) {
                    return withParameter(type, name, Arrays.asList(modifierContributor));
                }

                @Override
                public Annotatable<V> withParameter(Type type, String name, Collection<? extends ModifierContributor.ForParameter> modifierContributors) {
                    return withParameter(type, name, ModifierContributor.Resolver.of(modifierContributors).resolve());
                }

                @Override
                public Annotatable<V> withParameter(Type type, String name, int modifiers) {
                    return withParameter(TypeDefinition.Sort.describe(type), name, modifiers);
                }

                @Override
                public Annotatable<V> withParameter(TypeDefinition type, String name, ModifierContributor.ForParameter... modifierContributor) {
                    return withParameter(type, name, Arrays.asList(modifierContributor));
                }

                @Override
                public Annotatable<V> withParameter(TypeDefinition type, String name, Collection<? extends ModifierContributor.ForParameter> modifierContributors) {
                    return withParameter(type, name, ModifierContributor.Resolver.of(modifierContributors).resolve());
                }
            }
        }

        abstract class AbstractBase<U> extends Builder.AbstractBase.Delegator<U> implements MethodDefinition<U> {

            @Override
            public MethodDefinition<U> annotateMethod(Annotation... annotation) {
                return annotateMethod(Arrays.asList(annotation));
            }

            @Override
            public MethodDefinition<U> annotateMethod(List<? extends Annotation> annotations) {
                return annotateMethod(new AnnotationList.ForLoadedAnnotation(annotations));
            }

            @Override
            public MethodDefinition<U> annotateMethod(AnnotationDescription... annotation) {
                return annotateMethod(Arrays.asList(annotation));
            }

            @Override
            public MethodDefinition<U> annotateParameter(int index, Annotation... annotation) {
                return annotateParameter(index, Arrays.asList(annotation));
            }

            @Override
            public MethodDefinition<U> annotateParameter(int index, List<? extends Annotation> annotations) {
                return annotateParameter(index, new AnnotationList.ForLoadedAnnotation(annotations));
            }

            @Override
            public MethodDefinition<U> annotateParameter(int index, AnnotationDescription... annotation) {
                return annotateParameter(index, Arrays.asList(annotation));
            }

            protected abstract static class Adapter<V> extends MethodDefinition.AbstractBase<V> {

                protected final MethodRegistry.Handler handler;

                protected final MethodAttributeAppender.Factory methodAttributeAppenderFactory;

                protected final Transformer<MethodDescription> transformer;

                public Adapter(MethodRegistry.Handler handler, MethodAttributeAppender.Factory methodAttributeAppenderFactory, Transformer<MethodDescription> transformer) {
                    this.handler = handler;
                    this.methodAttributeAppenderFactory = methodAttributeAppenderFactory;
                    this.transformer = transformer;
                }

                @Override
                public MethodDefinition<V> attribute(MethodAttributeAppender.Factory methodAttributeAppenderFactory) {
                    return materialize(handler, new MethodAttributeAppender.Factory.Compound(this.methodAttributeAppenderFactory, methodAttributeAppenderFactory), transformer);
                }

                @Override
                public MethodDefinition<V> transform(Transformer<MethodDescription> transformer) {
                    return materialize(handler, methodAttributeAppenderFactory, new Transformer.Compound<MethodDescription>(this.transformer, transformer));
                }

                protected abstract MethodDefinition<V> materialize(MethodRegistry.Handler handler, MethodAttributeAppender.Factory methodAttributeAppenderFactory, Transformer<MethodDescription> transformer);
            }
        }
    }

    abstract class AbstractBase<S> implements Builder<S> {

        @Override
        public Builder<S> visit(ElementMatcher<? super FieldDescription> matcher, FieldVisitorWrapper fieldVisitorWrapper) {
            return visit(new FieldVisitorWrapper.Matching(matcher, fieldVisitorWrapper));
        }

        @Override
        public Builder<S> visit(ElementMatcher<? super MethodDescription> matcher, MethodVisitorWrapper methodVisitorWrapper) {
            return visit(new MethodVisitorWrapper.Matching(matcher, methodVisitorWrapper));
        }

        @Override
        public Builder<S> annotateType(Annotation... annotation) {
            return annotateType(Arrays.asList(annotation));
        }

        @Override
        public Builder<S> annotateType(List<? extends Annotation> annotations) {
            return annotateType(new AnnotationList.ForLoadedAnnotation(annotations));
        }

        @Override
        public Builder<S> annotateType(AnnotationDescription... annotation) {
            return annotateType(Arrays.asList(annotation));
        }

        @Override
        public Builder<S> modifiers(ModifierContributor.ForType... modifierContributor) {
            return modifiers(Arrays.asList(modifierContributor));
        }

        @Override
        public Builder<S> modifiers(Collection<? extends ModifierContributor.ForType> modifierContributors) {
            return modifiers(ModifierContributor.Resolver.of(modifierContributors).resolve());
        }

        @Override
        public MethodDefinition.ImplementationDefinition.Optional<S> implement(Type... type) {
            return implement(Arrays.asList(type));
        }

        @Override
        public MethodDefinition.ImplementationDefinition.Optional<S> implement(List<? extends Type> types) {
            return implement(new TypeList.Generic.ForLoadedTypes(types));
        }

        @Override
        public MethodDefinition.ImplementationDefinition.Optional<S> implement(TypeDefinition... type) {
            return implement(Arrays.asList(type));
        }

        @Override
        public Builder<S> typeVariable(String symbol) {
            return typeVariable(symbol, TypeDescription.Generic.OBJECT);
        }

        @Override
        public Builder<S> typeVariable(String symbol, Type bound) {
            return typeVariable(symbol, TypeDefinition.Sort.describe(bound));
        }

        @Override
        public FieldDefinition.Optional.Valuable<S> defineField(String name, Type type, ModifierContributor.ForField... modifierContributor) {
            return defineField(name, type, Arrays.asList(modifierContributor));
        }

        @Override
        public FieldDefinition.Optional.Valuable<S> defineField(String name, Type type, Collection<? extends ModifierContributor.ForField> modifierContributors) {
            return defineField(name, type, ModifierContributor.Resolver.of(modifierContributors).resolve());
        }

        @Override
        public FieldDefinition.Optional.Valuable<S> defineField(String name, Type type, int modifiers) {
            return defineField(name, TypeDefinition.Sort.describe(type), modifiers);
        }

        @Override
        public FieldDefinition.Optional.Valuable<S> defineField(String name, TypeDefinition type, ModifierContributor.ForField... modifierContributor) {
            return defineField(name, type, Arrays.asList(modifierContributor));
        }

        @Override
        public FieldDefinition.Optional.Valuable<S> defineField(String name, TypeDefinition type, Collection<? extends ModifierContributor.ForField> modifierContributors) {
            return defineField(name, type, ModifierContributor.Resolver.of(modifierContributors).resolve());
        }

        @Override
        public FieldDefinition.Optional.Valuable<S> define(Field field) {
            return defineField(field.getName(), field.getGenericType(), field.getModifiers());
        }

        @Override
        public MethodDefinition.ParameterDefinition.Initial<S> defineMethod(String name, Type returnType, ModifierContributor.ForMethod... modifierContributor) {
            return defineMethod(name, returnType, Arrays.asList(modifierContributor));
        }

        @Override
        public MethodDefinition.ParameterDefinition.Initial<S> defineMethod(String name, Type returnType, Collection<? extends ModifierContributor.ForMethod> modifierContributor) {
            return defineMethod(name, returnType, ModifierContributor.Resolver.of(modifierContributor).resolve());
        }

        @Override
        public MethodDefinition.ParameterDefinition.Initial<S> defineMethod(String name, Type returnType, int modifiers) {
            return defineMethod(name, TypeDefinition.Sort.describe(returnType), modifiers);
        }

        @Override
        public MethodDefinition.ParameterDefinition.Initial<S> defineMethod(String name, TypeDefinition returnType, ModifierContributor.ForMethod... modifierContributor) {
            return defineMethod(name, returnType, Arrays.asList(modifierContributor));
        }

        @Override
        public MethodDefinition.ParameterDefinition.Initial<S> defineMethod(String name, TypeDefinition returnType, Collection<? extends ModifierContributor.ForMethod> modifierContributor) {
            return defineMethod(name, returnType, ModifierContributor.Resolver.of(modifierContributor).resolve());
        }

        @Override
        public MethodDefinition.ParameterDefinition.Initial<S> defineConstructor(ModifierContributor.ForMethod... modifierContributor) {
            return defineConstructor(Arrays.asList(modifierContributor));
        }

        @Override
        public MethodDefinition.ParameterDefinition.Initial<S> defineConstructor(Collection<? extends ModifierContributor.ForMethod> modifierContributor) {
            return defineConstructor(ModifierContributor.Resolver.of(modifierContributor).resolve());
        }

        @Override
        public MethodDefinition.ImplementationDefinition<S> define(Method method) {
            return define(new MethodDescription.ForLoadedMethod(method));
        }

        @Override
        public MethodDefinition.ImplementationDefinition<S> define(Constructor<?> constructor) {
            return define(new MethodDescription.ForLoadedConstructor(constructor));
        }

        @Override
        public MethodDefinition.ImplementationDefinition<S> define(MethodDescription methodDescription) {
            MethodDefinition.ParameterDefinition.Initial<S> initialParameterDefinition = methodDescription.isConstructor()
                    ? defineConstructor(methodDescription.getModifiers())
                    : defineMethod(methodDescription.getInternalName(), methodDescription.getReturnType(), methodDescription.getModifiers());
            ParameterList<?> parameterList = methodDescription.getParameters();
            MethodDefinition.ExceptionDefinition<S> exceptionDefinition;
            if (parameterList.hasExplicitMetaData()) {
                MethodDefinition.ParameterDefinition<S> parameterDefinition = initialParameterDefinition;
                for (ParameterDescription parameter : parameterList) {
                    parameterDefinition = parameterDefinition.withParameter(parameter.getType(), parameter.getName(), parameter.getModifiers());
                }
                exceptionDefinition = parameterDefinition;
            } else {
                exceptionDefinition = initialParameterDefinition.withParameters(parameterList.asTypeList());
            }
            MethodDefinition.TypeVariableDefinition<S> typeVariableDefinition = exceptionDefinition.throwing(methodDescription.getExceptionTypes());
            for (TypeDescription.Generic typeVariable : methodDescription.getTypeVariables()) {
                typeVariableDefinition = typeVariableDefinition.typeVariable(typeVariable.getSymbol(), typeVariable.getUpperBounds());
            }
            return typeVariableDefinition;
        }

        @Override
        public MethodDefinition.ImplementationDefinition<S> method(ElementMatcher<? super MethodDescription> matcher) {
            return invokable(isMethod().and(matcher));
        }

        @Override
        public MethodDefinition.ImplementationDefinition<S> constructor(ElementMatcher<? super MethodDescription> matcher) {
            return invokable(isConstructor().and(matcher));
        }

        @Override
        public MethodDefinition.ImplementationDefinition<S> invokable(ElementMatcher<? super MethodDescription> matcher) {
            return invokable(new LatentMethodMatcher.Resolved(matcher));
        }

        public abstract static class Delegator<U> extends AbstractBase<U> {

            protected abstract Builder<U> materialize();

            @Override
            public Builder<U> visit(ClassVisitorWrapper classVisitorWrapper) {
                return materialize().visit(classVisitorWrapper);
            }

            @Override
            public Builder<U> visit(FieldVisitorWrapper fieldVisitorWrapper) {
                return materialize().visit(fieldVisitorWrapper);
            }

            @Override
            public Builder<U> visit(MethodVisitorWrapper methodVisitorWrapper) {
                return materialize().visit(methodVisitorWrapper);
            }

            @Override
            public Builder<U> initializer(LoadedTypeInitializer loadedTypeInitializer) {
                return materialize().initializer(loadedTypeInitializer);
            }

            @Override
            public Builder<U> annotateType(Collection<? extends AnnotationDescription> annotations) {
                return materialize().annotateType(annotations);
            }

            @Override
            public Builder<U> attribute(TypeAttributeAppender typeAttributeAppender) {
                return materialize().attribute(typeAttributeAppender);
            }

            @Override
            public Builder<U> modifiers(int modifiers) {
                return materialize().modifiers(modifiers);
            }

            @Override
            public Builder<U> name(String name) {
                return materialize().name(name);
            }

            @Override
            public MethodDefinition.ImplementationDefinition.Optional<U> implement(Collection<? extends TypeDefinition> types) {
                return materialize().implement(types);
            }

            @Override
            public Builder<U> initializer(ByteCodeAppender byteCodeAppender) {
                return materialize().initializer(byteCodeAppender);
            }

            @Override
            public Builder<U> ignore(ElementMatcher<? super MethodDescription> ignored) {
                return materialize().ignore(ignored);
            }

            @Override
            public Builder<U> typeVariable(String symbol, TypeDefinition bound) {
                return materialize().typeVariable(symbol, bound);
            }

            @Override
            public FieldDefinition.Optional.Valuable<U> defineField(String name, TypeDefinition type, int modifiers) {
                return materialize().defineField(name, type, modifiers);
            }

            @Override
            public FieldDefinition.Valuable<U> field(ElementMatcher<? super FieldDescription> matcher) {
                return materialize().field(matcher);
            }

            @Override
            public MethodDefinition.ParameterDefinition.Initial<U> defineMethod(String name, TypeDefinition returnType, int modifiers) {
                return materialize().defineMethod(name, returnType, modifiers);
            }

            @Override
            public MethodDefinition.ParameterDefinition.Initial<U> defineConstructor(int modifiers) {
                return materialize().defineConstructor(modifiers);
            }

            @Override
            public MethodDefinition.ImplementationDefinition<U> invokable(LatentMethodMatcher matcher) {
                return materialize().invokable(matcher);
            }

            @Override
            public DynamicType.Unloaded<U> make() {
                return materialize().make();
            }
        }

        public abstract static class Adapter<U> extends AbstractBase<U> {

            protected final InstrumentedType.WithFlexibleName instrumentedType;

            protected final FieldRegistry fieldRegistry;

            protected final MethodRegistry methodRegistry;

            protected final ElementMatcher<? super MethodDescription> ignored;

            protected final TypeAttributeAppender typeAttributeAppender;

            protected final ClassVisitorWrapper classVisitorWrapper;

            protected final FieldVisitorWrapper fieldVisitorWrapper;

            protected final MethodVisitorWrapper methodVisitorWrapper;

            protected Adapter(InstrumentedType.WithFlexibleName instrumentedType,
                              FieldRegistry fieldRegistry,
                              MethodRegistry methodRegistry,
                              ElementMatcher<? super MethodDescription> ignored,
                              TypeAttributeAppender typeAttributeAppender,
                              ClassVisitorWrapper classVisitorWrapper,
                              FieldVisitorWrapper fieldVisitorWrapper,
                              MethodVisitorWrapper methodVisitorWrapper) {
                this.instrumentedType = instrumentedType;
                this.fieldRegistry = fieldRegistry;
                this.methodRegistry = methodRegistry;
                this.ignored = ignored;
                this.typeAttributeAppender = typeAttributeAppender;
                this.classVisitorWrapper = classVisitorWrapper;
                this.fieldVisitorWrapper = fieldVisitorWrapper;
                this.methodVisitorWrapper = methodVisitorWrapper;
            }

            @Override
            public FieldDefinition.Optional.Valuable<U> defineField(String name, TypeDefinition type, int modifiers) {
                return new FieldDefinitionAdapter(new FieldDescription.Token(name, modifiers, type.asGenericType()));
            }

            @Override
            public FieldDefinition.Valuable<U> field(ElementMatcher<? super FieldDescription> matcher) {
                return new FieldMatchAdapter(matcher);
            }

            @Override
            public MethodDefinition.ParameterDefinition.Initial<U> defineMethod(String name, TypeDefinition returnType, int modifiers) {
                return new MethodDefinitionAdapter(new MethodDescription.Token(name, modifiers, returnType.asGenericType()));
            }

            @Override
            public MethodDefinition.ParameterDefinition.Initial<U> defineConstructor(int modifiers) {
                return new MethodDefinitionAdapter(new MethodDescription.Token(modifiers));
            }

            @Override
            public MethodDefinition.ImplementationDefinition<U> invokable(LatentMethodMatcher matcher) {
                return new MethodMatchAdapter(matcher);
            }

            @Override
            public MethodDefinition.ImplementationDefinition.Optional<U> implement(Collection<? extends TypeDefinition> types) {
                return new OptionalMethodMatchAdapter(new TypeList.Generic.Explicit(new ArrayList<TypeDefinition>(types)));
            }

            @Override
            public Builder<U> ignore(ElementMatcher<? super MethodDescription> ignored) {
                return materialize(instrumentedType,
                        fieldRegistry,
                        methodRegistry,
                        new ElementMatcher.Junction.Disjunction<MethodDescription>(this.ignored, ignored),
                        typeAttributeAppender,
                        classVisitorWrapper,
                        fieldVisitorWrapper,
                        methodVisitorWrapper);
            }

            @Override
            public Builder<U> initializer(ByteCodeAppender byteCodeAppender) {
                return materialize(instrumentedType.withInitializer(byteCodeAppender),
                        fieldRegistry,
                        methodRegistry,
                        ignored,
                        typeAttributeAppender,
                        classVisitorWrapper,
                        fieldVisitorWrapper,
                        methodVisitorWrapper);
            }

            @Override
            public Builder<U> initializer(LoadedTypeInitializer loadedTypeInitializer) {
                return materialize(instrumentedType.withInitializer(loadedTypeInitializer),
                        fieldRegistry,
                        methodRegistry,
                        ignored,
                        typeAttributeAppender,
                        classVisitorWrapper,
                        fieldVisitorWrapper,
                        methodVisitorWrapper);
            }

            @Override
            public Builder<U> name(String name) {
                return materialize(instrumentedType.withName(name),
                        fieldRegistry,
                        methodRegistry,
                        ignored,
                        typeAttributeAppender,
                        classVisitorWrapper,
                        fieldVisitorWrapper,
                        methodVisitorWrapper);
            }

            @Override
            public Builder<U> modifiers(int modifiers) {
                return materialize(instrumentedType.withModifiers(modifiers),
                        fieldRegistry,
                        methodRegistry,
                        ignored,
                        typeAttributeAppender,
                        classVisitorWrapper,
                        fieldVisitorWrapper,
                        methodVisitorWrapper);
            }

            @Override
            public Builder<U> typeVariable(String symbol, TypeDefinition bound) {
                return materialize(instrumentedType.withTypeVariable(symbol, bound.asGenericType()),
                        fieldRegistry,
                        methodRegistry,
                        ignored,
                        typeAttributeAppender,
                        classVisitorWrapper,
                        fieldVisitorWrapper,
                        methodVisitorWrapper);
            }

            @Override
            public Builder<U> attribute(TypeAttributeAppender typeAttributeAppender) {
                return materialize(instrumentedType,
                        fieldRegistry,
                        methodRegistry,
                        ignored,
                        new TypeAttributeAppender.Compound(this.typeAttributeAppender, typeAttributeAppender),
                        classVisitorWrapper,
                        fieldVisitorWrapper,
                        methodVisitorWrapper);
            }

            @Override
            public Builder<U> annotateType(Collection<? extends AnnotationDescription> annotations) {
                return materialize(instrumentedType.withAnnotations(new ArrayList<AnnotationDescription>(annotations)),
                        fieldRegistry,
                        methodRegistry,
                        ignored,
                        typeAttributeAppender,
                        classVisitorWrapper,
                        fieldVisitorWrapper,
                        methodVisitorWrapper);
            }

            @Override
            public Builder<U> visit(ClassVisitorWrapper classVisitorWrapper) {
                return materialize(instrumentedType,
                        fieldRegistry,
                        methodRegistry,
                        ignored,
                        typeAttributeAppender,
                        new ClassVisitorWrapper.Compound(this.classVisitorWrapper, classVisitorWrapper),
                        fieldVisitorWrapper,
                        methodVisitorWrapper);
            }

            @Override
            public Builder<U> visit(FieldVisitorWrapper fieldVisitorWrapper) {
                return materialize(instrumentedType,
                        fieldRegistry,
                        methodRegistry,
                        ignored,
                        typeAttributeAppender,
                        classVisitorWrapper,
                        fieldVisitorWrapper, // TODO: Compound
                        methodVisitorWrapper);
            }

            @Override
            public Builder<U> visit(MethodVisitorWrapper methodVisitorWrapper) {
                return materialize(instrumentedType,
                        fieldRegistry,
                        methodRegistry,
                        ignored,
                        typeAttributeAppender,
                        classVisitorWrapper,
                        fieldVisitorWrapper,
                        methodVisitorWrapper); // TODO: Compound
            }

            protected abstract Builder<U> materialize(InstrumentedType instrumentedType,
                                                      FieldRegistry fieldRegistry,
                                                      MethodRegistry methodRegistry,
                                                      ElementMatcher<? super MethodDescription> ignored,
                                                      TypeAttributeAppender typeAttributeAppender,
                                                      ClassVisitorWrapper classVisitorWrapper,
                                                      FieldVisitorWrapper fieldVisitorWrapper,
                                                      MethodVisitorWrapper methodVisitorWrapper);

            protected class FieldDefinitionAdapter extends FieldDefinition.Optional.Valuable.AbstractBase.Adapter<U> {

                private final FieldDescription.Token token;

                protected FieldDefinitionAdapter(FieldDescription.Token token) {
                    this(FieldAttributeAppender.NoOp.INSTANCE, Transformer.NoOp.<FieldDescription>make(), FieldDescription.NO_DEFAULT_VALUE, token); // TODO: Field appender
                }

                protected FieldDefinitionAdapter(FieldAttributeAppender.Factory fieldAttributeAppenderFactory,
                                                 Transformer<FieldDescription> transformer,
                                                 Object defaultValue,
                                                 FieldDescription.Token token) {
                    super(fieldAttributeAppenderFactory, transformer, defaultValue);
                    this.token = token;
                }

                @Override
                public Optional<U> annotateField(Collection<? extends AnnotationDescription> annotations) {
                    return new FieldDefinitionAdapter(fieldAttributeAppenderFactory, transformer, defaultValue, new FieldDescription.Token(token.getName(),
                            token.getModifiers(),
                            token.getType(),
                            CompoundList.of(token.getAnnotations(), new ArrayList<AnnotationDescription>(annotations))));
                }

                @Override
                protected Builder<U> materialize() {
                    return Builder.AbstractBase.Adapter.this.materialize(instrumentedType.withField(token),
                            fieldRegistry.include(token, fieldAttributeAppenderFactory, defaultValue),
                            methodRegistry,
                            ignored,
                            typeAttributeAppender,
                            classVisitorWrapper,
                            fieldVisitorWrapper,
                            methodVisitorWrapper);
                }

                @Override
                protected Optional<U> materialize(FieldAttributeAppender.Factory fieldAttributeAppenderFactory,
                                                  Transformer<FieldDescription> transformer,
                                                  Object defaultValue) {
                    return new FieldDefinitionAdapter(fieldAttributeAppenderFactory, transformer, defaultValue, token);
                }
            }

            protected class FieldMatchAdapter extends FieldDefinition.Optional.Valuable.AbstractBase.Adapter<U> {

                private final ElementMatcher<? super FieldDescription> matcher;

                protected FieldMatchAdapter(ElementMatcher<? super FieldDescription> matcher) {
                    this(FieldAttributeAppender.NoOp.INSTANCE, Transformer.NoOp.<FieldDescription>make(), FieldDescription.NO_DEFAULT_VALUE, matcher);
                }

                protected FieldMatchAdapter(FieldAttributeAppender.Factory fieldAttributeAppenderFactory,
                                            Transformer<FieldDescription> transformer,
                                            Object defaultValue,
                                            ElementMatcher<? super FieldDescription> matcher) {
                    super(fieldAttributeAppenderFactory, transformer, defaultValue);
                    this.matcher = matcher;
                }

                @Override
                public Optional<U> annotateField(Collection<? extends AnnotationDescription> annotations) {
                    return attribute(new FieldAttributeAppender.ForAnnotation(null, null)); // TODO: Appender default?
                }

                @Override
                protected Builder<U> materialize() {
                    return Builder.AbstractBase.Adapter.this.materialize(instrumentedType,
                            fieldRegistry.include(null, fieldAttributeAppenderFactory, defaultValue), //TODO: modernize field registry
                            methodRegistry,
                            ignored,
                            typeAttributeAppender,
                            classVisitorWrapper,
                            fieldVisitorWrapper,
                            methodVisitorWrapper);
                }

                @Override
                protected Optional<U> materialize(FieldAttributeAppender.Factory fieldAttributeAppenderFactory,
                                                  Transformer<FieldDescription> transformer,
                                                  Object defaultValue) {
                    return new FieldMatchAdapter(fieldAttributeAppenderFactory, transformer, defaultValue, matcher);
                }
            }

            protected class MethodDefinitionAdapter extends MethodDefinition.ParameterDefinition.Initial.AbstractBase<U> {

                private final MethodDescription.Token token;

                protected MethodDefinitionAdapter(MethodDescription.Token token) {
                    this.token = token;
                }

                @Override
                public MethodDefinition.ParameterDefinition.Annotatable<U> withParameter(TypeDefinition type, String name, int modifiers) {
                    return new ParameterAnnotationAdapter(new ParameterDescription.Token(type.asGenericType(), name, modifiers));
                }

                @Override
                public Simple.Annotatable<U> withParameter(TypeDefinition type) {
                    return new SimpleParameterAnnotationAdapter(new ParameterDescription.Token(type.asGenericType()));
                }

                @Override
                public MethodDefinition.ExceptionDefinition<U> throwing(Collection<? extends TypeDefinition> types) {
                    return new MethodDefinitionAdapter(new MethodDescription.Token(token.getName(),
                            token.getModifiers(),
                            token.getTypeVariables(),
                            token.getReturnType(),
                            token.getParameterTokens(),
                            CompoundList.of(token.getExceptionTypes(), new TypeList.Generic.Explicit(new ArrayList<TypeDefinition>(types))),
                            token.getAnnotations(),
                            token.getDefaultValue()));
                }

                @Override
                public MethodDefinition.TypeVariableDefinition<U> typeVariable(String symbol, Collection<? extends TypeDefinition> bounds) {
                    Map<String, TypeList.Generic> typeVariables = new LinkedHashMap<String, TypeList.Generic>(token.getTypeVariables());
                    typeVariables.put(symbol, new TypeList.Generic.Explicit(new ArrayList<TypeDefinition>(bounds)));
                    return new MethodDefinitionAdapter(new MethodDescription.Token(token.getName(),
                            token.getModifiers(),
                            typeVariables,
                            token.getReturnType(),
                            token.getParameterTokens(),
                            token.getExceptionTypes(),
                            token.getAnnotations(),
                            token.getDefaultValue()));
                }

                @Override
                public MethodDefinition<U> implement(Implementation implementation) {
                    return materialize(new MethodRegistry.Handler.ForImplementation(implementation));
                }

                @Override
                public MethodDefinition<U> withoutCode() {
                    return materialize(MethodRegistry.Handler.ForAbstractMethod.INSTANCE);
                }

                @Override
                public MethodDefinition<U> defaultValue(Object value) {
                    return new MethodDefinitionAdapter(new MethodDescription.Token(token.getName(),
                            token.getModifiers(),
                            token.getTypeVariables(),
                            token.getReturnType(),
                            token.getParameterTokens(),
                            token.getExceptionTypes(),
                            token.getAnnotations(),
                            value)).materialize(MethodRegistry.Handler.ForAnnotationValue.of(value));
                }

                protected MethodDefinition<U> materialize(MethodRegistry.Handler handler) {
                    return new AnnotationAdapter(handler);
                }

                protected class ParameterAnnotationAdapter extends MethodDefinition.ParameterDefinition.Annotatable.AbstractBase.Adapter<U> {

                    private final ParameterDescription.Token token;

                    protected ParameterAnnotationAdapter(ParameterDescription.Token token) {
                        this.token = token;
                    }

                    @Override
                    public Annotatable<U> annotateParameter(Collection<? extends AnnotationDescription> annotations) {
                        return new ParameterAnnotationAdapter(new ParameterDescription.Token(token.getType(),
                                CompoundList.of(token.getAnnotations(), new ArrayList<AnnotationDescription>(annotations)),
                                token.getName(),
                                token.getModifiers()));
                    }

                    @Override
                    protected MethodDefinition.ParameterDefinition<U> materialize() {
                        return new MethodDefinitionAdapter(new MethodDescription.Token(MethodDefinitionAdapter.this.token.getName(),
                                MethodDefinitionAdapter.this.token.getModifiers(),
                                MethodDefinitionAdapter.this.token.getTypeVariables(),
                                MethodDefinitionAdapter.this.token.getReturnType(),
                                CompoundList.of(MethodDefinitionAdapter.this.token.getParameterTokens(), token),
                                MethodDefinitionAdapter.this.token.getExceptionTypes(),
                                MethodDefinitionAdapter.this.token.getAnnotations(),
                                MethodDefinitionAdapter.this.token.getDefaultValue()));
                    }
                }

                protected class SimpleParameterAnnotationAdapter extends MethodDefinition.ParameterDefinition.Simple.Annotatable.AbstractBase.Adapter<U> {

                    private final ParameterDescription.Token token;

                    protected SimpleParameterAnnotationAdapter(ParameterDescription.Token token) {
                        this.token = token;
                    }

                    @Override
                    public Annotatable<U> annotateParameter(Collection<? extends AnnotationDescription> annotations) {
                        return new SimpleParameterAnnotationAdapter(new ParameterDescription.Token(token.getType(),
                                CompoundList.of(token.getAnnotations(), new ArrayList<AnnotationDescription>(annotations)),
                                token.getName(),
                                token.getModifiers()));
                    }

                    @Override
                    protected MethodDefinition.ParameterDefinition.Simple<U> materialize() {
                        return new MethodDefinitionAdapter(new MethodDescription.Token(MethodDefinitionAdapter.this.token.getName(),
                                MethodDefinitionAdapter.this.token.getModifiers(),
                                MethodDefinitionAdapter.this.token.getTypeVariables(),
                                MethodDefinitionAdapter.this.token.getReturnType(),
                                CompoundList.of(MethodDefinitionAdapter.this.token.getParameterTokens(), token),
                                MethodDefinitionAdapter.this.token.getExceptionTypes(),
                                MethodDefinitionAdapter.this.token.getAnnotations(),
                                MethodDefinitionAdapter.this.token.getDefaultValue()));
                    }
                }

                protected class AnnotationAdapter extends MethodDefinition.AbstractBase.Adapter<U> {

                    protected AnnotationAdapter(MethodRegistry.Handler handler) {
                        this(handler,
                                MethodAttributeAppender.NoOp.INSTANCE, // TODO
                                Transformer.NoOp.<MethodDescription>make());
                    }

                    protected AnnotationAdapter(MethodRegistry.Handler handler, MethodAttributeAppender.Factory methodAttributeAppenderFactory, Transformer<MethodDescription> transformer) {
                        super(handler, methodAttributeAppenderFactory, transformer);
                    }

                    @Override
                    public MethodDefinition<U> annotateMethod(Collection<? extends AnnotationDescription> annotations) {
                        return new MethodDefinitionAdapter(new MethodDescription.Token(token.getName(),
                                token.getModifiers(),
                                token.getTypeVariables(),
                                token.getReturnType(),
                                token.getParameterTokens(),
                                token.getExceptionTypes(),
                                token.getAnnotations(),
                                token.getDefaultValue())).new AnnotationAdapter(handler, methodAttributeAppenderFactory, transformer);
                    }

                    @Override
                    public MethodDefinition<U> annotateParameter(int index, Collection<? extends AnnotationDescription> annotations) {
                        List<ParameterDescription.Token> parameterTokens = new ArrayList<ParameterDescription.Token>(token.getParameterTokens());
                        parameterTokens.add(index, new ParameterDescription.Token(token.getParameterTokens().get(index).getType(),
                                CompoundList.of(token.getParameterTokens().get(index).getAnnotations(), new ArrayList<AnnotationDescription>(annotations)),
                                token.getParameterTokens().get(index).getName(),
                                token.getParameterTokens().get(index).getModifiers()));
                        return new MethodDefinitionAdapter(new MethodDescription.Token(token.getName(),
                                token.getModifiers(),
                                token.getTypeVariables(),
                                token.getReturnType(),
                                parameterTokens,
                                token.getExceptionTypes(),
                                token.getAnnotations(),
                                token.getDefaultValue())).new AnnotationAdapter(handler, methodAttributeAppenderFactory, transformer);
                    }

                    @Override
                    protected MethodDefinition<U> materialize(MethodRegistry.Handler handler, MethodAttributeAppender.Factory methodAttributeAppenderFactory, Transformer<MethodDescription> transformer) {
                        return new AnnotationAdapter(handler, methodAttributeAppenderFactory, transformer);
                    }

                    @Override
                    protected Builder<U> materialize() {
                        return Builder.AbstractBase.Adapter.this.materialize(instrumentedType.withMethod(token),
                                fieldRegistry,
                                methodRegistry.append(new LatentMethodMatcher.ForToken(token), handler, methodAttributeAppenderFactory, null), // TODO
                                ignored,
                                typeAttributeAppender,
                                classVisitorWrapper,
                                fieldVisitorWrapper,
                                methodVisitorWrapper);
                    }
                }
            }

            protected class MethodMatchAdapter extends MethodDefinition.ImplementationDefinition.AbstractBase<U> {

                private final LatentMethodMatcher matcher;

                protected MethodMatchAdapter(LatentMethodMatcher matcher) {
                    this.matcher = matcher;
                }

                @Override
                public MethodDefinition<U> implement(Implementation implementation) {
                    return materialize(new MethodRegistry.Handler.ForImplementation(implementation));
                }

                @Override
                public MethodDefinition<U> withoutCode() {
                    return materialize(MethodRegistry.Handler.ForAbstractMethod.INSTANCE);
                }

                @Override
                public MethodDefinition<U> defaultValue(Object value) {
                    return materialize(MethodRegistry.Handler.ForAnnotationValue.of(value));
                }

                protected MethodDefinition<U> materialize(MethodRegistry.Handler handler) {
                    return new AnnotationAdapter(handler);
                }

                protected class AnnotationAdapter extends MethodDefinition.AbstractBase.Adapter<U> {

                    protected AnnotationAdapter(MethodRegistry.Handler handler) {
                        this(handler, MethodAttributeAppender.NoOp.INSTANCE, Transformer.NoOp.<MethodDescription>make());
                    }

                    protected AnnotationAdapter(MethodRegistry.Handler handler, MethodAttributeAppender.Factory methodAttributeAppenderFactory, Transformer<MethodDescription> transformer) {
                        super(handler, methodAttributeAppenderFactory, transformer);
                    }

                    @Override
                    public MethodDefinition<U> annotateMethod(Collection<? extends AnnotationDescription> annotations) {
                        return new AnnotationAdapter(handler,
                                new MethodAttributeAppender.Factory.Compound(methodAttributeAppenderFactory, new MethodAttributeAppender.ForAnnotation(null)),  // TODO
                                transformer);
                    }

                    @Override
                    public MethodDefinition<U> annotateParameter(int index, Collection<? extends AnnotationDescription> annotations) {
                        return new AnnotationAdapter(handler,
                                new MethodAttributeAppender.Factory.Compound(methodAttributeAppenderFactory, new MethodAttributeAppender.ForAnnotation(index, null)),  // TODO
                                transformer);
                    }

                    @Override
                    protected MethodDefinition<U> materialize(MethodRegistry.Handler handler, MethodAttributeAppender.Factory methodAttributeAppenderFactory, Transformer<MethodDescription> transformer) {
                        return new AnnotationAdapter(handler, methodAttributeAppenderFactory, transformer);
                    }

                    @Override
                    protected Builder<U> materialize() {
                        return Builder.AbstractBase.Adapter.this.materialize(instrumentedType,
                                fieldRegistry,
                                methodRegistry.append(matcher, handler, methodAttributeAppenderFactory, null), // TODO
                                ignored,
                                typeAttributeAppender,
                                classVisitorWrapper,
                                fieldVisitorWrapper,
                                methodVisitorWrapper);
                    }
                }
            }

            protected class OptionalMethodMatchAdapter extends Builder.AbstractBase.Delegator<U> implements MethodDefinition.ImplementationDefinition.Optional<U> {

                private final List<TypeDescription.Generic> interfaces;

                protected OptionalMethodMatchAdapter(List<TypeDescription.Generic> interfaces) {
                    this.interfaces = interfaces;
                }

                @Override
                protected Builder<U> materialize() {
                    return Adapter.this.materialize(instrumentedType.withInterfaces(interfaces),
                            fieldRegistry,
                            methodRegistry,
                            ignored,
                            typeAttributeAppender,
                            classVisitorWrapper,
                            fieldVisitorWrapper,
                            methodVisitorWrapper);
                }

                // TODO: Inherited interface methods are not matched properly!

                @Override
                public MethodDefinition<U> implement(Implementation implementation) {
                    return materialize().invokable(isDeclaredByGeneric(anyOf(interfaces))).implement(implementation);
                }

                @Override
                public MethodDefinition<U> withoutCode() {
                    return materialize().invokable(isDeclaredByGeneric(anyOf(interfaces))).withoutCode();
                }

                @Override
                public MethodDefinition<U> defaultValue(Object value) {
                    return materialize().invokable(isDeclaredByGeneric(anyOf(interfaces))).defaultValue(value);
                }

                @Override
                public MethodDefinition<U> defaultValue(Object value, Class<?> type) {
                    return materialize().invokable(isDeclaredByGeneric(anyOf(interfaces))).defaultValue(value, type);
                }
            }
        }
    }
}
