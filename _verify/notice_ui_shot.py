import json, time, base64, urllib.request, urllib.error
import websocket

CDP = "http://127.0.0.1:9222"
APP = "http://localhost:5173"
API = "http://localhost:9090/api"
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))
OUT = "D:/project/work2/kids-care-system/验证截图"


def api_login(username, role):
    req = urllib.request.Request(API + "/auth/login",
                                 data=json.dumps({"username": username, "password": "123456", "role": role}).encode(),
                                 method="POST")
    req.add_header("Content-Type", "application/json")
    with OPENER.open(req, timeout=15) as r:
        d = json.loads(r.read().decode())
    return d["data"]["token"], d["data"]["user"]


def ws_connect():
    for _ in range(20):
        try:
            pages = json.loads(OPENER.open(CDP + "/json/list", timeout=5).read().decode())
            page = next(p for p in pages if p["type"] == "page")
            return websocket.create_connection(page["webSocketDebuggerUrl"], timeout=30)
        except Exception:
            time.sleep(0.5)
    raise RuntimeError("CDP 未就绪")


class Session:
    def __init__(self):
        self.ws = ws_connect()
        self.id = 0

    def send(self, method, **params):
        self.id += 1
        self.ws.send(json.dumps({"id": self.id, "method": method, "params": params}))
        while True:
            msg = json.loads(self.ws.recv())
            if msg.get("id") == self.id:
                return msg.get("result", {})

    def goto(self, url, wait=3.0):
        self.send("Page.navigate", url=url)
        time.sleep(wait)

    def eval(self, expr):
        r = self.send("Runtime.evaluate", expression=expr, returnByValue=True, awaitPromise=True)
        return r.get("result", {}).get("value")

    def shot(self, path):
        data = self.send("Page.captureScreenshot", format="png", captureBeyondViewport=False)["data"]
        with open(path, "wb") as f:
            f.write(base64.b64decode(data))
        print("saved:", path)


def run(role, username, tag):
    token, user = api_login(username, role)
    s = Session()
    s.send("Emulation.setDeviceMetricsOverride", width=1500, height=950, deviceScaleFactor=1, mobile=False)
    s.goto(APP + "/", 2)
    # 清掉上一轮的登录态再注入，并整页刷新，保证布局组件按新用户初始化
    s.eval("localStorage.clear(); localStorage.setItem('kids_token', %s); localStorage.setItem('kids_user', %s); location.reload();" %
           (json.dumps(token), json.dumps(json.dumps(user, ensure_ascii=False))))
    time.sleep(3)
    s.goto(APP + "/#/notice", 4)
    print(tag, "URL:", s.eval("location.href"))
    print(tag, "表头:", s.eval("Array.from(document.querySelectorAll('.el-table th .cell')).map(e=>e.textContent.trim()).join(' | ')"))
    print(tag, "有无新增按钮:", s.eval("!!Array.from(document.querySelectorAll('button')).find(b=>b.textContent.includes('新增'))"))
    print(tag, "操作列:", s.eval("Array.from(document.querySelectorAll('.el-table th .cell')).some(e=>e.textContent.trim()==='操作')"))
    print(tag, "状态筛选下拉:", s.eval("Array.from(document.querySelectorAll('.el-form-item__label')).map(e=>e.textContent.trim()).join(',')"))
    s.shot("%s/notice-%s.png" % (OUT, tag))
    s.ws.close()


run("parent", "parent", "parent")
run("admin", "admin", "admin")
