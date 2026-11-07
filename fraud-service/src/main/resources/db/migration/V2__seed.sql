INSERT INTO bank_transaction (customer_id, amount, currency, merchant, category, location, channel, timestamp, status) VALUES
('CUST-1001',  52.40,  'USD', 'Kroger',              'GROCERIES',   'Cincinnati, US', 'POS',    NOW() - INTERVAL '12 days',  'APPROVED'),
('CUST-1001',  120.00, 'USD', 'Shell',               'FUEL',        'Cincinnati, US', 'POS',    NOW() - INTERVAL '10 days',  'APPROVED'),
('CUST-1001',  89.99,  'USD', 'Amazon',              'RETAIL',      'Cincinnati, US', 'ONLINE', NOW() - INTERVAL '9 days',   'APPROVED'),
('CUST-1001',  240.00, 'USD', 'Delta Airlines',      'TRAVEL',      'Cincinnati, US', 'ONLINE', NOW() - INTERVAL '7 days',   'APPROVED'),
('CUST-1001',  64.25,  'USD', 'Chipotle',            'DINING',      'Cincinnati, US', 'POS',    NOW() - INTERVAL '5 days',   'APPROVED'),
('CUST-1001',  310.50, 'USD', 'Best Buy',            'ELECTRONICS', 'Cincinnati, US', 'POS',    NOW() - INTERVAL '4 days',   'APPROVED'),
('CUST-1001',  45.00,  'USD', 'Netflix',             'SUBSCRIPTION','Cincinnati, US', 'ONLINE', NOW() - INTERVAL '3 days',   'APPROVED'),
('CUST-1001',  150.75, 'USD', 'Target',              'RETAIL',      'Cincinnati, US', 'POS',    NOW() - INTERVAL '2 days',   'APPROVED'),
('CUST-1001', 15000.00,'USD', 'CoinBase',            'CRYPTO',      'Lagos, NG',      'ONLINE', DATE_TRUNC('day', NOW()) + INTERVAL '3 hours', 'BLOCKED'),
('CUST-1001',  75.00,  'USD', 'Starbucks',           'DINING',      'Cincinnati, US', 'POS',    NOW() - INTERVAL '30 minutes', 'APPROVED'),
('CUST-1001',  82.00,  'USD', 'Uber',                'TRANSPORT',   'Cincinnati, US', 'ONLINE', NOW() - INTERVAL '20 minutes', 'APPROVED'),
('CUST-1001',  910.00, 'USD', 'BetMGM',              'GAMBLING',    'Cincinnati, US', 'ONLINE', NOW() - INTERVAL '10 minutes', 'UNDER_REVIEW');
