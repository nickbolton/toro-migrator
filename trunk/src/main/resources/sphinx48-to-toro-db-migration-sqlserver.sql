
select * into temp_up_layout_restrictions from up_layout_restrictions;
drop table up_layout_restrictions ;

CREATE TABLE UP_LAYOUT_RESTRICTIONS(  LAYOUT_ID INTEGER NOT NULL ,
  USER_ID INTEGER NOT NULL ,
  NODE_ID INTEGER NOT NULL ,
  RESTRICTION_NAME VARCHAR(128) NOT NULL ,
  RESTRICTION_VALUE VARCHAR(128),
  RESTRICTION_TREE_PATH VARCHAR(128) NOT NULL );
ALTER TABLE UP_LAYOUT_RESTRICTIONS ADD CONSTRAINT UP_LAYOUT_RESTRICTIONS_PK PRIMARY KEY (LAYOUT_ID, USER_ID, NODE_ID, RESTRICTION_NAME, RESTRICTION_TREE_PATH);

insert into up_layout_restrictions select
layout_id,
user_id,
node_id,
'priority',
restriction_value,
restriction_tree_path
from temp_up_layout_restrictions;

drop table temp_up_layout_restrictions;


select * into temp_up_fragment_restrictions from up_fragment_restrictions;
drop table up_fragment_restrictions ;

CREATE TABLE UP_FRAGMENT_RESTRICTIONS(  FRAGMENT_ID INTEGER NOT NULL ,
  NODE_ID INTEGER NOT NULL ,
  RESTRICTION_NAME VARCHAR(128) NOT NULL ,
  RESTRICTION_VALUE VARCHAR(128),
  RESTRICTION_TREE_PATH VARCHAR(128) NOT NULL );

insert into up_fragment_restrictions select
fragment_id,
node_id,
'priority',
restriction_value,
restriction_tree_path
from temp_up_fragment_restrictions;

drop table temp_up_fragment_restrictions;

drop table up_restrictions ;
drop table upc_keyword ;

INSERT INTO UP_VERSIONS (FNAME, MAJOR, MINOR, MICRO, DESCRIPTION) VALUES ('UP_FRAMEWORK',2,5,2,NULL);

UPDATE UP_CHANNEL SET CHAN_CLASS='org.jasig.portal.layout.alm.channels.CContentSubscriber' WHERE CHAN_CLASS='org.jasig.portal.layout.channels.CContentSubscriber';

INSERT INTO UP_CHANNEL ( CHAN_ID, CHAN_TITLE, CHAN_NAME, CHAN_DESC, CHAN_PUBL_ID, CHAN_PUBL_DT, CHAN_APVL_ID, CHAN_APVL_DT, CHAN_TYPE_ID, CHAN_CLASS, CHAN_TIMEOUT, CHAN_EDITABLE, CHAN_HAS_HELP, CHAN_FNAME, CHAN_SECURE) VALUES ( 91, 'User Preferences', 'User Preferences', 'DLM User Preferences channel', 0, '2005-04-20 00:00:00', 0, '2005-04-20 00:00:00', 1, 'org.jasig.portal.channels.DLMUserPreferences.CUserPreferences', 10000, 'N', 'N', 'portal/userpreferences/dlm', 'N');
INSERT INTO UP_CHANNEL ( CHAN_ID, CHAN_TITLE, CHAN_NAME, CHAN_DESC, CHAN_PUBL_ID, CHAN_PUBL_DT, CHAN_APVL_ID, CHAN_APVL_DT, CHAN_TYPE_ID, CHAN_CLASS, CHAN_TIMEOUT, CHAN_EDITABLE, CHAN_HAS_HELP, CHAN_HAS_ABOUT, CHAN_FNAME, CHAN_SECURE) VALUES ( 82, 'Password Management', 'Password Management', 'Allows management of passwords', 0, '2005-04-20 00:00:00', 0, '2005-04-20 00:00:00', 1, 'org.jasig.portal.channels.cusermanager.CUserManager', 100000, 'N', 'Y', 'N', 'passwordmgr', 'N');
INSERT INTO UP_CHANNEL ( CHAN_ID, CHAN_TITLE, CHAN_NAME, CHAN_DESC, CHAN_PUBL_ID, CHAN_PUBL_DT, CHAN_APVL_ID, CHAN_APVL_DT, CHAN_TYPE_ID, CHAN_CLASS, CHAN_TIMEOUT, CHAN_EDITABLE, CHAN_HAS_HELP, CHAN_HAS_ABOUT, CHAN_FNAME, CHAN_SECURE ) VALUES ( 50, 'DLM Administrator''s Guide', 'DLM Administrator''s Guide', 'A channel that presents an administrator''s guide for Distributed Layout Management.', 2, '2005-04-20 00:00:00', 2, '2005-04-20 00:00:00', -1, 'org.jasig.portal.layout.dlm.channels.guide.DlmIntroChannel', 5000, 'N', 'N', 'N', 'dlm.admin.guide', 'N' );


UPDATE UP_CHAN_TYPE SET TYPE_NAME='XML SSL' WHERE TYPE_ID=1;

INSERT INTO UP_CHAN_TYPE (TYPE_ID, TYPE_NAME, TYPE, TYPE_DESCR, TYPE_DEF_URI) VALUES ( 8, 'XML XSLT', 'org.jasig.portal.channels.CGenericXSLT', 'Transforms an XML document into a fragment of markup language using a single XSLT.', '/org/jasig/portal/channels/CGenericXSLT/CGenericJustXSLT.cpd');


INSERT INTO UP_SS_STRUCT (SS_ID, SS_NAME, SS_URI, SS_DESCRIPTION_URI, SS_DESCRIPTION_TEXT) VALUES ( 4, 'DLM Tabs and columns', '/org/jasig/portal/layout/DLM-tab-column/tab-column.xsl', '/org/jasig/portal/layout/DLM-tab-column/tab-column.sdf', 'Presents the DLM layout in terms of tabs and columns.');

INSERT INTO UP_SS_THEME ( SS_ID, SS_NAME, SS_URI, SS_DESCRIPTION_URI, SS_DESCRIPTION_TEXT, STRUCT_SS_ID, SAMPLE_ICON_URI, SAMPLE_URI, MIME_TYPE, DEVICE_TYPE, SERIALIZER_NAME, UP_MODULE_CLASS) VALUES ( 4, 'DLM Nested tables', '/org/jasig/portal/layout/DLM-tab-column/nested-tables/nested-tables.xsl', '/org/jasig/portal/layout/DLM-tab-column/nested-tables/nested-tables.sdf', 'Renders DLM tabs and columns as nested tables', 4, 'media/org/jasig/portal/layout/DLM-tab-column/nested-tables/sample_icon.gif', 'media/org/jasig/portal/layout/DLM-tab-column/nested-tables/sample_full.gif', 'text/html', 'workstation', 'HTML', 'org.jasig.portal.channels.DLMUserPreferences.TabColumnPrefsState');


INSERT INTO UP_SS_THEME_PARM (SS_ID, PARAM_NAME, PARAM_DEFAULT_VAL, PARAM_DESCRIPT, TYPE) VALUES (4, 'skin', 'java', 'Design skin name', 1);
INSERT INTO UP_SS_THEME_PARM (SS_ID, PARAM_NAME, PARAM_DEFAULT_VAL, PARAM_DESCRIPT, TYPE) VALUES (4, 'minimized', 'false', 'Flag determining if the channel is minimized or not', 3);


INSERT INTO UP_SS_STRUCT_PAR (SS_ID, PARAM_NAME, PARAM_DEFAULT_VAL, PARAM_DESCRIPT, TYPE) VALUES (4, 'activeTab', '1', 'The number of the DLM tab that is initially active', 1);
INSERT INTO UP_SS_STRUCT_PAR (SS_ID, PARAM_NAME, PARAM_DEFAULT_VAL, PARAM_DESCRIPT, TYPE) VALUES (4, 'width', '100%', 'Width of a DLM column', 2);
INSERT INTO UP_SS_MAP (THEME_SS_ID, STRUCT_SS_ID, MIME_TYPE) VALUES (4, 4, 'text/html');


INSERT INTO UP_PERMISSION (OWNER, PRINCIPAL_TYPE, PRINCIPAL_KEY, ACTIVITY, TARGET, PERMISSION_TYPE) VALUES ('UP_FRAMEWORK', 3, 'local.0', 'SUBSCRIBE', 'CHAN_ID.91', 'GRANT');
INSERT INTO UP_PERMISSION (OWNER, PRINCIPAL_TYPE, PRINCIPAL_KEY, ACTIVITY, TARGET, PERMISSION_TYPE) VALUES ('UP_FRAMEWORK', 3, 'local.1', 'SUBSCRIBE', 'CHAN_ID.82', 'GRANT');
INSERT INTO UP_PERMISSION (OWNER, PRINCIPAL_TYPE, PRINCIPAL_KEY, ACTIVITY, TARGET, PERMISSION_TYPE) VALUES ('UP_FRAMEWORK', 3, 'local.2', 'SUBSCRIBE', 'CHAN_ID.82', 'GRANT');
INSERT INTO UP_PERMISSION (OWNER, PRINCIPAL_TYPE, PRINCIPAL_KEY, ACTIVITY, TARGET, PERMISSION_TYPE) VALUES ('UP_FRAMEWORK', 3, 'local.3', 'SUBSCRIBE', 'CHAN_ID.82', 'GRANT');
INSERT INTO UP_PERMISSION (OWNER, PRINCIPAL_TYPE, PRINCIPAL_KEY, ACTIVITY, TARGET, PERMISSION_TYPE) VALUES ('org.jasig.portal.channels.cusermanager.CUserManager', 3, 'local.14', 'acctmgr', 'Account Manager', 'GRANT');
INSERT INTO UP_PERMISSION (OWNER, PRINCIPAL_TYPE, PRINCIPAL_KEY, ACTIVITY, TARGET, PERMISSION_TYPE) VALUES ('UP_FRAMEWORK', 3, 'local.0', 'SUBSCRIBE', 'CHAN_ID.50', 'GRANT');

INSERT INTO UP_GROUP_MEMBERSHIP (GROUP_ID, MEMBER_SERVICE, MEMBER_KEY, MEMBER_IS_GROUP) VALUES (53, 'local', '82', 'F');
INSERT INTO UP_GROUP_MEMBERSHIP (GROUP_ID, MEMBER_SERVICE, MEMBER_KEY, MEMBER_IS_GROUP) VALUES (53, 'local', '50', 'F');
UPDATE UP_CHANNEL  SET CHAN_CLASS='org.jasig.portal.layout.alm.channels.CFragmentManager' where CHAN_CLASS='org.jasig.portal.layout.channels.CFragmentManager';
ALTER TABLE ASSESSMENT_RESULT DROP CONSTRAINT ASSESSMENT_RESULT_USER_NAME_FK;
ALTER TABLE CHAT_USERS DROP CONSTRAINT CHAT_USERS_USER_NAME_FK;
ALTER TABLE CALENDAR DROP CONSTRAINT CALENDAR_USER_NAME_FK;
ALTER TABLE CHANNEL_PREFERENCE DROP CONSTRAINT CHAN_PREF_USER_NAME_FK;
ALTER TABLE GRADEBOOK_SCORE DROP CONSTRAINT GBOOK_SCORE_USER_NAME_FK;
ALTER TABLE NOTEPAD DROP CONSTRAINT NOTEPAD_USER_NAME_FK;
altER TABLE USER_ACTIVATION DROP CONSTRAINT USER_ACT_USER_NAME_FK;
ALTER TABLE MEMBERSHIP DROP CONSTRAINT MEMBERSHIP_USER_NAME_FK;
ALTER TABLE PERSON_DIR_ATTR DROP CONSTRAINT PERSON_DIR_ATTR_USER_NAME_FK;
ALTER TABLE PERSON_DIR_METADATA DROP CONSTRAINT PERSON_DR_MTDT_USERNAME_FK;

DROP INDEX CALENDAR.CALENDAR_USER_NAME_IDX;

ALTER TABLE UP_USER DROP CONSTRAINT UP_USER_USER_NAME_PKEY;
ALTER TABLE UP_PERSON_DIR DROP CONSTRAINT UP_PERSON_DIR_PKEY;

ALTER TABLE up_user ALTER COLUMN user_name varchar(50) not null;
ALTER TABLE up_person_dir ALTER COLUMN user_name varchar(50) not null;
ALTER TABLE up_person_dir ALTER COLUMN last_name varchar(50) not null;
ALTER TABLE up_person_dir ALTER COLUMN first_name varchar(50) not null;
ALTER TABLE up_person_dir ALTER COLUMN email varchar(150) not null;
ALTER TABLE person_dir_attr ALTER COLUMN prefix varchar(25) not null;
ALTER TABLE person_dir_attr ALTER COLUMN suffix varchar(25) not null;
ALTER TABLE person_dir_attr ALTER COLUMN state varchar(64) not null;
ALTER TABLE person_dir_attr ALTER COLUMN zip varchar(64) not null;
ALTER TABLE topic ALTER COLUMN description varchar(2048) not null;
ALTER TABLE offering ALTER COLUMN description varchar(2048) not null;
ALTER TABLE offering ALTER COLUMN opt_offeringterm varchar(35);
ALTER TABLE ASSESSMENT_RESULT ALTER COLUMN USER_NAME varchar(50) not null;
ALTER TABLE CHAT_USERS ALTER COLUMN USER_NAME varchar(50) not null;
ALTER TABLE CALENDAR ALTER COLUMN USER_NAME varchar(50) not null;
ALTER TABLE CHANNEL_PREFERENCE ALTER COLUMN USER_NAME varchar(50) not null;
ALTER TABLE GRADEBOOK_SCORE ALTER COLUMN USER_NAME varchar(50) not null;
ALTER TABLE NOTEPAD ALTER COLUMN USER_NAME varchar(50) not null;
ALTER TABLE USER_ACTIVATION ALTER COLUMN USER_NAME varchar(50) not null;
ALTER TABLE MEMBERSHIP ALTER COLUMN USER_NAME varchar(50) not null;
ALTER TABLE PERSON_DIR_ATTR ALTER COLUMN USER_NAME varchar(50) not null;
ALTER TABLE PERSON_DIR_METADATA ALTER COLUMN USER_NAME varchar(50) not null;

ALTER TABLE UP_USER ADD CONSTRAINT UP_USER_USER_NAME_PKEY PRIMARY KEY (USER_NAME);
ALTER TABLE UP_PERSON_DIR ADD CONSTRAINT UP_PERSON_DIR_PKEY PRIMARY KEY (USER_NAME);

ALTER TABLE ASSESSMENT_RESULT ADD CONSTRAINT ASSESSMENT_RESULT_USER_NAME_FK FOREIGN KEY (USER_NAME) REFERENCES UP_USER (USER_NAME);
ALTER TABLE CHAT_USERS ADD CONSTRAINT CHAT_USERS_USER_NAME_FK FOREIGN KEY (USER_NAME) REFERENCES UP_USER (USER_NAME);
ALTER TABLE CALENDAR ADD CONSTRAINT CALENDAR_USER_NAME_FK FOREIGN KEY (USER_NAME) REFERENCES UP_USER (USER_NAME);
ALTER TABLE CHANNEL_PREFERENCE ADD CONSTRAINT CHAN_PREF_USER_NAME_FK FOREIGN KEY (USER_NAME) REFERENCES UP_USER (USER_NAME);
ALTER TABLE GRADEBOOK_SCORE ADD CONSTRAINT GBOOK_SCORE_USER_NAME_FK FOREIGN KEY (USER_NAME) REFERENCES UP_USER (USER_NAME);
ALTER TABLE NOTEPAD ADD CONSTRAINT NOTEPAD_USER_NAME_FK FOREIGN KEY (USER_NAME) REFERENCES UP_USER (USER_NAME);
ALTER TABLE USER_ACTIVATION ADD CONSTRAINT USER_ACT_USER_NAME_FK FOREIGN KEY (USER_NAME) REFERENCES UP_USER (USER_NAME);
ALTER TABLE MEMBERSHIP ADD CONSTRAINT MEMBERSHIP_USER_NAME_FK FOREIGN KEY (USER_NAME) REFERENCES UP_USER (USER_NAME);
ALTER TABLE PERSON_DIR_ATTR ADD CONSTRAINT PERSON_DIR_ATTR_USER_NAME_FK FOREIGN KEY (USER_NAME) REFERENCES UP_USER (USER_NAME);
ALTER TABLE PERSON_DIR_METADATA ADD CONSTRAINT PERSON_DR_MTDT_USERNAME_FK FOREIGN KEY (USER_NAME) REFERENCES UP_USER (USER_NAME);

update offering set enrollment_model = 'Student Information System' where enrollment_model = 'sis';

CREATE INDEX CALENDAR_USER_NAME_IDX ON CALENDAR (USER_NAME);

ALTER TABLE up_group ALTER COLUMN description VARCHAR(2048);
EXEC sp_rename 'DPCS_RECURRENCE.INTERVAL', 'INTRVL';
DROP INDEX DPCS_ENTRY.DPCS_ENTRY_COMP_IDX;
EXEC sp_rename 'DPCS_ENTRY.DAY', 'ENTRY_DAY', 'COLUMN';
CREATE INDEX DPCS_ENTRY_COMP_IDX ON DPCS_ENTRY (CEID, CESEQ, ENTRY_DAY, EOR);
EXEC sp_rename 'DPCS_ENTRY.SCOPE', 'ENTRY_SCOPE', 'COLUMN';
EXEC sp_rename 'DPCS_RECURRENCE.DAY', 'RECUR_DAY', 'COLUMN';
UPDATE UP_VERSIONS SET MICRO='3' WHERE FNAME='UP_FRAMEWORK';

INSERT INTO UP_VERSIONS (FNAME, MAJOR, MINOR, MICRO, DESCRIPTION) VALUES ('ACADEMUS_PORTAL', '2', '1', '0', 'Academus Portal marketing version number');
INSERT INTO UP_VERSIONS(FNAME, MAJOR, MINOR, MICRO, DESCRIPTION) VALUES ('ACADEMUS_BUILD', '2', '1', '9', 'Academus build number');

INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (205, 'baseHelpUrl', 'help_Briefcase_Portlet.html', 'N');

INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (139, 'baseHelpUrl', 'help_Notifications_Portlet.html', 'N');

INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (138, 'baseHelpUrl', 'help_Blogging_Portlet.html', 'N'); 

ALTER TABLE UP_USER ADD INITIAL_LOGIN_DATE DATETIME;
UPDATE UP_VERSIONS SET MICRO='3' WHERE FNAME='ACADEMUS_BUILD';
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (3, 'baseHelpUrl', 'help_Bookmark_Channel.html', 'N');
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (101, 'baseHelpUrl', 'help_Introduction_to_Groupware_Administration_Channels.html%23213', 'N');
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (108, 'baseHelpUrl', 'help_Calendar_Channel.html', 'N');
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (109, 'baseHelpUrl', 'help_Calendar_Channel.html', 'N');
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (110, 'baseHelpUrl', 'help_Calendar_Channel.html', 'N');
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (112, 'baseHelpUrl', 'help_My_Notes_Channel.html', 'N');
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (113, 'baseHelpUrl', 'help_Address_Book_Channel.html', 'N');
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (118, 'baseHelpUrl', 'help_Classifieds_Channel.html', 'N');
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (119, 'baseHelpUrl', 'help_Campus_News_Channel.html', 'N');
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (120, 'baseHelpUrl', 'help_Webmail_Channel.html', 'N');
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (124, 'baseHelpUrl', 'help_Discussion_Forums_Channel.html%23156', 'N');
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (125, 'baseHelpUrl', 'help_Group_Chat_Channel.html', 'N');
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (127, 'baseHelpUrl', 'help_Portal_User_Administration_Channel.html', 'N');
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (134, 'baseHelpUrl', 'help_Group_Chat_Channel.html%23164', 'N');
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (2013, 'baseHelpUrl', 'help_Campus_Announcements_Channel.html', 'N');
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (2014, 'baseHelpUrl', 'help_Discussion_Forums_Channel.html', 'N');
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (2015, 'baseHelpUrl', 'help_Survey_Channel.html', 'N');
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (2016, 'baseHelpUrl', 'help_Survey_Channel.html', 'N');
INSERT INTO UP_CHANNEL_PARAM (CHAN_ID, CHAN_PARM_NM, CHAN_PARM_VAL, CHAN_PARM_OVRD) VALUES (2017, 'baseHelpUrl', 'help_Survey_Channel.html%23193', 'N');

UPDATE UP_CHANNEL_PARAM SET CHAN_ID = '2005' WHERE CHAN_ID = '205' ;

create table HG_USAGE (
	USER_NAME VARCHAR(80) PRIMARY KEY NOT NULL,
	INITIAL_USAGE_DATE DATETIME NOT NULL
);

insert into HG_USAGE (USER_NAME, INITIAL_USAGE_DATE)
	select USER_NAME, INITIAL_LOGIN_DATE from UP_USER
	where INITIAL_LOGIN_DATE is not null;

drop index up_user_layout.up_user_layout_ck_idx;
alter table up_user_layout drop column cache_key;