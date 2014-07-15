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
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourcePackage;
import org.sonar.squidbridge.api.SourceProject;

public class PackageVisitor extends JavaAstVisitor {

  public static final String UNRESOLVED_PACKAGE = "!error!";

  @Override
  public void visitFile(AstNode astNode) {
    SourceProject sourceProject = (SourceProject) getContext().peekSourceCode();
    SourcePackage sourcePackage = findOrCreateSourcePackage(sourceProject, getPackageKey(astNode));
    getContext().addSourceCode(sourcePackage);
  }


  @Override
  public void leaveFile(AstNode astNode) {
    Preconditions.checkState(getContext().peekSourceCode().isType(SourcePackage.class));
    getContext().popSourceCode();
  }

  private static SourcePackage findOrCreateSourcePackage(SourceProject sourceProject, String packageKey) {
    if (sourceProject.hasChildren()) {
      for (SourceCode child : sourceProject.getChildren()) {
        if (child.getKey().equals(packageKey)) {
          return (SourcePackage) child;
        }
      }
    }
    return new SourcePackage(packageKey);
  }

  private String getPackageKey(AstNode astNode) {
    if (isEmptyFileOrParseError(astNode)) {
      // Cannot resolve package for empty file and parse error.
      return UNRESOLVED_PACKAGE;
    } else if (astNode.getFirstChild().is(JavaGrammar.PACKAGE_DECLARATION)) {
      AstNode packageNameNode = astNode.getFirstChild().getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER);
      return getAstNodeValue(packageNameNode).replace('.', '/');
    } else {
      // unnamed package
      return "";
    }
  }

  private boolean isEmptyFileOrParseError(AstNode astNode) {
    return astNode == null || astNode.getFirstChild().is(GenericTokenType.EOF);
  }

  private static String getAstNodeValue(AstNode astNode) {
    StringBuilder sb = new StringBuilder();
    for (AstNode child : astNode.getChildren()) {
      sb.append(child.getTokenValue());
    }
    return sb.toString();
  }

}
