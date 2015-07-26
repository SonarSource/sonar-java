/*
 * $Id: XmlParser.java 471754 2006-11-06 14:55:09Z husted $
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

package org.apache.struts.tiles.xmlDefinition;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

/**
 * Parse an XML definitions file.
 */
public class XmlParser
{

    /** Associated digester. */
  protected Digester digester;
    /**
     * Should we use a validating XML parser to read the configuration file.
     * Default is <code>false</code>.
     */
    protected boolean validating = false;
    /**
     * The set of public identifiers, and corresponding resource names for
     * the versions of the configuration file DTDs we know about.  There
     * <strong>MUST</strong> be an even number of Strings in this list!
     */
    protected String registrations[] = {
        "-//Apache Software Foundation//DTD Tiles Configuration 1.1//EN",
        "/org/apache/struts/resources/tiles-config_1_1.dtd",
        "-//Apache Software Foundation//DTD Tiles Configuration 1.3//EN",
        "/org/apache/struts/resources/tiles-config_1_3.dtd"
    };

     /**
      * Constructor.
      * Creates a digester parser and initializes syntax rules.
      */
  public XmlParser()
  {
    digester = new Digester();
    digester.setValidating(validating);
    digester.setNamespaceAware(true);
    digester.setUseContextClassLoader(true);
    // Register our local copy of the DTDs that we can find
  for (int i = 0; i < registrations.length; i += 2) {
      URL url = this.getClass().getResource(registrations[i+1]);
      if (url != null)
          {
          digester.register(registrations[i], url.toString());
          }
  }
    // Init syntax rules
  initDigester( digester );
  }

    /**
     * Set digester validating flag.
     */
  public void setValidating( boolean validating )
    {
    digester.setValidating( validating);
    }


   /**
    * Init digester for components syntax.
    * This is an old set of rules, left for backward compatibility.
    * @param digester Digester instance to use.
    */
  private void initDigesterForComponentsDefinitionsSyntax( Digester digester )
  {
     // Common constants
  String PACKAGE_NAME = "org.apache.struts.tiles.xmlDefinition";
  String DEFINITION_TAG = "component-definitions/definition";
  String definitionHandlerClass = PACKAGE_NAME + ".XmlDefinition";

  String PUT_TAG  = DEFINITION_TAG + "/put";
  String putAttributeHandlerClass = PACKAGE_NAME + ".XmlAttribute";

  String LIST_TAG = DEFINITION_TAG + "/putList";
  String listHandlerClass     = PACKAGE_NAME + ".XmlListAttribute";

  String ADD_LIST_ELE_TAG = LIST_TAG + "/add";

    // syntax rules
    digester.addObjectCreate(  DEFINITION_TAG, definitionHandlerClass );
    digester.addSetProperties( DEFINITION_TAG);
    digester.addSetNext(       DEFINITION_TAG, "putDefinition", definitionHandlerClass);
    // put / putAttribute rules
    digester.addObjectCreate(  PUT_TAG, putAttributeHandlerClass);
    digester.addSetNext(       PUT_TAG, "addAttribute", putAttributeHandlerClass);
    digester.addSetProperties( PUT_TAG);
    digester.addCallMethod(    PUT_TAG, "setBody", 0);
    // list rules
    digester.addObjectCreate(  LIST_TAG, listHandlerClass);
    digester.addSetProperties( LIST_TAG);
    digester.addSetNext(       LIST_TAG, "addAttribute", putAttributeHandlerClass);
    // list elements rules
    // We use Attribute class to avoid rewriting a new class.
    // Name part can't be used in listElement attribute.
    digester.addObjectCreate(  ADD_LIST_ELE_TAG, putAttributeHandlerClass);
    digester.addSetNext(       ADD_LIST_ELE_TAG, "add", putAttributeHandlerClass);
    digester.addSetProperties( ADD_LIST_ELE_TAG);
    digester.addCallMethod(    ADD_LIST_ELE_TAG, "setBody", 0);
  }

   /**
    * Init digester for Tiles syntax.
    * Same as components, but with first element = tiles-definitions
    * @param digester Digester instance to use.
    */
  private void initDigesterForTilesDefinitionsSyntax( Digester digester )
  {
     // Common constants
  String PACKAGE_NAME = "org.apache.struts.tiles.xmlDefinition";
  String DEFINITION_TAG = "tiles-definitions/definition";
  String definitionHandlerClass = PACKAGE_NAME + ".XmlDefinition";

  String PUT_TAG  = DEFINITION_TAG + "/put";
  String putAttributeHandlerClass = PACKAGE_NAME + ".XmlAttribute";

  //String LIST_TAG = DEFINITION_TAG + "/putList";
    // List tag value
  String LIST_TAG = "putList";
  String DEF_LIST_TAG = DEFINITION_TAG + "/" + LIST_TAG;
  String listHandlerClass     = PACKAGE_NAME + ".XmlListAttribute";
    // Tag value for adding an element in a list
  String ADD_LIST_ELE_TAG = "*/" + LIST_TAG + "/add";

    // syntax rules
    digester.addObjectCreate(  DEFINITION_TAG, definitionHandlerClass );
    digester.addSetProperties( DEFINITION_TAG);
    digester.addSetNext(       DEFINITION_TAG, "putDefinition", definitionHandlerClass);
    // put / putAttribute rules
    // Rules for a same pattern are called in order, but rule.end() are called
    // in reverse order.
    // SetNext and CallMethod use rule.end() method. So, placing SetNext in
    // first position ensure it will be called last (sic).
    digester.addObjectCreate(  PUT_TAG, putAttributeHandlerClass);
    digester.addSetNext(       PUT_TAG, "addAttribute", putAttributeHandlerClass);
    digester.addSetProperties( PUT_TAG);
    digester.addCallMethod(    PUT_TAG, "setBody", 0);
    // Definition level list rules
    // This is rules for lists nested in a definition
    digester.addObjectCreate(  DEF_LIST_TAG, listHandlerClass);
    digester.addSetProperties( DEF_LIST_TAG);
    digester.addSetNext(       DEF_LIST_TAG, "addAttribute", putAttributeHandlerClass);
    // list elements rules
    // We use Attribute class to avoid rewriting a new class.
    // Name part can't be used in listElement attribute.
    digester.addObjectCreate(  ADD_LIST_ELE_TAG, putAttributeHandlerClass);
    digester.addSetNext(       ADD_LIST_ELE_TAG, "add", putAttributeHandlerClass);
    digester.addSetProperties( ADD_LIST_ELE_TAG);
    digester.addCallMethod(    ADD_LIST_ELE_TAG, "setBody", 0);

    // nested list elements rules
    // Create a list handler, and add it to parent list
  String NESTED_LIST = "*/" + LIST_TAG + "/" + LIST_TAG;
    digester.addObjectCreate(  NESTED_LIST, listHandlerClass);
    digester.addSetProperties( NESTED_LIST);
    digester.addSetNext(       NESTED_LIST, "add", putAttributeHandlerClass);

    // item elements rules
    // We use Attribute class to avoid rewriting a new class.
    // Name part can't be used in listElement attribute.
  //String ADD_WILDCARD = LIST_TAG + "/addItem";
  // non String ADD_WILDCARD = LIST_TAG + "/addx*";
  String ADD_WILDCARD = "*/item";
  String menuItemDefaultClass = "org.apache.struts.tiles.beans.SimpleMenuItem";
    digester.addObjectCreate(  ADD_WILDCARD, menuItemDefaultClass, "classtype");
    digester.addSetNext(       ADD_WILDCARD, "add", "java.lang.Object");
    digester.addSetProperties( ADD_WILDCARD);

    // bean elements rules
  String BEAN_TAG = "*/bean";
  String beanDefaultClass = "org.apache.struts.tiles.beans.SimpleMenuItem";
    digester.addObjectCreate(  BEAN_TAG, beanDefaultClass, "classtype");
    digester.addSetNext(       BEAN_TAG, "add", "java.lang.Object");
    digester.addSetProperties( BEAN_TAG);

    // Set properties to surrounding element
  digester.addSetProperty(BEAN_TAG+ "/set-property", "property", "value");
  }

   /**
    * Init digester in order to parse instances definition file syntax.
    * Instances is an old name for "definition". This method is left for
    * backwards compatibility.
    * @param digester Digester instance to use.
    */
  private void initDigesterForInstancesSyntax( Digester digester )
  {
        // Build a digester to process our configuration resource
  String PACKAGE_NAME = "org.apache.struts.tiles.xmlDefinition";
  String INSTANCE_TAG = "component-instances/instance";
  String instanceHandlerClass = PACKAGE_NAME + ".XmlDefinition";

  String PUT_TAG = INSTANCE_TAG + "/put";
  String PUTATTRIBUTE_TAG = INSTANCE_TAG + "/putAttribute";
  String putAttributeHandlerClass = PACKAGE_NAME + ".XmlAttribute";

  String LIST_TAG     = INSTANCE_TAG + "/putList";
  String listHandlerClass     = PACKAGE_NAME + ".XmlListAttribute";

  String ADD_LIST_ELE_TAG = LIST_TAG + "/add";

    // component instance rules
    digester.addObjectCreate(  INSTANCE_TAG, instanceHandlerClass );
    digester.addSetProperties( INSTANCE_TAG);
    digester.addSetNext(       INSTANCE_TAG, "putDefinition", instanceHandlerClass);
    // put / putAttribute rules
    digester.addObjectCreate(  PUTATTRIBUTE_TAG, putAttributeHandlerClass);
    digester.addSetProperties( PUTATTRIBUTE_TAG);
    digester.addSetNext(       PUTATTRIBUTE_TAG, "addAttribute", putAttributeHandlerClass);
    // put / putAttribute rules
    digester.addObjectCreate(  PUT_TAG, putAttributeHandlerClass);
    digester.addSetProperties( PUT_TAG);
    digester.addSetNext(       PUT_TAG, "addAttribute", putAttributeHandlerClass);
    // list rules
    digester.addObjectCreate(  LIST_TAG, listHandlerClass);
    digester.addSetProperties( LIST_TAG);
    digester.addSetNext(       LIST_TAG, "addAttribute", putAttributeHandlerClass);
    // list elements rules
    // We use Attribute class to avoid rewriting a new class.
    // Name part can't be used in listElement attribute.
    digester.addObjectCreate(  ADD_LIST_ELE_TAG, putAttributeHandlerClass);
    digester.addSetProperties( ADD_LIST_ELE_TAG);
    digester.addSetNext(       ADD_LIST_ELE_TAG, "add", putAttributeHandlerClass);
  }

   /**
    * Init digester.
    * @param digester Digester instance to use.
    */
  protected void initDigester( Digester digester )
  {
  initDigesterForTilesDefinitionsSyntax( digester );
  initDigesterForComponentsDefinitionsSyntax( digester );
  initDigesterForInstancesSyntax( digester );
  }

  /**
   * Parse input reader and add encountered definitions to definitions set.
   * @param in Input stream.
   * @param definitions Xml Definitions set to which encountered definition are added.
   * @throws IOException On errors during file parsing.
   * @throws SAXException On errors parsing XML.
   */
  public void parse( InputStream in, XmlDefinitionsSet definitions ) throws IOException, SAXException
  {
    try
    {
      // set first object in stack
    //digester.clear();
    digester.push(definitions);
      // parse
      digester.parse(in);
      in.close();
      }
  catch (SAXException e)
    {
      //throw new ServletException( "Error while parsing " + mappingConfig, e);
    throw e;
      }

  }

    /**
     * Main method to check file syntax.
     */
  public static void main(String[] args)
  {
  //String filename = "E:/programs/jakarta-tomcat/webapps/wtiles-struts/WEB-INF/tiles-examples-defs.xml";
  String filename = "E:/programs/jakarta-tomcat-4.0.3/webapps/wtiles-struts/WEB-INF/tiles-examples-defs.xml";
  //String filename = "E:/programs/jakarta-tomcat/webapps/wtiles-struts/WEB-INF/tilesDefinitions.xml";
  //String filename = "E:/programs/jakarta-tomcat/webapps/wtiles-channel/WEB-INF/componentDefinitions.xml";
  //String filename2 = "E:/programs/jakarta-tomcat/webapps/wtiles-tutorial/WEB-INF/componentDefinitions.xml";


    if( args.length > 1 )
      {
      filename = args[1];
      } // end if

  System.out.println( "Read file '" + filename  +"'" );

  InputStream input = null;
  // InputStream input2 = null;
    // Open file
    try
      {
        input = new BufferedInputStream(
                             new FileInputStream( filename) );
    //    input2 = new BufferedInputStream(
          //                   new FileInputStream( filename2) );
      }
     catch( IOException ex )
      {
      System.out.println( "can't open file '" + filename + "' : " + ex.getMessage() );
      }
    // Check file syntax
    try
      {
        XmlParser parser = new XmlParser();
      parser.setValidating(true);
      XmlDefinitionsSet definitions = new XmlDefinitionsSet();
        System.out.println( "  Parse file" );
      parser.parse( input, definitions);
      //  System.out.println( "  Check file 2" );
      //parser.parse( input2, definitions);
        System.out.println( "  done." );
      System.out.println( "  Result : " + definitions.toString() );
      }
     catch( Exception ex )
      {
      System.out.println( "Error during parsing '" + filename + "' : " + ex.getMessage() );
      ex.printStackTrace();
      }
  }

}
