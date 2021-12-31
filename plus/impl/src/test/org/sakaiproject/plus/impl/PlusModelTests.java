/*
 * Copyright (c) 2021- The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *			 http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.plus.impl;

import java.time.Instant;

import org.sakaiproject.plus.api.Launch;
import org.sakaiproject.plus.api.LaunchService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import org.hibernate.SessionFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import static org.mockito.Mockito.*;

import lombok.extern.slf4j.Slf4j;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;



import org.tsugi.lti13.objects.LaunchJWT;
import org.tsugi.lti13.objects.ResourceLink;
// import org.tsugi.lti13.objects.Context; - Need to qualify
import org.tsugi.lti13.objects.ToolPlatform;
import org.tsugi.lti13.objects.LaunchLIS;
import org.tsugi.lti13.objects.BasicOutcome;
import org.tsugi.lti13.objects.Endpoint;

import org.tsugi.lti13.LTI13JwtUtil;

import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

import org.sakaiproject.plus.api.model.Tenant;
import org.sakaiproject.plus.api.model.Subject;
import org.sakaiproject.plus.api.model.Context;
import org.sakaiproject.plus.api.model.Link;
import org.sakaiproject.plus.api.model.LineItem;
import org.sakaiproject.plus.api.model.Score;

import org.sakaiproject.plus.api.repository.TenantRepository;
import org.sakaiproject.plus.api.repository.SubjectRepository;
import org.sakaiproject.plus.api.repository.ContextRepository;
import org.sakaiproject.plus.api.repository.LinkRepository;
import org.sakaiproject.plus.api.repository.LineItemRepository;
import org.sakaiproject.plus.api.repository.ScoreRepository;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PlusTestConfiguration.class})
public class PlusModelTests extends AbstractTransactionalJUnit4SpringContextTests {

	@Resource private SecurityService securityService;
	@Resource private SessionManager sessionManager;
	@Resource private UserDirectoryService userDirectoryService;
	@Resource private SessionFactory sessionFactory;

	@Resource private TenantRepository tenantRepository;
	@Resource private SubjectRepository subjectRepository;
	@Resource private ContextRepository contextRepository;
	@Resource private LinkRepository linkRepository;
	@Resource private LineItemRepository lineItemRepository;
	@Resource private ScoreRepository scoreRepository;
	@Resource private LaunchService launchService;

	User user1User = null;
	User user2User = null;

	@Before
	public void setup() {

		reset(sessionManager);
		reset(securityService);
		reset(userDirectoryService);
		user1User = mock(User.class);
		when(user1User.getDisplayName()).thenReturn("User 1");
		user2User = mock(User.class);
		when(user2User.getDisplayName()).thenReturn("User 2");
	}

	@Test
	public void testModelObjects() {

		Instant now = Instant.now();
		Tenant tenant = new Tenant();
		tenant.setCreated_at(now);
		tenant.setTitle("Yada");
		tenant.setIssuer("https://www.example.com");
		tenant.setClientId("42");
		tenant.setDeploymentId("1");
		tenant.setOidcAuth("https://www.example.com/auth");
		tenant.setOidcKeyset("https://www.example.com/keyset");
		assertTrue(tenant.isDraft());
		tenant.setOidcToken("https://www.example.com/token");
		assertFalse(tenant.isDraft());
		Map<String, String> settings = tenant.getSettings();
		settings.put("secret", "42");
		tenantRepository.save(tenant);
		String tenantId = tenant.getId();

		Optional<Tenant> optTenant = tenantRepository.findById(tenantId);
		if ( optTenant.isPresent() ) {
			Tenant newTenant = optTenant.get();
		}

		Subject subject = new Subject("Yada", tenant);
		subject.setEmail("hirouki@p.com");
		subjectRepository.save(subject);

		Subject newSubject = subjectRepository.findBySubjectAndTenant("Yada", tenant);

		Context context = new Context();
		context.setContext("SI364");
		context.setTenant(tenant);
		contextRepository.save(context);

		Context newContext = contextRepository.findByContextAndTenant("SI364", tenant);

		LineItem lineItem = new LineItem();
		lineItem.setResourceId("YADA");
		lineItem.setContext(context);
		lineItemRepository.save(lineItem);

		Link link = new Link();
		link.setLink("YADA");
		link.setContext(context);
		link.setLineItem(lineItem);
		linkRepository.save(link);

		Score score = new Score();
		score.setLineItem(lineItem);
		score.setSubject(subject);
		scoreRepository.save(score);

	}

	@Test
	public void testReceiveJWT() throws com.fasterxml.jackson.core.JsonProcessingException {
System.out.println("YADA testReceiveJWT");
		String id_token = getIdToken();
		JSONObject header = LTI13JwtUtil.jsonJwtHeader(id_token);
		assertNotNull(header);
		JSONObject body = LTI13JwtUtil.jsonJwtBody(id_token);
		assertNotNull(body);
		String rawbody = LTI13JwtUtil.rawJwtBody(id_token);
		assertNotNull(rawbody);

		// https://www.baeldung.com/jackson-deserialize-json-unknown-properties
		ObjectMapper mapper = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		LaunchJWT launchJWT = mapper.readValue(rawbody, LaunchJWT.class);
		assertNotNull(launchJWT);
System.out.println("launchJWT="+launchJWT);

		// Make sure funky stuff is ignored
		String funkybody = rawbody.replace("{\"iss\":", "{\"funky\":\"town\",\"iss\":");
System.out.println("funkybody="+funkybody);
		launchJWT = mapper.readValue(funkybody, LaunchJWT.class);
		assertNotNull(launchJWT);
System.out.println("launchJWT="+launchJWT);

		Launch launch = launchService.loadLaunchFromJWT(launchJWT);
System.out.println("launch="+launch);

	}

	public String getIdToken()
	{
		return "eyJraWQiOiIxNzkzNTI2OTg4IiwiYWxnIjoiUlMyNTYifQ.eyJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAiLCJhdWQiOiJhMmUwZjU4Yi0xZWFkLTQ3MjAtYWY3MC05OTExOGQwNmI2OTMiLCJzdWIiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvdXNlci80MmNjNTAxYi03ZWFiLTRhYmItOTQwZC1iMGUwZWRkZjUzMmIiLCJub25jZSI6IjYxY2RiMjU3YmNkNTgiLCJpYXQiOjE2NDA4NzA0ODcsImV4cCI6MTY0MDg3NDA4NywiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vZGVwbG95bWVudF9pZCI6IjEiLCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS90YXJnZXRfbGlua191cmkiOiJodHRwOi8vbG9jYWxob3N0Ojg4ODgvcHk0ZS9tb2QvbG1zdGVzdC8iLCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS9tZXNzYWdlX3R5cGUiOiJMdGlSZXNvdXJjZUxpbmtSZXF1ZXN0IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vdmVyc2lvbiI6IjEuMy4wIiwiZ2l2ZW5fbmFtZSI6IkNodWNrIiwiZmFtaWx5X25hbWUiOiJQIiwiZW1haWwiOiJwQHAuY29tIiwibmFtZSI6IkNodWNrIFAiLCJsb2NhbGUiOiJlbl9VUyIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2N1c3RvbSI6eyJhdmFpbGFibGVlbmQiOiIkUmVzb3VyY2VMaW5rLmF2YWlsYWJsZS5lbmREYXRlVGltZSIsImF2YWlsYWJsZXN0YXJ0IjoiJFJlc291cmNlTGluay5hdmFpbGFibGUuc3RhcnREYXRlVGltZSIsImNhbnZhc19jYWxpcGVyX3VybCI6IiRDYWxpcGVyLnVybCIsImNvbnRleHRfaWRfaGlzdG9yeSI6IiRDb250ZXh0LmlkLmhpc3RvcnkiLCJyZXNvdXJjZWxpbmtfaWRfaGlzdG9yeSI6IiRSZXNvdXJjZUxpbmsuaWQuaGlzdG9yeSIsInN1Ym1pc3Npb25lbmQiOiIkUmVzb3VyY2VMaW5rLnN1Ym1pc3Npb24uZW5kRGF0ZVRpbWUiLCJzdWJtaXNzaW9uc3RhcnQiOiIkUmVzb3VyY2VMaW5rLnN1Ym1pc3Npb24uc3RhcnREYXRlVGltZSJ9LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS9yb2xlcyI6WyJodHRwOi8vcHVybC5pbXNnbG9iYWwub3JnL3ZvY2FiL2xpcy92Mi9tZW1iZXJzaGlwI0luc3RydWN0b3IiXSwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vcm9sZV9zY29wZV9tZW50b3IiOltdLCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS9sYXVuY2hfcHJlc2VudGF0aW9uIjp7ImRvY3VtZW50X3RhcmdldCI6ImlmcmFtZSIsInJldHVybl91cmwiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvaW1zb2lkYy9sdGkxMS9yZXR1cm4tdXJsL3NpdGUvNjJjOWQ2NWItODk1OS00M2QyLWI2NjQtNWNmYmZjMjczMzgwIiwiY3NzX3VybCI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9saWJyYXJ5L3NraW4vdG9vbF9iYXNlLmNzcyJ9LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS9yZXNvdXJjZV9saW5rIjp7ImlkIjoiY29udGVudDoxMCIsInRpdGxlIjoiTE1TIFRlc3QiLCJkZXNjcmlwdGlvbiI6IlRoaXMgdG9vbCBleGVyY2lzZXMgdmFyaW91cyBMTVMgQWN0aXZpdGllcy4ifSwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vY29udGV4dCI6eyJpZCI6IjYyYzlkNjViLTg5NTktNDNkMi1iNjY0LTVjZmJmYzI3MzM4MCIsImxhYmVsIjoiWWFkYSIsInRpdGxlIjoiWWFkYSIsInR5cGUiOlsiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvY291cnNlI0NvdXJzZU9mZmVyaW5nIl19LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS90b29sX3BsYXRmb3JtIjp7Im5hbWUiOiJTYWthaSIsImRlc2NyaXB0aW9uIjoibG9jYWxob3N0LnNha2FpbG1zIiwicHJvZHVjdF9mYW1pbHlfY29kZSI6InNha2FpIiwidmVyc2lvbiI6IjIzLVNOQVBTSE9UIn0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2xpcyI6eyJwZXJzb25fc291cmNlZGlkIjoiY3NldiIsImNvdXJzZV9vZmZlcmluZ19zb3VyY2VkaWQiOiI2MmM5ZDY1Yi04OTU5LTQzZDItYjY2NC01Y2ZiZmMyNzMzODAiLCJjb3Vyc2Vfc2VjdGlvbl9zb3VyY2VkaWQiOiI2MmM5ZDY1Yi04OTU5LTQzZDItYjY2NC01Y2ZiZmMyNzMzODAiLCJ2ZXJzaW9uIjpbIjEuMC4wIiwiMS4xLjAiXX0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpLWFncy9jbGFpbS9lbmRwb2ludCI6eyJzY29wZSI6WyJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS1hZ3Mvc2NvcGUvbGluZWl0ZW0iXSwibGluZWl0ZW1zIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL2ltc2JsaXMvbHRpMTMvbGluZWl0ZW1zLzhmZGQyNjcyYzk1MTM1Y2QwNjRkOGFjMjgwNzI3NWE0Njc0MDY4NmY4YjQ5MmYxNzk5NWVhMDAyYzA1YzM4YzA6Ojo2MmM5ZDY1Yi04OTU5LTQzZDItYjY2NC01Y2ZiZmMyNzMzODA6Ojpjb250ZW50OjEwIiwibGluZWl0ZW0iOiJodHRwOi8vbG9jYWxob3N0OjgwODAvaW1zYmxpcy9sdGkxMy9saW5laXRlbS84ZmRkMjY3MmM5NTEzNWNkMDY0ZDhhYzI4MDcyNzVhNDY3NDA2ODZmOGI0OTJmMTc5OTVlYTAwMmMwNWMzOGMwOjo6NjJjOWQ2NWItODk1OS00M2QyLWI2NjQtNWNmYmZjMjczMzgwOjo6Y29udGVudDoxMCJ9LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS1ucnBzL2NsYWltL25hbWVzcm9sZXNlcnZpY2UiOnsiY29udGV4dF9tZW1iZXJzaGlwc191cmwiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvaW1zYmxpcy9sdGkxMy9uYW1lc2FuZHJvbGVzLzhmZGQyNjcyYzk1MTM1Y2QwNjRkOGFjMjgwNzI3NWE0Njc0MDY4NmY4YjQ5MmYxNzk5NWVhMDAyYzA1YzM4YzA6Ojo2MmM5ZDY1Yi04OTU5LTQzZDItYjY2NC01Y2ZiZmMyNzMzODA6Ojpjb250ZW50OjEwIiwic2VydmljZV92ZXJzaW9ucyI6WyIyLjAiXX0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2x0aTFwMSI6eyJ1c2VyX2lkIjoiNDJjYzUwMWItN2VhYi00YWJiLTk0MGQtYjBlMGVkZGY1MzJiIiwib2F1dGhfY29uc3VtZXJfa2V5IjoiNTQzMjEiLCJvYXV0aF9jb25zdW1lcl9rZXlfc2lnbiI6IndtaVM2MWVnck5hQ3hPNHFHUTg1aXA0T3ZRb3pNVDBKcm12a0hDeUxrczA9In0sImh0dHBzOi8vd3d3LnNha2FpbG1zLm9yZy9zcGVjL2x0aS9jbGFpbS9leHRlbnNpb24iOnsic2FrYWlfbGF1bmNoX3ByZXNlbnRhdGlvbl9jc3NfdXJsX2xpc3QiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvbGlicmFyeS9za2luL3Rvb2xfYmFzZS5jc3MsaHR0cDovL2xvY2FsaG9zdDo4MDgwL2xpYnJhcnkvc2tpbi9tb3JwaGV1cy1kZWZhdWx0L3Rvb2wuY3NzP3ZlcnNpb249ZTIzMjhhNmMiLCJzYWthaV9hY2FkZW1pY19zZXNzaW9uIjoiT1RIRVIiLCJzYWthaV9yb2xlIjoibWFpbnRhaW4iLCJzYWthaV9zZXJ2ZXIiOiJodHRwOi8vbG9jYWxob3N0OjgwODAiLCJzYWthaV9zZXJ2ZXJpZCI6Ik1hY0Jvb2stUHJvLTEwNS5sb2NhbCJ9fQ.S5IHPoUKbsOnjhmYOUcJCCfnNsdJGfNeqMuWnDRTApb7b44m-K5PZe5L2AgQkuRHebeu8bqGDZQpWGcEeoXJuBwGUsrQkWR6a95NSGUCiijWe5PsfJ1sGtuS7MqS5QLU-yaW9_x2fChR04LznyVMhsAPf3LLsvyaZm_36S-SpvtJkIyTLUKP0GITRNjmuk325701QUq4iFg2_n8SGR7T8azQxC5TrrkuXVz2kCz_91UG0mOASrq0bJAb6YbWxeF_hK27wKlDGnCkGe3_icXi_GJ7Vh_vQIBwJqAsrzUESJcka73dWL5ZFkfCRdH_pal-nI1jvVNAw3ceUIcas8AyOg";
	}

}
