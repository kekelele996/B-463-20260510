SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS asset_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE asset_db;

DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS equipment;
DROP TABLE IF EXISTS vehicle;

-- Users Table
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL, -- BCrypt encoded
    email VARCHAR(100),
    phone VARCHAR(20),
    status INT DEFAULT 1, -- 1: enabled, 0: disabled
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Roles Table
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE, -- Display name
    code VARCHAR(50) NOT NULL UNIQUE, -- Role code (e.g., ROLE_ADMIN)
    description VARCHAR(200),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Permissions/Menus Table
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(100), -- Permission identifier (e.g., user:add)
    path VARCHAR(200), -- Frontend route path
    component VARCHAR(200), -- Frontend component path
    type INT NOT NULL, -- 0: Directory, 1: Menu, 2: Button
    icon VARCHAR(50),
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User-Role Relation
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Role-Permission Relation
CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES sys_permission(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Existing Business Tables
CREATE TABLE IF NOT EXISTS equipment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50),
    status VARCHAR(20),
    maintenance_record TEXT,
    last_maintenance_date DATETIME,
    last_maintenance_by VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS vehicle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    license_plate VARCHAR(20) NOT NULL,
    model VARCHAR(50),
    status VARCHAR(20),
    mileage DOUBLE,
    maintenance_record TEXT,
    last_maintenance_date DATETIME,
    last_maintenance_by VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS borrow_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_type VARCHAR(20) NOT NULL,
    asset_id BIGINT NOT NULL,
    asset_name VARCHAR(100) NOT NULL,
    applicant_id BIGINT NOT NULL,
    applicant_name VARCHAR(50) NOT NULL,
    reason TEXT,
    borrow_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    approver_id BIGINT,
    approver_name VARCHAR(50),
    approve_time DATETIME,
    reject_reason TEXT,
    return_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Initial Data

-- Users (Password: 123456)
-- admin / 123456
-- user1 / 123456
INSERT INTO sys_user (id, username, password, status) VALUES (1, 'admin', '$2a$10$rE79XsS5paPLScVbxbXpIu095VVHBnNk6JRNYMG/zJCvyue1qxUNC', 1);
INSERT INTO sys_user (id, username, password, status) VALUES (2, 'user1', '$2a$10$rE79XsS5paPLScVbxbXpIu095VVHBnNk6JRNYMG/zJCvyue1qxUNC', 1);

-- Roles
INSERT INTO sys_role (id, name, code, description) VALUES (1, '管理员', 'ROLE_ADMIN', '系统管理员，拥有所有权限');
INSERT INTO sys_role (id, name, code, description) VALUES (2, '普通用户', 'ROLE_USER', '普通用户，仅限资产管理权限');

-- User Roles
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO sys_user_role (user_id, role_id) VALUES (2, 2);

-- Permissions (Basic Menu Structure)
-- 1. Dashboard
INSERT INTO sys_permission (id, parent_id, name, code, path, component, type, icon, sort_order) VALUES (1, 0, 'Dashboard', 'dashboard', '/dashboard', 'views/Dashboard', 1, 'Odometer', 1);

-- 2. System Management (Directory)
INSERT INTO sys_permission (id, parent_id, name, code, path, component, type, icon, sort_order) VALUES (2, 0, 'System Manage', 'sys:manage', '/system', 'Layout', 0, 'Setting', 2);
-- 2.1 User Management (Menu)
INSERT INTO sys_permission (id, parent_id, name, code, path, component, type, icon, sort_order) VALUES (3, 2, 'User Manage', 'sys:user:list', '/users', 'views/UserManage', 1, 'User', 1);
-- Buttons for User Manage
INSERT INTO sys_permission (id, parent_id, name, code, type) VALUES (4, 3, 'Add User', 'sys:user:add', 2);
INSERT INTO sys_permission (id, parent_id, name, code, type) VALUES (5, 3, 'Edit User', 'sys:user:edit', 2);
INSERT INTO sys_permission (id, parent_id, name, code, type) VALUES (6, 3, 'Delete User', 'sys:user:delete', 2);

-- 3. Asset Management (Directory)
INSERT INTO sys_permission (id, parent_id, name, code, path, component, type, icon, sort_order) VALUES (7, 0, 'Asset Manage', 'asset:manage', '/asset', 'Layout', 0, 'Box', 3);
-- 3.1 Equipment (Menu)
INSERT INTO sys_permission (id, parent_id, name, code, path, component, type, icon, sort_order) VALUES (8, 7, 'Equipment', 'asset:equip:list', '/equipments', 'views/EquipmentManage', 1, 'Monitor', 1);
-- 3.2 Vehicle (Menu)
INSERT INTO sys_permission (id, parent_id, name, code, path, component, type, icon, sort_order) VALUES (9, 7, 'Vehicle', 'asset:vehicle:list', '/vehicles', 'views/VehicleManage', 1, 'Van', 2);

-- 4. Borrow Management (Directory)
INSERT INTO sys_permission (id, parent_id, name, code, path, component, type, icon, sort_order) VALUES (10, 0, 'Borrow Manage', 'borrow:manage', '/borrow', 'Layout', 0, 'Document', 4);
-- 4.1 Borrow Apply (Menu)
INSERT INTO sys_permission (id, parent_id, name, code, path, component, type, icon, sort_order) VALUES (11, 10, 'Borrow Apply', 'borrow:apply', '/borrow-apply', 'views/BorrowApply', 1, 'Document', 1);
-- 4.2 Borrow Approve (Menu)
INSERT INTO sys_permission (id, parent_id, name, code, path, component, type, icon, sort_order) VALUES (12, 10, 'Borrow Approve', 'borrow:approve', '/borrow-approve', 'views/BorrowApprove', 1, 'Check', 2);

-- Role Permissions
-- Admin gets everything
INSERT INTO sys_role_permission (role_id, permission_id) SELECT 1, id FROM sys_permission;

-- User gets Dashboard and View Assets
INSERT INTO sys_role_permission (role_id, permission_id) VALUES (2, 1); -- Dashboard
INSERT INTO sys_role_permission (role_id, permission_id) VALUES (2, 7); -- Asset Manage Dir
INSERT INTO sys_role_permission (role_id, permission_id) VALUES (2, 8); -- Equipment
INSERT INTO sys_role_permission (role_id, permission_id) VALUES (2, 9); -- Vehicle
INSERT INTO sys_role_permission (role_id, permission_id) VALUES (2, 10); -- Borrow Manage Dir
INSERT INTO sys_role_permission (role_id, permission_id) VALUES (2, 11); -- Borrow Apply

-- Business Data
INSERT INTO equipment (name, type, status, maintenance_record) VALUES ('Server A', 'IT', 'online', 'Checkup done');
INSERT INTO equipment (name, type, status, maintenance_record) VALUES ('Lathe M1', 'Machine', 'maintenance', 'Oil change');

INSERT INTO vehicle (license_plate, model, status, mileage, maintenance_record) VALUES ('A88888', 'Sedan', 'driving', 10000, 'Tire check');
INSERT INTO vehicle (license_plate, model, status, mileage, maintenance_record) VALUES ('B12345', 'Truck', 'parked', 50000, 'Engine check');
