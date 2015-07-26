/*
 * $Id: TestTilesPlugin.java 471754 2006-11-06 14:55:09Z husted $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.struts.tiles;

import org.apache.struts.config.ModuleConfig;
import org.apache.struts.config.ModuleConfigFactory;
import org.apache.struts.config.PlugInConfig;
import org.apache.struts.mock.MockActionServlet;
import org.apache.struts.mock.TestMockBase;
import org.apache.struts.Globals;
import org.apache.struts.tiles.xmlDefinition.I18nFactorySet;
import org.apache.struts.util.RequestUtils;
import org.apache.struts.action.PlugIn;
import org.apache.commons.beanutils.BeanUtils;
import junit.framework.Test;
import junit.framework.TestSuite;

import javax.servlet.ServletException;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class TestTilesPlugin extends TestMockBase {


  protected ModuleConfig module1;
  protected ModuleConfig module2;
  protected MockActionServlet actionServlet;

    // ----------------------------------------------------------------- Basics


    public TestTilesPlugin(String name) {
        super(name);
    }


    public static void main(String args[]) {
        junit.awtui.TestRunner.main
            (new String[] { TestTilesPlugin.class.getName() } );
    }


    public static Test suite() {
        return (new TestSuite(TestTilesPlugin.class));
    }


    // ----------------------------------------------------- Instance Variables



    // ----------------------------------------------------- Setup and Teardown


    public void setUp()
    {

    super.setUp();
    TilesUtil.testReset();
    actionServlet = new MockActionServlet(context, config);
    }


    public void tearDown() {

        super.tearDown();

    }


    // ------------------------------------------------------- Individual Tests


    /**
     * Create a module configuration
     * @param moduleName
     */
    public ModuleConfig createModuleConfig(
        String moduleName,
        String configFileName,
        boolean moduleAware) {

        ModuleConfig moduleConfig =
            ModuleConfigFactory.createFactory().createModuleConfig(moduleName);

        context.setAttribute(Globals.MODULE_KEY + moduleName, moduleConfig);

        // Set tiles plugin
        PlugInConfig pluginConfig = new PlugInConfig();
        pluginConfig.setClassName("org.apache.struts.tiles.TilesPlugin");

        pluginConfig.addProperty(
            "moduleAware",
            (moduleAware == true ? "true" : "false"));

        pluginConfig.addProperty(
            "definitions-config",
            "/org/apache/struts/tiles/config/" + configFileName);

        moduleConfig.addPlugInConfig(pluginConfig);
        return moduleConfig;
    }

    /**
     * Fake call to init module plugins
     * @param moduleConfig
     */
  public void initModulePlugIns( ModuleConfig moduleConfig)
  {
  PlugInConfig plugInConfigs[] = moduleConfig.findPlugInConfigs();
  PlugIn plugIns[] = new PlugIn[plugInConfigs.length];

  context.setAttribute(Globals.PLUG_INS_KEY + moduleConfig.getPrefix(), plugIns);
  for (int i = 0; i < plugIns.length; i++) {
      try {
          plugIns[i] =
              (PlugIn) RequestUtils.applicationInstance(plugInConfigs[i].getClassName());
          BeanUtils.populate(plugIns[i], plugInConfigs[i].getProperties());
            // Pass the current plugIn config object to the PlugIn.
            // The property is set only if the plugin declares it.
            // This plugin config object is needed by Tiles
          BeanUtils.copyProperty( plugIns[i], "currentPlugInConfigObject", plugInConfigs[i]);
          plugIns[i].init(actionServlet, moduleConfig);
      } catch (ServletException e) {
          // Lets propagate
          e.printStackTrace();
          //throw e;
      } catch (Exception e) {
          e.printStackTrace();
          //throw e;
      }
  }
  }

    // ---------------------------------------------------------- absoluteURL()


    /**
     * Test multi factory creation when moduleAware=true.
     */
    public void testMultiFactory() {
        // init TilesPlugin
        module1 = createModuleConfig("/module1", "tiles-defs.xml", true);
        module2 = createModuleConfig("/module2", "tiles-defs.xml", true);
        initModulePlugIns(module1);
        initModulePlugIns(module2);

        // mock request context
        request.setAttribute(Globals.MODULE_KEY, module1);
        request.setPathElements("/myapp", "/module1/foo.do", null, null);
        // Retrieve factory for module1
        DefinitionsFactory factory1 =
            TilesUtil.getDefinitionsFactory(request, context);

        assertNotNull("factory found", factory1);
        assertEquals(
            "factory name",
            "/module1",
            factory1.getConfig().getFactoryName());

        // mock request context
        request.setAttribute(Globals.MODULE_KEY, module2);
        request.setPathElements("/myapp", "/module2/foo.do", null, null);
        // Retrieve factory for module2
        DefinitionsFactory factory2 =
            TilesUtil.getDefinitionsFactory(request, context);
        assertNotNull("factory found", factory2);
        assertEquals(
            "factory name",
            "/module2",
            factory2.getConfig().getFactoryName());

        // Check that factory are different
        assertNotSame("Factory from different modules", factory1, factory2);
    }

    /**
     * Test single factory creation when moduleAware=false.
     */
  public void testSingleSharedFactory()
  {
    // init TilesPlugin
  module1 = createModuleConfig( "/module1", "tiles-defs.xml", false );
  module2 = createModuleConfig( "/module2", "tiles-defs.xml", false );
  initModulePlugIns(module1);
  initModulePlugIns(module2);

    // mock request context
  request.setAttribute(Globals.MODULE_KEY, module1);
  request.setPathElements("/myapp", "/module1/foo.do", null, null);
    // Retrieve factory for module1
  DefinitionsFactory factory1 = TilesUtil.getDefinitionsFactory( request, context);
  assertNotNull( "factory found", factory1);
  assertEquals( "factory name", "/module1", factory1.getConfig().getFactoryName() );

    // mock request context
  request.setAttribute(Globals.MODULE_KEY, module2);
  request.setPathElements("/myapp", "/module2/foo.do", null, null);
    // Retrieve factory for module2
  DefinitionsFactory factory2 = TilesUtil.getDefinitionsFactory( request, context);
  assertNotNull( "factory found", factory2);
  assertEquals( "factory name", "/module1", factory2.getConfig().getFactoryName() );

    // Check that factory are different
  assertEquals("Same factory", factory1, factory2);
  }

  /**
   * Test I18nFactorySet.
   */
  public void testI18FactorySet_A() {

     Locale locale = null;
     ComponentDefinition definition = null;
     org.apache.struts.tiles.xmlDefinition.DefinitionsFactory factory = null;

     Map properties = new HashMap();

     // Set the file name
     properties.put(I18nFactorySet.DEFINITIONS_CONFIG_PARAMETER_NAME,
                    "config/I18nFactorySet-A.xml");

     try {
         CustomI18nFactorySet i18nFactorySet = new CustomI18nFactorySet(context, properties);
         String defName = "A-DEFAULT";

         // Default Locale
         locale = new Locale("", "", "");
         factory = i18nFactorySet.createFactory(locale , request, context);
         assertNotNull("DefinitionsFactory is nullfor locale='" + print(locale) + "'", factory);
         definition = factory.getDefinition(defName, request, context);
         assertNotNull("Definition '" + defName + "' Not Found for locale='" + print(locale) + "'", definition);
         assertEquals("Definition '" + defName + "' for locale='" + print(locale) + "'", defName, definition.getName());

         // Variant Only
         locale = new Locale("", "", "XX");
         factory = i18nFactorySet.createFactory(locale , request, context);
         assertNotNull("DefinitionsFactory is null for locale='" + print(locale) + "'", factory);
         definition = factory.getDefinition(defName, request, context);
         assertNotNull("Definition '" + defName + "' Not Found for locale='" + print(locale) + "'", definition);
         assertEquals("Definition '" + defName + "' for locale='" + print(locale) + "'", defName, definition.getName());

         // No Language, Country & Variant Locale
         locale = new Locale("", "US", "XX");
         factory = i18nFactorySet.createFactory(locale , request, context);
         assertNotNull("DefinitionsFactory is null for locale='" + print(locale) + "'", factory);
         definition = factory.getDefinition(defName, request, context);
         assertNotNull("Definition '" + defName + "' Not Found for locale='" + print(locale) + "'", definition);
         assertEquals("Definition '" + defName + "' for locale='" + print(locale) + "'", defName, definition.getName());

         // Language & Country
         locale = new Locale("en", "US");
         factory = i18nFactorySet.createFactory(locale , request, context);
         assertNotNull("DefinitionsFactory is null for locale='" + print(locale) + "'", factory);
         definition = factory.getDefinition(defName, request, context);
         assertNotNull("Definition '" + defName + "' Not Found for locale='" + print(locale) + "'", definition);
         assertEquals("Definition '" + defName + "' for locale='" + print(locale) + "'", defName, definition.getName());

     } catch(Exception ex) {
         fail(ex.toString());
     }
  }


  /**
   * Test I18nFactorySet.
   */
  public void testI18FactorySet_B() {

     Locale locale = null;
     ComponentDefinition definition = null;
     org.apache.struts.tiles.xmlDefinition.DefinitionsFactory factory = null;

     Map properties = new HashMap();

     // Set the file name
     properties.put(I18nFactorySet.DEFINITIONS_CONFIG_PARAMETER_NAME,
                    "config/I18nFactorySet-B.xml");

     try {

         CustomI18nFactorySet i18nFactorySet = new CustomI18nFactorySet(context, properties);
         String defName = null;

         // Default Locale
         locale = new Locale("", "", "");
         factory = i18nFactorySet.createFactory(locale , request, context);
         assertNotNull("1. DefinitionsFactory is nullfor locale='" + print(locale) + "'", factory);
         defName = "B-DEFAULT";
         definition = factory.getDefinition(defName, request, context);
         assertNotNull("2. Definition '" + defName + "' Not Found for locale='" + print(locale) + "'", definition);
         assertEquals("3. Definition '" + defName + "' for locale='" + print(locale) + "'", defName, definition.getName());

         // Variant Only
         locale = new Locale("", "", "XX");
         factory = i18nFactorySet.createFactory(locale , request, context);
         assertNotNull("4. DefinitionsFactory is null for locale='" + print(locale) + "'", factory);
         defName = "B___XX";
         definition = factory.getDefinition(defName, request, context);
         assertNotNull("5. Definition '" + defName + "' Not Found for locale='" + print(locale) + "'", definition);
         assertEquals("6. Definition '" + defName + "' for locale='" + print(locale) + "'", defName, definition.getName());
         defName = "B-DEFAULT";
         definition = factory.getDefinition(defName, request, context);
         assertNotNull("7. Definition '" + defName + "' Not Found for locale='" + print(locale) + "'", definition);
         assertEquals("8. Definition '" + defName + "' for locale='" + print(locale) + "'", defName, definition.getName());

         // No Language, Country & Unknown Variant
         locale = new Locale("", "US", "XX");
         factory = i18nFactorySet.createFactory(locale , request, context);
         assertNotNull("9. DefinitionsFactory is null for locale='" + print(locale) + "'", factory);
         defName = "B__US";
         definition = factory.getDefinition(defName, request, context);
         assertNotNull("10. Definition '" + defName + "' Not Found for locale='" + print(locale) + "'", definition);
         assertEquals("11. Definition '" + defName + "' for locale='" + print(locale) + "'", defName, definition.getName());

         // Language & Country
         locale = new Locale("en", "US");
         factory = i18nFactorySet.createFactory(locale , request, context);
         assertNotNull("12. DefinitionsFactory is null for locale='" + print(locale) + "'", factory);
         defName = "B_en_US";
         definition = factory.getDefinition(defName, request, context);
         assertNotNull("13. Definition '" + defName + "' Not Found for locale='" + print(locale) + "'", definition);
         assertEquals("14. Definition '" + defName + "' for locale='" + print(locale) + "'", defName, definition.getName());

         // Language, Country & Unknown Variant
         locale = new Locale("en", "GB", "XX");
         factory = i18nFactorySet.createFactory(locale , request, context);
         assertNotNull("15. DefinitionsFactory is null for locale='" + print(locale) + "'", factory);
         defName = "B_en_GB";
         definition = factory.getDefinition(defName, request, context);
         assertNotNull("16. Definition '" + defName + "' Not Found for locale='" + print(locale) + "'", definition);
         assertEquals("17. Definition '" + defName + "' for locale='" + print(locale) + "'", defName, definition.getName());

         // Language, Unknown Country & Unknown Variant
         locale = new Locale("en", "FR", "XX");
         factory = i18nFactorySet.createFactory(locale , request, context);
         assertNotNull("18. DefinitionsFactory is null for locale='" + print(locale) + "'", factory);
         defName = "B_en";
         definition = factory.getDefinition(defName, request, context);
         assertNotNull("19. Definition '" + defName + "' Not Found for locale='" + print(locale) + "'", definition);
         assertEquals("20. Definition '" + defName + "' for locale='" + print(locale) + "'", defName, definition.getName());

     } catch(Exception ex) {
         fail(ex.toString());
     }

  }

  /**
   * String representation of a Locale. A bug in the
   * Locale.toString() method results in Locales with
   * just a variant being incorrectly displayed.
   */
  private String print(Locale locale) {

      return locale.getLanguage() + "_" +
                locale.getCountry() + "_" +
                locale.getVariant();
  }



}

