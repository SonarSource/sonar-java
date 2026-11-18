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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.diagnostics.ProblemKind;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Semantic model of one translated file. Symbols are created from the SemSitter dump and from the
 * {@code types} response (project graph, classpath declarations including the JDK image, embedded JDK table).
 */
public final class JSema implements Sema {
  private static final String ANNOTATIONS = "annotations";
  private static final String MEMBERS = "members";
  private static final String THROWS = "throws";
  private static final int MEMBER_IDS_LIMIT = 200_000;
  private static final Map<String, MemberId> MEMBER_IDS = new ConcurrentHashMap<>();
  private static final MemberId NOT_A_MEMBER = new MemberId("", "", null, false, -1, List.of());
  private static final String STATIC = "static";
  private static final String ABSTRACT = "abstract";
  private static final String RECORD = "record";
  private static final String ARRAY = "array";


  private static final Set<String> PRIMITIVES = Set.of("byte", "short", "int", "long", "char", "float", "double", "boolean", "void");
  private static final Set<String> OBJECT_METHODS = Set.of("equals/1", "hashCode/0", "toString/0");
  private static final Pattern LAMBDA_SEGMENTS = Pattern.compile("^(\\$lambda\\$[^$()]*\\(\\))+");

  private final Map<String, JsonObject> facts = new HashMap<>();
  private final Map<String, JsonObject> dumpSymbols = new HashMap<>();
  private final Map<String, JsonObject> typeInfos = new HashMap<>();
  private final Map<String, List<DeclaredAnnotation>> libraryAnnotations = new HashMap<>();
  private final Map<String, List<String>> projectThrows = new HashMap<>();
  private final Map<String, JsonObject> projectMemberFacts = new HashMap<>();
  private final Map<String, Boolean> projectVarArgs = new HashMap<>();
  private final Map<String, List<DeclaredAnnotation>> packageAnnotations = new HashMap<>();
  private final Map<String, JsonObject> projectFieldTypes = new HashMap<>();
  private final Map<String, JSymbol> symbols = new HashMap<>();
  private final Map<String, Object> annotationDefaults = new HashMap<>();
  private final Map<String, Symbol> placeholders = new HashMap<>();
  private final Map<String, JTypeSymbol> typesByName = new HashMap<>();
  private final Map<String, JType> rawTypes = new HashMap<>();
  private final Map<String, String> binaryNames = new HashMap<>();
  private final Map<String, String> sourceNames = new HashMap<>();
  final Map<String, Tree> declarations = new HashMap<>();
  final Map<String, List<IdentifierTree>> usages = new HashMap<>();
  private final Set<JProblem> undefinedTypes = new HashSet<>();
  private final Set<String> creating = new HashSet<>();
  private final Set<String> requestedTypes = new HashSet<>();
  private Function<Set<String>, JsonObject> typeLoader = names -> new JsonObject();
  private Function<String, Boolean> packageLoader = name -> null;
  private boolean typesLoaded;
  private final JSymbol unknownSymbol = new JVariableSymbol(this, null, "", "local");

  public JSema() {
    unknownSymbol.unknown(true);
  }

  /**
   * Reads the symbols and graph facts of one CST document. Type declarations already known by
   * erased name are reused, so a later {@link #ingestTypes} call fills them in instead of
   * creating a second symbol.
   */
  public void ingestDocument(JsonObject document) {
    JsonObject factObject = document.getAsJsonObject("facts");
    if (factObject != null) {
      for (Map.Entry<String, JsonElement> entry : factObject.entrySet()) {
        facts.put(entry.getKey(), entry.getValue().getAsJsonObject());
      }
    }
    JsonArray symbolArray = document.getAsJsonArray("symbols");
    if (symbolArray != null) {
      for (JsonElement element : symbolArray) {
        JsonObject symbol = element.getAsJsonObject();
        String id = text(symbol, "id");
        if (id != null) {
          dumpSymbols.put(id, symbol);
          if ("type".equals(text(symbol, "kind")) && id.indexOf('<') < 0) {
            binaryName(erase(id));
          }
        }
      }
    }
    for (String id : dumpSymbols.keySet()) {
      createDumpSymbol(id);
    }
    for (String id : new ArrayList<>(dumpSymbols.keySet())) {
      linkDumpSymbol(id);
    }
  }

  /**
   * The binary name Java gives the type SemSitter names {@code id}: a local class {@code p.C#f()$Local} becomes
   * {@code p.C$1Local}, numbered by the first index not yet taken in its enclosing class, in declaration order.
   */
  private String binaryName(String id) {
    String known = binaryNames.get(id);
    if (known != null) {
      return known;
    }
    int hash = id.lastIndexOf('#');
    int open = id.indexOf('(', hash);
    int close = open < 0 ? -1 : id.indexOf(')', open);
    String rest = close < 0 ? "" : LAMBDA_SEGMENTS.matcher(id.substring(close + 1)).replaceFirst("");
    if (hash < 0 || !rest.startsWith("$") || rest.length() == 1) {
      return id;
    }
    int nested = rest.indexOf('$', 1);
    String name = nested < 0 ? rest.substring(1) : rest.substring(1, nested);
    String localId = id.substring(0, close + 1) + "$" + name;
    String local = binaryNames.get(localId);
    if (local == null) {
      String enclosing = binaryName(id.substring(0, hash));
      int index = 1;
      while (sourceNames.containsKey(enclosing + "$" + index + name)) {
        index++;
      }
      local = enclosing + "$" + index + name;
      binaryNames.put(localId, local);
      sourceNames.put(local, localId);
    }
    String binary = nested < 0 ? local : local + rest.substring(nested);
    binaryNames.put(id, binary);
    sourceNames.put(binary, id);
    return binary;
  }

  private String sourceName(String fqn) {
    return sourceNames.getOrDefault(fqn, fqn);
  }

  /** Whether the document of this file declares the symbol {@code id}. */
  boolean inDocument(String id) {
    return dumpSymbols.containsKey(id);
  }

  /** The name a member symbol created from {@code id} gets, or null when the id names no member. */
  @Nullable
  static String memberName(String id) {
    MemberId parsed = parseMember(id);
    return parsed == null ? null : parsed.name;
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
    typeLoader = loader;
    Set<String> names = referencedTypeNames();
    requestedTypes.addAll(names);
    ingestTypes(loader.apply(names));
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

  /** Types of the built-in library table, which are not on the analyzed classpath; off to leave them unresolved. */
  private static final boolean FRAMEWORK_TYPES = Boolean.parseBoolean(System.getProperty("sonar.java.semsitter.frameworkTypes", "true"));

  private boolean noLibraries;

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
      registerProjectMembers(info);
      JTypeSymbol symbol = typesByName.get(entry.getKey());
      if (symbol == null) {
        symbol = new JTypeSymbol(this, entry.getKey(), simpleName(entry.getKey()), false);
        symbol.owner(packageSymbol(packageName(entry.getKey())));
        symbol.type(rawNamed(entry.getKey()));
        typesByName.put(entry.getKey(), symbol);
        symbols.putIfAbsent(entry.getKey(), symbol);
      }
      applyTypeInfo(symbol, info);
    }
  }

  public Set<String> referencedTypeNames() {
    Set<String> names = new HashSet<>();
    for (String id : facts.keySet()) {
      addTypeName(names, id);
    }
    for (JsonObject symbol : dumpSymbols.values()) {
      addTypeName(names, text(symbol, "id"));
      JsonObject type = symbol.getAsJsonObject("facts");
      if (type != null && type.getAsJsonObject("type") != null) {
        collectFactNames(names, type.getAsJsonObject("type"));
      }
    }
    return names;
  }

  private static void addTypeName(Set<String> names, @Nullable String id) {
    if (id == null || id.isEmpty()) {
      return;
    }
    int hash = id.indexOf('#');
    String owner = hash < 0 ? id : id.substring(0, hash);
    if (owner.indexOf('%') >= 0 || owner.indexOf('(') >= 0 || owner.indexOf('<') >= 0 && hash < 0) {
      return;
    }
    String erased = erase(owner);
    if (!erased.isEmpty() && !PRIMITIVES.contains(erased) && erased.indexOf(' ') < 0) {
      names.add(erased);
    }
  }

  private static void collectFactNames(Set<String> names, JsonObject fact) {
    String id = text(fact, "id");
    if (id != null) {
      addTypeName(names, id);
    }
    if (fact.has("args")) {
      for (JsonElement arg : fact.getAsJsonArray("args")) {
        if (arg.isJsonObject()) {
          collectFactNames(names, arg.getAsJsonObject());
        }
      }
    }
    if (fact.has("elem") && fact.get("elem").isJsonObject()) {
      collectFactNames(names, fact.getAsJsonObject("elem"));
    }
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
      return (JType) arraySymbol(element).type();
    }
    if (PRIMITIVES.contains(fullyQualifiedName)) {
      return primitiveType(fullyQualifiedName);
    }
    typeSymbol(fullyQualifiedName);
    return rawNamed(fullyQualifiedName);
  }

  /** Nothing is held open per file; the instance is per sema so cleaners of different files stay distinct. */
  private final Runnable environmentCleaner = new Runnable() {
    @Override
    public void run() {
      // no file or jar stays open
    }
  };

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
      createDumpSymbol(id);
      linkDumpSymbol(id);
      return symbols.getOrDefault(id, unknownSymbol);
    }
    if (id.indexOf('#') >= 0) {
      return createMember(id);
    }
    if (id.indexOf('<') >= 0) {
      return typeParameter(id);
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
    JsonObject info = typeInfos.get(fqn);
    if (info == null && typesLoaded && requestedTypes.add(fqn)) {
      JsonObject loaded = typeLoader.apply(Set.of(fqn));
      if (loaded == null) {
        return created;
      }
      ingestTypes(loaded);
      if (typeInfos.containsKey(fqn)) {
        return created;
      }
    }
    if (info != null) {
      applyTypeInfo(created, info);
    } else if (typesLoaded && !isWellKnown(fqn)) {
      created.unknown(true);
      undefinedTypes.add(JProblem.undefinedType(fqn));
    }
    return created;
  }

  private static boolean isWellKnown(String fqn) {
    return "java.lang.Object".equals(fqn) || "java.lang.Cloneable".equals(fqn) || "java.io.Serializable".equals(fqn);
  }

  Symbol.TypeSymbol arraySymbol(JType element) {
    String name = element.fullyQualifiedName() + "[]";
    JTypeSymbol found = typesByName.get(name);
    if (found != null) {
      return found;
    }
    JTypeSymbol symbol = new JTypeSymbol(this, name, element.name() + "[]", false);
    symbol.elementFqn(element.fullyQualifiedName());
    symbol.owner(Symbol.ROOT_PACKAGE);
    symbol.type(JType.array(this, element));
    typesByName.put(name, symbol);
    return symbol;
  }

  Symbol.TypeSymbol primitiveSymbol(String name) {
    JTypeSymbol found = typesByName.get(name);
    if (found != null) {
      return found;
    }
    JTypeSymbol symbol = new JTypeSymbol(this, name, name, false);
    symbol.owner(Symbol.ROOT_PACKAGE);
    symbol.type(primitiveType(name));
    typesByName.put(name, symbol);
    return symbol;
  }

  List<String> annotationNames(@Nullable String id) {
    return declaredAnnotations(id).stream().map(DeclaredAnnotation::name).toList();
  }

  List<DeclaredAnnotation> declaredAnnotations(@Nullable String id) {
    if (id == null) {
      return List.of();
    }
    List<DeclaredAnnotation> typed = libraryAnnotations.get(id);
    if (typed != null && !typed.isEmpty()) {
      return typed;
    }
    JsonObject fact = facts.get(id);
    return fact == null ? List.of() : strings(fact, ANNOTATIONS).stream().map(name -> new DeclaredAnnotation(erase(name), List.of(), false)).toList();
  }

  /** Thrown types a {@code types} answer gives a project method, or null when it was not answered. */
  @Nullable
  List<Type> projectThrown(@Nullable String id) {
    List<String> names = id == null ? null : projectThrows.get(id);
    return names == null ? null : names.stream().map(this::typeFromName).toList();
  }

  /** The typed annotations and thrown types of the project members of a {@code types} answer. */
  private void registerProjectMembers(JsonObject info) {
    if (!"project".equals(text(info, "origin")) || !(info.get(MEMBERS) instanceof JsonArray members)) {
      return;
    }
    for (JsonElement element : members) {
      if (!(element instanceof JsonObject member) || !(member.get("id") instanceof JsonPrimitive memberId)) {
        continue;
      }
      List<String> spellings = new ArrayList<>(strings(member, "useIds"));
      spellings.add(memberId.getAsString());
      List<DeclaredAnnotation> annotations = readAnnotations(member);
      for (String id : spellings) {
        if (!annotations.isEmpty()) {
          libraryAnnotations.put(id, annotations);
        }
        registerParameterAnnotations(id, member);
        if (member.get(THROWS) instanceof JsonArray) {
          projectThrows.put(id, strings(member, THROWS));
        }
        if (member.get("varargs") instanceof JsonPrimitive varArgs && varArgs.isBoolean()) {
          projectVarArgs.put(id, varArgs.getAsBoolean());
        }
        if (member.get("facts") instanceof JsonObject memberFact) {
          projectMemberFacts.putIfAbsent(id, memberFact);
        }
        if (member.get("declaredType") instanceof JsonObject declared && (declared.get("args") instanceof JsonArray || "typevar".equals(text(declared, "kind")))) {
          projectFieldTypes.putIfAbsent(id, declared);
        }
      }
    }
  }

  /** Annotations written directly on the parameters of a method, as the member's type-annotation facts tell them. */
  private void registerParameterAnnotations(String methodId, JsonObject member) {
    if (!(member.get("facts") instanceof JsonObject facts && facts.get("props") instanceof JsonObject props)) {
      return;
    }
    for (String encoded : strings(props, "type_annotation_values")) {
      JsonElement parsed;
      try {
        parsed = JsonParser.parseString(encoded);
      } catch (JsonParseException e) {
        continue;
      }
      if (!(parsed instanceof JsonArray entries)) {
        continue;
      }
      for (JsonElement entry : entries) {
        if (entry instanceof JsonObject annotation && "param".equals(text(annotation, "on")) && annotation.get("path") instanceof JsonArray path && path.isEmpty()
          && annotation.get("index") instanceof JsonPrimitive index && text(annotation, "id") != null) {
          libraryAnnotations.computeIfAbsent(parameterKey(methodId, index.getAsInt()), key -> new ArrayList<>())
            .add(new DeclaredAnnotation(text(annotation, "id"), List.of()));
        }
      }
    }
  }

  private static String parameterKey(String methodId, int index) {
    return methodId + "@param" + index;
  }

  /** Annotations and deprecation a {@code types} answer gives a library type or member. */
  private void libraryAnnotations(JSymbol symbol, String id, JsonObject info) {
    if (info.get("parameterAnnotations") instanceof JsonArray parameters) {
      for (int i = 0; i < parameters.size(); i++) {
        List<DeclaredAnnotation> onParameter = readAnnotationList(parameters.get(i));
        if (!onParameter.isEmpty()) {
          libraryAnnotations.putIfAbsent(parameterKey(id, i), onParameter);
        }
      }
    }
    List<DeclaredAnnotation> annotations = readAnnotations(info);
    if (annotations.isEmpty()) {
      return;
    }
    libraryAnnotations.put(id, annotations);
    if (annotations.stream().anyMatch(annotation -> "java.lang.Deprecated".equals(annotation.name()))) {
      symbol.deprecated(true);
    }
  }

  /** The annotations of the package {@code name}, from its compiled {@code package-info}. */
  List<DeclaredAnnotation> packageAnnotations(String name) {
    if (!typesLoaded) {
      return List.of();
    }
    List<DeclaredAnnotation> cached = packageAnnotations.get(name);
    if (cached == null) {
      String key = name + ".package-info";
      JsonObject loaded = typeLoader.apply(Set.of(key));
      cached = loaded != null && loaded.get(key) instanceof JsonObject info ? readAnnotations(info) : List.of();
      packageAnnotations.put(name, cached);
    }
    return cached;
  }

  /** Annotations written on the outermost type of a field or return type are annotations of the member. */
  private List<DeclaredAnnotation> readAnnotations(JsonObject info) {
    List<DeclaredAnnotation> annotations = new ArrayList<>(readAnnotationList(info.get(ANNOTATIONS)));
    if (info.get("typeAnnotations") instanceof JsonArray typed) {
      for (JsonElement element : typed) {
        if (element instanceof JsonObject entry && "type".equals(text(entry, "on")) && entry.get("path") instanceof JsonArray path && path.isEmpty()) {
          JsonArray one = new JsonArray();
          one.add(entry);
          for (DeclaredAnnotation annotation : readAnnotationList(one)) {
            if (annotations.stream().noneMatch(known -> known.name().equals(annotation.name()))) {
              annotations.add(annotation);
            }
          }
        }
      }
    }
    return annotations;
  }

  private List<DeclaredAnnotation> readAnnotationList(@Nullable JsonElement list) {
    if (list == null || !list.isJsonArray()) {
      return List.of();
    }
    List<DeclaredAnnotation> annotations = new ArrayList<>();
    for (JsonElement element : list.getAsJsonArray()) {
      if (element.isJsonPrimitive()) {
        annotations.add(new DeclaredAnnotation(element.getAsString(), List.of()));
      } else if (element.isJsonObject()) {
        JsonObject annotation = element.getAsJsonObject();
        String name = text(annotation, "name");
        if (name != null) {
          annotations.add(new DeclaredAnnotation(name, readAnnotationValues(annotation.get("values"))));
        }
      }
    }
    return annotations;
  }

  private List<SymbolMetadata.AnnotationValue> readAnnotationValues(@Nullable JsonElement element) {
    if (!(element instanceof JsonArray array)) {
      return List.of();
    }
    List<SymbolMetadata.AnnotationValue> values = new ArrayList<>();
    for (JsonElement item : array) {
      if (item.isJsonObject()) {
        JsonObject value = item.getAsJsonObject();
        String name = text(value, "name");
        values.add(new AnnotationValueImpl(name == null ? "value" : name, readAnnotationElement(value)));
      }
    }
    return values;
  }

  @Nullable
  private Object readAnnotationElement(JsonObject value) {
    String kind = text(value, "kind");
    JsonElement raw = value.get("value");
    if (kind == null) {
      return null;
    }
    return switch (kind) {
      case "boolean" -> Boolean.valueOf(raw != null && raw.isJsonPrimitive() && raw.getAsBoolean());
      case "byte" -> raw == null || !raw.isJsonPrimitive() ? null : Byte.valueOf(raw.getAsByte());
      case "short" -> raw == null || !raw.isJsonPrimitive() ? null : Short.valueOf(raw.getAsShort());
      case "int" -> raw == null || !raw.isJsonPrimitive() ? null : Integer.valueOf(raw.getAsInt());
      case "long" -> raw == null || !raw.isJsonPrimitive() ? null : Long.valueOf(raw.getAsLong());
      case "float" -> raw == null || !raw.isJsonPrimitive() ? null : Float.valueOf(raw.getAsFloat());
      case "double" -> raw == null || !raw.isJsonPrimitive() ? null : Double.valueOf(raw.getAsDouble());
      case "char" -> raw == null || !raw.isJsonPrimitive() ? null : Character.valueOf((char) raw.getAsInt());
      case "string" -> raw == null || !raw.isJsonPrimitive() ? null : raw.getAsString();
      case "class" -> raw == null || !raw.isJsonPrimitive() ? null : typeSymbol(erase(raw.getAsString()));
      case "enum" -> enumConstant(text(value, "type"), text(value, "const"));
      case "array" -> readAnnotationArray(raw);
      case "annotation" -> raw != null && raw.isJsonObject() ? text(raw.getAsJsonObject(), "name") : null;
      default -> null;
    };
  }

  private Object[] readAnnotationArray(@Nullable JsonElement element) {
    if (!(element instanceof JsonArray array)) {
      return new Object[0];
    }
    List<Object> values = new ArrayList<>();
    for (JsonElement item : array) {
      values.add(item.isJsonObject() ? readAnnotationElement(item.getAsJsonObject()) : null);
    }
    return values.toArray();
  }

  @Nullable
  private Symbol.VariableSymbol enumConstant(@Nullable String typeName, @Nullable String constName) {
    if (constName == null) {
      return null;
    }
    String ownerName = typeName == null || typeName.isBlank() ? "" : erase(typeName);
    String id = (ownerName.isEmpty() ? "enum" : ownerName) + "#" + constName;
    if (symbols.get(id) instanceof Symbol.VariableSymbol variable) {
      return variable;
    }
    JTypeSymbol type = ownerName.isEmpty() ? null : typeSymbol(ownerName);
    JVariableSymbol created = new JVariableSymbol(this, id, constName, "enum_member");
    if (type != null) {
      created.owner(type);
      created.type(type.type());
    }
    created.addModifiers(List.of("public", "static", "final"));
    symbols.put(id, created);
    return created;
  }

  /**
   * Return type of a call. A JDK member can name the class type parameter its erased return
   * stands for ({@code List.get} returns {@code E}), which is taken from the receiver.
   */
  Symbol receiverSymbol(Symbol.TypeSymbol type, String name, Type receiverType) {
    String id = (type instanceof JSymbol symbol && symbol.id != null ? symbol.id : type.name()) + "#" + name;
    JSymbol found = symbols.get(id);
    if (found != null) {
      return found;
    }
    JVariableSymbol variable = new JVariableSymbol(this, id, name, "field");
    variable.owner(type);
    variable.type(receiverType);
    variable.addModifiers(List.of("final"));
    variable.receiver(true);
    symbols.put(id, variable);
    return variable;
  }

  /**
   * The type an invocation of {@code method} evaluates to. A self-typed method returns the receiver, or a
   * capture of it when the receiver leaves its own type argument as a wildcard.
   */
  Type invocationReturn(JMethodSymbol method, @Nullable Type receiver, List<Type> arguments) {
    if (receiver != null && receiver.isArray() && "clone".equals(method.name()) && arguments.isEmpty()) {
      return receiver;
    }
    Type result = invocationReturnType(method, receiver, arguments);
    return withEnclosingArguments(method, receiver, result);
  }

  /** The inner class a member of a parameterized class returns keeps the arguments of its enclosing class. */
  private Type withEnclosingArguments(JMethodSymbol method, @Nullable Type receiver, Type result) {
    if (receiver == null || method.isStatic() || !(result instanceof JType inner) || !inner.isClass() || inner.isParameterized() || method.owner().type() == null) {
      return result;
    }
    String ownerFqn = method.owner().type().erasure().fullyQualifiedName();
    if (!inner.fullyQualifiedName().startsWith(ownerFqn + "$") && !inner.fullyQualifiedName().startsWith(ownerFqn + ".")) {
      return result;
    }
    Type declaring = asSuperType(receiver, ownerFqn, new HashSet<>());
    return declaring instanceof JType enclosing && enclosing.isParameterized() ? inner.withOuter(enclosing) : result;
  }

  private Type invocationReturnType(JMethodSymbol method, @Nullable Type receiver, List<Type> arguments) {
    Integer index = method.returnTypeParameter();
    if (method.returnsSelf() && receiver instanceof JType returned && returned.isCapture()) {
      return returned;
    }
    if (receiver instanceof JType captured && captured.isCapture() && captured.element != null && captured.element.isClass() && !method.isStatic()) {
      // a member of a capture is a member of its bound
      return invocationReturnType(method, captured.element, arguments);
    }
    Type declaring = index == null || receiver == null ? null : declaringType(method, receiver);
    if (declaring != null && declaring.isParameterized() && declaring.typeArguments().size() > index) {
      Type argument = declaring.typeArguments().get(index);
      if (method.returnsSelf() && argument instanceof JType captured && captured.isCapture() && receiver instanceof JType self && self.isClass()) {
        return JType.capture(this, self);
      }
      if (!argument.isTypeVar()) {
        return argument;
      }
    }
    if (method.returnsSelf() && receiver instanceof JType returned && returned.isClass()) {
      boolean wildcard = returned.typeArguments().stream().anyMatch(argument -> argument instanceof JType type && type.isCapture());
      return wildcard ? JType.capture(this, returned) : returned;
    }
    JsonObject declaredReturn = declaredReturn(method);
    if (declaredReturn != null) {
      Map<String, Type> classSubstitution = receiver == null ? Map.of() : classSubstitution(method, receiver);
      Map<String, Type> substitution = new HashMap<>(methodSubstitution(method, arguments));
      substitution.putAll(classSubstitution);
      if (!substitution.isEmpty() || method.isStatic()) {
        JType substituted = typeFromFact(declaredReturn, substitution, true);
        boolean leaks = classSubstitution.isEmpty() ? mentionsTypeVariable(substituted) : mentionsMethodTypeVariable(method, substituted);
        if (!substituted.isUnknown() && !leaks) {
          return substituted;
        }
      }
    }
    if (declaredReturn != null && !method.isStatic()) {
      // a method with no type variables in play still returns the type arguments its signature spells out
      JType plain = typeFromFact(declaredReturn, Map.of(), true);
      if (!plain.isUnknown() && plain.isParameterized() && !mentionsTypeVariable(plain)) {
        return plain;
      }
    }
    Type declared = method.returnType().type();
    if ((declared == null || declared.isUnknown()) && declaredReturn != null) {
      // the declaration comes later in the file than this call
      JType plain = typeFromFact(declaredReturn, Map.of(), true);
      if (!plain.isUnknown() && !mentionsMethodTypeVariable(method, plain)) {
        return plain;
      }
    }
    return declared == null ? Type.UNKNOWN : declared;
  }

  /** The erased return type of an instance method of a generic class called on a raw receiver, or null when it does not apply. */
  @Nullable
  Type rawReturn(JMethodSymbol method) {
    JsonObject declaredReturn = declaredReturn(method);
    if (declaredReturn == null || method.isStatic() || !hasClassTypeVariables(method)) {
      return null;
    }
    JType plain = typeFromFact(declaredReturn, Map.of(), true);
    if (plain.isTypeVar() && !method.returnType().type().isUnknown()) {
      // the descriptor erases a type variable to its bound
      return method.returnType().type();
    }
    return plain.isUnknown() ? null : plain.erasure();
  }

  private boolean hasClassTypeVariables(JMethodSymbol method) {
    JsonObject info = typeInfos.get(sourceName(method.owner().type().erasure().fullyQualifiedName()));
    return info != null && !strings(info, "typeVars").isEmpty();
  }

  /**
   * The thrown types of a call when the method throws a type variable, taken from the arguments the variable is
   * inferred from, or null when the erased thrown types stand.
   */
  @Nullable
  List<Type> invocationThrown(JMethodSymbol method, @Nullable Type receiver, List<Type> arguments) {
    JsonObject member = method.isUnknown() ? null : lookupGenericMember(method);
    if (member == null || !(member.get("throwsTypes") instanceof JsonArray facts) || facts.isEmpty()) {
      return null;
    }
    Map<String, Type> substitution = new HashMap<>(methodSubstitution(method, arguments));
    if (receiver != null) {
      substitution.putAll(classSubstitution(method, receiver));
    }
    List<Type> erased = method.thrownTypes();
    List<Type> result = new ArrayList<>();
    boolean changed = false;
    for (int i = 0; i < facts.size(); i++) {
      Type fallback = i < erased.size() ? erased.get(i) : Type.UNKNOWN;
      Type inferred = facts.get(i) instanceof JsonObject fact && "typevar".equals(text(fact, "kind")) ? typeFromFact(fact, substitution, true) : null;
      if (inferred == null || inferred.isUnknown() || mentionsTypeVariable(inferred)) {
        result.add(fallback);
      } else {
        result.add(inferred);
        changed = true;
      }
    }
    return changed ? result : null;
  }

  /** Maps the type variables of {@code method} itself to the arguments they are inferred from. */
  private Map<String, Type> methodSubstitution(JMethodSymbol method, List<Type> arguments) {
    JsonObject member = arguments.isEmpty() && explicitTypeArguments.isEmpty() ? null : lookupGenericMember(method);
    if (member == null || !(member.get("typeVars") instanceof JsonArray names)) {
      return Map.of();
    }
    List<String> variables = parameterVariables(method);
    Map<String, Type> substitution = new HashMap<>();
    for (int i = 0; i < names.size(); i++) {
      Type inferred = methodArgument(variables, "m" + i, arguments);
      if (inferred != null && !inferred.isUnknown()) {
        substitution.put(names.get(i).getAsString(), inferred);
      }
    }
    Set<String> own = new HashSet<>();
    names.forEach(name -> own.add(name.getAsString()));
    List<JsonObject> declared = declaredParameters(method);
    for (int i = 0; i < declared.size() && i < arguments.size(); i++) {
      bindFromType(declared.get(i), arguments.get(i), own, substitution);
    }
    return substitution;
  }

  /**
   * The type {@code method} returns when the type its call is passed as fixes the type variables of the
   * method, or null when they stay open.
   */
  @Nullable
  Type targetedReturn(JMethodSymbol method, Type target) {
    JsonObject member = lookupGenericMember(method);
    JsonObject declared = declaredReturn(method);
    if (member == null || declared == null || !(member.get("typeVars") instanceof JsonArray names) || names.isEmpty()) {
      return null;
    }
    Set<String> own = new HashSet<>();
    names.forEach(name -> own.add(name.getAsString()));
    Map<String, Type> bound = new HashMap<>();
    bindReturnToTarget(declared, target, own, bound);
    if (bound.isEmpty()) {
      return null;
    }
    JType result = typeFromFact(declared, bound);
    return result.isUnknown() || mentionsTypeVariable(result) ? null : result;
  }

  /**
   * The result of a call that returns a bare method type variable, such as {@code Stream<E>.collect(collector)}, when
   * the type it is passed as takes a supertype of the element where the result carries it: a collector factory leaves
   * its element free, so {@code Collection<CharSequence>} fixes the result of a stream of {@code String} to
   * {@code List<CharSequence>}. Null when the result stands.
   */
  @Nullable
  Type retargetedElement(JMethodSymbol method, Type receiver, Type returned, Type target) {
    JsonObject declared = declaredReturn(method);
    JsonObject member = method.isUnknown() ? null : lookupGenericMember(method);
    if (declared == null || member == null || !"typevar".equals(text(declared, "kind"))
      || !(receiver instanceof JType owner) || owner.args.isEmpty()
      || !(returned instanceof JType result) || !result.isParameterized() || !(target instanceof JType wantedType) || !wantedType.isParameterized()) {
      return null;
    }
    JType element = owner.args.get(0);
    if (element.isWildcard() || element.isUnknown() || element.isParameterized() || !element.isClass()) {
      return null;
    }
    int open = -1;
    for (int i = 0; i < result.args.size(); i++) {
      if (result.args.get(i).fullyQualifiedName().equals(element.fullyQualifiedName())) {
        if (open >= 0) {
          return null;
        }
        open = i;
      }
    }
    Type aligned = open < 0 ? null : asSuperType(result, wantedType.erasure().fullyQualifiedName(), new HashSet<>());
    if (aligned == null || !aligned.isParameterized() || aligned.typeArguments().size() != wantedType.args.size()) {
      return null;
    }
    for (int k = 0; k < wantedType.args.size(); k++) {
      JType wanted = wantedType.args.get(k);
      if (aligned.typeArguments().get(k).fullyQualifiedName().equals(element.fullyQualifiedName())
        && !wanted.isWildcard() && !wanted.isUnknown() && !wanted.isParameterized() && wanted.isClass()
        && !wanted.fullyQualifiedName().equals(element.fullyQualifiedName()) && element.isSubtypeOf(wanted)) {
        List<JType> arguments = new ArrayList<>(result.args);
        arguments.set(open, wanted);
        return JType.named(this, result.erasure().fullyQualifiedName(), arguments);
      }
    }
    return null;
  }

  /**
   * The functional interface {@code parameter} with only the type argument its method returns known, as the type a
   * function that produces {@code produced} has there, or null when {@code parameter} is not such an interface.
   */
  @Nullable
  Type functionProducing(Type parameter, Type produced) {
    if (!(parameter instanceof JType declared) || !declared.isParameterized() || produced.isUnknown() || produced.isTypeVar() || mentionsTypeVariable(produced)) {
      return null;
    }
    JMethodSymbol functional = functionalMethod(declared);
    Integer position = functional == null ? null : returnedClassVariable(functional, declared);
    if (position == null || position >= declared.args.size()) {
      return null;
    }
    List<JType> arguments = new ArrayList<>();
    for (int i = 0; i < declared.args.size(); i++) {
      arguments.add(i == position ? asJType(produced.isPrimitive() ? produced.primitiveWrapperType() : produced) : JType.unknown(this));
    }
    return JType.inferred(this, declared.erasure().fullyQualifiedName(), arguments);
  }

  /**
   * The parameter types of {@code method} when the type its call is passed as fixes the type variables of the
   * method, or null when they stay open.
   */
  @Nullable
  List<Type> targetedParameters(JMethodSymbol method, Type target) {
    JsonObject member = lookupGenericMember(method);
    JsonObject declared = declaredReturn(method);
    if (member == null || declared == null || !(member.get("typeVars") instanceof JsonArray names) || names.isEmpty()) {
      return null;
    }
    Set<String> own = new HashSet<>();
    names.forEach(name -> own.add(name.getAsString()));
    Map<String, Type> bound = new HashMap<>();
    bindReturnToTarget(declared, target, own, bound);
    List<JsonObject> parameters = declaredParameters(method);
    if (bound.isEmpty() || parameters.isEmpty()) {
      return null;
    }
    List<Type> result = new ArrayList<>();
    for (JsonObject parameter : parameters) {
      JType type = typeFromFact(parameter, bound);
      result.add(type.isUnknown() || mentionsTypeVariable(type) ? Type.UNKNOWN : type);
    }
    return result;
  }

  /**
   * Binds the type variables a declared return type names from the type the call is passed as. The target may be a
   * supertype of the declared type: its arguments are read through the supertype of the declared type it names.
   */
  private void bindReturnToTarget(JsonObject declared, Type target, Set<String> variables, Map<String, Type> bound) {
    bindFromType(declared, target, variables, bound);
    if (!bound.isEmpty() || !(target instanceof JType expected) || !expected.isParameterized() || declared.get("args") == null) {
      return;
    }
    JType written = typeFromFact(declared);
    Type aligned = written.isParameterized() ? asSuperType(written, expected.erasure().fullyQualifiedName(), new HashSet<>()) : null;
    if (aligned == null || !aligned.isParameterized() || aligned.typeArguments().size() != expected.args.size()) {
      return;
    }
    for (int i = 0; i < expected.args.size(); i++) {
      Type argument = aligned.typeArguments().get(i);
      JType given = expected.args.get(i);
      if (argument.isTypeVar() && variables.contains(argument.name()) && !given.isWildcard() && !given.isUnknown()) {
        bound.putIfAbsent(argument.name(), given);
      }
    }
  }

  /**
   * Binds the type variables of {@code variables} that the declared type {@code declared} names to the types
   * {@code actual} gives them at the same position. Open type variables and unbounded wildcards bind nothing.
   */
  private void bindFromType(JsonObject declared, Type actual, Set<String> variables, Map<String, Type> bound) {
    Type known = actual;
    while (known instanceof JType wildcard && wildcard.isWildcard()) {
      JType ground = wildcard.groundType();
      known = ground == wildcard.element && !ground.is("java.lang.Object") ? JType.capture(this, ground) : ground;
    }
    if (known.isUnknown() || known.isNullType() || known.isTypeVar() || (actual != known && known.is("java.lang.Object"))) {
      return;
    }
    String kind = text(declared, "kind");
    if ("typevar".equals(kind)) {
      String id = text(declared, "id");
      if (variables.contains(id)) {
        bound.putIfAbsent(id, known.isPrimitive() ? known.primitiveWrapperType() : known);
      }
    } else if ("wildcard".equals(kind)) {
      JsonObject limit = declared.get("super") instanceof JsonObject lower ? lower : declared.get("extends") instanceof JsonObject upper ? upper : null;
      if (limit != null) {
        bindFromType(limit, known, variables, bound);
      }
    } else if (ARRAY.equals(kind) && declared.get("elem") instanceof JsonObject element && known instanceof Type.ArrayType array) {
      bindFromType(element, array.elementType(), variables, bound);
    } else if (declared.get("args") instanceof JsonArray arguments && !arguments.isEmpty() && text(declared, "id") != null) {
      Type aligned = asSuperType(known, erase(text(declared, "id")), new HashSet<>());
      if (aligned == null || !aligned.isParameterized() || aligned.typeArguments().size() != arguments.size()) {
        return;
      }
      for (int i = 0; i < arguments.size(); i++) {
        if (arguments.get(i) instanceof JsonObject argument) {
          bindFromType(argument, aligned.typeArguments().get(i), variables, bound);
        }
      }
    }
  }

  private static boolean mentionsTypeVariable(Type type) {
    return mentionsTypeVariable(type, name -> true);
  }

  private boolean mentionsMethodTypeVariable(JMethodSymbol method, Type type) {
    JsonObject member = lookupGenericMember(method);
    if (member == null || !(member.get("typeVars") instanceof JsonArray names) || names.isEmpty()) {
      return false;
    }
    Set<String> own = new HashSet<>();
    names.forEach(name -> own.add(name.getAsString()));
    return mentionsTypeVariable(type, own::contains);
  }

  private static boolean mentionsTypeVariable(Type type, Predicate<String> variable) {
    if (type.isTypeVar()) {
      return variable.test(type.name());
    }
    if (type.isArray()) {
      return mentionsTypeVariable(((Type.ArrayType) type).elementType(), variable);
    }
    return type.isParameterized() && type.typeArguments().stream().anyMatch(argument -> mentionsTypeVariable(argument, variable));
  }

  /** {@code declared}, written in terms of the type variables of {@code declaring}'s class, as it reads for that parameterization. */
  Type asMemberOf(Type declared, Type declaring) {
    if (!(declared instanceof JType type) || !(declaring instanceof JType owner) || !owner.isParameterized()) {
      return declared;
    }
    return type.substitute(typeArgumentsOf(owner));
  }

  /** The type of {@code field} read through {@code receiver}: the owner's type variables take the receiver's type arguments. */
  Type fieldTypeOn(Symbol.VariableSymbol field, @Nullable Type receiver) {
    Type declared = field.type();
    if (!(declared instanceof JType type) || receiver == null || field.isStatic() || !(field.owner() instanceof Symbol.TypeSymbol owner) || owner.type() == null) {
      return declared;
    }
    String ownerFqn = owner.type().erasure().fullyQualifiedName();
    JsonObject info = typeInfos.get(sourceName(ownerFqn));
    List<String> names = info == null ? List.of() : strings(info, "typeVars");
    Type declaring = names.isEmpty() ? null : asSuperType(receiver, ownerFqn, new HashSet<>());
    if (declaring == null || declaring.typeArguments().isEmpty()) {
      return declared;
    }
    Map<String, Type> substitution = new HashMap<>();
    for (int i = 0; i < names.size() && i < declaring.typeArguments().size(); i++) {
      substitution.put(names.get(i), declaring.typeArguments().get(i));
    }
    return type.substitute(substitution);
  }

  /** The supertype of {@code receiver} that declares {@code method}, with the receiver's type arguments applied. */
  @Nullable
  private Type declaringType(JMethodSymbol method, Type receiver) {
    Type owner = method.owner().type();
    if (owner == null || owner.isUnknown()) {
      return receiver;
    }
    Type found = asSuperType(receiver, owner.erasure().fullyQualifiedName(), new HashSet<>());
    return found == null ? receiver : found;
  }

  /**
   * The parameterization of {@code created} a diamond infers from {@code target}: each type parameter of the created
   * class takes the target argument it is passed to through the supertype matching the target.
   */
  Type inferDiamond(Type created, Type target) {
    if (!(created.erasure() instanceof JType raw) || raw.isUnknown() || !(target instanceof JType expected) || !expected.isParameterized()) {
      return created;
    }
    String targetFqn = expected.erasure().fullyQualifiedName();
    if (raw.fullyQualifiedName().equals(targetFqn)) {
      return JType.named(this, raw.fullyQualifiedName(), expected.args);
    }
    Type generic = raw.declaringType();
    Type declared = generic.isParameterized() ? asSuperType(generic, targetFqn, new HashSet<>()) : null;
    if (declared == null || declared.typeArguments().size() != expected.args.size()) {
      return created;
    }
    Map<String, JType> inferred = new HashMap<>();
    for (int i = 0; i < expected.args.size(); i++) {
      if (declared.typeArguments().get(i).isTypeVar()) {
        inferred.put(declared.typeArguments().get(i).fullyQualifiedName(), expected.args.get(i));
      }
    }
    List<JType> arguments = new ArrayList<>();
    for (Type variable : generic.typeArguments()) {
      JType argument = inferred.get(variable.fullyQualifiedName());
      if (argument == null) {
        return created;
      }
      arguments.add(argument);
    }
    return JType.named(this, raw.fullyQualifiedName(), arguments);
  }

  /**
   * The parameterization of {@code created} a diamond infers from the arguments of its constructor: each type
   * parameter of the class takes the argument type it is passed as, or {@code created} when one stays open.
   */
  Type inferDiamondFromArguments(JMethodSymbol constructor, Type created, List<Type> arguments) {
    if (!(created.erasure() instanceof JType raw) || raw.isUnknown() || created.isParameterized()) {
      return created;
    }
    JsonObject info = typeInfos.get(sourceName(raw.fullyQualifiedName()));
    List<String> names = info == null ? List.of() : strings(info, "typeVars");
    List<JsonObject> declared = names.isEmpty() ? List.of() : declaredParameters(constructor);
    if (declared.isEmpty() || declared.size() != arguments.size()) {
      return created;
    }
    Set<String> own = new HashSet<>(names);
    Map<String, Type> bound = new HashMap<>();
    for (int i = 0; i < declared.size(); i++) {
      bindFromType(declared.get(i), arguments.get(i), own, bound);
    }
    List<JType> inferred = new ArrayList<>();
    for (String name : names) {
      Type argument = bound.get(name);
      if (argument == null || argument.isUnknown()) {
        return created;
      }
      inferred.add(asJType(argument));
    }
    return JType.named(this, raw.fullyQualifiedName(), inferred);
  }

  /** The type a lambda or method reference implements for a wildcard-parameterized target (JLS 9.9). */
  Type groundType(Type target) {
    if (!(target instanceof JType type) || !type.isParameterized() || type.args.stream().noneMatch(JType::isWildcard)) {
      return target;
    }
    return JType.named(this, type.fullyQualifiedName(), type.args.stream().map(JType::groundType).toList());
  }

  /** Capture conversion: a member is accessed through captures of the receiver's wildcard arguments. */
  private Type captureWildcards(Type receiver) {
    if (!(receiver instanceof JType type) || !type.isParameterized() || type.args.stream().noneMatch(JType::isWildcard)) {
      return receiver;
    }
    List<JType> captured = type.args.stream().map(argument -> argument.isWildcard() && argument.element != null ? JType.capture(this, argument.element) : argument).toList();
    return JType.named(this, type.fullyQualifiedName(), captured);
  }

  /**
   * Whether {@code method} declares type variables of its own. A constructor, whose diamond infers them, and a
   * method whose declaration is unknown count when their types are substituted.
   */
  private boolean declaresTypeVariables(JMethodSymbol method, boolean substituted) {
    JsonObject member = "<init>".equals(method.name()) ? null : lookupGenericMember(method);
    if (member == null) {
      return substituted;
    }
    return member.get("typeVars") instanceof JsonArray names && !names.isEmpty();
  }

  /** The method as seen from {@code receiver}, so its owner keeps that parameterization. */
  JMethodSymbol invocationView(JMethodSymbol method, @Nullable Type receiver, List<Type> parameters, Type returned) {
    return invocationView(method, receiver, parameters, returned, null);
  }

  /** As above, with the thrown types the call infers when the method throws a type variable. */
  JMethodSymbol invocationView(JMethodSymbol method, @Nullable Type receiver, List<Type> parameters, Type returned, @Nullable List<Type> thrown) {
    boolean substituted = method.substitutes(parameters, returned) || thrown != null;
    JMethodSymbol view = method.withSignature(parameters, returned);
    if (thrown != null) {
      view.thrownTypes(thrown);
    }
    if (!view.isParametrizedMethod() && declaresTypeVariables(method, substituted)) {
      view.parametrized(true);
    }
    if (receiver != null && !method.isStatic() && method.owner().type() != null) {
      Type declaring = asSuperType(captureWildcards(receiver), method.owner().type().erasure().fullyQualifiedName(), new HashSet<>());
      if (declaring != null && declaring.isParameterized()) {
        view.owner(declaring.symbol());
      }
    }
    boolean same = !substituted && view.owner() == method.owner() && view.isParametrizedMethod() == method.isParametrizedMethod();
    return same ? method : view;
  }

  /**
   * The parameter types of {@code method} at one invocation: type variables of the owner are replaced by
   * the receiver's type arguments, and type variables of the method by the boxed type of the first argument
   * they are inferred from.
   */
  List<Type> invocationParameters(JMethodSymbol method, @Nullable Type receiver, List<Type> arguments) {
    List<Type> parameters = method.parameterTypes();
    List<String> variables = parameterVariables(method);
    if (variables.size() != parameters.size()) {
      return parameters;
    }
    List<Type> result = new ArrayList<>(parameters);
    for (int i = 0; i < variables.size(); i++) {
      String variable = variables.get(i);
      if (variable.isEmpty()) {
        continue;
      }
      String base = variable.replace("[]", "");
      Type bound = base.charAt(0) == 'c'
        ? classArgument(method, receiver, Integer.parseInt(base.substring(1)))
        : methodArgument(variables, base, arguments);
      if (bound != null) {
        JType type = asJType(bound);
        for (int dims = (variable.length() - base.length()) / 2; dims > 0; dims--) {
          type = JType.array(this, type);
        }
        result.set(i, type);
      }
    }
    substituteDeclaredParameters(method, receiver, variables, result);
    return result;
  }

  /**
   * Fills in the parameters that are not a type variable on their own but are written in terms of one,
   * such as {@code Consumer<T>}. Their declared form is substituted for the arguments the receiver gives
   * the declaring type, which may be a supertype of the receiver's own type. Without such arguments, a declared
   * parameterized type that names no type variable is taken as written.
   */
  private void substituteDeclaredParameters(JMethodSymbol method, @Nullable Type receiver, List<String> variables, List<Type> result) {
    List<JsonObject> declared = declaredParameters(method);
    if (declared.size() != result.size()) {
      return;
    }
    Map<String, Type> substitution = receiver == null ? Map.of() : classSubstitution(method, receiver);
    for (int i = 0; i < declared.size(); i++) {
      if (variables.isEmpty() || variables.get(i).isEmpty()) {
        JType type = typeFromFact(declared.get(i), substitution);
        if (!substitution.isEmpty() || (type.isParameterized() && !mentionsTypeVariable(type))) {
          result.set(i, type);
        }
      }
    }
  }

  /** Maps the type variables of the type declaring {@code method} to the arguments {@code receiver} gives them. */
  private Map<String, Type> classSubstitution(JMethodSymbol method, Type receiver) {
    String ownerFqn = method.owner().type().erasure().fullyQualifiedName();
    JsonObject info = typeInfos.get(sourceName(ownerFqn));
    List<String> names = info == null ? List.of() : strings(info, "typeVars");
    Map<String, Type> substitution = new HashMap<>();
    if (receiver instanceof JType inner && inner.outer() != null && inner.erasure().fullyQualifiedName().equals(ownerFqn)) {
      substitution.putAll(enclosingSubstitution(inner.outer()));
    }
    Type declaring = names.isEmpty() ? null : asSuperType(receiver, ownerFqn, new HashSet<>());
    if (declaring != null && declaring.isParameterized()) {
      List<Type> arguments = declaring.typeArguments();
      for (int i = 0; i < names.size() && i < arguments.size(); i++) {
        substitution.put(names.get(i), arguments.get(i));
      }
    }
    return substitution;
  }

  private Map<String, Type> enclosingSubstitution(JType enclosing) {
    JsonObject info = typeInfos.get(sourceName(enclosing.erasure().fullyQualifiedName()));
    List<String> names = info == null ? List.of() : strings(info, "typeVars");
    Map<String, Type> substitution = new HashMap<>();
    for (int i = 0; i < names.size() && i < enclosing.args.size(); i++) {
      substitution.put(names.get(i), enclosing.args.get(i));
    }
    return substitution;
  }

  /** Whether {@code from}, read as {@code to}'s class, gives it the type arguments {@code to} names; a wildcard argument takes any. */
  boolean preservesArguments(Type from, Type to) {
    if (!to.isParameterized()) {
      return true;
    }
    Type aligned = asSuperType(from, to.erasure().fullyQualifiedName(), new HashSet<>());
    if (aligned == null || !aligned.isParameterized() || aligned.typeArguments().size() != to.typeArguments().size()) {
      return false;
    }
    for (int i = 0; i < to.typeArguments().size(); i++) {
      Type written = to.typeArguments().get(i);
      if (!(written instanceof JType type && type.isWildcard()) && !written.equals(aligned.typeArguments().get(i))) {
        return false;
      }
    }
    return true;
  }

  /** The instantiation of {@code targetFqn} reached from {@code from}, which is itself or one of its supertypes. */
  @Nullable
  private Type asSuperType(Type from, String targetFqn, Set<String> visited) {
    if (from.erasure().fullyQualifiedName().equals(targetFqn)) {
      return from;
    }
    Symbol.TypeSymbol symbol = from.symbol();
    if (symbol.isUnknown() || !visited.add(from.erasure().fullyQualifiedName())) {
      return null;
    }
    List<Type> supertypes = new ArrayList<>(symbol.interfaces());
    Type superClass = symbol.superClass();
    if (superClass != null) {
      supertypes.add(0, superClass);
    }
    for (Type supertype : supertypes) {
      Type found = asSuperType(instantiateSupertype(from, supertype), targetFqn, visited);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  /** Applies {@code from}'s type arguments to a supertype written with those type variables. */
  Type instantiateSupertype(Type from, Type supertype) {
    if (!from.isParameterized() || !(from instanceof JType fromType) || !(supertype instanceof JType superType)) {
      return supertype;
    }
    return superType.substitute(typeArgumentsOf(fromType));
  }

  private Map<String, Type> typeArgumentsOf(JType type) {
    JsonObject info = typeInfos.get(sourceName(type.erasure().fullyQualifiedName()));
    List<String> names = info == null ? List.of() : strings(info, "typeVars");
    List<Type> arguments = type.typeArguments();
    Map<String, Type> substitution = new HashMap<>();
    for (int i = 0; i < names.size() && i < arguments.size(); i++) {
      substitution.put(names.get(i), arguments.get(i));
    }
    return substitution;
  }

  @Nullable
  private JsonObject declaredReturn(JMethodSymbol method) {
    if (method.declaredReturnLoaded()) {
      return method.declaredReturn();
    }
    JsonObject member = lookupGenericMember(method);
    JsonObject retType = member != null && member.get("retType") instanceof JsonObject fact ? fact : null;
    method.declaredReturn(retType);
    return retType;
  }

  private List<JsonObject> declaredParameters(JMethodSymbol method) {
    List<JsonObject> cached = method.declaredParameters();
    if (cached == null) {
      JsonObject member = lookupGenericMember(method);
      cached = member == null ? List.of() : objects(member, "paramTypes");
      method.declaredParameters(cached);
    }
    return cached;
  }

  @Nullable
  private static Type classArgument(JMethodSymbol method, @Nullable Type receiver, int index) {
    if (receiver == null || !receiver.isParameterized() || receiver.typeArguments().size() <= index
      || !receiver.erasure().equals(method.owner().type().erasure())) {
      return null;
    }
    return receiver.typeArguments().get(index);
  }

  private List<Type> explicitTypeArguments = List.of();

  /** Runs {@code action} with the type arguments an invocation spells out, which fix the type variables of its method. */
  <T> T withExplicitTypeArguments(List<Type> explicit, java.util.function.Supplier<T> action) {
    List<Type> previous = explicitTypeArguments;
    explicitTypeArguments = explicit;
    try {
      return action.get();
    } finally {
      explicitTypeArguments = previous;
    }
  }

  @Nullable
  private Type methodArgument(List<String> variables, String base, List<Type> arguments) {
    if (base.charAt(0) == 'm' && !explicitTypeArguments.isEmpty()) {
      try {
        int index = Integer.parseInt(base.substring(1));
        if (index < explicitTypeArguments.size() && !explicitTypeArguments.get(index).isUnknown()) {
          return explicitTypeArguments.get(index);
        }
      } catch (NumberFormatException e) {
        // not a method type variable
      }
    }
    for (int i = 0; i < variables.size() && i < arguments.size(); i++) {
      String variable = variables.get(i);
      if (!variable.replace("[]", "").equals(base)) {
        continue;
      }
      Type argument = arguments.get(i);
      if (!variable.equals(base)) {
        boolean last = i == variables.size() - 1;
        // an array of a primitive type cannot be the array of a type variable: it is one element of the variable arity
        if (arguments.size() == variables.size() && argument.isArray() && !((Type.ArrayType) argument).elementType().isPrimitive()) {
          argument = ((Type.ArrayType) argument).elementType();
        } else if (!last) {
          continue;
        }
      }
      if (!argument.isUnknown() && !argument.isNullType()) {
        return argument.isPrimitive() ? argument.primitiveWrapperType() : argument;
      }
    }
    return null;
  }

  /** The type {@code declaring} gives the class type variable that the parameter {@code index} of the library {@code method} is declared as. */
  @Nullable
  Type classVariableArgument(JMethodSymbol method, int index, Type declaring) {
    List<JsonObject> parameters = declaredParameters(method);
    if (index >= parameters.size() || !"typevar".equals(text(parameters.get(index), "kind")) || !declaring.isParameterized()) {
      return null;
    }
    JsonObject info = typeInfos.get(sourceName(declaring.erasure().fullyQualifiedName()));
    int position = info == null ? -1 : strings(info, "typeVars").indexOf(text(parameters.get(index), "id"));
    return position < 0 || position >= declaring.typeArguments().size() ? null : declaring.typeArguments().get(position);
  }

  /** The position, among the type variables of {@code declaring}, of the one the library {@code method} returns. */
  @Nullable
  Integer returnedClassVariable(JMethodSymbol method, Type declaring) {
    JsonObject returned = declaredReturn(method);
    if (returned == null || !"typevar".equals(text(returned, "kind"))) {
      return null;
    }
    JsonObject info = typeInfos.get(sourceName(declaring.erasure().fullyQualifiedName()));
    int position = info == null ? -1 : strings(info, "typeVars").indexOf(text(returned, "id"));
    return position < 0 ? null : position;
  }

  /**
   * The annotations written on type argument {@code position} of the supertype {@code declaring}, in the supertype clause
   * of {@code owner} or of one of its supertypes ({@code implements StatementCallback<@Nullable Object>}).
   */
  List<DeclaredAnnotation> supertypeArgumentAnnotations(Type owner, Type declaring, int position) {
    return supertypeArgumentAnnotations(sourceName(owner.erasure().fullyQualifiedName()), sourceName(declaring.erasure().fullyQualifiedName()), position, new HashSet<>());
  }

  private List<DeclaredAnnotation> supertypeArgumentAnnotations(String name, String declaring, int position, Set<String> visited) {
    JsonObject info = typeInfos.get(name);
    if (info == null || !visited.add(name)) {
      return List.of();
    }
    List<String> interfaces = strings(info, "interfaces");
    if (interfaces.isEmpty() && "interface".equals(text(info, "kind"))) {
      interfaces = strings(info, "super");
    }
    if (info.get("typeAnnotations") instanceof JsonArray typed) {
      for (JsonElement element : typed) {
        if (element instanceof JsonObject entry && isArgumentOf(entry, position)) {
          String on = text(entry, "on");
          String parent = "super".equals(on) ? text(info, "super") : "interface".equals(on) && entry.get("index") instanceof JsonPrimitive index
            && index.getAsInt() >= 0 && index.getAsInt() < interfaces.size() ? interfaces.get(index.getAsInt()) : null;
          if (parent != null && sourceName(erase(parent)).equals(declaring)) {
            JsonArray one = new JsonArray();
            one.add(entry);
            return readAnnotationList(one);
          }
        }
      }
    }
    for (String parent : parents(info)) {
      List<DeclaredAnnotation> found = supertypeArgumentAnnotations(sourceName(erase(parent)), declaring, position, visited);
      if (!found.isEmpty()) {
        return found;
      }
    }
    return List.of();
  }

  private static boolean isArgumentOf(JsonObject entry, int position) {
    return entry.get("path") instanceof JsonArray path && path.size() == 1 && path.get(0).isJsonPrimitive() && ("<" + position + ">").equals(path.get(0).getAsString());
  }

  private List<String> parameterVariables(JMethodSymbol method) {
    List<String> cached = method.parameterVariables();
    if (cached == null) {
      cached = lookupParameterVariables(method);
      method.parameterVariables(cached);
    }
    return cached;
  }

  private List<String> lookupParameterVariables(JMethodSymbol method) {
    JsonObject member = lookupGenericMember(method);
    if (member == null) {
      return List.of();
    }
    List<String> variables = new ArrayList<>();
    member.getAsJsonArray("paramVars").forEach(v -> variables.add(v.getAsString()));
    return variables;
  }

  /** The loaded declaration of {@code method}, when its declaring type is known and carries a generic signature. */
  @Nullable
  private JsonObject lookupGenericMember(JMethodSymbol method) {
    String ownerFqn = method.owner().type().erasure().fullyQualifiedName();
    if (!declaresType(ownerFqn)) {
      return null;
    }
    JsonArray members = typeInfos.get(sourceName(ownerFqn)).getAsJsonArray(MEMBERS);
    if (members == null) {
      return null;
    }
    List<Type> parameters = method.parameterTypes();
    for (JsonElement element : members) {
      JsonObject member = element.getAsJsonObject();
      String descriptor = text(member, "desc");
      if (method.name().equals(text(member, "name")) && descriptor != null && member.has("paramVars") && member.get("paramVars").isJsonArray()
        && sameParameters(parameters, parameterTypesOf(descriptor))) {
        return member;
      }
    }
    return null;
  }

  /** Whether the parameters match the descriptor's erasures; a type variable, which the symbol cannot name, stands for what the descriptor erases it to. */
  private static boolean sameParameters(List<Type> parameters, List<Type> descriptor) {
    if (parameters.size() != descriptor.size()) {
      return false;
    }
    for (int i = 0; i < parameters.size(); i++) {
      Type parameter = parameters.get(i);
      String name = parameter.erasure().fullyQualifiedName();
      boolean variable = parameter.isUnknown() || parameter.isTypeVar() || name.contains("::");
      if (!variable && !name.equals(descriptor.get(i).erasure().fullyQualifiedName())) {
        return false;
      }
    }
    return true;
  }

  Symbol memberFromTypeInfo(JTypeSymbol owner, JsonObject member) {
    if (member.has("id") && !member.get("id").isJsonNull()) {
      JSymbol project = resolve(member.get("id").getAsString());
      if (declaredAnnotations(project.id).stream().anyMatch(annotation -> "java.lang.Deprecated".equals(annotation.name()))) {
        project.deprecated(true);
      }
      return project;
    }
    String name = text(member, "name");
    String descriptor = text(member, "desc");
    String id = (owner.id == null ? owner.name() : owner.id) + "#" + name + (descriptor == null ? "" : descriptor);
    JSymbol existing = symbols.get(id);
    if (existing != null) {
      return existing;
    }
    if ("f".equals(text(member, "k"))) {
      JVariableSymbol field = new JVariableSymbol(this, id, name == null ? "" : name, "field");
      field.owner(owner);
      if (descriptor != null) {
        field.type(readDescriptor(descriptor, new int[] {0}));
      }
      applyMemberFlags(field, member);
      libraryAnnotations(field, id, member);
      if (truth(member, "enum")) {
        field.enumSymbol(true);
      }
      String constant = text(member, "constant");
      if (constant != null) {
        field.constantValue(constantOf(field.type(), constant));
      }
      symbols.put(id, field);
      return field;
    }
    JMethodSymbol method = new JMethodSymbol(this, id, name == null ? "" : name);
    method.owner(owner);
    if (descriptor != null && descriptor.startsWith("(")) {
      method.parameterTypes(parameterTypesOf(descriptor));
      method.returnTypeSymbol(readDescriptor(descriptor, new int[] {descriptor.indexOf(')') + 1}).symbol());
    }
    applyMemberFlags(method, member);
    if (owner.isInterface() && !truth(member, ABSTRACT) && !truth(member, STATIC) && !"private".equals(text(member, "vis"))) {
      method.addModifiers(List.of("default"));
    }
    libraryAnnotations(method, id, member);
    List<String> thrown = strings(member, THROWS);
    if (!thrown.isEmpty()) {
      method.thrownTypes(thrown.stream().map(this::typeFromName).toList());
    }
    if (member.has("varargs") && member.get("varargs").isJsonPrimitive()) {
      method.varArgs(member.get("varargs").getAsBoolean());
      method.varArgsKnown(true);
    }
    if (member.has("retParam") && !member.get("retParam").isJsonNull()) {
      method.returnTypeParameter(member.get("retParam").getAsInt());
    }
    if (member.get("retType") instanceof JsonObject retType) {
      method.declaredReturn(retType);
    }
    method.returnsSelf(member.has("retSelf") && member.get("retSelf").getAsBoolean());
    if (member.get("default") instanceof JsonObject defaultValue) {
      Object value = readAnnotationElement(defaultValue);
      if (value != null) {
        annotationDefaults.put(id, value);
      }
    }
    symbols.put(id, method);
    return method;
  }

  /** The default of an annotation element read from a class file. */
  @Nullable
  Object annotationDefault(JMethodSymbol method) {
    return method.id == null ? null : annotationDefaults.get(method.id);
  }

  /** The value a field of type {@code descriptor} holds, from the text Java prints for it. */
  @Nullable
  private static Object libraryConstant(@Nullable String descriptor, String text) {
    try {
      return switch (descriptor == null ? "" : descriptor) {
        case "Z" -> Boolean.valueOf(text);
        case "B" -> Byte.valueOf(text);
        case "S" -> Short.valueOf(text);
        case "C" -> text.length() == 1 ? (Object) (int) text.charAt(0) : null;
        case "I" -> Integer.valueOf(text);
        case "J" -> Long.valueOf(text);
        case "F" -> Float.valueOf(text);
        case "D" -> Double.valueOf(text);
        default -> text;
      };
    } catch (NumberFormatException notANumber) {
      return null;
    }
  }

  private void createDumpSymbol(String id) {
    if (symbols.containsKey(id)) {
      return;
    }
    JsonObject dump = dumpSymbols.get(id);
    String kind = text(dump, "kind");
    String name = text(dump, "name");
    if (name == null) {
      name = "";
    }
    if ("label".equals(kind)) {
      return;
    }
    JSymbol symbol;
    if ("type".equals(kind) && id.indexOf('<') >= 0) {
      symbol = new JTypeSymbol(this, id, name, true);
    } else if ("type".equals(kind)) {
      symbol = new JTypeSymbol(this, id, simpleName(erase(id)), false);
      typesByName.putIfAbsent(erase(id), (JTypeSymbol) symbol);
    } else if ("method".equals(kind) || "constructor".equals(kind)) {
      symbol = new JMethodSymbol(this, id, "constructor".equals(kind) ? "<init>" : name);
    } else {
      symbol = new JVariableSymbol(this, id, name, kind == null ? "field" : kind);
    }
    symbols.put(id, symbol);
  }

  private void linkDumpSymbol(String id) {
    JSymbol symbol = symbols.get(id);
    JsonObject dump = dumpSymbols.get(id);
    if (symbol == null || dump == null) {
      return;
    }
    JsonObject fact = facts.get(id);
    String ownerId = text(dump, "owner");
    String parent = text(fact, "parent");
    if ((ownerId == null && symbol instanceof JMethodSymbol) || (ownerId != null && parent != null && !parent.contains("::") && !facts.containsKey(ownerId) && !dumpSymbols.containsKey(ownerId))) {
      ownerId = parent;
    }
    if (ownerId != null) {
      symbol.owner(resolve(ownerId));
    } else if (symbol instanceof JTypeSymbol typeSymbol && !typeSymbol.isTypeParameter()) {
      symbol.owner(packageSymbol(packageName(erase(id))));
    }
    if (fact != null) {
      applyFact(symbol, fact);
    }
    if ((symbol.isMethodSymbol() || symbol.isVariableSymbol()) && (fact == null || !"IN_PROJECT".equals(text(fact, "tier")))) {
      applyLibraryMember(symbol, id);
    }
    JsonObject details = dump.getAsJsonObject("facts");
    if (symbol.isVariableSymbol() && details != null && details.getAsJsonObject("type") != null) {
      symbol.type(typeFromFact(details.getAsJsonObject("type")));
    } else if (symbol.isVariableSymbol() && fact != null && symbol.type().isUnknown()) {
      // A use before the declaration is translated still sees a primitive type.
      List<String> declared = strings(fact, "type");
      if (declared.size() == 1 && PRIMITIVES.contains(declared.get(0))) {
        symbol.type(primitiveType(declared.get(0)));
      }
    }
    if (symbol instanceof JMethodSymbol method) {
      fillMethod(method, fact, id);
    } else if (symbol instanceof JTypeSymbol typeSymbol && typeSymbol.isTypeParameter()) {
      symbol.type(JType.typeVar(this, symbol.name(), id));
    } else if (symbol.isTypeSymbol()) {
      symbol.type(rawNamed(erase(id)));
    }
  }

  /** Flags, annotations and thrown types the {@code types} answer gives a library member the dump names. */
  private void applyLibraryMember(JSymbol symbol, String id) {
    MemberId parsed = parseMember(id);
    if (parsed == null || parsed.arityOnly || ARRAY.equals(parsed.owner)) {
      return;
    }
    String ownerName = erase(parsed.owner);
    typeSymbol(ownerName);
    JsonObject member = findMember(ownerName, parsed, new HashSet<>(), Match.TYPED);
    if (member == null || member.has("id")) {
      return;
    }
    applyMemberFlags(symbol, member);
    libraryAnnotations(symbol, id, member);
    List<String> thrown = strings(member, THROWS);
    if (symbol instanceof JMethodSymbol method && !thrown.isEmpty()) {
      method.thrownTypes(thrown.stream().map(this::typeFromName).toList());
    }
    if (symbol instanceof JMethodSymbol method && member.get("varargs") instanceof JsonPrimitive varArgs && varArgs.isBoolean()) {
      method.varArgs(varArgs.getAsBoolean());
      method.varArgsKnown(true);
    }
  }

  /** A constant spelled as text, as the boxed value of the field's primitive type. */
  private static Object constantOf(@Nullable Type type, String text) {
    String name = type == null ? "" : type.fullyQualifiedName();
    try {
      return switch (name) {
        case "int" -> Integer.valueOf(text);
        case "long" -> Long.valueOf(text);
        case "short" -> Short.valueOf(text);
        case "byte" -> Byte.valueOf(text);
        case "float" -> Float.valueOf(text);
        case "double" -> Double.valueOf(text);
        case "boolean" -> Boolean.valueOf(text);
        case "char" -> text.length() == 1 ? (Object) (int) text.charAt(0) : text;
        default -> text;
      };
    } catch (NumberFormatException e) {
      return text;
    }
  }

  /** The upper bound of the type variable with this id: its first declared bound, or {@code Object}. */
  Type typeVariableBound(@Nullable String id) {
    if (id != null && symbols.get(id) instanceof JTypeSymbol parameter && parameter.firstBound() instanceof Type bound && !bound.isUnknown()) {
      return bound;
    }
    return getClassType("java.lang.Object");
  }

  /** Every declared bound of the type variable with this id, or {@code Object} when it has none. */
  List<Type> typeVariableBounds(@Nullable String id) {
    if (id != null && symbols.get(id) instanceof JTypeSymbol parameter && !parameter.bounds().isEmpty()) {
      return parameter.bounds();
    }
    return List.of(getClassType("java.lang.Object"));
  }

  private JSymbol typeParameter(String id) {
    int open = id.lastIndexOf('<');
    String name = open >= 0 && id.endsWith(">") ? id.substring(open + 1, id.length() - 1) : id;
    JTypeSymbol symbol = new JTypeSymbol(this, id, name, true);
    symbol.type(JType.typeVar(this, name, id));
    symbols.put(id, symbol);
    return symbol;
  }

  private JSymbol createMember(String id) {
    if (!creating.add(id)) {
      return unknownSymbol;
    }
    try {
      return createMemberOnce(id);
    } finally {
      creating.remove(id);
    }
  }

  private JSymbol createMemberOnce(String id) {
    MemberId parsed = parseMember(id);
    if (parsed == null) {
      return typeSymbol(erase(id));
    }
    if (ARRAY.equals(parsed.owner) && "length".equals(parsed.name)) {
      JVariableSymbol length = new JVariableSymbol(this, id, "length", "field");
      length.addModifiers(List.of("public", "final"));
      length.type(primitiveType("int"));
      length.owner(Symbol.ROOT_PACKAGE);
      symbols.put(id, length);
      return length;
    }
    String ownerName = erase(parsed.owner);
    JTypeSymbol owner = typeSymbol(ownerName);
    JsonObject member = null;
    if (parsed.params != null && !parsed.arityOnly) {
      member = findMember(ownerName, parsed, new HashSet<>(), Match.TYPED);
    }
    if (member == null) {
      member = findMember(ownerName, parsed, new HashSet<>(), Match.ARITY);
    }
    if (member == null && parsed.params != null) {
      member = findMember(ownerName, parsed, new HashSet<>(), Match.VARARGS);
    }
    String memberId = member == null ? null : text(member, "id");
    if (member != null && (memberId == null || !creating.contains(memberId))) {
      Symbol resolved = memberFromTypeInfo(owner, member);
      if (resolved instanceof JSymbol jSymbol) {
        symbols.put(id, jSymbol);
        return jSymbol;
      }
    }
    if (parsed.params == null && parsed.name.contains("$lambda$")) {
      JMethodSymbol lambda = new JMethodSymbol(this, id, parsed.name);
      lambda.owner(owner);
      lambda.lambda(true);
      symbols.put(id, lambda);
      return lambda;
    }
    if (parsed.params == null && parsed.name.contains(")$")) {
      return createTypeInMethodBody(id, parsed);
    }
    if (parsed.params == null) {
      JVariableSymbol field = new JVariableSymbol(this, id, parsed.name, "field");
      field.owner(owner);
      JsonObject fieldFact = factOf(id);
      List<String> fieldType = fieldFact == null ? List.of() : flat(fieldFact, "type");
      if (fieldFact != null && fieldType.isEmpty() && owner instanceof JTypeSymbol enumType && enumType.isEnum() && enumType.type() != null) {
        // the facts of an enum constant carry no type: it is of its enum
        applyFact(field, fieldFact);
        field.type(enumType.type());
        field.enumSymbol(true);
      } else if (fieldFact == null || fieldType.isEmpty()) {
        field.unknown(true);
      } else {
        applyFact(field, fieldFact);
        JsonObject declared = projectFieldTypes.get(id);
        field.type(declared != null ? typeFromFact(declared) : typeFromName(fieldType.get(0)));
      }
      markDeprecated(field, id);
      symbols.put(id, field);
      return field;
    }
    JMethodSymbol method = new JMethodSymbol(this, id, parsed.name);
    boolean undeclaredOwner = noLibraries && member == null && !declaresType(ownerName);
    method.owner(parsed.arityOnly || undeclaredOwner ? Symbol.TypeSymbol.UNKNOWN_TYPE : owner);
    method.unknown(parsed.arityOnly || undeclaredOwner);
    fillMethod(method, factOf(id), id);
    if ("<init>".equals(parsed.name) && factOf(id) == null && member == null) {
      // The implicit default constructor has the access of its class.
      method.addModifiers(List.of("public", "protected", "private").stream().filter(owner::hasModifier).toList());
    }
    if (method.parameterTypes().isEmpty() && !parsed.arityOnly && parsed.params != null && !parsed.params.isEmpty()) {
      List<Type> types = new ArrayList<>();
      for (String param : parsed.paramList()) {
        types.add(typeFromName(param));
      }
      method.parameterTypes(types);
    }
    JsonObject methodFact = factOf(id);
    if (methodFact != null && !parsed.arityOnly) {
      applyFact(method, methodFact);
    }
    markDeprecated(method, id);
    symbols.put(id, method);
    return method;
  }

  /** The facts of a project member: those of this file, else those a {@code types} answer gave with it. */
  @Nullable
  private JsonObject factOf(String id) {
    JsonObject fact = facts.get(id);
    return fact != null ? fact : projectMemberFacts.get(id);
  }

  private void markDeprecated(JSymbol symbol, String id) {
    if (declaredAnnotations(id).stream().anyMatch(annotation -> "java.lang.Deprecated".equals(annotation.name()))) {
      symbol.deprecated(true);
    }
  }

  /**
   * The method {@code name} of {@code ownerFqn} or of one of its supertypes that Java would pick for {@code arguments}:
   * applicable by subtyping first, then with boxing, then as varargs, and the most specific of those.
   */
  @Nullable
  Symbol.MethodSymbol declaredMethod(String ownerFqn, String name, List<Type> arguments) {
    return declaredMethod(ownerFqn, name, arguments, Set.of());
  }

  /** [declaredMethod] where the arguments at {@code functions} are lambdas or method references, which only a functional interface takes. */
  @Nullable
  Symbol.MethodSymbol declaredMethod(String ownerFqn, String name, List<Type> arguments, Set<Integer> functions) {
    if (!declaresType(ownerFqn)) {
      return null;
    }
    List<Symbol.MethodSymbol> candidates = new ArrayList<>();
    collectMethods(ownerFqn, ownerFqn, name, new HashSet<>(), candidates);
    if (!functions.isEmpty()) {
      candidates.removeIf(method -> takesNoFunction(method, functions));
    }
    for (int phase = 0; phase < 3; phase++) {
      int current = phase;
      List<Symbol.MethodSymbol> applicable = candidates.stream().filter(method -> applicable(method, arguments, current)).toList();
      if (!applicable.isEmpty()) {
        return applicable.stream().filter(method -> applicable.stream().allMatch(other -> moreSpecific(method, other))).findFirst().orElse(applicable.get(0));
      }
    }
    List<Symbol.MethodSymbol> sameArity = candidates.stream()
      .filter(method -> method.isVarArgsMethod() ? arguments.size() >= method.parameterTypes().size() - 1 : method.parameterTypes().size() == arguments.size())
      .toList();
    if (sameArity.size() == 1) {
      return sameArity.get(0);
    }
    return null;
  }

  private static boolean takesNoFunction(Symbol.MethodSymbol method, Set<Integer> functions) {
    List<Type> parameters = method.parameterTypes();
    for (int index : functions) {
      if (index >= parameters.size()) {
        continue;
      }
      Type target = parameters.get(index).erasure();
      if (target.isPrimitive() || target.isArray() || (target.isClass() && !target.isUnknown() && !target.isTypeVar() && !target.symbol().isInterface() && !target.symbol().isUnknown())) {
        return true;
      }
    }
    return false;
  }

  /** ECJ leaves a call unresolved when no declaration takes this many arguments, rather than guessing one. */
  static boolean arityMatches(Symbol.MethodSymbol method, int argumentCount) {
    int parameters = method.parameterTypes().size();
    return method.isVarArgsMethod() ? argumentCount >= parameters - 1 : argumentCount == parameters;
  }

  /** The single abstract method of {@code type} when it is a functional interface, else null. */
  @Nullable
  JMethodSymbol functionalMethod(Type type) {
    String fqn = type.erasure().fullyQualifiedName();
    if (!type.isClass() || !type.symbol().isInterface() || !declaresType(fqn)) {
      return null;
    }
    Map<String, JMethodSymbol> found = new LinkedHashMap<>();
    collectAbstractMethods(fqn, new HashSet<>(), found);
    return found.size() == 1 ? found.values().iterator().next() : null;
  }

  private void collectAbstractMethods(String ownerFqn, Set<String> seen, Map<String, JMethodSymbol> found) {
    if (!seen.add(ownerFqn) || !declaresType(ownerFqn)) {
      return;
    }
    JsonObject info = typeInfos.get(sourceName(ownerFqn));
    JsonArray members = info.getAsJsonArray(MEMBERS);
    if (members != null) {
      for (JsonElement element : members) {
        if (memberFromTypeInfo(typeSymbol(ownerFqn), element.getAsJsonObject()) instanceof JMethodSymbol method && !method.isUnknown() && method.isAbstract()
          && !OBJECT_METHODS.contains(method.name() + "/" + method.parameterTypes().size())) {
          found.putIfAbsent(method.name() + "/" + method.parameterTypes().size(), method);
        }
      }
    }
    parents(info).forEach(parent -> collectAbstractMethods(erase(parent), seen, found));
  }

  /** The field {@code name} that {@code ownerFqn} or one of its supertypes declares, if any. */
  @Nullable
  Symbol.VariableSymbol declaredField(String ownerFqn, String name) {
    return declaredField(ownerFqn, name, new HashSet<>());
  }

  @Nullable
  private Symbol.VariableSymbol declaredField(String ownerFqn, String name, Set<String> seen) {
    if (!seen.add(ownerFqn) || !declaresType(ownerFqn)) {
      return null;
    }
    JsonObject info = typeInfos.get(sourceName(ownerFqn));
    JsonArray members = info.getAsJsonArray(MEMBERS);
    if (members != null) {
      for (JsonElement element : members) {
        JsonObject member = element.getAsJsonObject();
        MemberId id = member.has("id") ? parseMember(member.get("id").getAsString()) : null;
        boolean named = id == null ? (name.equals(text(member, "name")) && "f".equals(text(member, "k"))) : (name.equals(id.name) && id.params == null);
        if (named && memberFromTypeInfo(typeSymbol(ownerFqn), member) instanceof Symbol.VariableSymbol field && !field.isUnknown()) {
          return field;
        }
      }
    }
    for (String parent : parents(info)) {
      Symbol.VariableSymbol found = declaredField(erase(parent), name, seen);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  /** Methods named {@code name} that {@code root} has: a static method of an interface is not inherited. */
  private void collectMethods(String root, String ownerFqn, String name, Set<String> seen, List<Symbol.MethodSymbol> found) {
    if (!seen.add(ownerFqn) || !declaresType(ownerFqn)) {
      return;
    }
    JsonObject info = typeInfos.get(sourceName(ownerFqn));
    boolean inheritedInterface = !ownerFqn.equals(root) && typeSymbol(ownerFqn).isInterface();
    JsonArray members = info.getAsJsonArray(MEMBERS);
    if (members != null) {
      for (JsonElement element : members) {
        JsonObject member = element.getAsJsonObject();
        MemberId id = member.has("id") ? parseMember(member.get("id").getAsString()) : null;
        boolean named = id == null ? (name.equals(text(member, "name")) && "m".equals(text(member, "k"))) : (name.equals(id.name) && id.params != null);
        if (named && memberFromTypeInfo(typeSymbol(ownerFqn), member) instanceof Symbol.MethodSymbol method && !method.isUnknown()
          && !(inheritedInterface && method.isStatic())) {
          found.add(method);
        }
      }
    }
    if (!"<init>".equals(name)) {
      parents(info).forEach(parent -> collectMethods(root, erase(parent), name, seen, found));
    }
  }

  /** The overload that matches {@code arguments} when {@code candidate} is more specific than {@code current}. */
  Symbol.MethodSymbol preferOverload(Symbol.MethodSymbol current, @Nullable Symbol.MethodSymbol candidate, List<Type> arguments, Set<Integer> functions) {
    if (candidate == null || candidate == current || candidate.isUnknown() || !arityMatches(candidate, arguments.size())) {
      return current;
    }
    if (!functions.isEmpty() && takesNoFunction(current, functions) && !takesNoFunction(candidate, functions)) {
      return candidate;
    }
    for (int phase = 0; phase < 3; phase++) {
      boolean currentOk = applicable(current, arguments, phase);
      boolean candidateOk = applicable(candidate, arguments, phase);
      if (currentOk && !candidateOk) {
        return current;
      }
      if (candidateOk && !currentOk) {
        return candidate;
      }
      if (currentOk && candidateOk) {
        return moreSpecific(candidate, current) && !moreSpecific(current, candidate) ? candidate : current;
      }
    }
    return current;
  }

  /** The most specific variable-arity overload of {@code chosen} applicable to the arguments, when no earlier phase applies to {@code chosen}. */
  JMethodSymbol mostSpecificVarArgs(JMethodSymbol chosen, List<Type> arguments) {
    if (chosen.isUnknown() || !chosen.isVarArgsMethod() || !(chosen.owner() instanceof Symbol.TypeSymbol owner) || applicable(chosen, arguments, 0) || applicable(chosen, arguments, 1)
      || arguments.stream().anyMatch(Type::isUnknown)) {
      return chosen;
    }
    List<JMethodSymbol> candidates = new ArrayList<>();
    for (Symbol member : owner.lookupSymbols(chosen.name())) {
      if (member instanceof JMethodSymbol method && !method.isUnknown() && method.isVarArgsMethod() && method.isStatic() == chosen.isStatic() && applicable(method, arguments, 2)) {
        candidates.add(method);
      }
    }
    for (JMethodSymbol candidate : candidates) {
      if (candidates.stream().allMatch(other -> other == candidate || moreSpecificVarArgs(candidate, other, arguments.size()))) {
        return candidate;
      }
    }
    return chosen;
  }

  private static boolean moreSpecificVarArgs(Symbol.MethodSymbol method, Symbol.MethodSymbol other, int count) {
    for (int i = 0; i < Math.max(count, Math.max(method.parameterTypes().size(), other.parameterTypes().size())); i++) {
      if (!expanded(method, i).erasure().isSubtypeOf(expanded(other, i).erasure())) {
        return false;
      }
    }
    return true;
  }

  private static Type expanded(Symbol.MethodSymbol method, int index) {
    List<Type> parameters = method.parameterTypes();
    return index < parameters.size() - 1 ? parameters.get(index) : ((Type.ArrayType) parameters.get(parameters.size() - 1)).elementType();
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

  private static boolean applicable(Symbol.MethodSymbol method, List<Type> arguments, int phase) {
    List<Type> parameters = method.parameterTypes();
    int count = parameters.size();
    boolean varargs = phase == 2 && method.isVarArgsMethod();
    if (varargs ? arguments.size() < count - 1 : arguments.size() != count) {
      return false;
    }
    for (int i = 0; i < arguments.size(); i++) {
      Type parameter = varargs && i >= count - 1 ? ((Type.ArrayType) parameters.get(count - 1)).elementType() : parameters.get(i);
      if (!convertible(arguments.get(i), parameter, phase > 0)) {
        return false;
      }
    }
    return true;
  }

  private static boolean convertible(Type argument, Type parameter, boolean boxing) {
    Type target = parameter.erasure();
    if (argument.isUnknown() || target.isUnknown() || target.isTypeVar() || argument.erasure().isSubtypeOf(target)) {
      return true;
    }
    if (!boxing || argument.isPrimitive() == target.isPrimitive()) {
      return false;
    }
    Type converted = argument.isPrimitive() ? argument.primitiveWrapperType() : argument.primitiveType();
    return converted != null && converted.isSubtypeOf(target);
  }

  private static boolean moreSpecific(Symbol.MethodSymbol method, Symbol.MethodSymbol other) {
    List<Type> mine = method.parameterTypes();
    List<Type> theirs = other.parameterTypes();
    if (mine.size() != theirs.size()) {
      return true;
    }
    for (int i = 0; i < mine.size(); i++) {
      Type target = theirs.get(i).erasure();
      if (!target.isTypeVar() && !mine.get(i).erasure().isSubtypeOf(target)) {
        return false;
      }
    }
    return true;
  }

  @Nullable
  private JsonObject findMember(String ownerFqn, MemberId parsed, Set<String> seen, Match match) {
    if (!seen.add(ownerFqn)) {
      return null;
    }
    JsonObject info = typeInfos.get(sourceName(ownerFqn));
    JsonObject found = matchMembers(info == null ? null : info.getAsJsonArray(MEMBERS), parsed, match);
    if (found != null) {
      return found;
    }
    JTypeSymbol type = typesByName.get(sourceName(ownerFqn));
    if (type != null) {
      for (JsonObject member : type.memberFacts()) {
        if (matches(member, parsed, match)) {
          return member;
        }
      }
    }
    if (info == null || "<init>".equals(parsed.name)) {
      return null;
    }
    for (String parent : parents(info)) {
      found = findMember(erase(parent), parsed, seen, match);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  @Nullable
  private JsonObject matchMembers(@Nullable JsonArray members, MemberId parsed, Match match) {
    if (members == null) {
      return null;
    }
    for (JsonElement element : members) {
      JsonObject member = element.getAsJsonObject();
      if (matches(member, parsed, match)) {
        return member;
      }
    }
    return null;
  }

  private boolean matches(JsonObject member, MemberId parsed, Match match) {
    if (member.has("id")) {
      MemberId other = parseMember(member.get("id").getAsString());
      return other != null && parsed.name.equals(other.name) && parsed.arity == other.arity
        && (parsed.arityOnly || other.arityOnly || Objects.equals(parsed.params, other.params));
    }
    if (!parsed.name.equals(text(member, "name"))) {
      return false;
    }
    boolean method = parsed.params != null;
    if (method != "m".equals(text(member, "k"))) {
      return false;
    }
    if (!method) {
      return true;
    }
    String descriptor = text(member, "desc");
    if (descriptor == null) {
      return false;
    }
    List<Type> declaredParams = parameterTypesOf(descriptor);
    int declaredCount = declaredParams.size();
    return switch (match) {
      case TYPED -> declaredCount == parsed.arity && sameErasures(declaredParams, parsed.paramList());
      case ARITY -> declaredCount == parsed.arity;
      case VARARGS -> declaredCount > 0 && parsed.arity >= declaredCount - 1 && declaredParams.get(declaredCount - 1).isArray();
    };
  }

  private static boolean sameErasures(List<Type> declared, List<String> names) {
    for (int i = 0; i < declared.size(); i++) {
      String expected = declared.get(i).erasure().fullyQualifiedName().replace('$', '.');
      if (!expected.equals(erasedName(names.get(i)))) {
        return false;
      }
    }
    return true;
  }

  private static String erasedName(String name) {
    StringBuilder out = new StringBuilder(name.length());
    int depth = 0;
    for (char current : erase(name).toCharArray()) {
      if (current == '<') {
        depth++;
      } else if (current == '>') {
        depth--;
      } else if (depth == 0) {
        out.append(current == '$' ? '.' : current);
      }
    }
    return out.toString().replace("...", "[]").trim();
  }

  private enum Match {
    TYPED, ARITY, VARARGS
  }

  private void fillMethod(JMethodSymbol method, @Nullable JsonObject fact, String id) {
    MemberId parsed = parseMember(id);
    if (fact != null && fact.has("returns")) {
      List<String> returns = flat(fact, "returns");
      if (!returns.isEmpty()) {
        method.returnTypeSymbol(typeFromName(returns.get(0)).symbol());
      }
    }
    List<String> factParams = fact == null ? List.of() : flat(fact, "params");
    if (!factParams.isEmpty() && (parsed == null || parsed.arityOnly || factParams.size() == parsed.arity)) {
      List<Type> types = new ArrayList<>();
      for (String param : factParams) {
        types.add(typeFromName(param));
      }
      method.parameterTypes(types);
    } else if (parsed != null && !parsed.arityOnly && parsed.params != null && !parsed.params.isEmpty()) {
      List<Type> types = new ArrayList<>();
      for (String param : parsed.paramList()) {
        types.add(typeFromName(param));
      }
      method.parameterTypes(types);
    }
    if ("<init>".equals(method.name())) {
      method.returnTypeSymbol(primitiveSymbol("void"));
    }
    JsonObject factProps = fact == null ? null : fact.getAsJsonObject("props");
    List<String> declaredParams = factProps == null ? List.of() : flat(factProps, "param_types");
    Boolean served = projectVarArgs.get(id);
    if (served != null) {
      method.varArgs(served);
      method.varArgsKnown(true);
    } else if (!declaredParams.isEmpty()) {
      method.varArgs(declaredParams.get(declaredParams.size() - 1).endsWith("..."));
      method.varArgsKnown(true);
    }
    method.parametrized(id.replace("<init>", "").replace("<clinit>", "").indexOf('<') >= 0);
  }

  private void applyFact(JSymbol symbol, JsonObject fact) {
    String kind = text(fact, "kind");
    if (symbol instanceof JTypeSymbol type) {
      boolean interfaceKind = "interface".equals(kind) || "annotation".equals(kind);
      type.interfaceSymbol(interfaceKind);
      type.annotationSymbol("annotation".equals(kind));
      List<String> extend = strings(fact, "extends");
      List<String> implement = strings(fact, "implements");
      type.enumSymbol("enum".equals(kind) || extend.stream().map(JSema::erase).anyMatch("java.lang.Enum"::equals));
      if (interfaceKind) {
        type.interfaceNames(extend.isEmpty() ? implement : extend);
      } else {
        type.superName(extend.isEmpty() ? null : extend.get(0));
        type.interfaceNames(implement);
      }
    }
    JsonObject props = fact.getAsJsonObject("props");
    if (props != null) {
      symbol.addModifiers(flat(props, "modifiers"));
    }
    if (symbol instanceof JTypeSymbol) {
      symbol.addModifiers(implicitTypeModifiers(kind, text(fact, "parent")));
    }
    for (String annotation : strings(fact, ANNOTATIONS)) {
      if ("java.lang.Deprecated".equals(erase(annotation))) {
        symbol.deprecated(true);
      }
    }
    if (symbol.isVariableSymbol() && ("enum".equals(kind) || "enum_member".equals(kind))) {
      symbol.enumSymbol(true);
      symbol.addModifiers(List.of("public", STATIC, "final"));
    }
  }

  /**
   * Modifiers Java implies: member interfaces, enums, records and annotations are static; enums and records are
   * final (an enum constant with a body is not modelled).
   */
  private List<String> implicitTypeModifiers(@Nullable String kind, @Nullable String parent) {
    List<String> implied = new ArrayList<>();
    boolean member = parent != null && !parent.endsWith("::<module>") && facts.get(parent) != null
      && isTypeFact(facts.get(parent));
    if (member && ("interface".equals(kind) || "enum".equals(kind) || RECORD.equals(kind) || "annotation".equals(kind))) {
      implied.add(STATIC);
    }
    if ("enum".equals(kind) || RECORD.equals(kind)) {
      implied.add("final");
    }
    return implied;
  }

  private static boolean isTypeFact(JsonObject fact) {
    String kind = text(fact, "kind");
    return "class".equals(kind) || "interface".equals(kind) || "enum".equals(kind) || RECORD.equals(kind)
      || "annotation".equals(kind) || "exception".equals(kind);
  }

  private void applyTypeInfo(JTypeSymbol symbol, JsonObject info) {
    String kind = text(info, "kind");
    boolean interfaceKind = "interface".equals(kind) || "annotation".equals(kind);
    symbol.interfaceSymbol(interfaceKind);
    symbol.annotationSymbol("annotation".equals(kind));
    symbol.enumSymbol("enum".equals(kind) || "java.lang.Enum".equals(text(info, "super")));
    if (interfaceKind) {
      List<String> extend = strings(info, "super");
      symbol.interfaceNames(extend.isEmpty() ? strings(info, "interfaces") : extend);
      symbol.superName(null);
    } else {
      symbol.superName(text(info, "super"));
      symbol.interfaceNames(strings(info, "interfaces"));
    }
    symbol.genericSupertypes(
      info.get("superType") instanceof JsonObject superType ? superType : null,
      objects(info, "interfaceTypes"));
    symbol.addModifiers(flat(info, "modifiers"));
    libraryAnnotations(symbol, symbol.id == null ? symbol.name() : symbol.id, info);
    if (info.has("typeParameters") && !info.get("typeParameters").isJsonNull()) {
      symbol.typeParameterCount(info.get("typeParameters").getAsInt());
    }
    List<JsonObject> members = new ArrayList<>();
    if (info.has(MEMBERS)) {
      for (JsonElement member : info.getAsJsonArray(MEMBERS)) {
        members.add(member.getAsJsonObject());
      }
    }
    symbol.memberFacts(members);
    symbol.unknown(false);
  }

  private static void applyMemberFlags(JSymbol symbol, JsonObject member) {
    List<String> modifiers = new ArrayList<>();
    if (truth(member, STATIC)) {
      modifiers.add(STATIC);
    }
    if (truth(member, "final")) {
      modifiers.add("final");
    }
    if (truth(member, "synchronized")) {
      modifiers.add("synchronized");
    }
    if (truth(member, ABSTRACT)) {
      modifiers.add(ABSTRACT);
    }
    String visibility = text(member, "vis");
    if (visibility != null && !"package".equals(visibility)) {
      modifiers.add(visibility);
    }
    symbol.addModifiers(modifiers);
  }

  private static List<String> parents(JsonObject info) {
    List<String> parents = new ArrayList<>();
    String superName = text(info, "super");
    if (superName != null) {
      parents.add(superName);
    }
    parents.addAll(strings(info, "interfaces"));
    parents.addAll(strings(info, "super"));
    return parents;
  }

  private JType typeFromFact(JsonObject fact) {
    return typeFromFact(fact, Map.of());
  }

  private JType typeFromFact(JsonObject fact, Map<String, Type> substitution) {
    return typeFromFact(fact, substitution, false);
  }

  private JType typeFromFact(JsonObject fact, Map<String, Type> substitution, boolean captureWildcard) {
    String kind = text(fact, "kind");
    if (ARRAY.equals(kind) && fact.getAsJsonObject("elem") != null) {
      return JType.array(this, typeFromFact(fact.getAsJsonObject("elem"), substitution, captureWildcard));
    }
    if ("wildcard".equals(kind)) {
      return wildcardFromFact(fact, substitution, captureWildcard);
    }
    if ("typevar".equals(kind)) {
      String id = text(fact, "id");
      Type substituted = substitution.get(id);
      if (substituted != null) {
        return asJType(substituted);
      }
      int open = id == null ? -1 : id.lastIndexOf('<');
      String name = open >= 0 && id.endsWith(">") ? id.substring(open + 1, id.length() - 1) : id;
      return JType.typeVar(this, name == null ? "" : name, id);
    }
    if ("unknown".equals(kind)) {
      return JType.unknown(this);
    }
    String id = text(fact, "id");
    if (id == null) {
      return JType.unknown(this);
    }
    List<JType> args = new ArrayList<>();
    if (fact.has("args")) {
      for (JsonElement arg : fact.getAsJsonArray("args")) {
        if (arg.isJsonObject()) {
          args.add(typeFromFact(arg.getAsJsonObject(), substitution, captureWildcard));
        }
      }
    }
    String erased = erase(id);
    if (PRIMITIVES.contains(erased)) {
      return primitiveType(erased);
    }
    if (captureWildcard) {
      boundCaptures(erased, args);
    }
    return JType.named(this, erased, args);
  }

  /** An unbounded wildcard argument is captured with the bound its type parameter declares. */
  private void boundCaptures(String owner, List<JType> arguments) {
    for (int i = 0; i < arguments.size(); i++) {
      JType argument = arguments.get(i);
      if (argument.isCapture() && argument.element != null && argument.element.is("java.lang.Object")) {
        JType bound = declaredBound(owner, i);
        if (bound != null) {
          arguments.set(i, JType.capture(this, bound));
        }
      }
    }
  }

  /** The class bound the {@code index}th type parameter of {@code owner} declares, when it is more than Object. */
  @Nullable
  private JType declaredBound(String owner, int index) {
    JsonObject info = typeInfos.get(sourceName(owner));
    if (info == null || !(info.get("typeVarBounds") instanceof JsonArray bounds) || bounds.size() <= index
      || !(bounds.get(index) instanceof JsonArray list) || list.isEmpty() || !(list.get(0) instanceof JsonObject fact)) {
      return null;
    }
    JType bound = typeFromFact(fact);
    return bound.isUnknown() || bound.is("java.lang.Object") ? null : bound;
  }

  private JType wildcardFromFact(JsonObject fact, Map<String, Type> substitution, boolean capture) {
    JType object = asJType(getClassType("java.lang.Object"));
    JType upper = fact.get("extends") instanceof JsonObject bound ? typeFromFact(bound, substitution) : object;
    if (capture) {
      return JType.capture(this, upper.isUnknown() ? object : upper);
    }
    if (fact.get("super") instanceof JsonObject bound) {
      return JType.superWildcard(this, typeFromFact(bound, substitution));
    }
    return JType.wildcard(this, upper);
  }

  /** A supertype fact: the loaded raw type, parameterized when the fact has type arguments. */
  Type supertypeFromFact(JsonObject fact) {
    Type raw = getClassType(erase(text(fact, "id")));
    JsonArray args = fact.getAsJsonArray("args");
    if (args == null || args.isEmpty() || !(raw instanceof JType rawType)) {
      return raw;
    }
    List<JType> resolved = new ArrayList<>();
    for (JsonElement arg : args) {
      if (arg.isJsonObject()) {
        resolved.add(typeFromFact(arg.getAsJsonObject()));
      }
    }
    return JType.named(this, rawType.fullyQualifiedName(), resolved);
  }

  private static List<JsonObject> objects(JsonObject object, String key) {
    if (!object.has(key) || !object.get(key).isJsonArray()) {
      return List.of();
    }
    List<JsonObject> values = new ArrayList<>();
    for (JsonElement item : object.getAsJsonArray(key)) {
      if (item.isJsonObject()) {
        values.add(item.getAsJsonObject());
      }
    }
    return values;
  }

  private Type typeFromName(String name) {
    String erased = erase(name);
    if (PRIMITIVES.contains(erased)) {
      return primitiveType(erased);
    }
    if (erased.endsWith("[]")) {
      return JType.array(this, asJType(typeFromName(erased.substring(0, erased.length() - 2))));
    }
    return getClassType(erased);
  }

  private JType primitiveType(String name) {
    return rawTypes.computeIfAbsent(name, key -> JType.primitive(this, key));
  }

  private final Map<Symbol.TypeSymbol, JInitializerBlockSymbol> initializerBlockSymbols = new HashMap<>();
  private final Map<Symbol.TypeSymbol, JInitializerBlockSymbol> staticInitializerBlockSymbols = new HashMap<>();

  JInitializerBlockSymbol initializerBlockSymbol(JTypeSymbol owner) {
    return initializerBlockSymbols.computeIfAbsent(owner, k -> new JInitializerBlockSymbol(k, false));
  }

  JInitializerBlockSymbol staticInitializerBlockSymbol(JTypeSymbol owner) {
    return staticInitializerBlockSymbols.computeIfAbsent(owner, k -> new JInitializerBlockSymbol(k, true));
  }

  /** The parameter of a method no source declares: one symbol per method and position, whichever call it is seen from. */
  Symbol parameterPlaceholder(JMethodSymbol method, int index, Type type) {
    String key = method.id + "#" + index + "#" + type.fullyQualifiedName();
    if (method.id != null && libraryAnnotations.containsKey(parameterKey(method.id, index))) {
      return placeholders.computeIfAbsent(key, k -> {
        JVariableSymbol annotated = new JVariableSymbol(this, parameterKey(method.id, index), "arg" + index, "param");
        annotated.owner(method);
        annotated.type(type);
        return annotated;
      });
    }
    return method.id == null ? new JVariableSymbol.ParameterPlaceholderSymbol(index, method, type)
      : placeholders.computeIfAbsent(key, k -> new JVariableSymbol.ParameterPlaceholderSymbol(index, method, type));
  }

  JType asJType(Type type) {
    return type instanceof JType jType ? jType : JType.unknown(this);
  }

  private JType rawNamed(String fqn) {
    return rawTypes.computeIfAbsent(sourceName(fqn), name -> JType.named(this, binaryNames.getOrDefault(name, name), List.of()));
  }

  private List<Type> parameterTypesOf(String descriptor) {
    List<Type> types = new ArrayList<>();
    int[] index = {1};
    while (index[0] < descriptor.length() && descriptor.charAt(index[0]) != ')') {
      types.add(readDescriptor(descriptor, index));
    }
    return types;
  }

  private Type readDescriptor(String descriptor, int[] index) {
    int array = 0;
    while (descriptor.charAt(index[0]) == '[') {
      array++;
      index[0]++;
    }
    Type type;
    char kind = descriptor.charAt(index[0]);
    if (kind == 'L') {
      int semi = descriptor.indexOf(';', index[0]);
      String binary = descriptor.substring(index[0] + 1, semi).replace('/', '.');
      index[0] = semi + 1;
      type = getClassType(binary);
    } else {
      index[0]++;
      type = primitiveType(switch (kind) {
        case 'B' -> "byte";
        case 'C' -> "char";
        case 'D' -> "double";
        case 'F' -> "float";
        case 'I' -> "int";
        case 'J' -> "long";
        case 'S' -> "short";
        case 'Z' -> "boolean";
        default -> "void";
      });
    }
    for (int i = 0; i < array; i++) {
      type = JType.array(this, asJType(type));
    }
    return type;
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

  /** The unknown type a written name stands for when nothing declares it. */
  JTypeSymbol recoveredType(String name) {
    JTypeSymbol recovered = typeSymbol(name);
    if (!recovered.isUnknown() && !declaresType(name)) {
      recovered.unknown(true);
    }
    return recovered;
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

  @Nullable
  /**
   * Local and anonymous classes are identified as members of their enclosing type, with the declaring
   * method embedded in the member name, as in {@code com.acme.Owner#create()$Local$1}. They carry no
   * declaration of their own, so the symbol is rebuilt from the identifier and owned by whatever
   * declares it, which is the method for a local class and the local class for a nested anonymous one.
   */
  private JSymbol createTypeInMethodBody(String id, MemberId parsed) {
    int lastSegment = parsed.name.lastIndexOf('$');
    String simpleName = parsed.name.substring(lastSegment + 1);
    boolean anonymous = simpleName.chars().allMatch(Character::isDigit);
    JTypeSymbol symbol = new JTypeSymbol(this, id, anonymous ? "" : simpleName, false);
    symbols.put(id, symbol);
    typesByName.putIfAbsent(id, symbol);
    symbol.owner(resolve(parsed.owner + "#" + parsed.name.substring(0, lastSegment)));
    symbol.type(rawNamed(id));
    JsonObject fact = facts.get(id);
    if (fact != null) {
      applyFact(symbol, fact);
    }
    return symbol;
  }

  /** Member ids repeat across lookups and files, so their parse is kept; the cache is dropped when it grows large. */
  @Nullable
  private static MemberId parseMember(String id) {
    MemberId cached = MEMBER_IDS.get(id);
    if (cached == null) {
      cached = parseMemberUncached(id);
      if (MEMBER_IDS.size() >= MEMBER_IDS_LIMIT) {
        MEMBER_IDS.clear();
      }
      MEMBER_IDS.put(id, cached == null ? NOT_A_MEMBER : cached);
    }
    return cached == NOT_A_MEMBER ? null : cached;
  }

  @Nullable
  private static MemberId parseMemberUncached(String id) {
    int hash = id.indexOf('#');
    if (hash < 0) {
      return null;
    }
    String owner = id.substring(0, hash);
    String rest = id.substring(hash + 1);
    int paren = rest.endsWith(")") ? rest.lastIndexOf('(') : -1;
    if (paren < 0) {
      return new MemberId(owner, rest, null, false, -1, List.of());
    }
    String name = rest.substring(0, paren);
    String params = rest.substring(paren + 1, rest.length() - 1);
    boolean arityOnly = !params.isEmpty() && params.chars().allMatch(Character::isDigit);
    List<String> paramList = splitParams(params);
    int arity = arityOnly ? Integer.parseInt(params) : paramList.size();
    return new MemberId(owner, name, params, arityOnly, arity, paramList);
  }

  private static List<String> splitParams(String params) {
    if (params.isEmpty()) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    int depth = 0;
    int start = 0;
    for (int i = 0; i < params.length(); i++) {
      char current = params.charAt(i);
      if (current == '<') {
        depth++;
      } else if (current == '>') {
        depth--;
      } else if (current == ',' && depth == 0) {
        result.add(params.substring(start, i));
        start = i + 1;
      }
    }
    result.add(params.substring(start));
    return result;
  }

  static String erase(String id) {
    if (id.indexOf('`') < 0) {
      return id;
    }
    StringBuilder out = new StringBuilder(id.length());
    for (int i = 0; i < id.length(); i++) {
      char current = id.charAt(i);
      if (current == '`') {
        while (i + 1 < id.length() && Character.isDigit(id.charAt(i + 1))) {
          i++;
        }
      } else {
        out.append(current);
      }
    }
    return out.toString();
  }

  private static String simpleName(String fqn) {
    int boundary = Math.max(fqn.lastIndexOf('.'), fqn.lastIndexOf('$'));
    String name = boundary < 0 ? fqn : fqn.substring(boundary + 1);
    return isAnonymousName(name) ? "" : name;
  }

  /** An anonymous class is named by its ordinal, which a source name never is. */
  static boolean isAnonymousName(String name) {
    return !name.isEmpty() && name.chars().allMatch(Character::isDigit);
  }

  private static String packageName(String fqn) {
    String top = fqn;
    int nested = top.indexOf('$');
    if (nested >= 0) {
      top = top.substring(0, nested);
    }
    int dot = top.lastIndexOf('.');
    return dot < 0 ? "" : top.substring(0, dot);
  }

  @Nullable
  private static String text(@Nullable JsonObject object, String key) {
    JsonElement element = object == null ? null : object.get(key);
    return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
  }

  private static boolean truth(JsonObject object, String key) {
    return object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsBoolean();
  }

  private static List<String> strings(JsonObject object, String key) {
    if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
      return List.of();
    }
    JsonElement element = object.get(key);
    if (element.isJsonPrimitive()) {
      return List.of(element.getAsString());
    }
    if (!element.isJsonArray()) {
      return List.of();
    }
    List<String> values = new ArrayList<>();
    for (JsonElement item : element.getAsJsonArray()) {
      if (item.isJsonPrimitive()) {
        values.add(item.getAsString());
      }
    }
    return values;
  }

  private static List<String> flat(JsonObject object, String key) {
    List<String> values = new ArrayList<>();
    for (String value : strings(object, key)) {
      for (String part : value.split(",")) {
        if (!part.isBlank()) {
          values.add(part.trim());
        }
      }
    }
    return values;
  }

  private record MemberId(String owner, String name, @Nullable String params, boolean arityOnly, int arity, List<String> paramList) {
  }

  record DeclaredAnnotation(String name, List<SymbolMetadata.AnnotationValue> values, boolean valuesKnown) {
    DeclaredAnnotation(String name, List<SymbolMetadata.AnnotationValue> values) {
      this(name, values, true);
    }
  }
}
