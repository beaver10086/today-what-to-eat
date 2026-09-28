package com.studyroom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyroom.common.BizException;
import com.studyroom.dto.TagRequest;
import com.studyroom.mapper.TagMapper;
import com.studyroom.model.Tag;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {
    @Mock private TagMapper tagMapper;
    private TagService service;

    @BeforeEach
    void setUp() {
        service = new TagService(tagMapper);
    }

    @Test
    void pagesTagsAndValidatesPageRange() {
        when(tagMapper.selectPage(0, 10)).thenReturn(List.of(tag(1L)));
        when(tagMapper.count()).thenReturn(1L);

        assertThat(service.page(1, 10).records().get(0).tagName()).isEqualTo("清淡");
        assertThatThrownBy(() -> service.page(1, 101)).isInstanceOf(BizException.class);
    }

    @Test
    void createsAndUpdatesTrimmedTagNames() {
        when(tagMapper.insert(any(Tag.class))).thenAnswer(invocation -> {
            Tag saved = invocation.getArgument(0);
            saved.setId(3L);
            return 1;
        });
        when(tagMapper.selectById(3L)).thenReturn(tag(3L));

        assertThat(service.create(new TagRequest(" 味道 ", 1, null)).tagName()).isEqualTo("味道");
        service.update(3L, new TagRequest(" 新标签 ", 2, 5));
        ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
        verify(tagMapper).update(captor.capture());
        assertThat(captor.getValue().getTagType()).isEqualTo(2);
    }

    @Test
    void deletingRequiresAnExistingTag() {
        when(tagMapper.selectById(1L)).thenReturn(tag(1L));
        service.delete(1L);
        verify(tagMapper).deleteById(1L);

        when(tagMapper.selectById(2L)).thenReturn(null);
        assertThatThrownBy(() -> service.delete(2L)).isInstanceOf(BizException.class);
    }

    private static Tag tag(Long id) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setTagName("清淡");
        tag.setTagType(1);
        tag.setSortOrder(0);
        return tag;
    }
}
