package net.bytebuddy.test.utility;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.MockitoAnnotations;

/**
 * A rule that applies Mockito's annotations to any test. This is preferred over the Mockito runner since it allows
 * to use tests with parameters that require a specific runner.
 */
public class MockitoRule implements TestRule {

    private final Object target;

    public MockitoRule(Object target) {
        this.target = target;
    }

    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            public void evaluate() throws Throwable {
                MockitoAnnotations.initMocks(target);
                base.evaluate();
            }
        };
    }
}
