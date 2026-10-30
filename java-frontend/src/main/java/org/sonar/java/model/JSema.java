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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Semantic model of one translated file. Symbols are created from the SemSitter dump and from the
 * {@code types} response (project graph, classpath declarations, embedded JDK table).
 */
public final class JSema implements Sema {

  private static final Set<String> PRIMITIVES = Set.of("byte", "short", "int", "long", "char", "float", "double", "boolean", "void");

  private final Map<String, JsonObject> facts = new HashMap<>();
  private final Map<String, JsonObject> dumpSymbols = new HashMap<>();
  private final Map<String, JsonObject> typeInfos = new HashMap<>();
  private final Map<String, JSymbol> symbols = new HashMap<>();
  private final Map<String, JTypeSymbol> typesByName = new HashMap<>();
  private final Map<String, JType> rawTypes = new HashMap<>();
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

  JTypeSymbol typeSymbol(String fqn) {
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
    if (id == null) {
      return List.of();
    }
    JsonObject fact = facts.get(id);
    return fact == null ? List.of() : strings(fact, "annotations");
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
    symbols.put(id, variable);
    return variable;
  }

  Type invocationReturn(JMethodSymbol method, @Nullable Type receiver) {
    Integer index = method.returnTypeParameter();
    if (index != null && receiver != null && receiver.isParameterized() && receiver.typeArguments().size() > index) {
      return receiver.typeArguments().get(index);
    }
    Type declared = method.returnType().type();
    return declared == null ? Type.UNKNOWN : declared;
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
    if (member.has("varargs") && member.get("varargs").getAsBoolean()) {
      method.varArgs(true);
    }
    if (member.has("retParam") && !member.get("retParam").isJsonNull()) {
      method.returnTypeParameter(member.get("retParam").getAsInt());
    }
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
    if (ownerId == null && symbol instanceof JMethodSymbol) {
      ownerId = text(fact, "parent");
    }
    if (ownerId != null) {
      symbol.owner(resolve(ownerId));
    } else if (symbol instanceof JTypeSymbol typeSymbol && !typeSymbol.isTypeParameter()) {
      symbol.owner(packageSymbol(packageName(erase(id))));
    }
    if (fact != null) {
      applyFact(symbol, fact);
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
    if ("array".equals(parsed.owner) && "length".equals(parsed.name)) {
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

  @Nullable
  private JsonObject findMember(String ownerFqn, MemberId parsed, Set<String> seen, Match match) {
    if (!seen.add(ownerFqn)) {
      return null;
    }
    JsonObject info = typeInfos.get(ownerFqn);
    JsonObject found = matchMembers(info == null ? null : info.getAsJsonArray("members"), parsed, match);
    if (found != null) {
      return found;
    }
    JTypeSymbol type = typesByName.get(ownerFqn);
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
    for (String annotation : strings(fact, "annotations")) {
      if ("java.lang.Deprecated".equals(erase(annotation))) {
        symbol.deprecated(true);
      }
    }
    if (symbol.isVariableSymbol() && ("enum".equals(kind) || "enum_member".equals(kind))) {
      symbol.enumSymbol(true);
      symbol.addModifiers(List.of("public", "static", "final"));
    }
  }

  private void applyTypeInfo(JTypeSymbol symbol, JsonObject info) {
    String kind = text(info, "kind");
    boolean interfaceKind = "interface".equals(kind) || "annotation".equals(kind);
    symbol.interfaceSymbol(interfaceKind);
    symbol.annotationSymbol("annotation".equals(kind));
    symbol.enumSymbol("enum".equals(kind));
    if (interfaceKind) {
      List<String> extend = strings(info, "super");
      symbol.interfaceNames(extend.isEmpty() ? strings(info, "interfaces") : extend);
      symbol.superName(null);
    } else {
      symbol.superName(text(info, "super"));
      symbol.interfaceNames(strings(info, "interfaces"));
    }
    symbol.addModifiers(flat(info, "modifiers"));
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
    if (truth(member, "static")) {
      modifiers.add("static");
    }
    if (truth(member, "final")) {
      modifiers.add("final");
    }
    if (truth(member, "abstract")) {
      modifiers.add("abstract");
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
    String kind = text(fact, "kind");
    if ("array".equals(kind) && fact.getAsJsonObject("elem") != null) {
      return JType.array(this, typeFromFact(fact.getAsJsonObject("elem")));
    }
    if ("typevar".equals(kind)) {
      String id = text(fact, "id");
      int open = id == null ? -1 : id.lastIndexOf('<');
      String name = open >= 0 && id.endsWith(">") ? id.substring(open + 1, id.length() - 1) : id;
      return JType.typeVar(this, name == null ? "" : name, id);
    }
    if ("unknown".equals(kind) || "wildcard".equals(kind)) {
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
          args.add(typeFromFact(arg.getAsJsonObject()));
        }
      }
    }
    String erased = erase(id);
    if (PRIMITIVES.contains(erased)) {
      return primitiveType(erased);
    }
    return JType.named(this, erased, args);
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
    return rawTypes.computeIfAbsent(fqn, name -> JType.named(this, name, List.of()));
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

  private Symbol packageSymbol(String name) {
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
    int dot = name.lastIndexOf('.');
    pkg.owner(dot < 0 ? Symbol.ROOT_PACKAGE : packageSymbol(name.substring(0, dot)));
    return pkg;
  }

  @Nullable
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
}
