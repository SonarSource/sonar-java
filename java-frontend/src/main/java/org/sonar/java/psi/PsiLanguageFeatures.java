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
package org.sonar.java.psi;

import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiCaseLabelElementList;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDeconstructionPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiImplicitClass;
import com.intellij.psi.PsiImportModuleStatement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPattern;
import com.intellij.psi.PsiSwitchExpression;
import com.intellij.psi.PsiSwitchLabeledRuleStatement;
import com.intellij.psi.PsiTypeTestPattern;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.ArrayList;
import java.util.List;

/** Collects syntax from PSI nodes, without interpreting comments or literal contents as code. */
public final class PsiLanguageFeatures {
  public enum Feature {
    LOCAL_VAR, LAMBDA_VAR, SWITCH_EXPRESSION, SWITCH_RULE, TEXT_BLOCK, RECORD,
    TYPE_PATTERN, RECORD_PATTERN, SWITCH_PATTERN, SEALED, UNNAMED_VARIABLE,
    MODULE_IMPORT, FLEXIBLE_CONSTRUCTOR, COMPACT_SOURCE
  }

  public record Occurrence(Feature feature, PsiElement element) {
  }

  private PsiLanguageFeatures() {
  }

  public static List<Occurrence> collect(PsiJavaFile file) {
    var result = new ArrayList<Occurrence>();
    file.accept(new JavaRecursiveElementWalkingVisitor() {
      private void add(Feature feature, PsiElement element) {
        result.add(new Occurrence(feature, element));
      }

      @Override
      public void visitClass(PsiClass declaration) {
        if (declaration.isRecord()) {
          add(Feature.RECORD, declaration);
        }
        if (declaration instanceof PsiImplicitClass) {
          add(Feature.COMPACT_SOURCE, declaration);
        }
        if (declaration.getModifierList() != null && declaration.getModifierList().hasExplicitModifier(PsiModifier.SEALED)) {
          add(Feature.SEALED, declaration);
        }
        super.visitClass(declaration);
      }

      @Override
      public void visitVariable(PsiVariable variable) {
        var type = variable.getTypeElement();
        if (type != null && type.getText().equals("var")) {
          add(variable instanceof PsiParameter ? Feature.LAMBDA_VAR : Feature.LOCAL_VAR, type);
        }
        if (variable.getNameIdentifier() != null && "_".equals(variable.getName())) {
          add(Feature.UNNAMED_VARIABLE, variable.getNameIdentifier());
        }
        super.visitVariable(variable);
      }

      @Override
      public void visitSwitchExpression(PsiSwitchExpression expression) {
        add(Feature.SWITCH_EXPRESSION, expression);
        super.visitSwitchExpression(expression);
      }

      @Override
      public void visitSwitchLabeledRuleStatement(PsiSwitchLabeledRuleStatement statement) {
        add(Feature.SWITCH_RULE, statement);
        super.visitSwitchLabeledRuleStatement(statement);
      }

      @Override
      public void visitLiteralExpression(PsiLiteralExpression expression) {
        if (expression.isTextBlock()) {
          add(Feature.TEXT_BLOCK, expression);
        }
        super.visitLiteralExpression(expression);
      }

      @Override
      public void visitTypeTestPattern(PsiTypeTestPattern pattern) {
        add(Feature.TYPE_PATTERN, pattern);
        super.visitTypeTestPattern(pattern);
      }

      @Override
      public void visitDeconstructionPattern(PsiDeconstructionPattern pattern) {
        add(Feature.RECORD_PATTERN, pattern);
        super.visitDeconstructionPattern(pattern);
      }

      @Override
      public void visitPattern(PsiPattern pattern) {
        if (PsiTreeUtil.getParentOfType(pattern, PsiCaseLabelElementList.class, true, PsiExpression.class) != null) {
          add(Feature.SWITCH_PATTERN, pattern);
        }
        super.visitPattern(pattern);
      }

      @Override
      public void visitImportModuleStatement(PsiImportModuleStatement statement) {
        add(Feature.MODULE_IMPORT, statement);
        super.visitImportModuleStatement(statement);
      }

      @Override
      public void visitMethod(PsiMethod method) {
        var body = method.getBody();
        if (method.isConstructor() && body != null) {
          var statements = body.getStatements();
          for (int index = 1; index < statements.length; index++) {
            if (statements[index] instanceof PsiExpressionStatement statement
              && statement.getExpression() instanceof PsiMethodCallExpression call
              && ("super".equals(call.getMethodExpression().getReferenceName()) || "this".equals(call.getMethodExpression().getReferenceName()))) {
              add(Feature.FLEXIBLE_CONSTRUCTOR, call);
            }
          }
        }
        super.visitMethod(method);
      }
    });
    return List.copyOf(result);
  }
}
