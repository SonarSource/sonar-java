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

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import org.sonar.java.model.JParseResult;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.semsitter.wire.Document;
import org.sonarsource.semsitter.wire.Node;
import org.sonarsource.semsitter.wire.Ref;

/** Recorded replay documents, changed in place to reach the shapes and answers the recordings do not have. */
final class TranslationStateFixtures {

  private static JsonObject types;
  private static JsonObject packages;

  private TranslationStateFixtures() {
  }

  static Document recorded(String sample) {
    try {
      return Replays.document(sample);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static CompilationUnitTree translate(String sample) {
    return translate(recorded(sample), Replays.javaVersion(sample));
  }

  /** Translates a document with the type declarations and package answers recorded for all the replay samples. */
  static CompilationUnitTree translate(Document document, int javaVersion) {
    loadRecordings();
    SessionTypes session = new SessionTypes(names -> {
      JsonObject answer = new JsonObject();
      names.stream().filter(types::has).forEach(name -> answer.add(name, types.get(name)));
      return answer;
    });
    JParseResult result = SemSitterTranslator.translate(document, session::get, TranslationStateFixtures::hasPackage, true, javaVersion);
    try {
      return result.get();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /** The document with {@code change} applied to every node of its tree, children first. */
  static Document mutate(Document document, UnaryOperator<Node> change) {
    return document.toBuilder().setTree(mutate(document.getTree(), change)).build();
  }

  private static Node mutate(Node node, UnaryOperator<Node> change) {
    Node.Builder builder = node.toBuilder().clearChildren();
    node.getChildrenList().forEach(child -> builder.addChildren(mutate(child, change)));
    return change.apply(builder.build());
  }

  /** Applies {@code change} to the nodes {@code selected} picks, and leaves the other nodes as they are. */
  static UnaryOperator<Node> where(Predicate<Node> selected, UnaryOperator<Node> change) {
    return node -> selected.test(node) ? change.apply(node) : node;
  }

  static Predicate<Node> text(String text) {
    return node -> text.equals(node.getText());
  }

  static Predicate<Node> kind(String kind) {
    return node -> kind.equals(node.getKind());
  }

  /** The node as SemSitter answers a name it could not resolve. */
  static Node unresolved(Node node) {
    return node.toBuilder().clearRefs().addRefs(Ref.newBuilder().setAccess("r").setOutcome("unresolved")).build();
  }

  static Node leaf(String kind, String text, int line, int col) {
    return Node.newBuilder().setKind(kind).setText(text).setStartLine(line).setStartCol(col).setEndLine(line).setEndCol(col + text.length()).build();
  }

  static Node inner(String kind, Node... children) {
    Node.Builder builder = Node.newBuilder().setKind(kind).setNamed(true);
    for (Node child : children) {
      builder.addChildren(child);
    }
    if (children.length > 0) {
      builder.setStartLine(children[0].getStartLine()).setStartCol(children[0].getStartCol());
    }
    return builder.build();
  }

  static <T extends Tree> List<T> all(Tree root, Class<T> type) {
    List<T> found = new ArrayList<>();
    collect(root, type, found);
    return found;
  }

  private static <T extends Tree> void collect(Tree tree, Class<T> type, List<T> found) {
    if (type.isInstance(tree)) {
      found.add(type.cast(tree));
    }
    if (tree instanceof JavaTree javaTree && !javaTree.isLeaf()) {
      for (Tree child : javaTree.getChildren()) {
        if (child != null) {
          collect(child, type, found);
        }
      }
    }
  }

  private static synchronized void loadRecordings() {
    if (types == null) {
      try {
        types = Replays.readJson(Replays.TYPES);
        packages = Replays.readJson(Replays.PACKAGES);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  private static Boolean hasPackage(String name) {
    if (!packages.has(name) || packages.get(name).isJsonNull()) {
      return null;
    }
    return packages.get(name).getAsBoolean();
  }
}
