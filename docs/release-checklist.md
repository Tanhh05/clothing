# Release Checklist (Staging -> Production)

## A. Pre-Deploy
- [ ] CI green (backend, storefront, admin where applicable).
- [ ] No leaked secret in diff.
- [ ] Flyway migration reviewed and reversible.
- [ ] Payment config set for target env (MoMo/VNPay).
- [ ] CORS + callback URLs verified.
- [ ] R2 bucket and CDN URL verified.

## B. Staging Sign-off
- [ ] Auth/register/login/logout.
- [ ] Product listing/category/search.
- [ ] Cart + checkout + voucher.
- [ ] Payment redirect/callback.
- [ ] Order list/detail/cancel/reorder/review.
- [ ] Admin CRUD product/order/voucher.
- [ ] Smoke test script pass.

## C. Production Deploy
- [ ] Maintenance notice disabled/enabled as planned.
- [ ] Deploy API first, then storefront/admin.
- [ ] Verify health endpoints.
- [ ] Run smoke tests against production domain.

## D. Post-Deploy (72h)
- [ ] Monitor 5xx, latency, payment callback errors.
- [ ] Verify first successful payment and order lifecycle.
- [ ] Confirm no broken images/uploads.
- [ ] Close release with known-issues log.

