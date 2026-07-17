package com.ltss.support;

import com.ltss.repository.auth.AccountTokenRepository;
import com.ltss.repository.auth.AuditLogRepository;
import com.ltss.repository.auth.AuthorizationRepository;
import com.ltss.repository.auth.PasswordHistoryRepository;
import com.ltss.repository.auth.RoleRepository;
import com.ltss.repository.auth.UserRepository;
import com.ltss.repository.auth.UserRoleRepository;
import com.ltss.repository.place.PlaceCategoryRepository;
import com.ltss.repository.place.PlaceRepository;
import com.ltss.repository.place.RelicDetailRepository;
import com.ltss.repository.place.PlaceMediaRepository;
import com.ltss.repository.place.FavoriteRepository;
import com.ltss.repository.place.SearchHistoryRepository;
import com.ltss.repository.content.ArticleCategoryRepository;
import com.ltss.repository.content.ArticleRepository;
import com.ltss.repository.content.BusinessPostRepository;
import com.ltss.repository.content.BusinessRepository;
import com.ltss.repository.content.ContentMediaRepository;
import com.ltss.repository.content.EventRepository;
import com.ltss.repository.content.PromotionRepository;
import com.ltss.repository.moderation.ModerationRepository;
import com.ltss.repository.moderation.NotificationRepository;
import com.ltss.repository.community.ProhibitedTermRepository;
import com.ltss.repository.community.ReviewMediaRepository;
import com.ltss.repository.community.ReviewReplyRepository;
import com.ltss.repository.community.ReviewRepository;
import com.ltss.repository.tour.TourItemRepository;
import com.ltss.repository.tour.TourRepository;
import com.ltss.repository.quiz.AnswerRepository;
import com.ltss.repository.quiz.BadgeRepository;
import com.ltss.repository.quiz.QuestionRepository;
import com.ltss.repository.quiz.QuizAttemptAnswerRepository;
import com.ltss.repository.quiz.QuizAttemptRepository;
import com.ltss.repository.quiz.QuizBadgeRepository;
import com.ltss.repository.quiz.QuizRepository;
import com.ltss.repository.quiz.UserBadgeRepository;
import com.ltss.repository.analytics.EngagementEventRepository;
import com.ltss.repository.analytics.EngagementEventTypeRepository;
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
