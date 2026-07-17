package com.ltss.mapper.moderation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ltss.entity.community.ReviewEntity;
import com.ltss.entity.content.*;
import com.ltss.dto.moderation.ModerationTargetContentResponse;
import com.ltss.entity.quiz.AnswerEntity;
import com.ltss.entity.quiz.QuestionEntity;
import com.ltss.entity.quiz.QuizEntity;
import com.ltss.repository.quiz.AnswerRepository;
import com.ltss.repository.quiz.QuestionRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ModerationTargetContentMapper {
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final ObjectMapper objectMapper;

    public ModerationTargetContentMapper(
            QuestionRepository questionRepository, AnswerRepository answerRepository, ObjectMapper objectMapper
    ) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> snapshot(Object target) {
        return objectMapper.convertValue(map(target), new TypeReference<>() {});
    }

    public ModerationTargetContentResponse fromSnapshot(Map<String, Object> snapshot) {
        return objectMapper.convertValue(snapshot, ModerationTargetContentResponse.class);
    }

    public ModerationTargetContentResponse map(Object target) {
        if (target instanceof ArticleEntity item) return article(item);
        if (target instanceof EventEntity item) return event(item);
        if (target instanceof BusinessPostEntity item) return post(item);
        if (target instanceof PromotionEntity item) return promotion(item);
        if (target instanceof QuizEntity item) return quiz(item);
        return review((ReviewEntity) target);
    }

    private ModerationTargetContentResponse article(ArticleEntity item) {
        return response(item.getSummary(), item.getContent(), List.of(
                field("Đường dẫn", item.getSlug()),
                field("ID địa điểm", item.getPlaceId()),
                field("ID sự kiện", item.getEventId())
        ));
    }

    private ModerationTargetContentResponse event(EventEntity item) {
        return response(null, item.getDescription(), List.of(
                field("Bắt đầu", item.getStartAt()),
                field("Kết thúc", item.getEndAt()),
                field("Địa điểm tổ chức", item.getLocationNote()),
                field("ID địa điểm", item.getPlaceId())
        ));
    }

    private ModerationTargetContentResponse post(BusinessPostEntity item) {
        return response(item.getSummary(), item.getContent(), List.of(
                field("Đường dẫn", item.getSlug()),
                field("ID doanh nghiệp", item.getBusinessId())
        ));
    }

    private ModerationTargetContentResponse promotion(PromotionEntity item) {
        return response(null, item.getDescription(), List.of(
                field("Mã khuyến mãi", item.getPromoCode()),
                field("Hình thức giảm", item.getDiscountType()),
                field("Giá trị giảm", item.getDiscountValue()),
                field("Bắt đầu", item.getStartAt()),
                field("Kết thúc", item.getEndAt()),
                field("ID doanh nghiệp", item.getBusinessId())
        ));
    }

    private ModerationTargetContentResponse review(ReviewEntity item) {
        return response(null, item.getComment(), List.of(
                field("Số sao", item.getRating()),
                field("Loại đối tượng được đánh giá", item.targetType()),
                field("ID đối tượng được đánh giá", item.targetId())
        ));
    }

    private ModerationTargetContentResponse quiz(QuizEntity item) {
        List<QuestionEntity> questions = questionRepository
                .findAllByQuizIdAndActiveTrueOrderByDisplayOrderAsc(item.getId());
        Map<Long, List<AnswerEntity>> answers = questions.isEmpty() ? Map.of()
                : answerRepository.findAllByQuestionIdInAndActiveTrueOrderByQuestionIdAscDisplayOrderAsc(
                        questions.stream().map(QuestionEntity::getId).toList()
                ).stream().collect(Collectors.groupingBy(
                        AnswerEntity::getQuestionId, LinkedHashMap::new, Collectors.toList()
                ));

        List<ModerationTargetContentResponse.Question> questionResponses = questions.stream().map(question ->
                new ModerationTargetContentResponse.Question(
                        question.getContent(), question.getExplanation(), question.getPoints(),
                        answers.getOrDefault(question.getId(), List.of()).stream()
                                .map(answer -> new ModerationTargetContentResponse.Answer(
                                        answer.getContent(), answer.isCorrect()
                                )).toList()
                )
        ).toList();

        return new ModerationTargetContentResponse(
                null, item.getDescription(), compact(List.of(
                        field("Thời gian làm bài", item.getTimeLimitSeconds() + " giây"),
                        field("Điểm đạt", item.getPassingScorePercent() + "%"),
                        field("ID địa điểm", item.getPlaceId())
                )), questionResponses
        );
    }

    private ModerationTargetContentResponse response(
            String summary, String body, List<ModerationTargetContentResponse.Field> fields
    ) {
        return new ModerationTargetContentResponse(summary, body, compact(fields), List.of());
    }

    private ModerationTargetContentResponse.Field field(String label, Object value) {
        return new ModerationTargetContentResponse.Field(label, value);
    }

    private List<ModerationTargetContentResponse.Field> compact(
            List<ModerationTargetContentResponse.Field> fields
    ) {
        return fields.stream().filter(field -> field.value() != null).toList();
    }
}
