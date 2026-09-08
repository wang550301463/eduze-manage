-- ============================================================================
-- V1.5.1 课程体系（罗恩菲尔德 5 阶段 + 778 维度）数据表
-- ============================================================================

-- 1. 阶段
CREATE TABLE IF NOT EXISTS t_curriculum_stage (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    age_min INT NOT NULL,
    age_max INT NOT NULL,
    order_no INT NOT NULL DEFAULT 0,
    lorenfield_phase VARCHAR(64) NULL,
    description VARCHAR(512) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_stage_code (tenant_id, code, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 维度
CREATE TABLE IF NOT EXISTS t_curriculum_dimension (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    kind VARCHAR(16) NOT NULL COMMENT 'ELEMENT/PRINCIPLE/MOVEMENT',
    code VARCHAR(48) NOT NULL,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(512) NULL,
    order_no INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_dim_code (tenant_id, code, deleted_at),
    KEY idx_dim_kind (kind, order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 阶段 ↔ 维度
CREATE TABLE IF NOT EXISTS t_stage_dimension (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    stage_id BIGINT NOT NULL,
    dimension_id BIGINT NOT NULL,
    weight TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_stage_dim (tenant_id, stage_id, dimension_id),
    KEY idx_sd_stage (stage_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================================
-- V9.1.0 课程体系 seed
-- 5 个阶段 + 22 个维度 (7 元素 + 7 原则 + 8 流派) + 阶段↔维度推荐挂载 + 5 个示例课程
-- ============================================================================

-- ============ 5 个阶段 ============
INSERT INTO t_curriculum_stage (id, tenant_id, code, name, age_min, age_max, order_no, lorenfield_phase, description) VALUES
 (200001,1,'STAGE_KMD','启蒙阶段',4,7,1,'Preschematic（样式化前期）','涂鸦向符号化过渡；以色彩感知与基础造型启发为主'),
 (200002,1,'STAGE_TS', '探索阶段',7,9,2,'Schematic（样式化期）','用图式表达事物；引入线条/形状/色彩三元素'),
 (200003,1,'STAGE_CZ', '成长阶段',9,11,3,'Dawning Realism（写实萌芽期）','写实意识萌发；引入空间/明度/比例与构图原则'),
 (200004,1,'STAGE_JJ', '进阶阶段',11,13,4,'Pseudo-Realistic（拟写实期）','主动追求"像"；引入对比/节奏/统一与流派启蒙'),
 (200005,1,'STAGE_SB', '思辨阶段',13,16,5,'Adolescent Art（决定期）','风格意识自觉；以现代主义诸流派为媒介进行个人表达');

-- ============ 22 个维度 ============
-- ELEMENTS 7
INSERT INTO t_curriculum_dimension (id, tenant_id, kind, code, name, description, order_no) VALUES
 (210001,1,'ELEMENT','EL_LINE','线条 (Line)','所有视觉表达的基础元素', 1),
 (210002,1,'ELEMENT','EL_SHAPE','形状 (Shape)','由线条围合形成的二维区域', 2),
 (210003,1,'ELEMENT','EL_FORM','形态 (Form)','具有三维感的形状', 3),
 (210004,1,'ELEMENT','EL_SPACE','空间 (Space)','画面的前后纵深与正负关系', 4),
 (210005,1,'ELEMENT','EL_COLOR','色彩 (Color)','色相、明度、饱和度三属性的综合', 5),
 (210006,1,'ELEMENT','EL_VALUE','明度 (Value)','色彩的明暗程度', 6),
 (210007,1,'ELEMENT','EL_TEXTURE','肌理 (Texture)','表面的视觉与触觉质感', 7),
 (210008,1,'PRINCIPLE','PR_BALANCE','平衡 (Balance)','视觉重量在画面中的分布', 1),
 (210009,1,'PRINCIPLE','PR_CONTRAST','对比 (Contrast)','差异化的并置带来的张力', 2),
 (210010,1,'PRINCIPLE','PR_EMPHASIS','强调 (Emphasis)','吸引视线的视觉焦点', 3),
 (210011,1,'PRINCIPLE','PR_RHYTHM','节奏 (Rhythm/Movement)','元素重复或变化产生的运动感', 4),
 (210012,1,'PRINCIPLE','PR_PATTERN','图案 (Pattern)','元素的规律性重复', 5),
 (210013,1,'PRINCIPLE','PR_UNITY','统一 (Unity)','整体协调的视觉感受', 6),
 (210014,1,'PRINCIPLE','PR_PROPORTION','比例 (Proportion)','元素之间大小关系的恰当性', 7),
 (210015,1,'MOVEMENT','MV_IMPRESSIONISM','印象派 (Impressionism)','19 世纪末光影与瞬间感受的捕捉', 1),
 (210016,1,'MOVEMENT','MV_POST_IMPRESSIONISM','后印象派 (Post-Impressionism)','在印象派基础上强化结构、情感与符号', 2),
 (210017,1,'MOVEMENT','MV_FAUVISM','野兽派 (Fauvism)','大胆色彩与强烈笔触的表现性流派', 3),
 (210018,1,'MOVEMENT','MV_EXPRESSIONISM','表现主义 (Expressionism)','以变形与色彩表达情感与心理', 4),
 (210019,1,'MOVEMENT','MV_CUBISM','立体主义 (Cubism)','多视点几何化对象的分解与重构', 5),
 (210020,1,'MOVEMENT','MV_SURREALISM','超现实主义 (Surrealism)','潜意识与梦境的视觉化', 6),
 (210021,1,'MOVEMENT','MV_ABSTRACT_EXPRESSIONISM','抽象表现主义 (Abstract Expressionism)','纯粹抽象的情感性表达', 7),
 (210022,1,'MOVEMENT','MV_POP_ART','波普艺术 (Pop Art)','以大众文化与商品图像为题材', 8);

-- ============ 阶段 ↔ 维度推荐挂载 ============
-- STAGE_KMD 4-7: EL_LINE/EL_SHAPE/EL_COLOR weight=2
INSERT IGNORE INTO t_stage_dimension (id,tenant_id,stage_id,dimension_id,weight) VALUES
 (211001,1,200001,210001,2),(211002,1,200001,210002,2),(211003,1,200001,210005,2),
-- STAGE_TS 7-9: +EL_TEXTURE weight=2, PR_PATTERN weight=1
 (211010,1,200002,210001,2),(211011,1,200002,210002,2),(211012,1,200002,210005,2),
 (211013,1,200002,210007,2),(211014,1,200002,210012,1),
-- STAGE_CZ 9-11: EL_SPACE/EL_VALUE/PR_PROPORTION/PR_BALANCE weight=2, EL_FORM weight=1
 (211020,1,200003,210004,2),(211021,1,200003,210006,2),(211022,1,200003,210014,2),
 (211023,1,200003,210008,2),(211024,1,200003,210003,1),
-- STAGE_JJ 11-13: PR_CONTRAST/PR_RHYTHM/PR_UNITY/MV_IMP/MV_POSTIMP weight=2, PR_EMPHASIS weight=1
 (211030,1,200004,210009,2),(211031,1,200004,210011,2),(211032,1,200004,210013,2),
 (211033,1,200004,210015,2),(211034,1,200004,210016,2),(211035,1,200004,210010,1),
-- STAGE_SB 13-16: 6 流派 weight=2, PR_EMPHASIS/PR_UNITY weight=1
 (211040,1,200005,210017,2),(211041,1,200005,210018,2),(211042,1,200005,210019,2),
 (211043,1,200005,210020,2),(211044,1,200005,210021,2),(211045,1,200005,210022,2),
 (211046,1,200005,210010,1),(211047,1,200005,210013,1);
