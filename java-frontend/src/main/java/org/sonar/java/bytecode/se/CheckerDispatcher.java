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
package org.sonar.java.bytecode.se;

import org.sonar.java.bytecode.cfg.Instruction;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.MethodYield;

import javax.annotation.Nullable;

import java.util.List;

public class CheckerDispatcher {
  private final BytecodeEGWalker explodedGraphWalker;
  private final List<BytecodeSECheck> checks;
  Instruction currentInstruction;
  // used by walker to store chosen yield when adding a transition from MIT
  @Nullable
  MethodYield methodYield = null;
  private int currentCheckerIndex = -1;
  private boolean transition = false;

  public CheckerDispatcher(BytecodeEGWalker explodedGraphWalker, List<BytecodeSECheck> checks) {
    this.explodedGraphWalker = explodedGraphWalker;
    this.checks = checks;
  }

  public boolean executeCheckPreStatement(Instruction instruction) {
    this.currentInstruction = instruction;
    ProgramState ps;
    for (BytecodeSECheck checker : checks) {
      ps = checker.checkPreStatement(this, instruction);
      if (ps == null) {
        return false;
      }
      explodedGraphWalker.programState = ps;
    }
    return true;
  }

  public void executeCheckPostStatement(Instruction instruction) {
    this.currentInstruction = instruction;
    addTransition(explodedGraphWalker.programState);
  }

  public void createSink() {
    transition = true;
  }

  public void addTransition(ProgramState state) {
    ProgramState oldState = explodedGraphWalker.programState;
    explodedGraphWalker.programState = state;
    currentCheckerIndex++;
    executePost();
    currentCheckerIndex--;
    explodedGraphWalker.programState = oldState;
    this.transition = true;
  }

  private void executePost() {
    this.transition = false;
    if (currentCheckerIndex < checks.size()) {
      explodedGraphWalker.programState = checks.get(currentCheckerIndex).checkPostStatement(this, currentInstruction);
      if (explodedGraphWalker.programState == null) {
        // one of the check interrupted exploration
        return;
      }
    } else {
      explodedGraphWalker.enqueue(explodedGraphWalker.programPosition.next(), explodedGraphWalker.programState);
      return;
    }
    if (!transition) {
      addTransition(explodedGraphWalker.programState);
    }
  }

  public void addExceptionalYield(SymbolicValue target, ProgramState exceptionalState, String exceptionFullyQualifiedName, SECheck check) {
    // TODO ?
  }

  public ProgramState getState() {
    return explodedGraphWalker.programState;
  }

  public ExplodedGraph.Node getNode() {
    return explodedGraphWalker.node;
  }
}
