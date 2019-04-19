package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EEnumConstant extends ETree implements EnumConstantTree {
  EModifiers modifiers = new EModifiers();
  IdentifierTree simpleName;
  EClassInstanceCreation initializer;

  @Override
  public ModifiersTree modifiers() {
    return modifiers;
  }

  @Override
  public IdentifierTree simpleName() {
    return simpleName;
  }

  @Override
  public NewClassTree initializer() {
    return initializer;
  }

  @Nullable
  @Override
  public SyntaxToken separatorToken() {
    throw new NotImplementedException();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitEnumConstant(this);
  }

  @Override
  public Kind kind() {
    return Kind.ENUM_CONSTANT;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    if (!modifiers.isEmpty()) {
      return modifiers.firstToken();
    }
    return simpleName.firstToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      modifiers,
      // simpleName excluded as in old implementation
      initializer
    );
  }
}
