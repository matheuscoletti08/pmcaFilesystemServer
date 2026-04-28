package info.schnatterer.pmcaFilesystemServer;

import com.github.ma1co.openmemories.framework.DeviceInfo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import fi.iki.elonen.SimpleWebServer;

public class HttpServer extends SimpleWebServer {
    static final int PORT = 8080;
    static final String HOST = null;
    static final String WWW_ROOT = "/";
    static final boolean QUIET = false;

    private static final String CSS = 
        "body { font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, sans-serif; margin: 0; padding: 0; background: #f8fafc; color: #1e293b; }\n" +
        "header { background: #0f172a; color: white; padding: 1rem; text-align: center; position: sticky; top: 0; z-index: 100; }\n" +
        "header h1 { margin: 0; font-size: 1.1rem; }\n" +
        ".container { padding: 0.75rem; max-width: 800px; margin: 0 auto; }\n" +
        ".breadcrumbs { margin-bottom: 1rem; font-size: 0.85rem; padding: 0.5rem; background: white; border-radius: 8px; border: 1px solid #e2e8f0; white-space: nowrap; overflow-x: auto; }\n" +
        ".breadcrumbs a { color: #3b82f6; text-decoration: none; }\n" +
        ".card { background: white; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); margin-bottom: 1rem; border: 1px solid #e2e8f0; }\n" +
        ".card-header { padding: 0.75rem 1rem; font-weight: 700; border-bottom: 1px solid #f1f5f9; background: #f8fafc; font-size: 0.9rem; }\n" +
        ".file-list { list-style: none; padding: 0; margin: 0; }\n" +
        ".file-item { display: flex; align-items: center; padding: 0.75rem 1rem; border-bottom: 1px solid #f1f5f9; text-decoration: none; color: inherit; }\n" +
        ".file-item:last-child { border-bottom: none; }\n" +
        ".file-item:active { background: #f1f5f9; }\n" +
        ".file-info { flex: 1; min-width: 0; }\n" +
        ".file-name { font-weight: 500; font-size: 0.95rem; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }\n" +
        ".file-meta { font-size: 0.75rem; color: #64748b; }\n" +
        ".file-icon { font-size: 1.5rem; margin-right: 1rem; flex-shrink: 0; display: flex; align-items: center; justify-content: center; width: 40px; }\n" +
        ".thumb-img { width: 40px; height: 40px; border-radius: 4px; object-fit: cover; margin-right: 1rem; background: #eee; flex-shrink: 0; }\n" +
        ".btn-action { padding: 0.5rem 0.75rem; background: #3b82f6; color: white; border-radius: 6px; font-size: 0.75rem; font-weight: 600; text-decoration: none; margin-left: 0.5rem; flex-shrink: 0; }\n" +
        ".preview-img { width: 100%; height: auto; display: block; border-radius: 8px; margin-bottom: 0.5rem; background: #eee; }\n" +
        ".preview-container { padding: 1rem; text-align: center; }\n" +
        "footer { text-align: center; padding: 2rem; color: #94a3b8; font-size: 0.75rem; }";

    public HttpServer() {
        super(HOST, PORT, new File(WWW_ROOT).getAbsoluteFile(), QUIET);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        File f = new File(uri);

        if (session.getParameters().containsKey("thumb")) {
            return serveThumbnail(f);
        }

        if (session.getParameters().containsKey("preview") && isImage(f.getName())) {
            return servePreview(uri, f);
        }

        if (f.isDirectory()) {
            return serveDirectory(uri, f);
        } else {
            return super.serve(session);
        }
    }

    private Response serveThumbnail(File f) {
        try {
            byte[] thumb = extractThumbnail(f);
            if (thumb != null) {
                return newFixedLengthResponse(Response.Status.OK, "image/jpeg", new ByteArrayInputStream(thumb), thumb.length);
            }
        } catch (IOException e) {
            // Ignorar erro e retornar 404
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Thumbnail not found");
    }

    private byte[] extractThumbnail(File f) throws IOException {
        if (!f.exists() || !f.isFile()) return null;
        RandomAccessFile raf = new RandomAccessFile(f, "r");
        try {
            // Miniaturas EXIF e previews costumam estar nos primeiros 256KB
            byte[] buffer = new byte[256 * 1024];
            int bytesRead = raf.read(buffer);
            if (bytesRead < 4) return null;

            for (int i = 0; i < bytesRead - 3; i++) {
                // Busca marcador SOI (Start of Image) de um JPEG: FF D8 FF
                if ((buffer[i] & 0xFF) == 0xFF && (buffer[i+1] & 0xFF) == 0xD8 && (buffer[i+2] & 0xFF) == 0xFF) {
                    int start = i;
                    // Busca marcador EOI (End of Image): FF D9
                    for (int j = i + 2; j < bytesRead - 1; j++) {
                        if ((buffer[j] & 0xFF) == 0xFF && (buffer[j+1] & 0xFF) == 0xD9) {
                            int end = j + 2;
                            byte[] thumb = new byte[end - start];
                            System.arraycopy(buffer, start, thumb, 0, thumb.length);
                            return thumb;
                        }
                    }
                    // Se não achar o fim no buffer, retorna o que tem (browsers geralmente lidam com isso)
                    int end = bytesRead;
                    byte[] thumb = new byte[end - start];
                    System.arraycopy(buffer, start, thumb, 0, thumb.length);
                    return thumb;
                }
            }
        } finally {
            raf.close();
        }
        return null;
    }

    private boolean isImage(String name) {
        String n = name.toLowerCase();
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".arw");
    }

    private Response servePreview(String uri, File f) {
        StringBuilder html = new StringBuilder();
        String title = "Preview: " + f.getName();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("<style>").append(CSS).append("</style></head><body>");
        html.append("<header><h1>").append(title).append("</h1></header>");
        html.append("<div class=\"container\">");
        html.append("<div class=\"breadcrumbs\"><a href=\"").append(f.getParent()).append("\">🔙 Voltar para pasta</a></div>");
        html.append("<div class=\"card preview-container\">");
        
        // Para ARW, usa o endpoint de thumbnail para o preview
        String imgSrc = f.getName().toLowerCase().endsWith(".arw") ? uri + "?thumb=1" : uri;
        html.append("<img src=\"").append(imgSrc).append("\" class=\"preview-img\">");
        
        html.append("<div class=\"file-meta\">").append(f.getName()).append(" (").append(formatSize(f.length())).append(")</div><br>");
        html.append("<a href=\"").append(uri).append("\" download class=\"btn-action\" style=\"font-size: 1rem; padding: 0.75rem 1.5rem;\">Download original .ARW</a>");
        html.append("</div></div></body></html>");
        return newFixedLengthResponse(Response.Status.OK, MIME_HTML, html.toString());
    }

    private Response serveDirectory(String uri, File directory) {
        StringBuilder html = new StringBuilder();
        String title = getDeviceInfo().getModel();
        
        html.append("<!DOCTYPE html><html lang=\"pt-BR\"><head>");
        html.append("<meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">");
        html.append("<title>").append(title).append("</title>");
        html.append("<style>").append(CSS).append("</style>");
        html.append("</head><body>");

        html.append("<header><h1>").append(getDeviceInfo().getBrand()).append(" ").append(title).append("</h1></header>");
        html.append("<div class=\"container\">");

        // Breadcrumbs
        html.append("<div class=\"breadcrumbs\">");
        html.append("<a href=\"/\">Início</a>");
        String[] parts = uri.split("/");
        StringBuilder currentPath = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            currentPath.append("/").append(part);
            html.append(" / <a href=\"").append(currentPath.toString()).append("\">").append(part).append("</a>");
        }
        html.append("</div>");

        if (uri.equals("/") || uri.isEmpty()) {
            renderSummary(html);
        }

        renderFileList(uri, directory, html);

        html.append("</div>");
        html.append("<footer>pmcaFilesystemServer &bull; ").append(title).append("</footer>");
        html.append("</body></html>");

        return newFixedLengthResponse(Response.Status.OK, MIME_HTML, html.toString());
    }

    private void renderSummary(StringBuilder html) {
        html.append("<div class=\"card\">");
        html.append("<div class=\"card-header\">Mídias Encontradas</div>");
        html.append("<div class=\"file-list\">");
        
        // Caminhos específicos solicitados para câmeras Sony
        String videoPath = "/sdcard/PRIVATE/M4ROOT/CLIP";
        String photosPath = "/sdcard/DCIM";

        // Soma JPEGs e RAWs para a categoria "Photos"
        int photoCount = FilesystemScanner.getJpegsOnExternalStorage().size() + 
                         FilesystemScanner.getRawsOnExternalStorage().size();

        renderSummaryItem("Vídeos", FilesystemScanner.getVideosOnExternalStorage().size(), "🎬", videoPath, html);
        renderSummaryItem("Photos", photoCount, "📸", photosPath, html);
        
        html.append("<a href=\"").append(Logger.getFile().getAbsolutePath()).append("\" class=\"file-item\">");
        html.append("<span class=\"file-icon\">📝</span><div class=\"file-info\"><span class=\"file-name\">System Log</span></div></a>");
        
        html.append("</div></div>");
    }

    private void renderSummaryItem(String label, int count, String icon, String path, StringBuilder html) {
        html.append("<a href=\"").append(path).append("\" class=\"file-item\">");
        html.append("<span class=\"file-icon\">").append(icon).append("</span>");
        html.append("<div class=\"file-info\"><span class=\"file-name\">").append(label).append("</span>");
        html.append("<span class=\"file-meta\">").append(count).append(" arquivos encontrados</span></div>");
        html.append("<span style=\"color:#3b82f6; font-weight:bold;\">Abrir &rarr;</span></a>");
    }

    private void renderFileList(String uri, File directory, StringBuilder html) {
        html.append("<div class=\"card\">");
        html.append("<div class=\"card-header\">Arquivos em ").append(uri).append("</div>");
        html.append("<div class=\"file-list\">");

        if (!uri.equals("/") && !uri.isEmpty()) {
            File parent = directory.getParentFile();
            String parentLink = parent != null ? parent.getAbsolutePath() : "/";
            html.append("<a href=\"").append(parentLink).append("\" class=\"file-item\">");
            html.append("<span class=\"file-icon\">⬅️</span><div class=\"file-info\"><span class=\"file-name\">.. (Voltar)</span></div></a>");
        }

        File[] files = directory.listFiles();
        if (files != null) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    if (a.isDirectory() && !b.isDirectory()) return -1;
                    if (!a.isDirectory() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            });

            for (File file : files) {
                if (file.getName().startsWith(".")) continue;

                String icon = file.isDirectory() ? "📁" : getIconForFile(file.getName());
                String link = file.getAbsolutePath();
                boolean isImg = isImage(file.getName());
                
                html.append("<div class=\"file-item\">");
                
                if (isImg && !file.isDirectory()) {
                    html.append("<img class=\"thumb-img\" src=\"").append(link).append("?thumb=1\">");
                } else {
                    html.append("<span class=\"file-icon\">").append(icon).append("</span>");
                }

                html.append("<div class=\"file-info\">");
                html.append("<a class=\"file-name\" href=\"").append(isImg ? link + "?preview=1" : link).append("\">").append(file.getName()).append("</a>");
                html.append("<span class=\"file-meta\">").append(file.isDirectory() ? "Pasta" : formatSize(file.length())).append("</span>");
                html.append("</div>");
                
                if (!file.isDirectory()) {
                    html.append("<a href=\"").append(link).append("\" download class=\"btn-action\">Baixar</a>");
                }
                html.append("</div>");
            }
        }
        
        html.append("</div></div>");
    }

    private String getIconForType(String type) {
        if (type.contains("Video")) return "🎬";
        if (type.contains("JPEG")) return "🖼️";
        if (type.contains("RAW")) return "📸";
        return "📁";
    }

    private String getIconForFile(String filename) {
        filename = filename.toLowerCase();
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "🖼️";
        if (filename.endsWith(".mp4") || filename.endsWith(".mts")) return "🎬";
        if (filename.endsWith(".arw")) return "📸";
        if (filename.endsWith(".txt") || filename.endsWith(".log")) return "📝";
        return "📄";
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }

    private DeviceInfo getDeviceInfo() {
        return DeviceInfo.getInstance();
    }
}
