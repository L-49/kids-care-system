import json, urllib.request, urllib.error
from urllib.parse import quote
from datetime import datetime, timedelta

BASE = "http://localhost:9090/api"
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))
PREFIX = "__TEST_"


def call(method, path, token=None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with OPENER.open(req, timeout=15) as r:
            return r.status, json.loads(r.read().decode() or "{}")
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw or "{}")
        except Exception:
            return e.code, {"raw": raw}


def login(username, role):
    st, res = call("POST", "/auth/login", body={"username": username, "password": "123456", "role": role})
    assert st == 200 and res.get("code") == 200, (st, res)
    return res["data"]["token"], res["data"]["user"]


admin_t, _ = login("admin", "admin")
parent_t, parent_u = login("parent", "parent")
print("家长账号:", parent_u.get("realName"), parent_u.get("phone"))


def page(token, query=""):
    st, res = call("GET", "/notice/page?pageNum=1&pageSize=100" + query, token)
    return st, res, res.get("data") or []


def search_kw(token, kw):
    return page(token, "&keyword=" + quote(kw))


# 0. 清理历史测试数据
st, res, rows = search_kw(admin_t, PREFIX)
old = [n["id"] for n in rows]
if old:
    call("DELETE", "/notice/" + ",".join(str(i) for i in old), admin_t)
    print("清理历史测试公告:", old)

# 家长孩子所在班级
st, res = call("GET", "/child/page?pageNum=1&pageSize=5&keyword=" + quote("张小明"), admin_t)
kid = (res.get("data") or [{}])[0]
my_clazz = kid.get("clazzId")
print("家长孩子:", kid.get("name"), "班级 id =", my_clazz)

# 1. 造三条测试公告：草稿、定时(未来)、定向到别的班级(已发布)
future = (datetime.now() + timedelta(days=3)).strftime("%Y-%m-%d %H:%M:%S")
for payload in [
    {"title": PREFIX + "草稿", "type": "通知", "status": 0, "targetType": "all", "content": "草稿，家长不可见"},
    {"title": PREFIX + "定时未来", "type": "通知", "status": 1, "publishTime": future,
     "targetType": "all", "content": "定时未到点，家长不可见"},
]:
    st, res = call("POST", "/notice", admin_t, payload)
    print("创建 %s -> code=%s msg=%s" % (payload["title"], res.get("code"), res.get("msg")))

# 另一班级：取一个不等于家长班级的 clazzId
st, res = call("GET", "/clazz/all", admin_t)
clazzes = res.get("data") or []
other = next((c["id"] for c in clazzes if c["id"] != my_clazz), None)
if other:
    st, res = call("POST", "/notice", admin_t, {
        "title": PREFIX + "他班定向", "type": "通知", "status": 2,
        "publishTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "targetType": "class", "targetId": other, "content": "定向到别的班级"
    })
    print("创建 %s(班级 %s) -> code=%s msg=%s" % (PREFIX + "他班定向", other, res.get("code"), res.get("msg")))

# 2. 取回 id
st, res, rows = search_kw(admin_t, PREFIX)
mine = {n["title"]: n for n in rows}
print("\n园长可见的测试公告:")
for n in rows:
    print("  id=%s status=%s target=%s/%s title=%s" % (n["id"], n["status"], n.get("targetType"), n.get("targetId"), n["title"]))

# 3. 家长端各 status 探测（库里另有 8 条历史已发布公告）
print("\n[家长] /page 各 status 探测")
for q in ["", "&status=0", "&status=1", "&status=3"]:
    st, res, rows2 = page(parent_t, q)
    titles = [n["title"] for n in rows2]
    print("  status=%-6s total=%-3s 含草稿=%s 含定时=%s 含他班定向=%s 状态集=%s" % (
        q.replace("&status=", "") or "(空)", res.get("total"),
        (PREFIX + "草稿") in titles, (PREFIX + "定时未来") in titles,
        (PREFIX + "他班定向") in titles, sorted({n["status"] for n in rows2})))

# 4. 家长按 id 直读详情
print("\n[详情接口]")
for name in [PREFIX + "草稿", PREFIX + "定时未来", PREFIX + "他班定向"]:
    n = mine.get(name)
    if not n:
        continue
    st, res = call("GET", "/notice/%s" % n["id"], parent_t)
    st2, res2 = call("GET", "/notice/%s" % n["id"], admin_t)
    print("  %-16s 家长: code=%-4s msg=%-24s | 园长: code=%s (%s)" % (
        name, res.get("code"), res.get("msg"), res2.get("code"), (res2.get("data") or {}).get("title")))

# 5. 家长 /all 口径
st, res = call("GET", "/notice/all", parent_t)
lst = res.get("data") or []
print("\n[家长] /all -> %s 条, 状态集=%s, 测试公告=%s" % (
    len(lst), sorted({n["status"] for n in lst}),
    [n["title"] for n in lst if n["title"].startswith(PREFIX)]))

# 6. 清理
ids = [str(n["id"]) for n in rows]
if ids:
    st, res = call("DELETE", "/notice/" + ",".join(ids), admin_t)
    print("\n清理测试公告 -> code=%s msg=%s" % (res.get("code"), res.get("msg")))
