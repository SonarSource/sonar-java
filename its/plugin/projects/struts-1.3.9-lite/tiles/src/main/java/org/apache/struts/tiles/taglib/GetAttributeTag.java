/*
 * $Id: GetAttributeTag.java 471754 2006-11-06 14:55:09Z husted $
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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.struts.tiles.ComponentContext;

  /**
   * Retrieve the value of the specified component/template attribute property,
   * and render it to the current JspWriter as a String.
   * The usual toString() conversion is applied on the found value.
   */
public class GetAttributeTag extends TagSupport implements ComponentConstants {

  private String attribute = null;
    /** Role attribute */
  private String role = null;
    /**
     * Do we ignore error if attribute is not found.
     * Default value is <code>false</code>, which will throw an exception.
     */
  private boolean isErrorIgnored = false;

  /**
   * Default constructor.
   */
  public GetAttributeTag() {
    super();
  }

    /**
     * Release all allocated resources.
     */
    public void release() {

        super.release();
        attribute = null;
        role = null;
        isErrorIgnored = false;
    }

    /**
     * Set attribute.
     * @param attribute Attribute.
     */
  public void setAttribute(String attribute){
    this.attribute = attribute;
  }

    /**
     * Get attribute.
     * @return Attribute.
     */
  public String getAttribute()
  {
  return attribute;
  }

    /**
     * Set Name.
     * Same as setAttribute().
     * @param value Attribute.
     */
  public void setName(String value)
    {
    this.attribute = value;
    }

    /**
     * Get Name.
     * Set as getAttribute().
     * @return Attribute.
     */
  public String getName()
  {
  return attribute;
  }

    /**
     * Set ignoring flag when attribute is not found.
     * @param ignore default: <code>false</code>: Exception is thrown when attribute is not found, set to <code>
     * true</code> to ignore missing attributes silently
     */
  public void setIgnore(boolean ignore)
    {
    this.isErrorIgnored = ignore;
    }

    /**
     * Get ignore flag.
     * @return <code>false</code>: Exception is thrown when attribute is not found, set to <code>
     * true</code> to ignore missing attributes silently
     */
  public boolean getIgnore()
  {
  return isErrorIgnored;
  }

    /**
     * Set role.
     * @param role The role the user must be in to store content.
     */
   public void setRole(String role) {
      this.role = role;
   }

    /**
     * Get role.
     * @return Role.
     */
  public String getRole()
  {
  return role;
  }

    /**
     * Close tag.
     * @throws JspException On error processing tag.
     */
  public int doEndTag() throws JspException {

      // Check role
    if(role != null && !((HttpServletRequest)pageContext.getRequest()).isUserInRole(role) )
      {
      return EVAL_PAGE;
      } // end if

      // Get context
    ComponentContext compContext = (ComponentContext)pageContext.getAttribute( ComponentConstants.COMPONENT_CONTEXT, PageContext.REQUEST_SCOPE);

    if( compContext == null )
      throw new JspException ( "Error - tag.getAsString : component context is not defined. Check tag syntax" );

    Object value = compContext.getAttribute(attribute);
    if( value == null)
      { // no value : throw error or fail silently according to ignore
      if(isErrorIgnored == false )
        throw new JspException ( "Error - tag.getAsString : attribute '"+ attribute + "' not found in context. Check tag syntax" );
       else
        return EVAL_PAGE;
      } // end if


    try
      {
      pageContext.getOut().print( value );
      }
     catch( IOException ex )
      {
      ex.printStackTrace();
      throw new JspException ( "Error - tag.getProperty : IOException ", ex);
      }

    return EVAL_PAGE;
  }
}
