package jadelab2;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MeetingAgent extends Agent {
    private static final String[] points = {"P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8","P9"};
    private static final Random random = new Random();
    private String location;
    private CityMap cityMap;
    private Map<String, Integer> myDistances;
    private final Map<String, Map<String, Integer>> receivedDistances = new HashMap<>();
    private final Map<String, Integer> proposedMeetingPoints = new HashMap<>();
    private static final Set<String> agentLocations = new HashSet<>();
    public int numAgents;
    private static String finalMeetingPoint;

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " is active");
        cityMap = new CityMap();
        cityMap.initializeMap();

        // Register the agent to the directory service
        registerAgent();

        // Set the agent's location
        setLocation();
        agentLocations.add(location);
        System.out.println(getLocalName() + " location is " + location);

        // Get the number of active agents added to JADE
        getNumberOfAgents();

        // Calculate the shortest paths from the location
        myDistances = cityMap.shortestPathsFrom(location);

        // Add behaviors
        addBehaviour(new SendDistancesBehaviour(this, 2000));
        addBehaviour(new ListenBehaviour());

        System.out.println();
    }

    private void setLocation() {
        do {
            location = points[random.nextInt(points.length)];
        } while (agentLocations.contains(location));
    }

    private void getNumberOfAgents() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("meeting-location");
        template.addServices(sd);
        try {
            DFAgentDescription[] results = DFService.search(this, template);
            numAgents = results.length;
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private void registerAgent() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("meeting-location");
        sd.setName("JADE-meeting-locations");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private class SendDistancesBehaviour extends TickerBehaviour {
        public SendDistancesBehaviour(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent(myDistances.toString());

            msg.setSender(getAID());

            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("meeting-location");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                for (DFAgentDescription dfAgentDescription : result) {
                    if (!dfAgentDescription.getName().equals(getAID())) {    // Avoid sending to self
                        msg.addReceiver(dfAgentDescription.getName());
                    }
                }
                send(msg);
                System.out.println(getLocalName() + " sent: " + msg.getContent());
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
    }

    private class ListenBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                String content = msg.getContent();
                String sender = msg.getSender().getLocalName();
                System.out.println(getLocalName() + " received from " + sender + ": " + content);

                if (content.startsWith("{")) {
                    // Parsing distances map
                    if (!receivedDistances.containsKey(sender)) {
                        receivedDistances.put(sender, parseContent(content));
                    }
                    if (receivedDistances.size() == numAgents - 1) { // Consider other agents only
                        String proposedMeetingPoint = determineMeetingPoint();
                        System.out.println(getLocalName() + " proposed meeting point: " + proposedMeetingPoint);
                        ACLMessage proposalMsg = new ACLMessage(ACLMessage.INFORM);
                        proposalMsg.setContent("PROPOSED_POINT:" + proposedMeetingPoint);

                        DFAgentDescription template = new DFAgentDescription();
                        ServiceDescription sd = new ServiceDescription();
                        sd.setType("meeting-location");
                        template.addServices(sd);
                        try {
                            DFAgentDescription[] result = DFService.search(myAgent, template);
                            for (DFAgentDescription dfAgentDescription : result) {
                                if (!dfAgentDescription.getName().equals(getAID())) {
                                    proposalMsg.addReceiver(dfAgentDescription.getName());
                                }
                            }
                            send(proposalMsg);
                            System.out.println(getLocalName() + " sent: " + proposalMsg.getContent());
                        } catch (FIPAException fe) {
                            fe.printStackTrace();
                        }
                    }
                } else if (content.startsWith("PROPOSED_POINT:")) {
                    String proposedPoint = content.split(":")[1];
                    proposedMeetingPoints.merge(proposedPoint, 1, Integer::sum);
                    if (proposedMeetingPoints.values().stream().mapToInt(Integer::intValue).sum() == numAgents - 1) {
                        String meetingPoint = agreeOnMeetingPoint();
                        System.out.println(getLocalName() + " determined final meeting point: " + meetingPoint);

                        ACLMessage finalMsg = new ACLMessage(ACLMessage.INFORM);
                        finalMsg.setContent("FINAL_MEETING_POINT:" + meetingPoint);

                        DFAgentDescription template = new DFAgentDescription();
                        ServiceDescription sd = new ServiceDescription();
                        sd.setType("meeting-location");
                        template.addServices(sd);

                        try {
                            DFAgentDescription[] result = DFService.search(myAgent, template);
                            for (DFAgentDescription dfAgentDescription : result) {
                                if (!dfAgentDescription.getName().equals(getAID())) {
                                    finalMsg.addReceiver(dfAgentDescription.getName());
                                }
                            }
                            send(finalMsg);
                            System.out.println(getLocalName() + " sent: " + finalMsg.getContent());

                            // Generate and save the graph image
                            Graph<String, DefaultEdge> graph = CityMapVisualizer.createGraphFromCityMap(cityMap.getCityMap());
                            CityMapVisualizer.saveGraphAsImage(graph, "final_meeting_map.png", agentLocations, meetingPoint);

                            finalMeetingPoint = meetingPoint;

                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (FIPAException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else if (content.startsWith("FINAL_MEETING_POINT:")) {
                    String finalPoint = content.split(":")[1];
                    System.out.println(getLocalName() + " agreed on final meeting point: " + finalPoint);
                    if (finalMeetingPoint == null) {
                        finalMeetingPoint = finalPoint;
                    } else if (!finalMeetingPoint.equals(finalPoint)) {
                        System.out.println("Error: Inconsistent final meeting points detected!");
                    }
                    myAgent.doDelete();
                }
            } else {
                block();
            }
        }

        private Map<String, Integer> parseContent(String content) {
            Map<String, Integer> distances = new HashMap<>();
            content = content.substring(1, content.length() - 1); // Removing curly braces
            String[] entries = content.split(", ");
            for (String entry : entries) {
                String[] keyValue = entry.split("=");
                distances.put(keyValue[0], Integer.parseInt(keyValue[1]));
            }
            return distances;
        }
    }

    private String determineMeetingPoint() {
        // Combine all distance maps including this agent's own map
        Map<String, Integer> combinedDistances = new HashMap<>();
        receivedDistances.values().forEach(map -> map.forEach((k, v) -> combinedDistances.merge(k, v, Integer::sum)));
        myDistances.forEach((k, v) -> combinedDistances.merge(k, v, Integer::sum));

        // Calculate the average distance for each location
        Map<String, Double> averageDistances = new HashMap<>();
        combinedDistances.forEach((k, v) -> {
            if (!agentLocations.contains(k)) { // Exclude points that are initial locations of agents
                averageDistances.put(k, v / (double) numAgents);
            }
        });

        return averageDistances.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private String agreeOnMeetingPoint() {
        return proposedMeetingPoints.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
