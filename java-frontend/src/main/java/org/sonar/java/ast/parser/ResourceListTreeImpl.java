/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.java.ast.parser;

import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import java.util.ArrayList;
import java.util.List;

public class ResourceListTreeImpl extends ListTreeImpl<Tree> {

  private ResourceListTreeImpl(List<Tree> resources, List<SyntaxToken> tokens) {
    super(resources, tokens);
  }

  public static ResourceListTreeImpl emptyList() {
    return new ResourceListTreeImpl(new ArrayList<>(), new ArrayList<>());
  }
}
