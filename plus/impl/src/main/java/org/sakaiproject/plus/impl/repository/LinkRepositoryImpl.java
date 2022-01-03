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

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Projections;

import org.sakaiproject.plus.api.model.Context;
import org.sakaiproject.plus.api.model.Link;
import org.sakaiproject.plus.api.repository.LinkRepository;
import org.sakaiproject.springframework.data.SpringCrudRepositoryImpl;

import org.springframework.transaction.annotation.Transactional;

public class LinkRepositoryImpl extends SpringCrudRepositoryImpl<Link, String>  implements LinkRepository {

	@Transactional(readOnly = true)
    public Link findByLinkAndContext(String link, Context context) {

        // TODO: Figure out LIMIT 1 without using a list hack - sheesh it is a uniqueness constraint
        System.out.println("findByResourceAndContext link="+link+" context="+context.getId());
        List<Link> list = (List<Link>) sessionFactory.getCurrentSession().createCriteria(Link.class)
            .add(Restrictions.eq("link", link))
            .add(Restrictions.eq("context", context))
            .list();
System.out.println("list = "+list);
        if ( list == null || list.size() < 1 ) return null;
        return list.get(0);
    }

}
