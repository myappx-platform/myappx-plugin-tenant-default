# MyAppx Plugin : Tenant Default

**语言 / Language:** 中文 | [English](README.en.md)

[![License](https://img.shields.io/badge/license-GPL--2.0-brightgreen.svg)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.html)

**MyAppx Plugin : Tenant Default** 是面向 [iDempiere](https://www.idempiere.org/) 的 OSGi 插件，在空库或新环境中预置名为 **`Default`** 的租户（组织同名），并在 Bundle 启动时由 `ODT2PackActivator` 自动 Pack-in `ODT2Pack-*.zip`：先在 **SYSTEM** 注册实体类型 **`DEFAULT`**，再向 **`Default` 租户** 导入角色模板、本地用户、动态校验与窗口/进程个性化。用于低代码 / 多租户开发的可重复默认租户样板。

| 项目 | 说明 |
|------|------|
| Artifact / Bundle Symbolic Name | `org.idempiere.myappx.plugin.tenant.default`（`singleton:=true`） |
| Bundle Version | `14.0.0.qualifier` |
| Bundle-Activator | `org.idempiere.myappx.plugin.tenant.x.Activator` |
| Java | 17+（`JavaSE-17`） |
| iDempiere | 14 |
| 构建 | Maven / Tycho 4.x（`eclipse-plugin`） |
| Parent | `idempiere.extension.parent` |
| Entity Type | `DEFAULT` |
| 必选 Bundle | `org.idempiere.myappx.plugin.odt` |
| License | [GNU General Public License Version 2](https://www.gnu.org/licenses/old-licenses/gpl-2.0.html) |
| Vendor | Ken Longnan \<ken.longnan@gmail.com\> |

---

## 目录

1. [功能概述](#1-功能概述)
2. [架构](#2-架构)
3. [构建与依赖](#3-构建与依赖)
4. [项目结构](#4-项目结构)
5. [启动与安装](#5-启动与安装)
6. [META-INF 资源](#6-meta-inf-资源)
7. [系统级 2Pack](#7-系统级-2pack)
8. [租户级 2Pack](#8-租户级-2pack)
9. [首次创建租户](#9-首次创建租户)
10. [内置账号](#10-内置账号)
11. [进程工厂](#11-进程工厂)
12. [事件委托](#12-事件委托)
13. [扩展与注册](#13-扩展与注册)
14. [部署](#14-部署)
15. [许可证](#15-许可证)

---

## 1. 功能概述

| 能力 | 说明 |
|------|------|
| **首次启动创建租户** | `Activator.preInstallPackage()` 若尚不存在 `Name='Default'` 的 `AD_Client`，则调用 `CustomInitialClientSetup`（进程 **53161** 参数集）创建租户、组织、**Tenant Admins** 角色与 **Tenant Admin** 用户，并把 Client / Org / 管理员角色 UUID 写成与 2Pack 一致的种子值。 |
| **系统级实体类型** | `ODT2Pack-1.0.0_SYSTEM_Tenant_Default.zip` 在 **Client=0（SYSTEM）** 注册 `EntityType=DEFAULT`（名称 *Tenant Default*）。 |
| **租户级样板** | `ODT2Pack-1.0.1_Default_Tenant.zip` 面向 `AD_Client_UU=7695b314-7a4e-46c0-99f7-280b57ade0ed` 的 `Default` 租户：角色模板、本地用户、表级/字段级校验、窗口与进程个性化、工具栏限制。 |
| **用户偏好占位** | `UpdateUserPreferenceEventDelegate` 挂在 `MUserPreference` 的 `@AfterNew`；方法体为空，预留扩展。 |

本插件 **没有** `ODTPackage*.xml` 与 `2Pack_*.zip`；启动安装仅走 `META-INF/ODT2Pack-*.zip`。

---

## 2. 架构

```text
┌─────────────────────────────────────────────────────────────┐
│  Activator  extends ODT2PackActivator                       │
│  preInstall → 创建 Default 租户                             │
│  packInAll  → ODTPackage*.xml（无）                         │
│             → 2Pack_*.zip（无）                             │
│             → ODT2Pack-*.zip（按版本号排序）                      │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│  OSGi DS                                                     │
│  DefaultAnnotationBasedEventManager                          │
│    扫描 org.idempiere.myappx.plugin.tenant.x.event.delegate  │
│  DefaultInitialSetupProcessFactory  (ranking=5)              │
│    扫描 org.idempiere.myappx.plugin.tenant.x.process         │
└─────────────────────────────────────────────────────────────┘
```

`Bundle-ActivationPolicy: lazy`。`Service-Component: OSGI-INF/*.xml`。

两个包按版本号安装：**`ODT2Pack-1.0.0_SYSTEM_Tenant_Default.zip`** 先于 **`ODT2Pack-1.0.1_Default_Tenant.zip`**。

---

## 3. 构建与依赖

**Require-Bundle：** `org.adempiere.base`、`org.adempiere.base.process`、`org.adempiere.plugin.utils`、`org.adempiere.ui.zk`、`org.idempiere.zk.extra`、`org.osgi.service.event`、ZK（`zcommon` / `zel` / `zk` / `zul` / `zhtml`）、`org.idempiere.myappx.plugin.odt`

**Import-Package：** `org.osgi.framework`、`org.osgi.service.event`

Tycho 需能从目标平台或同一 reactor 解析 `org.idempiere.myappx.plugin.odt`。设置 `IDEMPIERE_CORE_REPOSITORY_URL` 指向 iDempiere P2，并传入 `-Drevision=14.0.0-SNAPSHOT`。

```bash
cd myappx-plugins

# 与 ODT 同 reactor（ODT 尚未进入目标平台时）
mvn "-Drevision=14.0.0-SNAPSHOT" clean verify \
  -pl myappx-plugin-odt/org.idempiere.myappx.plugin.odt,myappx-plugin-tenant-default

# 目标平台已含 ODT 时，可只构建本模块
mvn -f myappx-plugin-tenant-default/pom.xml "-Drevision=14.0.0-SNAPSHOT" clean verify
```

产物：`myappx-plugin-tenant-default/target/org.idempiere.myappx.plugin.tenant.default-*.jar`

---

## 4. 项目结构

```text
myappx-plugin-tenant-default/                         # eclipse-plugin
├── pom.xml
├── readme.md                                         # 中文（本文件）
├── README.en.md                                      # English
├── META-INF/
│   ├── MANIFEST.MF
│   ├── ODT2Pack-1.0.0_SYSTEM_Tenant_Default.zip        # 系统级：EntityType DEFAULT
│   └── ODT2Pack-1.0.1_Default_Tenant.zip               # 租户 Default 字典包
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

Eclipse 工程名：`org.idempiere.myappx.plugin.tenant.default`。Java 包名为 `org.idempiere.myappx.plugin.tenant.x`。

---

## 5. 启动与安装

`Activator` 继承 `org.idempiere.myappx.plugin.odt.ODT2PackActivator`。iDempiere 服务就绪后异步执行：

```text
preInstallPackage()          # 若不存在 Default 租户 → CustomInitialClientSetup + UUID 种子
    ↓
packInAll()
    ├── packInODT()          → META-INF/ODTPackage*.xml（本模块未附带）
    ├── packIn2Pack()        → META-INF/2Pack_*.zip（本模块未附带）
    └── packInFolder()       → META-INF/ODT2Pack-*.zip
    ↓
postInstallPackage()         # 空钩子
```

`ODT2Pack-*.zip` 按文件名中的 Client 段安装；已安装过的同名包会跳过。运行前须已部署并可解析 **MyAppx Plugin ODT**。更详细的 ODT + 2Pack 行为见同仓库 `myappx-plugin-odt/README.md`。

---

## 6. META-INF 资源

| 文件 | 角色 |
|------|------|
| `ODT2Pack-1.0.0_SYSTEM_Tenant_Default.zip` | 系统级 Pack-in：实体类型 `DEFAULT` |
| `ODT2Pack-1.0.1_Default_Tenant.zip` | 租户 `Default` Pack-in：角色、用户、校验与个性化 |

两包均为 PackOutVersion `100`，版本 `1.0.0`。

---

## 7. 系统级 2Pack

包名 **`999901010010_SYSTEM_Tenant_Default`**。描述：*Tenant (Default) 系统级初始化*。目标 Client：`0-SYSTEM-System`。

| 路径 | 作用 |
|------|------|
| `dict/PackOut.xml` | 字典导出主体 |
| `doc/999901010010_SYSTEM_Tenant_DefaultDoc.xml` | 包说明页 |

### 数据内容

- **`AD_EntityType`**
  - **`EntityType`**：`DEFAULT`
  - **`Name`**：`Tenant Default`
  - **`AD_EntityType_UU`**：`3f801a86-d90d-4b02-b628-49b68fc15ed1`
- **`AD_Package_Exp` / `AD_Package_Exp_Detail`**：包自身导出定义，以及一条 **`Type=ET`** 细节（*Setup EntityType 'DEFAULT' for Tenant Default*）。

该系统包只建立实体类型，不含业务表数据。租户包中 `EntityType=DEFAULT` 的对象归入此类型。

---

## 8. 租户级 2Pack

包名 **`999902010010_Default_Tenant`**。描述：*Tenant (Default) 租户级初始化*。目标 Client：`1000000-Default-Default`（`AD_Client_UU` 与 Activator 种子一致）。

| 路径 | 作用 |
|------|------|
| `dict/PackOut.xml` | 租户级字典主体 |
| `doc/999902010010_Default_TenantDoc.xml` | 包说明页 |

### 8.1 导出明细（`AD_Package_Exp_Detail`）

| Line | 类型 | 说明 |
|------|------|------|
| 10 | `D` | `AD_Client`（`Name='Default'`） |
| 20 | `D` | `AD_Org`（`Name='Default'`） |
| 30 | `D` | 表级动态校验 `AD_TableValRule` |
| 50–53 | `S` | 角色：`Template-99`、`Template-00`、`Local Admins`、`Local Users` |
| 54 | `D` | `AD_Role_Included`（模板角色包含关系） |
| 55 | `D` | 用户 `Local Admin` / `Local User` 及 `AD_User_Roles` |
| 100 | `D` | `AD_Val_Rule`（`EntityType='DEFAULT'`） |
| 110 | `D` | 窗口个性化：`AD_UserDef_Win` → `AD_UserDef_Tab` → `AD_UserDef_Field` |
| 120 | `D` | 进程个性化：`AD_UserDef_Proc` / `AD_UserDef_Proc_Parameter` |
| 130 | `D` | 工具栏按钮限制 `AD_ToolBarButtonRestrict` |
| 140 | `D` | 租户级元素翻译 `AD_Element_Trl` |

### 8.2 租户与组织

与 Activator 种子对齐：

- **`AD_Client`**：`Name` / `Value` = **`Default`**，`Description` = *Default Tenant*；`AD_Client_UU` = **`7695b314-7a4e-46c0-99f7-280b57ade0ed`**；`RequestEMail` = `no-reply@default.corp`
- **`AD_Org`**：`Name` / `Value` = **`Default`**，`AD_Org_UU` = **`8e60b33e-522d-46b3-8d0e-c754968abb51`**

### 8.3 内置角色与包含关系

| 角色名 | Description | IsMasterRole | 备注 |
|--------|-------------|--------------|------|
| **Template-99** | *Build-In Role Template : All Access* | `Y` | 本租户功能全集模板，带窗口/进程/表单等 Access |
| **Template-00** | *Build-In Role Template : Base Role* | `Y` | 基础模板 |
| **Local Admins** | *Build-In Role : Local Admins* | `N` | `IsAccessAllOrgs=Y`，`PreferenceType=C`；**包含** Template-99（`SeqNo=10`） |
| **Local Users** | *Build-In Role : Local Users* | `N` | `IsAccessAllOrgs=Y`；**包含** Template-00（`SeqNo=10`） |

**Tenant Admins** 由 Activator / `CustomInitialClientSetup` 创建，不在本包的 `AD_Role` 导出中。多条 `AD_TableValRule` 对 **`AD_Role_UU=6b4859d5-02d3-419e-a9d6-13955dae9821`** 使用 `Code=1=1`，使 Tenant Admins 不受「仅限 Template-99 范围」等默认限制。

### 8.4 表级动态校验（`AD_TableValRule` ×15）

常规角色（无指定 Role）默认限制摘要：

| 表（`AD_Table_ID`） | 默认 Code 行为 |
|---------------------|----------------|
| `AD_Window` (105) | 限于 Template-99 已授权窗口 |
| `AD_Client` (112) | `AD_Client_ID = @#AD_Client_ID@` |
| `AD_User` (114) | 限制非 Client Administrator 用户范围 |
| `AD_Workflow` (117) | 限于 Template-99 已授权工作流 |
| `AD_Task` (118) | 限于 Template-99 已授权任务 |
| `AD_Role` (156) | `IsClientAdministrator='N' and IsManual='Y'` |
| `C_DocType` (217) | 限于 Template-99 单据类型访问 |
| `AD_Process` (284) | 限于 Template-99 已授权进程 |
| `AD_Form` (376) | 限于 Template-99 已授权表单 |
| `AD_InfoWindow` (895) | 限于 Template-99 已授权 InfoWindow |

对 Tenant Admins（上述 UUID）另有 Window / Client / User / Role / Process 的 `1=1` 例外行。

### 8.5 字段/表校验（`AD_Val_Rule`，`EntityType=DEFAULT`）

| Name | 摘要 |
|------|------|
| Default : AD_Role Security validation | 排除名为 `Tenant Admins` 的角色，且 `IsMasterRole='N'` |
| Default : AD_User Security Validation | 排除 Client Administrator 用户 |
| Default : AD_User Security Validation (!@AD_User_ID@) | 排除绑定 Tenant Admins 的用户，并排除当前 `@AD_User_ID@` |
| Default : AD_User Tenant | `AD_User.AD_Client_ID=@#AD_Client_ID@` |
| Default : C_BPartner Access validation | 排除名为 `Tenant Admin` 的业务伙伴 |
| Default : C_DocType Access validation | 限于 Template-99 单据类型访问 |
| Default : AD_Window / Process / Form / Task / Workflow / InfoWindow Access validation (Deprecated) | 限于 Template-99 Access；包内标明已由表级动态校验替代 |

### 8.6 用户与角色分配

| 用户名 | `Value` | 邮箱 | 角色 | 包内密码 |
|--------|---------|------|------|----------|
| **Local User** | `luser` | `user@default.corp` | Local Users | `default` |
| **Local Admin** | `ladmin` | `admin@default.corp` | Local Admins + Local Users | `default` |

**Tenant Admin** 由代码创建，不在本 2Pack 的 `AD_User` 段中。字典规则与校验引用 **Tenant Admins / Tenant Admin** 名称及管理员角色 UUID，须与启动时种子一致。

包内密码与演示邮箱 **仅用于开发/演示**；生产环境必须更换。

### 8.7 其它租户级对象

| 对象 | 数量（当前包） | 说明 |
|------|----------------|------|
| `AD_UserDef_Win` / Tab / Field | 39 / 100 / 47 | 按角色/用户裁剪窗口、Tab、字段 |
| `AD_UserDef_Proc` | 3 | 含 Cache Reset（进程 205）的中英文描述定制 |
| `AD_ToolBarButtonRestrict` | 2 | 对 **Local Users** 排除指定工具栏按钮（`IsExclude=Y`） |
| `AD_Element_Trl` | 2 | *Request Support* / *请求支持*（`en_US` / `zh_CN`） |

---

## 9. 首次创建租户

`preInstallPackage()` 调用 `initClientSetup`：

1. 查询是否已有 `Name='Default'` 的 `AD_Client`。若有，记警告并 **跳过** 创建（避免重复 Initial Client Setup），随后仍由父类 Pack-in `ODT2Pack-*.zip`。
2. 否则以进程 **53161** 启动 `CustomInitialClientSetup`（`@Process`，由 `DefaultInitialSetupProcessFactory` 发现）。该类使用核心 **`MSetup`**：`createClient4x` → `createCalendar` → `createAccounting` → `createAndValidateDocumentTypes` → `createEntities4x`。
3. SQL 把 UUID 写成种子，使后续租户 2Pack 能按 UUID 对齐。

Activator 传入的主要参数：

| 参数 | 值 |
|------|-----|
| ClientName / OrgName / OrgValue | `Default` |
| AdminRoleName | `Tenant Admins` |
| AdminUserName | `Tenant Admin` |
| AdminUserEmail | `9admin@default.corp` |
| EMail（组织支持邮箱） | `support@default.corp` |
| C_Currency_ID | `332` |
| C_Country_ID | `153` |
| UseDefaultCoA / InactivateDefaults / IsSetInitialPassword | `Y` |

科目表文件来自 `MSysConfig.DEFAULT_COA_PATH`（缺省 `{ADEMPIERE_HOME}/data/import/AccountingDefaultsOnly.csv`），文件必须存在且可读。

### UUID 种子

| 对象 | 名称 | UUID |
|------|------|------|
| `AD_Client` | Default | `7695b314-7a4e-46c0-99f7-280b57ade0ed` |
| `AD_Org` | Default | `8e60b33e-522d-46b3-8d0e-c754968abb51` |
| `AD_Role` | Tenant Admins | `6b4859d5-02d3-419e-a9d6-13955dae9821` |

`createClient4x` 只创建管理员角色与管理员用户（不创建普通用户角色）。管理员初始密码由 `MSetup` 设为租户名 **`Default`**。

---

## 10. 内置账号

联调用；**生产环境请修改密码与邮箱**。

### 由 `CustomInitialClientSetup` 创建

| 说明 | 值 |
|------|-----|
| 租户 / 组织 | `Default` / `Default` |
| 管理员显示名 | `Tenant Admin` |
| 管理员邮箱 | `9admin@default.corp` |
| 管理员角色 | `Tenant Admins` |
| 初始密码 | 租户名 `Default`（见 `MSetup.createClient4x`） |

### 由租户 2Pack 导入

| 用户 | 邮箱 | `Value` | 角色 | 包内密码 |
|------|------|---------|------|----------|
| Local Admin | `admin@default.corp` | `ladmin` | Local Admins、Local Users | `default` |
| Local User | `user@default.corp` | `luser` | Local Users | `default` |

---

## 11. 进程工厂

`DefaultInitialSetupProcessFactory` 继承 `AnnotationBasedProcessFactory`，扫描包 **`org.idempiere.myappx.plugin.tenant.x.process`**，注册 `@Process` 类（当前为 `CustomInitialClientSetup`）。OSGi `service.ranking=5`。

Activator 通过 `ProcessInfo.setClassName(CustomInitialClientSetup.class.getName())` 直接调用该类；进程 **53161** 用于 `MPInstance` 与参数定义。

---

## 12. 事件委托

由 `DefaultAnnotationBasedEventManager` 通过 `@EventTopicDelegate` + `@ModelEventTopic` 发现。

| 类 | 模型 | 时机 | 行为 |
|----|------|------|------|
| `UpdateUserPreferenceEventDelegate` | `MUserPreference` | `@AfterNew` | 扩展点（`update2default`），当前为空 |

---

## 13. 扩展与注册

- **模型事件**：在 `org.idempiere.myappx.plugin.tenant.x.event.delegate` 新增带 `@EventTopicDelegate`、`@ModelEventTopic` 的类即可。
- **进程**：在 `org.idempiere.myappx.plugin.tenant.x.process` 新增 `@Process` 类，由 `DefaultInitialSetupProcessFactory` 收集。

---

## 14. 部署

1. 目标实例已安装 **MyAppx Plugin ODT**（`org.idempiere.myappx.plugin.odt`）。
2. 将本插件 JAR 放入 iDempiere `plugins/`，OSGi 状态为 **Resolved**。Activator 在服务器启动后创建 `Default` 租户（若需要）并完成 Pack-in。
3. 在 **Pack In** 中确认 `ODT2Pack-1.0.0_SYSTEM_Tenant_Default.zip` 与 `ODT2Pack-1.0.1_Default_Tenant.zip` 已应用。

---

## 15. 许可证

与 [iDempiere](https://www.idempiere.org/) 相同，采用 [GNU General Public License Version 2](https://www.gnu.org/licenses/old-licenses/gpl-2.0.html)（GPLv2）。源文件头中的版权声明以各文件为准。

贡献者：ken.longnan@gmail.com

*Bundle: `org.idempiere.myappx.plugin.tenant.default`*
