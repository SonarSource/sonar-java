/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.resolve;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

public class Result {

  private final Collection<Symbol> symbolsUsed;
  private final Map<Tree, Symbol> symbolsTree;

  private Result(Collection<Symbol> symbolsUsed, Map<Tree, Symbol> symbolsTree) {
    this.symbolsUsed = symbolsUsed;
    this.symbolsTree = Collections.unmodifiableMap(symbolsTree);
  }

  public static Result createFor(String name) {
    return createForJavaFile("src/test/files/sym/" + name);
  }

  public static Result createForJavaFile(String filePath) {
    File file = new File(filePath + ".java");
    CompilationUnitTree compilationUnitTree = JParserTestUtils.parse(file);
    UsageVisitor usageVisitor = new UsageVisitor();
    compilationUnitTree.accept(usageVisitor);
    return new Result(usageVisitor.symbolsUsed, usageVisitor.symbolsTree);
  }

  private static class UsageVisitor extends BaseTreeVisitor {

    private final Collection<Symbol> symbolsUsed = new HashSet<>();
    private final Map<Tree, Symbol> symbolsTree = new HashMap<>();

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      Symbol symbol = tree.symbol();
      symbolsUsed.add(symbol);
      symbolsTree.put(tree, symbol);
      super.visitIdentifier(tree);
    }
  }

  public Symbol symbol(String name) {
    Symbol result = null;
    for (Symbol symbol : symbolsTree.values()) {
      if (name.equals(symbol.name())) {
        if (result != null) {
          throw new IllegalArgumentException("Ambiguous coordinates of symbol");
        }
        result = symbol;
      }
    }
    if (result == null) {
      throw new IllegalArgumentException("Symbol not found");
    }
    return result;
  }

  public Symbol symbol(String name, int line) {
    Symbol result = null;
    for (Map.Entry<Tree, Symbol> entry : symbolsTree.entrySet()) {
      if (name.equals(entry.getValue().name()) && ((JavaTree) entry.getKey()).getLine() == line) {
        if (result != null) {
          throw new IllegalArgumentException("Ambiguous coordinates of symbol");
        }
        result = entry.getValue();
      }
    }
    if (result == null) {
      throw new IllegalArgumentException("Symbol not found");
    }
    return result;
  }

  public Symbol reference(int line, int column) {
    return (Symbol) referenceTree(line, column, true, null);
  }

  public Symbol reference(int line, int column, String name) {
    return (Symbol) referenceTree(line, column, true, name);
  }

  public IdentifierTree referenceTree(int line, int column) {
    return (IdentifierTree) referenceTree(line, column, false, null);
  }

  private Object referenceTree(int line, int column, boolean searchSymbol, @Nullable String name) {
    column -= 1;
    for (Symbol symbol : symbolsUsed) {
      if (name != null && !name.equals(symbol.name())) {
        continue;
      }
      for (IdentifierTree usage : symbol.usages()) {
        SyntaxToken token = usage.identifierToken();
        if (token.line() == line && token.column() == column) {
          if(searchSymbol) {
            if (usage.parent() == null || !usage.parent().is(Tree.Kind.NEW_CLASS) || symbol.isMethodSymbol()) {
              return symbol;
            }
          } else {
            return usage;
          }
        }
      }
    }
    throw new IllegalArgumentException("Reference Tree not found "+line);
  }

}
