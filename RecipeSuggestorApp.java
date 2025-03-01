import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RecipeSuggestorApp {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "recipe_db";
    private static final String FULL_DB_URL = DB_URL + DB_NAME;
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "1234";
    private static final String CSV_FILE = "recipes.csv";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            initializeDatabase();
            new RecipeSuggestorApp().createAndShowGUI();
        });
    }

    private static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);

            try (Connection dbConn = DriverManager.getConnection(FULL_DB_URL, DB_USER, DB_PASSWORD);
                 Statement dbStmt = dbConn.createStatement()) {

                dbStmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS recipes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "dish_name VARCHAR(255) NOT NULL," +
                    "cuisine VARCHAR(100)," +
                    "course VARCHAR(50)," +
                    "ingredients TEXT," +
                    "prep_time INT," +
                    "cook_time INT," +
                    "total_time INT," +
                    "method TEXT," +
                    "servings INT" +
                    ")"
                );

                if (isTableEmpty(dbConn)) {
                    importFromCSV(dbConn);
                }
            }
        } catch (SQLException e) {
            showErrorDialog("Database initialization failed: " + e.getMessage(), true);
        }
    }

    private static boolean isTableEmpty(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM recipes")) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    private static void importFromCSV(Connection conn) {
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE))) {
            List<Recipe> recipes = parseCSV(br);
            insertRecipes(conn, recipes);
        } catch (IOException | SQLException e) {
            showErrorDialog("CSV import failed: " + e.getMessage(), true);
        }
    }

    private static List<Recipe> parseCSV(BufferedReader br) throws IOException {
        List<Recipe> recipes = new ArrayList<>();
        String line;
        boolean isFirstLine = true;

        while ((line = br.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                continue;
            }
            String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
            if (fields.length >= 9) {
                recipes.add(createRecipeFromCSV(fields));
            }
        }
        return recipes;
    }

    private static Recipe createRecipeFromCSV(String[] fields) {
        return new Recipe(
            cleanField(fields[0]),
            cleanField(fields[1]),
            cleanField(fields[2]),
            cleanField(fields[7]),
            parseInteger(fields[4]),
            parseInteger(fields[5]),
            parseInteger(fields[6]),
            cleanField(fields[3]),
            parseInteger(fields[8])
        );
    }

    private static String cleanField(String field) {
        return field.replaceAll("^\"|\"$", "").trim();
    }

    private static void insertRecipes(Connection conn, List<Recipe> recipes) throws SQLException {
        String sql = "INSERT INTO recipes (dish_name, cuisine, course, ingredients, prep_time, " +
                     "cook_time, total_time, method, servings) VALUES (?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Recipe recipe : recipes) {
                setRecipeParameters(pstmt, recipe);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private static void setRecipeParameters(PreparedStatement pstmt, Recipe recipe) throws SQLException {
        pstmt.setString(1, recipe.getDishName());
        pstmt.setString(2, recipe.getCuisine());
        pstmt.setString(3, recipe.getCourse());
        pstmt.setString(4, recipe.getIngredients());
        pstmt.setInt(5, recipe.getPrepTime());
        pstmt.setInt(6, recipe.getCookTime());
        pstmt.setInt(7, recipe.getTotalTime());
        pstmt.setString(8, recipe.getMethod());
        pstmt.setInt(9, recipe.getServings());
    }

    private static int parseInteger(String value) {
        try {
            return Integer.parseInt(value.trim().replaceAll("\"", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void showErrorDialog(String message, boolean fatal) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
        if (fatal) System.exit(1);
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Recipe Suggestor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null); // Center the window

        // Main panel with gradient background
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                Color color1 = new Color(255, 223, 186); // Light orange
                Color color2 = new Color(255, 183, 82);  // Darker orange
                g2d.setPaint(new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(new BorderLayout(10, 10));
        frame.add(mainPanel);

        // Title label with custom font and shadow
        JLabel titleLabel = new JLabel("Recipe Suggestor");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 36));
        titleLabel.setForeground(new Color(70, 50, 30)); // Dark brown
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Input panel with rounded corners
        JPanel inputPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, 200)); // Semi-transparent white
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            }
        };
        inputPanel.setOpaque(false);
        inputPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel inputLabel = new JLabel("Enter ingredients:");
        inputLabel.setFont(new Font("Arial", Font.BOLD, 16));
        inputLabel.setForeground(new Color(70, 50, 30)); // Dark brown

        JTextField ingredientInput = new JTextField(25);
        ingredientInput.setFont(new Font("Arial", Font.PLAIN, 14));
        ingredientInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 150, 100), 2),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        JButton findRecipesButton = new JButton("Find Recipes");
        findRecipesButton.setFont(new Font("Arial", Font.BOLD, 14));
        findRecipesButton.setBackground(new Color(255, 183, 82)); // Orange
        findRecipesButton.setForeground(Color.WHITE);
        findRecipesButton.setFocusPainted(false);
        findRecipesButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        inputPanel.add(inputLabel);
        inputPanel.add(ingredientInput);
        inputPanel.add(findRecipesButton);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        // Recipe display area with scrollable cards
        JPanel recipeCardsPanel = new JPanel();
        recipeCardsPanel.setLayout(new BoxLayout(recipeCardsPanel, BoxLayout.Y_AXIS));
        recipeCardsPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(recipeCardsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Load recipes and display initial list
        List<Recipe> recipes = loadRecipesFromDatabase();
        showRecipeSummaries(recipes, recipeCardsPanel, frame);

        // Action listener for recipe search
        findRecipesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                recipeCardsPanel.removeAll();
                String userIngredients = ingredientInput.getText();
                List<Recipe> suggestions = findRecipesByIngredients(recipes, userIngredients);
                if (suggestions.isEmpty()) {
                    JLabel noResults = new JLabel("No recipes found for the given ingredients.");
                    noResults.setFont(new Font("Arial", Font.ITALIC, 16));
                    noResults.setForeground(Color.RED);
                    recipeCardsPanel.add(noResults);
                } else {
                    showRecipeSummaries(suggestions, recipeCardsPanel, frame);
                }
                recipeCardsPanel.revalidate();
                recipeCardsPanel.repaint();
            }
        });

        frame.setVisible(true);
    }

    private List<Recipe> loadRecipesFromDatabase() {
        List<Recipe> recipes = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(FULL_DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM recipes")) {

            while (rs.next()) {
                Recipe recipe = new Recipe(
                    rs.getString("dish_name"),
                    rs.getString("cuisine"),
                    rs.getString("course"),
                    rs.getString("method"),
                    rs.getInt("prep_time"),
                    rs.getInt("cook_time"),
                    rs.getInt("total_time"),
                    rs.getString("ingredients"),
                    rs.getInt("servings")
                );
                recipes.add(recipe);
            }
        } catch (SQLException e) {
            showErrorDialog("Failed to load recipes from database: " + e.getMessage(), false);
        }
        return recipes;
    }

    private void showRecipeSummaries(List<Recipe> recipes, JPanel recipeCardsPanel, JFrame frame) {
        for (Recipe recipe : recipes) {
            JPanel summaryCard = createRecipeSummaryCard(recipe, frame);
            recipeCardsPanel.add(summaryCard);
            recipeCardsPanel.add(Box.createVerticalStrut(10)); // Spacing between cards
        }
        recipeCardsPanel.revalidate();
        recipeCardsPanel.repaint();
    }

    private JPanel createRecipeSummaryCard(Recipe recipe, JFrame frame) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, 220)); // Semi-transparent white
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        card.setOpaque(false);

        JLabel dishLabel = new JLabel("Dish: " + recipe.getDishName());
        dishLabel.setFont(new Font("Arial", Font.BOLD, 18));
        dishLabel.setForeground(new Color(70, 50, 30)); // Dark brown

        JLabel cuisineLabel = new JLabel("Cuisine: " + recipe.getCuisine());
        cuisineLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        cuisineLabel.setForeground(new Color(100, 70, 40)); // Lighter brown

        JButton viewDetailsButton = new JButton("View Details");
        viewDetailsButton.setFont(new Font("Arial", Font.BOLD, 14));
        viewDetailsButton.setBackground(new Color(255, 183, 82)); // Orange
        viewDetailsButton.setForeground(Color.WHITE);
        viewDetailsButton.setFocusPainted(false);
        viewDetailsButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        viewDetailsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showRecipeDetails(recipe, frame);
            }
        });

        card.add(dishLabel);
        card.add(cuisineLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(viewDetailsButton);

        return card;
    }

    private void showRecipeDetails(Recipe recipe, JFrame frame) {
        JDialog dialog = new JDialog(frame, "Recipe Details", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(frame);
        dialog.setLayout(new BorderLayout());

        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        detailsPanel.setBackground(new Color(255, 255, 255, 220)); // Semi-transparent white

        JLabel dishLabel = new JLabel("Dish: " + recipe.getDishName());
        dishLabel.setFont(new Font("Arial", Font.BOLD, 20));
        dishLabel.setForeground(new Color(70, 50, 30)); // Dark brown

        JLabel cuisineLabel = new JLabel("Cuisine: " + recipe.getCuisine());
        cuisineLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        cuisineLabel.setForeground(new Color(100, 70, 40)); // Lighter brown

        JLabel courseLabel = new JLabel("Course: " + recipe.getCourse());
        courseLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        courseLabel.setForeground(new Color(100, 70, 40)); // Lighter brown

        JLabel prepTimeLabel = new JLabel("Preparation Time: " + recipe.getPrepTime() + " mins");
        prepTimeLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        prepTimeLabel.setForeground(new Color(100, 70, 40)); // Lighter brown

        JLabel cookTimeLabel = new JLabel("Cooking Time: " + recipe.getCookTime() + " mins");
        cookTimeLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        cookTimeLabel.setForeground(new Color(100, 70, 40)); // Lighter brown

        JLabel totalTimeLabel = new JLabel("Total Time: " + recipe.getTotalTime() + " mins");
        totalTimeLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        totalTimeLabel.setForeground(new Color(100, 70, 40)); // Lighter brown

        JLabel servingsLabel = new JLabel("Servings: " + recipe.getServings());
        servingsLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        servingsLabel.setForeground(new Color(100, 70, 40)); // Lighter brown

        JTextArea methodArea = new JTextArea("Method: " + recipe.getMethod());
        methodArea.setLineWrap(true);
        methodArea.setWrapStyleWord(true);
        methodArea.setEditable(false);
        methodArea.setFont(new Font("Arial", Font.PLAIN, 14));
        methodArea.setBackground(new Color(255, 255, 255, 200)); // Semi-transparent white
        methodArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        detailsPanel.add(dishLabel);
        detailsPanel.add(cuisineLabel);
        detailsPanel.add(courseLabel);
        detailsPanel.add(prepTimeLabel);
        detailsPanel.add(cookTimeLabel);
        detailsPanel.add(totalTimeLabel);
        detailsPanel.add(servingsLabel);
        detailsPanel.add(Box.createVerticalStrut(10));
        detailsPanel.add(new JScrollPane(methodArea));

        dialog.add(detailsPanel, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font("Arial", Font.BOLD, 14));
        closeButton.setBackground(new Color(255, 183, 82)); // Orange
        closeButton.setForeground(Color.WHITE);
        closeButton.setFocusPainted(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private List<Recipe> findRecipesByIngredients(List<Recipe> recipes, String ingredients) {
        List<Recipe> suggestions = new ArrayList<>();
        String[] userIngredients = ingredients.split(",");
        for (Recipe recipe : recipes) {
            boolean match = true;
            for (String ingredient : userIngredients) {
                if (!recipe.getIngredients().toLowerCase().contains(ingredient.trim().toLowerCase())) {
                    match = false;
                    break;
                }
            }
            if (match) {
                suggestions.add(recipe);
            }
        }
        return suggestions;
    }
}

class Recipe {
    private String dishName;
    private String cuisine;
    private String course;
    private String method;
    private int prepTime;
    private int cookTime;
    private int totalTime;
    private String ingredients;
    private int servings;

    public Recipe(String dishName, String cuisine, String course, String method, int prepTime, int cookTime,
                  int totalTime, String ingredients, int servings) {
        this.dishName = dishName;
        this.cuisine = cuisine;
        this.course = course;
        this.method = method;
        this.prepTime = prepTime;
        this.cookTime = cookTime;
        this.totalTime = totalTime;
        this.ingredients = ingredients;
        this.servings = servings;
    }

    public String getDishName() {
        return dishName;
    }

    public String getCuisine() {
        return cuisine;
    }

    public String getCourse() {
        return course;
    }

    public String getMethod() {
        return method;
    }

    public int getPrepTime() {
        return prepTime;
    }

    public int getCookTime() {
        return cookTime;
    }

    public int getTotalTime() {
        return totalTime;
    }

    public String getIngredients() {
        return ingredients;
    }

    public int getServings() {
        return servings;
    }
}