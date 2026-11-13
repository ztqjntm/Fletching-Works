# TODO

优先级：**P1** 影响体验 · **P2** 技术债 · **P3** 可选特性

## P1 · 已知问题

暂无。本轮修掉了三条数据安全边界：

- **爆炸/活塞/结构破坏留下的制箭台残条目**：`FletchingTableData.sweep()` 每 20 tick 扫描已加载区块，坐标上已经不是制箭台就把内容物掉在原地并删条目；不再依赖“同坐标再放一张桌子并打开”才会触发的 `getOrCreate`。
- **红石脉冲提前被方块顶掉时吞掉原方块**：`preRemoveSideEffects` 有物品形态的方块直接掉原方块物品；雪层单独按 `layers` 掉雪球；其他没有物品形态的方块走掉落表，不再用单一的 `asItem()`。
- **跨维度关闭界面时删错条目**：`FletchingTableMenu` 记录建菜单时的 `ServerLevel`，`stillValid()`/`removed()` 都查这个维度，不再用 `player.level()`。

更早的 1.0.0/1.0.1 修复（流动水不再被改成水源、珍珠箭无 owner 不消耗）仍然有效。

## P2 · 技术债

1. ~~**配方体系不是官方实现**~~ —— 已完成：`RecipeType` + `RecipeSerializer` + `RecipeInput` 齐了，配方本体在 `data/update-ft/recipe/*.json`，数据包可增删改、`/reload` 生效。注意 1.21.11 客户端没有配方表，产物格是服务端算完随容器同步的。
2. **配方查看器**：JEI 已支持（自定义分类 + 催化剂 + Fabric 配方同步）；REI / EMI 仍未适配。
3. **没有自动化测试**：`test NO-SOURCE`。最值得补的是 `FletchingRecipe.matches()` 的匹配矩阵和 `InputSlot.mayPlace` 的栏位过滤（gametest）。
4. **CI**：`build.yml` 在每次 push / PR 时构建；`release.yml` 在 `v*` tag 时校验版本、构建并发布 GitHub Release / CurseForge，release job 已配置 Gradle 缓存。目前仍缺自动化测试（见上一条）。

## P3 · 可选特性

1. **制箭台方块模型**
   - 评估结论（2026-10-31）：制箭台没有方块/物品模型、也没有方块实体，世界里的桌子就是原版的。
     - **A（0.2，高风险高收益）**：自建 `update-ft:fletching_table` 方块（blockstate + 模型 + 物品模型 + loot + 配方 + **村民 POI** + 兼容原版方块），可顺带加 BlockEntity 做桌面展示。
2. **BlockEntity**：容器持久化与多人共享已经在 1.0.1 用 `SavedData` 解决，剩下的只有漏斗自动化和桌面展示材料/进度——这两件必须有真方块实体，而 1.21.11 的制箭台没有专属方块类，只能等方案 A。

## 发布检查清单

- [x] 版本号更新（`gradle.properties` 的 `mod_version`）
- [x] 英文简介（Modrinth 用，之前那份已 revert，需要重写两三句即可）
- [ ] Modrinth 字段：Categories `Adventure / Equipment / Game Mechanics`、Environment `Client and server`、Loader `Fabric`、Game version `1.21.11`、License `MPL-2.0`
- [ ] GitHub Release 写 changelog（取 `CHANGELOG.md` 对应版本段落）
- [ ] 进游戏实测：雪地红石箭、水下红石箭、爆炸/活塞拆桌后的残条目清理、跨维度关闭界面

---
