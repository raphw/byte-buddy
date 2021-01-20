package net.bytebuddy.test.utility;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.MockitoAnnotations;

/**
 * A rule that applies Mockito's annotations to any test. This is preferred over the Mockito runner since it allows
 * to use tests with parameters that require a specific runner. 将 Mockito 的注释应用于任何测试的规则。 这比 Mockito 运行器更可取，因为它允许使用带有需要特定运行器的参数的测试
 */
public class MockitoRule implements TestRule {

    private final Object target;

    public MockitoRule(Object target) {
        this.target = target;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                MockitoAnnotations.initMocks(target); // 想要在测试方法运行之前做一些事情，就在 base.evaluate() 之前做
                base.evaluate(); // 这其实就是运行测试方法
                // xxx 想要在测试方法运行之后做一些事情，就在 base.evaluate() 之后做
            }
        };
    }
}
