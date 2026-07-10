

# 🚗 Easy Rental API - Plateforme SaaS de Location de Véhicules

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen.svg)
![WebFlux](https://img.shields.io/badge/Spring%20WebFlux-Reactive-blue.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-R2DBC-blue.svg)
![Liquibase](https://img.shields.io/badge/Liquibase-Migrations-red.svg)

**Easy Rental API** est un backend robuste, réactif et multi-locataires (Multi-tenant) conçu pour gérer des flottes de véhicules. Il s'agit d'une plateforme **SaaS (B2B2C)** permettant à des entreprises de location (Organisations) de gérer leurs agences, leurs véhicules et leur personnel, tout en offrant aux clients finaux la possibilité de rechercher, réserver et payer des locations.

---

## 📑 Table des matières
1. [Fonctionnalités Principales](#-fonctionnalités-principales)
2. [Stack Technique](#-stack-technique)
3. [Architecture du Projet](#-architecture-du-projet)
4. [Prérequis](#-prérequis)
5. [Installation et Démarrage](#-installation-et-démarrage)
6. [Base de données et Jeu d'essai (Seeding)](#-base-de-données-et-jeu-dessai-seeding)
7. [Sécurité et RBAC](#-sécurité-et-rbac)
8. [Documentation API (Swagger)](#-documentation-api-swagger)
9. [Workflow d'utilisation (Comment tester)](#-workflow-dutilisation-comment-tester)

---

## 🌟 Fonctionnalités Principales

Le système est divisé en plusieurs modules gérant des aspects spécifiques du métier :

### 🏢 Espace Organisation (B2B)
* **Multi-Agences :** Création et gestion de plusieurs agences physiques avec géolocalisation.
* **Gestion de Flotte :** Ajout de véhicules (avec caractéristiques techniques, assurance, photos) et de chauffeurs.
* **Ressources Humaines (RBAC) :** Création de postes personnalisés, assignation de permissions granulaires et gestion du staff.
* **Abonnements (SaaS) :** Gestion des quotas (max véhicules, max agences) basés sur des plans d'abonnement (FREE, PRO, ENTERPRISE).
* **Statistiques & Finances :** Tableaux de bord complets (Revenus, taux d'occupation, évolution des locations).

### 📱 Espace Client (B2C)
* **Recherche Avancée :** Recherche d'agences et de véhicules disponibles par ville, dates, et catégories.
* **Réservation & Paiement :** Processus de location complet (Devis -> Acompte 60% -> Paiement total -> Récupération -> Retour).
* **Avis & Évaluations :** Possibilité de noter les véhicules et les chauffeurs.
* **Historique :** Suivi des transactions financières et des locations passées.

### ⚙️ Fonctionnalités Transverses
* **Notifications :** Système d'alertes en temps réel (Réservation confirmée, paiement reçu, etc.).
* **Gestion des Médias :** Upload sécurisé de fichiers (Photos de profil, CNI, Permis, Images véhicules).
* **Audit Trail :** Traçabilité complète des actions sensibles réalisées sur la plateforme.

---

## 🛠 Stack Technique

Ce projet utilise une approche **100% Réactive (Non-bloquante)** pour garantir de hautes performances.

* **Langage :** Java 21
* **Framework :** Spring Boot 3.4.x
* **Programmation Réactive :** Spring WebFlux (Project Reactor : `Mono`, `Flux`)
* **Base de données :** PostgreSQL 17
* **Accès aux données :** Spring Data R2DBC (Driver réactif)
* **Migrations BDD :** Liquibase (exécuté via JDBC au démarrage)
* **Sécurité :** Spring Security + JWT (JSON Web Tokens)
* **Documentation :** SpringDoc OpenAPI (Swagger UI)
* **Utilitaires :** Lombok, MapStruct (via constructeurs/records)

---

## 🏗 Architecture du Projet

Le code est organisé de manière modulaire (proche de l'architecture hexagonale / Domain-Driven Design) :

```text
src/main/java/com/project/apirental/
├── config/              # Configurations globales (Security, Swagger, Cors, Seeder)
├── modules/             # Modules métiers isolés
│   ├── agency/          # Gestion des agences
│   ├── auth/            # Authentification, Inscription, JWT
│   ├── driver/          # Gestion des chauffeurs
│   ├── media/           # Upload et gestion des fichiers
│   ├── notification/    # Système d'alertes
│   ├── organization/    # Gestion des entreprises locataires
│   ├── permission/      # Catalogue des droits
│   ├── poste/           # Rôles personnalisés pour le staff
│   ├── pricing/         # Tarification dynamique (Heure/Jour)
│   ├── rental/          # Moteur de réservation et paiements
│   ├── review/          # Système d'avis
│   ├── schedule/        # Gestion des indisponibilités (Planning)
│   ├── staff/           # Gestion des employés
│   ├── statistics/      # Tableaux de bord et rapports
│   ├── subscription/    # Plans SaaS et facturation
│   └── vehicle/         # Gestion de la flotte et catégories
└── shared/              # Code partagé (Exceptions, Enums, DTOs génériques, Sécurité)
```
*Chaque module contient généralement : `api` (Controllers), `domain` (Entities), `dto`, `repository`, et `services`.*

---

## 💻 Prérequis

* **Java 21** installé.
* **Maven 3.9+** (ou utiliser le wrapper `./mvnw` inclus).
* **PostgreSQL 17** en cours d'exécution.
* **Docker** (Optionnel, pour le déploiement).

---

## 🚀 Installation et Démarrage

### 1. Configuration de la Base de données
Créez une base de données PostgreSQL locale. Par défaut, l'application s'attend à :
* URL : `jdbc:postgresql://localhost:5432/rentaldb` (pour Liquibase)
* URL R2DBC : `r2dbc:postgresql://localhost:5432/rentaldb`
* User : `postgres`
* Password : `password`

*(Vous pouvez modifier ces valeurs dans `src/main/resources/application.yml` ou via des variables d'environnement).*

### 2. Lancement en local (Mode Développement)

Le projet inclut un profil `dev` qui lance automatiquement un **Seeder** pour remplir la base de données avec des données de test.

```bash
# Cloner le projet
git clone <url-du-repo>
cd apirental

# Compiler le projet
./mvnw clean install -DskipTests

# Lancer l'application avec le profil dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. Lancement via Docker

Un `Dockerfile` multi-stage est inclus pour conteneuriser l'application.

```bash
# Construire l'image Docker
docker build -t apirental-backend .

# Lancer le conteneur (en liant le port 8080 et un dossier pour les uploads)
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_R2DBC_URL=r2dbc:postgresql://<ip-db>:5432/rentaldb \
  -v /chemin/local/uploads:/app/uploads \
  --name apirental \
  apirental-backend
```

---

## 🗄 Base de données et Jeu d'essai (Seeding)

### Migrations (Liquibase)
Au démarrage, **Liquibase** lit les fichiers dans `src/main/resources/db/changelog/` et crée automatiquement toutes les tables, contraintes et index nécessaires.

### Data Seeder (Profil `dev`)
Si vous lancez l'application avec le profil `dev`, la classe `DataSeeder.java` va s'exécuter et générer :
1. **2 Organisations** : *Prestige Auto Cameroun* (Plan Enterprise) et *Logistics Express* (Plan Pro).
2. **Des Agences** : Douala, Yaoundé.
3. **Du Personnel** : Managers, Agents commerciaux.
4. **Une Flotte** : Véhicules (Toyota, Mercedes) avec photos et caractéristiques.
5. **Des Chauffeurs**.

**Identifiants de test générés :**
* **PDG Prestige Auto :** `contact@prestige-auto.cm` / Mdp: `password123`
* **PDG Logistics :** `info@logistics-express.cm` / Mdp: `password123`

---

### 4. Intégration kernel-core (P0)

Pour déléguer auth, organisations, agences et RBAC au kernel production :

```bash
# Depuis la racine du dépôt easy-rental-backend
cp kernel-core.credentials.env.example kernel-core.credentials.env
# Éditer kernel-core.credentials.env avec les secrets DevOps

set -a && source kernel-core.credentials.env && set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

| Variable | Description |
|---|---|
| `KERNEL_INTEGRATION_ENABLED` | `true` = délégation kernel, `false` = mode local (défaut) |
| `KERNEL_BASE_URL` | URL kernel (défaut `https://kernel-core.yowyob.com`) |
| `KERNEL_CLIENT_ID` | ClientApplication (`X-Client-Id`) |
| `KERNEL_API_KEY` | Secret machine (`X-Api-Key`) |
| `KERNEL_TENANT_ID` | UUID tenant (`X-Tenant-Id`) |
| `KERNEL_JWKS_URI` | JWKS pour validation JWT RS256 |

Scripts de validation : `scripts/kernel-validate-login.sh`, `scripts/kernel-bootstrap-org.sh`.

Démarrage local avec Kernel :

```bash
./scripts/start-local-kernel.sh
```

Voir aussi : `REFERENCE-INTEGRATION-KERNEL-EASY-RENTAL.md` (racine du dépôt).

#### Attribuer le rôle OWNER (obligatoire pour créer une org)

Le **sign-up** Kernel **n’attribue pas** le rôle `OWNER`. Sans `organizations:write`,
`POST /api/organizations` échoue (`Access Denied`, souvent remonté en `HTTP_500` dans l’UI).
L’absence de champ MFA sur l’UI est **normale** si le compte owner n’a pas MFA activée
(`mfaEnabled=false`) — ce n’est pas la cause du refus de création.

**Procédure concrète (platform-admin) :**

```bash
cd easy-rental-backend
set -a && source kernel-core.credentials.env && set +a

# 1) Login admin + MFA (2 étapes)
./scripts/kernel-validate-login.sh
./scripts/kernel-validate-login.sh <CODE_EMAIL>
export ACCESS_TOKEN=$(cat .kernel-access-token)

# 2) Récupérer l’id Kernel de l’utilisateur propriétaire
#    (après un login owner, ou via GET /api/users/me côté owner)
#    Exemple : TARGET_USER_ID=77c367d8-28a7-4dc4-943c-cb091fc47120
export TARGET_USER_ID=<uuid-user-kernel>

# 3) Trouver l’id du rôle OWNER
curl -sS "$KERNEL_BASE_URL/api/administration/roles" \
  -H "X-Client-Id: $KERNEL_CLIENT_ID" \
  -H "X-Api-Key: $KERNEL_API_KEY" \
  -H "X-Tenant-Id: $KERNEL_TENANT_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  | jq -r '.data[] | select(.code=="OWNER") | .id'
# → OWNER_ROLE_ID (ex. be780ab7-224d-443f-8bb5-1a4379eeaff9)

export OWNER_ROLE_ID=<uuid-role-OWNER>

# 4) Attribuer OWNER (scope TENANT)
curl -sS -X POST \
  "$KERNEL_BASE_URL/api/administration/users/$TARGET_USER_ID/roles" \
  -H "X-Client-Id: $KERNEL_CLIENT_ID" \
  -H "X-Api-Key: $KERNEL_API_KEY" \
  -H "X-Tenant-Id: $KERNEL_TENANT_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"roleId":"'"$OWNER_ROLE_ID"'","scopeType":"TENANT","scope":"TENANT"}' | jq .
# Attendu : HTTP 201, success true

# 5) Le propriétaire doit SE RECONNECTER sur l’UI (nouveau JWT avec permissions)
# 6) Remplir l’onboarding → Activer le dashboard → org en PENDING_APPROVAL
# 7) Approuver + services :
export ORGANIZATION_ID=<uuid-org-kernel>
./scripts/kernel-bootstrap-org.sh
```

Référence : `RAPPORT-organisation-workflows-api.pdf` §4.2 (devenir OWNER) + §5 (créer / approve).

---

## 🔐 Sécurité et RBAC

L'API est sécurisée par **JWT (JSON Web Token)**.
Il existe 4 grands rôles système :
* `ROLE_ADMIN` : Administrateur de la plateforme (gère les plans d'abonnement).
* `ROLE_ORGANIZATION` : Propriétaire d'une entreprise de location.
* `ROLE_STAFF` : Employé d'une agence.
* `ROLE_CLIENT` : Utilisateur final.

**Contrôle d'accès granulaire (RBAC) :**
Pour le staff, l'accès aux ressources est vérifié dynamiquement via le bean `@rbac` (`AccessControlService`).
Exemple : Un employé ne peut modifier un véhicule que si son `Poste` possède la permission `vehicle:update` pour cette organisation spécifique.

---

## 📚 Documentation API (Swagger)

L'API est entièrement documentée via OpenAPI 3. Une fois l'application démarrée, accédez à l'interface Swagger UI :

👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

La documentation est divisée en 3 groupes (en haut à droite de Swagger) :
1. **1-Public-Auth** : Inscription, Connexion, Upload de fichiers.
2. **2-Espace-Client** : Recherche de véhicules, Agences, Réservations clients.
3. **3-Espace-Gestion** : Back-office pour les organisations (Flotte, Staff, Stats).

**Pour tester les routes protégées dans Swagger :**
1. Utilisez `/auth/login` pour obtenir un `token`.
2. Cliquez sur le bouton **"Authorize"** (Cadenas vert).
3. Collez votre token (sans le mot "Bearer").

---

## 🔄 Workflow d'utilisation (Comment tester)

Voici le parcours classique pour comprendre le fonctionnement de l'API :

### Côté Organisation (Loueur)
1. **Inscription :** `POST /auth/register/organizationOwner` (Crée l'utilisateur et l'organisation avec le plan FREE).
2. **Connexion :** `POST /auth/login` -> Récupérer le Token.
3. **Créer une Agence :** `POST /api/agencies/org/{orgId}`.
4. **Ajouter un Véhicule :** `POST /api/vehicles/org/{orgId}` (Nécessite l'ID de l'agence et d'une catégorie).
5. **Définir le Prix :** `PATCH /api/vehicles/{id}/status-pricing` (Fixer le prix par jour/heure).

### Côté Client (Locataire)
1. **Inscription Client :** `POST /auth/register/client`.
2. **Connexion :** `POST /auth/login` -> Récupérer le Token.
3. **Rechercher un véhicule :** `GET /api/vehicles/search?city=Douala` (Route publique).
4. **Initier une location :** `POST /api/rentals/init` (Génère un devis et vérifie la disponibilité).
5. **Payer l'acompte :** `POST /api/rentals/{id}/pay` (Payer 60% pour passer le statut à `RESERVED`).
6. **Payer le solde :** `POST /api/rentals/{id}/pay` (Payer les 40% restants pour passer à `PAID`).

### Cycle de vie de la location (Par l'Agence)
1. **Départ :** `PUT /api/rentals/{id}/start` (Le client prend la voiture -> `ONGOING`).
2. **Signalement Retour :** `PUT /api/rentals/{id}/end-signal` (Le client ramène la voiture).
3. **Validation Retour :** `PUT /api/rentals/{id}/validate-return` (L'agence valide l'état -> `COMPLETED` et met le véhicule en `MAINTENANCE` pour 24h).


