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

package org.sakaiproject.plus.impl.repository;

import java.util.List;

import org.sakaiproject.plus.api.model.Context;
import org.sakaiproject.plus.api.model.ContextLog;
import org.sakaiproject.plus.api.repository.ContextLogRepository;
import org.sakaiproject.springframework.data.SpringCrudRepositoryImpl;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Predicate;

import org.springframework.transaction.annotation.Transactional;

public class ContextLogRepositoryImpl extends SpringCrudRepositoryImpl<ContextLog, Long>  implements ContextLogRepository {

    @Transactional(readOnly = true)
    public List<ContextLog> getLogEntries(Context context, Boolean success, int limit)
	{
        CriteriaBuilder cb = sessionFactory.getCriteriaBuilder();
        CriteriaQuery<ContextLog> cr = cb.createQuery(ContextLog.class);
        Root<ContextLog> root = cr.from(ContextLog.class);

        Predicate cond = null;
		if ( success == null ) {
			cond = cb.equal(root.get("context"), context);
		} else {
			cond = cb.and(
                cb.equal(root.get("success"), success),
                cb.equal(root.get("context"), context)
			);
		}

        CriteriaQuery<ContextLog> cq = cr
            .select(root)
            .where(cond)
            .orderBy(cb.desc(root.get("createdAt")))
        ;

        List<ContextLog> result = sessionFactory.getCurrentSession()
                .createQuery(cq)
				.setMaxResults(limit)
				.list();

        return result;
	}

}
