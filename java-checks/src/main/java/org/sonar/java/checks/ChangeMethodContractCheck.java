/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2638")
public class ChangeMethodContractCheck extends IssuableSubscriptionVisitor {

  private static final String JAVAX_ANNOTATION_CHECK_FOR_NULL = "javax.annotation.CheckForNull";
  private static final String JAVAX_ANNOTATION_NULLABLE = "javax.annotation.Nullable";
  private static final String JAVAX_ANNOTATION_NONNULL = "javax.annotation.Nonnull";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if(!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();
    Symbol.MethodSymbol overridee = methodSymbol.overriddenSymbol();
    if (overridee != null && overridee.isMethodSymbol()) {
      checkContractChange(methodTree, overridee);
    }
  }

  private void checkContractChange(MethodTree methodTree, Symbol.MethodSymbol overridee) {
    if (MethodTreeUtils.isEqualsMethod(methodTree) && methodTree.parameters().get(0).symbol().metadata().isAnnotatedWith(JAVAX_ANNOTATION_NONNULL)) {
      reportIssue(methodTree.parameters().get(0), "Equals method should accept null parameters and return false.");
      return;
    }
    for (int i = 0; i < methodTree.parameters().size(); i++) {
      VariableTree parameter = methodTree.parameters().get(i);
      Symbol overrideeParamSymbol = ((JavaSymbol.MethodJavaSymbol) overridee).getParameters().scopeSymbols().get(i);
      checkParameter(parameter, overrideeParamSymbol);
    }
    if (nonNullVsNull(overridee, methodTree.symbol())) {
      for (AnnotationTree annotationTree : methodTree.modifiers().annotations()) {
        if(annotationTree.symbolType().is(JAVAX_ANNOTATION_NULLABLE) || annotationTree.symbolType().is(JAVAX_ANNOTATION_CHECK_FOR_NULL)) {
          reportIssue(annotationTree, "Remove this \""+ annotationTree.symbolType().name() +"\" annotation to honor the overridden method's contract.");
        }
      }
    }
  }

  private void checkParameter(VariableTree parameter, Symbol overrideeParamSymbol) {
    Tree reportTree = parameter;
    if (nonNullVsNull(parameter.symbol(), overrideeParamSymbol)) {
      for (AnnotationTree annotationTree : parameter.modifiers().annotations()) {
        if(annotationTree.symbolType().is(JAVAX_ANNOTATION_NONNULL)) {
          reportTree = annotationTree;
        }
      }
      reportIssue(reportTree, "Remove this \"Nonnull\" annotation to honor the overridden method's contract.");
    }
  }

  private static boolean nonNullVsNull(Symbol sym1, Symbol sym2) {
    return sym1.metadata().isAnnotatedWith(JAVAX_ANNOTATION_NONNULL) &&
      (sym2.metadata().isAnnotatedWith(JAVAX_ANNOTATION_NULLABLE)
      || sym2.metadata().isAnnotatedWith(JAVAX_ANNOTATION_CHECK_FOR_NULL));

  }

}
