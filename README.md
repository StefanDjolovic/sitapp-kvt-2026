# SitApp KVT 2026

SitApp je studentska veb aplikacija za razmenu poruka, izrađena prema projektnoj specifikaciji za KVT 2026. Trenutni opseg prati zahteve za ocenu 6.

## Tehnologije

- backend: Java 21, Spring Boot, Spring Security, Spring Data JPA i PostgreSQL
- frontend: Angular 20 i TypeScript
- lokalna baza: PostgreSQL kroz Docker Compose

## Struktura projekta

```text
sitapp-kvt-2026/
|- backend/     Spring Boot aplikacija
|- frontend/    Angular aplikacija
|- compose.yaml lokalna PostgreSQL baza
```

## Funkcionalni opseg za ocenu 6

- pretraga korisnika po korisničkom imenu, imenu, prezimenu ili broju telefona
- otvaranje direktnog razgovora iz rezultata pretrage
- slanje i prijem tekstualnih poruka
- pregled razgovora sortiranih po poslednjoj poruci
- prikaz broja nepročitanih poruka

## Trenutno implementirano

- osam razvojnih korisnika koji se automatski dodaju pri prvom pokretanju
- pretraga korisnika po korisničkom imenu, imenu, prezimenu ili telefonu
- REST endpoint `GET /api/users/search?query=ana&currentUserId=1`
- razvojni CORS pristup za Angular aplikaciju na `http://localhost:4200`

Podrazumevani Spring profil je `dev`. U njemu autentikacija još nije potrebna, jer registracija i prijava nisu deo funkcionalnog opsega za ocenu 6. Testni profil ne učitava razvojne korisnike.

Parametar `currentUserId` je opcionalan i služi da se trenutno izabrani razvojni korisnik izostavi iz rezultata pretrage.

## Lokalno pokretanje

1. Kopirati `.env.example` u `.env` samo ako su potrebne drugačije vrednosti za PostgreSQL kontejner. Ako se podrazumevane vrednosti promene, odgovarajuće `DB_URL`, `DB_USERNAME` i `DB_PASSWORD` promenljive treba proslediti i backend procesu.
2. Pokrenuti PostgreSQL sa `docker compose up -d`.
3. U direktorijumu `backend` pokrenuti `./mvnw spring-boot:run` (Linux/macOS) ili `.\mvnw.cmd spring-boot:run` (Windows).
4. U direktorijumu `frontend` pokrenuti `npm install`, a zatim `npm start`.

Backend je dostupan na `http://localhost:8080`, a frontend na `http://localhost:4200`.

## Provere

- backend: `./mvnw test` ili `.\mvnw.cmd test`
- frontend testovi: `npm test -- --watch=false --browsers=ChromeHeadless`
- frontend produkcioni build: `npm run build`
