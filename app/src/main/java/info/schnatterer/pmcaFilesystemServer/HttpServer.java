package info.schnatterer.pmcaFilesystemServer;

import com.github.ma1co.openmemories.framework.DeviceInfo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import fi.iki.elonen.SimpleWebServer;

public class HttpServer extends SimpleWebServer {
    static final int PORT = 8080;
    static final String HOST = null;
    static final String WWW_ROOT = "/";
    static final boolean QUIET = false;

    private static final String CSS = 
        ":root { " +
        "  --bg: #f8fafc; --surface: #ffffff; --primary: #4f46e5; --primary-hover: #4338ca; " +
        "  --text-main: #0f172a; --text-muted: #64748b; --border: #e2e8f0; --accent: #10b981; " +
        "  --shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1); " +
        "  --radius: 12px; " +
        "} " +
        "* { box-sizing: border-box; -webkit-tap-highlight-color: transparent; } " +
        "body { font-family: -apple-system, system-ui, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; " +
        "       margin: 0; background: var(--bg); color: var(--text-main); line-height: 1.5; overflow-x: hidden; } " +
        
        "header { background: var(--surface); border-bottom: 1px solid var(--border); padding: 0.75rem 1rem; " +
        "         position: sticky; top: 0; z-index: 100; display: flex; align-items: center; justify-content: space-between; " +
        "         box-shadow: 0 1px 2px 0 rgb(0 0 0 / 0.05); } " +
        "header h1 { font-size: 1.1rem; font-weight: 700; margin: 0; color: var(--primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } " +
        
        ".btn-menu { background: none; border: none; font-size: 1.5rem; cursor: pointer; padding: 0.5rem; display: flex; align-items: center; } " +
        
        ".drawer { position: fixed; top: 0; left: -280px; width: 280px; height: 100%; background: var(--surface); " +
        "          z-index: 200; box-shadow: 4px 0 10px rgba(0,0,0,0.1); transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1); " +
        "          padding: 1.5rem; display: flex; flex-direction: column; } " +
        ".drawer.open { transform: translateX(280px); } " +
        ".drawer-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 150; display: none; backdrop-filter: blur(2px); } " +
        ".drawer-overlay.active { display: block; } " +
        
        ".nav-group { margin-bottom: 2rem; } " +
        ".nav-title { font-size: 0.75rem; font-weight: 700; text-transform: uppercase; color: var(--text-muted); margin-bottom: 0.75rem; letter-spacing: 0.05em; } " +
        ".nav-link { display: flex; align-items: center; padding: 0.75rem 1rem; text-decoration: none; color: var(--text-main); " +
        "            border-radius: var(--radius); margin-bottom: 0.25rem; font-weight: 500; transition: background 0.2s; } " +
        ".nav-link:active, .nav-link.active { background: var(--bg); color: var(--primary); } " +
        ".nav-link i { margin-right: 0.75rem; font-style: normal; } " +
        
        ".container { padding: 1rem; max-width: 1400px; margin: 0 auto; } " +
        
        ".breadcrumbs { display: flex; align-items: center; gap: 0.5rem; font-size: 0.875rem; color: var(--text-muted); margin-bottom: 1.5rem; " +
        "                overflow-x: auto; white-space: nowrap; padding-bottom: 0.5rem; -webkit-overflow-scrolling: touch; } " +
        ".breadcrumbs a { color: var(--text-muted); text-decoration: none; } " +
        ".breadcrumbs span { color: var(--text-main); font-weight: 600; } " +
        
        ".grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem; } " +
        "@media (min-width: 480px) { .grid { grid-template-columns: repeat(3, 1fr); } } " +
        "@media (min-width: 768px) { .grid { grid-template-columns: repeat(4, 1fr); } } " +
        "@media (min-width: 1024px) { .grid { grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); } } " +
        
        ".file-card { background: var(--surface); border-radius: var(--radius); overflow: hidden; " +
        "             border: 1px solid var(--border); box-shadow: var(--shadow); position: relative; " +
        "             display: flex; flex-direction: column; transition: transform 0.15s, box-shadow 0.15s; } " +
        ".file-card:active { transform: scale(0.98); } " +
        
        ".thumb-box { width: 100%; aspect-ratio: 4/3; background: #f1f5f9; position: relative; overflow: hidden; " +
        "             display: flex; align-items: center; justify-content: center; text-decoration: none; } " +
        ".thumb-box img { width: 100%; height: 100%; object-fit: cover; } " +
        ".file-icon { font-size: 2.5rem; opacity: 0.4; } " +
        
        ".card-info { padding: 0.75rem; flex-grow: 1; display: flex; flex-direction: column; } " +
        ".file-name { font-size: 0.875rem; font-weight: 600; margin: 0 0 0.25rem; overflow: hidden; " +
        "             text-overflow: ellipsis; white-space: nowrap; color: var(--text-main); } " +
        ".file-meta { font-size: 0.75rem; color: var(--text-muted); margin-bottom: 0.75rem; } " +
        
        ".actions { display: flex; gap: 0.5rem; margin-top: auto; } " +
        ".btn { flex: 1; padding: 0.6rem; border-radius: 8px; border: 1px solid var(--border); " +
        "       background: var(--surface); color: var(--text-main); font-size: 0.75rem; font-weight: 600; " +
        "       text-align: center; text-decoration: none; display: flex; align-items: center; justify-content: center; gap: 0.4rem; } " +
        ".btn-primary { background: var(--primary); color: white; border-color: var(--primary); } " +
        
        ".fab-zip { position: fixed; bottom: 1.5rem; right: 1.5rem; width: 56px; height: 56px; " +
        "           background: var(--accent); color: white; border-radius: 50%; display: flex; " +
        "           align-items: center; justify-content: center; box-shadow: 0 4px 12px rgba(0,0,0,0.25); " +
        "           text-decoration: none; font-size: 1.5rem; z-index: 90; } " +
        
        ".preview-overlay { padding: 1rem; text-align: center; background: var(--bg); min-height: 100vh; color: var(--text-main); } " +
        ".preview-img { max-width: 100%; height: auto; border-radius: 8px; margin-bottom: 2rem; box-shadow: var(--shadow); } " +
        
        "footer { padding: 3rem 1rem; text-align: center; color: var(--text-muted); font-size: 0.75rem; } ";

    private static final String JS = 
        "function toggleMenu() { " +
        "  document.getElementById('drawer').classList.toggle('open'); " +
        "  document.getElementById('overlay').classList.toggle('active'); " +
        "} " +
        "document.addEventListener('DOMContentLoaded', function() { " +
        "  document.getElementById('overlay').onclick = toggleMenu; " +
        "}); ";

    public HttpServer() {
        super(HOST, PORT, new File(WWW_ROOT).getAbsoluteFile(), QUIET);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Logger.info("HTTP Request: " + uri);
        File f = new File(uri);

        if (session.getParameters().containsKey("zip") && f.isDirectory()) {
            return serveZip(f);
        }

        if (session.getParameters().containsKey("thumb")) {
            String name = f.getName().toLowerCase();
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                return super.serve(session);
            }
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
            if (thumb != null && thumb.length > 0) {
                Response res = newFixedLengthResponse(Response.Status.OK, "image/jpeg", new ByteArrayInputStream(thumb), (long) thumb.length);
                res.addHeader("Cache-Control", "public, max-age=3600");
                return res;
            }
        } catch (Exception e) {
            Logger.error("Error serving thumb: " + e.getMessage());
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "No thumb");
    }

    private byte[] extractThumbnail(File f) throws IOException {
        if (!f.exists() || !f.isFile()) return null;
        RandomAccessFile raf = new RandomAccessFile(f, "r");
        try {
            byte[] buffer = new byte[1024 * 512];
            int bytesRead = raf.read(buffer);
            if (bytesRead < 4) return null;
            for (int i = 0; i < bytesRead - 3; i++) {
                if ((buffer[i] & 0xFF) == 0xFF && (buffer[i+1] & 0xFF) == 0xD8) {
                    int start = i;
                    for (int j = i + 2; j < bytesRead - 1; j++) {
                        if ((buffer[j] & 0xFF) == 0xFF && (buffer[j+1] & 0xFF) == 0xD9) {
                            byte[] thumb = new byte[j + 2 - start];
                            System.arraycopy(buffer, start, thumb, 0, thumb.length);
                            return thumb;
                        }
                    }
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
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=5.0\">");
        html.append("<style>").append(CSS).append("</style></head><body>");
        html.append("<div class=\"preview-overlay\">");
        html.append("<div style=\"text-align:left; margin-bottom:1rem;\"><a href=\"").append(f.getParent()).append("\" style=\"color:var(--primary); text-decoration:none; font-weight:700;\">✕ Voltar</a></div>");
        String imgSrc = f.getName().toLowerCase().endsWith(".arw") ? uri + "?thumb=1" : uri;
        html.append("<img src=\"").append(imgSrc).append("\" class=\"preview-img\">");
        html.append("<div class=\"file-name\">").append(f.getName()).append("</div>");
        html.append("<div class=\"file-meta\">").append(formatSize(f.length())).append("</div>");
        html.append("<div style=\"margin-top:2rem;\"><a href=\"").append(uri).append("\" download class=\"btn btn-primary\" style=\"padding:1rem\">Download Original</a></div>");
        html.append("</div></body></html>");
        return newFixedLengthResponse(Response.Status.OK, MIME_HTML, html.toString());
    }

    private Response serveDirectory(String uri, File directory) {
        StringBuilder html = new StringBuilder();
        DeviceInfo dev = DeviceInfo.getInstance();
        html.append("<!DOCTYPE html><html lang=\"pt-BR\"><head>");
        html.append("<meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\">");
        html.append("<title>Explorer - ").append(dev.getModel()).append("</title>");
        html.append("<style>").append(CSS).append("</style>");
        html.append("<script>").append(JS).append("</script>");
        html.append("</head><body>");

        // Header
        html.append("<header>");
        html.append("<button class=\"btn-menu\" onclick=\"toggleMenu()\">☰</button>");
        html.append("<h1>").append(dev.getModel()).append("</h1>");
        html.append("<div style=\"width:40px\"></div>"); // Spacer
        html.append("</header>");

        // Drawer
        html.append("<div class=\"drawer-overlay\" id=\"overlay\"></div>");
        html.append("<div class=\"drawer\" id=\"drawer\">");
        html.append("<div class=\"nav-group\"><div class=\"nav-title\">Navegação</div>");
        html.append("<a href=\"/\" class=\"nav-link\"><i>🏠</i> Início</a>");
        html.append("<a href=\"/sdcard/DCIM\" class=\"nav-link\"><i>📸</i> Galeria DCIM</a>");
        html.append("<a href=\"/sdcard/PRIVATE/M4ROOT/CLIP\" class=\"nav-link\"><i>🎬</i> Vídeos MP4</a>");
        html.append("</div>");
        html.append("<div style=\"margin-top:auto; font-size:0.7rem; color:var(--text-muted)\">PMCA Server v0.4.0</div>");
        html.append("</div>");

        html.append("<div class=\"container\">");
        
        // Breadcrumbs
        html.append("<div class=\"breadcrumbs\">");
        String[] parts = uri.split("/");
        StringBuilder currentPath = new StringBuilder();
        html.append("<a href=\"/\">root</a>");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            currentPath.append("/").append(part);
            html.append("<span> / </span><a href=\"").append(currentPath.toString()).append("\">").append(part).append("</a>");
        }
        html.append("</div>");

        // Grid
        html.append("<div class=\"grid\">");
        renderFileList(uri, directory, html);
        html.append("</div>");

        html.append("</div>"); // container

        if (!uri.equals("/") && !uri.isEmpty()) {
            html.append("<a href=\"").append(uri).append("?zip=1\" class=\"fab-zip\" title=\"Download ZIP\">📦</a>");
        }

        html.append("<footer>").append(dev.getBrand()).append(" &bull; ").append(dev.getModel()).append("<br>pmcaFilesystemServer Modern UI</footer>");
        html.append("</body></html>");

        return newFixedLengthResponse(Response.Status.OK, MIME_HTML, html.toString());
    }

    private void renderFileList(String uri, File directory, StringBuilder html) {
        File[] files = directory.listFiles();
        if (files == null) return;

        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return b.getName().compareToIgnoreCase(a.getName());
            }
        });

        for (File file : files) {
            if (file.getName().startsWith(".")) continue;
            String path = file.getAbsolutePath();
            boolean isImg = isImage(file.getName());
            
            html.append("<div class=\"file-card\">");
            html.append("<a href=\"").append(file.isDirectory() ? path : (isImg ? path + "?preview=1" : path)).append("\" class=\"thumb-box\">");
            if (isImg && !file.isDirectory()) {
                html.append("<img src=\"").append(path).append("?thumb=1\" loading=\"lazy\">");
            } else {
                html.append("<span class=\"file-icon\">").append(file.isDirectory() ? "📁" : getIcon(file.getName())).append("</span>");
            }
            html.append("</a>");
            html.append("<div class=\"card-info\">");
            html.append("<div class=\"file-name\">").append(file.getName()).append("</div>");
            html.append("<div class=\"file-meta\">").append(file.isDirectory() ? "Pasta" : formatSize(file.length())).append("</div>");
            if (!file.isDirectory()) {
                html.append("<div class=\"actions\">");
                html.append("<a href=\"").append(path).append("\" download class=\"btn btn-primary\">Baixar</a>");
                if (isImg) html.append("<a href=\"").append(path).append("?preview=1\" class=\"btn\">Ver</a>");
                html.append("</div>");
            }
            html.append("</div></div>");
        }
    }

    private Response serveZip(final File directory) {
        try {
            final PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ZipOutputStream zos = null;
                    try {
                        zos = new ZipOutputStream(pos);
                        zipFolder(directory, directory, zos);
                    } catch (IOException e) {
                    } finally {
                        try { if (zos != null) zos.close(); pos.close(); } catch (IOException e) {}
                    }
                }
            }).start();
            Response res = newChunkedResponse(Response.Status.OK, "application/zip", pis);
            res.addHeader("Content-Disposition", "attachment; filename=\"" + directory.getName() + ".zip\"");
            return res;
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Zip error");
        }
    }

    private void zipFolder(File root, File folder, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) return;
        byte[] buf = new byte[64 * 1024];
        for (File f : files) {
            if (f.isDirectory()) zipFolder(root, f, zos);
            else {
                zos.putNextEntry(new ZipEntry(f.getAbsolutePath().substring(root.getAbsolutePath().length() + 1)));
                FileInputStream fis = new FileInputStream(f);
                int len;
                while ((len = fis.read(buf)) > 0) zos.write(buf, 0, len);
                fis.close();
                zos.closeEntry();
            }
        }
    }

    private String getIcon(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".mp4") || name.endsWith(".mts")) return "VIDEO";
        return "FILE";
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        return String.format(Locale.US, "%.1f %cB", bytes / Math.pow(1024, exp), "KMGTPE".charAt(exp - 1));
    }
}
