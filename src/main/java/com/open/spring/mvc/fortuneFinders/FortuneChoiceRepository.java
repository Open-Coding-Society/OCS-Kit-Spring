package com.open.spring.mvc.fortuneFinders;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FortuneChoiceRepository extends JpaRepository<FortuneChoice, Long> {
    List<FortuneChoice> findByQuestionId(Long questionId);
}
