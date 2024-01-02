/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.ModuleDirectiveTree;
import org.sonar.plugins.java.api.tree.ModuleNameTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;

import java.util.List;

public class ModuleDeclarationTreeImpl extends JavaTree implements ModuleDeclarationTree {

  private final List<AnnotationTree> annotations;
  private final InternalSyntaxToken openKeyword;
  private final InternalSyntaxToken moduleKeyword;
  private final ModuleNameTree moduleName;
  private final InternalSyntaxToken openBraceToken;
  private final List<ModuleDirectiveTree> moduleDirectives;
  private final InternalSyntaxToken closeBraceToken;

  public ModuleDeclarationTreeImpl(List<AnnotationTree> annotations, @Nullable InternalSyntaxToken openKeyword, InternalSyntaxToken moduleKeyword, ModuleNameTree moduleName,
    InternalSyntaxToken openBraceToken, List<ModuleDirectiveTree> moduleDirectives, InternalSyntaxToken closeBraceToken) {
    this.annotations = annotations;
    this.openKeyword = openKeyword;
    this.moduleKeyword = moduleKeyword;
    this.moduleName = moduleName;
    this.openBraceToken = openBraceToken;
    this.moduleDirectives = moduleDirectives;
    this.closeBraceToken = closeBraceToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitModule(this);
  }

  @Override
  public Tree.Kind kind() {
    return Tree.Kind.MODULE;
  }

  @Override
  public List<AnnotationTree> annotations() {
    return annotations;
  }

  @Nullable
  @Override
  public SyntaxToken openKeyword() {
    return openKeyword;
  }

  @Override
  public SyntaxToken moduleKeyword() {
    return moduleKeyword;
  }

  @Override
  public ModuleNameTree moduleName() {
    return moduleName;
  }

  @Override
  public SyntaxToken openBraceToken() {
    return openBraceToken;
  }

  @Override
  public List<ModuleDirectiveTree> moduleDirectives() {
    return moduleDirectives;
  }

  @Override
  public SyntaxToken closeBraceToken() {
    return closeBraceToken;
  }

  @Override
  protected List<Tree> children() {
    List<Tree> list = new ArrayList<>(annotations);
    if (openKeyword != null) {
      list.add(openKeyword);
    }
    list.add(moduleKeyword);
    list.add(moduleName);
    list.add(openBraceToken);
    list.addAll(moduleDirectives);
    list.add(closeBraceToken);
    return Collections.unmodifiableList(list);
  }

}
