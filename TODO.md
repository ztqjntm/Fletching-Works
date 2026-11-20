# TODO

优先级：**P1** 影响体验 · **P2** 技术债 · **P3** 可选特性。已完成项只记在 [CHANGELOG.md](CHANGELOG.md)，这里只留还没做的。

## P1 · 体验缺口

1. **配方引导**：自定义 `RecipeType` 没有配方书入口，箭头格也没有提示；不装 JEI 的玩家只能翻 README。考虑两条 advancement（第一支箭 / 红石射手），或给空槽加 tooltip。
2. **Mod Menu 集成**：装 Mod Menu 的玩家没有入口；配置目前只能改文件 + `/fletchingworks reload`。

## P2 · 技术债

1. **配方查看器**：JEI 已支持；REI / EMI 未适配。
2. **测试覆盖**：单元测试已覆盖配方匹配、栏位路由、存档 v1→v2 codec；箭矢效果和 `AbstractArrowMixin` 仍没有自动化测试（要 GameTest，成本更高）。
3. **配置界面**：`config/fletching-works.json` 已覆盖主要数值，但没有游戏内 GUI（见 P1 第 2 条）。

## P3 · 可选特性

1. **制箭台方块模型**：自建 `update-ft:fletching_table` 方块（blockstate + 模型 + 物品模型 + loot + 配方 + 村民 POI + 兼容原版方块），可顺带加 BlockEntity 做桌面展示。
2. **BlockEntity**：漏斗自动化和桌面展示材料/进度必须有真方块实体；1.21.11 的制箭台没有专属方块类，只能等上一条。
