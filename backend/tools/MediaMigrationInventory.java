import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

class MediaMigrationInventory {
    private static final String IMAGE = "[a-zA-Z0-9_-]+\\.(?:webp|jpe?g|png|avif|gif)";
    private static final String VIDEO = "[a-zA-Z0-9_-]+\\.(?:mp4|webm)";
    private static final String UUID = "[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}";
    private final Map<String, Path> objects = new TreeMap<>();
    private final Path catalog;
    private final Path playback;
    private final Path community;
    private int external;

    private MediaMigrationInventory(Path backend, Properties settings) {
        Path media = backend.resolve("src/main/resources/static/api/media");
        this.catalog = media.resolve("catalog");
        this.playback = media.resolve("playback");
        this.community = backend.resolve(config(settings, "COMMUNITY_UPLOAD_DIR", "./uploads/community")).normalize();
    }

    public static void main(String[] args) {
        try {
            Path root = Path.of(args[0]).toRealPath();
            Path backend = root.resolve("backend");
            var settings = new Properties();
            try (var reader = Files.newBufferedReader(backend.resolve(".env"))) {
                settings.load(reader);
            }
            var inventory = new MediaMigrationInventory(backend, settings);
            inventory.scan("catalog", inventory.catalog, "(?:editions/)?" + IMAGE);
            inventory.scan("playback", inventory.playback, VIDEO);
            inventory.scan("site", root.resolve("frontend/src/assets"),
                    "(?:account/|reader/|shelf/|catalog/(?:editions/)?)" + IMAGE);
            inventory.references(settings);

            var output = new PrintStream(System.out, true, StandardCharsets.UTF_8);
            inventory.objects.forEach((key, file) ->
                    output.println(key + "\t" + (file == null ? "" : file.toString())));
            output.println("EXTERNAL\t" + inventory.external);
        } catch (SQLException error) {
            System.err.println("Could not verify Oracle media references (Oracle code " + error.getErrorCode() + ").");
            System.exit(1);
        } catch (Exception error) {
            System.err.println(error.getMessage());
            System.exit(1);
        }
    }

    private void references(Properties settings) throws SQLException, IOException {
        var jdbc = config(settings, "ORACLE_JDBC_URL", "");
        if (!jdbc.matches("jdbc:oracle:thin:@//(?:localhost|127\\.0\\.0\\.1):[0-9]+/[a-zA-Z0-9_.-]+")) {
            throw new IllegalStateException("This migration script expects the local Fantasy Library Oracle database.");
        }
        try (var db = DriverManager.getConnection(jdbc, config(settings, "ORACLE_USERNAME", ""),
                config(settings, "ORACLE_PASSWORD", ""))) {
            try (var query = db.createStatement()) {
                try (var rows = query.executeQuery("SELECT resource_url FROM fl_work_image")) {
                    while (rows.next()) source(rows.getString(1), "catalog", catalog,
                            "(?:editions/|uploads/)?" + IMAGE, "uploads/" + UUID + "\\.(?:webp|jpe?g|png|avif|gif)");
                }
                try (var rows = query.executeQuery("SELECT media_url FROM fl_episode_source")) {
                    while (rows.next()) source(rows.getString(1), "playback", playback, VIDEO, UUID + "\\.(?:mp4|webm)");
                }
                try (var rows = query.executeQuery("SELECT image_key FROM fl_forum_image")) {
                    while (rows.next()) uploaded("community", rows.getString(1));
                }
                try (var rows = query.executeQuery("SELECT avatar_key FROM fl_account WHERE avatar_key IS NOT NULL")) {
                    while (rows.next()) uploaded("avatars", rows.getString(1));
                }
            }
        }
    }

    private void scan(String prefix, Path folder, String pattern) throws IOException {
        try (var entries = Files.walk(folder)) {
            for (var file : entries.filter(Files::isRegularFile).toList()) {
                String name = folder.relativize(file).toString().replace('\\', '/');
                if (name.matches(pattern)) add(prefix + "/" + name, folder, name, false);
            }
        }
    }

    private void source(String url, String namespace, Path folder, String pattern, String uploads) throws IOException {
        if (url.matches("(?is)^https?://.+")) {
            external++;
            return;
        }
        String prefix = "/api/media/" + namespace + "/";
        if (!url.startsWith(prefix) || !url.substring(prefix.length()).matches(pattern)) {
            throw new IllegalStateException("Oracle contains an unrecognized " + namespace + " source path.");
        }
        String name = url.substring(prefix.length());
        add(namespace + "/" + name, folder, name, name.matches(uploads));
    }

    private void uploaded(String namespace, String name) throws IOException {
        if (!name.matches(UUID + "\\.(?:jpg|png)")) throw new IllegalStateException("Unrecognized upload key.");
        add(namespace + "/" + name, community, name, true);
    }

    private void add(String key, Path folder, String file, boolean allowRemote) throws IOException {
        var source = folder.resolve(file).normalize();
        if (!Files.isRegularFile(source)) {
            if (!allowRemote) throw new IllegalStateException("Missing local media: " + key);
            objects.putIfAbsent(key, null);
        } else {
            Path real = source.toRealPath();
            if (!real.startsWith(folder.toRealPath())) throw new IllegalStateException("Media path leaves its source directory.");
            objects.put(key, real);
        }
    }

    private static String config(Properties source, String name, String fallback) {
        return System.getenv().getOrDefault(name, source.getProperty(name, fallback));
    }
}
