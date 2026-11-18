package com.open.spring.mvc.fortuneFinders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import com.open.spring.mvc.bank.Bank;
import com.open.spring.mvc.bank.BankJpaRepository;
import com.open.spring.mvc.person.Person;
import com.open.spring.mvc.person.PersonJpaRepository;

/** Fortune Finders' /rpg_answer endpoints: own data only, points paid once per question. */
class FortuneApiControllerTest {

    private final FortuneQuestionRepository questions = mock(FortuneQuestionRepository.class);
    private final FortuneChoiceRepository choices = mock(FortuneChoiceRepository.class);
    private final FortuneAnswerRepository answers = mock(FortuneAnswerRepository.class);
    private final PersonJpaRepository people = mock(PersonJpaRepository.class);
    private final BankJpaRepository banks = mock(BankJpaRepository.class);
    private final FortuneApiController controller = new FortuneApiController();

    private final Person player = new Person();
    private final Bank bank = new Bank();
    private final FortuneQuestion question = new FortuneQuestion("Quiz", "What is Git?", "Fidelity", 500);
    private final FortuneChoice right = new FortuneChoice(question, "Version control", true);
    private final FortuneChoice wrong = new FortuneChoice(question, "A spreadsheet", false);
    private final Authentication self = new UsernamePasswordAuthenticationToken("alex", null, List.of());
    private final Authentication other = new UsernamePasswordAuthenticationToken("sam", null, List.of());
    private final Authentication admin = new UsernamePasswordAuthenticationToken("root", null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "questionRepository", questions);
        ReflectionTestUtils.setField(controller, "choiceRepository", choices);
        ReflectionTestUtils.setField(controller, "answerRepository", answers);
        ReflectionTestUtils.setField(controller, "personJpaRepository", people);
        ReflectionTestUtils.setField(controller, "bankJpaRepository", banks);

        ReflectionTestUtils.setField(player, "id", 1L);
        player.setUid("alex");
        question.setId(10L);
        right.setId(100L);
        wrong.setId(101L);
        bank.setBalance(1000, "test");

        when(people.findById(1L)).thenReturn(Optional.of(player));
        when(banks.findByUid("alex")).thenReturn(bank);
        when(questions.findById(10L)).thenReturn(Optional.of(question));
        when(choices.findById(100L)).thenReturn(Optional.of(right));
        when(choices.findById(101L)).thenReturn(Optional.of(wrong));
    }

    private FortuneApiController.MCQAnswerDto answer(Long questionId, Long choiceId) {
        FortuneApiController.MCQAnswerDto dto = new FortuneApiController.MCQAnswerDto();
        ReflectionTestUtils.setField(dto, "questionId", questionId);
        ReflectionTestUtils.setField(dto, "personId", 1L);
        ReflectionTestUtils.setField(dto, "choiceId", choiceId);
        return dto;
    }

    @Test
    void aPlayerReadsOnlyTheirOwnBalance() {
        assertEquals(1000.0, controller.getBalance(1L, self).getBody());
        assertEquals(HttpStatus.FORBIDDEN, controller.getBalance(1L, other).getStatusCode());
        assertEquals(HttpStatus.OK, controller.getBalance(1L, admin).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.getBalance(99L, self).getStatusCode());
    }

    @Test
    void updateBalanceAddsASmallAwardOnly() {
        assertEquals(1100.0, controller.updateBalance(1L, 100, self).getBody());
        assertEquals(HttpStatus.BAD_REQUEST, controller.updateBalance(1L, 1_000_000, self).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controller.updateBalance(1L, -50, self).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.updateBalance(1L, 100, other).getStatusCode());
        assertEquals(1100.0, bank.getBalance());
    }

    @Test
    void aCorrectAnswerPaysThePointsOnce() {
        when(answers.findByQuestionIdAndPersonId(10L, 1L)).thenReturn(List.of());
        controller.submitMCQAnswer(answer(10L, 100L), self);
        assertEquals(1500.0, bank.getBalance());

        FortuneAnswer first = new FortuneAnswer(question, player, right, true);
        when(answers.findByQuestionIdAndPersonId(10L, 1L)).thenReturn(List.of(first));
        controller.submitMCQAnswer(answer(10L, 100L), self);
        assertEquals(1500.0, bank.getBalance());
    }

    @Test
    void aWrongAnswerIsRecordedWithoutPay() {
        when(answers.findByQuestionIdAndPersonId(10L, 1L)).thenReturn(List.of());
        controller.submitMCQAnswer(answer(10L, 101L), self);
        assertEquals(1000.0, bank.getBalance());
        verify(answers).save(any(FortuneAnswer.class));
    }

    @Test
    void aChoiceFromAnotherQuestionOrPlayerIsRefused() {
        FortuneQuestion otherQuestion = new FortuneQuestion("Quiz", "Other", "Fidelity", 500);
        otherQuestion.setId(11L);
        FortuneChoice stray = new FortuneChoice(otherQuestion, "Anything", true);
        stray.setId(102L);
        when(choices.findById(102L)).thenReturn(Optional.of(stray));

        assertEquals(HttpStatus.BAD_REQUEST, controller.submitMCQAnswer(answer(10L, 102L), self).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.submitMCQAnswer(answer(10L, 100L), other).getStatusCode());
        verify(answers, never()).save(any());
        assertEquals(1000.0, bank.getBalance());
    }

    @Test
    void accuracyIsTheShareOfCorrectAnswers() {
        when(answers.findByPersonId(1L)).thenReturn(List.of(
                new FortuneAnswer(question, player, right, true),
                new FortuneAnswer(question, player, wrong, false)));
        assertEquals(0.5, controller.getQuestionAccuracy(1L, self).getBody());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getQuestionSkipsWhatThePlayerAlreadyGotRight() {
        FortuneQuestion done = new FortuneQuestion("Quiz", "Done", "Fidelity", 500);
        done.setId(11L);
        when(questions.findByCategory("Fidelity")).thenReturn(List.of(question, done));
        when(choices.findByQuestionId(10L)).thenReturn(List.of(right, wrong));
        when(answers.findByQuestionIdAndPersonId(11L, 1L))
                .thenReturn(List.of(new FortuneAnswer(done, player, right, true)));

        Map<String, Object> body = controller.getQuestion("Fidelity", 1L, self).getBody();
        List<Map<String, Object>> list = (List<Map<String, Object>>) body.get("questions");
        assertEquals(1, list.size());
        assertEquals(question, list.get(0).get("question"));
        assertEquals(2, ((List<?>) list.get(0).get("choices")).size());
    }
}
