package com.open.spring.mvc.fortuneFinders;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FortuneAnswerRepository extends JpaRepository<FortuneAnswer, Long> {
    List<FortuneAnswer> findByPersonId(Long personId);

    List<FortuneAnswer> findByQuestionIdAndPersonId(Long questionId, Long personId);
}
