package com.open.spring.mvc.fortuneFinders;

import com.open.spring.mvc.person.Person;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A player's answer to a FortuneQuestion. */
@Entity
@Table(name = "fortune_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FortuneAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private FortuneQuestion question;

    @ManyToOne
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne
    @JoinColumn(name = "choice_id", nullable = false)
    private FortuneChoice choice;

    @Column(nullable = false)
    private Boolean isCorrect;

    public FortuneAnswer(FortuneQuestion question, Person person, FortuneChoice choice, Boolean isCorrect) {
        this.question = question;
        this.person = person;
        this.choice = choice;
        this.isCorrect = isCorrect;
    }
}
