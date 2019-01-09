/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.resolve;

import org.junit.Test;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class MethodReferenceResolutionTest {

  @Test
  public void method_reference_as_variable_initializer() {
    Result result = Result.createFor("MethodReferencesVariableInitializers");

    JavaSymbol invalidate = result.symbol("invalidate");
    assertThat(invalidate.usages()).hasSize(1);
    assertThat(result.reference(4, 46)).isEqualTo(invalidate);

    JavaSymbol foo = result.symbol("foo");
    assertThat(foo.usages()).hasSize(1);
    assertThat(result.reference(5, 54)).isEqualTo(foo);
  }

  @Test
  public void method_reference_constructor_inference() throws Exception {
    Result result = Result.createFor("ConstructorInMethodRef");
    MethodTree methodTree = (MethodTree) result.symbol("erased").declaration();
    Type returnStatementType = ((ReturnStatementTree) methodTree.block().body().get(0)).expression().symbolType();
    assertThat(returnStatementType.is("java.util.List")).isTrue();
    assertThat(((JavaType) returnStatementType).isParameterized()).isTrue();
    assertThat(((ParametrizedTypeJavaType) returnStatementType).typeSubstitution.substitutedTypes()).hasSize(1);
    assertThat(((ParametrizedTypeJavaType) returnStatementType).typeSubstitution.substitutedTypes().get(0).is("java.util.LinkedHashSet")).isTrue();


    methodTree = (MethodTree) result.symbol("erased2").declaration();
    returnStatementType = ((ReturnStatementTree) methodTree.block().body().get(0)).expression().symbolType();
    assertThat(returnStatementType.is("java.util.List")).isTrue();
    assertThat(((JavaType) returnStatementType).isParameterized()).isTrue();
    assertThat(((ParametrizedTypeJavaType) returnStatementType).typeSubstitution.substitutedTypes()).hasSize(1);
    assertThat(((ParametrizedTypeJavaType) returnStatementType).typeSubstitution.substitutedTypes().get(0).is("java.util.LinkedHashSet")).isTrue();

    assertThat(result.symbol("<init>", 32).usages()).hasSize(1);
  }


  @Test
  public void method_references_type_defered_should_not_raise_npe() throws Exception {
    Result result = Result.createFor("MethodReferencesDeferedType");
    LambdaExpressionTree lambda = (LambdaExpressionTree) result.symbol("qualifier").declaration().parent();
    Type symbolType = ((MethodInvocationTree) lambda.body()).symbolType();
    assertThat(symbolType.is("java.util.stream.Stream")).isTrue() ;
    assertThat(symbolType).isInstanceOf(ParametrizedTypeJavaType.class);
    List<JavaType> substitutedTypes = ((ParametrizedTypeJavaType) symbolType).typeSubstitution.substitutedTypes();
    assertThat(substitutedTypes).hasSize(1);
    assertThat(substitutedTypes.get(0).is("Qualifier")).isTrue();
  }


  @Test
  public void MethodReference() throws Exception {
    Result result = Result.createFor("MethodReferences");
    JavaSymbol methodReference = result.symbol("methodReference");
    assertThat(methodReference.usages()).hasSize(3);

    JavaSymbol bar = result.symbol("bar");
    assertThat(bar.usages()).hasSize(3);
    JavaSymbol qix = result.symbol("qix");
    assertThat(result.reference(11, 27)).isSameAs(bar);
    assertThat(result.reference(12, 30)).isSameAs(bar);
    assertThat(result.reference(13, 24)).isSameAs(qix);
    assertThat(result.reference(14, 17)).isSameAs(bar);
    assertThat(result.reference(11, 21).owner).isSameAs(result.symbol("A"));
    assertThat(result.reference(11, 21).getName()).isEqualTo("this");
    assertThat(result.reference(12, 25).owner).isSameAs(result.symbol("A"));
    assertThat(result.reference(13, 21)).isSameAs(result.symbol("A"));

    JavaSymbol methodRefConstructor = result.symbol("methodRefConstructor");
    assertThat(methodRefConstructor.usages()).hasSize(1);
    assertThat(methodRefConstructor.isMethodSymbol()).isTrue();
    assertThat(((Symbol.MethodSymbol) methodRefConstructor).parameterTypes().get(0)).isSameAs(result.symbol("AProducer").type);

  }

  @Test
  public void MethodReferenceUsingThis() throws Exception {
    Result result = Result.createFor("MethodReferencesThis");

    JavaSymbol bar1 = result.symbol("bar1");
    assertThat(bar1.usages()).hasSize(2);

    JavaSymbol bar = result.symbol("bar");
    assertThat(bar.usages()).hasSize(1);

    MethodReferenceTree methodRef = (MethodReferenceTree) bar.usages().get(0).parent();
    assertThat(methodRef.symbolType().is("java.util.function.Consumer")).isTrue();

    MethodInvocationTree foreach = (MethodInvocationTree) methodRef.parent().parent();
    assertThat(foreach.symbol().owner().type().is("java.lang.Iterable")).isTrue();
  }

  @Test
  public void MethodReferenceWithArrayNew() throws Exception {
    Result result = Result.createFor("MethodReferencesArrayNew");

    JavaSymbol bar = result.symbol("bar");
    assertThat(bar.usages()).hasSize(1);

    MethodInvocationTree callingBar = (MethodInvocationTree) bar.usages().get(0).parent();
    MethodInvocationTree toArray = (MethodInvocationTree) callingBar.arguments().get(0);
    assertThat(toArray.symbolType().is("B[][]")).isTrue();

    JavaSymbol bool = result.symbol("bool");
    assertThat(bool.usages()).hasSize(1);
  }

  @Test
  public void MethodReferenceWithStream() throws Exception {
    Result result = Result.createFor("MethodReferencesStream");

    JavaSymbol flatipus1 = result.symbol("flatipus1");
    assertThat(flatipus1.usages()).hasSize(1);

    MethodInvocationTree flatMap = (MethodInvocationTree) flatipus1.usages().get(0).parent().parent().parent();
    Type symbolType = flatMap.symbolType();
    assertThat(symbolType.is("java.util.Optional")).isTrue();

    JavaSymbol flatipus2 = result.symbol("flatipus2");
    assertThat(flatipus2.usages()).hasSize(1);

    JavaSymbol bool = result.symbol("bool");
    assertThat(bool.usages()).hasSize(1);
  }

  @Test
  public void MethodReferencesNoArguments() throws Exception {
    Result result = Result.createFor("MethodReferencesNoArguments");

    JavaSymbol isTrue = result.symbol("isTrue");
    assertThat(isTrue.usages()).hasSize(1);

    JavaSymbol isFalse = result.symbol("isFalse");
    assertThat(isFalse.usages()).hasSize(1);

    JavaSymbol up = result.symbol("up");
    assertThat(up.usages()).hasSize(1);

    Tree upMethodRef = up.usages().get(0).parent();
    MethodInvocationTree map = (MethodInvocationTree) upMethodRef.parent().parent();

    JavaType mapType = (JavaType) map.symbolType();
    assertThat(mapType.is("java.util.stream.Stream")).isTrue();
    assertThat(mapType.isParameterized()).isTrue();
    List<JavaType> substitutedTypes = ((ParametrizedTypeJavaType) mapType).typeSubstitution.substitutedTypes();
    assertThat(substitutedTypes).hasSize(1);
    assertThat(substitutedTypes.get(0).is("A$B")).isTrue();

    JavaSymbol bool = result.symbol("bool", 28);
    assertThat(bool.usages().stream().map(id -> id.identifierToken().line()).collect(Collectors.toList())).containsExactly(11, 12, 13);

    bool = result.symbol("bool", 29);
    assertThat(bool.usages().stream().map(id -> id.identifierToken().line()).collect(Collectors.toList())).containsExactly(14, 15);
  }

  @Test
  public void MethodReferencesTypeArguments() throws Exception {
    Result result = Result.createFor("MethodReferencesTypeArguments");

    JavaSymbol getValue = result.symbol("getValue");
    // FIXME SONARJAVA-1663 type arguments are currently ignored
    assertThat(getValue.usages()).hasSize(0);
  }


}
