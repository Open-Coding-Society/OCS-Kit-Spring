package com.open.spring.mvc.fortuneFinders;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FortuneQuestionRepository extends JpaRepository<FortuneQuestion, Long> {
    List<FortuneQuestion> findByCategory(String category);
}
