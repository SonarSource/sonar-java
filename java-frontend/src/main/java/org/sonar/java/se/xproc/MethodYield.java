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
package org.sonar.java.se.xproc;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sonar.java.collections.PMap;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.Flow;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class MethodYield {
  final ExplodedGraph.Node node;
  private final Map<String, Map<String, Set<Flow>>> cachedFlows = new HashMap<>();
  private final MethodBehavior behavior;
  List<ConstraintsByDomain> parametersConstraints;

  public MethodYield(MethodBehavior behavior) {
    this.parametersConstraints = new ArrayList<>();
    this.node = null;
    this.behavior = behavior;
  }

  public MethodYield(ExplodedGraph.Node node, MethodBehavior behavior) {
    this.parametersConstraints = new ArrayList<>();
    this.node = node;
    this.behavior = behavior;
  }

  public abstract Stream<ProgramState> statesAfterInvocation(List<SymbolicValue> invocationArguments, List<Type> invocationTypes, ProgramState programState,
    Supplier<SymbolicValue> svSupplier);

  public Stream<ProgramState> parametersAfterInvocation(List<SymbolicValue> invocationArguments, List<Type> invocationTypes, ProgramState programState) {
    Set<ProgramState> results = new LinkedHashSet<>();
    for (int index = 0; index < invocationArguments.size(); index++) {
      ConstraintsByDomain constraints = getConstraint(index, invocationTypes);
      if (constraints == null) {
        // no constraints on this parameter, let's try next one.
        continue;
      }

      SymbolicValue invokedArg = invocationArguments.get(index);
      Set<ProgramState> programStates = programStatesForConstraint(results.isEmpty() ? Lists.newArrayList(programState) : results, invokedArg, constraints);
      if (programStates.isEmpty()) {
        // constraints can't be satisfied, no need to process things further, this yield is not applicable.
        // TODO there might be some issue to report in this case.
        return Stream.empty();
      }
      results = programStates;
    }

    // resulting program states can be empty if all constraints on params are null or if method has no arguments.
    // That means that this yield is still possible and we need to stack a returned SV with its eventual constraints.
    if(results.isEmpty()) {
      results.add(programState);
    }
    return results.stream();
  }

  @CheckForNull
  private ConstraintsByDomain getConstraint(int index, List<Type> invocationTypes) {
    if (!behavior.isMethodVarArgs() || applicableOnVarArgs(index, invocationTypes)) {
      return parametersConstraints.get(index);
    }
    return null;
  }

  /**
   * For varArgs methods, only apply the constraint on single array parameter, in order to not 
   * wrongly apply it on all the elements of the array.
   */
  private boolean applicableOnVarArgs(int index, List<Type> types) {
    if (index < parametersConstraints.size() - 1) {
      // not the varArg argument
      return true;
    }
    if (parametersConstraints.size() != types.size()) {
      // more than one element in the variadic part
      return false;
    }
    Type argumentType = types.get(index);
    return argumentType.isArray() || argumentType.is("<nulltype>");
  }

  private static Set<ProgramState> programStatesForConstraint(Collection<ProgramState> states, SymbolicValue invokedArg, ConstraintsByDomain constraints) {
    Set<ProgramState> programStates = new LinkedHashSet<>(states);

    constraints.forEach((d, c) ->  {
      Set<ProgramState> newPs = new LinkedHashSet<>();
      for (ProgramState programState : programStates) {
        newPs.addAll(invokedArg.setConstraint(programState, c));
      }
      programStates.clear();
      programStates.addAll(newPs);
    });
    return programStates;
  }

  public boolean generatedByCheck(SECheck check) {
    return false;
  }

  public MethodBehavior methodBehavior() {
    return behavior;
  }

  @Override
  public abstract String toString();

  @Override
  public int hashCode() {
    return new HashCodeBuilder(7, 1291)
      .append(parametersConstraints)
      .hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    MethodYield other = (MethodYield) obj;
    return new EqualsBuilder()
      .append(parametersConstraints,
        other.parametersConstraints)
      .isEquals();
  }

  static Stream<Constraint> pmapToStream(@Nullable  PMap<Class<? extends Constraint>, Constraint> pmap) {
    if(pmap == null) {
      return Stream.empty();
    }
    Stream.Builder<Constraint> result = Stream.builder();
    pmap.forEach((d, c) -> result.add(c));
    return result.build();
  }

  public Set<Flow> flow(List<Integer> parameterIndices, List<Class<? extends Constraint>> domains) {
    Preconditions.checkArgument(!parameterIndices.isEmpty(), "computing flow on empty symbolic value list should never happen");
    if(node == null || behavior == null) {
      return Collections.emptySet();
    }
    String key = parameterIndices.stream().sorted().map(Object::toString).collect(Collectors.joining(","));
    String domainKey = domains.stream().map(Class::getName).sorted().reduce("", String::concat);

    Map<String, Set<Flow>> flowByDomain = cachedFlows.computeIfAbsent(key, k -> new HashMap<>());
    return flowByDomain.computeIfAbsent(domainKey,
      k -> {
        Set<SymbolicValue> symbolicValues = getSymbolicValues(parameterIndices);
        Set<Symbol> trackedSymbols = getReturnSymbolAsTrackedSymbols(parameterIndices);
        return FlowComputation.flow(node, symbolicValues, c -> true, c -> false, domains, trackedSymbols);
      });
  }

  private Set<Symbol> getReturnSymbolAsTrackedSymbols(List<Integer> parameterIndices) {
    if (parameterIndices.contains(-1) && node.programState.peekValue() == node.programState.exitValue()) {
      Symbol symbol = node.programState.peekValueSymbol().symbol();
      if (symbol != null) {
        return Collections.singleton(symbol);
      }
    }
    return Collections.emptySet();
  }

  private Set<SymbolicValue> getSymbolicValues(List<Integer> parameterIndices) {
    ImmutableSet.Builder<SymbolicValue> parameterSVs = ImmutableSet.builder();
    parameterIndices.stream()
      // Ignore last parameter(s) of a variadic method
      .filter(i -> !behavior.isMethodVarArgs() || i < behavior.methodArity() -1)
      .forEach(parameterIndex ->  {
      if (parameterIndex == -1) {
        parameterSVs.add(node.programState.exitValue());
      } else {
        parameterSVs.add(behavior.parameters().get(parameterIndex));
      }
    });
    return parameterSVs.build();
  }
}
