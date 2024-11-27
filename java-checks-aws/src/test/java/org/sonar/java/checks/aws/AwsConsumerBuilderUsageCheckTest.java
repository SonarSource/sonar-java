/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.aws;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.CommonConstants.AWS_CLASSPATH;
import static org.sonar.java.checks.CommonConstants.AWS_MODULE;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPathInModule;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPathInModule;

class AwsConsumerBuilderUsageCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPathInModule(AWS_MODULE, "checks/aws/AwsConsumerBuilderUsageCheckSample.java"))
      .withCheck(new AwsConsumerBuilderUsageCheck())
      .withClassPath(AWS_CLASSPATH)
      .verifyIssues();
  }

  @Test
  void non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPathInModule(AWS_MODULE, "checks/aws/AwsConsumerBuilderUsageCheckSample.java"))
      .withCheck(new AwsConsumerBuilderUsageCheck())
      .withClassPath(AWS_CLASSPATH)
      .verifyIssues();
  }

}
