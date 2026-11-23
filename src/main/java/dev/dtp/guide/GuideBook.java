package dev.dtp.guide;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Das Handbuch einer Sprache, gelesen aus {@code assets/dtp/guide/<sprache>.md}. Reine Daten ohne Client-Klassen,
 * damit die GameTests die Dateien am Server prüfen können.
 *
 * <p>Format (kleine, feste Teilmenge von Markdown, siehe docs/71-entwicklung-und-test.md):
 * <ul>
 * <li>{@code # Titel} — neues Kapitel</li>
 * <li>{@code ## Titel {#id grid=name warning}} — neue Seite; {@code #id} ist Pflicht und in allen Sprachen gleich</li>
 * <li>Leerzeile trennt Absätze; einfache Zeilenumbrüche werden zu Leerzeichen</li>
 * <li>{@code - Punkt} — Aufzählung; {@code **fett**}, {@code *kursiv*} formatiert erst der Client</li>
 * <li>{@code <!-- … -->} — Kommentar, auch über mehrere Zeilen</li>
 * </ul>
 */
public record GuideBook(List<Chapter> chapters, List<Problem> problems) {
    public record Chapter(String title, List<Page> pages) {
    }

    /**
     * Eine Seite. {@code body} sind Zeilen: Absätze stehen je in einer Zeile, Aufzählungspunkte beginnen mit „• “,
     * eine leere Zeile trennt Blöcke. Inline-Formatierung ({@code **}, {@code *}) ist noch enthalten.
     * {@code grid} ist der Rastername oder {@code null}.
     */
    public record Page(String id, String title, List<String> body, String grid, boolean warning) {
        public Page {
            body = List.copyOf(body);
        }
    }

    /** Ein Fehler in der Datei, mit Zeilennummer (ab 1). */
    public record Problem(int line, String message) {
        @Override
        public String toString() {
            return "line " + line + ": " + message;
        }
    }

    public static final String BULLET = "• ";

    private static final Pattern PAGE_ATTRIBUTES = Pattern.compile("^(.*?)\\s*\\{([^}]*)}\\s*$");

    public GuideBook {
        chapters = List.copyOf(chapters);
        problems = List.copyOf(problems);
    }

    /** Alle Seiten in Lesereihenfolge. */
    public List<Page> pages() {
        return chapters.stream().flatMap(c -> c.pages().stream()).toList();
    }

    /** Ein Buch, das nur aus einer Fehlerseite besteht. */
    public static GuideBook error(String title, String message) {
        return new GuideBook(List.of(new Chapter(title, List.of(new Page("error", title, List.of(message), null, true)))), List.of());
    }

    public static GuideBook parse(String markdown) {
        return new Parser(stripComments(markdown)).parse();
    }

    /** Ersetzt Kommentare durch Leerzeichen und behält ihre Zeilenumbrüche, damit die Zeilennummern stimmen. */
    static String stripComments(String text) {
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            int start = text.indexOf("<!--", i);
            if (start < 0) {
                out.append(text, i, text.length());
                break;
            }
            out.append(text, i, start);
            int end = text.indexOf("-->", start + 4);
            int stop = end < 0 ? text.length() : end + 3;
            for (int k = start; k < stop; k++) {
                if (text.charAt(k) == '\n') {
                    out.append('\n');
                }
            }
            i = stop;
        }
        return out.toString();
    }

    private static final class Parser {
        private final String[] lines;
        private final List<Chapter> chapters = new ArrayList<>();
        private final List<Problem> problems = new ArrayList<>();
        private final Set<String> ids = new HashSet<>();

        private String chapterTitle;
        private int chapterLine;
        private List<Page> chapterPages;

        private String pageId;
        private String pageTitle;
        private String pageGrid;
        private boolean pageWarning;
        private int pageLine;
        private List<String> body;
        /** Zeile, an die eine Fortsetzungszeile angehängt wird, oder {@code -1} nach einer Leerzeile. */
        private int open = -1;

        Parser(String text) {
            this.lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        }

        GuideBook parse() {
            for (int n = 0; n < lines.length; n++) {
                line(n + 1, lines[n].strip());
            }
            closeChapter();
            if (chapters.isEmpty()) {
                problems.add(new Problem(1, "the handbook has no chapters"));
            }
            return new GuideBook(chapters, problems);
        }

        private void line(int number, String line) {
            if (line.startsWith("## ")) {
                closePage();
                openPage(number, line.substring(3).strip());
                return;
            }
            if (line.startsWith("# ")) {
                closeChapter();
                chapterTitle = line.substring(2).strip();
                chapterLine = number;
                chapterPages = new ArrayList<>();
                if (chapterTitle.isEmpty()) {
                    problems.add(new Problem(number, "chapter without a title"));
                }
                return;
            }
            if (line.isEmpty()) {
                if (body != null && open >= 0) {
                    body.add("");
                }
                open = -1;
                return;
            }
            if (body == null) {
                problems.add(new Problem(number, "text outside of a page: " + line));
                return;
            }
            if (line.startsWith("#")) {
                problems.add(new Problem(number, "only # chapters and ## pages are supported; shown as text"));
            } else if (line.startsWith("|") || line.contains("![") || line.matches(".*\\[[^]]*]\\([^)]*\\).*")) {
                problems.add(new Problem(number, "tables, images and links are not supported; shown as text"));
            }
            if (line.startsWith("- ") || line.startsWith("* ") && !line.startsWith("**")) {
                body.add(BULLET + line.substring(2).strip());
                open = body.size() - 1;
            } else if (open >= 0) {
                body.set(open, body.get(open) + " " + line);
            } else {
                body.add(line);
                open = body.size() - 1;
            }
        }

        private void openPage(int number, String header) {
            pageLine = number;
            pageId = null;
            pageGrid = null;
            pageWarning = false;
            body = new ArrayList<>();
            open = -1;
            Matcher m = PAGE_ATTRIBUTES.matcher(header);
            pageTitle = m.matches() ? m.group(1).strip() : header;
            if (m.matches()) {
                for (String token : m.group(2).strip().split("\\s+")) {
                    if (token.isEmpty()) {
                        continue;
                    }
                    if (token.startsWith("#") && token.length() > 1) {
                        pageId = token.substring(1);
                    } else if (token.startsWith("grid=")) {
                        String grid = token.substring(5);
                        if (GuideGrids.byName(grid) == null) {
                            problems.add(new Problem(number, "unknown grid '" + grid + "', known: " + String.join(", ", GuideGrids.names())));
                        } else {
                            pageGrid = grid;
                        }
                    } else if (token.equals("warning")) {
                        pageWarning = true;
                    } else {
                        problems.add(new Problem(number, "unknown page attribute '" + token + "'"));
                    }
                }
            }
            if (pageTitle.isEmpty()) {
                problems.add(new Problem(number, "page without a title"));
            }
            if (pageId == null) {
                problems.add(new Problem(number, "page without an id, write {#some.id} after the title"));
                pageId = "line-" + number;
            } else if (!ids.add(pageId)) {
                problems.add(new Problem(number, "duplicate page id '" + pageId + "'"));
            }
        }

        private void closePage() {
            if (body == null) {
                return;
            }
            while (!body.isEmpty() && body.getLast().isEmpty()) {
                body.removeLast();
            }
            if (body.isEmpty()) {
                problems.add(new Problem(pageLine, "page '" + pageId + "' has no text"));
            }
            Page page = new Page(pageId, pageTitle, body, pageGrid, pageWarning);
            if (chapterPages == null) {
                problems.add(new Problem(pageLine, "page '" + pageId + "' before the first # chapter"));
            } else {
                chapterPages.add(page);
            }
            body = null;
        }

        private void closeChapter() {
            closePage();
            if (chapterPages == null) {
                return;
            }
            if (chapterPages.isEmpty()) {
                problems.add(new Problem(chapterLine, "chapter '" + chapterTitle + "' has no pages"));
            } else {
                chapters.add(new Chapter(chapterTitle, List.copyOf(chapterPages)));
            }
            chapterPages = null;
        }
    }
}
