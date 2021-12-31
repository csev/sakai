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
@Table(name = "PLUS_SUBJECT",
  indexes = { @Index(columnList = "SUBJECT, TENNANT_GUID") },
  uniqueConstraints = { @UniqueConstraint(columnNames = { "SUBJECT", "TENNANT_GUID" }) }
)
@Getter
@Setter
public class Subject extends BaseLTI implements PersistableEntity<String> {

	public Subject(String subject, Tenant tenant) {
        this.subject = subject;
        this.tenant = tenant;
	}

    @Id
    @Column(name = "SUBJECT_GUID", length = 36, nullable = false)
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @Column(name = "SUBJECT", length = 1024, nullable = false)
    private String subject;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "TENNANT_GUID", nullable = false)
	private Tenant tenant;

    @Column(name = "DISPLAYNAME", length = 1024, nullable = true)
    private String displayName;

    @Column(name = "EMAIL", length = 1024, nullable = true)
    private String email;

    @Column(name = "LOCALE", length = 63, nullable = true)
    private String locale;

}
