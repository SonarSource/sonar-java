/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.java.api.tree;

import org.sonar.java.annotations.Beta;
import java.util.List;

/**
 * 'case' label in a 'switch' statement.
 *
 * JLS 14.11
 *
 * <pre>
 *   case {@link #expression()} :
 *   default :
 * </pre>
 *
 * @since Java 1.3
 *
 * <pre>
 *   case {@link #expressions()} :
 *   case {@link #expressions()} ->
 *   default ->
 * </pre>
 *
 * @since Java 12 (SonarJava 5.12 - Support of Java 12)
 */
@Beta
public interface CaseLabelTree extends Tree {

  SyntaxToken caseOrDefaultKeyword();

  /**
   * @return true for case with colon: "case 3:" or "default:"
   *         false for case with arrow: "case 3 ->" or "default ->"
   * @since SonarJava 5.12: Support of Java 12
   */
  boolean isFallThrough();

  /**
   * @since SonarJava 5.12: Support of Java 12
   */
  List<ExpressionTree> expressions();

  /**
   * @since SonarJava 5.12: Support of Java 12
   */
  SyntaxToken colonOrArrowToken();

}
