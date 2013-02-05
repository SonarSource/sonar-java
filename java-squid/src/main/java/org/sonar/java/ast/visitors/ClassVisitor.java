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
import com.sonar.sslr.api.GenericTokenType;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourcePackage;

public class ClassVisitor extends JavaAstVisitor {

  @Override
  public void init() {
    subscribeTo(
        getContext().getGrammar().classDeclaration,
        getContext().getGrammar().interfaceDeclaration,
        getContext().getGrammar().enumDeclaration,
        getContext().getGrammar().annotationTypeDeclaration);
  }

  @Override
  public void visitNode(AstNode astNode) {
    String className = astNode.getFirstChild(GenericTokenType.IDENTIFIER).getTokenValue();

    final SourceClass sourceClass;
    if (getContext().peekSourceCode().isType(SourceClass.class)) {
      sourceClass = createSourceClass((SourceClass) getContext().peekSourceCode(), className);
    } else {
      sourceClass = createSourceClass(peekParentPackage(), className);
    }

    sourceClass.setStartAtLine(astNode.getTokenLine());
    sourceClass.setMeasure(JavaMetric.CLASSES, 1);
    getContext().addSourceCode(sourceClass);
    // TODO sourceClass.setSuppressWarnings(suppressWarnings)
  }

  @Override
  public void leaveNode(AstNode astNode) {
    Preconditions.checkState(getContext().peekSourceCode().isType(SourceClass.class));
    getContext().popSourceCode();
  }

  static SourceClass createSourceClass(SourcePackage parentPackage, String className) {
    StringBuilder key = new StringBuilder();
    if (parentPackage != null && !"".equals(parentPackage.getKey())) {
      key.append(parentPackage.getKey());
      key.append("/");
    }
    key.append(className);
    return new SourceClass(key.toString(), className);
  }

  static SourceClass createSourceClass(SourceClass parentClass, String innerClassName) {
    return new SourceClass(parentClass.getKey() + "$" + innerClassName, innerClassName);
  }

}
