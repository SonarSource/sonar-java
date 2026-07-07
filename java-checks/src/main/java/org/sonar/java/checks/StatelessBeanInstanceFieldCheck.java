/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S8983")
public class StatelessBeanInstanceFieldCheck extends IssuableSubscriptionVisitor {

  private static final List<String> STATELESS_ANNOTATIONS = List.of(
    "javax.ejb.Stateless",
    "jakarta.ejb.Stateless"
  );

  private static final List<String> SAFE_FIELD_ANNOTATIONS = List.of(
    "javax.ejb.EJB",
    "jakarta.ejb.EJB",
    "javax.inject.Inject",
    "jakarta.inject.Inject",
    "javax.persistence.PersistenceContext",
    "jakarta.persistence.PersistenceContext",
    "javax.persistence.PersistenceUnit",
    "jakarta.persistence.PersistenceUnit",
    "javax.annotation.Resource",
    "jakarta.annotation.Resource",
    "javax.xml.ws.WebServiceRef",
    "jakarta.xml.ws.WebServiceRef"
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    var variable = (VariableTree) tree;
    if (isInstanceFieldOfStatelessBean(variable) && !isSafe(variable)) {
      reportIssue(variable.simpleName(),
        "Remove this mutable instance field or replace it with a local variable, a \"static final\" constant, or an injected resource.");
    }
  }

  private static boolean isInstanceFieldOfStatelessBean(VariableTree variable) {
    if (!variable.parent().is(Tree.Kind.CLASS)) {
      return false;
    }
    Symbol owner = variable.symbol().owner();
    if (!owner.isTypeSymbol()) {
      return false;
    }
    SymbolMetadata ownerMetadata = owner.metadata();
    return STATELESS_ANNOTATIONS.stream().anyMatch(ownerMetadata::isAnnotatedWith);
  }

  private static boolean isSafe(VariableTree variable) {
    if (ModifiersUtils.hasAnyOf(variable.modifiers(), Modifier.STATIC, Modifier.FINAL)) {
      return true;
    }
    SymbolMetadata varMetadata = variable.symbol().metadata();
    return SAFE_FIELD_ANNOTATIONS.stream().anyMatch(varMetadata::isAnnotatedWith);
  }
}
