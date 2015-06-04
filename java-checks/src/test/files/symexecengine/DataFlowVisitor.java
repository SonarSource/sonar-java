import org.fest.assertions.Assertions;
import org.junit.Test;
import org.sonar.java.symexecengine.DataFlowVisitorTest.Check;
import org.sonar.java.symexecengine.DataFlowVisitorTest.ExecutionContext;
import org.sonar.java.symexecengine.DataFlowVisitorTest.TestState;

public abstract class TestClass {

  public void test_unset(Value value) {
  }

  @ExpectedIssues({1})
  public void test_unconditional(Value value) {
    value.setState(1);
  }

  @ExpectedIssues({3})
  // FIXME should be 1, 2, 3
  public void test_conditional(Value value) {
    boolean result = value.setState(1) ? value.setState(2) : value.setState(3);
  }

  @ExpectedIssues({2})
  // FIXME should be 1, 2
  public void test_conditional_and(Value value) {
    boolean result = value.setState(1) && value.setState(2);
  }

  @ExpectedIssues({2})
  // FIXME should be 1, 2
  public void test_conditional_or(Value value) {
    boolean result = value.setState(1) || value.setState(2);
  }

  @ExpectedIssues({1, 2})
  // FIXME should be 2
  public void test_do_while(Value value) {
    value.setState(1);
    do {
      value.setState(2);
    } while (condition);
  }

  @ExpectedIssues({1, 2})
  public void test_for(Value value) {
    value.setState(1);
    for (; condition;) {
      value.setState(2);
    }
  }

  @ExpectedIssues({1, 2})
  public void test_if(Value value) {
    value.setState(1);
    if (condition) {
      value.setState(2);
    }
  }

}

interface Value {
  public boolean setState(int value);
}

@interface ExpectedIssues {
  int[] value();
}
