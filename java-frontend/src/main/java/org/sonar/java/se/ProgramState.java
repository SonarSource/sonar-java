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
package org.sonar.java.se;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.java.collections.PCollections;
import org.sonar.java.collections.PMap;
import org.sonar.java.collections.PStack;
import org.sonar.java.se.checks.CustomUnclosedResourcesCheck;
import org.sonar.java.se.checks.LocksNotUnlockedCheck;
import org.sonar.java.se.checks.StreamConsumedCheck;
import org.sonar.java.se.checks.UnclosedResourcesCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.BinarySymbolicValue;
import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

public class ProgramState {

  private static final Set<Class<? extends Constraint>> NON_DISPOSABLE_CONSTRAINTS = ImmutableSet.of(UnclosedResourcesCheck.ResourceConstraint.class,
    CustomUnclosedResourcesCheck.CustomResourceConstraint.class, LocksNotUnlockedCheck.LockConstraint.class, StreamConsumedCheck.StreamPipelineConstraint.class);
  private Set<RelationalSymbolicValue> knownRelations;

  public static class Pop {

    public final ProgramState state;
    public final List<SymbolicValue> values;
    public final List<SymbolicValueSymbol> valuesAndSymbols;

    public Pop(ProgramState programState, List<SymbolicValueSymbol> result) {
      state = programState;
      values = result.stream().map(SymbolicValueSymbol::symbolicValue).collect(Collectors.toList());
      valuesAndSymbols = result;
    }

  }

  /**
   * This class is used to keep on stack symbolic value together with symbol which was used to evaluate this value.
   * This is later used to store symbols of operands in {@link BinarySymbolicValue} and {@link org.sonar.java.se.symbolicvalues.SymbolicValue.UnarySymbolicValue}
   * so we are able to include references to symbols in reporting {@link FlowComputation}.
   * equals/hashCode is considering only stored symbolic value, so caching of ProgramState doesn't depend on symbols.
   */
  public static class SymbolicValueSymbol {
    final SymbolicValue sv;
    @Nullable
    final Symbol symbol;

    public SymbolicValueSymbol(SymbolicValue sv, @Nullable Symbol symbol) {
      this.sv = sv;
      this.symbol = symbol;
    }

    public SymbolicValue symbolicValue() {
      return sv;
    }

    public Symbol symbol() {
      return symbol;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SymbolicValueSymbol that = (SymbolicValueSymbol) o;
      return Objects.equals(sv, that.sv);
    }

    @Override
    public int hashCode() {
      return Objects.hash(sv);
    }

    @Override
    public String toString() {
      return symbol == null ? sv.toString() : (symbol.toString() + "->" + sv.toString());
    }
  }

  private int hashCode;

  private final int constraintSize;
  public static final ProgramState EMPTY_STATE = new ProgramState(
    PCollections.emptyMap(),
    PCollections.emptyMap(),
    PCollections.<SymbolicValue, ConstraintsByDomain>emptyMap()
      .put(SymbolicValue.NULL_LITERAL, ConstraintsByDomain.empty().put(ObjectConstraint.NULL))
      .put(SymbolicValue.TRUE_LITERAL, ConstraintsByDomain.empty().put(BooleanConstraint.TRUE).put(ObjectConstraint.NOT_NULL))
      .put(SymbolicValue.FALSE_LITERAL, ConstraintsByDomain.empty().put(BooleanConstraint.FALSE).put(ObjectConstraint.NOT_NULL)),
        PCollections.emptyMap(),
    PCollections.emptyStack(),
    null);

  private final PMap<ProgramPoint, Integer> visitedPoints;
  private final PStack<SymbolicValueSymbol> stack;
  private final PMap<SymbolicValue, Integer> references;
  private SymbolicValue exitSymbolicValue;
  final PMap<Symbol, SymbolicValue> values;
  final PMap<Integer, SymbolicValue> valuesByIndex;
  final PMap<SymbolicValue, ConstraintsByDomain> constraints;

  private ProgramState(PMap<Symbol, SymbolicValue> values,PMap<Integer, SymbolicValue> valuesByIndex, PMap<SymbolicValue, Integer> references,
                       PMap<SymbolicValue, ConstraintsByDomain> constraints, PMap<ProgramPoint, Integer> visitedPoints,
                       PStack<SymbolicValueSymbol> stack, SymbolicValue exitSymbolicValue) {
    this.values = values;
    this.valuesByIndex = valuesByIndex;
    this.references = references;
    this.constraints = constraints;
    this.visitedPoints = visitedPoints;
    this.stack = stack;
    this.exitSymbolicValue = exitSymbolicValue;
    constraintSize = 3;
  }

  private ProgramState(PMap<Symbol, SymbolicValue> values, PMap<SymbolicValue, Integer> references,
                       PMap<SymbolicValue, ConstraintsByDomain> constraints, PMap<ProgramPoint, Integer> visitedPoints,
                       PStack<SymbolicValueSymbol> stack, SymbolicValue exitSymbolicValue) {
    this.values = values;
    this.valuesByIndex = PCollections.emptyMap();
    this.references = references;
    this.constraints = constraints;
    this.visitedPoints = visitedPoints;
    this.stack = stack;
    this.exitSymbolicValue = exitSymbolicValue;
    constraintSize = 3;
  }

  private ProgramState(ProgramState ps, PStack<SymbolicValueSymbol> newStack) {
    values = ps.values;
    valuesByIndex = ps.valuesByIndex;
    references = ps.references;
    constraints = ps.constraints;
    constraintSize = ps.constraintSize;
    visitedPoints = ps.visitedPoints;
    exitSymbolicValue = ps.exitSymbolicValue;
    stack = newStack;
  }

  private ProgramState(ProgramState ps, PMap<SymbolicValue, ConstraintsByDomain> newConstraints) {
    values = ps.values;
    valuesByIndex = ps.valuesByIndex;
    references = ps.references;
    constraints = newConstraints;
    constraintSize = ps.constraintSize + 1;
    visitedPoints = ps.visitedPoints;
    exitSymbolicValue = ps.exitSymbolicValue;
    this.stack = ps.stack;
  }

  public ProgramState stackValue(SymbolicValue sv) {
    return new ProgramState(this, stack.push(new SymbolicValueSymbol(sv, null)));
  }

  public ProgramState stackValue(SymbolicValue sv, @Nullable Symbol symbol) {
    return new ProgramState(this, stack.push(new SymbolicValueSymbol(sv, symbol)));
  }

  ProgramState clearStack() {
    return stack.isEmpty() ? this : new ProgramState(this, PCollections.emptyStack());
  }

  public Pop unstackValue(int nbElements) {
    if (nbElements == 0 || stack.isEmpty()) {
      return new Pop(this, Collections.emptyList());
    }

    // FIXME can be made more efficient by reusing sub collection of PStack instead of copying to the new list
    PStack<SymbolicValueSymbol> newStack = stack;
    List<SymbolicValueSymbol> result = new ArrayList<>();
    for (int i = 0; i < nbElements && !newStack.isEmpty(); i++) {
      result.add(newStack.peek());
      newStack = newStack.pop();
    }
    return new Pop(new ProgramState(this, newStack), result);
  }

  @CheckForNull
  public SymbolicValue peekValue() {
    return stack.isEmpty() ? null : stack.peek().sv;
  }

  public SymbolicValueSymbol peekValueSymbol() {
    return stack.peek();
  }

  public SymbolicValue peekValue(int i) {
    return stack.peek(i).sv;
  }

  public List<SymbolicValue> peekValues(int n) {
    return peekValuesAndSymbols(n).stream().map(SymbolicValueSymbol::symbolicValue).collect(Collectors.toList());
  }

  public List<SymbolicValueSymbol> peekValuesAndSymbols(int n) {
    ImmutableList.Builder<SymbolicValueSymbol> result = ImmutableList.builder();
    PStack<SymbolicValueSymbol> tmpStack = this.stack;
    for (int i = 0; i < n; i++) {
      result.add(tmpStack.peek());
      tmpStack = tmpStack.pop();
    }
    return result.build();
  }

  public int numberOfTimeVisited(ProgramPoint programPoint) {
    Integer count = visitedPoints.get(programPoint);
    return count == null ? 0 : count;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProgramState that = (ProgramState) o;
    return Objects.equals(values, that.values) &&
      Objects.equals(constraints, that.constraints) &&
      Objects.equals(exitSymbolicValue, that.exitSymbolicValue) &&
      Objects.equals(stack, that.stack);
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      hashCode = Objects.hash(values, constraints, peekValue());
    }
    return hashCode;
  }

  @Override
  public String toString() {
    return "{" + values.toString() + "}  {" + constraints.toString() + "}" + " { " + stack.toString() + " }";
  }

  public ProgramState addConstraintTransitively(SymbolicValue symbolicValue, Constraint constraint) {
    List<SymbolicValue> transitiveSymbolicValues = knownRelations().stream()
      .filter(rsv -> rsv.isEquality() && (rsv.getLeftOp() == symbolicValue || rsv.getRightOp() == symbolicValue))
      .map(rsv -> rsv.getLeftOp() == symbolicValue ? rsv.getRightOp() : rsv.getLeftOp())
      .collect(Collectors.toList());
    ProgramState ps = addConstraint(symbolicValue, constraint);
    for (SymbolicValue sv : transitiveSymbolicValues) {
      ps = ps.addConstraint(sv, constraint);
    }
    return ps;
  }

  public Set<RelationalSymbolicValue> knownRelations() {
    if(knownRelations == null) {
      knownRelations = Collections.unmodifiableSet(getValuesWithConstraints(BooleanConstraint.TRUE)
        .stream()
        .filter(RelationalSymbolicValue.class::isInstance)
        .map(RelationalSymbolicValue.class::cast)
        .collect(Collectors.toSet()));
    }
    return knownRelations;
  }

  public ProgramState addConstraint(SymbolicValue symbolicValue, Constraint constraint) {
    Preconditions.checkState(!(symbolicValue instanceof RelationalSymbolicValue && constraint == BooleanConstraint.FALSE),
      "Relations stored in PS should always use TRUE constraint. SV: %s", symbolicValue);
    ConstraintsByDomain constraintsForSV = constraints.get(symbolicValue);
    if(constraintsForSV == null) {
      constraintsForSV = ConstraintsByDomain.empty();
    }
    return addConstraints(symbolicValue, constraintsForSV.put(constraint));
  }

  public ProgramState addConstraints(SymbolicValue symbolicValue, ConstraintsByDomain constraintsForSV) {
    PMap<SymbolicValue, ConstraintsByDomain> newConstraints = constraints.put(symbolicValue, constraintsForSV);
    if (newConstraints != constraints) {
      return new ProgramState(this, newConstraints);
    }
    return this;
  }

  public ProgramState removeConstraintsOnDomain(SymbolicValue sv, Class<? extends Constraint> domain) {
    ConstraintsByDomain svConstraint = constraints.get(sv);
    if(svConstraint == null) {
      return this;
    }
    ConstraintsByDomain newConstraintForSv = svConstraint.remove(domain);
    if(newConstraintForSv.isEmpty()) {
      return new ProgramState(this, constraints.remove(sv));
    }
    return addConstraints(sv, newConstraintForSv);
  }

  @VisibleForTesting
  public ProgramState put(int index, SymbolicValue value) {
    SymbolicValue sv = valuesByIndex.get(index);
    if(sv == null || sv != value) {
      return new ProgramState(values, valuesByIndex.put(index, value), references, constraints, visitedPoints, stack, exitSymbolicValue);
    }
    return this;
  }

  @VisibleForTesting
  public ProgramState put(Symbol symbol, SymbolicValue value) {
    if (symbol.isUnknown() || isVolatileField(symbol)) {
      return this;
    }
    SymbolicValue oldValue = values.get(symbol);
    if (oldValue == null || oldValue != value) {
      PMap<SymbolicValue, Integer> newReferences = references;
      if (oldValue != null) {
        newReferences = decreaseReference(newReferences, oldValue);
      }
      newReferences = increaseReference(newReferences, value);
      PMap<Symbol, SymbolicValue> newValues = values.put(symbol, value);
      return new ProgramState(newValues, newReferences, constraints, visitedPoints, stack, exitSymbolicValue);
    }
    return this;
  }

  private static boolean isVolatileField(Symbol symbol) {
    return isField(symbol) && symbol.isVolatile();
  }

  private static PMap<SymbolicValue, Integer> decreaseReference(PMap<SymbolicValue, Integer> givenReferences, SymbolicValue sv) {
    Integer value = givenReferences.get(sv);
    Objects.requireNonNull(value);
    return givenReferences.put(sv, value - 1);
  }

  private static PMap<SymbolicValue, Integer> increaseReference(PMap<SymbolicValue, Integer> givenReferences, SymbolicValue sv) {
    Integer value = givenReferences.get(sv);
    if (value == null) {
      return givenReferences.put(sv, 1);
    } else {
      return givenReferences.put(sv, value + 1);
    }
  }

  private static boolean isDisposable(SymbolicValue symbolicValue, @Nullable Constraint constraint) {
    //FIXME this should be handle with callbacks rather than keeping those value in programstate
    return SymbolicValue.isDisposable(symbolicValue) && (constraint == null || !NON_DISPOSABLE_CONSTRAINTS.contains(constraint.getClass()));
  }

  private static boolean isDisposable(SymbolicValue symbolicValue, @Nullable ConstraintsByDomain constraints) {
    //FIXME this should be handle with callbacks rather than keeping those value in programstate
    return SymbolicValue.isDisposable(symbolicValue) &&
      (constraints == null || NON_DISPOSABLE_CONSTRAINTS.stream().map(constraints::get).allMatch(Objects::isNull));
  }

  private static boolean inStack(PStack<SymbolicValueSymbol> stack, SymbolicValue symbolicValue) {
    return stack.anyMatch(valueSymbol -> valueSymbol.sv.equals(symbolicValue) || valueSymbol.sv.references(symbolicValue));
  }

  private static boolean isLocalVariable(Symbol symbol) {
    return symbol.isVariableSymbol() && symbol.owner().isMethodSymbol();
  }

  public ProgramState cleanupDeadSymbols(Set<Symbol> liveVariables, Collection<SymbolicValue> protectedSymbolicValues) {
    class CleanAction implements BiConsumer<Symbol, SymbolicValue> {
      boolean newProgramState = false;
      PMap<Symbol, SymbolicValue> newValues = values;
      PMap<SymbolicValue, Integer> newReferences = references;
      PMap<SymbolicValue, ConstraintsByDomain> newConstraints = constraints;

      @Override
      public void accept(Symbol symbol, SymbolicValue symbolicValue) {
        if (isLocalVariable(symbol) && !liveVariables.contains(symbol) && !protectedSymbolicValues.contains(symbolicValue)) {
          newProgramState = true;
          newValues = newValues.remove(symbol);
          newReferences = decreaseReference(newReferences, symbolicValue);
          if (!isReachable(symbolicValue, newReferences) && isDisposable(symbolicValue, newConstraints.get(symbolicValue)) && !inStack(stack, symbolicValue)) {
            newConstraints = newConstraints.remove(symbolicValue);
            newReferences = newReferences.remove(symbolicValue);
          }
        }
      }
    }
    CleanAction cleanAction = new CleanAction();
    values.forEach(cleanAction);
    return cleanAction.newProgramState ? new ProgramState(cleanAction.newValues, cleanAction.newReferences, cleanAction.newConstraints, visitedPoints, stack, exitSymbolicValue
    )
      : this;
  }

  public ProgramState cleanupConstraints(Collection<SymbolicValue> protectedSymbolicValues) {
    class CleanAction implements BiConsumer<SymbolicValue, ConstraintsByDomain> {
      boolean newProgramState = false;
      PMap<SymbolicValue, ConstraintsByDomain> newConstraints = constraints;
      PMap<SymbolicValue, Integer> newReferences = references;

      @Override
      public void accept(SymbolicValue symbolicValue, ConstraintsByDomain constraintPMap) {
        constraintPMap.forEach((domain, constraint) -> {
          if (!protectedSymbolicValues.contains(symbolicValue)
            && !isReachable(symbolicValue, newReferences)
            && isDisposable(symbolicValue, constraint)
            && !inStack(stack, symbolicValue)) {
            newProgramState = true;
            ConstraintsByDomain removed = newConstraints.get(symbolicValue).remove(domain);
            if (removed.isEmpty()) {
              newConstraints = newConstraints.remove(symbolicValue);
            } else {
              newConstraints = newConstraints.put(symbolicValue, removed);
            }
            newReferences = newReferences.remove(symbolicValue);
          }
        });
      }
    }
    CleanAction cleanAction = new CleanAction();
    constraints.forEach(cleanAction);
    return cleanAction.newProgramState ? new ProgramState(values, cleanAction.newReferences, cleanAction.newConstraints, visitedPoints, stack, exitSymbolicValue
    ) : this;
  }

  ProgramState resetFieldValues(ConstraintManager constraintManager, boolean resetOnlyStaticFields) {
    List<Symbol> fields = new ArrayList<>();
    values.forEach((symbol, symbolicValue) -> {
      if (isField(symbol) && !symbol.isFinal() && (symbol.isStatic() || !resetOnlyStaticFields) ) {
        fields.add(symbol);
      }
    });
    ProgramState newProgramState = this;
    for (Symbol field : fields) {
      newProgramState = newProgramState.put(field, constraintManager.createDefaultSymbolicValue());
    }
    return newProgramState;
  }

  public static boolean isField(Symbol symbol) {
    return symbol.isVariableSymbol() && !symbol.owner().isMethodSymbol();
  }

  private static boolean isReachable(SymbolicValue symbolicValue, PMap<SymbolicValue, Integer> references) {
    Integer integer = references.get(symbolicValue);
    return integer != null && integer > 0;
  }

  public boolean canReach(SymbolicValue symbolicValue) {
    return isReachable(symbolicValue, references);
  }

  public ProgramState visitedPoint(ProgramPoint programPoint, int nbOfVisit) {
    return new ProgramState(values, valuesByIndex, references, constraints, visitedPoints.put(programPoint, nbOfVisit), stack, exitSymbolicValue);
  }

  @Nullable
  public ConstraintsByDomain getConstraints(SymbolicValue sv) {
    return constraints.get(sv);
  }

  @CheckForNull
  public <T extends Constraint> T getConstraint(SymbolicValue sv, Class<T> domain) {
    ConstraintsByDomain classConstraintPMap = constraints.get(sv);
    if(classConstraintPMap == null) {
      return null;
    }
    return (T) classConstraintPMap.get(domain);
  }

  public int constraintsSize() {
    return constraintSize;
  }

  @CheckForNull
  public SymbolicValue getValue(int index) {
    return valuesByIndex.get(index);
  }

  @CheckForNull
  public SymbolicValue getValue(Symbol symbol) {
    return values.get(symbol);
  }

  public List<SymbolicValue> getValuesWithConstraints(final Constraint constraint) {
    final List<SymbolicValue> result = new ArrayList<>();
    constraints.forEach((symbolicValue, constraintByDomain) -> {
      Constraint find = constraintByDomain.get(constraint.getClass());
      if(constraint.equals(find)) {
        result.add(symbolicValue);
      }
    });
    return result;
  }

  public void storeExitValue() {
    this.exitSymbolicValue = peekValue();
  }

  @CheckForNull
  public SymbolicValue exitValue() {
    return this.exitSymbolicValue;
  }

  public boolean exitingOnRuntimeException() {
    if (exitSymbolicValue instanceof SymbolicValue.ExceptionalSymbolicValue) {
      Type exceptionType = ((SymbolicValue.ExceptionalSymbolicValue) exitSymbolicValue).exceptionType();
      return exceptionType == null || exceptionType.isSubtypeOf("java.lang.RuntimeException");
    }
    return false;
  }

  Set<LearnedConstraint> learnedConstraints(ProgramState parent) {
    ImmutableSet.Builder<LearnedConstraint> result = ImmutableSet.builder();
    constraints.forEach((sv, pmap) -> pmap.forEach((domain, c) -> {
      if (!c.equals(parent.getConstraint(sv, domain))) {
        result.add(new LearnedConstraint(sv, c));
      }
    }));
    return result.build();
  }

  Set<LearnedAssociation> learnedAssociations(ProgramState parent) {
    ImmutableSet.Builder<LearnedAssociation> result = ImmutableSet.builder();
    values.forEach((s, sv) -> {
      if (parent.getValue(s) != sv) {
        result.add(new LearnedAssociation(sv, s));
      }
    });
    return result.build();
  }
}
