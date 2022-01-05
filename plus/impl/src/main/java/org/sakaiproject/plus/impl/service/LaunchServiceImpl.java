/*
 * Copyright (c) 2021- Charles R. Severance
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.sakaiproject.plus.impl.service;

import org.tsugi.lti13.objects.LaunchJWT;

import java.util.Optional;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;

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
@Setter
public class LaunchServiceImpl implements LaunchService {

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
        String deploymentId = launchJWT.deployment_id;

		if ( issuer == null || clientId == null || deploymentId == null ) {
		   throw new RuntimeException("LaunchJWT issuer, clientId/audience, and deploymentId must be non-null");
		}

		if ( ! issuer.equals(tenant.getIssuer()) || ! clientId.equals(tenant.getClientId()) || ! deploymentId.equals(tenant.getDeploymentId()) ) {
		   throw new RuntimeException("issuer, clientId/audience and deploymentId must match between tenant and LaunchJWT");
		}

		if ( tenant.getId() == null ) {
		   throw new RuntimeException("loadLaunchFromJWT requires persisted tenant");
		}

        String contextId = launchJWT.context != null ? launchJWT.context.id : null;
        String subjectId = launchJWT.subject;
        String linkId =  launchJWT.resource_link != null ? launchJWT.resource_link.id : null;

		LaunchImpl launch = new LaunchImpl();
		launch.launchService = this;
		launch.tenant = tenant;

		boolean changed = false;
		Subject subject = null;
		if ( subjectId != null ) {
			subject = subjectRepository.findBySubjectAndTenant(subjectId, tenant);
			if ( subject == null ) {
				subject = new Subject(subjectId, tenant);
				subject.setSubject(subjectId);
				subject.setEmail(launchJWT.email);
				subject.setDisplayName(launchJWT.getDisplayName());
				changed = true;
			} else {
				if ( StringUtils.compare(subject.getEmail(), launchJWT.email) != 0 ) {
					subject.setEmail(launchJWT.email);
					changed = true;
				}
				if ( StringUtils.compare(subject.getDisplayName(), launchJWT.getDisplayName() ) != 0 ) {
					subject.setDisplayName(launchJWT.getDisplayName());
					changed = true;
				}
			}
			if ( changed ) {
				subjectRepository.save(subject);
			}
			launch.subject = subject;
		}

		Context context = null;
		if ( contextId != null ) {
			context = contextRepository.findByContextAndTenant(contextId, tenant);
			changed = false;
			if ( context == null ) {
				context = new Context();
				context.setContext(contextId);
				context.setTenant(tenant);
				context.setTitle(launchJWT.context.title);
				context.setLabel(launchJWT.context.label);
				context.setLineItems(launchJWT.endpoint.lineitems);
				context.setContextMemberships(launchJWT.names_and_roles.context_memberships_url);
				changed = true;
			} else {
				if ( StringUtils.compare(context.getTitle(), launchJWT.context.title) != 0 ) {
					context.setTitle(launchJWT.context.title);
					changed = true;
				}
				if ( StringUtils.compare(context.getLabel(), launchJWT.context.label) != 0 ) {
					context.setLabel(launchJWT.context.label);
					changed = true;
				}
				if ( StringUtils.compare(context.getLineItems(), launchJWT.endpoint.lineitems) != 0 ) {
					context.setLineItems(launchJWT.endpoint.lineitems);
					changed = true;
				}
				if ( StringUtils.compare(context.getContextMemberships(), launchJWT.names_and_roles.context_memberships_url) != 0 ) {
					context.setContextMemberships(launchJWT.names_and_roles.context_memberships_url);
					changed = true;
				}
			}
			if ( changed) contextRepository.save(context);
			launch.context = context;
		}

		if ( linkId != null && context != null ) {
			Link link = linkRepository.findByLinkAndContext(linkId, context);
			changed = false;
			if ( link == null ) {
				link = new Link();
				link.setLink(linkId);
				link.setContext(context);
				link.setTitle(launchJWT.resource_link.title);
				link.setDescription(launchJWT.resource_link.description);
				changed = true;
			} else {
				if ( StringUtils.compare(link.getTitle(), launchJWT.resource_link.title) != 0 ) {
					link.setTitle(launchJWT.resource_link.title);
					changed = true;
				}
				if ( StringUtils.compare(link.getDescription(), launchJWT.resource_link.description) != 0 ) {
					link.setDescription(launchJWT.resource_link.description);
					changed = true;
				}
			}
			if ( changed) linkRepository.save(link);
			launch.link = link;
		}

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

