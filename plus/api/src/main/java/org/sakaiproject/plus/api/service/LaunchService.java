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

package org.sakaiproject.plus.api.service;

import org.sakaiproject.plus.api.Launch;
import org.sakaiproject.plus.api.model.Tenant;

import org.tsugi.lti13.objects.LaunchJWT;

public interface LaunchService {

	/*
	 * Handle the initial launch - creating objects as needed (a.k.a. The BIG LEFT JOIN)
	 */
	Launch loadLaunchFromJWT(LaunchJWT tokenBody, Tenant tenant);
	/*
	 * The JSON only contains IDs - the actual objects will be brought in dynamically
	 */
	Launch loadLaunchFromJSON(String jsonString);
	/*
	 * Save the serializable bits of a LAUNCH into JSON to be put in a session
	 */
	String saveLaunchToJson(Launch launch);

}
