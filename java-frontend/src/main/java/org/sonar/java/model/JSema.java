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
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
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
  private final Map<String, JSymbol> symbols = new HashMap<>();
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

  /**
   * Loads the declarations of every type the document references, then keeps {@code loader} to
   * fetch the types reached later, like the return type of a library method.
   */
  public void loadTypes(Function<Set<String>, JsonObject> loader) {
    typeLoader = loader;
    Set<String> names = referencedTypeNames();
    requestedTypes.addAll(names);
    ingestTypes(loader.apply(names));
  }

  public void ingestTypes(JsonObject types) {
    typesLoaded = true;
    for (Map.Entry<String, JsonElement> entry : types.entrySet()) {
      JsonObject info = entry.getValue().getAsJsonObject();
      typeInfos.put(entry.getKey(), info);
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
      return JType.array(this, asJType(getClassType(fullyQualifiedName.substring(0, fullyQualifiedName.length() - 2))));
    }
    if (PRIMITIVES.contains(fullyQualifiedName)) {
      return primitiveType(fullyQualifiedName);
    }
    typeSymbol(fullyQualifiedName);
    return rawNamed(fullyQualifiedName);
  }

  public Runnable getEnvironmentCleaner() {
    return () -> {
    };
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
      ingestTypes(typeLoader.apply(Set.of(fqn)));
      if (typeInfos.containsKey(fqn)) {
        return created;
      }
    }
    if (info != null) {
      applyTypeInfo(created, info);
    } else if (typesLoaded && !isWellKnown(fqn)) {
      created.unknown(true);
      undefinedTypes.add(new JProblem(fqn, JProblem.Type.UNDEFINED_TYPE));
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
    JsonObject fact = facts.get(id);
    List<DeclaredAnnotation> fromValues = fact == null ? List.of() : annotationValues(fact);
    if (!fromValues.isEmpty()) {
      return fromValues;
    }
    List<String> fromFact = fact == null ? List.of() : strings(fact, ANNOTATIONS);
    if (!fromFact.isEmpty()) {
      List<DeclaredAnnotation> library = libraryAnnotations.getOrDefault(id, List.of());
      if (!library.isEmpty()) {
        return library;
      }
      return fromFact.stream().map(name -> new DeclaredAnnotation(erase(name), List.of())).toList();
    }
    return libraryAnnotations.getOrDefault(id, List.of());
  }

  private List<DeclaredAnnotation> annotationValues(JsonObject fact) {
    if (!fact.has("props") || !fact.get("props").isJsonObject()) {
      return List.of();
    }
    List<DeclaredAnnotation> annotations = new ArrayList<>();
    for (String encoded : strings(fact.getAsJsonObject("props"), "annotationValues")) {
      JsonElement parsed = JsonParser.parseString(encoded);
      if (!parsed.isJsonObject()) {
        continue;
      }
      JsonObject annotation = parsed.getAsJsonObject();
      String name = text(annotation, "name");
      if (name != null) {
        annotations.add(new DeclaredAnnotation(erase(name), readAnnotationValues(annotation.get("values"))));
      }
    }
    return annotations;
  }

  /** Annotations and deprecation a {@code types} answer gives a library type or member. */
  private void libraryAnnotations(JSymbol symbol, String id, JsonObject info) {
    List<DeclaredAnnotation> annotations = readAnnotations(info);
    if (annotations.isEmpty()) {
      return;
    }
    libraryAnnotations.put(id, annotations);
    if (annotations.stream().anyMatch(annotation -> "java.lang.Deprecated".equals(annotation.name()))) {
      symbol.deprecated(true);
    }
  }

  private List<DeclaredAnnotation> readAnnotations(JsonObject info) {
    if (!info.has(ANNOTATIONS) || !info.get(ANNOTATIONS).isJsonArray()) {
      return List.of();
    }
    List<DeclaredAnnotation> annotations = new ArrayList<>();
    for (JsonElement element : info.getAsJsonArray(ANNOTATIONS)) {
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
      case "string", "class" -> raw == null || !raw.isJsonPrimitive() ? null : raw.getAsString();
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

  Type invocationReturn(JMethodSymbol method, @Nullable Type receiver) {
    Integer index = method.returnTypeParameter();
    if (index != null && receiver != null && receiver.isParameterized() && receiver.typeArguments().size() > index) {
      return receiver.typeArguments().get(index);
    }
    if (method.returnsSelf() && receiver instanceof JType returned) {
      if (returned.isCapture()) {
        return returned;
      }
      if (returned.isClass()) {
        return JType.capture(this, returned);
      }
    }
    JsonObject declaredReturn = declaredReturn(method);
    if (declaredReturn != null && receiver != null) {
      Map<String, Type> substitution = classSubstitution(method, receiver);
      if (!substitution.isEmpty()) {
        JType substituted = typeFromFact(declaredReturn, substitution, true);
        if (!substituted.isUnknown()) {
          return substituted;
        }
      }
    }
    Type declared = method.returnType().type();
    return declared == null ? Type.UNKNOWN : declared;
  }

  /** The method as seen from {@code receiver}, so its owner keeps that parameterization. */
  JMethodSymbol invocationView(JMethodSymbol method, @Nullable Type receiver, List<Type> parameters, Type returned) {
    JMethodSymbol view = method.withSignature(parameters, returned);
    if (receiver != null && !method.isStatic() && method.owner().type() != null) {
      Type declaring = asSuperType(receiver, method.owner().type().erasure().fullyQualifiedName(), new HashSet<>());
      if (declaring != null && declaring.isParameterized()) {
        view.owner(declaring.symbol());
      }
    }
    return view;
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
   * the declaring type, which may be a supertype of the receiver's own type.
   */
  private void substituteDeclaredParameters(JMethodSymbol method, @Nullable Type receiver, List<String> variables, List<Type> result) {
    List<JsonObject> declared = declaredParameters(method);
    if (declared.size() != result.size() || receiver == null) {
      return;
    }
    Map<String, Type> substitution = classSubstitution(method, receiver);
    if (substitution.isEmpty()) {
      return;
    }
    for (int i = 0; i < declared.size(); i++) {
      if (variables.isEmpty() || variables.get(i).isEmpty()) {
        result.set(i, typeFromFact(declared.get(i), substitution));
      }
    }
  }

  /** Maps the type variables of the type declaring {@code method} to the arguments {@code receiver} gives them. */
  private Map<String, Type> classSubstitution(JMethodSymbol method, Type receiver) {
    String ownerFqn = method.owner().type().erasure().fullyQualifiedName();
    JsonObject info = typeInfos.get(sourceName(ownerFqn));
    List<String> names = info == null ? List.of() : strings(info, "typeVars");
    if (names.isEmpty()) {
      return Map.of();
    }
    Type declaring = asSuperType(receiver, ownerFqn, new HashSet<>());
    if (declaring == null || !declaring.isParameterized()) {
      return Map.of();
    }
    List<Type> arguments = declaring.typeArguments();
    Map<String, Type> substitution = new HashMap<>();
    for (int i = 0; i < names.size() && i < arguments.size(); i++) {
      substitution.put(names.get(i), arguments.get(i));
    }
    return substitution;
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
  private Type instantiateSupertype(Type from, Type supertype) {
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

  @Nullable
  private static Type methodArgument(List<String> variables, String base, List<Type> arguments) {
    for (int i = 0; i < variables.size() && i < arguments.size(); i++) {
      String variable = variables.get(i);
      if (!variable.replace("[]", "").equals(base)) {
        continue;
      }
      Type argument = arguments.get(i);
      if (!variable.equals(base)) {
        boolean last = i == variables.size() - 1;
        if (arguments.size() == variables.size() && argument.isArray()) {
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
    JsonArray members = typeInfos.get(sourceName(ownerFqn)).getAsJsonArray("members");
    if (members == null) {
      return null;
    }
    List<String> erased = method.parameterTypes().stream().map(t -> t.erasure().fullyQualifiedName()).toList();
    for (JsonElement element : members) {
      JsonObject member = element.getAsJsonObject();
      String descriptor = text(member, "desc");
      if (method.name().equals(text(member, "name")) && descriptor != null && member.has("paramVars") && member.get("paramVars").isJsonArray()
        && parameterTypesOf(descriptor).stream().map(t -> t.erasure().fullyQualifiedName()).toList().equals(erased)) {
        return member;
      }
    }
    return null;
  }

  Symbol memberFromTypeInfo(JTypeSymbol owner, JsonObject member) {
    if (member.has("id") && !member.get("id").isJsonNull()) {
      return resolve(member.get("id").getAsString());
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
        field.constantValue(constant);
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
    List<String> thrown = strings(member, "throws");
    if (!thrown.isEmpty()) {
      method.thrownTypes(thrown.stream().map(this::typeFromName).toList());
    }
    if (member.has("varargs") && member.get("varargs").getAsBoolean()) {
      method.varArgs(true);
    }
    if (member.has("retParam") && !member.get("retParam").isJsonNull()) {
      method.returnTypeParameter(member.get("retParam").getAsInt());
    }
    if (member.get("retType") instanceof JsonObject retType) {
      method.declaredReturn(retType);
    }
    method.returnsSelf(member.has("retSelf") && member.get("retSelf").getAsBoolean());
    symbols.put(id, method);
    return method;
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
    List<String> thrown = strings(member, "throws");
    if (symbol instanceof JMethodSymbol method && !thrown.isEmpty()) {
      method.thrownTypes(thrown.stream().map(this::typeFromName).toList());
    }
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
      field.unknown(true);
      symbols.put(id, field);
      return field;
    }
    JMethodSymbol method = new JMethodSymbol(this, id, parsed.name);
    method.owner(parsed.arityOnly ? Symbol.TypeSymbol.UNKNOWN_TYPE : owner);
    method.unknown(parsed.arityOnly);
    fillMethod(method, facts.get(id), id);
    if ("<init>".equals(parsed.name) && facts.get(id) == null && member == null) {
      // The implicit default constructor has the access of its class.
      method.addModifiers(List.of("public", "protected", "private").stream().filter(owner::hasModifier).toList());
    }
    if (method.parameterTypes().isEmpty() && !parsed.arityOnly && parsed.params != null && !parsed.params.isEmpty()) {
      List<Type> types = new ArrayList<>();
      for (String param : splitParams(parsed.params)) {
        types.add(typeFromName(param));
      }
      method.parameterTypes(types);
    }
    symbols.put(id, method);
    return method;
  }

  /**
   * The method {@code name} of {@code ownerFqn} or of one of its supertypes that Java would pick for {@code arguments}:
   * applicable by subtyping first, then with boxing, then as varargs, and the most specific of those.
   */
  @Nullable
  Symbol.MethodSymbol declaredMethod(String ownerFqn, String name, List<Type> arguments) {
    if (!declaresType(ownerFqn)) {
      return null;
    }
    List<Symbol.MethodSymbol> candidates = new ArrayList<>();
    collectMethods(ownerFqn, name, new HashSet<>(), candidates);
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
    return candidates.size() == 1 ? candidates.get(0) : null;
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
    JsonArray members = info.getAsJsonArray("members");
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
    JsonArray members = info.getAsJsonArray("members");
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

  private void collectMethods(String ownerFqn, String name, Set<String> seen, List<Symbol.MethodSymbol> found) {
    if (!seen.add(ownerFqn) || !declaresType(ownerFqn)) {
      return;
    }
    JsonObject info = typeInfos.get(sourceName(ownerFqn));
    JsonArray members = info.getAsJsonArray("members");
    if (members != null) {
      for (JsonElement element : members) {
        JsonObject member = element.getAsJsonObject();
        MemberId id = member.has("id") ? parseMember(member.get("id").getAsString()) : null;
        boolean named = id == null ? (name.equals(text(member, "name")) && "m".equals(text(member, "k"))) : (name.equals(id.name) && id.params != null);
        if (named && memberFromTypeInfo(typeSymbol(ownerFqn), member) instanceof Symbol.MethodSymbol method && !method.isUnknown()) {
          found.add(method);
        }
      }
    }
    if (!"<init>".equals(name)) {
      parents(info).forEach(parent -> collectMethods(erase(parent), name, seen, found));
    }
  }

  /** The overload that matches {@code arguments} when {@code candidate} is more specific than {@code current}. */
  Symbol.MethodSymbol preferOverload(Symbol.MethodSymbol current, @Nullable Symbol.MethodSymbol candidate, List<Type> arguments) {
    if (candidate == null || candidate == current || candidate.isUnknown()) {
      return current;
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
    JsonObject found = matchMembers(info == null ? null : info.getAsJsonArray("members"), parsed, match);
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
      case TYPED -> declaredCount == parsed.arity && sameErasures(declaredParams, splitParams(parsed.params));
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
      for (String param : splitParams(parsed.params)) {
        types.add(typeFromName(param));
      }
      method.parameterTypes(types);
    }
    if ("<init>".equals(method.name())) {
      method.returnTypeSymbol(primitiveSymbol("void"));
    }
    method.parametrized(id.indexOf('<') >= 0);
    if (fact != null && fact.get("props") instanceof JsonObject props) {
      List<String> thrown = strings(props, "thrownTypes");
      if (!thrown.isEmpty()) {
        method.thrownTypes(thrown.stream().map(this::typeFromName).toList());
      }
    }
  }

  private void applyFact(JSymbol symbol, JsonObject fact) {
    String kind = text(fact, "kind");
    if (symbol instanceof JTypeSymbol type) {
      boolean interfaceKind = "interface".equals(kind) || "annotation".equals(kind);
      type.interfaceSymbol(interfaceKind);
      type.annotationSymbol("annotation".equals(kind));
      type.enumSymbol("enum".equals(kind));
      List<String> extend = strings(fact, "extends");
      List<String> implement = strings(fact, "implements");
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
    if (info.has("members")) {
      for (JsonElement member : info.getAsJsonArray("members")) {
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
      return captureWildcard ? JType.capture(this, getClassType("java.lang.Object") instanceof JType object ? object : JType.unknown(this)) : JType.unknown(this);
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
    return JType.named(this, erased, args);
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
    return JType.primitive(this, name);
  }

  private JType asJType(Type type) {
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

  private static MemberId parseMember(String id) {
    int hash = id.indexOf('#');
    if (hash < 0) {
      return null;
    }
    String owner = id.substring(0, hash);
    String rest = id.substring(hash + 1);
    int paren = rest.endsWith(")") ? rest.lastIndexOf('(') : -1;
    if (paren < 0) {
      return new MemberId(owner, rest, null, false, -1);
    }
    String name = rest.substring(0, paren);
    String params = rest.substring(paren + 1, rest.length() - 1);
    boolean arityOnly = !params.isEmpty() && params.chars().allMatch(Character::isDigit);
    int arity = arityOnly ? Integer.parseInt(params) : (params.isEmpty() ? 0 : splitParams(params).size());
    return new MemberId(owner, name, params, arityOnly, arity);
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
    return boundary < 0 ? fqn : fqn.substring(boundary + 1);
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
  private static String text(JsonObject object, String key) {
    if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
      return null;
    }
    JsonElement element = object.get(key);
    return element.isJsonPrimitive() ? element.getAsString() : null;
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

  private record MemberId(String owner, String name, @Nullable String params, boolean arityOnly, int arity) {
  }

  record DeclaredAnnotation(String name, List<SymbolMetadata.AnnotationValue> values) {
  }
}
