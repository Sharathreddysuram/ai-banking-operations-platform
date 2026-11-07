INSERT INTO loan_application (customer_id, requested_amount, term_months, purpose, monthly_income, monthly_debt_payments, annual_income, credit_score, employment_status, status, created_at) VALUES
('CUST-1001', 15000, 36, 'Home renovation', 8000, 800, 96000, 780, 'FULL_TIME', 'PENDING', NOW()),
('CUST-1002', 25000, 36, 'Debt consolidation', 4500, 1300, 54000, 660, 'SELF_EMPLOYED', 'PENDING', NOW()),
('CUST-1003', 30000, 24, 'Business startup', 3000, 1500, 36000, 540, 'UNEMPLOYED', 'PENDING', NOW());
