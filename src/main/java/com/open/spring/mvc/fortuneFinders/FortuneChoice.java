package com.open.spring.mvc.fortuneFinders;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One answer option of a FortuneQuestion. */
@Entity
@Table(name = "fortune_choices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FortuneChoice {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private FortuneQuestion question;

    @Column(nullable = false)
    private String choice;

    // The game's quiz panel reads this name, so it keeps the underscore
    @Column(nullable = false)
    private Boolean is_correct;

    public FortuneChoice(FortuneQuestion question, String choice, Boolean is_correct) {
        this.question = question;
        this.choice = choice;
        this.is_correct = is_correct;
    }

    // Sample choices: {1-based question number in FortuneQuestion.init(), choice, is_correct}
    public static String[][] init() {
        return new String[][] {
            {"1", "To automate financial decision-making.", "true"},
            {"1", "To manage physical office supplies.", "false"},
            {"1", "To handle customer complaints manually.", "false"},
            {"1", "To replace all employees.", "false"},
            {"2", "Git.", "true"},
            {"2", "Excel.", "false"},
            {"2", "Slack.", "false"},
            {"2", "Zoom.", "false"},
            {"3", "Phishing.", "true"},
            {"3", "Cloud backup.", "false"},
            {"3", "Two-factor authentication.", "false"},
            {"3", "VPN usage.", "false"},
            {"4", "Allows access from multiple locations and devices.", "true"},
            {"4", "Eliminates the need for internet.", "false"},
            {"4", "Requires local installation of all software.", "false"},
            {"4", "Only works on weekends.", "false"},
            {"5", "To identify and fix software bugs.", "true"},
            {"5", "To redesign UI only.", "false"},
            {"5", "To launch advertisements.", "false"},
            {"5", "To reset passwords.", "false"},
            {"6", "It protects data from unauthorized access.", "true"},
            {"6", "It compresses image files.", "false"},
            {"6", "It blocks websites.", "false"},
            {"6", "It stores files temporarily.", "false"},
        };
    }
}
