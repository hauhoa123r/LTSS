package com.ltss.features.community.repository;

import com.ltss.features.community.entity.ProhibitedTermEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProhibitedTermRepository extends JpaRepository<ProhibitedTermEntity, Long> {
    List<ProhibitedTermEntity> findAllByActiveTrueAndSeverityOrderByIdAsc(String severity);
}
