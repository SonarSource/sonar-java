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

import java.util.List;
import org.sonar.java.annotations.Beta;

/**
 * Record pattern tree.
 * Introduced with Java 19 and JEP-405.
 * Finalized with Java 21 and JEP-440.
 *
 * <pre>
 *   case {@link #type()} ( {@link #patterns()} ) : ...
 *   case {@link #type()} ( {@link #patterns()} ) -> ...
 * </pre>
 *
 * @since Java 19
 */
@Beta
public interface RecordPatternTree extends PatternTree {

  TypeTree type();

  SyntaxToken openParenToken();

  List<PatternTree> patterns();

  SyntaxToken closeParenToken();
}
