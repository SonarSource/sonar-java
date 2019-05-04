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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S4738")
public class ReplaceGuavaWithJava8Check extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final String USE_INSTEAD = "Use \"%s\" instead.";

  private static final String GUAVA_BASE_ENCODING = "com.google.common.io.BaseEncoding";
  private static final String GUAVA_OPTIONAL = "com.google.common.base.Optional";
  private static final String GUAVA_JOINER = "com.google.common.base.Joiner";

  private static final Map<String, String> GUAVA_TO_JAVA_UTIL_TYPES = ImmutableMap.<String, String>builder()
    .put("com.google.common.base.Predicate", "java.util.function.Predicate")
    .put("com.google.common.base.Function", "java.util.function.Function")
    .put("com.google.common.base.Supplier", "java.util.function.Supplier")
    .put(GUAVA_OPTIONAL, "java.util.Optional")
    .build();

  private static final Map<String, String> GUAVA_OPTIONAL_TO_JAVA_UTIL_METHODS = ImmutableMap.<String, String>builder()
    .put("of", "of")
    .put("absent", "empty")
    .put("fromNullable", "ofNullable")
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition(GUAVA_BASE_ENCODING).name("base64").withoutParameter(),
      MethodMatcher.create().typeDefinition(GUAVA_BASE_ENCODING).name("base64Url").withoutParameter(),
      MethodMatcher.create().typeDefinition(GUAVA_OPTIONAL).name("absent").withoutParameter(),
      MethodMatcher.create().typeDefinition(GUAVA_OPTIONAL).name("fromNullable").withAnyParameters(),
      MethodMatcher.create().typeDefinition(GUAVA_OPTIONAL).name("of").withAnyParameters(),
      MethodMatcher.create().typeDefinition(GUAVA_JOINER).name("on").withAnyParameters()
    );
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    ArrayList<Tree.Kind> kinds = new ArrayList<>(super.nodesToVisit());
    kinds.add(Tree.Kind.VARIABLE);
    return kinds;
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    if (tree.is(Tree.Kind.VARIABLE)) {
      checkTypeToReplace((VariableTree) tree);
    } else {
      super.visitNode(tree);
    }
  }

  private void checkTypeToReplace(VariableTree variableTree) {
    String fullyQualifiedTypeName = variableTree.type().symbolType().fullyQualifiedName();
    if (GUAVA_TO_JAVA_UTIL_TYPES.containsKey(fullyQualifiedTypeName)) {
      reportIssue(variableTree.type(), replacementMessage(GUAVA_TO_JAVA_UTIL_TYPES.get(fullyQualifiedTypeName)));
    }
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    switch(mit.symbol().owner().type().fullyQualifiedName()) {
      case GUAVA_BASE_ENCODING:
        reportIssue(mit, replacementMessage("java.util.Base64"));
        break;
      case GUAVA_OPTIONAL:
        reportIssue(mit, replacementMessage("java.util.Optional." + GUAVA_OPTIONAL_TO_JAVA_UTIL_METHODS.get(mit.symbol().name())));
        break;
      case GUAVA_JOINER:
        reportIssue(mit, replacementMessage("String.join\" or \"java.util.stream.Collectors.joining"));
        break;
      default:
        break;
    }
  }

  private String replacementMessage(String replacement) {
    return String.format(USE_INSTEAD, replacement) + context.getJavaVersion().java8CompatibilityMessage();
  }
}
