package net.bytebuddy.dynamic;

import net.bytebuddy.ClassFileVersion;
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
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.CompoundList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;

public interface Builder<T> {

    Builder<T> classFileVersion(ClassFileVersion classFileVersion);

    Builder<T> visit(ClassVisitorWrapper classVisitorWrapper);

    Builder<T> visit(FieldVisitorWrapper fieldVisitorWrapper);

    Builder<T> visit(ElementMatcher<? extends FieldDescription> matcher, FieldVisitorWrapper fieldVisitorWrapper);

    Builder<T> visit(MethodVisitorWrapper methodVisitorWrapper);

    Builder<T> visit(ElementMatcher<? extends MethodDescription> matcher, MethodVisitorWrapper methodVisitorWrapper);

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

    Builder<T> typeVariable(String symbol);

    Builder<T> typeVariable(String symbol, Type bound);

    Builder<T> typeVariable(String symbol, TypeDefinition bound);

    FieldDefinition.Valuable<T> defineField(String name, Type type, ModifierContributor.ForField... modifierContributor);

    FieldDefinition.Valuable<T> defineField(String name, Type type, Collection<? extends ModifierContributor.ForField> modifierContributors);

    FieldDefinition.Valuable<T> defineField(String name, Type type, int modifiers);

    FieldDefinition.Valuable<T> defineField(String name, TypeDefinition type, ModifierContributor.ForField... modifierContributor);

    FieldDefinition.Valuable<T> defineField(String name, TypeDefinition type, Collection<? extends ModifierContributor.ForField> modifierContributors);

    FieldDefinition.Valuable<T> defineField(String name, TypeDefinition type, int modifiers);

    FieldDefinition.Valuable<T> define(Field field);

    FieldDefinition.Valuable<T> field(ElementMatcher<? super FieldDescription> matcher);

    Builder<T> ignore(ElementMatcher<? super MethodDescription> matcher);

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

    DynamicType.Unloaded<T> make();

    interface FieldDefinition<S> extends Builder<S> {

        FieldDefinition<S> annotateField(Annotation... annotation);

        FieldDefinition<S> annotateField(List<? extends Annotation> annotations);

        FieldDefinition<S> annotateField(AnnotationDescription... annotation);

        FieldDefinition<S> annotateField(Collection<? extends AnnotationDescription> annotations);

        FieldDefinition<S> attribute(FieldAttributeAppender.Factory fieldAttributeAppenderFactory);

        FieldDefinition<S> transform(Transformer<FieldDescription> transformer);

        interface Valuable<U> extends FieldDefinition<U> {

            FieldDefinition<U> value(boolean value);

            FieldDefinition<U> value(int value);

            FieldDefinition<U> value(long value);

            FieldDefinition<U> value(float value);

            FieldDefinition<U> value(double value);

            FieldDefinition<U> value(String value);

            abstract class AbstractBase<U> extends FieldDefinition.AbstractBase<U> implements Valuable<U> {

                @Override
                public FieldDefinition<U> value(boolean value) {
                    return value(value, boolean.class);
                }

                @Override
                public FieldDefinition<U> value(int value) {
                    return value(value, int.class);
                }

                @Override
                public FieldDefinition<U> value(long value) {
                    return value(value, long.class);
                }

                @Override
                public FieldDefinition<U> value(float value) {
                    return value(value, float.class);
                }

                @Override
                public FieldDefinition<U> value(double value) {
                    return value(value, double.class);
                }

                @Override
                public FieldDefinition<U> value(String value) {
                    return value(value, String.class);
                }

                protected abstract FieldDefinition<U> value(Object value, Class<?> type);
            }
        }

        abstract class AbstractBase<U> extends Builder.AbstractBase.Delegator<U> implements FieldDefinition<U> {

            @Override
            public FieldDefinition<U> annotateField(Annotation... annotation) {
                return annotateField(Arrays.asList(annotation));
            }

            @Override
            public FieldDefinition<U> annotateField(List<? extends Annotation> annotations) {
                return annotateField(new AnnotationList.ForLoadedAnnotation(annotations));
            }

            @Override
            public FieldDefinition<U> annotateField(AnnotationDescription... annotation) {
                return annotateField(Arrays.asList(annotation));
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

        interface TypeVariableDefinition<U> extends ImplementationDefinition<U> {

            TypeVariableDefinition<U> typeVariable(String symbol);

            TypeVariableDefinition<U> typeVariable(String symbol, Type... bound);

            TypeVariableDefinition<U> typeVariable(String symbol, List<? extends Type> bounds);

            TypeVariableDefinition<U> typeVariable(String symbol, TypeDefinition... bound);

            TypeVariableDefinition<U> typeVariable(String symbol, Collection<? extends TypeDefinition> bounds);

            abstract class AbstractBase<V> implements TypeVariableDefinition<V> {

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

        interface ImplementationDefinition<U> {

            MethodDefinition<U> implement(Implementation implementation);

            MethodDefinition<U> withoutCode();

            MethodDefinition<U> defaultValue(Object value);

            MethodDefinition<U> defaultValue(Object value, Class<?> type);

            interface Optional<U> extends ImplementationDefinition<U>, Builder<U> {
                /* union type */
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
        }
    }

    abstract class AbstractBase<S> implements Builder<S> {

        @Override
        public Builder<S> visit(ElementMatcher<? extends FieldDescription> matcher, FieldVisitorWrapper fieldVisitorWrapper) {
            return null; // TODO
        }

        @Override
        public Builder<S> visit(ElementMatcher<? extends MethodDescription> matcher, MethodVisitorWrapper methodVisitorWrapper) {
            return null; // TODO
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
        public FieldDefinition.Valuable<S> defineField(String name, Type type, ModifierContributor.ForField... modifierContributor) {
            return defineField(name, type, Arrays.asList(modifierContributor));
        }

        @Override
        public FieldDefinition.Valuable<S> defineField(String name, Type type, Collection<? extends ModifierContributor.ForField> modifierContributors) {
            return defineField(name, type, ModifierContributor.Resolver.of(modifierContributors).resolve());
        }

        @Override
        public FieldDefinition.Valuable<S> defineField(String name, Type type, int modifiers) {
            return defineField(name, TypeDefinition.Sort.describe(type), modifiers);
        }

        @Override
        public FieldDefinition.Valuable<S> defineField(String name, TypeDefinition type, ModifierContributor.ForField... modifierContributor) {
            return defineField(name, type, Arrays.asList(modifierContributor));
        }

        @Override
        public FieldDefinition.Valuable<S> defineField(String name, TypeDefinition type, Collection<? extends ModifierContributor.ForField> modifierContributors) {
            return defineField(name, type, ModifierContributor.Resolver.of(modifierContributors).resolve());
        }

        @Override
        public FieldDefinition.Valuable<S> define(Field field) {
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
            public Builder<U> classFileVersion(ClassFileVersion classFileVersion) {
                return materialize().classFileVersion(classFileVersion);
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
            public Builder<U> ignore(ElementMatcher<? super MethodDescription> matcher) {
                return materialize().ignore(matcher);
            }

            @Override
            public Builder<U> typeVariable(String symbol, TypeDefinition bound) {
                return materialize().typeVariable(symbol, bound);
            }

            @Override
            public FieldDefinition.Valuable<U> defineField(String name, TypeDefinition type, int modifiers) {
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
            public MethodDefinition.ImplementationDefinition<U> invokable(ElementMatcher<? super MethodDescription> matcher) {
                return materialize().invokable(matcher);
            }

            @Override
            public DynamicType.Unloaded<U> make() {
                return materialize().make();
            }
        }

        public abstract static class Adapter<U> extends AbstractBase<U> {

            protected final int modifiers;

            protected final Map<String, TypeList.Generic> typeVariables;

            protected final FieldRegistry fieldRegistry;

            protected final MethodRegistry methodRegistry;

            protected final TypeAttributeAppender typeAttributeAppender;

            protected final List<AnnotationDescription> annotationDescriptions;

            private final ClassVisitorWrapper classVisitorWrapper;

            private final FieldVisitorWrapper fieldVisitorWrapper;

            private final MethodVisitorWrapper methodVisitorWrapper;

            @Override
            public FieldDefinition.Valuable<U> defineField(String name, TypeDefinition type, int modifiers) {
                return new FieldDefinitionAdapter<U>();
            }

            @Override
            public FieldDefinition.Valuable<U> field(ElementMatcher<? super FieldDescription> matcher) {
                return null;
            }

            @Override
            public MethodDefinition.ParameterDefinition.Initial<U> defineMethod(String name, TypeDefinition returnType, int modifiers) {
                return null;
            }

            @Override
            public MethodDefinition.ParameterDefinition.Initial<U> defineConstructor(int modifiers) {
                return null;
            }

            @Override
            public MethodDefinition.ImplementationDefinition<U> invokable(ElementMatcher<? super MethodDescription> matcher) {
                return null;
            }

            @Override
            public Builder<U> typeVariable(String symbol, TypeDefinition bound) {
                return null; // TODO
            }

            @Override
            public Builder<U> modifiers(int modifiers) {
                return null; // TODO
            }

            @Override
            public Builder<U> attribute(TypeAttributeAppender typeAttributeAppender) {
                return null; // TODO
            }

            @Override
            public Builder<U> annotateType(Collection<? extends AnnotationDescription> annotations) {
                return null; // TODO
            }

            @Override
            public Builder<U> visit(MethodVisitorWrapper methodVisitorWrapper) {
                return null; // TODO
            }

            @Override
            public Builder<U> visit(FieldVisitorWrapper fieldVisitorWrapper) {
                return null; // TODO
            }

            @Override
            public Builder<U> visit(ClassVisitorWrapper classVisitorWrapper) {
                return null; // TODO
            }

            protected static abstract class FieldDefinitionAdapter<V> extends FieldDefinition.Valuable.AbstractBase<V> {

                protected final List<AnnotationDescription> annotations;

                protected final FieldAttributeAppender.Factory fieldAttributeAppenderFactory;

                protected final Transformer<FieldDescription> transformer;

                protected final Object value;

                protected FieldDefinitionAdapter(List<AnnotationDescription> annotations,
                                                 FieldAttributeAppender.Factory fieldAttributeAppenderFactory,
                                                 Transformer<FieldDescription> transformer,
                                                 Object value) {
                    this.annotations = annotations;
                    this.fieldAttributeAppenderFactory = fieldAttributeAppenderFactory;
                    this.transformer = transformer;
                    this.value = value;
                }

                @Override
                public FieldDefinition<V> annotateField(Collection<? extends AnnotationDescription> annotations) {
                    return materialize(CompoundList.of(this.annotations, new ArrayList<AnnotationDescription>(annotations)),
                            fieldAttributeAppenderFactory,
                            transformer,
                            value);
                }

                @Override
                public FieldDefinition<V> attribute(FieldAttributeAppender.Factory fieldAttributeAppenderFactory) {
                    return materialize(annotations,
                            new FieldAttributeAppender.Factory.Compound(this.fieldAttributeAppenderFactory, fieldAttributeAppenderFactory),
                            transformer,
                            value);
                }

                @Override
                public FieldDefinition<V> transform(Transformer<FieldDescription> transformer) {
                    return materialize(annotations,
                            fieldAttributeAppenderFactory,
                            new Transformer.Compound<FieldDescription>(this.transformer, transformer),
                            value);
                }

                @Override
                protected FieldDefinition<V> value(Object value, Class<?> type) {
                    return materialize(annotations,
                            fieldAttributeAppenderFactory,
                            transformer,
                            value);
                }

                @Override
                protected Builder<V> materialize() {
                    return materialize(annotations, fieldAttributeAppenderFactory, transformer, value);
                }

                protected abstract FieldDefinition<V> materialize(List<AnnotationDescription> annotations,
                                                                  FieldAttributeAppender.Factory fieldAttributeAppenderFactory,
                                                                  Transformer<FieldDescription> transformer,
                                                                  Object value);
            }
        }
    }
}
