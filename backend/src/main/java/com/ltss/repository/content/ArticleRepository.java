package com.ltss.repository.content;

import com.ltss.entity.content.ArticleEntity;
import com.ltss.entity.content.PublicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface ArticleRepository extends JpaRepository<ArticleEntity, Long> {
    @Query("""
            select article from ArticleEntity article
            join ArticleCategoryEntity category on category.id = article.categoryId
            where article.status = com.ltss.entity.content.PublicationStatus.PUBLISHED
              and category.active = true
              and (:categorySlug is null or category.slug = :categorySlug)
              and (:query is null
                   or lower(article.title) like lower(concat('%', :query, '%'))
                   or lower(coalesce(article.summary, '')) like lower(concat('%', :query, '%')))
            order by article.publishedAt desc
            """)
    Page<ArticleEntity> searchPublished(
            @Param("query") String query,
            @Param("categorySlug") String categorySlug,
            Pageable pageable
    );

    Optional<ArticleEntity> findBySlugAndStatus(String slug, PublicationStatus status);

    Page<ArticleEntity> findAllByAuthorUserIdAndStatusNotOrderByUpdatedAtDesc(
            Long authorUserId, PublicationStatus status, Pageable pageable
    );

    boolean existsBySlug(String slug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select article from ArticleEntity article where article.id = :id")
    Optional<ArticleEntity> findLockedById(@Param("id") Long id);
}
