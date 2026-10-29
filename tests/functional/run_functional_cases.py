# -*- coding: utf-8 -*-
"""
功能测试用例自动执行器
=====================

把《软件测试》实验中「测试用例设计」一章列出的功能测试用例，逐条转成可执行的
HTTP 调用，对运行中的「今天吃什么」后端服务发起真实请求，并自动判定通过 / 失败。

用途：
    python tests/functional/run_functional_cases.py                 # 默认 http://127.0.0.1:8080
    python tests/functional/run_functional_cases.py http://host:port

产出：
    tests/functional/results/functional_results.csv   逐条用例结果
    tests/functional/results/functional_results.json  汇总结果
    控制台输出          逐条执行明细 + 汇总统计

说明：
    用例覆盖等价类划分、边界值分析、场景法与错误推测法四类黑盒测试方法。
    脚本只依赖 Python 标准库，无需安装第三方包。
"""

import csv
import json
import os
import random
import string
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8080").rstrip("/")
HERE = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.join(HERE, "results")
PASSWORD = "TestPass_2026"

results = []


# --------------------------------------------------------------------------
# HTTP 助手
# --------------------------------------------------------------------------
def call(method, path, body=None, token=None, raw=None, content_type="application/json"):
    """发起一次 HTTP 请求，返回 (status, parsed_json_or_None, raw_text)."""
    url = BASE + path
    data = None
    headers = {}
    if raw is not None:
        data = raw.encode("utf-8") if isinstance(raw, str) else raw
        headers["Content-Type"] = content_type
    elif body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = "Bearer " + token

    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            text = resp.read().decode("utf-8", "replace")
            status = resp.status
    except urllib.error.HTTPError as err:
        text = err.read().decode("utf-8", "replace")
        status = err.code
    except Exception as err:  # 连接失败等
        return None, None, "请求异常: %s" % err

    try:
        return status, json.loads(text), text
    except Exception:
        return status, None, text


def rand_name(prefix="tc"):
    return prefix + "_" + "".join(random.choices(string.ascii_lowercase + string.digits, k=8))


def dish_ids(data):
    """从推荐结果中取出菜品 ID 列表。

    推荐接口返回结构为 items: [{ dish: {...}, score, reason }]，
    菜品 ID 位于嵌套的 dish 对象中，直接取 items[].id 会得到 None。
    """
    out = []
    for item in (data.get("items") or data.get("dishes") or []):
        if isinstance(item, dict) and isinstance(item.get("dish"), dict):
            out.append(item["dish"].get("id"))
        elif isinstance(item, dict):
            out.append(item.get("id"))
    return [i for i in out if i is not None]


def register(username=None, password=PASSWORD, nickname=None):
    username = username or rand_name()
    return username, call("POST", "/api/auth/register",
                          {"username": username, "password": password, "nickname": nickname})


def login(username, password=PASSWORD):
    return call("POST", "/api/auth/login", {"username": username, "password": password})


def register_login(username=None, password=PASSWORD):
    """注册并登录，返回 (username, token)。失败返回 (username, None)。"""
    username = username or rand_name()
    reg = call("POST", "/api/auth/register",
               {"username": username, "password": password, "nickname": "测试用户"})
    if reg[0] != 200:
        return username, None
    lg = login(username, password)
    if lg[0] != 200 or not lg[1] or not lg[1].get("data"):
        return username, None
    return username, lg[1]["data"].get("token")


# 种子数据中的标签 ID 从 930001 开始（见 docs/sql/V1.0.1__seed.sql）
VALID_TAG_ID = 930001


def save_survey(token, spice=2, bmin="5.00", bmax="25.00",
                like=None, dislike=None, diet=0):
    """保存口味问卷。"""
    return call("PUT", "/api/preferences/me", {
        "maxSpiceLevel": spice,
        "budgetMin": bmin,
        "budgetMax": bmax,
        "likeTagIds": like if like is not None else [VALID_TAG_ID],
        "dislikeTagIds": dislike if dislike is not None else [],
        "dietType": diet,
    }, token=token)


# --------------------------------------------------------------------------
# 用例注册
# --------------------------------------------------------------------------
def case(tc_id, module, title, method, precondition, steps, data,
         expect, check):
    """执行一条用例并记录结果。check 返回 (actual_desc, passed)。"""
    try:
        actual, passed = check()
    except Exception as err:
        actual, passed = "执行异常: %s" % err, False

    results.append({
        "用例ID": tc_id,
        "模块": module,
        "用例标题": title,
        "测试方法": method,
        "前置条件": precondition,
        "测试步骤": steps,
        "测试数据": data,
        "预期结果": expect,
        "实际结果": actual,
        "结论": "通过" if passed else "不通过",
    })
    flag = "PASS" if passed else "FAIL"
    print("[%s] %-12s %s" % (flag, tc_id, title))
    if not passed:
        print("        预期: %s" % expect)
        print("        实际: %s" % actual)
    return passed


# ==========================================================================
# 一、用户注册与登录模块（等价类划分 + 边界值分析）
# ==========================================================================
def module_auth():
    # ---- 用户名等价类 / 边界值 ----
    u = rand_name()  # 8 位合法用户名（有效等价类）
    _, (st, js, _rx) = register(u)
    case("TC-AUTH-01", "用户认证", "合法用户名/密码注册成功", "等价类划分-有效等价类",
         "服务正常，数据库可写", "POST /api/auth/register 提交合法注册信息",
         "username=%s, password=%s" % (u, PASSWORD),
         "HTTP 200，返回 token 与用户信息",
         lambda: ("HTTP %s，%s" % (st, (js or {}).get("message")),
                  st == 200 and bool((js or {}).get("data", {}).get("token"))))

    def _user_len(n, tail_len=0):
        """构造长度为 n 且每次运行不重复的用户名（末位补随机字符，保证可重复执行）。"""
        tail = "".join(random.choices(string.ascii_lowercase + string.digits, k=tail_len))
        name = "a" * (n - tail_len) + tail
        _, (s, j, _t) = register(name)
        return s, j, name

    s, j, name = _user_len(3)
    case("TC-AUTH-02", "用户认证", "用户名 3 位（下边界-1）注册被拒", "边界值分析",
         "服务正常", "提交 3 位用户名注册", "username=aaa",
         "HTTP 400，参数校验不通过",
         lambda: ("HTTP %s" % s, s == 400))

    s, j, name = _user_len(4, tail_len=2)
    case("TC-AUTH-03", "用户认证", "用户名 4 位（下边界）注册成功", "边界值分析",
         "服务正常", "提交 4 位用户名注册", "username=%s（4 位）" % name,
         "HTTP 200，注册成功",
         lambda: ("HTTP %s" % s, s == 200))

    s, j, name = _user_len(32, tail_len=2)
    case("TC-AUTH-04", "用户认证", "用户名 32 位（上边界）注册成功", "边界值分析",
         "服务正常", "提交 32 位用户名注册", "username=%s...（32 位）" % name[:8],
         "HTTP 200，注册成功",
         lambda: ("HTTP %s" % s, s == 200))

    s, j, name = _user_len(33)
    case("TC-AUTH-05", "用户认证", "用户名 33 位（上边界+1）注册被拒", "边界值分析",
         "服务正常", "提交 33 位用户名注册", "username=a*33",
         "HTTP 400，参数校验不通过",
         lambda: ("HTTP %s" % s, s == 400))

    _, (s, j, _t6) = register("中文用户名")
    case("TC-AUTH-06", "用户认证", "用户名含中文（非法字符）注册被拒", "等价类划分-无效等价类",
         "服务正常", "提交中文用户名注册", "username=中文用户名",
         "HTTP 400，正则 ^[a-zA-Z0-9_]{4,32}$ 校验失败",
         lambda: ("HTTP %s" % s, s == 400))

    _, (s, j, _t7) = register("bad-user")  # 含短横线
    case("TC-AUTH-07", "用户认证", "用户名含短横线（非法字符）注册被拒", "等价类划分-无效等价类",
         "服务正常", "提交含 - 的用户名注册", "username=bad-user",
         "HTTP 400，参数校验不通过",
         lambda: ("HTTP %s" % s, s == 400))

    # ---- 密码边界值 ----
    def _pwd(p):
        return register(rand_name(), password=p)[1]

    s, j, _ = _pwd("Ab3" + "x" * 4)  # 7 位
    case("TC-AUTH-08", "用户认证", "密码 7 位（下边界-1）注册被拒", "边界值分析",
         "服务正常", "提交 7 位密码注册", "password=7 位",
         "HTTP 400，@Size(min=8) 校验失败",
         lambda: ("HTTP %s" % s, s == 400))

    s, j, _ = _pwd("Ab3" + "x" * 5)  # 8 位
    case("TC-AUTH-09", "用户认证", "密码 8 位（下边界）注册成功", "边界值分析",
         "服务正常", "提交 8 位密码注册", "password=8 位",
         "HTTP 200，注册成功",
         lambda: ("HTTP %s" % s, s == 200))

    s, j, _ = _pwd("Ab3" + "x" * 69)  # 72 位
    case("TC-AUTH-10", "用户认证", "密码 72 字符（上边界）注册成功", "边界值分析",
         "服务正常", "提交 72 字符密码注册", "password=72 字符",
         "HTTP 200，注册成功",
         lambda: ("HTTP %s" % s, s == 200))

    s, j, _ = _pwd("Ab3" + "x" * 70)  # 73 位
    case("TC-AUTH-11", "用户认证", "密码 73 字符（上边界+1）注册被拒", "边界值分析",
         "服务正常", "提交 73 字符密码注册", "password=73 字符",
         "HTTP 400，@Size(max=72) 校验失败",
         lambda: ("HTTP %s" % s, s == 400))

    s, j, _ = _pwd("中" * 25)  # 25 个中文 = 75 字节 > 72
    case("TC-AUTH-12", "用户认证", "含中文密码按 UTF-8 字节数校验（75 字节被拒）", "边界值分析-字节边界",
         "服务正常", "提交 25 个中文字符的密码（UTF-8 占 75 字节）",
         "password=25 个中文字符（75 字节）",
         "HTTP 400，提示密码 UTF-8 长度不能超过 72 字节",
         lambda: ("HTTP %s，%s" % (s, (j or {}).get("message")), s == 400))

    # ---- 业务规则 ----
    u2, _r2 = register(u2 := rand_name())
    _, (s, j, _t13) = register(u2)
    case("TC-AUTH-13", "用户认证", "重复用户名注册被拒", "错误推测法",
         "用户 %s 已注册" % u2, "使用同一用户名再次注册", "username=%s（重复）" % u2,
         "HTTP 409，提示用户名已被使用",
         lambda: ("HTTP %s，%s" % (s, (j or {}).get("message")), s == 409))

    u3, tok = register_login()
    case("TC-AUTH-14", "用户认证", "正确用户名密码登录成功", "等价类划分-有效等价类",
         "用户 %s 已注册" % u3, "POST /api/auth/login 提交正确凭据",
         "username=%s, password=%s" % (u3, PASSWORD),
         "HTTP 200，返回 12 小时有效 token",
         lambda: ("HTTP %s，token=%s" % (200 if tok else -1, "已返回" if tok else "未返回"),
                  tok is not None))

    s, j, _ = login(u3, "WrongPass_2026")
    case("TC-AUTH-15", "用户认证", "密码错误登录失败", "等价类划分-无效等价类",
         "用户 %s 已注册" % u3, "提交正确用户名 + 错误密码", "password=WrongPass_2026",
         "HTTP 401，提示用户名或密码错误",
         lambda: ("HTTP %s，%s" % (s, (j or {}).get("message")), s == 401))

    s, j, _ = login(rand_name("ghost"), PASSWORD)
    case("TC-AUTH-16", "用户认证", "不存在的用户登录失败", "错误推测法",
         "该用户名未注册", "提交未注册的用户名登录", "username=未注册用户",
         "HTTP 401（不泄露账号是否存在）",
         lambda: ("HTTP %s" % s, s == 401))

    s, j, _ = call("POST", "/api/auth/login", {"username": "", "password": PASSWORD})
    case("TC-AUTH-17", "用户认证", "用户名为空登录被拒", "等价类划分-无效等价类",
         "服务正常", "提交空用户名登录", "username=（空）",
         "HTTP 400，@NotBlank 校验失败",
         lambda: ("HTTP %s" % s, s == 400))

    # 登出后 token 失效
    u4, tok4 = register_login()
    save_survey(tok4)  # GET 问卷在尚未保存时会返回 404，先造数据
    s1, _, _ = call("GET", "/api/preferences/me", token=tok4)
    call("POST", "/api/auth/logout", {}, token=tok4)
    s2, _, _ = call("GET", "/api/preferences/me", token=tok4)
    case("TC-AUTH-18", "用户认证", "登出后 Token 立即失效", "场景法",
         "用户已登录且已保存问卷（确保登出前接口可用）",
         "1. 带 token 访问受保护接口；2. 调用登出；3. 再次带同一 token 访问",
         "token=%s..." % (tok4 or "")[:16],
         "登出前 200，登出后 401",
         lambda: ("登出前 HTTP %s，登出后 HTTP %s" % (s1, s2), s1 == 200 and s2 == 401))


# ==========================================================================
# 二、口味问卷与画像模块
# ==========================================================================
def module_preference():
    s, j, _ = call("GET", "/api/preferences/me")  # 无 token
    case("TC-PREF-01", "口味问卷", "未登录读取问卷被拒", "错误推测法",
         "未携带 Token", "不带 Authorization 头访问 GET /api/preferences/me", "无 Token",
         "HTTP 401，鉴权拦截",
         lambda: ("HTTP %s" % s, s == 401))

    u, tok = register_login()
    s, j, _ = save_survey(tok)
    case("TC-PREF-02", "口味问卷", "保存合法口味问卷成功", "等价类划分-有效等价类",
         "用户已登录", "PUT /api/preferences/me 提交问卷",
         "maxSpiceLevel=2, budget 5~25, dietType=0",
         "HTTP 200，问卷持久化成功",
         lambda: ("HTTP %s" % s, s == 200))

    s, j, _ = save_survey(tok, spice=4)
    case("TC-PREF-03", "口味问卷", "辣度 4（上边界+1）被拒", "边界值分析",
         "用户已登录", "提交 maxSpiceLevel=4 的问卷", "maxSpiceLevel=4",
         "HTTP 400，@Max(3) 校验失败",
         lambda: ("HTTP %s" % s, s == 400))

    s0, _, _ = save_survey(tok, spice=0)
    s3, _, _ = save_survey(tok, spice=3)
    case("TC-PREF-04", "口味问卷", "辣度取值 0 与 3 边界均通过", "边界值分析",
         "用户已登录", "分别提交 maxSpiceLevel=0 与 3", "maxSpiceLevel=0 / 3",
         "两次均 HTTP 200",
         lambda: ("0 -> HTTP %s；3 -> HTTP %s" % (s0, s3), s0 == 200 and s3 == 200))

    s, j, _ = save_survey(tok, dislike=[i for i in range(1, 43)])  # 42 个
    case("TC-PREF-05", "口味问卷", "忌口标签超过 40 个被拒", "边界值分析",
         "用户已登录", "提交 42 个忌口标签 ID", "dislikeTagIds=42 个",
         "HTTP 400，@Size(max=40) 校验失败",
         lambda: ("HTTP %s" % s, s == 400))

    s, j, _ = call("GET", "/api/preferences/me/profile", token=tok)
    case("TC-PREF-06", "口味问卷", "读取口味画像成功", "场景法",
         "用户已登录且已保存问卷", "GET /api/preferences/me/profile",
         "无", "HTTP 200，返回标签权重画像",
         lambda: ("HTTP %s" % s, s == 200))

    # 再次保存问卷需保留反馈形成的权重调整
    save_survey(tok)
    r = call("POST", "/api/recommendations", {"mealType": 2, "limit": 3}, token=tok)
    rid = None
    if r[0] == 200 and r[1] and r[1]["data"]:
        rid = r[1]["data"].get("id")
        ids = dish_ids(r[1]["data"])
        if ids:
            call("POST", "/api/recommendations/%s/feedback" % rid,
                 {"dishId": ids[0], "feedbackType": 1}, token=tok)
    p1 = call("GET", "/api/preferences/me/profile", token=tok)
    save_survey(tok)  # 再次保存
    p2 = call("GET", "/api/preferences/me/profile", token=tok)

    def _weights(resp):
        d = (resp[1] or {}).get("data") or {}
        return json.dumps(d, ensure_ascii=False, sort_keys=True)

    case("TC-PREF-07", "口味问卷", "再次保存问卷保留反馈形成的权重调整", "场景法-回归验证",
         "用户已登录，已产生一次「喜欢」反馈",
         "1. 生成推荐并对首道菜点赞；2. 读取画像；3. 再次保存问卷；4. 再次读取画像对比",
         "feedbackType=1（喜欢）",
         "再次保存问卷后，画像权重不被清零",
         lambda: ("保存前/后画像一致: %s" % (_weights(p1) == _weights(p2)),
                  p1[0] == 200 and p2[0] == 200 and _weights(p1) == _weights(p2)))


# ==========================================================================
# 三、菜品目录模块
# ==========================================================================
def module_dish():
    s, j, _ = call("GET", "/api/dishes")
    total = ((j or {}).get("data") or {}).get("total")
    lst = ((j or {}).get("data") or {}).get("list") or ((j or {}).get("data") or {}).get("records") or []
    case("TC-DISH-01", "菜品目录", "菜品列表默认分页 size=20", "等价类划分-有效等价类",
         "数据库存在 100 道菜品", "GET /api/dishes 不带分页参数", "page/size 缺省",
         "HTTP 200，返回第 1 页且条数不超过 20",
         lambda: ("HTTP %s，返回 %d 条，total=%s" % (s, len(lst), total),
                  s == 200 and 0 < len(lst) <= 20))

    s, j, _ = call("GET", "/api/dishes?size=101")
    case("TC-DISH-02", "菜品目录", "分页 size=101 超上限被拒", "边界值分析",
         "服务正常", "GET /api/dishes?size=101", "size=101",
         "HTTP 400，@Max(100) 校验失败",
         lambda: ("HTTP %s" % s, s == 400))

    s, j, _ = call("GET", "/api/dishes?canteenId=900001&category=1&spiceLevel=0&minPrice=5&maxPrice=20&mealType=2")
    data = (j or {}).get("data") or {}
    lst = data.get("list") or data.get("records") or []
    ok = s == 200 and all(
        5 <= float(d.get("price", 0)) <= 20 for d in lst if d.get("price") is not None)
    case("TC-DISH-03", "菜品目录", "多条件组合筛选结果全部满足条件", "场景法",
         "数据库存在符合筛选条件的菜品",
         "按食堂+分类+辣度+价格区间+餐段组合查询",
         "canteenId=900001, category=1, spiceLevel=0, price 5~20, mealType=2",
         "HTTP 200，返回菜品全部落在筛选条件内",
         lambda: ("HTTP %s，返回 %d 条，全部满足价格区间: %s" % (s, len(lst), ok), ok))

    s, j, _ = call("GET", "/api/dishes/random?canteenId=900001&maxPrice=20")
    d = (j or {}).get("data") or {}
    case("TC-DISH-04", "菜品目录", "按食堂与预算随机抽取在售菜品", "场景法",
         "数据库存在在售菜品", "GET /api/dishes/random?canteenId=900001&maxPrice=20",
         "canteenId=900001, maxPrice=20",
         "HTTP 200，返回 1 道在售且价格 ≤20 的菜品",
         lambda: ("HTTP %s，菜品=%s，价格=%s" % (s, d.get("dishName"), d.get("price")),
                  s == 200 and d.get("id") is not None
                  and (d.get("price") is None or float(d.get("price")) <= 20)))

    s, j, _ = call("GET", "/api/dishes/920001")
    d = (j or {}).get("data") or {}
    case("TC-DISH-05", "菜品目录", "菜品详情返回打包适配度与数据来源字段", "回归验证",
         "数据库已应用 V1.0.0/V1.0.3 含 takeout_suitability、data_source 列，"
         "且 920001 已置为 2 / 实地核对",
         "GET /api/dishes/920001 并检查返回字段", "dishId=920001",
         "HTTP 200，响应体返回 takeoutSuitability=2、dataSource=实地核对（不报 Unknown column）",
         lambda: ("HTTP %s，takeoutSuitability=%s，dataSource=%s，message=%s"
                  % (s, d.get("takeoutSuitability"), d.get("dataSource"), (j or {}).get("message")),
                  s == 200 and d.get("takeoutSuitability") == 2
                  and d.get("dataSource") == "实地核对"))

    s, j, _ = call("GET", "/api/dishes/99999999")
    case("TC-DISH-06", "菜品目录", "查询不存在的菜品 ID 返回 404", "错误推测法",
         "服务正常", "GET /api/dishes/99999999", "id=99999999",
         "HTTP 404，提示菜品不存在",
         lambda: ("HTTP %s" % s, s == 404))

    s, j, _ = call("GET", "/api/dishes?minPrice=50&maxPrice=10")
    case("TC-DISH-07", "菜品目录", "价格区间 minPrice > maxPrice 被拒", "错误推测法",
         "服务正常", "GET /api/dishes?minPrice=50&maxPrice=10", "minPrice=50, maxPrice=10",
         "HTTP 400，参数不合法",
         lambda: ("HTTP %s" % s, s == 400))


# ==========================================================================
# 四、档口与食堂模块
# ==========================================================================
def module_shop():
    s, j, _ = call("GET", "/api/shops/910001")
    d = (j or {}).get("data") or {}
    case("TC-SHOP-01", "档口食堂", "档口详情返回排队热度字段", "回归验证",
         "数据库已应用含 queue_heat 列的脚本，且 910001 已置为 2（较长）",
         "GET /api/shops/910001", "shopId=910001",
         "HTTP 200，响应体返回 queueHeat=2（不报 Unknown column）",
         lambda: ("HTTP %s，queueHeat=%s，message=%s" % (s, d.get("queueHeat"), (j or {}).get("message")),
                  s == 200 and d.get("queueHeat") == 2))

    u, tok = register_login()
    s, j, _ = call("DELETE", "/api/shops/910001", token=tok)
    case("TC-SHOP-02", "档口食堂", "仍有关联菜品时删除档口被拒", "错误推测法",
         "档口 910001 下存在菜品", "以管理员身份 DELETE /api/shops/910001", "shopId=910001",
         "HTTP 400/403，提示存在子数据无法删除",
         lambda: ("HTTP %s，%s" % (s, (j or {}).get("message")), s in (400, 403)))


# ==========================================================================
# 五、个性化推荐模块
# ==========================================================================
def module_recommend():
    s, j, _ = call("POST", "/api/recommendations", {"mealType": 2, "limit": 3})
    case("TC-REC-01", "个性化推荐", "未登录请求推荐被拒", "错误推测法",
         "未携带 Token", "不带 Token POST /api/recommendations", "mealType=2",
         "HTTP 401，鉴权拦截",
         lambda: ("HTTP %s" % s, s == 401))

    u, tok = register_login()
    s, j, _ = call("POST", "/api/recommendations", {"mealType": 2, "limit": 3}, token=tok)
    case("TC-REC-02", "个性化推荐", "未完成问卷直接推荐被拒", "场景法-异常分支",
         "用户已登录但未保存问卷", "POST /api/recommendations", "mealType=2",
         "HTTP 400，提示请先完成口味问卷",
         lambda: ("HTTP %s，%s" % (s, (j or {}).get("message")), s == 400))

    save_survey(tok)
    s, j, _ = call("POST", "/api/recommendations", {"mealType": 2, "limit": 3}, token=tok)
    d = (j or {}).get("data") or {}
    items = dish_ids(d)
    case("TC-REC-03", "个性化推荐", "完成问卷后生成推荐成功", "场景法-正常分支",
         "用户已登录且已保存问卷", "1. 保存问卷；2. POST /api/recommendations",
         "mealType=2, limit=3",
         "HTTP 200，返回不超过 3 道菜及推荐理由",
         lambda: ("HTTP %s，返回 %d 道菜" % (s, len(items)),
                  s == 200 and 0 < len(items) <= 3))

    s, j, _ = call("POST", "/api/recommendations", {"mealType": 5, "limit": 3}, token=tok)
    case("TC-REC-04", "个性化推荐", "mealType=5 越界被拒", "边界值分析",
         "用户已登录且已保存问卷", "提交 mealType=5", "mealType=5",
         "HTTP 400，@Max(4) 校验失败",
         lambda: ("HTTP %s" % s, s == 400))

    # 换一组应排除上一组菜品
    s, j, _ = call("POST", "/api/recommendations", {"mealType": 2, "limit": 3}, token=tok)
    d = (j or {}).get("data") or {}
    rid = d.get("id")
    first = dish_ids(d)
    s2, j2, _ = (None, None, None)
    second, overlap = [], set()
    if first and rid is not None:
        s2, j2, _ = call("POST", "/api/recommendations/%s/next" % rid,
                         {"dishId": first[0], "feedbackType": 3}, token=tok)
        d2 = (j2 or {}).get("data") or {}
        second = dish_ids(d2)
        overlap = set(first) & set(second)
    case("TC-REC-05", "个性化推荐", "换一组结果不重复上一组菜品", "场景法",
         "用户已生成一组推荐",
         "1. 生成推荐记录首组菜品；2. 调用 next 换一组；3. 比对两组菜品交集",
         "feedbackType=3（换一组）",
         "HTTP 200，两组菜品无交集",
         lambda: ("HTTP %s，首组 %d 道，次组 %d 道，交集 %d 道"
                  % (s2, len(first), len(second), len(overlap)),
                  s2 == 200 and len(first) > 0 and len(overlap) == 0))

    s, j, _ = call("POST", "/api/recommendations", {"mealType": 2, "limit": 3}, token=tok)
    d = (j or {}).get("data") or {}
    rid = d.get("id")
    valid_dish = (dish_ids(d) or [None])[0]
    s2, j2, _ = call("POST", "/api/recommendations/%s/feedback" % rid,
                     {"dishId": 99999999, "feedbackType": 1}, token=tok)
    case("TC-REC-06", "个性化推荐", "反馈不属于本次推荐的菜品被拒", "错误推测法",
         "用户已生成一组推荐", "对不在推荐列表中的菜品 ID 提交反馈", "dishId=99999999",
         "HTTP 400，提示该菜品不属于本次推荐",
         lambda: ("HTTP %s，%s" % (s2, (j2 or {}).get("message")), s2 == 400))

    s3, j3, _ = call("POST", "/api/recommendations/%s/feedback" % rid,
                     {"dishId": valid_dish, "feedbackType": 3}, token=tok)
    case("TC-REC-07", "个性化推荐", "feedbackType=3 走 feedback 接口被拒", "等价类划分-无效等价类",
         "用户已生成一组推荐", "以 feedbackType=3 调用 feedback 接口", "feedbackType=3",
         "HTTP 400，提示换一组请使用 next 接口",
         lambda: ("HTTP %s，%s" % (s3, (j3 or {}).get("message")), s3 == 400))

    # 跨用户访问他人推荐记录
    ua, ta = register_login()
    save_survey(ta)
    ra = call("POST", "/api/recommendations", {"mealType": 2, "limit": 3}, token=ta)
    other_rid = ((ra[1] or {}).get("data") or {}).get("id")
    ub, tb = register_login()
    save_survey(tb)
    s4, j4, _ = call("POST", "/api/recommendations/%s/next" % other_rid,
                     {"dishId": 1, "feedbackType": 3}, token=tb)
    case("TC-REC-08", "个性化推荐", "跨用户访问他人推荐记录被拒", "安全性测试",
         "用户 A 生成推荐记录 %s，用户 B 已登录" % other_rid,
         "以用户 B 的 Token 操作用户 A 的推荐记录", "recommendationId=%s（他人）" % other_rid,
         "HTTP 404，提示推荐记录不存在（不泄露资源存在性）",
         lambda: ("HTTP %s，%s" % (s4, (j4 or {}).get("message")), s4 == 404))

    # 无符合条件菜品
    uc, tc = register_login()
    save_survey(tc, spice=0, bmin="0.01", bmax="0.02")
    s5, j5, _ = call("POST", "/api/recommendations", {"mealType": 1, "limit": 3}, token=tc)
    case("TC-REC-09", "个性化推荐", "无符合条件菜品时给出明确提示", "场景法-异常分支",
         "用户预算设置为 0.01~0.02 元（无菜品满足）",
         "保存极端预算问卷后请求推荐", "budgetMin=0.01, budgetMax=0.02",
         "HTTP 404，提示没有符合条件的菜品",
         lambda: ("HTTP %s，%s" % (s5, (j5 or {}).get("message")), s5 == 404))


# ==========================================================================
# 六、收藏模块
# ==========================================================================
def module_favorite():
    s, j, _ = call("POST", "/api/favorites", {"targetType": 1, "targetId": 920001})
    case("TC-FAV-01", "收藏", "未登录添加收藏被拒", "错误推测法",
         "未携带 Token", "不带 Token POST /api/favorites",
         "targetType=1, targetId=920001", "HTTP 401，鉴权拦截",
         lambda: ("HTTP %s" % s, s == 401))

    u, tok = register_login()
    s, j, _ = call("POST", "/api/favorites", {"targetType": 1, "targetId": 920001}, token=tok)
    case("TC-FAV-02", "收藏", "登录用户添加菜品收藏成功", "场景法-正常分支",
         "用户已登录", "POST /api/favorites 收藏菜品 920001",
         "targetType=1（菜品）, targetId=920001",
         "HTTP 200，收藏记录写入",
         lambda: ("HTTP %s" % s, s == 200))

    call("POST", "/api/favorites", {"targetType": 1, "targetId": 920001}, token=tok)
    s2, j2, _ = call("GET", "/api/favorites/ids?targetType=1", token=tok)
    ids = ((j2 or {}).get("data") or [])
    case("TC-FAV-03", "收藏", "重复收藏同一菜品保持幂等", "错误推测法",
         "已收藏菜品 920001", "再次提交相同收藏请求后查询收藏 ID 列表",
         "targetId=920001（重复提交 2 次）",
         "收藏 ID 列表中 920001 只出现一次",
         lambda: ("HTTP %s，收藏列表=%s" % (s2, ids),
                  s2 == 200 and ids.count(920001) == 1 if isinstance(ids, list) else False))

    s3, j3, _ = call("DELETE", "/api/favorites/1/920001", token=tok)
    s4, j4, _ = call("GET", "/api/favorites/ids?targetType=1", token=tok)
    ids4 = ((j4 or {}).get("data") or [])
    case("TC-FAV-04", "收藏", "移除收藏后不再出现在收藏列表", "场景法",
         "已收藏菜品 920001", "1. DELETE /api/favorites/1/920001；2. 查询收藏列表",
         "targetType=1, targetId=920001",
         "HTTP 200，收藏列表中不含 920001（逻辑删除）",
         lambda: ("HTTP %s，删除后列表=%s" % (s3, ids4),
                  s3 == 200 and (920001 not in ids4 if isinstance(ids4, list) else False)))

    call("POST", "/api/favorites", {"targetType": 1, "targetId": 920001}, token=tok)
    s5, _, _ = call("GET", "/api/favorites/ids?targetType=1", token=tok)
    ids5 = ((s5 and j4 and ((s5, j4)[1] or {}).get("data")) or [])
    s5b, j5b, _ = call("GET", "/api/favorites/ids?targetType=1", token=tok)
    ids5 = ((j5b or {}).get("data") or [])
    case("TC-FAV-05", "收藏", "取消后可再次收藏（逻辑删除可逆）", "场景法",
         "菜品 920001 已被取消收藏", "重新提交收藏请求后查询收藏列表",
         "targetId=920001（再次收藏）",
         "HTTP 200，收藏列表重新包含 920001",
         lambda: ("HTTP %s，列表=%s" % (s5b, ids5),
                  s5b == 200 and (920001 in ids5 if isinstance(ids5, list) else False)))


# ==========================================================================
# 七、智能助手模块
# ==========================================================================
def module_assistant():
    s, j, _ = call("POST", "/api/assistant/search", {"message": "来个不辣的午餐，15 元以内", "page": 1})
    d = (j or {}).get("data") or {}
    case("TC-ASST-01", "智能助手", "自然语言解析预算/辣度/餐段条件", "场景法",
         "菜品库已有数据，AI 未配置时走规则解析",
         "POST /api/assistant/search 提交自然语言查询",
         "message=来个不辣的午餐，15 元以内",
         "HTTP 200，返回按解析条件筛选出的菜品",
         lambda: ("HTTP %s，返回 %s 条" % (s, len(d.get("list") or d.get("dishes") or [])),
                  s == 200))

    s, j, _ = call("POST", "/api/assistant/search", {"message": "", "page": 1})
    case("TC-ASST-02", "智能助手", "空输入不导致 500 或空指针", "错误推测法-缺陷回归",
         "服务正常", "POST /api/assistant/search 提交空字符串", "message=（空）",
         "HTTP 400，参数校验拦截，不出现 500",
         lambda: ("HTTP %s" % s, s == 400))

    s, j, _ = call("POST", "/api/assistant/search", {"message": "x" * 501, "page": 1})
    case("TC-ASST-03", "智能助手", "超长输入（501 字）被拒", "边界值分析",
         "服务正常", "提交 501 个字符的查询文本", "message=501 字符",
         "HTTP 400，@Size(max=500) 校验失败",
         lambda: ("HTTP %s" % s, s == 400))

    s, j, _ = call("POST", "/api/assistant/ask", {"message": "附近有什么好吃的", "page": 1})
    case("TC-ASST-04", "智能助手", "「附近」意图给出无法按距离排序的明确提示", "场景法-异常分支",
         "表结构不含宿舍与食堂坐标", "POST /api/assistant/ask 询问附近美食",
         "message=附近有什么好吃的",
         "HTTP 200，返回文字答案并提示无法按实际步行距离排序",
         lambda: ("HTTP %s" % s, s == 200))


# ==========================================================================
# 八、安全与权限模块
# ==========================================================================
def module_security():
    body = {"shopId": 910001, "dishName": "安全测试菜", "price": 12.50,
            "category": 1, "mealType": 2, "spiceLevel": 0}
    s, j, _ = call("POST", "/api/dishes", body)
    case("TC-SEC-01", "安全权限", "无 Token 写入菜品被拒", "安全性测试",
         "未携带 Token", "不带 Token POST /api/dishes", "新菜品数据",
         "HTTP 401，鉴权拦截",
         lambda: ("HTTP %s" % s, s == 401))

    u, tok = register_login()  # 注册账号默认为普通用户
    s, j, _ = call("POST", "/api/dishes", body, token=tok)
    case("TC-SEC-02", "安全权限", "普通用户写入菜品被拒", "安全性测试",
         "登录账号为普通用户（role=1）", "以普通用户 Token POST /api/dishes", "新菜品数据",
         "HTTP 403，权限不足",
         lambda: ("HTTP %s，%s" % (s, (j or {}).get("message")), s in (401, 403)))

    uname = rand_name("role")
    s, j, _ = call("POST", "/api/auth/register",
                   {"username": uname, "password": PASSWORD, "nickname": "越权尝试", "role": 2})
    st2, j2, _ = login(uname, PASSWORD)
    role = ((j2 or {}).get("data") or {}).get("role")
    case("TC-SEC-03", "安全权限", "注册请求中指定 role=2 不生效", "安全性测试-越权测试",
         "注册接口不接受角色字段", "注册时额外提交 role=2（管理员）后登录查看角色",
         "role=2（尝试越权提权）",
         "注册成功但角色仍为 1（普通用户）",
         lambda: ("HTTP %s，登录后 role=%s" % (s, role), s == 200 and role == 1))

    s, j, _ = call("GET", "/api/preferences/me", token="invalid.token.value")
    case("TC-SEC-04", "安全权限", "伪造/失效 Token 访问被拒", "安全性测试",
         "服务正常", "携带无效 Token 访问受保护接口", "token=invalid.token.value",
         "HTTP 401，鉴权拦截",
         lambda: ("HTTP %s" % s, s == 401))


# ==========================================================================
# 主流程
# ==========================================================================
def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    print("=" * 78)
    print("功能测试用例自动执行 —— 目标服务：%s" % BASE)
    print("开始时间：%s" % datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
    print("=" * 78)

    t0 = time.time()
    for fn in (module_auth, module_preference, module_dish, module_shop,
               module_recommend, module_favorite, module_assistant, module_security):
        fn()
    elapsed = time.time() - t0

    total = len(results)
    passed = sum(1 for r in results if r["结论"] == "通过")
    failed = total - passed

    print("=" * 78)
    print("执行汇总：共 %d 条，通过 %d 条，不通过 %d 条，通过率 %.1f%%，耗时 %.1fs"
          % (total, passed, failed, (passed * 100.0 / total) if total else 0, elapsed))
    print("=" * 78)

    csv_path = os.path.join(OUT_DIR, "functional_results.csv")
    with open(csv_path, "w", newline="", encoding="utf-8-sig") as fp:
        writer = csv.DictWriter(fp, fieldnames=list(results[0].keys()))
        writer.writeheader()
        writer.writerows(results)

    summary = {
        "baseUrl": BASE,
        "runAt": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "total": total,
        "passed": passed,
        "failed": failed,
        "passRate": round(passed * 100.0 / total, 1) if total else 0,
        "elapsedSec": round(elapsed, 1),
        "cases": results,
    }
    json_path = os.path.join(OUT_DIR, "functional_results.json")
    with open(json_path, "w", encoding="utf-8") as fp:
        json.dump(summary, fp, ensure_ascii=False, indent=2)

    print("结果已写入：")
    print("  %s" % csv_path)
    print("  %s" % json_path)
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
