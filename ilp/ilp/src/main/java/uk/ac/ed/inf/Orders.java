package uk.ac.ed.inf;
import com.google.gson.Gson;

import java.sql.Date;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.sql.*;


public class Orders {
    public static final String DERBY_DB = "/derbyDB";
    String jdbcString; //For the database connection and queries.
    Menus menus; //must be connected to httpHandler.
    HttpHandler httpHandler; //Must be connected to menus
    String port;
    String localHost; // will be used for web server and database server.
    private Connection conn;
    private DatabaseMetaData databaseMetaData;
    private ResultSet resultSet;
    private Statement statement;
    private PreparedStatement ps;


    /**
     * We will need these to create the database queries.
     * @param localHost
     * @param databasePort
     * @param httpPort
     */
    public Orders(String localHost, String databasePort, String httpPort) {
        this.localHost = localHost; //localhost, used for httpHandler and database server.
        this.jdbcString = "jdbc:derby://localhost:" + databasePort + DERBY_DB;
        this.port = databasePort; // 1527 and 9876 for example.
        this.httpHandler = new HttpHandler(localHost, httpPort);
        this.menus = new Menus(localHost, httpPort);
    }

    public int deliveryInfo(ArrayList<Order> orders) {
        return orders.size();
    }


    /**
     * The order class.
     */
    public class Order {
        String orderNo;
        String customer;
        String item;
        String deliverFrom;
        String deliverTo;
        String deliveryDate;

        /**
         * Constructing the order class
         * @param orderNo
         * @param customer
         * @param item
         * @param deliverFrom
         * @param deliverTo
         * @param deliveryDate
         */
        public Order(String orderNo, String customer, String item, String deliverFrom,
                     String deliverTo, String deliveryDate) {
            this.orderNo = orderNo;
            this.customer = customer;
            this.item = item;
            this.deliverFrom = deliverFrom;
            this.deliverTo = deliverTo;
            this.deliveryDate = deliveryDate;
        }

        /**
         * getter for order number.
         * @return order number
         */
        public String getOrderNo() {return orderNo;}


    }

    /**
     * Getting orders from the database server. Parsing from database into Order class format.
     * @param localHost
     * @param date
     * @return
     * @throws SQLException
     */
    public ArrayList<Order> getOrders(String localHost, Date date) throws SQLException {
        String orderQuery;
        ArrayList<Order> orderList = new ArrayList<>();
        try {
            Connection connection = DriverManager.getConnection(this.jdbcString);
            Statement statement = connection.createStatement();
            orderQuery = "SELECT o.orderNo, item, deliverTo, customer, deliveryDate " +
                    "FROM orderDetails JOIN (SELECT * FROM orders WHERE deliveryDate=(?)) o " +
                    "ON orderDetails.orderNo = o.orderNo";
            PreparedStatement preparedStatement = connection.prepareStatement(orderQuery);
            preparedStatement.setString(1, String.valueOf(date));

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                String orderNumber = resultSet.getString("orderNo");
                String orderDeliveryDate = resultSet.getString("deliveryDate");
                String orderCustomer = resultSet.getString("customer");
                String orderDeliverTo = resultSet.getString("deliverTo");
                String orderItems = resultSet.getString("item");
                String orderDeliverFrom = menus.getRestaurant(orderItems);
                System.out.println(orderCustomer + " " + orderDeliveryDate + " " + orderCustomer + " " + orderDeliverTo + " " + orderItems + " " + orderDeliverTo);

                Order currentOrder = new Order(orderNumber,orderDeliveryDate, orderCustomer,orderDeliverTo,orderItems,orderDeliverFrom);

                orderList.add(currentOrder);}}
        catch (Exception e) {
            System.err.println("ERROR");
            System.err.println(e.getMessage());
        }
        return orderList;
    }

    /**
     * WhatThreeWords names in the webserver. Simply parsing.
     */
    public class WhatThreeWords {
        Coordinates coordinates;
        public class Coordinates {
            double lng;
            double lat;
        }
    }

    /**
     * Splitting WhatThreeWords into the correct format in the database by splitting them into 3 words.
     * Getting them from the webserver.
     * @param threeWords
     * @return
     */
    private LongLat getCoordinates(String threeWords){

        String[] s = threeWords.split(Pattern.quote("."));
        String directoryName = "/words/" + s[0] + "/" + s[1] + "/" + s[2] + "/" + "details.json";
        String string_threeWords = httpHandler.getRequest(directoryName);
        Orders.WhatThreeWords whatThreeWords = new Gson().fromJson(string_threeWords, WhatThreeWords.class);
        System.out.println(directoryName);
        return new LongLat(whatThreeWords.coordinates.lng, whatThreeWords.coordinates.lat);
    }

    /**
     * Class for all delivery informations.
     */
    public class DeliveryInformation {
        String orderNo;
        LongLat deliverTo;
        String[] items;
        List<LongLat> deliverFrom;
        int deliveryCost;

        /**
         * Constructor for the DeliveryInformation class.
         * @param orderNo
         * @param deliverTo
         * @param items
         * @param deliverFrom
         * @param to
         */
        public DeliveryInformation(String orderNo, LongLat deliverTo, String[] items, List<LongLat> deliverFrom, String to) {
            this.orderNo = orderNo;
            this.deliverTo = deliverTo;
            this.items = items;
            this.deliverFrom = deliverFrom;
            this.deliveryCost = menus.getDeliveryCost(items);
        }

        /**
         * Creating a huge list for DeliveryInformation related to their order numbers.
         * @param orders
         * @return
         */
        public ArrayList<DeliveryInformation> deliveryInfo(ArrayList<Order> orders) {
            ArrayList<DeliveryInformation> deliveryInformationArrayList = new ArrayList<DeliveryInformation>();
            Map<String, List<Order>> groupByOrder = orders.stream().collect(Collectors.groupingBy(Order::getOrderNo));

            for (Map.Entry<String,List<Order>> listEntry: groupByOrder.entrySet()){
                List<Order> values = listEntry.getValue();
                ArrayList<String> items = new ArrayList<>();
                ArrayList<LongLat> deliveryFromPlaces = new ArrayList<>();
                for (Order order: values){
                    items.add(order.item);
                    LongLat currentDeliveryFrom = getCoordinates(order.deliverFrom);
                    deliveryFromPlaces.add(currentDeliveryFrom);
                }
                LongLat currentDeliveryTo = getCoordinates(values.get(0).deliverTo);
                deliveryInformationArrayList.add(new DeliveryInformation(listEntry.getKey(), currentDeliveryTo,
                        items.toArray(items.toArray(new String[items.size()])), deliveryFromPlaces, values.get(0).deliverTo));
            }return  deliveryInformationArrayList;}

        public String getOrderNo() {return orderNo;}
    }

    /**
     * Creating the delivery database table.
     * @param orders
     * @param directory
     * @throws SQLException
     */
    public void setDeliveryTable(List<DeliveryInformation> orders, String directory) throws SQLException {
        this.conn = DriverManager.getConnection(this.jdbcString + directory);
        this.databaseMetaData = conn.getMetaData();
        this.resultSet = databaseMetaData.getTables(null,null, "DELIVERIES", null);
        this.statement = conn.createStatement();
        if (resultSet.next()){
            statement.execute("DROP TABLE DELIVERIES");
          }
        statement.execute("CREATE TABLE DELIVERIES( orderNo CHAR(8), deliveredTo VARCHAR(19), costInPence int)");
        this.ps = conn.prepareStatement("INSERT INTO DELIVERIES VALUES (?, ?, ?)");
        for (Orders.DeliveryInformation info: orders){
            ps.setString(1, info.orderNo);
            //ps.setString(2, info. getThreeWords());
            ps.setInt(2, info.deliveryCost);
            ps.execute();
        }
    }

}























