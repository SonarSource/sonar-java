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
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.squidbridge.api.SourceClass;
import org.sonar.squidbridge.api.SourceMethod;
import org.sonar.squidbridge.api.SourcePackage;

import javax.annotation.Nullable;

public class ClassVisitor extends JavaAstVisitor {

  private int localNameCounter;

  @Override
  public void init() {
    subscribeTo(
        JavaGrammar.CLASS_DECLARATION,
        JavaGrammar.INTERFACE_DECLARATION,
        JavaGrammar.ENUM_DECLARATION,
        JavaGrammar.ANNOTATION_TYPE_DECLARATION);
  }

  @Override
  public void visitFile(@Nullable AstNode astNode) {
    localNameCounter = 0;
  }

  @Override
  public void visitNode(AstNode astNode) {
    String className = astNode.getFirstChild(JavaTokenType.IDENTIFIER).getTokenValue();
    final SourceClass sourceClass;
    if (getContext().peekSourceCode().isType(SourceClass.class)) {
      sourceClass = createSourceClass((SourceClass) getContext().peekSourceCode(), className);
    } else if (getContext().peekSourceCode().isType(SourceMethod.class)) {
      localNameCounter++;
      sourceClass = createSourceClass((SourceClass) getContext().peekSourceCode().getParent(), localNameCounter+className);
    } else {
      sourceClass = createSourceClass(peekParentPackage(), className);
    }

    sourceClass.setStartAtLine(astNode.getTokenLine());
    sourceClass.setMeasure(JavaMetric.CLASSES, 1);
    sourceClass.setSuppressWarnings(SuppressWarningsAnnotationUtils.isSuppressAllWarnings(astNode));
    getContext().addSourceCode(sourceClass);
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
