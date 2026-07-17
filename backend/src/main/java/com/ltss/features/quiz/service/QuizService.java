package com.ltss.features.quiz.service;

import com.ltss.common.exception.ConflictException;
import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.common.response.PageResponse;
import com.ltss.features.auth.repository.AuthorizationRepository;
import com.ltss.features.auth.security.CurrentUserService;
import com.ltss.features.auth.service.AuditService;
import com.ltss.features.auth.service.ClientRequestInfo;
import com.ltss.features.place.entity.PlaceEntity;
import com.ltss.features.place.entity.PlaceStatus;
import com.ltss.features.place.repository.PlaceRepository;
import com.ltss.features.quiz.dto.*;
import com.ltss.features.quiz.entity.*;
import com.ltss.features.quiz.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizService {
    private static final Set<String> MANAGER_ROLES = Set.of("RELIC_MANAGER");
    private static final Set<String> REVIEWER_ROLES = Set.of("MODERATOR", "ADMINISTRATOR");

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final QuizAttemptRepository attemptRepository;
    private final PlaceRepository placeRepository;
    private final AuthorizationRepository authorizationRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final Clock clock;

    public QuizService(QuizRepository quizRepository, QuestionRepository questionRepository,
                       AnswerRepository answerRepository, QuizAttemptRepository attemptRepository,
                       PlaceRepository placeRepository, AuthorizationRepository authorizationRepository,
                       CurrentUserService currentUserService, AuditService auditService, Clock clock) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.attemptRepository = attemptRepository;
        this.placeRepository = placeRepository;
        this.authorizationRepository = authorizationRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<QuizSummaryResponse> published(Long placeId, int page, int size) {
        Page<QuizEntity> quizzes = quizRepository.findPublished(placeId, PageRequest.of(page, size));
        return PageResponse.from(quizzes.map(this::summary));
    }

    @Transactional(readOnly = true)
    public QuizSummaryResponse publishedDetail(Long quizId) {
        QuizEntity quiz = quizRepository.findById(quizId)
                .filter(item -> item.getStatus() == QuizStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz was not found"));
        return summary(quiz);
    }

    @Transactional(readOnly = true)
    public PageResponse<QuizSummaryResponse> mine(int page, int size) {
        Long actorId = currentUserService.requireUserId();
        requireManager(actorId);
        return PageResponse.from(quizRepository
                .findAllByCreatedByUserIdAndStatusNotOrderByUpdatedAtDesc(
                        actorId, QuizStatus.DELETED, PageRequest.of(page, size)
                ).map(this::summary));
    }

    @Transactional(readOnly = true)
    public QuizAuthorResponse managementDetail(Long quizId) {
        Long actorId = currentUserService.requireUserId();
        QuizEntity quiz = requireQuiz(quizId);
        boolean owner = Objects.equals(quiz.getCreatedByUserId(), actorId);
        boolean reviewer = hasAnyRole(actorId, REVIEWER_ROLES);
        if (!owner && !reviewer) throw new AccessDeniedException("Quiz is not accessible");
        return authorResponse(quiz);
    }

    @Transactional
    public QuizAuthorResponse create(QuizUpsertRequest request, ClientRequestInfo requestInfo) {
        Long actorId = currentUserService.requireUserId();
        requireManager(actorId);
        PlaceEntity place = requirePublishedPlace(request.placeId());
        validateRequestAggregate(request);
        QuizEntity quiz = quizRepository.save(new QuizEntity(
                place.getId(), actorId, normalize(request.title()), normalize(request.description()),
                request.timeLimitSeconds(), request.passingScorePercent()
        ));
        saveQuestions(quiz.getId(), request.questions());
        auditService.recordDomain(actorId, "QUIZ_CREATED", "QUIZ", quiz.getId(), requestInfo);
        return authorResponse(quiz, place);
    }

    @Transactional
    public QuizAuthorResponse update(Long quizId, QuizUpsertRequest request, ClientRequestInfo requestInfo) {
        Long actorId = currentUserService.requireUserId();
        requireManager(actorId);
        QuizEntity quiz = ownedLocked(quizId, actorId);
        requireEditable(quiz);
        requireVersion(quiz, request.version());
        if (!Objects.equals(quiz.getPlaceId(), request.placeId())) {
            throw new ConflictException("A quiz cannot be moved to another place");
        }
        PlaceEntity place = requirePublishedPlace(request.placeId());
        validateRequestAggregate(request);
        questionRepository.deleteAllByQuizId(quizId);
        quiz.update(actorId, normalize(request.title()), normalize(request.description()),
                request.timeLimitSeconds(), request.passingScorePercent());
        saveQuestions(quizId, request.questions());
        auditService.recordDomain(actorId, "QUIZ_UPDATED", "QUIZ", quizId, requestInfo);
        return authorResponse(quiz, place);
    }

    @Transactional
    public void delete(Long quizId, Integer version, ClientRequestInfo requestInfo) {
        Long actorId = currentUserService.requireUserId();
        requireManager(actorId);
        QuizEntity quiz = ownedLocked(quizId, actorId);
        requireEditable(quiz);
        requireVersion(quiz, version);
        if (attemptRepository.existsByQuizId(quizId)) {
            throw new ConflictException("Quiz with attempt history cannot be deleted");
        }
        quiz.delete(clock.instant());
        auditService.recordDomain(actorId, "QUIZ_DELETED", "QUIZ", quizId, requestInfo);
    }

    private void validateRequestAggregate(QuizUpsertRequest request) {
        for (QuestionUpsertRequest question : request.questions()) {
            if (question.answers().stream().filter(AnswerUpsertRequest::correct).count() != 1) {
                throw new ConflictException("Every question must have exactly one correct answer");
            }
        }
    }

    private void saveQuestions(Long quizId, List<QuestionUpsertRequest> requests) {
        for (int questionIndex = 0; questionIndex < requests.size(); questionIndex++) {
            QuestionUpsertRequest request = requests.get(questionIndex);
            QuestionEntity question = questionRepository.save(new QuestionEntity(
                    quizId, normalize(request.content()), normalize(request.explanation()),
                    questionIndex + 1, request.points()
            ));
            List<AnswerEntity> answers = new ArrayList<>();
            for (int answerIndex = 0; answerIndex < request.answers().size(); answerIndex++) {
                AnswerUpsertRequest answer = request.answers().get(answerIndex);
                answers.add(new AnswerEntity(question.getId(), normalize(answer.content()),
                        answer.correct(), answerIndex + 1));
            }
            answerRepository.saveAll(answers);
        }
    }

    private QuizAuthorResponse authorResponse(QuizEntity quiz) {
        return authorResponse(quiz, placeRepository.findById(quiz.getPlaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz place was not found")));
    }

    private QuizAuthorResponse authorResponse(QuizEntity quiz, PlaceEntity place) {
        List<QuestionEntity> questions = questionRepository
                .findAllByQuizIdAndActiveTrueOrderByDisplayOrderAsc(quiz.getId());
        Map<Long, List<AnswerEntity>> answers = answersByQuestion(questions);
        return new QuizAuthorResponse(
                quiz.getId(), quiz.getPlaceId(), place.getName(), quiz.getTitle(), quiz.getDescription(),
                quiz.getTimeLimitSeconds(), quiz.getPassingScorePercent(), quiz.getStatus(), quiz.getVersion(),
                quiz.getSubmittedAt(), quiz.getPublishedAt(), questions.stream().map(question ->
                        new QuizQuestionAuthorResponse(
                                question.getId(), question.getContent(), question.getExplanation(),
                                question.getDisplayOrder(), question.getPoints(),
                                answers.getOrDefault(question.getId(), List.of()).stream().map(answer ->
                                        new QuizAnswerAuthorResponse(answer.getId(), answer.getContent(),
                                                answer.isCorrect(), answer.getDisplayOrder())
                                ).toList()
                        )
                ).toList()
        );
    }

    private QuizSummaryResponse summary(QuizEntity quiz) {
        PlaceEntity place = placeRepository.findById(quiz.getPlaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz place was not found"));
        int count = questionRepository.findAllByQuizIdAndActiveTrueOrderByDisplayOrderAsc(quiz.getId()).size();
        return new QuizSummaryResponse(
                quiz.getId(), quiz.getPlaceId(), place.getName(), quiz.getTitle(), quiz.getDescription(),
                quiz.getTimeLimitSeconds(), quiz.getPassingScorePercent(), count, quiz.getStatus(),
                quiz.getVersion(), quiz.getPublishedAt(), quiz.getUpdatedAt()
        );
    }

    private Map<Long, List<AnswerEntity>> answersByQuestion(List<QuestionEntity> questions) {
        if (questions.isEmpty()) return Map.of();
        return answerRepository.findAllByQuestionIdInAndActiveTrueOrderByQuestionIdAscDisplayOrderAsc(
                questions.stream().map(QuestionEntity::getId).toList()
        ).stream().collect(Collectors.groupingBy(AnswerEntity::getQuestionId, LinkedHashMap::new, Collectors.toList()));
    }

    private QuizEntity requireQuiz(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz was not found"));
    }

    private QuizEntity ownedLocked(Long quizId, Long actorId) {
        QuizEntity quiz = quizRepository.findLockedById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz was not found"));
        if (!Objects.equals(quiz.getCreatedByUserId(), actorId)) {
            throw new AccessDeniedException("Quiz is not owned by this user");
        }
        return quiz;
    }

    private PlaceEntity requirePublishedPlace(Long placeId) {
        return placeRepository.findById(placeId)
                .filter(place -> place.getStatus() == PlaceStatus.PUBLISHED)
                .orElseThrow(() -> new ConflictException("Quiz must belong to a published place"));
    }

    private void requireEditable(QuizEntity quiz) {
        if (quiz.getStatus() != QuizStatus.DRAFT && quiz.getStatus() != QuizStatus.REJECTED) {
            throw new ConflictException("Quiz cannot be edited from its current state");
        }
    }

    private void requireVersion(QuizEntity quiz, Integer version) {
        if (version == null || !Objects.equals(quiz.getVersion(), version)) {
            throw new ConflictException("Quiz was changed by another request; reload and try again");
        }
    }

    private void requireManager(Long userId) {
        if (!hasAnyRole(userId, MANAGER_ROLES)) throw new AccessDeniedException("Relic Manager role is required");
    }

    private boolean hasAnyRole(Long userId, Set<String> roles) {
        return authorizationRepository.findEffectiveRoleCodes(userId).stream().anyMatch(roles::contains);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
