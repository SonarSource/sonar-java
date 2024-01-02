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
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S6485")
public class KnownCapacityHashBasedCollectionCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final Map<String, String> TYPES_TO_METHODS = Map.of(
    "HashMap", "HashMap.newHashMap(int numMappings)",
    "HashSet", "HashSet.newHashSet(int numMappings)",
    "LinkedHashMap", "LinkedHashMap.newLinkedHashMap(int numMappings)",
    "LinkedHashSet", "LinkedHashSet.newLinkedHashSet(int numMappings)",
    "WeakHashMap", "WeakHashMap.newWeakHashMap(int numMappings)");

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.NEW_CLASS);
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    reportIssue(newClassTree, getIssueMessage(newClassTree));
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("java.util.HashMap", "java.util.HashSet", "java.util.LinkedHashMap", "java.util.LinkedHashSet", "java.util.WeakHashMap")
      .constructor()
      .addParametersMatcher("int")
      .build();
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava19Compatible();
  }

  private static String getIssueMessage(NewClassTree newClassTree) {
    String replacementMethod = TYPES_TO_METHODS.get(newClassTree.symbolType().name());
    return String.format("Replace this call to the constructor with the better suited static method %s", replacementMethod);
  }

}
