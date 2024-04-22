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

package org.sonar.java.checks.quickfixes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.sonar.java.checks.quickfixes.Ast.*;

public final class Syntax {
  private Syntax(){
  }

  public static VarDecl Decl(String typeOrVar, String varName){
    return new VarDecl(typeOrVar, varName, Optional.empty());
  }

  public static VarDecl Decl(String typeOrVar, String varName, Expression initializerExpr){
    return new VarDecl(typeOrVar, varName, Optional.of(initializerExpr));
  }

  public static IfStat If(Expression condition, Block thenBr){
    return new IfStat(condition, thenBr, Optional.empty());
  }

  public static IfStat If(Expression condition, Block thenBr, ElseBranchStat elseBr){
    return new IfStat(condition, thenBr, Optional.of(elseBr));
  }

  public static Switch Switch(Expression scrutinee, Case... cases){
    return new Switch(scrutinee, Arrays.asList(cases));
  }

  public static Case Case(Pattern pattern, Ast body){
    return new Case(Optional.of(pattern), body);
  }

  public static Case Default(Ast body){
    return new Case(Optional.empty(), body);
  }

  public static ValuePattern Pat(String value){
    return new ValuePattern(value);
  }

  public static VariablePattern Pat(String typeOrVar, String varName){
    return new VariablePattern(typeOrVar, varName);
  }

  public static RecordPattern Pat(String recordName, Pattern... fields){
    return new RecordPattern(recordName, List.of(), Arrays.asList(fields));
  }

  public static RecordPattern Pat(String recordName, List<String> typeVars, Pattern... fields){
    return new RecordPattern(recordName, typeVars, Arrays.asList(fields));
  }

  public static Block Block(Statement... stats){
    return new Block(Arrays.asList(stats));
  }

  public static Const cst(Object cst){
    return new Const(cst);
  }

  public static HardCodedStat stat(String code){
    return new HardCodedStat(code);
  }

  public static HardCodedExpr expr(String code){
    return new HardCodedExpr(code);
  }

}
