/*
 * Copyright (c) 2021- The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.plus.api;

import org.sakaiproject.plus.api.model.Tenant;
import org.sakaiproject.plus.api.model.Subject;
import org.sakaiproject.plus.api.model.Context;
import org.sakaiproject.plus.api.model.Link;
import org.sakaiproject.plus.api.model.LineItem;
import org.sakaiproject.plus.api.model.Score;

// Need to create getters and setters by hand because lombok does not like interfaces :)
public interface Launch {

	public Tenant getTenant();
	public void setTenant(Tenant tenant);
	public String getTenantId();
	public void setTenantId(String tenantId);

	public Context getContext();
	public void setContext(Context context);
	public String getContextId();
	public void setContextId(String contextId);

	public Subject getSubject();
	public void setSubject(Subject subject);
	public String getSubjectId();
	public void setSubjectId(String subjectId);

	public Link getLink();
	public void setLink(Link link);
	public String getLinkId();
	public void setLinkId(String linkId);

	public LineItem getLineItem();
	public void setLineItem(LineItem lineItem);
	public String getLineItemId();
	public void setLineItemId(String lineItemId);

	public Score getScore();
	public void setScore(Score score);
	public String getScoreId();
	public void setScoreId(String scoreId);

}
