package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.MultipleParentClassLoader;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.ExceptionMethod;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.TypeCreation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.OpenedClassReader;
import org.objectweb.asm.*;

import java.lang.reflect.Method;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;

public abstract class ClassVisitorFactory<T> {

    // TODO: attribute-bypass

    private static final String DELEGATE = "delegate";

    private static final String LABELS = "labels";

    private static final String WRAP = "wrap";

    public static <S> ClassVisitorFactory<S> of(Class<S> classVisitor) {
        return of(classVisitor, new ByteBuddy().with(TypeValidation.DISABLED));
    }

    public static <S> ClassVisitorFactory<S> of(Class<S> classVisitor, ByteBuddy byteBuddy) {
        try {
            String prefix = classVisitor.getPackage().getName();
            Map<Class<?>, Class<?>> utilities = new HashMap<Class<?>, Class<?>>();
            for (Class<?> type : Arrays.asList(
                    Attribute.class,
                    Label.class,
                    Type.class,
                    TypePath.class,
                    Handle.class,
                    ConstantDynamic.class
            )) {
                utilities.put(type, Class.forName(prefix + "." + type.getSimpleName()));
            }
            Map<Class<?>, Class<?>> equivalents = new HashMap<Class<?>, Class<?>>();
            Map<Class<?>, DynamicType.Builder<?>> builders = new HashMap<Class<?>, DynamicType.Builder<?>>();
            for (Class<?> type : Arrays.asList(
                    ClassVisitor.class,
                    AnnotationVisitor.class,
                    FieldVisitor.class,
                    MethodVisitor.class,
                    RecordComponentVisitor.class,
                    ModuleVisitor.class
            )) {
                Class<?> equivalent = Class.forName(prefix + "." + type.getSimpleName());
                DynamicType.Builder<?> wrapper, unwrapper;
                if (type == MethodVisitor.class) {
                    wrapper = toMethodVisitorBuilder(byteBuddy, type, equivalent,
                            Label.class, utilities.get(Label.class),
                            Type.class, utilities.get(Type.class),
                            Handle.class, utilities.get(Handle.class),
                            TypePath.class, utilities.get(TypePath.class),
                            ConstantDynamic.class, utilities.get(ConstantDynamic.class));
                    unwrapper = toMethodVisitorBuilder(byteBuddy, equivalent, type,
                            utilities.get(Label.class), Label.class,
                            utilities.get(Type.class), Type.class,
                            utilities.get(Handle.class), Handle.class,
                            utilities.get(TypePath.class), TypePath.class,
                            utilities.get(ConstantDynamic.class), ConstantDynamic.class);
                } else {
                    wrapper = toBuilder(byteBuddy, type, equivalent, new Implementation.Simple(MethodReturn.VOID));
                    unwrapper = toBuilder(byteBuddy, equivalent, type, new Implementation.Simple(MethodReturn.VOID));
                }
                equivalents.put(type, equivalent);
                builders.put(type, wrapper);
                builders.put(equivalent, unwrapper);
            }
            List<DynamicType> dynamicTypes = new ArrayList<DynamicType>();
            Map<Class<?>, TypeDescription> generated = new HashMap<Class<?>, TypeDescription>();
            for (Map.Entry<Class<?>, Class<?>> entry : equivalents.entrySet()) {
                DynamicType.Builder<?> wrapper = builders.get(entry.getKey()), unwrapper = builders.get(entry.getValue());
                for (Method method : entry.getKey().getMethods()) {
                    if (method.getDeclaringClass() == Object.class) {
                        continue;
                    }
                    Class<?>[] parameter = method.getParameterTypes(), match = new Class<?>[parameter.length];
                    List<MethodCall.ArgumentLoader.Factory> left = new ArrayList<MethodCall.ArgumentLoader.Factory>(parameter.length);
                    List<MethodCall.ArgumentLoader.Factory> right = new ArrayList<MethodCall.ArgumentLoader.Factory>(match.length);
                    for (int index = 0; index < parameter.length; index++) {
                        if (entry.getKey() == MethodVisitor.class && parameter[index] == Label.class) { // TODO: add wrappers
                            match[index] = utilities.get(Label.class);
                            left.add(toConvertedParameter(builders.get(entry.getKey()).toTypeDescription(), match[index], LabelTranslator.NAME, index));
                            right.add(toConvertedParameter(builders.get(entry.getValue()).toTypeDescription(), parameter[index], LabelTranslator.NAME, index));
                        } else if (entry.getKey() == MethodVisitor.class && parameter[index] == Label[].class) {
                            match[index] = Class.forName("[L" + utilities.get(Label.class).getName() + ";", false, classVisitor.getClassLoader());
                            left.add(toConvertedParameter(builders.get(entry.getKey()).toTypeDescription(), match[index], LabelArrayTranslator.NAME, index));
                            right.add(toConvertedParameter(builders.get(entry.getValue()).toTypeDescription(), parameter[index], LabelArrayTranslator.NAME, index));
                        } else if (entry.getKey() == MethodVisitor.class && parameter[index] == TypePath.class) {
                            match[index] = utilities.get(TypePath.class);
                            left.add(toConvertedParameter(builders.get(entry.getKey()).toTypeDescription(), match[index], TypePathTranslator.NAME, index));
                            right.add(toConvertedParameter(builders.get(entry.getValue()).toTypeDescription(), parameter[index], TypePathTranslator.NAME, index));
                        } else if (entry.getKey() == MethodVisitor.class && parameter[index] == Handle.class) {
                            match[index] = utilities.get(Handle.class);
                            left.add(toConvertedParameter(builders.get(entry.getKey()).toTypeDescription(), match[index], HandleTranslator.NAME, index));
                            right.add(toConvertedParameter(builders.get(entry.getValue()).toTypeDescription(), parameter[index], HandleTranslator.NAME, index));
                        } else if (entry.getKey() == MethodVisitor.class && parameter[index] == Object.class) {
                            match[index] = Object.class;
                            left.add(toConvertedParameter(builders.get(entry.getKey()).toTypeDescription(), match[index], ConstantTranslator.NAME, index));
                            right.add(toConvertedParameter(builders.get(entry.getValue()).toTypeDescription(), parameter[index], ConstantTranslator.NAME, index));
                        } else if (entry.getKey() == MethodVisitor.class && parameter[index] == Object[].class) {
                            match[index] = Object[].class;
                            left.add(toConvertedParameter(builders.get(entry.getKey()).toTypeDescription(), match[index], ConstantArrayTranslator.NAME, index));
                            right.add(toConvertedParameter(builders.get(entry.getValue()).toTypeDescription(), parameter[index], ConstantArrayTranslator.NAME, index));
                        } else {
                            match[index] = parameter[index];
                            left.add(new MethodCall.ArgumentLoader.ForMethodParameter.Factory(index));
                            right.add(new MethodCall.ArgumentLoader.ForMethodParameter.Factory(index));
                        }
                    }
                    Method target = entry.getValue().getMethod(method.getName(), match);
                    if (method.getName().equals("visitAttribute")) {
                        wrapper = wrapper.method(is(method)).intercept(ExceptionMethod.throwing(UnsupportedOperationException.class));
                        unwrapper = unwrapper.method(is(target)).intercept(ExceptionMethod.throwing(UnsupportedOperationException.class));
                    } else {
                        MethodCall wrapping = MethodCall.invoke(target).onField(DELEGATE).with(left);
                        MethodCall unwrapping = MethodCall.invoke(method).onField(DELEGATE).with(right);
                        Class<?> returned = equivalents.get(method.getReturnType());
                        if (returned != null) {
                            wrapping = MethodCall.construct(builders.get(returned)
                                    .toTypeDescription()
                                    .getDeclaredMethods()
                                    .filter(ElementMatchers.<MethodDescription.InDefinedShape>isConstructor())
                                    .getOnly()).withMethodCall(wrapping);
                            unwrapping = MethodCall.construct(builders.get(equivalents.get(returned))
                                    .toTypeDescription()
                                    .getDeclaredMethods()
                                    .filter(ElementMatchers.<MethodDescription.InDefinedShape>isConstructor())
                                    .getOnly()).withMethodCall(unwrapping);
                        }
                        wrapper = wrapper.method(is(method)).intercept(wrapping);
                        unwrapper = unwrapper.method(is(target)).intercept(unwrapping);
                    }
                }
                DynamicType left = wrapper.make(), right = unwrapper.make();
                generated.put(entry.getKey(), left.getTypeDescription());
                generated.put(entry.getValue(), right.getTypeDescription());
                dynamicTypes.add(left);
                dynamicTypes.add(right);
            }
            ClassLoader classLoader = new MultipleParentClassLoader.Builder(false)
                    .append(ClassVisitor.class, classVisitor)
                    .build();
            @SuppressWarnings("unchecked")
            ClassVisitorFactory<S> factory = byteBuddy.subclass(ClassVisitorFactory.class)
                    .method(named("wrap")).intercept(MethodCall.construct(generated.get(classVisitor)
                            .getDeclaredMethods()
                            .filter(ElementMatchers.<MethodDescription.InDefinedShape>isConstructor())
                            .getOnly()).withArgument(0))
                    .method(named("unwrap")).intercept(MethodCall.construct(generated.get(ClassVisitor.class)
                            .getDeclaredMethods()
                            .filter(ElementMatchers.<MethodDescription.InDefinedShape>isConstructor())
                            .getOnly()).withArgument(0).withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                    .make()
                    .include(dynamicTypes)
                    .load(classLoader)
                    .getLoaded()
                    .newInstance();
            if (classLoader instanceof MultipleParentClassLoader
                    && classLoader != ClassVisitor.class.getClassLoader()
                    && classLoader != classVisitor.getClassLoader()
                    && !((MultipleParentClassLoader) classLoader).seal()) {
                throw new IllegalStateException("Failed to seal multiple parent class loader: " + classLoader);
            }
            return factory;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to generate factory for " + classVisitor.getName(), exception);
        }
    }

    private static DynamicType.Builder<?> toBuilder(ByteBuddy byteBuddy, Class<?> source, Class<?> target, Implementation appendix) throws NoSuchMethodException {
        return byteBuddy.subclass(source, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .defineField(DELEGATE, target, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineConstructor(Visibility.PUBLIC)
                .withParameters(target)
                .intercept(MethodCall.invoke(source.getDeclaredConstructor(int.class))
                        .with(OpenedClassReader.ASM_API)
                        .andThen(FieldAccessor.ofField(DELEGATE).setsArgumentAt(0))
                        .andThen(appendix))
                .defineMethod(WRAP, source, Visibility.PUBLIC, Ownership.STATIC)
                .withParameters(target)
                .intercept(new Implementation.Simple(new NullCheckedConstruction(target)));
    }

    private static DynamicType.Builder<?> toMethodVisitorBuilder(ByteBuddy byteBuddy,
                                                                 Class<?> source, Class<?> target,
                                                                 Class<?> sourceLabel, Class<?> targetLabel,
                                                                 Class<?> sourceType, Class<?> targetType,
                                                                 Class<?> sourceHandle, Class<?> targetHandle,
                                                                 Class<?> sourceTypePath, Class<?> targetTypePath,
                                                                 Class<?> sourceConstantDynamic, Class<?> targetConstantDynamic) throws Exception {
        return toBuilder(byteBuddy, source, target, FieldAccessor.ofField(LABELS).setsValue(new StackManipulation.Compound(TypeCreation.of(TypeDescription.ForLoadedType.of(HashMap.class)),
                Duplication.SINGLE,
                MethodInvocation.invoke(TypeDescription.ForLoadedType.of(HashMap.class)
                        .getDeclaredMethods()
                        .filter(ElementMatchers.<MethodDescription.InDefinedShape>isConstructor().and(ElementMatchers.<MethodDescription.InDefinedShape>takesArguments(0)))
                        .getOnly())), Map.class))
                .defineField(LABELS, Map.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineMethod(LabelTranslator.NAME, targetLabel, Visibility.PRIVATE, Ownership.STATIC)
                .withParameters(sourceLabel)
                .intercept(new Implementation.Simple(new LabelTranslator(targetLabel)))
                .defineMethod(LabelArrayTranslator.NAME, Class.forName("[L" + targetLabel.getName() + ";", false, targetLabel.getClassLoader()), Visibility.PRIVATE, Ownership.STATIC)
                .withParameters(Class.forName("[L" + sourceLabel.getName() + ";", false, targetLabel.getClassLoader()))
                .intercept(new Implementation.Simple(new LabelArrayTranslator(sourceLabel, targetLabel)))
                .defineMethod(HandleTranslator.NAME, targetHandle, Visibility.PRIVATE, Ownership.STATIC)
                .withParameters(sourceHandle)
                .intercept(new Implementation.Simple(new HandleTranslator(sourceHandle, targetHandle)))
                .defineMethod(ConstantDynamicTranslator.NAME, targetConstantDynamic, Visibility.PRIVATE, Ownership.STATIC)
                .withParameters(sourceConstantDynamic)
                .intercept(new Implementation.Simple(new ConstantDynamicTranslator(sourceConstantDynamic, targetConstantDynamic, sourceHandle, targetHandle)))
                .defineMethod(ConstantTranslator.NAME, Object.class, Visibility.PRIVATE, Ownership.STATIC)
                .withParameters(Object.class)
                .intercept(new Implementation.Simple(new ConstantTranslator(sourceHandle, targetHandle, sourceType, targetType, sourceConstantDynamic, targetConstantDynamic)))
                .defineMethod(ConstantArrayTranslator.NAME, Object[].class, Visibility.PRIVATE, Ownership.STATIC)
                .withParameters(Object[].class)
                .intercept(new Implementation.Simple(new ConstantArrayTranslator()))
                .defineMethod(FrameTranslator.NAME, Object[].class, Visibility.PRIVATE, Ownership.STATIC)
                .withParameters(Object[].class)
                .intercept(new Implementation.Simple(new FrameTranslator(sourceLabel, targetLabel)))
                .defineMethod(TypePathTranslator.NAME, TypePath.class, Visibility.PRIVATE, Ownership.STATIC)
                .withParameters(TypePath.class)
                .intercept(new Implementation.Simple(new TypePathTranslator(sourceTypePath, targetTypePath)));
    }

    private static MethodCall.ArgumentLoader.Factory toConvertedParameter(TypeDescription source, Class<?> target, String method, int index) {
        return new MethodCall.ArgumentLoader.ForStackManipulation(new StackManipulation.Compound(
                MethodVariableAccess.REFERENCE.loadFrom(index),
                MethodInvocation.invoke(source.getDeclaredMethods().filter(named(method)).getOnly())), target);
    }

    public abstract T wrap(ClassVisitor classVisitor);

    public abstract ClassVisitor unwrap(T classVisitor);

    protected static class NullCheckedConstruction implements ByteCodeAppender {

        private final Class<?> target;

        protected NullCheckedConstruction(Class<?> target) {
            this.target = target;
        }

        public Size apply(MethodVisitor methodVisitor,
                          Implementation.Context implementationContext,
                          MethodDescription instrumentedMethod) {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            Label label = new Label();
            methodVisitor.visitJumpInsn(Opcodes.IFNULL, label);
            methodVisitor.visitTypeInsn(Opcodes.NEW, implementationContext.getInstrumentedType().getInternalName());
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                    implementationContext.getInstrumentedType().getInternalName(),
                    MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(target)),
                    false);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            methodVisitor.visitLabel(label);
            if (implementationContext.getFrameGeneration().isActive()) { // TODO
                methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(3, 1);
        }
    }

    protected static class LabelTranslator implements ByteCodeAppender {

        protected static final String NAME = "label";

        private final Class<?> target;

        protected LabelTranslator(Class<?> target) {
            this.target = target;
        }

        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
            Label end = new Label();
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD,
                    implementationContext.getInstrumentedType().getInternalName(),
                    LABELS,
                    Type.getDescriptor(Map.class));
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class),
                    "get",
                    Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class)),
                    true);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(target));
            methodVisitor.visitVarInsn(Opcodes.ASTORE, 2);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitJumpInsn(Opcodes.IFNONNULL, end);
            methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(target));
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                    Type.getInternalName(target),
                    MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                    "()V",
                    false);
            methodVisitor.visitVarInsn(Opcodes.ASTORE, 2);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD,
                    implementationContext.getInstrumentedType().getInternalName(),
                    LABELS,
                    Type.getDescriptor(Map.class));
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                    Type.getInternalName(Map.class),
                    "put",
                    Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class), Type.getType(Object.class)),
                    true);
            methodVisitor.visitInsn(Opcodes.POP);
            methodVisitor.visitLabel(end);
            if (implementationContext.getFrameGeneration().isActive()) { // TODO
                methodVisitor.visitFrame(Opcodes.F_APPEND, 1, new Object[]{Type.getInternalName(target)}, 0, null);
            }
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(3, 3);
        }
    }

    protected static class LabelArrayTranslator implements ByteCodeAppender {

        protected static final String NAME = "labels";

        private final Class<?> source, target;

        protected LabelArrayTranslator(Class<?> source, Class<?> target) {
            this.source = source;
            this.target = target;
        }

        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
            Label loop = new Label(), end = new Label();
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitInsn(Opcodes.ARRAYLENGTH);
            methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(target));
            methodVisitor.visitVarInsn(Opcodes.ASTORE, 2);
            methodVisitor.visitInsn(Opcodes.ICONST_0);
            methodVisitor.visitVarInsn(Opcodes.ISTORE, 3);
            methodVisitor.visitLabel(loop);
            if (implementationContext.getFrameGeneration().isActive()) { // TODO
                methodVisitor.visitFrame(Opcodes.F_APPEND, 2, new Object[]{"[L" + Type.getInternalName(target) + ";", Opcodes.INTEGER}, 0, null);
            }
            methodVisitor.visitVarInsn(Opcodes.ILOAD, 3);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitInsn(Opcodes.ARRAYLENGTH);
            methodVisitor.visitJumpInsn(Opcodes.IF_ICMPGE, end);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitVarInsn(Opcodes.ILOAD, 3);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitVarInsn(Opcodes.ILOAD, 3);
            methodVisitor.visitInsn(Opcodes.AALOAD);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                    implementationContext.getInstrumentedType().getInternalName(),
                    LabelTranslator.NAME,
                    Type.getMethodDescriptor(Type.getType(target), Type.getType(source)),
                    false);
            methodVisitor.visitInsn(Opcodes.AASTORE);
            methodVisitor.visitIincInsn(3, 1);
            methodVisitor.visitJumpInsn(Opcodes.GOTO, loop);
            methodVisitor.visitLabel(end);

            if (implementationContext.getFrameGeneration().isActive()) { // TODO
                methodVisitor.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
            }
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(5, 4);
        }
    }

    protected static class HandleTranslator implements ByteCodeAppender {

        protected static final String NAME = "handle";

        private final Class<?> source, target;

        protected HandleTranslator(Class<?> source, Class<?> target) {
            this.source = source;
            this.target = target;
        }

        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
            methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(target));
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    Type.getInternalName(source),
                    "getTag",
                    Type.getMethodDescriptor(Type.INT_TYPE),
                    false);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    Type.getInternalName(source),
                    "getOwner",
                    Type.getMethodDescriptor(Type.getType(String.class)),
                    false);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    Type.getInternalName(source),
                    "getName",
                    Type.getMethodDescriptor(Type.getType(String.class)),
                    false);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(source),
                    "getDesc",
                    Type.getMethodDescriptor(Type.getType(String.class)),
                    false);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    Type.getInternalName(source),
                    "isInterface",
                    Type.getMethodDescriptor(Type.BOOLEAN_TYPE),
                    false);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                    Type.getInternalName(target),
                    MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.getType(String.class), Type.getType(String.class), Type.getType(String.class), Type.BOOLEAN_TYPE),
                    false);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(7, 1);
        }
    }

    protected static class ConstantDynamicTranslator implements ByteCodeAppender {

        protected static final String NAME = "constantDyanmic";

        private final Class<?> source, target, sourceHandle, targetHandle;

        protected ConstantDynamicTranslator(Class<?> source, Class<?> target, Class<?> sourceHandle, Class<?> targetHandle) {
            this.source = source;
            this.target = target;
            this.sourceHandle = sourceHandle;
            this.targetHandle = targetHandle;
        }

        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
            Label loop = new Label(), end = new Label();
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    Type.getInternalName(source),
                    "getBootstrapMethodArgumentCount",
                    Type.getMethodDescriptor(Type.INT_TYPE),
                    false);
            methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(Object.class));
            methodVisitor.visitVarInsn(Opcodes.ASTORE, 1);
            methodVisitor.visitInsn(Opcodes.ICONST_0);
            methodVisitor.visitVarInsn(Opcodes.ISTORE, 2);
            methodVisitor.visitLabel(loop);
            methodVisitor.visitFrame(Opcodes.F_APPEND,2, new Object[] {Type.getInternalName(Object[].class), Opcodes.INTEGER}, 0, null);
            methodVisitor.visitVarInsn(Opcodes.ILOAD, 2);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitInsn(Opcodes.ARRAYLENGTH);
            methodVisitor.visitJumpInsn(Opcodes.IF_ICMPGE, end);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitVarInsn(Opcodes.ILOAD, 2);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitVarInsn(Opcodes.ILOAD, 2);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    Type.getInternalName(source),
                    "getBootstrapMethodArgument",
                    Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE),
                    false);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                    implementationContext.getInstrumentedType().getInternalName(),
                    "ldc",
                    Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class)),
                    false);
            methodVisitor.visitInsn(Opcodes.AASTORE);
            methodVisitor.visitIincInsn(2, 1);
            methodVisitor.visitJumpInsn(Opcodes.GOTO, loop);
            methodVisitor.visitLabel(end);
            methodVisitor.visitFrame(Opcodes.F_CHOP,1, null, 0, null);
            methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(target));
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    Type.getInternalName(source),
                    "getName",
                    Type.getMethodDescriptor(Type.getType(String.class)),
                    false);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    Type.getInternalName(source),
                    "getDescriptor",
                    Type.getMethodDescriptor(Type.getType(String.class)),
                    false);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    Type.getInternalName(source),
                    "getBootstrapMethod",
                    Type.getMethodDescriptor(Type.getType(sourceHandle)),
                    false);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                    implementationContext.getInstrumentedType().getInternalName(),
                    HandleTranslator.NAME,
                    "(" + Type.getDescriptor(sourceHandle) + ")" + Type.getDescriptor(targetHandle),
                    false);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                    Type.getInternalName(target),
                    MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.getType(String.class), Type.getType(sourceHandle), Type.getType(Object[].class)),
                    false);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            methodVisitor.visitMaxs(6, 3);
            return new Size(6, 3);
        }
    }

    protected static class ConstantTranslator implements ByteCodeAppender {

        protected static final String NAME = "constant";

        private final Class<?> sourceHandle, targetHandle,
                sourceType, targetType,
                sourceConstantDynamic, targetConstantDynamic;

        protected ConstantTranslator(Class<?> sourceHandle,
                                     Class<?> targetHandle,
                                     Class<?> sourceType,
                                     Class<?> targetType,
                                     Class<?> sourceConstantDynamic,
                                     Class<?> targetConstantDynamic) {
            this.sourceHandle = sourceHandle;
            this.targetHandle = targetHandle;
            this.sourceType = sourceType;
            this.targetType = targetType;
            this.sourceConstantDynamic = sourceConstantDynamic;
            this.targetConstantDynamic = targetConstantDynamic;
        }

        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
            Label noHandle = new Label(), noType = new Label(), noConstantDynamic = new Label();
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(sourceHandle));
            methodVisitor.visitJumpInsn(Opcodes.IFEQ, noHandle);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(sourceHandle));
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                    implementationContext.getInstrumentedType().getInternalName(),
                    HandleTranslator.NAME,
                    Type.getMethodDescriptor(Type.getType(targetHandle), Type.getType(sourceHandle)),
                    false);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            methodVisitor.visitLabel(noHandle);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(sourceType));
            methodVisitor.visitJumpInsn(Opcodes.IFEQ, noType);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(sourceType));
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    Type.getInternalName(sourceType),
                    "getDescriptor",
                    Type.getMethodDescriptor(Type.getType(String.class)),
                    false);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                    Type.getInternalName(targetType),
                    "getType",
                    Type.getMethodDescriptor(Type.getType(targetType), Type.getType(String.class)),
                    false);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            methodVisitor.visitLabel(noType);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(sourceConstantDynamic));
            methodVisitor.visitJumpInsn(Opcodes.IFEQ, noConstantDynamic);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(sourceConstantDynamic));
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                    implementationContext.getInstrumentedType().getInternalName(),
                    ConstantDynamicTranslator.NAME,
                    Type.getMethodDescriptor(Type.getType(sourceConstantDynamic), Type.getType(targetConstantDynamic)),
                    false);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            methodVisitor.visitLabel(noConstantDynamic);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(1, 1);
        }
    }

    protected static class ConstantArrayTranslator implements ByteCodeAppender {

        protected static final String NAME = "constants";

        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
            Label loop = new Label(), end = new Label();
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitInsn(Opcodes.ARRAYLENGTH);
            methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(Object.class));
            methodVisitor.visitVarInsn(Opcodes.ASTORE, 1);
            methodVisitor.visitInsn(Opcodes.ICONST_0);
            methodVisitor.visitVarInsn(Opcodes.ISTORE, 2);
            methodVisitor.visitLabel(loop);
            methodVisitor.visitVarInsn(Opcodes.ILOAD, 2);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitInsn(Opcodes.ARRAYLENGTH);
            methodVisitor.visitJumpInsn(Opcodes.IF_ICMPGE, end);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitVarInsn(Opcodes.ILOAD, 2);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitVarInsn(Opcodes.ILOAD, 2);
            methodVisitor.visitInsn(Opcodes.AALOAD);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                    implementationContext.getInstrumentedType().getInternalName(),
                    ConstantTranslator.NAME,
                    Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class)),
                    false);
            methodVisitor.visitInsn(Opcodes.AASTORE);
            methodVisitor.visitIincInsn(2, 1);
            methodVisitor.visitJumpInsn(Opcodes.GOTO, loop);
            methodVisitor.visitLabel(end);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(4, 3);
        }
    }

    protected static class FrameTranslator implements ByteCodeAppender {

        protected static final String NAME = "frames";

        protected final Class<?> source, target;

        protected FrameTranslator(Class<?> source, Class<?> target) {
            this.source = source;
            this.target = target;
        }

        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
            Label nullCheck = new Label(), loop = new Label(), store = new Label(), end = new Label(), label = new Label();
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitJumpInsn(Opcodes.IFNONNULL, nullCheck);
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitLabel(nullCheck);
            methodVisitor.visitLineNumber(43, nullCheck);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitInsn(Opcodes.ARRAYLENGTH);
            methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(Object.class));
            methodVisitor.visitVarInsn(Opcodes.ASTORE, 2);
            methodVisitor.visitInsn(Opcodes.ICONST_0);
            methodVisitor.visitVarInsn(Opcodes.ISTORE, 3);
            methodVisitor.visitLabel(loop);
            methodVisitor.visitFrame(Opcodes.F_APPEND, 2, new Object[]{Type.getInternalName(Object[].class), Opcodes.INTEGER}, 0, null);
            methodVisitor.visitVarInsn(Opcodes.ILOAD, 3);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitInsn(Opcodes.ARRAYLENGTH);
            methodVisitor.visitJumpInsn(Opcodes.IF_ICMPGE, end);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitVarInsn(Opcodes.ILOAD, 3);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitVarInsn(Opcodes.ILOAD, 3);
            methodVisitor.visitInsn(Opcodes.AALOAD);
            methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(source));
            methodVisitor.visitJumpInsn(Opcodes.IFEQ, label);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitVarInsn(Opcodes.ILOAD, 3);
            methodVisitor.visitInsn(Opcodes.AALOAD);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(source));
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                    implementationContext.getInstrumentedType().getInternalName(),
                    LabelTranslator.NAME,
                    Type.getMethodDescriptor(Type.getType(target), Type.getType(source)),
                    false);
            methodVisitor.visitJumpInsn(Opcodes.GOTO, store);
            methodVisitor.visitLabel(label);
            methodVisitor.visitFrame(Opcodes.F_FULL,
                    4,
                    new Object[]{implementationContext.getInstrumentedType().getInternalName(), Type.getInternalName(Object[].class), Type.getInternalName(Object[].class), Opcodes.INTEGER},
                    2,
                    new Object[]{Type.getInternalName(Object[].class), Opcodes.INTEGER});
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitVarInsn(Opcodes.ILOAD, 3);
            methodVisitor.visitInsn(Opcodes.AALOAD);
            methodVisitor.visitLabel(store);
            methodVisitor.visitFrame(Opcodes.F_FULL,
                    4,
                    new Object[]{implementationContext.getInstrumentedType().getInternalName(), Type.getInternalName(Object[].class), Type.getInternalName(Object[].class), Opcodes.INTEGER},
                    3,
                    new Object[]{Type.getInternalName(Object[].class), Opcodes.INTEGER, Type.getInternalName(Object.class)});
            methodVisitor.visitInsn(Opcodes.AASTORE);
            methodVisitor.visitIincInsn(3, 1);
            methodVisitor.visitJumpInsn(Opcodes.GOTO, loop);
            methodVisitor.visitLabel(end);
            methodVisitor.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(5, 4);
        }
    }

    protected static class TypePathTranslator implements ByteCodeAppender {

        protected static final String NAME = "typePath";

        protected final Class<?> source, target;

        protected TypePathTranslator(Class<?> source, Class<?> target) {
            this.source = source;
            this.target = target;
        }

        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
            Label nullCheck = new Label(), end = new Label();
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitJumpInsn(Opcodes.IFNONNULL, nullCheck);
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            methodVisitor.visitJumpInsn(Opcodes.GOTO, end);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitLabel(nullCheck);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    Type.getInternalName(source),
                    "toString",
                    Type.getMethodDescriptor(Type.getType(String.class)),
                    false);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                    Type.getInternalName(target),
                    "fromString",
                    Type.getMethodDescriptor(Type.getType(String.class), Type.getType(target)),
                    false);
            methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Type.getInternalName(target)});
            methodVisitor.visitLabel(end);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(1, 2);
        }
    }
}
