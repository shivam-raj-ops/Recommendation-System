// RecommendationSystem.java

import java.util.*;
import java.util.Map.Entry;

public class RecommendationSystem {

    // Sample data: Map of users, where each user maps to a map of items and their ratings.
    // Ratings are on a scale, e.g., 1.0 to 5.0.
    private Map<String, Map<String, Double>> userData;

    public RecommendationSystem() {
        this.userData = new HashMap<>();
        // Initialize with some sample data
        loadSampleData();
    }

    /**
     * Loads sample user-item rating data into the system.
     * This simulates a database or external data source.
     */
    private void loadSampleData() {
        // User 1: Alice's ratings
        Map<String, Double> aliceRatings = new HashMap<>();
        aliceRatings.put("Item A", 5.0);
        aliceRatings.put("Item B", 3.0);
        aliceRatings.put("Item C", 4.0);
        aliceRatings.put("Item D", 4.0);
        userData.put("Alice", aliceRatings);

        // User 2: Bob's ratings
        Map<String, Double> bobRatings = new HashMap<>();
        bobRatings.put("Item A", 3.0);
        bobRatings.put("Item B", 1.0);
        bobRatings.put("Item C", 2.0);
        bobRatings.put("Item D", 3.0);
        bobRatings.put("Item E", 4.0);
        userData.put("Bob", bobRatings);

        // User 3: Charlie's ratings
        Map<String, Double> charlieRatings = new HashMap<>();
        charlieRatings.put("Item B", 4.0);
        charlieRatings.put("Item C", 5.0);
        charlieRatings.put("Item D", 5.0);
        charlieRatings.put("Item E", 3.0);
        userData.put("Charlie", charlieRatings);

        // User 4: David's ratings
        Map<String, Double> davidRatings = new HashMap<>();
        davidRatings.put("Item A", 4.0);
        davidRatings.put("Item B", 3.0);
        davidRatings.put("Item C", 4.0);
        davidRatings.put("Item E", 5.0); // David hasn't rated Item D
        userData.put("David", davidRatings);

        // User 5: Eve's ratings (new user, wants recommendations)
        Map<String, Double> eveRatings = new HashMap<>();
        eveRatings.put("Item A", 4.0);
        eveRatings.put("Item B", 5.0);
        // Eve hasn't rated C, D, E yet, and wants recommendations for them.
        userData.put("Eve", eveRatings);
    }

    /**
     * Calculates the Euclidean distance similarity between two users.
     * A smaller distance means higher similarity. We'll convert it to a similarity score.
     * Similarity = 1 / (1 + distance)
     *
     * @param user1Ratings Ratings of the first user.
     * @param user2Ratings Ratings of the second user.
     * @return A similarity score between 0 and 1, where 1 is identical.
     */
    private double calculateSimilarity(Map<String, Double> user1Ratings, Map<String, Double> user2Ratings) {
        if (user1Ratings == null || user2Ratings == null || user1Ratings.isEmpty() || user2Ratings.isEmpty()) {
            return 0.0; // Cannot calculate similarity if ratings are missing or empty
        }

        double sumOfSquares = 0.0;
        int commonItems = 0;

        // Iterate over items rated by user1
        for (Entry<String, Double> entry : user1Ratings.entrySet()) {
            String item = entry.getKey();
            Double rating1 = entry.getValue();

            // If user2 also rated this item
            if (user2Ratings.containsKey(item)) {
                Double rating2 = user2Ratings.get(item);
                sumOfSquares += Math.pow(rating1 - rating2, 2);
                commonItems++;
            }
        }

        // If no common items, similarity is 0
        if (commonItems == 0) {
            return 0.0;
        }

        // Euclidean distance
        double distance = Math.sqrt(sumOfSquares);

        // Convert distance to similarity score: 1 / (1 + distance)
        // This ensures higher similarity for smaller distances.
        return 1.0 / (1.0 + distance);
    }

    /**
     * Finds the most similar users to a given target user.
     *
     * @param targetUser The user for whom to find similar users.
     * @return A map of similar user names to their similarity scores, sorted by similarity (descending).
     */
    public Map<String, Double> findSimilarUsers(String targetUser) {
        Map<String, Double> targetUserRatings = userData.get(targetUser);
        if (targetUserRatings == null) {
            System.out.println("Target user '" + targetUser + "' not found.");
            return Collections.emptyMap();
        }

        Map<String, Double> similarities = new HashMap<>();

        // Iterate through all other users to calculate similarity
        for (Entry<String, Map<String, Double>> entry : userData.entrySet()) {
            String otherUser = entry.getKey();
            Map<String, Double> otherUserRatings = entry.getValue();

            // Don't compare a user to themselves
            if (!otherUser.equals(targetUser)) {
                double similarity = calculateSimilarity(targetUserRatings, otherUserRatings);
                if (similarity > 0) { // Only add if there's some commonality
                    similarities.put(otherUser, similarity);
                }
            }
        }

        // Sort similarities in descending order
        List<Entry<String, Double>> sortedSimilarities = new ArrayList<>(similarities.entrySet());
        sortedSimilarities.sort(Entry.comparingByValue(Comparator.reverseOrder()));

        Map<String, Double> sortedMap = new LinkedHashMap<>();
        for (Entry<String, Double> entry : sortedSimilarities) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    /**
     * Generates recommendations for a target user based on similar users' preferences.
     *
     * @param targetUser The user for whom to generate recommendations.
     * @param numRecommendations The maximum number of recommendations to return.
     * @return A list of recommended items, sorted by predicted rating (descending).
     */
    public List<String> getRecommendations(String targetUser, int numRecommendations) {
        Map<String, Double> targetUserRatings = userData.get(targetUser);
        if (targetUserRatings == null) {
            System.out.println("Target user '" + targetUser + "' not found.");
            return Collections.emptyList();
        }

        Map<String, Double> similarUsers = findSimilarUsers(targetUser);
        if (similarUsers.isEmpty()) {
            System.out.println("No similar users found for '" + targetUser + "'. Cannot provide recommendations.");
            return Collections.emptyList();
        }

        // Map to store predicted ratings for unrated items
        Map<String, Double> predictedRatings = new HashMap<>();
        // Map to store sum of similarities for normalization
        Map<String, Double> similaritySums = new HashMap<>();

        // Iterate through similar users
        for (Entry<String, Double> similarUserEntry : similarUsers.entrySet()) {
            String similarUserName = similarUserEntry.getKey();
            double similarity = similarUserEntry.getValue();
            Map<String, Double> similarUserRatings = userData.get(similarUserName);

            if (similarUserRatings != null) {
                // Iterate through items rated by the similar user
                for (Entry<String, Double> itemRatingEntry : similarUserRatings.entrySet()) {
                    String item = itemRatingEntry.getKey();
                    Double rating = itemRatingEntry.getValue();

                    // If the target user has not rated this item yet
                    if (!targetUserRatings.containsKey(item)) {
                        // Accumulate weighted rating and similarity sum
                        predictedRatings.put(item, predictedRatings.getOrDefault(item, 0.0) + (rating * similarity));
                        similaritySums.put(item, similaritySums.getOrDefault(item, 0.0) + similarity);
                    }
                }
            }
        }

        // Calculate final predicted ratings by normalizing
        Map<String, Double> finalPredictedRatings = new HashMap<>();
        for (Entry<String, Double> entry : predictedRatings.entrySet()) {
            String item = entry.getKey();
            double weightedSum = entry.getValue();
            double sumOfSimilarities = similaritySums.get(item);

            if (sumOfSimilarities > 0) {
                finalPredictedRatings.put(item, weightedSum / sumOfSimilarities);
            }
        }

        // Sort items by predicted rating in descending order
        List<Entry<String, Double>> sortedRecommendations = new ArrayList<>(finalPredictedRatings.entrySet());
        sortedRecommendations.sort(Entry.comparingByValue(Comparator.reverseOrder()));

        // Extract recommended item names
        List<String> recommendations = new ArrayList<>();
        for (int i = 0; i < Math.min(numRecommendations, sortedRecommendations.size()); i++) {
            recommendations.add(sortedRecommendations.get(i).getKey());
        }

        return recommendations;
    }

    public static void main(String[] args) {
        RecommendationSystem recommender = new RecommendationSystem();
        Scanner scanner = new Scanner(System.in);

        System.out.println("Welcome to the Simple Recommendation System!");
        System.out.println("Available users: " + recommender.userData.keySet());

        while (true) {
            System.out.print("\nEnter user name for recommendations (or 'exit' to quit): ");
            String userName = scanner.nextLine();

            if (userName.equalsIgnoreCase("exit")) {
                break;
            }

            if (!recommender.userData.containsKey(userName)) {
                System.out.println("User '" + userName + "' not found in the system. Please try again.");
                continue;
            }

            System.out.print("How many recommendations do you want? ");
            int numRecs;
            try {
                numRecs = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please enter an integer.");
                continue;
            }

            System.out.println("\n--- Similar Users for " + userName + " ---");
            Map<String, Double> similarUsers = recommender.findSimilarUsers(userName);
            if (similarUsers.isEmpty()) {
                System.out.println("No similar users found.");
            } else {
                similarUsers.forEach((user, similarity) ->
                        System.out.printf("- %s (Similarity: %.4f)\n", user, similarity)
                );
            }

            System.out.println("\n--- Top " + numRecs + " Recommendations for " + userName + " ---");
            List<String> recommendations = recommender.getRecommendations(userName, numRecs);

            if (recommendations.isEmpty()) {
                System.out.println("No recommendations could be generated for " + userName + ".");
            } else {
                for (int i = 0; i < recommendations.size(); i++) {
                    System.out.println((i + 1) + ". " + recommendations.get(i));
                }
            }
        }

        scanner.close();
        System.out.println("Exiting Recommendation System. Goodbye!");
    }
}
