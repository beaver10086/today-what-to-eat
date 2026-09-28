package com.studyroom.service;

import com.studyroom.common.BizException;
import com.studyroom.common.PageResult;
import com.studyroom.dto.CanteenRequest;
import com.studyroom.dto.CanteenView;
import com.studyroom.mapper.CanteenMapper;
import com.studyroom.mapper.ShopMapper;
import com.studyroom.model.Canteen;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CanteenService {
    private final CanteenMapper canteenMapper;
    private final ShopMapper shopMapper;

    public CanteenService(CanteenMapper canteenMapper, ShopMapper shopMapper) {
        this.canteenMapper = canteenMapper;
        this.shopMapper = shopMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<CanteenView> page(int page, int size) {
        validatePage(page, size);
        List<CanteenView> rows = canteenMapper.selectPage((long) (page - 1) * size, size)
                .stream().map(CanteenService::view).toList();
        return new PageResult<>(rows, canteenMapper.count(), page, size);
    }

    @Transactional(readOnly = true)
    public CanteenView get(Long id) {
        return view(require(id));
    }

    @Transactional
    public CanteenView create(CanteenRequest request) {
        Canteen canteen = new Canteen();
        apply(canteen, request);
        canteenMapper.insert(canteen);
        return view(canteen);
    }

    @Transactional
    public CanteenView update(Long id, CanteenRequest request) {
        Canteen canteen = require(id);
        apply(canteen, request);
        canteenMapper.update(canteen);
        return view(require(id));
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        if (shopMapper.countByCanteenId(id) > 0) {
            throw new BizException(409, "食堂下仍有档口，不能删除");
        }
        canteenMapper.deleteById(id);
    }

    public Canteen require(Long id) {
        Canteen canteen = id == null ? null : canteenMapper.selectById(id);
        if (canteen == null) {
            throw new BizException(404, "食堂不存在");
        }
        return canteen;
    }

    private static void apply(Canteen canteen, CanteenRequest request) {
        canteen.setCanteenName(request.canteenName().trim());
        canteen.setCampus(request.campus().trim());
        canteen.setLocation(request.location());
        canteen.setOpenTime(request.openTime());
        canteen.setCloseTime(request.closeTime());
        canteen.setDescription(request.description());
        canteen.setCoverUrl(request.coverUrl());
        canteen.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        canteen.setStatus(request.status() == null ? 1 : request.status());
    }

    private static CanteenView view(Canteen canteen) {
        return new CanteenView(canteen.getId(), canteen.getCanteenName(), canteen.getCampus(),
                canteen.getLocation(), canteen.getOpenTime(), canteen.getCloseTime(),
                canteen.getDescription(), canteen.getCoverUrl(), canteen.getSortOrder(), canteen.getStatus());
    }

    private static void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BizException(400, "分页参数范围无效");
        }
    }
}
