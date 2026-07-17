package com.ltss.features.place.dto;

import com.ltss.features.place.entity.RelicDetailEntity;

import java.time.LocalDate;

public record RelicDetailResponse(
        String historicalPeriod,
        String history,
        String architecture,
        String recognitionLevel,
        LocalDate recognizedAt,
        String preservationNote
) {
    public static RelicDetailResponse from(RelicDetailEntity detail) {
        return new RelicDetailResponse(
                detail.getHistoricalPeriod(),
                detail.getHistory(),
                detail.getArchitecture(),
                detail.getRecognitionLevel(),
                detail.getRecognizedAt(),
                detail.getPreservationNote()
        );
    }
}
