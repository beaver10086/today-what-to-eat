package com.studyroom.service;

import com.studyroom.common.BizException;
import com.studyroom.common.PageResult;
import com.studyroom.dto.DishQuery;
import com.studyroom.dto.DishRequest;
import com.studyroom.dto.DishView;
import com.studyroom.mapper.DishMapper;
import com.studyroom.mapper.DishTagMapper;
import com.studyroom.mapper.OperationLogMapper;
import com.studyroom.model.Dish;
import com.studyroom.model.DishTag;
import com.studyroom.model.OperationLog;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DishService {
    private final DishMapper dishMapper;
    private final ShopService shopService;
    private final CanteenService canteenService;
    private final TagService tagService;
    private final DishTagMapper dishTagMapper;
    private final OperationLogMapper operationLogMapper;

    public DishService(DishMapper dishMapper, ShopService shopService, CanteenService canteenService,
                       TagService tagService, DishTagMapper dishTagMapper,
                       OperationLogMapper operationLogMapper) {
        this.dishMapper = dishMapper;
        this.shopService = shopService;
        this.canteenService = canteenService;
        this.tagService = tagService;
        this.dishTagMapper = dishTagMapper;
        this.operationLogMapper = operationLogMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<DishView> search(DishQuery query) {
        if (query.getPage() < 1 || query.getSize() < 1 || query.getSize() > 100) {
            throw new BizException(400, "分页参数范围无效");
        }
        if (query.getMinPrice() != null && query.getMaxPrice() != null
                && query.getMinPrice().compareTo(query.getMaxPrice()) > 0) {
            throw new BizException(400, "最低价格不能高于最高价格");
        }
        long offset = (long) (query.getPage() - 1) * query.getSize();
        return new PageResult<>(dishMapper.search(query, offset, query.getSize()),
                dishMapper.countMatches(query), query.getPage(), query.getSize());
    }

    @Transactional(readOnly = true)
    public PageResult<DishView> adminPage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BizException(400, "分页参数范围无效");
        }
        List<DishView> rows = dishMapper.selectPage((long) (page - 1) * size, size)
                .stream().map(this::view).toList();
        return new PageResult<>(rows, dishMapper.count(), page, size);
    }

    @Transactional(readOnly = true)
    public DishView get(Long id) {
        return view(require(id));
    }

    @Transactional(readOnly = true)
    public DishView random(DishQuery query) {
        query.setPage(1);
        query.setSize(1);
        PageResult<DishView> matches = search(query);
        if (matches.total() == 0) {
            throw new BizException(404, "当前条件下没有可推荐的菜品");
        }
        long offset = ThreadLocalRandom.current().nextLong(matches.total());
        List<DishView> selected = dishMapper.search(query, offset, 1);
        if (selected.isEmpty()) {
            throw new BizException(404, "当前条件下没有可推荐的菜品");
        }
        return selected.get(0);
    }

    /** Imports new dishes only; validation and inserts share one transaction. */
    @Transactional
    public ImportResult importCsv(byte[] bytes, Long operatorId, String ip) {
        if (bytes == null || bytes.length == 0 || bytes.length > 512 * 1024) {
            throw new BizException(400, "CSV 文件不能为空且不能超过 512 KB");
        }
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            throw new BizException(400, "CSV 文件必须使用 UTF-8 编码");
        }
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        List<CsvRow> rows = parseCsv(text);
        List<String> header = List.of("shopId", "dishName", "price", "category", "mealType",
                "spiceLevel", "takeoutSuitability", "dataSource", "calorie", "description",
                "imageUrl", "isAvailable");
        if (rows.isEmpty() || !rows.get(0).cells().equals(header)) {
            throw new BizException(400, "CSV 表头与模板不一致");
        }
        if (rows.size() < 2 || rows.size() > 501) {
            throw new BizException(400, "CSV 须包含 1 至 500 条菜品");
        }
        List<DishRequest> requests = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (CsvRow row : rows.subList(1, rows.size())) {
            List<String> cells = row.cells();
            if (cells.size() != header.size()) {
                throw new BizException(400, "第 " + row.line() + " 行列数不正确");
            }
            int line = row.line();
            Long shopId = positiveLong(cells.get(0), line);
            String name = cells.get(1).trim();
            BigDecimal price = positivePrice(cells.get(2), line);
            Integer category = boundedInt(cells.get(3), 1, 7, line, false);
            Integer meal = boundedInt(cells.get(4), 1, 15, line, true);
            Integer spice = boundedInt(cells.get(5), 0, 3, line, true);
            Integer takeout = boundedInt(cells.get(6), 0, 2, line, true);
            String source = cells.get(7).trim();
            Integer calorie = boundedInt(cells.get(8), 0, Integer.MAX_VALUE, line, true);
            String description = cells.get(9).trim();
            String imageUrl = cells.get(10).trim();
            Integer available = boundedInt(cells.get(11), 0, 1, line, true);
            if (name.isEmpty() || name.length() > 60 || source.isEmpty() || source.length() > 100
                    || description.length() > 300 || imageUrl.length() > 255) {
                throw new BizException(400, "第 " + line + " 行名称、来源或文字长度不合法");
            }
            shopService.require(shopId);
            if (!seen.add(shopId + ":" + name.toLowerCase(Locale.ROOT))
                    || dishMapper.findActiveIdByShopAndName(shopId, name) != null) {
                throw new BizException(409, "第 " + line + " 行菜品已存在于该档口");
            }
            requests.add(new DishRequest(shopId, name, price, category, meal, spice, takeout,
                    source, calorie, description, imageUrl, 0, available));
        }
        Long firstId = null;
        for (DishRequest request : requests) {
            DishView created = create(request);
            if (firstId == null) {
                firstId = created.id();
            }
        }
        OperationLog log = new OperationLog();
        log.setOperatorId(operatorId);
        log.setModule("dish");
        log.setAction(4);
        log.setTargetId(firstId);
        log.setAfterData("{\"imported\":" + requests.size() + "}");
        log.setIp(ip);
        operationLogMapper.insert(log);
        return new ImportResult(requests.size());
    }

    private static List<CsvRow> parseCsv(String text) {
        List<CsvRow> rows = new ArrayList<>();
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        boolean closedQuote = false;
        int line = 1;
        int rowLine = 1;
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        for (int i = 0; i <= normalized.length(); i++) {
            char ch = i == normalized.length() ? '\n' : normalized.charAt(i);
            if (quoted) {
                if (i == normalized.length()) {
                    throw new BizException(400, "第 " + rowLine + " 行引号未闭合");
                }
                if (ch == '"') {
                    if (i + 1 < normalized.length() && normalized.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        quoted = false;
                        closedQuote = true;
                    }
                } else {
                    cell.append(ch);
                    if (ch == '\n') {
                        line++;
                    }
                }
            } else if (ch == ',' || ch == '\n') {
                cells.add(cell.toString());
                cell.setLength(0);
                closedQuote = false;
                if (ch == '\n') {
                    if (cells.size() != 1 || !cells.get(0).isBlank()) {
                        rows.add(new CsvRow(rowLine, List.copyOf(cells)));
                    }
                    cells.clear();
                    rowLine = ++line;
                }
            } else if (ch == '"' && cell.length() == 0 && !closedQuote) {
                quoted = true;
            } else if (ch == '"' || (closedQuote && !Character.isWhitespace(ch))) {
                throw new BizException(400, "第 " + rowLine + " 行 CSV 引号格式不正确");
            } else if (!closedQuote) {
                cell.append(ch);
            }
        }
        return rows;
    }

    private static Long positiveLong(String value, int line) {
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed > 0) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // Report the same row error for empty, non-numeric and out-of-range values.
        }
        throw new BizException(400, "第 " + line + " 行档口 ID 不合法");
    }

    private static BigDecimal positivePrice(String value, int line) {
        try {
            BigDecimal parsed = new BigDecimal(value.trim());
            if (parsed.signum() > 0 && parsed.scale() <= 2 && parsed.compareTo(new BigDecimal("999999.99")) <= 0) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // A clear row error follows.
        }
        throw new BizException(400, "第 " + line + " 行价格不合法");
    }

    private static Integer boundedInt(String value, int min, int max, int line, boolean optional) {
        if (value.isBlank() && optional) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed >= min && parsed <= max) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // A clear row error follows.
        }
        throw new BizException(400, "第 " + line + " 行数字字段不合法");
    }

    public record ImportResult(int imported) { }

    private record CsvRow(int line, List<String> cells) { }

    @Transactional
    public DishView create(DishRequest request) {
        shopService.require(request.shopId());
        Dish dish = new Dish();
        apply(dish, request);
        dish.setRating(BigDecimal.ZERO);
        dish.setRatingCount(0);
        dish.setLikeCount(0);
        dish.setDislikeCount(0);
        dish.setFavoriteCount(0);
        dish.setRecommendCount(0);
        dishMapper.insert(dish);
        return view(dish);
    }

    @Transactional
    public DishView update(Long id, DishRequest request) {
        shopService.require(request.shopId());
        Dish dish = require(id);
        apply(dish, request);
        dishMapper.update(dish);
        return view(require(id));
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        dishMapper.deleteById(id);
        dishTagMapper.deleteActiveByDishId(id);
    }

    @Transactional
    public List<Long> replaceTags(Long dishId, List<Long> requestedTagIds) {
        require(dishId);
        Set<Long> desired = new LinkedHashSet<>(requestedTagIds == null ? List.of() : requestedTagIds);
        desired.forEach(tagService::require);
        List<DishTag> existing = dishTagMapper.selectAllByDishId(dishId);
        Set<Long> existingTagIds = new LinkedHashSet<>();
        for (DishTag link : existing) {
            existingTagIds.add(link.getTagId());
            int deleted = desired.contains(link.getTagId()) ? 0 : 1;
            if (link.getIsDeleted() != deleted) {
                dishTagMapper.setDeleted(link.getId(), deleted);
            }
        }
        for (Long tagId : desired) {
            if (!existingTagIds.contains(tagId)) {
                DishTag link = new DishTag();
                link.setDishId(dishId);
                link.setTagId(tagId);
                dishTagMapper.insert(link);
            }
        }
        return new ArrayList<>(desired);
    }

    @Transactional(readOnly = true)
    public List<Long> tagIds(Long dishId) {
        require(dishId);
        return dishTagMapper.selectByDishId(dishId).stream().map(DishTag::getTagId).toList();
    }

    public Dish require(Long id) {
        Dish dish = id == null ? null : dishMapper.selectById(id);
        if (dish == null) {
            throw new BizException(404, "菜品不存在");
        }
        return dish;
    }

    private void apply(Dish dish, DishRequest request) {
        dish.setShopId(request.shopId());
        dish.setDishName(request.dishName().trim());
        dish.setPrice(request.price());
        dish.setCategory(request.category());
        dish.setMealType(request.mealType() == null ? 15 : request.mealType());
        dish.setSpiceLevel(request.spiceLevel() == null ? 0 : request.spiceLevel());
        dish.setTakeoutSuitability(request.takeoutSuitability());
        dish.setDataSource(request.dataSource());
        dish.setCalorie(request.calorie());
        dish.setDescription(request.description());
        dish.setImageUrl(request.imageUrl());
        dish.setIsSignature(request.isSignature() == null ? 0 : request.isSignature());
        dish.setIsAvailable(request.isAvailable() == null ? 1 : request.isAvailable());
    }

    private DishView view(Dish dish) {
        var shop = shopService.require(dish.getShopId());
        var canteen = canteenService.require(shop.getCanteenId());
        return new DishView(dish.getId(), dish.getShopId(), shop.getShopName(), canteen.getId(),
                canteen.getCanteenName(), dish.getDishName(), dish.getPrice(), dish.getCategory(),
                dish.getMealType(), dish.getSpiceLevel(), dish.getCalorie(), dish.getDescription(),
                dish.getImageUrl(), dish.getIsSignature(), dish.getIsAvailable(), dish.getRating(),
                dish.getTakeoutSuitability(), dish.getDataSource());
    }
}
