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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JLabelSymbol;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Tree;

/** State shared by the parts that translate one document. */
final class TranslationState {

  final JSema sema;
  final Map<org.sonarsource.semsitter.wire.Node, InternalSyntaxToken> tokens = new IdentityHashMap<>();
  final Deque<JLabelSymbol> labels = new ArrayDeque<>();
  final Deque<Symbol.TypeSymbol> types = new ArrayDeque<>();
  InternalSyntaxToken eof = new InternalSyntaxToken(1, 0, "", List.of(), true);
  boolean switchExpression;
  String implicitClassName = "";
  String packageName = "";
  boolean naming;
  final FileImports imports = new FileImports();
  final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();
  final Deque<Type> returnTargets = new ArrayDeque<>();
  final Set<Tree> rawResults = new HashSet<>();

  /** Whether {@code _} declares an unnamed variable (Java 22 and later) instead of a plain name. */
  boolean unnamedVariables = true;

  boolean records = true;

  /** Whether consecutive {@code ///} lines form one Markdown comment (Java 23 and later). */
  boolean markdownComments = true;

  /** Work that needs declarations met later in the file. */
  final List<Runnable> deferred = new ArrayList<>();

  final TranslatorParts parts;

  TranslationState(JSema sema) {
    this.sema = sema;
    parts = new TranslatorParts(this);
  }
}
