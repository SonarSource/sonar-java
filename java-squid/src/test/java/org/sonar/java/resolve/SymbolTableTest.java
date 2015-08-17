/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.failure;

public class SymbolTableTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  @Test
  public void Generics() {
    Result result = Result.createFor("Generics");
    JavaSymbol.TypeJavaSymbol typeSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("A");
    JavaSymbol symbolA1 = result.symbol("a1");
    assertThat(symbolA1.type.symbol).isSameAs(typeSymbol);
    JavaSymbol symbolA2 = result.symbol("a1");
    assertThat(symbolA2.type.symbol).isSameAs(typeSymbol);
    assertThat(symbolA2.type).isSameAs(symbolA1.type);
    JavaSymbol symbolA3 = result.symbol("a1");
    assertThat(symbolA3.type.symbol).isSameAs(typeSymbol);
    assertThat(result.reference(12, 5)).isSameAs(result.symbol("foo", 8));
    assertThat(result.reference(13, 5)).isSameAs(result.symbol("foo", 9));

    //Check erasure
    JavaType.TypeVariableJavaType STypeVariableType = (JavaType.TypeVariableJavaType) typeSymbol.typeParameters.lookup("S").get(0).type;
    assertThat(STypeVariableType.erasure().getSymbol().getName()).isEqualTo("CharSequence");
    JavaType arrayErasure = typeSymbol.members().lookup("arrayErasure").get(0).type;
    assertThat(arrayErasure.isTagged(JavaType.ARRAY)).isTrue();
    assertThat(arrayErasure.erasure().isTagged(JavaType.ARRAY)).isTrue();
    assertThat(((JavaType.ArrayJavaType)arrayErasure.erasure()).elementType().symbol.getName()).isEqualTo("CharSequence");

    IdentifierTree tree = result.referenceTree(20, 7);
    JavaType symbolType = (JavaType) tree.symbolType();
    assertThat(symbolType).isInstanceOf(JavaType.ParametrizedTypeJavaType.class);
    JavaType.ParametrizedTypeJavaType ptt = (JavaType.ParametrizedTypeJavaType) symbolType;
    assertThat(ptt.symbol.getName()).isEqualTo("C");
    assertThat(ptt.typeSubstitution.size()).isEqualTo(1);
    assertThat(ptt.typeSubstitution.substitutedType(ptt.typeSubstitution.typeVariables().iterator().next()).symbol.getName()).isEqualTo("String");

    JavaSymbol.MethodJavaSymbol method1 = (JavaSymbol.MethodJavaSymbol) typeSymbol.members().lookup("method1").get(0);
    assertThat(((JavaType.MethodJavaType)method1.type).resultType).isSameAs(STypeVariableType);

    JavaSymbol.MethodJavaSymbol method2 = (JavaSymbol.MethodJavaSymbol) typeSymbol.members().lookup("method2").get(0);
    JavaType.TypeVariableJavaType PTypeVariableType = (JavaType.TypeVariableJavaType) method2.typeParameters().lookup("P").get(0).type;
    assertThat(method2.getReturnType().type).isSameAs(PTypeVariableType);
    assertThat(method2.parameterTypes().get(0)).isSameAs(PTypeVariableType);

    //Type parameter defined in outer class
    JavaSymbol.TypeJavaSymbol classCSymbol = (JavaSymbol.TypeJavaSymbol) typeSymbol.members().lookup("C").get(0);
    JavaSymbol innerClassField = classCSymbol.members().lookup("innerClassField").get(0);
    assertThat(innerClassField.type).isSameAs(STypeVariableType);

    //Unknown parametrized type should be tagged as unknown
    MethodTree methodTree = (MethodTree) result.getTree(result.symbol("unknownSymbol"));
    VariableTree variableTree = (VariableTree) methodTree.block().body().get(0);
    assertThat(variableTree.type().symbolType().isUnknown()).isTrue();

  }

  @Test
  public void parameterized_method_type() throws Exception {
    Result result = Result.createFor("Generics");
    MethodTree method3 = (MethodTree) result.getTree(result.symbol("method3"));
    VariableTree variable = (VariableTree) method3.block().body().get(0);
    assertThat(variable.initializer().symbolType().symbol().name()).isEqualTo("String");

    MethodTree method4 = (MethodTree) result.getTree(result.symbol("method4"));
    variable = (VariableTree) method4.block().body().get(0);
    Type symbolType = variable.initializer().symbolType();
    assertThat(symbolType).isInstanceOf(JavaType.ParametrizedTypeJavaType.class);
    JavaType.ParametrizedTypeJavaType ptt = (JavaType.ParametrizedTypeJavaType) symbolType;
    assertThat(ptt.typeSubstitution.substitutedTypes().iterator().next().getSymbol().getName()).isEqualTo("String");

    assertThat(result.reference(58, 25)).isSameAs(result.symbol("method_of_e"));
  }

  @Test
  public void recursive_type_substitution() {
    Result result = Result.createFor("Generics");
    MethodTree ddt_method = (MethodTree) result.getTree(result.symbol("ddt_method"));
    VariableTree variable = (VariableTree) ddt_method.block().body().get(0);
    assertThat(variable.initializer().symbolType().name()).isEqualTo("String");
  }

  @Test
  public void ClassDeclaration() {
    Result result = Result.createFor("declarations/ClassDeclaration");
    JavaSymbol.TypeJavaSymbol typeSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("Declaration");
    JavaSymbol classDeclaration = result.symbol("ClassDeclaration");
    List<JavaSymbol> parameters = classDeclaration.type.symbol.typeParameters.lookup("T");
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
    JavaSymbol superSymbol = typeSymbol.members.lookup("super").get(0);

    typeSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("Superclass");
    assertThat(superSymbol.type.symbol).isSameAs(typeSymbol);
    assertThat(typeSymbol.symbolMetadata.isAnnotatedWith("java.lang.Override")).isFalse();
    assertThat(typeSymbol.members.lookup("super")).hasSize(1);
    superSymbol = typeSymbol.members.lookup("super").get(0);
    assertThat(superSymbol.owner).isSameAs(typeSymbol);
    assertThat(((JavaSymbol.VariableJavaSymbol) superSymbol).type.symbol).isSameAs(typeSymbol.getSuperclass().symbol);

    JavaSymbol superclass = typeSymbol.getSuperclass().symbol;
    assertThat(superclass.getName()).isEqualTo("Object");
    assertThat(superclass.owner).isInstanceOf(JavaSymbol.PackageJavaSymbol.class);
    assertThat(superclass.owner.getName()).isEqualTo("java.lang");

    assertThat(typeSymbol.getInterfaces()).isEmpty();

    typeSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("Foo");
    assertThat(typeSymbol.getSuperclass()).isSameAs(result.symbol("Baz").type);

    assertThat(result.reference(25,21)).isSameAs(result.symbol("method"));

    SymbolMetadataResolve metadata = classDeclaration.metadata();
    assertThat(metadata.annotations()).hasSize(1);
    assertThat(metadata.valuesForAnnotation("java.lang.Override")).isNull();
    assertThat(metadata.isAnnotatedWith("java.lang.Override")).isFalse();
    assertThat(metadata.valuesForAnnotation("java.lang.SuppressWarnings")).hasSize(1);
    assertThat(metadata.isAnnotatedWith("java.lang.SuppressWarnings")).isTrue();
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

    JavaSymbol.TypeJavaSymbol typeSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("methodInAnonymousClass").owner();
    assertThat(typeSymbol.owner()).isSameAs(result.symbol("method"));
    assertThat(typeSymbol.flags()).isEqualTo(0);
    assertThat(typeSymbol.name).isEqualTo("");
    assertThat(typeSymbol.getSuperclass()).isSameAs(result.symbol("Superclass").type);
    assertThat(typeSymbol.getInterfaces()).isEmpty();
    assertThat(typeSymbol.members.lookup("this")).isNotEmpty();


    typeSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("methodInAnonymousClassInterface").owner();
    assertThat(typeSymbol.owner()).isSameAs(result.symbol("method"));
    assertThat(typeSymbol.flags()).isEqualTo(0);
    assertThat(typeSymbol.name).isEqualTo("");
    assertThat(typeSymbol.getSuperclass().getSymbol().getName()).isEqualTo("Object");
    assertThat(typeSymbol.getInterfaces()).hasSize(1);
    assertThat(typeSymbol.getInterfaces().get(0)).isSameAs(result.symbol("SuperInterface").type);
    assertThat(typeSymbol.members.lookup("this")).isNotEmpty();
  }

  @Test
  public void LocalClassDeclaration() {
    Result result = Result.createFor("declarations/LocalClassDeclaration");

    JavaSymbol.TypeJavaSymbol typeSymbol;
    // TODO no forward references here, for the moment considered as a really rare situation
    // typeSymbol = (Symbol.TypeSymbol) result.symbol("Declaration", 14);
    // assertThat(typeSymbol.getSuperclass()).isSameAs(result.symbol("Superclass", 9));

    typeSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("Declaration", 22);
    assertThat(typeSymbol.getSuperclass()).isSameAs(result.symbol("Superclass", 22 - 2).type);
    assertThat(typeSymbol.members.lookup("this")).isNotEmpty();

    typeSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("Declaration", 25);
    assertThat(typeSymbol.getSuperclass()).isSameAs(result.symbol("Superclass", 9).type);
  }

  @Test
  public void InterfaceDeclaration() {
    Result result = Result.createFor("declarations/InterfaceDeclaration");

    JavaSymbol.TypeJavaSymbol interfaceSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("Declaration");
    assertThat(interfaceSymbol.owner()).isSameAs(result.symbol("InterfaceDeclaration"));
    assertThat(interfaceSymbol.flags()).isEqualTo(Flags.PRIVATE | Flags.INTERFACE);
    assertThat(interfaceSymbol.getSuperclass().getSymbol().getName()).isEqualTo("Object");
    assertThat(interfaceSymbol.getInterfaces()).containsExactly(
        result.symbol("FirstInterface").type,
        result.symbol("SecondInterface").type);
    assertThat(interfaceSymbol.members.lookup("this")).isEmpty();
    assertThat(interfaceSymbol.members.lookup("super")).isEmpty();

    JavaSymbol.VariableJavaSymbol variableSymbol = (JavaSymbol.VariableJavaSymbol) result.symbol("FIRST_CONSTANT");
    assertThat(variableSymbol.owner()).isSameAs(interfaceSymbol);
    assertThat(variableSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.STATIC | Flags.FINAL);

    variableSymbol = (JavaSymbol.VariableJavaSymbol) result.symbol("SECOND_CONSTANT");
    assertThat(variableSymbol.owner()).isSameAs(interfaceSymbol);
    assertThat(variableSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.STATIC | Flags.FINAL);

    JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) result.symbol("method");
    assertThat(methodSymbol.owner()).isSameAs(interfaceSymbol);
    assertThat(methodSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.ABSTRACT);

    JavaSymbol.MethodJavaSymbol staticMethodSymbol = (JavaSymbol.MethodJavaSymbol) result.symbol("staticMethod");
    assertThat(staticMethodSymbol.owner()).isSameAs(interfaceSymbol);
    assertThat(staticMethodSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.STATIC);

    JavaSymbol.MethodJavaSymbol defaultMethodSymbol = (JavaSymbol.MethodJavaSymbol) result.symbol("defaultMethod");
    assertThat(defaultMethodSymbol.owner()).isSameAs(interfaceSymbol);
    assertThat(defaultMethodSymbol.flags()).isEqualTo(Flags.DEFAULT | Flags.PUBLIC);

    JavaSymbol.TypeJavaSymbol typeSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("NestedClass");
    assertThat(typeSymbol.owner()).isSameAs(interfaceSymbol);
    assertThat(typeSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.STATIC);

    typeSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("NestedInterface");
    assertThat(typeSymbol.owner()).isSameAs(interfaceSymbol);
    assertThat(typeSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.STATIC | Flags.INTERFACE);

    typeSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("NestedEnum");
    assertThat(typeSymbol.owner()).isSameAs(interfaceSymbol);
    assertThat(typeSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.ENUM | Flags.STATIC);
  }

  @Test
  public void EnumDeclaration() {
    Result result = Result.createFor("declarations/EnumDeclaration");

    JavaSymbol.TypeJavaSymbol enumSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("Declaration");
    assertThat(enumSymbol.owner()).isSameAs(result.symbol("EnumDeclaration"));
    assertThat(enumSymbol.flags()).isEqualTo(Flags.PRIVATE | Flags.ENUM | Flags.STATIC);

    JavaType.ParametrizedTypeJavaType superType = (JavaType.ParametrizedTypeJavaType)enumSymbol.getSuperclass();
    JavaSymbol.TypeJavaSymbol superclass = superType.symbol;
    assertThat(superclass.getName()).isEqualTo("Enum");
    assertThat(superclass.owner).isInstanceOf(JavaSymbol.PackageJavaSymbol.class);
    assertThat(superclass.owner.getName()).isEqualTo("java.lang");
    assertThat(superType.typeSubstitution.size()).isEqualTo(1);
    Map.Entry<JavaType.TypeVariableJavaType, JavaType> entry = superType.typeSubstitution.substitutionEntries().iterator().next();
    assertThat(entry.getKey()).isSameAs(superclass.typeParameters.lookup("E").get(0).type);
    assertThat(entry.getValue()).isSameAs(enumSymbol.type);
    assertThat(enumSymbol.superClass()).isSameAs(result.symbol("parameterizedDeclaration").type);

    assertThat(enumSymbol.members.lookup("super")).hasSize(1);
    JavaSymbol.VariableJavaSymbol superSymbol = (JavaSymbol.VariableJavaSymbol) enumSymbol.members.lookup("super").get(0);
    assertThat(superSymbol.type).isSameAs(superType);

    assertThat(enumSymbol.getInterfaces()).containsExactly(
        result.symbol("FirstInterface").type,
        result.symbol("SecondInterface").type);
    assertThat(enumSymbol.members.lookup("this")).isNotEmpty();

    JavaSymbol.VariableJavaSymbol variableSymbol = (JavaSymbol.VariableJavaSymbol) result.symbol("FIRST_CONSTANT");
    assertThat(variableSymbol.owner()).isSameAs(enumSymbol);
    assertThat(variableSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.ENUM);

    JavaSymbol.TypeJavaSymbol anonymousSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("method", 11).owner();
    assertThat(anonymousSymbol.name).isEqualTo("");
    assertThat(anonymousSymbol.owner()).isSameAs(enumSymbol);
    assertThat(anonymousSymbol.flags()).isEqualTo(0); // FIXME should be ENUM
    assertThat(anonymousSymbol.getSuperclass()).isSameAs(result.symbol("Declaration").type);
    assertThat(anonymousSymbol.getInterfaces()).isEmpty();

    variableSymbol = (JavaSymbol.VariableJavaSymbol) result.symbol("SECOND_CONSTANT");
    assertThat(variableSymbol.owner()).isSameAs(enumSymbol);
    assertThat(variableSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.ENUM);

    anonymousSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("method", 16).owner();
    assertThat(anonymousSymbol.name).isEqualTo("");
    assertThat(anonymousSymbol.owner()).isSameAs(enumSymbol);
    assertThat(anonymousSymbol.flags()).isEqualTo(0); // FIXME should be ENUM
    assertThat(anonymousSymbol.getSuperclass()).isSameAs(result.symbol("Declaration").type);
    assertThat(anonymousSymbol.getInterfaces()).isEmpty();

    JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) result.symbol("method", 21);
    assertThat(methodSymbol.owner()).isSameAs(enumSymbol);
    assertThat(methodSymbol.flags()).isEqualTo(Flags.ABSTRACT);
    JavaSymbol.TypeJavaSymbol enumConstructorSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("ConstructorEnum");
    assertThat(enumConstructorSymbol).isNotNull();
    methodSymbol = (JavaSymbol.MethodJavaSymbol) enumConstructorSymbol.members().lookup("<init>").get(0);
    assertThat(methodSymbol.isPrivate()).isTrue();

    assertThat(result.reference(36,5)).isSameAs(result.symbol("<init>", 38));
    assertThat(result.reference(37,5)).isSameAs(result.symbol("<init>", 39));
  }

  @Test
  public void Enum() {
    Result result = Result.createFor("Enum");
    JavaSymbol.TypeJavaSymbol enumSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("Foo");
    assertThat(enumSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.ENUM);

    JavaType.ParametrizedTypeJavaType superType = (JavaType.ParametrizedTypeJavaType) enumSymbol.getSuperclass();
    JavaSymbol.TypeJavaSymbol superclass = superType.symbol;
    assertThat(superclass.getName()).isEqualTo("Enum");
  }

  @Test
  public void AnnotationTypeDeclaration() {
    Result result = Result.createFor("declarations/AnnotationTypeDeclaration");

    JavaSymbol.TypeJavaSymbol annotationSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("Declaration");
    assertThat(annotationSymbol.owner()).isSameAs(result.symbol("AnnotationTypeDeclaration"));
    assertThat(annotationSymbol.flags()).isEqualTo(Flags.PRIVATE | Flags.INTERFACE | Flags.ANNOTATION);
    assertThat(annotationSymbol.getSuperclass()).isNull(); // TODO should it be java.lang.Object?

    JavaSymbol superinterface = Iterables.getOnlyElement(annotationSymbol.getInterfaces()).symbol;
    assertThat(superinterface.getName()).isEqualTo("Annotation");
    assertThat(superinterface.owner).isInstanceOf(JavaSymbol.PackageJavaSymbol.class);
    assertThat(superinterface.owner.getName()).isEqualTo("java.lang.annotation");

    assertThat(annotationSymbol.members.lookup("this")).isEmpty();

    JavaSymbol.VariableJavaSymbol variableSymbol = (JavaSymbol.VariableJavaSymbol) result.symbol("FIRST_CONSTANT");
    assertThat(variableSymbol.owner()).isSameAs(annotationSymbol);
    assertThat(variableSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.STATIC | Flags.FINAL);

    variableSymbol = (JavaSymbol.VariableJavaSymbol) result.symbol("SECOND_CONSTANT");
    assertThat(variableSymbol.owner()).isSameAs(annotationSymbol);
    assertThat(variableSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.STATIC | Flags.FINAL);

    JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) result.symbol("value");
    assertThat(methodSymbol.owner()).isSameAs(annotationSymbol);
    assertThat(methodSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.ABSTRACT);

    JavaSymbol.TypeJavaSymbol typeSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("NestedClass");
    assertThat(typeSymbol.owner()).isSameAs(annotationSymbol);
    assertThat(typeSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.STATIC);

    typeSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("NestedInterface");
    assertThat(typeSymbol.owner()).isSameAs(annotationSymbol);
    assertThat(typeSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.STATIC | Flags.INTERFACE);

    typeSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("NestedEnum");
    assertThat(typeSymbol.owner()).isSameAs(annotationSymbol);
    assertThat(typeSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.ENUM | Flags.STATIC);

    typeSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("NestedAnnotationType");
    assertThat(typeSymbol.owner()).isSameAs(annotationSymbol);
    assertThat(typeSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.STATIC | Flags.INTERFACE | Flags.ANNOTATION);
  }

  @Test
  public void MethodDeclaration() {
    Result result = Result.createFor("declarations/MethodDeclaration");

    JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) result.symbol("declaration");
    assertThat(methodSymbol.owner()).isSameAs(result.symbol("MethodDeclaration"));
    assertThat(methodSymbol.flags()).isEqualTo(Flags.PROTECTED);
    assertThat(methodSymbol.getReturnType()).isSameAs(result.symbol("ReturnType"));
    assertThat(methodSymbol.thrownTypes()).containsExactly(
        result.symbol("FirstExceptionType").type(),
        result.symbol("SecondExceptionType").type());
  }

  @Test
  public void ConstructorDeclaration() {
    Result result = Result.createFor("declarations/ConstructorDeclaration");

    JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) result.symbol("<init>");
    assertThat(methodSymbol.owner()).isSameAs(result.symbol("ConstructorDeclaration"));
    assertThat(methodSymbol.flags()).isEqualTo(0);
    assertThat(methodSymbol.getReturnType()).isNull(); // TODO should it be result.symbol("ConstructorDeclaration")?
    assertThat(methodSymbol.parameterTypes()).hasSize(1);
    assertThat(methodSymbol.thrownTypes()).containsExactly(
        result.symbol("FirstExceptionType").type(),
        result.symbol("SecondExceptionType").type());

    assertThat(result.reference(21, 35)).isEqualTo(methodSymbol);
  }

  @Test
  public void CompleteHierarchyOfTypes() {
    Result result = Result.createFor("CompleteHierarchyOfTypes");

    JavaSymbol.TypeJavaSymbol typeSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("Foo");
    assertThat(typeSymbol.getSuperclass()).isSameAs(result.symbol("Baz").type);
  }

  @Test
  public void Accessibility() {
    Result result = Result.createFor("Accessibility");

    JavaSymbol.TypeJavaSymbol typeSymbol;
    typeSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("Target", 14);
    assertThat(typeSymbol.getSuperclass()).isSameAs(result.symbol("Member", 9).type);

    typeSymbol = (JavaSymbol.TypeJavaSymbol) result.symbol("Target", 29);
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
    assertThat(result.symbol("Class2").kind == JavaSymbol.TYP).isTrue();
    JavaSymbol.TypeJavaSymbol class1 = (JavaSymbol.TypeJavaSymbol) result.symbol("Class1");
    JavaSymbol.TypeJavaSymbol class2 = (JavaSymbol.TypeJavaSymbol) result.symbol("Class2");
    assertThat(class2.getSuperclass().symbol).isEqualTo(class1);
    assertThat(class1.getSuperclass()).isNotNull();
    assertThat(class1.getSuperclass().symbol.name).isEqualTo("Collection");
    JavaSymbol.TypeJavaSymbol interface1 = (JavaSymbol.TypeJavaSymbol) result.symbol("Interface1");
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
    JavaSymbol classA = result.symbol("A");
    assertThat(result.reference(7, 5).type.symbol).isEqualTo(classA);
    assertThat(result.reference(17, 17).type.symbol).isEqualTo(result.symbol("theHashtable").type.symbol);
  }

  @Test
  public void DeprecatedSymbols() {
    Result result = Result.createFor("DeprecatedSymbols");
    JavaSymbol sym = result.symbol("A");
    assertThat(sym.isDeprecated()).isTrue();
    sym = result.symbol("field");
    assertThat(sym.isDeprecated()).isTrue();
    sym = result.symbol("fun");
    assertThat(sym.isDeprecated()).isTrue();
  }

  @Test
  public void Lambdas() throws Exception {
    Result result = Result.createFor("Lambdas");
    JavaSymbol sym = result.symbol("o");
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

  @Test
  public void symbolNotFound() throws Exception {
    Result result = Result.createFor("SymbolsNotFound");

    MethodTree methodDeclaration = (MethodTree) result.symbol("method").declaration();
    ExpressionStatementTree expression = (ExpressionStatementTree) methodDeclaration.block().body().get(0);
    MethodInvocationTree methodInvocation = (MethodInvocationTree) expression.expression();

    Symbol symbolNotFound = methodInvocation.symbol();
    assertThat(symbolNotFound).isNotNull();
    assertThat(symbolNotFound.name()).isNull();
    assertThat(symbolNotFound.owner()).isSameAs(Symbols.unknownSymbol);
  }

  @Test
  public void annotations_on_fields() throws Exception {
    Result result = Result.createFor("AnnotationOnFields");

    JavaSymbol.TypeSymbol app = (JavaSymbol.TypeSymbol) result.symbol("App");
    for (Symbol sym : app.memberSymbols()) {
      if(!sym.isMethodSymbol() && !(sym.name().equals("super") || sym.name().equals("this"))) {
        assertThat(sym.metadata().isAnnotatedWith("java.lang.Deprecated")).isTrue();
      }
    }



  }

  public void assertThatReferenceNotFound(Result result, int line, int column){
    try {
      JavaSymbol reference = result.reference(line, column);
      failure("reference was found whereas it is not expected");
    } catch (IllegalArgumentException iae) {
    }
  }
}
