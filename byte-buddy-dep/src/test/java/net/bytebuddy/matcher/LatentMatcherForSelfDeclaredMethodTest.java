package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.MatcherAssert.assertThat;

public class LatentMatcherForSelfDeclaredMethodTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription typeDescription;

    @Test
    @SuppressWarnings("unchecked")
    public void testDeclared() throws Exception {
        assertThat(LatentMatcher.ForSelfDeclaredMethod.DECLARED.resolve(typeDescription), hasPrototype((ElementMatcher) isDeclaredBy(typeDescription)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNotDeclared() throws Exception {
        assertThat(LatentMatcher.ForSelfDeclaredMethod.NOT_DECLARED.resolve(typeDescription), hasPrototype((ElementMatcher) not(isDeclaredBy(typeDescription))));
    }
}
