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
package org.sonar.java.semsitter;

/** The collaborators of one {@link JSema}, each created once for it. */
final class SemaParts {
  final SemaAnnotations annotationFacts;
  final Inference inference;
  final FunctionalInference functionalInference;
  final Substitution substitution;
  final LibraryMembers libraryMembers;
  final MemberFactory memberFactory;
  final MemberLookup lookup;
  final OverloadRanking overloads;
  final FactTypes factTypes;

  SemaParts(JSema sema) {
    annotationFacts = new SemaAnnotations(sema);
    inference = new Inference(sema);
    functionalInference = new FunctionalInference(sema);
    substitution = new Substitution(sema);
    libraryMembers = new LibraryMembers(sema);
    memberFactory = new MemberFactory(sema);
    lookup = new MemberLookup(sema);
    overloads = new OverloadRanking(sema);
    factTypes = new FactTypes(sema);
  }
}
