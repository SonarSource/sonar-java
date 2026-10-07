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
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonarsource.api.sonarlint.SonarLintSide;

@ScannerSide
@SonarLintSide
public class SemanticReportScanner implements JavaFileScanner {

  private static final int PUBLIC = 1;
  private static final int PACKAGE = 2;
  private static final int PROTECTED = 4;
  private static final int PRIVATE = 8;

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
      private Symbol.TypeSymbol sourceClass;

      @Override
      public void visitClass(ClassTree tree) {
        Symbol.TypeSymbol previousClass = sourceClass;
        sourceClass = tree.symbol();
        try {
          super.visitClass(tree);
        } finally {
          sourceClass = previousClass;
        }
      }

      @Override
      public void visitIdentifier(IdentifierTree tree) {
        if (isIdentifierExpectSymbol(tree)) {
          collectIdentifier(relativePath, tree, sourceClass);
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

  private void collectIdentifier(String path, IdentifierTree identifier, @Nullable Symbol.TypeSymbol sourceClass) {
    Symbol symbol = identifier.symbol();
    if (symbol.isUnknown()) {
      currentModuleReferences.unknownIdentifiers.add(locationOf(path, identifier));
    } else {
      currentModuleReferences.resolvedIdentifierCount++;
      collectSymbol(symbol, () -> locationOf(path, identifier), sourceClass);
    }
  }

  private void collectTypes(Collection<? extends Type> types, Supplier<String> typeLocation, @Nullable Symbol.TypeSymbol sourceClass) {
    int i = 0;
    for (Type type : types) {
      int index = i;
      collectType(type, () -> typeLocation.get() + "[" + index + "]", sourceClass);
      i++;
    }
  }

  private void collectSymbols(Collection<? extends Symbol> symbols, Supplier<String> symbolLocation, @Nullable Symbol.TypeSymbol sourceClass) {
    int i = 0;
    for (Symbol symbol : symbols) {
      int index = i;
      collectSymbol(symbol, () -> symbolLocation.get() + "[" + index + "]", sourceClass);
      i++;
    }
  }

  private void collectType(@Nullable Type type, Supplier<String> typeLocation, @Nullable Symbol.TypeSymbol sourceClass) {
    if (type == null) {
      return;
    }
    if (type.isUnknown()) {
      currentModuleReferences.unknownTypes.add(typeLocation.get());
      return;
    }
    if (currentModuleReferences.allReferencedTypes.add(type)) {
      currentModuleReferences.resolvedTypeCount++;
    }
    if (currentModuleReferences.typeArgumentSources.computeIfAbsent(type, ignored -> newIdentitySet()).add(sourceClass)) {
      String name = type.fullyQualifiedName();
      collectType(type.declaringType(), locationOf(name, "declaringType"), sourceClass);
      collectTypes(type.typeArguments(), locationOf(name, "typeArguments"), sourceClass);
    }
    Type erasure = type.erasure();
    Type expandedType = erasure == null || erasure.isUnknown() ? type : erasure;
    if (currentModuleReferences.typeSources.computeIfAbsent(expandedType, ignored -> newIdentitySet()).add(sourceClass)) {
      currentModuleReferences.typesToResolve.push(new TypeReference(expandedType, sourceClass));
    }
  }

  private void collectTypeChildren(Type type, @Nullable Symbol.TypeSymbol sourceClass) {
    String name = type.fullyQualifiedName();
    collectSymbol(type.symbol(), locationOf(name, "symbol"), sourceClass);
    collectType(type.primitiveType(), locationOf(name, "primitiveType"), sourceClass);
    collectType(type.primitiveWrapperType(), locationOf(name, "primitiveWrapperType"), sourceClass);
    collectType(type.erasure(), locationOf(name, "erasure"), sourceClass);
    if (type.isArray() && type instanceof Type.ArrayType arrayType) {
      collectType(arrayType.elementType(), locationOf(name, "elementType"), sourceClass);
    }
  }

  private void collectSymbol(@Nullable Symbol symbol, Supplier<String> symbolLocation, @Nullable Symbol.TypeSymbol sourceClass) {
    if (symbol == null || symbol.isPackageSymbol()) {
      return;
    }
    if (symbol.isUnknown()) {
      currentModuleReferences.unknownSymbols.add(symbolLocation.get());
      return;
    }
    Symbol referencedSymbol = symbol;
    if (symbol instanceof Symbol.TypeSymbol typeSymbol) {
      symbol = erasedTypeSymbol(typeSymbol);
    }
    Symbol.TypeSymbol targetClass = symbol instanceof Symbol.TypeSymbol typeSymbol ? typeSymbol : symbol.enclosingClass();
    int visibility = visibility(sourceClass, targetClass);
    Integer previousVisibility = currentModuleReferences.allReferencedSymbols.get(symbol);
    if (previousVisibility == null) {
      currentModuleReferences.resolvedSymbolCount++;
    }
    currentModuleReferences.allReferencedSymbols.put(symbol, visibility | (previousVisibility == null ? 0 : previousVisibility));
    if (currentModuleReferences.symbolSources.computeIfAbsent(symbol, ignored -> newIdentitySet()).add(sourceClass)) {
      currentModuleReferences.symbolsToResolve.push(new SymbolReference(symbol, sourceClass));
    }
    if (referencedSymbol != symbol) {
      collectType(referencedSymbol.type(), locationOf(symbol.name(), "type"), sourceClass);
    }
  }

  private static Symbol.TypeSymbol erasedTypeSymbol(Symbol.TypeSymbol typeSymbol) {
    Type type = typeSymbol.type();
    if (type.isUnknown()) {
      return typeSymbol;
    }
    Type erasure = type.erasure();
    if (erasure == null || erasure.isUnknown()) {
      return typeSymbol;
    }
    Symbol.TypeSymbol erasedSymbol = erasure.symbol();
    return erasedSymbol == null || erasedSymbol.isUnknown() ? typeSymbol : erasedSymbol;
  }

  private void collectSymbolChildren(Symbol symbol, @Nullable Symbol.TypeSymbol sourceClass) {
    String name = symbol.name();
    if (symbol instanceof Symbol.TypeSymbol typeSymbol) {
      if (!typeSymbol.type().isUnknown()) {
        name = typeSymbol.type().fullyQualifiedName();
      }
      String typeName = name;
      collectType(typeSymbol.superClass(), locationOf(name, "superClass"), sourceClass);
      collectTypes(typeSymbol.interfaces(), locationOf(name, "interfaces"), sourceClass);
      int access = visibility(sourceClass, typeSymbol);
      int index = 0;
      for (Symbol member : typeSymbol.memberSymbols()) {
        int memberIndex = index;
        index++;
        if (isVisible(member, access)) {
          collectSymbol(member, () -> locationOf(typeName, "memberSymbols").get() + "[" + memberIndex + "]", sourceClass);
        }
      }
      collectTypes(typeSymbol.superTypes(), locationOf(name, "superTypes"), sourceClass);
    } else if (symbol instanceof Symbol.MethodSymbol methodSymbol) {
      name = methodSymbol.signature();

      collectTypes(methodSymbol.parameterTypes(), locationOf(name, "parameterTypes"), sourceClass);
      collectSymbols(methodSymbol.declarationParameters(), locationOf(name, "declarationParameters"), sourceClass);
      collectSymbol(methodSymbol.returnType(), locationOf(name, "returnType"), sourceClass);
      collectTypes(methodSymbol.thrownTypes(), locationOf(name, "thrownTypes"), sourceClass);
      collectSymbols(methodSymbol.overriddenSymbols(), locationOf(name, "overriddenSymbols"), sourceClass);

    }
    collectSymbol(symbol.owner(), locationOf(name, "owner"), sourceClass);
    collectType(symbol.type(), locationOf(name, "type"), sourceClass);
    collectSymbol(symbol.enclosingClass(), locationOf(name, "enclosingClass"), sourceClass);
    collectSymbols(symbol.metadata().symbolAnnotations()
      .stream()
      .map(SymbolMetadata.AnnotationInstance::symbol)
      .toList(), locationOf(name, "annotations"), sourceClass);
  }

  private void resolveSymbolsAndTypes() {
    while (!currentModuleReferences.symbolsToResolve.isEmpty() || !currentModuleReferences.typesToResolve.isEmpty()) {
      while (!currentModuleReferences.typesToResolve.isEmpty()) {
        TypeReference reference = currentModuleReferences.typesToResolve.pop();
        collectTypeChildren(reference.type(), reference.sourceClass());
      }
      while (!currentModuleReferences.symbolsToResolve.isEmpty()) {
        SymbolReference reference = currentModuleReferences.symbolsToResolve.pop();
        collectSymbolChildren(reference.symbol(), reference.sourceClass());
      }
    }
  }

  private static int visibility(@Nullable Symbol.TypeSymbol sourceClass, @Nullable Symbol.TypeSymbol targetClass) {
    if (sourceClass == null || targetClass == null) {
      return PUBLIC;
    }
    Symbol.TypeSymbol sourceOutermost = sourceClass.outermostClass();
    Symbol.TypeSymbol targetOutermost = targetClass.outermostClass();
    if (sourceOutermost != null && targetOutermost != null
      && sourceOutermost.type().fullyQualifiedName().equals(targetOutermost.type().fullyQualifiedName())) {
      return PUBLIC | PACKAGE | PROTECTED | PRIVATE;
    }
    if (packageName(sourceClass).equals(packageName(targetClass))) {
      return PUBLIC | PACKAGE | PROTECTED;
    }
    Symbol.TypeSymbol enclosingClass = sourceClass;
    while (enclosingClass != null) {
      if (enclosingClass.type().isSubtypeOf(targetClass.type())) {
        return PUBLIC | PROTECTED;
      }
      Symbol owner = enclosingClass.owner();
      enclosingClass = owner instanceof Symbol.TypeSymbol typeSymbol ? typeSymbol : null;
    }
    return PUBLIC;
  }

  private static String packageName(Symbol.TypeSymbol typeSymbol) {
    Symbol owner = typeSymbol.owner();
    while (owner != null && !owner.isPackageSymbol()) {
      owner = owner.owner();
    }
    return owner == null ? "" : owner.name();
  }

  private static boolean isVisible(Symbol symbol, int visibility) {
    if (symbol.isPublic()) {
      return (visibility & PUBLIC) != 0;
    }
    if (symbol.isProtected()) {
      return (visibility & PROTECTED) != 0;
    }
    if (symbol.isPrivate()) {
      return (visibility & PRIVATE) != 0;
    }
    return (visibility & PACKAGE) != 0;
  }

  private static <T> Set<T> newIdentitySet() {
    return Collections.newSetFromMap(new IdentityHashMap<>());
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
    final Deque<SymbolReference> symbolsToResolve = new java.util.ArrayDeque<>();
    final Deque<TypeReference> typesToResolve = new java.util.ArrayDeque<>();
    final Set<Type> allReferencedTypes = newIdentitySet();
    final Map<Symbol, Integer> allReferencedSymbols = new IdentityHashMap<>();
    final Map<Symbol, Set<Symbol.TypeSymbol>> symbolSources = new IdentityHashMap<>();
    final Map<Type, Set<Symbol.TypeSymbol>> typeSources = new IdentityHashMap<>();
    final Map<Type, Set<Symbol.TypeSymbol>> typeArgumentSources = new IdentityHashMap<>();
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
      symbolSources.clear();
      typeSources.clear();
      typeArgumentSources.clear();
      typesToResolve.clear();
      symbolsToResolve.clear();
    }
  }

  private record SymbolReference(Symbol symbol, @Nullable Symbol.TypeSymbol sourceClass) {
  }

  private record TypeReference(Type type, @Nullable Symbol.TypeSymbol sourceClass) {
  }

  private static String locationOf(String sourcePath, IdentifierTree identifierTree) {
    SyntaxToken firstToken = identifierTree.firstToken();
    String position = firstToken != null ? (":" + firstToken.range()) : "";
    return "<" + sourcePath + position + ">." + identifierTree.name();
  }

  private static Supplier<String> locationOf(String parentReference, String childName) {
    return  () -> "(" + parentReference + ")." + childName;
  }

}
