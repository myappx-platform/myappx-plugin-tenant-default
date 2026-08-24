package org.idempiere.myappx.plugin.tenant.x.process;

import java.io.File;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.Adempiere;
import org.compiere.model.MCurrency;
import org.compiere.model.MProcessPara;
import org.compiere.model.MSetup;
import org.compiere.model.MSysConfig;
import org.compiere.print.PrintUtil;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.EMail;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 * Tenant-default variant of initial client setup 
 */
@Process
public class CustomInitialClientSetup extends SvrProcess {

	private String p_ClientName = null;
	private String p_OrgValue = null;
	private String p_OrgName = null;
	private String p_AdminRoleName = null;
	private String p_AdminUserName = null;
	private boolean p_IsSetInitialPassword = true;
	private int p_C_Currency_ID = 0;
	private int p_C_Country_ID = 0;
	private String p_CityName = null;
	private String p_EMail = null;
	private String p_AdminUserEmail = null;

	public static final int WINDOW_THIS_PROCESS = 9999;

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("ClientName"))
				p_ClientName = (String) para[i].getParameter();
			else if (name.equals("OrgValue"))
				p_OrgValue = (String) para[i].getParameter();
			else if (name.equals("OrgName"))
				p_OrgName = (String) para[i].getParameter();
			else if (name.equals("AdminUserName"))
				p_AdminUserName = (String) para[i].getParameter();
			else if (name.equals("AdminRoleName"))
				p_AdminRoleName = (String) para[i].getParameter();
			else if (name.equals("IsSetInitialPassword"))
				p_IsSetInitialPassword = para[i].getParameterAsBoolean();
			else if (name.equals("C_Currency_ID"))
				p_C_Currency_ID = para[i].getParameterAsInt();
			else if (name.equals("C_Country_ID"))
				p_C_Country_ID = para[i].getParameterAsInt();
			else if (name.equals("CityName"))
				p_CityName = (String) para[i].getParameter();
			else if (name.equals("EMail"))
				p_EMail = (String) para[i].getParameter();
			else if (name.equals("AdminUserEmail"))
				p_AdminUserEmail = (String) para[i].getParameter();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}
	}

	@Override
	protected String doIt() throws Exception {
		StringBuilder msglog = new StringBuilder("InitialClientSetup")
				.append(": ClientName=").append(p_ClientName)
				.append(", OrgValue=").append(p_OrgValue)
				.append(", OrgName=").append(p_OrgName)
				.append(", AdminUserName=").append(p_AdminUserName)
				.append(", AdminRoleName=").append(p_AdminRoleName)
				.append(", IsSetInitialPassword=").append(p_IsSetInitialPassword)
				.append(", C_Currency_ID=").append(p_C_Currency_ID)
				.append(", C_Country_ID=").append(p_C_Country_ID)
				.append(", CityName=").append(p_CityName);
		
		if (log.isLoggable(Level.INFO))
			log.info(msglog.toString());

		if (p_ClientName == null || p_ClientName.length() == 0 || p_OrgName == null || p_OrgName.length() == 0
				|| p_OrgValue == null || p_OrgValue.length() == 0
				|| p_AdminUserName == null || p_AdminUserName.length() == 0 
				|| p_AdminRoleName == null || p_AdminRoleName.length() == 0 
				|| p_C_Currency_ID <= 0 || p_C_Country_ID <= 0)
			throw new IllegalArgumentException(Msg.getMsg(Env.getCtx(), "Missing required parameters"));

		if (DB.getSQLValueEx(null, "SELECT COUNT(*) FROM AD_Client WHERE Name=?", p_ClientName) != 0)
			throw new AdempiereException(Msg.parseTranslation(getCtx(), "@NotUnique@ @ClientName@ = " + p_ClientName));

		if (DB.getSQLValueEx(null, "SELECT COUNT(*) FROM AD_Client WHERE Value=?", p_ClientName) != 0)
			throw new AdempiereException(Msg.parseTranslation(getCtx(), "@NotUnique@ @ClientValue@ = " + p_ClientName));

		if (DB.getSQLValueEx(null, "SELECT COUNT(*) FROM AD_Client WHERE LoginPrefix=?", p_ClientName) != 0)
			throw new AdempiereException(Msg.parseTranslation(getCtx(), "@NotUnique@ @LoginPrefix@ = " + p_ClientName));

		if (DB.getSQLValueEx(null, "SELECT COUNT(*) FROM AD_User WHERE Name=?", p_AdminUserName) != 0)
			throw new AdempiereException(Msg.parseTranslation(getCtx(), "@NotUnique@ @AdminUserName@ = " + p_AdminUserName));

		boolean email_login = MSysConfig.getBooleanValue(MSysConfig.USE_EMAIL_FOR_LOGIN, false);
		if (email_login) {
			if (Util.isEmpty(p_AdminUserEmail))
				throw new AdempiereException(Msg.getMsg(Env.getCtx(), "AdminUserEmail is required"));
			if (!EMail.validate(p_AdminUserEmail))
				throw new AdempiereException(
						Msg.getMsg(Env.getCtx(), "AdminUserEmail") + " " + p_AdminUserEmail + " " + Msg.getMsg(Env.getCtx(), "is incorrect"));
		}
		
		String defaultCoAPath = MSysConfig.getValue(MSysConfig.DEFAULT_COA_PATH,
					Adempiere.getAdempiereHome() + File.separator + "data"
							+ File.separator + "import"
							+ File.separator + "AccountingDefaultsOnly.csv");
		File coaFile = new File(defaultCoAPath);
		if (!coaFile.exists())
			throw new AdempiereException(Msg.getMsg(Env.getCtx(), "CoaFile") + " " + defaultCoAPath + " " + Msg.getMsg(Env.getCtx(), "does not exist") );
		if (!coaFile.canRead())
			throw new AdempiereException(Msg.getMsg(Env.getCtx(), "Cannot read CoaFile") + " " + defaultCoAPath);
		if (!coaFile.isFile())
			throw new AdempiereException(Msg.getMsg(Env.getCtx(), "CoaFile") + " " + defaultCoAPath + " " + Msg.getMsg(Env.getCtx(), "is not a file"));
		if (coaFile.length() <= 0L)
			throw new AdempiereException(Msg.getMsg(Env.getCtx(), "CoaFile") +  " " + defaultCoAPath + " " + Msg.getMsg(Env.getCtx(), "is empty"));
		
		// Process
		MSetup ms = new MSetup(Env.getCtx(), WINDOW_THIS_PROCESS);
		try {
			if (!ms.createClient4x(p_ClientName, p_OrgValue, p_OrgName, p_AdminRoleName, p_AdminUserName, p_EMail, p_AdminUserEmail)) {
				ms.rollback();
				throw new AdempiereException(Msg.getMsg(Env.getCtx(), "Create client failed"));
			}

			addLog(ms.getInfo());
			
			//Create calendar (even if accounting is disabled, calendar is needed for period control)
			int C_Calendar_ID = ms.createCalendar();
			if (C_Calendar_ID <= 0) {
				ms.rollback();
				throw new AdempiereException(Msg.getMsg(Env.getCtx(), "Create calendar failed"));
			}
			
			//  Generate Accounting
			MCurrency currency = MCurrency.get(getCtx(), p_C_Currency_ID);
			KeyNamePair currency_kp = new KeyNamePair(p_C_Currency_ID, currency.getDescription());
			if (!ms.createAccounting(currency_kp,
					false, false, false, false, false, false,
					coaFile, true, true)) {
				ms.rollback();
				throw new AdempiereException(Msg.getMsg(Env.getCtx(), "AccountSetupError"));
			}
			
			// Create document types (master data)
			if (!ms.createAndValidateDocumentTypes()) {
				ms.rollback();
				throw new AdempiereException(Msg.getMsg(Env.getCtx(), "AccountSetupError"));
			}
			
			//  Generate Entities
			if (!ms.createEntities4x(p_C_Country_ID, p_CityName, 0, null, null )) {
				ms.rollback();
				throw new AdempiereException(Msg.getMsg(Env.getCtx(), "Create entities failed"));
			}
			addLog(ms.getInfo());
			
		} catch (Exception e) {
			ms.rollback();
			throw e;
		}

		return "@OK@";
	}

}
