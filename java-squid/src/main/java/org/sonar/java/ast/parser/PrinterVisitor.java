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
package org.sonar.java.ast.parser;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.model.JavaTree;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrinterVisitor extends BaseTreeVisitor {

  private static final int INDENT_SPACES = 2;
  private static final Logger LOG = LoggerFactory.getLogger(PrinterVisitor.class);

  private final StringBuilder sb;
  private final SemanticModel semanticModel;
  private final Map<IdentifierTree, JavaSymbol> idents = new HashMap<>();
  private int indentLevel;

  public PrinterVisitor(@Nullable SemanticModel semanticModel) {
    sb = new StringBuilder();
    indentLevel = 0;
    this.semanticModel = semanticModel;
  }

  public static String print(Tree tree) {
    return print(tree, null);
  }

  public static String print(Tree tree, @Nullable SemanticModel semanticModel) {
    PrinterVisitor pv = new PrinterVisitor(semanticModel);
    pv.scan(tree);
    return pv.sb.toString();
  }

  public static String print(List<? extends Tree> trees) {
    StringBuilder result = new StringBuilder();
    for (Tree tree : trees) {
      result.append(print(tree));
    }
    return result.toString();
  }

  private StringBuilder indent() {
    return sb.append(StringUtils.leftPad("", INDENT_SPACES * indentLevel));
  }

  @Override
  protected void scan(List<? extends Tree> trees) {
    if (!trees.isEmpty()) {
      sb.deleteCharAt(sb.length() - 1);
      sb.append(" : [\n");
      super.scan(trees);
      indent().append("]\n");
    }
  }

  @Override
  protected void scan(@Nullable Tree tree) {
    if (tree != null) {
      JavaSymbol sym = null;
      try {
        Method getSymbol = null;
        for (Method method : tree.getClass().getMethods()) {
          if ("getSymbol".equals(method.getName())) {
            getSymbol = tree.getClass().getMethod("getSymbol");
          }
        }
        if (getSymbol != null) {
          sym = (JavaSymbol) getSymbol.invoke(tree);
        }
      } catch (Exception e) {
        LOG.error("An error occured while retrieving symbol ", e);
      }

      Tree.Kind kind = tree.kind();
      String nodeName = ((JavaTree) tree).getClass().getSimpleName();
      if (kind != null) {
        nodeName = kind.getAssociatedInterface().getSimpleName();
      }
      indent().append(nodeName);
      int line = ((JavaTree) tree).getLine();
      if(line >= 0) {
        sb.append(" ").append(line);
      }
      if (idents.get(tree) != null) {
        Preconditions.checkState(sym == null);
        sym = idents.get(tree);
      }
      Type type = null;
      if (tree instanceof ExpressionTree) {
        type = ((ExpressionTree) tree).symbolType();

      } else if (tree instanceof TypeTree) {
        type = ((TypeTree) tree).symbolType();
      }
      if(type != null) {
        sb.append(" ").append(type.fullyQualifiedName());
      }

      if (sym != null && semanticModel != null) {
        //No forward reference possible... Need another visitor to build this info ?
        for (IdentifierTree identifierTree : sym.usages()) {
          idents.put(identifierTree, sym);
          sb.append(" ").append(sym.getName());
        }
        int refLine = ((JavaTree) sym.declaration()).getLine();
        if (refLine != line) {
          sb.append(" ref#").append(refLine);
        }
      }
      sb.append("\n");
    }
    indentLevel++;
    super.scan(tree);
    indentLevel--;
  }

  public static String printFile(String file, String bytecodePath) {
    final ActionParser p = JavaParser.createParser(Charsets.UTF_8);
    CompilationUnitTree cut = (CompilationUnitTree) p.parse(new File(file));
    List<File> bytecodeFiles = Lists.newArrayList();
    if (!bytecodePath.isEmpty()) {
      bytecodeFiles.add(new File(bytecodePath));
    }
    SemanticModel semanticModel = SemanticModel.createFor(cut, bytecodeFiles);
    return PrinterVisitor.print(cut, semanticModel);
  }
}
