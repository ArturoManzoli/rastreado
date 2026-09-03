# Tracking data

## Why Cainiao

An AliExpress parcel bound for Brazil is carried by Cainiao, Alibaba's logistics arm, and handed to
a Brazilian carrier (usually Correios) on arrival. Cainiao's public tracking feed relays the local
carrier's events verbatim, so a single lookup covers the whole journey, including the leg that
matters most to the buyer.

`https://global.cainiao.com/global/detail.json?mailNos=<code>&lang=pt-BR&language=pt-BR`

The endpoint needs no key and no account. It is what the AliExpress web tracking page calls, so it
is public in practice, but it is unversioned and undocumented: it can change without notice. That
is why the parser is written defensively (unknown JSON keys ignored, every field optional) and why
`TrackingProvider` is an interface rather than a single hardcoded call.

## Why not Correios

Correios shut its open tracking API and replaced it with a contract-only one. Access is granted
against a commercial contract, and it authorises the **sender** of a parcel, not the recipient. A
buyer receiving a parcel from a Chinese seller can never be the authorised party, so no amount of
paperwork would give this app access. Third-party gateways that resell Correios data exist, but they
are paid, need a server-side key, and would mean routing every user's parcel codes through a
stranger.

`LinkTrack`, which older projects use, has been discontinued.

## Response shape

The interesting parts of a response, with everything the app does not read left out:

```json
{
  "success": true,
  "module": [{
    "mailNo": "LP00837062153412",
    "realMailNo": "Último número de rastreamento:\tNN368369905BR",
    "originCountry": "China",
    "destCountry": "Brasil",
    "statusDesc": "Encaminhado",
    "processInfo": { "progressRate": 0.45 },
    "globalCombinedLogisticsTraceDTO": {
      "currentStatusCpInfo": { "cpName": "Correios(BR)" }
    },
    "detailList": [{
      "time": 1788360510000,
      "standerdDesc": "[SAO JOSE] Objeto em trânsito - por favor aguarde",
      "actionCode": "GTMS_SC_DEPART",
      "group": { "nodeDesc": "Em transporte" }
    }]
  }]
}
```

Three details drive the parser:

- **An unknown code answers with `success: true` and an empty `detailList`.** There is no error to
  detect, so the mapper treats an empty list as "no tracking yet", which is also the honest answer
  for a code the seller has only just registered.
- **`realMailNo` carries a localised label and the code, tab separated.** The Brazilian code is
  extracted from it by pattern, not by splitting on the label, since the label changes with `lang`.
- **`standerdDesc` (Cainiao's own spelling) carries a `[CITY]` prefix on Brazilian legs.** The
  prefix becomes the event's location and the remainder its description.

A captured response for a real parcel mid-journey is committed at
`app/src/test/resources/cainiao_lp_last_mile.json`, and the mapper is tested against it.

## Status normalisation

Cainiao's `actionCode` is a stable machine code, so `StatusResolver` maps it first and only falls
back to reading the event text when the code is missing or unrecognised. The fallback matters
because Brazilian carriers inject messages Cainiao passes through without classifying.

The canonical statuses are in `ParcelStatus`, ordered along the journey:

| Status | Meaning | Example `actionCode` |
| --- | --- | --- |
| `PENDING` | Order registered, still with the seller | `GWMS_ACCEPT` |
| `WAREHOUSE` | In a Chinese consolidation warehouse | `CW_INBOUND` |
| `SORTING` | At an origin sorting centre | `SC_INBOUND_SUCCESS` |
| `EXPORT_CUSTOMS` | Chinese export clearance | `CC_EX_START` |
| `IN_TRANSIT` | International line haul | `LH_DEPART` |
| `ARRIVED_DESTINATION` | Landed in Brazil | `LH_ARRIVE` |
| `IMPORT_CUSTOMS` | Brazilian customs, where parcels sit longest | `CC_IM_SUCCESS` |
| `LAST_MILE` | With the Brazilian carrier | `GTMS_ACCEPT` |
| `OUT_FOR_DELIVERY` | With the courier today, triggers the alarm | `GTMS_DO_DEPART` |
| `AWAITING_PICKUP` | Waiting at an agency or locker | `GTMS_WAIT_SELF_PICK` |
| `FAILED_ATTEMPT` | Delivery attempted and not completed | `GTMS_STA_SIGN_FAILURE` |
| `EXCEPTION` | Returned, lost or damaged | `EXCEPTION` |
| `DELIVERED` | Signed for | `GTMS_SIGNED` |

Text probes are matched against a lowercased, accent-stripped copy of the event, most specific
first, because Brazilian event text overlaps heavily: "Tentativa de entrega não efetuada" has to
resolve to `FAILED_ATTEMPT` and never to `OUT_FOR_DELIVERY` on the word "entrega".

### Adding a code

When a parcel shows an event Rastreado reads as "no information", the fix is a line in
`StatusResolver.ACTION_CODES`. Please open an issue with the `actionCode` and the event text as the
app displayed them, or a pull request adding the mapping with a case in `StatusResolverTest`.

## Adding another provider

`TrackingProvider` is the whole contract: one suspend function from a code to a
`ProviderResult`, which is a snapshot, a "not found yet", or a typed failure. A new provider needs
an implementation, a mapper to `TrackingSnapshot`, and to be handed to `AppContainer`. Nothing above
that layer knows Cainiao exists.

Failures are typed (`NETWORK`, `SERVER`, `MALFORMED`, `UNSUPPORTED_CODE`) rather than thrown, so the
repository can tell a parcel with no tracking yet from a lookup that did not go through, and only
the latter is worth retrying or reporting to the user.
