/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;


@Rule(key = "sxxx")
public class ParametrizedTestArgumentCheck extends IssuableSubscriptionVisitor {

  private String[] singleVariableAnnotations = {
    "ValueSource",
    "NullSource",
    "EmptySource",
    "NullAndEmptySource",
    "EnumSource",
    "CsvSource",
    "FieldSource",
    "ArgumentSource"
  };

  private String[] multipleVariablesAnnotations = {
    "CsvSource",
    "CsvFileSource",
    "MethodSource"
  };

  @Override
  public List<Tree.Kind> nodesToVisit() {
    // TODO: Specify the kind of nodes you want to be called to visit here.
    return Arrays.asList(Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {

    if (!tree.is(Kind.METHOD)) {
      return;
    }

    var method = (MethodTree) tree;
    var annotations = new ArrayList<String>(); // method.getAnnotations

//    if (singleAnnotation){
//    // 1. First do simple case with a single parameter

//      //check that the test has only 1 parameter
//    }else if(variableAnnotation){
//     // 2. Handle case with multiple parameters (with the csvSource)

//      // check that the test has the same amount of parameter as it should
//    }else if( singleAndMultiple){
//     // 3. Handle case with conflicting parameters size (test 5.2)
//      // report
//    }
    throw new UnsupportedOperationException("Not implemented yet");

    // On Method nodes, check for the annotations
    // If it has not the @ParameterizedTest annotation, return
    // else, for each possible parameter, check that it is used in the method definition
    //    @ValueSource
    //    @NullSource
    //    @EmptySource
    //    @NullAndEmptySource
    //    @EnumSource
    //    @CsvSource
    //    @CsvFileSource
    //    @MethodSource
    //    @FieldSource
    //    @ArgumentSource


  }

}
