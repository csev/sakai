/*
 * Copyright (c) 2003-2021 The Apereo Foundation
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

import java.util.Map;
import java.util.HashMap;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.Embeddable;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class BaseLTI implements Serializable {

    @Column(name = "CREATOR", length = 99)
    private String creator;

    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false)
    private Instant created_at;

    @Column(name = "MODIFIER", length = 99)
    private String modifier;

    @Column(name = "MODIFIED_AT")
    private Instant modified_at;

    @Column(name = "DELETED", length = 99)
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "DELETOR", length = 99)
    private String deletor;

    @Column(name = "DELETED_AT")
    private Instant deleted_at;

    @Column(name = "LOGIN_COUNT")
    private Integer login_count;

    @Column(name = "LOGIN_IP", length=64)
    private String login_ip;

    @Column(name = "LOGIN_USER", length = 99)
    private String login_user;

    @Column(name = "LOGIN_AT")
    private Instant login_at;

    @Column(name = "SETTINGS")
    private Map<String,String> settings = new HashMap<String, String> ();

    @Lob
    @Column(name = "JSON")
    private String json;

}
