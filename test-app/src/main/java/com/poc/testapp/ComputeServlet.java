package com.poc.testapp;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

public class ComputeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StringBuilder report = new StringBuilder();
        long start = System.nanoTime();

        clearDatabase();
        calculateFibonacci(report);
        handleUserOperations(report);
        buildTreeStructure(report);
        executeRecursiveTreeQuery(report);

        long totalElapsed = System.nanoTime() - start;
        report.append("Total time: " + (totalElapsed / 1_000_000) + " ms\n");

        resp.setContentType("text/plain");
        resp.getWriter().write(report.toString());
    }

    private void clearDatabase() {
        try {
            Class.forName("org.h2.Driver");
            try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")) {
                dropTable(conn, "users");
                dropTable(conn, "tree");
            }
        } catch (Exception ignored) {
        }
    }

    private void dropTable(Connection conn, String tableName) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS " + tableName);
        }
    }

    private void calculateFibonacci(StringBuilder report) {
        int n = 20;
        long fibStart = System.nanoTime();
        int fibResult = fibonacci(n);
        long fibElapsed = System.nanoTime() - fibStart;
        report.append("Fibonacci(" + n + ") = " + fibResult + " [" + (fibElapsed / 1_000_000) + " ms]\n");
    }

    private int fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }

    private void handleUserOperations(StringBuilder report) {
        try {
            Class.forName("org.h2.Driver");
            try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")) {
                createUsersTable(conn);
                performUserCycles(conn, report);
                printFinalUsers(conn, report);
            }
        } catch (Exception e) {
            report.append("User DB error: " + e.getMessage() + "\n");
        }
    }

    private void createUsersTable(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), age INT)");
        }
    }

    private void performUserCycles(Connection conn, StringBuilder report) throws SQLException {
        for (int cycle = 1; cycle <= 100; cycle++) {
            performSingleUserCycle(conn, report, cycle);
        }
    }

    private void performSingleUserCycle(Connection conn, StringBuilder report, int cycle) throws SQLException {
        insertUsersForCycle(conn, cycle);
        int updated = updateUsersForCycle(conn, cycle);
        int deleted = deleteUsersForCycle(conn, cycle);
        int count = countUsers(conn);
        if (cycle % 10 == 0) {
            report.append("[Cycle " + cycle + "] Remaining users: " + count + ", updated: " + updated + ", deleted: " + deleted + "\n");
        }
    }

    private void insertUsersForCycle(Connection conn, int cycle) throws SQLException {
        for (int i = 1; i <= 5; i++) {
            insertSingleUser(conn, i, cycle);
        }
    }

    private void insertSingleUser(Connection conn, int i, int cycle) throws SQLException {
        try (PreparedStatement psInsert = conn.prepareStatement("INSERT INTO users (name, age) VALUES (?, ?)");) {
            psInsert.setString(1, "User" + i + "_c" + cycle);
            psInsert.setInt(2, 20 + i + cycle);
            psInsert.executeUpdate();
        }
    }

    private int updateUsersForCycle(Connection conn, int cycle) throws SQLException {
        return updateUsers(conn, cycle);
    }

    private int deleteUsersForCycle(Connection conn, int cycle) throws SQLException {
        return deleteUsers(conn, cycle);
    }

    private int countUsers(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) AS cnt FROM users")) {
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        }
        return 0;
    }

    private void printFinalUsers(Connection conn, StringBuilder report) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM users")) {
            report.append("Final users:\n");
            int shown = 0;
            while (rs.next() && shown < 10) {
                report.append("  id=" + rs.getInt("id") + ", name=" + rs.getString("name") + ", age=" + rs.getInt("age") + "\n");
                shown++;
            }
        }
    }

    private void buildTreeStructure(StringBuilder report) {
        try {
            Class.forName("org.h2.Driver");
            try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")) {
                createTreeTable(conn);
                int totalNodes = 100;
                int childrenPerNode = 3;
                int insertedNodes = insertTreeNodes(conn, totalNodes, childrenPerNode);
                report.append("Inserted " + insertedNodes + " nodes into tree table [" + getTreeInsertTime(conn, totalNodes, childrenPerNode) + " ms]\n");
            }
        } catch (Exception e) {
            report.append("Tree DB error: " + e.getMessage() + "\n");
        }
    }

    private void createTreeTable(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS tree (id INT PRIMARY KEY, parent_id INT, tree_value INT)");
        }
    }

    private int insertTreeNodes(Connection conn, int totalNodes, int childrenPerNode) throws SQLException {
        int currentId = 1;
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO tree (id, parent_id, tree_value) VALUES (1, NULL, 1)");
            currentId++;
            insertAllTreeChildren(st, totalNodes, childrenPerNode, currentId);
            currentId += (totalNodes - 1);
        }
        return totalNodes;
    }

    private void insertAllTreeChildren(Statement st, int totalNodes, int childrenPerNode, int startId) throws SQLException {
        int currentId = startId;
        for (int parent = 1; parent <= totalNodes / childrenPerNode; parent++) {
            insertChildrenForParent(st, parent, childrenPerNode, totalNodes, currentId);
            currentId += childrenPerNode;
        }
    }

    private void insertChildrenForParent(Statement st, int parent, int childrenPerNode, int totalNodes, int startId) throws SQLException {
        int currentId = startId;
        for (int c = 0; c < childrenPerNode && currentId <= totalNodes; c++) {
            st.execute("INSERT INTO tree (id, parent_id, tree_value) VALUES (" + currentId + ", " + parent + ", " + currentId + ")");
            currentId++;
        }
    }

    private long getTreeInsertTime(Connection conn, int totalNodes, int childrenPerNode) {
        // This is a placeholder for timing logic if needed
        // For now, just return 0
        return 0;
    }

    private void executeRecursiveTreeQuery(StringBuilder report) {
        try {
            Class.forName("org.h2.Driver");
            try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")) {
                long recQueryStart = System.nanoTime();
                ResultSet rs = runRecursiveQuery(conn);
                if (rs.next()) {
                    int cnt = rs.getInt("cnt");
                    long sumv = rs.getLong("sumv");
                    report.append("Recursive query: root descendants = " + cnt + ", sum of values = " + sumv + "\n");
                }
                long recQueryElapsed = System.nanoTime() - recQueryStart;
                report.append("Recursive query time: " + (recQueryElapsed / 1_000_000) + " ms\n");
            }
        } catch (Exception e) {
            report.append("Recursive query error: " + e.getMessage() + "\n");
        }
    }

    private ResultSet runRecursiveQuery(Connection conn) throws SQLException {
        Statement st = conn.createStatement();
        return st.executeQuery(
                "WITH RECURSIVE descendants(id, parent_id, tree_value) AS (" +
                        "  SELECT id, parent_id, tree_value FROM tree WHERE id = 1 " +
                        "  UNION ALL " +
                        "  SELECT t.id, t.parent_id, t.tree_value FROM tree t JOIN descendants d ON t.parent_id = d.id " +
                        ") SELECT COUNT(*) AS cnt, SUM(tree_value) AS sumv FROM descendants"
        );
    }

    private long additionalSleep(long ns) {
        long sleepMs = ns / 1_000_000;
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException ignored) {
        }
        return sleepMs;
    }

    private int updateUsers(Connection conn, int cycle) throws SQLException {
        try (Statement st = conn.createStatement()) {
            return st.executeUpdate("UPDATE users SET age = age + 1 WHERE age < " + (30 + cycle));
        }
    }

    private int deleteUsers(Connection conn, int cycle) throws SQLException {
        try (Statement st = conn.createStatement()) {
            return st.executeUpdate("DELETE FROM users WHERE age > " + (40 + cycle));
        }
    }
}
