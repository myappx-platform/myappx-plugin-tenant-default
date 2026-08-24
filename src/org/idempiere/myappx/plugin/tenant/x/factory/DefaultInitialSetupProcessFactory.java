/***********************************************************************
 * This file is part of iDempiere ERP Open Source                      *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 **********************************************************************/
package org.idempiere.myappx.plugin.tenant.x.factory;

import org.adempiere.base.AnnotationBasedProcessFactory;
import org.adempiere.base.IProcessFactory;

/**
 * Registers {@link org.compiere.process.ProcessCall} classes in
 * {@code org.idempiere.myappx.plugin.tenant.x.process} (e.g. {@link org.idempiere.myappx.plugin.tenant.x.process.CustomInitialClientSetup}).
 */
public class DefaultInitialSetupProcessFactory extends AnnotationBasedProcessFactory {

	public DefaultInitialSetupProcessFactory() {
	}

	@Override
	protected String[] getPackages() {
		return new String[] { "org.idempiere.myappx.plugin.tenant.x.process" };
	}
}
