# Hotel & Accommodation

The web application exposes `/hotel`; authenticated APIs are under `/v1/hotel`.
Accommodation has its own enable flag and `HOTEL` menu, independent of restaurant
operations. Existing HOTEL/LODGE tenants and new hotel registrations enable it
by default. Other tenants can enable it in Settings → Hotel & Accommodation.

## Operation

1. Add room types with capacity and an all-inclusive nightly rate in KES.
2. Add numbered rooms and optionally block maintenance dates.
3. Reserve a room with guest details, arrival/departure dates and booking source.
   Departure is exclusive. Rooms cannot have overlapping active reservations.
4. Check in arrivals after housekeeping marks their room CLEAN or INSPECTED.
5. Use the guest folio for additional charges, credits, deposits and payments.
   Cash is recorded when actually received; card and M-Pesa use existing payment
   providers and receive folio credit only after a successful recorded settlement.
   M-Pesa amounts must be whole KES; Cash and Card can settle cents.
6. Settle the folio before checkout. Checkout marks the room DIRTY. Housekeeping
   can mark work IN_PROGRESS, then CLEAN/INSPECTED, and record maintenance notes.

Reservations retain their original nightly rate, including after room moves and
extensions. Adjust the departure date for early checkout before settling the
folio; every stay has a minimum of one night. Room moves require a clean room
and mark the old one dirty. Pending payment requests must be resolved before
changing dates or cancelling a stay. Uninitiated payment requests can be cancelled.
Initiated transactions must be resolved with the configured payment provider.

Cancellation/no-show removes accommodation charges. Existing deposits become
credits; additional charges remain. Discounts never make a refund exceed money
received. Cash refund entries record money actually returned and are included in
payment reports and deducted from the financial revenue summary. These entries
**do not initiate card or M-Pesa refunds**. Guest folios can be printed or exported
as CSV; they are not tax invoices. Rates are all-inclusive, without tax breakdown.

## Permissions

Hotel Manager, Hotel Front Desk and Hotel Housekeeping role presets are available
under Users & Access. Assign the `HOTEL` menu and `hotel.view`, then add specific
rights for reservations, front desk, billing, refunds, housekeeping, reports,
room configuration and calendar exchange. Front desk billing roles include the
PAYMENTS/ORDERS menus needed by the existing payment screens. Guest folios require
`hotel.billing`; refunds and credits additionally require `hotel.refunds`.

## Reports and booking channels

Occupancy, ADR and RevPAR reflect contracted room rates and booked room nights.
Room-night capacity excludes maintenance blocks and rooms currently out of
service. Future bookings are forecasts, not collections. Reports separately show
net collected funds, outstanding folios and refundable guest credit. Date ranges
use Nairobi time and an exclusive end date; historical capacity uses the current
room inventory, not an inventory snapshot.

Calendar import/export supports all-day `.ics` events. A channel import atomically
replaces that channel's blocks for one room, preserving other channels and manual
blocks. Conflicts with active reservations reject the entire import. Exported
calendars contain availability only, never guest information. Recurrent/timed
calendars must be exported as expanded all-day events. Exchange is manual;
automatic OTA rate/reservation sync needs a chosen provider and its API access.

## Verification

`HotelServiceTest` covers overlap prevention (including concurrent bookings),
tenant isolation, dirty-room check-in, deposits and idempotent payment requests,
settlement before checkout, cancellations/refunds, electronic payment verification,
rate snapshots, room moves, calendar replacement and conflicts, occupancy metrics,
and API permission enforcement.

Tests default to isolated H2 databases. To exercise the production PostgreSQL
constraints, first migrate an **isolated test database** and set
`HOTEL_TEST_POSTGRES_URL` to its JDBC URL. The current integration fixture uses
`postgres` / `hotel-test-only` and truncates that test database's tenant tables
before each test. Never point this setting at a live database.
