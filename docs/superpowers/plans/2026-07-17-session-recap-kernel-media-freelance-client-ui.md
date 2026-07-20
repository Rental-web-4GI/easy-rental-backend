# Session Recap — 16-17 Juillet 2026

**Objet :** Récapitulatif complet de tout ce qui a été fait sur `easy-rental-backend` et `easy-rental-frontend` durant cette longue session. Sert de handoff pour reprendre le travail depuis n'importe quelle nouvelle session Claude Code.

**Branche git :** `backup` (comme d'habitude — pas de push sur main)
**Backend :** `/home/denny/Projets/projet-reseau/easy-rental-backend/`
**Frontend :** `/home/denny/Projets/projet-reseau/easy-rental-frontend/`

---

## 1. Vue d'ensemble

4 grands axes traités dans cette session :

1. **Kernel core refonte** — l'équipe DevOps a migré le kernel vers `https://kernel-core.yowyob.com/kernel-api` (nouveau préfixe `/kernel-api`) et changé le contrat de `/api/files`. Notre backend cassait partout.
2. **Media / file upload** — bug `size:0`, endpoint `/content` cassé côté kernel, création d'un proxy backend pour l'affichage des images.
3. **Client console UI refactor** — remplacement navbar horizontale par sidebar, uniformisation des cartes, restructuration des panneaux détail (Mes locations + Réservations).
4. **Feature Freelance** — début de l'implémentation "particulier avec véhicule = organisation individuelle avec 1 agence" (backend fondations faites, admin plans UI faite, reste flow inscription et vue admin).

---

## 2. Backend — Kernel integration (nouvelle URL + auth)

### 2.1 Nouveau préfixe `/kernel-api`

Le kernel a été remplacé par "IWM Backend API" sous `https://kernel-core.yowyob.com/kernel-api/*`. Toutes les URLs kernel ont besoin de ce préfixe.

**Fichiers modifiés :**
- `src/main/resources/application.properties` : `kernel.base-url=${KERNEL_BASE_URL:https://kernel-core.yowyob.com/kernel-api}`
- `src/main/resources/application-local.properties` : idem
- `src/main/java/com/yowyob/easyrental/kernel/config/KernelClientProperties.java` : default Java aligné
- `kernel-core.credentials.env` (gitignored) : mis à jour manuellement `KERNEL_BASE_URL=https://kernel-core.yowyob.com/kernel-api`

**Endpoints kernel confirmés existants** : `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`, `/api/auth/register`, `/api/auth/sign-up`, `/api/auth/email-verification/*`, `/api/auth/discover-contexts`, `/api/auth/select-context`, `/api/organizations`, `/api/agencies`, `/api/clients`, `/api/resources`, `/api/employees`, `/api/roles`, `/api/files`, `/oauth2/token`, `/oauth2/userinfo`.

### 2.2 KernelAppTokenProvider — auto refresh sans MFA

**Problème résolu :** `/api/files` (upload) et beaucoup d'endpoints kernel exigent un **Bearer token utilisateur** en plus des machine headers (X-Client-Id/X-Api-Key). Sans user session, 500 "Access Denied".

**Découverte clé :** Le flow `POST /api/auth/discover-contexts` + `POST /api/auth/select-context` avec les credentials admin fonctionne **SANS MFA** et retourne un accessToken valide 15 min.

**Fichier créé :** `src/main/java/com/yowyob/easyrental/kernel/application/KernelAppTokenProvider.java`

Comportement :
- `@PostConstruct` bootstrap : refresh async du token au démarrage (avec `Schedulers.boundedElastic()`)
- `@Scheduled(fixedDelay = 600_000L)` : refresh périodique toutes les 10 min
- `currentToken()` : accès non-bloquant au cache (jamais `.block()` sur thread reactor)
- Utilise `kernel.admin-username` + `kernel.admin-password` (mappé depuis `KERNEL_ADMIN_USERNAME`/`KERNEL_ADMIN_PASSWORD` du .env)

**Propriétés ajoutées** dans `KernelClientProperties.java` : `adminUsername`, `adminPassword`.

### 2.3 KernelContextWebFilter — fix fallback JWT local

**Fichier modifié :** `src/main/java/com/yowyob/easyrental/kernel/infrastructure/KernelContextWebFilter.java`

Avant : `resolveKernelBearer` fallback sur le JWT local si pas de kernel session → kernel rejette avec 401.
Après : retourne `Optional.empty()` si pas de session kernel → l'adapter tombe sur le app token via `.or(appTokenProvider::currentToken)`.

### 2.4 KernelWebClientAdapter — app token fallback partout

**Fichier modifié :** `src/main/java/com/yowyob/easyrental/kernel/infrastructure/adapter/KernelWebClientAdapter.java`

Injection de `KernelAppTokenProvider`, utilisé dans `applyHeaders()` :
```java
context.bearerToken()
    .or(appTokenProvider::currentToken)
    .ifPresent(token -> headers.setBearerAuth(token));
```

Bénéfice : tous les adapters qui passent par `KernelHttpPort` (KernelOrganizationAdapter, KernelResourceAdapter, KernelAdministrationAdapter, KernelTpAdapter) ont maintenant un fallback token M2M automatique.

### 2.5 KernelWebClientConfig — augmentation limite buffer

**Fichier modifié :** `src/main/java/com/yowyob/easyrental/kernel/config/KernelWebClientConfig.java`

Ajout : `.codecs(config -> config.defaultCodecs().maxInMemorySize(32 * 1024 * 1024))` → 32 MB (défaut 256 KB) pour permettre proxying d'images/fichiers.

---

## 3. Backend — Media / File upload

### 3.1 KernelFileAdapter — nouveau contrat + fix size:0

**Fichier modifié :** `src/main/java/com/yowyob/easyrental/kernel/infrastructure/adapter/KernelFileAdapter.java`

**Changements :**
1. **Parsing nouveau format kernel** : la réponse `/api/files` ne contient plus `url` ni `filename` mais `{id, fileName, contentType, size, ...}`. On construit maintenant l'URL depuis `id`.
2. **Fix `size:0` bug** : au lieu d'utiliser `builder.asyncPart("file", filePart.content(), DataBuffer.class)` (qui streamait mal), on bufferise via `DataBufferUtils.join()` puis on wrap dans `ByteArrayResource`. Le fichier arrive maintenant complet côté kernel.
3. **Fallback app token** via `appTokenProvider::currentToken`
4. **Logging détaillé** : `[kernel-file] status=... body=...` pour debug

### 3.2 MediaController — endpoint proxy pour affichage

**Fichier modifié :** `src/main/java/com/yowyob/easyrental/modules/media/infrastructure/adapter/in/web/MediaController.java`

**Nouvel endpoint** : `GET /api/media/kernel-file/{fileId}` (public, permitAll)
- Utilise `GET /api/files/{id}` du kernel (pas `/content` qui retourne 500 "Access Denied" pour tout le monde — bug kernel)
- Utilise `exchangeToMono` + `bodyToMono(byte[].class)` pour bufferiser complètement AVANT d'envoyer (évite stream fermé prématurément qui causait des hangs)
- Retourne image avec bon Content-Type

### 3.3 MediaUseCaseImpl — URL de proxy interne

**Fichier modifié :** `src/main/java/com/yowyob/easyrental/modules/media/application/MediaUseCaseImpl.java`

Après upload kernel réussi, on sauve en DB l'URL de proxy interne (au lieu de l'URL kernel direct) :
```java
String fileUrl = baseUrl + "/api/media/kernel-file/" + result.fileId();
```
Le browser affiche via cette URL proxy qui a les creds kernel côté serveur.

**Aussi :** ajout `hasKernelMachineCredentials()` guard pour skip kernel si credentials vides (avec warn), et logging complet dans `uploadLocal` pour debug.

### 3.4 SecurityConfig — endpoint proxy public

**Fichier modifié :** `src/main/java/com/yowyob/easyrental/config/SecurityConfig.java`

Ajouté `/api/media/kernel-file/**` à la liste `permitAll` (les logos/photos doivent être visibles sans auth).

### 3.5 Frontend — rewrites Next.js

**Fichier modifié :** `apps/easy-rental-web/next.config.js`

Ajout rewrites (root + par prefix MFE) :
```js
{ source: '/api/media/kernel-file/:path*', destination: `${apiBaseUrl}/api/media/kernel-file/:path*` }
{ source: `/${prefix}/api/media/kernel-file/:path*`, destination: `${apiBaseUrl}/api/media/kernel-file/:path*` }
```

### 3.6 Frontend — media.mapper.ts

**Fichier modifié :** `packages/shared-services/src/api/media.mapper.ts`

- `canonicalMediaStoragePath` : reconnaît `/api/media/kernel-file/*` comme chemin canonique (au même titre que `/uploads/*`)
- `resolveMediaDisplayUrl` : prépend le proxy MFE prefix pour ce path
- Préserve les URLs externes non-localhost (kernel CDN, render) sans les tronquer

### 3.7 Route handler Next.js pour upload multipart

**Fichier créé :** `apps/easy-rental-web/src/app/api/media-upload/route.ts`

Contourne un bug des rewrites Next.js qui ne transmettaient pas correctement les réponses multipart en dev mode (retour 200 avec body vide). Le route handler proxy manuellement l'upload en `fetch` côté serveur.

**Fichier modifié :** `packages/shared-services/src/api/extra.service.ts` — `uploadMedia` utilise directement `/api/media-upload` au lieu du proxy rewrite classique, avec token depuis `auth-session`.

### 3.8 LogoUpload.tsx — affichage post-upload

**Fichiers modifiés :** `apps/easy-rental-web/src/consoles/organisation/components/LogoUpload.tsx` + `apps/mfe-organisation/src/components/LogoUpload.tsx`

Utilise maintenant `resolveMediaDisplayUrl(value)` pour l'affichage (avant : URL brute → `<img src>` cassé après clear du localPreview).

---

## 4. Backend — Feature Freelance (Étape 1 backend faite)

### 4.1 Migrations Liquibase

**Fichiers créés :**
- `src/main/resources/db/changelog/changes/19-organization-account-type.xml` : ajoute colonne `account_type VARCHAR(20) DEFAULT 'COMPANY' NOT NULL` sur `organizations`
- `src/main/resources/db/changelog/changes/20-subscription-plan-target-type.xml` : ajoute colonne `target_type VARCHAR(20) DEFAULT 'COMPANY' NOT NULL` sur `subscription_plans`

**Fichier supprimé :** `21-seed-freelance-plans.xml` (l'user préfère créer ses plans lui-même via l'admin — les 6 plans existants restent intacts, tous marqués COMPANY).

**db.changelog-master.xml** : inclut 19 et 20.

### 4.2 Entities Java enrichies

**Fichiers modifiés :**
- `OrganizationEntity.java` : `+ String accountType = "COMPANY"`
- `SubscriptionPlanEntity.java` : `+ String targetType = "COMPANY"`
- `OrgResponseDTO.java` : `+ String accountType` (exposé pour admin)
- `OrgMapper.java` : mappe `accountType`

### 4.3 DTO + use case registerFreelance

**Fichier créé :** `src/main/java/com/yowyob/easyrental/modules/auth/dto/RegisterFreelanceRequest.java`
```java
public record RegisterFreelanceRequest(
    String firstname, String lastname, String email,
    String phone, String city, String password, UUID planId
) {}
```

**AuthUseCase port** (modifié) : `+ Mono<OrganizationEntity> registerFreelance(RegisterFreelanceRequest)`

**AuthUseCaseImpl** (modifié) :
- Injection `AgencyRepositoryPort agencyRepository`
- Nouvelle méthode `registerFreelance()` qui délègue à `registerOrganization()` avec `orgName = firstname + " " + lastname`, puis post-process :
  - `markAsFreelance()` : set `accountType=FREELANCE`, `city`, `phone`, `subscriptionPlanId`
  - `createDefaultFreelanceAgency()` : crée AgencyEntity locale (nom="Agence {city}")

**Endpoint** : `POST /auth/register/freelance` dans `AuthController.java`

### 4.4 Plans admin — targetType

**Backend :**
- `CreatePlanRequest.java` : `+ String targetType`
- `SubscriptionUseCaseImpl.createPlan` : propage `targetType` via `normalizeTargetType()` (COMPANY | FREELANCE)
- `mergePlanFields` : merger `targetType` aussi

**Frontend :**
- `subscription.mapper.ts` : `NormalizedSubscriptionPlan.targetType`, parse `target_type` en entrée, envoie en sortie via `toPlanApiPayload`
- `extra.service.ts` : `CreatePlanPayload.targetType`
- `PlansView.tsx` (admin) :
  - **3 tabs filtres** en haut : Tous · Entreprises · Freelances (avec compteurs)
  - **Badge coloré** sur chaque carte : bleu Entreprise / orange Freelance
  - **Composant `TypeSelect`** dans form create + edit
  - **Auto-force** `maxAgencies=1` + `maxUsers=1` quand type=FREELANCE
  - **Masque** les champs "Max agences" et "Max utilisateurs" quand type=FREELANCE + bandeau orange explicatif

---

## 5. Frontend — Client console refactor

### 5.1 Nouvelle Sidebar client (pattern org/agency)

**Fichier créé :** `apps/easy-rental-web/src/consoles/client/components/Sidebar.tsx`

- Menu : Accueil, **Marketplace** (icon Store), Mes locations, Réservations, Notifications (avec badge unread)
- **Sticky** : `lg:sticky lg:top-0 lg:flex lg:w-72 flex-col h-screen`
- Item Notifications avec badge dynamique unreadCount

### 5.2 Header réduit (plus de nav horizontale)

**Fichier modifié :** `apps/easy-rental-web/src/consoles/client/components/Header.tsx`

Ne contient plus que : bouton menu mobile, logo (mobile), langue, thème, cloche notif, profil, logout. Propage `unreadCount` au parent via `onUnreadCountChange`.

### 5.3 page.tsx layout (flex row)

**Fichier modifié :** `apps/easy-rental-web/src/consoles/client/app/page.tsx`

Nouveau layout `<div class="flex">` : Sidebar à gauche + (Header + Main + Footer) à droite. `unreadCount` state partagé entre Header (qui l'update via polling) et Sidebar (qui affiche le badge).

### 5.4 i18n mis à jour

**Fichiers modifiés :** `client/locales/fr.ts` + `en.ts`
- `nav.catalog` renommé (label) → "Marketplace"
- `nav.myTrips` → "Mes locations" / "My rentals"
- Nouveau bloc `sidebar` (systemSubtitle, hub, home, marketplace, myRentals, reservations, notifications, install, logout, status)
- Bloc `trips` enrichi : planning, departure, return, duration, days, hours, mileage, seats, pickupAgency, financialSummary, remainingBalance, total, paidAmount, reference

### 5.5 MyBookingsView — cartes uniformisées + panneau restructuré + fix dates

**Fichier modifié :** `apps/easy-rental-web/src/consoles/client/views/MyBookingsView.tsx`

**Bug critique fixé** : `TripDetailPanel.formatDate()` utilisait `fr` (undefined dans scope) au lieu du `frLocale` importé → `try/catch` avalait l'erreur → dates affichaient "—". Maintenant utilise `dateLocale` passé en props (`frLocale` ou `enUS`).

**Cartes liste** : refaites en style rectangulaire type NotificationCard :
- Ligne 1 : dot + badge status + REF + date création
- Ligne 2 : Dates (Du...→...) + Paiement (montant + soldé/reste)
- Ligne 3 : bouton "Détails" + éventuel bouton "Noter"

**Panneau détail restructuré en 4 blocs a→b→c→d** :
- **(a) EN-TÊTE VÉHICULE** : image + brand/model + immat + couleur/année + référence + **bandeau grid 4-col** (statut, sièges, kilométrage, transmission) + **équipements en badges** (climatisation, bluetooth, GPS, USB, bagages, ordinateur) — tout regroupé
- **(b) FINANCIER** : solde restant + total + payé + statut paiement
- **(c) PLANNING** : Départ / Retour + badge "X Jours"
- **(d) AGENCE DE RETRAIT** : nom, adresse, phone/email
- **Extras** en bas si présents : chauffeur, client walk-in

### 5.6 ReservationDetail — même restructuration

**Fichier modifié :** `apps/easy-rental-web/src/consoles/client/views/reservation/ReservationDetail.tsx`

Mêmes 4 blocs a→b→c→d. Le bandeau du bloc (a) contient : Statut · Sièges · Kilométrage · Transmission + **équipements en badges** + VIN. Section "Équipements & Confort" dupliquée du bas a été supprimée.

---

## 6. État actuel — Ce qui fonctionne / Ce qui reste

### ✅ Ce qui fonctionne (validé par le user en cours de session)

- Kernel auth avec nouveau préfixe `/kernel-api`
- App token M2M auto-refresh (KernelAppTokenProvider)
- Upload de fichiers vers kernel (`POST /api/files`) avec vraies bytes (size correct, plus 0)
- Proxy affichage image via `GET /api/media/kernel-file/{id}`
- Création véhicule + upload photo depuis org console
- Sidebar client sticky + Marketplace + Mes locations
- Dates Départ/Retour visibles dans Mes locations
- Panneaux détail restructurés Mes locations + Réservations
- Admin plans : filtres Entreprises/Freelances, badges, select Type dans create/edit, auto-force max_agencies/max_users=1 pour Freelance, masquage conditionnel

### 🚧 Ce qui reste à faire (Freelance Étapes 2-3)

**Étape 2 — Frontend Freelance registration** (pas commencé) :
- Câbler le bouton "Freelance" de la landing → redirect `/register-freelance`
- Créer page `/register-freelance` avec formulaire simplifié (prénom/nom/email/téléphone/ville/password + sélection d'un des plans Freelance en cards)
- Adapter console org pour détecter `accountType === 'FREELANCE'` → masquer sidebar items "Postes & Rôles", "Gestion Staff", bouton "Ouvrir une agence" ; bandeau "Espace Freelance"

**Étape 3 — Vue admin Organisations** (pas commencé, hors plans qui sont faits) :
- Tab "Freelances" à côté du tab "Entreprises" dans OrganizationsView (filtre `accountType=FREELANCE`)
- KPIs par tab

### 🚧 Kernel integration — étapes manquantes (identifiées via PDF workflow API)

**Manque dans `AuthUseCaseImpl.kernelRegisterOrganization`** :
1. **Attribution du rôle OWNER** au user après sign-up (nécessite `platformAdminToken`, i.e. notre `KernelAppTokenProvider`)
   - `GET /api/administration/roles` pour trouver l'ID du rôle OWNER
   - `POST /api/administration/users/{userId}/roles` avec `{roleId, scopeType:"TENANT", scope:"TENANT"}`
   - Sans OWNER → `POST /api/organizations` renvoie 403 (pas de `organizations:write`)
2. **Auto-approbation de l'org** après création
   - L'org naît en `governanceStatus="PENDING_APPROVAL"` → tant que non approuvée, bloquée
   - OWNER a `tenant:admin` → peut approuver sa propre org via `POST /api/organizations/{id}/approve`

Sans ces 2 étapes, les orgs créées via easy-rental restent en `PENDING_APPROVAL`. À ajouter dans `kernelRegisterOrganization` (bénéficiera aussi automatiquement à `registerFreelance` qui délègue à cette méthode).

### 🚧 Autres pending mentionnés en cours de session (pas fait)

- Vérification email pour client (skippée actuellement via `easy-rental.client.skip-kernel-auth=true`)
- Client peut devenir freelancer (upgrade compte)
- Client sidebar menu (fait via refactor client ci-dessus ✅)
- Subscription plans freelance question (fait ✅)

---

## 7. Décisions techniques importantes

### 7.1 Auth kernel

- Le kernel n'expose PAS de vrai `refreshToken` classique dans son login/MFA-confirm response — `sessionToken` est un doublon de `accessToken`
- Le seul flow qui rafraîchit sans MFA : `discover-contexts` (principal+password) → `select-context` (selectionToken+contextId) → nouveau accessToken 15 min
- `/api/auth/refresh` avec le sessionToken ne marche pas ("Refresh token not found")
- Solution retenue : `KernelAppTokenProvider` boot au démarrage + refresh périodique 10 min via discover+select avec credentials admin

### 7.2 Kernel `/api/files` bugs

- **`GET /api/files/{id}/content` retourne 500 "Access Denied" pour TOUT LE MONDE**, même l'admin qui vient d'uploader — bug côté kernel
- **Workaround** : utiliser `GET /api/files/{id}` (sans `/content`) qui renvoie l'image binaire directement
- L'endpoint `/api/files/{id}/metadata` fonctionne bien pour récupérer les infos

### 7.3 Multipart streaming Spring WebFlux

- `filePart.content()` en tant que Flux<DataBuffer> se re-lit (peut être subscribed plusieurs fois — c'est un fichier temp Spring)
- `filePart.transferTo(path)` déplace le fichier temp (source disparaît après)
- Envoyer un multipart via WebClient : préférer `DataBufferUtils.join()` + `ByteArrayResource` plutôt que `builder.asyncPart("file", filePart.content(), DataBuffer.class)` pour éviter `size:0`
- Pour proxy download : `.retrieve().toEntity(byte[].class)` peut throw même sur 200 → préférer `exchangeToMono` + `bodyToMono(byte[].class).defaultIfEmpty(new byte[0])`
- Default WebClient `maxInMemorySize=256KB` → doit être augmenté pour transporter des images

### 7.4 Next.js rewrites dev mode

- Bug connu : les rewrites Next.js en dev mode ne transmettent PAS correctement les responses multipart → status 200 mais body vide
- Workaround : route handler Next.js (`src/app/api/.../route.ts`) qui fait le fetch côté serveur

### 7.5 Local vs FREELANCE storage

- Migration 21 (seed plans Freelance) **NON exécutée** — l'user préfère créer ses plans lui-même via l'interface admin
- Les 6 plans existants sont préservés, tous marqués `COMPANY` par la migration 20

---

## 8. Fichiers modifiés — liste complète

### Backend (Java)

**Config/Kernel :**
- `application.properties`, `application-local.properties`
- `kernel/config/KernelClientProperties.java`
- `kernel/config/KernelWebClientConfig.java`
- `kernel/infrastructure/KernelContextWebFilter.java`
- `kernel/application/KernelAppTokenProvider.java` (NEW)

**Adapters kernel :**
- `kernel/infrastructure/adapter/KernelWebClientAdapter.java`
- `kernel/infrastructure/adapter/KernelFileAdapter.java`

**Media :**
- `modules/media/application/MediaUseCaseImpl.java`
- `modules/media/infrastructure/adapter/in/web/MediaController.java`

**Auth / Freelance :**
- `modules/auth/dto/RegisterFreelanceRequest.java` (NEW)
- `modules/auth/domain/port/in/AuthUseCase.java`
- `modules/auth/application/AuthUseCaseImpl.java`
- `modules/auth/infrastructure/adapter/in/web/AuthController.java`

**Organization / Subscription :**
- `modules/organization/domain/OrganizationEntity.java`
- `modules/organization/dto/OrgResponseDTO.java`
- `modules/organization/mapper/OrgMapper.java`
- `modules/subscription/domain/SubscriptionPlanEntity.java`
- `modules/subscription/dto/CreatePlanRequest.java`
- `modules/subscription/application/SubscriptionUseCaseImpl.java`

**Security :**
- `config/SecurityConfig.java` (permitAll ajouté sur `/api/media/kernel-file/**`)

**Migrations Liquibase :**
- `db/changelog/changes/19-organization-account-type.xml` (NEW)
- `db/changelog/changes/20-subscription-plan-target-type.xml` (NEW)
- `db/changelog/db.changelog-master.xml` (includes)

**Scripts :**
- `scripts/kernel-validate-login.sh` (sauve aussi `.kernel-refresh-token`)

### Frontend (TypeScript/React)

**Shared services :**
- `packages/shared-services/src/api/media.mapper.ts`
- `packages/shared-services/src/api/subscription.mapper.ts`
- `packages/shared-services/src/api/extra.service.ts`

**Client console :**
- `apps/easy-rental-web/src/consoles/client/components/Sidebar.tsx` (NEW)
- `apps/easy-rental-web/src/consoles/client/components/Header.tsx`
- `apps/easy-rental-web/src/consoles/client/app/page.tsx`
- `apps/easy-rental-web/src/consoles/client/views/MyBookingsView.tsx`
- `apps/easy-rental-web/src/consoles/client/views/reservation/ReservationDetail.tsx`
- `apps/easy-rental-web/src/consoles/client/locales/fr.ts`, `en.ts`

**Organisation console :**
- `apps/easy-rental-web/src/consoles/organisation/components/LogoUpload.tsx`
- `apps/mfe-organisation/src/components/LogoUpload.tsx`

**Admin console :**
- `apps/easy-rental-web/src/consoles/admin/views/PlansView.tsx`

**Next.js config + route handler :**
- `apps/easy-rental-web/next.config.js`
- `apps/easy-rental-web/src/app/api/media-upload/route.ts` (NEW)

---

## 9. Commandes utiles

```bash
# Backend — démarrage local avec kernel activé (auto-obtient app token au boot)
cd /home/denny/Projets/projet-reseau/easy-rental-backend
./scripts/start-local-kernel.sh

# Sans kernel (skip forcé)
KERNEL_INTEGRATION_ENABLED=false ./scripts/start-local-kernel.sh

# Obtenir manuellement un token kernel via MFA (rarement nécessaire depuis KernelAppTokenProvider)
./scripts/kernel-validate-login.sh              # étape 1 login → mfaToken sauvé
./scripts/kernel-validate-login.sh 123456       # étape 2 avec code MFA reçu par email

# Frontend
cd /home/denny/Projets/projet-reseau/easy-rental-frontend
npm run dev

# Build check backend
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH ./mvnw compile -o

# Build check frontend
npx tsc --noEmit -p apps/easy-rental-web/tsconfig.json
```

---

## 10. Prochaine session — Priorités suggérées

**Court terme** (débloque des flows utilisateur importants) :
1. **Ajouter attribution OWNER + auto-approve** dans `AuthUseCaseImpl.kernelRegisterOrganization` (bénéficie à toutes les orgs incl. Freelance) — cf. section 6 "Kernel integration — étapes manquantes"
2. **Étape 2 Freelance** : bouton landing + page `/register-freelance` + adaptation console org (masquage items multi-agences si accountType=FREELANCE)

**Moyen terme** :
3. **Étape 3 admin Organisations** : tab "Freelances" à côté "Entreprises" dans OrganizationsView
4. **Client peut devenir freelancer** : upgrade compte CLIENT → ORGANIZATION+FREELANCE
5. **Email verification client** : activer vraiment (retirer `skip-kernel-auth` ou implémenter flow local)

**Long terme** :
6. Refresh token propre pour la session user (pas juste app token) — permettrait aux users de rester connectés plus longtemps
7. Password reset / change password (endpoints kernel disponibles, pas branchés)
8. Logout kernel (invalidation server-side)
