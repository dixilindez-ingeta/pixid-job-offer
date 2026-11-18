Ce fichier documente les décisions architecturales structurales prises lors du développement du service PIXID Job Offer.

# ADR-01 : Adoption de l'architecture en Oignon (Onion Architecture)

- ### Statut : Accepté
- ### Contexte :
  Le microservice (MS) gère un cycle de vie métier complexe (machine à états configurable par entreprise, règles de validation conditionnelles) tout en devant s'intégrer dans un écosystème plus vaste (back-office, job board public, futurs évènements asynchrones).
- ### Décision :
  L'architecture en oignon est pleinement adapté aux projets en DDD. On structure le code avec quatre couches concentriques, de l'intérieur vers l'extérieur (les couches internes ne dépendent pas des couches externes) :

  - `domain`
  - `application`
  - `infrastructure`
  - `présentation`

<div align="center">

`présentation (API REST: Controllers, DTOs REST...)`

↓

`infrastructure (Adapters : PostgreSQL, Spring Data, Flyway, JPA Entities,...)`

↓

`application (Use case : UseCase Handlers, Command/Queries, Ports,...)`

↓

`domain (Core business : Aggregate, State Machine, Value Objects,...)`
</div>

- ### Conséquences :
  - #### Positives :
    - Le métier (application+domain) ne dépend pas de la technologie (spring)
    - Testabilité unitaire instantanée et exhaustive du moteur d'états sans démarrage du moteur Spring
    - Evolution facilitée (ex : Ajout d'un adaptateur Kafka pour publier des évènements sans toucher au domaine,...)
  - #### Négatives :
    - Verbosité élevée avec par exemple des Mappers entre JPA/DTOs REST.

# ADR-02 : Séparation stricte entre les entités du domaine et les entités JPA

- ### Statut :** Accepté
- ### Contexte :
Spring Data JPA impose des contraintes techniques aux entités(mutabilité, annotations de mapping relationnel, constructeur sans arguments,...) qui rentrent en conflit avec les principes d'immuabilité des aggrégats DDD.
- ### Décision :
Créer des classes distinctes pour la persistance(dans infrastructure/persistence) et pour le métier (dans domain/model)

- ### Conséquences :
  - #### Positives :
  Le schéma de BDD peut être optimisé indépendemment de la structure objet métier. Le modèle de domaine garantit ses invariants à la construction et à chaque invariant.
  - #### Négatives :
  Classes de mapping nécessaires



# ADR-03 : Résolution dynamique des sauts d'états dans un service de domaine

- ### Statut : Accepté

- ### Contexte : 
Une offre d'emploi traverse jusqu'à 6 états, dont 3 optionnels(`TO_FINALIZE`, `TO_APPROVE`, `TO_POST`) selon la configuration de l'entreprise. De plus, l'API doit exposer dynamiquement les transitions disponibles (`availableTransitions`).

- ### Décision : 
  - Centraliser l'algorithme de calcul des transitions et de résolution de l'état cible dans une classe dédiée du
    domaine (`JobOfferStateMachine`), pilotée par **actions** (`TransitionAction`) : `allowedActions(status)` donne les
    actions autorisées par statut, `targetStatus(status, action, config)` l'état cible (ou
    `InvalidStateTransitionException`).
  - Cet algo saute automatiquement les étapes désactivées (`partialSaveEnabled` ↔ `TO_FINALIZE`, `approvalRequired` ↔
    `TO_APPROVE`, `manualPostingRequired` ↔ `TO_POST`) jusqu'à atteindre la prochaine étape requise ou l'état PUBLISHED.
  - L'agrégat `JobOffer` expose `apply(action, config, clock)` et `availableActions(config)` pour permettre aux
    consommateurs (API, IHM) de connaître les actions légitimes à un instant $t$ (règles de complétude incluses, voir
    ADR-06).

- ### Conséquences :
  - #### Positives : 
  Élimination complète de la duplication de conditions(`if/else`) entre les contrôleurs REST, les cas d'utilisation et les validations; ajout aisé d'un nouvel état optionnel dans le futur; Les règles de transition sont hautement testables unitairement sans nécessiter de contexte Spring ni de base de données.
  - #### Négatives : 
  Nécessite de toujours charger la CompanyConfig (ou ses valeurs par défaut) lors d'une lecture unitaire ou d'une transition d'offre.




# ADR-04 : Modélisation des Transitions via une Ressource dédiée (POST /transitions)

- ### Statut : Accepté

- ### Contexte : 
Les changements d'état d'une offre(`submit`, `approve`, `reject`, `post`, `close`, `expire`) déclenchent des règles de validation spécifiques et des effets de bord (ex: valorisation de `publishedAt`), contrairement à une simple mise à jour de champs (`PUT`).

- ### Décision : 
Exposer un endpoint dédié orienté commande `POST /api/v1/job-offers/{id}/transitions` recevant l'action demandée (ex: `{"action": "APPROVE"}`), plutôt que d'autoriser la modification directe du champ status via `PUT` ou `PATCH`.

- ### Conséquences :
  - #### Positives : 
  Sémantique REST claire et explicite ; séparation nette entre la modification du contenu de l'offre (autorisée uniquement en `DRAFT/TO_FINALIZE`) et l'avancement dans le workflow; gestion fine des erreurs de transition(`400 Bad Request` / `409 Conflict`).
  - #### Négatives : 
  Un endpoint supplémentaire à documenter dans OpenAPI.

# ADR-05 : Schéma de persistance (UUID natifs, pas de FK vers la configuration, verrou optimiste)

- ### Statut : Accepté

- ### Contexte :

`companyId` référence une entreprise gérée par un service Company **externe**. La table `company_configs` ne contient
que les entreprises ayant explicitement personnalisé leur configuration. Par ailleurs, une offre peut être enregistrée
incomplète (`partialSaveEnabled`) et peut être modifiée en concurrence par plusieurs recruteurs.

- ### Décision :
  - Identifiants stockés en type natif PostgreSQL `UUID`.
  - **Aucune clé étrangère** `job_offers.company_id → company_configs` : une entreprise sans ligne de configuration
    utilise `CompanyConfig.defaults()` (tous les paramètres à `false`), renvoyé par l'adaptateur.
  - Paramètres `approval_required`, `partial_save_enabled`, `manual_posting_required` en
    `BOOLEAN NOT NULL DEFAULT FALSE`.
  - `title` et `location_type` nullables : la complétude est une règle **métier** vérifiée par le domaine lors des
    transitions, pas une contrainte SQL.
  - Colonne `version` + `@Version` JPA (verrouillage optimiste) ; un conflit sera traduit en `409 Conflict` par l'API.
  - Index `(company_id, status, created_at DESC)` et `(status, created_at DESC)` pour les listes filtrées/triées, index
    sur `job_offer_id` des tables enfants ; `hibernate.default_batch_fetch_size` pour éviter le N+1 sur salaires/bonus.

- ### Conséquences :
  - #### Positives :
  Aucun couplage d'intégrité avec un service externe ; création d'offre possible sans configuration préalable ; requêtes
  de liste servies par index ; perte de mises à jour concurrentes détectée.
  - #### Négatives :
  L'existence de l'entreprise n'est pas garantie par la base (à valider côté Company si nécessaire) ; les invariants de
  complétude reposent entièrement sur le domaine.

# ADR-06 : Règles métier des transitions (complétude, REJECT, clôture, horloge)

- ### Statut : Accepté

- ### Contexte :

La spécification décrit le cycle de vie mais laisse plusieurs points ouverts : que devient une offre rejetée ? Depuis
quels états peut-on clore une offre ? À quel moment une offre enregistrée partiellement doit-elle être complète ? Par
ailleurs, les dates (`createdAt`, `updatedAt`, `publishedAt`) doivent être testables de façon déterministe.

- ### Décision :
  - **Actions par statut** : `DRAFT` → `SUBMIT` ; `TO_FINALIZE` → `FINALIZE` ; `TO_APPROVE` → `APPROVE`, `REJECT` ;
    `TO_POST` → `POST` ; `PUBLISHED` → `CLOSE`, `EXPIRE` ; `CLOSED` → aucune.
  - **`REJECT` → `DRAFT`** : l'offre rejetée redevient modifiable par le recruteur, qui peut la corriger puis la
    resoumettre (le cycle d'approbation repart du début).
  - **`CLOSE` / `EXPIRE` uniquement depuis `PUBLISHED`** : une offre non publiée n'a pas à être « close » (elle reste un
    brouillon ou est rejetée).
  - **Complétude** (titre, location, compensation avec ≥ 1 entrée) exigée dès que l'offre dépasse `TO_FINALIZE` :
    - `SUBMIT` avec `partialSaveEnabled` → `TO_FINALIZE` accepte une offre incomplète (elle sera complétée en
      `TO_FINALIZE`) ;
    - `SUBMIT` sans `partialSaveEnabled`, et `FINALIZE`, exigent une offre complète (`DomainValidationException`
      sinon) ;
    - à la création, des données partielles ne sont acceptées que si `partialSaveEnabled`.
  - `availableActions(config)` exclut les actions dont la règle de complétude n'est pas satisfaite.
  - `publishedAt` est renseigné à l'entrée en `PUBLISHED`.
  - Le temps est fourni par une `java.time.Clock` passée en paramètre (`createDraft`, `update`, `apply`) plutôt
    qu'appelé via `Instant.now()` ; `JobOffer.rehydrate(...)` reconstruit l'agrégat depuis la persistance sans
    validation métier ni appel à l'horloge, et porte la `version` (verrou optimiste, ADR-05).

- ### Conséquences :
  - #### Positives :
  Règles explicites et testées unitairement ; l'API peut n'afficher que les actions réellement exécutables ; tests de
  dates déterministes ; la persistance ne déclenche jamais de règle métier.
  - #### Négatives :
  Une offre rejetée perd son avancement (elle repasse par toutes les étapes) ; la `Clock` doit être transmise par la
  couche application à chaque commande.
