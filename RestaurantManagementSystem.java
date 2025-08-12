import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

// Menu class
class Menu {
    private Map<Integer, String> items;
    private Map<Integer, Double> prices;

    public Menu() {
        items = new HashMap<>();
        prices = new HashMap<>();
    }

    public void addItem(int id, String name, double price) {
        items.put(id, name);
        prices.put(id, price);
    }

    public void displayMenu() {
        System.out.println("------ Menu ------");
        for (int id : items.keySet()) {
            System.out.println(id + ". " + items.get(id) + " - ₹" + prices.get(id));
        }
    }

    public double getPrice(int id) {
        return prices.getOrDefault(id, 0.0);
    }

    public String getName(int id) {
        return items.getOrDefault(id, "Unknown");
    }
}

// Customer class
class Customer {
    private int id;
    private String name;

    public Customer(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
}

// Order class
class Order {
    private int orderId;
    private Customer customer;
    private Map<Integer, Integer> orderItems; // MenuItemID -> Quantity

    public Order(int orderId, Customer customer) {
        this.orderId = orderId;
        this.customer = customer;
        this.orderItems = new HashMap<>();
    }

    public void addItem(int menuId, int quantity) {
        orderItems.put(menuId, orderItems.getOrDefault(menuId, 0) + quantity);
    }

    public Map<Integer, Integer> getItems() {
        return orderItems;
    }

    public int getOrderId() {
        return orderId;
    }

    public Customer getCustomer() {
        return customer;
    }
}

// Billing class
class Billing {
    public static double generateBill(Order order, Menu menu) {
        double total = 0;
        System.out.println("------ Bill for " + order.getCustomer().getName() + " ------");
        for (Map.Entry<Integer, Integer> entry : order.getItems().entrySet()) {
            String itemName = menu.getName(entry.getKey());
            double price = menu.getPrice(entry.getKey());
            int quantity = entry.getValue();
            double cost = price * quantity;
            System.out.println(itemName + " x" + quantity + " = ₹" + cost);
            total += cost;
        }
        System.out.println("Total: ₹" + total);
        return total;
    }
}

// Database Utility class
class DBUtil {
    private static final String URL = "jdbc:mysql://localhost:3306/restaurant";
    private static final String USER = "root";
    private static final String PASS = "password";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}

// Multithreaded Order Processor
class OrderProcessor implements Runnable {
    private BlockingQueue<Order> orderQueue;
    private Menu menu;

    public OrderProcessor(BlockingQueue<Order> orderQueue, Menu menu) {
        this.orderQueue = orderQueue;
        this.menu = menu;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Order order = orderQueue.take();
                System.out.println("Processing order ID: " + order.getOrderId());
                Billing.generateBill(order, menu);
                Thread.sleep(1000); // simulate processing time
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}

// Main Application
public class RestaurantManagementSystem {
    public static void main(String[] args) {
        Menu menu = new Menu();
        menu.addItem(1, "Pizza", 250);
        menu.addItem(2, "Burger", 150);
        menu.addItem(3, "Pasta", 200);

        BlockingQueue<Order> orderQueue = new LinkedBlockingQueue<>();
        Thread processorThread = new Thread(new OrderProcessor(orderQueue, menu));
        processorThread.start();

        Customer c1 = new Customer(1, "Deepak Kumar");
        Order o1 = new Order(101, c1);
        o1.addItem(1, 2);
        o1.addItem(3, 1);

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            String insertSQL = "INSERT INTO orders (order_id, customer_id, item_id, quantity) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSQL)) {
                for (Map.Entry<Integer, Integer> entry : o1.getItems().entrySet()) {
                    ps.setInt(1, o1.getOrderId());
                    ps.setInt(2, o1.getCustomer().getId());
                    ps.setInt(3, entry.getKey());
                    ps.setInt(4, entry.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        orderQueue.add(o1);
    }
}
