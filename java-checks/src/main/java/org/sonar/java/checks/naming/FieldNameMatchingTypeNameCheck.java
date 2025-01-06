/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.naming;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1700")
public class FieldNameMatchingTypeNameCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;
  private String currentClassName;
  private final Set<Tree> fields = new HashSet<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    currentClassName = "";
    fields.clear();
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitClass(ClassTree tree) {
    IdentifierTree simpleName = tree.simpleName();
    if (simpleName != null) {
      Symbol.TypeSymbol classSymbol = tree.symbol();
      Collection<Symbol> members = classSymbol.memberSymbols();
      for (Symbol sym : members) {
        if (sym.isVariableSymbol() && !staticFieldSameType(classSymbol, sym)) {
          //Exclude static fields of the same type.
          fields.add(((Symbol.VariableSymbol) sym).declaration());
        }
      }
      currentClassName = simpleName.name();
    }
    super.visitClass(tree);
    currentClassName = "";
    fields.clear();
  }

  private static boolean staticFieldSameType(Symbol classSymbol, Symbol sym) {
    return sym.type().equals(classSymbol.type()) && sym.isStatic();
  }

  @Override
  public void visitVariable(VariableTree tree) {
    String name = tree.simpleName().name();
    if (fields.contains(tree) && currentClassName.equalsIgnoreCase(name)) {
      context.reportIssue(this, tree.simpleName(), "Rename field \"" + name + "\"");
    }
    super.visitVariable(tree);
  }
}
