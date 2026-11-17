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

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.ClassTree;

final class JTypeSymbol extends JSymbol implements Symbol.TypeSymbol {

  private static final String ENUM = "java.lang.Enum";

  private final boolean typeParameter;
  private int typeParameterCount;
  private JType declaringType;
  @Nullable
  private String superName;
  private List<String> interfaceNames = List.of();
  @Nullable
  private JsonObject superTypeFact;
  private List<JsonObject> interfaceTypeFacts = List.of();
  private List<JsonObject> memberFacts = List.of();
  @Nullable
  private String elementFqn;
  private Collection<Symbol> members;
  @Nullable
  private Type declaredSuperClass;
  private List<Type> declaredInterfaces = List.of();
  @Nullable
  private JTypeSymbol generic;

  JTypeSymbol(JSema sema, @Nullable String id, String name, boolean typeParameter) {
    super(sema, Kind.TYPE, id, name);
    this.typeParameter = typeParameter;
  }

  /**
   * A view of this generic type symbol whose {@link #type()} is the given parameterization,
   * so that {@code type.symbol().type()} keeps the type arguments.
   */
  JTypeSymbol parameterizedAs(JType parameterized) {
    JTypeSymbol view = new JTypeSymbol(sema, id, name(), typeParameter);
    view.copyStateFrom(this);
    view.type(parameterized);
    view.typeParameterCount = typeParameterCount;
    view.superName = superName;
    view.interfaceNames = interfaceNames;
    view.superTypeFact = superTypeFact;
    view.interfaceTypeFacts = interfaceTypeFacts;
    view.memberFacts = memberFacts;
    view.elementFqn = elementFqn;
    view.declaredSuperClass = declaredSuperClass;
    view.declaredInterfaces = declaredInterfaces;
    view.generic = this;
    return view;
  }

  void declaredSupertypes(@Nullable Type superClass, List<Type> interfaces) {
    this.declaredSuperClass = superClass;
    this.declaredInterfaces = List.copyOf(interfaces);
  }

  boolean isTypeParameter() {
    return typeParameter;
  }

  private List<TypeTree> boundTrees = List.of();

  void boundTrees(List<TypeTree> bounds) {
    this.boundTrees = List.copyOf(bounds);
  }

  /** The first declared bound of a type parameter, or {@code null} when it has none. */
  @Nullable
  Type firstBound() {
    return boundTrees.isEmpty() ? null : boundTrees.get(0).symbolType();
  }

  /** Every declared bound of a type parameter. */
  List<Type> bounds() {
    return boundTrees.stream().map(TypeTree::symbolType).toList();
  }

  /** The generic declaration of a raw use of this type: its type parameters as arguments, one instance per symbol. */
  JType declaringType(JType raw) {
    if (declaringType == null) {
      List<JType> variables = new ArrayList<>();
      for (int i = 0; i < typeParameterCount; i++) {
        variables.add(JType.typeVar(sema, "T" + i, raw.fqn + "<" + i + ">"));
      }
      declaringType = JType.named(sema, raw.fqn, variables);
    }
    return declaringType;
  }

  int typeParameterCount() {
    return typeParameterCount;
  }

  void typeParameterCount(int typeParameterCount) {
    this.typeParameterCount = typeParameterCount;
  }

  void superName(@Nullable String superName) {
    this.superName = superName;
  }

  void interfaceNames(List<String> interfaceNames) {
    this.interfaceNames = interfaceNames;
  }

  /** Supertypes with their type arguments, used when they name the same classes as the erased ones. */
  void genericSupertypes(@Nullable JsonObject superType, List<JsonObject> interfaceTypes) {
    this.superTypeFact = superType;
    this.interfaceTypeFacts = interfaceTypes;
  }

  void memberFacts(List<JsonObject> memberFacts) {
    this.memberFacts = memberFacts;
    this.members = null;
  }

  List<JsonObject> memberFacts() {
    return memberFacts;
  }

  void elementFqn(@Nullable String elementFqn) {
    this.elementFqn = elementFqn;
  }

  @CheckForNull
  @Override
  public Type superClass() {
    if (typeParameter || elementFqn != null) {
      return sema.getClassType("java.lang.Object");
    }
    if (isInterface() || isAnnotation()) {
      return sema.getClassType("java.lang.Object");
    }
    String erased = JSema.erase(id == null ? name() : id);
    if ("java.lang.Object".equals(erased)) {
      return null;
    }
    boolean noSuper = superName == null || superName.isEmpty();
    if (isEnum() && superTypeFact == null && (noSuper || ENUM.equals(JSema.erase(superName))) && type() instanceof JType self) {
      sema.getClassType(ENUM);
      return JType.named(sema, ENUM, List.of(self));
    }
    if (noSuper || inheritsFrom(erased) || interfaceDeclaredAsSuper() != null) {
      return sema.getClassType("java.lang.Object");
    }
    if (namesSameClass(superTypeFact, superName)) {
      return sema.supertypeFromFact(superTypeFact);
    }
    return withDeclaredArguments(sema.getClassType(JSema.erase(superName)), declaredSuperClass == null ? List.of() : List.of(declaredSuperClass));
  }

  private static Type withDeclaredArguments(Type erased, List<Type> declared) {
    return declared.stream().filter(type -> type.isParameterized() && type.erasure().equals(erased)).findFirst().orElse(erased);
  }

  private boolean inheritsFrom(String erasedSelf) {
    Set<String> visited = new HashSet<>();
    String current = JSema.erase(superName);
    while (visited.add(current)) {
      if (current.equals(erasedSelf)) {
        return true;
      }
      if (!(sema.getClassType(current).symbol() instanceof JTypeSymbol parent) || parent.superName == null || parent.superName.isEmpty()) {
        return false;
      }
      current = JSema.erase(parent.superName);
    }
    return false;
  }

  @Override
  public List<Type> interfaces() {
    List<Type> declared = ownInterfaces();
    if (generic == null || type() == null || !type().isParameterized()) {
      return declared;
    }
    return declared.stream().map(iface -> sema.instantiateSupertype(type(), iface)).toList();
  }

  private List<Type> ownInterfaces() {
    List<Type> resolved = resolvedInterfaces();
    List<Type> unresolved = declaredInterfaces.stream()
      .filter(Type::isUnknown)
      .filter(distinctByName(new HashSet<>()))
      .filter(declared -> resolved.stream().noneMatch(known -> known.fullyQualifiedName().equals(declared.fullyQualifiedName())))
      .toList();
    if (unresolved.isEmpty()) {
      return resolved;
    }
    List<Type> all = new ArrayList<>(resolved);
    all.addAll(unresolved);
    return all;
  }

  private static java.util.function.Predicate<Type> distinctByName(Set<String> seen) {
    return type -> seen.add(type.name());
  }

  private List<Type> resolvedInterfaces() {
    if (elementFqn != null) {
      return List.of(sema.getClassType("java.lang.Cloneable"), sema.getClassType("java.io.Serializable"));
    }
    if (interfaceNames.isEmpty()) {
      return withInterfaceSuper(isAnnotation() ? List.of(sema.getClassType("java.lang.annotation.Annotation")) : List.of());
    }
    boolean generic = interfaceTypeFacts.size() == interfaceNames.size();
    List<Type> types = new ArrayList<>();
    for (int i = 0; i < interfaceNames.size(); i++) {
      String name = interfaceNames.get(i);
      JsonObject fact = generic ? interfaceTypeFacts.get(i) : null;
      Type erased = sema.getClassType(JSema.erase(name));
      types.add(namesSameClass(fact, name) ? sema.supertypeFromFact(fact) : withDeclaredArguments(erased, declaredInterfaces));
    }
    return withInterfaceSuper(types);
  }

  /**
   * An anonymous class whose named supertype is an interface is recorded with that interface as its superclass.
   * Java implements the interface and extends Object.
   */
  @Nullable
  private Type interfaceDeclaredAsSuper() {
    if (superName == null || superName.isEmpty() || isInterface() || isAnnotation() || typeParameter || elementFqn != null) {
      return null;
    }
    String erased = JSema.erase(superName);
    JTypeSymbol parent = sema.typeSymbol(erased);
    if (!parent.isInterface() || parent.isUnknown()) {
      return null;
    }
    return sema.getClassType(erased);
  }

  private List<Type> withInterfaceSuper(List<Type> interfaces) {
    Type superInterface = interfaceDeclaredAsSuper();
    if (superInterface == null || interfaces.stream().anyMatch(type -> type.erasure().equals(superInterface.erasure()))) {
      return interfaces;
    }
    List<Type> combined = new ArrayList<>(interfaces.size() + 1);
    combined.add(superInterface);
    combined.addAll(interfaces);
    return combined;
  }

  private static boolean namesSameClass(@Nullable JsonObject fact, String name) {
    return fact != null && fact.has("id") && fact.get("id").isJsonPrimitive()
      && JSema.erase(fact.get("id").getAsString()).equals(JSema.erase(name));
  }

  @Override
  public Collection<Symbol> memberSymbols() {
    if (generic != null) {
      return generic.memberSymbols();
    }
    if (members == null) {
      List<Symbol> resolved = new ArrayList<>();
      for (JsonObject member : memberFacts) {
        if (!isTypeParameterFact(member)) {
          resolved.add(sema.memberFromTypeInfo(this, member));
        }
      }
      addImplicitConstructor(resolved);
      addImplicitEnumMethods(resolved);
      addDeclaredMembers(resolved);
      members = resolved;
    }
    return members;
  }

  /** A local type has no member facts besides its constructor: its members are those its declaration writes. */
  private void addDeclaredMembers(List<Symbol> resolved) {
    if (!(declaration() instanceof org.sonar.plugins.java.api.tree.ClassTree tree) || resolved.stream().anyMatch(member -> !member.name().endsWith("<init>"))) {
      return;
    }
    tree.recordComponents().forEach(component -> resolved.add(component.symbol()));
    for (org.sonar.plugins.java.api.tree.Tree member : tree.members()) {
      if (member instanceof org.sonar.plugins.java.api.tree.VariableTree variable) {
        resolved.add(variable.symbol());
      } else if (member instanceof org.sonar.plugins.java.api.tree.MethodTree method && !method.is(org.sonar.plugins.java.api.tree.Tree.Kind.CONSTRUCTOR)) {
        resolved.add(method.symbol());
      }
    }
  }

  /** A type parameter is listed among the members of a project type but is not a member symbol. */
  private static boolean isTypeParameterFact(JsonObject member) {
    return member.get("facts") instanceof JsonObject facts && facts.get("kind") instanceof JsonPrimitive kind && "typeparameter".equals(kind.getAsString());
  }

  /** An enum declared in this file has the static methods {@code values()} and {@code valueOf(String)}. */
  private void addImplicitEnumMethods(List<Symbol> resolved) {
    if (id == null || isUnknown() || !isEnum() || declaration() == null || !(type() instanceof JType declared)) {
      return;
    }
    if (resolved.stream().noneMatch(member -> member.isMethodSymbol() && "values".equals(member.name()))) {
      JMethodSymbol values = implicitEnumMethod("values", List.of(), JType.array(sema, declared));
      resolved.add(values);
    }
    if (resolved.stream().noneMatch(member -> member.isMethodSymbol() && "valueOf".equals(member.name()))) {
      resolved.add(implicitEnumMethod("valueOf", List.of(sema.getClassType("java.lang.String")), declared));
    }
  }

  private JMethodSymbol implicitEnumMethod(String name, List<Type> parameters, Type returned) {
    JMethodSymbol method = new JMethodSymbol(sema, id + "#" + name + "(" + (parameters.isEmpty() ? "" : "java.lang.String") + ")", name);
    method.owner(this);
    method.addModifiers(List.of("public", "static"));
    method.parameterTypes(parameters);
    method.returnTypeSymbol(returned.symbol());
    return method;
  }

  /** A class of this file that declares no constructor still has the default one as a member. */
  private void addImplicitConstructor(List<Symbol> resolved) {
    if (id == null || isUnknown() || isTypeParameter() || isInterface() || declaration() == null
      || resolved.stream().anyMatch(member -> member.isMethodSymbol() && "<init>".equals(member.name()))) {
      return;
    }
    Symbol constructor = sema.resolve(id + "#<init>()");
    if (!constructor.isUnknown()) {
      resolved.add(constructor);
    }
  }

  @Override
  public Collection<Symbol> lookupSymbols(String name) {
    return new HashSet<>(membersNamed(name));
  }

  /**
   * The members named {@code name}, in the order of {@link #memberSymbols()}. Of a type another file or a library declares,
   * only the members that may have that name are created, so a type with thousands of members costs a few per lookup.
   */
  List<Symbol> membersNamed(String name) {
    if (generic != null) {
      return generic.membersNamed(name);
    }
    List<Symbol> found = new ArrayList<>();
    if (members != null || id == null || sema.inDocument(id) || declaration() != null) {
      // The implicit members of a type this file declares depend on all of its members.
      for (Symbol member : memberSymbols()) {
        if (name.equals(member.name())) {
          found.add(member);
        }
      }
      return found;
    }
    for (JsonObject member : memberFacts) {
      if (!isTypeParameterFact(member) && mayBeNamed(member, name)) {
        Symbol symbol = sema.memberFromTypeInfo(this, member);
        if (name.equals(symbol.name())) {
          found.add(symbol);
        }
      }
    }
    return found;
  }

  /** False only when the fact surely names another member: by its written name, or by the name its id gives. */
  private boolean mayBeNamed(JsonObject member, String name) {
    if (member.get("id") instanceof JsonPrimitive memberId) {
      String simple = JSema.memberName(memberId.getAsString());
      return simple == null || simple.equals(name) || !isIdentifier(simple);
    }
    if (member.has("id")) {
      return true;
    }
    return !(member.get("name") instanceof JsonPrimitive written) || name.equals(written.getAsString());
  }

  private static boolean isIdentifier(String text) {
    if (text.isEmpty() || !Character.isJavaIdentifierStart(text.charAt(0))) {
      return false;
    }
    for (int i = 1; i < text.length(); i++) {
      if (!Character.isJavaIdentifierPart(text.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  @Nullable
  @Override
  public ClassTree declaration() {
    return super.declaration() instanceof ClassTree classTree ? classTree : null;
  }

  @Override
  public Set<Type> superTypes() {
    if (isUnknown()) {
      return Collections.emptySet();
    }
    Set<Type> collected = new HashSet<>();
    collectSuperTypes(collected, new HashSet<>(), type());
    return collected;
  }

  /** Collects the supertypes, each seen with the type arguments {@code from} gives the type declaring it. */
  private void collectSuperTypes(Set<Type> collected, Set<String> visited, Type from) {
    if (!visited.add(JSema.erase(id == null ? name() : id))) {
      return;
    }
    Type superClass = superClass();
    if (superClass != null && superClass.isUnknown() && JUtils.isRecovered(superClass)) {
      collected.add(superClass);
    } else if (superClass != null && !superClass.isUnknown()) {
      Type instantiated = sema.instantiateSupertype(from, superClass);
      collected.add(instantiated);
      if (superClass.symbol() instanceof JTypeSymbol parent) {
        parent.collectSuperTypes(collected, visited, instantiated);
      }
    }
    for (Type iface : interfaces()) {
      if (iface.isUnknown() && JUtils.isRecovered(iface)) {
        collected.add(iface);
      } else if (!iface.isUnknown()) {
        Type instantiated = sema.instantiateSupertype(from, iface);
        collected.add(instantiated);
        if (iface.symbol() instanceof JTypeSymbol parent) {
          parent.collectSuperTypes(collected, visited, instantiated);
        }
      }
    }
  }

  @Nullable
  @Override
  public TypeSymbol outermostClass() {
    Symbol current = this;
    Symbol result = this;
    while (current != null && !current.isPackageSymbol()) {
      result = current;
      current = current.owner();
    }
    return result instanceof TypeSymbol typeSymbol ? typeSymbol : this;
  }

  @Override
  public boolean isAnnotation() {
    return !isUnknown() && isAnnotationSymbol();
  }
}
