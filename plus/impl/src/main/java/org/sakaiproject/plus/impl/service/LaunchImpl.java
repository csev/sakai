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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LaunchImpl implements org.sakaiproject.plus.api.Launch, java.io.Serializable {

	protected LaunchService launchService;

	protected transient Tenant tenant = null;
	protected transient Context context = null;
	protected transient Subject subject = null;
	protected transient Link link = null;

}
