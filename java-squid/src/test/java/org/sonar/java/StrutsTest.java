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
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.measures.Measure;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.squidbridge.api.CodeVisitor;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class StrutsTest {

  private static SensorContext context;

  private void initAndScan(boolean separateAccessorsFromMethods) {
    File prjDir = new File("target/test-projects/struts-core-1.3.9");
    File srcDir = new File(prjDir, "src");
    File binDir = new File(prjDir, "bin");

    JavaConfiguration conf = new JavaConfiguration(Charsets.UTF_8);
    conf.setSeparateAccessorsFromMethods(separateAccessorsFromMethods);
    context = mock(SensorContext.class);
    DefaultFileSystem fs = new DefaultFileSystem(prjDir);
    Collection<File> files = FileUtils.listFiles(srcDir, new String[]{"java"}, true);
    for (File file : files) {
      fs.add(new DefaultInputFile(file.getPath()));
    }
    Measurer measurer = new Measurer(fs, context, separateAccessorsFromMethods);
    JavaSquid squid = new JavaSquid(conf, null, measurer, mock(JavaResourceLocator.class), new CodeVisitor[0]);
    squid.scan(files, Collections.<File>emptyList(), Collections.singleton(binDir));
  }

  private Map<String, Double> getMetrics() {
    ArgumentCaptor<Measure> captor = ArgumentCaptor.forClass(Measure.class);
    verify(context, atLeastOnce()).saveMeasure(any(InputFile.class), captor.capture());
    Map<String, Double> metrics = new HashMap<String, Double>();
    for (Measure measure : captor.getAllValues()) {
      if (measure.getValue() != null) {
        if (metrics.get(measure.getMetricKey()) == null) {
          metrics.put(measure.getMetricKey(), measure.getValue());
        } else {
          metrics.put(measure.getMetricKey(), metrics.get(measure.getMetricKey()) + measure.getValue());
        }
      }
    }
    return metrics;
  }

  private void verifySameResults(Map<String, Double> metrics) {
    assertThat(metrics.get("classes").intValue()).isEqualTo(146);
    assertThat(metrics.get("lines").intValue()).isEqualTo(32878);
    assertThat(metrics.get("ncloc").intValue()).isEqualTo(14007);
    assertThat(metrics.get("statements").intValue()).isEqualTo(6403 /* empty statements between members of class */+ 3);
    assertThat(metrics.get("comment_lines").intValue()).isEqualTo(7605);
  }

  @Test
  public void measures_on_project_accessors_separated_from_methods() throws Exception {
    initAndScan(true);
    Map<String, Double> metrics = getMetrics();

    verifySameResults(metrics);

    // 48: SONARJAVA-861 separatedAccessorsFromMethods property of the measurer is set to true. Getters and setters ignored.
    assertThat(metrics.get("public_api").intValue()).isEqualTo(1340 - 48);
    // 56 methods in anonymous classes: not part of metric but part of number of methods in project.
    assertThat(metrics.get("functions").intValue()).isEqualTo(1429 - 56 + 8);
    assertThat(metrics.get("complexity").intValue()).isEqualTo(3859 - 145 /* SONAR-3793 */- 1 /* SONAR-3794 */+ 98 /* SONARJAVA-861 */);
  }

  @Test
  public void measures_on_project_accessors_handled_as_methods() throws Exception {
    initAndScan(false);
    Map<String, Double> metrics = getMetrics();

    verifySameResults(metrics);

    assertThat(metrics.get("public_api").intValue()).isEqualTo(1340);
    assertThat(metrics.get("functions").intValue()).isEqualTo(1429);
    assertThat(metrics.get("complexity").intValue()).isEqualTo(3859);
  }

}
