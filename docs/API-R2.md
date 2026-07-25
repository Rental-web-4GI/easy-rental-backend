# API Release 2 — Caution / Inspection / Tracking / Ratings

Base path : `/api`. Auth : `Authorization: Bearer <token>`.

## Modèle pricing (source de vérité)

```
rentalAmount     = base + commission                         # location facturable (CA agence)
cautionAmount    = base × (agency.depositPercentage / 100)   # caution escrow (hors CA), fallback 10%
totalDue         = rentalAmount + cautionAmount
requestedUpfront = totalDue × 0.60                           # acompte (caution INCLUSE)
```

Allocation de chaque paiement : `rental_portion = amount × (rentalAmount/totalDue)`, `caution_portion = amount − rental_portion`.

Seuils de statut :

| Statut | Condition |
|--------|-----------|
| `PENDING` | `totalPaid < 0.60 × totalDue` |
| `RESERVED` | `totalPaid ≥ 0.60 × totalDue` |
| `PAID` | `totalPaid ≥ totalDue` (**check-in autorisé uniquement ici**) |

CA agence = `SUM(COALESCE(rental_portion, amount))` — la caution reste dans `agency.caution_escrow_balance`.

## Cycle location

| Étape | Méthode | Endpoint | Body | Rôles |
|-------|---------|----------|------|-------|
| Check-in | POST | `/rentals/{id}/check-in` | `CheckInRequest` | ADMIN, ORGANIZATION, STAFF |
| Signal fin | POST | `/rentals/{id}/signal-end` | — | ADMIN, CLIENT, ORGANIZATION, STAFF |
| Check-out | POST | `/rentals/{id}/check-out` | `CheckOutRequest` | ADMIN, ORGANIZATION, STAFF |
| Règlement | PUT | `/rentals/{id}/settle-return` | `CheckoutSettlementRequest` | ADMIN, ORGANIZATION, STAFF |

Les endpoints legacy `PUT /rentals/{id}/start`, `/end-signal`, `/validate-return` restent en place (rétro-compat). `startRental` est `@Deprecated`.

### CheckInRequest / CheckOutRequest
```json
{
  "startOdometer": 52340,               // (endOdometer pour check-out)
  "inspection": {
    "odometer": 52340,
    "fuelLevel": 6,                     // 0..8
    "notes": "RAS",
    "photoUrls": ["url1","url2","url3","url4"],   // min 4 obligatoire
    "items": [ { "itemCode": "TIRES", "status": "OK", "note": null } ]  // null → checklist défaut (12 items OK)
  }
}
```
Transitions : check-in exige `PAID` → `ONGOING` ; signal-end exige `ONGOING` → `UNDER_REVIEW` ; check-out exige `UNDER_REVIEW` (reste UNDER_REVIEW, calcule `trackedKm` GPS ou `endOdometer−startOdometer`).

### CheckoutSettlementRequest
```json
{ "cautionDeduction": 5000, "retentionReason": "rayure portière" }
```
Règles : `0 ≤ cautionDeduction ≤ cautionHeld` ; `retentionReason` obligatoire si `cautionDeduction > 0`. Effets : crée les paiements `CAUTION_REFUND` (= cautionHeld − deduction) et `CAUTION_RETENTION` (si > 0), décrémente l'escrow agence de `cautionHeld`, met à jour `vehicle.kilometrage`, statut → `COMPLETED`. Notifie le client (in-app + email).

## Inspections

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/inspections/rentals/{rentalId}` | Créer une inspection (min 4 photos) |
| GET | `/inspections/{id}` | Détail inspection + items |
| GET | `/inspections/rentals/{rentalId}/all` | Toutes les inspections d'une location |
| GET | `/inspections/rentals/{rentalId}/comparison` | Comparaison CHECK_IN vs CHECK_OUT |

Enums : `InspectionType {CHECK_IN, CHECK_OUT}`, `ItemStatus {OK, DAMAGED, BROKEN, MISSING, NOT_APPLICABLE}`.
Checklist par défaut (12) : TIRES, SPARE_TIRE, TRIANGLE_JACK, HEADLIGHTS, TURN_SIGNALS, BUMPERS, DOORS, WINDOWS_MIRRORS, INTERIOR, AC, RADIO, DOCUMENTS.

`comparison` renvoie `{ newDamages[], preExisting[], newMissing[], fuelDelta, kmTraveled }`.

## Tracking

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/rentals/{id}/positions` | `{ latitude, longitude, source? }` — push position |
| GET | `/rentals/{id}/tracking` | `{ positions[], trackedKm, source }` (source = GPS si ≥2 points, sinon ODOMETER) |

`source` : `CLIENT_PWA`, `AGENCY`, `MANUAL`. `trackedKm` = somme Haversine des segments.

## Ratings

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/ratings` | Soumettre une note (location `COMPLETED` requise, 1 note / pair) |
| GET | `/ratings/agencies/{agencyId}?page=&size=` | Avis reçus par l'agence |
| GET | `/ratings/agencies/{agencyId}/stats` | `{ average, count, distribution }` |

Body POST :
```json
{ "rentalId": "...", "raterType": "CLIENT", "raterId": "...",
  "targetType": "AGENCY", "targetId": "...", "stars": 5, "comment": "..." }
```
Erreurs : `RENTAL_NOT_COMPLETED`, `ALREADY_RATED`, `STARS_OUT_OF_RANGE`.

`AgencyResponseDTO` (marketplace) expose désormais `averageRating` + `ratingsCount`.
