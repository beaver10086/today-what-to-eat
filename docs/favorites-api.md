# 收藏接口说明

所有接口使用 `/api` 前缀，需在请求头中携带 `Authorization: Bearer <token>`。所有读写均以当前登录用户为范围，不能操作其他用户的收藏。

## 收藏菜品或档口

`POST /api/favorites`

请求体：

```json
{
  "targetType": 1,
  "targetId": 920001
}
```

`targetType`：`1` 为菜品，`2` 为档口。重复收藏是幂等操作；重新收藏已取消的目标会恢复原收藏记录。目标不存在时返回 `404`。

## 取消收藏

`DELETE /api/favorites/{targetType}/{targetId}`

例如 `DELETE /api/favorites/1/920001` 取消菜品收藏，`DELETE /api/favorites/2/910001` 取消档口收藏。重复取消不会产生额外记录。

## 收藏列表

`GET /api/favorites?page=1&size=20`

可选 `targetType=1` 或 `targetType=2` 按目标类型筛选。返回标准分页结构 `records/total/page/size`，每条记录含目标名称、展示文案、封面、价格、档口和食堂信息。

## 批量读取收藏状态

`GET /api/favorites/ids?targetType=1`

返回当前用户收藏的目标 ID 数组，前端用它一次性初始化菜品或档口卡片状态，避免逐条请求。

## 接口校验场景

| 场景 | 预期结果 |
| --- | --- |
| 未登录读取、添加或取消收藏 | `401` |
| 添加菜品/档口收藏后再次添加 | 成功，列表仍只有一条 |
| 取消后再次取消 | 成功，收藏保持取消状态 |
| 取消后重新收藏 | 成功，收藏重新出现在列表中 |
| 收藏不存在的菜品/档口 | `404` |
| 使用其他用户的令牌读取列表 | 只返回该用户自己的收藏 |
| `targetType` 不为 1 或 2 | `400` |
| 分页 size 大于 100 或 page 小于 1 | `400` |
