import json
import csv

civil_servant_jobs = [
  {
    "title": "综合管理一级干事",
    "organization": "国家税务总局北京市税务局",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "本科及以上",
    "major_requirements": ["计算机科学与技术", "软件工程"],
    "cert_requirements": ["CET-6", "法律职业资格证书(A证)"],
    "deadline": "2026-11-01"
  },
  {
    "title": "税收征管业务一级行政执法员",
    "organization": "国家税务总局上海市税务局",
    "category": "CIVIL_SERVANT",
    "location": "上海",
    "education_requirement": "本科及以上",
    "major_requirements": ["财政学", "税收学", "会计学"],
    "cert_requirements": ["CET-6", "注册会计师(CPA)或税务师"],
    "deadline": "2026-11-01"
  },
  {
    "title": "大企业税收服务与管理干事",
    "organization": "国家税务总局广东省税务局",
    "category": "CIVIL_SERVANT",
    "location": "广州",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["会计学", "财务管理", "审计学"],
    "cert_requirements": ["CET-6", "注册会计师(CPA)专业阶段"],
    "deadline": "2026-11-01"
  },
  {
    "title": "信息系统运行维护一级干事",
    "organization": "国家税务总局浙江省税务局",
    "category": "CIVIL_SERVANT",
    "location": "杭州",
    "education_requirement": "本科及以上",
    "major_requirements": ["计算机科学与技术", "网络工程", "信息安全"],
    "cert_requirements": ["CET-4", "网络工程师(软考中级)"],
    "deadline": "2026-11-01"
  },
  {
    "title": "金融监管综合管理岗",
    "organization": "国家金融监督管理总局北京监管局",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["金融学", "经济学", "法学"],
    "cert_requirements": ["CET-6", "法律职业资格证书(A证)"],
    "deadline": "2026-11-05"
  },
  {
    "title": "银行保险机构风险监测监管员",
    "organization": "国家金融监督管理总局上海监管局",
    "category": "CIVIL_SERVANT",
    "location": "上海",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["金融工程", "统计学", "精算学"],
    "cert_requirements": ["CET-6", "特许金融分析师(CFA)或FRM"],
    "deadline": "2026-11-05"
  },
  {
    "title": "上市公司监管综合业务岗",
    "organization": "中国证券监督管理委员会上海监管局",
    "category": "CIVIL_SERVANT",
    "location": "上海",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["法学", "经济法学", "民商法学"],
    "cert_requirements": ["CET-6", "法律职业资格证书(A证)"],
    "deadline": "2026-11-05"
  },
  {
    "title": "金融稳定与货币信贷调查员",
    "organization": "中国人民银行北京市分行",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["金融学", "宏观经济学", "计量经济学"],
    "cert_requirements": ["CET-6", "证券从业资格"],
    "deadline": "2026-11-08"
  },
  {
    "title": "外贸政策法规与进出口管理岗",
    "organization": "商务部配额许可证事务局",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "本科及以上",
    "major_requirements": ["国际经济与贸易", "法学", "英语"],
    "cert_requirements": ["CET-6 (450分及以上)", "法律职业资格证书(A证)"],
    "deadline": "2026-11-01"
  },
  {
    "title": "口岸通关监管一级行政执法员",
    "organization": "中华人民共和国海关总署北京海关",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "本科及以上",
    "major_requirements": ["海关管理", "国际贸易学", "应用化学"],
    "cert_requirements": ["CET-4", "全国计算机等级考试二级"],
    "deadline": "2026-11-01"
  },
  {
    "title": "进出口商品检验监管执法员",
    "organization": "中华人民共和国海关总署黄埔海关",
    "category": "CIVIL_SERVANT",
    "location": "广州",
    "education_requirement": "本科及以上",
    "major_requirements": ["机械工程", "化学工程与工艺", "食品质量与安全"],
    "cert_requirements": ["CET-4", "注册质量工程师基础"],
    "deadline": "2026-11-01"
  },
  {
    "title": "通信网络安全与信息技术审查员",
    "organization": "工业和信息化部信息通信管理局",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["信息安全", "网络空间安全", "软件工程"],
    "cert_requirements": ["CET-6", "信息安全工程师(软考中级)"],
    "deadline": "2026-11-05"
  },
  {
    "title": "法治督察与行政执法监督干事",
    "organization": "司法部法治督察局",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["法理学", "宪法学与行政法学", "诉讼法学"],
    "cert_requirements": ["CET-6", "法律职业资格证书(A证)"],
    "deadline": "2026-11-05"
  },
  {
    "title": "财政收支监管与绩效评价专员",
    "organization": "财政部监督评价局",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["财政学", "公共管理", "会计学"],
    "cert_requirements": ["CET-6", "注册会计师(CPA)"],
    "deadline": "2026-11-05"
  },
  {
    "title": "应急指挥与防灾减灾综合协调岗",
    "organization": "应急管理部应急指挥专员办公室",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "本科及以上",
    "major_requirements": ["安全工程", "防灾减灾工程及防护工程", "地理信息科学"],
    "cert_requirements": ["CET-4", "注册安全工程师(初级或中级)"],
    "deadline": "2026-11-02"
  },
  {
    "title": "反垄断与反不正当竞争行政执法员",
    "organization": "国家市场监督管理总局执法稽查局",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["经济法学", "知识产权法", "产业经济学"],
    "cert_requirements": ["CET-6", "法律职业资格证书(A证)"],
    "deadline": "2026-11-05"
  },
  {
    "title": "金融审计特派业务处一级科员",
    "organization": "审计署驻上海特派员办事处",
    "category": "CIVIL_SERVANT",
    "location": "上海",
    "education_requirement": "本科及以上",
    "major_requirements": ["审计学", "会计学", "金融工程"],
    "cert_requirements": ["CET-6", "初级审计师或CIA国际注册内部审计师"],
    "deadline": "2026-11-01"
  },
  {
    "title": "企业审计特派业务处一级科员",
    "organization": "审计署驻广州特派员办事处",
    "category": "CIVIL_SERVANT",
    "location": "广州",
    "education_requirement": "本科及以上",
    "major_requirements": ["财务管理", "资产评估", "会计学"],
    "cert_requirements": ["CET-6", "注册会计师(CPA)专业阶段合格证"],
    "deadline": "2026-11-01"
  },
  {
    "title": "智慧法院审判大数据系统运维工程师",
    "organization": "最高人民法院信息中心",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["数据科学与大数据技术", "软件工程", "人工智能"],
    "cert_requirements": ["CET-6", "系统架构设计师(软考高级)"],
    "deadline": "2026-11-05"
  },
  {
    "title": "检察技术司法鉴定与电子取证专家",
    "organization": "最高人民检察院检察技术信息研究中心",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["网络空间安全", "法医学", "电子信息工程"],
    "cert_requirements": ["CET-6", "网络安全工程师或电子数据取证认证"],
    "deadline": "2026-11-05"
  },
  {
    "title": "环境影响评价技术审批复核专员",
    "organization": "生态环境部环境工程评估中心",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["环境工程", "环境科学", "资源环境科学"],
    "cert_requirements": ["CET-6", "注册环境影响评价工程师(具备资格或报考通过)"],
    "deadline": "2026-11-05"
  },
  {
    "title": "海事执法与水上搜救指挥员",
    "organization": "交通运输部海事局深圳海事局",
    "category": "CIVIL_SERVANT",
    "location": "深圳",
    "education_requirement": "本科及以上",
    "major_requirements": ["航海技术", "轮机工程", "海事管理"],
    "cert_requirements": ["CET-4", "无限航区一等三副或三管轮适任证书"],
    "deadline": "2026-11-01"
  },
  {
    "title": "经济统计与数据调查分析一级科员",
    "organization": "国家统计局北京调查总队",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "本科及以上",
    "major_requirements": ["应用统计学", "经济统计学", "数据计算及应用"],
    "cert_requirements": ["CET-6", "初级统计师或全国计算机等级考试三级"],
    "deadline": "2026-11-01"
  },
  {
    "title": "出入境边防检查站一级警员",
    "organization": "国家移民管理局北京出入境边防检查总站",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "本科及以上",
    "major_requirements": ["侦查学", "法学", "阿拉伯语", "西班牙语"],
    "cert_requirements": ["CET-4 (英语专业需TEM-8)", "全国计算机等级考试二级"],
    "deadline": "2026-11-01"
  },
  {
    "title": "计算机与软件领域专利审查员",
    "organization": "国家知识产权局专利局",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["计算机科学与技术", "电子科学与技术", "自动化"],
    "cert_requirements": ["CET-6 (480分及以上)", "软件设计师(软考中级)"],
    "deadline": "2026-11-05"
  },
  {
    "title": "水资源监控与节水政策法规研拟干事",
    "organization": "水利部水资源管理司",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["水文与水资源工程", "水利水电工程", "农业水利工程"],
    "cert_requirements": ["CET-6", "注册土木工程师(水利水电)基础"],
    "deadline": "2026-11-05"
  },
  {
    "title": "全国社保系统安全运维主管",
    "organization": "人力资源和社会保障部信息中心",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "本科及以上",
    "major_requirements": ["计算机科学与技术", "信息管理与信息系统"],
    "cert_requirements": ["CET-6", "信息系统项目管理师(软考高级)"],
    "deadline": "2026-11-01"
  },
  {
    "title": "地区外交业务与双边领事随员",
    "organization": "中华人民共和国外交部地区业务司",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "本科及以上",
    "major_requirements": ["国际关系", "外交学", "俄语", "法语"],
    "cert_requirements": ["专业外语等级证书(TEM-8或俄语专八)", "法律职业资格证书(A证)优先"],
    "deadline": "2026-11-01"
  },
  {
    "title": "卫生健康立法与卫生监督指导岗",
    "organization": "国家卫生健康委员会法规司",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["卫生法学", "预防医学", "公共卫生"],
    "cert_requirements": ["CET-6", "法律职业资格证书(A证)"],
    "deadline": "2026-11-05"
  },
  {
    "title": "电力系统安全与防汛减灾监管专员",
    "organization": "国家能源局电力安全监管司",
    "category": "CIVIL_SERVANT",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["电气工程及其自动化", "高电压与绝缘技术"],
    "cert_requirements": ["CET-6", "注册电气工程师(发输变电)基础"],
    "deadline": "2026-11-05"
  }
]

enterprise_jobs = [
  {
    "title": "云原生微服务后端开发专家",
    "organization": "腾讯科技 (深圳) 有限公司",
    "category": "ENTERPRISE",
    "location": "深圳",
    "education_requirement": "本科及以上",
    "major_requirements": ["计算机科学与技术", "软件工程", "网络工程"],
    "cert_requirements": ["CET-6", "系统架构设计师(软考高级)"],
    "deadline": "2026-12-15"
  },
  {
    "title": "海量数据分析与检索平台研发工程师",
    "organization": "阿里巴巴 (中国) 网络技术有限公司",
    "category": "ENTERPRISE",
    "location": "杭州",
    "education_requirement": "本科及以上",
    "major_requirements": ["计算机科学与技术", "数据科学与大数据技术", "应用数学"],
    "cert_requirements": ["CET-6", "软件设计师(软考中级)"],
    "deadline": "2026-12-20"
  },
  {
    "title": "欧拉操作系统内核研发工程师",
    "organization": "华为技术有限公司",
    "category": "ENTERPRISE",
    "location": "深圳",
    "education_requirement": "本科及以上",
    "major_requirements": ["计算机科学与技术", "电子科学与技术", "自动化"],
    "cert_requirements": ["CET-6", "嵌入式系统设计师(软考中级)"],
    "deadline": "2026-12-30"
  },
  {
    "title": "移动端客户端架构师 (Android/iOS)",
    "organization": "字节跳动科技有限公司",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "本科及以上",
    "major_requirements": ["计算机科学与技术", "软件工程", "数字媒体技术"],
    "cert_requirements": ["CET-6", "全国计算机等级考试四级"],
    "deadline": "2026-12-15"
  },
  {
    "title": "无人配送车底盘控制算法专家",
    "organization": "美团",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["机械工程", "机器人工程", "车辆工程"],
    "cert_requirements": ["CET-6", "自动控制理论高级工程师水平认证"],
    "deadline": "2026-12-10"
  },
  {
    "title": "多模态大模型评测与训练对齐工程师",
    "organization": "百度在线网络技术有限公司",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["人工智能", "智能科学与技术", "模式识别与智能系统"],
    "cert_requirements": ["CET-6 (500分及以上)", "软件设计师(软考中级)"],
    "deadline": "2026-12-25"
  },
  {
    "title": "全球供应链智能调度算法工程师",
    "organization": "京东集团",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["运筹学", "工业工程", "物流工程"],
    "cert_requirements": ["CET-6", "供应链管理专业证书(CSCP/CPIM)"],
    "deadline": "2026-12-15"
  },
  {
    "title": "金融级数字风控合规技术专家",
    "organization": "蚂蚁科技集团",
    "category": "ENTERPRISE",
    "location": "杭州",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["金融科技", "信息安全", "法学"],
    "cert_requirements": ["CET-6", "法律职业资格证书(A证)"],
    "deadline": "2026-12-18"
  },
  {
    "title": "次世代3D游戏引擎研发工程师",
    "organization": "网易集团",
    "category": "ENTERPRISE",
    "location": "广州",
    "education_requirement": "本科及以上",
    "major_requirements": ["计算机科学与技术", "软件工程", "数字媒体艺术"],
    "cert_requirements": ["CET-6", "系统分析师(软考高级)"],
    "deadline": "2026-12-30"
  },
  {
    "title": "超大规模流媒体音视频编解码算法工程师",
    "organization": "快手科技",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["通信与信息系统", "电子与通信工程", "计算机应用技术"],
    "cert_requirements": ["CET-6", "通信工程师(中级)"],
    "deadline": "2026-12-20"
  },
  {
    "title": "澎湃OS底层驱动开发工程师",
    "organization": "小米集团",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "本科及以上",
    "major_requirements": ["电子科学与技术", "微电子科学与工程", "软件工程"],
    "cert_requirements": ["CET-6", "嵌入式系统设计师(软考中级)"],
    "deadline": "2026-12-10"
  },
  {
    "title": "高并发微服务治理与SRE可靠性工程师",
    "organization": "哔哩哔哩",
    "category": "ENTERPRISE",
    "location": "上海",
    "education_requirement": "本科及以上",
    "major_requirements": ["计算机科学与技术", "网络工程", "软件工程"],
    "cert_requirements": ["CET-6", "网络规划设计师(软考高级)"],
    "deadline": "2026-12-15"
  },
  {
    "title": "5G-A网络核心网架构研发工程师",
    "organization": "中国移动通信集团有限公司",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["信息与通信工程", "通信工程", "电子信息"],
    "cert_requirements": ["CET-6", "通信专业技术人员职业水平证书(中级)"],
    "deadline": "2026-12-31"
  },
  {
    "title": "天翼云分布式对象存储研发工程师",
    "organization": "中国电信集团有限公司",
    "category": "ENTERPRISE",
    "location": "广州",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["计算机系统结构", "软件工程", "网络空间安全"],
    "cert_requirements": ["CET-6", "系统架构设计师(软考高级)"],
    "deadline": "2026-12-31"
  },
  {
    "title": "智慧联通大数据中台研发架构师",
    "organization": "中国联合网络通信集团有限公司",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["数据科学与大数据技术", "计算机应用技术"],
    "cert_requirements": ["CET-6", "软件设计师(软考中级)"],
    "deadline": "2026-12-31"
  },
  {
    "title": "核心银行分布式交易系统开发工程师",
    "organization": "中国工商银行软件开发中心",
    "category": "ENTERPRISE",
    "location": "珠海",
    "education_requirement": "本科及以上",
    "major_requirements": ["计算机科学与技术", "金融工程", "软件工程"],
    "cert_requirements": ["CET-6", "全国计算机等级考试四级或软考中级"],
    "deadline": "2026-12-25"
  },
  {
    "title": "分布式数据库GaussDB内核调优工程师",
    "organization": "建信金融科技有限责任公司",
    "category": "ENTERPRISE",
    "location": "上海",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["计算机软件与理论", "网络空间安全"],
    "cert_requirements": ["CET-6", "数据库系统工程师(软考中级)"],
    "deadline": "2026-12-25"
  },
  {
    "title": "云原生数据中心灾备与网络安全主管",
    "organization": "中国农业银行数据中心",
    "category": "ENTERPRISE",
    "location": "上海",
    "education_requirement": "本科及以上",
    "major_requirements": ["信息安全", "网络工程", "保密技术"],
    "cert_requirements": ["CET-6", "信息安全工程师(软考中级)或CISP"],
    "deadline": "2026-12-25"
  },
  {
    "title": "跨境贸易融资与反洗钱合规审查专员",
    "organization": "中国银行总行",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["国际法学", "国际金融", "民商法学"],
    "cert_requirements": ["CET-6", "法律职业资格证书(A证)"],
    "deadline": "2026-12-30"
  },
  {
    "title": "量化信贷风险计量模型开发工程师",
    "organization": "交通银行信用卡中心",
    "category": "ENTERPRISE",
    "location": "上海",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["数理统计学", "金融工程", "数量经济学"],
    "cert_requirements": ["CET-6", "金融风险管理师(FRM)"],
    "deadline": "2026-12-20"
  },
  {
    "title": "特高压数字电网调度算法专家",
    "organization": "国家电网有限公司数字化部",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["电力系统及其自动化", "电气工程", "控制科学与工程"],
    "cert_requirements": ["CET-6", "注册电气工程师(发输变电)基础"],
    "deadline": "2026-12-31"
  },
  {
    "title": "电网实时态势感知与人工智能调度工程师",
    "organization": "中国南方电网有限责任公司",
    "category": "ENTERPRISE",
    "location": "广州",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["电气工程及其自动化", "模式识别与智能系统"],
    "cert_requirements": ["CET-6", "注册电气工程师(供配电)基础"],
    "deadline": "2026-12-31"
  },
  {
    "title": "载人航天器控制系统嵌入式软件研发岗",
    "organization": "中国航天科技集团有限公司第五研究院",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["航空宇航科学与技术", "控制理论与控制工程", "软件工程"],
    "cert_requirements": ["CET-6", "嵌入式系统设计师(软考中级)"],
    "deadline": "2026-12-31"
  },
  {
    "title": "先进隐身战机飞控律算法设计专家",
    "organization": "中国航空工业集团有限公司成都飞机设计研究所",
    "category": "ENTERPRISE",
    "location": "成都",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["飞行器设计", "导航、制导与控制", "力学"],
    "cert_requirements": ["CET-6 (500分及以上)", "全国计算机等级考试四级"],
    "deadline": "2026-12-31"
  },
  {
    "title": "油气管网智慧物联网系统研发专员",
    "organization": "中国石油天然气集团有限公司",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "本科及以上",
    "major_requirements": ["油气储运工程", "物联网工程", "自动化"],
    "cert_requirements": ["CET-4", "注册安全工程师(初级及以上)"],
    "deadline": "2026-12-20"
  },
  {
    "title": "高端合成树脂工业控制软件研发工程师",
    "organization": "中国石油化工集团有限公司",
    "category": "ENTERPRISE",
    "location": "上海",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["化学工程与技术", "控制工程", "过程装备与控制工程"],
    "cert_requirements": ["CET-6", "注册化工工程师基础"],
    "deadline": "2026-12-25"
  },
  {
    "title": "第四代核反应堆数字化仪表控系统工程师",
    "organization": "中国核工业集团有限公司",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["核能科学与工程", "辐射防护与环境保护", "核工程与核技术"],
    "cert_requirements": ["CET-6", "注册核安全工程师具备报考资格"],
    "deadline": "2026-12-31"
  },
  {
    "title": "大型邮轮综合电力推进控制软件工程师",
    "organization": "中国船舶集团有限公司上海外高桥造船",
    "category": "ENTERPRISE",
    "location": "上海",
    "education_requirement": "本科及以上",
    "major_requirements": ["船舶与海洋工程", "轮机工程", "电气工程"],
    "cert_requirements": ["CET-6", "全国造船工程技术人员认证"],
    "deadline": "2026-12-20"
  },
  {
    "title": "大型机场交通枢纽智能BIM研发主管",
    "organization": "中国建筑集团有限公司第三工程局",
    "category": "ENTERPRISE",
    "location": "武汉",
    "education_requirement": "本科及以上",
    "major_requirements": ["土木工程", "建筑学", "工程管理"],
    "cert_requirements": ["CET-4", "一级建造师(建筑工程或市政公用)"],
    "deadline": "2026-12-20"
  },
  {
    "title": "高速铁路列控通信信号研发工程师",
    "organization": "中国中铁电气化局集团有限公司",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "本科及以上",
    "major_requirements": ["铁道信号与控制", "自动化", "通信工程"],
    "cert_requirements": ["CET-4", "铁路信号工程师(中级水平)"],
    "deadline": "2026-12-20"
  },
  {
    "title": "军工关键基础设施零信任安全架构师",
    "organization": "中国电子科技集团公司第三十研究所",
    "category": "ENTERPRISE",
    "location": "成都",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["密码学", "网络空间安全", "计算机科学与技术"],
    "cert_requirements": ["CET-6", "信息安全工程师(软考中级)"],
    "deadline": "2026-12-31"
  },
  {
    "title": "银河麒麟操作系统内核适配专家",
    "organization": "中国电子信息产业集团有限公司",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "本科及以上",
    "major_requirements": ["计算机科学与技术", "软件工程", "信息工程"],
    "cert_requirements": ["CET-6", "系统架构设计师(软考高级)"],
    "deadline": "2026-12-31"
  },
  {
    "title": "消费品跨国并购与国际税务主管",
    "organization": "华润 (集团) 有限公司",
    "category": "ENTERPRISE",
    "location": "深圳",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["会计学", "财政税收学", "审计学"],
    "cert_requirements": ["CET-6", "注册会计师(CPA)或ACCA"],
    "deadline": "2026-12-20"
  },
  {
    "title": "全球干散货航运海商法务高级经理",
    "organization": "招商局能源运输股份有限公司",
    "category": "ENTERPRISE",
    "location": "上海",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["海商法学", "国际经济法学", "民商法学"],
    "cert_requirements": ["CET-6 (520分以上)", "法律职业资格证书(A证)"],
    "deadline": "2026-12-25"
  },
  {
    "title": "全球大宗农产品国际期货风控总监助理",
    "organization": "中粮集团有限公司",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["金融工程", "农业经济管理", "国际贸易"],
    "cert_requirements": ["CET-6", "期货从业资格及CFA二级"],
    "deadline": "2026-12-20"
  },
  {
    "title": "商业地产REITs公募基金运作法务主管",
    "organization": "中国保利集团有限公司",
    "category": "ENTERPRISE",
    "location": "广州",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["经济法学", "金融学", "房地产经济学"],
    "cert_requirements": ["CET-6", "法律职业资格证书(A证)"],
    "deadline": "2026-12-20"
  },
  {
    "title": "医疗大健康器械海外合规注册认证经理",
    "organization": "中国通用技术 (集团) 控股有限责任公司",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["生物医学工程", "药事管理", "医学英语"],
    "cert_requirements": ["CET-6 (TEM-8优先)", "执业药师或医疗器械注册专员证书"],
    "deadline": "2026-12-20"
  },
  {
    "title": "时速450公里CR450高铁牵引控制软件专家",
    "organization": "中国中车青岛四方机车车辆股份有限公司",
    "category": "ENTERPRISE",
    "location": "青岛",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["电气工程及其自动化", "电力电子与电力传动", "载运工具运用工程"],
    "cert_requirements": ["CET-6", "系统架构设计师(软考高级)"],
    "deadline": "2026-12-31"
  },
  {
    "title": "央企战略新兴产业战略投资总监助理",
    "organization": "国家开发投资集团有限公司",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["应用经济学", "工商管理(MBA)", "计算机科学与技术"],
    "cert_requirements": ["CET-6", "特许金融分析师(CFA)或保荐代表人胜任能力"],
    "deadline": "2026-12-25"
  },
  {
    "title": "硬科技半导体行业投资银行部分析师",
    "organization": "中国国际金融股份有限公司 (中金公司)",
    "category": "ENTERPRISE",
    "location": "上海",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["微电子科学与工程", "金融学", "会计学"],
    "cert_requirements": ["CET-6", "证券从业资格与CPA通过3科及以上"],
    "deadline": "2026-12-30"
  },
  {
    "title": "证券衍生品柜台交易业务法务合规专员",
    "organization": "中信证券股份有限公司",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["金融法学", "民商法学", "国际法学"],
    "cert_requirements": ["CET-6", "法律职业资格证书(A证)"],
    "deadline": "2026-12-30"
  },
  {
    "title": "高频算法交易系统C++核心架构师",
    "organization": "华泰证券股份有限公司",
    "category": "ENTERPRISE",
    "location": "南京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["计算机应用技术", "应用物理学", "计算数学"],
    "cert_requirements": ["CET-6", "系统分析师(软考高级)"],
    "deadline": "2026-12-25"
  },
  {
    "title": "手机银行超级App前端架构优化专家",
    "organization": "招商银行股份有限公司总行信息技术部",
    "category": "ENTERPRISE",
    "location": "深圳",
    "education_requirement": "本科及以上",
    "major_requirements": ["软件工程", "计算机科学与技术", "网络工程"],
    "cert_requirements": ["CET-6", "软件设计师(软考中级)"],
    "deadline": "2026-12-20"
  },
  {
    "title": "零售信贷实时风控流计算系统研发专家",
    "organization": "平安银行股份有限公司",
    "category": "ENTERPRISE",
    "location": "深圳",
    "education_requirement": "本科及以上",
    "major_requirements": ["数据科学与大数据技术", "计算机应用技术"],
    "cert_requirements": ["CET-6", "信息系统项目管理师(软考高级)"],
    "deadline": "2026-12-20"
  },
  {
    "title": "新能源汽车三电电控底层嵌入式系统专家",
    "organization": "比亚迪股份有限公司",
    "category": "ENTERPRISE",
    "location": "深圳",
    "education_requirement": "本科及以上",
    "major_requirements": ["车辆工程", "汽车电子工程", "控制科学与工程"],
    "cert_requirements": ["CET-6", "自动驾驶汽车系统认证工程师"],
    "deadline": "2026-12-20"
  },
  {
    "title": "端到端自动驾驶感知决策算法研究员",
    "organization": "上海汽车集团股份有限公司人工智能实验室",
    "category": "ENTERPRISE",
    "location": "上海",
    "education_requirement": "博士研究生或优秀硕士",
    "major_requirements": ["计算机视觉", "控制工程", "自动化", "机器学习"],
    "cert_requirements": ["CET-6", "全国计算机等级考试四级人工智能"],
    "deadline": "2026-12-25"
  },
  {
    "title": "新一代固态锂电池BMS热失控预警算法专家",
    "organization": "宁德时代新能源科技股份有限公司",
    "category": "ENTERPRISE",
    "location": "宁德",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["电化学", "材料物理与化学", "仪器科学与技术"],
    "cert_requirements": ["CET-6", "注册电化学工程师专业背景"],
    "deadline": "2026-12-30"
  },
  {
    "title": "工业级巡检无人机抗干扰通信链路架构师",
    "organization": "深圳大疆创新科技有限公司",
    "category": "ENTERPRISE",
    "location": "深圳",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["电磁场与微波技术", "通信工程", "信息工程"],
    "cert_requirements": ["CET-6", "射频与天线技术高级工程师认证"],
    "deadline": "2026-12-20"
  },
  {
    "title": "混合AI超算异构集群集群管理研发专家",
    "organization": "联想集团",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["计算机系统结构", "并行计算", "软件工程"],
    "cert_requirements": ["CET-6", "网络规划设计师(软考高级)"],
    "deadline": "2026-12-20"
  },
  {
    "title": "通用大模型基础视觉感知算法资深研究员",
    "organization": "北京市商汤科技开发有限公司",
    "category": "ENTERPRISE",
    "location": "北京",
    "education_requirement": "硕士研究生及以上",
    "major_requirements": ["模式识别与智能系统", "计算机应用技术", "计算数学"],
    "cert_requirements": ["CET-6", "全国计算机等级考试四级"],
    "deadline": "2026-12-30"
  }
]

all_jobs = civil_servant_jobs + enterprise_jobs
print(f"Total: {len(all_jobs)} (Civil: {len(civil_servant_jobs)}, Enterprise: {len(enterprise_jobs)})")

# Save JSON file
with open("jobs.json", "w", encoding="utf-8") as f:
    json.dump(all_jobs, f, ensure_ascii=False, indent=2)

# Save CSV file for Supabase
with open("supabase_jobs.csv", "w", encoding="utf-8", newline="") as f:
    writer = csv.writer(f)
    writer.writerow([
        "title",
        "organization",
        "category",
        "location",
        "education_requirement",
        "major_requirements",
        "cert_requirements",
        "deadline"
    ])
    for job in all_jobs:
        writer.writerow([
            job["title"],
            job["organization"],
            job["category"],
            job["location"],
            job["education_requirement"],
            "{" + ",".join(f'\"{m}\"' for m in job["major_requirements"]) + "}",
            "{" + ",".join(f'\"{c}\"' for c in job["cert_requirements"]) + "}",
            job["deadline"]
        ])

print("Both jobs.json and supabase_jobs.csv generated successfully!")
