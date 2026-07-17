package com.ltss.support;

import com.ltss.features.auth.repository.AccountTokenRepository;
import com.ltss.features.auth.repository.AuditLogRepository;
import com.ltss.features.auth.repository.AuthorizationRepository;
import com.ltss.features.auth.repository.PasswordHistoryRepository;
import com.ltss.features.auth.repository.RoleRepository;
import com.ltss.features.auth.repository.UserRepository;
import com.ltss.features.auth.repository.UserRoleRepository;
import com.ltss.features.place.repository.PlaceCategoryRepository;
import com.ltss.features.place.repository.PlaceRepository;
import com.ltss.features.place.repository.RelicDetailRepository;
import com.ltss.features.place.repository.PlaceMediaRepository;
import com.ltss.features.place.repository.FavoriteRepository;
import com.ltss.features.place.repository.SearchHistoryRepository;
import com.ltss.features.content.repository.ArticleCategoryRepository;
import com.ltss.features.content.repository.ArticleRepository;
import com.ltss.features.content.repository.BusinessPostRepository;
import com.ltss.features.content.repository.BusinessRepository;
import com.ltss.features.content.repository.ContentMediaRepository;
import com.ltss.features.content.repository.EventRepository;
import com.ltss.features.content.repository.PromotionRepository;
import com.ltss.features.moderation.repository.ModerationRepository;
import com.ltss.features.moderation.repository.NotificationRepository;
import com.ltss.features.community.repository.ProhibitedTermRepository;
import com.ltss.features.community.repository.ReviewMediaRepository;
import com.ltss.features.community.repository.ReviewReplyRepository;
import com.ltss.features.community.repository.ReviewRepository;
import com.ltss.features.tour.repository.TourItemRepository;
import com.ltss.features.tour.repository.TourRepository;
import com.ltss.features.quiz.repository.AnswerRepository;
import com.ltss.features.quiz.repository.BadgeRepository;
import com.ltss.features.quiz.repository.QuestionRepository;
import com.ltss.features.quiz.repository.QuizAttemptAnswerRepository;
import com.ltss.features.quiz.repository.QuizAttemptRepository;
import com.ltss.features.quiz.repository.QuizBadgeRepository;
import com.ltss.features.quiz.repository.QuizRepository;
import com.ltss.features.quiz.repository.UserBadgeRepository;
import com.ltss.features.analytics.repository.EngagementEventRepository;
import com.ltss.features.analytics.repository.EngagementEventTypeRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MockitoBean(types = {
        UserRepository.class,
        RoleRepository.class,
        UserRoleRepository.class,
        PasswordHistoryRepository.class,
        AccountTokenRepository.class,
        AuthorizationRepository.class,
        AuditLogRepository.class,
        PlaceCategoryRepository.class,
        PlaceRepository.class,
        RelicDetailRepository.class,
        PlaceMediaRepository.class,
        FavoriteRepository.class,
        SearchHistoryRepository.class,
        ArticleCategoryRepository.class,
        ArticleRepository.class,
        BusinessRepository.class,
        BusinessPostRepository.class,
        ContentMediaRepository.class,
        EventRepository.class,
        PromotionRepository.class,
        ModerationRepository.class,
        NotificationRepository.class,
        ReviewRepository.class,
        ReviewReplyRepository.class,
        ReviewMediaRepository.class,
        ProhibitedTermRepository.class,
        TourRepository.class,
        TourItemRepository.class,
        QuizRepository.class,
        QuestionRepository.class,
        AnswerRepository.class,
        QuizAttemptRepository.class,
        QuizAttemptAnswerRepository.class,
        BadgeRepository.class,
        QuizBadgeRepository.class,
        UserBadgeRepository.class,
        EngagementEventRepository.class,
        EngagementEventTypeRepository.class
})
public @interface MockAuthRepositories {
}
