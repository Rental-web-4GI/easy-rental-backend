# API — Release 3 (Fidélité · Chat · Sécurité admin)

> Périmètre R3 livré : **fidélité plateforme**, **messagerie 2-parties**, **sécurité
> admin** (audit IP + suspension d'organisation en cascade + dashboard étendu).
> Le module **litige/contestation de caution est reporté en R4** (voir plan
> `docs/superpowers/plans/2026-07-26-release-3-chat-loyalty-admin-disputes.md`,
> tâches 11-12).

Conventions communes :

- JSON **snake_case** en entrée comme en sortie (Jackson global). Le frontend
  envoie du snake_case et re-camelize les réponses (`deepCamelize`).
- Auth : `Authorization: Bearer <jwt>`. Les headers machine kernel
  (`X-Client-Id` / `X-Api-Key`) ne transitent **jamais** par le navigateur.
- Base locale : `http://localhost:8081`.

---

## 1. Fidélité — `/api/loyalty`

Barème (Global Constraints) : `points = floor(rental_portion / 1000)` gagnés à la
clôture d'une location `COMPLETED`. `1 pt = 10 FCFA` à l'usage, remise plafonnée à
**50 % du montant de base**. Paliers annuels : `BRONZE` / `ARGENT ≥ 500` /
`OR ≥ 2000` / `PLATINE ≥ 5000`.

| Méthode | Route | Rôle | Description |
|---|---|---|---|
| GET | `/api/loyalty/balance/{clientId}` | CLIENT | Solde courant + palier + points annuels. |
| GET | `/api/loyalty/history/{clientId}` | CLIENT | Journal des gains/dépenses (ledger). |

**Gain / dépense** : pas d'endpoint dédié — ça passe par le cycle location.
- Gain : automatique au `settleReturn` d'une location `COMPLETED`.
- Dépense : `POST /api/rentals/initiate` avec `redeem_points` (voir §rental).

`GET /api/loyalty/balance/{clientId}` →
```json
{ "client_id": "…", "balance": 340, "annual_points": 1250, "tier": "ARGENT" }
```

Remise à l'initiation d'une location :
```jsonc
// POST /api/rentals/initiate
{ "vehicle_id": "…", "start_date": "…", "end_date": "…", "redeem_points": 200 }
// → réponse
{ "rental_id": "…", "amount_due": 15000, "loyalty_discount": 2000 }
```

---

## 2. Messagerie — `/api/conversations`

Conversations 2-parties. `ParticipantType` ∈ {CLIENT, AGENCY, ADMIN} ;
`ConversationType` déduit de la paire (CLIENT_AGENCY / CLIENT_ADMIN /
AGENCY_ADMIN). Le participant courant est résolu depuis le rôle JWT.

| Méthode | Route | Description |
|---|---|---|
| POST | `/api/conversations/open` | Ouvre (ou récupère) la conversation avec la cible. Body : `{ "target_type": "AGENCY", "target_id": "<uuid>" }`. |
| POST | `/api/conversations/{id}/messages` | Envoie un message. Body : `{ "body": "texte" }`. Émet une notification `NEW_MESSAGE` au destinataire. |
| GET | `/api/conversations/{id}/messages?page=0&size=50` | Messages paginés de la conversation. |
| GET | `/api/conversations/mine` | Conversations du participant courant. |
| PUT | `/api/conversations/{id}/read` | Marque la conversation comme lue. |
| GET | `/api/conversations/admin/all?page=0&size=50` | ADMIN — supervision de toutes les conversations. |

---

## 3. Sécurité admin

### 3.1 Audit IP — `/api/admin/audit-events`
Chaque événement d'audit capte désormais l'IP (`X-Forwarded-For` → adresse
distante) et le `User-Agent` via `AuditContextFilter` (WebFilter → contexte
Reactor). Les audits de login sont chaînés (plus de `.subscribe()` détaché) pour
que l'IP se propage.

```
GET /api/admin/audit-events?action=LOGIN_FAILED&size=10   (ADMIN)
```

### 3.2 Suspension d'organisation — `/api/admin/organizations`
Cascade : agences masquées du catalogue + login des membres bloqué
(`ORG_SUSPENDED`).

| Méthode | Route | Body | Effet |
|---|---|---|---|
| POST | `/api/admin/organizations/{id}/suspend` | `{ "reason": "…" }` (optionnel) | `status=SUSPENDED`, `suspended_at`, `suspension_reason`. |
| POST | `/api/admin/organizations/{id}/reactivate` | — | `status=ACTIVE`, champs de suspension remis à null. |

### 3.3 Dashboard plateforme — `/api/admin/stats/platform`
Champs R3 ajoutés :
- `organizations.suspended` — nb d'orgs `SUSPENDED` (`countByStatus`).
- `revenue.total_outstanding_debt` — somme des dettes clients impayées.

---

## Rappel — ce qui n'est PAS dans R3
Le litige/contestation de caution (endpoints `/api/disputes`, écran client de
contestation, arbitrage admin) est **reporté en R4**. Rien dans R3 n'en dépend.
