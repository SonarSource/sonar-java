/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

@Rule(
  key = "S109",
  name = "Magic numbers should not be used",
  tags = {"brain-overload"},
  priority = Priority.MINOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_CHANGEABILITY)
@SqaleConstantRemediation("5min")
public class MagicNumberCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
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
        context.addIssue(tree, this, "Assign this magic number " + tree.value() + " to a well-named constant, and use the constant instead.");
      }
    }
  }

  private static boolean isNumberLiteral(LiteralTree tree) {
    return tree.is(Tree.Kind.DOUBLE_LITERAL, Tree.Kind.FLOAT_LITERAL, Tree.Kind.LONG_LITERAL, Tree.Kind.INT_LITERAL);
  }

  private static boolean isExcluded(BigDecimal bigDecimal) {
    return bigDecimal.compareTo(BigDecimal.ONE) == 0
      || bigDecimal.compareTo(BigDecimal.ZERO) == 0
      || bigDecimal.compareTo(BigDecimal.ONE.negate()) == 0;
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    // Ignore literals within annotation
  }

  @Override
  public void visitVariable(VariableTree tree) {
    // skip static final variables
    ModifiersTree modifiers = tree.modifiers();
    if (!(ModifiersUtils.hasModifier(modifiers, Modifier.STATIC) && ModifiersUtils.hasModifier(modifiers, Modifier.FINAL))) {
      super.visitVariable(tree);
    }
  }
}
