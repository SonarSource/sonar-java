/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ModuleNameTree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleNameTreeImpl extends ListTreeImpl<IdentifierTree> implements ModuleNameTree {

  private ModuleNameTreeImpl(List<IdentifierTree> identifiers) {
    super(identifiers, Collections.emptyList());
  }

  public static ModuleNameTreeImpl emptyList() {
    return new ModuleNameTreeImpl(new ArrayList<>());
  }

}
