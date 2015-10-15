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
package org.sonar.java;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.JavaSquid;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class JavaVersionAwareVisitorTest {

  @Test
  public void all_check_executed_when_no_java_version() {
    List<String> messages = checkIssues(new JavaConfiguration(Charsets.UTF_8));
    assertThat(messages).hasSize(3);
    assertThat(messages).containsOnly("JavaVersionCheck_7", "JavaVersionCheck_8", "SimpleCheck");
  }

  @Test
  public void all_check_executed_when_invalid_java_version() {
    JavaConfiguration conf = new JavaConfiguration(Charsets.UTF_8);
    conf.setJavaVersion(null);
    List<String> messages = checkIssues(conf);
    assertThat(messages).hasSize(3);
    assertThat(messages).containsOnly("JavaVersionCheck_7", "JavaVersionCheck_8", "SimpleCheck");
  }

  @Test
  public void only_checks_with_adequate_java_version_higher_than_configuration_version_are_executed() {
    JavaConfiguration conf = new JavaConfiguration(Charsets.UTF_8);
    conf.setJavaVersion(7);
    List<String> messages = checkIssues(conf);
    assertThat(messages).hasSize(2);
    assertThat(messages).containsOnly("JavaVersionCheck_7", "SimpleCheck");

    conf.setJavaVersion(8);
    messages = checkIssues(conf);
    assertThat(messages).hasSize(3);
    assertThat(messages).containsOnly("JavaVersionCheck_7", "JavaVersionCheck_8", "SimpleCheck");
  }

  @Test
  public void no_java_version_matching() {
    JavaConfiguration conf = new JavaConfiguration(Charsets.UTF_8);
    conf.setJavaVersion(6);
    List<String> messages = checkIssues(conf);
    assertThat(messages).hasSize(1);
    assertThat(messages).containsOnly("SimpleCheck");
  }

  private static List<String> checkIssues(JavaConfiguration conf) {
    List<String> messages = Lists.newArrayList();
    JavaCheck[] javaChecks = new JavaCheck[] {
      new JavaVersionCheck(7, messages),
      new JavaVersionCheck(8, messages),
      new SimpleCheck(messages)
    };

    ArrayList<File> files = Lists.newArrayList(new File("src/test/files/JavaVersionAwareChecks.java"));

    JavaSquid squid = new JavaSquid(conf, null, null, null, javaChecks);
    squid.scan(files, Collections.<File>emptyList(), Collections.<File>emptyList());

    return messages;
  }


  private static class SimpleCheck extends IssuableSubscriptionVisitor {
    private final List<String> messages;

    public SimpleCheck(List<String> messages) {
      this.messages = messages;
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return ImmutableList.of(Tree.Kind.CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
      messages.add(getName());
    }

    public String getName() {
      return this.getClass().getSimpleName().toString();
    }
  }

  private static class JavaVersionCheck extends SimpleCheck implements JavaVersionAwareVisitor {

    private final Integer target;

    private JavaVersionCheck(Integer target, List<String> messages) {
      super(messages);
      this.target = target;
    }

    @Override
    public boolean isCompatibleWithJavaVersion(Integer version) {
      return target <= version;
    }

    @Override
    public String getName() {
      return super.getName() + "_" + target;
    }
  }
}
