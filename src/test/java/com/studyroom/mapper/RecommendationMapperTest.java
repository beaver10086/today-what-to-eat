package com.studyroom.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.dto.DishView;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@MybatisTest(properties = "spring.datasource.url=jdbc:h2:mem:recommendation-mapper-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
class RecommendationMapperTest {
    @Autowired
    private RecommendationMapper recommendationMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createSchema() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS canteen (id BIGINT PRIMARY KEY,"
                + " canteen_name VARCHAR(50), status TINYINT, is_deleted TINYINT)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS shop (id BIGINT PRIMARY KEY, canteen_id BIGINT,"
                + " shop_name VARCHAR(50), status TINYINT, is_deleted TINYINT)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS dish (id BIGINT PRIMARY KEY, shop_id BIGINT,"
                + " dish_name VARCHAR(60), price DECIMAL(8,2), category TINYINT, meal_type TINYINT,"
                + " spice_level TINYINT, takeout_suitability TINYINT, data_source VARCHAR(100),"
                + " calorie INT, description VARCHAR(200), image_url VARCHAR(255),"
                + " is_signature TINYINT, is_available TINYINT, rating DECIMAL(2,1), is_deleted TINYINT)");
        jdbcTemplate.update("INSERT INTO canteen VALUES (1, '东区食堂', 1, 0)");
        jdbcTemplate.update("INSERT INTO shop VALUES (2, 1, '一号档口', 1, 0)");
        jdbcTemplate.update("INSERT INTO dish VALUES (101, 2, '适配晚餐', 12.00, 2, 4, 1, NULL, NULL, 500,"
                + " '测试菜品', NULL, 0, 1, 4.5, 0)");
        jdbcTemplate.update("INSERT INTO dish VALUES (102, 2, '辣度超标', 12.00, 2, 4, 3, NULL, NULL, 500,"
                + " '测试菜品', NULL, 0, 1, 4.8, 0)");
        jdbcTemplate.update("INSERT INTO dish VALUES (103, 2, '早餐菜', 12.00, 2, 1, 0, NULL, NULL, 400,"
                + " '测试菜品', NULL, 0, 1, 4.9, 0)");
    }

    @Test
    void candidateQueryAppliesMealSpiceBudgetAndAvailabilityFilters() {
        List<DishView> candidates = recommendationMapper.selectCandidates(4, 2,
                new BigDecimal("10.00"), new BigDecimal("20.00"), 0, List.of(), List.of());

        assertThat(candidates).extracting(DishView::id).containsExactly(101L);
        assertThat(candidates.get(0).shopName()).isEqualTo("一号档口");
        assertThat(candidates.get(0).canteenName()).isEqualTo("东区食堂");
    }
}
