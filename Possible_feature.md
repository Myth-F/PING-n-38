# PING - Plateforme de Gestion de Données Sensibles chez <ins>Orano<ins/>

## 1. Authentification et Gestion des Accès

```
Ci-contre un petit contre-rendu de ce premier jet d'idée. 
Pour toute question ou éclaircissement merci de me contacter sur Discord.
Les fonctionnalitées ci-contre énoncées ne pourront surement pas être mise 
en place dans les temps. Il s'agit de faire de notre mieux !

```
### 1.1 Connexion sécurisée
- **Authentification** : Identifiant/Mot de passe + OTP(one time password)
- **Sessions sécurisées** : Tokens JWT avec expiration (30 min d'inactivité)
- **Chiffrement** : HTTPS obligatoire, hashage pour les mots de passe

### 1.2 Différents Niveaux d'accréditation
```
Niveau 1 - Visiteur : Lecture seule des documents publics
Niveau 2 - Contributeur : Lecture/Écriture sur projets assignés
Niveau 3 - Validateur : Validation des modifications + Niveau 2
Niveau 4 - Administrateur : Gestion complète + Niveau 3
```

### 1.3 Traçabilité des connexions à la platforme
- Log de chaque tentative de connexion (succès/échec)
- Géolocalisation IP et détection d'anomalies
- Alertes automatiques en cas de comportement suspect

## 2. Gestion des Projets et Fichiers

### 2.1 Structure des projets
#### Ci-contre un exemple d'une structure d'une file structure:
```
/workspace
  ├── /projet-alpha [Niveau 3+] <-(Uniquement accéssible pour un validateur)
  │   ├── /documents
  │   ├── /analyses
  │   └── /rapports
  ├── /projet-beta [Niveau 2+]
  └── /archives [Lecture seule]
```

### 2.2 Métadonnées possibles
- **Classification** : Public / Confidentiel / Secret (Pourrait changer la visibilité)
- **Propriétaire** : Responsable du document
- **Date de révision** : Prochaine révision obligatoire
- **Tags** : Mots-clés pour recherche rapide

### 2.3 Opérations sur les fichiers
- Upload avec scan antivirus automatique
- Prévisualisation sécurisée sur le site.
- Téléchargement avec traçabilité

## 3. Versioning GIT

### 3.1 Workflow Git adapté
```
main (production)
  └── develop
      ├── feature/analyse-235
      ├── feature/rapport-q2
      └── hotfix/correction-calcul
```

### 3.2 Règles de commit
- Format : `[TYPE](scope): Description courte (max 50 char)`
- Types : FEAT, FIX, DOCS, SECURITY, REVIEW, ect...
- Signature GPG obligatoire pour Niveau 3+
- Revue obligatoire avant merge (2 validateurs minimum)

### 3.3 Hooks Git personnalisés
- Pre-commit : Vérification des données sensibles
- Pre-push : Validation des métadonnées
- Post-merge : Notification aux parties prenantes

## 4. Système de Logs

### 4.1 Types de logs
#### Exemple :
```json
{
  "timestamp": "Random-timestamp",
  "user_id": "john.doe@orano.com",
  "action": "FILE_ACCESS",
  "resource": "/projet-alpha/analyse-235.pdf",
  "ip_address": "192.168.1.XXX",
  "result": "SUCCESS",
  "metadata": {
    "file_classification": "CONFIDENTIEL",
    "access_level": 3
  }
}
```

### 4.2 Conservation et archivage
- Logs temps réel : 30 jours
- Archives compressées : 5 ans
- Chiffrement pour stockage

### 4.3 Dashboard de monitoring
- Rapports hebdomadaires pour la direction
- Export pour audit externe

## 5. Interfaces Utilisateur

### 5.1 Dashboard principal
- Vue d'ensemble des projets accessibles
- Notifications et alertes
- Activité récente de l'équipe
- Raccourcis vers documents fréquents

### 5.2 Gestionnaire de fichiers
- Interface type explorateur Windows
- Drag & drop sécurisé
- Prévisualisation intégrée
- Recherche avancée avec filtres

### 5.3 Interface Git
- Visualisation de l'historique
- Comparaison de versions
- Gestion des branches simplifiée
- Résolution de conflits assistée
