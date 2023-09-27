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
