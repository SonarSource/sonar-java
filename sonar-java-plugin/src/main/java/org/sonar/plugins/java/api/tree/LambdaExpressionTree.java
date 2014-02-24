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
package org.sonar.plugins.java.api.tree;

import java.util.List;

/**
 * A tree node for a lambda expression.
 *
 * For example:
 * <pre>{@code
 *   ()->{}
 *   (List<String> ls)->ls.size()
 *   (x,y)-> { return x + y; }
 * }</pre>
 */
public interface LambdaExpressionTree extends ExpressionTree {

  /**
   * Lambda expressions come in two forms: (i) expression lambdas, whose body
   * is an expression, and (ii) statement lambdas, whose body is a block
   */
  public enum BodyKind {
    /** enum constant for expression lambdas */
    EXPRESSION,
    /** enum constant for statement lambdas */
    STATEMENT;
  }

  List<? extends VariableTree> getParameters();
  Tree getBody();
  BodyKind getBodyKind();
}
