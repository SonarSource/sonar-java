import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.fest.assertions.Assertions;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.symexecengine.DataFlowVisitor;
import org.sonar.java.symexecengine.ExecutionState;
import org.sonar.java.symexecengine.State;
import org.sonar.java.symexecengine.SymbolicExecutionCheck;
import org.sonar.java.symexecengine.DataFlowVisitorTest.Check;
import org.sonar.java.symexecengine.DataFlowVisitorTest.ExecutionContext;
import org.sonar.java.symexecengine.DataFlowVisitorTest.TestState;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
