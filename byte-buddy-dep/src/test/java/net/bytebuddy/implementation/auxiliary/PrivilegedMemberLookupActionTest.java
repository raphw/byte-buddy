package net.bytebuddy.implementation.auxiliary;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodAccessorFactory;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(Parameterized.class)
public class PrivilegedMemberLookupActionTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        return Arrays.asList(new Object[][]{
                {PrivilegedMemberLookupAction.FOR_PUBLIC_METHOD, Object.class.getMethod("toString"), Object.class, "toString"},
                {PrivilegedMemberLookupAction.FOR_DECLARED_METHOD, Object.class.getMethod("toString"), Object.class, "toString"},
                {PrivilegedMemberLookupAction.FOR_PUBLIC_CONSTRUCTOR, Object.class.getConstructor(), Object.class, null},
                {PrivilegedMemberLookupAction.FOR_DECLARED_CONSTRUCTOR, Object.class.getConstructor(), Object.class, null}
        });
    }

    private final AuxiliaryType auxiliaryType;

    private final Member member;

    private final Class<?> type;

    private final String name;

    public PrivilegedMemberLookupActionTest(AuxiliaryType auxiliaryType, Member member, Class<?> type, String name) {
        this.auxiliaryType = auxiliaryType;
        this.member = member;
        this.type = type;
        this.name = name;
    }

    @Test
    public void testMemberLookup() throws Exception {
        DynamicType dynamicType = auxiliaryType.make("net.bytebuddy.test.Sample", ClassFileVersion.ofThisVm(), MethodAccessorFactory.Illegal.INSTANCE);
        Class<?> auxiliaryType = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                Collections.singletonMap(dynamicType.getTypeDescription().getTypeName(), dynamicType.getBytes()))
                .loadClass(dynamicType.getTypeDescription().getName());
        Object instance;
        if (name == null) {
            Constructor<?> constructor = auxiliaryType.getConstructor(Class.class, Class[].class);
            constructor.setAccessible(true);
            instance = constructor.newInstance(type, new Class<?>[0]);
        } else {
            Constructor<?> constructor = auxiliaryType.getConstructor(Class.class, String.class, Class[].class);
            constructor.setAccessible(true);
            instance = constructor.newInstance(type, name, new Class<?>[0]);
        }
        assertThat(instance, CoreMatchers.instanceOf(PrivilegedExceptionAction.class));
        assertThat(((PrivilegedExceptionAction) instance).run(), is((Object) member));
    }
}
