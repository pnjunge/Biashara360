# Hospitality workflows

The restaurant module uses explicit `hospitality.*` permissions for viewing,
orders, kitchen/bar tickets, billing, reservations, floor controls, menus/recipes,
stock, shifts, purchasing, financial reports, and deciding approvals. The Users
& Access matrix exposes these rights. Existing restaurant role presets are
migrated to their corresponding rights; custom roles must be assigned explicitly.

## Approvals

Create the tab at full price, then open Hospitality Operations → Approvals.
Select a specific unpaid tab, ingredient, or unreceived purchase order and supply
the reason and exact discount/stock quantity. Supported actions are discounts,
complimentary tabs, cancellation, stock events, and receiving purchase orders.
An authorized user other than the requester decides. Approving validates the
current target state and applies the action in the same transaction, once.
Failed actions leave the request pending; rejection changes no operational data.
A complimentary tab can close for zero without inventing a payment transaction.
Legacy requests without complete action details must be rejected and recreated.
Approval permission can be assigned to managers or supervisors without granting
an administrator login. Direct stock/purchasing actions still require their own
management permissions; requesters can ask an approver to apply them instead.

## Operations

Recipe quantities use the ingredient's stock unit. Demand is aggregated over the
entire order and stock is protected from concurrent overdrafts. Opening a bottle
changes no quantity; consumption, wastage and receipt events update stock.
Only food categories support Kitchen preparation; drinks and retail items create no preparation ticket. Routing is saved on each order item so later menu edits do not change existing tickets.
Cancelling a tab closes it, cancels its tickets and releases its table when no
other tabs remain. Prepared ingredient consumption is not blindly restored;
operators must account for unused/prepared ingredients through stock controls.
M-Pesa requires a whole KES total; cash and card accept cents.

Reservations can progress from BOOKED to SEATED then COMPLETED, or to CANCELLED/
NO_SHOW. Merged tables remain visible for unmerging. New table layouts are spaced.
Menus include recipe editing; Purchasing includes ingredient suppliers, purchase
orders and receiving goods independently of product purchase invoices.

Financial reports include paid, non-cancelled DINE_IN/TAKEAWAY/DELIVERY orders.
They exclude hotel/service orders and include modifier prices, discounts and
complimentary lines. Shift payment counts likewise exclude unrelated modules.
Food and beverage costs use the buying-cost snapshot saved on each sold order item; later ingredient price or recipe edits do not change those costs.

`HospitalityWorkflowTest` exercises actual workflows, concurrency, approvals,
rollback, stock, cancellation, report totals and API permission enforcement.
It supports the same isolated PostgreSQL fixture as `HotelServiceTest`; never
point `HOTEL_TEST_POSTGRES_URL` at a live database.

## Staff sound alerts

The web header's **Enable sound** control starts browser audio after a staff
click. Distinct tones identify new kitchen tickets, tickets becoming READY,
and new online orders from the portal queue. Alerts continue across staff pages;
**Mute alerts** stops sound while visual notices remain available. Hospitality
polling requires `hospitality.view` and hospitality mode to be enabled.
The first successful snapshot is silent. Seen orders and ready transitions are
remembered for the session so repeat polling and claiming do not replay sounds.
Polling occurs every five seconds after each response; keep the staff app open.
After refreshing or signing in again, enable sound again. No sound asset downloads
or customer notifications are involved.

## Bulk portions and purchases

Recipe products derive available portions from their limiting ingredient and consume ingredients once, without separately deducting product stock. Maintain ingredient stock in its base unit and define recipes in that unit. Use separate products for different portion sizes. Purchasing converts KG/G and L/ML automatically; configure the contents of bottles, packs or cases in Stock. Purchase orders save their conversion so later configuration changes do not alter receipts.

## Personal shifts and bill handover

Each waiter or cashier starts their own personal shift at login or from Open Tabs. No manager approval is required. If there is no open trading day, the first personal shift opens one with zero cash float; simultaneous staff starts share the same trading day. Managers can open the day with a cash float before staff start. Ending a personal shift leaves the trading day and other users’ shifts open. Select unpaid bills and an incoming staff member who is on duty, then request handover. Responsibility transfers only after the recipient accepts. Original serving attribution and earlier payment collectors remain unchanged. Settle or hand over all your unpaid bills before ending your shift.

End shift saves a tally of completed bills you served, held or settled during that shift. Payment totals include only payments collected by that user, grouped by method. Different staff may have worked on the same bill; do not add their bill values together as business revenue. Closed tallies remain fixed and support CSV download. Managers can review staff tallies in Operations → Shifts. The business day closing and reconciliation remain separate.

## Cashier stock restrictions

Cashiers can browse the product catalog for checkout and progress kitchen orders, but cannot create/edit products, adjust inventory, manage suppliers or record purchases. Inventory and purchase pages require their menus and permissions even when opened by a direct URL. Product mutations enforce `products.create`, `products.update` or `inventory.adjust`; purchases require `purchases.view` and `purchases.create`, and supplier management requires `inventory.suppliers`. Migration 47 removes stock-management rights from existing Cashier role/group presets and preserves purchasing for existing stock-management roles. Assign extra management rights through an appropriate separate role.

## Ingredient purchases, payments and profit

Receipts use the weighted average cost of remaining and newly received ingredient stock. Receiving an ingredient purchase atomically creates one linked `STOCK_PURCHASE` expenditure entry and receives converted stock. Choose receipt without payment or receipt with a real supplier payment. Supplier payments have their own ledger, amount, method, reference, staff attribution and retry key; they never create customer revenue. Partial payments reduce the supplier balance. Reused request keys return the existing payment, mismatched keys/references and overpayments fail, and receipt/payment failures roll back the entire transaction. Payments require `hospitality.purchase_payments` in addition to purchasing access; stock receivers can receive without paying.

Expenses displays purchase costs, payment status and balances, and protects source-linked entries from deletion. Stock purchases are excluded from operating expenses in profit summaries and dashboard profit because sold-item costs already enter COGS. Cash outflow uses recorded supplier payments rather than adding COGS again. Product stock purchases are also excluded from operating profit; their existing invoice payment status controls purchase cash outflow. The stock-purchase figures distinguish received purchase costs from payments made.

Mark a cash supplier payment as paid from the business drawer only when it was taken from that drawer during an open trading day. Day closing deducts those payments automatically; enter other cash expenses separately. Payment recording does not send funds. Existing received purchase orders receive linked expenditure entries in migration 48, with earlier payments marked UNRECORDED rather than invented. Confirm old supplier balances before recording payments.
