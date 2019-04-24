package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@MethodsAreNonnullByDefault
class ELambdaExpression extends EExpression implements LambdaExpressionTree {
  SyntaxToken openParenToken;
  List<VariableTree> parameters = new ArrayList<>();
  SyntaxToken closeParenToken;
  SyntaxToken arrowToken;
  Tree body;

  @Nullable
  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public List<VariableTree> parameters() {
    return parameters;
  }

  @Nullable
  @Override
  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }

  @Override
  public SyntaxToken arrowToken() {
    return arrowToken;
  }

  @Override
  public Tree body() {
    return body;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitLambdaExpression(this);
  }

  @Override
  public Kind kind() {
    return Kind.LAMBDA_EXPRESSION;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return openParenToken != null ? openParenToken : parameters.get(0).firstToken();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return body.lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.concat(
      Iterators.singletonIterator(openParenToken()),
      parameters().iterator(),
      Iterators.forArray(
        closeParenToken(),
        arrowToken(),
        body()
      )
    );
  }
}
