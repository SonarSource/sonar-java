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

import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceMethod;

public class MethodVisitor extends JavaAstVisitor {

  @Override
  public void init() {
    MethodHelper.subscribe(this);
  }

  @Override
  public void visitNode(AstNode astNode) {
    String methodName = buildMethodSignature(astNode);
    SourceClass sourceClass = peekSourceClass();
    SourceMethod sourceMethod = new SourceMethod(sourceClass, methodName, astNode.getTokenLine());
    sourceMethod.setMeasure(JavaMetric.METHODS, 1);
    getContext().addSourceCode(sourceMethod);
  }

  @Override
  public void leaveNode(AstNode astNode) {
    getContext().popSourceCode();
  }

  private String buildMethodSignature(AstNode astNode) {
    // TODO use real signature?
    return extractMethodName(astNode) + ":" + astNode.getTokenLine();
  }

  private String extractMethodName(AstNode astNode) {
    return new MethodHelper(getContext().getGrammar(), astNode).getName().getTokenValue();
  }

}
