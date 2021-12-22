/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.model;

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
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.location.InternalPosition;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.tree.Tree;

public final class JWarning extends JProblem {
  private final Position start;
  private final Position end;

  private Tree syntaxTree;

  JWarning(String message, Type type, int startLine, int startColumnOffset, int endLine, int endColumnOffset) {
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
    if (!(o instanceof JWarning)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    JWarning jWarning = (JWarning) o;
    // skip syntaxTree
    return start.equals(jWarning.start) && end.equals(jWarning.end);
  }

  @Override
  public int hashCode() {
    // skip syntaxTree
    return Objects.hash(super.hashCode(), start, end);
  }

  public static class Mapper extends SubscriptionVisitor {

    private static final Set<Tree.Kind> KINDS = Stream.of(Type.values())
      .map(Type::getKinds)
      .flatMap(Set::stream)
      .collect(Collectors.toSet());

    private final Map<Type, Set<JWarning>> warningsByType = new EnumMap<>(Type.class);

    private final PriorityQueue<JWarning> warnings = new PriorityQueue<>(Comparator.comparing(JWarning::start).thenComparing(JWarning::end));

    private Mapper(CompilationUnit ast) {
      Stream.of(ast.getProblems())
        .map(problem -> convert(problem, ast))
        .filter(Objects::nonNull)
        .forEach(warnings::add);
    }

    @CheckForNull
    private static JWarning convert(IProblem problem, CompilationUnit root) {
      for (Type type : Type.values()) {
        if (type.matches(problem)) {
          return new JWarning(problem.getMessage(),
            type,
            problem.getSourceLineNumber(),
            root.getColumnNumber(problem.getSourceStart()),
            root.getLineNumber(problem.getSourceEnd()),
            root.getColumnNumber(problem.getSourceEnd()) + 1);
        }
      }
      return null;
    }

    public static Mapper warningsFor(CompilationUnit ast) {
      return new Mapper(ast);
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
      for (Iterator<JWarning> iterator = warnings.iterator(); iterator.hasNext();) {
        JWarning warning = iterator.next();
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
    static void setSyntaxTree(JWarning warning, Tree tree) {
      if (warning.syntaxTree == null || isMorePreciseTree(warning.syntaxTree, tree)) {
        warning.syntaxTree = tree;
      }
    }

    @VisibleForTesting
    static boolean isInsideTree(JWarning warning, Tree tree) {
      if (warning.type().getKinds().stream().noneMatch(tree::is)) {
        // wrong kind
        return false;
      }
      return warning.start().compareTo(tree.firstToken().range().start()) >= 0
        && warning.end().compareTo(tree.lastToken().range().end()) <= 0;
    }

    @VisibleForTesting
    static boolean isMorePreciseTree(Tree currentTree, Tree newTree) {
      return newTree.firstToken().range().start().compareTo(currentTree.firstToken().range().start()) >= 0
        && newTree.lastToken().range().end().compareTo(currentTree.lastToken().range().end()) <= 0;
    }

    @VisibleForTesting
    static boolean matchesTreeExactly(JWarning warning) {
      Tree syntaxTree = warning.syntaxTree();
      return warning.start().compareTo(syntaxTree.firstToken().range().start()) == 0
        && warning.end().compareTo(syntaxTree.lastToken().range().end()) == 0;
    }
  }

}
