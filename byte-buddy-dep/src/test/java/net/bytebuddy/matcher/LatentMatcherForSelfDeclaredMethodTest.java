package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LatentMatcherForSelfDeclaredMethodTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

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
