package jadelab2;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import java.util.Random;

public class MeetingAgent extends Agent {
    private static final String[] points = {"P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8"};
    private static final Random random = new Random();
    private String location;

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " is active");

        // Ustaw lokalizację agenta
        setLocation();

        addBehaviour(new DisplayLocationBehaviour());
    }

    private void setLocation() {
        location = points[random.nextInt(points.length)];
    }

    private class DisplayLocationBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            System.out.println(getLocalName() + " location is: " + location);
            block();
        }
    }

    @Override
    protected void takeDown() {
        // Kod usuwający agenta
    }
}
