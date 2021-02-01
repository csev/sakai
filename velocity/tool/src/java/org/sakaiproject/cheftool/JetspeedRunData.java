/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2008 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.cheftool;

import javax.servlet.http.HttpServletRequest;

import org.sakaiproject.event.api.SessionState;
import org.sakaiproject.util.ParameterParser;

public class JetspeedRunData extends RunData
{
	protected SessionState state = null;

	protected String pid = null;
	protected HttpServletRequest my_req = null;

	public JetspeedRunData(HttpServletRequest req, SessionState state, String pid, ParameterParser params)
	{
		super(req, params);
System.out.println("JetspeedRunData "+state+" pid="+pid);
System.out.println("url="+req.getRequestURL().toString());
System.out.println(params);
System.out.println(state.getAttributeNames());
		this.state = state;
		this.pid = pid;
		this.my_req = req;
	}

	// support the return of the SessionState by:
	// SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());

	/**
	 * Access the current request's PortletSession state object.
	 * 
	 * @param id
	 *        The Portlet's unique id.
	 * @return the current request's PortletSession state object. (may be null).
	 */
	public SessionState getPortletSessionState(String id)
	{
System.out.println("--------------");
System.out.println("url="+this.my_req.getRequestURL().toString());
System.out.println("getPortletSessionState id="+id);
			for (String name : state.getAttributeNames())
            {
                Object value = state.getAttribute(name);
				System.out.println(name+"="+value);
            }
		return state;
	}

	/**
	 * Returns the portlet id (PEID) referenced in this request
	 * 
	 * @return the portlet id (PEID) referenced or null
	 */
	public String getJs_peid()
	{
System.out.println("getJs_peid pid="+pid);
		return pid;
	}
}
