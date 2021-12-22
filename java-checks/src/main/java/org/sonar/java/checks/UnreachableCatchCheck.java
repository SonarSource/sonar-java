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
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.model.JProblem;
import org.sonar.java.model.JWarning;
import org.sonar.java.model.JavaTree.CompilationUnitTreeImpl;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext.Location;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;

@Rule(key = "S4970")
public class UnreachableCatchCheck extends IssuableSubscriptionVisitor {

  private final List<JWarning> warnings = new ArrayList<>();
  private static final Comparator<Location> LOCATION_COMPARATOR = Comparator.comparing(loc -> loc.syntaxNode.firstToken().range().start());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.COMPILATION_UNIT, Tree.Kind.TRY_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      warnings.clear();
      warnings.addAll(((CompilationUnitTreeImpl) tree).warnings(JProblem.Type.MASKED_CATCH));
      return;
    }
    checkWarnings((TryStatementTree) tree);
  }

  private void checkWarnings(TryStatementTree tryStatementTree) {
    List<TypeTree> typesWithWarnings = new ArrayList<>(warnings.size());
    List<UnionTypeTree> unionTypes = new ArrayList<>();
    Map<TypeTree, Type> typeByExceptions = new HashMap<>();
    Map<TypeTree, Tree> reportTrees = new HashMap<>();

    for (CatchTree catchTree : tryStatementTree.catches()) {
      SyntaxToken catchKeyword = catchTree.catchKeyword();
      TypeTree caughtException = catchTree.parameter().type();

      boolean withinUnionType = caughtException.is(Tree.Kind.UNION_TYPE);
      if (withinUnionType) {
        // in case we need to report on the entire union type
        reportTrees.put(caughtException, catchKeyword);
        unionTypes.add((UnionTypeTree) caughtException);
      }

      for (TypeTree typeTree : caughtExceptionTypes(caughtException)) {
        typeByExceptions.put(typeTree, typeTree.symbolType());
        // types from union types should be reported individually
        reportTrees.put(typeTree, withinUnionType ? typeTree : catchKeyword);

        warnings.stream()
          .filter(warning -> typeTree.equals(warning.syntaxTree()))
          .forEach(warning -> typesWithWarnings.add(typeTree));
      }
    }
    reportUnreacheableCatch(typesWithWarnings, typeByExceptions, reportTrees, unionTypes);
  }

  private static List<TypeTree> caughtExceptionTypes(TypeTree caughtException) {
    if (caughtException.is(Tree.Kind.UNION_TYPE)) {
      return ((UnionTypeTree) caughtException).typeAlternatives();
    }
    return Collections.singletonList(caughtException);
  }

  private void reportUnreacheableCatch(List<TypeTree> typesWithWarnings, Map<TypeTree, Type> typeByExceptions, Map<TypeTree, Tree> reportTrees, List<UnionTypeTree> unionTypes) {
    for (TypeTree type : typesToReport(typesWithWarnings, unionTypes)) {
      reportIssue(reportTrees.get(type), message(reportTrees.get(type)), secondaries(type, typeByExceptions), null);
    }
  }

  private static List<TypeTree> typesToReport(List<TypeTree> typesWithWarnings, List<UnionTypeTree> unionTypes) {
    List<TypeTree> typesToReport = new ArrayList<>(typesWithWarnings);
    for (UnionTypeTree unionType : unionTypes) {
      List<TypeTree> typeAlternatives = unionType.typeAlternatives();
      if (typesWithWarnings.containsAll(typeAlternatives)) {
        typesToReport.add(unionType);
        typesToReport.removeAll(typeAlternatives);
      }
    }
    return typesToReport;
  }

  private static String message(Tree reportTree) {
    if (reportTree.is(Tree.Kind.TOKEN)) {
      return "Remove or refactor this catch clause because it is unreachable, hidden by previous catch block(s).";
    }
    // reporting on the type alternative of an union type
    return "Remove this type because it is unreachable, hidden by previous catch block(s).";
  }

  private static List<Location> secondaries(TypeTree type, Map<TypeTree, Type> typeByExceptions) {
    List<Type> targets;
    if (type.is(Tree.Kind.UNION_TYPE)) {
      targets = ((UnionTypeTree) type).typeAlternatives()
        .stream()
        .map(typeByExceptions::get)
        .collect(Collectors.toList());
    } else {
      targets = Collections.singletonList(typeByExceptions.get(type));
    }
    return childrenExceptionTypes(targets, typeByExceptions);
  }

  private static List<Location> childrenExceptionTypes(List<Type> targets, Map<TypeTree, Type> exceptionTypes) {
    List<Location> secondaries = new ArrayList<>();
    targets.forEach(target -> {
      for (Map.Entry<TypeTree, Type> exceptionType : exceptionTypes.entrySet()) {
        TypeTree tree = exceptionType.getKey();
        Type type = exceptionType.getValue();
        if (!type.equals(target) && type.isSubtypeOf(target)) {
          secondaries.add(new Location("Already catch the exception", tree));
        }
      }
    });
    secondaries.sort(LOCATION_COMPARATOR);
    return secondaries;
  }
}
