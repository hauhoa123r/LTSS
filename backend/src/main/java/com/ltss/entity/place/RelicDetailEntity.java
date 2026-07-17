package com.ltss.entity.place;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "relic_details")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RelicDetailEntity {
    @Id
    @Column(name = "place_id")
    private Long placeId;

    @Column(name = "historical_period", length = 150)
    private String historicalPeriod;

    @Column(columnDefinition = "LONGTEXT")
    private String history;

    @Column(columnDefinition = "LONGTEXT")
    private String architecture;

    @Column(name = "recognition_level", length = 100)
    private String recognitionLevel;

    @Column(name = "recognized_at")
    private LocalDate recognizedAt;

    @Column(name = "preservation_note", columnDefinition = "TEXT")
    private String preservationNote;
}
