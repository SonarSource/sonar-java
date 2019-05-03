/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import org.sonar.java.ast.parser.JavaLexer;
import org.sonar.java.ast.parser.ListTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Collections;
import java.util.List;

public class ModifiersTreeImpl extends ListTreeImpl<ModifierTree> implements ModifiersTree {

  private final List<ModifierKeywordTree> modifiers;
  private final List<AnnotationTree> annotations;

  public ModifiersTreeImpl(List<ModifierTree> javaTrees) {
    super(JavaLexer.MODIFIERS, javaTrees);
    ImmutableList.Builder<ModifierKeywordTree> modifierBuilder = ImmutableList.builder();
    ImmutableList.Builder<AnnotationTree> annotationBuilder = ImmutableList.builder();
    for (ModifierTree modifierTree : this) {
      if (modifierTree.is(Kind.ANNOTATION)) {
        annotationBuilder.add((AnnotationTree) modifierTree);
      } else {
        modifierBuilder.add((ModifierKeywordTree) modifierTree);
      }
    }
    this.annotations = annotationBuilder.build();
    this.modifiers = modifierBuilder.build();
  }

  public static ModifiersTreeImpl emptyModifiers() {
    return new ModifiersTreeImpl(Collections.emptyList());
  }

  @Override
  public Kind kind() {
    return Kind.MODIFIERS;
  }

  @Override
  public List<ModifierKeywordTree> modifiers() {
    return modifiers;
  }

  @Override
  public List<AnnotationTree> annotations() {
    return annotations;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitModifier(this);
  }

}
