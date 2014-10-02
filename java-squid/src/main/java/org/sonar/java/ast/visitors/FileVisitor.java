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
package org.sonar.java.ast.visitors;

import com.google.common.base.Preconditions;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import org.sonar.java.ast.parser.AstNodeHacks;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.model.JavaTreeMaker;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.io.File;

// FIXME TEST THIS CLASS!!!
public class FileVisitor extends SquidAstVisitor<LexerlessGrammar> {
  public static final String UNRESOLVED_PACKAGE = "!error!";

  @Override
  public void visitFile(AstNode astNode) {
    String packageKey = getPackageKey(astNode);
    SourceFile sourceFile = createSourceFile(packageKey, getContext().getFile());
    getContext().addSourceCode(sourceFile);
  }

  @Override
  public void leaveFile(AstNode astNode) {
    Preconditions.checkState(getContext().peekSourceCode().isType(SourceFile.class));
    getContext().popSourceCode();
  }

  private SourceFile createSourceFile(String parentPackage, File file) {
    StringBuilder key = new StringBuilder();
    if (!"".equals(parentPackage)) {
      key.append(parentPackage);
      key.append("/");
    }
    key.append(file.getName());
    return new SourceFile(key.toString(), file.getPath());
  }

  // TODO Reduce visibility
  public static String getPackageKey(AstNode astNode) {
    if (isEmptyFileOrParseError(astNode)) {
      // Cannot resolve package for empty file and parse error.
      return UNRESOLVED_PACKAGE;
    } else if (astNode.hasDirectChildren(JavaGrammar.PACKAGE_DECLARATION)) {
      AstNode packageNameNode = astNode.getFirstChild(JavaGrammar.PACKAGE_DECLARATION).getFirstChild(JavaTreeMaker.QUALIFIED_EXPRESSION_KINDS);
      return getAstNodeValue(packageNameNode).replace('.', '/');
    } else {
      // unnamed package
      return "";
    }
  }

  private static boolean isEmptyFileOrParseError(AstNode astNode) {
    return astNode == null || GenericTokenType.EOF.equals(astNode.getToken().getType());
  }

  private static String getAstNodeValue(AstNode astNode) {
    StringBuilder sb = new StringBuilder();
    for (AstNode child : AstNodeHacks.getDescendants(astNode)) {
      if (!child.hasChildren() && child.hasToken()) {
        sb.append(child.getTokenValue());
      }
    }
    return sb.toString();
  }

}
