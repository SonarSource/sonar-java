/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import jakarta.servlet.jsp.tagext.TagLibraryInfo;
import org.apache.jasper.Options;
import org.apache.jasper.TrimSpacesOption;
import org.apache.jasper.compiler.JspConfig;
import org.apache.jasper.compiler.TagPluginManager;
import org.apache.jasper.compiler.TldCache;
import org.apache.jasper.servlet.JspCServletContext;
import org.apache.jasper.servlet.TldScanner;
import org.sonar.java.AnalysisException;
import org.xml.sax.SAXException;

/**
 * This class is replacing {@link org.apache.jasper.JspC} to avoid dependency on Ant. Most of the initialization is
 * copied from JspC class which can be used as reference. Many of the methods are not actually used and are needed only
 * to implement the interface.
 */
class JasperOptions implements Options {

  static final String DEFAULT_IE_CLASS_ID = "clsid:8AD9C840-044E-11D1-B3E9-00805F499D93";

  private final Path outputDir;
  private final TagPluginManager tagPluginManager;
  private final TldCache tldCache;
  private final JspConfig jspConfig;

  JasperOptions(JspCServletContext context, Path outputDir) {
    this(context, outputDir, new TldScanner(context, false, false, true));
  }

  JasperOptions(JspCServletContext context, Path outputDir, TldScanner tldScanner) {
    this.outputDir = outputDir;
    tagPluginManager = new TagPluginManager(context);
    tldCache = initTldCache(context, tldScanner);
    jspConfig = new JspConfig(context);
  }

  private static TldCache initTldCache(JspCServletContext context, TldScanner tldScanner) {
    try {
      tldScanner.scan();
      return new TldCache(context, tldScanner.getUriTldResourcePathMap(),
        tldScanner.getTldResourcePathTaglibXmlMap());
    } catch (SAXException | IOException e) {
      throw new AnalysisException("Error scanning for TLD", e);
    }
  }

  @Override
  public boolean getErrorOnUseBeanInvalidClassAttribute() {
    return false;
  }

  @Override
  public boolean getKeepGenerated() {
    return true;
  }

  @Override
  public boolean isPoolingEnabled() {
    return false;
  }

  @Override
  public boolean getMappedFile() {
    return false;
  }

  @Override
  public boolean getClassDebugInfo() {
    return false;
  }

  @Override
  public int getCheckInterval() {
    return 0;
  }

  @Override
  public boolean getDevelopment() {
    return false;
  }

  @Override
  public boolean getDisplaySourceFragment() {
    return false;
  }

  @Override
  public boolean isSmapSuppressed() {
    return false;
  }

  @Override
  public boolean isSmapDumped() {
    return true;
  }

  @Override
  public TrimSpacesOption getTrimSpaces() {
    return TrimSpacesOption.FALSE;
  }

  public String getIeClassId() {
    return DEFAULT_IE_CLASS_ID;
  }

  @Override
  public File getScratchDir() {
    return outputDir.toFile();
  }

  @Override
  public String getClassPath() {
    return null;
  }

  @Override
  public String getCompiler() {
    return null;
  }

  @Override
  public String getCompilerTargetVM() {
    return null;
  }

  @Override
  public String getCompilerSourceVM() {
    return null;
  }

  @Override
  public String getCompilerClassName() {
    return null;
  }

  @Override
  public TldCache getTldCache() {
    return tldCache;
  }

  @Override
  public String getJavaEncoding() {
    return StandardCharsets.UTF_8.name();
  }

  @Override
  public boolean getFork() {
    return false;
  }

  @Override
  public JspConfig getJspConfig() {
    return jspConfig;
  }

  @Override
  public boolean isXpoweredBy() {
    return false;
  }

  @Override
  public TagPluginManager getTagPluginManager() {
    return tagPluginManager;
  }

  @Override
  public boolean genStringAsCharArray() {
    return false;
  }

  @Override
  public int getModificationTestInterval() {
    return 0;
  }

  @Override
  public boolean getRecompileOnFail() {
    return false;
  }

  @Override
  public boolean isCaching() {
    return false;
  }

  @Override
  public Map<String, TagLibraryInfo> getCache() {
    return Collections.emptyMap();
  }

  @Override
  public int getMaxLoadedJsps() {
    return 0;
  }

  @Override
  public int getJspIdleTimeout() {
    return 0;
  }

  @Override
  public boolean getStrictQuoteEscaping() {
    return false;
  }

  @Override
  public boolean getQuoteAttributeEL() {
    return false;
  }
}
