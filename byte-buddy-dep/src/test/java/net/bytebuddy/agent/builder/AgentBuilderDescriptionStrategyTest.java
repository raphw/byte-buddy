package net.bytebuddy.agent.builder;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

public class AgentBuilderDescriptionStrategyTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private AgentBuilder.TypeLocator typeLocator;

    @Mock
    private TypePool typePool;

    @Mock
    private TypeDescription typeDescription;

    @Test
    public void testDescriptionHybridWithLoaded() throws Exception {
        ClassFileLocator classFileLocator = ClassFileLocator.ForClassLoader.of(Object.class.getClassLoader());
        when(typeLocator.typePool(classFileLocator, Object.class.getClassLoader())).thenReturn(typePool);
        when(typePool.describe(Object.class.getName())).thenReturn(new TypePool.Resolution.Simple(typeDescription));
        TypeDescription typeDescription = AgentBuilder.DescriptionStrategy.Default.HYBRID.apply(Object.class.getName(),
                Object.class,
                typeLocator,
                Object.class.getClassLoader(),
                classFileLocator);
        assertThat(typeDescription, is(TypeDescription.OBJECT));
        assertThat(typeDescription, instanceOf(TypeDescription.ForLoadedType.class));
    }

    @Test
    public void testDescriptionHybridWithoutLoaded() throws Exception {
        ClassFileLocator classFileLocator = ClassFileLocator.ForClassLoader.of(Object.class.getClassLoader());
        when(typeLocator.typePool(classFileLocator, Object.class.getClassLoader())).thenReturn(typePool);
        when(typePool.describe(Object.class.getName())).thenReturn(new TypePool.Resolution.Simple(typeDescription));
        TypeDescription typeDescription = AgentBuilder.DescriptionStrategy.Default.HYBRID.apply(Object.class.getName(),
                null,
                typeLocator,
                Object.class.getClassLoader(),
                classFileLocator);
        assertThat(typeDescription, is(this.typeDescription));
    }

    @Test
    public void testDescriptionPoolOnly() throws Exception {
        ClassFileLocator classFileLocator = ClassFileLocator.ForClassLoader.of(Object.class.getClassLoader());
        when(typeLocator.typePool(classFileLocator, Object.class.getClassLoader())).thenReturn(typePool);
        when(typePool.describe(Object.class.getName())).thenReturn(new TypePool.Resolution.Simple(typeDescription));
        assertThat(AgentBuilder.DescriptionStrategy.Default.POOL_ONLY.apply(Object.class.getName(),
                Object.class,
                typeLocator,
                Object.class.getClassLoader(),
                classFileLocator), is(typeDescription));
    }

    @Test
    public void testLoadedDescriptionHybrid() throws Exception {
        assertThat(AgentBuilder.DescriptionStrategy.Default.HYBRID.apply(Object.class, typeLocator), is(TypeDescription.OBJECT));
        assertThat(AgentBuilder.DescriptionStrategy.Default.HYBRID.apply(Object.class, typeLocator), instanceOf(TypeDescription.ForLoadedType.class));
    }

    @Test
    public void testLoadedDescriptionPoolOnly() throws Exception {
        when(typeLocator.typePool(ClassFileLocator.ForClassLoader.of(Object.class.getClassLoader()), Object.class.getClassLoader())).thenReturn(typePool);
        when(typePool.describe(Object.class.getName())).thenReturn(new TypePool.Resolution.Simple(typeDescription));
        assertThat(AgentBuilder.DescriptionStrategy.Default.POOL_ONLY.apply(Object.class, typeLocator), is(typeDescription));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.DescriptionStrategy.Default.class).apply();
    }
}
