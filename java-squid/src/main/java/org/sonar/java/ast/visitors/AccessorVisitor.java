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
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.squid.api.SourceMethod;

import java.util.List;

public class AccessorVisitor extends JavaAstVisitor {

  @Override
  public void init() {
    MethodHelper.subscribe(this);
  }

  @Override
  public void visitNode(AstNode astNode) {
    SourceMethod sourceMethod = (SourceMethod) getContext().peekSourceCode();

    MethodHelper methodHelper = new MethodHelper(getContext().getGrammar(), astNode);
    if (methodHelper.isPublic() && isAccessor(methodHelper)) {
      sourceMethod.setMeasure(JavaMetric.ACCESSORS, 1);
    }
  }

  @Override
  public void leaveNode(AstNode astNode) {
    SourceMethod sourceMethod = (SourceMethod) getContext().peekSourceCode();
    if (sourceMethod.getInt(JavaMetric.ACCESSORS) != 0) {
      // TODO
      // sourceMethod.setMeasure(Metric.PUBLIC_API, 0);
      // sourceMethod.setMeasure(Metric.PUBLIC_DOC_API, 0);
      sourceMethod.setMeasure(JavaMetric.METHODS, 0);
      sourceMethod.setMeasure(JavaMetric.COMPLEXITY, 0);
    }
  }

  private boolean isAccessor(MethodHelper method) {
    return isValidGetter(method) || isValidSetter(method) || isValidBooleanGetter(method);
  }

  private boolean isValidGetter(MethodHelper method) {
    String methodName = method.getName().getTokenValue();
    if (methodName.startsWith("get") && !method.hasParameters() && !method.getReturnType().is(JavaKeyword.VOID)) {
      // TODO Godin: in previous version we had a more complex check of method body
      List<AstNode> statements = method.getStatements();
      return statements.size() == 1 && "return".equals(statements.get(0).getTokenValue());
    }
    return false;
  }

  private boolean isValidSetter(MethodHelper method) {
    String methodName = method.getName().getTokenValue();
    if (methodName.startsWith("set") && (method.getParameters().size() == 1) && method.getReturnType().is(JavaKeyword.VOID)) {
      // TODO Godin: in previous version we had a more complex check of method body
      List<AstNode> statements = method.getStatements();
      return statements.size() == 1 && "this".equals(statements.get(0).getTokenValue());
    }
    return false;
  }

  private boolean isValidBooleanGetter(MethodHelper method) {
    String methodName = method.getName().getTokenValue();
    if (methodName.startsWith("is") && !method.hasParameters() && hasBooleanReturnType(method)) {
      // TODO Godin: in previous version we had a more complex check of method body
      List<AstNode> statements = method.getStatements();
      return statements.size() == 1 && "return".equals(statements.get(0).getTokenValue());
    }
    return false;
  }

  private boolean hasBooleanReturnType(MethodHelper method) {
    AstNode node = method.getReturnType();
    return node.getChildren().size() == 1 && node.getChild(0).getChild(0).is(JavaKeyword.BOOLEAN);
  }

}
