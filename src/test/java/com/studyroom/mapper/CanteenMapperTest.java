package com.studyroom.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.model.Canteen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@MybatisTest(properties = "spring.datasource.url=jdbc:h2:mem:mapper-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
class CanteenMapperTest {
    @Autowired private CanteenMapper canteenMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS canteen");
        jdbcTemplate.execute("CREATE TABLE canteen (id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + " canteen_name VARCHAR(50) NOT NULL, campus VARCHAR(50) NOT NULL, location VARCHAR(100),"
                + " open_time TIME, close_time TIME, description VARCHAR(500), cover_url VARCHAR(255),"
                + " sort_order INT NOT NULL DEFAULT 0, status TINYINT NOT NULL DEFAULT 1,"
                + " create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + " is_deleted TINYINT NOT NULL DEFAULT 0)");
    }

    @Test
    void inheritedMapperSupportsInsertReadUpdatePageAndLogicalDelete() {
        Canteen canteen = new Canteen();
        canteen.setCanteenName("测试食堂");
        canteen.setCampus("测试校区");
        canteenMapper.insert(canteen);

        Canteen stored = canteenMapper.selectById(canteen.getId());
        assertThat(stored.getCanteenName()).isEqualTo("测试食堂");
        assertThat(canteenMapper.selectPage(0, 10)).hasSize(1);
        assertThat(canteenMapper.count()).isEqualTo(1);

        stored.setLocation("一层");
        canteenMapper.update(stored);
        assertThat(canteenMapper.selectById(stored.getId()).getLocation()).isEqualTo("一层");

        canteenMapper.deleteById(stored.getId());
        assertThat(canteenMapper.selectById(stored.getId())).isNull();
        assertThat(canteenMapper.count()).isZero();
    }
}
