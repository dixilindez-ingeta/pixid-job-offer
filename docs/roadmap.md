# ROADMAP de développement du Microservice Job Offer (PIXID)**

## Etape 1 : initialisation et infrastructure locale

- **1.1 :** Initialisation du projet Java 25, Spring Boot 4 (Spring Web MVC, Spring Data JPA, PostgresSQL, SpringDoc
  OpenAPI 3.x, Flyway,...)
- **1.2 :** Créer le fichier `compose.yml` avec un conteneur PostegresSQL et un volume persistant
- **1.3 :** Configurer `application.yml`
- **1.4 :** Créer les packages de base (`domain`, `application`, `intrastructure`, `prensentation`)
- **1.5 :** Figer les règles d'architecture avec `ArchUnit`
- **1.5 :** Vérifier le démarrage à vide de l'application

## Etape 2 : Coeur du  Domaine
- **2.1 :** Créer les Value Objects typés : `JobOfferId` (UUID), `CompanyId` (UUID), `Money`(montant >= 0 + devise ISO 4217).
- **2.2 :** Créer le Value Object `Address` avec validation des règles métier (max 3 lignes de 100 caractères, `city` obligatoire, code pays ISO 3166-1 alpha-2).
- **2.3 :** Créer le Value Object `Compensation` regroupant `SalaryEntry` et `BonusEntry` (invariant : au moins une entrée de rémunération requise lors de la soumission complète).
- **2.4 :** Créer l'entité `CompanyConfig` avec ses trois paramètres (`approvalRequired`, `partialSaveEnabled`, `manualPostingRequired`, tous `false` par défaut).
- **2.5 :** Créer l'énumération `JobOfferStatus` (`DRAFT`, `TO_FINALIZE`, `TO_APPROVE`, `TO_POST`, `PUBLISHED`, `CLOSED`) et les événements/actions de transition (`SUBMIT`, `FINALIZE`, `APPROVE`, `REJECT`, `POST`, `CLOSE`, `EXPIRE`).
- **2.6 :** Créer les exceptions métier du domaine (`InvalidTransitionException`, `DomainValidationException`, `ResourceNotFoundException`).

## Etape 3 : Machine à états et logiques métier pure
- **3.1 :** Implémenter l'agrégat `JobOffer` contenant les champs requis, les timestamps(`createdAt`, `updatedAt`, `publishedAt`) et la logique de mise à jour (autorisée uniquement en `DRAFT` ou `TO_FINALIZE`).
- **3.2 :** Implémenter le moteur de résolution des transitions (`JobOfferStateMachine`) qui calcule l'état cible en sautant dynamiquement les états optionnels désactivés dans `CompanyConfig` (qu'on a finalement décidé de traiter comme value object avec les champs `requireFinalization`, `requireApproval`, `requirePostReview`).
- **3.3 :** Implémenter la méthode `getAvailableTransitions(CompanyConfig)` sur `JobOffer` pour calculer les actions possibles à un instant $t$.
- **3.4 :** Écrire les tests unitaires sur la machine à états : tester le chemin nominal, chaque flag activé individuellement, les combinaisons de flags, et les transitions interdites.

# Etape 4 : Ports, adaptateurs et persistance

- **4.1 :** Définition des scripts de migration Flyway (`V1__init_schema.sql` : UUID natifs, pas de FK vers
  `company_configs`, paramètres `DEFAULT FALSE`, colonne `version`, index de liste — voir ADR-05).
- **4.2 :** Création des entités JPA techniques dans la couche infrastructure.
- **4.3 :** Implémentation du Port du domaine `CompanyConfigProvider` (`findByCompanyId` renvoie
  `CompanyConfig.defaults()` si absente, `save`).
- **4.4 :** Implémentation des mappers entre entités JPA et objets du domaine (`JobOffer`, `CompanyConfig`).
- **4.5 :** Tests d'intégration de la persistance (Testcontainers + `@ServiceConnection` ; `contextLoads` réactivé).

# Etape 5 : Exposition API et UseCase (Couche application)
- **5.1 :** Implémentation des cas d'utilisation (Use Cases / Command Handlers).
- **5.2 :** Exposition des contrôleurs REST Spring MVC.
- **5.3 :** Documentation OpenAPI / Swagger (springdoc).

# Etape 6 : Gardiens architecturaux et aualité
- **6.1 :** Règles ArchUnit strictes pour valider l'architecture en oignon (isolation totale du domaine, interdiction des dépendances Spring/JPA dans domain).