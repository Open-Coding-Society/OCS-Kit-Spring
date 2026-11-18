package com.open.spring.mvc.fortuneFinders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.open.spring.mvc.bank.Bank;
import com.open.spring.mvc.bank.BankJpaRepository;
import com.open.spring.mvc.person.Person;
import com.open.spring.mvc.person.PersonJpaRepository;

import lombok.Getter;

/**
 * Backend for the Fortune Finders game in OCS-Kit-Pages (features/finance-sim/).
 * OCS Spring removed these endpoints in "migrate adventure"; this brings back the ones the game calls.
 * They live under /api so they use the same JWT cookie login as the rest of the kit's APIs
 * (the old /rpg_answer sat outside the API security chain, where the cookie is not read).
 * A player may only read or change their own balance and answers (an Admin may use any).
 */
@RestController
@RequestMapping("/api/rpg_answer")
public class FortuneApiController {

    // Most a single updateBalance call may add, so the balance can't be set from the browser console
    private static final double MAX_AWARD = 1000;

    @Autowired
    private FortuneQuestionRepository questionRepository;
    @Autowired
    private FortuneChoiceRepository choiceRepository;
    @Autowired
    private FortuneAnswerRepository answerRepository;
    @Autowired
    private PersonJpaRepository personJpaRepository;
    @Autowired
    private BankJpaRepository bankJpaRepository;

    @Getter
    public static class MCQAnswerDto {
        private Long questionId;
        private Long personId;
        private Long choiceId;
    }

    // The game's quiz panel marks the right answer itself, so the choice carries is_correct
    @Getter
    public static class ChoiceDto {
        private final Long id;
        private final String choice;
        private final Boolean is_correct;

        ChoiceDto(FortuneChoice choice) {
            this.id = choice.getId();
            this.choice = choice.getChoice();
            this.is_correct = choice.getIs_correct();
        }
    }

    // Balance of a player's bank account
    @GetMapping("/getBalance/{personid}")
    public ResponseEntity<Double> getBalance(@PathVariable Long personid, Authentication auth) {
        Person person = personJpaRepository.findById(personid).orElse(null);
        if (person == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (!isSelfOrAdmin(auth, person)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        Bank bank = bankJpaRepository.findByUid(person.getUid());
        if (bank == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(bank.getBalance(), HttpStatus.OK);
    }

    // Adds a small award to a player's balance (the End level pays +100 per pickup)
    @GetMapping("/updateBalance/{personid}/{amount}")
    public ResponseEntity<Double> updateBalance(@PathVariable Long personid, @PathVariable double amount, Authentication auth) {
        if (!(amount > 0) || amount > MAX_AWARD) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Person person = personJpaRepository.findById(personid).orElse(null);
        if (person == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (!isSelfOrAdmin(auth, person)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        Bank bank = bankJpaRepository.findByUid(person.getUid());
        if (bank == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        bank.setBalance(bank.getBalance() + amount, "adventure");
        bankJpaRepository.save(bank);
        return new ResponseEntity<>(bank.getBalance(), HttpStatus.OK);
    }

    // Share of a player's answers that were right (0.0 to 1.0)
    @GetMapping("/getQuestionAccuracy/{personid}")
    public ResponseEntity<Double> getQuestionAccuracy(@PathVariable Long personid, Authentication auth) {
        Person person = personJpaRepository.findById(personid).orElse(null);
        if (person == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (!isSelfOrAdmin(auth, person)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        List<FortuneAnswer> answers = answerRepository.findByPersonId(personid);
        if (answers.isEmpty()) {
            return new ResponseEntity<>(0.0, HttpStatus.OK);
        }
        long right = answers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
        return new ResponseEntity<>((double) right / answers.size(), HttpStatus.OK);
    }

    // An NPC's questions the player has not answered correctly yet, with shuffled choices
    @GetMapping("/getQuestion")
    public ResponseEntity<Map<String, Object>> getQuestion(@RequestParam String category, @RequestParam Long personid, Authentication auth) {
        Person person = personJpaRepository.findById(personid).orElse(null);
        if (person == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (!isSelfOrAdmin(auth, person)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        List<Map<String, Object>> questionsWithChoices = new ArrayList<>();
        for (FortuneQuestion question : questionRepository.findByCategory(category)) {
            boolean wasCorrect = answerRepository.findByQuestionIdAndPersonId(question.getId(), personid).stream()
                .anyMatch(a -> Boolean.TRUE.equals(a.getIsCorrect()));
            if (wasCorrect) {
                continue;
            }
            List<ChoiceDto> choices = new ArrayList<>();
            for (FortuneChoice choice : choiceRepository.findByQuestionId(question.getId())) {
                choices.add(new ChoiceDto(choice));
            }
            Collections.shuffle(choices); // so the right answer isn't always first

            Map<String, Object> entry = new HashMap<>();
            entry.put("question", question);
            entry.put("choices", choices);
            questionsWithChoices.add(entry);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("questions", questionsWithChoices);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Records an answer; a first correct answer adds the question's points to the balance
    @PostMapping("/submitMCQAnswer")
    public ResponseEntity<Boolean> submitMCQAnswer(@RequestBody MCQAnswerDto dto, Authentication auth) {
        if (dto.getQuestionId() == null || dto.getPersonId() == null || dto.getChoiceId() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        FortuneQuestion question = questionRepository.findById(dto.getQuestionId()).orElse(null);
        Person person = personJpaRepository.findById(dto.getPersonId()).orElse(null);
        FortuneChoice choice = choiceRepository.findById(dto.getChoiceId()).orElse(null);
        // The choice has to belong to the question, or any choice could be sent for any question
        if (question == null || person == null || choice == null || !choice.getQuestion().getId().equals(question.getId())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (!isSelfOrAdmin(auth, person)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        Bank bank = bankJpaRepository.findByUid(person.getUid());
        if (bank == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        boolean alreadyCorrect = answerRepository.findByQuestionIdAndPersonId(question.getId(), person.getId()).stream()
            .anyMatch(a -> Boolean.TRUE.equals(a.getIsCorrect()));
        boolean isCorrect = Boolean.TRUE.equals(choice.getIs_correct());
        answerRepository.save(new FortuneAnswer(question, person, choice, isCorrect));

        // Points are paid once per question, so resubmitting can't farm the balance
        if (isCorrect && !alreadyCorrect) {
            bank.setBalance(bank.getBalance() + question.getPoints(), "adventure");
            bankJpaRepository.save(bank);
        }
        return ResponseEntity.ok(true);
    }

    // The signed-in player (matched by uid) or an Admin
    private boolean isSelfOrAdmin(Authentication auth, Person person) {
        if (auth == null) {
            return false;
        }
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return admin || auth.getName().equals(person.getUid());
    }
}
