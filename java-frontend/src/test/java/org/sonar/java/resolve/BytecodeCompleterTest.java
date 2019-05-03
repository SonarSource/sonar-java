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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.JavaSymbol.TypeJavaSymbol;
import org.sonar.java.resolve.targets.Annotations;
import org.sonar.java.resolve.targets.AnonymousClass;
import org.sonar.java.resolve.targets.HasInnerClass;
import org.sonar.java.resolve.targets.InnerClassBeforeOuter;
import org.sonar.java.resolve.targets.InnerClassConstructors;
import org.sonar.java.resolve.targets.NamedClassWithinMethod;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class BytecodeCompleterTest {

  @Rule
  public LogTester logTester = new LogTester();

  //used to load classes in same package
  public BytecodeCompleterPackageVisibility bytecodeCompleterPackageVisibility = new BytecodeCompleterPackageVisibility();
  private BytecodeCompleter bytecodeCompleter;

  private void accessPackageVisibility() {
    bytecodeCompleterPackageVisibility.add(1, 2);
  }

  @Before
  public void setUp() throws Exception {
    bytecodeCompleter = new BytecodeCompleter(new SquidClassLoader(Lists.newArrayList(new File("target/test-classes"), new File("target/classes"))), new ParametrizedTypeCache());
    new Symbols(bytecodeCompleter);

  }

  @Test
  public void class_names_ending_with_$() throws Exception {
    JavaSymbol.TypeJavaSymbol classSymbol = bytecodeCompleter.getClassSymbol("org/sonar/java/resolve/targets/OuterClassEndingWith$$InnerClassEndingWith$");
    assertThat(classSymbol.getName()).isEqualTo("InnerClassEndingWith$");
    assertThat(classSymbol.owner().getName()).isEqualTo("OuterClassEndingWith$");
  }

  @Test
  public void innerClassWithDollarName() throws Exception {
    JavaSymbol.TypeJavaSymbol classSymbol = bytecodeCompleter.getClassSymbol("org/sonar/java/resolve/targets/UseDollarNames");
    //Complete superclass which has an inner class named A$Bq
    JavaType superclass = classSymbol.getSuperclass();
    JavaType superSuperClass = superclass.getSymbol().getSuperclass();
    assertThat(superSuperClass.fullyQualifiedName()).isEqualTo("java.lang.Object");
    Collection<Symbol> symbols = superclass.getSymbol().memberSymbols();
    for (Symbol symbol : symbols) {
      if(symbol.isTypeSymbol()) {
        assertThat(symbol.name()).isEqualTo("A$B");
        Collection<Symbol> members = ((Symbol.TypeSymbol) symbol).lookupSymbols("C$D");
        assertThat(members).isNotEmpty();
      }
    }
  }

  @Test
  public void annotations() throws Exception {
    bytecodeCompleter.getClassSymbol(Annotations.class.getName().replace('.', '/')).complete();
    assertThat(bytecodeCompleter.classesNotFound()).isEmpty();
  }

  @Test
  public void anonymous_class() {
    bytecodeCompleter.getClassSymbol(AnonymousClass.class.getName().replace('.', '/')).complete();
  }

  @Test
  public void named_class_within_method() {
    bytecodeCompleter.getClassSymbol(NamedClassWithinMethod.class.getName().replace('.', '/')).complete();
  }

  @Test
  public void inner_class_before_outer() {
    JavaSymbol.TypeJavaSymbol symbol = bytecodeCompleter.getClassSymbol(InnerClassBeforeOuter.class.getName());
    JavaSymbol.TypeJavaSymbol innerClass = symbol.getSuperclass().symbol;
    JavaSymbol.TypeJavaSymbol outerClass = (JavaSymbol.TypeJavaSymbol) innerClass.owner();
    assertThat(outerClass.members().lookup(HasInnerClass.InnerClass.class.getSimpleName())).containsExactly(innerClass);
  }

  @Test
  public void outer_class_before_inner() {
    JavaSymbol.TypeJavaSymbol outerClass = bytecodeCompleter.getClassSymbol(HasInnerClass.class.getName());
    assertThat(outerClass.members().lookup(HasInnerClass.InnerClass.class.getSimpleName())).hasSize(1);
  }

  @Test
  public void inner_classes_constructors_have_outerclass_as_implicit_first_parameter() {
    JavaSymbol.TypeJavaSymbol outerClass = bytecodeCompleter.getClassSymbol(InnerClassConstructors.class.getName());
    List<JavaSymbol> constructors;
    JavaSymbol.MethodJavaSymbol defaultConstructor;

    JavaSymbol.TypeJavaSymbol privateInnerClass = (JavaSymbol.TypeJavaSymbol) Iterables.getFirst(outerClass.lookupSymbols("PrivateInnerClass"), null);
    constructors = privateInnerClass.members().lookup("<init>");
    assertThat(constructors).hasSize(1);
    defaultConstructor = (JavaSymbol.MethodJavaSymbol) constructors.get(0);
    assertThat(defaultConstructor.parameterTypes()).hasSize(1);
    assertThat(defaultConstructor.parameterTypes().get(0)).isSameAs(outerClass.type());

    JavaSymbol.TypeJavaSymbol staticInnerClass = bytecodeCompleter.getClassSymbol(InnerClassConstructors.StaticInnerClass.class.getName());
    constructors = staticInnerClass.members().lookup("<init>");
    assertThat(constructors).hasSize(1);
    defaultConstructor = (JavaSymbol.MethodJavaSymbol) constructors.get(0);
    assertThat(defaultConstructor.parameterTypes()).isEmpty();

    JavaSymbol.TypeJavaSymbol innerClass = bytecodeCompleter.getClassSymbol(InnerClassConstructors.InnerClass.class.getName());
    constructors = innerClass.members().lookup("<init>");
    assertThat(constructors).hasSize(1);
    defaultConstructor = (JavaSymbol.MethodJavaSymbol) constructors.get(0);
    assertThat(defaultConstructor.parameterTypes()).hasSize(1);
    assertThat(defaultConstructor.parameterTypes().get(0)).isSameAs(outerClass.type());

    JavaSymbol.TypeJavaSymbol innerClassWithConstructor = bytecodeCompleter.getClassSymbol(InnerClassConstructors.InnerClassWithConstructor.class.getName());
    constructors = innerClassWithConstructor.members().lookup("<init>");
    assertThat(constructors).hasSize(1);
    JavaSymbol.MethodJavaSymbol constructor = (JavaSymbol.MethodJavaSymbol) constructors.get(0);
    assertThat(constructor.parameterTypes()).hasSize(2);
    assertThat(constructor.parameterTypes().get(0)).isSameAs(outerClass.type());
    assertThat(constructor.parameterTypes().get(1).isPrimitive(Type.Primitives.INT)).isTrue();
  }

  @Test
  public void completing_symbol_ArrayList() throws Exception {
    JavaSymbol.TypeJavaSymbol arrayList = bytecodeCompleter.getClassSymbol("java/util/ArrayList");
    //Check supertype
    assertThat(arrayList.getSuperclass().symbol.name).isEqualTo("AbstractList");
    assertThat(arrayList.getSuperclass().symbol.owner().name).isEqualTo("java.util");

    //Check interfaces
    assertThat(arrayList.getInterfaces()).hasSize(4);
    List<String> interfacesName = new ArrayList<>();
    for (JavaType interfaceType : arrayList.getInterfaces()) {
      interfacesName.add(interfaceType.symbol.name);
    }
    assertThat(interfacesName).hasSize(4);
    assertThat(interfacesName).contains("List", "RandomAccess", "Cloneable", "Serializable");
  }

  @Test
  public void symbol_type_in_same_package_should_be_resolved() throws Exception {
    JavaSymbol.TypeJavaSymbol thisTest = bytecodeCompleter.getClassSymbol(Convert.bytecodeName(getClass().getName()));
    List<JavaSymbol> symbols = thisTest.members().lookup("bytecodeCompleterPackageVisibility");
    assertThat(symbols).hasSize(1);
    JavaSymbol.VariableJavaSymbol symbol = (JavaSymbol.VariableJavaSymbol) symbols.get(0);
    assertThat(symbol.type.symbol.name).isEqualTo("BytecodeCompleterPackageVisibility");
    assertThat(symbol.type.symbol.owner().name).isEqualTo(thisTest.owner().name);
  }

  @Test
  public void void_method_type_should_be_resolved() {
    JavaSymbol.TypeJavaSymbol thisTest = bytecodeCompleter.getClassSymbol(Convert.bytecodeName(getClass().getName()));
    List<JavaSymbol> symbols = thisTest.members().lookup("bytecodeCompleterPackageVisibility");
    assertThat(symbols).hasSize(1);
    JavaSymbol.VariableJavaSymbol symbol = (JavaSymbol.VariableJavaSymbol) symbols.get(0);
    symbols = symbol.getType().symbol.members().lookup("voidMethod");
    assertThat(symbols).hasSize(1);
    JavaSymbol method = symbols.get(0);
    assertThat(method.type).isInstanceOf(MethodJavaType.class);
    assertThat(((MethodJavaType) method.type).resultType.symbol.name).isEqualTo("void");
  }

  @Test
  public void inner_class_should_be_correctly_flagged() {
    JavaSymbol.TypeJavaSymbol interfaceWithInnerEnum = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.subpackage.FlagCompletion");
    List<JavaSymbol> members = interfaceWithInnerEnum.members().lookup("bar");
    JavaSymbol.TypeJavaSymbol innerEnum = ((JavaSymbol.MethodJavaSymbol) members.get(0)).getReturnType();
    //complete outer class
    innerEnum.owner().complete();
    //verify flag are set for inner class.
    assertThat(innerEnum.isEnum()).isTrue();
    assertThat(innerEnum.isPublic()).isTrue();
    assertThat(innerEnum.isStatic()).isTrue();
    assertThat(innerEnum.isFinal()).isTrue();
  }

  @Test
  public void deprecated_classes_should_be_flagged() throws Exception {
    JavaSymbol.TypeJavaSymbol deprecatedClass = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.DeprecatedClass");
    assertThat(deprecatedClass.isDeprecated()).isTrue();
    JavaSymbol.TypeJavaSymbol staticInnerClass = (JavaSymbol.TypeJavaSymbol) deprecatedClass.members().lookup("StaticInnerClass").get(0);
    assertThat(staticInnerClass.isDeprecated()).isTrue();
    JavaSymbol.TypeJavaSymbol innerClass = (JavaSymbol.TypeJavaSymbol) deprecatedClass.members().lookup("InnerClass").get(0);
    assertThat(innerClass.isDeprecated()).isTrue();

    JavaSymbol.TypeJavaSymbol deprecatedEnum = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.DeprecatedEnum");
    assertThat(deprecatedEnum.isDeprecated()).isTrue();
    assertThat(deprecatedEnum.memberSymbols().stream().filter(Symbol::isVariableSymbol).filter(Symbol::isEnum).noneMatch(Symbol::isDeprecated)).isTrue();

    JavaSymbol.TypeJavaSymbol partiallyDeprecatedEnum = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.PartiallyDeprecatedEnum");
    assertThat(partiallyDeprecatedEnum.isDeprecated()).isFalse();
    List<Symbol> deprecatedEnumConstants = partiallyDeprecatedEnum.memberSymbols().stream()
      .filter(Symbol::isVariableSymbol)
      .filter(Symbol::isEnum)
      .filter(Symbol::isDeprecated)
      .collect(Collectors.toList());
    assertThat(deprecatedEnumConstants).hasSize(1);
    assertThat(deprecatedEnumConstants.get(0).name()).isEqualTo("C");
  }

  @Test
  public void complete_flags_for_inner_class() throws Exception {
    JavaSymbol.TypeJavaSymbol classSymbol = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.ProtectedInnerClassChild");
    JavaSymbol.MethodJavaSymbol foo = (JavaSymbol.MethodJavaSymbol) classSymbol.members().lookup("foo").get(0);
    JavaSymbol.TypeJavaSymbol innerClassRef = foo.getReturnType();
    assertThat(innerClassRef.isPrivate()).isFalse();
    assertThat(innerClassRef.isPublic()).isFalse();
    assertThat(innerClassRef.isPackageVisibility()).isFalse();
    assertThat(innerClassRef.isDeprecated()).isTrue();
  }

  @Test
  public void complete_flags_for_varargs_methods() throws Exception {
    JavaSymbol.TypeJavaSymbol classSymbol = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.ProtectedInnerClassChild");
    JavaSymbol.MethodJavaSymbol foo = (JavaSymbol.MethodJavaSymbol) classSymbol.members().lookup("foo").get(0);
    assertThat((foo.flags & Flags.VARARGS) != 0).isTrue();
  }

  @Test
  public void annotationOnSymbols() throws Exception {
    JavaSymbol.TypeJavaSymbol classSymbol = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.AnnotationSymbolMethod");
    assertThat(classSymbol.isPublic()).isTrue();
    SymbolMetadataResolve metadata = classSymbol.metadata();
    assertThat(metadata.annotations()).hasSize(3);
    assertThat(metadata.valuesForAnnotation("org.sonar.java.resolve.targets.Dummy")).isNull();
    assertThat(metadata.valuesForAnnotation("org.sonar.java.resolve.targets.ClassAnnotation")).isEmpty();
    assertThat(metadata.valuesForAnnotation("org.sonar.java.resolve.targets.RuntimeAnnotation1")).hasSize(1);
    assertThat(metadata.valuesForAnnotation("org.sonar.java.resolve.targets.RuntimeAnnotation1").iterator().next().value()).isEqualTo("plopfoo");

    assertThat(metadata.valuesForAnnotation("org.sonar.java.resolve.targets.RuntimeAnnotation2")).hasSize(2);
    Iterator<SymbolMetadata.AnnotationValue> iterator = metadata.valuesForAnnotation("org.sonar.java.resolve.targets.RuntimeAnnotation2").iterator();
    Object value = iterator.next().value();
    assertAnnotationValue(value);
    value = iterator.next().value();
    assertAnnotationValue(value);
  }

  @Test
  public void annotationArrayOfEnum() {
    JavaSymbol.TypeJavaSymbol classSymbol = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.AnnotationSymbolMethod");
    Symbol barMethod = classSymbol.memberSymbols().stream().filter(symbol -> "bar".equals(symbol.name())).findFirst().get();
    SymbolMetadata barMetadata = barMethod.metadata();
    assertThat(barMetadata.annotations()).hasSize(1);
    List<SymbolMetadata.AnnotationValue> valuesForAnnotation = barMetadata.valuesForAnnotation("org.sonar.java.resolve.targets.ArrayEnumAnnotation");
    assertThat(valuesForAnnotation).hasSize(1);
    assertThat(valuesForAnnotation.get(0).name()).isEqualTo("value");
    Object annotationValue = valuesForAnnotation.get(0).value();
    assertThat(annotationValue).isInstanceOf(Object[].class);
    assertThat((Object[]) annotationValue).hasSize(2);
    assertThat(Arrays.stream((Object[]) annotationValue)).allMatch(o -> o instanceof Symbol && ((Symbol) o).type().is("org.sonar.java.resolve.targets.MyEnum"));
  }

  private void assertAnnotationValue(Object value) {
    if (value instanceof JavaSymbol.VariableJavaSymbol) {
      JavaSymbol.VariableJavaSymbol var = (JavaSymbol.VariableJavaSymbol) value;
      assertThat(var.getName()).isEqualTo("ONE");
      assertThat(var.type.is("org.sonar.java.resolve.targets.MyEnum")).isTrue();
      return;
    } else if (value instanceof Object[]) {
      Object[] array = (Object[]) value;
      assertThat(array).hasSize(4);
      assertThat(array).contains("one", "two", "three", "four");
      return;
    }
    fail("value is not array nor variableSymbol");
  }


  @Test
  public void type_parameters_should_be_read_from_bytecode() {
    JavaSymbol.TypeJavaSymbol typeParametersSymbol = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.TypeParameters");
    typeParametersSymbol.complete();
    assertThat(typeParametersSymbol.typeParameters).isNotNull();
    assertThat(typeParametersSymbol.typeParameters.scopeSymbols()).hasSize(2);
    assertThat(typeParametersSymbol.typeVariableTypes).hasSize(2);

    TypeVariableJavaType TtypeVariableType = typeParametersSymbol.typeVariableTypes.get(0);
    assertThat(TtypeVariableType.erasure().getSymbol().getName()).isEqualTo("Object");
    assertThat(typeParametersSymbol.typeVariableTypes.get(1).erasure().getSymbol().getName()).isEqualTo("CharSequence");

    assertThat(typeParametersSymbol.getSuperclass()).isInstanceOf(ParametrizedTypeJavaType.class);
    assertThat(((ParametrizedTypeJavaType) typeParametersSymbol.getSuperclass()).typeSubstitution.typeVariables()).hasSize(1);
    TypeVariableJavaType keyTypeVariable = ((ParametrizedTypeJavaType) typeParametersSymbol.getSuperclass()).typeSubstitution.typeVariables().iterator().next();
    assertThat(keyTypeVariable.symbol.getName()).isEqualTo("S");
    JavaType actual = ((ParametrizedTypeJavaType) typeParametersSymbol.getSuperclass()).typeSubstitution.substitutedType(keyTypeVariable);
    assertThat(actual).isInstanceOf(ParametrizedTypeJavaType.class);
    assertThat(((ParametrizedTypeJavaType) actual).typeSubstitution.typeVariables()).hasSize(1);

    assertThat(typeParametersSymbol.getInterfaces()).hasSize(2);
    assertThat(typeParametersSymbol.getInterfaces().get(0)).isInstanceOf(ParametrizedTypeJavaType.class);

    JavaSymbol.MethodJavaSymbol funMethod = (JavaSymbol.MethodJavaSymbol) typeParametersSymbol.members().lookup("fun").get(0);
    assertThat(funMethod.getReturnType().type).isSameAs(TtypeVariableType);
    assertThat(funMethod.parameterTypes().get(0)).isSameAs(TtypeVariableType);

    JavaSymbol.MethodJavaSymbol fooMethod = (JavaSymbol.MethodJavaSymbol) typeParametersSymbol.members().lookup("foo").get(0);
    TypeVariableJavaType WtypeVariableType = fooMethod.typeVariableTypes.get(0);
    assertThat(fooMethod.parameterTypes().get(0).isArray()).isTrue();
    assertThat(((ArrayJavaType)fooMethod.parameterTypes().get(0)).elementType()).isSameAs(WtypeVariableType);
    JavaType resultType = ((MethodJavaType) fooMethod.type).resultType;
    assertThat(resultType).isInstanceOf(ParametrizedTypeJavaType.class);
    ParametrizedTypeJavaType actualResultType = (ParametrizedTypeJavaType) resultType;
    assertThat(actualResultType.typeSubstitution.typeVariables()).hasSize(1);
    assertThat(actualResultType.typeSubstitution.substitutedTypes().iterator().next()).isSameAs(WtypeVariableType);

    //primitive types
    assertThat(fooMethod.parameterTypes().get(1).isPrimitive(org.sonar.plugins.java.api.semantic.Type.Primitives.INT)).isTrue();
    assertThat(fooMethod.parameterTypes().get(2).isPrimitive(org.sonar.plugins.java.api.semantic.Type.Primitives.LONG)).isTrue();

    //read field.
    JavaSymbol.VariableJavaSymbol field = (JavaSymbol.VariableJavaSymbol) typeParametersSymbol.members().lookup("field").get(0);
    assertThat(field.type).isInstanceOf(TypeVariableJavaType.class);
    assertThat(field.type).isSameAs(TtypeVariableType);
  }

  @Test
  public void type_parameters_in_inner_class() {
    JavaSymbol.TypeJavaSymbol innerClass = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.ParametrizedExtend$InnerClass");
    innerClass.complete();
    JavaSymbol.MethodJavaSymbol symbol = (JavaSymbol.MethodJavaSymbol) innerClass.members().lookup("innerMethod").get(0);
    assertThat(symbol.getReturnType().type).isInstanceOf(TypeVariableJavaType.class);
    assertThat(symbol.getReturnType().getName()).isEqualTo("S");
  }

  @Test
  public void annotations_on_members() {
    JavaSymbol.TypeJavaSymbol clazz = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.AnnotationsOnMembers");
    JavaSymbol.VariableJavaSymbol field = (JavaSymbol.VariableJavaSymbol) clazz.members().lookup("field").get(0);
    JavaSymbol.MethodJavaSymbol method = (JavaSymbol.MethodJavaSymbol) clazz.members().lookup("method").get(0);
    JavaSymbol.VariableJavaSymbol parameter = (JavaSymbol.VariableJavaSymbol) method.getParameters().scopeSymbols().get(0);
    assertThat(field.metadata().valuesForAnnotation("javax.annotation.Nullable")).isNotNull();
    assertThat(field.metadata().isAnnotatedWith("javax.annotation.Nullable")).isTrue();
    assertThat(method.metadata().valuesForAnnotation("javax.annotation.CheckForNull")).isNotNull();
    assertThat(parameter.metadata().valuesForAnnotation("javax.annotation.Nullable")).isNotNull();

  }

  @Test
  public void type_annotation_on_members() {
    JavaSymbol.TypeJavaSymbol clazz = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.TypeAnnotationOnMembers");
    JavaSymbol.VariableJavaSymbol field = (JavaSymbol.VariableJavaSymbol) clazz.members().lookup("field").get(0);
    JavaSymbol.MethodJavaSymbol method = (JavaSymbol.MethodJavaSymbol) clazz.members().lookup("method").get(0);
    JavaSymbol.VariableJavaSymbol param1 = (JavaSymbol.VariableJavaSymbol) method.getParameters().scopeSymbols().get(0);
    JavaSymbol.VariableJavaSymbol param2 = (JavaSymbol.VariableJavaSymbol) method.getParameters().scopeSymbols().get(1);
    assertTypeAnnotation(field, "f");
    assertTypeAnnotation(method, "r");
    assertTypeAnnotation(param1, "p1");
    assertTypeAnnotation(param2, "p2");

    // Limitation: Annotations on "Type Parameters" are not supported by our byte code visitor in the two following cases:
    // 1) Annotation @TypeAnnotation("t") is missing on symbol "C" (see {@link BytecodeFieldVisitor#visitTypeAnnotation})
    JavaSymbol.TypeVariableJavaSymbol typeParameterC = (JavaSymbol.TypeVariableJavaSymbol) clazz.typeParameters().lookup("C").get(0);
    assertThat(typeParameterC.metadata().isAnnotatedWith("org.sonar.java.resolve.targets.TypeAnnotation")).isFalse();
    // 2) Annotation @TypeAnnotation("t") is missing on symbol "T" (see {@link BytecodeMethodVisitor#visitTypeAnnotation})
    Symbol typeParameterTSymbol = ((MethodJavaType) method.getType()).argTypes().get(1).symbol();
    assertThat(typeParameterTSymbol.name()).isEqualTo("T");
    assertThat(typeParameterTSymbol.metadata().isAnnotatedWith("org.sonar.java.resolve.targets.TypeAnnotation")).isFalse();
  }

  private static void assertTypeAnnotation(JavaSymbol symbol, String expectedValue) {
    SymbolMetadataResolve metadata = symbol.metadata();
    assertThat(metadata.isAnnotatedWith("org.sonar.java.resolve.targets.TypeAnnotation")).isTrue();
    List<SymbolMetadata.AnnotationValue> fieldValues = metadata.valuesForAnnotation("org.sonar.java.resolve.targets.TypeAnnotation");
    assertThat(fieldValues).isNotNull();
    assertThat(fieldValues.size()).isEqualTo(1);
    assertThat(fieldValues.get(0).name()).isEqualTo("value");
    assertThat(fieldValues.get(0).value()).isEqualTo(expectedValue);
  }

  @Test
  public void super_class_can_be_an_inner_class() {
    JavaSymbol.TypeJavaSymbol innerClassDerivedSymbol = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.ParametrizedExtendDerived$InnerClassDerived");
    innerClassDerivedSymbol.complete();
    assertThat(innerClassDerivedSymbol.getSuperclass().symbol().type().fullyQualifiedName()).isEqualTo("org.sonar.java.resolve.targets.ParametrizedExtend$InnerClass");
    JavaSymbol.MethodJavaSymbol symbol = (JavaSymbol.MethodJavaSymbol) innerClassDerivedSymbol.members().lookup("innerMethod").get(0);
    assertThat(symbol.getReturnType().type).isInstanceOf(TypeVariableJavaType.class);
    assertThat(symbol.getReturnType().getName()).isEqualTo("S");
  }

  @Test
  public void annotated_enum_constructor() {
    //Test to handle difference between signature and descriptor for enum:
    //see : https://bugs.openjdk.java.net/browse/JDK-8071444 and https://bugs.openjdk.java.net/browse/JDK-8024694
    Symbol.TypeSymbol clazz = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.AnnotatedEnumConstructor");
    JavaSymbol.MethodJavaSymbol constructor = (JavaSymbol.MethodJavaSymbol) clazz.lookupSymbols("<init>").iterator().next();
    assertThat(constructor.getParameters().scopeSymbols()).hasSize(1);
    for (JavaSymbol arg : constructor.getParameters().scopeSymbols()) {
      assertThat(arg.metadata().annotations()).hasSize(1);
      assertThat(arg.metadata().annotations().get(0).symbol().type().is("javax.annotation.Nullable")).isTrue();
    }
    assertThat(bytecodeCompleter.classesNotFound()).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).doesNotContain("Class not found: java.lang.Synthetic");
  }

  @Test
  public void owning_class_name() throws Exception {
    TypeJavaSymbol classSymbolCase1 = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.DistinguishNames$Case1$OWNER$$Child");
    TypeJavaSymbol classSymbolCase2 = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.DistinguishNames$Case2$OWNER$$Child");
    assertThat(classSymbolCase1.owner().name).isEqualTo("OWNER");
    assertThat(classSymbolCase1.name).isEqualTo("$Child");
    assertThat(classSymbolCase2.owner().name).isEqualTo("OWNER$");
    assertThat(classSymbolCase2.name).isEqualTo("Child");
    // No warning about a class not found
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
  }

  @Test
  public void wildcards_type_equality() {
    JavaSymbol.TypeJavaSymbol clazz = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.Wildcards");

    assertTypeAreTheSame(clazz, "equalityUnboundedWildcard1", "equalityUnboundedWildcard2");
    assertTypeAreTheSame(clazz, "equalityExtendsWildcard1", "equalityExtendsWildcard2");
    assertTypeAreTheSame(clazz, "equalitySuperWildcard1", "equalitySuperWildcard2");

    assertTypeAreNotTheSame(clazz, "equalityUnboundedWildcard1", "equalityExtendsWildcard1");
    assertTypeAreNotTheSame(clazz, "equalityUnboundedWildcard1", "equalitySuperWildcard1");
    assertTypeAreNotTheSame(clazz, "equalityExtendsWildcard1", "equalitySuperWildcard1");
  }

  private static void assertTypeAreTheSame(TypeJavaSymbol clazz, String varName1, String varName2) {
    JavaSymbol.VariableJavaSymbol var1 = getVariable(clazz, varName1);
    JavaSymbol.VariableJavaSymbol var2 = getVariable(clazz, varName2);
    assertThat(var1.type).isSameAs(var2.type);
  }

  private static void assertTypeAreNotTheSame(TypeJavaSymbol clazz, String varName1, String varName2) {
    JavaSymbol.VariableJavaSymbol var1 = getVariable(clazz, varName1);
    JavaSymbol.VariableJavaSymbol var2 = getVariable(clazz, varName2);
    assertThat(var1.type).isNotSameAs(var2.type);
  }

  private static JavaSymbol.VariableJavaSymbol getVariable(JavaSymbol.TypeJavaSymbol clazz, String name) {
    return (JavaSymbol.VariableJavaSymbol) clazz.members().lookup(name).get(0);
  }

  @Test
  public void wildcards_in_fields_declaration() {
    JavaSymbol.TypeJavaSymbol clazz = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.Wildcards");
    JavaSymbol.VariableJavaSymbol unboudedItems = getVariable(clazz, "unboudedItems");
    JavaSymbol.VariableJavaSymbol extendsItems = getVariable(clazz, "extendsItems");
    JavaSymbol.VariableJavaSymbol superItems = getVariable(clazz, "superItems");

    assertThatWildcardIs(unboudedItems, WildCardType.BoundType.UNBOUNDED, "java.lang.Object");
    assertThatWildcardIs(extendsItems, WildCardType.BoundType.EXTENDS, "java.lang.String");
    assertThatWildcardIs(superItems, WildCardType.BoundType.SUPER, "java.lang.Number");
  }

  @Test
  public void wildcards_in_methods_declaration() {
    JavaSymbol.TypeJavaSymbol clazz = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.Wildcards");
    JavaSymbol.MethodJavaSymbol returnsUnboundedItems = (JavaSymbol.MethodJavaSymbol) clazz.members().lookup("returnsUnboundedItems").get(0);
    JavaSymbol.MethodJavaSymbol returnsExtendsItems = (JavaSymbol.MethodJavaSymbol) clazz.members().lookup("returnsExtendsItems").get(0);
    JavaSymbol.MethodJavaSymbol returnsSuperItems = (JavaSymbol.MethodJavaSymbol) clazz.members().lookup("returnsSuperItems").get(0);

    assertThatWildcardIs(returnsUnboundedItems, WildCardType.BoundType.UNBOUNDED, "java.lang.Object");
    assertThatWildcardIs(returnsExtendsItems, WildCardType.BoundType.EXTENDS, "java.lang.String");
    assertThatWildcardIs(returnsSuperItems, WildCardType.BoundType.SUPER, "java.lang.Number");

    assertThatWildcardInFirstParamIs(returnsUnboundedItems, WildCardType.BoundType.UNBOUNDED, "java.lang.Object");
    assertThatWildcardInFirstParamIs(returnsExtendsItems, WildCardType.BoundType.EXTENDS, "java.lang.String");
    assertThatWildcardInFirstParamIs(returnsSuperItems, WildCardType.BoundType.SUPER, "java.lang.Number");
  }

  @Test
  public void wildcards_in_class_declaration() {
    JavaSymbol.TypeJavaSymbol wildcardUnboundedClass = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.WildcardUnboundedClass");
    JavaSymbol.TypeJavaSymbol WildcardExtendsClass = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.WildcardExtendsClass");
    JavaSymbol.TypeJavaSymbol WildcardSuperClass = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.WildcardSuperClass");

    assertThatWildcardInTypeParamIs(wildcardUnboundedClass, WildCardType.BoundType.UNBOUNDED, "java.lang.Object");
    assertThatWildcardInTypeParamIs(WildcardExtendsClass, WildCardType.BoundType.EXTENDS, "java.lang.String");
    assertThatWildcardInTypeParamIs(WildcardSuperClass, WildCardType.BoundType.SUPER, "java.lang.Number");
  }

  private static void assertThatWildcardInTypeParamIs(JavaSymbol.TypeJavaSymbol symbol, WildCardType.BoundType wildcard, String bound) {
    JavaType javaType = ((TypeVariableJavaType) symbol.typeParameters().lookup("X").iterator().next().getType()).bounds.get(0);
    assertThatWildcardIs(javaType, wildcard, bound);
  }

  private static void assertThatWildcardInFirstParamIs(JavaSymbol.MethodJavaSymbol symbol, WildCardType.BoundType wildcard, String bound) {
    assertThatWildcardIs(symbol.parameterTypes().get(0), wildcard, bound);
  }

  private static void assertThatWildcardIs(JavaSymbol.MethodJavaSymbol symbol, WildCardType.BoundType wildcard, String bound) {
    assertThatWildcardIs(((MethodJavaType) symbol.type).resultType, wildcard, bound);
  }

  private static void assertThatWildcardIs(JavaSymbol.VariableJavaSymbol symbol, WildCardType.BoundType wildcard, String bound) {
    assertThatWildcardIs(symbol.type, wildcard, bound);
  }

  private static void assertThatWildcardIs(Type type, WildCardType.BoundType wildcard, String bound) {
    assertThat(type).isInstanceOf(ParametrizedTypeJavaType.class);
    
    ParametrizedTypeJavaType ptjt = (ParametrizedTypeJavaType) type;
    JavaType substitutedType = ptjt.typeSubstitution.substitutedTypes().get(0);
    assertThat(substitutedType).isInstanceOf(WildCardType.class);
    
    WildCardType wildcardType = (WildCardType) substitutedType;
    assertThat(wildcardType.boundType).isEqualTo(wildcard);
    assertThat(wildcardType.bound.is(bound)).isTrue();
  }

  @Test
  public void class_not_found_should_have_unknown_super_type_and_no_interfaces() {
    Symbol.TypeSymbol clazz = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.UnknownClass");
    assertThat(clazz.type()).isNotNull();
    Type superClass = clazz.superClass();
    assertThat(superClass).isNotNull();
    assertThat(superClass).isSameAs(Symbols.unknownType);
    List<Type> interfaces = clazz.interfaces();
    assertThat(interfaces).isNotNull();
    assertThat(interfaces).isEmpty();
    assertThat(bytecodeCompleter.classesNotFound()).containsOnly("org.sonar.java.resolve.targets.UnknownClass");
  }

  @Test
  public void loadClass_with_edge_cases_do_not_fail() {
    JavaSymbol symbolEmptyString = bytecodeCompleter.loadClass("");
    assertThat(symbolEmptyString).isNotNull();
    assertThat(symbolEmptyString.isUnknown()).isTrue();

    JavaSymbol symbolRandom = bytecodeCompleter.loadClass("^/^/v/v/$/$/$/$/B/A");
    assertThat(symbolRandom).isNotNull();
    assertThat(symbolRandom.isUnknown()).isTrue();

    JavaSymbol symbolUnknownClass = bytecodeCompleter.loadClass("org.sonar.java.resolve.targets.UnknownClass");
    assertThat(symbolUnknownClass).isNotNull();
    assertThat(symbolUnknownClass.isUnknown()).isTrue();

    JavaSymbol symbolNPE = bytecodeCompleter.loadClass("java.lang.NullPointerException");
    assertThat(symbolNPE).isNotNull();
    assertThat(symbolNPE.isUnknown()).isFalse();
    assertThat(symbolNPE.type().isSubtypeOf("java.lang.Exception")).isTrue();
  }

  @Test
  public void forward_type_parameter_in_methods() throws Exception {
    Symbol.TypeSymbol clazz = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.ForwardParameterInMethod");
    assertThat(clazz.type()).isNotNull();
    Collection<Symbol> symbols = clazz.lookupSymbols("bar");
    assertThat(symbols).hasSize(1);
    Symbol method = symbols.iterator().next();
    Collection<JavaSymbol> typeParameters = ((JavaSymbol.MethodJavaSymbol) method).typeParameters().scopeSymbols();
    assertThat(typeParameters).hasSize(2);
    JavaSymbol xSymbol = ((JavaSymbol.MethodJavaSymbol) method).typeParameters().lookup("X").iterator().next();
    JavaSymbol ySymbol = ((JavaSymbol.MethodJavaSymbol) method).typeParameters().lookup("Y").iterator().next();
    assertThat(((TypeVariableJavaType) xSymbol.type).bounds).hasSize(1);
    JavaType bound = ((TypeVariableJavaType) xSymbol.type).bounds.get(0);
    assertThat(((ParametrizedTypeJavaType)bound).typeParameters()).hasSize(1);
    assertThat(((ParametrizedTypeJavaType)bound).substitution(((ParametrizedTypeJavaType)bound).typeParameters().get(0))).isSameAs(ySymbol.type);
  }

  @Test
  public void forward_type_parameter_in_classes() throws Exception {
    Symbol.TypeSymbol clazz = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.ForwardParameterInClass");
    assertThat(clazz.type()).isNotNull();
    Collection<Symbol> symbols = clazz.lookupSymbols("bar");
    assertThat(symbols).hasSize(1);
    Collection<JavaSymbol> typeParameters = ((JavaSymbol.TypeJavaSymbol) clazz).typeParameters().scopeSymbols();
    assertThat(typeParameters).hasSize(2);
    JavaSymbol xSymbol = ((JavaSymbol.TypeJavaSymbol) clazz).typeParameters().lookup("X").iterator().next();
    JavaSymbol ySymbol = ((JavaSymbol.TypeJavaSymbol) clazz).typeParameters().lookup("Y").iterator().next();
    assertThat(((TypeVariableJavaType) xSymbol.type).bounds).hasSize(1);
    JavaType bound = ((TypeVariableJavaType) xSymbol.type).bounds.get(0);
    assertThat(((ParametrizedTypeJavaType)bound).typeParameters()).hasSize(1);
    assertThat(((ParametrizedTypeJavaType)bound).substitution(((ParametrizedTypeJavaType)bound).typeParameters().get(0))).isSameAs(ySymbol.type);

  }

  @Test
  public void test_completion_of_generic_inner_class() throws Exception {
    Symbol.TypeSymbol clazz = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.UseGenericInnerClass");
    Symbol builder = clazz.lookupSymbols("builder").iterator().next();
    Symbol.TypeSymbol genericInnerClass = (Symbol.TypeSymbol) builder.type().symbol().owner();
    Symbol methodBuilder = genericInnerClass.lookupSymbols("builder").iterator().next();

    JavaType resultType = ((MethodJavaType) methodBuilder.type()).resultType;
    assertThat(resultType).isInstanceOf(ParametrizedTypeJavaType.class);
  }

  @Test
  public void array_types_dimension_taken_into_account() throws Exception {
    Symbol.TypeSymbol clazz = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.ArrayTypes");
    Symbol field = clazz.lookupSymbols("field").iterator().next();

    Type type = field.type();
    assertThat(type.isArray()).isTrue();

    Type elementType = ((Type.ArrayType) type).elementType();
    assertThat(elementType.isArray()).isTrue();

    elementType = ((Type.ArrayType) elementType).elementType();
    assertThat(elementType.is("java.lang.Object")).isTrue();
  }

  @Test
  public void inner_class_obfuscated() throws Exception {
    Symbol.TypeSymbol clazz = bytecodeCompleter.getClassSymbol("ChartDirector.Layer");
    assertThat(clazz.memberSymbols().stream().anyMatch(s-> s.name().equals("fj"))).isTrue();

    Symbol.TypeSymbol library = bytecodeCompleter.getClassSymbol("com.jniwrapper.Library");
    Optional<Symbol> ar = library.memberSymbols().stream().filter(s -> s.name().equals("ar")).findFirst();
    assertThat(ar.isPresent()).isTrue();
    // Assert that the inner class fully qualified name does not contain the enclosing class name as it was obfuscated.
    ar.ifPresent(s -> assertThat(((TypeJavaSymbol) s).getFullyQualifiedName()).isEqualTo("com.jniwrapper.ar"));
  }

  @Test
  public void package_annotations() throws Exception {
    Symbol.TypeSymbol clazz = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.MethodSymbols");
    assertThat(((TypeJavaSymbol) clazz).packge().metadata().isAnnotatedWith("javax.annotation.ParametersAreNonnullByDefault")).isTrue();
  }

  @Test
  public void defaultMethods_should_be_correctly_flagged() throws Exception {
    Symbol.TypeSymbol clazz = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.DefaultMethods");
    ((TypeJavaSymbol) clazz).complete();

    assertThat(((TypeJavaSymbol) clazz).flags() & Flags.INTERFACE).isNotZero();
    JavaSymbol.MethodJavaSymbol abstractMethod = (JavaSymbol.MethodJavaSymbol) ((TypeJavaSymbol) clazz).members().lookup("abstractMethod").get(0);
    assertThat(abstractMethod.flags() & Flags.ABSTRACT).isNotZero();
    assertThat(abstractMethod.flags() & Flags.DEFAULT).isZero();

    JavaSymbol.MethodJavaSymbol defaultMethod = (JavaSymbol.MethodJavaSymbol) ((TypeJavaSymbol) clazz).members().lookup("defaultMethod").get(0);
    assertThat(defaultMethod.flags() & Flags.ABSTRACT).isZero();
    assertThat(defaultMethod.flags() & Flags.DEFAULT).isNotZero();

    JavaSymbol.MethodJavaSymbol staticMethod = (JavaSymbol.MethodJavaSymbol) ((TypeJavaSymbol) clazz).members().lookup("staticMethod").get(0);
    assertThat(staticMethod.flags() & Flags.ABSTRACT).isZero();
    assertThat(staticMethod.flags() & Flags.DEFAULT).isZero();
    assertThat(staticMethod.flags() & Flags.STATIC).isNotZero();
  }


  @Test
  public void bridge_method_not_synthetic_should_not_be_created_as_symbol_nor_fail_analysis() throws Exception {
    TypeJavaSymbol prezModel42 = bytecodeCompleter.getClassSymbol("model42.PresentationModel42");
    prezModel42.complete();
    assertThat(prezModel42.members().lookup("setSliderMinValue")).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("bridge method setSliderMinValue not marked as synthetic in class model42/PresentationModel42");
  }

  @Test
  public void test_loading_java9_class() throws Exception {
    BytecodeCompleter bytecodeCompleter = new BytecodeCompleter(
      new SquidClassLoader(Collections.singletonList(new File("src/test/files/bytecode/java9/bin"))),
      new ParametrizedTypeCache());
    new Symbols(bytecodeCompleter);
    TypeJavaSymbol classSymbol = (TypeJavaSymbol) bytecodeCompleter.loadClass("org.test.Hello9");
    classSymbol.complete();
    assertThat(classSymbol.getFullyQualifiedName()).isEqualTo("org.test.Hello9");
    assertThat(classSymbol.memberSymbols()).hasSize(2);
  }

  @Test
  public void test_loading_java9_iface() throws Exception {
    BytecodeCompleter bytecodeCompleter = new BytecodeCompleter(
      new SquidClassLoader(Collections.singletonList(new File("src/test/files/bytecode/java9/bin"))),
      new ParametrizedTypeCache());
    new Symbols(bytecodeCompleter);
    TypeJavaSymbol iface = (TypeJavaSymbol) bytecodeCompleter.loadClass("org.test.IfaceTest");
    iface.complete();
    assertThat(iface.getFullyQualifiedName()).isEqualTo("org.test.IfaceTest");
    assertThat(iface.memberSymbols()).hasSize(3);
    assertThat(iface.isInterface()).isTrue();

    JavaSymbol.MethodJavaSymbol privateMethod = (JavaSymbol.MethodJavaSymbol) Iterables.getOnlyElement(iface.lookupSymbols("privateMethod"));
    assertThat(privateMethod.flags()).isEqualTo(Flags.PRIVATE);

    JavaSymbol.MethodJavaSymbol defaultMethod = (JavaSymbol.MethodJavaSymbol) Iterables.getOnlyElement(iface.lookupSymbols("defaultMethod"));
    assertThat(defaultMethod.flags()).isEqualTo(Flags.DEFAULT | Flags.PUBLIC);

    JavaSymbol.MethodJavaSymbol staticMethod = (JavaSymbol.MethodJavaSymbol) Iterables.getOnlyElement(iface.lookupSymbols("staticMethod"));
    assertThat(staticMethod.flags()).isEqualTo(Flags.STATIC | Flags.PUBLIC);
  }

  @Test
  public void test_loading_java10_class() throws Exception {
    BytecodeCompleter bytecodeCompleter = new BytecodeCompleter(
      new SquidClassLoader(Collections.singletonList(new File("src/test/files/bytecode/java10/bin"))),
      new ParametrizedTypeCache());
    new Symbols(bytecodeCompleter);
    TypeJavaSymbol classSymbol = (TypeJavaSymbol) bytecodeCompleter.loadClass("org.foo.A");
    classSymbol.complete();
    assertThat(classSymbol.getFullyQualifiedName()).isEqualTo("org.foo.A");
    assertThat(classSymbol.memberSymbols()).hasSize(2);

    Scope members = classSymbol.members();
    Symbol implicitDefaultConstructor = members.lookup("<init>").get(0);
    Symbol foo = members.lookup("foo").get(0);
    assertThat(implicitDefaultConstructor.name()).isEqualTo("<init>");
    assertThat(implicitDefaultConstructor.isMethodSymbol()).isTrue();
    assertThat(((JavaSymbol.MethodJavaSymbol) implicitDefaultConstructor).isConstructor()).isTrue();
    assertThat(((JavaSymbol.MethodJavaSymbol) implicitDefaultConstructor).parameterTypes()).isEmpty();

    assertThat(foo.name()).isEqualTo("foo");
    assertThat(foo.isMethodSymbol()).isTrue();
    assertThat(((JavaSymbol.MethodJavaSymbol) foo).isConstructor()).isFalse();
    assertThat(((JavaSymbol.MethodJavaSymbol) foo).parameterTypes()).isEmpty();
  }

  @Test
  public void test_loading_java11_class() {
    BytecodeCompleter bytecodeCompleter = new BytecodeCompleter(
      new SquidClassLoader(Collections.singletonList(new File("src/test/files/bytecode/java11/bin"))),
      new ParametrizedTypeCache());
    new Symbols(bytecodeCompleter);
    TypeJavaSymbol classSymbol = (TypeJavaSymbol) bytecodeCompleter.loadClass("org.foo.A");
    classSymbol.complete();
    assertThat(classSymbol.getFullyQualifiedName()).isEqualTo("org.foo.A");
    assertThat(classSymbol.memberSymbols()).hasSize(4);

    Scope members = classSymbol.members();
    Symbol implicitDefaultConstructor = members.lookup("<init>").get(0);
    Symbol foo = members.lookup("foo").get(0);
    assertThat(implicitDefaultConstructor.name()).isEqualTo("<init>");
    assertThat(implicitDefaultConstructor.isMethodSymbol()).isTrue();
    assertThat(((JavaSymbol.MethodJavaSymbol) implicitDefaultConstructor).isConstructor()).isTrue();
    assertThat(((JavaSymbol.MethodJavaSymbol) implicitDefaultConstructor).parameterTypes()).isEmpty();

    assertThat(foo.name()).isEqualTo("foo");
    assertThat(foo.isMethodSymbol()).isTrue();
    assertThat(((JavaSymbol.MethodJavaSymbol) foo).isConstructor()).isFalse();
    assertThat(((JavaSymbol.MethodJavaSymbol) foo).parameterTypes()).isEmpty();
  }

  @Test
  public void default_value_of_annotation_methods() throws Exception {
    Symbol.TypeSymbol annotation = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.DefaultValueOfAnnotationMethods");

    Map<String, Object> nameToDefaultValue = new HashMap<>();
    List<JavaSymbol.MethodJavaSymbol> methodJavaSymbols = annotation.memberSymbols()
      .stream()
      .filter(Symbol::isMethodSymbol)
      .map(s -> (JavaSymbol.MethodJavaSymbol) s)
      .collect(Collectors.toList());
    for (JavaSymbol.MethodJavaSymbol methodJavaSymbol : methodJavaSymbols) {
      nameToDefaultValue.put(methodJavaSymbol.name(), methodJavaSymbol.defaultValue);
    }

    assertThat(nameToDefaultValue.get("valueString")).isEqualTo("valueDefault");
    assertThat(nameToDefaultValue.get("valueInt")).isEqualTo(42);
    assertThat(nameToDefaultValue.get("valueLong")).isEqualTo(42L);
    // constants are computed by compiler.
    assertThat(nameToDefaultValue.get("valueStringConstant")).isEqualTo("value4Default");
    // default ints are wrapped to arrays.
    assertThat(nameToDefaultValue.get("valueArray")).isEqualTo(new int[] {0});
    assertThat(nameToDefaultValue.get("noDefault")).isNull();
    // unsupported
    assertThat(nameToDefaultValue.get("valueEnum")).isNull();

  }

  @Test
  public void constant_value() {
    TypeJavaSymbol classSymbol = bytecodeCompleter.getClassSymbol("org.sonar.java.resolve.targets.ClassWithConstants");

    assertThat(lookupVariable(classSymbol, "CONST1").constantValue().orElse(null)).isEqualTo("CONST_VALUE");
    assertThat(lookupVariable(classSymbol, "nonStatic").constantValue()).isEmpty();
    assertThat(lookupVariable(classSymbol, "nonFinal").constantValue()).isEmpty();
  }

  private JavaSymbol.VariableJavaSymbol lookupVariable(TypeJavaSymbol classSymbol, String variableName) {
    return (JavaSymbol.VariableJavaSymbol) classSymbol.lookupSymbols(variableName).iterator().next();
  }
}
