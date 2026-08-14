# 标准化 Git 分支开发工作流

为了在编写核心业务逻辑（比如配置 MyBatis-Plus 实体类、调试 Redis 缓存或搭建 Spring Boot 核心模块）时，能够有条不紊地管理代码版本，请严格按照以下三个场景的工作流进行开发：

## 场景一：开发一个新功能 (Feature Workflow)
**适用情况：** 接到一个新需求，例如“开发登录功能”。

1. **拉取并创建分支**：永远从最新的 `develop` 创建功能分支。
```bash
# 切换回日常开发的主线分支 develop
git checkout develop

# 拉取云端最新的代码，确保你本地和云端同步，防止后续合并产生冲突
git pull

# 创建并自动切换到一个新的功能分支（-b 参数代表新建分支）
git checkout -b feature/spring-security-auth
```

2. **开发与规范提交**：在本地编写代码，并使用约定式提交。
```bash
# 将当前目录下所有新建或修改过的文件，添加到 Git 的“暂存区”备用
git add .

# 将暂存区的改动正式打包提交到本地仓库。-m 后面加上规范的说明（feat: 代表这是新增功能）
git commit -m "feat: 完成基于 JWT 的 Spring Security 登录鉴权接口"

# 首次推送建立远端分支
git push -u origin feature/spring-security-auth
```

3. **合并与清理**：功能开发完毕并自测通过后，合并回 `develop` 并删除本地的临时分支。
```bash
# 离开当前功能分支，切回到公共的开发主干 develop
git checkout develop

# 将你刚写好的登录功能代码，全部融入合并到当前的 develop 分支里
git merge feature/spring-security-auth

# 把本地合并好的、包含新功能的 develop 分支推送到 GitHub 云端保存
git push

# 功成身退，彻底删除本地的这个临时功能分支，保持本地环境整洁（-d 代表 delete 强制删除）
git branch -d feature/spring-security-auth

# 删除云端的临时分支（注意 --delete 参数）
git push origin --delete feature/spring-security-auth

```

## 场景二：修复测试阶段的 Bug (Bugfix Workflow)
**适用情况：** 功能合并到 `develop` 后，在自己测试或前端联调时发现了 Bug。

1. **拉取并创建分支**：从 `develop` 分支拉取。
```bash
# 切换回日常开发主分支 develop
git checkout develop

# 新建并切换到用于专门修复这个 Bug 的分支
git checkout -b bugfix/redis-cache-null
```

2. **修复与提交**：
```bash
# 把修复 Bug 产生的代码改动放入暂存区
git add .

# 提交修复代码（fix: 代表修复代码层面的缺陷或 Bug）
git commit -m "fix: 修复 Redis 缓存未命中时导致的空指针异常"
```

3. **合并与清理**：同场景一，合并回 `develop`，推送后删除分支。
```bash
# 切回开发主干分支
git checkout develop

# 把刚才修复好的代码合并进来
git merge bugfix/redis-cache-null

# 推送包含修复代码的 develop 分支到云端
git push

# 删掉本地无用的临时修复分支
git branch -d bugfix/redis-cache-null
```

## 场景三：修复生产环境的紧急 Bug (Hotfix Workflow)
**适用情况：** 代码已经发布到 `main` 分支（上线了），突然发现严重漏洞（比如数据库连接池泄露），需要立刻修复。

1. **从主分支拉取**：绝对不要从 `develop` 拉分支，必须从稳定的 `main` 拉取。
```bash
# 切换到代表线上生产环境最稳定的 main 分支
git checkout main

# 拉取云端最新代码，确保你现在改的代码和线上运行的代码完全一致
git pull

# 从 main 分支拉取并新建一个紧急修复分支（hotfix 通常用来指代紧急抢修）
git checkout -b hotfix/db-connection-leak
```

2. **紧急修复与提交**：
```bash
# 添加改动到暂存区
git add .

# 提交紧急修复的说明
git commit -m "fix: 紧急修复 HikariCP 数据库连接未正确释放的问题"
```

3. **双向合并（核心关键！）**：紧急修复不仅要合并回 `main` 止血，还必须合并回 `develop`，否则下次发布时这个 Bug 又会被带上去。
```bash
# ======== 第一步：合并回 main 止血，保证生产环境最新 ========
# 切换回 main 主分支
git checkout main
# 把紧急修复的代码合并进来
git merge hotfix/db-connection-leak
# 把修复后的最稳定代码直接推送到 GitHub
git push

# ======== 第二步：合并回 develop，防止后续版本又覆盖出 Bug ========
# 切换到日常开发分支
git checkout develop
# 把刚才同样的修复代码，也合并到日常开发分支里
git merge hotfix/db-connection-leak
# 把更新后的开发分支推送到 GitHub
git push

# ======== 第三步：过河拆桥，清理临时分支 ========
# 彻底删除本地的紧急修复分支
git branch -d hotfix/db-connection-leak
```
