/*
 * Copyright (c) 2003-2021 The Apereo Foundation
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
package org.sakaiproject.plus.tool;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import org.sakaiproject.portal.util.PortalUtils;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.plus.tool.exception.MissingSessionException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;

import org.sakaiproject.lti.api.LTIService;
import org.sakaiproject.basiclti.util.SakaiBLTIUtil;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

import org.sakaiproject.plus.api.PlusService;
import org.sakaiproject.plus.api.model.Tenant;
import org.sakaiproject.plus.api.model.Context;
import org.sakaiproject.plus.api.model.ContextLog;
import org.sakaiproject.plus.api.repository.TenantRepository;
import org.sakaiproject.plus.api.repository.ContextRepository;
import org.sakaiproject.plus.api.repository.ContextLogRepository;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class MainController {

	@Resource
	private SessionManager sessionManager;

	@Resource
	private ToolManager toolManager;

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private ContextRepository contextRepository;

	@Autowired
	private ContextLogRepository contextLogRepository;

	@Autowired
	private LTIService ltiService;

	@Autowired
	private ServerConfigurationService serverConfigurationService;

	@Autowired
	private PlusService plusService;

	@GetMapping(value = {"/", "/index"})
	public String pageIndex(Model model, HttpServletRequest request) {

		checkSakaiSession();

		loadModel(model, request);
		Iterable<Tenant> tenants = tenantRepository.findAll();
		model.addAttribute("tenants", tenants);
		model.addAttribute("enabled",  Boolean.valueOf(plusService.enabled()));
		return "index";
	}

	@GetMapping(value = {"/create"})
	public String createTenant(Model model, HttpServletRequest request) {

		checkSakaiSession();
		loadModel(model, request);

		Tenant tenant = new Tenant();
		model.addAttribute("tenant", tenant);
		model.addAttribute("doUpdate", Boolean.FALSE);
		return "form";
	}


	@PostMapping("/tenant")
	public String submitForm(@ModelAttribute("tenant") Tenant tenant) {

		Tenant editTenant = null;
		String oldTenantId = tenant.getId();
		if ( oldTenantId != null ) {
			Optional<Tenant> optTenant = tenantRepository.findById(oldTenantId);
			if ( optTenant.isPresent() ) {
				editTenant = optTenant.get();
			}

			editTenant.setTitle(tenant.getTitle());
			editTenant.setDescription(tenant.getDescription());
			editTenant.setIssuer(tenant.getIssuer());
			editTenant.setClientId(tenant.getClientId());
			editTenant.setDeploymentId(tenant.getDeploymentId());
			editTenant.setTrustEmail(tenant.getTrustEmail());
			editTenant.setTimeZone(tenant.getTimeZone());
			editTenant.setAllowedTools(tenant.getAllowedTools());
			editTenant.setNewWindowTools(tenant.getNewWindowTools());
			editTenant.setVerbose(tenant.getVerbose());
			editTenant.setOidcAuth(tenant.getOidcAuth());
			editTenant.setOidcKeySet(tenant.getOidcKeySet());
			editTenant.setOidcToken(tenant.getOidcToken());
			editTenant.setOidcAudience(tenant.getOidcAudience());
			editTenant.setOidcRegistrationLock(tenant.getOidcRegistrationLock());
			tenantRepository.save(editTenant);
			log.info("Updating Plus Tenant id={}", oldTenantId);

		} else {
			tenantRepository.save(tenant);
			log.info("Created Plus Tenant id={}", tenant.getId());
		}
		return "redirect:/";
	}

	@GetMapping(value = "/tenant/{tenantId}")
	public String tenantDetail(Model model, @PathVariable String tenantId, HttpServletRequest request) {

		checkSakaiSession();

		Optional<Tenant> optTenant = tenantRepository.findById(tenantId);
		Tenant tenant = null;
		if ( optTenant.isPresent() ) {
			tenant = optTenant.get();
		}

		loadModel(model, request);
		model.addAttribute("tenant", tenant);

		model.addAttribute("oidcKeySet", plusService.getOidcKeySet());
		model.addAttribute("oidcLogin", plusService.getOidcLogin(tenant));
		model.addAttribute("oidcLaunch", plusService.getOidcLaunch());

		// http://localhost:8080/plus/sakai/dynamic/123?unlock_token=42
		model.addAttribute("imsURL", plusService.getIMSDynamicRegistration(tenant));

		// https://dev1.sakaicloud.com/plus/sakai/canvas-config.json?guid=123456
		model.addAttribute("canvasURL", plusService.getCanvasConfig(tenant));
		return "tenant";
	}

	@GetMapping(value = "/edit/{tenantId}")
	public String tenantEdit(Model model, @PathVariable String tenantId, HttpServletRequest request) {

		checkSakaiSession();

		Optional<Tenant> optTenant = tenantRepository.findById(tenantId);
		Tenant tenant = null;
		if ( optTenant.isPresent() ) {
			tenant = optTenant.get();
		}

		loadModel(model, request);
		model.addAttribute("tenant", tenant);
		model.addAttribute("doUpdate", Boolean.TRUE);
		return "form";
	}

	@GetMapping(value = "/delete/{tenantId}")
	public String tenantDelete(Model model, @PathVariable String tenantId, HttpServletRequest request) {

		checkSakaiSession();

		Optional<Tenant> optTenant = tenantRepository.findById(tenantId);
		Tenant tenant = null;
		if ( optTenant.isPresent() ) {
			tenant = optTenant.get();
		}

		loadModel(model, request);
		model.addAttribute("tenant", tenant);
		return "delete";
	}

	@PostMapping(value = "/delete/{tenantId}")
	public String tenantDeletePost(Model model, @PathVariable String tenantId, HttpServletRequest request) {

		checkSakaiSession();

		Optional<Tenant> optTenant = tenantRepository.findById(tenantId);
		Tenant tenant = null;
		if ( optTenant.isPresent() ) {
			tenant = optTenant.get();
		}
		log.info("Deleteing Plus Tenant id={}", tenantId);

		tenantRepository.deleteById(tenantId);
		return "redirect:/";
	}

	@GetMapping(value = "/contexts/{tenantId}")
	public String contexts(Model model, @PathVariable String tenantId, HttpServletRequest request) {

		checkSakaiSession();
		loadModel(model, request);

		Optional<Tenant> optTenant = tenantRepository.findById(tenantId);
		Tenant tenant = null;
		if ( optTenant.isPresent() ) {
			tenant = optTenant.get();
		}

		model.addAttribute("tenant", tenant);

		List<Context> contexts = null;
		if ( tenant != null ) {
			contexts = contextRepository.findByTenant(tenant);
			model.addAttribute("contexts", contexts);
		}
		return "contexts";
	}

	@GetMapping(value = "/context/{contextId}")
	public String contextDetail(Model model, @PathVariable String contextId, HttpServletRequest request) {

		checkSakaiSession();

		Optional<Context> optContext = contextRepository.findById(contextId);
		Context context = null;
		if ( optContext.isPresent() ) {
			context = optContext.get();
		}

		loadModel(model, request);
		model.addAttribute("tenantId", context.getTenant().getId());
		model.addAttribute("context", context);

		List<ContextLog> failures = contextLogRepository.getLogEntries(context, Boolean.FALSE, 10);
		model.addAttribute("failures", failures);
		List<ContextLog> successes = contextLogRepository.getLogEntries(context, Boolean.TRUE, 10);
		model.addAttribute("successes", successes);

		return "context";
	}

	private void loadModel(Model model, HttpServletRequest request) {

		model.addAttribute("cdnQuery", PortalUtils.getCDNQuery());

		Placement placement = toolManager.getCurrentPlacement();
		model.addAttribute("siteId", placement.getContext());
		String baseUrl = "/portal/site/" + placement.getContext() + "/tool/" + toolManager.getCurrentPlacement().getId();
		model.addAttribute("baseUrl", baseUrl);
		String serverUrl = SakaiBLTIUtil.getOurServerUrl();
		model.addAttribute("serverUrl", serverUrl);
		model.addAttribute("sakaiHtmlHead", (String) request.getAttribute("sakai.html.head"));
	}

	/**
	 * Check for a valid session
	 * if not valid a 403 Forbidden will be returned
	 */
	private Session checkSakaiSession() {

		Session session;
		try {
			session = sessionManager.getCurrentSession();
			if (StringUtils.isBlank(session.getUserId())) {
				log.error("Sakai user session is invalid");
				throw new MissingSessionException();
			}
		} catch (IllegalStateException e) {
			log.error("Could not retrieve the sakai session");
			throw new MissingSessionException(e.getCause());
		}

		Placement placement = toolManager.getCurrentPlacement();
		if ( ! ltiService.isAdmin(placement.getContext()) ) {
			log.error("Attempt to run PlusAdmin outside of admin");
			throw new MissingSessionException("Must be administrator to manage SakaiPlus");
		}

		return session;
	}
}
