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
package org.sonar.java.se.xproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.checks.DivisionByZeroCheck;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Type;

public class MethodBehavior {
  private static final Pattern ANONYMOUS_CLASS_PATTERN = Pattern.compile(".+\\$\\d+");

  private boolean varArgs;
  private final int arity;

  private final Set<MethodYield> yields;
  private final List<SymbolicValue> parameters;
  private final String signature;
  private boolean complete = false;
  private boolean visited = false;
  private List<String> declaredExceptions;

  public MethodBehavior(String signature, boolean varArgs) {
    this.signature = signature;
    this.yields = new LinkedHashSet<>();
    this.parameters = new ArrayList<>();
    this.varArgs = varArgs;
    this.arity = SignatureUtils.numberOfArguments(signature);
    this.declaredExceptions = Collections.emptyList();
  }

  public void addYield(MethodYield methodYield) {
    yields.add(methodYield);
  }

  public void createYield(ExplodedGraph.Node node) {
    boolean expectReturnValue = !(SignatureUtils.isConstructor(signature) || SignatureUtils.isVoidMethod(signature));
    SymbolicValue resultSV = node.programState.exitValue();

    MethodYield methodYield;
    if ((resultSV == null && expectReturnValue) || resultSV instanceof SymbolicValue.ExceptionalSymbolicValue) {
      methodYield = newExceptionalYield(node, resultSV);
    } else {
      methodYield = newHappyPathYield(node, expectReturnValue, resultSV);
    }
    addParameterConstraints(node, methodYield);
    yields.add(methodYield);
  }

  private MethodYield newExceptionalYield(ExplodedGraph.Node nodeForYield, @Nullable SymbolicValue resultSV) {
    ExceptionalYield exceptionalYield = new ExceptionalYield(nodeForYield, this);
    if (resultSV != null) {
      Type type = ((SymbolicValue.ExceptionalSymbolicValue) resultSV).exceptionType();
      String typeName = null;

      if (type != null && ANONYMOUS_CLASS_PATTERN.matcher(type.fullyQualifiedName()).matches()) {
        // anonymous class extending an exception - assume it's the superclass
        type = type.symbol().superClass();
      }

      while (type != null && type.symbol().owner().isMethodSymbol()) {
        // skip anonymous or classes nested in methods to the closest exception type
        // because bytecode visitor does not support them (see org.sonar.java.resolve.BytecodeVisitor.visitOuterClass)
        type = type.symbol().superClass();
      }
      if(type != null) {
        typeName = type.fullyQualifiedName();
      }
      exceptionalYield.setExceptionType(typeName);
    }
    return exceptionalYield;
  }

  private MethodYield newHappyPathYield(ExplodedGraph.Node nodeForYield, boolean expectReturnValue, @Nullable SymbolicValue resultSV) {
    HappyPathYield happyPathYield = new HappyPathYield(nodeForYield, this);
    if (expectReturnValue) {
      ConstraintsByDomain cleanup = cleanup(nodeForYield.programState.getConstraints(resultSV), -1);
      if (cleanup.isEmpty()) {
        cleanup = null;
      }
      happyPathYield.setResult(parameters.indexOf(resultSV), cleanup);
    }
    return happyPathYield;
  }

  private void addParameterConstraints(ExplodedGraph.Node node, MethodYield methodYield) {
    // add the constraints on all the parameters
    int index = 0;
    for (SymbolicValue parameter : parameters) {
      ConstraintsByDomain constraints = node.programState.getConstraints(parameter);
      if (constraints == null) {
        constraints = ConstraintsByDomain.empty();
      } else {
        //cleanup based on signature
        constraints = cleanup(constraints, index);
      }
      methodYield.parametersConstraints.add(constraints);
      index++;
    }
  }

  /**
   * cleanup constraints to remove useless ones.
   *
   * @param constraints known constraints
   * @param argumentIndex the argument index to be cleaned, use -1 for result type
   * @return the cleaned constraints for the given parameter or result type
   */
  private ConstraintsByDomain cleanup(@Nullable ConstraintsByDomain constraints, int argumentIndex) {
    if (constraints == null || constraints.isEmpty()) {
      return ConstraintsByDomain.empty();
    }
    ConstraintsByDomain result = constraints;
    if (SignatureUtils.isBoolean(signature, argumentIndex)) {
      result = result.remove(DivisionByZeroCheck.ZeroConstraint.class);
    } else {
      result = result.remove(BooleanConstraint.class);
    }
    return result;
  }

  public ExceptionalYield createExceptionalCheckBasedYield(SymbolicValue target, ExplodedGraph.Node node, String exceptionType, SECheck check) {
    ExceptionalYield exceptionalYield = new ExceptionalCheckBasedYield(target, exceptionType, check.getClass(), node, this);
    addParameterConstraints(node, exceptionalYield);
    yields.add(exceptionalYield);
    return exceptionalYield;
  }

  public boolean isMethodVarArgs() {
    return varArgs;
  }

  public int methodArity() {
    return arity;
  }

  public List<MethodYield> yields() {
    return Collections.unmodifiableList(new ArrayList<>(yields));
  }

  public Stream<ExceptionalYield> exceptionalPathYields() {
    return yields.stream()
      .filter(ExceptionalYield.class::isInstance)
      .map(ExceptionalYield.class::cast);
  }

  public Stream<HappyPathYield> happyPathYields() {
    return yields.stream()
      .filter(HappyPathYield.class::isInstance)
      .map(HappyPathYield.class::cast);
  }

  public void addParameter(SymbolicValue sv) {
    parameters.add(sv);
  }

  public List<SymbolicValue> parameters() {
    return parameters;
  }

  public boolean isComplete() {
    return complete;
  }

  public void completed() {
    this.complete = true;
    this.visited = true;
    reduceYields();
  }

  private void reduceYields() {
    Set<HappyPathYield> happyPathYields = happyPathYields().filter(y -> y.resultIndex() == -1).collect(Collectors.toCollection(LinkedHashSet::new));
    yields.removeAll(happyPathYields);
    int sizeBeforeReduction;
    Set<HappyPathYield> newYields = happyPathYields;
    do {
      sizeBeforeReduction = newYields.size();
      newYields = reduce(newYields);
    } while (newYields.size() < sizeBeforeReduction);
    yields.addAll(newYields);
  }

  private Set<HappyPathYield> reduce(Set<HappyPathYield> yields) {
    LinkedList<HappyPathYield> yieldsToReduce = new LinkedList<>(yields);
    Set<HappyPathYield> newYields = new LinkedHashSet<>();
    while (!yieldsToReduce.isEmpty()) {
      HappyPathYield yield1 = yieldsToReduce.removeFirst();
      HappyPathYield reduced = null;
      for (ListIterator<HappyPathYield> iterator = yieldsToReduce.listIterator(); iterator.hasNext(); ) {
        HappyPathYield yield2 = iterator.next();
        reduced = reduce(yield1, yield2);
        if (reduced != null) {
          newYields.add(reduced);
          iterator.remove();
          break;
        }
      }
      if (reduced == null) {
        newYields.add(yield1);
      }
    }
    return newYields;
  }

  @CheckForNull
  private HappyPathYield reduce(HappyPathYield yield1, HappyPathYield yield2) {
    Optional<Integer> onlyConstraintDifferenceIndex = getOnlyConstraintDifferenceIndex(yield1, yield2);
    if(!onlyConstraintDifferenceIndex.isPresent()) {
      return null;
    }
    int constraintDifferenceIndex = onlyConstraintDifferenceIndex.get();

    HappyPathYield reducedYield = new HappyPathYield(this);
    reducedYield.parametersConstraints = new ArrayList<>(yield1.parametersConstraints);
    reducedYield.setResult(yield1.resultIndex(), yield1.resultConstraint());

    if (constraintDifferenceIndex == yield1.parametersConstraints.size()) {
      if (isIrreducible(yield1.resultConstraint()) || isIrreducible(yield2.resultConstraint())) {
        return null;
      }
      reducedYield.setResult(-1, null);
    } else {
      reducedYield.parametersConstraints.set(constraintDifferenceIndex, ConstraintsByDomain.empty());
    }
    return reducedYield;
  }

  private static Optional<Integer> getOnlyConstraintDifferenceIndex(HappyPathYield yield1, HappyPathYield yield2) {
    List<ConstraintsByDomain> constraints1 = new ArrayList<>(yield1.parametersConstraints);
    constraints1.add(yield1.resultConstraint());
    List<ConstraintsByDomain> constraints2 = new ArrayList<>(yield2.parametersConstraints);
    constraints2.add(yield2.resultConstraint());
    List<Integer> diff = new ArrayList<>();
    for (int i = 0; i < constraints1.size(); i++) {
      if (!Objects.equals(constraints1.get(i), constraints2.get(i))) {
        diff.add(i);
      }
    }
    if (diff.size() != 1) {
      return Optional.empty();
    }
    return Optional.of(diff.get(0));
  }

  private static boolean isIrreducible(@Nullable ConstraintsByDomain constraints) {
    return constraints != null
        && (constraints.hasConstraint(ObjectConstraint.NULL)
        || constraints.hasConstraint(DivisionByZeroCheck.ZeroConstraint.ZERO));
  }

  public boolean isVisited() {
    return visited;
  }

  public void visited() {
    visited = true;
  }

  public String signature() {
    return signature;
  }

  public void setVarArgs(boolean varArgs) {
    this.varArgs = varArgs;
  }

  public List<String> getDeclaredExceptions() {
    return declaredExceptions;
  }

  public void setDeclaredExceptions(List<String> declaredExceptions) {
    this.declaredExceptions = declaredExceptions;
  }

  @Override
  public String toString() {
    return signature + " [" + yields.size() + " yield(s)]";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    MethodBehavior other = (MethodBehavior) obj;
    return new EqualsBuilder()
      .append(signature, other.signature)
      .append(arity, other.arity)
      .append(varArgs, other.varArgs)
      .append(complete, other.complete)
      .append(visited, other.visited)
      .append(declaredExceptions, other.declaredExceptions)
      .append(yields, other.yields)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(11, 74377)
      .append(signature)
      .append(arity)
      .append(varArgs)
      .append(complete)
      .append(visited)
      .append(declaredExceptions)
      .append(yields)
      .toHashCode();
  }
}
