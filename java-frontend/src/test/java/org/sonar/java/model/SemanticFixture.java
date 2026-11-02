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
package org.sonar.java.model;

import java.io.File;
import java.util.List;
import org.sonar.java.test.TestResourceScope;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.VariableTree;

/** Backend-aware source fixtures expose Sonar declarations and retain the parser through lazy queries. */
final class SemanticFixture implements AutoCloseable {

  private final FrontendParser.ParsedFile parsed;

  private SemanticFixture(FrontendParser.ParsedFile parsed) {
    this.parsed = parsed;
  }

  static SemanticFixture scoped(String source) {
    return scoped(source, JParserTestUtils.DEFAULT_CLASSPATH);
  }

  static SemanticFixture scoped(String source, List<File> classpath) {
    return TestResourceScope.retain(new SemanticFixture(FrontendParser.parse(JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, classpath, "File.java", source)));
  }

  static SemanticFixture open(FrontendBackend backend, FrontendConfiguration configuration, String unitName, String source) {
    return new SemanticFixture(FrontendParser.parse(backend, configuration, unitName, source));
  }

  JavaTree.CompilationUnitTreeImpl tree() {
    return parsed.tree();
  }

  ClassTree declaration(int index) {
    return (ClassTree) tree().types().get(index);
  }

  MethodTree method(int typeIndex, int memberIndex) {
    return (MethodTree) declaration(typeIndex).members().get(memberIndex);
  }

  VariableTree variable(int typeIndex, int memberIndex) {
    return (VariableTree) declaration(typeIndex).members().get(memberIndex);
  }

  Type type(String name) {
    return tree().sema.getClassType(name);
  }

  List<SymbolMetadata.AnnotationInstance> annotations(Symbol symbol) {
    return symbol.metadata().annotations();
  }

  @Override
  public void close() {
    parsed.close();
  }
}
