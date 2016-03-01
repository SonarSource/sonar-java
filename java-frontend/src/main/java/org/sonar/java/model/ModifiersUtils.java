/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.model;

import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;

import javax.annotation.CheckForNull;

public final class ModifiersUtils {

  private ModifiersUtils() {
    // This class only contains static methods
  }

  public static boolean hasModifier(ModifiersTree modifiers, Modifier expectedModifier) {
    for (ModifierKeywordTree modifierKeywordTree : modifiers.modifiers()) {
      if (expectedModifier.equals(modifierKeywordTree.modifier())) {
        return true;
      }
    }
    return false;
  }

  @CheckForNull
  public static ModifierKeywordTree getModifier(ModifiersTree modifiers, Modifier modifier) {
    for (ModifierKeywordTree modifierKeywordTree : modifiers.modifiers()) {
      if (modifier.equals(modifierKeywordTree.modifier())) {
        return modifierKeywordTree;
      }
    }
    return null;
  }
}
