package org.idempiere.myappx.plugin.tenant.x.event.delegate;

import org.adempiere.base.annotation.EventTopicDelegate;
import org.adempiere.base.annotation.ModelEventTopic;
import org.adempiere.base.event.annotations.ModelEventDelegate;
import org.adempiere.base.event.annotations.po.AfterNew;
import org.compiere.model.MUserPreference;
import org.osgi.service.event.Event;

@EventTopicDelegate
@ModelEventTopic(modelClass = MUserPreference.class)
public class UpdateUserPreferenceEventDelegate extends ModelEventDelegate<MUserPreference> {

	public UpdateUserPreferenceEventDelegate(MUserPreference po, Event event) {
		super(po, event);
	}
	

	@AfterNew
	public void update2default() {
		MUserPreference up = getModel();
	}

}
