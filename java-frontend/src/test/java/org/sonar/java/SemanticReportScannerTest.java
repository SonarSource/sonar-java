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
package org.sonar.java;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SemanticReportScannerTest {

  @TempDir
  Path temp;

  @Test
  void adds_members_when_later_references_have_wider_visibility() throws IOException {
    Symbol.TypeSymbol target = typeSymbol("library.Target", "library", "Target");
    Symbol publicMember = member(target, "publicMember", 0);
    Symbol packageMember = member(target, "packageMember", 1);
    Symbol protectedMember = member(target, "protectedMember", 2);
    Symbol privateMember = member(target, "privateMember", 3);
    when(target.memberSymbols()).thenReturn(List.of(publicMember, packageMember, protectedMember, privateMember));

    Symbol.TypeSymbol unrelated = typeSymbol("consumer.Unrelated", "consumer", "Unrelated");
    Symbol.TypeSymbol samePackage = typeSymbol("library.Peer", "library", "Peer");
    Symbol.TypeSymbol subclass = typeSymbol("consumer.Subclass", "consumer", "Subclass");
    when(subclass.type().isSubtypeOf(target.type())).thenReturn(true);

    SemanticReportScanner scanner = new SemanticReportScanner();
    scanner.enterModule(temp);
    assertResolvedSymbols(scanner, unrelated, target, 2);
    assertResolvedSymbols(scanner, subclass, target, 3);
    assertResolvedSymbols(scanner, samePackage, target, 4);
    assertResolvedSymbols(scanner, target, target, 5);
    scanner.leaveModule();
  }

  private void assertResolvedSymbols(SemanticReportScanner scanner, Symbol.TypeSymbol source, Symbol.TypeSymbol target, int expected) throws IOException {
    Path file = Files.createTempFile(temp, "Reference", ".java");
    InputFile inputFile = mock(InputFile.class);
    when(inputFile.uri()).thenReturn(file.toUri());
    JavaFileScannerContext context = mock(JavaFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);

    IdentifierTree identifier = mock(IdentifierTree.class);
    when(identifier.name()).thenReturn("Target");
    when(identifier.symbol()).thenReturn(target);
    doAnswer(invocation -> {
      invocation.getArgument(0, TreeVisitor.class).visitIdentifier(identifier);
      return null;
    }).when(identifier).accept(org.mockito.ArgumentMatchers.any());

    ClassTree classTree = mock(ClassTree.class);
    when(classTree.symbol()).thenReturn(source);
    when(classTree.members()).thenReturn(List.of(identifier));
    doAnswer(invocation -> {
      invocation.getArgument(0, TreeVisitor.class).visitClass(classTree);
      return null;
    }).when(classTree).accept(org.mockito.ArgumentMatchers.any());

    CompilationUnitTree compilationUnit = mock(CompilationUnitTree.class);
    when(compilationUnit.types()).thenReturn(List.of(classTree));
    doAnswer(invocation -> {
      invocation.getArgument(0, TreeVisitor.class).visitCompilationUnit(compilationUnit);
      return null;
    }).when(compilationUnit).accept(org.mockito.ArgumentMatchers.any());
    when(context.getTree()).thenReturn(compilationUnit);

    scanner.scanFile(context);
    Path report = temp.resolve("report.json");
    scanner.writeReport(report, temp);
    assertThat(JsonParser.parseString(Files.readString(report)).getAsJsonObject().get("resolvedSymbolCount").getAsInt()).isEqualTo(expected);
  }

  private static Symbol.TypeSymbol typeSymbol(String qualifiedName, String packageName, String name) {
    Symbol.TypeSymbol symbol = mock(Symbol.TypeSymbol.class);
    Symbol packageSymbol = mock(Symbol.class);
    Type type = mock(Type.class);
    SymbolMetadata metadata = mock(SymbolMetadata.class);
    when(symbol.name()).thenReturn(name);
    when(symbol.owner()).thenReturn(packageSymbol);
    when(packageSymbol.isPackageSymbol()).thenReturn(true);
    when(packageSymbol.name()).thenReturn(packageName);
    when(symbol.outermostClass()).thenReturn(symbol);
    when(symbol.enclosingClass()).thenReturn(symbol);
    when(symbol.type()).thenReturn(type);
    when(type.fullyQualifiedName()).thenReturn(qualifiedName);
    when(type.isUnknown()).thenReturn(true);
    when(symbol.metadata()).thenReturn(metadata);
    when(metadata.symbolAnnotations()).thenReturn(List.of());
    return symbol;
  }

  private static Symbol member(Symbol.TypeSymbol owner, String name, int visibility) {
    Symbol symbol = mock(Symbol.class);
    SymbolMetadata metadata = mock(SymbolMetadata.class);
    when(symbol.name()).thenReturn(name);
    when(symbol.owner()).thenReturn(owner);
    when(symbol.enclosingClass()).thenReturn(owner);
    when(symbol.type()).thenReturn(Type.UNKNOWN);
    when(symbol.metadata()).thenReturn(metadata);
    when(metadata.symbolAnnotations()).thenReturn(List.of());
    if (visibility == 0) {
      when(symbol.isPublic()).thenReturn(true);
    } else if (visibility == 2) {
      when(symbol.isProtected()).thenReturn(true);
    } else if (visibility == 3) {
      when(symbol.isPrivate()).thenReturn(true);
    }
    return symbol;
  }
}
