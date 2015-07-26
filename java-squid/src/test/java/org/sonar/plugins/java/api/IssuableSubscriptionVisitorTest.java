/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.plugins.java.api;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.JavaSquid;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.indexer.QueryByType;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class IssuableSubscriptionVisitorTest {


  @Test
  public void test_custom_rules_report_issues() throws Exception {

    JavaSquid javaSquid = new JavaSquid(new JavaConfiguration(Charsets.UTF_8), Mockito.mock(JavaResourceLocator.class), new CustomRule());
    javaSquid.scan(Lists.newArrayList(new File("src/test/resources/IssuableSubscriptionClass.java")), Lists.<File>newArrayList(), Lists.<File>newArrayList());
    Collection<SourceCode> sourceCodes = javaSquid.search(new QueryByType(SourceFile.class));
    assertThat(sourceCodes).hasSize(1);
    SourceCode sourceCode = sourceCodes.iterator().next();
    assertThat(sourceCode.getCheckMessages()).hasSize(3);
  }

  private static class CustomRule extends IssuableSubscriptionVisitor {

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return ImmutableList.of(Tree.Kind.COMPILATION_UNIT);
    }

    @Override
    public void visitNode(Tree tree) {
      addIssue(tree, "issue on tree");
      addIssue(1, "issue on 1st line");
      addIssueOnFile("issue on file");
    }
  }
}