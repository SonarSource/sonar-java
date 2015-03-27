/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Collection;

@Rule(
  key = "S1694",
  name = "An abstract class should have both abstract and concrete methods",
  tags = {"convention"},
  priority = Priority.MINOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class AbstractClassWithoutAbstractMethodCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    if (tree.is(Tree.Kind.CLASS)) {
      Symbol.TypeSymbol typeSymbol = tree.symbol();
      if (typeSymbol != null && typeSymbol.isAbstract()) {
        Collection<Symbol> symbols = typeSymbol.memberSymbols();
        int abstractMethod = countAbstractMethods(symbols);
        if (isExtendingObject(tree) && abstractMethod == symbols.size() - 2) {
          //emtpy abstract class or only abstract method
          context.addIssue(tree, this, "Convert this \"" + typeSymbol + "\" class to an interface");
        }
        if (symbols.size() > 2 && abstractMethod == 0 && !isPartialImplementation(tree)) {
          //Not empty abstract class with no abstract method
          context.addIssue(tree, this, "Convert this \"" + typeSymbol + "\" class to a concrete class with a private constructor");
        }
      }
    }
    super.visitClass(tree);
  }

  private boolean isExtendingObject(ClassTree tree) {
    TypeTree superClass = tree.superClass();
    return superClass == null || superClass.symbolType().is("java.lang.Object");
  }

  private boolean isPartialImplementation(ClassTree tree) {
    return tree.superClass() != null || !tree.superInterfaces().isEmpty();
  }

  private int countAbstractMethods(Collection<? extends Symbol> symbols) {
    int abstractMethod = 0;
    for (Symbol sym : symbols) {
      if (isAbstractMethod(sym)) {
        abstractMethod++;
      }
    }
    return abstractMethod;
  }

  private boolean isAbstractMethod(Symbol sym) {
    return sym.isMethodSymbol() && sym.isAbstract();
  }
}
