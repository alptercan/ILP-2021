package uk.ac.ed.inf;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import java.awt.geom.Line2D;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Drone {
    //Private
    private int maxMoves = 1500;
    private int currOrderIndex = 0;
    private int currLocOrderIndex = 0;
    private boolean returnHome;
    private final LongLat initialPosition;
    private LongLat currentPosition;
    private final FeatureCollection noFlyZone;
    private final List<Line2D> noFlyAreas;
    private Connection conn;
    private DatabaseMetaData databaseMetaData;
    private ResultSet resultSet;
    private Statement statement;
    private  PreparedStatement ps;

    //Public
    public LongLat currentDest;
    public List<LongLat.Move> way = new ArrayList<>();
    public List<LongLat> landmarks = new ArrayList<>();
    public List<Orders.DeliveryInformation> orderLocations;
    public List<Orders.DeliveryInformation> ffOrders = new ArrayList<>();
    public List<Point> flightPaths = new ArrayList<>();
    private final String jdbc;



    public Drone(LongLat initialPosition, FeatureCollection noFlyZone, List<Orders.DeliveryInformation> optimized, String localHost, String database){
        this.initialPosition = initialPosition;
        this.currentPosition = initialPosition;
        this.noFlyZone = noFlyZone;
        this.noFlyAreas = getNoFlyAreas();
        this.currentDest = getLocations(this.orderLocations.get(0)).get(0);
        this.returnHome = false;
        flightPaths.add(Point.fromLngLat(currentPosition.longitude, currentPosition.latitude));
        this.jdbc = "jdbc:derby://" + localHost + ":" + database + "/";

    }
    /**
     * Getting all the locations.
     * @param delivery
     * @return
     */
    public List<LongLat> getLocations(Orders.DeliveryInformation delivery){
        List<LongLat> locations = new ArrayList<>(delivery.deliverFrom);
        locations.add(delivery.deliverTo);
        System.out.println(locations);
        return locations;

    }



    /**
     * We use this method to prevent the drone getting stuck.
     * @return
     */
    private List<Line2D> getNoFlyAreas() {
        List<Line2D> noFlyAreas = new ArrayList<>();
        assert noFlyZone.features() != null;
        for (Feature feature: this.noFlyZone.features()){
            Polygon polygon = (Polygon) feature.geometry();
            assert polygon != null;
            List<List<Point>> coordinatesLists = polygon.coordinates();
            List<Point> coordinatesList = coordinatesLists.get(0);
            for (int i = 0; coordinatesList.size() - 1 > i; i +=1){
                Point pointA = coordinatesList.get(i);
                Point pointB = coordinatesList.get(i+1);
                Line2D line = new Line2D.Double(pointA.longitude(),pointA.latitude(), pointB.longitude(), pointB.latitude());
                noFlyAreas.add(line);
                System.out.println(line);


            }
            System.out.println(coordinatesList);

        }
        return noFlyAreas;

    }

    /**
     * We check if the drone is reaching the move limit.
     * If it is close to die, it starts going to Appleton Tower.
     *
     */
    public void visitLocations() {
        boolean stillFly = true;
        while(stillFly){
            LongLat destination = this.currentDest;
            int direction = this.currentPosition.getClosestAngleToDestination(destination);
            droneMove(direction);
            double distanceHome = currentPosition.distanceTo(initialPosition);
            if ((maxMoves-30)*(0.00015)<distanceHome){
                returnHome = true;
                setCurrentDestination();
            }
            if (this.maxMoves == 0){stillFly = false;}
            if (returnHome && currentPosition.closeTo(initialPosition)){
                stillFly = false;

            }

        }

    }

    /**
     * Setting the destination by deciding either going back
     * to starting position or fulfillment of all orders.
     *
     */
    private void setCurrentDestination() {
        List<LongLat> currentOrderLocations = getLocations(orderLocations.get(currOrderIndex));
        currLocOrderIndex = currLocOrderIndex + 1;
        if (returnHome){
            currentDest = initialPosition;
            return;
        }
        if (this.currLocOrderIndex == currentOrderLocations.size()){
            ffOrders.add(orderLocations.get(currOrderIndex));
            currOrderIndex = currOrderIndex + 1;
            currLocOrderIndex = 0;
            if (this.currOrderIndex == orderLocations.size()){
                currOrderIndex = currOrderIndex -1;
                returnHome = true;
                currentDest = initialPosition;
                return;
            }
            currentOrderLocations = getLocations(orderLocations.get(currOrderIndex));
        }
        currentDest = currentOrderLocations.get(currLocOrderIndex);
        System.out.println(currentPosition);
    }

    /**
     * Drone move function. Checking if the drone will enter no fly zones in the next move. If it does,
     * we change the angle. If not, the drone continues to go direct to the pick or drop the order.
     * @param angle
     */
    private void droneMove(int angle){
        LongLat theNextPosition = this.currentPosition.nextPosition(angle);

        if (intersectionNoFlyZone(this.currentPosition,theNextPosition) || !theNextPosition.isConfined()){
            angle = antiClockWiseDirection(angle);
            droneMove(angle);
            droneMove(angle);
        }else {
            String orderNo = orderLocations.get(currOrderIndex).orderNo;
            LongLat.Move newMove = new LongLat.Move(orderNo, angle, currentPosition,currentPosition.nextPosition(angle));
            this.maxMoves = this.maxMoves -1;
            this.currentPosition = theNextPosition;
            way.add(newMove);
            flightPaths.add(Point.fromLngLat(currentPosition.longitude, currentPosition.latitude));
            if (currentPosition.closeTo(currentDest)){
                LongLat.Move hover = new LongLat.Move(orderNo, -999 ,currentPosition, currentPosition);
                this.maxMoves = this.maxMoves -1;
                way.add(hover);


            }
        }
    }

    /**
     * We check if the new position intersects No-Fly Zones.
     * @param theNextPosition
     * @param startPosition
     * @return
     */
    private boolean intersectionNoFlyZone(LongLat theNextPosition, LongLat startPosition) {
        var moveLine = new Line2D.Double(startPosition.longitude,startPosition.latitude, theNextPosition.longitude, theNextPosition.latitude);
        for (Line2D area: this.noFlyAreas){
            if (area.intersectsLine(moveLine)){
                return true;
            }
            System.out.println(moveLine);
        }
        return false;

    }

    /**
     * This is the direction of the drone from Appleton Tower and
     * back to the Appleton Tower after deliveries.
     * @param angle
     * @return
     */
    private int antiClockWiseDirection(int angle){return (angle - 10) %360;}

    /**
     * Creating flightpath data.
     * @param directory
     * @throws SQLException
     */
    public void setFlightPathTable(String directory) throws SQLException {
        this.conn = DriverManager.getConnection(this.jdbc + directory);
        this.databaseMetaData = conn.getMetaData();
        this.resultSet = databaseMetaData.getTables(null,null,"FLIGHTPATH",null);
        this.statement = conn.createStatement();
        if (resultSet.next()){
            statement.execute("DROP TABLE FLIGHTPATH");
           }
        statement.execute("CREATE TABLE FLIGHTPATH( orderNo CHAR(8), fromLongitude double, fromLatitude double, angle integer, toLongitude double, toLatitude double");
        this.ps = conn.prepareStatement("INSERT INTO FLIGHTPATH VALUES (?, ?, ?, ?, ?, ?, ?)");
        for (LongLat.Move move : way){
            ps.setString(1, move.orderNo);
            ps.setDouble(2, move.fromLong);
            ps.setDouble(3, move.fromLat);
            ps.setInt(4, move.angle);
            ps.setDouble(5, move.toLong);
            ps.setDouble(6, move.toLat);
            ps.execute();

        }

    }
}