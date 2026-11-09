/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.psi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.location.InternalPosition;
import org.sonar.plugins.java.api.diagnostics.ProblemKind;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.plugins.java.api.location.Position.endOf;
import static org.sonar.plugins.java.api.location.Position.startOf;

final class PsiWarning extends PsiProblem implements org.sonar.plugins.java.api.diagnostics.Warning {
  private final Position start;
  private final Position end;

  private Tree syntaxTree;

  PsiWarning(String message, ProblemKind type, int startLine, int startColumnOffset, int endLine, int endColumnOffset) {
    super(message, type);
    this.start = InternalPosition.atOffset(startLine, startColumnOffset);
    this.end = InternalPosition.atOffset(endLine, endColumnOffset);
  }

  public Tree syntaxTree() {
    return syntaxTree;
  }

  Position start() {
    return start;
  }

  Position end() {
    return end;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PsiWarning)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    PsiWarning jWarning = (PsiWarning) o;
    return start.equals(jWarning.start) && end.equals(jWarning.end);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), start, end);
  }

  public static class Mapper extends SubscriptionVisitor {

    private static final Map<ProblemKind, Set<Tree.Kind>> WARNING_KINDS = Map.of(
      ProblemKind.UNUSED_IMPORT, Set.of(Tree.Kind.IMPORT),
      ProblemKind.REDUNDANT_CAST, Set.of(Tree.Kind.TYPE_CAST, Tree.Kind.PARENTHESIZED_EXPRESSION),
      ProblemKind.ASSIGNMENT_HAS_NO_EFFECT, Set.of(Tree.Kind.ASSIGNMENT),
      ProblemKind.MASKED_CATCH, Set.of(Tree.Kind.IDENTIFIER, Tree.Kind.MEMBER_SELECT));

    private static final Set<Tree.Kind> KINDS = WARNING_KINDS.values().stream().flatMap(Set::stream).collect(Collectors.toSet());

    private final Map<ProblemKind, Set<PsiWarning>> warningsByType = new EnumMap<>(ProblemKind.class);

    private final PriorityQueue<PsiWarning> warnings = new PriorityQueue<>(Comparator.comparing(PsiWarning::start).thenComparing(PsiWarning::end));

    private Mapper(List<PsiWarning> diagnostics) {
      WARNING_KINDS.keySet().forEach(kind -> warningsByType.put(kind, new LinkedHashSet<>()));
      warnings.addAll(diagnostics);
    }

    public static Mapper warningsFor(List<PsiWarning> diagnostics) {
      return new Mapper(diagnostics);
    }

    public void mappedInto(JavaTree.CompilationUnitTreeImpl cut) {
      scanTree(cut);
      cut.addWarnings(warningsByType);
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return new ArrayList<>(KINDS);
    }

    @Override
    public void visitNode(Tree tree) {
      if (warnings.isEmpty()) {
        return;
      }
      for (Iterator<PsiWarning> iterator = warnings.iterator(); iterator.hasNext();) {
        PsiWarning warning = iterator.next();
        if (isInsideTree(warning, tree)) {
          setSyntaxTree(warning, tree);
          warningsByType.computeIfAbsent(warning.type(), k -> new LinkedHashSet<>()).add(warning);

          if (matchesTreeExactly(warning)) {
            iterator.remove();
          }
        }
      }
    }

    @VisibleForTesting
    static void setSyntaxTree(PsiWarning warning, Tree tree) {
      if (warning.syntaxTree == null || isMorePreciseTree(warning.syntaxTree, tree)) {
        warning.syntaxTree = tree;
      }
    }

    @VisibleForTesting
    static boolean isInsideTree(PsiWarning warning, Tree tree) {
      if (WARNING_KINDS.getOrDefault(warning.type(), Set.of()).stream().noneMatch(tree::is)) {
        return false;
      }
      return warning.start().compareTo(startOf(tree)) >= 0
        && warning.end().compareTo(endOf(tree)) <= 0;
    }

    @VisibleForTesting
    static boolean isMorePreciseTree(Tree currentTree, Tree newTree) {
      return startOf(newTree).compareTo(startOf(currentTree)) >= 0
        && endOf(newTree).compareTo(endOf(currentTree)) <= 0;
    }

    @VisibleForTesting
    static boolean matchesTreeExactly(PsiWarning warning) {
      Tree syntaxTree = warning.syntaxTree();
      return warning.start().compareTo(startOf(syntaxTree)) == 0
        && warning.end().compareTo(endOf(syntaxTree)) == 0;
    }
  }

}
