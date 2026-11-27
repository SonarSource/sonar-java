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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonarsource.semsitter.wire.Fact;

import static org.sonar.java.semsitter.SemaFacts.erase;
import static org.sonar.java.semsitter.SemaFacts.flat;
import static org.sonar.java.semsitter.SemaFacts.isTypeFact;
import static org.sonar.java.semsitter.SemaFacts.parseMember;
import static org.sonar.java.semsitter.SemaFacts.prop;
import static org.sonar.java.semsitter.SemaFacts.strings;
import static org.sonar.java.semsitter.SemaFacts.text;

/** Creates method, field, type parameter, array, primitive and local type symbols. */
final class MemberFactory {

  private static final String PUBLIC = "public";
  private static final String FINAL = "final";
  private static final String CONSTRUCTOR_NAME = "<init>";
  private static final String ANNOTATION = "annotation";
  private static final String INTERFACE = "interface";
  private static final String SUPER = "super";
  private static final String TYPE_PARAMETERS = "typeParameters";

  private final JSema sema;
  final Set<String> creating = new HashSet<>();

  MemberFactory(JSema sema) {
    this.sema = sema;
  }

  Symbol.TypeSymbol arraySymbol(JType element) {
    String name = element.fullyQualifiedName() + "[]";
    JTypeSymbol found = sema.typesByName.get(name);
    if (found != null) {
      return found;
    }
    JTypeSymbol symbol = new JTypeSymbol(sema, name, element.name() + "[]", false);
    symbol.elementFqn(element.fullyQualifiedName());
    symbol.owner(Symbol.ROOT_PACKAGE);
    symbol.type(JType.array(sema, element));
    sema.typesByName.put(name, symbol);
    return symbol;
  }

  Symbol.TypeSymbol primitiveSymbol(String name) {
    JTypeSymbol found = sema.typesByName.get(name);
    if (found != null) {
      return found;
    }
    JTypeSymbol symbol = new JTypeSymbol(sema, name, name, false);
    symbol.owner(Symbol.ROOT_PACKAGE);
    symbol.type(sema.primitiveType(name));
    sema.typesByName.put(name, symbol);
    return symbol;
  }

  /** The upper bound of the type variable with this id: its first declared bound, or {@code Object}. */
  Type typeVariableBound(@Nullable String id) {
    if (id != null && sema.symbols.get(id) instanceof JTypeSymbol parameter && parameter.firstBound() instanceof Type bound && !bound.isUnknown()) {
      return bound;
    }
    return sema.getClassType("java.lang.Object");
  }

  /** Every declared bound of the type variable with this id, or {@code Object} when it has none. */
  List<Type> typeVariableBounds(@Nullable String id) {
    if (id != null && sema.symbols.get(id) instanceof JTypeSymbol parameter && !parameter.bounds().isEmpty()) {
      return parameter.bounds();
    }
    return List.of(sema.getClassType("java.lang.Object"));
  }

  JSymbol typeParameter(String id) {
    int open = id.lastIndexOf('<');
    String name = open >= 0 && id.endsWith(">") ? id.substring(open + 1, id.length() - 1) : id;
    JTypeSymbol symbol = new JTypeSymbol(sema, id, name, true);
    symbol.type(JType.typeVar(sema, name, id));
    sema.symbols.put(id, symbol);
    return symbol;
  }

  JSymbol createMember(String id) {
    if (!creating.add(id)) {
      return sema.unknownSymbol;
    }
    try {
      return createMemberOnce(id);
    } finally {
      creating.remove(id);
    }
  }

  private JSymbol createMemberOnce(String id) {
    MemberId parsed = Objects.requireNonNull(parseMember(id));
    if (JSema.ARRAY.equals(parsed.owner()) && "length".equals(parsed.name())) {
      return createArrayLength(id);
    }
    String ownerName = erase(parsed.owner());
    JTypeSymbol owner = sema.typeSymbol(ownerName);
    JsonObject member = findLibraryMember(ownerName, parsed);
    String memberId = member == null ? null : text(member, "id");
    if (member != null && (memberId == null || !creating.contains(memberId))) {
      String declaring = sema.lookup().declaringType(ownerName, member, new HashSet<>());
      JSymbol resolved = sema.libraryMembers().memberFromTypeInfo(declaring == null ? owner : sema.typeSymbol(declaring), member);
      sema.symbols.put(id, resolved);
      return resolved;
    }
    if (parsed.params() == null) {
      return createFieldLikeMember(id, parsed, owner);
    }
    return createMethod(id, parsed, owner, ownerName, member == null);
  }

  private JSymbol createArrayLength(String id) {
    JVariableSymbol length = new JVariableSymbol(sema, id, "length", "field");
    length.addModifiers(List.of(PUBLIC, FINAL));
    length.type(sema.primitiveType("int"));
    length.owner(Symbol.UNKNOWN_SYMBOL);
    sema.symbols.put(id, length);
    return length;
  }

  @Nullable
  private JsonObject findLibraryMember(String ownerName, MemberId parsed) {
    JsonObject member = null;
    if (parsed.params() != null && !parsed.arityOnly()) {
      member = sema.lookup().findMember(ownerName, parsed, new HashSet<>(), MemberLookup.Match.TYPED);
    }
    if (member == null) {
      member = sema.lookup().findMember(ownerName, parsed, new HashSet<>(), MemberLookup.Match.ARITY);
    }
    if (member == null && parsed.params() != null) {
      member = sema.lookup().findMember(ownerName, parsed, new HashSet<>(), MemberLookup.Match.VARARGS);
    }
    return member;
  }

  private JSymbol createFieldLikeMember(String id, MemberId parsed, JTypeSymbol owner) {
    if (parsed.name().contains("$lambda$")) {
      JMethodSymbol lambda = new JMethodSymbol(sema, id, parsed.name());
      lambda.owner(owner);
      lambda.lambda(true);
      sema.symbols.put(id, lambda);
      return lambda;
    }
    if (parsed.name().contains(")$")) {
      return createTypeInMethodBody(id, parsed);
    }
    JVariableSymbol field = new JVariableSymbol(sema, id, parsed.name(), "field");
    field.owner(owner);
    Fact fieldFact = factOf(id);
    List<String> fieldType = fieldFact == null ? List.of() : flat(fieldFact.getTypeList());
    if (fieldFact != null && fieldType.isEmpty() && owner.isEnum()) {
      // the facts of an enum constant carry no type: it is of its enum
      applyFact(field, fieldFact);
      field.type(owner.type());
      field.enumSymbol(true);
    } else if (fieldFact == null || fieldType.isEmpty()) {
      field.unknown(true);
    } else {
      applyFact(field, fieldFact);
      JsonObject declared = sema.declaredMembers.projectFieldTypes.get(id);
      field.type(declared != null ? sema.factTypes().typeFromFact(declared) : sema.factTypes().typeFromName(fieldType.get(0)));
    }
    markDeprecated(field, id);
    sema.symbols.put(id, field);
    return field;
  }

  private JSymbol createMethod(String id, MemberId parsed, JTypeSymbol owner, String ownerName, boolean noLibraryMember) {
    JMethodSymbol method = new JMethodSymbol(sema, id, parsed.name());
    boolean undeclaredOwner = sema.noLibraries && noLibraryMember && !sema.declaresType(ownerName);
    method.owner(parsed.arityOnly() || undeclaredOwner ? Symbol.TypeSymbol.UNKNOWN_TYPE : owner);
    method.unknown(parsed.arityOnly() || undeclaredOwner);
    fillMethod(method, factOf(id), id);
    if (CONSTRUCTOR_NAME.equals(parsed.name()) && factOf(id) == null && noLibraryMember) {
      // The implicit default constructor has the access of its class.
      method.addModifiers(List.of(PUBLIC, "protected", "private").stream().filter(owner::hasModifier).toList());
    }
    Fact methodFact = factOf(id);
    if (methodFact != null && !parsed.arityOnly()) {
      applyFact(method, methodFact);
    }
    markDeprecated(method, id);
    sema.symbols.put(id, method);
    return method;
  }

  /** The facts of a project member: those of this file, else those a {@code types} answer gave with it. */
  @Nullable
  private Fact factOf(String id) {
    Fact fact = sema.facts.get(id);
    return fact != null ? fact : sema.declaredMembers.projectMemberFacts.get(id);
  }

  private void markDeprecated(JSymbol symbol, String id) {
    if (sema.annotationFacts().declaredAnnotations(id).stream().anyMatch(annotation -> "java.lang.Deprecated".equals(annotation.name()))) {
      symbol.deprecated(true);
    }
  }

  void fillMethod(JMethodSymbol method, @Nullable Fact fact, String id) {
    MemberId parsed = parseMember(id);
    List<String> returns = fact == null ? List.of() : flat(fact.getReturnsList());
    if (!returns.isEmpty()) {
      method.returnTypeSymbol(sema.factTypes().typeFromName(returns.get(0)).symbol());
    }
    fillParameterTypes(method, fact, parsed);
    if (CONSTRUCTOR_NAME.equals(method.name())) {
      method.returnTypeSymbol(primitiveSymbol("void"));
    }
    fillVarArgs(method, fact, id);
    method.parametrized(id.replace(CONSTRUCTOR_NAME, "").replace("<clinit>", "").indexOf('<') >= 0);
  }

  private void fillParameterTypes(JMethodSymbol method, @Nullable Fact fact, @Nullable MemberId parsed) {
    List<String> factParams = fact == null ? List.of() : flat(fact.getParamsList());
    if (!factParams.isEmpty() && (parsed == null || parsed.arityOnly() || factParams.size() == parsed.arity())) {
      method.parameterTypes(typesFromNames(factParams));
    } else if (parsed != null && !parsed.arityOnly() && parsed.params() != null && !parsed.params().isEmpty()) {
      method.parameterTypes(typesFromNames(parsed.paramList()));
    }
  }

  private List<Type> typesFromNames(List<String> names) {
    List<Type> types = new ArrayList<>();
    for (String name : names) {
      types.add(sema.factTypes().typeFromName(name));
    }
    return types;
  }

  private void fillVarArgs(JMethodSymbol method, @Nullable Fact fact, String id) {
    List<String> declaredParams = fact == null ? List.of() : flat(prop(fact, "param_types"));
    Boolean served = sema.declaredMembers.projectVarArgs.get(id);
    if (served != null) {
      method.varArgs(served);
      method.varArgsKnown(true);
    } else if (!declaredParams.isEmpty()) {
      method.varArgs(declaredParams.get(declaredParams.size() - 1).endsWith("..."));
      method.varArgsKnown(true);
    }
  }

  void applyFact(JSymbol symbol, Fact fact) {
    String kind = fact.getKind();
    if (symbol instanceof JTypeSymbol type) {
      applyTypeFact(type, fact, kind);
    }
    symbol.addModifiers(flat(prop(fact, "modifiers")));
    if (symbol instanceof JTypeSymbol) {
      symbol.addModifiers(implicitTypeModifiers(kind, text(fact.getParent())));
    }
    for (String annotation : fact.getAnnotationsList()) {
      if ("java.lang.Deprecated".equals(erase(annotation))) {
        symbol.deprecated(true);
      }
    }
    if (symbol.isVariableSymbol() && ("enum".equals(kind) || "enum_member".equals(kind))) {
      symbol.enumSymbol(true);
      symbol.addModifiers(List.of(PUBLIC, JSema.STATIC, FINAL));
    }
  }

  private static void applyTypeFact(JTypeSymbol type, Fact fact, String kind) {
    boolean interfaceKind = INTERFACE.equals(kind) || ANNOTATION.equals(kind);
    type.interfaceSymbol(interfaceKind);
    type.annotationSymbol(ANNOTATION.equals(kind));
    List<String> extend = fact.getExtendsList();
    List<String> implement = fact.getImplementsList();
    type.enumSymbol("enum".equals(kind) || extend.stream().map(SemaFacts::erase).anyMatch("java.lang.Enum"::equals));
    if (interfaceKind) {
      type.interfaceNames(extend.isEmpty() ? implement : extend);
    } else {
      type.superName(extend.isEmpty() ? null : extend.get(0));
      type.interfaceNames(implement);
    }
  }

  /**
   * Modifiers Java implies: member interfaces, enums, records and annotations are static; enums and records are
   * final (an enum constant with a body is not modelled).
   */
  private List<String> implicitTypeModifiers(String kind, @Nullable String parent) {
    List<String> implied = new ArrayList<>();
    boolean member = parent != null && !parent.endsWith("::<module>") && sema.facts.get(parent) != null
      && isTypeFact(sema.facts.get(parent));
    if (member && (INTERFACE.equals(kind) || "enum".equals(kind) || JSema.RECORD.equals(kind) || ANNOTATION.equals(kind))) {
      implied.add(JSema.STATIC);
    }
    if ("enum".equals(kind) || JSema.RECORD.equals(kind)) {
      implied.add(FINAL);
    }
    return implied;
  }

  void applyTypeInfo(JTypeSymbol symbol, JsonObject info) {
    JsonObject genericSuper = info.get("superType") instanceof JsonObject superType ? superType : null;
    String kind = text(info, "kind");
    boolean interfaceKind = INTERFACE.equals(kind) || ANNOTATION.equals(kind);
    symbol.interfaceSymbol(interfaceKind);
    symbol.annotationSymbol(ANNOTATION.equals(kind));
    symbol.enumSymbol("enum".equals(kind) || "java.lang.Enum".equals(text(info, SUPER)));
    if (interfaceKind) {
      List<String> extend = strings(info, SUPER);
      symbol.interfaceNames(extend.isEmpty() ? strings(info, "interfaces") : extend);
      symbol.superName(null);
    } else {
      symbol.superName(text(info, SUPER));
      symbol.interfaceNames(strings(info, "interfaces"));
    }
    symbol.genericSupertypes(genericSuper, FactTypes.objects(info, "interfaceTypes"));
    symbol.addModifiers(flat(info, "modifiers"));
    sema.annotationFacts().libraryAnnotations(symbol, symbol.id == null ? symbol.name() : symbol.id, info);
    JsonElement typeParameters = info.get(TYPE_PARAMETERS);
    if (typeParameters != null && !typeParameters.isJsonNull()) {
      symbol.typeParameterCount(typeParameters.getAsInt());
    }
    List<JsonObject> members = new ArrayList<>();
    if (info.has(JSema.MEMBERS)) {
      for (JsonElement member : info.getAsJsonArray(JSema.MEMBERS)) {
        members.add(member.getAsJsonObject());
      }
    }
    symbol.memberFacts(members);
    symbol.unknown(false);
  }

  /** The unknown type a written name stands for when nothing declares it. */
  JTypeSymbol recoveredType(String name) {
    JTypeSymbol recovered = sema.typeSymbol(name);
    if (!recovered.isUnknown() && !sema.declaresType(name)) {
      recovered.unknown(true);
    }
    return recovered;
  }

  @Nullable
  /**
   * Local and anonymous classes are identified as members of their enclosing type, with the declaring
   * method embedded in the member name, as in {@code com.acme.Owner#create()$Local$1}. They carry no
   * declaration of their own, so the symbol is rebuilt from the identifier and owned by whatever
   * declares it, which is the method for a local class and the local class for a nested anonymous one.
   */
  private JSymbol createTypeInMethodBody(String id, MemberId parsed) {
    int lastSegment = parsed.name().lastIndexOf('$');
    String simpleName = parsed.name().substring(lastSegment + 1);
    boolean anonymous = simpleName.chars().allMatch(Character::isDigit);
    JTypeSymbol symbol = new JTypeSymbol(sema, id, anonymous ? "" : simpleName, false);
    sema.symbols.put(id, symbol);
    sema.typesByName.putIfAbsent(id, symbol);
    symbol.owner(sema.resolve(parsed.owner() + "#" + parsed.name().substring(0, lastSegment)));
    symbol.type(sema.rawNamed(id));
    Fact fact = sema.facts.get(id);
    if (fact != null) {
      applyFact(symbol, fact);
    }
    return symbol;
  }
}
