package checks.tests.AssertionsInTestsCheck;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.ApplicationContextAssertProvider;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;

import static org.assertj.core.api.Assertions.assertThat;

class SpringBootAppContextRunnerSampleTest {

  ApplicationContextRunner contextRunner = new ApplicationContextRunner();

  @Test
  void test_with_assertable_app_context_different_file() { // FN, contextConsumer has no assertions, but we cannot know
    var contextConsumer = new ForeignNonAssertingContextConsumer();
    contextRunner.run(contextConsumer);
  }

  @Test
  void test_with_assertable_app_context() { // Compliant, contextConsumer has param type AssertableApplicationContext
    contextRunner.run(getAssertingContextConsumer());
  }

  @Test
  void test_with_assertable_app_context2() { // Compliant, contextConsumer has param type AssertableApplicationContext
    var contextConsumer = new LocalAssertingContextConsumer();
    contextRunner.run(contextConsumer);
  }

  @Test
  void test_without_assertable_app_context() { // Noncompliant
    var nonAssertingContextConsumer = new NonAssertingContextConsumer();
    contextRunner.run(nonAssertingContextConsumer);
  }

  @Test
  void test_with_lambda_assertable_app_ctx() {
    contextRunner.run(context -> {
      assertThat(context).getBean("myBean").isNotNull();
    });
  }

  @Test
  void test_without_assertable_app_ctx() { // Noncompliant
    contextRunner.run((ContextConsumer<ApplicationContextAssertProvider>) _ -> {
      // do some unrelated stuff ...
    });
  }

  private LocalAssertingContextConsumer getAssertingContextConsumer(){
    return new LocalAssertingContextConsumer();
  }

  class LocalAssertingContextConsumer implements ContextConsumer<AssertableApplicationContext> {
    @Override
    public void accept(AssertableApplicationContext context) throws Throwable {
      assertThat(context).getBean("myBean").isNotNull();
    }
  }

  class NonAssertingContextConsumer implements ContextConsumer<AssertableApplicationContext> {
    private int count = 0;

    @Override
    public void accept(AssertableApplicationContext context) throws Throwable {
      count++;
    }
  }

}
