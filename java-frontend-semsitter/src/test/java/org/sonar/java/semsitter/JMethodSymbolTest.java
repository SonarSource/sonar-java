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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.assertj.core.api.Assertions.assertThat;

class JMethodSymbolTest {

  private static final String SOURCE = """
    @interface Ann {
      int value() default 1;
      String[] names() default "x";
      String[] all() default {"a", "b"};
      int none();
    }
    class M {
      <T> void generic(T t) {
      }
      void plain() {
      }
      void varargs(String... args) {
      }
      void array(String[] args) {
      }
    }
    """;

  @TempDir
  Path baseDir;

  private final JSema sema = new JSema();
  private CompilationUnitTree unit;
  private JTypeSymbol owner;

  @BeforeEach
  void setUp() throws Exception {
    unit = (CompilationUnitTree) Replays.parseWithEcj(baseDir, "Ann", SOURCE).get();
    owner = sema.typeSymbol("p.A");
    owner.owner(sema.packageSymbol("p"));
  }

  @Test
  void views_keep_modifiers_and_detect_their_own_type_variables() {
    JType variable = (JType) sema.memberFactory().typeParameter("p.A#m(T)<T>").type();
    JType none = sema.primitiveType("void");
    JMethodSymbol view = viewOf(method("p.A#m(T)", "native", "synchronized", "abstract", "default", "protected", "final"), List.of(JType.array(sema, variable)), none);
    JMethodSymbol listView = viewOf(method("p.A#m(T)"), List.of(JType.named(sema, "java.util.List", List.of(JType.capture(sema, variable)))), none);
    JMethodSymbol returned = viewOf(method("p.A#m(T)"), List.of(), variable);
    JMethodSymbol foreign = viewOf(method("p.A#m(T)"), List.of(JType.typeVar(sema, "E", "p.A<E>"), sema.primitiveType("int")), none);
    JMethodSymbol anonymous = viewOf(new JMethodSymbol(sema, null, "m"), List.of(variable), variable);

    assertThat(view.isNativeMethod()).isTrue();
    assertThat(view.isSynchronizedMethod()).isTrue();
    assertThat(view.isDefaultMethod()).isTrue();
    assertThat(view.isProtected()).isTrue();
    assertThat(view.isFinal()).isTrue();
    assertThat(view.isParametrizedMethod()).isTrue();
    assertThat(listView.isParametrizedMethod()).isTrue();
    assertThat(returned.isParametrizedMethod()).isTrue();
    assertThat(foreign.isParametrizedMethod()).isFalse();
    assertThat(anonymous.isParametrizedMethod()).isFalse();
    assertThat(view.lambdaView("p.A#m(T)$lambda$1:2()")).isNotEqualTo(view);
  }

  @Test
  void a_lambda_view_is_a_lambda() {
    JMethodSymbol view = method("p.A#m()").lambdaView("p.A#m()$lambda$1:2()");

    assertThat(view.isLambda()).isTrue();
    assertThat(view.signature()).isEqualTo("p.A#m()Ljava/lang/Object;");
  }

  @Test
  void signature_is_the_descriptor_of_erased_types() {
    JMethodSymbol method = method("p.A#m(byte,char,double,float,int,long,short,boolean,java.lang.String[],X)");
    method.parameterTypes(List.of(primitive("byte"), primitive("char"), primitive("double"), primitive("float"), primitive("int"), primitive("long"),
      primitive("short"), primitive("boolean"), JType.array(sema, (JType) sema.getClassType("java.lang.String")), JType.unknown(sema)));
    method.returnTypeSymbol(sema.memberFactory().primitiveSymbol("void"));

    assertThat(method.signature()).isEqualTo("p.A#m(BCDFIJSZ[Ljava/lang/String;Ljava/lang/Object;)V");
  }

  @Test
  void constructors_of_enums_and_inner_classes_take_hidden_parameters() {
    owner.enumSymbol(true);
    JTypeSymbol inner = sema.typeSymbol("p.O$I");
    inner.owner(sema.typeSymbol("p.O"));
    JMethodSymbol innerConstructor = new JMethodSymbol(sema, "p.O$I#<init>()", "<init>");
    innerConstructor.owner(inner);
    JTypeSymbol nested = sema.typeSymbol("p.O$N");
    nested.owner(sema.typeSymbol("p.O"));
    nested.addModifiers(List.of("static"));
    JMethodSymbol nestedConstructor = new JMethodSymbol(sema, "p.O$N#<init>()", "<init>");
    nestedConstructor.owner(nested);

    assertThat(method("p.A#<init>()").signature()).isEqualTo("p.A#<init>(Ljava/lang/String;I)V");
    assertThat(innerConstructor.signature()).isEqualTo("p.O$I#<init>(Lp/O;)V");
    assertThat(nestedConstructor.signature()).isEqualTo("p.O$N#<init>()V");
  }

  @Test
  void signature_of_an_unknown_method_or_one_owned_by_no_type_is_its_id() {
    JMethodSymbol unknown = method("p.A#u`1()");
    unknown.unknown(true);
    JMethodSymbol inMethod = new JMethodSymbol(sema, "p.A#m()#n()", "n");
    inMethod.owner(sema.packageSymbol("p"));

    assertThat(unknown.signature()).isEqualTo("p.A#u()");
    assertThat(inMethod.signature()).isEqualTo("p.A#m()#n()");
  }

  @Test
  void unknown_methods_have_no_flags() {
    JMethodSymbol unknown = method("p.A#m()", "default", "synchronized", "native");
    unknown.parametrized(true);
    unknown.varArgs(true);
    unknown.unknown(true);

    assertThat(unknown.isOverridable()).isFalse();
    assertThat(unknown.isParametrizedMethod()).isFalse();
    assertThat(unknown.isDefaultMethod()).isFalse();
    assertThat(unknown.isSynchronizedMethod()).isFalse();
    assertThat(unknown.isNativeMethod()).isFalse();
    assertThat(unknown.isVarArgsMethod()).isFalse();
  }

  @Test
  void methods_of_final_classes_are_not_overridable() {
    owner.addModifiers(List.of("final"));

    assertThat(method("p.A#m()").isOverridable()).isFalse();
    assertThat(method("p.A#m()").isNativeMethod()).isFalse();
  }

  @Test
  void declared_methods_are_generic_and_variable_arity_as_written() {
    assertThat(declared("generic").isParametrizedMethod()).isTrue();
    assertThat(declared("plain").isParametrizedMethod()).isFalse();
    assertThat(declared("plain").isVarArgsMethod()).isFalse();
    assertThat(declared("varargs").isVarArgsMethod()).isTrue();
    assertThat(declared("array").isVarArgsMethod()).isFalse();
  }

  @Test
  void variable_arity_without_declaration_comes_from_the_answer_or_the_last_parameter() {
    JMethodSymbol known = method("p.A#k(java.lang.String[])");
    known.parameterTypes(List.of(JType.array(sema, (JType) sema.getClassType("java.lang.String"))));
    known.varArgsKnown(true);
    JMethodSymbol guessed = method("p.A#g(java.lang.String[])");
    guessed.parameterTypes(known.parameterTypes());

    assertThat(known.isVarArgsMethod()).isFalse();
    assertThat(guessed.isVarArgsMethod()).isTrue();
    assertThat(method("p.A#n()").isVarArgsMethod()).isFalse();
  }

  @Test
  void annotation_element_defaults_come_from_the_declaration_or_the_answer() {
    JMethodSymbol names = declared("names");
    names.returnTypeSymbol(sema.getClassType("java.lang.String[]").symbol());
    JMethodSymbol all = declared("all");
    all.returnTypeSymbol(sema.getClassType("java.lang.String[]").symbol());
    JMethodSymbol served = method("p.A#s()");
    sema.declaredMembers.annotationDefaults.put("p.A#s()", 3);

    assertThat(declared("value").annotationElementDefaultValue()).isEqualTo(1);
    assertThat(declared("none").annotationElementDefaultValue()).isNull();
    assertThat((Object[]) names.annotationElementDefaultValue()).containsExactly("x");
    assertThat((Object[]) all.annotationElementDefaultValue()).containsExactly("a", "b");
    assertThat(served.annotationElementDefaultValue()).isEqualTo(3);
    assertThat(new JMethodSymbol(sema, null, "x").annotationElementDefaultValue()).isNull();
  }

  private static JMethodSymbol viewOf(JMethodSymbol method, List<Type> parameters, Type returned) {
    method.parameterTypes(parameters);
    method.returnTypeSymbol(returned.symbol());
    return method.withSignature(parameters, returned);
  }

  private Type primitive(String name) {
    return sema.primitiveType(name);
  }

  private JMethodSymbol method(String id, String... modifiers) {
    JMethodSymbol method = new JMethodSymbol(sema, id, SemaFacts.memberName(id));
    method.owner(owner);
    method.addModifiers(List.of(modifiers));
    return method;
  }

  private JMethodSymbol declared(String name) {
    List<MethodTree> found = new ArrayList<>();
    unit.accept(new BaseTreeVisitor() {
      @Override
      public void visitMethod(MethodTree tree) {
        if (name.equals(tree.simpleName().name())) {
          found.add(tree);
        }
      }
    });
    JMethodSymbol method = method("p.A#" + name + "()");
    sema.declare(method.id, found.get(0));
    return method;
  }
}
