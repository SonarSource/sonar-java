/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaResourceLocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class StrutsTest {

  private static SensorContextTester context;

  private static void initAndScan() {
    File prjDir = new File("target/test-projects/struts-core-1.3.9");
    File srcDir = new File(prjDir, "src");

    context = SensorContextTester.create(prjDir);
    DefaultFileSystem fs = context.fileSystem();
    Collection<File> files = FileUtils.listFiles(srcDir, new String[]{"java"}, true);
    for (File file : files) {
      fs.add(new TestInputFileBuilder("",file.getPath()).build());
    }
    Measurer measurer = new Measurer(fs, context, mock(NoSonarFilter.class));
    JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);
    JavaSquid squid = new JavaSquid(new JavaVersionImpl(), null, measurer, javaResourceLocator, null, new JavaCheck[0]);
    squid.scan(files, Collections.<File>emptyList());
  }

  private Map<String, Double> getMetrics() {
    Map<String, Double> metrics = new HashMap<>();
    for (InputFile inputFile : context.fileSystem().inputFiles()) {
      for (Measure measure : context.measures(inputFile.key())) {
        if (measure.value() != null) {
          String key = measure.metric().key();
          double value = 0;
          try {
            value = Double.parseDouble("" + measure.value());
          } catch (NumberFormatException nfe) {
            //do nothing
          }
          if (metrics.get(key) == null) {
            metrics.put(key, value);
          } else {
            metrics.put(key, metrics.get(key) + value);
          }
        }
      }
    }
    return metrics;
  }

  @Test
  public void measures_on_project() throws Exception {
    initAndScan();
    Map<String, Double> metrics = getMetrics();

    assertThat(metrics.get("classes").intValue()).isEqualTo(146);
    assertThat(metrics.get("ncloc").intValue()).isEqualTo(14007);
    assertThat(metrics.get("statements").intValue()).isEqualTo(6403 /* empty statements between members of class */+ 3);
    assertThat(metrics.get("comment_lines").intValue()).isEqualTo(7605);
    assertThat(metrics.get("functions").intValue()).isEqualTo(1429);
    assertThat(metrics.get("complexity").intValue()).isEqualTo(3055);
  }

}
