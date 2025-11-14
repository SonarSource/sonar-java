/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
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
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(newClassTree)
      .withMessage(getIssueMessage(newClassTree))
      .withQuickFix(() -> computeQuickFix(newClassTree))
      .report();
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

  private static JavaQuickFix computeQuickFix(NewClassTree newClassTree) {
    String replacementMethod = TYPES_TO_METHODS.get(newClassTree.symbolType().name()).replace("(int numMappings)", "");
    JavaTextEdit edit = JavaTextEdit.replaceBetweenTree(newClassTree.firstToken(), newClassTree.identifier().lastToken(), replacementMethod);
    return JavaQuickFix.newQuickFix("Replace with \"" + replacementMethod + "\".")
      .addTextEdit(edit)
      .build();
  }

}
