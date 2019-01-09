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
package org.sonar.java.checks.serialization;

import com.google.common.collect.ImmutableSet;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;
import java.util.Set;

public final class SerializableContract {

  public static final Set<String> SERIALIZABLE_CONTRACT_METHODS = ImmutableSet.of(
    "writeObject",
    "readObject",
    "writeReplace",
    "readResolve",
    "readObjectNoData"
  );

  public static final String SERIAL_VERSION_UID_FIELD = "serialVersionUID";

  private SerializableContract() {
  }

  public static boolean hasSpecialHandlingSerializationMethods(ClassTree classTree) {
    boolean hasWriteObject = false;
    boolean hasReadObject = false;
    String classFullyQualifiedName = classTree.symbol().type().fullyQualifiedName();
    for (Tree member : classTree.members()) {

      MethodMatcher writeObjectMatcher = writeObjectMatcher(classFullyQualifiedName);
      MethodMatcher readObjectMatcher = readObjectMatcher(classFullyQualifiedName);

      if (member.is(Tree.Kind.METHOD)) {
        MethodTree methodTree = (MethodTree) member;
        if (ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.PRIVATE)) {
          hasWriteObject |= writeObjectMatcher.matches(methodTree) && methodThrows(methodTree, "java.io.IOException");
          hasReadObject |= readObjectMatcher.matches(methodTree) && methodThrows(methodTree, "java.io.IOException", "java.lang.ClassNotFoundException");
        }
      }
    }
    return hasReadObject && hasWriteObject;
  }

  public static MethodMatcher readObjectMatcher(String classFullyQualifiedName) {
    return MethodMatcher.create().typeDefinition(classFullyQualifiedName).name("readObject").addParameter("java.io.ObjectInputStream");
  }

  public static MethodMatcher writeObjectMatcher(String classFullyQualifiedName) {
    return MethodMatcher.create().typeDefinition(classFullyQualifiedName).name("writeObject").addParameter("java.io.ObjectOutputStream");
  }

  private static boolean methodThrows(MethodTree methodTree, String... throwClauseFullyQualifiedNames) {
    List<Type> thrownTypes = methodTree.symbol().thrownTypes();
    if (thrownTypes.isEmpty() || thrownTypes.size() != throwClauseFullyQualifiedNames.length) {
      return false;
    }
    for (Type thrownType : thrownTypes) {
      boolean match = false;
      for (String fullyQualifiedName : throwClauseFullyQualifiedNames) {
        match |= thrownType.is(fullyQualifiedName);
      }
      if (!match) {
        return false;
      }
    }
    return true;
  }

}
