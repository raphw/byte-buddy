package net.bytebuddy.matcher;

import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

public abstract class AbstractElementMatcherTest<T extends ElementMatcher<?>> {

    private final Class<? extends T> type;

    private final String startsWith;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    protected AbstractElementMatcherTest(Class<? extends T> type, String startsWith) {
        this.type = type;
        this.startsWith = startsWith;
    }

    @Test
    public void testObjectProperties() throws Exception {
        modify(ObjectPropertyAssertion.of(type)).specificToString(makeRegex(startsWith)).apply();
    }

    protected String makeRegex(String startsWith) {
        return "^" + startsWith + "\\(.*\\)$";
    }

    protected <S> ObjectPropertyAssertion<S> modify(ObjectPropertyAssertion<S> propertyAssertion) {
        return propertyAssertion;
    }
}
