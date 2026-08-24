package org.idempiere.myappx.plugin.tenant.x;

import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.util.ProcessUtil;
import org.compiere.model.MClient;
import org.compiere.model.MOrg;
import org.compiere.model.MPInstance;
import org.compiere.model.MRole;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.X_AD_Package_UUID_Map;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;

import org.idempiere.myappx.plugin.odt.ODT2PackActivator;
import org.idempiere.myappx.plugin.tenant.x.process.CustomInitialClientSetup;

public class Activator extends ODT2PackActivator {

	// Seed of Default AD_Client 
	private static String AD_Client_UU = "7695b314-7a4e-46c0-99f7-280b57ade0ed";  
	// Seed of Default AD_Org 
	private static String AD_Org_UU = "8e60b33e-522d-46b3-8d0e-c754968abb51"; 
	// Seed of Default Role (Tenant Admins)
	private static String AD_Role_UU_Admin = "6b4859d5-02d3-419e-a9d6-13955dae9821";
	
	private static String clientName = "Default";
	private static String orgName = "Default"; // orgName = orgValue
	private static String OrgValue = orgName;
	
	private static String AdminRoleName = "Tenant Admins";
	private static String AdminUserName = "Tenant Admin";
	private static String AdminUserEmail = "9admin@default.corp";

	private static String tenantEmail = "support@default.corp";

	protected void preInstallPackage() {
		initClientSetup(Env.getCtx());
	}

	protected void postInstallPackage() {
	}

	private void initClientSetup(Properties ctx) {
		PO po;
		Query queryClient = new Query(ctx, MClient.Table_Name, MClient.COLUMNNAME_Name + "=?", null);
		try {
			//PO.setCrossTenantSafe();
			po = queryClient.setParameters(clientName).firstOnly();
   		} finally {
   			//PO.clearCrossTenantSafe();
   		}
		if (po != null) { // Client existed
			logger.log(Level.WARNING, clientName + " Client existed, ODT2Pack from bundle:" + getName());
			return;
		}
		
		// Create Client
		ProcessInfo pi = new ProcessInfo("Initial Client Setup : Tenant " + clientName, 53161);
		pi.setAD_Client_ID(0);
		pi.setAD_User_ID(100);
		pi.setClassName(CustomInitialClientSetup.class.getName());

		ProcessInfoParameter p_ClientName = new ProcessInfoParameter("ClientName", clientName, null, null, null);
		ProcessInfoParameter p_OrgName = new ProcessInfoParameter("OrgName", orgName, null, null, null);
		ProcessInfoParameter p_OrgValue = new ProcessInfoParameter("OrgValue", OrgValue, null, null, null);
		ProcessInfoParameter p_AdminRoleName = new ProcessInfoParameter("AdminRoleName", AdminRoleName, null, null, null);
		ProcessInfoParameter p_AdminUserName = new ProcessInfoParameter("AdminUserName", AdminUserName, null, null, null);
		ProcessInfoParameter p_AdminUserEmail = new ProcessInfoParameter("AdminUserEmail", AdminUserEmail, null, null, null);
		ProcessInfoParameter p_IsSetInitialPassword = new ProcessInfoParameter("IsSetInitialPassword", true, null, null, null);
		ProcessInfoParameter p_C_Currency_ID = new ProcessInfoParameter("C_Currency_ID", 332, null, null, null);
		ProcessInfoParameter p_C_Country_ID = new ProcessInfoParameter("C_Country_ID", 153, null, null, null);
		ProcessInfoParameter p_UseDefaultCoA = new ProcessInfoParameter("UseDefaultCoA", true, null, null, null);
		ProcessInfoParameter p_InactivateDefaults = new ProcessInfoParameter("InactivateDefaults", true, null, null, null);
		ProcessInfoParameter p_EMail = new ProcessInfoParameter("EMail", tenantEmail, null, null, null);
	
		pi.setParameter(new ProcessInfoParameter[] { //p_TenantDefault
				p_ClientName
				,p_OrgName 
				,p_OrgValue
				,p_AdminRoleName
				,p_AdminUserName
				,p_AdminUserEmail
				,p_IsSetInitialPassword
				,p_C_Currency_ID
				,p_C_Country_ID
				,p_UseDefaultCoA
				,p_InactivateDefaults
				,p_EMail
				});

		MPInstance instance = new MPInstance(ctx, pi.getAD_Process_ID(), -1, pi.getRecord_ID(), null);
		if (!instance.save()) {
			pi.setSummary(Msg.getMsg(ctx, "ProcessNoInstance"));
			pi.setError(true);
		}
		pi.setAD_PInstance_ID(instance.getAD_PInstance_ID());
		ProcessUtil.startJavaProcess(ctx, pi, null);
		
		// Update AD_Client_UU to seed-value
		String sql_updateAD_Client_UU = "update AD_Client set AD_Client_UU = '" + AD_Client_UU 
				+ "' where Name = '" + clientName + "'";
		DB.executeUpdate(sql_updateAD_Client_UU, null);
		
		// Update AD_Org_UU to seed-value
		String sql_updateAD_Org_UU = "update AD_Org set AD_Org_UU = '" + AD_Org_UU 
				+ "' where name = '" + orgName + "'"
				+ " and AD_Client_ID = (select AD_Client_ID from AD_Client where AD_Client_UU = '" + AD_Client_UU + "')";
		DB.executeUpdate(sql_updateAD_Org_UU, null);

		// update AD_Role_UU to seed-value
		String sql_updateAD_Role_UU_Admin = "update AD_Role set AD_Role_UU = '" + AD_Role_UU_Admin + "'" 
				+ " where name = '" + AdminRoleName + "'"
				+ " and AD_Client_ID = (select AD_Client_ID from AD_Client where AD_Client_UU = '" + AD_Client_UU + "')";
		DB.executeUpdate(sql_updateAD_Role_UU_Admin, null);
		
		/////////////////////////////////////////////////////////////////////////////////////
		// Setup UUID Map for Default Client/Org
		
//		int AD_Client_ID;
//		int AD_Org_ID;
//		
//		// Create UUID Map for Client Default
//		{
//			try {
//				PO.setCrossTenantSafe();
//				po = queryClient.setParameters(clientName).firstOnly();
//	   		} finally {
//	   			PO.clearCrossTenantSafe();
//	   		}
//			if (po == null) {
//				return;
//			}
//			
//			AD_Client_ID = po.get_ID();
//			
//			String TargetOfAD_Client_UU = po.get_ValueAsString(po.getUUIDColumnName());
//			
//			X_AD_Package_UUID_Map mapClient = new X_AD_Package_UUID_Map(ctx, 0, null);
//			mapClient.setAD_Table_ID(MClient.Table_ID);
//			mapClient.set_ValueNoCheck("AD_Client_ID", AD_Client_ID);
//			mapClient.setSource_UUID(SourceOfAD_Client_UU);
//			mapClient.setTarget_UUID(TargetOfAD_Client_UU);
//			mapClient.saveEx();
//		}
//		
//		// Create UUID Map for Org Default
//		{
//			Query queryOrg = new Query(ctx, MOrg.Table_Name, MOrg.COLUMNNAME_Name + "=?" 
//					+ " and " + MOrg.COLUMNNAME_AD_Client_ID + "=?", null);
//			try {
//				PO.setCrossTenantSafe();
//				po = queryOrg.setParameters(orgName, AD_Client_ID).firstOnly();
//	   		} finally {
//	   			PO.clearCrossTenantSafe();
//	   		}
//			if (po == null) {
//				return;
//			}
//
//			AD_Org_ID = po.get_ID();
//			
//			String TargetOfAD_Org_UU = po.get_ValueAsString(po.getUUIDColumnName());
//			
//			X_AD_Package_UUID_Map mapOrg = new X_AD_Package_UUID_Map(ctx, 0, null);
//			mapOrg.setAD_Table_ID(MOrg.Table_ID);
//			mapOrg.set_ValueNoCheck("AD_Client_ID", AD_Client_ID);
//			mapOrg.setSource_UUID(sourceOfAD_Org_UU);
//			mapOrg.setTarget_UUID(TargetOfAD_Org_UU);
//			mapOrg.saveEx();
//		}
		

		return;
	}

}