-- ============================================================
-- 企业资产管理系统 初始数据脚本
-- 说明：在 schema.sql 之后执行。用户账号由应用启动时自动初始化。
-- ============================================================
USE `asset_management`;

-- ------------------------------------------------------------
-- 系统菜单（目录/菜单/按钮 + 权限标识）
-- ------------------------------------------------------------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`) VALUES
(1,  0,  '首页',     'C', '/dashboard',  'dashboard/index',    NULL,                      'Odometer',     1, 1, 1),
(2,  0,  '系统管理', 'M', '/system',     NULL,                 NULL,                      'Setting',     10, 1, 1),
(3,  2,  '用户管理', 'C', 'user',        'system/user',        'system:user:list',        'User',         1, 1, 1),
(4,  3,  '用户新增', 'F', NULL,          NULL,                 'system:user:add',         NULL,           1, 1, 1),
(5,  3,  '用户修改', 'F', NULL,          NULL,                 'system:user:edit',        NULL,           2, 1, 1),
(6,  3,  '用户删除', 'F', NULL,          NULL,                 'system:user:delete',      NULL,           3, 1, 1),
(7,  2,  '角色管理', 'C', 'role',        'system/role',        'system:role:list',        'Avatar',       2, 1, 1),
(8,  7,  '角色新增', 'F', NULL,          NULL,                 'system:role:add',         NULL,           1, 1, 1),
(9,  7,  '角色修改', 'F', NULL,          NULL,                 'system:role:edit',        NULL,           2, 1, 1),
(10, 7,  '角色删除', 'F', NULL,          NULL,                 'system:role:delete',      NULL,           3, 1, 1),
(11, 2,  '菜单管理', 'C', 'menu',        'system/menu',        'system:menu:list',        'Menu',         3, 1, 1),
(12, 2,  '操作日志', 'C', 'log',         'system/log',         'system:log:list',         'Document',     4, 1, 1),
(13, 0,  '资产管理', 'M', '/asset',      NULL,                 NULL,                      'Box',         20, 1, 1),
(14, 13, '资产分类', 'C', 'category',    'category/index',     'asset:category:list',     'FolderOpened', 1, 1, 1),
(15, 14, '分类新增', 'F', NULL,          NULL,                 'asset:category:add',      NULL,           1, 1, 1),
(16, 14, '分类修改', 'F', NULL,          NULL,                 'asset:category:edit',     NULL,           2, 1, 1),
(17, 14, '分类删除', 'F', NULL,          NULL,                 'asset:category:delete',   NULL,           3, 1, 1),
(18, 13, '资产台账', 'C', 'asset',       'asset/index',        'asset:list',              'Goods',        2, 1, 1),
(19, 18, '资产新增', 'F', NULL,          NULL,                 'asset:add',               NULL,           1, 1, 1),
(20, 18, '资产修改', 'F', NULL,          NULL,                 'asset:edit',              NULL,           2, 1, 1),
(21, 18, '资产删除', 'F', NULL,          NULL,                 'asset:delete',            NULL,           3, 1, 1),
(22, 18, '资产报废', 'F', NULL,          NULL,                 'asset:scrap',             NULL,           4, 1, 1),
(23, 18, '资产报修', 'F', NULL,          NULL,                 'asset:repair',            NULL,           5, 1, 1),
(24, 0,  '资产业务', 'M', '/business',   NULL,                 NULL,                      'List',        30, 1, 1),
(25, 24, '资产申请', 'C', 'application', 'application/index',  'asset:application:list',  'EditPen',      1, 1, 1),
(26, 25, '申请提交', 'F', NULL,          NULL,                 'asset:application:submit',NULL,           1, 1, 1),
(27, 25, '申请审批', 'F', NULL,          NULL,                 'asset:application:approve',NULL,          2, 1, 1),
(28, 24, '资产分配', 'C', 'allocation',  'allocation/index',   'asset:allocation:list',   'Share',        2, 1, 1),
(29, 28, '分配操作', 'F', NULL,          NULL,                 'asset:allocation:allocate',NULL,          1, 1, 1),
(30, 28, '领用确认', 'F', NULL,          NULL,                 'asset:allocation:confirm',NULL,          2, 1, 1),
(31, 24, '资产归还', 'C', 'return',      'return/index',       'asset:return:list',       'RefreshLeft',  3, 1, 1),
(32, 31, '归还提交', 'F', NULL,          NULL,                 'asset:return:submit',     NULL,           1, 1, 1),
(33, 31, '归还审核', 'F', NULL,          NULL,                 'asset:return:audit',      NULL,           2, 1, 1),
(34, 0,  '统计分析', 'C', '/statistics', 'statistics/index',   'statistics:view',         'DataAnalysis', 40, 1, 1),
(35, 2,  '部门管理', 'C', 'dept',        'system/dept',        'system:dept:list',        'OfficeBuilding',5, 1, 1),
(36, 35, '部门新增', 'F', NULL,          NULL,                 'system:dept:add',         NULL,           1, 1, 1),
(37, 35, '部门修改', 'F', NULL,          NULL,                 'system:dept:edit',        NULL,           2, 1, 1),
(38, 35, '部门删除', 'F', NULL,          NULL,                 'system:dept:delete',      NULL,           3, 1, 1),
(39, 35, '部门导入', 'F', NULL,          NULL,                 'system:dept:import',      NULL,           4, 1, 1),
(40, 35, '部门导出', 'F', NULL,          NULL,                 'system:dept:export',      NULL,           5, 1, 1);

-- ------------------------------------------------------------
-- 系统角色
-- ------------------------------------------------------------
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `status`, `sort`) VALUES
(1, '超级管理员', 'admin',         '拥有系统全部权限', 1, 1),
(2, '资产管理员', 'asset_manager', '负责资产登记、分配、审批、归还审核', 1, 2),
(3, '普通员工',   'employee',      '可申请、领用、归还资产', 1, 3);

-- ------------------------------------------------------------
-- 角色-菜单关联
-- ------------------------------------------------------------
-- 超级管理员（拥有全部权限，此处冗余配置以确保一致性）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),(1,10),(1,11),(1,12),
(1,13),(1,14),(1,15),(1,16),(1,17),(1,18),(1,19),(1,20),(1,21),(1,22),(1,23),
(1,24),(1,25),(1,26),(1,27),(1,28),(1,29),(1,30),(1,31),(1,32),(1,33),(1,34),
(1,35),(1,36),(1,37),(1,38),(1,39),(1,40);

-- 资产管理员（资产 + 业务 + 统计）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2,1),
(2,13),(2,14),(2,15),(2,16),(2,17),(2,18),(2,19),(2,20),(2,21),(2,22),(2,23),
(2,24),(2,25),(2,26),(2,27),(2,28),(2,29),(2,30),(2,31),(2,32),(2,33),
(2,34);

-- 普通员工（查看资产、申请、领用确认、归还）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(3,1),
(3,18),
(3,25),(3,26),
(3,28),(3,30),
(3,31),(3,32);

-- ------------------------------------------------------------
-- 部门（多级树形）
-- ------------------------------------------------------------
INSERT INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `leader`, `phone`, `email`, `sort`, `status`, `remark`) VALUES
(1, 0, '总公司',   '张伟',   '0755-88886666', 'hq@company.com',     1, 1, '集团总部'),
(2, 1, '人事部',   '李丽',   '0755-88886001', 'hr@company.com',     1, 1, '负责人事与行政事务'),
(3, 1, '财务部',   '王芳',   '0755-88886002', 'finance@company.com',2, 1, '负责财务核算与资金管理'),
(4, 1, '技术部',   '赵强',   '0755-88886003', 'dev@company.com',    3, 1, '负责产品研发与技术支持'),
(5, 4, '研发一组', '钱进',   '0755-88886004', 'dev1@company.com',   1, 1, '核心产品研发'),
(6, 4, '研发二组', '孙磊',   '0755-88886005', 'dev2@company.com',   2, 1, '新产品预研'),
(7, 1, '市场部',   '周敏',   '0755-88886006', 'market@company.com', 4, 1, '负责市场推广与品牌'),
(8, 1, '行政部',   '吴静',   '0755-88886007', 'admin@company.com',  5, 1, '负责行政后勤保障');

-- ------------------------------------------------------------
-- 资产分类（多级）
-- ------------------------------------------------------------
INSERT INTO `asset_category` (`id`, `parent_id`, `category_name`, `category_code`, `level`, `sort`, `description`, `attributes`, `status`) VALUES
(1,  0, '电子设备', 'ELECTRONIC',   1, 1, '各类电子设备', NULL, 1),
(2,  1, '计算机',   'COMPUTER',     2, 1, '计算机类设备', '[{"name":"CPU型号","type":"text"},{"name":"内存","type":"text"}]', 1),
(3,  2, '笔记本电脑', 'LAPTOP',      3, 1, '便携式计算机', NULL, 1),
(4,  2, '台式电脑', 'DESKTOP',      3, 2, '台式计算机',   NULL, 1),
(5,  1, '办公设备', 'OFFICE_EQUIP', 2, 2, '办公自动化设备', NULL, 1),
(6,  5, '打印机',  'PRINTER',       3, 1, '打印设备', NULL, 1),
(7,  5, '投影仪',  'PROJECTOR',     3, 2, '投影显示设备', NULL, 1),
(8,  0, '家具',    'FURNITURE',     1, 2, '办公家具', NULL, 1),
(9,  8, '办公桌椅', 'OFFICE_FURN',   2, 1, '办公桌、椅等', NULL, 1),
(10, 0, '其他',    'OTHER',         1, 3, '其他资产', NULL, 1);

-- ------------------------------------------------------------
-- 示例资产（在库状态，无保管人）
-- ------------------------------------------------------------
INSERT INTO `asset` (`asset_code`, `asset_name`, `category_id`, `brand`, `model`, `serial_number`, `status`, `price`, `purchase_date`, `custodian_id`, `location`, `remark`, `create_by`, `version`) VALUES
('ZC20240100001', 'ThinkPad X1 Carbon', 3, '联想',  'X1 Carbon Gen11', 'SN-TP-001', 'IN_STOCK', 9999.00, '2024-01-10', NULL, 'A栋3楼机房', '高管用机', 1, 0),
('ZC20240100002', 'MacBook Pro 14',     3, '苹果',  'MacBook Pro 14', 'SN-MB-002', 'IN_STOCK', 14999.00, '2024-02-15', NULL, 'A栋3楼机房', NULL, 1, 0),
('ZC20240200003', 'OptiPlex 7090 台式机', 4, '戴尔', 'OptiPlex 7090', 'SN-DE-003', 'IN_STOCK', 4500.00, '2024-02-20', NULL, 'B栋2楼办公区', NULL, 1, 0),
('ZC20240300004', 'HP LaserJet 打印机',  6, '惠普', 'LaserJet Pro',   'SN-HP-004', 'IN_STOCK', 1800.00, '2024-03-01', NULL, 'B栋2楼打印室', NULL, 1, 0),
('ZC20240300005', '明基投影仪 E580',     7, '明基', 'E580',           'SN-BQ-005', 'IN_STOCK', 3200.00, '2024-03-05', NULL, '会议室', NULL, 1, 0);
