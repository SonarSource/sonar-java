/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.regex.ast;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.java.regex.RegexParseResult;

public class RegexBaseVisitor implements RegexVisitor {

  private FlagSet activeFlags;

  @VisibleForTesting
  protected int getActiveFlags() {
    return activeFlags.getMask();
  }

  protected boolean flagActive(int flag) {
    return activeFlags.contains(flag);
  }

  /**
   * Returns the character inside the regex that was used to set the given flag. This will return null if the flag
   * is not set or if the flag has been set from outside of the regex (i.e. as an argument to Pattern.compile).
   * Therefore this should not be used to check whether a flag is set.
   */
  @CheckForNull
  protected JavaCharacter getJavaCharacterForFlag(int flag) {
    return activeFlags.getJavaCharacterForFlag(flag);
  }

  private void visit(RegexTree tree) {
    tree.accept(this);
  }

  private void visit(List<RegexTree> trees) {
    trees.forEach(this::visit);
  }

  @Override
  public void visit(RegexParseResult regexParseResult) {
    if (!regexParseResult.hasSyntaxErrors()) {
      activeFlags = regexParseResult.getInitialFlags();
      visit(regexParseResult.getResult());
      after(regexParseResult);
    }
  }

  /**
   * Override to perform an action after the entire regex has been visited.
   */
  protected void after(RegexParseResult regexParseResult) {
    // does nothing unless overridden
  }

  @Override
  public void visitPlainCharacter(PlainCharacterTree tree) {
    // No children to visit
  }

  @Override
  public void visitSequence(SequenceTree tree) {
    visit(tree.getItems());
  }

  @Override
  public void visitDisjunction(DisjunctionTree tree) {
    visit(tree.getAlternatives());
  }

  @Override
  public void visitCapturingGroup(CapturingGroupTree tree) {
    visitAndRestoreFlags(tree.getElement());
  }

  @Override
  public final void visitNonCapturingGroup(NonCapturingGroupTree tree) {
    FlagSet oldFlags = activeFlags;
    activeFlags = new FlagSet(activeFlags);
    activeFlags.addAll(tree.getEnabledFlags());
    activeFlags.removeAll(tree.getDisabledFlags());
    doVisitNonCapturingGroup(tree);
    if (tree.getElement() != null) {
      activeFlags = oldFlags;
    }
  }

  protected void doVisitNonCapturingGroup(NonCapturingGroupTree tree) {
    RegexTree element = tree.getElement();
    if (element != null) {
      visit(element);
    }
  }

  @Override
  public void visitAtomicGroup(AtomicGroupTree tree) {
    visitAndRestoreFlags(tree.getElement());
  }

  @Override
  public void visitLookAround(LookAroundTree tree) {
    visitAndRestoreFlags(tree.getElement());
  }

  private void visitAndRestoreFlags(RegexTree tree) {
    FlagSet oldFlags = activeFlags;
    activeFlags = new FlagSet(activeFlags);
    visit(tree);
    activeFlags = oldFlags;
  }

  @Override
  public void visitBackReference(BackReferenceTree tree) {
    // no children to visit
  }

  @Override
  public void visitRepetition(RepetitionTree tree) {
    visit(tree.getElement());
  }

  @Override
  public void visitCharacterClass(CharacterClassTree tree) {
    visit(tree.getContents());
  }

  @Override
  public void visitCharacterRange(CharacterRangeTree tree) {
    // No children to visit
  }

  @Override
  public void visitCharacterClassUnion(CharacterClassUnionTree tree) {
    visit(tree.getCharacterClasses());
  }

  @Override
  public void visitCharacterClassIntersection(CharacterClassIntersectionTree tree) {
    visit(tree.getCharacterClasses());
  }

  @Override
  public void visitDot(DotTree tree) {
    // No children to visit
  }

  @Override
  public void visitEscapedProperty(EscapedPropertyTree tree) {
    // No children to visit
  }

  @Override
  public void visitBoundary(BoundaryTree boundaryTree) {
    // no children to visit
  }

}
