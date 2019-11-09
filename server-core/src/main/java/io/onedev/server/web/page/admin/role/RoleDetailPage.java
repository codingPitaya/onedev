package io.onedev.server.web.page.admin.role;

import java.io.Serializable;

import org.apache.wicket.Session;
import org.apache.wicket.feedback.FencedFeedbackPanel;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import io.onedev.commons.utils.HtmlUtils;
import io.onedev.server.OneDev;
import io.onedev.server.OneException;
import io.onedev.server.entitymanager.RoleManager;
import io.onedev.server.model.Role;
import io.onedev.server.util.SecurityUtils;
import io.onedev.server.web.editable.BeanContext;
import io.onedev.server.web.editable.BeanEditor;
import io.onedev.server.web.editable.Path;
import io.onedev.server.web.editable.PathNode;
import io.onedev.server.web.model.EntityModel;
import io.onedev.server.web.page.admin.AdministrationPage;
import io.onedev.server.web.util.ConfirmOnClick;

@SuppressWarnings("serial")
public class RoleDetailPage extends AdministrationPage {
	
	private static final String PARAM_ROLE = "role";
	
	protected final IModel<Role> roleModel;
	
	private String oldName;
	
	private BeanEditor editor;
	
	public RoleDetailPage(PageParameters params) {
		super(params);
		
		Long roleId = params.get(PARAM_ROLE).toLong();
		
		roleModel = new EntityModel<Role>(Role.class, roleId);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		editor = BeanContext.editModel("editor", new IModel<Serializable>() {

			@Override
			public void detach() {
			}

			@Override
			public Serializable getObject() {
				return getRole();
			}

			@Override
			public void setObject(Serializable object) {
				oldName = getRole().getName();
				editor.getDescriptor().copyProperties(object, getRole());
			}
			
		});
		
		Form<?> form = new Form<Void>("form") {

			@Override
			protected void onSubmit() {
				super.onSubmit();
				
				Role role = getRole();
				RoleManager roleManager = OneDev.getInstance(RoleManager.class);
				Role roleWithSameName = roleManager.find(role.getName());
				if (roleWithSameName != null && !roleWithSameName.equals(role)) {
					editor.error(new Path(new PathNode.Named("name")),
							"This name has already been used by another role.");
				} 
				if (editor.isValid()) {
					roleManager.save(role, oldName);
					setResponsePage(RoleDetailPage.class, RoleDetailPage.paramsOf(role));
					Session.get().success("Role updated");
				}
			}
			
		};	
		form.add(editor);
		form.add(new FencedFeedbackPanel("feedback", form).setEscapeModelStrings(false));
		
		form.add(new Link<Void>("delete") {

			@Override
			public void onClick() {
				try {
					OneDev.getInstance(RoleManager.class).delete(getRole());
					setResponsePage(RoleListPage.class);
				} catch (OneException e) {
					error(HtmlUtils.formatAsHtml(e.getMessage()));
				}
			}
			
		}.add(new ConfirmOnClick("Do you really want to delete role '" + getRole().getName() + "'?")));
		
		add(form);
	}
	
	@Override
	protected void onDetach() {
		roleModel.detach();
		super.onDetach();
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new RoleCssResourceReference()));
	}
	
	public Role getRole() {
		return roleModel.getObject();
	}
	
	@Override
	protected boolean isPermitted() {
		return SecurityUtils.isAdministrator();
	}
	
	public static PageParameters paramsOf(Role role) {
		PageParameters params = new PageParameters();
		params.add(PARAM_ROLE, role.getId());
		return params;
	}

}