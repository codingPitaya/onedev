package io.onedev.server.web.page.project.pullrequests.detail.activities.activity;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.PullRequestCommentManager;
import io.onedev.server.model.Project;
import io.onedev.server.model.PullRequest;
import io.onedev.server.model.PullRequestComment;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.util.DateUtils;
import io.onedev.server.util.facade.UserFacade;
import io.onedev.server.util.userident.UserIdent;
import io.onedev.server.web.component.markdown.AttachmentSupport;
import io.onedev.server.web.component.markdown.ContentVersionSupport;
import io.onedev.server.web.component.project.comment.ProjectCommentPanel;
import io.onedev.server.web.page.project.pullrequests.detail.activities.SinceChangesLink;
import io.onedev.server.web.util.DeleteCallback;
import io.onedev.server.web.util.ProjectAttachmentSupport;

@SuppressWarnings("serial")
class PullRequestCommentedPanel extends GenericPanel<PullRequestComment> {

	private final DeleteCallback deleteCallback;
	
	public PullRequestCommentedPanel(String id, IModel<PullRequestComment> model, DeleteCallback deleteCallback) {
		super(id, model);
		this.deleteCallback = deleteCallback;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		UserIdent userIdent = UserIdent.of(UserFacade.of(getComment().getUser()), getComment().getUserName());
		add(new Label("user", userIdent.getName()));
		add(new Label("age", DateUtils.formatAge(getComment().getDate())));
		
		add(new SinceChangesLink("changes", new AbstractReadOnlyModel<PullRequest>() {

			@Override
			public PullRequest getObject() {
				return getComment().getRequest();
			}

		}, getComment().getDate()));
		
		add(new ProjectCommentPanel("body") {

			@Override
			protected String getComment() {
				return PullRequestCommentedPanel.this.getComment().getContent();
			}

			@Override
			protected void onSaveComment(AjaxRequestTarget target, String comment) {
				PullRequestCommentedPanel.this.getComment().setContent(comment);
				OneDev.getInstance(PullRequestCommentManager.class).save(PullRequestCommentedPanel.this.getComment());
			}

			@Override
			protected Project getProject() {
				return PullRequestCommentedPanel.this.getComment().getProject();
			}

			@Override
			protected AttachmentSupport getAttachmentSupport() {
				return new ProjectAttachmentSupport(PullRequestCommentedPanel.this.getComment().getProject(), 
						PullRequestCommentedPanel.this.getComment().getRequest().getUUID());
			}

			@Override
			protected boolean canModifyOrDeleteComment() {
				return SecurityUtils.canModifyOrDelete(PullRequestCommentedPanel.this.getComment());
			}

			@Override
			protected String getRequiredLabel() {
				return "Comment";
			}

			@Override
			protected ContentVersionSupport getContentVersionSupport() {
				return null;
			}

			@Override
			protected DeleteCallback getDeleteCallback() {
				return deleteCallback;
			}
			
		});
		
		setOutputMarkupId(true);
	}
	
	private PullRequestComment getComment() {
		return getModelObject();
	}
	
}
