# ROADMAP de développement du Microservice Job Offer (PIXID)**

## Etape 1 : initialisation et infrastructure locale
- **1.1 :** Initialisation du projet Java 25, Spring Boot 4 (Spring Web, Spring Data JPA, PostgresSQL, SpringDoc OpenAPI, Flyway,...)
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