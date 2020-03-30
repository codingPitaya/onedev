package io.onedev.server.web.page.project.issues.milestones;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.MilestoneManager;
import io.onedev.server.model.Milestone;
import io.onedev.server.model.Project;
import io.onedev.server.search.entity.issue.IssueQuery;
import io.onedev.server.search.entity.issue.MilestoneCriteria;
import io.onedev.server.search.entity.issue.StateCriteria;
import io.onedev.server.util.SecurityUtils;
import io.onedev.server.web.component.MultilineLabel;
import io.onedev.server.web.component.issue.list.IssueListPanel;
import io.onedev.server.web.component.issue.statestats.StateStatsBar;
import io.onedev.server.web.component.link.ViewStateAwarePageLink;
import io.onedev.server.web.component.milestone.MilestoneDueLabel;
import io.onedev.server.web.component.milestone.MilestoneStatusLabel;
import io.onedev.server.web.page.project.issues.ProjectIssuesPage;
import io.onedev.server.web.util.PagingHistorySupport;

@SuppressWarnings("serial")
public class MilestoneDetailPage extends ProjectIssuesPage {

	private static final String PARAM_MILESTONE = "milestone";
	
	private static final String PARAM_QUERY = "query";
	
	private static final String PARAM_PAGE = "page";
	
	private final IModel<Milestone> milestoneModel;
	
	private String query;
	
	public MilestoneDetailPage(PageParameters params) {
		super(params);
		
		String idString = params.get(PARAM_MILESTONE).toString();
		if (StringUtils.isBlank(idString))
			throw new RestartResponseException(MilestoneListPage.class, MilestoneListPage.paramsOf(getProject(), false, null));
		
		Long milestoneId = Long.valueOf(idString);
		milestoneModel = new LoadableDetachableModel<Milestone>() {

			@Override
			protected Milestone load() {
				return OneDev.getInstance(MilestoneManager.class).load(milestoneId);
			}
			
		};
		
		query = params.get(PARAM_QUERY).toString();
	}

	private Milestone getMilestone() {
		return milestoneModel.getObject();
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		add(new Label("name", getMilestone().getName()));
		
		add(new MilestoneStatusLabel("status", milestoneModel));
		
		add(new MilestoneDueLabel("due", milestoneModel));
		
		add(new MilestoneActionsPanel("actions", milestoneModel, true) {

			@Override
			protected void onDeleted(AjaxRequestTarget target) {
				setResponsePage(MilestoneDetailPage.class, MilestoneDetailPage.paramsOf(getMilestone(), query));
			}

			@Override
			protected void onUpdated(AjaxRequestTarget target) {
				setResponsePage(MilestoneDetailPage.class, MilestoneDetailPage.paramsOf(getMilestone(), query));
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(SecurityUtils.canManageIssues(getProject()));
			}

		});
		
		add(new MultilineLabel("description", getMilestone().getDescription()) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(getMilestone().getDescription() != null);
			}
			
		});
		
		add(new StateStatsBar("issueStats", new LoadableDetachableModel<Map<String, Integer>>() {

			@Override
			protected Map<String, Integer> load() {
				return getMilestone().getStateStats();
			}
			
		}) {

			@Override
			protected Link<Void> newStateLink(String componentId, String state) {
				String query = new IssueQuery(new StateCriteria(state)).toString();
				PageParameters params = MilestoneDetailPage.paramsOf(getMilestone(), query);
				return new ViewStateAwarePageLink<Void>(componentId, MilestoneDetailPage.class, params);
			}
			
		});
		
		add(newIssueList());
	}
	
	private IssueListPanel newIssueList() {
		return new IssueListPanel("issues", query) {
				
			@Override
			protected IssueQuery getBaseQuery() {
				return new IssueQuery(new MilestoneCriteria(getMilestone().getName()), new ArrayList<>());
			}

			@Override
			protected PagingHistorySupport getPagingHistorySupport() {
				return new PagingHistorySupport() {
					
					@Override
					public PageParameters newPageParameters(int currentPage) {
						PageParameters params = paramsOf(getMilestone(), query);
						params.add(PARAM_PAGE, currentPage+1);
						return params;
					}
					
					@Override
					public int getCurrentPage() {
						return getPageParameters().get(PARAM_PAGE).toInt(1)-1;
					}
					
				};
			}

			@Override
			protected void onQueryUpdated(AjaxRequestTarget target, String query) {
				CharSequence url = RequestCycle.get().urlFor(MilestoneDetailPage.class, paramsOf(getMilestone(), query));
				MilestoneDetailPage.this.query = query;
				pushState(target, url.toString(), query);
			}

			@Override
			protected Project getProject() {
				return MilestoneDetailPage.this.getProject();
			}

		};
	}
	
	@Override
	protected void onPopState(AjaxRequestTarget target, Serializable data) {
		query = (String) data;
		IssueListPanel listPanel = newIssueList();
		replace(listPanel);
		target.add(listPanel);
	}
	
	@Override
	protected void onDetach() {
		milestoneModel.detach();
		super.onDetach();
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new MilestonesResourceReference()));
	}

	public static PageParameters paramsOf(Milestone milestone, @Nullable String query) {
		PageParameters params = paramsOf(milestone.getProject());
		params.add(PARAM_MILESTONE, milestone.getId());
		if (query != null)
			params.add(PARAM_QUERY, query);
		return params;
	}
	
}
