package net.bytebuddy.implementation;

import java.lang.reflect.InvocationTargetException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.JavaVersionRule;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationToGetterTest {
	public final static String GETTER_NAME="getTarget";
	
    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    public void lazyProxyTest() throws Exception {
		Generic generic=TypeDescription.Generic.Builder.parameterizedType(GetterTestBase.class, TestTarget.class).build();

    	DynamicType.Loaded<?> loaded=new ByteBuddy()
        		.subclass(generic)
        		.implement(TestTarget.class)
                .method(isDeclaredBy(TestTarget.class))
                .intercept(MethodDelegation.withDefaultConfiguration()
                        .filter(isDeclaredBy(TestTarget.class))
                        .toGetter(GETTER_NAME))
                .make()
                .load(GetterTestBase.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
    	TestTarget proxy=(TestTarget)loaded.getLoaded().getConstructor(Class.class).newInstance(TestTargetImpl.class);
    	assertThat(proxy.foo(), is(1001));
    }
    
    @SuppressWarnings("unused")
	@Test(expected=IllegalStateException.class)
    public void noMethodTest() throws Exception {
		Generic generic=TypeDescription.Generic.Builder.parameterizedType(GetterInvalidBase.class, TestTarget.class).build();
    	DynamicType.Loaded<?> loaded=new ByteBuddy()
        		.subclass(generic)
        		.implement(TestTarget.class)
                .method(isDeclaredBy(TestTarget.class))
                .intercept(MethodDelegation.withDefaultConfiguration()
                        .filter(isDeclaredBy(TestTarget.class))
                        .toGetter(GETTER_NAME))
                .make()
                .load(GetterTestBase.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
    	
    }
    
    public static class GetterTestBase<T> {
    	private final Class<T> targetType;
    	
    	private T target;
    	
    	public GetterTestBase(Class<T> targetType) {
    		this.targetType=targetType;
		}
    	
    	public T getTarget() {
    		if(target==null) {	// lazy initialization
    			try {
					target=targetType.getConstructor().newInstance();
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new IllegalStateException("Cannot create: "+targetType, e);
				}
    		}
    		return target;
    	}
    }
    
    public static class GetterInvalidBase<T> {
    	public T getTarget(int something) {
    		return null;
    	}
    }
    
    public interface TestTarget {
    	int foo();
    }
    
    public static class TestTargetImpl implements TestTarget {
    	@Override
    	public int foo() {
    		return 1001;
    	}
    }
}
