/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.METHOD;

@Rule(key = "S4682")
public final class PrimitivesMarkedNullableCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    TypeTree returnType = methodTree.returnType();
    if (returnType.symbolType().isPrimitive()) {
      SymbolMetadata.NullabilityData nullabilityData = methodTree.symbol().metadata().nullabilityData();
      if (nullabilityData.isNullable(METHOD, true, false)) {
        SymbolMetadata.AnnotationInstance annotation = nullabilityData.annotation();
        Tree annotationTree = nullabilityData.declaration();
        // Both "annotation" and "declaration" should never be null, as we only target directly annotated methods. We keep the check for defensive programming.
        if (annotation != null && annotationTree != null) {
          String annotationName = annotation.symbol().name();
          QuickFixHelper.newIssue(context)
            .forRule(this)
            .onTree(returnType)
            .withMessage("\"@%s\" annotation should not be used on primitive types", annotationName)
            .withSecondaries(new JavaFileScannerContext.Location("Child annotation", annotationTree))
            .withQuickFix(() ->
              JavaQuickFix.newQuickFix("Remove \"@%s\"", annotationName)
                .addTextEdit(JavaTextEdit.removeTextSpan(textSpanBetween(annotationTree, true,
                  QuickFixHelper.nextToken(annotationTree), false)))
                .build())
            .report();
        }
      }
    }
  }

}
