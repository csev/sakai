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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.CascadeType;

import org.hibernate.annotations.GenericGenerator;

import org.sakaiproject.springframework.data.PersistableEntity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "PLUS_MEMBERSHIP",
  indexes = { @Index(columnList = "SUBJECT_GUID, CONTEXT_GUID") },
  uniqueConstraints = { @UniqueConstraint(columnNames = { "SUBJECT_GUID", "CONTEXT_GUID" }) }
)
@Getter
@Setter
public class Membership extends BaseLTI implements PersistableEntity<String> {

    @Id
    @Column(name = "MEMBERSHIP_GUID", length = 36, nullable = false)
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Column(name = "SUBJECT_GUID", nullable = false)
    private Subject subject;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "CONTEXT_GUID", nullable = false)
	private Context context;

    @Column(name = "ROLE", nullable = true)
    private Integer role;

    @Column(name = "ROLE_OVERRIDE", nullable = true)
    private Integer roleOveride;

}
