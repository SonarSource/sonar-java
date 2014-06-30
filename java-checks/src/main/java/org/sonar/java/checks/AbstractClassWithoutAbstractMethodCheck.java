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

import org.sonar.api.rule.RuleKey;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collection;

@Rule(
    key = AbstractClassWithoutAbstractMethodCheck.RULE_KEY,
    priority = Priority.MAJOR,
    tags = {"convention"})
public class AbstractClassWithoutAbstractMethodCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1694";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    if (tree.is(Tree.Kind.CLASS)) {
      Symbol.TypeSymbol typeSymbol = ((ClassTreeImpl) tree).getSymbol();
      if (typeSymbol != null && typeSymbol.isAbstract()) {
        Collection<Symbol> symbols = typeSymbol.members().scopeSymbols();
        int abstractMethod = countAbstractMethods(symbols);
        if (symbols.size() == 1 || abstractMethod == symbols.size() - 1) {
          //emtpy abstract class or only abstract method
          context.addIssue(tree, ruleKey, "Convert this \"" + typeSymbol + "\" class to an interface");
        }
        if (symbols.size()>1 && abstractMethod == 0) {
          //Not empty abstract class with no abstract method
          context.addIssue(tree, ruleKey, "Convert this \"" + typeSymbol + "\" class to a concrete class with a private constructor");
        }
      }
    }
    super.visitClass(tree);
  }

  private int countAbstractMethods(Collection<Symbol> symbols) {
    int abstractMethod = 0;
    for (Symbol sym : symbols) {
      //skip "this"
      if (!"this".equals(sym.getName()) && isAbstractMethod(sym)) {
        abstractMethod++;
      }
    }
    return abstractMethod;
  }

  private boolean isAbstractMethod(Symbol sym) {
    return sym.isKind(Symbol.MTH) && sym.isAbstract();
  }
}
