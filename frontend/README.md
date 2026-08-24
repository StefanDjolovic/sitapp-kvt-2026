# SitApp frontend

Angular 20 klijentska aplikacija za SitApp KVT projekat.

## Pokretanje

```bash
npm install
npm start
```

Razvojni server je dostupan na `http://localhost:4200`.

## Razvojni korisnik

Pošto prijava nije deo zahteva za ocenu 6, frontend privremeno koristi Anu Petrović sa ID-em `1` na čistoj razvojnoj bazi.
Vrednost je definisana samo jednom, kroz `DEVELOPMENT_USER_ID` konstantu.
Na novoj razvojnoj bazi to je prvi testni korisnik; ako baza već sadrži druge podatke, konstantu treba uskladiti sa stvarnim ID-em.

## Implementirano

- pretraga korisnika i otvaranje direktnog razgovora
- početna lista razgovora, sortirana po poslednjoj aktivnosti i automatski osvežavana
- prikaz i otvaranje direktnih i postojećih grupnih razgovora
- prikaz poslednje poruke i broja nepročitanih poruka
- učitavanje i slanje tekstualnih poruka
- automatsko osvežavanje otvorenog razgovora na svakih pet sekundi
- označavanje prikazanih poruka kao pročitanih

## Provere

```bash
npm test -- --watch=false --browsers=ChromeHeadless
npm run build
```
