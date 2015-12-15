/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Deque;
import java.util.LinkedList;

@Rule(
  key = "S2325",
  name = "\"private\" methods that don't access instance data should be \"static\"",
  priority = Priority.MINOR,
  tags = {Tag.PITFALL})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class StaticMethodCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String JAVA_IO_SERIALIZABLE = "java.io.Serializable";
  private static final MethodInvocationMatcherCollection EXCLUDED_SERIALIZABLE_METHODS = MethodInvocationMatcherCollection.create(
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf(JAVA_IO_SERIALIZABLE)).name("readObject").addParameter(TypeCriteria.subtypeOf("java.io.ObjectInputStream")),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf(JAVA_IO_SERIALIZABLE)).name("writeObject").addParameter(TypeCriteria.subtypeOf("java.io.ObjectOutputStream")),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf(JAVA_IO_SERIALIZABLE)).name("readObjectNoData")
  );

  private JavaFileScannerContext context;
  private Deque<Symbol> outerClasses = new LinkedList<>();
  private Deque<Boolean> atLeastOneReference = new LinkedList<>();
  private Deque<Symbol> currentMethod = new LinkedList<>();

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitClass(ClassTree tree) {
    outerClasses.push(tree.symbol());
    super.visitClass(tree);
    outerClasses.pop();
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (isExcluded(tree)) {
      return;
    }
    Symbol.MethodSymbol symbol = tree.symbol();
    if (outerClasses.size() > 1 && !outerClasses.peek().isStatic()) {
      return;
    }
    atLeastOneReference.push(Boolean.FALSE);
    currentMethod.push(symbol);
    scan(tree.block());
    Boolean oneReference = atLeastOneReference.pop();
    currentMethod.pop();
    if (symbol.isPrivate() && !symbol.isStatic() && !oneReference) {
      context.reportIssue(this, tree.simpleName(), "Make \"" + symbol.name() + "\" a \"static\" method.");
    }
  }

  private static boolean isExcluded(MethodTree tree) {
    return tree.is(Tree.Kind.CONSTRUCTOR) || EXCLUDED_SERIALIZABLE_METHODS.anyMatch(tree);
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    super.visitIdentifier(tree);
    Symbol symbol = tree.symbol();
    if (!atLeastOneReference.isEmpty() && !atLeastOneReference.peek() && !currentMethod.peek().equals(symbol)  && referenceInstance(symbol)) {
      atLeastOneReference.pop();
      atLeastOneReference.push(Boolean.TRUE);
    }
  }

  private boolean referenceInstance(Symbol symbol) {
    return symbol.isUnknown() || (!symbol.isStatic() && fromInstance(symbol.owner()));
  }

  private boolean fromInstance(Symbol owner) {
    for (Symbol outerClass : outerClasses) {
      Type ownerType = owner.type();
      if (ownerType != null) {
        if (owner.equals(outerClass) || outerClass.type().isSubtypeOf(ownerType)) {
          return true;
        } else {
          return fromInstance(ownerType.symbol().owner());
        }
      }
    }
    return false;
  }
}
