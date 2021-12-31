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
package org.sakaiproject.plus.impl.service;

import org.tsugi.lti13.objects.LaunchJWT;

import java.util.Optional;

import javax.annotation.Resource;

import org.hibernate.SessionFactory;

import org.sakaiproject.plus.api.Launch;
import org.sakaiproject.plus.api.service.LaunchService;

import org.sakaiproject.plus.api.model.Tenant;
import org.sakaiproject.plus.api.model.Subject;
import org.sakaiproject.plus.api.model.Context;
import org.sakaiproject.plus.api.model.Link;
import org.sakaiproject.plus.api.model.LineItem;
import org.sakaiproject.plus.api.model.Score;

import org.sakaiproject.plus.api.repository.TenantRepository;
import org.sakaiproject.plus.api.repository.SubjectRepository;
import org.sakaiproject.plus.api.repository.ContextRepository;
import org.sakaiproject.plus.api.repository.LinkRepository;
import org.sakaiproject.plus.api.repository.LineItemRepository;
import org.sakaiproject.plus.api.repository.ScoreRepository;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class LaunchServiceImpl implements LaunchService {

    @Resource private SessionFactory sessionFactory;

    @Resource private TenantRepository tenantRepository;
    @Resource private SubjectRepository subjectRepository;
    @Resource private ContextRepository contextRepository;
    @Resource private LinkRepository linkRepository;
    @Resource private LineItemRepository lineItemRepository;
    @Resource private ScoreRepository scoreRepository;

	/*
	 * Handle the initial launch - creating objects as needed (a.k.a. The BIG LEFT JOIN)
	 */
	public Launch loadLaunchFromJWT(LaunchJWT launchJWT, Tenant tenant)
	{
		if ( launchJWT == null || tenant == null ) {
		   throw new RuntimeException("launchJWT and tenant must be non-null");
		}

		String issuer = launchJWT.issuer;
        String clientId = launchJWT.audience;

		if ( issuer == null || clientId == null ) {
		   throw new RuntimeException("LaunchJWT issuer and clientId (audience) must be non-null");
		}

		if ( ! issuer.equals(tenant.getIssuer()) || ! clientId.equals(tenant.getClientId()) ) {
		   throw new RuntimeException("issuer and clientId (audience) must match between tenant and LaunchJWT");
		}

		if ( tenant.getId() == null ) {
		   throw new RuntimeException("loadLaunchFromJWT requires persisted tenant");
		}


        String subject = launchJWT.subject;
        String contextId = launchJWT.context.id;
        String resourceLinkId = launchJWT.resource_link.id;
        String deploymentId = launchJWT.deployment_id;
System.out.println(
        " issuer="+issuer+
        " clientId="+clientId+
        " deploymentId="+deploymentId+
        " contextId="+contextId+
        " resourceLinkId="+resourceLinkId+
        " subject="+subject
);
		LaunchImpl launch = new LaunchImpl();
		launch.launchService = this;
		return launch;
	}
	/*
	 * The JSON only contains IDs - the actual objects will be brought in dynamically
	 */
	public Launch loadLaunchFromJSON(String jsonString) {
		return null;
	}
	/*
	 * Save the serializable bits of a LAUNCH into JSON to be put in a session
	 */
	public String saveLaunchToJson(Launch launch){
		return null;
	}

}

