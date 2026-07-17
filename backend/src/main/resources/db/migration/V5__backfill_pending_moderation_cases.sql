-- Rebuild missing moderation cases for content that is genuinely pending.
-- Each statement is idempotent and captures the submitted content as JSON.

INSERT INTO moderation_records (
    submitted_by_user_id, article_id, target_snapshot, status, submission_note, submitted_at
)
SELECT
    article.author_user_id,
    article.id,
    JSON_OBJECT(
        'summary', article.summary,
        'body', article.content,
        'fields', JSON_ARRAY(
            JSON_OBJECT('label', 'Đường dẫn', 'value', article.slug),
            JSON_OBJECT('label', 'ID địa điểm', 'value', article.place_id),
            JSON_OBJECT('label', 'ID sự kiện', 'value', article.event_id)
        ),
        'questions', JSON_ARRAY()
    ),
    'PENDING',
    'Đồng bộ bản nội dung đang chờ duyệt.',
    COALESCE(article.submitted_at, article.created_at, CURRENT_TIMESTAMP(6))
FROM articles article
WHERE article.status = 'PENDING'
  AND NOT EXISTS (
      SELECT 1 FROM moderation_records record
      WHERE record.article_id = article.id AND record.status = 'PENDING'
  );

INSERT INTO moderation_records (
    submitted_by_user_id, event_id, target_snapshot, status, submission_note, submitted_at
)
SELECT
    event.created_by_user_id,
    event.id,
    JSON_OBJECT(
        'summary', NULL,
        'body', event.description,
        'fields', JSON_ARRAY(
            JSON_OBJECT('label', 'Bắt đầu', 'value', event.start_at),
            JSON_OBJECT('label', 'Kết thúc', 'value', event.end_at),
            JSON_OBJECT('label', 'Địa điểm tổ chức', 'value', event.location_note),
            JSON_OBJECT('label', 'ID địa điểm', 'value', event.place_id)
        ),
        'questions', JSON_ARRAY()
    ),
    'PENDING',
    'Đồng bộ bản nội dung đang chờ duyệt.',
    COALESCE(event.submitted_at, event.created_at, CURRENT_TIMESTAMP(6))
FROM events event
WHERE event.status = 'PENDING'
  AND NOT EXISTS (
      SELECT 1 FROM moderation_records record
      WHERE record.event_id = event.id AND record.status = 'PENDING'
  );

INSERT INTO moderation_records (
    submitted_by_user_id, business_post_id, target_snapshot, status, submission_note, submitted_at
)
SELECT
    post.created_by_user_id,
    post.id,
    JSON_OBJECT(
        'summary', post.summary,
        'body', post.content,
        'fields', JSON_ARRAY(
            JSON_OBJECT('label', 'Đường dẫn', 'value', post.slug),
            JSON_OBJECT('label', 'ID doanh nghiệp', 'value', post.business_id)
        ),
        'questions', JSON_ARRAY()
    ),
    'PENDING',
    'Đồng bộ bản nội dung đang chờ duyệt.',
    COALESCE(post.submitted_at, post.created_at, CURRENT_TIMESTAMP(6))
FROM business_posts post
WHERE post.status = 'PENDING'
  AND NOT EXISTS (
      SELECT 1 FROM moderation_records record
      WHERE record.business_post_id = post.id AND record.status = 'PENDING'
  );

INSERT INTO moderation_records (
    submitted_by_user_id, promotion_id, target_snapshot, status, submission_note, submitted_at
)
SELECT
    promotion.created_by_user_id,
    promotion.id,
    JSON_OBJECT(
        'summary', NULL,
        'body', promotion.description,
        'fields', JSON_ARRAY(
            JSON_OBJECT('label', 'Mã khuyến mãi', 'value', promotion.promo_code),
            JSON_OBJECT('label', 'Hình thức giảm', 'value', promotion.discount_type),
            JSON_OBJECT('label', 'Giá trị giảm', 'value', promotion.discount_value),
            JSON_OBJECT('label', 'Bắt đầu', 'value', promotion.start_at),
            JSON_OBJECT('label', 'Kết thúc', 'value', promotion.end_at),
            JSON_OBJECT('label', 'ID doanh nghiệp', 'value', promotion.business_id)
        ),
        'questions', JSON_ARRAY()
    ),
    'PENDING',
    'Đồng bộ bản nội dung đang chờ duyệt.',
    COALESCE(promotion.submitted_at, promotion.created_at, CURRENT_TIMESTAMP(6))
FROM promotions promotion
WHERE promotion.status = 'PENDING'
  AND NOT EXISTS (
      SELECT 1 FROM moderation_records record
      WHERE record.promotion_id = promotion.id AND record.status = 'PENDING'
  );

INSERT INTO moderation_records (
    submitted_by_user_id, review_id, target_snapshot, status, submission_note, submitted_at
)
SELECT
    review.user_id,
    review.id,
    JSON_OBJECT(
        'summary', NULL,
        'body', review.comment,
        'fields', JSON_ARRAY(
            JSON_OBJECT('label', 'Số sao', 'value', review.rating),
            JSON_OBJECT(
                'label', 'Loại đối tượng được đánh giá',
                'value', CASE
                    WHEN review.place_id IS NOT NULL THEN 'PLACE'
                    WHEN review.business_id IS NOT NULL THEN 'BUSINESS'
                    WHEN review.article_id IS NOT NULL THEN 'ARTICLE'
                    ELSE 'TOUR'
                END
            ),
            JSON_OBJECT(
                'label', 'ID đối tượng được đánh giá',
                'value', COALESCE(review.place_id, review.business_id, review.article_id, review.tour_id)
            )
        ),
        'questions', JSON_ARRAY()
    ),
    'PENDING',
    'Đồng bộ bản nội dung đang chờ duyệt.',
    COALESCE(review.submitted_at, review.created_at, CURRENT_TIMESTAMP(6))
FROM reviews review
WHERE review.status = 'PENDING'
  AND NOT EXISTS (
      SELECT 1 FROM moderation_records record
      WHERE record.review_id = review.id AND record.status = 'PENDING'
  );

INSERT INTO moderation_records (
    submitted_by_user_id, quiz_id, target_snapshot, status, submission_note, submitted_at
)
SELECT
    quiz.created_by_user_id,
    quiz.id,
    JSON_OBJECT(
        'summary', NULL,
        'body', quiz.description,
        'fields', JSON_ARRAY(
            JSON_OBJECT('label', 'Thời gian làm bài', 'value', CONCAT(quiz.time_limit_seconds, ' giây')),
            JSON_OBJECT('label', 'Điểm đạt', 'value', CONCAT(quiz.passing_score_percent, '%')),
            JSON_OBJECT('label', 'ID địa điểm', 'value', quiz.place_id)
        ),
        'questions', COALESCE(
            (
                SELECT JSON_ARRAYAGG(
                    JSON_OBJECT(
                        'content', question.content,
                        'explanation', question.explanation,
                        'points', question.points,
                        'answers', COALESCE(
                            (
                                SELECT JSON_ARRAYAGG(
                                    JSON_OBJECT('content', answer.content, 'correct', answer.is_correct)
                                )
                                FROM answers answer
                                WHERE answer.question_id = question.id AND answer.is_active = TRUE
                            ),
                            JSON_ARRAY()
                        )
                    )
                )
                FROM questions question
                WHERE question.quiz_id = quiz.id AND question.is_active = TRUE
            ),
            JSON_ARRAY()
        )
    ),
    'PENDING',
    'Đồng bộ bản nội dung đang chờ duyệt.',
    COALESCE(quiz.submitted_at, quiz.created_at, CURRENT_TIMESTAMP(6))
FROM quizzes quiz
WHERE quiz.status = 'PENDING'
  AND NOT EXISTS (
      SELECT 1 FROM moderation_records record
      WHERE record.quiz_id = quiz.id AND record.status = 'PENDING'
  );
