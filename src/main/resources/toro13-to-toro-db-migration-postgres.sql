create table HG_USAGE (
	USER_NAME VARCHAR(80) PRIMARY KEY NOT NULL,
	INITIAL_USAGE_DATE TIMESTAMP NOT NULL
);

insert into HG_USAGE (USER_NAME, INITIAL_USAGE_DATE)
	select USER_NAME, INITIAL_LOGIN_DATE from UP_USER
	where INITIAL_LOGIN_DATE is not null;

alter table up_user_layout drop column cache_key;
