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
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonarsource.analyzer.commons.collections.MapBuilder;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S4738")
public class ReplaceGuavaWithJavaCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final String USE_INSTEAD = "Use \"%s\" instead.";

  private static final String GUAVA_BASE_ENCODING = "com.google.common.io.BaseEncoding";
  private static final String GUAVA_OPTIONAL = "com.google.common.base.Optional";
  private static final String GUAVA_FILES = "com.google.common.io.Files";
  private static final String GUAVA_IMMUTABLE_SET = "com.google.common.collect.ImmutableSet";
  private static final String GUAVA_IMMUTABLE_LIST = "com.google.common.collect.ImmutableList";
  private static final String GUAVA_IMMUTABLE_MAP = "com.google.common.collect.ImmutableMap";

  private static final Map<String, String> GUAVA_TO_JAVA_UTIL_TYPES = MapBuilder.<String, String>newMap()
    .put("com.google.common.base.Predicate", "java.util.function.Predicate")
    .put("com.google.common.base.Function", "java.util.function.Function")
    .put("com.google.common.base.Supplier", "java.util.function.Supplier")
    .put(GUAVA_OPTIONAL, "java.util.Optional")
    .build();

  private static final Map<String, String> GUAVA_OPTIONAL_TO_JAVA_UTIL_METHODS = MapBuilder.<String, String>newMap()
    .put("of", "of")
    .put("absent", "empty")
    .put("fromNullable", "ofNullable")
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create().ofTypes(GUAVA_BASE_ENCODING).names("base64", "base64Url").addWithoutParametersMatcher().build(),
      MethodMatchers.create().ofTypes(GUAVA_OPTIONAL).names("absent").addWithoutParametersMatcher().build(),
      MethodMatchers.create().ofTypes(GUAVA_OPTIONAL).names("fromNullable", "of").withAnyParameters().build(),
      MethodMatchers.create().ofTypes(GUAVA_FILES).names("createTempDir").addWithoutParametersMatcher().build(),
      MethodMatchers.create().ofTypes(GUAVA_IMMUTABLE_LIST, GUAVA_IMMUTABLE_SET, GUAVA_IMMUTABLE_MAP)
        .names("of").withAnyParameters().build());
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    ArrayList<Tree.Kind> kinds = new ArrayList<>(super.nodesToVisit());
    kinds.add(Tree.Kind.VARIABLE);
    return kinds;
  }

  @Override
  public void visitNode(Tree tree) {
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
    switch(mit.methodSymbol().owner().type().fullyQualifiedName()) {
      case GUAVA_BASE_ENCODING:
        reportIssue(mit, replacementMessage("java.util.Base64"));
        break;
      case GUAVA_OPTIONAL:
        reportIssue(mit, replacementMessage("java.util.Optional." + GUAVA_OPTIONAL_TO_JAVA_UTIL_METHODS.get(mit.methodSymbol().name())));
        break;
      case GUAVA_FILES:
        reportIssue(mit, replacementMessage("java.nio.file.Files.createTempDirectory"));
        break;
      case GUAVA_IMMUTABLE_LIST:
        reportJava9Issue(mit, "java.util.List.of()");
        break;
      case GUAVA_IMMUTABLE_SET:
        reportJava9Issue(mit, "java.util.Set.of()");
        break;
      case GUAVA_IMMUTABLE_MAP:
        reportJava9Issue(mit, "java.util.Map.of()\" or \"java.util.Map.ofEntries()");
        break;
      default:
        break;
    }
  }

  private void reportJava9Issue(MethodInvocationTree mit, String replacement) {
    if (context.getJavaVersion().isJava9Compatible()) {
      reportIssue(mit, replacementMessage(replacement));
    }
  }

  private String replacementMessage(String replacement) {
    return String.format(USE_INSTEAD, replacement) + context.getJavaVersion().java8CompatibilityMessage();
  }
}
