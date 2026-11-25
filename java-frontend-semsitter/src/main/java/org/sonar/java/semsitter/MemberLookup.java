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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

import static org.sonar.java.semsitter.SemaFacts.erase;
import static org.sonar.java.semsitter.SemaFacts.parents;
import static org.sonar.java.semsitter.SemaFacts.parseMember;
import static org.sonar.java.semsitter.SemaFacts.text;

/** Finds declared methods and fields by name and arguments. */
final class MemberLookup {

  private static final Set<String> OBJECT_METHODS = Set.of("equals/1", "hashCode/0", "toString/0");
  private final JSema sema;

  MemberLookup(JSema sema) {
    this.sema = sema;
  }

  @Nullable
  Symbol.VariableSymbol enumConstant(@Nullable String typeName, @Nullable String constName) {
    if (constName == null) {
      return null;
    }
    String ownerName = typeName == null || typeName.isBlank() ? "" : erase(typeName);
    String id = (ownerName.isEmpty() ? "enum" : ownerName) + "#" + constName;
    if (sema.symbols.get(id) instanceof Symbol.VariableSymbol variable) {
      return variable;
    }
    JTypeSymbol type = ownerName.isEmpty() ? null : sema.typeSymbol(ownerName);
    JVariableSymbol created = new JVariableSymbol(sema, id, constName, "enum_member");
    if (type != null) {
      created.owner(type);
      created.type(type.type());
    }
    created.addModifiers(List.of("public", "static", "final"));
    sema.symbols.put(id, created);
    return created;
  }

  /**
   * Return type of a call. A JDK member can name the class type parameter its erased return
   * stands for ({@code List.get} returns {@code E}), which is taken from the receiver.
   */
  Symbol receiverSymbol(Symbol.TypeSymbol type, String name, Type receiverType) {
    String id = (type instanceof JSymbol symbol && symbol.id != null ? symbol.id : type.name()) + "#" + name;
    JSymbol found = sema.symbols.get(id);
    if (found != null) {
      return found;
    }
    JVariableSymbol variable = new JVariableSymbol(sema, id, name, "field");
    variable.owner(type);
    variable.type(receiverType);
    variable.addModifiers(List.of("final"));
    variable.receiver(true);
    sema.symbols.put(id, variable);
    return variable;
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
    if (!sema.declaresType(ownerFqn)) {
      return null;
    }
    List<Symbol.MethodSymbol> candidates = new ArrayList<>();
    collectMethods(ownerFqn, ownerFqn, name, new HashSet<>(), candidates);
    if (!functions.isEmpty()) {
      candidates.removeIf(method -> takesNoFunction(method, functions));
    }
    for (int phase = 0; phase < 3; phase++) {
      int current = phase;
      List<Symbol.MethodSymbol> applicable = candidates.stream().filter(method -> OverloadRanking.applicable(method, arguments, current)).toList();
      if (!applicable.isEmpty()) {
        return applicable.stream().filter(method -> applicable.stream().allMatch(other -> OverloadRanking.moreSpecific(method, other))).findFirst().orElse(applicable.get(0));
      }
    }
    List<Symbol.MethodSymbol> sameArity = candidates.stream()
      .filter(method -> method.isVarArgsMethod() ? (arguments.size() >= (method.parameterTypes().size() - 1)) : (method.parameterTypes().size() == arguments.size()))
      .toList();
    if (sameArity.size() == 1) {
      return sameArity.get(0);
    }
    return null;
  }

  boolean takesNoFunction(Symbol.MethodSymbol method, Set<Integer> functions) {
    List<Type> parameters = method.parameterTypes();
    for (int index : functions) {
      if (index >= parameters.size()) {
        continue;
      }
      Type target = parameters.get(index).erasure();
      if (target.isPrimitive() || target.isArray() || (target.isClass() && !target.isUnknown() && !target.isTypeVar() && !target.symbol().isUnknown()
        && (!target.symbol().isInterface() || (sema.declaresType(target.fullyQualifiedName()) && functionalMethod(target) == null)))) {
        return true;
      }
    }
    return false;
  }

  /** ECJ leaves a call unresolved when no declaration takes this many arguments, rather than guessing one. */
  static boolean arityMatches(Symbol.MethodSymbol method, int argumentCount) {
    int parameters = method.parameterTypes().size();
    return method.isVarArgsMethod() ? (argumentCount >= (parameters - 1)) : (argumentCount == parameters);
  }

  /** The single abstract method of {@code type} when it is a functional interface, else null. */
  @Nullable
  JMethodSymbol functionalMethod(Type type) {
    String fqn = type.erasure().fullyQualifiedName();
    if (!type.isClass() || !type.symbol().isInterface() || !sema.declaresType(fqn)) {
      return null;
    }
    Map<String, JMethodSymbol> found = new LinkedHashMap<>();
    collectAbstractMethods(fqn, new HashSet<>(), found);
    return found.size() == 1 ? found.values().iterator().next() : null;
  }

  private void collectAbstractMethods(String ownerFqn, Set<String> seen, Map<String, JMethodSymbol> found) {
    if (!seen.add(ownerFqn) || !sema.declaresType(ownerFqn)) {
      return;
    }
    JsonObject info = sema.typeInfos.get(sema.sourceName(ownerFqn));
    JsonArray members = info.getAsJsonArray(JSema.MEMBERS);
    if (members != null) {
      for (JsonElement element : members) {
        if (sema.libraryMembers().memberFromTypeInfo(sema.typeSymbol(ownerFqn), element.getAsJsonObject()) instanceof JMethodSymbol method
          && !method.isUnknown() && method.isAbstract()
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
  Symbol.VariableSymbol declaredField(String ownerFqn, String name, Set<String> seen) {
    if (!seen.add(ownerFqn) || !sema.declaresType(ownerFqn)) {
      return null;
    }
    JsonObject info = sema.typeInfos.get(sema.sourceName(ownerFqn));
    JsonArray members = info.getAsJsonArray(JSema.MEMBERS);
    if (members != null) {
      for (JsonElement element : members) {
        Symbol.VariableSymbol field = declaredFieldOf(ownerFqn, name, element.getAsJsonObject());
        if (field != null) {
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

  @Nullable
  private Symbol.VariableSymbol declaredFieldOf(String ownerFqn, String name, JsonObject member) {
    MemberId id = member.has("id") ? parseMember(member.get("id").getAsString()) : null;
    boolean named = id == null ? (name.equals(text(member, "name")) && "f".equals(text(member, "k"))) : (name.equals(id.name()) && id.params() == null);
    if (named && sema.libraryMembers().memberFromTypeInfo(sema.typeSymbol(ownerFqn), member) instanceof Symbol.VariableSymbol field && !field.isUnknown()) {
      return field;
    }
    return null;
  }

  /** Methods named {@code name} that {@code root} has: a static method of an interface is not inherited. */
  private void collectMethods(String root, String ownerFqn, String name, Set<String> seen, List<Symbol.MethodSymbol> found) {
    if (!seen.add(ownerFqn) || !sema.declaresType(ownerFqn)) {
      return;
    }
    JsonObject info = sema.typeInfos.get(sema.sourceName(ownerFqn));
    boolean inheritedInterface = !ownerFqn.equals(root) && sema.typeSymbol(ownerFqn).isInterface();
    JsonArray members = info.getAsJsonArray(JSema.MEMBERS);
    if (members != null) {
      for (JsonElement element : members) {
        addMethod(ownerFqn, name, element.getAsJsonObject(), inheritedInterface, found);
      }
    }
    if (!"<init>".equals(name)) {
      parents(info).forEach(parent -> collectMethods(root, erase(parent), name, seen, found));
    }
  }

  private void addMethod(String ownerFqn, String name, JsonObject member, boolean inheritedInterface, List<Symbol.MethodSymbol> found) {
    MemberId id = member.has("id") ? parseMember(member.get("id").getAsString()) : null;
    boolean named = id == null ? (name.equals(text(member, "name")) && "m".equals(text(member, "k"))) : (name.equals(id.name()) && id.params() != null);
    if (named && sema.libraryMembers().memberFromTypeInfo(sema.typeSymbol(ownerFqn), member) instanceof Symbol.MethodSymbol method && !method.isUnknown()
      && !(inheritedInterface && method.isStatic())) {
      found.add(method);
    }
  }

  /** The type, among {@code ownerFqn} and its supertypes, whose members include {@code member}, or null when none does. */
  @Nullable
  String declaringType(String ownerFqn, JsonObject member, Set<String> seen) {
    JsonObject info = seen.add(ownerFqn) ? sema.typeInfos.get(sema.sourceName(ownerFqn)) : null;
    if (info == null) {
      return null;
    }
    JsonArray members = info.getAsJsonArray(JSema.MEMBERS);
    if (members != null && members.asList().stream().anyMatch(candidate -> candidate == member)) {
      return ownerFqn;
    }
    for (String parent : parents(info)) {
      String found = declaringType(erase(parent), member, seen);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  @Nullable
  JsonObject findMember(String ownerFqn, MemberId parsed, Set<String> seen, Match match) {
    if (!seen.add(ownerFqn)) {
      return null;
    }
    JsonObject info = sema.typeInfos.get(sema.sourceName(ownerFqn));
    JsonObject found = matchMembers(info == null ? null : info.getAsJsonArray(JSema.MEMBERS), parsed, match);
    if (found != null) {
      return found;
    }
    if (info == null || "<init>".equals(parsed.name())) {
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
      return other != null && parsed.name().equals(other.name()) && parsed.arity() == other.arity()
        && (parsed.arityOnly() || other.arityOnly() || Objects.equals(parsed.params(), other.params()));
    }
    if (!parsed.name().equals(text(member, "name"))) {
      return false;
    }
    boolean method = parsed.params() != null;
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
    List<Type> declaredParams = sema.factTypes().parameterTypesOf(descriptor);
    int declaredCount = declaredParams.size();
    return switch (match) {
      case TYPED -> declaredCount == parsed.arity() && sameErasures(declaredParams, parsed.paramList());
      case ARITY -> declaredCount == parsed.arity();
      case VARARGS -> declaredCount > 0 && parsed.arity() >= declaredCount - 1 && declaredParams.get(declaredCount - 1).isArray();
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

  enum Match {
    TYPED, ARITY, VARARGS
  }
}
