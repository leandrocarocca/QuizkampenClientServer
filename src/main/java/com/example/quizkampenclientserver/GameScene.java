package com.example.quizkampenclientserver;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class GameScene implements Initializable
{
    @FXML
    private Label playerLabel;
    @FXML
    private Label roundLabel;
    @FXML
    private Label turnLabel;
    @FXML
    private Button answer1Button;
    @FXML
    private Label questionLabel;
    @FXML
    private GridPane gameGridPane;

    Player player;
    String category;
    Question currentQuestion;
    Boolean firstPlayerTurn;
    int currentTurn = 1;
    int numberOfTurnsPerRound = 2;
    int currentRound = 1;
    int numberOfRoundsPerGame = 2;
    ObjectOutputStream output;
    ObjectInputStream input;
    ArrayList<Boolean> answers;
    BufferedReader userInput;
    Socket socket;
    Boolean keepGoing;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
    }

    public void startQuiz(String cat, Player player,int round, ObjectOutputStream output, ObjectInputStream input, Question q, Boolean firstPlayerTurn)
    {
        this.output = output;
        this.input = input;
        this.currentRound = round;
        this.player = player;
        this.currentTurn = 1;
        this.category = cat;
        this.currentQuestion = q;
        this.firstPlayerTurn = firstPlayerTurn;
        setQuestion();
    }

    public void setQuestion()
    {
        questionLabel.setText(currentQuestion.getDescription());
        int index = 0;
        for (Node n : gameGridPane.getChildren())
        {
            try
            {
                Button b = (Button) n;
                b.setBackground(Background.fill(Color.LIGHTSKYBLUE));
                b.setText(currentQuestion.getAnswers()[index]);
                index++;
            } catch (ClassCastException e)
            {
                System.out.println("Fix");
            }
        }
        playerLabel.setText("Player: " + player.getName());
        roundLabel.setText("Round: " + currentRound + "/" + numberOfRoundsPerGame);
        turnLabel.setText("Question: " + currentTurn + "/" + numberOfTurnsPerRound);
    }

    public void checkAnswer(ActionEvent actionEvent)
    {
        Button button = (Button) actionEvent.getSource();
        // Hämta rätta svaret på frågan
        String correctAnswer = currentQuestion.getAnswers()[currentQuestion.getCorrectAnswerindex()];
        if (button.getText().equals(correctAnswer))
        {
           player.setPoints(player.getPoints() + 1);
        }
        showCorrectAnswer();
        PauseTransition wait = new PauseTransition(Duration.seconds(2));
        wait.setOnFinished(event ->
        {
            System.out.println("Current turn: " + currentTurn);
            if (currentTurn < numberOfTurnsPerRound)
            {

                try
                {
                    nextTurn();
                } catch (IOException | ClassNotFoundException e)
                {
                    e.printStackTrace();
                }
            }
            else if(firstPlayerTurn)
            {
                //nextRound();
                try
                {
                    switchPlayer();
                } catch (IOException | ClassNotFoundException e)
                {
                    e.printStackTrace();
                }
            }
            else {
                try
                {
                    nextRound();
                } catch (IOException | ClassNotFoundException e)
                {
                    e.printStackTrace();
                }
            }
        });
        wait.play();
    }

    public void showCorrectAnswer()
    {
        String correctAnswer = currentQuestion.getAnswers()[currentQuestion.getCorrectAnswerindex()];
        for (Node n : gameGridPane.getChildren())
        {
            try
            {
                Button b = (Button) n;
                if (b.getText().equals(correctAnswer))
                {
                    b.setBackground(Background.fill(Color.LIMEGREEN));
                } else
                {
                    b.setBackground(Background.fill(Color.RED));
                }

            } catch (ClassCastException e)
            {
                System.out.println("Fix");
            }
        }
    }
    public void nextTurn() throws IOException, ClassNotFoundException
    {
        currentTurn++;
        output.writeObject(category); // skicka till servern vilken kategori som gäller
        this.currentQuestion = (Question)input.readObject(); // be servern om en fråga från denna kategori
        setQuestion();
    }

    public void nextRound() throws IOException, ClassNotFoundException
    {
        if (currentRound < numberOfRoundsPerGame)
        {
            currentRound++;
            switchPlayer();
        }
        else{
            output.writeObject(player);
            endGame();
        }
    }

    private void switchPlayer() throws IOException, ClassNotFoundException
    {
        player.currentPlayer = false;
        output.writeObject(player); // skickar till servern att spelaren har kört klart
        switchToScoreScene(false);

    }

    private void endGame() throws IOException, ClassNotFoundException
    {
        switchToScoreScene(true);
    }



    private void switchToScoreScene(Boolean gameFinished) throws IOException, ClassNotFoundException
    {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(Client.class.getResource("scoreScene.fxml"));
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        scoreController controller = loader.getController();
        controller.run(output,input,userInput,player,currentRound,gameFinished);
        Stage stage = (Stage) answer1Button.getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

}
