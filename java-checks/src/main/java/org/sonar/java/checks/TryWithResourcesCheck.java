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
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

@Rule(key = "S2093")
public class TryWithResourcesCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final MethodMatchers AUTOCLOSEABLE_BUILDER_MATCHER = MethodMatchers.or(
    MethodMatchers.create().ofTypes("java.net.http.HttpClient$Builder").names("build").addWithoutParametersMatcher().build(),
    MethodMatchers.create().ofTypes("java.net.http.HttpClient").names("newHttpClient").addWithoutParametersMatcher().build()
  );

  private static final MethodMatchers AUTOCLOSEABLE_FACTORY_MATCHER =
    MethodMatchers.create().ofSubTypes("java.io.Reader")
      .names("of")
      .addParametersMatcher("java.lang.CharSequence").build();

  private final Deque<TryStatementTree> withinTry = new LinkedList<>();
  private final Deque<List<Tree>> toReport = new LinkedList<>();

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    withinTry.clear();
    toReport.clear();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.TRY_STATEMENT, Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.TRY_STATEMENT)) {
      withinTry.push((TryStatementTree) tree);
      if (withinTry.size() != toReport.size()) {
        toReport.push(new ArrayList<>());
      }
    } else if (isNewAutocloseableOrBuilder(tree, context)) {
      if (withinStandardTryWithFinally()) {
        toReport.peek().add(tree);
      } else if (isFollowedByTryWithFinally(tree)) {
        if (toReport.isEmpty() || withinTry.size() == toReport.size()) {
          // This newClass will be reported with the following tryStatement
          toReport.push(new ArrayList<>());
        }
        toReport.peek().add(tree);
      }
    }
  }

  private static boolean isNewAutocloseableOrBuilder(Tree tree, JavaFileScannerContext context) {
    return (tree instanceof NewClassTree newClass && newClass.symbolType().isSubtypeOf("java.lang.AutoCloseable")) ||
      (context.getJavaVersion().isJava21Compatible() && tree instanceof MethodInvocationTree mit && AUTOCLOSEABLE_BUILDER_MATCHER.matches(mit))||
      (tree instanceof MethodInvocationTree mit2 && AUTOCLOSEABLE_FACTORY_MATCHER.matches(mit2));
  }

  private static boolean isFollowedByTryWithFinally(Tree tree) {
    Tree blockParent = tree.parent();
    while (blockParent != null && !blockParent.is(Tree.Kind.BLOCK)) {
      blockParent = blockParent.parent();
    }

    if (blockParent != null) {
      CFG cfg = CFG.buildCFG(Collections.singletonList(blockParent));
      if (!cfg.blocks().isEmpty()) {
        return newFollowedByTryStatement(cfg.blocks().get(0));
      }
    }

    // Unreachable by construction because the CFG has been built on top of a NewClass element
    return false;
  }

  private static boolean newFollowedByTryStatement(CFG.Block cfgBlock) {
    boolean foundNewAutoCloseable = false;
    for (Tree element : cfgBlock.elements()) {
      switch (element.kind()) {
        case NEW_CLASS:
          boolean isAutoCloseable = ((NewClassTree) element).symbolType().isSubtypeOf("java.lang.AutoCloseable");
          if (!isAutoCloseable && foundNewAutoCloseable) {
            return false;
          }
          foundNewAutoCloseable = isAutoCloseable;
          break;
        case TRY_STATEMENT:
          if (((TryStatementTree) element).resourceList().isEmpty() && ((TryStatementTree) element).finallyBlock() != null) {
            return foundNewAutoCloseable;
          }
          return false;
        case VARIABLE:
          break;
        default:
          if (foundNewAutoCloseable) {
            return false;
          }
          break;
      }
    }
    // Unreachable: by construction at least one element of the CFG.Block is a NewClass of type "java.lang.AutoCloseable"
    return false;
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.TRY_STATEMENT)) {
      TryStatementTree tryStatementTree = withinTry.pop();
      List<Tree> secondaryTrees = toReport.pop();
      if (!secondaryTrees.isEmpty()) {
        List<JavaFileScannerContext.Location> secondary = new ArrayList<>();
        for (Tree autoCloseable : secondaryTrees) {
          secondary.add(new JavaFileScannerContext.Location("AutoCloseable resource", autoCloseable));
        }
        reportIssue(tryStatementTree.tryKeyword(),
          "Change this \"try\" to a try-with-resources." + context.getJavaVersion().java7CompatibilityMessage(), secondary, null);
      }
    }
  }

  private boolean withinStandardTryWithFinally() {
    return !withinTry.isEmpty() && withinTry.peek().resourceList().isEmpty() && withinTry.peek().finallyBlock() != null;
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava7Compatible();
  }
}
