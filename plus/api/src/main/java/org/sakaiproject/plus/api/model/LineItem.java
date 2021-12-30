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

import java.time.Instant;

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
@Table(name = "PLUS_LINEITEM",
  indexes = { @Index(columnList = "RESOURCE_ID, CONTEXT_GUID") },
  uniqueConstraints = { @UniqueConstraint(columnNames = { "RESOURCE_ID", "CONTEXT_GUID" }) }
)
@Getter
@Setter
public class LineItem extends BaseLTI implements PersistableEntity<String> {

    @Id
    @Column(name = "LINEITEM_GUID", length = 36, nullable = false)
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "CONTEXT_GUID", nullable = false)
	private Context context;

	// Can optionally belong to a link
	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "LINK_GUID", nullable = true)
	private Link link;

	// The AGS resourceId - recommended
    @Column(name = "RESOURCE_ID", length = 1024, nullable = true)
    private String resourceId;

    @Column(name = "TAG", length = 1024, nullable = true)
    private String tag;

    @Column(name = "LABEL", length = 1024, nullable = true)
    private String label;

    @Column(name = "SCOREMAXIMUM")
    private Integer scoreMaximum;

    @Column(name = "STARTDATETIME")
    private Instant startDateTime;

    @Column(name = "ENDDATETIME")
    private Instant endDateTime;

}

/*
{
  "id" : "https://lms.example.com/context/2923/lineitems/1",
  "scoreMaximum" : 60,
  "label" : "Chapter 5 Test",
  "resourceId" : "a-9334df-33",
  "tag" : "grade",
  "resourceLinkId" : "1g3k4dlk49fk",
  "startDateTime": "2018-03-06T20:05:02Z",
  "endDateTime": "2018-04-06T22:05:03Z"
}
*/
