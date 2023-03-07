/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.jsp;


import java.io.IOException;
import java.nio.file.Path;
import org.apache.jasper.TrimSpacesOption;
import org.apache.jasper.servlet.JspCServletContext;
import org.apache.jasper.servlet.TldScanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.java.AnalysisException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class JasperOptionsTest {

  @TempDir
  Path baseDir;
  @TempDir
  Path output;

  @Test
  void test() throws Exception {
    JspCServletContext servletContext = new Jasper.ServletContext(baseDir.toUri().toURL(), this.getClass().getClassLoader());
    JasperOptions options = new JasperOptions(servletContext, output);

    assertThat(options.getErrorOnUseBeanInvalidClassAttribute()).isFalse();
    assertThat(options.getKeepGenerated()).isTrue();
    assertThat(options.isPoolingEnabled()).isFalse();
    assertThat(options.getMappedFile()).isFalse();
    assertThat(options.getClassDebugInfo()).isFalse();
    assertThat(options.getCheckInterval()).isZero();
    assertThat(options.getDevelopment()).isFalse();
    assertThat(options.getDisplaySourceFragment()).isFalse();
    assertThat(options.isSmapDumped()).isTrue();
    assertThat(options.isSmapSuppressed()).isFalse();
    assertThat(options.getTrimSpaces()).isEqualTo(TrimSpacesOption.FALSE);
    assertThat(options.getIeClassId()).isEqualTo(JasperOptions.DEFAULT_IE_CLASS_ID);
    assertThat(options.getScratchDir()).isEqualTo(output.toFile());
    assertThat(options.getClassPath()).isNull();
    assertThat(options.getCompiler()).isNull();
    assertThat(options.getCompilerTargetVM()).isNull();
    assertThat(options.getCompilerSourceVM()).isNull();
    assertThat(options.getCompilerClassName()).isNull();
    assertThat(options.getTldCache()).isNotNull();
    assertThat(options.getJavaEncoding()).isEqualTo("UTF-8");
    assertThat(options.getFork()).isFalse();
    assertThat(options.getJspConfig()).isNotNull();
    assertThat(options.isXpoweredBy()).isFalse();
    assertThat(options.getTagPluginManager()).isNotNull();
    assertThat(options.genStringAsCharArray()).isFalse();
    assertThat(options.getModificationTestInterval()).isZero();
    assertThat(options.getRecompileOnFail()).isFalse();
    assertThat(options.isCaching()).isFalse();
    assertThat(options.getCache()).isEmpty();
    assertThat(options.getMaxLoadedJsps()).isZero();
    assertThat(options.getJspIdleTimeout()).isZero();
    assertThat(options.getStrictQuoteEscaping()).isFalse();
    assertThat(options.getQuoteAttributeEL()).isFalse();
  }

  @Test
  void test_failure() throws Exception {
    TldScanner tldScanner = mock(TldScanner.class);
    JspCServletContext servletContext = new Jasper.ServletContext(baseDir.toUri().toURL(), this.getClass().getClassLoader());
    doThrow(new IOException()).when(tldScanner).scan();
    assertThatThrownBy(() -> new JasperOptions(servletContext, output, tldScanner)).isInstanceOf(AnalysisException.class);
  }


}
