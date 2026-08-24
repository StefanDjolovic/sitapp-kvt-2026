# SitApp frontend

Angular 20 klijentska aplikacija za SitApp KVT projekat.

## Pokretanje

```bash
npm install
npm start
```

Razvojni server je dostupan na `http://localhost:4200`.

## Razvojni korisnik

Pošto prijava nije deo zahteva za ocenu 6, frontend privremeno koristi korisnika sa ID-em `1`.
Vrednost je definisana samo jednom, kroz `DEVELOPMENT_USER_ID` konstantu.
Na novoj razvojnoj bazi to je prvi testni korisnik; ako baza već sadrži druge podatke, konstantu treba uskladiti sa stvarnim ID-em.

## Provere

```bash
npm test -- --watch=false --browsers=ChromeHeadless
npm run build
```
