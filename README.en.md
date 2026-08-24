# MyAppx Plugin : Tenant Default

**Language:** English | [中文](readme.md)

[![License](https://img.shields.io/badge/license-GPL--2.0-brightgreen.svg)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.html)

**MyAppx Plugin : Tenant Default** is an OSGi plugin for [iDempiere](https://www.idempiere.org/). On an empty or new database it provisions a tenant named **`Default`** (organization of the same name). On bundle start, `ODT2PackActivator` Packs-in `9999*.zip`: first the **SYSTEM** entity type **`DEFAULT`**, then tenant-level role templates, local users, dynamic validation, and window/process personalization. It is a repeatable default-tenant baseline for low-code / multi-tenant development.

| Item | Value |
|------|--------|
| Artifact / Bundle Symbolic Name | `org.idempiere.myappx.plugin.tenant.default` (`singleton:=true`) |
| Bundle Version | `14.0.0.qualifier` |
| Bundle-Activator | `org.idempiere.myappx.plugin.tenant.x.Activator` |
| Java | 17+ (`JavaSE-17`) |
| iDempiere | 14 |
| Build | Maven / Tycho 4.x (`eclipse-plugin`) |
| Parent | `idempiere.extension.parent` |
| Entity Type | `DEFAULT` |
| Required bundle | `org.idempiere.myappx.plugin.odt` |
| License | [GNU General Public License Version 2](https://www.gnu.org/licenses/old-licenses/gpl-2.0.html) |
| Vendor | Ken Longnan \<ken.longnan@gmail.com\> |

---

## Contents

1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Build and dependencies](#3-build-and-dependencies)
4. [Project layout](#4-project-layout)
5. [Startup and install](#5-startup-and-install)
6. [META-INF resources](#6-meta-inf-resources)
7. [System 2Pack](#7-system-2pack)
8. [Tenant 2Pack](#8-tenant-2pack)
9. [Initial client setup](#9-initial-client-setup)
10. [Built-in accounts](#10-built-in-accounts)
11. [Process factory](#11-process-factory)
12. [Event delegates](#12-event-delegates)
13. [Extension and registration](#13-extension-and-registration)
14. [Deployment](#14-deployment)
15. [License](#15-license)

---

## 1. Overview

| Capability | Behavior |
|------------|----------|
| **Create tenant on first start** | `Activator.preInstallPackage()` runs `CustomInitialClientSetup` (process **53161** parameter set) if no `AD_Client` named **`Default`** exists: tenant, organization, **Tenant Admins** role, **Tenant Admin** user, then seed UUIDs for Client / Org / admin role to match the 2Pack. |
| **System entity type** | `999901010010_SYSTEM_Tenant_Default.zip` registers `EntityType=DEFAULT` (name *Tenant Default*) on **Client=0 (SYSTEM)**. |
| **Tenant baseline** | `999902010010_Default_Tenant.zip` targets tenant `Default` (`AD_Client_UU=7695b314-7a4e-46c0-99f7-280b57ade0ed`): role templates, local users, table/field validation, window and process personalization, toolbar restrictions. |
| **User preference hook** | `UpdateUserPreferenceEventDelegate` on `MUserPreference` `@AfterNew`; method body is an extension point. |

This plugin ships **no** `ODTPackage*.xml` and **no** `2Pack_*.zip`. Startup install uses `META-INF/9999*.zip` only.

---

## 2. Architecture

```text
┌─────────────────────────────────────────────────────────────┐
│  Activator  extends ODT2PackActivator                       │
│  preInstall → create Default tenant                         │
│  packInAll  → ODTPackage*.xml (none)                        │
│             → 2Pack_*.zip (none)                            │
│             → 9999*.zip (sorted by file name)               │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│  OSGi DS                                                     │
│  DefaultAnnotationBasedEventManager                          │
│    scans org.idempiere.myappx.plugin.tenant.x.event.delegate │
│  DefaultInitialSetupProcessFactory  (ranking=5)              │
│    scans org.idempiere.myappx.plugin.tenant.x.process        │
└─────────────────────────────────────────────────────────────┘
```

`Bundle-ActivationPolicy: lazy`. `Service-Component: OSGI-INF/*.xml`.

`9999*.zip` naming: `yyyymmddHHMM_ClientValue_InformationalDescription.zip`, with year fixed as `9999`. The two packs in this module install in file-name order: **SYSTEM** before **Default**.

---

## 3. Build and dependencies

**Require-Bundle:** `org.adempiere.base`, `org.adempiere.base.process`, `org.adempiere.plugin.utils`, `org.adempiere.ui.zk`, `org.idempiere.zk.extra`, `org.osgi.service.event`, ZK (`zcommon` / `zel` / `zk` / `zul` / `zhtml`), `org.idempiere.myappx.plugin.odt`

**Import-Package:** `org.osgi.framework`, `org.osgi.service.event`

Tycho must resolve `org.idempiere.myappx.plugin.odt` from the target platform or the same reactor. Set `IDEMPIERE_CORE_REPOSITORY_URL` to the iDempiere P2 and pass `-Drevision=14.0.0-SNAPSHOT`.

```bash
cd myappx-plugins

# Same reactor as ODT (when ODT is not yet on the target platform)
mvn "-Drevision=14.0.0-SNAPSHOT" clean verify \
  -pl myappx-plugin-odt/org.idempiere.myappx.plugin.odt,myappx-plugin-tenant-default

# This module only, if ODT is already on the target platform
mvn -f myappx-plugin-tenant-default/pom.xml "-Drevision=14.0.0-SNAPSHOT" clean verify
```

Artifact: `myappx-plugin-tenant-default/target/org.idempiere.myappx.plugin.tenant.default-*.jar`

---

## 4. Project layout

```text
myappx-plugin-tenant-default/                         # eclipse-plugin
├── pom.xml
├── readme.md                                         # Chinese
├── README.en.md                                      # English (this file)
├── META-INF/
│   ├── MANIFEST.MF
│   ├── 999901010010_SYSTEM_Tenant_Default.zip        # system: EntityType DEFAULT
│   └── 999902010010_Default_Tenant.zip               # tenant Default dictionary
├── OSGI-INF/
│   ├── DefaultAnnotationBasedEventManager.xml
│   └── DefaultInitialSetupProcessFactory.xml
├── src/org/idempiere/myappx/plugin/tenant/x/
│   ├── Activator.java
│   ├── DefaultAnnotationBasedEventManager.java
│   ├── factory/DefaultInitialSetupProcessFactory.java
│   ├── process/CustomInitialClientSetup.java
│   └── event/delegate/UpdateUserPreferenceEventDelegate.java
└── build.properties                                  # bin.includes: META-INF/, ., OSGI-INF/
```

Eclipse project name: `org.idempiere.myappx.plugin.tenant.default`. Java package: `org.idempiere.myappx.plugin.tenant.x`.

---

## 5. Startup and install

`Activator` extends `org.idempiere.myappx.plugin.odt.ODT2PackActivator`. After iDempiere is ready it runs asynchronously:

```text
preInstallPackage()          # if Default tenant missing → CustomInitialClientSetup + UUID seeds
    ↓
packInAll()
    ├── packInODT()          → META-INF/ODTPackage*.xml (none in this module)
    ├── packIn2Pack()        → META-INF/2Pack_*.zip (none in this module)
    └── packInFolder()       → META-INF/9999*.zip
    ↓
postInstallPackage()         # empty hook
```

`9999*.zip` installs by the Client segment in the file name; a pack already installed by file name is skipped. **MyAppx Plugin ODT** must already be deployed and resolvable. See `myappx-plugin-odt/README.md` for ODT + 2Pack details.

---

## 6. META-INF resources

| File | Role |
|------|------|
| `999901010010_SYSTEM_Tenant_Default.zip` | System Pack-in: entity type `DEFAULT` |
| `999902010010_Default_Tenant.zip` | Tenant `Default` Pack-in: roles, users, validation, personalization |

Both packs are PackOutVersion `100`, version `1.0.0`.

---

## 7. System 2Pack

Name **`999901010010_SYSTEM_Tenant_Default`**. Description: *Tenant (Default) 系统级初始化* (system-level init). Client: `0-SYSTEM-System`.

| Path | Role |
|------|------|
| `dict/PackOut.xml` | Dictionary export |
| `doc/999901010010_SYSTEM_Tenant_DefaultDoc.xml` | Package description page |

### Contents

- **`AD_EntityType`**
  - **`EntityType`**: `DEFAULT`
  - **`Name`**: `Tenant Default`
  - **`AD_EntityType_UU`**: `3f801a86-d90d-4b02-b628-49b68fc15ed1`
- **`AD_Package_Exp` / `AD_Package_Exp_Detail`**: self-export of the package, plus one **`Type=ET`** line (*Setup EntityType 'DEFAULT' for Tenant Default*).

This pack only creates the entity type. Tenant-pack objects with `EntityType=DEFAULT` belong to it.

---

## 8. Tenant 2Pack

Name **`999902010010_Default_Tenant`**. Description: *Tenant (Default) 租户级初始化* (tenant-level init). Client: `1000000-Default-Default` (`AD_Client_UU` matches the Activator seed).

| Path | Role |
|------|------|
| `dict/PackOut.xml` | Tenant dictionary |
| `doc/999902010010_Default_TenantDoc.xml` | Package description page |

### 8.1 Export details (`AD_Package_Exp_Detail`)

| Line | Type | Notes |
|------|------|--------|
| 10 | `D` | `AD_Client` (`Name='Default'`) |
| 20 | `D` | `AD_Org` (`Name='Default'`) |
| 30 | `D` | Table val rules `AD_TableValRule` |
| 50–53 | `S` | Roles: `Template-99`, `Template-00`, `Local Admins`, `Local Users` |
| 54 | `D` | `AD_Role_Included` |
| 55 | `D` | Users `Local Admin` / `Local User` and `AD_User_Roles` |
| 100 | `D` | `AD_Val_Rule` (`EntityType='DEFAULT'`) |
| 110 | `D` | Window personalization: `AD_UserDef_Win` → `AD_UserDef_Tab` → `AD_UserDef_Field` |
| 120 | `D` | Process personalization: `AD_UserDef_Proc` / `AD_UserDef_Proc_Parameter` |
| 130 | `D` | Toolbar restrictions `AD_ToolBarButtonRestrict` |
| 140 | `D` | Tenant-level element translation `AD_Element_Trl` |

### 8.2 Tenant and organization

Aligned with Activator seeds:

- **`AD_Client`**: `Name` / `Value` = **`Default`**, `Description` = *Default Tenant*; `AD_Client_UU` = **`7695b314-7a4e-46c0-99f7-280b57ade0ed`**; `RequestEMail` = `no-reply@default.corp`
- **`AD_Org`**: `Name` / `Value` = **`Default`**, `AD_Org_UU` = **`8e60b33e-522d-46b3-8d0e-c754968abb51`**

### 8.3 Built-in roles and inclusion

| Role | Description | IsMasterRole | Notes |
|------|-------------|--------------|--------|
| **Template-99** | *Build-In Role Template : All Access* | `Y` | Master template for the tenant feature set (window/process/form access) |
| **Template-00** | *Build-In Role Template : Base Role* | `Y` | Base template |
| **Local Admins** | *Build-In Role : Local Admins* | `N` | `IsAccessAllOrgs=Y`, `PreferenceType=C`; **includes** Template-99 (`SeqNo=10`) |
| **Local Users** | *Build-In Role : Local Users* | `N` | `IsAccessAllOrgs=Y`; **includes** Template-00 (`SeqNo=10`) |

**Tenant Admins** is created by Activator / `CustomInitialClientSetup`, not in this pack’s `AD_Role` export. Several `AD_TableValRule` rows for **`AD_Role_UU=6b4859d5-02d3-419e-a9d6-13955dae9821`** use `Code=1=1` so Tenant Admins are not limited to the Template-99 range.

### 8.4 Table val rules (`AD_TableValRule` ×15)

Default rows (no specific role) in summary:

| Table (`AD_Table_ID`) | Default Code |
|-----------------------|--------------|
| `AD_Window` (105) | Windows granted to Template-99 |
| `AD_Client` (112) | `AD_Client_ID = @#AD_Client_ID@` |
| `AD_User` (114) | Users that are not Client Administrator |
| `AD_Workflow` (117) | Workflows granted to Template-99 |
| `AD_Task` (118) | Tasks granted to Template-99 |
| `AD_Role` (156) | `IsClientAdministrator='N' and IsManual='Y'` |
| `C_DocType` (217) | Doc types granted via Template-99 document-action access |
| `AD_Process` (284) | Processes granted to Template-99 |
| `AD_Form` (376) | Forms granted to Template-99 |
| `AD_InfoWindow` (895) | InfoWindows granted to Template-99 |

Tenant Admins (UUID above) also have `1=1` exception rows for Window / Client / User / Role / Process.

### 8.5 Validation rules (`AD_Val_Rule`, `EntityType=DEFAULT`)

| Name | Summary |
|------|---------|
| Default : AD_Role Security validation | Exclude role named `Tenant Admins`; `IsMasterRole='N'` |
| Default : AD_User Security Validation | Exclude Client Administrator users |
| Default : AD_User Security Validation (!@AD_User_ID@) | Exclude users assigned Tenant Admins, and exclude current `@AD_User_ID@` |
| Default : AD_User Tenant | `AD_User.AD_Client_ID=@#AD_Client_ID@` |
| Default : C_BPartner Access validation | Exclude business partner named `Tenant Admin` |
| Default : C_DocType Access validation | Doc types granted to Template-99 |
| Default : AD_Window / Process / Form / Task / Workflow / InfoWindow Access validation (Deprecated) | Template-99 Access; pack notes they are superseded by table val rules |

### 8.6 Users and role assignment

| User | `Value` | EMail | Roles | Packed password |
|------|---------|-------|-------|-----------------|
| **Local User** | `luser` | `user@default.corp` | Local Users | `default` |
| **Local Admin** | `ladmin` | `admin@default.corp` | Local Admins + Local Users | `default` |

**Tenant Admin** is created in code, not in this 2Pack’s `AD_User` section. Dictionary rules refer to **Tenant Admins / Tenant Admin** names and the admin role UUID; those must match the startup seeds.

Packed passwords and sample emails are **for development/demo only**. Change them in production.

### 8.7 Other tenant objects

| Object | Count (this pack) | Notes |
|--------|-------------------|--------|
| `AD_UserDef_Win` / Tab / Field | 39 / 100 / 47 | Window, tab, and field personalization |
| `AD_UserDef_Proc` | 3 | Includes Cache Reset (process 205) EN/ZH descriptions |
| `AD_ToolBarButtonRestrict` | 2 | Exclude selected toolbar buttons for **Local Users** (`IsExclude=Y`) |
| `AD_Element_Trl` | 2 | *Request Support* / *请求支持* (`en_US` / `zh_CN`) |

---

## 9. Initial client setup

`preInstallPackage()` calls `initClientSetup`:

1. Query for `AD_Client` with `Name='Default'`. If it exists, log a warning and **skip** creation (no second Initial Client Setup). The parent still Packs-in `9999*.zip`.
2. Otherwise start `CustomInitialClientSetup` as process **53161** (`@Process`, discovered by `DefaultInitialSetupProcessFactory`). The class uses core **`MSetup`**: `createClient4x` → `createCalendar` → `createAccounting` → `createAndValidateDocumentTypes` → `createEntities4x`.
3. SQL writes the seed UUIDs so the tenant 2Pack can match by UUID.

Main Activator parameters:

| Parameter | Value |
|-----------|--------|
| ClientName / OrgName / OrgValue | `Default` |
| AdminRoleName | `Tenant Admins` |
| AdminUserName | `Tenant Admin` |
| AdminUserEmail | `9admin@default.corp` |
| EMail (org support) | `support@default.corp` |
| C_Currency_ID | `332` |
| C_Country_ID | `153` |
| UseDefaultCoA / InactivateDefaults / IsSetInitialPassword | `Y` |

Chart of accounts file comes from `MSysConfig.DEFAULT_COA_PATH` (default `{ADEMPIERE_HOME}/data/import/AccountingDefaultsOnly.csv`) and must exist and be readable.

### UUID seeds

| Object | Name | UUID |
|--------|------|------|
| `AD_Client` | Default | `7695b314-7a4e-46c0-99f7-280b57ade0ed` |
| `AD_Org` | Default | `8e60b33e-522d-46b3-8d0e-c754968abb51` |
| `AD_Role` | Tenant Admins | `6b4859d5-02d3-419e-a9d6-13955dae9821` |

`createClient4x` creates only the admin role and admin user (no regular user role). The admin initial password is the tenant name **`Default`** (`MSetup.createClient4x`).

---

## 10. Built-in accounts

For local testing. **Change passwords and emails in production.**

### Created by `CustomInitialClientSetup`

| Item | Value |
|------|--------|
| Tenant / organization | `Default` / `Default` |
| Admin display name | `Tenant Admin` |
| Admin email | `9admin@default.corp` |
| Admin role | `Tenant Admins` |
| Initial password | Tenant name `Default` (see `MSetup.createClient4x`) |

### Imported by the tenant 2Pack

| User | EMail | `Value` | Roles | Packed password |
|------|-------|---------|-------|-----------------|
| Local Admin | `admin@default.corp` | `ladmin` | Local Admins, Local Users | `default` |
| Local User | `user@default.corp` | `luser` | Local Users | `default` |

---

## 11. Process factory

`DefaultInitialSetupProcessFactory` extends `AnnotationBasedProcessFactory` and scans **`org.idempiere.myappx.plugin.tenant.x.process`** for `@Process` classes (currently `CustomInitialClientSetup`). OSGi `service.ranking=5`.

The Activator invokes the class with `ProcessInfo.setClassName(CustomInitialClientSetup.class.getName())`. Process **53161** supplies `MPInstance` and parameter definitions.

---

## 12. Event delegates

Discovered by `DefaultAnnotationBasedEventManager` via `@EventTopicDelegate` + `@ModelEventTopic`.

| Class | Model | When | Behavior |
|-------|--------|------|----------|
| `UpdateUserPreferenceEventDelegate` | `MUserPreference` | `@AfterNew` | Extension point (`update2default`); currently empty |

---

## 13. Extension and registration

- **Model events:** add a class under `org.idempiere.myappx.plugin.tenant.x.event.delegate` with `@EventTopicDelegate` and `@ModelEventTopic`.
- **Processes:** add a `@Process` class under `org.idempiere.myappx.plugin.tenant.x.process`; `DefaultInitialSetupProcessFactory` collects it.

---

## 14. Deployment

1. The target instance already has **MyAppx Plugin ODT** (`org.idempiere.myappx.plugin.odt`).
2. Place this plugin JAR in iDempiere `plugins/`. OSGi state should be **Resolved**. After server start the activator creates tenant `Default` if needed and completes Pack-in.
3. Confirm `999901010010_SYSTEM_Tenant_Default` and `999902010010_Default_Tenant` in **Pack In**.

---

## 15. License

Same license as [iDempiere](https://www.idempiere.org/): [GNU General Public License Version 2](https://www.gnu.org/licenses/old-licenses/gpl-2.0.html) (GPLv2). File headers in source take precedence for those files.

Contributor: ken.longnan@gmail.com

*Bundle: `org.idempiere.myappx.plugin.tenant.default`*
