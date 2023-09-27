CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- PURGE

delete from public.ms_notifications;
delete from public.ms_user;
delete from public.ms_team;
delete from public.slack_notifications;
delete from public.slack_user;
delete from public.slack_team;
delete from public.notifications;
delete from public.news;
delete from public.donations;
delete from public.campaigns;
delete from public.projects_members;
delete from public.projects;
delete from public.accounts;
delete from public.budgets;
delete from public.contents;
delete from public.user_authority_organizations;
delete from public.organizations_users;
delete from public.user_authority;
delete from public.api_tokens;
delete from public.users;
delete from public.organizations_authorities;
delete from public.organizations;

-- ORGANIZATION

insert into public.organizations
(id, created_at, created_by, updated_at, updated_by, logo_url, "name", social_name)
values
(nextval('organizations_seq'), NOW(), 'System', NOW(), 'System', 'https://eu.ui-avatars.com/api/?name=Super+Boite', 'Super Boite', 'Notre Super Boite');

--- ORGANIZATION AUTHORITIES

insert into public.organizations_authorities
(id, created_at, created_by, updated_at, updated_by, "name", organization_id)
values
(nextval('organizations_authorities_seq'), NOW(), 'System', NOW(), 'System', 'ROLE_SPONSOR', (select id from organizations where name = 'Super Boite')),
(nextval('organizations_authorities_seq'), NOW(), 'System', NOW(), 'System', 'ROLE_MANAGER', (select id from organizations where name = 'Super Boite')),
(nextval('organizations_authorities_seq'), NOW(), 'System', NOW(), 'System', 'ROLE_OWNER', (select id from organizations where name = 'Super Boite'));

--- USERS

insert into public.users
(id, created_at, created_by, updated_at, updated_by, avatar_url, email,
enabled, firstname, lastpasswordresetdate, lastname, "password", username)
values
(nextval('users_seq'), NOW(), 'System', NOW(), 'System', 'https://eu.ui-avatars.com/api/?name=Charlotte+Paquette', 'CharlottePaquette@rhyta.com',
true, 'Charlotte', NOW(), 'Paquette', '$2a$10$mTExeAyURZPO4mE7GEbQA.KCB/t.rf8NgCg67vaj7wB.ZzzHG78Ai', 'CharlottePaquette@rhyta.com'),
(nextval('users_seq'), NOW(), 'System', NOW(), 'System', 'https://eu.ui-avatars.com/api/?name=Alphonse+Lejeune', 'AlphonseLejeune@teleworm.us',
true, 'Alphonse', NOW(), 'Lejeune', '$2a$10$MSBSKYkqNF5AklPxR6VPOu05q4UlDWC9Oah71PFx5EAa3aMss/8V6', 'AlphonseLejeune@teleworm.us'),
(nextval('users_seq'), NOW(), 'System', NOW(), 'System', 'https://eu.ui-avatars.com/api/?name=Denis+Giroux', 'DenisGiroux@dayrep.com',
true, 'Denis', NOW(), 'Giroux', '$2a$10$xwq1y1xADwLzwFg/HloAg.w0SuWFTaOveBqvE8XbryXR.rfkTyUKy', 'DenisGiroux@dayrep.com'),
(nextval('users_seq'), NOW(), 'System', NOW(), 'System', 'https://eu.ui-avatars.com/api/?name=Matilda+Caisse', 'MatildaCaisse@dayrep.com',
true, 'Matilda', NOW(), 'Caisse', '$2a$10$AFJJJrZWD.b9zhY5jXeI2uUnmoG9Zah5tnFeWe2Ewb3OQeFx6ORAS', 'MatildaCaisse@dayrep.com');

--- USER AUTHORITY

insert into public.user_authority
(user_id,authority_id)
values
((select id from users where username = 'CharlottePaquette@rhyta.com'), (select id from authorities where name = 'ROLE_USER')),
((select id from users where username = 'AlphonseLejeune@teleworm.us'), (select id from authorities where name = 'ROLE_USER')),
((select id from users where username = 'DenisGiroux@dayrep.com'), (select id from authorities where name = 'ROLE_USER')),
((select id from users where username = 'MatildaCaisse@dayrep.com'), (select id from authorities where name = 'ROLE_USER'));

--- ORGANIZATIONS USERS

INSERT INTO public.organizations_users
(organization_id, user_id)
values
((select id from organizations where name = 'Super Boite'), (select id from users where username = 'CharlottePaquette@rhyta.com')),
((select id from organizations where name = 'Super Boite'), (select id from users where username = 'AlphonseLejeune@teleworm.us')),
((select id from organizations where name = 'Super Boite'), (select id from users where username = 'DenisGiroux@dayrep.com')),
((select id from organizations where name = 'Super Boite'), (select id from users where username = 'MatildaCaisse@dayrep.com'));

--- USER AUTHORITY ORGANIZATIONS

insert into public.user_authority_organizations
(user_id,organization_authority_id)
VALUES
((select id from users where username = 'AlphonseLejeune@teleworm.us'), (select id from organizations_authorities where name = 'ROLE_SPONSOR' and organization_id = (select id from organizations where name = 'Super Boite'))),
((select id from users where username = 'CharlottePaquette@rhyta.com'), (select id from organizations_authorities where name = 'ROLE_MANAGER' and organization_id = (select id from organizations where name = 'Super Boite'))),
((select id from users where username = 'DenisGiroux@dayrep.com'), (select id from organizations_authorities where name = 'ROLE_OWNER' and organization_id = (select id from organizations where name = 'Super Boite')));

--- CONTENTS

INSERT INTO public.contents (id,created_at,created_by,updated_at,updated_by,organization_id,"name","value") values
(nextval('contents_seq'),NOW(),'AlphonseLejeune@teleworm.us',NOW(),'AlphonseLejeune@teleworm.us',(select id from organizations where name = 'Super Boite'),'Règles d''utilisation du budget Super Boite','<h2 id="eligibilit-">Eligibilité</h2>
<p>Attention : tous les projets ne sont pas éligibles aux campagnes cagnottes. Avant de soumettre votre campagne sur la plateforme, vérifiez que vous respectez bien les règles suivantes :</p>
<blockquote>
<p>Ceci est une démo, faites ce que vous voulez</p>
</blockquote>
 ');

--- BUDGETS

insert into public.budgets
(id, created_at, created_by, updated_at, updated_by,
amount_per_member,
end_date,
is_distributed,
"name",
start_date,
total_donations,
organization_id,
rules_id,
sponsor_id)
values
(nextval('budgets_seq'), NOW(), 'AlphonseLejeune@teleworm.us', NOW(), 'AlphonseLejeune@teleworm.us',
500.0,
(SELECT date_trunc('month', now() - interval '6 months') + interval '1 year' - interval '1 sec'),
true,
'Cagnotte',
(date_trunc('month', now() - interval '6 months')),
0.0,
(select id from organizations where name = 'Super Boite'),
(select id from contents where name = 'Règles d''utilisation du budget Super Boite'),
(select id from users where username = 'AlphonseLejeune@teleworm.us'));

--- ACCOUNTS

insert into public.accounts
(id, created_at, created_by, updated_at, updated_by, amount, initial_amount, budget_id, owner_id)
values
(nextval('accounts_seq'), now(), 'AlphonseLejeune@teleworm.us', now(), 'AlphonseLejeune@teleworm.us', 500.0, 500.0, (select id from budgets where name = 'Cagnotte'), (select id from users where username = 'CharlottePaquette@rhyta.com')),
(nextval('accounts_seq'), now(), 'AlphonseLejeune@teleworm.us', now(), 'AlphonseLejeune@teleworm.us', 500.0, 500.0, (select id from budgets where name = 'Cagnotte'), (select id from users where username = 'AlphonseLejeune@teleworm.us')),
(nextval('accounts_seq'), now(), 'AlphonseLejeune@teleworm.us', now(), 'AlphonseLejeune@teleworm.us', 500.0, 500.0, (select id from budgets where name = 'Cagnotte'), (select id from users where username = 'DenisGiroux@dayrep.com')),
(nextval('accounts_seq'), now(), 'AlphonseLejeune@teleworm.us', now(), 'AlphonseLejeune@teleworm.us', 500.0, 500.0, (select id from budgets where name = 'Cagnotte'), (select id from users where username = 'MatildaCaisse@dayrep.com'));

--- PROJECTS

insert into public.projects
(id,created_at,created_by,updated_at,updated_by,people_required,short_description,status,title,leader_id,workspace,organization_id,long_description,published)
values
(nextval('projects_seq'), now() - interval '2 months', 'DenisGiroux@dayrep.com', now() - interval '2 months', 'DenisGiroux@dayrep.com',
2, 'Pour la salle de pause', 'FINISHED', 'Du nouveau mobilier', (select id from users where username = 'DenisGiroux@dayrep.com'),
uuid_generate_v4(),(select id from organizations where name = 'Super Boite'),
'<h1 id="du-nouveau-mobilier">Du nouveau mobilier</h1>
<h2 id="de-quoi-s-agit-il-">De quoi s&#39;agit-il ?</h2>
<h2 id="qui-est-concern-">Qui est concerné ?</h2>
<h2 id="a-quoi-va-servir-le-budget-">A quoi va servir le budget ?</h2>
<h2 id="pourquoi-a-me-tient-coeur">Pourquoi ça me tient à coeur</h2>', true),
(nextval('projects_seq'), now() - interval '1 month', 'CharlottePaquette@rhyta.com', now() - interval '1 month', 'CharlottePaquette@rhyta.com',
4, 'Notre boite est la meilleure', 'FINISHED', 'Un feu d''artifice pour le séminaire annuel', (select id from users where username = 'CharlottePaquette@rhyta.com'),
uuid_generate_v4(),(select id from organizations where name = 'Super Boite'),
'<h1 id="allumer-le-feu">Allumer le feu</h1>
<h2 id="de-quoi-s-agit-il-">De quoi s&#39;&#39;agit-il ?</h2>
<h2 id="qui-est-concern-">Qui est concerné ?</h2>
<h2 id="a-quoi-va-servir-le-budget-">A quoi va servir le budget ?</h2>
<h2 id="pourquoi-a-me-tient-coeur">Pourquoi ça me tient à coeur</h2>', true),
(nextval('projects_seq'), now() - interval '6 days', 'MatildaCaisse@dayrep.com', now() - interval '6 days', 'MatildaCaisse@dayrep.com',
2, 'Avec ça, nos concurrents n''ont qu''à bien se tenir','IN_PROGRESS','Le projet ABCD', (select id from users where username = 'MatildaCaisse@dayrep.com'),
uuid_generate_v4(),(select id from organizations where name = 'Super Boite'),
'<h1 id="le-projet-abcd">Le projet ABCD</h1>
<h2 id="de-quoi-s-agit-il-">De quoi s&#39;&#39;agit-il ?</h2>
<h2 id="qui-est-concern-">Qui est concerné ?</h2>
<h2 id="a-quoi-va-servir-le-budget-">A quoi va servir le budget ?</h2>
<h2 id="pourquoi-c-est-important">Pourquoi c&#39;&#39;est important</h2>', true);

--- PROJECTS MEMBERS

insert into public.projects_members
(project_id,user_id) values
((select id from projects where title = 'Un feu d''artifice pour le séminaire annuel'), (select id from users where username = 'CharlottePaquette@rhyta.com')),
((select id from projects where title = 'Du nouveau mobilier'), (select id from users where username = 'DenisGiroux@dayrep.com')),
((select id from projects where title = 'Du nouveau mobilier'), (select id from users where username = 'MatildaCaisse@dayrep.com')),
((select id from projects where title = 'Du nouveau mobilier'), (select id from users where username = 'CharlottePaquette@rhyta.com')),
((select id from projects where title = 'Le projet ABCD'), (select id from users where username = 'MatildaCaisse@dayrep.com')),
((select id from projects where title = 'Le projet ABCD'), (select id from users where username = 'DenisGiroux@dayrep.com'));

--- CAMPAIGNS

insert into public.campaigns
(id,created_at,created_by,updated_at,updated_by,project_id,budget_id,donations_required,funding_deadline,status,total_donations)
values
(nextval('campaigns_seq'), now() - interval '2 months', 'DenisGiroux@dayrep.com', now() - interval '2 months', 'DenisGiroux@dayrep.com',
(select id from projects where title = 'Du nouveau mobilier'),(select id from budgets where name = 'Cagnotte'),800.0, now() - interval '1 month', 'SUCCESSFUL', 0.0),
(nextval('campaigns_seq'), now() - interval '1 month', 'CharlottePaquette@rhyta.com', now() - interval '1 month', 'CharlottePaquette@rhyta.com',
(select id from projects where title = 'Un feu d''artifice pour le séminaire annuel'),(select id from budgets where name = 'Cagnotte'),2000.0, now() - interval '14 days', 'FAILED', 0.0),
(nextval('campaigns_seq'), now() - interval '6 days', 'MatildaCaisse@dayrep.com', now() - interval '6 days', 'MatildaCaisse@dayrep.com',
(select id from projects where title = 'Le projet ABCD'),(select id from budgets where name = 'Cagnotte'),500.0, now() + interval '15 days', 'IN_PROGRESS', 0.0);

--- DONATIONS

SELECT public.create_donation(
(select id from accounts where budget_id =((select id from budgets where name = 'Cagnotte')) and owner_id = (select id from users where username = 'DenisGiroux@dayrep.com')),
(select id from campaigns where created_by = 'DenisGiroux@dayrep.com'), 400.0);
SELECT public.create_donation(
(select id from accounts where budget_id =((select id from budgets where name = 'Cagnotte')) and owner_id = (select id from users where username = 'MatildaCaisse@dayrep.com')),
(select id from campaigns where created_by = 'DenisGiroux@dayrep.com'), 200.0);
SELECT public.create_donation(
(select id from accounts where budget_id =((select id from budgets where name = 'Cagnotte')) and owner_id = (select id from users where username = 'AlphonseLejeune@teleworm.us')),
(select id from campaigns where created_by = 'DenisGiroux@dayrep.com'), 200.0);
SELECT public.create_donation(
(select id from accounts where budget_id =((select id from budgets where name = 'Cagnotte')) and owner_id = (select id from users where username = 'MatildaCaisse@dayrep.com')),
(select id from campaigns where created_by = 'MatildaCaisse@dayrep.com'), 300.0);

UPDATE public.donations
	SET created_at=(now() - interval '45 days'), updated_at=(now() - interval '45 days')
	WHERE campaign_id=(select id from campaigns where created_by = 'DenisGiroux@dayrep.com');
UPDATE public.donations
	SET created_at=(now() - interval '6 days'), updated_at=(now() - interval '6 days')
	WHERE campaign_id=(select id from campaigns where created_by = 'MatildaCaisse@dayrep.com');

--- IDEAS

INSERT INTO public.projects
(id, created_at, created_by, updated_at, updated_by, long_description, people_required, short_description, status, title, leader_id, workspace, organization_id, last_status_update, idea_has_anonymous_creator, idea_has_leader_creator, published)
values
(nextval('projects_seq'), now(), 'CharlottePaquette@rhyta.com', now(), 'CharlottePaquette@rhyta.com', 'Ca va être génial !<br>', NULL, 'Clean walk du quartier', 'IDEA', 'Clean walk du quartier', (select id from users where username = 'CharlottePaquette@rhyta.com'), NULL, (select id from organizations where name = 'Super Boite'), now(), false, false, true),
(nextval('projects_seq'), now(), 'anonymous', now(), 'anonymous', 'C''est galère de poser des congés.<br>', NULL, 'Optimiser le logiciel de pose des congés', 'IDEA', 'Optimiser le logiciel de pose des congés', NULL, NULL, (select id from organizations where name = 'Super Boite'), now(), true, false, true),
(nextval('projects_seq'), now(), 'DenisGiroux@dayrep.com', now(), 'DenisGiroux@dayrep.com', 'Qui n''aime pas les fruits ?<br>', NULL, 'Des fruits frais en salle de pause', 'IDEA', 'Des fruits frais en salle de pause', (select id from users where username = 'DenisGiroux@dayrep.com'), NULL, (select id from organizations where name = 'Super Boite'), now(), false, true, true);
