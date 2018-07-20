package net.bytebuddy.implementation.auxiliary;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodAccessorFactory;
import net.bytebuddy.implementation.MethodCall;

import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;

public enum MethodConstantAction implements AuxiliaryType {

    FOR_METHOD("getDeclaredMethod", "name", String.class, "parameters", Class[].class),

    FOR_CONSTRUCTOR("getDeclaredConstructor", "parameters", Class[].class);

    private static final MethodDescription.InDefinedShape DEFAULT_CONSTRUCTOR = TypeDescription.OBJECT.getDeclaredMethods()
            .filter(isConstructor())
            .getOnly();

    private final MethodDescription.InDefinedShape methodDescription;

    private final Map<String, Class<?>> fields;

    MethodConstantAction(String name, String field, Class<?> type) {
        try {
            methodDescription = new MethodDescription.ForLoadedMethod(Class.class.getMethod(name, type));
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Could not locate method: " + name, exception);
        }
        fields = new LinkedHashMap<String, Class<?>>();
        fields.put("type", Class.class);
        fields.put(field, type);
    }

    MethodConstantAction(String name, String firstField, Class<?> firstType, String secondField, Class<?> secondType) {
        try {
            methodDescription = new MethodDescription.ForLoadedMethod(Class.class.getMethod(name, firstType, secondType));
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Could not locate method: " + name, exception);
        }
        fields = new LinkedHashMap<String, Class<?>>();
        fields.put("type", Class.class);
        fields.put(firstField, firstType);
        fields.put(secondField, secondType);
    }

    @Override
    public DynamicType make(String auxiliaryTypeName,
                            ClassFileVersion classFileVersion,
                            MethodAccessorFactory methodAccessorFactory) {
        Implementation.Composable constructor = MethodCall.invoke(DEFAULT_CONSTRUCTOR);
        int index = 0;
        for (String field : fields.keySet()) {
            constructor = constructor.andThen(FieldAccessor.ofField(field).setsArgumentAt(++index));
        }
        DynamicType.Builder<?> builder = new ByteBuddy(classFileVersion)
                .with(TypeValidation.DISABLED)
                .subclass(PrivilegedExceptionAction.class)
                .name(auxiliaryTypeName)
                .modifiers(DEFAULT_TYPE_MODIFIER)
                .defineConstructor(Visibility.PUBLIC)
                .withParameters(new ArrayList<Class<?>>(fields.values()))
                .intercept(constructor)
                .method(named("run"))
                .intercept(MethodCall.invoke(methodDescription).withField(fields.keySet().toArray(new String[fields.size()])));
        for (Map.Entry<String, Class<?>> entry : fields.entrySet()) {
            builder = builder.defineField(entry.getKey(), entry.getValue(), Visibility.PRIVATE);
        }
        return builder.make();
    }
}
