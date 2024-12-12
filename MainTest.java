
import org.junit.jupiter.api.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainTest {
//testMainInitialization(): Checks if the Main class initializes correctly with the expected initial values.
    @Test
    void testMainInitialization() {
        // Load questions (replace with your actual loading logic)
        ArrayList<MCQ> questions = loadQuestionsForTest(); 
        Main app = new Main(questions, 24, 15, "test_quiz.txt"); 
        assertNotNull(app);
        assertEquals(0, app.currentQuestionIndex);
    }
   // testLoadQuestionsFromFile(): This test checks if the loadQuestionsFromFile() method correctly
   // reads and parses questions from a file. It uses a temporary file with sample data to ensure the test is
   // self-contained and doesn't affect your actual quiz files.
    @Test
    void testLoadQuestionsFromFile() throws IOException {
        // Prepare a temporary test file
        File tempFile = createTempQuizFile();

        // Load questions using the method
        ArrayList<MCQ> questions = Main.loadQuestionsFromFile(tempFile.getAbsolutePath(), tempFile.getParentFile());

        // Assertions
        assertNotNull(questions);
        assertEquals(3, questions.size()); // Assuming 3 questions in the file

        // Additional assertions to check content
        MCQ firstQuestion = questions.get(0);
        assertEquals("Question 1 Text", firstQuestion.getQuestion());
        // ... more assertions

        // Clean up (delete the temporary file)
        tempFile.delete();
    }

    // Helper method to create a temporary quiz file
    private File createTempQuizFile() throws IOException {
        File tempFile = File.createTempFile("temp_quiz", ".txt");

        String quizData = "Question 1\n" +
                "Question 1 Text\n" +
                "A) Choice A1\n" +
                "B) Choice B1\n" +
                "C) Choice C1\n" +
                "D) Choice D1\n" +
                "Answer: A\n" +
                "Explanation: Explanation 1\n" +
                "Question 2\n" +
                "Question 2 Text\n" +
                "A) Choice A2\n" +
                "B) Choice B2\n" +
                "C) Choice C2\n" +
                "D) Choice D2\n" +
                "Answer: B\n" +
                "Explanation: Explanation 2\n" +
                "Question 3\n" +
                "Question 3 Text\n" +
                "A) Choice A3\n" +
                "B) Choice B3\n" +
                "C) Choice C3\n" +
                "D) Choice D3\n" +
                "Answer: C\n" +
                "Explanation: Explanation 3\n";
        java.nio.file.Files.writeString(tempFile.toPath(), quizData);

        return tempFile;
    }
//testShuffleQuestions(): Verifies that the shuffleQuestions() method actually changes the order
// of the questions in the questions ArrayList.
    @Test
    void testShuffleQuestions() {
        ArrayList<MCQ> questions = loadQuestionsForTest();
        ArrayList<MCQ> originalQuestions = new ArrayList<>(questions);

        System.out.println("Before shuffling: " + questions); // Print before

        Main.shuffleQuestions(questions);

        System.out.println("After shuffling: " + questions); // Print after

        // Check if the lists have different orders
        boolean orderChanged = false;
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i) != originalQuestions.get(i)) {
                orderChanged = true;
                break;
            }
        }

        assertTrue(orderChanged, "The order of questions should be different after shuffling");
    }
    
        
            @Test
            void testExtractAnswerText() {
                String[] choices = {"A) Choice A", "B) Choice B", "C) Choice C", "D) Choice D"};
                Main app = new Main(loadQuestionsForTest(), 24, 15, "test_quiz.txt");
        
                String answerText = app.extractAnswerText("C", choices);
                assertEquals("Choice C", answerText);
            }
        
 // Helper method to load questions for testing (ensure questions are unique)
    private ArrayList<MCQ> loadQuestionsForTest() {
        ArrayList<MCQ> questions = new ArrayList<>();
        String[] choices = {"A) Choice A", "B) Choice B", "C) Choice C", "D) Choice D"};
        questions.add(new MCQ("Question 1?", choices, "A", "Reason 1", "q1.wav", "e1.wav"));
        questions.add(new MCQ("Question 2?", choices, "B", "Reason 2", "q2.wav", "e2.wav")); // Different question
        questions.add(new MCQ("Question 3?", choices, "C", "Reason 3", "q3.wav", "e3.wav")); // Different question
        // ... add more unique questions ...
        return questions;
    }
}
