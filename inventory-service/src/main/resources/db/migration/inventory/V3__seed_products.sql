INSERT INTO product (sku, name, description, price, currency) VALUES
 ('KB-001', 'Mechanical Keyboard',         'Hot-swappable switches, RGB backlight', 89.00,   'USD'),
 ('MS-001', 'Wireless Mouse',              'Ergonomic, 70-day battery',             39.00,   'USD'),
 ('HD-001', 'Noise-Cancelling Headphones', 'Over-ear, 30h battery',                 249.00,  'USD'),
 ('MN-001', '27" 4K Monitor',              'IPS, USB-C 90W',                        399.00,  'USD'),
 ('LT-001', 'Ultrabook Laptop',            'Over 1000 USD: the demo payment gateway declines it -> compensation', 1299.00, 'USD'),
 ('CB-001', 'USB-C Cable',                 '2m, 240W',                              12.00,   'USD'),
 ('WC-001', 'HD Webcam',                   'Low stock: order 4+ to see InventoryRejected', 69.00, 'USD'),
 ('DK-001', 'Laptop Dock',                 'Out of stock until an admin restocks it', 159.00, 'USD');

INSERT INTO stock_item (sku, on_hand, reserved) VALUES
 ('KB-001', 25, 0), ('MS-001', 50, 0), ('HD-001', 10, 0), ('MN-001', 8, 0),
 ('LT-001', 5, 0), ('CB-001', 200, 0), ('WC-001', 3, 0), ('DK-001', 0, 0);
