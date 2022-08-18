package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.FieldManifestation;
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
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.OpenedClassReader;
import org.objectweb.asm.*;

import java.lang.reflect.Method;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;

public abstract class ClassVisitorFactory<T> {

    private static final String DELEGATE = "delegate";

    public static <S> ClassVisitorFactory<S> of(Class<S> classVisitor) {
        return of(classVisitor, new ByteBuddy().with(TypeValidation.DISABLED));
    }

    @SuppressWarnings("unchecked")
    public static <S> ClassVisitorFactory<S> of(Class<S> classVisitor, ByteBuddy byteBuddy) {
        try {
            String prefix = classVisitor.getPackage().getName();
            Map<Class<?>, Class<?>> translated = new HashMap<Class<?>, Class<?>>();
            for (Class<?> type : Arrays.asList(
                    ClassVisitor.class,
                    AnnotationVisitor.class,
                    FieldVisitor.class,
                    MethodVisitor.class,
                    RecordComponentVisitor.class,
                    ModuleVisitor.class
            )) {
                translated.put(type, Class.forName(prefix + "." + type.getSimpleName()));
            }
            Map<Class<?>, Class<?>> substituted = new HashMap<Class<?>, Class<?>>(translated);
            for (Class<?> type : Arrays.asList(
                    Label.class,
                    Type.class,
                    Handle.class,
                    ConstantDynamic.class
            )) {
                substituted.put(type, Class.forName(prefix + "." + type.getSimpleName()));
            }
            List<DynamicType> dynamicTypes = new ArrayList<DynamicType>();
            Map<Class<?>, TypeDescription> generated = new HashMap<Class<?>, TypeDescription>();
            for (Map.Entry<Class<?>, Class<?>> entry : translated.entrySet()) {
                DynamicType.Builder<?> wrapper = byteBuddy.subclass(entry.getKey(), ConstructorStrategy.Default.NO_CONSTRUCTORS)
                        .defineField(DELEGATE, entry.getValue(), Visibility.PRIVATE, FieldManifestation.FINAL)
                        .defineConstructor(Visibility.PUBLIC)
                        .withParameters(entry.getValue())
                        .intercept(MethodCall.invoke(entry.getKey().getDeclaredConstructor(int.class))
                                .with(OpenedClassReader.ASM_API)
                                .andThen(FieldAccessor.ofField(DELEGATE).setsArgumentAt(0)));
                DynamicType.Builder<?> unwrapper = byteBuddy.subclass(entry.getValue(), ConstructorStrategy.Default.NO_CONSTRUCTORS)
                        .defineField(DELEGATE, entry.getKey(), Visibility.PRIVATE, FieldManifestation.FINAL)
                        .defineConstructor(Visibility.PUBLIC)
                        .withParameters(entry.getKey())
                        .intercept(MethodCall.invoke(entry.getValue().getDeclaredConstructor(int.class))
                                .with(OpenedClassReader.ASM_API)
                                .andThen(FieldAccessor.ofField(DELEGATE).setsArgumentAt(0)));
                for (Method method : entry.getKey().getMethods()) {
                    Implementation implementation;
                    try {
                        Class<?>[] left = method.getParameterTypes(), right = new Class<?>[left.length];
                        for (int index = 0; index < left.length; index++) {
                            Class<?> substitute = substituted.get(left[index]);
                            right[index] = substitute == null
                                    ? left[index]
                                    : substitute;
                        }
                        Method target = entry.getValue().getMethod(method.getName(), right);
                        implementation = MethodCall.invoke(target).onField(DELEGATE).withAllArguments();
                    } catch (NoSuchMethodException ignored) {
                        implementation = ExceptionMethod.throwing(UnsupportedOperationException.class);
                    }
                    wrapper = wrapper.method(is(method)).intercept(implementation);
                    unwrapper = unwrapper.method(is(method)).intercept(implementation);
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
}
