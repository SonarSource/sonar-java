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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.semsitter.wire.Document;
import org.sonarsource.semsitter.wire.Fact;
import org.sonarsource.semsitter.wire.FactEntry;
import org.sonarsource.semsitter.wire.TypeRef;

import static org.sonar.java.semsitter.SemaFacts.addTypeName;
import static org.sonar.java.semsitter.SemaFacts.collectFactNames;
import static org.sonar.java.semsitter.SemaFacts.erase;
import static org.sonar.java.semsitter.SemaFacts.isWellKnown;
import static org.sonar.java.semsitter.SemaFacts.packageName;
import static org.sonar.java.semsitter.SemaFacts.simpleName;
import static org.sonar.java.semsitter.SemaFacts.text;
import static org.sonar.java.semsitter.SemaFacts.typeFact;

/**
 * Semantic model of one translated file. Symbols are created from the SemSitter dump and from the
 * {@code types} response (project graph, classpath declarations including the JDK image, embedded JDK table).
 */
public final class JSema implements Sema {

  static final String ANNOTATIONS = "annotations";
  static final String EXTENDS = "extends";
  static final String WILDCARD = "wildcard";
  static final String MEMBERS = "members";
  static final String STATIC = "static";
  static final String ABSTRACT = "abstract";
  static final String RECORD = "record";
  static final String ARRAY = "array";
  static final Set<String> PRIMITIVES = Set.of("byte", "short", "int", "long", "char", "float", "double", "boolean", "void");
  final Map<String, Fact> facts = new HashMap<>();
  final Map<String, org.sonarsource.semsitter.wire.Symbol> dumpSymbols = new LinkedHashMap<>();
  final Map<String, JsonObject> typeInfos = new HashMap<>();
  final DeclaredMembers declaredMembers = new DeclaredMembers();
  final Map<String, JSymbol> symbols = new HashMap<>();
  final Map<String, JTypeSymbol> typesByName = new HashMap<>();
  private final Map<String, JType> rawTypes = new HashMap<>();
  private final BinaryNames binaryNames = new BinaryNames();
  final Map<String, Tree> declarations = new HashMap<>();
  final Map<String, List<IdentifierTree>> usages = new HashMap<>();
  private final Set<JProblem> undefinedTypes = new HashSet<>();
  private final Set<String> requestedTypes = new HashSet<>();
  Function<Set<String>, JsonObject> typeLoader = names -> new JsonObject();
  private Function<String, Boolean> packageLoader = name -> null;
  boolean typesLoaded;
  final JSymbol unknownSymbol = new JVariableSymbol(this, null, "", "local");

  /** Types of the built-in library table, which are not on the analyzed classpath; off to leave them unresolved. */
  private static final boolean FRAMEWORK_TYPES = Boolean.parseBoolean(System.getProperty("sonar.java.semsitter.frameworkTypes", "true"));

  boolean noLibraries;

  private final Runnable environmentCleaner = new EnvironmentCleaner();

  List<Type> explicitTypeArguments = List.of();

  private final SemaParts parts = new SemaParts(this);

  public JSema() {
    unknownSymbol.unknown(true);
  }

  SemaAnnotations annotationFacts() {
    return parts.annotationFacts;
  }

  Inference inference() {
    return parts.inference;
  }

  FunctionalInference functionalInference() {
    return parts.functionalInference;
  }

  Substitution substitution() {
    return parts.substitution;
  }

  LibraryMembers libraryMembers() {
    return parts.libraryMembers;
  }

  MemberFactory memberFactory() {
    return parts.memberFactory;
  }

  MemberLookup lookup() {
    return parts.lookup;
  }

  OverloadRanking overloads() {
    return parts.overloads;
  }

  FactTypes factTypes() {
    return parts.factTypes;
  }

  /**
   * Reads the symbols and graph facts of one CST document. Type declarations already known by
   * erased name are reused, so a later {@link #ingestTypes} call fills them in instead of
   * creating a second symbol.
   */
  public void ingestDocument(Document document) {
    for (FactEntry entry : document.getFactsList()) {
      facts.put(entry.getKey(), entry.getValue());
    }
    for (org.sonarsource.semsitter.wire.Symbol element : document.getSymbolsList()) {
      String id = element.getId();
      if (!id.isEmpty()) {
        dumpSymbols.put(id, element);
        if ("type".equals(element.getKind()) && id.indexOf('<') < 0) {
          binaryNames.binaryName(erase(id));
        }
      }
    }
    for (String id : dumpSymbols.keySet()) {
      libraryMembers().createDumpSymbol(id);
    }
    for (String id : new ArrayList<>(dumpSymbols.keySet())) {
      libraryMembers().linkDumpSymbol(id);
    }
  }

  String sourceName(String fqn) {
    return binaryNames.sourceName(fqn);
  }

  /** Whether the document of this file declares the symbol {@code id}. */
  boolean inDocument(String id) {
    return dumpSymbols.containsKey(id);
  }

  /** Asks whether a package is declared, with null when the answer is unknown. */
  public void loadPackages(Function<String, Boolean> loader) {
    packageLoader = loader;
  }

  /**
   * Loads the declarations of every type the document references, then keeps {@code loader} to
   * fetch the types reached later, like the return type of a library method.
   */
  public void loadTypes(Function<Set<String>, JsonObject> loader) {
    typeLoader = names -> {
      JsonObject answer = loader.apply(names);
      return answer == null ? new JsonObject() : answer;
    };
    Set<String> names = referencedTypeNames();
    requestedTypes.addAll(names);
    ingestTypes(typeLoader.apply(names));
    markUndeclared();
  }

  /** A type some symbol names that no answer declares and the file does not declare is unknown, whenever its symbol was created. */
  private void markUndeclared() {
    Set<String> missing = new HashSet<>();
    for (Map.Entry<String, JTypeSymbol> entry : typesByName.entrySet()) {
      String fqn = entry.getKey();
      JTypeSymbol symbol = entry.getValue();
      if (!typeInfos.containsKey(fqn) && !isWellKnown(fqn) && !facts.containsKey(fqn) && !dumpSymbols.containsKey(fqn) && !symbol.isUnknown()
        && !symbol.isTypeParameter() && !PRIMITIVES.contains(fqn) && fqn.indexOf('<') < 0 && !fqn.endsWith("[]") && !fqn.contains("::")) {
        missing.add(fqn);
      }
    }
    Set<String> fresh = new HashSet<>(missing);
    fresh.removeIf(name -> !requestedTypes.add(name));
    if (!fresh.isEmpty()) {
      ingestTypes(typeLoader.apply(fresh));
    }
    for (String fqn : missing) {
      if (!typeInfos.containsKey(fqn)) {
        typesByName.get(fqn).unknown(true);
        undefinedTypes.add(JProblem.undefinedType(fqn));
      }
    }
  }

  /** Without any library, a method of a type nothing declares is unknown instead of named after its written owner. */
  public void noLibraries(boolean noLibraries) {
    this.noLibraries = noLibraries;
  }

  public void ingestTypes(JsonObject types) {
    typesLoaded = true;
    for (Map.Entry<String, JsonElement> entry : types.entrySet()) {
      JsonObject info = entry.getValue().getAsJsonObject();
      if (!FRAMEWORK_TYPES && "framework-db".equals(text(info, "origin"))) {
        continue;
      }
      typeInfos.put(entry.getKey(), info);
      libraryMembers().registerProjectMembers(info);
      JTypeSymbol symbol = typesByName.get(entry.getKey());
      if (symbol == null) {
        symbol = new JTypeSymbol(this, entry.getKey(), simpleName(entry.getKey()), false);
        symbol.owner(packageSymbol(packageName(entry.getKey())));
        symbol.type(rawNamed(entry.getKey()));
        typesByName.put(entry.getKey(), symbol);
        symbols.putIfAbsent(entry.getKey(), symbol);
      }
      memberFactory().applyTypeInfo(symbol, info);
    }
  }

  public Set<String> referencedTypeNames() {
    Set<String> names = new HashSet<>();
    for (String id : facts.keySet()) {
      addTypeName(names, id);
    }
    for (org.sonarsource.semsitter.wire.Symbol symbol : dumpSymbols.values()) {
      addTypeName(names, symbol.getId());
      TypeRef type = typeFact(symbol);
      if (type != null) {
        collectFactNames(names, type);
      }
    }
    return names;
  }

  public void declare(String id, Tree tree) {
    declarations.put(id, tree);
  }

  public void use(String id, IdentifierTree tree) {
    usages.computeIfAbsent(id, key -> new ArrayList<>()).add(tree);
  }

  @Override
  public Type getClassType(String fullyQualifiedName) {
    if (fullyQualifiedName == null || fullyQualifiedName.isBlank() || "!Unknown!".equals(fullyQualifiedName)) {
      return Type.UNKNOWN;
    }
    if ("<nulltype>".equals(fullyQualifiedName)) {
      return JType.nullType(this);
    }
    if (fullyQualifiedName.endsWith("[]")) {
      JType element = asJType(getClassType(fullyQualifiedName.substring(0, fullyQualifiedName.length() - 2)));
      return memberFactory().arraySymbol(element).type();
    }
    if (PRIMITIVES.contains(fullyQualifiedName)) {
      return primitiveType(fullyQualifiedName);
    }
    typeSymbol(fullyQualifiedName);
    return rawNamed(fullyQualifiedName);
  }

  public Runnable getEnvironmentCleaner() {
    return environmentCleaner;
  }

  public Set<JProblem> undefinedTypes() {
    return undefinedTypes;
  }

  JSymbol resolve(@Nullable String id) {
    if (id == null || id.isEmpty()) {
      return unknownSymbol;
    }
    JSymbol found = symbols.get(id);
    if (found != null) {
      return found;
    }
    if (dumpSymbols.containsKey(id)) {
      libraryMembers().createDumpSymbol(id);
      libraryMembers().linkDumpSymbol(id);
      return symbols.getOrDefault(id, unknownSymbol);
    }
    if (id.indexOf('#') >= 0) {
      return memberFactory().createMember(id);
    }
    if (id.indexOf('<') >= 0) {
      return memberFactory().typeParameter(id);
    }
    return typeSymbol(erase(id));
  }

  Symbol.TypeSymbol resolveTypeSymbol(@Nullable String id) {
    JSymbol symbol = resolve(id);
    return symbol instanceof Symbol.TypeSymbol typeSymbol ? typeSymbol : Symbol.TypeSymbol.UNKNOWN_TYPE;
  }

  JTypeSymbol typeSymbol(String name) {
    String fqn = sourceName(name);
    JTypeSymbol found = typesByName.get(fqn);
    if (found != null) {
      return found;
    }
    JTypeSymbol created = new JTypeSymbol(this, fqn, simpleName(fqn), false);
    created.owner(packageSymbol(packageName(fqn)));
    created.type(rawNamed(fqn));
    typesByName.put(fqn, created);
    symbols.putIfAbsent(fqn, created);
    if (typesLoaded && requestedTypes.add(fqn)) {
      JsonObject loaded = typeLoader.apply(Set.of(fqn));
      if (loaded == null) {
        return created;
      }
      ingestTypes(loaded);
      if (typeInfos.containsKey(fqn)) {
        return created;
      }
    }
    if (typesLoaded && !isWellKnown(fqn)) {
      created.unknown(true);
      undefinedTypes.add(JProblem.undefinedType(fqn));
    }
    return created;
  }

  /** Moves a recorded use of a method to another overload. */
  void moveUse(@Nullable String from, @Nullable String to, IdentifierTree use) {
    List<IdentifierTree> old = from == null ? null : usages.get(from);
    if (old != null) {
      old.remove(use);
    }
    if (to != null) {
      use(to, use);
    }
  }

  JType primitiveType(String name) {
    return rawTypes.computeIfAbsent(name, key -> JType.primitive(this, key));
  }

  JType asJType(Type type) {
    return type instanceof JType jType ? jType : JType.unknown(this);
  }

  JType rawNamed(String fqn) {
    return rawTypes.computeIfAbsent(sourceName(fqn), name -> JType.named(this, binaryNames.binaryOf(name), List.of()));
  }

  /** Whether the project, the classpath or the JDK declares the type {@code fqn}. */
  boolean declaresType(String name) {
    String fqn = sourceName(name);
    if (!typeInfos.containsKey(fqn) && typesLoaded && requestedTypes.add(fqn)) {
      ingestTypes(typeLoader.apply(Set.of(fqn)));
    }
    return typeInfos.containsKey(fqn);
  }

  /** Whether a type of the package {@code name} is known, which is how a package shows itself. */
  boolean knowsPackage(String name) {
    Boolean declared = typesLoaded ? packageLoader.apply(name) : null;
    if (declared != null) {
      return declared;
    }
    String prefix = sourceName(name) + ".";
    return typeInfos.keySet().stream().anyMatch(type -> type.startsWith(prefix) && type.indexOf('.', prefix.length()) < 0);
  }

  Symbol packageSymbol(String name) {
    if (name == null || name.isEmpty()) {
      return Symbol.ROOT_PACKAGE;
    }
    String key = "pkg:" + name;
    JSymbol found = symbols.get(key);
    if (found != null) {
      return found;
    }
    JPackageSymbol pkg = new JPackageSymbol(this, name);
    symbols.put(key, pkg);
    pkg.owner(Symbol.ROOT_PACKAGE);
    return pkg;
  }

  /** Nothing is held open per file; one instance per sema keeps the cleaners of different files distinct. */
  private static final class EnvironmentCleaner implements Runnable {
    @Override
    public void run() {
      // no file or jar stays open
    }
  }
}
