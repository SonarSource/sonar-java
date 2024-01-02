/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
  void anonymousClassHandling() throws Exception {
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
  void ifElseHandling() throws Exception {
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
  void ifHandling() throws Exception {
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
  void whileHandling() throws Exception {
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
  void forHandling() throws Exception {
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
  void lambdaHandling() throws Exception {
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
  void switchHandling() throws Exception {
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
  void tryHandling() throws Exception {
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
  void emptyMethod() throws Exception {
    CompilationUnitTree cut = JParserTestUtils.parse("class A {" +
      " abstract Object foo();" +
      "}");
    var methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    int nesting = new MethodNestingLevelVisitor().getMaxNestingLevel(methodTree);
    assertThat(nesting).isZero();
  }
}
