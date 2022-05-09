/**
 * Main Class for Drone Delivery System
 */
package uk.ac.ed.inf;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class App {


    public static void main(String[] args) throws SQLException {
        String day = args[0];
        String month = args[1];
        String year = args[2];
        String dateString = year + '-' + month + '-' + day;
        Date date = Date.valueOf(dateString);


        String webServerPort = args[3];
        String databaseServerPort = args[4];


        String localhost = "localhost";
        String database = "derbyDB";

        LongLat startPosition = new LongLat(LongLat.AT_LONG, LongLat.AT_LAT);

        Orders orders = new Orders(localhost, databaseServerPort, webServerPort);
        ArrayList<Orders.DeliveryInformation> destinations = new ArrayList<Orders.DeliveryInformation>(orders.deliveryInfo(orders.getOrders(database, date)));

        System.out.println(orders);


        FeatureCollection noFlyZone = startPosition.getNoFlyZones(new HttpHandler(localhost, webServerPort));


        Algorithm optimizer = new Algorithm(startPosition, destinations);
        System.out.println(startPosition);

        List<Orders.DeliveryInformation> optimized = optimizer.greedyAlgorithm();


        Drone drone = new Drone(startPosition, noFlyZone, optimized, localhost, databaseServerPort);
        drone.visitLocations();
        noFlyZone.features().add(Feature.fromGeometry(LineString.fromLngLats(drone.flightPaths)));
        noFlyZone.toJson();
        System.out.println(noFlyZone);


        orders.setDeliveryTable(drone.ffOrders, databaseServerPort);
        drone.setFlightPathTable(databaseServerPort);

        String pathString = "drone-" + day + '-' + month + '-' + year + ".geojson";

        Path path = Paths.get(pathString);


        try {
            Files.writeString(path, noFlyZone.toJson());
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }


}
