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
import net.bytebuddy.implementation.bytecode.assign.Assigner;
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

    private static final String WRAP = "wrap";

    public static <S> ClassVisitorFactory<S> of(Class<S> classVisitor) {
        return of(classVisitor, new ByteBuddy().with(TypeValidation.DISABLED));
    }

    public static <S> ClassVisitorFactory<S> of(Class<S> classVisitor, ByteBuddy byteBuddy) {
        try {
            String prefix = classVisitor.getPackage().getName();
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
                equivalents.put(type, equivalent);
                builders.put(type, byteBuddy.subclass(type, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                        .defineField(DELEGATE, equivalent, Visibility.PRIVATE, FieldManifestation.FINAL)
                        .defineConstructor(Visibility.PUBLIC)
                        .withParameters(equivalent)
                        .intercept(MethodCall.invoke(type.getDeclaredConstructor(int.class))
                                .with(OpenedClassReader.ASM_API)
                                .andThen(FieldAccessor.ofField(DELEGATE).setsArgumentAt(0)))
                        .defineMethod(WRAP, type, Visibility.PUBLIC, Ownership.STATIC)
                        .withParameters(equivalent)
                        .intercept(new Implementation.Simple(new NullCheckedConstruction(equivalent))));
                builders.put(equivalent, byteBuddy.subclass(equivalent, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                        .defineField(DELEGATE, type, Visibility.PRIVATE, FieldManifestation.FINAL)
                        .defineConstructor(Visibility.PUBLIC)
                        .withParameters(type)
                        .intercept(MethodCall.invoke(equivalent.getDeclaredConstructor(int.class))
                                .with(OpenedClassReader.ASM_API)
                                .andThen(FieldAccessor.ofField(DELEGATE).setsArgumentAt(0)))
                        .defineMethod(WRAP, equivalent, Visibility.PUBLIC, Ownership.STATIC)
                        .withParameters(type)
                        .intercept(new Implementation.Simple(new NullCheckedConstruction(type))));
            }
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
            List<DynamicType> dynamicTypes = new ArrayList<DynamicType>();
            Map<Class<?>, TypeDescription> generated = new HashMap<Class<?>, TypeDescription>();
            for (Map.Entry<Class<?>, Class<?>> entry : equivalents.entrySet()) {
                DynamicType.Builder<?> wrapper = builders.get(entry.getKey()), unwrapper = builders.get(entry.getValue());
                if (entry.getKey() == MethodVisitor.class) {
                    // TODO: add methods to translate utilities (ASMify)
                }
                for (Method method : entry.getKey().getMethods()) {
                    Class<?>[] parameter = method.getParameterTypes(), match = new Class<?>[parameter.length];
                    List<MethodCall.ArgumentLoader.Factory> left = new ArrayList<MethodCall.ArgumentLoader.Factory>(parameter.length);
                    List<MethodCall.ArgumentLoader.Factory> right = new ArrayList<MethodCall.ArgumentLoader.Factory>(match.length);
                    for (int index = 0; index < parameter.length; index++) {
                        Class<?> component = parameter[index];
                        int arity = 0;
                        while (component.isArray()) {
                            arity += 1;
                            component = component.getComponentType();
                        }
                        Class<?> substitute = utilities.get(component);
                        if (substitute == null) {
                            match[index] = match[index];
                        } else if (arity > 0) {
                            StringBuilder stringBuilder = new StringBuilder();
                            while (arity-- > 0) {
                                stringBuilder.append("[");
                            }
                            match[index] = Class.forName(stringBuilder.append("L")
                                    .append(substitute.getName().replace('.', '/'))
                                    .append(";")
                                    .toString(), false, substitute.getClassLoader());
                        } else {
                            match[index] = substitute;
                        }
                        if (component == Label.class) { // TODO: add wrappers

                        } else if (component == Type.class) {

                        } else if (component == Handle.class) {

                        } else if (component == ConstantDynamic.class) {

                        } else {
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
                    "(" + Type.getDescriptor(target) + ")V",
                    false);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            methodVisitor.visitLabel(label);
            if (implementationContext.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V6)) {
                methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(3, 1);
        }
    }
}
