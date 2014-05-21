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

import com.google.common.collect.Lists;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class BytecodeCompleterTest {

  @Test
  public void completing_symbol_ArrayList() throws Exception {
    BytecodeCompleter bytecodeCompleter = new BytecodeCompleter();
    BytecodeCompleter.PROJECT_CLASSPATH = Lists.newArrayList(new File("target/test-classes"), new File("target/classes"));
    Symbol.PackageSymbol  util = bytecodeCompleter.enterPackage("java.util");
    Symbol.TypeSymbol arrayList = new Symbol.TypeSymbol(Flags.PUBLIC, "ArrayList", util);
    bytecodeCompleter.complete(arrayList);
    //Check supertype
    assertThat(arrayList.getSuperclass().symbol.name).isEqualTo("AbstractList");
    assertThat(arrayList.getSuperclass().symbol.owner().name).isEqualTo(util.name);
    assertThat(arrayList.getSuperclass().symbol.owner()).isEqualTo(util);

    //Check interfaces
    assertThat(arrayList.getInterfaces()).hasSize(4);
    List<String> interfacesName = Lists.newArrayList();
    for(Type interfaceType : arrayList.getInterfaces()) {
      interfacesName.add(interfaceType.symbol.name);
    }
    assertThat(interfacesName).hasSize(4);
    assertThat(interfacesName).contains("List", "RandomAccess", "Cloneable", "Serializable");
  }

}
