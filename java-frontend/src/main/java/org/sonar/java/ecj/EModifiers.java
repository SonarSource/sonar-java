package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@MethodsAreNonnullByDefault
class EModifiers extends EList<ModifierTree> implements ModifiersTree {
  @Override
  public List<AnnotationTree> annotations() {
    // TODO suboptimal
    return elements.stream()
      .filter(t -> t.is(Kind.ANNOTATION))
      .map(AnnotationTree.class::cast)
      .collect(Collectors.toList());
  }

  @Override
  public List<ModifierKeywordTree> modifiers() {
    // TODO suboptimal
    return elements.stream()
      .filter(t -> !t.is(Kind.ANNOTATION))
      .map(ModifierKeywordTree.class::cast)
      .collect(Collectors.toList());
  }

  @Override
  public Kind kind() {
    return Kind.MODIFIERS;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    // TODO weird method name
    visitor.visitModifier(this);
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return elements.isEmpty() ? null : elements.get(0).firstToken();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return elements.isEmpty() ? null : elements.get(elements.size() - 1).lastToken();
  }
}

@MethodsAreNonnullByDefault
class EModifierKeyword extends ESyntaxToken implements ModifierKeywordTree {
  EModifierKeyword(SyntaxToken syntaxToken) {
    super(syntaxToken.line(), syntaxToken.column(), syntaxToken.text());
  }

  @Override
  public Modifier modifier() {
    return Modifier.valueOf(text().toUpperCase());
  }

  @Override
  public SyntaxToken keyword() {
    return this;
  }
}

@MethodsAreNonnullByDefault
class EAnnotation extends EExpression implements AnnotationTree {
  SyntaxToken atToken;
  TypeTree annotationType;
  EMethodInvocation.EArguments arguments = new EMethodInvocation.EArguments();

  @Override
  public SyntaxToken atToken() {
    return atToken;
  }

  @Override
  public TypeTree annotationType() {
    return annotationType;
  }

  @Override
  public Arguments arguments() {
    return arguments;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitAnnotation(this);
  }

  @Override
  public Kind kind() {
    return Kind.ANNOTATION;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return atToken();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    if (arguments.closeParenToken != null) {
      return arguments.closeParenToken;
    }
    return annotationType.lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      annotationType(),
      arguments()
    );
  }
}
