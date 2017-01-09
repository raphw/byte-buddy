package net.bytebuddy.agent.builder;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.JavaModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.concurrent.ExecutorService;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentBuilderDescriptionStrategyTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private AgentBuilder.LocationStrategy locationStrategy;

    @Mock
    private TypePool typePool;

    @Mock
    private TypeDescription typeDescription;

    @Test
    public void testDescriptionHybridWithLoaded() throws Exception {
        ClassFileLocator classFileLocator = ClassFileLocator.ForClassLoader.of(Object.class.getClassLoader());
        when(typePool.describe(Object.class.getName())).thenReturn(new TypePool.Resolution.Simple(typeDescription));
        when(locationStrategy.classFileLocator(Object.class.getClassLoader(), JavaModule.ofType(Object.class))).thenReturn(classFileLocator);
        TypeDescription typeDescription = AgentBuilder.DescriptionStrategy.Default.HYBRID.apply(Object.class.getName(),
                Object.class,
                typePool,
                mock(AgentBuilder.CircularityLock.class),
                Object.class.getClassLoader(),
                JavaModule.ofType(Object.class));
        assertThat(typeDescription, is(TypeDescription.OBJECT));
        assertThat(typeDescription, instanceOf(TypeDescription.ForLoadedType.class));
    }

    @Test
    public void testDescriptionHybridWithoutLoaded() throws Exception {
        when(typePool.describe(Object.class.getName())).thenReturn(new TypePool.Resolution.Simple(typeDescription));
        TypeDescription typeDescription = AgentBuilder.DescriptionStrategy.Default.HYBRID.apply(Object.class.getName(),
                null,
                typePool,
                mock(AgentBuilder.CircularityLock.class),
                Object.class.getClassLoader(),
                JavaModule.ofType(Object.class));
        assertThat(typeDescription, is(this.typeDescription));
    }

    @Test
    public void testDescriptionPoolOnly() throws Exception {
        when(typePool.describe(Object.class.getName())).thenReturn(new TypePool.Resolution.Simple(typeDescription));
        assertThat(AgentBuilder.DescriptionStrategy.Default.POOL_ONLY.apply(Object.class.getName(),
                Object.class,
                typePool,
                mock(AgentBuilder.CircularityLock.class),
                Object.class.getClassLoader(),
                JavaModule.ofType(Object.class)), is(typeDescription));
    }

    @Test
    public void testSuperTypeLoading() throws Exception {
        assertThat(AgentBuilder.DescriptionStrategy.Default.HYBRID.withSuperTypeLoading(), is((AgentBuilder.DescriptionStrategy) new AgentBuilder.DescriptionStrategy.SuperTypeLoading(AgentBuilder.DescriptionStrategy.Default.HYBRID)));
        assertThat(AgentBuilder.DescriptionStrategy.Default.POOL_FIRST.withSuperTypeLoading(), is((AgentBuilder.DescriptionStrategy) new AgentBuilder.DescriptionStrategy.SuperTypeLoading(AgentBuilder.DescriptionStrategy.Default.POOL_FIRST)));
        assertThat(AgentBuilder.DescriptionStrategy.Default.POOL_ONLY.withSuperTypeLoading(), is((AgentBuilder.DescriptionStrategy) new AgentBuilder.DescriptionStrategy.SuperTypeLoading(AgentBuilder.DescriptionStrategy.Default.POOL_ONLY)));
    }

    @Test
    public void testAsynchronousSuperTypeLoading() throws Exception {
        ExecutorService executorService = mock(ExecutorService.class);
        assertThat(AgentBuilder.DescriptionStrategy.Default.HYBRID.withSuperTypeLoading(executorService),
                is((AgentBuilder.DescriptionStrategy) new AgentBuilder.DescriptionStrategy.SuperTypeLoading.Asynchronous(AgentBuilder.DescriptionStrategy.Default.HYBRID, executorService)));
        assertThat(AgentBuilder.DescriptionStrategy.Default.POOL_FIRST.withSuperTypeLoading(executorService),
                is((AgentBuilder.DescriptionStrategy) new AgentBuilder.DescriptionStrategy.SuperTypeLoading.Asynchronous(AgentBuilder.DescriptionStrategy.Default.POOL_FIRST, executorService)));
        assertThat(AgentBuilder.DescriptionStrategy.Default.POOL_ONLY.withSuperTypeLoading(executorService),
                is((AgentBuilder.DescriptionStrategy) new AgentBuilder.DescriptionStrategy.SuperTypeLoading.Asynchronous(AgentBuilder.DescriptionStrategy.Default.POOL_ONLY, executorService)));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.DescriptionStrategy.Default.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.DescriptionStrategy.SuperTypeLoading.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.DescriptionStrategy.SuperTypeLoading.UnlockingClassLoadingDelegate.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.DescriptionStrategy.SuperTypeLoading.Asynchronous.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.DescriptionStrategy.SuperTypeLoading.Asynchronous.ThreadSwitchingClassLoadingDelegate.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.DescriptionStrategy.SuperTypeLoading.Asynchronous.ThreadSwitchingClassLoadingDelegate.SimpleClassLoadingAction.class).apply();
    }
}
