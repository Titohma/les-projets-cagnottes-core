--liquibase formatted sql

--changeset lesprojetscagnottes:merge-ideas-projects
ALTER TABLE projects
    ADD idea_has_anonymous_creator boolean,
    ADD idea_has_leader_creator boolean,
    ADD published boolean;
--rollback alter table projects drop column idea_has_anonymous_creator,drop column idea_has_leader_creator, drop column published;

--changeset lesprojetscagnottes:migrate-idea-projects
insert into projects(
	id,
	created_at,
	created_by,
	updated_at,
	updated_by,
	title,
	short_description,
	long_description,
	people_required,
	status,
	leader_id,
	organization_id,
	last_status_update,
	idea_has_anonymous_creator,
	idea_has_leader_creator,
	published
	)
(
	select nextval('projects_seq'),
	created_at,
	created_by,
	updated_at,
	updated_by,
	short_description as "title",
	short_description,
	long_description,
	NULL,
	'IDEA',
	submitter_id as "leader_id",
	organization_id,
	updated_at as "last_status_update",
	has_anonymous_creator as "idea_has_anonymous_creator",
	has_leader_creator as "idea_has_leader_creator",
	TRUE
	from ideas
);

--changeset lesprojetscagnottes:drop-ideas
DROP TABLE ideas;
--rollback CREATE TABLE ideas (
--rollback   id int8 NOT NULL,
--rollback   created_at timestamp NULL DEFAULT now(),
--rollback   created_by varchar(255) NULL DEFAULT 'System'::character varying,
--rollback 	 has_anonymous_creator bool NULL,
--rollback 	 has_leader_creator bool NULL,
--rollback 	 icon varchar(255) NULL,
--rollback 	 long_description text NULL,
--rollback 	 short_description varchar(255) NULL,
--rollback 	 updated_at timestamp NULL DEFAULT now(),
--rollback 	 updated_by varchar(255) NULL DEFAULT 'System'::character varying,
--rollback 	 organization_id int8 NULL,
--rollback 	 submitter_id int8 NULL,
--rollback 	 user_id int8 NULL,
--rollback 	 workspace varchar(255) NULL,
--rollback 	 CONSTRAINT ideas_pkey PRIMARY KEY (id),
--rollback 	 CONSTRAINT fk_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
--rollback 	 CONSTRAINT fk_submitter FOREIGN KEY (submitter_id) REFERENCES users(id)
--rollback );

--changeset lesprojetscagnottes:create-sequence-news_notifications
CREATE SEQUENCE IF NOT EXISTS news_notifications_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence news_notifications_seq;

--changeset lesprojetscagnottes:create-table-news_notifications
CREATE TABLE IF NOT EXISTS news_notifications (
    id bigint primary key,
    created_at timestamp without time zone DEFAULT now(),
    created_by character varying(255) DEFAULT 'System'::character varying,
    updated_at timestamp without time zone DEFAULT now(),
    updated_by character varying(255) DEFAULT 'System'::character varying,
    sent boolean,
    notification_id bigint,
    news_id bigint
);
--rollback drop table news_notifications;

--changeset lesprojetscagnottes:add-fk-news_notifications
ALTER TABLE ONLY news_notifications
    ADD CONSTRAINT fk_notification FOREIGN KEY (notification_id) REFERENCES notifications(id),
    ADD CONSTRAINT fk_news FOREIGN KEY (news_id) REFERENCES news(id);
--rollback alter table news_notifications drop constraint fk_news;
--rollback alter table news_notifications drop constraint fk_notification;

--changeset lesprojetscagnottes:add-campaigns-time-required
ALTER TABLE campaigns
    ADD days_required integer,
    ADD hours_required integer,
    ADD total_required real;
--rollback alter table projects drop column days_required,drop column hours_required, drop column total_required;

--changeset lesprojetscagnottes:set-campaigns-time-required
UPDATE campaigns SET days_required = 0, hours_required = 0, total_required = donations_required;

--changeset lesprojetscagnottes:add-budgets-cost-of-time-required
ALTER TABLE budgets
    ADD can_finance_time boolean,
    ADD cost_of_day real,
    ADD cost_of_hour real;
--rollback alter table projects drop column can_finance_time, drop column cost_of_day,drop column cost_of_hour;

--changeset lesprojetscagnottes:set-budgets-cost-of-time-required
UPDATE budgets SET can_finance_time = false, cost_of_day = 0, cost_of_hour = 0;

--changeset lesprojetscagnottes:create-sequence-votes
CREATE SEQUENCE IF NOT EXISTS votes_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence votes_seq;

--changeset lesprojetscagnottes:create-table-votes
CREATE TABLE IF NOT EXISTS votes (
    id bigint primary key,
    created_at timestamp without time zone DEFAULT now(),
    created_by character varying(255) DEFAULT 'System'::character varying,
    updated_at timestamp without time zone DEFAULT now(),
    updated_by character varying(255) DEFAULT 'System'::character varying,
    type character varying(50),
    project_id bigint,
    user_id bigint
);
--rollback drop table votes;

--changeset lesprojetscagnottes:add-fk-votes
ALTER TABLE ONLY votes
    ADD CONSTRAINT fk_project FOREIGN KEY (project_id) REFERENCES projects(id),
    ADD CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id);
--rollback alter table campaigns drop constraint fk_user;
--rollback alter table campaigns drop constraint fk_project;

--changeset lesprojetscagnottes:update-fk-news_notifications-cascade
ALTER TABLE news_notifications
    DROP CONSTRAINT fk_notification,
    ADD CONSTRAINT fk_notification FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE;
--rollback alter table news_notifications drop constraint fk_notification add constraint fk_notification foreign key (notification_id) references notifications(id);

--changeset lesprojetscagnottes:update-fk-slack_notifications-cascade
ALTER TABLE slack_notifications
    DROP CONSTRAINT fk_notification,
    ADD CONSTRAINT fk_notification FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE;
--rollback alter table slack_notifications drop constraint fk_notification add constraint fk_notification foreign key (notification_id) references notifications(id);

--changeset lesprojetscagnottes:update-fk-ms_notifications-cascade
ALTER TABLE ms_notifications
    DROP CONSTRAINT fk_notification,
    ADD CONSTRAINT fk_notification FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE;
--rollback alter table ms_notifications drop constraint fk_notification add constraint fk_notification foreign key (notification_id) references notifications(id);

--changeset lesprojetscagnottes:update-data-delete-accounts-for-disabled-users
delete from accounts as a
using
	budgets as b,
	users as u
where
	b.id = a.budget_id
	and u.id = a.owner_id
	and b.end_date > now()
	and u.enabled = false
	and a.initial_amount = a.amount;
