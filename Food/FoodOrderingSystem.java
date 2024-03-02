import java.util.logging.Logger;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.List;

class User {
    private String username;
    private String password;
    private String role;
    
    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public User(ResultSet resultSet) throws SQLException {
        this.username = resultSet.getString("username");
        this.password = resultSet.getString("password");
        this.role = resultSet.getString("role");
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public PreparedStatement toPreparedStatement(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO users (username, password, role) VALUES (?, ?, ?)");
        preparedStatement.setString(1, this.username);
        preparedStatement.setString(2, this.password);
        preparedStatement.setString(3, this.role);
        return preparedStatement;
    }
}

class Admin {
    private String username;
    private String password;

    public Admin(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}


class UserManager {
    private Connection connection;
    private Map<String, User> users;

    public UserManager() {
        connectToDatabase();
        createTableIfNotExists();
        users = new HashMap<>();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:database.db");
    }

    private void connectToDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:database.db");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTableIfNotExists() {
        try (Statement statement = connection.createStatement()) {
            String createUserTableQuery = "CREATE TABLE IF NOT EXISTS users (username TEXT UNIQUE PRIMARY KEY, password TEXT, role TEXT, is_online INTEGER, is_available INTEGER)";
            statement.executeUpdate(createUserTableQuery);
            System.out.println("Table created or already exists.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean signUp(String username, String password, String role ) {
        if (!users.containsKey(username)) {
            users.put(username, new User(username, password,role));
            saveUserToDatabase(users.get(username));
            return true;
        }
        return false;
    }

    public boolean login(String username, String password) {
        User user = users.get(username);
        return user != null && user.getPassword().equals(password);
    }

    public void loadUsersFromDatabase() {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM users")) {
            while (resultSet.next()) {
                User user = new User(resultSet);
                users.put(user.getUsername(), user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveUserToDatabase(User user) {
        try (PreparedStatement preparedStatement = user.toPreparedStatement(connection)) {
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void viewAllUsers() {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM users")) {
            System.out.println("All Users:");
            while (resultSet.next()) {
                System.out.println("Username: " + resultSet.getString("username") +
                        ", Role: " + resultSet.getString("role"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addAdmin(String username, String password) {
        if (!users.containsKey(username)) {
            users.put(username, new User(username, password, "admin"));
            saveUserToDatabase(users.get(username));
        } else {
            System.out.println("Admin account already exists.");
        }
    }

    public void deleteAccount(String username) {
        try (PreparedStatement preparedStatementUser = connection.prepareStatement("DELETE FROM users WHERE username = ?")) {
            preparedStatementUser.setString(1, username);
            preparedStatementUser.executeUpdate();
            System.out.println("User account deleted: " + username);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

class FoodItem {
    private int id;
    private String name;
    private double price;

    public FoodItem(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public FoodItem(int id,String name, double price) {
        this.id=id;
        this.name = name;
        this.price = price;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public PreparedStatement toPreparedStatement(Connection connection) throws SQLException {
        PreparedStatement preparedStatement;
        if (id == 0) {
            preparedStatement = connection.prepareStatement("INSERT INTO items (name, price) VALUES (?, ?)");
        } else {
            preparedStatement = connection.prepareStatement("INSERT INTO items (id, name, price) VALUES (?, ?, ?)");
            preparedStatement.setInt(1, id);
        }
        preparedStatement.setString(id == 0 ? 1 : 2, this.name);
        preparedStatement.setDouble(id == 0 ? 2 : 3, this.price);
        return preparedStatement;
    }
    
}

class Order {
    private int id;
    private Map<FoodItem, Integer> items;
    private DeliveryPerson deliveryPerson;

    public int getId() {
        return id;
    }

    public Order() {
        items = new HashMap<>();
    }

    public void addItem(FoodItem item, int quantity) {
        items.put(item, items.getOrDefault(item, 0) + quantity);
    }

    public Map<FoodItem, Integer> getItems() {
        return items;
    }

    public double calculateTotal() {
        double total = 0.0;
        for (Map.Entry<FoodItem, Integer> entry : items.entrySet()) {
            total += entry.getKey().getPrice() * entry.getValue();
        }
        return total;
    }
    public void setDeliveryPerson(DeliveryPerson deliveryPerson) {
        this.deliveryPerson = deliveryPerson;
    }

    public DeliveryPerson getDeliveryPerson() {
        return deliveryPerson;
    }
}

class Menu {
    private ArrayList<FoodItem> items;

    public Menu() {
        items = new ArrayList<>();
    }

    public void addItem(FoodItem item) {
        items.add(item);
    }

    public ArrayList<FoodItem> getItems() {
        return items;
    }
}

class DeliveryPerson {
    private String username;
    private boolean isOnline;
    private Delivery assignedDelivery;
    private boolean isAvailable;

    public DeliveryPerson(String username) {
        this.username = username;
        this.isOnline=false;
        this.isAvailable=true;
    }

    public String getName() {
        return username;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }
    public Delivery getAssignedDelivery() {
        return assignedDelivery;
    }
    public void setAssignedDelivery(Delivery assignedDelivery) {
        this.assignedDelivery = assignedDelivery;
    }
}

class Delivery {
    private String details;
    private List<DeliveryPerson> deliveryPersons;
    
    public Delivery(String details) {
        this.details = details;
        this.deliveryPersons = new ArrayList<>();
    }

    public String getDetails() {
        return details;
    }

    public void addDeliveryPerson(DeliveryPerson deliveryPerson) {
        this.deliveryPersons.add(deliveryPerson);
    }

    public List<DeliveryPerson> getDeliveryPersons() {
        return deliveryPersons;
    }

    public boolean assignToAvailableDeliveryPerson() {
        for (DeliveryPerson deliveryPerson : deliveryPersons) {
            if (deliveryPerson.isOnline() && deliveryPerson.getAssignedDelivery() == null) {
                deliveryPerson.setAssignedDelivery(this);
                System.out.println("Delivery assigned to: " + deliveryPerson.getName());
                System.out.println("Delivery details: " + getDetails());
                return true;
            }
        }
        return false;
    }

    public List<DeliveryPerson> getAvailableDeliveryPersons() {
        List<DeliveryPerson> availableDeliveryPersons = new ArrayList<>();
        for (DeliveryPerson deliveryPerson : deliveryPersons) {
            if (deliveryPerson.isOnline()) {
                availableDeliveryPersons.add(deliveryPerson);
            }
        }
        return availableDeliveryPersons;
    }
}

class OrderManager {
    private static ArrayList<Order> orders = new ArrayList<>();

    public static void placeOrder(Order order) {
        orders.add(order);
    }
    public static ArrayList<Order> getOrders() {
        return orders;
    }
}

public class FoodOrderingSystem {
    private static UserManager userManager;
    private static Scanner scanner;
    private static final Logger logger = Logger.getLogger(FoodOrderingSystem.class.getName());

    private static void saveFoodItemToDatabase(FoodItem foodItem) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement = foodItem.toPreparedStatement(connection)) {
                preparedStatement.executeUpdate();
                System.out.println("Food item added to the database!");
            }
        } catch (SQLException e) {
            System.err.println("Error saving food item to the database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void saveDeliveryPersonToDatabase(DeliveryPerson deliveryPerson) {
        try (Connection connection = userManager.getConnection();
            PreparedStatement checkStatement = connection.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?");
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO users (username, password, role, is_online, is_available) VALUES (?, ?, ?, ?, ?)")) {
            checkStatement.setString(1, deliveryPerson.getName());
            ResultSet resultSet = checkStatement.executeQuery();
            resultSet.next();
            int count = resultSet.getInt(1);
            resultSet.close();
            if(count == 0){
                preparedStatement.setString(1, deliveryPerson.getName());
                preparedStatement.setString(2, "");
                preparedStatement.setString(3, "delivery man");
                preparedStatement.setBoolean(4, deliveryPerson.isOnline());
                preparedStatement.setBoolean(5, deliveryPerson.isAvailable());
                preparedStatement.executeUpdate();
                System.out.println("Delivery person added to the database!");
            }
        } catch (SQLException e) {
                e.printStackTrace();
        }
    }
    

    private static void updateDeliveryPersonStatus(DeliveryPerson deliveryPerson) {
        System.out.println("Do you want to set yourself online? (1 for Yes, 0 for No): ");
        int setOnlineChoice = scanner.nextInt();
        if (setOnlineChoice == 1) {
            deliveryPerson.setOnline(true);
            deliveryPerson.setAvailable(true);
            saveDeliveryPersonToDatabase(deliveryPerson);
            System.out.println("You are now online.");
        } else if (setOnlineChoice == 0) {
            deliveryPerson.setOnline(false);
            deliveryPerson.setAvailable(false);
            saveDeliveryPersonToDatabase(deliveryPerson);
            System.out.println("You are now offline.");
        } else {
            System.out.println("Invalid choice. Please try again.");
            updateDeliveryPersonStatus(deliveryPerson);
        }
    }

    public static void main(String[] args) {

        logger.info("Application started");
        userManager = new UserManager();
        userManager.loadUsersFromDatabase();
        scanner = new Scanner(System.in);
        while (true) {
            clearConsole();
            System.out.println("1. Sign Up");
            System.out.println("2. Login");
            System.out.println("3. Admin Login");
            System.out.println("4. Exit");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();
            switch (choice) {
                case 1:
                    performSignUp();
                    break;
                case 2:
                    performLogin();
                    break;
                case 3:
                    performAdminLogin();
                    break;
                case 4:
                    System.out.println("Exiting the system. Goodbye!");
                    System.exit(0);
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void performSignUp() {
        clearConsole();
        System.out.print("Enter your username: ");
        String signUpUsername = scanner.nextLine();
        System.out.print("Enter your password: ");
        String signUpPassword = scanner.nextLine();
        System.out.print("Enter your role (user, merchant, delivery man): ");
        String signUpRole = scanner.nextLine();
        if (userManager.signUp(signUpUsername, signUpPassword,signUpRole)) {
            System.out.println("Sign up successful!");
            if ("user".equals(signUpRole)) {
                performFoodOrdering();
            } else if ("delivery man".equals(signUpRole)) {
                DeliveryPerson deliveryPerson = new DeliveryPerson(signUpUsername);
                updateDeliveryPersonStatus(deliveryPerson);
                performDeliveryFunctionality();
            } else if ("merchant".equals(signUpRole)) {
                System.out.println("Welcome to Merchant Panel! You can manage your restaurant by using the following functionalities:\n" +
                        "1. Manage Food Items\n" +
                        "2. View Orders");
                performMerchantFunctionality();
            } else {
                System.out.println("Invalid role. Please try again.");
            }
        } else {
            System.out.println("Username already exists. Please try again.");
        }
    }

    private static void performLogin() {
        clearConsole();
        System.out.print("Enter your username: ");
        String loginUsername = scanner.nextLine();
        System.out.print("Enter your password: ");
        String loginPassword = scanner.nextLine();
        System.out.print("Enter your role (user, merchant, delivery man): ");
        String loginRole = scanner.nextLine();
        clearConsole();
        if (userManager.login(loginUsername, loginPassword)) {
            System.out.println("Login successful!");
            if ("user".equals(loginRole)) {
                performRoleSpecificFunctionality(loginRole,loginUsername);
                performFoodOrdering();
            } else if ("delivery man".equals(loginRole)) {
                DeliveryPerson deliveryPerson = new DeliveryPerson(loginUsername);
                updateDeliveryPersonStatus(deliveryPerson);
                performDeliveryFunctionality();
            } else if ("merchant".equals(loginRole)) {
                System.out.println("Welcome to Merchant Panel! You can manage your restaurant by using the following functionalities:\n" +
                        "1. Manage Food Items\n" +
                        "2. View Orders");
                performMerchantFunctionality();
            } else {
                System.out.println("Invalid role. Please try again.");
            }
        } else {
            System.out.println("Invalid username or password. Please try again.");
        }
    }

    private static void performAdminLogin() {
        clearConsole();
        System.out.print("Enter admin username: ");
        String adminUsername = scanner.nextLine();
        System.out.print("Enter admin password: ");
        String adminPassword = scanner.nextLine();

        if (adminUsername.equals("admin") && adminPassword.equals("admin")) {
            System.out.println("Admin login successful!");
            performAdminFunctionality();
        } else {
            System.out.println("Invalid admin credentials. Please try again.");
        }
    }

    private static void performAdminFunctionality() {
        System.out.println("Admin functionalities:");
        System.out.println("1. View All Users");
        System.out.println("2. Delete User Account");
        System.out.println("3. Logout");
        System.out.print("Choose an option: ");
        int adminChoice = scanner.nextInt();
        scanner.nextLine();
        clearConsole();
        switch (adminChoice) {
            case 1:
                userManager.viewAllUsers();
                break;
            case 2:
                System.out.print("Enter the username to delete: ");
                String usernameToDelete = scanner.nextLine();
                userManager.deleteAccount(usernameToDelete);
                break;
            case 3:
                System.out.println("Logging out as admin.");
                return;
            default:
                System.out.println("Invalid choice. Please try again.");
        }
        performAdminFunctionality();
    }

    private static void performRoleSpecificFunctionality(String role,String username) {
        switch (role) {
            case "user":
                performFoodOrdering();
                break;
            case "merchant":
                performMerchantFunctionality();
                break;
            case "delivery man":
                DeliveryPerson deliveryPerson = new DeliveryPerson(username);
                updateDeliveryPersonStatus(deliveryPerson);
                performDeliveryFunctionality();
                break;
            default:
                System.out.println("Invalid role.");
                break;
        }
    }

    private static void performFoodOrdering() {
        clearConsole();
        Menu menu = new Menu();
        menu.addItem(new FoodItem("Burger", 5.99));
        menu.addItem(new FoodItem("Pizza", 8.99));
        menu.addItem(new FoodItem("Salad", 3.99));
        Order order = new Order();

        while (true) {
            System.out.println("1. Order Food");
            System.out.println("2. View Cart");
            System.out.println("3. Checkout");
            System.out.println("4. Exit");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            switch (choice) {
                case 1:
                    displayMenu(menu);
                    placeOrder(menu, order);
                    break;
                case 2:
                    displayCart(order);
                    break;
                case 3:
                    checkout(order);
                    sendOrderToMerchant(order);
                    break;
                case 4:
                    System.out.println("Exiting the food ordering system. Goodbye!");
                    System.exit(0);
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void saveOrderToDatabase(Order order) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:database.db")) {
            for (Map.Entry<FoodItem, Integer> entry : order.getItems().entrySet()) {
                FoodItem item = entry.getKey();
                int quantity = entry.getValue();
                try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO orders (item_name, quantity) VALUES (?, ?)")) {
                    preparedStatement.setString(1, item.getName());
                    preparedStatement.setInt(2, quantity);
                    preparedStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private static void sendOrderToMerchant(Order order) {
        System.out.println("Sending order to the merchant...");
        System.out.println("Order details:");
        for (Map.Entry<FoodItem, Integer> entry : order.getItems().entrySet()) {
            FoodItem item = entry.getKey();
            int quantity = entry.getValue();
            System.out.println("Item: " + item.getName() + ", Quantity: " + quantity);
        }
        System.out.println("Total: $" + order.calculateTotal());
        saveOrderToDatabase(order);
        System.out.println("Order sent to the merchant successfully!");
    }
    

    private static void performMerchantFunctionality() {
        clearConsole();
        Menu menu = new Menu();
        scanner.nextLine();
        while (true) {
            System.out.println("==================merchant panal=====================\n=====================================================");
            System.out.println("1. Add Food Item");
            System.out.println("2. View Menu");
            System.out.println("3. Choose delivery man");
            System.out.println("4. View orders");
            System.out.println("5. Exit");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            switch (choice) {
                case 1:
                    addFoodItem(menu);
                    break;
                case 2:
                    displayMenu(menu);
                    break;
                case 3:
                    viewDeliveryMen();
                    break;
                case 4:
                    viewOrders();
                    break;
                case 5:
                    System.out.println("Exiting the merchant functionality. Goodbye!");
                    System.exit(0);
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
    private static String viewDeliveryMen() {
        System.out.println("Available Delivery Men:");
        List<String> usernames = new ArrayList<>();
        try (Statement statement = userManager.getConnection().createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM users WHERE role='delivery man'")) {
            int index = 1;
            while (resultSet.next()) {
                String username = resultSet.getString("username");
                boolean isOnline = resultSet.getBoolean("is_online");
                boolean isAvailable = resultSet.getBoolean("is_available");
                System.out.println(index + ". Name: " + username);
                System.out.println("   Online: " + (isOnline ? "Offline" : "Online"));
                System.out.println("   Availability: " + (isAvailable ? "No" : "Yes"));
                System.out.println("----------");
                usernames.add(username);
                index++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    
        System.out.print("Choose a delivery person by entering the number (or 0 to cancel): ");
        int choice = scanner.nextInt();
        scanner.nextLine();
    
        if (choice >= 1 && choice <= usernames.size()) {
            String chosenUsername = usernames.get(choice - 1);
            System.out.println("The order is sent to " + chosenUsername);
            return chosenUsername;
        } else if (choice == 0) {
            return null;
        } else {
            System.out.println("Invalid choice. Returning to the main menu...");
            return viewDeliveryMen();
        }
    }

    private static void viewOrders() {
        System.out.println("View Orders:");
        try (Connection connection = userManager.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM orders")) {
            while (resultSet.next()) {
                int orderId = resultSet.getInt("id");
                int itemName = resultSet.getInt("item_name");
                int quantity = resultSet.getInt("quantity");
                System.out.println("Order ID: " + orderId);
                System.out.println("Item Name: " + itemName);
                System.out.println("Quantity: " + quantity);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private static void addFoodItem(Menu menu) {
        scanner.nextLine();
        System.out.print("Enter the name of the food item: ");
        String itemName = scanner.nextLine();
        System.out.print("Enter the price of the food item: ");
        double itemPrice = scanner.nextDouble();
        FoodItem foodItem = new FoodItem(itemName, itemPrice);
        saveFoodItemToDatabase(foodItem);
        menu.addItem(foodItem);
        System.out.println("Food item added to the menu!");
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:database.db");
    }

    private static void performDeliveryFunctionality() {
        clearConsole();
        List<DeliveryPerson> availableDeliveryPersons = getAvailableDeliveryPersons();
        if (!availableDeliveryPersons.isEmpty()) {
            System.out.println("Available Delivery Persons:");
            for (DeliveryPerson deliveryPerson : availableDeliveryPersons) {
                System.out.println("Name: " + deliveryPerson.getName());
                System.out.println("Online: " + (deliveryPerson.isOnline() ? "Online" : "Offline"));
                System.out.println("Available: " + (deliveryPerson.isAvailable() ? "Online" : "Offline"));
                System.out.println("----------");
            }
    
            ArrayList<Order> orders = OrderManager.getOrders();
            if (!orders.isEmpty()) {
                System.out.println("Orders waiting for acceptance:");
                for (int i = 0; i < orders.size(); i++) {
                    Order currentOrder = orders.get(i);
                    System.out.println((i + 1) + ". Order Total: $" + currentOrder.calculateTotal());
                }
                System.out.print("Enter the order number you want to accept (0 to exit): ");
                int orderNumber = scanner.nextInt();
                if (orderNumber >= 1 && orderNumber <= orders.size()) {
                    Delivery delivery = new Delivery("Delivery Details");
                    if (delivery.assignToAvailableDeliveryPerson()) {
                        Order acceptedOrder = orders.get(orderNumber - 1);
                        System.out.println("Delivery man accepted the order with total amount: $" + acceptedOrder.calculateTotal());
                        orders.remove(acceptedOrder);
                    }
                } else if (orderNumber != 0) {
                    System.out.println("Invalid order number. Please try again.");
                }
            } else {
                System.out.println("No orders waiting for acceptance.");
            }
        }
        System.out.println("Exiting the delivery portal. Goodbye!");
        System.exit(0);
    }

    private static void acceptOrder(Order order,  DeliveryPerson deliveryPerson) {
        System.out.println("Delivery man " + deliveryPerson.getName() +"accepted the order with total amount: $" + order.calculateTotal());
        deliveryPerson.setAvailable(true);
        OrderManager.getOrders().remove(order);
    }
    private static void displayMenu(Menu menu) {
        System.out.println("Menu:");
        List<FoodItem> items = menu.getItems();
        if (items == null || items.isEmpty()) {
            System.out.println("No items available in the menu.");
        } else {
            int index = 1;
            for (FoodItem item : items) {
                System.out.println(index + ". " + item.getName() + " - $" + item.getPrice());
                index++;
            }
        }
    }
    private static List<DeliveryPerson> deliveryPersons = new ArrayList<>();
    private static List<DeliveryPerson> getAvailableDeliveryPersons() {
        List<DeliveryPerson> availableDeliveryPersons = new ArrayList<>();
        for (DeliveryPerson deliveryPerson : deliveryPersons) {
            if (deliveryPerson.isOnline() && deliveryPerson.isAvailable()) {
                availableDeliveryPersons.add(deliveryPerson);
            }
        }
        return availableDeliveryPersons;
    }

    private static void placeOrder(Menu menu, Order order) {
        System.out.println("Enter the number of the item you want to order (0 to finish): ");
        int choice = scanner.nextInt();

        if (choice == 0) {
            return;
        }
        if (choice >= 1 && choice <= menu.getItems().size()) {
            System.out.println("Enter the quantity: ");
            int quantity = scanner.nextInt();
            order.addItem(menu.getItems().get(choice - 1), quantity);
        } else {
            System.out.println("Invalid choice. Please try again.");
        }
    }

    private static void displayCart(Order order) {
        System.out.println("Cart Contents:");
        for (Map.Entry<FoodItem, Integer> entry : order.getItems().entrySet()) {
            System.out.println(entry.getKey().getName() + " x " + entry.getValue());
        }
        System.out.println("Total: $" + order.calculateTotal());
    }

    private static void checkout(Order order) {
        System.out.println("Order placed successfully!");
        OrderManager.placeOrder(order);
    }

    private static void performMerchantOrDeliveryFunctionality() {
        DeliveryPerson deliveryPerson=null;
        System.out.println("Select your role:");
        System.out.println("1. User");
        System.out.println("2. Delivery Person");
        System.out.println("3. Exit");
        System.out.print("Choose an option: ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        clearConsole();
        switch (choice) {
            case 1:
                performMerchantFunctionality();
                break;
            case 2:
                performDeliveryFunctionality();
                break;
            case 3:
                System.out.println("Exiting the system. Goodbye!");
                System.exit(0);
            default:
                System.out.println("Invalid choice. Please try again.");
                performMerchantOrDeliveryFunctionality();
        }
        if (deliveryPerson != null) {
            displayOrdersForAcceptance(deliveryPerson);
        }
    }
    private static void displayOrdersForAcceptance(DeliveryPerson deliveryPerson) {
        ArrayList<Order> orders = OrderManager.getOrders();
        if (orders.isEmpty()) {
            System.out.println("No orders waiting for acceptance.");
            return;
        }
    
        System.out.println("Orders waiting for acceptance:");
        for (int i = 0; i < orders.size(); i++) {
            Order currentOrder = orders.get(i);
            System.out.println((i + 1) + ". Order Total: $" + currentOrder.calculateTotal());
        }
    
        System.out.print("Enter the order number you want to accept (0 to exit): ");
        int orderNumber = scanner.nextInt();
        if (orderNumber >= 1 && orderNumber <= orders.size()) {
            acceptOrder(orders.get(orderNumber - 1),deliveryPerson);
        } else if (orderNumber != 0) {
            System.out.println("Invalid order number. Please try again.");
        }
    
        performMerchantOrDeliveryFunctionality();
    }
}
