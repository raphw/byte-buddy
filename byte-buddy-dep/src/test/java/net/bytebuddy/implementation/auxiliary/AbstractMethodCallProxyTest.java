package net.bytebuddy.implementation.auxiliary;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodAccessorFactory;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.Opcodes;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AbstractMethodCallProxyTest {

    protected static final String FOO = "foo";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    protected Implementation.SpecialMethodInvocation specialMethodInvocation;

    @Mock
    private MethodAccessorFactory methodAccessorFactory;

    protected Class<?> proxyOnlyDeclaredMethodOf(Class<?> proxyTarget) throws Exception {
        MethodDescription.InDefinedShape proxyMethod = TypeDescription.ForLoadedType.of(proxyTarget)
                .getDeclaredMethods().filter(not(isConstructor())).getOnly();
        when(methodAccessorFactory.registerAccessorFor(specialMethodInvocation, MethodAccessorFactory.AccessType.DEFAULT)).thenReturn(proxyMethod);
        String auxiliaryTypeName = getClass().getName() + "$" + proxyTarget.getSimpleName() + "$Proxy";
        DynamicType dynamicType = new MethodCallProxy(specialMethodInvocation, false).make(auxiliaryTypeName,
                ClassFileVersion.ofThisVm(),
                methodAccessorFactory);
        DynamicType.Unloaded<?> unloaded = (DynamicType.Unloaded<?>) dynamicType;
        Class<?> auxiliaryType = unloaded.load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        assertThat(auxiliaryType.getName(), is(auxiliaryTypeName));
        verify(methodAccessorFactory).registerAccessorFor(specialMethodInvocation, MethodAccessorFactory.AccessType.DEFAULT);
        verifyNoMoreInteractions(methodAccessorFactory);
        verifyNoMoreInteractions(specialMethodInvocation);
        assertThat(auxiliaryType.getModifiers(), is(Opcodes.ACC_SYNTHETIC));
        assertThat(Callable.class.isAssignableFrom(auxiliaryType), is(true));
        assertThat(Runnable.class.isAssignableFrom(auxiliaryType), is(true));
        assertThat(auxiliaryType.getDeclaredConstructors().length, is(1));
        assertThat(auxiliaryType.getDeclaredMethods().length, is(2));
        assertThat(auxiliaryType.getDeclaredFields().length, is(proxyMethod.getParameters().size() + (proxyMethod.isStatic() ? 0 : 1)));
        int fieldIndex = 0;
        Field[] fields = auxiliaryType.getDeclaredFields();
        Arrays.sort(fields, new Comparator<Field>() {
            public int compare(Field field1, Field field2) {
                String typeName1 = field1.getType().getName();
                String typeName2 = field2.getType().getName();
                return typeName1.compareTo(typeName2);
            }
        });
        Class<?>[] parameterTypes = proxyTarget.getDeclaredMethods()[0].getParameterTypes();
        Arrays.sort(parameterTypes, new Comparator<Class<?>>() {
            public int compare(Class<?> class1, Class<?> class2) {
                String name1 = class1.getName();
                String name2 = class2.getName();
                return name1.compareTo(name2);
            }
        });
        if (!proxyMethod.isStatic()) {
        	Field field = fields[4];
            assertThat(field.getType(), CoreMatchers.<Class<?>>is(proxyTarget));
        }
        for(int i = 0; i < parameterTypes.length; i++) {
            Field field = fields[i];
            Class<?> fieldType = field.getType();
            Class<?> parameterType = parameterTypes[i];
            assertThat(fieldType, CoreMatchers.<Class<?>>is(parameterType));    
        }
        return auxiliaryType;
    }
}

