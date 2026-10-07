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

import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonarsource.api.sonarlint.SonarLintSide;

@ScannerSide
@SonarLintSide
public class SemanticReportScanner implements JavaFileScanner {

  private final Map<Path, ModuleReferences> moduleMap = new TreeMap<>();
  private ModuleReferences currentModuleReferences = null;

  public void enterModule(Path moduleDir) {
    Path normalizedPath = normalizePath(moduleDir);
    this.currentModuleReferences = moduleMap.computeIfAbsent(normalizedPath, k -> new ModuleReferences(normalizedPath));
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    if (currentModuleReferences == null) {
      throw new IllegalStateException("Module context is not set. Please call enterModule() before scanning files.");
    }
    String relativePath = relativePath(context.getInputFile());
    context.getTree().accept(new BaseTreeVisitor() {
      @Override
      public void visitIdentifier(IdentifierTree tree) {
        if (isIdentifierExpectSymbol(tree)) {
          collectIdentifier(relativePath, tree);
        }
        super.visitIdentifier(tree);
      }

      private static boolean isIdentifierExpectSymbol(IdentifierTree tree) {
        String name = tree.name();
        return !"new".equals(name) && !"class".equals(name) && !tree.isUnnamedVariable();
      }

      @Override
      public void visitCompilationUnit(CompilationUnitTree tree) {
        scan(tree.packageDeclaration());
        scan(tree.types());
        scan(tree.moduleDeclaration());
      }
    });
    resolveSymbolsAndTypes();
  }

  public void leaveModule() {
    this.currentModuleReferences.clearASTElements();
    this.currentModuleReferences = null;
  }

  private String relativePath(InputFile inputFile) {
    return currentModuleReferences.moduleDir.relativize(normalizePath(Path.of(inputFile.uri())))
      .toString().replace('\\', '/');
  }

  private void collectIdentifier(String path, IdentifierTree identifier) {
    Symbol symbol = identifier.symbol();
    if (symbol.isUnknown()) {
      currentModuleReferences.unknownIdentifiers.add(locationOf(path, identifier));
    } else {
      currentModuleReferences.resolvedIdentifierCount++;
      collectSymbol(symbol, () -> locationOf(path, identifier));
    }
  }

  private void collectTypes(Collection<? extends Type> types, Supplier<String> typeLocation) {
    int i = 0;
    for (Type type : types) {
      int index = i;
      collectType(type, () -> typeLocation.get() + "[" + index + "]");
      i++;
    }
  }

  private void collectSymbols(Collection<? extends Symbol> symbols, Supplier<String> symbolLocation) {
    int i = 0;
    for (Symbol symbol : symbols) {
      int index = i;
      collectSymbol(symbol, () -> symbolLocation.get() + "[" + index + "]");
      i++;
    }
  }

  private void collectType(@Nullable Type type, Supplier<String> typeLocation) {
    if (type == null) {
      return;
    }
    if (type.isUnknown()) {
      currentModuleReferences.unknownTypes.add(typeLocation.get());
      return;
    }
    if (currentModuleReferences.allReferencedTypes.add(type)) {
      currentModuleReferences.resolvedTypeCount++;
      currentModuleReferences.typesToResolve.push(type);
    }
  }

  private void collectTypeChildren(Type type) {
    String name = type.fullyQualifiedName();
    collectSymbol(type.symbol(), locationOf(name, "symbol"));
    collectType(type.primitiveType(), locationOf(name, "primitiveType"));
    collectType(type.primitiveWrapperType(), locationOf(name, "primitiveWrapperType"));
    collectType(type.declaringType(), locationOf(name, "declaringType"));
    collectType(type.erasure(), locationOf(name, "erasure"));
    collectTypes(type.typeArguments(), locationOf(name, "typeArguments"));
    if (type.isArray() && type instanceof Type.ArrayType arrayType) {
      collectType(arrayType.elementType(), locationOf(name, "elementType"));
    }
  }

  private void collectSymbol(@Nullable Symbol symbol, Supplier<String> symbolLocation) {
    if (symbol == null || symbol.isPackageSymbol()) {
      return;
    }
    if (symbol.isUnknown()) {
      currentModuleReferences.unknownSymbols.add(symbolLocation.get());
      return;
    }
    if (currentModuleReferences.allReferencedSymbols.add(symbol)) {
      currentModuleReferences.resolvedSymbolCount++;
      currentModuleReferences.symbolsToResolve.push(symbol);
    }
  }

  private void collectSymbolChildren(Symbol symbol) {
    String name = symbol.name();
    if (symbol instanceof Symbol.TypeSymbol typeSymbol) {
      if (!typeSymbol.type().isUnknown()) {
        name = typeSymbol.type().fullyQualifiedName();
      }
      collectType(typeSymbol.superClass(), locationOf(name, "superClass"));
      collectTypes(typeSymbol.interfaces(), locationOf(name, "interfaces"));
      if (typeSymbol.declaration() != null) {
        collectSymbols(typeSymbol.memberSymbols(), locationOf(name, "memberSymbols"));
      }
      collectTypes(typeSymbol.superTypes(), locationOf(name, "superTypes"));
    } else if (symbol instanceof Symbol.MethodSymbol methodSymbol) {
      name = methodSymbol.signature();

      collectTypes(methodSymbol.parameterTypes(), locationOf(name, "parameterTypes"));
      collectSymbols(methodSymbol.declarationParameters(), locationOf(name, "declarationParameters"));
      collectSymbol(methodSymbol.returnType(), locationOf(name, "returnType"));
      collectTypes(methodSymbol.thrownTypes(), locationOf(name, "thrownTypes"));
      collectSymbols(methodSymbol.overriddenSymbols(), locationOf(name, "overriddenSymbols"));

    }
    collectSymbol(symbol.owner(), locationOf(name, "owner"));
    collectType(symbol.type(), locationOf(name, "type"));
    collectSymbol(symbol.enclosingClass(), locationOf(name, "enclosingClass"));
    collectSymbols(symbol.metadata().symbolAnnotations()
      .stream()
      .map(SymbolMetadata.AnnotationInstance::symbol)
      .toList(),locationOf(name, "annotations"));
  }

  private void resolveSymbolsAndTypes() {
    while (!currentModuleReferences.symbolsToResolve.isEmpty() || !currentModuleReferences.typesToResolve.isEmpty()) {
      while (!currentModuleReferences.typesToResolve.isEmpty()) {
        Type type = currentModuleReferences.typesToResolve.pop();
        collectTypeChildren(type);
      }
      while (!currentModuleReferences.symbolsToResolve.isEmpty()) {
        Symbol symbol = currentModuleReferences.symbolsToResolve.pop();
        collectSymbolChildren(symbol);
      }
    }
  }

  public void writeReport(Path reportPath, Path projectRoot) {
    Path normalizedProjectRoot = normalizePath(projectRoot);
    int resolvedIdentifierCount = 0;
    int unknownIdentifierCount = 0;
    int resolvedSymbolCount = 0;
    int unknownSymbolCount = 0;
    int resolvedTypeCount = 0;
    int unknownTypeCount = 0;
    for (ModuleReferences module : moduleMap.values()) {
      resolvedIdentifierCount += module.resolvedIdentifierCount;
      unknownIdentifierCount += module.unknownIdentifiers.size();
      resolvedSymbolCount += module.resolvedSymbolCount;
      unknownSymbolCount += module.unknownSymbols.size();
      resolvedTypeCount += module.resolvedTypeCount;
      unknownTypeCount += module.unknownTypes.size();
    }
    try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(reportPath, StandardCharsets.UTF_8))) {
      writer.setIndent("  ");
      writer.beginObject();
      writer.name("resolvedIdentifierCount").value(resolvedIdentifierCount);
      writer.name("unknownIdentifierCount").value(unknownIdentifierCount);
      writer.name("percentageOfUnknownIdentifier").value(percentage(unknownIdentifierCount, resolvedIdentifierCount + unknownIdentifierCount));
      writer.name("resolvedSymbolCount").value(resolvedSymbolCount);
      writer.name("unknownSymbolCount").value(unknownSymbolCount);
      writer.name("resolvedTypeCount").value(resolvedTypeCount);
      writer.name("unknownTypeCount").value(unknownTypeCount);
      writer.name("modules").beginArray();
      for (Map.Entry<Path, ModuleReferences> moduleEntry : moduleMap.entrySet()) {
        ModuleReferences module = moduleEntry.getValue();
        String path = module.moduleDir.equals(normalizedProjectRoot) ? "." : normalizedProjectRoot.relativize(module.moduleDir).toString();
        writer.beginObject();

        writer.name("path").value(path.replace('\\', '/'));
        writer.name("resolvedIdentifierCount").value(module.resolvedIdentifierCount);
        writer.name("unknownIdentifierCount").value(module.unknownIdentifiers.size());
        writer.name("percentageOfUnknownIdentifier").value(percentage(module.unknownIdentifiers.size(), module.resolvedIdentifierCount + module.unknownIdentifiers.size()));
        writer.name("resolvedSymbolCount").value(module.resolvedSymbolCount);
        writer.name("unknownSymbolCount").value(module.unknownSymbols.size());
        writer.name("resolvedTypeCount").value(module.resolvedTypeCount);
        writer.name("unknownTypeCount").value(module.unknownTypes.size());

        writer.name("unknownIdentifiers").beginArray();
        for (String identifier : module.unknownIdentifiers) {
          writer.value(identifier);
        }
        writer.endArray();

        writer.name("unknownSymbols").beginArray();
        for (String symbol : module.unknownSymbols) {
          writer.value(symbol);
        }
        writer.endArray();

        writer.name("unknownTypes").beginArray();
        for (String type : module.unknownTypes) {
          writer.value(type);
        }
        writer.endArray();

        writer.endObject();
      }
      writer.endArray();
      writer.endObject();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to write semantic report to " + reportPath, e);
    }
  }

  private static Path normalizePath(Path path) {
    try {
      return path.toRealPath(LinkOption.NOFOLLOW_LINKS);
    } catch (IOException e) {
      return path.toAbsolutePath().normalize();
    }
  }

  private static BigDecimal percentage(int unknown, int total) {
    if (total == 0) {
      return BigDecimal.ZERO.setScale(3);
    }
    return BigDecimal.valueOf(unknown).multiply(BigDecimal.valueOf(100))
      .divide(BigDecimal.valueOf(total), 3, RoundingMode.HALF_UP);
  }

  private static class ModuleReferences {
    final Path moduleDir;
    final Deque<Symbol> symbolsToResolve = new java.util.ArrayDeque<>();
    final Deque<Type> typesToResolve = new java.util.ArrayDeque<>();
    final Set<Type> allReferencedTypes = Collections.newSetFromMap(new IdentityHashMap<>());
    final Set<Symbol> allReferencedSymbols = Collections.newSetFromMap(new IdentityHashMap<>());
    final Set<String> unknownIdentifiers = new TreeSet<>();
    final Set<String> unknownSymbols = new TreeSet<>();
    final Set<String> unknownTypes = new TreeSet<>();
    int resolvedIdentifierCount = 0;
    int resolvedSymbolCount = 0;
    int resolvedTypeCount = 0;
    ModuleReferences(Path moduleDir)  {
      this.moduleDir = moduleDir;
    }
    void clearASTElements() {
      allReferencedTypes.clear();
      allReferencedSymbols.clear();
      typesToResolve.clear();
      symbolsToResolve.clear();
    }
  }

  private static String locationOf(String sourcePath, IdentifierTree identifierTree) {
    SyntaxToken firstToken = identifierTree.firstToken();
    String position = firstToken != null ? ":" + firstToken.range() : "";
    return "<" + sourcePath + position + ">." + identifierTree.name();
  }

  private static Supplier<String> locationOf(String parentReference, String childName) {
    return  () -> "(" + parentReference + ")." + childName;
  }

}
