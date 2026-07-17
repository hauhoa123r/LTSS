package com.ltss.features.quiz.service;

import com.ltss.common.exception.ConflictException;
import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.common.response.PageResponse;
import com.ltss.features.auth.repository.UserRepository;
import com.ltss.features.auth.security.CurrentUserService;
import com.ltss.features.auth.service.AuditService;
import com.ltss.features.auth.service.ClientRequestInfo;
import com.ltss.features.moderation.entity.NotificationEntity;
import com.ltss.features.moderation.repository.NotificationRepository;
import com.ltss.features.place.entity.PlaceEntity;
import com.ltss.features.place.entity.PlaceStatus;
import com.ltss.features.place.repository.PlaceRepository;
import com.ltss.features.quiz.dto.*;
import com.ltss.features.quiz.entity.*;
import com.ltss.features.quiz.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuizAttemptService {
    private static final BigDecimal MAX_DISTANCE_METERS = new BigDecimal("200.00");

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAttemptAnswerRepository attemptAnswerRepository;
    private final QuizBadgeRepository quizBadgeRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;
    private final QuizAggregateValidator aggregateValidator;
    private final AuditService auditService;
    private final Clock clock;

    public QuizAttemptService(
            QuizRepository quizRepository, QuestionRepository questionRepository,
            AnswerRepository answerRepository, QuizAttemptRepository attemptRepository,
            QuizAttemptAnswerRepository attemptAnswerRepository, QuizBadgeRepository quizBadgeRepository,
            BadgeRepository badgeRepository, UserBadgeRepository userBadgeRepository,
            PlaceRepository placeRepository, UserRepository userRepository,
            NotificationRepository notificationRepository, CurrentUserService currentUserService,
            QuizAggregateValidator aggregateValidator, AuditService auditService, Clock clock
    ) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.quizBadgeRepository = quizBadgeRepository;
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.placeRepository = placeRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
        this.aggregateValidator = aggregateValidator;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public QuizAttemptResponse start(Long quizId, StartAttemptRequest request, ClientRequestInfo requestInfo) {
        Long userId = currentUserService.requireUserId();
        QuizEntity quiz = quizRepository.findById(quizId)
                .filter(item -> item.getStatus() == QuizStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published quiz was not found"));
        aggregateValidator.validate(quiz);
        PlaceEntity place = placeRepository.findById(quiz.getPlaceId())
                .filter(item -> item.getStatus() == PlaceStatus.PUBLISHED)
                .orElseThrow(() -> new ConflictException("Quiz place is not currently available"));
        if (place.getLatitude() == null || place.getLongitude() == null) {
            throw new ConflictException("Quiz place does not have coordinates for location verification");
        }
        BigDecimal distance = distanceMeters(
                request.latitude(), request.longitude(), place.getLatitude(), place.getLongitude()
        );
        if (distance.compareTo(MAX_DISTANCE_METERS) > 0) {
            throw new ConflictException("You must be within 200 meters of the place to start this quiz");
        }

        List<QuestionEntity> questions = questionRepository
                .findAllByQuizIdAndActiveTrueOrderByDisplayOrderAsc(quizId);
        Map<Long, List<AnswerEntity>> answers = answersByQuestion(questions);
        String seed = UUID.randomUUID().toString();
        List<QuestionEntity> randomized = new ArrayList<>(questions);
        Collections.shuffle(randomized, random(seed, 0));
        BigDecimal totalPoints = questions.stream().map(QuestionEntity::getPoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        Instant now = clock.instant();
        QuizAttemptEntity attempt = attemptRepository.save(new QuizAttemptEntity(
                quizId, userId, seed, now, now.plusSeconds(quiz.getTimeLimitSeconds()), totalPoints, distance
        ));
        List<QuizAttemptAnswerEntity> snapshots = new ArrayList<>();
        for (int index = 0; index < randomized.size(); index++) {
            QuestionEntity question = randomized.get(index);
            AnswerEntity correct = answers.getOrDefault(question.getId(), List.of()).stream()
                    .filter(AnswerEntity::isCorrect).findFirst()
                    .orElseThrow(() -> new ConflictException("Quiz answer configuration is invalid"));
            snapshots.add(new QuizAttemptAnswerEntity(
                    attempt.getId(), question.getId(), index + 1, question.getContent(),
                    correct.getContent(), question.getExplanation()
            ));
        }
        attemptAnswerRepository.saveAll(snapshots);
        auditService.recordDomain(userId, "QUIZ_ATTEMPT_STARTED", "QUIZ_ATTEMPT", attempt.getId(), requestInfo);
        return response(attempt, quiz, snapshots);
    }

    @Transactional
    public QuizAttemptResponse detail(Long attemptId, ClientRequestInfo requestInfo) {
        QuizAttemptEntity attempt = ownedLocked(attemptId);
        if (attempt.getStatus() == QuizAttemptStatus.IN_PROGRESS
                && !clock.instant().isBefore(attempt.getExpiresAt())) {
            return grade(attempt, List.of(), true, requestInfo);
        }
        return response(attempt);
    }

    @Transactional
    public QuizAttemptResponse submit(Long attemptId, SubmitAttemptRequest request,
                                      ClientRequestInfo requestInfo) {
        QuizAttemptEntity attempt = ownedLocked(attemptId);
        if (attempt.getStatus() != QuizAttemptStatus.IN_PROGRESS) return response(attempt);
        boolean expired = !clock.instant().isBefore(attempt.getExpiresAt());
        return grade(attempt, expired ? List.of() : request.answers(), expired, requestInfo);
    }

    @Transactional(readOnly = true)
    public PageResponse<QuizAttemptSummaryResponse> history(int page, int size) {
        Long userId = currentUserService.requireUserId();
        return PageResponse.from(attemptRepository
                .findAllByUserIdOrderByStartedAtDesc(userId, PageRequest.of(page, size))
                .map(attempt -> {
                    QuizEntity quiz = requireQuiz(attempt.getQuizId());
                    return new QuizAttemptSummaryResponse(
                            attempt.getId(), quiz.getId(), quiz.getTitle(), attempt.getStatus(),
                            attempt.getStartedAt(), attempt.getSubmittedAt(), attempt.getScorePercent(), attempt.isPassed()
                    );
                }));
    }

    @Transactional(readOnly = true)
    public PageResponse<AwardedBadgeResponse> badges(int page, int size) {
        Long userId = currentUserService.requireUserId();
        return PageResponse.from(userBadgeRepository
                .findAllByUserIdOrderByAwardedAtDesc(userId, PageRequest.of(page, size))
                .map(this::badgeResponse));
    }

    private QuizAttemptResponse grade(QuizAttemptEntity attempt, List<AnswerSubmissionRequest> submissions,
                                      boolean autoSubmitted, ClientRequestInfo requestInfo) {
        Map<Integer, AnswerSubmissionRequest> byOrder = new HashMap<>();
        for (AnswerSubmissionRequest submission : submissions) {
            if (byOrder.put(submission.questionOrder(), submission) != null) {
                throw new ConflictException("Each question can only be answered once");
            }
        }
        List<QuizAttemptAnswerEntity> rows = attemptAnswerRepository
                .findAllByAttemptIdOrderByQuestionOrderAsc(attempt.getId());
        Set<Integer> validOrders = rows.stream().map(QuizAttemptAnswerEntity::getQuestionOrder).collect(Collectors.toSet());
        if (!validOrders.containsAll(byOrder.keySet())) throw new ConflictException("Submitted question is not part of this attempt");

        Set<Long> selectedIds = byOrder.values().stream().map(AnswerSubmissionRequest::selectedAnswerId).collect(Collectors.toSet());
        Map<Long, AnswerEntity> selectedAnswers = answerRepository.findAllById(selectedIds).stream()
                .filter(AnswerEntity::isActive).collect(Collectors.toMap(AnswerEntity::getId, Function.identity()));
        Map<Long, QuestionEntity> questions = questionRepository.findAllById(
                rows.stream().map(QuizAttemptAnswerEntity::getQuestionId).toList()
        ).stream().collect(Collectors.toMap(QuestionEntity::getId, Function.identity()));
        Instant now = clock.instant();
        BigDecimal score = BigDecimal.ZERO;
        for (QuizAttemptAnswerEntity row : rows) {
            AnswerSubmissionRequest submitted = byOrder.get(row.getQuestionOrder());
            if (submitted == null) continue;
            AnswerEntity answer = selectedAnswers.get(submitted.selectedAnswerId());
            if (answer == null || !Objects.equals(answer.getQuestionId(), row.getQuestionId())) {
                throw new ConflictException("Selected answer does not belong to the attempt question");
            }
            QuestionEntity question = questions.get(row.getQuestionId());
            if (question == null) throw new ConflictException("Attempt question configuration is unavailable");
            BigDecimal awarded = answer.isCorrect() ? question.getPoints().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2);
            row.answer(answer.getId(), answer.getContent(), answer.isCorrect(), awarded, now);
            score = score.add(awarded);
        }
        BigDecimal percent = attempt.getTotalPoints().signum() == 0 ? BigDecimal.ZERO.setScale(2)
                : score.multiply(new BigDecimal("100")).divide(attempt.getTotalPoints(), 2, RoundingMode.HALF_UP);
        QuizEntity quiz = requireQuiz(attempt.getQuizId());
        boolean passed = percent.compareTo(quiz.getPassingScorePercent()) >= 0;
        attempt.complete(autoSubmitted ? QuizAttemptStatus.AUTO_SUBMITTED : QuizAttemptStatus.SUBMITTED,
                now, score.setScale(2, RoundingMode.HALF_UP), percent, passed);
        awardBadges(attempt, quiz, now);
        auditService.recordDomain(attempt.getUserId(),
                autoSubmitted ? "QUIZ_ATTEMPT_AUTO_SUBMITTED" : "QUIZ_ATTEMPT_SUBMITTED",
                "QUIZ_ATTEMPT", attempt.getId(), requestInfo);
        return response(attempt, quiz, rows);
    }

    private void awardBadges(QuizAttemptEntity attempt, QuizEntity quiz, Instant now) {
        userRepository.findLockedById(attempt.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Attempt user was not found"));
        List<QuizBadgeEntity> configs = quizBadgeRepository.findAllByQuizId(quiz.getId()).stream()
                .filter(config -> attempt.getScorePercent().compareTo(config.getMinimumScorePercent()) >= 0).toList();
        Map<Long, BadgeEntity> badges = badgeRepository.findAllById(
                configs.stream().map(QuizBadgeEntity::getBadgeId).toList()
        ).stream().filter(BadgeEntity::isActive).collect(Collectors.toMap(BadgeEntity::getId, Function.identity()));
        for (QuizBadgeEntity config : configs) {
            BadgeEntity badge = badges.get(config.getBadgeId());
            if (badge == null || userBadgeRepository.existsByUserIdAndBadgeId(attempt.getUserId(), badge.getId())) continue;
            userBadgeRepository.save(new UserBadgeEntity(
                    attempt.getUserId(), badge.getId(), quiz.getId(), attempt.getId(), now
            ));
            notificationRepository.save(new NotificationEntity(
                    attempt.getUserId(), "Bạn nhận được huy hiệu mới",
                    "Bạn đã nhận huy hiệu “" + badge.getBadgeName() + "” từ quiz “" + quiz.getTitle() + "”.",
                    "/badges", now
            ));
        }
    }

    private QuizAttemptEntity ownedLocked(Long attemptId) {
        QuizAttemptEntity attempt = attemptRepository.findLockedById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt was not found"));
        if (!Objects.equals(attempt.getUserId(), currentUserService.requireUserId())) {
            throw new AccessDeniedException("Quiz attempt is not owned by this user");
        }
        return attempt;
    }

    private QuizAttemptResponse response(QuizAttemptEntity attempt) {
        return response(attempt, requireQuiz(attempt.getQuizId()),
                attemptAnswerRepository.findAllByAttemptIdOrderByQuestionOrderAsc(attempt.getId()));
    }

    private QuizAttemptResponse response(QuizAttemptEntity attempt, QuizEntity quiz,
                                         List<QuizAttemptAnswerEntity> rows) {
        boolean terminal = attempt.getStatus() != QuizAttemptStatus.IN_PROGRESS;
        Map<Long, List<AnswerEntity>> answers = terminal ? Map.of() : answersByQuestionIds(
                rows.stream().map(QuizAttemptAnswerEntity::getQuestionId).toList()
        );
        List<AttemptQuestionResponse> questions = rows.stream().map(row -> {
            List<AttemptAnswerChoiceResponse> choices = new ArrayList<>(answers
                    .getOrDefault(row.getQuestionId(), List.of()).stream()
                    .map(answer -> new AttemptAnswerChoiceResponse(answer.getId(), answer.getContent())).toList());
            Collections.shuffle(choices, random(attempt.getRandomizationSeed(), row.getQuestionId()));
            return new AttemptQuestionResponse(
                    row.getQuestionOrder(), row.getQuestionId(), row.getQuestionTextSnapshot(), choices,
                    terminal ? row.getSelectedAnswerId() : null,
                    terminal ? row.getSelectedAnswerTextSnapshot() : null,
                    terminal ? row.getCorrectAnswerTextSnapshot() : null,
                    terminal ? row.getExplanationSnapshot() : null,
                    terminal ? row.isCorrect() : null,
                    terminal ? row.getAwardedPoints() : null
            );
        }).toList();
        return new QuizAttemptResponse(
                attempt.getId(), quiz.getId(), quiz.getTitle(), attempt.getStatus(), attempt.getStartedAt(),
                attempt.getExpiresAt(), attempt.getSubmittedAt(), attempt.getDistanceToPlaceMeters(),
                attempt.getScore(), attempt.getTotalPoints(), attempt.getScorePercent(), attempt.isPassed(),
                questions, terminal ? userBadgeRepository.findAllByAwardedAttemptIdOrderByAwardedAtAsc(attempt.getId())
                        .stream().map(this::badgeResponse).toList() : List.of()
        );
    }

    private AwardedBadgeResponse badgeResponse(UserBadgeEntity award) {
        BadgeEntity badge = badgeRepository.findById(award.getBadgeId())
                .orElseThrow(() -> new ResourceNotFoundException("Badge was not found"));
        QuizEntity quiz = award.getAwardedByQuizId() == null ? null : quizRepository.findById(award.getAwardedByQuizId()).orElse(null);
        return new AwardedBadgeResponse(
                badge.getId(), badge.getBadgeCode(), badge.getBadgeName(), badge.getDescription(), badge.getIconUrl(),
                award.getAwardedAt(), award.getAwardedByQuizId(), quiz == null ? null : quiz.getTitle()
        );
    }

    private QuizEntity requireQuiz(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz was not found"));
    }

    private Map<Long, List<AnswerEntity>> answersByQuestion(List<QuestionEntity> questions) {
        return answersByQuestionIds(questions.stream().map(QuestionEntity::getId).toList());
    }

    private Map<Long, List<AnswerEntity>> answersByQuestionIds(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return answerRepository.findAllByQuestionIdInAndActiveTrueOrderByQuestionIdAscDisplayOrderAsc(ids)
                .stream().collect(Collectors.groupingBy(AnswerEntity::getQuestionId));
    }

    private Random random(String seed, long salt) {
        return new Random(31L * seed.hashCode() + salt);
    }

    private BigDecimal distanceMeters(BigDecimal latitude1, BigDecimal longitude1,
                                      BigDecimal latitude2, BigDecimal longitude2) {
        double lat1 = Math.toRadians(latitude1.doubleValue());
        double lat2 = Math.toRadians(latitude2.doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(longitude2.doubleValue() - longitude1.doubleValue());
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double meters = 6_371_000d * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(meters).setScale(2, RoundingMode.HALF_UP);
    }
}
