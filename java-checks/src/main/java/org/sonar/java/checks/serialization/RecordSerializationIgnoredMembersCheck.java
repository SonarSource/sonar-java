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
package org.sonar.java.checks.serialization;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6209")
public class RecordSerializationIgnoredMembersCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers SERIALIZABLE_MATCHERS = MethodMatchers.or(
    // java.io.Serializable contract
    methodMatcher("readObjectNoData"),
    methodMatcher("readObject", "java.io.ObjectInputStream"),
    methodMatcher("writeObject", "java.io.ObjectOutputStream"),
    // java.io.Externalizable contract
    methodMatcher("readExternal", "java.io.ObjectInput"),
    methodMatcher("writeExternal", "java.io.ObjectOutput"));

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    for (Tree member : ((ClassTree) tree).members()) {
      if (member.is(Tree.Kind.VARIABLE)) {
        checkField((VariableTree) member);
      } else if (member.is(Tree.Kind.METHOD)) {
        checkMethod((MethodTree) member);
      }
    }
  }

  private void checkField(VariableTree field) {
    if (isSerialPersistentFields(field.symbol())) {
      reportIssue(field.simpleName(), issueMessage("field"));
    }
  }

  private static boolean isSerialPersistentFields(Symbol field) {
    // a non-static serialPersistentFields field causes compilation errors
    return "serialPersistentFields".equals(field.name())
      && field.isPrivate()
      && field.isFinal()
      && field.type().is("java.io.ObjectStreamField[]");
  }

  private void checkMethod(MethodTree method) {
    Symbol.MethodSymbol methodSymbol = method.symbol();
    if (!SERIALIZABLE_MATCHERS.matches(methodSymbol)) {
      return;
    }
    if (isFromExternalizable(methodSymbol) || isFromSerializable(methodSymbol)) {
      reportIssue(method.simpleName(), issueMessage("method"));
    }
  }

  private static boolean isFromSerializable(Symbol.MethodSymbol method) {
    return method.name().contains("Object") && method.isPrivate();
  }

  private static boolean isFromExternalizable(Symbol.MethodSymbol method) {
    return method.name().contains("External") && !method.overriddenSymbols().isEmpty();
  }

  private static String issueMessage(String tree) {
    return String.format("Remove this %s that will be ignored during record serialization.", tree);
  }

  private static MethodMatchers methodMatcher(String methodName, String ... parameterTypes) {
    return MethodMatchers.create()
      // since we target method declarations, checking the owner is not relevant
      .ofAnyType()
      .names(methodName)
      .addParametersMatcher(parameterTypes)
      .build();
  }
}
