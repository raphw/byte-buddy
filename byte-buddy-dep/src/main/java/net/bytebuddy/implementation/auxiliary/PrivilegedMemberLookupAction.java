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
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodAccessorFactory;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.utility.CompoundList;

import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * A {@link PrivilegedExceptionAction} to lookup a method constant using an {@link java.security.AccessController}.
 */
public enum PrivilegedMemberLookupAction implements AuxiliaryType {

    /**
     * Looks up a method using {@link Class#getDeclaredMethod(String, Class[])}.
     */
    FOR_PUBLIC_METHOD("getMethod", "name", String.class, "parameters", Class[].class),

    /**
     * Looks up a method using {@link Class#getDeclaredMethod(String, Class[])}.
     */
    FOR_DECLARED_METHOD("getDeclaredMethod", "name", String.class, "parameters", Class[].class),

    /**
     * Looks up a method using {@link Class#getConstructor(Class[])}.
     */
    FOR_PUBLIC_CONSTRUCTOR("getConstructor", "parameters", Class[].class),

    /**
     * Looks up a method using {@link Class#getDeclaredConstructor(Class[])}.
     */
    FOR_DECLARED_CONSTRUCTOR("getDeclaredConstructor", "parameters", Class[].class);

    /**
     * The name of the field that holds the type instance to look the method up from.
     */
    private static final String TYPE_FIELD = "type";

    /**
     * The default constructor of the {@link Object} class.
     */
    private static final MethodDescription.InDefinedShape DEFAULT_CONSTRUCTOR = TypeDescription.OBJECT.getDeclaredMethods()
            .filter(isConstructor())
            .getOnly();

    /**
     * The method to invoke from the action.
     */
    private final MethodDescription.InDefinedShape methodDescription;

    /**
     * A mapping of field names to their types in a fixed iteration order.
     */
    private final Map<String, Class<?>> fields;

    /**
     * Creates a privileged method constant action with one argument.
     *
     * @param name  The name of the method.
     * @param field The name of a field to define.
     * @param type  The type of the field to define.
     */
    PrivilegedMemberLookupAction(String name, String field, Class<?> type) {
        try {
            methodDescription = new MethodDescription.ForLoadedMethod(Class.class.getMethod(name, type));
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Could not locate method: " + name, exception);
        }
        fields = Collections.<String, Class<?>>singletonMap(field, type);
    }

    /**
     * Creates a privileged method constant action with one argument.
     *
     * @param name        The name of the method.
     * @param firstField  The name of the first field to define.
     * @param firstType   The type of the first field to define.
     * @param secondField The name of the second field to define.
     * @param secondType  The type of the second field to define.
     */
    PrivilegedMemberLookupAction(String name, String firstField, Class<?> firstType, String secondField, Class<?> secondType) {
        try {
            methodDescription = new MethodDescription.ForLoadedMethod(Class.class.getMethod(name, firstType, secondType));
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Could not locate method: " + name, exception);
        }
        fields = new LinkedHashMap<String, Class<?>>();
        fields.put(firstField, firstType);
        fields.put(secondField, secondType);
    }

    /**
     * Returns an auxiliary type for loading the supplied method description as a constant.
     *
     * @param methodDescription The method description to represent as a constant.
     * @return An appropriate auxiliary type.
     */
    public static AuxiliaryType of(MethodDescription methodDescription) {
        if (methodDescription.isConstructor()) {
            return methodDescription.isPublic()
                    ? FOR_PUBLIC_CONSTRUCTOR
                    : FOR_DECLARED_CONSTRUCTOR;
        } else if (methodDescription.isMethod()) {
            return methodDescription.isPublic()
                    ? FOR_PUBLIC_METHOD
                    : FOR_DECLARED_METHOD;
        } else {
            throw new IllegalStateException("Cannot load constant for type initializer: " + methodDescription);
        }
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType make(String auxiliaryTypeName,
                            ClassFileVersion classFileVersion,
                            MethodAccessorFactory methodAccessorFactory) {
        Implementation.Composable constructor = MethodCall.invoke(DEFAULT_CONSTRUCTOR).andThen(FieldAccessor.ofField(TYPE_FIELD).setsArgumentAt(0));
        int index = 1;
        for (String field : fields.keySet()) {
            constructor = constructor.andThen(FieldAccessor.ofField(field).setsArgumentAt(index++));
        }
        DynamicType.Builder<?> builder = new ByteBuddy(classFileVersion)
                .with(TypeValidation.DISABLED)
                .subclass(PrivilegedExceptionAction.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(auxiliaryTypeName)
                .modifiers(DEFAULT_TYPE_MODIFIER)
                .defineConstructor(Visibility.PUBLIC)
                .withParameters(CompoundList.of(Class.class, new ArrayList<Class<?>>(fields.values())))
                .intercept(constructor)
                .method(named("run"))
                .intercept(MethodCall.invoke(methodDescription)
                        .onField(TYPE_FIELD)
                        .withField(fields.keySet().toArray(new String[0])))
                .defineField(TYPE_FIELD, Class.class, Visibility.PRIVATE);
        for (Map.Entry<String, Class<?>> entry : fields.entrySet()) {
            builder = builder.defineField(entry.getKey(), entry.getValue(), Visibility.PRIVATE);
        }
        return builder.make();
    }
}
