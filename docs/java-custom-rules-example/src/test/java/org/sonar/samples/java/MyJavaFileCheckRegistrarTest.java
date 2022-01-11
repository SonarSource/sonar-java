/*
 * Copyright (C) 2012-2022 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
 */
package org.sonar.samples.java;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.CheckRegistrar;
import static org.assertj.core.api.Assertions.assertThat;

class MyJavaFileCheckRegistrarTest {

  @Test
  void checkNumberRules() {
    CheckRegistrar.RegistrarContext context = new CheckRegistrar.RegistrarContext();

    MyJavaFileCheckRegistrar registrar = new MyJavaFileCheckRegistrar();
    registrar.register(context);

    assertThat(context.checkClasses()).hasSize(8);
    assertThat(context.testCheckClasses()).hasSize(1);
  }

}
