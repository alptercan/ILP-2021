package uk.ac.ed.inf;

import com.mapbox.geojson.FeatureCollection;
import java.util.Objects;

public class LongLat {



    /**
     * MAX_V, MIN_V,
     * MAX_V1, and MAX_V1
     * shows the areas that the drone
     * makes the delivery
     * NOTE = longitude and latitude respectively.
     */
    final double MAX_V1 = -3.184319; //Maximum longitude
    final double MIN_V1 = -3192473; // Minimum longitude
    final double MAX_V = 55.946233; // Maximum latitude
    final double MIN_V = 55.942617; // Minimum latitude
    final double ONE_MOVE_DIST = 0.00015;
    final int HOVER = -999;

    double longitude;
    double latitude;
    /**
     * Starting point of the drone (Appleton Tower's coordinates).
     * Appleton Tower is the starting point.
     */
    public static final double AT_LONG = -3.186874;
    public static final double AT_LAT = 55.944494;
    boolean isHovering;


    public LongLat(double v, double v1) {
        this.longitude = v;
        this.latitude = v1;
        this.isHovering = false;
    }



    /**
     * this function should give us the allowed areas
     * that the drone must function.
     *
     * @return true if the drone goes around the confined area.
     */
    public boolean isConfined() {
        if ((MAX_V1 > longitude) && (MIN_V1 < longitude) && (MAX_V > latitude) && (MIN_V < latitude)) {
            return true;
        }
        return false;
    }

    /**
     * Need to find the pythagorean distance between current location and the business school
     *
     * @param businessSchool business school
     * @return Pythagorean distance between the drone and business school
     */

    public double distanceTo(LongLat businessSchool) {
        double x1 = Math.pow(this.longitude - businessSchool.longitude, 2);
        double x2 = Math.pow(this.latitude - businessSchool.latitude, 2);
        return Math.sqrt(x1 + x2);
    }

    /**
     * Check the drone if it is close to the target location.
     *
     * @param alsoAppletonTower Appleton Tower
     * @return true if the drone is close to the target location.
     */
    public boolean closeTo(LongLat alsoAppletonTower) {
        double currentDistance = this.distanceTo(alsoAppletonTower);
        if (ONE_MOVE_DIST >= currentDistance) {return true;}
        return false;
    }

    /**
     * We need to find the next drone move according to the angle. If the angle is -999 then we know that
     * the drone is landing or going up. Every drone move angle is 0.00015 degrees.
     * Finally the function gives us the drone move to the next position according to the angle input.
     *
     * @param i means the angle of the drone movement.
     * @return final position of the drone.
     * @throws IllegalArgumentException if the angle is not a multiple of 10.
     */
    public LongLat nextPosition(int i) {
        if (i != HOVER) {
            if (i % 10 != 0) {
                throw new IllegalArgumentException("Angle must be a multiple of 10");
            }
            double radians = Math.toRadians(i);
            double changeLong = ONE_MOVE_DIST * Math.cos(radians);
            double changeLat = ONE_MOVE_DIST * Math.sin(radians);
            this.longitude += changeLong;
            this.latitude += changeLat;

        }
        return new LongLat(this.longitude, this.latitude);
    }

    /**
     * This function helps us to get all the no fly zones in the webserver.
     * @param httpHandler
     * Result = The request that HttpHandler got. Contains the no fly zone geojson file.
     * @return result
     */
    public FeatureCollection getNoFlyZones(HttpHandler httpHandler) {
        String no_fly_zones = "/buildings/no-fly-zones.geojson";
        String result = httpHandler.getRequest(no_fly_zones);
        return FeatureCollection.fromJson(result);
    }

    /**
     * This function calculates the closest angle to destination.
     * @param destination
     * @return closest angle to destination.
     */
    public int getClosestAngleToDestination(LongLat destination) {

        double x = destination.getLongitude() - this.longitude;
        double y = destination.getLatitude() - this.latitude;
        double radians = Math.atan(y/x);
        double degrees = Math.toDegrees(radians);
        double angleFromStart = 0.0;
        double degrees180Plus = 180 + degrees;
        double degrees180Less = 180 - Math.abs(degrees);
        double degrees360Less = 360 - Math.abs(degrees);
        double returnValue = 10*(Math.round(angleFromStart/10));

        if (x > 0 && y > 0){ angleFromStart = degrees;}

        else if (x < 0 && y < 0){angleFromStart = degrees180Plus;}

        else if (x < 0 && y > 0){angleFromStart = degrees180Less;}

        else if (x > 0 && y < 0){angleFromStart = degrees360Less;}

        return (int) returnValue;
    }



    public static class Move {
        public String orderNo;
        public int angle;
        public double fromLong;
        public double fromLat;
        public double toLong;
        public double toLat;

        /**
         * Move class for moving between latitude ang longitude.
         * Initializing variables to change locations.
         * @param orderNo
         * @param angle
         * @param fromLocation
         * @param toLocation
         */

        public Move(String orderNo, int angle, LongLat fromLocation, LongLat toLocation) {
            this.orderNo = orderNo;

            this.fromLong = fromLocation.getLongitude();

            this.fromLat = fromLocation.getLatitude();

            this.toLong = toLocation.getLongitude();

            this.toLat = toLocation.getLatitude();

            this.angle = angle;
        }


        public Move(Move move) {
            this.orderNo = move.orderNo;

            this.fromLong = move.fromLong;

            this.fromLat = move.fromLat;

            this.angle = move.angle;

            this.toLong = move.toLong;

            this.toLat = move.toLat;
        }

        //Getters for Move Class.
        public String getOrderNo() {return orderNo;}

        public double getFromLongitude() {return fromLong;}

        public double getFromLatitude() {return fromLat;}

        public int getAngle() {return angle;}

        public double getToLongitude() {return toLong;}

        public double getToLatitude() {return toLat;}


        /**
         * Checks if two move objects are equal.
         * @param object
         * @return true in case it is equal, false otherwise.
         */
        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            Move move = (Move) object;
            return  Double.compare(move.fromLong, fromLong) == 0 &&
                    Double.compare(move.fromLat, fromLat) == 0 && angle == move.angle &&
                    Double.compare(move.toLong, toLong) == 0 &&
                    Double.compare(move.toLat, toLat) == 0 &&
                    Objects.equals(orderNo, move.orderNo);}


    }
    // Getters for longitude and latitude.
    double getLatitude() {return longitude;}

    double getLongitude() {return latitude;}
}




