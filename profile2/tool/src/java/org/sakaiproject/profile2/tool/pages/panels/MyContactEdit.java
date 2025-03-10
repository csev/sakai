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
package org.sakaiproject.profile2.tool.pages.panels;



import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.apache.wicket.validation.validator.UrlValidator;
import org.sakaiproject.api.common.edu.person.SakaiPerson;
import org.sakaiproject.profile2.logic.ProfileLogic;
import org.sakaiproject.profile2.logic.SakaiProxy;
import org.sakaiproject.profile2.model.UserProfile;
import org.sakaiproject.profile2.tool.components.ComponentVisualErrorBehaviour;
import org.sakaiproject.profile2.tool.components.ErrorLevelsFeedbackMessageFilter;
import org.sakaiproject.profile2.tool.components.FeedbackLabel;
import org.sakaiproject.profile2.tool.components.PhoneNumberValidator;
import org.sakaiproject.profile2.util.ProfileConstants;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MyContactEdit extends Panel {

	private static final long serialVersionUID = 1L;

	@SpringBean(name="org.sakaiproject.profile2.logic.SakaiProxy")
	private SakaiProxy sakaiProxy;
	
	@SpringBean(name="org.sakaiproject.profile2.logic.ProfileLogic")
	private ProfileLogic profileLogic;
	
    public MyContactEdit(final String id, final UserProfile userProfile) {
		super(id);
		
        log.debug("MyContactEdit()");

		//this panel
		final Component thisPanel = this;
			
		//get userId
		final String userId = userProfile.getUserUuid();
		
		//heading
		add(new Label("heading", new ResourceModel("heading.contact.edit")));
		
		//setup form	
		Form form = new Form("form", new Model(userProfile));
		form.setOutputMarkupId(true);
		
		//form submit feedback
		final Label formFeedback = new Label("formFeedback");
		formFeedback.setOutputMarkupPlaceholderTag(true);
		form.add(formFeedback);
		
		//add warning message if superUser and not editing own profile
		Label editWarning = new Label("editWarning");
		editWarning.setVisible(false);
		if(sakaiProxy.isSuperUserAndProxiedToUser(userId)) {
			editWarning.setDefaultModel(new StringResourceModel("text.edit.other.warning").setParameters(userProfile.getDisplayName()));
			editWarning.setEscapeModelStrings(false);
			editWarning.setVisible(true);
		}
		form.add(editWarning);
		
		//We don't need to get the info from userProfile, we load it into the form with a property model
	    //just make sure that the form element id's match those in the model
	   	
	    // FeedbackPanel
        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        form.add(feedback);

        // filteredErrorLevels will not be shown in the FeedbackPanel
        int[] filteredErrorLevels = new int[]{FeedbackMessage.ERROR};
        feedback.setFilter(new ErrorLevelsFeedbackMessageFilter(filteredErrorLevels));
	    
		//email
		WebMarkupContainer emailContainer = new WebMarkupContainer("emailContainer");
		emailContainer.add(new Label("emailLabel", new ResourceModel("profile.email")));
		final TextField email = new TextField("email", new PropertyModel(userProfile, "email"));
		email.setOutputMarkupId(true);
		email.setMarkupId("emailinput");
		email.add(EmailAddressValidator.getInstance());
		//readonly view
		Label emailReadOnly = new Label("emailReadOnly", new PropertyModel(userProfile, "email"));
		
		if(sakaiProxy.isAccountUpdateAllowed(userId)) {
			emailReadOnly.setVisible(false);
		} else {
			email.setVisible(false);
		}
		emailContainer.add(email);
		emailContainer.add(emailReadOnly);
		
		//email feedback
        final FeedbackLabel emailFeedback = new FeedbackLabel("emailFeedback", email);
        emailFeedback.setMarkupId("emailFeedback");
        emailFeedback.setOutputMarkupId(true);
        emailContainer.add(emailFeedback);
        email.add(new ComponentVisualErrorBehaviour("blur", emailFeedback));
		form.add(emailContainer);
		
		//mobilephone
		WebMarkupContainer mobilephoneContainer = new WebMarkupContainer("mobilephoneContainer");
		mobilephoneContainer.add(new Label("mobilephoneLabel", new ResourceModel("profile.phone.mobile")));
		final TextField<String> mobilephone = new TextField<>("mobilephone", new PropertyModel<>(userProfile, "mobilephone"));
		mobilephone.setMarkupId("mobilephoneinput");
        mobilephone.setOutputMarkupId(true);
		mobilephone.add(new PhoneNumberValidator());
		mobilephoneContainer.add(mobilephone);
		
		//mobilephone feedback
        final FeedbackLabel mobilephoneFeedback = new FeedbackLabel("mobilephoneFeedback", mobilephone);
        mobilephoneFeedback.setMarkupId("mobilephoneFeedback");
        mobilephoneFeedback.setOutputMarkupId(true);
        mobilephoneContainer.add(mobilephoneFeedback);
        mobilephone.add(new ComponentVisualErrorBehaviour("blur", mobilephoneFeedback));
		form.add(mobilephoneContainer);
		
		//submit button
		AjaxFallbackButton submitButton = new AjaxFallbackButton("submit", new ResourceModel("button.save.changes"), form) {
			@Override
			protected void onSubmit(Optional<AjaxRequestTarget> targetOptional) {
				//save() form, show message, then load display panel
				if(save(form)) {
					
					//post update event
					sakaiProxy.postEvent(ProfileConstants.EVENT_PROFILE_CONTACT_UPDATE, "/profile/"+userId, true);
					
					//repaint panel
					Component newPanel = new MyContactDisplay(id, userProfile);
					newPanel.setOutputMarkupId(true);
					thisPanel.replaceWith(newPanel);
					targetOptional.ifPresent(target -> {
						target.add(newPanel);
						//resize iframe
						target.appendJavaScript("setMainFrameHeight(window.name);");
					});
				
				} else {
					//String js = "alert('Failed to save information. Contact your system administrator.');";
					//target.prependJavascript(js);
					targetOptional.ifPresent(target -> {
						formFeedback.setDefaultModel(new ResourceModel("error.profile.save.contact.failed"));
						formFeedback.add(new AttributeModifier("class", new Model<String>("save-failed-error")));
						target.add(formFeedback);
					});
				}
				
				
            }
			
			// This is called if the form validation fails, ie Javascript turned off, 
			//or we had preexisting invalid data before this fix was introduced
			@Override
			protected void onError(Optional<AjaxRequestTarget> targetOptional) {
				targetOptional.ifPresent(target -> {
					//check which item didn't validate and update the class and feedback model for that component
					if (!email.isValid()) {
						email.add(new AttributeAppender("class", new Model<String>("invalid"), " "));
						emailFeedback.setDefaultModel(new ResourceModel("EmailAddressValidator"));
						target.add(email);
						target.add(emailFeedback);
					}
					if (!mobilephone.isValid()) {
						mobilephone.add(new AttributeAppender("class", new Model<String>("invalid"), " "));
						mobilephoneFeedback.setDefaultModel(new ResourceModel("PhoneNumberValidator"));
						target.add(mobilephone);
						target.add(mobilephoneFeedback);
					}
				});
			}
		};
		form.add(submitButton);
        
		//cancel button
		AjaxFallbackButton cancelButton = new AjaxFallbackButton("cancel", new ResourceModel("button.cancel"), form) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(Optional<AjaxRequestTarget> targetOptional) {
            	Component newPanel = new MyContactDisplay(id, userProfile);
				newPanel.setOutputMarkupId(true);
				thisPanel.replaceWith(newPanel);
				targetOptional.ifPresent(target -> {
					target.add(newPanel);
					//resize iframe
					target.appendJavaScript("setMainFrameHeight(window.name);");
				});
            	
            }
        };
        cancelButton.setDefaultFormProcessing(false);
        form.add(cancelButton);
		
		//add form to page
		add(form);
	}
	
	//called when the form is to be saved
	private boolean save(Form form) {
		
		//get the backing model
		UserProfile userProfile = (UserProfile) form.getModelObject();
		
		//get userId from the UserProfile (because admin could be editing), then get existing SakaiPerson for that userId
		String userId = userProfile.getUserUuid();
		SakaiPerson sakaiPerson = sakaiProxy.getSakaiPerson(userId);
	
		//set the attributes from userProfile that this form dealt with, into sakaiPerson
		//this WILL fail if there is no sakaiPerson for the user however this should have been caught already
		//as a new Sakaiperson for a user is created in MyProfile if they don't have one.
		
		sakaiPerson.setMobile(userProfile.getMobilephone()); //mobilephone

		if(profileLogic.saveUserProfile(sakaiPerson)) {
            log.info("Saved SakaiPerson for: {}", userId);
			
			//update their email address in their account if allowed
			if(sakaiProxy.isAccountUpdateAllowed(userId)) {
				sakaiProxy.updateEmailForUser(userId, userProfile.getEmail());
			}
						
			return true;
		} else {
            log.info("Couldn't save SakaiPerson for: {}", userId);
			return false;
		}
	}
}
