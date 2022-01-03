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

package org.sakaiproject.plus.api.model;

import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.UniqueConstraint;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.CascadeType;

import org.hibernate.annotations.GenericGenerator;

import org.sakaiproject.springframework.data.PersistableEntity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "PLUS_CONTEXT",
  indexes = { @Index(columnList = "CONTEXT, TENNANT_GUID") },
  uniqueConstraints = { @UniqueConstraint(columnNames = { "CONTEXT", "TENNANT_GUID" }) }
)
@Getter
@Setter
public class Context extends BaseLTI implements PersistableEntity<String> {

    @Id
    @Column(name = "CONTEXT_GUID", length = 36, nullable = false)
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @Column(name = "CONTEXT", length = 1024, nullable = false)
    private String context;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "TENNANT_GUID", nullable = false)
	private Tenant tenant;

    @Column(name = "TITLE", length = 4000, nullable = true)
    private String title;

    @Column(name = "LABEL", length = 4000, nullable = true)
    private String label;

	// launchjwt.endpoint.lineitems
    @Column(name = "LINEITEMS", length = 4000, nullable = true)
    private String lineItems;

	// launchjwt.names_and_roles.context_memberships_url
    @Column(name = "CONTEXT_MEMBERSHIPS", length = 4000, nullable = true)
    private String contextMemberships;


}
