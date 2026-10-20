# 知阅 Vue 前端

Vue3、TypeScript、Vite与Pinia前端，通过Axios请求Spring Boot。
整合版本使用JWT与HttpOnly Cookie，写请求附带CSRF；实验二的Session实现属于早期实验阶段。

## 本地运行

先按[完整环境说明](../docs/setup.md)配置数据库和后端，以18085端口启动后端。
在本目录打开PowerShell，使用Node24：

```powershell
npm ci
$env:VITE_BACKEND_URL='http://127.0.0.1:18085'
npm run dev -- --host 127.0.0.1 --port 5179 --strictPort
```

访问 <http://127.0.0.1:5179/books>；后端CORS来源须设为`http://127.0.0.1:5179`。
浏览器、代理和CORS使用一致主机名，避免混用localhost与127.0.0.1。
源码Vite配置的默认开发/预览端口为5173/5174、默认代理后端为8081，
上面的显式参数会覆盖默认值；课程使用步骤统一按5179/18085配置。

## 验证与构建

```powershell
npm run typecheck
npm run test
npm run build
```

构建输出为`dist/`。在同一代理环境变量下运行：

```powershell
npm run preview -- --host 127.0.0.1 --port 5179 --strictPort
```

开发或预览服务在所属窗口按Ctrl+C停止。端口被占用时先核实运行实例，不自动换端口。
最新自动检查见[main CI](https://github.com/shiox212/zhiyue/actions/workflows/ci.yml?query=branch%3Amain)，
具体功能验证范围见[证据索引](../docs/evidence/evidence-index.md)。

## 部署与数据边界

源码新空库不包含原演示图书和用户。前端环境变量会进入浏览器产物，不能存放数据库口令或模型Key。
服务端密钥只写在接收者本机环境中。

静态部署可将`dist/`交给静态服务器，`/api/`反向代理至后端，未知页面路径回退至`index.html`。
若直接跨源访问后端，构建前设置`VITE_API_BASE_URL`，后端CORS必须匹配准确前端来源，
不得将通配来源与凭证混用。项目未做公网生产部署验收。
