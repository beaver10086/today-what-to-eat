package com.studyroom.service;

import com.studyroom.common.BizException;
import com.studyroom.common.PageResult;
import com.studyroom.dto.TagRequest;
import com.studyroom.dto.TagView;
import com.studyroom.mapper.TagMapper;
import com.studyroom.model.Tag;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {
    private final TagMapper tagMapper;

    public TagService(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<TagView> page(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BizException(400, "分页参数范围无效");
        }
        List<TagView> rows = tagMapper.selectPage((long) (page - 1) * size, size)
                .stream().map(TagService::view).toList();
        return new PageResult<>(rows, tagMapper.count(), page, size);
    }

    @Transactional(readOnly = true)
    public TagView get(Long id) {
        return view(require(id));
    }

    @Transactional
    public TagView create(TagRequest request) {
        Tag tag = new Tag();
        apply(tag, request);
        tagMapper.insert(tag);
        return view(tag);
    }

    @Transactional
    public TagView update(Long id, TagRequest request) {
        Tag tag = require(id);
        apply(tag, request);
        tagMapper.update(tag);
        return view(require(id));
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        tagMapper.deleteById(id);
    }

    public Tag require(Long id) {
        Tag tag = id == null ? null : tagMapper.selectById(id);
        if (tag == null) {
            throw new BizException(404, "标签不存在");
        }
        return tag;
    }

    private static void apply(Tag tag, TagRequest request) {
        tag.setTagName(request.tagName().trim());
        tag.setTagType(request.tagType());
        tag.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private static TagView view(Tag tag) {
        return new TagView(tag.getId(), tag.getTagName(), tag.getTagType(), tag.getSortOrder());
    }
}
