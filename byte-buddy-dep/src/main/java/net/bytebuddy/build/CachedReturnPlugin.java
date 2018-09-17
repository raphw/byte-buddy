package net.bytebuddy.build;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.FieldPersistence;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.utility.RandomString;

import java.lang.annotation.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A plugin that caches the return value of a method in a synthetic field. The caching mechanism is not thread-safe but can be used in a
 * concurrent setup if the cached value is frozen, i.e. only defines {@code final} fields. In this context, it is possible that
 * the method is executed multiple times by different threads but at the same time, this approach avoids a {@code volatile} field
 * declaration. For methods with a primitive return type, the type's default value is used to indicate that a method was not yet invoked.
 * For methods that return a reference type, {@code null} is used as an indicator. If a method returns such a value, this mechanism will
 * not work.
 */
public class CachedReturnPlugin extends Plugin.ForElementMatcher {

    /**
     * An infix between a field and the random suffix if no field name is chosen.
     */
    private static final String NAME_INFIX = "_";

    /**
     * A random string to use for avoid field name collisions.
     */
    private final RandomString randomString;

    /**
     * Creates a plugin for caching method return values.
     */
    public CachedReturnPlugin() {
        super(declaresMethod(isAnnotatedWith(Enhance.class)));
        randomString = new RandomString();
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        for (MethodDescription.InDefinedShape methodDescription : typeDescription.getDeclaredMethods()
                .filter(not(isBridge()).<MethodDescription>and(isAnnotatedWith(Enhance.class)))) {
            if (methodDescription.isAbstract()) {
                throw new IllegalStateException("Cannot cache the value of an abstract method: " + methodDescription);
            } else if (!methodDescription.getParameters().isEmpty()) {
                throw new IllegalStateException("Cannot cache the value of a method with parameters: " + methodDescription);
            }
            String name = methodDescription.getDeclaredAnnotations().ofType(Enhance.class).loadSilent().value();
            if (name.length() == 0) {
                name = methodDescription.getName() + NAME_INFIX + randomString.nextString();
            }
            Class<?> advice;
            if (!methodDescription.getReturnType().isPrimitive()) {
                advice = ReferenceAdvice.class;
            } else if (methodDescription.getReturnType().represents(boolean.class)) {
                advice = BooleanAdvice.class;
            } else if (methodDescription.getReturnType().represents(byte.class)) {
                advice = ByteAdvice.class;
            } else if (methodDescription.getReturnType().represents(short.class)) {
                advice = ShortAdvice.class;
            } else if (methodDescription.getReturnType().represents(char.class)) {
                advice = CharacterAdvice.class;
            } else if (methodDescription.getReturnType().represents(int.class)) {
                advice = IntegerAdvice.class;
            } else if (methodDescription.getReturnType().represents(long.class)) {
                advice = LongAdvice.class;
            } else if (methodDescription.getReturnType().represents(float.class)) {
                advice = FloatAdvice.class;
            } else if (methodDescription.getReturnType().represents(double.class)) {
                advice = DoubleAdvice.class;
            } else {
                throw new IllegalStateException("Cannot cache a method that returns void: " + methodDescription);
            }
            builder = builder
                    .defineField(name, methodDescription.getReturnType().asErasure(), methodDescription.isStatic()
                            ? Ownership.STATIC
                            : Ownership.MEMBER, Visibility.PRIVATE, SyntheticState.SYNTHETIC, FieldPersistence.TRANSIENT)
                    .visit(Advice.withCustomMapping()
                            .bind(CacheField.class, new CacheFieldOffsetMapping(name))
                            .to(advice)
                            .on(is(methodDescription)));
        }
        return builder;
    }

    /**
     * Indicates methods that should be cached, i.e. where the return value is stored in a synthetic field. For this to be
     * possible, the returned value should not be altered and the instance must be thread-safe if the value might be used from
     * multiple threads.
     */
    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Enhance {

        /**
         * The fields name or an empty string if the name should be generated randomly.
         *
         * @return The fields name or an empty string if the name should be generated randomly.
         */
        String value() default "";
    }

    /**
     * Indicates the field that stores the cached value.
     */
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface CacheField {
        /* empty */
    }

    /**
     * An offset mapping for the cached field.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class CacheFieldOffsetMapping implements Advice.OffsetMapping {

        /**
         * The field's name.
         */
        private final String name;

        /**
         * Creates an offset mapping for the cached field.
         *
         * @param name The field's name.
         */
        protected CacheFieldOffsetMapping(String name) {
            this.name = name;
        }

        /**
         * {@inheritDoc}
         */
        public Target resolve(TypeDescription instrumentedType,
                              MethodDescription instrumentedMethod,
                              Assigner assigner,
                              Advice.ArgumentHandler argumentHandler,
                              Sort sort) {
            return new Target.ForField.ReadWrite(instrumentedType.getDeclaredFields().filter(named(name)).getOnly());
        }
    }

    /**
     * An advice class for caching a {@code boolean} value.
     */
    private static class BooleanAdvice {

        /**
         * A constructor that prohibits the instantiation of the class.
         */
        private BooleanAdvice() {
            throw new UnsupportedOperationException("This class is merely an advice template and should not be instantiated");
        }

        /**
         * The enter advice.
         *
         * @param cached The cached field's value.
         * @return {@code true} if a cached value exists.
         */
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        protected static boolean enter(@CacheField boolean cached) {
            return cached;
        }

        /**
         * The exit advice.
         *
         * @param returned The value that was returned by the method's execution or {@code 0} if it was not executed.
         * @param cached   The previously cached value or {@code 0} if no previous value exists.
         */
        @Advice.OnMethodExit
        @SuppressFBWarnings(value = {"UC_USELESS_VOID_METHOD", "IP_PARAMETER_IS_DEAD_BUT_OVERWRITTEN"}, justification = "Advice method serves as a template")
        protected static void exit(@Advice.Return(readOnly = false) boolean returned, @CacheField boolean cached) {
            if (returned) {
                cached = true;
            } else {
                returned = true;
            }
        }
    }

    /**
     * An advice class for caching a {@code byte} value.
     */
    private static class ByteAdvice {

        /**
         * A constructor that prohibits the instantiation of the class.
         */
        private ByteAdvice() {
            throw new UnsupportedOperationException("This class is merely an advice template and should not be instantiated");
        }

        /**
         * The enter advice.
         *
         * @param cached The cached field's value.
         * @return {@code true} if a cached value exists.
         */
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        protected static byte enter(@CacheField byte cached) {
            return cached;
        }

        /**
         * The exit advice.
         *
         * @param returned The value that was returned by the method's execution or {@code 0} if it was not executed.
         * @param cached   The previously cached value or {@code 0} if no previous value exists.
         */
        @Advice.OnMethodExit
        @SuppressFBWarnings(value = {"UC_USELESS_VOID_METHOD", "DLS_DEAD_LOCAL_STORE"}, justification = "Advice method serves as a template")
        protected static void exit(@Advice.Return(readOnly = false) byte returned, @CacheField byte cached) {
            if (returned == 0) {
                returned = cached;
            } else {
                cached = returned;
            }
        }
    }

    /**
     * An advice class for caching a {@code short} value.
     */
    private static class ShortAdvice {

        /**
         * A constructor that prohibits the instantiation of the class.
         */
        private ShortAdvice() {
            throw new UnsupportedOperationException("This class is merely an advice template and should not be instantiated");
        }

        /**
         * The enter advice.
         *
         * @param cached The cached field's value.
         * @return {@code true} if a cached value exists.
         */
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        protected static short enter(@CacheField short cached) {
            return cached;
        }

        /**
         * The exit advice.
         *
         * @param returned The value that was returned by the method's execution or {@code 0} if it was not executed.
         * @param cached   The previously cached value or {@code 0} if no previous value exists.
         */
        @Advice.OnMethodExit
        @SuppressFBWarnings(value = {"UC_USELESS_VOID_METHOD", "DLS_DEAD_LOCAL_STORE"}, justification = "Advice method serves as a template")
        protected static void exit(@Advice.Return(readOnly = false) short returned, @CacheField short cached) {
            if (returned == 0) {
                returned = cached;
            } else {
                cached = returned;
            }
        }
    }

    /**
     * An advice class for caching a {@code char} value.
     */
    private static class CharacterAdvice {

        /**
         * A constructor that prohibits the instantiation of the class.
         */
        private CharacterAdvice() {
            throw new UnsupportedOperationException("This class is merely an advice template and should not be instantiated");
        }

        /**
         * The enter advice.
         *
         * @param cached The cached field's value.
         * @return {@code true} if a cached value exists.
         */
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        protected static char enter(@CacheField char cached) {
            return cached;
        }

        /**
         * The exit advice.
         *
         * @param returned The value that was returned by the method's execution or {@code 0} if it was not executed.
         * @param cached   The previously cached value or {@code 0} if no previous value exists.
         */
        @Advice.OnMethodExit
        @SuppressFBWarnings(value = {"UC_USELESS_VOID_METHOD", "DLS_DEAD_LOCAL_STORE"}, justification = "Advice method serves as a template")
        protected static void exit(@Advice.Return(readOnly = false) char returned, @CacheField char cached) {
            if (returned == 0) {
                returned = cached;
            } else {
                cached = returned;
            }
        }
    }

    /**
     * An advice class for caching a {@code int} value.
     */
    private static class IntegerAdvice {

        /**
         * A constructor that prohibits the instantiation of the class.
         */
        private IntegerAdvice() {
            throw new UnsupportedOperationException("This class is merely an advice template and should not be instantiated");
        }

        /**
         * The enter advice.
         *
         * @param cached The cached field's value.
         * @return {@code true} if a cached value exists.
         */
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        protected static int enter(@CacheField int cached) {
            return cached;
        }

        /**
         * The exit advice.
         *
         * @param returned The value that was returned by the method's execution or {@code 0} if it was not executed.
         * @param cached   The previously cached value or {@code 0} if no previous value exists.
         */
        @Advice.OnMethodExit
        @SuppressFBWarnings(value = {"UC_USELESS_VOID_METHOD", "DLS_DEAD_LOCAL_STORE"}, justification = "Advice method serves as a template")
        protected static void exit(@Advice.Return(readOnly = false) int returned, @CacheField int cached) {
            if (returned == 0) {
                returned = cached;
            } else {
                cached = returned;
            }
        }
    }

    /**
     * An advice class for caching a {@code long} value.
     */
    private static class LongAdvice {

        /**
         * A constructor that prohibits the instantiation of the class.
         */
        private LongAdvice() {
            throw new UnsupportedOperationException("This class is merely an advice template and should not be instantiated");
        }

        /**
         * The enter advice.
         *
         * @param cached The cached field's value.
         * @return {@code true} if a cached value exists.
         */
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        protected static long enter(@CacheField long cached) {
            return cached;
        }

        /**
         * The exit advice.
         *
         * @param returned The value that was returned by the method's execution or {@code 0} if it was not executed.
         * @param cached   The previously cached value or {@code 0} if no previous value exists.
         */
        @Advice.OnMethodExit
        @SuppressFBWarnings(value = {"UC_USELESS_VOID_METHOD", "DLS_DEAD_LOCAL_STORE"}, justification = "Advice method serves as a template")
        protected static void exit(@Advice.Return(readOnly = false) long returned, @CacheField long cached) {
            if (returned == 0L) {
                returned = cached;
            } else {
                cached = returned;
            }
        }
    }

    /**
     * An advice class for caching a {@code float} value.
     */
    private static class FloatAdvice {

        /**
         * A constructor that prohibits the instantiation of the class.
         */
        private FloatAdvice() {
            throw new UnsupportedOperationException("This class is merely an advice template and should not be instantiated");
        }

        /**
         * The enter advice.
         *
         * @param cached The cached field's value.
         * @return {@code true} if a cached value exists.
         */
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        protected static float enter(@CacheField float cached) {
            return cached;
        }

        /**
         * The exit advice.
         *
         * @param returned The value that was returned by the method's execution or {@code 0} if it was not executed.
         * @param cached   The previously cached value or {@code 0} if no previous value exists.
         */
        @Advice.OnMethodExit
        @SuppressFBWarnings(value = {"UC_USELESS_VOID_METHOD", "DLS_DEAD_LOCAL_STORE"}, justification = "Advice method serves as a template")
        protected static void exit(@Advice.Return(readOnly = false) float returned, @CacheField float cached) {
            if (returned == 0f) {
                returned = cached;
            } else {
                cached = returned;
            }
        }
    }

    /**
     * An advice class for caching a {@code double} value.
     */
    private static class DoubleAdvice {

        /**
         * A constructor that prohibits the instantiation of the class.
         */
        private DoubleAdvice() {
            throw new UnsupportedOperationException("This class is merely an advice template and should not be instantiated");
        }

        /**
         * The enter advice.
         *
         * @param cached The cached field's value.
         * @return {@code true} if a cached value exists.
         */
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        protected static double enter(@CacheField double cached) {
            return cached;
        }

        /**
         * The exit advice.
         *
         * @param returned The value that was returned by the method's execution or {@code 0} if it was not executed.
         * @param cached   The previously cached value or {@code 0} if no previous value exists.
         */
        @Advice.OnMethodExit
        @SuppressFBWarnings(value = {"UC_USELESS_VOID_METHOD", "DLS_DEAD_LOCAL_STORE"}, justification = "Advice method serves as a template")
        protected static void exit(@Advice.Return(readOnly = false) double returned, @CacheField double cached) {
            if (returned == 0d) {
                returned = cached;
            } else {
                cached = returned;
            }
        }
    }

    /**
     * An advice class for caching a reference value.
     */
    private static class ReferenceAdvice {

        /**
         * A constructor that prohibits the instantiation of the class.
         */
        private ReferenceAdvice() {
            throw new UnsupportedOperationException("This class is merely an advice template and should not be instantiated");
        }

        /**
         * The enter advice.
         *
         * @param cached The cached field's value.
         * @return {@code true} if a cached value exists.
         */
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        protected static Object enter(@CacheField Object cached) {
            return cached;
        }

        /**
         * The exit advice.
         *
         * @param returned The value that was returned by the method's execution or {@code 0} if it was not executed.
         * @param cached   The previously cached value or {@code 0} if no previous value exists.
         */
        @Advice.OnMethodExit
        @SuppressFBWarnings(value = {"UC_USELESS_VOID_METHOD", "DLS_DEAD_LOCAL_STORE"}, justification = "Advice method serves as a template")
        protected static void exit(@Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returned, @CacheField Object cached) {
            if (returned == null) {
                returned = cached;
            } else {
                cached = returned;
            }
        }
    }
}
