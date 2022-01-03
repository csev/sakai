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

import java.util.Set;

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
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import org.sakaiproject.springframework.data.PersistableEntity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "PLUS_TENANT",
  indexes = { @Index(columnList = "ISSUER, CLIENT_ID") },
  uniqueConstraints = { @UniqueConstraint(columnNames = { "ISSUER", "CLIENT_ID" }) }
)
@Getter
@Setter
public class Tenant extends BaseLTI implements PersistableEntity<String> {

    @Id
    @Column(name = "TENNANT_GUID", length = 36, nullable = false)
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

	/*
	// https://thorben-janssen.com/best-practices-many-one-one-many-associations-mappings/
	// https://thorben-janssen.com/hibernate-tips-unidirectional-one-to-many-association-without-junction-table/
	@OneToMany(orphanRemoval = true)
	@JoinColumn(name = "SUBJECT_GUID")
	private Set<Subject> subjects;
	*/

    @Column(name = "TITLE", length = 1024, nullable = false)
    private String title;

	// Issuer and client_id can be null while a key is being built but a key is not usable
	// until both fields are defined and the other values are present
    @Column(name = "ISSUER", length = 1024, nullable = true)
    protected String issuer;

    @Column(name = "CLIENT_ID", length = 1024, nullable = true)
    private String clientId;

    @Column(name = "DEPLOYMENT_ID", length = 1024, nullable = true)
    private String deploymentId;

    @Column(name = "OIDC_AUTH", length = 1024, nullable = true)
    private String oidcAuth;

    @Column(name = "OIDC_KEYSET", length = 1024, nullable = true)
    private String oidcKeyset;

    @Column(name = "OIDC_TOKEN", length = 1024, nullable = true)
    private String oidcToken;

	// This is usually optional except for D2L
    @Column(name = "OIDC_AUDIENCE", length = 1024, nullable = true)
    private String oidcAudience;

    @Column(name = "CACHE_KID", length = 1024, nullable = true)
    private String cache_kid;

    @Column(name = "CACHE_PUBKEY", length = 1024, nullable = true)
    private String cache_pubkey;

	@Lob
    @Column(name = "CACHE_KEYSET", nullable = true)
    private String cache_keyset;

	public boolean isDraft()
	{
		if ( issuer == null || clientId == null || deploymentId == null ||
				oidcAuth == null || oidcKeyset == null || oidcToken == null ) return true;

		if ( issuer.length() < 1 || clientId.length() < 1 ||
				deploymentId.length() < 1 || oidcAuth.length() < 1 ||
				oidcKeyset.length() < 1 || oidcToken.length() < 1 ) return true;
		return false;
	}

}
