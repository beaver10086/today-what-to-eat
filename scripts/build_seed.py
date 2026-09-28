from decimal import Decimal
from pathlib import Path

out = Path("docs/sql/V1.0.1__seed.sql")
canteens = [
    (900001, "演示一食堂", "演示校区", "一食堂主楼"),
    (900002, "演示二食堂", "演示校区", "教学区北侧"),
    (900003, "演示三食堂", "演示校区", "生活区东侧"),
]
shop_names = [
    "面食档口", "家常小炒", "麻辣香锅", "轻食沙拉", "饮品甜点",
    "早餐面点", "黄焖鸡档口", "地方风味", "米粉档口", "砂锅煲仔",
    "清真风味", "自选餐线", "烧腊档口", "素食档口", "西点咖啡",
]
shops = [(910001 + i, 900001 + i // 5, f"演示{shop_names[i]}", f"{i % 5 + 1}号窗口") for i in range(15)]
dish_names = [
    "番茄炒蛋盖饭", "宫保鸡丁饭", "红烧肉盖饭", "鱼香肉丝盖饭", "青椒肉丝盖饭",
    "香菇滑鸡饭", "黄焖鸡米饭", "咖喱鸡排饭", "黑椒牛柳饭", "糖醋里脊饭",
    "土豆牛腩饭", "照烧鸡腿饭", "卤肉饭", "扬州炒饭", "蛋炒饭", "扬州炒面",
    "牛肉拉面", "番茄鸡蛋面", "炸酱面", "重庆小面", "酸菜鱼米线", "红烧牛肉粉",
    "螺蛳粉", "过桥米线", "馄饨", "鲜肉包", "小笼包", "烧麦", "葱油饼", "茶叶蛋",
    "香煎鸡排", "奥尔良鸡腿", "孜然牛肉", "鱼香茄子", "麻婆豆腐", "地三鲜",
    "清炒时蔬", "蒜蓉西兰花", "手撕包菜", "酸辣土豆丝", "冬瓜排骨汤", "紫菜蛋花汤",
    "玉米排骨汤", "皮蛋瘦肉粥", "南瓜小米粥", "鸡蛋灌饼", "煎饺", "锅贴", "炸鸡块",
    "烤肠", "鸡肉沙拉", "牛肉藜麦碗", "金枪鱼三明治", "鸡蛋三明治", "水果酸奶碗",
    "全麦鸡胸卷", "素什锦饭", "香菇青菜面", "豆腐蔬菜煲", "清真牛肉饭", "孜然羊肉饭",
    "香辣鸡丁", "水煮肉片", "酸菜鱼", "红烧鱼块", "清蒸鸡腿", "蜜汁叉烧饭", "烧鸭饭",
    "咖喱牛肉饭", "番茄牛腩面", "担担面", "鸡丝凉面", "凉拌木耳", "凉拌黄瓜", "卤鸡腿",
    "卤蛋", "烤红薯", "玉米棒", "煎蛋", "豆浆", "鲜榨橙汁", "柠檬茶", "珍珠奶茶",
    "美式咖啡", "拿铁", "红豆双皮奶", "杨枝甘露", "蛋挞", "奶油泡芙", "红豆面包",
    "吐司套餐", "酸奶", "绿豆汤", "银耳羹", "芝麻汤圆", "水果拼盘", "凉皮", "肉夹馍",
    "鲜肉小馄饨", "牛肉煎饼",
]
tags = [
    (930001, "清淡", 1), (930002, "香辣", 1), (930003, "酸甜", 1), (930004, "麻辣", 1),
    (930005, "蒜香", 1), (930006, "奶香", 1), (930007, "咸鲜", 1), (930008, "不辣", 1),
    (930009, "川菜", 2), (930010, "家常菜", 2), (930011, "面食", 2), (930012, "粤式", 2),
    (930013, "轻食", 2), (930014, "清真风味", 2), (930015, "东北风味", 2),
    (930016, "鸡肉", 3), (930017, "牛肉", 3), (930018, "猪肉", 3), (930019, "鱼虾", 3),
    (930020, "鸡蛋", 3), (930021, "豆制品", 3), (930022, "蔬菜", 3), (930023, "奶制品", 3),
    (930024, "素食", 4), (930025, "清真", 4), (930026, "含花生", 4), (930027, "含海鲜", 4),
    (930028, "早餐", 5), (930029, "低脂", 5), (930030, "招牌推荐", 5),
]


def q(value):
    return "'" + str(value).replace("'", "''") + "'"


lines = [
    "-- 演示用种子数据：名称及价格均为虚构示例，落地时请替换为本校核实数据。",
    "-- 依赖 V1.0.0__init.sql；固定使用 900000 段 ID，便于反复导入和清理。",
    "USE `what_to_eat`;",
    "",
    "INSERT INTO `canteen` (`id`, `canteen_name`, `campus`, `location`, `open_time`, `close_time`, `sort_order`, `status`) VALUES",
]
lines += [f"({i}, {q(name)}, {q(campus)}, {q(location)}, '06:30:00', '21:00:00', {i - 900001}, 1)" + ("," if n < 2 else ";") for n, (i, name, campus, location) in enumerate(canteens)]
lines += ["", "INSERT INTO `shop` (`id`, `canteen_id`, `shop_name`, `location_desc`, `open_time`, `close_time`, `cuisine`, `avg_price`, `rating`, `rating_count`, `status`, `sort_order`) VALUES"]
for n, (shop_id, canteen_id, name, location) in enumerate(shops):
    lines.append(f"({shop_id}, {canteen_id}, {q(name)}, {q(location)}, '07:00:00', '20:30:00', {q(['家常菜','面食','川菜','轻食','饮品'][n % 5])}, {Decimal(10 + n % 9):.2f}, 4.5, 20, 1, {n % 5})" + ("," if n < 14 else ";"))

lines += ["", "INSERT INTO `tag` (`id`, `tag_name`, `tag_type`, `sort_order`) VALUES"]
for n, (tag_id, name, tag_type) in enumerate(tags):
    lines.append(f"({tag_id}, {q(name)}, {tag_type}, {n})" + ("," if n < len(tags) - 1 else ";"))

lines += ["", "INSERT INTO `dish` (`id`, `shop_id`, `dish_name`, `price`, `category`, `meal_type`, `spice_level`, `calorie`, `description`, `is_signature`, `is_available`) VALUES"]
for i, name in enumerate(dish_names):
    shop_id = 910001 + (i * 7 % 15)
    price = Decimal("6.00") + Decimal(i % 23) * Decimal("0.75")
    category = [1, 2, 3, 4, 5, 6, 7][i % 7]
    meal_type = [1, 2, 4, 6, 7, 15][i % 6]
    spice = [0, 1, 2, 3][i % 4]
    calorie = 180 + (i * 37 % 620)
    desc = f"演示菜品，辣度{spice}级；数据仅用于联调。"
    lines.append(f"({920001+i}, {shop_id}, {q(name)}, {price:.2f}, {category}, {meal_type}, {spice}, {calorie}, {q(desc)}, {1 if i % 11 == 0 else 0}, 1)" + ("," if i < 99 else ";"))

lines += ["", "INSERT INTO `dish_tag` (`id`, `dish_id`, `tag_id`) VALUES"]
tag_ids = [tag_id for tag_id, _, _ in tags]
associations = []
for i in range(100):
    first = tag_ids[i % len(tag_ids)]
    second = tag_ids[(i * 7 + 8) % len(tag_ids)]
    associations.extend([(first, 0), (second, 1)])
for n, (tag_id, offset) in enumerate(associations):
    dish_id = 920001 + n // 2
    lines.append(f"({940001+n}, {dish_id}, {tag_id})" + ("," if n < len(associations) - 1 else ";"))

lines += [
    "",
    "-- 安全清理演示数据时，请先确认库中没有业务数据引用这些固定 ID。",
    "-- DELETE FROM `dish_tag` WHERE `id` BETWEEN 940001 AND 940200;",
    "-- DELETE FROM `dish` WHERE `id` BETWEEN 920001 AND 920100;",
    "-- DELETE FROM `shop` WHERE `id` BETWEEN 910001 AND 910015;",
    "-- DELETE FROM `canteen` WHERE `id` BETWEEN 900001 AND 900003;",
]

out.write_text("\n".join(lines) + "\n", encoding="utf-8")
obsolete_notes = Path("docs/sql/V1.0.0__schema-notes.md")
if obsolete_notes.exists():
    obsolete_notes.unlink()
print(f"Wrote {len(canteens)} canteens, {len(shops)} shops, {len(dish_names)} dishes, {len(tags)} tags, and {len(associations)} dish-tag links.")
