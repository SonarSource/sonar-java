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
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.squid.api.AnalysisException;
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
      // TODO error during parse?
      return "";
    }
    String packageKey;
    if (astNode.getChild(0).is(getContext().getGrammar().packageDeclaration)) {
      AstNode packageNameNode = astNode.getChild(0).findFirstDirectChild(getContext().getGrammar().qualifiedIdentifier);
      packageKey = getAstNodeValue(packageNameNode).replace('.', '/');
    } else {
      // Guess package key from directory
      packageKey = InputFileUtils.getRelativeDirectory(getInputFile());
    }
    checkPhysicalDirectory(packageKey);
    return packageKey;
  }

  private static String getAstNodeValue(AstNode astNode) {
    StringBuilder sb = new StringBuilder();
    for (AstNode child : astNode.getChildren()) {
      sb.append(child.getTokenValue());
    }
    return sb.toString();
  }

  private InputFile getInputFile() {
    return ((VisitorContext) getContext()).getInputFile(); // TODO Unchecked cast
  }

  /**
   * Check that package declaration is consistent with the physical location of Java file.
   * It aims to detect two cases :
   * - wrong package declaration : "package org.foo" stored in the directory "org/bar"
   * - source directory badly configured : src/ instead of src/main/java/
   *
   * @since 2.8
   */
  private void checkPhysicalDirectory(String key) {
    String relativeDirectory = InputFileUtils.getRelativeDirectory(getInputFile());
    // both relativeDirectory and key use slash '/' as separator
    if (!StringUtils.equals(relativeDirectory, key)) {
      String packageName = StringUtils.replace(key, "/", ".");
      if (StringUtils.contains(relativeDirectory, key) || StringUtils.contains(key, relativeDirectory)) {
        throw new AnalysisException(String.format("The source directory does not correspond to the package declaration %s", packageName));
      }
      throw new AnalysisException(String.format("The package declaration %s does not correspond to the file path %s", packageName, getInputFile().getRelativePath()));
    }
  }

}
