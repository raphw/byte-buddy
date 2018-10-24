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
package net.bytebuddy.build;

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
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.RandomString;

import java.lang.annotation.*;
import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A plugin that caches the return value of a method in a synthetic field. The caching mechanism is not thread-safe but can be used in a
 * concurrent setup if the cached value is frozen, i.e. only defines {@code final} fields. In this context, it is possible that
 * the method is executed multiple times by different threads but at the same time, this approach avoids a {@code volatile} field
 * declaration. For methods with a primitive return type, the type's default value is used to indicate that a method was not yet invoked.
 * For methods that return a reference type, {@code null} is used as an indicator. If a method returns such a value, this mechanism will
 * not work. This plugin does not need to be closed.
 */
@HashCodeAndEqualsPlugin.Enhance
public class CachedReturnPlugin extends Plugin.ForElementMatcher implements Plugin.Factory {

    /**
     * An infix between a field and the random suffix if no field name is chosen.
     */
    private static final String NAME_INFIX = "_";

    /**
     * The infix symbol for advice classes.
     */
    private static final String ADVICE_INFIX = "$";

    /**
     * A random string to use for avoid field name collisions.
     */
    @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.IGNORE)
    private final RandomString randomString;

    /**
     * The class file locator to use.
     */
    private final ClassFileLocator classFileLocator;

    /**
     * A map of advice types mapped by their argument type. All advice types are precompiled using Java 6 to allow
     * for releasing Byte Buddy with a Java 5 byte code level where compiled classes do not contain stack map frames.
     * Byte Buddy filters stack map frames when applying advice in newer version but it cannot add stack map frames
     * without explicit frame computation which is expensive which is why precompilation was used. To avoid loading
     * Java classes in incompatible versions, all advice types are resolved using a type pool.
     */
    @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.IGNORE)
    private final Map<TypeDescription, TypeDescription> adviceByType;

    /**
     * Creates a plugin for caching method return values.
     */
    public CachedReturnPlugin() {
        super(declaresMethod(isAnnotatedWith(Enhance.class)));
        randomString = new RandomString();
        classFileLocator = ClassFileLocator.ForClassLoader.of(CachedReturnPlugin.class.getClassLoader());
        TypePool typePool = TypePool.Default.of(classFileLocator);
        adviceByType = new HashMap<TypeDescription, TypeDescription>();
        for (Class<?> type : new Class<?>[]{
                boolean.class,
                byte.class,
                short.class,
                char.class,
                int.class,
                long.class,
                float.class,
                double.class,
                Object.class
        }) {
            adviceByType.put(TypeDescription.ForLoadedType.ForLoadedType.of(type), typePool.describe(CachedReturnPlugin.class.getName()
                    + ADVICE_INFIX
                    + type.getSimpleName()).resolve());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Plugin make() {
        return this;
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
            } else if (methodDescription.getReturnType().represents(void.class)) {
                throw new IllegalStateException("Cannot cache void result for " + methodDescription);
            }
            String name = methodDescription.getDeclaredAnnotations().ofType(Enhance.class).loadSilent().value();
            if (name.length() == 0) {
                name = methodDescription.getName() + NAME_INFIX + randomString.nextString();
            }
            builder = builder
                    .defineField(name, methodDescription.getReturnType().asErasure(), methodDescription.isStatic()
                            ? Ownership.STATIC
                            : Ownership.MEMBER, Visibility.PRIVATE, SyntheticState.SYNTHETIC, FieldPersistence.TRANSIENT)
                    .visit(Advice.withCustomMapping()
                            .bind(CacheField.class, new CacheFieldOffsetMapping(name))
                            .to(adviceByType.get(methodDescription.getReturnType().isPrimitive()
                                    ? methodDescription.getReturnType().asErasure()
                                    : TypeDescription.OBJECT), this.classFileLocator)
                            .on(is(methodDescription)));
        }
        return builder;
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        /* do nothing */
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
    protected @interface CacheField {
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
}
