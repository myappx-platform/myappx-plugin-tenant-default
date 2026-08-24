package org.idempiere.myappx.plugin.tenant.x;

import org.adempiere.base.AnnotationBasedEventManager;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = {DefaultAnnotationBasedEventManager.class})
public class DefaultAnnotationBasedEventManager extends AnnotationBasedEventManager {

	/**
	 * default constructor
	 */
	public DefaultAnnotationBasedEventManager() {
	}

	@Override
	public String[] getPackages() {
		return new String[] {"org.idempiere.myappx.plugin.tenant.x.event.delegate"};
	}

}
