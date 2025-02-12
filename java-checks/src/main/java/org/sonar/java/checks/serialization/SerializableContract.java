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
package org.sonar.java.checks.serialization;

import java.util.List;
import java.util.Set;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;

public final class SerializableContract {

  public static final Set<String> SERIALIZABLE_CONTRACT_METHODS = SetUtils.immutableSetOf(
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

      MethodMatchers writeObjectMatcher = writeObjectMatcher(classFullyQualifiedName);
      MethodMatchers readObjectMatcher = readObjectMatcher(classFullyQualifiedName);

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

  public static MethodMatchers readObjectMatcher(String classFullyQualifiedName) {
    return MethodMatchers.create().ofTypes(classFullyQualifiedName).names("readObject").addParametersMatcher("java.io.ObjectInputStream").build();
  }

  public static MethodMatchers writeObjectMatcher(String classFullyQualifiedName) {
    return MethodMatchers.create().ofTypes(classFullyQualifiedName).names("writeObject").addParametersMatcher("java.io.ObjectOutputStream").build();
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
