package net.bytebuddy.build;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.test.utility.MockitoRule;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static net.bytebuddy.test.utility.FieldByFieldComparison.matchesPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class EntryPointUnvalidatedTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private EntryPoint entryPoint;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private ByteBuddy byteBuddy, otherByteBuddy;

    @Mock
    private ClassFileLocator classFileLocator;

    @Mock
    private ClassFileVersion classFileVersion;

    @Mock
    private MethodNameTransformer methodNameTransformer;

    @Mock
    private DynamicType.Builder<?> builder;

    @Test
    public void testByteBuddy() throws Exception {
        when(entryPoint.byteBuddy(classFileVersion)).thenReturn(byteBuddy);
        when(byteBuddy.with(TypeValidation.DISABLED)).thenReturn(otherByteBuddy);

        assertThat(new EntryPoint.Unvalidated(entryPoint).byteBuddy(classFileVersion), is(otherByteBuddy));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTransform() throws Exception {
        when(entryPoint.transform(typeDescription,
                byteBuddy,
                classFileLocator,
                methodNameTransformer)).thenReturn((DynamicType.Builder) builder);

        assertThat(new EntryPoint.Unvalidated(entryPoint).transform(typeDescription,
                byteBuddy,
                classFileLocator,
                methodNameTransformer), is((DynamicType.Builder) builder));
    }
}
