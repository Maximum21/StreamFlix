# ADR-004: Certificate pinning for the TMDB API

Status: accepted

## Context

The app sends a TMDB bearer token with every API request. Pinning the API host's certificate
chain limits the damage from a rogue or compromised certificate authority. Pinning also carries
an availability risk: if the server moves to a different issuer, a pinned app stops working until
it is updated.

## Decision

- Pins are declared in `network_security_config.xml`, so they apply at the platform level and
  need no OkHttp code.
- Only `api.themoviedb.org` is pinned. It is served with Amazon-issued certificates, whose root
  is stable. `image.tmdb.org` (a CDN on Let's Encrypt, used by the image loader) and the Google
  and Firebase endpoints are not pinned: the first is a third-party CDN that can change provider
  without notice, and Google rotates its chains regularly.
- The pins are public-key hashes of the **Amazon Root CA 1** certificate and the current
  **Amazon RSA 2048 M04** intermediate, not of the leaf, so routine leaf renewals are safe. Two
  independent pins mean one issuer change does not immediately break the app.
- The pin set has an `expiration` date (2027-12-31). After it, the platform stops enforcing the
  pins and falls back to normal TLS validation, so an old install degrades instead of failing.
- Cleartext traffic is disabled for the whole app. Debug builds additionally trust user-installed
  certificates so traffic can be inspected with a proxy.

## How to refresh the pins

```
echo | openssl s_client -connect api.themoviedb.org:443 -servername api.themoviedb.org -showcerts
# for each certificate in the chain, extract it and compute:
openssl x509 -in cert.pem -pubkey -noout | openssl pkey -pubin -outform DER \
  | openssl dgst -sha256 -binary | base64
```

Update the pins and the expiration before the current expiration date, and release the app
before the old certificates leave the chain. Check the chain again before each release.

## Consequences

- A pinning failure shows as a network error for every TMDB request. If this happens in
  production, a release with updated pins is the fix; the expiration date bounds the exposure.
- The pins have to be maintained, which is part of the release checklist.
