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
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonarsource.semsitter.wire.Fact;
import org.sonarsource.semsitter.wire.FactValue;
import org.sonarsource.semsitter.wire.TypeRef;

import static org.sonar.java.semsitter.SemaFacts.applyMemberFlags;
import static org.sonar.java.semsitter.SemaFacts.constantOf;
import static org.sonar.java.semsitter.SemaFacts.erase;
import static org.sonar.java.semsitter.SemaFacts.fact;
import static org.sonar.java.semsitter.SemaFacts.packageName;
import static org.sonar.java.semsitter.SemaFacts.parseMember;
import static org.sonar.java.semsitter.SemaFacts.simpleName;
import static org.sonar.java.semsitter.SemaFacts.strings;
import static org.sonar.java.semsitter.SemaFacts.text;
import static org.sonar.java.semsitter.SemaFacts.truth;
import static org.sonar.java.semsitter.SemaFacts.typeFact;

/** Members of library and project types, created from type information and symbol dumps. */
final class LibraryMembers {

  private static final String THROWS = "throws";
  private static final String VARARGS = "varargs";
  private static final String CONSTANT = "constant";
  private static final String RET_PARAM = "retParam";
  private final JSema sema;

  LibraryMembers(JSema sema) {
    this.sema = sema;
  }

  /** Thrown types a {@code types} answer gives a project method, or null when it was not answered. */
  @Nullable
  List<Type> projectThrown(@Nullable String id) {
    List<String> names = id == null ? null : sema.declaredMembers.projectThrows.get(id);
    return names == null ? null : names.stream().map(sema.factTypes()::typeFromName).toList();
  }

  /** The typed annotations and thrown types of the project members of a {@code types} answer. */
  void registerProjectMembers(JsonObject info) {
    JsonElement membersElement = info.get(JSema.MEMBERS);
    if (!"project".equals(text(info, "origin")) || !(membersElement instanceof JsonArray members)) {
      return;
    }
    for (JsonElement element : members) {
      if (element instanceof JsonObject member && member.get("id") instanceof JsonPrimitive memberId) {
        registerProjectMember(member, memberId);
      }
    }
  }

  private void registerProjectMember(JsonObject member, JsonPrimitive memberId) {
    List<String> spellings = new ArrayList<>(strings(member, "useIds"));
    spellings.add(memberId.getAsString());
    List<DeclaredAnnotation> annotations = sema.annotationFacts().readAnnotations(member);
    for (String id : spellings) {
      if (!annotations.isEmpty()) {
        sema.declaredMembers.libraryAnnotations.put(id, annotations);
      }
      sema.annotationFacts().registerParameterAnnotations(id, member);
      if (member.get(THROWS) instanceof JsonArray) {
        sema.declaredMembers.projectThrows.put(id, strings(member, THROWS));
      }
      if (member.get(VARARGS) instanceof JsonPrimitive varArgs && varArgs.isBoolean()) {
        sema.declaredMembers.projectVarArgs.put(id, varArgs.getAsBoolean());
      }
      if (member.get("facts") instanceof JsonObject memberFact) {
        sema.declaredMembers.projectMemberFacts.computeIfAbsent(id, key -> fact(memberFact));
      }
      if (member.get("declaredType") instanceof JsonObject declared && (declared.get("args") instanceof JsonArray || "typevar".equals(text(declared, "kind")))) {
        sema.declaredMembers.projectFieldTypes.putIfAbsent(id, declared);
      }
    }
  }

  JSymbol memberFromTypeInfo(JTypeSymbol owner, JsonObject member) {
    if (member.has("id") && !member.get("id").isJsonNull()) {
      return projectMember(member);
    }
    String name = text(member, "name");
    String descriptor = text(member, "desc");
    String id = (owner.id == null ? owner.name() : owner.id) + "#" + name + (descriptor == null ? "" : descriptor);
    JSymbol existing = sema.symbols.get(id);
    if (existing != null) {
      return existing;
    }
    if ("f".equals(text(member, "k"))) {
      return newField(owner, id, name, descriptor, member);
    }
    return newMethod(owner, id, name, descriptor, member);
  }

  private JSymbol projectMember(JsonObject member) {
    JSymbol project = sema.resolve(member.get("id").getAsString());
    if (sema.annotationFacts().declaredAnnotations(project.id).stream().anyMatch(annotation -> "java.lang.Deprecated".equals(annotation.name()))) {
      project.deprecated(true);
    }
    String constant = text(member, CONSTANT);
    if (constant != null && project instanceof JVariableSymbol field && field.declaration() == null) {
      field.constantValue(constantOf(field.type(), constant));
    }
    return project;
  }

  private JSymbol newField(JTypeSymbol owner, String id, @Nullable String name, @Nullable String descriptor, JsonObject member) {
    JVariableSymbol field = new JVariableSymbol(sema, id, name == null ? "" : name, "field");
    field.owner(owner);
    if (descriptor != null) {
      field.type(sema.factTypes().readDescriptor(descriptor, new int[] {0}));
    }
    applyMemberFlags(field, member);
    sema.annotationFacts().libraryAnnotations(field, id, member);
    if (truth(member, "enum")) {
      field.enumSymbol(true);
    }
    String constant = text(member, CONSTANT);
    if (constant != null) {
      field.constantValue(constantOf(field.type(), constant));
    }
    sema.symbols.put(id, field);
    return field;
  }

  private JSymbol newMethod(JTypeSymbol owner, String id, @Nullable String name, @Nullable String descriptor, JsonObject member) {
    JMethodSymbol method = new JMethodSymbol(sema, id, name == null ? "" : name);
    method.owner(owner);
    if (descriptor != null && descriptor.startsWith("(")) {
      method.parameterTypes(sema.factTypes().parameterTypesOf(descriptor));
      method.returnTypeSymbol(sema.factTypes().readDescriptor(descriptor, new int[] {descriptor.indexOf(')') + 1}).symbol());
    }
    applyMemberFlags(method, member);
    if (owner.isInterface() && !truth(member, JSema.ABSTRACT) && !truth(member, JSema.STATIC) && !"private".equals(text(member, "vis"))) {
      method.addModifiers(List.of("default"));
    }
    sema.annotationFacts().libraryAnnotations(method, id, member);
    List<String> thrown = strings(member, THROWS);
    if (!thrown.isEmpty()) {
      method.thrownTypes(thrown.stream().map(sema.factTypes()::typeFromName).toList());
    }
    if (member.has(VARARGS) && member.get(VARARGS).isJsonPrimitive()) {
      method.varArgs(member.get(VARARGS).getAsBoolean());
      method.varArgsKnown(true);
    }
    if (member.has(RET_PARAM) && !member.get(RET_PARAM).isJsonNull()) {
      method.returnTypeParameter(member.get(RET_PARAM).getAsInt());
    }
    if (member.get("retType") instanceof JsonObject retType) {
      applyDeclaredReturn(method, retType);
    }
    method.returnsSelf(member.has("retSelf") && member.get("retSelf").getAsBoolean());
    registerAnnotationDefault(id, member);
    sema.symbols.put(id, method);
    return method;
  }

  private void applyDeclaredReturn(JMethodSymbol method, JsonObject retType) {
    method.declaredReturn(retType);
    // a parameterized return keeps its arguments, so the member's return type is the one a call reports
    if (!method.isStatic() && retType.has("args")) {
      JType plain = sema.factTypes().typeFromFact(retType, Map.of(), true);
      if (!plain.isUnknown() && plain.isParameterized()
        && plain.args.stream().noneMatch(argument -> argument.isCapture() || argument.isWildcard())) {
        method.returnTypeSymbol(plain.symbol());
      }
    }
  }

  private void registerAnnotationDefault(String id, JsonObject member) {
    if (member.get("default") instanceof JsonObject defaultValue) {
      Object value = sema.annotationFacts().readAnnotationElement(defaultValue);
      if (value != null) {
        sema.declaredMembers.annotationDefaults.put(id, value);
      }
    }
  }

  void createDumpSymbol(String id) {
    if (sema.symbols.containsKey(id)) {
      return;
    }
    org.sonarsource.semsitter.wire.Symbol dump = sema.dumpSymbols.get(id);
    String kind = dump.getKind();
    String name = dump.getName();
    if ("label".equals(kind)) {
      return;
    }
    JSymbol symbol;
    if ("type".equals(kind) && id.indexOf('<') >= 0) {
      symbol = new JTypeSymbol(sema, id, name, true);
    } else if ("type".equals(kind)) {
      symbol = new JTypeSymbol(sema, id, simpleName(erase(id)), false);
      sema.typesByName.putIfAbsent(erase(id), (JTypeSymbol) symbol);
    } else if ("method".equals(kind) || "constructor".equals(kind)) {
      symbol = new JMethodSymbol(sema, id, "constructor".equals(kind) ? "<init>" : name);
    } else {
      symbol = new JVariableSymbol(sema, id, name, kind);
    }
    sema.symbols.put(id, symbol);
  }

  void linkDumpSymbol(String id) {
    JSymbol symbol = sema.symbols.get(id);
    org.sonarsource.semsitter.wire.Symbol dump = sema.dumpSymbols.get(id);
    if (symbol == null) {
      return;
    }
    Fact fact = sema.facts.get(id);
    String ownerId = linkedOwnerId(symbol, dump, fact);
    linkOwner(symbol, id, ownerId);
    if (fact != null) {
      sema.memberFactory().applyFact(symbol, fact);
    }
    if ((symbol.isMethodSymbol() || symbol.isVariableSymbol()) && (fact == null || !"IN_PROJECT".equals(fact.getTier()))) {
      applyLibraryMember(symbol, id);
    }
    applyDeclaredType(symbol, dump, fact);
    if (symbol instanceof JMethodSymbol method) {
      sema.memberFactory().fillMethod(method, fact, id);
      implicitEnumMethod(method, id, ownerId, dump);
    } else if (symbol instanceof JTypeSymbol typeSymbol && typeSymbol.isTypeParameter()) {
      symbol.type(JType.typeVar(sema, symbol.name(), id));
    } else if (symbol.isTypeSymbol()) {
      symbol.type(sema.rawNamed(erase(id)));
    }
  }

  @Nullable
  private String linkedOwnerId(JSymbol symbol, org.sonarsource.semsitter.wire.Symbol dump, @Nullable Fact fact) {
    String ownerId = text(dump.getOwner());
    String parent = fact == null ? null : text(fact.getParent());
    boolean methodWithoutOwner = ownerId == null && symbol instanceof JMethodSymbol;
    boolean ownerOutsideDump = ownerId != null && parent != null && !parent.contains("::") && !sema.facts.containsKey(ownerId) && !sema.dumpSymbols.containsKey(ownerId);
    return (methodWithoutOwner || ownerOutsideDump) ? parent : ownerId;
  }

  private void linkOwner(JSymbol symbol, String id, @Nullable String ownerId) {
    if (ownerId != null) {
      symbol.owner(sema.resolve(ownerId));
    } else if (symbol instanceof JTypeSymbol typeSymbol && !typeSymbol.isTypeParameter()) {
      symbol.owner(sema.packageSymbol(packageName(erase(id))));
    }
  }

  private void applyDeclaredType(JSymbol symbol, org.sonarsource.semsitter.wire.Symbol dump, @Nullable Fact fact) {
    TypeRef declaredType = typeFact(dump);
    if (symbol.isVariableSymbol() && declaredType != null) {
      symbol.type(sema.factTypes().typeFromRef(declaredType));
    } else if (symbol.isVariableSymbol() && fact != null && symbol.type().isUnknown()) {
      // A use before the declaration is translated still sees a primitive type.
      List<String> declared = fact.getTypeList();
      if (declared.size() == 1 && JSema.PRIMITIVES.contains(declared.get(0))) {
        symbol.type(sema.primitiveType(declared.get(0)));
      }
    }
  }

  /** The constant the served type information gives a field of another source file, if any. */
  @Nullable
  Object servedConstant(JVariableSymbol field) {
    String id = field.id;
    int hash = id == null ? -1 : id.indexOf('#');
    if (hash < 0 || !sema.declaresType(erase(id.substring(0, hash)))) {
      return null;
    }
    JsonArray members = sema.typeInfos.get(sema.sourceName(erase(id.substring(0, hash)))).getAsJsonArray(JSema.MEMBERS);
    if (members != null) {
      for (JsonElement element : members) {
        JsonObject member = element.getAsJsonObject();
        String constant = text(member, CONSTANT);
        if (constant != null && id.equals(text(member, "id"))) {
          return constantOf(field.type(), constant);
        }
      }
    }
    return null;
  }

  /** The implicit valueOf and values of an enum come without a return type fact: derive it from the owner. */
  private void implicitEnumMethod(JMethodSymbol method, String id, @Nullable String ownerId, org.sonarsource.semsitter.wire.Symbol dump) {
    FactValue implicit = dump.getFactsMap().get("implicit");
    if (implicit == null || implicit.getValueCase() == FactValue.ValueCase.VALUE_NOT_SET || ownerId == null) {
      return;
    }
    MemberId parsed = parseMember(id);
    boolean valueOf = "valueOf".equals(parsed.name()) && "java.lang.String".equals(parsed.params());
    boolean values = "values".equals(parsed.name()) && parsed.params() != null && parsed.params().isEmpty();
    if (valueOf || values) {
      Type enumType = sema.factTypes().typeFromName(erase(ownerId));
      method.returnTypeSymbol((valueOf ? enumType : JType.array(sema, (JType) enumType)).symbol());
      method.addModifiers(List.of("public", "static"));
    }
  }

  /** Flags, annotations and thrown types the {@code types} answer gives a library member the dump names. */
  private void applyLibraryMember(JSymbol symbol, String id) {
    MemberId parsed = parseMember(id);
    if (parsed == null || parsed.arityOnly() || JSema.ARRAY.equals(parsed.owner())) {
      return;
    }
    String ownerName = erase(parsed.owner());
    sema.typeSymbol(ownerName);
    JsonObject member = sema.lookup().findMember(ownerName, parsed, new HashSet<>(), MemberLookup.Match.TYPED);
    if (member == null || member.has("id")) {
      return;
    }
    applyMemberFlags(symbol, member);
    sema.annotationFacts().libraryAnnotations(symbol, id, member);
    List<String> thrown = strings(member, THROWS);
    if (symbol instanceof JMethodSymbol method && !thrown.isEmpty()) {
      method.thrownTypes(thrown.stream().map(sema.factTypes()::typeFromName).toList());
    }
    if (symbol instanceof JMethodSymbol method && member.get(VARARGS) instanceof JsonPrimitive varArgs && varArgs.isBoolean()) {
      method.varArgs(varArgs.getAsBoolean());
      method.varArgsKnown(true);
    }
  }
}
