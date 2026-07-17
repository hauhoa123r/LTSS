package com.ltss.repository.community;

import com.ltss.entity.community.ProhibitedTermEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProhibitedTermRepository extends JpaRepository<ProhibitedTermEntity, Long> {
    List<ProhibitedTermEntity> findAllByActiveTrueAndSeverityOrderByIdAsc(String severity);
}
