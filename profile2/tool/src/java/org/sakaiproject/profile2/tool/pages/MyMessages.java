/**
 * Copyright (c) 2008-2012 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.profile2.tool.pages;


import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.ResourceModel;
import org.sakaiproject.profile2.tool.pages.panels.ComposeNewMessage;
import org.sakaiproject.profile2.tool.pages.panels.MessageThreadsView;
import org.sakaiproject.profile2.tool.pages.panels.MessageView;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MyMessages extends BasePage {

	private Panel tabPanel;
	
	public MyMessages() {
		renderMyMessages(null);
	}
	
	
	public MyMessages(final String threadId) {
		renderMyMessages(threadId);
	}


	private void renderMyMessages(final String threadId) {

        log.debug("MyMessages( {})", threadId);
		
		disableLink(myMessagesLink);
		
		//get user 
		final String currentUserUuid = sakaiProxy.getCurrentUserId();
		
		
		//action buttons
		Form<Void> tabs = new Form<Void>("tabs");
		RepeatingView buttons = new RepeatingView("repeater");
				
		buttons.add(new AjaxFallbackButton(buttons.newChildId(), new ResourceModel("link.messages.mymessages"), tabs) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit(Optional<AjaxRequestTarget> targetOptional) {
				log.debug("Showing message list");
				targetOptional.ifPresent(target -> {
					switchContentPanel(new MessageThreadsView("tabPanel"), target);
				});
			}
		});
		buttons.add(new AjaxFallbackButton(buttons.newChildId(), new ResourceModel("link.messages.compose"), tabs) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit(Optional<AjaxRequestTarget> targetOptional) {
				log.debug("Showing compose panel");
				targetOptional.ifPresent(target -> {
					switchContentPanel(new ComposeNewMessage("tabPanel"), target);
				});
			}
		});
		
		tabs.add(buttons);
		add(tabs);
		
		if(StringUtils.isNotBlank(threadId)){
			//default view for viewing message
			tabPanel = new MessageView("tabPanel", currentUserUuid, threadId);
		} else {
			//default view for viewing threads
			tabPanel = new MessageThreadsView("tabPanel");
		}
		
		tabPanel.setOutputMarkupId(true);
		add(tabPanel);
		
	}
	
	
	private void switchContentPanel(Panel replacement, AjaxRequestTarget target) {
		
		replacement.setOutputMarkupId(true);
		tabPanel.replaceWith(replacement);
		if(target != null) {
			target.add(replacement);
			//resize iframe
			target.appendJavaScript("setMainFrameHeight(window.name);");
		}
		
		//must keep reference up to date
		tabPanel=replacement;
		
	}
	
	
}



