package com.ltss.service.administration;

import com.ltss.entity.auth.AuditLogEntity;
import com.ltss.repository.auth.UserRepository;
import com.ltss.entity.community.ReviewEntity;
import com.ltss.repository.community.ReviewRepository;
import com.ltss.entity.content.BusinessEntity;
import com.ltss.repository.content.*;
import com.ltss.repository.place.PlaceRepository;
import com.ltss.entity.quiz.QuizAttemptEntity;
import com.ltss.repository.quiz.QuizAttemptRepository;
import com.ltss.repository.quiz.QuizRepository;
import com.ltss.repository.tour.TourRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

@Component
public class AuditEntityDisplayNameResolver {
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final BusinessRepository businessRepository;
    private final ArticleRepository articleRepository;
    private final EventRepository eventRepository;
    private final BusinessPostRepository businessPostRepository;
    private final PromotionRepository promotionRepository;
    private final TourRepository tourRepository;
    private final ReviewRepository reviewRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    public AuditEntityDisplayNameResolver(
            UserRepository userRepository,
            PlaceRepository placeRepository,
            BusinessRepository businessRepository,
            ArticleRepository articleRepository,
            EventRepository eventRepository,
            BusinessPostRepository businessPostRepository,
            PromotionRepository promotionRepository,
            TourRepository tourRepository,
            ReviewRepository reviewRepository,
            QuizRepository quizRepository,
            QuizAttemptRepository quizAttemptRepository
    ) {
        this.userRepository = userRepository;
        this.placeRepository = placeRepository;
        this.businessRepository = businessRepository;
        this.articleRepository = articleRepository;
        this.eventRepository = eventRepository;
        this.businessPostRepository = businessPostRepository;
        this.promotionRepository = promotionRepository;
        this.tourRepository = tourRepository;
        this.reviewRepository = reviewRepository;
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
    }

    public Map<Long, String> resolve(Collection<AuditLogEntity> logs) {
        Map<String, Set<Long>> idsByType = new HashMap<>();
        logs.forEach(log -> {
            if (log.getEntityType() != null && log.getEntityId() != null) {
                idsByType.computeIfAbsent(log.getEntityType(), ignored -> new LinkedHashSet<>()).add(log.getEntityId());
            }
        });

        Map<String, String> names = new HashMap<>();
        collect(names, "USER", userRepository.findAllById(ids(idsByType, "USER")), user -> user.getId(), user -> user.getDisplayName());
        collect(names, "PLACE", placeRepository.findAllById(ids(idsByType, "PLACE")), place -> place.getId(), place -> place.getName());
        collect(names, "ARTICLE", articleRepository.findAllById(ids(idsByType, "ARTICLE")), article -> article.getId(), article -> article.getTitle());
        collect(names, "EVENT", eventRepository.findAllById(ids(idsByType, "EVENT")), event -> event.getId(), event -> event.getTitle());
        collect(names, "BUSINESS_POST", businessPostRepository.findAllById(ids(idsByType, "BUSINESS_POST")), post -> post.getId(), post -> post.getTitle());
        collect(names, "PROMOTION", promotionRepository.findAllById(ids(idsByType, "PROMOTION")), promotion -> promotion.getId(), promotion -> promotion.getTitle());
        collect(names, "TOUR", tourRepository.findAllById(ids(idsByType, "TOUR")), tour -> tour.getId(), tour -> tour.getTitle());
        collect(names, "QUIZ", quizRepository.findAllById(ids(idsByType, "QUIZ")), quiz -> quiz.getId(), quiz -> quiz.getTitle());
        resolveBusinesses(idsByType, names);
        resolveReviews(idsByType, names);
        resolveQuizAttempts(idsByType, names);

        Map<Long, String> namesByAuditId = new HashMap<>();
        logs.forEach(log -> {
            String name = names.get(key(log.getEntityType(), log.getEntityId()));
            if (name != null && !name.isBlank()) namesByAuditId.put(log.getId(), name);
        });
        return namesByAuditId;
    }

    private void resolveBusinesses(Map<String, Set<Long>> idsByType, Map<String, String> names) {
        List<BusinessEntity> businesses = businessRepository.findAllById(ids(idsByType, "BUSINESS"));
        Set<Long> placeIds = new LinkedHashSet<>();
        businesses.forEach(business -> placeIds.add(business.getPlaceId()));
        Map<Long, String> placeNames = new HashMap<>();
        placeRepository.findAllById(placeIds).forEach(place -> placeNames.put(place.getId(), place.getName()));
        businesses.forEach(business -> {
            String placeName = placeNames.get(business.getPlaceId());
            if (placeName != null) names.put(key("BUSINESS", business.getId()), placeName);
        });
    }

    private void resolveReviews(Map<String, Set<Long>> idsByType, Map<String, String> names) {
        List<ReviewEntity> reviews = reviewRepository.findAllById(ids(idsByType, "REVIEW"));
        Set<Long> userIds = new LinkedHashSet<>();
        reviews.forEach(review -> userIds.add(review.getUserId()));
        Map<Long, String> userNames = new HashMap<>();
        userRepository.findAllById(userIds).forEach(user -> userNames.put(user.getId(), user.getDisplayName()));
        reviews.forEach(review -> {
            String userName = userNames.get(review.getUserId());
            if (userName != null) names.put(key("REVIEW", review.getId()), "Đánh giá của " + userName);
        });
    }

    private void resolveQuizAttempts(Map<String, Set<Long>> idsByType, Map<String, String> names) {
        List<QuizAttemptEntity> attempts = quizAttemptRepository.findAllById(ids(idsByType, "QUIZ_ATTEMPT"));
        Set<Long> quizIds = new LinkedHashSet<>();
        attempts.forEach(attempt -> quizIds.add(attempt.getQuizId()));
        Map<Long, String> quizNames = new HashMap<>();
        quizRepository.findAllById(quizIds).forEach(quiz -> quizNames.put(quiz.getId(), quiz.getTitle()));
        attempts.forEach(attempt -> {
            String quizName = quizNames.get(attempt.getQuizId());
            if (quizName != null) names.put(key("QUIZ_ATTEMPT", attempt.getId()), quizName);
        });
    }

    private Set<Long> ids(Map<String, Set<Long>> idsByType, String type) {
        return idsByType.getOrDefault(type, Set.of());
    }

    private <T> void collect(Map<String, String> names, String type, Iterable<T> entities,
                             Function<T, Long> id, Function<T, String> name) {
        entities.forEach(entity -> names.put(key(type, id.apply(entity)), name.apply(entity)));
    }

    private String key(String type, Long id) {
        return type == null || id == null ? "" : type + ":" + id;
    }
}
