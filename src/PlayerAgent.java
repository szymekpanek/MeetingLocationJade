package jadelab2;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.json.JSONObject;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class PlayerAgent extends Agent {
    private double rockProbability;
    private double paperProbability;
    private double scissorsProbability;
    private Random random;

    // Opponent AID
    private AID opponent;

    // Game statistics
    private int wins = 0;
    private int losses = 0;
    private int draws = 0;
    private int rounds = 0;
    private final int maxRounds = 100;

    // Log file
    private BufferedWriter logWriter;

    @Override
    protected void setup() {
        random = new Random();
        initializeProbabilities();

        // Log initialization
        try {
            logWriter = new BufferedWriter(new FileWriter(getLocalName() + "_log.txt"));
            logWriter.write("Initialized with rock: "
                    + rockProbability + ", paper: "
                    + paperProbability + ", scissors: "
                    + scissorsProbability + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Add behavior for playing the game
        addBehaviour(new PlayGameBehaviour());

        // Set opponent AID from arguments
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            opponent = new AID((String) args[0], AID.ISLOCALNAME);
        }
    }

    private void initializeProbabilities() {
        double total = 1.0;

        // Generate random probabilities for rock, paper, and scissors
        rockProbability = random.nextDouble() * total;
        total -= rockProbability;

        paperProbability = random.nextDouble() * total;
        total -= paperProbability;

        scissorsProbability = total; // The remaining value

        // Normalize to ensure sum is 1.0 (in case of rounding errors)
        double sum = rockProbability + paperProbability + scissorsProbability;
        rockProbability /= sum;
        paperProbability /= sum;
        scissorsProbability /= sum;
    }

    private String makeMove() {
        double rand = random.nextDouble();
        if (rand < rockProbability) {
            return "ROCK";
        } else if (rand < rockProbability + paperProbability) {
            return "PAPER";
        } else {
            return "SCISSORS";
        }
    }

    private String determineResult(String myMove, String opponentMove) {
        if (myMove.equals(opponentMove)) {
            draws++;
            return "DRAW";
        } else if ((myMove.equals("ROCK") && opponentMove.equals("SCISSORS")) ||
                (myMove.equals("PAPER") && opponentMove.equals("ROCK")) ||
                (myMove.equals("SCISSORS") && opponentMove.equals("PAPER"))) {
            wins++;
            return "WIN";
        } else {
            losses++;
            return "LOSE";
        }
    }

    private void updateProbabilities(String opponentMove, String result) {
        if (!result.equals("DRAW")) {
            switch (result) {
                case "WIN":
                    // Do not change probabilities if you already won
                    break;
                case "LOSE":
                    // Adjust probabilities only on a loss
                    switch (opponentMove) {
                        case "ROCK":
                            paperProbability += 0.1;
                            break;
                        case "PAPER":
                            scissorsProbability += 0.1;
                            break;
                        case "SCISSORS":
                            rockProbability += 0.1;
                            break;
                    }
                    break;
            }

            // Normalize to ensure sum is 1.0
            double sum = rockProbability + paperProbability + scissorsProbability;
            if (sum > 1.0) {
                rockProbability /= sum;
                paperProbability /= sum;
                scissorsProbability /= sum;
            }
        }
    }

    private class PlayGameBehaviour extends CyclicBehaviour {
        private String playerTurn = "player1";
        private String myMove;
        private boolean firstRound = true;

        @Override
        public void action() {
            if (rounds >= maxRounds) {
                myAgent.doDelete();
                return;
            }

            if (firstRound && playerTurn.equals(getLocalName())) {
                firstRound = false;
                this.sendMove("playerOneResponse");
            } else {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage moveMsg = receive(mt);
                if (moveMsg != null) {
                    JSONObject json = new JSONObject(moveMsg.getContent());
                    String _type = json.getString("type");
                    String opponentMove = json.getString("move");

                    if (_type.equals("playerTwoResponse")) { // teraz tu jest player1
                        this.compareMoves(opponentMove);
                        this.sendMove("playerOneResponse");
                        rounds++;
                    } else if (_type.equals("playerOneResponse")) { // teraz tu jest player2
                        this.sendMove("playerTwoResponse");
                        this.compareMoves(opponentMove);
                        rounds++;
                    }
                }  else {
                    block(100);
                }
            }
        }

        private void compareMoves(String opponentMove) {
            logMove(opponent.getLocalName() + " played", opponentMove);
            String result = determineResult(myMove, opponentMove);
            updateProbabilities(opponentMove, result);
            logResult(result);
        }

        private void sendMove(String type) {
            this.myMove = makeMove();
            logMove(getLocalName() + " played", this.myMove);

            ACLMessage moveMsg = new ACLMessage(ACLMessage.INFORM);
            moveMsg.addReceiver(opponent);
            JSONObject json = new JSONObject();
            json.put("type", type);
            json.put("move", myMove);
            moveMsg.setContent(json.toString());
            send(moveMsg);
        }

        private void logMove(String action, String move) {
            try {
                logWriter.write("Round " + rounds + ": " + action + " " + move + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void logResult(String result) {
            try {
                logWriter.write("Round "
                        + rounds + ": Result: "
                        + result + ", Updated probabilities: Rock: "
                        + rockProbability + ", Paper: "
                        + paperProbability + ", Scissors: "
                        + scissorsProbability + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void takeDown() {
        try {
            logWriter.write("Final statistics: Wins: " + wins + ", Losses: " + losses + ", Draws: " + draws + "\n");
            logWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(getLocalName() + " terminating.");
    }
}