package jadelab2;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MeetingAgent extends Agent {
    private static final String[] points = {"P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8"};
    private static final Random random = new Random();
    private String location;
    private CityMap cityMap;
    private Map<String, Integer> myDistances;
    private final Map<String, Map<String, Integer>> receivedDistances = new HashMap<>();
    private final Map<String, Integer> proposedMeetingPoints = new HashMap<>();
    private int numAgents;

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " is active");
        cityMap = new CityMap();
        cityMap.initializeMap();

        // Register the agent to the directory service
        registerAgent();

        // Set the agent's location
        setLocation();
        System.out.println(getLocalName() + " location is " + location);

        // Get the number of active Agents added to JADE
        getNumberOfAgents();

        // Calculate the shortest paths from the location
        myDistances = cityMap.shortestPathsFrom(location);

        // Add behaviors
        addBehaviour(new SendDistancesBehaviour(this, 2000));
        addBehaviour(new ListenBehaviour());

        System.out.println();
    }

    private void setLocation() {
        location = points[random.nextInt(points.length)];
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
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("meeting-location");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                for (int i = 0; i < result.length; ++i) {
                    msg.addReceiver(result[i].getName());
                }
                send(msg);
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
                System.out.println(getLocalName() + " received: " + content);
                String sender = msg.getSender().getLocalName();
                if (content.startsWith("{")) {
                    // Parsing distances map
                    if (!receivedDistances.containsKey(sender)) {
                        receivedDistances.put(sender, parseContent(content));
                    }
                    if (receivedDistances.size() == numAgents) {
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
                            for (int i = 0; i < result.length; ++i) {
                                proposalMsg.addReceiver(result[i].getName());
                            }
                            send(proposalMsg);
                        } catch (FIPAException fe) {
                            fe.printStackTrace();
                        }
                    }
                } else if (content.startsWith("PROPOSED_POINT:")) {
                    String proposedPoint = content.split(":")[1];
                    proposedMeetingPoints.merge(proposedPoint, 1, Integer::sum);
                    if (proposedMeetingPoints.values().stream().mapToInt(Integer::intValue).sum() == numAgents) {
                        // Are sum of proposed meeting = number of agents
                        String meetingPoint = agreeOnMeetingPoint();
                        System.out.println(getLocalName() + " determined final meeting point: " + meetingPoint);
                        myAgent.doDelete();
                    }
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
        receivedDistances.values().forEach(map -> {
            map.forEach((k, v) -> combinedDistances.merge(k, v, Integer::sum));
        });
        myDistances.forEach((k, v) -> combinedDistances.merge(k, v, Integer::sum));

        // Calculate the average distance for each location
        Map<String, Double> averageDistances = new HashMap<>();
        combinedDistances.forEach((k, v) -> averageDistances.put(k, v.doubleValue() / (receivedDistances.size() + 1)));

        // Find the location with the minimum average distance
        // Dodano zabezpieczenie, że zgłoszone punkty nie mogą być punktem początkowym agenta
        return averageDistances.entrySet().stream()
                .filter(e -> !e.getKey().equals(location))
                .min(Map.Entry.comparingByValue())
                .get()
                .getKey();
    }

    private String agreeOnMeetingPoint() {
        return proposedMeetingPoints.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .get()
                .getKey();
    }

    @Override
    protected void takeDown() {
        // Deregister the agent
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println(getLocalName() + " terminating.");
    }
}