/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.model.declaration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.ModuleNameTree;
import org.sonar.plugins.java.api.tree.Tree;

public abstract class SimpleModuleDirectiveTreeImpl extends ModuleDirectiveTreeImpl {

  protected final ExpressionTree packageName;
  @Nullable
  protected final InternalSyntaxToken toKeyword;
  protected final ListTree<ModuleNameTree> moduleNames;

  protected SimpleModuleDirectiveTreeImpl(InternalSyntaxToken keyword, ExpressionTree packageName, @Nullable InternalSyntaxToken toKeyword, ListTree<ModuleNameTree> moduleNames,
                                       InternalSyntaxToken semicolonToken) {
    super(keyword, semicolonToken);
    this.packageName = packageName;
    this.toKeyword = toKeyword;
    this.moduleNames = moduleNames;
  }

  @Override
  protected List<Tree> children() {
    List<Tree> list = new ArrayList<>();
    list.add(directiveKeyword());
    list.add(packageName);
    if (toKeyword != null) {
      list.add(toKeyword);
      list.add(moduleNames);
    }
    list.add(semicolonToken());
    return Collections.unmodifiableList(list);
  }

}
