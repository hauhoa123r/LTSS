package com.ltss.features.community.entity;

import lombok.*;
import java.io.Serializable;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class ReviewMediaId implements Serializable {
    private Long reviewId;
    private Long mediaAssetId;
}
