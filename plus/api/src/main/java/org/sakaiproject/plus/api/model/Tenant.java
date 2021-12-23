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
    private String client_id;

    @Column(name = "OIDC_AUTH", length = 1024, nullable = true)
    private String oidc_auth;

    @Column(name = "OIDC_KEYSET", length = 1024, nullable = true)
    private String oidc_keyset;

    @Column(name = "OIDC_TOKEN", length = 1024, nullable = true)
    private String oidc_token;

	// This is usually optional except for D2L 
    @Column(name = "OIDC_AUDIENCE", length = 1024, nullable = true)
    private String oidc_audience;

    @Column(name = "CACHE_KID", length = 1024, nullable = true)
    private String cache_kid;

    @Column(name = "CACHE_PUBKEY", length = 1024, nullable = true)
    private String cache_pubkey;

	@Lob
    @Column(name = "CACHE_KEYSET", nullable = true)
    private String cache_keyset;

	public boolean isDraft()
	{
		if ( issuer == null || client_id == null || oidc_auth == null || 
				oidc_keyset == null || oidc_token == null ) return true;

		if ( issuer.length() < 1 || client_id.length() < 1 || oidc_auth.length() < 1 || 
				oidc_keyset.length() < 1 || oidc_token.length() < 1 ) return true;
		return false;
	}
/*
"create table {$CFG->dbprefix}lti_key (
    key_id              INTEGER NOT NULL AUTO_INCREMENT,
    key_title           TEXT NULL,
    key_sha256          CHAR(64) NULL,

    deleted             TINYINT(1) NOT NULL DEFAULT 0,

    -- Issuer / client_id / deployment_id defines a client (i.e. who -- pays the bill)

    deploy_sha256       CHAR(64) NULL,
    deploy_key          TEXT NULL,     -- deployment_id renamed

    -- But if the issuer is not pre-existing during dynamic configuration,
    -- we leave issuer_id null -- and store the security arrangement data
    -- here in the key.  The user never touches these LTI 1.3 fields in the
    -- management UI These columns are explicitly *not* named the same as the
    -- fields in the lti_issuers table so as to allow LEFT JOIN and COALESCE
    -- to be easily used and to make sure we are doing the right things
    -- to the right tables.

    -- Issuer is not unique - especially in single instance cloud LMS systems
    -- Issuer / client_id uniquely identifies a security arrangement
    -- But because Tsugi forces oidc_login and oidc_launch to a URL that
    -- includes key_id, we can just look up the proper row in this table by PK

    lms_issuer           TEXT NULL,  -- iss from the JWT
    lms_issuer_sha256    CHAR(64) NULL,
    lms_client           TEXT NULL,  -- aud from the JWT / client_id in OAuth

    lms_oidc_auth       TEXT NULL,
    lms_keyset_url      TEXT NULL,
    lms_token_url       TEXT NULL,
    lms_token_audience  TEXT NULL,

    -- Our cache of the LMS data
    lms_cache_keyset    TEXT NULL,
    lms_cache_pubkey    TEXT NULL,
    lms_cache_kid       TEXT NULL,

    xapi_url            TEXT NULL,
    xapi_user           TEXT NULL,
    xapi_password       TEXT NULL,

    caliper_url         TEXT NULL,
    caliper_key         TEXT NULL,

    json                MEDIUMTEXT NULL,
    user_json           MEDIUMTEXT NULL,
    settings            MEDIUMTEXT NULL,
    settings_url        TEXT NULL,
    entity_version      INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NULL,
    deleted_at          TIMESTAMP NULL,
    login_at            TIMESTAMP NULL,
    login_count         BIGINT DEFAULT 0,
    login_time          BIGINT DEFAULT 0,

    CONSTRAINT `{$CFG->dbprefix}lti_key_ibfk_1`
        FOREIGN KEY (`issuer_id`)
        REFERENCES `{$CFG->dbprefix}lti_issuer` (`issuer_id`)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT `{$CFG->dbprefix}lti_key_const_1` UNIQUE(key_sha256, deploy_sha256),
    CONSTRAINT `{$CFG->dbprefix}lti_key_const_2` UNIQUE(issuer_id, deploy_sha256),
    CONSTRAINT `{$CFG->dbprefix}lti_key_const_pk` PRIMARY KEY (key_id)
 ) ENGINE = InnoDB DEFAULT CHARSET=utf8"),

 */
}
