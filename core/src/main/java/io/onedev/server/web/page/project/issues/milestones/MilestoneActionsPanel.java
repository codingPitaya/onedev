package io.onedev.server.web.page.project.issues.milestones;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.MilestoneManager;
import io.onedev.server.model.Milestone;
import io.onedev.server.web.component.milestone.closelink.MilestoneCloseLink;
import io.onedev.server.web.component.milestone.deletelink.MilestoneDeleteLink;

@SuppressWarnings("serial")
abstract class MilestoneActionsPanel extends GenericPanel<Milestone> {

	public MilestoneActionsPanel(String id, IModel<Milestone> model) {
		super(id, model);
	}

	private Milestone getMilestone() {
		return getModelObject();
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new AjaxLink<Void>("reopen") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				Milestone milestone = getMilestone();
				milestone.setClosed(false);
				OneDev.getInstance(MilestoneManager.class).save(milestone);
				target.add(MilestoneActionsPanel.this);
				onUpdated(target);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(getMilestone().isClosed());
			}
			
		});
		
		add(new MilestoneCloseLink("close") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!getMilestone().isClosed());
			}

			@Override
			protected Milestone getMilestone() {
				return MilestoneActionsPanel.this.getMilestone();
			}

			@Override
			protected void onMilestoneClosed(AjaxRequestTarget target) {
				target.add(MilestoneActionsPanel.this);
				onUpdated(target);
			}
			
		});
		
		add(new BookmarkablePageLink<Void>("edit", MilestoneEditPage.class, 
				MilestoneEditPage.paramsOf(getMilestone())));

		add(new MilestoneDeleteLink("delete") {

			@Override
			protected Milestone getMilestone() {
				return MilestoneActionsPanel.this.getMilestone();
			}

			@Override
			protected void onMilestoneDeleted(AjaxRequestTarget target) {
				target.add(MilestoneActionsPanel.this);
				onDeleted(target);
			}
			
		});		
		setOutputMarkupId(true);
	}

	protected abstract void onDeleted(AjaxRequestTarget target);
	
	protected abstract void onUpdated(AjaxRequestTarget target);
	
}
