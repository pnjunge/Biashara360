# Customer ordering and QR codes

Customers open `https://biashara360.co.ke/shop/<storefront-slug>` without signing in.
They can browse available products and pay with M-Pesa, card, or cash on delivery.

Open **Settings → Storefront → Open shop and table QR codes**, or the QR link on
**Hospitality**. The desktop dashboard also has an **Ordering QR** button.
The QR page is `/shop/<storefront-slug>/qr`.

Choose **General online shop** or an active table, then download the PNG or print
a sign. A table QR opens `/shop/<storefront-slug>?table=<table-id>`. Keep each sign
on its corresponding table. Inactive tables cannot receive new orders.

Table orders appear as open hospitality tabs with an `ECOM` order reference.
Food-category items appear on kitchen tickets; drinks stay on the same bill.
Customers can pay online or choose **Pay your server**. An STK request does not
mark the bill paid: payment confirmation or staff cash settlement does that.
The server resolves products, prices, stock, and table ownership for each order.
Repeated checkout references reuse the original order and preparation ticket.

Stores must be active with an enabled subscription. Table ordering also requires
hospitality mode and active tables. Products must be active and in stock.
