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

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Rule(key = "S109")
public class MagicNumberCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String DEFAULT_AUTHORIZED_NUMBERS = "-1,0,1";

  @RuleProperty(
    key = "Authorized numbers",
    description = "Comma separated list of authorized numbers. Example: -1,0,1,2",
    defaultValue = "" + DEFAULT_AUTHORIZED_NUMBERS)
  public String authorizedNumbers = DEFAULT_AUTHORIZED_NUMBERS;
  private List<BigDecimal> authorizedNumbersList = null;
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    this.authorizedNumbersList = new ArrayList<>();
    for (String s : authorizedNumbers.split(",")) {
      authorizedNumbersList.add(new BigDecimal(s.trim()));
    }
    scan(context.getTree());
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    scan(tree.initializer().classBody());
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    if (isNumberLiteral(tree)) {
      DecimalFormat decimalFormat = new DecimalFormat();
      decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));
      decimalFormat.setParseBigDecimal(true);
      BigDecimal checked = null;
      try {
        checked = (BigDecimal) decimalFormat.parse(tree.value());
      } catch (ParseException e) {
        // noop case not encountered
      }
      if (checked != null && !isExcluded(checked)) {
        context.reportIssue(this, tree, "Assign this magic number " + tree.value() + " to a well-named constant, and use the constant instead.");
      }
    }
  }

  private static boolean isNumberLiteral(LiteralTree tree) {
    return tree.is(Tree.Kind.DOUBLE_LITERAL, Tree.Kind.FLOAT_LITERAL, Tree.Kind.LONG_LITERAL, Tree.Kind.INT_LITERAL);
  }

  private boolean isExcluded(BigDecimal bigDecimal) {
    for (BigDecimal bd : this.authorizedNumbersList) {
      if (bigDecimal.compareTo(bd) == 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    // Ignore literals within annotation
  }

  @Override
  public void visitVariable(VariableTree tree) {
    ExpressionTree initializer = tree.initializer();
    boolean arrayNotInitialized = initializer != null && initializer.is(Kind.NEW_ARRAY) && ((NewArrayTree) initializer).initializers().isEmpty();
    boolean isFinalOrNoSemantic = context.getSemanticModel() == null || tree.symbol().isFinal();
    if (arrayNotInitialized || !isFinalOrNoSemantic) {
      super.visitVariable(tree);
    }
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (!MethodTreeUtils.isHashCodeMethod(tree)) {
      super.visitMethod(tree);
    }
  }
}
