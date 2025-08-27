# navikt-mock-oauth2-server

Dette er en fork av NAV IT sin [mock-oauth2-server](https://github.com/navikt/mock-oauth2-server), tilpasset for å kunne brukes som en Mock IdP for Digipost IdP. Det vil si at den brukes til å ukritisk autentisere en bruker med claims og credentials den får oppgitt, _og skal derfor aldri brukes i prod_. 

Videre i denne README'en vil "mock-oauth2-server" og "mock IdP" brukes om hverandre, 

## Bygge og deploye til ACR

Har du gjort noen endringer så kan du kjøre `dp mock-idp-docker-build` for å bygge et lokalt snapshot.

Vil du pushe til ACR, så kan du kjøre `dp mock-idp-docker-build push`. Står du på master branch får du ikke lov til å deploye til ACR med mindre du står på en tagget commit (`git tag ${TAG}`).
Bilder som bygges og deployes fra en (git-)tagget commit på master vil også pushes med tag "latest".

Står du på en annen branch, vil bildet pushes med taggen `${NAVN_PÅ_BRANCH}-SNAPSHOT`.

Du kan alltids også pushe med din egen tag: `dp mock-idp-docker-build push ${NAVN_PÅ_TAGGEN_DIN}`
