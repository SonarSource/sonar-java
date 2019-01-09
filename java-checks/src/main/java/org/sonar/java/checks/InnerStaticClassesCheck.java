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

import java.util.Deque;
import java.util.LinkedList;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2694")
public class InnerStaticClassesCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;
  private Deque<Symbol> outerClasses = new LinkedList<>();
  private Deque<Boolean> atLeastOneReference = new LinkedList<>();

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    Symbol.TypeSymbol symbol = tree.symbol();
    if (!tree.is(Tree.Kind.CLASS)) {
      return;
    }
    outerClasses.push(symbol);
    atLeastOneReference.push(Boolean.FALSE);
    scan(tree.members());
    Boolean oneReference = atLeastOneReference.pop();
    outerClasses.pop();
    if (!symbol.isStatic() && !oneReference && couldBeDeclaredStatic(symbol)) {
      Tree reportTree = tree.simpleName();
      if(reportTree == null) {
        // Ignore issues on anonymous classes
        return;
      }
      String message = "Make this a \"static\" inner class.";
      if(symbol.owner().isMethodSymbol()) {
        message = "Make this local class a \"static\" inner class.";
      }
      context.reportIssue(this, reportTree, message);
    }
  }

  private boolean couldBeDeclaredStatic(Symbol.TypeSymbol symbol) {
    Type superClass = symbol.superClass();
    if (superClass != null) {
      Symbol superClassSymbol = superClass.symbol();
      if (!superClassSymbol.owner().isPackageSymbol() && !superClassSymbol.isStatic()) {
        return false;
      }
    }
    if (outerClasses.size() == 1) {
      return true;
    }
    for (Symbol outerClass : outerClasses) {
      if (outerClass.isStatic()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    super.visitIdentifier(tree);
    checkSymbol(tree.symbol());
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    super.visitNewClass(tree);
    checkSymbol(tree.symbolType().symbol());
  }

  private void checkSymbol(Symbol symbol) {
    if (!atLeastOneReference.isEmpty()) {
      int level = referenceInstance(symbol);
      if (level >= 0) {
        for (int i = 0; i < level; i++) {
          atLeastOneReference.pop();
        }
        while (atLeastOneReference.size() != outerClasses.size()) {
          atLeastOneReference.push(Boolean.TRUE);
        }
      }
    }
  }

  private int referenceInstance(Symbol symbol) {
    Symbol owner = symbol.owner();
    if(owner != null && owner.isMethodSymbol()) {
      //local variable, use owner of the method
      owner = owner.owner();
    }
    int result = -1;
    if (owner != null && !outerClasses.peek().equals(owner)) {
      if (symbol.isUnknown()) {
        result = atLeastOneReference.size() - 1;
      } else if (!symbol.isStatic()) {
        result = fromInstance(symbol, owner);
      }
    }
    return result;
  }

  private int fromInstance(Symbol symbol, Symbol owner) {
    int i = -1;
    Type ownerType = owner.type();
    for (Symbol outerClass : outerClasses) {
      i++;
      if (symbol.equals(outerClass) || (ownerType != null && owner.isTypeSymbol() && outerClass.type().isSubtypeOf(ownerType))) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public void visitVariable(VariableTree tree) {
    Symbol symbol = tree.symbol();
    if (symbol != null && !symbol.isStatic()) {
      scan(tree.modifiers());
      scan(tree.type());
      // skip the simple name
      scan(tree.initializer());
    }
  }

  @Override
  public void visitMethod(MethodTree tree) {
    scan(tree.modifiers());
    scan(tree.typeParameters());
    scan(tree.returnType());
    // skip the simple name
    scan(tree.parameters());
    scan(tree.defaultValue());
    scan(tree.throwsClauses());
    scan(tree.block());
  }
}
