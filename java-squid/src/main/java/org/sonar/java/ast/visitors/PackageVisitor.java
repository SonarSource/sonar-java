/*
 * Sonar Java
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
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourcePackage;
import org.sonar.squid.api.SourceProject;

public class PackageVisitor extends JavaAstVisitor {

  @Override
  public void visitFile(AstNode astNode) {
    SourceProject sourceProject = (SourceProject) getContext().peekSourceCode();
    SourcePackage sourcePackage = findOrCreateSourcePackage(sourceProject, getPackageKey(astNode));
    sourcePackage.setMeasure(JavaMetric.PACKAGES, 1);
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
    if (astNode == null) {
      // TODO error during parse, e.g. empty file
      return "";
    }
    if (astNode.getChild(0).is(getContext().getGrammar().packageDeclaration)) {
      AstNode packageNameNode = astNode.getChild(0).findFirstDirectChild(getContext().getGrammar().qualifiedIdentifier);
      String packageName = getAstNodeValue(packageNameNode);
      return packageName.replace('.', '/');
    } else {
      return "";
    }
  }

  private static String getAstNodeValue(AstNode astNode) {
    StringBuilder sb = new StringBuilder();
    for (AstNode child : astNode.getChildren()) {
      sb.append(child.getTokenValue());
    }
    return sb.toString();
  }

}
