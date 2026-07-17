package com.ltss;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false"
})
@Testcontainers(disabledWithoutDocker = true)
class MySqlSchemaIntegrationTest {

    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4.10");

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName("ltss")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureMySql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    private final Flyway flyway;
    private final EntityManagerFactory entityManagerFactory;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    MySqlSchemaIntegrationTest(
            Flyway flyway,
            EntityManagerFactory entityManagerFactory,
            JdbcTemplate jdbcTemplate
    ) {
        this.flyway = flyway;
        this.entityManagerFactory = entityManagerFactory;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void flywayMigratesAndHibernateValidatesTheMySqlSchema() {
        assertThat(flyway.info().applied()).hasSize(2);
        assertThat(entityManagerFactory.isOpen()).isTrue();

        Integer domainTableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                """, Integer.class);
        Integer roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM roles",
                Integer.class
        );
        Integer inheritanceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role_inheritances",
                Integer.class
        );
        Integer removedCheckCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE constraint_schema = DATABASE()
                  AND constraint_type = 'CHECK'
                  AND constraint_name IN (
                    'chk_role_inheritance_not_self',
                    'chk_tours_not_self_source',
                    'chk_reviews_exactly_one_target',
                    'chk_engagement_exactly_one_target',
                    'chk_moderation_exactly_one_target',
                    'chk_panorama_hotspots_target',
                    'chk_panorama_hotspots_not_self'
                  )
                """, Integer.class);
        String generatedColumnExtra = jdbcTemplate.queryForObject("""
                SELECT extra
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'media_assets'
                  AND column_name = 'storage_key_hash'
                """, String.class);

        assertThat(domainTableCount).isEqualTo(49);
        assertThat(roleCount).isEqualTo(5);
        assertThat(inheritanceCount).isEqualTo(2);
        assertThat(removedCheckCount).isZero();
        assertThat(generatedColumnExtra).containsIgnoringCase("STORED GENERATED");
    }
}
