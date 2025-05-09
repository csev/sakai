/**
 * Copyright (c) 2003-2017 The Apereo Foundation
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
package org.sakaiproject.gradebookng.tool.panels.importExport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

import org.sakaiproject.gradebookng.business.GradeSaveResponse;
import org.sakaiproject.gradebookng.business.model.ProcessedGradeItem;
import org.sakaiproject.gradebookng.business.model.ProcessedGradeItem.Type;
import org.sakaiproject.gradebookng.business.model.ProcessedGradeItemDetail;
import org.sakaiproject.gradebookng.business.util.FormatHelper;
import org.sakaiproject.gradebookng.business.util.EventHelper;
import org.sakaiproject.gradebookng.tool.model.ImportWizardModel;
import org.sakaiproject.gradebookng.tool.pages.GradebookPage;
import org.sakaiproject.gradebookng.tool.pages.ImportExportPage;
import org.sakaiproject.gradebookng.tool.panels.BasePanel;
import org.sakaiproject.grading.api.Assignment;
import org.sakaiproject.grading.api.AssignmentHasIllegalPointsException;
import org.sakaiproject.grading.api.ConflictingAssignmentNameException;
import org.sakaiproject.grading.api.ConflictingExternalIdException;
import org.sakaiproject.grading.api.GradeDefinition;
import org.sakaiproject.grading.api.MessageHelper;
import org.sakaiproject.grading.api.model.Gradebook;
import org.sakaiproject.rubrics.api.RubricsConstants;
import org.sakaiproject.rubrics.api.beans.RubricTransferBean;
import org.sakaiproject.util.ResourceLoader;

/**
 * Confirmation page for what is going to be imported
 */
@Slf4j
public class GradeImportConfirmationStep extends BasePanel {

	private static final long serialVersionUID = 1L;

	private final String panelId;
	private final IModel<ImportWizardModel> model;

	@SuppressWarnings("unchecked")
	private static ResourceLoader RL = new ResourceLoader();

	private final String yes = MessageHelper.getString("importExport.confirmation.yes", RL.getLocale());
	private final String no = MessageHelper.getString("importExport.confirmation.no", RL.getLocale());
	private final String none = MessageHelper.getString("importExport.confirmation.none", RL.getLocale());

	private boolean errors = false;

	public GradeImportConfirmationStep(final String id, final IModel<ImportWizardModel> importWizardModel) {
		super(id);
		this.panelId = id;
		this.model = importWizardModel;
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		// unpack model
		final ImportWizardModel importWizardModel = this.model.getObject();

		final List<ProcessedGradeItem> itemsToUpdate = importWizardModel.getItemsToUpdate();
		final List<ProcessedGradeItem> itemsToModify = importWizardModel.getItemsToModify();

		// note these are sorted alphabetically for now.
		// This may be changed to reflect the original order however any sorting needs to take into account that comments have the same
		// title as the item
		Collections.sort(itemsToUpdate);
		Collections.sort(itemsToModify);
		final Map<ProcessedGradeItem, Assignment> assignmentsToCreate = importWizardModel.getAssignmentsToCreate();

		final Form<?> form = new Form("form");
		add(form);

		// back button
		final AjaxButton backButton = new AjaxButton("backbutton") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit(AjaxRequestTarget target) {

				// clear any previous errors
				final ImportExportPage page = (ImportExportPage) getPage();
				page.clearFeedback();
				page.updateFeedback(target);

				// Create the previous panel
				Component previousPanel;
				if (assignmentsToCreate.size() > 0) {
					previousPanel = new CreateGradeItemStep(GradeImportConfirmationStep.this.panelId, Model.of(importWizardModel));
					((CreateGradeItemStep)previousPanel).setCurrentGradebookAndSite(currentGradebookUid, currentSiteId);
				} else {
					previousPanel = new GradeItemImportSelectionStep(GradeImportConfirmationStep.this.panelId, Model.of(importWizardModel));
					((GradeItemImportSelectionStep)previousPanel).setCurrentGradebookAndSite(currentGradebookUid, currentSiteId);
				}

				// AJAX the previous panel into place
				previousPanel.setOutputMarkupId(true);
				WebMarkupContainer container = page.container;
				container.addOrReplace(previousPanel);
				target.add(container);
			}
		};
		backButton.setDefaultFormProcessing(false);
		form.add(backButton);

		// finish button
		final AjaxButton finishButton = new AjaxButton("finishbutton") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit(AjaxRequestTarget target) {

				EventHelper.postImportBeginEvent(getGradebook());

				final Map<String, Long> assignmentMap = new HashMap<>();
				final List<ProcessedGradeItem> itemsToSave = new ArrayList<>();
				Set<ProcessedGradeItem> errorColumns = new HashSet<>();

				// Create new GB items
				Iterator<Map.Entry<ProcessedGradeItem, Assignment>> itAssignments = assignmentsToCreate.entrySet().iterator();
				while(itAssignments.hasNext()) {
					Map.Entry<ProcessedGradeItem, Assignment> entry = itAssignments.next();
					Assignment assignment = entry.getValue();

					Long assignmentId = null;
					try {
						ProcessedGradeItem pgi = entry.getKey();
						assignmentId = GradeImportConfirmationStep.this.businessService.addAssignment(currentGradebookUid, currentSiteId, assignment);
						Map<String, String> rubricParams = pgi.getRubricParameters();
						if (!rubricParams.isEmpty()) {
							rubricsService.saveRubricAssociation(RubricsConstants.RBCS_TOOL_GRADEBOOKNG, assignmentId.toString(), rubricParams);
						}

						success(MessageHelper.getString("notification.addgradeitem.success", RL.getLocale(), assignment.getName()));

						// set the processedGradeItem's itemId so we can later save scores from the spreadsheet
						pgi.setItemId(assignmentId);

						// since it's new, add this item to the list of items that have grades that need to be written
						itemsToSave.add(pgi);

						// remove the item from the wizard so that we can't edit it again if there are validation errors on other items and we use the back button
						importWizardModel.getItemsToCreate().remove(pgi);
						importWizardModel.setStep(importWizardModel.getStep() - 1);
						importWizardModel.setTotalSteps(importWizardModel.getTotalSteps() - 1);
						itAssignments.remove();
					} catch (final AssignmentHasIllegalPointsException e) {
						getSession().error(new ResourceModel("error.addgradeitem.points").getObject());
						GradeImportConfirmationStep.this.errors = true;
						errorColumns.add(entry.getKey());
					} catch (final ConflictingAssignmentNameException e) {
						String title = assignment.getName();
						if (!StringUtils.isBlank(title)) {
							error(MessageHelper.getString("error.addgradeitem.title.duplicate", RL.getLocale(), title));
						} else {
							error(new ResourceModel("error.addgradeitem.title").getObject());
						}
						GradeImportConfirmationStep.this.errors = true;
						errorColumns.add(entry.getKey());
					} catch (final ConflictingExternalIdException e) {
						getSession().error(new ResourceModel("error.addgradeitem.exception").getObject());
						GradeImportConfirmationStep.this.errors = true;
						errorColumns.add(entry.getKey());
					} catch (final Exception e) {
						getSession().error(new ResourceModel("error.addgradeitem.exception").getObject());
						GradeImportConfirmationStep.this.errors = true;
						errorColumns.add(entry.getKey());
					}

					assignmentMap.put(StringUtils.trim(assignment.getName()), assignmentId);
				}

				// Modify any that need modification
				itemsToModify.forEach(item -> {

					final String itemPoints = FormatHelper.formatGradeFromUserLocale(item.getItemPointValue());
					final Double points = FormatHelper.validateDouble(itemPoints);
					final Assignment assignment = GradeImportConfirmationStep.this.businessService.getAssignment(currentGradebookUid, currentSiteId, item.getItemTitle());
					assignment.setPoints(points);

					try {
						GradeImportConfirmationStep.this.businessService.updateAssignment(currentGradebookUid, currentSiteId, assignment);
					}
					catch (final Exception e) {
						getSession().error(MessageHelper.getString("importExport.error.pointsmodification", RL.getLocale(), assignment.getName()));
						GradeImportConfirmationStep.this.errors = true;
						errorColumns.add(item);
						log.warn("An error occurred updating the assignment", e);
					}

					assignmentMap.put(StringUtils.trim(assignment.getName()), assignment.getId());
				});

				// add/update the data
				itemsToSave.addAll(itemsToUpdate);
				itemsToSave.addAll(itemsToModify);
				itemsToSave.removeAll(errorColumns);

				itemsToSave.forEach(processedGradeItem -> {
					log.debug("Processing item: {}", processedGradeItem);

					// get data; if its an update/modify, this will get the id
					Long assignmentId = processedGradeItem.getItemId();

					// a newly created assignment will have a null ID here and need a lookup from the map to get the ID
					if (assignmentId == null) {

						// if assignment title was modified, we need to use that instead
						final String assignmentTitle = StringUtils.trim((processedGradeItem.getAssignmentTitle() != null)
															? processedGradeItem.getAssignmentTitle() : processedGradeItem.getItemTitle());
						assignmentId = assignmentMap.get(assignmentTitle);
					}
					//TODO if assignmentId is still null, there will be a problem

					// Get the assignment and details
					final Assignment assignment = businessService.getAssignment(currentGradebookUid, currentSiteId, assignmentId);
					final List<ProcessedGradeItemDetail> processedGradeItemDetails = processedGradeItem.getProcessedGradeItemDetails();
					List<GradeDefinition> gradeDefList = new ArrayList<>();
					for (ProcessedGradeItemDetail processedGradeItemDetail : processedGradeItemDetails) {
						GradeDefinition gradeDef = new GradeDefinition();
						gradeDef.setStudentUid(processedGradeItemDetail.getUser().getUserUuid());
						gradeDef.setGrade(FormatHelper.formatGradeForDisplay(processedGradeItemDetail.getGrade()));
						gradeDef.setGradeComment(processedGradeItemDetail.getComment());
						gradeDefList.add(gradeDef);
					}

					final GradeSaveResponse saveResponse = businessService.saveGradesAndCommentsForImport(currentGradebookUid, currentSiteId, assignment, gradeDefList);
					switch (saveResponse) {
						case OK:
							break;
						case ERROR:
							error(new ResourceModel("importExport.error.grade").getObject());
							errors = true;
							break;
						default:
							break;
					}
				});

				final ImportExportPage page = (ImportExportPage) getPage();
				if (!errors) {
					// Clear any previous errors
					page.clearFeedback();
					getSession().success(getString("importExport.confirmation.success"));
					setResponsePage(GradebookPage.class);
				} else {
					// Present errors to the user
					page.updateFeedback(target);
				}

				EventHelper.postImportCompletedEvent(getGradebook(), !GradeImportConfirmationStep.this.errors);
			}
		};
		form.add(finishButton);

		// cancel button
		final AjaxButton cancelButton = new AjaxButton("cancelbutton") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit(AjaxRequestTarget target) {
				// clear any previous errors
				final ImportExportPage page = (ImportExportPage) getPage();
				page.clearFeedback();
				page.updateFeedback(target);
				setResponsePage(ImportExportPage.class);
			}
		};
		cancelButton.setDefaultFormProcessing(false);
		form.add(cancelButton);

		// render items to be updated
		final boolean hasItemsToUpdate = !itemsToUpdate.isEmpty();
		final WebMarkupContainer gradesUpdateContainer = new WebMarkupContainer("grades_update_container") {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return hasItemsToUpdate;
			}
		};
		add(gradesUpdateContainer);

		if (hasItemsToUpdate) {
			final ListView<ProcessedGradeItem> updateList = makeListView("grades_update", itemsToUpdate);
			updateList.setReuseItems(true);
			gradesUpdateContainer.add(updateList);
		}

		// render items to be created
		final boolean hasItemsToCreate = !assignmentsToCreate.isEmpty();
		final WebMarkupContainer gradesCreateContainer = new WebMarkupContainer("grades_create_container") {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return hasItemsToCreate;
			}
		};
		add(gradesCreateContainer);

		if (hasItemsToCreate) {
			final ListView<Assignment> createList = makeAssignmentsToCreateListView("grades_create", new ArrayList<>(assignmentsToCreate.values()));
			createList.setReuseItems(true);
			gradesCreateContainer.add(createList);
		}

		// render items to be modified
		final boolean hasItemsToModify = !itemsToModify.isEmpty();
		final WebMarkupContainer gradesModifyContainer = new WebMarkupContainer("grades_modify_container") {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return hasItemsToModify;
			}
		};
		add(gradesModifyContainer);

		if (hasItemsToModify) {
			final ListView<ProcessedGradeItem> modifyList = makeListView("grades_modify", itemsToModify);
			modifyList.setReuseItems(true);
			gradesModifyContainer.add(modifyList);
		}

	}

	/**
	 * Helper to create a listview for what needs to be shown
	 * 
	 * @param markupId wicket markup id
	 * @param itemList ist of stuff
	 * @return
	 */
	private ListView<ProcessedGradeItem> makeListView(final String markupId, final List<ProcessedGradeItem> itemList) {

		final ListView<ProcessedGradeItem> rval = new ListView<ProcessedGradeItem>(markupId, itemList) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(final ListItem<ProcessedGradeItem> item) {

				final ProcessedGradeItem gradeItem = item.getModelObject();

				String displayTitle = gradeItem.getItemTitle();
				if (gradeItem.getType() == Type.COMMENT) {
					displayTitle = MessageHelper.getString("importExport.confirmation.commentsdisplay", RL.getLocale(), gradeItem.getItemTitle());
				}

				item.add(new Label("title", displayTitle));
				item.add(new Label("points", gradeItem.getItemPointValue()));
			}
		};

		return rval;
	}

	/**
	 * Helper to create a listview for what needs to be shown for new assignments
	 * @param markupId wicket markup id
	 * @param itemList list of Assignments populated by the item creation steps
	 */
	private ListView<Assignment> makeAssignmentsToCreateListView(final String markupId, final List<Assignment> itemList) {
		final ListView<Assignment> rval = new ListView<Assignment>(markupId, itemList) {
			@Override
			protected void populateItem(final ListItem<Assignment> item) {
				final Assignment assignment = item.getModelObject();


				String extraCredit = assignment.getExtraCredit() ? yes : no;
				String dueDate = GradeImportConfirmationStep.this.businessService.formatDate(assignment.getDueDate(), "");
				String releaseToStudents = assignment.getReleased() ? yes : no;
				String includeInCourseGrades = assignment.getCounted() ? yes : no;

				item.add(new Label("title", assignment.getName()));
				item.add(new Label("points", assignment.getPoints()));
				item.add(new Label("extraCredit", extraCredit));
				item.add(new Label("dueDate", dueDate));
				item.add(new Label("releaseToStudents", releaseToStudents));
				item.add(new Label("includeInCourseGrades", includeInCourseGrades));

				ImportWizardModel importWizardModel = GradeImportConfirmationStep.this.model.getObject();

				Optional<ProcessedGradeItem> optPgi
					= importWizardModel.getAssignmentsToCreate().keySet().stream()
						.filter(e -> e.getItemTitle().equals(assignment.getName())).findFirst();

				String rubricTitle = none;
				if (optPgi.isPresent()) {
					String rubricId = optPgi.get().getRubricParameters().get(RubricsConstants.RBCS_LIST);
					if (rubricId != null) {
						try {
							Optional<RubricTransferBean> optRubric = rubricsService.getRubric(Long.parseLong(rubricId));
							if (optRubric.isPresent()) {
								rubricTitle = optRubric.get().getTitle();
							}
						} catch (Exception e) {
							log.error("Failed to get rubric for id {}", rubricId, e);
						}
					}
				}
				item.add(new Label("rubricId", rubricTitle));
			}
		};

		return rval;
	}
}
