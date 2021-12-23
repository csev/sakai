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
package org.sakaiproject.plus.impl;

import javax.annotation.Resource;
import org.hibernate.SessionFactory;

import org.sakaiproject.plus.api.Launch;

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

@Getter
@Setter
public class LaunchImpl implements org.sakaiproject.plus.api.Launch, java.io.Serializable {

	private transient Tenant tenant = null;
	private transient Context context = null;
	private transient Subject subject = null;
	private transient Link link = null;

	// Those specifically associated with the launch
	private transient LineItem lineItem = null;
	private transient Score score = null;

	// To make good on the promise of serialization :)
	private String tenantId = null;
	private String contextId = null;
	private String subjectId = null;
	private String linkId = null;
	private String lineItemId = null;
	private String scoreId = null;

    @Resource private SessionFactory sessionFactory;

    @Resource private TenantRepository tenantRepository;
    @Resource private SubjectRepository subjectRepository;
    @Resource private ContextRepository contextRepository;
    @Resource private LinkRepository linkRepository;
    @Resource private LineItemRepository lineItemRepository;
    @Resource private ScoreRepository scoreRepository;

}
