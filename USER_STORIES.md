# User Stories - Gestion des accès par niveau

## Niveau 1 – Visiteur

| ID & Titre | Rôle | Besoin | But | Critères d'acceptation |
|------------|------|--------|-----|------------------------|
| **US-V01** : Consultation basique | Visiteur | Consulter les documents publics du projet | Prendre connaissance des informations non sensibles | • Accès uniquement aux fichiers Public<br>• Téléchargement interdit<br>• Message « Accès restreint » sur les fichiers confidentiels<br>• Journalisation des consultations |
| **US-V02** : Navigation limitée | Visiteur | Naviguer dans l'arborescence des projets publics | Comprendre l'organisation des données | • Cadenas sur les dossiers confidentiels<br>• Accès aux logs système interdit<br>• Menu Administration masqué |

## Niveau 2 – Contributeur

| ID & Titre | Rôle | Besoin | But | Critères d'acceptation |
|------------|------|--------|-----|------------------------|
| **US-C01** : Upload de fichiers | Contributeur | Uploader des fichiers dans ses projets | Partager son travail avec l'équipe | • Scan antivirus automatique à l'upload<br>• Métadonnées obligatoires (classification, tags)<br>• Notification email aux validateurs |
| **US-C02** : Gestion des versions | Contributeur | Créer des commits pour ses modifications | Assurer la traçabilité du travail | • Format imposé [TYPE](scope): description (vérifié par RegEx)<br>• Push direct sur main interdit<br>• Branches feature/ autorisées<br>• Historique Git visible |
| **US-C03** : Collaboration équipe | Contributeur | Visualiser l'activité de son équipe projet | Coordonner son travail | • Fil d'activité restreint au projet<br>• Notifications pour modifications importantes<br>• Commentaires sur les fichiers possibles |

## Niveau 3 – Validateur

| ID & Titre | Rôle | Besoin | But | Critères d'acceptation |
|------------|------|--------|-----|------------------------|
| **US-VA01** : Validation des contenus | Validateur | Approuver ou rejeter les modifications proposées | Garantir la qualité du code | • Tableau de bord des contributions en attente<br>• Commentaires ligne par ligne<br>• 2 validateurs minimum requis<br>• Signature après validation |
| **US-VA02** : Gestion des accès | Validateur | Gérer les permissions sur ses projets | Contrôler l'accès aux données sensibles | • Interface de gestion des membres<br>• Attribution des niveaux 1–2 uniquement<br>• Révocation immédiate<br>• Log de toutes les modifications d'accès |
| **US-VA03** : Monitoring sécurité | Validateur | Surveiller les tentatives d'accès non autorisées | Détecter des comportements suspects | • Alertes temps réel<br>• Dashboard des logs de sécurité<br>• Export des logs pour analyse |

## Niveau 4 – Administrateur

| ID & Titre | Rôle | Besoin | But | Critères d'acceptation |
|------------|------|--------|-----|------------------------|
| **US-AD01** : Administration globale | Administrateur | Gérer l'ensemble de la plateforme | Garantir son bon fonctionnement | • Accès à tous les projets et fichiers<br>• Gestion des utilisateurs tous niveaux<br>• Configuration des règles de sécurité<br>• Maintenance et backups |
| **US-AD02** : Audit et conformité | Administrateur | Générer des rapports d'audit | Respecter les obligations réglementaires | • Export des logs sur 5 ans<br>• Rapport mensuel automatique<br>• Traçabilité complète des actions<br>• Conformité RGPD et normes nucléaires |
| **US-AD03** : Gestion de crise | Administrateur | Réagir rapidement en cas d'incident | Protéger les données sensibles | • Mode urgence (blocage global)<br>• Révocation massive d'accès<br>• Restauration depuis backup<br>• Communication d'urgence à tous |

## Transverse – Sécurité

| ID & Titre | Rôle | Besoin | But | Critères d'acceptation |
|------------|------|--------|-----|------------------------|
| **US-SEC01** : Double authentification | Utilisateur niveau 2+ | Authentification renforcée | Protéger le compte | • OTP par SMS ou application<br>• Session expire après 30 min d'inactivité<br>• Déconnexion hors réseau Orano |

