-- Development seed. Safe to run repeatedly: inserts are keyed on reference.
insert into account (id, reference, currency, balance, version) values
  ('11111111-1111-1111-1111-111111111111', 'ACC-OPERATIONS', 'EUR', 1000000.0000, 0),
  ('22222222-2222-2222-2222-222222222222', 'ACC-CLIENT-A',   'EUR',     5000.0000, 0),
  ('33333333-3333-3333-3333-333333333333', 'ACC-CLIENT-B',   'EUR',     5000.0000, 0)
on conflict (reference) do nothing;
