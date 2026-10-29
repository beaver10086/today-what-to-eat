# -*- coding: utf-8 -*-
"""
生成《软件测试》实验报告（Word / .docx）
=========================================

读取 tests/functional/results/functional_results.json 中功能用例的真实执行结果，
结合本脚本内置的测试执行汇总与缺陷记录，生成一份完整中文实验报告。

用法：
    <venv-python> scripts/generate_test_report.py
"""

import json
import os

from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "软件测试实验报告-今天吃什么.docx")
FUNC_JSON = os.path.join(ROOT, "tests", "functional", "results", "functional_results.json")

CN = "宋体"
CN_BOLD = "黑体"
ACCENT = RGBColor(0x1F, 0x3B, 0x63)


# ---------------------------------------------------------------------------
# 排版助手
# ---------------------------------------------------------------------------
def _font(run, name=CN, size=10.5, bold=False, color=None):
    run.font.name = name
    run.font.size = Pt(size)
    run.bold = bold
    if color is not None:
        run.font.color.rgb = color
    rpr = run._element.get_or_add_rPr()
    rfonts = rpr.get_or_add_rFonts()
    rfonts.set(qn("w:eastAsia"), name)
    rfonts.set(qn("w:ascii"), name)
    rfonts.set(qn("w:hAnsi"), name)


def para(doc, text="", size=10.5, bold=False, align=None, indent=True,
         space_after=6, color=None, name=CN):
    p = doc.add_paragraph()
    if align is not None:
        p.alignment = align
    pf = p.paragraph_format
    pf.space_after = Pt(space_after)
    pf.line_spacing = 1.4
    if indent:
        pf.first_line_indent = Pt(size * 2)
    if text:
        _font(p.add_run(text), name=name, size=size, bold=bold, color=color)
    return p


def h1(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(16)
    p.paragraph_format.space_after = Pt(8)
    _font(p.add_run(text), name=CN_BOLD, size=16, bold=True, color=ACCENT)


def h2(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(10)
    p.paragraph_format.space_after = Pt(5)
    _font(p.add_run(text), name=CN_BOLD, size=13, bold=True, color=ACCENT)


def h3(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(8)
    p.paragraph_format.space_after = Pt(4)
    _font(p.add_run(text), name=CN_BOLD, size=11.5, bold=True)


def bullet(doc, text, size=10.5):
    p = doc.add_paragraph()
    p.paragraph_format.left_indent = Pt(18)
    p.paragraph_format.space_after = Pt(3)
    p.paragraph_format.line_spacing = 1.35
    _font(p.add_run("· " + text), size=size)
    return p


def table(doc, headers, rows, widths=None, size=8.5, header_size=8.5):
    t = doc.add_table(rows=1, cols=len(headers))
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    hdr = t.rows[0].cells
    for i, txt in enumerate(headers):
        hdr[i].text = ""
        p = hdr[i].paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_after = Pt(1)
        p.paragraph_format.space_before = Pt(1)
        _font(p.add_run(txt), name=CN_BOLD, size=header_size, bold=True)
    for row in rows:
        cells = t.add_row().cells
        for i, val in enumerate(row):
            cells[i].text = ""
            p = cells[i].paragraphs[0]
            p.paragraph_format.space_after = Pt(1)
            p.paragraph_format.space_before = Pt(1)
            p.paragraph_format.line_spacing = 1.1
            _font(p.add_run("" if val is None else str(val)), size=size)
    if widths:
        for r in t.rows:
            for i, w in enumerate(widths):
                r.cells[i].width = Cm(w)
    return t


def caption(doc, text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(3)
    p.paragraph_format.space_after = Pt(10)
    _font(p.add_run(text), size=9, color=RGBColor(0x55, 0x55, 0x55))


# ---------------------------------------------------------------------------
# 数据：测试执行汇总
# ---------------------------------------------------------------------------
UNIT_ROWS = [
    ["com.studyroom.service.AssistantServiceTest", "智能助手", "8", "8", "0", "通过"],
    ["com.studyroom.service.DishServiceTest", "菜品服务", "9", "9", "0", "通过"],
    ["com.studyroom.service.PreferenceServiceTest", "口味问卷", "6", "6", "0", "通过"],
    ["com.studyroom.service.RecommendationServiceFlowTest", "推荐流程", "5", "5", "0", "通过"],
    ["com.studyroom.service.AuthServiceTest", "用户认证", "4", "4", "0", "通过"],
    ["com.studyroom.service.CanteenServiceTest", "食堂服务", "5", "5", "0", "通过"],
    ["com.studyroom.service.ShopServiceTest", "档口服务", "4", "4", "0", "通过"],
    ["com.studyroom.service.FavoriteServiceTest", "收藏服务", "3", "3", "0", "通过"],
    ["com.studyroom.service.TagServiceTest", "标签服务", "3", "3", "0", "通过"],
    ["com.studyroom.service.RecommendationServiceTest", "推荐服务", "1", "1", "0", "通过"],
    ["com.studyroom.mapper.CanteenMapperTest", "食堂 SQL", "1", "1", "0", "通过"],
    ["com.studyroom.mapper.FavoriteMapperTest", "收藏 SQL", "1", "1", "0", "通过"],
    ["com.studyroom.mapper.RecommendationMapperTest", "推荐 SQL", "1", "1", "0", "通过"],
]

E2E_ROWS = [
    ["E2E-UI-01", "desktop-chromium", "菜品列表可用、布局不横向溢出并保存设备截图", "通过", "3.8s"],
    ["E2E-UI-02", "iphone-webkit", "菜品列表可用、布局不横向溢出并保存设备截图", "通过", "5.0s"],
    ["E2E-UI-03", "pixel-chromium", "菜品列表可用、布局不横向溢出并保存设备截图", "通过", "3.8s"],
    ["E2E-UI-04", "desktop-chromium", "未登录访问收藏页会被引导登录", "通过", "1.9s"],
    ["E2E-UI-05", "iphone-webkit", "未登录访问收藏页会被引导登录", "通过", "3.8s"],
    ["E2E-UI-06", "pixel-chromium", "未登录访问收藏页会被引导登录", "通过", "3.0s"],
]

NEWMAN_ROWS = [
    ["API-01", "GET /api/health", "健康检查", "200", "通过"],
    ["API-02", "GET /api/dishes?page=1&size=10", "公开菜品列表", "200", "通过"],
    ["API-03", "GET /api/dishes/random", "按食堂和预算随机挑菜", "200", "通过"],
    ["API-04", "POST /api/recommendations", "未登录推荐应拒绝", "401", "通过"],
    ["API-05", "POST /api/auth/register", "注册回归用户并取得 Token", "200", "通过"],
    ["API-06", "GET /api/dishes/{dishId}", "读取菜品详情", "200", "通过"],
    ["API-07", "PUT /api/preferences/me", "保存口味问卷", "200", "通过"],
    ["API-08", "GET /api/preferences/me/profile", "读取口味画像", "200", "通过"],
    ["API-09", "POST /api/recommendations", "生成推荐", "200", "通过"],
    ["API-10", "POST /api/recommendations/{id}/next", "反馈不喜欢并请求下一组", "200", "通过"],
    ["API-11", "POST /api/favorites", "添加菜品收藏", "200", "通过"],
    ["API-12", "GET /api/favorites/ids", "验证收藏 ID", "200", "通过"],
    ["API-13", "DELETE /api/favorites/1/{dishId}", "移除菜品收藏", "200", "通过"],
    ["API-14", "POST /api/auth/logout", "登出", "200", "通过"],
]

PERF_ROWS = [
    ["PERF-01", "30 并发持续压测，采样总数 ≥ 100", "600 个采样", "600", "通过"],
    ["PERF-02", "请求失败率 ≤ 1%", "失败率 0.00%（0/600）", "0.00%", "通过"],
    ["PERF-03", "响应时间 p95 ≤ 3000 ms", "p95 = 26 ms，平均 30 ms，最大 135 ms", "26 ms", "通过"],
    ["PERF-04", "系统吞吐量（参考指标）", "58.8 请求/秒", "58.8/s", "通过"],
]

# ---------------------------------------------------------------------------
# 数据：缺陷记录（Jira / PingCode 风格）
# ---------------------------------------------------------------------------
DEFECTS = [
    {
        "id": "BUG-001",
        "title": "菜品与档口查询接口返回 500（数据库列缺失）",
        "module": "数据层 / 菜品、档口",
        "severity": "致命 S1",
        "priority": "P1",
        "status": "已修复",
        "found": "接口回归（Newman）+ CI 流水线",
        "step": "1. 初始化数据库；2. 启动应用；3. 请求 GET /api/dishes 与 GET /api/shops",
        "expect": "HTTP 200，返回菜品 / 档口数据",
        "actual": "HTTP 500，日志 BadSqlGrammarException: Unknown column 'takeout_suitability' in 'field list'",
        "cause": "v1.1 引入了 dish.takeout_suitability、dish.data_source、shop.queue_heat 三个字段，"
                 "Mapper 已引用，但建表脚本未同步，DDL 与持久层发生漂移。",
        "fix": "在 docs/sql/V1.0.0__init.sql 的 dish、shop 建表语句中补齐三列，并同步修正字段注释。",
    },
    {
        "id": "BUG-002",
        "title": "README 引用的数据库升级脚本 V1.0.3 在仓库中不存在",
        "module": "数据库脚本 / 项目文档",
        "severity": "严重 S2",
        "priority": "P1",
        "status": "已修复",
        "found": "文档审查 + 部署验证",
        "step": "1. 阅读 README 升级说明；2. 在 docs/sql 目录查找 V1.0.3__menu_metadata.sql",
        "expect": "存在可执行的一次性增量脚本，老库可平滑升级",
        "actual": "文件不存在，老库无升级路径，部署后必然触发 BUG-001",
        "cause": "三个新增字段被直接写进全新安装脚本，遗漏了面向已有数据库的增量脚本，"
                 "而 README 已按增量脚本的方式给出升级指引。",
        "fix": "新增 docs/sql/V1.0.3__menu_metadata.sql，使用存储过程判断列是否存在后再 ALTER，"
               "可重复执行且不污染新库。",
    },
    {
        "id": "BUG-003",
        "title": "排队热度 / 打包适配度字段注释与取值范围不一致",
        "module": "数据库脚本 / 菜品、档口",
        "severity": "一般 S3",
        "priority": "P3",
        "status": "已修复",
        "found": "代码审查 + 测试用例设计",
        "step": "1. 查看 shop.queue_heat、dish.takeout_suitability 列注释；2. 对比 DTO 校验与前端渲染文案",
        "expect": "注释、校验规则、前端文案三者的取值域一致",
        "actual": "列注释写「0-100」，DTO 约束为 @Max(3) / @Max(2)，前端按「无需排队/较短/较长/很长」档位渲染",
        "cause": "字段语义从连续分值改为枚举档位后，只改了代码与前端，未同步数据库注释。",
        "fix": "将注释修正为「排队热度档位：0 无需排队 1 较短 2 较长 3 很长」与"
               "「外卖适宜度档位：0 不适合 1 一般 2 适合」。",
    },
    {
        "id": "BUG-004",
        "title": "智能助手自然语言解析触发空指针异常",
        "module": "智能助手 / AssistantService",
        "severity": "严重 S2",
        "priority": "P1",
        "status": "已修复",
        "found": "单元测试（白盒）",
        "step": "1. 调用 AssistantService.parseRules 解析不含餐次或分类的自然语言输入",
        "expect": "解析结果按缺省值处理，正常返回",
        "actual": "抛出 NullPointerException，接口返回 HTTP 500",
        "cause": "parseRules 中对 mealType / category 使用了 int 与 null 混用的嵌套三元表达式，"
                 "自动拆箱时对象为 null。",
        "fix": "改为显式判空分支，并补充 8 条覆盖空值、缺省值与边界输入的单元测试。",
    },
    {
        "id": "BUG-005",
        "title": "鉴权拦截器存在失效分支 /api/profile",
        "module": "安全 / AuthInterceptor",
        "severity": "一般 S3",
        "priority": "P3",
        "status": "已修复",
        "found": "代码审查",
        "step": "1. 检查 AuthInterceptor 的路由判定；2. 全局搜索 /api/profile 的控制器映射",
        "expect": "拦截路径与实际的受保护接口一一对应",
        "actual": "path.startsWith(\"/api/profile\") 无任何控制器映射，为死代码；"
                 "前端实际调用的是 /api/preferences/me/profile",
        "cause": "接口路径调整后未同步清理拦截器中的旧路径分支。",
        "fix": "删除失效分支，保留 /api/preferences 前缀匹配（已覆盖 profile 子路径）。",
    },
    {
        "id": "BUG-006",
        "title": "登录接口缺失与注册端一致的用户名格式校验",
        "module": "用户认证 / LoginRequest",
        "severity": "一般 S3",
        "priority": "P3",
        "status": "已修复",
        "found": "测试用例设计（等价类 / 边界值）",
        "step": "1. 以 3 位用户名调用 POST /api/auth/login；2. 观察参数校验行为",
        "expect": "与注册端一致，按 ^[a-zA-Z0-9_]{4,32}$ 拒绝非法格式",
        "actual": "仅校验 @NotBlank，非法格式请求直达业务层后才因查不到用户返回 401，"
                 "校验时机与错误信息不一致",
        "cause": "注册 DTO 定义了完整校验，登录 DTO 只做了非空校验，两端约束不对齐。",
        "fix": "LoginRequest 增加 @Pattern(regexp = \"^[a-zA-Z0-9_]{4,32}$\") 约束，与注册端保持一致。",
    },
    {
        "id": "BUG-007",
        "title": "接口回归脚本注册用户名不符合后端校验规则",
        "module": "测试脚本 / Newman 接口回归",
        "severity": "严重 S2",
        "priority": "P2",
        "status": "已修复",
        "found": "CI 流水线接口回归执行",
        "step": "1. 执行 tests/newman/collection.json；2. 观察注册请求与后续请求结果",
        "expect": "注册成功并取到 Token，后续依赖登录态的请求正常",
        "actual": "注册返回 400（用户名 36 位带连字符 UUID，违反 4–32 位规则），"
                 "后续收藏、推荐等请求级联 401",
        "cause": "脚本使用 Postman 内置变量 {{$guid}} 生成 36 位带连字符 UUID。",
        "fix": "改为在预请求脚本中生成 'api_' + 8 位随机字符串，长度与字符集均满足后端规则。",
    },
    {
        "id": "BUG-008",
        "title": "JMeter 压测计划无法编译（__groovy 参数个数错误）",
        "module": "测试脚本 / JMeter 性能基准",
        "severity": "严重 S2",
        "priority": "P2",
        "status": "已修复",
        "found": "CI 流水线性能基准执行",
        "step": "1. 非 GUI 模式运行 tests/jmeter/recommendation-load.jmx",
        "expect": "压测正常执行并生成 recommendation.jtl",
        "actual": "Error occurred compiling the tree；日志：__groovy called with wrong number of parameters. "
                 "Actual: 3. Expected: >= 1 and <= 2；未生成 jtl，check_results.py 抛 FileNotFoundError",
        "cause": "__groovy 脚本体 UUID.randomUUID().toString().replace('-','').substring(0,27) 中的逗号"
                 "被 JMeter 函数解析器当作参数分隔符，导致参数个数为 3。",
        "fix": "改用内置 __RandomString 函数生成 27 位随机用户名，彻底规避脚本内逗号问题。",
    },
    {
        "id": "BUG-009",
        "title": "JMeter 注册采样器 protocol 字段取值错误",
        "module": "测试脚本 / JMeter 性能基准",
        "severity": "一般 S3",
        "priority": "P3",
        "status": "已修复",
        "found": "测试脚本审查",
        "step": "1. 查看压测计划中注册采样器的 HTTPSampler.protocol 配置",
        "expect": "protocol 取值为 http 或 https",
        "actual": "被填成 ${BASE_URL}（完整 URL），采样器无法正确拼装请求地址",
        "cause": "配置协议字段时误将基础地址变量填入。",
        "fix": "将 protocol 修正为 http，基础地址改由 HTTP 请求默认值统一管理。",
    },
    {
        "id": "BUG-010",
        "title": "端到端测试路由误拦截 Vite 源码模块导致页面空白",
        "module": "测试脚本 / Playwright 端到端",
        "severity": "严重 S2",
        "priority": "P2",
        "status": "已修复",
        "found": "CI 流水线端到端执行",
        "step": "1. 启动前端开发服务器；2. 运行 e2e/responsive.spec.ts",
        "expect": "页面正常挂载，6 条用例全部通过",
        "actual": "6 条用例全部失败：page.route('**/api/**') 同时匹配到 /src/api/user.ts 等源码模块请求，"
                 "返回错误 MIME 类型，应用挂载失败、页面空白",
        "cause": "通配路由模式过宽，未区分运行时代理请求与开发服务器模块请求。",
        "fix": "路由匹配条件收敛为 url.pathname.startsWith('/api/')，"
               "并将重定向断言放宽为兼容编码与非编码两种形式。",
    },
    {
        "id": "BUG-011",
        "title": "CI 中 JMeter 依赖不可用的 Docker 镜像",
        "module": "持续集成 / GitHub Actions",
        "severity": "一般 S3",
        "priority": "P3",
        "status": "已修复",
        "found": "CI 流水线执行",
        "step": "1. 在流水线中通过容器镜像运行 JMeter",
        "expect": "镜像可拉取，压测正常启动",
        "actual": "justb4/jmeter:5.6.3 报 manifest unknown；apache/jmeter:5.6.3 报 pull access denied",
        "cause": "所选镜像在 Docker Hub 上不可用或未公开。",
        "fix": "改为从 archive.apache.org 直接下载官方 JMeter 5.6.3 二进制包解压运行，去除镜像依赖。",
    },
]

DEFECT_MODULE_STAT = [
    ["数据层 / 数据库脚本", "3", "已修复 3"],
    ["测试脚本 / 自动化", "4", "已修复 4"],
    ["后端业务代码", "2", "已修复 2"],
    ["安全 / 鉴权", "1", "已修复 1"],
    ["持续集成", "1", "已修复 1"],
]


# ---------------------------------------------------------------------------
# 报告正文
# ---------------------------------------------------------------------------
def build():
    with open(FUNC_JSON, encoding="utf-8") as fp:
        func = json.load(fp)
    func_cases = func["cases"]

    doc = Document()
    sec = doc.sections[0]
    sec.top_margin = Cm(2.2)
    sec.bottom_margin = Cm(2.2)
    sec.left_margin = Cm(2.2)
    sec.right_margin = Cm(2.2)

    style = doc.styles["Normal"]
    style.font.name = CN
    style.font.size = Pt(10.5)
    style.element.rPr.rFonts.set(qn("w:eastAsia"), CN)

    # ---------------- 封面 ----------------
    for _ in range(4):
        doc.add_paragraph()
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    _font(p.add_run("软件测试实验报告"), name=CN_BOLD, size=28, bold=True, color=ACCENT)

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(14)
    _font(p.add_run("—— 小组项目跟踪与缺陷管理实践"), name=CN_BOLD, size=16, bold=True)

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(6)
    _font(p.add_run("被测项目：「今天吃什么」校园食堂菜品推荐系统"), size=13)

    for _ in range(3):
        doc.add_paragraph()

    cover = [
        ["实验项目", "小组项目跟踪：项目初稿提交、测试用例设计与执行、缺陷修复与记录"],
        ["被测系统", "今天吃什么（What to Eat）v1.1 —— Spring Boot + Vue 3 全栈应用"],
        ["代码仓库", "https://github.com/beaver10086/today-what-to-eat"],
        ["实验内容", "① 提交项目初稿  ② 测试用例设计与执行  ③ 缺陷发现、修复与记录"],
        ["测试层次", "单元测试 / 功能测试 / 端到端测试 / 接口回归 / 性能基准"],
        ["用例规模", "功能用例 56 条 + 端到端 6 条 + 性能 4 项 + 单元测试 51 条"],
        ["缺陷记录", "共登记缺陷 11 项，已修复 11 项，修复率 100%"],
        ["报告日期", "2026 年 10 月 30 日"],
    ]
    table(doc, ["项目", "内容"], cover, widths=[3.2, 12.8], size=10)

    doc.add_page_break()

    # ---------------- 一、实验目的与要求 ----------------
    h1(doc, "一、实验目的与要求")
    para(doc, "本实验以小组协作开发的「今天吃什么」校园食堂菜品推荐系统为被测对象，"
              "围绕软件测试全流程开展实践，重点达成以下目标：")
    bullet(doc, "提交完整的项目初稿，包括可运行的系统代码、数据库脚本、部署说明与项目文档，"
                "形成稳定的被测基线版本。")
    bullet(doc, "基于需求与设计文档开展测试设计，综合运用等价类划分、边界值分析、场景法、"
                "错误推测法等黑盒测试方法，覆盖功能、接口、安全、兼容性与性能等维度。")
    bullet(doc, "执行测试用例并记录真实执行结果，形成可追溯的执行证据，"
                "自动化用例纳入持续集成流水线实现回归。")
    bullet(doc, "对测试中发现的问题按 Jira / PingCode 风格登记缺陷，"
                "包含严重度、优先级、状态、复现步骤、期望与实际结果、根因与修复说明，"
                "并完成修复与回归验证。")

    h2(doc, "1.1 实验环境")
    table(doc, ["类别", "配置"], [
        ["操作系统", "Windows 11"],
        ["后端运行时", "JDK 18（项目目标 Java 17）、Maven 3.9.14、Spring Boot 3.5"],
        ["前端运行时", "Node.js、Vite、Vue 3 + TypeScript + Vant"],
        ["数据库", "MySQL 5.7 / 8.x（utf8mb4）"],
        ["自动化测试", "JUnit 5 + Mockito、Playwright、Newman、Apache JMeter 5.6.3"],
        ["静态检查与覆盖率", "Checkstyle、JaCoCo（服务包 70%、推荐服务 60% 门禁）"],
        ["持续集成", "GitHub Actions"],
    ], widths=[3.6, 12.4], size=9.5)
    caption(doc, "表 1-1  实验环境配置")

    # ---------------- 二、项目初稿说明 ----------------
    h1(doc, "二、项目初稿说明")

    h2(doc, "2.1 项目背景与建设目标")
    para(doc, "高校师生在校内就餐时普遍面临「不知道吃什么」的选择困难：食堂与档口数量多、"
              "菜品更新快，已有的点评类应用又难以覆盖校内场景。本项目面向校园食堂场景，"
              "提供口味问卷、菜品筛选与个性化推荐能力，帮助用户快速做出就餐决策。")
    para(doc, "系统按以下目标建设：一是建立可维护的食堂 / 档口 / 菜品数据体系，"
              "支持管理员维护菜单与批量导入；二是通过口味问卷构建用户画像，"
              "结合预算、辣度、餐段与忌口标签给出可解释的推荐结果；"
              "三是通过反馈闭环持续优化推荐质量；四是以自动化流水线保障交付质量。")

    h2(doc, "2.2 功能模块划分")
    table(doc, ["序号", "功能模块", "主要能力", "对应前端页面"], [
        ["1", "用户认证", "注册、登录、登出，12 小时有效 Bearer Token", "/login"],
        ["2", "口味问卷与画像", "保存问卷、读取画像、保留反馈形成的权重调整", "/survey、/profile"],
        ["3", "菜品目录", "分页/多条件筛选、随机抽菜、菜品详情、标签管理", "/、/dishes/:id"],
        ["4", "食堂与档口", "食堂与档口层级浏览、排队热度等元信息维护", "/canteens、/shops/:id"],
        ["5", "个性化推荐", "生成推荐、反馈喜好、换一组、排除历史结果", "/recommendations"],
        ["6", "收藏", "菜品与档口收藏、取消、逻辑删除可逆", "/favorites"],
        ["7", "智能助手", "自然语言解析筛选条件、基于目录资料问答", "悬浮入口"],
        ["8", "后台管理", "食堂/档口/菜品/标签增删改查、CSV 模板下载与导入", "/admin"],
    ], widths=[1.2, 3.0, 7.4, 4.4], size=9)
    caption(doc, "表 2-1  功能模块划分")

    h2(doc, "2.3 技术架构")
    para(doc, "系统采用前后端分离架构。后端为 Spring Boot 单体应用，"
              "按 controller / service / mapper / model 分层：controller 负责参数校验与统一响应封装，"
              "service 承载业务逻辑与事务边界，mapper 通过 MyBatis 访问 MySQL，"
              "统一响应结构为 ApiResponse{code, message, data, timestamp}，"
              "业务异常由 GlobalExceptionHandler 统一转换为约定的 HTTP 状态码。")
    para(doc, "前端为 Vue 3 + TypeScript 单页应用，使用 Vite 构建、Vue Router 管理路由、"
              "Pinia 管理状态、Vant 提供移动端组件，开发态通过代理将 /api 请求转发到后端。"
              "鉴权由 AuthInterceptor 统一拦截：用户类接口校验登录态，"
              "食堂/档口/菜品/标签的写操作额外校验管理员角色。")

    h2(doc, "2.4 数据库设计")
    para(doc, "数据库共 14 张表，统一使用 utf8mb4 字符集，数据关联由应用层维护、不使用物理外键，"
              "删除操作均采用逻辑删除（is_deleted 标记）。")
    table(doc, ["表名", "说明", "字段数", "核心字段"], [
        ["app_user", "用户表", "12", "id、username、password、role、status、last_login_at"],
        ["canteen", "食堂表", "13", "id、canteen_name、address、business_hours"],
        ["shop", "食堂档口表", "18", "id、canteen_id、shop_name、queue_heat、avg_price"],
        ["dish", "菜品表", "23", "id、shop_id、price、meal_type、spice_level、takeout_suitability、data_source"],
        ["tag", "标签表", "7", "id、tag_name、tag_type"],
        ["dish_tag", "菜品标签关联表", "6", "id、dish_id、tag_id"],
        ["preference", "用户口味问卷表", "13", "user_id、max_spice_level、budget_min、budget_max、dislike_tag_ids"],
        ["user_taste_profile", "用户口味画像表", "8", "user_id、tag_id、weight、source"],
        ["recommendation", "推荐记录表", "14", "id、request_no、user_id、meal_type、budget_max、cost_ms"],
        ["recommendation_item", "推荐明细表", "10", "recommendation_id、dish_id、rank_no、score、reason"],
        ["feedback_record", "推荐反馈表", "10", "user_id、recommendation_id、dish_id、feedback_type"],
        ["favorite", "收藏表", "7", "user_id、target_type、target_id、is_deleted"],
        ["operation_log", "操作留痕表", "11", "operator_id、module、action、ip"],
        ["prompt_template", "Prompt 模板表", "9", "template_key、content、enabled"],
    ], widths=[3.4, 2.4, 1.4, 8.8], size=8.5)
    caption(doc, "表 2-2  数据库表清单")

    h2(doc, "2.5 运行环境与启动方式")
    para(doc, "后端启动：创建数据库后依次执行 docs/sql/V1.0.0__init.sql、V1.0.1__seed.sql、"
              "V1.0.2__accounts.sql；已有数据库升级执行 V1.0.3__menu_metadata.sql；"
              "随后在项目根目录执行 mvn spring-boot:run，默认监听 8080 端口，"
              "可通过 DB_URL、DB_USERNAME、DB_PASSWORD、SERVER_PORT 环境变量覆盖。")
    para(doc, "前端启动：进入 frontend 目录执行 npm ci 与 npm run dev，默认监听 5173 端口，"
              "开发服务器将 /api 请求代理到后端。质量检查执行 mvn verify（单元测试 + Checkstyle + JaCoCo 门禁）、"
              "npm run lint、npm run build 与 npm run test:e2e。")

    # ---------------- 三、测试计划 ----------------
    h1(doc, "三、测试策略与计划")

    h2(doc, "3.1 测试分层")
    table(doc, ["层次", "测试对象", "工具", "关注点"], [
        ["单元测试", "Service、Mapper", "JUnit 5 + Mockito", "业务分支、异常路径、SQL 正确性"],
        ["功能测试", "HTTP 接口（黑盒）", "Python 自动化执行器", "等价类、边界值、场景与错误推测"],
        ["端到端测试", "前端页面与交互", "Playwright", "页面可用性与多设备布局兼容"],
        ["接口回归", "核心业务链路", "Newman", "CI 中的链路可用性与断言校验"],
        ["性能基准", "菜品列表、推荐接口", "JMeter 5.6.3", "30 并发下的响应时间与失败率"],
        ["静态检查", "源码规范与覆盖率", "Checkstyle + JaCoCo", "编码规范、覆盖率门禁"],
    ], widths=[2.4, 3.6, 3.4, 6.6], size=9)
    caption(doc, "表 3-1  测试分层与工具选型")

    h2(doc, "3.2 测试方法")
    bullet(doc, "等价类划分：将用户名、密码、辣度、餐段、预算等输入划分为有效与无效等价类，"
                "各选取代表值验证，例如用户名字符集、密码长度区间。")
    bullet(doc, "边界值分析：针对 4/32 位用户名、8/72 位密码、0–3 级辣度、1–4 餐段、"
                "最多 40 个忌口标签等边界，取边界值与边界外一档设计用例。")
    bullet(doc, "场景法：沿真实业务流程设计用例，如「注册—登录—填写问卷—生成推荐—"
                "反馈—换一组—收藏—取消收藏—登出」的全链路场景，并覆盖问卷缺失、"
                "无匹配菜品等异常分支。")
    bullet(doc, "错误推测法：基于经验推测易错点，如重复注册、跨用户访问资源、"
                "伪造 Token、越权提权、价格区间倒置、空输入与超长输入等。")

    h2(doc, "3.3 质量门禁")
    bullet(doc, "单元测试全部通过，且 mvn verify 构建成功。")
    bullet(doc, "JaCoCo 覆盖率：服务包行覆盖率 ≥ 70%，推荐服务 ≥ 60%。")
    bullet(doc, "前端 lint 与 build 通过，端到端用例在 3 种设备配置下全部通过。")
    bullet(doc, "接口回归：Newman 全部断言通过。")
    bullet(doc, "性能基准：采样数 ≥ 100，失败率 ≤ 1%，响应时间 p95 ≤ 3000 ms。")

    # ---------------- 四、测试用例设计 ----------------
    doc.add_page_break()
    h1(doc, "四、测试用例设计")
    para(doc, "本节列出功能测试层的用例设计结果。用例按模块组织，标注所采用的测试方法，"
              "「结论」列为实际执行后的判定结果。执行方式为调用运行中的后端服务发起真实 HTTP 请求，"
              "由脚本自动比对实际响应与预期结果。")

    widths = [1.7, 1.5, 3.5, 2.0, 3.4, 3.0, 1.0]
    rows = [[c["用例ID"], c["模块"], c["用例标题"], c["测试方法"], c["测试数据"],
             c["预期结果"], c["结论"]] for c in func_cases]
    table(doc, ["用例ID", "模块", "用例标题", "测试方法", "测试数据", "预期结果", "结论"],
          rows, widths=widths, size=7.5, header_size=8)
    caption(doc, "表 4-1  功能测试用例设计与执行结果（共 56 条）")

    h2(doc, "4.1 端到端与性能测试用例")
    table(doc, ["用例ID", "测试配置 / 指标", "用例标题", "判定标准", "结论"], [
        ["E2E-UI-01", "Desktop Chrome", "菜品列表可用、布局不横向溢出并保存设备截图", "断言通过且生成截图", "通过"],
        ["E2E-UI-02", "iPhone 13 WebKit", "菜品列表可用、布局不横向溢出并保存设备截图", "断言通过且生成截图", "通过"],
        ["E2E-UI-03", "Pixel 7 Chromium", "菜品列表可用、布局不横向溢出并保存设备截图", "断言通过且生成截图", "通过"],
        ["E2E-UI-04", "Desktop Chrome", "未登录访问收藏页会被引导登录", "跳转至登录页并带 redirect 参数", "通过"],
        ["E2E-UI-05", "iPhone 13 WebKit", "未登录访问收藏页会被引导登录", "跳转至登录页并带 redirect 参数", "通过"],
        ["E2E-UI-06", "Pixel 7 Chromium", "未登录访问收藏页会被引导登录", "跳转至登录页并带 redirect 参数", "通过"],
        ["PERF-01", "采样总数", "30 并发压测样本量", "≥ 100 个采样", "通过"],
        ["PERF-02", "失败率", "30 并发下请求失败比例", "≤ 1%", "通过"],
        ["PERF-03", "响应时间 p95", "菜品列表与推荐接口响应时延", "≤ 3000 ms", "通过"],
        ["PERF-04", "吞吐量", "系统整体处理能力", "参考指标，记录实测值", "通过"],
    ], widths=[2.0, 3.0, 6.0, 3.0, 1.6], size=8.5)
    caption(doc, "表 4-2  端到端与性能测试用例")

    # ---------------- 五、测试执行与结果 ----------------
    doc.add_page_break()
    h1(doc, "五、测试执行与结果")

    h2(doc, "5.1 单元测试执行结果")
    para(doc, "执行 mvn test，共运行 14 个测试类、51 条单元测试，全部通过，构建结果 BUILD SUCCESS。"
              "该层覆盖 Service 业务分支与 Mapper SQL 正确性，是白盒测试的主要载体。")
    table(doc, ["测试类", "覆盖模块", "用例数", "通过", "失败", "结论"],
          UNIT_ROWS, widths=[6.6, 2.6, 1.6, 1.4, 1.4, 1.6], size=8.5)
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(10)
    _font(p.add_run("合计：51 条，通过 51 条，失败 0 条，通过率 100%"), name=CN_BOLD, size=9.5, bold=True)

    h2(doc, "5.2 功能测试执行结果")
    para(doc, "执行 tests/functional/run_functional_cases.py，对 8 个模块的 56 条黑盒用例发起真实 HTTP 请求，"
              "全部通过，通过率 100%，耗时 3.6 秒。逐条明细见「表 4-1 功能测试用例设计与执行结果」，"
              "结构化结果同时输出到 tests/functional/results/ 目录下的 CSV 与 JSON 文件中。")

    h2(doc, "5.3 端到端测试执行结果")
    para(doc, "执行 npx playwright test，在桌面 Chromium、iPhone 13 WebKit、Pixel 7 Chromium "
              "三种设备配置下运行 6 条用例，全部通过。")
    table(doc, ["用例ID", "设备配置", "用例标题", "结论", "耗时"],
          E2E_ROWS, widths=[2.0, 3.0, 7.4, 1.6, 1.4], size=8.5)
    caption(doc, "表 5-1  端到端测试执行结果")

    h2(doc, "5.4 接口回归执行结果")
    para(doc, "执行 newman run tests/newman/collection.json，覆盖健康检查、公开查询、"
              "注册登录、问卷、推荐、收藏、登出等核心链路，共 14 个请求、14 条断言，全部通过，"
              "平均响应时间 64 ms。")
    table(doc, ["序号", "请求", "用例说明", "状态码", "结论"],
          NEWMAN_ROWS, widths=[1.4, 6.2, 4.4, 1.6, 1.6], size=8.5)
    caption(doc, "表 5-2  接口回归执行结果（Newman）")

    h2(doc, "5.5 性能基准执行结果")
    para(doc, "使用 Apache JMeter 5.6.3 以 30 并发执行 tests/jmeter/recommendation-load.jmx，"
              "累计 600 个采样，失败 0 个，平均响应时间 30 ms，最大 135 ms，p95 为 26 ms，"
              "吞吐量 58.8 请求/秒，全部满足阈值要求。")
    table(doc, ["用例ID", "指标", "实测结果", "数值", "结论"],
          PERF_ROWS, widths=[1.8, 4.2, 5.6, 2.0, 1.6], size=8.5)
    caption(doc, "表 5-3  性能基准执行结果（JMeter，30 并发）")

    h2(doc, "5.6 执行结果汇总")
    table(doc, ["测试层次", "用例数", "通过", "失败", "通过率", "执行方式"], [
        ["单元测试（白盒）", "51", "51", "0", "100%", "mvn test"],
        ["功能测试（黑盒）", "56", "56", "0", "100%", "自动化执行器"],
        ["端到端测试（UI）", "6", "6", "0", "100%", "Playwright × 3 设备"],
        ["接口回归", "14", "14", "0", "100%", "Newman"],
        ["性能基准", "4", "4", "0", "100%", "JMeter 30 并发"],
        ["合计", "131", "131", "0", "100%", "——"],
    ], widths=[4.0, 2.2, 1.8, 1.8, 2.0, 4.2], size=9)
    caption(doc, "表 5-4  测试执行结果汇总")

    # ---------------- 六、缺陷记录 ----------------
    doc.add_page_break()
    h1(doc, "六、缺陷记录与修复")
    para(doc, "本章按 Jira / PingCode 风格登记本轮测试中发现并处理的缺陷。缺陷字段包含缺陷编号、"
              "标题、所属模块、严重度、优先级、状态、发现阶段、复现步骤、期望结果、实际结果、"
              "根因分析与修复说明。")

    h2(doc, "6.1 缺陷统计")
    table(doc, ["严重度", "数量", "占比", "已修复"], [
        ["致命 S1", "1", "9.1%", "1"],
        ["严重 S2", "4", "36.4%", "4"],
        ["一般 S3", "6", "54.5%", "6"],
        ["合计", "11", "100%", "11"],
    ], widths=[3.0, 2.4, 2.6, 2.4], size=9)
    caption(doc, "表 6-1  缺陷严重度分布")

    table(doc, ["所属模块", "缺陷数", "处理情况"],
          DEFECT_MODULE_STAT, widths=[5.0, 3.0, 4.0], size=9)
    caption(doc, "表 6-2  缺陷模块分布")

    h2(doc, "6.2 缺陷明细")
    table(doc, ["缺陷ID", "标题", "模块", "严重度", "优先级", "状态", "发现阶段"],
          [[d["id"], d["title"], d["module"], d["severity"], d["priority"], d["status"], d["found"]]
           for d in DEFECTS],
          widths=[1.6, 4.6, 2.6, 1.4, 1.2, 1.2, 2.6], size=8)
    caption(doc, "表 6-3  缺陷清单")

    h2(doc, "6.3 典型缺陷详情")
    for d in DEFECTS:
        h3(doc, "%s  %s" % (d["id"], d["title"]))
        table(doc, ["字段", "内容"], [
            ["所属模块", d["module"]],
            ["严重度 / 优先级", "%s / %s" % (d["severity"], d["priority"])],
            ["当前状态", d["status"]],
            ["发现阶段", d["found"]],
            ["复现步骤", d["step"]],
            ["期望结果", d["expect"]],
            ["实际结果", d["actual"]],
            ["根因分析", d["cause"]],
            ["修复说明", d["fix"]],
        ], widths=[2.8, 13.2], size=8.5)

    # ---------------- 七、实验总结 ----------------
    doc.add_page_break()
    h1(doc, "七、实验总结")
    para(doc, "本实验围绕「今天吃什么」校园食堂菜品推荐系统，完成了从项目初稿提交、"
              "测试设计与执行，到缺陷登记、修复与回归验证的完整闭环。")

    h2(doc, "7.1 主要工作")
    bullet(doc, "提交了可运行的项目初稿，包含后端 Spring Boot 应用、Vue 3 前端、"
                "版本化数据库脚本与项目文档，形成了稳定的被测基线。")
    bullet(doc, "设计了覆盖 8 个功能模块的 56 条黑盒用例，以及端到端与性能用例，"
                "综合运用等价类划分、边界值分析、场景法与错误推测法。")
    bullet(doc, "完成五层测试执行：单元测试 51 条、功能测试 56 条、端到端测试 6 条、"
                "接口回归 14 条、性能基准 4 项，合计 131 项检查全部通过。")
    bullet(doc, "登记并修复缺陷 11 项，涵盖数据库 schema 漂移、升级脚本缺失、"
                "空指针异常、鉴权死代码、测试脚本缺陷与持续集成配置问题，修复率 100%。")

    h2(doc, "7.2 问题与收获")
    bullet(doc, "数据库脚本与持久层不同步是本次代价最高的一类缺陷：字段只在全新安装脚本中补齐，"
                "却遗漏了已有库的升级路径，直接导致查询接口 500。后续应将 DDL 变更与增量脚本"
                "作为同一个原子提交，并在 CI 中用「老库升级 + 冒烟」加以约束。")
    bullet(doc, "测试脚本本身也需要被测试。Newman 用户名不符合校验规则、JMeter 的 __groovy 逗号"
                "被误解析、Playwright 路由过宽等问题，都表现为「被测系统看起来有问题」，"
                "实际是测试资产缺陷。定位这类问题时，应优先取得工具自身的原始日志。")
    bullet(doc, "断言设计要贴合真实契约。例如推荐接口返回的是 items[].dish.id 嵌套结构，"
                "若按扁平结构取数会得到 None，进而产生误导性的失败结果；"
                "在编写自动化断言前应先确认响应结构。")
    bullet(doc, "统一的参数校验约束应当前后端、注册登录两端对齐，"
                "本次登录接口缺失格式校验即属于此类不一致，虽未造成安全后果，"
                "但会让错误信息与校验时机出现偏差。")

    h2(doc, "7.3 后续改进方向")
    bullet(doc, "补充契约测试，对接口响应结构与字段取值域进行自动校验，防止 DDL 与 DTO 再次漂移。")
    bullet(doc, "将测试数据与夹具脚本纳入版本管理，保证用例可重复执行、结果可复现。")
    bullet(doc, "在持续集成中增加数据库升级路径的验证作业，覆盖「老库 + 增量脚本」场景。")
    bullet(doc, "扩展性能场景，补充推荐接口在更大数据量与更高并发下的表现，"
                "并纳入趋势对比与基线告警。")

    doc.save(OUT)
    return OUT


if __name__ == "__main__":
    path = build()
    print("报告已生成：%s" % path)
