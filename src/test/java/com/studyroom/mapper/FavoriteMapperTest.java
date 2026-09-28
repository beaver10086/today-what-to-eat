package com.studyroom.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.dto.FavoriteView;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@MybatisTest(properties = "spring.datasource.url=jdbc:h2:mem:favorite-mapper-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
class FavoriteMapperTest {
    @Autowired
    private FavoriteMapper favoriteMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createSchema() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS canteen (id BIGINT PRIMARY KEY,"
                + " canteen_name VARCHAR(50), is_deleted TINYINT)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS shop (id BIGINT PRIMARY KEY, canteen_id BIGINT,"
                + " shop_name VARCHAR(50), location_desc VARCHAR(100), cover_url VARCHAR(255),"
                + " avg_price DECIMAL(8,2), is_deleted TINYINT)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS dish (id BIGINT PRIMARY KEY, shop_id BIGINT,"
                + " dish_name VARCHAR(60), image_url VARCHAR(255), price DECIMAL(8,2), is_deleted TINYINT)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS favorite (id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + " user_id BIGINT, target_type TINYINT, target_id BIGINT,"
                + " create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + " is_deleted TINYINT DEFAULT 0)");
        jdbcTemplate.update("INSERT INTO canteen VALUES (10, '北区食堂', 0)");
        jdbcTemplate.update("INSERT INTO shop VALUES (20, 10, '面食档', '一层', NULL, 15.00, 0)");
        jdbcTemplate.update("INSERT INTO dish VALUES (100, 20, '番茄面', NULL, 13.50, 0)");
        jdbcTemplate.update("INSERT INTO favorite (user_id,target_type,target_id,is_deleted) VALUES (7,1,100,0)");
        jdbcTemplate.update("INSERT INTO favorite (user_id,target_type,target_id,is_deleted) VALUES (7,2,20,0)");
        jdbcTemplate.update("INSERT INTO favorite (user_id,target_type,target_id,is_deleted) VALUES (8,1,100,0)");
    }

    @Test
    void favoriteIdsAndViewsAreScopedToUserAndSupportBothTargetTypes() {
        assertThat(favoriteMapper.selectTargetIds(7L, 1)).containsExactly(100L);
        assertThat(favoriteMapper.countByUser(7L, null)).isEqualTo(2);

        List<FavoriteView> views = favoriteMapper.selectFavoriteViews(7L, null, 0, 20);
        assertThat(views).extracting(FavoriteView::targetName).containsExactlyInAnyOrder("番茄面", "面食档");
        assertThat(favoriteMapper.selectFavoriteViews(7L, 2, 0, 20))
                .extracting(FavoriteView::targetName).containsExactly("面食档");
    }
}
