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

import org.sonar.java.regex.RegexParseResult;

public interface RegexVisitor {

  void visit(RegexParseResult regexParseResult);

  void visitBackReference(BackReferenceTree tree);

  void visitPlainCharacter(PlainCharacterTree tree);

  void visitSequence(SequenceTree tree);

  void visitDisjunction(DisjunctionTree tree);

  void visitCapturingGroup(CapturingGroupTree tree);

  void visitNonCapturingGroup(NonCapturingGroupTree tree);

  void visitAtomicGroup(AtomicGroupTree tree);

  void visitLookAround(LookAroundTree tree);

  void visitRepetition(RepetitionTree tree);

  void visitCharacterClass(CharacterClassTree tree);

  void visitCharacterRange(CharacterRangeTree tree);

  void visitCharacterClassUnion(CharacterClassUnionTree tree);

  void visitCharacterClassIntersection(CharacterClassIntersectionTree tree);

  void visitDot(DotTree tree);

  void visitEscapedProperty(EscapedPropertyTree tree);

  void visitBoundary(BoundaryTree boundaryTree);

}
