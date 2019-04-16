package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@MethodsAreNonnullByDefault
class ESwitchExpression extends EExpression implements SwitchExpressionTree {
  SyntaxToken switchKeyword;
  ExpressionTree expression;
  SyntaxToken closeParenToken;
  SyntaxToken openBraceToken;
  List<CaseGroupTree> groups = new ArrayList<>();
  SyntaxToken closeBraceToken;

  @Override
  public SyntaxToken switchKeyword() {
    return switchKeyword;
  }

  @Override
  public SyntaxToken openParenToken() {
    throw new UnexpectedAccessException();
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }

  @Override
  public SyntaxToken openBraceToken() {
    return openBraceToken;
  }

  @Override
  public List<CaseGroupTree> cases() {
    return groups;
  }

  @Override
  public SyntaxToken closeBraceToken() {
    return closeBraceToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitSwitchExpression(this);
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return switchKeyword();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return closeBraceToken();
  }

  @Override
  public Kind kind() {
    return Kind.SWITCH_EXPRESSION;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.concat(
      Iterators.singletonIterator(expression()),
      cases().iterator()
    );
  }


  static class ECaseGroup extends ETree implements CaseGroupTree {
    List<CaseLabelTree> labels = new ArrayList<>();
    List<StatementTree> body = new ArrayList<>();

    @Override
    public List<CaseLabelTree> labels() {
      return labels;
    }

    @Override
    public List<StatementTree> body() {
      return body;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitCaseGroup(this);
    }

    @Override
    public Kind kind() {
      return Kind.CASE_GROUP;
    }

    @Nullable
    @Override
    public SyntaxToken firstToken() {
      return labels.get(0).firstToken();
    }

    @Override
    Iterator<? extends Tree> childrenIterator() {
      return Iterators.concat(
        labels().iterator(),
        body().iterator()
      );
    }
  }


  static class ECaseLabel extends ETree implements CaseLabelTree {
    SyntaxToken caseOrDefaultKeyword;
    ExpressionTree expression;
    SyntaxToken colonToken;

    @Override
    public SyntaxToken caseOrDefaultKeyword() {
      return caseOrDefaultKeyword;
    }

    @Override
    public boolean isFallThrough() {
      throw new NotImplementedException();
    }

    @Nullable
    @Override
    public ExpressionTree expression() {
      return expression;
    }

    @Override
    public List<ExpressionTree> expressions() {
      // FIXME
      return Collections.singletonList(expression);
    }

    @Override
    public SyntaxToken colonToken() {
      return colonToken;
    }

    @Override
    public SyntaxToken colonOrArrowToken() {
      throw new NotImplementedException();
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitCaseLabel(this);
    }

    @Override
    public Kind kind() {
      return Kind.CASE_LABEL;
    }

    @Nullable
    @Override
    public SyntaxToken firstToken() {
      return caseOrDefaultKeyword();
    }

    @Nullable
    @Override
    public SyntaxToken lastToken() {
      return colonToken();
    }

    @Override
    Iterator<? extends Tree> childrenIterator() {
      return Iterators.singletonIterator(
        expression()
      );
    }
  }
}
