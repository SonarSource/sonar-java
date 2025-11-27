/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.ast.visitors;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

class MethodNestingLevelVisitorTest {

  @Test
  void anonymousClassHandling() {
    CompilationUnitTree cut = JParserTestUtils.parse("class A {" +
      " Object foo(){" +
      "   Runnable runnable = new Runnable() { " +
      "     @Override " +
      "     public void run() { if(true){ System.out.println(\"Hi.\"); } }" +
      "     };" +
      "   abstract class AbsAnonym { " +
      "     abstract void absVoid(); " +
      "   } " +
      " } " +
      "}");
    var methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    int nesting = new MethodNestingLevelVisitor().getMaxNestingLevel(methodTree);
    assertThat(nesting).isEqualTo(3);
    var anonymousClass = (NewClassTree) ((VariableTree) methodTree.block().body().get(0)).initializer();
    var anonymousMethod = (MethodTree) anonymousClass.classBody().members().get(0);
    int anonymousMethodNesting = new MethodNestingLevelVisitor().getMaxNestingLevel(anonymousMethod);
    assertThat(anonymousMethodNesting).isEqualTo(1);
  }

  void testt() {
    abstract class Goo {
      abstract void goo();
    }
  }

  @Test
  void ifElseHandling() {
    CompilationUnitTree cut = JParserTestUtils.parse("class A {" +
      " Object foo(){" +
      "   if(a) { return new Object(); } " +
      "   else if(b){ return null; } " +
      "   else { " +
      "     if(c){ something(); } " +
      "   } " +
      " } " +
      "}");
    var methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    int nesting = new MethodNestingLevelVisitor().getMaxNestingLevel(methodTree);
    assertThat(nesting).isEqualTo(2);
  }
  
  @Test
  void ifHandling() {
    CompilationUnitTree cut = JParserTestUtils.parse("class A {" +
      " Object foo(){" +
      "   if(a) { return new Object(); } " +
      "   else { " +
      "     if(c){ something(); } " +
      "     else{  if(d){ return null; }  }"+
      "   } " +
      " } " +
      "}");
    var methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    int nesting = new MethodNestingLevelVisitor().getMaxNestingLevel(methodTree);
    assertThat(nesting).isEqualTo(3);
  }

  @Test
  void whileHandling() {
    CompilationUnitTree cut = JParserTestUtils.parse(
      "class A {" +
        "  Object foo(java.util.List<Integer> list){" +
        "    while(a) { " +
        "      do{" +
        "        list.forEach(elem -> { " +
        "          System.out.println(elem);" +
        "        });" +
        "      }" +
        "      while(list.size() > 10);" +
        "    }" +
        "  } " +
        "}");
    var methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    int nesting = new MethodNestingLevelVisitor().getMaxNestingLevel(methodTree);
    assertThat(nesting).isEqualTo(3);
  }

  @Test
  void forHandling() {
    CompilationUnitTree cut = JParserTestUtils.parse(
      "class A {" +
        "  Object foo(java.util.List<Integer> list){" +
        "    if(a) { " +
        "      for(int i : new int[]{1,2}) {" +
        "        for(int x =0; x < i; x++) { " +
        "          System.out.println(elem2);" +
        "        }" +
        "      }" +
        "      return new Object();   " +
        "    }" +
        "  } " +
        "}");
    var methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    int nesting = new MethodNestingLevelVisitor().getMaxNestingLevel(methodTree);
    assertThat(nesting).isEqualTo(3);
  }

  @Test
  void lambdaHandling() {
    CompilationUnitTree cut = JParserTestUtils.parse(
      "class A {" +
        "  Object foo(java.util.List<Integer> list, Stream<Object> objs){" +
        "    list.forEach( x -> {" +
        "      objs.forEach( o -> {" +
        "        System.out.println(o.toString());" +
        "      });" +
        "    });" +
        "  } " +
        "}");
    var methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    int nesting = new MethodNestingLevelVisitor().getMaxNestingLevel(methodTree);
    assertThat(nesting).isEqualTo(2);
  }

  @Test
  void switchHandling() {
    CompilationUnitTree cut = JParserTestUtils.parse(
      "class A {" +
        "  String foo(int a) {" +
        "    switch (a) {" +
        "      case 0:" +
        "        return \"none\";" +
        "      case 1:" +
        "        return \"one\";" +
        "      case 2:" +
        "        if(i==2){ return null; } else { return null; } " +
        "      default:" +
        "        return \"it's complicated\";" +
        "    }" +
        "  }" +
        "}");
    var methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    int nesting = new MethodNestingLevelVisitor().getMaxNestingLevel(methodTree);
    assertThat(nesting).isEqualTo(2);
  }

  @Test
  void tryHandling() {
    CompilationUnitTree cut = JParserTestUtils.parse("class A {" +
      " Object foo(){" +
      " try { " +
      "    if(a) { " +
      "      for(int i=0; i<3; i++) { if(i==2){ return null; } }" +
      "      return new Object();   " +
      "    };" +
      "    }catch(Exception e){}" +
      " } " +
      "}");
    var methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    int nesting = new MethodNestingLevelVisitor().getMaxNestingLevel(methodTree);
    assertThat(nesting).isEqualTo(4);
  }

  @Test
  void emptyMethod() {
    CompilationUnitTree cut = JParserTestUtils.parse("class A {" +
      " abstract Object foo();" +
      "}");
    var methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    int nesting = new MethodNestingLevelVisitor().getMaxNestingLevel(methodTree);
    assertThat(nesting).isZero();
  }
}
