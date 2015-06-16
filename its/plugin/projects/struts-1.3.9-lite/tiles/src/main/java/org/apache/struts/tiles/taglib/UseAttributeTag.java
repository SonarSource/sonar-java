/*
 * $Id: UseAttributeTag.java 471754 2006-11-06 14:55:09Z husted $
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

package org.apache.struts.tiles.taglib;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.struts.tiles.taglib.util.TagUtils;
import org.apache.struts.tiles.ComponentContext;


/**
 * Custom tag exposing a component attribute to page.
 *
 */
public class UseAttributeTag extends TagSupport {


    // ----------------------------------------------------- Instance Variables


    /**
     * Class name of object.
     */
    private String  classname = null;


    /**
     * The scope name.
     */
    private String scopeName = null;

    /**
     * The scope value.
     */
    private int scope = PageContext.PAGE_SCOPE;



    /**
     * The attribute name to be exposed.
     */
    private String attributeName = null;

    /**
     * Are errors ignored. This is the property for attribute 'ignore'.
     * Default value is <code>false</code>, which throws an exception.
     * Only "attribute not found" - errors are ignored.
     */
  protected boolean isErrorIgnored = false;


    // ------------------------------------------------------------- Properties


    /**
     * Release all allocated resources.
     */
    public void release() {

        super.release();
        attributeName = null;
        classname = null;
        scope = PageContext.PAGE_SCOPE;
        scopeName = null;
        isErrorIgnored = false;
          // Parent doesn't clear id, so we do it
          // bug reported by Heath Chiavettone on 18 Mar 2002
        id = null;
    }

    /**
     * Get class name.
     */
    public String getClassname() {

  return (this.classname);

    }


    /**
     * Set the class name.
     *
     * @param name The new class name.
     */
    public void setClassname(String name) {

  this.classname = name;

    }

    /**
     * Set name.
     */
  public void setName(String value){
    this.attributeName = value;
  }

    /**
     * Get name.
     */
  public String getName()
  {
  return attributeName;
  }

    /**
     * Set the scope.
     *
     * @param scope The new scope.
     */
    public void setScope(String scope) {
  this.scopeName = scope;
    }

    /**
     * Get scope.
     */
  public String getScope()
  {
  return scopeName;
  }

    /**
     * Set ignore.
     */
  public void setIgnore(boolean ignore)
    {
    this.isErrorIgnored = ignore;
    }

    /**
     * Get ignore.
     */
  public boolean getIgnore()
  {
  return isErrorIgnored;
  }

    // --------------------------------------------------------- Public Methods


    /**
     * Expose the requested attribute from component context.
     *
     * @exception JspException if a JSP exception has occurred
     */
  public int doStartTag() throws JspException
    {
      // Do a local copy of id
    String localId=this.id;
    if( localId==null )
      localId=attributeName;

    ComponentContext compContext = (ComponentContext)pageContext.getAttribute( ComponentConstants.COMPONENT_CONTEXT, PageContext.REQUEST_SCOPE);
    if( compContext == null )
      throw new JspException ( "Error - tag useAttribute : no tiles context found." );

    Object value = compContext.getAttribute(attributeName);
        // Check if value exists and if we must send a runtime exception
    if( value == null )
      if(!isErrorIgnored)
        throw new JspException ( "Error - tag useAttribute : attribute '"+ attributeName + "' not found in context. Check tag syntax" );
       else
        return SKIP_BODY;

    if( scopeName != null )
      {
      scope = TagUtils.getScope( scopeName, PageContext.PAGE_SCOPE );
      if(scope!=ComponentConstants.COMPONENT_SCOPE)
        pageContext.setAttribute(localId, value, scope);
      }
     else
      pageContext.setAttribute(localId, value);

      // Continue processing this page
    return SKIP_BODY;
    }




    /**
     * Clean up after processing this enumeration.
     *
     * @exception JspException if a JSP exception has occurred
     */
  public int doEndTag() throws JspException
    {
    return (EVAL_PAGE);
    }

}
