package com.studyroom.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyroom.dto.FavoriteRequest;
import com.studyroom.mapper.FavoriteMapper;
import com.studyroom.model.Favorite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {
    @Mock
    private FavoriteMapper favoriteMapper;
    @Mock
    private DishService dishService;
    @Mock
    private ShopService shopService;

    private FavoriteService service;

    @BeforeEach
    void setUp() {
        service = new FavoriteService(favoriteMapper, dishService, shopService);
    }

    @Test
    void addingAnAlreadySavedDishIsIdempotent() {
        Favorite active = favorite(31L, 0);
        when(favoriteMapper.selectByUserAndTarget(7L, 1, 100L)).thenReturn(active);

        service.add(7L, new FavoriteRequest(1, 100L));

        verify(favoriteMapper, never()).insert(any(Favorite.class));
        verify(favoriteMapper, never()).setDeleted(31L, 0);
    }

    @Test
    void addingAgainRestoresACancelledFavorite() {
        when(favoriteMapper.selectByUserAndTarget(7L, 2, 20L)).thenReturn(favorite(41L, 1));

        service.add(7L, new FavoriteRequest(2, 20L));

        verify(favoriteMapper).setDeleted(41L, 0);
    }

    @Test
    void removingChangesOnlyTheCurrentUsersFavoriteState() {
        when(favoriteMapper.selectByUserAndTarget(7L, 1, 100L)).thenReturn(favorite(31L, 0));

        service.remove(7L, 1, 100L);

        verify(favoriteMapper).setDeleted(31L, 1);
    }

    private static Favorite favorite(Long id, int deleted) {
        Favorite favorite = new Favorite();
        favorite.setId(id);
        favorite.setIsDeleted(deleted);
        return favorite;
    }
}
