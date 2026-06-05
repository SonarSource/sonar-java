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
package org.sonar.java.model.springcontext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SpringContextModelTest {

  @Test
  void testInitialization() {
    SpringContextModel model = new SpringContextModel();

    assertNotNull(model.getBeanDefinitionRegistry(), "BeanDefinitionRegistry should be initialized");
    assertNotNull(model.getProjectPackageScan(), "ProjectPackageScan should be initialized");
    assertNotNull(model.getTypeToBeanNamesIndex(), "TypeToBeanNamesIndex should be initialized");
    assertNotNull(model.getEntityClassToPropertiesIndex(), "EntityClassToPropertiesIndex should be initialized");
  }

}
