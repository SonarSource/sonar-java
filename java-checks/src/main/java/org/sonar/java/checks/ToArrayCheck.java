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

import java.util.ArrayList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.JUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

@Rule(key = "S3020")
public class ToArrayCheck extends AbstractMethodDetection {

  private static final MethodMatchers COLLECTION_TO_ARRAY = MethodMatchers.create().ofSubTypes("java.util.Collection")
    .names("toArray").addWithoutParametersMatcher().build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return COLLECTION_TO_ARRAY;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    Tree parent = mit.parent();
    if (parent.is(Tree.Kind.TYPE_CAST)) {
      checkCast(((TypeCastTree) parent), mit);
    }
  }

  private void checkCast(TypeCastTree castTree, MethodInvocationTree mit) {
    Type type = castTree.symbolType();
    if (type.isArray() && !type.is("java.lang.Object[]")) {
      Type elementType = ((Type.ArrayType) type).elementType();
      ExpressionTree methodSelect = mit.methodSelect();
      // Do not report an issue for type variables and call to toArray from the Collection itself
      if (!JUtils.isTypeVar(elementType) && methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        String typeName = String.format("new %s[0]", elementType.name());
        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(mit)
          .withMessage("Pass \"%s\" as argument to \"toArray\".", typeName)
          .withQuickFix(() -> getQuickFix(castTree, mit, (MemberSelectExpressionTree) methodSelect, typeName))
          .report();
      }
    }
  }

  private static JavaQuickFix getQuickFix(TypeCastTree castTree, MethodInvocationTree mit, MemberSelectExpressionTree methodSelect, String typeName) {
    List<JavaTextEdit> textEdits = new ArrayList<>();
    textEdits.add(JavaTextEdit.insertAfterTree(mit.arguments().firstToken(), typeName));
    if (!JUtils.isRawType(methodSelect.expression().symbolType())) {
      textEdits.add(JavaTextEdit.removeTextSpan(AnalyzerMessage.textSpanBetween(castTree, true, mit, false)));
    }
    return JavaQuickFix.newQuickFix("Pass \"%s\" as argument", typeName)
      .addTextEdits(textEdits)
      .build();
  }
}
