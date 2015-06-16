/*
 * $Id: TilesPlugin.java 471754 2006-11-06 14:55:09Z husted $
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

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.PlugIn;
import org.apache.struts.action.RequestProcessor;
import org.apache.struts.chain.ComposableRequestProcessor;
import org.apache.struts.config.ControllerConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.config.PlugInConfig;
import org.apache.struts.util.RequestUtils;

/**
 * Tiles Plugin used to initialize Tiles.
 * This plugin is to be used with Struts 1.1 in association with
 * {@link TilesRequestProcessor}.
 * <br>
 * This plugin creates one definition factory for each Struts-module. The definition factory
 * configuration is read first from 'web.xml' (backward compatibility), then it is
 * overloaded with values found in the plugin property values.
 * <br>
 * The plugin changes the Struts configuration by specifying a {@link TilesRequestProcessor} as
 * request processor. If you want to use your own RequestProcessor,
 * it should subclass TilesRequestProcessor.
 * <br>
 * This plugin can also be used to create one single factory for all modules.
 * This behavior is enabled by specifying <code>moduleAware=false</code> in each
 * plugin properties. In this case, the definition factory
 * configuration file is read by the first Tiles plugin to be initialized. The order is
 * determined by the order of modules declaration in web.xml. The first module
 * is always the default one if it exists.
 * The plugin should be declared in each struts-config.xml file in order to
 * properly initialize the request processor.
 * @since Struts 1.1
 */
public class TilesPlugin implements PlugIn {

    /**
     * Commons Logging instance.
     */
    protected static Log log = LogFactory.getLog(TilesPlugin.class);

    /**
     * Is the factory module aware?
     */
    protected boolean moduleAware = false;

    /**
     * Tiles util implementation classname. This property can be set
     * by user in the plugin declaration.
     */
    protected String tilesUtilImplClassname = null;

    /**
     * Associated definition factory.
     */
    protected DefinitionsFactory definitionFactory = null;

    /**
     * The plugin config object provided by the ActionServlet initializing
     * this plugin.
     */
    protected PlugInConfig currentPlugInConfigObject=null;

    /**
     * Get the module aware flag.
     * @return <code>true</code>: user wants a single factory instance,
     * <code>false:</code> user wants multiple factory instances (one per module with Struts)
     */
    public boolean isModuleAware() {
        return moduleAware;
    }

    /**
     * Set the module aware flag.
     * This flag is only meaningful if the property <code>tilesUtilImplClassname</code> is not
     * set.
     * @param moduleAware <code>true</code>: user wants a single factory instance,
     * <code>false:</code> user wants multiple factory instances (one per module with Struts)
     */
    public void setModuleAware(boolean moduleAware) {
        this.moduleAware = moduleAware;
    }

    /**
     * <p>Receive notification that the specified module is being
     * started up.</p>
     *
     * @param servlet ActionServlet that is managing all the modules
     *  in this web application.
     * @param moduleConfig ModuleConfig for the module with which
     *  this plugin is associated.
     *
     * @exception ServletException if this <code>PlugIn</code> cannot
     *  be successfully initialized.
     */
    public void init(ActionServlet servlet, ModuleConfig moduleConfig)
        throws ServletException {

        // Create factory config object
        DefinitionsFactoryConfig factoryConfig =
            readFactoryConfig(servlet, moduleConfig);

        // Set the module name in the config. This name will be used to compute
        // the name under which the factory is stored.
        factoryConfig.setFactoryName(moduleConfig.getPrefix());

        // Set RequestProcessor class
        this.initRequestProcessorClass(moduleConfig);

        this.initTilesUtil();

        this.initDefinitionsFactory(servlet.getServletContext(), moduleConfig, factoryConfig);
    }

    /**
     * Set TilesUtil implementation according to properties 'tilesUtilImplClassname'
     * and 'moduleAware'.  These properties are taken into account only once. A
     * side effect is that only the values set in the first initialized plugin are
     * effectively taken into account.
     * @throws ServletException
     */
    private void initTilesUtil() throws ServletException {

        if (TilesUtil.isTilesUtilImplSet()) {
            return;
        }

        // Check if user has specified a TilesUtil implementation classname or not.
        // If no implementation is specified, check if user has specified one
        // shared single factory for all module, or one factory for each module.

        if (this.getTilesUtilImplClassname() == null) {

            if (isModuleAware()) {
                TilesUtil.setTilesUtil(new TilesUtilStrutsModulesImpl());
            } else {
                TilesUtil.setTilesUtil(new TilesUtilStrutsImpl());
            }

        } else { // A classname is specified for the tilesUtilImp, use it.
            try {
                TilesUtilStrutsImpl impl =
                    (TilesUtilStrutsImpl) RequestUtils
                        .applicationClass(getTilesUtilImplClassname())
                        .newInstance();
                TilesUtil.setTilesUtil(impl);

            } catch (ClassCastException ex) {
                throw new ServletException(
                    "Can't set TilesUtil implementation to '"
                        + getTilesUtilImplClassname()
                        + "'. TilesUtil implementation should be a subclass of '"
                        + TilesUtilStrutsImpl.class.getName()
                        + "'");

            } catch (Exception ex) {
                throw new ServletException(
                    "Can't set TilesUtil implementation.",
                    ex);
            }
        }

    }

    /**
     * Initialize the DefinitionsFactory this module will use.
     * @param servletContext
     * @param moduleConfig
     * @param factoryConfig
     * @throws ServletException
     */
    private void initDefinitionsFactory(
        ServletContext servletContext,
        ModuleConfig moduleConfig,
        DefinitionsFactoryConfig factoryConfig)
        throws ServletException {

        // Check if a factory already exist for this module
        definitionFactory =
            ((TilesUtilStrutsImpl) TilesUtil.getTilesUtil()).getDefinitionsFactory(
                servletContext,
                moduleConfig);

        if (definitionFactory != null) {
            log.info(
                "Factory already exists for module '"
                    + moduleConfig.getPrefix()
                    + "'. The factory found is from module '"
                    + definitionFactory.getConfig().getFactoryName()
                    + "'. No new creation.");

            return;
        }

        // Create configurable factory
        try {
            definitionFactory =
                TilesUtil.createDefinitionsFactory(
                    servletContext,
                    factoryConfig);

        } catch (DefinitionsFactoryException ex) {
            log.error(
                "Can't create Tiles definition factory for module '"
                    + moduleConfig.getPrefix()
                    + "'.");

            throw new ServletException(ex);
        }

        log.info(
            "Tiles definition factory loaded for module '"
                + moduleConfig.getPrefix()
                + "'.");
    }

    /**
     * End plugin.
     */
    public void destroy() {
        definitionFactory.destroy();
        definitionFactory = null;
    }

    /**
     * Create FactoryConfig and initialize it from web.xml and struts-config.xml.
     *
     * @param servlet ActionServlet that is managing all the modules
     *  in this web application.
     * @param config ModuleConfig for the module with which
     *  this plugin is associated.
     * @exception ServletException if this <code>PlugIn</code> cannot
     *  be successfully initialized.
     */
    protected DefinitionsFactoryConfig readFactoryConfig(
        ActionServlet servlet,
        ModuleConfig config)
        throws ServletException {

        // Create tiles definitions config object
        DefinitionsFactoryConfig factoryConfig = new DefinitionsFactoryConfig();
        // Get init parameters from web.xml files
        try {
            DefinitionsUtil.populateDefinitionsFactoryConfig(
                factoryConfig,
                servlet.getServletConfig());

        } catch (Exception ex) {
            if (log.isDebugEnabled()){
                log.debug("", ex);
            }
            ex.printStackTrace();
            throw new UnavailableException(
                "Can't populate DefinitionsFactoryConfig class from 'web.xml': "
                    + ex.getMessage());
        }

        // Get init parameters from struts-config.xml
        try {
            Map strutsProperties = findStrutsPlugInConfigProperties(servlet, config);
            factoryConfig.populate(strutsProperties);

        } catch (Exception ex) {
            if (log.isDebugEnabled()) {
                log.debug("", ex);
            }

            throw new UnavailableException(
                "Can't populate DefinitionsFactoryConfig class from '"
                    + config.getPrefix()
                    + "/struts-config.xml':"
                    + ex.getMessage());
        }

        return factoryConfig;
    }

    /**
     * Find original properties set in the Struts PlugInConfig object.
     * First, we need to find the index of this plugin. Then we retrieve the array of configs
     * and then the object for this plugin.
     * @param servlet ActionServlet that is managing all the modules
     *  in this web application.
     * @param config ModuleConfig for the module with which
     *  this plug in is associated.
     *
     * @exception ServletException if this <code>PlugIn</code> cannot
     *  be successfully initialized.
     */
    protected Map findStrutsPlugInConfigProperties(
        ActionServlet servlet,
        ModuleConfig config)
        throws ServletException {

        return currentPlugInConfigObject.getProperties();
    }

    /**
     * Set RequestProcessor to appropriate Tiles {@link RequestProcessor}.
     * First, check if a RequestProcessor is specified. If yes, check if it extends
     * the appropriate {@link TilesRequestProcessor} class. If not, set processor class to
     * TilesRequestProcessor.
     *
     * @param config ModuleConfig for the module with which
     *  this plugin is associated.
     * @throws ServletException On errors.
     */
    protected void initRequestProcessorClass(ModuleConfig config)
        throws ServletException {

        String tilesProcessorClassname = TilesRequestProcessor.class.getName();
        ControllerConfig ctrlConfig = config.getControllerConfig();
        String configProcessorClassname = ctrlConfig.getProcessorClass();

        // Check if specified classname exist
        Class configProcessorClass;
        try {
            configProcessorClass =
                RequestUtils.applicationClass(configProcessorClassname);

        } catch (ClassNotFoundException ex) {
            log.fatal(
                "Can't set TilesRequestProcessor: bad class name '"
                    + configProcessorClassname
                    + "'.");
            throw new ServletException(ex);
        }

        // Check to see if request processor uses struts-chain.  If so,
        // no need to replace the request processor.
        if (ComposableRequestProcessor.class.isAssignableFrom(configProcessorClass)) {
            return;
        }

        // Check if it is the default request processor or Tiles one.
        // If true, replace by Tiles' one.
        if (configProcessorClassname.equals(RequestProcessor.class.getName())
            || configProcessorClassname.endsWith(tilesProcessorClassname)) {

            ctrlConfig.setProcessorClass(tilesProcessorClassname);
            return;
        }

        // Check if specified request processor is compatible with Tiles.
        Class tilesProcessorClass = TilesRequestProcessor.class;
        if (!tilesProcessorClass.isAssignableFrom(configProcessorClass)) {
            // Not compatible
            String msg =
                "TilesPlugin : Specified RequestProcessor not compatible with TilesRequestProcessor";
            if (log.isFatalEnabled()) {
                log.fatal(msg);
            }
            throw new ServletException(msg);
        }
    }

    /**
     * Set Tiles util implemention classname.
     * If this property is set, the flag <code>moduleAware</code> will not be used anymore.
     * @param tilesUtilImplClassname Classname.
     */
    public void setTilesUtilImplClassname(String tilesUtilImplClassname) {
        this.tilesUtilImplClassname = tilesUtilImplClassname;
    }

    /**
     * Get Tiles util implemention classname.
     * @return The classname or <code>null</code> if none is set.
     */
    public String getTilesUtilImplClassname() {
        return tilesUtilImplClassname;
    }

    /**
     * Method used by the ActionServlet initializing this plugin.
     * Set the plugin config object read from module config.
     * @param plugInConfigObject PlugInConfig.
     */
    public void setCurrentPlugInConfigObject(PlugInConfig plugInConfigObject) {
        this.currentPlugInConfigObject = plugInConfigObject;
    }

}
