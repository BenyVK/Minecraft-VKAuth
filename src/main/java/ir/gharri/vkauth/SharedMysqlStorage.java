package ir.gharri.vkauth;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * match-signalVK: accounts are kept in ONE table together with the RankVK data of the same player
 * (database "minecraft", table "players" for example). One row per player:
 * <pre>
 *   uuid | name | auth_name auth_hash auth_registered auth_last_login | rank_present rank_prefix rank_suffix
 * </pre>
 * This class only ever writes the auth_* columns (and the name when the row is new), so the RankVK part of the
 * row is never touched. RankVK creates the very same columns, so it does not matter which plugin starts first.
 */
public final class SharedMysqlStorage implements Storage {

    /** Same list as RankVK's SqlStorage.PLAYER_COLUMNS. */
    private static final String[][] COLUMNS = {
            {"name", "VARCHAR(32) NULL"},
            {"auth_name", "VARCHAR(32) NULL"},
            {"auth_hash", "VARCHAR(255) NULL"},
            {"auth_registered", "BIGINT NOT NULL DEFAULT 0"},
            {"auth_last_login", "BIGINT NOT NULL DEFAULT 0"},
            {"rank_present", "TINYINT NOT NULL DEFAULT 0"},
            {"rank_prefix", "VARCHAR(255) NULL"},
            {"rank_suffix", "VARCHAR(255) NULL"},
    };

    private final String url;
    private final String user;
    private final String pass;
    private final String table;

    public SharedMysqlStorage(String host, int port, String database, String user, String pass, String table) {
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&autoReconnect=true"
                + "&connectTimeout=5000";
        this.user = user;
        this.pass = pass;
        this.table = table;
    }

    @Override
    public String name() {
        return "mysql (shared: " + table + ")";
    }

    private Connection connect() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found on this server", e);
        }
        return DriverManager.getConnection(url, user, pass);
    }

    private static String likeEscape(String s) {
        return s.replace("\\", "\\\\").replace("_", "\\_").replace("%", "\\%");
    }

    private static boolean hasTable(Connection c, String t) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SHOW TABLES LIKE ?")) {
            ps.setString(1, likeEscape(t));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean hasColumn(Connection c, String t, String column) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SHOW COLUMNS FROM `" + t + "` LIKE ?")) {
            ps.setString(1, likeEscape(column));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            if (!hasTable(c, t)) return false;
            throw e;
        }
    }

    @Override
    public void init() throws Exception {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            if (hasTable(c, table) && !hasColumn(c, table, "uuid")) {
                throw new SQLException("The table '" + table + "' already exists but has no 'uuid' column, so it "
                        + "cannot be shared. Set match-table to another name in configvk.yml.");
            }
            st.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table + "` (uuid VARCHAR(36) NOT NULL PRIMARY KEY) "
                    + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            for (String[] col : COLUMNS) {
                if (!hasColumn(c, table, col[0])) {
                    st.executeUpdate("ALTER TABLE `" + table + "` ADD COLUMN `" + col[0] + "` " + col[1]);
                }
            }
        }
    }

    @Override
    public Account getAccount(UUID uuid) throws Exception {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("SELECT auth_hash, auth_registered, auth_last_login, "
                     + "auth_name, name FROM `" + table + "` WHERE uuid = ? AND auth_hash IS NOT NULL")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return read(rs);
            }
        }
    }

    private static Account read(ResultSet rs) throws SQLException {
        String registeredAs = rs.getString("auth_name");
        if (registeredAs == null) registeredAs = rs.getString("name");
        return new Account(rs.getString("auth_hash"), rs.getLong("auth_registered"), rs.getLong("auth_last_login"),
                registeredAs);
    }

    @Override
    public void saveAccount(UUID uuid, String name, Account account) throws Exception {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("INSERT INTO `" + table + "` "
                     + "(uuid, name, auth_name, auth_hash, auth_registered, auth_last_login) VALUES (?,?,?,?,?,?) "
                     + "ON DUPLICATE KEY UPDATE name = COALESCE(name, VALUES(name)), auth_name = VALUES(auth_name), "
                     + "auth_hash = VALUES(auth_hash), "
                     + "auth_registered = IF(auth_registered > 0, auth_registered, VALUES(auth_registered)), "
                     + "auth_last_login = VALUES(auth_last_login)")) {
            String shown = name == null ? "unknown" : name;
            ps.setString(1, uuid.toString());
            ps.setString(2, shown);
            ps.setString(3, account.name() != null ? account.name() : shown);
            ps.setString(4, account.hash());
            ps.setLong(5, account.registered() > 0 ? account.registered() : System.currentTimeMillis());
            ps.setLong(6, account.lastLogin());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateLastLogin(UUID uuid, long time) throws Exception {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE `" + table + "` SET auth_last_login = ? WHERE uuid = ? AND auth_hash IS NOT NULL")) {
            ps.setLong(1, time);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    /** Removes the login data only. The row stays while RankVK still has data in it. */
    @Override
    public void deleteAccount(UUID uuid) throws Exception {
        try (Connection c = connect()) {
            try (PreparedStatement ps = c.prepareStatement("UPDATE `" + table + "` SET auth_name = NULL, "
                    + "auth_hash = NULL, auth_registered = 0, auth_last_login = 0 WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM `" + table + "` WHERE uuid = ? AND rank_present = 0 AND auth_hash IS NULL")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
        }
    }

    @Override
    public List<StoredAccount> getAll() throws Exception {
        List<StoredAccount> out = new ArrayList<>();
        try (Connection c = connect()) {
            if (!hasTable(c, table)) return out;
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT uuid, name, auth_name, auth_hash, auth_registered, "
                         + "auth_last_login FROM `" + table + "` WHERE auth_hash IS NOT NULL")) {
                while (rs.next()) {
                    UUID id;
                    try {
                        id = UUID.fromString(rs.getString("uuid"));
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    Account account = read(rs);
                    out.add(new StoredAccount(id, account.name(), account));
                }
            }
        }
        return out;
    }

    /** Clears the login data of every row (the table itself stays, RankVK uses it too). */
    @Override
    public void deleteAll() throws Exception {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.executeUpdate("UPDATE `" + table + "` SET auth_name = NULL, auth_hash = NULL, "
                    + "auth_registered = 0, auth_last_login = 0");
            st.executeUpdate("DELETE FROM `" + table + "` WHERE rank_present = 0 AND auth_hash IS NULL");
        }
    }

    @Override
    public void close() {
    }
}
