package com.open.spring.mvc.fortuneFinders;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A multiple choice question an NPC asks in Fortune Finders. The category is the NPC's name. */
@Entity
@Table(name = "fortune_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FortuneQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = true)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private int points;

    public FortuneQuestion(String title, String content, String category, int points) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.points = points;
    }

    // Sample questions: {title, content, category, points}
    public static String[][] init() {
        return new String[][] {
            {"Fidelity Quiz", "What is the purpose of Artificial Intelligence in FinTech?", "Fidelity", "10000"},
            {"Fidelity Quiz", "Which tool is essential for version control in software projects?", "Fidelity", "10000"},
            {"Fidelity Quiz", "Which of the following is a common cybersecurity threat?", "Fidelity", "10000"},
            {"Tech Quiz", "What is a benefit of using cloud computing in business?", "Schwab", "10000"},
            {"Tech Quiz", "Why do developers use debugging tools?", "Schwab", "10000"},
            {"Tech Quiz", "What does encryption do?", "Schwab", "10000"},
        };
    }
}
