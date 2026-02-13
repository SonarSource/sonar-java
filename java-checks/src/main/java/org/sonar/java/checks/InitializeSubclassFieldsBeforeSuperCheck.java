package org.sonar.java.checks;

import java.util.List;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;

public final class InitializeSubclassFieldsBeforeSuperCheck extends FlexibleConstructorVisitor {

  @Override
  void validateConstructor(MethodTree constructor, List<StatementTree> body, int constructorCallIndex) {

  }
}
