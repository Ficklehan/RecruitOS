-- ============================================================
-- 候选人 + 简历完整信息补全（工作经历/项目/AI洞察）
-- 在 test-data-linkage-seed.sql 之后执行
-- ============================================================
USE `recruit_os`;

-- 1. 候选人基础字段补全
UPDATE `candidate` SET
  `gender`='MALE', `birth_date`='1992-05-18', `major`='计算机科学与技术', `work_location`='北京',
  `remark`='阿里 P7 前端，Vue3 源码级理解，主导过千万 DAU 产品前端架构'
WHERE `id`=1;
UPDATE `candidate` SET
  `gender`='FEMALE', `birth_date`='1998-03-22', `major`='软件工程', `work_location`='北京',
  `remark`='腾讯 IEG 前端，Vue3 + 微前端实践，沟通表达优秀'
WHERE `id`=2;
UPDATE `candidate` SET
  `gender`='MALE', `birth_date`='1996-11-08', `major`='计算机科学', `work_location`='北京',
  `remark`='字节后端，Spring Cloud 微服务，有高并发订单系统经验'
WHERE `id`=3;
UPDATE `candidate` SET
  `gender`='FEMALE', `birth_date`='1993-07-15', `major`='信息管理与信息系统', `work_location`='北京',
  `remark`='美团 B 端产品 7 年，熟悉 SaaS 商业化与需求管理方法论'
WHERE `id`=4;
UPDATE `candidate` SET
  `gender`='MALE', `birth_date`='1990-01-30', `major`='计算机应用', `work_location`='北京',
  `remark`='百度前端架构师背景，带 8 人团队，工程化体系建设经验丰富'
WHERE `id`=5;
UPDATE `candidate` SET
  `gender`='MALE', `birth_date`='1997-09-05', `major`='统计学', `work_location`='北京',
  `remark`='京东数据分析，熟悉招聘漏斗建模与 BI 看板搭建'
WHERE `id`=6;
UPDATE `candidate` SET
  `gender`='MALE', `birth_date`='1999-12-01', `major`='通信工程', `work_location`='北京',
  `remark`='小米前端 3 年，成长性好，基础扎实'
WHERE `id`=7;
UPDATE `candidate` SET
  `gender`='FEMALE', `birth_date`='1996-04-19', `major`='软件工程', `work_location`='杭州',
  `remark`='网易 Java 开发，熟悉分布式事务与消息队列'
WHERE `id`=8;
UPDATE `candidate` SET
  `gender`='MALE', `birth_date`='1988-06-12', `major`='计算机科学', `work_location`='深圳',
  `remark`='华为 10 年前端架构，大型 ToB 项目经验丰富，薪资预期偏高'
WHERE `id`=9;
UPDATE `candidate` SET
  `gender`='FEMALE', `birth_date`='1998-08-25', `major`='工商管理', `work_location`='北京',
  `remark`='滴滴产品 4 年，C 端转 B 端，学习能力强'
WHERE `id`=10;
UPDATE `candidate` SET
  `gender`='MALE', `birth_date`='1995-02-14', `major`='计算机科学', `work_location`='北京',
  `remark`='快手 Java 高级，熟悉高可用架构设计'
WHERE `id`=11;
UPDATE `candidate` SET
  `gender`='FEMALE', `birth_date`='1996-10-30', `major`='软件工程', `work_location`='杭州',
  `remark`='蚂蚁前端，React 技术栈为主，可转 Vue3'
WHERE `id`=12;
UPDATE `candidate` SET
  `gender`='MALE', `birth_date`='1999-05-07', `major`='数据科学', `work_location`='上海',
  `remark`='拼多多数据工程，ETL 与数仓建设经验'
WHERE `id`=13;
UPDATE `candidate` SET
  `gender`='FEMALE', `birth_date`='1997-01-18', `major`='视觉传达', `work_location`='北京',
  `remark`='字节 UI 设计师，Figma 组件库建设经验'
WHERE `id`=14;
UPDATE `candidate` SET
  `gender`='MALE', `birth_date`='1998-07-03', `major`='计算机科学', `work_location`='深圳',
  `remark`='腾讯后端，微服务与中间件实践丰富'
WHERE `id`=15;

-- 2. 完整简历（15 份，含 parsed_json + raw_text）
DELETE FROM `resume` WHERE `tenant_id` = 1;

-- 简历 #1 张伟
INSERT INTO `resume` (`id`,`tenant_id`,`candidate_id`,`file_name`,`file_url`,`file_type`,`parsed_json`,`raw_text`,`parse_status`,`version`,`created_at`) VALUES
(1,1,1,'张伟-高级前端.pdf','/demo/resumes/zhangwei.pdf','pdf',
JSON_OBJECT(
  'basic', JSON_OBJECT('name','张伟','phone','13900000001','email','zhangwei@test.com','company','阿里巴巴','position','高级前端工程师','workYears',6,'education','硕士','expectedSalary','45K','school','北京大学','major','计算机科学与技术','location','北京','gender','男'),
  'skills', JSON_ARRAY('Vue3','TypeScript','前端架构','React','Vite','Webpack','Node.js','微前端'),
  'summary', '6年前端开发经验，现任阿里高级前端工程师，负责核心电商产品线前端架构与性能优化，具备大型项目从0到1搭建能力。',
  'workExperience', JSON_ARRAY(
    JSON_OBJECT('company','阿里巴巴','position','高级前端工程师','startDate','2021-03','endDate',NULL,'description','负责淘宝核心交易链路前端架构，主导 Vue3 + Vite 迁移，页面性能提升 40%；搭建组件库覆盖 200+ 业务组件；指导 3 名 junior 工程师成长。'),
    JSON_OBJECT('company','美团','position','前端工程师','startDate','2018-07','endDate','2021-02','description','参与外卖商家端重构，负责订单模块与数据可视化大屏；引入 TypeScript 规范团队代码质量。')
  ),
  'projectExperience', JSON_ARRAY(
    JSON_OBJECT('name','淘宝交易链路性能优化','role','技术负责人','startDate','2022-06','endDate','2023-12','description','通过代码分割、预加载、SSR 优化等手段，将 LCP 从 3.2s 降至 1.8s，转化率提升 2.3%。'),
    JSON_OBJECT('name','企业级组件库 RecruitUI','role','架构师','startDate','2021-09','endDate','2022-05','description','基于 Vue3 + TS 搭建内部组件库，支持主题定制与按需加载，被 5 个业务线采纳。')
  ),
  'education', JSON_ARRAY(JSON_OBJECT('school','北京大学','major','计算机科学与技术','degree','硕士','startDate','2014-09','endDate','2017-06')),
  'aiInsights', JSON_OBJECT('strength','Vue3/TS 深度匹配岗位核心标签，有大厂复杂项目架构经验','risk','期望薪资接近上限，需确认职级与期权 package')
),
'【张伟】高级前端工程师 | 6年经验 | 北京\n阿里巴巴 · 高级前端工程师 (2021.03-至今)\n- 负责淘宝核心交易链路前端架构\n- 主导 Vue3 迁移，性能提升 40%\n\n美团 · 前端工程师 (2018.07-2021.02)\n\n北京大学 · 计算机硕士\n技能: Vue3, TypeScript, React, 微前端', 2, 1, '2026-06-01 10:00:00');

-- 简历 #2 李娜
INSERT INTO `resume` (`id`,`tenant_id`,`candidate_id`,`file_name`,`file_url`,`file_type`,`parsed_json`,`raw_text`,`parse_status`,`version`,`created_at`) VALUES
(2,1,2,'李娜-前端.pdf','/demo/resumes/lina.pdf','pdf',
JSON_OBJECT(
  'basic', JSON_OBJECT('name','李娜','phone','13900000002','email','lina@test.com','company','腾讯','position','前端工程师','workYears',4,'education','本科','expectedSalary','35K','school','浙江大学','major','软件工程','location','北京','gender','女'),
  'skills', JSON_ARRAY('Vue3','JavaScript','Webpack','CSS3','Element Plus','Git'),
  'summary', '4年前端经验，腾讯 IEG 游戏运营平台前端开发，Vue3 技术栈熟练，有完整业务模块交付经验。',
  'workExperience', JSON_ARRAY(
    JSON_OBJECT('company','腾讯','position','前端工程师','startDate','2021-06','endDate',NULL,'description','负责游戏运营后台前端开发，独立完成活动配置、数据报表等 6 个模块；参与微前端架构试点。'),
    JSON_OBJECT('company','网易','position','前端实习生','startDate','2020-03','endDate','2021-05','description','参与云音乐 H5 活动页开发，熟悉移动端适配与性能优化。')
  ),
  'projectExperience', JSON_ARRAY(
    JSON_OBJECT('name','游戏运营活动平台','role','前端开发','startDate','2022-01','endDate',NULL,'description','支持 50+ 运营活动快速配置上线，组件化方案缩短开发周期 60%。')
  ),
  'education', JSON_ARRAY(JSON_OBJECT('school','浙江大学','major','软件工程','degree','本科','startDate','2016-09','endDate','2020-06')),
  'aiInsights', JSON_OBJECT('strength','Vue3 经验丰富，沟通清晰，初面反馈积极','risk','项目规模偏小，架构经验需复试进一步验证')
),
'李娜 | 腾讯前端 | 4年 | Vue3\n浙江大学 软件工程本科', 2, 1, '2026-06-02 11:00:00');

-- 简历 #3 陈浩
INSERT INTO `resume` (`id`,`tenant_id`,`candidate_id`,`file_name`,`file_url`,`file_type`,`parsed_json`,`raw_text`,`parse_status`,`version`,`created_at`) VALUES
(3,1,5,'陈浩-前端架构.pdf','/demo/resumes/chenhao.pdf','pdf',
JSON_OBJECT(
  'basic', JSON_OBJECT('name','陈浩','phone','13900000005','email','chenhao@test.com','company','百度','position','高级前端/前端架构师','workYears',8,'education','本科','expectedSalary','55K','school','武汉大学','major','计算机应用','location','北京','gender','男'),
  'skills', JSON_ARRAY('Vue3','React','前端架构','工程化','Monorepo','CI/CD','Team Lead'),
  'summary', '8年前端经验，百度前端架构师，曾带领 8 人团队，擅长大型前端工程化体系建设与技术选型。',
  'workExperience', JSON_ARRAY(
    JSON_OBJECT('company','百度','position','高级前端工程师/架构师','startDate','2019-04','endDate',NULL,'description','负责搜索产品线前端架构，搭建 Monorepo 工程体系；制定前端规范与 Code Review 流程；团队技术分享与人才培养。'),
    JSON_OBJECT('company','京东','position','前端工程师','startDate','2016-07','endDate','2019-03','description','参与商城首页重构，负责性能监控与错误追踪体系搭建。')
  ),
  'projectExperience', JSON_ARRAY(
    JSON_OBJECT('name','百度搜索前端工程化平台','role','架构负责人','startDate','2020-01','endDate','2022-08','description','统一 10+ 子应用构建流程，发布效率提升 3 倍，线上故障率下降 50%。'),
    JSON_OBJECT('name','前端监控告警系统','role','技术负责人','startDate','2019-06','endDate','2020-12','description','接入 Sentry + 自研埋点，实现全链路错误追踪与性能大盘。')
  ),
  'education', JSON_ARRAY(JSON_OBJECT('school','武汉大学','major','计算机应用','degree','本科','startDate','2012-09','endDate','2016-06')),
  'aiInsights', JSON_OBJECT('strength','匹配分 95，架构与工程化能力突出，终面已通过','risk','薪资预期 55K 高于岗位中位，需 HR 确认')
),
'陈浩 | 百度前端架构 | 8年经验', 2, 1, '2026-05-25 09:00:00');

-- 简历 #4 刘洋
INSERT INTO `resume` (`id`,`tenant_id`,`candidate_id`,`file_name`,`file_url`,`file_type`,`parsed_json`,`raw_text`,`parse_status`,`version`,`created_at`) VALUES
(4,1,6,'刘洋-数据分析.pdf','/demo/resumes/liuyang.pdf','pdf',
JSON_OBJECT(
  'basic', JSON_OBJECT('name','刘洋','phone','13900000006','email','liuyang@test.com','company','京东','position','数据分析师','workYears',4,'education','硕士','expectedSalary','38K','school','中科院','major','统计学','location','北京','gender','男'),
  'skills', JSON_ARRAY('SQL','Python','Tableau','Power BI','A/B测试','漏斗分析','数据建模'),
  'summary', '4年数据分析经验，专注电商与 HR 领域数据洞察，熟悉指标体系搭建与可视化报表。',
  'workExperience', JSON_ARRAY(
    JSON_OBJECT('company','京东','position','数据分析师','startDate','2021-08','endDate',NULL,'description','负责零售业务数据分析，搭建 GMV/转化/留存核心指标看板；支持运营策略 A/B 测试评估。'),
    JSON_OBJECT('company','滴滴','position','数据分析实习生','startDate','2020-06','endDate','2021-07','description','参与司机侧运营数据分析，输出周报与专题分析报告。')
  ),
  'projectExperience', JSON_ARRAY(
    JSON_OBJECT('name','招聘漏斗分析体系','role','分析负责人','startDate','2022-03','endDate','2023-06','description','设计从简历到入职全链路漏斗指标，识别瓶颈环节，助力招聘效率提升 18%。')
  ),
  'education', JSON_ARRAY(JSON_OBJECT('school','中科院','major','统计学','degree','硕士','startDate','2018-09','endDate','2021-06')),
  'aiInsights', JSON_OBJECT('strength','与数据分析师岗位高度匹配，已入职','risk','无')
),
'刘洋 | 京东数据分析 | 硕士', 2, 1, '2026-05-15 14:00:00');

-- 简历 #5 赵敏
INSERT INTO `resume` (`id`,`tenant_id`,`candidate_id`,`file_name`,`file_url`,`file_type`,`parsed_json`,`raw_text`,`parse_status`,`version`,`created_at`) VALUES
(5,1,4,'赵敏-产品经理.pdf','/demo/resumes/zhaomin.pdf','pdf',
JSON_OBJECT(
  'basic', JSON_OBJECT('name','赵敏','phone','13900000004','email','zhaomin@test.com','company','美团','position','高级产品经理','workYears',7,'education','硕士','expectedSalary','50K','school','复旦大学','major','信息管理','location','北京','gender','女'),
  'skills', JSON_ARRAY('B端产品','需求分析','PRD','Axure','HR SaaS','用户研究','数据驱动'),
  'summary', '7年 B 端产品经验，美团 SaaS 产品线产品负责人，熟悉 HR/企业服务领域。',
  'workExperience', JSON_ARRAY(
    JSON_OBJECT('company','美团','position','高级产品经理','startDate','2020-05','endDate',NULL,'description','负责商家 SaaS 工具产品规划，管理 3 个产品线 roadmap；推动 DAU 增长 120%。'),
    JSON_OBJECT('company','用友','position','产品经理','startDate','2017-07','endDate','2020-04','description','参与 ERP 人力资源模块产品设计，完成 20+ 客户需求迭代。')
  ),
  'projectExperience', JSON_ARRAY(
    JSON_OBJECT('name','美团商家 HR 工具','role','产品负责人','startDate','2021-01','endDate',NULL,'description','从0到1设计排班、考勤、绩效一体化工具，获 5000+ 商家使用。')
  ),
  'education', JSON_ARRAY(JSON_OBJECT('school','复旦大学','major','信息管理','degree','硕士','startDate','2014-09','endDate','2017-06')),
  'aiInsights', JSON_OBJECT('strength','B端+HR SaaS 经验与岗位高度契合','risk','薪资预期较高，需确认预算')
),
'赵敏 | 美团高级产品 | 7年', 2, 1, '2026-06-04 16:00:00');

-- 简历 #6-15（其余候选人完整简历）
INSERT INTO `resume` (`id`,`tenant_id`,`candidate_id`,`file_name`,`file_url`,`file_type`,`parsed_json`,`raw_text`,`parse_status`,`version`,`created_at`) VALUES
(6,1,12,'马丽-前端.pdf','/demo/resumes/mali.pdf','pdf',
JSON_OBJECT('basic',JSON_OBJECT('name','马丽','phone','13900000012','email','mali@test.com','company','蚂蚁集团','position','前端工程师','workYears',5,'education','硕士','expectedSalary','40K','school','上海交通大学','major','软件工程','location','杭州'),
 'skills',JSON_ARRAY('React','Vue3','TypeScript','Ant Design','Redux'),
 'summary','5年前端，蚂蚁财富业务线，React 为主，有复杂表单与数据可视化经验',
 'workExperience',JSON_ARRAY(JSON_OBJECT('company','蚂蚁集团','position','前端工程师','startDate','2020-09','endDate',NULL,'description','负责财富 App H5 与后台管理系统开发')),
 'projectExperience',JSON_ARRAY(JSON_OBJECT('name','财富数据看板','role','前端开发','startDate','2022-01','endDate',NULL,'description','ECharts 可视化大屏，支持实时数据刷新')),
 'aiInsights',JSON_OBJECT('strength','React 经验丰富，可快速转 Vue3','risk','杭州 base，需确认是否接受北京')),
'马丽 | 蚂蚁前端 | React',2,1,'2026-06-05 10:00:00'),
(7,1,3,'王强-Java.pdf','/demo/resumes/wangqiang.pdf','pdf',
JSON_OBJECT('basic',JSON_OBJECT('name','王强','phone','13900000003','email','wangqiang@test.com','company','字节跳动','position','Java工程师','workYears',5,'education','本科','expectedSalary','40K','school','清华大学','major','计算机科学','location','北京'),
 'skills',JSON_ARRAY('Java','Spring Boot','Spring Cloud','MySQL','Redis','Kafka','Docker'),
 'summary','5年 Java 后端，字节电商中台，微服务与高并发场景经验丰富',
 'workExperience',JSON_ARRAY(JSON_OBJECT('company','字节跳动','position','Java工程师','startDate','2020-07','endDate',NULL,'description','负责订单中台服务开发，日订单量 500万+')),
 'projectExperience',JSON_ARRAY(JSON_OBJECT('name','订单中台重构','role','核心开发','startDate','2022-03','endDate','2023-08','description','DDD 领域驱动设计，服务拆分与性能优化')),
 'aiInsights',JSON_OBJECT('strength','Java+微服务匹配后端岗','risk','尚未完成初筛')),
'王强 | 字节 Java',2,1,'2026-06-03 09:00:00'),
(8,1,15,'徐明-后端.pdf','/demo/resumes/xuming.pdf','pdf',
JSON_OBJECT('basic',JSON_OBJECT('name','徐明','phone','13900000015','email','xuming@test.com','company','腾讯','position','后端开发','workYears',4,'education','硕士','expectedSalary','38K','school','北京航空航天大学','major','计算机科学','location','深圳'),
 'skills',JSON_ARRAY('Java','Spring Boot','微服务','MySQL','Redis','gRPC'),
 'summary','4年腾讯后端，社交产品线，熟悉高可用服务设计',
 'workExperience',JSON_ARRAY(JSON_OBJECT('company','腾讯','position','后端开发','startDate','2021-07','endDate',NULL,'description','负责 IM 消息服务模块，QPS 10万+')),
 'projectExperience',JSON_ARRAY(JSON_OBJECT('name','消息服务容灾改造','role','开发','startDate','2023-01','endDate',NULL,'description','多机房部署，RTO<30s')),
 'aiInsights',JSON_OBJECT('strength','微服务经验丰富，筛选已通过','risk','深圳 base')),
'徐明 | 腾讯后端',2,1,'2026-06-06 11:00:00'),
(9,1,7,'杨帆-前端.pdf','/demo/resumes/yangfan.pdf','pdf',
JSON_OBJECT('basic',JSON_OBJECT('name','杨帆','phone','13900000007','email','yangfan@test.com','company','小米','position','前端开发','workYears',3,'education','本科','expectedSalary','28K','school','北京邮电大学','major','通信工程','location','北京'),
 'skills',JSON_ARRAY('Vue3','JavaScript','CSS','移动端H5'),
 'summary','3年小米前端，IoT 控制面板与官网开发',
 'workExperience',JSON_ARRAY(JSON_OBJECT('company','小米','position','前端开发','startDate','2022-07','endDate',NULL,'description','米家 App 控制面板模块')),
 'projectExperience',JSON_ARRAY(JSON_OBJECT('name','米家设备控制面板','role','前端','startDate','2023-01','endDate',NULL,'description','Vue3 重构 legacy 页面')),
 'aiInsights',JSON_OBJECT('strength','潜力型中级前端','risk','经验偏少，需技术面验证')),
'杨帆 | 小米前端 3年',2,1,'2026-06-06 14:00:00'),
(10,1,9,'吴磊-前端架构.pdf','/demo/resumes/wulei.pdf','pdf',
JSON_OBJECT('basic',JSON_OBJECT('name','吴磊','phone','13900000009','email','wulei@test.com','company','华为','position','前端架构师','workYears',10,'education','硕士','expectedSalary','60K','school','中国科学技术大学','major','计算机科学','location','深圳'),
 'skills',JSON_ARRAY('前端架构','Vue3','React','微前端','性能优化','Team Lead'),
 'summary','10年前端架构师，华为企业业务线，ToB 大型项目经验丰富',
 'workExperience',JSON_ARRAY(JSON_OBJECT('company','华为','position','前端架构师','startDate','2018-03','endDate',NULL,'description','企业云服务前端架构，20+ 子系统')),
 'projectExperience',JSON_ARRAY(JSON_OBJECT('name','华为云控制台','role','架构师','startDate','2019-01','endDate',NULL,'description','微前端 + 模块联邦方案')),
 'aiInsights',JSON_OBJECT('strength','资深前端，人才库储备','risk','薪资远超当前岗位预算')),
'吴磊 | 华为前端架构 10年',2,1,'2026-05-20 10:00:00'),
(11,1,10,'郑雪-产品.pdf','/demo/resumes/zhengxue.pdf','pdf',
JSON_OBJECT('basic',JSON_OBJECT('name','郑雪','phone','13900000010','email','zhengxue@test.com','company','滴滴','position','产品经理','workYears',4,'education','本科','expectedSalary','35K','school','中国人民大学','major','工商管理','location','北京'),
 'skills',JSON_ARRAY('产品设计','用户研究','数据分析','Axure'),
 'summary','4年滴滴产品，出行与平台产品经验',
 'workExperience',JSON_ARRAY(JSON_OBJECT('company','滴滴','position','产品经理','startDate','2021-04','endDate',NULL,'description','司机端功能迭代')),
 'projectExperience',JSON_ARRAY(JSON_OBJECT('name','司机激励体系','role','产品','startDate','2022-06','endDate',NULL,'description','提升司机活跃度 15%')),
 'aiInsights',JSON_OBJECT('strength','内推候选人，产品基础扎实','risk','B端经验不足')),
'郑雪 | 滴滴产品',2,1,'2026-06-01 09:00:00'),
(12,1,11,'孙超-Java.pdf','/demo/resumes/sunchao.pdf','pdf',
JSON_OBJECT('basic',JSON_OBJECT('name','孙超','phone','13900000011','email','sunchao@test.com','company','快手','position','Java高级开发','workYears',6,'education','本科','expectedSalary','42K','school','北京理工大学','major','计算机科学','location','北京'),
 'skills',JSON_ARRAY('Java','Spring Boot','高并发','MySQL','Redis'),
 'summary','6年快手 Java，直播业务后端',
 'workExperience',JSON_ARRAY(JSON_OBJECT('company','快手','position','Java高级开发','startDate','2019-06','endDate',NULL,'description','直播礼物系统核心开发')),
 'projectExperience',JSON_ARRAY(JSON_OBJECT('name','礼物系统重构','role','核心开发','startDate','2022-01','endDate','2023-06','description','支撑峰值 QPS 50万')),
 'aiInsights',JSON_OBJECT('strength','Java 高级，高并发场景','risk','新简历待筛选')),
'孙超 | 快手 Java',2,1,'2026-06-07 08:30:00'),
(13,1,13,'黄涛-数据.pdf','/demo/resumes/huangtao.pdf','pdf',
JSON_OBJECT('basic',JSON_OBJECT('name','黄涛','phone','13900000013','email','huangtao@test.com','company','拼多多','position','数据工程师','workYears',3,'education','本科','expectedSalary','30K','school','华中科技大学','major','数据科学','location','上海'),
 'skills',JSON_ARRAY('SQL','Python','Spark','Hive','ETL'),
 'summary','3年数据工程，拼多多数仓建设',
 'workExperience',JSON_ARRAY(JSON_OBJECT('company','拼多多','position','数据工程师','startDate','2022-08','endDate',NULL,'description','ODS/DWD 层建设')),
 'projectExperience',JSON_ARRAY(JSON_OBJECT('name','用户行为数仓','role','开发','startDate','2023-01','endDate',NULL,'description','日增 10TB 数据处理')),
 'aiInsights',JSON_OBJECT('strength','数据工程基础好','risk','偏工程非分析')),
'黄涛 | 拼多多数据工程',2,1,'2026-06-05 16:00:00'),
(14,1,14,'林琳-设计.pdf','/demo/resumes/linlin.pdf','pdf',
JSON_OBJECT('basic',JSON_OBJECT('name','林琳','phone','13900000014','email','linlin@test.com','company','字节跳动','position','UI设计师','workYears',5,'education','本科','expectedSalary','32K','school','中央美术学院','major','视觉传达','location','北京'),
 'skills',JSON_ARRAY('Figma','Sketch','UI设计','设计系统','交互设计'),
 'summary','5年 UI 设计，字节设计系统团队成员',
 'workExperience',JSON_ARRAY(JSON_OBJECT('company','字节跳动','position','UI设计师','startDate','2020-03','endDate',NULL,'description','飞书设计系统组件设计')),
 'projectExperience',JSON_ARRAY(JSON_OBJECT('name','飞书 Design Token','role','设计师','startDate','2022-01','endDate',NULL,'description','统一 300+ 设计 token')),
 'aiInsights',JSON_OBJECT('strength','设计能力优秀','risk','与前端岗不匹配，已淘汰')),
'林琳 | 字节 UI设计',2,1,'2026-05-28 11:00:00'),
(15,1,8,'周婷-Java.pdf','/demo/resumes/zhouting.pdf','pdf',
JSON_OBJECT('basic',JSON_OBJECT('name','周婷','phone','13900000008','email','zhouting@test.com','company','网易','position','Java开发','workYears',5,'education','本科','expectedSalary','38K','school','南开大学','major','软件工程','location','杭州'),
 'skills',JSON_ARRAY('Java','Spring','MySQL','Redis','分布式'),
 'summary','5年网易 Java，游戏服务端开发',
 'workExperience',JSON_ARRAY(JSON_OBJECT('company','网易','position','Java开发','startDate','2020-07','endDate',NULL,'description','游戏登录与支付模块')),
 'projectExperience',JSON_ARRAY(JSON_OBJECT('name','分布式事务方案','role','开发','startDate','2022-06','endDate',NULL,'description','Seata 集成落地')),
 'aiInsights',JSON_OBJECT('strength','Java 基础扎实','risk','待安排面试')),
'周婷 | 网易 Java 5年',2,1,'2026-06-07 08:00:00');

-- 3. 同步寻源轨迹 resume_id
UPDATE `campaign_candidate_trace` SET `resume_id` = 1 WHERE `candidate_id` = 1 AND `tenant_id` = 1;
UPDATE `campaign_candidate_trace` SET `resume_id` = 2 WHERE `candidate_id` = 2 AND `tenant_id` = 1;
UPDATE `campaign_candidate_trace` SET `resume_id` = 7 WHERE `candidate_id` = 3 AND `tenant_id` = 1;
UPDATE `campaign_candidate_trace` SET `resume_id` = 8 WHERE `candidate_id` = 15 AND `tenant_id` = 1;
