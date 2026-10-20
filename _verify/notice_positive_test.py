import json, urllib.request, urllib.error
from urllib.parse import quote
from datetime import datetime

BASE = "http://localhost:9090/api"
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))
PREFIX = "__TEST2_"


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


def login(u, role):
    st, res = call("POST", "/auth/login", body={"username": u, "password": "123456", "role": role})
    assert res.get("code") == 200, res
    return res["data"]["token"]


admin_t = login("admin", "admin")
teacher_t = login("teacher", "teacher")
parent_t = login("parent", "parent")

# 清理旧数据
st, res = call("GET", "/notice/page?pageNum=1&pageSize=50&keyword=" + quote(PREFIX), admin_t)
for n in res.get("data") or []:
    call("DELETE", "/notice/%s" % n["id"], admin_t)

# 家长孩子在本班(3)，造一条本班定向的已发布公告 + 一条草稿
now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
st, res = call("POST", "/notice", admin_t, {
    "title": PREFIX + "本班定向", "type": "通知", "status": 2, "publishTime": now,
    "targetType": "class", "targetId": 3, "content": "小班家长应能看到"
})
print("创建本班定向公告:", res.get("code"), res.get("msg"))
st, res = call("POST", "/notice", admin_t, {
    "title": PREFIX + "草稿", "type": "通知", "status": 0, "targetType": "all", "content": "草稿"
})
print("创建草稿:", res.get("code"), res.get("msg"))

st, res = call("GET", "/notice/page?pageNum=1&pageSize=50&keyword=" + quote(PREFIX), admin_t)
rows = res.get("data") or []
ids = [str(n["id"]) for n in rows]

st, res = call("GET", "/notice/page?pageNum=1&pageSize=50&keyword=" + quote(PREFIX), parent_t)
print("\n[家长] 搜索测试公告 ->", [(n["title"], n["status"], n.get("targetType"), n.get("targetId")) for n in res.get("data") or []])
print("  期望：只剩「本班定向」")

st, res = call("GET", "/notice/page?pageNum=1&pageSize=50&status=0&keyword=" + quote(PREFIX), teacher_t)
print("[教师] status=0 ->", [(n["title"], n["status"]) for n in res.get("data") or []], "期望含草稿")

for n in rows:
    if n["title"].endswith("本班定向"):
        st, res = call("GET", "/notice/%s" % n["id"], parent_t)
        print("[家长] 读本班定向详情 -> code=%s title=%s（期望 200）" % (res.get("code"), (res.get("data") or {}).get("title")))

if ids:
    st, res = call("DELETE", "/notice/" + ",".join(ids), admin_t)
    print("\n清理 -> code=%s msg=%s" % (res.get("code"), res.get("msg")))
