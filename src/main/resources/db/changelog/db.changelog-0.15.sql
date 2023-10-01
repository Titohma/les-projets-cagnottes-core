--liquibase formatted sql

--changeset lesprojetscagnottes:create-sequence-accounts
CREATE SEQUENCE IF NOT EXISTS accounts_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence accounts_seq;

--changeset lesprojetscagnottes:create-sequence-api_tokens
CREATE SEQUENCE IF NOT EXISTS api_tokens_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence api_tokens_seq;

--changeset lesprojetscagnottes:create-sequence-authorities
CREATE SEQUENCE IF NOT EXISTS authorities_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence authorities_seq;

--changeset lesprojetscagnottes:create-sequence-budgets
CREATE SEQUENCE IF NOT EXISTS budgets_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence budgets_seq;

--changeset lesprojetscagnottes:create-sequence-campaigns
CREATE SEQUENCE IF NOT EXISTS campaigns_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence campaigns_seq;

--changeset lesprojetscagnottes:create-sequence-contents
CREATE SEQUENCE IF NOT EXISTS contents_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence contents_seq;

--changeset lesprojetscagnottes:create-sequence-donations
CREATE SEQUENCE IF NOT EXISTS donations_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence donations_seq;

--changeset lesprojetscagnottes:create-sequence-files
CREATE SEQUENCE IF NOT EXISTS files_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence files_seq;

--changeset lesprojetscagnottes:create-sequence-ideas
CREATE SEQUENCE IF NOT EXISTS ideas_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence ideas_seq;

--changeset lesprojetscagnottes:create-sequence-ms_notifications
CREATE SEQUENCE IF NOT EXISTS ms_notifications_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence ms_notifications_seq;

--changeset lesprojetscagnottes:create-sequence-ms_team
CREATE SEQUENCE IF NOT EXISTS ms_team_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence ms_team_seq;

--changeset lesprojetscagnottes:create-sequence-ms_user
CREATE SEQUENCE IF NOT EXISTS ms_user_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence ms_user_seq;

--changeset lesprojetscagnottes:create-sequence-news
CREATE SEQUENCE IF NOT EXISTS news_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence news_seq;

--changeset lesprojetscagnottes:create-sequence-notifications
CREATE SEQUENCE IF NOT EXISTS notifications_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence notifications_seq;

--changeset lesprojetscagnottes:create-sequence-organizations
CREATE SEQUENCE IF NOT EXISTS organizations_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence organizations_seq;

--changeset lesprojetscagnottes:create-sequence-organizations_authorities
CREATE SEQUENCE IF NOT EXISTS organizations_authorities_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence organizations_authorities_seq;

--changeset lesprojetscagnottes:create-sequence-projects
CREATE SEQUENCE IF NOT EXISTS projects_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence projects_seq;

--changeset lesprojetscagnottes:create-sequence-slack_team
CREATE SEQUENCE IF NOT EXISTS slack_team_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence slack_team_seq;

--changeset lesprojetscagnottes:create-sequence-slack_user
CREATE SEQUENCE IF NOT EXISTS slack_user_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence slack_user_seq;

--changeset lesprojetscagnottes:create-sequence-users
CREATE SEQUENCE IF NOT EXISTS users_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence users_seq;

--changeset lesprojetscagnottes:set-sequence-value-accounts
SELECT setval('accounts_seq', (select max(id) from accounts) + 1, true);
--rollback select setval('accounts_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-api_tokens
SELECT setval('api_tokens_seq', (select max(id) from api_tokens) + 1, true);
--rollback select setval('api_tokens_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-authorities
SELECT setval('authorities_seq', (select max(id) from authorities) + 1, true);
--rollback select setval('authorities_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-budgets
SELECT setval('budgets_seq', (select max(id) from budgets) + 1, true);
--rollback select setval('budgets_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-campaigns
SELECT setval('campaigns_seq', (select max(id) from campaigns) + 1, true);
--rollback select setval('campaigns_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-contents
SELECT setval('contents_seq', (select max(id) from contents) + 1, true);
--rollback select setval('contents_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-donations
SELECT setval('donations_seq', (select max(id) from donations) + 1, true);
--rollback select setval('donations_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-files
SELECT setval('files_seq', (select max(id) from files) + 1, true);
--rollback select setval('files_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-ideas
SELECT setval('ideas_seq', (select max(id) from ideas) + 1, true);
--rollback select setval('ideas_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-ms_notifications
SELECT setval('ms_notifications_seq', (select max(id) from ms_notifications) + 1, true);
--rollback select setval('ms_notifications_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-ms_team
SELECT setval('ms_team_seq', (select max(id) from ms_team) + 1, true);
--rollback select setval('ms_team_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-ms_user
SELECT setval('ms_user_seq', (select max(id) from ms_user) + 1, true);
--rollback select setval('ms_user_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-news
SELECT setval('news_seq', (select max(id) from news) + 1, true);
--rollback select setval('news_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-notifications
SELECT setval('notifications_seq', (select max(id) from notifications) + 1, true);
--rollback select setval('notifications_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-organizations
SELECT setval('organizations_seq', (select max(id) from organizations) + 1, true);
--rollback select setval('organizations_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-organizations_authorities
SELECT setval('organizations_authorities_seq', (select max(id) from organizations_authorities) + 1, true);
--rollback select setval('organizations_authorities_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-projects
SELECT setval('projects_seq', (select max(id) from projects) + 1, true);
--rollback select setval('projects_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-slack_team
SELECT setval('slack_team_seq', (select max(id) from slack_team) + 1, true);
--rollback select setval('slack_team_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-slack_user
SELECT setval('slack_user_seq', (select max(id) from slack_user) + 1, true);
--rollback select setval('slack_user_seq', 1, true);

--changeset lesprojetscagnottes:set-sequence-value-users
SELECT setval('users_seq', (select max(id) from users) + 1, true);
--rollback select setval('users_seq', 1, true);

--changeset lesprojetscagnottes:update-function-create_donation-sequence
CREATE OR REPLACE FUNCTION create_donation(_account_id bigint, _campaign_id bigint, _amount real)
    RETURNS boolean
    LANGUAGE plpgsql
    AS '
        DECLARE
            _account_amount FLOAT4;
            _donation_id INT8;
            _budget_id INT8;
        BEGIN
            select amount into _account_amount from accounts where id = _account_id;
            IF _account_amount < _amount THEN
                RAISE EXCEPTION ''Not enough amount on account %'', _account_id
                    USING HINT = ''Please check your budget'';
                return false;
            END IF;
            select nextval(''donations_seq'') into _donation_id;
            select budget_id
                into _budget_id
                from accounts where id = _account_id;
            insert into donations (id, amount, campaign_id, account_id)
                values(_donation_id, _amount, _campaign_id, _account_id);
            update accounts set amount = (amount - _amount) where id = _account_id;
            update campaigns set total_donations = total_donations  + _amount where id = _campaign_id;
            update budgets set total_donations = total_donations + _amount where id = _budget_id;

            return true;
        END;
    ';

--changeset lesprojetscagnottes:drop-sequence-hibernate_sequence
DROP SEQUENCE hibernate_sequence;
--rollback create sequence hibernate_sequence start with 1 increment by 1 no minvalue no maxvalue cache 1;

--changeset lesprojetscagnottes:create-table-slack_notifications
CREATE TABLE IF NOT EXISTS slack_notifications (
    id bigint primary key,
    created_at timestamp without time zone DEFAULT now(),
    created_by character varying(255) DEFAULT 'System'::character varying,
    updated_at timestamp without time zone DEFAULT now(),
    updated_by character varying(255) DEFAULT 'System'::character varying,
    sent boolean,
    notification_id bigint,
    team_id bigint
);
--rollback drop table slack_notifications;

--changeset lesprojetscagnottes:add-fk-slack_notification
ALTER TABLE ONLY slack_notifications
    ADD CONSTRAINT fk_notification FOREIGN KEY (notification_id) REFERENCES notifications(id),
    ADD CONSTRAINT fk_slack_team FOREIGN KEY (team_id) REFERENCES slack_team(id);
--rollback alter table ms_notifications drop constraint fk_slack_team;
--rollback alter table ms_notifications drop constraint fk_notification;

--changeset lesprojetscagnottes:create-sequence-slack_notifications
CREATE SEQUENCE IF NOT EXISTS slack_notifications_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--rollback drop sequence slack_notifications_seq;

--changeset lesprojetscagnottes:set-sequence-value-slack_notifications
SELECT setval('slack_notifications_seq', 1, true);
--rollback select setval('slack_notifications_seq', 1, true);

--changeset lesprojetscagnottes:drop-donations-contributor_id
ALTER TABLE donations DROP COLUMN contributor_id;
--rollback alter table donations add column contributor_id bigint;
