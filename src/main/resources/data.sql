-- Initial Enterprise Seed Data

INSERT INTO departments (name, code, budget) VALUES 
('Engineering & AI Systems', 'ENG-AI', 850000.00),
('Information Security & Risk', 'SEC-RSK', 420000.00),
('Finance & Operations', 'FIN-OPS', 310000.00),
('Strategic People Management', 'HR-TAL', 190000.00);

INSERT INTO employees (first_name, last_name, email, role, salary, department_id, is_active, hired_date) VALUES 
('Frank', 'Vargas', 'frank5vargashuaman@gmail.com', 'Lead AI Systems Architect', 125000.00, 1, TRUE, '2023-01-15'),
('Elena', 'Rostova', 'elena.rostova@enterprise.internal', 'Senior Backend Engineer', 98000.00, 1, TRUE, '2023-03-20'),
('Mateo', 'Salazar', 'mateo.salazar@enterprise.internal', 'DevSecOps Specialist', 105000.00, 2, TRUE, '2022-11-01'),
('Sofia', 'Chen', 'sofia.chen@enterprise.internal', 'Financial Risk Controller', 89000.00, 3, TRUE, '2024-02-10');

INSERT INTO audit_logs (action, actor, details, severity) VALUES 
('MCP_SERVER_BOOTSTRAP', 'SYSTEM', 'Spring Boot AI MCP Gateway initialized successfully', 'INFO'),
('SECURITY_POLICY_ENFORCED', 'SqlSanitizer', 'Read-only query enforcement active with 5s timeout', 'INFO');
