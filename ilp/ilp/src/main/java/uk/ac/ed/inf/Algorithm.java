package uk.ac.ed.inf;
import java.util.ArrayList;
import java.util.List;


public class Algorithm {
    private final ArrayList<Orders.DeliveryInformation> deliveries;
    private final LongLat initialPosition;
    public Algorithm(LongLat initialPosition, ArrayList<Orders.DeliveryInformation> deliveries){
        this.initialPosition = initialPosition;
        this.deliveries = deliveries;
    }
    public List<Orders.DeliveryInformation> greedyAlgorithm(){

        int currentIndex = 0;
        double currentMin = 100000;
        Orders.DeliveryInformation currOrder = null;
        boolean second = false;

        LongLat currentPosition = initialPosition;


        List<Integer> visitedIndices = new ArrayList<>();

        List<Orders.DeliveryInformation> optimizedLoc = new ArrayList<>();


        while (deliveries.size() >= visitedIndices.size()) {

            for (Orders.DeliveryInformation delivery : deliveries) {
                double firstShop = 0;
                double secondShop = 0;

                double firstShop1 = currentPosition.distanceTo(delivery.deliverFrom.get(0));
                double firstShop2 = delivery.deliverFrom.get(0).distanceTo(delivery.deliverFrom.get(1));
                double firstShop3 = delivery.deliverFrom.get(1).distanceTo(delivery.deliverTo);

                double secondShop1 = currentPosition.distanceTo(delivery.deliverFrom.get(1));
                double secondShop2 = delivery.deliverFrom.get(1).distanceTo(delivery.deliverFrom.get(0));
                double secondShop3 = delivery.deliverFrom.get(0).distanceTo(delivery.deliverTo);


                if (delivery.deliverFrom.size() == 1) {
                    firstShop = firstShop + currentPosition.distanceTo(delivery.deliverFrom.get(0));
                    firstShop = firstShop + delivery.deliverFrom.get(0).distanceTo(delivery.deliverTo);
                    second = false;

                } else{
                    firstShop += firstShop1;
                    firstShop += firstShop2;
                    firstShop += firstShop3;
                    secondShop += secondShop + secondShop1;
                    secondShop += secondShop2;
                    secondShop += secondShop3;
                }
                if (!visitedIndices.contains(deliveries.indexOf(delivery)) &
                        deliveries.indexOf(delivery) != currentIndex &
                        firstShop < currentMin) {

                    currentMin = firstShop;
                    currOrder = delivery;
                    currentIndex = deliveries.indexOf(delivery);
                    second = false;
                }
                if (!visitedIndices.contains(deliveries.indexOf(delivery)) &
                        deliveries.indexOf(delivery) != currentIndex &
                        delivery.deliverFrom.size() == 2 &
                        secondShop < currentMin) {


                    currentMin = secondShop;
                    currOrder = delivery;
                    currentIndex = deliveries.indexOf(delivery);
                    second = true;


                }
            }


        }
        visitedIndices.remove(visitedIndices.size()-1);
        return optimizedLoc;


    }

}
