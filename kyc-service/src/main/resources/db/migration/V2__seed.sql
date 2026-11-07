INSERT INTO customer (full_name, email, phone, document_type, document_number, document_holder_name, address, date_of_birth, status, risk_rating, created_at, updated_at) VALUES
('Alice Johnson', 'alice.johnson@example.com', '+1-513-555-0101', 'PASSPORT', 'A1234567', 'Alice Johnson', '123 Main St, Cincinnati, OH 45220', '1990-05-14', 'PENDING', NULL, NOW(), NOW()),
('Bob Martinez', 'bob.martinez@example.com', '+1-513-555-0102', 'DRIVERS_LICENSE', 'D12345678', 'Bob Martinez', '456 Oak Ave, Columbus, OH 43215', '1985-11-02', 'PENDING', NULL, NOW(), NOW()),
('Carol Nguyen', 'carol.nguyen@example.com', '+1-513-555-0103', 'NATIONAL_ID', '123456789', 'Carol Nguyen', '789 Pine Rd, Cleveland, OH 44101', '1992-08-23', 'PENDING', NULL, NOW(), NOW());
