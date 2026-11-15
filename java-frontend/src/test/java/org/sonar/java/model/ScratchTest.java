package org.sonar.java.model;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.declaration.*;
import org.sonar.plugins.java.api.semantic.*;

class ScratchTest {
  @Test
  void t() {
    var cu = JParserTestUtils.parse("class C { <T> void m(T p) { m(1); } }");
    var c = (ClassTreeImpl) cu.types().get(0);
    var m = (MethodTreeImpl) c.members().get(0);
    System.out.println("SCR param type " + m.symbol().parameterTypes().get(0) + " / " + m.parameters().get(0).type().symbolType() + " id " + ((JType) m.symbol().parameterTypes().get(0)).fullyQualifiedName());
    JType a = (JType) m.symbol().parameterTypes().get(0);
    JType b = (JType) m.parameters().get(0).type().symbolType();
    System.out.println("SCR eq " + a.equals(b) + " " + a.isTypeVar() + b.isTypeVar() + " sym " + a.symbol() + " " + b.symbol());
  }
}
