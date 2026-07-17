package com.ltss.entity.community;

import lombok.*;
import java.io.Serializable;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class ReviewMediaId implements Serializable {
    private Long reviewId;
    private Long mediaAssetId;
}
