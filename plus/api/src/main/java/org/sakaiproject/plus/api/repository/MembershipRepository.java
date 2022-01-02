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
package org.sakaiproject.plus.api.repository;

import java.util.List;

import org.sakaiproject.plus.api.model.Context;
import org.sakaiproject.plus.api.model.Subject;
import org.sakaiproject.plus.api.model.Membership;
import org.sakaiproject.springframework.data.SpringCrudRepository;

public interface MembershipRepository extends SpringCrudRepository<Membership, String> {

	Membership findBySubjectAndContext(Subject subject, Context context);

}
