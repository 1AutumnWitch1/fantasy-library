package com.fantasylibrary.community;

import com.fantasylibrary.media.MediaStoreException;
import com.fantasylibrary.media.ObjectMediaStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class CommunityImages {
    private static final Logger LOG = LoggerFactory.getLogger(CommunityImages.class);
    private static final Pattern KEY = Pattern.compile("[0-9a-f-]{36}\\.(jpg|png)");
    private final Path root;
    private final ObjectMediaStore storage;

    public CommunityImages(@Value("${library.community.upload-dir}") String directory, ObjectMediaStore storage) {
        root = Path.of(directory).toAbsolutePath().normalize();
        this.storage = storage;
    }

    public List<String> save(List<MultipartFile> files) {
        List<String> result = new ArrayList<>();
        try {
            for (MultipartFile file : files) result.add(save(file, Area.FORUM, 0));
            return List.copyOf(result);
        } catch (RuntimeException failure) {
            remove(result);
            throw failure;
        }
    }

    public String saveAvatar(MultipartFile file) {
        if (file.getSize() > 2 * 1024 * 1024) throw CommunityProblem.invalid("头像图片最多 2 MB");
        return save(file, Area.AVATAR, 384);
    }

    public Resource load(String key) {
        return load(key, Area.FORUM);
    }

    public Resource loadAvatar(String key) {
        return load(key, Area.AVATAR);
    }

    private Resource load(String key, Area area) {
        if (!valid(key)) throw CommunityProblem.notFound();
        if (storage.enabled()) {
            try {
                return storage.resource(area.key(key));
            } catch (MediaStoreException failure) {
                if (failure.isMissing()) throw CommunityProblem.notFound();
                throw CommunityProblem.storage();
            }
        }
        var resource = new FileSystemResource(root.resolve(key));
        if (!resource.isReadable()) throw CommunityProblem.notFound();
        return resource;
    }

    public static boolean valid(String key) {
        return key != null && KEY.matcher(key).matches();
    }

    public void remove(List<String> keys) {
        for (String key : keys) remove(key, Area.FORUM);
    }

    public void removeAvatar(String key) {
        remove(key, Area.AVATAR);
    }

    private void remove(String key, Area area) {
        if (!valid(key)) return;
        try {
            if (storage.enabled()) storage.delete(area.key(key));
            else Files.deleteIfExists(root.resolve(key));
        } catch (IOException | MediaStoreException error) {
            LOG.warn("Could not remove an unreferenced community image ({})", error.getClass().getSimpleName());
        }
    }

    private String save(MultipartFile file, Area area, int maxEdge) {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
            throw CommunityProblem.invalid("每张图片须小于或等于 5 MB");
        }
        Decoded decoded;
        try {
            decoded = decode(file);
        } catch (LinkageError error) {
            LOG.error("Community image decoding requires a full Java Desktop runtime ({})", error.getClass().getSimpleName());
            throw CommunityProblem.imageRuntime();
        }
        String key = UUID.randomUUID() + "." + decoded.format();
        Path tmp = null;
        try {
            boolean remote = storage.enabled();
            if (remote) {
                tmp = Files.createTempFile("library-image-", ".pending");
            } else {
                Files.createDirectories(root);
                tmp = Files.createTempFile(root, "image-", ".pending");
            }
            var raster = maxEdge > 0 ? resize(decoded.image(), maxEdge) : decoded.image();
            if (!ImageIO.write(raster, decoded.format(), tmp.toFile())) throw CommunityProblem.storage();
            if (remote) {
                storage.put(area.key(key), tmp, decoded.format().equals("png") ? "image/png" : "image/jpeg");
            } else {
                Files.move(tmp, root.resolve(key), StandardCopyOption.ATOMIC_MOVE);
            }
            return key;
        } catch (IOException | MediaStoreException error) {
            throw CommunityProblem.storage();
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException error) {
                    LOG.warn("Could not remove a temporary community image");
                }
            }
        }
    }

    private static Decoded decode(MultipartFile file) {
        try (var bytes = file.getInputStream(); var input = ImageIO.createImageInputStream(bytes)) {
            if (input == null) throw badFormat();
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw badFormat();
            var reader = readers.next();
            try {
                String format = switch (reader.getFormatName().toLowerCase(Locale.ROOT)) {
                    case "jpeg" -> "jpg";
                    case "png" -> "png";
                    default -> throw badFormat();
                };
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 4096 || height > 4096 || (long) width * height > 12_000_000) {
                    throw CommunityProblem.invalid("图片边长最多 4096 像素，总像素最多 1200 万");
                }
                var image = reader.read(0);
                if (image == null) throw badFormat();
                if (format.equals("jpg") && image.getType() != BufferedImage.TYPE_INT_RGB) {
                    var rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    var graphics = rgb.createGraphics();
                    try { graphics.drawImage(image, 0, 0, null); } finally { graphics.dispose(); }
                    image = rgb;
                }
                return new Decoded(format, image);
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException | IndexOutOfBoundsException error) {
            throw badFormat();
        }
    }

    private static BufferedImage resize(BufferedImage image, int maxEdge) {
        int edge = Math.max(image.getWidth(), image.getHeight());
        if (edge <= maxEdge) return image;
        double scale = (double) maxEdge / edge;
        var result = new BufferedImage(Math.max(1, (int) Math.round(image.getWidth() * scale)),
                Math.max(1, (int) Math.round(image.getHeight() * scale)),
                image.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        var graphics = result.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(image, 0, 0, result.getWidth(), result.getHeight(), null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static CommunityProblem badFormat() {
        return CommunityProblem.invalid("请上传有效的 JPEG 或 PNG 图片");
    }

    private enum Area {
        FORUM("community/"), AVATAR("avatars/");

        private final String prefix;

        Area(String prefix) {
            this.prefix = prefix;
        }

        String key(String fileName) {
            return prefix + fileName;
        }
    }

    private record Decoded(String format, BufferedImage image) {}
}
