/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.JavaTree.UnionTypeTreeImpl;
import org.sonar.java.model.LineUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S2147")
public class CombineCatchCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TRY_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ArrayDeque<CatchTree> catches = new ArrayDeque<>();
    for (CatchTree catchTree : ((TryStatementTree) tree).catches()) {
      for (CatchTree catchTreeToBeCompared : catches) {
        if (SyntacticEquivalence.areSemanticallyEquivalent(catchTree.block().body(), catchTreeToBeCompared.block().body())) {
          reportIssueWithQuickFix(catchTree, catchTreeToBeCompared);
          break;
        }
      }
      catches.push(catchTree);
    }
  }

  private void reportIssueWithQuickFix(CatchTree catchTree, CatchTree catchTreeToBeCompared) {
    String quickFixMessage = "Combine this catch with the one at line " + LineUtils.startLine(catchTreeToBeCompared.catchKeyword());
    String issueMessage = quickFixMessage + ", which has the same body." + context.getJavaVersion().java7CompatibilityMessage();
    List<JavaFileScannerContext.Location> flow = Collections.singletonList(new JavaFileScannerContext.Location("Combine with this catch", catchTreeToBeCompared));
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(catchTree.parameter())
      .withMessage(issueMessage)
      .withSecondaries(flow)
      .withQuickFix( () -> computeQuickFix(catchTree, catchTreeToBeCompared, quickFixMessage) )
      .report();
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava7Compatible();
  }

  private JavaQuickFix computeQuickFix(CatchTree catchTree, CatchTree catchTreeToBeCompared, String qfMessage) {
    List<TypeTree> upperCatchTypes = getExceptionTypesCaught(catchTreeToBeCompared);
    List<TypeTree> lowerCatchTypes = getExceptionTypesCaught(catchTree);

    List<TypeTree> mergedTypes = mergeCatchTypes(upperCatchTypes, lowerCatchTypes);

    var builder = JavaQuickFix.newQuickFix(qfMessage);
    builder.addTextEdit(JavaTextEdit.removeTree(catchTree));
    String replacement = computeReplacementString(mergedTypes, catchTreeToBeCompared);
    builder.addTextEdit(
      JavaTextEdit.replaceBetweenTree(
        catchTreeToBeCompared.openParenToken(), false,
        catchTreeToBeCompared.closeParenToken(), false,
        replacement)
      );
    return builder.build();
  }

  private String computeReplacementString(Collection<TypeTree> types, CatchTree toReplaceCatch) {
    StringBuilder sb = new StringBuilder();
    types.forEach(type -> sb.append(formatType(type) + " | "));
    sb.delete(sb.lastIndexOf("| "), sb.length());
    sb.append(toReplaceCatch.parameter().simpleName().name());
    return sb.toString();
  }

  private static List<TypeTree> mergeCatchTypes(List<TypeTree> upperCatchTypes, List<TypeTree> lowerCatchTypes) {
    List<TypeTree> result = new ArrayList<>();
    for (TypeTree upperType : upperCatchTypes) {
      if (isNotMaskedBySuperType(upperType, lowerCatchTypes)) {
        result.add(upperType);
      }
    }
    result.addAll(lowerCatchTypes);
    return result;
  }

  private static boolean isNotMaskedBySuperType(TypeTree type, List<TypeTree> types) {
    for (TypeTree other : types) {
      if (type.symbolType().isSubtypeOf(other.symbolType())) {
        return false;
      }
    }
    return true;
  }

  private String formatType(TypeTree type) {
    if(type instanceof MemberSelectExpressionTreeImpl) {
      MemberSelectExpressionTreeImpl mtype = (MemberSelectExpressionTreeImpl) type;
      return QuickFixHelper.contentForTree(mtype, context);
    }
    return type.toString();
  }

  private static List<TypeTree> getExceptionTypesCaught(CatchTree catchTree){
    TypeTree catchType = catchTree.parameter().type();
    if(catchType instanceof UnionTypeTreeImpl) {
      UnionTypeTreeImpl unionTypes = (UnionTypeTreeImpl) catchType;
      unionTypes.symbolType();
      return unionTypes.typeAlternatives();
    }else {
      return List.of(catchType);
    }
  }

}
