# Let's Play API

API REST sécurisée construite avec Spring Boot et MongoDB.

## Stack technique

- Java 17
- Spring Boot 3.2.5
- Spring Security + JWT
- MongoDB
- Lombok
- Bucket4j (Rate Limiting)

## Prérequis

- Java 17+
- Maven 3.9+
- MongoDB 8+

## Installation

### 1. Cloner le projet
```bash
git clone https://github.com/coulou90/Lets-Play.git
cd lets-play
```

### 2. Démarrer MongoDB
```bash
# Windows - vérifier que le service tourne
Get-Service -Name MongoDB
```

### 3. Lancer l'application
```bash
mvn clean spring-boot:run
```

L'API sera disponible sur `http://localhost:8080`

---

## Authentification

L'API utilise JWT (JSON Web Token).

### Register
```http
POST /api/auth/register
Content-Type: application/json

{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "123456"
}
```

Réponse `201 Created` :
```json
{
    "message": "Compte cree avec succes"
}
```

### Login
```http
POST /api/auth/login
Content-Type: application/json

{
    "email": "john@example.com",
    "password": "123456"
}
```

Réponse `200 OK` :
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "role": "USER",
    "name": "John Doe"
}
```

> Utilise le token dans le header `Authorization: Bearer <token>` pour les requêtes protégées.

---

## Endpoints Produits

### GET /api/products
Récupérer tous les produits — **public, aucune authentification requise**

Réponse `200 OK` :
```json
[
    {
        "id": "64abc...",
        "name": "PlayStation 5",
        "description": "Console de jeu Sony",
        "price": 499.99,
        "userId": "64xyz..."
    }
]
```

### GET /api/products/{id}
Récupérer un produit par ID — **public**

Réponse `200 OK` :
```json
{
    "id": "64abc...",
    "name": "PlayStation 5",
    "description": "Console de jeu Sony",
    "price": 499.99,
    "userId": "64xyz..."
}
```

### POST /api/products
Créer un produit — **authentifié**

```http
POST /api/products
Authorization: Bearer 
Content-Type: application/json

{
    "name": "PlayStation 5",
    "description": "Console de jeu Sony",
    "price": 499.99
}
```

Réponse `201 Created` :
```json
{
    "id": "64abc...",
    "name": "PlayStation 5",
    "description": "Console de jeu Sony",
    "price": 499.99,
    "userId": "64xyz..."
}
```

### PUT /api/products/{id}
Modifier un produit — **propriétaire ou admin**

```http
PUT /api/products/{id}
Authorization: Bearer 
Content-Type: application/json

{
    "name": "PlayStation 5 Pro",
    "description": "Console de jeu Sony - Edition Pro",
    "price": 699.99
}
```

Réponse `200 OK` :
```json
{
    "id": "64abc...",
    "name": "PlayStation 5 Pro",
    "description": "Console de jeu Sony - Edition Pro",
    "price": 699.99,
    "userId": "64xyz..."
}
```

### DELETE /api/products/{id}
Supprimer un produit — **propriétaire ou admin**

```http
DELETE /api/products/{id}
Authorization: Bearer 
```

Réponse `200 OK` :
```json
{
    "message": "Produit supprime"
}
```

---

## Endpoints Utilisateurs (Admin uniquement)

### GET /api/users
Récupérer tous les utilisateurs

```http
GET /api/users
Authorization: Bearer 
```

Réponse `200 OK` :
```json
[
    {
        "id": "64xyz...",
        "name": "John Doe",
        "email": "john@example.com",
        "role": "USER"
    }
]
```

### GET /api/users/{id}
Récupérer un utilisateur par ID

```http
GET /api/users/{id}
Authorization: Bearer 
```

### PUT /api/users/{id}
Modifier un utilisateur

```http
PUT /api/users/{id}
Authorization: Bearer 
Content-Type: application/json

{
    "name": "John Updated",
    "email": "john.updated@example.com"
}
```

### DELETE /api/users/{id}
Supprimer un utilisateur

```http
DELETE /api/users/{id}
Authorization: Bearer 
```

Réponse `200 OK` :
```json
{
    "message": "Utilisateur supprime"
}
```

---

## Codes de réponse HTTP

| Code | Description |
|------|-------------|
| 200 | Succès |
| 201 | Ressource créée |
| 400 | Données invalides |
| 401 | Non authentifié |
| 403 | Accès refusé |
| 404 | Ressource introuvable |
| 409 | Conflit (email déjà utilisé) |
| 429 | Trop de requêtes |
| 500 | Erreur interne |

---

## Sécurité

- Mots de passe hashés avec **BCrypt**
- Authentification par **JWT** (expire après 24h)
- **RBAC** — Admin et User ont des permissions différentes
- **Rate Limiting** — 20 requêtes/min par IP, 5 tentatives de login/min
- **CORS** configuré pour les origines autorisées
- Headers de sécurité HTTP activés
- Données sensibles (mot de passe) jamais exposées dans les réponses

---

## Structure du projet
src/main/java/lets_play/
├── config/
│   └── SecurityConfig.java
├── controller/
│   ├── AuthController.java
│   ├── ProductController.java
│   └── UserController.java
├── dto/
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   └── UserResponse.java
├── exception/
│   ├── ApiException.java
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
├── model/
│   ├── Product.java
│   └── User.java
├── repository/
│   ├── ProductRepository.java
│   └── UserRepository.java
├── security/
│   ├── JwtFilter.java
│   ├── JwtUtil.java
│   ├── RateLimitFilter.java
│   └── UserDetailsServiceImpl.java
└── service/
├── ProductService.java
└── UserService.java

---

## Auteur
## Souleymane Coulibaly


Projet réalisé dans le cadre d'un apprentissage du développement backend avec Spring Boot.
