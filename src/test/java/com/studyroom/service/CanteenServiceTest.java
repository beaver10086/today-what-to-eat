package com.studyroom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyroom.common.BizException;
import com.studyroom.dto.CanteenRequest;
import com.studyroom.mapper.CanteenMapper;
import com.studyroom.mapper.ShopMapper;
import com.studyroom.model.Canteen;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CanteenServiceTest {
    @Mock private CanteenMapper canteenMapper;
    @Mock private ShopMapper shopMapper;
    private CanteenService service;

    @BeforeEach
    void setUp() {
        service = new CanteenService(canteenMapper, shopMapper);
    }

    @Test
    void pagesMapsRowsAndValidatesBounds() {
        when(canteenMapper.selectPage(10, 10)).thenReturn(List.of(canteen(3L)));
        when(canteenMapper.count()).thenReturn(11L);

        assertThat(service.page(2, 10).records()).hasSize(1);
        assertThat(service.page(2, 10).total()).isEqualTo(11L);
        assertThatThrownBy(() -> service.page(0, 10)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.page(1, 101)).isInstanceOf(BizException.class);
    }

    @Test
    void createsWithDefaultsAndTrimsNames() {
        when(canteenMapper.insert(any(Canteen.class))).thenAnswer(invocation -> {
            Canteen saved = invocation.getArgument(0);
            saved.setId(5L);
            return 1;
        });

        var created = service.create(request(null, null));

        assertThat(created.id()).isEqualTo(5L);
        assertThat(created.canteenName()).isEqualTo("测试食堂");
        assertThat(created.sortOrder()).isZero();
        assertThat(created.status()).isEqualTo(1);
    }

    @Test
    void updatesAnExistingCanteenAndRejectsUnknownIds() {
        when(canteenMapper.selectById(5L)).thenReturn(canteen(5L));

        assertThat(service.update(5L, request(4, 0)).status()).isZero();
        verify(canteenMapper).update(any(Canteen.class));
        when(canteenMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.update(99L, request(null, null)))
                .isInstanceOf(BizException.class).hasMessage("食堂不存在");
    }

    @Test
    void deletingWithShopsIsRejectedAndEmptyCanteenCanBeDeleted() {
        when(canteenMapper.selectById(5L)).thenReturn(canteen(5L));
        when(shopMapper.countByCanteenId(5L)).thenReturn(2L, 0L);

        assertThatThrownBy(() -> service.delete(5L)).isInstanceOf(BizException.class)
                .hasMessageContaining("仍有档口");
        service.delete(5L);
        verify(canteenMapper).deleteById(5L);
        verify(canteenMapper, never()).deleteById(99L);
    }

    @Test
    void getRequiresAnExistingCanteen() {
        when(canteenMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(BizException.class);
    }

    private static CanteenRequest request(Integer sortOrder, Integer status) {
        return new CanteenRequest(" 测试食堂 ", " 校区 ", null, null, null,
                null, null, sortOrder, status);
    }

    private static Canteen canteen(Long id) {
        Canteen canteen = new Canteen();
        canteen.setId(id);
        canteen.setCanteenName("示例食堂");
        canteen.setCampus("校区");
        canteen.setStatus(1);
        canteen.setSortOrder(0);
        canteen.setIsDeleted(0);
        return canteen;
    }
}
