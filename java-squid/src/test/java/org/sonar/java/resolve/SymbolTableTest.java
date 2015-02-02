/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.resolve;

import com.google.common.collect.Iterables;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.failure;

public class SymbolTableTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  @Test
  public void Generics() {
    Result result = Result.createFor("Generics");
    Symbol.TypeSymbol typeSymbol = (Symbol.TypeSymbol) result.symbol("A");
    Symbol symbolA1 = result.symbol("a1");
    assertThat(symbolA1.type.symbol).isSameAs(typeSymbol);
    Symbol symbolA2 = result.symbol("a1");
    assertThat(symbolA2.type.symbol).isSameAs(typeSymbol);
    assertThat(symbolA2.type).isSameAs(symbolA1.type);
    Symbol symbolA3 = result.symbol("a1");
    assertThat(symbolA3.type.symbol).isSameAs(typeSymbol);
    assertThat(result.reference(12, 5)).isSameAs(result.symbol("foo", 8));
    assertThat(result.reference(13, 5)).isSameAs(result.symbol("foo", 9));

    //Check erasure
    Type.TypeVariableType STypeVariableType = (Type.TypeVariableType) typeSymbol.typeParameters.lookup("S").get(0).type;
    assertThat(STypeVariableType.erasure().getSymbol().getName()).isEqualTo("CharSequence");
    Type arrayErasure = typeSymbol.members().lookup("arrayErasure").get(0).type;
    assertThat(arrayErasure.isTagged(Type.ARRAY));
    assertThat(arrayErasure.erasure().isTagged(Type.ARRAY));
    assertThat(((Type.ArrayType)arrayErasure.erasure()).elementType().symbol.getName()).isEqualTo("CharSequence");

    IdentifierTree tree = result.referenceTree(20, 7);
    Type symbolType = ((AbstractTypedTree) tree).getSymbolType();
    assertThat(symbolType).isInstanceOf(Type.ParametrizedTypeType.class);
    Type.ParametrizedTypeType ptt = (Type.ParametrizedTypeType) symbolType;
    assertThat(ptt.symbol.getName()).isEqualTo("C");
    assertThat(ptt.typeSubstitution).hasSize(1);
    assertThat(ptt.typeSubstitution.get(ptt.typeSubstitution.keySet().iterator().next()).symbol.getName()).isEqualTo("String");

    Symbol.MethodSymbol method1 = (Symbol.MethodSymbol) typeSymbol.members().lookup("method1").get(0);
    assertThat(((Type.MethodType)method1.type).resultType).isSameAs(STypeVariableType);

    Symbol.MethodSymbol method2 = (Symbol.MethodSymbol) typeSymbol.members().lookup("method2").get(0);
    Type.TypeVariableType PTypeVariableType = (Type.TypeVariableType) method2.typeParameters().lookup("P").get(0).type;
    assertThat(method2.getReturnType().type).isSameAs(PTypeVariableType);
    assertThat(method2.getParametersTypes().get(0)).isSameAs(PTypeVariableType);

    //Type parameter defined in outer class
    Symbol.TypeSymbol classCSymbol = (Symbol.TypeSymbol) typeSymbol.members().lookup("C").get(0);
    Symbol innerClassField = classCSymbol.members().lookup("innerClassField").get(0);
    assertThat(innerClassField.type).isSameAs(STypeVariableType);

  }

  @Test
  public void parameterized_method_type() throws Exception {
    Result result = Result.createFor("Generics");
    MethodTree method3 = (MethodTree) result.getTree(result.symbol("method3"));
    VariableTree variable = (VariableTree) method3.block().body().get(0);
    assertThat(((AbstractTypedTree)variable.initializer()).getSymbolType().getSymbol().getName()).isEqualTo("String");

    MethodTree method4 = (MethodTree) result.getTree(result.symbol("method4"));
    variable = (VariableTree) method4.block().body().get(0);
    Type symbolType = ((AbstractTypedTree) variable.initializer()).getSymbolType();
    assertThat(symbolType).isInstanceOf(Type.ParametrizedTypeType.class);
    Type.ParametrizedTypeType ptt = (Type.ParametrizedTypeType) symbolType;
    assertThat(ptt.typeSubstitution.values().iterator().next().getSymbol().getName()).isEqualTo("String");

    assertThat(result.reference(58, 25)).isSameAs(result.symbol("method_of_e"));
  }

  @Test
  public void recursive_type_substitution() {
    Result result = Result.createFor("Generics");
    MethodTree ddt_method = (MethodTree) result.getTree(result.symbol("ddt_method"));
    VariableTree variable = (VariableTree) ddt_method.block().body().get(0);
    Type symbolType = ((AbstractTypedTree) variable.initializer()).getSymbolType();
    assertThat(symbolType.getSymbol().getName()).isEqualTo("String");
  }

  @Test
  public void ClassDeclaration() {
    Result result = Result.createFor("declarations/ClassDeclaration");
    Symbol.TypeSymbol typeSymbol = (Symbol.TypeSymbol) result.symbol("Declaration");
    Symbol classDeclaration = result.symbol("ClassDeclaration");
    assertThat(classDeclaration.isParametrized).isTrue();
    List<Symbol> parameters = classDeclaration.type.symbol.typeParameters.lookup("T");
    assertThat(parameters).hasSize(1);
    assertThat(parameters.get(0).getName()).isEqualTo("T");
    parameters = classDeclaration.type.symbol.typeParameters.lookup("S");
    assertThat(parameters).hasSize(1);
    assertThat(parameters.get(0).getName()).isEqualTo("S");
    assertThat(typeSymbol.owner()).isSameAs(classDeclaration);
    assertThat(typeSymbol.flags()).isEqualTo(Flags.PRIVATE);
    assertThat(typeSymbol.getSuperclass()).isSameAs(result.symbol("Superclass").type);
    assertThat(typeSymbol.getInterfaces()).containsExactly(
        result.symbol("FirstInterface").type,
        result.symbol("SecondInterface").type);
    assertThat(typeSymbol.members.lookup("this")).isNotEmpty();
    assertThat(typeSymbol.members.lookup("super")).hasSize(1);
    Symbol superSymbol = typeSymbol.members.lookup("super").get(0);

    typeSymbol = (Symbol.TypeSymbol) result.symbol("Superclass");
    assertThat(superSymbol.type.symbol).isSameAs(typeSymbol);

    Symbol superclass = typeSymbol.getSuperclass().symbol;
    assertThat(superclass.getName()).isEqualTo("Object");
    assertThat(superclass.owner).isInstanceOf(Symbol.PackageSymbol.class);
    assertThat(superclass.owner.getName()).isEqualTo("java.lang");

    assertThat(typeSymbol.getInterfaces()).isEmpty();

    typeSymbol = (Symbol.TypeSymbol) result.symbol("Foo");
    assertThat(typeSymbol.getSuperclass()).isSameAs(result.symbol("Baz").type);

    assertThat(result.reference(25,21)).isSameAs(result.symbol("method"));

    SymbolMetadata metadata = classDeclaration.metadata();
    assertThat(metadata.annotations()).hasSize(1);
    assertThat(metadata.getValuesFor("java.lang.Override")).isNull();
    assertThat(metadata.getValuesFor("java.lang.SuppressWarnings")).hasSize(1);
  }

  @Test
  public void DirectCyclingClassDeclaration() {
    expectedEx.expectMessage("Cycling class hierarchy detected with symbol : Foo");
    expectedEx.expect(IllegalStateException.class);
    Result.createForJavaFile("src/test/filesInError/DirectCyclingClassDeclaration");
  }

  @Test
  public void CyclingClassDeclaration() {
    expectedEx.expect(IllegalStateException.class);
    expectedEx.expectMessage("Cycling class hierarchy detected with symbol : Qix");
    Result.createForJavaFile("src/test/filesInError/CyclingClassDeclaration");
  }

  @Test
  public void AnonymousClassDeclaration() {
    Result result = Result.createFor("declarations/AnonymousClassDeclaration");

    Symbol.TypeSymbol typeSymbol = (Symbol.TypeSymbol) result.symbol("methodInAnonymousClass").owner();
    assertThat(typeSymbol.owner()).isSameAs(result.symbol("method"));
    assertThat(typeSymbol.flags()).isEqualTo(0);
    assertThat(typeSymbol.name).isEqualTo("");
    assertThat(typeSymbol.getSuperclass()).isNull(); // FIXME should be result.symbol("Superclass")
    assertThat(typeSymbol.getInterfaces()).isEmpty();
    assertThat(typeSymbol.members.lookup("this")).isNotEmpty();
  }

  @Test
  public void LocalClassDeclaration() {
    Result result = Result.createFor("declarations/LocalClassDeclaration");

    Symbol.TypeSymbol typeSymbol;
    // TODO no forward references here, for the moment considered as a really rare situation
    // typeSymbol = (Symbol.TypeSymbol) result.symbol("Declaration", 14);
    // assertThat(typeSymbol.getSuperclass()).isSameAs(result.symbol("Superclass", 9));

    typeSymbol = (Symbol.TypeSymbol) result.symbol("Declaration", 22);
    assertThat(typeSymbol.getSuperclass()).isSameAs(result.symbol("Superclass", 22 - 2).type);
    assertThat(typeSymbol.members.lookup("this")).isNotEmpty();

    typeSymbol = (Symbol.TypeSymbol) result.symbol("Declaration", 25);
    assertThat(typeSymbol.getSuperclass()).isSameAs(result.symbol("Superclass", 9).type);
  }

  @Test
  public void InterfaceDeclaration() {
    Result result = Result.createFor("declarations/InterfaceDeclaration");

    Symbol.TypeSymbol interfaceSymbol = (Symbol.TypeSymbol) result.symbol("Declaration");
    assertThat(interfaceSymbol.owner()).isSameAs(result.symbol("InterfaceDeclaration"));
    assertThat(interfaceSymbol.flags()).isEqualTo(Flags.PRIVATE | Flags.INTERFACE);
    assertThat(interfaceSymbol.getSuperclass()).isNull(); // TODO should it be java.lang.Object?
    assertThat(interfaceSymbol.getInterfaces()).containsExactly(
        result.symbol("FirstInterface").type,
        result.symbol("SecondInterface").type);
    assertThat(interfaceSymbol.members.lookup("this")).isEmpty();

    Symbol.VariableSymbol variableSymbol = (Symbol.VariableSymbol) result.symbol("FIRST_CONSTANT");
    assertThat(variableSymbol.owner()).isSameAs(interfaceSymbol);
    assertThat(variableSymbol.flags()).isEqualTo(Flags.PUBLIC);

    variableSymbol = (Symbol.VariableSymbol) result.symbol("SECOND_CONSTANT");
    assertThat(variableSymbol.owner()).isSameAs(interfaceSymbol);
    assertThat(variableSymbol.flags()).isEqualTo(Flags.PUBLIC);

    Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) result.symbol("method");
    assertThat(methodSymbol.owner()).isSameAs(interfaceSymbol);
    assertThat(methodSymbol.flags()).isEqualTo(Flags.PUBLIC);

    Symbol.TypeSymbol typeSymbol = (Symbol.TypeSymbol) result.symbol("NestedClass");
    assertThat(typeSymbol.owner()).isSameAs(interfaceSymbol);
    assertThat(typeSymbol.flags()).isEqualTo(Flags.PUBLIC);

    typeSymbol = (Symbol.TypeSymbol) result.symbol("NestedInterface");
    assertThat(typeSymbol.owner()).isSameAs(interfaceSymbol);
    assertThat(typeSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.INTERFACE);

    typeSymbol = (Symbol.TypeSymbol) result.symbol("NestedEnum");
    assertThat(typeSymbol.owner()).isSameAs(interfaceSymbol);
    assertThat(typeSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.ENUM);
  }

  @Test
  public void EnumDeclaration() {
    Result result = Result.createFor("declarations/EnumDeclaration");

    Symbol.TypeSymbol enumSymbol = (Symbol.TypeSymbol) result.symbol("Declaration");
    assertThat(enumSymbol.owner()).isSameAs(result.symbol("EnumDeclaration"));
    assertThat(enumSymbol.flags()).isEqualTo(Flags.PRIVATE | Flags.ENUM);

    Symbol superclass = enumSymbol.getSuperclass().symbol;
    assertThat(superclass.getName()).isEqualTo("Enum");
    assertThat(superclass.owner).isInstanceOf(Symbol.PackageSymbol.class);
    assertThat(superclass.owner.getName()).isEqualTo("java.lang");

    assertThat(enumSymbol.getInterfaces()).containsExactly(
        result.symbol("FirstInterface").type,
        result.symbol("SecondInterface").type);
    assertThat(enumSymbol.members.lookup("this")).isNotEmpty();

    Symbol.VariableSymbol variableSymbol = (Symbol.VariableSymbol) result.symbol("FIRST_CONSTANT");
    assertThat(variableSymbol.owner()).isSameAs(enumSymbol);
    assertThat(variableSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.ENUM);

    Symbol.TypeSymbol anonymousSymbol = (Symbol.TypeSymbol) result.symbol("method", 11).owner();
    assertThat(anonymousSymbol.name).isEqualTo("");
    assertThat(anonymousSymbol.owner()).isSameAs(enumSymbol);
    assertThat(anonymousSymbol.flags()).isEqualTo(0); // FIXME should be ENUM
    assertThat(anonymousSymbol.getSuperclass()).isNull(); // FIXME should be result.symbol("EnumDeclaration")
    assertThat(anonymousSymbol.getInterfaces()).isEmpty();

    variableSymbol = (Symbol.VariableSymbol) result.symbol("SECOND_CONSTANT");
    assertThat(variableSymbol.owner()).isSameAs(enumSymbol);
    assertThat(variableSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.ENUM);

    anonymousSymbol = (Symbol.TypeSymbol) result.symbol("method", 16).owner();
    assertThat(anonymousSymbol.name).isEqualTo("");
    assertThat(anonymousSymbol.owner()).isSameAs(enumSymbol);
    assertThat(anonymousSymbol.flags()).isEqualTo(0); // FIXME should be ENUM
    assertThat(anonymousSymbol.getSuperclass()).isNull(); // FIXME should be result.symbol("EnumDeclaration")
    assertThat(anonymousSymbol.getInterfaces()).isEmpty();

    Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) result.symbol("method", 21);
    assertThat(methodSymbol.owner()).isSameAs(enumSymbol);
    assertThat(methodSymbol.flags()).isEqualTo(Flags.ABSTRACT);
    Symbol.TypeSymbol enumConstructorSymbol = (Symbol.TypeSymbol) result.symbol("ConstructorEnum");
    assertThat(enumConstructorSymbol).isNotNull();
    methodSymbol = (Symbol.MethodSymbol) enumConstructorSymbol.members().lookup("<init>").get(0);
    assertThat(methodSymbol.isPrivate()).isTrue();

    assertThat(result.reference(36,5)).isSameAs(result.symbol("<init>", 38));
    assertThat(result.reference(37,5)).isSameAs(result.symbol("<init>", 39));
  }

  @Test
  public void AnnotationTypeDeclaration() {
    Result result = Result.createFor("declarations/AnnotationTypeDeclaration");

    Symbol.TypeSymbol annotationSymbol = (Symbol.TypeSymbol) result.symbol("Declaration");
    assertThat(annotationSymbol.owner()).isSameAs(result.symbol("AnnotationTypeDeclaration"));
    assertThat(annotationSymbol.flags()).isEqualTo(Flags.PRIVATE | Flags.INTERFACE | Flags.ANNOTATION);
    assertThat(annotationSymbol.getSuperclass()).isNull(); // TODO should it be java.lang.Object?

    Symbol superinterface = Iterables.getOnlyElement(annotationSymbol.getInterfaces()).symbol;
    assertThat(superinterface.getName()).isEqualTo("Annotation");
    assertThat(superinterface.owner).isInstanceOf(Symbol.PackageSymbol.class);
    assertThat(superinterface.owner.getName()).isEqualTo("java.lang.annotation");

    assertThat(annotationSymbol.members.lookup("this")).isEmpty();

    Symbol.VariableSymbol variableSymbol = (Symbol.VariableSymbol) result.symbol("FIRST_CONSTANT");
    assertThat(variableSymbol.owner()).isSameAs(annotationSymbol);
    assertThat(variableSymbol.flags()).isEqualTo(Flags.PUBLIC);

    variableSymbol = (Symbol.VariableSymbol) result.symbol("SECOND_CONSTANT");
    assertThat(variableSymbol.owner()).isSameAs(annotationSymbol);
    assertThat(variableSymbol.flags()).isEqualTo(Flags.PUBLIC);

    Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) result.symbol("value");
    assertThat(methodSymbol.owner()).isSameAs(annotationSymbol);
    assertThat(methodSymbol.flags()).isEqualTo(Flags.PUBLIC);

    Symbol.TypeSymbol typeSymbol = (Symbol.TypeSymbol) result.symbol("NestedClass");
    assertThat(typeSymbol.owner()).isSameAs(annotationSymbol);
    assertThat(typeSymbol.flags()).isEqualTo(Flags.PUBLIC);

    typeSymbol = (Symbol.TypeSymbol) result.symbol("NestedInterface");
    assertThat(typeSymbol.owner()).isSameAs(annotationSymbol);
    assertThat(typeSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.INTERFACE);

    typeSymbol = (Symbol.TypeSymbol) result.symbol("NestedEnum");
    assertThat(typeSymbol.owner()).isSameAs(annotationSymbol);
    assertThat(typeSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.ENUM);

    typeSymbol = (Symbol.TypeSymbol) result.symbol("NestedAnnotationType");
    assertThat(typeSymbol.owner()).isSameAs(annotationSymbol);
    assertThat(typeSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.INTERFACE | Flags.ANNOTATION);
  }

  @Test
  public void MethodDeclaration() {
    Result result = Result.createFor("declarations/MethodDeclaration");

    Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) result.symbol("declaration");
    assertThat(methodSymbol.owner()).isSameAs(result.symbol("MethodDeclaration"));
    assertThat(methodSymbol.flags()).isEqualTo(Flags.PROTECTED);
    assertThat(methodSymbol.getReturnType()).isSameAs(result.symbol("ReturnType"));
    assertThat(methodSymbol.getThrownTypes()).containsExactly(
        result.symbol("FirstExceptionType"),
        result.symbol("SecondExceptionType"));
    assertThat(methodSymbol.isParametrized).isTrue();
  }

  @Test
  public void ConstructorDeclaration() {
    Result result = Result.createFor("declarations/ConstructorDeclaration");

    Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) result.symbol("<init>");
    assertThat(methodSymbol.owner()).isSameAs(result.symbol("ConstructorDeclaration"));
    assertThat(methodSymbol.flags()).isEqualTo(0);
    assertThat(methodSymbol.getReturnType()).isNull(); // TODO should it be result.symbol("ConstructorDeclaration")?
    assertThat(methodSymbol.getParametersTypes()).hasSize(1);
    assertThat(methodSymbol.getThrownTypes()).containsExactly(
        result.symbol("FirstExceptionType"),
        result.symbol("SecondExceptionType"));

    assertThat(result.reference(21, 35)).isEqualTo(methodSymbol);
  }

  @Test
  public void CompleteHierarchyOfTypes() {
    Result result = Result.createFor("CompleteHierarchyOfTypes");

    Symbol.TypeSymbol typeSymbol = (Symbol.TypeSymbol) result.symbol("Foo");
    assertThat(typeSymbol.getSuperclass()).isSameAs(result.symbol("Baz").type);
  }

  @Test
  public void Accessibility() {
    Result result = Result.createFor("Accessibility");

    Symbol.TypeSymbol typeSymbol;
    typeSymbol = (Symbol.TypeSymbol) result.symbol("Target", 14);
    assertThat(typeSymbol.getSuperclass()).isSameAs(result.symbol("Member", 9).type);

    typeSymbol = (Symbol.TypeSymbol) result.symbol("Target", 29);
    assertThat(typeSymbol.getSuperclass()).isSameAs(result.symbol("Member", 20).type);
  }

  @Test
  public void Example() {
    Result.createFor("Example");
  }

  @Test
  public void ScopesAndSymbols() {
    Result.createFor("ScopesAndSymbols");
  }

  @Test
  public void TypesOfDeclarations() {
    Result result = Result.createFor("TypesOfDeclarations");
    assertThat(result.symbol("Class2").kind == Symbol.TYP);
    Symbol.TypeSymbol class1 = (Symbol.TypeSymbol) result.symbol("Class1");
    Symbol.TypeSymbol class2 = (Symbol.TypeSymbol) result.symbol("Class2");
    assertThat(class2.getSuperclass().symbol).isEqualTo(class1);
    assertThat(class1.getSuperclass()).isNotNull();
    assertThat(class1.getSuperclass().symbol.name).isEqualTo("Collection");
    Symbol.TypeSymbol interface1 = (Symbol.TypeSymbol) result.symbol("Interface1");
    assertThat(interface1.getInterfaces()).isNotEmpty();
    assertThat(interface1.getInterfaces().get(0).symbol.name).isEqualTo("List");
  }

  @Test
  public void Labels() {
    Result result = Result.createFor("references/Labels");

    assertThat(result.reference(8, 13)).isSameAs(result.symbol("label", 6));
    assertThat(result.reference(13, 13)).isSameAs(result.symbol("label", 11));
    assertThat(result.reference(20, 18)).isSameAs(result.symbol("label", 16));
  }

  @Test
  public void FieldAccess() {
    Result result = Result.createFor("references/FieldAccess");

    assertThat(result.reference(9, 5)).isSameAs(result.symbol("field"));

    assertThat(result.reference(10, 10)).isSameAs(result.symbol("field"));

    assertThat(result.reference(11, 5)).isSameAs(result.symbol("FieldAccess"));
    assertThat(result.reference(11, 17)).isSameAs(result.symbol("field"));

    // FIXME
    // assertThat(result.reference(12, 5)).isSameAs(/*package "references"*/);

    assertThat(result.reference(14, 5)).isSameAs(result.symbol("FirstStaticNestedClass"));
    assertThat(result.reference(14, 28)).isSameAs(result.symbol("field_in_FirstStaticNestedClass"));

    assertThat(result.reference(15, 5)).isSameAs(result.symbol("FirstStaticNestedClass"));
    assertThat(result.reference(15, 28)).isSameAs(result.symbol("SecondStaticNestedClass"));
    assertThat(result.reference(15, 52)).isSameAs(result.symbol("field_in_SecondStaticNestedClass"));

    assertThat(result.reference(16, 5)).isSameAs(result.symbol("field"));
    assertThat(result.reference(16, 11)).isSameAs(result.symbol("field_in_FirstStaticNestedClass"));

    assertThat(result.reference(17, 5)).isSameAs(result.symbol("field"));
    assertThat(result.reference(17, 11)).isSameAs(result.symbol("field_in_Superclass"));
  }

  @Test
  public void MethodParameterAccess() {
    Result result = Result.createFor("references/MethodParameterAccess");

    result.symbol("param");

    assertThat(result.reference(7, 5)).isSameAs(result.symbol("param"));

    assertThat(result.reference(8, 5)).isSameAs(result.symbol("param"));
    assertThat(result.reference(8, 11)).isSameAs(result.symbol("field"));
  }

  @Test
  public void ExpressionInAnnotation() {
    Result result = Result.createFor("references/ExpressionInAnnotation");

    assertThat(result.reference(3, 19)).isSameAs(result.symbol("ExpressionInAnnotation"));
    assertThat(result.reference(3, 42)).isSameAs(result.symbol("VALUE"));
    assertThat(result.reference(18, 6)).isSameAs(result.symbol("foo", 11));
    assertThat(result.reference(19, 6)).isSameAs(result.symbol("foo", 14));
    assertThat(result.reference(19, 14)).isSameAs(result.symbol("bar", 15));
  }

  @Test
  public void MethodCall() {
    Result result = Result.createFor("references/MethodCall");
    assertThat(result.reference(10, 5)).isSameAs(result.symbol("target"));
    assertThat(result.reference(11, 5)).isSameAs(result.symbol("foo"));
    assertThat(result.reference(30, 5)).isSameAs(result.symbol("fun", 22));
    assertThat(result.reference(42, 5)).isSameAs(result.symbol("bar", 35));
    assertThat(result.reference(52, 5)).isSameAs(result.symbol("bar", 35));
    assertThat(result.reference(61, 5)).isSameAs(result.symbol("bar", 57));
    assertThat(result.reference(67, 5)).isSameAs(result.symbol("bar", 35));
    assertThat(result.reference(79, 5)).isSameAs(result.symbol("defaultMethod", 72));
    assertThat(result.reference(88, 7)).isSameAs(result.symbol("func", 84));
    assertThat(result.reference(95, 5)).isSameAs(result.symbol("num", 94));
    assertThat(result.reference(102, 5)).isSameAs(result.symbol("varargs", 100));
    assertThat(result.reference(103, 5)).isSameAs(result.symbol("varargs", 100));
    assertThat(result.reference(104, 5)).isSameAs(result.symbol("varargs", 100));
    assertThat(result.reference(105, 5)).isSameAs(result.symbol("varargs", 100));
    assertThat(result.reference(106, 5)).isSameAs(result.symbol("varargs", 111));
    assertThat(result.reference(121, 5)).isSameAs(result.symbol("fun1", 115));
    assertThat(result.reference(122, 5)).isSameAs(result.symbol("fun2", 116));
    assertThat(result.reference(123, 5)).isSameAs(result.symbol("fun3", 117));
    assertThat(result.reference(124, 5)).isSameAs(result.symbol("fun4", 118));
    assertThat(result.reference(125, 5)).isSameAs(result.symbol("fun5", 119));
    assertThat(result.reference(132, 5)).isSameAs(result.symbol("fun", 131));
    assertThat(result.reference(134, 5)).isSameAs(result.symbol("fun", 131));

    assertThat(result.reference(143, 5)).isSameAs(result.symbol("process", 140));
    assertThat(result.reference(144, 5)).isSameAs(result.symbol("process", 141));

    assertThat(result.reference(149, 5)).isSameAs(result.symbol("process2", 147));
    assertThat(result.reference(150, 5)).isSameAs(result.symbol("process2", 146));

    assertThat(result.reference(156, 5)).isSameAs(result.symbol("process3", 153));
    assertThat(result.reference(157, 5)).isSameAs(result.symbol("process3", 154));
  }

  @Test
  public void FieldTypes() {
    Result result = Result.createFor("FieldTypes");
    assertThat(result.symbol("fieldBoolean").type.symbol.name).isEqualTo("Boolean");
    assertThat(result.symbol("fieldBoolean").type.symbol.owner().name).isEqualTo("java.lang");
    assertThat(result.symbol("fieldList").type.toString()).isEqualTo("List");
    assertThat(result.symbol("fieldList").type.symbol.owner.name).isEqualTo("java.util");
    assertThat(result.symbol("fieldInt").type).isNotNull();
    assertThat(result.symbol("fieldInt").type.symbol.name).isEqualTo("int");
  }

  @Test
  public void ArrayTypes() {
    Result result = Result.createFor("ArrayTypes");
    assertThat(result.symbol("strings1").type.symbol.name).isEqualTo("Array");
    assertThat(result.symbol("strings2").type.symbol.name).isEqualTo("Array");
    assertThat(result.symbol("strings3").type.symbol.name).isEqualTo("Array");
    assertThat(result.symbol("strings3").type.toString()).isEqualTo("String[][][]");
    assertThat(result.symbol("objects").type.toString()).isEqualTo("Object[][][]");
  }

  @Test
  public void ThisReference() {
    Result result = Result.createFor("references/ThisReference");
    Symbol classA = result.symbol("A");
    assertThat(result.reference(7, 5).type.symbol).isEqualTo(classA);
    assertThat(result.reference(17, 17).type.symbol).isEqualTo(result.symbol("theHashtable").type.symbol);
  }

  @Test
  public void DeprecatedSymbols() {
    Result result = Result.createFor("DeprecatedSymbols");
    Symbol sym = result.symbol("A");
    assertThat(sym.isDeprecated()).isTrue();
    sym = result.symbol("field");
    assertThat(sym.isDeprecated()).isTrue();
    sym = result.symbol("fun");
    assertThat(sym.isDeprecated()).isTrue();
  }

  @Test
  public void Lambdas() throws Exception {
    Result result = Result.createFor("Lambdas");
    Symbol sym = result.symbol("o");
    assertThat(sym.type.toString()).isEqualTo("!unknown!");
    assertThat(result.reference(8, 16)).isEqualTo(result.symbol("v", 8));
    assertThat(result.reference(9, 16)).isEqualTo(result.symbol("v", 9));
  }

  @Test
  public void MethodReference() throws Exception {
    Result result = Result.createFor("MethodReferences");
    assertThatReferenceNotFound(result, 11, 27);
    assertThatReferenceNotFound(result, 12, 30);
    assertThatReferenceNotFound(result, 13, 24);
    assertThatReferenceNotFound(result, 14, 17);
    assertThat(result.reference(11, 21).owner).isSameAs(result.symbol("A"));
    assertThat(result.reference(11, 21).getName()).isEqualTo("this");
    assertThat(result.reference(12, 25).owner).isSameAs(result.symbol("A"));
    assertThat(result.reference(13, 21)).isSameAs(result.symbol("A"));

  }

  public void assertThatReferenceNotFound(Result result, int line, int column){
    try {
      Symbol reference = result.reference(line, column);
      failure("reference was found whereas it is not expected");
    } catch (IllegalArgumentException iae) {
    }
  }
}
