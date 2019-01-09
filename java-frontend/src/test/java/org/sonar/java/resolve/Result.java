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
package org.sonar.java.resolve;

import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class Result {

  private static final ActionParser parser = JavaParser.createParser();
  private final SemanticModel semanticModel;
  private final Collection<Symbol> symbolsUsed;

  private Result(SemanticModel semanticModel, Collection<Symbol> symbolsUsed) {
    this.semanticModel = semanticModel;
    this.symbolsUsed = symbolsUsed;
  }

  public static Result createFor(String name) {
    return createForJavaFile("src/test/files/sym/" + name);
  }

  public static Result createForJavaFile(String filePath) {
    File file = new File(filePath + ".java");
    CompilationUnitTree compilationUnitTree = (CompilationUnitTree) parser.parse(file);
    SemanticModel semanticModel = SemanticModel.createFor(compilationUnitTree, new SquidClassLoader(Lists.newArrayList(new File("target/test-classes"), new File("target/classes"))));
    UsageVisitor usageVisitor = new UsageVisitor();
    compilationUnitTree.accept(usageVisitor);
    return new Result(semanticModel, usageVisitor.symbolsUsed);
  }

  private static class UsageVisitor extends BaseTreeVisitor {

    public Collection<Symbol> symbolsUsed = new HashSet<>();

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      symbolsUsed.add(tree.symbol());
      super.visitIdentifier(tree);
    }
  }

  public JavaSymbol symbol(String name) {
    Symbol result = null;
    for (Symbol symbol : semanticModel.getSymbolsTree().values()) {
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
    return (JavaSymbol) result;
  }

  public JavaSymbol symbol(String name, int line) {
    Symbol result = null;
    for (Map.Entry<Tree, Symbol> entry : semanticModel.getSymbolsTree().entrySet()) {
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
    return (JavaSymbol) result;
  }

  public JavaSymbol reference(int line, int column) {
    return (JavaSymbol) referenceTree(line, column, true, null);
  }

  public JavaSymbol reference(int line, int column, String name) {
    return (JavaSymbol) referenceTree(line, column, true, name);
  }

  public IdentifierTree referenceTree(int line, int column) {
    return (IdentifierTree) referenceTree(line, column, false, null);
  }

  private Object referenceTree(int line, int column, boolean searchSymbol, @Nullable String name) {
    // In SSLR column starts at 0, but here we want consistency with IDE, so we start from 1:
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
