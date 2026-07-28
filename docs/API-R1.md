# Release 1 — API additions

Documente les endpoints ajoutés / modifiés par la Release 1 (freelance V2 + fondations admin + audit).

## Freelance

### `GET /api/org/auth/me`
Réponse enrichie : `defaultAgencyId: UUID | null` — non-null lorsque `organization.accountType == "FREELANCE"` (agence auto-créée au signup). Utilisé par la console freelance pour brancher les vues agence sur la bonne agence.

### `PUT /api/org/upgrade-to-company`
Bascule un compte freelance vers un compte organisation. Idempotent : renvoie l'org telle quelle si `accountType` est déjà `COMPANY`. Sinon met à jour `accountType` à `COMPANY` et débloque la création d'agences supplémentaires côté client.
- Auth : `hasRole('ORGANIZATION')`
- Body : vide
- Response : `OrgResponseDTO`

## Marketplace client

### `GET /api/agencies/*` (toutes les méthodes exposées client)
`AgencyResponseDTO` gagne un champ : `organizationAccountType: "COMPANY" | "FREELANCE" | null`. Concerne : `getAllAgencies`, `getAgency(id)`, `searchAgencies(keyword, city)`. Utilisé côté client pour afficher un badge « Particulier » sur les agences freelance.

## Admin — Statistiques plateforme

### `GET /api/admin/stats/platform`
Auth : `hasRole('ADMIN')`. Réponse `PlatformStatsDTO` :

```json
{
  "users": { "total": 0, "clients": 0, "orgOwners": 0, "freelances": 0, "staff": 0 },
  "organizations": { "total": 0, "companies": 0, "freelances": 0, "suspended": 0 },
  "agencies": { "total": 0, "averagePerCompany": 0.0 },
  "vehicles": { "total": 0, "published": 0 },
  "rentals": { "total": 0, "ongoing": 0, "completed": 0, "monthlyCompleted": 0 },
  "revenue": { "subscriptionsMonthlyMRR": 0.0, "subscriptionsActiveCount": 0 }
}
```

Notes d'implémentation :
- `organizations.suspended` = orgs avec `governance_status = 'REJECTED'` (pas de statut SUSPENDED dédié en V1).
- `vehicles.published` = véhicules avec `statut = 'AVAILABLE'`.
- `revenue.subscriptionsMonthlyMRR` = `SUM(subscription_plans.price)` pour les subscriptions actives.

## Admin — Audit log

### `GET /api/admin/audit-events`
Auth : `hasRole('ADMIN')`. Query params (tous optionnels) :

| Param | Type | Description |
|-------|------|-------------|
| `userId` | UUID | Filtre par user |
| `action` | String | Filtre par action (ex : `LOGIN_FAILED`) |
| `from` | ISO Instant | Date de début (`created_at >= from`) |
| `to` | ISO Instant | Date de fin (`created_at <= to`) |
| `page` | int | Défaut 0 |
| `size` | int | Défaut 50 |

Réponse : `AuditEventResponseDTO[]` triés `created_at DESC`.

**Actions instrumentées en R1** :
- `LOGIN_SUCCESS`, `LOGIN_FAILED`
- `SIGNUP_CLIENT_SUCCESS`, `SIGNUP_CLIENT_FAILED`
- `SIGNUP_ORG_SUCCESS`, `SIGNUP_ORG_FAILED`
- `SIGNUP_FREELANCE_SUCCESS`, `SIGNUP_FREELANCE_FAILED`
- `UPGRADE_TO_COMPANY`

**Concerns R1 (à traiter en R2 si besoin)** :
- IP et User-Agent ne sont pas capturés (nullable) — plumbing `ServerWebExchange` reporté.
- Les signups avec `EMAIL_NOT_VERIFIED` (flow control) génèrent aussi un événement `*_FAILED` en plus du succès attendu — comportement acceptable en R1.

## Migrations DB

Les 3 changelogs Liquibase ajoutés :
- `24-audit-events.xml` — table `audit_events`
- `25-loyalty-ledger.xml` — table `loyalty_ledger` (créée pour R3)
- `26-ratings.xml` — table `ratings` (créée pour R2)
