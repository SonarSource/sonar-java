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

public class RegexBaseVisitor implements RegexVisitor {

  private int activeFlags = 0;

  @Override
  public void setActiveFlags(int activeFlags) {
    this.activeFlags = activeFlags;
  }

  protected int getActiveFlags() {
    return activeFlags;
  }

  protected boolean flagActive(int flag) {
    return (activeFlags & flag) != 0;
  }

  @Override
  public void visitPlainCharacter(PlainCharacterTree tree) {
    // No children to visit
  }

  @Override
  public void visitSequence(SequenceTree tree) {
    for (RegexTree item : tree.getItems()) {
      visit(item);
    }
  }

  @Override
  public void visitDisjunction(DisjunctionTree tree) {
    for (RegexTree alternative : tree.getAlternatives()) {
      visit(alternative);
    }
  }

  @Override
  public void visitCapturingGroup(CapturingGroupTree tree) {
    visit(tree.getElement());
  }

  @Override
  public final void visitNonCapturingGroup(NonCapturingGroupTree tree) {
    int oldFlags = activeFlags;
    activeFlags ^= tree.getEnabledFlags();
    activeFlags ^= ~tree.getDisabledFlags();
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
    visit(tree.getElement());
  }

  @Override
  public void visitLookAround(LookAroundTree tree) {
    visit(tree.getElement());
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
    for (RegexTree child : tree.getCharacterClasses()) {
      visit(child);
    }
  }

  @Override
  public void visitCharacterClassIntersection(CharacterClassIntersectionTree tree) {
    for (RegexTree child : tree.getCharacterClasses()) {
      visit(child);
    }
  }

  @Override
  public void visitDot(DotTree tree) {
    // No children to visit
  }

  @Override
  public void visitEscapedProperty(EscapedPropertyTree tree) {
    // No children to visit
  }

}
