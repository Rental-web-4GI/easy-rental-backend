-- =========================================================================
-- Backfill des kernel_principal pour les comptes migrés par Kernel
-- =========================================================================
-- Contexte : Kernel a remplacé silencieusement l'identifiant de connexion
-- des comptes dont le username envoyé au signup était invalide (contenait
-- un @, car on envoyait l'email complet). Le nouvel identifiant est de
-- la forme pending-<uuid>@yowyob.com.
--
-- Le mapping ci-dessous a été vérifié en interrogeant discover-contexts
-- avec les identifiants correspondants (voir logs). L'utilisateur continue
-- de saisir son email d'origine dans l'UI Easy Rental — c'est le backend
-- qui traduit vers le principal Kernel via le champ kernel_principal.
--
-- À exécuter UNE FOIS sur la base de production, après le déploiement
-- de la migration 21-users-kernel-principal.xml.
-- =========================================================================

BEGIN;

-- pdennybrayan@gmail.com  →  pending-499f433ab5db2e3c0c04f763@yowyob.com
UPDATE users
SET kernel_principal = 'pending-499f433ab5db2e3c0c04f763@yowyob.com'
WHERE email = 'pdennybrayan@gmail.com'
  AND (kernel_principal IS NULL OR kernel_principal = '');

-- aizakifujimoto@gmail.com  →  pending-da654379b2a30c6c4c1a8ea2@yowyob.com
UPDATE users
SET kernel_principal = 'pending-da654379b2a30c6c4c1a8ea2@yowyob.com'
WHERE email = 'aizakifujimoto@gmail.com'
  AND (kernel_principal IS NULL OR kernel_principal = '');

-- johnnyfinger71@gmail.com  →  pending-3a534cd3868813d178f86994@yowyob.com
UPDATE users
SET kernel_principal = 'pending-3a534cd3868813d178f86994@yowyob.com'
WHERE email = 'johnnyfinger71@gmail.com'
  AND (kernel_principal IS NULL OR kernel_principal = '');

-- Les deux autres comptes (escanoragre, sanchezroycmr) doivent être mis
-- à jour dès qu'on aura leur mail. Copier-coller les deux lignes suivantes
-- en remplaçant le pending-<uuid> par celui reçu dans le mail Kernel.

-- UPDATE users
-- SET kernel_principal = 'pending-XXXXXXXXXXXXXXXXXXXXXXXX@yowyob.com'
-- WHERE email = 'escanoragre@gmail.com'
--   AND (kernel_principal IS NULL OR kernel_principal = '');

-- UPDATE users
-- SET kernel_principal = 'pending-XXXXXXXXXXXXXXXXXXXXXXXX@yowyob.com'
-- WHERE email = 'sanchezroycmr@gmail.com'
--   AND (kernel_principal IS NULL OR kernel_principal = '');

-- Vérification : afficher les mappings restaurés
SELECT email, kernel_principal, kernel_user_id, kernel_actor_id
FROM users
WHERE email IN (
  'pdennybrayan@gmail.com',
  'aizakifujimoto@gmail.com',
  'johnnyfinger71@gmail.com',
  'escanoragre@gmail.com',
  'sanchezroycmr@gmail.com'
);

COMMIT;
